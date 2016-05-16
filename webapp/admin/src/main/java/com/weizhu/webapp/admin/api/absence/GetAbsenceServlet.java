package com.weizhu.webapp.admin.api.absence;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.common.util.concurrent.Futures;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.weizhu.common.service.ServiceUtil;
import com.weizhu.common.utils.JsonUtil;
import com.weizhu.proto.AbsenceProtos;
import com.weizhu.proto.AbsenceProtos.GetAbsenceSerRequest;
import com.weizhu.proto.AbsenceProtos.GetAbsenceSerResponse;
import com.weizhu.proto.AbsenceService;
import com.weizhu.proto.AdminProtos.AdminHead;
import com.weizhu.proto.AdminUserProtos.GetUserByIdRequest;
import com.weizhu.proto.AdminUserProtos.GetUserByIdResponse;
import com.weizhu.proto.AdminUserProtos.GetUserListRequest;
import com.weizhu.proto.AdminUserProtos.GetUserListResponse;
import com.weizhu.proto.AdminUserService;
import com.weizhu.proto.UploadProtos.GetUploadUrlPrefixResponse;
import com.weizhu.proto.UploadService;
import com.weizhu.proto.UserProtos;
import com.weizhu.web.ParamUtil;

@Singleton
public class GetAbsenceServlet extends HttpServlet {

	private static final long serialVersionUID = 1L;

	private final Provider<AdminHead> adminHeadProvider;
	private final AbsenceService absenceService;
	private final AdminUserService adminUserService;
	private final UploadService uploadService;
	
	@Inject
	public GetAbsenceServlet(Provider<AdminHead> adminHeadProvider, AbsenceService absenceService, AdminUserService adminUserService, UploadService uploadService) {
		this.adminHeadProvider = adminHeadProvider;
		this.absenceService = absenceService;
		this.adminUserService = adminUserService;
		this.uploadService = uploadService;
	}
	
	@Override
	public void doPost(HttpServletRequest httpRequest, HttpServletResponse httpResponse) throws IOException {
		final int start = ParamUtil.getInt(httpRequest, "start", 0);
		final int length = ParamUtil.getInt(httpRequest, "length", 50);
		
		final String userName = ParamUtil.getString(httpRequest, "user_name", "");
		final Integer startTime = ParamUtil.getInt(httpRequest, "start_time", null);
		final Integer endTime = ParamUtil.getInt(httpRequest, "end_time", null);
		final String actionStr = ParamUtil.getString(httpRequest, "action", "");
		
		final AdminHead head = adminHeadProvider.get();
		
		List<Long> userIdList = Lists.newArrayList();
		if (!userName.isEmpty()) {
			GetUserListResponse getUserResponse = Futures.getUnchecked(adminUserService.getUserList(head, GetUserListRequest.newBuilder()
					.setStart(0)
					.setLength(200)
					.setKeyword(userName)
					.build()));
			for (UserProtos.User user : getUserResponse.getUserList()) {
				userIdList.add(user.getBase().getUserId());
			}
		}
		
		GetAbsenceSerRequest.Action action = null;
		for (GetAbsenceSerRequest.Action tmpAction : GetAbsenceSerRequest.Action.values()) {
			if (tmpAction.name().equals(actionStr)) {
				action = tmpAction;
			}
		}
		
		GetAbsenceSerRequest.Builder requestBuilder = GetAbsenceSerRequest.newBuilder()
				.setStart(start)
				.setLength(length);
				
		if (!userIdList.isEmpty()) {
			requestBuilder.addAllUserId(userIdList);
		}
		if (startTime != null) {
			requestBuilder.setStartTime(startTime);
		}
		if (endTime != null) {
			requestBuilder.setEndTime(endTime);
		}
		if (action != null) {
			requestBuilder.setAction(action);
		}

		GetAbsenceSerResponse response = Futures.getUnchecked(absenceService.getAbsenceSer(head, requestBuilder.build()));
		
		Set<Long> userIdSet = Sets.newHashSet();
		for (AbsenceProtos.Absence absence : response.getAbsenceList()) {
			userIdSet.add(absence.getCreateUser());
			userIdSet.addAll(absence.getUserIdList());
		}
		
		GetUserByIdResponse getUserByIdResponse = Futures.getUnchecked(adminUserService.getUserById(head, GetUserByIdRequest.newBuilder()
				.addAllUserId(userIdSet)
				.build()));
		Map<Long, UserProtos.User> userMap = Maps.newHashMap();
		for (UserProtos.User user : getUserByIdResponse.getUserList()) {
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
		
		GetUploadUrlPrefixResponse getUploadUrlPrefixResponse = Futures.getUnchecked(this.uploadService.getUploadUrlPrefix(head, ServiceUtil.EMPTY_REQUEST));

		SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		
		JsonArray absenceArray = new JsonArray();
		for (AbsenceProtos.Absence absence : response.getAbsenceList()) {
			JsonObject obj = new JsonObject();
			
			obj.addProperty("absence_id", absence.getAbsenceId());
			obj.addProperty("type", absence.getType());
			obj.addProperty("start_time", absence.getStartTime());
			obj.addProperty("pre_end_time", absence.getPreEndTime());
			obj.addProperty("fac_end_time", absence.getFacEndTime());
			obj.addProperty("days", absence.getDays());
			obj.addProperty("desc", absence.getDesc());
			if (absence.hasState()) {
				obj.addProperty("state", absence.getState());
			}
			if (absence.hasCreateTime()) {
				obj.addProperty("create_time", df.format(new Date(absence.getCreateTime() * 1000L)));
			}
			if (absence.hasCreateUser()) {
				UserProtos.User user = userMap.get(absence.getCreateUser());
				AbsenceUtil.getUserTeamPosition(obj, user, getUploadUrlPrefixResponse.getImageUrlPrefix(), teamMap, positionMap);
			}
			JsonArray array = new JsonArray();
			for (long userId : absence.getUserIdList()) {
				JsonObject userObj = new JsonObject();
				
				UserProtos.User user = userMap.get(userId);
				AbsenceUtil.getUserTeamPosition(userObj, user, getUploadUrlPrefixResponse.getImageUrlPrefix(), teamMap, positionMap);
				array.add(userObj);
			}
			obj.add("user_list", array);
			absenceArray.add(obj);
		}
		
		JsonObject result = new JsonObject();
		result.add("absence_list", absenceArray);
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
