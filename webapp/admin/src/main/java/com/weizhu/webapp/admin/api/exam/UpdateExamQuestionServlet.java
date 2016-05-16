package com.weizhu.webapp.admin.api.exam;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

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
import com.weizhu.proto.AdminExamProtos.UpdateExamQuestionRandomRequest;
import com.weizhu.proto.AdminExamProtos.UpdateExamQuestionRandomResponse;
import com.weizhu.proto.AdminExamProtos.UpdateExamQuestionRequest;
import com.weizhu.proto.AdminExamProtos.UpdateExamQuestionResponse;
import com.weizhu.proto.AdminExamService;
import com.weizhu.proto.AdminProtos.AdminHead;
import com.weizhu.web.ParamUtil;

@Singleton
public class UpdateExamQuestionServlet extends HttpServlet {

	private static final long serialVersionUID = 1L;

	private final Provider<AdminHead> adminHeadProvider;
	private final AdminExamService adminExamService;
	
	@Inject
	public UpdateExamQuestionServlet(Provider<AdminHead> adminHeadProvider, AdminExamService adminExamService) {
		this.adminHeadProvider = adminHeadProvider;
		this.adminExamService = adminExamService;
	}
	
	@Override
	public void doGet(HttpServletRequest httpRequest, HttpServletResponse httpResponse) throws IOException {
		this.doPost(httpRequest, httpResponse);
	}
	
	@Override
	public void doPost(HttpServletRequest httpRequest, HttpServletResponse httpResponse) throws IOException {
		final String examQuestion = ParamUtil.getString(httpRequest, "exam_question", "").replace("question_info", "exam_question");
		final int examId = ParamUtil.getInt(httpRequest, "exam_id", -1);
		
		final List<Integer> questionCategoryIdList = ParamUtil.getIntList(httpRequest, "category_id_list", Collections.<Integer>emptyList());
		final int questionNum = ParamUtil.getInt(httpRequest, "question_num", 0);
		
		final AdminHead head = adminHeadProvider.get();
		
		if (questionNum == 0) {
			// "question_info": [{"question_id": 6,"score": 30},{"question_id": 7,"score": 30}] -- web端要把以前的exam_question 修改成这样的。。。
			UpdateExamQuestionRequest.Builder updateExamQuestionBuilder = UpdateExamQuestionRequest.newBuilder();
			try {
				JsonUtil.PROTOBUF_JSON_FORMAT.merge(examQuestion, ExtensionRegistry.getEmptyRegistry(), updateExamQuestionBuilder);
			} catch (Exception ex) {
				JsonObject result = new JsonObject();
				result.addProperty("result", "FAIL_QUESTION_INVALID");
				result.addProperty("fail_text", "请传入正确的考试题目！");

				httpResponse.setContentType("application/json;charset=UTF-8");
				JsonUtil.GSON.toJson(result, httpResponse.getWriter());
				return ;
			}
			
			UpdateExamQuestionResponse response = Futures.getUnchecked(adminExamService.updateExamQuestion(head, updateExamQuestionBuilder.setExamId(examId).build()));
			httpResponse.setContentType("application/json;charset=UTF-8");
			JsonUtil.PROTOBUF_JSON_FORMAT.print(response, httpResponse.getWriter());
		} else {
			UpdateExamQuestionRandomResponse response = Futures.getUnchecked(adminExamService.updateExamQuestionRandom(head, UpdateExamQuestionRandomRequest.newBuilder()
					.addAllCategoryId(questionCategoryIdList)
					.setQuestionNum(questionNum)
					.setExamId(examId)
					.build()));
			
			httpResponse.setContentType("application/json;charset=UTF-8");
			JsonUtil.PROTOBUF_JSON_FORMAT.print(response, httpResponse.getWriter());
		}
	}
}
