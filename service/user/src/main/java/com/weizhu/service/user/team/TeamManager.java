package com.weizhu.service.user.team;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.weizhu.common.db.DBUtil;
import com.weizhu.common.jedis.JedisTaskLoader;
import com.weizhu.proto.UserProtos;
import com.weizhu.proto.AdminProtos.AdminHead;
import com.weizhu.proto.AdminUserProtos.CreateTeamRequest;
import com.weizhu.proto.AdminUserProtos.CreateTeamResponse;
import com.weizhu.proto.AdminUserProtos.DeleteTeamRequest;
import com.weizhu.proto.AdminUserProtos.DeleteTeamResponse;
import com.weizhu.proto.AdminUserProtos.UpdateTeamRequest;
import com.weizhu.proto.AdminUserProtos.UpdateTeamResponse;
import com.zaxxer.hikari.HikariDataSource;

@Singleton
public class TeamManager {

	private static final Logger logger = LoggerFactory.getLogger(TeamManager.class);
	
	private static final String TEAM_LOAD_TYPE = "user:team:load";
	private static final ImmutableSet<UserProtos.State> STATE_SET = ImmutableSet.of(UserProtos.State.NORMAL);
	private static final TeamData EMPTY_TEAM_DATA = new TeamData();
	
	private final HikariDataSource hikariDataSource;
	private final Executor serviceExecutor;
	private final JedisTaskLoader jedisTaskLoader;
	
	private final ConcurrentHashMap<Long, TeamData> teamDataMap = new ConcurrentHashMap<Long, TeamData>();
	
	@Inject
	public TeamManager(
			HikariDataSource hikariDataSource, 
			@Named("service_executor") Executor serviceExecutor,
			JedisTaskLoader jedisTaskLoader) {
		this.hikariDataSource = hikariDataSource;
		this.serviceExecutor = serviceExecutor;
		this.jedisTaskLoader = jedisTaskLoader;
		
		this.jedisTaskLoader.register(
				TEAM_LOAD_TYPE, 
				serviceExecutor, 
				new JedisTaskLoader.TaskFactory() {
					
					@Override
					public Runnable createTask(String key) {
						return new LoadTeamDataTask(Long.parseLong(key));
					}
				});
		
		this.serviceExecutor.execute(new Runnable() {

			@Override
			public void run() {
				try {
					Set<Long> companyIdSet;
					Connection dbConn = null;
					try {
						dbConn = TeamManager.this.hikariDataSource.getConnection();
						companyIdSet = TeamDB.getAllTeamCompanyId(dbConn);
					} finally {
						DBUtil.closeQuietly(dbConn);
					}
					
					for (Long companyId : companyIdSet) {
						TeamManager.this.jedisTaskLoader.notifyLoadLocal(TEAM_LOAD_TYPE, Long.toString(companyId));
					}
				} catch (Throwable th) {
					logger.error("init load all company team data fail", th);
				}
			}
			
		});
	}
	
	public TeamData getTeamData(long companyId) {
		TeamData result = teamDataMap.get(companyId);
		if (result == null) {
			this.jedisTaskLoader.notifyLoadLocal(TEAM_LOAD_TYPE, Long.toString(companyId));
			return EMPTY_TEAM_DATA;
		} else {
			return result;
		}
	}
	
	private class LoadTeamDataTask implements Runnable {

		private final long companyId;
		
		public LoadTeamDataTask(long companyId) {
			this.companyId = companyId;
		}
		
