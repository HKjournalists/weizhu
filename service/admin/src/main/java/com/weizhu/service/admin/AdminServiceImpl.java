package com.weizhu.service.admin;

import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.Executor;

import javax.annotation.Nullable;
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
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.google.common.hash.Hashing;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.google.protobuf.InvalidProtocolBufferException;
import com.weizhu.common.db.DBUtil;
import com.weizhu.common.service.ServiceUtil;
import com.weizhu.common.utils.DataPage;
import com.weizhu.common.utils.EmailUtil;
import com.weizhu.common.utils.PasswordUtil;
import com.weizhu.proto.AdminProtos.AdminAnonymousHead;
import com.weizhu.proto.AdminProtos.AdminForgotPasswordRequest;
import com.weizhu.proto.AdminProtos.AdminForgotPasswordResetRequest;
import com.weizhu.proto.AdminProtos.AdminForgotPasswordResetResponse;
import com.weizhu.proto.AdminProtos.AdminForgotPasswordResponse;
import com.weizhu.proto.AdminProtos.AdminHead;
import com.weizhu.proto.AdminProtos.AdminLoginRequest;
import com.weizhu.proto.AdminProtos.AdminLoginResponse;
import com.weizhu.proto.AdminProtos.AdminResetPasswordRequest;
import com.weizhu.proto.AdminProtos.AdminResetPasswordResponse;
import com.weizhu.proto.AdminProtos.AdminSession;
import com.weizhu.proto.AdminProtos.AdminVerifySessionRequest;
import com.weizhu.proto.AdminProtos.AdminVerifySessionResponse;
import com.weizhu.proto.AdminProtos.CreateAdminRequest;
import com.weizhu.proto.AdminProtos.CreateAdminResponse;
import com.weizhu.proto.AdminProtos.CreateRoleRequest;
import com.weizhu.proto.AdminProtos.CreateRoleResponse;
import com.weizhu.proto.AdminProtos.GetAdminByIdRequest;
import com.weizhu.proto.AdminProtos.GetAdminByIdResponse;
import com.weizhu.proto.AdminProtos.GetAdminListRequest;
import com.weizhu.proto.AdminProtos.GetAdminListResponse;
import com.weizhu.proto.AdminProtos.GetRoleByIdRequest;
import com.weizhu.proto.AdminProtos.GetRoleByIdResponse;
import com.weizhu.proto.AdminProtos.GetRoleListRequest;
import com.weizhu.proto.AdminProtos.GetRoleListResponse;
import com.weizhu.proto.AdminProtos.UpdateAdminRequest;
import com.weizhu.proto.AdminProtos.UpdateAdminResponse;
import com.weizhu.proto.AdminProtos.UpdateAdminStateRequest;
import com.weizhu.proto.AdminProtos.UpdateAdminStateResponse;
import com.weizhu.proto.AdminProtos.UpdateRoleRequest;
import com.weizhu.proto.AdminProtos.UpdateRoleResponse;
import com.weizhu.proto.AdminProtos.UpdateRoleStateRequest;
import com.weizhu.proto.AdminProtos.UpdateRoleStateResponse;
import com.weizhu.proto.ExternalProtos.SendEmailRequest;
import com.weizhu.proto.AdminProtos;
import com.weizhu.proto.AdminService;
import com.weizhu.proto.CompanyProtos;
import com.weizhu.proto.CompanyService;
import com.weizhu.proto.ExternalService;
import com.weizhu.proto.WeizhuProtos.EmptyRequest;
import com.weizhu.proto.WeizhuProtos.EmptyResponse;
import com.zaxxer.hikari.HikariDataSource;

public class AdminServiceImpl implements AdminService {
	
	private static final ImmutableSet<AdminProtos.State> NORMAL_STATE_SET = ImmutableSet.of(AdminProtos.State.NORMAL);
	private static final ImmutableSet<AdminProtos.State> NORMAL_DISABLE_STATE_SET = ImmutableSet.of(AdminProtos.State.NORMAL, AdminProtos.State.DISABLE);
	
	private final Executor serviceExecutor;
	private final HikariDataSource hikariDataSource;
	private final JedisPool jedisPool;
	private final CompanyService companyService;
	private final ExternalService externalService;
	
	private final String passwordSalt;
	private final SecretKey sessionSecretKey;
	private final Random rand = new Random();
	
	@Inject
	public AdminServiceImpl(
			@Named("service_executor") Executor serviceExecutor, 
			HikariDataSource hikariDataSource, JedisPool jedisPool, 
			CompanyService companyService, ExternalService externalService,
			@Named("admin_session_secret_key") String sessionSecretKey,
			@Named("admin_password_salt") String passwordSalt
			) {
		this.serviceExecutor = serviceExecutor;
		this.hikariDataSource = hikariDataSource;
		this.jedisPool = jedisPool;
		this.companyService = companyService;
		this.externalService = externalService;
		this.passwordSalt = passwordSalt;
		
		byte[] sessionSecretKeyData = new byte[16];
		byte[] tmpBytes = sessionSecretKey.getBytes(Charsets.UTF_8);
		System.arraycopy(tmpBytes, 0, sessionSecretKeyData, 0, tmpBytes.length < sessionSecretKeyData.length ? tmpBytes.length : sessionSecretKeyData.length);
		this.sessionSecretKey = new SecretKeySpec(sessionSecretKeyData, "AES");
	}
	
	private Map<Long, AdminProtos.AdminSessionData> doGetAdminSessionData(Set<Long> adminIdSet) {
		if (adminIdSet.isEmpty()) {
			return Collections.emptyMap();
		}
		
		Map<Long, AdminProtos.AdminSessionData> sessionDataMap = new TreeMap<Long, AdminProtos.AdminSessionData>();
		Set<Long> noCacheKeySet = new TreeSet<Long>();
		Jedis jedis = jedisPool.getResource();
		try {
			sessionDataMap.putAll(AdminCache.getAdminSession(jedis, adminIdSet, noCacheKeySet));
		} finally {
			jedis.close();
		}
		
		if (!noCacheKeySet.isEmpty()) {
			Map<Long, AdminProtos.AdminSessionData> noCacheSessionDataMap;
			Connection dbConn = null;
			try {
				dbConn = hikariDataSource.getConnection();
				noCacheSessionDataMap = AdminDB.getLatestAdminSession(dbConn, noCacheKeySet);
			} catch (SQLException e) {
				throw new RuntimeException("adminVerifySession db fail!", e);
			} finally {
				DBUtil.closeQuietly(dbConn);
			}
			
			jedis = jedisPool.getResource();
			try {
				AdminCache.setAdminSession(jedis, noCacheKeySet, noCacheSessionDataMap);
			} finally {
				jedis.close();
			}
			
			sessionDataMap.putAll(noCacheSessionDataMap);
		}
		return sessionDataMap;
	}
	
	private Map<Long, AdminProtos.Admin> doGetAdmin(Collection<Long> adminIds, @Nullable Collection<AdminProtos.State> states) {
		if (adminIds.isEmpty()) {
			return Collections.emptyMap();
		}
		
		Map<Long, AdminProtos.Admin> adminMap = new TreeMap<Long, AdminProtos.Admin>();
		Set<Long> noCacheKeySet = new TreeSet<Long>();
		Jedis jedis = jedisPool.getResource();
		try {
			adminMap.putAll(AdminCache.getAdmin(jedis, adminIds, noCacheKeySet));
		} finally {
			jedis.close();
		}
		
		if (!noCacheKeySet.isEmpty()) {
			Map<Long, AdminProtos.Admin> noCacheAdminMap;
			
			Connection dbConn = null;
			try {
				dbConn = hikariDataSource.getConnection();
				noCacheAdminMap = AdminDB.getAdmin(dbConn, noCacheKeySet, null);
			} catch (SQLException e) {
				throw new RuntimeException("adminVerifySession db fail!", e);
			} finally {
				DBUtil.closeQuietly(dbConn);
			}
			
			jedis = jedisPool.getResource();
			try {
				AdminCache.setAdmin(jedis, noCacheKeySet, noCacheAdminMap);
			} finally {
				jedis.close();
			}
			
			adminMap.putAll(noCacheAdminMap);
		}
		
		if (states != null) {
			Iterator<AdminProtos.Admin> it = adminMap.values().iterator();
			while (it.hasNext()) {
				if (!states.contains(it.next().getState())) {
					it.remove();
				}
			}
		}
		return adminMap;
	}
	
