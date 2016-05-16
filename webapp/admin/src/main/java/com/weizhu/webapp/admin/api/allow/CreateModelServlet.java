package com.weizhu.webapp.admin.api.allow;

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
import com.weizhu.proto.AdminProtos.AdminHead;
import com.weizhu.proto.AllowProtos;
import com.weizhu.proto.AllowProtos.CreateModelRequest;
import com.weizhu.proto.AllowProtos.CreateModelResponse;
import com.weizhu.proto.AllowService;
import com.weizhu.web.ParamUtil;

@Singleton
public class CreateModelServlet extends HttpServlet {

	private static final long serialVersionUID = 1L;

	private final Provider<AdminHead> adminHeadProvider;
	private final AllowService allowService;
	
	@Inject
	public CreateModelServlet(Provider<AdminHead> adminHeadProvider, AllowService allowService) {
		this.adminHeadProvider = adminHeadProvider;
		this.allowService = allowService;
	}
	
	@Override
	public void doPost(HttpServletRequest httpRequest, HttpServletResponse httpResponse) throws IOException {
		final String defaultActionName = ParamUtil.getString(httpRequest, "default_action", "");
		AllowProtos.Action defaultAction = null;
		for (AllowProtos.Action action : AllowProtos.Action.values()) {
			if (action.name().equals(defaultActionName)) {
				defaultAction = action;
			}
		}
		if (defaultAction == null) {
			JsonObject result = new JsonObject();
			result.addProperty("result", "FAIL_DEFAULT_ACTION_INVALID");
			result.addProperty("fail_text", "默认状态不正确！");

			httpResponse.setContentType("application/json;charset=UTF-8");
			JsonUtil.GSON.toJson(result, httpResponse.getWriter());
			return ;
		}
		
		/**
		 * RULE的json格式报文
		 * 
		 * "rule": [{"rule_id": 0,"rule_name": "1111","action": "ALLOW","position_rule": {"position_id": [1]}},
		 *          {"rule_id": 1,"rule_name": "2222","action": "ALLOW","team_rule": {"team_id": [1]}}
		 *         ]
		 */
		final String ruleListStr = ParamUtil.getString(httpRequest, "rule_list", "");
		
		final String modelName = ParamUtil.getString(httpRequest, "model_name", "");
		CreateModelRequest.Builder createModelRequestBuilder = CreateModelRequest.newBuilder()
				.setModelName(modelName)
				.setDefaultAction(defaultAction);
		
		try {
			JsonUtil.PROTOBUF_JSON_FORMAT.merge(ruleListStr, ExtensionRegistry.getEmptyRegistry(), createModelRequestBuilder);
		} catch (Exception ex) {
			JsonObject result = new JsonObject();
			result.addProperty("result", "FAIL_RULE_INVALID");
			result.addProperty("fail_text", "传入的规则格式不正确");

			httpResponse.setContentType("application/json;charset=UTF-8");
			JsonUtil.GSON.toJson(result, httpResponse.getWriter());
			return ;
		}
		
		AdminHead head = this.adminHeadProvider.get();
		
		CreateModelResponse createModelResponse = Futures.getUnchecked(allowService.createModel(head, createModelRequestBuilder.build()));
		
		httpResponse.setContentType("application/json;charset=UTF-8");
		JsonUtil.PROTOBUF_JSON_FORMAT.print(createModelResponse, httpResponse.getWriter());
	}
	
	@Override
	public void doGet(HttpServletRequest httpRequest, HttpServletResponse httpResponse) throws IOException {
		this.doPost(httpRequest, httpResponse);
	}
}
