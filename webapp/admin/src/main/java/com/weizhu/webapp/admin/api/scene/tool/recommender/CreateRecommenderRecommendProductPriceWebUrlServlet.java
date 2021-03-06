package com.weizhu.webapp.admin.api.scene.tool.recommender;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.common.util.concurrent.Futures;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.google.protobuf.ExtensionRegistry;
import com.weizhu.proto.AdminProtos.AdminHead;
import com.weizhu.common.utils.JsonUtil;
import com.weizhu.proto.AdminSceneProtos;
import com.weizhu.proto.AdminSceneProtos.CreateRecommenderRecommendProductPriceWebUrlRequest;
import com.weizhu.proto.AdminSceneService;
import com.weizhu.web.ParamUtil;

@Singleton
@SuppressWarnings("serial")
public class CreateRecommenderRecommendProductPriceWebUrlServlet extends HttpServlet {
	private final Provider<AdminHead> adminHeadProvider;
	private final AdminSceneService adminSceneService;

	@Inject
	public CreateRecommenderRecommendProductPriceWebUrlServlet(Provider<AdminHead> adminHeadProvider, AdminSceneService adminSceneService) {
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
		String createPriceWebUrlRequest = ParamUtil.getString(httpRequest, "create_recommender_recommend_product_price_web_url_request", "");

		CreateRecommenderRecommendProductPriceWebUrlRequest.Builder conditionBuilder = CreateRecommenderRecommendProductPriceWebUrlRequest.newBuilder();
		JsonUtil.PROTOBUF_JSON_FORMAT.merge(createPriceWebUrlRequest, ExtensionRegistry.getEmptyRegistry(), conditionBuilder);
		
		AdminSceneProtos.CreateRecommenderRecommendProductPriceWebUrlResponse response = Futures.getUnchecked(this.adminSceneService.createRecommenderRecommendProductPriceWebUrl(this.adminHeadProvider.get(),
				conditionBuilder.build()));

		httpResponse.setContentType("application/json;charset=UTF-8");
		JsonUtil.PROTOBUF_JSON_FORMAT.print(response, httpResponse.getWriter());
	}

}