	private Map<Integer, AdminProtos.Role> doGetRole(Collection<Integer> roleIds, @Nullable Collection<AdminProtos.State> states) {
		if (roleIds.isEmpty()) {
			return Collections.emptyMap();
		}
		
		Map<Integer, AdminProtos.Role> roleMap = new TreeMap<Integer, AdminProtos.Role>();
		Set<Integer> noCacheKeySet = new TreeSet<Integer>();
		Jedis jedis = jedisPool.getResource();
		try {
			roleMap.putAll(AdminCache.getRole(jedis, roleIds, noCacheKeySet));
		} finally {
			jedis.close();
		}
		
		if (!noCacheKeySet.isEmpty()) {
			Map<Integer, AdminProtos.Role> noCacheRoleMap;
			
			Connection dbConn = null;
			try {
				dbConn = hikariDataSource.getConnection();
				noCacheRoleMap = AdminDB.getRole(dbConn, noCacheKeySet, null);
			} catch (SQLException e) {
				throw new RuntimeException("adminVerifySession db fail!", e);
			} finally {
				DBUtil.closeQuietly(dbConn);
			}
			
			jedis = jedisPool.getResource();
			try {
				AdminCache.setRole(jedis, noCacheKeySet, noCacheRoleMap);
			} finally {
				jedis.close();
			}
			
			roleMap.putAll(noCacheRoleMap);
		}
		
		if (states != null) {
			Iterator<AdminProtos.Role> it = roleMap.values().iterator();
			while (it.hasNext()) {
				if (!states.contains(it.next().getState())) {
					it.remove();
				}
			}
		}
		return roleMap;
	}
	
	private Map<Long, CompanyProtos.Company> doGetCompany(AdminAnonymousHead head) {
		List<CompanyProtos.Company> companyList = Futures.getUnchecked(
				this.companyService.getCompanyList(head, ServiceUtil.EMPTY_REQUEST))
				.getCompanyList();
		if (companyList.isEmpty()) {
			return Collections.emptyMap();
		}
		Map<Long, CompanyProtos.Company> companyMap = new TreeMap<Long, CompanyProtos.Company>();
		for (CompanyProtos.Company company : companyList) {
			companyMap.put(company.getCompanyId(), company);
		}
		return companyMap;
	}
	
	@Override
	public ListenableFuture<AdminVerifySessionResponse> adminVerifySession(AdminAnonymousHead head, AdminVerifySessionRequest request) {
		final AdminDAOProtos.SessionKey sessionKey = this.decryptSessionKey(request.getSessionKey());
		if (sessionKey == null) {
			return Futures.immediateFuture(AdminVerifySessionResponse.newBuilder()
					.setResult(AdminVerifySessionResponse.Result.FAIL_SESSION_DECRYPTION)
					.setFailText("会话信息格式不正确")
					.build());
		}
		
		AdminProtos.AdminSessionData sessionData = this.doGetAdminSessionData(Collections.singleton(sessionKey.getAdminId())).get(sessionKey.getAdminId());
		
		final int now = (int) (System.currentTimeMillis() / 1000L);
		if (sessionData == null || sessionData.hasLogoutTime() 
				|| sessionData.getSession().getAdminId() != sessionKey.getAdminId()
				|| sessionData.getSession().getSessionId() != sessionKey.getSessionId()
				|| sessionData.getLoginTime() != sessionKey.getLoginTime() 
				|| now - sessionKey.getLoginTime() > 12 * 60 * 60 
				) {
			return Futures.immediateFuture(AdminVerifySessionResponse.newBuilder()
					.setResult(AdminVerifySessionResponse.Result.FAIL_SESSION_EXPIRED)
					.setFailText("会话信息过期请重新登陆")
					.build());
		}
		
		final AdminProtos.Admin admin = this.doGetAdmin(Collections.singleton(sessionKey.getAdminId()), null).get(sessionKey.getAdminId());
		if (admin == null) {
			return Futures.immediateFuture(AdminVerifySessionResponse.newBuilder()
					.setResult(AdminVerifySessionResponse.Result.FAIL_SESSION_EXPIRED)
					.setFailText("该管理员不存在")
					.build());
		} else if (admin.getState() == AdminProtos.State.DELETE) {
			return Futures.immediateFuture(AdminVerifySessionResponse.newBuilder()
					.setResult(AdminVerifySessionResponse.Result.FAIL_SESSION_EXPIRED)
					.setFailText("该管理员已被删除")
					.build());
		} else if (admin.getState() != AdminProtos.State.NORMAL) {
			return Futures.immediateFuture(AdminVerifySessionResponse.newBuilder()
					.setResult(AdminVerifySessionResponse.Result.FAIL_ADMIN_DISABLE)
					.setFailText("该管理员已被停用")
					.build());
		} else if (admin.getForceResetPassword()) {
			return Futures.immediateFuture(AdminVerifySessionResponse.newBuilder()
					.setResult(AdminVerifySessionResponse.Result.FAIL_SESSION_EXPIRED)
					.setFailText("该管理员需要重新设置密码后再登录")
					.build());
		} else if (admin.getCompanyCount() <= 0) {
			return Futures.immediateFuture(AdminVerifySessionResponse.newBuilder()
					.setResult(AdminVerifySessionResponse.Result.FAIL_ADMIN_DISABLE)
					.setFailText("该管理员已被停用.无可管理公司")
					.build());
		}
		
		final Map<Long, CompanyProtos.Company> companyMap = this.doGetCompany(head);
		boolean isFind = false;
		for (AdminProtos.Admin.Company c : admin.getCompanyList()) {
			if (companyMap.containsKey(c.getCompanyId())) {
				isFind = true;
				break;
			}
		}
		if (!isFind) {
			return Futures.immediateFuture(AdminVerifySessionResponse.newBuilder()
					.setResult(AdminVerifySessionResponse.Result.FAIL_ADMIN_DISABLE)
					.setFailText("该管理员已被停用.无可管理公司")
					.build());
		}
		
		if (!head.getUserAgent().equals(sessionData.getUserAgent()) 
				|| now - sessionData.getActiveTime() > 600) {
			
			final AdminProtos.AdminSessionData newSessionData = sessionData.toBuilder()
					.setUserAgent(head.getUserAgent())
					.setActiveTime(now)
					.build();
			
			Jedis jedis = jedisPool.getResource();
			try {
				AdminCache.setAdminSession(jedis, Collections.singletonMap(sessionData.getSession().getAdminId(), sessionData));
			} finally {
				jedis.close();
			}
			
			this.serviceExecutor.execute(new Runnable() {

				@Override
				public void run() {
					Connection dbConn = null;
					try {
						dbConn = hikariDataSource.getConnection();
						AdminDB.updateAdminSessionUserAgentAndActiveTime(dbConn, newSessionData.getSession(), 
								newSessionData.getUserAgent(), newSessionData.getActiveTime());
					} catch (SQLException e) {
						throw new RuntimeException("updateSession db fail", e);
					} finally {
						DBUtil.closeQuietly(dbConn);
					}
				}
				
			});
		}
		
		AdminVerifySessionResponse.Builder responseBuilder = AdminVerifySessionResponse.newBuilder();
		responseBuilder.setResult(AdminVerifySessionResponse.Result.SUCC);
		responseBuilder.setSession(sessionData.getSession());
		responseBuilder.setAdmin(admin);

		Set<Integer> roleIdSet = new TreeSet<Integer>();
		for (AdminProtos.Admin.Company c : admin.getCompanyList()) {
			CompanyProtos.Company company = companyMap.get(c.getCompanyId());
			if (company != null) {
				responseBuilder.addRefCompany(company);
				roleIdSet.addAll(c.getRoleIdList());
			}
		}
		responseBuilder.addAllRefRole(this.doGetRole(roleIdSet, NORMAL_STATE_SET).values());
		
		return Futures.immediateFuture(responseBuilder.build());
	}

