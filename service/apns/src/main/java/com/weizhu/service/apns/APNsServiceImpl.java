package com.weizhu.service.apns;

import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;

import java.security.KeyStore;
import java.security.Security;
import java.sql.Connection;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.Executor;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.annotation.Nullable;
import javax.net.ssl.KeyManagerFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import com.google.common.base.Charsets;
import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.name.Named;
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Message;
import com.weizhu.common.db.DBUtil;
import com.weizhu.common.service.ServiceUtil;
import com.weizhu.common.utils.HexUtil;
import com.weizhu.proto.APNsProtos;
import com.weizhu.proto.APNsProtos.DeleteDeviceTokenExpireRequest;
import com.weizhu.proto.APNsProtos.DeleteDeviceTokenRequest;
import com.weizhu.proto.APNsProtos.SendNotificationRequest;
import com.weizhu.proto.APNsProtos.UpdateDeviceTokenRequest;
import com.weizhu.proto.APNsService;
import com.weizhu.proto.AdminOfficialProtos;
import com.weizhu.proto.AdminOfficialService;
import com.weizhu.proto.AdminProtos.AdminHead;
import com.weizhu.proto.AdminUserProtos;
import com.weizhu.proto.AdminUserService;
import com.weizhu.proto.CommunityProtos;
import com.weizhu.proto.IMProtos;
import com.weizhu.proto.OfficialProtos;
import com.weizhu.proto.OfficialService;
import com.weizhu.proto.PushProtos;
import com.weizhu.proto.SettingsProtos;
import com.weizhu.proto.SettingsService;
import com.weizhu.proto.UserProtos;
import com.weizhu.proto.UserService;
import com.weizhu.proto.WeizhuProtos;
import com.weizhu.proto.WeizhuProtos.EmptyRequest;
import com.weizhu.proto.WeizhuProtos.EmptyResponse;
import com.weizhu.proto.WeizhuProtos.RequestHead;
import com.weizhu.proto.WeizhuProtos.SystemHead;
import com.weizhu.service.apns.net.APNsManager;
import com.weizhu.service.apns.net.FeedbackListener;
import com.weizhu.service.apns.net.PushNotification;
import com.zaxxer.hikari.HikariDataSource;

public class APNsServiceImpl implements APNsService {

	private static final Logger logger = LoggerFactory.getLogger(APNsServiceImpl.class);
	
	private final HikariDataSource hikariDataSource;
	private final JedisPool jedisPool;
	private final Executor serviceExecutor;	
	private final ScheduledExecutorService scheduledExecutorService;
	
	private final Provider<UserService> userServiceProvider;
	private final Provider<AdminUserService> adminUserServiceProvider;
	private final Provider<SettingsService> settingServiceProvider;
	private final Provider<OfficialService> officialServiceProvider;
	private final Provider<AdminOfficialService> adminOfficialServiceProvider;
	
	private final ImmutableList<APNsManager> apnsManagerList;
	
	@Inject
	public APNsServiceImpl(HikariDataSource hikariDataSource, JedisPool jedisPool,
			@Named("service_executor") Executor serviceExecutor, 
			@Named("service_scheduled_executor") ScheduledExecutorService scheduledExecutorService,
			Provider<UserService> userServiceProvider, 
			Provider<AdminUserService> adminUserServiceProvider, 
			Provider<SettingsService> settingServiceProvider, 
			Provider<OfficialService> officialServiceProvider, 
			Provider<AdminOfficialService> adminOfficialServiceProvider,
			NioEventLoopGroup eventLoop
			) {
		this.hikariDataSource = hikariDataSource;
		this.jedisPool = jedisPool;
		this.serviceExecutor = serviceExecutor;
		this.scheduledExecutorService = scheduledExecutorService;
		this.userServiceProvider = userServiceProvider;
		this.adminUserServiceProvider = adminUserServiceProvider;
		this.settingServiceProvider = settingServiceProvider;
		this.officialServiceProvider = officialServiceProvider;
		this.adminOfficialServiceProvider = adminOfficialServiceProvider;
		
		List<APNsProtos.APNsCert> certList;
		Connection dbConn = null;
		try {
			dbConn = this.hikariDataSource.getConnection();
			certList = APNsDB.getAllAPNsCertList(dbConn);
		} catch (SQLException e) {
			throw new RuntimeException("load cert fail", e);
		} finally {
			DBUtil.closeQuietly(dbConn);
		}
		
		try {
			String algorithm = Security.getProperty("ssl.KeyManagerFactory.algorithm");
			if (algorithm == null) {
				algorithm = "SunX509";
			}
			
			ImmutableList.Builder<APNsManager> apnsManagerListBuilder = ImmutableList.<APNsManager>builder();
			for (final APNsProtos.APNsCert cert : certList) {
				final KeyStore keyStore = KeyStore.getInstance("PKCS12");
				keyStore.load(cert.getCertP12().newInput(), cert.getCertPass().toCharArray());
				
				final KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(algorithm);
				keyManagerFactory.init(keyStore, cert.getCertPass().toCharArray());
				
				final SslContext sslContext = SslContextBuilder.forClient().keyManager(keyManagerFactory).build();
				
				apnsManagerListBuilder.add(new APNsManager(cert.getAppId(), cert.getIsProduction(), sslContext, eventLoop, 1, 
						new FeedbackListener() {
					
					@Override
					public void handleExpiredToken(int timestamp, byte[] deviceTokenBytes) {
						final String deviceToken = HexUtil.bin2Hex(deviceTokenBytes);
						
						logger.info("expired token: " + cert.getAppId() + ", " + cert.getIsProduction() + 
								", " + deviceToken + ", " + new SimpleDateFormat("yyyyMMddHHmmss").format(new Date(timestamp * 1000L)));
						
						APNsServiceImpl.this.serviceExecutor.execute(new Runnable() {

							@Override
							public void run() {
								APNsServiceImpl.this.doDeleteDeviceToken(cert.getAppId(), cert.getIsProduction(), deviceToken);
							}
							
						});
					}
					
				}));
				
				logger.info("load apns cert : " + cert.getAppId() + ", " + cert.getIsProduction());
			}
			this.apnsManagerList = apnsManagerListBuilder.build();
		} catch (Exception e) {
			throw new RuntimeException("create apns manager fail", e);
		}
		
		this.scheduledExecutorService.scheduleAtFixedRate(new Runnable() {

			@Override
			public void run() {
				for (APNsManager manager : APNsServiceImpl.this.apnsManagerList) {
					manager.tryFeedbackConnect();
				}
			}
			
		}, 30, 60 * 60, TimeUnit.SECONDS);
	}
	
