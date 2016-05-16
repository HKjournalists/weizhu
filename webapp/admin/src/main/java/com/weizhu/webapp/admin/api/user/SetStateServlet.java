package com.weizhu.webapp.admin.api.user;

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
import com.weizhu.proto.AdminUserProtos.SetStateRequest;
import com.weizhu.proto.AdminUserProtos.SetStateResponse;
import com.weizhu.common.utils.JsonUtil;
import com.weizhu.proto.AdminUserService;
import com.weizhu.proto.UserProtos;
import com.weizhu.proto.AdminProtos.AdminHead;
import com.weizhu.web.ParamUtil;

@Singleton
@SuppressWarnings("serial")
public class SetStateServlet extends HttpServlet {
	
	private final Provider<AdminHead> adminHeadProvider;
	private final AdminUserService adminUserService;
	
	@Inject
	public SetStateServlet(Provider<AdminHead> adminHeadProvider, AdminUserService adminUserService) {
		this.adminHeadProvider = adminHeadProvider;
		this.adminUserService = adminUserService;
	}
	
	@Override
	protected void doGet(HttpServletRequest httpRequest, HttpServletResponse httpResponse) throws ServletException, IOException {
		this.doPost(httpRequest, httpResponse);
	}

	@Override
	protected void doPost(HttpServletRequest httpRequest, HttpServletResponse httpResponse) throws ServletException, IOException {

		// 1. 取出参数
		List<Long> userIdList = ParamUtil.getLongList(httpRequest, "user_id", Collections.<Long>emptyList());
		String stateStr = ParamUtil.getString(httpRequest, "state", "");
		
		UserProtos.UserBase.State state = null;
		for (UserProtos.UserBase.State s : UserProtos.UserBase.State.values()) {
			if (s.name().equals(stateStr)) {
				state = s;
				break;
			}
		}
		
		if (state == null) {
			JsonObject ret = new JsonObject();
			ret.addProperty("result", "FAIL_STATE_INVALID");
			ret.addProperty("fail_text", "状态错误");
			
			httpResponse.setContentType("application/json;charset=UTF-8");
			JsonUtil.GSON.toJson(ret, httpResponse.getWriter());
			return;
		}
		
		
		// 2. 调用Service
		
		final AdminHead head = adminHeadProvider.get();
		
		SetStateRequest request = SetStateRequest.newBuilder()
				.addAllUserId(userIdList)
				.setState(state)
				.build();
		
		SetStateResponse response = Futures.getUnchecked(adminUserService.setState(head, request));
		
		httpResponse.setContentType("application/json;charset=UTF-8");
		JsonUtil.PROTOBUF_JSON_FORMAT.print(response, httpResponse.getWriter());
	}

}
