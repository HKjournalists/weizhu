package com.weizhu.service.user.position;

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
import com.weizhu.proto.AdminUserProtos.DeletePositionRequest;
import com.weizhu.proto.AdminUserProtos.DeletePositionResponse;
import com.weizhu.proto.UserProtos;
import com.weizhu.proto.AdminProtos.AdminHead;
import com.weizhu.proto.AdminUserProtos.CreatePositionRequest;
import com.weizhu.proto.AdminUserProtos.CreatePositionResponse;
import com.weizhu.proto.AdminUserProtos.UpdatePositionRequest;
import com.weizhu.proto.AdminUserProtos.UpdatePositionResponse;
import com.zaxxer.hikari.HikariDataSource;

@Singleton
public class PositionManager {

	private static final ImmutableSet<UserProtos.State> STATE_SET = ImmutableSet.of(UserProtos.State.NORMAL);
	
	private final HikariDataSource hikariDataSource;
	private final JedisPool jedisPool;
	
	@Inject
	public PositionManager(HikariDataSource hikariDataSource, JedisPool jedisPool) {
		this.hikariDataSource = hikariDataSource;
		this.jedisPool = jedisPool;
	}
	
	public Map<Integer, UserProtos.Position> getAllPosition(long companyId) {
		
		Map<Integer, UserProtos.Position> positionMap = null;
		
		Jedis jedis = jedisPool.getResource();
		try {
			positionMap = PositionCache.getAllPosition(jedis, companyId);
		} finally {
			jedis.close();
		}
		
		if (positionMap != null) {
			return positionMap;
		}
		
		Connection dbConn = null;
		try {
			dbConn = hikariDataSource.getConnection();
			positionMap = PositionDB.getAllPosition(dbConn, companyId, STATE_SET);
		} catch (SQLException e) {
			throw new RuntimeException("db fail", e);
		} finally {
			DBUtil.closeQuietly(dbConn);
		}
		
		jedis = jedisPool.getResource();
		try {
			PositionCache.setAllPosition(jedis, companyId, positionMap);
		} finally {
			jedis.close();
		}
		
		return positionMap;
	}
	
	public CreatePositionResponse createPosition(AdminHead head, CreatePositionRequest request) {
		if (!head.hasCompanyId()) {
			return CreatePositionResponse.newBuilder()
					.setResult(CreatePositionResponse.Result.FAIL_UNKNOWN)
					.setFailText("company_id 参数未填")
					.build();
		}
		
		final long companyId = head.getCompanyId();
		final long adminId = head.getSession().getAdminId();
		
		if (request.getPositionName().isEmpty()) {
			return CreatePositionResponse.newBuilder()
					.setResult(CreatePositionResponse.Result.FAIL_NAME_INVALID)
					.setFailText("职位名称为空")
					.build();
		}
		if (request.getPositionName().length() > 100) {
			return CreatePositionResponse.newBuilder()
					.setResult(CreatePositionResponse.Result.FAIL_NAME_INVALID)
					.setFailText("职位名称长度过长")
					.build();
		}
		if (request.getPositionDesc().length() > 100) {
			return CreatePositionResponse.newBuilder()
					.setResult(CreatePositionResponse.Result.FAIL_DESC_INVALID)
					.setFailText("职位描述长度过长")
					.build();
		}
		
		UserProtos.Position position = UserProtos.Position.newBuilder()
				.setPositionId(0)
				.setPositionName(request.getPositionName())
				.setPositionDesc(request.getPositionDesc())
				.setState(UserProtos.State.NORMAL)
				.setCreateAdminId(adminId)
				.setCreateTime((int) (System.currentTimeMillis() / 1000L))
				.build();
		
		int positionId;
		Map<Integer, UserProtos.Position> positionMap;
		Connection dbConn = null;
		try {
			dbConn = hikariDataSource.getConnection();
			positionId = PositionDB.insertPosition(dbConn, companyId, Collections.singletonList(position)).get(0);
			positionMap = PositionDB.getAllPosition(dbConn, companyId, STATE_SET);
		} catch (SQLException e) {
			throw new RuntimeException("db fail", e);
		} finally {
			DBUtil.closeQuietly(dbConn);
		}
		
		// update cache
		Jedis jedis = jedisPool.getResource();
		try {
			PositionCache.setAllPosition(jedis, companyId, positionMap);
		} finally {
			jedis.close();
		}
		
		return CreatePositionResponse.newBuilder()
				.setResult(CreatePositionResponse.Result.SUCC)
				.setPositionId(positionId)
				.build();
	}

