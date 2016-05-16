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
public class CreateRecommenderCompetitorProductServlet extends HttpServlet {
	private final Provider<AdminHead> adminHeadProvider;
	private final AdminSceneService adminSceneService;

	@Inject
	public CreateRecommenderCompetitorProductServlet(Provider<AdminHead> adminHeadProvider, AdminSceneService adminSceneService) {
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
		String competitorProductName = ParamUtil.getString(httpRequest, "competitor_product_name", "");
		String imageName = ParamUtil.getString(httpRequest, "image_name", "");
		int categoryId = ParamUtil.getInt(httpRequest, "category_id", -1);
		Integer allowModelId = ParamUtil.getInt(httpRequest, "allow_model_id", -1);
		List<Integer> recommendProductIds = ParamUtil.getIntList(httpRequest, "recommend_product_id", Collections.<Integer> emptyList());

		AdminSceneProtos.CreateRecommenderCompetitorProductRequest.Builder request = AdminSceneProtos.CreateRecommenderCompetitorProductRequest.newBuilder()
				.setCompetitorProductName(competitorProductName)
				.setImageName(imageName)
				.setCategoryId(categoryId)
				.addAllRecommendProductId(recommendProductIds);
		if (allowModelId != null) {
			request.setAllowModelId(allowModelId);
		}

		AdminSceneProtos.CreateRecommenderCompetitorProductResponse response = Futures.getUnchecked(this.adminSceneService.createRecommenderCompetitorProduct(this.adminHeadProvider.get(),
				request.build()));

		httpResponse.setContentType("application/json;charset=UTF-8");
		JsonUtil.PROTOBUF_JSON_FORMAT.print(response, httpResponse.getWriter());
	}

}
