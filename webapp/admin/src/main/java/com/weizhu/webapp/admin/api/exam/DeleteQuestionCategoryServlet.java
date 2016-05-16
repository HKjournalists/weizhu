package com.weizhu.webapp.admin.api.exam;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.common.util.concurrent.Futures;
import com.google.gson.JsonObject;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.weizhu.common.db.DBUtil;
import com.weizhu.common.utils.JsonUtil;
import com.weizhu.proto.AdminExamProtos.DeleteQuestionCategoryRequest;
import com.weizhu.proto.AdminExamProtos.DeleteQuestionCategoryResponse;
import com.weizhu.proto.AdminExamService;
import com.weizhu.proto.AdminProtos.AdminHead;
import com.weizhu.web.ParamUtil;

@Singleton
public class DeleteQuestionCategoryServlet extends HttpServlet {

	private static final long serialVersionUID = 1L;

	private final Provider<AdminHead> adminHeadProvider;
	private final AdminExamService adminExamService;
	
	@Inject
	public DeleteQuestionCategoryServlet(Provider<AdminHead> adminHeadProvider, AdminExamService adminExamService) {
		this.adminHeadProvider = adminHeadProvider;
		this.adminExamService = adminExamService;
	}
	
	@Override
	public void doPost(HttpServletRequest httpRequest, HttpServletResponse httpResponse) throws IOException {
		final String questionCategoryIdStr = ParamUtil.getString(httpRequest, "question_category_id", "");
		
		if (questionCategoryIdStr.isEmpty()) {
			JsonObject result = new JsonObject();
			result.addProperty("result", "FAIL_QUESTION_CATEGORY_INVALID");
			result.addProperty("fail_text", "请选择正确的题库分类！");

			httpResponse.setContentType("application/json;charset=UTF-8");
			JsonUtil.GSON.toJson(result, httpResponse.getWriter());
			return ;
		}
		
		List<Integer> questionCategoryIdList = new ArrayList<Integer>(); 
		try {
			for (String questionCategoryId : DBUtil.COMMA_SPLITTER.split(questionCategoryIdStr)) {
				questionCategoryIdList.add(Integer.parseInt(questionCategoryId));
			}
		} catch (Exception ex) {
			JsonObject result = new JsonObject();
			result.addProperty("result", "FAIL_QUESTION_CATEGORY_INVALID");
			result.addProperty("fail_text", "请选择正确的题库分类！");

			httpResponse.setContentType("application/json;charset=UTF-8");
			JsonUtil.GSON.toJson(result, httpResponse.getWriter());
			return ;
		}
		
		AdminHead head = adminHeadProvider.get();
		DeleteQuestionCategoryRequest request = DeleteQuestionCategoryRequest.newBuilder()
				.addAllCategoryId(questionCategoryIdList)
				.build();
		
		DeleteQuestionCategoryResponse response = Futures.getUnchecked(adminExamService.deleteQuestionCategory(head, request));
		
		httpResponse.setContentType("application/json;charset=UTF-8");
		JsonUtil.PROTOBUF_JSON_FORMAT.print(response, httpResponse.getWriter());
	}
	
	@Override
	public void doGet(HttpServletRequest httpRequest, HttpServletResponse httpResponse) throws IOException {
		this.doPost(httpRequest, httpResponse);
	}
}
