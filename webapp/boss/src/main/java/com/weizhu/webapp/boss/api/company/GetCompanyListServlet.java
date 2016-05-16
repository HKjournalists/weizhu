package com.weizhu.webapp.boss.api.company;

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
import com.weizhu.common.service.ServiceUtil;
import com.weizhu.common.utils.JsonUtil;
import com.weizhu.proto.BossProtos.BossHead;
import com.weizhu.proto.CompanyProtos.GetCompanyListResponse;
import com.weizhu.proto.CompanyProtos;
import com.weizhu.proto.CompanyService;

@Singleton
@SuppressWarnings("serial")
public class GetCompanyListServlet extends HttpServlet {

	private final Provider<BossHead> bossHeadProvider;
	private final CompanyService companyService;
	
	@Inject
	public GetCompanyListServlet(Provider<BossHead> bossHeadProvider, CompanyService companyService) {
		this.bossHeadProvider = bossHeadProvider;
		this.companyService = companyService;
	}
	
	@Override
	protected void doGet(HttpServletRequest httpRequest, HttpServletResponse httpResponse) throws ServletException, IOException {
		this.doPost(httpRequest, httpResponse);
	}
	
	@Override
	protected void doPost(HttpServletRequest httpRequest, HttpServletResponse httpResponse) throws ServletException, IOException {
		// 1. 取出参数
		
		// 2. 调用Service
		final BossHead head = this.bossHeadProvider.get();
		
		GetCompanyListResponse response = Futures.getUnchecked(this.companyService.getCompanyList(head, ServiceUtil.EMPTY_REQUEST));

		JsonObject resultObj = new JsonObject();
		resultObj.addProperty("result", "SUCC");
		
		JsonArray array = new JsonArray();
		for (CompanyProtos.Company company : response.getCompanyList()) {
			JsonObject c = new JsonObject();
			c.addProperty("company_id", company.getCompanyId());
			c.addProperty("company_name", company.getCompanyName());
			
			JsonArray a = new JsonArray();
			for (String k : company.getCompanyKeyList()) {
				a.add(k);
			}
			c.add("company_key", a);
			
			c.addProperty("server_name", company.getServerName());
			array.add(c);
		}
		resultObj.add("company", array);
		
		httpResponse.setContentType("application/json;charset=UTF-8");
		JsonUtil.GSON.toJson(resultObj, httpResponse.getWriter());
	}
}