package com.weizhu.proto;

import com.weizhu.network.Future;
import com.weizhu.proto.PushPollingProtos.GetPushMsgRequest;
import com.weizhu.proto.PushPollingProtos.GetPushMsgResponse;

public interface PushPollingService {

	@ResponseType(GetPushMsgResponse.class)
	Future<GetPushMsgResponse> getPushMsg(GetPushMsgRequest request, int priorityNum);
	
}
