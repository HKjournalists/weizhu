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
import com.weizhu.proto.UploadService;
import com.weizhu.web.ParamUtil;
import com.weizhu.webapp.admin.api.Util;

@Singleton
@SuppressWarnings("serial")
public class GetRecommenderRecommendProductPriceWebUrlServlet extends HttpServlet {
	private final Provider<AdminHead> adminHeadProvider;
	private final AdminSceneService adminSceneService;
	private final AdminService adminService;
	private final UploadService uploadService;

	@Inject
	public GetRecommenderRecommendProductPriceWebUrlServlet(Provider<AdminHead> adminHeadProvider, AdminSceneService adminSceneService,
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

		int recommendProductId = ParamUtil.getInt(httpRequest, "recommend_product_id", -1);
		
		final AdminHead adminHead = this.adminHeadProvider.get();

		AdminSceneProtos.GetRecommenderRecommendProductPriceWebUrlRequest.Builder request = AdminSceneProtos.GetRecommenderRecommendProductPriceWebUrlRequest.newBuilder();
		request.setRecommendProductId(recommendProductId);
		
		AdminSceneProtos.GetRecommenderRecommendProductPriceWebUrlResponse response = Futures.getUnchecked(this.adminSceneService.getRecommenderRecommendProductPriceWebUrl(adminHead,
				request.build()));
		// 获取admin信息,及其分类相关信息
		Set<Long> adminIds = new TreeSet<Long>();
		for (SceneProtos.RecommenderPriceWebUrl priceWebUrl : response.getPriceWebUrlList()) {
			adminIds.add(priceWebUrl.getCreateAdminId());
		}
	
		AdminProtos.GetAdminByIdResponse adminResponse = Futures.getUnchecked(adminService.getAdminById(this.adminHeadProvider.get(),
				AdminProtos.GetAdminByIdRequest.newBuilder().addAllAdminId(adminIds).build()));

		Map<Long, AdminProtos.Admin> adminMap = Util.getAdminMap(adminResponse.getAdminList());

		String imageUrlPrefix = Futures.getUnchecked(this.uploadService.getUploadUrlPrefix(adminHead, ServiceUtil.EMPTY_REQUEST)).getImageUrlPrefix();
		
		JsonObject result = new JsonObject();
		JsonArray priceWebUrlArray = new JsonArray();
		
		for (SceneProtos.RecommenderPriceWebUrl priceWebUrl : response.getPriceWebUrlList()) {
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
			
			priceWebUrlArray.add(priceWebUrlObj);
		}
		result.add("price_web_url", priceWebUrlArray);

		httpResponse.setContentType("application/json;charset=UTF-8");
		JsonUtil.GSON.toJson(result, httpResponse.getWriter());
	}

}
