package com.weizhu.service.user.base;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Pattern;

import javax.annotation.Nullable;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.weizhu.common.db.DBUtil;
import com.weizhu.common.utils.DataPage;
import com.weizhu.common.utils.EmailUtil;
import com.weizhu.common.utils.MobileNoUtil;
import com.weizhu.common.utils.ProfileManager;
import com.weizhu.proto.AdminProtos.AdminHead;
import com.weizhu.proto.AdminUserProtos.CreateUserRequest;
import com.weizhu.proto.AdminUserProtos.CreateUserResponse;
import com.weizhu.proto.AdminUserProtos.SetExpertRequest;
import com.weizhu.proto.AdminUserProtos.SetExpertResponse;
import com.weizhu.proto.AdminUserProtos.SetStateRequest;
import com.weizhu.proto.AdminUserProtos.SetStateResponse;
import com.weizhu.proto.AdminUserProtos.UpdateUserRequest;
import com.weizhu.proto.AdminUserProtos.UpdateUserResponse;
import com.weizhu.proto.ExternalProtos.SendSmsRequest;
import com.weizhu.proto.ExternalService;
import com.weizhu.proto.UserProtos;
import com.weizhu.proto.UserProtos.UpdateUserAvatarRequest;
import com.weizhu.proto.UserProtos.UpdateUserAvatarResponse;
import com.weizhu.proto.UserProtos.UpdateUserInterestRequest;
import com.weizhu.proto.UserProtos.UpdateUserInterestResponse;
import com.weizhu.proto.UserProtos.UpdateUserSignatureRequest;
import com.weizhu.proto.UserProtos.UpdateUserSignatureResponse;
import com.weizhu.proto.WeizhuProtos.RequestHead;
import com.weizhu.service.user.UserUtil;
import com.zaxxer.hikari.HikariDataSource;

@Singleton
public class UserBaseManager {
	
	private static final ProfileManager.ProfileKey<String> USER_APPROVE_SUCC_SMS_TEXT = 
			ProfileManager.createKey("user:approve_succ_sms_text", (String) null);

	private final HikariDataSource hikariDataSource;
	private final JedisPool jedisPool;
	
	private final ExternalService externalService;
	private final ProfileManager profileManager;
	
	@Inject
	public UserBaseManager(HikariDataSource hikariDataSource, JedisPool jedisPool,
			ExternalService externalService, ProfileManager profileManager
			) {
		this.hikariDataSource = hikariDataSource;
		this.jedisPool = jedisPool;
		
		this.externalService = externalService;
		this.profileManager = profileManager;
	}
	
	public Map<String, Long> getUserIdByMobileNoUnique(long companyId, Collection<String> mobileNos) {
		Connection dbConn = null;
		try {
			dbConn = hikariDataSource.getConnection();
			return UserBaseDB.getUserIdByMobileNoUnique(dbConn, companyId, mobileNos);
		} catch (SQLException e) {
			throw new RuntimeException("db fail", e);
		} finally {
			DBUtil.closeQuietly(dbConn);
		}
	}
	
	public Map<String, Long> getUserIdByRawIdUnique(long companyId, Collection<String> rawIds) {
		Connection dbConn = null;
		try {
			dbConn = hikariDataSource.getConnection();
			return UserBaseDB.getUserIdByRawIdUnique(dbConn, companyId, rawIds);
		} catch (SQLException e) {
			throw new RuntimeException("db fail", e);
		} finally {
			DBUtil.closeQuietly(dbConn);
		}
	}
	
