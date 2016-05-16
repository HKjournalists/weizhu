package com.weizhu.webapp.admin.api.allow;

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
import com.weizhu.proto.AdminProtos.Admin;
import com.weizhu.proto.AdminProtos.AdminHead;
import com.weizhu.proto.AdminProtos.GetAdminByIdRequest;
import com.weizhu.proto.AdminProtos.GetAdminByIdResponse;
import com.weizhu.proto.AdminService;
import com.weizhu.proto.AllowProtos;
import com.weizhu.proto.AllowProtos.GetModelListRequest;
import com.weizhu.proto.AllowProtos.GetModelListResponse;
import com.weizhu.proto.AllowService;
import com.weizhu.web.ParamUtil;

@Singleton
public class GetModelListServlet extends HttpServlet {

	private static final long serialVersionUID = 1L;

	private final Provider<AdminHead> adminHeadProvider;
	private final AllowService allowService;
	private final AdminService adminService;
	
	@Inject
	public GetModelListServlet(Provider<AdminHead> adminHeadProvider, AllowService allowService, AdminService adminService) {
		this.adminHeadProvider = adminHeadProvider;
		this.allowService = allowService;
		this.adminService = adminService;
	}
	
	@Override
	public void doPost(HttpServletRequest httpRequest, HttpServletResponse httpResponse) throws IOException {
		final Integer start = ParamUtil.getInt(httpRequest, "start", null);
		if (start == null) {
			JsonObject result = new JsonObject();
			result.addProperty("result", "FAIL_START_INVALID");
			result.addProperty("fail_text", "没有分页的开始标识！");

			httpResponse.setContentType("application/json;charset=UTF-8");
			JsonUtil.GSON.toJson(result, httpResponse.getWriter());
			return ;
		}
		
		final Integer length = ParamUtil.getInt(httpRequest, "length", null);
		if (length == null) {
			JsonObject result = new JsonObject();
			result.addProperty("result", "FAIL_LENTH_INVALID");
			result.addProperty("fail_text", "没有分页的结束标识！");

			httpResponse.setContentType("application/json;charset=UTF-8");
			JsonUtil.GSON.toJson(result, httpResponse.getWriter());
			return ;
		}
		
		final String keyword = ParamUtil.getString(httpRequest, "model_name", "");
		
		AdminHead head = this.adminHeadProvider.get();
		
		GetModelListRequest getModelListRequest = GetModelListRequest.newBuilder()
				.setStart(start)
				.setLength(length)
				.setKeyword(keyword)
				.build();
		GetModelListResponse getModelListResponse = Futures.getUnchecked(allowService.getModelList(head, getModelListRequest));
		List<AllowProtos.Model> modelList = getModelListResponse.getModelList();
		
		List<Long> createAdminIdList = new ArrayList<Long>();
		for (AllowProtos.Model model : modelList) {
			createAdminIdList.add(model.getCreateAdminId());
		}
		
		GetAdminByIdRequest getAdminByIdRequest = GetAdminByIdRequest.newBuilder()
				.addAllAdminId(createAdminIdList)
				.build();
		GetAdminByIdResponse getAdminByIdResponse = Futures.getUnchecked(adminService.getAdminById(head, getAdminByIdRequest));
		Map<Long, String> adminMap = new HashMap<Long, String>();
		for (Admin admin : getAdminByIdResponse.getAdminList()) {
			adminMap.put(admin.getAdminId(), admin.getAdminName());
		}
		
		JsonArray jsonArray = new JsonArray();
		for (AllowProtos.Model model : modelList) {
			JsonObject jsonObject = new JsonObject();
			jsonObject.addProperty("model_id", model.getModelId());
			jsonObject.addProperty("model_name", model.getModelName());
			jsonObject.addProperty("create_admin_name", adminMap.get(model.getCreateAdminId()) == null ? "" : adminMap.get(model.getCreateAdminId()));
			jsonObject.addProperty("create_time", model.getCreateTime());
			jsonObject.addProperty("default_action", model.getDefaultAction().name());
			
			jsonArray.add(jsonObject);
		}
		
		JsonObject result = new JsonObject();
		result.add("model", jsonArray);
		result.addProperty("total_size", getModelListResponse.getTotalSize());
		result.addProperty("filtered_size", getModelListResponse.getFilteredSize());

		httpResponse.setContentType("application/json;charset=UTF-8");
		JsonUtil.GSON.toJson(result, httpResponse.getWriter());
	}
	
	@Override
	public void doGet(HttpServletRequest httpRequest, HttpServletResponse httpResponse) throws IOException {
		this.doPost(httpRequest, httpResponse);
	}
}
