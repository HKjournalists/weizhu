package com.weizhu.proto;

import com.google.common.util.concurrent.ListenableFuture;
import com.weizhu.proto.DiscoverV2Protos.CommentItemRequest;
import com.weizhu.proto.DiscoverV2Protos.CommentItemResponse;
import com.weizhu.proto.DiscoverV2Protos.DeleteCommentRequest;
import com.weizhu.proto.DiscoverV2Protos.DeleteCommentResponse;
import com.weizhu.proto.DiscoverV2Protos.GetDiscoverHomeRequest;
import com.weizhu.proto.DiscoverV2Protos.GetDiscoverHomeResponse;
import com.weizhu.proto.DiscoverV2Protos.GetItemByIdRequest;
import com.weizhu.proto.DiscoverV2Protos.GetItemByIdResponse;
import com.weizhu.proto.DiscoverV2Protos.GetItemCommentListRequest;
import com.weizhu.proto.DiscoverV2Protos.GetItemCommentListResponse;
import com.weizhu.proto.DiscoverV2Protos.GetItemLearnListRequest;
import com.weizhu.proto.DiscoverV2Protos.GetItemLearnListResponse;
import com.weizhu.proto.DiscoverV2Protos.GetItemLikeListRequest;
import com.weizhu.proto.DiscoverV2Protos.GetItemLikeListResponse;
import com.weizhu.proto.DiscoverV2Protos.GetItemListRequest;
import com.weizhu.proto.DiscoverV2Protos.GetItemListResponse;
import com.weizhu.proto.DiscoverV2Protos.GetItemScoreListRequest;
import com.weizhu.proto.DiscoverV2Protos.GetItemScoreListResponse;
import com.weizhu.proto.DiscoverV2Protos.GetItemShareListRequest;
import com.weizhu.proto.DiscoverV2Protos.GetItemShareListResponse;
import com.weizhu.proto.DiscoverV2Protos.GetModuleCategoryItemListRequest;
import com.weizhu.proto.DiscoverV2Protos.GetModuleCategoryItemListResponse;
import com.weizhu.proto.DiscoverV2Protos.GetModulePromptIndexRequest;
import com.weizhu.proto.DiscoverV2Protos.GetModulePromptIndexResponse;
import com.weizhu.proto.DiscoverV2Protos.GetUserCommentListRequest;
import com.weizhu.proto.DiscoverV2Protos.GetUserCommentListResponse;
import com.weizhu.proto.DiscoverV2Protos.GetUserDiscoverRequest;
import com.weizhu.proto.DiscoverV2Protos.GetUserDiscoverResponse;
import com.weizhu.proto.DiscoverV2Protos.GetUserLearnListRequest;
import com.weizhu.proto.DiscoverV2Protos.GetUserLearnListResponse;
import com.weizhu.proto.DiscoverV2Protos.GetUserLikeListRequest;
import com.weizhu.proto.DiscoverV2Protos.GetUserLikeListResponse;
import com.weizhu.proto.DiscoverV2Protos.GetUserScoreListRequest;
import com.weizhu.proto.DiscoverV2Protos.GetUserScoreListResponse;
import com.weizhu.proto.DiscoverV2Protos.GetUserShareListRequest;
import com.weizhu.proto.DiscoverV2Protos.GetUserShareListResponse;
import com.weizhu.proto.DiscoverV2Protos.LearnItemRequest;
import com.weizhu.proto.DiscoverV2Protos.LearnItemResponse;
import com.weizhu.proto.DiscoverV2Protos.LikeItemRequest;
import com.weizhu.proto.DiscoverV2Protos.LikeItemResponse;
import com.weizhu.proto.DiscoverV2Protos.ReportLearnItemRequest;
import com.weizhu.proto.DiscoverV2Protos.ScoreItemRequest;
import com.weizhu.proto.DiscoverV2Protos.ScoreItemResponse;
import com.weizhu.proto.DiscoverV2Protos.SearchItemRequest;
import com.weizhu.proto.DiscoverV2Protos.SearchItemResponse;
import com.weizhu.proto.DiscoverV2Protos.ShareItemRequest;
import com.weizhu.proto.DiscoverV2Protos.ShareItemResponse;
import com.weizhu.proto.WeizhuProtos.EmptyResponse;
import com.weizhu.proto.WeizhuProtos.RequestHead;
import com.weizhu.proto.WeizhuProtos.SystemHead;

public interface DiscoverV2Service {

	@ResponseType(GetDiscoverHomeResponse.class)
	ListenableFuture<GetDiscoverHomeResponse> getDiscoverHome(RequestHead head, GetDiscoverHomeRequest request);
	
