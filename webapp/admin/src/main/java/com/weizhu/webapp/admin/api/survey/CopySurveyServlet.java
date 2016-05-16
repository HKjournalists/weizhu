package com.weizhu.webapp.admin.api.survey;

import java.io.IOException;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.common.util.concurrent.Futures;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.weizhu.common.utils.JsonUtil;
import com.weizhu.proto.SurveyProtos.CopySurveyResponse;
import com.weizhu.proto.SurveyService;
import com.weizhu.proto.AdminProtos.AdminHead;
import com.weizhu.proto.SurveyProtos.CopySurveyRequest;
import com.weizhu.web.ParamUtil;

@Singleton
public class CopySurveyServlet extends HttpServlet {

	private static final long serialVersionUID = 1L;

	private final Provider<AdminHead> adminHeadProvider;
	private final SurveyService surveyService;
	
	@Inject
	public CopySurveyServlet(Provider<AdminHead> adminHeadProvider, SurveyService surveyService) {
		this.adminHeadProvider = adminHeadProvider;
		this.surveyService = surveyService;
	}
	
	@Override
	public void doPost(HttpServletRequest httpRequest, HttpServletResponse httpResponse) throws IOException {
		final int surveyId = ParamUtil.getInt(httpRequest, "survey_id", 0);
		final String surveyName = ParamUtil.getString(httpRequest, "survey_name", "");
		
		final int startTime = ParamUtil.getInt(httpRequest, "start_time", 0);
		final int endTime = ParamUtil.getInt(httpRequest, "end_time", 0);
		
		final Integer allowModelId = ParamUtil.getInt(httpRequest, "allow_model_id", null);
		
		CopySurveyRequest.Builder requestBuilder = CopySurveyRequest.newBuilder()
				.setSurveyId(surveyId)
				.setSurveyName(surveyName)
				.setStartTime(startTime)
				.setEndTime(endTime);
		if (allowModelId != null) {
			requestBuilder.setAllowModelId(allowModelId);
		}
		
		final AdminHead head = adminHeadProvider.get();
		
		CopySurveyResponse response = Futures.getUnchecked(surveyService.copySurvey(head, requestBuilder.build()));
		
		httpResponse.setContentType("application/json;charset=UTF-8");
		JsonUtil.PROTOBUF_JSON_FORMAT.print(response, httpResponse.getWriter());
	}
	
	@Override
	public void doGet(HttpServletRequest httpRequest, HttpServletResponse httpResponse) throws IOException {
		this.doPost(httpRequest, httpResponse);
	}
}
