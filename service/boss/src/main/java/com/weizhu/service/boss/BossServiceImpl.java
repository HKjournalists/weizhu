package com.weizhu.service.boss;

import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Base64;
import java.util.Collections;
import java.util.Random;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.Executor;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import com.google.common.base.Charsets;
import com.google.common.hash.Hashing;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.google.protobuf.InvalidProtocolBufferException;
import com.weizhu.common.db.DBUtil;
import com.weizhu.common.service.ServiceUtil;
import com.weizhu.common.utils.PasswordUtil;
import com.weizhu.proto.BossProtos;
import com.weizhu.proto.BossProtos.BossAnonymousHead;
import com.weizhu.proto.BossProtos.BossHead;
import com.weizhu.proto.BossProtos.LoginRequest;
import com.weizhu.proto.BossProtos.LoginResponse;
import com.weizhu.proto.BossProtos.VerifySessionRequest;
import com.weizhu.proto.BossProtos.VerifySessionResponse;
import com.weizhu.proto.BossService;
import com.weizhu.proto.WeizhuProtos.EmptyRequest;
import com.weizhu.proto.WeizhuProtos.EmptyResponse;
import com.zaxxer.hikari.HikariDataSource;

public class BossServiceImpl implements BossService {

	// private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(BossServiceImpl.class);

	private final Executor serviceExecutor;
	private final HikariDataSource hikariDataSource;
	private final JedisPool jedisPool;
	
	private final String passwordSalt;
	private final SecretKey sessionSecretKey;
	private final Random rand = new Random();

	@Inject
	public BossServiceImpl(
			@Named("service_executor") Executor serviceExecutor, 
			HikariDataSource hikariDataSource, 
			JedisPool jedisPool,
			@Named("boss_session_secret_key") String sessionSecretKey,
			@Named("boss_password_salt") String passwordSalt
			) {
		this.serviceExecutor = serviceExecutor;
		this.hikariDataSource = hikariDataSource;
		this.jedisPool = jedisPool;
		this.passwordSalt = passwordSalt;
		
		byte[] sessionSecretKeyData = new byte[16];
		byte[] tmpBytes = sessionSecretKey.getBytes(Charsets.UTF_8);
		System.arraycopy(tmpBytes, 0, sessionSecretKeyData, 0, tmpBytes.length < sessionSecretKeyData.length ? tmpBytes.length : sessionSecretKeyData.length);
		this.sessionSecretKey = new SecretKeySpec(sessionSecretKeyData, "AES");
	}
	
	private BossProtos.BossSessionData doGetBossSessionData(String bossId) {
		BossProtos.BossSessionData sessionData = null;
		Set<String> noCacheKeySet = new TreeSet<String>();
		Jedis jedis = jedisPool.getResource();
		try {
			sessionData = BossCache.getBossSession(jedis, Collections.singleton(bossId), noCacheKeySet).get(bossId);
		} finally {
			jedis.close();
		}
		
		if (noCacheKeySet.contains(bossId)) {
			Connection dbConn = null;
			try {
				dbConn = hikariDataSource.getConnection();
				sessionData = BossDB.getBossLatestSession(dbConn, bossId);
			} catch (SQLException e) {
				throw new RuntimeException("adminVerifySession db fail!", e);
			} finally {
				DBUtil.closeQuietly(dbConn);
			}
			
			jedis = jedisPool.getResource();
			try {
				if (sessionData == null) {
					BossCache.setBossSession(jedis, Collections.singleton(bossId), Collections.<String, BossProtos.BossSessionData>emptyMap());
				} else {
					BossCache.setBossSession(jedis, Collections.singletonMap(bossId, sessionData));
				}
			} finally {
				jedis.close();
			}
		}
		
		return sessionData;
	}

