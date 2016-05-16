package com.weizhu.service.discover_v2.test;

import static org.junit.Assert.*;

import java.util.Arrays;

import org.junit.BeforeClass;
import org.junit.Test;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.weizhu.common.module.FakeExternalServiceModule;
import com.weizhu.common.module.FakeProfileServiceModule;
import com.weizhu.common.module.FakePushServiceModule;
import com.weizhu.common.module.TestModule;
import com.weizhu.common.service.ServiceUtil;
import com.weizhu.common.utils.TestUtil;
import com.weizhu.proto.AdminDiscoverProtos.AddItemToCategoryRequest;
import com.weizhu.proto.AdminDiscoverProtos.AddItemToCategoryResponse;
import com.weizhu.proto.AdminDiscoverProtos.CreateBannerRequest;
import com.weizhu.proto.AdminDiscoverProtos.CreateBannerResponse;
import com.weizhu.proto.AdminDiscoverProtos.CreateItemRequest;
import com.weizhu.proto.AdminDiscoverProtos.CreateItemResponse;
import com.weizhu.proto.AdminDiscoverProtos.CreateModuleCategoryRequest;
import com.weizhu.proto.AdminDiscoverProtos.CreateModuleCategoryResponse;
import com.weizhu.proto.AdminDiscoverProtos.CreateModuleRequest;
import com.weizhu.proto.AdminDiscoverProtos.CreateModuleResponse;
import com.weizhu.proto.AdminDiscoverProtos.GetBannerResponse;
import com.weizhu.proto.AdminDiscoverProtos.GetItemCommentListRequest;
import com.weizhu.proto.AdminDiscoverProtos.GetItemCommentListResponse;
import com.weizhu.proto.AdminDiscoverProtos.GetItemListRequest;
import com.weizhu.proto.AdminDiscoverProtos.GetItemListResponse;
import com.weizhu.proto.AdminDiscoverProtos.GetItemShareListRequest;
import com.weizhu.proto.AdminDiscoverProtos.GetItemShareListResponse;
import com.weizhu.proto.AdminDiscoverProtos.GetModuleCategoryRequest;
import com.weizhu.proto.AdminDiscoverProtos.GetModuleCategoryResponse;
import com.weizhu.proto.AdminDiscoverProtos.GetModuleResponse;
import com.weizhu.proto.AdminDiscoverProtos.MigrateModuleCategoryRequest;
import com.weizhu.proto.AdminDiscoverProtos.MigrateModuleCategoryResponse;
import com.weizhu.proto.AdminDiscoverProtos.SetDiscoverHomeRequest;
import com.weizhu.proto.AdminDiscoverProtos.SetDiscoverHomeResponse;
import com.weizhu.proto.AdminDiscoverProtos.UpdateBannerRequest;
import com.weizhu.proto.AdminDiscoverProtos.UpdateBannerResponse;
import com.weizhu.proto.AdminDiscoverProtos.UpdateBannerStateRequest;
import com.weizhu.proto.AdminDiscoverProtos.UpdateBannerStateResponse;
import com.weizhu.proto.AdminDiscoverProtos.UpdateItemRequest;
import com.weizhu.proto.AdminDiscoverProtos.UpdateItemResponse;
import com.weizhu.proto.AdminDiscoverProtos.UpdateItemStateRequest;
import com.weizhu.proto.AdminDiscoverProtos.UpdateItemStateResponse;
import com.weizhu.proto.AdminDiscoverProtos.UpdateModuleCategoryRequest;
import com.weizhu.proto.AdminDiscoverProtos.UpdateModuleCategoryResponse;
import com.weizhu.proto.AdminDiscoverProtos.UpdateModuleCategoryStateRequest;
import com.weizhu.proto.AdminDiscoverProtos.UpdateModuleCategoryStateResponse;
import com.weizhu.proto.AdminDiscoverProtos.UpdateModuleRequest;
import com.weizhu.proto.AdminDiscoverProtos.UpdateModuleResponse;
import com.weizhu.proto.AdminDiscoverProtos.UpdateModuleStateRequest;
import com.weizhu.proto.AdminDiscoverProtos.UpdateModuleStateResponse;
import com.weizhu.proto.AdminDiscoverService;
import com.weizhu.proto.AdminProtos.AdminHead;
import com.weizhu.proto.DiscoverV2Protos.State;
import com.weizhu.proto.DiscoverV2Protos;
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

public class AdminDiscoverServiceTest {
	
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
	
	private final AdminHead adminHead;
	private final AdminDiscoverService adminDiscoverService;
	
	public AdminDiscoverServiceTest() {
		this.adminHead = INJECTOR.getInstance(AdminHead.class);
		this.adminDiscoverService = INJECTOR.getInstance(AdminDiscoverService.class);
	}
	
