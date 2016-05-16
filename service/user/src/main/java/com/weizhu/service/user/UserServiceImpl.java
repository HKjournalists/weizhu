package com.weizhu.service.user;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.TreeSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableSet;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.inject.Inject;
import com.weizhu.common.service.ServiceUtil;
import com.weizhu.proto.UserProtos;
import com.weizhu.proto.UserProtos.CreateAbilityTagRequest;
import com.weizhu.proto.UserProtos.CreateAbilityTagResponse;
import com.weizhu.proto.UserProtos.CreateUserExperienceRequest;
import com.weizhu.proto.UserProtos.CreateUserExperienceResponse;
import com.weizhu.proto.UserProtos.DeleteAbilityTagRequest;
import com.weizhu.proto.UserProtos.DeleteUserExperienceRequest;
import com.weizhu.proto.UserProtos.DeleteUserExperienceResponse;
import com.weizhu.proto.UserProtos.GetAbilityTagUserIdRequest;
import com.weizhu.proto.UserProtos.GetAbilityTagUserIdResponse;
import com.weizhu.proto.UserProtos.GetAllLevelRequest;
import com.weizhu.proto.UserProtos.GetAllLevelResponse;
import com.weizhu.proto.UserProtos.GetAllPositionRequest;
import com.weizhu.proto.UserProtos.GetAllPositionResponse;
import com.weizhu.proto.UserProtos.GetMarkStarUserRequest;
import com.weizhu.proto.UserProtos.GetMarkStarUserResponse;
import com.weizhu.proto.UserProtos.GetRandomAbilityTagUserRequest;
import com.weizhu.proto.UserProtos.GetRandomAbilityTagUserResponse;
import com.weizhu.proto.UserProtos.GetTeamRequest;
import com.weizhu.proto.UserProtos.GetTeamResponse;
import com.weizhu.proto.UserProtos.GetUserAbilityTagRequest;
import com.weizhu.proto.UserProtos.GetUserAbilityTagResponse;
import com.weizhu.proto.UserProtos.GetUserBaseByIdRequest;
import com.weizhu.proto.UserProtos.GetUserBaseByIdResponse;
import com.weizhu.proto.UserProtos.GetUserByIdRequest;
import com.weizhu.proto.UserProtos.GetUserByMobileNoRequest;
import com.weizhu.proto.UserProtos.GetUserExperienceRequest;
import com.weizhu.proto.UserProtos.GetUserExperienceResponse;
import com.weizhu.proto.UserProtos.GetUserResponse;
import com.weizhu.proto.UserProtos.MarkUserNameRequest;
import com.weizhu.proto.UserProtos.MarkUserNameResponse;
import com.weizhu.proto.UserProtos.MarkUserStarRequest;
import com.weizhu.proto.UserProtos.MarkUserStarResponse;
import com.weizhu.proto.UserProtos.SearchUserRequest;
import com.weizhu.proto.UserProtos.SearchUserResponse;
import com.weizhu.proto.UserProtos.TagUserAbilityRequest;
import com.weizhu.proto.UserProtos.TagUserAbilityResponse;
import com.weizhu.proto.UserProtos.UpdateUserAvatarRequest;
import com.weizhu.proto.UserProtos.UpdateUserAvatarResponse;
import com.weizhu.proto.UserProtos.UpdateUserExperienceRequest;
import com.weizhu.proto.UserProtos.UpdateUserExperienceResponse;
import com.weizhu.proto.UserProtos.UpdateUserInterestRequest;
import com.weizhu.proto.UserProtos.UpdateUserInterestResponse;
import com.weizhu.proto.UserProtos.UpdateUserSignatureRequest;
import com.weizhu.proto.UserProtos.UpdateUserSignatureResponse;
import com.weizhu.proto.UserService;
import com.weizhu.proto.WeizhuProtos.AnonymousHead;
import com.weizhu.proto.WeizhuProtos.EmptyResponse;
import com.weizhu.proto.WeizhuProtos.RequestHead;
import com.weizhu.service.user.abilitytag.AbilityTagManager;
import com.weizhu.service.user.base.UserBaseManager;
import com.weizhu.service.user.experience.ExperienceManager;
import com.weizhu.service.user.exts.UserExtendsManager;
import com.weizhu.service.user.level.LevelManager;
import com.weizhu.service.user.mark.UserMarkManager;
import com.weizhu.service.user.position.PositionManager;
import com.weizhu.service.user.team.TeamManager;

