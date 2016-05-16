package com.weizhu.service.official;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import javax.annotation.Nullable;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import com.weizhu.common.db.DBUtil;
import com.weizhu.common.utils.ProfileManager;
import com.weizhu.proto.AdminOfficialProtos;
import com.weizhu.proto.OfficialProtos;
import com.zaxxer.hikari.HikariDataSource;

public class OfficialUtil {
	
	public static final ProfileManager.ProfileKey<OfficialProtos.Official> WEIZHU_SECRETARY_OFFICIAL =
			ProfileManager.createKey(
					"official:weizhu_secretary_official", 
					OfficialProtos.Official.newBuilder()
						.setOfficialId(AdminOfficialProtos.ReservedOfficialId.WEIZHU_SECRETARY_VALUE)
						.setOfficialName("小秘书")
						.setAvatar("")
						.setState(OfficialProtos.State.NORMAL)
						.build(), 
					OfficialProtos.Official.getDefaultInstance());

	public static Map<Long, OfficialProtos.Official> getOfficial(
			HikariDataSource hikariDataSource, JedisPool jedisPool, ProfileManager.Profile profile, 
			long companyId, Collection<Long> officialIds, @Nullable Set<OfficialProtos.State> stateSet
			) {
		if (officialIds.isEmpty() || (stateSet != null && stateSet.isEmpty())) {
			return Collections.emptyMap();
		}
		
		Set<Long> officialIdSet = new TreeSet<Long>(officialIds);
		Map<Long, OfficialProtos.Official> officialMap = new TreeMap<Long, OfficialProtos.Official>();
		
		Long weizhuSecretaryOfficialId = Long.valueOf(AdminOfficialProtos.ReservedOfficialId.WEIZHU_SECRETARY_VALUE);
		if (officialIdSet.remove(weizhuSecretaryOfficialId)) {
			OfficialProtos.Official weizhuSecretaryOfficial = profile.get(WEIZHU_SECRETARY_OFFICIAL);
			if (stateSet == null || stateSet.contains(weizhuSecretaryOfficial.getState())) {
				officialMap.put(weizhuSecretaryOfficialId, profile.get(WEIZHU_SECRETARY_OFFICIAL));
			}
		}
		
		if (officialIdSet.isEmpty()) {
			return officialMap;
		}
		
		Set<Long> noCacheOfficialIdSet = new TreeSet<Long>();
		Jedis jedis = jedisPool.getResource();
		try {
			officialMap.putAll(OfficialCache.getOfficial(jedis, companyId, officialIdSet, noCacheOfficialIdSet));
		} finally {
			jedis.close();
		}
		
		if (!noCacheOfficialIdSet.isEmpty()) {
			Map<Long, OfficialProtos.Official> noCacheOfficialMap;
			Connection dbConn = null;
			try {
				dbConn = hikariDataSource.getConnection();
				noCacheOfficialMap = OfficialDB.getOfficial(dbConn, companyId, noCacheOfficialIdSet);
			} catch (SQLException e) {
				throw new RuntimeException("db fail", e);
			} finally {
				DBUtil.closeQuietly(dbConn);
			}
			
			jedis = jedisPool.getResource();
			try {
				OfficialCache.setOfficial(jedis, companyId, noCacheOfficialIdSet, noCacheOfficialMap);
			} finally {
				jedis.close();
			}
			
			officialMap.putAll(noCacheOfficialMap);
		}
		
		if (stateSet == null) {
			return officialMap;
		}
		
		Iterator<Entry<Long, OfficialProtos.Official>> it = officialMap.entrySet().iterator();
		while (it.hasNext()) {
			OfficialProtos.Official official = it.next().getValue();
			if (!stateSet.contains(official.getState())) {
				it.remove();
			}
		}
		return officialMap;
	}
	
