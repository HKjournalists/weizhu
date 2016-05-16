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
import com.weizhu.common.utils.JsonUtil;
import com.weizhu.proto.AdminDiscoverProtos;
import com.weizhu.proto.AdminDiscoverService;
import com.weizhu.proto.AdminProtos.AdminHead;
import com.weizhu.proto.DiscoverV2Protos.State;
import com.weizhu.web.ParamUtil;

@Singleton
@SuppressWarnings("serial")
public class DeleteDiscoverModuleCategoryServlet extends HttpServlet {
	
	private final Provider<AdminHead> adminHeadProvider;
	private final AdminDiscoverService adminDiscoverService;

	@Inject
	public DeleteDiscoverModuleCategoryServlet(Provider<AdminHead> adminHeadProvider, AdminDiscoverService adminDiscoverService) {
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
		List<Integer> categoryIdList = ParamUtil.getIntList(httpRequest, "category_id", Collections.<Integer> emptyList());	
		
		final AdminHead head = this.adminHeadProvider.get();
		
		AdminDiscoverProtos.UpdateModuleCategoryStateRequest request = AdminDiscoverProtos.UpdateModuleCategoryStateRequest.newBuilder()
					.addAllCategoryId(categoryIdList)
					.setState(State.DELETE)
					.build();
		
		AdminDiscoverProtos.UpdateModuleCategoryStateResponse response = Futures.getUnchecked(this.adminDiscoverService.updateModuleCategoryState(head, request));
		
		httpResponse.setContentType("application/json;charset=UTF-8");
		JsonUtil.PROTOBUF_JSON_FORMAT.print(response, httpResponse.getWriter());
	}

}
