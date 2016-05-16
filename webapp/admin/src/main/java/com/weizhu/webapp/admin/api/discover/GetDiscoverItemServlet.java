package com.weizhu.webapp.admin.api.discover;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
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
import com.weizhu.proto.AdminDiscoverProtos;
import com.weizhu.proto.AdminDiscoverService;
import com.weizhu.proto.AdminProtos;
import com.weizhu.proto.AdminService;
import com.weizhu.proto.AllowProtos;
import com.weizhu.proto.AllowService;
import com.weizhu.proto.DiscoverV2Protos;
import com.weizhu.proto.UploadService;
import com.weizhu.proto.AdminProtos.AdminHead;
import com.weizhu.web.ParamUtil;

@Singleton
@SuppressWarnings("serial")
public class GetDiscoverItemServlet extends HttpServlet {
	
	private final Provider<AdminHead> adminHeadProvider;
	private final AdminDiscoverService adminDiscoverService;
	private final AdminService adminService;
	private final UploadService uploadService;
	private final AllowService allowService;

	@Inject
	public GetDiscoverItemServlet(Provider<AdminHead> adminHeadProvider, 
			AdminDiscoverService adminDiscoverService, 
			AdminService adminService,
			UploadService uploadService, 
			AllowService allowService
			) {
		this.adminHeadProvider = adminHeadProvider;
		this.adminDiscoverService = adminDiscoverService;
		this.adminService = adminService;
		this.uploadService = uploadService;
		this.allowService = allowService;
	}

	@Override
	protected void doGet(HttpServletRequest httpRequest, HttpServletResponse httpResponse) throws ServletException, IOException {
		this.doPost(httpRequest, httpResponse);
	}

