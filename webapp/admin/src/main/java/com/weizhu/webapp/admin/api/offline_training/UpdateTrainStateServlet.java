package com.weizhu.webapp.admin.api.offline_training;

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
import com.weizhu.proto.AdminOfflineTrainingService;
import com.weizhu.proto.OfflineTrainingProtos;
import com.weizhu.proto.AdminOfflineTrainingProtos.UpdateTrainStateRequest;
import com.weizhu.proto.AdminOfflineTrainingProtos.UpdateTrainStateResponse;
import com.weizhu.proto.AdminProtos.AdminHead;
import com.weizhu.web.ParamUtil;

@Singleton
@SuppressWarnings("serial")
public class UpdateTrainStateServlet extends HttpServlet {
	
	private final Provider<AdminHead> adminHeadProvider;
	private final AdminOfflineTrainingService adminOfflineTrainingService;
	
	@Inject
	public UpdateTrainStateServlet(Provider<AdminHead> adminHeadProvider, 
			AdminOfflineTrainingService adminOfflineTrainingService
			) {
		this.adminHeadProvider = adminHeadProvider;
		this.adminOfflineTrainingService = adminOfflineTrainingService;
	}
	
	@Override
	protected void doPost(HttpServletRequest httpRequest, HttpServletResponse httpResponse) throws ServletException, IOException {
		final List<Integer> trainIdList = ParamUtil.getIntList(httpRequest, "train_id", Collections.<Integer>emptyList());
		final OfflineTrainingProtos.State state = ParamUtil.getEnum(httpRequest, OfflineTrainingProtos.State.class, "state", OfflineTrainingProtos.State.NORMAL);
		
		final AdminHead head = adminHeadProvider.get();
		
		UpdateTrainStateRequest request = UpdateTrainStateRequest.newBuilder()
				.addAllTrainId(trainIdList)
				.setState(state)
				.build();
		
		UpdateTrainStateResponse response = Futures.getUnchecked(this.adminOfflineTrainingService.updateTrainState(head, request));
		
		httpResponse.setContentType("application/json;charset=UTF-8");
		JsonUtil.PROTOBUF_JSON_FORMAT.print(response, httpResponse.getWriter());
	}

}
