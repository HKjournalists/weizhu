package com.weizhu.service.apns;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.weizhu.common.jedis.JedisValueCacheEx;

import redis.clients.jedis.Jedis;

public class APNsCache {
	
	private static final JedisValueCacheEx<Long, APNsDAOProtos.APNsDeviceTokenList> APNS_DEVICE_TOKEN_CACHE = 
			JedisValueCacheEx.create("apns:device_token:", APNsDAOProtos.APNsDeviceTokenList.PARSER);
	
	public static Map<Long, List<APNsDAOProtos.APNsDeviceToken>> getDeviceToken(Jedis jedis, long companyId, Collection<Long> userIds) {
		return convertDeviceTokenValue(APNS_DEVICE_TOKEN_CACHE.get(jedis, companyId, userIds));
	}
	
	public static Map<Long, List<APNsDAOProtos.APNsDeviceToken>> getDeviceToken(Jedis jedis, long companyId, Collection<Long> userIds, Collection<Long> noCacheUserIds) {
		return convertDeviceTokenValue(APNS_DEVICE_TOKEN_CACHE.get(jedis, companyId, userIds, noCacheUserIds));
	}
	
	public static void setDeviceToken(Jedis jedis, long companyId, Map<Long, List<APNsDAOProtos.APNsDeviceToken>> deviceTokenMap) {
		APNS_DEVICE_TOKEN_CACHE.set(jedis, companyId, convertDeviceTokenDAO(deviceTokenMap));
	}
	
	public static void setDeviceToken(Jedis jedis, long companyId, Collection<Long> userIds, Map<Long, List<APNsDAOProtos.APNsDeviceToken>> deviceTokenMap) {
		APNS_DEVICE_TOKEN_CACHE.set(jedis, companyId, userIds, convertDeviceTokenDAO(deviceTokenMap));
	}
	
	public static void delDeviceToken(Jedis jedis, long companyId, Collection<Long> userIds) {
		APNS_DEVICE_TOKEN_CACHE.del(jedis, companyId, userIds);
	}
	
	private static Map<Long, List<APNsDAOProtos.APNsDeviceToken>> convertDeviceTokenValue(Map<Long, APNsDAOProtos.APNsDeviceTokenList> daoValueMap) {
		if (daoValueMap.isEmpty()) {
			return Collections.emptyMap();
		}
		
		Map<Long, List<APNsDAOProtos.APNsDeviceToken>> valueMap = new HashMap<Long, List<APNsDAOProtos.APNsDeviceToken>>(daoValueMap.size());
		for (Entry<Long, APNsDAOProtos.APNsDeviceTokenList> entry : daoValueMap.entrySet()) {
			valueMap.put(entry.getKey(), entry.getValue().getDeviceTokenList());
		}
		return valueMap;
	}
	
	private static Map<Long, APNsDAOProtos.APNsDeviceTokenList> convertDeviceTokenDAO(Map<Long, List<APNsDAOProtos.APNsDeviceToken>> valueMap) {
		if (valueMap.isEmpty()) {
			return Collections.emptyMap();
		}
		
		Map<Long, APNsDAOProtos.APNsDeviceTokenList> daoMap = new HashMap<Long, APNsDAOProtos.APNsDeviceTokenList>(valueMap.size());
		
		APNsDAOProtos.APNsDeviceTokenList.Builder tmpBuilder = APNsDAOProtos.APNsDeviceTokenList.newBuilder();
		for (Entry<Long, List<APNsDAOProtos.APNsDeviceToken>> entry : valueMap.entrySet()) {
			tmpBuilder.clear();
			
			tmpBuilder.addAllDeviceToken(entry.getValue());
			daoMap.put(entry.getKey(), tmpBuilder.build());
		}
		return daoMap;
	}
}
