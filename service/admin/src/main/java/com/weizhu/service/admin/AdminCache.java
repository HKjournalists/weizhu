package com.weizhu.service.admin;

import java.util.Collection;
import java.util.Map;
import redis.clients.jedis.Jedis;

import com.weizhu.common.jedis.JedisValueCache;
import com.weizhu.proto.AdminProtos;

public class AdminCache {
	
	private static final JedisValueCache<Long, AdminProtos.AdminSessionData> ADMIN_SESSION_CACHE = 
			JedisValueCache.create("admin:session:", AdminProtos.AdminSessionData.PARSER);
	private static final JedisValueCache<Long, AdminProtos.Admin> ADMIN_CACHE = 
			JedisValueCache.create("admin:info:", AdminProtos.Admin.PARSER);
	private static final JedisValueCache<Integer, AdminProtos.Role> ROLE_CACHE = 
			JedisValueCache.create("admin:role:", AdminProtos.Role.PARSER);
	
	public static Map<Long, AdminProtos.AdminSessionData> getAdminSession(Jedis jedis, Collection<Long> adminIds) {
		return ADMIN_SESSION_CACHE.get(jedis, adminIds);
	}
	
	public static Map<Long, AdminProtos.AdminSessionData> getAdminSession(Jedis jedis, Collection<Long> adminIds, Collection<Long> noCacheAdminIds) {
		return ADMIN_SESSION_CACHE.get(jedis, adminIds, noCacheAdminIds);
	}
	
	public static void setAdminSession(Jedis jedis, Map<Long, AdminProtos.AdminSessionData> adminSessionDataMap) {
		ADMIN_SESSION_CACHE.set(jedis, adminSessionDataMap);
	}
	
	public static void setAdminSession(Jedis jedis, Collection<Long> adminIds, Map<Long, AdminProtos.AdminSessionData> adminSessionDataMap) {
		ADMIN_SESSION_CACHE.set(jedis, adminIds, adminSessionDataMap);
	}
	
	public static void delAdminSession(Jedis jedis, Collection<Long> adminIds) {
		ADMIN_SESSION_CACHE.del(jedis, adminIds);
	}
	
	public static Map<Long, AdminProtos.Admin> getAdmin(Jedis jedis, Collection<Long> adminIds) {
		return ADMIN_CACHE.get(jedis, adminIds);
	}
	
	public static Map<Long, AdminProtos.Admin> getAdmin(Jedis jedis, Collection<Long> adminIds, Collection<Long> noCacheAdminIds) {
		return ADMIN_CACHE.get(jedis, adminIds, noCacheAdminIds);
	}
	
	public static void setAdmin(Jedis jedis, Map<Long, AdminProtos.Admin> adminMap) {
		ADMIN_CACHE.set(jedis, adminMap);
	}
	
	public static void setAdmin(Jedis jedis, Collection<Long> adminIds, Map<Long, AdminProtos.Admin> adminMap) {
		ADMIN_CACHE.set(jedis, adminIds, adminMap);
	}
	
	public static void delAdmin(Jedis jedis, Collection<Long> adminIds) {
		ADMIN_CACHE.del(jedis, adminIds);
	}
	
	public static Map<Integer, AdminProtos.Role> getRole(Jedis jedis, Collection<Integer> roleIds) {
		return ROLE_CACHE.get(jedis, roleIds);
	}
	
	public static Map<Integer, AdminProtos.Role> getRole(Jedis jedis, Collection<Integer> roleIds, Collection<Integer> noCacheRoleIds) {
		return ROLE_CACHE.get(jedis, roleIds, noCacheRoleIds);
	}
	
	public static void setRole(Jedis jedis, Map<Integer, AdminProtos.Role> roleMap) {
		ROLE_CACHE.set(jedis, roleMap);
	}
	
	public static void setRole(Jedis jedis, Collection<Integer> roleIds, Map<Integer, AdminProtos.Role> roleMap) {
		ROLE_CACHE.set(jedis, roleIds, roleMap);
	}
	
	public static void delRole(Jedis jedis, Collection<Integer> roleIds) {
		ROLE_CACHE.del(jedis, roleIds);
	}
	
	
	private static final JedisValueCache<Long, AdminDAOProtos.ForgotToken> ADMIN_FORGOT_TOKEN_CACHE = 
			JedisValueCache.create("admin:forgot_token:", AdminDAOProtos.ForgotToken.PARSER);
	
	public static Map<Long, AdminDAOProtos.ForgotToken> getAdminForgotToken(Jedis jedis, Collection<Long> adminIds) {
		return ADMIN_FORGOT_TOKEN_CACHE.get(jedis, adminIds);
	}
	
	public static void setAdminForgotToken(Jedis jedis, Map<Long, AdminDAOProtos.ForgotToken> forgotTokenMap) {
		ADMIN_FORGOT_TOKEN_CACHE.set(jedis, forgotTokenMap);
	}
	
	public static void delAdminForgotToken(Jedis jedis, Collection<Long> adminIds) {
		ADMIN_FORGOT_TOKEN_CACHE.del(jedis, adminIds);
	}
}
