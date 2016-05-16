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
import com.weizhu.proto.AdminOfficialProtos.GetOfficialByIdRequest;
import com.weizhu.proto.AdminOfficialProtos.GetOfficialByIdResponse;
import com.weizhu.proto.AdminOfficialProtos.GetOfficialMessageRequest;
import com.weizhu.proto.AdminOfficialProtos.GetOfficialMessageResponse;
import com.weizhu.proto.AdminOfficialService;
import com.weizhu.proto.AdminUserProtos.GetUserByIdResponse;
import com.weizhu.proto.AdminUserService;
import com.weizhu.proto.OfficialProtos;
import com.weizhu.proto.UploadService;
import com.weizhu.proto.UserProtos;
import com.weizhu.proto.AdminProtos.AdminHead;
import com.weizhu.proto.AdminUserProtos.GetUserByIdRequest;
import com.weizhu.web.ParamUtil;

@Singleton
@SuppressWarnings("serial")
public class GetOfficialRecvMessageServlet extends HttpServlet {
	
	private final Provider<AdminHead> adminHeadProvider;
	private final AdminOfficialService adminOfficialService;
	private final AdminUserService adminUserService;
	private final UploadService uploadService;
	
	@Inject
	public GetOfficialRecvMessageServlet(Provider<AdminHead> adminHeadProvider, 
			AdminOfficialService adminOfficialService,
			AdminUserService adminUserService,
			UploadService uploadService
			) {
		this.adminHeadProvider = adminHeadProvider;
		this.adminOfficialService = adminOfficialService;
		this.adminUserService = adminUserService;
		this.uploadService = uploadService;
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
		long officialId = ParamUtil.getLong(httpRequest, "official_id", -1L);
		
		// 2. 调用Service
		
		final AdminHead head = adminHeadProvider.get();
		
		GetOfficialByIdResponse getOfficialByIdResponse = Futures.getUnchecked(
				adminOfficialService.getOfficialById(head, 
						GetOfficialByIdRequest.newBuilder()
							.addOfficialId(officialId)
							.build()));
		
		Map<Long, OfficialProtos.Official> refOfficialMap = new TreeMap<Long, OfficialProtos.Official>();
		for (OfficialProtos.Official official : getOfficialByIdResponse.getOfficialList()) {
			refOfficialMap.put(official.getOfficialId(), official);
		}
		
		GetOfficialMessageRequest.Builder requestBuilder = GetOfficialMessageRequest.newBuilder()
				.setOfficialId(officialId)
				.setStart(start)
				.setLength(length)
				.setIsFromUser(true);
		
		GetOfficialMessageResponse response = Futures.getUnchecked(adminOfficialService.getOfficialMessage(head, requestBuilder.build()));
		
		Set<Long> refUserIdSet = new TreeSet<Long>();
		for (AdminOfficialProtos.OfficialMessageInfo msgInfo : response.getMsgInfoList()) {
			refUserIdSet.add(msgInfo.getUserId());
			
			if (msgInfo.getMsg().getMsgTypeCase() == OfficialProtos.OfficialMessage.MsgTypeCase.USER) {
				refUserIdSet.add(msgInfo.getMsg().getUser().getUserId());
			}
		}
		
		Map<Long, UserProtos.User> refUserMap;
		if (refUserIdSet.isEmpty()) {
			refUserMap = Collections.emptyMap();
		} else {
			GetUserByIdResponse getUserByIdResponse = Futures.getUnchecked(
					adminUserService.getUserById(head, GetUserByIdRequest.newBuilder()
					.addAllUserId(refUserIdSet)
					.build()));
			refUserMap = new TreeMap<Long, UserProtos.User>();
			for (UserProtos.User user : getUserByIdResponse.getUserList()) {
				refUserMap.put(user.getBase().getUserId(), user);
			}
		}
		
		String imageUrlPrefix = Futures.getUnchecked(this.uploadService.getUploadUrlPrefix(head, ServiceUtil.EMPTY_REQUEST)).getImageUrlPrefix();

		JsonArray data = new JsonArray();
		for (AdminOfficialProtos.OfficialMessageInfo msgInfo : response.getMsgInfoList()) {
			OfficialProtos.Official official = refOfficialMap.get(msgInfo.getOfficialId());
			UserProtos.User user = refUserMap.get(msgInfo.getUserId());
			
			JsonObject m = OfficialUtil.buildJsonObject(msgInfo.getMsg(), refUserMap, imageUrlPrefix);

			m.addProperty("user_id", msgInfo.getUserId());
			m.addProperty("user_name", user != null ? user.getBase().getUserName() : "[UserId:" + msgInfo.getUserId() + "]");
			
			m.addProperty("official_id", msgInfo.getOfficialId());
			m.addProperty("official_name", official != null ? official.getOfficialName() : "[OfficialId:" + msgInfo.getOfficialId() + "]");
			
			data.add(m);
		}
		
		JsonObject result = new JsonObject();
		result.addProperty("draw", draw);
		result.addProperty("recordsTotal", response.getFilteredSize());
		result.addProperty("recordsFiltered", response.getFilteredSize());
		result.add("data", data);
		
		httpResponse.setContentType("application/json;charset=UTF-8");
		JsonUtil.GSON.toJson(result, httpResponse.getWriter());
	}

}
