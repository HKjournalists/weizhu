package com.weizhu.service.scene.test;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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
import com.weizhu.proto.AdminSceneProtos;
import com.weizhu.proto.AdminSceneProtos.CreateSceneItemRequest;
import com.weizhu.proto.AdminSceneProtos.DeleteRecommenderRecommendProductPriceWebUrlRequest;
import com.weizhu.proto.AdminSceneProtos.PriceWebUrlCreateCondition;
import com.weizhu.proto.AdminSceneProtos.UpdateRecommenderRecommendProductPriceWebUrlRequest;
import com.weizhu.proto.DiscoverV2Protos.WebUrl;
import com.weizhu.proto.SceneProtos;
import com.weizhu.proto.AdminProtos.AdminHead;
import com.weizhu.proto.AdminSceneService;
import com.weizhu.proto.SceneProtos.RecommenderPriceWebUrl;
import com.weizhu.proto.WeizhuProtos.EmptyRequest;
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
public class AdminSceneServiceTest {

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

	private final AdminHead adminHead;
	private final AdminSceneService adminSceneService;

	public AdminSceneServiceTest() {
		this.adminHead = INJECTOR.getInstance(AdminHead.class);
		this.adminSceneService = INJECTOR.getInstance(AdminSceneService.class);
	}

	@Test
	public void testSetSceneHome() throws Exception {

		AdminSceneProtos.SetSceneHomeResponse response = adminSceneService.setSceneHome(adminHead,
				AdminSceneProtos.SetSceneHomeRequest.newBuilder().setSceneIdOrderStr("1,2,3").build()).get();

		assertEquals(AdminSceneProtos.SetSceneHomeResponse.Result.SUCC, response.getResult());
	}

	@Test
	public void testGetSceneModule() throws Exception {

		AdminSceneProtos.GetSceneResponse response = adminSceneService.getScene(adminHead, EmptyRequest.newBuilder().build()).get();
		//		System.out.println(response.getModuleList());
		assertEquals(true, response.getSceneCount() > 0);
	}

	@Test
	public void testCreateSceneModule() throws Exception {

		AdminSceneProtos.CreateSceneRequest request = AdminSceneProtos.CreateSceneRequest.newBuilder()
				.setSceneName("场景2.2")
				.setSceneDesc("场景2.2描述")
				.setParentSceneId(3)
				.setImageName("")
				.build();
		AdminSceneProtos.CreateSceneResponse response = adminSceneService.createScene(adminHead, request).get();
		//		System.out.println(response.getModuleId());
		assertEquals(AdminSceneProtos.CreateSceneResponse.Result.SUCC, response.getResult());
	}

	@Test
	public void testUpdateSceneModule() throws Exception {

		AdminSceneProtos.UpdateSceneRequest request = AdminSceneProtos.UpdateSceneRequest.newBuilder()
				.setSceneId(3)
				.setSceneName("场景2.2")
				.setSceneDesc("场景2.2描述")
				.setParentSceneId(1)
				.setImageName("")
				.build();
		AdminSceneProtos.UpdateSceneResponse response = adminSceneService.updateScene(adminHead, request).get();

		assertEquals(AdminSceneProtos.UpdateSceneResponse.Result.SUCC, response.getResult());
	}

	@Test
	public void testUpdateSceneState() throws Exception {

		AdminSceneProtos.UpdateSceneStateRequest request = AdminSceneProtos.UpdateSceneStateRequest.newBuilder()
				.setSceneId(3)
				.setState(SceneProtos.State.DISABLE)
				.build();
		AdminSceneProtos.UpdateSceneStateResponse response = adminSceneService.updateSceneState(adminHead, request).get();

		//		System.out.println();
		assertEquals(AdminSceneProtos.UpdateSceneStateResponse.Result.SUCC, response.getResult());
	}

