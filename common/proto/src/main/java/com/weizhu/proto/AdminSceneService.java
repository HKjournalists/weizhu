package com.weizhu.proto;

import com.google.common.util.concurrent.ListenableFuture;
import com.weizhu.proto.AdminProtos.AdminHead;
import com.weizhu.proto.AdminSceneProtos.CreateRecommenderCategoryRequest;
import com.weizhu.proto.AdminSceneProtos.CreateRecommenderCategoryResponse;
import com.weizhu.proto.AdminSceneProtos.CreateRecommenderCompetitorProductRequest;
import com.weizhu.proto.AdminSceneProtos.CreateRecommenderCompetitorProductResponse;
import com.weizhu.proto.AdminSceneProtos.CreateRecommenderRecommendProductPriceWebUrlRequest;
import com.weizhu.proto.AdminSceneProtos.CreateRecommenderRecommendProductPriceWebUrlResponse;
import com.weizhu.proto.AdminSceneProtos.CreateRecommenderRecommendProductRequest;
import com.weizhu.proto.AdminSceneProtos.CreateRecommenderRecommendProductResponse;
import com.weizhu.proto.AdminSceneProtos.CreateSceneItemRequest;
import com.weizhu.proto.AdminSceneProtos.CreateSceneItemResponse;
import com.weizhu.proto.AdminSceneProtos.CreateSceneRequest;
import com.weizhu.proto.AdminSceneProtos.CreateSceneResponse;
import com.weizhu.proto.AdminSceneProtos.DeleteRecommendProdFromCompetitorProdRequest;
import com.weizhu.proto.AdminSceneProtos.DeleteRecommendProdFromCompetitorProdResponse;
import com.weizhu.proto.AdminSceneProtos.DeleteRecommenderRecommendProductPriceWebUrlRequest;
import com.weizhu.proto.AdminSceneProtos.DeleteRecommenderRecommendProductPriceWebUrlResponse;
import com.weizhu.proto.AdminSceneProtos.GetRecommenderHomeResponse;
import com.weizhu.proto.AdminSceneProtos.GetRecommenderCompetitorProductRequest;
import com.weizhu.proto.AdminSceneProtos.GetRecommenderCompetitorProductResponse;
import com.weizhu.proto.AdminSceneProtos.GetRecommenderRecommendProductPriceWebUrlRequest;
import com.weizhu.proto.AdminSceneProtos.GetRecommenderRecommendProductPriceWebUrlResponse;
import com.weizhu.proto.AdminSceneProtos.GetRecommenderRecommendProductRequest;
import com.weizhu.proto.AdminSceneProtos.GetRecommenderRecommendProductResponse;
import com.weizhu.proto.AdminSceneProtos.GetSceneItemRequest;
import com.weizhu.proto.AdminSceneProtos.GetSceneItemResponse;
import com.weizhu.proto.AdminSceneProtos.GetSceneResponse;
import com.weizhu.proto.AdminSceneProtos.MigrateRecommenderCompetitorProductRequest;
import com.weizhu.proto.AdminSceneProtos.MigrateRecommenderCompetitorProductResponse;
import com.weizhu.proto.AdminSceneProtos.MigrateSceneItemRequest;
import com.weizhu.proto.AdminSceneProtos.MigrateSceneItemResponse;
import com.weizhu.proto.AdminSceneProtos.AddRecommendProdToCompetitorProdRequest;
import com.weizhu.proto.AdminSceneProtos.AddRecommendProdToCompetitorProdResponse;
import com.weizhu.proto.AdminSceneProtos.SetSceneHomeRequest;
import com.weizhu.proto.AdminSceneProtos.SetSceneHomeResponse;
import com.weizhu.proto.AdminSceneProtos.UpdateRecommenderCategoryRequest;
import com.weizhu.proto.AdminSceneProtos.UpdateRecommenderCategoryResponse;
import com.weizhu.proto.AdminSceneProtos.UpdateRecommenderCategoryStateRequest;
import com.weizhu.proto.AdminSceneProtos.UpdateRecommenderCategoryStateResponse;
import com.weizhu.proto.AdminSceneProtos.UpdateRecommenderCompetitorProductRequest;
import com.weizhu.proto.AdminSceneProtos.UpdateRecommenderCompetitorProductResponse;
import com.weizhu.proto.AdminSceneProtos.UpdateRecommenderCompetitorProductStateRequest;
import com.weizhu.proto.AdminSceneProtos.UpdateRecommenderCompetitorProductStateResponse;
import com.weizhu.proto.AdminSceneProtos.UpdateRecommenderRecommendProductPriceWebUrlRequest;
import com.weizhu.proto.AdminSceneProtos.UpdateRecommenderRecommendProductPriceWebUrlResponse;
import com.weizhu.proto.AdminSceneProtos.UpdateRecommenderRecommendProductRequest;
import com.weizhu.proto.AdminSceneProtos.UpdateRecommenderRecommendProductResponse;
import com.weizhu.proto.AdminSceneProtos.UpdateRecommenderRecommendProductStateRequest;
import com.weizhu.proto.AdminSceneProtos.UpdateRecommenderRecommendProductStateResponse;
import com.weizhu.proto.AdminSceneProtos.UpdateSceneItemOrderRequest;
import com.weizhu.proto.AdminSceneProtos.UpdateSceneItemOrderResponse;
import com.weizhu.proto.AdminSceneProtos.UpdateSceneItemStateRequest;
import com.weizhu.proto.AdminSceneProtos.UpdateSceneItemStateResponse;
import com.weizhu.proto.AdminSceneProtos.UpdateSceneRequest;
import com.weizhu.proto.AdminSceneProtos.UpdateSceneResponse;
import com.weizhu.proto.AdminSceneProtos.UpdateSceneStateRequest;
import com.weizhu.proto.AdminSceneProtos.UpdateSceneStateResponse;
import com.weizhu.proto.WeizhuProtos.EmptyRequest;

