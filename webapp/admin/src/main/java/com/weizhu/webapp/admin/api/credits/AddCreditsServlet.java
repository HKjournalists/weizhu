package com.weizhu.webapp.admin.api.credits;

import java.io.IOException;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.common.util.concurrent.Futures;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.weizhu.common.utils.JsonUtil;
import com.weizhu.proto.AdminCreditsProtos.AddCreditsRequest;
import com.weizhu.proto.AdminCreditsProtos.AddCreditsResponse;
import com.weizhu.proto.AdminCreditsService;
import com.weizhu.proto.AdminProtos.AdminHead;
import com.weizhu.web.ParamUtil;

@Singleton
public class AddCreditsServlet extends HttpServlet {

	private static final long serialVersionUID = 1L;

	private final Provider<AdminHead> adminHeadProvider;
	private final AdminCreditsService adminCreditsService;
	
	@Inject
	public AddCreditsServlet(Provider<AdminHead> adminHeadProvider, AdminCreditsService adminCreditsService) {
		this.adminHeadProvider = adminHeadProvider;
		this.adminCreditsService = adminCreditsService;
	}
	
	@Override
	public void doPost(HttpServletRequest httpRequest, HttpServletResponse httpResponse) throws IOException {
		final String desc = ParamUtil.getString(httpRequest, "desc", null);
		final long creditsDelta = ParamUtil.getLong(httpRequest, "credits_delta", 0L);
		
		AddCreditsRequest.Builder requestBuilder = AddCreditsRequest.newBuilder()
				.setCreditsDelta(creditsDelta);
		
		if (desc != null) {
			requestBuilder.setDesc(desc);
		}
		
		final AdminHead head = adminHeadProvider.get();
		
		AddCreditsResponse response = Futures.getUnchecked(adminCreditsService.addCredits(head, requestBuilder.build()));
	
		httpResponse.setContentType("application/json;charset=UTF-8");
		JsonUtil.PROTOBUF_JSON_FORMAT.print(response, httpResponse.getWriter());
	}
	
	@Override
	public void doGet(HttpServletRequest httpRequest, HttpServletResponse httpResponse) throws IOException {
		this.doPost(httpRequest, httpResponse);
	}
}
