package com.weizhu.proto;

import com.google.common.util.concurrent.ListenableFuture;
import com.weizhu.proto.AdminCommunityProtos.DeleteBoardRequest;
import com.weizhu.proto.AdminCommunityProtos.DeleteBoardResponse;
import com.weizhu.proto.AdminCommunityProtos.DeleteBoardTagRequest;
import com.weizhu.proto.AdminCommunityProtos.DeleteBoardTagResponse;
import com.weizhu.proto.AdminCommunityProtos.DeleteCommentRequest;
import com.weizhu.proto.AdminCommunityProtos.DeleteCommentResponse;
import com.weizhu.proto.AdminCommunityProtos.DeletePostRequest;
import com.weizhu.proto.AdminCommunityProtos.DeletePostResponse;
import com.weizhu.proto.AdminCommunityProtos.ExportCommentListRequest;
import com.weizhu.proto.AdminCommunityProtos.ExportCommentListResponse;
import com.weizhu.proto.AdminCommunityProtos.ExportPostLikeListRequest;
import com.weizhu.proto.AdminCommunityProtos.ExportPostLikeListResponse;
import com.weizhu.proto.AdminCommunityProtos.ExportPostListRequest;
import com.weizhu.proto.AdminCommunityProtos.ExportPostListResponse;
import com.weizhu.proto.AdminCommunityProtos.GetBoardListRequest;
import com.weizhu.proto.AdminCommunityProtos.GetBoardListResponse;
import com.weizhu.proto.AdminCommunityProtos.GetBoardTagRequest;
import com.weizhu.proto.AdminCommunityProtos.GetBoardTagResponse;
import com.weizhu.proto.AdminCommunityProtos.GetCommentListRequest;
import com.weizhu.proto.AdminCommunityProtos.GetCommentListResponse;
import com.weizhu.proto.AdminCommunityProtos.GetCommunityResponse;
import com.weizhu.proto.AdminCommunityProtos.GetPostListRequest;
import com.weizhu.proto.AdminCommunityProtos.GetPostListResponse;
import com.weizhu.proto.AdminCommunityProtos.GetRecommendPostResponse;
import com.weizhu.proto.AdminCommunityProtos.MigratePostRequest;
import com.weizhu.proto.AdminCommunityProtos.MigratePostResponse;
import com.weizhu.proto.AdminCommunityProtos.CreateBoardRequest;
import com.weizhu.proto.AdminCommunityProtos.CreateBoardResponse;
import com.weizhu.proto.AdminCommunityProtos.CreateBoardTagRequest;
import com.weizhu.proto.AdminCommunityProtos.CreateBoardTagResponse;
import com.weizhu.proto.AdminCommunityProtos.CreateCommentRequest;
import com.weizhu.proto.AdminCommunityProtos.CreateCommentResponse;
import com.weizhu.proto.AdminCommunityProtos.CreatePostRequest;
import com.weizhu.proto.AdminCommunityProtos.CreatePostResponse;
import com.weizhu.proto.AdminCommunityProtos.RecommendPostRequest;
import com.weizhu.proto.AdminCommunityProtos.RecommendPostResponse;
import com.weizhu.proto.AdminCommunityProtos.SetStickyPostRequest;
import com.weizhu.proto.AdminCommunityProtos.SetStickyPostResponse;
import com.weizhu.proto.AdminCommunityProtos.UpdateBoardOrderRequest;
import com.weizhu.proto.AdminCommunityProtos.UpdateBoardOrderResponse;
import com.weizhu.proto.AdminCommunityProtos.UpdateBoardRequest;
import com.weizhu.proto.AdminCommunityProtos.UpdateBoardResponse;
import com.weizhu.proto.AdminCommunityProtos.SetCommunityRequest;
import com.weizhu.proto.AdminCommunityProtos.SetCommunityResponse;
import com.weizhu.proto.AdminProtos.AdminHead;
import com.weizhu.proto.WeizhuProtos.EmptyRequest;

public interface AdminCommunityService {
	
	@ResponseType(GetCommunityResponse.class)
	ListenableFuture<GetCommunityResponse> getCommunity(AdminHead head, EmptyRequest request);

