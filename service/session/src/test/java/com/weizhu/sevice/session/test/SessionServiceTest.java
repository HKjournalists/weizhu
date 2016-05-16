package com.weizhu.sevice.session.test;

import static org.junit.Assert.*;

import org.junit.BeforeClass;
import org.junit.Test;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.protobuf.ByteString;
import com.weizhu.common.module.FakeAPNsServiceModule;
import com.weizhu.common.module.FakeExternalServiceModule;
import com.weizhu.common.module.FakeProfileServiceModule;
import com.weizhu.common.module.FakePushServiceModule;
import com.weizhu.common.module.TestModule;
import com.weizhu.common.utils.HexUtil;
import com.weizhu.common.utils.TestUtil;
import com.weizhu.proto.SessionProtos.CreateSessionKeyResponse;
import com.weizhu.proto.SessionProtos.VerifySessionKeyRequest;
import com.weizhu.proto.SessionProtos.VerifySessionKeyResponse;
import com.weizhu.proto.SessionService;
import com.weizhu.proto.SessionProtos.CreateSessionKeyRequest;
import com.weizhu.proto.WeizhuProtos.AnonymousHead;
import com.weizhu.service.session.SessionServiceModule;
import com.weizhu.service.user.UserServiceModule;
import com.weizhu.service.user.test.UserServiceTestModule;

public class SessionServiceTest {
	
	static {
		System.setProperty("logback.configurationFile", "com/weizhu/service/session/test/logback.xml");
	}

	private static final Injector INJECTOR = Guice.createInjector(new TestModule(), 
			new SessionServiceTestModule(), new SessionServiceModule(),
			new UserServiceTestModule(), new UserServiceModule(),
			new FakePushServiceModule(), new FakeProfileServiceModule(), new FakeExternalServiceModule(), new FakeAPNsServiceModule());
	
	@BeforeClass
	public static void init() throws Exception {
		TestUtil.clearCache(INJECTOR);
		TestUtil.loadTestDataDB(INJECTOR);
	}
	
	private final AnonymousHead anonymousHead;
	private final SessionService sessionService;
	
	public SessionServiceTest() {
		this.anonymousHead = INJECTOR.getInstance(AnonymousHead.class);
		this.sessionService = INJECTOR.getInstance(SessionService.class);
	}
	
	@Test
	public void testCreate() throws Exception {
		long userId = 10000124196L;

		CreateSessionKeyRequest request = CreateSessionKeyRequest.newBuilder()
				.setCompanyId(0)
				.setUserId(userId)
				.build();
		
		CreateSessionKeyResponse response = sessionService.createSessionKey(anonymousHead, request).get();
		
		System.out.println(userId + ":" + HexUtil.bin2Hex(response.getSessionKey().toByteArray()));
		
		assertTrue(response.hasSessionKey());
	}
	
	@Test
	public void testVerify() throws Exception {
		byte[] sessionKey = HexUtil.hex2bin("72b0a67e4f282d0672cfcb638edc1df7ea26a25cd8da0d5f513db9ecedaa4140");
		
		VerifySessionKeyRequest request = VerifySessionKeyRequest.newBuilder().setSessionKey(ByteString.copyFrom(sessionKey)).build();
		
		VerifySessionKeyResponse response = sessionService.verifySessionKey(anonymousHead, request).get();
		
		assertEquals(VerifySessionKeyResponse.Result.SUCC, response.getResult());
		assertEquals(10000124196L, response.getSession().getUserId());
	}
	
}
