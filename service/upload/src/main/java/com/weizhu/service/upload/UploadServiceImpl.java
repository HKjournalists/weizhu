package com.weizhu.service.upload;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.annotation.Nullable;

import org.im4java.core.ConvertCmd;
import org.im4java.core.IM4JavaException;
import org.im4java.core.IMOperation;
import org.im4java.core.IdentifyCmd;
import org.im4java.process.ArrayListOutputConsumer;

import com.google.common.base.Charsets;
import com.google.common.hash.Funnels;
import com.google.common.hash.Hasher;
import com.google.common.hash.Hashing;
import com.google.common.io.Files;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.gson.JsonElement;
import com.google.inject.Inject;
import com.google.protobuf.ByteString;
import com.qiniu.common.QiniuException;
import com.qiniu.http.Response;
import com.qiniu.storage.UploadManager;
import com.qiniu.util.Auth;
import com.weizhu.common.db.DBUtil;
import com.weizhu.common.service.AsyncImpl;
import com.weizhu.common.utils.JsonUtil;
import com.weizhu.proto.AdminProtos.AdminHead;
import com.weizhu.proto.BossProtos.BossHead;
import com.weizhu.proto.UploadProtos;
import com.weizhu.proto.UploadProtos.GetQiniuUploadImageTokenRequest;
import com.weizhu.proto.UploadProtos.GetQiniuUploadImageTokenResponse;
import com.weizhu.proto.UploadProtos.GetUploadUrlPrefixResponse;
import com.weizhu.proto.UploadProtos.SaveUploadImageActionRequest;
import com.weizhu.proto.UploadProtos.SaveUploadImageActionResponse;
import com.weizhu.proto.UploadProtos.UploadImageRequest;
import com.weizhu.proto.UploadProtos.UploadImageResponse;
import com.weizhu.proto.UploadProtos.UploadVideoRequest;
import com.weizhu.proto.UploadProtos.UploadVideoResponse;
import com.weizhu.proto.UploadService;
import com.weizhu.proto.WeizhuProtos.AnonymousHead;
import com.weizhu.proto.WeizhuProtos.EmptyRequest;
import com.weizhu.proto.WeizhuProtos.RequestHead;
import com.weizhu.proto.WeizhuProtos.SystemHead;
import com.zaxxer.hikari.HikariDataSource;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

public class UploadServiceImpl implements UploadService {
	
	private final HikariDataSource hikariDataSource;
	private final JedisPool jedisPool;
	
	private final UploadConfig config;
	private final UploadManager qiniuUploadManager;
	
	private final Auth imageQiniuAuth;
	private final Auth videoQiniuAuth;
	private final ListenableFuture<GetUploadUrlPrefixResponse> getUploadUrlPrefixResponse;
	private final ListenableFuture<SaveUploadImageActionResponse> saveUploadImageActionResponse;
	
	@Inject
	public UploadServiceImpl(HikariDataSource hikariDataSource, JedisPool jedisPool, UploadConfig config, UploadManager qiniuUploadManager){
		this.hikariDataSource = hikariDataSource;
		this.jedisPool = jedisPool;
		this.config = config;
		this.qiniuUploadManager = qiniuUploadManager;
		
		this.imageQiniuAuth = this.config.imageQiniuEnable() ? Auth.create(this.config.imageQiniuAccessKey(), this.config.imageQiniuSecretKey()) : null;
		this.videoQiniuAuth = this.config.videoQiniuEnable() ? Auth.create(this.config.videoQiniuAccessKey(), this.config.videoQiniuSecretKey()) : null;
		this.getUploadUrlPrefixResponse = Futures.immediateFuture(GetUploadUrlPrefixResponse.newBuilder()
				.setImageUrlPrefix(this.config.imageUrlPrefixBase() + "/original/")
				.setImage60UrlPrefix(this.config.imageUrlPrefixBase() + "/thumb60/")
				.setImage120UrlPrefix(this.config.imageUrlPrefixBase() + "/thumb120/")
				.setImage240UrlPrefix(this.config.imageUrlPrefixBase() + "/thumb240/")
				.setImage480UrlPrefix(this.config.imageUrlPrefixBase() + "/thumb480/")
				.setVideoUrlPrefix(this.config.videoUrlPrefix())
				.build());
		
		this.saveUploadImageActionResponse = Futures.immediateFuture(SaveUploadImageActionResponse.newBuilder()
				.setImageUrlPrefix(this.config.imageUrlPrefixBase() + "/original/")
				.setImage60UrlPrefix(this.config.imageUrlPrefixBase() + "/thumb60/")
				.setImage120UrlPrefix(this.config.imageUrlPrefixBase() + "/thumb120/")
				.setImage240UrlPrefix(this.config.imageUrlPrefixBase() + "/thumb240/")
				.setImage480UrlPrefix(this.config.imageUrlPrefixBase() + "/thumb480/")
				.build());
	}
	
