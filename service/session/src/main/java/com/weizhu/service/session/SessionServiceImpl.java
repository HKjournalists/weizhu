package com.weizhu.service.session;

import java.nio.ByteBuffer;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.Executor;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import com.google.common.base.Function;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.google.protobuf.ByteString;
import com.weizhu.common.db.DBUtil;
import com.weizhu.common.service.ServiceUtil;
import com.weizhu.common.utils.HexUtil;
import com.weizhu.proto.APNsService;
import com.weizhu.proto.APNsProtos.UpdateDeviceTokenRequest;
import com.weizhu.proto.AdminProtos.AdminHead;
import com.weizhu.proto.PushService;
import com.weizhu.proto.SessionProtos;
import com.weizhu.proto.PushProtos.PushUserDeleteRequest;
import com.weizhu.proto.PushProtos.PushUserExpireRequest;
import com.weizhu.proto.SessionProtos.CreateSessionKeyRequest;
import com.weizhu.proto.SessionProtos.CreateSessionKeyResponse;
import com.weizhu.proto.SessionProtos.CreateWebLoginSessionKeyResponse;
import com.weizhu.proto.SessionProtos.DeleteSessionDataRequest;
import com.weizhu.proto.SessionProtos.DeleteSessionDataResponse;
import com.weizhu.proto.SessionProtos.GetSessionDataRequest;
import com.weizhu.proto.SessionProtos.GetSessionDataResponse;
import com.weizhu.proto.SessionProtos.VerifySessionKeyRequest;
import com.weizhu.proto.SessionProtos.VerifySessionKeyResponse;
import com.weizhu.proto.SessionProtos.VerifyWebLoginSessionKeyRequest;
import com.weizhu.proto.SessionProtos.VerifyWebLoginSessionKeyResponse;
import com.weizhu.proto.UserProtos;
import com.weizhu.proto.UserProtos.GetUserBaseByIdRequest;
import com.weizhu.proto.UserProtos.GetUserBaseByIdResponse;
import com.weizhu.proto.UserService;
import com.weizhu.proto.WeizhuProtos;
import com.weizhu.proto.WeizhuProtos.AnonymousHead;
import com.weizhu.proto.SessionService;
import com.weizhu.proto.WeizhuProtos.EmptyRequest;
import com.weizhu.proto.WeizhuProtos.EmptyResponse;
import com.weizhu.proto.WeizhuProtos.RequestHead;
import com.zaxxer.hikari.HikariDataSource;

public class SessionServiceImpl implements SessionService {

	private static final int USER_SESSION_MAX_NUM = 10;
	private static final String ENCRYPTION_ALGORITHM = "DESede";

	private static final Logger logger = LoggerFactory.getLogger(SessionServiceImpl.class);

	private final Executor serviceExecutor;
	private final HikariDataSource hikariDataSource;
	private final JedisPool jedisPool;
	private final UserService userService;
	private final APNsService apnsService;
	private final PushService pushService;
	private final SecretKey secretKey;

	@Inject
	public SessionServiceImpl(@Named("service_executor") Executor serviceExecutor,
			HikariDataSource hikariDataSource, JedisPool jedisPool,
			UserService userService, APNsService apnsService, PushService pushService,
			@Named("session_secret_key") byte[] secretKey) {
		this.serviceExecutor = serviceExecutor;
		this.hikariDataSource = hikariDataSource;
		this.jedisPool = jedisPool;
		this.userService = userService;
		this.apnsService = apnsService;
		this.pushService = pushService;
		this.secretKey = new SecretKeySpec(secretKey, ENCRYPTION_ALGORITHM);
	}

	private final Random rand = new Random();
	
