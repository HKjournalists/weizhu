package com.weizhu.service.user;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.TreeSet;
import java.util.Map.Entry;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import com.google.common.primitives.Ints;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.weizhu.common.db.DBUtil;
import com.weizhu.common.utils.EmailUtil;
import com.weizhu.common.utils.MobileNoUtil;
import com.weizhu.proto.AdminUserProtos.RawUserExtends;
import com.weizhu.proto.UserProtos;
import com.weizhu.proto.AdminProtos.AdminHead;
import com.weizhu.proto.AdminUserProtos.ImportUserRequest;
import com.weizhu.proto.AdminUserProtos.ImportUserResponse;
import com.weizhu.proto.AdminUserProtos.RawUser;
import com.weizhu.proto.AdminUserProtos.RawUserTeam;
import com.weizhu.proto.AdminUserProtos.ImportUserResponse.InvalidUser;
import com.weizhu.service.user.abilitytag.AbilityTagCache;
import com.weizhu.service.user.abilitytag.AbilityTagDB;
import com.weizhu.service.user.base.UserBaseCache;
import com.weizhu.service.user.base.UserBaseDB;
import com.weizhu.service.user.exts.UserExtendsCache;
import com.weizhu.service.user.exts.UserExtendsDB;
import com.weizhu.service.user.level.LevelCache;
import com.weizhu.service.user.level.LevelDB;
import com.weizhu.service.user.position.PositionCache;
import com.weizhu.service.user.position.PositionDB;
import com.weizhu.service.user.team.TeamDB;
import com.zaxxer.hikari.HikariDataSource;

@Singleton
public class ImportUserManager {

	private static final ImmutableSet<UserProtos.State> STATE_SET = ImmutableSet.of(UserProtos.State.NORMAL);
	
	private final HikariDataSource hikariDataSource;
	private final JedisPool jedisPool;
	
	@Inject
	public ImportUserManager(HikariDataSource hikariDataSource, JedisPool jedisPool) {
		this.hikariDataSource = hikariDataSource;
		this.jedisPool = jedisPool;
	}
	
	private static final Comparator<InvalidUser> INVALID_USER_CMP = new Comparator<InvalidUser>() {

		@Override
		public int compare(InvalidUser o1, InvalidUser o2) {
			if (o1.getInvalidIndex() != o2.getInvalidIndex()) {
				return Ints.compare(o1.getInvalidIndex(), o2.getInvalidIndex());
			}
			return o1.getInvalidText().compareTo(o2.getInvalidText());
		}
		
	};
	
	public ImportUserResponse importUser(AdminHead head, ImportUserRequest request) {
		if (!head.hasCompanyId()) {
			return ImportUserResponse.newBuilder()
					.setResult(ImportUserResponse.Result.FAIL_UNKNOWN)
					.setFailText("company_id 参数未填")
					.build();
		}
		
		final long companyId = head.getCompanyId();
		final long adminId = head.getSession().getAdminId();
		final int now = (int) (System.currentTimeMillis() / 1000L);
		final List<RawUser> rawUserList = request.getRawUserList();
		
		List<InvalidUser> invalidUserList = new ArrayList<InvalidUser>();
		
		// 1. 检查输入用户数据,并构造node
		final ImmutableMap<String, TeamNode> teamNodeMap = fetchTeamNodeMap(rawUserList, invalidUserList);
		final ImmutableMap<String, PositionNode> positionNodeMap = fetchPositionNodeMap(rawUserList, invalidUserList);
		final ImmutableMap<String, LevelNode> levelNodeMap = fetchLevelNodeMap(rawUserList, invalidUserList);
		final ImmutableMap<String, UserNode> userNodeMap = fetchUserNodeMap(rawUserList, teamNodeMap, positionNodeMap, levelNodeMap, invalidUserList);
		
		if (!invalidUserList.isEmpty()) {
			Collections.sort(invalidUserList, INVALID_USER_CMP);
			return ImportUserResponse.newBuilder()
					.setResult(ImportUserResponse.Result.FAIL_USER_INVALID)
					.addAllInvalidUser(invalidUserList)
					.build();
		}
		
		// 2. 从现有db中获取所有团队信息，职位职级信息，用户信息，并填入对应node

		int insertUserCnt = 0;
		int updateUserCnt = 0;
		int insertTeamCnt = 0;
		int insertPositionCnt = 0;
		int insertLevelCnt = 0;
		
		Connection dbConn = null;
		try {
			dbConn = hikariDataSource.getConnection();
			
			fillTeamNodeMap(dbConn, companyId, teamNodeMap);
			fillPositionNodeMap(dbConn, companyId, positionNodeMap);
			fillLevelNodeMap(dbConn, companyId, levelNodeMap);
			fillUserNodeMap(dbConn, companyId, userNodeMap, invalidUserList); // 会从db中获取其他人的手机号，检查是否有冲突
			
			
			if (!invalidUserList.isEmpty()) {
				Collections.sort(invalidUserList, INVALID_USER_CMP);
				return ImportUserResponse.newBuilder()
						.setResult(ImportUserResponse.Result.FAIL_USER_INVALID)
						.addAllInvalidUser(invalidUserList)
						.build();
			}
			
			// 3. 更新团队信息, 更新职位职级信息
			
			insertTeamCnt = insertDBTeamNodeMap(dbConn, companyId, teamNodeMap, adminId, now);
			insertPositionCnt = insertDBPositionNodeMap(dbConn, companyId, positionNodeMap, adminId, now);
			insertLevelCnt = insertDBLevelNodeMap(dbConn, companyId, levelNodeMap, adminId, now);
			UpdateUserResult res = updateDBUserNodeMap(dbConn, companyId, userNodeMap, adminId, now);
			insertUserCnt = res.insertUserCnt;
			updateUserCnt = res.updateUserCnt;
			
		} catch (SQLException e) {
			throw new RuntimeException("importUser db fail!", e);
		} finally {
			DBUtil.closeQuietly(dbConn);
		}
		
		Jedis jedis = jedisPool.getResource();
		try {
			PositionCache.delAllPosition(jedis, companyId);
			LevelCache.delAllLevel(jedis, companyId);
			
			List<Long> userIdList = new ArrayList<Long>(userNodeMap.size());
			for (UserNode userNode : userNodeMap.values()) {
				userIdList.add(userNode.userBase.getUserId());
			}
			UserBaseCache.delUserBase(jedis, companyId, userIdList);
			
			// notify reload team data
			jedis.publish("__default_task_loader__", "user:team:load@" + companyId);
			
			UserExtendsCache.delUserExtends(jedis, companyId, userIdList);
			
			AbilityTagCache.delAbilityTag(jedis, companyId, userIdList);
		} finally {
			jedis.close();
		}
		
		return ImportUserResponse.newBuilder()
				.setResult(ImportUserResponse.Result.SUCC)
				.setCreateUserCnt(insertUserCnt)
				.setUpdateUserCnt(updateUserCnt)
				.setCreateTeamCnt(insertTeamCnt)
				.setCreatePositionCnt(insertPositionCnt)
				.setCreateLevelCnt(insertLevelCnt)
				.build();
	}
	
