package com.weizhu.service.offline_training;

import java.util.Collection;
import java.util.Map;

import com.weizhu.common.jedis.JedisValueCache;
import com.weizhu.common.jedis.JedisValueCacheEx;
import com.weizhu.proto.OfflineTrainingProtos;

import redis.clients.jedis.Jedis;

public class OfflineTrainingCache {

	private static final JedisValueCacheEx<Integer, OfflineTrainingProtos.Train> TRAIN_CACHE = 
			JedisValueCacheEx.create("offline_training:train:", OfflineTrainingProtos.Train.PARSER);
	
	public static Map<Integer, OfflineTrainingProtos.Train> getTrain(Jedis jedis, long companyId, Collection<Integer> trainIds) {
		return TRAIN_CACHE.get(jedis, companyId, trainIds);
	}
	
	public static Map<Integer, OfflineTrainingProtos.Train> getTrain(Jedis jedis, long companyId, Collection<Integer> trainIds, Collection<Integer> noCacheTrainIds) {
		return TRAIN_CACHE.get(jedis, companyId, trainIds, noCacheTrainIds);
	}
	
	public static void setTrain(Jedis jedis, long companyId, Map<Integer, OfflineTrainingProtos.Train> trainMap) {
		TRAIN_CACHE.set(jedis, companyId, trainMap);
	}
	
	public static void setTrain(Jedis jedis, long companyId, Collection<Integer> trainIds, Map<Integer, OfflineTrainingProtos.Train> trainMap) {
		TRAIN_CACHE.set(jedis, companyId, trainIds, trainMap);
	}
	
	public static void delTrain(Jedis jedis, long companyId, Collection<Integer> trainIds) {
		TRAIN_CACHE.del(jedis, companyId, trainIds);
	}
	
	private static final JedisValueCacheEx<Integer, OfflineTrainingProtos.TrainCount> TRAIN_COUNT_CACHE = 
			JedisValueCacheEx.create("offline_training:train_count:", OfflineTrainingProtos.TrainCount.PARSER);
	
	public static Map<Integer, OfflineTrainingProtos.TrainCount> getTrainCount(Jedis jedis, long companyId, Collection<Integer> trainIds) {
		return TRAIN_COUNT_CACHE.get(jedis, companyId, trainIds);
	}
	
	public static Map<Integer, OfflineTrainingProtos.TrainCount> getTrainCount(Jedis jedis, long companyId, Collection<Integer> trainIds, Collection<Integer> noCacheTrainIds) {
		return TRAIN_COUNT_CACHE.get(jedis, companyId, trainIds, noCacheTrainIds);
	}
	
	public static void setTrainCount(Jedis jedis, long companyId, Map<Integer, OfflineTrainingProtos.TrainCount> trainCountMap) {
		TRAIN_COUNT_CACHE.set(jedis, companyId, trainCountMap);
	}
	
	public static void setTrainCount(Jedis jedis, long companyId, Collection<Integer> trainIds, Map<Integer, OfflineTrainingProtos.TrainCount> trainCountMap) {
		TRAIN_COUNT_CACHE.set(jedis, companyId, trainIds, trainCountMap);
	}
	
	public static void delTrainCount(Jedis jedis, long companyId, Collection<Integer> trainIds) {
		TRAIN_COUNT_CACHE.del(jedis, companyId, trainIds);
	}
	
	private static final JedisValueCache<Long, OfflineTrainingDAOProtos.TrainIndexList> OPEN_TRAIN_INDEX_LIST_CACHE = 
			JedisValueCache.create("offline_training:open_train_index_list:", OfflineTrainingDAOProtos.TrainIndexList.PARSER);

	public static Map<Long, OfflineTrainingDAOProtos.TrainIndexList> getOpenTrainIndexList(Jedis jedis, Collection<Long> companyIds) {
		return OPEN_TRAIN_INDEX_LIST_CACHE.get(jedis, companyIds);
	}

	public static Map<Long, OfflineTrainingDAOProtos.TrainIndexList> getOpenTrainIndexList(Jedis jedis, Collection<Long> companyIds, Collection<Long> noCacheCompanyIds) {
		return OPEN_TRAIN_INDEX_LIST_CACHE.get(jedis, companyIds, noCacheCompanyIds);
	}

	public static void setOpenTrainIndexList(Jedis jedis, Map<Long, OfflineTrainingDAOProtos.TrainIndexList> openTrainIndexListMap) {
		OPEN_TRAIN_INDEX_LIST_CACHE.set(jedis, openTrainIndexListMap);
	}

	public static void delOpenTrainIndexList(Jedis jedis, Collection<Long> companyIds) {
		OPEN_TRAIN_INDEX_LIST_CACHE.del(jedis, companyIds);
	}
	
	private static final JedisValueCache<Long, OfflineTrainingDAOProtos.TrainIndexList> CLOSED_TRAIN_INDEX_LIST_CACHE = 
			JedisValueCache.create("offline_training:closed_train_index_list:", OfflineTrainingDAOProtos.TrainIndexList.PARSER);

	public static Map<Long, OfflineTrainingDAOProtos.TrainIndexList> getClosedTrainIndexList(Jedis jedis, Collection<Long> companyIds) {
		return CLOSED_TRAIN_INDEX_LIST_CACHE.get(jedis, companyIds);
	}

	public static Map<Long, OfflineTrainingDAOProtos.TrainIndexList> getClosedTrainIndexList(Jedis jedis, Collection<Long> companyIds, Collection<Long> noCacheCompanyIds) {
		return CLOSED_TRAIN_INDEX_LIST_CACHE.get(jedis, companyIds, noCacheCompanyIds);
	}

	public static void setClosedTrainIndexList(Jedis jedis, Map<Long, OfflineTrainingDAOProtos.TrainIndexList> closedTrainIndexListMap) {
		CLOSED_TRAIN_INDEX_LIST_CACHE.set(jedis, closedTrainIndexListMap);
	}

	public static void delClosedTrainIndexList(Jedis jedis, Collection<Long> companyIds) {
		CLOSED_TRAIN_INDEX_LIST_CACHE.del(jedis, companyIds);
	}
}
