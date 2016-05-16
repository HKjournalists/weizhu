package com.weizhu.server.conn;

import io.netty.channel.Channel;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.channel.socket.SocketChannel;

import java.util.ArrayDeque;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.TreeMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.protobuf.ByteString;
import com.weizhu.common.server.LogUtil;
import com.weizhu.common.service.ServiceInvoker;
import com.weizhu.common.service.exception.HeadUnknownException;
import com.weizhu.common.service.exception.InvokeUnknownException;
import com.weizhu.common.service.exception.RequestParseException;
import com.weizhu.proto.PushService;
import com.weizhu.proto.SessionService;
import com.weizhu.proto.WeizhuProtos;
import com.weizhu.proto.PushProtos.GetOfflineMsgRequest;
import com.weizhu.proto.PushProtos.GetOfflineMsgResponse;
import com.weizhu.proto.SessionProtos.VerifySessionKeyRequest;
import com.weizhu.proto.SessionProtos.VerifySessionKeyResponse;
import com.weizhu.proto.WeizhuProtos.AnonymousHead;
import com.weizhu.proto.WeizhuProtos.RequestHead;
import com.weizhu.proto.WeizhuProtos.SocketApiRequest;
import com.weizhu.proto.WeizhuProtos.SocketApiResponse;
import com.weizhu.proto.WeizhuProtos.SocketDownPacket;
import com.weizhu.proto.WeizhuProtos.SocketEstablishRequest;
import com.weizhu.proto.WeizhuProtos.SocketEstablishResponse;
import com.weizhu.proto.WeizhuProtos.SocketPong;
import com.weizhu.proto.WeizhuProtos.SocketPushAck;
import com.weizhu.proto.WeizhuProtos.SocketPushMsg;

public class SocketConnectionHandler extends ChannelDuplexHandler{

	private static final Logger logger = LoggerFactory.getLogger(SocketConnectionHandler.class);
	
	private final SessionService sessionService;
	private final PushService pushService;
	private final ImmutableMap<String, ServiceInvoker> serviceInvokerMap;
	private final SocketRegistry socketRegistry;
	
	SocketConnectionHandler(SessionService sessionService, PushService pushService, 
			ImmutableMap<String, ServiceInvoker> serviceInvokerMap, 
			SocketRegistry socketRegistry) {
		this.sessionService = sessionService;
		this.pushService = pushService;
		this.serviceInvokerMap = serviceInvokerMap;
		this.socketRegistry = socketRegistry;
	}
	
	private static final int MAX_PUSHING_SIZE = 20;
	
	private final Queue<Long> pushingSeqQueue = new ArrayDeque<Long>(MAX_PUSHING_SIZE);
	private final LinkedList<WeizhuProtos.PushMessage> pushMsgList = new LinkedList<WeizhuProtos.PushMessage>();
	
	private WeizhuProtos.RequestHead headPrototype;
	private WeizhuProtos.Session session;
	
	@Override
	public void channelActive(ChannelHandlerContext ctx) throws Exception {
	    log(ctx, "active", null);
	    ctx.fireChannelActive();
	}
	
