package com.weizhu.webapp.mobile.scene;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

@Singleton
@SuppressWarnings("serial")
public class GetSceneHomeServlet extends HttpServlet {
	private final Provider<RequestHead> requestHeadProvider;
	private final SceneService sceneService;
	@SuppressWarnings("unused")
	private final UserService userService;
	private final UploadService uploadService;

	@Inject
	public GetSceneHomeServlet(Provider<RequestHead> requestHeadProvider, SceneService sceneService, UserService userService,
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
		SceneProtos.GetSceneHomeResponse response = Futures.getUnchecked(this.sceneService.getSceneHome(head, EmptyRequest.newBuilder().build()));

		Map<Integer, List<Integer>> sceneIdSubSceneIdListMap = new HashMap<Integer, List<Integer>>();
		List<Integer> rootSceneIdList = new ArrayList<Integer>();
		Map<Integer, SceneProtos.Scene> sceneMap = new HashMap<Integer, SceneProtos.Scene>();

		// 获取场景的相关信息
		for (SceneProtos.Scene scene : response.getSceneList()) {

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
		
		String imageUrlPrefix = Futures.getUnchecked(this.uploadService.getUploadUrlPrefix(head, ServiceUtil.EMPTY_REQUEST)).getImageUrlPrefix();
		
		//拼接json
		JsonObject result = new JsonObject();
		JsonArray sceneArray = new JsonArray();

		for (Integer sceneId : rootSceneIdList) {
			SceneProtos.Scene scene = sceneMap.get(sceneId);
			if (scene == null) {
				continue;
			}
			sceneArray.add(this.getSceneObj(scene, sceneIdSubSceneIdListMap, sceneMap, imageUrlPrefix));
		}

		result.add("scene", sceneArray);
		httpResponse.setContentType("application/json;charset=UTF-8");
		JsonUtil.GSON.toJson(result, httpResponse.getWriter());
	}

	private JsonObject getSceneObj(SceneProtos.Scene scene, Map<Integer, List<Integer>> sceneIdSubSceneIdListMap,
			Map<Integer, SceneProtos.Scene> sceneMap, String imageUrlPrefix) {
		JsonObject sceneObj = new JsonObject();
		sceneObj.addProperty("scene_id", scene.getSceneId());
		sceneObj.addProperty("scene_name", scene.getSceneName());
		sceneObj.addProperty("image_name", scene.getImageName());
		sceneObj.addProperty("image_url", imageUrlPrefix + scene.getImageName());
		sceneObj.addProperty("scene_desc", scene.getSceneDesc());
		sceneObj.addProperty("parent_scene_id", scene.getParentSceneId());
		sceneObj.addProperty("is_leaf_scene", scene.getIsLeafScene());
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
				childrenSceneArray.add(this.getSceneObj(tmpScene, sceneIdSubSceneIdListMap, sceneMap, imageUrlPrefix));
			}
			sceneObj.add("children_scene", childrenSceneArray);
		}
		//		}
		return sceneObj;
	}
}