	@Override
	public ListenableFuture<AdminLoginResponse> adminLogin(AdminAnonymousHead head, AdminLoginRequest request) {
		if (request.getAdminEmail().isEmpty()) {
			return Futures.immediateFuture(AdminLoginResponse.newBuilder()
					.setResult(AdminLoginResponse.Result.FAIL_EMAIL_INVALID)
					.setFailText("管理员邮箱为空.")
					.build());
		}
		if (!EmailUtil.isValid(request.getAdminEmail())) {
			return Futures.immediateFuture(AdminLoginResponse.newBuilder()
					.setResult(AdminLoginResponse.Result.FAIL_EMAIL_INVALID)
					.setFailText("管理员邮箱格式错误.")
					.build());
		}
		if (request.getAdminPassword().isEmpty()) {
			return Futures.immediateFuture(AdminLoginResponse.newBuilder()
					.setResult(AdminLoginResponse.Result.FAIL_PASSWORD_INVALID)
					.setFailText("管理员登陆密码为空")
					.build());
		}
		if (!PasswordUtil.isValid(request.getAdminPassword())) {
			return Futures.immediateFuture(AdminLoginResponse.newBuilder()
					.setResult(AdminLoginResponse.Result.FAIL_PASSWORD_INVALID)
					.setFailText("管理员登陆密码格式错误." + PasswordUtil.tips())
					.build());
		}

		final String adminEmail = request.getAdminEmail();
		final String adminPassword = this.hashPassword(request.getAdminPassword());
		final Long adminId;
		Connection dbConn = null;
		try {
			dbConn = hikariDataSource.getConnection();
			adminId = AdminDB.getAdminIdByEmailUniqueAndPassword(dbConn, adminEmail, adminPassword);
		} catch (SQLException e) {
			throw new RuntimeException("adminLogin db fail!", e);
		} finally {
			DBUtil.closeQuietly(dbConn);
		}
		
		if (adminId == null) {
			return Futures.immediateFuture(AdminLoginResponse.newBuilder()
					.setResult(AdminLoginResponse.Result.FAIL_EMAIL_OR_PASSWORD_INVALID)
					.setFailText("管理员邮箱或者密码错误.")
					.build());
		}
		
		final AdminProtos.Admin admin = this.doGetAdmin(Collections.singleton(adminId), null).get(adminId);
		if (admin == null) {
			return Futures.immediateFuture(AdminLoginResponse.newBuilder()
					.setResult(AdminLoginResponse.Result.FAIL_EMAIL_OR_PASSWORD_INVALID)
					.setFailText("管理员邮箱或者密码错误.")
					.build());
		} else if (admin.getState() == AdminProtos.State.DELETE) {
			return Futures.immediateFuture(AdminLoginResponse.newBuilder()
					.setResult(AdminLoginResponse.Result.FAIL_ADMIN_DISABLE)
					.setFailText("该管理员已被删除")
					.build());
		} else if (admin.getState() != AdminProtos.State.NORMAL) {
			return Futures.immediateFuture(AdminLoginResponse.newBuilder()
					.setResult(AdminLoginResponse.Result.FAIL_ADMIN_DISABLE)
					.setFailText("该管理员已被停用")
					.build());
		} else if (admin.getForceResetPassword()) {
			return Futures.immediateFuture(AdminLoginResponse.newBuilder()
					.setResult(AdminLoginResponse.Result.FAIL_ADMIN_FORCE_RESET_PASSWORD)
					.setFailText("该管理员需要重置密码后再登陆")
					.build());
		} else if (admin.getCompanyCount() <= 0) {
			return Futures.immediateFuture(AdminLoginResponse.newBuilder()
					.setResult(AdminLoginResponse.Result.FAIL_ADMIN_DISABLE)
					.setFailText("该管理员已被停用.无可管理公司")
					.build());
		}
		
		final Map<Long, CompanyProtos.Company> companyMap = this.doGetCompany(head);
		boolean isFind = false;
		for (AdminProtos.Admin.Company c : admin.getCompanyList()) {
			if (companyMap.containsKey(c.getCompanyId())) {
				isFind = true;
				break;
			}
		}
		if (!isFind) {
			return Futures.immediateFuture(AdminLoginResponse.newBuilder()
					.setResult(AdminLoginResponse.Result.FAIL_ADMIN_DISABLE)
					.setFailText("该管理员已被停用.无可管理公司")
					.build());
		}
		
		final boolean firstLogin = this.doGetAdminSessionData(Collections.singleton(adminId)).containsKey(adminId);
		final int now = (int) (System.currentTimeMillis() / 1000L);
		final long sessionId = rand.nextLong();
		final AdminProtos.AdminSessionData sessionData = AdminProtos.AdminSessionData.newBuilder()
					.setSession(AdminSession.newBuilder()
						.setAdminId(admin.getAdminId())
						.setSessionId(sessionId)
						.build())
					.setLoginTime(now)
					.setLoginHost(head.getRemoteHost())
					.setUserAgent(head.getUserAgent())
					.setActiveTime(now)
					.build();
		
		dbConn = null;
		try {
			dbConn = hikariDataSource.getConnection();
			AdminDB.insertAdminSession(dbConn, sessionData);
		} catch (SQLException e) {
			throw new RuntimeException("db fail!", e);
		} finally {
			DBUtil.closeQuietly(dbConn);
		}
		
		Jedis jedis = jedisPool.getResource();
		try {
			AdminCache.setAdminSession(jedis, Collections.singletonMap(admin.getAdminId(), sessionData));
		} finally {
			jedis.close();
		}
		
		AdminLoginResponse.Builder responseBuilder = AdminLoginResponse.newBuilder();
		responseBuilder.setResult(AdminLoginResponse.Result.SUCC);
		responseBuilder.setSessionKey(this.encryptSessionKey(AdminDAOProtos.SessionKey.newBuilder()
				.setAdminId(sessionData.getSession().getAdminId())
				.setSessionId(sessionData.getSession().getSessionId())
				.setLoginTime(sessionData.getLoginTime())
				.build()));
		responseBuilder.setSession(sessionData.getSession());
		responseBuilder.setAdmin(admin);

		Set<Integer> roleIdSet = new TreeSet<Integer>();
		for (AdminProtos.Admin.Company c : admin.getCompanyList()) {
			CompanyProtos.Company company = companyMap.get(c.getCompanyId());
			if (company != null) {
				responseBuilder.addRefCompany(company);
				roleIdSet.addAll(c.getRoleIdList());
			}
		}
		responseBuilder.addAllRefRole(this.doGetRole(roleIdSet, NORMAL_STATE_SET).values());
		responseBuilder.setFirstLogin(firstLogin);
		
		return Futures.immediateFuture(responseBuilder.build());
	}

	@Override
	public ListenableFuture<EmptyResponse> adminLogout(AdminHead head, EmptyRequest request) {
		final int now = (int) (System.currentTimeMillis() / 1000L);
		Connection dbConn = null;
		try {
			dbConn = hikariDataSource.getConnection();
			AdminDB.updateAdminSessionLogoutTime(dbConn, head.getSession(), now);
		} catch (SQLException e) {
			throw new RuntimeException("adminLogout db fail!", e);
		} finally {
			DBUtil.closeQuietly(dbConn);
		}
		Jedis jedis = jedisPool.getResource();
		try {
			AdminCache.delAdminSession(jedis, Collections.singleton(head.getSession().getAdminId()));
		} finally {
			jedis.close();
		}
		return ServiceUtil.EMPTY_RESPONSE_IMMEDIATE_FUTURE;
	}

