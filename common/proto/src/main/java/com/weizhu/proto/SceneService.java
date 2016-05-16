package com.weizhu.proto;

import com.google.common.util.concurrent.ListenableFuture;
import com.weizhu.proto.SceneProtos.GetRecommenderHomeResponse;
import com.weizhu.proto.SceneProtos.GetRecommenderCompetitorProductRequest;
import com.weizhu.proto.SceneProtos.GetRecommenderCompetitorProductResponse;
import com.weizhu.proto.SceneProtos.GetRecommenderRecommendProductRequest;
import com.weizhu.proto.SceneProtos.GetRecommenderRecommendProductResponse;
import com.weizhu.proto.SceneProtos.GetSceneHomeResponse;
import com.weizhu.proto.SceneProtos.GetSceneItemRequest;
import com.weizhu.proto.SceneProtos.GetSceneItemResponse;
import com.weizhu.proto.WeizhuProtos.EmptyRequest;
import com.weizhu.proto.WeizhuProtos.RequestHead;

public interface SceneService {

	@ResponseType(GetSceneHomeResponse.class)
	ListenableFuture<GetSceneHomeResponse> getSceneHome(RequestHead head, EmptyRequest request);
	
	@ResponseType(GetSceneItemResponse.class)
	ListenableFuture<GetSceneItemResponse> getSceneItem(RequestHead head, GetSceneItemRequest request);
	
	// 工具——盖帽神器（超值推荐）
	@ResponseType(GetRecommenderHomeResponse.class)
	ListenableFuture<GetRecommenderHomeResponse> getRecommenderHome(RequestHead head, EmptyRequest request);

	@ResponseType(GetRecommenderCompetitorProductResponse.class)
	ListenableFuture<GetRecommenderCompetitorProductResponse> getRecommenderCompetitorProduct(RequestHead head, GetRecommenderCompetitorProductRequest request);

	@ResponseType(GetRecommenderRecommendProductResponse.class)
	ListenableFuture<GetRecommenderRecommendProductResponse> getRecommenderRecommendProduct(RequestHead head, GetRecommenderRecommendProductRequest request);

}
