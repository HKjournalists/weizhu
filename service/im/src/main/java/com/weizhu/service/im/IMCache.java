package com.weizhu.service.im;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import com.google.common.base.Charsets;
import com.google.common.primitives.Longs;
import com.google.protobuf.InvalidProtocolBufferException;
import com.weizhu.common.jedis.JedisValueCacheEx;
import com.weizhu.proto.IMProtos;
import redis.clients.jedis.Jedis;

public class IMCache {

	private static final String IM_P2P_MSG_SEQ_DOMAIN = "im:p2p:seq:";
	private static final String IM_GROUP_MSG_SEQ_DOMAIN = "im:group:seq:";
	
	private static final byte[] INCRX_SCRIPT = "if redis.call('exists', KEYS[1]) == 1 then return redis.call('incr', KEYS[1]) else return nil end".getBytes();
	private static byte[] INCRX_SCRIPT_SHA;
	
	public static void loadScript(Jedis jedis) {
		INCRX_SCRIPT_SHA = jedis.scriptLoad(INCRX_SCRIPT);
	}
	
	/* p2p chat */
	
	public static Long generateP2PMsgSeq(Jedis jedis, long companyId, long userIdMost, long userIdLeast) {
		StringBuilder keyBuilder = new StringBuilder();
		keyBuilder.append(IM_P2P_MSG_SEQ_DOMAIN).append(companyId).append(":");
		keyBuilder.append(userIdMost).append(":").append(userIdLeast);
		final byte[] key = keyBuilder.toString().getBytes(Charsets.UTF_8);
		return (Long) jedis.evalsha(INCRX_SCRIPT_SHA, 1, key);
	}
	
	public static Long getLatestP2PMsgSeq(Jedis jedis, long companyId, long userIdMost, long userIdLeast) {
		StringBuilder keyBuilder = new StringBuilder();
		keyBuilder.append(IM_P2P_MSG_SEQ_DOMAIN).append(companyId).append(":");
		keyBuilder.append(userIdMost).append(":").append(userIdLeast);
		final byte[] key = keyBuilder.toString().getBytes(Charsets.UTF_8);
		
		byte[] value = jedis.get(key);
		if (value == null || value.length <= 0) {
			return null;
		} else {
			return Long.parseLong(new String(value, Charsets.UTF_8));
		}
	}
	
	public static void setnxP2PMsgSeq(Jedis jedis, long companyId, long userIdMost, long userIdLeast, long latestMsqSeq) {
		StringBuilder keyBuilder = new StringBuilder();
		keyBuilder.append(IM_P2P_MSG_SEQ_DOMAIN).append(companyId).append(":");
		keyBuilder.append(userIdMost).append(":").append(userIdLeast);
		final byte[] key = keyBuilder.toString().getBytes(Charsets.UTF_8);

		jedis.setnx(key, Long.toString(latestMsqSeq).getBytes(Charsets.UTF_8));
	}
	
	/* group chat */
	
	public static Long generateGroupMsgSeq(Jedis jedis, long companyId, long groupId) {
		StringBuilder keyBuilder = new StringBuilder();
		keyBuilder.append(IM_GROUP_MSG_SEQ_DOMAIN).append(companyId).append(":").append(groupId);
		final byte[] key = keyBuilder.toString().getBytes(Charsets.UTF_8);
		
		return (Long) jedis.evalsha(INCRX_SCRIPT_SHA, 1, key);
	}
	
	public static Long getLatestGroupMsgSeq(Jedis jedis, long companyId, long groupId) {
		StringBuilder keyBuilder = new StringBuilder();
		keyBuilder.append(IM_GROUP_MSG_SEQ_DOMAIN).append(companyId).append(":").append(groupId);
		final byte[] key = keyBuilder.toString().getBytes(Charsets.UTF_8);
		
		byte[] value = jedis.get(key);
		if (value == null || value.length <= 0) {
			return null;
		} else {
			return Long.parseLong(new String(value, Charsets.UTF_8));
		}
	}
	
	public static void setnxGroupMsgSeq(Jedis jedis, long companyId, long groupId, long latestMsqSeq) {
		StringBuilder keyBuilder = new StringBuilder();
		keyBuilder.append(IM_GROUP_MSG_SEQ_DOMAIN).append(companyId).append(":").append(groupId);
		final byte[] key = keyBuilder.toString().getBytes(Charsets.UTF_8);
		
		jedis.setnx(key, Long.toString(latestMsqSeq).getBytes(Charsets.UTF_8));
	}
	
