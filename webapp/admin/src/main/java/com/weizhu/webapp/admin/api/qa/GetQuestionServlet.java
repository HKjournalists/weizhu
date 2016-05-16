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
public class GetQuestionServlet extends HttpServlet {
	private final Provider<AdminHead> adminHeadProvider;
	private final AdminQAService adminQAService;
	private final AdminUserService adminUserService;
	private final AdminService adminService;

	@Inject
	public GetQuestionServlet(Provider<AdminHead> adminHeadProvider, AdminQAService adminQAService, AdminUserService adminUserService, AdminService adminService) {
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
		Integer categoryId = ParamUtil.getInt(httpRequest, "category_id", null);
		int length = ParamUtil.getInt(httpRequest, "length", -1);
		String keyword = ParamUtil.getString(httpRequest, "keyword", null);
		// 4. 调用Service
		final AdminHead head = this.adminHeadProvider.get();

		AdminQAProtos.GetQuestionRequest.Builder requestBuilder = AdminQAProtos.GetQuestionRequest.newBuilder();
		requestBuilder.setLength(length);
		if (start != null) {
			requestBuilder.setStart(start);
		}
		if (categoryId != null) {
			requestBuilder.setCategoryId(categoryId);
		}
		if (keyword != null) {
			requestBuilder.setKeyword(keyword);
		}
		AdminQAProtos.GetQuestionResponse response = Futures.getUnchecked(this.adminQAService.getQuestion(head, requestBuilder.build()));
		//获取用户信息
		Set<Long> userIds = new TreeSet<Long>();
		Set<Long> adminIds = new TreeSet<Long>();
		for (QAProtos.Question question : response.getQuestionList()) {
			if (question.hasUserId()) {
				userIds.add(question.getUserId());
			} else {
				adminIds.add(question.getAdminId());
			}
		}
		for (QAProtos.Category category : response.getRefCategoryList()) {
			if (category.hasUserId()) {
				userIds.add(category.getUserId());
			} else {
				adminIds.add(category.getAdminId());
			}
		}
		AdminUserProtos.GetUserByIdResponse userResponse = Futures.getUnchecked(this.adminUserService.getUserById(head, GetUserByIdRequest.newBuilder()
				.addAllUserId(userIds)
				.build()));
		AdminProtos.GetAdminByIdResponse adminResponse = Futures.getUnchecked(this.adminService.getAdminById(head,
				AdminProtos.GetAdminByIdRequest.newBuilder().addAllAdminId(adminIds).build()));
		Map<Long, AdminProtos.Admin> adminMap = QAServletUtil.getAdminMap(adminResponse.getAdminList());
		Map<Long, UserProtos.User> userMap = QAServletUtil.getUserMap(userResponse.getUserList());
		Map<Integer, QAProtos.Category> categoryMap = QAServletUtil.getCategoryMap(response.getRefCategoryList());
		//拼接json
		JsonObject result = new JsonObject();
		JsonArray questions = new JsonArray();
		for (int i = 0; i < response.getQuestionCount(); i++) {
			QAProtos.Question question = response.getQuestion(i);
			JsonObject u = new JsonObject();
			long userId = question.hasUserId() ? question.getUserId() : question.getAdminId();
			boolean isAdmin = !question.hasUserId();
			u.addProperty("question_id", question.getQuestionId());
			u.addProperty("question_content", question.getQuestionContent());
			u.addProperty("user_id", userId);
			u.addProperty("answer_num", question.getAnswerNum());
			u.addProperty("category_id", question.getCategoryId());
			u.addProperty("create_time", question.getCreateTime());
			u.addProperty("user_name", QAServletUtil.getUserName(userMap, adminMap, userId, isAdmin));
			u.addProperty("category_name", QAServletUtil.getCategoryName(categoryMap, question.getCategoryId()));
			questions.add(u);
		}
		result.add("question", questions);
		result.addProperty("total_size", response.getTotalSize());
		result.addProperty("filtered_size", response.getFilteredSize());
		httpResponse.setContentType("application/json;charset=UTF-8");
		JsonUtil.GSON.toJson(result, httpResponse.getWriter());
	}

}
