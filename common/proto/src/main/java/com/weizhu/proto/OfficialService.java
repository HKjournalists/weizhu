package com.weizhu.proto;

import com.google.common.util.concurrent.ListenableFuture;
import com.weizhu.proto.OfficialProtos.GetOfficialByIdRequest;
import com.weizhu.proto.OfficialProtos.GetOfficialByIdResponse;
import com.weizhu.proto.OfficialProtos.GetOfficialListRequest;
import com.weizhu.proto.OfficialProtos.GetOfficialListResponse;
import com.weizhu.proto.OfficialProtos.GetOfficialMessageRequest;
import com.weizhu.proto.OfficialProtos.GetOfficialMessageResponse;
import com.weizhu.proto.OfficialProtos.SendOfficialMessageRequest;
import com.weizhu.proto.OfficialProtos.SendOfficialMessageResponse;
import com.weizhu.proto.WeizhuProtos.RequestHead;

public interface OfficialService {
	
	@ResponseType(GetOfficialByIdResponse.class)
	ListenableFuture<GetOfficialByIdResponse> getOfficialById(RequestHead head, GetOfficialByIdRequest request);
	
	@ResponseType(GetOfficialListResponse.class)
	ListenableFuture<GetOfficialListResponse> getOfficialList(RequestHead head, GetOfficialListRequest request);
	
	@ResponseType(GetOfficialMessageResponse.class)
	ListenableFuture<GetOfficialMessageResponse> getOfficialMessage(RequestHead head, GetOfficialMessageRequest request);
	
	@WriteMethod
	@ResponseType(SendOfficialMessageResponse.class)
	ListenableFuture<SendOfficialMessageResponse> sendOfficialMessage(RequestHead head, SendOfficialMessageRequest request);
}
