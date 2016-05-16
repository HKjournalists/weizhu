package com.weizhu.webapp.admin.api.credits;

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
import com.weizhu.proto.AdminCreditsProtos.GetCreditsLogRequest;
import com.weizhu.proto.AdminCreditsProtos.GetCreditsLogResponse;
import com.weizhu.proto.AdminCreditsService;
import com.weizhu.proto.AdminProtos;
import com.weizhu.proto.AdminProtos.AdminHead;
import com.weizhu.proto.AdminProtos.GetAdminByIdRequest;
import com.weizhu.proto.AdminProtos.GetAdminByIdResponse;
import com.weizhu.proto.AdminService;
import com.weizhu.web.ParamUtil;

@Singleton
public class GetCreditsLogServlet extends HttpServlet {

	private static final long serialVersionUID = 1L;

	private final Provider<AdminHead> adminHeadProvider;
	private final AdminCreditsService adminCreditsService;
	private final AdminService adminService;
	
	@Inject
	public GetCreditsLogServlet(Provider<AdminHead> adminHeadProvider, AdminCreditsService adminCreditsService, AdminService adminService) {
		this.adminHeadProvider = adminHeadProvider;
		this.adminCreditsService = adminCreditsService;
		this.adminService = adminService;
	}
	
	@Override
	public void doPost(HttpServletRequest httpRequest, HttpServletResponse httpResponse) throws IOException {
		final int start = ParamUtil.getInt(httpRequest, "start", 0);
		final int length = ParamUtil.getInt(httpRequest, "length", 0);
		
		GetCreditsLogRequest request = GetCreditsLogRequest.newBuilder()
				.setStart(start)
				.setLength(length)
				.build();
		
		final AdminHead head = adminHeadProvider.get();
		
		GetCreditsLogResponse response = Futures.getUnchecked(adminCreditsService.getCreditsLog(head, request));
		
		List<Long> adminIdList = new ArrayList<Long>();
		for (GetCreditsLogResponse.CreditsLog creditsLog : response.getCreditsLogList()) {
			adminIdList.add(creditsLog.getCreateAdmin());
		}
		
		GetAdminByIdResponse getAdminByIdResponse = Futures.getUnchecked(adminService.getAdminById(head, GetAdminByIdRequest.newBuilder()
				.addAllAdminId(adminIdList)
				.build()));
		Map<Long, AdminProtos.Admin> adminMap = new HashMap<Long, AdminProtos.Admin>();
		for (AdminProtos.Admin admin : getAdminByIdResponse.getAdminList()) {
			adminMap.put(admin.getAdminId(), admin);
		}
		
		JsonArray array = new JsonArray();
		for (GetCreditsLogResponse.CreditsLog creditsLog : response.getCreditsLogList()) {
			JsonObject obj = new JsonObject();
			
			obj.addProperty("credits_delta", creditsLog.getCreditsDelta());
			obj.addProperty("desc", creditsLog.getDesc());
			obj.addProperty("create_time", creditsLog.getCreateTime());
			AdminProtos.Admin admin = adminMap.get(creditsLog.getCreateAdmin());
			obj.addProperty("create_admin", admin == null ? "未知的管理员" : admin.getAdminName());
			
			array.add(obj);
		}
		
		JsonObject result = new JsonObject();
		result.add("credits_log", array);
		result.addProperty("total", response.getTotal());
		result.addProperty("filtered_size", response.getFilteredSize());
		
		httpResponse.setContentType("application/json;charset=UTF-8");
		JsonUtil.GSON.toJson(result, httpResponse.getWriter());
	}
	
	@Override
	public void doGet(HttpServletRequest httpRequest, HttpServletResponse httpResponse) throws IOException {
		this.doPost(httpRequest, httpResponse);
	}
}
