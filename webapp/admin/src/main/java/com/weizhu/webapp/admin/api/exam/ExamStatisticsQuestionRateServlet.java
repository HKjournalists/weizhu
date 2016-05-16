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
import com.weizhu.proto.AdminExamProtos.GetQuestionCorrectRateRequest;
import com.weizhu.proto.AdminExamProtos.GetQuestionCorrectRateResponse;
import com.weizhu.proto.AdminExamService;
import com.weizhu.proto.AdminProtos.AdminHead;
import com.weizhu.web.ParamUtil;

@Singleton
public class ExamStatisticsQuestionRateServlet extends HttpServlet {

	private static final long serialVersionUID = 1L;

	private final Provider<AdminHead> adminHeadProvider;
	private final AdminExamService adminExamService;

	@Inject
	public ExamStatisticsQuestionRateServlet(Provider<AdminHead> adminHeadProvider,
			AdminExamService adminExamService) {
		this.adminHeadProvider = adminHeadProvider;
		this.adminExamService = adminExamService;
	}
	
	@Override
	public void doPost(HttpServletRequest httpRequest, HttpServletResponse httpResponse) throws IOException {
		final int examId = ParamUtil.getInt(httpRequest, "exam_id", 0);
		
		final int start = ParamUtil.getInt(httpRequest, "start", 0);
		final int length = ParamUtil.getInt(httpRequest, "length", 0);
		
		final AdminHead head = adminHeadProvider.get();
		
		GetQuestionCorrectRateResponse response = Futures.getUnchecked(adminExamService.getQuestionCorrectRate(head, GetQuestionCorrectRateRequest.newBuilder()
				.setExamId(examId)
				.setStart(start)
				.setLength(length)
				.build()));
		
		httpResponse.setContentType("application/json;charset=UTF-8");
		JsonUtil.PROTOBUF_JSON_FORMAT.print(response, httpResponse.getWriter());
	}
	
	@Override
	public void doGet(HttpServletRequest httpRequest, HttpServletResponse httpResponse) throws IOException {
		this.doPost(httpRequest, httpResponse);
	}
}
