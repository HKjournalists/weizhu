package com.weizhu.webapp.admin.api.exam;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.common.collect.Lists;
import com.google.common.util.concurrent.Futures;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.weizhu.common.service.ServiceUtil;
import com.weizhu.common.utils.JsonUtil;
import com.weizhu.proto.AdminExamProtos.GetExamRequest;
import com.weizhu.proto.AdminExamProtos.GetExamResponse;
import com.weizhu.proto.AdminExamService;
import com.weizhu.proto.AdminProtos;
import com.weizhu.proto.AdminProtos.AdminHead;
import com.weizhu.proto.AdminProtos.GetAdminByIdRequest;
import com.weizhu.proto.AdminProtos.GetAdminByIdResponse;
import com.weizhu.proto.AdminService;
import com.weizhu.proto.AllowProtos;
import com.weizhu.proto.AllowProtos.GetModelByIdRequest;
import com.weizhu.proto.AllowProtos.GetModelByIdResponse;
import com.weizhu.proto.AllowService;
import com.weizhu.proto.ExamProtos.Exam;
import com.weizhu.proto.UploadProtos.GetUploadUrlPrefixResponse;
import com.weizhu.proto.UploadService;
import com.weizhu.web.ParamUtil;

@Singleton
public class GetExamServlet extends HttpServlet {

	private static final long serialVersionUID = 1L;

	private final Provider<AdminHead> adminHeadProvider;
	private final AdminExamService adminExamService;
	private final AdminService adminService;
	private final AllowService allowService;
	private final UploadService uploadService;
	
	@Inject
	public GetExamServlet(Provider<AdminHead> adminHeadProvider, AdminExamService adminExamService, 
			AdminService adminService,
			AllowService allowService,
			UploadService uploadService) {
		this.adminHeadProvider = adminHeadProvider;
		this.adminExamService = adminExamService;
		this.adminService = adminService;
		this.allowService = allowService;
		this.uploadService = uploadService;
	}
	
	@Override
	public void doGet(HttpServletRequest httpRequest, HttpServletResponse httpResponse) throws IOException {
		this.doPost(httpRequest, httpResponse);
	}
	
	@Override
	public void doPost(HttpServletRequest httpRequest, HttpServletResponse httpResponse) throws IOException {
		final int start = ParamUtil.getInt(httpRequest, "start", 0);
		final int length = ParamUtil.getInt(httpRequest, "length", 0);
		final String examName = ParamUtil.getString(httpRequest, "condition", "");// 根据examName过滤
		final Integer stat = ParamUtil.getInt(httpRequest, "state", null);// 根据考试状态过滤
		
		int now = (int) (System.currentTimeMillis() / 1000L);
		
		final AdminHead head = adminHeadProvider.get();
		
		GetExamRequest.Builder requestBuilder = GetExamRequest.newBuilder()
				.setStart(start)
				.setLength(length)
				.setExamName(examName);
		if (stat != null) {
			requestBuilder.setState(stat);
		}
		
		GetExamResponse response = Futures.getUnchecked(adminExamService.getExam(head, requestBuilder.build()));
		
		List<Integer> examIdList = Lists.newArrayList();
		List<Long> adminIdList = Lists.newArrayList();
		List<Integer> modelIdList = Lists.newArrayList();
		for (Exam exam : response.getExamList()) {
			examIdList.add(exam.getExamId());
			adminIdList.add(exam.getCreateExamAdminId());
			modelIdList.add(exam.getAllowModelId());
		}
		
		GetAdminByIdRequest adminRequest = GetAdminByIdRequest.newBuilder().addAllAdminId(adminIdList).build();
		GetAdminByIdResponse adminResponse = Futures.getUnchecked(adminService.getAdminById(head, adminRequest));
		Map<Long, String> adminMap = new HashMap<Long, String>();
		for (AdminProtos.Admin admin : adminResponse.getAdminList()) {
			adminMap.put(admin.getAdminId(), admin.getAdminName());
		}
		
		GetModelByIdResponse getModelByIdResponse = Futures.getUnchecked(allowService.getModelById(head, GetModelByIdRequest.newBuilder()
				.addAllModelId(modelIdList)
				.build()));
		Map<Integer, String> modelMap = new HashMap<Integer, String>();
		for (AllowProtos.Model model : getModelByIdResponse.getModelList()) {
			modelMap.put(model.getModelId(), model.getModelName());
		}
		
		GetUploadUrlPrefixResponse getUploadUrlPrefixResponse = Futures.getUnchecked(this.uploadService.getUploadUrlPrefix(head, ServiceUtil.EMPTY_REQUEST));
		
		JsonArray jsonArry = new JsonArray();
		for (Exam exam : response.getExamList()) {
			JsonObject obj = new JsonObject();
			obj.addProperty("exam_id", exam.getExamId());
			obj.addProperty("exam_name", exam.getExamName());
			if (exam.hasImageName()) {
				obj.addProperty("image_name", exam.getImageName());
				obj.addProperty("image_url", getUploadUrlPrefixResponse.getImageUrlPrefix() + exam.getImageName());
			}
			obj.addProperty("create_exam_name", adminMap.get(exam.getCreateExamAdminId()));
			obj.addProperty("create_time", exam.getCreateExamTime());
			obj.addProperty("start_time", exam.getStartTime());
			obj.addProperty("end_time", exam.getEndTime());
			obj.addProperty("type", exam.getType().name());
			
			if (exam.hasAllowModelId()) {
				int modelId = exam.getAllowModelId();
				obj.addProperty("allow_model_id", modelId);
				obj.addProperty("allow_model_name", modelMap.get(modelId) == null ? "" : modelMap.get(modelId));
			} else {
				obj.addProperty("allow_model_id", "");
				obj.addProperty("allow_model_name", "");
			}
			
			if (now < exam.getStartTime()) {
				obj.addProperty("state", "0");
			} else if (now >= exam.getStartTime() && now < exam.getEndTime()) {
				obj.addProperty("state", "1");
			} else {
				obj.addProperty("state", "2");
			}
			obj.addProperty("is_load_all_user", exam.hasIsLoadAllUser() ? exam.getIsLoadAllUser() : false);
			jsonArry.add(obj);
		}
		JsonObject result = new JsonObject();
		result.add("exam", jsonArry);
		result.addProperty("total", response.getTotal());
		result.addProperty("filterd_size", response.getFilteredSize());
		
		httpResponse.setContentType("application/json;charset=UTF-8");
		JsonUtil.GSON.toJson(result, httpResponse.getWriter());
	}
}
