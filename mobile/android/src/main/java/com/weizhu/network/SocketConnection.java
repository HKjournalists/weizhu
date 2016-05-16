package com.weizhu.network;

import java.io.IOException;
import java.lang.reflect.Method;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.MessageLite;
import com.google.protobuf.Parser;
import com.weizhu.proto.ServiceInvoker;
import com.weizhu.proto.WeizhuProtos;
import com.weizhu.proto.WeizhuProtos.SocketApiRequest;
import com.weizhu.proto.WeizhuProtos.SocketPushMsg;


//长连接，Nio, 重连
public class SocketConnection implements ServiceInvoker {
	
	private static final Map<String, PushMsgMethod> PUSH_MSG_METHOD_MAP = Collections.unmodifiableMap(traitsPushMsgMethod(PushListener.class));
	
	// push
	private final ExecutorService pushExecutor;
	private final PushListener pushListener;
	
	// api
	private final ExecutorService apiExecutor;
	private final AtomicInteger apiInvokeId;
	
	// common
	private final WeizhuProtos.Weizhu weizhu;
	private final WeizhuProtos.Android android;
	private final WorkThead workThread;
	
	public SocketConnection(PushListener pushListener, int apiExecutorSize, WeizhuProtos.Weizhu weizhu, WeizhuProtos.Android android) {
		this.pushExecutor = Executors.newSingleThreadExecutor();
		this.pushListener = pushListener;
		
		this.apiExecutor = Executors.newFixedThreadPool(apiExecutorSize);
		this.apiInvokeId = new AtomicInteger(0);
		
		this.weizhu = weizhu;
		this.android = android;

		this.workThread = new WorkThead();
	}
	
	public void connect(InetSocketAddress socketAddress, ByteString sessionKey, long pushSeq) {
		workThread.sendEvent(new ConnectEvent(socketAddress, sessionKey, pushSeq));
	}
	
	public void tryConnect() {
		workThread.sendEvent(TryConnectEvent.INSTANCE);
	}
	
	public void disconnect() {
		workThread.sendEvent(DisconnectEvent.INSTANCE);
	}
	
	@Override
	public <V extends MessageLite> Future<V> invoke(String serviceName, String functionName, 
			MessageLite request, Parser<V> responseParser, int priorityNum) {
		
		WeizhuProtos.Invoke invoke = WeizhuProtos.Invoke.newBuilder()
				.setServiceName(serviceName)
				.setFunctionName(functionName)
				.setInvokeId(apiInvokeId.incrementAndGet())
				.build();
		
		ApiAdapterTask<V> adapterTask = new ApiAdapterTask<V>(responseParser);
		FutureTask<V> task = new FutureTask<V>(adapterTask, priorityNum);
		
		workThread.sendEvent(new ApiInvokeEvent(SocketApiRequest.newBuilder()
				.setInvoke(invoke)
				.setRequestBody(request.toByteString())
				.build(), 
				System.currentTimeMillis() + 30000, 
				new ApiCallback<V>(apiExecutor, adapterTask, task)));
		
		return task;
	}
	
	private void pushAck(long pushSeq) {
		workThread.sendEvent(new PushAckEvent(pushSeq));
	}
	
	public void shutdown() {
		workThread.interrupt();
		apiExecutor.shutdown();
		pushExecutor.shutdown();
	}
	
	private void doPushMsg(WeizhuProtos.SocketPushMsg socketPushMsg) {
		PushMsgMethod pushMsgMethod = PUSH_MSG_METHOD_MAP.get(socketPushMsg.getPushMsg().getPushName());
		if (pushMsgMethod != null) {
			try {
				final Method pushMethod = pushMsgMethod.pushMethod;
				final long pushSeq = socketPushMsg.getPushMsg().getPushSeq();
				final MessageLite pushMsg = pushMsgMethod.pushMsgParser.parseFrom(socketPushMsg.getPushMsg().getPushBody());
				final boolean hasMore = socketPushMsg.getHasMore();
				
				pushExecutor.execute(new Runnable() {

					@Override
					public void run() {
						try {
							pushMethod.invoke(SocketConnection.this.pushListener, pushSeq, pushMsg, hasMore);
						} catch (Throwable th) {
							// log error
						} finally {
							SocketConnection.this.pushAck(pushSeq);
						}
					}
					
				});
			} catch (InvalidProtocolBufferException e) {
				// ignore
			}
		}
	}
	
