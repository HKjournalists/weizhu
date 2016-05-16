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
import com.weizhu.proto.AdminOfflineTrainingProtos.UpdateTrainRequest;
import com.weizhu.proto.AdminOfflineTrainingProtos.UpdateTrainResponse;
import com.weizhu.proto.AdminProtos.AdminHead;
import com.weizhu.web.ParamUtil;

@Singleton
@SuppressWarnings("serial")
public class UpdateTrainServlet extends HttpServlet {
	
	private final Provider<AdminHead> adminHeadProvider;
	private final AdminOfflineTrainingService adminOfflineTrainingService;
	
	@Inject
	public UpdateTrainServlet(Provider<AdminHead> adminHeadProvider, 
			AdminOfflineTrainingService adminOfflineTrainingService
			) {
		this.adminHeadProvider = adminHeadProvider;
		this.adminOfflineTrainingService = adminOfflineTrainingService;
	}
	
	@Override
	protected void doPost(HttpServletRequest httpRequest, HttpServletResponse httpResponse) throws ServletException, IOException {
		final int trainId = ParamUtil.getInt(httpRequest, "train_id", 0);
		final String trainName = ParamUtil.getString(httpRequest, "train_name", "");
		final String imageName = ParamUtil.getString(httpRequest, "image_name", null);
		final int startTime = ParamUtil.getInt(httpRequest, "start_time", 0);
		final int endTime = ParamUtil.getInt(httpRequest, "end_time", 0);
		final boolean applyEnable = ParamUtil.getBoolean(httpRequest, "apply_enable", false);
		final Integer applyStartTime = ParamUtil.getInt(httpRequest, "apply_start_time", null);
		final Integer applyEndTime = ParamUtil.getInt(httpRequest, "apply_end_time", null);
		final Integer applyUserCount = ParamUtil.getInt(httpRequest, "apply_user_count", null);
		final String trainAddress = ParamUtil.getString(httpRequest, "train_address", "");
		final String lecturerName = ParamUtil.getString(httpRequest, "lecturer_name", null);
		final List<Long> lecturerUserIdList = ParamUtil.getLongList(httpRequest, "lecturer_user_id", Collections.<Long>emptyList());
		final int checkInStartTime = ParamUtil.getInt(httpRequest, "check_in_start_time", 0);
		final int checkInEndTime = ParamUtil.getInt(httpRequest, "check_in_end_time", 0);
		final String arrangementText = ParamUtil.getString(httpRequest, "arrangement_text", "");
		final String describeText = ParamUtil.getString(httpRequest, "describe_text", null);
		final Integer allowModelId = ParamUtil.getInt(httpRequest, "allow_model_id", null);
		final OfflineTrainingProtos.State state = ParamUtil.getEnum(httpRequest, OfflineTrainingProtos.State.class, "state", null);
		final boolean enableNotifyUser = ParamUtil.getBoolean(httpRequest, "enable_notify_user", false);
		
		final AdminHead head = adminHeadProvider.get();
		
		UpdateTrainRequest.Builder requestBuilder = UpdateTrainRequest.newBuilder();
		requestBuilder.setTrainId(trainId);
		requestBuilder.setTrainName(trainName);
		if (imageName != null) {
			requestBuilder.setImageName(imageName);
		}
		requestBuilder.setStartTime(startTime);
		requestBuilder.setEndTime(endTime);
		requestBuilder.setApplyEnable(applyEnable);
		if (applyStartTime != null) {
			requestBuilder.setApplyStartTime(applyStartTime);
		}
		if (applyEndTime != null) {
			requestBuilder.setApplyEndTime(applyEndTime);
		}
		if (applyUserCount != null) {
			requestBuilder.setApplyUserCount(applyUserCount);
		}
		requestBuilder.setTrainAddress(trainAddress);
		if (lecturerName != null) {
			requestBuilder.setLecturerName(lecturerName);
		}
		requestBuilder.addAllLecturerUserId(lecturerUserIdList);
		requestBuilder.setCheckInStartTime(checkInStartTime);
		requestBuilder.setCheckInEndTime(checkInEndTime);
		requestBuilder.setArrangementText(arrangementText);
		if (describeText != null) {
			requestBuilder.setDescribeText(describeText);
		}
		if (allowModelId != null) {
			requestBuilder.setAllowModelId(allowModelId);
		}
		if (state != null) {
			requestBuilder.setState(state);
		}
		requestBuilder.setEnableNotifyUser(enableNotifyUser);
		
		UpdateTrainResponse response = Futures.getUnchecked(this.adminOfflineTrainingService.updateTrain(head, requestBuilder.build()));
		
		httpResponse.setContentType("application/json;charset=UTF-8");
		JsonUtil.PROTOBUF_JSON_FORMAT.print(response, httpResponse.getWriter());
	}

}
