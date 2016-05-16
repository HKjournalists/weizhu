package com.weizhu.service.credits;

import java.util.Collection;
import java.util.Map;

import redis.clients.jedis.Jedis;

import com.weizhu.common.jedis.JedisValueCacheEx;
import com.weizhu.proto.CreditsProtos;

public class CreditsCache {
	
	private static final JedisValueCacheEx<Long, CreditsProtos.Credits> CREDITS_CACHE =
			JedisValueCacheEx.create("credits:", CreditsProtos.Credits.PARSER);
	
	public static Map<Long, CreditsProtos.Credits> getCredits(Jedis jedis, long companyId, Collection<Long> userIds) {
		return CREDITS_CACHE.get(jedis, companyId, userIds);
	}
	
	public static Map<Long, CreditsProtos.Credits> getCredits(Jedis jedis, long companyId, Collection<Long> userIds, Collection<Long> noCacheUserIds) {
		return CREDITS_CACHE.get(jedis, companyId, userIds, noCacheUserIds);
	}
	
	public static void setCredits(Jedis jedis, long companyId, Map<Long, CreditsProtos.Credits> creditsMap) {
		CREDITS_CACHE.set(jedis, companyId, creditsMap);
	}
	
	public static void setCredits(Jedis jedis, long companyId, Collection<Long> userIds, Map<Long, CreditsProtos.Credits> creditsMap) {
		CREDITS_CACHE.set(jedis, companyId, userIds, creditsMap);
	}
	
	public static void delCredits(Jedis jedis, long companyId, Collection<Long> userIds) {
		CREDITS_CACHE.del(jedis, companyId, userIds);
	}
}
