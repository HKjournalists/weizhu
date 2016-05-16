package com.weizhu.webapp.admin.api.exam;

import java.io.IOException;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.common.util.concurrent.Futures;
import com.google.gson.JsonObject;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.google.protobuf.ExtensionRegistry;
import com.weizhu.common.utils.JsonUtil;
import com.weizhu.proto.AdminExamProtos.CreateQuestionRequest;
import com.weizhu.proto.AdminExamProtos.CreateQuestionResponse;
import com.weizhu.proto.AdminExamService;
import com.weizhu.proto.AdminProtos.AdminHead;
import com.weizhu.proto.ExamProtos;
import com.weizhu.web.ParamUtil;

@Singleton
public class CreateQuestionServlet extends HttpServlet {

	private static final long serialVersionUID = 1L;

	private final Provider<AdminHead> adminHeadProvider;
	private final AdminExamService adminExamService;
	
	@Inject
	public CreateQuestionServlet(Provider<AdminHead> adminHeadProvider, AdminExamService adminExamService) {
		this.adminHeadProvider = adminHeadProvider;
		this.adminExamService = adminExamService;
	}
	
	@Override
	public void doGet(HttpServletRequest httpRequest, HttpServletResponse httpResponse) throws IOException {
		this.doPost(httpRequest, httpResponse);
	}
	
	@Override
	public void doPost(HttpServletRequest httpRequest, HttpServletResponse httpResponse) throws IOException {
		final String params = ParamUtil.getString(httpRequest, "option", "");
		final String questionName = ParamUtil.getString(httpRequest, "question_name", "");
		final String questionType = ParamUtil.getString(httpRequest, "type", "");
		final Integer questionCategoryId = ParamUtil.getInt(httpRequest, "question_category_id", -1);
		
		final AdminHead head = adminHeadProvider.get();
		
		if (questionCategoryId == null) {
			JsonObject result = new JsonObject();
			result.addProperty("result", "FAIL_QUESTION_CATEGORY_INVALID");
			result.addProperty("fail_text", "请选择正确的题库分类！");

			httpResponse.setContentType("application/json;charset=UTF-8");
			JsonUtil.GSON.toJson(result, httpResponse.getWriter());
			return ;
		}
		
		CreateQuestionRequest.Builder createQuestionBuilder = CreateQuestionRequest.newBuilder()
				.setQuestionName(questionName)
				.setCategoryId(questionCategoryId);
		
		if (!questionType.isEmpty()) {
			for (ExamProtos.Question.Type type : ExamProtos.Question.Type.values()) {
				if (type.name().equals(questionType)) {
					createQuestionBuilder.setType(type);
					break;
				}
			}
		}
		
		try {
			JsonUtil.PROTOBUF_JSON_FORMAT.merge(params, ExtensionRegistry.getEmptyRegistry(), createQuestionBuilder);
		} catch (Exception ex) {
			JsonObject result = new JsonObject();
			result.addProperty("result", "FAIL_OPTION_INVALID");
			result.addProperty("fail_text", "请选择正确的考题信息！");

			httpResponse.setContentType("application/json;charset=UTF-8");
			JsonUtil.GSON.toJson(result, httpResponse.getWriter());
			return ;
		}
		
		CreateQuestionResponse response = Futures.getUnchecked(adminExamService.createQuestion(head, createQuestionBuilder.build()));
		
		httpResponse.setContentType("application/json;charset=UTF-8");
		JsonUtil.PROTOBUF_JSON_FORMAT.print(response, httpResponse.getWriter());
	}
}
