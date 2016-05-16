package com.weizhu.service.tools.productcolock;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.weizhu.common.db.DBUtil;
import com.weizhu.proto.ProductclockProtos;
import com.zaxxer.hikari.HikariDataSource;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

public class ToolsProductclockUtil {

	public static Map<Integer, ProductclockProtos.Customer> getCustomer(HikariDataSource hikariDataSource, JedisPool jedisPool, 
			long companyId, Collection<Integer> customerIds) {
		if (customerIds.isEmpty()) {
			return Collections.emptyMap();
		}
		
		Map<Integer, ProductclockProtos.Customer> customerMap = Maps.newHashMap();
		
		List<Integer> noCachedCustomerIdList = Lists.newArrayList();
		Jedis jedis = jedisPool.getResource();
		try {
			customerMap.putAll(ToolsProductclockCache.getCustomer(jedis, companyId, customerIds, noCachedCustomerIdList));
		} finally {
			jedis.close();
		}
		
		if (noCachedCustomerIdList.isEmpty()) {
			return customerMap;
		}
		
		Map<Integer, ProductclockProtos.Customer> noCacheCustomerMap = Maps.newHashMap();
		Connection conn = null;
		try {
			conn = hikariDataSource.getConnection();
			
			noCacheCustomerMap = ToolsProductclockDB.getCustomerById(conn, companyId, noCachedCustomerIdList);
		} catch (SQLException ex) {
			throw new RuntimeException("db fail", ex);
		} finally {
			DBUtil.closeQuietly(conn);
		}
		
		jedis = jedisPool.getResource();
		try {
			ToolsProductclockCache.setCustomer(jedis, companyId, noCachedCustomerIdList, noCacheCustomerMap);
		} finally {
			jedis.close();
		}
		
		customerMap.putAll(noCacheCustomerMap);
		
		return customerMap;
	}
	
	public static Map<Integer, ProductclockProtos.Product> getProduct(HikariDataSource hikariDataSource, JedisPool jedisPool, 
			long companyId, Collection<Integer> productIds) {
		if (productIds.isEmpty()) {
			return Collections.emptyMap();
		}
		
		Map<Integer, ProductclockProtos.Product> productMap = Maps.newHashMap();
		
		List<Integer> noCachedProductIdList = Lists.newArrayList();
		Jedis jedis = jedisPool.getResource();
		try {
			productMap.putAll(ToolsProductclockCache.getProduct(jedis, companyId, productIds, noCachedProductIdList));
		} finally {
			jedis.close();
		}
		
		if (noCachedProductIdList.isEmpty()) {
			return productMap;
		}
		
		Map<Integer, ProductclockProtos.Product> noCachedProductMap = Maps.newHashMap();
		Connection conn = null;
		try {
			conn = hikariDataSource.getConnection();
			
			noCachedProductMap = ToolsProductclockDB.getProductById(conn, companyId, noCachedProductIdList);
		} catch (SQLException ex) {
			throw new RuntimeException("db fail", ex);
		} finally {
			DBUtil.closeQuietly(conn);
		}
		
		jedis = jedisPool.getResource();
		try {
			ToolsProductclockCache.setProduct(jedis, companyId, noCachedProductIdList, noCachedProductMap);
		} finally {
			jedis.close();
		}
		
		productMap.putAll(noCachedProductMap);
		
		return productMap;
	}
	
}
