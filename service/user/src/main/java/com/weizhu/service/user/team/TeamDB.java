package com.weizhu.service.user.team;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;

import javax.annotation.Nullable;

import com.weizhu.common.db.DBUtil;
import com.weizhu.common.db.ProtobufMapper;
import com.weizhu.proto.UserProtos;

public class TeamDB {
	
	public static List<Long> getAllUserId(Connection conn, long companyId) throws SQLException {
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try {
			pstmt = conn.prepareStatement("SELECT user_id FROM weizhu_user_base WHERE company_id = ? ORDER BY user_id ASC; ");
			pstmt.setLong(1, companyId);
			
			rs = pstmt.executeQuery();
			
			List<Long> userIdList = new ArrayList<Long>();
			while (rs.next()) {
				userIdList.add(rs.getLong("user_id"));
			}
			return userIdList;
		} finally {
			DBUtil.closeQuietly(rs);
			DBUtil.closeQuietly(pstmt);
		}
	}
	
	public static Set<Long> getAllTeamCompanyId(Connection conn) throws SQLException {
		Statement stmt = null;
		ResultSet rs = null;
		try {
			stmt = conn.createStatement();
			rs = stmt.executeQuery("SELECT DISTINCT(company_id) AS id FROM weizhu_team; ");
			
			Set<Long> companyIdSet = new TreeSet<Long>();
			while (rs.next()) {
				companyIdSet.add(rs.getLong("id"));
			}
			return companyIdSet;
		} finally {
			DBUtil.closeQuietly(rs);
			DBUtil.closeQuietly(stmt);
		}
	}

	private static final ProtobufMapper<UserProtos.Team> TEAM_MAPPER = 
			ProtobufMapper.createMapper(UserProtos.Team.getDefaultInstance(), 
					"team_id", 
					"team_name",
					"parent_team_id",
					"state",
					"create_admin_id",
					"create_time",
					"update_admin_id",
					"update_time"
					);
	
	public static Map<Integer, UserProtos.Team> getAllTeam(Connection conn, long companyId, @Nullable Set<UserProtos.State> stateSet) throws SQLException {
		if (stateSet != null && stateSet.isEmpty()) {
			return Collections.emptyMap();
		}
		
		StringBuilder sqlBuilder = new StringBuilder();
		sqlBuilder.append("SELECT * FROM weizhu_team WHERE company_id = ");
		sqlBuilder.append(companyId);
		
		if (stateSet != null) {
			sqlBuilder.append(" AND state IN ('");
			DBUtil.QUOTE_COMMA_JOINER.appendTo(sqlBuilder, stateSet);
			sqlBuilder.append("')");
		}
		
		sqlBuilder.append(" ORDER BY team_id ASC; ");
		
		String sql = sqlBuilder.toString();
		
		Statement stmt = null;
		ResultSet rs = null;
		try {
			stmt = conn.createStatement();
			rs = stmt.executeQuery(sql);
			
			Map<Integer, UserProtos.Team> resultMap = new LinkedHashMap<Integer, UserProtos.Team>();
			
			UserProtos.Team.Builder tmpTeamBuilder = UserProtos.Team.newBuilder();
			while (rs.next()) {
				tmpTeamBuilder.clear();
				
				UserProtos.Team team = TEAM_MAPPER.mapToItem(rs, tmpTeamBuilder).build();
				resultMap.put(team.getTeamId(), team);
			}
			
			return resultMap;
			
		} finally {
			DBUtil.closeQuietly(rs);
			DBUtil.closeQuietly(stmt);
		}
	}
	
	private static final ProtobufMapper<UserProtos.UserTeam> USER_TEAM_MAPPER = 
			ProtobufMapper.createMapper(UserProtos.UserTeam.getDefaultInstance(), 
					"user_id", 
					"team_id",
					"position_id"
					);
	
