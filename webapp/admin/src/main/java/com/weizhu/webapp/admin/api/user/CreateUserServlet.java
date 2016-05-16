package com.weizhu.webapp.admin.api.user;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.common.util.concurrent.Futures;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.weizhu.common.utils.JsonUtil;
import com.weizhu.proto.AdminUserService;
import com.weizhu.proto.UserProtos;
import com.weizhu.proto.AdminProtos.AdminHead;
import com.weizhu.proto.AdminUserProtos.CreateUserRequest;
import com.weizhu.proto.AdminUserProtos.CreateUserResponse;
import com.weizhu.web.ParamUtil;

@Singleton
@SuppressWarnings("serial")
public class CreateUserServlet extends HttpServlet {
	
	private final Provider<AdminHead> adminHeadProvider;
	private final AdminUserService adminUserService;
	
	@Inject
	public CreateUserServlet(Provider<AdminHead> adminHeadProvider, AdminUserService adminUserService) {
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
		String rawId = ParamUtil.getString(httpRequest, "raw_id", "");
		String userName = ParamUtil.getString(httpRequest, "user_name", "");
		String genderStr = ParamUtil.getString(httpRequest, "gender", "");
		List<String> mobileNoList = ParamUtil.getStringList(httpRequest, "mobile_no", Collections.<String>emptyList());
		List<String> phoneNoList = ParamUtil.getStringList(httpRequest, "phone_no", Collections.<String>emptyList());
		String email = ParamUtil.getString(httpRequest, "email","");
		Integer levelId = ParamUtil.getInt(httpRequest, "level_id", null);
		Integer teamId = ParamUtil.getInt(httpRequest, "team_id", null);
		Integer positionId = ParamUtil.getInt(httpRequest, "position_id", null);
		
		// 2. 调用Service
		
		final AdminHead head = adminHeadProvider.get();
		
		CreateUserRequest.Builder requestBuilder = CreateUserRequest.newBuilder();
		requestBuilder.setRawId(rawId);
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
		if (levelId != null) {
			requestBuilder.setLevelId(levelId);
		}
		if (teamId != null) {
			UserProtos.UserTeam.Builder userTeamBuilder = UserProtos.UserTeam.newBuilder();
			userTeamBuilder.setUserId(0);
			userTeamBuilder.setTeamId(teamId);
			if (positionId != null) {
				userTeamBuilder.setPositionId(positionId);
			}
			requestBuilder.addUserTeam(userTeamBuilder.build());
		}
		
		CreateUserResponse response = Futures.getUnchecked(adminUserService.createUser(head, requestBuilder.build()));
		
		httpResponse.setContentType("application/json;charset=UTF-8");
		JsonUtil.PROTOBUF_JSON_FORMAT.print(response, httpResponse.getWriter());
	}

}
