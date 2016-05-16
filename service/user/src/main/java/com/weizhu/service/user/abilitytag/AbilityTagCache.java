package com.weizhu.service.user.abilitytag;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import redis.clients.jedis.Jedis;

import com.weizhu.common.jedis.JedisValueCacheEx;
import com.weizhu.proto.UserProtos;
import com.weizhu.service.user.UserDAOProtos;

public class AbilityTagCache {
	
	private static final JedisValueCacheEx<Long, UserDAOProtos.UserAbilityTagList> USER_ABILITY_TAG_CACHE = 
			JedisValueCacheEx.create("user:ability_tag:", UserDAOProtos.UserAbilityTagList.PARSER);
	
	public static Map<Long, List<UserProtos.UserAbilityTag>> getAbilityTag(Jedis jedis, long companyId, Collection<Long> userIds) {
		return convertToList(USER_ABILITY_TAG_CACHE.get(jedis, companyId, userIds));
	}
	
	public static Map<Long, List<UserProtos.UserAbilityTag>> getAbilityTag(Jedis jedis, long companyId, Collection<Long> userIds, Collection<Long> noCacheUserIds) {
		return convertToList(USER_ABILITY_TAG_CACHE.get(jedis, companyId, userIds, noCacheUserIds));
	}
	
	public static void setAbilityTag(Jedis jedis, long companyId, Map<Long, List<UserProtos.UserAbilityTag>> abilityTagMap) {
		USER_ABILITY_TAG_CACHE.set(jedis, companyId, convertToDAO(abilityTagMap));
	}
	
	public static void setAbilityTag(Jedis jedis, long companyId, Collection<Long> userIds, Map<Long, List<UserProtos.UserAbilityTag>> abilityTagMap) {
		USER_ABILITY_TAG_CACHE.set(jedis, companyId, userIds, convertToDAO(abilityTagMap));
	}
	
	public static void delAbilityTag(Jedis jedis, long companyId, Collection<Long> userIds) {
		USER_ABILITY_TAG_CACHE.del(jedis, companyId, userIds);
	}
	
	private static Map<Long, List<UserProtos.UserAbilityTag>> convertToList(Map<Long, UserDAOProtos.UserAbilityTagList> daoMap) {
		if (daoMap.isEmpty()) {
			return Collections.emptyMap();
		}
		
		Map<Long, List<UserProtos.UserAbilityTag>> resultMap = new HashMap<Long, List<UserProtos.UserAbilityTag>>(daoMap.size());
		for (Map.Entry<Long, UserDAOProtos.UserAbilityTagList> entry : daoMap.entrySet()) {
			resultMap.put(entry.getKey(), entry.getValue().getUserAbilityTagList());
		}
		return resultMap;
	}
	
	private static Map<Long, UserDAOProtos.UserAbilityTagList> convertToDAO(Map<Long, List<UserProtos.UserAbilityTag>> abilityTagMap) {
		if (abilityTagMap.isEmpty()) {
			return Collections.emptyMap();
		}
		
		Map<Long, UserDAOProtos.UserAbilityTagList> resultMap = new HashMap<Long, UserDAOProtos.UserAbilityTagList>(abilityTagMap.size());
		
		UserDAOProtos.UserAbilityTagList.Builder tmpBuilder = UserDAOProtos.UserAbilityTagList.newBuilder();
		for (Map.Entry<Long, List<UserProtos.UserAbilityTag>> entry : abilityTagMap.entrySet()) {
			tmpBuilder.clear();
			resultMap.put(entry.getKey(), tmpBuilder.addAllUserAbilityTag(entry.getValue()).build());
		}
		return resultMap;
	}
}
