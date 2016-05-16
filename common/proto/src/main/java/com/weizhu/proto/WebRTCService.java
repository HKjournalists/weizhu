package com.weizhu.proto;

import com.google.common.util.concurrent.ListenableFuture;
import com.weizhu.proto.WebRTCProtos.AnswerCallRequest;
import com.weizhu.proto.WebRTCProtos.HangUpCallRequest;
import com.weizhu.proto.WebRTCProtos.MakeCallRequest;
import com.weizhu.proto.WebRTCProtos.MakeCallResponse;
import com.weizhu.proto.WebRTCProtos.UpdateIceCandidateRequest;
import com.weizhu.proto.WeizhuProtos.EmptyResponse;
import com.weizhu.proto.WeizhuProtos.RequestHead;

public interface WebRTCService {

	@ResponseType(EmptyResponse.class)
	ListenableFuture<EmptyResponse> updateIceCandidate(RequestHead head, UpdateIceCandidateRequest request);
	
	@ResponseType(MakeCallResponse.class)
	ListenableFuture<MakeCallResponse> makeCall(RequestHead head, MakeCallRequest request);
	
	@ResponseType(EmptyResponse.class)
	ListenableFuture<EmptyResponse> answerCall(RequestHead head, AnswerCallRequest request);
	
	@ResponseType(EmptyResponse.class)
	ListenableFuture<EmptyResponse> hangUpCall(RequestHead head, HangUpCallRequest request);
	
}
