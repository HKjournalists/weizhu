package com.weizhu.webapp.admin.api.credits;

import java.io.IOException;
import java.util.ArrayList;
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
import com.weizhu.proto.AdminCreditsProtos.CreditsOperation;
import com.weizhu.proto.AdminCreditsProtos.GetCreditsOperationRequest;
import com.weizhu.proto.AdminCreditsProtos.GetCreditsOperationResponse;
import com.weizhu.proto.AdminCreditsProtos.UserCreditsDelta;
import com.weizhu.proto.AdminCreditsService;
import com.weizhu.proto.AdminProtos.AdminHead;
import com.weizhu.proto.AdminProtos.GetAdminByIdRequest;
import com.weizhu.proto.AdminProtos.GetAdminByIdResponse;
import com.weizhu.proto.AdminUserProtos.GetUserByIdRequest;
import com.weizhu.proto.AdminUserProtos.GetUserByIdResponse;
import com.weizhu.proto.UserProtos.User;
import com.weizhu.proto.AdminProtos;
import com.weizhu.proto.AdminService;
import com.weizhu.proto.AdminUserService;
import com.weizhu.proto.UserProtos;
import com.weizhu.web.ParamUtil;

@Singleton
public class GetCreditsOperationServlet extends HttpServlet {

	private static final long serialVersionUID = 1L;

	private final Provider<AdminHead> adminHeadProvider;
	private final AdminCreditsService adminCreditsService;
	private final AdminUserService adminUserService;
	private final AdminService adminService;
	
	@Inject
	public GetCreditsOperationServlet(Provider<AdminHead> adminHeadProvider, AdminCreditsService adminCreditsService,
			AdminUserService adminUserService, AdminService adminService) {
		this.adminHeadProvider = adminHeadProvider;
		this.adminCreditsService = adminCreditsService;
		this.adminUserService = adminUserService;
		this.adminService = adminService;
	}
	
	@Override
	public void doPost(HttpServletRequest httpRequest, HttpServletResponse httpResponse) throws IOException {
		final int start = ParamUtil.getInt(httpRequest, "start", 0);
		final int length = ParamUtil.getInt(httpRequest, "length", 10);
		
		final AdminHead head = adminHeadProvider.get();
		
		GetCreditsOperationResponse response = Futures.getUnchecked(adminCreditsService.getCreditsOperation(head, GetCreditsOperationRequest.newBuilder()
				.setStart(start)
				.setLength(length)
				.build()));
		
		List<Long> userIdList = new ArrayList<Long>();
		List<Long> adminIdList = new ArrayList<Long>();
		for (CreditsOperation creditsOperation : response.getCreditsOperationList()) {
			adminIdList.add(creditsOperation.getCreateAdmin());
			
			for (UserCreditsDelta userCreditsDelta : creditsOperation.getUserCreditsDeltaList()) {
				userIdList.add(userCreditsDelta.getUserId());
			}
		}
		
		GetUserByIdRequest getUserByIdRequest = GetUserByIdRequest.newBuilder()
				.addAllUserId(userIdList)
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
				.addAllAdminId(adminIdList)
				.build()));
		Map<Long, AdminProtos.Admin> adminMap = new HashMap<Long, AdminProtos.Admin>();
		for (AdminProtos.Admin admin : getAdminByIdResponse.getAdminList()) {
			adminMap.put(admin.getAdminId(), admin);
		}
		
		JsonArray array = new JsonArray();
		for (CreditsOperation creditsOperation : response.getCreditsOperationList()) {
			JsonObject obj = new JsonObject();
			
			obj.addProperty("operation_id", creditsOperation.getOperationId());
			obj.addProperty("desc", creditsOperation.getDesc());
			obj.addProperty("create_time", creditsOperation.getCreateTime());
			obj.addProperty("create_admin", adminMap.get(creditsOperation.getCreateAdmin()) == null ? "未知的管理员" : adminMap.get(creditsOperation.getCreateAdmin()).getAdminName());
		
			long creditsTotal = 0;
			JsonArray userDeltaArray = new JsonArray();
			for (UserCreditsDelta userCreditsDelta : creditsOperation.getUserCreditsDeltaList()) {
				JsonObject obj1 = new JsonObject();
				
				UserProtos.User user = userMap.get(userCreditsDelta.getUserId());
				obj1.addProperty("user_name", user == null ? "未知名称" : user.getBase().getUserName());
				UserInfoUtil.getUserTeamPosition(obj1, user, teamMap, positionMap);
				obj1.addProperty("user_mobile", user == null ? "" : DBUtil.COMMA_JOINER.join(user.getBase().getMobileNoList()));
				obj1.addProperty("credits_delta", userCreditsDelta.getCreditsDelta());
				
				creditsTotal += userCreditsDelta.getCreditsDelta();
				
				userDeltaArray.add(obj1);
			}
			obj.add("user_credits_delta", userDeltaArray);
			obj.addProperty("credits_total", creditsTotal);
			
			array.add(obj);
		}
		
		JsonObject result = new JsonObject();
		result.add("credits_operation", array);
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
