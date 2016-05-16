package com.weizhu.webapp.admin.api.user;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.common.util.concurrent.Futures;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.weizhu.proto.AdminUserService;
import com.weizhu.proto.UserProtos;
import com.weizhu.common.utils.JsonUtil;
import com.weizhu.proto.AdminProtos.AdminHead;
import com.weizhu.proto.AdminUserProtos.GetTeamRequest;
import com.weizhu.proto.AdminUserProtos.GetTeamResponse;
import com.weizhu.web.ParamUtil;

@Singleton
@SuppressWarnings("serial")
public class GetTeamServlet extends HttpServlet {
	
	private final Provider<AdminHead> adminHeadProvider;
	private final AdminUserService adminUserService;
	
	@Inject
	public GetTeamServlet(Provider<AdminHead> adminHeadProvider, AdminUserService adminUserService) {
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
		Integer teamId = ParamUtil.getInt(httpRequest, "team_id", null);
		
		// 2. 调用Service
		
		final AdminHead head = adminHeadProvider.get();
		
		GetTeamRequest.Builder requestBuilder = GetTeamRequest.newBuilder();
		if (teamId != null) {
			requestBuilder.setTeamId(teamId);
		}
		
		GetTeamResponse response = Futures.getUnchecked(this.adminUserService.getTeam(head, requestBuilder.build()));

		JsonArray resultArray = new JsonArray();
		
		Map<Integer, UserProtos.Team> refTeamMap = new HashMap<Integer, UserProtos.Team>();
		for (UserProtos.Team team : response.getRefTeamList()) {
			refTeamMap.put(team.getTeamId(), team);
		}
		
		Set<Integer> hasSubTeamIdSet = new TreeSet<Integer>(response.getSubTeamIdHasSubList());
		for (Integer subTeamId : response.getSubTeamIdList()) {
			UserProtos.Team team = refTeamMap.get(subTeamId);
			
			if (team != null) {
				JsonObject teamObj = new JsonObject();
				teamObj.addProperty("team_id", team.getTeamId());
				teamObj.addProperty("team_name", team.getTeamName());
				if (team.hasParentTeamId()) {
					teamObj.addProperty("parent_team_id", team.getParentTeamId());
				}
				teamObj.addProperty("has_sub_team", hasSubTeamIdSet.contains(team.getTeamId()));
				
				resultArray.add(teamObj);
			}
		}
		
		httpResponse.setContentType("application/json;charset=UTF-8");
		JsonUtil.GSON.toJson(resultArray, httpResponse.getWriter());
	}

}
