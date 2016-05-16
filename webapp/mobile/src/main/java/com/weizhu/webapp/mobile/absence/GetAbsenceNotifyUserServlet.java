package com.weizhu.webapp.mobile.absence;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.common.collect.Maps;
import com.google.common.util.concurrent.Futures;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.weizhu.common.utils.JsonUtil;
import com.weizhu.proto.UploadProtos.GetUploadUrlPrefixResponse;
import com.weizhu.proto.UploadService;
import com.weizhu.proto.UserProtos;
import com.weizhu.proto.UserProtos.SearchUserRequest;
import com.weizhu.proto.UserProtos.SearchUserResponse;
import com.weizhu.proto.UserService;
import com.weizhu.proto.WeizhuProtos.EmptyRequest;
import com.weizhu.proto.WeizhuProtos.RequestHead;
import com.weizhu.web.ParamUtil;

@Singleton
public class GetAbsenceNotifyUserServlet extends HttpServlet {

	private static final long serialVersionUID = 1L;

	private final Provider<RequestHead> requestHeadProvider;
	private final UserService userService;
	private final UploadService uploadService;
	
	@Inject
	public GetAbsenceNotifyUserServlet(Provider<RequestHead> requestHeadProvider, UserService userService, UploadService uploadService) {
		this.requestHeadProvider = requestHeadProvider;
		this.userService = userService;
		this.uploadService = uploadService;
	}
	
	@Override
	public void doPost(HttpServletRequest httpRequest, HttpServletResponse httpResponse) throws IOException {
		final String userName = ParamUtil.getString(httpRequest, "user_name", "");
		
		final RequestHead head = requestHeadProvider.get();
		
		GetUploadUrlPrefixResponse getUploadUrlPrefixResponse = Futures.getUnchecked(uploadService.getUploadUrlPrefix(head, EmptyRequest.getDefaultInstance()));
		
		SearchUserResponse response = Futures.getUnchecked(userService.searchUser(head, SearchUserRequest.newBuilder()
				.setKeyword(userName)
				.build()));
		Map<Long, UserProtos.User> userMap = Maps.newHashMap();
		for (UserProtos.User user : response.getUserList()) {
			userMap.put(user.getBase().getUserId(), user);
		}
		
		Map<Integer, UserProtos.Team> teamMap = new HashMap<Integer, UserProtos.Team>();
		for (int i = 0; i < response.getRefTeamCount(); ++i) {
			UserProtos.Team team = response.getRefTeam(i);
			teamMap.put(team.getTeamId(), team);
		}
		
		Map<Integer, UserProtos.Position> positionMap = new HashMap<Integer, UserProtos.Position>();
		for (int i = 0; i < response.getRefPositionCount(); ++i) {
			UserProtos.Position position = response
					.getRefPosition(i);
			positionMap.put(position.getPositionId(), position);
		}
		
		JsonArray array = new JsonArray();
		for (UserProtos.User user : response.getUserList()) {
			JsonObject obj = new JsonObject();
			obj.addProperty("user_id", user.getBase().getUserId());
			AbsenceUtil.getUserTeamPosition(obj, user, getUploadUrlPrefixResponse.getImageUrlPrefix(), teamMap, positionMap);
		
			array.add(obj);
		}
		
		JsonObject result = new JsonObject();
		result.add("user_list", array);
		
		httpResponse.setContentType("application/json;charset=UTF-8");
		JsonUtil.GSON.toJson(result, httpResponse.getWriter());
	}
	
	@Override
	public void doGet(HttpServletRequest httpRequest, HttpServletResponse httpResponse) throws IOException {
		this.doPost(httpRequest, httpResponse);
	}
}
