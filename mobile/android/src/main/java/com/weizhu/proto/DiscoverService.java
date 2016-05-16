package com.weizhu.proto;

import com.weizhu.network.Future;
import com.weizhu.proto.DiscoverProtos.CommentItemRequest;
import com.weizhu.proto.DiscoverProtos.CommentItemResponse;
import com.weizhu.proto.DiscoverProtos.DeleteCommentRequest;
import com.weizhu.proto.DiscoverProtos.DeleteCommentResponse;
import com.weizhu.proto.DiscoverProtos.GetDiscoverHomeResponse;
import com.weizhu.proto.DiscoverProtos.GetItemByIdRequest;
import com.weizhu.proto.DiscoverProtos.GetItemByIdResponse;
import com.weizhu.proto.DiscoverProtos.GetItemCommentListRequest;
import com.weizhu.proto.DiscoverProtos.GetItemCommentListResponse;
import com.weizhu.proto.DiscoverProtos.GetItemScoreRequest;
import com.weizhu.proto.DiscoverProtos.GetItemScoreResponse;
import com.weizhu.proto.DiscoverProtos.GetModuleItemListRequest;
import com.weizhu.proto.DiscoverProtos.GetModuleItemListResponse;
import com.weizhu.proto.DiscoverProtos.ScoreItemRequest;
import com.weizhu.proto.DiscoverProtos.ScoreItemResponse;
import com.weizhu.proto.DiscoverProtos.SearchItemRequest;
import com.weizhu.proto.DiscoverProtos.SearchItemResponse;
import com.weizhu.proto.WeizhuProtos.EmptyRequest;

@Deprecated
public interface DiscoverService {

	@ResponseType(GetDiscoverHomeResponse.class)
	Future<GetDiscoverHomeResponse> getDiscoverHome(EmptyRequest request, int priorityNum);
	
	@ResponseType(GetModuleItemListResponse.class)
	Future<GetModuleItemListResponse> getModuleItemList(GetModuleItemListRequest request, int priorityNum);
	
	@ResponseType(GetItemByIdResponse.class)
	Future<GetItemByIdResponse> getItemById(GetItemByIdRequest request, int priorityNum);
	
	@ResponseType(SearchItemResponse.class)
	Future<SearchItemResponse> searchItem(SearchItemRequest request, int priorityNum);
	
	
	/* 打分相关 */
	
	@ResponseType(GetItemScoreResponse.class)
	Future<GetItemScoreResponse> getItemScore(GetItemScoreRequest request, int priorityNum);
	
	@ResponseType(ScoreItemResponse.class)
	Future<ScoreItemResponse> scoreItem(ScoreItemRequest request, int priorityNum);
	
	/* 评论相关 */
	
	@ResponseType(GetItemCommentListResponse.class)
	Future<GetItemCommentListResponse> getItemAllCommentList(GetItemCommentListRequest request, int priorityNum);
	
	@ResponseType(GetItemCommentListResponse.class)
	Future<GetItemCommentListResponse> getItemMyCommentList(GetItemCommentListRequest request, int priorityNum);
	
	@ResponseType(CommentItemResponse.class)
	Future<CommentItemResponse> commentItem(CommentItemRequest request, int priorityNum);
	
	@ResponseType(DeleteCommentResponse.class)
	Future<DeleteCommentResponse> deleteComment(DeleteCommentRequest request, int priorityNum);
}
