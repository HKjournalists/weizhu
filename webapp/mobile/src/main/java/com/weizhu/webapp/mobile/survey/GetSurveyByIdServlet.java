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
import com.weizhu.proto.SurveyProtos.GetSurveyByIdRequest;
import com.weizhu.proto.SurveyProtos.GetSurveyByIdResponse;
import com.weizhu.proto.UploadProtos.GetUploadUrlPrefixResponse;
import com.weizhu.proto.UploadService;
import com.weizhu.common.service.ServiceUtil;
import com.weizhu.common.utils.JsonUtil;
import com.weizhu.proto.SurveyService;
import com.weizhu.proto.WeizhuProtos.RequestHead;
import com.weizhu.web.ParamUtil;

@Singleton
public class GetSurveyByIdServlet extends HttpServlet {

	private static final long serialVersionUID = 1L;

	private final Provider<RequestHead> requestHeadProvider;
	private final SurveyService surveyService;
	private final UploadService uploadService;
	
	@Inject
	public GetSurveyByIdServlet(Provider<RequestHead> requestHeadProvider, SurveyService surveyService, UploadService uploadService) {
		this.requestHeadProvider = requestHeadProvider;
		this.surveyService = surveyService;
		this.uploadService = uploadService;
	}
	
	@Override
	public void doPost(HttpServletRequest httpRequest, HttpServletResponse httpResponse) throws IOException {
		final int surveyId = ParamUtil.getInt(httpRequest, "survey_id", 0);
		
		final RequestHead requestHead = requestHeadProvider.get();
		
		GetUploadUrlPrefixResponse getUploadUrlPrefixResponse = Futures.getUnchecked(this.uploadService.getUploadUrlPrefix(requestHead, ServiceUtil.EMPTY_REQUEST));
		
		GetSurveyByIdResponse response = Futures.getUnchecked(surveyService.getSurveyById(requestHead, GetSurveyByIdRequest.newBuilder()
				.setSurveyId(surveyId)
				.build()));
		
		JsonObject result = new JsonObject();
		
		final int now = (int) (System.currentTimeMillis()/ 1000L);
		if (response.hasSurvey()) {
			if (now < response.getSurvey().getStartTime()) {
				result.addProperty("survey_state", "NOT_START");
			} else if (now >= response.getSurvey().getStartTime() && now < response.getSurvey().getEndTime()) {
				result.addProperty("survey_state", "ON_GOING");
			} else if (now >= response.getSurvey().getEndTime()) {
				result.addProperty("survey_state", "END");
			}
		} else {
			result.addProperty("survey_state", "NULL");
		}
		result.addProperty("image_url_prefix", getUploadUrlPrefixResponse.getImageUrlPrefix());
		result.addProperty("survey", JsonUtil.PROTOBUF_JSON_FORMAT.printToString(response));
		
		httpResponse.setContentType("application/json;charset=UTF-8");
		JsonUtil.GSON.toJson(result, httpResponse.getWriter());
	}
	
	@Override
	public void doGet(HttpServletRequest httpRequest, HttpServletResponse httpResponse) throws IOException {
		this.doPost(httpRequest, httpResponse);
	}
}
