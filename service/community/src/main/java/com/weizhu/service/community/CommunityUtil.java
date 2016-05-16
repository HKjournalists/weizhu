package com.weizhu.service.community;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import com.google.common.collect.ImmutableList;
import com.weizhu.common.db.DBUtil; 
import com.weizhu.proto.CommunityProtos;
import com.zaxxer.hikari.HikariDataSource;

public class CommunityUtil {
	public static final int MAX_BOARD_POST_LIST_INDEX_SIZE = 100;
	public static final int MAX_RECOMMENDED_POST_LIST_SIZE = 10;
	public static final int MAX_BOARD_STICKY_POST_LIST_SIZE = 10;
	
	public static final ImmutableList<CommunityProtos.Post.State> USER_POST_STATE_LIST = ImmutableList.of(CommunityProtos.Post.State.NORMAL);
	public static final ImmutableList<CommunityProtos.Comment.State> USER_COMMENT_STATE_LIST = ImmutableList.of(CommunityProtos.Comment.State.NORMAL);
	public static final ImmutableList<CommunityProtos.Post.State> ADMIN_POST_STATE_LIST = ImmutableList.of(CommunityProtos.Post.State.NORMAL);
	public static final ImmutableList<CommunityProtos.Comment.State> ADMIN_COMMENT_STATE_LIST = ImmutableList.of(CommunityProtos.Comment.State.NORMAL);
	public static final ImmutableList<CommunityProtos.Post.State> POST_ALL_STATE_LIST = ImmutableList.of(CommunityProtos.Post.State.NORMAL,
			CommunityProtos.Post.State.DELETE);

	public static CommunityDAOProtos.CommunityInfo doGetCommunityInfo(JedisPool jedisPool, HikariDataSource hikariDataSource, long companyId) {
		CommunityDAOProtos.CommunityInfo communityInfo = null;

		Jedis jedis = jedisPool.getResource();
		try {
			communityInfo = CommunityCache.getCommunityInfo(jedis, Collections.singleton(companyId)).get(companyId);
		} finally {
			jedis.close();
		}

		if (communityInfo != null) {
			return communityInfo;
		}

		Connection dbConn = null;
		try {
			dbConn = hikariDataSource.getConnection();
			communityInfo = CommunityDB.getCommunityInfo(dbConn, companyId);
		} catch (SQLException e) {
			throw new RuntimeException("db fail", e);
		} finally {
			DBUtil.closeQuietly(dbConn);
		}

		jedis = jedisPool.getResource();
		try {
			CommunityCache.setCommunityInfo(jedis, Collections.singletonMap(companyId, communityInfo));
		} finally {
			jedis.close();
		}

		return communityInfo;
	}

	public static List<CommunityProtos.Board> doGetBoard(JedisPool jedisPool, HikariDataSource hikariDataSource, long companyId, Collection<Integer> boardIds) {
		if (boardIds.isEmpty()) {
			return Collections.emptyList();
		}
		CommunityDAOProtos.CommunityInfo communityInfo = doGetCommunityInfo(jedisPool, hikariDataSource, companyId);
		if (communityInfo == null) {
			return null;
		}
		Map<Integer, CommunityProtos.Board> boardInfoMap = new HashMap<Integer, CommunityProtos.Board>();
		for (CommunityProtos.Board board : communityInfo.getBoardList()) {
			boardInfoMap.put(board.getBoardId(), board);
		}
		List<CommunityProtos.Board> boardList = new ArrayList<CommunityProtos.Board>();
		for (int boardId : boardIds) {
			CommunityProtos.Board board = boardInfoMap.get(boardId);
			if (null != board) {
				boardList.add(board);
			}

		}
		return boardList;
	}

