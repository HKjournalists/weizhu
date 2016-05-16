package com.weizhu.webapp.admin.api.user;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.common.util.concurrent.Futures;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.weizhu.proto.AdminUserService;
import com.weizhu.proto.SessionProtos;
import com.weizhu.proto.SessionService;
import com.weizhu.proto.UserProtos;
import com.weizhu.proto.WeizhuProtos;
import com.weizhu.proto.AdminProtos.AdminHead;
import com.weizhu.proto.AdminProtos.GetAdminByIdRequest;
import com.weizhu.proto.AdminProtos.GetAdminByIdResponse;
import com.weizhu.common.utils.JsonUtil;
import com.weizhu.proto.AdminProtos;
import com.weizhu.proto.AdminService;
import com.weizhu.proto.AdminUserProtos.GetUserAbilityTagRequest;
import com.weizhu.proto.AdminUserProtos.GetUserAbilityTagResponse;
import com.weizhu.proto.AdminUserProtos.GetUserListRequest;
import com.weizhu.proto.AdminUserProtos.GetUserListResponse;
import com.weizhu.proto.SessionProtos.GetSessionDataRequest;
import com.weizhu.proto.SessionProtos.GetSessionDataResponse;
import com.weizhu.web.ParamUtil;

@Singleton
@SuppressWarnings("serial")
public class GetUserListServlet extends HttpServlet {
	
	private final Provider<AdminHead> adminHeadProvider;
	private final AdminUserService adminUserService;
	private final AdminService adminService;
	private final SessionService sessionService;
	
	@Inject
	public GetUserListServlet(Provider<AdminHead> adminHeadProvider, 
			AdminUserService adminUserService, 
			AdminService adminService, 
			SessionService sessionService
			) {
		this.adminHeadProvider = adminHeadProvider;
		this.adminUserService = adminUserService;
		this.adminService = adminService;
		this.sessionService = sessionService;
	}
	
	@Override
	protected void doGet(HttpServletRequest httpRequest, HttpServletResponse httpResponse) throws ServletException, IOException {
		this.doPost(httpRequest, httpResponse);
	}

