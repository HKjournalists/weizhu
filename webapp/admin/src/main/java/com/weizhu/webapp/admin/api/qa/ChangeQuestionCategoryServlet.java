package com.weizhu.webapp.admin.api.qa;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.common.util.concurrent.Futures;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.weizhu.common.utils.JsonUtil;
import com.weizhu.proto.AdminQAProtos;
import com.weizhu.proto.AdminQAService;
import com.weizhu.proto.AdminProtos.AdminHead;
import com.weizhu.web.ParamUtil;

@Singleton
@SuppressWarnings("serial")
public class ChangeQuestionCategoryServlet extends HttpServlet {
	private final Provider<AdminHead> adminHeadProvider;
	private final AdminQAService adminQAService;

	@Inject
	public ChangeQuestionCategoryServlet(Provider<AdminHead> adminHeadProvider, AdminQAService adminQAService) {
		this.adminHeadProvider = adminHeadProvider;
		this.adminQAService = adminQAService;
	}

	@Override
	protected void doGet(HttpServletRequest httpRequest, HttpServletResponse httpResponse) throws ServletException, IOException {
		this.doPost(httpRequest, httpResponse);
	}

	@Override
	protected void doPost(HttpServletRequest httpRequest, HttpServletResponse httpResponse) throws ServletException, IOException {
		// 1. 取出参数
		List<Integer> questionIds = ParamUtil.getIntList(httpRequest, "question_id", Collections.<Integer> emptyList());
		int categoryId = ParamUtil.getInt(httpRequest, "category_id", -1);
		// 4. 调用Service
		final AdminHead head = this.adminHeadProvider.get();

		AdminQAProtos.ChangeQuestionCategoryRequest request = AdminQAProtos.ChangeQuestionCategoryRequest.newBuilder()
				.setCategoryId(categoryId)
				.addAllQuestionId(questionIds)
				.build();

		AdminQAProtos.ChangeQuestionCategoryResponse response = Futures.getUnchecked(this.adminQAService.changeQuestionCategory(head, request));

		httpResponse.setContentType("application/json;charset=UTF-8");
		JsonUtil.PROTOBUF_JSON_FORMAT.print(response, httpResponse.getWriter());
	}

}