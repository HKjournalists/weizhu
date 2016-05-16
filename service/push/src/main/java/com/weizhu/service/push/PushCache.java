package com.weizhu.service.push;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import com.google.common.base.Charsets;
import com.google.protobuf.InvalidProtocolBufferException;
import com.weizhu.common.jedis.JedisUtil;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.Pipeline;
import redis.clients.jedis.Response;

public class PushCache {

	private static final byte[] PUSH_SEQ_DOMAIN = "push:seq:".getBytes();
	private static final byte[] PUSH_OFFLINE_MSG_DOMAIN = "push:offline_msg:".getBytes();
	private static final byte[] PUSH_OFFLINE_STATE_DOMAIN = "push:offline_state:".getBytes();
	
	private static final String INCRX_SCRIPT = "if redis.call('exists', KEYS[1]) == 1 then return tostring(redis.call('incr', KEYS[1])) else return nil end";
	private static String INCRX_SCRIPT_SHA;
	
	public static void loadScript(Jedis jedis) {
		INCRX_SCRIPT_SHA = jedis.scriptLoad(INCRX_SCRIPT);
	}
	
	/** 
	 * target user id -> push seq (not null)
	 */
	public static Map<Long, Long> generatePushSeq(Jedis jedis, long companyId, Set<Long> userIdSet) {
		if (userIdSet.isEmpty()) {
			return Collections.emptyMap();
		}
		
		Map<Long, Response<String>> responseMap = new HashMap<Long, Response<String>>(userIdSet.size());
		
		Pipeline pipeline = jedis.pipelined();
		for (Long userId : userIdSet) {
			String key = new String(JedisUtil.makeKey(PUSH_SEQ_DOMAIN, companyId, userId));
			responseMap.put(userId, pipeline.evalsha(INCRX_SCRIPT_SHA, 1, new String[]{key}));
		}
		pipeline.sync();
		
		Map<Long, Long> resultMap = new HashMap<Long, Long>(userIdSet.size());
		for (Entry<Long, Response<String>> entry : responseMap.entrySet()) {
			String v = entry.getValue().get();
			if (v != null) {
				resultMap.put(entry.getKey(), Long.parseLong(v));
			}
		}
		return resultMap;
	}
	
	public static List<Long> generatePushSeq(Jedis jedis, long companyId, List<Long> userIdList) {
		if (userIdList.isEmpty()) {
			return Collections.emptyList();
		}
		
		List<Response<String>> responseList = new ArrayList<Response<String>>(userIdList.size());
		
		Pipeline pipeline = jedis.pipelined();
		for (Long userId : userIdList) {
			String key = new String(JedisUtil.makeKey(PUSH_SEQ_DOMAIN, companyId, userId));
			responseList.add(pipeline.evalsha(INCRX_SCRIPT_SHA, 1, new String[]{key}));
		}
		pipeline.sync();
		
		List<Long> resultList = new ArrayList<Long>(userIdList.size());
		for (Response<String> response : responseList) {
			String value = response.get();
			if (value == null || value.isEmpty()) {
				resultList.add(null);
			} else {
				resultList.add(Long.parseLong(value));
			}
		}
		return resultList;
	}
	
	public static Long getPushSeq(Jedis jedis, long companyId, long userId) {
		byte[] data = jedis.get(JedisUtil.makeKey(PUSH_SEQ_DOMAIN, companyId, userId));
		if (data == null || data.length <= 0) {
			return null;
		}
		
		try {
			return Long.parseLong(new String(data, Charsets.UTF_8));
		} catch (NumberFormatException e) {
			return null;
		}
	}
	
	public static void setnxPushSeq(Jedis jedis, long companyId, Map<Long, Long> pushSeqMap) {
		if (pushSeqMap.isEmpty()) {
			return;
		}
		
		// msetnx 方法中，只要一个key存在，则整体操作失败。 所以不使用msetnx
		Pipeline pipeline = jedis.pipelined();
		for (Entry<Long, Long> entry : pushSeqMap.entrySet()) {
			long userId = entry.getKey();
			long pushSeq = entry.getValue();
			pipeline.setnx(JedisUtil.makeKey(PUSH_SEQ_DOMAIN, companyId, userId), JedisUtil.makeValue(pushSeq));
		}
		pipeline.sync();
	}
	
	public static Map<Long, Long> addOfflineMsg(Jedis jedis, long companyId, Map<Long, List<PushDAOProtos.OfflineMsg>> offlineMsgListMap) {
		if (offlineMsgListMap.isEmpty()) {
			return Collections.emptyMap();
		}
		
		boolean isAllEmpty = true;
		for (Entry<Long, List<PushDAOProtos.OfflineMsg>> entry : offlineMsgListMap.entrySet()) {
			if (!entry.getValue().isEmpty()) {
				isAllEmpty = false;
				break;
			}
		}
		if (isAllEmpty) {
			return Collections.emptyMap();
		}
		
		Map<Long, Response<Long>> responseMap = new HashMap<Long, Response<Long>>(offlineMsgListMap.size());
		Pipeline pipeline = jedis.pipelined();
		for (Entry<Long, List<PushDAOProtos.OfflineMsg>> entry : offlineMsgListMap.entrySet()) {
			final Long userId = entry.getKey();
			final List<PushDAOProtos.OfflineMsg> msgList = entry.getValue();
			
			if (!msgList.isEmpty()) {
				byte[][] values = new byte[msgList.size()][];
				int idx = 0;
				for (PushDAOProtos.OfflineMsg msg : msgList) {
					values[idx] = msg.toByteArray();
					++idx;
				}
				
				responseMap.put(userId, pipeline.rpush(JedisUtil.makeKey(PUSH_OFFLINE_MSG_DOMAIN, companyId, userId), values));
			}
		}
		pipeline.sync();
		
		Map<Long, Long> resultMap = new HashMap<Long, Long>(offlineMsgListMap.size());
		for (Entry<Long, Response<Long>> entry : responseMap.entrySet()) {
			resultMap.put(entry.getKey(), entry.getValue().get());
		}
		return resultMap;
	}
	