	@Override
	public ListenableFuture<AdminResetPasswordResponse> adminResetPassword(AdminAnonymousHead head, AdminResetPasswordRequest request) {
		if (request.getAdminEmail().isEmpty()) {
			return Futures.immediateFuture(AdminResetPasswordResponse.newBuilder()
					.setResult(AdminResetPasswordResponse.Result.FAIL_EMAIL_INVALID)
					.setFailText("管理员邮箱为空.")
					.build());
		}
		if (!EmailUtil.isValid(request.getAdminEmail())) {
			return Futures.immediateFuture(AdminResetPasswordResponse.newBuilder()
					.setResult(AdminResetPasswordResponse.Result.FAIL_EMAIL_INVALID)
					.setFailText("管理员邮箱格式错误.")
					.build());
		}
		if (request.getOldPassword().isEmpty()) {
			return Futures.immediateFuture(AdminResetPasswordResponse.newBuilder()
					.setResult(AdminResetPasswordResponse.Result.FAIL_OLD_PASSWORD_INVALID)
					.setFailText("旧密码为空")
					.build());
		}
		if (!PasswordUtil.isValid(request.getOldPassword())) {
			return Futures.immediateFuture(AdminResetPasswordResponse.newBuilder()
					.setResult(AdminResetPasswordResponse.Result.FAIL_OLD_PASSWORD_INVALID)
					.setFailText("旧密码格式不正确.")
					.build());
		}
		if (request.getNewPassword().isEmpty()) {
			return Futures.immediateFuture(AdminResetPasswordResponse.newBuilder()
					.setResult(AdminResetPasswordResponse.Result.FAIL_NEW_PASSWORD_INVALID)
					.setFailText("新密码为空")
					.build());
		}
		if (!PasswordUtil.isValid(request.getNewPassword())) {
			return Futures.immediateFuture(AdminResetPasswordResponse.newBuilder()
					.setResult(AdminResetPasswordResponse.Result.FAIL_NEW_PASSWORD_INVALID)
					.setFailText("新密码格式不正确." + PasswordUtil.tips())
					.build());
		}
		if (request.getOldPassword().equals(request.getNewPassword())) {
			return Futures.immediateFuture(AdminResetPasswordResponse.newBuilder()
					.setResult(AdminResetPasswordResponse.Result.FAIL_NEW_PASSWORD_INVALID)
					.setFailText("新密码不能和原有密码相同.")
					.build());
		}
		
		final String adminEmail = request.getAdminEmail();
		final String oldPassword = hashPassword(request.getOldPassword());
		final String newPassword = hashPassword(request.getNewPassword());
		
		final Long adminId;
		Connection dbConn = null;
		try {
			dbConn = hikariDataSource.getConnection();
			adminId = AdminDB.getAdminIdByEmailUnique(dbConn, adminEmail);
		} catch (SQLException e) {
			throw new RuntimeException("reset password db fail!", e);
		} finally {
			DBUtil.closeQuietly(dbConn);
		}
		
		AdminProtos.Admin admin = adminId == null ? null : this.doGetAdmin(Collections.singleton(adminId), null).get(adminId);
		if (admin == null) {
			return Futures.immediateFuture(AdminResetPasswordResponse.newBuilder()
					.setResult(AdminResetPasswordResponse.Result.FAIL_ADMIN_NOT_EXIST)
					.setFailText("该管理员不存在")
					.build());
		} else if (admin.getState() == AdminProtos.State.DELETE) {
			return Futures.immediateFuture(AdminResetPasswordResponse.newBuilder()
					.setResult(AdminResetPasswordResponse.Result.FAIL_ADMIN_NOT_EXIST)
					.setFailText("该管理员已被删除")
					.build());
		} else if (admin.getState() != AdminProtos.State.NORMAL) {
			return Futures.immediateFuture(AdminResetPasswordResponse.newBuilder()
					.setResult(AdminResetPasswordResponse.Result.FAIL_ADMIN_NOT_EXIST)
					.setFailText("该管理员已被停用")
					.build());
		}
		
		dbConn = null;
		try {
			dbConn = hikariDataSource.getConnection();
			boolean succ = AdminDB.updateAdminPassword(dbConn, adminId, oldPassword, newPassword, false, NORMAL_STATE_SET);
			if (!succ) {				
				return Futures.immediateFuture(AdminResetPasswordResponse.newBuilder()
						.setResult(AdminResetPasswordResponse.Result.FAIL_OLD_PASSWORD_INVALID)
						.setFailText("管理员密码不正确")
						.build());
			}
		} catch (SQLException e) {
			throw new RuntimeException("reset password db fail!", e);
		} finally {
			DBUtil.closeQuietly(dbConn);
		}
		
		Jedis jedis = jedisPool.getResource();
		try {
			AdminCache.delAdmin(jedis, Collections.singleton(adminId));
		} finally {
			jedis.close();
		}
		
		return Futures.immediateFuture(AdminResetPasswordResponse.newBuilder()
				.setResult(AdminResetPasswordResponse.Result.SUCC)
				.build());
	}

	@Override
	public ListenableFuture<AdminForgotPasswordResponse> adminForgotPassword(AdminAnonymousHead head, AdminForgotPasswordRequest request) {
		if (request.getAdminEmail().isEmpty()) {
			return Futures.immediateFuture(AdminForgotPasswordResponse.newBuilder()
					.setResult(AdminForgotPasswordResponse.Result.FAIL_EMAIL_INVALID)
					.setFailText("管理员邮箱为空.")
					.build());
		}
		if (!EmailUtil.isValid(request.getAdminEmail())) {
			return Futures.immediateFuture(AdminForgotPasswordResponse.newBuilder()
					.setResult(AdminForgotPasswordResponse.Result.FAIL_EMAIL_INVALID)
					.setFailText("管理员邮箱格式错误.")
					.build());
		}
		
		final Long adminId;
		Connection dbConn = null;
		try {
			dbConn = hikariDataSource.getConnection();
			adminId = AdminDB.getAdminIdByEmailUnique(dbConn, request.getAdminEmail());
		} catch (SQLException e) {
			throw new RuntimeException("adminLogin db fail!", e);
		} finally {
			DBUtil.closeQuietly(dbConn);
		}
		
		AdminProtos.Admin admin = adminId == null ? null : this.doGetAdmin(Collections.singleton(adminId), null).get(adminId);
		if (admin == null) {
			return Futures.immediateFuture(AdminForgotPasswordResponse.newBuilder()
					.setResult(AdminForgotPasswordResponse.Result.FAIL_ADMIN_NOT_EXIST)
					.setFailText("该管理员不存在")
					.build());
		} else if (admin.getState() == AdminProtos.State.DELETE) {
			return Futures.immediateFuture(AdminForgotPasswordResponse.newBuilder()
					.setResult(AdminForgotPasswordResponse.Result.FAIL_ADMIN_NOT_EXIST)
					.setFailText("该管理员已被删除")
					.build());
		} else if (admin.getState() != AdminProtos.State.NORMAL) {
			return Futures.immediateFuture(AdminForgotPasswordResponse.newBuilder()
					.setResult(AdminForgotPasswordResponse.Result.FAIL_ADMIN_NOT_EXIST)
					.setFailText("该管理员已被停用")
					.build());
		}
		
		final long token = 10000000 + rand.nextInt(90000000);
		final int now = (int) (System.currentTimeMillis() / 1000L);
		
		Jedis jedis = this.jedisPool.getResource();
		try {
			AdminDAOProtos.ForgotToken forgotToken = AdminCache.getAdminForgotToken(jedis, Collections.singleton(adminId)).get(adminId);
			
			if (forgotToken != null && now - forgotToken.getCreateTime() < 60) {
				return Futures.immediateFuture(AdminForgotPasswordResponse.newBuilder()
						.setResult(AdminForgotPasswordResponse.Result.FAIL_SEND_LIMIT_EXCEEDED)
						.setFailText("发送邮件太频繁，每分钟只能发送一封邮件。请等待一下")
						.build());
			}
			
			forgotToken = AdminDAOProtos.ForgotToken.newBuilder()
					.setForgotToken(token)
					.setCreateTime(now)
					.build();
			
			AdminCache.setAdminForgotToken(jedis, Collections.singletonMap(adminId, forgotToken));
		} finally {
			jedis.close();
		}
		
		this.externalService.sendEmail(head, SendEmailRequest.newBuilder()
				.addToRecipients(request.getAdminEmail())
				.setSubject("[微助] 管理员找回密码")
				.setHtmlContent("您正在进行邮箱验证，本次请求的验证码为：" + token + " (为了保障您帐号的安全性，请在1小时内完成验证)")
				.build());
		
		return Futures.immediateFuture(AdminForgotPasswordResponse.newBuilder()
				.setResult(AdminForgotPasswordResponse.Result.SUCC)
				.build());
	}

