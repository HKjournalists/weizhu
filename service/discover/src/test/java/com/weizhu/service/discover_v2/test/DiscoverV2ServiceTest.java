package com.weizhu.service.discover_v2.test;

import static org.junit.Assert.assertTrue;

import org.junit.BeforeClass;
import org.junit.Test;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.weizhu.common.module.FakeExternalServiceModule;
import com.weizhu.common.module.FakeProfileServiceModule;
import com.weizhu.common.module.FakePushServiceModule;
import com.weizhu.common.module.TestModule;
import com.weizhu.common.utils.TestUtil;
import com.weizhu.proto.WeizhuProtos.RequestHead;
import com.weizhu.proto.DiscoverV2Protos.CommentItemRequest;
import com.weizhu.proto.DiscoverV2Protos.CommentItemResponse;
import com.weizhu.proto.DiscoverV2Protos.DeleteCommentRequest;
import com.weizhu.proto.DiscoverV2Protos.DeleteCommentResponse;
import com.weizhu.proto.DiscoverV2Protos.GetDiscoverHomeRequest;
import com.weizhu.proto.DiscoverV2Protos.GetDiscoverHomeResponse;
import com.weizhu.proto.DiscoverV2Protos.GetItemByIdRequest;
import com.weizhu.proto.DiscoverV2Protos.GetItemByIdResponse;
import com.weizhu.proto.DiscoverV2Protos.GetItemCommentListRequest;
import com.weizhu.proto.DiscoverV2Protos.GetItemCommentListResponse;
import com.weizhu.proto.DiscoverV2Protos.GetItemLearnListRequest;
import com.weizhu.proto.DiscoverV2Protos.GetItemLearnListResponse;
import com.weizhu.proto.DiscoverV2Protos.GetItemLikeListRequest;
import com.weizhu.proto.DiscoverV2Protos.GetItemLikeListResponse;
import com.weizhu.proto.DiscoverV2Protos.GetItemScoreListRequest;
import com.weizhu.proto.DiscoverV2Protos.GetItemScoreListResponse;
import com.weizhu.proto.DiscoverV2Protos.GetItemShareListRequest;
import com.weizhu.proto.DiscoverV2Protos.GetItemShareListResponse;
import com.weizhu.proto.DiscoverV2Protos.GetModuleCategoryItemListRequest;
import com.weizhu.proto.DiscoverV2Protos.GetModuleCategoryItemListResponse;
import com.weizhu.proto.DiscoverV2Protos.GetUserCommentListRequest;
import com.weizhu.proto.DiscoverV2Protos.GetUserCommentListResponse;
import com.weizhu.proto.DiscoverV2Protos.GetUserDiscoverRequest;
import com.weizhu.proto.DiscoverV2Protos.GetUserDiscoverResponse;
import com.weizhu.proto.DiscoverV2Protos.GetUserLearnListRequest;
import com.weizhu.proto.DiscoverV2Protos.GetUserLearnListResponse;
import com.weizhu.proto.DiscoverV2Protos.GetUserLikeListRequest;
import com.weizhu.proto.DiscoverV2Protos.GetUserLikeListResponse;
import com.weizhu.proto.DiscoverV2Protos.GetUserScoreListRequest;
import com.weizhu.proto.DiscoverV2Protos.GetUserScoreListResponse;
import com.weizhu.proto.DiscoverV2Protos.GetUserShareListRequest;
import com.weizhu.proto.DiscoverV2Protos.GetUserShareListResponse;
import com.weizhu.proto.DiscoverV2Protos.LearnItemRequest;
import com.weizhu.proto.DiscoverV2Protos.LearnItemResponse;
import com.weizhu.proto.DiscoverV2Protos.LikeItemRequest;
import com.weizhu.proto.DiscoverV2Protos.LikeItemResponse;
import com.weizhu.proto.DiscoverV2Protos.ScoreItemRequest;
import com.weizhu.proto.DiscoverV2Protos.ScoreItemResponse;
import com.weizhu.proto.DiscoverV2Protos.SearchItemRequest;
import com.weizhu.proto.DiscoverV2Protos.SearchItemResponse;
import com.weizhu.proto.DiscoverV2Protos.ShareItemRequest;
import com.weizhu.proto.DiscoverV2Protos.ShareItemResponse;
import com.weizhu.proto.DiscoverV2Service;
import com.weizhu.service.allow.AllowServiceModule;
import com.weizhu.service.discover.DiscoverServiceModule;
import com.weizhu.service.exam.ExamServiceModule;
import com.weizhu.service.exam.test.ExamServiceTestModule;
import com.weizhu.service.official.OfficialServiceModule;
import com.weizhu.service.offline_training.OfflineTrainingServiceModule;
import com.weizhu.service.offline_training.test.OfflineTrainingServiceTestModule;
import com.weizhu.service.survey.SurveyServiceModule;
import com.weizhu.service.survey.test.SurveyServiceTestModule;
import com.weizhu.service.user.UserServiceModule;
import com.weizhu.service.user.test.UserServiceTestModule;