	@AsyncImpl
	@Override
	public ListenableFuture<GetUploadUrlPrefixResponse> getUploadUrlPrefix(BossHead head, EmptyRequest request) {
		return this.getUploadUrlPrefixResponse;
	}
	
	@AsyncImpl
	@Override
	public ListenableFuture<GetUploadUrlPrefixResponse> getUploadUrlPrefix(AdminHead head, EmptyRequest request) {
		return this.getUploadUrlPrefixResponse;
	}
	
	@AsyncImpl
	@Override
	public ListenableFuture<GetUploadUrlPrefixResponse> getUploadUrlPrefix(AnonymousHead head, EmptyRequest request) {
		return this.getUploadUrlPrefixResponse;
	}

	@AsyncImpl
	@Override
	public ListenableFuture<GetUploadUrlPrefixResponse> getUploadUrlPrefix(RequestHead head, EmptyRequest request) {
		return this.getUploadUrlPrefixResponse;
	}
	
	@Override
	public ListenableFuture<UploadImageResponse> uploadImage(BossHead head, UploadImageRequest request) {
		return Futures.immediateFuture(this.doUploadImage(null, null, null, request));
	}
	
	@Override
	public ListenableFuture<UploadImageResponse> uploadImage(AdminHead head, UploadImageRequest request) {
		return Futures.immediateFuture(this.doUploadImage(head.hasCompanyId() ? head.getCompanyId() : null, head.getSession().getAdminId(), null, request));
	}
	
	@Override
	public ListenableFuture<UploadImageResponse> uploadImage(RequestHead head, UploadImageRequest request) {
		return Futures.immediateFuture(this.doUploadImage(head.getSession().getCompanyId(), null, head.getSession().getUserId(), request));
	}

	@Override
	public ListenableFuture<UploadImageResponse> uploadImage(AnonymousHead head, UploadImageRequest request) {
		return Futures.immediateFuture(this.doUploadImage(null, null, null, request));
	}

