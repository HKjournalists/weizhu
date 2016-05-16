package com.weizhu.service.allow;

import java.util.Collection;
import java.util.Map;

import redis.clients.jedis.Jedis;

import com.weizhu.common.jedis.JedisValueCacheEx;

public class AllowCache {
	
	private static final JedisValueCacheEx<Integer, AllowDAOProtos.ModelRule> ALLOW_MODEL_RULE_CACHE = 
			JedisValueCacheEx.create("allow:model_rule:", AllowDAOProtos.ModelRule.PARSER);
	
	public static Map<Integer, AllowDAOProtos.ModelRule> getModelRule(Jedis jedis, long companyId, Collection<Integer> modelIds) {
		return ALLOW_MODEL_RULE_CACHE.get(jedis, companyId, modelIds);
	}
	
	public static Map<Integer, AllowDAOProtos.ModelRule> getModelRule(Jedis jedis, long companyId, Collection<Integer> modelIds, Collection<Integer> noCacheModelIds) {
		return ALLOW_MODEL_RULE_CACHE.get(jedis, companyId, modelIds, noCacheModelIds);
	}
	
	public static void setModelRule(Jedis jedis, long companyId, Map<Integer, AllowDAOProtos.ModelRule> modelRuleMap) {
		ALLOW_MODEL_RULE_CACHE.set(jedis, companyId, modelRuleMap);
	}
	
	public static void setModelRule(Jedis jedis, long companyId, Collection<Integer> modelIds, Map<Integer, AllowDAOProtos.ModelRule> modelRuleMap) {
		ALLOW_MODEL_RULE_CACHE.set(jedis, companyId, modelIds, modelRuleMap);
	}
	
	public static void delModelRule(Jedis jedis, long companyId, Collection<Integer> modelIds) {
		ALLOW_MODEL_RULE_CACHE.del(jedis, companyId, modelIds);
	}
	
}
