package com.weizhu.service.user.exts;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import com.google.inject.Inject;
import com.weizhu.common.db.DBUtil;
import com.weizhu.proto.UserProtos;
import com.zaxxer.hikari.HikariDataSource;

public class UserExtendsManager {

	private final HikariDataSource hikariDataSource;
	private final JedisPool jedisPool;
	
	@Inject
	public UserExtendsManager(HikariDataSource hikariDataSource, JedisPool jedisPool) {
		this.hikariDataSource = hikariDataSource;
		this.jedisPool = jedisPool;
	}
	
	public Map<Long, List<UserProtos.UserExtends>> getUserExtends(long companyId, Collection<Long> userIds) {
		if (userIds.isEmpty()) {
			return Collections.emptyMap();
		}
		
		Map<Long, List<UserProtos.UserExtends>> extendsMap = new HashMap<Long, List<UserProtos.UserExtends>>();
		
		Set<Long> noCacheUserIdSet = new TreeSet<Long>();
		Jedis jedis = this.jedisPool.getResource();
		try {
			extendsMap.putAll(UserExtendsCache.getUserExtends(jedis, companyId, userIds, noCacheUserIdSet));
		} finally {
			jedis.close();
		}
		
		if (noCacheUserIdSet.isEmpty()) {
			return extendsMap;
		}
		
		Map<Long, List<UserProtos.UserExtends>> noCacheExtendsMap;
		Connection dbConn = null;
		try {
			dbConn = hikariDataSource.getConnection();
			noCacheExtendsMap = UserExtendsDB.getUserExtends(dbConn, companyId, noCacheUserIdSet);
		} catch (SQLException e) {
			throw new RuntimeException("db fail", e);
		} finally {
			DBUtil.closeQuietly(dbConn);
		}
		
		jedis = jedisPool.getResource();
		try {
			UserExtendsCache.setUserExtends(jedis, companyId, noCacheUserIdSet, noCacheExtendsMap);
		} finally {
			jedis.close();
		}
		
		extendsMap.putAll(noCacheExtendsMap);
		
		return extendsMap;
	}
	
	public void updateUserExtends(long companyId, 
			Map<Long, List<UserProtos.UserExtends>> oldUserExtendsMap,
			Map<Long, List<UserProtos.UserExtends>> newUserExtendsMap
			) {
		
		// 必须保证key完全一样
		if (!oldUserExtendsMap.keySet().equals(newUserExtendsMap.keySet())) {
			throw new IllegalArgumentException("update key not equals");
		}
		
		Map<Long, List<UserProtos.UserExtends>> userExtendsMap;
		Connection dbConn = null;
		try {
			dbConn = this.hikariDataSource.getConnection();
			UserExtendsDB.updateUserExtends(dbConn, companyId, oldUserExtendsMap, newUserExtendsMap);
			userExtendsMap = UserExtendsDB.getUserExtends(dbConn, companyId, newUserExtendsMap.keySet());
		} catch (SQLException e) {
			throw new RuntimeException("db fail", e);
		} finally {
			DBUtil.closeQuietly(dbConn);
		}
		
		Jedis jedis = jedisPool.getResource();
		try {
			UserExtendsCache.setUserExtends(jedis, companyId, newUserExtendsMap.keySet(), userExtendsMap);
		} finally {
			jedis.close();
		}
	}
	
	public List<String> getUserExtendsName(long companyId) {
		Connection dbConn = null;
		try {
			dbConn = this.hikariDataSource.getConnection();
			return UserExtendsDB.getUserExtendsName(dbConn, companyId);
		} catch (SQLException e) {
			throw new RuntimeException("db fail", e);
		} finally {
			DBUtil.closeQuietly(dbConn);
		}
	}
	
}
