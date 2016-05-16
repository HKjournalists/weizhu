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
import com.weizhu.proto.UserProtos;
import com.weizhu.proto.AdminProtos.AdminHead;
import com.weizhu.proto.AdminUserProtos.GetUserByIdRequest;
import com.weizhu.proto.QAProtos;
import com.weizhu.proto.WeizhuProtos.EmptyRequest;

@Singleton
@SuppressWarnings("serial")
public class GetCategoryServlet extends HttpServlet {
	private final Provider<AdminHead> adminHeadProvider;
	private final AdminQAService adminQAService;
	private final AdminUserService adminUserService;
	private final AdminService adminService;

	@Inject
	public GetCategoryServlet(Provider<AdminHead> adminHeadProvider, AdminQAService adminQAService, AdminUserService adminUserService, AdminService adminService) {
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
		// 2. 调用Service
		final AdminHead head = this.adminHeadProvider.get();

		EmptyRequest request = EmptyRequest.newBuilder().build();

		AdminQAProtos.GetCategoryResponse response = Futures.getUnchecked(this.adminQAService.getCategory(head, request));
		//获取用户信息
		Set<Long> userIds = new TreeSet<Long>();
		Set<Long> adminIds = new TreeSet<Long>();
		for (QAProtos.Category category : response.getCategoryList()) {
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
		//拼接json
		JsonObject result = new JsonObject();
		JsonArray categorys = new JsonArray();

		for (int i = 0; i < response.getCategoryCount(); i++) {
			QAProtos.Category category = response.getCategory(i);
			JsonObject u = new JsonObject();
			long userId = category.hasUserId() ? category.getUserId() : category.getAdminId();
			boolean isAdmin = !category.hasUserId();
			u.addProperty("category_id", category.getCategoryId());
			u.addProperty("category_name", category.getCategoryName());
			u.addProperty("question_num", category.getQuestionNum());
			u.addProperty("user_id", userId);
			u.addProperty("create_time", category.getCreateTime());
			u.addProperty("user_name", QAServletUtil.getUserName(userMap, adminMap, userId, isAdmin));
			categorys.add(u);
		}
		result.add("category", categorys);
		httpResponse.setContentType("application/json;charset=UTF-8");
		JsonUtil.GSON.toJson(result, httpResponse.getWriter());
	}

}