	@Test
	public void testCreateSceneItem() throws Exception {

		AdminSceneProtos.CreateSceneItemRequest request = AdminSceneProtos.CreateSceneItemRequest.newBuilder()
				.addCreateItemParameter(CreateSceneItemRequest.CreateItemParameter.newBuilder().setDiscoverItemId(6).setSceneId(2).build())
				.addCreateItemParameter(CreateSceneItemRequest.CreateItemParameter.newBuilder().setDiscoverItemId(7).setSceneId(2).build())
				.build();
		AdminSceneProtos.CreateSceneItemResponse response = adminSceneService.createSceneItem(adminHead, request).get();

		assertEquals(AdminSceneProtos.CreateSceneItemResponse.Result.SUCC, response.getResult());
	}

	@Test
	public void testUpdaateSceneItemState() throws Exception {

		AdminSceneProtos.UpdateSceneItemStateRequest request = AdminSceneProtos.UpdateSceneItemStateRequest.newBuilder()
				.addItemId(1)
				.addItemId(2)
				.setState(SceneProtos.State.DELETE)
				.build();
		AdminSceneProtos.UpdateSceneItemStateResponse response = adminSceneService.updateSceneItemState(adminHead, request).get();

		assertEquals(AdminSceneProtos.UpdateSceneItemStateResponse.Result.SUCC, response.getResult());
	}

	@Test
	public void testMigrateSceneItem() throws Exception {

		AdminSceneProtos.MigrateSceneItemRequest request = AdminSceneProtos.MigrateSceneItemRequest.newBuilder()
				.addItemId(1)
				.addItemId(2)
				.setSceneId(2)
				.build();
		AdminSceneProtos.MigrateSceneItemResponse response = adminSceneService.migrateSceneItem(adminHead, request).get();

		assertEquals(AdminSceneProtos.MigrateSceneItemResponse.Result.SUCC, response.getResult());
	}

	@Test
	public void testGetSceneItem() throws Exception {

		//		AdminSceneProtos.GetSceneItemRequest request = AdminSceneProtos.GetSceneItemRequest.newBuilder().setLength(100).setSceneId(2).build();
		AdminSceneProtos.GetSceneItemRequest request = AdminSceneProtos.GetSceneItemRequest.newBuilder()
				.setStart(2)
				.setLength(2)
				.setItemTitle("BCD")
				.build();

		AdminSceneProtos.GetSceneItemResponse response = adminSceneService.getSceneItem(adminHead, request).get();
		//		System.out.println(TextFormat.printToUnicodeString(response));

		assertEquals(true, response.getItemCount() > 0);
	}

	/**
	 * 以下为工具盖帽神器（超值推荐）test
	 *
	 */

	@Test
	public void testGetRecommenderHome() throws Exception {

		AdminSceneProtos.GetRecommenderHomeResponse response = adminSceneService.getRecommenderHome(adminHead, EmptyRequest.newBuilder().build())
				.get();

		assertEquals(true, response.getCategoryCount() > 0);
	}

	@Test
	public void testCreateRecommenderCategory() throws Exception {

		AdminSceneProtos.CreateRecommenderCategoryResponse response = adminSceneService.createRecommenderCategory(adminHead,
				AdminSceneProtos.CreateRecommenderCategoryRequest.newBuilder()
						.setCategoryName("category")
						.setCategoryDesc("category")
						.setImageName("")
						.setParentCategoryId(4)
						.build()).get();

		assertEquals(AdminSceneProtos.CreateRecommenderCategoryResponse.Result.SUCC, response.getResult());
	}

	@Test
	public void testUpdateRecommenderCategory() throws Exception {

		AdminSceneProtos.UpdateRecommenderCategoryResponse response = adminSceneService.updateRecommenderCategory(adminHead,
				AdminSceneProtos.UpdateRecommenderCategoryRequest.newBuilder()
						.setCategoryName("category")
						.setCategoryDesc("category")
						.setImageName("")
						.setCategoryId(4)
						.build()).get();

		assertEquals(AdminSceneProtos.UpdateRecommenderCategoryResponse.Result.SUCC, response.getResult());
	}