		@Override
		public void run() {
			logger.info("start load team data. company_id : " + companyId);
			
			Set<Long> userIdSet;
			Map<Integer, UserProtos.Team> teamMap;
			Map<Long, List<UserProtos.UserTeam>> userTeamMap;
			
			// 从DB中获取所有团队信息
			Connection dbConn = null;
			try {
				dbConn = TeamManager.this.hikariDataSource.getConnection();
				userIdSet = new TreeSet<Long>(TeamDB.getAllUserId(dbConn, this.companyId));
				teamMap = TeamDB.getAllTeam(dbConn, this.companyId, STATE_SET);
				userTeamMap = TeamDB.getAllUserTeam(dbConn, companyId);
			} catch (SQLException e) {
				throw new RuntimeException("TeamMananger load db fail", e);
			} finally {
				DBUtil.closeQuietly(dbConn);
			}
			
			// 检查数据
			
			teamMap = checkTeamMap(teamMap);
			userTeamMap = checkUserTeamMap(userTeamMap, teamMap, userIdSet);
			
			// 填装最后数据结果
			
			List<Integer> rootTeamIdList = new ArrayList<Integer>();
			
			Map<Integer, List<Integer>> teamIdToSubTeamIdListMap = new HashMap<Integer, List<Integer>>(teamMap.size());
			for (UserProtos.Team team : teamMap.values()) {
				if (team.hasParentTeamId()) {
					List<Integer> list = teamIdToSubTeamIdListMap.get(team.getParentTeamId());
					if (list == null) {
						list = new ArrayList<Integer>();
						teamIdToSubTeamIdListMap.put(team.getParentTeamId(), list);
					}
					list.add(team.getTeamId());
				} else {
					rootTeamIdList.add(team.getTeamId());
				}
			}
			
			Map<Integer, List<UserProtos.UserTeam>> teamIdToSubUserTeamListMap = new HashMap<Integer, List<UserProtos.UserTeam>>(teamMap.size());
			for (List<UserProtos.UserTeam> userTeamList : userTeamMap.values()) {
				for (UserProtos.UserTeam userTeam : userTeamList) {
					if (teamMap.containsKey(userTeam.getTeamId())) {
						List<UserProtos.UserTeam> list = teamIdToSubUserTeamListMap.get(userTeam.getTeamId());
						if (list == null) {
							list = new ArrayList<UserProtos.UserTeam>();
							teamIdToSubUserTeamListMap.put(userTeam.getTeamId(), list);
						}
						list.add(userTeam);
					}
				}
			}
			
			Map<Integer, TeamInfo> teamInfoMap = new LinkedHashMap<Integer, TeamInfo>(teamMap.size());
			for (Entry<Integer, UserProtos.Team> entry : teamMap.entrySet()) {
				Integer teamId = entry.getKey();
				UserProtos.Team team = entry.getValue();
				
				List<Integer> subTeamIdList = teamIdToSubTeamIdListMap.get(teamId);
				if (subTeamIdList == null) {
					subTeamIdList = Collections.emptyList();
				}
				
				List<UserProtos.UserTeam> subUserTeamList = teamIdToSubUserTeamListMap.get(teamId);
				if (subUserTeamList == null) {
					subUserTeamList = Collections.emptyList();
				}
				
				teamInfoMap.put(teamId, new TeamInfo(team, subTeamIdList, subUserTeamList));
			}
			
			for (TeamInfo teamInfo : teamInfoMap.values()) {
				for (UserProtos.UserTeam userTeam : teamInfo.subUserTeamList) {
					userIdSet.remove(userTeam.getUserId());
				}
			}
			
			TeamManager.this.teamDataMap.put(companyId, new TeamData(rootTeamIdList, teamInfoMap, userTeamMap, userIdSet));
			
			logger.info("finish load team data. company_id : " + companyId + ", team size : " + teamInfoMap.size() + ", user team size : " + userTeamMap.size());
		}
		
		/**
		 * 校验团队信息的隶属关系: 必须为树形结构
		 * @param teamMap
		 * @return
		 */
		private Map<Integer, UserProtos.Team> checkTeamMap(Map<Integer, UserProtos.Team> teamMap) {
			List<Integer> rootTeamIdList = new ArrayList<Integer>();
			Map<Integer, List<Integer>> teamIdToSubTeamIdListMap = new HashMap<Integer, List<Integer>>(teamMap.size());
			for (UserProtos.Team team : teamMap.values()) {
				if (team.hasParentTeamId()) {
					List<Integer> list = teamIdToSubTeamIdListMap.get(team.getParentTeamId());
					if (list == null) {
						list = new ArrayList<Integer>();
						teamIdToSubTeamIdListMap.put(team.getParentTeamId(), list);
					}
					list.add(team.getTeamId());
				} else {
					rootTeamIdList.add(team.getTeamId());
				}
			}
			
			// 对组织结构树进行广度优先遍历，校验结构. 并将正确的team 放入到 tmpTeamMap 中
			Map<Integer, UserProtos.Team> tmpTeamMap = new LinkedHashMap<Integer, UserProtos.Team>(teamMap.size());
			
			Queue<Integer> queue = new LinkedList<Integer>();
			for (Integer teamId : rootTeamIdList) {
				UserProtos.Team team = teamMap.get(teamId);
				tmpTeamMap.put(teamId, team);
				
				queue.add(teamId);
			}
			
			while (!queue.isEmpty()) {
				Integer teamId = queue.remove();
				
				List<Integer> subTeamIdList = teamIdToSubTeamIdListMap.get(teamId);
				if (subTeamIdList != null) {
					for (Integer subTeamId : subTeamIdList) {
						UserProtos.Team subTeam = teamMap.get(subTeamId);
						tmpTeamMap.put(subTeamId, subTeam);
						
						queue.add(subTeamId);
					}
				}
			}
			
			return tmpTeamMap;
		}
		
