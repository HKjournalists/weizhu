package com.weizhu.proto;

import com.google.common.util.concurrent.ListenableFuture;
import com.weizhu.proto.AdminProtos.AdminHead;
import com.weizhu.proto.SessionProtos.CreateSessionKeyRequest;
import com.weizhu.proto.SessionProtos.CreateSessionKeyResponse;
import com.weizhu.proto.SessionProtos.CreateWebLoginSessionKeyResponse;
import com.weizhu.proto.SessionProtos.DeleteSessionDataRequest;
import com.weizhu.proto.SessionProtos.DeleteSessionDataResponse;
import com.weizhu.proto.SessionProtos.GetSessionDataRequest;
import com.weizhu.proto.SessionProtos.GetSessionDataResponse;
import com.weizhu.proto.SessionProtos.VerifySessionKeyRequest;
import com.weizhu.proto.SessionProtos.VerifySessionKeyResponse;
import com.weizhu.proto.SessionProtos.VerifyWebLoginSessionKeyRequest;
import com.weizhu.proto.SessionProtos.VerifyWebLoginSessionKeyResponse;
import com.weizhu.proto.WeizhuProtos.AnonymousHead;
import com.weizhu.proto.WeizhuProtos.EmptyRequest;
import com.weizhu.proto.WeizhuProtos.EmptyResponse;
import com.weizhu.proto.WeizhuProtos.RequestHead;

public interface SessionService {

	@WriteMethod
	@ResponseType(CreateSessionKeyResponse.class)
	ListenableFuture<CreateSessionKeyResponse> createSessionKey(AnonymousHead head, CreateSessionKeyRequest request);
	
	@WriteMethod
	@ResponseType(EmptyResponse.class)
	ListenableFuture<EmptyResponse> deleteSessionKey(RequestHead head, EmptyRequest request);
	
	@ResponseType(VerifySessionKeyResponse.class)
	ListenableFuture<VerifySessionKeyResponse> verifySessionKey(AnonymousHead head, VerifySessionKeyRequest request);
	
	@ResponseType(GetSessionDataResponse.class)
	ListenableFuture<GetSessionDataResponse> getSessionData(AdminHead head, GetSessionDataRequest request);
	
	@WriteMethod
	@ResponseType(DeleteSessionDataResponse.class)
	ListenableFuture<DeleteSessionDataResponse> deleteSessionData(AdminHead head, DeleteSessionDataRequest request);
	
	@WriteMethod
	@ResponseType(CreateWebLoginSessionKeyResponse.class)
	ListenableFuture<CreateWebLoginSessionKeyResponse> createWebLoginSessionKey(RequestHead head, EmptyRequest request);
	
	@WriteMethod
	@ResponseType(EmptyResponse.class)
	ListenableFuture<EmptyResponse> deleteWebLoginSessionKey(RequestHead head, EmptyRequest request);
	
	@ResponseType(VerifyWebLoginSessionKeyResponse.class)
	ListenableFuture<VerifyWebLoginSessionKeyResponse> verifyWebLoginSessionKey(AnonymousHead head, VerifyWebLoginSessionKeyRequest request);
}
