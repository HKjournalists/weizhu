package com.weizhu.server.conn;

import javax.annotation.Nullable;

import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.inject.Inject;
import com.weizhu.common.service.ServiceUtil;
import com.weizhu.proto.AdminProtos.AdminHead;
import com.weizhu.proto.ConnProtos.CloseConnectionExpireRequest;
import com.weizhu.proto.ConnProtos.CloseConnectionRequest;
import com.weizhu.proto.ConnProtos.CloseConnectionResponse;
import com.weizhu.proto.ConnProtos.GetOnlineStatusRequest;
import com.weizhu.proto.ConnProtos.GetOnlineStatusResponse;
import com.weizhu.proto.ConnProtos.SendMessageRequest;
import com.weizhu.proto.ConnProtos.SendMessageResponse;
import com.weizhu.proto.ConnService;
import com.weizhu.proto.PushProtos;
import com.weizhu.proto.WeizhuProtos;
import com.weizhu.proto.WeizhuProtos.EmptyRequest;
import com.weizhu.proto.WeizhuProtos.EmptyResponse;
import com.weizhu.proto.WeizhuProtos.RequestHead;
import com.weizhu.proto.WeizhuProtos.SystemHead;
import com.weizhu.server.conn.SocketRegistry.ChannelHolder;

public class ConnServiceImpl implements ConnService {

	private final SocketRegistry socketRegistry;
	
	@Inject
	public ConnServiceImpl(SocketRegistry socketRegistry) {
		this.socketRegistry = socketRegistry;
	}
	
	@Override
	public ListenableFuture<SendMessageResponse> sendMessage(RequestHead head, SendMessageRequest request) {
		return Futures.immediateFuture(this.doSendMessage(head, null, null, request));
	}
	
	@Override
	public ListenableFuture<SendMessageResponse> sendMessage(AdminHead head, SendMessageRequest request) {
		return Futures.immediateFuture(this.doSendMessage(null, head, null, request));
	}
	
	@Override
	public ListenableFuture<SendMessageResponse> sendMessage(SystemHead head, SendMessageRequest request) {
		return Futures.immediateFuture(this.doSendMessage(null, null, head, request));
	}
	
	private SendMessageResponse doSendMessage(
			@Nullable RequestHead requestHead, @Nullable AdminHead adminHead, @Nullable SystemHead systemHead, 
			SendMessageRequest request) {
		if (requestHead == null && adminHead == null && systemHead == null) {
			throw new RuntimeException("no head");
		}
		
		if (request.getPushPacketCount() <= 0) {
			return SendMessageResponse.newBuilder().build();
		}
		
		final Long companyId;
		if (requestHead != null) {
			companyId = requestHead.getSession().getCompanyId();
		} else if (adminHead != null) {
			companyId = adminHead.hasCompanyId() ? adminHead.getCompanyId() : null;
		} else if (systemHead != null) {
			companyId = systemHead.hasCompanyId() ? systemHead.getCompanyId() : null;
		} else {
			companyId = null;
		}
		
		if (companyId == null) {
			throw new RuntimeException("cannot find company id");
		}
		
		SendMessageResponse.Builder responseBuilder = SendMessageResponse.newBuilder();
		
		PushProtos.PushSession.Builder tmpPushSessionBuilder = PushProtos.PushSession.newBuilder();
		WeizhuProtos.PushMessage.Builder tmpPushMessageBuilder = WeizhuProtos.PushMessage.newBuilder();
		for (int i=0; i<request.getPushPacketCount(); ++i) {
			final PushProtos.PushPacket packet = request.getPushPacket(i);
			
			tmpPushMessageBuilder.clear().setPushName(packet.getPushName()).setPushBody(packet.getPushBody());
			
			for (int j=0; j<packet.getPushTargetCount(); ++j) {
				final PushProtos.PushTarget target = packet.getPushTarget(j);
				
				ImmutableList<ChannelHolder> channelList = socketRegistry.get(companyId, target.getUserId());
				if (!channelList.isEmpty()) {
					final WeizhuProtos.PushMessage pushMessage = tmpPushMessageBuilder.setPushSeq(target.getPushSeq()).build();
					tmpPushMessageBuilder.clearPushSeq();
					
					for (int k=0; k<channelList.size(); ++k) {
						final ChannelHolder h = channelList.get(k);
						if (h.pushNameSet().contains(pushMessage.getPushName()) 
								&& (target.getIncludeSessionIdCount() <= 0 || target.getIncludeSessionIdList().contains(h.session().getSessionId()))
								&& !target.getExcludeSessionIdList().contains(h.session().getSessionId())
								) {
							h.channel().writeAndFlush(pushMessage, h.channel().voidPromise());
							
							responseBuilder.addPushSession(
									tmpPushSessionBuilder.clear()
									.setPacketIdx(i)
									.setTargetIdx(j)
									.setSession(h.session())
									.build());
						}
					}
				}
			}
		}
		
		return responseBuilder.build();
	}

