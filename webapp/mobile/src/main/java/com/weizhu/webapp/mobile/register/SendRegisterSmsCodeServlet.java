package com.weizhu.webapp.mobile.register;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.common.util.concurrent.Futures;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.weizhu.proto.LoginProtos.SendRegisterSmsCodeResponse;
import com.weizhu.common.utils.JsonUtil;
import com.weizhu.proto.LoginService;
import com.weizhu.proto.LoginProtos.SendRegisterSmsCodeRequest;
import com.weizhu.proto.WeizhuProtos.AnonymousHead;
import com.weizhu.web.ParamUtil;

@Singleton
@SuppressWarnings("serial")
public class SendRegisterSmsCodeServlet extends HttpServlet {

	private final Provider<AnonymousHead> anonymousHeadProvider;
	private final LoginService loginService;
	
	@Inject
	public SendRegisterSmsCodeServlet(Provider<AnonymousHead> anonymousHeadProvider, LoginService loginService) {
		this.anonymousHeadProvider = anonymousHeadProvider;
		this.loginService = loginService;
	}
	
	@Override
	protected void doGet(HttpServletRequest httpRequest, HttpServletResponse httpResponse) throws ServletException, IOException {
		this.doPost(httpRequest, httpResponse);
	}

	@Override
	protected void doPost(HttpServletRequest httpRequest, HttpServletResponse httpResponse) throws ServletException, IOException {
		String companyKey = ParamUtil.getString(httpRequest, "company_key", "");
		String mobileNo = ParamUtil.getString(httpRequest, "mobile_no", "");
		
		final AnonymousHead head = anonymousHeadProvider.get();
		
		SendRegisterSmsCodeResponse response = Futures.getUnchecked(
				this.loginService.sendRegisterSmsCode(head, SendRegisterSmsCodeRequest.newBuilder()
						.setCompanyKey(companyKey)
						.setMobileNo(mobileNo)
						.build()));
		
		httpResponse.setContentType("application/json;charset=UTF-8");
		JsonUtil.PROTOBUF_JSON_FORMAT.print(response, httpResponse.getWriter());
	}
	
}