	@Test
	public void testUpdateRecommenderCategoryState() throws Exception {

		AdminSceneProtos.UpdateRecommenderCategoryStateResponse response = adminSceneService.updateRecommenderCategoryState(adminHead,
				AdminSceneProtos.UpdateRecommenderCategoryStateRequest.newBuilder().setCategoryId(4).setState(SceneProtos.State.DISABLE).build())
				.get();

		assertEquals(AdminSceneProtos.UpdateRecommenderCategoryStateResponse.Result.SUCC, response.getResult());
	}

	@Test
	public void testMigrateRecommenderCompetitorProduct() throws Exception {

		AdminSceneProtos.MigrateRecommenderCompetitorProductResponse response = adminSceneService.migrateRecommenderCompetitorProduct(adminHead,
				AdminSceneProtos.MigrateRecommenderCompetitorProductRequest.newBuilder()
						.setCategoryId(3)
						.addAllCompetitorProductId(Arrays.<Integer> asList(1, 2))
						.build()).get();

		assertEquals(AdminSceneProtos.MigrateRecommenderCompetitorProductResponse.Result.SUCC, response.getResult());
	}

	@Test
	public void testGetRecommenderCompetitorProduct() throws Exception {

		AdminSceneProtos.GetRecommenderCompetitorProductResponse response = adminSceneService.getRecommenderCompetitorProduct(adminHead,
				AdminSceneProtos.GetRecommenderCompetitorProductRequest.newBuilder().setCategoryId(4).setLength(100).build()).get();

		assertEquals(true, response.getCompetitorProductCount() > 0);
	}

	@Test
	public void testCreateRecommenderCompetitorProduct() throws Exception {

		AdminSceneProtos.CreateRecommenderCompetitorProductResponse response = adminSceneService.createRecommenderCompetitorProduct(adminHead,
				AdminSceneProtos.CreateRecommenderCompetitorProductRequest.newBuilder()
						.setCategoryId(3)
						.setCompetitorProductName("CompetitorProduct")
						.setImageName("")
						.addAllRecommendProductId(Arrays.<Integer> asList(1, 2))
						.build()).get();

		assertEquals(AdminSceneProtos.CreateRecommenderCompetitorProductResponse.Result.SUCC, response.getResult());
	}

	@Test
	public void testUpdateRecommenderCompetitorProduct() throws Exception {

		AdminSceneProtos.UpdateRecommenderCompetitorProductResponse response = adminSceneService.updateRecommenderCompetitorProduct(adminHead,
				AdminSceneProtos.UpdateRecommenderCompetitorProductRequest.newBuilder()
						.setCategoryId(3)
						.setCompetitorProductName("CompetitorProduct")
						.setImageName("")
						.addAllRecommendProductId(Arrays.<Integer> asList(1, 2))
						.setCompetitorProductId(1)
						.build()).get();

		assertEquals(AdminSceneProtos.UpdateRecommenderCompetitorProductResponse.Result.SUCC, response.getResult());
	}

	@Test
	public void testUpdateRecommenderCompetitorProductState() throws Exception {

		AdminSceneProtos.UpdateRecommenderCompetitorProductStateResponse response = adminSceneService.updateRecommenderCompetitorProductState(adminHead,
				AdminSceneProtos.UpdateRecommenderCompetitorProductStateRequest.newBuilder()
						.addAllCompetitorProductId(Arrays.<Integer> asList(1, 2))
						.setState(SceneProtos.State.DISABLE)
						.build())
				.get();

		assertEquals(AdminSceneProtos.UpdateRecommenderCompetitorProductStateResponse.Result.SUCC, response.getResult());
	}

	@Test
	public void testGetRecommenderRecommendProduct() throws Exception {

		AdminSceneProtos.GetRecommenderRecommendProductResponse response = adminSceneService.getRecommenderRecommendProduct(adminHead,
				AdminSceneProtos.GetRecommenderRecommendProductRequest.newBuilder()
						.setCompetitorProductId(1)
						.setRecommendProductName("推荐")
						.setLength(100)
						.build()).get();
		//				System.out.println(TextFormat.printToUnicodeString(response));

		assertEquals(true, response.getRecommendProductCount() > 0);
	}

