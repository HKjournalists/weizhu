package com.weizhu.service.session;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.weizhu.common.jedis.JedisValueCacheEx;
import com.weizhu.proto.SessionProtos;
import redis.clients.jedis.Jedis;

public final class SessionCache {
	
	private static final JedisValueCacheEx<Long, SessionDAOProtos.SessionDataList> SESSION_DATA_CACHE = 
			JedisValueCacheEx.create("session:data:", SessionDAOProtos.SessionDataList.PARSER);
	
	public static Map<Long, List<SessionProtos.SessionData>> getSessionData(Jedis jedis, long companyId, Collection<Long> userIds) {
		return convertToList(SESSION_DATA_CACHE.get(jedis, companyId, userIds));
	}
	
	public static Map<Long, List<SessionProtos.SessionData>> getSessionData(Jedis jedis, long companyId, Collection<Long> userIds, Collection<Long> noCacheUserIds) {
		return convertToList(SESSION_DATA_CACHE.get(jedis, companyId, userIds, noCacheUserIds));
	}
	
	public static void setSessionData(Jedis jedis, long companyId, Map<Long, List<SessionProtos.SessionData>> sessionDataMap) {
		SESSION_DATA_CACHE.set(jedis, companyId, convertToDAO(sessionDataMap));
	}
	
	public static void setSessionData(Jedis jedis, long companyId, Collection<Long> userIds, Map<Long, List<SessionProtos.SessionData>> sessionDataMap) {
		SESSION_DATA_CACHE.set(jedis, companyId, userIds, convertToDAO(sessionDataMap));
	}
	
	public static void delSessionData(Jedis jedis, long companyId, Collection<Long> userIds) {
		SESSION_DATA_CACHE.del(jedis, companyId, userIds);
	}
	
	private static Map<Long, List<SessionProtos.SessionData>> convertToList(Map<Long, SessionDAOProtos.SessionDataList> daoMap) {
		if (daoMap.isEmpty()) {
			return Collections.emptyMap();
		}
		
		Map<Long, List<SessionProtos.SessionData>> resultMap = new HashMap<Long, List<SessionProtos.SessionData>>(daoMap.size());
		for (Map.Entry<Long, SessionDAOProtos.SessionDataList> entry : daoMap.entrySet()) {
			resultMap.put(entry.getKey(), entry.getValue().getSessionDataList());
		}
		return resultMap;
	}
	
	private static Map<Long, SessionDAOProtos.SessionDataList> convertToDAO(Map<Long, List<SessionProtos.SessionData>> sessionDataMap) {
		if (sessionDataMap.isEmpty()) {
			return Collections.emptyMap();
		}
		
		Map<Long, SessionDAOProtos.SessionDataList> resultMap = new HashMap<Long, SessionDAOProtos.SessionDataList>(sessionDataMap.size());
		
		SessionDAOProtos.SessionDataList.Builder tmpBuilder = SessionDAOProtos.SessionDataList.newBuilder();
		for (Map.Entry<Long, List<SessionProtos.SessionData>> entry : sessionDataMap.entrySet()) {
			tmpBuilder.clear();
			resultMap.put(entry.getKey(), tmpBuilder.addAllSessionData(entry.getValue()).build());
		}
		return resultMap;
	}
}
