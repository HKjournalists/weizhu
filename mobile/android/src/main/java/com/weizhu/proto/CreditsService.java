package com.weizhu.proto;

import com.weizhu.network.Future;
import com.weizhu.proto.CreditsProtos.DuibaShopUrlRequest;
import com.weizhu.proto.CreditsProtos.DuibaShopUrlResponse;
import com.weizhu.proto.CreditsProtos.GetCreditsOrderRequest;
import com.weizhu.proto.CreditsProtos.GetCreditsOrderResponse;
import com.weizhu.proto.CreditsProtos.GetCreditsResponse;
import com.weizhu.proto.CreditsProtos.GetCreditsRuleResponse;
import com.weizhu.proto.WeizhuProtos.EmptyRequest;

public interface CreditsService {

	@ResponseType(DuibaShopUrlResponse.class)
	Future<DuibaShopUrlResponse> duibaShopUrl(DuibaShopUrlRequest request, int priorityNum);
	
	@ResponseType(GetCreditsResponse.class)
	Future<GetCreditsResponse> getCredits(EmptyRequest request, int priorityNum);
	
	@ResponseType(GetCreditsOrderResponse.class)
	Future<GetCreditsOrderResponse> getCreditsOrder(GetCreditsOrderRequest request, int priorityNum);
	
	@ResponseType(GetCreditsRuleResponse.class)
	Future<GetCreditsRuleResponse> getCreditsRule(EmptyRequest request, int priorityNum);
	
}
