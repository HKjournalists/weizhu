package com.weizhu.service.discover_v2;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

import redis.clients.jedis.Jedis;

import com.weizhu.common.jedis.JedisValueCache;
import com.weizhu.common.jedis.JedisValueCacheEx;
import com.weizhu.proto.DiscoverV2Protos;

public class DiscoverV2Cache {

	private static final JedisValueCache<Long, DiscoverV2DAOProtos.DiscoverHome> DISCOVER_HOME_CACHE = JedisValueCache.create("discover_v2:home:",
			DiscoverV2DAOProtos.DiscoverHome.PARSER);

	public static Map<Long, DiscoverV2DAOProtos.DiscoverHome> getDiscoverHome(Jedis jedis, Collection<Long> companyIds) {
		return DISCOVER_HOME_CACHE.get(jedis, companyIds);
	}

	public static Map<Long, DiscoverV2DAOProtos.DiscoverHome> getDiscoverHome(Jedis jedis, Collection<Long> companyIds,
			Collection<Long> noCacheCompanyIds) {
		return DISCOVER_HOME_CACHE.get(jedis, companyIds, noCacheCompanyIds);
	}

	public static void setDiscoverHome(Jedis jedis, Map<Long, DiscoverV2DAOProtos.DiscoverHome> discoverHomeMap) {
		DISCOVER_HOME_CACHE.set(jedis, discoverHomeMap);
	}

	public static void delDiscoverHome(Jedis jedis, Collection<Long> companyIds) {
		DISCOVER_HOME_CACHE.del(jedis, companyIds);
	}

	private static final JedisValueCacheEx<Integer, DiscoverV2DAOProtos.ModuleCategoryItemList> DISCOVER_MODULE_CATEGORY_ITEM_LIST_CACHE = JedisValueCacheEx
			.create("discover_v2:module_category_item_list:", DiscoverV2DAOProtos.ModuleCategoryItemList.PARSER);

	public static Map<Integer, DiscoverV2DAOProtos.ModuleCategoryItemList> getModuleCategoryItemList(Jedis jedis, long companyId, Collection<Integer> categoryIds) {
		return DISCOVER_MODULE_CATEGORY_ITEM_LIST_CACHE.get(jedis, companyId, categoryIds);
	}

	public static Map<Integer, DiscoverV2DAOProtos.ModuleCategoryItemList> getModuleCategoryItemList(Jedis jedis, long companyId, Collection<Integer> categoryIds,
			Collection<Integer> noCacheCategoryIds) {
		return DISCOVER_MODULE_CATEGORY_ITEM_LIST_CACHE.get(jedis, companyId, categoryIds, noCacheCategoryIds);
	}

	public static void setModuleCategoryItemList(Jedis jedis, long companyId, Map<Integer, DiscoverV2DAOProtos.ModuleCategoryItemList> moduleCategoryItemListMap) {
		DISCOVER_MODULE_CATEGORY_ITEM_LIST_CACHE.set(jedis, companyId, moduleCategoryItemListMap);
	}

	public static void setModuleCategoryItemList(Jedis jedis, long companyId, Collection<Integer> categoryIds,
			Map<Integer, DiscoverV2DAOProtos.ModuleCategoryItemList> moduleCategoryItemListMap) {
		DISCOVER_MODULE_CATEGORY_ITEM_LIST_CACHE.set(jedis, companyId, categoryIds, moduleCategoryItemListMap);
	}

	public static void delModuleCategoryItemList(Jedis jedis, long companyId, Collection<Integer> categoryIds) {
		DISCOVER_MODULE_CATEGORY_ITEM_LIST_CACHE.del(jedis, companyId, categoryIds);
	}

	private static final JedisValueCacheEx<Long, DiscoverV2Protos.Item.Base> DISCOVER_ITEM_BASE_CACHE = JedisValueCacheEx.create("discover_v2:item_base:",
			DiscoverV2Protos.Item.Base.PARSER);

