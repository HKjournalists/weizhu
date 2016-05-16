package com.weizhu.service.login;

import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.annotation.Nullable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import com.google.common.base.Splitter;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.weizhu.common.service.AsyncImpl;
import com.weizhu.common.service.ServiceUtil;
import com.weizhu.common.utils.MobileNoUtil;
import com.weizhu.common.utils.ProfileManager;
import com.weizhu.common.utils.TaskManager;
import com.weizhu.proto.AdminOfficialService;
import com.weizhu.proto.AdminProtos.AdminHead;
import com.weizhu.proto.AdminUserProtos.GetUserByIdRequest;
import com.weizhu.proto.AdminUserProtos.GetUserByIdResponse;
import com.weizhu.proto.AdminUserProtos.GetUserByMobileNoUniqueRequest;
import com.weizhu.proto.AdminUserProtos.GetUserByMobileNoUniqueResponse;
import com.weizhu.proto.AdminUserProtos.RegisterUserRequest;
import com.weizhu.proto.AdminUserProtos.RegisterUserResponse;
import com.weizhu.proto.AdminUserService;
import com.weizhu.proto.CompanyProtos;
import com.weizhu.proto.CompanyProtos.VerifyCompanyKeyResponse;
import com.weizhu.proto.ExternalProtos.SendSmsRequest;
import com.weizhu.proto.ExternalProtos.SendSmsResponse;
import com.weizhu.proto.ExternalService;
import com.weizhu.proto.LoginProtos;
import com.weizhu.proto.LoginProtos.GetLoginSmsCodeRequest;
import com.weizhu.proto.LoginProtos.GetLoginSmsCodeResponse;
import com.weizhu.proto.LoginProtos.GetWebLoginByTokenRequest;
import com.weizhu.proto.LoginProtos.GetWebLoginByTokenResponse;
import com.weizhu.proto.LoginProtos.LoginAutoRequest;
import com.weizhu.proto.LoginProtos.LoginAutoResponse;
import com.weizhu.proto.LoginProtos.RegisterBySmsCodeRequest;
import com.weizhu.proto.LoginProtos.RegisterBySmsCodeResponse;
import com.weizhu.proto.LoginProtos.SendRegisterSmsCodeRequest;
import com.weizhu.proto.LoginProtos.SendRegisterSmsCodeResponse;
import com.weizhu.proto.UserProtos;
import com.weizhu.proto.AdminOfficialProtos.SendSecretaryMessageRequest;
import com.weizhu.proto.CompanyProtos.VerifyCompanyKeyRequest;
import com.weizhu.proto.CompanyService;
import com.weizhu.proto.LoginProtos.LoginBySmsCodeRequest;
import com.weizhu.proto.LoginProtos.LoginBySmsCodeResponse;
import com.weizhu.proto.LoginProtos.NotifyWebLoginByTokenRequest;
import com.weizhu.proto.LoginProtos.NotifyWebLoginByTokenResponse;
import com.weizhu.proto.LoginProtos.SendSmsCodeRequest;
import com.weizhu.proto.LoginProtos.SendSmsCodeResponse;
import com.weizhu.proto.LoginProtos.WebLoginByTokenRequest;
import com.weizhu.proto.LoginProtos.WebLoginByTokenResponse;
import com.weizhu.proto.SessionProtos.CreateSessionKeyRequest;
import com.weizhu.proto.SessionProtos.CreateSessionKeyResponse;
import com.weizhu.proto.SessionProtos.CreateWebLoginSessionKeyResponse;
import com.weizhu.proto.SessionService;
import com.weizhu.proto.WeizhuProtos.AnonymousHead;
import com.weizhu.proto.LoginService;
import com.weizhu.proto.OfficialProtos;
import com.weizhu.proto.WeizhuProtos.EmptyRequest;
import com.weizhu.proto.WeizhuProtos.EmptyResponse;
import com.weizhu.proto.WeizhuProtos.RequestHead;
import com.weizhu.service.login.LoginDAOProtos.NotifyWebLoginData;

/**
 * 全局服务，需要考虑多公司情况
 * @author lindongjlu
 *
 */
public class LoginServiceImpl implements LoginService {

	private static final Logger logger = LoggerFactory.getLogger(LoginServiceImpl.class);
	
	private static final int SMS_CODE_EXPIRED_SECOND = 1 * 60 * 60; // 1 hour
	
	private static final ProfileManager.ProfileKey<String> LOGIN_SECRETARY_SAY_HELLO_CONTENT = 
			ProfileManager.createKey("login:secretary_sayhello_content", (String) null);
	private static final ProfileManager.ProfileKey<String> LOGIN_SMS_TEXT = 
			ProfileManager.createKey("login:login_sms_text", "【微助】您的验证码是${sms_code}，在60分钟内有效。如非本人操作请忽略本短信。");
	private static final ProfileManager.ProfileKey<String> REGISTER_SMS_TEXT = 
			ProfileManager.createKey("login:register_sms_text", "【微助】您的验证码是${sms_code}，在60分钟内有效。如非本人操作请忽略本短信。");
	private static final ProfileManager.ProfileKey<String> FAKE_SMS_CODE_MOBILE_NO = 
			ProfileManager.createKey("login:fake_sms_code_mobile_no", (String)null);
	private static final ProfileManager.ProfileKey<Boolean> FAKE_SMS_CODE_ALL = 
			ProfileManager.createKey("login:fake_sms_code_all", false);
	private static final ProfileManager.ProfileKey<Integer> FAKE_SMS_CODE_VALUE = 
			ProfileManager.createKey("login:fake_sms_code_value", 666666);
	private static final ProfileManager.ProfileKey<UserProtos.UserBase.State> REGISTER_USER_STATE = 
			ProfileManager.createKey("login:register_user_state", UserProtos.UserBase.State.APPROVE, UserProtos.UserBase.State.class);
	
	private static final Splitter MOBILE_NO_SPLITTER = Splitter.on(',').trimResults().omitEmptyStrings();
	
	private final JedisPool jedisPool;
	private final CompanyService companyService;
	private final AdminUserService adminUserService;
	private final ExternalService externalService;
	private final SessionService sessionService;
	private final AdminOfficialService adminOfficialService;
	
	private final ProfileManager profileManager;
	
	private final boolean fakeSmsCode;
	private final boolean autoLoginEnable;
	
	private final TaskManager.TaskHandler<LoginDAOProtos.NotifyWebLoginData> notifyWebLoginByTokenTaskHandler;

