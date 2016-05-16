package com.weizhu.service.profile;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicReference;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.weizhu.proto.AdminProtos.AdminHead;
import com.weizhu.proto.BossProtos.BossHead;
import com.weizhu.proto.ProfileProtos.GetProfileRequest;
import com.weizhu.proto.ProfileProtos.GetProfileResponse;
import com.weizhu.proto.ProfileProtos.SetProfileRequest;
import com.weizhu.proto.ProfileProtos.SetProfileResponse;
import com.weizhu.common.db.DBUtil;
import com.weizhu.common.jedis.JedisTaskLoader;
import com.weizhu.common.service.AsyncImpl;
import com.weizhu.proto.ProfileProtos;
import com.weizhu.proto.ProfileService;
import com.weizhu.proto.WeizhuProtos.AnonymousHead;
import com.weizhu.proto.WeizhuProtos.RequestHead;
import com.weizhu.proto.WeizhuProtos.SystemHead;
import com.zaxxer.hikari.HikariDataSource;

@Singleton
public class ProfileServiceImpl implements ProfileService {

	private static final Logger logger = LoggerFactory.getLogger(ProfileServiceImpl.class);
	private static final String PROFILE_LOAD_TYPE = "profile:load";
	
	private final HikariDataSource hikariDataSource;
	private final JedisTaskLoader jedisTaskLoader;
	
	private final AtomicReference<ImmutableMap<Long, ImmutableList<ProfileProtos.Profile>>> profileMapRef = 
			new AtomicReference<ImmutableMap<Long, ImmutableList<ProfileProtos.Profile>>>(
					ImmutableMap.<Long, ImmutableList<ProfileProtos.Profile>>of());
	
	@Inject
	public ProfileServiceImpl(
			HikariDataSource hikariDataSource, 
			@Named("service_executor") Executor serviceExecutor,
			JedisTaskLoader jedisTaskLoader
			) {
		
		this.hikariDataSource = hikariDataSource;
		this.jedisTaskLoader = jedisTaskLoader;
		
		this.jedisTaskLoader.register(
				PROFILE_LOAD_TYPE, 
				serviceExecutor, 
				new JedisTaskLoader.TaskFactory() {
					
					@Override
					public Runnable createTask(String key) {
						return new LoadProfileTask();
					}
				});
		this.jedisTaskLoader.notifyLoadLocal(PROFILE_LOAD_TYPE, "");
	}
	
	private class LoadProfileTask implements Runnable {

		@Override
		public void run() {
			try {
				ImmutableMap<Long, ImmutableList<ProfileProtos.Profile>> profileMap;
				Connection dbConn = null;
				try {
					dbConn = ProfileServiceImpl.this.hikariDataSource.getConnection();
					profileMap = ProfileDB.getAllProfileValue(dbConn);
				} finally {
					DBUtil.closeQuietly(dbConn);
				}
				ProfileServiceImpl.this.profileMapRef.set(profileMap);
			} catch (Throwable th) {
				logger.error("init load all company team data fail", th);
			}
		}
		
	}
	
	private static final ListenableFuture<GetProfileResponse> EMPTY_GET_PROFILE_RESPONSE = 
			Futures.immediateFuture(GetProfileResponse.newBuilder().build()); 
	
	@AsyncImpl
	@Override
	public ListenableFuture<GetProfileResponse> getProfile(AnonymousHead head, GetProfileRequest request) {
		if (!head.hasCompanyId()) {
			return EMPTY_GET_PROFILE_RESPONSE;
		}
		return this.doGetProfile(head.getCompanyId(), request);
	}

	@AsyncImpl
	@Override
	public ListenableFuture<GetProfileResponse> getProfile(RequestHead head, GetProfileRequest request) {
		return this.doGetProfile(head.getSession().getCompanyId(), request);
	}

	@AsyncImpl
	@Override
	public ListenableFuture<GetProfileResponse> getProfile(AdminHead head, GetProfileRequest request) {
		if (!head.hasCompanyId()) {
			return EMPTY_GET_PROFILE_RESPONSE;
		}
		return this.doGetProfile(head.getCompanyId(), request);
	}
	
