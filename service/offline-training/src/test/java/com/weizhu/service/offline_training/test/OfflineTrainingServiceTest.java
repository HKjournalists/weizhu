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
import com.weizhu.common.service.ServiceUtil;
import com.weizhu.common.utils.TestUtil;
import com.weizhu.proto.OfflineTrainingService;
import com.weizhu.proto.OfflineTrainingProtos.ApplyTrainRequest;
import com.weizhu.proto.OfflineTrainingProtos.ApplyTrainResponse;
import com.weizhu.proto.OfflineTrainingProtos.CheckInTrainRequest;
import com.weizhu.proto.OfflineTrainingProtos.CheckInTrainResponse;
import com.weizhu.proto.OfflineTrainingProtos.GetClosedTrainListRequest;
import com.weizhu.proto.OfflineTrainingProtos.GetClosedTrainListResponse;
import com.weizhu.proto.OfflineTrainingProtos.GetOpenTrainCountResponse;
import com.weizhu.proto.OfflineTrainingProtos.GetOpenTrainListRequest;
import com.weizhu.proto.OfflineTrainingProtos.GetOpenTrainListResponse;
import com.weizhu.proto.OfflineTrainingProtos.GetTrainByIdRequest;
import com.weizhu.proto.OfflineTrainingProtos.GetTrainByIdResponse;
import com.weizhu.proto.OfflineTrainingProtos.LeaveTrainRequest;
import com.weizhu.proto.OfflineTrainingProtos.LeaveTrainResponse;
import com.weizhu.proto.WeizhuProtos.RequestHead;
import com.weizhu.service.allow.AllowServiceModule;
import com.weizhu.service.official.OfficialServiceModule;
import com.weizhu.service.offline_training.OfflineTrainingServiceModule;
import com.weizhu.service.user.UserServiceModule;
import com.weizhu.service.user.test.UserServiceTestModule;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class OfflineTrainingServiceTest {

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
	
	private final RequestHead requestHead;
	private final OfflineTrainingService offlineTrainingService;
	
	public OfflineTrainingServiceTest() {
		this.requestHead = INJECTOR.getInstance(RequestHead.class);
		this.offlineTrainingService = INJECTOR.getInstance(OfflineTrainingService.class);
	}
	
	@Test
	public void test01GetOpenTrainList() throws Exception {
		GetOpenTrainListRequest request = GetOpenTrainListRequest.newBuilder()
				.setSize(10)
				.build();
		
		GetOpenTrainListResponse response = this.offlineTrainingService.getOpenTrainList(this.requestHead, request).get();
		
		Assert.assertEquals(1, response.getTrainCount());
	}
	
	@Test
	public void test02GetOpenTrainCountList() throws Exception {
		GetOpenTrainCountResponse response = this.offlineTrainingService.getOpenTrainCount(this.requestHead, ServiceUtil.EMPTY_REQUEST).get();
		
		Assert.assertEquals(1, response.getOpenTrainCount());
	}
	
	@Test
	public void test03GetClosedTrainList() throws Exception {
		GetClosedTrainListRequest request = GetClosedTrainListRequest.newBuilder()
				.setSize(10)
				.build();
		
		GetClosedTrainListResponse response = this.offlineTrainingService.getClosedTrainList(this.requestHead, request).get();
		
		Assert.assertEquals(1, response.getTrainCount());
	}
	
	@Test
	public void test04GetTrainById() throws Exception {
		GetTrainByIdRequest request = GetTrainByIdRequest.newBuilder()
				.addTrainId(1)
				.build();
		
		GetTrainByIdResponse response = this.offlineTrainingService.getTrainById(this.requestHead, request).get();
		
		Assert.assertEquals(1, response.getTrainCount());
	}
	
	@Test
	public void test05ApplyTrain() throws Exception {
		ApplyTrainRequest request = ApplyTrainRequest.newBuilder()
				.setTrainId(1)
				.setIsCancel(false)
				.build();
		
		ApplyTrainResponse response = this.offlineTrainingService.applyTrain(this.requestHead, request).get();
		
//		System.out.println(response.getFailText());
		
		Assert.assertEquals(ApplyTrainResponse.Result.SUCC, response.getResult());
	}
	
	@Test
	public void test06CheckInTrain() throws Exception {
		CheckInTrainRequest request = CheckInTrainRequest.newBuilder()
				.setTrainId(1)
				.build();
		
		CheckInTrainResponse response = this.offlineTrainingService.checkInTrain(this.requestHead, request).get();
		
//		System.out.println(response.getFailText());
		
		Assert.assertEquals(CheckInTrainResponse.Result.SUCC, response.getResult());
	}
	
	@Test
	public void test07LeaveTrain() throws Exception {
		LeaveTrainRequest request = LeaveTrainRequest.newBuilder()
				.setTrainId(1)
				.setIsCancel(false)
				.setLeaveReason("aaaa")
				.build();
		
		LeaveTrainResponse response = this.offlineTrainingService.leaveTrain(this.requestHead, request).get();
		
//		System.out.println(response.getFailText());
		
		Assert.assertEquals(LeaveTrainResponse.Result.SUCC, response.getResult());
	}
}
