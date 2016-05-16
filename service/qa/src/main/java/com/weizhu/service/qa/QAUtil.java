package com.weizhu.service.qa;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.Map.Entry;

import javax.annotation.Nullable;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import com.weizhu.common.db.DBUtil;
import com.weizhu.proto.QAProtos;
import com.weizhu.proto.QAProtos.Answer;
import com.weizhu.proto.QAProtos.Category;
import com.weizhu.proto.QAProtos.Question;
import com.zaxxer.hikari.HikariDataSource;

public class QAUtil {
	public static final int CATEGORY_MAX_NUMBER = 6;

	/**
	 * 从缓存中获取Question，如果缓存中没有，则从db中获取
	 * 
	 * @param hikariDataSource
	 * @param jedisPool
	 * @param companyId
	 * @param questionIds
	 * @return
	 * Map<Integer,QAProtos.Question>
	 * @throws
	 */
	public static Map<Integer, QAProtos.Question> getQuestion(HikariDataSource hikariDataSource, JedisPool jedisPool, long companyId, Collection<Integer> questionIds) {
		if (questionIds.isEmpty()) {
			return Collections.emptyMap();
		}
		Map<Integer, QAProtos.Question> questionInfoMap = new HashMap<Integer, QAProtos.Question>(questionIds.size());
		Set<Integer> noCacheQuestionIdSet = new TreeSet<Integer>();
		Jedis jedis = jedisPool.getResource();
		try {
			questionInfoMap.putAll(QACache.getQuestion(jedis, companyId, questionIds, noCacheQuestionIdSet));
		} finally {
			jedis.close();
		}

		if (!noCacheQuestionIdSet.isEmpty()) {
			Connection conn = null;
			Map<Integer, QAProtos.Question> noCacheQuestionInfoMap;
			try {
				conn = hikariDataSource.getConnection();
				noCacheQuestionInfoMap = QADB.getQuestion(conn, companyId, noCacheQuestionIdSet);
			} catch (SQLException e) {
				throw new RuntimeException("db fail", e);
			} finally {
				DBUtil.closeQuietly(conn);
			}
			jedis = jedisPool.getResource();
			try {
				QACache.setQuestion(jedis, companyId, noCacheQuestionIdSet, noCacheQuestionInfoMap);
			} finally {
				jedis.close();
			}
			questionInfoMap.putAll(noCacheQuestionInfoMap);
		}

		// get answer_num of question
		Map<Integer, Integer> quesAnswerNumMap = null;
		Connection conn = null;
		try {
			conn = hikariDataSource.getConnection();
			quesAnswerNumMap = QADB.getAnswerNum(conn, companyId, questionIds);
		} catch (SQLException e) {
			throw new RuntimeException("db failed", e);
		} finally {
			DBUtil.closeQuietly(conn);
		}
		Question.Builder questionBuilder = Question.newBuilder();
		for (Entry<Integer, Question> entry : questionInfoMap.entrySet()) {
			questionBuilder.clear();
			questionBuilder.mergeFrom(entry.getValue());
			int anwerNum;
			if (quesAnswerNumMap.isEmpty() || quesAnswerNumMap.get(entry.getKey()) == null) {
				anwerNum = 0;
			} else {
				anwerNum = quesAnswerNumMap.get(entry.getKey());
			}
			questionBuilder.setAnswerNum(anwerNum);
			entry.setValue(questionBuilder.build());
		}
		return questionInfoMap;
	}

	/**
	 * 创建问题，并更新缓存
	 * 
	 * @param hikariDataSource
	 * @param jedisPool
	 * @param companyId
	 * @param qConList
	 * @param userId
	 * @param adminId
	 * @param categoryId
	 * @return
	 * List<Integer>
	 * @throws
	 */
	public static List<Integer> addQuestion(HikariDataSource hikariDataSource, JedisPool jedisPool, long companyId, List<String> qConList, @Nullable Long userId,
			@Nullable Long adminId, int categoryId) {

		Connection dbConn = null;
		List<Integer> questionIds = null;
		int createTime = (int) (System.currentTimeMillis() / 1000L);
		try {
			dbConn = hikariDataSource.getConnection();
			questionIds = QADB.insertQuestion(dbConn, companyId, qConList, userId, adminId, categoryId, createTime);
			if (questionIds.size() != qConList.size()) {
				throw new RuntimeException("插入数据出错");
			}

		} catch (Exception e) {
			throw new RuntimeException("db fail", e);
		} finally {
			DBUtil.closeQuietly(dbConn);
		}

		Jedis jedis = jedisPool.getResource();
		try {

			if (questionIds.size() == 1) {
				// 将新添加的question写入缓存
				QAProtos.Question.Builder questionBuilder = QAProtos.Question.newBuilder();
				int questionId = questionIds.iterator().next();
				questionBuilder.setQuestionId(questionId);
				questionBuilder.setQuestionContent(qConList.get(0));
				if (userId != null) {
					questionBuilder.setUserId(userId);
				}
				questionBuilder.setCategoryId(categoryId);
				questionBuilder.setCreateTime(createTime);
				if (adminId != null) {
					questionBuilder.setAdminId(adminId);
				}

				QACache.setQuestion(jedis, companyId, questionIds, Collections.singletonMap(questionId, questionBuilder.build()));
			} else {
				QACache.delQuestion(jedis, companyId, questionIds);
			}

		} finally {
			jedis.close();
		}
		return questionIds;
	}

