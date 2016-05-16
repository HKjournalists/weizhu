package com.weizhu.service.survey;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import com.weizhu.common.db.DBUtil;
import com.weizhu.proto.SurveyProtos;
import com.zaxxer.hikari.HikariDataSource;

public class SurveyUtil {

	public static Map<Integer, SurveyProtos.Survey> getSurveyById(HikariDataSource hikariDataSource, JedisPool jedisPool, long companyId, Collection<Integer> surveyIds) {
		if (surveyIds.isEmpty()) {
			return Collections.emptyMap();
		}
		
		Map<Integer, SurveyProtos.Survey> surveyMap = new HashMap<Integer, SurveyProtos.Survey>(surveyIds.size());
		
		List<Integer> noCacheSurveyIdList = new ArrayList<Integer>();
		
		Jedis jedis = jedisPool.getResource();
		try {
			surveyMap = SurveyCache.getSurvey(jedis, companyId, surveyIds, noCacheSurveyIdList);
		} finally {
			jedis.close();
		}
		
		if (noCacheSurveyIdList.isEmpty()) {
			return surveyMap;
		}
		
		Connection conn = null;
		Map<Integer, SurveyProtos.Survey> noCacheSurveyMap = null;
		try {
			conn = hikariDataSource.getConnection();
			noCacheSurveyMap = SurveyDB.getSurveyById(conn, companyId, noCacheSurveyIdList);
		} catch (SQLException ex) {
			throw new RuntimeException("db fail", ex);
		} finally {
			DBUtil.closeQuietly(conn);
		}
		
		jedis = jedisPool.getResource();
		try {
			SurveyCache.setSurvey(jedis, companyId, noCacheSurveyIdList, noCacheSurveyMap);
		} finally {
			jedis.close();
		}
		
		surveyMap.putAll(noCacheSurveyMap);
		
		return surveyMap;
	}
	
	public static Map<Integer, SurveyProtos.Question> getSurveyQuestionById(HikariDataSource hikariDataSource, JedisPool jedisPool, long companyId, Collection<Integer> questionIds) {
		if (questionIds.isEmpty()) {
			return Collections.emptyMap();
		}
		
		Map<Integer, SurveyProtos.Question> questionMap = new HashMap<Integer, SurveyProtos.Question>();
		
		List<Integer> noCacheQuestionIdList = new ArrayList<Integer>();
		Jedis jedis = jedisPool.getResource();
		try {
			questionMap = SurveyCache.getQuestion(jedis, companyId, questionIds, noCacheQuestionIdList);
		} finally {
			jedis.close();
		}
		
		if (noCacheQuestionIdList.isEmpty()) {
			return questionMap;
		}
		
		Connection conn = null;
		Map<Integer, SurveyProtos.Question> noCacheQuestionMap = null;
		try {
			conn = hikariDataSource.getConnection();
			
			noCacheQuestionMap = SurveyDB.getQuestionById(conn, companyId, noCacheQuestionIdList);
		} catch (SQLException ex) {
			throw new RuntimeException("db fail", ex);
		} finally {
			DBUtil.closeQuietly(conn);
		}
		
		jedis = jedisPool.getResource();
		try {
			SurveyCache.setQuestion(jedis, companyId, noCacheQuestionIdList, noCacheQuestionMap);
		} finally {
			jedis.close();
		}
		
		questionMap.putAll(noCacheQuestionMap);
		
		return questionMap;
	}
	
	public static Map<Integer, SurveyDAOProtos.SurveyCount> getSurveyCount(HikariDataSource hikariDataSource, JedisPool jedisPool, long companyId, Collection<Integer> surveyIds) {
		if (surveyIds.isEmpty()) {
			return Collections.emptyMap();
		}
		
		Map<Integer, SurveyDAOProtos.SurveyCount> surveyCountMap = new HashMap<Integer, SurveyDAOProtos.SurveyCount>();
		
		List<Integer> noCacheSurveyIds = new ArrayList<Integer>();
		Jedis jedis = jedisPool.getResource();
		try {
			surveyCountMap = SurveyCache.getSurveyCount(jedis, companyId, surveyIds, noCacheSurveyIds);
		} finally {
			jedis.close();
		}
		
		if (noCacheSurveyIds.isEmpty()) {
			return surveyCountMap;
		}
		
		Connection conn = null;
		Map<Integer, SurveyDAOProtos.SurveyCount> noCacheSurveyCountMap = new HashMap<Integer, SurveyDAOProtos.SurveyCount>();
		try {
			conn = hikariDataSource.getConnection();
			
			noCacheSurveyCountMap = SurveyDB.getSurveyCount(conn, companyId, noCacheSurveyIds);
		} catch (SQLException ex) {
			
		} finally {
			DBUtil.closeQuietly(conn);
		}
		
		jedis = jedisPool.getResource();
		try {
			SurveyCache.setSurveyCount(jedis, companyId, noCacheSurveyIds, noCacheSurveyCountMap);
		} finally {
			jedis.close();
		}
		
		surveyCountMap.putAll(noCacheSurveyCountMap);
		
		return surveyCountMap;
	}
	
	public static Map<Integer, SurveyDAOProtos.QuestionCount> getQuestionCount(HikariDataSource hikariDataSource, JedisPool jedisPool, long companyId, Collection<Integer> questionIds) {
		if (questionIds.isEmpty()) {
			return Collections.emptyMap();
		}
		
		Map<Integer, SurveyDAOProtos.QuestionCount> questionCountMap = new HashMap<Integer, SurveyDAOProtos.QuestionCount>();
		
		List<Integer> noCacheQuestionIds = new ArrayList<Integer>();
		Jedis jedis = jedisPool.getResource();
		try {
			questionCountMap = SurveyCache.getQuestionCount(jedis, companyId, questionIds, noCacheQuestionIds);
		} finally {
			jedis.close();
		}
		
		if (noCacheQuestionIds.isEmpty()) {
			return questionCountMap;
		}
		
		Map<Integer, SurveyDAOProtos.QuestionCount> noCacheQuestionCountMap = new HashMap<Integer, SurveyDAOProtos.QuestionCount>();
		Connection conn = null;
		try {
			conn = hikariDataSource.getConnection();
			
			noCacheQuestionCountMap = SurveyDB.getQuestionCount(conn, companyId, noCacheQuestionIds);
		} catch (SQLException ex) {
			throw new RuntimeException("db fail", ex);
		} finally {
			DBUtil.closeQuietly(conn);
		}
		
		jedis = jedisPool.getResource();
		try {
			SurveyCache.setQuestionCount(jedis, companyId, noCacheQuestionIds, noCacheQuestionCountMap);
		} finally {
			jedis.close();
		}
		
		questionCountMap.putAll(noCacheQuestionCountMap);
		
		return questionCountMap;
	}
}