	private static ImmutableMap<String, TeamNode> fetchTeamNodeMap(List<RawUser> rawUserList, List<InvalidUser> invalidUserList) {
		final Map<String, Object> teamMap = new LinkedHashMap<String, Object>();
		
		for (int i=0; i<rawUserList.size(); ++i) {
			RawUser rawUser = rawUserList.get(i);
			for (int j=0; j<rawUser.getUserTeamCount(); ++j) {
				RawUserTeam rawUserTeam = rawUser.getUserTeam(j);
				
				if (rawUserTeam.getTeamCount() > 0) {
					Map<String, Object> curTeamMap = teamMap;
					for (int k=0; k<rawUserTeam.getTeamCount(); ++k) {
						String teamName = rawUserTeam.getTeam(k);
						
						if (teamName.isEmpty()) {
							invalidUserList.add(InvalidUser.newBuilder().setInvalidIndex(i).setInvalidText("人员部门名称为空字符串").build());
							break;
						} else {
							@SuppressWarnings("unchecked")
							Map<String, Object> subTeamMap = (Map<String, Object>) curTeamMap.get(teamName);
							if (subTeamMap == null) {
								subTeamMap = new LinkedHashMap<String, Object>();
								curTeamMap.put(teamName, subTeamMap);
							}
							
							curTeamMap = subTeamMap;
						}
					}
				}
			}
		}
		
		return buildTeamNodeMap(teamMap);
	}
	
	@SuppressWarnings("unchecked")
	private static ImmutableMap<String, TeamNode> buildTeamNodeMap(Map<String, Object> teamMap) {
		if (teamMap.isEmpty()) {
			return ImmutableMap.of();
		}
		
		Map<String, TeamNode> teamNodeMap = Maps.newTreeMap();
		for (Entry<String, Object> entry : teamMap.entrySet()) {
			teamNodeMap.put(entry.getKey(), new TeamNode(entry.getKey(), buildTeamNodeMap((Map<String, Object>)entry.getValue())));
		}
		return ImmutableMap.copyOf(teamNodeMap);
	}
	
	private static ImmutableMap<String, PositionNode> fetchPositionNodeMap(List<RawUser> rawUserList, List<InvalidUser> invalidUserList) {
		Map<String, PositionNode> positionNodeMap = new LinkedHashMap<String, PositionNode>();
		for (int i=0; i<rawUserList.size(); ++i) {
			RawUser rawUser = rawUserList.get(i);
			for (int j=0; j<rawUser.getUserTeamCount(); ++j) {
				RawUserTeam rawUserTeam = rawUser.getUserTeam(j);
				
				if (rawUserTeam.hasPosition()) {
					if (rawUserTeam.getPosition().isEmpty()) {
						invalidUserList.add(InvalidUser.newBuilder().setInvalidIndex(i).setInvalidText("人员职位名称为空字符串").build());
					} else if (!positionNodeMap.containsKey(rawUserTeam.getPosition())) {
						positionNodeMap.put(rawUserTeam.getPosition(), new PositionNode(rawUserTeam.getPosition()));
					}
				}
			}
		}
		return ImmutableMap.copyOf(positionNodeMap);
	}
	
	private static ImmutableMap<String, LevelNode> fetchLevelNodeMap(List<RawUser> rawUserList, List<InvalidUser> invalidUserList) {
		Map<String, LevelNode> levelNodeMap = new LinkedHashMap<String, LevelNode>();
		for (int i=0; i<rawUserList.size(); ++i) {
			RawUser rawUser = rawUserList.get(i);
			if (rawUser.hasLevel()) {
				if (rawUser.getLevel().isEmpty()) {
					invalidUserList.add(InvalidUser.newBuilder().setInvalidIndex(i).setInvalidText("人员职级名称为空字符串").build());
				} else if (!levelNodeMap.containsKey(rawUser.getLevel())) {
					levelNodeMap.put(rawUser.getLevel(), new LevelNode(rawUser.getLevel()));
				}
			}
		}
		return ImmutableMap.copyOf(levelNodeMap);
	}
	
