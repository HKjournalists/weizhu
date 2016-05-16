package com.weizhu.webapp.admin.api.credits;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.common.util.concurrent.Futures;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.weizhu.common.utils.JsonUtil;
import com.weizhu.proto.AdminCreditsProtos.ClearUserCreditsRequest;
import com.weizhu.proto.AdminCreditsProtos.ClearUserCreditsResponse;
import com.weizhu.proto.AdminCreditsService;
import com.weizhu.proto.AdminProtos.AdminHead;
import com.weizhu.web.ParamUtil;

@Singleton
public class ClearUserCreditsServlet extends HttpServlet {

	private static final long serialVersionUID = 1L;

	private final Provider<AdminHead> adminHeadProvider;
	private final AdminCreditsService adminCreditsService;
	
	@Inject
	public ClearUserCreditsServlet(Provider<AdminHead> adminHeadProvider, AdminCreditsService adminCreditsService) {
		this.adminHeadProvider = adminHeadProvider;
		this.adminCreditsService = adminCreditsService;
	}
	
	@Override
	public void doPost(HttpServletRequest httpRequest, HttpServletResponse httpResponse) throws IOException {
		final List<Long> userIdList = ParamUtil.getLongList(httpRequest, "user_id", Collections.<Long>emptyList());
		
		final AdminHead head = adminHeadProvider.get();
		
		ClearUserCreditsResponse response = Futures.getUnchecked(adminCreditsService.clearUserCredits(head, ClearUserCreditsRequest.newBuilder()
				.addAllUserId(userIdList)
				.build()));
		
		httpResponse.setContentType("application/json;charset=UTF-8");
		JsonUtil.PROTOBUF_JSON_FORMAT.print(response, httpResponse.getWriter());
	}
	
	@Override
	public void doGet(HttpServletRequest httpRequest, HttpServletResponse httpResponses) throws IOException {
		this.doPost(httpRequest, httpResponses);
	}
}