	public UpdatePositionResponse updatePosition(AdminHead head, UpdatePositionRequest request) {
		if (!head.hasCompanyId()) {
			return UpdatePositionResponse.newBuilder()
					.setResult(UpdatePositionResponse.Result.FAIL_UNKNOWN)
					.setFailText("company_id 参数未填")
					.build();
		}
		
		final long companyId = head.getCompanyId();
		final long adminId = head.getSession().getAdminId();
		
		if (request.getPositionName().isEmpty()) {
			return UpdatePositionResponse.newBuilder()
					.setResult(UpdatePositionResponse.Result.FAIL_NAME_INVALID)
					.setFailText("职位名称为空")
					.build();
		}
		if (request.getPositionName().length() > 100) {
			return UpdatePositionResponse.newBuilder()
					.setResult(UpdatePositionResponse.Result.FAIL_NAME_INVALID)
					.setFailText("职位名称长度过长")
					.build();
		}
		if (request.getPositionDesc().length() > 100) {
			return UpdatePositionResponse.newBuilder()
					.setResult(UpdatePositionResponse.Result.FAIL_DESC_INVALID)
					.setFailText("职位描述长度过长")
					.build();
		}
		
		final int positionId = request.getPositionId();
		
		UserProtos.Position oldPosition = this.getAllPosition(companyId).get(positionId);
		if (oldPosition == null) {
			return UpdatePositionResponse.newBuilder()
					.setResult(UpdatePositionResponse.Result.FAIL_POSITION_NOT_EXIST)
					.setFailText("职位未找到")
					.build();
		}
		
		if (oldPosition.getPositionName().equals(request.getPositionName()) 
				&& oldPosition.getPositionDesc().equals(request.getPositionDesc())) {
			return UpdatePositionResponse.newBuilder()
					.setResult(UpdatePositionResponse.Result.SUCC)
					.build();
		}
		
		UserProtos.Position newPosition = oldPosition.toBuilder()
				.setPositionName(request.getPositionName())
				.setPositionDesc(request.getPositionDesc())
				.setUpdateAdminId(adminId)
				.setUpdateTime((int) (System.currentTimeMillis() / 1000L))
				.build();
		
		Map<Integer, UserProtos.Position> positionMap;
		Connection dbConn = null;
		try {
			dbConn = hikariDataSource.getConnection();
			
			PositionDB.updatePosition(dbConn,
					companyId,
					Collections.singletonMap(positionId, oldPosition), 
					Collections.singletonMap(positionId, newPosition)
					);
			
			positionMap = PositionDB.getAllPosition(dbConn, companyId, STATE_SET);
			
		} catch (SQLException e) {
			throw new RuntimeException("db fail", e);
		} finally {
			DBUtil.closeQuietly(dbConn);
		}
		
		// update cache
		Jedis jedis = jedisPool.getResource();
		try {
			PositionCache.setAllPosition(jedis, companyId, positionMap);
		} finally {
			jedis.close();
		}
		
		return UpdatePositionResponse.newBuilder()
				.setResult(UpdatePositionResponse.Result.SUCC)
				.build();
	}
	
	public DeletePositionResponse deletePosition(AdminHead head, DeletePositionRequest request) {
		if (!head.hasCompanyId()) {
			return DeletePositionResponse.newBuilder()
					.setResult(DeletePositionResponse.Result.FAIL_UNKNOWN)
					.setFailText("company_id 参数未填")
					.build();
		}
		
		final long companyId = head.getCompanyId();
		final long adminId = head.getSession().getAdminId();
		
		if (request.getPositionIdCount() <= 0) {
			return DeletePositionResponse.newBuilder()
					.setResult(DeletePositionResponse.Result.SUCC)
					.build();
		}
		
		Set<Integer> deletePositionIdSet = new TreeSet<Integer>();
		
		Map<Integer, UserProtos.Position> positionMap = this.getAllPosition(companyId);
		for (Integer positionId : request.getPositionIdList()) {
			if (positionMap.containsKey(positionId)) {
				deletePositionIdSet.add(positionId);
			}
		}
		
		if (deletePositionIdSet.isEmpty()) {
			return DeletePositionResponse.newBuilder()
					.setResult(DeletePositionResponse.Result.SUCC)
					.build();
		}
		
		Connection dbConn = null;
		try {
			dbConn = hikariDataSource.getConnection();
			PositionDB.deletePosition(dbConn, companyId, deletePositionIdSet, adminId, (int) (System.currentTimeMillis() / 1000L));
			positionMap = PositionDB.getAllPosition(dbConn, companyId, STATE_SET);
		} catch (SQLException e) {
			throw new RuntimeException("db fail", e);
		} finally {
			DBUtil.closeQuietly(dbConn);
		}
		
		// update cache
		Jedis jedis = jedisPool.getResource();
		try {
			PositionCache.setAllPosition(jedis, companyId, positionMap);
		} finally {
			jedis.close();
		}
		
		return DeletePositionResponse.newBuilder()
				.setResult(DeletePositionResponse.Result.SUCC)
				.build();
	}
	
}
