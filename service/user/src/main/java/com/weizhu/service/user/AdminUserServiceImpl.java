package com.weizhu.service.user;

import java.sql.Connection;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.annotation.Nullable;

import java.util.Queue;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableSet;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.inject.Inject;
import com.weizhu.common.db.DBUtil;
import com.weizhu.common.service.ServiceUtil;
import com.weizhu.common.utils.DataPage;
import com.weizhu.common.utils.EmailUtil;
import com.weizhu.common.utils.MobileNoUtil;
import com.weizhu.proto.AdminProtos.AdminHead;
import com.weizhu.proto.AdminUserProtos.CreateLevelRequest;
import com.weizhu.proto.AdminUserProtos.CreateLevelResponse;
import com.weizhu.proto.AdminUserProtos.CreatePositionRequest;
import com.weizhu.proto.AdminUserProtos.CreatePositionResponse;
import com.weizhu.proto.AdminUserProtos.CreateTeamRequest;
import com.weizhu.proto.AdminUserProtos.CreateTeamResponse;
import com.weizhu.proto.AdminUserProtos.CreateUserRequest;
import com.weizhu.proto.AdminUserProtos.CreateUserResponse;
import com.weizhu.proto.AdminUserProtos.DeleteLevelRequest;
import com.weizhu.proto.AdminUserProtos.DeleteLevelResponse;
import com.weizhu.proto.AdminUserProtos.DeletePositionRequest;
import com.weizhu.proto.AdminUserProtos.DeletePositionResponse;
import com.weizhu.proto.AdminUserProtos.DeleteTeamRequest;
import com.weizhu.proto.AdminUserProtos.DeleteTeamResponse;
import com.weizhu.proto.AdminUserProtos.DeleteUserRequest;
import com.weizhu.proto.AdminUserProtos.DeleteUserResponse;
import com.weizhu.proto.AdminUserProtos.GetAbilityTagUserIdRequest;
import com.weizhu.proto.AdminUserProtos.GetAbilityTagUserIdResponse;
import com.weizhu.proto.AdminUserProtos.GetAllTeamResponse;
import com.weizhu.proto.AdminUserProtos.GetLevelResponse;
import com.weizhu.proto.AdminUserProtos.GetPositionResponse;
import com.weizhu.proto.AdminUserProtos.GetTeamAllUserIdRequest;
import com.weizhu.proto.AdminUserProtos.GetTeamAllUserIdResponse;
import com.weizhu.proto.AdminUserProtos.GetTeamByIdRequest;
import com.weizhu.proto.AdminUserProtos.GetTeamByIdResponse;
import com.weizhu.proto.AdminUserProtos.GetTeamRequest;
import com.weizhu.proto.AdminUserProtos.GetTeamResponse;
import com.weizhu.proto.AdminUserProtos.GetUserAbilityTagRequest;
import com.weizhu.proto.AdminUserProtos.GetUserAbilityTagResponse;
import com.weizhu.proto.AdminUserProtos.GetUserByIdRequest;
import com.weizhu.proto.AdminUserProtos.GetUserByIdResponse;
import com.weizhu.proto.AdminUserProtos.GetUserByMobileNoUniqueRequest;
import com.weizhu.proto.AdminUserProtos.GetUserByMobileNoUniqueResponse;
import com.weizhu.proto.AdminUserProtos.GetUserExtendsNameResponse;
import com.weizhu.proto.AdminUserProtos.GetUserListRequest;
import com.weizhu.proto.AdminUserProtos.GetUserListResponse;
import com.weizhu.proto.AdminUserProtos.ImportUserRequest;
import com.weizhu.proto.AdminUserProtos.ImportUserResponse;
import com.weizhu.proto.AdminUserProtos.RegisterUserRequest;
import com.weizhu.proto.AdminUserProtos.RegisterUserResponse;
import com.weizhu.proto.AdminUserProtos.SetExpertRequest;
import com.weizhu.proto.AdminUserProtos.SetExpertResponse;
import com.weizhu.proto.AdminUserProtos.SetStateRequest;
import com.weizhu.proto.AdminUserProtos.SetStateResponse;
import com.weizhu.proto.AdminUserProtos.SetUserAbilityTagRequest;
import com.weizhu.proto.AdminUserProtos.SetUserAbilityTagResponse;
import com.weizhu.proto.AdminUserProtos.UpdateLevelRequest;
import com.weizhu.proto.AdminUserProtos.UpdateLevelResponse;
import com.weizhu.proto.AdminUserProtos.UpdatePositionRequest;
import com.weizhu.proto.AdminUserProtos.UpdatePositionResponse;
import com.weizhu.proto.AdminUserProtos.UpdateTeamRequest;
import com.weizhu.proto.AdminUserProtos.UpdateTeamResponse;
import com.weizhu.proto.AdminUserProtos.UpdateUserRequest;
import com.weizhu.proto.AdminUserProtos.UpdateUserResponse;
import com.weizhu.proto.AdminUserService;
import com.weizhu.proto.PushProtos.PushUserDisableRequest;
import com.weizhu.proto.PushService;
import com.weizhu.proto.UserProtos;
import com.weizhu.proto.PushProtos.PushUserDeleteRequest;
import com.weizhu.proto.WeizhuProtos.AnonymousHead;
import com.weizhu.proto.WeizhuProtos.EmptyRequest;
import com.weizhu.proto.WeizhuProtos.EmptyResponse;
import com.weizhu.proto.WeizhuProtos.RequestHead;
import com.weizhu.proto.WeizhuProtos.SystemHead;
import com.weizhu.service.user.abilitytag.AbilityTagManager;
import com.weizhu.service.user.base.UserBaseManager;
import com.weizhu.service.user.exts.UserExtendsManager;
import com.weizhu.service.user.level.LevelCache;
import com.weizhu.service.user.level.LevelDB;
import com.weizhu.service.user.level.LevelManager;
import com.weizhu.service.user.position.PositionCache;
import com.weizhu.service.user.position.PositionDB;
import com.weizhu.service.user.position.PositionManager;
import com.weizhu.service.user.team.TeamDB;
import com.weizhu.service.user.team.TeamManager;
import com.zaxxer.hikari.HikariDataSource;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

public class AdminUserServiceImpl implements AdminUserService {

	@SuppressWarnings("unused")
	private static final Logger logger = LoggerFactory.getLogger(AdminUserServiceImpl.class);
	
	private final HikariDataSource hikariDataSource;
	private final JedisPool jedisPool;
	
	private final ImportUserManager importUserManager;
	private final TeamManager teamManager;
	private final PositionManager positionManager;
	private final UserBaseManager userBaseManager;
	private final UserExtendsManager userExtendsManager;
	private final LevelManager levelManager;
	private final AbilityTagManager abilityTagManager;
	
	private final PushService pushService;
	