	public static Map<Long, List<UserProtos.UserTeam>> getAllUserTeam(Connection conn, long companyId) throws SQLException {
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try {
			pstmt = conn.prepareStatement("SELECT user_id, team_id, position_id FROM weizhu_user_team WHERE company_id = ? ORDER BY user_id ASC, team_id ASC; ");
			pstmt.setLong(1, companyId);
			
			rs = pstmt.executeQuery();
			
			Map<Long, List<UserProtos.UserTeam>> resultMap = new LinkedHashMap<Long, List<UserProtos.UserTeam>>();
			
			UserProtos.UserTeam.Builder tmpUserTeamBuilder = UserProtos.UserTeam.newBuilder();
			while (rs.next()) {
				tmpUserTeamBuilder.clear();
				
				UserProtos.UserTeam userTeam = USER_TEAM_MAPPER.mapToItem(rs, tmpUserTeamBuilder).build();
				DBUtil.addMapLinkedList(resultMap, userTeam.getUserId(), userTeam);
			}
			
			return resultMap;
		} finally {
			DBUtil.closeQuietly(rs);
			DBUtil.closeQuietly(pstmt);
		}
	}
	
	/* admin */
	
	public static List<Integer> insertTeam(Connection conn, long companyId, List<UserProtos.Team> teamList) throws SQLException {
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try {
			pstmt = conn.prepareStatement("INSERT INTO weizhu_team (company_id, team_id, team_name, parent_team_id, state, create_admin_id, create_time, update_admin_id, update_time) VALUES (?, NULL, ?, ?, ?, ?, ?, ?, ?); ", Statement.RETURN_GENERATED_KEYS);
			
			for (UserProtos.Team team : teamList) {
				DBUtil.set(pstmt, 1, true, companyId);
				DBUtil.set(pstmt, 2, team.hasTeamName(), team.getTeamName());
				DBUtil.set(pstmt, 3, team.hasParentTeamId(), team.getParentTeamId());
				DBUtil.set(pstmt, 4, team.hasState(), team.getState());
				DBUtil.set(pstmt, 5, team.hasCreateAdminId(), team.getCreateAdminId());
				DBUtil.set(pstmt, 6, team.hasCreateTime(), team.getCreateTime());
				DBUtil.set(pstmt, 7, team.hasUpdateAdminId(), team.getUpdateAdminId());
				DBUtil.set(pstmt, 8, team.hasUpdateTime(), team.getUpdateTime());
				
				pstmt.addBatch();
			}
			
			pstmt.executeBatch();
			
			rs = pstmt.getGeneratedKeys();
			
			List<Integer> teamIdList = new ArrayList<Integer>(teamList.size());
			while (rs.next()) {
				teamIdList.add(rs.getInt(1));
			}
			
			if (teamIdList.size() != teamList.size()) {
				throw new RuntimeException("insert fail");
			}
			
			return teamIdList;
			
		} finally {
			DBUtil.closeQuietly(rs);
			DBUtil.closeQuietly(pstmt);
		}
	}
	
	public static void updateTeam(Connection conn, 
			long companyId, 
			Map<Integer, UserProtos.Team> oldTeamMap, 
			Map<Integer, UserProtos.Team> newTeamMap
			) throws SQLException {

		// 必须保证key完全一样
		if (!oldTeamMap.keySet().equals(newTeamMap.keySet())) {
			throw new IllegalArgumentException("update key not equals");
		}
		
		PreparedStatement pstmt = null;
		try {
			pstmt = conn.prepareStatement("UPDATE weizhu_team SET team_name = ?, parent_team_id = ?, state = ?, update_admin_id = ?, update_time = ? WHERE company_id = ? AND team_id = ?; ");
			
			for (Entry<Integer, UserProtos.Team> entry : oldTeamMap.entrySet()) {
				UserProtos.Team oldValue = entry.getValue();
				UserProtos.Team newValue = newTeamMap.get(entry.getKey());
				
				if (!oldValue.equals(newValue)) {
					
					DBUtil.set(pstmt, 1, newValue.hasTeamName(),      newValue.getTeamName());
					DBUtil.set(pstmt, 2, newValue.hasParentTeamId(),  newValue.getParentTeamId());
					DBUtil.set(pstmt, 3, newValue.hasState(),         newValue.getState());
					DBUtil.set(pstmt, 4, newValue.hasUpdateAdminId(), newValue.getUpdateAdminId());
					DBUtil.set(pstmt, 5, newValue.hasUpdateTime(),    newValue.getUpdateTime());
					
					DBUtil.set(pstmt, 6, true, companyId);
					DBUtil.set(pstmt, 7, newValue.hasTeamId(), newValue.getTeamId());
					
					pstmt.addBatch();
				}
			}
			pstmt.executeBatch();
		} finally {
			DBUtil.closeQuietly(pstmt);
		}
	}
	
