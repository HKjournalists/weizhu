package com.weizhu.webapp.admin.api.scene;

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
import com.weizhu.proto.UploadService;
import com.weizhu.web.ParamUtil;
import com.weizhu.webapp.admin.api.Util;

@Singleton
@SuppressWarnings("serial")
public class GetSceneItemServlet extends HttpServlet {
	private final Provider<AdminHead> adminHeadProvider;
	private final AdminSceneService adminSceneService;
	private final AdminService adminService;
	private final UploadService uploadService;

	@Inject
	public GetSceneItemServlet(Provider<AdminHead> adminHeadProvider, AdminSceneService adminSceneService, AdminService adminService,
			UploadService uploadService) {
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
		Integer sceneId = ParamUtil.getInt(httpRequest, "scene_id", null);
		Integer start = ParamUtil.getInt(httpRequest, "start", null);
		Integer length = ParamUtil.getInt(httpRequest, "length", null);
		String itemTitle = ParamUtil.getString(httpRequest, "item_title", null);
		
		final AdminHead head = this.adminHeadProvider.get();

		AdminSceneProtos.GetSceneItemRequest.Builder request = AdminSceneProtos.GetSceneItemRequest.newBuilder();
		if (sceneId != null) {
			request.setSceneId(sceneId);
		}
		if (length != null) {
			request.setLength(length);
		}
		if (start != null) {
			request.setStart(start);
		}

		if (itemTitle != null) {
			request.setItemTitle(itemTitle);
		}

		AdminSceneProtos.GetSceneItemResponse response = Futures.getUnchecked(this.adminSceneService.getSceneItem(head,
				request.build()));

		// 获取admin信息
		Set<Long> adminIds = new TreeSet<Long>();
		Set<Long> userIds = new TreeSet<Long>();

		for (SceneProtos.Item item : response.getItemList()) {
			adminIds.add(item.getItemIndex().getCreateAdminId());
			adminIds.add(item.getItemIndex().getUpdateAdminId());
			if (item.hasDiscoverItem()) {
				adminIds.add(item.getDiscoverItem().getCreateAdminId());
				adminIds.add(item.getDiscoverItem().getUpdateAdminId());
			}

			if (item.hasCommunityPost()) {
				userIds.add(item.getCommunityPost().getCreateUserId());
			}
		}

		Map<Integer, SceneProtos.Scene> sceneMap = new HashMap<Integer, SceneProtos.Scene>();
		for (SceneProtos.Scene scene : response.getRefSceneList()) {
			sceneMap.put(scene.getSceneId(), scene);
		}

		AdminProtos.GetAdminByIdResponse adminResponse = Futures.getUnchecked(adminService.getAdminById(head,
				AdminProtos.GetAdminByIdRequest.newBuilder().addAllAdminId(adminIds).build()));

		Map<Long, AdminProtos.Admin> adminMap = Util.getAdminMap(adminResponse.getAdminList());
		
		String imageUrlPrefix = Futures.getUnchecked(this.uploadService.getUploadUrlPrefix(head, ServiceUtil.EMPTY_REQUEST)).getImageUrlPrefix();

		JsonObject result = new JsonObject();
		JsonArray itemArray = new JsonArray();

		for (SceneProtos.Item item : response.getItemList()) {
			JsonObject itemObj = new JsonObject();
			JsonObject itemIndexObj = new JsonObject();
			itemIndexObj.addProperty("item_id", item.getItemIndex().getItemId());
			itemIndexObj.addProperty("scene_id", item.getItemIndex().getSceneId());
			itemIndexObj.addProperty("scene_name",
					sceneMap.get(item.getItemIndex().getSceneId()) == null ? "未知的场景" : sceneMap.get(item.getItemIndex().getSceneId()).getSceneName());
			itemIndexObj.addProperty("discover_item_id", item.getItemIndex().getDiscoverItemId());
			itemIndexObj.addProperty("state", item.getItemIndex().getState().name());
			itemIndexObj.addProperty("create_admin_id", item.getItemIndex().getCreateAdminId());

			itemIndexObj.addProperty("create_time", item.getItemIndex().getCreateTime());
			itemIndexObj.addProperty("update_admin_id", item.getItemIndex().getUpdateAdminId());

			itemIndexObj.addProperty("update_time", item.getItemIndex().getUpdateTime());

			itemObj.add("item_index", itemIndexObj);

			JsonObject discoverItemBaseObj = new JsonObject();
			if (item.hasDiscoverItem()) {
				discoverItemBaseObj.addProperty("item_id", item.getDiscoverItem().getItemId());
				discoverItemBaseObj.addProperty("item_name", item.getDiscoverItem().getItemName());
				discoverItemBaseObj.addProperty("item_desc", item.getDiscoverItem().getItemDesc());
				discoverItemBaseObj.addProperty("image_name", item.getDiscoverItem().getImageName());
				discoverItemBaseObj.addProperty("image_url", imageUrlPrefix + item.getDiscoverItem().getImageName());
				discoverItemBaseObj.addProperty("enable_comment", item.getDiscoverItem().getEnableComment());
				discoverItemBaseObj.addProperty("enable_score", item.getDiscoverItem().getEnableScore());
				discoverItemBaseObj.addProperty("allow_model_id", item.getDiscoverItem().getAllowModelId());
				String content;
				if (item.getDiscoverItem().hasWebUrl()) {
					content = JsonUtil.PROTOBUF_JSON_FORMAT.printToString(item.getDiscoverItem().getWebUrl());
				} else if (item.getDiscoverItem().hasDocument()) {
					content = JsonUtil.PROTOBUF_JSON_FORMAT.printToString(item.getDiscoverItem().getDocument());
				} else if (item.getDiscoverItem().hasVideo()) {
					content = JsonUtil.PROTOBUF_JSON_FORMAT.printToString(item.getDiscoverItem().getVideo());
				} else if (item.getDiscoverItem().hasAudio()) {
					content = JsonUtil.PROTOBUF_JSON_FORMAT.printToString(item.getDiscoverItem().getAudio());
				} else if (item.getDiscoverItem().hasAppUri()) {
					content = JsonUtil.PROTOBUF_JSON_FORMAT.printToString(item.getDiscoverItem().getAppUri());
				} else {
					content = "";
				}

				discoverItemBaseObj.addProperty("content", content);

				discoverItemBaseObj.addProperty("state", item.getDiscoverItem().getState().name());
				discoverItemBaseObj.addProperty("create_admin_id", item.getDiscoverItem().getCreateAdminId());
				discoverItemBaseObj.addProperty("create_admin_name", Util.getAdminName(adminMap, item.getDiscoverItem().getCreateAdminId()));
				discoverItemBaseObj.addProperty("create_time", Util.getDate(item.getDiscoverItem().getCreateTime()));
				discoverItemBaseObj.addProperty("update_admin_id", item.getDiscoverItem().getUpdateAdminId());
				discoverItemBaseObj.addProperty("update_admin_name", Util.getAdminName(adminMap, item.getDiscoverItem().getUpdateAdminId()));
				discoverItemBaseObj.addProperty("update_time", Util.getDate(item.getDiscoverItem().getUpdateTime()));
			}

			itemObj.add("discover_item", discoverItemBaseObj);

			itemArray.add(itemObj);
		}

		result.add("item", itemArray);
		result.addProperty("total_size", response.getTotalSize());
		result.addProperty("filtered_size", response.getFilteredSize());
		httpResponse.setContentType("application/json;charset=UTF-8");
		JsonUtil.GSON.toJson(result, httpResponse.getWriter());
	}

}