	public static void trimOfflineMsg(Jedis jedis, long companyId, Collection<Long> userIds, int size) {
		if (userIds.isEmpty()) {
			return;
		}
		
		Pipeline pipeline = jedis.pipelined();
		for (Long userId : userIds) {
			pipeline.ltrim(JedisUtil.makeKey(PUSH_OFFLINE_MSG_DOMAIN, companyId, userId), -size, 1);
		}
		pipeline.sync();
	}
	
	public static List<PushDAOProtos.OfflineMsg> getOfflineMsg(Jedis jedis, long companyId, long userId) {
		List<byte[]> list = jedis.lrange(JedisUtil.makeKey(PUSH_OFFLINE_MSG_DOMAIN, companyId, userId), 0, -1);
		if (list.isEmpty()) {
			return Collections.emptyList();
		}
		List<PushDAOProtos.OfflineMsg> msgList = new ArrayList<PushDAOProtos.OfflineMsg>(list.size());
		for (byte[] data : list) {
			if (data == null || data.length <= 0) {
				continue;
			}
			
			try {
				msgList.add(PushDAOProtos.OfflineMsg.PARSER.parseFrom(data));
			} catch (InvalidProtocolBufferException e) {
				// ignore
			}
		}
		return msgList;
	}
	
	public static void setOfflineState(Jedis jedis, long companyId, Map<Long, List<PushDAOProtos.OfflineState>> offlineStateListMap) {
		if (offlineStateListMap.isEmpty()) {
			return ;
		}
		
		boolean isAllEmpty = true;
		for (Entry<Long, List<PushDAOProtos.OfflineState>> entry : offlineStateListMap.entrySet()) {
			if (!entry.getValue().isEmpty()) {
				isAllEmpty = false;
				break;
			}
		}
		if (isAllEmpty) {
			return;
		}
		
		Pipeline pipeline = jedis.pipelined();
		PushDAOProtos.StateKey.Builder tmpStateKeyBuilder = PushDAOProtos.StateKey.newBuilder();
		for (Entry<Long, List<PushDAOProtos.OfflineState>> entry : offlineStateListMap.entrySet()) {
			final Long userId = entry.getKey();
			for (PushDAOProtos.OfflineState offlineState : entry.getValue()) {
				
				byte[] field = tmpStateKeyBuilder.clear()
						.setPushName(offlineState.getPushName())
						.setPushKey(offlineState.getPushKey())
						.build().toByteArray();
				
				pipeline.hset(
						JedisUtil.makeKey(PUSH_OFFLINE_STATE_DOMAIN, companyId, userId), 
						field, 
						Long.toString(offlineState.getPushSeq()).getBytes(Charsets.UTF_8)
						);
			}
		}
		pipeline.sync();
	}
	
	public static List<PushDAOProtos.OfflineState> getOfflineState(Jedis jedis, long companyId, long userId) {
		Map<byte[], byte[]> resultMap = jedis.hgetAll(JedisUtil.makeKey(PUSH_OFFLINE_STATE_DOMAIN, companyId, userId));
		if (resultMap.isEmpty()) {
			return Collections.emptyList();
		}
		
		List<PushDAOProtos.OfflineState> list = new ArrayList<PushDAOProtos.OfflineState>(resultMap.size());
		PushDAOProtos.OfflineState.Builder tmpOfflineStateBuilder = PushDAOProtos.OfflineState.newBuilder();
		for (Entry<byte[], byte[]> entry : resultMap.entrySet()) {
			tmpOfflineStateBuilder.clear();
			try {
				PushDAOProtos.StateKey stateKey = PushDAOProtos.StateKey.parseFrom(entry.getKey());
				long pushSeq = Long.parseLong(new String(entry.getValue(), Charsets.UTF_8));
				
				list.add(tmpOfflineStateBuilder
						.setPushSeq(pushSeq)
						.setPushName(stateKey.getPushName())
						.setPushKey(stateKey.getPushKey())
						.build());
				
			} catch (InvalidProtocolBufferException e) {
				// ignore
			} catch (NumberFormatException e) {
				// ignore
			}
		}
		return list;
	}
	
	public static void delOfflineState(Jedis jedis, long companyId, Map<Long, PushDAOProtos.OfflineState> offlineStateMap) {
		if (offlineStateMap.isEmpty()) {
			return ;
		}
		
		Pipeline pipeline = jedis.pipelined();
		
		PushDAOProtos.StateKey.Builder tmpStateKeyBuilder = PushDAOProtos.StateKey.newBuilder();
		for (Entry<Long, PushDAOProtos.OfflineState> entry : offlineStateMap.entrySet()) {
			byte[] field = tmpStateKeyBuilder.clear()
					.setPushName(entry.getValue().getPushName())
					.setPushKey(entry.getValue().getPushKey())
					.build().toByteArray();
			pipeline.hdel(JedisUtil.makeKey(PUSH_OFFLINE_STATE_DOMAIN, companyId, entry.getKey()), field);
		}
		pipeline.sync();
	}
	
}
