package com.weizhu.webapp.admin.api;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.common.util.concurrent.Futures;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.weizhu.common.utils.JsonUtil;
import com.weizhu.proto.AdminService;
import com.weizhu.proto.AdminProtos.AdminHead;
import com.weizhu.proto.AdminProtos.CreateRoleRequest;
import com.weizhu.proto.AdminProtos.CreateRoleResponse;
import com.weizhu.web.ParamUtil;
import com.weizhu.webapp.admin.PermissionConst;


@Singleton
@SuppressWarnings("serial")
public class CreateRoleServlet extends HttpServlet {

	private final Provider<AdminHead> adminHeadProvider;
	private final AdminService adminService;
	
	@Inject
	public CreateRoleServlet(Provider<AdminHead> adminHeadProvider, AdminService adminService) {
		this.adminHeadProvider = adminHeadProvider;
		this.adminService = adminService;
	}

	@Override
	protected void doPost(HttpServletRequest httpRequest, HttpServletResponse httpResponse) throws ServletException, IOException {
		// 1. 取出参数
		String roleName = ParamUtil.getString(httpRequest, "role_name", "");
		List<String> permissionIdList = ParamUtil.getStringList(httpRequest, "permission_id", Collections.<String> emptyList());
		
		Set<String> permissionIdSet = new TreeSet<String>();
		for (String permissionId : permissionIdList) {
			if (PermissionConst.permissionMap().containsKey(permissionId)) {
				permissionIdSet.add(permissionId);
			}
		}
		
		// 2. 调用Service
		final AdminHead head = this.adminHeadProvider.get();
		
		CreateRoleRequest request = CreateRoleRequest.newBuilder()
				.setRoleName(roleName)
				.addAllPermissionId(permissionIdSet)
				.build();
		
		CreateRoleResponse response = Futures.getUnchecked(this.adminService.createRole(head, request));
		
		httpResponse.setContentType("application/json;charset=UTF-8");
		JsonUtil.PROTOBUF_JSON_FORMAT.print(response, httpResponse.getWriter());
	}
}
