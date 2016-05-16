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
import com.weizhu.proto.OfflineTrainingProtos.CheckInTrainRequest;
import com.weizhu.proto.OfflineTrainingProtos.CheckInTrainResponse;
import com.weizhu.proto.WeizhuProtos.RequestHead;
import com.weizhu.web.ParamUtil;

@Singleton
@SuppressWarnings("serial")
public class CheckInTrainServlet extends HttpServlet {

	private final Provider<RequestHead> requestHeadProvider;
	private final OfflineTrainingService offlineTrainingService;
	
	@Inject
	public CheckInTrainServlet(Provider<RequestHead> requestHeadProvider, OfflineTrainingService offlineTrainingService) {
		this.requestHeadProvider = requestHeadProvider;
		this.offlineTrainingService = offlineTrainingService;
	}
	
	@Override
	protected void doPost(HttpServletRequest httpRequest, HttpServletResponse httpResponse) throws ServletException, IOException {
		final int trainId = ParamUtil.getInt(httpRequest, "train_id", 0);
		
		final RequestHead head = this.requestHeadProvider.get();
		
		CheckInTrainRequest request = CheckInTrainRequest.newBuilder()
				.setTrainId(trainId)
				.build();
		
		CheckInTrainResponse response = Futures.getUnchecked(this.offlineTrainingService.checkInTrain(head, request));
		
		httpResponse.setContentType("application/json;charset=UTF-8");
		JsonUtil.PROTOBUF_JSON_FORMAT.print(response, httpResponse.getWriter());
	}

}
