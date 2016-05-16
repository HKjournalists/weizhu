package com.weizhu.webapp.admin.api.allow;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.common.util.concurrent.Futures;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.weizhu.common.db.DBUtil;
import com.weizhu.common.utils.JsonUtil;
import com.weizhu.proto.AdminProtos.AdminHead;
import com.weizhu.proto.AllowProtos.CheckAllowRequest;
import com.weizhu.proto.AllowProtos.CheckAllowResponse;
import com.weizhu.proto.AllowProtos.CheckAllowResponse.CheckResult;
import com.weizhu.proto.AllowService;
import com.weizhu.web.ParamUtil;

@Singleton
public class CheckAllowServlet extends HttpServlet {

	private static final long serialVersionUID = 1L;
	
	private final Provider<AdminHead> adminHeadProvider;
	private final AllowService allowService;

	@Inject
	public CheckAllowServlet(Provider<AdminHead> adminHeadProvider, AllowService allowService) {
		this.adminHeadProvider = adminHeadProvider;
		this.allowService = allowService;
	}
	
	@Override
	public void doPost(HttpServletRequest httpRequest, HttpServletResponse httpResponse) throws IOException {
		final String userIdStr = ParamUtil.getString(httpRequest, "user_id", "");
		final String modelIdStr = ParamUtil.getString(httpRequest, "model_id", "");
		
		List<String> userIdStrList = DBUtil.COMMA_SPLITTER.splitToList(userIdStr);
		List<String> modelIdStrList = DBUtil.COMMA_SPLITTER.splitToList(modelIdStr);
		
		List<Long> userIdList = new ArrayList<Long>();
		List<Integer> modelIdList = new ArrayList<Integer>();
		
		try {
			for (String userId : userIdStrList) {
				userIdList.add(Long.parseLong(userId));
			}
		} catch (Exception ex) {
			JsonObject result = new JsonObject();
			result.addProperty("result", "FAIL_USER_INVALID");
			result.addProperty("fail_text", "传入的用户不正确！");

			httpResponse.setContentType("application/json;charset=UTF-8");
			JsonUtil.GSON.toJson(result, httpResponse.getWriter());
			return ;
		}
		try {
			for (String modelId : modelIdStrList) {
				modelIdList.add(Integer.parseInt(modelId));
			}
		} catch (Exception ex) {
			JsonObject result = new JsonObject();
			result.addProperty("result", "FAIL_MODEL_INVALID");
			result.addProperty("fail_text", "传入的模型不正确！");

			httpResponse.setContentType("application/json;charset=UTF-8");
			JsonUtil.GSON.toJson(result, httpResponse.getWriter());
			return ;
		}
		
		final AdminHead head = this.adminHeadProvider.get();
		
		CheckAllowRequest checkAllowRequest = CheckAllowRequest.newBuilder()
				.addAllModelId(modelIdList)
				.addAllUserId(userIdList)
				.build();
		CheckAllowResponse checkAllowResponse = Futures.getUnchecked(allowService.checkAllow(head, checkAllowRequest));
		
		List<CheckResult> checkResultList = checkAllowResponse.getCheckResultList();
		JsonArray resultArray = new JsonArray();
		for (CheckResult checkResult : checkResultList) {
			JsonObject tmpCheckResult = new JsonObject();
			tmpCheckResult.addProperty("model_id", checkResult.getModelId());
			JsonArray userArray = new JsonArray();
			for (long userId : checkResult.getAllowUserIdList()) {
				JsonObject userJson = new JsonObject();
				userJson.addProperty("user_id", userId);
				userArray.add(userJson);
			}
			tmpCheckResult.add("user_ids", userArray);
			resultArray.add(tmpCheckResult);
		}
		
		JsonObject result = new JsonObject();
		result.addProperty("result", "SUCC");
		result.add("allow_user", resultArray);
		
		httpResponse.setContentType("application/json;charset=UTF-8");
		JsonUtil.GSON.toJson(result, httpResponse.getWriter());
	}
	
	@Override
	public void doGet(HttpServletRequest httpRequest, HttpServletResponse httpResponse) throws IOException {
		this.doPost(httpRequest, httpResponse);
	}
}