	private Map<Long, List<SessionProtos.SessionData>> doGetSessionData(long companyId, Collection<Long> userIds) {
		if (userIds.isEmpty()) {
			return Collections.emptyMap();
		}
		
		Map<Long, List<SessionProtos.SessionData>> resultMap = new HashMap<Long, List<SessionProtos.SessionData>>(userIds.size());
		
		Set<Long> noCacheUserIdSet = new TreeSet<Long>();
		Jedis jedis = jedisPool.getResource();
		try {
			resultMap.putAll(SessionCache.getSessionData(jedis, companyId, userIds, noCacheUserIdSet));
		} finally {
			jedis.close();
		}
		
		if (noCacheUserIdSet.isEmpty()) {
			return resultMap;
		}
		
		Map<Long, List<SessionProtos.SessionData>> noCacheSessionDataMap;
		Connection dbConn = null;
		try {
			dbConn = hikariDataSource.getConnection();
			noCacheSessionDataMap = SessionDB.getSessionData(dbConn, companyId, noCacheUserIdSet);
		} catch (SQLException e) {
			throw new RuntimeException("db fail", e);
		} finally {
			DBUtil.closeQuietly(dbConn);
		}
		
		jedis = jedisPool.getResource();
		try {
			SessionCache.setSessionData(jedis, companyId, noCacheUserIdSet, noCacheSessionDataMap);
		} finally {
			jedis.close();
		}
		
		resultMap.putAll(noCacheSessionDataMap);
		
		return resultMap;
	}

	@Override
	public ListenableFuture<CreateSessionKeyResponse> createSessionKey(AnonymousHead head, CreateSessionKeyRequest request) {
		
		final long companyId = request.getCompanyId();
		final long userId = request.getUserId();
		final int now = (int) (System.currentTimeMillis() / 1000L);
		
		List<Long> deleteSessionIdList;
		List<SessionProtos.SessionData> sessionDataList;
		
		SessionProtos.SessionData sessionData = null;
		Connection dbConn = null;
		try {
			dbConn = hikariDataSource.getConnection();

			final int retry = 3;
			boolean succ = false;
			for (int i = 0; i < retry && !succ; ++i) {
				long sessionId = rand.nextLong();
				
				SessionProtos.SessionData.Builder sessionDataBuilder = SessionProtos.SessionData.newBuilder();
				sessionDataBuilder.setSession(WeizhuProtos.Session.newBuilder()
						.setCompanyId(companyId)
						.setUserId(userId)
						.setSessionId(sessionId)
						.build());
				sessionDataBuilder.setLoginTime(now);
				sessionDataBuilder.setActiveTime(now);
				sessionDataBuilder.setWeizhu(head.getWeizhu());
				if (head.hasAndroid()) {
					sessionDataBuilder.setAndroid(head.getAndroid());
				}
				if (head.hasIphone()) {
					sessionDataBuilder.setIphone(head.getIphone());
				}
				sessionData = sessionDataBuilder.build();
				
				succ = SessionDB.insertSession(dbConn, sessionData);
			}

			if (!succ) {
				logger.error("create session key conflict!");
				throw new RuntimeException("Cannot create session id!");
			}
			
			List<SessionProtos.SessionData> list = SessionDB.getSessionData(dbConn, companyId, Collections.<Long>singleton(userId)).get(userId);
			if (list == null) {
				throw new RuntimeException("Cannot find create session list!");
			}
			
			deleteSessionIdList = new ArrayList<Long>();
			sessionDataList = new ArrayList<SessionProtos.SessionData>();
			for (SessionProtos.SessionData data : list) {
				if (data.getSession().getCompanyId() != companyId || data.getSession().getUserId() != userId) {
					continue;
				}
				
				if (sessionDataList.size() >= USER_SESSION_MAX_NUM || (data.getSession().getSessionId() != sessionData.getSession().getSessionId() && ( 
						// android 串号相同 说明为同一手机登录
						(sessionData.hasAndroid() && data.hasAndroid() 
								&& !sessionData.getAndroid().getSerial().trim().isEmpty()
								&& !"unknown".equals(sessionData.getAndroid().getSerial())
								&& sessionData.getAndroid().getSerial().equals(data.getAndroid().getSerial()) ) 
						|| // iphone mac 地址相同 说明为同一手机登录
						(sessionData.hasIphone() && data.hasIphone()
								&& !sessionData.getIphone().getMac().trim().isEmpty()
								&& sessionData.getIphone().getMac().equals(data.getIphone().getMac())) )
					)) {
					deleteSessionIdList.add(data.getSession().getSessionId());
				} else {
					sessionDataList.add(data);
				}
			}
			
			SessionDB.deleteSession(dbConn, companyId, userId, deleteSessionIdList);
		} catch (SQLException e) {
			throw new RuntimeException("createSessionKey db fail", e);
		} finally {
			DBUtil.closeQuietly(dbConn);
		}

		Jedis jedis = jedisPool.getResource();
		try {
			SessionCache.setSessionData(jedis, companyId, Collections.<Long, List<SessionProtos.SessionData>> singletonMap(userId, sessionDataList));
		} finally {
			jedis.close();
		}
		
		if (!deleteSessionIdList.isEmpty()) {
			pushService.pushUserExpire(ServiceUtil.toRequestHead(head, sessionData.getSession()), 
					PushUserExpireRequest.newBuilder().addAllExpireSessionId(deleteSessionIdList).build());
		}
		
		if (head.hasIphone() && head.getIphone().hasDeviceToken() && !head.getIphone().getDeviceToken().isEmpty()) {
			apnsService.updateDeviceToken(ServiceUtil.toRequestHead(head, sessionData.getSession()), UpdateDeviceTokenRequest.newBuilder().build());
		}

		ByteBuffer content = ByteBuffer.allocate(28);
		content.putLong(sessionData.getSession().getCompanyId());
		content.putLong(sessionData.getSession().getUserId());
		content.putLong(sessionData.getSession().getSessionId());
		content.putInt(sessionData.getLoginTime());

		byte[] sessionKey = encrypt(content.array());

		logger.info("CreateSession:" + 
				sessionData.getSession().getCompanyId() + "," + 
				sessionData.getSession().getUserId() + "," + 
				sessionData.getSession().getSessionId() + ": " +
				HexUtil.bin2Hex(sessionKey));
		
		return Futures.immediateFuture(CreateSessionKeyResponse.newBuilder()
				.setSessionKey(ByteString.copyFrom(sessionKey))
				.setSession(sessionData.getSession())
				.build());
	}
	