	public static OfficialProtos.OfficialMessage saveOfficialSingleMessage(
			HikariDataSource hikariDataSource, JedisPool jedisPool, 
			long companyId, long officialId, long userId, 
			OfficialProtos.OfficialMessage msg,
			int msgTime, boolean isFromUser
			) {
		
		Long msgSeq;
		Jedis jedis = jedisPool.getResource();
		try {
			// 生成 msgSeq
			msgSeq = OfficialCache.generateOfficialMsgSeq(jedis, companyId, officialId, Collections.singleton(userId)).get(userId);
		} finally {
			jedis.close();
		}
		
		if (msgSeq == null) {
			Long tmpMsgSeq;
			Connection dbConn = null;
			try {
				dbConn = hikariDataSource.getConnection();
				// 获取不在Cache中的msgSeq
				tmpMsgSeq = OfficialDB.getOfficialLatestMsgSeq(dbConn, companyId, officialId, Collections.singleton(userId)).get(userId);
			} catch (SQLException e) {
				throw new RuntimeException("get officialId message db fail!", e);
			} finally {
				DBUtil.closeQuietly(dbConn);
			}
			
			if (tmpMsgSeq == null) {
				throw new RuntimeException("get official msg seq from db fail! " + companyId + "," + officialId + "," + userId);
			}
			
			jedis = jedisPool.getResource();
			try {
				OfficialCache.setnxOfficialMsgSeq(jedis, companyId, officialId, Collections.singletonMap(userId, tmpMsgSeq));
				msgSeq = OfficialCache.generateOfficialMsgSeq(jedis, companyId, officialId, Collections.singleton(userId)).get(userId);
			} finally {
				jedis.close();
			}
			
			if (msgSeq == null) {
				throw new RuntimeException("generate official msg seq fail! " + companyId + "," + officialId + "," + userId);
			}
		}
		
		OfficialProtos.OfficialMessage msgWithSeq = msg.toBuilder()
				.setMsgSeq(msgSeq)
				.setMsgTime(msgTime)
				.setIsFromUser(isFromUser)
				.build();
		
		Connection dbConn = null;
		try {
			dbConn = hikariDataSource.getConnection();
			// 将带seq的msgMap 存储到db中
			OfficialDB.insertOfficialSingleMessage(dbConn, companyId, officialId, userId, msgWithSeq);
		} catch (SQLException e) {
			throw new RuntimeException("get officialId message db fail!", e);
		} finally {
			DBUtil.closeQuietly(dbConn);
		}
		
		return msgWithSeq;
	}
	
