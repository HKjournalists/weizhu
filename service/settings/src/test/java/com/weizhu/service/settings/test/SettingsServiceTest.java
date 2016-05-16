package com.weizhu.service.settings.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.BeforeClass;
import org.junit.Test;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.weizhu.common.module.TestModule;
import com.weizhu.common.service.ServiceUtil;
import com.weizhu.common.utils.TestUtil;
import com.weizhu.proto.SettingsProtos.GetUserSettingsResponse;
import com.weizhu.proto.SettingsProtos.SettingsResponse;
import com.weizhu.proto.SettingsService;
import com.weizhu.proto.SettingsProtos.GetUserSettingsRequest;
import com.weizhu.proto.SettingsProtos.SetDoNotDisturbRequest;
import com.weizhu.proto.SettingsProtos.Settings;
import com.weizhu.proto.WeizhuProtos.RequestHead;
import com.weizhu.service.settings.SettingsServiceModule;

public class SettingsServiceTest {
	
	static {
		System.setProperty("logback.configurationFile", "com/weizhu/service/settings/test/logback.xml");
	}

	private static final Injector INJECTOR = Guice.createInjector(new TestModule(), 
			new SettingsServiceTestModule(), new SettingsServiceModule());
	
	@BeforeClass
	public static void init() throws Exception {
		TestUtil.clearCache(INJECTOR);
		TestUtil.loadTestDataDB(INJECTOR);
	}
	
	private final RequestHead requestHead;
	private final SettingsService settingsService;
	
	public SettingsServiceTest() {
		this.requestHead = INJECTOR.getInstance(RequestHead.class);
		this.settingsService = INJECTOR.getInstance(SettingsService.class);
	}
	
	@Test
	public void testGetSettings() throws Exception {
		SettingsResponse response = settingsService.getSettings(requestHead, ServiceUtil.EMPTY_REQUEST).get();
		
		assertEquals(requestHead.getSession().getUserId(), response.getSettings().getUserId());
	}
	
	@Test
	public void testUpdateDoNotDisturb() throws Exception {
		SetDoNotDisturbRequest request = SetDoNotDisturbRequest.newBuilder()
				.setDoNotDisturb(Settings.DoNotDisturb.newBuilder()
						.setEnable(true)
						.setBeginTime(123)
						.setEndTime(234)
						.build())
				.build();
		
		SettingsResponse response = settingsService.setDoNotDisturb(requestHead, request).get();
		
		assertTrue(response.getSettings().getDoNotDisturb().getEnable());
	}
	
	@Test
	public void testGetUserSettingsRequest() throws Exception {
		GetUserSettingsRequest request = GetUserSettingsRequest.newBuilder()
				.addUserId(2)
				.addUserId(10000124196L)
				.build();
		
		GetUserSettingsResponse response = settingsService.getUserSettings(requestHead, request).get();
		
		assertTrue(response.getSettingsCount() > 0);
	}
	
}