	@Test
	public void testCreateRecommenderRecommendProduct() throws Exception {

		List<PriceWebUrlCreateCondition> priceWebUrlCreateConditions = new ArrayList<PriceWebUrlCreateCondition>();

		priceWebUrlCreateConditions.add(PriceWebUrlCreateCondition.newBuilder()
				.setImageName("image")
				.setIsWeizhu(false)
				.setUrlContent("urlContent")
				.setUrlName("name")
				.build());

		priceWebUrlCreateConditions.add(PriceWebUrlCreateCondition.newBuilder()
				.setImageName("image2")
				.setIsWeizhu(false)
				.setUrlContent("urlContent2")
				.setUrlName("name2")
				.build());
		AdminSceneProtos.CreateRecommenderRecommendProductResponse response = adminSceneService.createRecommenderRecommendProduct(adminHead,
				AdminSceneProtos.CreateRecommenderRecommendProductRequest.newBuilder()
						.setRecommendProductName("RecommendProductName")
						.setImageName("")
						.setRecommendProductDesc("RecommendProductDesc")
						.setWebUrl(WebUrl.newBuilder().setIsWeizhu(false).setWebUrl("http://www.baidu.com").build())
						.addAllPriceWebUrlCreateCondition(priceWebUrlCreateConditions)
						.build()).get();
		//				System.out.println(TextFormat.printToUnicodeString(response));

		assertEquals(AdminSceneProtos.CreateRecommenderRecommendProductResponse.Result.SUCC, response.getResult());
	}

	@Test
	public void testUpdateRecommenderRecommendProduct() throws Exception {

		AdminSceneProtos.UpdateRecommenderRecommendProductResponse response = adminSceneService.updateRecommenderRecommendProduct(adminHead,
				AdminSceneProtos.UpdateRecommenderRecommendProductRequest.newBuilder()
						.setRecommendProductId(1)
						.setRecommendProductName("RecommendProductName")
						.setImageName("")
						.setRecommendProductDesc("RecommendProductDesc")
						.setWebUrl(WebUrl.newBuilder().setIsWeizhu(false).setWebUrl("http://www.baidu.com").build())
						.build()).get();
		//				System.out.println(TextFormat.printToUnicodeString(response));

		assertEquals(AdminSceneProtos.UpdateRecommenderRecommendProductResponse.Result.SUCC, response.getResult());
	}

	@Test
	public void testUpdateRecommenderRecommendProductState() throws Exception {

		AdminSceneProtos.UpdateRecommenderRecommendProductStateResponse response = adminSceneService.updateRecommenderRecommendProductState(adminHead,
				AdminSceneProtos.UpdateRecommenderRecommendProductStateRequest.newBuilder()
						.addAllRecommendProductId(Arrays.<Integer> asList(1, 2))
						.setState(SceneProtos.State.DISABLE)
						.build())
				.get();
		//				System.out.println(TextFormat.printToUnicodeString(response));

		assertEquals(AdminSceneProtos.UpdateRecommenderRecommendProductStateResponse.Result.SUCC, response.getResult());
	}

	@Test
	public void testAddRecommendProdToCompetitorProd() throws Exception {

		AdminSceneProtos.AddRecommendProdToCompetitorProdResponse response = adminSceneService.addRecommendProdToCompetitorProd(adminHead,
				AdminSceneProtos.AddRecommendProdToCompetitorProdRequest.newBuilder()
						.addAllRecommendProductId(Arrays.<Integer> asList(1, 2))
						.setCompetitorProductId(1)
						.build()).get();
		//				System.out.println(TextFormat.printToUnicodeString(response));

		assertEquals(AdminSceneProtos.AddRecommendProdToCompetitorProdResponse.Result.SUCC, response.getResult());
	}

