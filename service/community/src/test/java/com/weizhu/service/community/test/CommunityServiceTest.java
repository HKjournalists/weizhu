package com.weizhu.service.community.test;

import static org.junit.Assert.*;


import org.junit.BeforeClass;
import org.junit.Test;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.weizhu.common.module.FakeExternalServiceModule;
import com.weizhu.common.module.FakeProfileServiceModule;
import com.weizhu.common.module.FakePushServiceModule;
import com.weizhu.common.module.TestModule;
import com.weizhu.common.utils.TestUtil;
import com.weizhu.proto.CommunityProtos;
import com.weizhu.proto.CommunityProtos.GetBoardListRequest;
import com.weizhu.proto.CommunityProtos.GetBoardListResponse;
import com.weizhu.proto.CommunityProtos.GetCommunityResponse;
import com.weizhu.proto.CommunityProtos.GetPostListRequest;
import com.weizhu.proto.CommunityProtos.GetPostListResponse;
import com.weizhu.proto.CommunityProtos.GetRecommendPostResponse;
import com.weizhu.proto.CommunityService;
import com.weizhu.proto.CommunityProtos.GetCommunityRequest;
import com.weizhu.proto.WeizhuProtos.EmptyRequest;
import com.weizhu.proto.WeizhuProtos.RequestHead;
import com.weizhu.service.allow.AllowServiceModule;
import com.weizhu.service.community.CommunityServiceModule;
import com.weizhu.service.official.OfficialServiceModule;
import com.weizhu.service.user.UserServiceModule;

public class CommunityServiceTest {

	static {
		System.setProperty("logback.configurationFile", "com/weizhu/service/community/test/logback.xml");
	}
	
	private static final Injector INJECTOR = Guice.createInjector(new TestModule(),
			new CommunityServiceTestModule(),
			new CommunityServiceModule(),
			new FakePushServiceModule(),
			new FakeProfileServiceModule(),
			new FakeExternalServiceModule(),
			new AllowServiceModule(),
			new OfficialServiceModule(),
			new UserServiceModule());
	
	@BeforeClass
	public static void init() throws Exception {
		TestUtil.clearCache(INJECTOR);
		TestUtil.loadTestDataDB(INJECTOR);
	}
	
	private final RequestHead requestHead;
	private final CommunityService communityService;
	
	public CommunityServiceTest() {
		this.requestHead = INJECTOR.getInstance(RequestHead.class);
		this.communityService = INJECTOR.getInstance(CommunityService.class);
	}
	
	@Test
	public void test() {
		System.out.println(System.currentTimeMillis() / 1000L);
	}
	
	@Test
	public void testGetCommunity() throws Exception {
		
		GetCommunityRequest request = GetCommunityRequest.newBuilder()
				.addBoardLatestPostId(102)
//				.addAllBoardLatestPostId(Arrays.asList(3808,3036,3730,3799,3467,3800,3803,3804,3807))
				.build();
		
		GetCommunityResponse response = communityService.getCommunity(requestHead, request).get();
		 System.out.println(com.google.protobuf.TextFormat.printToUnicodeString(response));

		assertEquals("TestBBS", response.getCommunityName());
		assertEquals(12, response.getPostNewCount());
	}
	
	@Test
	public void testGetBoardList() throws Exception {
		
		GetBoardListRequest request = GetBoardListRequest.newBuilder()
				.addBoardLatestPostId(102)
				.build();
		
		GetBoardListResponse response = communityService.getBoardList(requestHead, request).get();
		
		assertTrue(response.getBoardCount()>0);
		assertEquals(2, response.getBoard(0).getBoardId());
		
//		 System.out.println(com.google.protobuf.TextFormat.printToUnicodeString(response));
	}
	
	@Test
	public void testGetPostListTopHot() throws Exception {
		
		GetPostListRequest request = GetPostListRequest.newBuilder()
				.setBoardId(1)
				.setListType(GetPostListRequest.ListType.TOP_HOT)
				.setSize(4)
				.build();
		
		GetPostListResponse response = communityService.getPostList(requestHead, request).get();
		
		assertEquals(true, response.getPostCount() >= 0);
		
//		System.out.println(com.google.protobuf.TextFormat.printToUnicodeString(response));
	}
	
	@Test
	public void testGetPostListCreateTime() throws Exception {
		
		GetPostListRequest request = GetPostListRequest.newBuilder()
				.setBoardId(1)
				.setListType(GetPostListRequest.ListType.CREATE_TIME)
//				.setOffsetIndex(com.google.protobuf.ByteString.copyFrom("\bp\020\347\363\227\253\005\030\247\312\220\253\005 \000(\001", "ISO-8859-1"))
				.setSize(40)
				.build();
		
		GetPostListResponse response = communityService.getPostListV2(requestHead, request).get();

		assertEquals(true, response.getPostCount() > 0);
//		System.out.println(com.google.protobuf.TextFormat.printToUnicodeString(response));
	}
	
