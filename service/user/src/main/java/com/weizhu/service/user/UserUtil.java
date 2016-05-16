package com.weizhu.service.user;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import javax.annotation.Nullable;

import com.weizhu.proto.UserProtos;
import com.weizhu.service.user.base.UserBaseManager;
import com.weizhu.service.user.exts.UserExtendsManager;
import com.weizhu.service.user.level.LevelManager;
import com.weizhu.service.user.mark.UserMarkManager;
import com.weizhu.service.user.position.PositionManager;
import com.weizhu.service.user.team.TeamManager;

public class UserUtil {

	public static boolean isValidRawId(String rawId) {
		return rawId != null && !rawId.isEmpty() && rawId.length() < 191; 
	}
	
	public static String tipsRawId() {
		return "人员员工id长度小于191,非空";
	}
	
	public static boolean isValidUserName(String userName) {
		return userName != null && !userName.isEmpty() && userName.length() <= 8; 
	}
	
	public static String tipsUserName() {
		return "人员名长度为8字,非空";
	}
	
	public static boolean isValidGender(String gender) {
		return "男".equals(gender) || "女".equals(gender); 
	}
	
	public static String tipsGender() {
		return "人员性别请填写‘男’或‘女’";
	}
	
	public static boolean isValidPhoneNo(String phoneNo) {
		return phoneNo != null && !phoneNo.isEmpty() && phoneNo.length() < 191; 
	}
	
	public static String tipsPhoneNo() {
		return "人员座机号长度大于0小于191";
	}
	
	static Map<Long, UserProtos.User> doGetUser(
			final UserBaseManager userBaseManager, @Nullable final UserMarkManager userMarkManager, UserExtendsManager userExtendsManager, 
			final long companyId, @Nullable Long reqUserId, Collection<Long> userIds, @Nullable Set<UserProtos.UserBase.State> userStateSet, 
			TeamManager.TeamData teamData) {
		if (userIds.isEmpty()) {
			return Collections.emptyMap();
		}
		
		final Map<Long, UserProtos.UserBase> userBaseMap = userBaseManager.getUserBase(companyId, userIds, userStateSet);
		if (userBaseMap.isEmpty()) {
			return Collections.emptyMap();
		}
		
		final Map<Long, UserProtos.UserMark> userMarkMap;
		if (userMarkManager == null || reqUserId == null ) {
			userMarkMap = Collections.emptyMap();
		} else {
			Map<Long, UserProtos.UserMark> map = userMarkManager.getUserMark(companyId, Collections.singleton(reqUserId)).get(reqUserId);
			if (map == null) {
				userMarkMap = Collections.emptyMap();
			} else {
				userMarkMap = map;
			}
		}
		
		final Map<Long, List<UserProtos.UserExtends>> userExtendsMap = userExtendsManager.getUserExtends(companyId, userIds);
		
		Map<Long, UserProtos.User> userMap = new HashMap<Long, UserProtos.User>(userBaseMap.size());
		UserProtos.User.Builder tmpUserBuilder = UserProtos.User.newBuilder();
		for (UserProtos.UserBase userBase : userBaseMap.values()) {
			tmpUserBuilder.clear();
			
			tmpUserBuilder.setBase(userBase);
			
			UserProtos.UserMark userMark = userMarkMap.get(userBase.getUserId());
			if (userMark != null) {
				tmpUserBuilder.setMark(userMark);
			}
			
			List<UserProtos.UserTeam> userTeamList = teamData.userTeamMap().get(userBase.getUserId());
			if (userTeamList != null) {
				tmpUserBuilder.addAllTeam(userTeamList);
			}
			
			List<UserProtos.UserExtends> userExtendsList = userExtendsMap.get(userBase.getUserId());
			if (userExtendsList != null) {
				tmpUserBuilder.addAllExt(userExtendsList);
			}
			
			userMap.put(userBase.getUserId(), tmpUserBuilder.build());
		}

		return userMap;
	}
	