	@Inject
	public LoginServiceImpl(JedisPool jedisPool, 
			@Named("service_scheduled_executor") ScheduledExecutorService scheduledExecutorService, 
			CompanyService companyService, AdminUserService adminUserService,
			ExternalService externalService, SessionService sessionService, 
			AdminOfficialService adminOfficialService, 
			ProfileManager profileManager, TaskManager taskManager, 
			@Named("login_fake_sms_code") boolean fakeSmsCode,
			@Named("login_auto_login_enable") boolean autoLoginEnable
			) {
		this.jedisPool = jedisPool;
		this.companyService = companyService;
		this.adminUserService = adminUserService;
		this.externalService = externalService;
		this.sessionService = sessionService;
		this.adminOfficialService = adminOfficialService;
		this.profileManager = profileManager;
		
		this.fakeSmsCode = fakeSmsCode;
		this.autoLoginEnable = autoLoginEnable;
		
		scheduledExecutorService.scheduleAtFixedRate(new WebLoginExpireCheckTask(), 5, 5, TimeUnit.SECONDS);
		
		this.notifyWebLoginByTokenTaskHandler = 
				taskManager.register("login:notify_web_login_by_token", LoginDAOProtos.NotifyWebLoginData.PARSER, new NotifyWebLoginByTokenTask());
	}
	
	private CompanyProtos.Company doVerifyCompanyKey(AnonymousHead head, String companyKey) {
		VerifyCompanyKeyResponse response = 
				Futures.getUnchecked(this.companyService.verifyCompanyKey(head, 
						VerifyCompanyKeyRequest.newBuilder()
						.setCompanyKey(companyKey)
						.build()));
		
		return response.hasCompany() ? response.getCompany() : null;
	}
	
	private UserProtos.User checkUserMobileNo(List<UserProtos.User> userList, String mobileNo) {
		if (userList.isEmpty()) {
			return null;
		}
		
		// 校验一下，保证一个手机号只能获取到一个非删除状态的用户
		UserProtos.User user = null;
		for (UserProtos.User u : userList) {
			if (u.getBase().getMobileNoList().contains(mobileNo) 
					&& u.getBase().getState() != UserProtos.UserBase.State.DELETE
					) {
				if (user == null) {
					user = u;
				} else {
					throw new RuntimeException("multi user has same mobile no. mobile_no : " + mobileNo + ", user_id : " + user.getBase().getUserId() + ", " + u.getBase().getUserId());
				}
			}
		}
		
		return user;
	}

	private final Random rand = new Random();

	@Override
	public ListenableFuture<SendSmsCodeResponse> sendSmsCode(AnonymousHead head, final SendSmsCodeRequest request) {
		if (!MobileNoUtil.isValid(request.getMobileNo())) {
			return Futures.immediateFuture(SendSmsCodeResponse.newBuilder()
					.setResult(SendSmsCodeResponse.Result.FAIL_MOBILE_NO_INVALID)
					.setFailText("手机号格式不正确")
					.build());
		}
		
		final String mobileNo = MobileNoUtil.adjustMobileNo(request.getMobileNo());
		final CompanyProtos.Company company = this.doVerifyCompanyKey(head, request.getCompanyKey());
		
		if (company == null) {
			logger.warn("mobile no cannot find company : " + request.getCompanyKey() + ", " + mobileNo);			
			return Futures.immediateFuture(SendSmsCodeResponse.newBuilder()
					.setResult(SendSmsCodeResponse.Result.FAIL_USER_NOT_EXSIT)
					.setFailText("该手机号找不到对应的用户")
					.build());
		}
		
		final AnonymousHead newHead = head.toBuilder().setCompanyId(company.getCompanyId()).build();
		
		GetUserByMobileNoUniqueResponse getUserByMobileNoUniqueResponse = 
				Futures.getUnchecked(this.adminUserService.getUserByMobileNoUnique(newHead,
						GetUserByMobileNoUniqueRequest.newBuilder()
						.addMobileNo(mobileNo)
						.build()));
		
		final UserProtos.User user = this.checkUserMobileNo(getUserByMobileNoUniqueResponse.getUserList(), mobileNo);
		
		if (user == null) {
			logger.warn("mobile no cannot find user : " + request.getCompanyKey() + ", " + mobileNo);			
			return Futures.immediateFuture(SendSmsCodeResponse.newBuilder()
					.setResult(SendSmsCodeResponse.Result.FAIL_USER_NOT_EXSIT)
					.setFailText("该手机号找不到对应的用户")
					.setCompanyId(company.getCompanyId())
					.build());
		}
		
		switch (user.getBase().getState()) {
			case NORMAL:
				break;
			case DISABLE:
				return Futures.immediateFuture(SendSmsCodeResponse.newBuilder()
						.setResult(SendSmsCodeResponse.Result.FAIL_USER_NOT_EXSIT)
						.setFailText("该用户已被停用，请联系管理员")
						.setCompanyId(company.getCompanyId())
						.build());
			case DELETE:
				return Futures.immediateFuture(SendSmsCodeResponse.newBuilder()
						.setResult(SendSmsCodeResponse.Result.FAIL_USER_NOT_EXSIT)
						.setFailText("该用户已被删除，请联系管理员")
						.setCompanyId(company.getCompanyId())
						.build());
			case APPROVE:
				return Futures.immediateFuture(SendSmsCodeResponse.newBuilder()
						.setResult(SendSmsCodeResponse.Result.FAIL_USER_NOT_EXSIT)
						.setFailText("该用户正在审核中，请稍后")
						.setCompanyId(company.getCompanyId())
						.build());
			default:
				return Futures.immediateFuture(SendSmsCodeResponse.newBuilder()
						.setResult(SendSmsCodeResponse.Result.FAIL_USER_NOT_EXSIT)
						.setFailText("用户状态异常，请联系管理员")
						.setCompanyId(company.getCompanyId())
						.build());
		}
		
		final ProfileManager.Profile profile = this.profileManager.getProfile(newHead, "login:");
		
		final long companyId = company.getCompanyId();
		final long userId = user.getBase().getUserId();
		final int now = (int) (System.currentTimeMillis() / 1000L);
		final int code; 
		final boolean isCodeFake;
		if (this.fakeSmsCode || profile.get(FAKE_SMS_CODE_ALL)) {
			code = profile.get(FAKE_SMS_CODE_VALUE);
			isCodeFake = true;
		} else {
			String fakeSmsCodeMobileNo = profile.get(FAKE_SMS_CODE_MOBILE_NO);
			if (fakeSmsCodeMobileNo != null && MOBILE_NO_SPLITTER.splitToList(fakeSmsCodeMobileNo).contains(mobileNo)) {
				code = profile.get(FAKE_SMS_CODE_VALUE);
				isCodeFake = true;
			} else {
				code = 100000 + rand.nextInt(900000);
				isCodeFake = false;
			}
		}
		
		Jedis jedis = jedisPool.getResource();
		try {
			LoginDAOProtos.SmsCodeList.Builder builder = LoginDAOProtos.SmsCodeList.newBuilder();
			builder.addSmsCode(LoginProtos.SmsCode.newBuilder()
							.setSmsCode(code)
							.setCreateTime(now)
							.setMobileNo(mobileNo)
							.build());
			
			LoginDAOProtos.SmsCodeList smsCodeList = LoginCache.getLoginSmsCode(jedis, companyId, Collections.singleton(userId)).get(userId);
			if (smsCodeList != null) {
				for (LoginProtos.SmsCode smsCode : smsCodeList.getSmsCodeList()) {
					if (mobileNo.equals(smsCode.getMobileNo()) && now - smsCode.getCreateTime() < 60) {
						return Futures.immediateFuture(SendSmsCodeResponse.newBuilder()
								.setResult(SendSmsCodeResponse.Result.FAIL_SEND_LIMIT_EXCEEDED)
								.setFailText("短信验证码发送太频繁")
								.setCompanyId(company.getCompanyId())
								.build());
					}
					
					if (builder.getSmsCodeCount() < 3) {
						builder.addSmsCode(smsCode);
					}
				}
			}
			
			smsCodeList = builder.build();
			
			LoginCache.setLoginSmsCode(jedis, companyId, Collections.<Long, LoginDAOProtos.SmsCodeList> singletonMap(userId, smsCodeList));
		} finally {
			jedis.close();
		}
		
		if (isCodeFake) {
			return Futures.immediateFuture(SendSmsCodeResponse.newBuilder()
					.setResult(SendSmsCodeResponse.Result.SUCC)
					.setCompanyId(company.getCompanyId())
					.build());
		}
		
		String smsText = profile.get(LOGIN_SMS_TEXT);
		
		smsText = smsText.replace("${sms_code}", String.valueOf(code));
		
		SendSmsResponse sendSmsResponse = Futures.getUnchecked(
				this.externalService.sendSms(newHead, SendSmsRequest.newBuilder()
				.addMobileNo(mobileNo)
				.setSmsText(smsText)
				.build()));
		
		if (sendSmsResponse.getResult() == SendSmsResponse.Result.SUCC) {
			return Futures.immediateFuture(SendSmsCodeResponse.newBuilder()
					.setResult(SendSmsCodeResponse.Result.SUCC)
					.setCompanyId(company.getCompanyId())
					.build());
		} else {
			return Futures.immediateFuture(SendSmsCodeResponse.newBuilder()
					.setResult(SendSmsCodeResponse.Result.FAIL_SEND_FAIL)
					.setFailText("发送短信验证码失败")
					.setCompanyId(company.getCompanyId())
					.build());
		}
	}
	
