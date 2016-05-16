package com.weizhu.webapp.admin.api.allow;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.common.util.concurrent.Futures;
import com.google.gson.JsonObject;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.weizhu.common.db.DBUtil;
import com.weizhu.common.utils.JsonUtil;
import com.weizhu.proto.AllowProtos;
import com.weizhu.proto.AllowProtos.UpdatePositionRuleResponse;
import com.weizhu.proto.AllowService;
import com.weizhu.proto.AdminProtos.AdminHead;
import com.weizhu.proto.AllowProtos.UpdatePositionRuleRequest;
import com.weizhu.web.ParamUtil;

@Singleton
public class UpdatePositionRuleServlet extends HttpServlet {

	private static final long serialVersionUID = 1L;

	private final Provider<AdminHead> adminHeadProvider;
	private final AllowService allowService;
	
	@Inject
	public UpdatePositionRuleServlet(Provider<AdminHead> adminHeadProvider, AllowService allowService) {
		this.adminHeadProvider = adminHeadProvider;
		this.allowService = allowService;
	}
	
	@Override
	public void doPost(HttpServletRequest httpRequest, HttpServletResponse httpResponse) throws IOException {
		final Integer modelId = ParamUtil.getInt(httpRequest, "model_id", null);
		if (modelId == null) {
			JsonObject result = new JsonObject();
			result.addProperty("result", "FAIL_MODEL_INVALID");
			result.addProperty("fail_text", "传入的模型不正确！");

			httpResponse.setContentType("application/json;charset=UTF-8");
			JsonUtil.GSON.toJson(result, httpResponse.getWriter());
			return ;
		}
		
		final Integer ruleId = ParamUtil.getInt(httpRequest, "rule_id", null);
		if (ruleId == null) {
			JsonObject result = new JsonObject();
			result.addProperty("result", "FAIL_RULE_INVALID");
			result.addProperty("fail_text", "传入的规则不正确！");

			httpResponse.setContentType("application/json;charset=UTF-8");
			JsonUtil.GSON.toJson(result, httpResponse.getWriter());
			return ;
		}
		
		final String positionIdStr = ParamUtil.getString(httpRequest, "position_id", "");
		List<Integer> positionIdList = new ArrayList<Integer>();
		try {
			List<String> positionIdStrList = DBUtil.COMMA_SPLITTER.splitToList(positionIdStr);
			for (String positionId : positionIdStrList) {
				positionIdList.add(Integer.parseInt(positionId));
			}
			
		} catch (Exception ex) {
			JsonObject result = new JsonObject();
			result.addProperty("result", "FAIL_POSITION_INVALID");
			result.addProperty("fail_text", "传入的职位不正确！");

			httpResponse.setContentType("application/json;charset=UTF-8");
			JsonUtil.GSON.toJson(result, httpResponse.getWriter());
			return ;
		}
		
		final String ruleName = ParamUtil.getString(httpRequest, "rule_name", "");
		final String actionName = ParamUtil.getString(httpRequest, "rule_action", "");
		AllowProtos.Action defaultAction = null;
		for (AllowProtos.Action action : AllowProtos.Action.values()) {
			if (action.name().equals(actionName)) {
				defaultAction = action;
			}
		}
		
		AdminHead head = this.adminHeadProvider.get();
		
		UpdatePositionRuleRequest updatePositionRuleRequest = UpdatePositionRuleRequest.newBuilder()
				.setModelId(modelId)
				.setRuleId(ruleId)
				.setRuleName(ruleName)
				.setRuleAction(defaultAction)
				.addAllPositionId(positionIdList)
				.build();
		UpdatePositionRuleResponse updatePositionRuleResponse = Futures.getUnchecked(allowService.updatePositionRule(head, updatePositionRuleRequest));
		
		httpResponse.setContentType("application/json;charset=UTF-8");
		JsonUtil.PROTOBUF_JSON_FORMAT.print(updatePositionRuleResponse, httpResponse.getWriter());
	}

	@Override
	public void doGet(HttpServletRequest httpRequest, HttpServletResponse httpResponse) throws IOException {
		this.doPost(httpRequest, httpResponse);
	}
}
