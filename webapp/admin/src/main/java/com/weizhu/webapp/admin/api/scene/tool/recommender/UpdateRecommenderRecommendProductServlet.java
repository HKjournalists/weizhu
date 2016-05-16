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
import com.google.protobuf.ExtensionRegistry;
import com.weizhu.proto.AdminProtos.AdminHead;
import com.weizhu.common.utils.JsonUtil;
import com.weizhu.proto.AdminSceneProtos;
import com.weizhu.proto.AdminSceneService;
import com.weizhu.proto.DiscoverV2Protos;
import com.weizhu.web.ParamUtil;

@Singleton
@SuppressWarnings("serial")
public class UpdateRecommenderRecommendProductServlet extends HttpServlet {
	private final Provider<AdminHead> adminHeadProvider;
	private final AdminSceneService adminSceneService;

	@Inject
	public UpdateRecommenderRecommendProductServlet(Provider<AdminHead> adminHeadProvider, AdminSceneService adminSceneService) {
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
		String recommendProductName = ParamUtil.getString(httpRequest, "recommend_product_name", "");
		String imageName = ParamUtil.getString(httpRequest, "image_name", "");
		String recommendProductDesc = ParamUtil.getString(httpRequest, "recommend_product_desc", "");
		String webUrl = ParamUtil.getString(httpRequest, "web_url", null);
		String document = ParamUtil.getString(httpRequest, "document", null);
		String video = ParamUtil.getString(httpRequest, "video", null);
		String audio = ParamUtil.getString(httpRequest, "audio", null);
		String appUri = ParamUtil.getString(httpRequest, "app_uri", null);

		Integer allowModelId = ParamUtil.getInt(httpRequest, "allow_model_id", -1);
		List<String> priceWebUrlCreateConditions = ParamUtil.getStringList(httpRequest,
				"price_web_url_create_condition",
				Collections.<String> emptyList());

		AdminSceneProtos.UpdateRecommenderRecommendProductRequest.Builder request = AdminSceneProtos.UpdateRecommenderRecommendProductRequest.newBuilder()
				.setRecommendProductName(recommendProductName)
				.setImageName(imageName)
				.setRecommendProductDesc(recommendProductDesc);

		if (null != allowModelId) {
			request.setAllowModelId(allowModelId);
		}
		if (null != webUrl) {
			DiscoverV2Protos.WebUrl.Builder webUrlBuilder = DiscoverV2Protos.WebUrl.newBuilder();
			JsonUtil.PROTOBUF_JSON_FORMAT.merge(webUrl, ExtensionRegistry.getEmptyRegistry(), webUrlBuilder);
			request.setWebUrl(webUrlBuilder.build());
		}
		if (null != document) {
			DiscoverV2Protos.Document.Builder documentBuilder = DiscoverV2Protos.Document.newBuilder();
			JsonUtil.PROTOBUF_JSON_FORMAT.merge(document, ExtensionRegistry.getEmptyRegistry(), documentBuilder);
			request.setDocument(documentBuilder.build());
		}
		if (null != video) {
			DiscoverV2Protos.Video.Builder videoBuilder = DiscoverV2Protos.Video.newBuilder();
			JsonUtil.PROTOBUF_JSON_FORMAT.merge(video, ExtensionRegistry.getEmptyRegistry(), videoBuilder);
			request.setVideo(videoBuilder.build());
		}
		if (null != audio) {
			DiscoverV2Protos.Audio.Builder audioBuilder = DiscoverV2Protos.Audio.newBuilder();
			JsonUtil.PROTOBUF_JSON_FORMAT.merge(audio, ExtensionRegistry.getEmptyRegistry(), audioBuilder);
			request.setAudio(audioBuilder.build());
		}
		if (null != appUri) {
			DiscoverV2Protos.AppUri.Builder appUriBuilder = DiscoverV2Protos.AppUri.newBuilder();
			JsonUtil.PROTOBUF_JSON_FORMAT.merge(appUri, ExtensionRegistry.getEmptyRegistry(), appUriBuilder);
			request.setAppUri(appUriBuilder.build());
		}

		if (!priceWebUrlCreateConditions.isEmpty()) {

			for (String priceWebUrlCreateCondition : priceWebUrlCreateConditions) {
				AdminSceneProtos.PriceWebUrlCreateCondition.Builder priceWebUrlCreateCdtnBuilder = AdminSceneProtos.PriceWebUrlCreateCondition.newBuilder();
				JsonUtil.PROTOBUF_JSON_FORMAT.merge(priceWebUrlCreateCondition, ExtensionRegistry.getEmptyRegistry(), priceWebUrlCreateCdtnBuilder);
				request.addPriceWebUrlCreateCondition(priceWebUrlCreateCdtnBuilder.build());
			}
		}

		AdminSceneProtos.UpdateRecommenderRecommendProductResponse response = Futures.getUnchecked(this.adminSceneService.updateRecommenderRecommendProduct(this.adminHeadProvider.get(),
				request.build()));

		httpResponse.setContentType("application/json;charset=UTF-8");
		JsonUtil.PROTOBUF_JSON_FORMAT.print(response, httpResponse.getWriter());
	}

}
