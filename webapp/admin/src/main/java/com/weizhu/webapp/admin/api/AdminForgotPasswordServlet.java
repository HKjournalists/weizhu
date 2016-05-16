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
import com.weizhu.common.utils.JsonUtil;
import com.weizhu.proto.AdminProtos.AdminAnonymousHead;
import com.weizhu.proto.AdminService;
import com.weizhu.proto.AdminProtos.AdminForgotPasswordRequest;
import com.weizhu.proto.AdminProtos.AdminForgotPasswordResponse;
import com.weizhu.web.ParamUtil;

@Singleton
@SuppressWarnings("serial")
public class AdminForgotPasswordServlet extends HttpServlet {

	private final Provider<AdminAnonymousHead> adminAnonymousHeadProvider;
	private final AdminService adminService;
	
	@Inject
	public AdminForgotPasswordServlet(Provider<AdminAnonymousHead> adminAnonymousHeadProvider, AdminService adminService) {
		this.adminAnonymousHeadProvider = adminAnonymousHeadProvider;
		this.adminService = adminService;
	}

	@Override
	protected void doPost(HttpServletRequest httpRequest, HttpServletResponse httpResponse) throws ServletException, IOException {
		// 1. 取出参数
		String adminEmail = ParamUtil.getString(httpRequest, "admin_email", "");
		
		// 2. 调用Service
		final AdminAnonymousHead head = this.adminAnonymousHeadProvider.get();
		
		AdminForgotPasswordRequest request = AdminForgotPasswordRequest.newBuilder()
				.setAdminEmail(adminEmail)
				.build();
		
		AdminForgotPasswordResponse response = Futures.getUnchecked(this.adminService.adminForgotPassword(head, request));
		
		httpResponse.setContentType("application/json;charset=UTF-8");
		JsonUtil.PROTOBUF_JSON_FORMAT.print(response, httpResponse.getWriter());
	}

}
