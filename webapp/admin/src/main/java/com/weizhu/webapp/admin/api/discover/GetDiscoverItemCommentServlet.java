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
import com.weizhu.proto.DiscoverV2Protos.ItemComment;
import com.weizhu.proto.UserProtos;
import com.weizhu.web.ParamUtil;

@Singleton
@SuppressWarnings("serial")
public class GetDiscoverItemCommentServlet extends HttpServlet {
	
	private final Provider<AdminHead> adminHeadProvider;
	private final AdminDiscoverService adminDiscoverService;
	private final AdminUserService adminUserService;

	@Inject
	public GetDiscoverItemCommentServlet(Provider<AdminHead> adminHeadProvider, AdminDiscoverService adminDiscoverService, AdminUserService adminUserService) {
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
		
		AdminDiscoverProtos.GetItemCommentListRequest request = AdminDiscoverProtos.GetItemCommentListRequest.newBuilder()
				.setItemId(itemId)
				.setStart(start)
				.setLength(length)
				.build();

		AdminDiscoverProtos.GetItemCommentListResponse response = Futures.getUnchecked(this.adminDiscoverService.getItemCommentList(head, request));

		// 获取user信息
		Set<Long> userIdSet = new TreeSet<Long>();
		for (DiscoverV2Protos.ItemComment itemComment : response.getItemCommentList()) {
			userIdSet.add(itemComment.getUserId());
		}
		
		final Map<Long, UserProtos.User> refUserMap = DiscoverServletUtil.getUserMap(adminUserService, head, userIdSet);

		JsonArray commentArray = new JsonArray();
		for (ItemComment itemComment : response.getItemCommentList()) {
			JsonObject commentObj = new JsonObject();
			commentObj.addProperty("comment_id", itemComment.getCommentId());
			commentObj.addProperty("item_id", itemComment.getItemId());
			commentObj.addProperty("user_id", itemComment.getUserId());
			commentObj.addProperty("user_name", DiscoverServletUtil.getUserName(refUserMap, true, itemComment.getUserId()));
			commentObj.addProperty("comment_time", DiscoverServletUtil.getDateStr(true, itemComment.getCommentTime()));
			commentObj.addProperty("comment_text", itemComment.getCommentText());
			commentObj.addProperty("is_delete", itemComment.getIsDelete());
			
			commentArray.add(commentObj);
		}
		
		JsonObject resultObj = new JsonObject();
		resultObj.add("item_comment", commentArray);
		resultObj.addProperty("total_size", response.getTotalSize());
		resultObj.addProperty("filtered_size", response.getFilteredSize());
		httpResponse.setContentType("application/json;charset=UTF-8");
		JsonUtil.GSON.toJson(resultObj, httpResponse.getWriter());
	}
}
