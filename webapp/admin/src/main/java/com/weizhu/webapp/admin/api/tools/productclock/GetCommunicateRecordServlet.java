package com.weizhu.webapp.admin.api.tools.productclock;

import java.io.IOException;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.common.util.concurrent.Futures;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.weizhu.common.utils.JsonUtil;
import com.weizhu.proto.AdminProtos.AdminHead;
import com.weizhu.proto.AdminUserProtos.GetUserByIdRequest;
import com.weizhu.proto.AdminUserProtos.GetUserByIdResponse;
import com.weizhu.proto.AdminUserService;
import com.weizhu.proto.ProductclockProtos;
import com.weizhu.proto.ProductclockProtos.GetCommunicateRecordAdminRequest;
import com.weizhu.proto.ProductclockProtos.GetCommunicateRecordAdminResponse;
import com.weizhu.proto.ToolsProductclockService;
import com.weizhu.proto.UserProtos;
import com.weizhu.web.ParamUtil;

@Singleton
public class GetCommunicateRecordServlet extends HttpServlet {

	private static final long serialVersionUID = 1L;

	private final Provider<AdminHead> adminHeadProvider;
	private final ToolsProductclockService toolsProductclockService;
	private final AdminUserService adminUserService;
	
	@Inject
	public GetCommunicateRecordServlet(Provider<AdminHead> adminHeadProvider, ToolsProductclockService toolsProductclockService,
			AdminUserService adminUserService) {
		this.adminHeadProvider = adminHeadProvider;
		this.toolsProductclockService = toolsProductclockService;
		this.adminUserService = adminUserService;
	}
	
	@Override
	public void doPost(HttpServletRequest httpRequest, HttpServletResponse httpResponse) throws IOException {
		final int customerId = ParamUtil.getInt(httpRequest, "customer_id", 0);
		final int start = ParamUtil.getInt(httpRequest, "start", 0);
		final int length = ParamUtil.getInt(httpRequest, "length", 10);
		
		final AdminHead adminHead = adminHeadProvider.get();
		
		GetCommunicateRecordAdminResponse response = Futures.getUnchecked(toolsProductclockService.getCommunicateRecordAdmin(adminHead, GetCommunicateRecordAdminRequest.newBuilder()
				.setCustomerId(customerId)
				.setStart(start)
				.setLength(length)
				.build()));
		Set<Long> userIdSet = Sets.newHashSet();
		for (ProductclockProtos.CommunicateRecord communicateRecord : response.getCommunicateRecordList()) {
			userIdSet.add(communicateRecord.getUserId());
		}
		
		GetUserByIdResponse getUserByIdResponse = Futures.getUnchecked(adminUserService.getUserById(adminHead, GetUserByIdRequest.newBuilder()
				.addAllUserId(userIdSet)
				.build()));
		Map<Long, UserProtos.User> userMap = Maps.newHashMap();
		for (UserProtos.User user : getUserByIdResponse.getUserList()) {
			userMap.put(user.getBase().getUserId(), user);
		}
		
		JsonArray array = new JsonArray();
		for (ProductclockProtos.CommunicateRecord communicateRecord : response.getCommunicateRecordList()) {
			JsonObject obj = new JsonObject();
			
			obj.addProperty("record_id", communicateRecord.getRecordId());
			obj.addProperty("content_text", communicateRecord.getContentText());
			obj.addProperty("create_time", communicateRecord.getCreateTime());
			UserProtos.User user = userMap.get(communicateRecord.getUserId());
			if (user != null) {
				obj.addProperty("user_name", user.getBase().getUserName());
			}
			array.add(obj);
		}
		
		JsonObject result = new JsonObject();
		result.add("communicate_record", array);
		result.addProperty("total_size", response.getTotalSize());
		result.addProperty("filtered_size", response.getFilteredSize());
		
		httpResponse.setContentType("application/json;charset=UTF-8");
		JsonUtil.GSON.toJson(result, httpResponse.getWriter());
	}
	
	@Override
	public void doGet(HttpServletRequest httpRequest, HttpServletResponse httpResponse) throws IOException {
		this.doPost(httpRequest, httpResponse);
	}
}
