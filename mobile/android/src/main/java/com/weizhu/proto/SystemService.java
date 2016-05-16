package com.weizhu.proto;

import com.weizhu.network.Future;
import com.weizhu.proto.SystemProtos.CheckNewVersionResponse;
import com.weizhu.proto.SystemProtos.GetAuthUrlRequest;
import com.weizhu.proto.SystemProtos.GetAuthUrlResponse;
import com.weizhu.proto.SystemProtos.GetConfigResponse;
import com.weizhu.proto.SystemProtos.GetConfigV2Response;
import com.weizhu.proto.SystemProtos.GetUserConfigResponse;
import com.weizhu.proto.SystemProtos.SendFeedbackRequest;
import com.weizhu.proto.WeizhuProtos.EmptyRequest;
import com.weizhu.proto.WeizhuProtos.EmptyResponse;

public interface SystemService {

	@ResponseType(GetUserConfigResponse.class)
	Future<GetUserConfigResponse> getUserConfig(EmptyRequest request, int priorityNum);
	
	@ResponseType(CheckNewVersionResponse.class)
	Future<CheckNewVersionResponse> checkNewVersion(EmptyRequest request, int priorityNum);
	
	@ResponseType(EmptyResponse.class)
	Future<EmptyResponse> sendFeedback(SendFeedbackRequest request, int priorityNum);
	
	@ResponseType(GetAuthUrlResponse.class)
	Future<GetAuthUrlResponse> getAuthUrl(GetAuthUrlRequest request, int priorityNum);
	
	
	@Deprecated
	@ResponseType(GetConfigResponse.class)
	Future<GetConfigResponse> getConfig(EmptyRequest request, int priorityNum);
	
	@Deprecated
	@ResponseType(GetConfigV2Response.class)
	Future<GetConfigV2Response> getConfigV2(EmptyRequest request, int priorityNum);
}
