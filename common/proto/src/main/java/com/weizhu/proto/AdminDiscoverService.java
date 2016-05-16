package com.weizhu.proto;

import com.google.common.util.concurrent.ListenableFuture;
import com.weizhu.proto.AdminDiscoverProtos.AddItemToCategoryRequest;
import com.weizhu.proto.AdminDiscoverProtos.AddItemToCategoryResponse;
import com.weizhu.proto.AdminDiscoverProtos.CreateBannerRequest;
import com.weizhu.proto.AdminDiscoverProtos.CreateBannerResponse;
import com.weizhu.proto.AdminDiscoverProtos.CreateItemRequest;
import com.weizhu.proto.AdminDiscoverProtos.CreateItemResponse;
import com.weizhu.proto.AdminDiscoverProtos.CreateModuleCategoryRequest;
import com.weizhu.proto.AdminDiscoverProtos.CreateModuleCategoryResponse;
import com.weizhu.proto.AdminDiscoverProtos.CreateModuleRequest;
import com.weizhu.proto.AdminDiscoverProtos.CreateModuleResponse;
import com.weizhu.proto.AdminDiscoverProtos.DeleteItemFromCategoryRequest;
import com.weizhu.proto.AdminDiscoverProtos.DeleteItemFromCategoryResponse;
import com.weizhu.proto.AdminDiscoverProtos.GetBannerResponse;
import com.weizhu.proto.AdminDiscoverProtos.GetItemByIdRequest;
import com.weizhu.proto.AdminDiscoverProtos.GetItemByIdResponse;
import com.weizhu.proto.AdminDiscoverProtos.GetItemCommentListRequest;
import com.weizhu.proto.AdminDiscoverProtos.GetItemCommentListResponse;
import com.weizhu.proto.AdminDiscoverProtos.GetItemLearnListRequest;
import com.weizhu.proto.AdminDiscoverProtos.GetItemLearnListResponse;
import com.weizhu.proto.AdminDiscoverProtos.GetItemLikeListRequest;
import com.weizhu.proto.AdminDiscoverProtos.GetItemLikeListResponse;
import com.weizhu.proto.AdminDiscoverProtos.GetItemListRequest;
import com.weizhu.proto.AdminDiscoverProtos.GetItemListResponse;
import com.weizhu.proto.AdminDiscoverProtos.GetItemScoreListRequest;
import com.weizhu.proto.AdminDiscoverProtos.GetItemScoreListResponse;
import com.weizhu.proto.AdminDiscoverProtos.GetItemShareListRequest;
import com.weizhu.proto.AdminDiscoverProtos.GetItemShareListResponse;
import com.weizhu.proto.AdminDiscoverProtos.GetModuleCategoryRequest;
import com.weizhu.proto.AdminDiscoverProtos.GetModuleCategoryResponse;
import com.weizhu.proto.AdminDiscoverProtos.GetModuleResponse;
import com.weizhu.proto.AdminDiscoverProtos.ImportItemRequest;
import com.weizhu.proto.AdminDiscoverProtos.ImportItemResponse;
import com.weizhu.proto.AdminDiscoverProtos.MigrateModuleCategoryRequest;
import com.weizhu.proto.AdminDiscoverProtos.MigrateModuleCategoryResponse;
import com.weizhu.proto.AdminDiscoverProtos.SetDiscoverHomeRequest;
import com.weizhu.proto.AdminDiscoverProtos.SetDiscoverHomeResponse;
import com.weizhu.proto.AdminDiscoverProtos.UpdateBannerRequest;
import com.weizhu.proto.AdminDiscoverProtos.UpdateBannerResponse;
import com.weizhu.proto.AdminDiscoverProtos.UpdateBannerStateRequest;
import com.weizhu.proto.AdminDiscoverProtos.UpdateBannerStateResponse;
import com.weizhu.proto.AdminDiscoverProtos.UpdateItemRequest;
import com.weizhu.proto.AdminDiscoverProtos.UpdateItemResponse;
import com.weizhu.proto.AdminDiscoverProtos.UpdateItemStateRequest;
import com.weizhu.proto.AdminDiscoverProtos.UpdateItemStateResponse;
import com.weizhu.proto.AdminDiscoverProtos.UpdateModuleCategoryOrderRequest;
import com.weizhu.proto.AdminDiscoverProtos.UpdateModuleCategoryOrderResponse;
import com.weizhu.proto.AdminDiscoverProtos.UpdateModuleCategoryRequest;
import com.weizhu.proto.AdminDiscoverProtos.UpdateModuleCategoryResponse;
import com.weizhu.proto.AdminDiscoverProtos.UpdateModuleCategoryStateRequest;
import com.weizhu.proto.AdminDiscoverProtos.UpdateModuleCategoryStateResponse;
import com.weizhu.proto.AdminDiscoverProtos.UpdateModuleRequest;
import com.weizhu.proto.AdminDiscoverProtos.UpdateModuleResponse;
import com.weizhu.proto.AdminDiscoverProtos.UpdateModuleStateRequest;
import com.weizhu.proto.AdminDiscoverProtos.UpdateModuleStateResponse;
import com.weizhu.proto.AdminProtos.AdminHead;
import com.weizhu.proto.BossProtos.BossHead;
import com.weizhu.proto.WeizhuProtos.EmptyRequest;
import com.weizhu.proto.WeizhuProtos.SystemHead;

