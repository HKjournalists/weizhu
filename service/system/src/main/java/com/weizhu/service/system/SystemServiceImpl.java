package com.weizhu.service.system;

import java.util.Map.Entry;

import javax.annotation.Nullable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Strings;
import com.google.common.net.HostAndPort;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.inject.Inject;
import com.google.protobuf.TextFormat;
import com.weizhu.common.service.AsyncImpl;
import com.weizhu.common.service.ServiceUtil;
import com.weizhu.common.utils.ProfileManager;
import com.weizhu.proto.APNsProtos.UpdateDeviceTokenRequest;
import com.weizhu.proto.APNsService;
import com.weizhu.proto.AdminProtos.AdminHead;
import com.weizhu.proto.BossProtos.BossHead;
import com.weizhu.proto.SystemProtos.CheckNewVersionResponse;
import com.weizhu.proto.SystemProtos.GetAdminConfigResponse;
import com.weizhu.proto.SystemProtos.GetAuthUrlRequest;
import com.weizhu.proto.SystemProtos.GetAuthUrlResponse;
import com.weizhu.proto.SystemProtos.GetBossConfigResponse;
import com.weizhu.proto.SystemProtos.GetConfigResponse;
import com.weizhu.proto.SystemProtos;
import com.weizhu.proto.SystemProtos.GetConfigV2Response;
import com.weizhu.proto.SystemProtos.GetUserConfigResponse;
import com.weizhu.proto.SystemProtos.SendFeedbackRequest;
import com.weizhu.proto.SystemProtos.UpdateBadgeNumberRequest;
import com.weizhu.proto.SystemService;
import com.weizhu.proto.UploadProtos.GetUploadUrlPrefixResponse;
import com.weizhu.proto.UploadService;
import com.weizhu.proto.WeizhuProtos.AnonymousHead;
import com.weizhu.proto.WeizhuProtos.EmptyRequest;
import com.weizhu.proto.WeizhuProtos.EmptyResponse;
import com.weizhu.proto.WeizhuProtos.RequestHead;

public class SystemServiceImpl implements SystemService {

	private static final Logger logger = LoggerFactory.getLogger(SystemServiceImpl.class);
	
	private static final ProfileManager.ProfileKey<String> SCENE_HOME_URL = 
			ProfileManager.createKey("system:user:scene_home_url", "");
	private static final ProfileManager.ProfileKey<String> RECOMMEND_IMAGE_URL = 
			ProfileManager.createKey("system:user:recommend_image_url", "");
	private static final ProfileManager.ProfileKey<SystemProtos.NewVersion> ANDROID_NEW_VERSION = 
			ProfileManager.createKey("system:android_new_version", null, SystemProtos.NewVersion.getDefaultInstance());
	private static final ProfileManager.ProfileKey<SystemProtos.NewVersion> IPHONE_NEW_VERSION = 
			ProfileManager.createKey("system:iphone_new_version", null, SystemProtos.NewVersion.getDefaultInstance());
	
	private final SystemConfig systemConfig;
	private final ProfileManager profileManager;
	private final UploadService uploadService;
	private final APNsService apnsService;
	
	@Inject
	public SystemServiceImpl(
			SystemConfig systemConfig,
			ProfileManager profileManager,
			UploadService uploadService,
			APNsService apnsService
			) {
		this.systemConfig = systemConfig;
		this.profileManager = profileManager;
		this.uploadService = uploadService;
		this.apnsService = apnsService;
		
		logger.info("http_api_url:" + systemConfig.httpApiUrlList());
		logger.info("socket_conn_addr:" + systemConfig.socketConnAddrList());
		logger.info("webrtc_ice_server_addr:" + systemConfig.webRTCIceServerAddrList());
		logger.info("webapp_admin_url_prefix:" + systemConfig.webappAdminUrlPrefix());
		logger.info("webapp_mobile_url_prefix:" + systemConfig.webappMobileUrlPrefix());
		logger.info("webapp_web_url_prefix:" + systemConfig.webappWebUrlPrefix());
		logger.info("webapp_upload_url_prefix:" + systemConfig.webappUploadUrlPrefix());
	}
	
