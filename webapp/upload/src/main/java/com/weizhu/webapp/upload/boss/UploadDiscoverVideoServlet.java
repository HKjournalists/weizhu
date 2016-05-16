package com.weizhu.webapp.upload.boss;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;
import javax.annotation.Nullable;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.FileUploadBase.FileSizeLimitExceededException;
import org.apache.commons.fileupload.FileUploadBase.SizeLimitExceededException;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;

import com.google.common.base.Charsets;
import com.google.common.hash.Funnels;
import com.google.common.hash.Hasher;
import com.google.common.hash.Hashing;
import com.google.common.io.ByteStreams;
import com.google.common.io.Files;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.qiniu.common.QiniuException;
import com.qiniu.http.Response;
import com.qiniu.storage.UploadManager;
import com.qiniu.util.Auth;
import com.weizhu.common.utils.JsonUtil;
import com.weizhu.proto.BossProtos.BossHead;

@Singleton
@SuppressWarnings("serial")
public class UploadDiscoverVideoServlet extends HttpServlet {

	@SuppressWarnings("unused")
	private final Provider<BossHead> bossHeadProvider;
	
	private final File discoverTmpDir;
	private final File discoverVideoLocalSaveDir;
	
	private final UploadManager qiniuUploadManager;
	private final boolean discoverVideoQiniuEnable;
	private final String discoverVideoQiniuBacketName;
	private final String discoverVideoQiniuKeyPrefix;
	private final Auth discoverVideoQiniuAuth;
	
	private final String discoverVideoUrlPrefix;
	private final boolean discoverVideoAuthEnable;
	
	private final String ffmpegSearchPath;
	
	@Inject
	public UploadDiscoverVideoServlet(
			Provider<BossHead> bossHeadProvider, 
			@Named("upload_discover_tmp_dir") File discoverTmpDir,
			@Named("upload_discover_video_local_save_dir") File discoverVideoLocalSaveDir, 
			UploadManager qiniuUploadManager, 
			@Named("upload_discover_video_qiniu_enable") boolean discoverVideoQiniuEnable,
			@Named("upload_discover_video_qiniu_backet_name") @Nullable String discoverVideoQiniuBacketName, 
			@Named("upload_discover_video_qiniu_key_prefix") @Nullable String discoverVideoQiniuKeyPrefix, 
			@Named("upload_discover_video_qiniu_access_key") @Nullable String discoverVideoQiniuAccessKey,
			@Named("upload_discover_video_qiniu_secret_key") @Nullable String discoverVideoQiniuSecretKey, 
			@Named("upload_discover_video_url_prefix") String discoverVideoUrlPrefix,
			@Named("upload_discover_video_auth_enable") boolean discoverVideoAuthEnable,
			@Named("upload_ffmpeg_search_path") String ffmpegSearchPath
			) {
		this.bossHeadProvider = bossHeadProvider;
		this.discoverTmpDir = discoverTmpDir;
		this.discoverVideoLocalSaveDir = discoverVideoLocalSaveDir;
		this.qiniuUploadManager = qiniuUploadManager;
		this.discoverVideoQiniuEnable = discoverVideoQiniuEnable;
		this.discoverVideoQiniuBacketName = discoverVideoQiniuBacketName;
		this.discoverVideoQiniuKeyPrefix = discoverVideoQiniuKeyPrefix;
		
		if (this.discoverVideoQiniuEnable) {
			this.discoverVideoQiniuAuth = Auth.create(discoverVideoQiniuAccessKey, discoverVideoQiniuSecretKey);
		} else {
			this.discoverVideoQiniuAuth = null;
		}

		this.discoverVideoUrlPrefix = discoverVideoUrlPrefix;
		this.discoverVideoAuthEnable = discoverVideoAuthEnable;
		this.ffmpegSearchPath = ffmpegSearchPath;
	}
	