	public static Map<Long, DiscoverV2Protos.Item.Base> getItemBase(Jedis jedis, long companyId, Collection<Long> itemIds) {
		return DISCOVER_ITEM_BASE_CACHE.get(jedis, companyId, itemIds);
	}

	public static Map<Long, DiscoverV2Protos.Item.Base> getItemBase(Jedis jedis, long companyId, Collection<Long> itemIds, Collection<Long> noCacheItemIds) {
		return DISCOVER_ITEM_BASE_CACHE.get(jedis, companyId, itemIds, noCacheItemIds);
	}

	public static void setItemBase(Jedis jedis, long companyId, Map<Long, DiscoverV2Protos.Item.Base> itemBaseMap) {
		DISCOVER_ITEM_BASE_CACHE.set(jedis, companyId, itemBaseMap);
	}

	public static void setItemBase(Jedis jedis, long companyId, Collection<Long> itemIds, Map<Long, DiscoverV2Protos.Item.Base> itemBaseMap) {
		DISCOVER_ITEM_BASE_CACHE.set(jedis, companyId, itemIds, itemBaseMap);
	}

	public static void delItemBase(Jedis jedis, long companyId, Collection<Long> itemIds) {
		DISCOVER_ITEM_BASE_CACHE.del(jedis, companyId, itemIds);
	}

	private static final JedisValueCacheEx<Long, DiscoverV2Protos.Item.Count> DISCOVER_ITEM_COUNT_CACHE = JedisValueCacheEx
			.create("discover_v2:item_count:", DiscoverV2Protos.Item.Count.PARSER);

	public static Map<Long, DiscoverV2Protos.Item.Count> getItemCount(Jedis jedis, long companyId, Collection<Long> itemIds) {
		return DISCOVER_ITEM_COUNT_CACHE.get(jedis, companyId, itemIds);
	}

	public static Map<Long, DiscoverV2Protos.Item.Count> getItemCount(Jedis jedis, long companyId, Collection<Long> itemIds, Collection<Long> noCacheItemIds) {
		return DISCOVER_ITEM_COUNT_CACHE.get(jedis, companyId, itemIds, noCacheItemIds);
	}

	public static void setItemCount(Jedis jedis, long companyId, Map<Long, DiscoverV2Protos.Item.Count> itemCountMap) {
		DISCOVER_ITEM_COUNT_CACHE.set(jedis, companyId, itemCountMap);
	}

	public static void setItemCount(Jedis jedis, long companyId, Collection<Long> itemIds, Map<Long, DiscoverV2Protos.Item.Count> itemCountMap) {
		DISCOVER_ITEM_COUNT_CACHE.set(jedis, companyId, itemIds, itemCountMap);
	}

	public static void delItemCount(Jedis jedis, long companyId, Collection<Long> itemIds) {
		DISCOVER_ITEM_COUNT_CACHE.del(jedis, companyId, itemIds);
	}

	private static final JedisValueCacheEx<Long, DiscoverV2DAOProtos.ItemLearnList> DISCOVER_ITEM_LEARN_LIST_CACHE = JedisValueCacheEx
			.create("discover_v2:item_learn_list:", DiscoverV2DAOProtos.ItemLearnList.PARSER);

	public static Map<Long, DiscoverV2DAOProtos.ItemLearnList> getItemLearnList(Jedis jedis, long companyId, Collection<Long> itemIds) {
		return DISCOVER_ITEM_LEARN_LIST_CACHE.get(jedis, companyId, itemIds);
	}

	public static Map<Long, DiscoverV2DAOProtos.ItemLearnList> getItemLearnList(Jedis jedis, long companyId, Collection<Long> itemIds,
			Collection<Long> noCacheItemIds) {
		return DISCOVER_ITEM_LEARN_LIST_CACHE.get(jedis, companyId, itemIds, noCacheItemIds);
	}

	public static void setItemLearnList(Jedis jedis, long companyId, Map<Long, DiscoverV2DAOProtos.ItemLearnList> itemLearnListMap) {
		DISCOVER_ITEM_LEARN_LIST_CACHE.set(jedis, companyId, itemLearnListMap);
	}

