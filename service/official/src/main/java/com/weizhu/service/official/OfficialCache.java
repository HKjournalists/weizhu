package com.weizhu.service.official;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.Pipeline;
import redis.clients.jedis.Response;

import com.weizhu.common.jedis.JedisValueCacheEx;
import com.weizhu.proto.OfficialProtos;

public class OfficialCache {
	
	private static final String INCRX_SCRIPT = "if redis.call('exists', KEYS[1]) == 1 then return tostring(redis.call('incr', KEYS[1])) else return nil end";
	private static String INCRX_SCRIPT_SHA;
	
	public static void loadScript(Jedis jedis) {
		INCRX_SCRIPT_SHA = jedis.scriptLoad(INCRX_SCRIPT);
	}
	
	/**
	 * official:seq:[companyId]:[officialId]:[userId]
	 */
	private static final String OFFICIAL_MSG_SEQ_DOMAIN = "official:seq:";
	
	/**
	 * @param jedis
	 * @param officialId
	 * @param userIds
	 * @return  userId -> msgSeq
	 */
	public static Map<Long, Long> generateOfficialMsgSeq(Jedis jedis, long companyId, long officialId, Collection<Long> userIds) {
		if (userIds.isEmpty()) {
			return Collections.emptyMap();
		}
		
		Map<Long, Response<String>> responseMap = new TreeMap<Long, Response<String>>();
		
		Pipeline pipe = jedis.pipelined();
		for (Long userId : userIds) {
			String key = new StringBuilder().append(OFFICIAL_MSG_SEQ_DOMAIN)
					.append(companyId).append(":")
					.append(officialId).append(":")
					.append(userId)
					.toString();
			
			responseMap.put(userId, pipe.evalsha(INCRX_SCRIPT_SHA, 1, key));
		}
		pipe.sync();
		
		Map<Long, Long> resultMap = new TreeMap<Long, Long>();
		for (Entry<Long, Response<String>> entry : responseMap.entrySet()) {
			String value = entry.getValue().get();
			if (value != null && !value.isEmpty()) {
				resultMap.put(entry.getKey(), Long.parseLong(value));
			}
		}
		return resultMap;
	}
	
	public static Map<Long, Long> getLatestOfficialMsgSeq(Jedis jedis, long companyId, long officialId, Collection<Long> userIds) {
		if (userIds.isEmpty()) {
			return Collections.emptyMap();
		}
		
		String[] keys = new String[userIds.size()];
		int idx = 0;
		for (Long userId : userIds) {
			keys[idx] = new StringBuilder().append(OFFICIAL_MSG_SEQ_DOMAIN)
					.append(companyId).append(":")
					.append(officialId).append(":")
					.append(userId)
					.toString();
			++idx;
		}
		
		List<String> dataList = jedis.mget(keys);
		
		Map<Long, Long> resultMap = new TreeMap<Long, Long>();
		
		Iterator<Long> userIdIt = userIds.iterator();
		Iterator<String> dataIt = dataList.iterator();
		
		while(userIdIt.hasNext() && dataIt.hasNext()) {
			Long userId = userIdIt.next();
			String data = dataIt.next();
			if (data != null && !data.isEmpty()) {
				resultMap.put(userId, Long.parseLong(data));
			}
		}
		return resultMap;
	}
	
	public static void setnxOfficialMsgSeq(Jedis jedis, long companyId, long officialId, Map<Long, Long> userIdToMsgSeqMap) {
		if (userIdToMsgSeqMap.isEmpty()) {
			return;
		}
		
		Pipeline pipe = jedis.pipelined();
		for (Entry<Long, Long> entry : userIdToMsgSeqMap.entrySet()) {
			String key = new StringBuilder().append(OFFICIAL_MSG_SEQ_DOMAIN)
					.append(companyId).append(":")
					.append(officialId).append(":")
					.append(entry.getKey())
					.toString();
			
			pipe.setnx(key, entry.getValue().toString());
		}
		pipe.sync();
	}
	
	private static final JedisValueCacheEx<Long, OfficialProtos.Official> OFFICIAL_INFO_CACHE = 
			JedisValueCacheEx.create("official:info:", OfficialProtos.Official.PARSER);
	
	public static Map<Long, OfficialProtos.Official> getOfficial(Jedis jedis, long companyId, Collection<Long> officialIds) {
		return OFFICIAL_INFO_CACHE.get(jedis, companyId, officialIds);
	}
	
	public static Map<Long, OfficialProtos.Official> getOfficial(Jedis jedis, long companyId, Collection<Long> officialIds, Collection<Long> noCacheOfficialIds) {
		return OFFICIAL_INFO_CACHE.get(jedis, companyId, officialIds, noCacheOfficialIds);
	}
	
	public static void setOfficial(Jedis jedis, long companyId, Map<Long, OfficialProtos.Official> officialInfoMap) {
		OFFICIAL_INFO_CACHE.set(jedis, companyId, officialInfoMap);
	}
	
	public static void setOfficial(Jedis jedis, long companyId, Collection<Long> officialIds, Map<Long, OfficialProtos.Official> officialInfoMap) {
		OFFICIAL_INFO_CACHE.set(jedis, companyId, officialIds, officialInfoMap);
	}
	
	public static void delOfficial(Jedis jedis, long companyId, Collection<Long> officialIds) {
		OFFICIAL_INFO_CACHE.del(jedis, companyId, officialIds);
	}
	
}
