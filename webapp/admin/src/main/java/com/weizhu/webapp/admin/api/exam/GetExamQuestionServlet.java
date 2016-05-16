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
import com.weizhu.common.utils.JsonUtil;
import com.weizhu.proto.AdminExamProtos.GetExamByIdRequest;
import com.weizhu.proto.AdminExamProtos.GetExamByIdResponse;
import com.weizhu.proto.AdminExamProtos.GetExamQuestionRandomRequest;
import com.weizhu.proto.AdminExamProtos.GetExamQuestionRandomResponse;
import com.weizhu.proto.AdminExamProtos.GetExamQuestionRequest;
import com.weizhu.proto.AdminExamProtos.GetExamQuestionResponse;
import com.weizhu.proto.AdminExamService;
import com.weizhu.proto.AdminProtos.AdminHead;
import com.weizhu.proto.ExamProtos;
import com.weizhu.web.ParamUtil;

@Singleton
public class GetExamQuestionServlet extends HttpServlet {

	private static final long serialVersionUID = 1L;

	private final Provider<AdminHead> adminHeadProvider;
	private final AdminExamService adminExamService;
	
	@Inject
	public GetExamQuestionServlet(Provider<AdminHead> adminHeadProvider, AdminExamService adminExamService) {
		this.adminHeadProvider = adminHeadProvider;
		this.adminExamService = adminExamService;
	}
	
	@Override
	public void doGet(HttpServletRequest httpRequest, HttpServletResponse httpResponse) throws IOException {
		this.doPost(httpRequest, httpResponse);
	}
	
	@Override
	public void doPost(HttpServletRequest httpRequest, HttpServletResponse httpResponse) throws IOException {
		final Integer examId = ParamUtil.getInt(httpRequest, "exam_id", null);
		
		if (examId == null) {
			JsonObject result = new JsonObject();
			result.addProperty("result", "FAIL_EXAM_INVALID");
			result.addProperty("fail_text", "请选择正确的考试！");

			httpResponse.setContentType("application/json;charset=UTF-8");
			JsonUtil.GSON.toJson(result, httpResponse.getWriter());
			return ;
		}
		
		final AdminHead head = adminHeadProvider.get();
		
		GetExamByIdResponse getExamByIdResponse = Futures.getUnchecked(adminExamService.getExamById(head, GetExamByIdRequest.newBuilder()
				.addExamId(examId)
				.build()));
		ExamProtos.Exam exam = null;
		for (ExamProtos.Exam tmp : getExamByIdResponse.getExamList()) {
			if (tmp.getExamId() == examId) {
				exam = tmp;
			}
		}
		
		if (exam != null) {
			if (exam.getType().equals(ExamProtos.Exam.Type.MANUAL)) {
				GetExamQuestionResponse response = Futures.getUnchecked(adminExamService.getExamQuestion(head, GetExamQuestionRequest.newBuilder()
						.setExamId(examId)
						.build()));
				
				httpResponse.setContentType("application/json;charset=UTF-8");
				JsonUtil.PROTOBUF_JSON_FORMAT.print(response, httpResponse.getWriter());
			} else if (exam.getType().equals(ExamProtos.Exam.Type.AUTO)) {
				GetExamQuestionRandomResponse response = Futures.getUnchecked(adminExamService.getExamQuestionRandom(head, GetExamQuestionRandomRequest.newBuilder()
						.setExamId(examId)
						.build()));
				
				httpResponse.setContentType("application/json;charset=UTF-8");
				JsonUtil.PROTOBUF_JSON_FORMAT.print(response, httpResponse.getWriter());
			}
		}
	}
}
