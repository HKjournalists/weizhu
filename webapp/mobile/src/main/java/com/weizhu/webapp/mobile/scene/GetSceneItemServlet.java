package com.weizhu.webapp.mobile.scene;

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
import com.weizhu.proto.WeizhuProtos.RequestHead;
import com.weizhu.web.ParamUtil;
import com.weizhu.webapp.mobile.MobileServletUtil;

@Singleton
@SuppressWarnings("serial")
public class GetSceneItemServlet extends HttpServlet {
	private final Provider<RequestHead> requestHeadProvider;
	private final SceneService sceneService;
	@SuppressWarnings("unused")
	private final UserService userService;
	private final UploadService uploadService;

	@Inject
	public GetSceneItemServlet(Provider<RequestHead> requestHeadProvider, SceneService sceneService, UserService userService,
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
		// 1. 取出参数
		Integer sceneId = ParamUtil.getInt(httpRequest, "scene_id", null);
		Integer size = ParamUtil.getInt(httpRequest, "size", null);
		String offsetIndex = ParamUtil.getString(httpRequest, "offset_index", null);
		String itemTitle = ParamUtil.getString(httpRequest, "item_title", null);

		final RequestHead head = requestHeadProvider.get();

		SceneProtos.GetSceneItemRequest.Builder requestBuilder = SceneProtos.GetSceneItemRequest.newBuilder();
		if (sceneId != null) {
			requestBuilder.setSceneId(sceneId);
		}
		if (size != null) {
			requestBuilder.setSize(size);
		}
		if (offsetIndex != null) {
			try {
				requestBuilder.setOffsetIndex(ByteString.copyFrom(HexUtil.hex2bin(offsetIndex)));
			} catch (Exception e) {
				// ignore
			}
		}
		if (itemTitle != null) {
			requestBuilder.setItemTitle(itemTitle);
		}
		SceneProtos.GetSceneItemResponse response = Futures.getUnchecked(this.sceneService.getSceneItem(head, requestBuilder.build()));

		String imageUrlPrefix = Futures.getUnchecked(this.uploadService.getUploadUrlPrefix(head, ServiceUtil.EMPTY_REQUEST)).getImageUrlPrefix();
		
		//拼接json
		JsonObject result = new JsonObject();
		JsonArray itemArray = new JsonArray();

		for (SceneProtos.Item item : response.getItemList()) {
			JsonObject itemObj = new JsonObject();
			JsonObject itemIndexObj = new JsonObject();
			itemIndexObj.addProperty("item_id", item.getItemIndex().getItemId());
			itemIndexObj.addProperty("scene_id", item.getItemIndex().getSceneId());
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
				discoverItemBaseObj.addProperty("create_time", MobileServletUtil.getDate(item.getDiscoverItem().getCreateTime()));
				discoverItemBaseObj.addProperty("update_admin_id", item.getDiscoverItem().getUpdateAdminId());
				discoverItemBaseObj.addProperty("update_time", MobileServletUtil.getDate(item.getDiscoverItem().getUpdateTime()));
			}

			itemObj.add("discover_item", discoverItemBaseObj);

			itemArray.add(itemObj);
		}

		result.add("item", itemArray);
		result.addProperty("has_more", response.getHasMore());
		result.addProperty("offset_index", HexUtil.bin2Hex(response.getOffsetIndex().toByteArray()));
		httpResponse.setContentType("application/json;charset=UTF-8");
		JsonUtil.GSON.toJson(result, httpResponse.getWriter());
	}
}
