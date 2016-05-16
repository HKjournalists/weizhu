package com.weizhu.proto;

import com.google.common.util.concurrent.ListenableFuture;
import com.weizhu.proto.AdminProtos.AdminHead;
import com.weizhu.proto.PushProtos.GetOfflineMsgRequest;
import com.weizhu.proto.PushProtos.GetOfflineMsgResponse;
import com.weizhu.proto.PushProtos.PushMsgRequest;
import com.weizhu.proto.PushProtos.PushStateRequest;
import com.weizhu.proto.PushProtos.PushUserDeleteRequest;
import com.weizhu.proto.PushProtos.PushUserDisableRequest;
import com.weizhu.proto.PushProtos.PushUserExpireRequest;
import com.weizhu.proto.WeizhuProtos.EmptyRequest;
import com.weizhu.proto.WeizhuProtos.EmptyResponse;
import com.weizhu.proto.WeizhuProtos.RequestHead;
import com.weizhu.proto.WeizhuProtos.SystemHead;

public interface PushService {
	
	@ResponseType(EmptyResponse.class)
	ListenableFuture<EmptyResponse> pushMsg(RequestHead head, PushMsgRequest request);
	
	@ResponseType(EmptyResponse.class)
	ListenableFuture<EmptyResponse> pushMsg(AdminHead head, PushMsgRequest request);
	
	@ResponseType(EmptyResponse.class)
	ListenableFuture<EmptyResponse> pushMsg(SystemHead head, PushMsgRequest request);

	@ResponseType(EmptyResponse.class)
	ListenableFuture<EmptyResponse> pushState(RequestHead head, PushStateRequest request);
	
	@ResponseType(EmptyResponse.class)
	ListenableFuture<EmptyResponse> pushState(AdminHead head, PushStateRequest request);
	
	@ResponseType(EmptyResponse.class)
	ListenableFuture<EmptyResponse> pushState(SystemHead head, PushStateRequest request);
	
	@ResponseType(GetOfflineMsgResponse.class)
	ListenableFuture<GetOfflineMsgResponse> getOfflineMsg(RequestHead head, GetOfflineMsgRequest request);
	
	@WriteMethod
	@ResponseType(EmptyResponse.class)
	ListenableFuture<EmptyResponse> pushUserDelete(AdminHead head, PushUserDeleteRequest request);
	
	@WriteMethod
	@ResponseType(EmptyResponse.class)
	ListenableFuture<EmptyResponse> pushUserDisable(AdminHead head, PushUserDisableRequest request);
	
	@WriteMethod
	@ResponseType(EmptyResponse.class)
	ListenableFuture<EmptyResponse> pushUserExpire(RequestHead head, PushUserExpireRequest request);
	
	@WriteMethod
	@ResponseType(EmptyResponse.class)
	ListenableFuture<EmptyResponse> pushUserLogout(RequestHead head, EmptyRequest request);
	
}