	@Override
	public void channelInactive(ChannelHandlerContext ctx) throws Exception {
		log(ctx, "inactive", null);
		if (headPrototype != null && session != null) {
			socketRegistry.unregister(session, (SocketChannel) ctx.channel());
		}
		ctx.fireChannelInactive();
	}
	
	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
		log(ctx, "exception", cause);
	}
	
	@Override
	public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
		if (!(msg instanceof WeizhuProtos.SocketUpPacket)) {
			ctx.fireChannelRead(msg);
			return;
		}
		
		final WeizhuProtos.SocketUpPacket packet = (WeizhuProtos.SocketUpPacket) msg;
		switch (packet.getPacketCase()) {
			case ESTABLISH_REQUEST: {
				processEstablishRequest(ctx, packet.getEstablishRequest());
				break;
			}
			case API_REQUEST: {
				processApiRequest(ctx, packet.getApiRequest());
				break;
			}
			case PUSH_ACK: {
				processPushAck(ctx, packet.getPushAck());
				break;
			}
			case PING: {
				log(ctx, "Ping", null);
				ctx.writeAndFlush(SocketDownPacket.newBuilder()
						.setPong(SocketPong.newBuilder()
								.setId(packet.getPing().getId())
								.build())
						.build(), ctx.voidPromise());
				break;
			}
			default:
				break;
		}
	}
	
	@Override
	public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
		if (msg instanceof SocketEstablishResult) {
			SocketEstablishResult result = (SocketEstablishResult) msg;
			
			this.session = result.session;
			TreeMap<Long, WeizhuProtos.PushMessage> pushMsgMap = new TreeMap<Long, WeizhuProtos.PushMessage>();
			for (WeizhuProtos.PushMessage pushMsg : result.offlineMsgList) {
				pushMsgMap.put(pushMsg.getPushSeq(), pushMsg);
			}
			for (WeizhuProtos.PushMessage pushMsg : pushMsgList) {
				pushMsgMap.put(pushMsg.getPushSeq(), pushMsg);
			}
			
			this.pushMsgList.clear();
			this.pushMsgList.addAll(pushMsgMap.values());
			doPush(ctx);
		} else if (msg instanceof WeizhuProtos.PushMessage) {
			pushMsgList.addLast((WeizhuProtos.PushMessage) msg);
			doPush(ctx);
		} else {
			ctx.write(msg, promise);
		}
	}
	
	private void processEstablishRequest(ChannelHandlerContext ctx, final SocketEstablishRequest establishRequest) {
		logEstablishRequest(ctx, establishRequest);
		
		if (headPrototype != null) {
			// invalid state
			ctx.close();
		}
		
		WeizhuProtos.RequestHead.Builder headBuilder = WeizhuProtos.RequestHead.newBuilder();
		headBuilder.setNetwork(WeizhuProtos.Network.newBuilder()
				.setType(establishRequest.getNetworkType())
				.setProtocol(WeizhuProtos.Network.Protocol.SOCKET_PB)
				.setRemoteHost(((SocketChannel)(ctx.channel())).remoteAddress().getHostString())
				.setRemotePort(((SocketChannel)(ctx.channel())).remoteAddress().getPort())
				.build());
		headBuilder.setWeizhu(establishRequest.getWeizhu());
		if (establishRequest.hasAndroid()) {
			headBuilder.setAndroid(establishRequest.getAndroid());
		}
		if (establishRequest.hasIphone()) {
			headBuilder.setIphone(establishRequest.getIphone());
		}
		headPrototype = headBuilder.buildPartial();
		
		AnonymousHead anonymousHead = buildAnonymousHead(headPrototype, ESTABLISH_INVOKE);
		VerifySessionKeyRequest verifySessionRequest = VerifySessionKeyRequest.newBuilder()
				.setSessionKey(establishRequest.getSessionKey())
				.build();
		ListenableFuture<VerifySessionKeyResponse> verifyFuture;
		try {
			verifyFuture = this.sessionService.verifySessionKey(anonymousHead, verifySessionRequest);
		} catch (Throwable th) {
			verifyFuture = Futures.immediateFailedFuture(th);
		}
		
		final Channel channel = ctx.channel();
		Futures.addCallback(verifyFuture, new FutureCallback<VerifySessionKeyResponse>() {

			@Override
			public void onFailure(Throwable t) {
				channel.writeAndFlush(SocketDownPacket.newBuilder()
						.setEstablishResponse(SocketEstablishResponse.newBuilder()
								.setResult(SocketEstablishResponse.Result.FAIL_SERVER_EXCEPTION)
								.setFailText("身份验证服务处理异常")
								.build())
						.build()).addListener(ChannelFutureListener.CLOSE);
			}
			
			@Override
			public void onSuccess(VerifySessionKeyResponse verifyResponse) {
				if (verifyResponse.getResult() != VerifySessionKeyResponse.Result.SUCC) {
					SocketEstablishResponse.Result result;
					String failText;
					switch(verifyResponse.getResult()) {
						case FAIL_SESSION_DECRYPTION:
							result = SocketEstablishResponse.Result.FAIL_SESSION_DECRYPTION;
							failText = verifyResponse.getFailText();
							break;
						case FAIL_SESSION_EXPIRED:
							result = SocketEstablishResponse.Result.FAIL_SESSION_EXPIRED;
							failText = verifyResponse.getFailText();
							break;
						case FAIL_USER_NOT_EXSIT:
							result = SocketEstablishResponse.Result.FAIL_USER_DISABLE;
							failText = verifyResponse.getFailText();
							break;
						case FAIL_USER_DISABLE:
							result = SocketEstablishResponse.Result.FAIL_USER_DISABLE;
							failText = verifyResponse.getFailText();
							break;
						default:
							result = SocketEstablishResponse.Result.FAIL_SERVER_EXCEPTION;
							failText = "身份验证服务处理异常";
							break;
					}
					
					channel.writeAndFlush(SocketDownPacket.newBuilder()
							.setEstablishResponse(SocketEstablishResponse.newBuilder()
									.setResult(result)
									.setFailText(failText)
									.build())
							.build()).addListener(ChannelFutureListener.CLOSE);
					return;
				}
				
				final WeizhuProtos.Session session = verifyResponse.getSession();
				
				// 先close掉 所有该session 产生的连接
				ImmutableList<SocketRegistry.ChannelHolder> channelList = 
						SocketConnectionHandler.this.socketRegistry.get(session.getCompanyId(), session.getUserId());
				for (SocketRegistry.ChannelHolder holder : channelList) {
					if (holder.session().getCompanyId() == session.getCompanyId()
							&& holder.session().getUserId() == session.getUserId()
							&& holder.session().getSessionId() == session.getSessionId()) {
						holder.channel().close();
					}
				}
				
				SocketConnectionHandler.this.socketRegistry.register(session, 
						ImmutableSet.copyOf(establishRequest.getPushNameList()), channel);
				
				RequestHead head = buildRequestHead(headPrototype, ESTABLISH_INVOKE, session);

				GetOfflineMsgRequest getOfflineMsgRequest = GetOfflineMsgRequest.newBuilder()
						.setPushSeq(establishRequest.getPushSeq())
						.build();
				
				ListenableFuture<GetOfflineMsgResponse> getOfflineFuture = null;
				try {
					getOfflineFuture = SocketConnectionHandler.this.pushService.getOfflineMsg(head, getOfflineMsgRequest);
				} catch (Throwable th) {
					getOfflineFuture = Futures.immediateFailedFuture(th);
				}
				
				Futures.addCallback(getOfflineFuture, new FutureCallback<GetOfflineMsgResponse>() {

					@Override
					public void onFailure(Throwable t) {
						SocketConnectionHandler.this.socketRegistry.unregister(session, channel);
						channel.writeAndFlush(SocketDownPacket.newBuilder()
								.setEstablishResponse(SocketEstablishResponse.newBuilder()
										.setResult(SocketEstablishResponse.Result.FAIL_SERVER_EXCEPTION)
										.setFailText("获取离线消息处理异常")
										.build())
								.build()).addListener(ChannelFutureListener.CLOSE);
					}
					
					@Override
					public void onSuccess(GetOfflineMsgResponse getOfflineMsgResponse) {
						SocketEstablishResponse.Builder responseBuilder = SocketEstablishResponse.newBuilder()
								.setResult(SocketEstablishResponse.Result.SUCC);
						// 客户端传上来的push seq 比server存储的还大，有问题，需要重置
						if (establishRequest.getPushSeq() > getOfflineMsgResponse.getPushSeq()) {
							responseBuilder.setResetPushSeq(getOfflineMsgResponse.getPushSeq());
						}
						
						channel.writeAndFlush(SocketDownPacket.newBuilder()
								.setEstablishResponse(responseBuilder.build())
								.build(), channel.voidPromise());
						
						channel.write(new SocketEstablishResult(
								session, 
								getOfflineMsgResponse.getOfflineMsgList()), 
								channel.voidPromise());
					}
					
				});
				
			}
			
		});
	}
	
	private void processApiRequest(ChannelHandlerContext ctx, final SocketApiRequest apiRequest) {
		logApiRequest(ctx, apiRequest);
		
		if (headPrototype == null || session == null) {
			ctx.writeAndFlush(SocketDownPacket.newBuilder()
					.setApiResponse(SocketApiResponse.newBuilder()
						.setResult(SocketApiResponse.Result.FAIL_ESTABLISH_INVALID)
						.setFailText("连接建立不正确")
						.build())
					.build()).addListener(ChannelFutureListener.CLOSE);
			return;
		}
		
		final WeizhuProtos.Invoke invoke = apiRequest.getInvoke();
		final ServiceInvoker serviceInvoker = serviceInvokerMap.get(invoke.getServiceName());
		if (serviceInvoker == null) {
			ctx.writeAndFlush(SocketDownPacket.newBuilder()
					.setApiResponse(SocketApiResponse.newBuilder()
						.setInvoke(invoke)
						.setResult(SocketApiResponse.Result.FAIL_INVOKE_UNKNOWN)
						.setFailText("调用服务名或函数名未找到")
						.build())
					.build(), ctx.voidPromise());
			return;
		}
		
		final long begin = System.currentTimeMillis();
		final RequestHead head = buildRequestHead(headPrototype, invoke, session);
		
		ListenableFuture<ByteString> responseFuture = null;
		try {
			responseFuture = serviceInvoker.invoke(invoke.getFunctionName(), head, apiRequest.getRequestBody());
		} catch (Throwable th) {
			responseFuture = Futures.immediateFailedFuture(th);
		}
		
		final Channel channel = ctx.channel();
		Futures.addCallback(responseFuture, new FutureCallback<ByteString>() {

			@Override
			public void onSuccess(ByteString responseBody) {
				SocketApiResponse socketApiResponse = SocketApiResponse.newBuilder()
						.setInvoke(invoke)
						.setResult(SocketApiResponse.Result.SUCC)
						.setResponseBody(responseBody)
						.build();
				
				long time = System.currentTimeMillis() - begin;
				try {
					LogUtil.logApiAccess(head, apiRequest.getRequestBody().size(), 
							socketApiResponse.getResult().name(), null, socketApiResponse.getResponseBody().size(), 
							time, null);
				} catch (Throwable th) {
					logger.warn("log access print fail", th);
				}
				
				channel.writeAndFlush(SocketDownPacket.newBuilder()
						.setApiResponse(socketApiResponse)
						.build(), channel.voidPromise());
			}

			@Override
			public void onFailure(Throwable t) {
				SocketApiResponse socketApiResponse;
				
				if (t instanceof InvokeUnknownException) {
					socketApiResponse = SocketApiResponse.newBuilder()
							.setInvoke(invoke)
							.setResult(SocketApiResponse.Result.FAIL_INVOKE_UNKNOWN)
							.setFailText("调用服务名或函数名未找到")
							.build();
				} else if (t instanceof HeadUnknownException) {
					socketApiResponse = SocketApiResponse.newBuilder()
							.setInvoke(invoke)
							.setResult(SocketApiResponse.Result.FAIL_INVOKE_UNKNOWN)
							.setFailText("调用服务名或函数名未找到")
							.build();
				} else if (t instanceof RequestParseException) {
					socketApiResponse = SocketApiResponse.newBuilder()
							.setInvoke(invoke)
							.setResult(SocketApiResponse.Result.FAIL_BODY_PARSE_FAIL)
							.setFailText("请求数据包解析失败")
							.build();
				} else {
					socketApiResponse = SocketApiResponse.newBuilder()
							.setInvoke(invoke)
							.setResult(SocketApiResponse.Result.FAIL_SERVER_EXCEPTION)
							.setFailText("服务器处理错误")
							.build();
				}
				
				long time = System.currentTimeMillis() - begin;
				try {
					LogUtil.logApiAccess(head, apiRequest.getRequestBody().size(), 
							socketApiResponse.getResult().name(), socketApiResponse.getFailText(), socketApiResponse.getResponseBody().size(), 
							time, t);
				} catch (Throwable th) {
					logger.warn("log access print fail", th);
				}
				
				channel.writeAndFlush(SocketDownPacket.newBuilder()
						.setApiResponse(socketApiResponse)
						.build(), channel.voidPromise());
			}
			
		});
	}
	
	private void processPushAck(ChannelHandlerContext ctx, SocketPushAck pushAck) {
		logPushAck(ctx, pushAck);
		
		while (!pushingSeqQueue.isEmpty() && pushingSeqQueue.peek() <= pushAck.getPushSeq()) {
			pushingSeqQueue.poll();
		}
		doPush(ctx);
	}
	
	private void doPush(ChannelHandlerContext ctx) {
		if (headPrototype == null || session == null) {
			return;
		}
		
		while (pushingSeqQueue.size() < MAX_PUSHING_SIZE && !pushMsgList.isEmpty()) {
			WeizhuProtos.PushMessage pushMsg = pushMsgList.pollFirst();
			boolean hasMore = !pushMsgList.isEmpty();
			pushingSeqQueue.offer(pushMsg.getPushSeq());
			ctx.writeAndFlush(SocketDownPacket.newBuilder()
					.setPushMsg(SocketPushMsg.newBuilder()
							.setPushMsg(pushMsg)
							.setHasMore(hasMore)
							.build())
					.build(), ctx.voidPromise());
			
			logPushMsg(ctx, pushMsg);
		}
	}
	
	private static final WeizhuProtos.Invoke ESTABLISH_INVOKE = WeizhuProtos.Invoke.newBuilder()
			.setInvokeId(0)
			.setServiceName("SocketConnection")
			.setFunctionName("socketEstablish")
			.build();
	
	private static class SocketEstablishResult {
		final WeizhuProtos.Session session;
		final List<WeizhuProtos.PushMessage> offlineMsgList;
		
		SocketEstablishResult(WeizhuProtos.Session session, List<WeizhuProtos.PushMessage> offlineMsgList) {
			this.session = session;
			this.offlineMsgList = offlineMsgList;
		}
	}
	
	private void log(ChannelHandlerContext ctx, String event, Throwable th) {
		StringBuilder sb = new StringBuilder();
		sb.append(event).append("|");
		sb.append(ctx.channel().remoteAddress()).append("|");
		if (session != null) {
			sb.append(session.getCompanyId()).append("/");
			sb.append(session.getUserId()).append("/");
			sb.append(session.getSessionId()).append("|");
		} else {
			sb.append("-|");
		}
		if (th == null) {
			logger.info(sb.toString());
		} else {
			logger.warn(sb.toString(), th);
		}
	}
	
	private void logEstablishRequest(ChannelHandlerContext ctx, SocketEstablishRequest establishRequest) {
		StringBuilder sb = new StringBuilder();
		sb.append("processEstablishRequest|").append(ctx.channel().remoteAddress()).append("|-|");
		sb.append(establishRequest.getWeizhu().getPlatform()).append("/");
		sb.append(establishRequest.getWeizhu().getVersionName()).append("/");
		sb.append(establishRequest.getWeizhu().getVersionCode()).append("/");
		sb.append(establishRequest.getWeizhu().getStage()).append("/");
		sb.append(establishRequest.getWeizhu().getBuildTime()).append("|");
		if (establishRequest.hasAndroid()) {
			WeizhuProtos.Android android = establishRequest.getAndroid();
			sb.append("Android/");
			sb.append(android.getDevice()).append("/");
			sb.append(android.getManufacturer()).append("/");
			sb.append(android.getBrand()).append("/");
			sb.append(android.getModel()).append("/");
			sb.append(android.getSerial()).append("/");
			sb.append(android.getRelease()).append("/");
			sb.append(android.getSdkInt()).append("/");
			sb.append(android.getCodename()).append("|");
		}
		if (establishRequest.hasIphone()) {
			WeizhuProtos.Iphone iphone = establishRequest.getIphone();
			sb.append("|Iphone/");
			sb.append(iphone.getName()).append("/");
			sb.append(iphone.getSystemName()).append("/");
			sb.append(iphone.getSystemVersion()).append("/");
			sb.append(iphone.getModel()).append("/");
			sb.append(iphone.getLocalizedModel()).append("/");
			sb.append(iphone.getDeviceToken()).append("/");
			sb.append(iphone.getMac()).append("|");
		}
		
		sb.append("push_seq:").append(establishRequest.getPushSeq()).append("|");
		sb.append("push_name:").append(establishRequest.getPushNameList()).append("|");
		
		logger.info(sb.toString());
	}
	
	private void logApiRequest(ChannelHandlerContext ctx, SocketApiRequest apiRequest) {
		StringBuilder sb = new StringBuilder();
		sb.append("processApiRequest|").append(ctx.channel().remoteAddress()).append("|");
		if (session != null) {
			sb.append(session.getCompanyId()).append("/");
			sb.append(session.getUserId()).append("/");
			sb.append(session.getSessionId()).append("|");
		} else {
			sb.append("-|");
		}
		sb.append(apiRequest.getInvoke().getServiceName()).append("|");
		sb.append(apiRequest.getInvoke().getFunctionName()).append("|");
		sb.append(apiRequest.getInvoke().getInvokeId()).append("|");
		logger.info(sb.toString());
	}
	
	private void logPushAck(ChannelHandlerContext ctx, SocketPushAck pushAck) {
		StringBuilder sb = new StringBuilder();
		sb.append("processPushAck|").append(ctx.channel().remoteAddress()).append("|");
		if (session != null) {
			sb.append(session.getCompanyId()).append("/");
			sb.append(session.getUserId()).append("/");
			sb.append(session.getSessionId()).append("|");
		} else {
			sb.append("-|");
		}
		sb.append(pushAck.getPushSeq()).append("|");
		logger.info(sb.toString());
	}
	
	private void logPushMsg(ChannelHandlerContext ctx, WeizhuProtos.PushMessage pushMsg) {
		StringBuilder sb = new StringBuilder();
		sb.append("doPushMsg|").append(ctx.channel().remoteAddress()).append("|");
		if (session != null) {
			sb.append(session.getCompanyId()).append("/");
			sb.append(session.getUserId()).append("/");
			sb.append(session.getSessionId()).append("|");
		} else {
			sb.append("-|");
		}
		sb.append(pushMsg.getPushSeq()).append("|");
		sb.append(pushMsg.getPushName()).append("|");
		sb.append(pushMsg.getPushBody().size()).append("(bytes)|");
		logger.info(sb.toString());
	}
	
	private static RequestHead buildRequestHead(RequestHead prototype, WeizhuProtos.Invoke invoke, WeizhuProtos.Session session) {
		return RequestHead.newBuilder(prototype).setInvoke(invoke).setSession(session).build();
	}
	
	private static AnonymousHead buildAnonymousHead(RequestHead prototype, WeizhuProtos.Invoke invoke) {
		AnonymousHead.Builder anonymousHeadBuilder = AnonymousHead.newBuilder()
				.setInvoke(invoke)
				.setNetwork(prototype.getNetwork());
		if (prototype.hasWeizhu()) {
			anonymousHeadBuilder.setWeizhu(prototype.getWeizhu());
		}
		if (prototype.hasAndroid()) {
			anonymousHeadBuilder.setAndroid(prototype.getAndroid());
		}
		if (prototype.hasIphone()) {
			anonymousHeadBuilder.setIphone(prototype.getIphone());
		}
		return anonymousHeadBuilder.build();
	}
	
}
