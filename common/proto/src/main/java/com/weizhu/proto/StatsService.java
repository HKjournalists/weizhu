package com.weizhu.proto;

import com.google.common.util.concurrent.ListenableFuture;
import com.weizhu.proto.BossProtos.BossHead;
import com.weizhu.proto.WeizhuProtos.EmptyRequest;
import com.weizhu.proto.WeizhuProtos.EmptyResponse;

public interface StatsService {
	
	@ResponseType(EmptyResponse.class)
	ListenableFuture<EmptyResponse> loadDimDate(BossHead head, EmptyRequest request);
	
	@ResponseType(EmptyResponse.class)
	ListenableFuture<EmptyResponse> loadDimUser(BossHead head, EmptyRequest request);
	
	@ResponseType(EmptyResponse.class)
	ListenableFuture<EmptyResponse> loadDimDiscover(BossHead head, EmptyRequest request);
}
