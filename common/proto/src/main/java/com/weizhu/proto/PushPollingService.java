package com.weizhu.proto;

import com.google.common.util.concurrent.ListenableFuture;
import com.weizhu.proto.PushPollingProtos.GetPushMsgRequest;
import com.weizhu.proto.PushPollingProtos.GetPushMsgResponse;
import com.weizhu.proto.WeizhuProtos.RequestHead;

public interface PushPollingService {

	@ResponseType(GetPushMsgResponse.class)
	ListenableFuture<GetPushMsgResponse> getPushMsg(RequestHead head, GetPushMsgRequest request);
	
}
