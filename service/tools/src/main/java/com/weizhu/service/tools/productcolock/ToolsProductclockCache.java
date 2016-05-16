package com.weizhu.service.tools.productcolock;

import java.util.Collection;
import java.util.Map;

import com.weizhu.common.jedis.JedisValueCacheEx;
import com.weizhu.proto.ProductclockProtos;

import redis.clients.jedis.Jedis;

public class ToolsProductclockCache {

	private static final JedisValueCacheEx<Integer, ProductclockProtos.Customer> CUSTOMER_CACHE = 
			JedisValueCacheEx.create("tools:productclock:customer", ProductclockProtos.Customer.PARSER);
	private static final JedisValueCacheEx<Integer, ProductclockProtos.Product> PRODUCT_CACHE = 
			JedisValueCacheEx.create("tools:productclock:product", ProductclockProtos.Product.PARSER);
	
	public static Map<Integer, ProductclockProtos.Customer> getCustomer(Jedis jedis, long companyId, Collection<Integer> customerIds) {
		return CUSTOMER_CACHE.get(jedis, companyId, customerIds);
	}
	
	public static Map<Integer, ProductclockProtos.Customer> getCustomer(Jedis jedis, long companyId, Collection<Integer> customerIds, Collection<Integer> noCacheCustomerIds) {
		return CUSTOMER_CACHE.get(jedis, companyId, customerIds, noCacheCustomerIds);
	}
	
	public static void setCustomer(Jedis jedis, long companyId, Map<Integer, ProductclockProtos.Customer> customerMap) {
		CUSTOMER_CACHE.set(jedis, companyId, customerMap);
	}
	
	public static void setCustomer(Jedis jedis, long companyId, Collection<Integer> customerIds, Map<Integer, ProductclockProtos.Customer> customerMap) {
		CUSTOMER_CACHE.set(jedis, companyId, customerIds, customerMap);
	}
	
	public static void delCustomer(Jedis jedis, long companyId, Collection<Integer> customerIds) {
		CUSTOMER_CACHE.del(jedis, companyId, customerIds);
	}
	
	public static Map<Integer, ProductclockProtos.Product> getProduct(Jedis jedis, long companyId, Collection<Integer> productIds) {
		return PRODUCT_CACHE.get(jedis, companyId, productIds);
	}
	
	public static Map<Integer, ProductclockProtos.Product> getProduct(Jedis jedis, long companyId, Collection<Integer> productIds, Collection<Integer> noCacheProductIds) {
		return PRODUCT_CACHE.get(jedis, companyId, productIds, noCacheProductIds);
	}
	
	public static void setProduct(Jedis jedis, long companyId, Map<Integer, ProductclockProtos.Product> productMap) {
		PRODUCT_CACHE.set(jedis, companyId, productMap);
	}
	
	public static void setProduct(Jedis jedis, long companyId, Collection<Integer> productIds, Map<Integer, ProductclockProtos.Product> productMap) {
		PRODUCT_CACHE.set(jedis, companyId, productIds, productMap);
	}
	
	public static void delProduct(Jedis jedis, long companyId, Collection<Integer> productIds) {
		PRODUCT_CACHE.del(jedis, companyId, productIds);
	}
}
