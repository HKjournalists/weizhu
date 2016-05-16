package com.weizhu.webapp.boss.api.stats;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.common.util.concurrent.Futures;
import com.google.gson.JsonObject;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.weizhu.common.service.ServiceUtil;
import com.weizhu.common.utils.JsonUtil;
import com.weizhu.proto.StatsService;
import com.weizhu.proto.BossProtos.BossHead;
import com.weizhu.web.ParamUtil;

@Singleton
@SuppressWarnings("serial")
public class LoadDimServlet extends HttpServlet {

	private final Provider<BossHead> bossHeadProvider;
	private final StatsService statsService;
	
	@Inject
	public LoadDimServlet(Provider<BossHead> bossHeadProvider, StatsService statsService) {
		this.bossHeadProvider = bossHeadProvider;
		this.statsService = statsService;
	}
	
	@Override
	protected void doGet(HttpServletRequest httpRequest, HttpServletResponse httpResponse) throws ServletException, IOException {
		this.doPost(httpRequest, httpResponse);
	}
	
	@Override
	protected void doPost(HttpServletRequest httpRequest, HttpServletResponse httpResponse) throws ServletException, IOException {
		// 1. 取出参数
		String dimName = ParamUtil.getString(httpRequest, "dim_name", "");
		
		// 2. 调用Service
		
		final BossHead head = this.bossHeadProvider.get();
		
		if ("user".equals(dimName)) {
			Futures.getUnchecked(this.statsService.loadDimUser(head, ServiceUtil.EMPTY_REQUEST));
		} else if ("discover".equals(dimName)) {
			Futures.getUnchecked(this.statsService.loadDimDiscover(head, ServiceUtil.EMPTY_REQUEST));
		}
		
		JsonObject resultObj = new JsonObject();
		resultObj.addProperty("result", "SUCC");
		
		httpResponse.setContentType("application/json;charset=UTF-8");
		JsonUtil.GSON.toJson(resultObj, httpResponse.getWriter());
	}
}
