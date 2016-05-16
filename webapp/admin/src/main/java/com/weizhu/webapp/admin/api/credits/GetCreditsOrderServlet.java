package com.weizhu.webapp.admin.api.credits;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
import com.weizhu.proto.AdminCreditsProtos.GetCreditsOrderRequest;
import com.weizhu.proto.AdminCreditsProtos.GetCreditsOrderResponse;
import com.weizhu.proto.AdminCreditsService;
import com.weizhu.proto.AdminProtos;
import com.weizhu.proto.AdminProtos.AdminHead;
import com.weizhu.proto.AdminProtos.GetAdminByIdRequest;
import com.weizhu.proto.AdminProtos.GetAdminByIdResponse;
import com.weizhu.proto.AdminService;
import com.weizhu.proto.AdminUserProtos.GetUserByIdRequest;
import com.weizhu.proto.AdminUserProtos.GetUserByIdResponse;
import com.weizhu.proto.AdminUserService;
import com.weizhu.proto.CreditsProtos;
import com.weizhu.proto.UserProtos;
import com.weizhu.proto.UserProtos.User;
import com.weizhu.web.ParamUtil;

@Singleton
public class GetCreditsOrderServlet extends HttpServlet {

	private static final long serialVersionUID = 1L;

	private final Provider<AdminHead> adminHeadProvider;
	private final AdminCreditsService adminCreditsService;
	private final AdminUserService adminUserService;
	private final AdminService adminService;
	
	@Inject
	public GetCreditsOrderServlet(Provider<AdminHead> adminHeadProvider, AdminCreditsService adminCreditsService,
			AdminUserService adminUserService, AdminService adminService) {
		this.adminHeadProvider = adminHeadProvider;
		this.adminCreditsService = adminCreditsService;
		this.adminUserService = adminUserService;
		this.adminService = adminService;
	}
	
	@Override
	public void doPost(HttpServletRequest httpRequest, HttpServletResponse httpResponse) throws IOException {
		final List<Long> userIdList = ParamUtil.getLongList(httpRequest, "user_id", Collections.<Long>emptyList());
		final boolean isExpense = ParamUtil.getBoolean(httpRequest, "is_expense", false);
		final Integer startTime = ParamUtil.getInt(httpRequest, "start_time", null);
		final Integer endTime = ParamUtil.getInt(httpRequest, "end_time", null);
		final int start = ParamUtil.getInt(httpRequest, "start", 0);
		final int length = ParamUtil.getInt(httpRequest, "length", 10);
		
		GetCreditsOrderRequest.Builder requestBuilder = GetCreditsOrderRequest.newBuilder()
				.setStart(start)
				.setLength(length)
				.setIsExpense(isExpense);
		if (startTime != null) {
			requestBuilder.setStartTime(startTime);
		}
		if (endTime != null) {
			requestBuilder.setEndTime(endTime);
		}
		
		final AdminHead head = adminHeadProvider.get();
		
		GetCreditsOrderResponse response = Futures.getUnchecked(adminCreditsService.getCreditsOrder(head, requestBuilder
				.addAllUserId(userIdList)
				.build()));
		List<Long> tmpUserIdList = new ArrayList<Long>();
		List<Long> tmpAdminIdList = new ArrayList<Long>();
		for (CreditsProtos.CreditsOrder creditsOrder : response.getCreditsOrderList()) {
			tmpUserIdList.add(creditsOrder.getUserId());
			tmpAdminIdList.add(creditsOrder.getCreateAdmin());
		}
	
		GetUserByIdRequest getUserByIdRequest = GetUserByIdRequest.newBuilder()
				.addAllUserId(tmpUserIdList)
				.build();
		GetUserByIdResponse getUserByIdResponse = Futures.getUnchecked(adminUserService.getUserById(head, getUserByIdRequest));
		Map<Long, User> userMap = new HashMap<Long, User>();
		for (User user : getUserByIdResponse.getUserList()) {
			userMap.put(user.getBase().getUserId(), user);
		}

		Map<Integer, UserProtos.Team> teamMap = new HashMap<Integer, UserProtos.Team>();
		for (int i = 0; i < getUserByIdResponse.getRefTeamCount(); ++i) {
			UserProtos.Team team = getUserByIdResponse.getRefTeam(i);
			teamMap.put(team.getTeamId(), team);
		}
		
		Map<Integer, UserProtos.Position> positionMap = new HashMap<Integer, UserProtos.Position>();
		for (int i = 0; i < getUserByIdResponse.getRefPositionCount(); ++i) {
			UserProtos.Position position = getUserByIdResponse
					.getRefPosition(i);
			positionMap.put(position.getPositionId(), position);
		}
		
		GetAdminByIdResponse getAdminByIdResponse = Futures.getUnchecked(adminService.getAdminById(head, GetAdminByIdRequest.newBuilder()
				.addAllAdminId(tmpAdminIdList)
				.build()));
		Map<Long, AdminProtos.Admin> adminMap = new HashMap<Long, AdminProtos.Admin>();
		for (AdminProtos.Admin admin : getAdminByIdResponse.getAdminList()) {
			adminMap.put(admin.getAdminId(), admin);
		}
		
		JsonArray array = new JsonArray();
		for (CreditsProtos.CreditsOrder creditsOrder : response.getCreditsOrderList()) {
			JsonObject obj = new JsonObject();
			
			long userId = creditsOrder.getUserId();
			UserProtos.User user = userMap.get(userId);
			if (user == null) {
				continue;
			}
			obj.addProperty("user_id", userId);
			obj.addProperty("user_name", userMap.get(userId).getBase().getUserName());
			UserInfoUtil.getUserTeamPosition(obj, user, teamMap, positionMap);
			obj.addProperty("mobile_no", DBUtil.COMMA_JOINER.join(userMap.get(userId).getBase().getMobileNoList()));
			obj.addProperty("type", creditsOrder.getType().name());
			obj.addProperty("credits_delta", creditsOrder.getCreditsDelta());
			obj.addProperty("desc", creditsOrder.getDesc());
			obj.addProperty("state", creditsOrder.getState().name());
			obj.addProperty("create_time", creditsOrder.getCreateTime());
			obj.addProperty("create_admin", adminMap.get(creditsOrder.getCreateAdmin()) == null ? "未知的管理员" : adminMap.get(creditsOrder.getCreateAdmin()).getAdminName());
			
			array.add(obj);
		}
		
		JsonObject result = new JsonObject();
		result.add("credits_order", array);
		result.addProperty("total", response.getTotal());
		result.addProperty("filtered_size", response.getFilteredSize());
		
		httpResponse.setContentType("application/json;charset=UTF-8");
		JsonUtil.GSON.toJson(result, httpResponse.getWriter());
	}
	
	@Override
	public void doGet(HttpServletRequest httpRequest, HttpServletResponse httpResponse) throws IOException {
		this.doPost(httpRequest, httpResponse);
	}
}