package com.weizhu.webapp.admin.api.offline_training;

import java.io.IOException;
import java.util.Collections;
import java.util.LinkedList;
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
import com.weizhu.common.utils.JsonUtil;
import com.weizhu.proto.AdminOfflineTrainingService;
import com.weizhu.proto.AdminUserService;
import com.weizhu.proto.OfflineTrainingProtos;
import com.weizhu.proto.UserProtos;
import com.weizhu.proto.AdminOfflineTrainingProtos.GetTrainUserListRequest;
import com.weizhu.proto.AdminOfflineTrainingProtos.GetTrainUserListResponse;
import com.weizhu.proto.AdminProtos.AdminHead;
import com.weizhu.proto.AdminUserProtos.GetUserByIdRequest;
import com.weizhu.proto.AdminUserProtos.GetUserByIdResponse;
import com.weizhu.web.ParamUtil;

@Singleton
@SuppressWarnings("serial")
public class GetTrainUserListServlet extends HttpServlet {
	
	private final Provider<AdminHead> adminHeadProvider;
	private final AdminOfflineTrainingService adminOfflineTrainingService;
	private final AdminUserService adminUserService;
	
	@Inject
	public GetTrainUserListServlet(Provider<AdminHead> adminHeadProvider, 
			AdminOfflineTrainingService adminOfflineTrainingService,
			AdminUserService adminUserService
			) {
		this.adminHeadProvider = adminHeadProvider;
		this.adminOfflineTrainingService = adminOfflineTrainingService;
		this.adminUserService = adminUserService;
	}
	
	@Override
	protected void doGet(HttpServletRequest httpRequest, HttpServletResponse httpResponse) throws ServletException, IOException {
		this.doPost(httpRequest, httpResponse);
	}

	@Override
	protected void doPost(HttpServletRequest httpRequest, HttpServletResponse httpResponse) throws ServletException, IOException {

		// 1. 取出参数
		final int trainId = ParamUtil.getInt(httpRequest, "train_id", 0);
		final int draw = ParamUtil.getInt(httpRequest, "draw", 1);
		final int start = ParamUtil.getInt(httpRequest, "start", 0);
		final int length = ParamUtil.getInt(httpRequest, "length", 10);
		final Boolean isCheckIn = ParamUtil.getBoolean(httpRequest, "is_check_in", null);
		final Boolean isLeave = ParamUtil.getBoolean(httpRequest, "is_leave", null);
		
		// 2. 调用Service
		
		final AdminHead head = adminHeadProvider.get();
		
		GetTrainUserListRequest.Builder requestBuilder = GetTrainUserListRequest.newBuilder()
				.setTrainId(trainId)
				.setStart(start)
				.setLength(length);
		if (isCheckIn != null) {
			requestBuilder.setIsCheckIn(isCheckIn);
		}
		if (isLeave != null) {
			requestBuilder.setIsLeave(isLeave);
		}
		
		GetTrainUserListResponse response = Futures.getUnchecked(this.adminOfflineTrainingService.getTrainUserList(head, requestBuilder.build()));

		Set<Long> userIdSet = new TreeSet<Long>();
		for (OfflineTrainingProtos.TrainUser trainUser : response.getTrainUserList()) {
			userIdSet.add(trainUser.getUserId());
		}
		
		final Map<Long, UserProtos.User> userMap;
		final Map<Integer, UserProtos.Team> teamMap;
		final Map<Integer, UserProtos.Position> positionMap;
		final Map<Integer, UserProtos.Level> levelMap;
		if (userIdSet.isEmpty()) {
			userMap = Collections.emptyMap();
			teamMap = Collections.emptyMap();
			positionMap = Collections.emptyMap();
			levelMap = Collections.emptyMap();
		} else {
			GetUserByIdResponse getUserResponse = Futures.getUnchecked(
					this.adminUserService.getUserById(head, 
							GetUserByIdRequest.newBuilder()
							.addAllUserId(userIdSet)
							.build()));
			userMap = new TreeMap<Long, UserProtos.User>();
			for (UserProtos.User user : getUserResponse.getUserList()) {
				userMap.put(user.getBase().getUserId(), user);
			}
			
			teamMap = new TreeMap<Integer, UserProtos.Team>();
			for (UserProtos.Team team : getUserResponse.getRefTeamList()) {
				teamMap.put(team.getTeamId(), team);
			}
			
			positionMap = new TreeMap<Integer, UserProtos.Position>();
			for (UserProtos.Position position : getUserResponse.getRefPositionList()) {
				positionMap.put(position.getPositionId(), position);
			}
			
			levelMap = new TreeMap<Integer, UserProtos.Level>();
			for (UserProtos.Level level : getUserResponse.getRefLevelList()) {
				levelMap.put(level.getLevelId(), level);
			}
		}
		
		JsonArray data = new JsonArray();
		for (OfflineTrainingProtos.TrainUser trainUser : response.getTrainUserList()) {
			JsonObject trainUserObj = new JsonObject();
			trainUserObj.addProperty("user_id", trainUser.getUserId());
			trainUserObj.addProperty("is_apply", trainUser.getIsApply());
			if (trainUser.hasApplyTime()) {
				trainUserObj.addProperty("apply_time", trainUser.getApplyTime());
			}
			trainUserObj.addProperty("is_check_in", trainUser.getIsCheckIn());
			if (trainUser.hasCheckInTime()) {
				trainUserObj.addProperty("check_in_time", trainUser.getCheckInTime());
			}
			trainUserObj.addProperty("is_leave", trainUser.getIsLeave());
			if (trainUser.hasLeaveTime()) {
				trainUserObj.addProperty("leave_time", trainUser.getLeaveTime());
			}
			if (trainUser.hasLeaveReason()) {
				trainUserObj.addProperty("leave_reason", trainUser.getLeaveReason());
			}
			
			UserProtos.User user = userMap.get(trainUser.getUserId());
			if (user != null) {
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
				
				trainUserObj.add("user", u);
			}
			
			data.add(trainUserObj);
		}
		
		JsonObject result = new JsonObject();
		result.addProperty("draw", draw);
		result.addProperty("recordsTotal", response.getTotalSize());
		result.addProperty("recordsFiltered", response.getTotalSize());
		result.add("data", data);
		
		httpResponse.setContentType("application/json;charset=UTF-8");
		JsonUtil.GSON.toJson(result, httpResponse.getWriter());
	}

}
