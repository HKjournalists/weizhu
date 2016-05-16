package com.weizhu.proto;

import com.google.common.util.concurrent.ListenableFuture;
import com.weizhu.proto.AdminOfficialProtos.CancelOfficialSendPlanRequest;
import com.weizhu.proto.AdminOfficialProtos.CancelOfficialSendPlanResponse;
import com.weizhu.proto.AdminOfficialProtos.CreateOfficialRequest;
import com.weizhu.proto.AdminOfficialProtos.CreateOfficialResponse;
import com.weizhu.proto.AdminOfficialProtos.CreateOfficialSendPlanRequest;
import com.weizhu.proto.AdminOfficialProtos.CreateOfficialSendPlanResponse;
import com.weizhu.proto.AdminOfficialProtos.DeleteOfficialRequest;
import com.weizhu.proto.AdminOfficialProtos.DeleteOfficialResponse;
import com.weizhu.proto.AdminOfficialProtos.GetOfficialByIdRequest;
import com.weizhu.proto.AdminOfficialProtos.GetOfficialByIdResponse;
import com.weizhu.proto.AdminOfficialProtos.GetOfficialListRequest;
import com.weizhu.proto.AdminOfficialProtos.GetOfficialListResponse;
import com.weizhu.proto.AdminOfficialProtos.GetOfficialMessageRequest;
import com.weizhu.proto.AdminOfficialProtos.GetOfficialMessageResponse;
import com.weizhu.proto.AdminOfficialProtos.GetOfficialSendPlanByIdRequest;
import com.weizhu.proto.AdminOfficialProtos.GetOfficialSendPlanByIdResponse;
import com.weizhu.proto.AdminOfficialProtos.GetOfficialSendPlanListRequest;
import com.weizhu.proto.AdminOfficialProtos.GetOfficialSendPlanListResponse;
import com.weizhu.proto.AdminOfficialProtos.SendSecretaryMessageRequest;
import com.weizhu.proto.AdminOfficialProtos.SendSecretaryMessageResponse;
import com.weizhu.proto.AdminOfficialProtos.SetOfficialStateRequest;
import com.weizhu.proto.AdminOfficialProtos.SetOfficialStateResponse;
import com.weizhu.proto.AdminOfficialProtos.UpdateOfficialRequest;
import com.weizhu.proto.AdminOfficialProtos.UpdateOfficialResponse;
import com.weizhu.proto.AdminProtos.AdminHead;
import com.weizhu.proto.WeizhuProtos.RequestHead;
import com.weizhu.proto.WeizhuProtos.SystemHead;

public interface AdminOfficialService {

	@ResponseType(GetOfficialByIdResponse.class)
	ListenableFuture<GetOfficialByIdResponse> getOfficialById(AdminHead head, GetOfficialByIdRequest request);
	
	@ResponseType(GetOfficialByIdResponse.class)
	ListenableFuture<GetOfficialByIdResponse> getOfficialById(SystemHead head, GetOfficialByIdRequest request);
	
	@ResponseType(GetOfficialListResponse.class)
	ListenableFuture<GetOfficialListResponse> getOfficialList(AdminHead head, GetOfficialListRequest request);
	
	@WriteMethod
	@ResponseType(CreateOfficialResponse.class)
	ListenableFuture<CreateOfficialResponse> createOfficial(AdminHead head, CreateOfficialRequest request);
	
	@WriteMethod
	@ResponseType(UpdateOfficialResponse.class)
	ListenableFuture<UpdateOfficialResponse> updateOfficial(AdminHead head, UpdateOfficialRequest request);
	
	@WriteMethod
	@ResponseType(DeleteOfficialResponse.class)
	ListenableFuture<DeleteOfficialResponse> deleteOfficial(AdminHead head, DeleteOfficialRequest request);
	
	@WriteMethod
	@ResponseType(SetOfficialStateResponse.class)
	ListenableFuture<SetOfficialStateResponse> setOfficialState(AdminHead head, SetOfficialStateRequest request);
	
	@ResponseType(GetOfficialSendPlanByIdResponse.class)
	ListenableFuture<GetOfficialSendPlanByIdResponse> getOfficialSendPlanById(AdminHead head, GetOfficialSendPlanByIdRequest request);
	
	@WriteMethod
	@ResponseType(CreateOfficialSendPlanResponse.class)
	ListenableFuture<CreateOfficialSendPlanResponse> createOfficialSendPlan(AdminHead head, CreateOfficialSendPlanRequest request);
	
	@WriteMethod
	@ResponseType(CancelOfficialSendPlanResponse.class)
	ListenableFuture<CancelOfficialSendPlanResponse> cancelOfficialSendPlan(AdminHead head, CancelOfficialSendPlanRequest request);
	
	@ResponseType(GetOfficialSendPlanListResponse.class)
	ListenableFuture<GetOfficialSendPlanListResponse> getOfficialSendPlanList(AdminHead head, GetOfficialSendPlanListRequest request);
	
	@ResponseType(GetOfficialMessageResponse.class)
	ListenableFuture<GetOfficialMessageResponse> getOfficialMessage(AdminHead head, GetOfficialMessageRequest request);
	
	@WriteMethod
	@ResponseType(SendSecretaryMessageResponse.class)
	ListenableFuture<SendSecretaryMessageResponse> sendSecretaryMessage(AdminHead head, SendSecretaryMessageRequest request);
	
	@WriteMethod
	@ResponseType(SendSecretaryMessageResponse.class)
	ListenableFuture<SendSecretaryMessageResponse> sendSecretaryMessage(RequestHead head, SendSecretaryMessageRequest request);
	
	@WriteMethod
	@ResponseType(SendSecretaryMessageResponse.class)
	ListenableFuture<SendSecretaryMessageResponse> sendSecretaryMessage(SystemHead head, SendSecretaryMessageRequest request);
}
