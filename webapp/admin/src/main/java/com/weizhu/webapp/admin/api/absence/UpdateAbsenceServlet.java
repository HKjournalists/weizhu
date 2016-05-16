package com.weizhu.webapp.admin.api.absence;

import java.io.IOException;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.common.util.concurrent.Futures;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.weizhu.proto.AbsenceProtos.UpdateAbsenceRequest;
import com.weizhu.proto.AbsenceProtos.UpdateAbsenceResponse;
import com.weizhu.common.utils.JsonUtil;
import com.weizhu.proto.AbsenceService;
import com.weizhu.proto.AdminProtos.AdminHead;
import com.weizhu.web.ParamUtil;

@Singleton
public class UpdateAbsenceServlet extends HttpServlet {

	private static final long serialVersionUID = 1L;

	private final Provider<AdminHead> adminHeadProvider;
	private final AbsenceService absenceService;
	
	@Inject
	public UpdateAbsenceServlet(Provider<AdminHead> adminHeadProvider, AbsenceService absenceService) {
		this.adminHeadProvider = adminHeadProvider;
		this.absenceService = absenceService;
	}
	
	@Override
	public void doPost(HttpServletRequest httpRequest, HttpServletResponse httpResponse) throws IOException {
		final int absenceId = ParamUtil.getInt(httpRequest, "absence_id", 0);
		final String type = ParamUtil.getString(httpRequest, "type", "");
		final int startTime = ParamUtil.getInt(httpRequest, "start_time", 0);
		final int endTime = ParamUtil.getInt(httpRequest, "pre_end_time", 0);
		final Integer facEndTime = ParamUtil.getInt(httpRequest, "fac_end_time", null);
		final String days = ParamUtil.getString(httpRequest, "days", "");
		final String desc = ParamUtil.getString(httpRequest, "desc", "");
		
		final AdminHead head = adminHeadProvider.get();
		
		UpdateAbsenceRequest.Builder requestBuilder = UpdateAbsenceRequest.newBuilder()
				.setAbsenceId(absenceId)
				.setType(type)
				.setStartTime(startTime)
				.setPreEndTime(endTime)
				.setDays(days)
				.setDesc(desc);
		if (facEndTime != null) {
			requestBuilder.setFacEndTime(facEndTime);
		}
				
		UpdateAbsenceResponse response = Futures.getUnchecked(absenceService.updateAbsence(head, requestBuilder.build()));
		
		httpResponse.setContentType("application/json;charset=UTF-8");
		JsonUtil.PROTOBUF_JSON_FORMAT.print(response, httpResponse.getWriter());
	}
	
	@Override
	public void doGet(HttpServletRequest httpRequest, HttpServletResponse httpResponse) throws IOException {
		this.doPost(httpRequest, httpResponse);
	}
}