	@Override
	public ListenableFuture<VerifySessionResponse> verifySession(BossAnonymousHead head, VerifySessionRequest request) {
		BossDAOProtos.SessionKey sessionKey = this.decryptSessionKey(request.getSessionKey());
		if (sessionKey == null) {
			return Futures.immediateFuture(VerifySessionResponse.newBuilder()
					.setResult(VerifySessionResponse.Result.FAIL_SESSION_DECRYPTION)
					.setFailText("会话信息格式不正确")
					.build());
		}
		
		BossProtos.BossSessionData sessionData = this.doGetBossSessionData(sessionKey.getBossId());
		
		final int now = (int) (System.currentTimeMillis() / 1000L);
		if (sessionData == null || sessionData.hasLogoutTime() 
				|| !sessionData.getSession().getBossId().equals(sessionKey.getBossId())
				|| sessionData.getSession().getSessionId() != sessionKey.getSessionId()
				|| sessionData.getLoginTime() != sessionKey.getLoginTime() 
				|| now - sessionKey.getLoginTime() > 12 * 60 * 60 
				) {
			return Futures.immediateFuture(VerifySessionResponse.newBuilder()
					.setResult(VerifySessionResponse.Result.FAIL_SESSION_EXPIRED)
					.setFailText("会话信息过期请重新登陆")
					.build());
		}
		
		if (!head.getUserAgent().equals(sessionData.getUserAgent()) 
				|| now - sessionData.getActiveTime() > 600) {
			
			final BossProtos.BossSessionData newSessionData = sessionData.toBuilder()
					.setUserAgent(head.getUserAgent())
					.setActiveTime(now)
					.build();
			
			Jedis jedis = jedisPool.getResource();
			try {
				BossCache.setBossSession(jedis, Collections.singletonMap(sessionData.getSession().getBossId(), sessionData));
			} finally {
				jedis.close();
			}
			
			this.serviceExecutor.execute(new Runnable() {

				@Override
				public void run() {
					Connection dbConn = null;
					try {
						dbConn = hikariDataSource.getConnection();
						BossDB.setBossSessionUserAgentAndActiveTime(dbConn, 
								newSessionData.getSession(), 
								newSessionData.getUserAgent(), 
								newSessionData.getActiveTime());
					} catch (SQLException e) {
						throw new RuntimeException("updateSession db fail", e);
					} finally {
						DBUtil.closeQuietly(dbConn);
					}
				}
				
			});
		}
		
		return Futures.immediateFuture(VerifySessionResponse.newBuilder()
				.setResult(VerifySessionResponse.Result.SUCC)
				.setSession(sessionData.getSession())
				.build());
	}

	@Override
	public ListenableFuture<LoginResponse> login(BossAnonymousHead head, LoginRequest request) {
		if (request.getBossId().isEmpty()) {
			return Futures.immediateFuture(LoginResponse.newBuilder()
					.setResult(LoginResponse.Result.FAIL_UNKNOWN)
					.setFailText("bossId为空")
					.build());
		}
		if (request.getBossPassword().isEmpty()) {
			return Futures.immediateFuture(LoginResponse.newBuilder()
					.setResult(LoginResponse.Result.FAIL_PASSWORD_INVALID)
					.setFailText("boss登录密码为空")
					.build());
		}
		if (!PasswordUtil.isValid(request.getBossPassword())) {
			return Futures.immediateFuture(LoginResponse.newBuilder()
					.setResult(LoginResponse.Result.FAIL_PASSWORD_INVALID)
					.setFailText("boss登陆密码格式错误." + PasswordUtil.tips())
					.build());
		}

		final String bossId = request.getBossId();
		final String bossPassword = hashPassword(request.getBossPassword());

		Connection dbConn = null;
		try {
			dbConn = hikariDataSource.getConnection();
			String password = BossDB.getBossPassword(dbConn, bossId);
			if (!bossPassword.equals(password)) {
				return Futures.immediateFuture(LoginResponse.newBuilder()
						.setResult(LoginResponse.Result.FAIL_PASSWORD_INVALID)
						.setFailText("boss登陆密码错误.")
						.build());
			}
		} catch (SQLException e) {
			throw new RuntimeException("adminLogin db fail!", e);
		} finally {
			DBUtil.closeQuietly(dbConn);
		}
		
		BossProtos.BossSessionData sessionData = this.doGetBossSessionData(bossId);
		
		final int now = (int) (System.currentTimeMillis() / 1000L);
		final long sessionId = rand.nextLong();
		dbConn = null;
		try {
			dbConn = hikariDataSource.getConnection();
			
			if (sessionData != null) {
				BossDB.setBossSessionLogoutTime(dbConn, sessionData.getSession(), now);
				sessionData = null;
			}
			
			sessionData = BossProtos.BossSessionData.newBuilder()
					.setSession(BossProtos.BossSession.newBuilder()
						.setBossId(bossId)
						.setSessionId(sessionId)
						.build())
					.setLoginTime(now)
					.setLoginHost(head.getRemoteHost())
					.setUserAgent(head.getUserAgent())
					.setActiveTime(now)
					.build();
			BossDB.insertBossSession(dbConn, sessionData);
		} catch (SQLException e) {
			throw new RuntimeException("db fail!", e);
		} finally {
			DBUtil.closeQuietly(dbConn);
		}
		
		Jedis jedis = jedisPool.getResource();
		try {
			BossCache.setBossSession(jedis, Collections.singletonMap(bossId, sessionData));
		} finally {
			jedis.close();
		}
		
		final String sessionKey = this.encryptSessionKey(BossDAOProtos.SessionKey.newBuilder()
				.setBossId(bossId)
				.setSessionId(sessionId)
				.setLoginTime(now)
				.build());
		
		return Futures.immediateFuture(LoginResponse.newBuilder()
				.setResult(LoginResponse.Result.SUCC)
				.setSessionKey(sessionKey)
				.build());
	}