	@Override
	public ListenableFuture<EmptyResponse> deleteSessionKey(RequestHead head, EmptyRequest request) {
		final long companyId = head.getSession().getCompanyId();
		final long userId = head.getSession().getUserId();
		
		Map<Long, List<SessionProtos.SessionData>> sessionDataMap;
		
		Connection dbConn = null;
		try {
			dbConn = hikariDataSource.getConnection();
			SessionDB.deleteSession(dbConn, companyId, userId, Collections.singleton(head.getSession().getSessionId()));
			sessionDataMap = SessionDB.getSessionData(dbConn, companyId, Collections.singleton(userId));
		} catch (SQLException e) {
			throw new RuntimeException("deleteSession db fail", e);
		} finally {
			DBUtil.closeQuietly(dbConn);
		}
		
		Jedis jedis = jedisPool.getResource();
		try {
			SessionCache.setSessionData(jedis, companyId, Collections.<Long> singleton(userId), sessionDataMap);
		} finally {
			jedis.close();
		}
		
		pushService.pushUserLogout(head, ServiceUtil.EMPTY_REQUEST);
		
		return ServiceUtil.EMPTY_RESPONSE_IMMEDIATE_FUTURE;
	}

	@Override
	public ListenableFuture<VerifySessionKeyResponse> verifySessionKey(AnonymousHead head, VerifySessionKeyRequest request) {
		byte[] bytes = decrypt(request.getSessionKey().toByteArray());
		if (bytes == null || bytes.length != 28) {
			return Futures.immediateFuture(VerifySessionKeyResponse.newBuilder()
					.setResult(VerifySessionKeyResponse.Result.FAIL_SESSION_DECRYPTION)
					.setFailText("身份key解码失败")
					.build());
		}

		ByteBuffer content = ByteBuffer.wrap(bytes);

		final WeizhuProtos.Session session = WeizhuProtos.Session.newBuilder()
				.setCompanyId(content.getLong())
				.setUserId(content.getLong())
				.setSessionId(content.getLong())
				.build();
		final int loginTime = content.getInt();

		SessionProtos.SessionData sessionData = null;
		
		List<SessionProtos.SessionData> sessionDataList = this.doGetSessionData(session.getCompanyId(), 
				Collections.singleton(session.getUserId())).get(session.getUserId());
		if (sessionDataList != null) {
			for (SessionProtos.SessionData data : sessionDataList) {
				if (session.equals(data.getSession())) {
					sessionData = data;
					break;
				}
			}
		}
		
		if (sessionData == null || loginTime != sessionData.getLoginTime()) {
			return Futures.immediateFuture(VerifySessionKeyResponse.newBuilder()
					.setResult(VerifySessionKeyResponse.Result.FAIL_SESSION_EXPIRED)
					.setFailText("身份key过期，请重新登录...")
					.build());
		}
		
		// async update active time and session weizhu
		final int now = (int)(System.currentTimeMillis() / 1000L);
		if (now - sessionData.getActiveTime() > 4 * 60 * 60 // active_time 每隔4小时更新一次，避免频繁写db
				|| (head.hasWeizhu() && !head.getWeizhu().equals(sessionData.getWeizhu()))
				|| (head.hasAndroid() && !head.getAndroid().equals(sessionData.getAndroid())) 
				|| (head.hasIphone() && !head.getIphone().equals(sessionData.getIphone()))
				|| (head.hasWebMobile() && !head.getWebMobile().equals(sessionData.getWebMobile()))
				) {
			
			SessionProtos.SessionData.Builder newSessionDataBuilder = SessionProtos.SessionData.newBuilder()
					.setSession(session)
					.setLoginTime(loginTime)
					.setActiveTime(now)
					;
			if (head.hasWeizhu()) {
				newSessionDataBuilder.setWeizhu(head.getWeizhu());
			}
			if (head.hasAndroid()) {
				newSessionDataBuilder.setAndroid(head.getAndroid());
			}
			if (head.hasIphone()) {
				newSessionDataBuilder.setIphone(head.getIphone());
			}
			if (head.hasWebMobile()) {
				newSessionDataBuilder.setWebMobile(head.getWebMobile());
			}
			this.serviceExecutor.execute(new AsyncUpdateSessionTask(newSessionDataBuilder.build()));
		}
		
		final RequestHead reqHead = ServiceUtil.toRequestHead(head, session);
		
		// get user and check state
		return Futures.transform(
				this.userService.getUserBaseById(reqHead, GetUserBaseByIdRequest.newBuilder().addUserId(session.getUserId()).build()), 
				new Function<GetUserBaseByIdResponse, VerifySessionKeyResponse>() {

					@Override
					public VerifySessionKeyResponse apply(GetUserBaseByIdResponse getUserBaseRsp) {
						UserProtos.UserBase userBase = null;
						for (UserProtos.UserBase u : getUserBaseRsp.getUserBaseList()) {
							if (u.getUserId() == session.getUserId()) {
								userBase = u;
								break;
							}
						}
						
						if (userBase == null) {
							return VerifySessionKeyResponse.newBuilder()
									.setResult(VerifySessionKeyResponse.Result.FAIL_USER_NOT_EXSIT)
									.setFailText("该用户已被删除，请登录其他账号或者联系管理员")
									.setSession(session)
									.build();
						} else if (userBase.hasState() && userBase.getState() == UserProtos.UserBase.State.DISABLE) {
							return VerifySessionKeyResponse.newBuilder()
									.setResult(VerifySessionKeyResponse.Result.FAIL_USER_DISABLE)
									.setFailText("该用户已被停用无法进行任何操作，如有疑问请联系管理员")
									.setSession(session)
									.build();
						} else {
							
							if (reqHead.hasIphone() && reqHead.getIphone().hasDeviceToken() && !reqHead.getIphone().getDeviceToken().isEmpty()) {
								apnsService.updateDeviceToken(reqHead, UpdateDeviceTokenRequest.newBuilder().build());
							}
							
							return VerifySessionKeyResponse.newBuilder()
									.setResult(VerifySessionKeyResponse.Result.SUCC)
									.setSession(session)
									.build();
						}
						
					}
				});
	}
	
