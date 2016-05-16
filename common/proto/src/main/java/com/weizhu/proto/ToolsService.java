package com.weizhu.proto;

import com.google.common.util.concurrent.ListenableFuture;
import com.weizhu.proto.ToolsProtos.GetInfoHomeRequest;
import com.weizhu.proto.ToolsProtos.GetInfoHomeResponse;
import com.weizhu.proto.ToolsProtos.GetInfoItemContentRequest;
import com.weizhu.proto.ToolsProtos.GetInfoItemContentResponse;
import com.weizhu.proto.WeizhuProtos.RequestHead;

public interface ToolsService {

	@ResponseType(GetInfoHomeResponse.class)
	ListenableFuture<GetInfoHomeResponse> getInfoHome(RequestHead head, GetInfoHomeRequest request);

	@ResponseType(GetInfoItemContentResponse.class)
	ListenableFuture<GetInfoItemContentResponse> getInfoItemContent(RequestHead head, GetInfoItemContentRequest request);
	
}
