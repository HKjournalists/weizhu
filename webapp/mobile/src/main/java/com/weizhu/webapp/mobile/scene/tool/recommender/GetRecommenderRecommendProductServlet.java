package com.weizhu.webapp.mobile.scene.tool.recommender;

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
import com.weizhu.proto.SceneProtos;
import com.weizhu.proto.SceneProtos.RecommenderPriceWebUrl;
import com.weizhu.proto.SceneProtos.RecommenderRecommendProduct;
import com.weizhu.proto.SceneService;
import com.weizhu.proto.UploadService;
import com.weizhu.proto.UserService;
import com.weizhu.proto.WeizhuProtos.RequestHead;
import com.weizhu.web.ParamUtil;
import com.weizhu.webapp.mobile.MobileServletUtil;

@Singleton
@SuppressWarnings("serial")
public class GetRecommenderRecommendProductServlet extends HttpServlet {
	private final Provider<RequestHead> requestHeadProvider;
	private final SceneService sceneService;
	@SuppressWarnings("unused")
	private final UserService userService;
	private final UploadService uploadService;

	@Inject
	public GetRecommenderRecommendProductServlet(Provider<RequestHead> requestHeadProvider, SceneService sceneService, UserService userService,
			UploadService uploadService) {
		this.requestHeadProvider = requestHeadProvider;
		this.sceneService = sceneService;
		this.userService = userService;
		this.uploadService = uploadService;
	}

	@Override
	protected void doGet(HttpServletRequest httpRequest, HttpServletResponse httpResponse) throws ServletException, IOException {
		this.doPost(httpRequest, httpResponse);
	}

	@Override
	protected void doPost(HttpServletRequest httpRequest, HttpServletResponse httpResponse) throws ServletException, IOException {

		final int competitorProductId = ParamUtil.getInt(httpRequest, "competitor_product_id", -1);
		
		final RequestHead head = requestHeadProvider.get();
		
		SceneProtos.GetRecommenderRecommendProductRequest request = SceneProtos.GetRecommenderRecommendProductRequest.newBuilder()
				.setCompetitorProductId(competitorProductId)
				.build();

		SceneProtos.GetRecommenderRecommendProductResponse response = Futures.getUnchecked(this.sceneService.getRecommenderRecommendProduct(head,
				request));
		String imageUrlPrefix = Futures.getUnchecked(this.uploadService.getUploadUrlPrefix(head, ServiceUtil.EMPTY_REQUEST)).getImageUrlPrefix();

		JsonObject result = new JsonObject();
		JsonArray cptorPdtArray = new JsonArray();

		for (RecommenderRecommendProduct recommendProduct : response.getRecommendProductList()) {
			JsonObject rmdPdtObj = new JsonObject();
			rmdPdtObj.addProperty("recommend_product_id", recommendProduct.getRecommendProductId());
			rmdPdtObj.addProperty("recommend_product_name", recommendProduct.getRecommendProductName());
			rmdPdtObj.addProperty("recommend_product_desc", recommendProduct.getRecommendProductDesc());
			rmdPdtObj.addProperty("image_name", recommendProduct.getImageName());
			rmdPdtObj.addProperty("allow_model_id", recommendProduct.getAllowModelId());
			rmdPdtObj.addProperty("state", recommendProduct.getState().name());
			rmdPdtObj.addProperty("create_admin_id", recommendProduct.getCreateAdminId());
			rmdPdtObj.addProperty("create_time", MobileServletUtil.getDate(recommendProduct.getCreateTime()));
			rmdPdtObj.addProperty("update_admin_id", recommendProduct.getUpdateAdminId());
			rmdPdtObj.addProperty("update_time", MobileServletUtil.getDate(recommendProduct.getUpdateTime()));
		
			JsonArray priceWebUrlArray = new JsonArray();
			for(RecommenderPriceWebUrl priceWebUrl : recommendProduct.getPriceWebUrlList()){
				JsonObject priceWebUrlObj = new JsonObject();
				priceWebUrlObj.addProperty("url_id", priceWebUrl.getUrlId());
				priceWebUrlObj.addProperty("recommend_product_id", priceWebUrl.getRecommendProductId());
				priceWebUrlObj.addProperty("url_name", priceWebUrl.getUrlName());
				priceWebUrlObj.addProperty("url_content", priceWebUrl.getUrlContent());
				priceWebUrlObj.addProperty("image_name", priceWebUrl.getImageName());
				priceWebUrlObj.addProperty("image_url", imageUrlPrefix + priceWebUrl.getImageName());
				priceWebUrlObj.addProperty("is_weizhu", priceWebUrl.getIsWeizhu());
				priceWebUrlObj.addProperty("create_admin_id", priceWebUrl.getCreateAdminId());
				priceWebUrlObj.addProperty("create_time", MobileServletUtil.getDate(priceWebUrl.getCreateTime()));
			}
			rmdPdtObj.add("price_web_url", priceWebUrlArray);
			
			String content;
			if (recommendProduct.hasWebUrl()) {
				content = JsonUtil.PROTOBUF_JSON_FORMAT.printToString(recommendProduct.getWebUrl());
			} else if (recommendProduct.hasDocument()) {
				content = JsonUtil.PROTOBUF_JSON_FORMAT.printToString(recommendProduct.getDocument());
			} else if (recommendProduct.hasVideo()) {
				content = JsonUtil.PROTOBUF_JSON_FORMAT.printToString(recommendProduct.getVideo());
			} else if (recommendProduct.hasAudio()) {
				content = JsonUtil.PROTOBUF_JSON_FORMAT.printToString(recommendProduct.getAudio());
			} else if (recommendProduct.hasAppUri()) {
				content = JsonUtil.PROTOBUF_JSON_FORMAT.printToString(recommendProduct.getAppUri());
			} else {
				content = "";
			}
			
			rmdPdtObj.addProperty("content", content);
			
			cptorPdtArray.add(rmdPdtObj);
		}
		result.add("recommend_product", cptorPdtArray);

		httpResponse.setContentType("application/json;charset=UTF-8");
		JsonUtil.GSON.toJson(result, httpResponse.getWriter());
	}
}
