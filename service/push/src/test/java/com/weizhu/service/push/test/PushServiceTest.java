package com.weizhu.service.push.test;

import static org.junit.Assert.assertTrue;

import java.util.Collections;
import java.util.Set;

import org.junit.BeforeClass;
import org.junit.Test;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.util.Types;
import com.google.protobuf.TextFormat;
import com.weizhu.common.module.TestModule;
import com.weizhu.proto.PushPollingProtos.GetPushMsgRequest;
import com.weizhu.proto.PushPollingProtos.GetPushMsgResponse;
import com.weizhu.proto.ConnService;
import com.weizhu.proto.PushPollingService;
import com.weizhu.proto.PushProtos.GetOfflineMsgResponse;
import com.weizhu.proto.PushService;
import com.weizhu.proto.PushProtos.GetOfflineMsgRequest;
import com.weizhu.proto.PushProtos.PushMsgRequest;
import com.weizhu.proto.PushProtos.PushPacket;
import com.weizhu.proto.PushProtos.PushTarget;
import com.weizhu.proto.WeizhuProtos;
import com.weizhu.proto.WeizhuProtos.EmptyResponse;
import com.weizhu.proto.WeizhuProtos.RequestHead;
import com.weizhu.service.push.PushServiceModule;

@org.junit.Ignore
public class PushServiceTest {

	static {
		System.setProperty("logback.configurationFile", "com/weizhu/service/push/test/logback.xml");
	}
	
	private static final Injector INJECTOR = Guice.createInjector(new TestModule(), 
			new PushServiceTestModule(), new PushServiceModule(),
			new AbstractModule() {

				@SuppressWarnings("unchecked")
				@Override
				protected void configure() {
					bind((Key<Set<ConnService>>)Key.get(Types.setOf(ConnService.class))).toInstance(Collections.<ConnService>emptySet());
				}
		
	});
	
	@BeforeClass
	public static void init() throws Exception {
		// TestUtil.clearCache(INJECTOR);
		// TestUtil.loadTestDataDB(INJECTOR);
	}
	
	private final RequestHead requestHead;
	private final PushService pushService;
	private final PushPollingService pushPollingService;
	
	public PushServiceTest() {
		this.requestHead = INJECTOR.getInstance(RequestHead.class);
		this.pushService = INJECTOR.getInstance(PushService.class);
		this.pushPollingService = INJECTOR.getInstance(PushPollingService.class);
	}
	
	@Test
	public void testPushMsg() throws Exception {
		
		PushMsgRequest request = PushMsgRequest.newBuilder()
				.addPushPacket(PushPacket.newBuilder()
						.addPushTarget(PushTarget.newBuilder()
								.setUserId(10000124196L)
								.setEnableOffline(true)
								.build())
						.setPushName("TestPush")
						.setPushBody(WeizhuProtos.TestPush.newBuilder().setMessage("测试推送sdf").build().toByteString())
						.build())
				.build();
		
		EmptyResponse response = pushService.pushMsg(requestHead, request).get();
		
		assertTrue(response != null);
	}
	
	@Test
	public void testGetOfflineMsg() throws Exception {
		GetOfflineMsgRequest request = GetOfflineMsgRequest.newBuilder()
				.setPushSeq(1)
				.build();
		GetOfflineMsgResponse response = pushService.getOfflineMsg(requestHead, request).get();
		
		System.out.println(TextFormat.printToString(response));
		
		assertTrue(response.getOfflineMsgCount() > 0);
	}
	
	@Test
	public void testGetPushMsg() throws Exception {
		GetPushMsgRequest request = GetPushMsgRequest.newBuilder()
				.setPushSeq(1)
				.setMsgSize(100)
				.build();
		GetPushMsgResponse response = pushPollingService.getPushMsg(requestHead, request).get();
		
		System.out.println(TextFormat.printToString(response));
		
		assertTrue(response.getPushMsgCount() > 0);
	}
}
