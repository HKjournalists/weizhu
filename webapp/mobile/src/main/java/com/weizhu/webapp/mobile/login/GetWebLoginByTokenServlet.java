package com.weizhu.webapp.mobile.login;

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
import com.weizhu.proto.LoginService;
import com.weizhu.proto.LoginProtos.GetWebLoginByTokenRequest;
import com.weizhu.proto.LoginProtos.GetWebLoginByTokenResponse;
import com.weizhu.proto.WeizhuProtos.RequestHead;
import com.weizhu.web.ParamUtil;

@Singleton
@SuppressWarnings("serial")
public class GetWebLoginByTokenServlet extends HttpServlet {

	private final Provider<RequestHead> requestHeadProvider;
	private final LoginService loginService;
	
	@Inject
	public GetWebLoginByTokenServlet(Provider<RequestHead> requestHeadProvider, LoginService loginService) {
		this.requestHeadProvider = requestHeadProvider;
		this.loginService = loginService;
	}
	
	@Override
	protected void doGet(HttpServletRequest httpRequest, HttpServletResponse httpResponse) throws ServletException, IOException {
		this.doPost(httpRequest, httpResponse);
	}
	
	@Override
	protected void doPost(HttpServletRequest httpRequest, HttpServletResponse httpResponse) throws ServletException, IOException {
		final String token = ParamUtil.getString(httpRequest, "token", "");
		
		final RequestHead head = requestHeadProvider.get();
		
		GetWebLoginByTokenRequest request = GetWebLoginByTokenRequest.newBuilder()
				.setToken(token)
				.build();

		GetWebLoginByTokenResponse response = Futures.getUnchecked(this.loginService.getWebLoginByToken(head, request));

		httpResponse.setContentType("application/json;charset=UTF-8");
		JsonUtil.PROTOBUF_JSON_FORMAT.print(response, httpResponse.getWriter());
	}

}
