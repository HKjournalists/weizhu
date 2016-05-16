package com.weizhu.webapp.admin.api.exam;

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
import com.weizhu.common.utils.JsonUtil;
import com.weizhu.proto.AdminExamProtos.GetQuestionCategoryResponse;
import com.weizhu.proto.AdminExamProtos.QuestionCategory;
import com.weizhu.proto.AdminExamService;
import com.weizhu.proto.AdminProtos;
import com.weizhu.proto.AdminProtos.AdminHead;
import com.weizhu.proto.AdminProtos.GetAdminByIdRequest;
import com.weizhu.proto.AdminProtos.GetAdminByIdResponse;
import com.weizhu.proto.AdminService;
import com.weizhu.proto.WeizhuProtos.EmptyRequest;

@Singleton
public class GetQuestionCategoryServlet extends HttpServlet {

	private static final long serialVersionUID = 1L;

	private final Provider<AdminHead> adminHeadProvider;
	private final AdminExamService adminExamService;
	private final AdminService adminService;

	@Inject
	public GetQuestionCategoryServlet(Provider<AdminHead> adminHeadProvider, AdminExamService adminExamService, AdminService adminService) {
		this.adminHeadProvider = adminHeadProvider;
		this.adminExamService = adminExamService;
		this.adminService = adminService;
	}
	
	@Override
	public void doPost(HttpServletRequest httpRequest, HttpServletResponse httpResponse) throws IOException {
		final AdminHead adminHead = adminHeadProvider.get();
		
		GetQuestionCategoryResponse response = Futures.getUnchecked(adminExamService.getQuestionCategory(adminHead, EmptyRequest.getDefaultInstance()));
		
		List<Long> adminIdList = new ArrayList<Long>();
		for (QuestionCategory questionCategory : response.getQuestionCategoryList()) {
			getCategoryAdminId(adminIdList, questionCategory);
		}
		
		GetAdminByIdRequest adminRequest = GetAdminByIdRequest.newBuilder().addAllAdminId(adminIdList).build();
		GetAdminByIdResponse adminResponse = Futures.getUnchecked(adminService.getAdminById(adminHead, adminRequest));
		Map<Long, String> adminMap = new HashMap<Long, String>();
		for (AdminProtos.Admin admin : adminResponse.getAdminList()) {
			adminMap.put(admin.getAdminId(), admin.getAdminName());
		}
		
		// 递归为题库添加信息
		JsonArray jsonArray = new JsonArray();
		for (QuestionCategory questionCategory : response.getQuestionCategoryList()) {
			JsonObject obj = new JsonObject();
			obj.addProperty("category_id", questionCategory.getCategoryId());
			obj.addProperty("category_name", questionCategory.getCategoryName());
			obj.addProperty("create_admin_name", adminMap.get(questionCategory.getCreateAdminId()) == null ? "" : adminMap.get(questionCategory.getCreateAdminId()));
			obj.addProperty("create_time", questionCategory.getCreateTime());
			
			JsonObject childObj = new JsonObject();
			
			int count = addCategoryInfo(childObj, questionCategory, adminMap);
			obj.addProperty("question_count", count);
			
			obj.add("children", childObj);
			jsonArray.add(obj);
		}
		
		JsonObject result = new JsonObject();
		result.add("question_category", jsonArray);
		
		httpResponse.setContentType("application/json;charset=UTF-8");
		JsonUtil.GSON.toJson(result, httpResponse.getWriter());
	}
	
	@Override
	public void doGet(HttpServletRequest httpRequest, HttpServletResponse httpResponse) throws IOException {
		this.doPost(httpRequest, httpResponse);
	}
	
	private void getCategoryAdminId(List<Long> categoryAdminIdList, QuestionCategory questionCategory) {
		categoryAdminIdList.add(questionCategory.getCreateAdminId());
		for (QuestionCategory tmpQuestionCategory : questionCategory.getQuestionCategoryList()) {
			getCategoryAdminId(categoryAdminIdList, tmpQuestionCategory);
		}
	}
	
	private int addCategoryInfo(JsonObject obj, QuestionCategory questionCategory, Map<Long, String> adminMap) {
		JsonArray array = new JsonArray();
		int total = 0;
		for (QuestionCategory tmpQuestionCategory : questionCategory.getQuestionCategoryList()) {
			JsonObject children = new JsonObject();
			children.addProperty("category_id", tmpQuestionCategory.getCategoryId());
			children.addProperty("category_name", tmpQuestionCategory.getCategoryName());
			children.addProperty("create_admin_name", adminMap.get(tmpQuestionCategory.getCreateAdminId()) == null ? "" : adminMap.get(tmpQuestionCategory.getCreateAdminId()));
			children.addProperty("create_time", tmpQuestionCategory.getCreateTime());
			
			JsonObject childObj = new JsonObject();
			int count = addCategoryInfo(childObj, tmpQuestionCategory, adminMap);
			
			total += count;
			children.addProperty("question_count", count);
			
			children.add("children", childObj);
			array.add(children);
		}
		obj.add("question_category", array);
		return total + questionCategory.getQuestionIdCount();
	}
	
	
}
