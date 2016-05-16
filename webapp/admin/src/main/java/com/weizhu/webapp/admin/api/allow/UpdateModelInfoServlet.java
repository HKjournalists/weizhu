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
import com.weizhu.proto.AllowService;
import com.weizhu.proto.AdminProtos.AdminHead;
import com.weizhu.proto.AllowProtos.UpdateModelRequest;
import com.weizhu.proto.AllowProtos.UpdateModelResponse;
import com.weizhu.proto.AllowProtos.UpdateModelRuleOrderRequest;
import com.weizhu.proto.AllowProtos.UpdateModelRuleOrderResponse;
import com.weizhu.web.ParamUtil;

/**
 * 此类包括了更新模型名称和规则顺序
 */
@Singleton
public class UpdateModelInfoServlet extends HttpServlet {

	private static final long serialVersionUID = 1L;

	private final Provider<AdminHead> adminHeadProvider;
	private final AllowService allowService;
	
	@Inject
	public UpdateModelInfoServlet(Provider<AdminHead> adminHeadProvider, AllowService allowService) {
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
		
		final String ruleIdStr = ParamUtil.getString(httpRequest, "rule_id", "");
		List<Integer> ruleIdList = new ArrayList<Integer>();
		try {
			List<String> ruleIdStrList = DBUtil.COMMA_SPLITTER.splitToList(ruleIdStr);
			for (String ruleId : ruleIdStrList) {
				ruleIdList.add(Integer.parseInt(ruleId));
			}
			
		} catch (Exception ex) {
			JsonObject result = new JsonObject();
			result.addProperty("result", "FAIL_MODEL_INVALID");
			result.addProperty("fail_text", "传入的规则不正确！");

			httpResponse.setContentType("application/json;charset=UTF-8");
			JsonUtil.GSON.toJson(result, httpResponse.getWriter());
			return ;
		}
		
		AdminHead head = this.adminHeadProvider.get();
		
		final String modelName = ParamUtil.getString(httpRequest, "model_name", "");
		final String defaultActionName = ParamUtil.getString(httpRequest, "default_action", "");
		AllowProtos.Action defaultAction = null;
		for (AllowProtos.Action action : AllowProtos.Action.values()) {
			if (action.name().equals(defaultActionName)) {
				defaultAction = action;
			}
		}
		
		UpdateModelRequest updateModelRequest = UpdateModelRequest.newBuilder()
				.setDefaultAction(defaultAction)
				.setModelName(modelName)
				.setModelId(modelId)
				.build();
		UpdateModelResponse updateModelResponse = Futures.getUnchecked(allowService.updateModel(head, updateModelRequest));
		if (!updateModelResponse.getResult().name().equals("SUCC")) {
			httpResponse.setContentType("application/json;charset=UTF-8");
			JsonUtil.PROTOBUF_JSON_FORMAT.print(updateModelResponse, httpResponse.getWriter());
			return ;
		}
		
		UpdateModelRuleOrderRequest updateModelRuleOrderRequest = UpdateModelRuleOrderRequest.newBuilder()
				.setModelId(modelId)
				.addAllRuleId(ruleIdList)
				.build();
		UpdateModelRuleOrderResponse updateModelRuleOrderResponse = Futures.getUnchecked(allowService.updateModelRuleOrder(head, updateModelRuleOrderRequest));
		
		httpResponse.setContentType("application/json;charset=UTF-8");
		JsonUtil.PROTOBUF_JSON_FORMAT.print(updateModelRuleOrderResponse, httpResponse.getWriter());
	}
	
	@Override
	public void doGet(HttpServletRequest httpRequest, HttpServletResponse httpResponse) throws IOException {
		this.doPost(httpRequest, httpResponse);
	}
}
