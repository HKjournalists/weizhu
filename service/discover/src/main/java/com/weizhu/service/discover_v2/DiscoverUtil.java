package com.weizhu.service.discover_v2;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import javax.annotation.Nullable;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import com.weizhu.common.db.DBUtil;
import com.weizhu.proto.DiscoverV2Protos;
import com.zaxxer.hikari.HikariDataSource;

public class DiscoverUtil {
	
	private static final DiscoverV2Protos.Item.Count EMPTY_ITEM_COUNT = 
			DiscoverV2Protos.Item.Count.newBuilder()
				.setLearnCnt(0)
				.setLearnUserCnt(0)
				.setCommentCnt(0)
				.setCommentUserCnt(0)
				.setScoreNumber(0)
				.setScoreUserCnt(0)
				.setLikeCnt(0)
				.setShareCnt(0)
				.build();
	
	public static Map<Long, DiscoverV2Protos.Item> getItem(
			HikariDataSource hikariDataSource, JedisPool jedisPool, long companyId,
			Collection<Long> itemIds, @Nullable Collection<DiscoverV2Protos.State> states, @Nullable Long userId
			) {
		if (itemIds.isEmpty() || (states != null && states.isEmpty())) {
			return Collections.emptyMap();
		}
		
		Map<Long, DiscoverV2Protos.Item.Base> itemBaseMap = DiscoverUtil.getItemBase(hikariDataSource, jedisPool, companyId, itemIds, states);
		if (itemBaseMap.isEmpty()) {
			// fail fast: 基础信息为空，立即返回吧
			return Collections.emptyMap();
		}
		
		Map<Long, DiscoverV2Protos.Item.Count> itemCountMap = DiscoverUtil.getItemCount(hikariDataSource, jedisPool, companyId, itemBaseMap.keySet());
		
		Map<Long, DiscoverV2Protos.Item.User> itemUserMap;
		if (userId == null) {
			itemUserMap = Collections.emptyMap();
		} else {
			Connection dbConn = null;
			try {
				dbConn = hikariDataSource.getConnection();
				itemUserMap = DiscoverV2DB.getItemUser(dbConn, companyId, itemBaseMap.keySet(), userId);
			} catch (SQLException e) {
				throw new RuntimeException("db failed");
			} finally {
				DBUtil.closeQuietly(dbConn);
			}
		}
		
		Map<Long, DiscoverV2Protos.Item> itemMap = new TreeMap<Long, DiscoverV2Protos.Item>();
		
		DiscoverV2Protos.Item.Builder tmpItemBuilder = DiscoverV2Protos.Item.newBuilder();
		DiscoverV2Protos.Item.User emptyItemUser = null;
		for (DiscoverV2Protos.Item.Base itemBase : itemBaseMap.values()) {
			tmpItemBuilder.clear();
			
			tmpItemBuilder.setBase(itemBase);
			
			// haier使用v5调研需要获取用户id特殊需求
			if (itemBase.hasWebUrl() && itemBase.getWebUrl().getWebUrl().contains("${short_user_id}") && userId != null) {
				String url = itemBase.getWebUrl().getWebUrl();
				String v = String.valueOf(userId);
				if (v.length() > 9) {
					v = v.substring(v.length() - 9);
				}
				url = url.replace("${short_user_id}", v);
				
				tmpItemBuilder.setBase(itemBase.toBuilder()
						.setWebUrl(itemBase.getWebUrl().toBuilder()
								.setWebUrl(url)
								.build())
						.build());
			}
			
			DiscoverV2Protos.Item.Count itemCount = itemCountMap.get(itemBase.getItemId());
			tmpItemBuilder.setCount(itemCount == null ? EMPTY_ITEM_COUNT : itemCount);
			
			if (userId != null) {
				DiscoverV2Protos.Item.User itemUser = itemUserMap.get(itemBase.getItemId());
				if (itemUser == null) {
					if (emptyItemUser == null) {
						emptyItemUser = DiscoverV2Protos.Item.User.newBuilder()
								.setUserId(userId)
								.setIsComment(false)
								.setIsLearn(false)
								.setIsScore(false)
								.build();
					}
					
					itemUser = emptyItemUser;
				}
				
				tmpItemBuilder.setUser(itemUser);
			}
			
			itemMap.put(itemBase.getItemId(), tmpItemBuilder.build());
		}
		
		return itemMap;
	}
	