public class UserServiceImpl implements UserService {

	@SuppressWarnings("unused")
	private static final Logger logger = LoggerFactory.getLogger(UserServiceImpl.class);
	
	private final TeamManager teamManager;
	private final PositionManager positionManager;
	private final UserBaseManager userBaseManager;
	private final UserMarkManager userMarkManager;
	private final UserExtendsManager userExtendsManager;
	private final LevelManager levelManager;
	private final ExperienceManager experienceManager;
	private final AbilityTagManager abilityTagManager;
	
	@Inject
	public UserServiceImpl(
			TeamManager teamManager,
			PositionManager positionManager,
			UserBaseManager userBaseManager,
			UserMarkManager userMarkManager,
			UserExtendsManager userExtendsManager,
			LevelManager levelManager,
			ExperienceManager experienceManager,
			AbilityTagManager abilityTagManager
			) {
		this.teamManager = teamManager;
		this.positionManager = positionManager;
		this.userBaseManager = userBaseManager;
		this.userMarkManager = userMarkManager;
		this.userExtendsManager = userExtendsManager;
		this.levelManager = levelManager;
		this.experienceManager = experienceManager;
		this.abilityTagManager = abilityTagManager;
	}
	
	private static final ImmutableSet<UserProtos.UserBase.State> USER_STATE_SET = 
			ImmutableSet.of(UserProtos.UserBase.State.NORMAL, UserProtos.UserBase.State.DISABLE);
	

	@Override
	public ListenableFuture<GetUserBaseByIdResponse> getUserBaseById(RequestHead head, GetUserBaseByIdRequest request) {
		final long companyId = head.getSession().getCompanyId();
		Map<Long, UserProtos.UserBase> userBaseMap = this.userBaseManager.getUserBase(companyId, request.getUserIdList(), USER_STATE_SET);
		return Futures.immediateFuture(GetUserBaseByIdResponse.newBuilder()
				.addAllUserBase(userBaseMap.values()).build());
	}
	
	private static final ListenableFuture<GetUserResponse> EMPTY_GET_USER_RESPONSE_FUTURE = 
			Futures.immediateFuture(GetUserResponse.newBuilder().build());
	
	@Override
	public ListenableFuture<GetUserResponse> getUserByMobileNo(AnonymousHead head, GetUserByMobileNoRequest request) {
		if (!head.hasCompanyId()) {
			return EMPTY_GET_USER_RESPONSE_FUTURE;
		}
		
		final long companyId = head.getCompanyId();
		final String mobileNo = request.getMobileNo();
		final Long userId = this.userBaseManager.getUserIdByMobileNoUnique(companyId, Collections.singleton(mobileNo)).get(mobileNo);
		
		if (userId == null) {
			return EMPTY_GET_USER_RESPONSE_FUTURE;
		}
		
		final TeamManager.TeamData teamData = this.teamManager.getTeamData(companyId);
		Map<Long, UserProtos.User> userMap = UserUtil.doGetUser(userBaseManager, userMarkManager, userExtendsManager, companyId, null, Collections.singleton(userId), USER_STATE_SET, teamData);
		if (userMap.isEmpty()) {
			return EMPTY_GET_USER_RESPONSE_FUTURE;
		}
		
		return Futures.immediateFuture(GetUserResponse.newBuilder()
				.addAllUser(userMap.values())
				.addAllRefTeam(UserUtil.doGetRefTeam(userMap, Collections.<Integer>emptyList(), teamData).values())
				.addAllRefPosition(UserUtil.doGetRefPosition(positionManager, companyId, userMap, Collections.<Integer>emptyList()).values())
				.addAllRefLevel(UserUtil.doGetRefLevel(levelManager, companyId, userMap, Collections.<Integer>emptyList()).values())
				.build());
	}
	