	@Inject
	public AdminUserServiceImpl(
			HikariDataSource hikariDataSource, JedisPool jedisPool, 
			ImportUserManager importUserManager, 
			TeamManager teamManager,
			PositionManager positionManager,
			UserBaseManager userBaseManager,
			UserExtendsManager userExtendsManager,
			LevelManager levelManager,
			AbilityTagManager abilityTagManager,
			PushService pushService
			) {
		this.hikariDataSource = hikariDataSource;
		this.jedisPool = jedisPool;
		
		this.importUserManager = importUserManager;
		this.teamManager = teamManager;
		this.positionManager = positionManager;
		this.userBaseManager = userBaseManager;
		this.userExtendsManager = userExtendsManager;
		this.levelManager = levelManager;
		this.abilityTagManager = abilityTagManager;
		
		this.pushService = pushService;
	}
	
	private static final ImmutableSet<UserProtos.UserBase.State> ADMIN_USER_STATE_SET = 
			ImmutableSet.of(UserProtos.UserBase.State.NORMAL, UserProtos.UserBase.State.DISABLE, UserProtos.UserBase.State.APPROVE);
	
	@Override
	public ListenableFuture<ImportUserResponse> importUser(AdminHead head, ImportUserRequest request) {
		return Futures.immediateFuture(this.importUserManager.importUser(head, request));
	}

	@Override
	public ListenableFuture<CreateUserResponse> createUser(AdminHead head, CreateUserRequest request) {
		if (!head.hasCompanyId()) {
			return Futures.immediateFuture(CreateUserResponse.newBuilder()
					.setResult(CreateUserResponse.Result.FAIL_UNKNOWN)
					.setFailText("company_id 参数未填")
					.build());
		}
		final long companyId = head.getCompanyId();
		final long adminId = head.getSession().getAdminId();
		CreateUserResponse failResponse = this.userBaseManager.checkCreateUserRequest(companyId, request);
		if (failResponse != null) {
			return Futures.immediateFuture(failResponse);
		}
		
		if (request.hasLevelId()) {
			final Map<Integer, UserProtos.Level> levelMap = this.levelManager.getAllLevel(companyId);
			if (!levelMap.containsKey(request.getLevelId())) {
				return Futures.immediateFuture(CreateUserResponse.newBuilder()
						.setResult(CreateUserResponse.Result.FAIL_LEVEL_INVALID)
						.setFailText("人员职级不存在:" + request.getLevelId())
						.build());
			}
		}
		
		if (request.getUserTeamCount() > 0) {
			final TeamManager.TeamData teamData = this.teamManager.getTeamData(companyId);
			final Map<Integer, UserProtos.Position> positionMap = this.positionManager.getAllPosition(companyId);
			
			for (UserProtos.UserTeam userTeam : request.getUserTeamList()) {
				if (!teamData.teamInfoMap().containsKey(userTeam.getTeamId())) {
					return Futures.immediateFuture(CreateUserResponse.newBuilder()
							.setResult(CreateUserResponse.Result.FAIL_TEAM_INVALID)
							.setFailText("人员部门不存在:" + userTeam.getTeamId())
							.build());
				}
				
				if (userTeam.hasPositionId() && !positionMap.containsKey(userTeam.getPositionId())) {
					return Futures.immediateFuture(CreateUserResponse.newBuilder()
							.setResult(CreateUserResponse.Result.FAIL_POSITION_INVALID)
							.setFailText("人员职位不存在:" + userTeam.getPositionId())
							.build());
				}
			}
		}
		
		// create user base
		
		UserProtos.UserBase.Builder userBaseBuilder = UserProtos.UserBase.newBuilder();
		userBaseBuilder.setUserId(0L);
		userBaseBuilder.setUserName(request.getUserName());
		userBaseBuilder.setRawId(request.getRawId());
		
		if (request.hasGender()) {
			userBaseBuilder.setGender(request.getGender());
		}
		
		userBaseBuilder.addAllMobileNo(request.getMobileNoList());
		userBaseBuilder.addAllPhoneNo(request.getPhoneNoList());
		
		if (request.hasEmail()) {
			userBaseBuilder.setEmail(request.getEmail());
		}
		
		if (request.hasLevelId()) {
			userBaseBuilder.setLevelId(request.getLevelId());	
		}
		
		userBaseBuilder.setState(UserProtos.UserBase.State.NORMAL);
		userBaseBuilder.setCreateAdminId(adminId);
		userBaseBuilder.setCreateTime((int) (System.currentTimeMillis() / 1000L));
		
		final long userId = this.userBaseManager.createUserBase(companyId, Collections.singletonList(userBaseBuilder.build())).get(0);

		// update user team
		List<UserProtos.UserTeam> oldUserTeamList = Collections.emptyList();
		
		List<UserProtos.UserTeam> newUserTeamList = new ArrayList<UserProtos.UserTeam>(request.getUserTeamCount());
		UserProtos.UserTeam.Builder tmpUserTeamBuilder = UserProtos.UserTeam.newBuilder();
		for (UserProtos.UserTeam userTeam : request.getUserTeamList()) {
			newUserTeamList.add(tmpUserTeamBuilder.clear()
					.mergeFrom(userTeam)
					.setUserId(userId)
					.build());
		}

		this.teamManager.updateUserTeam(
				companyId,
				Collections.<Long, List<UserProtos.UserTeam>>singletonMap(userId, oldUserTeamList), 
				Collections.<Long, List<UserProtos.UserTeam>>singletonMap(userId, newUserTeamList)
				);
		
		return Futures.immediateFuture(CreateUserResponse.newBuilder()
				.setResult(CreateUserResponse.Result.SUCC)
				.setUserId(userId)
				.build());
	}

