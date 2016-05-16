package com.weizhu.webapp.admin.api.official;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.common.util.concurrent.Futures;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.weizhu.proto.AdminOfficialProtos.CreateOfficialRequest;
import com.weizhu.proto.AdminOfficialProtos.CreateOfficialResponse;
import com.weizhu.common.utils.JsonUtil;
import com.weizhu.proto.AdminOfficialService;
import com.weizhu.proto.AdminProtos.AdminHead;
import com.weizhu.web.ParamUtil;

@Singleton
@SuppressWarnings("serial")
public class CreateOfficialServlet extends HttpServlet {
	
	private final Provider<AdminHead> adminHeadProvider;
	private final AdminOfficialService adminOfficialService;
	
	@Inject
	public CreateOfficialServlet(Provider<AdminHead> adminHeadProvider, AdminOfficialService adminOfficialService) {
		this.adminHeadProvider = adminHeadProvider;
		this.adminOfficialService = adminOfficialService;
	}
	
	@Override
	protected void doGet(HttpServletRequest httpRequest, HttpServletResponse httpResponse) throws ServletException, IOException {
		this.doPost(httpRequest, httpResponse);
	}

	@Override
	protected void doPost(HttpServletRequest httpRequest, HttpServletResponse httpResponse) throws ServletException, IOException {

		// 1. 取出参数
		String officialName = ParamUtil.getString(httpRequest, "official_name", "");
		String avatarName = ParamUtil.getString(httpRequest, "avatar_name", "");
		String officialDesc = ParamUtil.getString(httpRequest, "official_desc", null);
		String functionDesc = ParamUtil.getString(httpRequest, "function_desc", null);
		Integer modelId = ParamUtil.getInt(httpRequest, "allow_model_id", null);
		
		// 2. 调用Service
		
		final AdminHead head = adminHeadProvider.get();
		
		CreateOfficialRequest.Builder requestBuilder = CreateOfficialRequest.newBuilder()
				.setOfficialName(officialName)
				.setAvatar(avatarName);
		
		if (officialDesc != null && !officialDesc.trim().isEmpty()) {
			requestBuilder.setOfficialDesc(officialDesc.trim());
		}
		if (functionDesc != null && !functionDesc.trim().isEmpty()) {
			requestBuilder.setFunctionDesc(functionDesc.trim());
		}
		
		if (modelId != null) {
			requestBuilder.setAllowModelId(modelId);
		}
		
		CreateOfficialResponse response = Futures.getUnchecked(adminOfficialService.createOfficial(head, requestBuilder.build()));

		httpResponse.setContentType("application/json;charset=UTF-8");
		JsonUtil.PROTOBUF_JSON_FORMAT.print(response, httpResponse.getWriter());
	}
}
