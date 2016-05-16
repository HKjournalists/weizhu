package com.weizhu.service.settings;

import java.util.Collection;
import java.util.Map;
import redis.clients.jedis.Jedis;

import com.weizhu.common.jedis.JedisValueCacheEx;
import com.weizhu.proto.SettingsProtos;

public class SettingsCache {
	
	private static final JedisValueCacheEx<Long, SettingsProtos.Settings> SETTINGS_CACHE = 
			JedisValueCacheEx.create("settings:", SettingsProtos.Settings.PARSER);
	
	public static Map<Long, SettingsProtos.Settings> getSettings(Jedis jedis, long companyId, Collection<Long> userIds) {
		return SETTINGS_CACHE.get(jedis, companyId, userIds);
	}
	
	public static Map<Long, SettingsProtos.Settings> getSettings(Jedis jedis, long companyId, Collection<Long> userIds, Collection<Long> noCacheUserIds) {
		return SETTINGS_CACHE.get(jedis, companyId, userIds, noCacheUserIds);
	}
	
	public static void setSettings(Jedis jedis, long companyId, Map<Long, SettingsProtos.Settings> settingsMap) {
		SETTINGS_CACHE.set(jedis, companyId, settingsMap);
	}
	
	public static void setSettings(Jedis jedis, long companyId, Collection<Long> userIds, Map<Long, SettingsProtos.Settings> settingsMap) {
		SETTINGS_CACHE.set(jedis, companyId, userIds, settingsMap);
	}
	
	public static void delSettings(Jedis jedis, long companyId, Collection<Long> userIds) {
		SETTINGS_CACHE.del(jedis, companyId, userIds);
	}
}
