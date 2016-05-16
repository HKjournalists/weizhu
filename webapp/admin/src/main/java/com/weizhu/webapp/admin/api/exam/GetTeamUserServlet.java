package com.weizhu.webapp.admin.api.exam;

import java.io.IOException;
import java.util.HashMap;
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
import com.weizhu.proto.AdminProtos.AdminHead;
import com.weizhu.proto.AdminUserProtos.GetPositionResponse;
import com.weizhu.proto.AdminUserProtos.GetTeamRequest;
import com.weizhu.proto.AdminUserProtos.GetTeamResponse;
import com.weizhu.proto.AdminUserService;
import com.weizhu.proto.UserProtos;
import com.weizhu.proto.UserProtos.Position;
import com.weizhu.proto.WeizhuProtos.EmptyRequest;
import com.weizhu.web.ParamUtil;

@Singleton
public class GetTeamUserServlet extends HttpServlet {

	private static final long serialVersionUID = 1L;
	
	private final Provider<AdminHead> adminHeadProvider;
	private final AdminUserService adminUserService;
	
	@Inject
	public GetTeamUserServlet(Provider<AdminHead> adminHeadProvider, AdminUserService adminUserService) {
		this.adminHeadProvider = adminHeadProvider;
		this.adminUserService = adminUserService;
	}

	public void doPost(HttpServletRequest httpRequest, HttpServletResponse httpResponse) throws IOException {
		final Integer teamId = ParamUtil.getInt(httpRequest, "team_id", null);
		
		final AdminHead head = adminHeadProvider.get();
		
		GetTeamRequest request = null;
		request = teamId == null ? GetTeamRequest.newBuilder().build() 
				: GetTeamRequest.newBuilder()
					.setTeamId(teamId)
					.build();
		
		GetTeamResponse response = Futures.getUnchecked(adminUserService.getTeam(head, request));
		
		EmptyRequest positionRequest = EmptyRequest.newBuilder().build();
		GetPositionResponse positionResponse = Futures.getUnchecked(adminUserService.getPosition(head, positionRequest));
		Map<Integer, String> positionMap = new HashMap<Integer, String>();
		for (Position position : positionResponse.getPositionList()) {
			positionMap.put(position.getPositionId(), position.getPositionName());
		}
		
		JsonArray userArray = new JsonArray();
		for (UserProtos.User user : response.getRefUserList()) {
			JsonObject obj = new JsonObject();
			
			obj.addProperty("user_name", user.getBase().getUserName());
			obj.addProperty("user_id", user.getBase().getUserId());
			obj.addProperty("mobile", DBUtil.COMMA_JOINER.join(user.getBase().getMobileNoList()));
			
			if (user.getTeamList() == null || user.getTeamList().get(0) == null || positionMap.get(user.getTeamList().get(0).getPositionId()) == null) {
				obj.addProperty("position_name", "");
			} else {
				obj.addProperty("position_name", positionMap.get(user.getTeamList().get(0).getPositionId()));
			}

			userArray.add(obj);
		}
		JsonObject resultObj = new JsonObject();
		resultObj.add("users", userArray);
		
		httpResponse.setContentType("application/json;charset=UTF-8");
		JsonUtil.GSON.toJson(resultObj, httpResponse.getWriter());
	}
	
	public void doGet(HttpServletRequest httpRequest, HttpServletResponse httpResponse) throws IOException {
		this.doPost(httpRequest, httpResponse);
	}
}
