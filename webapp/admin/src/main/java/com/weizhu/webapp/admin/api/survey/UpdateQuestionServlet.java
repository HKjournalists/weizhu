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
import com.weizhu.proto.SurveyProtos.UpdateQuestionRequest;
import com.weizhu.proto.SurveyProtos.UpdateQuestionResponse;
import com.weizhu.common.utils.JsonUtil;
import com.weizhu.proto.SurveyService;
import com.weizhu.web.ParamUtil;

@Singleton
public class UpdateQuestionServlet extends HttpServlet {

	private static final long serialVersionUID = 1L;

	private final Provider<AdminHead> adminHeadProvider;
	private final SurveyService surveyService;
	
	@Inject
	public UpdateQuestionServlet(Provider<AdminHead> adminHeadProvider, SurveyService surveyService) {
		this.adminHeadProvider = adminHeadProvider;
		this.surveyService = surveyService;
	}
	
	@Override
	public void doPost(HttpServletRequest httpRequest, HttpServletResponse httpResponse) throws IOException {
		final int questionId = ParamUtil.getInt(httpRequest, "question_id", 0);
		final String questionName = ParamUtil.getString(httpRequest, "question_name", "");
		final String imageName = ParamUtil.getString(httpRequest, "image_name", null);
		final boolean isOptional = ParamUtil.getBoolean(httpRequest, "is_optional", false);
		
		UpdateQuestionRequest.Builder requestBuilder = UpdateQuestionRequest.newBuilder()
				.setQuestionId(questionId)
				.setQuestionName(questionName)
				.setIsOptional(isOptional);
		
		if (imageName != null) {
			requestBuilder.setImageName(imageName);
		}
		
		/**
		 * 创建题目需要的json格式范例：（投票、下拉框，输入框的不需要）
		 * type_param : "{\"vote\": {\"option\": [{\"option_id\": 0,\"option_name\": \"aaa\",\"image_name\": \"a.png\",\"option_cnt\": 0},{\"option_id\": 0,\"option_name\": \"bbb\",\"image_name\": \"b.png\",\"option_cnt\": 0}],\"check_num\": 3}}"
		 * type_param : "{\"input_select\": {\"option\": [{\"option_id\": 0,\"option_name\": \"下拉框1\"},{\"option_id\": 0,\"option_name\": \"下拉框2\"}]}}"
		 * 
		 */
		final String typeParam = ParamUtil.getString(httpRequest, "type_param", "");
		try {
			JsonUtil.PROTOBUF_JSON_FORMAT.merge(typeParam, ExtensionRegistry.getEmptyRegistry(), requestBuilder);
		} catch (Exception ex) {
			JsonObject result = new JsonObject();
			result.addProperty("result", "FAIL_UNKNOWN");
			result.addProperty("fail_text", "传入的题目有误！");

			httpResponse.setContentType("application/json;charset=UTF-8");
			JsonUtil.GSON.toJson(result, httpResponse.getWriter());
			return ;
		}
		
		final AdminHead adminHead = adminHeadProvider.get();
		
		UpdateQuestionResponse response = Futures.getUnchecked(surveyService.updateQuestion(adminHead, requestBuilder.build()));
		
		httpResponse.setContentType("application/json;charset=UTF-8");
		JsonUtil.PROTOBUF_JSON_FORMAT.print(response, httpResponse.getWriter());
	}
	
	@Override
	public void doGet(HttpServletRequest httpRequest, HttpServletResponse httpResponse) throws IOException {
		this.doPost(httpRequest, httpResponse);
	}
}
