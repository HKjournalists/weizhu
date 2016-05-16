package com.weizhu.webapp.mobile.scene.tool.recommender;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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
import com.weizhu.proto.SceneProtos;
import com.weizhu.proto.SceneService;
import com.weizhu.proto.UploadService;
import com.weizhu.proto.UserService;
import com.weizhu.proto.WeizhuProtos.EmptyRequest;
import com.weizhu.proto.WeizhuProtos.RequestHead;
import com.weizhu.webapp.mobile.MobileServletUtil;

@Singleton
@SuppressWarnings("serial")
public class GetRecommenderHomeServlet extends HttpServlet {
	private final Provider<RequestHead> requestHeadProvider;
	private final SceneService sceneService;
	@SuppressWarnings("unused")
	private final UserService userService;
	private final UploadService uploadService;

	@Inject
	public GetRecommenderHomeServlet(Provider<RequestHead> requestHeadProvider, SceneService sceneService, UserService userService,
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

		final RequestHead head = requestHeadProvider.get();
		SceneProtos.GetRecommenderHomeResponse response = Futures.getUnchecked(this.sceneService.getRecommenderHome(head, EmptyRequest.newBuilder().build()));

		Map<Integer, List<Integer>> categoryIdSubCategoryIdListMap = new HashMap<Integer, List<Integer>>();
		List<Integer> rootCategoryIdList = new ArrayList<Integer>();
		Map<Integer, SceneProtos.RecommenderCategory> categoryMap = new HashMap<Integer, SceneProtos.RecommenderCategory>();

		// 获取admin信息,及其分类相关信息
		Set<Long> adminIds = new TreeSet<Long>();
		for (SceneProtos.RecommenderCategory category : response.getCategoryList()) {
			adminIds.add(category.getCreateAdminId());
			adminIds.add(category.getUpdateAdminId());

			if (!category.hasParentCategoryId()) {
				rootCategoryIdList.add(category.getCategoryId());
			} else {
				List<Integer> subCategoryIdList = categoryIdSubCategoryIdListMap.get(category.getParentCategoryId());
				if (subCategoryIdList == null) {
					subCategoryIdList = new ArrayList<Integer>();
					categoryIdSubCategoryIdListMap.put(category.getParentCategoryId(), subCategoryIdList);
				}
				subCategoryIdList.add(category.getCategoryId());
			}

			categoryMap.put(category.getCategoryId(), category);
		}

		String imageUrlPrefix = Futures.getUnchecked(this.uploadService.getUploadUrlPrefix(head, ServiceUtil.EMPTY_REQUEST)).getImageUrlPrefix();
		
		JsonObject result = new JsonObject();
		JsonArray categoryArray = new JsonArray();

		for (Integer categoryId : rootCategoryIdList) {
			SceneProtos.RecommenderCategory category = categoryMap.get(categoryId);
			if (category == null) {
				continue;
			}
			categoryArray.add(this.getCategoryObj(category, categoryIdSubCategoryIdListMap, categoryMap, imageUrlPrefix));
		}

		result.add("category", categoryArray);
		httpResponse.setContentType("application/json;charset=UTF-8");
		JsonUtil.GSON.toJson(result, httpResponse.getWriter());
	}

	private JsonObject getCategoryObj(SceneProtos.RecommenderCategory category, 
			Map<Integer, List<Integer>> categoryIdSubCategoryIdListMap, Map<Integer, SceneProtos.RecommenderCategory> categoryMap, 
			String imageUrlPrefix) {
		JsonObject categoryObj = new JsonObject();
		categoryObj.addProperty("category_id", category.getCategoryId());
		categoryObj.addProperty("category_name", category.getCategoryName());
		categoryObj.addProperty("image_name", category.getImageName());
		categoryObj.addProperty("image_url", imageUrlPrefix + category.getImageName());
		categoryObj.addProperty("category_desc", category.getCategoryDesc());
		categoryObj.addProperty("parent_category_id", category.getParentCategoryId());
		categoryObj.addProperty("is_leaf_category", category.getIsLeafCategory());
		categoryObj.addProperty("state", category.getState().name());
		categoryObj.addProperty("create_admin_id", category.getCreateAdminId());
		categoryObj.addProperty("create_time", MobileServletUtil.getDate(category.getCreateTime()));
		categoryObj.addProperty("update_admin_id", category.getUpdateAdminId());
		categoryObj.addProperty("update_time", MobileServletUtil.getDate(category.getUpdateTime()));
		
		// 叶子节点下有可能存在已经作废的子节点
		//		if (!scene.getIsLeafScene()) {
		List<Integer> subCategoryIdList = categoryIdSubCategoryIdListMap.get(category.getCategoryId());
		if (subCategoryIdList != null) {
			JsonArray childrenCategoryArray = new JsonArray();
			for (Integer categoryId : subCategoryIdList) {
				SceneProtos.RecommenderCategory tmpCategory = categoryMap.get(categoryId);
				if (tmpCategory == null) {
					continue;
				}
				childrenCategoryArray.add(this.getCategoryObj(tmpCategory, categoryIdSubCategoryIdListMap, categoryMap, imageUrlPrefix));
			}
			categoryObj.add("children_category", childrenCategoryArray);
		}
		//		}
		return categoryObj;
	}
}
