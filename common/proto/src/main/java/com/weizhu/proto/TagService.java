package com.weizhu.proto;

import com.google.common.util.concurrent.ListenableFuture;
import com.weizhu.proto.TagProtos.CreateSubscribeTagRequest;
import com.weizhu.proto.TagProtos.CreateSubscribeTagResponse;
import com.weizhu.proto.TagProtos.DeleteSubscribeTagRequest;
import com.weizhu.proto.TagProtos.DeleteSubscribeTagResponse;
import com.weizhu.proto.TagProtos.GetCategoryResponse;
import com.weizhu.proto.TagProtos.GetCategoryTagListRequest;
import com.weizhu.proto.TagProtos.GetCategoryTagListResponse;
import com.weizhu.proto.TagProtos.GetRecommendTagResponse;
import com.weizhu.proto.TagProtos.GetResourceTagListRequest;
import com.weizhu.proto.TagProtos.GetResourceTagListResponse;
import com.weizhu.proto.TagProtos.GetUserTagListRequest;
import com.weizhu.proto.TagProtos.GetUserTagListResponse;
import com.weizhu.proto.WeizhuProtos.EmptyRequest;
import com.weizhu.proto.WeizhuProtos.RequestHead;

public interface TagService {

	@ResponseType(GetRecommendTagResponse.class)
	ListenableFuture<GetRecommendTagResponse> getRecommendTag(RequestHead head, EmptyRequest request);
	
	@ResponseType(GetCategoryResponse.class)
	ListenableFuture<GetCategoryResponse> getCategory(RequestHead head, EmptyRequest request);
	
	@ResponseType(GetCategoryTagListResponse.class)
	ListenableFuture<GetCategoryTagListResponse> getCategoryTagList(RequestHead head, GetCategoryTagListRequest request);
	
	@ResponseType(GetUserTagListResponse.class)
	ListenableFuture<GetUserTagListResponse> getUserTagList(RequestHead head, GetUserTagListRequest request);
	
	@ResponseType(GetResourceTagListResponse.class)
	ListenableFuture<GetResourceTagListResponse> getResourceTagList(RequestHead head, GetResourceTagListRequest request);
	
	@ResponseType(CreateSubscribeTagResponse.class)
	ListenableFuture<CreateSubscribeTagResponse> createSubscribeTag(RequestHead head, CreateSubscribeTagRequest request);
	
	@ResponseType(DeleteSubscribeTagResponse.class)
	ListenableFuture<DeleteSubscribeTagResponse> deleteSubscribeTag(RequestHead head, DeleteSubscribeTagRequest request);
}
