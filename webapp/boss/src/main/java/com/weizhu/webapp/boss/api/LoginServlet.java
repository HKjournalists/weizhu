package com.weizhu.webapp.boss.api;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.common.util.concurrent.Futures;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.weizhu.common.utils.JsonUtil;
import com.weizhu.proto.BossProtos.BossAnonymousHead;
import com.weizhu.proto.BossProtos.LoginRequest;
import com.weizhu.proto.BossProtos.LoginResponse;
import com.weizhu.proto.BossService;
import com.weizhu.web.ParamUtil;

@Singleton
@SuppressWarnings("serial")
public class LoginServlet extends HttpServlet {

	private final Provider<BossAnonymousHead> bossAnonymousHeadProvider;
	private final BossService bossService;
	
	@Inject
	public LoginServlet(Provider<BossAnonymousHead> bossAnonymousHeadProvider, BossService bossService) {
		this.bossAnonymousHeadProvider = bossAnonymousHeadProvider;
		this.bossService = bossService;
	}
	
	@Override
	protected void doGet(HttpServletRequest httpRequest, HttpServletResponse httpResponse) throws ServletException, IOException {
		this.doPost(httpRequest, httpResponse);
	}
	
	@Override
	protected void doPost(HttpServletRequest httpRequest, HttpServletResponse httpResponse) throws ServletException, IOException {
		
		// 1. 取出参数
		String bossId = ParamUtil.getString(httpRequest, "boss_id", "");
		String bossPassword = ParamUtil.getString(httpRequest, "boss_password", "");
		
		String sslClientCommonName = this.getSslClientCommonName(httpRequest);
		if (sslClientCommonName != null) {
			bossId = sslClientCommonName;
		}
		
		// 2. 调用Service
		final BossAnonymousHead head = this.bossAnonymousHeadProvider.get();
		
		LoginRequest request = LoginRequest.newBuilder()
				.setBossId(bossId)
				.setBossPassword(bossPassword)
				.build();
		
		LoginResponse response = Futures.getUnchecked(this.bossService.login(head, request));
		
		if (response.getResult() == LoginResponse.Result.SUCC) {
			Cookie cookie = new Cookie("x-boss-session-key", response.getSessionKey());
			cookie.setPath("/");
			cookie.setMaxAge(12 * 60 * 60);
			httpResponse.addCookie(cookie);
		}
		
		httpResponse.setContentType("application/json;charset=UTF-8");
		JsonUtil.PROTOBUF_JSON_FORMAT.print(response, httpResponse.getWriter());
	}
	
	private String getSslClientCommonName(HttpServletRequest httpRequest) {
		// /C=CN/ST=Beijing/L=Beijing/O=weizhu/OU=weizhu/CN=francislin/emailAddress=francislin@wehelpu.cn
		String sslClientSubjectDN = httpRequest.getHeader("X-SSL-Client-Subject-DN");
		if (sslClientSubjectDN == null) {
			return null;
		}
		
		String[] fields = sslClientSubjectDN.split("/");
		for (String field : fields) {
			if (field.startsWith("CN=")) {
				return field.substring("CN=".length());
			}
		}
		return null;
	}

}
