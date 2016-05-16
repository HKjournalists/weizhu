package com.weizhu.proto;

import com.google.common.util.concurrent.ListenableFuture;
import com.weizhu.proto.ComponentProtos.GetScoreByIdRequest;
import com.weizhu.proto.ComponentProtos.GetScoreByIdResponse;
import com.weizhu.proto.ComponentProtos.GetScoreUserListRequest;
import com.weizhu.proto.ComponentProtos.GetScoreUserListResponse;
import com.weizhu.proto.ComponentProtos.GetUserScoreListRequest;
import com.weizhu.proto.ComponentProtos.GetUserScoreListResponse;
import com.weizhu.proto.ComponentProtos.ScoreRequest;
import com.weizhu.proto.ComponentProtos.ScoreResponse;
import com.weizhu.proto.WeizhuProtos.RequestHead;

public interface ComponentService {

	@ResponseType(GetScoreByIdResponse.class)
	ListenableFuture<GetScoreByIdResponse> getScoreById(RequestHead head,GetScoreByIdRequest request);
	
	@ResponseType(GetScoreUserListResponse.class)
	ListenableFuture<GetScoreUserListResponse> getScoreUserList(RequestHead head,GetScoreUserListRequest request);
	
	@ResponseType(GetUserScoreListResponse.class)
	ListenableFuture<GetUserScoreListResponse> getUserScoreList(RequestHead head,GetUserScoreListRequest request);
	
	@WriteMethod
	@ResponseType(ScoreResponse.class)
	ListenableFuture<ScoreResponse> score(RequestHead head,ScoreRequest request);
}
