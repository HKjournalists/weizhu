package com.weizhu.network;

import java.io.IOException;
import java.net.ConnectException;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Queue;

import com.google.protobuf.CodedInputStream;
import com.google.protobuf.CodedOutputStream;
import com.weizhu.proto.WeizhuProtos;

public class SocketStateMachine {
	
	private final Context context;
	
	public SocketStateMachine(SocketChannel socketChannel, SelectionKey selectionKey, 
			int connectTimeout, int verifyTimeout, int pingTimeout, int pingInterval, int sendBufferSize, int recvBufferSize,
			StateListener stateListener) {
		this.context = new Context(socketChannel, selectionKey, 
				connectTimeout, verifyTimeout, pingTimeout, pingInterval, 
				sendBufferSize, recvBufferSize, stateListener, 
				new InitState());
	}
	
	public boolean isWorking() {
		return context.currentState.isWorking();
	}
	
	public boolean isTerminated() {
		return context.currentState.isTerminated();
	}
	
	public State currentState() {
		return context.currentState;
	}
	
	public void handleEstablish(InetSocketAddress remoteAddress, WeizhuProtos.SocketEstablishRequest establishRequest) {
		this.context.handleEstablish(remoteAddress, establishRequest);
	}
	
	public void handleStop() {
		this.context.handleStop();
	}
	
	public void handlePing() {
		this.context.handlePing();
	}
	
	public void handleApiInvoke(WeizhuProtos.SocketApiRequest apiRequest, long expiredTime, ApiCallback callback) {
		this.context.handleApiInvoke(apiRequest, expiredTime, callback);
	}
	
	public void handlePushAck(WeizhuProtos.SocketPushAck pushAck) {
		this.context.handlePushAck(pushAck);
	}
	
	public void handleIO() {
		this.context.handleIO();
	}
	
	public void handleTimerCheck(long now) {
		this.context.handleTimerCheck(now);
	}

	private static class Context {
		
		private final SocketChannel socketChannel;
		private final SelectionKey selectionKey;
		
		private final int connectTimeout;
		private final int verifyTimeout;
		private final int pingTimeout;
		private final int pingInterval;
		private final int sendBufferSize;
		private final int recvBufferSize;
		
		private final StateListener stateListener;
		
		private State currentState;
		
		public Context(SocketChannel socketChannel, SelectionKey selectionKey, 
				int connectTimeout, int verifyTimeout, int pingTimeout, int pingInterval,
				int sendBufferSize, int recvBufferSize, StateListener stateListener, 
				State initState) {
			this.socketChannel = socketChannel;
			this.selectionKey = selectionKey;
			
			this.connectTimeout = connectTimeout;
			this.verifyTimeout = verifyTimeout;
			this.pingTimeout = pingTimeout;
			this.pingInterval = pingInterval;
			
			this.sendBufferSize = sendBufferSize;
			this.recvBufferSize = recvBufferSize;
			this.stateListener = stateListener;
			
			this.currentState = initState;
		}
		
		void handleEstablish(InetSocketAddress remoteAddress, WeizhuProtos.SocketEstablishRequest establishRequest) {
			final State curState = this.currentState;
			State nxtState;
			try {
				nxtState = curState.handleEstablish(this, remoteAddress, establishRequest);
			} catch (IOException e) {
				nxtState = new IOExceptionState(e);
			} catch (Exception e) {
				nxtState = new ExceptionState(e);
			}
			this.checkStateTransfer(nxtState);
		}
		
		void handleStop() {
			final State curState = this.currentState;
			State nxtState;
			try {
				nxtState = curState.handleStop(this);
			} catch (IOException e) {
				nxtState = new IOExceptionState(e);
			} catch (Exception e) {
				nxtState = new ExceptionState(e);
			}
			this.checkStateTransfer(nxtState);
		}
		
		void handlePing() {
			final State curState = this.currentState;
			State nxtState;
			try {
				nxtState = curState.handlePing(this);
			} catch (IOException e) {
				nxtState = new IOExceptionState(e);
			} catch (Exception e) {
				nxtState = new ExceptionState(e);
			}
			this.checkStateTransfer(nxtState);
		}
		
		void handleApiInvoke(WeizhuProtos.SocketApiRequest apiRequest, long expiredTime, ApiCallback callback) {
			final State curState = this.currentState;
			State nxtState;
			try {
				nxtState = curState.handleApiInvoke(this, apiRequest, expiredTime, callback);
			} catch (IOException e) {
				nxtState = new IOExceptionState(e);
			} catch (Exception e) {
				nxtState = new ExceptionState(e);
			}
			this.checkStateTransfer(nxtState);
		}
		
