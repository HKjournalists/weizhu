package com.weizhu.service.im.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.weizhu.common.module.FakeExternalServiceModule;
import com.weizhu.common.module.FakeProfileServiceModule;
import com.weizhu.common.module.FakePushServiceModule;
import com.weizhu.common.module.TestModule;
import com.weizhu.common.utils.TestUtil;
import com.weizhu.proto.IMProtos.CreateGroupChatRequest;
import com.weizhu.proto.IMProtos.CreateGroupChatResponse;
import com.weizhu.proto.IMProtos.GetGroupChatByIdRequest;
import com.weizhu.proto.IMProtos.GetGroupChatByIdResponse;
import com.weizhu.proto.IMProtos.GetGroupChatListRequest;
import com.weizhu.proto.IMProtos.GetGroupChatListResponse;
import com.weizhu.proto.IMProtos.GetGroupMessageRequest;
import com.weizhu.proto.IMProtos.GetMessageResponse;
import com.weizhu.proto.IMProtos.GetP2PChatListResponse;
import com.weizhu.proto.IMProtos.GetP2PMessageRequest;
import com.weizhu.proto.IMProtos.InstantMessage;
import com.weizhu.proto.IMProtos.JoinGroupChatRequest;
import com.weizhu.proto.IMProtos.JoinGroupChatResponse;
import com.weizhu.proto.IMProtos.LeaveGroupChatRequest;
import com.weizhu.proto.IMProtos.LeaveGroupChatResponse;
import com.weizhu.proto.IMProtos.SendGroupMessageRequest;
import com.weizhu.proto.IMProtos.SendGroupMessageResponse;
import com.weizhu.proto.IMProtos.SendP2PMessageRequest;
import com.weizhu.proto.IMProtos.SendP2PMessageResponse;
import com.weizhu.proto.IMProtos.SetGroupNameRequest;
import com.weizhu.proto.IMProtos.SetGroupNameResponse;
import com.weizhu.proto.IMService;
import com.weizhu.proto.IMProtos.GetP2PChatListRequest;
import com.weizhu.proto.IMProtos.InstantMessage.DiscoverItem;
import com.weizhu.proto.WeizhuProtos.RequestHead;
import com.weizhu.service.im.IMServiceModule;
import com.weizhu.service.user.UserServiceModule;
import com.weizhu.service.user.test.UserServiceTestModule;

public class IMServiceTest {

	static {
		System.setProperty("logback.configurationFile", "com/weizhu/service/im/test/logback.xml");
	}
	
	private static final Injector INJECTOR = Guice.createInjector(new TestModule(), 
			new IMServiceTestModule(), new IMServiceModule(),
			new UserServiceTestModule(), new UserServiceModule(),
			new FakePushServiceModule(), new FakeProfileServiceModule(), new FakeExternalServiceModule());
	
	@BeforeClass
	public static void init() throws Exception {
		TestUtil.clearCache(INJECTOR);
		TestUtil.loadTestDataDB(INJECTOR);
	}
	
	private final RequestHead requestHead;
	private final IMService imService;
	
	public IMServiceTest() {
		this.requestHead = INJECTOR.getInstance(RequestHead.class);
		this.imService = INJECTOR.getInstance(IMService.class);
	}
	
	@Test
	public void testSendP2PMessage() throws Exception {
		SendP2PMessageRequest request = SendP2PMessageRequest.newBuilder()
				.setToUserId(10000124207L)
				.setMsg(InstantMessage.newBuilder()
						.setMsgSeq(0)
						.setMsgTime(0)
						.setFromUserId(0)
						.setText(InstantMessage.Text.newBuilder()
								.setContent("888888")
								.build())
						.build())
				.build();
		
		SendP2PMessageResponse response = imService.sendP2PMessage(requestHead, request).get();
		
		assertEquals(SendP2PMessageResponse.Result.SUCC, response.getResult());
	}
	
	@Test
	public void testSendP2PMessage2() throws Exception {
		SendP2PMessageRequest request = SendP2PMessageRequest.newBuilder()
				.setToUserId(10000124207L)
				.setMsg(InstantMessage.newBuilder()
						.setMsgSeq(0)
						.setMsgTime(0)
						.setFromUserId(0)
						.setDiscoverItem(DiscoverItem.newBuilder()
								.setItemId(123L)
								.build())
						.build())
				.build();
		
		SendP2PMessageResponse response = imService.sendP2PMessage(requestHead, request).get();
		
		assertEquals(SendP2PMessageResponse.Result.SUCC, response.getResult());
	}
	
