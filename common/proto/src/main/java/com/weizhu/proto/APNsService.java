package com.weizhu.proto;

import com.google.common.util.concurrent.ListenableFuture;
import com.weizhu.proto.APNsProtos.DeleteDeviceTokenExpireRequest;
import com.weizhu.proto.APNsProtos.DeleteDeviceTokenRequest;
import com.weizhu.proto.APNsProtos.SendNotificationRequest;
import com.weizhu.proto.APNsProtos.UpdateDeviceTokenRequest;
import com.weizhu.proto.AdminProtos.AdminHead;
import com.weizhu.proto.WeizhuProtos.EmptyRequest;
import com.weizhu.proto.WeizhuProtos.EmptyResponse;
import com.weizhu.proto.WeizhuProtos.RequestHead;
import com.weizhu.proto.WeizhuProtos.SystemHead;

public interface APNsService {

	@ResponseType(EmptyResponse.class)
	ListenableFuture<EmptyResponse> updateDeviceToken(RequestHead head, UpdateDeviceTokenRequest request);
	
	@ResponseType(EmptyResponse.class)
	ListenableFuture<EmptyResponse> deleteDeviceToken(AdminHead head, DeleteDeviceTokenRequest request);
	
	@ResponseType(EmptyResponse.class)
	ListenableFuture<EmptyResponse> deleteDeviceTokenExpire(RequestHead head, DeleteDeviceTokenExpireRequest request);
	
	@ResponseType(EmptyResponse.class)
	ListenableFuture<EmptyResponse> deleteDeviceTokenLogout(RequestHead head, EmptyRequest request);
	
	@ResponseType(EmptyResponse.class)
	ListenableFuture<EmptyResponse> sendNotification(RequestHead head, SendNotificationRequest request);
	
	@ResponseType(EmptyResponse.class)
	ListenableFuture<EmptyResponse> sendNotification(AdminHead head, SendNotificationRequest request);
	
	@ResponseType(EmptyResponse.class)
	ListenableFuture<EmptyResponse> sendNotification(SystemHead head, SendNotificationRequest request);
}
