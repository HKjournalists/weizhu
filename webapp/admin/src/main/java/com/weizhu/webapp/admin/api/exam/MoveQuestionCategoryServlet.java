package com.weizhu.webapp.admin.api.exam;

import java.io.IOException;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.common.util.concurrent.Futures;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.weizhu.common.utils.JsonUtil;
import com.weizhu.proto.AdminExamProtos.MoveQuestionCategoryRequest;
import com.weizhu.proto.AdminExamProtos.MoveQuestionCategoryResponse;
import com.weizhu.proto.AdminExamService;
import com.weizhu.proto.AdminProtos.AdminHead;
import com.weizhu.web.ParamUtil;

@Singleton
public class MoveQuestionCategoryServlet extends HttpServlet {

	private static final long serialVersionUID = 1L;

	private final Provider<AdminHead> adminHeadProvider;
	private final AdminExamService adminExamService;

	@Inject
	public MoveQuestionCategoryServlet(Provider<AdminHead> adminHeadProvider, AdminExamService adminExamService) {
		this.adminHeadProvider = adminHeadProvider;
		this.adminExamService = adminExamService;
	}
	
	@Override
	public void doPost(HttpServletRequest httpRequest, HttpServletResponse httpResponse) throws IOException {
		final AdminHead adminHead = adminHeadProvider.get();
		
		final Integer categoryId = ParamUtil.getInt(httpRequest, "category_id", null);
		final Integer parentCategoryId = ParamUtil.getInt(httpRequest, "parent_category_id", null);
		
		MoveQuestionCategoryRequest request = MoveQuestionCategoryRequest.newBuilder()
				.setCategoryId(categoryId)
				.setParentCategoryId(parentCategoryId)
				.build();
		MoveQuestionCategoryResponse response = Futures.getUnchecked(adminExamService.moveQuestionCategoryResponse(adminHead, request));
		
		httpResponse.setContentType("application/json;charset=UTF-8");
		JsonUtil.GSON.toJson(response, httpResponse.getWriter());
	}
	
	@Override
	public void doGet(HttpServletRequest httpRequest, HttpServletResponse httpResponse) throws IOException {
		this.doPost(httpRequest, httpResponse);
	}
}
