package com.weizhu.webapp.admin.api.offline_training;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.common.util.concurrent.Futures;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.weizhu.common.utils.JsonUtil;
import com.weizhu.proto.AdminOfflineTrainingService;
import com.weizhu.proto.AdminProtos;
import com.weizhu.proto.AdminService;
import com.weizhu.proto.AdminUserService;
import com.weizhu.proto.AllowProtos;
import com.weizhu.proto.AllowService;
import com.weizhu.proto.OfflineTrainingProtos;
import com.weizhu.proto.UserProtos;
import com.weizhu.proto.AdminOfflineTrainingProtos.GetTrainListRequest;
import com.weizhu.proto.AdminOfflineTrainingProtos.GetTrainListResponse;
import com.weizhu.proto.AdminProtos.AdminHead;
import com.weizhu.proto.AdminProtos.GetAdminByIdRequest;
import com.weizhu.proto.AdminProtos.GetAdminByIdResponse;
import com.weizhu.proto.AdminUserProtos.GetUserByIdRequest;
import com.weizhu.proto.AdminUserProtos.GetUserByIdResponse;
import com.weizhu.proto.AllowProtos.GetModelByIdRequest;
import com.weizhu.proto.AllowProtos.GetModelByIdResponse;
import com.weizhu.web.ParamUtil;

@Singleton
@SuppressWarnings("serial")
public class GetTrainListServlet extends HttpServlet {
	
	private final Provider<AdminHead> adminHeadProvider;
	private final AdminOfflineTrainingService adminOfflineTrainingService;
	private final AdminUserService adminUserService;
	private final AdminService adminService;
	private final AllowService allowService;
	
	@Inject
	public GetTrainListServlet(Provider<AdminHead> adminHeadProvider, 
			AdminOfflineTrainingService adminOfflineTrainingService,
			AdminUserService adminUserService,
			AdminService adminService,
			AllowService allowService
			) {
		this.adminHeadProvider = adminHeadProvider;
		this.adminOfflineTrainingService = adminOfflineTrainingService;
		this.adminUserService = adminUserService;
		this.adminService = adminService;
		this.allowService = allowService;
	}
	
	@Override
	protected void doGet(HttpServletRequest httpRequest, HttpServletResponse httpResponse) throws ServletException, IOException {
		this.doPost(httpRequest, httpResponse);
	}

