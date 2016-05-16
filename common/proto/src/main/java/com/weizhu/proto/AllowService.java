package com.weizhu.proto;

import com.google.common.util.concurrent.ListenableFuture;
import com.weizhu.proto.AdminProtos.AdminHead;
import com.weizhu.proto.AllowProtos.CheckAllowRequest;
import com.weizhu.proto.AllowProtos.CheckAllowResponse;
import com.weizhu.proto.AllowProtos.CopyModelRequest;
import com.weizhu.proto.AllowProtos.CopyModelResponse;
import com.weizhu.proto.AllowProtos.CreateLevelRuleRequest;
import com.weizhu.proto.AllowProtos.CreateLevelRuleResponse;
import com.weizhu.proto.AllowProtos.CreateModelRequest;
import com.weizhu.proto.AllowProtos.CreateModelResponse;
import com.weizhu.proto.AllowProtos.CreatePositionRuleRequest;
import com.weizhu.proto.AllowProtos.CreatePositionRuleResponse;
import com.weizhu.proto.AllowProtos.CreateTeamRuleRequest;
import com.weizhu.proto.AllowProtos.CreateTeamRuleResponse;
import com.weizhu.proto.AllowProtos.CreateUserRuleRequest;
import com.weizhu.proto.AllowProtos.CreateUserRuleResponse;
import com.weizhu.proto.AllowProtos.DeleteModelRequest;
import com.weizhu.proto.AllowProtos.DeleteModelResponse;
import com.weizhu.proto.AllowProtos.DeleteRuleRequest;
import com.weizhu.proto.AllowProtos.DeleteRuleResponse;
import com.weizhu.proto.AllowProtos.GetModelByIdRequest;
import com.weizhu.proto.AllowProtos.GetModelByIdResponse;
import com.weizhu.proto.AllowProtos.GetModelListRequest;
import com.weizhu.proto.AllowProtos.GetModelListResponse;
import com.weizhu.proto.AllowProtos.GetModelRuleListRequest;
import com.weizhu.proto.AllowProtos.GetModelRuleListResponse;
import com.weizhu.proto.AllowProtos.UpdateLevelRuleRequest;
import com.weizhu.proto.AllowProtos.UpdateLevelRuleResponse;
import com.weizhu.proto.AllowProtos.UpdateModelRequest;
import com.weizhu.proto.AllowProtos.UpdateModelResponse;
import com.weizhu.proto.AllowProtos.UpdateModelRuleOrderRequest;
import com.weizhu.proto.AllowProtos.UpdateModelRuleOrderResponse;
import com.weizhu.proto.AllowProtos.UpdatePositionRuleRequest;
import com.weizhu.proto.AllowProtos.UpdatePositionRuleResponse;
import com.weizhu.proto.AllowProtos.UpdateTeamRuleRequest;
import com.weizhu.proto.AllowProtos.UpdateTeamRuleResponse;
import com.weizhu.proto.AllowProtos.UpdateUserRuleRequest;
import com.weizhu.proto.AllowProtos.UpdateUserRuleResponse;
import com.weizhu.proto.WeizhuProtos.RequestHead;
import com.weizhu.proto.WeizhuProtos.SystemHead;

public interface AllowService {
	
	@ResponseType(CheckAllowResponse.class)
	ListenableFuture<CheckAllowResponse> checkAllow(AdminHead head, CheckAllowRequest request);
	
	@ResponseType(CheckAllowResponse.class)
	ListenableFuture<CheckAllowResponse> checkAllow(RequestHead head, CheckAllowRequest request);
	
	@ResponseType(CheckAllowResponse.class)
	ListenableFuture<CheckAllowResponse> checkAllow(SystemHead head, CheckAllowRequest request);
	
	
	@ResponseType(GetModelListResponse.class)
	ListenableFuture<GetModelListResponse> getModelList(AdminHead head, GetModelListRequest request);
	
	@ResponseType(GetModelByIdResponse.class)
	ListenableFuture<GetModelByIdResponse> getModelById(AdminHead head, GetModelByIdRequest request);
	
	@ResponseType(GetModelRuleListResponse.class)
	ListenableFuture<GetModelRuleListResponse> getModelRuleList(AdminHead head, GetModelRuleListRequest request);
	
	@WriteMethod
	@ResponseType(CreateModelResponse.class)
	ListenableFuture<CreateModelResponse> createModel(AdminHead head, CreateModelRequest request);
	
	
	@WriteMethod
	@ResponseType(CreateUserRuleResponse.class)
	ListenableFuture<CreateUserRuleResponse> createUserRule(AdminHead head, CreateUserRuleRequest request);
	
	@WriteMethod
	@ResponseType(CreateTeamRuleResponse.class)
	ListenableFuture<CreateTeamRuleResponse> createTeamRule(AdminHead head, CreateTeamRuleRequest request);
	
	@WriteMethod
	@ResponseType(CreatePositionRuleResponse.class)
	ListenableFuture<CreatePositionRuleResponse> createPositionRule(AdminHead head, CreatePositionRuleRequest request);
	
	@WriteMethod
	@ResponseType(CreateLevelRuleResponse.class)
	ListenableFuture<CreateLevelRuleResponse> createLevelRule(AdminHead head, CreateLevelRuleRequest request);
	
	
	@WriteMethod
	@ResponseType(DeleteModelResponse.class)
	ListenableFuture<DeleteModelResponse> deleteModel(AdminHead head, DeleteModelRequest request);
	
	@WriteMethod
	@ResponseType(DeleteRuleResponse.class)
	ListenableFuture<DeleteRuleResponse> deleteRule(AdminHead head, DeleteRuleRequest request);
	
	@WriteMethod
	@ResponseType(UpdateModelResponse.class)
	ListenableFuture<UpdateModelResponse> updateModel(AdminHead head, UpdateModelRequest request);
	
	@WriteMethod
	@ResponseType(UpdateModelRuleOrderResponse.class)
	ListenableFuture<UpdateModelRuleOrderResponse> updateModelRuleOrder(AdminHead head, UpdateModelRuleOrderRequest request);
	
	
	@WriteMethod
	@ResponseType(UpdateUserRuleResponse.class)
	ListenableFuture<UpdateUserRuleResponse> updateUserRule(AdminHead head, UpdateUserRuleRequest request);
	
	@WriteMethod
	@ResponseType(UpdateTeamRuleResponse.class)
	ListenableFuture<UpdateTeamRuleResponse> updateTeamRule(AdminHead head, UpdateTeamRuleRequest request);
	
	@WriteMethod
	@ResponseType(UpdatePositionRuleResponse.class)
	ListenableFuture<UpdatePositionRuleResponse> updatePositionRule(AdminHead head, UpdatePositionRuleRequest request);
	
	@WriteMethod
	@ResponseType(UpdateLevelRuleResponse.class)
	ListenableFuture<UpdateLevelRuleResponse> updateLevelRule(AdminHead head, UpdateLevelRuleRequest request);
	
	
	@WriteMethod
	@ResponseType(CopyModelResponse.class)
	ListenableFuture<CopyModelResponse> copyModel(AdminHead head, CopyModelRequest request);
}
