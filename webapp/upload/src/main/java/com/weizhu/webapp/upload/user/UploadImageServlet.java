package com.weizhu.webapp.upload.user;

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
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.google.protobuf.ByteString;
import com.weizhu.common.utils.JsonUtil;
import com.weizhu.proto.UploadProtos;
import com.weizhu.proto.UploadProtos.UploadImageRequest;
import com.weizhu.proto.UploadProtos.UploadImageResponse;
import com.weizhu.proto.UploadService;
import com.weizhu.proto.WeizhuProtos.RequestHead;
import com.weizhu.web.ParamUtil;

@Singleton
@SuppressWarnings("serial")
public class UploadImageServlet extends HttpServlet {

	private final Provider<RequestHead> requestHeadProvider;
	private final UploadService uploadService;
	
	private final File imageTmpDir;
	
	@Inject
	public UploadImageServlet(
			Provider<RequestHead> requestHeadProvider, 
			UploadService uploadService,
			@Named("upload_image_tmp_dir") File imageTmpDir
			) {
		this.requestHeadProvider = requestHeadProvider;
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
		Set<String> imageTagSet = new TreeSet<String>();
		for (FileItem item : fileItemList) {
			if (("upload_file".equals(item.getFieldName()) || "upload_image".equals(item.getFieldName())) && !item.isFormField()) {
				uploadFileItem = item;
			} else if ("image_tag".equals(item.getFieldName()) && item.isFormField()) {
				imageTagSet.addAll(ParamUtil.COMMA_SPLITTER.splitToList(item.getString("UTF-8")));
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
		
		final RequestHead head = this.requestHeadProvider.get();
		
		imageTagSet.add("用户上传");
		Iterator<String> tagIt = imageTagSet.iterator();
		while (tagIt.hasNext()) {
			String tag = tagIt.next();
			if (tag.length() <= 0 || tag.length() > 20) {
				tagIt.remove();
			}
		}
		
		final ByteString imageData = ByteString.readFrom(uploadFileItem.getInputStream());
		
		UploadImageResponse response = Futures.getUnchecked(
				this.uploadService.uploadImage(head, UploadImageRequest.newBuilder()
				.setImageData(imageData)
				.addAllTag(imageTagSet)
				.build()));
		
		if (response.getResult() == UploadImageResponse.Result.SUCC) {
			UploadProtos.Image image = response.getImage();
			
			JsonObject obj = new JsonObject();
			obj.addProperty("result", "SUCC");
			obj.addProperty("image_name", image.getName());
			obj.addProperty("image_type", image.getType());
			obj.addProperty("image_size", image.getSize());
			obj.addProperty("image_md5", image.getMd5());
			obj.addProperty("image_width", image.getWidth());
			obj.addProperty("image_hight", image.getHight());
			
			JsonArray tagArr = new JsonArray();
			for (String tag : image.getTagList()) {
				tagArr.add(new JsonPrimitive(tag));
			}
			obj.add("image_tag", tagArr);
			
			obj.addProperty("image_url", "");
			obj.addProperty("image_60_url", "");
			obj.addProperty("image_120_url", "");
			obj.addProperty("image_240_url", "");
			obj.addProperty("image_480_url", "");
			
			httpResponse.setContentType("application/json;charset=UTF-8");
			JsonUtil.GSON.toJson(obj, httpResponse.getWriter());
		} else {
			httpResponse.setContentType("application/json;charset=UTF-8");
			JsonUtil.PROTOBUF_JSON_FORMAT.print(response, httpResponse.getWriter());
		}
	}
}
