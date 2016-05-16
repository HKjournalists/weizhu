package com.weizhu.webapp.admin.api;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.weizhu.common.utils.JsonUtil;
import com.weizhu.proto.AdminProtos;
import com.weizhu.proto.CompanyProtos;
import com.weizhu.web.filter.AdminInfo;

@Singleton
@SuppressWarnings("serial")
public class GetCompanyListServlet extends HttpServlet {

	private final Provider<AdminInfo> adminInfoProvider;
	
	@Inject
	public GetCompanyListServlet( 
			Provider<AdminInfo> adminInfoProvider
			) {
		this.adminInfoProvider = adminInfoProvider;
	}
	
	@Override
	protected void doGet(HttpServletRequest httpRequest, HttpServletResponse httpResponse) throws ServletException, IOException {
		this.doPost(httpRequest, httpResponse);
	}
	
	@Override
	protected void doPost(HttpServletRequest httpRequest, HttpServletResponse httpResponse) throws ServletException, IOException {		
		final AdminInfo adminInfo = this.adminInfoProvider.get();
		
		JsonArray companyArray = new JsonArray();
		if (adminInfo != null) {
			for (AdminProtos.Admin.Company c : adminInfo.admin().getCompanyList()) {
				CompanyProtos.Company company = adminInfo.refCompanyMap().get(c.getCompanyId());
				if (company != null) {
					companyArray.add(AdminUtil.toJsonCompany(company));
				}
			}
		}
		
		JsonObject resultObj = new JsonObject();
		resultObj.add("company_list", companyArray);
		
		httpResponse.setContentType("application/json;charset=UTF-8");
		JsonUtil.GSON.toJson(resultObj, httpResponse.getWriter());
	}
}
