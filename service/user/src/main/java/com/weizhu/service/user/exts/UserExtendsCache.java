package com.weizhu.service.user.exts;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import redis.clients.jedis.Jedis;

import com.weizhu.common.jedis.JedisValueCacheEx;
import com.weizhu.proto.UserProtos;
import com.weizhu.service.user.UserDAOProtos;

public class UserExtendsCache {

	private static final JedisValueCacheEx<Long, UserDAOProtos.UserExtendsList> USER_EXTENDS_CACHE = 
			JedisValueCacheEx.create("user:extends:", UserDAOProtos.UserExtendsList.PARSER);
	
	public static Map<Long, List<UserProtos.UserExtends>> getUserExtends(Jedis jedis, long companyId, Collection<Long> userIds) {
		return convertToList(USER_EXTENDS_CACHE.get(jedis, companyId, userIds));
	}
	
	public static Map<Long, List<UserProtos.UserExtends>> getUserExtends(Jedis jedis, long companyId, Collection<Long> userIds, Collection<Long> noCacheUserIds) {
		return convertToList(USER_EXTENDS_CACHE.get(jedis, companyId, userIds, noCacheUserIds));
	}
	
	public static void setUserExtends(Jedis jedis, long companyId, Map<Long, List<UserProtos.UserExtends>> userExperienceMap) {
		USER_EXTENDS_CACHE.set(jedis, companyId, convertToDAO(userExperienceMap));
	}
	
	public static void setUserExtends(Jedis jedis, long companyId, Collection<Long> userIds, Map<Long, List<UserProtos.UserExtends>> userExperienceMap) {
		USER_EXTENDS_CACHE.set(jedis, companyId, userIds, convertToDAO(userExperienceMap));
	}
	
	public static void delUserExtends(Jedis jedis, long companyId, Collection<Long> userIds) {
		USER_EXTENDS_CACHE.del(jedis, companyId, userIds);
	}
	
	private static Map<Long, List<UserProtos.UserExtends>> convertToList(Map<Long, UserDAOProtos.UserExtendsList> daoMap) {
		if (daoMap.isEmpty()) {
			return Collections.emptyMap();
		}
		
		Map<Long, List<UserProtos.UserExtends>> resultMap = new HashMap<Long, List<UserProtos.UserExtends>>(daoMap.size());
		for (Map.Entry<Long, UserDAOProtos.UserExtendsList> entry : daoMap.entrySet()) {
			resultMap.put(entry.getKey(), entry.getValue().getUserExtendsList());
		}
		return resultMap;
	}
	
	private static Map<Long, UserDAOProtos.UserExtendsList> convertToDAO(Map<Long, List<UserProtos.UserExtends>> experienceMap) {
		if (experienceMap.isEmpty()) {
			return Collections.emptyMap();
		}
		
		Map<Long, UserDAOProtos.UserExtendsList> resultMap = new HashMap<Long, UserDAOProtos.UserExtendsList>(experienceMap.size());
		
		UserDAOProtos.UserExtendsList.Builder tmpBuilder = UserDAOProtos.UserExtendsList.newBuilder();
		for (Map.Entry<Long, List<UserProtos.UserExtends>> entry : experienceMap.entrySet()) {
			tmpBuilder.clear();
			resultMap.put(entry.getKey(), tmpBuilder.addAllUserExtends(entry.getValue()).build());
		}
		return resultMap;
	}
	
}