	@Override
	public ListenableFuture<GetOnlineStatusResponse> getOnlineStatus(AdminHead head, GetOnlineStatusRequest request) {
		if (!head.hasCompanyId() || request.getUserIdCount() <= 0) {
			return Futures.immediateFuture(GetOnlineStatusResponse.newBuilder().build());
		}
		
		GetOnlineStatusResponse.Builder responseBuilder = GetOnlineStatusResponse.newBuilder();
		
		for (Long userId : request.getUserIdList()) {
			ImmutableList<ChannelHolder> channelList = socketRegistry.get(head.getCompanyId(), userId);
			if (!channelList.isEmpty()) {
				for (int j=0; j<channelList.size(); ++j) {
					ChannelHolder h = channelList.get(j);
					responseBuilder.addOnlineSession(h.session());
				}
			}
		}
		
		return Futures.immediateFuture(responseBuilder.build());
	}

	@Override
	public ListenableFuture<CloseConnectionResponse> closeConnection(AdminHead head, CloseConnectionRequest request) {
		if (!head.hasCompanyId() || (request.getUserIdCount() <= 0 && request.getSessionCount() <= 0)) {
			return Futures.immediateFuture(CloseConnectionResponse.newBuilder().build());
		}
		
		// check session company id
		for (WeizhuProtos.Session session : request.getSessionList()) {
			if (session.getCompanyId() != head.getCompanyId()) {
				return Futures.immediateFailedFuture(new RuntimeException("invalid companyId : " + session.getCompanyId() + ", expect : " + head.getCompanyId()));
			}
		}
		
		CloseConnectionResponse.Builder responseBuilder = CloseConnectionResponse.newBuilder();
		
		for (Long userId : request.getUserIdList()) {
			ImmutableList<ChannelHolder> channelList = socketRegistry.get(head.getCompanyId(), userId);
			for (ChannelHolder h : channelList) {
				h.channel().close();
				responseBuilder.addCloseSession(h.session());
			}
		}
		
		for (WeizhuProtos.Session session : request.getSessionList()) {
			ImmutableList<ChannelHolder> channelList = socketRegistry.get(head.getCompanyId(), session.getUserId());
			for (ChannelHolder h : channelList) {
				if (h.session().equals(session)) {
					h.channel().close();
					responseBuilder.addCloseSession(h.session());
				}
			}
		}
		
		return Futures.immediateFuture(responseBuilder.build());
	}
	
	@Override
	public ListenableFuture<EmptyResponse> closeConnectionExpire(RequestHead head, CloseConnectionExpireRequest request) {
		if (request.getExpireSessionIdCount() <= 0) {
			return ServiceUtil.EMPTY_RESPONSE_IMMEDIATE_FUTURE;
		}
		
		ImmutableList<ChannelHolder> channelList = socketRegistry.get(head.getSession().getCompanyId(), head.getSession().getUserId());
		for (ChannelHolder h : channelList) {
			if (h.session().getCompanyId() == head.getSession().getCompanyId()
					&& h.session().getUserId() == head.getSession().getUserId()
					&& request.getExpireSessionIdList().contains(h.session().getSessionId())) {
				h.channel().close();
			}
		}
		return ServiceUtil.EMPTY_RESPONSE_IMMEDIATE_FUTURE;
	}

	@Override
	public ListenableFuture<EmptyResponse> closeConnectionLogout(RequestHead head, EmptyRequest request) {
		ImmutableList<ChannelHolder> channelList = socketRegistry.get(head.getSession().getCompanyId(), head.getSession().getUserId());
		for (ChannelHolder h : channelList) {
			if (h.session().equals(head.getSession())) {
				h.channel().close();
			}
		}
		return ServiceUtil.EMPTY_RESPONSE_IMMEDIATE_FUTURE;
	}

}
