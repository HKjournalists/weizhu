package com.weizhu.webapp.admin.api.exam;

import java.io.IOException;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.common.util.concurrent.Futures;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.google.protobuf.ExtensionRegistry;
import com.weizhu.proto.AdminExamProtos.UpdateQuestionRequest;
import com.weizhu.proto.AdminExamProtos.UpdateQuestionResponse;
import com.weizhu.common.utils.JsonUtil;
import com.weizhu.proto.AdminExamService;
import com.weizhu.proto.AdminProtos.AdminHead;
import com.weizhu.proto.ExamProtos;
import com.weizhu.web.ParamUtil;

@Singleton
public class UpdateQuestionServlet extends HttpServlet {

	private static final long serialVersionUID = 1L;

	private final Provider<AdminHead> adminHeadProvider;
	private final AdminExamService adminExamService;
	
	@Inject
	public UpdateQuestionServlet(Provider<AdminHead> adminHeadProvider, AdminExamService adminExamService) {
		this.adminHeadProvider = adminHeadProvider;
		this.adminExamService = adminExamService;
	}
	
	@Override
	public void doGet(HttpServletRequest httpRequest, HttpServletResponse httpResponse) throws IOException {
		this.doPost(httpRequest, httpResponse);
	}
	
	@Override
	public void doPost(HttpServletRequest httpRequest, HttpServletResponse httpResponse) throws IOException {
		final Integer questionId = ParamUtil.getInt(httpRequest, "question_id", -1);
		final String questionName = ParamUtil.getString(httpRequest, "question_name", "");
		
		final String questionType = ParamUtil.getString(httpRequest, "type", "");
		
		final String optionParam = ParamUtil.getString(httpRequest, "option", "");
		
		UpdateQuestionRequest.Builder updateQuestionBuilder = UpdateQuestionRequest.newBuilder()
				.setQuestionId(questionId)
				.setQuestionName(questionName);
		
		if (!questionType.isEmpty()) {
			for (ExamProtos.Question.Type type : ExamProtos.Question.Type.values()) {
				if (type.name().equals(questionType)) {
					updateQuestionBuilder.setType(type);
					break;
				}
			}
		}
		
		JsonUtil.PROTOBUF_JSON_FORMAT.merge(optionParam, ExtensionRegistry.getEmptyRegistry(), updateQuestionBuilder);
		
		final AdminHead head = adminHeadProvider.get();
		
		UpdateQuestionResponse response = Futures.getUnchecked(adminExamService.updateQuestion(head, updateQuestionBuilder.build()));
		
		httpResponse.setContentType("application/json;charset=UTF-8");
		JsonUtil.PROTOBUF_JSON_FORMAT.print(response, httpResponse.getWriter());
	}
}