	private APNsManager getAPNsManager(String appId, boolean isProduction) {
		for (APNsManager apnsManager : apnsManagerList) {
			if (apnsManager.appId().equals(appId) && apnsManager.isProduction() == isProduction) {
				return apnsManager;
			}
		}
		return null;
	}
	
	@Override
	public ListenableFuture<EmptyResponse> updateDeviceToken(RequestHead head, UpdateDeviceTokenRequest request) {
		if (!head.hasWeizhu() || !head.hasIphone() || head.getIphone().getDeviceToken().isEmpty()) {
			return ServiceUtil.EMPTY_RESPONSE_IMMEDIATE_FUTURE;
		}
		
		final long companyId = head.getSession().getCompanyId();
		
		APNsDAOProtos.APNsDeviceToken oldDeviceToken = null;
		
		Jedis jedis = jedisPool.getResource();
		try {
			List<APNsDAOProtos.APNsDeviceToken> oldDeviceTokenList = APNsCache.getDeviceToken(jedis, 
					companyId, Collections.singleton(head.getSession().getUserId()))
					.get(head.getSession().getUserId());
			
			if (oldDeviceTokenList != null) {
				for (APNsDAOProtos.APNsDeviceToken deviceToken : oldDeviceTokenList) {
					if (deviceToken.getSession().equals(head.getSession())) {
						oldDeviceToken = deviceToken;
						break;
					}
				}
			}
			
		} finally {
			jedis.close();
		}
		
		if (oldDeviceToken != null 
				&& oldDeviceToken.getAppId().equals(head.getIphone().getAppId()) 
				&& oldDeviceToken.getIsProduction() == (head.getWeizhu().getStage() == WeizhuProtos.Weizhu.Stage.RELEASE)
				&& oldDeviceToken.getDeviceToken().equals(head.getIphone().getDeviceToken())
				&& (!request.hasBadgeNumber() || oldDeviceToken.getBadgeNumber() == request.getBadgeNumber())) {
			return ServiceUtil.EMPTY_RESPONSE_IMMEDIATE_FUTURE;
		}
		
		Map<Long, List<APNsDAOProtos.APNsDeviceToken>> newDeviceTokenMap = null;
		Connection dbConn = null;
		try {
			dbConn = hikariDataSource.getConnection();

			if (oldDeviceToken == null) {
				APNsDB.replaceDeviceToken(dbConn, APNsDAOProtos.APNsDeviceToken.newBuilder()
						.setSession(head.getSession())
						.setAppId(head.getIphone().getAppId())
						.setIsProduction(head.getWeizhu().getStage() == WeizhuProtos.Weizhu.Stage.RELEASE)
						.setDeviceToken(head.getIphone().getDeviceToken())
						.setBadgeNumber(request.hasBadgeNumber() ? request.getBadgeNumber() : 0)
						.build());
			} else {
				if (!oldDeviceToken.getAppId().equals(head.getIphone().getAppId()) 
					|| oldDeviceToken.getIsProduction() != (head.getWeizhu().getStage() == WeizhuProtos.Weizhu.Stage.RELEASE)
					|| !oldDeviceToken.getDeviceToken().equals(head.getIphone().getDeviceToken())
					) {
					APNsDB.updateDeviceToken(dbConn, head.getSession(), 
							head.getIphone().getAppId(),
							head.getWeizhu().getStage() == WeizhuProtos.Weizhu.Stage.RELEASE, 
							head.getIphone().getDeviceToken());
				}
				
				if (request.hasBadgeNumber() && oldDeviceToken.getBadgeNumber() != request.getBadgeNumber()) {
					APNsDB.updateBadgeNumber(dbConn, Collections.singletonMap(head.getSession(), request.getBadgeNumber()));
				}
			}

			newDeviceTokenMap = APNsDB.getDeviceToken(dbConn, 
					companyId, Collections.singletonList(head.getSession().getUserId()));
		} catch (SQLException e) {
			throw new RuntimeException("setDeviceToken db fail", e);
		} finally {
			DBUtil.closeQuietly(dbConn);
		}
		
		jedis = jedisPool.getResource();
		try {
			APNsCache.setDeviceToken(jedis, 
					companyId, Collections.singleton(head.getSession().getUserId()), newDeviceTokenMap);
		} finally {
			jedis.close();
		}
		
		return ServiceUtil.EMPTY_RESPONSE_IMMEDIATE_FUTURE;
	}
	
	private void doDeleteDeviceToken(String appId, boolean isProduction, String deviceToken) {
		
		List<WeizhuProtos.Session> sessionList = new ArrayList<WeizhuProtos.Session>();
		
		Connection dbConn = null;
		try {
			dbConn = hikariDataSource.getConnection();
			
			List<APNsDAOProtos.APNsDeviceToken> list = APNsDB.getDeviceToken(dbConn, appId, isProduction, deviceToken);
			for (APNsDAOProtos.APNsDeviceToken d : list) {
				sessionList.add(d.getSession());
			}
			
			APNsDB.deleteDeviceToken(dbConn, sessionList);
		} catch (SQLException e) {
			throw new RuntimeException("setDeviceToken db fail", e);
		} finally {
			DBUtil.closeQuietly(dbConn);
		}
		
		if (sessionList.isEmpty()) {
			return;
		}
		
		Map<Long, Set<Long>> companyToUserIdMap = new TreeMap<Long, Set<Long>>();
		for (WeizhuProtos.Session session : sessionList) {
			Set<Long> userIdSet = companyToUserIdMap.get(session.getCompanyId());
			if (userIdSet == null) {
				userIdSet = new TreeSet<Long>();
				companyToUserIdMap.put(session.getCompanyId(), userIdSet);
			}
			userIdSet.add(session.getUserId());
		}
		
		Jedis jedis = jedisPool.getResource();
		try {
			for (Entry<Long, Set<Long>> entry : companyToUserIdMap.entrySet()) {
				APNsCache.delDeviceToken(jedis, entry.getKey(), entry.getValue());
			}
		} finally {
			jedis.close();
		}
	}
	
