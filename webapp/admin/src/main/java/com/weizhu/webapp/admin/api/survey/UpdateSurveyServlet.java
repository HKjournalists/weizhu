package com.weizhu.webapp.admin.api.survey;

import java.io.IOException;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.common.util.concurrent.Futures;
import com.google.gson.JsonObject;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.weizhu.proto.AdminProtos.AdminHead;
import com.weizhu.common.utils.JsonUtil;
import com.weizhu.proto.SurveyProtos;
import com.weizhu.proto.SurveyProtos.UpdateSurveyRequest;
import com.weizhu.proto.SurveyProtos.UpdateSurveyResponse;
import com.weizhu.proto.SurveyService;
import com.weizhu.web.ParamUtil;

@Singleton
public class UpdateSurveyServlet extends HttpServlet {

	private static final long serialVersionUID = 1L;

	private final Provider<AdminHead> adminHeadProvider;
	private final SurveyService surveyService;
	
	@Inject
	public UpdateSurveyServlet(Provider<AdminHead> adminHeadProvider, SurveyService surveyService) {
		this.adminHeadProvider = adminHeadProvider;
		this.surveyService = surveyService;
	}
	
	@Override
	public void doPost(HttpServletRequest httpRequest, HttpServletResponse httpResponse) throws IOException {
		final int surveyId = ParamUtil.getInt(httpRequest, "survey_id", 0);
		final String surveyName = ParamUtil.getString(httpRequest, "survey_name", "");
		final String surveyDesc = ParamUtil.getString(httpRequest, "survey_desc", "");
		final String imageName = ParamUtil.getString(httpRequest, "image_name", null);
		final int startTime = ParamUtil.getInt(httpRequest, "start_time", 0);
		final int endTime = ParamUtil.getInt(httpRequest, "end_time", 0);
		final String showResultType = ParamUtil.getString(httpRequest, "show_result_type", "");
		final Integer allowModelId = ParamUtil.getInt(httpRequest, "allow_model_id", null);
		
		SurveyProtos.ShowResultType tmpShowResultType = null;
		for (SurveyProtos.ShowResultType type : SurveyProtos.ShowResultType.values()) {
			if (type.name().equals(showResultType)) {
				tmpShowResultType = type;
			}
		}
		if (tmpShowResultType == null) {
			JsonObject result = new JsonObject();
			result.addProperty("result", "FAIL_SHOW_RESULT_INVALID");
			result.addProperty("fail_text", "结果显示类型不正确！");

			httpResponse.setContentType("application/json;charset=UTF-8");
			JsonUtil.GSON.toJson(result, httpResponse.getWriter());
			return ;
		}
		
		UpdateSurveyRequest.Builder requestBuilder = UpdateSurveyRequest.newBuilder()
				.setSurveyId(surveyId)
				.setSurveyName(surveyName)
				.setSurveyDesc(surveyDesc)
				.setStartTime(startTime)
				.setEndTime(endTime)
				.setShowResultType(tmpShowResultType);
		
		if (imageName != null) {
			requestBuilder.setImageName(imageName);
		}
		
		if (allowModelId != null) {
			requestBuilder.setAllowModelId(allowModelId);
		}
		
		final AdminHead adminHead = adminHeadProvider.get();
		
		UpdateSurveyResponse response = Futures.getUnchecked(surveyService.updateSurvey(adminHead, requestBuilder.build()));
		
		httpResponse.setContentType("application/json;charset=UTF-8");
		JsonUtil.PROTOBUF_JSON_FORMAT.print(response, httpResponse.getWriter());
	}
	
	@Override
	public void doGet(HttpServletRequest httpRequest, HttpServletResponse httpResponse) throws IOException {
		this.doPost(httpRequest, httpResponse);
	}
}
