package com.weizhu.webapp.admin.api.allow;

import java.io.IOException;
import java.util.Iterator;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.common.base.Splitter;
import com.google.common.collect.Iterables;
import com.google.common.primitives.Longs;
import com.google.common.util.concurrent.Futures;
import com.google.gson.JsonObject;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.weizhu.proto.AllowProtos;
import com.weizhu.proto.AllowService;
import com.weizhu.common.utils.JsonUtil;
import com.weizhu.proto.AdminProtos.AdminHead;
import com.weizhu.proto.AllowProtos.CreateUserRuleRequest;
import com.weizhu.proto.AllowProtos.CreateUserRuleResponse;
import com.weizhu.web.ParamUtil;

@Singleton
public class CreateUserRuleServlet extends HttpServlet {

	private static final long serialVersionUID = 1L;

	private final Provider<AdminHead> adminHeadProvider;
	private final AllowService allowService;
	
	@Inject
	public CreateUserRuleServlet(Provider<AdminHead> adminHeadProvider, AllowService allowService) {
		this.adminHeadProvider = adminHeadProvider;
		this.allowService = allowService;
	}

	private static final Splitter USER_ID_FIELD_SPLITTER = Splitter.on(",").trimResults().omitEmptyStrings();
	
	@Override
	public void doPost(HttpServletRequest httpRequest, HttpServletResponse httpResponse) throws IOException {
		final int modelId = ParamUtil.getInt(httpRequest, "model_id", -1);
		final String ruleName = ParamUtil.getString(httpRequest, "rule_name", "");
		
		final String userIdStr = ParamUtil.getString(httpRequest, "user_id", "");
		CreateUserRuleRequest.Builder createUserRuleRequestBuilder = CreateUserRuleRequest.newBuilder()
				.setModelId(modelId)
				.setRuleName(ruleName);
		
		final String ruleAction = ParamUtil.getString(httpRequest, "rule_action", "ALLOW");
		for (AllowProtos.Action action : AllowProtos.Action.values()) {
			if (action.name().equals(ruleAction)) {
				createUserRuleRequestBuilder.setRuleAction(action);
				break;
			}
		}
				
		try {
			Iterator<Long> it = Iterables.transform(USER_ID_FIELD_SPLITTER.split(userIdStr), Longs.stringConverter()).iterator();
			
			if (it.hasNext()) {
				createUserRuleRequestBuilder.addUserId(it.next());
			}
		} catch (Exception ex) {
			JsonObject result = new JsonObject();
			result.addProperty("result", "FAIL_USER_INVALID");
			result.addProperty("fail_text", "传入的用户信息不正确！");

			httpResponse.setContentType("application/json;charset=UTF-8");
			JsonUtil.GSON.toJson(result, httpResponse.getWriter());
			return ;
		}
		
		AdminHead head = this.adminHeadProvider.get();
		
		CreateUserRuleResponse createUserRuleResponse = Futures.getUnchecked(allowService.createUserRule(head, createUserRuleRequestBuilder.build()));
		
		httpResponse.setContentType("application/json;charset=UTF-8");
		JsonUtil.PROTOBUF_JSON_FORMAT.print(createUserRuleResponse, httpResponse.getWriter());
	}
	
	@Override
	public void doGet(HttpServletRequest httpRequest, HttpServletResponse httpResponse) throws IOException {
		this.doPost(httpRequest, httpResponse);
	}
}
