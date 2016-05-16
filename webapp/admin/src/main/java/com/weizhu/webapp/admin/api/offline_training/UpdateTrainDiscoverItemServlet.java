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
import com.weizhu.proto.AdminOfflineTrainingProtos.UpdateTrainDiscoverItemRequest;
import com.weizhu.proto.AdminOfflineTrainingProtos.UpdateTrainDiscoverItemResponse;
import com.weizhu.proto.AdminProtos.AdminHead;
import com.weizhu.web.ParamUtil;

@Singleton
@SuppressWarnings("serial")
public class UpdateTrainDiscoverItemServlet extends HttpServlet {
	
	private final Provider<AdminHead> adminHeadProvider;
	private final AdminOfflineTrainingService adminOfflineTrainingService;
	
	@Inject
	public UpdateTrainDiscoverItemServlet(Provider<AdminHead> adminHeadProvider, 
			AdminOfflineTrainingService adminOfflineTrainingService
			) {
		this.adminHeadProvider = adminHeadProvider;
		this.adminOfflineTrainingService = adminOfflineTrainingService;
	}
	
	@Override
	protected void doPost(HttpServletRequest httpRequest, HttpServletResponse httpResponse) throws ServletException, IOException {
		final int trainId = ParamUtil.getInt(httpRequest, "train_id", 0);
		final List<Long> discoverItemIdList = ParamUtil.getLongList(httpRequest, "discover_item_id", Collections.<Long>emptyList());
		
		final AdminHead head = adminHeadProvider.get();
		
		UpdateTrainDiscoverItemRequest request = UpdateTrainDiscoverItemRequest.newBuilder()
				.setTrainId(trainId)
				.addAllDiscoverItemId(discoverItemIdList)
				.build();
		
		UpdateTrainDiscoverItemResponse response = Futures.getUnchecked(this.adminOfflineTrainingService.updateTrainDiscoverItem(head, request));
		
		httpResponse.setContentType("application/json;charset=UTF-8");
		JsonUtil.PROTOBUF_JSON_FORMAT.print(response, httpResponse.getWriter());
	}
}
