package com.weizhu.webapp.admin.api.credits;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
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
import com.weizhu.proto.AdminCreditsProtos.GetUserCreditsRequest;
import com.weizhu.proto.AdminCreditsProtos.GetUserCreditsResponse;
import com.weizhu.proto.AdminCreditsService;
import com.weizhu.proto.AdminProtos.AdminHead;
import com.weizhu.proto.AdminUserProtos.GetTeamAllUserIdRequest;
import com.weizhu.proto.AdminUserProtos.GetTeamAllUserIdResponse;
import com.weizhu.proto.AdminUserProtos.GetUserByIdRequest;
import com.weizhu.proto.AdminUserProtos.GetUserByIdResponse;
import com.weizhu.proto.AdminUserProtos.GetUserListRequest;
import com.weizhu.proto.AdminUserProtos.GetUserListResponse;
import com.weizhu.proto.AdminUserService;
import com.weizhu.proto.CreditsProtos;
import com.weizhu.proto.UserProtos;
import com.weizhu.proto.UserProtos.User;
import com.weizhu.web.ParamUtil;

@Singleton
public class GetUserCreditsServlet extends HttpServlet {

	private static final long serialVersionUID = 1L;

	private final Provider<AdminHead> adminHeadProvider;
	private final AdminCreditsService adminCreditsService;
	private final AdminUserService adminUserService;
	
	@Inject
	public GetUserCreditsServlet(Provider<AdminHead> adminHeadProvider, AdminCreditsService adminCreditsService, AdminUserService adminUserService) {
		this.adminHeadProvider = adminHeadProvider;
		this.adminCreditsService = adminCreditsService;
		this.adminUserService = adminUserService;
	}
	
	@Override
	public void doPost(HttpServletRequest httpRequest, HttpServletResponse httpResponse) throws IOException {
		final List<Long> userIdList = ParamUtil.getLongList(httpRequest, "user_id", Lists.newArrayList());
		final int start = ParamUtil.getInt(httpRequest, "start", 0);
		final int length = ParamUtil.getInt(httpRequest, "length", 0);

		final Integer teamId = ParamUtil.getInt(httpRequest, "team_id", null);
		final String userName = ParamUtil.getString(httpRequest, "user_name", null);
		
		final AdminHead head = adminHeadProvider.get();
		
		List<Long> teamUserIdList = Lists.newArrayList();
		if (teamId != null) {
			GetTeamAllUserIdResponse response = Futures.getUnchecked(adminUserService.getTeamAllUserId(head, GetTeamAllUserIdRequest.newBuilder()
					.addAllTeamId(Collections.singleton(teamId))
					.build()));
			teamUserIdList.addAll(response.getUserIdList());
		}
		
		List<Long> nameUserIdList = Lists.newArrayList();
		if (userName != null && !userName.isEmpty()) {
			GetUserListResponse response = Futures.getUnchecked(adminUserService.getUserList(head, GetUserListRequest.newBuilder()
					.setStart(0)
					.setLength(50)
					.setKeyword(userName)
					.build()));
			for (UserProtos.User user : response.getUserList()) {
				nameUserIdList.add(user.getBase().getUserId());
			}
		}
		
		if (teamUserIdList.isEmpty() && !nameUserIdList.isEmpty()) {
			userIdList.addAll(nameUserIdList);
		} else if (!teamUserIdList.isEmpty() && nameUserIdList.isEmpty()) {
			userIdList.addAll(teamUserIdList);
		} else if (!teamUserIdList.isEmpty() && !nameUserIdList.isEmpty()) {
			for (long nameUserId : nameUserIdList) {
				if (teamUserIdList.contains(nameUserId)) {
					userIdList.add(nameUserId);
				}
			}
		}
		
		GetUserCreditsRequest request = GetUserCreditsRequest.newBuilder()
				.setStart(start)
				.setLength(length)
				.addAllUserId(userIdList)
				.build();
		
		GetUserCreditsResponse response = Futures.getUnchecked(adminCreditsService.getUserCredits(head, request));
		
		List<Long> tmpUserIdList = Lists.newArrayList();
		for (CreditsProtos.Credits credits : response.getCreditsList()) {
			tmpUserIdList.add(credits.getUserId());
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
		
		Map<Long, Long> userExpenseCredits = Maps.newHashMap();
		int startIdx = 0;
		while (true) {
			GetCreditsOrderResponse getCreditsOrderResponse = Futures.getUnchecked(adminCreditsService.getCreditsOrder(head, GetCreditsOrderRequest.newBuilder()
					.addAllUserId(tmpUserIdList)
					.setStart(startIdx)
					.setLength(50)
					.setIsExpense(true)
					.build()));
			
			for (CreditsProtos.CreditsOrder creditsOrder : getCreditsOrderResponse.getCreditsOrderList()) {
				long userId = creditsOrder.getUserId();
				Long credits = userExpenseCredits.get(userId);
				
				if (credits == null) {
					credits = 0L;
				}
				
				if (creditsOrder.getState().equals(CreditsProtos.CreditsOrder.State.SUCCESS) || creditsOrder.getState().equals(CreditsProtos.CreditsOrder.State.CONFIRM)) {
					userExpenseCredits.put(userId, credits += creditsOrder.getCreditsDelta());
				}
				
			}
			
			if (getCreditsOrderResponse.getCreditsOrderCount() < 50) {
				break;
			}
			
			startIdx += 50;
		}
		
		
		JsonArray array = new JsonArray();
		for (CreditsProtos.Credits credits : response.getCreditsList()) {
			JsonObject obj = new JsonObject();
			
			long userId = credits.getUserId();
			UserProtos.User user = userMap.get(userId);
			Long expenseCredits = userExpenseCredits.get(userId);
			
			obj.addProperty("user_id", userId);
			obj.addProperty("user_name", user == null ? "" : user.getBase().getUserName());
			UserInfoUtil.getUserTeamPosition(obj, user, teamMap, positionMap);
			obj.addProperty("mobile_no", user == null ? "" : DBUtil.COMMA_JOINER.join(user.getBase().getMobileNoList()));
			obj.addProperty("total_credits", expenseCredits == null ? credits.getCredits() : credits.getCredits() - expenseCredits.longValue());
			obj.addProperty("useable_credits", credits.getCredits());
			obj.addProperty("expense_credits", expenseCredits == null ? 0L : expenseCredits.longValue());
			
			array.add(obj);
		}
		
		JsonObject result = new JsonObject();
		result.add("credits", array);
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