	@WriteMethod
	@ResponseType(SetCommunityResponse.class)
	ListenableFuture<SetCommunityResponse> setCommunity(AdminHead head, SetCommunityRequest request);

	@WriteMethod
	@ResponseType(UpdateBoardOrderResponse.class)
	ListenableFuture<UpdateBoardOrderResponse> updateBoardOrder(AdminHead head, UpdateBoardOrderRequest request);

	@ResponseType(GetBoardListResponse.class)
	ListenableFuture<GetBoardListResponse> getBoardList(AdminHead head, GetBoardListRequest request);
	
	@WriteMethod
	@ResponseType(CreateBoardResponse.class)
	ListenableFuture<CreateBoardResponse> createBoard(AdminHead head, CreateBoardRequest request);
	
	@WriteMethod
	@ResponseType(UpdateBoardResponse.class)
	ListenableFuture<UpdateBoardResponse> updateBoard(AdminHead head, UpdateBoardRequest request);
	
	@WriteMethod
	@ResponseType(DeleteBoardResponse.class)
	ListenableFuture<DeleteBoardResponse> deleteBoard(AdminHead head, DeleteBoardRequest request);
		
	@ResponseType(GetPostListResponse.class)
	ListenableFuture<GetPostListResponse> getPostList(AdminHead head, GetPostListRequest request);
	
	@ResponseType(ExportPostListResponse.class)
	ListenableFuture<ExportPostListResponse> exportPostList(AdminHead head, ExportPostListRequest request);
	
	@WriteMethod
	@ResponseType(DeletePostResponse.class)
	ListenableFuture<DeletePostResponse> deletePost(AdminHead head, DeletePostRequest request);

	@WriteMethod
	@ResponseType(MigratePostResponse.class)
	ListenableFuture<MigratePostResponse> migratePost(AdminHead head, MigratePostRequest request);	
	
	@ResponseType(GetCommentListResponse.class)
	ListenableFuture<GetCommentListResponse> getCommentList(AdminHead head, GetCommentListRequest request);

	@WriteMethod
	@ResponseType(DeleteCommentResponse.class)
	ListenableFuture<DeleteCommentResponse> deleteComment(AdminHead head, DeleteCommentRequest request);
	
	@WriteMethod
	@ResponseType(SetStickyPostResponse.class)
	ListenableFuture<SetStickyPostResponse> setStickyPost(AdminHead head, SetStickyPostRequest request);	
	
	@WriteMethod
	@ResponseType(RecommendPostResponse.class)
	ListenableFuture<RecommendPostResponse> recommendPost(AdminHead head, RecommendPostRequest request);

	@ResponseType(GetRecommendPostResponse.class)
	ListenableFuture<GetRecommendPostResponse> getRecommendPost(AdminHead head, EmptyRequest request);
		
	@ResponseType(ExportCommentListResponse.class)
	ListenableFuture<ExportCommentListResponse> exportCommentList(AdminHead head, ExportCommentListRequest request);

	@ResponseType(CreateBoardTagResponse.class)
	ListenableFuture<CreateBoardTagResponse> createBoardTag(AdminHead head, CreateBoardTagRequest request);
	
	@ResponseType(DeleteBoardTagResponse.class)
	ListenableFuture<DeleteBoardTagResponse> deleteBoardTag(AdminHead head, DeleteBoardTagRequest request);
	
	@ResponseType(GetBoardTagResponse.class)
	ListenableFuture<GetBoardTagResponse> getBoardTag(AdminHead head, GetBoardTagRequest request);

	@ResponseType(CreateCommentResponse.class)
	ListenableFuture<CreateCommentResponse> createComment(AdminHead head, CreateCommentRequest request);
	
	@ResponseType(ExportPostLikeListResponse.class)
	ListenableFuture<ExportPostLikeListResponse> exportPostLikeList(AdminHead head, ExportPostLikeListRequest request);
	
	@ResponseType(CreatePostResponse.class)
	ListenableFuture<CreatePostResponse> createPost(AdminHead head, CreatePostRequest request);

}
