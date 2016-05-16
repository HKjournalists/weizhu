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
import com.google.gson.JsonArray;
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
import com.weizhu.proto.SurveyProtos.GetSurveyListRequest;
import com.weizhu.proto.SurveyProtos.GetSurveyListResponse;
import com.weizhu.proto.SurveyService;
import com.weizhu.proto.UploadProtos.GetUploadUrlPrefixResponse;
import com.weizhu.proto.UploadService;
import com.weizhu.web.ParamUtil;

@Singleton
public class GetSurveyListServlet extends HttpServlet {

	private static final long serialVersionUID = 1L;

	private final Provider<AdminHead> adminHeadProvider;
	private final SurveyService surveyService;
	private final AllowService allowService;
	private final AdminService adminService;
	private final UploadService uploadService;

	@Inject
	public GetSurveyListServlet(
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
		final int start = ParamUtil.getInt(httpRequest, "start", 0);
		final int length = ParamUtil.getInt(httpRequest, "length", 20);
		
		final String surveyName = ParamUtil.getString(httpRequest, "survey_name", "");
		
		GetSurveyListRequest.Builder getSurveyListRequestBuilder = GetSurveyListRequest.newBuilder()
				.setStart(start)
				.setLength(length);
		
		if (!surveyName.isEmpty()) {
			getSurveyListRequestBuilder.setSurveyName(surveyName);
		}
		
		AdminHead head = adminHeadProvider.get();
		GetSurveyListResponse getSurveyListResponse = Futures.getUnchecked(surveyService.getSurveyList(head, getSurveyListRequestBuilder.build()));
		
		// 获取调研中访问模型的名称
		Map<Integer, AllowProtos.Model> modelMap = new HashMap<Integer, AllowProtos.Model>();
		List<Integer> allowModelIdList = new ArrayList<Integer>();
		List<Long> adminIdList = new ArrayList<Long>();
		for (SurveyProtos.Survey survey : getSurveyListResponse.getSurveyList()) {
			if (survey.hasAllowModelId()) {
				allowModelIdList.add(survey.getAllowModelId());
			}
			if (survey.hasUpdateAdminId()) {
				adminIdList.add(survey.getUpdateAdminId());
			}
			if (survey.hasCreateAdminId()) {
				adminIdList.add(survey.getCreateAdminId());
			}
		}
		
		GetModelByIdRequest getModelByIdRequest = GetModelByIdRequest.newBuilder()
				.addAllModelId(allowModelIdList)
				.build();
		GetModelByIdResponse getModelByIdResponse = Futures.getUnchecked(allowService.getModelById(head, getModelByIdRequest));
		for (AllowProtos.Model model : getModelByIdResponse.getModelList()) {
			modelMap.put(model.getModelId(), model);
		}
		
		// 获取调研中创建者的信息
		GetAdminByIdRequest adminRequest = GetAdminByIdRequest.newBuilder().addAllAdminId(adminIdList).build();
		GetAdminByIdResponse adminResponse = Futures.getUnchecked(adminService.getAdminById(head, adminRequest));
		Map<Long, AdminProtos.Admin> adminMap = new HashMap<Long, AdminProtos.Admin>();
		for (AdminProtos.Admin admin : adminResponse.getAdminList()) {
			adminMap.put(admin.getAdminId(), admin);
		}
		
		GetUploadUrlPrefixResponse getUploadUrlPrefixResponse = Futures.getUnchecked(this.uploadService.getUploadUrlPrefix(head, ServiceUtil.EMPTY_REQUEST));
		
		JsonArray array = new JsonArray();
		for (SurveyProtos.Survey survey : getSurveyListResponse.getSurveyList()) {
			array.add(SurveyUtil.buildSurveyJsonObject(survey, modelMap, adminMap, getUploadUrlPrefixResponse.getImageUrlPrefix()));
		}
		
		JsonObject result = new JsonObject();
		result.add("survey", array);
		result.addProperty("total", getSurveyListResponse.getTotal());
		result.addProperty("filtered_size", getSurveyListResponse.getFilteredSize());
		
		httpResponse.setContentType("application/json;charset=UTF-8");
		JsonUtil.GSON.toJson(result, httpResponse.getWriter());
	}
	
	@Override
	public void doGet(HttpServletRequest httpRequest, HttpServletResponse httpResponse) throws IOException {
		this.doPost(httpRequest, httpResponse);
	}
}
