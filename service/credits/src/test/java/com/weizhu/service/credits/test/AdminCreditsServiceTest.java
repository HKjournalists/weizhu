package com.weizhu.service.credits.test;

import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.BeforeClass;
import org.junit.Test;

import com.google.common.util.concurrent.Futures;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.protobuf.TextFormat;
import com.weizhu.common.module.FakeExternalServiceModule;
import com.weizhu.common.module.FakePushServiceModule;
import com.weizhu.common.module.TestModule;
import com.weizhu.common.utils.TestUtil;
import com.weizhu.proto.AdminCreditsProtos.AddCreditsRequest;
import com.weizhu.proto.AdminCreditsProtos.ClearUserCreditsRequest;
import com.weizhu.proto.AdminCreditsProtos.ClearUserCreditsResponse;
import com.weizhu.proto.AdminCreditsProtos.CreateCreditsOrderRequest;
import com.weizhu.proto.AdminCreditsProtos.CreateCreditsOrderResponse;
import com.weizhu.proto.AdminCreditsProtos.GetCreditsLogRequest;
import com.weizhu.proto.AdminCreditsProtos.GetCreditsLogResponse;
import com.weizhu.proto.AdminCreditsProtos.GetCreditsOperationRequest;
import com.weizhu.proto.AdminCreditsProtos.GetCreditsOperationResponse;
import com.weizhu.proto.AdminCreditsProtos.GetCreditsOrderRequest;
import com.weizhu.proto.AdminCreditsProtos.GetCreditsOrderResponse;
import com.weizhu.proto.AdminCreditsProtos.GetCreditsResponse;
import com.weizhu.proto.AdminCreditsProtos.GetCreditsRuleResponse;
import com.weizhu.proto.AdminCreditsProtos.GetExpenseCreditsResponse;
import com.weizhu.proto.AdminCreditsProtos.GetUserCreditsRequest;
import com.weizhu.proto.AdminCreditsProtos.GetUserCreditsResponse;
import com.weizhu.proto.AdminCreditsProtos.UpdateCreditsRuleRequest;
import com.weizhu.proto.AdminCreditsProtos.UserCreditsDelta;
import com.weizhu.proto.AdminCreditsService;
import com.weizhu.proto.AdminProtos.AdminHead;
import com.weizhu.proto.WeizhuProtos.EmptyRequest;
import com.weizhu.service.credits.CreditsServiceModule;
import com.weizhu.service.profile.ProfileServiceModule;
import com.weizhu.service.profile.test.ProfileServiceTestModule;
import com.weizhu.service.user.UserServiceModule;
import com.weizhu.service.user.test.UserServiceTestModule;

public class AdminCreditsServiceTest {

	static {
		System.setProperty("logback.configurationFile", "com/weizhu/service/credits/test/logback.xml");
	}
	
	private static final Injector INJECTOR = Guice.createInjector(
			new TestModule(), 
			new CreditsServiceTestModule(), new CreditsServiceModule(),
			new UserServiceTestModule(), new UserServiceModule(),
			new FakePushServiceModule(), new FakeExternalServiceModule(),
			new ProfileServiceModule(), new ProfileServiceTestModule());
	
	@BeforeClass
	public static void init() throws Exception {
		TestUtil.clearCache(INJECTOR);
		TestUtil.loadTestDataDB(INJECTOR);
	}
	
	private final AdminHead head;
	private final AdminCreditsService adminCreditsService;
	
	public AdminCreditsServiceTest () {
		this.head = INJECTOR.getInstance(AdminHead.class);
		this.adminCreditsService = INJECTOR.getInstance(AdminCreditsService.class);
	}
	
	@Test
	public void testGetCredits() {
		GetCreditsResponse response = Futures.getUnchecked(adminCreditsService.getCredits(head, EmptyRequest.getDefaultInstance()));
		assertTrue(response.getCredits() > 0);
		//System.out.println(TextFormat.printToUnicodeString(response));
	}
	
	@Test
	public void testAddCredtis() {
		AddCreditsRequest request = AddCreditsRequest.newBuilder()
				.setCreditsDelta(100)
				.setDesc("加100")
				.build();
		Futures.getUnchecked(adminCreditsService.addCredits(head, request));
		
		AddCreditsRequest request1 = AddCreditsRequest.newBuilder()
				.setCreditsDelta(- 200)
				.setDesc("减200")
				.build();
		Futures.getUnchecked(adminCreditsService.addCredits(head, request1));
		
		GetCreditsResponse response = Futures.getUnchecked(adminCreditsService.getCredits(head, EmptyRequest.getDefaultInstance()));
		assertTrue(response.getCredits() > 0);
		//System.out.println(TextFormat.printToUnicodeString(response));
		
		GetCreditsLogResponse response1 = Futures.getUnchecked(adminCreditsService.getCreditsLog(head, GetCreditsLogRequest.newBuilder()
				.setStart(0)
				.setLength(10)
				.build()));
		assertTrue(response1.getCreditsLogCount() > 0);
		//System.out.println(TextFormat.printToUnicodeString(response1));
		
	}
	
