package com.weizhu.webapp.upload;

import java.io.File;
import java.util.Properties;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.google.inject.servlet.RequestScoped;
import com.google.inject.servlet.ServletModule;
import com.qiniu.storage.UploadManager;
import com.weizhu.common.config.ConfigUtil;
import com.weizhu.proto.AdminProtos.AdminAnonymousHead;
import com.weizhu.proto.AdminProtos.AdminHead;
import com.weizhu.proto.BossProtos.BossAnonymousHead;
import com.weizhu.proto.BossProtos.BossHead;
import com.weizhu.proto.WeizhuProtos.AnonymousHead;
import com.weizhu.proto.WeizhuProtos.RequestHead;
import com.weizhu.web.filter.AdminSessionFilter;
import com.weizhu.web.filter.BossSessionFilter;
import com.weizhu.web.filter.UserSessionFilter;

public class UploadServletModule extends ServletModule {
	
	public UploadServletModule() {
	}
	
	@Override
	protected void configureServlets() {
		
		/* filter */
		
		filter("/admin/*", "/api/admin/*").through(AdminSessionFilter.class);
		filter("/user/*", "/api/user/*", "/avatar", "/im/image", "/community/image").through(UserSessionFilter.class);
		filter("/boss/*", "/api/boss/*").through(BossSessionFilter.class);
		filter("/*").through(WebappUploadFilter.class);
		
		/* upload image */
		
		serve("/api/boss/upload_image.json").with(com.weizhu.webapp.upload.boss.UploadImageServlet.class);
		serve("/api/admin/upload_image.json").with(com.weizhu.webapp.upload.admin.UploadImageServlet.class);
		serve("/api/user/upload_image.json").with(com.weizhu.webapp.upload.user.UploadImageServlet.class);
		serve("/avatar", "/im/image", "/community/image").with(com.weizhu.webapp.upload.user.Old2UploadImageServlet.class); // deprecated, old image upload
		
		/* upload video */
		
		serve("/api/boss/upload_video.json").with(com.weizhu.webapp.upload.boss.UploadVideoServlet.class);
		serve("/api/admin/upload_video.json").with(com.weizhu.webapp.upload.admin.UploadVideoServlet.class);
		serve("/api/user/upload_video.json").with(com.weizhu.webapp.upload.user.UploadVideoServlet.class);
		
		/* upload discover */
		
		serve("/api/admin/upload_discover_document.json").with(com.weizhu.webapp.upload.admin.UploadDiscoverDocumentServlet.class);
		serve("/api/admin/upload_discover_video.json").with(com.weizhu.webapp.upload.admin.UploadDiscoverVideoServlet.class);
		serve("/api/admin/upload_discover_audio.json").with(com.weizhu.webapp.upload.admin.UploadDiscoverAudioServlet.class);
		
		serve("/api/boss/upload_discover_document.json").with(com.weizhu.webapp.upload.boss.UploadDiscoverDocumentServlet.class);
		serve("/api/boss/upload_discover_video.json").with(com.weizhu.webapp.upload.boss.UploadDiscoverVideoServlet.class);
		serve("/api/boss/upload_discover_audio.json").with(com.weizhu.webapp.upload.boss.UploadDiscoverAudioServlet.class);
		
		/* qiniu upload */
		
		bind(UploadManager.class).in(Singleton.class);
	}
	
	@Provides
	@Singleton
	@Named("upload_image_tmp_dir")
	public File provideUploadTmpDir(@Named("server_conf") Properties confProperties) {
		return ConfigUtil.getDirIfNonExistCreate(confProperties, "upload_image_tmp_dir");
	}
	
	@Provides
	@Singleton
	@Named("upload_video_tmp_dir")
	public File provideVideoTmpDir(@Named("server_conf") Properties confProperties) {
		return ConfigUtil.getDirIfNonExistCreate(confProperties, "upload_video_tmp_dir");
	}
	
	@Provides
	@Singleton
	@Named("upload_ffmpeg_search_path")
	public String provideFfmpegSearchPath(@Named("server_conf") Properties confProperties) {
		return ConfigUtil.getNotEmpty(confProperties, "upload_ffmpeg_search_path");
	}
	
	@Provides
	@Singleton
	@Named("upload_discover_tmp_dir")
	public File provideUploadDiscoverTmpDir(@Named("server_conf") Properties confProperties) {
		return ConfigUtil.getDirIfNonExistCreate(confProperties, "upload_discover_tmp_dir");
	}
	
	@Provides
	@Singleton
	@Named("upload_discover_document_local_save_dir")
	public File provideUploadDiscoverDocumentLocalSaveDir(@Named("server_conf") Properties confProperties) {
		return ConfigUtil.getDir(confProperties, "upload_discover_document_local_save_dir");
	}
	
