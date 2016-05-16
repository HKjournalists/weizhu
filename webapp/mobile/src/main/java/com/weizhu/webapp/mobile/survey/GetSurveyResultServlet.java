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
import com.weizhu.proto.SurveyProtos.GetSurveyResultRequest;
import com.weizhu.proto.SurveyProtos.GetSurveyResultResponse;
import com.weizhu.common.utils.JsonUtil;
import com.weizhu.proto.SurveyService;
import com.weizhu.proto.WeizhuProtos.RequestHead;
import com.weizhu.web.ParamUtil;

@Singleton
public class GetSurveyResultServlet extends HttpServlet {

	private static final long serialVersionUID = 1L;

	private final Provider<RequestHead> requestHeadProvider;
	private final SurveyService surveyService;
	
	@Inject
	public GetSurveyResultServlet(Provider<RequestHead> requestHeadProvider, SurveyService surveyService) {
		this.requestHeadProvider = requestHeadProvider;
		this.surveyService = surveyService;
	}
	
	@Override
	public void doPost(HttpServletRequest httpRequest, HttpServletResponse httpResponse) throws IOException {
		final int surveyId = ParamUtil.getInt(httpRequest, "survey_id", 0);
		final int size = ParamUtil.getInt(httpRequest, "size", 0);
		
		GetSurveyResultRequest.Builder requestBuilder = GetSurveyResultRequest.newBuilder()
				.setSurveyId(surveyId)
				.setSize(size);
		
		final String offSetIndexStr = ParamUtil.getString(httpRequest, "off_set_index", null);
		if (offSetIndexStr != null) {
			requestBuilder.setOffsetIndex(ByteString.copyFromUtf8(offSetIndexStr));
		}
		
		final RequestHead requestHead = requestHeadProvider.get();
		
		GetSurveyResultResponse response = Futures.getUnchecked(surveyService.getSurveyResult(requestHead, requestBuilder.build()));
		
		httpResponse.setContentType("application/json;charset=UTF-8");
		JsonUtil.PROTOBUF_JSON_FORMAT.print(response, httpResponse.getWriter());
	}
	
	@Override
	public void doGet(HttpServletRequest httpRequest, HttpServletResponse httpResponse) throws IOException {
		this.doPost(httpRequest, httpResponse);
	}
}