	private UploadImageResponse doUploadImage(@Nullable Long companyId, @Nullable Long adminId, @Nullable Long userId, UploadImageRequest request) {
		final ByteString imageData = request.getImageData();
		if (imageData.isEmpty()) {
			return UploadImageResponse.newBuilder()
					.setResult(UploadImageResponse.Result.FAIL_IMAGE_INVALID)
					.setFailText("image data is empty")
					.build();
		}
		if (imageData.size() > 10 * 1024 * 1024) {
			return UploadImageResponse.newBuilder()
					.setResult(UploadImageResponse.Result.FAIL_IMAGE_INVALID)
					.setFailText("image data is too big")
					.build();
		}
		
		if (request.getTagList().size() > 10) {
			return UploadImageResponse.newBuilder()
					.setResult(UploadImageResponse.Result.FAIL_TAG_INVALID)
					.setFailText("tag list size is too big")
					.build();
		}
		final List<String> tagList = new ArrayList<String>(request.getTagList().size());
		for (String tag : request.getTagList()) {
			tag = tag.trim();
			if (!tag.isEmpty() && tag.length() <= 20 && !tagList.contains(tag)) {
				tagList.add(tag);
			}
		}
		
		final String md5 = hashMd5(imageData);
		final File tmpFile = new File(this.config.imageTmpDir(), md5 + ".tmp");
		
		writeDataToFile(imageData, tmpFile);
		
		UploadProtos.Image image = this.getImageInfo(tmpFile, md5, imageData.size());
		if (image == null) {
			return UploadImageResponse.newBuilder()
					.setResult(UploadImageResponse.Result.FAIL_IMAGE_INVALID)
					.setFailText("image data format is invalid")
					.build();
		}
		
		this.resizeAndSaveImage(tmpFile, new int[]{60, 120, 240, 480}, image);
		
		tmpFile.delete();
		
		UploadProtos.UploadImageAction.Builder uploadActionBuilder = UploadProtos.UploadImageAction.newBuilder();	
		uploadActionBuilder.setActionId(-1L);
		uploadActionBuilder.setImageName(image.getName());
		uploadActionBuilder.setUploadTime((int) (System.currentTimeMillis() / 1000L));
		if (adminId != null) {
			uploadActionBuilder.setUploadAdminId(adminId);
		}
		if (userId != null) {
			uploadActionBuilder.setUploadUserId(userId);
		}
		
		final UploadProtos.UploadImageAction uploadAction = uploadActionBuilder.build();
		
		Map<String, UploadProtos.Image> newImageMap;
		Map<String, List<String>> newImageTagListMap;
		
		Connection dbConn = null;
		try {
			dbConn = hikariDataSource.getConnection();
			UploadDB.replaceImage(dbConn, Collections.singletonList(image));
			if (companyId != null) {
				UploadDB.insertImageTag(dbConn, companyId, Collections.singletonMap(image.getName(), tagList));
				UploadDB.insertUploadImageAction(dbConn, companyId, Collections.singletonList(uploadAction));
			}
			
			newImageMap = UploadDB.getImage(dbConn, Collections.singleton(image.getName()));
			if (companyId != null) {
				newImageTagListMap = UploadDB.getImageTag(dbConn, companyId, Collections.singleton(image.getName()));
			} else {
				newImageTagListMap = null;
			}
		} catch (SQLException e) {
			throw new RuntimeException("db fail", e);
		} finally {
			DBUtil.closeQuietly(dbConn);
		}
		
		Jedis jedis = this.jedisPool.getResource();
		try {
			UploadCache.setImage(jedis, Collections.singleton(image.getName()), newImageMap);
			if (companyId != null && newImageTagListMap != null) {
				UploadCache.setImageTagList(jedis, companyId, Collections.singleton(image.getName()), newImageTagListMap);
			}
		} finally {
			jedis.close();
		}
		
		return UploadImageResponse.newBuilder()
				.setResult(UploadImageResponse.Result.SUCC)
				.setImage(image)
				.build();
	}
	
	private UploadProtos.Image getImageInfo(File imageFile, String md5, int size) {
		IMOperation op = new IMOperation();
		op.ping();
		op.format("%m\n%w\n%h\n");
		op.addImage(imageFile.getAbsolutePath());
		
		IdentifyCmd identifyCmd = new IdentifyCmd();
		identifyCmd.setSearchPath(this.config.imageMagickSearchPath());
		
		ArrayListOutputConsumer output=new ArrayListOutputConsumer();
		identifyCmd.setOutputConsumer(output);
		try {
			identifyCmd.run(op);
		} catch (IOException e) {
			throw new RuntimeException(e);
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		} catch (IM4JavaException e) {
			throw new RuntimeException(e);
		}
		ArrayList<String> cmdOutput = output.getOutput();
		if (cmdOutput.size() < 3) {
			return null;
		}
		
		UploadProtos.Image.Builder builder = UploadProtos.Image.newBuilder();
		
		String format = cmdOutput.get(0).trim();
		if ("JPEG".equals(format)) {
			builder.setType("jpg");
		} else if ("PNG".equals(format)){
			builder.setType("png");
		} else if ("GIF".equals(format)){
			builder.setType("gif");
		} else {
			return null;
		}
		
		try {
			builder.setWidth(Integer.parseInt(cmdOutput.get(1)));
			builder.setHight(Integer.parseInt(cmdOutput.get(2)));
		} catch (NumberFormatException e) {
			return null;
		}
		builder.setName(md5 + "." + builder.getType());
		builder.setSize(size);
		builder.setMd5(md5);
		return builder.build();
	}
	