	@ResponseType(GetModuleCategoryItemListResponse.class)
	ListenableFuture<GetModuleCategoryItemListResponse> getModuleCategoryItemList(RequestHead head, GetModuleCategoryItemListRequest request);
	
	@ResponseType(GetModulePromptIndexResponse.class)
	ListenableFuture<GetModulePromptIndexResponse> getModulePromptIndex(RequestHead head, GetModulePromptIndexRequest request);
	
	@ResponseType(GetItemByIdResponse.class)
	ListenableFuture<GetItemByIdResponse> getItemById(RequestHead head, GetItemByIdRequest request);
	
	@ResponseType(GetItemByIdResponse.class)
	ListenableFuture<GetItemByIdResponse> getItemById(SystemHead head, GetItemByIdRequest request);
	
	@ResponseType(GetItemLearnListResponse.class)
	ListenableFuture<GetItemLearnListResponse> getItemLearnList(RequestHead head, GetItemLearnListRequest request);
	
	@ResponseType(GetUserLearnListResponse.class)
	ListenableFuture<GetUserLearnListResponse> getUserLearnList(RequestHead head, GetUserLearnListRequest request);
	
	@ResponseType(GetItemCommentListResponse.class)
	ListenableFuture<GetItemCommentListResponse> getItemCommentList(RequestHead head, GetItemCommentListRequest request);
	
	@ResponseType(GetUserCommentListResponse.class)
	ListenableFuture<GetUserCommentListResponse> getUserCommentList(RequestHead head, GetUserCommentListRequest request);
	
	@ResponseType(GetItemScoreListResponse.class)
	ListenableFuture<GetItemScoreListResponse> getItemScoreList(RequestHead head, GetItemScoreListRequest request);
	
	@ResponseType(GetUserScoreListResponse.class)
	ListenableFuture<GetUserScoreListResponse> getUserScoreList(RequestHead head, GetUserScoreListRequest request);

	@ResponseType(GetItemLikeListResponse.class)
	ListenableFuture<GetItemLikeListResponse> getItemLikeList(RequestHead head, GetItemLikeListRequest request);
	
	@ResponseType(GetUserLikeListResponse.class)
	ListenableFuture<GetUserLikeListResponse> getUserLikeList(RequestHead head, GetUserLikeListRequest request);

	@ResponseType(GetItemShareListResponse.class)
	ListenableFuture<GetItemShareListResponse> getItemShareList(RequestHead head, GetItemShareListRequest request);
	
	@ResponseType(GetUserShareListResponse.class)
	ListenableFuture<GetUserShareListResponse> getUserShareList(RequestHead head, GetUserShareListRequest request);

	@ResponseType(GetUserDiscoverResponse.class)
	ListenableFuture<GetUserDiscoverResponse> getUserDiscover(RequestHead head, GetUserDiscoverRequest request);
	
	@ResponseType(SearchItemResponse.class)
	ListenableFuture<SearchItemResponse> searchItem(RequestHead head, SearchItemRequest request);
	
	
	@ResponseType(GetItemListResponse.class)
	ListenableFuture<GetItemListResponse> getItemList(SystemHead head, GetItemListRequest request);
	
	/* 以下为写操作 */
	
	@WriteMethod
	@ResponseType(LearnItemResponse.class)
	ListenableFuture<LearnItemResponse> learnItem(RequestHead head, LearnItemRequest request);
	
	@WriteMethod
	@ResponseType(EmptyResponse.class)
	ListenableFuture<EmptyResponse> reportLearnItem(RequestHead head, ReportLearnItemRequest request);
	
	@WriteMethod
	@ResponseType(CommentItemResponse.class)
	ListenableFuture<CommentItemResponse> commentItem(RequestHead head, CommentItemRequest request);
	
	@WriteMethod
	@ResponseType(DeleteCommentResponse.class)
	ListenableFuture<DeleteCommentResponse> deleteComment(RequestHead head, DeleteCommentRequest request);
	
	@WriteMethod
	@ResponseType(ScoreItemResponse.class)
	ListenableFuture<ScoreItemResponse> scoreItem(RequestHead head, ScoreItemRequest request);
	
	@WriteMethod
	@ResponseType(LikeItemResponse.class)
	ListenableFuture<LikeItemResponse> likeItem(RequestHead head, LikeItemRequest request);

	@WriteMethod
	@ResponseType(ShareItemResponse.class)
	ListenableFuture<ShareItemResponse> shareItem(RequestHead head, ShareItemRequest request);

}
