package com.weizhu.webapp.upload.admin;

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
import com.weizhu.proto.AdminProtos.AdminHead;

@Singleton
@SuppressWarnings("serial")
public class UploadDiscoverAudioServlet extends HttpServlet {

	private final Provider<AdminHead> adminHeadProvider;
	
	private final File discoverTmpDir;
	private final File discoverAudioLocalSaveDir;
	
	private final UploadManager qiniuUploadManager;
	private final boolean discoverAudioQiniuEnable;
	private final String discoverAudioQiniuBacketName;
	private final String discoverAudioQiniuKeyPrefix;
	private final Auth discoverAudioQiniuAuth;
	
	private final String discoverAudioUrlPrefix;
	private final boolean discoverAudioAuthEnable;
	
	private final String ffmpegSearchPath;
	
	@Inject
	public UploadDiscoverAudioServlet(
			Provider<AdminHead> adminHeadProvider, 
			@Named("upload_discover_tmp_dir") File discoverTmpDir,
			@Named("upload_discover_audio_local_save_dir") File discoverAudioLocalSaveDir, 
			UploadManager qiniuUploadManager, 
			@Named("upload_discover_audio_qiniu_enable") boolean discoverAudioQiniuEnable,
			@Named("upload_discover_audio_qiniu_backet_name") @Nullable String discoverAudioQiniuBacketName, 
			@Named("upload_discover_audio_qiniu_key_prefix") @Nullable String discoverAudioQiniuKeyPrefix, 
			@Named("upload_discover_audio_qiniu_access_key") @Nullable String discoverAudioQiniuAccessKey,
			@Named("upload_discover_audio_qiniu_secret_key") @Nullable String discoverAudioQiniuSecretKey, 
			@Named("upload_discover_audio_url_prefix") String discoverAudioUrlPrefix,
			@Named("upload_discover_audio_auth_enable") boolean discoverAudioAuthEnable,
			@Named("upload_ffmpeg_search_path") String ffmpegSearchPath
			) {
		this.adminHeadProvider = adminHeadProvider;
		this.discoverTmpDir = discoverTmpDir;
		this.discoverAudioLocalSaveDir = discoverAudioLocalSaveDir;
		this.qiniuUploadManager = qiniuUploadManager;
		this.discoverAudioQiniuEnable = discoverAudioQiniuEnable;
		this.discoverAudioQiniuBacketName = discoverAudioQiniuBacketName;
		this.discoverAudioQiniuKeyPrefix = discoverAudioQiniuKeyPrefix;
		
		if (this.discoverAudioQiniuEnable) {
			this.discoverAudioQiniuAuth = Auth.create(discoverAudioQiniuAccessKey, discoverAudioQiniuSecretKey);
		} else {
			this.discoverAudioQiniuAuth = null;
		}

		this.discoverAudioUrlPrefix = discoverAudioUrlPrefix;
		this.discoverAudioAuthEnable = discoverAudioAuthEnable;
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
		upload.setSizeMax(21 * 1024 * 1024);
		upload.setFileSizeMax(20 * 1024 * 1024);
		
     	final List<FileItem> fileItemList;
     	try {
			fileItemList = upload.parseRequest(httpRequest);
		} catch (SizeLimitExceededException e) {
			JsonObject obj = new JsonObject();
			obj.addProperty("result", "FAIL_UNKNOWN");
			obj.addProperty("fail_text", "上传音频大小超过最大值20M");
			
			httpResponse.setContentType("application/json;charset=UTF-8");
			JsonUtil.GSON.toJson(obj, httpResponse.getWriter());
			return;
		} catch (FileSizeLimitExceededException e) {
			JsonObject obj = new JsonObject();
			obj.addProperty("result", "FAIL_UNKNOWN");
			obj.addProperty("fail_text", "上传音频大小超过最大值20M");
			
			httpResponse.setContentType("application/json;charset=UTF-8");
			JsonUtil.GSON.toJson(obj, httpResponse.getWriter());
			return;
		} catch (FileUploadException e) {
			throw new ServletException("file upload fail", e);
		}
     	
		FileItem uploadFileItem = null;
		for (FileItem item : fileItemList) {
			if ("upload_file".equals(item.getFieldName()) && !item.isFormField()) {
				uploadFileItem = item;
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
		
		final AdminHead head = this.adminHeadProvider.get();
		
		Audio audio = this.getAudioInfo(head, tmpFile, md5, size);
		if (audio == null) {
			JsonObject obj = new JsonObject();
			obj.addProperty("result", "FAIL_UNKNOWN");
			obj.addProperty("fail_text", "音频格式错误");
			
			httpResponse.setContentType("application/json;charset=UTF-8");
			JsonUtil.GSON.toJson(obj, httpResponse.getWriter());
			return;
		}
		
		this.saveAudio(tmpFile, audio);
		
		uploadFileItem.delete();
		tmpFile.delete();
		
		JsonObject obj = new JsonObject();
		obj.addProperty("result", "SUCC");
		obj.addProperty("file_name", audio.name);
		obj.addProperty("file_url", this.discoverAudioUrlPrefix + audio.name);
		obj.addProperty("file_size", audio.size);
		obj.addProperty("time", audio.time);
		obj.addProperty("is_auth", this.discoverAudioAuthEnable);
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
	
	private Audio getAudioInfo(AdminHead head, File audioFile, String md5, int size) {
		final String audioFormat;
		final Double audioTime;
		try {
			Process process = new ProcessBuilder(
					this.ffmpegSearchPath + "/ffprobe", "-v", "quiet", "-print_format", "json", "-show_format", 
					audioFile.getAbsolutePath()).start();
			JsonElement json = JsonUtil.JSON_PARSER.parse(new InputStreamReader(process.getInputStream(), Charsets.UTF_8));
			audioFormat = JsonUtil.tryGetString(json, "format.format_name");
			audioTime = JsonUtil.tryGetDouble(json, "format.duration");
		} catch (IOException e) {
			return null;
		}
		
		if (audioFormat == null || audioTime == null) {
			return null;
		}
		
		final String type;
		if ("mp3".equals(audioFormat)) {
			type = "mp3";
		} 
		else {
			return null;
		}
		final int time = (int) Math.round(audioTime);
		
		String name = head.getCompanyId() + "/discover/audio/" + md5 + "." + type;
		return new Audio(name, type, size, md5, time);
	}
	
	@SuppressWarnings("unused")
	private static class Audio {
		final String name;
		final String type;
		final int size;
		final String md5;
		final int time;
		
		Audio(String name, String type, int size, String md5, int time) {
			this.name = name;
			this.type = type;
			this.size = size;
			this.md5 = md5;
			this.time = time;
		}
	}
	
	private void saveAudio(File tmpAudioFile, Audio audio) {
		try {
			File localSaveFile = new File(this.discoverAudioLocalSaveDir.getAbsolutePath() + "/" + audio.name);
			Files.createParentDirs(localSaveFile);
			Files.copy(tmpAudioFile, localSaveFile);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		
		if (this.discoverAudioQiniuEnable) {
			final String uploadQiniuAudioKey = this.discoverAudioQiniuKeyPrefix + audio.name;
			final String uploadQiniuAudioToken = this.discoverAudioQiniuAuth.uploadToken(this.discoverAudioQiniuBacketName, uploadQiniuAudioKey);
			try {
				Response qiniuResponse = this.qiniuUploadManager.put(tmpAudioFile, uploadQiniuAudioKey, uploadQiniuAudioToken);
				if (!qiniuResponse.isOK()) {
					throw new RuntimeException("upload qiniu fail : " + qiniuResponse.toString());
				}
			} catch (QiniuException e) {
				throw new RuntimeException(e);
			}
		}
	}
	
}
