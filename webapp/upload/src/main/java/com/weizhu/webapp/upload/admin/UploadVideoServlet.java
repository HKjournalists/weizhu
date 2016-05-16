package com.weizhu.webapp.upload.admin;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

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

import com.google.common.util.concurrent.Futures;
import com.google.gson.JsonObject;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.google.protobuf.ByteString;
import com.weizhu.common.utils.JsonUtil;
import com.weizhu.proto.UploadService;
import com.weizhu.proto.AdminProtos.AdminHead;
import com.weizhu.proto.UploadProtos.UploadVideoRequest;
import com.weizhu.proto.UploadProtos.UploadVideoResponse;
import com.weizhu.web.ParamUtil;

@Singleton
@SuppressWarnings("serial")
public class UploadVideoServlet extends HttpServlet {

	private final Provider<AdminHead> adminHeadProvider;
	private final UploadService uploadService;
	
	private final File videoTmpDir;
	
	@Inject
	public UploadVideoServlet(
			Provider<AdminHead> adminHeadProvider, 
			UploadService uploadService,
			@Named("upload_video_tmp_dir") File videoTmpDir
			) {
		this.adminHeadProvider = adminHeadProvider;
		this.uploadService = uploadService;
		this.videoTmpDir = videoTmpDir;
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
		
		ServletFileUpload upload = new ServletFileUpload(new DiskFileItemFactory(128 * 1024, this.videoTmpDir));
		upload.setSizeMax(11 * 1024 * 1024);
		upload.setFileSizeMax(10 * 1024 * 1024);
		
     	final List<FileItem> fileItemList;
     	try {
			fileItemList = upload.parseRequest(httpRequest);
		} catch (SizeLimitExceededException e) {
			JsonObject obj = new JsonObject();
			obj.addProperty("result", "FAIL_UNKNOWN");
			obj.addProperty("fail_text", "上传视频大小超过最大值10M");
			
			httpResponse.setContentType("application/json;charset=UTF-8");
			JsonUtil.GSON.toJson(obj, httpResponse.getWriter());
			return;
		} catch (FileSizeLimitExceededException e) {
			JsonObject obj = new JsonObject();
			obj.addProperty("result", "FAIL_UNKNOWN");
			obj.addProperty("fail_text", "上传视频大小超过最大值10M");
			
			httpResponse.setContentType("application/json;charset=UTF-8");
			JsonUtil.GSON.toJson(obj, httpResponse.getWriter());
			return;
		} catch (FileUploadException e) {
			throw new ServletException("file upload fail", e);
		}
     	
		FileItem uploadFileItem = null;
		Set<String> videoTagSet = new TreeSet<String>();
		for (FileItem item : fileItemList) {
			if (("upload_file".equals(item.getFieldName()) || "upload_video".equals(item.getFieldName())) && !item.isFormField()) {
				uploadFileItem = item;
			} else if ("video_tag".equals(item.getFieldName()) && item.isFormField()) {
				videoTagSet.addAll(ParamUtil.COMMA_SPLITTER.splitToList(item.getString("UTF-8")));
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
		
		final AdminHead head = this.adminHeadProvider.get();
		
		videoTagSet.add("管理员上传");
		Iterator<String> tagIt = videoTagSet.iterator();
		while (tagIt.hasNext()) {
			String tag = tagIt.next();
			if (tag.length() <= 0 || tag.length() > 20) {
				tagIt.remove();
			}
		}
		
		final ByteString videoData = ByteString.readFrom(uploadFileItem.getInputStream());
		
		UploadVideoResponse response = Futures.getUnchecked(
				this.uploadService.uploadVideo(head, UploadVideoRequest.newBuilder()
				.setVideoData(videoData)
				.addAllTag(videoTagSet)
				.build()));
		
		httpResponse.setContentType("application/json;charset=UTF-8");
		JsonUtil.PROTOBUF_JSON_FORMAT.print(response, httpResponse.getWriter());
	}
}