		void handlePushAck(WeizhuProtos.SocketPushAck pushAck) {
			final State curState = this.currentState;
			State nxtState;
			try {
				nxtState = curState.handlePushAck(this, pushAck);
			} catch (IOException e) {
				nxtState = new IOExceptionState(e);
			} catch (Exception e) {
				nxtState = new ExceptionState(e);
			}
			this.checkStateTransfer(nxtState);
		}
		
		void handleIO() {
			final State curState = this.currentState;
			State nxtState;
			try {
				nxtState = curState.handleIO(this);
			} catch (IOException e) {
				nxtState = new IOExceptionState(e);
			} catch (Exception e) {
				nxtState = new ExceptionState(e);
			}
			this.checkStateTransfer(nxtState);
		}
		
		void handleTimerCheck(long now) {
			final State curState = this.currentState;
			State nxtState;
			try {
				nxtState = curState.handleTimerCheck(this, now);
			} catch (IOException e) {
				nxtState = new IOExceptionState(e);
			} catch (Exception e) {
				nxtState = new ExceptionState(e);
			}
			this.checkStateTransfer(nxtState);
		}
		
		private void checkStateTransfer(State newState) {
			if (newState == null || newState == this.currentState) {
				return;
			}
			
			if (this.currentState.isTerminated()) {
				throw new Error("cannot transfer teminated state");
			}
			
			final State oldState = this.currentState;
			
			oldState.onExitState(this);
			
			this.currentState = newState;
			
			newState.onEnterState(this);
			
			if (stateListener != null) {
				stateListener.onStateChange(oldState, newState);
			}
		}
	}
	
	public static interface State {
		boolean isWorking();
		boolean isTerminated();
		
		void onEnterState(Context ctx);
		void onExitState(Context ctx);
		
		State handleEstablish(Context ctx, InetSocketAddress remoteAddress, WeizhuProtos.SocketEstablishRequest establishRequest) throws Exception;
		State handleStop(Context ctx) throws Exception;
		State handlePing(Context ctx) throws Exception;
		State handleApiInvoke(Context ctx, WeizhuProtos.SocketApiRequest apiRequest, long expiredTime, ApiCallback callback) throws Exception;
		State handlePushAck(Context ctx, WeizhuProtos.SocketPushAck pushAck) throws Exception;
		State handleIO(Context ctx) throws Exception;
		State handleTimerCheck(Context ctx, long now) throws Exception;
	}
	
	public static interface StateListener {
		void onStateChange(State oldState, State newState);
		void onResetPushSeq(long pushSeq);
		void onPushMsg(WeizhuProtos.SocketPushMsg pushMsg);
	}
	
	public static interface ApiCallback {
		void onResponse(WeizhuProtos.SocketApiResponse response);
		void onTimeout();
		void onSocketInactive();
	}
	
	private static abstract class AbstractState implements State {

		@Override
		public boolean isWorking() {
			return false;
		}
		
		@Override
		public boolean isTerminated() {
			return false;
		}
		
		@Override
		public void onEnterState(Context ctx) {
		}
		
		@Override
		public void onExitState(Context ctx) {
		}
		
		@Override
		public State handleEstablish(Context ctx, InetSocketAddress remoteAddress, WeizhuProtos.SocketEstablishRequest establishRequest) throws Exception {
			return null;
		}
		
		@Override
		public State handleStop(Context ctx) throws Exception {
			return null;
		}
		
		@Override
		public State handlePing(Context ctx) throws Exception {
			return null;
		}

		@Override
		public State handleApiInvoke(Context ctx, WeizhuProtos.SocketApiRequest apiRequest, long invokeTime, ApiCallback callback) throws Exception {
			callback.onSocketInactive();
			return null;
		}
		
		@Override
		public State handlePushAck(Context ctx, WeizhuProtos.SocketPushAck pushAck) throws Exception {
			return null;
		}

		@Override
		public State handleIO(Context ctx) throws Exception {
			return null;
		}

		@Override
		public State handleTimerCheck(Context ctx, long now) throws Exception {
			return null;
		}
	}
	
	public static class ExceptionState extends AbstractState {

		private final Throwable cause;
		
		public ExceptionState(Throwable cause) {
			this.cause = cause;
		}
		
		public Throwable cause() {
			return cause;
		}

		@Override
		public boolean isTerminated() {
			return true;
		}
		
