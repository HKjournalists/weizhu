package com.weizhu.network;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.apache.http.impl.client.DefaultHttpClient;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import com.google.protobuf.ByteString;
import com.weizhu.proto.ContactsService;
import com.weizhu.proto.DiscoverService;
import com.weizhu.proto.IMService;
import com.weizhu.proto.LoginService;
import com.weizhu.proto.PushPollingService;
import com.weizhu.proto.ServiceProxy;
import com.weizhu.proto.SettingsService;
import com.weizhu.proto.UserService;
import com.weizhu.proto.WeizhuProtos;
import com.weizhu.proto.DiscoverProtos.GetDiscoverHomeResponse;
import com.weizhu.proto.DiscoverProtos.GetModuleItemListRequest;
import com.weizhu.proto.DiscoverProtos.GetModuleItemListResponse;
import com.weizhu.proto.IMProtos.CreateGroupChatRequest;
import com.weizhu.proto.IMProtos.CreateGroupChatResponse;
import com.weizhu.proto.IMProtos.GetGroupChatListRequest;
import com.weizhu.proto.IMProtos.GetGroupChatListResponse;
import com.weizhu.proto.IMProtos.InstantMessage;
import com.weizhu.proto.IMProtos.SendGroupMessageRequest;
import com.weizhu.proto.IMProtos.SendGroupMessageResponse;
import com.weizhu.proto.IMProtos.SendP2PMessageRequest;
import com.weizhu.proto.LoginProtos.LoginBySmsCodeRequest;
import com.weizhu.proto.LoginProtos.LoginBySmsCodeResponse;
import com.weizhu.proto.LoginProtos.SendSmsCodeRequest;
import com.weizhu.proto.LoginProtos.SendSmsCodeResponse;
import com.weizhu.proto.PushPollingProtos.GetPushMsgRequest;
import com.weizhu.proto.PushPollingProtos.GetPushMsgResponse;
import com.weizhu.proto.SettingsProtos.SetDoNotDisturbRequest;
import com.weizhu.proto.SettingsProtos.Settings;
import com.weizhu.proto.SettingsProtos.SettingsResponse;
import com.weizhu.proto.UserProtos.CreateUserExperienceRequest;
import com.weizhu.proto.UserProtos.CreateUserExperienceResponse;
import com.weizhu.proto.UserProtos.GetTeamRequest;
import com.weizhu.proto.UserProtos.GetTeamResponse;
import com.weizhu.proto.UserProtos.GetUserByIdRequest;
import com.weizhu.proto.UserProtos.GetUserByMobileNoRequest;
import com.weizhu.proto.UserProtos.GetUserResponse;
import com.weizhu.proto.UserProtos.Team;
import com.weizhu.proto.UserProtos.UserExperience;

@org.junit.Ignore
public class HttpApiDevTest {

	private static HttpApi httpApi;

	private static LoginService loginService;
	private static UserService userService;
	@SuppressWarnings("unused")
	private static ContactsService contactsService;
	private static IMService imService;
	private static PushPollingService pushPollingService;
	private static DiscoverService discoverService;
	private static SettingsService settingsService;
	
	@BeforeClass
	public static void init() {
		
		WeizhuProtos.Weizhu weizhuVersion = WeizhuProtos.Weizhu.newBuilder()
				.setPlatform(WeizhuProtos.Weizhu.Platform.ANDROID)
				.setVersionName("1.0.0")
				.setVersionCode(0)
				.setStage(WeizhuProtos.Weizhu.Stage.ALPHA)
				.setBuildTime(123) // (int)(System.currentTimeMillis()/1000L))
				.build();
		
		WeizhuProtos.Android androidOs = WeizhuProtos.Android.newBuilder()
				.setDevice("device")
				.setManufacturer("LGE")
				.setBrand("google")
				.setModel("Nexus 5")
				.setSerial("02c288c1f0a697d9")
				.setRelease("4.4.4")
				.setSdkInt(19)
				.setCodename("REL")
				.build();
		
		httpApi = new HttpApi(new DefaultHttpClient(), 5, 5, weizhuVersion, androidOs);
		httpApi.setApiUrl("http://218.241.220.36:8099/api/pb");

		loginService = ServiceProxy.create(LoginService.class, httpApi);
		userService = ServiceProxy.create(UserService.class, httpApi);
		contactsService = ServiceProxy.create(ContactsService.class, httpApi);
		imService = ServiceProxy.create(IMService.class, httpApi);
		pushPollingService = ServiceProxy.create(PushPollingService.class, httpApi);
		discoverService = ServiceProxy.create(DiscoverService.class, httpApi);
		settingsService = ServiceProxy.create(SettingsService.class, httpApi);
	}
	
