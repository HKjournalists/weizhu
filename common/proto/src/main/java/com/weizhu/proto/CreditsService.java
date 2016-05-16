package com.weizhu.proto;

import com.google.common.util.concurrent.ListenableFuture;
import com.weizhu.proto.CreditsProtos.GetCreditsRuleResponse;
import com.weizhu.proto.CreditsProtos.DuibaConsumeCreditsRequest;
import com.weizhu.proto.CreditsProtos.DuibaConsumeCreditsResponse;
import com.weizhu.proto.CreditsProtos.DuibaNotifyRequest;
import com.weizhu.proto.CreditsProtos.DuibaNotifyResponse;
import com.weizhu.proto.CreditsProtos.DuibaShopUrlRequest;
import com.weizhu.proto.CreditsProtos.DuibaShopUrlResponse;
import com.weizhu.proto.CreditsProtos.GetCreditsOrderRequest;
import com.weizhu.proto.CreditsProtos.GetCreditsOrderResponse;
import com.weizhu.proto.CreditsProtos.GetCreditsResponse;
import com.weizhu.proto.WeizhuProtos.AnonymousHead;
import com.weizhu.proto.WeizhuProtos.EmptyRequest;
import com.weizhu.proto.WeizhuProtos.RequestHead;

public interface CreditsService {

	@ResponseType(DuibaShopUrlResponse.class)
	ListenableFuture<DuibaShopUrlResponse> duibaShopUrl(RequestHead head, DuibaShopUrlRequest request);
	
	@WriteMethod
	@ResponseType(DuibaConsumeCreditsResponse.class)
	ListenableFuture<DuibaConsumeCreditsResponse> duibaConsumeCredits(AnonymousHead head, DuibaConsumeCreditsRequest request);
	
	@WriteMethod
	@ResponseType(DuibaNotifyResponse.class)
	ListenableFuture<DuibaNotifyResponse> duibaNotify(AnonymousHead head, DuibaNotifyRequest request);
	
	@ResponseType(GetCreditsResponse.class)
	ListenableFuture<GetCreditsResponse> getCredits(RequestHead head, EmptyRequest request);
	
	@ResponseType(GetCreditsOrderResponse.class)
	ListenableFuture<GetCreditsOrderResponse> getCreditsOrder(RequestHead head, GetCreditsOrderRequest request);
	
	@ResponseType(GetCreditsRuleResponse.class)
	ListenableFuture<GetCreditsRuleResponse> getCreditsRule(RequestHead head, EmptyRequest request);
}