	@Override
	public ListenableFuture<AdminForgotPasswordResetResponse> adminForgotPasswordReset(AdminAnonymousHead head, AdminForgotPasswordResetRequest request) {
		if (request.getAdminEmail().isEmpty()) {
			return Futures.immediateFuture(AdminForgotPasswordResetResponse.newBuilder()
					.setResult(AdminForgotPasswordResetResponse.Result.FAIL_EMAIL_INVALID)
					.setFailText("管理员邮箱为空.")
					.build());
		}
		if (!EmailUtil.isValid(request.getAdminEmail())) {
			return Futures.immediateFuture(AdminForgotPasswordResetResponse.newBuilder()
					.setResult(AdminForgotPasswordResetResponse.Result.FAIL_EMAIL_INVALID)
					.setFailText("管理员邮箱格式错误.")
					.build());
		}
		if (request.getNewPassword().isEmpty()) {
			return Futures.immediateFuture(AdminForgotPasswordResetResponse.newBuilder()
					.setResult(AdminForgotPasswordResetResponse.Result.FAIL_NEW_PASSWORD_INVALID)
					.setFailText("新密码为空")
					.build());
		}
		if (!PasswordUtil.isValid(request.getNewPassword())) {
			return Futures.immediateFuture(AdminForgotPasswordResetResponse.newBuilder()
					.setResult(AdminForgotPasswordResetResponse.Result.FAIL_NEW_PASSWORD_INVALID)
					.setFailText("新密码格式不正确." + PasswordUtil.tips())
					.build());
		}
		
		final Long adminId;
		Connection dbConn = null;
		try {
			dbConn = hikariDataSource.getConnection();
			adminId = AdminDB.getAdminIdByEmailUnique(dbConn, request.getAdminEmail());
		} catch (SQLException e) {
			throw new RuntimeException("db fail!", e);
		} finally {
			DBUtil.closeQuietly(dbConn);
		}
		
		AdminProtos.Admin admin = adminId == null ? null : this.doGetAdmin(Collections.singleton(adminId), null).get(adminId);
		if (admin == null) {
			return Futures.immediateFuture(AdminForgotPasswordResetResponse.newBuilder()
					.setResult(AdminForgotPasswordResetResponse.Result.FAIL_UNKNOWN)
					.setFailText("该管理员不存在")
					.build());
		} else if (admin.getState() == AdminProtos.State.DELETE) {
			return Futures.immediateFuture(AdminForgotPasswordResetResponse.newBuilder()
					.setResult(AdminForgotPasswordResetResponse.Result.FAIL_UNKNOWN)
					.setFailText("该管理员已被删除")
					.build());
		} else if (admin.getState() != AdminProtos.State.NORMAL) {
			return Futures.immediateFuture(AdminForgotPasswordResetResponse.newBuilder()
					.setResult(AdminForgotPasswordResetResponse.Result.FAIL_UNKNOWN)
					.setFailText("该管理员已被停用")
					.build());
		}
		
		final int now = (int) (System.currentTimeMillis() / 1000L);
		Jedis jedis = this.jedisPool.getResource();
		try {
			AdminDAOProtos.ForgotToken forgotToken = AdminCache.getAdminForgotToken(jedis, Collections.singleton(adminId)).get(adminId);
			
			if (forgotToken == null || now - forgotToken.getCreateTime() > 60 * 60) {
				return Futures.immediateFuture(AdminForgotPasswordResetResponse.newBuilder()
						.setResult(AdminForgotPasswordResetResponse.Result.FAIL_FORGOT_TOKEN_EXPIRE)
						.setFailText("找回密码邮件已过期，请重新发起找回密码请求")
						.build());
			}
			
			if (forgotToken.getForgotToken() != request.getForgotToken()) {
				return Futures.immediateFuture(AdminForgotPasswordResetResponse.newBuilder()
						.setResult(AdminForgotPasswordResetResponse.Result.FAIL_FORGOT_TOKEN_EXPIRE)
						.setFailText("找回密码邮件不正确，请重新发起找回密码请求")
						.build());
			}
			
			AdminCache.delAdminForgotToken(jedis, Collections.singleton(adminId));
		} finally {
			jedis.close();
		}
		
		final String newPassword = hashPassword(request.getNewPassword());
		dbConn = null;
		try {
			dbConn = hikariDataSource.getConnection();
			AdminDB.updateAdminPassword(dbConn, adminId, newPassword, NORMAL_STATE_SET);
		} catch (SQLException e) {
			throw new RuntimeException("db fail!", e);
		} finally {
			DBUtil.closeQuietly(dbConn);
		}
		
		return Futures.immediateFuture(AdminForgotPasswordResetResponse.newBuilder()
				.setResult(AdminForgotPasswordResetResponse.Result.SUCC)
				.build());
	}
	
	@Override
	public ListenableFuture<GetAdminByIdResponse> getAdminById(AdminHead head, GetAdminByIdRequest request) {
		Map<Long, AdminProtos.Admin> adminMap = this.doGetAdmin(request.getAdminIdList(), NORMAL_DISABLE_STATE_SET);
		
		Set<Integer> refRoleIdSet = new TreeSet<Integer>();
		for (AdminProtos.Admin admin : adminMap.values()) {
			for (AdminProtos.Admin.Company company : admin.getCompanyList()) {
				refRoleIdSet.addAll(company.getRoleIdList());
			}
		}
		Map<Integer, AdminProtos.Role> refRoleMap = this.doGetRole(refRoleIdSet, NORMAL_STATE_SET);
		
		return Futures.immediateFuture(GetAdminByIdResponse.newBuilder()
				.addAllAdmin(adminMap.values())
				.addAllRefRole(refRoleMap.values())
				.build());
	}
	
	@Override
	public ListenableFuture<GetAdminListResponse> getAdminList(AdminHead head, GetAdminListRequest request) {
		if (!head.hasCompanyId()) {
			return Futures.immediateFuture(GetAdminListResponse.newBuilder()
					.setTotalSize(0)
					.setFilteredSize(0)
					.build());
		}
		final long companyId = head.getCompanyId();
		final AdminProtos.State state = request.hasState() ? request.getState() : null;
		final String nameKeyword = request.hasNameKeyword() && !request.getNameKeyword().trim().isEmpty() ? request.getNameKeyword().trim() : null;
		
		final DataPage<Long> adminIdPage;
		Connection dbConn = null;
		try {
			dbConn = hikariDataSource.getConnection();
			adminIdPage = AdminDB.getAdminIdPage(dbConn, companyId, request.getStart(), request.getLength(), state, nameKeyword, NORMAL_DISABLE_STATE_SET);
		} catch (SQLException e) {
			throw new RuntimeException("getAdminList db fail!", e);
		} finally {
			DBUtil.closeQuietly(dbConn);
		}
		
		Map<Long, AdminProtos.Admin> adminMap = this.doGetAdmin(adminIdPage.dataList(), NORMAL_DISABLE_STATE_SET);
		
		Set<Integer> refRoleIdSet = new TreeSet<Integer>();
		for (AdminProtos.Admin admin : adminMap.values()) {
			for (AdminProtos.Admin.Company company : admin.getCompanyList()) {
				refRoleIdSet.addAll(company.getRoleIdList());
			}
		}
		Map<Integer, AdminProtos.Role> refRoleMap = this.doGetRole(refRoleIdSet, NORMAL_STATE_SET);
		
		GetAdminListResponse.Builder responseBuilder = GetAdminListResponse.newBuilder();
		for (Long adminId : adminIdPage.dataList()) {
			AdminProtos.Admin admin = adminMap.get(adminId);
			if (admin != null) {
				responseBuilder.addAdmin(admin);
			}
		}
		
		responseBuilder.setTotalSize(adminIdPage.totalSize());
		responseBuilder.setFilteredSize(adminIdPage.filteredSize());
		responseBuilder.addAllRefRole(refRoleMap.values());
		return Futures.immediateFuture(responseBuilder.build());
	}

