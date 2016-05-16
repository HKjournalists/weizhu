package com.weizhu.webapp.admin.api.scene.tool.recommender;

import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

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
import com.weizhu.proto.AdminProtos.AdminHead;
import com.weizhu.proto.AdminProtos;
import com.weizhu.proto.AdminSceneProtos;
import com.weizhu.proto.AdminSceneService;
import com.weizhu.proto.AdminService;
import com.weizhu.proto.SceneProtos;
import com.weizhu.proto.SceneProtos.RecommenderPriceWebUrl;
import com.weizhu.proto.SceneProtos.RecommenderRecommendProduct;
import com.weizhu.proto.UploadService;
import com.weizhu.web.ParamUtil;
import com.weizhu.webapp.admin.api.Util;

@Singleton
@SuppressWarnings("serial")
public class GetRecommenderRecommendProductServlet extends HttpServlet {
	private final Provider<AdminHead> adminHeadProvider;
	private final AdminSceneService adminSceneService;
	private final AdminService adminService;
	private final UploadService uploadService;

	@Inject
	public GetRecommenderRecommendProductServlet(Provider<AdminHead> adminHeadProvider, AdminSceneService adminSceneService,
			AdminService adminService, UploadService uploadService) {
		this.adminHeadProvider = adminHeadProvider;
		this.adminSceneService = adminSceneService;
		this.adminService = adminService;
		this.uploadService = uploadService;
	}

	@Override
	protected void doGet(HttpServletRequest httpRequest, HttpServletResponse httpResponse) throws ServletException, IOException {
		this.doPost(httpRequest, httpResponse);
	}

	@Override
	protected void doPost(HttpServletRequest httpRequest, HttpServletResponse httpResponse) throws ServletException, IOException {

		// 1. 取出参数

		Integer competitorProductId = ParamUtil.getInt(httpRequest, "competitor_product_id", null);
		String recommendProductName = ParamUtil.getString(httpRequest, "recommend_product_name", null);
		Integer start = ParamUtil.getInt(httpRequest, "start", null);
		int length = ParamUtil.getInt(httpRequest, "length", 0);

		final AdminHead head = adminHeadProvider.get();
		
		AdminSceneProtos.GetRecommenderRecommendProductRequest.Builder request = AdminSceneProtos.GetRecommenderRecommendProductRequest.newBuilder();
		request.setLength(length);
		if (competitorProductId != null) {
			request.setCompetitorProductId(competitorProductId);
		}
		if (recommendProductName != null) {
			request.setRecommendProductName(recommendProductName);
		}
		if (start == null) {
			request.setLength(length);
		}
		AdminSceneProtos.GetRecommenderRecommendProductResponse response = Futures.getUnchecked(this.adminSceneService.getRecommenderRecommendProduct(head,
				request.build()));
		// 获取admin信息,及其分类相关信息
		Set<Long> adminIds = new TreeSet<Long>();
		for (SceneProtos.RecommenderRecommendProduct recommendProduct : response.getRecommendProductList()) {
			adminIds.add(recommendProduct.getCreateAdminId());
			adminIds.add(recommendProduct.getUpdateAdminId());
		}

		AdminProtos.GetAdminByIdResponse adminResponse = Futures.getUnchecked(adminService.getAdminById(this.adminHeadProvider.get(),
				AdminProtos.GetAdminByIdRequest.newBuilder().addAllAdminId(adminIds).build()));

		Map<Long, AdminProtos.Admin> adminMap = Util.getAdminMap(adminResponse.getAdminList());

		String imageUrlPrefix = Futures.getUnchecked(this.uploadService.getUploadUrlPrefix(head, ServiceUtil.EMPTY_REQUEST)).getImageUrlPrefix();

		JsonObject result = new JsonObject();
		JsonArray rmdPdtArray = new JsonArray();

		for (RecommenderRecommendProduct recommendProduct : response.getRecommendProductList()) {
			JsonObject rmdPdtObj = new JsonObject();
			rmdPdtObj.addProperty("recommend_product_id", recommendProduct.getRecommendProductId());
			rmdPdtObj.addProperty("recommend_product_name", recommendProduct.getRecommendProductName());
			rmdPdtObj.addProperty("recommend_product_desc", recommendProduct.getRecommendProductDesc());
			rmdPdtObj.addProperty("image_name", recommendProduct.getImageName());
			rmdPdtObj.addProperty("image_url", imageUrlPrefix + recommendProduct.getImageName());
			rmdPdtObj.addProperty("allow_model_id", recommendProduct.getAllowModelId());
			rmdPdtObj.addProperty("state", recommendProduct.getState().name());
			rmdPdtObj.addProperty("create_admin_id", recommendProduct.getCreateAdminId());
			rmdPdtObj.addProperty("create_admin_name", Util.getAdminName(adminMap, recommendProduct.getCreateAdminId()));
			rmdPdtObj.addProperty("create_time", Util.getDate(recommendProduct.getCreateTime()));
			rmdPdtObj.addProperty("update_admin_id", recommendProduct.getUpdateAdminId());
			rmdPdtObj.addProperty("update_admin_name", Util.getAdminName(adminMap, recommendProduct.getUpdateAdminId()));
			rmdPdtObj.addProperty("update_time", Util.getDate(recommendProduct.getUpdateTime()));

			JsonArray priceWebUrlArray = new JsonArray();
			for (RecommenderPriceWebUrl priceWebUrl : recommendProduct.getPriceWebUrlList()) {
				JsonObject priceWebUrlObj = new JsonObject();
				priceWebUrlObj.addProperty("url_id", priceWebUrl.getUrlId());
				priceWebUrlObj.addProperty("recommend_product_id", priceWebUrl.getRecommendProductId());
				priceWebUrlObj.addProperty("url_name", priceWebUrl.getUrlName());
				priceWebUrlObj.addProperty("url_content", priceWebUrl.getUrlContent());
				priceWebUrlObj.addProperty("image_name", priceWebUrl.getImageName());
				priceWebUrlObj.addProperty("image_url", imageUrlPrefix + priceWebUrl.getImageName());
				priceWebUrlObj.addProperty("is_weizhu", priceWebUrl.getIsWeizhu());
				priceWebUrlObj.addProperty("create_admin_id", priceWebUrl.getCreateAdminId());
				priceWebUrlObj.addProperty("create_admin_name", Util.getAdminName(adminMap, priceWebUrl.getCreateAdminId()));
				priceWebUrlObj.addProperty("create_time", Util.getDate(priceWebUrl.getCreateTime()));
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

			rmdPdtArray.add(rmdPdtObj);
		}
		result.add("recommend_product", rmdPdtArray);
		result.addProperty("total_size", response.getTotalSize());
		result.addProperty("filtered_size", response.getFilteredSize());

		httpResponse.setContentType("application/json;charset=UTF-8");
		JsonUtil.GSON.toJson(result, httpResponse.getWriter());
	}
}
