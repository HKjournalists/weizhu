package com.weizhu.service.upload.test;

import static org.junit.Assert.assertEquals;

import org.junit.BeforeClass;
import org.junit.Test;

import com.google.common.io.Resources;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.protobuf.ByteString;
import com.weizhu.common.module.TestModule;
import com.weizhu.common.utils.TestUtil;
import com.weizhu.proto.UploadService;
import com.weizhu.proto.UploadProtos.UploadImageRequest;
import com.weizhu.proto.UploadProtos.UploadImageResponse;
import com.weizhu.proto.UploadProtos.UploadVideoRequest;
import com.weizhu.proto.UploadProtos.UploadVideoResponse;
import com.weizhu.proto.WeizhuProtos.RequestHead;
import com.weizhu.service.upload.UploadServiceModule;

public class UploadServiceTest {
	
	static {
		System.setProperty("logback.configurationFile", "com/weizhu/service/upload/test/logback.xml");
	}
	
	private static final Injector INJECTOR = Guice.createInjector(new TestModule(), 
			new UploadServiceTestModule(), new UploadServiceModule());
	
	@BeforeClass
	public static void init() throws Exception {
		TestUtil.clearCache(INJECTOR);
		TestUtil.loadTestDataDB(INJECTOR);
	}
	
	private final RequestHead requestHead;
	private final UploadService uploadService;
	
	public UploadServiceTest() {
		this.requestHead = INJECTOR.getInstance(RequestHead.class);
		this.uploadService = INJECTOR.getInstance(UploadService.class);
	}
	
	@Test
	@org.junit.Ignore
	public void testUploadImage() throws Exception {
		byte[] imageData = Resources.toByteArray(Resources.getResource("com/weizhu/service/upload/test/test.jpg"));;
		
		UploadImageRequest request = UploadImageRequest.newBuilder()
				.setImageData(ByteString.copyFrom(imageData))
				.addTag("哈哈")
				.addTag("测试")
				.build();
		UploadImageResponse repsonse = this.uploadService.uploadImage(requestHead, request).get();
		
		assertEquals(UploadImageResponse.Result.SUCC, repsonse.getResult());
	}
	
	@Test
	@org.junit.Ignore
	public void testUploadVideo() throws Exception {
		byte[] videoData = Resources.toByteArray(Resources.getResource("com/weizhu/service/upload/test/test.mp4"));;
		
		UploadVideoRequest request = UploadVideoRequest.newBuilder()
				.setVideoData(ByteString.copyFrom(videoData))
				.addTag("哈哈2")
				.addTag("测试2")
				.build();
		UploadVideoResponse repsonse = this.uploadService.uploadVideo(requestHead, request).get();
		
		assertEquals(UploadVideoResponse.Result.SUCC, repsonse.getResult());
	}
	
}
