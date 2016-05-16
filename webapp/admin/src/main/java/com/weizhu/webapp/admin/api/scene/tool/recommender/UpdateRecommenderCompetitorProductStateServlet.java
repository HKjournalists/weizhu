package com.weizhu.webapp.admin.api.scene.tool.recommender;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

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
import com.weizhu.proto.SceneProtos;
import com.weizhu.web.ParamUtil;

@Singleton
@SuppressWarnings("serial")
public class UpdateRecommenderCompetitorProductStateServlet extends HttpServlet {
	private final Provider<AdminHead> adminHeadProvider;
	private final AdminSceneService adminSceneService;

	@Inject
	public UpdateRecommenderCompetitorProductStateServlet(Provider<AdminHead> adminHeadProvider, AdminSceneService adminSceneService) {
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

		List<Integer> competitorProductIds = ParamUtil.getIntList(httpRequest, "competitor_product_id", Collections.<Integer> emptyList());
		String state = ParamUtil.getString(httpRequest, "state", "");

		AdminSceneProtos.UpdateRecommenderCompetitorProductStateRequest.Builder request = AdminSceneProtos.UpdateRecommenderCompetitorProductStateRequest.newBuilder()
				.addAllCompetitorProductId(competitorProductIds);

		if (state.equals("DELETE")) {
			request.setState(SceneProtos.State.DELETE);
		} else if (state.equals("DISABLE")) {
			request.setState(SceneProtos.State.DISABLE);
		} else if (state.equals("NORMAL")) {
			request.setState(SceneProtos.State.NORMAL);
		} else {
			throw new RuntimeException("没有该状态值！");
		}
		AdminSceneProtos.UpdateRecommenderCompetitorProductStateResponse response = Futures.getUnchecked(this.adminSceneService.updateRecommenderCompetitorProductState(this.adminHeadProvider.get(),
				request.build()));

		httpResponse.setContentType("application/json;charset=UTF-8");
		JsonUtil.PROTOBUF_JSON_FORMAT.print(response, httpResponse.getWriter());
	}

}