	private void doLog(final InetSocketAddress remoteAddress, final String msg, final Throwable th) {
		pushExecutor.execute(new Runnable() {

			@Override
			public void run() {
				SocketConnection.this.pushListener.log(remoteAddress, msg, th);
			}
			
		});
	}
	
	private class WorkThead extends Thread {
		
		private final Selector selector;
		
		WorkThead() {
			try {
				this.selector = Selector.open();
			} catch (IOException e) {
				throw new RuntimeException("cannot open selector", e);
			}
			this.setName("WeizhuSocketConnection");
			this.start();
		}
		
		private final ConcurrentLinkedQueue<Object> eventQueue = new ConcurrentLinkedQueue<Object>();
		
		void sendEvent(Object event) {
			eventQueue.offer(event);
			selector.wakeup();
		}
		
		@Override
		public void run() {
			InetSocketAddress remoteAddress = null;
			ByteString sessionKey = null;
			long pushSeq = 0;
			
			SocketStateMachine currentSocket = null;
			long socketTerminatedTime = 0;
			long socketCreateInterval = 0;
			int socketFailCount = 0;
			
			final long[] SOCKET_CREATE_INTERVAL = new long[]{0, 3000, 15000, 30000, 60000};
			
			final LinkedList<ApiInvokeEvent> waitApiInvokeList = new LinkedList<ApiInvokeEvent>();
			
			while (true) {
				try {
					selector.select(3000);
					
					Set<SelectionKey> selectedKeySet = selector.selectedKeys();
					if (!selectedKeySet.isEmpty()) {
						Iterator<SelectionKey> it = selectedKeySet.iterator();
						while (it.hasNext()) {
							final SelectionKey key = it.next();
							final SocketStateMachine socket = (SocketStateMachine) key.attachment();
							socket.handleIO();
							it.remove();
						}
					}
					
					long now = System.currentTimeMillis();
					
					if (currentSocket != null) {
						if (currentSocket.isTerminated()) {
							socketTerminatedTime = now;
							if (currentSocket.currentState() instanceof SocketStateMachine.VerifyFailState) {
								sessionKey = null;
							} else {
								socketCreateInterval = socketFailCount < SOCKET_CREATE_INTERVAL.length ? 
										SOCKET_CREATE_INTERVAL[socketFailCount] : SOCKET_CREATE_INTERVAL[SOCKET_CREATE_INTERVAL.length - 1];
								socketFailCount++;
							}
							currentSocket = null;
						} else if (currentSocket.isWorking()) {
							socketFailCount = 0;
						}
					}
					
					// do event
					Object event = null;
					while ((event = eventQueue.poll()) != null) {
						if (event instanceof ConnectEvent) {
							ConnectEvent connectEvent = (ConnectEvent) event;
							
							if (currentSocket != null) {
								currentSocket.handleStop();
								currentSocket = null;
							}
							
							remoteAddress = connectEvent.remoteAddress;
							sessionKey = connectEvent.sessionKey;
							if (connectEvent.pushSeq >= 0) {
								pushSeq = connectEvent.pushSeq;
							}
							
							currentSocket = createSocket(new StateListener(remoteAddress));
							currentSocket.handleEstablish(remoteAddress, buildEstablishRequest(sessionKey, pushSeq));
							socketFailCount = 0;
							
						} else if (event instanceof TryConnectEvent) {
							if (currentSocket != null) {
								currentSocket.handlePing();
							} else {
								socketCreateInterval = 0; // immediate create
							}
						} else if (event instanceof DisconnectEvent) {
							remoteAddress = null;
							sessionKey = null;
							pushSeq = 0;
							
							if (currentSocket != null) {
								currentSocket.handleStop();
								currentSocket = null;
							}
						} else if (event instanceof PushAckEvent) {
							PushAckEvent pushAckEvent = (PushAckEvent) event;
							
							if (pushAckEvent.pushSeq > pushSeq) {
								pushSeq = pushAckEvent.pushSeq;
							}
							
							if (currentSocket != null) {
								currentSocket.handlePushAck(WeizhuProtos.SocketPushAck.newBuilder()
										.setPushSeq(pushAckEvent.pushSeq)
										.build());
							}
						} else if (event instanceof ApiInvokeEvent) {
							ApiInvokeEvent apiInvokeEvent = (ApiInvokeEvent) event;
							waitApiInvokeList.add(apiInvokeEvent);
						}
					}
					
					if (!waitApiInvokeList.isEmpty()) {
						boolean isWorking = currentSocket != null && currentSocket.isWorking();
						boolean isDisconnect = remoteAddress == null || sessionKey == null;
						
						Iterator<ApiInvokeEvent> it = waitApiInvokeList.iterator();
						while (it.hasNext()) {
							ApiInvokeEvent apiInvokeEvent = it.next();
							if (now > apiInvokeEvent.expiredTime) {
								apiInvokeEvent.apiCallback.onTimeout();
								it.remove();
							} else if (isWorking) {
								currentSocket.handleApiInvoke(apiInvokeEvent.apiRequest, apiInvokeEvent.expiredTime, apiInvokeEvent.apiCallback);
								it.remove();
							} else if (isDisconnect) {
								apiInvokeEvent.apiCallback.onSocketInactive();
								it.remove();
							}
						}
						
						if (currentSocket == null && !waitApiInvokeList.isEmpty()) {
							socketCreateInterval = 0;
						}
					}
					
					// timer check
					if (currentSocket != null) {
						currentSocket.handleTimerCheck(now);
					} else { 
						if (now >= socketTerminatedTime + socketCreateInterval
								&& remoteAddress != null && sessionKey != null) {
							currentSocket = createSocket(new StateListener(remoteAddress));
							currentSocket.handleEstablish(remoteAddress, buildEstablishRequest(sessionKey, pushSeq));
						}
					}
					
					if (Thread.interrupted()) {
						if (currentSocket != null) {
							remoteAddress = null;
							sessionKey = null;
							pushSeq = 0;
							currentSocket.handleStop();
							currentSocket = null;
						}
						break;
					}
				} catch (Throwable th) {
					SocketConnection.this.doLog(remoteAddress, "selector loop exception", th);
					try {
						// 防止IO异常导致死循环
						Thread.sleep(1000);
					} catch (InterruptedException e) {
						// ignore
					}
				}
			}
			
			try {
				selector.close();
			} catch (IOException e) {
				// log error
			}
		}
		
