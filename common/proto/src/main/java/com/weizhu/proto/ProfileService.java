package com.weizhu.proto;

import com.google.common.util.concurrent.ListenableFuture;
import com.weizhu.proto.AdminProtos.AdminHead;
import com.weizhu.proto.BossProtos.BossHead;
import com.weizhu.proto.ProfileProtos.GetProfileRequest;
import com.weizhu.proto.ProfileProtos.GetProfileResponse;
import com.weizhu.proto.ProfileProtos.SetProfileRequest;
import com.weizhu.proto.ProfileProtos.SetProfileResponse;
import com.weizhu.proto.WeizhuProtos.AnonymousHead;
import com.weizhu.proto.WeizhuProtos.RequestHead;
import com.weizhu.proto.WeizhuProtos.SystemHead;

public interface ProfileService {
	
	@ResponseType(GetProfileResponse.class)
	ListenableFuture<GetProfileResponse> getProfile(AnonymousHead head, GetProfileRequest request);
	
	@ResponseType(GetProfileResponse.class)
	ListenableFuture<GetProfileResponse> getProfile(RequestHead head, GetProfileRequest request);
	
	@ResponseType(GetProfileResponse.class)
	ListenableFuture<GetProfileResponse> getProfile(AdminHead head, GetProfileRequest request);
	
	@ResponseType(GetProfileResponse.class)
	ListenableFuture<GetProfileResponse> getProfile(BossHead head, GetProfileRequest request);
	
	@ResponseType(GetProfileResponse.class)
	ListenableFuture<GetProfileResponse> getProfile(SystemHead head, GetProfileRequest request);
	
	@WriteMethod
	@ResponseType(SetProfileResponse.class)
	ListenableFuture<SetProfileResponse> setProfile(AdminHead head, SetProfileRequest request);
	
	@WriteMethod
	@ResponseType(SetProfileResponse.class)
	ListenableFuture<SetProfileResponse> setProfile(BossHead head, SetProfileRequest request);
}