	@AsyncImpl
	@Override
	public ListenableFuture<GetProfileResponse> getProfile(SystemHead head, GetProfileRequest request) {
		if (!head.hasCompanyId()) {
			return EMPTY_GET_PROFILE_RESPONSE;
		}
		return this.doGetProfile(head.getCompanyId(), request);
	}
	
	private ListenableFuture<GetProfileResponse> doGetProfile(long companyId, GetProfileRequest request) {
		if (request.getNamePrefixCount() <= 0) {
			return EMPTY_GET_PROFILE_RESPONSE;
		}
		
		final ImmutableList<ProfileProtos.Profile> profileList = this.profileMapRef.get().get(companyId);
		if (profileList == null) {
			return EMPTY_GET_PROFILE_RESPONSE;
		}
		
		GetProfileResponse.Builder responseBuilder = GetProfileResponse.newBuilder();
		for (ProfileProtos.Profile profile : profileList) {
			for (String namePrefix : request.getNamePrefixList()) {
				if (profile.getName().startsWith(namePrefix)) {
					responseBuilder.addProfile(profile);
					break;
				}
			}
		}
		return Futures.immediateFuture(responseBuilder.build());
	}
	
	@Override
	public ListenableFuture<GetProfileResponse> getProfile(BossHead head, GetProfileRequest request) {
		if (!head.hasCompanyId() || request.getNamePrefixCount() <= 0) {
			return EMPTY_GET_PROFILE_RESPONSE;
		}
		
		final long companyId = head.getCompanyId();
		final ImmutableList<ProfileProtos.Profile> profileList = this.profileMapRef.get().get(companyId);
		if (profileList == null) {
			return EMPTY_GET_PROFILE_RESPONSE;
		}
		
		Map<String, String> valueMap = new TreeMap<String, String>();
		for (ProfileProtos.Profile profile : profileList) {
			for (String namePrefix : request.getNamePrefixList()) {
				if (profile.getName().startsWith(namePrefix)) {
					valueMap.put(profile.getName(), profile.getValue());
					break;
				}
			}
		}
		
		Map<String, String> commentMap;
		Connection dbConn = null;
		try {
			dbConn = this.hikariDataSource.getConnection();
			commentMap = ProfileDB.getProfileComment(dbConn, companyId, valueMap.keySet());
		} catch (SQLException e) {
			throw new RuntimeException(e);
		} finally {
			DBUtil.closeQuietly(dbConn);
		}
		
		GetProfileResponse.Builder responseBuilder = GetProfileResponse.newBuilder();
		ProfileProtos.Profile.Builder tmpBuilder = ProfileProtos.Profile.newBuilder();
		for (Entry<String, String> entry : valueMap.entrySet()) {
			tmpBuilder.clear();
			
			String name = entry.getKey();
			tmpBuilder.setName(name);
			tmpBuilder.setValue(entry.getValue());
			String comment = commentMap.get(name);
			if (comment != null) {
				tmpBuilder.setComment(comment);
			}
			responseBuilder.addProfile(tmpBuilder.build());
		}
		return Futures.immediateFuture(responseBuilder.build());
	}

