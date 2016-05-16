package com.weizhu.webapp.upload.user;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
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
import com.google.common.hash.Funnels;
import com.google.common.hash.Hasher;
import com.google.common.hash.Hashing;
import com.google.common.io.ByteStreams;
import com.google.common.util.concurrent.Futures;
import com.google.gson.JsonObject;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.google.protobuf.ByteString;
import com.weizhu.common.utils.JsonUtil;
import com.weizhu.proto.UploadProtos.UploadImageRequest;
import com.weizhu.proto.UploadProtos.UploadImageResponse;
import com.weizhu.proto.UploadService;
import com.weizhu.proto.WeizhuProtos.AnonymousHead;

@Singleton
@SuppressWarnings("serial")
public class Old2UploadImageServlet extends HttpServlet {

	private final Provider<AnonymousHead> anonymousHeadProvider;
	private final UploadService uploadService;
	
	private final File imageTmpDir;
	
	@Inject
	public Old2UploadImageServlet(Provider<AnonymousHead> anonymousHeadProvider, 
			UploadService uploadService,
			@Named("upload_image_tmp_dir") File imageTmpDir
			) {
		this.anonymousHeadProvider = anonymousHeadProvider;
		this.uploadService = uploadService;
		this.imageTmpDir = imageTmpDir;
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
		
		ServletFileUpload upload = new ServletFileUpload(new DiskFileItemFactory(128 * 1024, this.imageTmpDir));
		upload.setSizeMax(11 * 1024 * 1024);
		upload.setFileSizeMax(10 * 1024 * 1024);
		
     	final List<FileItem> fileItemList;
     	try {
			fileItemList = upload.parseRequest(httpRequest);
		} catch (SizeLimitExceededException e) {
			JsonObject obj = new JsonObject();
			obj.addProperty("result", "FAIL_UNKNOWN");
			obj.addProperty("fail_text", "上传图片大小超过最大值10M");
			
			httpResponse.setContentType("application/json;charset=UTF-8");
			JsonUtil.GSON.toJson(obj, httpResponse.getWriter());
			return;
		} catch (FileSizeLimitExceededException e) {
			JsonObject obj = new JsonObject();
			obj.addProperty("result", "FAIL_UNKNOWN");
			obj.addProperty("fail_text", "上传图片大小超过最大值10M");
			
			httpResponse.setContentType("application/json;charset=UTF-8");
			JsonUtil.GSON.toJson(obj, httpResponse.getWriter());
			return;
		} catch (FileUploadException e) {
			throw new ServletException("file upload fail", e);
		}
     	
		FileItem uploadFileItem = null;
		String hashMd5 = null;
		for (FileItem item : fileItemList) {
			if (("upload_file".equals(item.getFieldName()) || "upload_content".equals(item.getFieldName())) && !item.isFormField()) {
				uploadFileItem = item;
			} else if ("hash_md5".equals(item.getFieldName()) && item.isFormField()) {
				hashMd5 = item.getString();
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
		
		if (hashMd5 == null) {
			JsonObject obj = new JsonObject();
			obj.addProperty("result", "FAIL_INVALID_PARAM");
			obj.addProperty("fail_text", "hash_md5参数未找到");
			
			httpResponse.setContentType("application/json;charset=UTF-8");
			JsonUtil.GSON.toJson(obj, httpResponse.getWriter());
			return;
		}
		
		final String md5 = hashMd5(uploadFileItem);
		
		if (!md5.equalsIgnoreCase(hashMd5)) {
			JsonObject obj = new JsonObject();
			obj.addProperty("result", "FAIL_MD5_CHECK");
			obj.addProperty("fail_text", "上传成功，md5校验失败");
			
			httpResponse.setContentType("application/json;charset=UTF-8");
			JsonUtil.GSON.toJson(obj, httpResponse.getWriter());
			return;
		}
		
		final AnonymousHead head = this.anonymousHeadProvider.get();
		final ByteString imageData = ByteString.readFrom(uploadFileItem.getInputStream());
		
		UploadImageResponse response = Futures.getUnchecked(
				this.uploadService.uploadImage(head, UploadImageRequest.newBuilder()
				.setImageData(imageData)
				.build()));
		
		if (response.getResult() == UploadImageResponse.Result.SUCC) {
			JsonObject obj = new JsonObject();
			obj.addProperty("result", "SUCC");
			obj.addProperty("name", response.getImage().getName());
			
			httpResponse.setContentType("application/json;charset=UTF-8");
			JsonUtil.GSON.toJson(obj, httpResponse.getWriter());
		} else {
			httpResponse.setContentType("application/json;charset=UTF-8");
			JsonUtil.PROTOBUF_JSON_FORMAT.print(response, httpResponse.getWriter());
		}
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
	
}
