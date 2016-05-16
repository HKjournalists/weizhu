package com.weizhu.webapp.boss.api;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.gson.JsonObject;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.weizhu.common.service.ServiceUtil;
import com.weizhu.common.utils.JsonUtil;
import com.weizhu.proto.BossProtos.BossHead;
import com.weizhu.proto.BossService;

@Singleton
@SuppressWarnings("serial")
public class LogoutServlet extends HttpServlet {

	private final Provider<BossHead> bossHeadProvider;
	private final BossService bossService;
	
	@Inject
	public LogoutServlet(Provider<BossHead> bossHeadProvider, BossService bossService) {
		this.bossHeadProvider = bossHeadProvider;
		this.bossService = bossService;
	}
	
	@Override
	protected void doGet(HttpServletRequest httpRequest, HttpServletResponse httpResponse) throws ServletException, IOException {
		this.doPost(httpRequest, httpResponse);
	}

	@Override
	protected void doPost(HttpServletRequest httpRequest, HttpServletResponse httpResponse) throws ServletException, IOException {

		final BossHead head = this.bossHeadProvider.get();
		
		this.bossService.logout(head, ServiceUtil.EMPTY_REQUEST);
		
		httpResponse.setContentType("application/json;charset=UTF-8");
		
		JsonObject ret = new JsonObject();
		ret.addProperty("result", "SUCC");
		JsonUtil.GSON.toJson(ret, httpResponse.getWriter());
	}
}