	public Map<Long, UserProtos.UserBase> getUserBase(long companyId, Collection<Long> userIds, @Nullable Set<UserProtos.UserBase.State> userStateSet) {
		if (userIds.isEmpty() || (userStateSet != null && userStateSet.isEmpty())) {
			return Collections.emptyMap();
		}
		
		Map<Long, UserProtos.UserBase> userBaseMap = new HashMap<Long, UserProtos.UserBase>(userIds.size());
		
		Set<Long> noCacheUserIdSet = new TreeSet<Long>();
		Jedis jedis = jedisPool.getResource();
		try {
			userBaseMap.putAll(UserBaseCache.getUserBase(jedis, companyId, userIds, noCacheUserIdSet));
		} finally {
			jedis.close();
		}
		
		if (!noCacheUserIdSet.isEmpty()) {
			
			Map<Long, UserProtos.UserBase> noCacheUserBaseMap;
			Connection dbConn = null;
			try {
				dbConn = hikariDataSource.getConnection();
				noCacheUserBaseMap = UserBaseDB.getUserBase(dbConn, companyId, noCacheUserIdSet);
			} catch (SQLException e) {
				throw new RuntimeException("db fail", e);
			} finally {
				DBUtil.closeQuietly(dbConn);
			}
			
			jedis = jedisPool.getResource();
			try {
				UserBaseCache.setUserBase(jedis, companyId, noCacheUserIdSet, noCacheUserBaseMap);
			} finally {
				jedis.close();
			}
			
			userBaseMap.putAll(noCacheUserBaseMap);
		}
		
		if (userStateSet != null) {
			Iterator<Map.Entry<Long, UserProtos.UserBase>> it = userBaseMap.entrySet().iterator();
			while (it.hasNext()) {
				Map.Entry<Long, UserProtos.UserBase> entry = it.next();
				if (!userStateSet.contains(entry.getValue().getState())) {
					it.remove();
				}
			}
		}
		
		return userBaseMap;
	}
	
	public UpdateUserAvatarResponse updateUserAvatar(RequestHead head, UpdateUserAvatarRequest request, @Nullable Set<UserProtos.UserBase.State> userStateSet) {
		final long companyId = head.getSession().getCompanyId();
		final long userId = head.getSession().getUserId();
		final String avatar = request.getAvatar().trim();
		
		if (avatar.isEmpty() || avatar.length() > 191) {
			return UpdateUserAvatarResponse.newBuilder()
					.setResult(UpdateUserAvatarResponse.Result.FAIL_AVATAR_INVALID)
					.setFailText("头像错误")
					.build();
		}
		
		UserProtos.UserBase userBase = this.getUserBase(companyId, Collections.singleton(userId), userStateSet).get(userId);
		if (userBase == null) {
			throw new RuntimeException("cannot find request user !");
		}
		
		if (avatar.equals(userBase.getAvatar())) {
			return UpdateUserAvatarResponse.newBuilder()
					.setResult(UpdateUserAvatarResponse.Result.SUCC)
					.build();
		}
		
		Map<Long, UserProtos.UserBase> userBaseMap;
		Connection dbConn = null;
		try {
			dbConn = this.hikariDataSource.getConnection();
			UserBaseDB.updateUserAvatar(dbConn, companyId, userId, avatar);
			userBaseMap = UserBaseDB.getUserBase(dbConn, companyId, Collections.singleton(userId));
		} catch (SQLException e) {
			throw new RuntimeException("db fail", e);
		} finally {
			DBUtil.closeQuietly(dbConn);
		}
 		
		// 更新 cache
		Jedis jedis = this.jedisPool.getResource();
		try {
			UserBaseCache.setUserBase(jedis, companyId, Collections.singleton(userId), userBaseMap);
		} finally {
			jedis.close();
		}
		
		return UpdateUserAvatarResponse.newBuilder()
				.setResult(UpdateUserAvatarResponse.Result.SUCC)
				.build();
	}
	
