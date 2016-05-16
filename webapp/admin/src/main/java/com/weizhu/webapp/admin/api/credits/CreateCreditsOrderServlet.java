package com.weizhu.webapp.admin.api.credits;

import java.io.IOException;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.common.util.concurrent.Futures;
import com.google.gson.JsonObject;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.google.protobuf.ExtensionRegistry;
import com.weizhu.common.utils.JsonUtil;
import com.weizhu.proto.AdminCreditsProtos.CreateCreditsOrderRequest;
import com.weizhu.proto.AdminCreditsProtos.CreateCreditsOrderResponse;
import com.weizhu.proto.AdminCreditsService;
import com.weizhu.proto.AdminProtos.AdminHead;
import com.weizhu.web.ParamUtil;

@Singleton
public class CreateCreditsOrderServlet extends HttpServlet {

	private static final long serialVersionUID = 1L;

	private final Provider<AdminHead> adminHeadProvider;
	private final AdminCreditsService adminCreditsService;
	
	@Inject
	public CreateCreditsOrderServlet(Provider<AdminHead> adminHeadProvider, AdminCreditsService adminCreditsService) {
		this.adminHeadProvider = adminHeadProvider;
		this.adminCreditsService = adminCreditsService;
	}
	
	@Override
	public void doPost(HttpServletRequest httpRequest, HttpServletResponse httpResponse) throws IOException {
		final String userCreditsDelta = ParamUtil.getString(httpRequest, "param", "");
		final String desc = ParamUtil.getString(httpRequest, "desc", "");
		
		CreateCreditsOrderRequest.Builder requestBuilder = CreateCreditsOrderRequest.newBuilder();
		// 示例："{\"user_credits_delta\": [{\"credits_delta\": 10,\"user_id\": 1},{\"credits_delta\": 20,\"user_id\": 2},{\"credits_delta\": 30,\"user_id\": 3}]}"
		try {
			JsonUtil.PROTOBUF_JSON_FORMAT.merge(userCreditsDelta, ExtensionRegistry.getEmptyRegistry(), requestBuilder);
		} catch (Exception ex) {
			JsonObject result = new JsonObject();
			result.addProperty("result", "FAIL_USER_CREDITS_INVALID");
			result.addProperty("fail_text", "请传入正确的用户积分信息");

			httpResponse.setContentType("application/json;charset=UTF-8");
			JsonUtil.GSON.toJson(result, httpResponse.getWriter());
			return ;
		}
		
		requestBuilder.setDesc(desc);
		
		final AdminHead head = adminHeadProvider.get();
		
		CreateCreditsOrderResponse response = Futures.getUnchecked(adminCreditsService.createCreditsOrder(head, requestBuilder.build()));
	
		httpResponse.setContentType("application/json;charset=UTF-8");
		JsonUtil.PROTOBUF_JSON_FORMAT.print(response, httpResponse.getWriter());
	}
	
	@Override
	public void doGet(HttpServletRequest httpRequest, HttpServletResponse httpResponse) throws IOException {
		this.doPost(httpRequest, httpResponse);
	}
}