public class DiscoverV2ServiceTest {
	
	static {
		System.setProperty("logback.configurationFile", "com/weizhu/service/discover_v2/test/logback.xml");
	}
	
	private static final Injector INJECTOR = Guice.createInjector(new TestModule(), 
			new DiscoverV2ServiceTestModule(), new DiscoverServiceModule(),
			new ExamServiceTestModule(), new ExamServiceModule(),
			new SurveyServiceTestModule(), new SurveyServiceModule(),
			new OfflineTrainingServiceTestModule(), new OfflineTrainingServiceModule(), 
			new UserServiceTestModule(), new UserServiceModule(),
			new FakePushServiceModule(), new FakeProfileServiceModule(), new FakeExternalServiceModule(),
			new AllowServiceModule(), new OfficialServiceModule());
	
	@BeforeClass
	public static void init() {
		TestUtil.clearCache(INJECTOR);
		TestUtil.loadTestDataDB(INJECTOR);
	}
	
	private final RequestHead requestHead;
	private final DiscoverV2Service discoverV2Service;
	
	public DiscoverV2ServiceTest() {
		this.requestHead = INJECTOR.getInstance(RequestHead.class);
		this.discoverV2Service = INJECTOR.getInstance(DiscoverV2Service.class);
	}
	
	@Test
	public void testGetDiscoverHome() throws Exception {
		GetDiscoverHomeResponse response = discoverV2Service.getDiscoverHome(requestHead, GetDiscoverHomeRequest.newBuilder().build()).get();
		
//		 System.out.println(com.google.protobuf.TextFormat.printToUnicodeString(response));
		 
		assertTrue(response.getBannerCount() > 0);
	}

	@Test
	public void testGetModuleCategoryItemList() throws Exception {
		GetModuleCategoryItemListResponse response = discoverV2Service.getModuleCategoryItemList(requestHead, GetModuleCategoryItemListRequest.newBuilder().setModuleId(1).setCategoryId(1).setItemSize(100).build()).get();
		assertTrue(response.getItemCount() > 0);
	}

	@Test
	public void testGetItemById() throws Exception {
		GetItemByIdResponse response = discoverV2Service.getItemById(requestHead, GetItemByIdRequest.newBuilder().addItemId(1).build()).get();
		assertTrue(response.getItemCount() > 0);
	}
	
	@Test
	public void testGetItemLearnList() throws Exception {
		GetItemLearnListResponse response = discoverV2Service.getItemLearnList(requestHead, GetItemLearnListRequest.newBuilder().setItemId(1).setSize(100).build()).get();
		assertTrue(response.getItemLearnCount() > 0);
	}

	@Test
	public void testGetUserLearnList() throws Exception {
		GetUserLearnListResponse response = discoverV2Service.getUserLearnList(requestHead, GetUserLearnListRequest.newBuilder().setUserId(Long.parseLong("10000124196")).setSize(100).build()).get();
		assertTrue(response.getItemLearnCount() > 0);
	}

	@Test
	public void testGetItemCommentList() throws Exception {
		GetItemCommentListResponse response = discoverV2Service.getItemCommentList(requestHead, GetItemCommentListRequest.newBuilder().setItemId(1).setSize(100).build()).get();
		assertTrue(response.getItemCommentCount() > 0);
	}
	
	@Test
	public void testGetUserCommentList() throws Exception {
		GetUserCommentListResponse response = discoverV2Service.getUserCommentList(requestHead, GetUserCommentListRequest.newBuilder().setUserId(Long.parseLong("10000124196")).setSize(100).build()).get();
		assertTrue(response.getItemCommentCount() > 0);
	}