	public UpdateUserSignatureResponse updateUserSignature(RequestHead head, UpdateUserSignatureRequest request, @Nullable Set<UserProtos.UserBase.State> userStateSet) {
		final long companyId = head.getSession().getCompanyId();
		final long userId = head.getSession().getUserId();
		final String signature = request.getSignature().trim();
		
		if (signature.length() > 70) {
			return UpdateUserSignatureResponse.newBuilder()
					.setResult(UpdateUserSignatureResponse.Result.FAIL_SIGNATURE_INVALID)
					.setFailText("个性签名最多70字")
					.build();
		}
		
		UserProtos.UserBase userBase = this.getUserBase(companyId, Collections.singleton(userId), userStateSet).get(userId);
		if (userBase == null) {
			throw new RuntimeException("cannot find request user !");
		}
		
		if (signature.equals(userBase.getSignature())) {
			return UpdateUserSignatureResponse.newBuilder()
					.setResult(UpdateUserSignatureResponse.Result.SUCC)
					.build();
		}
		
		Map<Long, UserProtos.UserBase> userBaseMap;
		Connection dbConn = null;
		try {
			dbConn = this.hikariDataSource.getConnection();
			UserBaseDB.updateUserSignature(dbConn, companyId, userId, signature);
			userBaseMap = UserBaseDB.getUserBase(dbConn, companyId, Collections.singleton(userId));
		} catch (SQLException e) {
			throw new RuntimeException("db fail", e);
		} finally {
			DBUtil.closeQuietly(dbConn);
		}
 		
		// 更新 cache
		Jedis jedis = this.jedisPool.getResource();
		try {
			UserBaseCache.setUserBase(jedis, companyId, Collections.singleton(userId), userBaseMap);
		} finally {
			jedis.close();
		}
		
		return UpdateUserSignatureResponse.newBuilder()
				.setResult(UpdateUserSignatureResponse.Result.SUCC)
				.build();
	}

	public UpdateUserInterestResponse updateUserInterest(RequestHead head, UpdateUserInterestRequest request, @Nullable Set<UserProtos.UserBase.State> userStateSet) {
		final long companyId = head.getSession().getCompanyId();
		final long userId = head.getSession().getUserId();
		final String interest = request.getInterest().trim();
		
		if (interest.length() > 70) {
			return UpdateUserInterestResponse.newBuilder()
					.setResult(UpdateUserInterestResponse.Result.FAIL_INTEREST_INVALID)
					.setFailText("兴趣爱好最多70字")
					.build();
		}
		
		UserProtos.UserBase userBase = this.getUserBase(companyId, Collections.singleton(userId), userStateSet).get(userId);
		if (userBase == null) {
			throw new RuntimeException("cannot find request user !");
		}
		
		if (interest.equals(userBase.getInterest())) {
			return UpdateUserInterestResponse.newBuilder()
					.setResult(UpdateUserInterestResponse.Result.SUCC)
					.build();
		}
		
		Map<Long, UserProtos.UserBase> userBaseMap;
		Connection dbConn = null;
		try {
			dbConn = this.hikariDataSource.getConnection();
			UserBaseDB.updateUserInterest(dbConn, companyId, userId, interest);
			userBaseMap = UserBaseDB.getUserBase(dbConn, companyId, Collections.singleton(userId));
		} catch (SQLException e) {
			throw new RuntimeException("db fail", e);
		} finally {
			DBUtil.closeQuietly(dbConn);
		}
 		
		// 更新 cache
		Jedis jedis = this.jedisPool.getResource();
		try {
			UserBaseCache.setUserBase(jedis, companyId, Collections.singleton(userId), userBaseMap);
		} finally {
			jedis.close();
		}
		
		return UpdateUserInterestResponse.newBuilder()
				.setResult(UpdateUserInterestResponse.Result.SUCC)
				.build();
	}
	
	private static final Pattern NUMBER_PATTERN = Pattern.compile("^\\d{4,11}$");
	
	public List<Long> searchUserId(long companyId, String keyword0, int size, @Nullable Set<UserProtos.UserBase.State> userStateSet) {
		final String keyword = keyword0.trim();
		if (keyword.isEmpty()) {
			return Collections.emptyList();
		}
		
		Connection dbConn = null;
		try {
			dbConn = this.hikariDataSource.getConnection();
			List<Long> userIdList = UserBaseDB.searchUserIdByName(dbConn, companyId, keyword, size, userStateSet);
			
			if (userIdList.size() < size && NUMBER_PATTERN.matcher(keyword).find()) {
				
				List<Long> list = UserBaseDB.searchUserIdByMobileNoUnique(dbConn, companyId, keyword, size);
				
				for (Long userId : list) {
					if (!userIdList.contains(userId)) {
						userIdList.add(userId);
					}
				}
			}
			
			return userIdList;
		} catch (SQLException e) {
			throw new RuntimeException("db fail", e);
		} finally {
			DBUtil.closeQuietly(dbConn);
		}
	}
	
