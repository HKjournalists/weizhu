package com.weizhu.proto;

import com.google.common.util.concurrent.ListenableFuture;
import com.weizhu.proto.AdminProtos.AdminHead;
import com.weizhu.proto.BossProtos.BossHead;
import com.weizhu.proto.UploadProtos.GetQiniuUploadImageTokenRequest;
import com.weizhu.proto.UploadProtos.GetQiniuUploadImageTokenResponse;
import com.weizhu.proto.UploadProtos.GetUploadUrlPrefixResponse;
import com.weizhu.proto.UploadProtos.SaveUploadImageActionRequest;
import com.weizhu.proto.UploadProtos.SaveUploadImageActionResponse;
import com.weizhu.proto.UploadProtos.UploadImageRequest;
import com.weizhu.proto.UploadProtos.UploadImageResponse;
import com.weizhu.proto.UploadProtos.UploadVideoRequest;
import com.weizhu.proto.UploadProtos.UploadVideoResponse;
import com.weizhu.proto.WeizhuProtos.AnonymousHead;
import com.weizhu.proto.WeizhuProtos.EmptyRequest;
import com.weizhu.proto.WeizhuProtos.RequestHead;
import com.weizhu.proto.WeizhuProtos.SystemHead;

public interface UploadService {
	
	@ResponseType(GetUploadUrlPrefixResponse.class)
	ListenableFuture<GetUploadUrlPrefixResponse> getUploadUrlPrefix(BossHead head, EmptyRequest request);
	
	@ResponseType(GetUploadUrlPrefixResponse.class)
	ListenableFuture<GetUploadUrlPrefixResponse> getUploadUrlPrefix(AdminHead head, EmptyRequest request);
	
	@ResponseType(GetUploadUrlPrefixResponse.class)
	ListenableFuture<GetUploadUrlPrefixResponse> getUploadUrlPrefix(AnonymousHead head, EmptyRequest request);
	
	@ResponseType(GetUploadUrlPrefixResponse.class)
	ListenableFuture<GetUploadUrlPrefixResponse> getUploadUrlPrefix(RequestHead head, EmptyRequest request);
	
	
	@ResponseType(UploadImageResponse.class)
	ListenableFuture<UploadImageResponse> uploadImage(BossHead head, UploadImageRequest request);
	
	@ResponseType(UploadImageResponse.class)
	ListenableFuture<UploadImageResponse> uploadImage(AdminHead head, UploadImageRequest request);
	
	@ResponseType(UploadImageResponse.class)
	ListenableFuture<UploadImageResponse> uploadImage(RequestHead head, UploadImageRequest request);
	
	@ResponseType(UploadImageResponse.class)
	ListenableFuture<UploadImageResponse> uploadImage(AnonymousHead head, UploadImageRequest request);
	
	@ResponseType(UploadVideoResponse.class)
	ListenableFuture<UploadVideoResponse> uploadVideo(BossHead head, UploadVideoRequest request);
	
	@ResponseType(UploadVideoResponse.class)
	ListenableFuture<UploadVideoResponse> uploadVideo(AdminHead head, UploadVideoRequest request);
	
	@ResponseType(UploadVideoResponse.class)
	ListenableFuture<UploadVideoResponse> uploadVideo(RequestHead head, UploadVideoRequest request);
	
	// for upload webapp
	
	@ResponseType(GetQiniuUploadImageTokenResponse.class)
	ListenableFuture<GetQiniuUploadImageTokenResponse> getQiniuUploadImageToken(BossHead head, GetQiniuUploadImageTokenRequest request);
	
	@ResponseType(GetQiniuUploadImageTokenResponse.class)
	ListenableFuture<GetQiniuUploadImageTokenResponse> getQiniuUploadImageToken(AdminHead head, GetQiniuUploadImageTokenRequest request);
	
	@ResponseType(GetQiniuUploadImageTokenResponse.class)
	ListenableFuture<GetQiniuUploadImageTokenResponse> getQiniuUploadImageToken(RequestHead head, GetQiniuUploadImageTokenRequest request);
	
	@ResponseType(GetQiniuUploadImageTokenResponse.class)
	ListenableFuture<GetQiniuUploadImageTokenResponse> getQiniuUploadImageToken(SystemHead head, GetQiniuUploadImageTokenRequest request);
	
	@ResponseType(SaveUploadImageActionResponse.class)
	ListenableFuture<SaveUploadImageActionResponse> saveUploadImageAction(BossHead head, SaveUploadImageActionRequest request);
	
	@ResponseType(SaveUploadImageActionResponse.class)
	ListenableFuture<SaveUploadImageActionResponse> saveUploadImageAction(AdminHead head, SaveUploadImageActionRequest request);
	
	@ResponseType(SaveUploadImageActionResponse.class)
	ListenableFuture<SaveUploadImageActionResponse> saveUploadImageAction(RequestHead head, SaveUploadImageActionRequest request);
	
	@ResponseType(SaveUploadImageActionResponse.class)
	ListenableFuture<SaveUploadImageActionResponse> saveUploadImageAction(SystemHead head, SaveUploadImageActionRequest request);
	
}
