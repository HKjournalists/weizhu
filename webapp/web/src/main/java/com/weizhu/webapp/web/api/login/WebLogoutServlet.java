package com.weizhu.webapp.web.api.login;

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
import com.weizhu.proto.LoginService;
import com.weizhu.proto.WeizhuProtos.RequestHead;

@Singleton
@SuppressWarnings("serial")
public class WebLogoutServlet extends HttpServlet {

	private final Provider<RequestHead> requestHeadProvider;
	private final LoginService loginService;
	
	@Inject
	public WebLogoutServlet(Provider<RequestHead> requestHeadProvider, LoginService loginService) {
		this.requestHeadProvider = requestHeadProvider;
		this.loginService = loginService;
	}
	
	@Override
	protected void doGet(HttpServletRequest httpRequest, HttpServletResponse httpResponse) throws ServletException, IOException {
		this.doPost(httpRequest, httpResponse);
	}
	
	@Override
	protected void doPost(HttpServletRequest httpRequest, HttpServletResponse httpResponse) throws ServletException, IOException {
		
		final RequestHead head = this.requestHeadProvider.get();
		
		this.loginService.webLogout(head, ServiceUtil.EMPTY_REQUEST);
		
		Cookie cookie = new Cookie("x-web-login-session-key", "");
		cookie.setPath("/");
		cookie.setMaxAge(0);
		httpResponse.addCookie(cookie);
		
		JsonObject ret = new JsonObject();
		ret.addProperty("result", "SUCC");
		httpResponse.setContentType("application/json;charset=UTF-8");
		JsonUtil.GSON.toJson(ret, httpResponse.getWriter());
	}

}
