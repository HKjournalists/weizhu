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
import com.google.protobuf.ExtensionRegistry;
import com.weizhu.proto.AdminProtos.AdminHead;
import com.weizhu.common.utils.JsonUtil;
import com.weizhu.proto.SurveyProtos;
import com.weizhu.proto.SurveyProtos.CreateSurveyRequest;
import com.weizhu.proto.SurveyProtos.CreateSurveyResponse;
import com.weizhu.proto.SurveyService;
import com.weizhu.web.ParamUtil;

@Singleton
public class CreateSurveyServlet extends HttpServlet {

	private static final long serialVersionUID = 1L;
	
	private final Provider<AdminHead> adminHeadProvider;
	private final SurveyService surveyService;
	
	@Inject
	public CreateSurveyServlet(Provider<AdminHead> adminHeadProvider, SurveyService surveyService) {
		this.adminHeadProvider = adminHeadProvider;
		this.surveyService = surveyService;
	}
	
	@Override
	public void doPost(HttpServletRequest httpRequest, HttpServletResponse httpResponse) throws IOException {
		final String surveyName = ParamUtil.getString(httpRequest, "survey_name", "");
		final String surveyDesc = ParamUtil.getString(httpRequest, "survey_desc", "");
		final String imageName = ParamUtil.getString(httpRequest, "image_name", null);
		final int startTime = ParamUtil.getInt(httpRequest, "start_time", 0);
		final int endTime = ParamUtil.getInt(httpRequest, "end_time", 0);
		final String showResultType = ParamUtil.getString(httpRequest, "show_result_type", "AFTER_SUBMIT_COUNT");
		final Integer allowModelId = ParamUtil.getInt(httpRequest, "allow_model_id", null);
		
		CreateSurveyRequest.Builder requestBuilder = CreateSurveyRequest.newBuilder()
				.setSurveyName(surveyName)
				.setSurveyDesc(surveyDesc)
				.setStartTime(startTime)
				.setEndTime(endTime);
		
		if (imageName != null) {
			requestBuilder.setImageName(imageName);
		}
		
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
		
		requestBuilder.setShowResultType(tmpShowResultType);
		
		if (allowModelId != null) {
			requestBuilder.setAllowModelId(allowModelId);
		}
		
		/**
		 * 创建调研所需要的题目json格式范例：
		 * question_list ： {\"question\": [{\"question_id\": 0,\"question_name\": \"投票\",\"image_name\": \"a.png\",\"is_optional\": false,\"vote\": {\"option\": [{\"option_id\": 0,\"option_name\": \"投票1\",\"image_name\": \"a.png\"},{\"option_id\": 0,\"option_name\": \"投票2\",\"image_name\": \"b.png\"}],\"check_num\": 2},\"create_time\": 111111,\"create_admin_id\": 111111},{\"question_id\": 0,\"question_name\": \"下拉框\",\"image_name\": \"a.png\",\"is_optional\": false,\"input_select\": {\"option\": [{\"option_id\": 0,\"option_name\": \"下拉框1\"},{\"option_id\": 0,\"option_name\": \"下拉框2\"}]},\"create_time\": 111111,\"create_admin_id\": 111111},{\"question_id\": 0,\"question_name\": \"输入框\",\"image_name\": \"a.png\",\"is_optional\": false,\"input_text\": {\"input_prompt\": \"此处答题\"},\"create_time\": 111111,\"create_admin_id\": 111111}]}
		 */
		final String questionParam = ParamUtil.getString(httpRequest, "question_list", "");
		try {
			JsonUtil.PROTOBUF_JSON_FORMAT.merge(questionParam, ExtensionRegistry.getEmptyRegistry(), requestBuilder);
		} catch (Exception ex) {
			JsonObject result = new JsonObject();
			result.addProperty("result", "FAIL_QUESTION_INVALID");
			result.addProperty("fail_text", "题目格式不正确！");

			httpResponse.setContentType("application/json;charset=UTF-8");
			JsonUtil.GSON.toJson(result, httpResponse.getWriter());
			return ;
		}
		
		final AdminHead adminHead = adminHeadProvider.get();
		
		CreateSurveyResponse response = Futures.getUnchecked(surveyService.createSurvey(adminHead, requestBuilder.build()));
		
		httpResponse.setContentType("application/json;charset=UTF-8");
		JsonUtil.PROTOBUF_JSON_FORMAT.print(response, httpResponse.getWriter());
	}
	
	@Override
	public void doGet(HttpServletRequest httpRequest, HttpServletResponse httpResponse) throws IOException {
		this.doPost(httpRequest, httpResponse);
	}

}