		@Override
		public void onEnterState(Context ctx) {
			try {
				ctx.socketChannel.close();
			} catch (IOException e) {
				// ignore
			}
			ctx.selectionKey.cancel();
		}
	}
	
	public static class IOExceptionState extends AbstractState {
		
		private final IOException cause;
		
		public IOExceptionState(IOException cause) {
			this.cause = cause;
		}
		
		public IOException cause() {
			return cause;
		}

		@Override
		public boolean isTerminated() {
			return true;
		}
		
		@Override
		public void onEnterState(Context ctx) {
			try {
				ctx.socketChannel.close();
			} catch (IOException e) {
				// ignore
			}
			ctx.selectionKey.cancel();
		}
		
	}
	
	public static final class TerminateState extends AbstractState {
		
		public static final TerminateState INSTANCE = new TerminateState(0);
		
		private TerminateState(int x) {
		}
		
		@Override
		public boolean isTerminated() {
			return true;
		}
		
		@Override
		public void onEnterState(Context ctx) {
			try {
				ctx.socketChannel.close();
			} catch (IOException e) {
				// ignore
			}
			ctx.selectionKey.cancel();
		}
		
	}
	
	public static class InitState extends AbstractState {

		@Override
		public State handleEstablish(Context ctx, InetSocketAddress remoteAddress, WeizhuProtos.SocketEstablishRequest establishRequest) throws Exception {
			boolean connected = ctx.socketChannel.connect(remoteAddress);
			if (!connected) {
				return new ConnectingState(System.currentTimeMillis(), establishRequest);
			} else {
				WeizhuProtos.SocketUpPacket packet = WeizhuProtos.SocketUpPacket.newBuilder()
						.setEstablishRequest(establishRequest)
						.build();
				int packetLen = packet.getSerializedSize();
				
				ByteBuffer sendBuffer = ByteBuffer.allocate(packetLen + 4);
				sendBuffer.putInt(packetLen);
				CodedOutputStream codedOutput = CodedOutputStream.newInstance(sendBuffer);
				packet.writeTo(codedOutput);
				codedOutput.flush();
				sendBuffer.flip();
				
				while (true) {
					int writeByte = ctx.socketChannel.write(sendBuffer);
					
					if (!sendBuffer.hasRemaining()) {
						return new VerifyRecvingState(System.currentTimeMillis());
					}
					
					if (writeByte == 0) {
						return new VerifySendingState(sendBuffer);
					}
				}
			}
		}
		
		@Override
		public State handleStop(Context ctx) throws Exception {
			return TerminateState.INSTANCE;
		}
	}
	
	public static class ConnectingState extends AbstractState {
		
		private final long connectStartTime;
		private final WeizhuProtos.SocketEstablishRequest establishRequest;
		
		public ConnectingState(long connectStartTime, WeizhuProtos.SocketEstablishRequest establishRequest) {
			this.connectStartTime = connectStartTime;
			this.establishRequest = establishRequest;
		}
		
		@Override
		public void onEnterState(Context ctx) {
			ctx.selectionKey.interestOps(SelectionKey.OP_CONNECT);
		}
		
		@Override
		public State handleStop(Context ctx) throws Exception {
			return TerminateState.INSTANCE;
		}

		@Override
		public State handleIO(Context ctx) throws Exception {
			if (ctx.selectionKey.isConnectable()) {
				boolean connected = ctx.socketChannel.finishConnect();
				if (!connected) {
					// connect not complete
					return null;
				}

				WeizhuProtos.SocketUpPacket packet = WeizhuProtos.SocketUpPacket.newBuilder()
						.setEstablishRequest(establishRequest)
						.build();
				int packetLen = packet.getSerializedSize();

				ByteBuffer sendBuffer = ByteBuffer.allocate(packetLen + 4);
				sendBuffer.putInt(packetLen);
				CodedOutputStream codedOutput = CodedOutputStream.newInstance(sendBuffer);
				packet.writeTo(codedOutput);
				codedOutput.flush();
				sendBuffer.flip();
				
				while (true) {
					int writeByte = ctx.socketChannel.write(sendBuffer);
					
					if (!sendBuffer.hasRemaining()) {
						return new VerifyRecvingState(System.currentTimeMillis());
					}
					
					if (writeByte == 0) {
						return new VerifySendingState(sendBuffer);
					}
				}
			}
			return null;
		}