	private static ImmutableMap<String, UserNode> fetchUserNodeMap(
			List<RawUser> rawUserList, 
			ImmutableMap<String, TeamNode> teamNodeMap, 
			ImmutableMap<String, PositionNode> positionNodeMap, 
			ImmutableMap<String, LevelNode> levelNodeMap,
			List<InvalidUser> invalidUserList) {
		
		final Map<String, UserNode> userNodeMap = new LinkedHashMap<String, UserNode>(rawUserList.size());
		
		final Map<String, String> mobileNoToUserMap = new HashMap<String, String>(rawUserList.size());
		
		for (int i=0; i<rawUserList.size(); ++i) {
			RawUser rawUser = rawUserList.get(i);
			
			if (!UserUtil.isValidRawId(rawUser.getRawId())) {
				invalidUserList.add(InvalidUser.newBuilder().setInvalidIndex(i).setInvalidText("人员id错误：" + UserUtil.tipsRawId()).build());
				continue;
			}
			
			if (userNodeMap.containsKey(rawUser.getRawId())) {
				invalidUserList.add(InvalidUser.newBuilder().setInvalidIndex(i).setInvalidText("人员id重复").build());
				continue;
			}
			
			if (!UserUtil.isValidUserName(rawUser.getUserName())) {
				invalidUserList.add(InvalidUser.newBuilder().setInvalidIndex(i).setInvalidText("人员名称错误: " + UserUtil.tipsUserName()).build());
			}
			
			if (rawUser.hasGender() && !UserUtil.isValidGender(rawUser.getGender())) {
				invalidUserList.add(InvalidUser.newBuilder().setInvalidIndex(i).setInvalidText("人员性别错误: " + UserUtil.tipsGender()).build());
			}
			
			if (rawUser.getMobileNoCount() <= 0) {
				invalidUserList.add(InvalidUser.newBuilder().setInvalidIndex(i).setInvalidText("人员手机号错误: 必须包含一个手机号").build());
			} else {
				for (int j=0; j<rawUser.getMobileNoCount(); ++j) {
					String mobileNo = rawUser.getMobileNo(j);
					if (!MobileNoUtil.isValid(mobileNo)) {
						invalidUserList.add(InvalidUser.newBuilder().setInvalidIndex(i).setInvalidText("人员手机号格式错误: " + mobileNo).build());
					} else {
						String rawId = mobileNoToUserMap.get(mobileNo);
						if (rawId != null) {
							invalidUserList.add(InvalidUser.newBuilder().setInvalidIndex(i).setInvalidText("人员手机号冲突：和人员" + rawId + "重复").build());
						} else {
							mobileNoToUserMap.put(mobileNo, rawUser.getRawId());
						}
					}
				}
			}
			
			for (int j=0; j<rawUser.getPhoneNoCount(); ++j) {
				String phoneNo = rawUser.getPhoneNo(j);
				if (!UserUtil.isValidPhoneNo(phoneNo)) {
					invalidUserList.add(InvalidUser.newBuilder().setInvalidIndex(i).setInvalidText("人员座机号错误：" + phoneNo + ", " + UserUtil.tipsPhoneNo()).build());
				}
			}
			
			if (rawUser.hasEmail() && !EmailUtil.isValid(rawUser.getEmail())) {
				invalidUserList.add(InvalidUser.newBuilder().setInvalidIndex(i).setInvalidText("人员邮箱格式错误").build());
			}
			
			LevelNode levelNode = rawUser.hasLevel() ? levelNodeMap.get(rawUser.getLevel()) : null;
			
			List<UserTeamNode> userTeamNodeList = new ArrayList<UserTeamNode>();
			
			for (int j=0; j<rawUser.getUserTeamCount(); ++j) {
				RawUserTeam rawUserTeam = rawUser.getUserTeam(j);
				
				PositionNode positionNode = rawUserTeam.hasPosition() ? positionNodeMap.get(rawUserTeam.getPosition()) : null;
				TeamNode teamNode = getUserTeamNode(rawUserTeam, teamNodeMap);
				
				if (teamNode != null) {
					userTeamNodeList.add(new UserTeamNode(teamNode, positionNode));
				}
			}
			
			Map<String, String> userExtendsMap = new HashMap<String, String>();
			for (RawUserExtends exts : rawUser.getUserExtsList()) {
				if (exts.getName().isEmpty() || exts.getValue().isEmpty()) {
					continue;
				}
				
				if (exts.getName().length() > 10) {
					invalidUserList.add(InvalidUser.newBuilder().setInvalidIndex(i).setInvalidText("扩展字段名最多10个字: " + exts.getName()).build());
				}
				if (exts.getValue().length() > 20) {
					invalidUserList.add(InvalidUser.newBuilder().setInvalidIndex(i).setInvalidText("扩展字段" + exts.getName() + "值最多20个字: " + exts.getValue()).build());
				}
				
				userExtendsMap.put(exts.getName(), exts.getValue());
			}
			
			ImmutableSet.Builder<String> abilityTagSetBuilder = ImmutableSet.builder();
			for (String tag : rawUser.getAbilityTagList()) {
				tag = tag.trim();
				if (tag.isEmpty()) {
					continue;
				}
				
				if (tag.length() > 10) {
					invalidUserList.add(InvalidUser.newBuilder().setInvalidIndex(i).setInvalidText("能力标签最多10个字").build());
				}
				
				abilityTagSetBuilder.add(tag);
			}
			
			ImmutableSet<String> abilityTagSet = abilityTagSetBuilder.build();
			if (abilityTagSet.size() > 50) {
				invalidUserList.add(InvalidUser.newBuilder().setInvalidIndex(i).setInvalidText("能力标签最多50个").build());
			}
			
			userNodeMap.put(rawUser.getRawId(), new UserNode(i, rawUser, levelNode, ImmutableList.copyOf(userTeamNodeList), ImmutableMap.copyOf(userExtendsMap), abilityTagSet));
		}
		
		return ImmutableMap.copyOf(userNodeMap);
	}
	