	static Map<Integer, UserProtos.Team> doGetRefTeam(Map<Long, UserProtos.User> userMap, Collection<Integer> teamIds, TeamManager.TeamData teamData) {
		if (userMap.isEmpty() && teamIds.isEmpty()) {
			return Collections.emptyMap();
		}
		
		Set<Integer> teamIdSet = new TreeSet<Integer>(teamIds);
		for (UserProtos.User user : userMap.values()) {
			for (UserProtos.UserTeam userTeam : user.getTeamList()) {
				int teamId = userTeam.getTeamId();
				while (true) {
					teamIdSet.add(teamId);
					
					TeamManager.TeamInfo teamInfo = teamData.teamInfoMap().get(teamId);
					if (teamInfo == null) {
						// log warn
						break;
					} else if (teamInfo.team().hasParentTeamId()) {
						teamId = teamInfo.team().getParentTeamId();
					} else {
						// root team
						break;
					}
				}
			}
		}
		
		if (teamIdSet.isEmpty()) {
			return Collections.emptyMap();
		} else {
			Map<Integer, UserProtos.Team> teamMap = new HashMap<Integer, UserProtos.Team>(teamIdSet.size());
			for (Integer teamId : teamIdSet) {
				TeamManager.TeamInfo teamInfo = teamData.teamInfoMap().get(teamId);
				if (teamInfo != null) {
					teamMap.put(teamId, teamInfo.team());
				}
			}
			return teamMap;
		}
	}
	
	static Map<Integer, UserProtos.Position> doGetRefPosition(
			final PositionManager positionManager, long companyId, 
			Map<Long, UserProtos.User> userMap, Collection<Integer> positionIds) {
		if (userMap.isEmpty() && positionIds.isEmpty()) {
			return Collections.emptyMap();
		}
		
		Set<Integer> positionIdSet = new TreeSet<Integer>(positionIds);
		
		for (UserProtos.User user : userMap.values()) {
			for (UserProtos.UserTeam userTeam : user.getTeamList()) {
				if (userTeam.hasPositionId()) {
					positionIdSet.add(userTeam.getPositionId());
				}
			}
		}
		
		if (positionIdSet.isEmpty()) {
			return Collections.emptyMap();
		}
		
		Map<Integer, UserProtos.Position> allPositionMap = positionManager.getAllPosition(companyId);
		
		Map<Integer, UserProtos.Position> positionMap = new HashMap<Integer, UserProtos.Position>(positionIdSet.size());
		for (Integer positionId : positionIdSet) {
			UserProtos.Position position = allPositionMap.get(positionId);
			if (position != null) {
				positionMap.put(positionId, position);
			}
		}
		return positionMap;
	}
	
	static Map<Integer, UserProtos.Level> doGetRefLevel(
			final LevelManager levelManager, long companyId, 
			Map<Long, UserProtos.User> userMap, Collection<Integer> levelIds) {
		if (userMap.isEmpty() && levelIds.isEmpty()) {
			return Collections.emptyMap();
		}
		
		Set<Integer> levelIdSet = new TreeSet<Integer>(levelIds);
		
		for (UserProtos.User user : userMap.values()) {
			if (user.getBase().hasLevelId()) {
				levelIdSet.add(user.getBase().getLevelId());
			}
		}
		
		if (levelIdSet.isEmpty()) {
			return Collections.emptyMap();
		}
		
		Map<Integer, UserProtos.Level> allLevelMap = levelManager.getAllLevel(companyId);
		
		Map<Integer, UserProtos.Level> levelMap = new HashMap<Integer, UserProtos.Level>(levelIdSet.size());
		for (Integer levelId : levelIdSet) {
			UserProtos.Level level = allLevelMap.get(levelId);
			if (level != null) {
				levelMap.put(levelId, level);
			}
		}
		return levelMap;
	}
	
}
