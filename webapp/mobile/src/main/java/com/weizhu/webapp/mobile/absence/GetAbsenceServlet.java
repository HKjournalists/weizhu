package com.weizhu.webapp.mobile.absence;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

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
import com.weizhu.proto.AbsenceProtos.GetAbsenceCliRequest;
import com.weizhu.proto.AbsenceProtos.GetAbsenceCliResponse;
import com.weizhu.proto.AbsenceService;
import com.weizhu.proto.UserProtos.GetUserByIdRequest;
import com.weizhu.proto.UploadProtos.GetUploadUrlPrefixResponse;
import com.weizhu.proto.UploadService;
import com.weizhu.proto.UserProtos;
import com.weizhu.proto.UserProtos.GetUserResponse;
import com.weizhu.proto.UserService;
import com.weizhu.proto.WeizhuProtos.RequestHead;
import com.weizhu.web.ParamUtil;

@Singleton
public class GetAbsenceServlet extends HttpServlet {

	private static final long serialVersionUID = 1L;

	private final Provider<RequestHead> requestHeadProvider;
	private final AbsenceService absenceService;
	private final UserService userService;
	private final UploadService uploadService;
	
	@Inject
	public GetAbsenceServlet(Provider<RequestHead> requestHeadProvider, AbsenceService absenceService,
			UserService userService, UploadService uploadService) {
		this.requestHeadProvider = requestHeadProvider;
		this.absenceService = absenceService;
		this.userService = userService;
		this.uploadService = uploadService;
	}
	
	@Override
	public void doPost(HttpServletRequest httpRequest, HttpServletResponse httpResponse) throws IOException {
		final int size = ParamUtil.getInt(httpRequest, "size", 10);
		final Integer lastAbsenceId = ParamUtil.getInt(httpRequest, "last_absence_id", null);
		
		final RequestHead head = requestHeadProvider.get();
		
		GetAbsenceCliRequest.Builder requestBuilder = GetAbsenceCliRequest.newBuilder()
				.setSize(size);
		if (lastAbsenceId != null) {
			requestBuilder.setLastAbsenceId(lastAbsenceId);
		}
		
		GetAbsenceCliResponse response = Futures.getUnchecked(absenceService.getAbsenceCli(head, requestBuilder.build()));
		

		Set<Long> userIdSet = Sets.newHashSet();
		for (AbsenceProtos.Absence absence : response.getAbsenceList()) {
			userIdSet.add(absence.getCreateUser());
			userIdSet.addAll(absence.getUserIdList());
		}
		
		GetUserResponse getUserResponse = Futures.getUnchecked(userService.getUserById(head, GetUserByIdRequest.newBuilder()
				.addAllUserId(userIdSet)
				.build()));
		Map<Long, UserProtos.User> userMap = Maps.newHashMap();
		for (UserProtos.User user : getUserResponse.getUserList()) {
			userMap.put(user.getBase().getUserId(), user);
		}
		
		Map<Integer, UserProtos.Team> teamMap = new HashMap<Integer, UserProtos.Team>();
		for (int i = 0; i < getUserResponse.getRefTeamCount(); ++i) {
			UserProtos.Team team = getUserResponse.getRefTeam(i);
			teamMap.put(team.getTeamId(), team);
		}
		
		Map<Integer, UserProtos.Position> positionMap = new HashMap<Integer, UserProtos.Position>();
		for (int i = 0; i < getUserResponse.getRefPositionCount(); ++i) {
			UserProtos.Position position = getUserResponse
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
		
		httpResponse.setContentType("application/json;charset=UTF-8");
		JsonUtil.GSON.toJson(result, httpResponse.getWriter());
		
	}
	
	@Override
	public void doGet(HttpServletRequest httpRequest, HttpServletResponse httpResponse) throws IOException {
		this.doPost(httpRequest, httpResponse);
	}
}