	@Override
	public ListenableFuture<CreateAdminResponse> createAdmin(AdminHead head, CreateAdminRequest request) {
		if (!head.hasCompanyId()) {
			return Futures.immediateFuture(CreateAdminResponse.newBuilder()
					.setResult(CreateAdminResponse.Result.FAIL_UNKNOWN)
					.setFailText("company_id 参数为空")
					.build());
		}
		final long companyId = head.getCompanyId();
		
		// check name
		if (request.getAdminName().isEmpty()) {
			return Futures.immediateFuture(CreateAdminResponse.newBuilder()
					.setResult(CreateAdminResponse.Result.FAIL_NAME_INVALID)
					.setFailText("管理员名为空.")
					.build());
		}
		if (request.getAdminName().length() > 100) {
			return Futures.immediateFuture(CreateAdminResponse.newBuilder()
					.setResult(CreateAdminResponse.Result.FAIL_NAME_INVALID)
					.setFailText("管理员名错误. 名字长度最大100")
					.build());
		}
		
		// check email format
		if (request.getAdminEmail().isEmpty()) {
			return Futures.immediateFuture(CreateAdminResponse.newBuilder()
					.setResult(CreateAdminResponse.Result.FAIL_EMAIL_INVALID)
					.setFailText("邮件名为空")
					.build());
		}
		if (!EmailUtil.isValid(request.getAdminEmail())) {
			return Futures.immediateFuture(CreateAdminResponse.newBuilder()
					.setResult(CreateAdminResponse.Result.FAIL_EMAIL_INVALID)
					.setFailText("邮件名不正确")
					.build());
		}
		
		// check password
		if (request.getAdminPassword().isEmpty()) {
			return Futures.immediateFuture(CreateAdminResponse.newBuilder()
					.setResult(CreateAdminResponse.Result.FAIL_PASSWORD_INVALID)
					.setFailText("管理员密码为空." + PasswordUtil.tips())
					.build());
		}
		if (!PasswordUtil.isValid(request.getAdminPassword())) {
			return Futures.immediateFuture(CreateAdminResponse.newBuilder()
					.setResult(CreateAdminResponse.Result.FAIL_PASSWORD_INVALID)
					.setFailText("管理员密码格式不正确." + PasswordUtil.tips())
					.build());
		}
		
		List<Integer> roleIdList = new ArrayList<Integer>();
		for (AdminProtos.Role role : this.doGetRole(request.getRoleIdList(), NORMAL_STATE_SET).values()) {
			if (!role.hasCompanyId() || role.getCompanyId() == companyId) {
				roleIdList.add(role.getRoleId());
			}
		}
		
		final AdminProtos.Admin admin = AdminProtos.Admin.newBuilder()
				.setAdminId(-1L)
				.setAdminName(request.getAdminName())
				.setAdminEmail(request.getAdminEmail())
				.setForceResetPassword(true)
				.addCompany(AdminProtos.Admin.Company.newBuilder()
						.setCompanyId(companyId)
						.addAllRoleId(roleIdList)
						.setEnableTeamPermit(request.getEnableTeamPermit())
						.addAllPermitTeamId(request.getEnableTeamPermit() ? request.getPermitTeamIdList() : Collections.<Integer>emptyList())
						.build())
				.setCreateTime((int) (System.currentTimeMillis() / 1000L))
				.setCreateAdminId(head.getSession().getAdminId())
				.build();
		
		final String password = hashPassword(request.getAdminPassword());

		final long adminId;
		Connection dbConn = null;
		try {
			dbConn = hikariDataSource.getConnection();

			if (AdminDB.getAdminIdByEmailUnique(dbConn, admin.getAdminEmail()) != null) {
				return Futures.immediateFuture(CreateAdminResponse.newBuilder()
						.setResult(CreateAdminResponse.Result.FAIL_EMAIL_INVALID)
						.setFailText("管理员邮箱已存在")
						.build());
			}
			
			adminId = AdminDB.insertAdmin(dbConn, admin, password);
		} catch (SQLException e) {
			throw new RuntimeException("createAdmin db fail!", e);
		} finally {
			DBUtil.closeQuietly(dbConn);
		}
		
		Jedis jedis = jedisPool.getResource();
		try {
			AdminCache.delAdmin(jedis, Collections.singleton(adminId));
			AdminCache.delAdminSession(jedis, Collections.singleton(adminId));
		} finally {
			jedis.close();
		}
		
		return Futures.immediateFuture(CreateAdminResponse.newBuilder()
				.setResult(CreateAdminResponse.Result.SUCC)
				.setAdminId(adminId)
				.build());
	}

	@Override
	public ListenableFuture<UpdateAdminResponse> updateAdmin(AdminHead head, UpdateAdminRequest request) {
		if (!head.hasCompanyId()) {
			return Futures.immediateFuture(UpdateAdminResponse.newBuilder()
					.setResult(UpdateAdminResponse.Result.FAIL_UNKNOWN)
					.setFailText("company_id 参数为空")
					.build());
		}
		final long companyId = head.getCompanyId();
		
		// check name
		if (request.getAdminName().isEmpty()) {
			return Futures.immediateFuture(UpdateAdminResponse.newBuilder()
					.setResult(UpdateAdminResponse.Result.FAIL_NAME_INVALID)
					.setFailText("管理员名为空.")
					.build());
		}
		if (request.getAdminName().length() > 100) {
			return Futures.immediateFuture(UpdateAdminResponse.newBuilder()
					.setResult(UpdateAdminResponse.Result.FAIL_NAME_INVALID)
					.setFailText("管理员名错误. 名字长度最大100")
					.build());
		}
		// check email format
		if (request.getAdminEmail().isEmpty()) {
			return Futures.immediateFuture(UpdateAdminResponse.newBuilder()
					.setResult(UpdateAdminResponse.Result.FAIL_EMAIL_INVALID)
					.setFailText("邮件名为空")
					.build());
		}
		if (!EmailUtil.isValid(request.getAdminEmail())) {
			return Futures.immediateFuture(UpdateAdminResponse.newBuilder()
					.setResult(UpdateAdminResponse.Result.FAIL_EMAIL_INVALID)
					.setFailText("邮件名不正确")
					.build());
		}
		
		Set<Long> adminIdSet = new TreeSet<Long>();
		adminIdSet.add(head.getSession().getAdminId());
		adminIdSet.add(request.getAdminId());
		
		Map<Long, AdminProtos.Admin> adminMap = this.doGetAdmin(adminIdSet, NORMAL_DISABLE_STATE_SET);
		final AdminProtos.Admin reqAdmin = adminMap.get(head.getSession().getAdminId()); // 操作的管理员
		if (reqAdmin == null) {
			return Futures.immediateFuture(UpdateAdminResponse.newBuilder()
					.setResult(UpdateAdminResponse.Result.FAIL_UNKNOWN)
					.setFailText("操作管理员信息错误")
					.build());
		}
		
		Set<Long> reqAdminCompanyIdSet = new TreeSet<Long>();
		for (AdminProtos.Admin.Company c : reqAdmin.getCompanyList()) {
			reqAdminCompanyIdSet.add(c.getCompanyId());
		}
		// 检查操作管理员是否可以操作此公司
		if (!reqAdminCompanyIdSet.contains(companyId)) {
			return Futures.immediateFuture(UpdateAdminResponse.newBuilder()
					.setResult(UpdateAdminResponse.Result.FAIL_UNKNOWN)
					.setFailText("您无权修改此公司管理员信息")
					.build());
		}
		
		final AdminProtos.Admin oldAdmin = adminMap.get(request.getAdminId()); // 被操作的管理员
		if (oldAdmin == null) {
			return Futures.immediateFuture(UpdateAdminResponse.newBuilder()
					.setResult(UpdateAdminResponse.Result.FAIL_ADMIN_NOT_EXIST)
					.setFailText("被修改管理员信息未找到或者已经被删除")
					.build());
		}
		
		Set<Long> oldAdminCompanyIdSet = new TreeSet<Long>();
		for (AdminProtos.Admin.Company c : oldAdmin.getCompanyList()) {
			oldAdminCompanyIdSet.add(c.getCompanyId());
		}
		// 检查被操作管理员是否隶属于此公司
		if (!oldAdminCompanyIdSet.contains(companyId)) {
			return Futures.immediateFuture(UpdateAdminResponse.newBuilder()
					.setResult(UpdateAdminResponse.Result.FAIL_UNKNOWN)
					.setFailText("被修改管理员不属于此公司")
					.build());
		}
		
		// 检查请求操作的管理员所属公司是否包含被操作的管理员公司
		if (!reqAdminCompanyIdSet.containsAll(oldAdminCompanyIdSet)) {
			return Futures.immediateFuture(UpdateAdminResponse.newBuilder()
					.setResult(UpdateAdminResponse.Result.FAIL_UNKNOWN)
					.setFailText("您无权修改此管理员信息：" + oldAdmin.getAdminName())
					.build());
		}
		
		List<Integer> roleIdList = new ArrayList<Integer>();
		for (AdminProtos.Role role : this.doGetRole(request.getRoleIdList(), NORMAL_STATE_SET).values()) {
			if (!role.hasCompanyId() || role.getCompanyId() == companyId) {
				roleIdList.add(role.getRoleId());
			}
		}
		
		AdminProtos.Admin.Builder newAdminBuilder = oldAdmin.toBuilder()
				.setAdminName(request.getAdminName())
				.setAdminEmail(request.getAdminEmail())
				.setForceResetPassword(request.getForceResetPassword())
				.setUpdateTime((int) (System.currentTimeMillis() / 1000L))
				.setUpdateAdminId(head.getSession().getAdminId())
				.clearCompany()
				.addCompany(AdminProtos.Admin.Company.newBuilder()
						.setCompanyId(companyId)
						.addAllRoleId(roleIdList)
						.setEnableTeamPermit(request.getEnableTeamPermit())
						.addAllPermitTeamId(request.getEnableTeamPermit() ? request.getPermitTeamIdList() : Collections.<Integer>emptyList())
						.build());
		for (AdminProtos.Admin.Company oldAdminCompany : oldAdmin.getCompanyList()) {
			if (oldAdminCompany.getCompanyId() != companyId) {
				newAdminBuilder.addCompany(oldAdminCompany);
			}
		}
		
		final AdminProtos.Admin newAdmin = newAdminBuilder.build();
		
		Connection dbConn = null;
		try {
			dbConn = hikariDataSource.getConnection();
			
			if (!oldAdmin.getAdminEmail().equals(newAdmin.getAdminEmail())) {
				if (AdminDB.getAdminIdByEmailUnique(dbConn, newAdmin.getAdminEmail()) != null) {
					return Futures.immediateFuture(UpdateAdminResponse.newBuilder()
							.setResult(UpdateAdminResponse.Result.FAIL_EMAIL_INVALID)
							.setFailText("该邮箱已被使用")
							.build());
				}
			}
			
			AdminDB.updateAdmin(dbConn, 
					Collections.singletonMap(request.getAdminId(), oldAdmin), 
					Collections.singletonMap(request.getAdminId(), newAdmin));
		} catch (SQLException e) {
			throw new RuntimeException("updateAdmin db fail!", e);
		} finally {
			DBUtil.closeQuietly(dbConn);
		}
		
		Jedis jedis = jedisPool.getResource();
		try {
			AdminCache.delAdmin(jedis, Collections.singleton(request.getAdminId()));
		} finally {
			jedis.close();
		}
		
		return Futures.immediateFuture(UpdateAdminResponse.newBuilder()
				.setResult(UpdateAdminResponse.Result.SUCC)
				.build());
	}