	@Override
	protected void doPost(HttpServletRequest httpRequest, HttpServletResponse httpResponse) throws ServletException, IOException {

		// 1. 取出参数
		int draw = ParamUtil.getInt(httpRequest, "draw", 1);
		int start = ParamUtil.getInt(httpRequest, "start", 0);
		int length = ParamUtil.getInt(httpRequest, "length", 10);
		Boolean isExpert = ParamUtil.getBoolean(httpRequest, "is_expert", null);
		Integer teamId = ParamUtil.getInt(httpRequest, "team_id", null);
		Integer positionId = ParamUtil.getInt(httpRequest, "position_id", null);
		String keyword = ParamUtil.getString(httpRequest, "keyword", null);
		String mobileNo = ParamUtil.getString(httpRequest, "mobile_no", null);
		
		// 2. 调用Service
		
		final AdminHead head = adminHeadProvider.get();
		
		GetUserListRequest.Builder requestBuilder = GetUserListRequest.newBuilder();
		requestBuilder.setStart(start);
		requestBuilder.setLength(length);
		if (isExpert != null) {
			requestBuilder.setIsExpert(isExpert);
		}
		if (teamId != null) {
			requestBuilder.setTeamId(teamId);
		}
		if (positionId != null) {
			requestBuilder.setPositionId(positionId);
		}
		if (keyword != null) {
			requestBuilder.setKeyword(keyword);
		}
		if (mobileNo != null) {
			requestBuilder.setMobileNo(mobileNo);
		}
		
		GetUserListResponse response = Futures.getUnchecked(adminUserService.getUserList(head, requestBuilder.build()));
		
		Map<Integer, UserProtos.Team> teamMap = new HashMap<Integer, UserProtos.Team>();
		for (int i=0; i<response.getRefTeamCount(); ++i) {
			UserProtos.Team team = response.getRefTeam(i);
			teamMap.put(team.getTeamId(), team);
		}
		
		Map<Integer, UserProtos.Position> positionMap = new HashMap<Integer, UserProtos.Position>();
		for (int i=0; i<response.getRefPositionCount(); ++i) {
			UserProtos.Position position = response.getRefPosition(i);
			positionMap.put(position.getPositionId(), position);
		}
		
		Map<Integer, UserProtos.Level> levelMap = new HashMap<Integer, UserProtos.Level>();
		for (int i=0; i<response.getRefLevelCount(); ++i) {
			UserProtos.Level level = response.getRefLevel(i);
			levelMap.put(level.getLevelId(), level);
		}
		
		Set<Long> adminIdSet = new TreeSet<Long>();
		for (UserProtos.User user : response.getUserList()) {
			if (user.getBase().hasCreateAdminId()) {
				adminIdSet.add(user.getBase().getCreateAdminId());
			}
			if (user.getBase().hasUpdateAdminId()) {
				adminIdSet.add(user.getBase().getUpdateAdminId());
			}
		}
		
		GetAdminByIdResponse getAdminByIdResponse = Futures.getUnchecked(
				this.adminService.getAdminById(head, 
						GetAdminByIdRequest.newBuilder()
							.addAllAdminId(adminIdSet)
							.build()));
		
		Map<Long, AdminProtos.Admin> adminMap = new TreeMap<Long, AdminProtos.Admin>();
		for (AdminProtos.Admin admin : getAdminByIdResponse.getAdminList()) {
			adminMap.put(admin.getAdminId(), admin);
		}
		
		Set<Long> userIdSet = new TreeSet<Long>();
		for (UserProtos.User user : response.getUserList()) {
			userIdSet.add(user.getBase().getUserId());
		}
		
		GetSessionDataResponse getSessionDataResponse = Futures.getUnchecked(
				this.sessionService.getSessionData(head, 
						GetSessionDataRequest.newBuilder()
							.addAllUserId(userIdSet)
							.build()));
		
		Map<Long, SessionProtos.SessionData> sessionDataMap = new TreeMap<Long, SessionProtos.SessionData>();
		for (SessionProtos.SessionData data : getSessionDataResponse.getSessionDataList()) {
			SessionProtos.SessionData d = sessionDataMap.get(data.getSession().getUserId());
			if (d == null || d.getActiveTime() < data.getActiveTime()) {
				sessionDataMap.put(data.getSession().getUserId(), data);
			}
		}
		
		GetUserAbilityTagResponse getAbilityTagResponse = Futures.getUnchecked(
				this.adminUserService.getUserAbilityTag(head, GetUserAbilityTagRequest.newBuilder()
						.addAllUserId(userIdSet)
						.build()));
		Map<Long, List<String>> abilityTagMap = new TreeMap<Long, List<String>>();
		for (UserProtos.UserAbilityTag tag : getAbilityTagResponse.getAbilityTagList()) {
			List<String> list = abilityTagMap.get(tag.getUserId());
			if (list == null) {
				list = new ArrayList<String>();
				abilityTagMap.put(tag.getUserId(), list);
			}
			list.add(tag.getTagName());
		}
		
		JsonArray data = new JsonArray();
		for (int i=0; i<response.getUserCount(); ++i) {
			UserProtos.User user = response.getUser(i);
			
			JsonObject u = new JsonObject();
			u.addProperty("user_id", user.getBase().getUserId());
			u.addProperty("raw_id", user.getBase().getRawId());
			u.addProperty("user_name", user.getBase().getUserName());
			if (user.getBase().hasGender()) {
				u.addProperty("gender", user.getBase().getGender().name());
			}
			
			JsonArray mobileNoArray = new JsonArray();
			for (int j=0; j<user.getBase().getMobileNoCount(); ++j) {
				mobileNoArray.add(new JsonPrimitive(user.getBase().getMobileNo(j)));
			}
			u.add("mobile_no", mobileNoArray);
			
			JsonArray phoneNoArray = new JsonArray();
			for (int j=0; j<user.getBase().getPhoneNoCount(); ++j) {
				phoneNoArray.add(new JsonPrimitive(user.getBase().getPhoneNo(j)));
			}
			u.add("phone_no", phoneNoArray);
			
			if (user.getBase().hasEmail()) {
				u.addProperty("email", user.getBase().getEmail());
			}
			
			if (user.getBase().hasIsExpert()) {
				u.addProperty("is_expert", user.getBase().getIsExpert());
			} else {
				u.addProperty("is_expert", false);
			}
			
			if (user.getBase().hasLevelId()) {
				u.addProperty("level_id", user.getBase().getLevelId());
				
				UserProtos.Level level = levelMap.get(user.getBase().getLevelId());
				if (level != null) {
					JsonObject levelObj = new JsonObject();
					levelObj.addProperty("level_id", level.getLevelId());
					levelObj.addProperty("level_name", level.getLevelName());
					u.add("level", levelObj);
				}
			}
			
			if (user.getBase().hasState()) {
				u.addProperty("state", user.getBase().getState().name());
			} else {
				u.addProperty("state", UserProtos.UserBase.State.NORMAL.name());
			}
			
			if (user.getTeamCount() > 0) {
				UserProtos.UserTeam userTeam = user.getTeam(0);
				u.addProperty("team_id", userTeam.getTeamId());
				
				LinkedList<UserProtos.Team> teamList = new LinkedList<UserProtos.Team>();
				int tmpTeamId = userTeam.getTeamId();
				while (true) {
					UserProtos.Team team = teamMap.get(tmpTeamId);
					if (team == null) {
						// warn : cannot find team
						teamList.clear();
						break;
					}
					
					teamList.addFirst(team);
					
					if (team.hasParentTeamId()) {
						tmpTeamId = team.getParentTeamId();
					} else {
						break;
					}
				}
				
				JsonArray teamObjArray = new JsonArray();
				for (UserProtos.Team team : teamList) {
					JsonObject teamObj = new JsonObject();
					teamObj.addProperty("team_id", team.getTeamId());
					teamObj.addProperty("team_name", team.getTeamName());
					if (team.hasParentTeamId()) {
						teamObj.addProperty("parent_team_id", team.getParentTeamId());
					}
					
					teamObjArray.add(teamObj);
				}
				u.add("team", teamObjArray);
				
				if (userTeam.hasPositionId()) {
					u.addProperty("position_id", userTeam.getPositionId());
					
					UserProtos.Position position = positionMap.get(userTeam.getPositionId());
					if (position != null) {
						JsonObject positionObj = new JsonObject();
						positionObj.addProperty("position_id", position.getPositionId());
						positionObj.addProperty("position_name", position.getPositionName());
						positionObj.addProperty("position_desc", position.getPositionDesc());
						u.add("position", positionObj);
					}
				}
			}
			
			JsonArray extsArrayObj = new JsonArray();
			for (UserProtos.UserExtends exts : user.getExtList()) {
				JsonObject extsObj = new JsonObject();
				extsObj.addProperty("name", exts.getName());
				extsObj.addProperty("value", exts.getValue());
				extsArrayObj.add(extsObj);
			}
			
			u.add("exts", extsArrayObj);
			
			if (user.getBase().hasCreateTime()) {
				u.addProperty("create_time", user.getBase().getCreateTime());
			}
			if (user.getBase().hasCreateAdminId()) {
				AdminProtos.Admin admin = adminMap.get(user.getBase().getCreateAdminId());
				if (admin != null) {
					u.addProperty("create_admin", admin.getAdminName());
				}
			}
			
			if (user.getBase().hasUpdateTime()) {
				u.addProperty("update_time", user.getBase().getUpdateTime());
			}
			if (user.getBase().hasUpdateAdminId()) {
				AdminProtos.Admin admin = adminMap.get(user.getBase().getUpdateAdminId());
				if (admin != null) {
					u.addProperty("update_admin", admin.getAdminName());
				}
			}
			
			SessionProtos.SessionData sessionData = sessionDataMap.get(user.getBase().getUserId());
			if (sessionData != null) {
				JsonObject sessionDataObj = new JsonObject();
				
				sessionDataObj.addProperty("session_id", String.valueOf(sessionData.getSession().getSessionId()));
				sessionDataObj.addProperty("login_time", sessionData.getLoginTime());
				sessionDataObj.addProperty("active_time", sessionData.getActiveTime());
				if (sessionData.hasWeizhu()) {
					sessionDataObj.addProperty("weizhu_platform", sessionData.getWeizhu().getPlatform().name());
					sessionDataObj.addProperty("weizhu_version_name", sessionData.getWeizhu().getVersionName());
					sessionDataObj.addProperty("weizhu_stage", sessionData.getWeizhu().getStage().name());
					sessionDataObj.addProperty("weizhu_build_time", sessionData.getWeizhu().getBuildTime());
				} else {
					sessionDataObj.addProperty("weizhu_platform", "");
					sessionDataObj.addProperty("weizhu_version_name", "");
					sessionDataObj.addProperty("weizhu_stage", "");
					sessionDataObj.addProperty("weizhu_build_time", 0);
				}
				
				StringBuilder sb = new StringBuilder();
				if (sessionData.hasAndroid()) {
					final WeizhuProtos.Android android = sessionData.getAndroid();
					sb.append("[Android:");
					sb.append(android.getDevice()).append("/");
					sb.append(android.getManufacturer()).append("/");
					sb.append(android.getBrand()).append("/");
					sb.append(android.getModel()).append("/");
					sb.append(android.getSerial()).append("/");
					sb.append(android.getRelease()).append("/");
					sb.append(android.getSdkInt()).append("/");
					sb.append(android.getCodename()).append("]");
				}
				
				if (sessionData.hasIphone()) {
					final WeizhuProtos.Iphone iphone = sessionData.getIphone();
					sb.append("[Iphone:");
					sb.append(iphone.getName()).append("/");
					sb.append(iphone.getSystemName()).append("/");
					sb.append(iphone.getSystemVersion()).append("/");
					sb.append(iphone.getModel()).append("/");
					sb.append(iphone.getLocalizedModel()).append("/");
					sb.append(iphone.getDeviceToken()).append("/");
					sb.append(iphone.getMac()).append("/");
					sb.append(iphone.getAppId()).append("]");
				}
				sessionDataObj.addProperty("device_info", sb.toString());
			
				u.add("session_data", sessionDataObj);
			}
			
			JsonArray abilityTagObj = new JsonArray();
			
			List<String> tagList = abilityTagMap.get(user.getBase().getUserId());
			if (tagList != null) {
				for (String tagName : tagList) {
					abilityTagObj.add(tagName);
				}
			}
			
			u.add("ability_tag", abilityTagObj);
			
			data.add(u);
		}		
		
		JsonObject result = new JsonObject();
		result.addProperty("draw", draw);
		result.addProperty("recordsTotal", response.getTotalSize());
		result.addProperty("recordsFiltered", response.getFilteredSize());
		result.add("data", data);
		
		httpResponse.setContentType("application/json;charset=UTF-8");
		JsonUtil.GSON.toJson(result, httpResponse.getWriter());
	}
}