	@Override
	public ListenableFuture<EmptyResponse> deleteDeviceToken(AdminHead head, DeleteDeviceTokenRequest request) {
		if (!head.hasCompanyId() || (request.getUserIdCount() <= 0 && request.getSessionCount() <= 0)) {
			return ServiceUtil.EMPTY_RESPONSE_IMMEDIATE_FUTURE;
		}
		
		final long companyId = head.getCompanyId();
		final Set<Long> userIdSet = new TreeSet<Long>();
		
		userIdSet.addAll(request.getUserIdList());
		
		for (WeizhuProtos.Session session : request.getSessionList()) {
			if (session.getCompanyId() != companyId) {
				return Futures.immediateFailedFuture(new RuntimeException("invalid companyId : " + session.getCompanyId() + ", expect : " + companyId));
			}
			userIdSet.add(session.getUserId());
		}
		
		Map<Long, List<APNsDAOProtos.APNsDeviceToken>> deviceTokenMap = null;
		Connection dbConn = null;
		try {
			dbConn = hikariDataSource.getConnection();
			
			APNsDB.deleteDeviceToken(dbConn, companyId, request.getUserIdList());
			APNsDB.deleteDeviceToken(dbConn, request.getSessionList());
			
			deviceTokenMap = APNsDB.getDeviceToken(dbConn, companyId, userIdSet);
			
		} catch (SQLException e) {
			throw new RuntimeException("removeDeviceToken db fail", e);
		} finally {
			DBUtil.closeQuietly(dbConn);
		}
		
		Jedis jedis = jedisPool.getResource();
		try {
			APNsCache.setDeviceToken(jedis, companyId, userIdSet, deviceTokenMap);
		} finally {
			jedis.close();
		}
		
		return ServiceUtil.EMPTY_RESPONSE_IMMEDIATE_FUTURE;
	}
	
	@Override
	public ListenableFuture<EmptyResponse> deleteDeviceTokenExpire(RequestHead head, DeleteDeviceTokenExpireRequest request) {
		if (request.getExpireSessionIdCount() <= 0) {
			return ServiceUtil.EMPTY_RESPONSE_IMMEDIATE_FUTURE;
		}
		
		final long companyId = head.getSession().getCompanyId();
		final long userId = head.getSession().getUserId();
		
		List<WeizhuProtos.Session> sessionList = new ArrayList<WeizhuProtos.Session>(request.getExpireSessionIdCount());
		for (long sessionId : request.getExpireSessionIdList()) {
			sessionList.add(WeizhuProtos.Session.newBuilder()
					.setCompanyId(companyId)
					.setUserId(userId)
					.setSessionId(sessionId)
					.build());
		}
		
		Map<Long, List<APNsDAOProtos.APNsDeviceToken>> deviceTokenMap = null;
		Connection dbConn = null;
		try {
			dbConn = hikariDataSource.getConnection();
			
			APNsDB.deleteDeviceToken(dbConn, sessionList);
			
			deviceTokenMap = APNsDB.getDeviceToken(dbConn, companyId, Collections.singleton(userId));
			
		} catch (SQLException e) {
			throw new RuntimeException("removeDeviceToken db fail", e);
		} finally {
			DBUtil.closeQuietly(dbConn);
		}
		
		Jedis jedis = jedisPool.getResource();
		try {
			APNsCache.setDeviceToken(jedis, companyId, Collections.singleton(userId), deviceTokenMap);
		} finally {
			jedis.close();
		}
		
		return ServiceUtil.EMPTY_RESPONSE_IMMEDIATE_FUTURE;
	}

	@Override
	public ListenableFuture<EmptyResponse> deleteDeviceTokenLogout(RequestHead head, EmptyRequest request) {
		final long companyId = head.getSession().getCompanyId();
		
		Map<Long, List<APNsDAOProtos.APNsDeviceToken>> deviceTokenMap = null;
		Connection dbConn = null;
		try {
			dbConn = hikariDataSource.getConnection();
			
			APNsDB.deleteDeviceToken(dbConn, Collections.singleton(head.getSession()));
			
			deviceTokenMap = APNsDB.getDeviceToken(dbConn, companyId, Collections.singleton(head.getSession().getUserId()));
			
		} catch (SQLException e) {
			throw new RuntimeException("removeDeviceToken db fail", e);
		} finally {
			DBUtil.closeQuietly(dbConn);
		}
		
		Jedis jedis = jedisPool.getResource();
		try {
			APNsCache.setDeviceToken(jedis, companyId, Collections.singleton(head.getSession().getUserId()), deviceTokenMap);
		} finally {
			jedis.close();
		}
		
		return ServiceUtil.EMPTY_RESPONSE_IMMEDIATE_FUTURE;
	}
	
	@Override
	public ListenableFuture<EmptyResponse> sendNotification(RequestHead head, SendNotificationRequest request) {
		this.doSendNotification(head, null, null, request);
		return ServiceUtil.EMPTY_RESPONSE_IMMEDIATE_FUTURE;
	}
	
	@Override
	public ListenableFuture<EmptyResponse> sendNotification(AdminHead head, SendNotificationRequest request) {
		this.doSendNotification(null, head, null, request);
		return ServiceUtil.EMPTY_RESPONSE_IMMEDIATE_FUTURE;
	}
	
	@Override
	public ListenableFuture<EmptyResponse> sendNotification(SystemHead head, SendNotificationRequest request) {
		this.doSendNotification(null, null, head, request);
		return ServiceUtil.EMPTY_RESPONSE_IMMEDIATE_FUTURE;
	}

