package com.weizhu.proto;

import com.weizhu.network.Future;
import com.weizhu.proto.SettingsProtos.SetDoNotDisturbRequest;
import com.weizhu.proto.SettingsProtos.SettingsResponse;
import com.weizhu.proto.WeizhuProtos.EmptyRequest;

public interface SettingsService {

	@ResponseType(SettingsResponse.class)
	Future<SettingsResponse> getSettings(EmptyRequest request, int priorityNum);
	
	@ResponseType(SettingsResponse.class)
	Future<SettingsResponse> setDoNotDisturb(SetDoNotDisturbRequest request, int priorityNum);

}