		private SocketStateMachine createSocket(StateListener stateListener) throws IOException {
			SocketChannel channel = SocketChannel.open();
			channel.configureBlocking(false);
			channel.socket().setTcpNoDelay(true);
			channel.socket().setKeepAlive(true);
			
			SelectionKey selectionKey = channel.register(selector, 0);
			
			SocketStateMachine socket = new SocketStateMachine(channel, selectionKey, 
					30000, 30000, 15000, 90000, 64 * 1024, 64 * 1024, 
					stateListener);
			
			selectionKey.attach(socket);
			
			return socket;
		}
		
		private WeizhuProtos.SocketEstablishRequest buildEstablishRequest(ByteString sessionKey, long pushSeq) {
			return WeizhuProtos.SocketEstablishRequest.newBuilder()
					.setSessionKey(sessionKey)
					.setNetworkType(WeizhuProtos.Network.Type.UNKNOWN)
					.setWeizhu(SocketConnection.this.weizhu)
					.setAndroid(SocketConnection.this.android)
					.setPushSeq(pushSeq)
					.addAllPushName(PUSH_MSG_METHOD_MAP.keySet())
					.build();
		}
		
	}
	
	private static class ConnectEvent {
		final InetSocketAddress remoteAddress;
		final ByteString sessionKey;
		final long pushSeq;
		ConnectEvent(InetSocketAddress remoteAddress, ByteString sessionKey, long pushSeq) {
			this.remoteAddress = remoteAddress;
			this.sessionKey = sessionKey;
			this.pushSeq = pushSeq;
		}
	}
	