	/**
	 * 从缓存中获取回答，缓存中没有则从db中获取
	 * 
	 * @param hikariDataSource
	 * @param jedisPool
	 * @param companyId
	 * @param answerIdList
	 * @param userId
	 * @return
	 * Map<Integer,QAProtos.Answer>
	 * @throws
	 */
	public static Map<Integer, QAProtos.Answer> getAnswer(HikariDataSource hikariDataSource, JedisPool jedisPool, long companyId, Collection<Integer> answerIdList,
			@Nullable Long userId) {
		if (answerIdList.isEmpty()) {
			return Collections.emptyMap();
		}
		// get answerInfo from redis or DB
		Map<Integer, QAProtos.Answer> answerInfoMap = new HashMap<Integer, QAProtos.Answer>(answerIdList.size());

		Set<Integer> noCacheAnswerIdSet = new TreeSet<Integer>();
		Jedis jedis = jedisPool.getResource();
		try {
			answerInfoMap.putAll(QACache.getAnswer(jedis, companyId, answerIdList, noCacheAnswerIdSet));
		} finally {
			jedis.close();
		}
		Connection conn = null;
		if (!noCacheAnswerIdSet.isEmpty()) {

			Map<Integer, QAProtos.Answer> noCacheAnswerInfoMap = null;
			try {
				conn = hikariDataSource.getConnection();
				noCacheAnswerInfoMap = QADB.getAnswer(conn, companyId, noCacheAnswerIdSet);
			} catch (SQLException e) {
				e.printStackTrace();
				throw new RuntimeException("db fail", e);
			} finally {
				DBUtil.closeQuietly(conn);
			}
			jedis = jedisPool.getResource();
			try {
				QACache.setAnswer(jedis, companyId, noCacheAnswerIdSet, noCacheAnswerInfoMap);
			} finally {
				jedis.close();
			}

			answerInfoMap.putAll(noCacheAnswerInfoMap);
		}
		// get like_num of answer
		Map<Integer, Integer> answLikeNumMap = null;
		try {
			conn = hikariDataSource.getConnection();
			answLikeNumMap = QADB.getLikeNum(conn, companyId, answerIdList);
		} catch (SQLException e) {
			throw new RuntimeException("db failed", e);
		} finally {
			DBUtil.closeQuietly(conn);
		}

		if (userId != null) {

			// get is_like value of answers
			Set<Integer> isLikeAnswerIdSet = null;
			try {
				conn = hikariDataSource.getConnection();
				isLikeAnswerIdSet = QADB.getIsLike(conn, companyId, answerInfoMap.keySet(), userId);
			} catch (SQLException e) {
				throw new RuntimeException("db fail", e);
			} finally {
				DBUtil.closeQuietly(conn);
			}
			Answer.Builder answerBuilder = Answer.newBuilder();
			for (Entry<Integer, Answer> entry : answerInfoMap.entrySet()) {
				answerBuilder.clear();
				answerBuilder.mergeFrom(entry.getValue());
				int likeNum;
				if (answLikeNumMap.isEmpty() || answLikeNumMap.get(entry.getKey()) == null) {
					likeNum = 0;
				} else {
					likeNum = answLikeNumMap.get(entry.getKey());
				}
				answerBuilder.setLikeNum(likeNum);

				answerBuilder.setIsLike(isLikeAnswerIdSet.contains(entry.getKey()));
				if (entry.getValue().hasUserId()) {
					answerBuilder.setCanDelete(entry.getValue().getUserId() == userId);
				} else {
					answerBuilder.setCanDelete(false);
				}
				entry.setValue(answerBuilder.build());
			}
		} else {
			Answer.Builder answerBuilder = Answer.newBuilder();
			for (Entry<Integer, Answer> entry : answerInfoMap.entrySet()) {
				answerBuilder.clear();
				answerBuilder.mergeFrom(entry.getValue());
				int likeNum;
				if (answLikeNumMap.isEmpty() || answLikeNumMap.get(entry.getKey()) == null) {
					likeNum = 0;
				} else {
					likeNum = answLikeNumMap.get(entry.getKey());
				}
				answerBuilder.setLikeNum(likeNum);
				entry.setValue(answerBuilder.build());
			}
		}

		return answerInfoMap;
	}