	@Override
	public ListenableFuture<EmptyResponse> logout(BossHead head, EmptyRequest request) {
		final int now = (int) (System.currentTimeMillis() / 1000L);
		Connection dbConn = null;
		try {
			dbConn = hikariDataSource.getConnection();
			BossDB.setBossSessionLogoutTime(dbConn, head.getSession(), now);
		} catch (SQLException e) {
			throw new RuntimeException("adminLogout db fail!", e);
		} finally {
			DBUtil.closeQuietly(dbConn);
		}
		Jedis jedis = jedisPool.getResource();
		try {
			BossCache.delBossSession(jedis, Collections.singleton(head.getSession().getBossId()));
		} finally {
			jedis.close();
		}
		return ServiceUtil.EMPTY_RESPONSE_IMMEDIATE_FUTURE;
	}
	
	private static final String ENCRYPTION_ALGORITHM = "AES/CBC/PKCS5PADDING";
	private static final IvParameterSpec IV_PARAMETER_SPEC = new IvParameterSpec(Base64.getDecoder().decode("M7fSJlrImFALbyxyO+7E3g=="));
	
	private String encryptSessionKey(BossDAOProtos.SessionKey sessionKey) {
		try {
			Cipher cipher = Cipher.getInstance(ENCRYPTION_ALGORITHM);
			cipher.init(Cipher.ENCRYPT_MODE, this.sessionSecretKey, IV_PARAMETER_SPEC);
			return Base64.getUrlEncoder().encodeToString(cipher.doFinal(sessionKey.toByteArray()));
		} catch (InvalidKeyException e) {
			throw new Error(e);
		} catch (NoSuchAlgorithmException e) {
			throw new Error(e);
		} catch (NoSuchPaddingException e) {
			throw new Error(e);
		} catch (IllegalBlockSizeException e) {
			throw new Error(e);
		} catch (BadPaddingException e) {
			throw new Error(e);
		} catch (InvalidAlgorithmParameterException e) {
			throw new Error(e);
		}
	}

	private BossDAOProtos.SessionKey decryptSessionKey(String sessionKeyEncrypted) {
		byte[] dataEncrypted;
		try {
			dataEncrypted = Base64.getUrlDecoder().decode(sessionKeyEncrypted);
		} catch (IllegalArgumentException e) {
			return null;
		}
		
		Cipher cipher;
		try {
			cipher = Cipher.getInstance(ENCRYPTION_ALGORITHM);
			cipher.init(Cipher.DECRYPT_MODE, sessionSecretKey, IV_PARAMETER_SPEC);
		} catch (InvalidKeyException e) {
			throw new Error(e);
		} catch (NoSuchAlgorithmException e) {
			throw new Error(e);
		} catch (NoSuchPaddingException e) {
			throw new Error(e);
		} catch (InvalidAlgorithmParameterException e) {
			throw new Error(e);
		}
		
		byte[] data;
		try {
			data = cipher.doFinal(dataEncrypted);
		} catch (IllegalBlockSizeException e) {
			return null;
		} catch (BadPaddingException e) {
			return null;
		}
		
		try {
			return BossDAOProtos.SessionKey.parseFrom(data);
		} catch (InvalidProtocolBufferException e) {
			return null;
		}
	}
	
	private String hashPassword(String password) {
		return Hashing.sha1().hashString(password + this.passwordSalt + "weizhu@2015", Charsets.UTF_8).toString();
	}

}