	private void resizeAndSaveImage(File tmpImageFile, int[] thumbSizes, UploadProtos.Image image) {
		// 为了防止单个文件夹下的文件过多，加一个二级目录
		final String localSaveFolder = image.getName().substring(0, 2);
		
		// 1. resize to local save dir
		try {
			File originalFile = new File(this.config.imageLocalSaveDir().getAbsolutePath() + "/original/" + localSaveFolder + "/"+ image.getName());
			Files.createParentDirs(originalFile);
			Files.copy(tmpImageFile, originalFile);
			
			for (int thumbSize : thumbSizes) {
				File thumbFile = new File(this.config.imageLocalSaveDir().getAbsolutePath() + "/thumb" + thumbSize + "/" + localSaveFolder + "/"+ image.getName());
				Files.createParentDirs(thumbFile);
				
				IMOperation op = new IMOperation();  
		        op.addImage(tmpImageFile.getAbsolutePath());  
		        op.resize(thumbSize, thumbSize, '>');
		        op.addImage(thumbFile.getAbsolutePath());
		        
		        ConvertCmd convertCmd = new ConvertCmd();  
		        convertCmd.setSearchPath(this.config.imageMagickSearchPath());  
		        convertCmd.run(op);
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		} catch (IM4JavaException e) {
			throw new RuntimeException(e);
		}
		
		// 2. upload to qiniu cdn
		if (this.config.imageQiniuEnable()) {
			try {
				String originalKey = this.config.imageQiniuKeyPrefix() + "original/" + image.getName();
				String originalToken = this.imageQiniuAuth.uploadToken(this.config.imageQiniuBucketName(), originalKey);
				
				File originalFile = new File(this.config.imageLocalSaveDir().getAbsolutePath() + "/original/" + localSaveFolder + "/"+ image.getName());
				Response qiniuResponse = this.qiniuUploadManager.put(originalFile, originalKey, originalToken);
				if (!qiniuResponse.isOK()) {
					throw new RuntimeException("upload qiniu fail : " + qiniuResponse.toString());
				}
				
				for (int thumbSize : thumbSizes) {
					String thumbKey = this.config.imageQiniuKeyPrefix() + "thumb" + thumbSize + "/" + image.getName();
					String thumbToken = this.imageQiniuAuth.uploadToken(this.config.imageQiniuBucketName(), thumbKey);
					
					File thumbFile = new File(this.config.imageLocalSaveDir().getAbsolutePath() + "/thumb" + thumbSize + "/" + localSaveFolder + "/"+ image.getName());
					qiniuResponse = this.qiniuUploadManager.put(thumbFile, thumbKey, thumbToken);
					if (!qiniuResponse.isOK()) {
						throw new RuntimeException("upload qiniu fail : " + qiniuResponse.toString());
					}
				}
			} catch (QiniuException e) {
				throw new RuntimeException(e);
			}
		}
	}
	
	@Override
	public ListenableFuture<UploadVideoResponse> uploadVideo(BossHead head, UploadVideoRequest request) {
		return Futures.immediateFuture(this.doUploadVideo(null, null, null, request));
	}
	
	@Override
	public ListenableFuture<UploadVideoResponse> uploadVideo(AdminHead head, UploadVideoRequest request) {
		return Futures.immediateFuture(this.doUploadVideo(head.hasCompanyId() ? head.getCompanyId() : null, head.getSession().getAdminId(), null, request));
	}
	
	@Override
	public ListenableFuture<UploadVideoResponse> uploadVideo(RequestHead head, UploadVideoRequest request) {
		return Futures.immediateFuture(this.doUploadVideo(head.getSession().getCompanyId(), null, head.getSession().getUserId(), request));
	}
	
	private UploadVideoResponse doUploadVideo(@Nullable Long companyId, @Nullable Long adminId, @Nullable Long userId, UploadVideoRequest request) {
		final ByteString videoData = request.getVideoData();
		if (videoData.isEmpty()) {
			return UploadVideoResponse.newBuilder()
					.setResult(UploadVideoResponse.Result.FAIL_VIDEO_INVALID)
					.setFailText("video data is empty")
					.build();
		}
		if (videoData.size() > 10 * 1024 * 1024) {
			return UploadVideoResponse.newBuilder()
					.setResult(UploadVideoResponse.Result.FAIL_VIDEO_INVALID)
					.setFailText("video data is too large")
					.build();
		}
		if (request.getTagList().size() > 10) {
			return UploadVideoResponse.newBuilder()
					.setResult(UploadVideoResponse.Result.FAIL_TAG_INVALID)
					.setFailText("tag list size is too big")
					.build();
		}
		final List<String> tagList = new ArrayList<String>(request.getTagList().size());
		for (String tag : request.getTagList()) {
			tag = tag.trim();
			if (!tag.isEmpty() && tag.length() <= 20 && !tagList.contains(tag)) {
				tagList.add(tag);
			}
		}
		
		if (!tagList.contains("视频截图")) {
			tagList.add("视频截图");
		}
		
		final String videoMd5 = hashMd5(videoData);
		final File videoTmpFile = new File(this.config.videoTmpDir(), videoMd5 + ".tmp");
		
		writeDataToFile(videoData, videoTmpFile);
		
		UploadProtos.Video video = this.getVideoInfo(videoTmpFile, videoMd5, videoData.size());
		if (video == null) {
			return UploadVideoResponse.newBuilder()
					.setResult(UploadVideoResponse.Result.FAIL_VIDEO_INVALID)
					.setFailText("video format is invalid")
					.build();
		}
		
		final File imageTmpFile = this.createVideoShotImage(videoTmpFile);
		UploadProtos.Image image = this.getImageInfo(imageTmpFile, hashMd5(imageTmpFile), (int)imageTmpFile.length());
		if (image == null) {
			return UploadVideoResponse.newBuilder()
					.setResult(UploadVideoResponse.Result.FAIL_VIDEO_INVALID)
					.setFailText("video shot image error")
					.build();
		}
		
		this.saveVideo(videoTmpFile, video);
		videoTmpFile.delete();
		
		this.resizeAndSaveImage(imageTmpFile, new int[]{60, 120, 240, 480}, image);
		imageTmpFile.delete();
		
		UploadProtos.UploadImageAction.Builder uploadActionBuilder = UploadProtos.UploadImageAction.newBuilder();	
		uploadActionBuilder.setActionId(-1L);
		uploadActionBuilder.setImageName(image.getName());
		uploadActionBuilder.setUploadTime((int) (System.currentTimeMillis() / 1000L));
		if (adminId != null) {
			uploadActionBuilder.setUploadAdminId(adminId);
		}
		if (userId != null) {
			uploadActionBuilder.setUploadUserId(userId);
		}
		
		final UploadProtos.UploadImageAction uploadAction = uploadActionBuilder.build();
		
		Map<String, UploadProtos.Image> newImageMap;
		Map<String, List<String>> newImageTagListMap;
		
		Connection dbConn = null;
		try {
			dbConn = hikariDataSource.getConnection();
			UploadDB.replaceImage(dbConn, Collections.singletonList(image));
			if (companyId != null) {
				UploadDB.insertImageTag(dbConn, companyId, Collections.singletonMap(image.getName(), tagList));
				UploadDB.insertUploadImageAction(dbConn, companyId, Collections.singletonList(uploadAction));
			}
			
			newImageMap = UploadDB.getImage(dbConn, Collections.singleton(image.getName()));
			if (companyId != null) {
				newImageTagListMap = UploadDB.getImageTag(dbConn, companyId, Collections.singleton(image.getName()));
			} else {
				newImageTagListMap = null;
			}
		} catch (SQLException e) {
			throw new RuntimeException("db fail", e);
		} finally {
			DBUtil.closeQuietly(dbConn);
		}
		
		Jedis jedis = this.jedisPool.getResource();
		try {
			UploadCache.setImage(jedis, Collections.singleton(image.getName()), newImageMap);
			if (companyId != null && newImageTagListMap != null) {
				UploadCache.setImageTagList(jedis, companyId, Collections.singleton(image.getName()), newImageTagListMap);
			}
		} finally {
			jedis.close();
		}
		
		return UploadVideoResponse.newBuilder()
				.setResult(UploadVideoResponse.Result.SUCC)
				.setVideo(video.toBuilder().setImage(image).build())
				.build();
	}
	
	private UploadProtos.Video getVideoInfo(File videoFile, String md5, int size) {
		final String videoFormat;
		final Double videoTime;
		try {
			Process process = new ProcessBuilder(
					this.config.ffmpegSearchPath() + "/ffprobe", "-v", "quiet", "-print_format", "json", "-show_format", 
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
		} else {
			return null;
		}
		final int time = (int) Math.round(videoTime);
		
		return UploadProtos.Video.newBuilder()
				.setName(md5 + "." + type)
				.setType(type)
				.setSize(size)
				.setTime(time)
				.setMd5(md5)
				.buildPartial();
	}
	
	private File createVideoShotImage(File videoFile) {
		String videoAbsolutePath = videoFile.getAbsolutePath();
		String imageAbsolutePath = videoAbsolutePath + ".jpg";
		try {
			Process process = new ProcessBuilder(
					this.config.ffmpegSearchPath() + "/ffmpeg", "-ss", "0", "-i", videoAbsolutePath, "-vframes", "1", "-f", "image2", "-y", imageAbsolutePath).start();
			process.waitFor();
		} catch (IOException e) {
			return null;
		} catch (InterruptedException e) {
			return null;
		}
		
		File imageFile = new File(imageAbsolutePath);
		if (imageFile.exists() && imageFile.isFile()) {
			return imageFile;
		} else {
			return null;
		}
	}
	
	private void saveVideo(File tmpVideoFile, UploadProtos.Video video) {
		// 为了防止单个文件夹下的文件过多，加一个二级目录
		final String localSaveFolder = video.getName().substring(0, 2);
		
		File videoFile = new File(this.config.videoLocalSaveDir().getAbsolutePath() + "/" + localSaveFolder + "/" +  video.getName());
		try {
			Files.createParentDirs(videoFile);
			Files.copy(tmpVideoFile, videoFile);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		
		// upload to qiniu cdn
		if (this.config.videoQiniuEnable()) {
			try {
				String key = this.config.videoQiniuKeyPrefix() + video.getName();
				String token = this.videoQiniuAuth.uploadToken(this.config.videoQiniuBucketName(), key);
				
				Response qiniuResponse = this.qiniuUploadManager.put(videoFile, key, token);
				if (!qiniuResponse.isOK()) {
					throw new RuntimeException("upload qiniu fail : " + qiniuResponse.toString());
				}
			} catch (QiniuException e) {
				throw new RuntimeException(e);
			}
		}
	}
	
	private static String hashMd5(ByteString data) {
		try {
			Hasher hasher = Hashing.md5().newHasher();
			data.writeTo(Funnels.asOutputStream(hasher));
			return hasher.hash().toString();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
	
	private static String hashMd5(File file) {
		try {
			return Files.hash(file, Hashing.md5()).toString();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
	
	private static void writeDataToFile(ByteString data, File file) {
		FileOutputStream out;
		try {
			out = new FileOutputStream(file);
		} catch (FileNotFoundException e) {
			throw new RuntimeException(e);
		}
		
		try {
			data.writeTo(out);
		} catch (IOException e) {
			throw new RuntimeException(e);
		} finally {
			if (out != null) {
				try {
					out.close();
				} catch (IOException e) {
					// ignore
				}
			}
		}
	}
	
	@Override
	public ListenableFuture<GetQiniuUploadImageTokenResponse> getQiniuUploadImageToken(BossHead head, GetQiniuUploadImageTokenRequest request) {
		return Futures.immediateFuture(this.doGetQiniuUploadImageToken(request));
	}

	@Override
	public ListenableFuture<GetQiniuUploadImageTokenResponse> getQiniuUploadImageToken(AdminHead head, GetQiniuUploadImageTokenRequest request) {
		return Futures.immediateFuture(this.doGetQiniuUploadImageToken(request));
	}

	@Override
	public ListenableFuture<GetQiniuUploadImageTokenResponse> getQiniuUploadImageToken(RequestHead head, GetQiniuUploadImageTokenRequest request) {
		return Futures.immediateFuture(this.doGetQiniuUploadImageToken(request));
	}
	
	@Override
	public ListenableFuture<GetQiniuUploadImageTokenResponse> getQiniuUploadImageToken(SystemHead head, GetQiniuUploadImageTokenRequest request) {
		return Futures.immediateFuture(this.doGetQiniuUploadImageToken(request));
	}
	
	private GetQiniuUploadImageTokenResponse doGetQiniuUploadImageToken(GetQiniuUploadImageTokenRequest request) {
		final String imageName = request.getImageName().trim();
		if (imageName.isEmpty()) {
			return GetQiniuUploadImageTokenResponse.newBuilder()
					.setResult(GetQiniuUploadImageTokenResponse.Result.FAIL_IMAGE_NAME_INVALID)
					.setFailText("image name is empty")
					.build();
		}
		
		if (!this.config.imageQiniuEnable()) {
			return GetQiniuUploadImageTokenResponse.newBuilder()
					.setResult(GetQiniuUploadImageTokenResponse.Result.FAIL_QINIU_DISABLE)
					.setFailText("qiniu cdn is disable")
					.build();
		}
		
		final String key = this.config.imageQiniuKeyPrefix() + "original/" + imageName;
		final String key60 = this.config.imageQiniuKeyPrefix() + "thumb60/" + imageName;
		final String key120 = this.config.imageQiniuKeyPrefix() + "thumb120/" + imageName;
		final String key240 = this.config.imageQiniuKeyPrefix() + "thumb240/" + imageName;
		final String key480 = this.config.imageQiniuKeyPrefix() + "thumb480/" + imageName;
		
		return GetQiniuUploadImageTokenResponse.newBuilder()
				.setResult(GetQiniuUploadImageTokenResponse.Result.SUCC)
				.setUploadImageKey(key)
				.setUploadImageToken(this.imageQiniuAuth.uploadToken(this.config.imageQiniuBucketName(), key))
				.setUploadImage60Key(key60)
				.setUploadImage60Token(this.imageQiniuAuth.uploadToken(this.config.imageQiniuBucketName(), key60))
				.setUploadImage120Key(key120)
				.setUploadImage120Token(this.imageQiniuAuth.uploadToken(this.config.imageQiniuBucketName(), key120))
				.setUploadImage240Key(key240)
				.setUploadImage240Token(this.imageQiniuAuth.uploadToken(this.config.imageQiniuBucketName(), key240))
				.setUploadImage480Key(key480)
				.setUploadImage480Token(this.imageQiniuAuth.uploadToken(this.config.imageQiniuBucketName(), key480))
				.build();
	}
	
	@Override
	public ListenableFuture<SaveUploadImageActionResponse> saveUploadImageAction(BossHead head, SaveUploadImageActionRequest request) {
		this.doSaveUploadImageAction(head.hasCompanyId() ? head.getCompanyId() : null, null, null, request);
		return this.saveUploadImageActionResponse;
	}

	@Override
	public ListenableFuture<SaveUploadImageActionResponse> saveUploadImageAction(AdminHead head, SaveUploadImageActionRequest request) {
		this.doSaveUploadImageAction(head.hasCompanyId() ? head.getCompanyId() : null, head.getSession().getAdminId(), null, request);
		return this.saveUploadImageActionResponse;
	}

	@Override
	public ListenableFuture<SaveUploadImageActionResponse> saveUploadImageAction(RequestHead head, SaveUploadImageActionRequest request) {
		this.doSaveUploadImageAction(head.getSession().getCompanyId(), null, head.getSession().getUserId(), request);
		return this.saveUploadImageActionResponse;
	}
	
	@Override
	public ListenableFuture<SaveUploadImageActionResponse> saveUploadImageAction(SystemHead head, SaveUploadImageActionRequest request) {
		this.doSaveUploadImageAction(head.hasCompanyId() ? head.getCompanyId() : null, null, null, request);
		return this.saveUploadImageActionResponse;
	}
	
	private void doSaveUploadImageAction(@Nullable Long companyId, @Nullable Long adminId, @Nullable Long userId, SaveUploadImageActionRequest request) {
		if (request.getImageCount() <= 0) {
			return;
		}
		
		List<UploadProtos.UploadImageAction> actionList = new ArrayList<UploadProtos.UploadImageAction>(request.getImageCount());
		Map<String, List<String>> tagListMap = new TreeMap<String, List<String>>();
		UploadProtos.UploadImageAction.Builder tmpBuilder = UploadProtos.UploadImageAction.newBuilder();
		for (UploadProtos.Image image : request.getImageList()) {
			tmpBuilder.clear();
			
			tmpBuilder.setActionId(-1L);
			tmpBuilder.setImageName(image.getName());
			tmpBuilder.setUploadTime((int) (System.currentTimeMillis() / 1000L));
			if (adminId != null) {
				tmpBuilder.setUploadAdminId(adminId);
			}
			if (userId != null) {
				tmpBuilder.setUploadUserId(userId);
			}
			actionList.add(tmpBuilder.build());
			
			tagListMap.put(image.getName(), image.getTagList());
		}
		
		Map<String, UploadProtos.Image> newImageMap;
		Map<String, List<String>> newImageTagListMap;
		
		Connection dbConn = null;
		try {
			dbConn = hikariDataSource.getConnection();
			UploadDB.replaceImage(dbConn, request.getImageList());
			if (companyId != null) {
				UploadDB.insertImageTag(dbConn, companyId, tagListMap);
				UploadDB.insertUploadImageAction(dbConn, companyId, actionList);
			}
			
			newImageMap = UploadDB.getImage(dbConn, tagListMap.keySet());
			if (companyId != null) {
				newImageTagListMap = UploadDB.getImageTag(dbConn, companyId, tagListMap.keySet());
			} else {
				newImageTagListMap = null;
			}
		} catch (SQLException e) {
			throw new RuntimeException("db fail", e);
		} finally {
			DBUtil.closeQuietly(dbConn);
		}
		
		Jedis jedis = this.jedisPool.getResource();
		try {
			UploadCache.setImage(jedis, tagListMap.keySet(), newImageMap);
			if (companyId != null && newImageTagListMap != null) {
				UploadCache.setImageTagList(jedis, companyId, newImageTagListMap);
			}
		} finally {
			jedis.close();
		}
	}
	
}
