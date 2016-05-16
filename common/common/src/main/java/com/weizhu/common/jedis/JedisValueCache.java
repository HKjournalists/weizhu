package com.weizhu.common.jedis;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import redis.clients.jedis.Jedis;

import com.google.common.base.Charsets;
import com.google.common.primitives.Bytes;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Message;
import com.google.protobuf.Parser;

/**
 * redis kv cache封装
 * @author lindongjlu
 *
 * @param <K>
 * @param <V>
 */
public class JedisValueCache<K, V extends Message> {

	private static final Logger logger = LoggerFactory.getLogger(JedisValueCache.class);
	
	private final byte[] keyDomain;
	private final Parser<V> valueParser;
	
	private JedisValueCache(String keyDomain, Parser<V> valueParser) {
		this.keyDomain = keyDomain.getBytes(Charsets.UTF_8);
		this.valueParser = valueParser;
	}
	
	/**
	 * 批量获取value
	 * @param jedis
	 * @param keys
	 * @return
	 */
	public Map<K, V> get(Jedis jedis, Collection<K> keys) {
		if (keys.isEmpty()) {
			return Collections.emptyMap();
		}
		return this.get(jedis, keys, new ArrayList<K>());
	}
	
	/**
	 * 批量获取value
	 * @param jedis
	 * @param keys
	 * @param noCacheKeys 不在cache中的key
	 * @return
	 */
	public Map<K, V> get(Jedis jedis, Collection<K> keys, Collection<K> noCacheKeys) {
		if (keys.isEmpty()) {
			return Collections.emptyMap();
		}
		
		byte[][] mKey = new byte[keys.size()][];
		int idx = 0;
		for (K key : keys) {
			mKey[idx] = Bytes.concat(keyDomain, String.valueOf(key).getBytes(Charsets.UTF_8));
			++idx;
		}
		
		List<byte[]> dataList = jedis.mget(mKey);
		
		Map<K, V> resultMap = new HashMap<K, V>(dataList.size());
		
		Iterator<K> keyIt = keys.iterator();
		Iterator<byte[]> dataIt = dataList.iterator();
		
		while(keyIt.hasNext() && dataIt.hasNext()) {
			K key = keyIt.next();
			byte[] data = dataIt.next();
			if (data == null) {
				noCacheKeys.add(key);
			} else if (data.length > 0) {
				try {
					resultMap.put(key, valueParser.parseFrom(data));
				} catch (InvalidProtocolBufferException e) {
					// parse fail : log error
					logger.warn("cache key : " + new String(keyDomain, Charsets.UTF_8) + key + " parse fail", e);
					// parse fail : add no cache keys
					noCacheKeys.add(key);
				}
			}
		}
		return resultMap;
	}
	
	/**
	 * 批量设置value<br>
	 * @param jedis
	 * @param keyValueMap
	 */
	public void set(Jedis jedis, Map<K, V> keyValueMap) {
		this.set(jedis, keyValueMap.keySet(), keyValueMap);
	}
	
	/**
	 * 批量设置value<br>
	 * 如果再keys中的key 在keyValueMap无法找到对应的value，则放置一个空byte[0]到redis，标记该key没有value。避免下次读取时没有该值，仍取读db
	 * @param jedis
	 * @param keys 需要设置的key
	 * @param keyValueMap 放置value的map
	 */
	public void set(Jedis jedis, Collection<K> keys, Map<K, V> keyValueMap) {
		if (keys.isEmpty()) {
			return;
		}
		
		byte[][] kvs = new byte[keys.size() * 2][];
		int idx = 0;
		for (K key : keys) {
			V value = keyValueMap.get(key);
			
			kvs[idx] = Bytes.concat(keyDomain, String.valueOf(key).getBytes(Charsets.UTF_8));
			kvs[idx + 1] = value == null ? JedisUtil.EMPTY_BYTES : value.toByteArray();
			idx += 2;
		}
		
		jedis.mset(kvs);
	}
	
	/**
	 * 批量删除key
	 * @param jedis
	 * @param keys
	 */
	public void del(Jedis jedis, Collection<K> keys) {
		if (keys.isEmpty()) {
			return ;
		}
		
		byte[][] mKey = new byte[keys.size()][];
		int idx = 0;
		for (K key : keys) {
			mKey[idx] = Bytes.concat(keyDomain, String.valueOf(key).getBytes(Charsets.UTF_8));
			idx++;
		}
		jedis.del(mKey);
	}
	
	public static <K, V extends Message> JedisValueCache<K, V> create(String keyDomain, Parser<V> valueParser) {
		return new JedisValueCache<K, V>(keyDomain, valueParser);
	}
}