	@Override
	public ListenableFuture<GetUserResponse> getUserByMobileNo(RequestHead head, GetUserByMobileNoRequest request) {
		final long companyId = head.getSession().getCompanyId();
		final String mobileNo = request.getMobileNo();
		final Long userId = this.userBaseManager.getUserIdByMobileNoUnique(companyId, Collections.singleton(mobileNo)).get(mobileNo);
		
		if (userId == null) {
			return EMPTY_GET_USER_RESPONSE_FUTURE;
		}
		
		final TeamManager.TeamData teamData = this.teamManager.getTeamData(companyId);
		Map<Long, UserProtos.User> userMap = UserUtil.doGetUser(userBaseManager, userMarkManager, userExtendsManager, companyId, head.getSession().getUserId(), Collections.singleton(userId), USER_STATE_SET, teamData);
		UserProtos.User user = userMap.get(userId);
		if (user == null) {
			return EMPTY_GET_USER_RESPONSE_FUTURE;
		}
		
		return Futures.immediateFuture(GetUserResponse.newBuilder()
				.addUser(user)
				.addAllRefTeam(UserUtil.doGetRefTeam(userMap, Collections.<Integer>emptyList(), teamData).values())
				.addAllRefPosition(UserUtil.doGetRefPosition(positionManager, companyId, userMap, Collections.<Integer>emptyList()).values())
				.addAllRefLevel(UserUtil.doGetRefLevel(levelManager, companyId, userMap, Collections.<Integer>emptyList()).values())
				.build());
	}

	@Override
	public ListenableFuture<GetUserResponse> getUserById(RequestHead head, GetUserByIdRequest request) {
		final long companyId = head.getSession().getCompanyId();
		final TeamManager.TeamData teamData = this.teamManager.getTeamData(companyId);
		Map<Long, UserProtos.User> userMap = UserUtil.doGetUser(userBaseManager, userMarkManager, userExtendsManager, companyId, head.getSession().getUserId(), request.getUserIdList(), USER_STATE_SET, teamData);
		if (userMap.isEmpty()) {
			return EMPTY_GET_USER_RESPONSE_FUTURE;
		}
		
		return Futures.immediateFuture(GetUserResponse.newBuilder()
				.addAllUser(userMap.values())
				.addAllRefTeam(UserUtil.doGetRefTeam(userMap, Collections.<Integer>emptyList(),teamData).values())
				.addAllRefPosition(UserUtil.doGetRefPosition(positionManager, companyId, userMap, Collections.<Integer>emptyList()).values())
				.addAllRefLevel(UserUtil.doGetRefLevel(levelManager, companyId, userMap, Collections.<Integer>emptyList()).values())
				.build());
	}

