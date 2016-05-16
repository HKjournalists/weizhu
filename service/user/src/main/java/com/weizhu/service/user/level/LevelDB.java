package com.weizhu.service.user.level;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import javax.annotation.Nullable;

import com.weizhu.common.db.DBUtil;
import com.weizhu.common.db.ProtobufMapper;
import com.weizhu.proto.UserProtos;

public class LevelDB {

	private static final ProtobufMapper<UserProtos.Level> LEVEL_MAPPER = 
			ProtobufMapper.createMapper(UserProtos.Level.getDefaultInstance(), 
					"level_id", 
					"level_name",
					"state",
					"create_admin_id",
					"create_time",
					"update_admin_id",
					"update_time"
					);
	
	public static Map<Integer, UserProtos.Level> getAllLevel(Connection conn, long companyId, @Nullable Set<UserProtos.State> stateSet) throws SQLException {
		if (stateSet != null && stateSet.isEmpty()) {
			return Collections.emptyMap();
		}
		
		StringBuilder sqlBuilder = new StringBuilder();
		sqlBuilder.append("SELECT * FROM weizhu_level WHERE company_id = ");
		sqlBuilder.append(companyId);
		
		if (stateSet != null) {
			sqlBuilder.append(" AND state IN ('");
			DBUtil.QUOTE_COMMA_JOINER.appendTo(sqlBuilder, stateSet);
			sqlBuilder.append("')");
		}
	
		sqlBuilder.append(" ORDER BY level_id ASC; ");
		
		String sql = sqlBuilder.toString();
		
		Statement stmt = null;
		ResultSet rs = null;
		try {
			stmt = conn.createStatement();
			rs = stmt.executeQuery(sql);
			
			Map<Integer, UserProtos.Level> resultMap = new LinkedHashMap<Integer, UserProtos.Level>();
			
			UserProtos.Level.Builder tmpLevelBuilder = UserProtos.Level.newBuilder();
			while (rs.next()) {
				tmpLevelBuilder.clear();
				
				UserProtos.Level level = LEVEL_MAPPER.mapToItem(rs, tmpLevelBuilder).build();
				resultMap.put(level.getLevelId(), level);
			}
			
			return resultMap;
		} finally {
			DBUtil.closeQuietly(rs);
			DBUtil.closeQuietly(stmt);
		}
	}
	
	public static List<Integer> insertLevel(Connection conn, long companyId, List<UserProtos.Level> levelList) throws SQLException {
		if (levelList.isEmpty()) {
			return Collections.emptyList();
		}
		
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try {
			pstmt = conn.prepareStatement("INSERT INTO weizhu_level (company_id, level_id, level_name, state, create_admin_id, create_time, update_admin_id, update_time) VALUES (?, NULL, ?, ?, ?, ?, ?, ?); ", Statement.RETURN_GENERATED_KEYS);
			
			for (UserProtos.Level level : levelList) {
				
				DBUtil.set(pstmt, 1, true, companyId);
				DBUtil.set(pstmt, 2, level.hasLevelName(), level.getLevelName());
				DBUtil.set(pstmt, 3, level.hasState(), level.getState());
				DBUtil.set(pstmt, 4, level.hasCreateAdminId(), level.getCreateAdminId());
				DBUtil.set(pstmt, 5, level.hasCreateTime(), level.getCreateTime());
				DBUtil.set(pstmt, 6, level.hasUpdateAdminId(), level.getUpdateAdminId());
				DBUtil.set(pstmt, 7, level.hasUpdateTime(), level.getUpdateTime());
				
				pstmt.addBatch();
			}
			
			pstmt.executeBatch();
			
			rs = pstmt.getGeneratedKeys();
			
			List<Integer> levelIdList = new ArrayList<Integer>(levelList.size());
			while (rs.next()) {
				levelIdList.add(rs.getInt(1));
			}
			
			if (levelIdList.size() != levelList.size()) {
				throw new RuntimeException("insert fail");
			}
			
			return levelIdList;
			
		} finally {
			DBUtil.closeQuietly(rs);
			DBUtil.closeQuietly(pstmt);
		}
	}
	
	/**
	 * @param conn
	 * @param oldUserBaseMap  不要包含null元素
	 * @param newUserBaseMap  不要包含null元素
	 * @throws SQLException
	 */
	public static void updateLevel(Connection conn, 
			long companyId, 
			Map<Integer, UserProtos.Level> oldLevelMap, 
			Map<Integer, UserProtos.Level> newLevelMap
			) throws SQLException {
		
		// 必须保证key完全一样
		if (!oldLevelMap.keySet().equals(newLevelMap.keySet())) {
			throw new IllegalArgumentException("update key not equals");
		}
		
		PreparedStatement pstmt = null;
		try {
			pstmt = conn.prepareStatement("UPDATE weizhu_level SET level_name = ?, state = ?, update_admin_id = ?, update_time = ? WHERE company_id = ? AND level_id = ?; ");
			
			for (Entry<Integer, UserProtos.Level> entry : oldLevelMap.entrySet()) {
				UserProtos.Level oldValue = entry.getValue();
				UserProtos.Level newValue = newLevelMap.get(entry.getKey());
				
				if (!oldValue.equals(newValue)) {
					DBUtil.set(pstmt, 1, newValue.hasLevelName(), newValue.getLevelName());
					DBUtil.set(pstmt, 2, newValue.hasState(), newValue.getState());
					DBUtil.set(pstmt, 3, newValue.hasUpdateAdminId(), newValue.getUpdateAdminId());
					DBUtil.set(pstmt, 4, newValue.hasUpdateTime(), newValue.getUpdateTime());
					
					DBUtil.set(pstmt, 5, true,   companyId);
					DBUtil.set(pstmt, 6, true,   newValue.getLevelId());
					
					pstmt.addBatch();
				}
			}
			pstmt.executeBatch();
		} finally {
			DBUtil.closeQuietly(pstmt);
		}
	}
	
	public static void deleteLevel(Connection conn, long companyId, Collection<Integer> levelIds, @Nullable Long updateAdminId, @Nullable Integer updateTime) throws SQLException {
		if (levelIds.isEmpty()) {
			return;
		}
		
		PreparedStatement pstmt = null;
		try {
			pstmt = conn.prepareStatement("UPDATE weizhu_level SET state = 'DELETE', update_admin_id = ?, update_time = ? WHERE company_id = ? AND level_id = ?; ");
			
			for (Integer levelId : levelIds) {
				
				DBUtil.set(pstmt, 1, updateAdminId != null, updateAdminId == null ? 0 : updateAdminId);
				DBUtil.set(pstmt, 2, updateTime != null, updateTime == null ? 0 : updateTime);
				DBUtil.set(pstmt, 3, true, companyId);
				DBUtil.set(pstmt, 4, true, levelId);
				
				pstmt.addBatch();
			}
			pstmt.executeBatch();
		} finally {
			DBUtil.closeQuietly(pstmt);
		}
	}
	
}
