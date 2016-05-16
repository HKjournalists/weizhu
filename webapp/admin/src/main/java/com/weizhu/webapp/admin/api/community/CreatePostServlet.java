package com.weizhu.webapp.admin.api.community;

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
import com.weizhu.common.utils.JsonUtil;
import com.weizhu.proto.AdminCommunityProtos;
import com.weizhu.proto.AdminCommunityService;
import com.weizhu.proto.AdminProtos.AdminHead;
import com.weizhu.web.ParamUtil;

@Singleton
@SuppressWarnings("serial")
public class CreatePostServlet extends HttpServlet {
	private final Provider<AdminHead> adminHeadProvider;
	private final AdminCommunityService adminCommunityService;

	@Inject
	public CreatePostServlet(Provider<AdminHead> adminHeadProvider, AdminCommunityService adminCommunityService) {
		this.adminHeadProvider = adminHeadProvider;
		this.adminCommunityService = adminCommunityService;
	}

	@Override
	protected void doGet(HttpServletRequest httpRequest, HttpServletResponse httpResponse) throws ServletException, IOException {
		this.doPost(httpRequest, httpResponse);
	}

	@Override
	protected void doPost(HttpServletRequest httpRequest, HttpServletResponse httpResponse) throws ServletException, IOException {

		// 1. 取出参数
		int boardId = ParamUtil.getInt(httpRequest, "board_id", -1);
		String title = ParamUtil.getString(httpRequest, "title", "");
		String text = ParamUtil.getString(httpRequest, "text", null);
		String imageName = ParamUtil.getString(httpRequest, "image_name", null);
		List<String> tagList = ParamUtil.getStringList(httpRequest, "tag", Collections.emptyList());
		long createUserId = ParamUtil.getLong(httpRequest, "create_user_id", (long)-1);

		AdminCommunityProtos.CreatePostRequest.Builder requestBuilder = AdminCommunityProtos.CreatePostRequest.newBuilder();
		requestBuilder.setBoardId(boardId);
		requestBuilder.setTitle(title);
		if (text != null) {
			requestBuilder.setText(text);
		}
		if (imageName != null) {
			requestBuilder.setImageName(imageName);
		}
		requestBuilder.addAllTag(tagList);
		requestBuilder.setCreateUserId(createUserId);

		AdminCommunityProtos.CreatePostResponse response = Futures
				.getUnchecked(this.adminCommunityService.createPost(this.adminHeadProvider.get(), requestBuilder.build()));

		httpResponse.setContentType("application/json;charset=UTF-8");
		JsonUtil.PROTOBUF_JSON_FORMAT.print(response, httpResponse.getWriter());
	}

}
