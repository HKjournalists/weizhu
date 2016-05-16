package com.weizhu.service.exam;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import com.weizhu.common.db.DBUtil;
import com.weizhu.proto.ExamProtos;
import com.zaxxer.hikari.HikariDataSource;

public class ExamUtil {
	
	public static Map<Integer, ExamDAOProtos.ExamInfo> getExamInfo(HikariDataSource hikariDataSource, JedisPool jedisPool, long companyId, Collection<Integer> examIds) {
		if (examIds.isEmpty()) {
			return Collections.emptyMap();
		}
		
		Map<Integer, ExamDAOProtos.ExamInfo> examInfoMap = new TreeMap<Integer, ExamDAOProtos.ExamInfo>();
		Set<Integer> noCacheExamIdSet = new TreeSet<Integer>();
		Jedis jedis = jedisPool.getResource();
		try {
			examInfoMap.putAll(ExamCache.getExamInfo(jedis, companyId, examIds, noCacheExamIdSet));
		} finally {
			jedis.close();
		}
		
		if (noCacheExamIdSet.isEmpty()) {
			return examInfoMap;
		}
		
		Map<Integer, ExamDAOProtos.ExamInfo> noCacheExamInfoMap;
		Connection conn = null;
		try {
			conn = hikariDataSource.getConnection();
			noCacheExamInfoMap = ExamDB.getExamInfo(conn, companyId, noCacheExamIdSet);
		} catch (SQLException ex) {
			throw new RuntimeException("db fail", ex);
		} finally {
			DBUtil.closeQuietly(conn);
		}
		
		jedis = jedisPool.getResource();
		try {
			ExamCache.setExamInfo(jedis, companyId, noCacheExamIdSet, noCacheExamInfoMap);
		} finally {
			jedis.close();
		}
		
		examInfoMap.putAll(noCacheExamInfoMap);
		
		return examInfoMap;
	}
	
	public static Map<Integer, ExamProtos.Question> getQuestion(HikariDataSource hikariDataSource, JedisPool jedisPool, long companyId, Collection<Integer> questionIds) {
		if (questionIds.isEmpty()) {
			return Collections.emptyMap();
		}
		
		Map<Integer, ExamProtos.Question> questionMap = new TreeMap<Integer, ExamProtos.Question>();
		
		Set<Integer> noCacheQuestionIdSet = new TreeSet<Integer>();
		Jedis jedis = jedisPool.getResource();
		try {
			questionMap.putAll(ExamCache.getQuestion(jedis, companyId, questionIds, noCacheQuestionIdSet));
		} finally {
			jedis.close();
		}
		
		if (noCacheQuestionIdSet.isEmpty()) {
			return questionMap;
		}
		
		Map<Integer, ExamProtos.Question> noCacheQuestionMap;
		Connection conn = null;
		try {
			conn = hikariDataSource.getConnection();
			noCacheQuestionMap = ExamDB.getQuestion(conn, companyId, noCacheQuestionIdSet);
		} catch (SQLException ex) {
			throw new RuntimeException("db fail", ex);
		} finally {
			DBUtil.closeQuietly(conn);
		}
		
		jedis = jedisPool.getResource();
		try {
			ExamCache.setQuestion(jedis, companyId, noCacheQuestionIdSet, noCacheQuestionMap);
		} finally {
			jedis.close();
		}
		
		questionMap.putAll(noCacheQuestionMap);
		return questionMap;
	}
}
