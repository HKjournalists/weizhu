package com.weizhu.proto;

import com.google.common.util.concurrent.ListenableFuture;
import com.weizhu.proto.OfflineTrainingProtos.ApplyTrainRequest;
import com.weizhu.proto.OfflineTrainingProtos.ApplyTrainResponse;
import com.weizhu.proto.OfflineTrainingProtos.CheckInTrainRequest;
import com.weizhu.proto.OfflineTrainingProtos.CheckInTrainResponse;
import com.weizhu.proto.OfflineTrainingProtos.GetClosedTrainListRequest;
import com.weizhu.proto.OfflineTrainingProtos.GetClosedTrainListResponse;
import com.weizhu.proto.OfflineTrainingProtos.GetOpenTrainCountResponse;
import com.weizhu.proto.OfflineTrainingProtos.GetOpenTrainListRequest;
import com.weizhu.proto.OfflineTrainingProtos.GetOpenTrainListResponse;
import com.weizhu.proto.OfflineTrainingProtos.GetTrainByIdRequest;
import com.weizhu.proto.OfflineTrainingProtos.GetTrainByIdResponse;
import com.weizhu.proto.OfflineTrainingProtos.LeaveTrainRequest;
import com.weizhu.proto.OfflineTrainingProtos.LeaveTrainResponse;
import com.weizhu.proto.WeizhuProtos.EmptyRequest;
import com.weizhu.proto.WeizhuProtos.RequestHead;

public interface OfflineTrainingService {

	@ResponseType(GetOpenTrainListResponse.class)
	ListenableFuture<GetOpenTrainListResponse> getOpenTrainList(RequestHead head, GetOpenTrainListRequest request);
	
	@ResponseType(GetOpenTrainCountResponse.class)
	ListenableFuture<GetOpenTrainCountResponse> getOpenTrainCount(RequestHead head, EmptyRequest request);
	
	@ResponseType(GetClosedTrainListResponse.class)
	ListenableFuture<GetClosedTrainListResponse> getClosedTrainList(RequestHead head, GetClosedTrainListRequest request);
	
	@ResponseType(GetTrainByIdResponse.class)
	ListenableFuture<GetTrainByIdResponse> getTrainById(RequestHead head, GetTrainByIdRequest request);
	
	@WriteMethod
	@ResponseType(ApplyTrainResponse.class)
	ListenableFuture<ApplyTrainResponse> applyTrain(RequestHead head, ApplyTrainRequest request);
	
	@WriteMethod
	@ResponseType(CheckInTrainResponse.class)
	ListenableFuture<CheckInTrainResponse> checkInTrain(RequestHead head, CheckInTrainRequest request);
	
	@WriteMethod
	@ResponseType(LeaveTrainResponse.class)
	ListenableFuture<LeaveTrainResponse> leaveTrain(RequestHead head, LeaveTrainRequest request);
	
}
