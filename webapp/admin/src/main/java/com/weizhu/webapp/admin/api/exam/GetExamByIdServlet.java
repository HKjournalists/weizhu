package com.weizhu.webapp.admin.api.exam;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
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
import com.weizhu.proto.AdminExamProtos.GetExamByIdRequest;
import com.weizhu.proto.AdminExamProtos.GetExamByIdResponse;
import com.weizhu.proto.AdminExamService;
import com.weizhu.proto.AdminProtos;
import com.weizhu.proto.AllowProtos;
import com.weizhu.proto.AdminProtos.AdminHead;
import com.weizhu.proto.AdminProtos.GetAdminByIdRequest;
import com.weizhu.proto.AdminProtos.GetAdminByIdResponse;
import com.weizhu.proto.AllowProtos.GetModelByIdRequest;
import com.weizhu.proto.AdminService;
import com.weizhu.proto.AllowProtos.GetModelByIdResponse;
import com.weizhu.proto.AllowService;
import com.weizhu.proto.ExamProtos.Exam;
import com.weizhu.proto.UploadProtos.GetUploadUrlPrefixResponse;
import com.weizhu.proto.UploadService;
import com.weizhu.web.ParamUtil;

@Singleton
public class GetExamByIdServlet extends HttpServlet {

	private static final long serialVersionUID = 1L;
    
	private final Provider<AdminHead> adminHeadProvider;
	private final AdminExamService adminExamService;
	private final AdminService adminService;
	private final AllowService allowService;
	private final UploadService uploadService;
	
	@Inject
	public GetExamByIdServlet(Provider<AdminHead> adminHeadProvider, AdminExamService adminExamService
			, AdminService adminService, AllowService allowService, UploadService uploadService) {
		this.adminHeadProvider = adminHeadProvider;
		this.adminExamService = adminExamService;
		this.adminService = adminService;
		this.allowService = allowService;
		this.uploadService = uploadService;
	}
	
	@Override
	public void doPost(HttpServletRequest httpRequest, HttpServletResponse httpResponse) throws IOException {
		final Integer examId = ParamUtil.getInt(httpRequest, "exam_id", null);
		
		if (examId == null) {
			JsonObject result = new JsonObject();
			result.addProperty("result", "FAIL_EXAM_INVALID");
			result.addProperty("fail_text", "请输一个入合法的考试！");
			
			httpResponse.setContentType("application/json;charset=UTF-8");
			JsonUtil.GSON.toJson(result, httpResponse.getWriter());
		}
		
		final AdminHead head = adminHeadProvider.get();
		
		GetExamByIdRequest request = GetExamByIdRequest.newBuilder()
				.addAllExamId(Collections.singletonList(examId))
				.build();
		GetExamByIdResponse response = Futures.getUnchecked(adminExamService.getExamById(head, request));
		
		List<Long> adminIdList = new ArrayList<Long>();
		for (Exam exam : response.getExamList()) {
			adminIdList.add(exam.getCreateExamAdminId());
		}
		
		GetAdminByIdRequest adminRequest = GetAdminByIdRequest.newBuilder().addAllAdminId(adminIdList).build();
		GetAdminByIdResponse adminResponse = Futures.getUnchecked(adminService.getAdminById(head, adminRequest));
		Map<Long, String> adminMap = new HashMap<Long, String>();
		for (AdminProtos.Admin admin : adminResponse.getAdminList()) {
			adminMap.put(admin.getAdminId(), admin.getAdminName());
		}
		
		int now = (int) (System.currentTimeMillis() / 1000L);
		
		List<Integer> modelIdList = new ArrayList<Integer>();
		for (Exam exam : response.getExamList()) {
			modelIdList.add(exam.getAllowModelId());
		}
		
		GetModelByIdRequest getModelByIdRequest = GetModelByIdRequest.newBuilder()
				.addAllModelId(modelIdList)
				.build();
		GetModelByIdResponse getModelByIdResponse = Futures.getUnchecked(allowService.getModelById(head, getModelByIdRequest));
		Map<Integer, AllowProtos.Model> modelMap = new HashMap<Integer, AllowProtos.Model>();
		for (AllowProtos.Model model : getModelByIdResponse.getModelList()) {
			modelMap.put(model.getModelId(), model);
		}
		
		GetUploadUrlPrefixResponse getUploadUrlPrefixResponse = Futures.getUnchecked(this.uploadService.getUploadUrlPrefix(head, ServiceUtil.EMPTY_REQUEST));
		
		JsonArray jsonArry = new JsonArray();
		for (Exam exam : response.getExamList()) {
			JsonObject obj = new JsonObject();
			obj.addProperty("exam_id", exam.getExamId());
			obj.addProperty("exam_name", exam.getExamName());
			obj.addProperty("image_name", exam.getImageName());
			obj.addProperty("image_url", getUploadUrlPrefixResponse.getImageUrlPrefix() + exam.getImageName());
			obj.addProperty("show_result", exam.getShowResult().name());
			obj.addProperty("create_exam_name", adminMap.get(exam.getCreateExamAdminId()));
			obj.addProperty("create_time", exam.getCreateExamTime());
			obj.addProperty("start_time", exam.getStartTime());
			obj.addProperty("end_time", exam.getEndTime());
			obj.addProperty("type", exam.getType().name());
			
			if (now < exam.getStartTime()) {
				obj.addProperty("state", "0");
			} else if (now >= exam.getStartTime() && now < exam.getEndTime()) {
				obj.addProperty("state", "1");
			} else {
				obj.addProperty("state", "2");
			}
			
			obj.addProperty("pass_mark", exam.getPassMark());
			obj.addProperty("allow_model_id", exam.getAllowModelId());
			AllowProtos.Model model = modelMap.get(exam.getAllowModelId());
			obj.addProperty("allow_model_name", model == null ? "" : model.getModelName());
			
			jsonArry.add(obj);
		}
		JsonObject result = new JsonObject();
		result.add("exam", jsonArry);
		
		httpResponse.setContentType("application/json;charset=UTF-8");
		JsonUtil.GSON.toJson(result, httpResponse.getWriter());
	}
	
	@Override
	public void doGet(HttpServletRequest httpRequest, HttpServletResponse httpResponse) throws IOException {
		this.doPost(httpRequest, httpResponse);
	}
}