	@AfterClass
	public static void destroy() {
		httpApi.shutdown();
	}
	
	@Ignore
	@Test
	public void testSendSmsCode() throws Exception {
		
		SendSmsCodeRequest sendSmsCodeRequest = SendSmsCodeRequest.newBuilder()
				.setCompanyKey("")
				.setMobileNo("18601191171")
				.build();
		
		SendSmsCodeResponse sendSmsCodeResponse = loginService.sendSmsCode(sendSmsCodeRequest, 0).get();
		
		assertEquals(SendSmsCodeResponse.Result.SUCC, sendSmsCodeResponse.getResult());
		
		LoginBySmsCodeRequest loginBySmsCodeRequest = LoginBySmsCodeRequest.newBuilder()
				.setCompanyKey("")
				.setMobileNo("18601191171")
				.setSmsCode(666666)
				.build();
		
		LoginBySmsCodeResponse loginBySmsCodeResponse = loginService.loginBySmsCode(loginBySmsCodeRequest, 0).get();
		
		long userId = loginBySmsCodeResponse.getUser().getBase().getUserId();
		
		System.out.println(userId + ":" + HexUtil.bin2Hex(loginBySmsCodeResponse.getSessionKey().toByteArray()));
		System.out.println(loginBySmsCodeResponse.getRefTeamCount());
		
		assertEquals(LoginBySmsCodeResponse.Result.SUCC, loginBySmsCodeResponse.getResult());
	}
	
	// 10000124196:265b69b0c448ddd51c3d483624a239a8de3b27ce3bbddd6b91ce545e1fd64c7c
	// 10000124207:265b69b0c448ddd57a9169dba3d7754cdde4f2826839acf23c7952cfceb20eb5
	private final ByteString sessionKey = ByteString.copyFrom(HexUtil.hex2bin("265b69b0c448ddd51c3d483624a239a8de3b27ce3bbddd6b91ce545e1fd64c7c"));
	
	@Test
	public void testGetUserById() throws Exception {
		httpApi.setSessionKey(sessionKey);
		
		GetUserByIdRequest request = GetUserByIdRequest.newBuilder().addUserId(10000124196L).build();
		
		GetUserResponse response = userService.getUserById(request, 0).get();
		
		assertTrue(response.getUserCount() > 0);
		// assertEquals("15910318525", response.getUser().getBase().getMobileNo(0));
	}
	
	@Test
	public void testGetUserByMobileNo() throws Exception {
		httpApi.setSessionKey(sessionKey);
		
		GetUserByMobileNoRequest request = GetUserByMobileNoRequest.newBuilder().setMobileNo("15911064399").build();
		
		GetUserResponse response = userService.getUserByMobileNo(request, 0).get();
		
		assertTrue(response.getUserCount() > 0);
		
		System.out.println(response.getRefTeamCount());
	}
	
	@Test
	public void testAddExperience() throws Exception {
		httpApi.setSessionKey(sessionKey);
		
		CreateUserExperienceRequest request = CreateUserExperienceRequest.newBuilder()
				.setExperience(UserExperience.newBuilder().setExperienceId(0).setExperienceContent("中文test").build())
				.build();
		
		CreateUserExperienceResponse response = userService.createUserExperience(request, 0).get();
		
		assertEquals(CreateUserExperienceResponse.Result.SUCC, response.getResult());
	}
	
	@Test
	public void testSendP2PMessage() throws Exception {
		httpApi.setSessionKey(sessionKey);
		
		SendP2PMessageRequest request = SendP2PMessageRequest.newBuilder()
				.setToUserId(10000124207L)
				.setMsg(InstantMessage.newBuilder()
						.setMsgSeq(0)
						.setMsgTime(0)
						.setFromUserId(0L)
						.setText(InstantMessage.Text.newBuilder()
								.setContent("我了个去")
								.build())
						.build())
				.build();
		
		imService.sendP2PMessage(request, 0).get();
	}
	
	@Test
	public void testSendP2PMessageUser() throws Exception {
		httpApi.setSessionKey(sessionKey);
		
		SendP2PMessageRequest request = SendP2PMessageRequest.newBuilder()
				.setToUserId(10000124207L)
				.setMsg(InstantMessage.newBuilder()
						.setMsgSeq(0)
						.setMsgTime(0)
						.setFromUserId(0L)
						.setUser(InstantMessage.User.newBuilder()
								.setUserId(10000124207L)
								.build())
						.build())
				.build();
		
		imService.sendP2PMessage(request, 0).get();
	}
	