	@Override
	public ListenableFuture<LoginBySmsCodeResponse> loginBySmsCode(final AnonymousHead head, final LoginBySmsCodeRequest request) {
		if (!MobileNoUtil.isValid(request.getMobileNo())) {
			return Futures.immediateFuture(LoginBySmsCodeResponse.newBuilder()
					.setResult(LoginBySmsCodeResponse.Result.FAIL_MOBILE_NO_INVALID)
					.setFailText("手机号格式错误")
					.build());
		}
		
		final String mobileNo = MobileNoUtil.adjustMobileNo(request.getMobileNo());
		final CompanyProtos.Company company = this.doVerifyCompanyKey(head, request.getCompanyKey());
		
		if (company == null) {
			logger.warn("mobile no cannot find company : " + request.getCompanyKey() + ", " + mobileNo);			
			return Futures.immediateFuture(LoginBySmsCodeResponse.newBuilder()
					.setResult(LoginBySmsCodeResponse.Result.FAIL_MOBILE_NO_INVALID)
					.setFailText("该手机号找不到对应的用户")
					.build());
		}
		
		final AnonymousHead newHead = head.toBuilder().setCompanyId(company.getCompanyId()).build();
		
		GetUserByMobileNoUniqueResponse getUserByMobileNoUniqueResponse = 
				Futures.getUnchecked(this.adminUserService.getUserByMobileNoUnique(newHead,
						GetUserByMobileNoUniqueRequest.newBuilder()
						.addMobileNo(mobileNo)
						.build()));
		
		final UserProtos.User user = this.checkUserMobileNo(getUserByMobileNoUniqueResponse.getUserList(), mobileNo);
		
		if (user == null) {
			logger.warn("mobile no cannot find user : " + request.getCompanyKey() + ", " + mobileNo);			
			return Futures.immediateFuture(LoginBySmsCodeResponse.newBuilder()
					.setResult(LoginBySmsCodeResponse.Result.FAIL_MOBILE_NO_INVALID)
					.setFailText("该手机号找不到对应的用户")
					.setCompanyId(company.getCompanyId())
					.build());
		}
		
		switch (user.getBase().getState()) {
			case NORMAL:
				break;
			case DISABLE:
				return Futures.immediateFuture(LoginBySmsCodeResponse.newBuilder()
						.setResult(LoginBySmsCodeResponse.Result.FAIL_MOBILE_NO_INVALID)
						.setFailText("该用户已被停用，请联系管理员")
						.setCompanyId(company.getCompanyId())
						.build());
			case DELETE:
				return Futures.immediateFuture(LoginBySmsCodeResponse.newBuilder()
						.setResult(LoginBySmsCodeResponse.Result.FAIL_MOBILE_NO_INVALID)
						.setFailText("该用户已被删除，请联系管理员")
						.setCompanyId(company.getCompanyId())
						.build());
			case APPROVE:
				return Futures.immediateFuture(LoginBySmsCodeResponse.newBuilder()
						.setResult(LoginBySmsCodeResponse.Result.FAIL_MOBILE_NO_INVALID)
						.setFailText("该用户正在审核中，请稍后")
						.setCompanyId(company.getCompanyId())
						.build());
			default:
				return Futures.immediateFuture(LoginBySmsCodeResponse.newBuilder()
						.setResult(LoginBySmsCodeResponse.Result.FAIL_MOBILE_NO_INVALID)
						.setFailText("用户状态异常，请联系管理员")
						.setCompanyId(company.getCompanyId())
						.build());
		}
		
		final long companyId = company.getCompanyId();
		final long userId = user.getBase().getUserId();
		final int now = (int) (System.currentTimeMillis() / 1000L);
		
		Jedis jedis = jedisPool.getResource();
		try {
			LoginDAOProtos.SmsCodeList smsCodeList = LoginCache.getLoginSmsCode(jedis, companyId, Collections.<Long>singleton(userId)).get(userId);
			if (smsCodeList == null || smsCodeList.getSmsCodeCount() <= 0) {
				return Futures.immediateFuture(LoginBySmsCodeResponse.newBuilder()
						.setResult(LoginBySmsCodeResponse.Result.FAIL_SMS_CODE_EXPIRED)
						.setFailText("验证码已过期，请重新发送")
						.setCompanyId(company.getCompanyId())
						.build());
			}
			
			LoginDAOProtos.SmsCodeList.Builder builder = LoginDAOProtos.SmsCodeList.newBuilder();
			
			boolean hasMobileNo = false;
			boolean isCodeEquals = false;
			for (LoginProtos.SmsCode smsCode : smsCodeList.getSmsCodeList()) {
				if (now - smsCode.getCreateTime() < SMS_CODE_EXPIRED_SECOND) {
					if (mobileNo.equals(smsCode.getMobileNo())) {
						hasMobileNo = true;
						if (request.getSmsCode() == smsCode.getSmsCode()) {
							isCodeEquals = true;
							continue;
						}
					}
						
					builder.addSmsCode(smsCode);
				}
			}
		
			if (!isCodeEquals) {
				if (hasMobileNo) {
					return Futures.immediateFuture(LoginBySmsCodeResponse.newBuilder()
							.setResult(LoginBySmsCodeResponse.Result.FAIL_SMS_CODE_INVALID)
							.setFailText("短信验证码错误")
							.setCompanyId(company.getCompanyId())
							.build());
				} else {
					return Futures.immediateFuture(LoginBySmsCodeResponse.newBuilder()
							.setResult(LoginBySmsCodeResponse.Result.FAIL_SMS_CODE_EXPIRED)
							.setFailText("短信验证码已过期，请重新发送")
							.setCompanyId(company.getCompanyId())
							.build());
				}
			}
			
			LoginCache.setLoginSmsCode(jedis, companyId, Collections.<Long, LoginDAOProtos.SmsCodeList> singletonMap(userId, builder.build()));
			
		} finally {
			jedis.close();
		}
		
		CreateSessionKeyResponse createSessionKeyResponse = Futures.getUnchecked(
				this.sessionService.createSessionKey(newHead, 
						CreateSessionKeyRequest.newBuilder()
						.setCompanyId(companyId)
						.setUserId(userId)
						.build()));
		

		final RequestHead requestHead = ServiceUtil.toRequestHead(newHead, createSessionKeyResponse.getSession());
		final ProfileManager.Profile profile = this.profileManager.getProfile(requestHead, "login:");
		
		String sayHelloContent = profile.get(LOGIN_SECRETARY_SAY_HELLO_CONTENT);
		if (sayHelloContent != null && !sayHelloContent.trim().isEmpty()) {
			this.adminOfficialService.sendSecretaryMessage(requestHead, SendSecretaryMessageRequest.newBuilder()
					.addUserId(userId)
					.setSendMsg(OfficialProtos.OfficialMessage.newBuilder()
							.setMsgSeq(-1L)
							.setMsgTime(0)
							.setIsFromUser(false)
							.setText(OfficialProtos.OfficialMessage.Text.newBuilder()
									.setContent(sayHelloContent.trim())
									.build())
							.build())
					.build());
		}
		
		return Futures.immediateFuture(LoginBySmsCodeResponse.newBuilder()
				.setResult(LoginBySmsCodeResponse.Result.SUCC)
				.setSessionKey(createSessionKeyResponse.getSessionKey())
				.setUser(user)
				.addAllRefTeam(getUserByMobileNoUniqueResponse.getRefTeamList())
				.addAllRefPosition(getUserByMobileNoUniqueResponse.getRefPositionList())
				.addAllRefLevel(getUserByMobileNoUniqueResponse.getRefLevelList())
				.setCompanyId(companyId)
				.build());
	}