	/* admin */

	public List<Long> getUserIdList(long companyId, @Nullable Long lastUserId, int size, @Nullable Set<UserProtos.UserBase.State> userStateSet) {
		if (size <= 0 || (userStateSet != null && userStateSet.isEmpty())) {
			return Collections.emptyList();
		}
		
		Connection dbConn = null;
		try {
			dbConn = this.hikariDataSource.getConnection();
			return UserBaseDB.getUserIdList(dbConn, companyId, lastUserId, size, userStateSet);
		} catch (SQLException e) {
			throw new RuntimeException("db fail", e);
		} finally {
			DBUtil.closeQuietly(dbConn);
		}
	}
	
	public CreateUserResponse checkCreateUserRequest(long companyId, CreateUserRequest request) {	
		// check arg
		if (request.getRawId().isEmpty()) {
			return CreateUserResponse.newBuilder()
					.setResult(CreateUserResponse.Result.FAIL_RAW_ID_INVALID)
					.setFailText("人员员工id为空")
					.build();
		}
		if (!UserUtil.isValidRawId(request.getRawId())) {
			return CreateUserResponse.newBuilder()
					.setResult(CreateUserResponse.Result.FAIL_RAW_ID_INVALID)
					.setFailText("人员员工id错误: " + UserUtil.tipsRawId())
					.build();
		}
		if (request.getUserName().isEmpty()) {
			return CreateUserResponse.newBuilder()
					.setResult(CreateUserResponse.Result.FAIL_NAME_INVALID)
					.setFailText("人员名称为空")
					.build();
		}
		if (!UserUtil.isValidUserName(request.getUserName())) {
			return CreateUserResponse.newBuilder()
					.setResult(CreateUserResponse.Result.FAIL_NAME_INVALID)
					.setFailText("人员名称错误: " + UserUtil.tipsUserName())
					.build();
		}
		if (request.getMobileNoCount() <= 0) {
			return CreateUserResponse.newBuilder()
					.setResult(CreateUserResponse.Result.FAIL_MOBILE_NO_INVALID)
					.setFailText("人员手机号为空")
					.build();
		}
		for (String mobileNo : request.getMobileNoList()) {
			if (!MobileNoUtil.isValid(mobileNo)) {
				return CreateUserResponse.newBuilder()
						.setResult(CreateUserResponse.Result.FAIL_MOBILE_NO_INVALID)
						.setFailText("人员手机号格式错误: " + mobileNo)
						.build();
			}
		}
		
		Map<String, Long> mobileNoToUserIdMap = this.getUserIdByMobileNoUnique(companyId, request.getMobileNoList());
		if (!mobileNoToUserIdMap.isEmpty()) {
			return CreateUserResponse.newBuilder()
					.setResult(CreateUserResponse.Result.FAIL_MOBILE_NO_INVALID)
					.setFailText("人员手机号已被使用: " + mobileNoToUserIdMap)
					.build();
		}
		
		if (request.hasEmail() && !EmailUtil.isValid(request.getEmail())) {
			return CreateUserResponse.newBuilder()
					.setResult(CreateUserResponse.Result.FAIL_EMAIL_INVALID)
					.setFailText("人员邮箱格式错误")
					.build();
		}
		
		final String rawId = request.getRawId();
		Long userId = this.getUserIdByRawIdUnique(companyId, Collections.singleton(rawId)).get(rawId);
		if (userId != null) {
			return CreateUserResponse.newBuilder()
					.setResult(CreateUserResponse.Result.FAIL_RAW_ID_INVALID)
					.setFailText("人员员工id错误: 此id已存在")
					.build();
		}
		
		return null;
	}
	
