package com.weizhu.webapp.admin.api.offline_training;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
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
import com.weizhu.proto.AdminOfflineTrainingProtos.GetTrainByIdRequest;
import com.weizhu.proto.AdminOfflineTrainingProtos.GetTrainByIdResponse;
import com.weizhu.proto.AdminOfflineTrainingService;
import com.weizhu.proto.AdminProtos;
import com.weizhu.proto.AdminProtos.AdminHead;
import com.weizhu.proto.AdminProtos.GetAdminByIdRequest;
import com.weizhu.proto.AdminProtos.GetAdminByIdResponse;
import com.weizhu.proto.AdminService;
import com.weizhu.proto.AdminUserProtos.GetUserByIdRequest;
import com.weizhu.proto.AdminUserProtos.GetUserByIdResponse;
import com.weizhu.proto.AllowProtos.GetModelByIdRequest;
import com.weizhu.proto.AllowProtos.GetModelByIdResponse;
import com.weizhu.proto.AdminUserService;
import com.weizhu.proto.AllowProtos;
import com.weizhu.proto.AllowService;
import com.weizhu.proto.OfflineTrainingProtos;
import com.weizhu.proto.UserProtos;
import com.weizhu.web.ParamUtil;

@Singleton
@SuppressWarnings("serial")
public class GetTrainByIdServlet extends HttpServlet {
	
	private final Provider<AdminHead> adminHeadProvider;
	private final AdminOfflineTrainingService adminOfflineTrainingService;
	private final AdminUserService adminUserService;
	private final AdminService adminService;
	private final AllowService allowService;
	
	@Inject
	public GetTrainByIdServlet(Provider<AdminHead> adminHeadProvider, 
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
		List<Integer> trainIdList = ParamUtil.getIntList(httpRequest, "train_id", Collections.<Integer>emptyList());
		
		// 2. 调用Service
		
		final AdminHead head = adminHeadProvider.get();
		
		GetTrainByIdRequest request = GetTrainByIdRequest.newBuilder()
				.addAllTrainId(trainIdList)
				.build();
		
		GetTrainByIdResponse response = Futures.getUnchecked(this.adminOfflineTrainingService.getTrainById(head, request));
		
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
		JsonArray trainArray = new JsonArray();
		for (OfflineTrainingProtos.Train train : response.getTrainList()) {
			trainArray.add(OfflineTrainingUtil.buildTrain(now, train, refTrainCountMap, refUserMap, refAdminMap, refAllowModelMap));
		}
		
		JsonObject resultObj = new JsonObject();
		resultObj.add("train", trainArray);
		httpResponse.setContentType("application/json;charset=UTF-8");
		JsonUtil.GSON.toJson(resultObj, httpResponse.getWriter());
	}
}