	@Override
	protected void doPost(HttpServletRequest httpRequest, HttpServletResponse httpResponse) throws ServletException, IOException {

		// 1. 取出参数
		final int draw = ParamUtil.getInt(httpRequest, "draw", 1);
		final int start = ParamUtil.getInt(httpRequest, "start", 0);
		final int length = ParamUtil.getInt(httpRequest, "length", 10);
		final Integer startTime = ParamUtil.getInt(httpRequest, "start_time", null);
		final Integer endTime = ParamUtil.getInt(httpRequest, "end_time", null);
		final Long createAdminId = ParamUtil.getLong(httpRequest, "create_admin_id", null);
		final OfflineTrainingProtos.State state = ParamUtil.getEnum(httpRequest, OfflineTrainingProtos.State.class, "state", null);;
		final String trainName = ParamUtil.getString(httpRequest, "train_name", null);
		
		// 2. 调用Service
		
		final AdminHead head = adminHeadProvider.get();
		
		GetTrainListRequest.Builder requestBuilder = GetTrainListRequest.newBuilder()
				.setStart(start)
				.setLength(length);
		if (startTime != null) {
			requestBuilder.setStartTime(startTime);
		}
		if (endTime != null) {
			requestBuilder.setEndTime(endTime);
		}
		if (createAdminId != null) {
			requestBuilder.setCreateAdminId(createAdminId);
		}
		if (state != null) {
			requestBuilder.setState(state);
		}
		if (trainName != null && !trainName.isEmpty()) {
			requestBuilder.setTrainName(trainName);
		}
		
		GetTrainListResponse response = Futures.getUnchecked(this.adminOfflineTrainingService.getTrainList(head, requestBuilder.build()));

		final Map<Integer, OfflineTrainingProtos.TrainCount> refTrainCountMap = new TreeMap<Integer, OfflineTrainingProtos.TrainCount>();
		for (OfflineTrainingProtos.TrainCount trainCount : response.getRefTrainCountList()) {
			refTrainCountMap.put(trainCount.getTrainId(), trainCount);
		}
		
		Set<Long> refUserIdSet = new TreeSet<Long>();
		Set<Long> refAdminIdSet = new TreeSet<Long>();
		Set<Integer> refAllowModelIdSet = new TreeSet<Integer>();
		for (OfflineTrainingProtos.Train train : response.getTrainList()) {
			refUserIdSet.addAll(train.getLecturerUserIdList());
			
			if (train.hasCreateAdminId()) {
				refAdminIdSet.add(train.getCreateAdminId());
			}
			if (train.hasUpdateAdminId()) {
				refAdminIdSet.add(train.getUpdateAdminId());
			}
			
			if (train.hasAllowModelId()) {
				refAllowModelIdSet.add(train.getAllowModelId());
			}
		}
		
		final Map<Long, UserProtos.User> refUserMap;
		if (refUserIdSet.isEmpty()) {
			refUserMap = Collections.emptyMap();
		} else {
			GetUserByIdResponse getUserResponse = Futures.getUnchecked(
					this.adminUserService.getUserById(head, 
							GetUserByIdRequest.newBuilder()
							.addAllUserId(refUserIdSet)
							.build()));
			refUserMap = new TreeMap<Long, UserProtos.User>();
			for (UserProtos.User user : getUserResponse.getUserList()) {
				refUserMap.put(user.getBase().getUserId(), user);
			}
		}
		
		final Map<Long, AdminProtos.Admin> refAdminMap;
		if (refAdminIdSet.isEmpty()) {
			refAdminMap = Collections.emptyMap();
		} else {
			GetAdminByIdResponse getAdminResponse = Futures.getUnchecked(
					this.adminService.getAdminById(head, 
							GetAdminByIdRequest.newBuilder()
							.addAllAdminId(refAdminIdSet)
							.build()));
			refAdminMap = new TreeMap<Long, AdminProtos.Admin>();
			for (AdminProtos.Admin admin : getAdminResponse.getAdminList()) {
				refAdminMap.put(admin.getAdminId(), admin);
			}
		}

		final Map<Integer, AllowProtos.Model> refAllowModelMap;
		if (refAllowModelIdSet.isEmpty()) {
			refAllowModelMap = Collections.emptyMap();
		} else {
			GetModelByIdResponse getAllowModelResponse = Futures.getUnchecked(
					this.allowService.getModelById(head, 
							GetModelByIdRequest.newBuilder()
							.addAllModelId(refAllowModelIdSet)
							.build()));
			refAllowModelMap = new TreeMap<Integer, AllowProtos.Model>();
			for (AllowProtos.Model model : getAllowModelResponse.getModelList()) {
				refAllowModelMap.put(model.getModelId(), model);
			}
		}
		
		final int now = (int)(System.currentTimeMillis() / 1000L);
		JsonArray data = new JsonArray();
		for (OfflineTrainingProtos.Train train : response.getTrainList()) {
			data.add(OfflineTrainingUtil.buildTrain(now, train, refTrainCountMap, refUserMap, refAdminMap, refAllowModelMap));
		}
		
		JsonObject result = new JsonObject();
		result.addProperty("draw", draw);
		result.addProperty("recordsTotal", response.getTotalSize());
		result.addProperty("recordsFiltered", response.getTotalSize());
		result.add("data", data);
		
		httpResponse.setContentType("application/json;charset=UTF-8");
		JsonUtil.GSON.toJson(result, httpResponse.getWriter());
	}

}