	@Override
	public ListenableFuture<UpdateUserResponse> updateUser(AdminHead head, UpdateUserRequest request) {
		if (!head.hasCompanyId()) {
			return Futures.immediateFuture(UpdateUserResponse.newBuilder()
					.setResult(UpdateUserResponse.Result.FAIL_UNKNOWN)
					.setFailText("company_id 参数未填")
					.build());
		}
		final long companyId = head.getCompanyId();
		final long adminId = head.getSession().getAdminId();
		UpdateUserResponse failResponse = this.userBaseManager.checkUpdateUserRequest(companyId, request);
		if (failResponse != null) {
			return Futures.immediateFuture(failResponse);
		}
		
		if (request.hasLevelId()) {
			final Map<Integer, UserProtos.Level> levelMap = this.levelManager.getAllLevel(companyId);
			if (!levelMap.containsKey(request.getLevelId())) {
				return Futures.immediateFuture(UpdateUserResponse.newBuilder()
						.setResult(UpdateUserResponse.Result.FAIL_LEVEL_INVALID)
						.setFailText("人员职级不存在:" + request.getLevelId())
						.build());
			}
		}
		
		final TeamManager.TeamData teamData = this.teamManager.getTeamData(companyId);
		
		if (request.getUserTeamCount() > 0) {
			final Map<Integer, UserProtos.Position> positionMap = this.positionManager.getAllPosition(companyId);
			
			for (UserProtos.UserTeam userTeam : request.getUserTeamList()) {
				if (!teamData.teamInfoMap().containsKey(userTeam.getTeamId())) {
					return Futures.immediateFuture(UpdateUserResponse.newBuilder()
							.setResult(UpdateUserResponse.Result.FAIL_TEAM_INVALID)
							.setFailText("人员部门不存在:" + userTeam.getTeamId())
							.build());
				}
				
				if (userTeam.hasPositionId() && !positionMap.containsKey(userTeam.getPositionId())) {
					return Futures.immediateFuture(UpdateUserResponse.newBuilder()
							.setResult(UpdateUserResponse.Result.FAIL_POSITION_INVALID)
							.setFailText("人员职位不存在:" + userTeam.getPositionId())
							.build());
				}
			}
		}
		
		// update user
		
		final UserProtos.UserBase oldUserBase = this.userBaseManager.getUserBase(companyId, Collections.singleton(request.getUserId()), ADMIN_USER_STATE_SET).get(request.getUserId());
		if (oldUserBase == null) {
			return Futures.immediateFuture(UpdateUserResponse.newBuilder()
					.setResult(UpdateUserResponse.Result.FAIL_USER_NOT_EXIST)
					.setFailText("人员不存在")
					.build());
		}
		
		UserProtos.UserBase.Builder userBaseBuilder = oldUserBase.toBuilder();
		userBaseBuilder.setUserName(request.getUserName());
		if (request.hasGender()) {
			userBaseBuilder.setGender(request.getGender());
		} else {
			userBaseBuilder.clearGender();
		}
		userBaseBuilder.clearMobileNo().addAllMobileNo(request.getMobileNoList());
		userBaseBuilder.clearPhoneNo().addAllPhoneNo(request.getPhoneNoList());
		if (request.hasEmail()) {
			userBaseBuilder.setEmail(request.getEmail());
		} else {
			userBaseBuilder.clearEmail();
		}
		if (request.hasIsExpert()) {
			userBaseBuilder.setIsExpert(request.getIsExpert());
		} else {
			userBaseBuilder.clearIsExpert();
		}
		if (request.hasLevelId()) {
			userBaseBuilder.setLevelId(request.getLevelId());
		} else {
			userBaseBuilder.clearLevelId();
		}
		
		if (request.hasState()) {
			userBaseBuilder.setState(request.getState());
		}
		
		userBaseBuilder.setUpdateAdminId(adminId);
		userBaseBuilder.setUpdateTime((int) (System.currentTimeMillis() / 1000L));
		
		final UserProtos.UserBase newUserBase = userBaseBuilder.build();
		
		this.userBaseManager.updateUserBase(
				head,
				Collections.<Long, UserProtos.UserBase>singletonMap(request.getUserId(), oldUserBase), 
				Collections.<Long, UserProtos.UserBase>singletonMap(request.getUserId(), newUserBase)
				);
		
		// update user team
		
		List<UserProtos.UserTeam> oldUserTeamList = teamData.userTeamMap().get(request.getUserId());
		if (oldUserTeamList == null) {
			oldUserTeamList = Collections.emptyList();
		}
		
		List<UserProtos.UserTeam> newUserTeamList = request.getUserTeamList();
		
		this.teamManager.updateUserTeam(
				companyId,
				Collections.<Long, List<UserProtos.UserTeam>>singletonMap(request.getUserId(), oldUserTeamList), 
				Collections.<Long, List<UserProtos.UserTeam>>singletonMap(request.getUserId(), newUserTeamList)
				);
		
		if (request.hasState() && (request.getState() != UserProtos.UserBase.State.NORMAL)) {
			pushService.pushUserDisable(head, PushUserDisableRequest.newBuilder().addUserId(request.getUserId()).build());
		}
		
		return Futures.immediateFuture(UpdateUserResponse.newBuilder()
				.setResult(UpdateUserResponse.Result.SUCC)
				.build());
	}

	@Override
	public ListenableFuture<DeleteUserResponse> deleteUser(AdminHead head, DeleteUserRequest request) {
		if (!head.hasCompanyId()) {
			return Futures.immediateFuture(DeleteUserResponse.newBuilder()
					.setResult(DeleteUserResponse.Result.FAIL_UNKNOWN)
					.setFailText("company_id 参数未填")
					.build());
		}
		final long companyId = head.getCompanyId();
		final long adminId = head.getSession().getAdminId();
		this.userBaseManager.deleteUserBase(companyId, request.getUserIdList(), adminId, (int) (System.currentTimeMillis() / 1000L));
		this.teamManager.deleteUserTeam(companyId, request.getUserIdList());
		
		if (request.getUserIdCount() > 0) {
			pushService.pushUserDelete(head, PushUserDeleteRequest.newBuilder().addAllUserId(request.getUserIdList()).build());
		}
		
		return Futures.immediateFuture(DeleteUserResponse.newBuilder()
				.setResult(DeleteUserResponse.Result.SUCC)
				.build());
	}

	@Override
	public ListenableFuture<GetUserListResponse> getUserList(AdminHead head, GetUserListRequest request) {
		if (!head.hasCompanyId()) {
			return Futures.immediateFuture(GetUserListResponse.newBuilder()
					.setTotalSize(0)
					.setFilteredSize(0)
					.build());
		}
		return Futures.immediateFuture(this.doGetUserList(head.getCompanyId(), request, ADMIN_USER_STATE_SET));
	}
	
	@Override
	public ListenableFuture<GetUserListResponse> getUserList(SystemHead head, GetUserListRequest request) {
		if (!head.hasCompanyId()) {
			return Futures.immediateFuture(GetUserListResponse.newBuilder()
					.setTotalSize(0)
					.setFilteredSize(0)
					.build());
		}
		return Futures.immediateFuture(this.doGetUserList(head.getCompanyId(), request, null));
	}
	
