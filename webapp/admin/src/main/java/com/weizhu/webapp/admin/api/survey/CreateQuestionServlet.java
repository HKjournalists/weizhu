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
import com.weizhu.proto.SurveyProtos.CreateQuestionRequest;
import com.weizhu.proto.SurveyProtos.CreateQuestionResponse;
import com.weizhu.common.utils.JsonUtil;
import com.weizhu.proto.SurveyService;
import com.weizhu.web.ParamUtil;

@Singleton
public class CreateQuestionServlet extends HttpServlet {

	private static final long serialVersionUID = 1L;

	private final Provider<AdminHead> adminHeadProvider;
	private final SurveyService surveyService;
	
	@Inject
	public CreateQuestionServlet(Provider<AdminHead> adminHeadProvider, SurveyService surveyService) {
		this.adminHeadProvider = adminHeadProvider;
		this.surveyService = surveyService;
	}
	
	@Override
	public void doPost(HttpServletRequest httpRequest, HttpServletResponse httpResponse) throws IOException {
		final int surveyId = ParamUtil.getInt(httpRequest, "survey_id", 0);
		final String questionName = ParamUtil.getString(httpRequest, "question_name", "");
		final String imageName = ParamUtil.getString(httpRequest, "image_name", "");
		final boolean isOptional = ParamUtil.getBoolean(httpRequest, "is_optional", false);
		
		CreateQuestionRequest.Builder requestBuilder = CreateQuestionRequest.newBuilder()
				.setSurveyId(surveyId)
				.setQuestionName(questionName)
				.setImageName(imageName)
				.setIsOptional(isOptional);
		
		/**
		 * 创建题目需要的json格式范例：（投票、下拉框、输入框）
		 * type_param : "{\"vote\": {\"option\": [{\"option_id\": 0,\"option_name\": \"aaa\",\"image_name\": \"a.png\",\"option_cnt\": 0},{\"option_id\": 0,\"option_name\": \"bbb\",\"image_name\": \"b.png\",\"option_cnt\": 0}],\"check_num\": 3}}"
		 * type_param : "{\"input_select\": {\"option\": [{\"option_id\": 0,\"option_name\": \"下拉框1\"},{\"option_id\": 0,\"option_name\": \"下拉框2\"}]}}"
		 * type_param : "{\"input_text\": {\"input_prompt\": \"在这里填入\"}}"
		 */
		final String typeParam = ParamUtil.getString(httpRequest, "type_param", "");
		
		try {
			JsonUtil.PROTOBUF_JSON_FORMAT.merge(typeParam, ExtensionRegistry.getEmptyRegistry(), requestBuilder);
		} catch (Exception ex) {
			JsonObject result = new JsonObject();
			result.addProperty("result", "FAIL_QUESTION_INVALID");
			result.addProperty("fail_text", "传入的题目有误！");

			httpResponse.setContentType("application/json;charset=UTF-8");
			JsonUtil.GSON.toJson(result, httpResponse.getWriter());
			return ;
		}
		
		final AdminHead adminHead = adminHeadProvider.get();
		CreateQuestionResponse response = Futures.getUnchecked(surveyService.createQuestion(adminHead, requestBuilder.build()));
		
		httpResponse.setContentType("application/json;charset=UTF-8");
		JsonUtil.PROTOBUF_JSON_FORMAT.print(response, httpResponse.getWriter());
	}
	
	@Override
	public void doGet(HttpServletRequest httpRequest, HttpServletResponse httpResponse) throws IOException {
		this.doPost(httpRequest, httpResponse);
	}
	
}