	private String doGetHttpApiUrl(@Nullable Long companyId) {
		if (companyId == null) {
			return this.systemConfig.httpApiUrlList().get(0);
		} else {
			int mod = (int) (companyId % systemConfig.httpApiUrlList().size());
			if (mod < 0) {
				mod += systemConfig.httpApiUrlList().size();
			}
			return this.systemConfig.httpApiUrlList().get(mod);
		}
	}
	
	private String doGetSocketConnAddr(@Nullable Long companyId) {
		if (companyId == null) {
			return this.systemConfig.socketConnAddrList().get(0);
		} else {
			int mod = (int) (companyId % systemConfig.socketConnAddrList().size());
			if (mod < 0) {
				mod += systemConfig.socketConnAddrList().size();
			}
			return this.systemConfig.socketConnAddrList().get(mod);
		}
	}
	
	@Override
	public ListenableFuture<GetUserConfigResponse> getUserConfig(AnonymousHead head, EmptyRequest request) {
		final Long companyId;
		final ProfileManager.Profile profile;
		if (head.hasCompanyId()) {
			companyId = head.getCompanyId();
			profile = this.profileManager.getProfile(head, "system:");
		} else {
			companyId = null;
			profile = ProfileManager.Profile.EMPTY;
		}
		
		GetUserConfigResponse.Builder responseBuilder = GetUserConfigResponse.newBuilder()
				.setUser(SystemProtos.UserConfig.newBuilder()
						.setHttpApiUrl(this.doGetHttpApiUrl(companyId))
						.setSocketConnAddr(this.doGetSocketConnAddr(companyId))
						.setImageUploadUrl(this.systemConfig.webappUploadUrlPrefix() + "api/user/upload_image.json")
						.setVideoUploadUrl(this.systemConfig.webappUploadUrlPrefix() + "api/user/upload_video.json")
						.addAllWebrtcIceServerAddr(this.systemConfig.webRTCIceServerAddrList())
						.addWeizhuUrlPrefix(this.systemConfig.webappMobileUrlPrefix())
						.addWeizhuUrlPrefix(this.systemConfig.webappUploadUrlPrefix())
						.build());
		
		GetUploadUrlPrefixResponse uploadUrlPrefixResponse = Futures.getUnchecked(this.uploadService.getUploadUrlPrefix(head, ServiceUtil.EMPTY_REQUEST));
		responseBuilder.setImage(SystemProtos.ImageConfig.newBuilder()
				.setImageUrlPrefix(uploadUrlPrefixResponse.getImageUrlPrefix())
				.setImage60UrlPrefix(uploadUrlPrefixResponse.getImage60UrlPrefix())
				.setImage120UrlPrefix(uploadUrlPrefixResponse.getImage120UrlPrefix())
				.setImage240UrlPrefix(uploadUrlPrefixResponse.getImage240UrlPrefix())
				.setImage480UrlPrefix(uploadUrlPrefixResponse.getImage480UrlPrefix())
				.build());
		responseBuilder.setVideo(SystemProtos.VideoConfig.newBuilder()
				.setVideoUrlPrefix(uploadUrlPrefixResponse.getVideoUrlPrefix())
				.build());
		
		final String profileKeyPrefix = "system:user:";
		SystemProtos.DynamicConfig.Builder tmpBuilder = SystemProtos.DynamicConfig.newBuilder();
		for (Entry<String, String> entry : profile.valueMap().entrySet()) {
			if (entry.getKey().startsWith(profileKeyPrefix)) {
				responseBuilder.addDynamic(tmpBuilder.clear()
						.setName(entry.getKey().substring(profileKeyPrefix.length()))
						.setValue(entry.getValue())
						.build());
			}
		}
		return Futures.immediateFuture(responseBuilder.build());
	}