public interface AdminDiscoverService {
	
	@WriteMethod
	@ResponseType(SetDiscoverHomeResponse.class)
	ListenableFuture<SetDiscoverHomeResponse> setDiscoverHome(AdminHead head, SetDiscoverHomeRequest request);
	
	
	@ResponseType(GetBannerResponse.class)
	ListenableFuture<GetBannerResponse> getBanner(AdminHead head, EmptyRequest request);
	
	@WriteMethod
	@ResponseType(CreateBannerResponse.class)
	ListenableFuture<CreateBannerResponse> createBanner(AdminHead head, CreateBannerRequest request);

	@WriteMethod
	@ResponseType(UpdateBannerResponse.class)
	ListenableFuture<UpdateBannerResponse> updateBanner(AdminHead head, UpdateBannerRequest request);

	@WriteMethod
	@ResponseType(UpdateBannerStateResponse.class)
	ListenableFuture<UpdateBannerStateResponse> updateBannerState(AdminHead head, UpdateBannerStateRequest request);
	
	
	@ResponseType(GetModuleResponse.class)
	ListenableFuture<GetModuleResponse> getModule(AdminHead head, EmptyRequest request);
	
	@WriteMethod
	@ResponseType(CreateModuleResponse.class)
	ListenableFuture<CreateModuleResponse> createModule(AdminHead head, CreateModuleRequest request);
	
	@WriteMethod
	@ResponseType(UpdateModuleResponse.class)
	ListenableFuture<UpdateModuleResponse> updateModule(AdminHead head, UpdateModuleRequest request);
	
	@WriteMethod
	@ResponseType(UpdateModuleStateResponse.class)
	ListenableFuture<UpdateModuleStateResponse> updateModuleState(AdminHead head, UpdateModuleStateRequest request);
	
	
	@ResponseType(GetModuleCategoryResponse.class)
	ListenableFuture<GetModuleCategoryResponse> getModuleCategory(AdminHead head, GetModuleCategoryRequest request);	

	@WriteMethod
	@ResponseType(CreateModuleCategoryResponse.class)
	ListenableFuture<CreateModuleCategoryResponse> createModuleCategory(AdminHead head, CreateModuleCategoryRequest request);

	@WriteMethod
	@ResponseType(UpdateModuleCategoryResponse.class)
	ListenableFuture<UpdateModuleCategoryResponse> updateModuleCategory(AdminHead head, UpdateModuleCategoryRequest request);	
	
	@WriteMethod
	@ResponseType(UpdateModuleCategoryStateResponse.class)
	ListenableFuture<UpdateModuleCategoryStateResponse> updateModuleCategoryState(AdminHead head, UpdateModuleCategoryStateRequest request);
	