	private void doSendNotification(
			@Nullable RequestHead requestHead, @Nullable AdminHead adminHead, @Nullable SystemHead systemHead,
			SendNotificationRequest request
			) {
		if (requestHead == null && adminHead == null && systemHead == null) {
			throw new RuntimeException("no head");
		}
		if (request.getPushPacketCount() <= 0) {
			return ;
		}
		
		final Long companyId;
		if (requestHead != null) {
			companyId = requestHead.getSession().getCompanyId();
		} else if (adminHead != null) {
			companyId = adminHead.hasCompanyId() ? adminHead.getCompanyId() : null;
		} else if (systemHead != null) {
			companyId = systemHead.hasCompanyId() ? systemHead.getCompanyId() : null;
		} else {
			companyId = null;
		}
		
		if (companyId == null) {
			throw new RuntimeException("no companyId");
		}
		
		Set<Long> userIdSet = new TreeSet<Long>();
		for (PushProtos.PushPacket packet : request.getPushPacketList()) {
			for (PushProtos.PushTarget target : packet.getPushTargetList()) {
				userIdSet.add(target.getUserId());
			}
		}
		
		Map<Long, UserProtos.User> userMap = this.doGetUser(requestHead, adminHead, systemHead, userIdSet);
		Map<Long, SettingsProtos.Settings> settingsMap = this.doGetSettings(requestHead, adminHead, systemHead, userIdSet);
		Map<Long, List<APNsDAOProtos.APNsDeviceToken>> deviceTokenMap = this.doGetDeviceToken(companyId, userIdSet);
		
		Calendar cal = Calendar.getInstance();
		final int secondsInDay = cal.get(Calendar.SECOND) + 60 * cal.get(Calendar.MINUTE) + 60 * 60 * cal.get(Calendar.HOUR_OF_DAY);

		List<NotificationMessage> notificationMessageList = new ArrayList<NotificationMessage>(request.getPushPacketCount());
		
		// check deviceToken, user, state, settings
		for (PushProtos.PushPacket packet : request.getPushPacketList()) {
			
			// parse message
			final Message pushMessage = this.parsePushMessage(packet.getPushName(), packet.getPushBody());
			if (pushMessage == null) {
				// unknown message
				continue;
			}
			
			// find push deviceToken
			final List<APNsDAOProtos.APNsDeviceToken> pushDeviceTokenList = new ArrayList<APNsDAOProtos.APNsDeviceToken>();
			for (PushProtos.PushTarget target : packet.getPushTargetList()) {
				final long userId = target.getUserId();
				
				final UserProtos.User user = userMap.get(userId);
				if (user == null) {
					// no user
					continue;
				}
				if (user.getBase().hasState() && user.getBase().getState() == UserProtos.UserBase.State.DISABLE) {
					// user state disable
					continue;
				}
				
				final SettingsProtos.Settings settings = settingsMap.get(userId);
				if (settings != null && settings.hasDoNotDisturb()) {
					// 消息提醒时段
					SettingsProtos.Settings.DoNotDisturb doNotDisturb = settings.getDoNotDisturb();
					if (doNotDisturb.getEnable()) {
						if (doNotDisturb.getBeginTime() <= doNotDisturb.getEndTime()) {
							if (secondsInDay >= doNotDisturb.getBeginTime() && secondsInDay < doNotDisturb.getEndTime()) {
								continue;
							}
						} else {
							if (secondsInDay >= doNotDisturb.getBeginTime() || secondsInDay < doNotDisturb.getEndTime()) {
								continue;
							}
						}
					}
				}
				
				final List<APNsDAOProtos.APNsDeviceToken> deviceTokenList = deviceTokenMap.get(userId);
				if (deviceTokenList == null || deviceTokenList.isEmpty()) {
					continue;
				}
				
				for (APNsDAOProtos.APNsDeviceToken deviceToken : deviceTokenList) {
					if ((target.getIncludeSessionIdCount() <= 0 || target.getIncludeSessionIdList().contains(deviceToken.getSession().getSessionId()))
							&& !target.getExcludeSessionIdList().contains(deviceToken.getSession().getSessionId())
							&& this.getAPNsManager(deviceToken.getAppId(), deviceToken.getIsProduction()) != null
							) {
						pushDeviceTokenList.add(deviceToken);
					}
				}
			}
			
			if (!pushDeviceTokenList.isEmpty()) {
				notificationMessageList.add(new NotificationMessage(pushDeviceTokenList, pushMessage));
			}
		}
		
		// 构建push结果 (push信息 + badgeNumber)
		List<NotificationResult> resultList = buildNotificationResult(requestHead, adminHead, systemHead, notificationMessageList, userMap);
		
		Map<WeizhuProtos.Session, Integer> updateBadgeNumberMap = new HashMap<WeizhuProtos.Session, Integer>();
		
		for (NotificationResult result : resultList) {
			APNsManager apnsManager = this.getAPNsManager(result.deviceToken.getAppId(), result.deviceToken.getIsProduction());
			if (apnsManager != null) {
				apnsManager.sendNotification(result.pushNotification);
				
				if (result.badgeNumber != result.deviceToken.getBadgeNumber()) {
					updateBadgeNumberMap.put(result.deviceToken.getSession(), result.badgeNumber);
				}
			}
		}
		
		if (!updateBadgeNumberMap.isEmpty()) {
			Set<Long> updateUserIdSet = new TreeSet<Long>();
			for (WeizhuProtos.Session session : updateBadgeNumberMap.keySet()) {
				updateUserIdSet.add(session.getUserId());
			}
			
			Map<Long, List<APNsDAOProtos.APNsDeviceToken>> updateDeviceTokenMap;
			Connection dbConn = null;
			try {
				dbConn = hikariDataSource.getConnection();
				APNsDB.updateBadgeNumber(dbConn, updateBadgeNumberMap);
				
				updateDeviceTokenMap = APNsDB.getDeviceToken(dbConn, companyId, updateUserIdSet);
			} catch (SQLException e) {
				throw new RuntimeException("db fail", e);
			} finally {
				DBUtil.closeQuietly(dbConn);
			}
			
			Jedis jedis = jedisPool.getResource();
			try {
				APNsCache.setDeviceToken(jedis, companyId, updateUserIdSet, updateDeviceTokenMap);
			} finally {
				jedis.close();
			}
		}
	}
	
