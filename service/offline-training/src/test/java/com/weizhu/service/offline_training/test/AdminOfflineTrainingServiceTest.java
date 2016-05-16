package com.weizhu.service.offline_training.test;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.weizhu.common.module.FakeExternalServiceModule;
import com.weizhu.common.module.FakeProfileServiceModule;
import com.weizhu.common.module.FakePushServiceModule;
import com.weizhu.common.module.TestModule;
import com.weizhu.common.utils.TestUtil;
import com.weizhu.proto.AdminOfflineTrainingService;
import com.weizhu.proto.OfflineTrainingProtos;
import com.weizhu.proto.AdminOfflineTrainingProtos.CreateTrainRequest;
import com.weizhu.proto.AdminOfflineTrainingProtos.CreateTrainResponse;
import com.weizhu.proto.AdminOfflineTrainingProtos.GetTrainByIdRequest;
import com.weizhu.proto.AdminOfflineTrainingProtos.GetTrainByIdResponse;
import com.weizhu.proto.AdminOfflineTrainingProtos.GetTrainListRequest;
import com.weizhu.proto.AdminOfflineTrainingProtos.GetTrainListResponse;
import com.weizhu.proto.AdminOfflineTrainingProtos.GetTrainUserListRequest;
import com.weizhu.proto.AdminOfflineTrainingProtos.GetTrainUserListResponse;
import com.weizhu.proto.AdminOfflineTrainingProtos.UpdateTrainDiscoverItemRequest;
import com.weizhu.proto.AdminOfflineTrainingProtos.UpdateTrainDiscoverItemResponse;
import com.weizhu.proto.AdminOfflineTrainingProtos.UpdateTrainRequest;
import com.weizhu.proto.AdminOfflineTrainingProtos.UpdateTrainResponse;
import com.weizhu.proto.AdminOfflineTrainingProtos.UpdateTrainStateRequest;
import com.weizhu.proto.AdminOfflineTrainingProtos.UpdateTrainStateResponse;
import com.weizhu.proto.AdminProtos.AdminHead;
import com.weizhu.service.allow.AllowServiceModule;
import com.weizhu.service.official.OfficialServiceModule;
import com.weizhu.service.offline_training.OfflineTrainingServiceModule;
import com.weizhu.service.user.UserServiceModule;
import com.weizhu.service.user.test.UserServiceTestModule;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class AdminOfflineTrainingServiceTest {

	static {
		System.setProperty("logback.configurationFile", "com/weizhu/service/offline_training/test/logback.xml");
	}
	
	private static final Injector INJECTOR = Guice.createInjector(
			new TestModule(), 
			new OfflineTrainingServiceTestModule(), 
			new OfflineTrainingServiceModule(),
			new UserServiceTestModule(), new UserServiceModule(),
			new FakePushServiceModule(), new FakeProfileServiceModule(), new FakeExternalServiceModule(), 
			new AllowServiceModule(), new OfficialServiceModule()
			);
	
	@BeforeClass
	public static void init() throws Exception {
		TestUtil.clearCache(INJECTOR);
		TestUtil.loadTestDataDB(INJECTOR);
	}
	
	private final AdminHead adminHead;
	private final AdminOfflineTrainingService adminOfflineTrainingService;
	
	public AdminOfflineTrainingServiceTest() {
		this.adminHead = INJECTOR.getInstance(AdminHead.class);
		this.adminOfflineTrainingService = INJECTOR.getInstance(AdminOfflineTrainingService.class);
	}
	
	@Test
	public void test01GetTrainById() throws Exception {
		GetTrainByIdRequest request = GetTrainByIdRequest.newBuilder()
				.addTrainId(1)
				.addTrainId(2)
				.addTrainId(3)
				.addTrainId(4)
				.build();
		
		GetTrainByIdResponse response = this.adminOfflineTrainingService.getTrainById(this.adminHead, request).get();
		
		// System.out.println(response);
		
		Assert.assertEquals(3, response.getTrainCount());
		// TimeUnit.SECONDS.sleep(30);
	}
	
	@Test
	public void test02GetTrainList() throws Exception {
		GetTrainListRequest request = GetTrainListRequest.newBuilder()
				.setStart(0)
				.setLength(2)
				.build();
		
		GetTrainListResponse response = this.adminOfflineTrainingService.getTrainList(this.adminHead, request).get();
		
		// System.out.println(response);
		
		Assert.assertEquals(3, response.getTotalSize());
	}
	
	@Test
	public void test03CreateTrain() throws Exception {
		
		int now = (int)(System.currentTimeMillis() / 1000L);
		
		CreateTrainRequest request = CreateTrainRequest.newBuilder()
				.setTrainName("新创建")
				.setStartTime(now)
				.setEndTime(now + 3600)
				.setApplyEnable(false)
				.setTrainAddress("西二旗")
				.setCheckInStartTime(now)
				.setCheckInEndTime(now + 3600)
				.setArrangementText("无安排")
				.build();
		
		CreateTrainResponse response = this.adminOfflineTrainingService.createTrain(this.adminHead, request).get();
		
		// System.out.println(response);
		
		Assert.assertEquals(CreateTrainResponse.Result.SUCC, response.getResult());
	}
	
	@Test
	public void test04UpdateTrain() throws Exception {
		
		int now = (int)(System.currentTimeMillis() / 1000L);
		
		UpdateTrainRequest request = UpdateTrainRequest.newBuilder()
				.setTrainId(3)
				.setTrainName("修改一下")
				.setStartTime(now)
				.setEndTime(now + 3600)
				.setApplyEnable(false)
				.setTrainAddress("西二旗")
				.setCheckInStartTime(now)
				.setCheckInEndTime(now + 3600)
				.setArrangementText("无安排")
				.setEnableNotifyUser(false)
				.build();
		
		UpdateTrainResponse response = this.adminOfflineTrainingService.updateTrain(this.adminHead, request).get();
		
		// System.out.println(response);
		
		Assert.assertEquals(UpdateTrainResponse.Result.SUCC, response.getResult());
	}
	
	@Test
	public void test05UpdateTrainState() throws Exception {
		UpdateTrainStateRequest request = UpdateTrainStateRequest.newBuilder()
				.addTrainId(3)
				.setState(OfflineTrainingProtos.State.DELETE)
				.build();
		UpdateTrainStateResponse response = this.adminOfflineTrainingService.updateTrainState(this.adminHead, request).get();
		
		Assert.assertEquals(UpdateTrainStateResponse.Result.SUCC, response.getResult());
	}
	
	@Test
	public void test06UpdateTrainDiscoverItem() throws Exception {
		UpdateTrainDiscoverItemRequest request = UpdateTrainDiscoverItemRequest.newBuilder()
				.setTrainId(1)
				.addDiscoverItemId(1001)
				.addDiscoverItemId(1002)
				.addDiscoverItemId(1005)
				.build();
		UpdateTrainDiscoverItemResponse response = this.adminOfflineTrainingService.updateTrainDiscoverItem(this.adminHead, request).get();
		
		Assert.assertEquals(UpdateTrainDiscoverItemResponse.Result.SUCC, response.getResult());
	}
	
	@Test
	public void test07GetTrainUserList() throws Exception {
		GetTrainUserListRequest request = GetTrainUserListRequest.newBuilder()
				.setTrainId(1)
				.setStart(0)
				.setLength(10)
				.build();
		
		GetTrainUserListResponse response = this.adminOfflineTrainingService.getTrainUserList(this.adminHead, request).get();
		
		Assert.assertEquals(1, response.getTotalSize());
	}
}