	/**
	 * 新增问题，并更新缓存
	 * 
	 * @param hikariDataSource
	 * @param jedisPool
	 * @param companyId
	 * @param answerContent
	 * @param userId
	 * @param adminId
	 * @param questionId
	 * @return
	 * int
	 * @throws
	 */
	public static int addAnswer(HikariDataSource hikariDataSource, JedisPool jedisPool, long companyId, String answerContent, @Nullable Long userId,
			@Nullable Long adminId, int questionId) {

		int createTime = (int) (System.currentTimeMillis() / 1000L);
		Connection dbConn = null;
		int answerId;
		try {
			dbConn = hikariDataSource.getConnection();

			answerId = QADB.insertAnswer(dbConn, companyId, questionId, userId, adminId, answerContent, createTime);

		} catch (SQLException e) {
			throw new RuntimeException("db fail", e);
		} finally {
			DBUtil.closeQuietly(dbConn);
		}

		// 添加回答的缓存
		QAProtos.Answer.Builder answerBuilder = QAProtos.Answer.newBuilder();
		answerBuilder.setAnswerId(answerId);
		answerBuilder.setAnswerContent(answerContent);
		answerBuilder.setQuestionId(questionId);
		if (userId != null) {
			answerBuilder.setUserId(userId);
		}
		answerBuilder.setCreateTime(createTime);
		if (adminId != null) {
			answerBuilder.setAdminId(adminId);
		}
		Jedis jedis = jedisPool.getResource();
		try {
			QACache.setAnswer(jedis, companyId, Collections.singleton(answerId), Collections.singletonMap(answerId, answerBuilder.build()));
		} finally {
			jedis.close();
		}
		return answerId;
	}

	/**
	 * 从内存中获取分类，如果内存中没有，则从db中获取
	 * 
	 * @param hikariDataSource
	 * @param jedisPool
	 * @param companyId
	 * @param categoryIds
	 * @return
	 * Map<Integer,QAProtos.Category>
	 * @throws
	 */
	public static Map<Integer, QAProtos.Category> getCategory(HikariDataSource hikariDataSource, JedisPool jedisPool, long companyId, Collection<Integer> categoryIds) {
		if (categoryIds.isEmpty()) {
			return Collections.emptyMap();
		}
		Connection conn = null;
		Set<Integer> noCacheCategoryIds = new TreeSet<Integer>();
		Map<Integer, QAProtos.Category> categoryInfoMap = new HashMap<Integer, QAProtos.Category>(categoryIds.size());
		Jedis jedis = jedisPool.getResource();
		try {
			categoryInfoMap.putAll(QACache.getCategory(jedis, companyId, categoryIds, noCacheCategoryIds));
		} finally {
			jedis.close();
		}
		if (!noCacheCategoryIds.isEmpty()) {
			Map<Integer, Category> noCacheCategoryInfoMap;
			try {
				conn = hikariDataSource.getConnection();
				noCacheCategoryInfoMap = QADB.getCategory(conn, companyId, noCacheCategoryIds);
			} catch (SQLException e) {
				throw new RuntimeException("db fail", e);
			} finally {
				DBUtil.closeQuietly(conn);
			}
			jedis = jedisPool.getResource();
			try {
				QACache.setCategory(jedis, companyId, noCacheCategoryIds, noCacheCategoryInfoMap);
			} finally {
				jedis.close();
			}
			categoryInfoMap.putAll(noCacheCategoryInfoMap);
		}
		// get like_num of answer
		Map<Integer, Integer> cateQuestionNumMap = null;
		try {
			conn = hikariDataSource.getConnection();
			cateQuestionNumMap = QADB.getQuestionNum(conn, companyId, categoryIds);
		} catch (SQLException e) {
			throw new RuntimeException("db failed", e);
		} finally {
			DBUtil.closeQuietly(conn);
		}
		Category.Builder categoryBuilder = Category.newBuilder();
		for (Entry<Integer, Category> entry : categoryInfoMap.entrySet()) {
			categoryBuilder.clear();
			categoryBuilder.mergeFrom(entry.getValue());
			int questionNum;
			if (cateQuestionNumMap.isEmpty() || cateQuestionNumMap.get(entry.getKey()) == null) {
				questionNum = 0;
			} else {
				questionNum = cateQuestionNumMap.get(entry.getKey());
			}
			categoryBuilder.setQuestionNum(questionNum);
			entry.setValue(categoryBuilder.build());
		}

		return categoryInfoMap;
	}

