package com.weizhu.proto;

import com.google.common.util.concurrent.ListenableFuture;
import com.weizhu.proto.AdminComponentProtos.CreateScoreRequest;
import com.weizhu.proto.AdminComponentProtos.CreateScoreResponse;
import com.weizhu.proto.AdminComponentProtos.GetScoreByIdRequest;
import com.weizhu.proto.AdminComponentProtos.GetScoreByIdResponse;
import com.weizhu.proto.AdminComponentProtos.GetScoreListRequest;
import com.weizhu.proto.AdminComponentProtos.GetScoreListResponse;
import com.weizhu.proto.AdminComponentProtos.UpdateScoreRequest;
import com.weizhu.proto.AdminComponentProtos.UpdateScoreResponse;
import com.weizhu.proto.AdminComponentProtos.UpdateScoreStateRequest;
import com.weizhu.proto.AdminComponentProtos.UpdateScoreStateResponse;
import com.weizhu.proto.AdminProtos.AdminHead;

public interface AdminComponentService {

	@ResponseType(GetScoreByIdResponse.class)
	ListenableFuture<GetScoreByIdResponse> getScoreById(AdminHead head,GetScoreByIdRequest request);
	
	@ResponseType(GetScoreListResponse.class)
	ListenableFuture<GetScoreListResponse> getScoreList(AdminHead head,GetScoreListRequest request);
	
	@WriteMethod
	@ResponseType(CreateScoreResponse.class)
	ListenableFuture<CreateScoreResponse> createScore(AdminHead head,CreateScoreRequest request);
	
	@WriteMethod
	@ResponseType(UpdateScoreResponse.class)
	ListenableFuture<UpdateScoreResponse> updateScore(AdminHead head,UpdateScoreRequest request);
	
	@WriteMethod
	@ResponseType(UpdateScoreStateResponse.class)
	ListenableFuture<UpdateScoreStateResponse> updateScoreState(AdminHead head,UpdateScoreStateRequest request);
}
