package com.weizhu.service.user.base;

import java.util.Collection;
import java.util.Map;

import redis.clients.jedis.Jedis;

import com.weizhu.common.jedis.JedisValueCacheEx;
import com.weizhu.proto.UserProtos;

public class UserBaseCache {

	private static final JedisValueCacheEx<Long, UserProtos.UserBase> USER_BASE_CACHE = 
			JedisValueCacheEx.create("user:base:", UserProtos.UserBase.PARSER);
	
	public static Map<Long, UserProtos.UserBase> getUserBase(Jedis jedis, long companyId, Collection<Long> userIds) {
		return USER_BASE_CACHE.get(jedis, companyId, userIds);
	}
	
	public static Map<Long, UserProtos.UserBase> getUserBase(Jedis jedis, long companyId, Collection<Long> userIds, Collection<Long> noCacheUserIds) {
		return USER_BASE_CACHE.get(jedis, companyId, userIds, noCacheUserIds);
	}
	
	public static void setUserBase(Jedis jedis, long companyId, Map<Long, UserProtos.UserBase> userBaseMap) {
		USER_BASE_CACHE.set(jedis, companyId, userBaseMap);
	}
	
	public static void setUserBase(Jedis jedis, long companyId, Collection<Long> userIds, Map<Long, UserProtos.UserBase> userBaseMap) {
		USER_BASE_CACHE.set(jedis, companyId, userIds, userBaseMap);
	}
	
	public static void delUserBase(Jedis jedis, long companyId, Collection<Long> userIds) {
		USER_BASE_CACHE.del(jedis, companyId, userIds);
	}
}
