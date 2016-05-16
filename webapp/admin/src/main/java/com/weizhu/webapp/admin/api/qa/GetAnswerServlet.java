package com.weizhu.webapp.admin.api.qa;

import java.io.IOException;
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
import com.weizhu.common.utils.JsonUtil;
import com.weizhu.proto.AdminProtos;
import com.weizhu.proto.AdminQAProtos;
import com.weizhu.proto.AdminQAService;
import com.weizhu.proto.AdminService;
import com.weizhu.proto.AdminUserProtos;
import com.weizhu.proto.AdminUserService;
import com.weizhu.proto.QAProtos;
import com.weizhu.proto.UserProtos;
import com.weizhu.proto.AdminProtos.AdminHead;
import com.weizhu.proto.AdminUserProtos.GetUserByIdRequest;
import com.weizhu.web.ParamUtil;

@Singleton
@SuppressWarnings("serial")
public class GetAnswerServlet extends HttpServlet {
	private final Provider<AdminHead> adminHeadProvider;
	private final AdminQAService adminQAService;
	private final AdminUserService adminUserService;
	private final AdminService adminService;

	@Inject
	public GetAnswerServlet(Provider<AdminHead> adminHeadProvider, AdminQAService adminQAService, AdminUserService adminUserService, AdminService adminService) {
		this.adminHeadProvider = adminHeadProvider;
		this.adminQAService = adminQAService;
		this.adminUserService = adminUserService;
		this.adminService = adminService;
	}

	@Override
	protected void doGet(HttpServletRequest httpRequest, HttpServletResponse httpResponse) throws ServletException, IOException {
		this.doPost(httpRequest, httpResponse);
	}

	@Override
	protected void doPost(HttpServletRequest httpRequest, HttpServletResponse httpResponse) throws ServletException, IOException {
		// 1. 取出参数
		Integer start = ParamUtil.getInt(httpRequest, "start", null);
		int questionId = ParamUtil.getInt(httpRequest, "question_id", -1);
		int length = ParamUtil.getInt(httpRequest, "length", -1);
		// 4. 调用Service
		final AdminHead head = this.adminHeadProvider.get();

		AdminQAProtos.GetAnswerRequest.Builder requestBuilder = AdminQAProtos.GetAnswerRequest.newBuilder();
		requestBuilder.setLength(length);
		requestBuilder.setQuestionId(questionId);
		if (start != null) {
			requestBuilder.setStart(start);
		}
		AdminQAProtos.GetAnswerResponse response = Futures.getUnchecked(this.adminQAService.getAnswer(head, requestBuilder.build()));
		//获取用户信息
		Set<Long> userIds = new TreeSet<Long>();
		Set<Long> adminIds = new TreeSet<Long>();
		for (QAProtos.Answer answer : response.getAnswerList()) {
			if (answer.hasUserId()) {
				userIds.add(answer.getUserId());
			} else {
				adminIds.add(answer.getAdminId());
			}
		}
		AdminUserProtos.GetUserByIdResponse userResponse = Futures.getUnchecked(this.adminUserService.getUserById(head, GetUserByIdRequest.newBuilder()
				.addAllUserId(userIds)
				.build()));
		AdminProtos.GetAdminByIdResponse adminResponse = Futures.getUnchecked(this.adminService.getAdminById(head,
				AdminProtos.GetAdminByIdRequest.newBuilder().addAllAdminId(adminIds).build()));
		Map<Long, AdminProtos.Admin> adminMap = QAServletUtil.getAdminMap(adminResponse.getAdminList());
		Map<Long, UserProtos.User> userMap = QAServletUtil.getUserMap(userResponse.getUserList());
		//拼接json
		JsonObject result = new JsonObject();
		JsonArray answers = new JsonArray();
		for (int i = 0; i < response.getAnswerCount(); i++) {
			QAProtos.Answer answer = response.getAnswer(i);
			JsonObject u = new JsonObject();
			long userId = answer.hasUserId() ? answer.getUserId() : answer.getAdminId();
			boolean isAdmin = !answer.hasUserId();
			u.addProperty("answer_id", answer.getAnswerId());
			u.addProperty("question_id", answer.getQuestionId());
			u.addProperty("user_id", userId);
			u.addProperty("answer_content", answer.getAnswerContent());
			u.addProperty("like_num", answer.getLikeNum());
			u.addProperty("create_time", answer.getCreateTime());
			u.addProperty("user_name", QAServletUtil.getUserName(userMap, adminMap, userId, isAdmin));
			answers.add(u);
		}
		result.add("answer", answers);
		result.addProperty("total_size", response.getTotalSize());
		result.addProperty("filtered_size", response.getFilteredSize());
		httpResponse.setContentType("application/json;charset=UTF-8");
		JsonUtil.GSON.toJson(result, httpResponse.getWriter());
	}

}
