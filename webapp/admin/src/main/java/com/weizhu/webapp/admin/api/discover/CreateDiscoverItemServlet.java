package com.weizhu.webapp.admin.api.discover;

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
import com.google.protobuf.ExtensionRegistry;
import com.weizhu.common.utils.JsonUtil;
import com.weizhu.proto.AdminDiscoverProtos;
import com.weizhu.proto.AdminDiscoverService;
import com.weizhu.proto.AdminProtos.AdminHead;
import com.weizhu.proto.DiscoverV2Protos;
import com.weizhu.web.ParamUtil;

@Singleton
@SuppressWarnings("serial")
public class CreateDiscoverItemServlet extends HttpServlet {
	
	private final Provider<AdminHead> adminHeadProvider;
	private final AdminDiscoverService adminDiscoverService;

	@Inject
	public CreateDiscoverItemServlet(Provider<AdminHead> adminHeadProvider, AdminDiscoverService adminDiscoverService) {
		this.adminHeadProvider = adminHeadProvider;
		this.adminDiscoverService = adminDiscoverService;
	}

	@Override
	protected void doGet(HttpServletRequest httpRequest, HttpServletResponse httpResponse) throws ServletException, IOException {
		this.doPost(httpRequest, httpResponse);
	}

	@Override
	protected void doPost(HttpServletRequest httpRequest, HttpServletResponse httpResponse) throws ServletException, IOException {
		// 1. 取出参数
		List<Integer> categoryIdList = ParamUtil.getIntList(httpRequest, "category_id", Collections.<Integer>emptyList());
		String itemName = ParamUtil.getString(httpRequest, "item_name", "");
		String itemDesc = ParamUtil.getString(httpRequest, "item_desc", "");
		String imageName = ParamUtil.getString(httpRequest, "image_name", "");
		Integer allowModelId = ParamUtil.getInt(httpRequest, "allow_model_id", null);
		boolean enableComment = ParamUtil.getBoolean(httpRequest, "enable_comment", false);
		boolean enableScore = ParamUtil.getBoolean(httpRequest, "enable_score", false);
		boolean enableRemind = ParamUtil.getBoolean(httpRequest, "enable_remind", false);
		boolean enableLike = ParamUtil.getBoolean(httpRequest, "enable_like", false);
		boolean enableShare = ParamUtil.getBoolean(httpRequest, "enable_share", false);
		Boolean enableExternalShare = ParamUtil.getBoolean(httpRequest, "enable_external_share", null);
		String webUrlJson = ParamUtil.getString(httpRequest, "web_url", null);
		String documentJson = ParamUtil.getString(httpRequest, "document", null);
		String videoJson = ParamUtil.getString(httpRequest, "video", null);
		String audioJson = ParamUtil.getString(httpRequest, "audio", null);
		String appUriJson = ParamUtil.getString(httpRequest, "app_uri", null);
		
		if (allowModelId != null && allowModelId == 0) {
			allowModelId = null;
		}
		
		final AdminHead head = this.adminHeadProvider.get();
		
		AdminDiscoverProtos.CreateItemRequest.Builder ruequestBuilder= AdminDiscoverProtos.CreateItemRequest.newBuilder()
					.addAllCategoryId(categoryIdList)
					.setItemName(itemName)
					.setItemDesc(itemDesc)
					.setImageName(imageName)
					.setEnableComment(enableComment)
					.setEnableScore(enableScore)
					.setEnableRemind(enableRemind)
					.setEnableLike(enableLike)
					.setEnableShare(enableShare);
		if (null != allowModelId) {
			ruequestBuilder.setAllowModelId(allowModelId);
		}
		if (null != enableExternalShare) {
			ruequestBuilder.setEnableExternalShare(enableExternalShare);
		}
		if (null != webUrlJson) {
			DiscoverV2Protos.WebUrl.Builder webUrlBuilder = DiscoverV2Protos.WebUrl.newBuilder();
			JsonUtil.PROTOBUF_JSON_FORMAT.merge(webUrlJson, ExtensionRegistry.getEmptyRegistry(), webUrlBuilder);
			ruequestBuilder.setWebUrl(webUrlBuilder.build());
		}
		if (null != documentJson) {
			DiscoverV2Protos.Document.Builder documentBuilder = DiscoverV2Protos.Document.newBuilder();
			JsonUtil.PROTOBUF_JSON_FORMAT.merge(documentJson, ExtensionRegistry.getEmptyRegistry(), documentBuilder);
			ruequestBuilder.setDocument(documentBuilder.build());
		}
		if (null != videoJson) {
			DiscoverV2Protos.Video.Builder videoBuilder = DiscoverV2Protos.Video.newBuilder();
			JsonUtil.PROTOBUF_JSON_FORMAT.merge(videoJson, ExtensionRegistry.getEmptyRegistry(), videoBuilder);
			ruequestBuilder.setVideo(videoBuilder.build());
		}
		if (null != audioJson) {
			DiscoverV2Protos.Audio.Builder audioBuilder = DiscoverV2Protos.Audio.newBuilder();
			JsonUtil.PROTOBUF_JSON_FORMAT.merge(audioJson, ExtensionRegistry.getEmptyRegistry(), audioBuilder);
			ruequestBuilder.setAudio(audioBuilder.build());
		}
		if (null != appUriJson) {
			DiscoverV2Protos.AppUri.Builder appUriBuilder = DiscoverV2Protos.AppUri.newBuilder();
			JsonUtil.PROTOBUF_JSON_FORMAT.merge(appUriJson, ExtensionRegistry.getEmptyRegistry(), appUriBuilder);
			ruequestBuilder.setAppUri(appUriBuilder.build());
		}
		
		AdminDiscoverProtos.CreateItemResponse response = Futures.getUnchecked(this.adminDiscoverService.createItem(head, ruequestBuilder.build()));

		httpResponse.setContentType("application/json;charset=UTF-8");
		JsonUtil.PROTOBUF_JSON_FORMAT.print(response, httpResponse.getWriter());
	}

}