	@Override
	public ListenableFuture<SendSmsCodeResponse> sendSmsCode(RequestHead head, SendSmsCodeRequest request) {
		return sendSmsCode(ServiceUtil.toAnonymousHead(head), request);
	}

	@Override
	public ListenableFuture<LoginBySmsCodeResponse> loginBySmsCode(RequestHead head, LoginBySmsCodeRequest request) {
		return loginBySmsCode(ServiceUtil.toAnonymousHead(head), request);
	}
	
	@Override
	public ListenableFuture<LoginAutoResponse> loginAuto(AnonymousHead head, LoginAutoRequest request) {
		if (!this.autoLoginEnable) {
			return Futures.immediateFuture(LoginAutoResponse.newBuilder()
					.setResult(LoginAutoResponse.Result.FAIL_AUTO_LOGIN_DISABLE)
					.setFailText("自动登录已禁用")
					.build());
		}
		if (!MobileNoUtil.isValid(request.getMobileNo())) {
			return Futures.immediateFuture(LoginAutoResponse.newBuilder()
					.setResult(LoginAutoResponse.Result.FAIL_MOBILE_NO_INVALID)
					.setFailText("手机号格式错误")
					.build());
		}
		
		final String mobileNo = MobileNoUtil.adjustMobileNo(request.getMobileNo());
		final CompanyProtos.Company company = this.doVerifyCompanyKey(head, request.getCompanyKey());
		
		if (company == null) {
			logger.warn("mobile no cannot find company : " + request.getCompanyKey() + ", " + mobileNo);			
			return Futures.immediateFuture(LoginAutoResponse.newBuilder()
					.setResult(LoginAutoResponse.Result.FAIL_MOBILE_NO_INVALID)
					.setFailText("该手机号找不到对应的用户")
					.build());
		}
		
		final AnonymousHead newHead = head.toBuilder().setCompanyId(company.getCompanyId()).build();
		
		GetUserByMobileNoUniqueResponse getUserByMobileNoUniqueResponse = 
				Futures.getUnchecked(this.adminUserService.getUserByMobileNoUnique(newHead,
						GetUserByMobileNoUniqueRequest.newBuilder()
						.addMobileNo(mobileNo)
						.build()));
		
		final UserProtos.User user = this.checkUserMobileNo(getUserByMobileNoUniqueResponse.getUserList(), mobileNo);
		
		if (user == null) {
			logger.warn("mobile no cannot find user : " + request.getCompanyKey() + ", " + mobileNo);			
			return Futures.immediateFuture(LoginAutoResponse.newBuilder()
					.setResult(LoginAutoResponse.Result.FAIL_USER_NOT_FOUND)
					.setFailText("该手机号找不到对应的用户")
					.setCompanyId(company.getCompanyId())
					.build());
		}
		
		final long companyId = company.getCompanyId();
		final long userId = user.getBase().getUserId();
		
		if (request.hasCompanyId() && request.getCompanyId() != companyId) {
			logger.warn("auto login company id invalid : " + request.getCompanyId() + " -> " + companyId);			
			return Futures.immediateFuture(LoginAutoResponse.newBuilder()
					.setResult(LoginAutoResponse.Result.FAIL_UNKNOWN)
					.setFailText("自动登录信息错误")
					.setCompanyId(company.getCompanyId())
					.build());
		}
		
		if (request.hasUserId() && request.getUserId() != userId) {
			logger.warn("auto login user id invalid : " + request.getUserId() + " -> " + userId);			
			return Futures.immediateFuture(LoginAutoResponse.newBuilder()
					.setResult(LoginAutoResponse.Result.FAIL_UNKNOWN)
					.setFailText("自动登录信息错误")
					.setCompanyId(company.getCompanyId())
					.build());
		}
		
		switch (user.getBase().getState()) {
			case NORMAL:
				break;
			case DISABLE:
				return Futures.immediateFuture(LoginAutoResponse.newBuilder()
						.setResult(LoginAutoResponse.Result.FAIL_USER_DISABLE)
						.setFailText("该用户已被停用，请联系管理员")
						.setCompanyId(company.getCompanyId())
						.build());
			case DELETE:
				return Futures.immediateFuture(LoginAutoResponse.newBuilder()
						.setResult(LoginAutoResponse.Result.FAIL_USER_DISABLE)
						.setFailText("该用户已被删除，请联系管理员")
						.setCompanyId(company.getCompanyId())
						.build());
			case APPROVE:
				return Futures.immediateFuture(LoginAutoResponse.newBuilder()
						.setResult(LoginAutoResponse.Result.FAIL_USER_DISABLE)
						.setFailText("该用户正在审核中，请稍后")
						.setCompanyId(company.getCompanyId())
						.build());
			default:
				return Futures.immediateFuture(LoginAutoResponse.newBuilder()
						.setResult(LoginAutoResponse.Result.FAIL_USER_DISABLE)
						.setFailText("用户状态异常，请联系管理员")
						.setCompanyId(company.getCompanyId())
						.build());
		}
		
		CreateSessionKeyResponse createSessionKeyResponse = Futures.getUnchecked(
				this.sessionService.createSessionKey(newHead, 
						CreateSessionKeyRequest.newBuilder()
						.setCompanyId(companyId)
						.setUserId(userId)
						.build()));
		

		final RequestHead requestHead = ServiceUtil.toRequestHead(newHead, createSessionKeyResponse.getSession());
		final ProfileManager.Profile profile = this.profileManager.getProfile(requestHead, "login:");
		
		String sayHelloContent = profile.get(LOGIN_SECRETARY_SAY_HELLO_CONTENT);
		if (sayHelloContent != null && !sayHelloContent.trim().isEmpty()) {
			this.adminOfficialService.sendSecretaryMessage(requestHead, SendSecretaryMessageRequest.newBuilder()
					.addUserId(userId)
					.setSendMsg(OfficialProtos.OfficialMessage.newBuilder()
							.setMsgSeq(-1L)
							.setMsgTime(0)
							.setIsFromUser(false)
							.setText(OfficialProtos.OfficialMessage.Text.newBuilder()
									.setContent(sayHelloContent.trim())
									.build())
							.build())
					.build());
		}
		
		return Futures.immediateFuture(LoginAutoResponse.newBuilder()
				.setResult(LoginAutoResponse.Result.SUCC)
				.setSessionKey(createSessionKeyResponse.getSessionKey())
				.setUser(user)
				.addAllRefTeam(getUserByMobileNoUniqueResponse.getRefTeamList())
				.addAllRefPosition(getUserByMobileNoUniqueResponse.getRefPositionList())
				.addAllRefLevel(getUserByMobileNoUniqueResponse.getRefLevelList())
				.setCompanyId(companyId)
				.build());
	}