	@Override
	protected void doPost(HttpServletRequest httpRequest, HttpServletResponse httpResponse) throws ServletException, IOException {
		int start = ParamUtil.getInt(httpRequest, "start", 0);
		int length = ParamUtil.getInt(httpRequest, "length", 0);
		Integer categoryId = ParamUtil.getInt(httpRequest, "category_id", null);
		String itemName = ParamUtil.getString(httpRequest, "item_name", null);

		final AdminHead head = this.adminHeadProvider.get();

		AdminDiscoverProtos.GetItemListRequest.Builder requestBuilder = AdminDiscoverProtos.GetItemListRequest.newBuilder()
				.setStart(start)
				.setLength(length);
		if (categoryId != null) {
			requestBuilder.setCategoryId(categoryId);
		}
		if (itemName != null) {
			requestBuilder.setItemName(itemName);
		}

		AdminDiscoverProtos.GetItemListResponse response = Futures.getUnchecked(this.adminDiscoverService.getItemList(head, requestBuilder.build()));
		
		Map<Long, List<Integer>> itemCategoryIdMap = new TreeMap<Long, List<Integer>>();
		for (AdminDiscoverProtos.ItemCategory itemCategory : response.getRefItemCategoryList()) {
			itemCategoryIdMap.put(itemCategory.getItemId(), itemCategory.getCategoryIdList());
		}
		Map<Integer, DiscoverV2Protos.Module.Category> refCategoryMap = new TreeMap<Integer, DiscoverV2Protos.Module.Category>();
		for (DiscoverV2Protos.Module.Category category : response.getRefCategoryList()) {
			refCategoryMap.put(category.getCategoryId(), category);
		}
		Map<Integer, DiscoverV2Protos.Module> refModuleMap = new TreeMap<Integer, DiscoverV2Protos.Module>();
		for (DiscoverV2Protos.Module module : response.getRefModuleList()) {
			refModuleMap.put(module.getModuleId(), module);
		}

		// 获取admin信息,和allowModel信息
		Set<Long> adminIdSet = new TreeSet<Long>();
		Set<Integer> allowModelIdSet = new TreeSet<Integer>();
		for (DiscoverV2Protos.Item item : response.getItemList()) {
			if (item.getBase().hasCreateAdminId()) {
				adminIdSet.add(item.getBase().getCreateAdminId());
			}
			if (item.getBase().hasUpdateAdminId()) {
				adminIdSet.add(item.getBase().getUpdateAdminId());
			}
			if (item.getBase().hasAllowModelId()) {
				allowModelIdSet.add(item.getBase().getAllowModelId());
			}
		}
		
		final Map<Long, AdminProtos.Admin> refAdminMap = DiscoverServletUtil.getAdminMap(adminService, head, adminIdSet);
		final Map<Integer, AllowProtos.Model> refAllowModelMap = DiscoverServletUtil.getAllowModelMap(allowService, head, allowModelIdSet);
		final String imageUrlPrefix = Futures.getUnchecked(this.uploadService.getUploadUrlPrefix(head, ServiceUtil.EMPTY_REQUEST)).getImageUrlPrefix();
		
		JsonArray itemArray = new JsonArray();
		for (DiscoverV2Protos.Item item : response.getItemList()) {
			JsonObject itemObj = new JsonObject();
			// base
			JsonObject itemBaseObj = new JsonObject();
			itemBaseObj.addProperty("item_id", item.getBase().getItemId());
			itemBaseObj.addProperty("item_name", item.getBase().getItemName());
			itemBaseObj.addProperty("item_desc", item.getBase().getItemDesc());
			itemBaseObj.addProperty("image_name", item.getBase().getImageName());
			itemBaseObj.addProperty("image_url", imageUrlPrefix + item.getBase().getImageName());
			itemBaseObj.addProperty("enable_comment", item.getBase().getEnableComment());
			itemBaseObj.addProperty("enable_score", item.getBase().getEnableScore());
			itemBaseObj.addProperty("enable_remind", item.getBase().getEnableRemind());
			itemBaseObj.addProperty("enable_like", item.getBase().getEnableLike());
			itemBaseObj.addProperty("enable_share", item.getBase().getEnableShare());
			itemBaseObj.addProperty("enable_external_share", item.getBase().getEnableExternalShare());
			if (item.getBase().hasAllowModelId()) {
				itemBaseObj.addProperty("allow_model_id", item.getBase().getAllowModelId());
				itemBaseObj.addProperty("allow_model_name", DiscoverServletUtil.getAllowModelName(refAllowModelMap, item.getBase().getAllowModelId()));
			} else {
				itemBaseObj.addProperty("allow_model_id", "");
				itemBaseObj.addProperty("allow_model_name", "");
			}

			String content;
			if (item.getBase().hasWebUrl()) {
				content = JsonUtil.PROTOBUF_JSON_FORMAT.printToString(item.getBase().getWebUrl());
			} else if (item.getBase().hasDocument()) {
				content = JsonUtil.PROTOBUF_JSON_FORMAT.printToString(item.getBase().getDocument());
			} else if (item.getBase().hasVideo()) {
				content = JsonUtil.PROTOBUF_JSON_FORMAT.printToString(item.getBase().getVideo());
			} else if (item.getBase().hasAudio()) {
				content = JsonUtil.PROTOBUF_JSON_FORMAT.printToString(item.getBase().getAudio());
			} else if (item.getBase().hasAppUri()) {
				content = JsonUtil.PROTOBUF_JSON_FORMAT.printToString(item.getBase().getAppUri());
			} else {
				content = "";
			}
			itemBaseObj.addProperty("content", content);

			itemBaseObj.addProperty("state", item.getBase().getState().name());
			itemBaseObj.addProperty("create_admin_id", item.getBase().getCreateAdminId());
			itemBaseObj.addProperty("create_admin_name", DiscoverServletUtil.getAdminName(refAdminMap, item.getBase().hasCreateAdminId(), item.getBase().getCreateAdminId()));
			itemBaseObj.addProperty("create_time", DiscoverServletUtil.getDateStr(item.getBase().hasCreateTime(), item.getBase().getCreateTime()));
			itemBaseObj.addProperty("update_admin_id", item.getBase().getUpdateAdminId());
			itemBaseObj.addProperty("update_admin_name", DiscoverServletUtil.getAdminName(refAdminMap, item.getBase().hasUpdateAdminId(), item.getBase().getUpdateAdminId()));
			itemBaseObj.addProperty("update_time", DiscoverServletUtil.getDateStr(item.getBase().hasUpdateTime(), item.getBase().getUpdateTime()));

			itemObj.add("base", itemBaseObj);

			// count
			itemObj.addProperty("count", JsonUtil.PROTOBUF_JSON_FORMAT.printToString(item.getCount()));

			// item category
			JsonArray categoryArray = new JsonArray();
			List<Integer> categoryIdList = itemCategoryIdMap.get(item.getBase().getItemId());
			if (categoryIdList != null) {
				for (Integer catId : categoryIdList) {
					DiscoverV2Protos.Module.Category category = refCategoryMap.get(catId);
					DiscoverV2Protos.Module module = category == null ? null : refModuleMap.get(category.getModuleId());
					if (category != null && module != null) {
						JsonObject categoryObj = new JsonObject();
						categoryObj.addProperty("category_id", category.getCategoryId());
						categoryObj.addProperty("category_name", category.getCategoryName());
	
						JsonObject moduleObj = new JsonObject();
						moduleObj.addProperty("module_id", module.getModuleId());
						moduleObj.addProperty("module_name", module.getModuleName());
						moduleObj.addProperty("image_name", module.getImageName());
						moduleObj.addProperty("image_url", imageUrlPrefix + module.getImageName());
						categoryObj.add("module", moduleObj);
						
						categoryArray.add(categoryObj);
					}
				}
			}
			
			itemObj.add("category", categoryArray);
			
			itemArray.add(itemObj);
		}
		
		JsonObject resultObj = new JsonObject();
		resultObj.add("item", itemArray);
		resultObj.addProperty("total_size", response.getTotalSize());
		resultObj.addProperty("filtered_size", response.getFilteredSize());
		httpResponse.setContentType("application/json;charset=UTF-8");
		JsonUtil.GSON.toJson(resultObj, httpResponse.getWriter());
	}
}