	public static void deleteTeam(Connection conn, long companyId, Collection<Integer> teamIds, @Nullable Long updateAdminId, @Nullable Integer updateTime) throws SQLException {
		if (teamIds.isEmpty()) {
			return;
		}
		
		PreparedStatement pstmt = null;
		try {
			pstmt = conn.prepareStatement("UPDATE weizhu_team SET state = 'DELETE', update_admin_id = ?, update_time = ? WHERE company_id = ? AND team_id = ?; DELETE FROM weizhu_user_team WHERE company_id = ? AND team_id = ?; ");
			
			for (Integer teamId : teamIds) {
				DBUtil.set(pstmt, 1, updateAdminId != null, updateAdminId == null ? 0 : updateAdminId);
				DBUtil.set(pstmt, 2, updateTime != null, updateTime == null ? 0 : updateTime);
				DBUtil.set(pstmt, 3, true, companyId);
				DBUtil.set(pstmt, 4, true, teamId);
				DBUtil.set(pstmt, 5, true, companyId);
				DBUtil.set(pstmt, 6, true, teamId);
				
				pstmt.addBatch();
			}
			pstmt.executeBatch();
		} finally {
			DBUtil.closeQuietly(pstmt);
		}
	}
	
	public static Map<Long, List<UserProtos.UserTeam>> getUserTeam(Connection conn, long companyId, Collection<Long> userIds) throws SQLException {
		if (userIds.isEmpty()) {
			return Collections.emptyMap();
		}
		
		StringBuilder sqlBuilder = new StringBuilder();
		sqlBuilder.append("SELECT user_id, team_id, position_id FROM weizhu_user_team WHERE company_id = ");
		sqlBuilder.append(companyId).append(" AND user_id IN (");
		DBUtil.COMMA_JOINER.appendTo(sqlBuilder, userIds);
		sqlBuilder.append(") ORDER BY user_id ASC, team_id ASC; ");
		
		String sql = sqlBuilder.toString();
		
		Statement stmt = null;
		ResultSet rs = null;
		try {
			stmt = conn.createStatement();
			rs = stmt.executeQuery(sql);
			
			Map<Long, List<UserProtos.UserTeam>> userTeamMap = new HashMap<Long, List<UserProtos.UserTeam>>(userIds.size());
			
			UserProtos.UserTeam.Builder tmpUserTeamBuilder = UserProtos.UserTeam.newBuilder();
			while (rs.next()) {
				tmpUserTeamBuilder.clear();
				
				UserProtos.UserTeam userTeam = USER_TEAM_MAPPER.mapToItem(rs, tmpUserTeamBuilder).build();
				
				DBUtil.addMapArrayList(userTeamMap, userTeam.getUserId(), userTeam);
			}
			
			for (Long userId : userIds) {
				if (!userTeamMap.containsKey(userId)) {
					userTeamMap.put(userId, Collections.<UserProtos.UserTeam> emptyList());
				}
			}

			return userTeamMap;
		} finally {
			DBUtil.closeQuietly(rs);
			DBUtil.closeQuietly(stmt);
		}
	}
	