		/**
		 * 去掉不正确的team关系
		 * @param userTeamMap
		 * @param teamMap
		 * @return
		 */
		private Map<Long, List<UserProtos.UserTeam>> checkUserTeamMap(Map<Long, List<UserProtos.UserTeam>> userTeamMap, Map<Integer, UserProtos.Team> teamMap, Set<Long> userIdSet) {
			Map<Long, List<UserProtos.UserTeam>> tmpUserTeamMap = new LinkedHashMap<Long, List<UserProtos.UserTeam>>(userTeamMap.size());
			
			for (Entry<Long, List<UserProtos.UserTeam>> entry : userTeamMap.entrySet()) {
				List<UserProtos.UserTeam> tmpUserTeamList = new ArrayList<UserProtos.UserTeam>(entry.getValue().size());
				for (UserProtos.UserTeam userTeam : entry.getValue()) {
					if (teamMap.containsKey(userTeam.getTeamId()) && userIdSet.contains(userTeam.getUserId())) {
						tmpUserTeamList.add(userTeam);
					}
				}
				tmpUserTeamMap.put(entry.getKey(), tmpUserTeamList);
			}
			return tmpUserTeamMap;
		}
	}
	
	public static final class TeamInfo {
		private final UserProtos.Team team;
		private final ImmutableList<Integer> subTeamIdList;
		private final ImmutableList<UserProtos.UserTeam> subUserTeamList;
		
		TeamInfo(UserProtos.Team team, List<Integer> subTeamIdList, List<UserProtos.UserTeam> subUserTeamList) {
			this.team = team;
			this.subTeamIdList = ImmutableList.copyOf(subTeamIdList);
			this.subUserTeamList = ImmutableList.copyOf(subUserTeamList);
		}

		public UserProtos.Team team() {
			return team;
		}

		public ImmutableList<Integer> subTeamIdList() {
			return subTeamIdList;
		}

		public ImmutableList<UserProtos.UserTeam> subUserTeamList() {
			return subUserTeamList;
		}
	}
	
	public static final class TeamData {
		
		private final ImmutableList<Integer> rootTeamIdList;
		private final ImmutableMap<Integer, TeamInfo> teamInfoMap;
		private final ImmutableMap<Long, ImmutableList<UserProtos.UserTeam>> userTeamMap;
		private final ImmutableList<Long> noTeamUserIdList;
		
		TeamData(List<Integer> rootTeamIdList, Map<Integer, TeamInfo> teamInfoMap, Map<Long, List<UserProtos.UserTeam>> userTeamMap, Set<Long> noTeamUserIdSet) {
			this.rootTeamIdList = ImmutableList.copyOf(rootTeamIdList);
			this.teamInfoMap = ImmutableMap.copyOf(teamInfoMap);
			
			Map<Long, ImmutableList<UserProtos.UserTeam>> userTeamMap0 = Maps.newTreeMap();
			for (Map.Entry<Long, List<UserProtos.UserTeam>> entry : userTeamMap.entrySet()) {
				userTeamMap0.put(entry.getKey(), ImmutableList.copyOf(entry.getValue()));
			}
			this.userTeamMap = ImmutableMap.copyOf(userTeamMap0);
			this.noTeamUserIdList = ImmutableList.copyOf(noTeamUserIdSet);
		}
		
		TeamData() {
			this.rootTeamIdList = ImmutableList.of();
			this.teamInfoMap = ImmutableMap.of();
			this.userTeamMap = ImmutableMap.of();
			this.noTeamUserIdList = ImmutableList.of();
		}
		
		public ImmutableList<Integer> rootTeamIdList() {
			return rootTeamIdList;
		}

		public ImmutableMap<Integer, TeamInfo> teamInfoMap() {
			return teamInfoMap;
		}

		public ImmutableMap<Long, ImmutableList<UserProtos.UserTeam>> userTeamMap() {
			return userTeamMap;
		}
		
		public ImmutableList<Long> noTeamUserIdList() {
			return noTeamUserIdList;
		}
	}
	
	/* admin */
	
