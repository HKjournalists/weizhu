package com.weizhu.service.discover.test;

import static org.junit.Assert.assertTrue;

import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.protobuf.ByteString;
import com.weizhu.common.module.FakeExternalServiceModule;
import com.weizhu.common.module.FakePushServiceModule;
import com.weizhu.common.module.TestModule;
import com.weizhu.common.service.ServiceUtil;
import com.weizhu.common.utils.TestUtil;
import com.weizhu.proto.DiscoverProtos.GetDiscoverHomeResponse;
import com.weizhu.proto.DiscoverProtos.GetModuleItemListRequest;
import com.weizhu.proto.DiscoverProtos.GetModuleItemListResponse;
import com.weizhu.proto.WeizhuProtos.RequestHead;
import com.weizhu.proto.DiscoverService;
import com.weizhu.service.allow.AllowServiceModule;
import com.weizhu.service.discover.DiscoverServiceModule;
import com.weizhu.service.exam.ExamServiceModule;
import com.weizhu.service.exam.test.ExamServiceTestModule;
import com.weizhu.service.official.OfficialServiceModule;
import com.weizhu.service.user.UserServiceModule;
import com.weizhu.service.user.test.UserServiceTestModule;

@Ignore
@SuppressWarnings("deprecation")
public class DiscoverServiceTest {
	
	static {
		System.setProperty("logback.configurationFile", "com/weizhu/service/discover/test/logback.xml");
	}
	
	private static final Injector INJECTOR = Guice.createInjector(new TestModule(), 
			new DiscoverServiceTestModule(), new DiscoverServiceModule(),
			new ExamServiceTestModule(), new ExamServiceModule(),
			new UserServiceTestModule(), new UserServiceModule(),
			new FakePushServiceModule(), new FakeExternalServiceModule(),
			new AllowServiceModule(), new OfficialServiceModule());
	
	@BeforeClass
	public static void init() {
		TestUtil.clearCache(INJECTOR);
		// TestUtil.loadTestDataDB(INJECTOR);
	}
	
	private final RequestHead requestHead;
	private final DiscoverService discoverService;
	
	public DiscoverServiceTest() {
		this.requestHead = INJECTOR.getInstance(RequestHead.class);
		this.discoverService = INJECTOR.getInstance(DiscoverService.class);
	}
	
	@Test
	public void testGetDiscoverHome() throws Exception {
		@SuppressWarnings("unused")
		GetDiscoverHomeResponse response = discoverService.getDiscoverHome(requestHead, ServiceUtil.EMPTY_REQUEST).get();
		
		// System.out.println(TextFormat.printToUnicodeString(response));
	}
	
	@Test
	public void testGetModuleItemList() throws Exception {
		GetModuleItemListRequest request = GetModuleItemListRequest.newBuilder()
				.setModuleId(1)
				.setCategoryId(1)
				.setItemSize(3)
				.build();
		
		GetModuleItemListResponse response = discoverService.getModuleItemList(requestHead, request).get();
		
		// System.out.println(TextFormat.printToUnicodeString(response));
		
		assertTrue(response.getItemCount() > 0);
		
		ByteString listIdx = response.getListIndexEnd();
		
		request = GetModuleItemListRequest.newBuilder()
				.setModuleId(1)
				.setCategoryId(1)
				.setItemSize(3)
				.setListIndexBegin(listIdx)
				.build();
		
		response = discoverService.getModuleItemList(requestHead, request).get();
		
		// System.out.println(TextFormat.printToUnicodeString(response));
		
		assertTrue(response.getItemCount() > 0);
	}
	
//	@Test
	public void testGetModuleExamItemList() throws Exception {
		GetModuleItemListRequest request = GetModuleItemListRequest.newBuilder()
				.setModuleId(7)
				.setCategoryId(1)
				.setItemSize(3)
				.build();
		
		GetModuleItemListResponse response = discoverService.getModuleItemList(requestHead, request).get();
		
		// System.out.println(TextFormat.printToUnicodeString(response));
		
		assertTrue(response.getItemCount() > 0);
	}
	
	@Test
	public void test() {
		System.out.println((int) (System.currentTimeMillis() / 1000L));
	}
	
	@Test
	public void testGetItemPV() {
		
	}
	
}
