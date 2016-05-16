package com.weizhu.proto;

import com.google.common.util.concurrent.ListenableFuture;
import com.weizhu.proto.AdminCreditsProtos.AddCreditsRequest;
import com.weizhu.proto.AdminCreditsProtos.AddCreditsResponse;
import com.weizhu.proto.AdminCreditsProtos.ClearUserCreditsRequest;
import com.weizhu.proto.AdminCreditsProtos.ClearUserCreditsResponse;
import com.weizhu.proto.AdminCreditsProtos.CreateCreditsOrderRequest;
import com.weizhu.proto.AdminCreditsProtos.CreateCreditsOrderResponse;
import com.weizhu.proto.AdminCreditsProtos.GetCreditsLogRequest;
import com.weizhu.proto.AdminCreditsProtos.GetCreditsLogResponse;
import com.weizhu.proto.AdminCreditsProtos.GetCreditsOperationRequest;
import com.weizhu.proto.AdminCreditsProtos.GetCreditsOperationResponse;
import com.weizhu.proto.AdminCreditsProtos.GetCreditsOrderRequest;
import com.weizhu.proto.AdminCreditsProtos.GetCreditsOrderResponse;
import com.weizhu.proto.AdminCreditsProtos.GetCreditsResponse;
import com.weizhu.proto.AdminCreditsProtos.GetCreditsRuleResponse;
import com.weizhu.proto.AdminCreditsProtos.GetExpenseCreditsResponse;
import com.weizhu.proto.AdminCreditsProtos.GetUserCreditsRequest;
import com.weizhu.proto.AdminCreditsProtos.GetUserCreditsResponse;
import com.weizhu.proto.AdminCreditsProtos.UpdateCreditsRuleRequest;
import com.weizhu.proto.AdminCreditsProtos.UpdateCreditsRuleResponse;
import com.weizhu.proto.AdminProtos.AdminHead;
import com.weizhu.proto.WeizhuProtos.EmptyRequest;

public interface AdminCreditsService {

	@ResponseType(GetCreditsResponse.class)
	ListenableFuture<GetCreditsResponse> getCredits(AdminHead head, EmptyRequest request);
	
	@WriteMethod
	@ResponseType(AddCreditsResponse.class)
	ListenableFuture<AddCreditsResponse> addCredits(AdminHead head, AddCreditsRequest request);
	
	@ResponseType(GetCreditsLogResponse.class)
	ListenableFuture<GetCreditsLogResponse> getCreditsLog(AdminHead head, GetCreditsLogRequest request);
	
	@ResponseType(GetUserCreditsResponse.class)
	ListenableFuture<GetUserCreditsResponse> getUserCredits(AdminHead head, GetUserCreditsRequest request);
	
	@ResponseType(GetCreditsOrderResponse.class)
	ListenableFuture<GetCreditsOrderResponse> getCreditsOrder(AdminHead head, GetCreditsOrderRequest request);
	
	@WriteMethod
	@ResponseType(CreateCreditsOrderResponse.class)
	ListenableFuture<CreateCreditsOrderResponse> createCreditsOrder(AdminHead head, CreateCreditsOrderRequest request);
	
	@WriteMethod
	@ResponseType(ClearUserCreditsResponse.class)
	ListenableFuture<ClearUserCreditsResponse> clearUserCredits(AdminHead head, ClearUserCreditsRequest request);
	
	@ResponseType(GetCreditsRuleResponse.class)
	ListenableFuture<GetCreditsRuleResponse> getCreditsRule(AdminHead head, EmptyRequest request);
	
	@WriteMethod
	@ResponseType(UpdateCreditsRuleResponse.class)
	ListenableFuture<UpdateCreditsRuleResponse> updateCreditsRule(AdminHead head, UpdateCreditsRuleRequest request);

	@ResponseType(GetCreditsOperationResponse.class)
	ListenableFuture<GetCreditsOperationResponse> getCreditsOperation(AdminHead head, GetCreditsOperationRequest request);

	@ResponseType(GetExpenseCreditsResponse.class)
	ListenableFuture<GetExpenseCreditsResponse> getExpenseCredits(AdminHead head, EmptyRequest request);
}