	@Override
	@AsyncImpl
	public ListenableFuture<EmptyResponse> logout(AnonymousHead head, EmptyRequest request) {
		return ServiceUtil.EMPTY_RESPONSE_IMMEDIATE_FUTURE;
	}

	@Override
	public ListenableFuture<EmptyResponse> logout(RequestHead head, EmptyRequest request) {
		sessionService.deleteSessionKey(head, ServiceUtil.EMPTY_REQUEST);
		return ServiceUtil.EMPTY_RESPONSE_IMMEDIATE_FUTURE;
	}

	@Override
	public ListenableFuture<GetLoginSmsCodeResponse> getLoginSmsCode(AdminHead head, GetLoginSmsCodeRequest request) {
		if (!head.hasCompanyId()) {
			return Futures.immediateFuture(GetLoginSmsCodeResponse.newBuilder().build());
		}
		final long companyId = head.getCompanyId();
		final long userId = request.getUserId();
		
		LoginDAOProtos.SmsCodeList smsCodeList;
		Jedis jedis = jedisPool.getResource();
		try {
			smsCodeList = LoginCache.getLoginSmsCode(jedis, companyId, Collections.<Long>singleton(userId)).get(userId);
		} finally {
			jedis.close();
		}
		
		if (smsCodeList == null) {
			return Futures.immediateFuture(GetLoginSmsCodeResponse.newBuilder().build());
		} else {
			return Futures.immediateFuture(GetLoginSmsCodeResponse.newBuilder()
					.addAllSmsCode(smsCodeList.getSmsCodeList())
					.build());
		}
	}

