package com.weizhu.service.user.level;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import redis.clients.jedis.Jedis;

import com.google.common.base.Charsets;
import com.google.common.primitives.Bytes;
import com.google.protobuf.InvalidProtocolBufferException;
import com.weizhu.common.jedis.JedisUtil;
import com.weizhu.proto.UserProtos;
import com.weizhu.service.user.UserDAOProtos;

public class LevelCache {
	
	private static final Logger logger = LoggerFactory.getLogger(LevelCache.class);

	private static final byte[] LEVEL_CACHE_KEY = "user:level:".getBytes();
	
	public static Map<Integer, UserProtos.Level> getAllLevel(Jedis jedis, long companyId) {
		
		byte[] data = jedis.get(Bytes.concat(LEVEL_CACHE_KEY, String.valueOf(companyId).getBytes(Charsets.UTF_8)));
		
		if (data == null) {
			return null;
		} else if (data.length <= 0) {
			return Collections.emptyMap();
		} else {
			UserDAOProtos.LevelList daoLevelList;
			try {
				daoLevelList = UserDAOProtos.LevelList.parseFrom(data);
			} catch (InvalidProtocolBufferException e) {
				// parse fail : log error
				logger.warn("cache key : user:level parse fail", e);
				return null;
			}
			
			Map<Integer, UserProtos.Level> resultMap = new LinkedHashMap<Integer, UserProtos.Level>(daoLevelList.getLevelCount());
			for (int i=0; i<daoLevelList.getLevelCount(); ++i) {
				UserProtos.Level level = daoLevelList.getLevel(i);
				resultMap.put(level.getLevelId(), level);
			}
			return resultMap;
		}
	}
	
	public static void setAllLevel(Jedis jedis, long companyId, Map<Integer, UserProtos.Level> levelMap) {
		final byte[] data;
		if (levelMap.isEmpty()) {
			data = JedisUtil.EMPTY_BYTES;
		} else {
			data = UserDAOProtos.LevelList.newBuilder()
					.addAllLevel(levelMap.values())
					.build().toByteArray();
		}
		jedis.set(Bytes.concat(LEVEL_CACHE_KEY, String.valueOf(companyId).getBytes(Charsets.UTF_8)), data);
	}
	
	public static void delAllLevel(Jedis jedis, long companyId) {
		jedis.del(Bytes.concat(LEVEL_CACHE_KEY, String.valueOf(companyId).getBytes(Charsets.UTF_8)));
	}
	
}
