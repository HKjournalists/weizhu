package com.weizhu.webapp.admin.api.scene.tool.recommender;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.common.util.concurrent.Futures;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.weizhu.proto.AdminProtos.AdminHead;
import com.weizhu.common.utils.JsonUtil;
import com.weizhu.proto.AdminSceneProtos;
import com.weizhu.proto.AdminSceneService;
import com.weizhu.web.ParamUtil;

@Singleton
@SuppressWarnings("serial")
public class AddRecommendProdToCompetitorProdServlet extends HttpServlet {
	private final Provider<AdminHead> adminHeadProvider;
	private final AdminSceneService adminSceneService;

	@Inject
	public AddRecommendProdToCompetitorProdServlet(Provider<AdminHead> adminHeadProvider, AdminSceneService adminSceneService) {
		this.adminHeadProvider = adminHeadProvider;
		this.adminSceneService = adminSceneService;
	}

	@Override
	protected void doGet(HttpServletRequest httpRequest, HttpServletResponse httpResponse) throws ServletException, IOException {
		this.doPost(httpRequest, httpResponse);
	}

	@Override
	protected void doPost(HttpServletRequest httpRequest, HttpServletResponse httpResponse) throws ServletException, IOException {

		// 1. 取出参数

		List<Integer> recommendProductIds = ParamUtil.getIntList(httpRequest, "recommend_product_id", Collections.<Integer> emptyList());
		int competitorProductId = ParamUtil.getInt(httpRequest, "competitor_product_id", -1);

		AdminSceneProtos.AddRecommendProdToCompetitorProdRequest.Builder request = AdminSceneProtos.AddRecommendProdToCompetitorProdRequest.newBuilder()
				.addAllRecommendProductId(recommendProductIds)
				.setCompetitorProductId(competitorProductId);


		AdminSceneProtos.AddRecommendProdToCompetitorProdResponse response = Futures.getUnchecked(this.adminSceneService.addRecommendProdToCompetitorProd(this.adminHeadProvider.get(),
				request.build()));

		httpResponse.setContentType("application/json;charset=UTF-8");
		JsonUtil.PROTOBUF_JSON_FORMAT.print(response, httpResponse.getWriter());
	}

}
