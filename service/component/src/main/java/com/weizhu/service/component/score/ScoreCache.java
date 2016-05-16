package com.weizhu.service.component.score;

import java.util.Collection;
import java.util.Map;

import com.weizhu.common.jedis.JedisValueCacheEx;
import com.weizhu.proto.ComponentProtos;

import redis.clients.jedis.Jedis;

public class ScoreCache {
	
	private static final JedisValueCacheEx<Integer,ComponentProtos.Score> SCORE_CACHE = JedisValueCacheEx.create("component:score:info：", ComponentProtos.Score.PARSER);
	private static final JedisValueCacheEx<Integer,ComponentProtos.ScoreCount> SCORE_COUNT_CACHE = JedisValueCacheEx.create("component:score:count：", ComponentProtos.ScoreCount.PARSER);
	
	public static Map<Integer, ComponentProtos.Score> getScore(Jedis jedis, long companyId, Collection<Integer> scoreIds) {
		return SCORE_CACHE.get(jedis, companyId, scoreIds);
	}
	
	public static Map<Integer, ComponentProtos.Score> getScore(Jedis jedis, long companyId, Collection<Integer> scoreIds, Collection<Integer> noCacheScoreIds) {
		return SCORE_CACHE.get(jedis, companyId, scoreIds, noCacheScoreIds);
	}
	
	public static void setScore(Jedis jedis, long companyId, Map<Integer, ComponentProtos.Score> scoreMap) {
		SCORE_CACHE.set(jedis, companyId, scoreMap);
	}

	public static void setScore(Jedis jedis, long companyId, Collection<Integer> scoreIds, Map<Integer, ComponentProtos.Score> scoreMap) {
		SCORE_CACHE.set(jedis, companyId, scoreIds, scoreMap);
	}
	
	public static void delScore(Jedis jedis, long companyId, Collection<Integer> scoreIds) {
		SCORE_CACHE.del(jedis, companyId, scoreIds);
	}
	
	public static Map<Integer, ComponentProtos.ScoreCount> getScoreCount(Jedis jedis, long companyId, Collection<Integer> scoreIds) {
		return SCORE_COUNT_CACHE.get(jedis, companyId, scoreIds);
	}
	
	public static Map<Integer, ComponentProtos.ScoreCount> getScoreCount(Jedis jedis, long companyId, Collection<Integer> scoreIds, Collection<Integer> noCacheScoreIds) {
		return SCORE_COUNT_CACHE.get(jedis, companyId, scoreIds, noCacheScoreIds);
	}
	
	public static void setScoreCount(Jedis jedis, long companyId, Map<Integer, ComponentProtos.ScoreCount> scoreCountMap) {
		SCORE_COUNT_CACHE.set(jedis, companyId, scoreCountMap);
	}

	public static void setScoreCount(Jedis jedis, long companyId, Collection<Integer> scoreIds, Map<Integer, ComponentProtos.ScoreCount> scoreCountMap) {
		SCORE_COUNT_CACHE.set(jedis, companyId, scoreIds, scoreCountMap);
	}
	
	public static void delScoreCount(Jedis jedis, long companyId, Collection<Integer> scoreIds) {
		SCORE_COUNT_CACHE.del(jedis, companyId, scoreIds);
	}
}
