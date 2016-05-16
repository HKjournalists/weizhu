package com.weizhu.webapp.admin.api.official;

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
import com.weizhu.proto.AdminOfficialService;
import com.weizhu.common.utils.JsonUtil;
import com.weizhu.proto.AdminOfficialProtos.CancelOfficialSendPlanRequest;
import com.weizhu.proto.AdminOfficialProtos.CancelOfficialSendPlanResponse;
import com.weizhu.proto.AdminProtos.AdminHead;
import com.weizhu.web.ParamUtil;

@Singleton
@SuppressWarnings("serial")
public class CancelOfficialSendPlanServlet extends HttpServlet {
	
	private final Provider<AdminHead> adminHeadProvider;
	private final AdminOfficialService adminOfficialService;
	
	@Inject
	public CancelOfficialSendPlanServlet(Provider<AdminHead> adminHeadProvider, AdminOfficialService adminOfficialService) {
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
		List<Integer> planIdList = ParamUtil.getIntList(httpRequest, "plan_id", Collections.<Integer>emptyList());
		
		// 2. 调用Service
		
		final AdminHead head = adminHeadProvider.get();
		
		CancelOfficialSendPlanRequest request = CancelOfficialSendPlanRequest.newBuilder()
				.addAllPlanId(planIdList)
				.build();
		
		CancelOfficialSendPlanResponse response = Futures.getUnchecked(adminOfficialService.cancelOfficialSendPlan(head, request));

		httpResponse.setContentType("application/json;charset=UTF-8");
		JsonUtil.PROTOBUF_JSON_FORMAT.print(response, httpResponse.getWriter());
	}

}
