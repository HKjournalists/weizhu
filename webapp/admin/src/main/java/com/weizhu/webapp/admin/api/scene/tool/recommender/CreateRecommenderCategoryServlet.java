package com.weizhu.webapp.admin.api.scene.tool.recommender;

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
public class CreateRecommenderCategoryServlet extends HttpServlet {
	private final Provider<AdminHead> adminHeadProvider;
	private final AdminSceneService adminSceneService;

	@Inject
	public CreateRecommenderCategoryServlet(Provider<AdminHead> adminHeadProvider, AdminSceneService adminSceneService) {
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
		String categoryName = ParamUtil.getString(httpRequest, "category_name", "");
		String imageName = ParamUtil.getString(httpRequest, "image_name", "");
		String categoryDesc = ParamUtil.getString(httpRequest, "category_desc", "");
		Integer parentCategoryId = ParamUtil.getInt(httpRequest, "parent_category_id", null);

		AdminSceneProtos.CreateRecommenderCategoryRequest.Builder request = AdminSceneProtos.CreateRecommenderCategoryRequest.newBuilder()
				.setCategoryName(categoryName)
				.setImageName(imageName)
				.setCategoryDesc(categoryDesc);
		if (parentCategoryId != null) {
			request.setParentCategoryId(parentCategoryId);
		}

		AdminSceneProtos.CreateRecommenderCategoryResponse response = Futures.getUnchecked(this.adminSceneService.createRecommenderCategory(this.adminHeadProvider.get(),
				request.build()));

		httpResponse.setContentType("application/json;charset=UTF-8");
		JsonUtil.PROTOBUF_JSON_FORMAT.print(response, httpResponse.getWriter());
	}

}
