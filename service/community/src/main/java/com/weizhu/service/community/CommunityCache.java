package com.weizhu.service.community;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import redis.clients.jedis.Jedis;

import com.weizhu.common.jedis.JedisValueCache;
import com.weizhu.common.jedis.JedisValueCacheEx;
import com.weizhu.proto.CommunityProtos;

public class CommunityCache {


	private static final JedisValueCache<Long, CommunityDAOProtos.CommunityInfo> COMMUNITY_INFO_CACHE = 
			JedisValueCache.create("community:info:", CommunityDAOProtos.CommunityInfo.PARSER);

	public static Map<Long, CommunityDAOProtos.CommunityInfo> getCommunityInfo(Jedis jedis, Collection<Long> companyIds) {
		return COMMUNITY_INFO_CACHE.get(jedis, companyIds);
	}

	public static Map<Long, CommunityDAOProtos.CommunityInfo> getCommunityInfo(Jedis jedis, Collection<Long> companyIds, Collection<Long> noCacheCompanyIds) {
		return COMMUNITY_INFO_CACHE.get(jedis, companyIds, noCacheCompanyIds);
	}
	
	public static void setCommunityInfo(Jedis jedis, Map<Long, CommunityDAOProtos.CommunityInfo> communityInfoMap) {
		COMMUNITY_INFO_CACHE.set(jedis, communityInfoMap);
	}
	
	public static void delCommunityInfo(Jedis jedis, Collection<Long> companyIds) {
		COMMUNITY_INFO_CACHE.del(jedis, companyIds);
	}
	
	private static final JedisValueCacheEx<Integer, CommunityDAOProtos.BoardExt> BOARD_EXT_CACHE = 
			JedisValueCacheEx.create("community:board_ext:", CommunityDAOProtos.BoardExt.PARSER);
	
	public static Map<Integer, CommunityDAOProtos.BoardExt> getBoardExt(Jedis jedis, long companyId, Collection<Integer> boardIds) {
		return BOARD_EXT_CACHE.get(jedis, companyId, boardIds);
	}
	
	public static Map<Integer, CommunityDAOProtos.BoardExt> getBoardExt(Jedis jedis, long companyId, Collection<Integer> boardIds, Collection<Integer> noCacheBoardIds) {
		return BOARD_EXT_CACHE.get(jedis, companyId, boardIds, noCacheBoardIds);
	}
	
	public static void setBoardExt(Jedis jedis, long companyId, Map<Integer, CommunityDAOProtos.BoardExt> boardExtMap) {
		BOARD_EXT_CACHE.set(jedis, companyId, boardExtMap);
	}
	
	public static void setBoardExt(Jedis jedis, long companyId, Collection<Integer> boardIds, Map<Integer, CommunityDAOProtos.BoardExt> boardExtMap) {
		BOARD_EXT_CACHE.set(jedis, companyId, boardIds, boardExtMap);
	}
	
	public static void delBoardExt(Jedis jedis, long companyId, Collection<Integer> boardIds) {
		BOARD_EXT_CACHE.del(jedis, companyId, boardIds);
	}
	
	private static final JedisValueCacheEx<Integer, CommunityProtos.Post> POST_CACHE = 
			JedisValueCacheEx.create("community:post:", CommunityProtos.Post.PARSER);
	
	public static Map<Integer, CommunityProtos.Post> getPost(Jedis jedis, long companyId, Collection<Integer> postIds) {
		return POST_CACHE.get(jedis, companyId, postIds);
	}
	
	public static Map<Integer, CommunityProtos.Post> getPost(Jedis jedis, long companyId, Collection<Integer> postIds, Collection<Integer> noCachePostIds) {
		return POST_CACHE.get(jedis, companyId, postIds, noCachePostIds);
	}
	
	public static void setPost(Jedis jedis, long companyId, Map<Integer, CommunityProtos.Post> postMap) {
		POST_CACHE.set(jedis, companyId, postMap);
	}
	
	public static void setPost(Jedis jedis, long companyId, Collection<Integer> postIds, Map<Integer, CommunityProtos.Post> postMap) {
		POST_CACHE.set(jedis, companyId, postIds, postMap);
	}
	
	public static void delPost(Jedis jedis, long companyId, Collection<Integer> postIds) {
		POST_CACHE.del(jedis, companyId, postIds);
	}
	
	private static final JedisValueCacheEx<Integer, CommunityDAOProtos.PostExt> POST_EXT_CACHE = 
			JedisValueCacheEx.create("community:post_ext:", CommunityDAOProtos.PostExt.PARSER);
	
	public static Map<Integer, CommunityDAOProtos.PostExt> getPostExt(Jedis jedis, long companyId, Collection<Integer> postIds) {
		return POST_EXT_CACHE.get(jedis, companyId, postIds);
	}
	