	@WriteMethod
	@ResponseType(UpdateModuleCategoryOrderResponse.class)
	ListenableFuture<UpdateModuleCategoryOrderResponse> updateModuleCategoryOrder(AdminHead head, UpdateModuleCategoryOrderRequest request);
	
	@WriteMethod
	@ResponseType(MigrateModuleCategoryResponse.class)
	ListenableFuture<MigrateModuleCategoryResponse> migrateModuleCategory(AdminHead head, MigrateModuleCategoryRequest request);
	
	@WriteMethod
	@ResponseType(AddItemToCategoryResponse.class)
	ListenableFuture<AddItemToCategoryResponse> addItemToCategory(AdminHead head, AddItemToCategoryRequest request);	

	@WriteMethod
	@ResponseType(DeleteItemFromCategoryResponse.class)
	ListenableFuture<DeleteItemFromCategoryResponse> deleteItemFromCategory(AdminHead head, DeleteItemFromCategoryRequest request);
	
	
	@ResponseType(GetItemListResponse.class)
	ListenableFuture<GetItemListResponse> getItemList(AdminHead head, GetItemListRequest request);
	
	@ResponseType(GetItemByIdResponse.class)
	ListenableFuture<GetItemByIdResponse> getItemById(AdminHead head, GetItemByIdRequest request);
	
	@WriteMethod
	@ResponseType(CreateItemResponse.class)
	ListenableFuture<CreateItemResponse> createItem(AdminHead head, CreateItemRequest request);

	@WriteMethod
	@ResponseType(ImportItemResponse.class)
	ListenableFuture<ImportItemResponse> importItem(AdminHead head, ImportItemRequest request);

	@WriteMethod
	@ResponseType(UpdateItemResponse.class)
	ListenableFuture<UpdateItemResponse> updateItem(AdminHead head, UpdateItemRequest request);

	@WriteMethod
	@ResponseType(UpdateItemStateResponse.class)
	ListenableFuture<UpdateItemStateResponse> updateItemState(AdminHead head, UpdateItemStateRequest request);
	
	
	@ResponseType(GetItemLearnListResponse.class)
	ListenableFuture<GetItemLearnListResponse> getItemLearnList(AdminHead head, GetItemLearnListRequest request);
	
	@ResponseType(GetItemCommentListResponse.class)
	ListenableFuture<GetItemCommentListResponse> getItemCommentList(AdminHead head, GetItemCommentListRequest request);
	
	@ResponseType(GetItemScoreListResponse.class)
	ListenableFuture<GetItemScoreListResponse> getItemScoreList(AdminHead head, GetItemScoreListRequest request);
	
	@ResponseType(GetItemLikeListResponse.class)
	ListenableFuture<GetItemLikeListResponse> getItemLikeList(AdminHead head, GetItemLikeListRequest request);
	
	@ResponseType(GetItemShareListResponse.class)
	ListenableFuture<GetItemShareListResponse> getItemShareList(AdminHead head, GetItemShareListRequest request);
	
	
	/* for system */
	
	@ResponseType(GetItemListResponse.class)
	ListenableFuture<GetItemListResponse> getItemList(SystemHead head, GetItemListRequest request);
	
	/* for boss */
	
	@ResponseType(GetModuleResponse.class)
	ListenableFuture<GetModuleResponse> getModule(BossHead head, EmptyRequest request);
	
	@ResponseType(GetItemListResponse.class)
	ListenableFuture<GetItemListResponse> getItemList(BossHead head, GetItemListRequest request);
	
	@ResponseType(GetItemByIdResponse.class)
	ListenableFuture<GetItemByIdResponse> getItemById(BossHead head, GetItemByIdRequest request);
	
	@WriteMethod
	@ResponseType(CreateItemResponse.class)
	ListenableFuture<CreateItemResponse> createItem(BossHead head, CreateItemRequest request);

	@WriteMethod
	@ResponseType(UpdateItemResponse.class)
	ListenableFuture<UpdateItemResponse> updateItem(BossHead head, UpdateItemRequest request);
}
