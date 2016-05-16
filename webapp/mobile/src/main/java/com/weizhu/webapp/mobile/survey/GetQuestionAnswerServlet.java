package com.weizhu.webapp.mobile.survey;

import java.io.IOException;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.common.util.concurrent.Futures;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.google.protobuf.ByteString;
import com.weizhu.proto.SurveyProtos.GetQuestionAnswerRequest;
import com.weizhu.proto.SurveyProtos.GetQuestionAnswerResponse;
import com.weizhu.common.utils.JsonUtil;
import com.weizhu.proto.SurveyService;
import com.weizhu.proto.WeizhuProtos.RequestHead;
import com.weizhu.web.ParamUtil;

@Singleton
public class GetQuestionAnswerServlet extends HttpServlet {

	private static final long serialVersionUID = 1L;

	private final Provider<RequestHead> requestHeadProvider;
	private final SurveyService surveyService;
	
	@Inject
	public GetQuestionAnswerServlet(Provider<RequestHead> requestHeadProvider, SurveyService surveyService) {
		this.requestHeadProvider = requestHeadProvider;
		this.surveyService = surveyService;
	}
	
	@Override
	public void doPost(HttpServletRequest httpRequest, HttpServletResponse httpResponse) throws IOException {
		final int questionId = ParamUtil.getInt(httpRequest, "question_id", 0);
		final int size = ParamUtil.getInt(httpRequest, "size", 0);
		
		GetQuestionAnswerRequest.Builder requestBuilder = GetQuestionAnswerRequest.newBuilder()
				.setQuestionId(questionId)
				.setSize(size);
		
		final String offSetIndexStr = ParamUtil.getString(httpRequest, "off_set_index", null);
		if (offSetIndexStr != null) {
			requestBuilder.setOffsetIndex(ByteString.copyFromUtf8(offSetIndexStr));
		}
		
		final RequestHead requestHead = requestHeadProvider.get();
		
		GetQuestionAnswerResponse response = Futures.getUnchecked(surveyService.getQuestionAnswer(requestHead, GetQuestionAnswerRequest.newBuilder()
				.setQuestionId(questionId)
				.build()));
		
		httpResponse.setContentType("application/json;charset=UTF-8");
		JsonUtil.PROTOBUF_JSON_FORMAT.print(response, httpResponse.getWriter());
	}
	
	@Override
	public void doGet(HttpServletRequest httpRequest, HttpServletResponse httpResponse) throws IOException {
		this.doPost(httpRequest, httpResponse);
	}
	
}