	@Override
	public ListenableFuture<GetSessionDataResponse> getSessionData(AdminHead head, GetSessionDataRequest request) {
		if (!head.hasCompanyId()) {
			return Futures.immediateFuture(GetSessionDataResponse.newBuilder().build());
		}
		final long companyId = head.getCompanyId();
		
		Map<Long, List<SessionProtos.SessionData>> sessionDataMap = this.doGetSessionData(companyId, request.getUserIdList());
		
		GetSessionDataResponse.Builder responseBuilder = GetSessionDataResponse.newBuilder();
		for (List<SessionProtos.SessionData> list : sessionDataMap.values()) {
			responseBuilder.addAllSessionData(list);
		}
		return Futures.immediateFuture(responseBuilder.build());
	}
	
	@Override
	public ListenableFuture<DeleteSessionDataResponse> deleteSessionData(AdminHead head, DeleteSessionDataRequest request) {
		if (!head.hasCompanyId() || request.getSessionIdCount() <= 0) {
			return Futures.immediateFuture(DeleteSessionDataResponse.newBuilder()
					.setResult(DeleteSessionDataResponse.Result.SUCC)
					.build());
		}
		
		final long companyId = head.getCompanyId();
		final long userId = request.getUserId();
		
		Map<Long, List<SessionProtos.SessionData>> sessionDataMap;
		
		Connection dbConn = null;
		try {
			dbConn = hikariDataSource.getConnection();
			SessionDB.deleteSession(dbConn, companyId, userId, request.getSessionIdList());
			sessionDataMap = SessionDB.getSessionData(dbConn, companyId, Collections.singleton(userId));
		} catch (SQLException e) {
			throw new RuntimeException("deleteSession db fail", e);
		} finally {
			DBUtil.closeQuietly(dbConn);
		}
		
		Jedis jedis = jedisPool.getResource();
		try {
			SessionCache.setSessionData(jedis, companyId, Collections.<Long> singleton(userId), sessionDataMap);
		} finally {
			jedis.close();
		}
		
		PushUserDeleteRequest.Builder pushUserDeleteRequestBuilder = PushUserDeleteRequest.newBuilder();
		for (long sessionId : request.getSessionIdList()) {
			pushUserDeleteRequestBuilder.addSession(WeizhuProtos.Session.newBuilder()
					.setCompanyId(companyId)
					.setUserId(userId)
					.setSessionId(sessionId)
					.build());
		}
		
		pushService.pushUserDelete(head, pushUserDeleteRequestBuilder.build());
		
		return Futures.immediateFuture(DeleteSessionDataResponse.newBuilder()
				.setResult(DeleteSessionDataResponse.Result.SUCC)
				.build());
	}
	