public interface AdminSceneService {
	
	@ResponseType(SetSceneHomeResponse.class)
	ListenableFuture<SetSceneHomeResponse> setSceneHome(AdminHead head, SetSceneHomeRequest request);

	@ResponseType(GetSceneResponse.class)
	ListenableFuture<GetSceneResponse> getScene(AdminHead head, EmptyRequest request);

	@WriteMethod
	@ResponseType(CreateSceneResponse.class)
	ListenableFuture<CreateSceneResponse> createScene(AdminHead head, CreateSceneRequest request);

	@WriteMethod
	@ResponseType(UpdateSceneResponse.class)
	ListenableFuture<UpdateSceneResponse> updateScene(AdminHead head, UpdateSceneRequest request);
	
	@WriteMethod
	@ResponseType(UpdateSceneStateResponse.class)
	ListenableFuture<UpdateSceneStateResponse> updateSceneState(AdminHead head, UpdateSceneStateRequest request);
	
	@WriteMethod
	@ResponseType(CreateSceneItemResponse.class)
	ListenableFuture<CreateSceneItemResponse> createSceneItem(AdminHead head, CreateSceneItemRequest request);
	
	@WriteMethod
	@ResponseType(UpdateSceneItemStateResponse.class)
	ListenableFuture<UpdateSceneItemStateResponse> updateSceneItemState(AdminHead head, UpdateSceneItemStateRequest request);

	@WriteMethod
	@ResponseType(MigrateSceneItemResponse.class)
	ListenableFuture<MigrateSceneItemResponse> migrateSceneItem(AdminHead head, MigrateSceneItemRequest request);

	@ResponseType(GetSceneItemResponse.class)
	ListenableFuture<GetSceneItemResponse> getSceneItem(AdminHead head, GetSceneItemRequest request);		

	@WriteMethod
	@ResponseType(UpdateSceneItemOrderResponse.class)
	ListenableFuture<UpdateSceneItemOrderResponse> updateSceneItemOrder(AdminHead head, UpdateSceneItemOrderRequest request);		

	
	
	// 工具——盖帽神器（超值推荐）
	
	@ResponseType(GetRecommenderHomeResponse.class)
	ListenableFuture<GetRecommenderHomeResponse> getRecommenderHome(AdminHead head, EmptyRequest request);

	@WriteMethod
	@ResponseType(CreateRecommenderCategoryResponse.class)
	ListenableFuture<CreateRecommenderCategoryResponse> createRecommenderCategory(AdminHead head, CreateRecommenderCategoryRequest request);

	@WriteMethod
	@ResponseType(UpdateRecommenderCategoryResponse.class)
	ListenableFuture<UpdateRecommenderCategoryResponse> updateRecommenderCategory(AdminHead head, UpdateRecommenderCategoryRequest request);

	@WriteMethod
	@ResponseType(UpdateRecommenderCategoryStateResponse.class)
	ListenableFuture<UpdateRecommenderCategoryStateResponse> updateRecommenderCategoryState(AdminHead head, UpdateRecommenderCategoryStateRequest request);
	
