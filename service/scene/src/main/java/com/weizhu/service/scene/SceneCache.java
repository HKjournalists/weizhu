package com.weizhu.service.scene;

import java.util.Collection;
import java.util.Map;

import redis.clients.jedis.Jedis;

import com.weizhu.common.jedis.JedisValueCache;
import com.weizhu.common.jedis.JedisValueCacheEx;

public class SceneCache {

	private static final JedisValueCacheEx<Integer, SceneDAOProtos.SceneExt> SCENE_SCENE_EXT_CACHE = 
			JedisValueCacheEx.create("scene:scene_ext:", SceneDAOProtos.SceneExt.PARSER);
	
	private static final JedisValueCache<Long, SceneDAOProtos.SceneHome> SCENE_HOME_CACHE = 
			JedisValueCache.create("scene:home:", SceneDAOProtos.SceneHome.PARSER);

	public static Map<Long, SceneDAOProtos.SceneHome> getSceneHome(Jedis jedis, Collection<Long> companyIds) {
		return SCENE_HOME_CACHE.get(jedis, companyIds);
	}

	public static Map<Long, SceneDAOProtos.SceneHome> getSceneHome(Jedis jedis, Collection<Long> companyIds, Collection<Long> noCacheCompanyIds) {
		return SCENE_HOME_CACHE.get(jedis, companyIds, noCacheCompanyIds);
	}
	
	public static void setSceneHome(Jedis jedis, Map<Long, SceneDAOProtos.SceneHome> sceneHomeMap) {
		SCENE_HOME_CACHE.set(jedis, sceneHomeMap);
	}
	
	public static void delSceneHome(Jedis jedis, Collection<Long> companyIds) {
		SCENE_HOME_CACHE.del(jedis, companyIds);
	}
	

	public static Map<Integer, SceneDAOProtos.SceneExt> getSceneExt(Jedis jedis, long companyId, Collection<Integer> sceneIds) {
		return SCENE_SCENE_EXT_CACHE.get(jedis, companyId, sceneIds);
	}
	
	public static Map<Integer, SceneDAOProtos.SceneExt> getSceneExt(Jedis jedis, long companyId, Collection<Integer> sceneIds, Collection<Integer> noCacheSceneIds) {
		return SCENE_SCENE_EXT_CACHE.get(jedis, companyId, sceneIds, noCacheSceneIds);
	}
	
	public static void setSceneExt(Jedis jedis, long companyId, Map<Integer, SceneDAOProtos.SceneExt> sceneExtMap) {
		SCENE_SCENE_EXT_CACHE.set(jedis, companyId, sceneExtMap);
	}
	
	public static void setSceneExt(Jedis jedis, long companyId, Collection<Integer> sceneIds, Map<Integer, SceneDAOProtos.SceneExt> sceneExtMap) {
		SCENE_SCENE_EXT_CACHE.set(jedis, companyId, sceneIds, sceneExtMap);
	}
	
	public static void delSceneExt(Jedis jedis, long companyId, Collection<Integer> sceneIds) {
		SCENE_SCENE_EXT_CACHE.del(jedis, companyId, sceneIds);
	}
	
}