	@Override
	public ListenableFuture<CreateWebLoginSessionKeyResponse> createWebLoginSessionKey(RequestHead head, EmptyRequest request) {
		final WeizhuProtos.Session session = head.getSession();
		final int now = (int) (System.currentTimeMillis() / 1000L);
		
		final WeizhuProtos.WebLogin webLogin = WeizhuProtos.WebLogin.newBuilder()
				.setWebloginId(this.rand.nextLong())
				.setLoginTime(now)
				.setActiveTime(now)
				.setUserAgent(head.hasWebLogin() ? head.getWebLogin().getUserAgent() : "_init_")
				.build();
		
		final Map<Long, List<SessionProtos.SessionData>> sessionDataMap;
		Connection dbConn = null;
		try {
			dbConn = this.hikariDataSource.getConnection();
			SessionDB.replaceSessionWebLogin(dbConn, session, webLogin);
			sessionDataMap = SessionDB.getSessionData(dbConn, session.getCompanyId(), Collections.singleton(session.getUserId()));
		} catch (SQLException e) {
			throw new RuntimeException(e);
		} finally {
			DBUtil.closeQuietly(dbConn);
		}
		
		Jedis jedis = jedisPool.getResource();
		try {
			SessionCache.setSessionData(jedis, session.getCompanyId(), Collections.<Long> singleton(session.getUserId()), sessionDataMap);
		} finally {
			jedis.close();
		}
		
		ByteBuffer content = ByteBuffer.allocate(36);
		content.putLong(session.getCompanyId());
		content.putLong(session.getUserId());
		content.putLong(session.getSessionId());
		content.putLong(webLogin.getWebloginId());
		content.putInt(webLogin.getLoginTime());

		String webLoginSessionKey = Base64.getUrlEncoder().encodeToString(encrypt(content.array()));
		
		return Futures.immediateFuture(CreateWebLoginSessionKeyResponse.newBuilder()
				.setWebLoginSessionKey(webLoginSessionKey)
				.setWebLogin(webLogin)
				.build());
	}
	
