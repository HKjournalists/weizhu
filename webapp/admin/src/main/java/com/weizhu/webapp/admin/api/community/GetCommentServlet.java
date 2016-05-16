package com.weizhu.webapp.admin.api.community;

import java.io.IOException;
import java.util.List;
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
import com.weizhu.proto.AdminCommunityProtos;
import com.weizhu.proto.AdminCommunityService;
import com.weizhu.proto.AdminUserProtos;
import com.weizhu.proto.AdminUserService;
import com.weizhu.proto.CommunityProtos;
import com.weizhu.proto.UserProtos;
import com.weizhu.proto.AdminProtos.AdminHead;
import com.weizhu.proto.AdminUserProtos.GetUserByIdRequest;
import com.weizhu.web.ParamUtil;

@Singleton
@SuppressWarnings("serial")
public class GetCommentServlet extends HttpServlet {
	private final Provider<AdminHead> adminHeadProvider;
	private final AdminCommunityService adminCommunityService;
	private final AdminUserService adminUserService;

	@Inject
	public GetCommentServlet(Provider<AdminHead> adminHeadProvider, AdminCommunityService adminCommunityService, AdminUserService adminUserService) {
		this.adminHeadProvider = adminHeadProvider;
		this.adminCommunityService = adminCommunityService;
		this.adminUserService = adminUserService;
	}

	@Override
	protected void doGet(HttpServletRequest httpRequest, HttpServletResponse httpResponse) throws ServletException, IOException {
		this.doPost(httpRequest, httpResponse);
	}

	@Override
	protected void doPost(HttpServletRequest httpRequest, HttpServletResponse httpResponse) throws ServletException, IOException {

		// 1. 取出参数
		int post_id = ParamUtil.getInt(httpRequest, "post_id", -1);
		Integer start = ParamUtil.getInt(httpRequest, "start", null);
		int length = ParamUtil.getInt(httpRequest, "length", 0);
		//未指定状态
		//String state = ParamUtil.getString(httpRequest, "state", null);

		
		AdminCommunityProtos.GetCommentListRequest.Builder requestBuilder= AdminCommunityProtos.GetCommentListRequest.newBuilder();
		requestBuilder.setPostId(post_id);
		if(null!=start){
			requestBuilder.setStart(start);
		}
		requestBuilder.setLength(length);

		AdminCommunityProtos.GetCommentListResponse response = Futures.getUnchecked(this.adminCommunityService.getCommentList(this.adminHeadProvider.get(),
				requestBuilder.build()));

		//获取问题列表对应的用户列表信息
		List<CommunityProtos.Comment> comments = response.getCommentList();
		Set<Long> userIds = new TreeSet<Long>();
		for (CommunityProtos.Comment comment : comments) {
			userIds.add(comment.getCreateUserId());
		}
		AdminUserProtos.GetUserByIdResponse userResponse = Futures.getUnchecked(this.adminUserService.getUserById(this.adminHeadProvider.get(),
				GetUserByIdRequest.newBuilder().addAllUserId(userIds).build()));
		
		Map<Long, UserProtos.User> userMap = CommunityServletUtil.getUserMap(userResponse.getUserList());
		
		//拼接json
		JsonObject result = new JsonObject();
		JsonArray jsonArray = new JsonArray();

		for (int i = 0; i < response.getCommentCount(); i++) {
			CommunityProtos.Comment comment = response.getComment(i);
			JsonObject u = new JsonObject();
			long userId = comment.getCreateUserId();
			u.addProperty("post_id", comment.getPostId());
			u.addProperty("comment_id", comment.getCommentId());
			u.addProperty("reply_comment_id", comment.getReplyCommentId());
			u.addProperty("content", comment.getContent());
			u.addProperty("create_user_id", userId);
			u.addProperty("create_user_name", userId == 0 ? "官方用户" : CommunityServletUtil.getUserName(userMap, userId));
			u.addProperty("create_time", CommunityServletUtil.getDate(comment.getCreateTime()));
			u.addProperty("state", comment.getState().name());
			u.addProperty("like_count", comment.getLikeCount());
			jsonArray.add(u);
		}
		result.add("comment", jsonArray);
		result.addProperty("total_size", response.getTotalSize());
		result.addProperty("filtered_size", response.getFilteredSize());
		httpResponse.setContentType("application/json;charset=UTF-8");
		JsonUtil.GSON.toJson(result, httpResponse.getWriter());
	}

}