	public List<Long> createUserBase(long companyId, List<UserProtos.UserBase> userBaseList) {
		if (userBaseList.isEmpty()) {
			return Collections.emptyList();
		}
		
		List<Long> userIdList;
		Map<Long, UserProtos.UserBase> userBaseMap;
		Connection dbConn = null;
		try {
			dbConn = this.hikariDataSource.getConnection();
			userIdList = UserBaseDB.insertUserBase(dbConn, companyId, userBaseList);
			userBaseMap = UserBaseDB.getUserBase(dbConn, companyId, userIdList);
		} catch (SQLException e) {
			throw new RuntimeException("db fail", e);
		} finally {
			DBUtil.closeQuietly(dbConn);
		}

		Jedis jedis = this.jedisPool.getResource();
		try {
			UserBaseCache.setUserBase(jedis, companyId, userIdList, userBaseMap);
		} finally {
			jedis.close();
		}
		
		return userIdList;
	}

	public UpdateUserResponse checkUpdateUserRequest(long companyId, UpdateUserRequest request) {
		// check argument
		if (request.getUserName().isEmpty()) {
			return UpdateUserResponse.newBuilder()
					.setResult(UpdateUserResponse.Result.FAIL_NAME_INVALID)
					.setFailText("人员名称为空")
					.build();
		}
		if (!UserUtil.isValidUserName(request.getUserName())) {
			return UpdateUserResponse.newBuilder()
					.setResult(UpdateUserResponse.Result.FAIL_NAME_INVALID)
					.setFailText("人员名称错误: " + UserUtil.tipsUserName())
					.build();
		}
		if (request.getMobileNoCount() <= 0) {
			return UpdateUserResponse.newBuilder()
					.setResult(UpdateUserResponse.Result.FAIL_MOBILE_NO_INVALID)
					.setFailText("人员手机号为空")
					.build();
		}
		
		for (String mobileNo : request.getMobileNoList()) {
			if (!MobileNoUtil.isValid(mobileNo)) {
				return UpdateUserResponse.newBuilder()
						.setResult(UpdateUserResponse.Result.FAIL_MOBILE_NO_INVALID)
						.setFailText("人员手机号格式错误: " + mobileNo)
						.build();
			}
		}
		
		Map<String, Long> mobileNoToUserIdMap = this.getUserIdByMobileNoUnique(companyId, request.getMobileNoList());
		
		Map<String, Long> conflictMap = new HashMap<String, Long>();
		for (Entry<String, Long> entry : mobileNoToUserIdMap.entrySet()) {
			if (entry.getValue().longValue() != request.getUserId()) {
				conflictMap.put(entry.getKey(), entry.getValue());
			}
		}
		
		if (!conflictMap.isEmpty()) {
			return UpdateUserResponse.newBuilder()
					.setResult(UpdateUserResponse.Result.FAIL_MOBILE_NO_INVALID)
					.setFailText("人员手机号已被使用: " + conflictMap)
					.build();
		}
		
		if (request.hasEmail() && !EmailUtil.isValid(request.getEmail())) {
			return UpdateUserResponse.newBuilder()
					.setResult(UpdateUserResponse.Result.FAIL_EMAIL_INVALID)
					.setFailText("人员邮箱格式错误")
					.build();
		}
		
		return null;
	}
	