	public static Map<Long, OfficialProtos.OfficialMessage> saveOfficialMultiMessage(
			HikariDataSource hikariDataSource, JedisPool jedisPool, 
			long companyId, long officialId, Set<Long> userIdSet, 
			OfficialProtos.OfficialMessage msg, long msgRefId,
			int msgTime, boolean isFromUser) {
		if (userIdSet.isEmpty()) {
			return Collections.emptyMap();
		}
		
		Map<Long, Long> msgSeqMap = new TreeMap<Long, Long>();
		Jedis jedis = jedisPool.getResource();
		try {
			// 生成 msgSeq
			msgSeqMap.putAll(OfficialCache.generateOfficialMsgSeq(jedis, companyId, officialId, userIdSet));
		} finally {
			jedis.close();
		}
		
		if (!userIdSet.equals(msgSeqMap.keySet())) {
			
			Set<Long> noCacheUserIdSet = new TreeSet<Long>();
			for (Long userId : userIdSet) {
				if (!msgSeqMap.containsKey(userId)) {
					noCacheUserIdSet.add(userId);
				}
			}
			
			Map<Long, Long> tmpMsgSeqMap;
			Connection dbConn = null;
			try {
				dbConn = hikariDataSource.getConnection();
				// 获取不在Cache中的msgSeq
				tmpMsgSeqMap = OfficialDB.getOfficialLatestMsgSeq(dbConn, companyId, officialId, noCacheUserIdSet);
			} catch (SQLException e) {
				throw new RuntimeException("get officialId message db fail!", e);
			} finally {
				DBUtil.closeQuietly(dbConn);
			}
			
			jedis = jedisPool.getResource();
			try {
				OfficialCache.setnxOfficialMsgSeq(jedis, companyId, officialId, tmpMsgSeqMap);
				msgSeqMap.putAll(OfficialCache.generateOfficialMsgSeq(jedis, companyId, officialId, noCacheUserIdSet));
			} finally {
				jedis.close();
			}
			
			for (Long userId : userIdSet) {
				if (!msgSeqMap.containsKey(userId)) {
					// TODO log error
				}
			}
		}
		
		Map<Long, OfficialProtos.OfficialMessage> msgMap = new TreeMap<Long, OfficialProtos.OfficialMessage>();
		
		OfficialProtos.OfficialMessage.Builder tmpBuilder = OfficialProtos.OfficialMessage.newBuilder();
		for (Entry<Long, Long> entry : msgSeqMap.entrySet()) {
			Long userId = entry.getKey();
			Long msgSeq = entry.getValue();
			
			msgMap.put(userId, tmpBuilder.clear()
					.mergeFrom(msg)
					.setMsgSeq(msgSeq)
					.setMsgTime(msgTime)
					.setIsFromUser(isFromUser)
					.build());
		}
		
		Connection dbConn = null;
		try {
			dbConn = hikariDataSource.getConnection();
			// 将带seq的msgMap 存储到db中
			OfficialDB.insertOfficialMultiMessage(dbConn, companyId, officialId, msgMap, msgRefId);
		} catch (SQLException e) {
			throw new RuntimeException("get officialId message db fail!", e);
		} finally {
			DBUtil.closeQuietly(dbConn);
		}
		
		return msgMap;
	}

	/**
	 * 检查发送的消息是否正确。 如果正确 return null, 如果不正确 return failText;
	 */
	public static String checkSendMessage(OfficialProtos.OfficialMessage msg) {
		switch (msg.getMsgTypeCase()) {
			case TEXT: {
				OfficialProtos.OfficialMessage.Text text = msg.getText();
				if (text.getContent().length() > 65535) {
					return "发送文本内容超长";
				}
				return null;
			}
			case VOICE: {
				OfficialProtos.OfficialMessage.Voice voice = msg.getVoice();
				if (voice.getData().size() > 65535) {
					return "发送语音内容超长";
				}
				return null;
			}
			case IMAGE: {
				OfficialProtos.OfficialMessage.Image image = msg.getImage();
				if (image.getName().length() > 191) {
					return "图片名称超长";
				}
				return null;
			}
			case USER: {
				return null;
			}
			case VIDEO: {
				OfficialProtos.OfficialMessage.Video video = msg.getVideo();
				if (video.getName().isEmpty()) {
					return "视频名称为空";
				} else if (video.getName().length() > 191) {
					return "视频名称超长";
				} else if (video.getType().isEmpty()) {
					return "视频类型为空";
				} else if (video.getType().length() > 191) {
					return "视频类型超长";
				} else if (video.getSize() <= 0 || video.getSize() > 40 * 1024 * 1024) {
					return "视频大小不正确";
				} else if (video.getTime() <= 0 || video.getTime() > 1000) {
					return "视频长度不正确";
				} else if (video.getImageName().isEmpty()) {
					return "视频截图名称为空";
				} else if (video.getImageName().length() > 191) {
					return "视频截图名称超长";
				}
				return null;
			}
			case FILE: {
				return "暂不支持发送此类型消息";
			}
			case DISCOVER_ITEM: {
				return null;
			}
			case COMMUNITY_POST: {
				OfficialProtos.OfficialMessage.CommunityPost communityPost = msg.getCommunityPost();
				if (communityPost.hasText() && communityPost.getText().length() > 65535) {
					return "发送社区帖子描述文本内容超长";
				}
				return null;
			}
			case MSGTYPE_NOT_SET:
				return "不能发送空类型消息";
			default:
				return "发送消息类型未知";
		}
	}
	
}
