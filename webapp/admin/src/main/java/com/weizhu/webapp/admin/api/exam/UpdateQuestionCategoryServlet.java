package com.weizhu.webapp.admin.api.exam;

import java.io.IOException;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.common.util.concurrent.Futures;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.weizhu.proto.AdminExamProtos.UpdateQuestionCategoryRequest;
import com.weizhu.proto.AdminExamProtos.UpdateQuestionCategoryResponse;
import com.weizhu.common.utils.JsonUtil;
import com.weizhu.proto.AdminExamService;
import com.weizhu.proto.AdminProtos.AdminHead;
import com.weizhu.web.ParamUtil;

@Singleton
public class UpdateQuestionCategoryServlet extends HttpServlet {

	private static final long serialVersionUID = 1L;

	private final Provider<AdminHead> adminHeadProvider;
	private final AdminExamService adminExamService;
	
	@Inject
	public UpdateQuestionCategoryServlet(Provider<AdminHead> adminHeadProvider, AdminExamService adminExamService) {
		this.adminHeadProvider = adminHeadProvider;
		this.adminExamService = adminExamService;
	}
	
	public void doPost(HttpServletRequest httpRequest, HttpServletResponse httpResponse) throws IOException {
		final String categoryName = ParamUtil.getString(httpRequest, "category_name", "");
		final Integer categoryId = ParamUtil.getInt(httpRequest, "category_id", -1);
		
		UpdateQuestionCategoryRequest request = UpdateQuestionCategoryRequest.newBuilder()
				.setCategoryId(categoryId)
				.setCategoryName(categoryName)
				.build();
		
		final AdminHead adminHead = adminHeadProvider.get();
		
		UpdateQuestionCategoryResponse response = Futures.getUnchecked(adminExamService.updateQuestionCategory(adminHead, request));
		
		httpResponse.setContentType("application/json;charset=UTF-8");
		JsonUtil.PROTOBUF_JSON_FORMAT.print(response, httpResponse.getWriter());
	}
	
	public void doGet(HttpServletRequest httpRequest, HttpServletResponse httpResponse) throws IOException {
		this.doPost(httpRequest, httpResponse);
	}
}