	private GetUserListResponse doGetUserList(long companyId, GetUserListRequest request, @Nullable Set<UserProtos.UserBase.State> userStateSet) {
		final TeamManager.TeamData teamData = this.teamManager.getTeamData(companyId);
		
		final Boolean isExpert = request.hasIsExpert() ? request.getIsExpert() : null;
		final Set<Integer> teamIdSet;
		if (request.hasTeamId()) {
			teamIdSet = new TreeSet<Integer>();
			
			Queue<Integer> queue = new LinkedList<Integer>();
			queue.add(request.getTeamId());
			while (!queue.isEmpty()) {
				Integer teamId = queue.poll();
				
				TeamManager.TeamInfo teamInfo = teamData.teamInfoMap().get(teamId);
				if (teamInfo != null && !teamIdSet.contains(teamId)){
					teamIdSet.add(teamId);
					queue.addAll(teamInfo.subTeamIdList());
				}
			}

		} else {
			teamIdSet = null;
		}
		final Integer positionId = request.hasPositionId() ? request.getPositionId() : null;
		final String keyword = request.hasKeyword() && !request.getKeyword().trim().isEmpty() ? request.getKeyword().trim() : null;
		final String mobileNo = request.hasMobileNo() && !request.getMobileNo().trim().isEmpty() ? request.getMobileNo().trim() : null;
		
		final DataPage<Long> userIdPage = this.userBaseManager.getUserIdPage(companyId, request.getStart(), request.getLength(), userStateSet, isExpert, teamIdSet, positionId, keyword, mobileNo);
		final Map<Long, UserProtos.User> userMap = UserUtil.doGetUser(userBaseManager, null, userExtendsManager, companyId, null, userIdPage.dataList(), userStateSet, teamData);
		
		GetUserListResponse.Builder responseBuilder = GetUserListResponse.newBuilder();
		for (Long userId : userIdPage.dataList()) {
			UserProtos.User user = userMap.get(userId);
			if (user != null) {
				responseBuilder.addUser(user);
			}
		}
		
		return responseBuilder
				.setTotalSize(userIdPage.totalSize())
				.setFilteredSize(userIdPage.filteredSize())
				.addAllRefTeam(UserUtil.doGetRefTeam(userMap, Collections.<Integer>emptyList(),teamData).values())
				.addAllRefPosition(UserUtil.doGetRefPosition(positionManager, companyId, userMap, Collections.<Integer>emptyList()).values())
				.addAllRefLevel(UserUtil.doGetRefLevel(levelManager, companyId, userMap, Collections.<Integer>emptyList()).values())
				.build();
	}
	
	@Override
	public ListenableFuture<GetUserByIdResponse> getUserById(AdminHead head, GetUserByIdRequest request) {
		if (!head.hasCompanyId() || request.getUserIdCount() <= 0) {
			return Futures.immediateFuture(GetUserByIdResponse.newBuilder().build());
		}
		
		final long companyId = head.getCompanyId();
		final TeamManager.TeamData teamData = this.teamManager.getTeamData(companyId);
		
		final Map<Long, UserProtos.User> userMap = UserUtil.doGetUser(userBaseManager, null, userExtendsManager, companyId, null, request.getUserIdList(), ADMIN_USER_STATE_SET, teamData);
		
		return Futures.immediateFuture(GetUserByIdResponse.newBuilder()
				.addAllUser(userMap.values())
				.addAllRefTeam(UserUtil.doGetRefTeam(userMap, Collections.<Integer>emptyList(),teamData).values())
				.addAllRefPosition(UserUtil.doGetRefPosition(positionManager, companyId, userMap, Collections.<Integer>emptyList()).values())
				.addAllRefLevel(UserUtil.doGetRefLevel(levelManager, companyId, userMap, Collections.<Integer>emptyList()).values())
				.build());
	}
	
	@Override
	public ListenableFuture<GetUserByIdResponse> getUserById(SystemHead head, GetUserByIdRequest request) {
		if (!head.hasCompanyId() || request.getUserIdCount() <= 0) {
			return Futures.immediateFuture(GetUserByIdResponse.newBuilder().build());
		}
		
		final long companyId = head.getCompanyId();
		final TeamManager.TeamData teamData = this.teamManager.getTeamData(companyId);
		
		final Map<Long, UserProtos.User> userMap = UserUtil.doGetUser(userBaseManager, null, userExtendsManager, companyId, null, request.getUserIdList(), null, teamData);
		
		return Futures.immediateFuture(GetUserByIdResponse.newBuilder()
				.addAllUser(userMap.values())
				.addAllRefTeam(UserUtil.doGetRefTeam(userMap, Collections.<Integer>emptyList(),teamData).values())
				.addAllRefPosition(UserUtil.doGetRefPosition(positionManager, companyId, userMap, Collections.<Integer>emptyList()).values())
				.addAllRefLevel(UserUtil.doGetRefLevel(levelManager, companyId, userMap, Collections.<Integer>emptyList()).values())
				.build());
	}
	
	@Override
	public ListenableFuture<GetUserByIdResponse> getUserById(RequestHead head, GetUserByIdRequest request) {
		final long companyId = head.getSession().getCompanyId();
		final TeamManager.TeamData teamData = this.teamManager.getTeamData(companyId);
		
		final Map<Long, UserProtos.User> userMap = UserUtil.doGetUser(userBaseManager, null, userExtendsManager, companyId, null, request.getUserIdList(), ADMIN_USER_STATE_SET, teamData);
		
		return Futures.immediateFuture(GetUserByIdResponse.newBuilder()
				.addAllUser(userMap.values())
				.addAllRefTeam(UserUtil.doGetRefTeam(userMap, Collections.<Integer>emptyList(),teamData).values())
				.addAllRefPosition(UserUtil.doGetRefPosition(positionManager, companyId, userMap, Collections.<Integer>emptyList()).values())
				.addAllRefLevel(UserUtil.doGetRefLevel(levelManager, companyId, userMap, Collections.<Integer>emptyList()).values())
				.build());
	}
	
	@Override
	public ListenableFuture<GetUserExtendsNameResponse> getUserExtendsName(AdminHead head, EmptyRequest request) {
		if (!head.hasCompanyId()) {
			return Futures.immediateFuture(GetUserExtendsNameResponse.newBuilder().build());
		}
		return Futures.immediateFuture(GetUserExtendsNameResponse.newBuilder()
				.addAllExtendsName(this.userExtendsManager.getUserExtendsName(head.getCompanyId()))
				.build());
	}
	
	@Override
	public ListenableFuture<GetUserByMobileNoUniqueResponse> getUserByMobileNoUnique(AdminHead head, GetUserByMobileNoUniqueRequest request) {
		if (!head.hasCompanyId() || request.getMobileNoCount() < 0) {
			return Futures.immediateFuture(GetUserByMobileNoUniqueResponse.newBuilder().build());
		}
		
		Set<String> mobileNoSet = new TreeSet<String>();
		for (String mobileNo : request.getMobileNoList()) {
			if (MobileNoUtil.isValid(mobileNo)) {
				mobileNoSet.add(mobileNo);
			}
		}
		if (mobileNoSet.isEmpty()) {
			return Futures.immediateFuture(GetUserByMobileNoUniqueResponse.newBuilder().build());
		}
		
		final long companyId = head.getCompanyId();
		Map<String, Long> mobileNoToUserIdMap = this.userBaseManager.getUserIdByMobileNoUnique(companyId, mobileNoSet);
		
		final TeamManager.TeamData teamData = this.teamManager.getTeamData(companyId);
		final Map<Long, UserProtos.User> userMap = UserUtil.doGetUser(userBaseManager, null, userExtendsManager, companyId, null, mobileNoToUserIdMap.values(), ADMIN_USER_STATE_SET, teamData);
		
		return Futures.immediateFuture(GetUserByMobileNoUniqueResponse.newBuilder()
				.addAllUser(userMap.values())
				.addAllRefTeam(UserUtil.doGetRefTeam(userMap, Collections.<Integer>emptyList(),teamData).values())
				.addAllRefPosition(UserUtil.doGetRefPosition(positionManager, companyId, userMap, Collections.<Integer>emptyList()).values())
				.addAllRefLevel(UserUtil.doGetRefLevel(levelManager, companyId, userMap, Collections.<Integer>emptyList()).values())
				.build());
	}

