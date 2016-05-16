package com.weizhu.webapp.admin.api;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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
import com.weizhu.proto.AdminUserService;
import com.weizhu.proto.UserProtos;
import com.weizhu.web.ParamUtil;
import com.weizhu.proto.AdminProtos.AdminHead;
import com.weizhu.proto.AdminProtos.CreateAdminRequest;
import com.weizhu.proto.AdminProtos.CreateAdminResponse;
import com.weizhu.proto.AdminUserProtos.GetTeamByIdRequest;


@Singleton
@SuppressWarnings("serial")
public class CreateAdminServlet extends HttpServlet {

	private final Provider<AdminHead> adminHeadProvider;
	private final AdminService adminService;
	private final AdminUserService adminUserService;
	
	@Inject
	public CreateAdminServlet(Provider<AdminHead> adminHeadProvider, AdminService adminService, AdminUserService adminUserService) {
		this.adminHeadProvider = adminHeadProvider;
		this.adminService = adminService;
		this.adminUserService = adminUserService;
	}

	@Override
	protected void doPost(HttpServletRequest httpRequest, HttpServletResponse httpResponse) throws ServletException, IOException {
		// 1. 取出参数
		String adminName = ParamUtil.getString(httpRequest, "admin_name", "");
		String adminEmail = ParamUtil.getString(httpRequest, "admin_email", "");
		String adminPassword = ParamUtil.getString(httpRequest, "admin_password", "");
		List<Integer> roleIdList = ParamUtil.getIntList(httpRequest, "role_id", Collections.<Integer> emptyList());
		boolean enableTeamPermit = ParamUtil.getBoolean(httpRequest, "enable_team_permit", false);
		List<Integer> permitTeamIdList = enableTeamPermit ? ParamUtil.getIntList(httpRequest, "permit_team_id", Collections.<Integer>emptyList()) : Collections.<Integer>emptyList();
		
		// 2. 调用Service
		final AdminHead head = this.adminHeadProvider.get();
		
		if (enableTeamPermit && !permitTeamIdList.isEmpty()) {
			List<UserProtos.Team> teamList = Futures.getUnchecked(
					this.adminUserService.getTeamById(head, 
							GetTeamByIdRequest.newBuilder()
							.addAllTeamId(permitTeamIdList)
							.build())).getTeamList();
			List<Integer> newPermitTeamIdList = new ArrayList<Integer>();
			for (UserProtos.Team team : teamList) {
				if (permitTeamIdList.contains(team.getTeamId())) {
					newPermitTeamIdList.add(team.getTeamId());
				}
			}
			permitTeamIdList = newPermitTeamIdList;
		}
		
		CreateAdminRequest request = CreateAdminRequest.newBuilder()
				.setAdminName(adminName)
				.setAdminEmail(adminEmail)
				.setAdminPassword(adminPassword)
				.addAllRoleId(roleIdList)
				.setEnableTeamPermit(enableTeamPermit)
				.addAllPermitTeamId(permitTeamIdList)
				.build();
		
		CreateAdminResponse response = Futures.getUnchecked(this.adminService.createAdmin(head, request));
		
		httpResponse.setContentType("application/json;charset=UTF-8");
		JsonUtil.PROTOBUF_JSON_FORMAT.print(response, httpResponse.getWriter());
	}
}