	@Override
	public ListenableFuture<SendRegisterSmsCodeResponse> sendRegisterSmsCode(AnonymousHead head, SendRegisterSmsCodeRequest request) {
		if (!MobileNoUtil.isValid(request.getMobileNo())) {
			return Futures.immediateFuture(SendRegisterSmsCodeResponse.newBuilder()
					.setResult(SendRegisterSmsCodeResponse.Result.FAIL_MOBILE_NO_INVALID)
					.setFailText("手机号格式不正确")
					.build());
		}
		
		final String mobileNo = MobileNoUtil.adjustMobileNo(request.getMobileNo());
		final CompanyProtos.Company company = this.doVerifyCompanyKey(head, request.getCompanyKey());
		
		if (company == null) {
			logger.warn("cannot find company : " + request.getCompanyKey());			
			return Futures.immediateFuture(SendRegisterSmsCodeResponse.newBuilder()
					.setResult(SendRegisterSmsCodeResponse.Result.FAIL_UNKNOWN)
					.setFailText("未找到公司信息")
					.build());
		}
		
		final AnonymousHead newHead = head.toBuilder().setCompanyId(company.getCompanyId()).build();
		
		GetUserByMobileNoUniqueResponse getUserByMobileNoUniqueResponse = 
				Futures.getUnchecked(this.adminUserService.getUserByMobileNoUnique(newHead,
						GetUserByMobileNoUniqueRequest.newBuilder()
						.addMobileNo(mobileNo)
						.build()));
		
		for (UserProtos.User user : getUserByMobileNoUniqueResponse.getUserList()) {
			if (user.getBase().getState() != UserProtos.UserBase.State.DELETE 
					&& user.getBase().getMobileNoList().contains(mobileNo)
					) {
				return Futures.immediateFuture(SendRegisterSmsCodeResponse.newBuilder()
						.setResult(SendRegisterSmsCodeResponse.Result.FAIL_SEND_FAIL)
						.setFailText("该手机号已被其他用户注册")
						.setCompanyId(company.getCompanyId())
						.build());
			}
		}
		
		final ProfileManager.Profile profile = this.profileManager.getProfile(newHead, "login:");
		
		final long companyId = company.getCompanyId();
		final int now = (int) (System.currentTimeMillis() / 1000L);
		final int code;
		final boolean isCodeFake;
		if (this.fakeSmsCode || profile.get(FAKE_SMS_CODE_ALL)) {
			code = profile.get(FAKE_SMS_CODE_VALUE);
			isCodeFake = true;
		} else {
			String fakeSmsCodeMobileNo = profile.get(FAKE_SMS_CODE_MOBILE_NO);
			if (fakeSmsCodeMobileNo != null && MOBILE_NO_SPLITTER.splitToList(fakeSmsCodeMobileNo).contains(mobileNo)) {
				code = profile.get(FAKE_SMS_CODE_VALUE);
				isCodeFake = true;
			} else {
				code = 100000 + rand.nextInt(900000);
				isCodeFake = false;
			}
		}
		
		Jedis jedis = jedisPool.getResource();
		try {
			LoginDAOProtos.SmsCodeList.Builder builder = LoginDAOProtos.SmsCodeList.newBuilder();
			builder.addSmsCode(LoginProtos.SmsCode.newBuilder()
							.setSmsCode(code)
							.setCreateTime(now)
							.setMobileNo(mobileNo)
							.build());
			
			LoginDAOProtos.SmsCodeList smsCodeList = LoginCache.getRegisterSmsCode(jedis, companyId, Collections.singleton(mobileNo)).get(mobileNo);
			if (smsCodeList != null) {
				for (LoginProtos.SmsCode smsCode : smsCodeList.getSmsCodeList()) {
					if (mobileNo.equals(smsCode.getMobileNo()) && now - smsCode.getCreateTime() < 60) {
						return Futures.immediateFuture(SendRegisterSmsCodeResponse.newBuilder()
								.setResult(SendRegisterSmsCodeResponse.Result.FAIL_SEND_LIMIT_EXCEEDED)
								.setFailText("短信验证码发送太频繁")
								.setCompanyId(company.getCompanyId())
								.build());
					}
					
					if (builder.getSmsCodeCount() < 3) {
						builder.addSmsCode(smsCode);
					}
				}
			}
			
			smsCodeList = builder.build();
			
			LoginCache.setRegisterSmsCode(jedis, companyId, Collections.<String, LoginDAOProtos.SmsCodeList> singletonMap(mobileNo, smsCodeList));
		} finally {
			jedis.close();
		}
		
		if (isCodeFake) {
			return Futures.immediateFuture(SendRegisterSmsCodeResponse.newBuilder()
					.setResult(SendRegisterSmsCodeResponse.Result.SUCC)
					.setCompanyId(company.getCompanyId())
					.build());
		}
		
		String smsText = profile.get(REGISTER_SMS_TEXT);
		
		smsText = smsText.replace("${sms_code}", String.valueOf(code));
		
		SendSmsResponse sendSmsResponse = Futures.getUnchecked(
				this.externalService.sendSms(newHead, SendSmsRequest.newBuilder()
				.addMobileNo(mobileNo)
				.setSmsText(smsText)
				.build()));
		
		if (sendSmsResponse.getResult() == SendSmsResponse.Result.SUCC) {
			return Futures.immediateFuture(SendRegisterSmsCodeResponse.newBuilder()
					.setResult(SendRegisterSmsCodeResponse.Result.SUCC)
					.setCompanyId(company.getCompanyId())
					.build());
		} else {
			return Futures.immediateFuture(SendRegisterSmsCodeResponse.newBuilder()
					.setResult(SendRegisterSmsCodeResponse.Result.FAIL_SEND_FAIL)
					.setFailText("发送短信验证码失败")
					.setCompanyId(company.getCompanyId())
					.build());
		}
	}

	@Override
	public ListenableFuture<RegisterBySmsCodeResponse> registerBySmsCode(AnonymousHead head, RegisterBySmsCodeRequest request) {
		if (!MobileNoUtil.isValid(request.getMobileNo())) {
			return Futures.immediateFuture(RegisterBySmsCodeResponse.newBuilder()
					.setResult(RegisterBySmsCodeResponse.Result.FAIL_MOBILE_NO_INVALID)
					.setFailText("手机号格式错误")
					.build());
		}
		
		final String mobileNo = MobileNoUtil.adjustMobileNo(request.getMobileNo());
		final CompanyProtos.Company company = this.doVerifyCompanyKey(head, request.getCompanyKey());
		
		if (company == null) {
			logger.warn("cannot find company : " + request.getCompanyKey() + ", " + mobileNo);			
			return Futures.immediateFuture(RegisterBySmsCodeResponse.newBuilder()
					.setResult(RegisterBySmsCodeResponse.Result.FAIL_UNKNOWN)
					.setFailText("未找到公司信息")
					.build());
		}
		
		final AnonymousHead newHead = head.toBuilder().setCompanyId(company.getCompanyId()).build();
		final ProfileManager.Profile profile = this.profileManager.getProfile(newHead, "login:");
		
		final long companyId = company.getCompanyId();
		final int now = (int) (System.currentTimeMillis() / 1000L);
		
		Jedis jedis = jedisPool.getResource();
		try {
			LoginDAOProtos.SmsCodeList smsCodeList = LoginCache.getRegisterSmsCode(jedis, companyId, Collections.<String> singleton(mobileNo)).get(mobileNo);
			if (smsCodeList == null || smsCodeList.getSmsCodeCount() <= 0) {
				return Futures.immediateFuture(RegisterBySmsCodeResponse.newBuilder()
						.setResult(RegisterBySmsCodeResponse.Result.FAIL_SMS_CODE_EXPIRED)
						.setFailText("验证码已过期，请重新发送")
						.setCompanyId(company.getCompanyId())
						.build());
			}
			
			LoginDAOProtos.SmsCodeList.Builder builder = LoginDAOProtos.SmsCodeList.newBuilder();
			
			boolean hasMobileNo = false;
			boolean isCodeEquals = false;
			for (LoginProtos.SmsCode smsCode : smsCodeList.getSmsCodeList()) {
				if (now - smsCode.getCreateTime() < SMS_CODE_EXPIRED_SECOND) {
					if (mobileNo.equals(smsCode.getMobileNo())) {
						hasMobileNo = true;
						if (request.getSmsCode() == smsCode.getSmsCode()) {
							isCodeEquals = true;
							continue;
						}
					}
						
					builder.addSmsCode(smsCode);
				}
			}
		
			if (!isCodeEquals) {
				if (hasMobileNo) {
					return Futures.immediateFuture(RegisterBySmsCodeResponse.newBuilder()
							.setResult(RegisterBySmsCodeResponse.Result.FAIL_SMS_CODE_INVALID)
							.setFailText("短信验证码错误")
							.setCompanyId(company.getCompanyId())
							.build());
				} else {
					return Futures.immediateFuture(RegisterBySmsCodeResponse.newBuilder()
							.setResult(RegisterBySmsCodeResponse.Result.FAIL_SMS_CODE_EXPIRED)
							.setFailText("短信验证码已过期，请重新发送")
							.setCompanyId(company.getCompanyId())
							.build());
				}
			}
			
			LoginCache.setRegisterSmsCode(jedis, companyId, Collections.<String, LoginDAOProtos.SmsCodeList> singletonMap(mobileNo, builder.build()));
			
		} finally {
			jedis.close();
		}
		
		RegisterUserRequest.Builder registerUserRequestBuilder = RegisterUserRequest.newBuilder();
		registerUserRequestBuilder.setUserName(request.getUserName());
		if (request.hasGender()) {
			registerUserRequestBuilder.setGender(registerUserRequestBuilder.getGender());
		}
		if (request.hasEmail()) {
			registerUserRequestBuilder.setEmail(request.getEmail());
		}
		
		registerUserRequestBuilder.addAllTeam(request.getTeamList());
		if (request.hasPosition()) {
			registerUserRequestBuilder.setPosition(request.getPosition());
		}
		registerUserRequestBuilder.setMobileNo(request.getMobileNo());
		registerUserRequestBuilder.addAllExtsName(request.getExtsNameList());
		registerUserRequestBuilder.addAllExtsValue(request.getExtsValueList());
		
		if (request.hasLevel()) {
			registerUserRequestBuilder.setLevel(request.getLevel());
		}
		if (request.hasPhoneNo()) {
			registerUserRequestBuilder.setPhoneNo(request.getPhoneNo());
		}
		registerUserRequestBuilder.setState(profile.get(REGISTER_USER_STATE));
		
		RegisterUserResponse registerUserResponse = Futures.getUnchecked(
				this.adminUserService.registerUser(newHead, registerUserRequestBuilder.build()));
		
		final RegisterBySmsCodeResponse.Result result;
		switch (registerUserResponse.getResult()) {
			case SUCC:
				result = RegisterBySmsCodeResponse.Result.SUCC;
				break;
			case FAIL_NAME_INVALID:
				result = RegisterBySmsCodeResponse.Result.FAIL_NAME_INVALID;
				break;
			case FAIL_EMAIL_INVALID:
				result = RegisterBySmsCodeResponse.Result.FAIL_EMAIL_INVALID;
				break;
			case FAIL_TEAM_INVALID:
				result = RegisterBySmsCodeResponse.Result.FAIL_TEAM_INVALID;
				break;
			case FAIL_POSITION_INVALID:
				result = RegisterBySmsCodeResponse.Result.FAIL_POSITION_INVALID;
				break;
			case FAIL_MOBILE_NO_INVALID:
				result = RegisterBySmsCodeResponse.Result.FAIL_MOBILE_NO_INVALID;
				break;
			case FAIL_LEVEL_INVALID:
				result = RegisterBySmsCodeResponse.Result.FAIL_LEVEL_INVALID;
				break;
			case FAIL_UNKNOWN:
				result = RegisterBySmsCodeResponse.Result.FAIL_UNKNOWN;
				break;
			default:
				result = RegisterBySmsCodeResponse.Result.FAIL_UNKNOWN;
				break;
		}
		
		RegisterBySmsCodeResponse.Builder registerBySmsCodeResponseBuilder = RegisterBySmsCodeResponse.newBuilder();
		registerBySmsCodeResponseBuilder.setResult(result);
		if (registerUserResponse.hasFailText()) {
			registerBySmsCodeResponseBuilder.setFailText(registerUserResponse.getFailText());
		}
		registerBySmsCodeResponseBuilder.setCompanyId(companyId);
		
		return Futures.immediateFuture(registerBySmsCodeResponseBuilder.build());
	}
	
