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
import com.weizhu.proto.AdminUserProtos.GetUserByMobileNoUniqueRequest;
import com.weizhu.proto.AdminUserProtos.GetUserByMobileNoUniqueResponse;
import com.weizhu.proto.AdminUserService;
import com.weizhu.proto.AllowProtos;
import com.weizhu.proto.AllowProtos.CreateModelRequest;
import com.weizhu.proto.AllowProtos.CreateModelResponse;
import com.weizhu.proto.AllowService;
import com.weizhu.proto.UserProtos;
import com.weizhu.web.ParamUtil;

@Singleton
public class ImportUserRuleServlet extends HttpServlet {

	private static final long serialVersionUID = 1L;

	private final Provider<AdminHead> adminHeadProvider; 
	private final AdminUserService adminUserService;
	private final AllowService allowService;
	
	@Inject
	public ImportUserRuleServlet(AdminUserService adminUserService, AllowService allowService, Provider<AdminHead> adminHeadProvider) {
		this.adminUserService = adminUserService;
		this.allowService = allowService;
		this.adminHeadProvider = adminHeadProvider;
	}
	
	@Override
	public void doPost(HttpServletRequest httpRequest, HttpServletResponse httpResponse) throws IOException {
		final String mobileNoStr = ParamUtil.getString(httpRequest, "mobile_no_list", "");
		if (mobileNoStr.isEmpty()) {
			JsonObject result = new JsonObject();
			result.addProperty("result", "FAIL_MOBILE_NO_INVALID");
			result.addProperty("fail_text", "传入的手机号信息不正确！");

			httpResponse.setContentType("application/json;charset=UTF-8");
			JsonUtil.GSON.toJson(result, httpResponse.getWriter());
			return ;
		}
		List<String> mobileNoList = new ArrayList<String>();
		try {
			for (String mobileNo : DBUtil.COMMA_SPLITTER.splitToList(mobileNoStr)) {
				mobileNoList.add(mobileNo);
			}
		} catch (Exception ex) {
			JsonObject result = new JsonObject();
			result.addProperty("result", "FAIL_MOBILE_NO_INVALID");
			result.addProperty("fail_text", "传入的用户信息不正确！");

			httpResponse.setContentType("application/json;charset=UTF-8");
			JsonUtil.GSON.toJson(result, httpResponse.getWriter());
			return ;
		}
		
		final AdminHead adminHead = adminHeadProvider.get();
		
		final String modelName = ParamUtil.getString(httpRequest, "model_name", "人员导入模型");
		final String defaultActionStr = ParamUtil.getString(httpRequest, "default_action", "DENY");
		AllowProtos.Action defaultAction = null;
		for (AllowProtos.Action action : AllowProtos.Action.values()) {
			if (action.name().equals(defaultActionStr)) {
				defaultAction = action;
				break;
			}
		}
		
		List<AllowProtos.Rule> ruleList = new ArrayList<AllowProtos.Rule>();
		
		GetUserByMobileNoUniqueRequest.Builder getUserByMobileNoUniqueRequestBuilder = GetUserByMobileNoUniqueRequest.newBuilder();
		List<Long> userIdList = new ArrayList<Long>();
		
		int times = (mobileNoList.size() / 100) + 1;
		for (int i = 0; i < times; i ++) {
			getUserByMobileNoUniqueRequestBuilder.clear();
			
			List<String> mobileList = null;
			if (i + 1 == times) {
				mobileList = mobileNoList.subList(i * 100, mobileNoList.size());
			} else {
				mobileList = mobileNoList.subList(i * 100, (i + 1) * 100);
			}
			
			GetUserByMobileNoUniqueResponse response = Futures.getUnchecked(adminUserService.getUserByMobileNoUnique(adminHead, 
					getUserByMobileNoUniqueRequestBuilder.addAllMobileNo(mobileList).build()));
			userIdList.clear();
			for (UserProtos.User user : response.getUserList()) {
				userIdList.add(user.getBase().getUserId());
			}
			
			AllowProtos.UserRule userRule = AllowProtos.UserRule.newBuilder()
					.addAllUserId(userIdList)
					.build();

			AllowProtos.Rule rule = AllowProtos.Rule.newBuilder()
					.setAction(AllowProtos.Action.ALLOW)
					.setUserRule(userRule)
					.setRuleId(0)
					.setRuleName("人员导入模型" + i)
					.build();
			
			ruleList.add(rule);
		}
		
		CreateModelResponse response = Futures.getUnchecked(allowService.createModel(adminHead, CreateModelRequest.newBuilder()
				.setModelName(modelName)
				.setDefaultAction(defaultAction)
				.addAllRule(ruleList)
				.build()));
		
		httpResponse.setContentType("application/json;charset=UTF-8");
		JsonUtil.PROTOBUF_JSON_FORMAT.print(response, httpResponse.getWriter());
	}
	
	@Override
	public void doGet(HttpServletRequest httpRequest, HttpServletResponse httpResponse) throws IOException {
		this.doPost(httpRequest, httpResponse);
	}
	
}
