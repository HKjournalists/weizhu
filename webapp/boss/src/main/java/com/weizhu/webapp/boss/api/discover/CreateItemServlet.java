package com.weizhu.webapp.boss.api.discover;

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
import com.weizhu.proto.AdminDiscoverService;
import com.weizhu.proto.AdminDiscoverProtos.CreateItemRequest;
import com.weizhu.proto.AdminDiscoverProtos.CreateItemResponse;
import com.weizhu.proto.BossProtos.BossHead;
import com.weizhu.web.ParamUtil;

@Singleton
@SuppressWarnings("serial")
public class CreateItemServlet extends HttpServlet {

	private final Provider<BossHead> bossHeadProvider;
	private final AdminDiscoverService adminDiscoverService;
	
	@Inject
	public CreateItemServlet(Provider<BossHead> bossHeadProvider, AdminDiscoverService adminDiscoverService) {
		this.bossHeadProvider = bossHeadProvider;
		this.adminDiscoverService = adminDiscoverService;
	}
	
	@Override
	protected void doGet(HttpServletRequest httpRequest, HttpServletResponse httpResponse) throws ServletException, IOException {
	}
	
	@Override
	protected void doPost(HttpServletRequest httpRequest, HttpServletResponse httpResponse) throws ServletException, IOException {
		Long companyId = ParamUtil.getLong(httpRequest, "company_id", null);

		final BossHead head = companyId == null ? this.bossHeadProvider.get() : this.bossHeadProvider.get().toBuilder().setCompanyId(companyId).build();
		
		CreateItemRequest.Builder requestBuilder = CreateItemRequest.newBuilder();
		
		JsonUtil.PROTOBUF_JSON_FORMAT.merge(httpRequest.getInputStream(), requestBuilder);
		
		CreateItemResponse response = Futures.getUnchecked(this.adminDiscoverService.createItem(head, requestBuilder.build()));
		
		httpResponse.setContentType("application/json;charset=UTF-8");
		JsonUtil.PROTOBUF_JSON_FORMAT.print(response, httpResponse.getWriter());
	}
}