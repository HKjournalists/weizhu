package com.weizhu.service.discover;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.base.Charsets;
import com.google.protobuf.InvalidProtocolBufferException;
import com.weizhu.common.jedis.JedisUtil;
import com.weizhu.common.jedis.JedisValueCache;
import com.weizhu.proto.DiscoverProtos;
import redis.clients.jedis.Jedis;

public class DiscoverCache {
	
	private static final JedisValueCache<Long, DiscoverProtos.ItemContent> DISCOVER_ITEM_CONTENT_CACHE = 
			JedisValueCache.create("discover:item:", DiscoverProtos.ItemContent.PARSER);
	
	public static Map<Long, DiscoverProtos.ItemContent> getItemContent(Jedis jedis, Collection<Long> itemIds) {
		return DISCOVER_ITEM_CONTENT_CACHE.get(jedis, itemIds);
	}
	
	public static Map<Long, DiscoverProtos.ItemContent> getItemContent(Jedis jedis, Collection<Long> itemIds, Collection<Long> noCacheItemIds) {
		return DISCOVER_ITEM_CONTENT_CACHE.get(jedis, itemIds, noCacheItemIds);
	}
	
	public static void setItemContent(Jedis jedis, Map<Long, DiscoverProtos.ItemContent> itemContentMap) {
		DISCOVER_ITEM_CONTENT_CACHE.set(jedis, itemContentMap);
	}
	
	public static void setItemContent(Jedis jedis, Collection<Long> itemIds, Map<Long, DiscoverProtos.ItemContent> itemContentMap) {
		DISCOVER_ITEM_CONTENT_CACHE.set(jedis, itemIds, itemContentMap);
	}
	
	public static void delItemContent(Jedis jedis, Collection<Long> itemIds) {
		DISCOVER_ITEM_CONTENT_CACHE.del(jedis, itemIds);
	}
	
	private static final byte[] DISCOVER_HOME_KEY = "discover:home".getBytes();
	private static final byte[] DISCOVER_MODULE_ITEM_DEFAULT_DOMAIN = "discover:module_item:default:".getBytes();
	private static final byte[] DISCOVER_ITEM_PV_DOMAIN = "discover:item_pv:".getBytes();
	
	private static final byte[] INCRX_SCRIPT = "if redis.call('exists', KEYS[1]) == 1 then return redis.call('incr', KEYS[1]) else return nil end".getBytes();
	private static byte[] INCRX_SCRIPT_SHA;
	
	public static void loadScript(Jedis jedis) {
		INCRX_SCRIPT_SHA = jedis.scriptLoad(INCRX_SCRIPT);
	}

	public static DiscoverDAOProtos.DiscoverHome getDiscoverHome(Jedis jedis) {
		byte[] data = jedis.get(DISCOVER_HOME_KEY);
		if (data == null || data.length <= 0) {
			return null;
		} else {
			try {
				return DiscoverDAOProtos.DiscoverHome.parseFrom(data);
			} catch (InvalidProtocolBufferException e) {
				return null;
			}
		}
	}
	
	public static void setDiscoverHome(Jedis jedis, DiscoverDAOProtos.DiscoverHome discoverHome) {
		jedis.set(DISCOVER_HOME_KEY, JedisUtil.makeValue(discoverHome));
	}
	
	public static DiscoverDAOProtos.ModuleItemDefaultList getModuleItemDefaultList(Jedis jedis, int moduleId, int categoryId) {
		byte[] key = JedisUtil.makeKey(DISCOVER_MODULE_ITEM_DEFAULT_DOMAIN, moduleId, categoryId);
		byte[] data = jedis.get(key);
		if (data == null || data.length <= 0) {
			return null;
		} else {
			try {
				return DiscoverDAOProtos.ModuleItemDefaultList.parseFrom(data);
			} catch (InvalidProtocolBufferException e) {
				return null;
			}
		}
	}
	
	public static void setModuleItemDefaultList(Jedis jedis, int moduleId, int categoryId, DiscoverDAOProtos.ModuleItemDefaultList moduleItemDefaultList) {
		byte[] key = JedisUtil.makeKey(DISCOVER_MODULE_ITEM_DEFAULT_DOMAIN, moduleId, categoryId);
		jedis.set(key, JedisUtil.makeValue(moduleItemDefaultList));
	}
	
	public static void clearCache(Jedis jedis) {
		Set<byte[]> keys = jedis.keys("discover:*".getBytes());
		if (keys.isEmpty()) {
			return;
		}
		jedis.del(keys.toArray(new byte[keys.size()][]));
	}
	
	public static Map<Long, Integer> getItemPV(Jedis jedis, Collection<Long> itemIds, Collection<Long> noCacheItemIds) {
		if (itemIds.isEmpty()) {
			return Collections.emptyMap();
		}
		
		byte[][] keys = new byte[itemIds.size()][];
		int idx = 0;
		for (Long itemId : itemIds) {
			keys[idx] = JedisUtil.makeKey(DISCOVER_ITEM_PV_DOMAIN, itemId);
			idx++;
		}
		
		List<byte[]> dataList = jedis.mget(keys);
		
		Map<Long, Integer> resultMap = new HashMap<Long, Integer>(itemIds.size());
		
		Iterator<Long> itemIdIt = itemIds.iterator();
		Iterator<byte[]> dataIt = dataList.iterator();
		
		while (itemIdIt.hasNext() && dataIt.hasNext()) {
			Long itemId = itemIdIt.next();
			byte[] data = dataIt.next();
			
			if (data == null) {
				noCacheItemIds.add(itemId);
			} else {
				resultMap.put(itemId, Integer.parseInt(new String(data, Charsets.UTF_8)));
			}
		}
		return resultMap;
	}
	
	public static void setItemPV(Jedis jedis, Map<Long, Integer> itemPVMap) {
		if (itemPVMap.isEmpty()) {
			return ;
		}
		
		byte[][] keyvalues = new byte[itemPVMap.size() * 2][];
		int idx = 0;
		for (Map.Entry<Long, Integer> entry : itemPVMap.entrySet()) {
			keyvalues[idx] = JedisUtil.makeKey(DISCOVER_ITEM_PV_DOMAIN, entry.getKey());
			keyvalues[idx + 1] = entry.getValue().toString().getBytes(Charsets.UTF_8);
			idx += 2;
		}
		jedis.mset(keyvalues);
	}
	
	public static Long increItemPV(Jedis jedis, long itemId) {
		return (Long) jedis.evalsha(INCRX_SCRIPT_SHA, 1, JedisUtil.makeKey(DISCOVER_ITEM_PV_DOMAIN, itemId));
	}
	
}
