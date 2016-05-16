package com.weizhu.service.user.level;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import com.google.common.collect.ImmutableSet;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.weizhu.common.db.DBUtil;
import com.weizhu.proto.UserProtos;
import com.weizhu.proto.AdminProtos.AdminHead;
import com.weizhu.proto.AdminUserProtos.CreateLevelRequest;
import com.weizhu.proto.AdminUserProtos.CreateLevelResponse;
import com.weizhu.proto.AdminUserProtos.DeleteLevelRequest;
import com.weizhu.proto.AdminUserProtos.DeleteLevelResponse;
import com.weizhu.proto.AdminUserProtos.UpdateLevelRequest;
import com.weizhu.proto.AdminUserProtos.UpdateLevelResponse;
import com.zaxxer.hikari.HikariDataSource;

@Singleton
public class LevelManager {
	
	private static final ImmutableSet<UserProtos.State> STATE_SET = ImmutableSet.of(UserProtos.State.NORMAL);

	private final HikariDataSource hikariDataSource;
	private final JedisPool jedisPool;
	
	@Inject
	public LevelManager(HikariDataSource hikariDataSource, JedisPool jedisPool) {
		this.hikariDataSource = hikariDataSource;
		this.jedisPool = jedisPool;
	}
	
	public Map<Integer, UserProtos.Level> getAllLevel(long companyId) {
		
		Map<Integer, UserProtos.Level> levelMap = null;
		
		Jedis jedis = jedisPool.getResource();
		try {
			levelMap = LevelCache.getAllLevel(jedis, companyId);
		} finally {
			jedis.close();
		}
		
		if (levelMap != null) {
			return levelMap;
		}
		
		Connection dbConn = null;
		try {
			dbConn = hikariDataSource.getConnection();
			levelMap = LevelDB.getAllLevel(dbConn, companyId, STATE_SET);
		} catch (SQLException e) {
			throw new RuntimeException("db fail", e);
		} finally {
			DBUtil.closeQuietly(dbConn);
		}
		
		jedis = jedisPool.getResource();
		try {
			LevelCache.setAllLevel(jedis, companyId, levelMap);
		} finally {
			jedis.close();
		}
		
		return levelMap;
	}
	
	public CreateLevelResponse createLevel(AdminHead head, CreateLevelRequest request) {
		if (!head.hasCompanyId()) {
			return CreateLevelResponse.newBuilder()
					.setResult(CreateLevelResponse.Result.FAIL_UNKNOWN)
					.setFailText("company_id 参数未填")
					.build();
		}
		
		final long companyId = head.getCompanyId();
		final long adminId = head.getSession().getAdminId();
		
		if (request.getLevelName().isEmpty()) {
			return CreateLevelResponse.newBuilder()
					.setResult(CreateLevelResponse.Result.FAIL_NAME_INVALID)
					.setFailText("职级名称为空")
					.build();
		}
		if (request.getLevelName().length() > 100) {
			return CreateLevelResponse.newBuilder()
					.setResult(CreateLevelResponse.Result.FAIL_NAME_INVALID)
					.setFailText("职级名称长度过长")
					.build();
		}
		
		UserProtos.Level level = UserProtos.Level.newBuilder()
				.setLevelId(0)
				.setLevelName(request.getLevelName())
				.setState(UserProtos.State.NORMAL)
				.setCreateAdminId(adminId)
				.setCreateTime((int) (System.currentTimeMillis() / 1000L))
				.build();
		
		int levelId;
		Map<Integer, UserProtos.Level> levelMap;
		Connection dbConn = null;
		try {
			dbConn = hikariDataSource.getConnection();
			levelId = LevelDB.insertLevel(dbConn, companyId, Collections.singletonList(level)).get(0);
			levelMap = LevelDB.getAllLevel(dbConn, companyId, STATE_SET);
		} catch (SQLException e) {
			throw new RuntimeException("db fail", e);
		} finally {
			DBUtil.closeQuietly(dbConn);
		}
		
		// update cache
		Jedis jedis = jedisPool.getResource();
		try {
			LevelCache.setAllLevel(jedis, companyId, levelMap);
		} finally {
			jedis.close();
		}
		
		return CreateLevelResponse.newBuilder()
				.setResult(CreateLevelResponse.Result.SUCC)
				.setLevelId(levelId)
				.build();
	}

