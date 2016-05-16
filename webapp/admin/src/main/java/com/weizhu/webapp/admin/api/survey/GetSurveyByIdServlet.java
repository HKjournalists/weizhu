package com.weizhu.webapp.admin.api.survey;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.common.util.concurrent.Futures;
import com.google.gson.JsonObject;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.weizhu.common.service.ServiceUtil;
import com.weizhu.common.utils.JsonUtil;
import com.weizhu.proto.AdminProtos;
import com.weizhu.proto.AdminProtos.AdminHead;
import com.weizhu.proto.AdminProtos.GetAdminByIdRequest;
import com.weizhu.proto.AdminProtos.GetAdminByIdResponse;
import com.weizhu.proto.AdminService;
import com.weizhu.proto.AllowProtos;
import com.weizhu.proto.AllowProtos.GetModelByIdRequest;
import com.weizhu.proto.AllowProtos.GetModelByIdResponse;
import com.weizhu.proto.AllowService;
import com.weizhu.proto.SurveyProtos;
import com.weizhu.proto.SurveyProtos.GetSurveyByIdRequest;
import com.weizhu.proto.SurveyProtos.GetSurveyByIdResponse;
import com.weizhu.proto.UploadProtos.GetUploadUrlPrefixResponse;
import com.weizhu.proto.SurveyService;
import com.weizhu.proto.UploadService;
import com.weizhu.web.ParamUtil;

@Singleton
public class GetSurveyByIdServlet extends HttpServlet {

	private static final long serialVersionUID = 1L;

	private final Provider<AdminHead> adminHeadProvider;
	private final SurveyService surveyService;
	private final AllowService allowService;
	private final AdminService adminService;
	private final UploadService uploadService;

	@Inject
	public GetSurveyByIdServlet(
			Provider<AdminHead> adminHeadProvider, 
			SurveyService surveyService, 
			AllowService allowService, 
			AdminService adminService,
			UploadService uploadService
			) {
		this.adminHeadProvider = adminHeadProvider;
		this.surveyService = surveyService;
		this.allowService = allowService;
		this.adminService = adminService;
		this.uploadService = uploadService;
	}
	
	@Override
	public void doPost(HttpServletRequest httpRequest, HttpServletResponse httpResponse) throws IOException {
		final int surveyId = ParamUtil.getInt(httpRequest, "survey_id", 0);
		final AdminHead adminHead = adminHeadProvider.get();
		
		GetSurveyByIdRequest request = GetSurveyByIdRequest.newBuilder()
				.setSurveyId(surveyId)
				.build();
		
		GetSurveyByIdResponse response = Futures.getUnchecked(surveyService.getSurveyById(adminHead, request));
		
		SurveyProtos.Survey survey = response.getSurvey();
		
		// 获取调研中访问模型的名称
		GetModelByIdResponse getModelByIdResponse = null;
		Map<Integer, AllowProtos.Model> modelMap = new HashMap<Integer, AllowProtos.Model>();
		if (survey.hasAllowModelId()) {
			GetModelByIdRequest getModelByIdRequest = GetModelByIdRequest.newBuilder()
					.addModelId(survey.getAllowModelId())
					.build();
			getModelByIdResponse = Futures.getUnchecked(allowService.getModelById(adminHead, getModelByIdRequest));
			for (AllowProtos.Model model : getModelByIdResponse.getModelList()) {
				modelMap.put(model.getModelId(), model);
			}
		}
		
		// 获取调研中用户创建用户更新的名称
		List<Long> adminIdList = new ArrayList<Long>();
		adminIdList.add(survey.getCreateAdminId());
		adminIdList.add(survey.getUpdateAdminId());
		
		GetAdminByIdRequest adminRequest = GetAdminByIdRequest.newBuilder().addAllAdminId(adminIdList).build();
		GetAdminByIdResponse adminResponse = Futures.getUnchecked(adminService.getAdminById(adminHead, adminRequest));
		Map<Long, AdminProtos.Admin> adminMap = new HashMap<Long, AdminProtos.Admin>();
		for (AdminProtos.Admin admin : adminResponse.getAdminList()) {
			adminMap.put(admin.getAdminId(), admin);
		}
		
		// 调研题目
		List<SurveyProtos.Question> questionList = response.getQuestionList();
		
		GetUploadUrlPrefixResponse getUploadUrlPrefixResponse = Futures.getUnchecked(this.uploadService.getUploadUrlPrefix(adminHead, ServiceUtil.EMPTY_REQUEST));
		
		JsonObject surveyJson = SurveyUtil.buildSurveyJsonObject(survey, modelMap, adminMap, getUploadUrlPrefixResponse.getImageUrlPrefix());
		
		JsonObject questionsJson = SurveyUtil.buildQuestionJsonObject(questionList, adminMap, getUploadUrlPrefixResponse.getImageUrlPrefix());
		
		JsonObject result = new JsonObject();
		result.add("survey", surveyJson);
		result.add("questions", questionsJson);
		
		httpResponse.setContentType("application/json;charset=UTF-8");
		JsonUtil.GSON.toJson(result, httpResponse.getWriter());
	}
	
	@Override
	public void doGet(HttpServletRequest httpRequest, HttpServletResponse httpResponse) throws IOException {
		this.doPost(httpRequest, httpResponse);
	}
}