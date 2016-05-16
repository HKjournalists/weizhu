package com.weizhu.proto;

import com.weizhu.network.Future;
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

public interface TagService {

	@ResponseType(GetRecommendTagResponse.class)
	Future<GetRecommendTagResponse> getRecommendTag(EmptyRequest request, int priorityNum);
	
	@ResponseType(GetCategoryResponse.class)
	Future<GetCategoryResponse> getCategory(EmptyRequest request, int priorityNum);
	
	@ResponseType(GetCategoryTagListResponse.class)
	Future<GetCategoryTagListResponse> getCategoryTagList(GetCategoryTagListRequest request, int priorityNum);
	
	@ResponseType(GetUserTagListResponse.class)
	Future<GetUserTagListResponse> getUserTagList(GetUserTagListRequest request, int priorityNum);
	
	@ResponseType(GetResourceTagListResponse.class)
	Future<GetResourceTagListResponse> getResourceTagList(GetResourceTagListRequest request, int priorityNum);
	
	@ResponseType(CreateSubscribeTagResponse.class)
	Future<CreateSubscribeTagResponse> createSubscribeTag(CreateSubscribeTagRequest request, int priorityNum);
	
	@ResponseType(DeleteSubscribeTagResponse.class)
	Future<DeleteSubscribeTagResponse> deleteSubscribeTag(DeleteSubscribeTagRequest request, int priorityNum);
}
