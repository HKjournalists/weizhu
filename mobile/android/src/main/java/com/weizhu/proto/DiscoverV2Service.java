package com.weizhu.proto;

import com.weizhu.network.Future;
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

public interface DiscoverV2Service {

	@ResponseType(GetDiscoverHomeResponse.class)
	Future<GetDiscoverHomeResponse> getDiscoverHome(GetDiscoverHomeRequest request, int priorityNum);
	
	@ResponseType(GetModuleCategoryItemListResponse.class)
	Future<GetModuleCategoryItemListResponse> getModuleCategoryItemList(GetModuleCategoryItemListRequest request, int priorityNum);
	
	@ResponseType(GetModulePromptIndexResponse.class)
	Future<GetModulePromptIndexResponse> getModulePromptIndex(GetModulePromptIndexRequest request, int priorityNum);
	
	@ResponseType(GetItemByIdResponse.class)
	Future<GetItemByIdResponse> getItemById(GetItemByIdRequest request, int priorityNum);
	
	@ResponseType(GetItemLearnListResponse.class)
	Future<GetItemLearnListResponse> getItemLearnList(GetItemLearnListRequest request, int priorityNum);
	
	@ResponseType(GetUserLearnListResponse.class)
	Future<GetUserLearnListResponse> getUserLearnList(GetUserLearnListRequest request, int priorityNum);
	
	@ResponseType(GetItemCommentListResponse.class)
	Future<GetItemCommentListResponse> getItemCommentList(GetItemCommentListRequest request, int priorityNum);
	
	@ResponseType(GetUserCommentListResponse.class)
	Future<GetUserCommentListResponse> getUserCommentList(GetUserCommentListRequest request, int priorityNum);
	
	@ResponseType(GetItemScoreListResponse.class)
	Future<GetItemScoreListResponse> getItemScoreList(GetItemScoreListRequest request, int priorityNum);
	
	@ResponseType(GetUserScoreListResponse.class)
	Future<GetUserScoreListResponse> getUserScoreList(GetUserScoreListRequest request, int priorityNum);

	@ResponseType(GetItemLikeListResponse.class)
	Future<GetItemLikeListResponse> getItemLikeList(GetItemLikeListRequest request, int priorityNum);
	
	@ResponseType(GetUserLikeListResponse.class)
	Future<GetUserLikeListResponse> getUserLikeList(GetUserLikeListRequest request, int priorityNum);
	
	@ResponseType(GetItemShareListResponse.class)
	Future<GetItemShareListResponse> getItemShareList(GetItemShareListRequest request, int priorityNum);
	
	@ResponseType(GetUserShareListResponse.class)
	Future<GetUserShareListResponse> getUserShareList(GetUserShareListRequest request, int priorityNum);
	
	@ResponseType(GetUserDiscoverResponse.class)
	Future<GetUserDiscoverResponse> getUserDiscover(GetUserDiscoverRequest request, int priorityNum);
	
	@ResponseType(SearchItemResponse.class)
	Future<SearchItemResponse> searchItem(SearchItemRequest request, int priorityNum);
	
	/* 以下为写操作 */
	
	@ResponseType(LearnItemResponse.class)
	Future<LearnItemResponse> learnItem(LearnItemRequest request, int priorityNum);
	
	@ResponseType(EmptyResponse.class)
	Future<EmptyResponse> reportLearnItem(ReportLearnItemRequest request, int priorityNum);
	
	@ResponseType(CommentItemResponse.class)
	Future<CommentItemResponse> commentItem(CommentItemRequest request, int priorityNum);
	
	@ResponseType(DeleteCommentResponse.class)
	Future<DeleteCommentResponse> deleteComment(DeleteCommentRequest request, int priorityNum);
	
	@ResponseType(ScoreItemResponse.class)
	Future<ScoreItemResponse> scoreItem(ScoreItemRequest request, int priorityNum);
	
	@ResponseType(LikeItemResponse.class)
	Future<LikeItemResponse> likeItem(LikeItemRequest request, int priorityNum);
	
	@ResponseType(ShareItemResponse.class)
	Future<ShareItemResponse> shareItem(ShareItemRequest request, int priorityNum);
	
}