	@Override
	public ListenableFuture<GetUserConfigResponse> getUserConfig(RequestHead head, EmptyRequest request) {
		final long companyId = head.getSession().getCompanyId();
		final ProfileManager.Profile profile = this.profileManager.getProfile(head, "system:");
		
		GetUserConfigResponse.Builder responseBuilder = GetUserConfigResponse.newBuilder()
				.setUser(SystemProtos.UserConfig.newBuilder()
						.setHttpApiUrl(this.doGetHttpApiUrl(companyId))
						.setSocketConnAddr(this.doGetSocketConnAddr(companyId))
						.setImageUploadUrl(this.systemConfig.webappUploadUrlPrefix() + "api/user/upload_image.json")
						.setVideoUploadUrl(this.systemConfig.webappUploadUrlPrefix() + "api/user/upload_video.json")
						.addAllWebrtcIceServerAddr(this.systemConfig.webRTCIceServerAddrList())
						.addWeizhuUrlPrefix(this.systemConfig.webappMobileUrlPrefix())
						.addWeizhuUrlPrefix(this.systemConfig.webappUploadUrlPrefix())
						.build());
		
		GetUploadUrlPrefixResponse uploadUrlPrefixResponse = Futures.getUnchecked(this.uploadService.getUploadUrlPrefix(head, ServiceUtil.EMPTY_REQUEST));
		responseBuilder.setImage(SystemProtos.ImageConfig.newBuilder()
				.setImageUrlPrefix(uploadUrlPrefixResponse.getImageUrlPrefix())
				.setImage60UrlPrefix(uploadUrlPrefixResponse.getImage60UrlPrefix())
				.setImage120UrlPrefix(uploadUrlPrefixResponse.getImage120UrlPrefix())
				.setImage240UrlPrefix(uploadUrlPrefixResponse.getImage240UrlPrefix())
				.setImage480UrlPrefix(uploadUrlPrefixResponse.getImage480UrlPrefix())
				.build());
		responseBuilder.setVideo(SystemProtos.VideoConfig.newBuilder()
				.setVideoUrlPrefix(uploadUrlPrefixResponse.getVideoUrlPrefix())
				.build());
		
		final String profileKeyPrefix = "system:user:";
		SystemProtos.DynamicConfig.Builder tmpBuilder = SystemProtos.DynamicConfig.newBuilder();
		for (Entry<String, String> entry : profile.valueMap().entrySet()) {
			if (entry.getKey().startsWith(profileKeyPrefix)) {
				responseBuilder.addDynamic(tmpBuilder.clear()
						.setName(entry.getKey().substring(profileKeyPrefix.length()))
						.setValue(entry.getValue())
						.build());
			}
		}
		return Futures.immediateFuture(responseBuilder.build());
	}
	
	@Override
	public ListenableFuture<GetAdminConfigResponse> getAdminConfig(AdminHead head, EmptyRequest request) {
		final ProfileManager.Profile profile = this.profileManager.getProfile(head, "system:");
		
		GetAdminConfigResponse.Builder responseBuilder = GetAdminConfigResponse.newBuilder()
				.setAdmin(SystemProtos.AdminConfig.newBuilder()
						.setWebappMobileUrlPrefix(this.systemConfig.webappMobileUrlPrefix())
						.setWebappWebUrlPrefix(this.systemConfig.webappWebUrlPrefix())
						.setWebappUploadUrlPrefix(this.systemConfig.webappUploadUrlPrefix())
						.build());
		
		GetUploadUrlPrefixResponse uploadUrlPrefixResponse = Futures.getUnchecked(this.uploadService.getUploadUrlPrefix(head, ServiceUtil.EMPTY_REQUEST));
		responseBuilder.setImage(SystemProtos.ImageConfig.newBuilder()
				.setImageUrlPrefix(uploadUrlPrefixResponse.getImageUrlPrefix())
				.setImage60UrlPrefix(uploadUrlPrefixResponse.getImage60UrlPrefix())
				.setImage120UrlPrefix(uploadUrlPrefixResponse.getImage120UrlPrefix())
				.setImage240UrlPrefix(uploadUrlPrefixResponse.getImage240UrlPrefix())
				.setImage480UrlPrefix(uploadUrlPrefixResponse.getImage480UrlPrefix())
				.build());
		responseBuilder.setVideo(SystemProtos.VideoConfig.newBuilder()
				.setVideoUrlPrefix(uploadUrlPrefixResponse.getVideoUrlPrefix())
				.build());
		
		final String profileKeyPrefix = "system:admin:";
		SystemProtos.DynamicConfig.Builder tmpBuilder = SystemProtos.DynamicConfig.newBuilder();
		for (Entry<String, String> entry : profile.valueMap().entrySet()) {
			if (entry.getKey().startsWith(profileKeyPrefix)) {
				responseBuilder.addDynamic(tmpBuilder.clear()
						.setName(entry.getKey().substring(profileKeyPrefix.length()))
						.setValue(entry.getValue())
						.build());
			}
		}
		return Futures.immediateFuture(responseBuilder.build());
	}
	