	public UpdateLevelResponse updateLevel(AdminHead head, UpdateLevelRequest request) {
		if (!head.hasCompanyId()) {
			return UpdateLevelResponse.newBuilder()
					.setResult(UpdateLevelResponse.Result.FAIL_UNKNOWN)
					.setFailText("company_id 参数未填")
					.build();
		}
		
		final long companyId = head.getCompanyId();
		final long adminId = head.getSession().getAdminId();
		
		if (request.getLevelName().isEmpty()) {
			return UpdateLevelResponse.newBuilder()
					.setResult(UpdateLevelResponse.Result.FAIL_NAME_INVALID)
					.setFailText("职级名称为空")
					.build();
		}
		if (request.getLevelName().length() > 100) {
			return UpdateLevelResponse.newBuilder()
					.setResult(UpdateLevelResponse.Result.FAIL_NAME_INVALID)
					.setFailText("职级名称长度过长")
					.build();
		}
		
		final int levelId = request.getLevelId();
		final UserProtos.Level oldLevel = this.getAllLevel(companyId).get(levelId);
		if (oldLevel == null) {
			return UpdateLevelResponse.newBuilder()
					.setResult(UpdateLevelResponse.Result.FAIL_LEVEL_NOT_EXIST)
					.setFailText("职级未找到")
					.build();
		}
		
		if (oldLevel.getLevelName().equals(request.getLevelName())) {
			return UpdateLevelResponse.newBuilder()
					.setResult(UpdateLevelResponse.Result.SUCC)
					.build();
		}
		
		final UserProtos.Level newLevel = oldLevel.toBuilder()
				.setLevelName(request.getLevelName())
				.setUpdateAdminId(adminId)
				.setUpdateTime((int) (System.currentTimeMillis() / 1000L))
				.build();
		
		Map<Integer, UserProtos.Level> levelMap;
		Connection dbConn = null;
		try {
			dbConn = hikariDataSource.getConnection();
			LevelDB.updateLevel(dbConn, 
					companyId,
					Collections.singletonMap(levelId, oldLevel), 
					Collections.singletonMap(levelId, newLevel)
					);
			levelMap = LevelDB.getAllLevel(dbConn, companyId, STATE_SET);
		} catch (SQLException e) {
			throw new RuntimeException("db fail", e);
		} finally {
			DBUtil.closeQuietly(dbConn);
		}
		
		// update cache
		Jedis jedis = jedisPool.getResource();
		try {
			LevelCache.setAllLevel(jedis, companyId, levelMap);
		} finally {
			jedis.close();
		}
		
		return UpdateLevelResponse.newBuilder()
				.setResult(UpdateLevelResponse.Result.SUCC)
				.build();
	}

	public DeleteLevelResponse deleteLevel(AdminHead head, DeleteLevelRequest request) {
		if (!head.hasCompanyId()) {
			return DeleteLevelResponse.newBuilder()
					.setResult(DeleteLevelResponse.Result.FAIL_UNKNOWN)
					.setFailText("company_id 参数未填")
					.build();
		}
		
		final long companyId = head.getCompanyId();
		final long adminId = head.getSession().getAdminId();
		
		if (request.getLevelIdCount() <= 0) {
			return DeleteLevelResponse.newBuilder()
					.setResult(DeleteLevelResponse.Result.SUCC)
					.build();
		}
		
		Set<Integer> deleteLevelIdSet = new TreeSet<Integer>();
		
		Map<Integer, UserProtos.Level> levelMap = this.getAllLevel(companyId);
		for (Integer levelId : request.getLevelIdList()) {
			if (levelMap.containsKey(levelId)) {
				deleteLevelIdSet.add(levelId);
			}
		}
		
		if (deleteLevelIdSet.isEmpty()) {
			return DeleteLevelResponse.newBuilder()
					.setResult(DeleteLevelResponse.Result.SUCC)
					.build();
		}
		
		Connection dbConn = null;
		try {
			dbConn = hikariDataSource.getConnection();
			LevelDB.deleteLevel(dbConn, companyId, deleteLevelIdSet, adminId, (int) (System.currentTimeMillis() / 1000L));
			levelMap = LevelDB.getAllLevel(dbConn, companyId, STATE_SET);
		} catch (SQLException e) {
			throw new RuntimeException("db fail", e);
		} finally {
			DBUtil.closeQuietly(dbConn);
		}
		
		// update cache
		Jedis jedis = jedisPool.getResource();
		try {
			LevelCache.setAllLevel(jedis, companyId, levelMap);
		} finally {
			jedis.close();
		}
		
		return DeleteLevelResponse.newBuilder()
				.setResult(DeleteLevelResponse.Result.SUCC)
				.build();
	}
	
}