	/**
	 * 创建分类，并更新缓存
	 * 
	 * @param hikariDataSource
	 * @param jedisPool
	 * @param companyId
	 * @param categoryName
	 * @param userId
	 * @param adminId
	 * @return
	 * int
	 * @throws
	 */
	public static int addCategory(HikariDataSource hikariDataSource, JedisPool jedisPool, long companyId, String categoryName, @Nullable Long userId,
			@Nullable Long adminId) {
		Connection dbConn = null;
		int categoryId;
		int createTime = (int) (System.currentTimeMillis() / 1000L);
		try {
			dbConn = hikariDataSource.getConnection();
			categoryId = QADB.insertCategory(dbConn, companyId, categoryName, userId, adminId, createTime);
		} catch (SQLException e) {
			throw new RuntimeException("db fail", e);
		} finally {
			DBUtil.closeQuietly(dbConn);
		}
		// 将新添加的question写入缓存
		QAProtos.Category.Builder categoryBuilder = QAProtos.Category.newBuilder();
		categoryBuilder.setCategoryId(categoryId);
		categoryBuilder.setCategoryName(categoryName);
		if (userId != null) {
			categoryBuilder.setUserId(userId);
		}
		categoryBuilder.setCreateTime(createTime);
		if (adminId != null) {
			categoryBuilder.setAdminId(adminId);
		}

		Jedis jedis = jedisPool.getResource();
		try {
			QACache.setCategory(jedis, companyId, Collections.singleton(categoryId), Collections.singletonMap(categoryId, categoryBuilder.build()));
		} finally {
			jedis.close();
		}
		return categoryId;
	}

	/**
	 * 删除回答，并更新缓存
	 * 
	 * @param hikariDataSource
	 * @param jedisPool
	 * @param companyId
	 * @param aIdList
	 * void
	 * @throws
	 */
	public static void deleteAnswer(HikariDataSource hikariDataSource, JedisPool jedisPool, long companyId, List<Integer> aIdList) {
		Connection dbConn = null;
		try {
			dbConn = hikariDataSource.getConnection();

			QADB.deleteAnswer(dbConn, companyId, aIdList);
		} catch (SQLException e) {
			throw new RuntimeException("db fail", e);
		} finally {
			DBUtil.closeQuietly(dbConn);
		}

		// 删除缓存中的回答数据
		Jedis jedis = jedisPool.getResource();
		try {
			QACache.delAnswer(jedis, companyId, aIdList);
		} finally {
			jedis.close();
		}

	}

	/**
	 * 删除问题，并更新缓存
	 * 
	 * @param hikariDataSource
	 * @param jedisPool
	 * @param companyId
	 * @param qIdList
	 * void
	 * @throws
	 */
	public static void deleteQuestion(HikariDataSource hikariDataSource, JedisPool jedisPool, long companyId, List<Integer> qIdList) {
		Connection dbConn = null;
		List<Integer> answerIds = null;
		try {
			dbConn = hikariDataSource.getConnection();
			answerIds = QADB.getAnswerIdListByQusIds(dbConn, companyId, qIdList);

			QADB.deleteQuestion(dbConn, companyId, qIdList, answerIds);
		} catch (SQLException e) {
			throw new RuntimeException("db fail", e);
		} finally {
			DBUtil.closeQuietly(dbConn);
		}

		Jedis jedis = jedisPool.getResource();
		try {
			QACache.delQuestion(jedis, companyId, qIdList);
			QACache.delAnswer(jedis, companyId, answerIds);
		} finally {
			jedis.close();
		}
	}

	/**
	 * 新增回答，并更新缓存
	 * 
	 * @param hikariDataSource
	 * @param jedisPool
	 * @param companyId
	 * @param queIdAnsContMap
	 * @param userId
	 * @param adminId
	 * void
	 * @throws
	 */
	public static void addAnswer(HikariDataSource hikariDataSource, JedisPool jedisPool, long companyId, Map<Integer, String> queIdAnsContMap, @Nullable Long userId,
			@Nullable Long adminId) {
		int createTime = (int) (System.currentTimeMillis() / 1000L);
		Connection dbConn = null;
		List<Integer> answerIds;
		try {
			dbConn = hikariDataSource.getConnection();

			answerIds = QADB.insertAnswer(dbConn, companyId, queIdAnsContMap, userId, adminId, createTime);

		} catch (SQLException e) {
			throw new RuntimeException("db fail", e);
		} finally {
			DBUtil.closeQuietly(dbConn);
		}

		// 删除缓存中的问题数据,
		Jedis jedis = jedisPool.getResource();
		try {
			QACache.delAnswer(jedis, companyId, answerIds);
		} finally {
			jedis.close();
		}
	}
}
