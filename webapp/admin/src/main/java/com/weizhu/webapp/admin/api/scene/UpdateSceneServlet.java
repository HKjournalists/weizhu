package com.weizhu.webapp.admin.api.scene;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.common.util.concurrent.Futures;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.weizhu.proto.AdminProtos.AdminHead;
import com.weizhu.common.utils.JsonUtil;
import com.weizhu.proto.AdminSceneProtos;
import com.weizhu.proto.AdminSceneService;
import com.weizhu.web.ParamUtil;

@Singleton
@SuppressWarnings("serial")
public class UpdateSceneServlet extends HttpServlet {
	private final Provider<AdminHead> adminHeadProvider;
	private final AdminSceneService adminSceneService;

	@Inject
	public UpdateSceneServlet(Provider<AdminHead> adminHeadProvider, AdminSceneService adminSceneService) {
		this.adminHeadProvider = adminHeadProvider;
		this.adminSceneService = adminSceneService;
	}

	@Override
	protected void doGet(HttpServletRequest httpRequest, HttpServletResponse httpResponse) throws ServletException, IOException {
		this.doPost(httpRequest, httpResponse);
	}

	@Override
	protected void doPost(HttpServletRequest httpRequest, HttpServletResponse httpResponse) throws ServletException, IOException {

		// 1. 取出参数
		int sceneId = ParamUtil.getInt(httpRequest, "scene_id", -1);
		String sceneName = ParamUtil.getString(httpRequest, "scene_name", "");
		String imageName = ParamUtil.getString(httpRequest, "image_name", "");
		String sceneDesc = ParamUtil.getString(httpRequest, "scene_desc", "");
		Integer parentSceneId = ParamUtil.getInt(httpRequest, "parent_scene_id", null);

		AdminSceneProtos.UpdateSceneRequest.Builder request = AdminSceneProtos.UpdateSceneRequest.newBuilder()
				.setSceneId(sceneId)
				.setSceneName(sceneName)
				.setImageName(imageName)
				.setSceneDesc(sceneDesc);
		if (parentSceneId != null) {
			request.setParentSceneId(parentSceneId);
		}

		AdminSceneProtos.UpdateSceneResponse response = Futures.getUnchecked(this.adminSceneService.updateScene(this.adminHeadProvider.get(),
				request.build()));

		httpResponse.setContentType("application/json;charset=UTF-8");
		JsonUtil.PROTOBUF_JSON_FORMAT.print(response, httpResponse.getWriter());
	}

}
