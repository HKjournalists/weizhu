package com.weizhu.service.scene.test;

import static org.junit.Assert.assertEquals;

import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.weizhu.common.module.FakeExternalServiceModule;
import com.weizhu.common.module.FakeProfileServiceModule;
import com.weizhu.common.module.FakePushServiceModule;
import com.weizhu.common.module.TestModule;
import com.weizhu.common.utils.TestUtil;
import com.weizhu.proto.SceneProtos;
import com.weizhu.proto.SceneService;
import com.weizhu.proto.WeizhuProtos.EmptyRequest;
import com.weizhu.proto.WeizhuProtos.RequestHead;
import com.weizhu.service.allow.AllowServiceModule;
import com.weizhu.service.discover.DiscoverServiceModule;
import com.weizhu.service.discover_v2.test.DiscoverV2ServiceTestModule;
import com.weizhu.service.exam.ExamServiceModule;
import com.weizhu.service.exam.test.ExamServiceTestModule;
import com.weizhu.service.official.OfficialServiceModule;
import com.weizhu.service.scene.SceneServiceModule;
import com.weizhu.service.survey.SurveyServiceModule;
import com.weizhu.service.survey.test.SurveyServiceTestModule;
import com.weizhu.service.user.UserServiceModule;
import com.weizhu.service.user.test.UserServiceTestModule;

@Ignore
public class SceneServiceTest {

	static {
		System.setProperty("logback.configurationFile", "com/weizhu/service/scene/test/logback.xml");
	}

	private static final Injector INJECTOR = Guice.createInjector(new TestModule(),
			new SceneServiceTestModule(),
			new SceneServiceModule(),
			new DiscoverV2ServiceTestModule(),
			new DiscoverServiceModule(),
			new ExamServiceTestModule(),
			new ExamServiceModule(),
			new SurveyServiceTestModule(),
			new SurveyServiceModule(),
			new UserServiceTestModule(),
			new UserServiceModule(),
			new FakePushServiceModule(), 
			new FakeProfileServiceModule(),
			new FakeExternalServiceModule(),
			new AllowServiceModule(),
			new OfficialServiceModule());

	@BeforeClass
	public static void init() throws Exception {
		TestUtil.clearCache(INJECTOR);
		TestUtil.loadTestDataDB(INJECTOR);
	}

	private final RequestHead head;
	private final SceneService sceneService;

	public SceneServiceTest() {
		this.head = INJECTOR.getInstance(RequestHead.class);
		this.sceneService = INJECTOR.getInstance(SceneService.class);
	}

	@Test
	public void testGetSceneHome() throws Exception {

		SceneProtos.GetSceneHomeResponse response = sceneService.getSceneHome(head, EmptyRequest.newBuilder().build()).get();

		assertEquals(true, response.getSceneCount() > 0);
	}

	@Test
	public void testGetSceneItem() throws Exception {

		SceneProtos.GetSceneItemRequest request = SceneProtos.GetSceneItemRequest.newBuilder().setSize(4).setItemTitle("BCD").build();
		SceneProtos.GetSceneItemResponse response = sceneService.getSceneItem(head, request).get();

		//		System.out.println(TextFormat.printToUnicodeString(response));
		assertEquals(true, response.getItemCount() > 0);
	}

	/**
	 * 以下为工具盖帽神器（超值推荐）test
	 *
	 */
	@Test
	public void testGetRecommenderHome() throws Exception {

		SceneProtos.GetRecommenderHomeResponse response = sceneService.getRecommenderHome(head, EmptyRequest.newBuilder().build()).get();

		//		System.out.println(TextFormat.printToUnicodeString(response));
		assertEquals(true, response.getCategoryCount() > 0);
	}

	@Test
	public void testGetRecommenderCompetitorProduct() throws Exception {

		SceneProtos.GetRecommenderCompetitorProductResponse response = sceneService.getRecommenderCompetitorProduct(head,
				SceneProtos.GetRecommenderCompetitorProductRequest.newBuilder().setCategoryId(3).setSize(100).build()).get();

		//		System.out.println(TextFormat.printToUnicodeString(response));
		assertEquals(true, response.getCompetitorProductCount() > 0);
	}
	
	@Test
	public void testGetRecommenderRecommendProduct() throws Exception {

		SceneProtos.GetRecommenderRecommendProductResponse response = sceneService.getRecommenderRecommendProduct(head,
				SceneProtos.GetRecommenderRecommendProductRequest.newBuilder().setCompetitorProductId(1).build()).get();

//				System.out.println(TextFormat.printToUnicodeString(response));
		assertEquals(true, response.getRecommendProductCount() > 0);
	}
}
