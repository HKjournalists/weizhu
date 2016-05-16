package com.weizhu.service.credits;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import com.google.common.base.Charsets;
import com.google.common.hash.Hashing;
import com.weizhu.common.db.DBUtil;
import com.weizhu.proto.CreditsProtos;
import com.zaxxer.hikari.HikariDataSource;

public class CreditsUtil {

	public static Map<Long, CreditsProtos.Credits> getUserCredits(HikariDataSource hikariDataSource, JedisPool jedisPool, long companyId, Collection<Long> userIds) {
		
		Map<Long, CreditsProtos.Credits> creditsMap = new HashMap<Long, CreditsProtos.Credits>();
		
		Jedis jedis = jedisPool.getResource();
		List<Long> noCacheUserIdList = new ArrayList<Long>();
		
		try {
			creditsMap = CreditsCache.getCredits(jedis, companyId, userIds, noCacheUserIdList);
		} finally {
			jedis.close();
		}
		
		if (noCacheUserIdList.isEmpty()) {
			return creditsMap;
		}
		
		Map<Long, CreditsProtos.Credits> noCacheCreditsMap = null;
		
		Connection conn = null;
		try {
			conn = hikariDataSource.getConnection();
			
			noCacheCreditsMap = CreditsDB.getUserCredits(conn, companyId, noCacheUserIdList);
		} catch (SQLException ex) {
			throw new RuntimeException("db fail", ex);
		} finally {
			DBUtil.closeQuietly(conn);
		}
		
		jedis = jedisPool.getResource();
		
		try {
			CreditsCache.setCredits(jedis, companyId, noCacheUserIdList, noCacheCreditsMap);
		} finally {
			jedis.close();
		}
		
		creditsMap.putAll(noCacheCreditsMap);
		
		return creditsMap;
	}
	
	public static String createMD5(String param) {
		return Hashing.md5().hashBytes(param.getBytes(Charsets.UTF_8)).toString();
	}
	
}
