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
import com.weizhu.proto.AdminProtos.AdminHead;
import com.weizhu.proto.AllowProtos.DeleteRuleRequest;
import com.weizhu.proto.AllowProtos.DeleteRuleResponse;
import com.weizhu.proto.AllowService;
import com.weizhu.web.ParamUtil;

@Singleton
public class DeleteRuleServlet extends HttpServlet {

	private static final long serialVersionUID = 1L;

	private final Provider<AdminHead> adminHeadProvider;
	private final AllowService allowService;
	
	@Inject
	public DeleteRuleServlet(Provider<AdminHead> adminHeadProvider, AllowService allowService) {
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
		
		DeleteRuleRequest deleteRuleRequest = DeleteRuleRequest.newBuilder()
				.setModelId(modelId)
				.addAllRuleId(ruleIdList)
				.build();
		DeleteRuleResponse deleteRuleResponse = Futures.getUnchecked(allowService.deleteRule(head, deleteRuleRequest));
		
		httpResponse.setContentType("application/json;charset=UTF-8");
		JsonUtil.PROTOBUF_JSON_FORMAT.print(deleteRuleResponse, httpResponse.getWriter());
	}
	
	@Override
	public void doGet(HttpServletRequest httpRequest, HttpServletResponse httpResponse) throws IOException {
		this.doPost(httpRequest, httpResponse);
	}
}
