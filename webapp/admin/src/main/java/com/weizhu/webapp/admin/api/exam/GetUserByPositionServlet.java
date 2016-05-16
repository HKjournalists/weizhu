package com.weizhu.webapp.admin.api.exam;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

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
import com.weizhu.proto.AdminUserProtos.GetTeamAllUserIdRequest;
import com.weizhu.proto.AdminUserProtos.GetTeamAllUserIdResponse;
import com.weizhu.proto.AdminUserProtos.GetUserByIdRequest;
import com.weizhu.proto.AdminUserProtos.GetUserByIdResponse;
import com.weizhu.proto.AdminUserService;
import com.weizhu.proto.UserProtos;
import com.weizhu.proto.UserProtos.User;
import com.weizhu.web.ParamUtil;

@Singleton
public class GetUserByPositionServlet extends HttpServlet {

	private static final long serialVersionUID = 1L;

	private final Provider<AdminHead> adminHeadProvider;
	private final AdminUserService adminUserService;
	
	@Inject
	public GetUserByPositionServlet(Provider<AdminHead> adminHeadProvider, AdminUserService adminUserService) {
		this.adminHeadProvider = adminHeadProvider;
		this.adminUserService = adminUserService;
	}
	
	@Override
	public void doPost(HttpServletRequest httpRequest, HttpServletResponse httpResponse) throws IOException {
		final Integer teamId = ParamUtil.getInt(httpRequest, "team_id", null);
		
		final AdminHead head = adminHeadProvider.get();
		
		GetTeamAllUserIdRequest request = GetTeamAllUserIdRequest.newBuilder()
				.addAllTeamId(Collections.singletonList(teamId))
				.build();
		GetTeamAllUserIdResponse response = Futures.getUnchecked(adminUserService.getTeamAllUserId(head, request));
		
		GetUserByIdRequest getUserByIdRequest = GetUserByIdRequest.newBuilder()
				.addAllUserId(response.getUserIdList())
				.build();
		GetUserByIdResponse getUserByIdResponse = Futures.getUnchecked(adminUserService.getUserById(head, getUserByIdRequest));
		
		Map<Integer, String> positionMap = new HashMap<Integer, String>();
		for (int i = 0; i < getUserByIdResponse.getRefPositionCount(); ++i) {
			UserProtos.Position position = getUserByIdResponse
					.getRefPosition(i);
			positionMap.put(position.getPositionId(), position.getPositionName());
		}
		
		final String positionName = ParamUtil.getString(httpRequest, "position_name", "");
		List<Integer> postionIdList = new ArrayList<Integer>();
		for (Entry<Integer, String> entry : positionMap.entrySet()) {
			if (entry.getValue().contains(positionName)) {
				postionIdList.add(entry.getKey());
			}
		}
		
		JsonArray userArray = new JsonArray();
		for (User user : getUserByIdResponse.getUserList()) {
			if (user.getTeamList().size() != 0 && postionIdList.contains(user.getTeamList().get(0).getPositionId())) {
				JsonObject userObj = new JsonObject();
				
				userObj.addProperty("user_id", user.getBase().getUserId());
				userObj.addProperty("user_name", user.getBase().getUserName());
				userObj.addProperty("raw_id", user.getBase().getRawId());
				userObj.addProperty("mobile", DBUtil.COMMA_JOINER.join(user.getBase().getMobileNoList()));
				String realPositionName = positionMap.get(user.getTeamList().get(0).getPositionId());
				userObj.addProperty("position_name", realPositionName == null ? "" : realPositionName);
				userArray.add(userObj);
			}
		}
		
		JsonObject resultObj = new JsonObject();
		resultObj.add("user_result", userArray);
		
		httpResponse.setContentType("application/json;charset=UTF-8");
		JsonUtil.GSON.toJson(resultObj, httpResponse.getWriter());
		return ;
	}
	
	@Override
	public void doGet(HttpServletRequest httpRequest, HttpServletResponse httpResponse) throws IOException {
		this.doPost(httpRequest, httpResponse);
	}
}
