package com.weizhu.service.user.mark;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.weizhu.common.db.DBUtil;
import com.weizhu.proto.UserProtos;
import com.weizhu.proto.UserProtos.MarkUserNameRequest;
import com.weizhu.proto.UserProtos.MarkUserNameResponse;
import com.weizhu.proto.UserProtos.MarkUserStarRequest;
import com.weizhu.proto.UserProtos.MarkUserStarResponse;
import com.weizhu.proto.WeizhuProtos.RequestHead;
import com.zaxxer.hikari.HikariDataSource;

@Singleton
public class UserMarkManager {

	private final HikariDataSource hikariDataSource;
	private final JedisPool jedisPool;
	
	@Inject
	public UserMarkManager(HikariDataSource hikariDataSource, JedisPool jedisPool) {
		this.hikariDataSource = hikariDataSource;
		this.jedisPool = jedisPool;
	}
	
	public Map<Long, Map<Long, UserProtos.UserMark>> getUserMark(long companyId, Collection<Long> markerIds) {
		if (markerIds.isEmpty()) {
			return Collections.emptyMap();
		}
		
		Map<Long, Map<Long, UserProtos.UserMark>> userMarkMap = new HashMap<Long, Map<Long, UserProtos.UserMark>>(markerIds.size());
		
		Set<Long> noCacheMarkerIdSet = new TreeSet<Long>();
		Jedis jedis = jedisPool.getResource();
		try {
			userMarkMap.putAll(UserMarkCache.getUserMark(jedis, companyId, markerIds, noCacheMarkerIdSet));
		} finally {
			jedis.close();
		}
		
		if (noCacheMarkerIdSet.isEmpty()) {
			return userMarkMap;
		}
		
		Map<Long, Map<Long, UserProtos.UserMark>> noCacheUserMarkMap;
		Connection dbConn = null;
		try {
			dbConn = hikariDataSource.getConnection();
			noCacheUserMarkMap = UserMarkDB.getUserMark(dbConn, companyId, noCacheMarkerIdSet);
		} catch (SQLException e) {
			throw new RuntimeException("db fail", e);
		} finally {
			DBUtil.closeQuietly(dbConn);
		}
		
		jedis = jedisPool.getResource();
		try {
			UserMarkCache.setUserMark(jedis, companyId, noCacheMarkerIdSet, noCacheUserMarkMap);
		} finally {
			jedis.close();
		}
		
		userMarkMap.putAll(noCacheUserMarkMap);
		
		return userMarkMap;
	}
	
	public MarkUserNameResponse markUserName(RequestHead head, MarkUserNameRequest request) {
		final long companyId = head.getSession().getCompanyId();
		final long markerId = head.getSession().getUserId();
		final long userId = request.getUserId();
		
		if (markerId == userId) {
			return MarkUserNameResponse.newBuilder()
					.setResult(MarkUserNameResponse.Result.FAIL_MARK_SELF)
					.setFailText("不能标记自己")
					.build();
		}
		
		final UserProtos.UserMark mark;
		
		if (request.hasMarkName()) {
			// 添加标记名称
			final String markName = request.getMarkName().trim();
			if (markName.isEmpty()) {
				return MarkUserNameResponse.newBuilder()
						.setResult(MarkUserNameResponse.Result.FAIL_MARK_NAME_INVALID)
						.setFailText("标记名称不能为空")
						.build();
			}
			if (markName.length() > 20) {
				return MarkUserNameResponse.newBuilder()
						.setResult(MarkUserNameResponse.Result.FAIL_MARK_NAME_INVALID)
						.setFailText("标记名称最多20个字")
						.build();
			}
			
			Map<Long, UserProtos.UserMark> userMarkMap = this.getUserMark(companyId, Collections.singleton(markerId)).get(markerId);
			UserProtos.UserMark oldMark = userMarkMap == null ? null : userMarkMap.get(userId);
			
			if (oldMark != null && oldMark.hasMarkName() && oldMark.getMarkName().equals(markName)) {
				return MarkUserNameResponse.newBuilder()
						.setResult(MarkUserNameResponse.Result.SUCC)
						.build();
			}
			
			if (oldMark == null) {
				mark = UserProtos.UserMark.newBuilder()
						.setUserId(userId)
						.setIsStar(false)
						.setMarkName(markName)
						.build();
			} else {
				mark = oldMark.toBuilder()
						.setMarkName(markName)
						.build();
			}
		} else {
			// 删除标记名称
			Map<Long, UserProtos.UserMark> userMarkMap = this.getUserMark(companyId, Collections.singleton(markerId)).get(markerId);
			UserProtos.UserMark oldMark = userMarkMap == null ? null : userMarkMap.get(userId);
			
			if (oldMark == null || !oldMark.hasMarkName()) {
				return MarkUserNameResponse.newBuilder()
						.setResult(MarkUserNameResponse.Result.SUCC)
						.build();
			}
			
			if (oldMark.getIsStar()) {
				mark = oldMark.toBuilder()
						.clearMarkName()
						.build();
			} else {
				//既没有星标 又没有标记名称，删掉吧
				mark = null;
			}
		}
		
		Map<Long, Map<Long, UserProtos.UserMark>> resultMap;
		Connection dbConn = null;
		try {
			dbConn = hikariDataSource.getConnection();
			
			if (mark == null) {
				UserMarkDB.deleteUserMark(dbConn, companyId, markerId, userId);
			} else {
				UserMarkDB.replaceUserMark(dbConn, companyId, markerId, mark);
			}
			
			resultMap = UserMarkDB.getUserMark(dbConn, companyId, Collections.singleton(markerId));
		} catch (SQLException e) {
			throw new RuntimeException("db fail", e);
		} finally {
			DBUtil.closeQuietly(dbConn);
		}
		
		Jedis jedis = this.jedisPool.getResource();
		try {
			UserMarkCache.setUserMark(jedis, companyId, Collections.singleton(markerId), resultMap);
		} finally {
			jedis.close();
		}
		
		return MarkUserNameResponse.newBuilder()
				.setResult(MarkUserNameResponse.Result.SUCC)
				.build();
	}
	