	@Override
	public ListenableFuture<GetBossConfigResponse> getBossConfig(BossHead head, EmptyRequest request) {
		GetBossConfigResponse.Builder responseBuilder = GetBossConfigResponse.newBuilder()
				.setBoss(SystemProtos.BossConfig.newBuilder()
						.setWebappAdminUrlPrefix(this.systemConfig.webappAdminUrlPrefix())
						.setWebappMobileUrlPrefix(this.systemConfig.webappMobileUrlPrefix())
						.setWebappWebUrlPrefix(this.systemConfig.webappWebUrlPrefix())
						.setWebappUploadUrlPrefix(this.systemConfig.webappUploadUrlPrefix())
						.build());
		
		GetUploadUrlPrefixResponse uploadUrlPrefixResponse = Futures.getUnchecked(this.uploadService.getUploadUrlPrefix(head, ServiceUtil.EMPTY_REQUEST));
		responseBuilder.setImage(SystemProtos.ImageConfig.newBuilder()
				.setImageUrlPrefix(uploadUrlPrefixResponse.getImageUrlPrefix())
				.setImage60UrlPrefix(uploadUrlPrefixResponse.getImage60UrlPrefix())
				.setImage120UrlPrefix(uploadUrlPrefixResponse.getImage120UrlPrefix())
				.setImage240UrlPrefix(uploadUrlPrefixResponse.getImage240UrlPrefix())
				.setImage480UrlPrefix(uploadUrlPrefixResponse.getImage480UrlPrefix())
				.build());
		responseBuilder.setVideo(SystemProtos.VideoConfig.newBuilder()
				.setVideoUrlPrefix(uploadUrlPrefixResponse.getVideoUrlPrefix())
				.build());
		
		return Futures.immediateFuture(responseBuilder.build());
	}
	
	private static final ListenableFuture<CheckNewVersionResponse> CHECK_NEW_VERSION_EMPTY_RESPONSE = 
			Futures.immediateFuture(CheckNewVersionResponse.newBuilder().build());
	
	@Override
	@AsyncImpl
	public ListenableFuture<CheckNewVersionResponse> checkNewVersion(AnonymousHead head, EmptyRequest request) {
		return CHECK_NEW_VERSION_EMPTY_RESPONSE;
	}

	@Override
	public ListenableFuture<CheckNewVersionResponse> checkNewVersion(RequestHead head, EmptyRequest request) {
		if (!head.hasWeizhu()) {
			return CHECK_NEW_VERSION_EMPTY_RESPONSE;
		}
		
		final ProfileManager.Profile profile = this.profileManager.getProfile(head, "system:");
		
		SystemProtos.NewVersion newVersion = null;
		switch (head.getWeizhu().getPlatform()) {
			case ANDROID:
				newVersion = profile.get(ANDROID_NEW_VERSION);
				break;
			case IPHONE:
				newVersion = profile.get(IPHONE_NEW_VERSION);
				break;
			default:
				break;
		}
		
		if (newVersion == null) {
			return CHECK_NEW_VERSION_EMPTY_RESPONSE;
		}
		
		if (head.getWeizhu().getVersionCode() < newVersion.getVersionCode()) {
			return Futures.immediateFuture(
					CheckNewVersionResponse.newBuilder()
						.setNewVersion(newVersion)
						.build()); 
		} else {
			return CHECK_NEW_VERSION_EMPTY_RESPONSE;
		}
	}

	@Override
	public ListenableFuture<EmptyResponse> sendFeedback(AnonymousHead head, SendFeedbackRequest request) {
		Throwable throwable = null;
		StringBuilder sb = new StringBuilder();
		try {
			sb.append("sendFeedback\n");
			TextFormat.printUnicode(head, sb);
			sb.append("\n");
			TextFormat.printUnicode(request, sb);
		} catch (Throwable th) {
			throwable = th;
		} finally {
			if (throwable == null) {
				logger.info(sb.toString());
			} else {
				logger.warn(sb.toString(), throwable);
			}
		}
		return ServiceUtil.EMPTY_RESPONSE_IMMEDIATE_FUTURE;
	}

