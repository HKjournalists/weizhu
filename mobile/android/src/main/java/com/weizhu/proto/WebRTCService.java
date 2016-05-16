package com.weizhu.proto;

import com.weizhu.network.Future;
import com.weizhu.proto.WebRTCProtos.AnswerCallRequest;
import com.weizhu.proto.WebRTCProtos.HangUpCallRequest;
import com.weizhu.proto.WebRTCProtos.MakeCallRequest;
import com.weizhu.proto.WebRTCProtos.MakeCallResponse;
import com.weizhu.proto.WebRTCProtos.UpdateIceCandidateRequest;
import com.weizhu.proto.WeizhuProtos.EmptyResponse;

public interface WebRTCService {

	@ResponseType(EmptyResponse.class)
	Future<EmptyResponse> updateIceCandidate(UpdateIceCandidateRequest request, int priorityNum);
	
	@ResponseType(MakeCallResponse.class)
	Future<MakeCallResponse> makeCall(MakeCallRequest request, int priorityNum);
	
	@ResponseType(EmptyResponse.class)
	Future<EmptyResponse> answerCall(AnswerCallRequest request, int priorityNum);
	
	@ResponseType(EmptyResponse.class)
	Future<EmptyResponse> hangUpCall(HangUpCallRequest request, int priorityNum);
	
}
