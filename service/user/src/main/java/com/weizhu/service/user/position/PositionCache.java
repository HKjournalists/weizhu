package com.weizhu.service.user.position;

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

public class PositionCache {
	
	private static final Logger logger = LoggerFactory.getLogger(PositionCache.class);

	private static final byte[] POSITION_CACHE_KEY = "user:position:".getBytes();
	
	public static Map<Integer, UserProtos.Position> getAllPosition(Jedis jedis, long companyId) {
		
		byte[] data = jedis.get(Bytes.concat(POSITION_CACHE_KEY, String.valueOf(companyId).getBytes(Charsets.UTF_8)));
		
		if (data == null) {
			return null;
		} else if (data.length <= 0) {
			return Collections.emptyMap();
		} else {
			UserDAOProtos.PositionList daoPositionList;
			try {
				daoPositionList = UserDAOProtos.PositionList.parseFrom(data);
			} catch (InvalidProtocolBufferException e) {
				// parse fail : log error
				logger.warn("cache key : user:position parse fail", e);
				return null;
			}
			
			Map<Integer, UserProtos.Position> resultMap = new LinkedHashMap<Integer, UserProtos.Position>(daoPositionList.getPositionCount());
			for (int i=0; i<daoPositionList.getPositionCount(); ++i) {
				UserProtos.Position position = daoPositionList.getPosition(i);
				resultMap.put(position.getPositionId(), position);
			}
			return resultMap;
		}
	}
	
	public static void setAllPosition(Jedis jedis, long companyId, Map<Integer, UserProtos.Position> positionMap) {
		final byte[] data;
		if (positionMap.isEmpty()) {
			data = JedisUtil.EMPTY_BYTES;
		} else {
			data = UserDAOProtos.PositionList.newBuilder()
					.addAllPosition(positionMap.values())
					.build().toByteArray();
		}
		jedis.set(Bytes.concat(POSITION_CACHE_KEY, String.valueOf(companyId).getBytes(Charsets.UTF_8)), data);
	}
	
	public static void delAllPosition(Jedis jedis, long companyId) {
		jedis.del(Bytes.concat(POSITION_CACHE_KEY, String.valueOf(companyId).getBytes(Charsets.UTF_8)));
	}
	
}