	public void updateUserTeam(
			long companyId,
			Map<Long, List<UserProtos.UserTeam>> oldUserTeamMap, 
			Map<Long, List<UserProtos.UserTeam>> newUserTeamMap
			) {
		
		// 必须保证key完全一样
		if (!oldUserTeamMap.keySet().equals(newUserTeamMap.keySet())) {
			throw new IllegalArgumentException("update key not equals");
		}
		
		Connection dbConn = null;
		try {
			dbConn = this.hikariDataSource.getConnection();
			TeamDB.updateUserTeam(dbConn, companyId, oldUserTeamMap, newUserTeamMap);
		} catch (SQLException e) {
			throw new RuntimeException("db fail", e);
		} finally {
			DBUtil.closeQuietly(dbConn);
		}
		
		this.jedisTaskLoader.notifyLoad(TEAM_LOAD_TYPE, Long.toString(companyId));
	}
	
	public void deleteUserTeam(long companyId, Collection<Long> userIds) {
		if (userIds.isEmpty()) {
			return;
		}
		
		Connection dbConn = null;
		try {
			dbConn = this.hikariDataSource.getConnection();
			TeamDB.deleteUserTeam(dbConn, companyId, userIds);
		} catch (SQLException e) {
			throw new RuntimeException("db fail", e);
		} finally {
			DBUtil.closeQuietly(dbConn);
		}
		
		this.jedisTaskLoader.notifyLoad(TEAM_LOAD_TYPE, Long.toString(companyId));
	}
	
	public CreateTeamResponse createTeam(AdminHead head, CreateTeamRequest request) {
		if (!head.hasCompanyId()) {
			return CreateTeamResponse.newBuilder()
					.setResult(CreateTeamResponse.Result.FAIL_UNKNOWN)
					.setFailText("company_id 参数未填")
					.build();
		}
		
		final long companyId = head.getCompanyId();
		final long adminId = head.getSession().getAdminId();
		if (request.getTeamName().trim().isEmpty()) {
			return CreateTeamResponse.newBuilder()
					.setResult(CreateTeamResponse.Result.FAIL_NAME_INVALID)
					.setFailText("部门名称不能为空")
					.build();
		}
		if (request.getTeamName().length() > 100) {
			return CreateTeamResponse.newBuilder()
					.setResult(CreateTeamResponse.Result.FAIL_NAME_INVALID)
					.setFailText("部门名称过长")
					.build();
		}
		
		final TeamData teamData = this.getTeamData(companyId);
		if (request.hasParentTeamId() && !teamData.teamInfoMap().containsKey(request.getParentTeamId())) {
			return CreateTeamResponse.newBuilder()
					.setResult(CreateTeamResponse.Result.FAIL_PARAENT_INVALID)
					.setFailText("父部门不存在")
					.build();
		}
		
		UserProtos.Team.Builder teamBuilder = UserProtos.Team.newBuilder();
		teamBuilder.setTeamId(0);
		teamBuilder.setTeamName(request.getTeamName());
		if (request.hasParentTeamId()) {
			teamBuilder.setParentTeamId(request.getParentTeamId());
		}
		teamBuilder.setState(UserProtos.State.NORMAL);
		teamBuilder.setCreateAdminId(adminId);
		teamBuilder.setCreateTime((int) (System.currentTimeMillis() / 1000L));
		
		UserProtos.Team team = teamBuilder.build();
		
		int teamId;
		Connection dbConn = null;
		try {
			dbConn = hikariDataSource.getConnection();
			
			teamId = TeamDB.insertTeam(dbConn, companyId, Collections.singletonList(team)).get(0);
			
		} catch (SQLException e) {
			throw new RuntimeException("db fail", e);
		} finally {
			DBUtil.closeQuietly(dbConn);
		}
		
		this.jedisTaskLoader.notifyLoad(TEAM_LOAD_TYPE, Long.toString(companyId));
		
		return CreateTeamResponse.newBuilder()
				.setResult(CreateTeamResponse.Result.SUCC)
				.setTeamId(teamId)
				.build();
	}