	@Override
	public ListenableFuture<SetExpertResponse> setExpert(AdminHead head, SetExpertRequest request) {
		return Futures.immediateFuture(this.userBaseManager.setExpert(head, request, ADMIN_USER_STATE_SET));
	}
	
	@Override
	public ListenableFuture<SetStateResponse> setState(AdminHead head, SetStateRequest request) {
		SetStateResponse response = this.userBaseManager.setState(head, request, ADMIN_USER_STATE_SET);
		
		if (request.getState() != UserProtos.UserBase.State.NORMAL
				&& response.getResult() == SetStateResponse.Result.SUCC
				&& request.getUserIdCount() > 0
				) {
			pushService.pushUserDisable(head, PushUserDisableRequest.newBuilder().addAllUserId(request.getUserIdList()).build());
		}
		
		return Futures.immediateFuture(response);
	}

	@Override
	public ListenableFuture<CreatePositionResponse> createPosition(AdminHead head, CreatePositionRequest request) {
		return Futures.immediateFuture(this.positionManager.createPosition(head, request));
	}

	@Override
	public ListenableFuture<UpdatePositionResponse> updatePosition(AdminHead head, UpdatePositionRequest request) {
		return Futures.immediateFuture(this.positionManager.updatePosition(head, request));
	}
	
	@Override
	public ListenableFuture<DeletePositionResponse> deletePosition(AdminHead head, DeletePositionRequest request) {
		return Futures.immediateFuture(this.positionManager.deletePosition(head, request));
	}

	@Override
	public ListenableFuture<GetPositionResponse> getPosition(AdminHead head, EmptyRequest request) {
		if (!head.hasCompanyId()) {
			return Futures.immediateFuture(GetPositionResponse.newBuilder().build());
		}
		return Futures.immediateFuture(GetPositionResponse.newBuilder()
				.addAllPosition(this.positionManager.getAllPosition(head.getCompanyId()).values())
				.build());
	}

	@Override
	public ListenableFuture<CreateLevelResponse> createLevel(AdminHead head, CreateLevelRequest request) {
		return Futures.immediateFuture(this.levelManager.createLevel(head, request));
	}

	@Override
	public ListenableFuture<UpdateLevelResponse> updateLevel(AdminHead head, UpdateLevelRequest request) {
		return Futures.immediateFuture(this.levelManager.updateLevel(head, request));
	}

	@Override
	public ListenableFuture<DeleteLevelResponse> deleteLevel(AdminHead head, DeleteLevelRequest request) {
		return Futures.immediateFuture(this.levelManager.deleteLevel(head, request));
	}

	@Override
	public ListenableFuture<GetLevelResponse> getLevel(AdminHead head, EmptyRequest request) {
		if (!head.hasCompanyId()) {
			return Futures.immediateFuture(GetLevelResponse.newBuilder().build());
		}
		return Futures.immediateFuture(GetLevelResponse.newBuilder()
				.addAllLevel(this.levelManager.getAllLevel(head.getCompanyId()).values())
				.build());
	}

	@Override
	public ListenableFuture<CreateTeamResponse> createTeam(AdminHead head, CreateTeamRequest request) {
		return Futures.immediateFuture(this.teamManager.createTeam(head, request));
	}

	@Override
	public ListenableFuture<UpdateTeamResponse> updateTeam(AdminHead head, UpdateTeamRequest request) {
		return Futures.immediateFuture(this.teamManager.updateTeam(head, request));
	}

	@Override
	public ListenableFuture<DeleteTeamResponse> deleteTeam(AdminHead head, DeleteTeamRequest request) {
		return Futures.immediateFuture(this.teamManager.deleteTeam(head, request));
	}

	@Override
	public ListenableFuture<GetTeamResponse> getTeam(AdminHead head, GetTeamRequest request) {
		if (!head.hasCompanyId()) {
			return Futures.immediateFuture(GetTeamResponse.newBuilder().build());
		}
		
		final long companyId = head.getCompanyId();
		final TeamManager.TeamData teamData = this.teamManager.getTeamData(companyId);
		
		GetTeamResponse.Builder responseBuilder = GetTeamResponse.newBuilder();
		
		Set<Integer> refTeamIdSet = new TreeSet<Integer>();
		Set<Long> refUserIdSet = new TreeSet<Long>();
		Set<Integer> refPositionIdSet = new TreeSet<Integer>();
		
		if (!request.hasTeamId()) {
			// root team
			responseBuilder.addAllSubTeamId(teamData.rootTeamIdList());
			refTeamIdSet.addAll(teamData.rootTeamIdList());
		} else {
			TeamManager.TeamInfo teamInfo = teamData.teamInfoMap().get(request.getTeamId());
			if (teamInfo != null) {
				responseBuilder.addAllSubUserTeam(teamInfo.subUserTeamList());
				responseBuilder.addAllSubTeamId(teamInfo.subTeamIdList());
				
				refTeamIdSet.addAll(teamInfo.subTeamIdList());
				
				for (UserProtos.UserTeam userTeam : teamInfo.subUserTeamList()) {
					refTeamIdSet.add(userTeam.getTeamId());
					refUserIdSet.add(userTeam.getUserId());
					if (userTeam.hasPositionId()) {
						refPositionIdSet.add(userTeam.getPositionId());
					}
				}
			}
		}
		
		for (Integer subTeamId : responseBuilder.getSubTeamIdList()) {
			TeamManager.TeamInfo subTeamInfo = teamData.teamInfoMap().get(subTeamId);
			if (subTeamInfo != null && !subTeamInfo.subTeamIdList().isEmpty()) {
				responseBuilder.addSubTeamIdHasSub(subTeamId);
			}
		}
		
		final Map<Long, UserProtos.User> refUserMap = UserUtil.doGetUser(userBaseManager, null, userExtendsManager, companyId, null, refUserIdSet, ADMIN_USER_STATE_SET, teamData);
		
		responseBuilder.addAllRefUser(refUserMap.values());
		responseBuilder.addAllRefTeam(UserUtil.doGetRefTeam(refUserMap, refTeamIdSet, teamData).values());
		responseBuilder.addAllRefPosition(UserUtil.doGetRefPosition(positionManager, companyId, refUserMap, refPositionIdSet).values());
		responseBuilder.addAllRefLevel(UserUtil.doGetRefLevel(levelManager, companyId, refUserMap, Collections.<Integer>emptyList()).values());
		return Futures.immediateFuture(responseBuilder.build());
	}
	