	private static TeamNode getUserTeamNode(RawUserTeam rawUserTeam, ImmutableMap<String, TeamNode> teamNodeMap) {
		if (rawUserTeam.getTeamCount() <= 0) {
			return null;
		}
		
		TeamNode teamNode = teamNodeMap.get(rawUserTeam.getTeam(0));
		if (teamNode == null) {
			return null;
		}
		
		for (int i=1; i<rawUserTeam.getTeamCount(); ++i) {
			teamNode = teamNode.subTeamNode.get(rawUserTeam.getTeam(i));
			if (teamNode == null) {
				return null;
			}
		}
		return teamNode;
	}
	
	private static void fillTeamNodeMap(Connection dbConn, long companyId, ImmutableMap<String, TeamNode> teamNodeMap) throws SQLException {
		
		Map<Integer, UserProtos.Team> teamMap = TeamDB.getAllTeam(dbConn, companyId, STATE_SET);
		
		Map<Integer, List<UserProtos.Team>> subTeamMap = new HashMap<Integer, List<UserProtos.Team>>();
		for (UserProtos.Team team : teamMap.values()) {
			Integer parentTeamId = team.hasParentTeamId() ? team.getParentTeamId() : null;
			List<UserProtos.Team> list = subTeamMap.get(parentTeamId);
			if (list == null) {
				list = new LinkedList<UserProtos.Team>();
				subTeamMap.put(parentTeamId, list);
			}
			list.add(team);
		}
		
		Queue<TeamNode> queue = new LinkedList<TeamNode>();
		
		List<UserProtos.Team> rootTeamList = subTeamMap.get(null);
		if (rootTeamList != null) {
			for (UserProtos.Team team : rootTeamList) {
				TeamNode teamNode = teamNodeMap.get(team.getTeamName());
				if (teamNode != null && teamNode.team == null) {
					teamNode.team = team;
					queue.offer(teamNode);
				}
			}
		}
		
		while (!queue.isEmpty()) {
			TeamNode teamNode = queue.poll();
			List<UserProtos.Team> subTeamList = subTeamMap.get(teamNode.team.getTeamId());
			if (subTeamList != null) {
				for (UserProtos.Team subTeam : subTeamList) {
					TeamNode subTeamNode = teamNode.subTeamNode.get(subTeam.getTeamName());
					if (subTeamNode != null && subTeamNode.team == null) {
						subTeamNode.team = subTeam;
						queue.offer(subTeamNode);
					}
				}
			}
		}
	}
	
	private static void fillPositionNodeMap(Connection dbConn, long companyId, ImmutableMap<String, PositionNode> positionNodeMap) throws SQLException {
		Map<Integer, UserProtos.Position> positionMap = PositionDB.getAllPosition(dbConn, companyId, STATE_SET);
		for (UserProtos.Position position : positionMap.values()) {
			PositionNode positionNode = positionNodeMap.get(position.getPositionName());
			if (positionNode != null && positionNode.position == null) {
				positionNode.position = position;
			}
		}
	}
	
	private static void fillLevelNodeMap(Connection dbConn, long companyId, ImmutableMap<String, LevelNode> levelNodeMap) throws SQLException {
		Map<Integer, UserProtos.Level> levelMap = LevelDB.getAllLevel(dbConn, companyId, STATE_SET);
		for (UserProtos.Level level : levelMap.values()) {
			LevelNode levelNode = levelNodeMap.get(level.getLevelName());
			if (levelNode != null && levelNode.level == null) {
				levelNode.level = level;
			}
		}
	}
	
