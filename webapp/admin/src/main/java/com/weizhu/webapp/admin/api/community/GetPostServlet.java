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
import com.weizhu.common.service.ServiceUtil;
import com.weizhu.common.utils.JsonUtil;
import com.weizhu.proto.AdminCommunityProtos;
import com.weizhu.proto.AdminCommunityService;
import com.weizhu.proto.AdminUserProtos;
import com.weizhu.proto.AdminUserService;
import com.weizhu.proto.CommunityProtos;
import com.weizhu.proto.WeizhuProtos.EmptyRequest;
import com.weizhu.proto.UploadService;
import com.weizhu.proto.UserProtos;
import com.weizhu.proto.AdminCommunityProtos.GetBoardListRequest;
import com.weizhu.proto.AdminCommunityProtos.GetBoardListResponse;
import com.weizhu.proto.UserProtos.User;
import com.weizhu.proto.AdminProtos.AdminHead;
import com.weizhu.proto.AdminUserProtos.GetUserByIdRequest;
import com.weizhu.web.ParamUtil;

@Singleton
@SuppressWarnings("serial")
public class GetPostServlet extends HttpServlet {

	private final Provider<AdminHead> adminHeadProvider;
	private final AdminCommunityService adminCommunityService;
	private final AdminUserService adminUserService;
	private final UploadService uploadService;