	public static Map<Long, DiscoverV2Protos.Item.Base> getItemBase(
			HikariDataSource hikariDataSource, JedisPool jedisPool, long companyId,
			Collection<Long> itemIds, @Nullable Collection<DiscoverV2Protos.State> states) {
		if (itemIds.isEmpty() || (states != null && states.isEmpty())) {
			return Collections.emptyMap();
		}

		Map<Long, DiscoverV2Protos.Item.Base> itemBaseMap = new TreeMap<Long, DiscoverV2Protos.Item.Base>();
		
		Set<Long> noCacheItemIds = new TreeSet<Long>();
		Jedis jedis = jedisPool.getResource();
		try {
			itemBaseMap.putAll(DiscoverV2Cache.getItemBase(jedis, companyId, itemIds, noCacheItemIds));
		} finally {
			jedis.close();
		}

		if (!noCacheItemIds.isEmpty()) {

			Map<Long, DiscoverV2Protos.Item.Base> noCacheItemBaseMap;
			Connection dbConn = null;
			try {
				dbConn = hikariDataSource.getConnection();
				noCacheItemBaseMap = DiscoverV2DB.getItemBase(dbConn, companyId, noCacheItemIds);
			} catch (SQLException e) {
				throw new RuntimeException("db failed");
			} finally {
				DBUtil.closeQuietly(dbConn);
			}

			jedis = jedisPool.getResource();
			try {
				DiscoverV2Cache.setItemBase(jedis, companyId, noCacheItemBaseMap);
			} finally {
				jedis.close();
			}

			itemBaseMap.putAll(noCacheItemBaseMap);
		}
		
		if (states == null) {
			return itemBaseMap;
		}
		
		Map<Long, DiscoverV2Protos.Item.Base> resultMap = new TreeMap<Long, DiscoverV2Protos.Item.Base>();

		// 根据state过滤item
		for (Entry<Long, DiscoverV2Protos.Item.Base> entry : itemBaseMap.entrySet()) {
			if (states.contains(entry.getValue().getState())) {
				resultMap.put(entry.getKey(), entry.getValue());
			}
		}

		return resultMap;
	}

	public static Map<Long, DiscoverV2Protos.Item.Count> getItemCount(
			HikariDataSource hikariDataSource, JedisPool jedisPool, long companyId, Collection<Long> itemIds) {
		if (itemIds.isEmpty()) {
			return Collections.emptyMap();
		}

		Map<Long, DiscoverV2Protos.Item.Count> itemCountMap = new TreeMap<Long, DiscoverV2Protos.Item.Count>();
		
		Set<Long> noCacheItemIdSet = new TreeSet<Long>();
		Jedis jedis = jedisPool.getResource();
		try {
			itemCountMap.putAll(DiscoverV2Cache.getItemCount(jedis, companyId, itemIds, noCacheItemIdSet));
		} finally {
			jedis.close();
		}

		if (!noCacheItemIdSet.isEmpty()) {

			Map<Long, DiscoverV2Protos.Item.Count> noCacheItemCountMap;
			Connection dbConn = null;
			try {
				dbConn = hikariDataSource.getConnection();
				noCacheItemCountMap = DiscoverV2DB.getItemCount(dbConn, companyId, noCacheItemIdSet);
			} catch (SQLException e) {
				throw new RuntimeException("db failed");
			} finally {
				DBUtil.closeQuietly(dbConn);
			}

			jedis = jedisPool.getResource();
			try {
				DiscoverV2Cache.setItemCount(jedis, companyId, noCacheItemCountMap);
			} finally {
				jedis.close();
			}

			itemCountMap.putAll(noCacheItemCountMap);
		}

		return itemCountMap;
	}
	
	public static String checkWebUrl(DiscoverV2Protos.WebUrl webUrl) {
		if (webUrl.getWebUrl().length() > 191) {
			return "web url长度超出范围！";
		}
		return null;
	}
	
	public static String checkDocument(DiscoverV2Protos.Document document) {
		if (document.getDocumentUrl().length() > 191) {
			return "文档url过长！";
		}
		if (document.getDocumentType().length() > 191) {
			return "文档类型过长！";
		}
		if (document.hasCheckMd5() && document.getCheckMd5().length() > 191) {
			return "文档校验md5过长！";
		}
		return null;
	}
	
	public static String checkVideo(DiscoverV2Protos.Video video) {
		if (video.getVideoUrl().length() > 191) {
			return "视频url过长！";
		}
		if (video.getVideoType().length() > 191) {
			return "视频类型过长！";
		}
		if (video.hasCheckMd5() && video.getCheckMd5().length() > 191) {
			return "视频校验md5过长！";
		}
		return null;
	}
	
	public static String checkAudio(DiscoverV2Protos.Audio audio) {
		if (audio.getAudioUrl().length() > 191) {
			return "音频url过长！";
		}
		if (audio.getAudioType().length() > 191) {
			return "音频类型过长！";
		}
		if (audio.hasCheckMd5() && audio.getCheckMd5().length() > 191) {
			return "音频校验md5过长！";
		}
		return null;
	}
	
	public static String checkAppUri(DiscoverV2Protos.AppUri appUri) {
		if (appUri.getAppUri().length() > 191) {
			return "app uri长度超出范围！";
		}
		return null;
	}

}