	private static class TryConnectEvent {
		static final TryConnectEvent INSTANCE = new TryConnectEvent();
	}
	
	private static class DisconnectEvent {
		static final DisconnectEvent INSTANCE = new DisconnectEvent();
	}
	
	private static class PushAckEvent {
		final long pushSeq;
		PushAckEvent(long pushSeq) {
			this.pushSeq = pushSeq;
		}
	}
	
	private static class ApiInvokeEvent {
		final WeizhuProtos.SocketApiRequest apiRequest;
		final long expiredTime;
		final ApiCallback<?> apiCallback;
		ApiInvokeEvent(WeizhuProtos.SocketApiRequest apiRequest,
				long expiredTime,
				ApiCallback<?> apiCallback) {
			this.apiRequest = apiRequest;
			this.expiredTime = expiredTime;
			this.apiCallback = apiCallback;
		}
	}
	
	private static class PushMsgMethod {
		final Method pushMethod;
		final Parser<? extends MessageLite> pushMsgParser;
		
		PushMsgMethod(Method pushMethod, Parser<? extends MessageLite> pushMsgParser) {
			this.pushMethod = pushMethod;
			this.pushMsgParser = pushMsgParser;
		}
	}
	
	// name , method , parser,
	private static Map<String, PushMsgMethod> traitsPushMsgMethod(Class<?> pushListenerClazz) {
		Map<String, PushMsgMethod> pushMap = new LinkedHashMap<String, PushMsgMethod>();
		for (Method method : pushListenerClazz.getMethods()) {
			if (method.getName().startsWith("on") && method.getName().endsWith("Push")) {
				Class<?>[] paramTypes = method.getParameterTypes();
				if (paramTypes.length == 3 && paramTypes[0] == long.class 
						&& MessageLite.class.isAssignableFrom(paramTypes[1])
						&& paramTypes[2] == boolean.class) {
					try {
						String pushName = method.getName().substring("on".length());
						MessageLite pushMsgDefaultInstance = (MessageLite) (paramTypes[1].getMethod("getDefaultInstance").invoke(null));
						
						pushMap.put(pushName, new PushMsgMethod(method, pushMsgDefaultInstance.getParserForType()));
						
					} catch (Exception e) {
						// error , ignore
					}
				}
			}
		}
		return pushMap;
	}
	
	private class StateListener implements SocketStateMachine.StateListener {
		
		final InetSocketAddress remoteAddress;
		
		StateListener(InetSocketAddress remoteAddress) {
			this.remoteAddress = remoteAddress;
		}
		
