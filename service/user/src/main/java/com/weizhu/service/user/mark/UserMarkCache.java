package com.weizhu.service.user.mark;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

import redis.clients.jedis.Jedis;

import com.weizhu.common.jedis.JedisValueCacheEx;
import com.weizhu.proto.UserProtos;
import com.weizhu.service.user.UserDAOProtos;

public class UserMarkCache {

	private static final JedisValueCacheEx<Long, UserDAOProtos.UserMarkList> USER_MARK_CACHE = 
			JedisValueCacheEx.create("user:mark:", UserDAOProtos.UserMarkList.PARSER);
	
	public static Map<Long, Map<Long, UserProtos.UserMark>> getUserMark(Jedis jedis, long companyId, Collection<Long> markerIds) {
		return convertToMap(USER_MARK_CACHE.get(jedis, companyId, markerIds));
	}
	
	public static Map<Long, Map<Long, UserProtos.UserMark>> getUserMark(Jedis jedis, long companyId, Collection<Long> markerIds, Collection<Long> noCacheMarkerIds) {
		return convertToMap(USER_MARK_CACHE.get(jedis, companyId, markerIds, noCacheMarkerIds));
	}
	
	public static void setUserMark(Jedis jedis, long companyId, Map<Long, Map<Long, UserProtos.UserMark>> userMarkMap) {
		USER_MARK_CACHE.set(jedis, companyId, convertToDAO(userMarkMap));
	}
	
	public static void setUserMark(Jedis jedis, long companyId, Collection<Long> markerIds, Map<Long, Map<Long, UserProtos.UserMark>> userMarkMap) {
		USER_MARK_CACHE.set(jedis, companyId, markerIds, convertToDAO(userMarkMap));
	}
	
	public static void delUserMark(Jedis jedis, long companyId, Collection<Long> markerIds) {
		USER_MARK_CACHE.del(jedis, companyId, markerIds);
	}
	
	private static Map<Long, Map<Long, UserProtos.UserMark>> convertToMap(Map<Long, UserDAOProtos.UserMarkList> daoMap) {
		if (daoMap.isEmpty()) {
			return Collections.emptyMap();
		}
		
		Map<Long, Map<Long, UserProtos.UserMark>> resultMap = new HashMap<Long, Map<Long, UserProtos.UserMark>>(daoMap.size());
		for (Map.Entry<Long, UserDAOProtos.UserMarkList> entry : daoMap.entrySet()) {
			if (entry.getValue().getUserMarkCount() <= 0) {
				resultMap.put(entry.getKey(), Collections.<Long, UserProtos.UserMark>emptyMap());
			} else {
				Map<Long, UserProtos.UserMark> map = new TreeMap<Long, UserProtos.UserMark>();
				for (UserProtos.UserMark mark : entry.getValue().getUserMarkList()) {
					map.put(mark.getUserId(), mark);
				}
				resultMap.put(entry.getKey(), map);
			}
		}
		return resultMap;
	}
	
	private static Map<Long, UserDAOProtos.UserMarkList> convertToDAO(Map<Long, Map<Long, UserProtos.UserMark>> markMap) {
		if (markMap.isEmpty()) {
			return Collections.emptyMap();
		}
		
		Map<Long, UserDAOProtos.UserMarkList> resultMap = new HashMap<Long, UserDAOProtos.UserMarkList>(markMap.size());
		
		UserDAOProtos.UserMarkList.Builder tmpBuilder = UserDAOProtos.UserMarkList.newBuilder();
		for (Map.Entry<Long, Map<Long, UserProtos.UserMark>> entry : markMap.entrySet()) {
			tmpBuilder.clear();
			resultMap.put(entry.getKey(), tmpBuilder.addAllUserMark(entry.getValue().values()).build());
		}
		return resultMap;
	}
	
}