	@Override
	public ListenableFuture<EmptyResponse> sendFeedback(RequestHead head, SendFeedbackRequest request) {
		Throwable throwable = null;
		StringBuilder sb = new StringBuilder();
		try {
			sb.append("sendFeedback\n");
			TextFormat.printUnicode(head, sb);
			sb.append("\n");
			TextFormat.printUnicode(request, sb);
		} catch (Throwable th) {
			throwable = th;
		} finally {
			if (throwable == null) {
				logger.info(sb.toString());
			} else {
				logger.warn(sb.toString(), throwable);
			}
		}
		return ServiceUtil.EMPTY_RESPONSE_IMMEDIATE_FUTURE;
	}

	@Override
	@AsyncImpl
	public ListenableFuture<EmptyResponse> updateBadgeNumber(RequestHead head, UpdateBadgeNumberRequest request) {
		this.apnsService.updateDeviceToken(head, UpdateDeviceTokenRequest.newBuilder()
				.setBadgeNumber(request.getBadgeNumber())
				.build());
		return ServiceUtil.EMPTY_RESPONSE_IMMEDIATE_FUTURE;
	}
	
	@Override
	public ListenableFuture<GetAuthUrlResponse> getAuthUrl(RequestHead head, GetAuthUrlRequest request) {
		return Futures.immediateFuture(this.doAuthUrl(head.getSession().getCompanyId(), request));
	}

	@Override
	public ListenableFuture<GetAuthUrlResponse> getAuthUrl(AdminHead head, GetAuthUrlRequest request) {
		if (!head.hasCompanyId()) {
			return Futures.immediateFuture(GetAuthUrlResponse.newBuilder()
					.setResult(GetAuthUrlResponse.Result.FAIL_UNKNOWN)
					.setFailText("url授权失败, company_id为空")
					.setAuthUrl(request.getUrl())
					.build());
		}
		return Futures.immediateFuture(this.doAuthUrl(head.getCompanyId(), request));
	}

	@Override
	public ListenableFuture<GetAuthUrlResponse> getAuthUrl(BossHead head, GetAuthUrlRequest request) {
		if (!head.hasCompanyId()) {
			return Futures.immediateFuture(GetAuthUrlResponse.newBuilder()
					.setResult(GetAuthUrlResponse.Result.FAIL_UNKNOWN)
					.setFailText("url授权失败, company_id为空")
					.setAuthUrl(request.getUrl())
					.build());
		}
		return Futures.immediateFuture(this.doAuthUrl(head.getCompanyId(), request));
	}
	
	private GetAuthUrlResponse doAuthUrl(long companyId, GetAuthUrlRequest request) {
		String authUrl = this.systemConfig.authUrl().auth(companyId, request.getUrl());
		if (authUrl == null) {
			return GetAuthUrlResponse.newBuilder()
					.setResult(GetAuthUrlResponse.Result.FAIL_UNKNOWN)
					.setFailText("url授权失败")
					.setAuthUrl(request.getUrl())
					.build();
		} else {
			return GetAuthUrlResponse.newBuilder()
					.setResult(GetAuthUrlResponse.Result.SUCC)
					.setAuthUrl(authUrl)
					.build();
		}
	}
	
	/* Deprecated */
	
	@Override
	public ListenableFuture<GetConfigResponse> getConfig(AnonymousHead head, EmptyRequest request) {
		return Futures.immediateFuture(convert(Futures.getUnchecked(this.getConfigV2(head, request))));
	}
	
	@Override
	public ListenableFuture<GetConfigResponse> getConfig(RequestHead head, EmptyRequest request) {
		return Futures.immediateFuture(convert(Futures.getUnchecked(this.getConfigV2(head, request))));
	}
	
	private GetConfigResponse convert(GetConfigV2Response response) {
		final HostAndPort socketConnAddr = HostAndPort.fromString(response.getConfig().getSocketConnAddr());
		return GetConfigResponse.newBuilder()
				.setConfig(SystemProtos.Config.newBuilder()
						.setHttpApiUrl(response.getConfig().getHttpApiUrl())
						.setSocketConnHost(socketConnAddr.getHostText())
						.setSocketConnPort(socketConnAddr.getPort())
						.setUploadAvatarUrl(response.getConfig().getAvatarUploadUrl())
						.setUploadImImageUrl(response.getConfig().getImImageUploadUrl())
						.setUploadImFileUrl("")
						.setAvatarUrl(response.getConfig().getAvatarUrlPrefix())
						.setImImageUrl(response.getConfig().getImImageUrlPrefix())
						.setImFileUrl("")
						.setDiscoverImageUrl(response.getConfig().getDiscoverImageUrlPrefix())
						.setDiscoverIconUrl(response.getConfig().getDiscoverImageUrlPrefix())
						.setDiscoverItemUrl(response.getConfig().getDiscoverItemUrlPrefix())
						.setUploadCommunityImageUrl(response.getConfig().getCommunityImageUploadUrl())
						.setCommunityImageUrl(response.getConfig().getCommunityImageUrlPrefix())
						.build()).build();
	}
	