	private Map<Long, List<APNsDAOProtos.APNsDeviceToken>> doGetDeviceToken(long companyId, Collection<Long> userIds) {
		if (userIds.isEmpty()) {
			return Collections.emptyMap();
		}
		
		Map<Long, List<APNsDAOProtos.APNsDeviceToken>> resultMap = new HashMap<Long, List<APNsDAOProtos.APNsDeviceToken>>();
		Set<Long> noCacheUserIdSet = new TreeSet<Long>();
		Jedis jedis = jedisPool.getResource();
		try {
			resultMap.putAll(APNsCache.getDeviceToken(jedis, companyId, userIds, noCacheUserIdSet));
		} finally {
			jedis.close();
		}
		
		if (noCacheUserIdSet.isEmpty()) {
			return resultMap;
		}
		
		Map<Long, List<APNsDAOProtos.APNsDeviceToken>> noCacheDeviceTokenMap;
		Connection dbConn = null;
		try {
			dbConn = hikariDataSource.getConnection();
			noCacheDeviceTokenMap = APNsDB.getDeviceToken(dbConn, companyId, noCacheUserIdSet);
		} catch (SQLException e) {
			throw new RuntimeException("db fail", e);
		} finally {
			DBUtil.closeQuietly(dbConn);
		}
		
		jedis = jedisPool.getResource();
		try {
			APNsCache.setDeviceToken(jedis, companyId, noCacheUserIdSet, noCacheDeviceTokenMap);
		} finally {
			jedis.close();
		}
		
		resultMap.putAll(noCacheDeviceTokenMap);
		return resultMap;
	}
	
	private Map<Long, UserProtos.User> doGetUser(@Nullable RequestHead requestHead, @Nullable AdminHead adminHead, @Nullable SystemHead systemHead, Collection<Long> userIds) {
		if (userIds.isEmpty()) {
			return Collections.emptyMap();
		}
		
		if (requestHead != null) {
			UserProtos.GetUserResponse response = Futures.getUnchecked(
					this.userServiceProvider.get().getUserById(requestHead, 
							UserProtos.GetUserByIdRequest.newBuilder()
							.addAllUserId(userIds)
							.build()));
			
			if (response.getUserCount() <= 0) {
				return Collections.emptyMap();
			}
			
			Map<Long, UserProtos.User> userMap = new TreeMap<Long, UserProtos.User>();
			for (UserProtos.User user : response.getUserList()) {
				userMap.put(user.getBase().getUserId(), user);
			}
			return userMap;
		}
		if (adminHead != null) {
			AdminUserProtos.GetUserByIdResponse response = Futures.getUnchecked(
					this.adminUserServiceProvider.get().getUserById(adminHead, AdminUserProtos.GetUserByIdRequest.newBuilder()
							.addAllUserId(userIds)
							.build()));
			
			if (response.getUserCount() <= 0) {
				return Collections.emptyMap();
			}
			
			Map<Long, UserProtos.User> userMap = new TreeMap<Long, UserProtos.User>();
			for (UserProtos.User user : response.getUserList()) {
				userMap.put(user.getBase().getUserId(), user);
			}
			return userMap;
		}
		if (systemHead != null) {
			AdminUserProtos.GetUserByIdResponse response = Futures.getUnchecked(
					this.adminUserServiceProvider.get().getUserById(systemHead, AdminUserProtos.GetUserByIdRequest.newBuilder()
							.addAllUserId(userIds)
							.build()));
			
			if (response.getUserCount() <= 0) {
				return Collections.emptyMap();
			}
			
			Map<Long, UserProtos.User> userMap = new TreeMap<Long, UserProtos.User>();
			for (UserProtos.User user : response.getUserList()) {
				userMap.put(user.getBase().getUserId(), user);
			}
			return userMap;
		}
		
		throw new RuntimeException("no head found!");
	}
	
	private Map<Long, SettingsProtos.Settings> doGetSettings(@Nullable RequestHead requestHead, @Nullable AdminHead adminHead, @Nullable SystemHead systemHead, Collection<Long> userIds) {
		if (userIds.isEmpty()) {
			return Collections.emptyMap();
		}
		
		if (requestHead != null) {
			SettingsProtos.GetUserSettingsResponse response = Futures.getUnchecked(
					this.settingServiceProvider.get().getUserSettings(requestHead, 
							SettingsProtos.GetUserSettingsRequest.newBuilder()
							.addAllUserId(userIds)
							.build()));
			if (response.getSettingsCount() <= 0) {
				return Collections.emptyMap();
			}
			
			Map<Long, SettingsProtos.Settings> settingsMap = new TreeMap<Long, SettingsProtos.Settings>();
			for (SettingsProtos.Settings settings : response.getSettingsList()) {
				settingsMap.put(settings.getUserId(), settings);
			}
			return settingsMap;
		}
		if (adminHead != null) {
			SettingsProtos.GetUserSettingsResponse response = Futures.getUnchecked(
					this.settingServiceProvider.get().getUserSettings(adminHead, 
							SettingsProtos.GetUserSettingsRequest.newBuilder()
							.addAllUserId(userIds)
							.build()));
			if (response.getSettingsCount() <= 0) {
				return Collections.emptyMap();
			}
			
			Map<Long, SettingsProtos.Settings> settingsMap = new TreeMap<Long, SettingsProtos.Settings>();
			for (SettingsProtos.Settings settings : response.getSettingsList()) {
				settingsMap.put(settings.getUserId(), settings);
			}
			return settingsMap;
		}
		if (systemHead != null) {
			SettingsProtos.GetUserSettingsResponse response = Futures.getUnchecked(
					this.settingServiceProvider.get().getUserSettings(systemHead, 
							SettingsProtos.GetUserSettingsRequest.newBuilder()
							.addAllUserId(userIds)
							.build()));
			if (response.getSettingsCount() <= 0) {
				return Collections.emptyMap();
			}
			
			Map<Long, SettingsProtos.Settings> settingsMap = new TreeMap<Long, SettingsProtos.Settings>();
			for (SettingsProtos.Settings settings : response.getSettingsList()) {
				settingsMap.put(settings.getUserId(), settings);
			}
			return settingsMap;
		}
		
		throw new RuntimeException("no head found!");
	}
	
