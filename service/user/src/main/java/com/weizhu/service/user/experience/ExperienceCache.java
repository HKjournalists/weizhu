package com.weizhu.service.user.experience;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import redis.clients.jedis.Jedis;

import com.weizhu.common.jedis.JedisValueCacheEx;
import com.weizhu.proto.UserProtos;
import com.weizhu.service.user.UserDAOProtos;

public class ExperienceCache {
	
	private static final JedisValueCacheEx<Long, UserDAOProtos.UserExperienceList> USER_EXPERIENCE_CACHE = 
			JedisValueCacheEx.create("user:experience:", UserDAOProtos.UserExperienceList.PARSER);
	
	public static Map<Long, List<UserProtos.UserExperience>> getUserExperience(Jedis jedis, long companyId, Collection<Long> userIds) {
		return convertToList(USER_EXPERIENCE_CACHE.get(jedis, companyId, userIds));
	}
	
	public static Map<Long, List<UserProtos.UserExperience>> getUserExperience(Jedis jedis, long companyId, Collection<Long> userIds, Collection<Long> noCacheUserIds) {
		return convertToList(USER_EXPERIENCE_CACHE.get(jedis, companyId, userIds, noCacheUserIds));
	}
	
	public static void setUserExperience(Jedis jedis, long companyId, Map<Long, List<UserProtos.UserExperience>> userExperienceMap) {
		USER_EXPERIENCE_CACHE.set(jedis, companyId, convertToDAO(userExperienceMap));
	}
	
	public static void setUserExperience(Jedis jedis, long companyId, Collection<Long> userIds, Map<Long, List<UserProtos.UserExperience>> userExperienceMap) {
		USER_EXPERIENCE_CACHE.set(jedis, companyId, userIds, convertToDAO(userExperienceMap));
	}
	
	public static void delUserExperience(Jedis jedis, long companyId, Collection<Long> userIds) {
		USER_EXPERIENCE_CACHE.del(jedis, companyId, userIds);
	}
	
	private static Map<Long, List<UserProtos.UserExperience>> convertToList(Map<Long, UserDAOProtos.UserExperienceList> daoMap) {
		if (daoMap.isEmpty()) {
			return Collections.emptyMap();
		}
		
		Map<Long, List<UserProtos.UserExperience>> resultMap = new HashMap<Long, List<UserProtos.UserExperience>>(daoMap.size());
		for (Map.Entry<Long, UserDAOProtos.UserExperienceList> entry : daoMap.entrySet()) {
			resultMap.put(entry.getKey(), entry.getValue().getUserExperienceList());
		}
		return resultMap;
	}
	
	private static Map<Long, UserDAOProtos.UserExperienceList> convertToDAO(Map<Long, List<UserProtos.UserExperience>> experienceMap) {
		if (experienceMap.isEmpty()) {
			return Collections.emptyMap();
		}
		
		Map<Long, UserDAOProtos.UserExperienceList> resultMap = new HashMap<Long, UserDAOProtos.UserExperienceList>(experienceMap.size());
		
		UserDAOProtos.UserExperienceList.Builder tmpBuilder = UserDAOProtos.UserExperienceList.newBuilder();
		for (Map.Entry<Long, List<UserProtos.UserExperience>> entry : experienceMap.entrySet()) {
			tmpBuilder.clear();
			resultMap.put(entry.getKey(), tmpBuilder.addAllUserExperience(entry.getValue()).build());
		}
		return resultMap;
	}
	
}