	@Override
	public ListenableFuture<EmptyResponse> deleteWebLoginSessionKey(RequestHead head, EmptyRequest request) {
		final WeizhuProtos.Session session = head.getSession();
		final long companyId = session.getCompanyId();
		final long userId = session.getUserId();
		
		if (!head.hasWebLogin()) {
			return ServiceUtil.EMPTY_RESPONSE_IMMEDIATE_FUTURE;
		}
		
		final Map<Long, List<SessionProtos.SessionData>> sessionDataMap;
		Connection dbConn = null;
		try {
			dbConn = hikariDataSource.getConnection();
			SessionDB.deleteSessionWebLogin(dbConn, session, head.getWebLogin().getWebloginId());
			sessionDataMap = SessionDB.getSessionData(dbConn, companyId, Collections.singleton(userId));
		} catch (SQLException e) {
			throw new RuntimeException("deleteSession db fail", e);
		} finally {
			DBUtil.closeQuietly(dbConn);
		}
		
		Jedis jedis = jedisPool.getResource();
		try {
			SessionCache.setSessionData(jedis, companyId, Collections.<Long> singleton(userId), sessionDataMap);
		} finally {
			jedis.close();
		}
		return ServiceUtil.EMPTY_RESPONSE_IMMEDIATE_FUTURE;
	}

	@Override
	public ListenableFuture<VerifyWebLoginSessionKeyResponse> verifyWebLoginSessionKey(AnonymousHead head, VerifyWebLoginSessionKeyRequest request) {
		byte[] webLoginSessionKeyBytes = null;
		try {
			webLoginSessionKeyBytes = Base64.getUrlDecoder().decode(request.getWebLoginSessionKey());
		} catch (IllegalArgumentException e) {
			webLoginSessionKeyBytes = null;
		}
		
		byte[] bytes = webLoginSessionKeyBytes == null ? null : decrypt(webLoginSessionKeyBytes);
		if (bytes == null || bytes.length != 36) {
			return Futures.immediateFuture(VerifyWebLoginSessionKeyResponse.newBuilder()
					.setResult(VerifyWebLoginSessionKeyResponse.Result.FAIL_SESSION_DECRYPTION)
					.setFailText("身份key解码失败")
					.build());
		}

		ByteBuffer content = ByteBuffer.wrap(bytes);

		final WeizhuProtos.Session session = WeizhuProtos.Session.newBuilder()
				.setCompanyId(content.getLong())
				.setUserId(content.getLong())
				.setSessionId(content.getLong())
				.build();
		final long webloginId = content.getLong();
		final int loginTime = content.getInt();

		SessionProtos.SessionData sessionData = null;
		
		List<SessionProtos.SessionData> sessionDataList = this.doGetSessionData(session.getCompanyId(), 
				Collections.singleton(session.getUserId())).get(session.getUserId());
		if (sessionDataList != null) {
			for (SessionProtos.SessionData data : sessionDataList) {
				if (session.equals(data.getSession())) {
					sessionData = data;
					break;
				}
			}
		}
		
		final int now = (int) (System.currentTimeMillis() / 1000L);
		
		if (sessionData == null 
				|| !sessionData.hasWebLogin() 
				|| sessionData.getWebLogin().getWebloginId() != webloginId 
				|| sessionData.getWebLogin().getLoginTime() != loginTime 
				|| now - sessionData.getWebLogin().getActiveTime() > 12 * 60 * 60) {
			return Futures.immediateFuture(VerifyWebLoginSessionKeyResponse.newBuilder()
					.setResult(VerifyWebLoginSessionKeyResponse.Result.FAIL_SESSION_EXPIRED)
					.setFailText("身份key过期，请重新登录...")
					.build());
		}
		
		// async update active time and session weizhu
		if (now - sessionData.getWebLogin().getActiveTime() > 4 * 60 * 60 // active_time 每隔4小时更新一次，避免频繁写db
				|| (head.hasWebLogin() && !head.getWebLogin().getUserAgent().equals(sessionData.getWebLogin().getUserAgent()))
				) {
			
			WeizhuProtos.WebLogin.Builder webLoginBuilder = sessionData.getWebLogin().toBuilder();
			webLoginBuilder.setActiveTime(now);
			if (head.hasWebLogin()) {
				webLoginBuilder.setUserAgent(head.getWebLogin().getUserAgent());
			}
			
			SessionProtos.SessionData newSessionData = sessionData.toBuilder()
					.setWebLogin(webLoginBuilder.build())
					.build();
			this.serviceExecutor.execute(new AsyncUpdateSessionTask(newSessionData));
		}
		
		final WeizhuProtos.WebLogin webLogin = WeizhuProtos.WebLogin.newBuilder()
				.setWebloginId(webloginId)
				.setLoginTime(loginTime)
				.setActiveTime(now)
				.setUserAgent(head.hasWebLogin() ? head.getWebLogin().getUserAgent() : "")
				.build();
		final RequestHead reqHead = ServiceUtil.toRequestHead(head, session);
		
		// get user and check state
		return Futures.transform(
				this.userService.getUserBaseById(reqHead, GetUserBaseByIdRequest.newBuilder().addUserId(session.getUserId()).build()), 
				new Function<GetUserBaseByIdResponse, VerifyWebLoginSessionKeyResponse>() {

					@Override
					public VerifyWebLoginSessionKeyResponse apply(GetUserBaseByIdResponse getUserBaseRsp) {
						UserProtos.UserBase userBase = null;
						for (UserProtos.UserBase u : getUserBaseRsp.getUserBaseList()) {
							if (u.getUserId() == session.getUserId()) {
								userBase = u;
								break;
							}
						}
						
						if (userBase == null) {
							return VerifyWebLoginSessionKeyResponse.newBuilder()
									.setResult(VerifyWebLoginSessionKeyResponse.Result.FAIL_USER_NOT_EXSIT)
									.setFailText("该用户已被删除，请登录其他账号或者联系管理员")
									.setSession(session)
									.setWebLogin(webLogin)
									.build();
						} else if (userBase.hasState() && userBase.getState() == UserProtos.UserBase.State.DISABLE) {
							return VerifyWebLoginSessionKeyResponse.newBuilder()
									.setResult(VerifyWebLoginSessionKeyResponse.Result.FAIL_USER_DISABLE)
									.setFailText("该用户已被停用无法进行任何操作，如有疑问请联系管理员")
									.setSession(session)
									.setWebLogin(webLogin)
									.build();
						} else {
							return VerifyWebLoginSessionKeyResponse.newBuilder()
									.setResult(VerifyWebLoginSessionKeyResponse.Result.SUCC)
									.setSession(session)
									.setWebLogin(webLogin)
									.build();
						}
						
					}
				});
	}