	private static final JedisValueCacheEx<Long, IMProtos.GroupChat> IM_GROUP_CHAT_CACHE = 
			JedisValueCacheEx.create("im:group:chat:", IMProtos.GroupChat.PARSER);
	
	public static Map<Long, IMProtos.GroupChat> getGroupChat(Jedis jedis, long companyId, Collection<Long> groupIds) {
		return IM_GROUP_CHAT_CACHE.get(jedis, companyId, groupIds);
	}
	
	public static Map<Long, IMProtos.GroupChat> getGroupChat(Jedis jedis, long companyId, Collection<Long> groupIds, Collection<Long> noCacheGroupIds) {
		return IM_GROUP_CHAT_CACHE.get(jedis, companyId, groupIds, noCacheGroupIds);
	}
	
	public static void setGroupChat(Jedis jedis, long companyId, Map<Long, IMProtos.GroupChat> groupChatMap) {
		IM_GROUP_CHAT_CACHE.set(jedis, companyId, groupChatMap);
	}
	
	public static void setGroupChat(Jedis jedis, long companyId, Collection<Long> groupIds, Map<Long, IMProtos.GroupChat> groupChatMap) {
		IM_GROUP_CHAT_CACHE.set(jedis, companyId, groupIds, groupChatMap);
	}
	
	public static void delGroupChat(Jedis jedis, long companyId, Collection<Long> groupIds) {
		IM_GROUP_CHAT_CACHE.del(jedis, companyId, groupIds);
	}
	
	private static final String IM_GROUP_MSG_DOMAIN = "im:group:msg:";
	private static final int MAX_GROUP_MSG_CACHE_SIZE = 100;
	
	public static void addGroupMsg(Jedis jedis, long companyId, long groupId, List<IMProtos.InstantMessage> msgList) {
		if (msgList.isEmpty()) {
			return;
		}
		if (msgList.size() > MAX_GROUP_MSG_CACHE_SIZE) {
			msgList = msgList.subList(msgList.size() - MAX_GROUP_MSG_CACHE_SIZE, msgList.size());
		}
		
		final byte[] key = new StringBuilder(IM_GROUP_MSG_DOMAIN).append(companyId).append(":").append(groupId).toString().getBytes(Charsets.UTF_8);
		
		final byte[][] values = new byte[msgList.size()][];
		int idx = 0;
		for (IMProtos.InstantMessage msg : msgList) {
			values[idx] = msg.toByteArray();
			idx++;
		}
		
		long size = jedis.rpush(key, values);
		if (size > MAX_GROUP_MSG_CACHE_SIZE) {
			jedis.ltrim(key, -MAX_GROUP_MSG_CACHE_SIZE, -1);
		}
	}
	
	private static final Comparator<IMProtos.InstantMessage> INSTANT_MSG_COMPARATOR = new Comparator<IMProtos.InstantMessage>() {

		@Override
		public int compare(IMProtos.InstantMessage o1, IMProtos.InstantMessage o2) {
			return -Longs.compare(o1.getMsgSeq(), o2.getMsgSeq());
		}
		
	};
	
	public static List<IMProtos.InstantMessage> getGroupMsg(Jedis jedis, long companyId, long groupId) {
		
		final byte[] key = new StringBuilder(IM_GROUP_MSG_DOMAIN).append(companyId).append(":").append(groupId).toString().getBytes(Charsets.UTF_8);
		
		List<byte[]> list = jedis.lrange(key, 0, -1);
		if (list.isEmpty()) {
			return Collections.emptyList();
		}
		List<IMProtos.InstantMessage> msgList = new ArrayList<IMProtos.InstantMessage>(list.size());
		for (byte[] data : list) {
			final IMProtos.InstantMessage msg;
			
			if (data == null || data.length <= 0) {
				msg = null;
			} else {
				IMProtos.InstantMessage tmp = null;
				try {
					tmp = IMProtos.InstantMessage.parseFrom(data);
				} catch (InvalidProtocolBufferException e) {
					// ignore
				}
				msg = tmp;
			}
			
			if (msg != null) {
				msgList.add(msg);
			}
		}
		Collections.sort(msgList, INSTANT_MSG_COMPARATOR);
		return msgList;
	}
}