	public void updateUserBase(
			AdminHead head,
			Map<Long, UserProtos.UserBase> oldUserBaseMap, 
			Map<Long, UserProtos.UserBase> newUserBaseMap
			) {
		
		// 必须保证key完全一样
		if (!oldUserBaseMap.keySet().equals(newUserBaseMap.keySet())) {
			throw new IllegalArgumentException("update key not equals");
		}
		
		if (!head.hasCompanyId()) {
			throw new RuntimeException("company_id 参数未填");
		}
		
		final long companyId = head.getCompanyId();
		
		Map<Long, UserProtos.UserBase> userBaseMap;
		Connection dbConn = null;
		try {
			dbConn = this.hikariDataSource.getConnection();
			UserBaseDB.updateUserBase(dbConn, companyId, oldUserBaseMap, newUserBaseMap);
			userBaseMap = UserBaseDB.getUserBase(dbConn, companyId, oldUserBaseMap.keySet());
		} catch (SQLException e) {
			throw new RuntimeException("db fail", e);
		} finally {
			DBUtil.closeQuietly(dbConn);
		}

		Jedis jedis = this.jedisPool.getResource();
		try {
			UserBaseCache.setUserBase(jedis, companyId, oldUserBaseMap.keySet(), userBaseMap);
		} finally {
			jedis.close();
		}
		
		Set<String> sendSmsMobileNoSet = new TreeSet<String>();
		for (Entry<Long, UserProtos.UserBase> entry : oldUserBaseMap.entrySet()) {
			UserProtos.UserBase oldUserBase = entry.getValue();
			UserProtos.UserBase newUserBase = newUserBaseMap.get(entry.getKey());
			
			if (oldUserBase.getState() == UserProtos.UserBase.State.APPROVE 
					&& newUserBase.getState() == UserProtos.UserBase.State.NORMAL) {
				sendSmsMobileNoSet.addAll(newUserBase.getMobileNoList());
			}
		}
		
		final ProfileManager.Profile profile = this.profileManager.getProfile(head, "user:");
		final String smsText = profile.get(USER_APPROVE_SUCC_SMS_TEXT);
		
		if (smsText != null && !smsText.trim().isEmpty() && !sendSmsMobileNoSet.isEmpty()) {
			this.externalService.sendSms(head, SendSmsRequest.newBuilder()
					.addAllMobileNo(sendSmsMobileNoSet)
					.setSmsText(smsText.trim())
					.build());
		}
	}
	
	public void deleteUserBase(long companyId, Collection<Long> userIds, @Nullable Long updateAdminId, @Nullable Integer updateTime) {
		if (userIds.isEmpty()) {
			return;
		}
		
		Connection dbConn = null;
		try {
			dbConn = this.hikariDataSource.getConnection();
			UserBaseDB.deleteUserBase(dbConn, companyId, userIds, updateAdminId, updateTime);
		} catch (SQLException e) {
			throw new RuntimeException("db fail", e);
		} finally {
			DBUtil.closeQuietly(dbConn);
		}

		Jedis jedis = this.jedisPool.getResource();
		try {
			UserBaseCache.delUserBase(jedis, companyId, userIds);
		} finally {
			jedis.close();
		}
	}
	
	public DataPage<Long> getUserIdPage(long companyId, int start, int length, @Nullable Set<UserProtos.UserBase.State> userStateSet, 
			@Nullable Boolean isExpert, 
			@Nullable Collection<Integer> teamIds, 
			@Nullable Integer positionId, 
			@Nullable String keyword,
			@Nullable String mobileNo
			) {
		Connection dbConn = null;
		try {
			dbConn = this.hikariDataSource.getConnection();
			return UserBaseDB.getUserIdPage(dbConn, companyId, start, length, userStateSet, isExpert, teamIds, positionId, keyword, mobileNo);
		} catch (SQLException e) {
			throw new RuntimeException("db fail", e);
		} finally {
			DBUtil.closeQuietly(dbConn);
		}
	}
	
