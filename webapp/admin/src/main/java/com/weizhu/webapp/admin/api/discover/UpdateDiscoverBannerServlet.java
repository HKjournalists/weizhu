package com.weizhu.webapp.admin.api.discover;

import java.io.IOException;

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
public class UpdateDiscoverBannerServlet extends HttpServlet {
	
	private final Provider<AdminHead> adminHeadProvider;
	private final AdminDiscoverService adminDiscoverService;

	@Inject
	public UpdateDiscoverBannerServlet(Provider<AdminHead> adminHeadProvider, AdminDiscoverService adminDiscoverService) {
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
		int bannerId = ParamUtil.getInt(httpRequest, "banner_id", -1);
		String bannerName = ParamUtil.getString(httpRequest, "banner_name", "");
		String imageName = ParamUtil.getString(httpRequest, "image_name", "");
		Long itemId = ParamUtil.getLong(httpRequest, "item_id", null);
		String webUrlJson = ParamUtil.getString(httpRequest, "web_url", null);
		String appUriJson = ParamUtil.getString(httpRequest, "app_uri", null);
		Integer allowModelId = ParamUtil.getInt(httpRequest, "allow_model_id", null);
		
		if (allowModelId != null && allowModelId == 0) {
			allowModelId = null;
		}
		
		final AdminHead head = this.adminHeadProvider.get();
		
		AdminDiscoverProtos.UpdateBannerRequest.Builder requestBuilder = AdminDiscoverProtos.UpdateBannerRequest.newBuilder()
				.setBannerId(bannerId)
				.setBannerName(bannerName)
				.setImageName(imageName);
		if (null != itemId) {
			requestBuilder.setItemId(itemId);
		}
		if (null != webUrlJson) {
			DiscoverV2Protos.WebUrl.Builder webUrlBuilder = DiscoverV2Protos.WebUrl.newBuilder();
			JsonUtil.PROTOBUF_JSON_FORMAT.merge(webUrlJson, ExtensionRegistry.getEmptyRegistry(), webUrlBuilder);
			requestBuilder.setWebUrl(webUrlBuilder.build());
		}
		if (null != appUriJson) {
			DiscoverV2Protos.AppUri.Builder appUriBuilder = DiscoverV2Protos.AppUri.newBuilder();
			JsonUtil.PROTOBUF_JSON_FORMAT.merge(appUriJson, ExtensionRegistry.getEmptyRegistry(), appUriBuilder);
			requestBuilder.setAppUri(appUriBuilder.build());
		}
		if (null != allowModelId) {
			requestBuilder.setAllowModelId(allowModelId);
		}
		
		AdminDiscoverProtos.UpdateBannerResponse response = Futures.getUnchecked(this.adminDiscoverService.updateBanner(head, requestBuilder.build()));

		httpResponse.setContentType("application/json;charset=UTF-8");
		JsonUtil.PROTOBUF_JSON_FORMAT.print(response, httpResponse.getWriter());
	}
}
