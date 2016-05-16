package com.weizhu.service.offline_training;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.annotation.Nullable;

import com.weizhu.common.db.DBUtil;
import com.weizhu.proto.OfflineTrainingProtos;
import com.zaxxer.hikari.HikariDataSource;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

public class OfflineTrainingUtil {

	public static Map<Integer, OfflineTrainingProtos.Train> getTrain(HikariDataSource hikariDataSource, JedisPool jedisPool, long companyId, 
			Collection<Integer> trainIds, 
			@Nullable Collection<OfflineTrainingProtos.State> states
			) {
		if (trainIds.isEmpty() || (states != null && states.isEmpty())) {
			return Collections.emptyMap();
		}
		
		Map<Integer, OfflineTrainingProtos.Train> trainMap = new TreeMap<Integer, OfflineTrainingProtos.Train>();
		
		List<Integer> noCacheTrainIdList = new ArrayList<Integer>();
		Jedis jedis = jedisPool.getResource();
		try {
			trainMap.putAll(OfflineTrainingCache.getTrain(jedis, companyId, trainIds, noCacheTrainIdList));
		} finally {
			jedis.close();
		}
		
		if (!noCacheTrainIdList.isEmpty()) {
			Connection dbConn = null;
			Map<Integer, OfflineTrainingProtos.Train> noCacheTrainMap;;
			try {
				dbConn = hikariDataSource.getConnection();
				noCacheTrainMap = OfflineTrainingDB.getTrain(dbConn, companyId, noCacheTrainIdList, null);
			} catch (SQLException ex) {
				throw new RuntimeException("db fail", ex);
			} finally {
				DBUtil.closeQuietly(dbConn);
			}
			
			jedis = jedisPool.getResource();
			try {
				OfflineTrainingCache.setTrain(jedis, companyId, noCacheTrainIdList, noCacheTrainMap);
			} finally {
				jedis.close();
			}
			
			trainMap.putAll(noCacheTrainMap);
		}
		
		if (states != null) {
			Iterator<OfflineTrainingProtos.Train> it = trainMap.values().iterator();
			while (it.hasNext()) {
				if (!states.contains(it.next().getState())) {
					it.remove();
				}
			}
		}
		return trainMap;
	}
	
	public static Map<Integer, OfflineTrainingProtos.TrainCount> getTrainCount(HikariDataSource hikariDataSource, JedisPool jedisPool, long companyId, 
			Collection<Integer> trainIds
			) {
		if (trainIds.isEmpty()) {
			return Collections.emptyMap();
		}
		
		Map<Integer, OfflineTrainingProtos.TrainCount> trainCountMap = new TreeMap<Integer, OfflineTrainingProtos.TrainCount>();
		
		List<Integer> noCacheTrainIdList = new ArrayList<Integer>();
		Jedis jedis = jedisPool.getResource();
		try {
			trainCountMap.putAll(OfflineTrainingCache.getTrainCount(jedis, companyId, trainIds, noCacheTrainIdList));
		} finally {
			jedis.close();
		}
		
		if (!noCacheTrainIdList.isEmpty()) {
			Connection dbConn = null;
			Map<Integer, OfflineTrainingProtos.TrainCount> noCacheTrainCountMap;;
			try {
				dbConn = hikariDataSource.getConnection();
				noCacheTrainCountMap = OfflineTrainingDB.getTrainCount(dbConn, companyId, noCacheTrainIdList);
			} catch (SQLException ex) {
				throw new RuntimeException("db fail", ex);
			} finally {
				DBUtil.closeQuietly(dbConn);
			}
			
			jedis = jedisPool.getResource();
			try {
				OfflineTrainingCache.setTrainCount(jedis, companyId, noCacheTrainIdList, noCacheTrainCountMap);
			} finally {
				jedis.close();
			}
			
			trainCountMap.putAll(noCacheTrainCountMap);
		}
		
		return trainCountMap;
	}
}