	@Override
	public ListenableFuture<GetTeamByIdResponse> getTeamById(AdminHead head, GetTeamByIdRequest request) {
		if (!head.hasCompanyId() || request.getTeamIdCount() <= 0) {
			return Futures.immediateFuture(GetTeamByIdResponse.newBuilder().build());
		}
		
		final long companyId = head.getCompanyId();
		final TeamManager.TeamData teamData = this.teamManager.getTeamData(companyId);
		
		Map<Integer, UserProtos.Team> teamMap = new LinkedHashMap<Integer, UserProtos.Team>();
		for (Integer teamId : request.getTeamIdList()) {
			TeamManager.TeamInfo teamInfo = teamData.teamInfoMap().get(teamId);
			if (teamInfo != null) {
				teamMap.put(teamId, teamInfo.team());
			}
		}
		
		return Futures.immediateFuture(GetTeamByIdResponse.newBuilder()
				.addAllTeam(teamMap.values())
				.build());
	}
	
	@Override
	public ListenableFuture<GetTeamAllUserIdResponse> getTeamAllUserId(AdminHead head, GetTeamAllUserIdRequest request) {
		if (!head.hasCompanyId()) {
			return Futures.immediateFuture(GetTeamAllUserIdResponse.newBuilder().build());
		}
		final long companyId = head.getCompanyId();
		final TeamManager.TeamData teamData = this.teamManager.getTeamData(companyId);
		
		if (request.getTeamIdCount() <= 0) {
			return Futures.immediateFuture(GetTeamAllUserIdResponse.newBuilder()
					.addAllUserId(teamData.noTeamUserIdList())
					.build());
		} else {
			Set<Long> userIdSet = new TreeSet<Long>();
			
			Queue<Integer> queue = new LinkedList<Integer>();
			queue.addAll(request.getTeamIdList());
			
			while (!queue.isEmpty()) {
				Integer teamId = queue.poll();
				
				TeamManager.TeamInfo teamInfo = teamData.teamInfoMap().get(teamId);
				if (teamInfo != null) {
					for (UserProtos.UserTeam userTeam : teamInfo.subUserTeamList()) {
						userIdSet.add(userTeam.getUserId());
					}
					
					queue.addAll(teamInfo.subTeamIdList());
				}
			}
			
			return Futures.immediateFuture(GetTeamAllUserIdResponse.newBuilder()
					.addAllUserId(userIdSet)
					.build());
		}
	}
	
	@Override
	public ListenableFuture<GetAllTeamResponse> getAllTeam(AdminHead head, EmptyRequest request) {
		if (!head.hasCompanyId()) {
			return Futures.immediateFuture(GetAllTeamResponse.newBuilder().build());
		}
		final long companyId = head.getCompanyId();
		final TeamManager.TeamData teamData = this.teamManager.getTeamData(companyId);
		
		GetAllTeamResponse.Builder responseBuilder = GetAllTeamResponse.newBuilder();
		
		Queue<Integer> queue = new LinkedList<Integer>();
		queue.addAll(teamData.rootTeamIdList());
		while (!queue.isEmpty()) {
			Integer teamId = queue.poll();
			TeamManager.TeamInfo teamInfo = teamData.teamInfoMap().get(teamId);
			if (teamInfo != null) {
				responseBuilder.addTeam(teamInfo.team());
				queue.addAll(teamInfo.subTeamIdList());
			}
		}
		return Futures.immediateFuture(responseBuilder.build());
	}

	@Override
	public ListenableFuture<EmptyResponse> reloadTeam(AdminHead head, EmptyRequest request) {
		this.teamManager.reloadTeam(head);
		return ServiceUtil.EMPTY_RESPONSE_IMMEDIATE_FUTURE;
	}
	
	@Override
	public ListenableFuture<GetUserAbilityTagResponse> getUserAbilityTag(AdminHead head, GetUserAbilityTagRequest request) {
		if (!head.hasCompanyId()) {
			return Futures.immediateFuture(GetUserAbilityTagResponse.newBuilder().build());
		}
		Map<Long, List<UserProtos.UserAbilityTag>> tagMap = this.abilityTagManager.getUserAbilityTag(head.getCompanyId(), request.getUserIdList(), null);
		
		GetUserAbilityTagResponse.Builder responseBuilder = GetUserAbilityTagResponse.newBuilder();
		for (List<UserProtos.UserAbilityTag> tagList : tagMap.values()) {
			responseBuilder.addAllAbilityTag(tagList);
		}
		return Futures.immediateFuture(responseBuilder.build());
	}

	@Override
	public ListenableFuture<SetUserAbilityTagResponse> setUserAbilityTag(AdminHead head, SetUserAbilityTagRequest request) {
		return Futures.immediateFuture(this.abilityTagManager.setUserAbilityTag(head, request));
	}
	
	@Override
	public ListenableFuture<GetAbilityTagUserIdResponse> getAbilityTagUserId(AdminHead head, GetAbilityTagUserIdRequest request) {
		if (!head.hasCompanyId()) {
			return Futures.immediateFuture(GetAbilityTagUserIdResponse.newBuilder().build());
		}
		
		final Set<String> tagNameSet = new TreeSet<String>(request.getTagNameList());
		final Boolean isExpert = request.hasIsExpert() ? request.getIsExpert() : null;
		
		List<Long> userIdList = this.abilityTagManager.getAbilityTagUserId(head.getCompanyId(), tagNameSet, isExpert);
		
		return Futures.immediateFuture(GetAbilityTagUserIdResponse.newBuilder()
				.addAllUserId(userIdList)
				.build());
	}
	
	@Override
	public ListenableFuture<GetUserByMobileNoUniqueResponse> getUserByMobileNoUnique(AnonymousHead head, GetUserByMobileNoUniqueRequest request) {
		if (!head.hasCompanyId() || request.getMobileNoCount() <= 0) {
			return Futures.immediateFuture(GetUserByMobileNoUniqueResponse.newBuilder().build());
		}
		
		Set<String> mobileNoSet = new TreeSet<String>();
		for (String mobileNo : request.getMobileNoList()) {
			if (MobileNoUtil.isValid(mobileNo)) {
				mobileNoSet.add(mobileNo);
			}
		}
		if (mobileNoSet.isEmpty()) {
			return Futures.immediateFuture(GetUserByMobileNoUniqueResponse.newBuilder().build());
		}
		
		final long companyId = head.getCompanyId();
		Map<String, Long> mobileNoToUserIdMap = this.userBaseManager.getUserIdByMobileNoUnique(companyId, mobileNoSet);
		
		final TeamManager.TeamData teamData = this.teamManager.getTeamData(companyId);
		final Map<Long, UserProtos.User> userMap = UserUtil.doGetUser(userBaseManager, null, userExtendsManager, companyId, null, mobileNoToUserIdMap.values(), ADMIN_USER_STATE_SET, teamData);
		
		return Futures.immediateFuture(GetUserByMobileNoUniqueResponse.newBuilder()
				.addAllUser(userMap.values())
				.addAllRefTeam(UserUtil.doGetRefTeam(userMap, Collections.<Integer>emptyList(),teamData).values())
				.addAllRefPosition(UserUtil.doGetRefPosition(positionManager, companyId, userMap, Collections.<Integer>emptyList()).values())
				.addAllRefLevel(UserUtil.doGetRefLevel(levelManager, companyId, userMap, Collections.<Integer>emptyList()).values())
				.build());
	}
	