		@Override
		public State handleTimerCheck(Context ctx, long now) throws Exception {
			if (now - connectStartTime > ctx.connectTimeout) {
				throw new ConnectException("connect timeout : " + ctx.connectTimeout);
			}
			return null;
		}
	}
	
	public static class VerifySendingState extends AbstractState {

		private final ByteBuffer sendBuffer;
		
		public VerifySendingState(ByteBuffer sendBuffer) {
			this.sendBuffer = sendBuffer;
		}
		
		@Override
		public void onEnterState(Context ctx) {
			ctx.selectionKey.interestOps(SelectionKey.OP_WRITE);
		}
		
		@Override
		public State handleStop(Context ctx) throws Exception {
			return TerminateState.INSTANCE;
		}
		
		@Override
		public State handleIO(Context ctx) throws Exception {
			if (ctx.selectionKey.isWritable()) {
				while (true) {
					int writeByte = ctx.socketChannel.write(sendBuffer);
					
					if (!sendBuffer.hasRemaining()) {
						return new VerifyRecvingState(System.currentTimeMillis());
					}
					
					if (writeByte == 0) {
						return null;
					}
				}
			}
			return null;
		}
		
	}
	
	public static class VerifyRecvingState extends AbstractState {

		private final long verifyStartTime;
		private int packetLength = -1;
		private ByteBuffer recvBuffer = ByteBuffer.allocate(4);
		
		public VerifyRecvingState(long verifyStartTime) {
			this.verifyStartTime = verifyStartTime;
		}
		
		@Override
		public void onEnterState(Context ctx) {
			ctx.selectionKey.interestOps(SelectionKey.OP_READ);
		}
		
		@Override
		public State handleStop(Context ctx) throws Exception {
			return TerminateState.INSTANCE;
		}
		
		@Override
		public State handleIO(Context ctx) throws Exception {
			if (ctx.selectionKey.isReadable()) {
				while (true) {
					int readByte = ctx.socketChannel.read(recvBuffer);

					if (readByte < 0) {
						throw new SocketException("socket closed by peer");
					} else if (readByte == 0) {
						return null;
					}
					
					if (!recvBuffer.hasRemaining()) {
						recvBuffer.flip();
						if (packetLength < 0) {
							packetLength = recvBuffer.getInt();
							if (packetLength <= 0 || packetLength > 4 * 1024 * 1024) {
								throw new RuntimeException("invalid verify response packet length : " + packetLength);
							}
							recvBuffer = ByteBuffer.allocate(packetLength);
						} else {
							WeizhuProtos.SocketDownPacket packet = WeizhuProtos.SocketDownPacket
									.parseFrom(CodedInputStream.newInstance(recvBuffer));
							if (packet.getPacketCase() == WeizhuProtos.SocketDownPacket.PacketCase.ESTABLISH_RESPONSE) {
								WeizhuProtos.SocketEstablishResponse establishResponse = packet.getEstablishResponse();
								if (establishResponse.getResult() == WeizhuProtos.SocketEstablishResponse.Result.SUCC) {
									if (establishResponse.hasResetPushSeq()) {
										ctx.stateListener.onResetPushSeq(establishResponse.getResetPushSeq());
									}
									return new WorkingState(ctx.sendBufferSize, ctx.recvBufferSize);
								} else {
									return new VerifyFailState(establishResponse.getResult(), establishResponse.getFailText());
								}
							} else {
								throw new RuntimeException("invalid verify response packet : " + packet.getPacketCase());
							}
						}
					}
				}
			}
			return null;
		}

		@Override
		public State handleTimerCheck(Context ctx, long now) throws Exception {
			if (now - verifyStartTime > ctx.verifyTimeout) {
				throw new IOException("verify timeout");
			}
			return null;
		}
		
	}
	
	public static class VerifyFailState extends AbstractState {

		private final WeizhuProtos.SocketEstablishResponse.Result failResult;
		private final String failText;
		
		public VerifyFailState(WeizhuProtos.SocketEstablishResponse.Result failResult, String failText) {
			this.failResult = failResult;
			this.failText = failText;
		}
		
		public WeizhuProtos.SocketEstablishResponse.Result failResult() {
			return failResult;
		}
		
		public String failText() {
			return failText;
		}
		
		@Override
		public boolean isTerminated() {
			return true;
		}
		
		@Override
		public void onEnterState(Context ctx) {
			try {
				ctx.socketChannel.close();
			} catch (IOException e) {
				// ignore
			}
			ctx.selectionKey.cancel();
		}
	}
	
	public static class WorkingState extends AbstractState {