	@Test
	public void testGetPushPollingMsg() throws Exception {
		httpApi.setSessionKey(sessionKey);
		
		GetPushMsgRequest request = GetPushMsgRequest.newBuilder()
				.setPushSeq(0)
				.setMsgSize(10)
				.build();
		
		GetPushMsgResponse response = pushPollingService.getPushMsg(request, 0).get();
		
		assertTrue(response.getPushMsgCount() > 0);
				
	}
	
	@Test
	public void testGetTeam() throws Exception {
		httpApi.setSessionKey(sessionKey);
		
		GetTeamRequest request = GetTeamRequest.newBuilder()
				.setTeamId(1)
				.build();
		
		GetTeamResponse response = userService.getTeam(request, 0).get();
		
		System.out.println(response.getSubTeamIdList());
		
		for (Team team : response.getRefTeamList()) {
			System.out.println("teamId:" + team.getTeamId() + ", teamName:" + team.getTeamName());
		}
	}
	
	@Ignore
	@Test
	public void testCreateGroupChat() throws Exception {
		httpApi.setSessionKey(sessionKey);
		
		CreateGroupChatRequest request = CreateGroupChatRequest.newBuilder()
				.setGroupName("我的群")
				.addMemberUserId(10000124207L)
				.addMemberUserId(10000124209L)
				.build();
		
		CreateGroupChatResponse response = imService.createGroupChat(request, 0).get();
		
		assertEquals(CreateGroupChatResponse.Result.SUCC, response.getResult());
	}
	
	@Test
	public void testSendGroupMessage() throws Exception {
		httpApi.setSessionKey(sessionKey);
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
		
		SendGroupMessageResponse response = imService.sendGroupMessage(request, 0).get();
		
		assertEquals(SendGroupMessageResponse.Result.SUCC, response.getResult());
	}
	
	@Test
	public void testGetGroupChatList() throws Exception {
		httpApi.setSessionKey(sessionKey);
		
		GetGroupChatListRequest request = GetGroupChatListRequest.newBuilder()
				.setChatSize(20)
				.build();
		
		GetGroupChatListResponse response = imService.getGroupChatList(request, 0).get();
		
		assertTrue(response.getChatCount() > 0);
		
		System.out.println("response size : " + response.getSerializedSize());
		for (int i=0; i<response.getChatCount(); ++i) {
			System.out.println(response.getChat(i).getGroupName());
		}
	}
	
	@Test
	public void testGetDiscoverHome() throws Exception {
		httpApi.setSessionKey(sessionKey);
		GetDiscoverHomeResponse response = discoverService.getDiscoverHome(WeizhuProtos.EmptyRequest.newBuilder().build(), 0).get();
		
		for (int i=0; i<response.getBannerCount(); ++i) {
			System.out.println(response.getBanner(i).getBannerId());
		}
	}
	
	@Test
	public void testGetModuleItemList() throws Exception {
		httpApi.setSessionKey(sessionKey);
		GetModuleItemListRequest request = GetModuleItemListRequest.newBuilder()
				.setModuleId(1)
				.setCategoryId(3)
				.setItemSize(3)
				.build();
		
		GetModuleItemListResponse response = discoverService.getModuleItemList(request, 0).get();
		
		// System.out.println(TextFormat.printToUnicodeString(response));
		
		assertTrue(response.getItemCount() > 0);
		
		ByteString listIdx = response.getListIndexEnd();
		
		request = GetModuleItemListRequest.newBuilder()
				.setModuleId(1)
				.setCategoryId(1)
				.setItemSize(3)
				.setListIndexBegin(listIdx)
				.build();
		
		response = discoverService.getModuleItemList(request, 0).get();
		
		// System.out.println(TextFormat.printToUnicodeString(response));
		
		assertTrue(response.getItemCount() > 0);
	}
	
	@Test
	public void testUpdateDoNotDisturb() throws Throwable {
		httpApi.setSessionKey(sessionKey);
		SetDoNotDisturbRequest request = SetDoNotDisturbRequest.newBuilder()
				.setDoNotDisturb(Settings.DoNotDisturb.newBuilder()
						.setEnable(true)
						.setBeginTime(123)
						.setEndTime(234)
						.build())
				.build();
		
		SettingsResponse response = settingsService.setDoNotDisturb(request, 0).get();
		
		assertTrue(response.getSettings().getDoNotDisturb().getEnable());
	}
}