package com.weizhu.service.official.test;

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
import com.weizhu.proto.OfficialProtos.GetOfficialByIdRequest;
import com.weizhu.proto.OfficialProtos.GetOfficialByIdResponse;
import com.weizhu.proto.OfficialProtos.GetOfficialListResponse;
import com.weizhu.proto.OfficialService;
import com.weizhu.proto.OfficialProtos.GetOfficialListRequest;
import com.weizhu.proto.WeizhuProtos.RequestHead;
import com.weizhu.service.allow.AllowServiceModule;
import com.weizhu.service.official.OfficialServiceModule;
import com.weizhu.service.user.UserServiceModule;
import com.weizhu.service.user.test.UserServiceTestModule;

public class OfficialServiceTest {

	static {
		System.setProperty("logback.configurationFile", "com/weizhu/service/official/test/logback.xml");
	}
	
	private static final Injector INJECTOR = Guice.createInjector(new TestModule(), 
			new OfficialServiceTestModule(), new OfficialServiceModule(),
			new UserServiceTestModule(), new UserServiceModule(),
			new FakePushServiceModule(), new FakeProfileServiceModule(), new FakeExternalServiceModule(), 
			new AllowServiceModule());
	
	@BeforeClass
	public static void init() throws Exception {
		TestUtil.clearCache(INJECTOR);
		TestUtil.loadTestDataDB(INJECTOR);
	}
	
	private final RequestHead requestHead;
	private final OfficialService officialService;
	
	public OfficialServiceTest() {
		this.requestHead = INJECTOR.getInstance(RequestHead.class);
		this.officialService = INJECTOR.getInstance(OfficialService.class);
	}
	
	@Test
	public void testGetOfficialById() throws Exception {
		
		GetOfficialByIdRequest request = GetOfficialByIdRequest.newBuilder()
				.addOfficialId(1)
				.addOfficialId(100003)
				.build();
		
		GetOfficialByIdResponse response = officialService.getOfficialById(requestHead, request).get();
		
		assertEquals(2, response.getOfficialCount());
	}
	
	@Test
	public void testGetOfficialList() throws Exception {
		
		GetOfficialListRequest request = GetOfficialListRequest.newBuilder()
				.setOfficialSize(10)
				.build();
		
		GetOfficialListResponse response = officialService.getOfficialList(requestHead, request).get();
		
		assertEquals(5, response.getOfficialCount());
	}
}
