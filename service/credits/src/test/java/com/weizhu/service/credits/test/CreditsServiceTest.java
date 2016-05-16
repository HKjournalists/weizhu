package com.weizhu.service.credits.test;

import static org.junit.Assert.assertTrue;

import java.util.Map;
import java.util.TreeMap;

import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import com.google.common.base.Charsets;
import com.google.common.hash.Hashing;
import com.google.common.util.concurrent.Futures;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.weizhu.common.module.FakeExternalServiceModule;
import com.weizhu.common.module.FakePushServiceModule;
import com.weizhu.common.module.TestModule;
import com.weizhu.common.utils.TestUtil;
import com.weizhu.proto.CreditsProtos.DuibaConsumeCreditsRequest;
import com.weizhu.proto.CreditsProtos.DuibaConsumeCreditsResponse;
import com.weizhu.proto.CreditsProtos.DuibaNotifyRequest;
import com.weizhu.proto.CreditsProtos.DuibaNotifyResponse;
import com.weizhu.proto.CreditsProtos.DuibaShopUrlRequest;
import com.weizhu.proto.CreditsProtos.DuibaShopUrlResponse;
import com.weizhu.proto.CreditsProtos.GetCreditsOrderRequest;
import com.weizhu.proto.CreditsProtos.GetCreditsOrderResponse;
import com.weizhu.proto.CreditsProtos.GetCreditsResponse;
import com.weizhu.proto.CreditsProtos.GetCreditsRuleResponse;
import com.weizhu.proto.CreditsService;
import com.weizhu.proto.WeizhuProtos.AnonymousHead;
import com.weizhu.proto.WeizhuProtos.EmptyRequest;
import com.weizhu.proto.WeizhuProtos.RequestHead;
import com.weizhu.service.credits.CreditsServiceModule;
import com.weizhu.service.profile.ProfileServiceModule;
import com.weizhu.service.profile.test.ProfileServiceTestModule;
import com.weizhu.service.user.UserServiceModule;
import com.weizhu.service.user.test.UserServiceTestModule;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class CreditsServiceTest {
	
	static {
		System.setProperty("logback.configurationFile", "com/weizhu/service/credits/test/logback.xml");
	}
	
	private static final Injector INJECTOR = Guice.createInjector(
			new TestModule(), 
			new CreditsServiceTestModule(), new CreditsServiceModule(),
			new UserServiceTestModule(), new UserServiceModule(),
			new FakePushServiceModule(), new FakeExternalServiceModule(),
			new ProfileServiceModule(), new ProfileServiceTestModule());
	
	@BeforeClass
	public static void init() throws Exception {
		TestUtil.clearCache(INJECTOR);
		TestUtil.loadTestDataDB(INJECTOR);
	}
	
	private final AnonymousHead anonymousHead;
	private final RequestHead head;
	private final CreditsService creditsService;
	
	public CreditsServiceTest () {
		this.anonymousHead = INJECTOR.getInstance(AnonymousHead.class);
		this.head = INJECTOR.getInstance(RequestHead.class);
		this.creditsService = INJECTOR.getInstance(CreditsService.class);
	}
	
	@Test
	public void atestDuibaShopUrl() {
		String redirect = "";
		
		DuibaShopUrlRequest request = DuibaShopUrlRequest.newBuilder()
				.setRedirect(redirect)
				.build();
		
		DuibaShopUrlResponse response = Futures.getUnchecked(creditsService.duibaShopUrl(head, request));
		assertTrue(response.getUrl().length() > 0);
		//System.out.println(TextFormat.printToString(response));
	}
	
	@Test
	public void btestDuibaConsumeCredits() {
		String uid = "0:10000124196";
		long credits = 20L;     
		String appKey = "3E9TTxbaiN5cMYRQryvHe5w974xC";
		String appSecret = "ADW564kbSzR93SpCDSE7EEq8QDH";
		long timestamp = System.currentTimeMillis();
		String description = "兑换商品扣除20积分";
		String orderNum = "201601151234567";
		String type = "object";
		int facePrice = 20;
        int actualPrice = 2;
        String ip = "127.0.0.1";          
		boolean waitAudit = false;  
		String params = "详情参数，不同的类型，返回不同的内容，中间用英文冒号分隔。(支付宝类型带中文，请用utf-8进行解码) 实物商品：返回收货信息(姓名:手机号:省份:城市:区域:详细地址)、支付宝：返回账号信息(支付宝账号:实名)、话费：返回手机号、QB：返回QQ号";     
		
		Map<String, String> param = new TreeMap<String, String>();
		param.put("uid", uid);
		param.put("credits", String.valueOf(credits));
		param.put("appKey", appKey);
		param.put("appSecret", appSecret);
		param.put("timestamp", String.valueOf(timestamp));
		param.put("description", String.valueOf(description));
		param.put("orderNum", orderNum);
		param.put("type", type);
		param.put("facePrice", String.valueOf(facePrice));
		param.put("actualPrice", String.valueOf(actualPrice));
		param.put("ip", ip);
		param.put("waitAudit", String.valueOf(waitAudit));
		param.put("params", params);
		
		StringBuilder signParam = new StringBuilder();
		for (String value : param.values()) {
			signParam.append(value);
		}

		String sign = Hashing.md5().hashBytes(signParam.toString().getBytes(Charsets.UTF_8)).toString();
	
		DuibaConsumeCreditsRequest request = DuibaConsumeCreditsRequest.newBuilder()
				.setUid(uid)
				.setCredits(credits)
				.setAppKey(appKey)
				.setTimeStamp(String.valueOf(timestamp))
				.setDescription(description)
				.setOrderNum(orderNum)
				.setType(type)
				.setFacePrice(facePrice)
				.setActualPrice(actualPrice)
				.setIp(ip)
				.setWaitAudit(waitAudit)
				.setParams(params)
				.setSign(sign)
				.build();
		DuibaConsumeCreditsResponse response = Futures.getUnchecked(creditsService.duibaConsumeCredits(anonymousHead, request));
		assertTrue(response.getCredits() > 0);
		//System.out.println(TextFormat.printToUnicodeString(response));
	}
	
	@Test
	public void ctestDuibaNotify() {
		String appKey = "3E9TTxbaiN5cMYRQryvHe5w974xC";
		long timestamp = System.currentTimeMillis();
		boolean success = true;
		String orderNum = "201601151234567";
		String bizID = "0:9";
		
		String appSecret = "ADW564kbSzR93SpCDSE7EEq8QDH";
		
		Map<String, String> param = new TreeMap<String, String>();
		param.put("appKey", appKey);
		param.put("timestamp", String.valueOf(timestamp));
		param.put("success", String.valueOf(success));
		param.put("orderNum", orderNum);
		param.put("bizID", bizID);
		param.put("appSecret", appSecret);
		
		StringBuilder signParam = new StringBuilder();
		for (String value : param.values()) {
			signParam.append(value);
		}
		
		String sign = Hashing.md5().hashBytes(signParam.toString().getBytes(Charsets.UTF_8)).toString();
		
		DuibaNotifyRequest request = DuibaNotifyRequest.newBuilder()
				.setAppKey(appKey)
				.setTimeStamp(timestamp)
				.setSuccess(success)
				.setOrderNum(orderNum)
				.setBizId(bizID)
				.setSign(sign)
				.build();
		
		DuibaNotifyResponse response = Futures.getUnchecked(creditsService.duibaNotify(anonymousHead, request));
		assertTrue(response.getResult().toString().equals("SUCC"));
		//System.out.println(TextFormat.printToUnicodeString(response));
	}
	
	@Test
	public void ftestDuibaConsumeCredits() {
		String uid = "0:10000124196";
		long credits = 30L;
		String appKey = "3E9TTxbaiN5cMYRQryvHe5w974xC";
		String appSecret = "ADW564kbSzR93SpCDSE7EEq8QDH";
		long timestamp = System.currentTimeMillis();
		String description = "兑换商品扣除30积分,积分严重不足";
		String orderNum = "201601151234568";
		String type = "object";
		int facePrice = 30;
        int actualPrice = 3;
        String ip = "127.0.0.1";          
		boolean waitAudit = false;  
		String params = "详情参数，不同的类型，返回不同的内容，中间用英文冒号分隔。(支付宝类型带中文，请用utf-8进行解码) 实物商品：返回收货信息(姓名:手机号:省份:城市:区域:详细地址)、支付宝：返回账号信息(支付宝账号:实名)、话费：返回手机号、QB：返回QQ号";     
		
		Map<String, String> param = new TreeMap<String, String>();
		param.put("uid", uid);
		param.put("credits", String.valueOf(credits));
		param.put("appKey", appKey);
		param.put("appSecret", appSecret);
		param.put("timestamp", String.valueOf(timestamp));
		param.put("description", String.valueOf(description));
		param.put("orderNum", orderNum);
		param.put("type", type);
		param.put("facePrice", String.valueOf(facePrice));
		param.put("actualPrice", String.valueOf(actualPrice));
		param.put("ip", ip);
		param.put("waitAudit", String.valueOf(waitAudit));
		param.put("params", params);
		
		StringBuilder signParam = new StringBuilder();
		for (String value : param.values()) {
			signParam.append(value);
		}

		String sign = Hashing.md5().hashBytes(signParam.toString().getBytes(Charsets.UTF_8)).toString();
	
		DuibaConsumeCreditsRequest request = DuibaConsumeCreditsRequest.newBuilder()
				.setUid(uid)
				.setCredits(credits)
				.setAppKey(appKey)
				.setTimeStamp(String.valueOf(timestamp))
				.setDescription(description)
				.setOrderNum(orderNum)
				.setType(type)
				.setFacePrice(facePrice)
				.setActualPrice(actualPrice)
				.setIp(ip)
				.setWaitAudit(waitAudit)
				.setParams(params)
				.setSign(sign)
				.build();
		DuibaConsumeCreditsResponse response = Futures.getUnchecked(creditsService.duibaConsumeCredits(anonymousHead, request));
		assertTrue(response.getStatus().toString().equals("ok"));
		//System.out.println(TextFormat.printToUnicodeString(response));
	}
	
	@Test
	public void gtestDuibaNotify() {
		String appKey = "3E9TTxbaiN5cMYRQryvHe5w974xC";
		long timestamp = System.currentTimeMillis();
		boolean success = false;
		String orderNum = "201601151234568";
		String bizID = "0:10";
		
		String appSecret = "ADW564kbSzR93SpCDSE7EEq8QDH";
		
		Map<String, String> param = new TreeMap<String, String>();
		param.put("appKey", appKey);
		param.put("timestamp", String.valueOf(timestamp));
		param.put("success", String.valueOf(success));
		param.put("orderNum", orderNum);
		param.put("bizID", bizID);
		param.put("appSecret", appSecret);
		
		StringBuilder signParam = new StringBuilder();
		for (String value : param.values()) {
			signParam.append(value);
		}
		
		String sign = Hashing.md5().hashBytes(signParam.toString().getBytes(Charsets.UTF_8)).toString();
		
		DuibaNotifyRequest request = DuibaNotifyRequest.newBuilder()
				.setAppKey(appKey)
				.setTimeStamp(timestamp)
				.setSuccess(success)
				.setOrderNum(orderNum)
				.setBizId(bizID)
				.setSign(sign)
				.build();
		
		DuibaNotifyResponse response = Futures.getUnchecked(creditsService.duibaNotify(anonymousHead, request));
		assertTrue(response.getResult().toString().equals("SUCC"));
		//System.out.println(TextFormat.printToUnicodeString(response));
	}

	@Test
	public void htestDuibaConsumeCredits() {
		String uid = "0:10000124196";
		long credits = 300L;
		String appKey = "3E9TTxbaiN5cMYRQryvHe5w974xC";
		String appSecret = "ADW564kbSzR93SpCDSE7EEq8QDH";
		long timestamp = System.currentTimeMillis();
		String description = "兑换商品扣除300积分,积分严重不足";
		String orderNum = "201601151234569";
		String type = "object";
		int facePrice = 300;
        int actualPrice = 3;
        String ip = "127.0.0.1";          
		boolean waitAudit = false;  
		String params = "详情参数，不同的类型，返回不同的内容，中间用英文冒号分隔。(支付宝类型带中文，请用utf-8进行解码) 实物商品：返回收货信息(姓名:手机号:省份:城市:区域:详细地址)、支付宝：返回账号信息(支付宝账号:实名)、话费：返回手机号、QB：返回QQ号";     
		
		Map<String, String> param = new TreeMap<String, String>();
		param.put("uid", uid);
		param.put("credits", String.valueOf(credits));
		param.put("appKey", appKey);
		param.put("appSecret", appSecret);
		param.put("timestamp", String.valueOf(timestamp));
		param.put("description", String.valueOf(description));
		param.put("orderNum", orderNum);
		param.put("type", type);
		param.put("facePrice", String.valueOf(facePrice));
		param.put("actualPrice", String.valueOf(actualPrice));
		param.put("ip", ip);
		param.put("waitAudit", String.valueOf(waitAudit));
		param.put("params", params);
		
		StringBuilder signParam = new StringBuilder();
		for (String value : param.values()) {
			signParam.append(value);
		}

		String sign = Hashing.md5().hashBytes(signParam.toString().getBytes(Charsets.UTF_8)).toString();
	
		DuibaConsumeCreditsRequest request = DuibaConsumeCreditsRequest.newBuilder()
				.setUid(uid)
				.setCredits(credits)
				.setAppKey(appKey)
				.setTimeStamp(String.valueOf(timestamp))
				.setDescription(description)
				.setOrderNum(orderNum)
				.setType(type)
				.setFacePrice(facePrice)
				.setActualPrice(actualPrice)
				.setIp(ip)
				.setWaitAudit(waitAudit)
				.setParams(params)
				.setSign(sign)
				.build();
		DuibaConsumeCreditsResponse response = Futures.getUnchecked(creditsService.duibaConsumeCredits(anonymousHead, request));
		assertTrue(response.getStatus().toString().equals("ok"));
		//System.out.println(TextFormat.printToUnicodeString(response));
	}
	
	@Test
	public void itestDuibaNotify() {
		String appKey = "3E9TTxbaiN5cMYRQryvHe5w974xC";
		long timestamp = System.currentTimeMillis();
		boolean success = false;
		String orderNum = "201601151234569";
		String bizID = "0:12";
		
		String appSecret = "ADW564kbSzR93SpCDSE7EEq8QDH";
		
		Map<String, String> param = new TreeMap<String, String>();
		param.put("appKey", appKey);
		param.put("timestamp", String.valueOf(timestamp));
		param.put("success", String.valueOf(success));
		param.put("orderNum", orderNum);
		param.put("bizID", bizID);
		param.put("appSecret", appSecret);
		
		StringBuilder signParam = new StringBuilder();
		for (String value : param.values()) {
			signParam.append(value);
		}
		
		String sign = Hashing.md5().hashBytes(signParam.toString().getBytes(Charsets.UTF_8)).toString();
		
		DuibaNotifyRequest request = DuibaNotifyRequest.newBuilder()
				.setAppKey(appKey)
				.setTimeStamp(timestamp)
				.setSuccess(success)
				.setOrderNum(orderNum)
				.setBizId(bizID)
				.setSign(sign)
				.build();
		
		DuibaNotifyResponse response = Futures.getUnchecked(creditsService.duibaNotify(anonymousHead, request));
		assertTrue(response.getResult().toString().equals("SUCC"));
		//System.out.println(TextFormat.printToUnicodeString(response));
	}

	@Test
	public void dtestGetCredits() {
		GetCreditsResponse response = Futures.getUnchecked(creditsService.getCredits(head, EmptyRequest.getDefaultInstance()));
		assertTrue(response.getCredits().getCredits() > 0);
		//System.out.println(TextFormat.printToUnicodeString(response));
	}
	
	@Test
	public void jtestGetCreditsOrder() {
		GetCreditsOrderRequest request = GetCreditsOrderRequest.newBuilder()
				.setIsExpense(false)
				.setSize(10)
				.build();
		
		GetCreditsOrderResponse response = Futures.getUnchecked(creditsService.getCreditsOrder(head, request));
		assertTrue(response.getHasMore());
		//System.out.println(TextFormat.printToUnicodeString(response));
	}
	
	@Test
	public void ktestGetCreditsOrder() {
		GetCreditsOrderRequest request = GetCreditsOrderRequest.newBuilder()
				.setIsExpense(true)
				.setSize(10)
				.build();
		
		GetCreditsOrderResponse response = Futures.getUnchecked(creditsService.getCreditsOrder(head, request));
		assertTrue(response.getCreditsOrderCount() > 0);
		//System.out.println(TextFormat.printToUnicodeString(response));
	}
	
	@Test
	public void testGetCreditsRule() {
		GetCreditsRuleResponse response = Futures.getUnchecked(creditsService.getCreditsRule(head, EmptyRequest.getDefaultInstance()));
		assertTrue(response.getCreditsRule().length() > 0);
		//System.out.println(TextFormat.printToUnicodeString(response));
	}
}
