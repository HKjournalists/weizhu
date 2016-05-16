package com.weizhu.service.webrtc;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.inject.Inject;
import com.weizhu.common.service.ServiceUtil;
import com.weizhu.proto.PushProtos;
import com.weizhu.proto.PushService;
import com.weizhu.proto.UserProtos;
import com.weizhu.proto.UserService;
import com.weizhu.proto.WebRTCProtos;
import com.weizhu.proto.WebRTCProtos.AnswerCallRequest;
import com.weizhu.proto.WebRTCProtos.HangUpCallRequest;
import com.weizhu.proto.WebRTCProtos.MakeCallRequest;
import com.weizhu.proto.WebRTCProtos.MakeCallResponse;
import com.weizhu.proto.WebRTCProtos.UpdateIceCandidateRequest;
import com.weizhu.proto.WebRTCService;
import com.weizhu.proto.WeizhuProtos.EmptyResponse;
import com.weizhu.proto.WeizhuProtos.RequestHead;

public class WebRTCServiceImpl implements WebRTCService {

	private final UserService userService;
	private final PushService pushService;
	
	@Inject
	public WebRTCServiceImpl(UserService userService, PushService pushService) {
		this.userService = userService;
		this.pushService = pushService;
	}
	
	@Override
	public ListenableFuture<EmptyResponse> updateIceCandidate(RequestHead head, UpdateIceCandidateRequest request) {
		if (request.getIceCandidateCount() <= 0 ) {
			return ServiceUtil.EMPTY_RESPONSE_IMMEDIATE_FUTURE;
		}
		
		this.pushService.pushMsg(head, PushProtos.PushMsgRequest.newBuilder()
				.addPushPacket(PushProtos.PushPacket.newBuilder()
						.addPushTarget(PushProtos.PushTarget.newBuilder()
								.setUserId(request.getUserId())
								.addIncludeSessionId(request.getSessionId())
								.setEnableOffline(false) // 不需要离线
								.build())
						.setPushName("WebRTCIceCandidateMessagePush")
						.setPushBody(WebRTCProtos.WebRTCIceCandidateMessagePush.newBuilder()
								.setUserId(head.getSession().getUserId())
								.setSessionId(head.getSession().getSessionId())
								.addAllIceCandidate(request.getIceCandidateList())
								.build().toByteString())
						.build())
				.build());
		return ServiceUtil.EMPTY_RESPONSE_IMMEDIATE_FUTURE;
	}

	@Override
	public ListenableFuture<MakeCallResponse> makeCall(RequestHead head, MakeCallRequest request) {
		
		UserProtos.GetUserResponse getUserRsp = Futures.getUnchecked(
				this.userService.getUserById(head, UserProtos.GetUserByIdRequest.newBuilder()
						.addUserId(request.getUserId())
						.build()));
		
		UserProtos.User user = null;
		for (UserProtos.User u : getUserRsp.getUserList()) {
			if (u.getBase().getUserId() == request.getUserId()) {
				user = u;
				break;
			}
		}
		
		if (user == null) {
			return Futures.immediateFuture(MakeCallResponse.newBuilder()
					.setResult(MakeCallResponse.Result.FAIL_USER_NOT_EXIST)
					.setFailText("该用户不存在")
					.build());
		}
		
		if (user.getBase().getState() == UserProtos.UserBase.State.DISABLE) {
			return Futures.immediateFuture(MakeCallResponse.newBuilder()
					.setResult(MakeCallResponse.Result.FAIL_USER_NOT_EXIST)
					.setFailText("该用户已被禁用")
					.build());
		}
		
		this.pushService.pushMsg(head, PushProtos.PushMsgRequest.newBuilder()
				.addPushPacket(PushProtos.PushPacket.newBuilder()
						.addPushTarget(PushProtos.PushTarget.newBuilder()
								.setUserId(request.getUserId())
								.setEnableOffline(false) // 不需要离线
								.build())
						.setPushName("WebRTCIncomingCallMessagePush")
						.setPushBody(WebRTCProtos.WebRTCIncomingCallMessagePush.newBuilder()
								.setUserId(head.getSession().getUserId())
								.setSessionId(head.getSession().getSessionId())
								.setEnableVideo(request.getEnableVideo())
								.setOfferSdp(request.getOfferSdp())
								.build().toByteString())
						.build())
				.build());
		
		return Futures.immediateFuture(MakeCallResponse.newBuilder()
				.setResult(MakeCallResponse.Result.SUCC)
				.build());
	}

