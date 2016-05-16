package com.weizhu.service.scene.tools.recommender;

import java.util.Collection;
import java.util.Map;

import redis.clients.jedis.Jedis;

import com.weizhu.common.jedis.JedisValueCache;
import com.weizhu.common.jedis.JedisValueCacheEx;
import com.weizhu.proto.SceneProtos;
import com.weizhu.service.scene.SceneDAOProtos;

public class RecommenderCache {


	private static final JedisValueCacheEx<Integer, SceneDAOProtos.RecommenderCategoryExt> CATEGORY_EXT_CACHE = JedisValueCacheEx.create("recommender:category_ext:",
			SceneDAOProtos.RecommenderCategoryExt.PARSER);

	private static final JedisValueCacheEx<Integer, SceneProtos.RecommenderRecommendProduct> RECOMMEND_PRODUCT_CACHE = JedisValueCacheEx.create("recommender:recommend_category:",
			SceneProtos.RecommenderRecommendProduct.PARSER);
	
	private static final JedisValueCache<Long, SceneDAOProtos.RecommenderHome> RECOMMENDER_HOME_CACHE = 
			JedisValueCache.create("scene:home:", SceneDAOProtos.RecommenderHome.PARSER);

	public static Map<Long, SceneDAOProtos.RecommenderHome> getRecommenderHome(Jedis jedis, Collection<Long> companyIds) {
		return RECOMMENDER_HOME_CACHE.get(jedis, companyIds);
	}

	public static Map<Long, SceneDAOProtos.RecommenderHome> getRecommenderHome(Jedis jedis, Collection<Long> companyIds, Collection<Long> noCacheCompanyIds) {
		return RECOMMENDER_HOME_CACHE.get(jedis, companyIds, noCacheCompanyIds);
	}
	
	public static void setRecommenderHome(Jedis jedis, Map<Long, SceneDAOProtos.RecommenderHome> recommenderHomeMap) {
		RECOMMENDER_HOME_CACHE.set(jedis, recommenderHomeMap);
	}
	
	public static void delRecommenderHome(Jedis jedis, Collection<Long> companyIds) {
		RECOMMENDER_HOME_CACHE.del(jedis, companyIds);
	}
	

	
	public static Map<Integer, SceneDAOProtos.RecommenderCategoryExt> getCategoryExt(Jedis jedis, long companyId, Collection<Integer> categoryIds) {
		return CATEGORY_EXT_CACHE.get(jedis, companyId, categoryIds);
	}

	public static Map<Integer, SceneDAOProtos.RecommenderCategoryExt> getCategoryExt(Jedis jedis, long companyId, Collection<Integer> categoryIds,
			Collection<Integer> noCacheCategoryIds) {
		return CATEGORY_EXT_CACHE.get(jedis, companyId, categoryIds, noCacheCategoryIds);
	}

	public static void setCategoryExt(Jedis jedis, long companyId, Map<Integer, SceneDAOProtos.RecommenderCategoryExt> categoryExtMap) {
		CATEGORY_EXT_CACHE.set(jedis, companyId, categoryExtMap);
	}

	public static void setCategoryExt(Jedis jedis, long companyId, Collection<Integer> categoryIds, Map<Integer, SceneDAOProtos.RecommenderCategoryExt> categoryExtMap) {
		CATEGORY_EXT_CACHE.set(jedis, companyId, categoryIds, categoryExtMap);
	}

	public static void delCategoryExt(Jedis jedis, long companyId, Collection<Integer> categoryIds) {
		CATEGORY_EXT_CACHE.del(jedis, companyId, categoryIds);
	}

	public static Map<Integer, SceneProtos.RecommenderRecommendProduct> getRecommendProduct(Jedis jedis, long companyId, Collection<Integer> recommendProductIds) {
		return RECOMMEND_PRODUCT_CACHE.get(jedis, companyId, recommendProductIds);
	}

	public static Map<Integer, SceneProtos.RecommenderRecommendProduct> getRecommendProduct(Jedis jedis, long companyId, Collection<Integer> recommendProductIds,
			Collection<Integer> noCacheRecommendProductIds) {
		return RECOMMEND_PRODUCT_CACHE.get(jedis, companyId, recommendProductIds, noCacheRecommendProductIds);
	}

	public static void setRecommendProduct(Jedis jedis, long companyId, Map<Integer, SceneProtos.RecommenderRecommendProduct> recommendProductMap) {
		RECOMMEND_PRODUCT_CACHE.set(jedis, companyId, recommendProductMap);
	}

	public static void setRecommendProduct(Jedis jedis, long companyId, Collection<Integer> recommendProductIds,
			Map<Integer, SceneProtos.RecommenderRecommendProduct> recommendProductMap) {
		RECOMMEND_PRODUCT_CACHE.set(jedis, companyId, recommendProductIds, recommendProductMap);
	}

	public static void delRecommendProduct(Jedis jedis, long companyId, Collection<Integer> recommendProductIds) {
		RECOMMEND_PRODUCT_CACHE.del(jedis, companyId, recommendProductIds);
	}
}
