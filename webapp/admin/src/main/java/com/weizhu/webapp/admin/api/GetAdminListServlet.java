package com.weizhu.webapp.admin.api;

import java.io.IOException;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.common.util.concurrent.Futures;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.weizhu.common.utils.JsonUtil;
import com.weizhu.proto.AdminProtos;
import com.weizhu.proto.AdminService;
import com.weizhu.proto.AdminProtos.AdminHead;
import com.weizhu.proto.AdminProtos.GetAdminListRequest;
import com.weizhu.proto.AdminProtos.GetAdminListResponse;
import com.weizhu.web.ParamUtil;

@Singleton
@SuppressWarnings("serial")
public class GetAdminListServlet extends HttpServlet {

	private final Provider<AdminHead> adminHeadProvider;
	private final AdminService adminService;
	
	@Inject
	public GetAdminListServlet(Provider<AdminHead> adminHeadProvider, AdminService adminService) {
		this.adminHeadProvider = adminHeadProvider;
		this.adminService = adminService;
	}
	
	@Override
	protected void doGet(HttpServletRequest httpRequest, HttpServletResponse httpResponse) throws ServletException, IOException {
		this.doPost(httpRequest, httpResponse);
	}

	@Override
	protected void doPost(HttpServletRequest httpRequest, HttpServletResponse httpResponse) throws ServletException, IOException {
		// 1. 取出参数
		int draw = ParamUtil.getInt(httpRequest, "draw", 1);
		int start = ParamUtil.getInt(httpRequest, "start", 0);
		int length = ParamUtil.getInt(httpRequest, "length", 10);
		String stateStr = ParamUtil.getString(httpRequest, "state", "");
		String nameKeyword = ParamUtil.getString(httpRequest, "name_keyword", null);
		
		AdminProtos.State state = null;
		for (AdminProtos.State s : AdminProtos.State.values()) {
			if (s.name().equals(stateStr)) {
				state = s;
				break;
			}
		}
		
		// 2. 调用Service
		final AdminHead head = this.adminHeadProvider.get();
		
		GetAdminListRequest.Builder requestBuilder = GetAdminListRequest.newBuilder()
				.setStart(start)
				.setLength(length);
		
		if (state != null) {
			requestBuilder.setState(state);
		}
		if (nameKeyword != null && !nameKeyword.isEmpty()) {
			requestBuilder.setNameKeyword(nameKeyword);
		}
		
		GetAdminListResponse response = Futures.getUnchecked(this.adminService.getAdminList(head, requestBuilder.build()));
		
		Map<Integer, AdminProtos.Role> refRoleMap = AdminUtil.toRefRoleMap(response.getRefRoleList());
		
		JsonArray dataArray = new JsonArray();
		for (AdminProtos.Admin admin : response.getAdminList()) {
			dataArray.add(AdminUtil.toJsonAdmin(admin, head.getCompanyId(), refRoleMap));
		}		
		
		JsonObject result = new JsonObject();
		result.addProperty("draw", draw);
		result.addProperty("recordsTotal", response.getTotalSize());
		result.addProperty("recordsFiltered", response.getFilteredSize());
		result.add("data", dataArray);
		
		httpResponse.setContentType("application/json;charset=UTF-8");
		JsonUtil.GSON.toJson(result, httpResponse.getWriter());
	}

}