	@Override
	public ListenableFuture<GetTeamResponse> getTeam(RequestHead head, GetTeamRequest request) {
		final long companyId = head.getSession().getCompanyId();
		final TeamManager.TeamData teamData = this.teamManager.getTeamData(companyId);
		
		GetTeamResponse.Builder repsonseBuilder = GetTeamResponse.newBuilder();
		
		Set<Integer> refTeamIdSet = new TreeSet<Integer>();
		Set<Long> refUserIdSet = new TreeSet<Long>();
		Set<Integer> refPositionIdSet = new TreeSet<Integer>();
		
		if (!request.hasTeamId()) {
			// root team
			repsonseBuilder.addAllSubTeamId(teamData.rootTeamIdList());
			refTeamIdSet.addAll(teamData.rootTeamIdList());
		} else {
			TeamManager.TeamInfo teamInfo = teamData.teamInfoMap().get(request.getTeamId());
			if (teamInfo != null) {
				repsonseBuilder.setTeam(teamInfo.team());
				repsonseBuilder.addAllSubUserTeam(teamInfo.subUserTeamList());
				repsonseBuilder.addAllSubTeamId(teamInfo.subTeamIdList());
				
				refTeamIdSet.add(teamInfo.team().getTeamId());
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
		
		final Map<Long, UserProtos.User> refUserMap = UserUtil.doGetUser(userBaseManager, userMarkManager, userExtendsManager, companyId, head.getSession().getUserId(), refUserIdSet, USER_STATE_SET, teamData);
		
		repsonseBuilder.addAllRefUser(refUserMap.values());
		repsonseBuilder.addAllRefTeam(UserUtil.doGetRefTeam(refUserMap, refTeamIdSet, teamData).values());
		repsonseBuilder.addAllRefPosition(UserUtil.doGetRefPosition(positionManager, companyId, refUserMap, refPositionIdSet).values());
		repsonseBuilder.addAllRefLevel(UserUtil.doGetRefLevel(levelManager, companyId, refUserMap, Collections.<Integer>emptyList()).values());
		return Futures.immediateFuture(repsonseBuilder.build());
	}

	@Override
	public ListenableFuture<UpdateUserAvatarResponse> updateUserAvatar(RequestHead head, UpdateUserAvatarRequest request) {
		return Futures.immediateFuture(this.userBaseManager.updateUserAvatar(head, request, USER_STATE_SET));
	}

	@Override
	public ListenableFuture<UpdateUserSignatureResponse> updateUserSignature(RequestHead head, UpdateUserSignatureRequest request) {
		return Futures.immediateFuture(this.userBaseManager.updateUserSignature(head, request, USER_STATE_SET));
	}

	@Override
	public ListenableFuture<UpdateUserInterestResponse> updateUserInterest(RequestHead head, UpdateUserInterestRequest request) {
		return Futures.immediateFuture(this.userBaseManager.updateUserInterest(head, request, USER_STATE_SET));
	}
	
	@Override
	public ListenableFuture<SearchUserResponse> searchUser(RequestHead head, SearchUserRequest request) {
		final long companyId = head.getSession().getCompanyId();
		List<Long> userIdList = this.userBaseManager.searchUserId(companyId, request.getKeyword(), 20, USER_STATE_SET);
		final TeamManager.TeamData teamData = this.teamManager.getTeamData(companyId);
		Map<Long, UserProtos.User> userMap = UserUtil.doGetUser(userBaseManager, userMarkManager, userExtendsManager, companyId, head.getSession().getUserId(), userIdList, USER_STATE_SET, teamData);
		return Futures.immediateFuture(SearchUserResponse.newBuilder()
				.addAllUser(userMap.values())
				.addAllRefTeam(UserUtil.doGetRefTeam(userMap, Collections.<Integer>emptyList(),teamData).values())
				.addAllRefPosition(UserUtil.doGetRefPosition(positionManager, companyId, userMap, Collections.<Integer>emptyList()).values())
				.addAllRefLevel(UserUtil.doGetRefLevel(levelManager, companyId, userMap, Collections.<Integer>emptyList()).values())
				.build());
	}

	@Override
	public ListenableFuture<GetUserExperienceResponse> getUserExperience(RequestHead head, GetUserExperienceRequest request) {
		return Futures.immediateFuture(this.experienceManager.getUserExperience(head, request));
	}

	@Override
	public ListenableFuture<CreateUserExperienceResponse> createUserExperience(RequestHead head, CreateUserExperienceRequest request) {
		return Futures.immediateFuture(this.experienceManager.createUserExperience(head, request));
	}

	@Override
	public ListenableFuture<UpdateUserExperienceResponse> updateUserExperience(RequestHead head, UpdateUserExperienceRequest request) {
		return Futures.immediateFuture(this.experienceManager.updateUserExperience(head, request));
	}

	@Override
	public ListenableFuture<DeleteUserExperienceResponse> deleteUserExperience(RequestHead head, DeleteUserExperienceRequest request) {
		return Futures.immediateFuture(this.experienceManager.deleteUserExperience(head, request));
	}

	@Override
	public ListenableFuture<MarkUserNameResponse> markUserName(RequestHead head, MarkUserNameRequest request) {
		final long companyId = head.getSession().getCompanyId();
		final long userId = request.getUserId();
		UserProtos.UserBase userBase = this.userBaseManager.getUserBase(companyId, Collections.singleton(userId), USER_STATE_SET).get(userId);
		if (userBase == null) {
			return Futures.immediateFuture(MarkUserNameResponse.newBuilder()
						.setResult(MarkUserNameResponse.Result.FAIL_USER_NOT_EXSIT)
						.setFailText("被标记用户不存在")
						.build());
		}
		return Futures.immediateFuture(this.userMarkManager.markUserName(head, request));
	}

	@Override
	public ListenableFuture<MarkUserStarResponse> markUserStar(RequestHead head, MarkUserStarRequest request) {
		final long companyId = head.getSession().getCompanyId();
		final long userId = request.getUserId();
		UserProtos.UserBase userBase = this.userBaseManager.getUserBase(companyId, Collections.singleton(userId), USER_STATE_SET).get(userId);
		if (userBase == null) {
			return Futures.immediateFuture(MarkUserStarResponse.newBuilder()
						.setResult(MarkUserStarResponse.Result.FAIL_USER_NOT_EXSIT)
						.setFailText("被标星用户不存在")
						.build());
		}
		return Futures.immediateFuture(this.userMarkManager.markUserStar(head, request));
	}

	@Override
	public ListenableFuture<GetMarkStarUserResponse> getMarkStarUser(RequestHead head, GetMarkStarUserRequest request) {
		final long companyId = head.getSession().getCompanyId();
		final Map<Long, UserProtos.UserMark> userMarkMap = this.userMarkManager.getMarkStarUser(companyId, head.getSession().getUserId());
		final Map<Long, UserProtos.UserBase> userBaseMap = this.userBaseManager.getUserBase(companyId, userMarkMap.keySet(), USER_STATE_SET);
		
		final TeamManager.TeamData teamData = this.teamManager.getTeamData(companyId);
		
		Map<Long, UserProtos.User> userMap = new LinkedHashMap<Long, UserProtos.User>(userBaseMap.size());
		
		UserProtos.User.Builder tmpUserBuilder = UserProtos.User.newBuilder();
		for (UserProtos.UserMark userMark : userMarkMap.values()) {
			tmpUserBuilder.clear();
			
			UserProtos.UserBase userBase = userBaseMap.get(userMark.getUserId());
			if (userBase == null) {
				continue;
			}
			
			tmpUserBuilder.setBase(userBase);
			tmpUserBuilder.setMark(userMark);
			
			List<UserProtos.UserTeam> userTeamList = teamData.userTeamMap().get(userBase.getUserId());
			if (userTeamList != null) {
				tmpUserBuilder.addAllTeam(userTeamList);
			}
			
			userMap.put(userBase.getUserId(), tmpUserBuilder.build());
		}
		
		return Futures.immediateFuture(GetMarkStarUserResponse.newBuilder()
				.addAllUser(userMap.values())
				.addAllRefTeam(UserUtil.doGetRefTeam(userMap, Collections.<Integer>emptyList(), teamData).values())
				.addAllRefPosition(UserUtil.doGetRefPosition(positionManager, companyId, userMap, Collections.<Integer>emptyList()).values())
				.addAllRefLevel(UserUtil.doGetRefLevel(levelManager, companyId, userMap, Collections.<Integer>emptyList()).values())
				.build());
	}

	@Override
	public ListenableFuture<GetUserAbilityTagResponse> getUserAbilityTag(RequestHead head, GetUserAbilityTagRequest request) {
		final long companyId = head.getSession().getCompanyId();
		final long userId = request.getUserId();
		
		List<UserProtos.UserAbilityTag> tagList = this.abilityTagManager.getUserAbilityTag(
				companyId, Collections.singleton(request.getUserId()), head.getSession().getUserId()).get(userId);
		
		if (tagList == null) {
			return Futures.immediateFuture(GetUserAbilityTagResponse.newBuilder()
					.build());
		} else {
			return Futures.immediateFuture(GetUserAbilityTagResponse.newBuilder()
					.addAllAbilityTag(tagList)
					.build());
		}
	}
	
	@Override
	public ListenableFuture<TagUserAbilityResponse> tagUserAbility(RequestHead head, TagUserAbilityRequest request) {
		final long companyId = head.getSession().getCompanyId();
		final long userId = request.getUserId();
		UserProtos.UserBase userBase = this.userBaseManager.getUserBase(companyId, Collections.singleton(userId), USER_STATE_SET).get(userId);
		if (userBase == null) {
			return Futures.immediateFuture(TagUserAbilityResponse.newBuilder()
						.setResult(TagUserAbilityResponse.Result.FAIL_USER_NOT_EXSIT)
						.setFailText("被打标签用户不存在")
						.build());
		}
		
		return Futures.immediateFuture(this.abilityTagManager.tagUserAbility(head, request));
	}
	
	@Override
	public ListenableFuture<CreateAbilityTagResponse> createAbilityTag(RequestHead head, CreateAbilityTagRequest request) {
		final long companyId = head.getSession().getCompanyId();
		final long userId = request.getUserId();
		UserProtos.UserBase userBase = this.userBaseManager.getUserBase(companyId, Collections.singleton(userId), USER_STATE_SET).get(userId);
		if (userBase == null) {
			return Futures.immediateFuture(CreateAbilityTagResponse.newBuilder()
						.setResult(CreateAbilityTagResponse.Result.FAIL_USER_NOT_EXSIT)
						.setFailText("被打标签用户不存在")
						.build());
		}
		
		return Futures.immediateFuture(this.abilityTagManager.createAbilityTag(head, request));
	}

	@Override
	public ListenableFuture<EmptyResponse> deleteAbilityTag(RequestHead head, DeleteAbilityTagRequest request) {
		this.abilityTagManager.deleteAbilityTag(head, request);
		return ServiceUtil.EMPTY_RESPONSE_IMMEDIATE_FUTURE;
	}
	
	private static final Random rand = new Random();
	
	@Override
	public ListenableFuture<GetRandomAbilityTagUserResponse> getRandomAbilityTagUser(RequestHead head, GetRandomAbilityTagUserRequest request) {
		final Set<String> tagNameSet = new TreeSet<String>(request.getTagNameList());
		final Boolean isExpert = request.hasIsExpert() ? request.getIsExpert() : null;
		final int size = request.getSize() < 0 ? 0 : request.getSize();
		
		List<Long> userIdList = this.abilityTagManager.getAbilityTagUserId(head.getSession().getCompanyId(), tagNameSet, isExpert);
		
		Collections.shuffle(userIdList, rand);
		if (userIdList.size() > size) {
			userIdList = userIdList.subList(0, size);
		}
		
		final long companyId = head.getSession().getCompanyId();
		final TeamManager.TeamData teamData = this.teamManager.getTeamData(companyId);
		Map<Long, UserProtos.User> userMap = UserUtil.doGetUser(userBaseManager, userMarkManager, userExtendsManager, companyId, head.getSession().getUserId(), userIdList, USER_STATE_SET, teamData);
		Map<Long, List<UserProtos.UserAbilityTag>> userAbilityTagMap = this.abilityTagManager.getUserAbilityTag(companyId, userMap.keySet(), head.getSession().getUserId());
		
		GetRandomAbilityTagUserResponse.Builder responseBuilder = GetRandomAbilityTagUserResponse.newBuilder();
		for (Long userId : userIdList) {
			UserProtos.User user = userMap.get(userId);
			if (user != null && user.getBase().getState() == UserProtos.UserBase.State.NORMAL
					&& (!request.hasIsExpert() || (user.getBase().hasIsExpert() && user.getBase().getIsExpert() == request.getIsExpert()) )) {
				responseBuilder.addUser(user);
				
				List<UserProtos.UserAbilityTag> abilityTagList = userAbilityTagMap.get(userId);
				if (abilityTagList != null) {
					responseBuilder.addAllRefAbilityTag(abilityTagList);
				}
			}
		}
		
		responseBuilder.addAllRefTeam(UserUtil.doGetRefTeam(userMap, Collections.<Integer>emptyList(),teamData).values());
		responseBuilder.addAllRefPosition(UserUtil.doGetRefPosition(positionManager, companyId, userMap, Collections.<Integer>emptyList()).values());
		responseBuilder.addAllRefLevel(UserUtil.doGetRefLevel(levelManager, companyId, userMap, Collections.<Integer>emptyList()).values());
		
		return Futures.immediateFuture(responseBuilder.build());
	}
	
	@Override
	public ListenableFuture<GetAbilityTagUserIdResponse> getAbilityTagUserId(RequestHead head, GetAbilityTagUserIdRequest request) {
		final Set<String> tagNameSet = new TreeSet<String>(request.getTagNameList());
		final Boolean isExpert = request.hasIsExpert() ? request.getIsExpert() : null;
		
		List<Long> userIdList = this.abilityTagManager.getAbilityTagUserId(head.getSession().getCompanyId(), tagNameSet, isExpert);
		
		return Futures.immediateFuture(GetAbilityTagUserIdResponse.newBuilder()
				.addAllUserId(userIdList)
				.build());
	}

	@Override
	public ListenableFuture<GetAllPositionResponse> getAllPosition(RequestHead head, GetAllPositionRequest request) {
		final long companyId = head.getSession().getCompanyId();
		return Futures.immediateFuture(GetAllPositionResponse.newBuilder()
				.addAllPosition(this.positionManager.getAllPosition(companyId).values())
				.build());
	}

	@Override
	public ListenableFuture<GetAllLevelResponse> getAllLevel(RequestHead head, GetAllLevelRequest request) {
		final long companyId = head.getSession().getCompanyId();
		return Futures.immediateFuture(GetAllLevelResponse.newBuilder()
				.addAllLevel(this.levelManager.getAllLevel(companyId).values())
				.build());
	}
	
}
