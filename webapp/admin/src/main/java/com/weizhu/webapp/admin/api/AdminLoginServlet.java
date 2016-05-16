package com.weizhu.webapp.admin.api;

import java.io.IOException;
import java.util.Map;
import java.util.TreeMap;

import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.common.util.concurrent.Futures;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.weizhu.proto.AdminProtos;
import com.weizhu.proto.AdminProtos.AdminAnonymousHead;
import com.weizhu.proto.AdminProtos.AdminLoginRequest;
import com.weizhu.proto.AdminProtos.AdminLoginResponse;
import com.weizhu.common.utils.JsonUtil;
import com.weizhu.proto.AdminService;
import com.weizhu.proto.CompanyProtos;
import com.weizhu.web.ParamUtil;

@Singleton
@SuppressWarnings("serial")
public class AdminLoginServlet extends HttpServlet {

	private final Provider<AdminAnonymousHead> adminAnonymousHeadProvider;
	private final AdminService adminService;
	
	@Inject
	public AdminLoginServlet(Provider<AdminAnonymousHead> adminAnonymousHeadProvider, AdminService adminService) {
		this.adminAnonymousHeadProvider = adminAnonymousHeadProvider;
		this.adminService = adminService;
	}
	
	@Override
	protected void doPost(HttpServletRequest httpRequest, HttpServletResponse httpResponse) throws ServletException, IOException {
		// 1. 取出参数
		String adminEmail = ParamUtil.getString(httpRequest, "admin_email", "");
		String adminPassword = ParamUtil.getString(httpRequest, "admin_password", "");
		
		// 2. 调用Service
		final AdminAnonymousHead head = this.adminAnonymousHeadProvider.get();
		
		AdminLoginRequest request = AdminLoginRequest.newBuilder()
				.setAdminEmail(adminEmail)
				.setAdminPassword(adminPassword)
				.build();
		
		AdminLoginResponse response = Futures.getUnchecked(this.adminService.adminLogin(head, request));
		
		if (response.getResult() != AdminLoginResponse.Result.SUCC) {
			httpResponse.setContentType("application/json;charset=UTF-8");
			JsonUtil.PROTOBUF_JSON_FORMAT.print(response, httpResponse.getWriter());
			return;
		}
		
		Cookie cookie = new Cookie("x-admin-session-key", response.getSessionKey());
		cookie.setPath("/");
		cookie.setMaxAge(12 * 60 * 60);
		httpResponse.addCookie(cookie);
		
		JsonObject resultObj = new JsonObject();
		resultObj.addProperty("result", "SUCC");
		resultObj.add("admin", AdminUtil.toJsonAdmin(response.getAdmin(), null, null));
		
		Map<Long, CompanyProtos.Company> refCompanyMap = new TreeMap<Long, CompanyProtos.Company>();
		for (CompanyProtos.Company company : response.getRefCompanyList()) {
			refCompanyMap.put(company.getCompanyId(), company);
		}
		
		JsonArray companyArray = new JsonArray();
		for (AdminProtos.Admin.Company c : response.getAdmin().getCompanyList()) {
			CompanyProtos.Company company = refCompanyMap.get(c.getCompanyId());
			if (company != null) {
				companyArray.add(AdminUtil.toJsonCompany(company));
			}
		}
		
		resultObj.add("company", companyArray);
		
		httpResponse.setContentType("application/json;charset=UTF-8");
		JsonUtil.GSON.toJson(resultObj, httpResponse.getWriter());
	}

}