	@Override
	protected void doPost(HttpServletRequest httpRequest, HttpServletResponse httpResponse) throws ServletException, IOException {
		boolean isMultipart = ServletFileUpload.isMultipartContent(httpRequest);
		if (!isMultipart) {
			JsonObject obj = new JsonObject();
			obj.addProperty("result", "FAIL_UNKNOWN");
			obj.addProperty("fail_text", "请使用mutipart请求上传文件");
			
			httpResponse.setContentType("application/json;charset=UTF-8");
			JsonUtil.GSON.toJson(obj, httpResponse.getWriter());
			return;
		}
		
		ServletFileUpload upload = new ServletFileUpload(new DiskFileItemFactory(128 * 1024, this.discoverTmpDir));
		upload.setSizeMax(41 * 1024 * 1024);
		upload.setFileSizeMax(40 * 1024 * 1024);
		
     	final List<FileItem> fileItemList;
     	try {
			fileItemList = upload.parseRequest(httpRequest);
		} catch (SizeLimitExceededException e) {
			JsonObject obj = new JsonObject();
			obj.addProperty("result", "FAIL_UNKNOWN");
			obj.addProperty("fail_text", "上传视频大小超过最大值40M");
			
			httpResponse.setContentType("application/json;charset=UTF-8");
			JsonUtil.GSON.toJson(obj, httpResponse.getWriter());
			return;
		} catch (FileSizeLimitExceededException e) {
			JsonObject obj = new JsonObject();
			obj.addProperty("result", "FAIL_UNKNOWN");
			obj.addProperty("fail_text", "上传视频大小超过最大值40M");
			
			httpResponse.setContentType("application/json;charset=UTF-8");
			JsonUtil.GSON.toJson(obj, httpResponse.getWriter());
			return;
		} catch (FileUploadException e) {
			throw new ServletException("file upload fail", e);
		}
     	
		FileItem uploadFileItem = null;
		Long companyId = null;
		for (FileItem item : fileItemList) {
			if ("upload_file".equals(item.getFieldName()) && !item.isFormField()) {
				uploadFileItem = item;
			} else if ("company_id".equals(item.getFieldName()) && item.isFormField()) {
				try {
					companyId = Long.parseLong(item.getString().trim());
				} catch (NumberFormatException e) {
					// ignore
				}
			}
		}
		
		if (uploadFileItem == null) {
			JsonObject obj = new JsonObject();
			obj.addProperty("result", "FAIL_UNKNOWN");
			obj.addProperty("fail_text", "upload_file参数未找到");
			
			httpResponse.setContentType("application/json;charset=UTF-8");
			JsonUtil.GSON.toJson(obj, httpResponse.getWriter());
			return;
		}
		
		if (companyId == null) {
			JsonObject obj = new JsonObject();
			obj.addProperty("result", "FAIL_UNKNOWN");
			obj.addProperty("fail_text", "company_id参数未找到");
			
			httpResponse.setContentType("application/json;charset=UTF-8");
			JsonUtil.GSON.toJson(obj, httpResponse.getWriter());
			return;
		}
		
		final String md5 = hashMd5(uploadFileItem);
		final int size = (int) uploadFileItem.getSize();
		final File tmpFile = new File(this.discoverTmpDir, md5 + ".tmp");
		
		try {
			uploadFileItem.write(tmpFile);
		} catch (IOException e) {
			throw e;
		} catch (Exception e) {
			throw new ServletException(e);
		}
		
		Video video = this.getVideoInfo(companyId, tmpFile, md5, size);
		if (video == null) {
			JsonObject obj = new JsonObject();
			obj.addProperty("result", "FAIL_UNKNOWN");
			obj.addProperty("fail_text", "视频格式错误");
			
			httpResponse.setContentType("application/json;charset=UTF-8");
			JsonUtil.GSON.toJson(obj, httpResponse.getWriter());
			return;
		}
		
		this.saveVideo(tmpFile, video);
		
		uploadFileItem.delete();
		tmpFile.delete();
		
		JsonObject obj = new JsonObject();
		obj.addProperty("result", "SUCC");
		obj.addProperty("file_name", video.name);
		obj.addProperty("file_url", this.discoverVideoUrlPrefix + video.name);
		obj.addProperty("file_size", video.size);
		obj.addProperty("time", video.time);
		obj.addProperty("is_auth", this.discoverVideoAuthEnable);
		obj.addProperty("check_md5", md5);
		
		httpResponse.setContentType("application/json;charset=UTF-8");
		JsonUtil.GSON.toJson(obj, httpResponse.getWriter());
	}
	
	private static String hashMd5(FileItem fileItem) throws IOException {
		InputStream in = fileItem.getInputStream();
		try {
			Hasher hasher = Hashing.md5().newHasher();
			ByteStreams.copy(in, Funnels.asOutputStream(hasher));
			return hasher.hash().toString();
		} finally {
			in.close();
		}
	}
	
	private Video getVideoInfo(long companyId, File videoFile, String md5, int size) {
		final String videoFormat;
		final Double videoTime;
		try {
			Process process = new ProcessBuilder(
					this.ffmpegSearchPath + "/ffprobe", "-v", "quiet", "-print_format", "json", "-show_format", 
					videoFile.getAbsolutePath()).start();
			JsonElement json = JsonUtil.JSON_PARSER.parse(new InputStreamReader(process.getInputStream(), Charsets.UTF_8));
			videoFormat = JsonUtil.tryGetString(json, "format.format_name");
			videoTime = JsonUtil.tryGetDouble(json, "format.duration");
		} catch (IOException e) {
			return null;
		}
		
		if (videoFormat == null || videoTime == null) {
			return null;
		}
		
		final String type;
		if ("mov,mp4,m4a,3gp,3g2,mj2".equals(videoFormat)) {
			type = "mp4";
		} /*if ("avi".equals(videoFormat)) {
			type = "avi";
		} */
		else {
			return null;
		}
		final int time = (int) Math.round(videoTime);
		
		String name = companyId + "/discover/video/" + md5 + "." + type;
		return new Video(name, type, size, md5, time);
	}
	
	@SuppressWarnings("unused")
	private static class Video {
		final String name;
		final String type;
		final int size;
		final String md5;
		final int time;
		
		Video(String name, String type, int size, String md5, int time) {
			this.name = name;
			this.type = type;
			this.size = size;
			this.md5 = md5;
			this.time = time;
		}
	}
	
	private void saveVideo(File tmpVideoFile, Video video) {
		try {
			File localSaveFile = new File(this.discoverVideoLocalSaveDir.getAbsolutePath() + "/" + video.name);
			Files.createParentDirs(localSaveFile);
			Files.copy(tmpVideoFile, localSaveFile);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		
		if (this.discoverVideoQiniuEnable) {
			final String uploadQiniuAudioKey = this.discoverVideoQiniuKeyPrefix + video.name;
			final String uploadQiniuAudioToken = this.discoverVideoQiniuAuth.uploadToken(this.discoverVideoQiniuBacketName, uploadQiniuAudioKey);
			try {
				Response qiniuResponse = this.qiniuUploadManager.put(tmpVideoFile, uploadQiniuAudioKey, uploadQiniuAudioToken);
				if (!qiniuResponse.isOK()) {
					throw new RuntimeException("upload qiniu fail : " + qiniuResponse.toString());
				}
			} catch (QiniuException e) {
				throw new RuntimeException(e);
			}
		}
	}
	
}
