package com.weizhu.webapp.admin.api;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.common.util.concurrent.Futures;
import com.google.gson.JsonObject;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.weizhu.common.utils.JsonUtil;
import com.weizhu.proto.AdminProtos;
import com.weizhu.proto.AdminService;
import com.weizhu.proto.AdminProtos.AdminHead;
import com.weizhu.proto.AdminProtos.UpdateAdminStateRequest;
import com.weizhu.proto.AdminProtos.UpdateAdminStateResponse;
import com.weizhu.web.ParamUtil;


@Singleton
@SuppressWarnings("serial")
public class UpdateAdminStateServlet extends HttpServlet {

	private final Provider<AdminHead> adminHeadProvider;
	private final AdminService adminService;
	
	@Inject
	public UpdateAdminStateServlet(Provider<AdminHead> adminHeadProvider, AdminService adminService) {
		this.adminHeadProvider = adminHeadProvider;
		this.adminService = adminService;
	}

	@Override
	protected void doPost(HttpServletRequest httpRequest, HttpServletResponse httpResponse) throws ServletException, IOException {
		// 1. 取出参数
		List<Long> adminIdList = ParamUtil.getLongList(httpRequest, "admin_id", Collections.<Long>emptyList());
		String stateStr = ParamUtil.getString(httpRequest, "state", "");
		
		AdminProtos.State state = null;
		for (AdminProtos.State s : AdminProtos.State.values()) {
			if (s.name().equals(stateStr)) {
				state = s;
				break;
			}
		}
		
		if (state == null || (state != AdminProtos.State.DISABLE && state != AdminProtos.State.NORMAL)) {
			JsonObject resultObj = new JsonObject();
			resultObj.addProperty("result", "FAIL_STATE_INVALID");
			resultObj.addProperty("fail_text", "状态参数填写错误");
			
			httpResponse.setContentType("application/json;charset=UTF-8");
			JsonUtil.GSON.toJson(resultObj, httpResponse.getWriter());
			return;
		}
		
		final AdminHead head = this.adminHeadProvider.get();
		
		UpdateAdminStateResponse response = Futures.getUnchecked(
				this.adminService.updateAdminState(head, 
						UpdateAdminStateRequest.newBuilder()
						.addAllAdminId(adminIdList)
						.setState(state)
						.build()));
		
		httpResponse.setContentType("application/json;charset=UTF-8");
		JsonUtil.PROTOBUF_JSON_FORMAT.print(response, httpResponse.getWriter());
	}

}
