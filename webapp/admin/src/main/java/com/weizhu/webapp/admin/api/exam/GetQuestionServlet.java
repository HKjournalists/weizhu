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
import com.weizhu.proto.AdminExamProtos.GetQuestionRequest;
import com.weizhu.proto.AdminExamProtos.GetQuestionResponse;
import com.weizhu.proto.AdminExamService;
import com.weizhu.proto.AdminProtos;
import com.weizhu.proto.AdminProtos.AdminHead;
import com.weizhu.proto.AdminProtos.GetAdminByIdRequest;
import com.weizhu.proto.AdminProtos.GetAdminByIdResponse;
import com.weizhu.proto.AdminService;
import com.weizhu.proto.ExamProtos;
import com.weizhu.web.ParamUtil;

@Singleton
public class GetQuestionServlet extends HttpServlet{

	private static final long serialVersionUID = 1L;

	private final Provider<AdminHead> adminHeadProvider;
	private final AdminExamService adminExamService;
	private final AdminService adminService;
	
	@Inject
	public GetQuestionServlet(Provider<AdminHead> adminHeadProvider, AdminExamService adminExamService, AdminService adminService) {
		this.adminHeadProvider = adminHeadProvider;
		this.adminExamService = adminExamService;
		this.adminService = adminService;
	}
	
	@Override
	public void doGet(HttpServletRequest httpRequest, HttpServletResponse httpResponse) throws IOException {
		this.doPost(httpRequest, httpResponse);
	}
	
	@Override
	public void doPost(HttpServletRequest httpRequest, HttpServletResponse httpResponse) throws IOException {
		final Integer start = ParamUtil.getInt(httpRequest, "start", 0);
		final Integer length = ParamUtil.getInt(httpRequest, "length", 10);
		final String questionName = ParamUtil.getString(httpRequest, "condition", "");
		
		final AdminHead head = adminHeadProvider.get();
		
		GetQuestionRequest request = GetQuestionRequest.newBuilder()
				.setStart(start)
				.setLength(length)
				.setQuestionName(questionName)
				.build();
		
		GetQuestionResponse response = Futures.getUnchecked(adminExamService.getQuestion(head, request));
		
		List<Long> adminIdList = new ArrayList<Long>();
		for (ExamProtos.Question question : response.getQuestionList()) {
			adminIdList.add(question.getCreateQuestionAdminId());
		}
		
		GetAdminByIdRequest adminRequest = GetAdminByIdRequest.newBuilder().addAllAdminId(adminIdList).build();
		GetAdminByIdResponse adminResponse = Futures.getUnchecked(adminService.getAdminById(head, adminRequest));
		Map<Long, String> adminMap = new HashMap<Long, String>();
		for (AdminProtos.Admin admin : adminResponse.getAdminList()) {
			adminMap.put(admin.getAdminId(), admin.getAdminName());
		}
		
		JsonArray jsonArry = new JsonArray();
		for (ExamProtos.Question question : response.getQuestionList()) {
			JsonObject questionObj = new JsonObject();
			JsonArray optionArray = new JsonArray();
			for (ExamProtos.Option option : question.getOptionList()) {
				JsonObject optionObj = new JsonObject();
				
				optionObj.addProperty("option_id", option.getOptionId());
				optionObj.addProperty("option_name", option.getOptionName());
				optionObj.addProperty("is_right", option.getIsRight());
				optionArray.add(optionObj);
			}
			questionObj.add("option", optionArray);
			questionObj.addProperty("question_id", question.getQuestionId());
			questionObj.addProperty("question_name", question.getQuestionName());
			questionObj.addProperty("create_admin_name", adminMap.get(question.getCreateQuestionAdminId()));
			questionObj.addProperty("create_time", question.getCreateQuestionTime());
			jsonArry.add(questionObj);
		}
		JsonObject result = new JsonObject();
		result.add("question", jsonArry);
		result.addProperty("total", response.getTotal());
		result.addProperty("filterd_size", response.getFilterdSize());
		
		httpResponse.setContentType("application/json;charset=UTF-8");
		JsonUtil.GSON.toJson(result, httpResponse.getWriter());
	}
}
