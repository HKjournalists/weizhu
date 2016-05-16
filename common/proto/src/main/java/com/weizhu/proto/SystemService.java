package com.weizhu.proto;

import com.google.common.util.concurrent.ListenableFuture;
import com.weizhu.proto.AdminProtos.AdminHead;
import com.weizhu.proto.BossProtos.BossHead;
import com.weizhu.proto.SystemProtos.CheckNewVersionResponse;
import com.weizhu.proto.SystemProtos.GetAdminConfigResponse;
import com.weizhu.proto.SystemProtos.GetAuthUrlRequest;
import com.weizhu.proto.SystemProtos.GetAuthUrlResponse;
import com.weizhu.proto.SystemProtos.GetBossConfigResponse;
import com.weizhu.proto.SystemProtos.GetConfigResponse;
import com.weizhu.proto.SystemProtos.GetConfigV2Response;
import com.weizhu.proto.SystemProtos.GetUserConfigResponse;
import com.weizhu.proto.SystemProtos.SendFeedbackRequest;
import com.weizhu.proto.SystemProtos.UpdateBadgeNumberRequest;
import com.weizhu.proto.WeizhuProtos.AnonymousHead;
import com.weizhu.proto.WeizhuProtos.EmptyRequest;
import com.weizhu.proto.WeizhuProtos.EmptyResponse;
import com.weizhu.proto.WeizhuProtos.RequestHead;

public interface SystemService {

	/* 匿名访问接口 */
	
	@ResponseType(GetUserConfigResponse.class)
	ListenableFuture<GetUserConfigResponse> getUserConfig(AnonymousHead head, EmptyRequest request);
	
	@ResponseType(CheckNewVersionResponse.class)
	ListenableFuture<CheckNewVersionResponse> checkNewVersion(AnonymousHead head, EmptyRequest request);
	
	@WriteMethod
	@ResponseType(EmptyResponse.class)
	ListenableFuture<EmptyResponse> sendFeedback(AnonymousHead head, SendFeedbackRequest request);
	
	/* 带登陆身份接口 */
	
	@ResponseType(GetUserConfigResponse.class)
	ListenableFuture<GetUserConfigResponse> getUserConfig(RequestHead head, EmptyRequest request);
	
	@ResponseType(CheckNewVersionResponse.class)
	ListenableFuture<CheckNewVersionResponse> checkNewVersion(RequestHead head, EmptyRequest request);
	
	@WriteMethod
	@ResponseType(EmptyResponse.class)
	ListenableFuture<EmptyResponse> sendFeedback(RequestHead head, SendFeedbackRequest request);
	
	@WriteMethod
	@ResponseType(EmptyResponse.class)
	ListenableFuture<EmptyResponse> updateBadgeNumber(RequestHead head, UpdateBadgeNumberRequest request);
	
	@ResponseType(GetAuthUrlResponse.class)
	ListenableFuture<GetAuthUrlResponse> getAuthUrl(RequestHead head, GetAuthUrlRequest request);
	
	/* admin 带登陆身份接口 */
	
	@ResponseType(GetAdminConfigResponse.class)
	ListenableFuture<GetAdminConfigResponse> getAdminConfig(AdminHead head, EmptyRequest request);
	
	@ResponseType(GetAuthUrlResponse.class)
	ListenableFuture<GetAuthUrlResponse> getAuthUrl(AdminHead head, GetAuthUrlRequest request);
	
	/* boss 带登陆身份接口 */
	
	@ResponseType(GetBossConfigResponse.class)
	ListenableFuture<GetBossConfigResponse> getBossConfig(BossHead head, EmptyRequest request);
	
	@ResponseType(GetAuthUrlResponse.class)
	ListenableFuture<GetAuthUrlResponse> getAuthUrl(BossHead head, GetAuthUrlRequest request);
	
	/* 以下接口废弃 */
	
	@Deprecated
	@ResponseType(GetConfigResponse.class)
	ListenableFuture<GetConfigResponse> getConfig(AnonymousHead head, EmptyRequest request);
	
	@Deprecated
	@ResponseType(GetConfigV2Response.class)
	ListenableFuture<GetConfigV2Response> getConfigV2(AnonymousHead head, EmptyRequest request);
	
	@Deprecated
	@ResponseType(GetConfigResponse.class)
	ListenableFuture<GetConfigResponse> getConfig(RequestHead head, EmptyRequest request);
	
	@Deprecated
	@ResponseType(GetConfigV2Response.class)
	ListenableFuture<GetConfigV2Response> getConfigV2(RequestHead head, EmptyRequest request);
}
