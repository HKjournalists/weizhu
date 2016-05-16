package com.weizhu.network;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;

import org.apache.http.impl.client.DefaultHttpClient;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.google.protobuf.ByteString;
import com.google.protobuf.TextFormat;
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
import com.weizhu.proto.IMProtos.InstantMessage;
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
import com.weizhu.proto.UserProtos.GetUserByIdRequest;
import com.weizhu.proto.UserProtos.GetUserResponse;

@org.junit.Ignore
public class HttpApiTest {

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
				.setBuildTime(123)
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
		// httpApi.setApiUrl("http://218.241.220.36:8099/api/pb");
		// httpApi.setApiUrl("http://112.126.80.93:8090/api/pb");
		
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
	
	@org.junit.Ignore
	@Test
	public void testSendSmsCode() throws Exception {
		
		SendSmsCodeRequest sendSmsCodeRequest = SendSmsCodeRequest.newBuilder()
				.setCompanyKey("weizhu")
				.setMobileNo("18601191171")
				.build();
		
		SendSmsCodeResponse sendSmsCodeResponse = loginService.sendSmsCode(sendSmsCodeRequest, 0).get();
		
		assertEquals(SendSmsCodeResponse.Result.SUCC, sendSmsCodeResponse.getResult());
		
		LoginBySmsCodeRequest loginBySmsCodeRequest = LoginBySmsCodeRequest.newBuilder()
				.setCompanyKey("weizhu")
				.setMobileNo("18601191171")
				.setSmsCode(666666)
				.build();
		
		LoginBySmsCodeResponse loginBySmsCodeResponse = loginService.loginBySmsCode(loginBySmsCodeRequest, 0).get();
		
		ArrayList<Byte> byteList = new ArrayList<Byte>();
		for (byte b : loginBySmsCodeResponse.getSessionKey().toByteArray()) {
			byteList.add(b);
		}
		System.out.println("userId: " + loginBySmsCodeResponse.getUser().getBase().getUserId() + ", " + byteList);
		
		assertEquals(LoginBySmsCodeResponse.Result.SUCC, loginBySmsCodeResponse.getResult());
	}
	
	// 10000124207:72b0a67e4f282d06121f3bbb1a9629de69208018da3085557cde6ab7405a1d0a
	// 10000124196:72b0a67e4f282d0672cfcb638edc1df7ea26a25cd8da0d5f513db9ecedaa4140
	private final ByteString sessionKey = ByteString.copyFrom(HexUtil.hex2bin("72b0a67e4f282d0672cfcb638edc1df7ea26a25cd8da0d5f513db9ecedaa4140"));
	
	@Test
	public void testGetUserById() throws Exception {
		httpApi.setSessionKey(sessionKey);
		
		GetUserByIdRequest request = GetUserByIdRequest.newBuilder().addUserId(10000124196L).build();
		
		GetUserResponse response = userService.getUserById(request, 0).get();
		
		assertEquals("lindong@21tb.com", response.getUser(0).getBase().getEmail());
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
				.setCategoryId(1)
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
		
		System.out.println(TextFormat.printToUnicodeString(response));
		
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