	@Override
	public ListenableFuture<SetProfileResponse> setProfile(AdminHead head, SetProfileRequest request) {
		if (!head.hasCompanyId()) {
			return Futures.immediateFuture(SetProfileResponse.newBuilder()
					.setResult(SetProfileResponse.Result.FAIL_UNKNOWN)
					.setFailText("company_id 参数未填")
					.build());
		}
		final long companyId = head.getCompanyId();
		
		if (request.getProfileCount() <= 0) {
			return Futures.immediateFuture(SetProfileResponse.newBuilder()
					.setResult(SetProfileResponse.Result.SUCC)
					.build());
		}
		
		for (ProfileProtos.Profile profile : request.getProfileList()) {
			if (profile.getName().isEmpty()) {
				return Futures.immediateFuture(SetProfileResponse.newBuilder()
						.setResult(SetProfileResponse.Result.FAIL_NAME_INVALID)
						.setFailText("profile name is empty")
						.build());
			}
			if (profile.getName().length() > 191) {
				return Futures.immediateFuture(SetProfileResponse.newBuilder()
						.setResult(SetProfileResponse.Result.FAIL_NAME_INVALID)
						.setFailText("profile name is too long")
						.build());
			}
			if (profile.getValue().length() > 65535) {
				return Futures.immediateFuture(SetProfileResponse.newBuilder()
						.setResult(SetProfileResponse.Result.FAIL_VALUE_INVALID)
						.setFailText("profile value is too long. name : " + profile.getName())
						.build());
			}
		}
		
		Connection dbConn = null;
		try {
			dbConn = this.hikariDataSource.getConnection();
			ProfileDB.replaceProfileValue(dbConn, companyId, request.getProfileList());
		} catch (SQLException e) {
			throw new RuntimeException("db fail", e);
		} finally {
			DBUtil.closeQuietly(dbConn);
		}
		
		this.jedisTaskLoader.notifyLoad(PROFILE_LOAD_TYPE, "");
		
		return Futures.immediateFuture(SetProfileResponse.newBuilder()
				.setResult(SetProfileResponse.Result.SUCC)
				.build());
	}

	@Override
	public ListenableFuture<SetProfileResponse> setProfile(BossHead head, SetProfileRequest request) {
		if (!head.hasCompanyId()) {
			return Futures.immediateFuture(SetProfileResponse.newBuilder()
					.setResult(SetProfileResponse.Result.FAIL_UNKNOWN)
					.setFailText("company id not find.")
					.build());
		}
		
		final long companyId = head.getCompanyId();
		
		if (request.getProfileCount() <= 0) {
			return Futures.immediateFuture(SetProfileResponse.newBuilder()
					.setResult(SetProfileResponse.Result.SUCC)
					.build());
		}
		
		Map<String, String> commentMap = new TreeMap<String, String>();
		for (ProfileProtos.Profile profile : request.getProfileList()) {
			if (profile.getName().isEmpty()) {
				return Futures.immediateFuture(SetProfileResponse.newBuilder()
						.setResult(SetProfileResponse.Result.FAIL_NAME_INVALID)
						.setFailText("profile name is empty")
						.build());
			}
			if (profile.getName().length() > 191) {
				return Futures.immediateFuture(SetProfileResponse.newBuilder()
						.setResult(SetProfileResponse.Result.FAIL_NAME_INVALID)
						.setFailText("profile name is too long")
						.build());
			}
			if (profile.getValue().length() > 65535) {
				return Futures.immediateFuture(SetProfileResponse.newBuilder()
						.setResult(SetProfileResponse.Result.FAIL_VALUE_INVALID)
						.setFailText("profile value is too long. name : " + profile.getName())
						.build());
			}
			if (profile.hasComment()) {
				if (profile.getComment().length() > 65535) {
					return Futures.immediateFuture(SetProfileResponse.newBuilder()
							.setResult(SetProfileResponse.Result.FAIL_COMMENT_INVALID)
							.setFailText("profile comment is too long. name : " + profile.getName())
							.build());
				}
				commentMap.put(profile.getName(), profile.getComment());
			}
		}
		
		Connection dbConn = null;
		try {
			dbConn = this.hikariDataSource.getConnection();
			ProfileDB.replaceProfileValue(dbConn, companyId, request.getProfileList());
			ProfileDB.replaceProfileComment(dbConn, companyId, commentMap);
		} catch (SQLException e) {
			throw new RuntimeException("db fail", e);
		} finally {
			DBUtil.closeQuietly(dbConn);
		}
		
		this.jedisTaskLoader.notifyLoad(PROFILE_LOAD_TYPE, "");
		
		return Futures.immediateFuture(SetProfileResponse.newBuilder()
				.setResult(SetProfileResponse.Result.SUCC)
				.build());
	}
	
}