		@Override
		public void onStateChange(final SocketStateMachine.State oldState, final SocketStateMachine.State newState) {
			StringBuilder sb = new StringBuilder();
			sb.append(oldState.getClass().getSimpleName());
			sb.append(" -> ");
			sb.append(newState.getClass().getSimpleName());
			SocketConnection.this.doLog(remoteAddress, sb.toString(), null);
			
			if ((oldState instanceof SocketStateMachine.InitState ) 
					&& (newState instanceof SocketStateMachine.ConnectingState 
							|| newState instanceof SocketStateMachine.VerifySendingState 
							|| newState instanceof SocketStateMachine.VerifyRecvingState)) {
				SocketConnection.this.pushExecutor.execute(new Runnable() {

					@Override
					public void run() {
						SocketConnection.this.pushListener.onEstablishing(StateListener.this.remoteAddress);
					}
					
				});
			} else if (newState instanceof SocketStateMachine.WorkingState) {
				SocketConnection.this.pushExecutor.execute(new Runnable() {

					@Override
					public void run() {
						SocketConnection.this.pushListener.onWorking(StateListener.this.remoteAddress);
					}
					
				});
			} else if (newState instanceof SocketStateMachine.VerifyFailState) {
				final SocketStateMachine.VerifyFailState verifyFailState = (SocketStateMachine.VerifyFailState) newState;
				SocketConnection.this.pushExecutor.execute(new Runnable() {

					@Override
					public void run() {
						SocketConnection.this.pushListener.onVerifyFail(StateListener.this.remoteAddress, verifyFailState.failResult(), verifyFailState.failText());
					}
					
				});
			} else if (newState instanceof SocketStateMachine.TerminateState) {
				SocketConnection.this.pushExecutor.execute(new Runnable() {

					@Override
					public void run() {
						SocketConnection.this.pushListener.onTerminate(StateListener.this.remoteAddress);
					}
					
				});
			} else if (newState instanceof SocketStateMachine.ExceptionState) {
				final SocketStateMachine.ExceptionState exceptionState = (SocketStateMachine.ExceptionState) newState;
				SocketConnection.this.pushExecutor.execute(new Runnable() {

					@Override
					public void run() {
						SocketConnection.this.pushListener.onException(StateListener.this.remoteAddress, exceptionState.cause());
					}
					
				});
			} else if (newState instanceof SocketStateMachine.IOExceptionState) {
				final SocketStateMachine.IOExceptionState ioExceptionState = (SocketStateMachine.IOExceptionState) newState;
				SocketConnection.this.pushExecutor.execute(new Runnable() {

					@Override
					public void run() {
						SocketConnection.this.pushListener.onIOException(StateListener.this.remoteAddress, ioExceptionState.cause());
					}
					
				});
			}
			
		}

		@Override
		public void onResetPushSeq(final long pushSeq) {
			SocketConnection.this.pushExecutor.execute(new Runnable() {

				@Override
				public void run() {
					SocketConnection.this.pushListener.onResetPushSeq(pushSeq);
				}
				
			});
		}

		@Override
		public void onPushMsg(SocketPushMsg pushMsg) {
			SocketConnection.this.doPushMsg(pushMsg);
		}

	}
	
	private static class ApiAdapterTask<V extends MessageLite> implements Callable<V> {

		private final Parser<V> responseParser;
		
		public ApiAdapterTask(Parser<V> responseParser) {
			this.responseParser = responseParser;
		}
		
		private String failText;
		private WeizhuProtos.SocketApiResponse apiResponse;
		
		void setFail(String failText) {
			this.failText = failText;
		}
		
		void setResponse(WeizhuProtos.SocketApiResponse apiResponse) {
			this.apiResponse = apiResponse;
		}
		
		@Override
		public V call() throws Exception {
			if (failText != null) {
				throw new RuntimeException(failText);
			}
			switch(apiResponse.getResult()) {
				case SUCC:
					return responseParser.parseFrom(apiResponse.getResponseBody());
				default:
					throw new SocketApiException(apiResponse.getResult(), apiResponse.getFailText(), 
							apiResponse.getResult() + ":" + apiResponse.getFailText());
			}
		}
	}
	
	private static class ApiCallback<V extends MessageLite> implements SocketStateMachine.ApiCallback {

		private final Executor executor;
		private final ApiAdapterTask<V> adapterTask;
		private final FutureTask<V> futureTask;
		
		ApiCallback(Executor executor,
				ApiAdapterTask<V> adapterTask, FutureTask<V> futureTask) {
			this.executor = executor;
			this.adapterTask = adapterTask;
			this.futureTask = futureTask;
		}
		
		@Override
		public void onResponse(WeizhuProtos.SocketApiResponse response) {
			adapterTask.setResponse(response);
			executor.execute(futureTask);
		}

		@Override
		public void onTimeout() {
			adapterTask.setFail("调用超时");
			executor.execute(futureTask);
		}

		@Override
		public void onSocketInactive() {
			adapterTask.setFail("连接不可用");
			executor.execute(futureTask);
		}
		
	}
}