	@Test
	public void testGetP2PMessage() throws Exception {
		GetP2PMessageRequest request = GetP2PMessageRequest.newBuilder()
				.setMsgSize(10)
				.setUserId(10000124207L)
				.build();
		
		GetMessageResponse response = imService.getP2PMessage(requestHead, request).get();
		
		assertTrue(response.getMsgCount() > 0);
	}
	
	@Test
	public void testGetP2PChatList() throws Exception {
		GetP2PChatListRequest request = GetP2PChatListRequest.newBuilder()
				.setChatSize(10)
				.build();
		
		GetP2PChatListResponse response = imService.getP2PChatList(requestHead, request).get();
		
		assertTrue(response.getChatCount() > 0);
		
		// System.out.println(TextFormat.printToUnicodeString(response));
	}
	
	@Ignore
	@Test
	public void testGetP2PChatListLast() throws Exception {
		GetP2PChatListRequest request = GetP2PChatListRequest.newBuilder()
				.setChatSize(10)
				.setLastMsgTime(1418456170)
				.setLastUserId(1)
				.build();
		
		GetP2PChatListResponse response = imService.getP2PChatList(requestHead, request).get();
		
		assertTrue(response.getChatCount() > 0);
	}
	
	@Test
	public void testCreateGroupChat() throws Exception {
		CreateGroupChatRequest request = CreateGroupChatRequest.newBuilder()
				.setGroupName("我的群")
				.addMemberUserId(10000124207L)
				.addMemberUserId(10000124209L)
				.build();
		
		CreateGroupChatResponse response = imService.createGroupChat(requestHead, request).get();
		
		assertEquals(CreateGroupChatResponse.Result.SUCC, response.getResult());
	}
	
	@Test
	public void testSetGroupName() throws Exception {
		SetGroupNameRequest request = SetGroupNameRequest.newBuilder()
				.setGroupId(1)
				.setGroupName("测试改")
				.build();
		
		SetGroupNameResponse response = imService.setGroupName(requestHead, request).get();
		
		assertEquals(SetGroupNameResponse.Result.SUCC, response.getResult());
	}
	
	@Test
	public void testJoinGroupChat() throws Exception {
		JoinGroupChatRequest request = JoinGroupChatRequest.newBuilder()
				.setGroupId(1)
				.addJoinUserId(10000124188L)
				.build();
		
		JoinGroupChatResponse response = imService.joinGroupChat(requestHead, request).get();
		
		assertEquals(JoinGroupChatResponse.Result.SUCC, response.getResult());
	}
	
	@Test
	public void testLeaveGroupChat() throws Exception {
		LeaveGroupChatRequest request = LeaveGroupChatRequest.newBuilder()
				.setGroupId(2)
				.build();
		
		LeaveGroupChatResponse response = imService.leaveGroupChat(requestHead, request).get();
		
		assertEquals(LeaveGroupChatResponse.Result.SUCC, response.getResult());
	}
	
	@Test
	public void testSendGroupMessage() throws Exception {
		SendGroupMessageRequest request = SendGroupMessageRequest.newBuilder()
				.setGroupId(1)
				.setMsg(InstantMessage.newBuilder()
						.setMsgSeq(0)
						.setMsgTime(0)
						.setFromUserId(0)
						.setText(InstantMessage.Text.newBuilder()
								.setContent("999999")
								.build())
						.build())
				.build();
		
		SendGroupMessageResponse response = imService.sendGroupMessage(requestHead, request).get();
		
		assertEquals(SendGroupMessageResponse.Result.SUCC, response.getResult());
	}
	
	@Test
	public void testGetGroupMessage() throws Exception {
		GetGroupMessageRequest request = GetGroupMessageRequest.newBuilder()
				.setGroupId(1)
				.setMsgSize(10)
				.build();
		
		GetMessageResponse response = imService.getGroupMessage(requestHead, request).get();
		
		assertTrue(response.getMsgCount() > 0);
		// System.out.println(TextFormat.printToUnicodeString(response));
	}
	
	@Test
	public void testGetGroupChatList() throws Exception {
		GetGroupChatListRequest request = GetGroupChatListRequest.newBuilder()
				.setChatSize(10)
				.build();
		
		GetGroupChatListResponse response = imService.getGroupChatList(requestHead, request).get();
		
		assertTrue(response.getChatCount() > 0);
		// System.out.println(TextFormat.printToUnicodeString(response));
	}
	
	@Test
	public void testGetGroupChatById() throws Exception {
		GetGroupChatByIdRequest request = GetGroupChatByIdRequest.newBuilder()
				.addGroupId(1)
				.addGroupId(2)
				.addGroupId(3)
				.build();
		
		GetGroupChatByIdResponse response = imService.getGroupChatById(requestHead, request).get();
		assertTrue(response.getGroupChatCount() > 0);
		// System.out.println(TextFormat.printToUnicodeString(response));
	}
}
