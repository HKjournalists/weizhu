package com.weizhu.webapp.admin.api;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
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
import com.weizhu.proto.AdminProtos;
import com.weizhu.proto.AdminService;
import com.weizhu.proto.AdminProtos.AdminHead;
import com.weizhu.proto.AdminProtos.GetRoleByIdRequest;
import com.weizhu.proto.AdminProtos.GetRoleByIdResponse;
import com.weizhu.web.ParamUtil;

@Singleton
@SuppressWarnings("serial")
public class GetRoleByIdServlet extends HttpServlet {

	private final Provider<AdminHead> adminHeadProvider;
	private final AdminService adminService;
	
	@Inject
	public GetRoleByIdServlet(Provider<AdminHead> adminHeadProvider, AdminService adminService) {
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
		List<Integer> roleIdList = ParamUtil.getIntList(httpRequest, "role_id", Collections.<Integer>emptyList());

		// 4. 调用Service
		final AdminHead head = this.adminHeadProvider.get();
		
		GetRoleByIdRequest request = GetRoleByIdRequest.newBuilder()
				.addAllRoleId(roleIdList)
				.build();
		
		GetRoleByIdResponse response = Futures.getUnchecked(this.adminService.getRoleById(head, request));
		
		JsonArray roleArray = new JsonArray();
		for (AdminProtos.Role role : response.getRoleList()) {
			roleArray.add(AdminUtil.toJsonRole(role));
		}
		
		JsonObject resultObj = new JsonObject();
		resultObj.add("role", roleArray);
		
		httpResponse.setContentType("application/json;charset=UTF-8");
		JsonUtil.GSON.toJson(resultObj, httpResponse.getWriter());
	}
}