	private Map<Long, OfficialProtos.Official> doGetOfficial(@Nullable RequestHead requestHead, @Nullable AdminHead adminHead, @Nullable SystemHead systemHead, Collection<Long> officialIds) {
		if (officialIds.isEmpty()) {
			return Collections.emptyMap();
		}
		
		if (requestHead != null) {
			OfficialProtos.GetOfficialByIdResponse response = Futures.getUnchecked(
					this.officialServiceProvider.get().getOfficialById(requestHead, 
							OfficialProtos.GetOfficialByIdRequest.newBuilder()
							.addAllOfficialId(officialIds)
							.build()));
			
			if (response.getOfficialCount() <= 0) {
				return Collections.emptyMap();
			}
			
			Map<Long, OfficialProtos.Official> officialMap = new TreeMap<Long, OfficialProtos.Official>();
			for (OfficialProtos.Official official : response.getOfficialList()) {
				officialMap.put(official.getOfficialId(), official);
			}
			return officialMap;
		}
		if (adminHead != null) {
			AdminOfficialProtos.GetOfficialByIdResponse response = Futures.getUnchecked(
					this.adminOfficialServiceProvider.get().getOfficialById(adminHead, 
							AdminOfficialProtos.GetOfficialByIdRequest.newBuilder()
							.addAllOfficialId(officialIds)
							.build()));
			if (response.getOfficialCount() <= 0) {
				return Collections.emptyMap();
			}
			
			Map<Long, OfficialProtos.Official> officialMap = new TreeMap<Long, OfficialProtos.Official>();
			for (OfficialProtos.Official official : response.getOfficialList()) {
				officialMap.put(official.getOfficialId(), official);
			}
			return officialMap;
		}
		if (systemHead != null) {
			AdminOfficialProtos.GetOfficialByIdResponse response = Futures.getUnchecked(
					this.adminOfficialServiceProvider.get().getOfficialById(systemHead, 
							AdminOfficialProtos.GetOfficialByIdRequest.newBuilder()
							.addAllOfficialId(officialIds)
							.build()));
			if (response.getOfficialCount() <= 0) {
				return Collections.emptyMap();
			}
			
			Map<Long, OfficialProtos.Official> officialMap = new TreeMap<Long, OfficialProtos.Official>();
			for (OfficialProtos.Official official : response.getOfficialList()) {
				officialMap.put(official.getOfficialId(), official);
			}
			return officialMap;
		}
		
		throw new RuntimeException("no head found!");
	}
	
	private Message parsePushMessage(String pushName, ByteString pushBody) {
		try {
			if ("IMP2PMessagePush".equals(pushName)) {
				return IMProtos.IMP2PMessagePush.parseFrom(pushBody);
			} else if ("IMGroupStatePush".equals(pushName)) {
				return IMProtos.IMGroupStatePush.parseFrom(pushBody);
			} else if ("OfficialMessagePush".equals(pushName)) {
				return OfficialProtos.OfficialMessagePush.parseFrom(pushBody);
			} else if ("CommunityPostMessagePush".equals(pushName)) {
				return CommunityProtos.CommunityPostMessagePush.parseFrom(pushBody);
			} else if ("CommunityCommentMessagePush".equals(pushName)) {
				return CommunityProtos.CommunityCommentMessagePush.parseFrom(pushBody);
			}
		} catch (InvalidProtocolBufferException e) {
			logger.error("parse push msg fail : " + pushName, e);
		}
		return null;
	}
	
	private static final Gson GSON = new Gson();
	
	private List<NotificationResult> buildNotificationResult (
			@Nullable RequestHead requestHead, @Nullable AdminHead adminHead, @Nullable SystemHead systemHead,
			List<NotificationMessage> notificationMessageList,
			Map<Long, UserProtos.User> refUserMap
			) {
		if (notificationMessageList.isEmpty()) {
			return Collections.emptyList();
		}
		
		// 1. 获取引用的相关信息.比如 用户，服务号 等等信息
		Set<Long> refUserIdSet = new TreeSet<Long>();
		Set<Long> refOfficialIdSet = new TreeSet<Long>();
		for (NotificationMessage message : notificationMessageList) {
			if (message.pushMessage instanceof IMProtos.IMP2PMessagePush) {
				final IMProtos.IMP2PMessagePush p2pMsgPush = (IMProtos.IMP2PMessagePush) message.pushMessage;
				refUserIdSet.add(p2pMsgPush.getMsg().getFromUserId());
			} else if (message.pushMessage instanceof OfficialProtos.OfficialMessagePush) {
				final OfficialProtos.OfficialMessagePush officialMsgPush = (OfficialProtos.OfficialMessagePush) message.pushMessage;
				refOfficialIdSet.add(officialMsgPush.getOfficialId());
			}
		}
		refUserIdSet.removeAll(refUserMap.keySet());
		
		Map<Long, UserProtos.User> newRefUserMap = new HashMap<Long, UserProtos.User>(refUserMap);
		newRefUserMap.putAll(this.doGetUser(requestHead, adminHead, systemHead, refUserIdSet));
		
		Map<Long, OfficialProtos.Official> refOfficialMap = this.doGetOfficial(requestHead, adminHead, systemHead, refOfficialIdSet);
		
		// 2. 构建result List
		List<NotificationResult> allResultList = new ArrayList<NotificationResult>();
		for (NotificationMessage message : notificationMessageList) {
			List<NotificationResult> resultList = null;
			if (message.pushMessage instanceof IMProtos.IMP2PMessagePush) {
				resultList = buildIMP2PMessagePush(message.deviceTokenList, (IMProtos.IMP2PMessagePush)message.pushMessage, newRefUserMap);
			} else if (message.pushMessage instanceof IMProtos.IMGroupStatePush) {
				resultList = buildIMGroupStatePush(message.deviceTokenList, (IMProtos.IMGroupStatePush)message.pushMessage);
			} else if (message.pushMessage instanceof OfficialProtos.OfficialMessagePush) {
				resultList = buildOfficialMessagePush(message.deviceTokenList, (OfficialProtos.OfficialMessagePush)message.pushMessage, refOfficialMap);
			} else if (message.pushMessage instanceof CommunityProtos.CommunityPostMessagePush) {
				resultList = buildCommunityPostMessagePush(message.deviceTokenList, (CommunityProtos.CommunityPostMessagePush)message.pushMessage);
			} else if (message.pushMessage instanceof CommunityProtos.CommunityCommentMessagePush) {
				resultList = buildCommunityCommentMessagePush(message.deviceTokenList, (CommunityProtos.CommunityCommentMessagePush)message.pushMessage);
			}
			
			if (resultList != null && !resultList.isEmpty()) {
				allResultList.addAll(resultList);
			}
		}
		return allResultList;
	}
	
