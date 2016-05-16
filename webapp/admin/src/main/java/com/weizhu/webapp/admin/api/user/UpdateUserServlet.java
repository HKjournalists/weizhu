package com.weizhu.webapp.admin.api.user;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.common.util.concurrent.Futures;
import com.google.gson.JsonObject;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.weizhu.common.utils.JsonUtil;
import com.weizhu.proto.AdminUserService;
import com.weizhu.proto.UserProtos;
import com.weizhu.proto.AdminProtos.AdminHead;
import com.weizhu.proto.AdminUserProtos.UpdateUserRequest;
import com.weizhu.proto.AdminUserProtos.UpdateUserResponse;
import com.weizhu.web.ParamUtil;

@Singleton
@SuppressWarnings("serial")
public class UpdateUserServlet extends HttpServlet {
	
	private final Provider<AdminHead> adminHeadProvider;
	private final AdminUserService adminUserService;
	
	@Inject
	public UpdateUserServlet(Provider<AdminHead> adminHeadProvider, AdminUserService adminUserService) {
		this.adminHeadProvider = adminHeadProvider;
		this.adminUserService = adminUserService;
	}
	
	@Override
	protected void doGet(HttpServletRequest httpRequest, HttpServletResponse httpResponse) throws ServletException, IOException {
		this.doPost(httpRequest, httpResponse);
	}

	@Override
	protected void doPost(HttpServletRequest httpRequest, HttpServletResponse httpResponse) throws ServletException, IOException {
		// 1. 取出参数
		long userId = ParamUtil.getLong(httpRequest, "user_id", -1L);
		String userName = ParamUtil.getString(httpRequest, "user_name", "");
		String genderStr = ParamUtil.getString(httpRequest, "gender", "");
		List<String> mobileNoList = ParamUtil.getStringList(httpRequest, "mobile_no", Collections.<String>emptyList());
		List<String> phoneNoList = ParamUtil.getStringList(httpRequest, "phone_no", Collections.<String>emptyList());
		String email = ParamUtil.getString(httpRequest, "email", "");
		Boolean isExpert = ParamUtil.getBoolean(httpRequest, "is_expert", null);
		Integer levelId = ParamUtil.getInt(httpRequest, "level_id", null);
		Integer teamId = ParamUtil.getInt(httpRequest, "team_id", null);
		Integer positionId = ParamUtil.getInt(httpRequest, "position_id", null);
		String stateStr = ParamUtil.getString(httpRequest, "state", "");
		
		// 2. 调用Service
		
		final AdminHead head = adminHeadProvider.get();
		
		UpdateUserRequest.Builder requestBuilder = UpdateUserRequest.newBuilder();
		requestBuilder.setUserId(userId);
		requestBuilder.setUserName(userName);
		if (!genderStr.isEmpty()) {
			for (UserProtos.UserBase.Gender gender : UserProtos.UserBase.Gender.values()) {
				if (gender.name().equals(genderStr)) {
					requestBuilder.setGender(gender);
					break;
				}
			}
		}
		requestBuilder.addAllMobileNo(mobileNoList);
		requestBuilder.addAllPhoneNo(phoneNoList);
		if (!email.isEmpty()) {
			requestBuilder.setEmail(email);
		}
		if (isExpert != null) {
			requestBuilder.setIsExpert(isExpert);
		}
		if (levelId != null) {
			requestBuilder.setLevelId(levelId);
		}
		if (teamId != null) {
			UserProtos.UserTeam.Builder userTeamBuilder = UserProtos.UserTeam.newBuilder();
			userTeamBuilder.setUserId(userId);
			userTeamBuilder.setTeamId(teamId);
			if (positionId != null) {
				userTeamBuilder.setPositionId(positionId);
			}
			requestBuilder.addUserTeam(userTeamBuilder.build());
		}
		
		if (!stateStr.isEmpty()) {
			for (UserProtos.UserBase.State state : UserProtos.UserBase.State.values()) {
				if (state.name().equals(stateStr)) {
					requestBuilder.setState(state);
					break;
				}
			}
			
			if (!requestBuilder.hasState()) {
				JsonObject ret = new JsonObject();
				ret.addProperty("result", "FAIL_STATE_INVALID");
				ret.addProperty("fail_text", "用户状态错误");
				
				httpResponse.setContentType("application/json;charset=UTF-8");
				JsonUtil.GSON.toJson(ret, httpResponse.getWriter());
				return;
			}
		}
		
		UpdateUserResponse response = Futures.getUnchecked(adminUserService.updateUser(head, requestBuilder.build()));
		
		httpResponse.setContentType("application/json;charset=UTF-8");
		JsonUtil.PROTOBUF_JSON_FORMAT.print(response, httpResponse.getWriter());
	}

}
