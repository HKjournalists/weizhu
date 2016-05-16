package com.weizhu.webapp.admin.api.user;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.common.util.concurrent.Futures;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.weizhu.common.utils.JsonUtil;
import com.weizhu.proto.AdminProtos.AdminHead;
import com.weizhu.proto.LoginProtos;
import com.weizhu.proto.LoginProtos.GetLoginSmsCodeRequest;
import com.weizhu.proto.LoginProtos.GetLoginSmsCodeResponse;
import com.weizhu.proto.LoginService;
import com.weizhu.proto.SessionProtos.GetSessionDataRequest;
import com.weizhu.proto.SessionProtos.GetSessionDataResponse;
import com.weizhu.proto.SessionProtos;
import com.weizhu.proto.SessionService;
import com.weizhu.proto.WeizhuProtos;
import com.weizhu.web.ParamUtil;

@Singleton
@SuppressWarnings("serial")
public class GetUserLoginSessionServlet extends HttpServlet {
	
	private final Provider<AdminHead> adminHeadProvider;
	private final LoginService loginService;
	private final SessionService sessionService;
	
	@Inject
	public GetUserLoginSessionServlet(Provider<AdminHead> adminHeadProvider, LoginService loginService, SessionService sessionService) {
		this.adminHeadProvider = adminHeadProvider;
		this.loginService = loginService;
		this.sessionService = sessionService;
	}
	
	@Override
	protected void doGet(HttpServletRequest httpRequest, HttpServletResponse httpResponse) throws ServletException, IOException {
		this.doPost(httpRequest, httpResponse);
	}

	@Override
	protected void doPost(HttpServletRequest httpRequest, HttpServletResponse httpResponse) throws ServletException, IOException {
		
		// 1. 取出参数
		long userId = ParamUtil.getLong(httpRequest, "user_id", 0L);
		
		// 2. 调用Service
		
		final AdminHead head = adminHeadProvider.get();
		
		JsonObject resultObj = new JsonObject();
		
		{
			GetLoginSmsCodeRequest request = GetLoginSmsCodeRequest.newBuilder()
					.setUserId(userId)
					.build();
			
			GetLoginSmsCodeResponse response = Futures.getUnchecked(this.loginService.getLoginSmsCode(head, request));
			
			if (response.getSmsCodeCount() > 0) {
				int now = (int) (System.currentTimeMillis() / 1000L);
				LoginProtos.SmsCode smsCode = response.getSmsCode(0);
				JsonObject obj = new JsonObject();
				obj.addProperty("code", smsCode.getSmsCode());
				obj.addProperty("create_time", smsCode.getCreateTime());
				obj.addProperty("mobile_no", smsCode.getMobileNo());
				obj.addProperty("is_expired", now - smsCode.getCreateTime() > 60 * 60);
				resultObj.add("login", obj);
			}
		}
		{
			GetSessionDataRequest request = GetSessionDataRequest.newBuilder()
					.addUserId(userId)
					.build();
			
			GetSessionDataResponse response = Futures.getUnchecked(this.sessionService.getSessionData(head, request));
			
			JsonArray data = new JsonArray();
			
			for (SessionProtos.SessionData sessionData : response.getSessionDataList()) {
				JsonObject s = new JsonObject();
				
				s.addProperty("session_id", String.valueOf(sessionData.getSession().getSessionId()));
				s.addProperty("login_time", sessionData.getLoginTime());
				s.addProperty("active_time", sessionData.getActiveTime());
				if (sessionData.hasWeizhu()) {
					s.addProperty("weizhu_platform", sessionData.getWeizhu().getPlatform().name());
					s.addProperty("weizhu_version_name", sessionData.getWeizhu().getVersionName());
					s.addProperty("weizhu_stage", sessionData.getWeizhu().getStage().name());
					s.addProperty("weizhu_build_time", sessionData.getWeizhu().getBuildTime());
				} else {
					s.addProperty("weizhu_platform", "");
					s.addProperty("weizhu_version_name", "");
					s.addProperty("weizhu_stage", "");
					s.addProperty("weizhu_build_time", 0);
				}
				
				StringBuilder sb = new StringBuilder();
				if (sessionData.hasAndroid()) {
					final WeizhuProtos.Android android = sessionData.getAndroid();
					sb.append("[Android:");
					sb.append(android.getDevice()).append("/");
					sb.append(android.getManufacturer()).append("/");
					sb.append(android.getBrand()).append("/");
					sb.append(android.getModel()).append("/");
					sb.append(android.getSerial()).append("/");
					sb.append(android.getRelease()).append("/");
					sb.append(android.getSdkInt()).append("/");
					sb.append(android.getCodename()).append("]");
				}
				
				if (sessionData.hasIphone()) {
					final WeizhuProtos.Iphone iphone = sessionData.getIphone();
					sb.append("[Iphone:");
					sb.append(iphone.getName()).append("/");
					sb.append(iphone.getSystemName()).append("/");
					sb.append(iphone.getSystemVersion()).append("/");
					sb.append(iphone.getModel()).append("/");
					sb.append(iphone.getLocalizedModel()).append("/");
					sb.append(iphone.getDeviceToken()).append("/");
					sb.append(iphone.getMac()).append("/");
					sb.append(iphone.getAppId()).append("]");
				}
				s.addProperty("device_info", sb.toString());
				
				data.add(s);
			}
			
			resultObj.add("session", data);
		}
		
		httpResponse.setContentType("application/json;charset=UTF-8");
		JsonUtil.GSON.toJson(resultObj, httpResponse.getWriter());
	}

}
