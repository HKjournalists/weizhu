package com.weizhu.service.survey;

import java.util.Collection;
import java.util.Map;

import redis.clients.jedis.Jedis;

import com.weizhu.common.jedis.JedisValueCacheEx;
import com.weizhu.proto.SurveyProtos;

public class SurveyCache {

	private static final JedisValueCacheEx<Integer, SurveyProtos.Survey> SURVEY_CACHE =
			JedisValueCacheEx.create("survey:", SurveyProtos.Survey.PARSER);
	private static final JedisValueCacheEx<Integer, SurveyProtos.Question> SURVEY_QUESTION_CACHE = 
			JedisValueCacheEx.create("survey:question:", SurveyProtos.Question.PARSER);
	private static final JedisValueCacheEx<Integer, SurveyDAOProtos.SurveyCount> SURVEY_COUNT_CACHE = 
			JedisValueCacheEx.create("survey:count:", SurveyDAOProtos.SurveyCount.PARSER);
	private static final JedisValueCacheEx<Integer, SurveyDAOProtos.QuestionCount> SURVEY_QUESTION_COUNT_CACHE = 
			JedisValueCacheEx.create("survey:question:count", SurveyDAOProtos.QuestionCount.PARSER);
	
	public static Map<Integer, SurveyProtos.Survey> getSurvey(Jedis jedis, long companyId, Collection<Integer> surveyIds) {
		return SURVEY_CACHE.get(jedis, companyId, surveyIds);
	}
	
	public static Map<Integer, SurveyProtos.Survey> getSurvey(Jedis jedis, long companyId, Collection<Integer> surveyIds, Collection<Integer> noCacheSurveyIds) {
		return SURVEY_CACHE.get(jedis, companyId, surveyIds, noCacheSurveyIds);
	}
	
	public static void setSurvey(Jedis jedis, long companyId, Map<Integer, SurveyProtos.Survey> surveyMap) {
		SURVEY_CACHE.set(jedis, companyId, surveyMap);
	}
	
	public static void setSurvey(Jedis jedis, long companyId, Collection<Integer> surveyIds, Map<Integer, SurveyProtos.Survey> surveyMap) {
		SURVEY_CACHE.set(jedis, companyId, surveyIds, surveyMap);
	}
	
	public static void delSurvey(Jedis jedis, long companyId, Collection<Integer> surveyIds) {
		SURVEY_CACHE.del(jedis, companyId, surveyIds);
	}
	
	public static Map<Integer, SurveyProtos.Question> getQuestion(Jedis jedis, long companyId, Collection<Integer> questionIds) {
		return SURVEY_QUESTION_CACHE.get(jedis, companyId, questionIds);
	}
	
	public static Map<Integer, SurveyProtos.Question> getQuestion(Jedis jedis, long companyId, Collection<Integer> questionIds, Collection<Integer> noCacheQuestionIds) {
		return SURVEY_QUESTION_CACHE.get(jedis, companyId, questionIds, noCacheQuestionIds);
	}
	
	public static void setQuestion(Jedis jedis, long companyId, Map<Integer, SurveyProtos.Question> questionMap) {
		SURVEY_QUESTION_CACHE.set(jedis, companyId, questionMap);
	}
	
	public static void setQuestion(Jedis jedis, long companyId, Collection<Integer> questionIds, Map<Integer, SurveyProtos.Question> questionMap) {
		SURVEY_QUESTION_CACHE.set(jedis, companyId, questionIds, questionMap);
	}
	
	public static void delQuestion(Jedis jedis, long companyId, Collection<Integer> questionIds) {
		SURVEY_QUESTION_CACHE.del(jedis, companyId, questionIds);
	}
	
	public static Map<Integer, SurveyDAOProtos.SurveyCount> getSurveyCount (Jedis jedis, long companyId, Collection<Integer> surveyIds) {
		return SURVEY_COUNT_CACHE.get(jedis, companyId, surveyIds);
	}
	
	public static Map<Integer, SurveyDAOProtos.SurveyCount> getSurveyCount (Jedis jedis, long companyId, Collection<Integer> surveyIds, Collection<Integer> noCacheSurveyIds) {
		return SURVEY_COUNT_CACHE.get(jedis, companyId, surveyIds, noCacheSurveyIds);
	}
	
	public static void setSurveyCount(Jedis jedis, long companyId, Map<Integer, SurveyDAOProtos.SurveyCount> surveyCountMap) {
		SURVEY_COUNT_CACHE.set(jedis, companyId, surveyCountMap);
	}
	
	public static void setSurveyCount(Jedis jedis, long companyId, Collection<Integer> surveyIds, Map<Integer, SurveyDAOProtos.SurveyCount> surveyCountMap) {
		SURVEY_COUNT_CACHE.set(jedis, companyId, surveyIds, surveyCountMap);
	}
	
	public static void delSurveyCount(Jedis jedis, long companyId, Collection<Integer> surveyIds) {
		SURVEY_COUNT_CACHE.del(jedis, companyId, surveyIds);
	}
	
	public static Map<Integer, SurveyDAOProtos.QuestionCount> getQuestionCount(Jedis jedis, long companyId, Collection<Integer> questionIds) {
		return SURVEY_QUESTION_COUNT_CACHE.get(jedis, companyId, questionIds);
	}
	
	public static Map<Integer, SurveyDAOProtos.QuestionCount> getQuestionCount(Jedis jedis, long companyId, Collection<Integer> questionIds, Collection<Integer> noCacheQuestionIds) {
		return SURVEY_QUESTION_COUNT_CACHE.get(jedis, companyId, questionIds, noCacheQuestionIds);
	}
	
	public static void setQuestionCount(Jedis jedis, long companyId, Map<Integer, SurveyDAOProtos.QuestionCount> questionCountMap) {
		SURVEY_QUESTION_COUNT_CACHE.set(jedis, companyId, questionCountMap);
	}
	
	public static void setQuestionCount(Jedis jedis, long companyId, Collection<Integer> questionIds, Map<Integer, SurveyDAOProtos.QuestionCount> questionCountMap) {
		SURVEY_QUESTION_COUNT_CACHE.set(jedis, companyId, questionIds, questionCountMap);
	}
	
	public static void delQuestionCount(Jedis jedis, long companyId, Collection<Integer> questionIds) {
		SURVEY_QUESTION_COUNT_CACHE.del(jedis, companyId, questionIds);
	}
}
