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
import com.weizhu.proto.AdminSceneService;
import com.weizhu.proto.SceneProtos;
import com.weizhu.web.ParamUtil;

@Singleton
@SuppressWarnings("serial")
public class UpdateRecommenderRecommendProductPriceWebUrlServlet extends HttpServlet {
	private final Provider<AdminHead> adminHeadProvider;
	private final AdminSceneService adminSceneService;

	@Inject
	public UpdateRecommenderRecommendProductPriceWebUrlServlet(Provider<AdminHead> adminHeadProvider, AdminSceneService adminSceneService) {
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
		String priceWebUrl = ParamUtil.getString(httpRequest, "price_web_url", "");

		AdminSceneProtos.UpdateRecommenderRecommendProductPriceWebUrlRequest.Builder request = AdminSceneProtos.UpdateRecommenderRecommendProductPriceWebUrlRequest.newBuilder();
		if (null != priceWebUrl) {
			SceneProtos.RecommenderPriceWebUrl.Builder priceWebUrlCreateCdnBuilder = SceneProtos.RecommenderPriceWebUrl.newBuilder();
			JsonUtil.PROTOBUF_JSON_FORMAT.merge(priceWebUrl, ExtensionRegistry.getEmptyRegistry(), priceWebUrlCreateCdnBuilder);
			request.setPriceWebUrl(priceWebUrlCreateCdnBuilder.build());
		}
		AdminSceneProtos.UpdateRecommenderRecommendProductPriceWebUrlResponse response = Futures.getUnchecked(this.adminSceneService.updateRecommenderRecommendProductPriceWebUrl(this.adminHeadProvider.get(),
				request.build()));

		httpResponse.setContentType("application/json;charset=UTF-8");
		JsonUtil.PROTOBUF_JSON_FORMAT.print(response, httpResponse.getWriter());
	}

}