	private List<NotificationResult> buildIMP2PMessagePush(
			List<APNsDAOProtos.APNsDeviceToken> deviceTokenList, IMProtos.IMP2PMessagePush p2pMsgPush,
			Map<Long, UserProtos.User> refUserMap
			) {

		boolean hasOther = false;
		for (APNsDAOProtos.APNsDeviceToken deviceToken : deviceTokenList) {
			if (deviceToken.getSession().getUserId() != p2pMsgPush.getMsg().getFromUserId()) {
				hasOther = true;
				break;
			}
		}
		
		// fail fast: 点对点私聊不发给自己apns推送
		if (!hasOther) {
			return null;
		}
		
		String msgContent = null;
		switch (p2pMsgPush.getMsg().getMsgTypeCase()) {
			case TEXT:
				msgContent = p2pMsgPush.getMsg().getText().getContent();
				break;
			case VOICE:
				msgContent = "发送了一段语音";
				break;
			case IMAGE:
				msgContent = "发送了一张图片";
				break;
			case USER:
				msgContent = "发送了一张名片";
				break;
			case VIDEO:
				msgContent = "发送了一个视频";
				break;
			case FILE:
				msgContent = "发送了一个文件";
				break;
			case GROUP:
				break;
			case DISCOVER_ITEM:
				msgContent = "分享了一个课程";
				break;
			default:
				break;
		}
		
		// 该内容不需要发送apns推送
		if (msgContent == null) {
			return null;
		}
		
		UserProtos.User msgFromUser = refUserMap.get(p2pMsgPush.getMsg().getFromUserId());
		if (msgFromUser == null) {
			return null;
		}
		
		String alertContent = new StringBuilder().append(msgFromUser.getBase().getUserName()).append(": ").append(msgContent).toString();
		if (alertContent.length() > 64) {
			alertContent = alertContent.substring(0, 61) + "...";
		}
		
		List<NotificationResult> resultList = new ArrayList<NotificationResult>(deviceTokenList.size());
		for (APNsDAOProtos.APNsDeviceToken deviceToken : deviceTokenList) {
			if (deviceToken.getSession().getUserId() == p2pMsgPush.getMsg().getFromUserId()) {
				continue;
			}
			
			JsonObject aps = new JsonObject();
			aps.addProperty("badge", deviceToken.getBadgeNumber() + 1);
			aps.addProperty("alert", alertContent);
			aps.addProperty("content-available", 1);
			aps.addProperty("sound", "default");
			
			JsonObject weizhu = new JsonObject();
			weizhu.addProperty("push_name", "IMP2PMessagePush");
			weizhu.addProperty("user_id", p2pMsgPush.getUserId());
			
			JsonObject payload = new JsonObject();
			payload.add("aps", aps);
			payload.add("weizhu", weizhu);
			
			PushNotification notification = 
					new PushNotification(
							HexUtil.hex2bin(deviceToken.getDeviceToken()), 
							GSON.toJson(payload).getBytes(Charsets.UTF_8), 
							0, (byte)10);
			
			if (notification.checkValid()) {
				resultList.add(new NotificationResult(deviceToken, notification, deviceToken.getBadgeNumber() + 1));
			} else {
				logger.error("invalid notification : " + notification);
			}
		}
		
		return resultList;
	}
	
	private List<NotificationResult> buildIMGroupStatePush(
			List<APNsDAOProtos.APNsDeviceToken> deviceTokenList, IMProtos.IMGroupStatePush groupStatePush
			) {
		
		String msgContent = "您收到了一条群消息";
		
		List<NotificationResult> resultList = new ArrayList<NotificationResult>(deviceTokenList.size());
		for (APNsDAOProtos.APNsDeviceToken deviceToken : deviceTokenList) {
			
			JsonObject aps = new JsonObject();
			aps.addProperty("badge", deviceToken.getBadgeNumber());
			aps.addProperty("alert", msgContent);
			aps.addProperty("content-available", 1);
			
			JsonObject weizhu = new JsonObject();
			weizhu.addProperty("push_name", "IMGroupStatePush");
			weizhu.addProperty("group_id", groupStatePush.getGroupId());
			
			JsonObject payload = new JsonObject();
			payload.add("aps", aps);
			payload.add("weizhu", weizhu);
			
			PushNotification notification = 
					new PushNotification(
							HexUtil.hex2bin(deviceToken.getDeviceToken()), 
							GSON.toJson(payload).getBytes(Charsets.UTF_8), 
							0, (byte)10);
			
			if (notification.checkValid()) {
				resultList.add(new NotificationResult(deviceToken, notification, deviceToken.getBadgeNumber()));
			} else {
				logger.error("invalid notification : " + notification);
			}
		}
		
		return resultList;
	}
	
