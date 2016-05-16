package com.weizhu.proto;

import com.google.common.util.concurrent.ListenableFuture;
import com.weizhu.proto.AdminProtos.AdminHead;
import com.weizhu.proto.SettingsProtos.GetUserSettingsRequest;
import com.weizhu.proto.SettingsProtos.GetUserSettingsResponse;
import com.weizhu.proto.SettingsProtos.SetDoNotDisturbRequest;
import com.weizhu.proto.SettingsProtos.SettingsResponse;
import com.weizhu.proto.WeizhuProtos.EmptyRequest;
import com.weizhu.proto.WeizhuProtos.RequestHead;
import com.weizhu.proto.WeizhuProtos.SystemHead;

public interface SettingsService {

	@ResponseType(SettingsResponse.class)
	ListenableFuture<SettingsResponse> getSettings(RequestHead head, EmptyRequest request);
	
	@WriteMethod
	@ResponseType(SettingsResponse.class)
	ListenableFuture<SettingsResponse> setDoNotDisturb(RequestHead head, SetDoNotDisturbRequest request);
	
	@ResponseType(GetUserSettingsResponse.class)
	ListenableFuture<GetUserSettingsResponse> getUserSettings(RequestHead head, GetUserSettingsRequest request);
	
	@ResponseType(GetUserSettingsResponse.class)
	ListenableFuture<GetUserSettingsResponse> getUserSettings(AdminHead head, GetUserSettingsRequest request);
	
	@ResponseType(GetUserSettingsResponse.class)
	ListenableFuture<GetUserSettingsResponse> getUserSettings(SystemHead head, GetUserSettingsRequest request);

}