	private static void fillUserNodeMap(Connection dbConn, long companyId, ImmutableMap<String, UserNode> userNodeMap, List<InvalidUser> invalidUserList) throws SQLException {
		Map<String, Long> rawIdToUserIdMap = UserBaseDB.getUserIdByRawIdUnique(dbConn, companyId, userNodeMap.keySet());
		
		Map<Long, UserProtos.UserBase> userBaseMap = UserBaseDB.getUserBase(dbConn, companyId, rawIdToUserIdMap.values());
		for (UserProtos.UserBase userBase : userBaseMap.values()) {
			UserNode userNode = userNodeMap.get(userBase.getRawId());
			if (userNode != null) {
				userNode.userBase = userBase;
			}
		}
		
		Map<Long, List<UserProtos.UserTeam>> userTeamMap = TeamDB.getUserTeam(dbConn, companyId, userBaseMap.keySet());
		for (Map.Entry<Long, List<UserProtos.UserTeam>> entry : userTeamMap.entrySet()) {
			UserProtos.UserBase userBase = userBaseMap.get(entry.getKey());
			if (userBase != null) {
				UserNode userNode = userNodeMap.get(userBase.getRawId());
				if (userNode != null) {
					userNode.userTeamList = entry.getValue();
				}
			}
		}
		
		Map<Long, List<UserProtos.UserExtends>> userExtendsMap = UserExtendsDB.getUserExtends(dbConn, companyId, userBaseMap.keySet());
		for (Map.Entry<Long, List<UserProtos.UserExtends>> entry : userExtendsMap.entrySet()) {
			UserProtos.UserBase userBase = userBaseMap.get(entry.getKey());
			if (userBase != null) {
				UserNode userNode = userNodeMap.get(userBase.getRawId());
				if (userNode != null) {
					userNode.userExtendsList = entry.getValue();
				}
			}
		}
		
		Map<Long, List<UserProtos.UserAbilityTag>> abilityTagMap = AbilityTagDB.getUserAbilityTagList(dbConn, companyId, userBaseMap.keySet());
		for (Map.Entry<Long, List<UserProtos.UserAbilityTag>> entry : abilityTagMap.entrySet()) {
			UserProtos.UserBase userBase = userBaseMap.get(entry.getKey());
			if (userBase != null) {
				UserNode userNode = userNodeMap.get(userBase.getRawId());
				if (userNode != null) {
					Set<String> set = new TreeSet<String>();
					for (UserProtos.UserAbilityTag tag : entry.getValue()) {
						set.add(tag.getTagName());
					}
					userNode.abilityTagSet = set;
				}
			}
		}
		
		// Map<String, Long> mobileNoToUserIdMap = UserBaseDB.getMobileNoExcludeUserId(dbConn, userBaseMap.keySet());
		
		Map<String, Long> mobileNoToUserIdMap = UserBaseDB.getMobileNoUniqueExcludeUserId(dbConn, companyId, Collections.<Long>emptySet());
		for (UserNode userNode : userNodeMap.values()) {
			for (int i=0; i<userNode.rawUser.getMobileNoCount(); ++i) {
				String mobileNo = userNode.rawUser.getMobileNo(i);
				Long userId = mobileNoToUserIdMap.get(mobileNo);
				
				if (userId != null && (userNode.userBase == null || userNode.userBase.getUserId() != userId.longValue() )) {
					invalidUserList.add(InvalidUser.newBuilder().setInvalidIndex(userNode.index).setInvalidText("人员手机号冲突：" + mobileNo + "手机号已存在 " + userId).build());
				}
				
//				if (mobileNoToUserIdMap.containsKey(mobileNo)) {
//					invalidUserList.add(InvalidUser.newBuilder().setInvalidIndex(userNode.index).setInvalidText("人员手机号冲突：" + mobileNo + "手机号已存在 " + mobileNoToUserIdMap.get(mobileNo)).build());
//				}
			}
		}
	}
	
	private static int insertDBTeamNodeMap(Connection dbConn, long companyId, ImmutableMap<String, TeamNode> teamNodeMap, long adminId, int now) throws SQLException {
		int insertTeamCnt = 0;
		
		UserProtos.Team.Builder teamBuilder = UserProtos.Team.newBuilder();
		Queue<TeamNode> queue = new LinkedList<TeamNode>();
		
		for (TeamNode teamNode : teamNodeMap.values()) {
			if (teamNode.team == null) {
				teamBuilder.clear();
				
				teamBuilder.setTeamId(0);
				teamBuilder.setTeamName(teamNode.teamName);
				teamBuilder.setState(UserProtos.State.NORMAL);
				teamBuilder.setCreateAdminId(adminId);
				teamBuilder.setCreateTime(now);
				int teamId = TeamDB.insertTeam(dbConn, companyId, Collections.<UserProtos.Team>singletonList(teamBuilder.build())).get(0);
				teamBuilder.setTeamId(teamId);
				teamNode.team = teamBuilder.build();
				
				insertTeamCnt++;
			}
			
			queue.offer(teamNode);
		}
		
		while (!queue.isEmpty()) {
			TeamNode parentTeamNode = queue.poll();
			for (TeamNode subTeamNode : parentTeamNode.subTeamNode.values()) {
				if (subTeamNode.team == null) {
					teamBuilder.clear();
					
					teamBuilder.setTeamId(0);
					teamBuilder.setTeamName(subTeamNode.teamName);
					teamBuilder.setParentTeamId(parentTeamNode.team.getTeamId());
					teamBuilder.setState(UserProtos.State.NORMAL);
					teamBuilder.setCreateAdminId(adminId);
					teamBuilder.setCreateTime(now);
					int teamId = TeamDB.insertTeam(dbConn, companyId, Collections.<UserProtos.Team>singletonList(teamBuilder.build())).get(0);
					teamBuilder.setTeamId(teamId);
					subTeamNode.team = teamBuilder.build();
					
					insertTeamCnt++;
				}
				queue.offer(subTeamNode);
			}
		}
		
		return insertTeamCnt;
	}
	
	private static int insertDBPositionNodeMap(Connection dbConn, long companyId, ImmutableMap<String, PositionNode> positionNodeMap, long adminId, int now) throws SQLException {
		List<UserProtos.Position> insertPositionList = new ArrayList<UserProtos.Position>();
		
		UserProtos.Position.Builder positionBuilder = UserProtos.Position.newBuilder();
		for (PositionNode positionNode : positionNodeMap.values()) {
			if (positionNode.position == null) {
				positionBuilder.clear();
				
				positionBuilder.setPositionId(0);
				positionBuilder.setPositionName(positionNode.positionName);
				positionBuilder.setPositionDesc("");
				positionBuilder.setState(UserProtos.State.NORMAL);
				positionBuilder.setCreateAdminId(adminId);
				positionBuilder.setCreateTime(now);
				
				insertPositionList.add(positionBuilder.build());
			}
		}
		
		List<Integer> insertPositionIdList = PositionDB.insertPosition(dbConn, companyId, insertPositionList);
		for (int i=0; i<insertPositionList.size(); ++i) {
			positionBuilder.clear();
			positionBuilder.mergeFrom(insertPositionList.get(i));
			positionBuilder.setPositionId(insertPositionIdList.get(i));
			
			PositionNode positionNode = positionNodeMap.get(positionBuilder.getPositionName());
			if (positionNode != null && positionNode.position == null) {
				positionNode.position = positionBuilder.build();
			}
		}
		
		return insertPositionIdList.size();
	}
	