	public static void updateUserTeam(Connection conn, 
			final long companyId,
			Map<Long, List<UserProtos.UserTeam>> oldUserTeamMap, 
			Map<Long, List<UserProtos.UserTeam>> newUserTeamMap
			) throws SQLException {
		
		// 必须保证key完全一样
		if (!oldUserTeamMap.keySet().equals(newUserTeamMap.keySet())) {
			throw new IllegalArgumentException("update key not equals");
		}
		
		PreparedStatement pstmt = null;
		try {
			pstmt = conn.prepareStatement("DELETE FROM weizhu_user_team WHERE company_id = ? AND user_id = ? AND team_id = ?; ");
			
			for (Entry<Long, List<UserProtos.UserTeam>> entry : oldUserTeamMap.entrySet()) {
				List<UserProtos.UserTeam> newUserTeamList = newUserTeamMap.get(entry.getKey());
				for (UserProtos.UserTeam oldUserTeam : entry.getValue()) {
					boolean find = false;
					for (UserProtos.UserTeam newUserTeam : newUserTeamList) {
						if (oldUserTeam.getUserId() == newUserTeam.getUserId() 
								&& oldUserTeam.getTeamId() == newUserTeam.getTeamId()) {
							find = true;
							break;
						}
					}
					
					if (!find) {
						DBUtil.set(pstmt, 1, true, companyId);
						DBUtil.set(pstmt, 2, oldUserTeam.hasUserId(), oldUserTeam.getUserId());
						DBUtil.set(pstmt, 3, oldUserTeam.hasTeamId(), oldUserTeam.getTeamId());
						
						pstmt.addBatch();
					}
				}
			}
			
			pstmt.executeBatch();
		} finally {
			DBUtil.closeQuietly(pstmt);
		}
		
		pstmt = null;
		try {
			pstmt = conn.prepareStatement("REPLACE INTO weizhu_user_team (company_id, user_id, team_id, position_id) VALUES (?, ?, ?, ?); ");
			
			for (Entry<Long, List<UserProtos.UserTeam>> entry : newUserTeamMap.entrySet()) {
				List<UserProtos.UserTeam> oldUserTeamList = oldUserTeamMap.get(entry.getKey());
				for (UserProtos.UserTeam newUserTeam : entry.getValue()) {
					boolean find = false;
					for (UserProtos.UserTeam oldUserTeam : oldUserTeamList) {
						if (newUserTeam.equals(oldUserTeam)) {
							find = true;
							break;
						}
					}
					
					if (!find) {
						DBUtil.set(pstmt, 1, true,                        companyId);
						DBUtil.set(pstmt, 2, newUserTeam.hasUserId(),     newUserTeam.getUserId());
						DBUtil.set(pstmt, 3, newUserTeam.hasTeamId(),     newUserTeam.getTeamId());
						DBUtil.set(pstmt, 4, newUserTeam.hasPositionId(), newUserTeam.getPositionId());
						
						pstmt.addBatch();
					}
				}
			}
			
			pstmt.executeBatch();
		} finally {
			DBUtil.closeQuietly(pstmt);
		}
	}
	
	public static void deleteUserTeam(Connection conn, long companyId, Collection<Long> userIds) throws SQLException {
		if (userIds.isEmpty()) {
			return;
		}
		
		PreparedStatement pstmt = null;
		try {
			pstmt = conn.prepareStatement("DELETE FROM weizhu_user_team WHERE company_id = ? AND user_id = ?; ");
			
			for (long userId : userIds) {
				DBUtil.set(pstmt, 1, true, companyId);
				DBUtil.set(pstmt, 2, true, userId);
				
				pstmt.addBatch();
			}
			
			pstmt.executeBatch();
		} finally {
			DBUtil.closeQuietly(pstmt);
		}
	}
	
}