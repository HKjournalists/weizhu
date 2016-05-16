package com.weizhu.webapp.admin.api.official;

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
import com.weizhu.proto.AdminOfficialProtos.DeleteOfficialResponse;
import com.weizhu.proto.AdminOfficialService;
import com.weizhu.proto.AdminOfficialProtos.DeleteOfficialRequest;
import com.weizhu.proto.AdminProtos.AdminHead;
import com.weizhu.web.ParamUtil;

@Singleton
public class DeleteOfficialServlet extends HttpServlet {

	private static final long serialVersionUID = 1L;
	
	private final Provider<AdminHead> adminHeadProvider;
	private final AdminOfficialService adminOfficialService;
	
	@Inject
	public DeleteOfficialServlet(Provider<AdminHead> adminHeadProvider, AdminOfficialService adminOfficialService) {
		this.adminHeadProvider = adminHeadProvider;
		this.adminOfficialService = adminOfficialService;
	}
	
	@Override
	public void doPost(HttpServletRequest httpRequest, HttpServletResponse httpResponse) throws IOException {
		final List<Long> officialIdList = ParamUtil.getLongList(httpRequest, "official_id_list", Collections.<Long>emptyList());
		
		final AdminHead adminHead = adminHeadProvider.get();
		
		DeleteOfficialResponse response = Futures.getUnchecked(adminOfficialService.deleteOfficial(adminHead, DeleteOfficialRequest.newBuilder()
				.addAllOfficialId(officialIdList)
				.build()));
		
		httpResponse.setContentType("application/json;charset=UTF-8");
		JsonUtil.PROTOBUF_JSON_FORMAT.print(response, httpResponse.getWriter());
	}
	
	@Override
	public void doGet(HttpServletRequest httpRequest, HttpServletResponse httpResponse) throws IOException {
		this.doPost(httpRequest, httpResponse);
	}
}
