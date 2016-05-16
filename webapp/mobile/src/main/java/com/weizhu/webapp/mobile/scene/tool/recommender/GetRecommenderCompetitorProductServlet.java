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
import com.google.protobuf.ByteString;
import com.weizhu.common.service.ServiceUtil;
import com.weizhu.common.utils.HexUtil;
import com.weizhu.common.utils.JsonUtil;
import com.weizhu.proto.SceneProtos;
import com.weizhu.proto.SceneService;
import com.weizhu.proto.UploadService;
import com.weizhu.proto.UserService;
import com.weizhu.proto.SceneProtos.RecommenderCompetitorProduct;
import com.weizhu.proto.WeizhuProtos.RequestHead;
import com.weizhu.web.ParamUtil;
import com.weizhu.webapp.mobile.MobileServletUtil;

@Singleton
@SuppressWarnings("serial")
public class GetRecommenderCompetitorProductServlet extends HttpServlet {
	private final Provider<RequestHead> requestHeadProvider;
	private final SceneService sceneService;
	@SuppressWarnings("unused")
	private final UserService userService;
	private final UploadService uploadService;

	@Inject
	public GetRecommenderCompetitorProductServlet(Provider<RequestHead> requestHeadProvider, SceneService sceneService, UserService userService,
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

		final Integer categoryId = ParamUtil.getInt(httpRequest, "category_id", null);
		final String competitorProductName = ParamUtil.getString(httpRequest, "competitor_product_name", null);
		final String offsetIndex = ParamUtil.getString(httpRequest, "offset_index", null);
		final int size = ParamUtil.getInt(httpRequest, "size", 0);
		
		final RequestHead head = this.requestHeadProvider.get();
		
		SceneProtos.GetRecommenderCompetitorProductRequest.Builder request = SceneProtos.GetRecommenderCompetitorProductRequest.newBuilder();
		request.setSize(size);
		if (null != categoryId) {
			request.setCategoryId(categoryId);
		}
		if (null != competitorProductName) {
			request.setCompetitorProductName(competitorProductName);
		}
		if (null != offsetIndex) {
			request.setOffsetIndex(ByteString.copyFrom(HexUtil.hex2bin(offsetIndex)));
		}
		SceneProtos.GetRecommenderCompetitorProductResponse response = Futures.getUnchecked(this.sceneService.getRecommenderCompetitorProduct(head,
				request.build()));
		String imageUrlPrefix = Futures.getUnchecked(this.uploadService.getUploadUrlPrefix(head, ServiceUtil.EMPTY_REQUEST)).getImageUrlPrefix();
		
		JsonObject result = new JsonObject();
		JsonArray cptorPdtArray = new JsonArray();
		
		for(RecommenderCompetitorProduct competitorProduct : response.getCompetitorProductList()){
			JsonObject cptorPdtObj = new JsonObject();
			cptorPdtObj.addProperty("competitor_product_id", competitorProduct.getCompetitorProductId());
			cptorPdtObj.addProperty("competitor_product_name", competitorProduct.getCompetitorProductName());
			cptorPdtObj.addProperty("image_name", competitorProduct.getImageName());
			cptorPdtObj.addProperty("image_url", imageUrlPrefix + competitorProduct.getImageName());
			cptorPdtObj.addProperty("category_id", competitorProduct.getCategoryId());
			cptorPdtObj.addProperty("allow_model_id", competitorProduct.getAllowModelId());
			cptorPdtObj.addProperty("state", competitorProduct.getState().name());
			cptorPdtObj.addProperty("create_admin_id", competitorProduct.getCreateAdminId());
			cptorPdtObj.addProperty("create_time", MobileServletUtil.getDate(competitorProduct.getCreateTime()));
			cptorPdtObj.addProperty("update_admin_id", competitorProduct.getUpdateAdminId());
			cptorPdtObj.addProperty("update_time", MobileServletUtil.getDate(competitorProduct.getUpdateTime()));
			
			cptorPdtArray.add(cptorPdtObj);
		}
		result.add("competitor_product", cptorPdtArray);
		result.addProperty("has_more", response.getHasMore());
		result.addProperty("offset_index", HexUtil.bin2Hex(response.getOffsetIndex().toByteArray()));

		httpResponse.setContentType("application/json;charset=UTF-8");
		JsonUtil.GSON.toJson(result, httpResponse.getWriter());
	}
}
