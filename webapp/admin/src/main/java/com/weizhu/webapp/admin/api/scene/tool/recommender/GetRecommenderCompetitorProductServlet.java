package com.weizhu.webapp.admin.api.scene.tool.recommender;

import java.io.IOException;
import java.util.HashMap;
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
import com.weizhu.proto.SceneProtos.RecommenderCompetitorProduct;
import com.weizhu.proto.UploadService;
import com.weizhu.web.ParamUtil;
import com.weizhu.webapp.admin.api.Util;

@Singleton
@SuppressWarnings("serial")
public class GetRecommenderCompetitorProductServlet extends HttpServlet {
	private final Provider<AdminHead> adminHeadProvider;
	private final AdminSceneService adminSceneService;
	private final AdminService adminService;
	private final UploadService uploadService;

	@Inject
	public GetRecommenderCompetitorProductServlet(Provider<AdminHead> adminHeadProvider, AdminSceneService adminSceneService,
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

		Integer categoryId = ParamUtil.getInt(httpRequest, "category_id", null);
		String competitorProductName = ParamUtil.getString(httpRequest, "competitor_product_name", null);
		Integer start = ParamUtil.getInt(httpRequest, "start", null);
		int length = ParamUtil.getInt(httpRequest, "length", 0);

		final AdminHead head = this.adminHeadProvider.get();
		
		AdminSceneProtos.GetRecommenderCompetitorProductRequest.Builder request = AdminSceneProtos.GetRecommenderCompetitorProductRequest.newBuilder();
		request.setLength(length);
		if (categoryId != null) {
			request.setCategoryId(categoryId);
		}
		if (competitorProductName != null) {
			request.setCompetitorProductName(competitorProductName);
		}
		if (start == null) {
			request.setLength(length);
		}
		AdminSceneProtos.GetRecommenderCompetitorProductResponse response = Futures.getUnchecked(this.adminSceneService.getRecommenderCompetitorProduct(head,
				request.build()));
		// 获取admin信息,及其分类相关信息
		Set<Long> adminIds = new TreeSet<Long>();
		for (SceneProtos.RecommenderCompetitorProduct competitorProduct : response.getCompetitorProductList()) {
			adminIds.add(competitorProduct.getCreateAdminId());
			adminIds.add(competitorProduct.getUpdateAdminId());
		}

		AdminProtos.GetAdminByIdResponse adminResponse = Futures.getUnchecked(adminService.getAdminById(head,
				AdminProtos.GetAdminByIdRequest.newBuilder().addAllAdminId(adminIds).build()));

		Map<Long, AdminProtos.Admin> adminMap = Util.getAdminMap(adminResponse.getAdminList());

		Map<Integer, SceneProtos.RecommenderCategory> categoryMap = new HashMap<Integer, SceneProtos.RecommenderCategory>();
		for (SceneProtos.RecommenderCategory category : response.getRefCategoryList()) {
			categoryMap.put(category.getCategoryId(), category);
		}
		
		String imageUrlPrefix = Futures.getUnchecked(this.uploadService.getUploadUrlPrefix(head, ServiceUtil.EMPTY_REQUEST)).getImageUrlPrefix();

		JsonObject result = new JsonObject();
		JsonArray cptorPdtArray = new JsonArray();

		for (RecommenderCompetitorProduct competitorProduct : response.getCompetitorProductList()) {
			JsonObject cptorPdtObj = new JsonObject();
			cptorPdtObj.addProperty("competitor_product_id", competitorProduct.getCompetitorProductId());
			cptorPdtObj.addProperty("competitor_product_name", competitorProduct.getCompetitorProductName());
			cptorPdtObj.addProperty("image_name", competitorProduct.getImageName());
			cptorPdtObj.addProperty("image_url", imageUrlPrefix + competitorProduct.getImageName());
			cptorPdtObj.addProperty("allow_model_id", competitorProduct.getAllowModelId());
			cptorPdtObj.addProperty("state", competitorProduct.getState().name());
			cptorPdtObj.addProperty("create_admin_id", competitorProduct.getCreateAdminId());
			cptorPdtObj.addProperty("create_admin_name", Util.getAdminName(adminMap, competitorProduct.getCreateAdminId()));
			cptorPdtObj.addProperty("create_time", Util.getDate(competitorProduct.getCreateTime()));
			cptorPdtObj.addProperty("update_admin_id", competitorProduct.getUpdateAdminId());
			cptorPdtObj.addProperty("update_admin_name", Util.getAdminName(adminMap, competitorProduct.getUpdateAdminId()));
			cptorPdtObj.addProperty("update_time", Util.getDate(competitorProduct.getUpdateTime()));

			cptorPdtObj.add("category", this.getCategoryJsonObj(categoryMap, competitorProduct.getCategoryId(), imageUrlPrefix));

			cptorPdtArray.add(cptorPdtObj);
		}
		result.add("competitor_product", cptorPdtArray);
		result.addProperty("total_size", response.getTotalSize());
		result.addProperty("filtered_size", response.getFilteredSize());

		httpResponse.setContentType("application/json;charset=UTF-8");
		JsonUtil.GSON.toJson(result, httpResponse.getWriter());
	}

	private JsonObject getCategoryJsonObj(Map<Integer, SceneProtos.RecommenderCategory> categoryMap, int categoryId, String imageUrlPrefix) {
		JsonObject categoryObj = new JsonObject();
		SceneProtos.RecommenderCategory category = categoryMap.get(categoryId);
		if (category != null) {
			categoryObj.addProperty("category_id", category.getCategoryId());
			categoryObj.addProperty("category_name", category.getCategoryName());
			categoryObj.addProperty("category_desc", category.getCategoryDesc());
			categoryObj.addProperty("image_name", category.getImageName());
			categoryObj.addProperty("image_url", imageUrlPrefix + category.getImageName());
			if (category.hasParentCategoryId()) {
				categoryObj.add("parent_category", this.getCategoryJsonObj(categoryMap, category.getParentCategoryId(), imageUrlPrefix));
			}
		}
		return categoryObj;
	}
}
