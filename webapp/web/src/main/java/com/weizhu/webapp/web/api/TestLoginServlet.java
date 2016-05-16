package com.weizhu.webapp.web.api;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.util.concurrent.Futures;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.weizhu.common.utils.HexUtil;
import com.weizhu.common.utils.JsonUtil;
import com.weizhu.proto.LoginProtos.LoginBySmsCodeRequest;
import com.weizhu.proto.LoginProtos.LoginBySmsCodeResponse;
import com.weizhu.proto.LoginProtos.SendSmsCodeRequest;
import com.weizhu.proto.LoginProtos.SendSmsCodeResponse;
import com.weizhu.proto.LoginService;
import com.weizhu.proto.WeizhuProtos.Android;
import com.weizhu.proto.WeizhuProtos.AnonymousHead;
import com.weizhu.proto.WeizhuProtos.Invoke;
import com.weizhu.proto.WeizhuProtos.Network;
import com.weizhu.proto.WeizhuProtos.Weizhu;
import com.weizhu.web.ParamUtil;

@Singleton
@SuppressWarnings("serial")
public class TestLoginServlet extends HttpServlet {

	private static final Logger logger = LoggerFactory.getLogger(TestLoginServlet.class);
	
	private final LoginService loginService;
	
	@Inject
	public TestLoginServlet(LoginService loginService) {
		this.loginService = loginService;
	}
	
	@Override
	protected void doGet(HttpServletRequest httpRequest, HttpServletResponse httpResponse) throws ServletException, IOException {
		this.doPost(httpRequest, httpResponse);
	}
	
	@Override
	protected void doPost(HttpServletRequest httpRequest, HttpServletResponse httpResponse) throws ServletException, IOException {
		
		final String companyKey = ParamUtil.getString(httpRequest, "company_key", "");
		final String mobileNo = ParamUtil.getString(httpRequest, "mobile_no", "");
		final int smsCode = ParamUtil.getInt(httpRequest, "sms_code", 0);
		final String action = ParamUtil.getString(httpRequest, "action", "");
		
		logger.info("|" + action + "|" + companyKey + "|" + mobileNo + "|" + smsCode + "|");
		
		// just for test!
		AnonymousHead head = AnonymousHead.newBuilder()
				.setInvoke(Invoke.newBuilder().setInvokeId(0).setServiceName("TestService").setFunctionName("testLogin"))
				.setNetwork(Network.newBuilder()
						.setType(Network.Type.WIFI)
						.setProtocol(Network.Protocol.HTTP_PB)
						.setRemoteHost("127.0.0.1")
						.setRemotePort(8080))
				.setWeizhu(Weizhu.newBuilder()
						.setPlatform(Weizhu.Platform.ANDROID)
						.setVersionName("1.0.0")
						.setVersionCode(0)
						.setStage(Weizhu.Stage.ALPHA)
						.setBuildTime((int)(System.currentTimeMillis()/1000L)))
				.setAndroid(Android.newBuilder()
						.setDevice("device")
						.setManufacturer("LGE")
						.setBrand("google")
						.setModel("Nexus 5")
						.setSerial("test")
						.setRelease("4.4.4")
						.setSdkInt(19)
						.setCodename("REL"))
				.build();
		
		if ("SendSmsCode".equals(action)) {
			
			SendSmsCodeRequest request = SendSmsCodeRequest.newBuilder()
					.setCompanyKey(companyKey)
					.setMobileNo(mobileNo)
					.build();
			
			SendSmsCodeResponse response = Futures.getUnchecked(this.loginService.sendSmsCode(head, request));
			
			httpResponse.setContentType("application/json;charset=UTF-8");
			JsonUtil.PROTOBUF_JSON_FORMAT.print(response, httpResponse.getWriter());
		} else {
		
			LoginBySmsCodeRequest request = LoginBySmsCodeRequest.newBuilder()
					.setCompanyKey(companyKey)
					.setMobileNo(mobileNo)
					.setSmsCode(smsCode)
					.build();
			
			LoginBySmsCodeResponse response = Futures.getUnchecked(this.loginService.loginBySmsCode(head, request));
			
			if (response.getResult() == LoginBySmsCodeResponse.Result.SUCC) {
				Cookie cookie = new Cookie("x-session-key", HexUtil.bin2Hex(response.getSessionKey().toByteArray()));
				cookie.setPath("/");
				cookie.setMaxAge(24 * 60 * 60);
				
				httpResponse.addCookie(cookie);
				httpResponse.sendRedirect(httpRequest.getContextPath() + "/index.html");
				return;
			}
			
			httpResponse.setContentType("application/json;charset=UTF-8");
			JsonUtil.PROTOBUF_JSON_FORMAT.print(response, httpResponse.getWriter());
		}
	}

}