	public UpdateTeamResponse updateTeam(AdminHead head, UpdateTeamRequest request) {
		if (!head.hasCompanyId()) {
			return UpdateTeamResponse.newBuilder()
					.setResult(UpdateTeamResponse.Result.FAIL_UNKNOWN)
					.setFailText("company_id 参数未填")
					.build();
		}
		
		final long companyId = head.getCompanyId();
		final long adminId = head.getSession().getAdminId();
		if (request.getTeamName().isEmpty()) {
			return UpdateTeamResponse.newBuilder()
					.setResult(UpdateTeamResponse.Result.FAIL_NAME_INVALID)
					.setFailText("部门名称不能为空")
					.build();
		}
		if (request.getTeamName().length() > 100) {
			return UpdateTeamResponse.newBuilder()
					.setResult(UpdateTeamResponse.Result.FAIL_NAME_INVALID)
					.setFailText("部门名称过长")
					.build();
		}
		
		final int teamId = request.getTeamId();
		
		final TeamData teamData = this.getTeamData(companyId);
		TeamInfo teamInfo = teamData.teamInfoMap().get(teamId);
		if (teamInfo == null) {
			return UpdateTeamResponse.newBuilder()
					.setResult(UpdateTeamResponse.Result.FAIL_TEAM_NOT_EXIST)
					.setFailText("部门不存在")
					.build();
		}
		
		final UserProtos.Team oldTeam = teamInfo.team();
		
		if (oldTeam.getTeamName().equals(request.getTeamName())) {
			return UpdateTeamResponse.newBuilder()
					.setResult(UpdateTeamResponse.Result.SUCC)
					.build();
		}
		
		final UserProtos.Team newTeam = oldTeam.toBuilder()
				.setTeamName(request.getTeamName())
				.setUpdateAdminId(adminId)
				.setUpdateTime((int) (System.currentTimeMillis() / 1000L))
				.build();

		Connection dbConn = null;
		try {
			dbConn = hikariDataSource.getConnection();
			
			TeamDB.updateTeam(dbConn,
					companyId,
					Collections.singletonMap(teamId, oldTeam), 
					Collections.singletonMap(teamId, newTeam)
					);
			
		} catch (SQLException e) {
			throw new RuntimeException("db fail", e);
		} finally {
			DBUtil.closeQuietly(dbConn);
		}
		
		this.jedisTaskLoader.notifyLoad(TEAM_LOAD_TYPE, Long.toString(companyId));
		
		return UpdateTeamResponse.newBuilder()
				.setResult(UpdateTeamResponse.Result.SUCC)
				.build();
	}

	public DeleteTeamResponse deleteTeam(AdminHead head, DeleteTeamRequest request) {
		if (!head.hasCompanyId()) {
			return DeleteTeamResponse.newBuilder()
					.setResult(DeleteTeamResponse.Result.FAIL_UNKNOWN)
					.setFailText("company_id 参数未填")
					.build();
		}
		
		final long companyId = head.getCompanyId();
		final long adminId = head.getSession().getAdminId();
		
		if (request.getTeamIdCount() <= 0) {
			return DeleteTeamResponse.newBuilder()
					.setResult(DeleteTeamResponse.Result.SUCC)
					.build();
		}
		
		final TeamData teamData = this.getTeamData(companyId);
		
		Set<Integer> deleteTeamIdSet = new TreeSet<Integer>();
		
		for (Integer teamId : request.getTeamIdList()) {
			TeamInfo teamInfo = teamData.teamInfoMap().get(teamId);
			if (teamInfo != null) {
				
				if (!request.getRecursive() && !teamInfo.subTeamIdList().isEmpty()) {
					return DeleteTeamResponse.newBuilder()
							.setResult(DeleteTeamResponse.Result.FAIL_HAS_SUB_TEAM)
							.setFailText("该部门下有子部门，请先删除子部门再操作 [" + teamId + ":" + teamInfo.team().getTeamName() + "]")
							.build();
				}
				
				deleteTeamIdSet.add(teamId);
			}
		}
		
		if (request.getRecursive()) {
			Queue<Integer> queue = new LinkedList<Integer>();
			queue.addAll(deleteTeamIdSet);
			while (!queue.isEmpty()) {
				Integer teamId = queue.remove();
				TeamInfo teamInfo = teamData.teamInfoMap().get(teamId);
				if (teamInfo != null) {
					deleteTeamIdSet.addAll(teamInfo.subTeamIdList());
					queue.addAll(teamInfo.subTeamIdList());
				}
			}
		}
		
		
		Connection dbConn = null;
		try {
			dbConn = hikariDataSource.getConnection();
			
			TeamDB.deleteTeam(dbConn, companyId, deleteTeamIdSet, adminId, (int) (System.currentTimeMillis() / 1000L));
			
		} catch (SQLException e) {
			throw new RuntimeException("db fail", e);
		} finally {
			DBUtil.closeQuietly(dbConn);
		}
		
		this.jedisTaskLoader.notifyLoad(TEAM_LOAD_TYPE, Long.toString(companyId));
		
		return DeleteTeamResponse.newBuilder()
				.setResult(DeleteTeamResponse.Result.SUCC)
				.build();
	}

	public void reloadTeam(AdminHead head) {
		if (!head.hasCompanyId()) {
			return;
		}
		this.jedisTaskLoader.notifyLoad(TEAM_LOAD_TYPE, Long.toString(head.getCompanyId()));
	}
	
}
