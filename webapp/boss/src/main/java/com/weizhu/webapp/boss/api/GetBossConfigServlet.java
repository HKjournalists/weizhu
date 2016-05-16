package com.weizhu.webapp.boss.api;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.common.util.concurrent.Futures;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.weizhu.common.service.ServiceUtil;
import com.weizhu.common.utils.JsonUtil;
import com.weizhu.proto.BossProtos.BossHead;
import com.weizhu.proto.SystemProtos.GetBossConfigResponse;
import com.weizhu.proto.SystemService;

@Singleton
@SuppressWarnings("serial")
public class GetBossConfigServlet extends HttpServlet {

	private final Provider<BossHead> bossHeadProvider;
	private final SystemService systemService;
	
	@Inject
	public GetBossConfigServlet(Provider<BossHead> bossHeadProvider, SystemService systemService) {
		this.bossHeadProvider = bossHeadProvider;
		this.systemService = systemService;
	}
	
	@Override
	protected void doGet(HttpServletRequest httpRequest, HttpServletResponse httpResponse) throws ServletException, IOException {
		this.doPost(httpRequest, httpResponse);
	}
	
	@Override
	protected void doPost(HttpServletRequest httpRequest, HttpServletResponse httpResponse) throws ServletException, IOException {
		final BossHead head = this.bossHeadProvider.get();
		
		GetBossConfigResponse response = Futures.getUnchecked(this.systemService.getBossConfig(head, ServiceUtil.EMPTY_REQUEST));
		httpResponse.setContentType("application/json;charset=UTF-8");
		JsonUtil.PROTOBUF_JSON_FORMAT.print(response, httpResponse.getWriter());
	}
	
}
