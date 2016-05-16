package com.weizhu.proto;

import com.google.common.util.concurrent.ListenableFuture;
import com.weizhu.proto.CommunityProtos.CreateCommentRequest;
import com.weizhu.proto.CommunityProtos.CreateCommentResponse;
import com.weizhu.proto.CommunityProtos.CreatePostRequest;
import com.weizhu.proto.CommunityProtos.CreatePostResponse;
import com.weizhu.proto.CommunityProtos.DeleteCommentRequest;
import com.weizhu.proto.CommunityProtos.DeleteCommentResponse;
import com.weizhu.proto.CommunityProtos.DeletePostRequest;
import com.weizhu.proto.CommunityProtos.DeletePostResponse;
import com.weizhu.proto.CommunityProtos.GetBoardListRequest;
import com.weizhu.proto.CommunityProtos.GetBoardListResponse;
import com.weizhu.proto.CommunityProtos.GetCommentListRequest;
import com.weizhu.proto.CommunityProtos.GetCommentListResponse;
import com.weizhu.proto.CommunityProtos.GetCommunityRequest;
import com.weizhu.proto.CommunityProtos.GetCommunityResponse;
import com.weizhu.proto.CommunityProtos.GetHotCommentListRequest;
import com.weizhu.proto.CommunityProtos.GetHotCommentListResponse;
import com.weizhu.proto.CommunityProtos.GetMyCommentListRequest;
import com.weizhu.proto.CommunityProtos.GetMyCommentListResponse;
import com.weizhu.proto.CommunityProtos.GetMyPostListRequest;
import com.weizhu.proto.CommunityProtos.GetMyPostListResponse;
import com.weizhu.proto.CommunityProtos.GetPostByIdsRequest;
import com.weizhu.proto.CommunityProtos.GetPostByIdsResponse;
import com.weizhu.proto.CommunityProtos.GetPostCommentByIdRequest;
import com.weizhu.proto.CommunityProtos.GetPostCommentByIdResponse;
import com.weizhu.proto.CommunityProtos.GetPostListRequest;
import com.weizhu.proto.CommunityProtos.GetPostListResponse;
import com.weizhu.proto.CommunityProtos.GetRecommendPostResponse;
import com.weizhu.proto.CommunityProtos.LikeCommentRequest;
import com.weizhu.proto.CommunityProtos.LikeCommentResponse;
import com.weizhu.proto.CommunityProtos.LikePostRequest;
import com.weizhu.proto.CommunityProtos.LikePostResponse;
import com.weizhu.proto.WeizhuProtos.EmptyRequest;
import com.weizhu.proto.WeizhuProtos.RequestHead;

public interface CommunityService {

	@ResponseType(GetCommunityResponse.class)
	ListenableFuture<GetCommunityResponse> getCommunity(RequestHead head, GetCommunityRequest request);
	
	@ResponseType(GetBoardListResponse.class)
	ListenableFuture<GetBoardListResponse> getBoardList(RequestHead head, GetBoardListRequest request);
	
	@ResponseType(GetPostListResponse.class)
	ListenableFuture<GetPostListResponse> getPostList(RequestHead head, GetPostListRequest request);

	@ResponseType(GetPostListResponse.class)
	ListenableFuture<GetPostListResponse> getPostListV2(RequestHead head, GetPostListRequest request);
	
	@ResponseType(GetCommentListResponse.class)
	ListenableFuture<GetCommentListResponse> getCommentList(RequestHead head, GetCommentListRequest request);

	@ResponseType(GetHotCommentListResponse.class)
	ListenableFuture<GetHotCommentListResponse> getHotCommentList(RequestHead head, GetHotCommentListRequest request);

	@WriteMethod
	@ResponseType(CreatePostResponse.class)
	ListenableFuture<CreatePostResponse> createPost(RequestHead head, CreatePostRequest request);
	
	@WriteMethod
	@ResponseType(DeletePostResponse.class)
	ListenableFuture<DeletePostResponse> deletePost(RequestHead head, DeletePostRequest request);
	
	@WriteMethod
	@ResponseType(LikePostResponse.class)
	ListenableFuture<LikePostResponse> likePost(RequestHead head, LikePostRequest request);

	@WriteMethod
	@ResponseType(CreateCommentResponse.class)
	ListenableFuture<CreateCommentResponse> createComment(RequestHead head, CreateCommentRequest request);
	
	@WriteMethod
	@ResponseType(DeleteCommentResponse.class)
	ListenableFuture<DeleteCommentResponse> deleteComment(RequestHead head, DeleteCommentRequest request);
	
	@ResponseType(GetMyPostListResponse.class)
	ListenableFuture<GetMyPostListResponse> getMyPostList(RequestHead head, GetMyPostListRequest request);
	
	@ResponseType(GetMyCommentListResponse.class)
	ListenableFuture<GetMyCommentListResponse> getMyCommentList(RequestHead head, GetMyCommentListRequest request);
	
	@ResponseType(GetRecommendPostResponse.class)
	ListenableFuture<GetRecommendPostResponse> getRecommendPost(RequestHead head, EmptyRequest request);
	
	@WriteMethod
	@ResponseType(LikeCommentResponse.class)
	ListenableFuture<LikeCommentResponse> likeComment(RequestHead head, LikeCommentRequest request);

	@ResponseType(GetPostCommentByIdResponse.class)
	ListenableFuture<GetPostCommentByIdResponse> getPostCommentById(RequestHead head, GetPostCommentByIdRequest request);

	@ResponseType(GetPostByIdsResponse.class)
	ListenableFuture<GetPostByIdsResponse> getPostByIds(RequestHead head, GetPostByIdsRequest request);
}
