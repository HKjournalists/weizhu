package com.weizhu.proto;

import com.google.common.util.concurrent.ListenableFuture;
import com.weizhu.proto.AdminProtos.AdminHead;
import com.weizhu.proto.AdminToolsProtos.CreateInfoDimensionRequest;
import com.weizhu.proto.AdminToolsProtos.CreateInfoDimensionResponse;
import com.weizhu.proto.AdminToolsProtos.CreateInfoItemRequest;
import com.weizhu.proto.AdminToolsProtos.CreateInfoItemResponse;
import com.weizhu.proto.AdminToolsProtos.CreateInfoTagRequest;
import com.weizhu.proto.AdminToolsProtos.CreateInfoTagResponse;
import com.weizhu.proto.AdminToolsProtos.CreateToolRequest;
import com.weizhu.proto.AdminToolsProtos.CreateToolResponse;
import com.weizhu.proto.AdminToolsProtos.DeleteInfoDimensionRequest;
import com.weizhu.proto.AdminToolsProtos.DeleteInfoDimensionResponse;
import com.weizhu.proto.AdminToolsProtos.DeleteInfoItemRequest;
import com.weizhu.proto.AdminToolsProtos.DeleteInfoItemResponse;
import com.weizhu.proto.AdminToolsProtos.DeleteInfoTagRequest;
import com.weizhu.proto.AdminToolsProtos.DeleteInfoTagResponse;
import com.weizhu.proto.AdminToolsProtos.DeleteToolRequest;
import com.weizhu.proto.AdminToolsProtos.DeleteToolResponse;
import com.weizhu.proto.AdminToolsProtos.GetInfoDimensionTagRequest;
import com.weizhu.proto.AdminToolsProtos.GetInfoDimensionTagResponse;
import com.weizhu.proto.AdminToolsProtos.GetInfoItemContentRequest;
import com.weizhu.proto.AdminToolsProtos.GetInfoItemContentResponse;
import com.weizhu.proto.AdminToolsProtos.GetInfoItemListRequest;
import com.weizhu.proto.AdminToolsProtos.GetInfoItemListResponse;
import com.weizhu.proto.AdminToolsProtos.GetToolListRequest;
import com.weizhu.proto.AdminToolsProtos.GetToolListResponse;
import com.weizhu.proto.AdminToolsProtos.UpdateInfoDimensionRequest;
import com.weizhu.proto.AdminToolsProtos.UpdateInfoDimensionResponse;
import com.weizhu.proto.AdminToolsProtos.UpdateInfoItemContentRequest;
import com.weizhu.proto.AdminToolsProtos.UpdateInfoItemContentResponse;
import com.weizhu.proto.AdminToolsProtos.UpdateInfoItemRequest;
import com.weizhu.proto.AdminToolsProtos.UpdateInfoItemResponse;
import com.weizhu.proto.AdminToolsProtos.UpdateInfoTagRequest;
import com.weizhu.proto.AdminToolsProtos.UpdateInfoTagResponse;
import com.weizhu.proto.AdminToolsProtos.UpdateToolRequest;
import com.weizhu.proto.AdminToolsProtos.UpdateToolResponse;

public interface AdminToolsService {
	
	/* tool */
	
	@ResponseType(GetToolListResponse.class)
	ListenableFuture<GetToolListResponse> getToolList(AdminHead head, GetToolListRequest request);
	
	@WriteMethod
	@ResponseType(CreateToolResponse.class)
	ListenableFuture<CreateToolResponse> createTool(AdminHead head, CreateToolRequest request);
	
	@WriteMethod
	@ResponseType(UpdateToolResponse.class)
	ListenableFuture<UpdateToolResponse> updateTool(AdminHead head, UpdateToolRequest request);
	
	@WriteMethod
	@ResponseType(DeleteToolResponse.class)
	ListenableFuture<DeleteToolResponse> deleteTool(AdminHead head, DeleteToolRequest request);
	
	/* info tool */
	
	@ResponseType(GetInfoDimensionTagResponse.class)
	ListenableFuture<GetInfoDimensionTagResponse> getInfoDimensionTag(AdminHead head, GetInfoDimensionTagRequest request);
	
	@ResponseType(GetInfoItemListResponse.class)
	ListenableFuture<GetInfoItemListResponse> getInfoItemList(AdminHead head, GetInfoItemListRequest request);
	
	@ResponseType(GetInfoItemContentResponse.class)
	ListenableFuture<GetInfoItemContentResponse> getInfoItemContent(AdminHead head, GetInfoItemContentRequest request);
	
	@WriteMethod
	@ResponseType(CreateInfoDimensionResponse.class)
	ListenableFuture<CreateInfoDimensionResponse> createInfoDimension(AdminHead head, CreateInfoDimensionRequest request);
	
	@WriteMethod
	@ResponseType(UpdateInfoDimensionResponse.class)
	ListenableFuture<UpdateInfoDimensionResponse> updateInfoDimension(AdminHead head, UpdateInfoDimensionRequest request);
	
	@WriteMethod
	@ResponseType(DeleteInfoDimensionResponse.class)
	ListenableFuture<DeleteInfoDimensionResponse> deleteInfoDimension(AdminHead head, DeleteInfoDimensionRequest request);
	
	@WriteMethod
	@ResponseType(CreateInfoTagResponse.class)
	ListenableFuture<CreateInfoTagResponse> createInfoTag(AdminHead head, CreateInfoTagRequest request);
	
	@WriteMethod
	@ResponseType(UpdateInfoTagResponse.class)
	ListenableFuture<UpdateInfoTagResponse> updateInfoTag(AdminHead head, UpdateInfoTagRequest request);
	
	@WriteMethod
	@ResponseType(DeleteInfoTagResponse.class)
	ListenableFuture<DeleteInfoTagResponse> deleteInfoTag(AdminHead head, DeleteInfoTagRequest request);
	
	@WriteMethod
	@ResponseType(CreateInfoItemResponse.class)
	ListenableFuture<CreateInfoItemResponse> createInfoItem(AdminHead head, CreateInfoItemRequest request);
	
	@WriteMethod
	@ResponseType(UpdateInfoItemResponse.class)
	ListenableFuture<UpdateInfoItemResponse> updateInfoItem(AdminHead head, UpdateInfoItemRequest request);
	
	@WriteMethod
	@ResponseType(UpdateInfoItemContentResponse.class)
	ListenableFuture<UpdateInfoItemContentResponse> updateInfoItemContent(AdminHead head, UpdateInfoItemContentRequest request);
	
	@WriteMethod
	@ResponseType(DeleteInfoItemResponse.class)
	ListenableFuture<DeleteInfoItemResponse> deleteInfoItem(AdminHead head, DeleteInfoItemRequest request);
	
}
