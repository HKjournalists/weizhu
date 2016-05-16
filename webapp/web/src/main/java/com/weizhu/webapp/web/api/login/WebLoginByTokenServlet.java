package com.weizhu.webapp.web.api.login;

import java.io.IOException;
import javax.servlet.AsyncContext;
import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.gson.JsonObject;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.weizhu.common.utils.JsonUtil;
import com.weizhu.proto.LoginService;
import com.weizhu.proto.LoginProtos.WebLoginByTokenRequest;
import com.weizhu.proto.LoginProtos.WebLoginByTokenResponse;
import com.weizhu.proto.WeizhuProtos.AnonymousHead;
import com.weizhu.web.ParamUtil;

@Singleton
@SuppressWarnings("serial")
public class WebLoginByTokenServlet extends HttpServlet {

	private final Provider<AnonymousHead> anonymousHeadProvider;
	private final LoginService loginService;
	
	@Inject
	public WebLoginByTokenServlet(Provider<AnonymousHead> anonymousHeadProvider, LoginService loginService) {
		this.anonymousHeadProvider = anonymousHeadProvider;
		this.loginService = loginService;
	}
	
	@Override
	protected void doGet(HttpServletRequest httpRequest, HttpServletResponse httpResponse) throws ServletException, IOException {
		this.doPost(httpRequest, httpResponse);
	}

	private static final String RESPONSE_FUTURE_ATTR = "com.weizhu.webapp.web.api.login.WebLoginByTokenServlet.RESPONSE_FUTURE_ATTR";
	
	@Override
	protected void doPost(HttpServletRequest httpRequest, HttpServletResponse httpResponse) throws ServletException, IOException {
		@SuppressWarnings("unchecked")
		ListenableFuture<WebLoginByTokenResponse> responseFuture = (ListenableFuture<WebLoginByTokenResponse>) httpRequest.getAttribute(RESPONSE_FUTURE_ATTR);
		
		if (responseFuture == null) {
			final String token = ParamUtil.getString(httpRequest, "token", "");
			final AnonymousHead head = this.anonymousHeadProvider.get();
			
			WebLoginByTokenRequest request = WebLoginByTokenRequest.newBuilder()
					.setToken(token)
					.build();
			
			responseFuture = this.loginService.webLoginByToken(head, request);
		}
		
		if (responseFuture.isDone()) {
			WebLoginByTokenResponse response = Futures.getUnchecked(responseFuture);
			
			if (response.getResult() == WebLoginByTokenResponse.Result.SUCC) {
				Cookie cookie = new Cookie("x-web-login-session-key", response.getWebLoginSessionKey());
				cookie.setPath("/");
				cookie.setMaxAge(24 * 60 * 60);
				
				httpResponse.addCookie(cookie);
			}
			
			JsonObject resultObj = new JsonObject();
			resultObj.addProperty("result", response.getResult().name());
			if (response.hasFailText()) {
				resultObj.addProperty("fail_text", response.getFailText());
			}
			if (response.hasWebLoginSessionKey()) {
				resultObj.addProperty("web_login_session_key", response.getWebLoginSessionKey());
			}
			if (response.hasUser()) {
				resultObj.addProperty("user_id", response.getUser().getBase().getUserId());
				resultObj.addProperty("user_name", response.getUser().getBase().getUserName());
			}
			
			httpResponse.setContentType("application/json;charset=UTF-8");
			JsonUtil.GSON.toJson(resultObj, httpResponse.getWriter());
		} else {
			httpRequest.setAttribute(RESPONSE_FUTURE_ATTR, responseFuture);
			final AsyncContext asyncContext = httpRequest.startAsync();
			asyncContext.setTimeout(65000);
			responseFuture.addListener(new Runnable() {

				@Override
				public void run() {
					asyncContext.dispatch();
				}
			
			}, MoreExecutors.directExecutor());
		}
	}
	
}