	private final ConcurrentMap<String, WebLoginInfoHolder> webLoginInfoHolderMap = new ConcurrentHashMap<String, WebLoginInfoHolder>();

	@Override
	public ListenableFuture<WebLoginByTokenResponse> webLoginByToken(AnonymousHead head, WebLoginByTokenRequest request) {
		final String token = request.getToken();
		if (token.isEmpty()) {
			return Futures.immediateFuture(WebLoginByTokenResponse.newBuilder()
					.setResult(WebLoginByTokenResponse.Result.FAIL_TOKEN_INVALID)
					.setFailText("token不能为空")
					.build());
		}
		try {
			Base64.getUrlDecoder().decode(token);
		} catch (IllegalArgumentException e) {
			return Futures.immediateFuture(WebLoginByTokenResponse.newBuilder()
					.setResult(WebLoginByTokenResponse.Result.FAIL_TOKEN_INVALID)
					.setFailText("token格式不正确")
					.build());
		}
		
		LoginDAOProtos.WebLoginInfo webLoginInfo = LoginDAOProtos.WebLoginInfo.newBuilder()
				.setToken(token)
				.setRemoteHost(head.getNetwork().getRemoteHost())
				.setUserAgent(head.getWebLogin().getUserAgent())
				.setCreateTime((int) (System.currentTimeMillis() / 1000L))
				.build();
		
		SettableFuture<WebLoginByTokenResponse> responseFuture = SettableFuture.create();
		
		this.webLoginInfoHolderMap.put(token, new WebLoginInfoHolder(head, webLoginInfo, responseFuture));
		
		Jedis jedis = this.jedisPool.getResource();
		try {
			LoginCache.setWebLoginInfo(jedis, Collections.singletonMap(token, webLoginInfo));
		} finally {
			jedis.close();
		}
		
		return responseFuture;
	}

	@Override
	public ListenableFuture<EmptyResponse> webLogout(RequestHead head, EmptyRequest request) {
		this.sessionService.deleteWebLoginSessionKey(head, request);
		return ServiceUtil.EMPTY_RESPONSE_IMMEDIATE_FUTURE;
	}

	@Override
	public ListenableFuture<GetWebLoginByTokenResponse> getWebLoginByToken(RequestHead head, GetWebLoginByTokenRequest request) {
		final String token = request.getToken();
		if (token.isEmpty()) {
			return Futures.immediateFuture(GetWebLoginByTokenResponse.newBuilder()
					.setResult(GetWebLoginByTokenResponse.Result.FAIL_TOKEN_INVALID)
					.setFailText("token不能为空")
					.build());
		}
		try {
			Base64.getUrlDecoder().decode(token);
		} catch (IllegalArgumentException e) {
			return Futures.immediateFuture(GetWebLoginByTokenResponse.newBuilder()
					.setResult(GetWebLoginByTokenResponse.Result.FAIL_TOKEN_INVALID)
					.setFailText("token格式不正确")
					.build());
		}
		
		final LoginDAOProtos.WebLoginInfo webLoginInfo;
		Jedis jedis = this.jedisPool.getResource();
		try {
			webLoginInfo = LoginCache.getWebLoginInfo(jedis, Collections.singleton(token)).get(token);
		} finally {
			jedis.close();
		}
		
		if (webLoginInfo == null || (int) (System.currentTimeMillis() / 1000L) - webLoginInfo.getCreateTime() > 60) {
			return Futures.immediateFuture(GetWebLoginByTokenResponse.newBuilder()
					.setResult(GetWebLoginByTokenResponse.Result.FAIL_TOKEN_INVALID)
					.setFailText("token已过期，请重新登录")
					.build());
		}
		
		return Futures.immediateFuture(GetWebLoginByTokenResponse.newBuilder()
				.setResult(GetWebLoginByTokenResponse.Result.SUCC)
				.setRemoteHost(webLoginInfo.getRemoteHost())
				.setUserAgent(webLoginInfo.getUserAgent())
				.build());
	}