	@ResponseType(GetRecommenderCompetitorProductResponse.class)
	ListenableFuture<GetRecommenderCompetitorProductResponse> getRecommenderCompetitorProduct(AdminHead head, GetRecommenderCompetitorProductRequest request);
	
	@ResponseType(MigrateRecommenderCompetitorProductResponse.class)
	ListenableFuture<MigrateRecommenderCompetitorProductResponse> migrateRecommenderCompetitorProduct(AdminHead head, MigrateRecommenderCompetitorProductRequest request);

	@WriteMethod
	@ResponseType(CreateRecommenderCompetitorProductResponse.class)
	ListenableFuture<CreateRecommenderCompetitorProductResponse> createRecommenderCompetitorProduct(AdminHead head, CreateRecommenderCompetitorProductRequest request);
	
	@WriteMethod
	@ResponseType(UpdateRecommenderCompetitorProductResponse.class)
	ListenableFuture<UpdateRecommenderCompetitorProductResponse> updateRecommenderCompetitorProduct(AdminHead head, UpdateRecommenderCompetitorProductRequest request);

	@WriteMethod
	@ResponseType(UpdateRecommenderCompetitorProductStateResponse.class)
	ListenableFuture<UpdateRecommenderCompetitorProductStateResponse> updateRecommenderCompetitorProductState(AdminHead head, UpdateRecommenderCompetitorProductStateRequest request);

	@ResponseType(GetRecommenderRecommendProductResponse.class)
	ListenableFuture<GetRecommenderRecommendProductResponse> getRecommenderRecommendProduct(AdminHead head, GetRecommenderRecommendProductRequest request);		

	@WriteMethod
	@ResponseType(CreateRecommenderRecommendProductResponse.class)
	ListenableFuture<CreateRecommenderRecommendProductResponse> createRecommenderRecommendProduct(AdminHead head, CreateRecommenderRecommendProductRequest request);		

	@WriteMethod
	@ResponseType(UpdateRecommenderRecommendProductResponse.class)
	ListenableFuture<UpdateRecommenderRecommendProductResponse> updateRecommenderRecommendProduct(AdminHead head, UpdateRecommenderRecommendProductRequest request);		

	@WriteMethod
	@ResponseType(UpdateRecommenderRecommendProductStateResponse.class)
	ListenableFuture<UpdateRecommenderRecommendProductStateResponse> updateRecommenderRecommendProductState(AdminHead head, UpdateRecommenderRecommendProductStateRequest request);		

	@WriteMethod
	@ResponseType(AddRecommendProdToCompetitorProdResponse.class)
	ListenableFuture<AddRecommendProdToCompetitorProdResponse> addRecommendProdToCompetitorProd(AdminHead head, AddRecommendProdToCompetitorProdRequest request);		

	@WriteMethod
	@ResponseType(DeleteRecommendProdFromCompetitorProdResponse.class)
	ListenableFuture<DeleteRecommendProdFromCompetitorProdResponse> deleteRecommendProdFromCompetitorProd(AdminHead head, DeleteRecommendProdFromCompetitorProdRequest request);		

	@ResponseType(GetRecommenderRecommendProductPriceWebUrlResponse.class)
	ListenableFuture<GetRecommenderRecommendProductPriceWebUrlResponse> getRecommenderRecommendProductPriceWebUrl(AdminHead head, GetRecommenderRecommendProductPriceWebUrlRequest request);		

	@WriteMethod
	@ResponseType(CreateRecommenderRecommendProductPriceWebUrlResponse.class)
	ListenableFuture<CreateRecommenderRecommendProductPriceWebUrlResponse> createRecommenderRecommendProductPriceWebUrl(AdminHead head, CreateRecommenderRecommendProductPriceWebUrlRequest request);		

	@WriteMethod
	@ResponseType(UpdateRecommenderRecommendProductPriceWebUrlResponse.class)
	ListenableFuture<UpdateRecommenderRecommendProductPriceWebUrlResponse> updateRecommenderRecommendProductPriceWebUrl(AdminHead head, UpdateRecommenderRecommendProductPriceWebUrlRequest request);		

	@WriteMethod
	@ResponseType(DeleteRecommenderRecommendProductPriceWebUrlResponse.class)
	ListenableFuture<DeleteRecommenderRecommendProductPriceWebUrlResponse> deleteRecommenderRecommendProductPriceWebUrl(AdminHead head, DeleteRecommenderRecommendProductPriceWebUrlRequest request);		

}