	public static Map<Integer, CommunityDAOProtos.PostExt> getPostExt(Jedis jedis, long companyId, Collection<Integer> postIds, Collection<Integer> noCachePostIds) {
		return POST_EXT_CACHE.get(jedis, companyId, postIds, noCachePostIds);
	}
	
	public static void setPostExt(Jedis jedis, long companyId, Map<Integer, CommunityDAOProtos.PostExt> postExtMap) {
		POST_EXT_CACHE.set(jedis, companyId, postExtMap);
	}
	
	public static void setPostExt(Jedis jedis, long companyId, Collection<Integer> postIds, Map<Integer, CommunityDAOProtos.PostExt> postExtMap) {
		POST_EXT_CACHE.set(jedis, companyId, postIds, postExtMap);
	}
	
	public static void delPostExt(Jedis jedis, long companyId, Collection<Integer> postIds) {
		POST_EXT_CACHE.del(jedis, companyId, postIds);
	}
	
	private static final JedisValueCacheEx<Integer, CommunityDAOProtos.BoardHotPostIndexList> BOARD_HOT_POST_ID_LIST_CACHE = 
			JedisValueCacheEx.create("community:board_hot_post_id_list:", CommunityDAOProtos.BoardHotPostIndexList.PARSER);
	
	public static Map<Integer, List<CommunityDAOProtos.HotPostListIndex>> getBoardHotPostListIndex(Jedis jedis, long companyId, Collection<Integer> boardIds) {
		return convertToList(BOARD_HOT_POST_ID_LIST_CACHE.get(jedis, companyId, boardIds));
	}
	
	public static Map<Integer, List<CommunityDAOProtos.HotPostListIndex>> getBoardHotPostListIndex(Jedis jedis, long companyId, Collection<Integer> boardIds, Collection<Integer> noCacheBoardIds) {
		return convertToList(BOARD_HOT_POST_ID_LIST_CACHE.get(jedis, companyId, boardIds, noCacheBoardIds));
	}
	
	public static void setBoardHotPostListIndex(Jedis jedis, long companyId, Map<Integer, List<CommunityDAOProtos.HotPostListIndex>> boardHotPostIdListMap) {
		BOARD_HOT_POST_ID_LIST_CACHE.set(jedis, companyId, convertToDAO(boardHotPostIdListMap));
	}
	
	public static void setBoardHotPostListIndex(Jedis jedis, long companyId, Collection<Integer> boardIds, Map<Integer, List<CommunityDAOProtos.HotPostListIndex>> boardHotPostIdListMap) {
		BOARD_HOT_POST_ID_LIST_CACHE.set(jedis, companyId, boardIds, convertToDAO(boardHotPostIdListMap));
	}
	
	public static void delBoardHotPostList(Jedis jedis, long companyId, Collection<Integer> boardIds) {
		BOARD_HOT_POST_ID_LIST_CACHE.del(jedis, companyId, boardIds);
	}
	
	private static Map<Integer, List<CommunityDAOProtos.HotPostListIndex>> convertToList(Map<Integer, CommunityDAOProtos.BoardHotPostIndexList> daoMap) {
		if (daoMap.isEmpty()) {
			return Collections.emptyMap();
		}
		
		Map<Integer, List<CommunityDAOProtos.HotPostListIndex>> resultMap = new HashMap<Integer, List<CommunityDAOProtos.HotPostListIndex>>(daoMap.size());
		for (Map.Entry<Integer, CommunityDAOProtos.BoardHotPostIndexList> entry : daoMap.entrySet()) {
			resultMap.put(entry.getKey(), entry.getValue().getHotPostListIndexList());
		}
		return resultMap;
	}
	
	private static Map<Integer, CommunityDAOProtos.BoardHotPostIndexList> convertToDAO(Map<Integer, List<CommunityDAOProtos.HotPostListIndex>> postIdListMap) {
		if (postIdListMap.isEmpty()) {
			return Collections.emptyMap();
		}
		
		Map<Integer, CommunityDAOProtos.BoardHotPostIndexList> resultMap = new HashMap<Integer, CommunityDAOProtos.BoardHotPostIndexList>(postIdListMap.size());
		
		CommunityDAOProtos.BoardHotPostIndexList.Builder tmpBuilder = CommunityDAOProtos.BoardHotPostIndexList.newBuilder();
		for (Map.Entry<Integer, List<CommunityDAOProtos.HotPostListIndex>> entry : postIdListMap.entrySet()) {
			tmpBuilder.clear();
			resultMap.put(entry.getKey(), tmpBuilder.addAllHotPostListIndex(entry.getValue()).build());
		}
		return resultMap;
	}
}