	public static Map<Integer, CommunityProtos.Post> doGetPost(JedisPool jedisPool, HikariDataSource hikariDataSource, long companyId, 
			Collection<Integer> postIds, Collection<CommunityProtos.Post.State> states) {
		if (postIds.isEmpty() || states.isEmpty()) {
			return Collections.emptyMap();
		}

		Map<Integer, CommunityProtos.Post> resultMap = new HashMap<Integer, CommunityProtos.Post>();

		Set<Integer> noCachePostIdSet = new TreeSet<Integer>();
		Jedis jedis = jedisPool.getResource();
		try {
			resultMap.putAll(CommunityCache.getPost(jedis, companyId, postIds, noCachePostIdSet));
		} finally {
			jedis.close();
		}

		if (!noCachePostIdSet.isEmpty()) {
			Map<Integer, CommunityProtos.Post> noCacheMap;
			Connection dbConn = null;
			try {
				dbConn = hikariDataSource.getConnection();
				noCacheMap = CommunityDB.getPost(dbConn, companyId, noCachePostIdSet);
			} catch (SQLException e) {
				throw new RuntimeException("db fail", e);
			} finally {
				DBUtil.closeQuietly(dbConn);
			}

			jedis = jedisPool.getResource();
			try {
				CommunityCache.setPost(jedis, companyId, noCachePostIdSet, noCacheMap);
			} finally {
				jedis.close();
			}

			resultMap.putAll(noCacheMap);
		}

		Iterator<CommunityProtos.Post> it = resultMap.values().iterator();
		while (it.hasNext()) {
			CommunityProtos.Post post = it.next();
			if (!states.contains(post.getState())) {
				it.remove();
			}
		}

		return resultMap;
	}

	public static Map<Integer, CommunityDAOProtos.PostExt> doGetPostExt(JedisPool jedisPool, HikariDataSource hikariDataSource, long companyId, 
			Collection<Integer> postIds) {
		if (postIds.isEmpty()) {
			return Collections.emptyMap();
		}

		Map<Integer, CommunityDAOProtos.PostExt> resultMap = new HashMap<Integer, CommunityDAOProtos.PostExt>();

		Set<Integer> noCachePostIdSet = new TreeSet<Integer>();
		Jedis jedis = jedisPool.getResource();
		try {
			resultMap.putAll(CommunityCache.getPostExt(jedis, companyId, postIds, noCachePostIdSet));
		} finally {
			jedis.close();
		}

		if (noCachePostIdSet.isEmpty()) {
			return resultMap;
		}

		Map<Integer, CommunityDAOProtos.PostExt> noCacheMap;
		Connection dbConn = null;
		try {
			dbConn = hikariDataSource.getConnection();
			noCacheMap = CommunityDB.getPostExt(dbConn, companyId, noCachePostIdSet);
		} catch (SQLException e) {
			throw new RuntimeException("db fail", e);
		} finally {
			DBUtil.closeQuietly(dbConn);
		}

		jedis = jedisPool.getResource();
		try {
			CommunityCache.setPostExt(jedis, companyId, noCachePostIdSet, noCacheMap);
		} finally {
			jedis.close();
		}

		resultMap.putAll(noCacheMap);
		return resultMap;
	}

	public static Map<Integer, CommunityDAOProtos.BoardExt> doGetBoardExt(JedisPool jedisPool, HikariDataSource hikariDataSource, long companyId, 
			Collection<Integer> boardIds) {
		if (boardIds.isEmpty()) {
			return Collections.emptyMap();
		}

		Map<Integer, CommunityDAOProtos.BoardExt> resultMap = new HashMap<Integer, CommunityDAOProtos.BoardExt>();

		Set<Integer> noCacheBoardIdSet = new TreeSet<Integer>();
		Jedis jedis = jedisPool.getResource();
		try {
			resultMap.putAll(CommunityCache.getBoardExt(jedis, companyId, boardIds, noCacheBoardIdSet));
		} finally {
			jedis.close();
		}

		if (noCacheBoardIdSet.isEmpty()) {
			return resultMap;
		}

		Map<Integer, CommunityDAOProtos.BoardExt> noCacheMap;
		Connection dbConn = null;
		try {
			dbConn = hikariDataSource.getConnection();
			noCacheMap = CommunityDB.getBoardExt(dbConn, companyId, noCacheBoardIdSet, MAX_BOARD_POST_LIST_INDEX_SIZE);
		} catch (SQLException e) {
			throw new RuntimeException("db fail", e);
		} finally {
			DBUtil.closeQuietly(dbConn);
		}

		jedis = jedisPool.getResource();
		try {
			CommunityCache.setBoardExt(jedis, companyId, noCacheBoardIdSet, noCacheMap);
		} finally {
			jedis.close();
		}

		resultMap.putAll(noCacheMap);
		return resultMap;
	}


}
