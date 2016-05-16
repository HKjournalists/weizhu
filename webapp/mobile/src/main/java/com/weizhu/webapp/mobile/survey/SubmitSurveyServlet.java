package com.weizhu.webapp.mobile.survey;

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
import com.weizhu.proto.SurveyProtos.SubmitSurveyRequest;
import com.weizhu.proto.SurveyProtos.SubmitSurveyResponse;
import com.weizhu.common.utils.JsonUtil;
import com.weizhu.proto.SurveyProtos;
import com.weizhu.proto.SurveyService;
import com.weizhu.proto.WeizhuProtos.RequestHead;
import com.weizhu.web.ParamUtil;

@Singleton
public class SubmitSurveyServlet extends HttpServlet {

	private static final long serialVersionUID = 1L;

	private final Provider<RequestHead> requestHeadProvider;
	private final SurveyService surveyService;
	
	@Inject
	public SubmitSurveyServlet(Provider<RequestHead> requestHeadProvider, SurveyService surveyService) {
		this.requestHeadProvider = requestHeadProvider;
		this.surveyService = surveyService;
	}
	
	@Override
	public void doPost(HttpServletRequest httpRequest, HttpServletResponse httpResponse) throws IOException {
		final int surveyId = ParamUtil.getInt(httpRequest, "survey_id", 0);
		/**
		 * {\"answer\": [{\"question_id\": 1,\"user_id\": 1111,\"answer_time\": 1444374692,\"vote\": {\"option_id\": [1,2]}},{\"question_id\": 2,\"user_id\": 1111,\"answer_time\": 1444374692,\"input_select\": {\"option_id\": 1}},{\"question_id\": 3,\"user_id\": 1111,\"answer_time\": 1444374692,\"input_text\": {\"result_text\": \"这就是答案\"}}]}
		 */
		final String answerListStr = ParamUtil.getString(httpRequest, "answer_list", "");
		
		SubmitSurveyRequest.Builder requestBuilder = SubmitSurveyRequest.newBuilder()
				.setSurveyId(surveyId);
		try {
			JsonUtil.PROTOBUF_JSON_FORMAT.merge(answerListStr, ExtensionRegistry.getEmptyRegistry(), requestBuilder);
		} catch (Exception ex) {
			JsonObject result = new JsonObject();
			result.addProperty("result", "FAIL_ANSWER_INVALID");
			result.addProperty("fail_text", "传入的题目有误！");

			httpResponse.setContentType("application/json;charset=UTF-8");
			JsonUtil.GSON.toJson(result, httpResponse.getWriter());
			return ;
		}
		
		final RequestHead requestHead = requestHeadProvider.get();
		final long userId = requestHead.getSession().getUserId();
		final int now = (int) (System.currentTimeMillis() / 1000L);
		
		SubmitSurveyRequest.Builder submitSurveyRequest = SubmitSurveyRequest.newBuilder().setSurveyId(surveyId);
		SurveyProtos.Answer.Builder answerBuilder = SurveyProtos.Answer.newBuilder();
		for (SurveyProtos.Answer answer : requestBuilder.getAnswerList()) {
			answerBuilder.clear();
			
			answerBuilder.mergeFrom(answer).setUserId(userId).setAnswerTime(now);
			submitSurveyRequest.addAnswer(answerBuilder.build());
		}
		
		SubmitSurveyResponse response = Futures.getUnchecked(surveyService.submitSurvey(requestHead, submitSurveyRequest.build()));
		
		httpResponse.setContentType("application/json;charset=UTF-8");
		JsonUtil.PROTOBUF_JSON_FORMAT.print(response, httpResponse.getWriter());
	}
	
	@Override
	public void doGet(HttpServletRequest httpRequest, HttpServletResponse httpResponse) throws IOException {
		this.doPost(httpRequest, httpResponse);
	}
}
