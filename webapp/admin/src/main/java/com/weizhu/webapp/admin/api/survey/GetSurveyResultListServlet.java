package com.weizhu.webapp.admin.api.survey;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.common.util.concurrent.Futures;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.weizhu.common.utils.JsonUtil;
import com.weizhu.proto.AdminProtos.AdminHead;
import com.weizhu.proto.AdminUserProtos.GetUserByIdRequest;
import com.weizhu.proto.AdminUserProtos.GetUserByIdResponse;
import com.weizhu.proto.AdminUserService;
import com.weizhu.proto.SurveyProtos;
import com.weizhu.proto.SurveyProtos.GetSurveyResultListRequest;
import com.weizhu.proto.SurveyProtos.GetSurveyResultListResponse;
import com.weizhu.proto.SurveyService;
import com.weizhu.proto.UserProtos;
import com.weizhu.web.ParamUtil;

@Singleton
public class GetSurveyResultListServlet extends HttpServlet {

	private static final long serialVersionUID = 1L;

	private final Provider<AdminHead> adminHeadProvider;
	private final SurveyService surveyService;
	private final AdminUserService adminUserService;
	
	@Inject
	public GetSurveyResultListServlet(Provider<AdminHead> adminHeadProvider, SurveyService surveyService,
			AdminUserService adminUserService) {
		this.adminHeadProvider = adminHeadProvider;
		this.surveyService = surveyService;
		this.adminUserService = adminUserService;
	}
	
	@Override
	public void doPost(HttpServletRequest httpRequest, HttpServletResponse httpResponse) throws IOException {
		final int start = ParamUtil.getInt(httpRequest, "start", 0);
		final int length = ParamUtil.getInt(httpRequest, "length", 20);
		
		final int surveyId = ParamUtil.getInt(httpRequest, "survey_id", 0);
		
		GetSurveyResultListRequest getSurveyResultListRequest = GetSurveyResultListRequest.newBuilder()
				.setStart(start)
				.setLength(length)
				.setSurveyId(surveyId)
				.build();
		
		final AdminHead head = adminHeadProvider.get();
		GetSurveyResultListResponse getSurveyResultListResponse = Futures.getUnchecked(surveyService.getSurveyResultList(head, getSurveyResultListRequest));
		
		Set<Long> userIdSet = new TreeSet<Long>();
		for (SurveyProtos.SurveyResult surveyResult : getSurveyResultListResponse.getSurveyResultList()) {
			userIdSet.add(surveyResult.getUserId());
		}
		
		// 获取所有的用户的详细信息
		GetUserByIdRequest getUserByIdRequest = GetUserByIdRequest.newBuilder()
				.addAllUserId(userIdSet)
				.build();
		
		GetUserByIdResponse getUserByIdResponse = Futures.getUnchecked(adminUserService.getUserById(head, getUserByIdRequest));

		Map<Long, UserProtos.User> userMap = new HashMap<Long, UserProtos.User>();
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

		JsonArray array = new JsonArray();
		for (SurveyProtos.SurveyResult surveyResult : getSurveyResultListResponse.getSurveyResultList()) {
			long userId = surveyResult.getUserId();
			UserProtos.User user = userMap.get(userId);
			JsonObject obj = new JsonObject();
			if (user == null) {
				continue;
			}
			SurveyUtil.getUserTeamPosition(obj, user, teamMap, positionMap);
			obj.addProperty("survey_result", JsonUtil.PROTOBUF_JSON_FORMAT.printToString(surveyResult));
			
			array.add(obj);
		}
		
		JsonObject result = new JsonObject();
		result.add("survey_result_list", array);
		result.addProperty("total", getSurveyResultListResponse.getTotal());
		result.addProperty("filtered_size", getSurveyResultListResponse.getFilteredSize());
		
		httpResponse.setContentType("application/json;charset=UTF-8");
		JsonUtil.GSON.toJson(result, httpResponse.getWriter());
	}
	
	@Override
	public void doGet(HttpServletRequest httpRequest, HttpServletResponse httpResponse) throws IOException {
		this.doPost(httpRequest, httpResponse);
	}
}
