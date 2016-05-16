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
import com.weizhu.common.utils.JsonUtil;
import com.weizhu.proto.AdminDiscoverProtos;
import com.weizhu.proto.AdminDiscoverService;
import com.weizhu.proto.AdminProtos.AdminHead;
import com.weizhu.web.ParamUtil;

@Singleton
@SuppressWarnings("serial")
public class CreateDiscoverModuleCategoryServlet extends HttpServlet {
	
	private final Provider<AdminHead> adminHeadProvider;
	private final AdminDiscoverService adminDiscoverService;

	@Inject
	public CreateDiscoverModuleCategoryServlet(Provider<AdminHead> adminHeadProvider, AdminDiscoverService adminDiscoverService) {
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
		int moduleId = ParamUtil.getInt(httpRequest, "module_id", -1);
		String categoryName = ParamUtil.getString(httpRequest, "category_name", "");
		Integer allowModelId = ParamUtil.getInt(httpRequest, "allow_model_id", null);
		
		if (allowModelId != null && allowModelId == 0) {
			allowModelId = null;
		}
		
		final AdminHead head = this.adminHeadProvider.get();
		
		AdminDiscoverProtos.CreateModuleCategoryRequest.Builder requestBuilder= AdminDiscoverProtos.CreateModuleCategoryRequest.newBuilder()
					.setModuleId(moduleId)
					.setCategoryName(categoryName);
		
		if (null != allowModelId) {
			requestBuilder.setAllowModelId(allowModelId);
		}

		AdminDiscoverProtos.CreateModuleCategoryResponse response = Futures.getUnchecked(this.adminDiscoverService.createModuleCategory(head, requestBuilder.build()));

		httpResponse.setContentType("application/json;charset=UTF-8");
		JsonUtil.PROTOBUF_JSON_FORMAT.print(response, httpResponse.getWriter());
	}

}
