package com.weizhu.webapp.mobile.qa;

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
import com.weizhu.common.service.ServiceUtil;
import com.weizhu.common.utils.JsonUtil;
import com.weizhu.proto.QAProtos;
import com.weizhu.proto.QAService;
import com.weizhu.proto.UploadService;
import com.weizhu.proto.UserProtos;
import com.weizhu.proto.UserProtos.GetUserResponse;
import com.weizhu.proto.UserService;
import com.weizhu.proto.WeizhuProtos.EmptyRequest;
import com.weizhu.proto.WeizhuProtos.RequestHead;

@Singleton
@SuppressWarnings("serial")
public class GetCategoryServlet extends HttpServlet {
	private final Provider<RequestHead> requestHeadProvider;
	private final QAService qaService;
	private final UserService userService;
	private final UploadService uploadService;

	@Inject
	public GetCategoryServlet(Provider<RequestHead> requestHeadProvider, QAService qaService, UserService userService, UploadService uploadService) {
		this.requestHeadProvider = requestHeadProvider;
		this.qaService = qaService;
		this.userService = userService;
		this.uploadService = uploadService;
	}

	@Override
	protected void doGet(HttpServletRequest httpRequest, HttpServletResponse httpResponse) throws ServletException, IOException {
		this.doPost(httpRequest, httpResponse);
	}

	@Override
	protected void doPost(HttpServletRequest httpRequest, HttpServletResponse httpResponse) throws ServletException, IOException {

		final RequestHead head = requestHeadProvider.get();

		EmptyRequest request = EmptyRequest.newBuilder().build();

		QAProtos.GetCategoryResponse response = Futures.getUnchecked(this.qaService.getCategory(head, request));
		//获取用户信息
		Set<Long> userIds = new TreeSet<Long>();
		for (QAProtos.Category category : response.getCategoryList()) {
			if (category.hasUserId()) {
				userIds.add(category.getUserId());
			}
		}
		GetUserResponse userResponse = Futures.getUnchecked(this.userService.getUserById(head, UserProtos.GetUserByIdRequest.newBuilder().addAllUserId(userIds).build()));
		String imageUrlPrefix = Futures.getUnchecked(this.uploadService.getUploadUrlPrefix(head, ServiceUtil.EMPTY_REQUEST)).getImageUrlPrefix();
		Map<Long, JsonObject> userJsonMap = QAServletUtil.getUserJson(userResponse, imageUrlPrefix);
		//拼接json
		JsonObject result = new JsonObject();
		JsonArray categorys = new JsonArray();
		for (int i = 0; i < response.getCategoryCount(); i++) {
			categorys.add(QAServletUtil.getCategoryJson(response.getCategory(i), userJsonMap));
		}
		result.add("category", categorys);
		httpResponse.setContentType("application/json;charset=UTF-8");
		JsonUtil.GSON.toJson(result, httpResponse.getWriter());
	}

}