	@Test
	public void testGetItemScoreList() throws Exception {
		GetItemScoreListResponse response = discoverV2Service.getItemScoreList(requestHead, GetItemScoreListRequest.newBuilder().setItemId(1).setSize(100).build()).get();
		assertTrue(response.getItemScoreCount() > 0);
	}

	@Test
	public void testGetUserScoreList() throws Exception {
		GetUserScoreListResponse response = discoverV2Service.getUserScoreList(requestHead, GetUserScoreListRequest.newBuilder().setUserId(Long.parseLong("10000124196")).setSize(100).build()).get();
		assertTrue(response.getItemScoreCount() > 0);
	}
	
	@Test
	public void testGetItemLikeList() throws Exception {
		GetItemLikeListResponse response = discoverV2Service.getItemLikeList(requestHead, GetItemLikeListRequest.newBuilder().setItemId(1).setSize(100).build()).get();
		assertTrue(response.getItemLikeCount() > 0);
	}
	
	@Test
	public void testGetUserLikeList() throws Exception {
		GetUserLikeListResponse response = discoverV2Service.getUserLikeList(requestHead, GetUserLikeListRequest.newBuilder().setUserId(Long.parseLong("10000124196")).setSize(100).build()).get();
		assertTrue(response.getItemLikeCount() > 0);
	}
	
	@Test
	public void testGetItemShareList() throws Exception {
		GetItemShareListResponse response = discoverV2Service.getItemShareList(requestHead, GetItemShareListRequest.newBuilder().setItemId(1).setSize(100).build()).get();
		assertTrue(response.getItemShareCount() > 0);
	}
	
	@Test
	public void testGetUserItemShareList() throws Exception {
		GetUserShareListResponse response = discoverV2Service.getUserShareList(requestHead, GetUserShareListRequest.newBuilder().setUserId(Long.parseLong("10000124196")).setSize(100).build()).get();
		assertTrue(response.getItemShareCount() > 0);
	}
	
	@Test
	public void testShareItem() throws Exception {
		ShareItemResponse response = discoverV2Service.shareItem(requestHead, ShareItemRequest.newBuilder().setItemId(1).build()).get();
		assertTrue(response.hasItemShareContent());
	}
	
	@Test
	public void testGetUserDiscover() throws Exception {
		GetUserDiscoverResponse response = discoverV2Service.getUserDiscover(requestHead, GetUserDiscoverRequest.newBuilder().setUserId(Long.parseLong("10000124196")).build()).get();
		assertTrue(response.getWeekShareItemCnt()>0);
	}
	
	@Test
	public void testSearchItem() throws Exception {
		SearchItemResponse response = discoverV2Service.searchItem(requestHead, SearchItemRequest.newBuilder().setKeyword("海尔").build()).get();
		assertTrue(response.getItemCount()>0);
	}
	
	@Test
	public void testLearnItem() throws Exception {
		LearnItemResponse response = discoverV2Service.learnItem(requestHead, LearnItemRequest.newBuilder().setItemId(1).setLearnDuration(10).build()).get();
		assertTrue(LearnItemResponse.Result.SUCC.equals(response.getResult()));
	}
	
	@Test
	public void testCommentItem() throws Exception {
		CommentItemResponse response = discoverV2Service.commentItem(requestHead, CommentItemRequest.newBuilder().setItemId(1).setCommentText("test").build()).get();
		assertTrue(CommentItemResponse.Result.SUCC.equals(response.getResult()));
	}
	
	@Test
	public void testDeleteComment() throws Exception {
		DeleteCommentResponse response = discoverV2Service.deleteComment(requestHead, DeleteCommentRequest.newBuilder().setItemId(1).setCommentId(4).build()).get();
		assertTrue(DeleteCommentResponse.Result.SUCC.equals(response.getResult()));
	}
	
	@Test
	public void testScoreItem() throws Exception {
		ScoreItemResponse response = discoverV2Service.scoreItem(requestHead, ScoreItemRequest.newBuilder().setItemId(5).setScore(50).build()).get();
		assertTrue(ScoreItemResponse.Result.SUCC.equals(response.getResult()));
	}
	
	@Test
	public void testLikeItem() throws Exception {
		LikeItemResponse response = discoverV2Service.likeItem(requestHead, LikeItemRequest.newBuilder().setItemId(5).setIsLike(true).build()).get();
		assertTrue(LikeItemResponse.Result.SUCC.equals(response.getResult()));
	}
}
