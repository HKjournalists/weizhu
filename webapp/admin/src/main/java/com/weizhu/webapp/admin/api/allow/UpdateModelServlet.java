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
import com.weizhu.proto.AdminProtos.AdminHead;
import com.weizhu.common.utils.JsonUtil;
import com.weizhu.proto.AllowProtos;
import com.weizhu.proto.AllowProtos.UpdateModelRequest;
import com.weizhu.proto.AllowProtos.UpdateModelResponse;
import com.weizhu.proto.AllowService;
import com.weizhu.web.ParamUtil;

@Singleton
public class UpdateModelServlet extends HttpServlet {

	private static final long serialVersionUID = 1L;

	private final Provider<AdminHead> adminHeadProvider;
	private final AllowService allowService;
	
	@Inject
	public UpdateModelServlet(Provider<AdminHead> adminHeadProvider, AllowService allowService) {
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
		
		final String modelName = ParamUtil.getString(httpRequest, "model_name", "");
		final String defaultActionName = ParamUtil.getString(httpRequest, "default_action", "");
		AllowProtos.Action defaultAction = null;
		for (AllowProtos.Action action : AllowProtos.Action.values()) {
			if (action.name().equals(defaultActionName)) {
				defaultAction = action;
			}
		}
		
		AdminHead head = this.adminHeadProvider.get();
		UpdateModelRequest updateModelRequest = UpdateModelRequest.newBuilder()
				.setDefaultAction(defaultAction)
				.setModelName(modelName)
				.setModelId(modelId)
				.build();
		UpdateModelResponse updateModelResponse = Futures.getUnchecked(allowService.updateModel(head, updateModelRequest));
		
		httpResponse.setContentType("application/json;charset=UTF-8");
		JsonUtil.PROTOBUF_JSON_FORMAT.print(updateModelResponse, httpResponse.getWriter());
	}
	
	@Override
	public void doGet(HttpServletRequest httpRequest, HttpServletResponse httpResponse) throws IOException {
		this.doPost(httpRequest, httpResponse);
	}
}
