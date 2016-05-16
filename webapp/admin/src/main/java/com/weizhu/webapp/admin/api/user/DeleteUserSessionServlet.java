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
import com.weizhu.proto.SessionProtos.DeleteSessionDataRequest;
import com.weizhu.proto.SessionProtos.DeleteSessionDataResponse;
import com.weizhu.common.utils.JsonUtil;
import com.weizhu.proto.SessionService;
import com.weizhu.proto.AdminProtos.AdminHead;
import com.weizhu.web.ParamUtil;

@Singleton
@SuppressWarnings("serial")
public class DeleteUserSessionServlet extends HttpServlet {
	
	private final Provider<AdminHead> adminHeadProvider;
	private final SessionService sessionService;
	
	@Inject
	public DeleteUserSessionServlet(Provider<AdminHead> adminHeadProvider, SessionService sessionService) {
		this.adminHeadProvider = adminHeadProvider;
		this.sessionService = sessionService;
	}
	
	@Override
	protected void doGet(HttpServletRequest httpRequest, HttpServletResponse httpResponse) throws ServletException, IOException {
		this.doPost(httpRequest, httpResponse);
	}

	@Override
	protected void doPost(HttpServletRequest httpRequest, HttpServletResponse httpResponse) throws ServletException, IOException {
		
		// 1. 取出参数
		long userId = ParamUtil.getLong(httpRequest, "user_id", 0L);
		List<Long> sessionIdList = ParamUtil.getLongList(httpRequest, "session_id", Collections.<Long>emptyList());
		
		// 2. 调用Service
		
		final AdminHead head = adminHeadProvider.get();
		
		DeleteSessionDataRequest request = DeleteSessionDataRequest.newBuilder()
				.setUserId(userId)
				.addAllSessionId(sessionIdList)
				.build();
		
		DeleteSessionDataResponse response = Futures.getUnchecked(this.sessionService.deleteSessionData(head, request));
		
		httpResponse.setContentType("application/json;charset=UTF-8");
		JsonUtil.PROTOBUF_JSON_FORMAT.print(response, httpResponse.getWriter());
	}

}
