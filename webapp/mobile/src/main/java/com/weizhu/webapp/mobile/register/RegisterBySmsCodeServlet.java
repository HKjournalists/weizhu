package com.weizhu.webapp.mobile.register;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.common.base.Splitter;
import com.google.common.util.concurrent.Futures;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.weizhu.proto.LoginProtos.RegisterBySmsCodeResponse;
import com.weizhu.common.utils.JsonUtil;
import com.weizhu.proto.LoginService;
import com.weizhu.proto.LoginProtos.RegisterBySmsCodeRequest;
import com.weizhu.proto.UserProtos;
import com.weizhu.proto.WeizhuProtos.AnonymousHead;
import com.weizhu.web.ParamUtil;

@Singleton
@SuppressWarnings("serial")
public class RegisterBySmsCodeServlet extends HttpServlet {

	private final Provider<AnonymousHead> anonymousHeadProvider;
	private final LoginService loginService;
	
	@Inject
	public RegisterBySmsCodeServlet(Provider<AnonymousHead> anonymousHeadProvider, LoginService loginService) {
		this.anonymousHeadProvider = anonymousHeadProvider;
		this.loginService = loginService;
	}
	
	@Override
	protected void doGet(HttpServletRequest httpRequest, HttpServletResponse httpResponse) throws ServletException, IOException {
		this.doPost(httpRequest, httpResponse);
	}
	
	private static final Splitter EXTS_SPLITTER = Splitter.on("|").trimResults().omitEmptyStrings();
	private static final Splitter EXTS_FIELD_SPLITTER = Splitter.on("^").trimResults().omitEmptyStrings();

	@Override
	protected void doPost(HttpServletRequest httpRequest, HttpServletResponse httpResponse) throws ServletException, IOException {
		String companyKey = ParamUtil.getString(httpRequest, "company_key", "");
		String userName = ParamUtil.getString(httpRequest, "user_name", "");
		String genderStr = ParamUtil.getString(httpRequest, "gender", "");
		String email = ParamUtil.getString(httpRequest, "email", "");
		List<String> teamList = ParamUtil.getStringList(httpRequest, "team", Collections.<String>emptyList());
		String position = ParamUtil.getString(httpRequest, "position", "");
		String level = ParamUtil.getString(httpRequest, "level", "");
		String mobileNo = ParamUtil.getString(httpRequest, "mobile_no", "");
		int smsCode = ParamUtil.getInt(httpRequest, "sms_code", 0);
		String phoneNo = ParamUtil.getString(httpRequest, "phone_no", null);
		
		Map<String, String> extsMap = new TreeMap<String, String>();
		
		for (String extsStr : EXTS_SPLITTER.splitToList(ParamUtil.getString(httpRequest, "exts", ""))) {
			List<String> extsFieldList = EXTS_FIELD_SPLITTER.splitToList(extsStr);
			if (extsFieldList.size() >= 2) {
				String name = extsFieldList.get(0);
				String value = extsFieldList.get(1);
				
				if (!name.isEmpty() && !value.isEmpty()) {
					extsMap.put(name, value);
				}
			}
		}
		
		
		final AnonymousHead head = anonymousHeadProvider.get();
		
		RegisterBySmsCodeRequest.Builder requestBuilder = RegisterBySmsCodeRequest.newBuilder();
		
		requestBuilder.setCompanyKey(companyKey);
		requestBuilder.setUserName(userName);
		
		for (UserProtos.UserBase.Gender gender : UserProtos.UserBase.Gender.values()) {
			if (gender.name().equals(genderStr)) {
				requestBuilder.setGender(gender);
				break;
			}
		}
		
		if (!email.isEmpty()) {
			requestBuilder.setEmail(email);
		}
		
		for (String team : teamList) {
			if (!team.isEmpty()) {
				requestBuilder.addTeam(team);
			}
		}
		
		if (!position.isEmpty()) {
			requestBuilder.setPosition(position);
		}
		
		if (!level.isEmpty()) {
			requestBuilder.setLevel(level);
		}
		
		requestBuilder.setMobileNo(mobileNo);
		requestBuilder.setSmsCode(smsCode);
		
		for (Entry<String, String> entry : extsMap.entrySet()) {
			requestBuilder.addExtsName(entry.getKey());
			requestBuilder.addExtsValue(entry.getValue());
		}
		if (phoneNo != null && !phoneNo.isEmpty()) {
			requestBuilder.setPhoneNo(phoneNo);
		}
		
		RegisterBySmsCodeResponse response = Futures.getUnchecked(this.loginService.registerBySmsCode(head, requestBuilder.build()));
		
		httpResponse.setContentType("application/json;charset=UTF-8");
		JsonUtil.PROTOBUF_JSON_FORMAT.print(response, httpResponse.getWriter());
	}

}
