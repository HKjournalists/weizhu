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
import com.weizhu.common.utils.JsonUtil;
import com.weizhu.proto.AdminOfficialProtos.SetOfficialStateRequest;
import com.weizhu.proto.AdminOfficialProtos.SetOfficialStateResponse;
import com.weizhu.proto.AdminOfficialService;
import com.weizhu.proto.OfficialProtos;
import com.weizhu.proto.AdminProtos.AdminHead;
import com.weizhu.web.ParamUtil;

@Singleton
@SuppressWarnings("serial")
public class SetOfficialEnableServlet extends HttpServlet {
	
	private final Provider<AdminHead> adminHeadProvider;
	private final AdminOfficialService adminOfficialService;
	
	@Inject
	public SetOfficialEnableServlet(Provider<AdminHead> adminHeadProvider, AdminOfficialService adminOfficialService) {
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
		List<Long> officialIdList = ParamUtil.getLongList(httpRequest, "official_id", Collections.<Long>emptyList());
		boolean isEnable = ParamUtil.getBoolean(httpRequest, "is_enable", false);
		
		// 2. 调用Service
		
		final AdminHead head = adminHeadProvider.get();
		
		SetOfficialStateRequest request = SetOfficialStateRequest.newBuilder()
				.addAllOfficialId(officialIdList)
				.setState(isEnable ? OfficialProtos.State.NORMAL : OfficialProtos.State.DISABLE)
				.build();
		
		SetOfficialStateResponse response = Futures.getUnchecked(adminOfficialService.setOfficialState(head, request));

		httpResponse.setContentType("application/json;charset=UTF-8");
		JsonUtil.PROTOBUF_JSON_FORMAT.print(response, httpResponse.getWriter());
	}

}
