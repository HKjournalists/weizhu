package com.weizhu.proto;

import com.google.common.util.concurrent.ListenableFuture;
import com.weizhu.proto.AdminOfflineTrainingProtos.CreateTrainRequest;
import com.weizhu.proto.AdminOfflineTrainingProtos.CreateTrainResponse;
import com.weizhu.proto.AdminOfflineTrainingProtos.GetTrainByIdRequest;
import com.weizhu.proto.AdminOfflineTrainingProtos.GetTrainByIdResponse;
import com.weizhu.proto.AdminOfflineTrainingProtos.GetTrainListRequest;
import com.weizhu.proto.AdminOfflineTrainingProtos.GetTrainListResponse;
import com.weizhu.proto.AdminOfflineTrainingProtos.GetTrainUserListRequest;
import com.weizhu.proto.AdminOfflineTrainingProtos.GetTrainUserListResponse;
import com.weizhu.proto.AdminOfflineTrainingProtos.UpdateTrainDiscoverItemRequest;
import com.weizhu.proto.AdminOfflineTrainingProtos.UpdateTrainDiscoverItemResponse;
import com.weizhu.proto.AdminOfflineTrainingProtos.UpdateTrainRequest;
import com.weizhu.proto.AdminOfflineTrainingProtos.UpdateTrainResponse;
import com.weizhu.proto.AdminOfflineTrainingProtos.UpdateTrainStateRequest;
import com.weizhu.proto.AdminOfflineTrainingProtos.UpdateTrainStateResponse;
import com.weizhu.proto.AdminProtos.AdminHead;

public interface AdminOfflineTrainingService {

	@ResponseType(GetTrainByIdResponse.class)
	ListenableFuture<GetTrainByIdResponse> getTrainById(AdminHead head, GetTrainByIdRequest request);
	
	@ResponseType(GetTrainListResponse.class)
	ListenableFuture<GetTrainListResponse> getTrainList(AdminHead head, GetTrainListRequest request);
	
	@WriteMethod
	@ResponseType(CreateTrainResponse.class)
	ListenableFuture<CreateTrainResponse> createTrain(AdminHead head, CreateTrainRequest request);
	
	@WriteMethod
	@ResponseType(UpdateTrainResponse.class)
	ListenableFuture<UpdateTrainResponse> updateTrain(AdminHead head, UpdateTrainRequest request);
	
	@WriteMethod
	@ResponseType(UpdateTrainStateResponse.class)
	ListenableFuture<UpdateTrainStateResponse> updateTrainState(AdminHead head, UpdateTrainStateRequest request);
	
	@WriteMethod
	@ResponseType(UpdateTrainDiscoverItemResponse.class)
	ListenableFuture<UpdateTrainDiscoverItemResponse> updateTrainDiscoverItem(AdminHead head, UpdateTrainDiscoverItemRequest request);
	
	@ResponseType(GetTrainUserListResponse.class)
	ListenableFuture<GetTrainUserListResponse> getTrainUserList(AdminHead head, GetTrainUserListRequest request);
	
}
