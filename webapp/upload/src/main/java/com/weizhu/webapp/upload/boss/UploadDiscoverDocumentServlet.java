package com.weizhu.webapp.upload.boss;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
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
import com.google.common.hash.Funnels;
import com.google.common.hash.Hasher;
import com.google.common.hash.Hashing;
import com.google.common.io.ByteStreams;
import com.google.common.io.Files;
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
public class UploadDiscoverDocumentServlet extends HttpServlet {

	@SuppressWarnings("unused")
	private final Provider<BossHead> bossHeadProvider;
	
	private final File discoverTmpDir;
	private final File discoverDocumentLocalSaveDir;
	
	private final UploadManager qiniuUploadManager;
	private final boolean discoverDocumentQiniuEnable;
	private final String discoverDocumentQiniuBacketName;
	private final String discoverDocumentQiniuKeyPrefix;
	private final Auth discoverDocumentQiniuAuth;
	
	private final String discoverDocumentUrlPrefix;
	private final boolean discoverDocumentAuthEnable;
	
	@Inject
	public UploadDiscoverDocumentServlet(
			Provider<BossHead> bossHeadProvider, 
			@Named("upload_discover_tmp_dir") File discoverTmpDir,
			@Named("upload_discover_document_local_save_dir") File discoverDocumentLocalSaveDir, 
			UploadManager qiniuUploadManager, 
			@Named("upload_discover_document_qiniu_enable") boolean discoverDocumentQiniuEnable,
			@Named("upload_discover_document_qiniu_backet_name") @Nullable String discoverDocumentQiniuBacketName, 
			@Named("upload_discover_document_qiniu_key_prefix") @Nullable String discoverDocumentQiniuKeyPrefix, 
			@Named("upload_discover_document_qiniu_access_key") @Nullable String discoverDocumentQiniuAccessKey,
			@Named("upload_discover_document_qiniu_secret_key") @Nullable String discoverDocumentQiniuSecretKey, 
			@Named("upload_discover_document_url_prefix") String discoverDocumentUrlPrefix,
			@Named("upload_discover_document_auth_enable") boolean discoverDocumentAuthEnable
			) {
		this.bossHeadProvider = bossHeadProvider;
		this.discoverTmpDir = discoverTmpDir;
		this.discoverDocumentLocalSaveDir = discoverDocumentLocalSaveDir;
		this.qiniuUploadManager = qiniuUploadManager;
		this.discoverDocumentQiniuEnable = discoverDocumentQiniuEnable;
		this.discoverDocumentQiniuBacketName = discoverDocumentQiniuBacketName;
		this.discoverDocumentQiniuKeyPrefix = discoverDocumentQiniuKeyPrefix;
		
		if (this.discoverDocumentQiniuEnable) {
			this.discoverDocumentQiniuAuth = Auth.create(discoverDocumentQiniuAccessKey, discoverDocumentQiniuSecretKey);
		} else {
			this.discoverDocumentQiniuAuth = null;
		}

		this.discoverDocumentUrlPrefix = discoverDocumentUrlPrefix;
		this.discoverDocumentAuthEnable = discoverDocumentAuthEnable;
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
			obj.addProperty("fail_text", "上传文档大小超过最大值20M");
			
			httpResponse.setContentType("application/json;charset=UTF-8");
			JsonUtil.GSON.toJson(obj, httpResponse.getWriter());
			return;
		} catch (FileSizeLimitExceededException e) {
			JsonObject obj = new JsonObject();
			obj.addProperty("result", "FAIL_UNKNOWN");
			obj.addProperty("fail_text", "上传文档大小超过最大值20M");
			
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
		
		Document document = this.getDocumentInfo(companyId, tmpFile, md5, size, uploadFileItem);
		if (document == null) {
			JsonObject obj = new JsonObject();
			obj.addProperty("result", "FAIL_UNKNOWN");
			obj.addProperty("fail_text", "文档格式错误");
			
			httpResponse.setContentType("application/json;charset=UTF-8");
			JsonUtil.GSON.toJson(obj, httpResponse.getWriter());
			return;
		}
		
		this.saveDocument(tmpFile, document);
		
		uploadFileItem.delete();
		tmpFile.delete();
		
		JsonObject obj = new JsonObject();
		obj.addProperty("result", "SUCC");
		obj.addProperty("file_name", document.name);
		obj.addProperty("file_url", this.discoverDocumentUrlPrefix + document.name);
		obj.addProperty("file_size", document.size);
		obj.addProperty("is_auth", this.discoverDocumentAuthEnable);
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
	
	private Document getDocumentInfo(long companyId, File audioFile, String md5, int size, FileItem fileItem) {
		
		String type = null;
		if (fileItem.getName().toLowerCase().endsWith(".pdf")) {
			type = "pdf";
		}
		
		if (type == null) {
			return null;
		}
		
		String name = companyId + "/discover/document/" + md5 + "." + type;
		return new Document(name, type, size, md5);
	}
	
	@SuppressWarnings("unused")
	private static class Document {
		final String name;
		final String type;
		final int size;
		final String md5;
		
		Document(String name, String type, int size, String md5) {
			this.name = name;
			this.type = type;
			this.size = size;
			this.md5 = md5;
		}
	}
	
	private void saveDocument(File tmpDocumentFile, Document document) {
		try {
			File localSaveFile = new File(this.discoverDocumentLocalSaveDir.getAbsolutePath() + "/" + document.name);
			Files.createParentDirs(localSaveFile);
			Files.copy(tmpDocumentFile, localSaveFile);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		
		if (this.discoverDocumentQiniuEnable) {
			final String uploadQiniuDocumentKey = this.discoverDocumentQiniuKeyPrefix + document.name;
			final String uploadQiniuDocumentToken = this.discoverDocumentQiniuAuth.uploadToken(this.discoverDocumentQiniuBacketName, uploadQiniuDocumentKey);
			try {
				Response qiniuResponse = this.qiniuUploadManager.put(tmpDocumentFile, uploadQiniuDocumentKey, uploadQiniuDocumentToken);
				if (!qiniuResponse.isOK()) {
					throw new RuntimeException("upload qiniu fail : " + qiniuResponse.toString());
				}
			} catch (QiniuException e) {
				throw new RuntimeException(e);
			}
		}
	}
	
}
