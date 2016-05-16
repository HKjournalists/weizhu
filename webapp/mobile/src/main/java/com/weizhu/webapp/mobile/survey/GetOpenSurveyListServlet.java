package com.weizhu.webapp.mobile.survey;

import java.io.IOException;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.common.util.concurrent.Futures;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.google.protobuf.ByteString;
import com.weizhu.common.service.ServiceUtil;
import com.weizhu.common.utils.HexUtil;
import com.weizhu.common.utils.JsonUtil;
import com.weizhu.proto.SurveyProtos;
import com.weizhu.proto.SurveyProtos.GetOpenSurveyRequest;
import com.weizhu.proto.SurveyProtos.GetOpenSurveyResponse;
import com.weizhu.proto.SurveyService;
import com.weizhu.proto.UploadProtos.GetUploadUrlPrefixResponse;
import com.weizhu.proto.UploadService;
import com.weizhu.proto.WeizhuProtos.RequestHead;
import com.weizhu.web.ParamUtil;

@Singleton
public class GetOpenSurveyListServlet extends HttpServlet {

	private static final long serialVersionUID = 1L;

	private final Provider<RequestHead> requestHeadProvider;
	private final SurveyService surveyService;
	private final UploadService uploadService;
	
	@Inject
	public GetOpenSurveyListServlet(Provider<RequestHead> requestHeadProvider, SurveyService surveyService, UploadService uploadService) {
		this.requestHeadProvider = requestHeadProvider;
		this.surveyService = surveyService;
		this.uploadService = uploadService;
	}
	
	@Override
	public void doPost(HttpServletRequest httpRequest, HttpServletResponse httpResponse) throws IOException {
		final String offSetIndexStr = ParamUtil.getString(httpRequest, "off_set_index", null);
		final int size = ParamUtil.getInt(httpRequest, "size", 0);
		
		GetOpenSurveyRequest.Builder requestBuilder = GetOpenSurveyRequest.newBuilder();
		
		if (offSetIndexStr != null && !offSetIndexStr.equals("0")) {
			requestBuilder.setOffsetIndex(ByteString.copyFrom(HexUtil.hex2bin(offSetIndexStr)));
		}
		
		requestBuilder.setSize(size);
		
		final RequestHead requestHead = requestHeadProvider.get();
		
		GetUploadUrlPrefixResponse getUploadUrlPrefixResponse = Futures.getUnchecked(this.uploadService.getUploadUrlPrefix(requestHead, ServiceUtil.EMPTY_REQUEST));
		
		GetOpenSurveyResponse response = Futures.getUnchecked(surveyService.getOpenSurvey(requestHead, requestBuilder.build()));
		
		JsonObject result = new JsonObject();
		JsonArray array = new JsonArray();
		for (SurveyProtos.Survey survey : response.getSurveyList()) {
			JsonObject obj = new JsonObject();
			obj.addProperty("survey_id", survey.getSurveyId());
			obj.addProperty("survey_name", survey.getSurveyName());
			obj.addProperty("survey_desc", survey.getSurveyDesc());
			obj.addProperty("image_name", survey.getImageName());
			obj.addProperty("image_url", getUploadUrlPrefixResponse.getImageUrlPrefix() + survey.getImageName());
			obj.addProperty("start_time", survey.getStartTime());
			obj.addProperty("end_time", survey.getEndTime());
			obj.addProperty("survey_user_cnt", survey.getSurveyUserCnt());
			
			array.add(obj);
		}

		result.add("survey", array);
		result.addProperty("offset_index", HexUtil.bin2Hex(response.getOffsetIndex().toByteArray()));
		result.addProperty("has_more", response.getHasMore());
		
		httpResponse.setContentType("application/json;charset=UTF-8");
		JsonUtil.GSON.toJson(result, httpResponse.getWriter());
	}
	
	@Override
	public void doGet(HttpServletRequest httpRequest, HttpServletResponse httpResponse) throws IOException {
		this.doPost(httpRequest, httpResponse);
	}
}