	private byte[] encrypt(byte[] content) {
		try {
			Cipher c = Cipher.getInstance(ENCRYPTION_ALGORITHM);
			c.init(Cipher.ENCRYPT_MODE, secretKey);
			return c.doFinal(content);
		} catch (Exception e) {
			throw new Error("encrypt error", e);
		}
	}

	private byte[] decrypt(byte[] input) {
		try {
			Cipher c = Cipher.getInstance(ENCRYPTION_ALGORITHM);
			c.init(Cipher.DECRYPT_MODE, secretKey);
			try {
				return c.doFinal(input);
			} catch (Exception e) {
				return null;
			}
		} catch (Exception e) {
			throw new Error("decrypt error", e);
		}
	}
	
	private final class AsyncUpdateSessionTask implements Runnable {

		private final SessionProtos.SessionData sessionData;
		
		AsyncUpdateSessionTask(SessionProtos.SessionData sessionData) {
			this.sessionData = sessionData;
		}
		
		@Override
		public void run() {
			final long companyId = sessionData.getSession().getCompanyId();
			final long userId = sessionData.getSession().getUserId();
			
			Map<Long, List<SessionProtos.SessionData>> sessionDataMap;
			
			Connection dbConn = null;
			try {
				dbConn = hikariDataSource.getConnection();
				SessionDB.updateSession(dbConn, sessionData);
				sessionDataMap = SessionDB.getSessionData(dbConn, companyId, Collections.singleton(userId));
			} catch (SQLException e) {
				throw new RuntimeException("updateSession db fail", e);
			} finally {
				DBUtil.closeQuietly(dbConn);
			}
			
			Jedis jedis = jedisPool.getResource();
			try {
				SessionCache.setSessionData(jedis, companyId, Collections.<Long> singleton(userId), sessionDataMap);
			} finally {
				jedis.close();
			}
		}
		
	}

}
