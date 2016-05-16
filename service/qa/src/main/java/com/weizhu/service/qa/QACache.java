package com.weizhu.service.qa;

import java.util.Collection;
import java.util.Map;

import redis.clients.jedis.Jedis;

import com.weizhu.common.jedis.JedisValueCacheEx;
import com.weizhu.proto.QAProtos;

/**
 * 问答缓存，对应于手机端
 * 
 * @author zhangjun
 *
 */
public class QACache {

	private static final JedisValueCacheEx<Integer, QAProtos.Question> QUESTION_CACHE = JedisValueCacheEx.create("qa:question:", QAProtos.Question.PARSER);

	private static final JedisValueCacheEx<Integer, QAProtos.Answer> ANSWER_CACHE = JedisValueCacheEx.create("qa:answer:", QAProtos.Answer.PARSER);
	private static final JedisValueCacheEx<Integer, QAProtos.Category> CATEGORY_CACHE = JedisValueCacheEx.create("qa:category:", QAProtos.Category.PARSER);

	/**
	 * 根据questionId获取缓存中的问题信息
	 * 
	 * @param jedis
	 * @param questionIds
	 * @param noCacheQuestionIds 未命中缓存的id
	 */
	public static Map<Integer, QAProtos.Question> getQuestion(Jedis jedis, long companyId, Collection<Integer> questionIds, Collection<Integer> noCacheQuestionIds) {
		return QUESTION_CACHE.get(jedis, companyId, questionIds, noCacheQuestionIds);
	}

	/**
	 * 根据questionId缓存问题信息
	 * 
	 * @param jedis
	 * @param questionIds 放入缓存的id
	 * @param questionInfoMap 结果数据
	 */
	public static void setQuestion(Jedis jedis, long companyId, Collection<Integer> questionIds, Map<Integer, QAProtos.Question> questionInfoMap) {
		QUESTION_CACHE.set(jedis, companyId, questionIds, questionInfoMap);
	}

	/**
	 * 根据questionId删除缓存中的问题信息
	 * 
	 * @param jedis
	 * @param questionIds
	 */
	public static void delQuestion(Jedis jedis, long companyId, Collection<Integer> questionIds) {
		QUESTION_CACHE.del(jedis, companyId, questionIds);
	}

	/**
	 * 根据answerId获取缓存中的回答信息
	 * 
	 * @param jedis
	 * @param answerIds
	 * @param noCacheAnswerIds 未命中缓存的id
	 */
	public static Map<Integer, QAProtos.Answer> getAnswer(Jedis jedis, long companyId, Collection<Integer> answerIds, Collection<Integer> noCacheAnswerIds) {

		return ANSWER_CACHE.get(jedis, companyId, answerIds, noCacheAnswerIds);
	}

	/**
	 * 根据answerId缓存回答信息
	 * 
	 * @param jedis
	 * @param answerIds 放入缓存的id
	 * @param answerInfoMap 结果数据
	 */
	public static void setAnswer(Jedis jedis, long companyId, Collection<Integer> answerIds, Map<Integer, QAProtos.Answer> answerInfoMap) {
		ANSWER_CACHE.set(jedis, companyId, answerIds, answerInfoMap);
	}

	/**
	 * 根据answerId删除缓存中的回答信息
	 * 
	 * @param jedis
	 * @param answerIds 需要从缓存删除的id
	 */
	public static void delAnswer(Jedis jedis, long companyId, Collection<Integer> answerIds) {
		ANSWER_CACHE.del(jedis, companyId, answerIds);
	}

	/**
	 * 根据categoryId获取缓存中的问题信息
	 * 
	 * @param jedis
	 * @param categoryIds
	 * @param noCacheCategoryIds 未命中缓存的id
	 */
	public static Map<Integer, QAProtos.Category> getCategory(Jedis jedis, long companyId, Collection<Integer> categoryIds, Collection<Integer> noCacheCategoryIds) {
		return CATEGORY_CACHE.get(jedis, companyId, categoryIds, noCacheCategoryIds);
	}

	/**
	 * 根据categoryId缓存问题信息
	 * 
	 * @param jedis
	 * @param categoryIds 放入缓存的id
	 * @param categoryInfoMap 结果数据
	 */
	public static void setCategory(Jedis jedis, long companyId, Collection<Integer> categoryIds, Map<Integer, QAProtos.Category> categoryInfoMap) {
		CATEGORY_CACHE.set(jedis, companyId, categoryIds, categoryInfoMap);
	}

	/**
	 * 根据categoryId删除缓存中的问题信息
	 * 
	 * @param jedis
	 * @param categoryIds
	 */
	public static void delCategory(Jedis jedis, long companyId, Collection<Integer> categoryIds) {
		CATEGORY_CACHE.del(jedis, companyId, categoryIds);
	}

}