	private static int insertDBLevelNodeMap(Connection dbConn, long companyId, ImmutableMap<String, LevelNode> levelNodeMap, long adminId, int now) throws SQLException {
		List<UserProtos.Level> insertLevelList = new ArrayList<UserProtos.Level>();
		
		UserProtos.Level.Builder levelBuilder = UserProtos.Level.newBuilder();
		for (LevelNode levelNode : levelNodeMap.values()) {
			if (levelNode.level == null) {
				levelBuilder.clear();
				
				levelBuilder.setLevelId(0);
				levelBuilder.setLevelName(levelNode.levelName);
				levelBuilder.setState(UserProtos.State.NORMAL);
				levelBuilder.setCreateAdminId(adminId);
				levelBuilder.setCreateTime(now);
				
				insertLevelList.add(levelBuilder.build());
			}
		}
		
		List<Integer> insertLevelIdList = LevelDB.insertLevel(dbConn, companyId, insertLevelList);
		for (int i=0; i<insertLevelList.size(); ++i) {
			levelBuilder.clear();
			levelBuilder.mergeFrom(insertLevelList.get(i));
			levelBuilder.setLevelId(insertLevelIdList.get(i));
			
			LevelNode levelNode = levelNodeMap.get(levelBuilder.getLevelName());
			if (levelNode != null && levelNode.level == null) {
				levelNode.level = levelBuilder.build();
			}
		}
		
		return insertLevelIdList.size();
	}
	
	private static class UpdateUserResult {
		UpdateUserResult(int insertUserCnt, int updateUserCnt) {
			this.insertUserCnt = insertUserCnt;
			this.updateUserCnt = updateUserCnt;
		}
		final int insertUserCnt;
		final int updateUserCnt;
	}
	