	private List<NotificationResult> buildOfficialMessagePush(
			List<APNsDAOProtos.APNsDeviceToken> deviceTokenList, OfficialProtos.OfficialMessagePush officialMessagePush,
			Map<Long, OfficialProtos.Official> refOfficialMap
			) {
		
		if (officialMessagePush.getMsg().getIsFromUser()) {
			// 自己发的信息 不需要apns推送
			return null;
		}
		
		String msgContent = null;
		switch (officialMessagePush.getMsg().getMsgTypeCase()) {
			case TEXT:
				msgContent = officialMessagePush.getMsg().getText().getContent();
				break;
			case VOICE:
				msgContent = "发送了一段语音";
				break;
			case IMAGE:
				msgContent = "发送了一张图片";
				break;
			case USER:
				msgContent = "发送了一张名片";
				break;
			case VIDEO:
				msgContent = "发送了一个视频";
				break;
			case FILE:
				msgContent = "发送了一个文件";
				break;
			case DISCOVER_ITEM:
				msgContent = "分享了一个课程";
				break;
			default:
				break;
		}
		
		// 该内容不需要发送apns推送
		if (msgContent == null) {
			return null;
		}
		
		OfficialProtos.Official official = refOfficialMap.get(officialMessagePush.getOfficialId());
		if (official == null) {
			return null;
		}
		
		String alertContent = new StringBuilder().append(official.getOfficialName()).append(": ").append(msgContent).toString();
		if (alertContent.length() > 64) {
			alertContent = alertContent.substring(0, 61) + "...";
		}
		
		List<NotificationResult> resultList = new ArrayList<NotificationResult>(deviceTokenList.size());
		for (APNsDAOProtos.APNsDeviceToken deviceToken : deviceTokenList) {
			
			JsonObject aps = new JsonObject();
			aps.addProperty("badge", deviceToken.getBadgeNumber() + 1);
			aps.addProperty("alert", alertContent);
			aps.addProperty("content-available", 1);
			aps.addProperty("sound", "default");
			
			JsonObject weizhu = new JsonObject();
			weizhu.addProperty("push_name", "OfficialMessagePush");
			weizhu.addProperty("official_id", officialMessagePush.getOfficialId());
			
			JsonObject payload = new JsonObject();
			payload.add("aps", aps);
			payload.add("weizhu", weizhu);
			
			PushNotification notification = 
					new PushNotification(
							HexUtil.hex2bin(deviceToken.getDeviceToken()), 
							GSON.toJson(payload).getBytes(Charsets.UTF_8), 
							0, (byte)10);
			
			if (notification.checkValid()) {
				resultList.add(new NotificationResult(deviceToken, notification, deviceToken.getBadgeNumber() + 1));
			} else {
				logger.error("invalid notification : " + notification);
			}
		}
		
		return resultList;
	}
	
	private List<NotificationResult> buildCommunityPostMessagePush(
			List<APNsDAOProtos.APNsDeviceToken> deviceTokenList, CommunityProtos.CommunityPostMessagePush postMsgPush
			) {

		List<NotificationResult> resultList = new ArrayList<NotificationResult>(deviceTokenList.size());
		for (APNsDAOProtos.APNsDeviceToken deviceToken : deviceTokenList) {

			JsonObject aps = new JsonObject();
			aps.addProperty("badge", deviceToken.getBadgeNumber() + 1);
			aps.addProperty("alert", "您在社区发表的帖子收到了新评论");
			aps.addProperty("content-available", 1);
			aps.addProperty("sound", "default");
			
			JsonObject weizhu = new JsonObject();
			weizhu.addProperty("push_name", "CommunityPostMessagePush");
			weizhu.addProperty("post_id", postMsgPush.getPostId());
			weizhu.addProperty("comment_id", postMsgPush.getCommentId());
			
			JsonObject payload = new JsonObject();
			payload.add("aps", aps);
			payload.add("weizhu", weizhu);
			
			PushNotification notification = 
					new PushNotification(
							HexUtil.hex2bin(deviceToken.getDeviceToken()), 
							GSON.toJson(payload).getBytes(Charsets.UTF_8), 
							0, (byte)10);
			
			if (notification.checkValid()) {
				resultList.add(new NotificationResult(deviceToken, notification, deviceToken.getBadgeNumber() + 1));
			} else {
				logger.error("invalid notification : " + notification);
			}
		}
		
		return resultList;
	}
	
	private List<NotificationResult> buildCommunityCommentMessagePush(
			List<APNsDAOProtos.APNsDeviceToken> deviceTokenList, CommunityProtos.CommunityCommentMessagePush commentMsgPush
			) {

		List<NotificationResult> resultList = new ArrayList<NotificationResult>(deviceTokenList.size());
		for (APNsDAOProtos.APNsDeviceToken deviceToken : deviceTokenList) {

			JsonObject aps = new JsonObject();
			aps.addProperty("badge", deviceToken.getBadgeNumber() + 1);
			aps.addProperty("alert", "您在社区发表的评论收到了新回复");
			aps.addProperty("content-available", 1);
			aps.addProperty("sound", "default");
			
			JsonObject weizhu = new JsonObject();
			weizhu.addProperty("push_name", "CommunityCommentMessagePush");
			weizhu.addProperty("post_id", commentMsgPush.getPostId());
			weizhu.addProperty("comment_id", commentMsgPush.getCommentId());
			weizhu.addProperty("reply_comment_id", commentMsgPush.getReplyCommentId());
			
			JsonObject payload = new JsonObject();
			payload.add("aps", aps);
			payload.add("weizhu", weizhu);
			
			PushNotification notification = 
					new PushNotification(
							HexUtil.hex2bin(deviceToken.getDeviceToken()), 
							GSON.toJson(payload).getBytes(Charsets.UTF_8), 
							0, (byte)10);
			
			if (notification.checkValid()) {
				resultList.add(new NotificationResult(deviceToken, notification, deviceToken.getBadgeNumber() + 1));
			} else {
				logger.error("invalid notification : " + notification);
			}
		}
		
		return resultList;
	}
	
	private static final class NotificationMessage {
		final List<APNsDAOProtos.APNsDeviceToken> deviceTokenList;
		final Message pushMessage;
		
		NotificationMessage(List<APNsDAOProtos.APNsDeviceToken> deviceTokenList, Message pushMessage) {
			this.deviceTokenList = deviceTokenList;
			this.pushMessage = pushMessage;
		}
	}
	
	private static final class NotificationResult {
		final APNsDAOProtos.APNsDeviceToken deviceToken;
		final PushNotification pushNotification;
		final int badgeNumber;
		
		NotificationResult(APNsDAOProtos.APNsDeviceToken deviceToken, PushNotification pushNotification, int badgeNumber) {
			this.deviceToken = deviceToken;
			this.pushNotification = pushNotification;
			this.badgeNumber = badgeNumber;
		}
	}
	
}