	@Override
	public ListenableFuture<UpdateAdminStateResponse> updateAdminState(AdminHead head, UpdateAdminStateRequest request) {
		if (request.getAdminIdCount() <= 0) {
			return Futures.immediateFuture(UpdateAdminStateResponse.newBuilder()
					.setResult(UpdateAdminStateResponse.Result.SUCC)
					.build()); 
		}
		
		Set<Long> adminIdSet = new TreeSet<Long>(request.getAdminIdList());
		if (adminIdSet.contains(head.getSession().getAdminId())) {
			return Futures.immediateFuture(UpdateAdminStateResponse.newBuilder()
					.setResult(UpdateAdminStateResponse.Result.FAIL_UPDATE_SELF)
					.setFailText("管理员不能改变自己的状态")
					.build()); 
		}
		
		adminIdSet.add(head.getSession().getAdminId());
		Map<Long, AdminProtos.Admin> adminMap = this.doGetAdmin(adminIdSet, NORMAL_DISABLE_STATE_SET);
		if (!adminIdSet.equals(adminMap.keySet())) {
			return Futures.immediateFuture(UpdateAdminStateResponse.newBuilder()
					.setResult(UpdateAdminStateResponse.Result.FAIL_UNKNOWN)
					.setFailText("以下管理员id未找到对应的管理员信息：" + Sets.difference(adminIdSet, adminMap.keySet()))
					.build());
		}
		
		final AdminProtos.Admin reqAdmin = adminMap.get(head.getSession().getAdminId());
		Set<Long> reqAdminCompanyIdSet = new TreeSet<Long>();
		for (AdminProtos.Admin.Company c : reqAdmin.getCompanyList()) {
			reqAdminCompanyIdSet.add(c.getCompanyId());
		}
		
		// 检查请求操作的管理员所属公司是否包含被操作的管理员公司
		List<String> invalidAdminNameList = new ArrayList<String>();
		for (AdminProtos.Admin admin : adminMap.values()) {
			if (admin.getAdminId() != head.getSession().getAdminId()) {
				boolean isInvalid = false;
				for (AdminProtos.Admin.Company c : admin.getCompanyList()) {
					if (!reqAdminCompanyIdSet.contains(c.getCompanyId())) {
						isInvalid = true;
						break;
					}
				}
				
				if (isInvalid) {
					invalidAdminNameList.add(admin.getAdminName());
				}
			}
		}
		
		if (!invalidAdminNameList.isEmpty()) {
			return Futures.immediateFuture(UpdateAdminStateResponse.newBuilder()
					.setResult(UpdateAdminStateResponse.Result.FAIL_UNKNOWN)
					.setFailText("您无权修改以下管理员状态：" + invalidAdminNameList)
					.build());
		}
		
		// 删除掉不需要修改状态的管理员id
		Iterator<Long> it = adminIdSet.iterator();
		while (it.hasNext()) {
			Long adminId = it.next();
			if (adminId == head.getSession().getAdminId() || adminMap.get(adminId).getState() == request.getState()) {
				it.remove();
			}
		}
		
		Connection dbConn = null;
		try {
			dbConn = hikariDataSource.getConnection();
			AdminDB.updateAdminState(dbConn, adminIdSet, request.getState());
		} catch (SQLException e) {
			throw new RuntimeException("updateAdminState db fail!", e);
		} finally {
			DBUtil.closeQuietly(dbConn);
		}
		
		Jedis jedis = jedisPool.getResource();
		try {
			AdminCache.delAdmin(jedis, adminIdSet);
		} finally {
			jedis.close();
		}
		return Futures.immediateFuture(UpdateAdminStateResponse.newBuilder()
				.setResult(UpdateAdminStateResponse.Result.SUCC)
				.build());
	}
	
	@Override
	public ListenableFuture<GetRoleByIdResponse> getRoleById(AdminHead head, GetRoleByIdRequest request) {
		Map<Integer, AdminProtos.Role> roleMap = this.doGetRole(request.getRoleIdList(), NORMAL_DISABLE_STATE_SET);
		return Futures.immediateFuture(GetRoleByIdResponse.newBuilder()
				.addAllRole(roleMap.values())
				.build());
	}

	@Override
	public ListenableFuture<GetRoleListResponse> getRoleList(AdminHead head, GetRoleListRequest request) {
		if (!head.hasCompanyId()) {
			return Futures.immediateFuture(GetRoleListResponse.newBuilder()
					.setTotalSize(0)
					.setFilteredSize(0)
					.build());
		}
		final long companyId = head.getCompanyId();
		final DataPage<Integer> roleIdPage;
		Connection dbConn = null;
		try {
			dbConn = this.hikariDataSource.getConnection();
			roleIdPage = AdminDB.getRoleIdPage(dbConn, companyId, request.getStart(), request.getLength(), NORMAL_DISABLE_STATE_SET);
		} catch (SQLException e) {
			throw new RuntimeException("deleteAdmin db fail!", e);
		} finally {
			DBUtil.closeQuietly(dbConn);
		}
		
		Map<Integer, AdminProtos.Role> roleMap = this.doGetRole(roleIdPage.dataList(), NORMAL_DISABLE_STATE_SET);
		
		GetRoleListResponse.Builder responseBuilder = GetRoleListResponse.newBuilder();
		for (Integer roleId : roleIdPage.dataList()) {
			AdminProtos.Role role = roleMap.get(roleId);
			if (role != null) {
				responseBuilder.addRole(role);
			}
		}
		responseBuilder.setTotalSize(roleIdPage.totalSize());
		responseBuilder.setFilteredSize(roleIdPage.filteredSize());
		return Futures.immediateFuture(responseBuilder.build());
	}

