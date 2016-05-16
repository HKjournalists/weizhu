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
public class CreateDiscoverModuleServlet extends HttpServlet {
	
	private final Provider<AdminHead> adminHeadProvider;
	private final AdminDiscoverService adminDiscoverService;

	@Inject
	public CreateDiscoverModuleServlet(Provider<AdminHead> adminHeadProvider, AdminDiscoverService adminDiscoverService) {
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
		String moduleName = ParamUtil.getString(httpRequest, "module_name", "");
		String imageName = ParamUtil.getString(httpRequest, "image_name", "");
		Integer allowModelId = ParamUtil.getInt(httpRequest, "allow_model_id", null);
		String webUrlJson = ParamUtil.getString(httpRequest, "web_url", null);
		String appUriJson = ParamUtil.getString(httpRequest, "app_uri", null);
		Boolean promptDot = ParamUtil.getBoolean(httpRequest, "prompt_dot", null);
		// List<Integer> categoryOrderIdList = ParamUtil.getIntList(httpRequest, "category_order_str", Collections.<Integer>emptyList());
		
		if (allowModelId != null && allowModelId == 0) {
			allowModelId = null;
		}
		
		final AdminHead head = this.adminHeadProvider.get();
		
		AdminDiscoverProtos.CreateModuleRequest.Builder requestBuilder = AdminDiscoverProtos.CreateModuleRequest.newBuilder()
				.setModuleName(moduleName)
				.setImageName(imageName);

		if (null != allowModelId) {
			requestBuilder.setAllowModelId(allowModelId);
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
		if (null != promptDot) {
			requestBuilder.setIsPromptDot(promptDot);
		}
		
		AdminDiscoverProtos.CreateModuleResponse response = Futures.getUnchecked(this.adminDiscoverService.createModule(head, requestBuilder.build()));

		httpResponse.setContentType("application/json;charset=UTF-8");
		JsonUtil.PROTOBUF_JSON_FORMAT.print(response, httpResponse.getWriter());
	}
}
