package com.weizhu.webapp.mobile.absence;

import java.io.IOException;
import java.util.List;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.common.collect.Lists;
import com.google.common.util.concurrent.Futures;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.weizhu.common.utils.JsonUtil;
import com.weizhu.proto.AbsenceProtos.CreateAbsenceRequest;
import com.weizhu.proto.AbsenceProtos.CreateAbsenceResponse;
import com.weizhu.proto.AbsenceService;
import com.weizhu.proto.WeizhuProtos.RequestHead;
import com.weizhu.web.ParamUtil;

@Singleton
public class CreateAbsenceServlet extends HttpServlet {

	private static final long serialVersionUID = 1L;

	private final Provider<RequestHead> requestHeadProvider;
	private final AbsenceService absenceService;
	
	@Inject
	public CreateAbsenceServlet(Provider<RequestHead> requestHeadProvider, AbsenceService absenceService) {
		this.requestHeadProvider = requestHeadProvider;
		this.absenceService = absenceService;
	}

	@Override
	public void doPost(HttpServletRequest httpRequest, HttpServletResponse httpResponse) throws IOException {
		final String type = ParamUtil.getString(httpRequest, "type", "");
		final int startTime = ParamUtil.getInt(httpRequest, "start_time", 0);
		final int endTime = ParamUtil.getInt(httpRequest, "end_time", 0);
		final String desc = ParamUtil.getString(httpRequest, "desc", "");
		final String days = ParamUtil.getString(httpRequest, "days", "");
		final List<Long> userIdList = ParamUtil.getLongList(httpRequest, "user_id_list", Lists.newArrayList());
		
		final RequestHead head = requestHeadProvider.get();
		
		CreateAbsenceResponse response = Futures.getUnchecked(absenceService.createAbsence(head, CreateAbsenceRequest.newBuilder()
				.setType(type)
				.setStartTime(startTime)
				.setEndTime(endTime)
				.setDesc(desc)
				.setDays(days)
				.addAllUserId(userIdList)
				.build()));
		
		httpResponse.setContentType("application/json;charset=UTF-8");
		JsonUtil.PROTOBUF_JSON_FORMAT.print(response, httpResponse.getWriter());
		
	}
	
	@Override
	public void doGet(HttpServletRequest httpRequest, HttpServletResponse httpResponse) throws IOException {
		this.doPost(httpRequest, httpResponse);
	}
	
}
