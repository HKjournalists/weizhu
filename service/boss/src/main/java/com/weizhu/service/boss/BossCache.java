package com.weizhu.service.boss;

import java.util.Collection;
import java.util.Map;

import com.weizhu.common.jedis.JedisValueCache;
import com.weizhu.proto.BossProtos;

import redis.clients.jedis.Jedis;

public class BossCache {
	
	private static final JedisValueCache<String, BossProtos.BossSessionData> BOSS_SESSION_CACHE = 
			JedisValueCache.create("boss:session:", BossProtos.BossSessionData.PARSER);
	
	public static Map<String, BossProtos.BossSessionData> getBossSession(Jedis jedis, Collection<String> bossIds) {
		return BOSS_SESSION_CACHE.get(jedis, bossIds);
	}
	
	public static Map<String, BossProtos.BossSessionData> getBossSession(Jedis jedis, Collection<String> bossIds, Collection<String> noCacheBossIds) {
		return BOSS_SESSION_CACHE.get(jedis, bossIds, noCacheBossIds);
	}
	
	public static void setBossSession(Jedis jedis, Map<String, BossProtos.BossSessionData> bossSessionDataMap) {
		BOSS_SESSION_CACHE.set(jedis, bossSessionDataMap);
	}
	
	public static void setBossSession(Jedis jedis, Collection<String> bossIds, Map<String, BossProtos.BossSessionData> bossSessionDataMap) {
		BOSS_SESSION_CACHE.set(jedis, bossIds, bossSessionDataMap);
	}
	
	public static void delBossSession(Jedis jedis, Collection<String> bossIds) {
		BOSS_SESSION_CACHE.del(jedis, bossIds);
	}
	
}