	@Test
	public void testGetUserCredits() {
		GetUserCreditsRequest request = GetUserCreditsRequest.newBuilder()
				.addAllUserId(Arrays.asList(10000124196L, 20000124196L, 30000124196L, 40000124196L, 50000124196L,
						60000124196L, 70000124196L, 80000124196L))
				.setStart(0)
				.setLength(5)
				.build();
		GetUserCreditsResponse response = Futures.getUnchecked(adminCreditsService.getUserCredits(head, request));
		assertTrue(response.getCreditsCount() > 0);
		//System.out.println(TextFormat.printToUnicodeString(response));
	}
	
	@Test
	public void testGetCreditsOrder() {
		GetCreditsOrderRequest request = GetCreditsOrderRequest.newBuilder()
				.setIsExpense(true)
				.addUserId(10000124196L)
				.setStart(0)
				.setLength(10)
				.build();
		GetCreditsOrderResponse response = Futures.getUnchecked(adminCreditsService.getCreditsOrder(head, request));
		
		GetCreditsOrderRequest request1 = GetCreditsOrderRequest.newBuilder()
				.setIsExpense(false)
				.addUserId(10000124196L)
				.setStart(0)
				.setLength(10)
				.build();
		GetCreditsOrderResponse response1 = Futures.getUnchecked(adminCreditsService.getCreditsOrder(head, request1));
		assertTrue(response1.getCreditsOrderCount() > response.getCreditsOrderCount());
	}
	
	@Test
	public void testCreateCreditsOrder() {
		List<UserCreditsDelta> userCreditsDeltaList = new ArrayList<UserCreditsDelta>();
		UserCreditsDelta.Builder userCreditsDeltaBuilder = UserCreditsDelta.newBuilder();
		userCreditsDeltaList.add(userCreditsDeltaBuilder.setCreditsDelta(100).setUserId(10000124196L).build());
		userCreditsDeltaList.add(userCreditsDeltaBuilder.setCreditsDelta(100).setUserId(20000124196L).build());
		userCreditsDeltaList.add(userCreditsDeltaBuilder.setCreditsDelta(100).setUserId(10000124207L).build());
		userCreditsDeltaList.add(userCreditsDeltaBuilder.setCreditsDelta(100).setUserId(1L).build());
		userCreditsDeltaList.add(userCreditsDeltaBuilder.setCreditsDelta(100).setUserId(2L).build());
				
		CreateCreditsOrderRequest request = CreateCreditsOrderRequest.newBuilder()
				.setDesc("给大家都加100分喽")
				.addAllUserCreditsDelta(userCreditsDeltaList)
				.build();
		CreateCreditsOrderResponse response = Futures.getUnchecked(adminCreditsService.createCreditsOrder(head, request));
		assertTrue(response.getOpterationId() > 0);
		//System.out.println(TextFormat.printToUnicodeString(response));
	}
	
	@Test
	public void testClearUserCredits() {
		List<Long> userIdList = new ArrayList<Long>();
		userIdList.add(20000124196L);
		
		ClearUserCreditsResponse response = Futures.getUnchecked(adminCreditsService.clearUserCredits(head, ClearUserCreditsRequest.newBuilder()
				.addAllUserId(userIdList)
				.build()));
		assertTrue(response.getResult().toString().equals("SUCC"));
		//System.out.println(TextFormat.printToUnicodeString(response));
	}
	
	@Test
	public void updateCreditsRule() {
		Futures.getUnchecked(adminCreditsService.updateCreditsRule(head, UpdateCreditsRuleRequest.newBuilder()
				.setCreditsRule("aaaaaaaaaaaaaaaaaaaaaaaaaaaabbbbbbbbbbbbbbbbbbbbbbbbbbbbbcccccccccc")
				.build()));
		GetCreditsRuleResponse response = Futures.getUnchecked(adminCreditsService.getCreditsRule(head, EmptyRequest.getDefaultInstance()));
		assertTrue(response.getCreditsRule().equals("aaaaaaaaaaaaaaaaaaaaaaaaaaaabbbbbbbbbbbbbbbbbbbbbbbbbbbbbcccccccccc"));
		//System.out.println(TextFormat.printToUnicodeString(response));
	}
	
	@Test
	public void getCreditsOperation() {
		GetCreditsOperationResponse response = Futures.getUnchecked(adminCreditsService.getCreditsOperation(head, GetCreditsOperationRequest.newBuilder()
				.setStart(0)
				.setLength(10)
				.build()));
		assertTrue(response.getCreditsOperationCount() > 0);
		//System.out.println(TextFormat.printToUnicodeString(response));
		
	}
	
	@Test
	public void getExpenseCredits() {
		GetExpenseCreditsResponse response = Futures.getUnchecked(adminCreditsService.getExpenseCredits(head, EmptyRequest.getDefaultInstance()));
		
		System.out.println(TextFormat.printToUnicodeString(response));
		
	}
	
}