	@Test
	public void testRecommendPost() throws Exception {
		
		@SuppressWarnings("unused")
		GetRecommendPostResponse response = communityService.getRecommendPost(requestHead, EmptyRequest.newBuilder().build()).get();
//		System.out.println(response.getPostList());
//		assertEquals(true, response.getPostCount() > 0);
	}
	
	@Test
	public void testCreateComment() throws Exception {
		
		CommunityProtos.CreateCommentRequest request = CommunityProtos.CreateCommentRequest.newBuilder()
				.setPostId(101)
				.setContent("回复")
				.build();
		
		CommunityProtos.CreateCommentResponse response = communityService.createComment(requestHead,request).get();

		assertEquals(CommunityProtos.CreateCommentResponse.Result.SUCC, response.getResult());
	}
	
	@Test
	public void testCreatePost() throws Exception {
		
		CommunityProtos.CreatePostRequest request = CommunityProtos.CreatePostRequest.newBuilder()
				.setBoardId(1)
				.setText("test txt")
				.setTitle("test title")
				.build();
		
		CommunityProtos.CreatePostResponse response = communityService.createPost(requestHead,request).get();

		assertEquals(CommunityProtos.CreatePostResponse.Result.SUCC, response.getResult());
	}
	
	@Test
	public void testLikeComment() throws Exception {
		
		CommunityProtos.LikeCommentRequest request = CommunityProtos.LikeCommentRequest.newBuilder()
				.setPostId(100)
				.setCommentId(1)
				.setIsLike(true)
				.build();
		
		CommunityProtos.LikeCommentResponse response = communityService.likeComment(requestHead,request).get();

		assertEquals(CommunityProtos.LikeCommentResponse.Result.SUCC, response.getResult());
	}
	
	@Test
	public void testGetMyComment() throws Exception {
		
		CommunityProtos.GetMyCommentListRequest request = CommunityProtos.GetMyCommentListRequest.newBuilder()
				.setSize(100)
				.build();
		
		CommunityProtos.GetMyCommentListResponse response = communityService.getMyCommentList(requestHead,request).get();

		assertEquals(true, response.getCommentCount() > 0);
	}
	
	@Test
	public void testGetMyPost() throws Exception {
		
		CommunityProtos.GetMyPostListRequest request = CommunityProtos.GetMyPostListRequest.newBuilder()
				.setSize(100)
				.build();
		
		CommunityProtos.GetMyPostListResponse response = communityService.getMyPostList(requestHead,request).get();

		assertEquals(true, response.getPostCount() > 0);
	}
	
	@Test
	public void testDeleteComment() throws Exception {
		
		CommunityProtos.DeleteCommentRequest request = CommunityProtos.DeleteCommentRequest.newBuilder()
				.setPostId(100)
				.setCommentId(2)
				.build();
		
		CommunityProtos.DeleteCommentResponse response = communityService.deleteComment(requestHead,request).get();

		assertEquals(CommunityProtos.DeleteCommentResponse.Result.SUCC, response.getResult());
	}
	
	
	@Test
	public void testLikePost() throws Exception {
		
		CommunityProtos.LikePostRequest request = CommunityProtos.LikePostRequest.newBuilder()
				.setPostId(100)
				.setIsLike(true)
				.build();
		
		CommunityProtos.LikePostResponse response = communityService.likePost(requestHead,request).get();

		assertEquals(CommunityProtos.LikePostResponse.Result.SUCC, response.getResult());
	}
	
	@Test
	public void testDeletePost() throws Exception {
		
		CommunityProtos.DeletePostRequest request = CommunityProtos.DeletePostRequest.newBuilder()
				.setPostId(101)
				.build();
		
		CommunityProtos.DeletePostResponse response = communityService.deletePost(requestHead,request).get();

		assertEquals(CommunityProtos.DeletePostResponse.Result.SUCC, response.getResult());
	}
	
	@Test
	public void testGetPostCommentById() throws Exception {
		
		CommunityProtos.GetPostCommentByIdRequest request = CommunityProtos.GetPostCommentByIdRequest.newBuilder()
				.addPostCommentId(CommunityProtos.GetPostCommentByIdRequest.PostCommentId.newBuilder().setPostId(100).setCommentId(1).build())
				.addPostCommentId(CommunityProtos.GetPostCommentByIdRequest.PostCommentId.newBuilder().setPostId(100).setCommentId(2).build())
				.build();

		CommunityProtos.GetPostCommentByIdResponse response = communityService.getPostCommentById(requestHead, request).get();
		// System.out.println(response.getCommentList());
		assertEquals(true, response.getCommentCount() > 0 && response.getRefPostCount() > 0);
	}
	
	@Test
	public void testGetHotCommentList() throws Exception {
		
		CommunityProtos.GetHotCommentListRequest request = CommunityProtos.GetHotCommentListRequest.newBuilder()
				.setPostId(100)
				.build();

		CommunityProtos.GetHotCommentListResponse response = communityService.getHotCommentList(requestHead, request).get();
//		 System.out.println(response.getCommentList());
		assertEquals(true, response.getCommentCount() > 0);
	}	
}