	public MarkUserStarResponse markUserStar(RequestHead head, MarkUserStarRequest request) {
		final long companyId = head.getSession().getCompanyId();
		final long markerId = head.getSession().getUserId();
		final long userId = request.getUserId();
		
		if (markerId == userId) {
			return MarkUserStarResponse.newBuilder()
					.setResult(MarkUserStarResponse.Result.FAIL_MARK_SELF)
					.setFailText("不能标星自己")
					.build();
		}
		
		final UserProtos.UserMark mark;
		
		if (request.getIsStar()) {
			Map<Long, UserProtos.UserMark> userMarkMap = this.getUserMark(companyId, Collections.singleton(markerId)).get(markerId);
			
			if (userMarkMap != null) {
				int starNum = 0;
				for (UserProtos.UserMark m : userMarkMap.values()) {
					if (m.getIsStar()) {
						starNum ++;
					}
				}
				if (starNum >= 20) {
					return MarkUserStarResponse.newBuilder()
							.setResult(MarkUserStarResponse.Result.FAIL_MARK_STAR_NUM_LIMIT)
							.setFailText("您最多可以标星20个用户")
							.build();
				}
			}
			
			UserProtos.UserMark oldMark = userMarkMap == null ? null : userMarkMap.get(userId);
			
			if (oldMark != null && oldMark.getIsStar()) {
				return MarkUserStarResponse.newBuilder()
						.setResult(MarkUserStarResponse.Result.SUCC)
						.build();
			}
			
			if (oldMark == null) {
				mark = UserProtos.UserMark.newBuilder()
						.setUserId(userId)
						.setIsStar(true)
						.setStarTime((int) (System.currentTimeMillis() / 1000L))
						.build();
			} else {
				mark = oldMark.toBuilder()
						.setIsStar(true)
						.setStarTime((int) (System.currentTimeMillis() / 1000L))
						.build();
			}
		} else {
			Map<Long, UserProtos.UserMark> userMarkMap = this.getUserMark(companyId, Collections.singleton(markerId)).get(markerId);
			UserProtos.UserMark oldMark = userMarkMap == null ? null : userMarkMap.get(userId);
			
			if (oldMark == null || !oldMark.getIsStar()) {
				return MarkUserStarResponse.newBuilder()
						.setResult(MarkUserStarResponse.Result.SUCC)
						.build();
			}
			
			if (oldMark.hasMarkName()) {
				mark = oldMark.toBuilder()
						.setIsStar(false)
						.build();
			} else {
				mark = null;
			}
		}
		
		Map<Long, Map<Long, UserProtos.UserMark>> resultMap;
		Connection dbConn = null;
		try {
			dbConn = hikariDataSource.getConnection();
			
			if (mark == null) {
				UserMarkDB.deleteUserMark(dbConn, companyId, markerId, userId);
			} else {
				UserMarkDB.replaceUserMark(dbConn, companyId, markerId, mark);
			}
			
			resultMap = UserMarkDB.getUserMark(dbConn, companyId, Collections.singleton(markerId));
		} catch (SQLException e) {
			throw new RuntimeException("db fail", e);
		} finally {
			DBUtil.closeQuietly(dbConn);
		}
		
		Jedis jedis = this.jedisPool.getResource();
		try {
			UserMarkCache.setUserMark(jedis, companyId, Collections.singleton(markerId), resultMap);
		} finally {
			jedis.close();
		}
		
		return MarkUserStarResponse.newBuilder()
				.setResult(MarkUserStarResponse.Result.SUCC)
				.build();
	}
	
	private static final Comparator<UserProtos.UserMark> MARK_STAR_USER_CMP = new Comparator<UserProtos.UserMark>() {

		@Override
		public int compare(UserProtos.UserMark o1, UserProtos.UserMark o2) {
			if (o1.getStarTime() == o2.getStarTime()) {
				if (o1.getUserId() == o2.getUserId()) {
					return 0;
				} else {
					return o1.getUserId() < o2.getUserId() ? -1 : 1;
				}
			}
			return o1.getStarTime() > o2.getStarTime() ? -1 : 1;
		}
		
	};
	
	/**
	 * 获取标星用户，按照标星时间倒序排列
	 * @param markerId
	 * @return
	 */
	public Map<Long, UserProtos.UserMark> getMarkStarUser(long companyId, long markerId) {
		Map<Long, UserProtos.UserMark> userMarkMap = this.getUserMark(companyId, Collections.singleton(markerId)).get(markerId);
		if (userMarkMap == null || userMarkMap.isEmpty()) {
			return Collections.emptyMap();
		}
		
		List<UserProtos.UserMark> userMarkList = new ArrayList<UserProtos.UserMark>(5);
		for (UserProtos.UserMark mark : userMarkMap.values()) {
			if (mark.getIsStar()) {
				userMarkList.add(mark);
			}
		}
		
		if (userMarkList.isEmpty()) {
			return Collections.emptyMap();
		}
		
		Collections.sort(userMarkList, MARK_STAR_USER_CMP);
		
		Map<Long, UserProtos.UserMark> resultMap = new LinkedHashMap<Long, UserProtos.UserMark>(userMarkList.size());
		for (UserProtos.UserMark mark : userMarkList) {
			resultMap.put(mark.getUserId(), mark);
		}
		
		return resultMap;
	}
	
}