	@Override
	public ListenableFuture<EmptyResponse> answerCall(RequestHead head, AnswerCallRequest request) {
		
		this.pushService.pushMsg(head, PushProtos.PushMsgRequest.newBuilder()
				 // 向call发起人推送接听消息
				.addPushPacket(PushProtos.PushPacket.newBuilder()
						.addPushTarget(PushProtos.PushTarget.newBuilder()
								.setUserId(request.getUserId())
								.addIncludeSessionId(request.getSessionId())
								.setEnableOffline(false) // 不需要离线
								.build())
						.setPushName("WebRTCAnswerCallMessagePush")
						.setPushBody(WebRTCProtos.WebRTCAnswerCallMessagePush.newBuilder()
								.setUserId(head.getSession().getUserId())
								.setSessionId(head.getSession().getSessionId())
								.setAnswerSdp(request.getAnswerSdp())
								.build().toByteString())
						.build())
				// 向自己的其他会话推送挂断消息
				.addPushPacket(PushProtos.PushPacket.newBuilder()
						.addPushTarget(PushProtos.PushTarget.newBuilder()
								.setUserId(head.getSession().getUserId())
								.addExcludeSessionId(head.getSession().getSessionId())
								.setEnableOffline(false) // 不需要离线
								.build())
						.setPushName("WebRTCHangUpCallMessagePush")
						.setPushBody(WebRTCProtos.WebRTCHangUpCallMessagePush.newBuilder()
								.setUserId(request.getUserId())
								.setSessionId(request.getSessionId())
								.setHangUpMsg("该通话已被其他会话接听")
								.build().toByteString())
						.build())
				.build());
		
		return ServiceUtil.EMPTY_RESPONSE_IMMEDIATE_FUTURE;
	}

	@Override
	public ListenableFuture<EmptyResponse> hangUpCall(RequestHead head, HangUpCallRequest request) {
		PushProtos.PushMsgRequest pushRequest;
		
		if (!request.hasSessionId()) {
			pushRequest = PushProtos.PushMsgRequest.newBuilder()
					// 向对方所有会话推送挂断消息
					.addPushPacket(PushProtos.PushPacket.newBuilder()
							.addPushTarget(PushProtos.PushTarget.newBuilder()
									.setUserId(request.getUserId())
									.setEnableOffline(false) // 不需要离线
									.build())
							.setPushName("WebRTCHangUpCallMessagePush")
							.setPushBody(WebRTCProtos.WebRTCHangUpCallMessagePush.newBuilder()
									.setUserId(head.getSession().getUserId())
									.setSessionId(head.getSession().getSessionId())
									.setHangUpMsg("该通话已被对方挂断")
									.build().toByteString())
							.build())
					.build();
		} else {
			pushRequest = PushProtos.PushMsgRequest.newBuilder()
					// 向对方推送挂断消息
					.addPushPacket(PushProtos.PushPacket.newBuilder()
							.addPushTarget(PushProtos.PushTarget.newBuilder()
									.setUserId(request.getUserId())
									.addIncludeSessionId(request.getSessionId())
									.setEnableOffline(false) // 不需要离线
									.build())
							.setPushName("WebRTCHangUpCallMessagePush")
							.setPushBody(WebRTCProtos.WebRTCHangUpCallMessagePush.newBuilder()
									.setUserId(head.getSession().getUserId())
									.setSessionId(head.getSession().getSessionId())
									.setHangUpMsg("该通话已被对方挂断")
									.build().toByteString())
							.build())
					// 向自己的其他会话推送挂断消息
					.addPushPacket(PushProtos.PushPacket.newBuilder()
							.addPushTarget(PushProtos.PushTarget.newBuilder()
									.setUserId(head.getSession().getUserId())
									.addExcludeSessionId(head.getSession().getSessionId())
									.setEnableOffline(false) // 不需要离线
									.build())
							.setPushName("WebRTCHangUpCallMessagePush")
							.setPushBody(WebRTCProtos.WebRTCHangUpCallMessagePush.newBuilder()
									.setUserId(request.getUserId())
									.setSessionId(request.getSessionId())
									.setHangUpMsg("该通话已被其他会话挂断")
									.build().toByteString())
							.build())
					.build();
		}
		
		this.pushService.pushMsg(head, pushRequest);
		
		return ServiceUtil.EMPTY_RESPONSE_IMMEDIATE_FUTURE;
	}

}
