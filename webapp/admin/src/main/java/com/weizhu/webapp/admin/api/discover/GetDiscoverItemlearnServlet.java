package com.weizhu.webapp.admin.api.discover;

import java.io.IOException;
import java.util.Map;
import java.util.Set;
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
import com.weizhu.proto.AdminDiscoverProtos;
import com.weizhu.proto.AdminDiscoverService;
import com.weizhu.proto.AdminProtos.AdminHead;
import com.weizhu.proto.AdminUserService;
import com.weizhu.proto.DiscoverV2Protos;
import com.weizhu.proto.UserProtos;
import com.weizhu.web.ParamUtil;

@Singleton
@SuppressWarnings("serial")
public class GetDiscoverItemlearnServlet extends HttpServlet {
	
	private final Provider<AdminHead> adminHeadProvider;
	private final AdminDiscoverService adminDiscoverService;
	private final AdminUserService adminUserService;

	@Inject
	public GetDiscoverItemlearnServlet(Provider<AdminHead> adminHeadProvider, AdminDiscoverService adminDiscoverService, AdminUserService adminUserService) {
		this.adminHeadProvider = adminHeadProvider;
		this.adminDiscoverService = adminDiscoverService;
		this.adminUserService = adminUserService;
	}

	@Override
	protected void doGet(HttpServletRequest httpRequest, HttpServletResponse httpResponse) throws ServletException, IOException {
		this.doPost(httpRequest, httpResponse);
	}

	@Override
	protected void doPost(HttpServletRequest httpRequest, HttpServletResponse httpResponse) throws ServletException, IOException {
		int itemId = ParamUtil.getInt(httpRequest, "item_id", -1);
		int start = ParamUtil.getInt(httpRequest, "start", 0);
		int length = ParamUtil.getInt(httpRequest, "length", 0);

		final AdminHead head = this.adminHeadProvider.get();
		
		AdminDiscoverProtos.GetItemLearnListRequest request = AdminDiscoverProtos.GetItemLearnListRequest.newBuilder()
				.setItemId(itemId)
				.setStart(start)
				.setLength(length)
				.build();
		AdminDiscoverProtos.GetItemLearnListResponse response = Futures.getUnchecked(this.adminDiscoverService.getItemLearnList(this.adminHeadProvider.get(), request));

		// 获取user信息
		Set<Long> userIdSet = new TreeSet<Long>();
		for (DiscoverV2Protos.ItemLearn itemLearn : response.getItemLearnList()) {
			userIdSet.add(itemLearn.getUserId());
		}
		final Map<Long, UserProtos.User> refUserMap = DiscoverServletUtil.getUserMap(adminUserService, head, userIdSet);
		
		JsonArray learnArray = new JsonArray();
		for (DiscoverV2Protos.ItemLearn itemLearn : response.getItemLearnList()) {
			JsonObject learnObj = new JsonObject();
			learnObj.addProperty("item_id", itemLearn.getItemId());
			learnObj.addProperty("user_id", itemLearn.getUserId());
			learnObj.addProperty("user_name", DiscoverServletUtil.getUserName(refUserMap, true, itemLearn.getUserId()));
			learnObj.addProperty("learn_time", DiscoverServletUtil.getDateStr(true, itemLearn.getLearnTime()));
			learnObj.addProperty("learn_duration", itemLearn.getLearnDuration());
			learnObj.addProperty("learn_cnt", itemLearn.getLearnCnt());
			
			learnArray.add(learnObj);
		}
		
		JsonObject resultObj = new JsonObject();
		resultObj.add("item_learn", learnArray);
		resultObj.addProperty("total_size", response.getTotalSize());
		resultObj.addProperty("filtered_size", response.getFilteredSize());
		httpResponse.setContentType("application/json;charset=UTF-8");
		JsonUtil.GSON.toJson(resultObj, httpResponse.getWriter());
	}
}