		private final Queue<WeizhuProtos.SocketUpPacket> waitSendQueue = new LinkedList<WeizhuProtos.SocketUpPacket>();
		private final LinkedList<ApiInvoke> waitApiResponseList = new LinkedList<ApiInvoke>();
		
		private final ByteBuffer defaultSendBuffer;
		private final ByteBuffer defaultRecvBuffer;
		
		private ByteBuffer sendBuffer = null;
		private ByteBuffer recvBuffer = null;
		private int recvPacketLength = -1;
		
		private long lastActiveTime = 0;
		private long pingStartTime = 0;
		
		public WorkingState(int sendBufferSize, int recvBufferSize) {
			this.defaultSendBuffer = ByteBuffer.allocate(sendBufferSize);
			this.defaultRecvBuffer = ByteBuffer.allocate(recvBufferSize);
			this.lastActiveTime = System.currentTimeMillis();
		}
		
		@Override
		public boolean isWorking() {
			return true;
		}
		
		@Override
		public void onEnterState(Context ctx) {
			ctx.selectionKey.interestOps(SelectionKey.OP_READ);
		}
		
		@Override
		public void onExitState(Context ctx) {
			waitSendQueue.clear();
			if (!waitApiResponseList.isEmpty()) {
				for (ApiInvoke apiInvoke : waitApiResponseList) {
					apiInvoke.callback.onSocketInactive();
				}
				waitApiResponseList.clear();
			}
			this.sendBuffer = null;
			this.recvBuffer = null;
		}
		
		@Override
		public State handleStop(Context ctx) throws Exception {
			return TerminateState.INSTANCE;
		}
		
		@Override
		public State handlePing(Context ctx) throws Exception {
			if (pingStartTime <= 0) {
				pingStartTime = System.currentTimeMillis();
				waitSendQueue.offer(WeizhuProtos.SocketUpPacket.newBuilder()
						.setPing(WeizhuProtos.SocketPing.newBuilder()
								.setId((int)(pingStartTime/1000L))
								.build())
						.build());
				doSend(ctx);
			}
			return null;
		}

		@Override
		public State handleApiInvoke(Context ctx, WeizhuProtos.SocketApiRequest apiRequest, long expiredTime, ApiCallback callback) throws Exception {
			waitSendQueue.offer(WeizhuProtos.SocketUpPacket.newBuilder()
					.setApiRequest(apiRequest)
					.build());
			waitApiResponseList.addLast(new ApiInvoke(apiRequest.getInvoke(), expiredTime, callback));
			doSend(ctx);
			return null;
		}
		
		@Override
		public State handlePushAck(Context ctx, WeizhuProtos.SocketPushAck pushAck) throws Exception {
			waitSendQueue.offer(WeizhuProtos.SocketUpPacket.newBuilder()
					.setPushAck(pushAck)
					.build());
			doSend(ctx);
			return null;
		}

		@Override
		public State handleIO(Context ctx) throws Exception {
			if (ctx.selectionKey.isReadable()) {
				lastActiveTime = System.currentTimeMillis();
				doRecv(ctx);
			}
			if (ctx.selectionKey.isWritable()) {
				doSend(ctx);
			}
			return null;
		}
		
		@Override
		public State handleTimerCheck(Context ctx, long now) throws Exception {
			if (!waitApiResponseList.isEmpty()) {
				Iterator<ApiInvoke> it = waitApiResponseList.iterator();
				while (it.hasNext()) {
					ApiInvoke apiInvoke = it.next();
					if (now > apiInvoke.expiredTime) {
						apiInvoke.callback.onTimeout();
						it.remove();
					}
				}
			}
			
			if (pingStartTime <= 0) {
				if (now - lastActiveTime > ctx.pingInterval) {
					pingStartTime = now;
					waitSendQueue.offer(WeizhuProtos.SocketUpPacket.newBuilder()
							.setPing(WeizhuProtos.SocketPing.newBuilder()
									.setId((int)(pingStartTime/1000L))
									.build())
							.build());
					doSend(ctx);
				}
			} else {
				if (now - pingStartTime > ctx.pingTimeout) {
					throw new IOException("ping timeout : " + ctx.pingTimeout);
				}
			}
			return null;
		}