	public static void setItemLearnList(Jedis jedis, long companyId, Collection<Long> itemIds, Map<Long, DiscoverV2DAOProtos.ItemLearnList> itemLearnListMap) {
		DISCOVER_ITEM_LEARN_LIST_CACHE.set(jedis, companyId, itemIds, itemLearnListMap);
	}

	public static void delItemLearnList(Jedis jedis, long companyId, Collection<Long> itemIds) {
		DISCOVER_ITEM_LEARN_LIST_CACHE.del(jedis, companyId, itemIds);
	}

	private static final JedisValueCacheEx<Long, DiscoverV2DAOProtos.ItemCommentList> DISCOVER_ITEM_COMMENT_LIST_CACHE = JedisValueCacheEx
			.create("discover_v2:item_comment_list:", DiscoverV2DAOProtos.ItemCommentList.PARSER);

	public static Map<Long, DiscoverV2DAOProtos.ItemCommentList> getItemCommentList(Jedis jedis, long companyId, Collection<Long> itemIds) {
		return DISCOVER_ITEM_COMMENT_LIST_CACHE.get(jedis, companyId, itemIds);
	}

	public static Map<Long, DiscoverV2DAOProtos.ItemCommentList> getItemCommentList(Jedis jedis, long companyId, Collection<Long> itemIds,
			Collection<Long> noCacheItemIds) {
		return DISCOVER_ITEM_COMMENT_LIST_CACHE.get(jedis, companyId, itemIds, noCacheItemIds);
	}

	public static void setItemCommentList(Jedis jedis, long companyId, Map<Long, DiscoverV2DAOProtos.ItemCommentList> itemCommentListMap) {
		DISCOVER_ITEM_COMMENT_LIST_CACHE.set(jedis, companyId, itemCommentListMap);
	}

	public static void setItemCommentList(Jedis jedis, long companyId, Collection<Long> itemIds, Map<Long, DiscoverV2DAOProtos.ItemCommentList> itemCommentListMap) {
		DISCOVER_ITEM_COMMENT_LIST_CACHE.set(jedis, companyId, itemIds, itemCommentListMap);
	}

	public static void delItemCommentList(Jedis jedis, long companyId, Collection<Long> itemIds) {
		DISCOVER_ITEM_COMMENT_LIST_CACHE.del(jedis, companyId, itemIds);
	}

	private static final JedisValueCacheEx<Long, DiscoverV2DAOProtos.ItemScoreList> DISCOVER_ITEM_SCORE_LIST_CACHE = JedisValueCacheEx
			.create("discover_v2:item_score_list:", DiscoverV2DAOProtos.ItemScoreList.PARSER);

	public static Map<Long, DiscoverV2DAOProtos.ItemScoreList> getItemScoreList(Jedis jedis, long companyId, Collection<Long> itemIds) {
		return DISCOVER_ITEM_SCORE_LIST_CACHE.get(jedis, companyId, itemIds);
	}

	public static Map<Long, DiscoverV2DAOProtos.ItemScoreList> getItemScoreList(Jedis jedis, long companyId, Collection<Long> itemIds,
			Collection<Long> noCacheItemIds) {
		return DISCOVER_ITEM_SCORE_LIST_CACHE.get(jedis, companyId, itemIds, noCacheItemIds);
	}

	public static void setItemScoreList(Jedis jedis, long companyId, Map<Long, DiscoverV2DAOProtos.ItemScoreList> itemScoreListMap) {
		DISCOVER_ITEM_SCORE_LIST_CACHE.set(jedis, companyId, itemScoreListMap);
	}

	public static void setItemScoreList(Jedis jedis, long companyId, Collection<Long> itemIds, Map<Long, DiscoverV2DAOProtos.ItemScoreList> itemScoreListMap) {
		DISCOVER_ITEM_SCORE_LIST_CACHE.set(jedis, companyId, itemIds, itemScoreListMap);
	}

