package com.weizhu.proto;

import com.google.common.util.concurrent.ListenableFuture;
import com.weizhu.proto.AdminProtos.AdminHead;
import com.weizhu.proto.ConnProtos.CloseConnectionExpireRequest;
import com.weizhu.proto.ConnProtos.CloseConnectionRequest;
import com.weizhu.proto.ConnProtos.CloseConnectionResponse;
import com.weizhu.proto.ConnProtos.GetOnlineStatusRequest;
import com.weizhu.proto.ConnProtos.GetOnlineStatusResponse;
import com.weizhu.proto.ConnProtos.SendMessageRequest;
import com.weizhu.proto.ConnProtos.SendMessageResponse;
import com.weizhu.proto.WeizhuProtos.EmptyRequest;
import com.weizhu.proto.WeizhuProtos.EmptyResponse;
import com.weizhu.proto.WeizhuProtos.RequestHead;
import com.weizhu.proto.WeizhuProtos.SystemHead;

public interface ConnService {

	@ResponseType(SendMessageResponse.class)
	ListenableFuture<SendMessageResponse> sendMessage(RequestHead head, SendMessageRequest request);
	
	@ResponseType(SendMessageResponse.class)
	ListenableFuture<SendMessageResponse> sendMessage(AdminHead head, SendMessageRequest request);
	
	@ResponseType(SendMessageResponse.class)
	ListenableFuture<SendMessageResponse> sendMessage(SystemHead head, SendMessageRequest request);
	
	@ResponseType(GetOnlineStatusResponse.class)
	ListenableFuture<GetOnlineStatusResponse> getOnlineStatus(AdminHead head, GetOnlineStatusRequest request);
	
	@ResponseType(CloseConnectionResponse.class)
	ListenableFuture<CloseConnectionResponse> closeConnection(AdminHead head, CloseConnectionRequest request);
	
	@ResponseType(EmptyResponse.class)
	ListenableFuture<EmptyResponse> closeConnectionExpire(RequestHead head, CloseConnectionExpireRequest request);
	
	@ResponseType(EmptyResponse.class)
	ListenableFuture<EmptyResponse> closeConnectionLogout(RequestHead head, EmptyRequest request);
	
}