	private static UpdateUserResult updateDBUserNodeMap(Connection dbConn, long companyId, ImmutableMap<String, UserNode> userNodeMap, long adminId, int now) throws SQLException {

		Set<Long> allModifyUserIdSet = new TreeSet<Long>();
		
		List<UserProtos.UserBase> insertUserBaseList = new ArrayList<UserProtos.UserBase>();
		Map<Long, UserProtos.UserBase> updateOldUserBaseMap = new HashMap<Long, UserProtos.UserBase>();
		Map<Long, UserProtos.UserBase> updateNewUserBaseMap = new HashMap<Long, UserProtos.UserBase>();
		
		UserProtos.UserBase.Builder tmpUserBaseBuilder = UserProtos.UserBase.newBuilder();
		for (UserNode userNode : userNodeMap.values()) {
			tmpUserBaseBuilder.clear();
			
			if (userNode.userBase == null) {
				
				tmpUserBaseBuilder.setUserId(0);
				tmpUserBaseBuilder.setRawId(userNode.rawUser.getRawId());
				tmpUserBaseBuilder.setUserName(userNode.rawUser.getUserName());
				if (userNode.rawUser.hasGender()) {
					if ("男".equals(userNode.rawUser.getGender())) {
						tmpUserBaseBuilder.setGender(UserProtos.UserBase.Gender.MALE);
					} else if ("女".equals(userNode.rawUser.getGender())) {
						tmpUserBaseBuilder.setGender(UserProtos.UserBase.Gender.FEMALE);
					}
				}
				
				tmpUserBaseBuilder.addAllMobileNo(userNode.rawUser.getMobileNoList());
				tmpUserBaseBuilder.addAllPhoneNo(userNode.rawUser.getPhoneNoList());
				
				if (userNode.rawUser.hasEmail()) {
					tmpUserBaseBuilder.setEmail(userNode.rawUser.getEmail());
				}
				if (userNode.rawUser.hasIsExpert()) {
					tmpUserBaseBuilder.setIsExpert(userNode.rawUser.getIsExpert());
				}
				
				if (userNode.levelNode != null) {
					tmpUserBaseBuilder.setLevelId(userNode.levelNode.level.getLevelId());
				}
				
				tmpUserBaseBuilder.setState(UserProtos.UserBase.State.NORMAL);
				tmpUserBaseBuilder.setCreateAdminId(adminId);
				tmpUserBaseBuilder.setCreateTime(now);

				insertUserBaseList.add(tmpUserBaseBuilder.build());
			} else if (!checkUserEquals(userNode.userBase, userNode.rawUser, userNode.levelNode)) {
				
				tmpUserBaseBuilder.mergeFrom(userNode.userBase);
				tmpUserBaseBuilder.setUserName(userNode.rawUser.getUserName());
				
				tmpUserBaseBuilder.clearGender();
				if (userNode.rawUser.hasGender()) {
					if ("男".equals(userNode.rawUser.getGender())) {
						tmpUserBaseBuilder.setGender(UserProtos.UserBase.Gender.MALE);
					} else if ("女".equals(userNode.rawUser.getGender())) {
						tmpUserBaseBuilder.setGender(UserProtos.UserBase.Gender.FEMALE);
					}
				}
				
				tmpUserBaseBuilder.clearMobileNo();
				tmpUserBaseBuilder.addAllMobileNo(userNode.rawUser.getMobileNoList());
				tmpUserBaseBuilder.clearPhoneNo();
				tmpUserBaseBuilder.addAllPhoneNo(userNode.rawUser.getPhoneNoList());
				
				if (userNode.rawUser.hasEmail()) {
					tmpUserBaseBuilder.setEmail(userNode.rawUser.getEmail());
				} else {
					tmpUserBaseBuilder.clearEmail();
				}
				if (userNode.rawUser.hasIsExpert()) {
					tmpUserBaseBuilder.setIsExpert(userNode.rawUser.getIsExpert());
				} else {
					tmpUserBaseBuilder.clearIsExpert();
				}
				if (userNode.levelNode != null && userNode.levelNode.level != null) {
					tmpUserBaseBuilder.setLevelId(userNode.levelNode.level.getLevelId());
				} else {
					tmpUserBaseBuilder.clearLevelId();
				}
				
				tmpUserBaseBuilder.setUpdateAdminId(adminId);
				tmpUserBaseBuilder.setUpdateTime(now);
				
				long userId = userNode.userBase.getUserId();
				updateOldUserBaseMap.put(userId, userNode.userBase);
				updateNewUserBaseMap.put(userId, tmpUserBaseBuilder.build());
			}
		}
		
		UserBaseDB.updateUserBase(dbConn, companyId, updateOldUserBaseMap, updateNewUserBaseMap);
		List<Long> insertUserIdList = UserBaseDB.insertUserBase(dbConn, companyId, insertUserBaseList);
		
		for (int i=0; i<insertUserBaseList.size(); ++i) {
			tmpUserBaseBuilder.clear();
			
			tmpUserBaseBuilder.mergeFrom(insertUserBaseList.get(i));
			tmpUserBaseBuilder.setUserId(insertUserIdList.get(i));
			
			UserNode userNode = userNodeMap.get(tmpUserBaseBuilder.getRawId());
			if (userNode != null && userNode.userBase == null) {
				userNode.userBase = tmpUserBaseBuilder.build();
			}
		}
		
		allModifyUserIdSet.addAll(updateNewUserBaseMap.keySet());
		
		Map<Long, List<UserProtos.UserTeam>> oldUserTeamMap = new HashMap<Long, List<UserProtos.UserTeam>>(userNodeMap.size());
		Map<Long, List<UserProtos.UserTeam>> newUserTeamMap = new HashMap<Long, List<UserProtos.UserTeam>>(userNodeMap.size());
		
		UserProtos.UserTeam.Builder tmpUserTeamBuilder = UserProtos.UserTeam.newBuilder();
		for (UserNode userNode : userNodeMap.values()) {
			long userId = userNode.userBase.getUserId();
			
			List<UserProtos.UserTeam> oldUserTeamList;
			if (userNode.userTeamList == null ) {
				oldUserTeamList = Collections.<UserProtos.UserTeam> emptyList();
			} else {
				oldUserTeamList = userNode.userTeamList;
			}
		
			List<UserProtos.UserTeam> newUserTeamList;
			if (userNode.userTeamNodeList == null) {
				newUserTeamList = Collections.<UserProtos.UserTeam> emptyList();
			} else {
				List<UserProtos.UserTeam> list = new ArrayList<UserProtos.UserTeam>(userNode.userTeamNodeList.size());
				for (UserTeamNode userTeamNode : userNode.userTeamNodeList) {
					tmpUserTeamBuilder.clear();
					
					tmpUserTeamBuilder.setUserId(userId);
					tmpUserTeamBuilder.setTeamId(userTeamNode.teamNode.team.getTeamId());
					if (userTeamNode.positionNode != null) {
						tmpUserTeamBuilder.setPositionId(userTeamNode.positionNode.position.getPositionId());
					}
					
					list.add(tmpUserTeamBuilder.build());
				}
				newUserTeamList = list;
			}
			
			if (!oldUserTeamList.containsAll(newUserTeamList) || !oldUserTeamList.containsAll(oldUserTeamList)) {
				allModifyUserIdSet.add(userId);
			}
			
			oldUserTeamMap.put(userId, oldUserTeamList);
			newUserTeamMap.put(userId, newUserTeamList);
		}
		
		TeamDB.updateUserTeam(dbConn, companyId, oldUserTeamMap, newUserTeamMap);
		
		Map<Long, List<UserProtos.UserExtends>> oldUserExtendsMap = new HashMap<Long, List<UserProtos.UserExtends>>(userNodeMap.size());
		Map<Long, List<UserProtos.UserExtends>> newUserExtendsMap = new HashMap<Long, List<UserProtos.UserExtends>>(userNodeMap.size());
		
		UserProtos.UserExtends.Builder tmpUserExtendsBuilder = UserProtos.UserExtends.newBuilder();
		for (UserNode userNode : userNodeMap.values()) {
			long userId = userNode.userBase.getUserId();
			
			List<UserProtos.UserExtends> oldUserExtendsList;
			if (userNode.userExtendsList == null ) {
				oldUserExtendsList = Collections.<UserProtos.UserExtends> emptyList();
			} else {
				oldUserExtendsList = userNode.userExtendsList;
			}
		
			List<UserProtos.UserExtends> newUserExtendsList;
			if (userNode.userExtendsMap == null) {
				newUserExtendsList = Collections.<UserProtos.UserExtends> emptyList();
			} else {
				List<UserProtos.UserExtends> list = new ArrayList<UserProtos.UserExtends>(userNode.userExtendsMap.size());
				for (Entry<String, String> entry : userNode.userExtendsMap.entrySet()) {
					tmpUserExtendsBuilder.clear();
					
					tmpUserExtendsBuilder.setUserId(userId);
					tmpUserExtendsBuilder.setName(entry.getKey());
					tmpUserExtendsBuilder.setValue(entry.getValue());
					
					list.add(tmpUserExtendsBuilder.build());
				}
				newUserExtendsList = list;
			}
			
			if (!oldUserExtendsList.containsAll(newUserExtendsList) || !newUserExtendsList.containsAll(oldUserExtendsList)) {
				allModifyUserIdSet.add(userId);
			}
			
			oldUserExtendsMap.put(userId, oldUserExtendsList);
			newUserExtendsMap.put(userId, newUserExtendsList);
		}
		
		UserExtendsDB.updateUserExtends(dbConn, companyId, oldUserExtendsMap, newUserExtendsMap);
		
		Map<Long, Set<String>> oldAilityTagMap = new HashMap<Long, Set<String>>(userNodeMap.size());
		Map<Long, Set<String>> newAilityTagMap = new HashMap<Long, Set<String>>(userNodeMap.size());
		
		for (UserNode userNode : userNodeMap.values()) {
			long userId = userNode.userBase.getUserId();
			
			Set<String> oldAbilityTagSet;
			if (userNode.abilityTagSet == null ) {
				oldAbilityTagSet = Collections.<String> emptySet();
			} else {
				oldAbilityTagSet = userNode.abilityTagSet;
			}
			
			Set<String> newAbilityTagSet;
			if (userNode.newAbilityTagSet == null) {
				newAbilityTagSet = Collections.<String> emptySet();
			} else {
				newAbilityTagSet = userNode.newAbilityTagSet;
			}
			
			if (!oldAbilityTagSet.containsAll(newAbilityTagSet)) {
				allModifyUserIdSet.add(userId);
			}
			
			oldAilityTagMap.put(userId, oldAbilityTagSet);
			newAilityTagMap.put(userId, newAbilityTagSet);
		}
		
		AbilityTagDB.incrementalUpdateAbilityTag(dbConn, companyId, oldAilityTagMap, newAilityTagMap, null, now);
		
		allModifyUserIdSet.removeAll(insertUserIdList);
		
		return new UpdateUserResult(insertUserBaseList.size(), allModifyUserIdSet.size());
	}
	
