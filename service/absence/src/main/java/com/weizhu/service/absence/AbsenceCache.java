package com.weizhu.service.absence;

import java.util.Collection;
import java.util.Map;

import redis.clients.jedis.Jedis;

import com.weizhu.common.jedis.JedisValueCacheEx;
import com.weizhu.proto.AbsenceProtos;

public class AbsenceCache {

	private static final JedisValueCacheEx<Integer, AbsenceProtos.Absence> ABSENCE_CACHE =
			JedisValueCacheEx.create("absence:", AbsenceProtos.Absence.PARSER);
	
	public static Map<Integer, AbsenceProtos.Absence> getAbsence(Jedis jedis, long companyId, Collection<Integer> absenceIds) {
		return ABSENCE_CACHE.get(jedis, companyId, absenceIds);
	}
	
	public static Map<Integer, AbsenceProtos.Absence> getAbsence(Jedis jedis, long companyId, Collection<Integer> absenceIds, Collection<Integer> noCacheAbsenceIds) {
		return ABSENCE_CACHE.get(jedis, companyId, absenceIds, noCacheAbsenceIds);
	}
	
	public static void setAbsence(Jedis jedis, long companyId, Map<Integer, AbsenceProtos.Absence> absenceMap) {
		ABSENCE_CACHE.set(jedis, companyId, absenceMap);
	}
	
	public static void setAbsence(Jedis jedis, long companyId, Collection<Integer> absenceIds, Map<Integer, AbsenceProtos.Absence> absenceMap) {
		ABSENCE_CACHE.set(jedis, companyId, absenceIds, absenceMap);
	}
	
	public static void delAbsence(Jedis jedis, long companyId, Collection<Integer> absenceIds) {
		ABSENCE_CACHE.del(jedis, companyId, absenceIds);
	}
}
