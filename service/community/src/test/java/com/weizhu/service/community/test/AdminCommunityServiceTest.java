package com.weizhu.service.community.test;

import static org.junit.Assert.assertEquals;

import org.junit.BeforeClass;
import org.junit.Test;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.weizhu.common.module.FakeExternalServiceModule;
import com.weizhu.common.module.FakeProfileServiceModule;
import com.weizhu.common.module.FakePushServiceModule;
import com.weizhu.common.module.TestModule;
import com.weizhu.common.utils.TestUtil;
import com.weizhu.proto.AdminCommunityProtos;
import com.weizhu.proto.AdminCommunityService;
import com.weizhu.proto.AdminProtos.AdminHead;
import com.weizhu.proto.WeizhuProtos;
import com.weizhu.service.allow.AllowServiceModule;
import com.weizhu.service.community.CommunityServiceModule;
import com.weizhu.service.official.OfficialServiceModule;
import com.weizhu.service.user.UserServiceModule;

public class AdminCommunityServiceTest {

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

	private final AdminHead adminHead;
	private final AdminCommunityService adminCommunityService;

	public AdminCommunityServiceTest() {
		this.adminHead = INJECTOR.getInstance(AdminHead.class);
		this.adminCommunityService = INJECTOR.getInstance(AdminCommunityService.class);
	}

	@Test
	public void testGetCommunity() throws Exception {

		AdminCommunityProtos.GetCommunityResponse response = adminCommunityService.getCommunity(adminHead,
				WeizhuProtos.EmptyRequest.newBuilder().build()).get();

		assertEquals("TestBBS", response.getCommunityName());
	}

	@Test
	public void testDeleteBoard() throws Exception {

		AdminCommunityProtos.DeleteBoardResponse response = adminCommunityService.deleteBoard(adminHead,
				AdminCommunityProtos.DeleteBoardRequest.newBuilder().setBoardId(3).setIsForceDelete(true).build()).get();

		assertEquals(AdminCommunityProtos.DeleteBoardResponse.Result.SUCC, response.getResult());
	}

	@Test
	public void testGetBoard() throws Exception {

		AdminCommunityProtos.GetBoardListResponse response = adminCommunityService.getBoardList(adminHead,
				AdminCommunityProtos.GetBoardListRequest.newBuilder().build()).get();
//			System.out.println(com.google.protobuf.TextFormat.printToUnicodeString(response));
		assertEquals(true, response.getBoardCount() > 0);
	}

	@Test
	public void testGetPostList() throws Exception {

		AdminCommunityProtos.GetPostListRequest request = AdminCommunityProtos.GetPostListRequest.newBuilder()
				.setBoardId(1)
				.setStart(0)
				.setLength(20)
				.build();

		AdminCommunityProtos.GetPostListResponse response = adminCommunityService.getPostList(adminHead, request).get();
//				 System.out.println(com.google.protobuf.TextFormat.printToUnicodeString(response));

		assertEquals(true, response.getPostCount() > 0);
	}

	@Test
	public void testExportPostList() throws Exception {

		AdminCommunityProtos.ExportPostListRequest request = AdminCommunityProtos.ExportPostListRequest.newBuilder().setSize(100).build();

		AdminCommunityProtos.ExportPostListResponse response = adminCommunityService.exportPostList(adminHead, request).get();
		//		 System.out.println(TextFormat.printToUnicodeString(response));

		assertEquals(true, response.getPostCount() > 0);
	}

	@Test
	public void testMigratePost() throws Exception {

		AdminCommunityProtos.MigratePostRequest request = AdminCommunityProtos.MigratePostRequest.newBuilder()
				.setBoardId(4)
				.addPostId(110)
				.addPostId(109)
				.build();

		AdminCommunityProtos.MigratePostResponse response = adminCommunityService.migratePost(adminHead, request).get();
		assertEquals(AdminCommunityProtos.MigratePostResponse.Result.SUCC, response.getResult());
	}

	@Test
	public void testGetCommentList() throws Exception {

		AdminCommunityProtos.GetCommentListRequest request = AdminCommunityProtos.GetCommentListRequest.newBuilder()
				.setPostId(100)
				.setLength(5)
				.build();

		AdminCommunityProtos.GetCommentListResponse response = adminCommunityService.getCommentList(adminHead, request).get();

		//		System.out.println(TextFormat.printToUnicodeString(response));
		assertEquals(true, response.getCommentCount() > 0);
	}

	@Test
	public void testGetRecommendedPost() throws Exception {

		AdminCommunityProtos.GetRecommendPostResponse response = adminCommunityService.getRecommendPost(adminHead,
				WeizhuProtos.EmptyRequest.newBuilder().build()).get();
		assertEquals(true, response.getPostCount() > 0);
	}

	@Test
	public void testRecommendedPost() throws Exception {

		AdminCommunityProtos.RecommendPostResponse response = adminCommunityService.recommendPost(adminHead,
				AdminCommunityProtos.RecommendPostRequest.newBuilder().setPostId(101).setIsRecommend(true).build()).get();
		assertEquals(AdminCommunityProtos.RecommendPostResponse.Result.SUCC, response.getResult());
	}

	@Test
	public void testCreateBoard() throws Exception {

		AdminCommunityProtos.CreateBoardResponse response = adminCommunityService.createBoard(adminHead,
				AdminCommunityProtos.CreateBoardRequest.newBuilder()
						.setParentBoardId(1)
						.setBoardName("name")
						.setBoardDesc("desc")
						.setBoardIcon("icon")
						.build()).get();
		assertEquals(AdminCommunityProtos.CreateBoardResponse.Result.SUCC, response.getResult());
	}
	
	@Test
	public void testCreateBoardTag() throws Exception {

		AdminCommunityProtos.CreateBoardTagResponse response = adminCommunityService
				.createBoardTag(adminHead, AdminCommunityProtos.CreateBoardTagRequest.newBuilder().setBoardId(1).addTag("考试").addTag("问答").build())
				.get();
		assertEquals(AdminCommunityProtos.CreateBoardTagResponse.Result.SUCC, response.getResult());
	}
	
	@Test
	public void testDeleteBoardTag() throws Exception {

		AdminCommunityProtos.DeleteBoardTagResponse response = adminCommunityService
				.deleteBoardTag(adminHead, AdminCommunityProtos.DeleteBoardTagRequest.newBuilder().setBoardId(1).addTag("学习").addTag("问答").build())
				.get();
		assertEquals(AdminCommunityProtos.DeleteBoardTagResponse.Result.SUCC, response.getResult());
	}
	
	@Test
	public void testGetBoardTag() throws Exception {

		AdminCommunityProtos.GetBoardTagResponse response = adminCommunityService
				.getBoardTag(adminHead, AdminCommunityProtos.GetBoardTagRequest.newBuilder().setBoardId(1).build())
				.get();
		assertEquals(true, response.getTagCount()>0);
	}
	
	
	@Test
	public void testCreateComment() throws Exception {
		
		AdminCommunityProtos.CreateCommentRequest request = AdminCommunityProtos.CreateCommentRequest.newBuilder()
				.setPostId(101)
				.setContent("回复")
				.setCreateUserId(Long.parseLong("10000124196"))
				.build();
		
		AdminCommunityProtos.CreateCommentResponse response = adminCommunityService.createComment(adminHead, request).get();

		assertEquals(AdminCommunityProtos.CreateCommentResponse.Result.SUCC, response.getResult());
	}
}
