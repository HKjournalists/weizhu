package com.weizhu.webapp.admin.api.scene;

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
import com.weizhu.proto.AdminProtos.AdminHead;
import com.weizhu.proto.AdminProtos;
import com.weizhu.proto.AdminSceneProtos;
import com.weizhu.proto.AdminSceneService;
import com.weizhu.proto.AdminService;
import com.weizhu.proto.SceneProtos;
import com.weizhu.proto.UploadService;
import com.weizhu.proto.WeizhuProtos;
import com.weizhu.webapp.admin.api.Util;

@Singleton
@SuppressWarnings("serial")
public class GetSceneServlet extends HttpServlet {
	private final Provider<AdminHead> adminHeadProvider;
	private final AdminSceneService adminSceneService;
	private final AdminService adminService;
	private final UploadService uploadService;

	@Inject
	public GetSceneServlet(Provider<AdminHead> adminHeadProvider, AdminSceneService adminSceneService, AdminService adminService,
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
		
		final AdminHead head = this.adminHeadProvider.get();

		AdminSceneProtos.GetSceneResponse response = Futures.getUnchecked(this.adminSceneService.getScene(head,
				WeizhuProtos.EmptyRequest.newBuilder().build()));

		Map<Integer, List<Integer>> sceneIdSubSceneIdListMap = new HashMap<Integer, List<Integer>>();
		List<Integer> rootSceneIdList = new ArrayList<Integer>();
		Map<Integer, SceneProtos.Scene> sceneMap = new HashMap<Integer, SceneProtos.Scene>();

		// 获取admin信息,及其场景的相关信息
		Set<Long> adminIds = new TreeSet<Long>();
		for (SceneProtos.Scene scene : response.getSceneList()) {
			adminIds.add(scene.getCreateAdminId());
			adminIds.add(scene.getUpdateAdminId());

			if (!scene.hasParentSceneId()) {
				rootSceneIdList.add(scene.getSceneId());
			} else {
				List<Integer> subSceneIdList = sceneIdSubSceneIdListMap.get(scene.getParentSceneId());
				if (subSceneIdList == null) {
					subSceneIdList = new ArrayList<Integer>();
					sceneIdSubSceneIdListMap.put(scene.getParentSceneId(), subSceneIdList);
				}
				subSceneIdList.add(scene.getSceneId());
			}

			sceneMap.put(scene.getSceneId(), scene);
		}
		AdminProtos.GetAdminByIdResponse adminResponse = Futures.getUnchecked(adminService.getAdminById(head,
				AdminProtos.GetAdminByIdRequest.newBuilder().addAllAdminId(adminIds).build()));

		Map<Long, AdminProtos.Admin> adminMap = Util.getAdminMap(adminResponse.getAdminList());

		String imageUrlPrefix = Futures.getUnchecked(this.uploadService.getUploadUrlPrefix(head, ServiceUtil.EMPTY_REQUEST)).getImageUrlPrefix();

		JsonObject result = new JsonObject();
		JsonArray sceneArray = new JsonArray();

		for (Integer sceneId : rootSceneIdList) {
			SceneProtos.Scene scene = sceneMap.get(sceneId);
			if (scene == null) {
				continue;
			}
			sceneArray.add(this.getSceneObj(scene, adminMap, sceneIdSubSceneIdListMap, sceneMap, imageUrlPrefix));
		}

		result.add("scene", sceneArray);
		httpResponse.setContentType("application/json;charset=UTF-8");
		JsonUtil.GSON.toJson(result, httpResponse.getWriter());
	}

	private JsonObject getSceneObj(SceneProtos.Scene scene, Map<Long, AdminProtos.Admin> adminMap,
			Map<Integer, List<Integer>> sceneIdSubSceneIdListMap, Map<Integer, SceneProtos.Scene> sceneMap, 
			String imageUrlPrefix) {
		JsonObject sceneObj = new JsonObject();
		sceneObj.addProperty("scene_id", scene.getSceneId());
		sceneObj.addProperty("scene_name", scene.getSceneName());
		sceneObj.addProperty("image_name", scene.getImageName());
		sceneObj.addProperty("image_url", imageUrlPrefix + scene.getImageName());
		sceneObj.addProperty("scene_desc", scene.getSceneDesc());
		sceneObj.addProperty("parent_scene_id", scene.getParentSceneId());
		sceneObj.addProperty("is_leaf_scene", scene.getIsLeafScene());
		sceneObj.addProperty("item_id_order_str", scene.getItemIdOrderStr());
		sceneObj.addProperty("state", scene.getState().name());
		sceneObj.addProperty("create_admin_id", scene.getCreateAdminId());
		sceneObj.addProperty("create_admin_name", Util.getAdminName(adminMap, scene.getCreateAdminId()));
		sceneObj.addProperty("create_time", Util.getDate(scene.getCreateTime()));
		sceneObj.addProperty("update_admin_id", scene.getUpdateAdminId());
		sceneObj.addProperty("update_admin_name", Util.getAdminName(adminMap, scene.getUpdateAdminId()));
		sceneObj.addProperty("update_time", Util.getDate(scene.getUpdateTime()));
		
		// 叶子节点下有可能存在已经作废的子节点
		//		if (!scene.getIsLeafScene()) {
		List<Integer> subSceneIdList = sceneIdSubSceneIdListMap.get(scene.getSceneId());
		if (subSceneIdList != null) {
			JsonArray childrenSceneArray = new JsonArray();
			for (Integer sceneId : subSceneIdList) {
				SceneProtos.Scene tmpScene = sceneMap.get(sceneId);
				if (tmpScene == null) {
					continue;
				}
				childrenSceneArray.add(this.getSceneObj(tmpScene, adminMap, sceneIdSubSceneIdListMap, sceneMap, imageUrlPrefix));
			}
			sceneObj.add("children_scene", childrenSceneArray);
		}
		//		}
		return sceneObj;
	}
}
