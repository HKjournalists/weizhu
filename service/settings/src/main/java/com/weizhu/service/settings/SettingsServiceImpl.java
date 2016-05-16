package com.weizhu.service.settings;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.inject.Inject;
import com.weizhu.common.db.DBUtil;
import com.weizhu.proto.AdminProtos.AdminHead;
import com.weizhu.proto.SettingsProtos.GetUserSettingsRequest;
import com.weizhu.proto.SettingsProtos.GetUserSettingsResponse;
import com.weizhu.proto.SettingsProtos.SetDoNotDisturbRequest;
import com.weizhu.proto.SettingsProtos.SettingsResponse;
import com.weizhu.proto.SettingsProtos;
import com.weizhu.proto.SettingsService;
import com.weizhu.proto.WeizhuProtos.EmptyRequest;
import com.weizhu.proto.WeizhuProtos.RequestHead;
import com.weizhu.proto.WeizhuProtos.SystemHead;
import com.zaxxer.hikari.HikariDataSource;

public class SettingsServiceImpl implements SettingsService {

	private static final Logger logger = LoggerFactory.getLogger(SettingsServiceImpl.class);
	
	private final HikariDataSource hikariDataSource;
	private final JedisPool jedisPool;

	@Inject
	public SettingsServiceImpl(HikariDataSource hikariDataSource, JedisPool jedisPool) {
		this.hikariDataSource = hikariDataSource;
		this.jedisPool = jedisPool;
	}

	@Override
	public ListenableFuture<SettingsResponse> getSettings(RequestHead head, EmptyRequest request) {
		final long companyId = head.getSession().getCompanyId();
		final long userId = head.getSession().getUserId();
		
		SettingsProtos.Settings settings = this.doGetSettings(companyId, Collections.<Long>singleton(userId)).get(userId);
		
		if (settings == null) {
			return Futures.immediateFuture(SettingsResponse.newBuilder()
					.setSettings(SettingsProtos.Settings.newBuilder()
							.setUserId(userId)
							.build())
					.build());
		} else {
			return Futures.immediateFuture(SettingsResponse.newBuilder()
					.setSettings(settings)
					.build());
		}
	}
	
	private static final int MAX_DAY_SECONDS = 24 * 60 * 60 - 1;

	@Override
	public ListenableFuture<SettingsResponse> setDoNotDisturb(RequestHead head, SetDoNotDisturbRequest request) {
		final long companyId = head.getSession().getCompanyId();
		final long userId = head.getSession().getUserId();
		
		SettingsProtos.Settings.DoNotDisturb.Builder doNotDisturbBuilder = SettingsProtos.Settings.DoNotDisturb.newBuilder();
		
		if (request.getDoNotDisturb().getEnable()) {
			doNotDisturbBuilder.setEnable(true);
			if (!request.getDoNotDisturb().hasBeginTime()) {
				doNotDisturbBuilder.setBeginTime(0);
			} else if (request.getDoNotDisturb().getBeginTime() < 0 ) {
				doNotDisturbBuilder.setBeginTime(0);
			} else if (request.getDoNotDisturb().getBeginTime() > MAX_DAY_SECONDS) {
				doNotDisturbBuilder.setBeginTime(MAX_DAY_SECONDS);
			} else {
				doNotDisturbBuilder.setBeginTime(request.getDoNotDisturb().getBeginTime());
			}
			
			if (!request.getDoNotDisturb().hasEndTime()) {
				doNotDisturbBuilder.setEndTime(MAX_DAY_SECONDS);
			} else if (request.getDoNotDisturb().getEndTime() < 0) {
				doNotDisturbBuilder.setEndTime(0);
			} else if (request.getDoNotDisturb().getEndTime() > MAX_DAY_SECONDS) {
				doNotDisturbBuilder.setEndTime(MAX_DAY_SECONDS);
			} else {
				doNotDisturbBuilder.setEndTime(request.getDoNotDisturb().getEndTime());
			}
		} else {
			doNotDisturbBuilder.setEnable(false);
		}
		
		final SettingsProtos.Settings.DoNotDisturb doNotDisturb = doNotDisturbBuilder.build();
		
		SettingsProtos.Settings settings = null;
		Connection dbConn = null;
		try {
			dbConn = hikariDataSource.getConnection();
			SettingsDB.updateDoNotDisturb(dbConn, companyId, userId, doNotDisturb);
			settings = SettingsDB.getSettings(dbConn, companyId, Collections.singleton(userId)).get(userId);
		} catch (SQLException e) {
			logger.error("setDoNotDisturb db fail", e);
			throw new RuntimeException("setDoNotDisturb db fail", e);
		} finally {
			DBUtil.closeQuietly(dbConn);
		}
		
		Jedis jedis = jedisPool.getResource();
		try {
			SettingsCache.setSettings(jedis, companyId, Collections.singletonMap(userId, settings));
		} finally {
			jedis.close();
		}
		
		return Futures.immediateFuture(SettingsResponse.newBuilder()
				.setSettings(settings)
				.build());
	}

