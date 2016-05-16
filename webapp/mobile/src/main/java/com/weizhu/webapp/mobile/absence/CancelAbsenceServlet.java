package com.weizhu.webapp.mobile.absence;

import java.io.IOException;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.common.util.concurrent.Futures;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.weizhu.common.utils.JsonUtil;
import com.weizhu.proto.AbsenceProtos.CancelAbsenceRequest;
import com.weizhu.proto.AbsenceProtos.CancelAbsenceResponse;
import com.weizhu.proto.AbsenceService;
import com.weizhu.proto.WeizhuProtos.RequestHead;
import com.weizhu.web.ParamUtil;

@Singleton
public class CancelAbsenceServlet extends HttpServlet {

	private static final long serialVersionUID = 1L;

	private final Provider<RequestHead> requestHeadProvider;
	private final AbsenceService absenceService;
	
	@Inject
	public CancelAbsenceServlet(Provider<RequestHead> requestHeadProvider, AbsenceService absenceService) {
		this.requestHeadProvider = requestHeadProvider;
		this.absenceService = absenceService;
	}
	
	@Override
	public void doPost(HttpServletRequest httpRequest, HttpServletResponse httpResponse) throws IOException {
		final int absenceId = ParamUtil.getInt(httpRequest, "absence_id", 0);
		final String days = ParamUtil.getString(httpRequest, "days", "");
		
		final RequestHead head = requestHeadProvider.get();
		
		CancelAbsenceResponse response = Futures.getUnchecked(absenceService.cancelAbsence(head, CancelAbsenceRequest.newBuilder()
				.setAbsenceId(absenceId)
				.setDays(days)
				.build()));
		
		httpResponse.setContentType("application/json;charset=UTF-8");
		JsonUtil.PROTOBUF_JSON_FORMAT.print(response, httpResponse.getWriter());
	}
	
	@Override
	public void doGet(HttpServletRequest httpRequest, HttpServletResponse httpResponse) throws IOException {
		this.doPost(httpRequest, httpResponse);
	}
	
}