	@Provides
	@Singleton
	@Named("upload_discover_document_qiniu_enable")
	public boolean provideUploadDiscoverDocumentQiniuEnable(@Named("server_conf") Properties confProperties) {
		return Boolean.parseBoolean(confProperties.getProperty("upload_discover_document_qiniu_enable"));
	}
	
	@Provides
	@Singleton
	@Named("upload_discover_document_qiniu_backet_name")
	public String provideUploadDiscoverDocumentQiniuBucketName(
			@Named("server_conf") Properties confProperties,
			@Named("upload_discover_document_qiniu_enable") boolean qiniuEnable
			) {
		return qiniuEnable ? ConfigUtil.getNotEmpty(confProperties, "upload_discover_document_qiniu_backet_name") : null;
	}
	
	@Provides
	@Singleton
	@Named("upload_discover_document_qiniu_key_prefix")
	public String provideUploadDiscoverDocumentQiniuKeyPrefix(
			@Named("server_conf") Properties confProperties,
			@Named("upload_discover_document_qiniu_enable") boolean qiniuEnable
			) {
		return qiniuEnable ? ConfigUtil.getNullToEmpty(confProperties, "upload_discover_document_qiniu_key_prefix") : null;
	}
	
	@Provides
	@Singleton
	@Named("upload_discover_document_qiniu_access_key")
	public String provideUploadDiscoverDocumentQiniuAccessKey(
			@Named("server_conf") Properties confProperties,
			@Named("upload_discover_document_qiniu_enable") boolean qiniuEnable
			) {
		return qiniuEnable ? ConfigUtil.getNotEmpty(confProperties, "upload_discover_document_qiniu_access_key") : null;
	}
	
	@Provides
	@Singleton
	@Named("upload_discover_document_qiniu_secret_key")
	public String provideUploadDiscoverDocumentQiniuSecretKey(
			@Named("server_conf") Properties confProperties,
			@Named("upload_discover_document_qiniu_enable") boolean qiniuEnable
			) {
		return qiniuEnable ? ConfigUtil.getNotEmpty(confProperties, "upload_discover_document_qiniu_secret_key") : null;
	}
	
	@Provides
	@Singleton
	@Named("upload_discover_document_url_prefix")
	public String provideUploadDiscoverDocumentUrlPrefix(@Named("server_conf") Properties confProperties) {
		return ConfigUtil.getNotEmpty(confProperties, "upload_discover_document_url_prefix");
	}
	
	@Provides
	@Singleton
	@Named("upload_discover_document_auth_enable")
	public boolean provideUploadDiscoverDocumentAuthEnable(@Named("server_conf") Properties confProperties) {
		return Boolean.parseBoolean(confProperties.getProperty("upload_discover_document_auth_enable"));
	}
	
	@Provides
	@Singleton
	@Named("upload_discover_video_local_save_dir")
	public File provideUploadDiscoverVideoLocalSaveDir(@Named("server_conf") Properties confProperties) {
		return ConfigUtil.getDir(confProperties, "upload_discover_video_local_save_dir");
	}
	
	@Provides
	@Singleton
	@Named("upload_discover_video_qiniu_enable")
	public boolean provideUploadDiscoverVideoQiniuEnable(@Named("server_conf") Properties confProperties) {
		return Boolean.parseBoolean(confProperties.getProperty("upload_discover_video_qiniu_enable"));
	}
	
	@Provides
	@Singleton
	@Named("upload_discover_video_qiniu_backet_name")
	public String provideUploadDiscoverVideoQiniuBucketName(
			@Named("server_conf") Properties confProperties,
			@Named("upload_discover_video_qiniu_enable") boolean qiniuEnable
			) {
		return qiniuEnable ? ConfigUtil.getNotEmpty(confProperties, "upload_discover_video_qiniu_backet_name") : null;
	}
	
	@Provides
	@Singleton
	@Named("upload_discover_video_qiniu_key_prefix")
	public String provideUploadDiscoverVideoQiniuKeyPrefix(
			@Named("server_conf") Properties confProperties,
			@Named("upload_discover_video_qiniu_enable") boolean qiniuEnable
			) {
		return qiniuEnable ? ConfigUtil.getNullToEmpty(confProperties, "upload_discover_video_qiniu_key_prefix") : null;
	}
	
	@Provides
	@Singleton
	@Named("upload_discover_video_qiniu_access_key")
	public String provideUploadDiscoverVideoQiniuAccessKey(
			@Named("server_conf") Properties confProperties,
			@Named("upload_discover_video_qiniu_enable") boolean qiniuEnable
			) {
		return qiniuEnable ? ConfigUtil.getNotEmpty(confProperties, "upload_discover_video_qiniu_access_key") : null;
	}
	
