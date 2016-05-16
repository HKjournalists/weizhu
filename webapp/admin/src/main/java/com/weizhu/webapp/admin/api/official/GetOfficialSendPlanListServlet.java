package com.weizhu.webapp.admin.api.official;

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
import com.weizhu.common.service.ServiceUtil;
import com.weizhu.common.utils.JsonUtil;
import com.weizhu.proto.AdminOfficialProtos;
import com.weizhu.proto.AdminOfficialProtos.GetOfficialSendPlanListRequest;
import com.weizhu.proto.AdminOfficialProtos.GetOfficialSendPlanListResponse;
import com.weizhu.proto.AdminOfficialService;
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
import com.weizhu.proto.OfficialProtos;
import com.weizhu.proto.UploadService;
import com.weizhu.proto.UserProtos;
import com.weizhu.web.ParamUtil;

@Singleton
@SuppressWarnings("serial")
public class GetOfficialSendPlanListServlet extends HttpServlet {
	
	private final Provider<AdminHead> adminHeadProvider;
	private final AdminOfficialService adminOfficialService;
	private final AdminService adminService;
	private final AdminUserService adminUserService;
	private final UploadService uploadService;
	private final AllowService allowService;
	
	@Inject
	public GetOfficialSendPlanListServlet(
			Provider<AdminHead> adminHeadProvider, 
			AdminOfficialService adminOfficialService,
			AdminService adminService,
			AdminUserService adminUserService,
			UploadService uploadService,
			AllowService allowService) {
		this.adminHeadProvider = adminHeadProvider;
		this.adminOfficialService = adminOfficialService;
		this.adminService = adminService;
		this.adminUserService = adminUserService;
		this.uploadService = uploadService;
		this.allowService = allowService;
	}
	
	@Override
	protected void doGet(HttpServletRequest httpRequest, HttpServletResponse httpResponse) throws ServletException, IOException {
		this.doPost(httpRequest, httpResponse);
	}

	@Override
	protected void doPost(HttpServletRequest httpRequest, HttpServletResponse httpResponse) throws ServletException, IOException {

		// 1. 取出参数
		int draw = ParamUtil.getInt(httpRequest, "draw", 1);
		int start = ParamUtil.getInt(httpRequest, "start", 0);
		int length = ParamUtil.getInt(httpRequest, "length", 10);
		Long officialId = ParamUtil.getLong(httpRequest, "official_id", null);
		
		// 2. 调用Service
		
		final AdminHead head = adminHeadProvider.get();
		
		GetOfficialSendPlanListRequest.Builder requestBuilder = GetOfficialSendPlanListRequest.newBuilder()
				.setStart(start)
				.setLength(length);
		if (officialId != null) {
			requestBuilder.setOfficialId(officialId);
		}
		
		GetOfficialSendPlanListResponse response = Futures.getUnchecked(adminOfficialService.getOfficialSendPlanList(head, requestBuilder.build()));
		
		Map<Long, OfficialProtos.Official> refOfficialMap = new TreeMap<Long, OfficialProtos.Official>();
		for (OfficialProtos.Official official : response.getRefOfficialList()) {
			refOfficialMap.put(official.getOfficialId(), official);
		}
		
		Set<Integer> allowModelIdSet = new TreeSet<Integer>();
		Set<Long> refAdminIdSet = new TreeSet<Long>();
		Set<Long> refUserIdSet = new TreeSet<Long>();
		for (AdminOfficialProtos.OfficialSendPlan sendPlan : response.getOfficialSendPlanList()) {
			if (sendPlan.hasAllowModelId()) {
				allowModelIdSet.add(sendPlan.getAllowModelId());
			}
			if (sendPlan.hasCreateAdminId()) {
				refAdminIdSet.add(sendPlan.getCreateAdminId());
			}
			if (sendPlan.hasUpdateAdminId()) {
				refAdminIdSet.add(sendPlan.getUpdateAdminId());
			}
			if (sendPlan.getSendMsg().getMsgTypeCase().equals(OfficialProtos.OfficialMessage.MsgTypeCase.USER)) {
				refUserIdSet.add(sendPlan.getSendMsg().getUser().getUserId());
			}
		}
		
		Map<Integer, AllowProtos.Model> allowModelMap;
		if (allowModelIdSet.isEmpty()) {
			allowModelMap = Collections.emptyMap();
		} else {
			GetModelByIdResponse getModelByIdResponse = Futures.getUnchecked(
					allowService.getModelById(head, 
							GetModelByIdRequest.newBuilder().addAllModelId(allowModelIdSet).build()
							));
			
			allowModelMap = new TreeMap<Integer, AllowProtos.Model>();
			for (AllowProtos.Model model : getModelByIdResponse.getModelList()) {
				allowModelMap.put(model.getModelId(), model);
			}
		}
		
		Map<Long, AdminProtos.Admin> refAdminMap;
		if (refAdminIdSet.isEmpty()) {
			refAdminMap = Collections.emptyMap();
		} else {
			
			GetAdminByIdResponse getAdminByIdResponse = Futures.getUnchecked(
					adminService.getAdminById(head, 
							GetAdminByIdRequest.newBuilder().addAllAdminId(refAdminIdSet).build()
							));
			
			refAdminMap = new TreeMap<Long, AdminProtos.Admin>();
			for (AdminProtos.Admin admin : getAdminByIdResponse.getAdminList()) {
				refAdminMap.put(admin.getAdminId(), admin);
			}
		}
		
		Map<Long, UserProtos.User> refUserMap;
		if (!refUserIdSet.isEmpty()) {
			GetUserByIdResponse getUserByIdResponse = Futures.getUnchecked(
					this.adminUserService.getUserById(head, GetUserByIdRequest.newBuilder()
							.addAllUserId(refUserIdSet)
							.build()));
			refUserMap = new TreeMap<Long, UserProtos.User>();
			for (UserProtos.User user : getUserByIdResponse.getUserList()) {
				refUserMap.put(user.getBase().getUserId(), user);
			}
		} else {
			refUserMap = Collections.emptyMap();
		}
		
		String imageUrlPrefix = Futures.getUnchecked(this.uploadService.getUploadUrlPrefix(head, ServiceUtil.EMPTY_REQUEST)).getImageUrlPrefix();

		JsonArray data = new JsonArray();
		for (AdminOfficialProtos.OfficialSendPlan sendPlan : response.getOfficialSendPlanList()) {
			data.add(OfficialUtil.buildJsonObject(sendPlan, allowModelMap, refOfficialMap, refAdminMap, refUserMap, imageUrlPrefix));
		}		

		JsonObject result = new JsonObject();
		result.addProperty("draw", draw);
		result.addProperty("recordsTotal", response.getTotalSize());
		result.addProperty("recordsFiltered", response.getFilteredSize());
		result.add("data", data);
		
		httpResponse.setContentType("application/json;charset=UTF-8");
		JsonUtil.GSON.toJson(result, httpResponse.getWriter());
	}

}