	@Override
	public ListenableFuture<RegisterUserResponse> registerUser(AnonymousHead head, RegisterUserRequest request) {
		if (!head.hasCompanyId()) {
			return Futures.immediateFuture(RegisterUserResponse.newBuilder()
					.setResult(RegisterUserResponse.Result.FAIL_UNKNOWN)
					.setFailText("未找到公司信息")
					.build());
		}
		
		final long companyId = head.getCompanyId();
		
		if (request.getUserName().isEmpty()) {
			return Futures.immediateFuture(RegisterUserResponse.newBuilder()
					.setResult(RegisterUserResponse.Result.FAIL_NAME_INVALID)
					.setFailText("人员名称为空")
					.build());
		}
		if (!UserUtil.isValidUserName(request.getUserName())) {
			return Futures.immediateFuture(RegisterUserResponse.newBuilder()
					.setResult(RegisterUserResponse.Result.FAIL_NAME_INVALID)
					.setFailText("人员名称错误: " + UserUtil.tipsUserName())
					.build());
		}
		
		if (request.hasEmail() && !EmailUtil.isValid(request.getEmail())) {
			return Futures.immediateFuture(RegisterUserResponse.newBuilder()
					.setResult(RegisterUserResponse.Result.FAIL_EMAIL_INVALID)
					.setFailText("人员邮箱格式错误")
					.build());
		}
		
		if (!MobileNoUtil.isValid(request.getMobileNo())) {
			return Futures.immediateFuture(RegisterUserResponse.newBuilder()
					.setResult(RegisterUserResponse.Result.FAIL_MOBILE_NO_INVALID)
					.setFailText("手机号格式错误")
					.build());
		}
		final String mobileNo = MobileNoUtil.adjustMobileNo(request.getMobileNo());
		
		Map<String, Long> mobileNoToUserIdMap = this.userBaseManager.getUserIdByMobileNoUnique(companyId, Collections.singleton(mobileNo));
		if (!mobileNoToUserIdMap.isEmpty()) {
			return Futures.immediateFuture(RegisterUserResponse.newBuilder()
					.setResult(RegisterUserResponse.Result.FAIL_MOBILE_NO_INVALID)
					.setFailText("该手机号已被注册使用")
					.build());
		}
		
		final Map<String, String> extsMap = new TreeMap<String, String>();
		for (int i=0; i<request.getExtsNameCount() && i<request.getExtsValueCount(); ++i) {
			String name = request.getExtsName(i).trim();
			String value = request.getExtsValue(i).trim();
			
			if (!name.isEmpty() && name.length() <= 10 && !value.isEmpty() && value.length() <= 20) {
				extsMap.put(name, value);
			}
		}
		
		if (request.getTeamList().size() > 10) {
			return Futures.immediateFuture(RegisterUserResponse.newBuilder()
					.setResult(RegisterUserResponse.Result.FAIL_TEAM_INVALID)
					.setFailText("注册部门层级太多")
					.build());
		}
		
		final List<String> teamNameList = new ArrayList<String>();
		for (String teamName : request.getTeamList()) {
			if (teamName.isEmpty()) {
				return Futures.immediateFuture(RegisterUserResponse.newBuilder()
						.setResult(RegisterUserResponse.Result.FAIL_TEAM_INVALID)
						.setFailText("注册部门名称为空")
						.build());
			}
			if (teamName.length() > 100) {
				return Futures.immediateFuture(RegisterUserResponse.newBuilder()
						.setResult(RegisterUserResponse.Result.FAIL_TEAM_INVALID)
						.setFailText("注册部门名称太长")
						.build());
			}
			teamNameList.add(teamName);
		}
		
		final String positionName;
		if (!request.hasPosition()) {
			positionName = null;
		} else {
			if (request.getPosition().isEmpty()) {
				return Futures.immediateFuture(RegisterUserResponse.newBuilder()
						.setResult(RegisterUserResponse.Result.FAIL_POSITION_INVALID)
						.setFailText("注册职位名称为空")
						.build());
			}
			if (request.getPosition().length() > 100) {
				return Futures.immediateFuture(RegisterUserResponse.newBuilder()
						.setResult(RegisterUserResponse.Result.FAIL_POSITION_INVALID)
						.setFailText("注册职位名称太长")
						.build());
			}
			positionName = request.getPosition();
		}
		
		final String levelName;
		if (!request.hasLevel()) {
			levelName = null;
		} else {
			if (request.getLevel().isEmpty()) {
				return Futures.immediateFuture(RegisterUserResponse.newBuilder()
						.setResult(RegisterUserResponse.Result.FAIL_LEVEL_INVALID)
						.setFailText("注册职级名称为空")
						.build());
			}
			if (request.getLevel().length() > 100) {
				return Futures.immediateFuture(RegisterUserResponse.newBuilder()
						.setResult(RegisterUserResponse.Result.FAIL_LEVEL_INVALID)
						.setFailText("注册职级名称太长")
						.build());
			}
			levelName = request.getLevel();
		}
		
		final String phoneNo;
		if (!request.hasPhoneNo()) {
			phoneNo = null;
		} else {
			if (request.getPhoneNo().length() > 191) {
				return Futures.immediateFuture(RegisterUserResponse.newBuilder()
						.setResult(RegisterUserResponse.Result.FAIL_UNKNOWN)
						.setFailText("座机号太长")
						.build());
			}
			phoneNo = request.getPhoneNo();
		}
		
		// insert team postion level
		
		final int now = (int) (System.currentTimeMillis() / 1000L);
		final TeamManager.TeamData teamData = this.teamManager.getTeamData(companyId);
		final Map<Integer, UserProtos.Position> positionMap = this.positionManager.getAllPosition(companyId);
		final Map<Integer, UserProtos.Level> levelMap = this.levelManager.getAllLevel(companyId);
		
		final Integer teamId;
		final Integer positionId;
		final Integer levelId;
		
		boolean isUpdateDBTeam = false;
		boolean isUpdateDBPosition = false;
		boolean isUpdateDBLevel = false;
		
		Connection dbConn = null;
		try {
			dbConn = this.hikariDataSource.getConnection();
			
			if (teamNameList.isEmpty()) {
				teamId = null;
			} else {
				List<Integer> teamIdList = teamData.rootTeamIdList();
				Integer tmpTeamId = null;
				
				UserProtos.Team.Builder teamBuilder = UserProtos.Team.newBuilder();
				
				for (String teamName : teamNameList) {
					
					TeamManager.TeamInfo teamInfo = null;
					for (Integer id : teamIdList) {
						TeamManager.TeamInfo info = teamData.teamInfoMap().get(id);
						if (info != null && teamName.equals(info.team().getTeamName())) {
							teamInfo = info;
							break;
						}
					}
					
					if (teamInfo != null) {
						teamIdList = teamInfo.subTeamIdList();
						tmpTeamId = teamInfo.team().getTeamId();
					} else {
						teamIdList = Collections.emptyList();
						
						teamBuilder.clear();
						teamBuilder.setTeamId(0);
						teamBuilder.setTeamName(teamName);
						teamBuilder.setState(UserProtos.State.NORMAL);
						teamBuilder.setCreateTime(now);
						if (tmpTeamId != null) {
							teamBuilder.setParentTeamId(tmpTeamId);
						}
						
						tmpTeamId = TeamDB.insertTeam(dbConn, companyId, Collections.<UserProtos.Team>singletonList(teamBuilder.build())).get(0);
						
						isUpdateDBTeam = true;
					}
				}
				
				teamId = tmpTeamId;
			}
			
			if (positionName == null) {
				positionId = null;
			} else {
				Integer tmpPositionId = null;
				for (UserProtos.Position position : positionMap.values()) {
					if (position.getPositionName().equals(positionName)) {
						tmpPositionId = position.getPositionId();
						break;
					}
				}
				
				if (tmpPositionId != null) {
					positionId = tmpPositionId;
				} else {
					positionId = PositionDB.insertPosition(dbConn, companyId, 
							Collections.singletonList(UserProtos.Position.newBuilder()
									.setPositionId(0)
									.setPositionName(positionName)
									.setPositionDesc("")
									.setState(UserProtos.State.NORMAL)
									.setCreateTime(now)
									.build())).get(0);
					isUpdateDBPosition = true;
				}
			}
			
			if (levelName == null) {
				levelId = null;
			} else {
				Integer tmpLevelId = null;
				for (UserProtos.Level level : levelMap.values()) {
					if (level.getLevelName().equals(levelName)) {
						tmpLevelId = level.getLevelId();
						break;
					}
				}
				if (tmpLevelId != null) {
					levelId = tmpLevelId;
				} else {
					levelId = LevelDB.insertLevel(dbConn, companyId, 
							Collections.singletonList(UserProtos.Level.newBuilder()
									.setLevelId(0)
									.setLevelName(levelName)
									.setState(UserProtos.State.NORMAL)
									.setCreateTime(now)
									.build())).get(0);
					isUpdateDBLevel = true;
				}
			}
		} catch (SQLException e) { 
			throw new RuntimeException("register user db fail", e);
		} finally {
			DBUtil.closeQuietly(dbConn);
		}
		
		if (isUpdateDBTeam || isUpdateDBPosition || isUpdateDBLevel) {
			Jedis jedis = this.jedisPool.getResource();
			try {
				if (isUpdateDBTeam) {
					// notify reload team data
					jedis.publish("__default_task_loader__", "user:team:load@" + companyId);
				}
				if (isUpdateDBPosition) {
					PositionCache.delAllPosition(jedis, companyId);
				}
				if (isUpdateDBLevel) {
					LevelCache.delAllLevel(jedis, companyId);
				}
			} finally {
				jedis.close();
			}
		}
		
		UserProtos.UserBase.Builder userBaseBuilder = UserProtos.UserBase.newBuilder();
		userBaseBuilder.setUserId(0L);
		userBaseBuilder.setUserName(request.getUserName());
		userBaseBuilder.setRawId("register_" + mobileNo + "_" + (new SimpleDateFormat("yyyyMMddHHmmss").format(new Date())));
		
		if (request.hasGender()) {
			userBaseBuilder.setGender(request.getGender());
		}
		
		userBaseBuilder.addMobileNo(mobileNo);
		
		if (request.hasEmail()) {
			userBaseBuilder.setEmail(request.getEmail());
		}
		
		if (levelId != null) {
			userBaseBuilder.setLevelId(levelId);
		}
		if (request.hasPhoneNo()) {
			userBaseBuilder.addPhoneNo(phoneNo);
		}
		
		if (request.hasState()) {
			userBaseBuilder.setState(request.getState());
		} else {
			userBaseBuilder.setState(UserProtos.UserBase.State.APPROVE);
		}
		userBaseBuilder.setCreateTime((int) (System.currentTimeMillis() / 1000L));
		
		final long userId = this.userBaseManager.createUserBase(companyId, Collections.singletonList(userBaseBuilder.build())).get(0);

		if (teamId != null) {
			// update user team
			UserProtos.UserTeam.Builder userTeamBuilder = UserProtos.UserTeam.newBuilder();
			userTeamBuilder.setTeamId(teamId);
			userTeamBuilder.setUserId(userId);
			if (positionId != null) {
				userTeamBuilder.setPositionId(positionId);
			}

			this.teamManager.updateUserTeam(
					companyId,
					Collections.<Long, List<UserProtos.UserTeam>>singletonMap(userId, Collections.<UserProtos.UserTeam>emptyList()), 
					Collections.<Long, List<UserProtos.UserTeam>>singletonMap(userId, Collections.<UserProtos.UserTeam>singletonList(userTeamBuilder.build()))
					);
		}
		
		if (!extsMap.isEmpty()) {
			
			List<UserProtos.UserExtends> extsList = new ArrayList<UserProtos.UserExtends>();
			UserProtos.UserExtends.Builder tmpBuilder = UserProtos.UserExtends.newBuilder();
			for (Entry<String, String> entry : extsMap.entrySet()) {
				tmpBuilder.clear();
				extsList.add(tmpBuilder.setUserId(userId).setName(entry.getKey()).setValue(entry.getValue()).build());
			}
			
			this.userExtendsManager.updateUserExtends(companyId, 
					Collections.<Long, List<UserProtos.UserExtends>> singletonMap(userId, Collections.<UserProtos.UserExtends>emptyList()), 
					Collections.<Long, List<UserProtos.UserExtends>> singletonMap(userId, extsList)
					);
			
		}
		
		return Futures.immediateFuture(RegisterUserResponse.newBuilder()
				.setResult(RegisterUserResponse.Result.SUCC)
				.setUserId(userId)
				.build());
	}

}
