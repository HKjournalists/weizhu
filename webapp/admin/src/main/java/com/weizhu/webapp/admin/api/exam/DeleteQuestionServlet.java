package com.weizhu.webapp.admin.api.exam;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.common.util.concurrent.Futures;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.weizhu.proto.AdminExamProtos.DeleteQuestionRequest;
import com.weizhu.proto.AdminExamProtos.DeleteQuestionResponse;
import com.weizhu.common.utils.JsonUtil;
import com.weizhu.proto.AdminExamService;
import com.weizhu.proto.AdminProtos.AdminHead;
import com.weizhu.web.ParamUtil;

@Singleton
public class DeleteQuestionServlet extends HttpServlet {

	private static final long serialVersionUID = 1L;

	private final Provider<AdminHead> adminHeadProvider;
	private final AdminExamService adminExamService;
	
	@Inject
	public DeleteQuestionServlet(Provider<AdminHead> adminHeadProvider, AdminExamService adminExamService) {
		this.adminHeadProvider = adminHeadProvider;
		this.adminExamService = adminExamService;
	}
	
	@Override
	public void doGet(HttpServletRequest httpRequest, HttpServletResponse httpResponse) throws IOException {
		this.doPost(httpRequest, httpResponse);
	}
	
	@Override
	public void doPost(HttpServletRequest httpRequest, HttpServletResponse httpResponse) throws IOException {
		final List<Integer> questionIdList = ParamUtil.getIntList(httpRequest, "question_id", Collections.<Integer>emptyList());
		
		final AdminHead head = adminHeadProvider.get();
		
		DeleteQuestionRequest request = DeleteQuestionRequest.newBuilder()
				.addAllQuestionId(questionIdList)
				.build();
		
		DeleteQuestionResponse response = Futures.getUnchecked(adminExamService.deleteQuestion(head, request));
		
		httpResponse.setContentType("application/json;charset=UTF-8");
		JsonUtil.PROTOBUF_JSON_FORMAT.print(response, httpResponse.getWriter());
	}
}
