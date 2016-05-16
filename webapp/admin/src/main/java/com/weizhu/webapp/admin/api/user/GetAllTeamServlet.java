package com.weizhu.webapp.admin.api.user;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

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
import com.weizhu.common.service.ServiceUtil;
import com.weizhu.common.utils.JsonUtil;
import com.weizhu.proto.AdminUserService;
import com.weizhu.proto.UserProtos;
import com.weizhu.proto.AdminProtos.AdminHead;
import com.weizhu.proto.AdminUserProtos.GetAllTeamResponse;

@Singleton
@SuppressWarnings("serial")
public class GetAllTeamServlet extends HttpServlet {
	
	private final Provider<AdminHead> adminHeadProvider;
	private final AdminUserService adminUserService;
	
	@Inject
	public GetAllTeamServlet(Provider<AdminHead> adminHeadProvider, AdminUserService adminUserService) {
		this.adminHeadProvider = adminHeadProvider;
		this.adminUserService = adminUserService;
	}
	
	@Override
	protected void doGet(HttpServletRequest httpRequest, HttpServletResponse httpResponse) throws ServletException, IOException {
		this.doPost(httpRequest, httpResponse);
	}

	@Override
	protected void doPost(HttpServletRequest httpRequest, HttpServletResponse httpResponse) throws ServletException, IOException {

		final AdminHead head = adminHeadProvider.get();
		
		GetAllTeamResponse response = Futures.getUnchecked(this.adminUserService.getAllTeam(head, ServiceUtil.EMPTY_REQUEST));
		
		Map<Integer, UserProtos.Team> teamMap = new TreeMap<Integer, UserProtos.Team>();
		List<Integer> rootTeamIdList = new ArrayList<Integer>();
		Map<Integer, List<Integer>> subTeamIdMap = new TreeMap<Integer, List<Integer>>();
		
		for (UserProtos.Team team : response.getTeamList()) {
			teamMap.put(team.getTeamId(), team);
			if (!team.hasParentTeamId()) {
				rootTeamIdList.add(team.getTeamId());
			} else {
				List<Integer> list = subTeamIdMap.get(team.getParentTeamId());
				if (list == null) {
					list = new ArrayList<Integer>();
					subTeamIdMap.put(team.getParentTeamId(), list);
				}
				list.add(team.getTeamId());
			}
		}
		
		JsonArray teamArray = new JsonArray();
		for (Integer teamId : rootTeamIdList) {
			JsonObject teamObj = toJsonTeamObj(teamId, teamMap, subTeamIdMap);
			if (teamObj != null) {
				teamArray.add(teamObj);
			}
		}
		
		JsonObject resultObj = new JsonObject();
		resultObj.add("team", teamArray);
		
		httpResponse.setContentType("application/json;charset=UTF-8");
		JsonUtil.GSON.toJson(resultObj, httpResponse.getWriter());
	}
	
	private JsonObject toJsonTeamObj(Integer teamId, Map<Integer, UserProtos.Team> teamMap, Map<Integer, List<Integer>> subTeamIdMap) {
		UserProtos.Team team = teamMap.get(teamId);
		if (team == null) {
			return null;
		}
		
		JsonObject obj = new JsonObject();
		obj.addProperty("team_id", team.getTeamId());
		obj.addProperty("team_name", team.getTeamName());
		
		JsonArray subArray = new JsonArray();
		List<Integer> subTeamIdList = subTeamIdMap.get(teamId);
		if (subTeamIdList != null) {
			for (Integer subTeamId : subTeamIdList) {
				JsonObject subObj = toJsonTeamObj(subTeamId, teamMap, subTeamIdMap);
				if (subObj != null) {
					subArray.add(subObj);
				}
			}
		}
		obj.add("sub_team", subArray);
		return obj;
	}
	
}
