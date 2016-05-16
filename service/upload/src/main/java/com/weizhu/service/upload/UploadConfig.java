package com.weizhu.service.upload;

import java.io.File;
import java.util.Properties;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.weizhu.common.config.ConfigUtil;

@Singleton
public class UploadConfig {

	private final File imageTmpDir;
	private final File imageLocalSaveDir;
	
	private final boolean imageQiniuEnable;
	private final String imageQiniuBucketName;
	private final String imageQiniuKeyPrefix;
	private final String imageQiniuAccessKey;
	private final String imageQiniuSecretKey;
	
	private final String imageUrlPrefixBase;
	
	private final String imageMagickSearchPath;
	
	private final File videoTmpDir;
	private final File videoLocalSaveDir;
	
	private final boolean videoQiniuEnable;
	private final String videoQiniuBucketName;
	private final String videoQiniuKeyPrefix;
	private final String videoQiniuAccessKey;
	private final String videoQiniuSecretKey;
	
	private final String videoUrlPrefix;
	private final String ffmpegSearchPath;
	
	@Inject
	public UploadConfig(@Named("server_conf") Properties confProperties) {
		// image config
		
		this.imageTmpDir = ConfigUtil.getDirIfNonExistCreate(confProperties, "upload_image_tmp_dir");
		this.imageLocalSaveDir = ConfigUtil.getDir(confProperties, "upload_image_local_save_dir");
		this.imageQiniuEnable = ConfigUtil.getBoolean(confProperties, "upload_image_qiniu_enable");
		
		if (this.imageQiniuEnable) {
			this.imageQiniuBucketName = ConfigUtil.getNotEmpty(confProperties, "upload_image_qiniu_backet_name");
			String imageQiniuKeyPrefix = ConfigUtil.getNotEmpty(confProperties, "upload_image_qiniu_key_prefix");
			if (!imageQiniuKeyPrefix.endsWith("/")) {
				imageQiniuKeyPrefix += "/";
			}
			this.imageQiniuKeyPrefix = imageQiniuKeyPrefix;
			this.imageQiniuAccessKey = ConfigUtil.getNotEmpty(confProperties, "upload_image_qiniu_access_key");
			this.imageQiniuSecretKey = ConfigUtil.getNotEmpty(confProperties, "upload_image_qiniu_secret_key");
		} else {
			this.imageQiniuBucketName = null;
			this.imageQiniuKeyPrefix = null;
			this.imageQiniuAccessKey = null;
			this.imageQiniuSecretKey = null;
		}
		
		this.imageUrlPrefixBase = ConfigUtil.getNotEmpty(confProperties, "upload_image_url_prefix_base");
		this.imageMagickSearchPath = ConfigUtil.getNotEmpty(confProperties, "upload_image_magick_search_path");
		
		// video config
		
		this.videoTmpDir = ConfigUtil.getDirIfNonExistCreate(confProperties, "upload_video_tmp_dir");
		this.videoLocalSaveDir = ConfigUtil.getDir(confProperties, "upload_video_local_save_dir");
		this.videoQiniuEnable = ConfigUtil.getBoolean(confProperties, "upload_video_qiniu_enable");
		
		if (this.videoQiniuEnable) {
			this.videoQiniuBucketName = ConfigUtil.getNotEmpty(confProperties, "upload_video_qiniu_backet_name");
			String videoQiniuKeyPrefix = ConfigUtil.getNotEmpty(confProperties, "upload_video_qiniu_key_prefix");
			if (!videoQiniuKeyPrefix.endsWith("/")) {
				videoQiniuKeyPrefix += "/";
			}
			this.videoQiniuKeyPrefix = videoQiniuKeyPrefix;
			this.videoQiniuAccessKey = ConfigUtil.getNotEmpty(confProperties, "upload_video_qiniu_access_key");
			this.videoQiniuSecretKey = ConfigUtil.getNotEmpty(confProperties, "upload_video_qiniu_secret_key");
		} else {
			this.videoQiniuBucketName = null;
			this.videoQiniuKeyPrefix = null;
			this.videoQiniuAccessKey = null;
			this.videoQiniuSecretKey = null;
		}
		
		this.videoUrlPrefix = ConfigUtil.getNotEmpty(confProperties, "upload_video_url_prefix");
		this.ffmpegSearchPath = ConfigUtil.getNotEmpty(confProperties, "upload_ffmpeg_search_path");
	}
	
	public File imageTmpDir() {
		return imageTmpDir;
	}
	
	public File imageLocalSaveDir() {
		return imageLocalSaveDir;
	}

	public boolean imageQiniuEnable() {
		return imageQiniuEnable;
	}

	public String imageQiniuBucketName() {
		return imageQiniuBucketName;
	}
	
	public String imageQiniuKeyPrefix() {
		return imageQiniuKeyPrefix;
	}

	public String imageQiniuAccessKey() {
		return imageQiniuAccessKey;
	}

	public String imageQiniuSecretKey() {
		return imageQiniuSecretKey;
	}
	
	public String imageUrlPrefixBase() {
		return imageUrlPrefixBase;
	}
	
	public String imageMagickSearchPath() {
		return imageMagickSearchPath;
	}

	public File videoTmpDir() {
		return videoTmpDir;
	}

	public File videoLocalSaveDir() {
		return videoLocalSaveDir;
	}

	public boolean videoQiniuEnable() {
		return videoQiniuEnable;
	}

	public String videoQiniuBucketName() {
		return videoQiniuBucketName;
	}

	public String videoQiniuKeyPrefix() {
		return videoQiniuKeyPrefix;
	}

	public String videoQiniuAccessKey() {
		return videoQiniuAccessKey;
	}

	public String videoQiniuSecretKey() {
		return videoQiniuSecretKey;
	}

	public String videoUrlPrefix() {
		return videoUrlPrefix;
	}

	public String ffmpegSearchPath() {
		return ffmpegSearchPath;
	}
}
