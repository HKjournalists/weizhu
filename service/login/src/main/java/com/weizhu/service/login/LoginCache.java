package com.weizhu.service.login;

import java.util.Collection;
import java.util.Map;

import redis.clients.jedis.Jedis;

import com.weizhu.common.jedis.JedisValueCache;
import com.weizhu.common.jedis.JedisValueCacheEx;

public class LoginCache {
	
	private static final JedisValueCacheEx<Long, LoginDAOProtos.SmsCodeList> LOGIN_SMS_CODE_CACHE = 
			JedisValueCacheEx.create("login:login_sms_code:", LoginDAOProtos.SmsCodeList.PARSER);
	
	public static Map<Long, LoginDAOProtos.SmsCodeList> getLoginSmsCode(Jedis jedis, long companyId, Collection<Long> userIds) {
		return LOGIN_SMS_CODE_CACHE.get(jedis, companyId, userIds);
	}
	
	public static Map<Long, LoginDAOProtos.SmsCodeList> getLoginSmsCode(Jedis jedis, long companyId, Collection<Long> userIds, Collection<Long> noCacheUserIds) {
		return LOGIN_SMS_CODE_CACHE.get(jedis, companyId, userIds, noCacheUserIds);
	}
	
	public static void setLoginSmsCode(Jedis jedis, long companyId, Map<Long, LoginDAOProtos.SmsCodeList> smsCodeMap) {
		LOGIN_SMS_CODE_CACHE.set(jedis, companyId, smsCodeMap);
	}
	
	public static void setLoginSmsCode(Jedis jedis, long companyId, Collection<Long> userIds, Map<Long, LoginDAOProtos.SmsCodeList> smsCodeMap) {
		LOGIN_SMS_CODE_CACHE.set(jedis, companyId, userIds, smsCodeMap);
	}
	
	public static void delLoginSmsCode(Jedis jedis, long companyId, Collection<Long> userIds) {
		LOGIN_SMS_CODE_CACHE.del(jedis, companyId, userIds);
	}
	
	private static final JedisValueCacheEx<String, LoginDAOProtos.SmsCodeList> REGISTER_SMS_CODE_CACHE = 
			JedisValueCacheEx.create("login:register_sms_code:", LoginDAOProtos.SmsCodeList.PARSER);
	
	public static Map<String, LoginDAOProtos.SmsCodeList> getRegisterSmsCode(Jedis jedis, long companyId, Collection<String> mobileNos) {
		return REGISTER_SMS_CODE_CACHE.get(jedis, companyId, mobileNos);
	}
	
	public static Map<String, LoginDAOProtos.SmsCodeList> getRegisterSmsCode(Jedis jedis, long companyId, Collection<String> mobileNos, Collection<String> noCacheMobileNos) {
		return REGISTER_SMS_CODE_CACHE.get(jedis, companyId, mobileNos, noCacheMobileNos);
	}
	
	public static void setRegisterSmsCode(Jedis jedis, long companyId, Map<String, LoginDAOProtos.SmsCodeList> mobileNoSmsCodeMap) {
		REGISTER_SMS_CODE_CACHE.set(jedis, companyId, mobileNoSmsCodeMap);
	}
	
	public static void setRegisterSmsCode(Jedis jedis, long companyId, Collection<String> mobileNos, Map<String, LoginDAOProtos.SmsCodeList> mobileNoSmsCodeMap) {
		REGISTER_SMS_CODE_CACHE.set(jedis, companyId, mobileNos, mobileNoSmsCodeMap);
	}
	
	public static void delRegisterSmsCode(Jedis jedis, long companyId, Collection<String> mobileNos) {
		REGISTER_SMS_CODE_CACHE.del(jedis, companyId, mobileNos);
	}
	
	private static final JedisValueCache<String, LoginDAOProtos.WebLoginInfo> WEB_LOGIN_INFO_CACHE = 
			JedisValueCache.create("login:web_login_info:", LoginDAOProtos.WebLoginInfo.PARSER);
	
	public static Map<String, LoginDAOProtos.WebLoginInfo> getWebLoginInfo(Jedis jedis, Collection<String> tokens) {
		return WEB_LOGIN_INFO_CACHE.get(jedis, tokens);
	}
	
	public static Map<String, LoginDAOProtos.WebLoginInfo> getWebLoginInfo(Jedis jedis, Collection<String> tokens, Collection<String> noCacheTokens) {
		return WEB_LOGIN_INFO_CACHE.get(jedis, tokens, noCacheTokens);
	}
	
	public static void setWebLoginInfo(Jedis jedis, Map<String, LoginDAOProtos.WebLoginInfo> webLoginInfoMap) {
		WEB_LOGIN_INFO_CACHE.set(jedis, webLoginInfoMap);
	}
	
	public static void setWebLoginInfo(Jedis jedis, Collection<String> tokens, Map<String, LoginDAOProtos.WebLoginInfo> webLoginInfoMap) {
		WEB_LOGIN_INFO_CACHE.set(jedis, tokens, webLoginInfoMap);
	}
	
	public static void delWebLoginInfo(Jedis jedis, Collection<String> tokens) {
		WEB_LOGIN_INFO_CACHE.del(jedis, tokens);
	}
	
}