	public SetExpertResponse setExpert(AdminHead head, SetExpertRequest request, @Nullable Set<UserProtos.UserBase.State> userStateSet) {
		if (!head.hasCompanyId()) {
			return SetExpertResponse.newBuilder()
					.setResult(SetExpertResponse.Result.FAIL_UNKNOWN)
					.setFailText("company_id 参数未填")
					.build();
		}
		
		final long companyId = head.getCompanyId();
		final long userId = request.getUserId();
		
		UserProtos.UserBase userBase = this.getUserBase(companyId, Collections.singleton(userId), userStateSet).get(userId);
		if (userBase == null) {
			return SetExpertResponse.newBuilder()
					.setResult(SetExpertResponse.Result.FAIL_USER_NOT_EXIST)
					.setFailText("没有找到该人员的信息")
					.build();
		}
		
		if (userBase.hasIsExpert() && userBase.getIsExpert() == request.getIsExpert()) {
			return SetExpertResponse.newBuilder()
					.setResult(SetExpertResponse.Result.SUCC)
					.build();
		}
		
		Map<Long, UserProtos.UserBase> userBaseMap;
		Connection dbConn = null;
		try {
			dbConn = this.hikariDataSource.getConnection();
			UserBaseDB.setUserExpert(dbConn, companyId, userId, request.getIsExpert());
			
			userBaseMap = UserBaseDB.getUserBase(dbConn, companyId, Collections.singleton(userId));
		} catch (SQLException e) {
			throw new RuntimeException("db fail", e);
		} finally {
			DBUtil.closeQuietly(dbConn);
		}
		
		Jedis jedis = this.jedisPool.getResource();
		try {
			UserBaseCache.setUserBase(jedis, companyId, Collections.singleton(userId), userBaseMap);
		} finally {
			jedis.close();
		}
		
		return SetExpertResponse.newBuilder()
				.setResult(SetExpertResponse.Result.SUCC)
				.build();
	}
	
	public SetStateResponse setState(AdminHead head, SetStateRequest request, @Nullable Set<UserProtos.UserBase.State> userStateSet) {
		if (!head.hasCompanyId()) {
			return SetStateResponse.newBuilder()
					.setResult(SetStateResponse.Result.FAIL_UNKNOWN)
					.setFailText("company_id 参数未填")
					.build();
		}
		
		final long companyId = head.getCompanyId();
		final Set<Long> userIdSet = new TreeSet<Long>(request.getUserIdList());
		
		Map<Long, UserProtos.UserBase> userBaseMap = this.getUserBase(companyId, userIdSet, userStateSet);

		Set<Long> updateUserIdSet = new TreeSet<Long>();
		for (UserProtos.UserBase userBase : userBaseMap.values()) {
			if (!userBase.hasState() || userBase.getState() != request.getState()) {
				updateUserIdSet.add(userBase.getUserId());
			}
		}
		
		if (updateUserIdSet.isEmpty()) {
			return SetStateResponse.newBuilder()
					.setResult(SetStateResponse.Result.SUCC)
					.build();
		}
		
		Map<Long, UserProtos.UserBase> updateUserBaseMap;
		Connection dbConn = null;
		try {
			dbConn = this.hikariDataSource.getConnection();
			UserBaseDB.setUserState(dbConn, companyId, updateUserIdSet, request.getState());
			
			updateUserBaseMap = UserBaseDB.getUserBase(dbConn, companyId, updateUserIdSet);
		} catch (SQLException e) {
			throw new RuntimeException("db fail", e);
		} finally {
			DBUtil.closeQuietly(dbConn);
		}
		
		Jedis jedis = this.jedisPool.getResource();
		try {
			UserBaseCache.setUserBase(jedis, companyId, updateUserIdSet, updateUserBaseMap);
		} finally {
			jedis.close();
		}
		
		final ProfileManager.Profile profile = this.profileManager.getProfile(head, "user:");
		final String smsText = profile.get(USER_APPROVE_SUCC_SMS_TEXT);
		
		if (smsText != null && !smsText.trim().isEmpty()) {
			Set<String> sendSmsMobileNoSet = new TreeSet<String>();
			
			for (Long updateUserId : updateUserIdSet) {
				UserProtos.UserBase userBase = userBaseMap.get(updateUserId);
				
				if (userBase != null 
						&& userBase.getState() == UserProtos.UserBase.State.APPROVE 
						&& request.getState() == UserProtos.UserBase.State.NORMAL
						) {
					sendSmsMobileNoSet.addAll(userBase.getMobileNoList());
				}
			}
			
			if (!sendSmsMobileNoSet.isEmpty()) {
				this.externalService.sendSms(head, SendSmsRequest.newBuilder()
						.addAllMobileNo(sendSmsMobileNoSet)
						.setSmsText(smsText.trim())
						.build());
			}
		}
		
		return SetStateResponse.newBuilder()
				.setResult(SetStateResponse.Result.SUCC)
				.build();
	}
	
}
