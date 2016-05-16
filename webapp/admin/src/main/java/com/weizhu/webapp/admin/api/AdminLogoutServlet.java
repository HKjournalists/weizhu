package com.weizhu.webapp.admin.api;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.gson.JsonObject;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.weizhu.common.service.ServiceUtil;
import com.weizhu.common.utils.JsonUtil;
import com.weizhu.proto.AdminProtos.AdminHead;
import com.weizhu.proto.AdminService;


@Singleton
@SuppressWarnings("serial")
public class AdminLogoutServlet extends HttpServlet {

	private final Provider<AdminHead> adminHeadProvider;
	private final AdminService adminService;
	
	@Inject
	public AdminLogoutServlet(Provider<AdminHead> adminHeadProvider, AdminService adminService) {
		this.adminHeadProvider = adminHeadProvider;
		this.adminService = adminService;
	}
	
	@Override
	protected void doPost(HttpServletRequest httpRequest, HttpServletResponse httpResponse) throws ServletException, IOException {

		final AdminHead head = this.adminHeadProvider.get();
		
		this.adminService.adminLogout(head, ServiceUtil.EMPTY_REQUEST);
		
		Cookie cookie = new Cookie("x-admin-session-key", "");
		cookie.setPath("/");
		cookie.setMaxAge(0);
		httpResponse.addCookie(cookie);
		
		JsonObject ret = new JsonObject();
		ret.addProperty("result", "SUCC");
		httpResponse.setContentType("application/json;charset=UTF-8");
		JsonUtil.GSON.toJson(ret, httpResponse.getWriter());
	}
}