	@Inject
	public GetPostServlet(Provider<AdminHead> adminHeadProvider, AdminCommunityService adminCommunityService, AdminUserService adminUserService,
			UploadService uploadService) {
		this.adminHeadProvider = adminHeadProvider;
		this.adminCommunityService = adminCommunityService;
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
		Integer board_id = ParamUtil.getInt(httpRequest, "board_id", null);
		String post_title = ParamUtil.getString(httpRequest, "post_title", null);
		Integer start = ParamUtil.getInt(httpRequest, "start", null);
		Integer length = ParamUtil.getInt(httpRequest, "length", null);
		Boolean isRecommend = ParamUtil.getBoolean(httpRequest, "is_recommend", null);
		String createUserName = ParamUtil.getString(httpRequest, "create_user_name", null);

		final AdminHead head = this.adminHeadProvider.get();

		Set<Long> searchUserIds = new TreeSet<Long>();
		if (createUserName != null && !createUserName.isEmpty()) {
			AdminUserProtos.GetUserListResponse userResponse = Futures.getUnchecked(this.adminUserService.getUserList(head,
					AdminUserProtos.GetUserListRequest.newBuilder().setKeyword(createUserName).setStart(0).setLength(100).build()));
			
			// 当返回的user列表为空时说明未查询到用户，则该用户所发的帖子为空，所以直接返回
			if (userResponse.getUserCount() <= 0) {
				JsonObject result = new JsonObject();
				result.add("post", new JsonArray());
				result.addProperty("total_size", 0);
				result.addProperty("filtered_size", 0);
				httpResponse.setContentType("application/json;charset=UTF-8");
				JsonUtil.GSON.toJson(result, httpResponse.getWriter());

				return;
			}
			for (User user : userResponse.getUserList()) {
				searchUserIds.add(user.getBase().getUserId());
			}
		}

		List<CommunityProtos.Post> posts = null;
		int totalSize;
		int filteredSize;
		if (isRecommend != null && isRecommend) {
			AdminCommunityProtos.GetRecommendPostResponse response = Futures
					.getUnchecked(this.adminCommunityService.getRecommendPost(head, EmptyRequest.newBuilder().build()));

			posts = response.getPostList();
			totalSize = 0;
			filteredSize = 0;
		} else {
			//未指定状态
			//String state = ParamUtil.getString(httpRequest, "state", null);

			AdminCommunityProtos.GetPostListRequest.Builder requestBuilder = AdminCommunityProtos.GetPostListRequest.newBuilder();
			if (null != board_id) {
				requestBuilder.setBoardId(board_id);
			}
			if (null != post_title) {
				requestBuilder.setPostTitle(post_title);
			}
			if (null != start) {
				requestBuilder.setStart(start);
			}
			if (null != length) {
				requestBuilder.setLength(length);
			}
			
			requestBuilder.addAllCreateUserId(searchUserIds);
			
			AdminCommunityProtos.GetPostListResponse response = Futures
					.getUnchecked(this.adminCommunityService.getPostList(head, requestBuilder.build()));

			posts = response.getPostList();
			totalSize = response.getTotalSize();
			filteredSize = response.getFilteredSize();
		}

		//获取问题列表对应的用户列表信息
		Set<Long> userIds = new TreeSet<Long>();
		for (CommunityProtos.Post post : posts) {
			userIds.add(post.getCreateUserId());
		}
		AdminUserProtos.GetUserByIdResponse userResponse = Futures
				.getUnchecked(this.adminUserService.getUserById(head, GetUserByIdRequest.newBuilder().addAllUserId(userIds).build()));
		String imageUrlPrefix = Futures.getUnchecked(this.uploadService.getUploadUrlPrefix(head, ServiceUtil.EMPTY_REQUEST)).getImageUrlPrefix();

		Map<Long, UserProtos.User> userMap = CommunityServletUtil.getUserMap(userResponse.getUserList());
		
		GetBoardListResponse getBoardListResponse = Futures.getUnchecked(adminCommunityService.getBoardList(head, 
				GetBoardListRequest.newBuilder().build()));
		Map<Integer, CommunityProtos.Board> boardMap = CommunityServletUtil.getBoardMap(getBoardListResponse.getBoardList());

		//拼接json
		JsonObject result = new JsonObject();
		JsonArray postJsonArray = new JsonArray();

		for (CommunityProtos.Post post : posts) {
			JsonObject u = new JsonObject();
			long userId = post.getCreateUserId();
			u.addProperty("post_id", post.getPostId());
			u.addProperty("post_title", post.getPostTitle());
			u.addProperty("board_id", post.getBoardId());
			u.addProperty("board_name", CommunityServletUtil.getBoardName(boardMap, post.getBoardId()));
			u.addProperty("create_user_id", userId);
			u.addProperty("create_user_name", CommunityServletUtil.getUserName(userMap, userId));
			u.addProperty("create_time", CommunityServletUtil.getDate(post.getCreateTime()));
			u.addProperty("is_hot", post.getIsHot() ? "是" : "否");
			u.addProperty("comment_count", post.getCommentCount());
			u.addProperty("like_count", post.hasLikeCount() ? post.getLikeCount() : 0);
			u.addProperty("is_sticky", post.hasIsSticky() ? post.getIsSticky() : false);
			u.addProperty("sticky_time", post.hasStickyTime() ? CommunityServletUtil.getDate(post.getStickyTime()) : "");
			u.addProperty("is_recommend", post.hasIsRecommend() ? post.getIsRecommend() : false);
			u.addProperty("recommend_time", post.hasRecommendTime() ? CommunityServletUtil.getDate(post.getRecommendTime()) : "");

			u.add("post_part", this.getPostPartJson(post.getPostPartList(), imageUrlPrefix));
			postJsonArray.add(u);
		}
		result.add("post", postJsonArray);
		result.addProperty("total_size", totalSize);
		result.addProperty("filtered_size", filteredSize);

		httpResponse.setContentType("application/json;charset=UTF-8");
		JsonUtil.GSON.toJson(result, httpResponse.getWriter());
	}

	private JsonArray getPostPartJson(List<CommunityProtos.Post.Part> partList, String imageUrlPrefix) {

		JsonArray partJsonArray = new JsonArray();
		for (CommunityProtos.Post.Part part : partList) {
			String imageName = part.getImageName();
			JsonObject u = new JsonObject();
			u.addProperty("part_id", part.getPartId());
			u.addProperty("text", part.getText());
			u.addProperty("image_name", imageName);
			u.addProperty("image_url", imageName.isEmpty() ? "" : imageUrlPrefix + imageName);
			partJsonArray.add(u);
		}
		return partJsonArray;
	}

}
