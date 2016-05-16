package com.weizhu.service.apns.test;

import org.junit.BeforeClass;
import org.junit.Test;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.weizhu.common.module.FakeExternalServiceModule;
import com.weizhu.common.module.FakeProfileServiceModule;
import com.weizhu.common.module.FakePushServiceModule;
import com.weizhu.common.module.TestModule;
import com.weizhu.common.utils.TestUtil;
import com.weizhu.proto.APNsService;
import com.weizhu.proto.WeizhuProtos.RequestHead;
import com.weizhu.service.allow.AllowServiceModule;
import com.weizhu.service.apns.APNsServiceModule;
import com.weizhu.service.official.OfficialServiceModule;
import com.weizhu.service.official.test.OfficialServiceTestModule;
import com.weizhu.service.settings.SettingsServiceModule;
import com.weizhu.service.settings.test.SettingsServiceTestModule;
import com.weizhu.service.user.UserServiceModule;
import com.weizhu.service.user.test.UserServiceTestModule;

public class APNsServiceTest {

	static {
		System.setProperty("logback.configurationFile", "com/weizhu/service/apns/test/logback.xml");
	}
	
	private static final Injector INJECTOR = Guice.createInjector(new TestModule(), 
			new APNsServiceTestModule(), new APNsServiceModule(),
			new UserServiceTestModule(), new UserServiceModule(),
			new SettingsServiceTestModule(), new SettingsServiceModule(),
			new OfficialServiceTestModule(), new OfficialServiceModule(),
			new FakePushServiceModule(), new FakeProfileServiceModule(), new FakeExternalServiceModule(), 
			new AllowServiceModule());
	
	@BeforeClass
	public static void init() {
		TestUtil.clearCache(INJECTOR);
		TestUtil.loadTestDataDB(INJECTOR);
	}
	
	@SuppressWarnings("unused")
	private final RequestHead requestHead;
	@SuppressWarnings("unused")
	private final APNsService apnsService;
	
	public APNsServiceTest() {
		this.requestHead = INJECTOR.getInstance(RequestHead.class);
		this.apnsService = INJECTOR.getInstance(APNsService.class);
	}
	
	@Test
	public void test() {
	}
	
}
