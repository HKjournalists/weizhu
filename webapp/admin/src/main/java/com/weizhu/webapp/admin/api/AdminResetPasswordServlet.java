package com.weizhu.webapp.admin.api;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.common.util.concurrent.Futures;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.weizhu.proto.AdminProtos.AdminAnonymousHead;
import com.weizhu.common.utils.JsonUtil;
import com.weizhu.proto.AdminService;
import com.weizhu.proto.AdminProtos.AdminResetPasswordRequest;
import com.weizhu.proto.AdminProtos.AdminResetPasswordResponse;
import com.weizhu.web.ParamUtil;

@Singleton
@SuppressWarnings("serial")
public class AdminResetPasswordServlet extends HttpServlet {

	private final Provider<AdminAnonymousHead> adminAnonymousHeadProvider;
	private final AdminService adminService;
	
	@Inject
	public AdminResetPasswordServlet(Provider<AdminAnonymousHead> adminAnonymousHeadProvider, AdminService adminService) {
		this.adminAnonymousHeadProvider = adminAnonymousHeadProvider;
		this.adminService = adminService;
	}

	@Override
	protected void doPost(HttpServletRequest httpRequest, HttpServletResponse httpResponse) throws ServletException, IOException {
		// 1. 取出参数
		String adminEmail = ParamUtil.getString(httpRequest, "admin_email", "");
		String oldPassword = ParamUtil.getString(httpRequest, "old_password", "");
		String newPassword = ParamUtil.getString(httpRequest, "new_password", "");
		
		// 2. 调用Service
		final AdminAnonymousHead head = this.adminAnonymousHeadProvider.get();
		
		AdminResetPasswordRequest request = AdminResetPasswordRequest.newBuilder()
				.setAdminEmail(adminEmail)
				.setOldPassword(oldPassword)
				.setNewPassword(newPassword)
				.build();
		
		AdminResetPasswordResponse response = Futures.getUnchecked(this.adminService.adminResetPassword(head, request));
		
		httpResponse.setContentType("application/json;charset=UTF-8");
		JsonUtil.PROTOBUF_JSON_FORMAT.print(response, httpResponse.getWriter());
	}
	
}
