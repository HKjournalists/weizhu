package com.weizhu.proto;

import com.google.common.util.concurrent.ListenableFuture;
import com.weizhu.proto.BossProtos.BossAnonymousHead;
import com.weizhu.proto.BossProtos.BossHead;
import com.weizhu.proto.BossProtos.LoginRequest;
import com.weizhu.proto.BossProtos.LoginResponse;
import com.weizhu.proto.BossProtos.VerifySessionRequest;
import com.weizhu.proto.BossProtos.VerifySessionResponse;
import com.weizhu.proto.WeizhuProtos.EmptyRequest;
import com.weizhu.proto.WeizhuProtos.EmptyResponse;


public interface BossService {
	
	@ResponseType(VerifySessionResponse.class)
	ListenableFuture<VerifySessionResponse> verifySession(BossAnonymousHead head, VerifySessionRequest request);
		
	@ResponseType(LoginResponse.class)
	ListenableFuture<LoginResponse> login(BossAnonymousHead head, LoginRequest request);

	@ResponseType(EmptyResponse.class)
	ListenableFuture<EmptyResponse> logout(BossHead head, EmptyRequest request);

}
