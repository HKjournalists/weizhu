package com.weizhu.webapp.admin.api;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;
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
import com.weizhu.proto.AdminService;
import com.weizhu.proto.AdminProtos;
import com.weizhu.proto.AdminProtos.AdminHead;
import com.weizhu.proto.AdminProtos.GetAdminByIdRequest;
import com.weizhu.proto.AdminProtos.GetAdminByIdResponse;
import com.weizhu.web.ParamUtil;

@Singleton
@SuppressWarnings("serial")
public class GetAdminByIdServlet extends HttpServlet {

	private final Provider<AdminHead> adminHeadProvider;
	private final AdminService adminService;
	
	@Inject
	public GetAdminByIdServlet(Provider<AdminHead> adminHeadProvider, AdminService adminService) {
		this.adminHeadProvider = adminHeadProvider;
		this.adminService = adminService;
	}
	
	@Override
	protected void doGet(HttpServletRequest httpRequest, HttpServletResponse httpResponse) throws ServletException, IOException {
		this.doPost(httpRequest, httpResponse);
	}

	@Override
	protected void doPost(HttpServletRequest httpRequest, HttpServletResponse httpResponse) throws ServletException, IOException {
		// 1. 取出参数
		List<Long> adminIdList = ParamUtil.getLongList(httpRequest, "admin_id", Collections.<Long>emptyList());

		// 4. 调用Service
		final AdminHead head = this.adminHeadProvider.get();
		
		GetAdminByIdRequest request = GetAdminByIdRequest.newBuilder()
				.addAllAdminId(adminIdList)
				.build();
		
		GetAdminByIdResponse response = Futures.getUnchecked(this.adminService.getAdminById(head, request));
		
		Map<Integer, AdminProtos.Role> refRoleMap = AdminUtil.toRefRoleMap(response.getRefRoleList());
		
		JsonArray adminArray = new JsonArray();
		for (AdminProtos.Admin admin : response.getAdminList()) {
			adminArray.add(AdminUtil.toJsonAdmin(admin, head.getCompanyId(), refRoleMap));
		}
		
		JsonObject resultObj = new JsonObject();
		resultObj.add("admin", adminArray);
		
		httpResponse.setContentType("application/json;charset=UTF-8");
		JsonUtil.GSON.toJson(resultObj, httpResponse.getWriter());
	}

}
