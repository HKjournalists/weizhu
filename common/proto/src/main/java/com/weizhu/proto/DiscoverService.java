package com.weizhu.proto;

import com.google.common.util.concurrent.ListenableFuture;
import com.weizhu.proto.DiscoverProtos.CommentItemRequest;
import com.weizhu.proto.DiscoverProtos.CommentItemResponse;
import com.weizhu.proto.DiscoverProtos.DeleteCommentRequest;
import com.weizhu.proto.DiscoverProtos.DeleteCommentResponse;
import com.weizhu.proto.DiscoverProtos.GetDiscoverHomeResponse;
import com.weizhu.proto.DiscoverProtos.GetItemByIdRequest;
import com.weizhu.proto.DiscoverProtos.GetItemByIdResponse;
import com.weizhu.proto.DiscoverProtos.GetItemCommentListRequest;
import com.weizhu.proto.DiscoverProtos.GetItemCommentListResponse;
import com.weizhu.proto.DiscoverProtos.GetItemContentRequest;
import com.weizhu.proto.DiscoverProtos.GetItemContentResponse;
import com.weizhu.proto.DiscoverProtos.GetItemListRequest;
import com.weizhu.proto.DiscoverProtos.GetItemListResponse;
import com.weizhu.proto.DiscoverProtos.GetItemPVRequest;
import com.weizhu.proto.DiscoverProtos.GetItemPVResponse;
import com.weizhu.proto.DiscoverProtos.GetItemScoreRequest;
import com.weizhu.proto.DiscoverProtos.GetItemScoreResponse;
import com.weizhu.proto.DiscoverProtos.GetModuleItemListRequest;
import com.weizhu.proto.DiscoverProtos.GetModuleItemListResponse;
import com.weizhu.proto.DiscoverProtos.ScoreItemRequest;
import com.weizhu.proto.DiscoverProtos.ScoreItemResponse;
import com.weizhu.proto.DiscoverProtos.SearchItemRequest;
import com.weizhu.proto.DiscoverProtos.SearchItemResponse;
import com.weizhu.proto.WeizhuProtos.EmptyRequest;
import com.weizhu.proto.WeizhuProtos.EmptyResponse;
import com.weizhu.proto.WeizhuProtos.RequestHead;
import com.weizhu.proto.WeizhuProtos.SystemHead;

public interface DiscoverService {
	
	@Deprecated
	@ResponseType(GetDiscoverHomeResponse.class)
	ListenableFuture<GetDiscoverHomeResponse> getDiscoverHome(RequestHead head, EmptyRequest request);
	
	@Deprecated
	@ResponseType(GetModuleItemListResponse.class)
	ListenableFuture<GetModuleItemListResponse> getModuleItemList(RequestHead head, GetModuleItemListRequest request);
	
	@Deprecated
	@ResponseType(GetItemByIdResponse.class)
	ListenableFuture<GetItemByIdResponse> getItemById(RequestHead head, GetItemByIdRequest request);
	
	@Deprecated
	@ResponseType(GetItemContentResponse.class)
	ListenableFuture<GetItemContentResponse> getItemContent(RequestHead head, GetItemContentRequest request);
	
	@Deprecated
	@ResponseType(SearchItemResponse.class)
	ListenableFuture<SearchItemResponse> searchItem(RequestHead head, SearchItemRequest request);
	
	@Deprecated
	@ResponseType(EmptyResponse.class)
	ListenableFuture<EmptyResponse> clearCache(SystemHead head, EmptyRequest request);
	
	@Deprecated
	@ResponseType(GetItemListResponse.class)
	ListenableFuture<GetItemListResponse> getItemList(SystemHead head, GetItemListRequest request);
	
	/* 浏览量相关 */
	
	@Deprecated
	@ResponseType(GetItemPVResponse.class)
	ListenableFuture<GetItemPVResponse> getItemPV(RequestHead head, GetItemPVRequest request);
	
	/* 打分相关 */
	
	@Deprecated
	@ResponseType(GetItemScoreResponse.class)
	ListenableFuture<GetItemScoreResponse> getItemScore(RequestHead head, GetItemScoreRequest request);
	
	@Deprecated
	@WriteMethod
	@ResponseType(ScoreItemResponse.class)
	ListenableFuture<ScoreItemResponse> scoreItem(RequestHead head, ScoreItemRequest request);
	
	/* 评论相关 */
	
	@Deprecated
	@ResponseType(GetItemCommentListResponse.class)
	ListenableFuture<GetItemCommentListResponse> getItemAllCommentList(RequestHead head, GetItemCommentListRequest request);
	
	@Deprecated
	@ResponseType(GetItemCommentListResponse.class)
	ListenableFuture<GetItemCommentListResponse> getItemMyCommentList(RequestHead head, GetItemCommentListRequest request);
	
	@Deprecated
	@WriteMethod
	@ResponseType(CommentItemResponse.class)
	ListenableFuture<CommentItemResponse> commentItem(RequestHead head, CommentItemRequest request);
	
	@Deprecated
	@WriteMethod
	@ResponseType(DeleteCommentResponse.class)
	ListenableFuture<DeleteCommentResponse> deleteComment(RequestHead head, DeleteCommentRequest request);
	
}
