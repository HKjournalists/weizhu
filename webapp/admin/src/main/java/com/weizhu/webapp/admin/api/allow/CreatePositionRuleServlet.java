package com.weizhu.webapp.admin.api.allow;

import java.io.IOException;
import java.util.Iterator;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.common.base.Splitter;
import com.google.common.collect.Iterables;
import com.google.common.primitives.Ints;
import com.google.common.util.concurrent.Futures;
import com.google.gson.JsonObject;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.weizhu.common.utils.JsonUtil;
import com.weizhu.proto.AdminProtos.AdminHead;
import com.weizhu.proto.AllowProtos;
import com.weizhu.proto.AllowProtos.CreatePositionRuleRequest;
import com.weizhu.proto.AllowProtos.CreatePositionRuleResponse;
import com.weizhu.proto.AllowService;
import com.weizhu.web.ParamUtil;

@Singleton
public class CreatePositionRuleServlet extends HttpServlet {

	private static final long serialVersionUID = 1L;

	private final Provider<AdminHead> adminHeadProvider;
	private final AllowService allowService;
	
	@Inject
	public CreatePositionRuleServlet(Provider<AdminHead> adminHeadProvider, AllowService allowService) {
		this.adminHeadProvider = adminHeadProvider;
		this.allowService = allowService;
	}

	private static final Splitter POSITION_ID_FIELD_SPLITTER = Splitter.on(",").trimResults().omitEmptyStrings();
	
	@Override
	public void doPost(HttpServletRequest httpRequest, HttpServletResponse httpResponse) throws IOException {
		final int modelId = ParamUtil.getInt(httpRequest, "model_id", -1);
		final String ruleName = ParamUtil.getString(httpRequest, "rule_name", "");
		
		final String positionIdStr = ParamUtil.getString(httpRequest, "position_id", "");
		CreatePositionRuleRequest.Builder createPositionRuleRequestBuilder = CreatePositionRuleRequest.newBuilder()
				.setModelId(modelId)
				.setRuleName(ruleName);
		
		final String ruleAction = ParamUtil.getString(httpRequest, "rule_action", "ALLOW");
		for (AllowProtos.Action action : AllowProtos.Action.values()) {
			if (action.name().equals(ruleAction)) {
				createPositionRuleRequestBuilder.setRuleAction(action);
				break;
			}
		}
				
		try {
			Iterator<Integer> it = Iterables.transform(POSITION_ID_FIELD_SPLITTER.split(positionIdStr), Ints.stringConverter()).iterator();
			
			if (it.hasNext()) {
				createPositionRuleRequestBuilder.addPositionId(it.next());
			}
		} catch (Exception ex) {
			JsonObject result = new JsonObject();
			result.addProperty("result", "FAIL_POSITION_INVALID");
			result.addProperty("fail_text", "传入的职位信息不正确！");

			httpResponse.setContentType("application/json;charset=UTF-8");
			JsonUtil.GSON.toJson(result, httpResponse.getWriter());
			return ;
		}
		
		AdminHead head = this.adminHeadProvider.get();
		
		CreatePositionRuleResponse createPositionRuleResponse = Futures.getUnchecked(allowService.createPositionRule(head, createPositionRuleRequestBuilder.build()));
		
		httpResponse.setContentType("application/json;charset=UTF-8");
		JsonUtil.PROTOBUF_JSON_FORMAT.print(createPositionRuleResponse, httpResponse.getWriter());
	}
	
	@Override
	public void doGet(HttpServletRequest httpRequest, HttpServletResponse httpResponse) throws IOException {
		this.doPost(httpRequest, httpResponse);
	}
}
