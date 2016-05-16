package com.weizhu.service.profile.test;

import org.junit.BeforeClass;
import org.junit.Test;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.weizhu.common.module.TestModule;
import com.weizhu.common.utils.TestUtil;
import com.weizhu.proto.ProfileService;
import com.weizhu.proto.WeizhuProtos.RequestHead;
import com.weizhu.service.profile.ProfileServiceModule;

@SuppressWarnings("unused")
public class ProfileServiceTest {
	
	static {
		System.setProperty("logback.configurationFile", "com/weizhu/service/profile/test/logback.xml");
	}
	
	private static final Injector INJECTOR = Guice.createInjector(
			new TestModule(), new ProfileServiceModule(), new ProfileServiceTestModule());
	
	@BeforeClass
	public static void init() throws Exception {
		TestUtil.clearCache(INJECTOR);
		TestUtil.loadTestDataDB(INJECTOR);
	}
	
	private final RequestHead requestHead;
	private final ProfileService profileService;
	
	public ProfileServiceTest() {
		this.requestHead = INJECTOR.getInstance(RequestHead.class);
		this.profileService = INJECTOR.getInstance(ProfileService.class);
	}
	
	@Test
	public void test() {
		
	}
}
