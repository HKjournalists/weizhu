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
public class GetDiscoverItemLikeServlet extends HttpServlet {
	
	private final Provider<AdminHead> adminHeadProvider;
	private final AdminDiscoverService adminDiscoverService;
	private final AdminUserService adminUserService;

	@Inject
	public GetDiscoverItemLikeServlet(Provider<AdminHead> adminHeadProvider, AdminDiscoverService adminDiscoverService, AdminUserService adminUserService) {
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
		
		AdminDiscoverProtos.GetItemLikeListRequest request = AdminDiscoverProtos.GetItemLikeListRequest.newBuilder()
				.setItemId(itemId)
				.setStart(start)
				.setLength(length)
				.build();
		
		AdminDiscoverProtos.GetItemLikeListResponse response = Futures.getUnchecked(this.adminDiscoverService.getItemLikeList(head, request));

		// 获取user信息
		Set<Long> userIdSet = new TreeSet<Long>();
		for (DiscoverV2Protos.ItemLike itemLearn : response.getItemLikeList()) {
			userIdSet.add(itemLearn.getUserId());
		}
		final Map<Long, UserProtos.User> refUserMap = DiscoverServletUtil.getUserMap(adminUserService, head, userIdSet);
		
		JsonArray likeArray = new JsonArray();
		for (DiscoverV2Protos.ItemLike itemLike : response.getItemLikeList()) {
			JsonObject likeObj = new JsonObject();
			likeObj.addProperty("item_id", itemLike.getItemId());
			likeObj.addProperty("user_id", itemLike.getUserId());
			likeObj.addProperty("user_name", DiscoverServletUtil.getUserName(refUserMap, true, itemLike.getUserId()));
			likeObj.addProperty("like_time", DiscoverServletUtil.getDateStr(true, itemLike.getLikeTime()));
			
			likeArray.add(likeObj);
		}
		
		JsonObject resultObj = new JsonObject();
		resultObj.add("item_like", likeArray);
		resultObj.addProperty("total_size", response.getTotalSize());
		resultObj.addProperty("filtered_size", response.getFilteredSize());
		httpResponse.setContentType("application/json;charset=UTF-8");
		JsonUtil.GSON.toJson(resultObj, httpResponse.getWriter());
	}

}
