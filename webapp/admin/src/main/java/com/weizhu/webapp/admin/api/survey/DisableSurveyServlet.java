package com.weizhu.webapp.admin.api.survey;

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
import com.weizhu.proto.AdminProtos.AdminHead;
import com.weizhu.proto.SurveyProtos.DisableSurveyRequest;
import com.weizhu.proto.SurveyProtos.DisableSurveyResponse;
import com.weizhu.common.utils.JsonUtil;
import com.weizhu.proto.SurveyService;
import com.weizhu.web.ParamUtil;

@Singleton
public class DisableSurveyServlet extends HttpServlet {

	private static final long serialVersionUID = 1L;

	private final Provider<AdminHead> adminHeadProvider;
	private final SurveyService surveyService;
	
	@Inject
	public DisableSurveyServlet(Provider<AdminHead> adminHeadProvider, SurveyService surveyService) {
		this.adminHeadProvider = adminHeadProvider;
		this.surveyService = surveyService;
	}
	
	@Override
	public void doPost(HttpServletRequest httpRequest, HttpServletResponse httpResponse) throws IOException {
		final List<Integer> surveyIdList = ParamUtil.getIntList(httpRequest, "survey_id_list", Collections.<Integer>emptyList());
		
		DisableSurveyRequest request = DisableSurveyRequest.newBuilder()
				.addAllSurveyId(surveyIdList)
				.build();
		
		final AdminHead adminHead = adminHeadProvider.get();
		
		DisableSurveyResponse response = Futures.getUnchecked(surveyService.disableSurvey(adminHead, request));
		
		httpResponse.setContentType("application/json;charset=UTF-8");
		JsonUtil.PROTOBUF_JSON_FORMAT.print(response, httpResponse.getWriter());
	}
	
	@Override
	public void doGet(HttpServletRequest httpRequest, HttpServletResponse httpResponse) throws IOException {
		this.doPost(httpRequest, httpResponse);
	}
}