	private ListenableFuture<GetConfigV2Response> doGetConfigV2(
			@Nullable Long companyId, 
			GetUploadUrlPrefixResponse getUploadUrlPrefixResponse, 
			ProfileManager.Profile profile
			) {
		SystemProtos.ConfigV2.Builder builder = SystemProtos.ConfigV2.newBuilder();
		builder.setHttpApiUrl(this.doGetHttpApiUrl(companyId));
		builder.setSocketConnAddr(this.doGetSocketConnAddr(companyId));
		builder.setImageUploadUrl(this.systemConfig.webappUploadUrlPrefix() + "api/user/upload_image.json");
		
		builder.setAvatarUrlPrefix(getUploadUrlPrefixResponse.getImageUrlPrefix());
		builder.setAvatarUploadUrl(this.systemConfig.webappUploadUrlPrefix() + "avatar");
		builder.setImImageUrlPrefix(getUploadUrlPrefixResponse.getImageUrlPrefix());
		builder.setImImageUploadUrl(this.systemConfig.webappUploadUrlPrefix() + "im/image");
		builder.setDiscoverImageUrlPrefix(getUploadUrlPrefixResponse.getImageUrlPrefix());
		builder.setDiscoverImageUploadUrl("");
		builder.setDiscoverItemUrlPrefix(this.systemConfig.webappMobileUrlPrefix() + "discover/item_content?item_id=");
		builder.setCommunityImageUrlPrefix(getUploadUrlPrefixResponse.getImageUrlPrefix());
		builder.setCommunityImageUploadUrl(this.systemConfig.webappMobileUrlPrefix() + "community/image");
		builder.setSceneImageUrlPrefix(getUploadUrlPrefixResponse.getImageUrlPrefix());
		
		String sceneHomeUrl = profile.get(SCENE_HOME_URL);
		if (!Strings.isNullOrEmpty(sceneHomeUrl)) {
			builder.setSceneHomeUrl(sceneHomeUrl);
		}
		
		String recommendImageUrl = profile.get(RECOMMEND_IMAGE_URL);
		if (!Strings.isNullOrEmpty(recommendImageUrl)) {
			builder.setRecommendImageUrl(recommendImageUrl);
		}
		
		builder.setImageUrlPrefix(getUploadUrlPrefixResponse.getImageUrlPrefix());
		builder.setImage60UrlPrefix(getUploadUrlPrefixResponse.getImage60UrlPrefix());
		builder.setImage120UrlPrefix(getUploadUrlPrefixResponse.getImage120UrlPrefix());
		builder.setImage240UrlPrefix(getUploadUrlPrefixResponse.getImage240UrlPrefix());
		builder.setImage480UrlPrefix(getUploadUrlPrefixResponse.getImage480UrlPrefix());
		
		return Futures.immediateFuture(GetConfigV2Response.newBuilder()
				.setConfig(builder.build())
				.build());
	}

	@Override
	public ListenableFuture<GetConfigV2Response> getConfigV2(AnonymousHead head, EmptyRequest request) {
		return this.doGetConfigV2(head.hasCompanyId() ? head.getCompanyId() : null, 
				Futures.getUnchecked(this.uploadService.getUploadUrlPrefix(head, ServiceUtil.EMPTY_REQUEST)), 
				ProfileManager.Profile.EMPTY
				);
	}

	@Override
	public ListenableFuture<GetConfigV2Response> getConfigV2(RequestHead head, EmptyRequest request) {
		return this.doGetConfigV2(head.getSession().getCompanyId(), 
				Futures.getUnchecked(this.uploadService.getUploadUrlPrefix(head, ServiceUtil.EMPTY_REQUEST)), 
				this.profileManager.getProfile(head, "system:")
				);
	}

}