	public static void delItemScoreList(Jedis jedis, long companyId, Collection<Long> itemIds) {
		DISCOVER_ITEM_SCORE_LIST_CACHE.del(jedis, companyId, itemIds);
	}

	private static final JedisValueCacheEx<Long, DiscoverV2DAOProtos.ItemLikeList> DISCOVER_ITEM_LIKE_LIST_CACHE = JedisValueCacheEx
			.create("discover_v2:item_like_list:", DiscoverV2DAOProtos.ItemLikeList.PARSER);

	public static Map<Long, DiscoverV2DAOProtos.ItemLikeList> getItemLikeList(Jedis jedis, long companyId, Collection<Long> itemIds) {
		return DISCOVER_ITEM_LIKE_LIST_CACHE.get(jedis, companyId, itemIds);
	}

	public static Map<Long, DiscoverV2DAOProtos.ItemLikeList> getItemLikeList(Jedis jedis, long companyId, Collection<Long> itemIds,
			Collection<Long> noCacheItemIds) {
		return DISCOVER_ITEM_LIKE_LIST_CACHE.get(jedis, companyId, itemIds, noCacheItemIds);
	}

	public static void setItemLikeList(Jedis jedis, long companyId, Map<Long, DiscoverV2DAOProtos.ItemLikeList> itemLikeListMap) {
		DISCOVER_ITEM_LIKE_LIST_CACHE.set(jedis, companyId, itemLikeListMap);
	}

	public static void setItemLikeList(Jedis jedis, long companyId, Collection<Long> itemIds, Map<Long, DiscoverV2DAOProtos.ItemLikeList> itemLikeListMap) {
		DISCOVER_ITEM_LIKE_LIST_CACHE.set(jedis, companyId, itemIds, itemLikeListMap);
	}

	public static void delItemLikeList(Jedis jedis, long companyId, Collection<Long> itemIds) {
		DISCOVER_ITEM_LIKE_LIST_CACHE.del(jedis, companyId, itemIds);
	}

	private static final JedisValueCacheEx<Long, DiscoverV2DAOProtos.ItemShareList> DISCOVER_ITEM_SHARE_LIST_CACHE = JedisValueCacheEx
			.create("discover_v2:item_share_list:", DiscoverV2DAOProtos.ItemShareList.PARSER);

	public static Map<Long, DiscoverV2DAOProtos.ItemShareList> getItemShareList(Jedis jedis, long companyId, Collection<Long> itemIds) {
		return DISCOVER_ITEM_SHARE_LIST_CACHE.get(jedis, companyId, itemIds);
	}

	public static Map<Long, DiscoverV2DAOProtos.ItemShareList> getItemShareList(Jedis jedis, long companyId, Collection<Long> itemIds,
			Collection<Long> noCacheItemIds) {
		return DISCOVER_ITEM_SHARE_LIST_CACHE.get(jedis, companyId, itemIds, noCacheItemIds);
	}

	public static void setItemShareList(Jedis jedis, long companyId, Map<Long, DiscoverV2DAOProtos.ItemShareList> itemShareListMap) {
		DISCOVER_ITEM_SHARE_LIST_CACHE.set(jedis, companyId, itemShareListMap);
	}

	public static void setItemShareList(Jedis jedis, long companyId, Collection<Long> itemIds, Map<Long, DiscoverV2DAOProtos.ItemShareList> itemShareListMap) {
		DISCOVER_ITEM_SHARE_LIST_CACHE.set(jedis, companyId, itemIds, itemShareListMap);
	}

	public static void delItemShareListMap(Jedis jedis, long companyId, Collection<Long> itemIds) {
		DISCOVER_ITEM_SHARE_LIST_CACHE.del(jedis, companyId, itemIds);
	}

	public static void clearCache(Jedis jedis) {
		Set<byte[]> keys = jedis.keys("discover_v2:*".getBytes());
		if (keys.isEmpty()) {
			return;
		}
		jedis.del(keys.toArray(new byte[keys.size()][]));
	}
}