	@Test
	public void testDeleteRecommendProdFromCompetitorProd() throws Exception {

		AdminSceneProtos.DeleteRecommendProdFromCompetitorProdResponse response = adminSceneService.deleteRecommendProdFromCompetitorProd(adminHead,
				AdminSceneProtos.DeleteRecommendProdFromCompetitorProdRequest.newBuilder()
						.addAllRecommendProductId(Arrays.<Integer> asList(1, 2))
						.setCompetitorProductId(1)
						.build()).get();
		//				System.out.println(TextFormat.printToUnicodeString(response));

		assertEquals(AdminSceneProtos.DeleteRecommendProdFromCompetitorProdResponse.Result.SUCC, response.getResult());
	}

	@Test
	public void testGetRecommenderRecommendProductPriceUrl() throws Exception {

		AdminSceneProtos.GetRecommenderRecommendProductPriceWebUrlResponse response = adminSceneService.getRecommenderRecommendProductPriceWebUrl(adminHead,
				AdminSceneProtos.GetRecommenderRecommendProductPriceWebUrlRequest.newBuilder().setRecommendProductId(1).build())
				.get();
		//				System.out.println(TextFormat.printToUnicodeString(response));

		assertEquals(true, response.getPriceWebUrlCount() > 0);
	}

	@Test
	public void testCreateRecommenderRecommendProductPriceUrl() throws Exception {
		List<PriceWebUrlCreateCondition> priceWebUrlCreateConditions = new ArrayList<PriceWebUrlCreateCondition>();

		priceWebUrlCreateConditions.add(PriceWebUrlCreateCondition.newBuilder()
				.setImageName("image")
				.setIsWeizhu(false)
				.setUrlContent("urlContent")
				.setUrlName("name")
				.build());

		priceWebUrlCreateConditions.add(PriceWebUrlCreateCondition.newBuilder()
				.setImageName("image2")
				.setIsWeizhu(false)
				.setUrlContent("urlContent2")
				.setUrlName("name2")
				.build());
		AdminSceneProtos.CreateRecommenderRecommendProductPriceWebUrlResponse response = adminSceneService.createRecommenderRecommendProductPriceWebUrl(adminHead,
				AdminSceneProtos.CreateRecommenderRecommendProductPriceWebUrlRequest.newBuilder()
						.addAllPriceWebUrlCreateCondition(priceWebUrlCreateConditions)
						.setRecommendProductId(1)
						.build())
				.get();
		//				System.out.println(TextFormat.printToUnicodeString(response));

		assertEquals(AdminSceneProtos.CreateRecommenderRecommendProductPriceWebUrlResponse.Result.SUCC, response.getResult());
	}

	@Test
	public void testUpdateRecommenderRecommendProductPriceUrl() throws Exception {

		AdminSceneProtos.UpdateRecommenderRecommendProductPriceWebUrlResponse response = adminSceneService.updateRecommenderRecommendProductPriceWebUrl(adminHead,
				UpdateRecommenderRecommendProductPriceWebUrlRequest.newBuilder()
						.setPriceWebUrl(RecommenderPriceWebUrl.newBuilder()
								.setCreateAdminId(0)
								.setCreateTime(0)
								.setRecommendProductId(1)
								.setUrlId(1)
								.setImageName("image")
								.setIsWeizhu(false)
								.setUrlContent("urlContent")
								.setUrlName("name")
								.build())
						.build())
				.get();
		//				System.out.println(TextFormat.printToUnicodeString(response));

		assertEquals(AdminSceneProtos.UpdateRecommenderRecommendProductPriceWebUrlResponse.Result.SUCC, response.getResult());
	}

	@Test
	public void testDeleteRecommenderRecommendProductPriceUrl() throws Exception {

		AdminSceneProtos.DeleteRecommenderRecommendProductPriceWebUrlResponse response = adminSceneService.deleteRecommenderRecommendProductPriceWebUrl(adminHead,
				DeleteRecommenderRecommendProductPriceWebUrlRequest.newBuilder().addAllUrlId(Arrays.<Integer>asList(1,2)).build())
				.get();
		//				System.out.println(TextFormat.printToUnicodeString(response));

		assertEquals(AdminSceneProtos.DeleteRecommenderRecommendProductPriceWebUrlResponse.Result.SUCC, response.getResult());
	}
}
