package com.weizhu.webapp.admin.api.official;

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
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.weizhu.common.service.ServiceUtil;
import com.weizhu.common.utils.JsonUtil;
import com.weizhu.proto.AdminOfficialProtos.GetOfficialByIdRequest;
import com.weizhu.proto.AdminOfficialProtos.GetOfficialByIdResponse;
import com.weizhu.proto.AdminOfficialService;
import com.weizhu.proto.AdminProtos;
import com.weizhu.proto.AdminProtos.AdminHead;
import com.weizhu.proto.AdminProtos.GetAdminByIdRequest;
import com.weizhu.proto.AdminProtos.GetAdminByIdResponse;
import com.weizhu.proto.AllowProtos.GetModelByIdRequest;
import com.weizhu.proto.AllowProtos.GetModelByIdResponse;
import com.weizhu.proto.AdminService;
import com.weizhu.proto.AllowProtos;
import com.weizhu.proto.AllowService;
import com.weizhu.proto.OfficialProtos;
import com.weizhu.proto.UploadService;
import com.weizhu.web.ParamUtil;

@Singleton
@SuppressWarnings("serial")
public class GetOfficialByIdServlet extends HttpServlet {
	
	private final Provider<AdminHead> adminHeadProvider;
	private final AdminOfficialService adminOfficialService;
	private final AdminService adminService;
	private final UploadService uploadService;
	private final AllowService allowService;
	
	@Inject
	public GetOfficialByIdServlet(
			Provider<AdminHead> adminHeadProvider, 
			AdminOfficialService adminOfficialService,
			AdminService adminService,
			UploadService uploadService,
			AllowService allowService
			) {
		this.adminHeadProvider = adminHeadProvider;
		this.adminOfficialService = adminOfficialService;
		this.adminService = adminService;
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
		List<Long> officialIdList = ParamUtil.getLongList(httpRequest, "official_id", Collections.<Long>emptyList());
		
		// 2. 调用Service
		
		final AdminHead head = adminHeadProvider.get();
		
		GetOfficialByIdRequest request = GetOfficialByIdRequest.newBuilder()
				.addAllOfficialId(officialIdList)
				.build();
		
		GetOfficialByIdResponse response = Futures.getUnchecked(adminOfficialService.getOfficialById(head, request));
		
		Set<Integer> allowModelIdSet = new TreeSet<Integer>();
		Set<Long> refAdminIdSet = new TreeSet<Long>();
		for (OfficialProtos.Official official : response.getOfficialList()) {
			if (official.hasAllowModelId()) {
				allowModelIdSet.add(official.getAllowModelId());
			}
			if (official.hasCreateAdminId()) {
				refAdminIdSet.add(official.getCreateAdminId());
			}
			if (official.hasUpdateAdminId()) {
				refAdminIdSet.add(official.getUpdateAdminId());
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
		
		String imageUrlPrefix = Futures.getUnchecked(this.uploadService.getUploadUrlPrefix(head, ServiceUtil.EMPTY_REQUEST)).getImageUrlPrefix();

		JsonArray data = new JsonArray();
		for (OfficialProtos.Official official : response.getOfficialList()) {
			data.add(OfficialUtil.buildJsonObject(official, allowModelMap, refAdminMap, imageUrlPrefix));
		}		

		httpResponse.setContentType("application/json;charset=UTF-8");
		JsonUtil.GSON.toJson(data, httpResponse.getWriter());
	}

}
