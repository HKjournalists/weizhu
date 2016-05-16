package com.weizhu.service.exam;

import java.util.Collection;
import java.util.Map;

import redis.clients.jedis.Jedis;

import com.weizhu.common.jedis.JedisValueCacheEx;
import com.weizhu.proto.ExamProtos;

public class ExamCache {
	
	private static final JedisValueCacheEx<Integer, ExamDAOProtos.ExamInfo> EXAM_INFO_CACHE = 
			JedisValueCacheEx.create("exam:exam_info:", ExamDAOProtos.ExamInfo.PARSER);
	
	public static Map<Integer, ExamDAOProtos.ExamInfo> getExamInfo(Jedis jedis, long companyId, Collection<Integer> examIds) {
		return EXAM_INFO_CACHE.get(jedis, companyId, examIds);
	}
	
	public static Map<Integer, ExamDAOProtos.ExamInfo> getExamInfo(Jedis jedis, long companyId, Collection<Integer> examIds, Collection<Integer> noCacheExamIds) {
		return EXAM_INFO_CACHE.get(jedis, companyId, examIds, noCacheExamIds);
	}
	
	public static void setExamInfo(Jedis jedis, long companyId, Map<Integer, ExamDAOProtos.ExamInfo> examMap) {
		EXAM_INFO_CACHE.set(jedis, companyId, examMap);
	}
	
	public static void setExamInfo(Jedis jedis, long companyId, Collection<Integer> examIds, Map<Integer, ExamDAOProtos.ExamInfo> examMap) {
		EXAM_INFO_CACHE.set(jedis, companyId, examIds, examMap);
	}
	
	public static void delExamInfo(Jedis jedis, long companyId, Collection<Integer> examIds) {
		EXAM_INFO_CACHE.del(jedis, companyId, examIds);
	}
	
	private static final JedisValueCacheEx<Integer, ExamProtos.Question> QUESTION_CACHE = 
			JedisValueCacheEx.create("exam:question:", ExamProtos.Question.PARSER);
	
	public static Map<Integer, ExamProtos.Question> getQuestion(Jedis jedis, long companyId, Collection<Integer> questionIds) {
		return QUESTION_CACHE.get(jedis, companyId, questionIds);
	}
	
	public static Map<Integer, ExamProtos.Question> getQuestion(Jedis jedis, long companyId, Collection<Integer> questionIds, Collection<Integer> noCacheQuestionIds) {
		return QUESTION_CACHE.get(jedis, companyId, questionIds, noCacheQuestionIds);
	}
	
	public static void setQuestion(Jedis jedis, long companyId, Map<Integer, ExamProtos.Question> questionMap) {
		QUESTION_CACHE.set(jedis, companyId, questionMap);
	}
	
	public static void setQuestion(Jedis jedis, long companyId, Collection<Integer> questionIds, Map<Integer, ExamProtos.Question> questionMap) {
		QUESTION_CACHE.set(jedis, companyId, questionIds, questionMap);
	}
	
	public static void delQuestion(Jedis jedis, long companyId, Collection<Integer> questionIds) {
		QUESTION_CACHE.del(jedis, companyId, questionIds);
	}
	
	private static final JedisValueCacheEx<Long, ExamDAOProtos.ExamUserAnswer> EXAM_USER_ANSWER_CACHE = 
			JedisValueCacheEx.create("exam:user_answer:", ExamDAOProtos.ExamUserAnswer.PARSER);
	
	public static Map<Long, ExamDAOProtos.ExamUserAnswer> getExamUserAnswer(Jedis jedis, long companyId, Collection<Long> userIds) {
		return EXAM_USER_ANSWER_CACHE.get(jedis, companyId, userIds);
	}
	
	public static Map<Long, ExamDAOProtos.ExamUserAnswer> getExamUserAnswer(Jedis jedis, long companyId, Collection<Long> userIds, Collection<Long> noCacheUserIds) {
		return EXAM_USER_ANSWER_CACHE.get(jedis, companyId, userIds, noCacheUserIds);
	}
	
	public static void setExamUserAnswer(Jedis jedis, long companyId, Map<Long, ExamDAOProtos.ExamUserAnswer> examUserAnswerMap) {
		EXAM_USER_ANSWER_CACHE.set(jedis, companyId, examUserAnswerMap);
	}
	
	public static void setExamUserAnswer(Jedis jedis, long companyId, Collection<Long> userIds, Map<Long, ExamDAOProtos.ExamUserAnswer> examUserAnswerMap) {
		EXAM_USER_ANSWER_CACHE.set(jedis, companyId, userIds, examUserAnswerMap);
	}
	
	public static void delExamUserAnswer(Jedis jedis, long companyId, Collection<Long> userIds) {
		EXAM_USER_ANSWER_CACHE.del(jedis, companyId, userIds);
	}
}
