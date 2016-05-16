package com.weizhu.service.absence;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.weizhu.common.db.DBUtil;
import com.weizhu.proto.AbsenceProtos;
import com.zaxxer.hikari.HikariDataSource;

public class AbsenceUtil {

	public static Map<Integer, AbsenceProtos.Absence> getAbsence(HikariDataSource hikariDataSource, JedisPool jedisPool, long companyId, Collection<Integer> absenceIds) {
		
		Map<Integer, AbsenceProtos.Absence> absenceMap = Maps.newHashMap();
		
		List<Integer> noCacheAbsenceIdList = Lists.newArrayList();
		Jedis jedis = jedisPool.getResource();
		try {
			absenceMap = AbsenceCache.getAbsence(jedis, companyId, absenceIds, noCacheAbsenceIdList);
		} finally {
			jedis.close();
		}
		
		if (noCacheAbsenceIdList.isEmpty()) {
			return absenceMap;
		}
		
		Map<Integer, AbsenceProtos.Absence> noCacheAbsenceMap = Maps.newHashMap();
		Connection conn = null;
		try {
			conn = hikariDataSource.getConnection();
			
			noCacheAbsenceMap = AbsenceDB.getAbsence(conn, companyId, noCacheAbsenceIdList);
		} catch (SQLException ex) {
			throw new RuntimeException("db fail", ex);
		} finally {
			DBUtil.closeQuietly(conn);
		}
		
		jedis = jedisPool.getResource();
		
		try {
			AbsenceCache.setAbsence(jedis, companyId, noCacheAbsenceIdList, noCacheAbsenceMap);
		} finally {
			jedis.close();
		}
		
		absenceMap.putAll(noCacheAbsenceMap);
		
		return absenceMap;
	}
	
}
