package com.weizhu.proto;

import com.weizhu.network.Future;
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

public interface CommunityService {

	@ResponseType(GetCommunityResponse.class)
	Future<GetCommunityResponse> getCommunity(GetCommunityRequest request, int priorityNum);
	
	@ResponseType(GetBoardListResponse.class)
	Future<GetBoardListResponse> getBoardList(GetBoardListRequest request, int priorityNum);
	
	@ResponseType(GetPostListResponse.class)
	Future<GetPostListResponse> getPostList(GetPostListRequest request, int priorityNum);
	
	@ResponseType(GetPostListResponse.class)
	Future<GetPostListResponse> getPostListV2(GetPostListRequest request, int priorityNum);
	
	@ResponseType(GetCommentListResponse.class)
	Future<GetCommentListResponse> getCommentList(GetCommentListRequest request, int priorityNum);
	
	@ResponseType(GetHotCommentListResponse.class)
	Future<GetHotCommentListResponse> getHotCommentList(GetHotCommentListRequest request, int priorityNum);

	@ResponseType(CreatePostResponse.class)
	Future<CreatePostResponse> createPost(CreatePostRequest request, int priorityNum);
	
	@ResponseType(DeletePostResponse.class)
	Future<DeletePostResponse> deletePost(DeletePostRequest request, int priorityNum);

	@ResponseType(LikePostResponse.class)
	Future<LikePostResponse> likePost(LikePostRequest request, int priorityNum);
	
	@ResponseType(CreateCommentResponse.class)
	Future<CreateCommentResponse> createComment(CreateCommentRequest request, int priorityNum);
	
	@ResponseType(DeleteCommentResponse.class)
	Future<DeleteCommentResponse> deleteComment(DeleteCommentRequest request, int priorityNum);
	
	@ResponseType(GetMyPostListResponse.class)
	Future<GetMyPostListResponse> getMyPostList(GetMyPostListRequest request, int priorityNum);
	
	@ResponseType(GetMyCommentListResponse.class)
	Future<GetMyCommentListResponse> getMyCommentList(GetMyCommentListRequest request, int priorityNum);
	
	@ResponseType(GetRecommendPostResponse.class)
	Future<GetRecommendPostResponse> getRecommendPost(EmptyRequest request, int priorityNum);
	
	@ResponseType(LikeCommentResponse.class)
	Future<LikeCommentResponse> likeComment(LikeCommentRequest request, int priorityNum);

	@ResponseType(GetPostCommentByIdResponse.class)
	Future<GetPostCommentByIdResponse> getPostCommentById(GetPostCommentByIdRequest request, int priorityNum);
	
	@ResponseType(GetPostByIdsResponse.class)
	Future<GetPostByIdsResponse> getPostByIds(GetPostByIdsRequest request, int priorityNum);
}
