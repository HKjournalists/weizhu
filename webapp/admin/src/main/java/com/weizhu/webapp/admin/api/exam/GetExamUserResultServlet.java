package com.weizhu.webapp.admin.api.exam;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
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
import com.weizhu.proto.AdminExamProtos.GetExamByIdRequest;
import com.weizhu.proto.AdminExamProtos.GetExamByIdResponse;
import com.weizhu.proto.AdminExamProtos.GetExamUserResultRequest;
import com.weizhu.proto.AdminExamProtos.GetExamUserResultResponse;
import com.weizhu.proto.AdminExamService;
import com.weizhu.proto.AdminProtos.AdminHead;
import com.weizhu.proto.AdminUserProtos.GetUserByIdRequest;
import com.weizhu.proto.AdminUserProtos.GetUserByIdResponse;
import com.weizhu.proto.AdminUserProtos.GetUserListRequest;
import com.weizhu.proto.AdminUserProtos.GetUserListResponse;
import com.weizhu.proto.AdminUserService;
import com.weizhu.proto.ExamProtos.UserResult;
import com.weizhu.proto.UserProtos;
import com.weizhu.proto.UserProtos.User;
import com.weizhu.web.ParamUtil;

@Singleton
public class GetExamUserResultServlet extends HttpServlet {

	private static final long serialVersionUID = 1L;

	private final Provider<AdminHead> adminHeadProvider;
	private final AdminExamService adminExamService;
	private final AdminUserService adminUserService;
	
	@Inject
	public GetExamUserResultServlet(Provider<AdminHead> adminHeadProvider, AdminExamService adminExamService
			, AdminUserService adminUserService) {
		this.adminHeadProvider = adminHeadProvider;
		this.adminExamService = adminExamService;
		this.adminUserService = adminUserService;
	}
	
	@Override
	public void doGet(HttpServletRequest httpRequest, HttpServletResponse httpResponse) throws IOException {
		this.doPost(httpRequest, httpResponse);
	}
	
	@Override
	public void doPost(HttpServletRequest httpRequest, HttpServletResponse httpResponse) throws IOException {
		final Integer examId = ParamUtil.getInt(httpRequest, "exam_id", -1);
		final Integer start = ParamUtil.getInt(httpRequest, "start", 0);
		final Integer length = ParamUtil.getInt(httpRequest, "length", 10);
		final String userName = ParamUtil.getString(httpRequest, "user_name", "");
		
		final AdminHead head = adminHeadProvider.get();
		
		GetExamUserResultRequest request = GetExamUserResultRequest.newBuilder()
				.setExamId(examId)
				.setStart(start)
				.setLength(length)
				.build();
		
		GetExamUserResultResponse response = Futures.getUnchecked(adminExamService.getExamUserResult(head, request));
		
		GetExamByIdRequest getExamByIdRequest = GetExamByIdRequest.newBuilder()
				.addAllExamId(Collections.singleton(examId))
				.build();
		GetExamByIdResponse getExamByIdResponse = Futures.getUnchecked(adminExamService.getExamById(head, getExamByIdRequest));
		
		List<Long> userIdList = new ArrayList<Long>();

		if (!userName.isEmpty()) {
			GetUserListRequest getUserRequest = GetUserListRequest.newBuilder()
					.setKeyword(userName)
					.setStart(start)
					.setLength(length)
					.build();
			GetUserListResponse getUserResponse = Futures.getUnchecked(adminUserService.getUserList(head, getUserRequest));
			for (UserProtos.User user : getUserResponse.getUserList()) {
				userIdList.add(user.getBase().getUserId());
			}
		} else {
			for (UserResult userResult : response.getUserReusltList()) {
				userIdList.add(userResult.getUserId());
			}
		}
		GetUserByIdRequest getUserByIdRequest = GetUserByIdRequest.newBuilder()
				.addAllUserId(userIdList)
				.build();
		GetUserByIdResponse getUserByIdResponse = Futures.getUnchecked(adminUserService.getUserById(head, getUserByIdRequest));
		Map<Long, User> userMap = new HashMap<Long, User>();
		for (User user : getUserByIdResponse.getUserList()) {
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

		JsonArray jsonArray = new JsonArray();
		for (UserResult userResult : response.getUserReusltList()) {
			JsonObject resultObject = new JsonObject();
			User user = userMap.get(userResult.getUserId());
			
			if (user == null) {
				resultObject.addProperty("user_id", userResult.getUserId());
				resultObject.addProperty("user_name", "【未知用户】");
				if (getExamByIdResponse.getExam(0) != null) {
					resultObject.addProperty("exam_id", getExamByIdResponse.getExam(0).getExamName());
				} else {
					resultObject.addProperty("exam_id", "");
				}
				resultObject.addProperty("start_time", userResult.getStartTime());
				resultObject.addProperty("submit_time", userResult.getSubmitTime());
				resultObject.addProperty("score", userResult.getScore());
			} else {
				resultObject.addProperty("user_id", user.getBase().getUserId());
				resultObject.addProperty("user_name", user.getBase().getUserName());
				if (getExamByIdResponse.getExam(0) != null) {
					resultObject.addProperty("exam_id", getExamByIdResponse.getExam(0).getExamName());
				} else {
					resultObject.addProperty("exam_id", "");
				}
				resultObject.addProperty("start_time", userResult.getStartTime());
				resultObject.addProperty("submit_time", userResult.getSubmitTime());
				resultObject.addProperty("score", userResult.getScore());
				
				UserInfoUtil.getUserTeamPosition(resultObject, user, teamMap, positionMap);
				resultObject.addProperty("mobile_no", DBUtil.COMMA_JOINER.join(user.getBase().getMobileNoList()));
			}
			
			jsonArray.add(resultObject);
		}
		
		JsonObject resultObj = new JsonObject();
		resultObj.add("user_result", jsonArray);
		resultObj.addProperty("total", response.getTotal());
		resultObj.addProperty("filtered_size", response.getFilteredSize());
		
		httpResponse.setContentType("application/json;charset=UTF-8");
		JsonUtil.GSON.toJson(resultObj, httpResponse.getWriter());
	}
}