	@Test
	public void testSetDiscoverHome() throws Exception {
		SetDiscoverHomeRequest request = SetDiscoverHomeRequest.newBuilder()
				.addAllBannerOrderId(Arrays.asList(1,2,3))
				.addAllModuleOrderId(Arrays.asList(1,2,3))
				.build();
		
		SetDiscoverHomeResponse response = adminDiscoverService.setDiscoverHome(adminHead, request).get();
		assertEquals(SetDiscoverHomeResponse.Result.SUCC, response.getResult());
	}
	
	@Test
	public void testGeBanner() throws Exception {
		GetBannerResponse response = adminDiscoverService.getBanner(adminHead, ServiceUtil.EMPTY_REQUEST).get();
		assertTrue(response.getBannerCount() > 0);
	}
	
	@Test
	public void testCreateBanner() throws Exception {
		CreateBannerRequest request = CreateBannerRequest.newBuilder()
				.setBannerName("问答")
				.setImageName("image_name")
				.setItemId(1)
//				.setWebUrl(DiscoverV2Protos.WebUrl.newBuilder().setWebUrl("web_url").setIsWeizhu(true).build())
				.build();
		
		CreateBannerResponse response = adminDiscoverService.createBanner(adminHead, request).get();
		assertEquals(CreateBannerResponse.Result.SUCC, response.getResult());
	}
	
	@Test
	public void testUpdateBanner() throws Exception {
		UpdateBannerRequest request = UpdateBannerRequest.newBuilder()
				.setBannerId(6)
				.setBannerName("问答")
				.setImageName("image_name")
				.setItemId(1)
				.setWebUrl(DiscoverV2Protos.WebUrl.newBuilder().setWebUrl("web_url").setIsWeizhu(true).build())
				.build();
		
		UpdateBannerResponse response = adminDiscoverService.updateBanner(adminHead, request).get();
		assertEquals(UpdateBannerResponse.Result.SUCC, response.getResult());
	}
	
	@Test
	public void testUpdateBannerState() throws Exception {
		UpdateBannerStateRequest request = UpdateBannerStateRequest.newBuilder()
				.addBannerId(7)
				.setState(State.DELETE)
				.build();
		
		UpdateBannerStateResponse response = adminDiscoverService.updateBannerState(adminHead, request).get();
		assertEquals(UpdateBannerStateResponse.Result.SUCC, response.getResult());
	}
	
	@Test
	public void testGetModule() throws Exception {
		GetModuleResponse response = adminDiscoverService.getModule(adminHead, ServiceUtil.EMPTY_REQUEST).get();	
		assertTrue(response.getModuleCount() > 0);
	}
	
	@Test
	public void testCreateModule() throws Exception {
		CreateModuleRequest request = CreateModuleRequest.newBuilder()
				.setModuleName("问答")
				.setImageName("image_name")
				.setWebUrl(DiscoverV2Protos.WebUrl.newBuilder().setWebUrl("web_url").setIsWeizhu(true).build())
				.build();
		
		CreateModuleResponse response = adminDiscoverService.createModule(adminHead, request).get();
		assertEquals(CreateModuleResponse.Result.SUCC, response.getResult());
	}
	
	@Test
	public void testUpdateModule() throws Exception {
		UpdateModuleRequest request = UpdateModuleRequest.newBuilder()
				.setModuleId(6)
				.setModuleName("问答")
				.setImageName("image_name")
				.setWebUrl(DiscoverV2Protos.WebUrl.newBuilder().setWebUrl("web_url").setIsWeizhu(true).build())
				.build();
		
		UpdateModuleResponse response = adminDiscoverService.updateModule(adminHead, request).get();
		assertEquals(UpdateModuleResponse.Result.SUCC, response.getResult());
	}
	
	@Test
	public void testUpdateModuleState() throws Exception {
		UpdateModuleStateRequest request = UpdateModuleStateRequest.newBuilder()
				.addModuleId(7)
				.setState(State.DELETE)
				.build();
		
		UpdateModuleStateResponse response = adminDiscoverService.updateModuleState(adminHead, request).get();
		assertEquals(UpdateModuleStateResponse.Result.SUCC, response.getResult());
	}
	
	@Test
	public void testGetModuleCategory() throws Exception {
		GetModuleCategoryRequest request = GetModuleCategoryRequest.newBuilder()
				.setModuleId(1)
				.build();
		
		GetModuleCategoryResponse response = adminDiscoverService.getModuleCategory(adminHead, request).get();	
		assertTrue(response.getCategoryCount() > 0);
	}
	
	@Test
	public void testCreateModuleCategory() throws Exception {
		CreateModuleCategoryRequest request = CreateModuleCategoryRequest.newBuilder()
				.setModuleId(1)
				.setCategoryName("categoryTest1")
				.build();
		
		CreateModuleCategoryResponse response = adminDiscoverService.createModuleCategory(adminHead, request).get();
		assertEquals(CreateModuleCategoryResponse.Result.SUCC, response.getResult());
	}
	
