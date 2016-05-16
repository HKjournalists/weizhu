package com.weizhu.webapp.admin.api.user;

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
import com.weizhu.proto.AdminUserService;
import com.weizhu.proto.AdminProtos.AdminHead;
import com.weizhu.proto.AdminUserProtos.DeleteUserRequest;
import com.weizhu.proto.AdminUserProtos.DeleteUserResponse;
import com.weizhu.web.ParamUtil;

@Singleton
@SuppressWarnings("serial")
public class DeleteUserServlet extends HttpServlet {
	
	private final Provider<AdminHead> adminHeadProvider;
	private final AdminUserService adminUserService;
	
	@Inject
	public DeleteUserServlet(Provider<AdminHead> adminHeadProvider, AdminUserService adminUserService) {
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
		
		// 2. 调用Service
		
		final AdminHead head = adminHeadProvider.get();
		
		DeleteUserRequest request = DeleteUserRequest.newBuilder()
				.addAllUserId(userIdList)
				.build();
		
		DeleteUserResponse response = Futures.getUnchecked(adminUserService.deleteUser(head, request));
		
		httpResponse.setContentType("application/json;charset=UTF-8");
		JsonUtil.PROTOBUF_JSON_FORMAT.print(response, httpResponse.getWriter());
	}

}