	@Override
	public ListenableFuture<CreateRoleResponse> createRole(AdminHead head, CreateRoleRequest request) {
		if (!head.hasCompanyId()) {
			return Futures.immediateFuture(CreateRoleResponse.newBuilder()
					.setResult(CreateRoleResponse.Result.FAIL_UNKNOWN)
					.setFailText("company_id 参数为空")
					.build());
		}
		final long companyId = head.getCompanyId();
		
		final String roleName = request.getRoleName().trim();
		if (roleName.isEmpty()) {
			return Futures.immediateFuture(CreateRoleResponse.newBuilder()
					.setResult(CreateRoleResponse.Result.FAIL_NAME_INVALID)
					.setFailText("角色名称为空")
					.build());
		}
		if (roleName.length() > 100) {
			return Futures.immediateFuture(CreateRoleResponse.newBuilder()
					.setResult(CreateRoleResponse.Result.FAIL_NAME_INVALID)
					.setFailText("角色名称太长")
					.build());
		}
		Set<String> permissionIdSet = new TreeSet<String>();
		for (String permissionId : request.getPermissionIdList()) {
			permissionId = permissionId.trim();
			if (permissionId.isEmpty()) {
				return Futures.immediateFuture(CreateRoleResponse.newBuilder()
						.setResult(CreateRoleResponse.Result.FAIL_PERMISSION_INVALID)
						.setFailText("权限id不能为空字符串")
						.build());
			}
			if (permissionId.length() > 191) {
				return Futures.immediateFuture(CreateRoleResponse.newBuilder()
						.setResult(CreateRoleResponse.Result.FAIL_PERMISSION_INVALID)
						.setFailText("权限id不能超过191个字符")
						.build());
			}
			permissionIdSet.add(permissionId);
		}
		
		final AdminProtos.Role role = AdminProtos.Role.newBuilder()
				.setCompanyId(companyId)
				.setRoleId(0)
				.setRoleName(roleName)
				.addAllPermissionId(permissionIdSet)
				.setState(AdminProtos.State.NORMAL)
				.setCreateTime((int) (System.currentTimeMillis()))
				.setCreateAdminId(head.getSession().getAdminId())
				.build();
		
		final int roleId;
		Connection dbConn = null;
		try {
			dbConn = this.hikariDataSource.getConnection();
			roleId = AdminDB.insertRole(dbConn, role);
		} catch (SQLException e) {
			throw new RuntimeException("createRole db fail!", e);
		} finally {
			DBUtil.closeQuietly(dbConn);
		}
		
		Jedis jedis = jedisPool.getResource();
		try {
			AdminCache.delRole(jedis, Collections.singleton(roleId));
		} finally {
			jedis.close();
		}
		
		return Futures.immediateFuture(CreateRoleResponse.newBuilder()
				.setResult(CreateRoleResponse.Result.SUCC)
				.setRoleId(roleId)
				.build());
	}

	@Override
	public ListenableFuture<UpdateRoleResponse> updateRole(AdminHead head, UpdateRoleRequest request) {
		if (!head.hasCompanyId()) {
			return Futures.immediateFuture(UpdateRoleResponse.newBuilder()
					.setResult(UpdateRoleResponse.Result.FAIL_UNKNOWN)
					.setFailText("company_id 参数为空")
					.build());
		}
		final long companyId = head.getCompanyId();
		
		final String roleName = request.getRoleName().trim();
		if (roleName.isEmpty()) {
			return Futures.immediateFuture(UpdateRoleResponse.newBuilder()
					.setResult(UpdateRoleResponse.Result.FAIL_NAME_INVALID)
					.setFailText("角色名称为空")
					.build());
		}
		if (roleName.length() > 100) {
			return Futures.immediateFuture(UpdateRoleResponse.newBuilder()
					.setResult(UpdateRoleResponse.Result.FAIL_NAME_INVALID)
					.setFailText("角色名称太长")
					.build());
		}
		Set<String> permissionIdSet = new TreeSet<String>();
		for (String permissionId : request.getPermissionIdList()) {
			permissionId = permissionId.trim();
			if (permissionId.isEmpty()) {
				return Futures.immediateFuture(UpdateRoleResponse.newBuilder()
						.setResult(UpdateRoleResponse.Result.FAIL_PERMISSION_INVALID)
						.setFailText("权限id不能为空字符串")
						.build());
			}
			if (permissionId.length() > 191) {
				return Futures.immediateFuture(UpdateRoleResponse.newBuilder()
						.setResult(UpdateRoleResponse.Result.FAIL_PERMISSION_INVALID)
						.setFailText("权限id不能超过191个字符")
						.build());
			}
			permissionIdSet.add(permissionId);
		}
		
		final int roleId = request.getRoleId();
		final AdminProtos.Role oldRole = this.doGetRole(Collections.singleton(roleId), NORMAL_DISABLE_STATE_SET).get(roleId);
		if (oldRole == null || (oldRole.hasCompanyId() && oldRole.getCompanyId() != companyId)) {
			return Futures.immediateFuture(UpdateRoleResponse.newBuilder()
					.setResult(UpdateRoleResponse.Result.FAIL_ROLE_NOT_EXIST)
					.setFailText("角色不存在")
					.build());
		}
		if (!oldRole.hasCompanyId()) {
			return Futures.immediateFuture(UpdateRoleResponse.newBuilder()
					.setResult(UpdateRoleResponse.Result.FAIL_UNKNOWN)
					.setFailText("该角色为系统内置角色，不可修改")
					.build());
		}
		
		final AdminProtos.Role newRole = oldRole.toBuilder()
				.setRoleName(roleName)
				.clearPermissionId()
				.addAllPermissionId(permissionIdSet)
				.setUpdateTime((int) (System.currentTimeMillis() / 1000L))
				.setUpdateAdminId(head.getSession().getAdminId())
				.build();
		
		Connection dbConn = null;
		try {
			dbConn = this.hikariDataSource.getConnection();
			AdminDB.updateRole(dbConn, 
					Collections.<Integer, AdminProtos.Role>singletonMap(roleId, oldRole), 
					Collections.<Integer, AdminProtos.Role>singletonMap(roleId, newRole)
					);
		} catch (SQLException e) {
			throw new RuntimeException("updateRole db fail!", e);
		} finally {
			DBUtil.closeQuietly(dbConn);
		}
		
		Jedis jedis = jedisPool.getResource();
		try {
			AdminCache.delRole(jedis, Collections.singleton(roleId));
		} finally {
			jedis.close();
		}
		
		return Futures.immediateFuture(UpdateRoleResponse.newBuilder()
				.setResult(UpdateRoleResponse.Result.SUCC)
				.build());
	}

	@Override
	public ListenableFuture<UpdateRoleStateResponse> updateRoleState(AdminHead head, UpdateRoleStateRequest request) {
		if (!head.hasCompanyId()) {
			return Futures.immediateFuture(UpdateRoleStateResponse.newBuilder()
					.setResult(UpdateRoleStateResponse.Result.FAIL_UNKNOWN)
					.setFailText("company_id 参数为空")
					.build());
		}
		final long companyId = head.getCompanyId();
		
		Map<Integer, AdminProtos.Role> roleMap = this.doGetRole(request.getRoleIdList(), NORMAL_DISABLE_STATE_SET);
		List<Integer> roleIdList = new ArrayList<Integer>();
		for (AdminProtos.Role role : roleMap.values()) {
			if (!role.hasCompanyId()) {
				return Futures.immediateFuture(UpdateRoleStateResponse.newBuilder()
						.setResult(UpdateRoleStateResponse.Result.FAIL_UNKNOWN)
						.setFailText("\'" + role.getRoleName() + "\'为系统内置角色，不能修改" )
						.build());
			}
			
			if (role.getCompanyId() == companyId && role.getState() != request.getState()) {
				roleIdList.add(role.getRoleId());
			}
		}
		
		if (roleIdList.isEmpty()) {
			return Futures.immediateFuture(UpdateRoleStateResponse.newBuilder()
					.setResult(UpdateRoleStateResponse.Result.SUCC)
					.build());
		}
		
		Connection dbConn = null;
		try {
			dbConn = this.hikariDataSource.getConnection();
			AdminDB.updateRoleState(dbConn, roleIdList, request.getState(), NORMAL_DISABLE_STATE_SET);
		} catch (SQLException e) {
			throw new RuntimeException("updateRoleState db fail!", e);
		} finally {
			DBUtil.closeQuietly(dbConn);
		}
		
		Jedis jedis = jedisPool.getResource();
		try {
			AdminCache.delRole(jedis, roleIdList);
		} finally {
			jedis.close();
		}
		
		return Futures.immediateFuture(UpdateRoleStateResponse.newBuilder()
				.setResult(UpdateRoleStateResponse.Result.SUCC)
				.build());
	}

	private static final String ENCRYPTION_ALGORITHM = "AES/CBC/PKCS5PADDING";
	private static final IvParameterSpec IV_PARAMETER_SPEC = new IvParameterSpec(Base64.getDecoder().decode("Nmut/dRCX7oOMtu7hk0w/g=="));
	
	private String encryptSessionKey(AdminDAOProtos.SessionKey sessionKey) {
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

	private AdminDAOProtos.SessionKey decryptSessionKey(String sessionKeyEncrypted) {
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
			return AdminDAOProtos.SessionKey.parseFrom(data);
		} catch (InvalidProtocolBufferException e) {
			return null;
		}
	}
	
	private String hashPassword(String password) {
		return Hashing.sha1().hashString(password + this.passwordSalt + "admin@2016", Charsets.UTF_8).toString();
	}

}
