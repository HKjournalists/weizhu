package com.weizhu.service.upload;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import com.weizhu.common.jedis.JedisValueCache;
import com.weizhu.common.jedis.JedisValueCacheEx;
import com.weizhu.proto.UploadProtos;

import redis.clients.jedis.Jedis;

public class UploadCache {

	private static final JedisValueCache<String, UploadProtos.Image> IMAGE_CACHE = 
			JedisValueCache.create("upload:image:", UploadProtos.Image.PARSER);
	
	public static Map<String, UploadProtos.Image> getImage(Jedis jedis, Collection<String> imageNames) {
		return IMAGE_CACHE.get(jedis, imageNames);
	}
	
	public static Map<String, UploadProtos.Image> getImage(Jedis jedis, Collection<String> imageNames, Collection<String> noCacheImageNames) {
		return IMAGE_CACHE.get(jedis, imageNames, noCacheImageNames);
	}
	
	public static void setImage(Jedis jedis, Map<String, UploadProtos.Image> imageMap) {
		IMAGE_CACHE.set(jedis, imageMap);
	}
	
	public static void setImage(Jedis jedis, Collection<String> imageNames, Map<String, UploadProtos.Image> imageMap) {
		IMAGE_CACHE.set(jedis, imageNames, imageMap);
	}
	
	public static void delImage(Jedis jedis, Collection<String> imageNames) {
		IMAGE_CACHE.del(jedis, imageNames);
	}
	
	
	private static final JedisValueCacheEx<String, UploadDAOProtos.ImageTagList> IMAGE_TAG_LIST_CACHE = 
			JedisValueCacheEx.create("upload:image_tag:", UploadDAOProtos.ImageTagList.PARSER);
	
	public static Map<String, List<String>> getImageTagList(Jedis jedis, long companyId, Collection<String> imageNames) {
		return convertToList(IMAGE_TAG_LIST_CACHE.get(jedis, companyId, imageNames));
	}
	
	public static Map<String, List<String>> getImageTagList(Jedis jedis, long companyId, Collection<String> imageNames, Collection<String> noCacheImageNames) {
		return convertToList(IMAGE_TAG_LIST_CACHE.get(jedis, companyId, imageNames, noCacheImageNames));
	}
	
	public static void setImageTagList(Jedis jedis, long companyId, Map<String, List<String>> imageTagListMap) {
		IMAGE_TAG_LIST_CACHE.set(jedis, companyId, convertToDAO(imageTagListMap));
	}
	
	public static void setImageTagList(Jedis jedis, long companyId, Collection<String> imageNames, Map<String, List<String>> imageTagListMap) {
		IMAGE_TAG_LIST_CACHE.set(jedis, companyId, imageNames, convertToDAO(imageTagListMap));
	}
	
	public static void delImageTagList(Jedis jedis, long companyId, Collection<String> imageNames) {
		IMAGE_TAG_LIST_CACHE.del(jedis, companyId, imageNames);
	}
	
	private static Map<String, List<String>> convertToList(Map<String, UploadDAOProtos.ImageTagList> daoMap) {
		if (daoMap.isEmpty()) {
			return Collections.emptyMap();
		}
		
		Map<String, List<String>> resultMap = new TreeMap<String, List<String>>();
		for (Map.Entry<String, UploadDAOProtos.ImageTagList> entry : daoMap.entrySet()) {
			resultMap.put(entry.getKey(), entry.getValue().getTagList());
		}
		return resultMap;
	}
	
	private static Map<String, UploadDAOProtos.ImageTagList> convertToDAO(Map<String, List<String>> tagListMap) {
		if (tagListMap.isEmpty()) {
			return Collections.emptyMap();
		}
		
		Map<String, UploadDAOProtos.ImageTagList> resultMap = new TreeMap<String, UploadDAOProtos.ImageTagList>();
		
		UploadDAOProtos.ImageTagList.Builder tmpBuilder = UploadDAOProtos.ImageTagList.newBuilder();
		for (Map.Entry<String, List<String>> entry : tagListMap.entrySet()) {
			tmpBuilder.clear();
			resultMap.put(entry.getKey(), tmpBuilder.addAllTag(entry.getValue()).build());
		}
		return resultMap;
	}
	
}