	@Provides
	@Singleton
	@Named("upload_discover_video_qiniu_secret_key")
	public String provideUploadDiscoverVideoQiniuSecretKey(
			@Named("server_conf") Properties confProperties,
			@Named("upload_discover_video_qiniu_enable") boolean qiniuEnable
			) {
		return qiniuEnable ? ConfigUtil.getNotEmpty(confProperties, "upload_discover_video_qiniu_secret_key") : null;
	}
	
	@Provides
	@Singleton
	@Named("upload_discover_video_url_prefix")
	public String provideUploadDiscoverVideoUrlPrefix(@Named("server_conf") Properties confProperties) {
		return ConfigUtil.getNotEmpty(confProperties, "upload_discover_video_url_prefix");
	}
	
	@Provides
	@Singleton
	@Named("upload_discover_video_auth_enable")
	public boolean provideUploadDiscoverVideoAuthEnable(@Named("server_conf") Properties confProperties) {
		return Boolean.parseBoolean(confProperties.getProperty("upload_discover_video_auth_enable"));
	}
	
	@Provides
	@Singleton
	@Named("upload_discover_audio_local_save_dir")
	public File provideUploadDiscoverAudioLocalSaveDir(@Named("server_conf") Properties confProperties) {
		return ConfigUtil.getDir(confProperties, "upload_discover_audio_local_save_dir");
	}
	
	@Provides
	@Singleton
	@Named("upload_discover_audio_qiniu_enable")
	public boolean provideUploadDiscoverAudioQiniuEnable(@Named("server_conf") Properties confProperties) {
		return Boolean.parseBoolean(confProperties.getProperty("upload_discover_audio_qiniu_enable"));
	}
	
	@Provides
	@Singleton
	@Named("upload_discover_audio_qiniu_backet_name")
	public String provideUploadDiscoverAudioQiniuBucketName(
			@Named("server_conf") Properties confProperties,
			@Named("upload_discover_audio_qiniu_enable") boolean qiniuEnable
			) {
		return qiniuEnable ? ConfigUtil.getNotEmpty(confProperties, "upload_discover_audio_qiniu_backet_name") : null;
	}
	
	@Provides
	@Singleton
	@Named("upload_discover_audio_qiniu_key_prefix")
	public String provideUploadDiscoverAudioQiniuKeyPrefix(
			@Named("server_conf") Properties confProperties,
			@Named("upload_discover_audio_qiniu_enable") boolean qiniuEnable
			) {
		return qiniuEnable ? ConfigUtil.getNullToEmpty(confProperties, "upload_discover_audio_qiniu_key_prefix") : null;
	}
	
	@Provides
	@Singleton
	@Named("upload_discover_audio_qiniu_access_key")
	public String provideUploadDiscoverAudioQiniuAccessKey(
			@Named("server_conf") Properties confProperties,
			@Named("upload_discover_audio_qiniu_enable") boolean qiniuEnable
			) {
		return qiniuEnable ? ConfigUtil.getNotEmpty(confProperties, "upload_discover_audio_qiniu_access_key") : null;
	}
	
	@Provides
	@Singleton
	@Named("upload_discover_audio_qiniu_secret_key")
	public String provideUploadDiscoverAudioQiniuSecretKey(
			@Named("server_conf") Properties confProperties,
			@Named("upload_discover_audio_qiniu_enable") boolean qiniuEnable
			) {
		return qiniuEnable ? ConfigUtil.getNotEmpty(confProperties, "upload_discover_audio_qiniu_secret_key") : null;
	}
	
	@Provides
	@Singleton
	@Named("upload_discover_audio_url_prefix")
	public String provideUploadDiscoverAudioUrlPrefix(@Named("server_conf") Properties confProperties) {
		return ConfigUtil.getNotEmpty(confProperties, "upload_discover_audio_url_prefix");
	}
	
	@Provides
	@Singleton
	@Named("upload_discover_audio_auth_enable")
	public boolean provideUploadDiscoverAudioAuthEnable(@Named("server_conf") Properties confProperties) {
		return Boolean.parseBoolean(confProperties.getProperty("upload_discover_audio_auth_enable"));
	}
	
	
	@Provides
	@RequestScoped
	public AdminHead provideAdminHead() {
		return null;
	}
	
	@Provides
	@RequestScoped
	public AdminAnonymousHead provideAdminAnonymousHead() {
		return null;
	}
	
	@Provides
	@RequestScoped
	public RequestHead provideRequestHead() {
		return null;
	}
	
	@Provides
	@RequestScoped
	public AnonymousHead provideAnonymousHead() {
		return null;
	}
	
	@Provides
	@RequestScoped
	public BossHead provideBossHead() {
		return null;
	}
	
	@Provides
	@RequestScoped
	public BossAnonymousHead provideBossAnonymousHead() {
		return null;
	}
	
}