	private static boolean checkUserEquals(UserProtos.UserBase userBase, RawUser rawUser, LevelNode levelNode) {
		if (!userBase.getRawId().equals(rawUser.getRawId())) {
			return false;
		}
		
		if (!userBase.getUserName().equals(rawUser.getUserName())) {
			return false;
		}
		
		if (userBase.hasGender() != rawUser.hasGender()) {
			return false;
		}
		
		if (userBase.hasGender() && rawUser.hasGender()) {
			if (userBase.getGender() == UserProtos.UserBase.Gender.MALE && !"男".equals(rawUser.getGender())) {
				return false;
			}
			if (userBase.getGender() == UserProtos.UserBase.Gender.FEMALE && !"女".equals(rawUser.getGender())) {
				return false;
			}
		}
		
		if (userBase.hasEmail() != rawUser.hasEmail()) {
			return false;
		}
		
		if (userBase.hasEmail() && rawUser.hasEmail() && !userBase.getEmail().equals(rawUser.getEmail())) {
			return false;
		}
		
		if (userBase.hasIsExpert() != rawUser.hasIsExpert()) {
			return false;
		}
		
		if (userBase.hasIsExpert() && rawUser.hasIsExpert() && userBase.getIsExpert() != rawUser.getIsExpert()) {
			return false;
		}
		
		if (userBase.hasLevelId() != (levelNode != null)) {
			return false;
		}
		
		if (userBase.hasLevelId() && (levelNode != null) && userBase.getLevelId() != levelNode.level.getLevelId()) {
			return false;
		}
		
		return true;
	}

	private static class TeamNode {
		final String teamName;
		final ImmutableMap<String, TeamNode> subTeamNode;
		UserProtos.Team team = null;
		
		TeamNode(String teamName, ImmutableMap<String, TeamNode> subTeamNode) {
			this.teamName = teamName;
			this.subTeamNode = subTeamNode;
		}
	}
	
	private static class PositionNode {
		final String positionName;
		UserProtos.Position position = null;
		
		PositionNode(String positionName) {
			this.positionName = positionName;
		}
	}
	
	private static class UserTeamNode {
		final TeamNode teamNode;
		final PositionNode positionNode;
		
		UserTeamNode(TeamNode teamNode, PositionNode positionNode) {
			this.teamNode = teamNode;
			this.positionNode = positionNode;
		}
	}
	
	private static class LevelNode {
		final String levelName;
		UserProtos.Level level = null;
		
		public LevelNode(String levelName) {
			this.levelName = levelName;
		}
	}
	
	private static class UserNode {
		final int index;
		final RawUser rawUser;
		final LevelNode levelNode;
		final ImmutableList<UserTeamNode> userTeamNodeList;
		final ImmutableMap<String, String> userExtendsMap;
		final ImmutableSet<String> newAbilityTagSet;
		
		UserProtos.UserBase userBase;
		List<UserProtos.UserTeam> userTeamList;
		List<UserProtos.UserExtends> userExtendsList;
		Set<String> abilityTagSet;
		
		public UserNode(int index, RawUser rawUser, LevelNode levelNode, ImmutableList<UserTeamNode> userTeamNodeList, ImmutableMap<String, String> userExtendsMap, ImmutableSet<String> newAbilityTagSet) {
			this.index = index;
			this.rawUser = rawUser;
			this.levelNode = levelNode;
			this.userTeamNodeList = userTeamNodeList;
			this.userExtendsMap = userExtendsMap;
			this.newAbilityTagSet = newAbilityTagSet;
		}
	}
	
}