	@Test
	public void testUpdateModuleCategory() throws Exception {
		UpdateModuleCategoryRequest request = UpdateModuleCategoryRequest.newBuilder()
				.setCategoryId(1)
				.setModuleId(1)
				.setCategoryName("categoryTest2")
				.build();
		
		UpdateModuleCategoryResponse response = adminDiscoverService.updateModuleCategory(adminHead, request).get();
		assertEquals(UpdateModuleCategoryResponse.Result.SUCC, response.getResult());
	}
	
	@Test
	public void testUpdateModuleCategoryState() throws Exception {
		UpdateModuleCategoryStateRequest request = UpdateModuleCategoryStateRequest.newBuilder()
				.addCategoryId(7)
				.setState(State.DELETE)
				.build();
		
		UpdateModuleCategoryStateResponse response = adminDiscoverService.updateModuleCategoryState(adminHead, request).get();
		assertEquals(UpdateModuleCategoryStateResponse.Result.SUCC, response.getResult());
	}
	
	@Test
	public void testMigrateModuleCategory() throws Exception {
		MigrateModuleCategoryRequest request = MigrateModuleCategoryRequest.newBuilder()
				.addCategoryId(1)
				.setModuleId(1)
				.build();
		
		MigrateModuleCategoryResponse response = adminDiscoverService.migrateModuleCategory(adminHead, request).get();
		assertEquals(MigrateModuleCategoryResponse.Result.SUCC, response.getResult());
	}
	
	@Test
	public void testAddItemToCategory() throws Exception {
		AddItemToCategoryRequest request = AddItemToCategoryRequest.newBuilder()
				.setCategoryId(1)
				.addItemId(1)
				.addItemId(2)
				.build();
		
		AddItemToCategoryResponse response = adminDiscoverService.addItemToCategory(adminHead, request).get();		
		assertEquals(AddItemToCategoryResponse.Result.SUCC, response.getResult());
	}
	
	@Test
	public void testGetItemList() throws Exception {
		GetItemListRequest request = GetItemListRequest.newBuilder()
				.setStart(0)
				.setLength(100)
				.setCategoryId(1)
				.setItemName("1")
				.build();
		
		GetItemListResponse response = adminDiscoverService.getItemList(adminHead, request).get();
		assertTrue(response.getItemCount() > 0);
	}
	
	@Test
	public void testCreateItem() throws Exception {
		CreateItemRequest request = CreateItemRequest.newBuilder()
				.addCategoryId(1)
				.setItemName("itemTest1")
				.setItemDesc("ItemDesc")
				.setImageName("ImageName")
				.setEnableComment(true)
				.setEnableScore(true)
				.setEnableRemind(true)
				.setEnableLike(true)
				.setEnableShare(true)
				.setEnableExternalShare(false)
				.setWebUrl(DiscoverV2Protos.WebUrl.newBuilder().setWebUrl("weburl").setIsWeizhu(true).build())
				.build();
		
		CreateItemResponse response = adminDiscoverService.createItem(adminHead, request).get();
		assertEquals(CreateItemResponse.Result.SUCC, response.getResult());
	}
	
	@Test
	public void testUpdateItem() throws Exception {
		UpdateItemRequest request = UpdateItemRequest.newBuilder()
				.addCategoryId(1)
				.addCategoryId(2)
				.setItemId(1)
				.setItemName("itemTest1")
				.setItemDesc("ItemDesc")
				.setImageName("ImageName1")
				.setEnableComment(true)
				.setEnableScore(true)
				.setEnableRemind(true)
				.setEnableLike(true)
				.setEnableShare(true)
				.setEnableExternalShare(true)
				.setWebUrl(DiscoverV2Protos.WebUrl.newBuilder().setWebUrl("weburl").setIsWeizhu(true).build())
				.build();
		
		UpdateItemResponse response = adminDiscoverService.updateItem(adminHead, request).get();
		assertEquals(UpdateItemResponse.Result.SUCC, response.getResult());
	}
	
	@Test
	public void testUpdateItemState() throws Exception {
		UpdateItemStateRequest request = UpdateItemStateRequest.newBuilder()
				.addItemId(7)
				.setState(State.DELETE)
				.build();
		
		UpdateItemStateResponse response = adminDiscoverService.updateItemState(adminHead, request).get();
		
		assertEquals(UpdateItemStateResponse.Result.SUCC, response.getResult());
	}
	
	@Test
	public void testGetItemCommentList() throws Exception {
		GetItemCommentListRequest request = GetItemCommentListRequest.newBuilder()
				.setItemId(1)
				.setStart(0)
				.setLength(100)
				.build();
		
		GetItemCommentListResponse response = adminDiscoverService.getItemCommentList(adminHead, request).get();		
		assertTrue(response.getItemCommentCount() >= 0);
	}
	
	@Test
	public void testGetItemShareList() throws Exception {
		GetItemShareListRequest request = GetItemShareListRequest.newBuilder()
				.setItemId(1)
				.setStart(0)
				.setLength(100)
				.build();
		
		GetItemShareListResponse response = adminDiscoverService.getItemShareList(adminHead, request).get();
		assertTrue(response.getItemShareCount() > 0);
	}
}