	@Override
	public ListenableFuture<GetUserSettingsResponse> getUserSettings(RequestHead head, GetUserSettingsRequest request) {
		final long companyId = head.getSession().getCompanyId();

		if (request.getUserIdCount() <= 0) {
			return Futures.immediateFuture(GetUserSettingsResponse.newBuilder().build());
		}
		
		Map<Long, SettingsProtos.Settings> settingsMap = this.doGetSettings(companyId, request.getUserIdList());
		
		return Futures.immediateFuture(GetUserSettingsResponse.newBuilder()
				.addAllSettings(settingsMap.values())
				.build());
	}
	

	@Override
	public ListenableFuture<GetUserSettingsResponse> getUserSettings(AdminHead head, GetUserSettingsRequest request) {
		if (!head.hasCompanyId() || request.getUserIdCount() <= 0) {
			return Futures.immediateFuture(GetUserSettingsResponse.newBuilder().build());
		}
		
		final long companyId = head.getCompanyId();
		Map<Long, SettingsProtos.Settings> settingsMap = this.doGetSettings(companyId, request.getUserIdList());
		
		return Futures.immediateFuture(GetUserSettingsResponse.newBuilder()
				.addAllSettings(settingsMap.values())
				.build());
	}
	
	@Override
	public ListenableFuture<GetUserSettingsResponse> getUserSettings(SystemHead head, GetUserSettingsRequest request) {
		if (!head.hasCompanyId() || request.getUserIdCount() <= 0) {
			return Futures.immediateFuture(GetUserSettingsResponse.newBuilder().build());
		}
		
		final long companyId = head.getCompanyId();
		Map<Long, SettingsProtos.Settings> settingsMap = this.doGetSettings(companyId, request.getUserIdList());
		
		return Futures.immediateFuture(GetUserSettingsResponse.newBuilder()
				.addAllSettings(settingsMap.values())
				.build());
	}
	
	private Map<Long, SettingsProtos.Settings> doGetSettings(long companyId, Collection<Long> userIds) {
		if (userIds.isEmpty()) {
			return Collections.emptyMap();
		}
		
		Map<Long, SettingsProtos.Settings> resultMap = new HashMap<Long, SettingsProtos.Settings>();
		
		Set<Long> noCacheUserIdSet = new TreeSet<Long>();
		Jedis jedis = jedisPool.getResource();
		try {
			resultMap.putAll(SettingsCache.getSettings(jedis, companyId, userIds, noCacheUserIdSet));
		} finally {
			jedis.close();
		}
		
		if (noCacheUserIdSet.isEmpty()) {
			return resultMap;
		}
		
		Map<Long, SettingsProtos.Settings> noCacheMap;
		Connection dbConn = null;
		try {
			dbConn = hikariDataSource.getConnection();
			noCacheMap = SettingsDB.getSettings(dbConn, companyId, noCacheUserIdSet);
		} catch (SQLException e) {
			logger.error("getUserSettings db fail", e);
			throw new RuntimeException("db fail", e);
		} finally {
			DBUtil.closeQuietly(dbConn);
		}
		
		jedis = jedisPool.getResource();
		try {
			SettingsCache.setSettings(jedis, companyId, noCacheUserIdSet, noCacheMap);
		} finally {
			jedis.close();
		}
		
		resultMap.putAll(noCacheMap);
		
		return resultMap;
	}

}