	@Override
	public ListenableFuture<NotifyWebLoginByTokenResponse> notifyWebLoginByToken(RequestHead head, NotifyWebLoginByTokenRequest request) {
		final String token = request.getToken();
		if (token.isEmpty()) {
			return Futures.immediateFuture(NotifyWebLoginByTokenResponse.newBuilder()
					.setResult(NotifyWebLoginByTokenResponse.Result.FAIL_TOKEN_INVALID)
					.setFailText("token不能为空")
					.build());
		}
		try {
			Base64.getUrlDecoder().decode(token);
		} catch (IllegalArgumentException e) {
			return Futures.immediateFuture(NotifyWebLoginByTokenResponse.newBuilder()
					.setResult(NotifyWebLoginByTokenResponse.Result.FAIL_TOKEN_INVALID)
					.setFailText("token格式不正确")
					.build());
		}
		
		final LoginDAOProtos.WebLoginInfo webLoginInfo;
		Jedis jedis = this.jedisPool.getResource();
		try {
			webLoginInfo = LoginCache.getWebLoginInfo(jedis, Collections.singleton(token)).get(token);
		} finally {
			jedis.close();
		}
		
		if (webLoginInfo == null || (int) (System.currentTimeMillis() / 1000L) - webLoginInfo.getCreateTime() > 60) {
			return Futures.immediateFuture(NotifyWebLoginByTokenResponse.newBuilder()
					.setResult(NotifyWebLoginByTokenResponse.Result.FAIL_TOKEN_INVALID)
					.setFailText("token已过期，请重新登录")
					.build());
		}
		
		this.notifyWebLoginByTokenTaskHandler.executeAll(LoginDAOProtos.NotifyWebLoginData.newBuilder()
				.setToken(token)
				.setSession(head.getSession())
				.build());
		
		return Futures.immediateFuture(NotifyWebLoginByTokenResponse.newBuilder()
				.setResult(NotifyWebLoginByTokenResponse.Result.SUCC)
				.build());
	}
	
	private final class NotifyWebLoginByTokenTask implements TaskManager.TaskPrototype<LoginDAOProtos.NotifyWebLoginData> {

		@Override
		public void execute(@Nullable NotifyWebLoginData data) {
			if (data == null) {
				return;
			}
			WebLoginInfoHolder holder = LoginServiceImpl.this.webLoginInfoHolderMap.remove(data.getToken());
			if (holder == null) {
				return;
			}
			
			Jedis jedis = LoginServiceImpl.this.jedisPool.getResource();
			try {
				LoginCache.delWebLoginInfo(jedis, Collections.singleton(data.getToken()));
			} finally {
				jedis.close();
			}
			
			final RequestHead head = ServiceUtil.toRequestHead(holder.anonymousHead, data.getSession());
			
			// get user info 
			GetUserByIdResponse getUserResponse = 
					Futures.getUnchecked(LoginServiceImpl.this.adminUserService.getUserById(head,
							GetUserByIdRequest.newBuilder()
							.addUserId(head.getSession().getUserId())
							.build()));
			
			UserProtos.User user = null;
			for (UserProtos.User u : getUserResponse.getUserList()) {
				if (u.getBase().getUserId() == head.getSession().getUserId()) {
					user = u;
					break;
				}
			}
			
			if (user == null || user.getBase().getState() != UserProtos.UserBase.State.NORMAL) {
				holder.responseFuture.set(WebLoginByTokenResponse.newBuilder()
						.setResult(WebLoginByTokenResponse.Result.FAIL_UNKNOWN)
						.setFailText("登录失败，用户信息错误")
						.build());
			}
			
			// create session
			CreateWebLoginSessionKeyResponse createSessionResponse = 
					Futures.getUnchecked(LoginServiceImpl.this.sessionService.createWebLoginSessionKey(head, ServiceUtil.EMPTY_REQUEST));
			
			holder.responseFuture.set(WebLoginByTokenResponse.newBuilder()
					.setResult(WebLoginByTokenResponse.Result.SUCC)
					.setWebLoginSessionKey(createSessionResponse.getWebLoginSessionKey())
					.setUser(user)
					.addAllRefTeam(getUserResponse.getRefTeamList())
					.addAllRefPosition(getUserResponse.getRefPositionList())
					.addAllRefLevel(getUserResponse.getRefLevelList())
					.setCompanyId(head.getSession().getCompanyId())
					.build());
		}
	}
	
	private final class WebLoginExpireCheckTask implements Runnable {

		@Override
		public void run() {
			int now = (int) (System.currentTimeMillis() / 1000L);
			
			Map<String, WebLoginInfoHolder> expireMap = new TreeMap<String, WebLoginInfoHolder>();
			for (Entry<String, WebLoginInfoHolder> entry : LoginServiceImpl.this.webLoginInfoHolderMap.entrySet()) {
				if (now - entry.getValue().webLoginInfo.getCreateTime() > 55) {
					expireMap.put(entry.getKey(), entry.getValue());
				}
			}
			
			if (expireMap.isEmpty()) {
				return;
			}
			
			List<String> expireTokenList = new ArrayList<String>();
			for (Entry<String, WebLoginInfoHolder> entry : expireMap.entrySet()) {
				if (LoginServiceImpl.this.webLoginInfoHolderMap.remove(entry.getKey(), entry.getValue())) {
					expireTokenList.add(entry.getKey());
					entry.getValue().responseFuture.set(WebLoginByTokenResponse.newBuilder()
					.setResult(WebLoginByTokenResponse.Result.FAIL_LOGIN_EXPIRE)
					.setFailText("登录过期，请重新生成token继续登录")
					.build());
				}
			}
			
			if (!expireTokenList.isEmpty()) {
				Jedis jedis = LoginServiceImpl.this.jedisPool.getResource();
				try {
					LoginCache.delWebLoginInfo(jedis, expireTokenList);
				} finally {
					jedis.close();
				}
			}
		}
		
	}
	
	private static final class WebLoginInfoHolder {
		final AnonymousHead anonymousHead;
		final LoginDAOProtos.WebLoginInfo webLoginInfo;
		final SettableFuture<WebLoginByTokenResponse> responseFuture;
		
		WebLoginInfoHolder(AnonymousHead anonymousHead, LoginDAOProtos.WebLoginInfo webLoginInfo, SettableFuture<WebLoginByTokenResponse> responseFuture) {
			this.anonymousHead = anonymousHead;
			this.webLoginInfo = webLoginInfo;
			this.responseFuture = responseFuture;
		}
	}

}
