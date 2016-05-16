package com.weizhu.webapp.mobile.offline_training;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.common.util.concurrent.Futures;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.weizhu.common.utils.JsonUtil;
import com.weizhu.proto.OfflineTrainingService;
import com.weizhu.proto.OfflineTrainingProtos.LeaveTrainRequest;
import com.weizhu.proto.OfflineTrainingProtos.LeaveTrainResponse;
import com.weizhu.proto.WeizhuProtos.RequestHead;
import com.weizhu.web.ParamUtil;

@Singleton
@SuppressWarnings("serial")
public class LeaveTrainServlet extends HttpServlet {

	private final Provider<RequestHead> requestHeadProvider;
	private final OfflineTrainingService offlineTrainingService;
	
	@Inject
	public LeaveTrainServlet(Provider<RequestHead> requestHeadProvider, OfflineTrainingService offlineTrainingService) {
		this.requestHeadProvider = requestHeadProvider;
		this.offlineTrainingService = offlineTrainingService;
	}
	
	@Override
	protected void doPost(HttpServletRequest httpRequest, HttpServletResponse httpResponse) throws ServletException, IOException {
		final int trainId = ParamUtil.getInt(httpRequest, "train_id", 0);
		final boolean isCancel = ParamUtil.getBoolean(httpRequest, "is_cancel", false);
		final String leaveReason = ParamUtil.getString(httpRequest, "leave_reason", null);
		
		final RequestHead head = this.requestHeadProvider.get();
		
		LeaveTrainRequest.Builder requestBuilder = LeaveTrainRequest.newBuilder()
				.setTrainId(trainId)
				.setIsCancel(isCancel);
		if (leaveReason != null) {
			requestBuilder.setLeaveReason(leaveReason);
		}
		
		LeaveTrainResponse response = Futures.getUnchecked(this.offlineTrainingService.leaveTrain(head, requestBuilder.build()));
		
		httpResponse.setContentType("application/json;charset=UTF-8");
		JsonUtil.PROTOBUF_JSON_FORMAT.print(response, httpResponse.getWriter());
	}

}