		private void doSend(Context ctx) throws Exception {
			while (true) {
				if (sendBuffer == null) {
					WeizhuProtos.SocketUpPacket packet = waitSendQueue.poll();
					if (packet == null) {
						ctx.selectionKey.interestOps(SelectionKey.OP_READ);
						return;
					}
					
					int packetLength = packet.getSerializedSize();
					if (packetLength <= 0) {
						throw new RuntimeException("invalid SerializedSize");
					}
					
					if (packetLength + 4 <= defaultSendBuffer.capacity()) {
						defaultSendBuffer.clear();
						sendBuffer = defaultSendBuffer;
					} else {
						sendBuffer = ByteBuffer.allocate(packetLength + 4);
					}
					
					sendBuffer.putInt(packetLength);
					CodedOutputStream codedOutput = CodedOutputStream.newInstance(sendBuffer);
					packet.writeTo(codedOutput);
					codedOutput.flush();
					sendBuffer.flip();
				}
				
				int writeByte = ctx.socketChannel.write(sendBuffer);
				
				if (!sendBuffer.hasRemaining()) {
					sendBuffer = null;
				} 
				
				if (writeByte == 0) {
					ctx.selectionKey.interestOps(SelectionKey.OP_READ | SelectionKey.OP_WRITE);
					return;
				}
			}
		}
		
		private void doRecv(Context ctx) throws Exception {
			while (true) {
				if (recvBuffer == null) {
					defaultRecvBuffer.clear();
					defaultRecvBuffer.position(0).limit(4);
					recvBuffer = defaultRecvBuffer;
				}
				
				int readByte = ctx.socketChannel.read(recvBuffer);
				if (!recvBuffer.hasRemaining()) {
					recvBuffer.flip();
					if (recvPacketLength < 0) {
						recvPacketLength = recvBuffer.getInt();
						
						if (recvPacketLength <= 0 || recvPacketLength > 4 * 1024 * 1024) {
							throw new RuntimeException("recv invalid packet length : " + recvPacketLength);
						}
						
						if (recvPacketLength <= defaultRecvBuffer.capacity()) {
							defaultRecvBuffer.clear();
							defaultRecvBuffer.position(0).limit(recvPacketLength);
							recvBuffer = defaultRecvBuffer;
						} else {
							recvBuffer = ByteBuffer.allocate(recvPacketLength);
						}
					} else {
						doRecvPacket(ctx, WeizhuProtos.SocketDownPacket.parseFrom(CodedInputStream.newInstance(recvBuffer)));
						recvPacketLength = -1;
						recvBuffer = null;
					}
				}
				
				if (readByte < 0) {
					throw new IOException("socket closed by peer");
				} else if (readByte == 0) {
					return;
				}
			}
		}
		
		private void doRecvPacket(Context ctx, WeizhuProtos.SocketDownPacket packet) throws Exception {
			switch (packet.getPacketCase()) {
				case API_RESPONSE: {
					WeizhuProtos.SocketApiResponse apiResponse = packet.getApiResponse();
					
					Iterator<ApiInvoke> it = waitApiResponseList.iterator();
					while (it.hasNext()) {
						ApiInvoke apiInvoke = it.next();
						if (apiInvoke.invoke.getServiceName().equals(apiResponse.getInvoke().getServiceName())
								&& apiInvoke.invoke.getFunctionName().equals(apiResponse.getInvoke().getFunctionName())
								&& apiInvoke.invoke.getInvokeId() == apiResponse.getInvoke().getInvokeId()) {
							apiInvoke.callback.onResponse(apiResponse);
							it.remove();
							break;
						}
					}
					
					break;
				}
				case PUSH_MSG: {
					ctx.stateListener.onPushMsg(packet.getPushMsg());
					break;
				}
				case PING: {
					WeizhuProtos.SocketPing ping = packet.getPing();
					waitSendQueue.offer(WeizhuProtos.SocketUpPacket.newBuilder()
							.setPong(WeizhuProtos.SocketPong.newBuilder()
									.setId(ping.getId())
									.build())
							.build());
					doSend(ctx);
					break;
				}
				case PONG: {
					WeizhuProtos.SocketPong pong = packet.getPong();
					if ((int) (pingStartTime / 1000L) != pong.getId()) {
						throw new RuntimeException("invalid pong packet!");
					}
					pingStartTime = -1;
					break;
				}
				default:
					break;
			}
		}
		
	}
	
	private static class ApiInvoke {
		final WeizhuProtos.Invoke invoke;
		final long expiredTime;
		final ApiCallback callback;
		
		ApiInvoke(WeizhuProtos.Invoke invoke, long expiredTime, ApiCallback callback) {
			this.invoke = invoke;
			this.expiredTime = expiredTime;
			this.callback = callback;
		}
	}
	
}
