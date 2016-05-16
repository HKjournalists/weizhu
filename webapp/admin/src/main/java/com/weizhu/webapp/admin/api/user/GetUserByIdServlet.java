package com.weizhu.webapp.admin.api.user;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
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
import com.google.gson.JsonPrimitive;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.weizhu.proto.AdminUserProtos.GetUserAbilityTagRequest;
import com.weizhu.proto.AdminUserProtos.GetUserAbilityTagResponse;
import com.weizhu.proto.AdminUserProtos.GetUserByIdRequest;
import com.weizhu.proto.AdminUserProtos.GetUserByIdResponse;
import com.weizhu.proto.AdminUserService;
import com.weizhu.proto.UserProtos;
import com.weizhu.common.utils.JsonUtil;
import com.weizhu.proto.AdminProtos.AdminHead;
import com.weizhu.web.ParamUtil;

@Singleton
@SuppressWarnings("serial")
public class GetUserByIdServlet extends HttpServlet {
	
	private final Provider<AdminHead> adminHeadProvider;
	private final AdminUserService adminUserService;
	
	@Inject
	public GetUserByIdServlet(Provider<AdminHead> adminHeadProvider, AdminUserService adminUserService) {
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
		List<Long> userIdList = ParamUtil.getLongList(httpRequest, "user_id", Collections.<Long>emptyList());
		
		// 2. 调用Service
		
		final AdminHead head = adminHeadProvider.get();
		
		GetUserByIdRequest request = GetUserByIdRequest.newBuilder()
				.addAllUserId(userIdList)
				.build();
		
		GetUserByIdResponse response = Futures.getUnchecked(adminUserService.getUserById(head, request));
		
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
		
		GetUserAbilityTagResponse getAbilityTagResponse = Futures.getUnchecked(
				this.adminUserService.getUserAbilityTag(head, GetUserAbilityTagRequest.newBuilder()
						.addAllUserId(userIdList)
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
		
		httpResponse.setContentType("application/json;charset=UTF-8");
		JsonUtil.GSON.toJson(data, httpResponse.getWriter());
	}

}
