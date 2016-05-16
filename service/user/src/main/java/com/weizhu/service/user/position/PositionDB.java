package com.weizhu.service.user.position;

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
import java.util.Map.Entry;
import java.util.Set;

import javax.annotation.Nullable;

import com.weizhu.common.db.DBUtil;
import com.weizhu.common.db.ProtobufMapper;
import com.weizhu.proto.UserProtos;

public class PositionDB {
	
	private static final ProtobufMapper<UserProtos.Position> POSITION_MAPPER = 
			ProtobufMapper.createMapper(UserProtos.Position.getDefaultInstance(), 
					"position_id", 
					"position_name",
					"position_desc",
					"state",
					"create_admin_id",
					"create_time",
					"update_admin_id",
					"update_time"
					);
	
	public static Map<Integer, UserProtos.Position> getAllPosition(Connection conn, long companyId, @Nullable Set<UserProtos.State> stateSet) throws SQLException {
		if (stateSet != null && stateSet.isEmpty()) {
			return Collections.emptyMap();
		}
		
		StringBuilder sqlBuilder = new StringBuilder();
		sqlBuilder.append("SELECT * FROM weizhu_position WHERE company_id = ");
		sqlBuilder.append(companyId);
		
		if (stateSet != null) {
			sqlBuilder.append(" AND state IN ('");
			DBUtil.QUOTE_COMMA_JOINER.appendTo(sqlBuilder, stateSet);
			sqlBuilder.append("')");
		}
	
		sqlBuilder.append(" ORDER BY position_id ASC; ");
		
		String sql = sqlBuilder.toString();
		
		Statement stmt = null;
		ResultSet rs = null;
		try {
			stmt = conn.createStatement();
			rs = stmt.executeQuery(sql);

			Map<Integer, UserProtos.Position> resultMap = new LinkedHashMap<Integer, UserProtos.Position>();
			
			UserProtos.Position.Builder tmpPositionBuilder = UserProtos.Position.newBuilder();
			while (rs.next()) {
				tmpPositionBuilder.clear();
				
				UserProtos.Position position = POSITION_MAPPER.mapToItem(rs, tmpPositionBuilder).build();
				resultMap.put(position.getPositionId(), position);
			}
			
			return resultMap;
			
		} finally {
			DBUtil.closeQuietly(rs);
			DBUtil.closeQuietly(stmt);
		}
	}
	
	public static List<Integer> insertPosition(Connection conn, long companyId, List<UserProtos.Position> positionList) throws SQLException {
		if (positionList.isEmpty()) {
			return Collections.emptyList();
		}
		
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try {
			pstmt = conn.prepareStatement("INSERT INTO weizhu_position (company_id, position_id, position_name, position_desc, state, create_admin_id, create_time, update_admin_id, update_time) VALUES (?, NULL, ?, ?, ?, ?, ?, ?, ?); ", Statement.RETURN_GENERATED_KEYS);
			
			for (UserProtos.Position position : positionList) {
				DBUtil.set(pstmt, 1, true, companyId);
				DBUtil.set(pstmt, 2, position.hasPositionName(), position.getPositionName());
				DBUtil.set(pstmt, 3, position.hasPositionDesc(), position.getPositionDesc());
				DBUtil.set(pstmt, 4, position.hasState(), position.getState());
				DBUtil.set(pstmt, 5, position.hasCreateAdminId(), position.getCreateAdminId());
				DBUtil.set(pstmt, 6, position.hasCreateTime(), position.getCreateTime());
				DBUtil.set(pstmt, 7, position.hasUpdateAdminId(), position.getUpdateAdminId());
				DBUtil.set(pstmt, 8, position.hasUpdateTime(), position.getUpdateTime());
				
				pstmt.addBatch();
			}
			
			pstmt.executeBatch();
			
			rs = pstmt.getGeneratedKeys();
			
			List<Integer> positionIdList = new ArrayList<Integer>(positionList.size());
			while (rs.next()) {
				positionIdList.add(rs.getInt(1));
			}
			
			if (positionIdList.size() != positionList.size()) {
				throw new RuntimeException("insert fail");
			}
			
			return positionIdList;
			
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
	public static void updatePosition(Connection conn, 
			long companyId, 
			Map<Integer, UserProtos.Position> oldPositionMap, 
			Map<Integer, UserProtos.Position> newPositionMap
			) throws SQLException {
		
		// 必须保证key完全一样
		if (!oldPositionMap.keySet().equals(newPositionMap.keySet())) {
			throw new IllegalArgumentException("update key not equals");
		}
		
		PreparedStatement pstmt = null;
		try {
			pstmt = conn.prepareStatement("UPDATE weizhu_position SET position_name = ?, position_desc = ?, state = ?, update_admin_id = ?, update_time = ? WHERE company_id = ? AND position_id = ?; ");
			
			for (Entry<Integer, UserProtos.Position> entry : oldPositionMap.entrySet()) {
				UserProtos.Position oldValue = entry.getValue();
				UserProtos.Position newValue = newPositionMap.get(entry.getKey());
				
				if (!oldValue.equals(newValue)) {
					DBUtil.set(pstmt, 1, newValue.hasPositionName(), newValue.getPositionName());
					DBUtil.set(pstmt, 2, newValue.hasPositionDesc(), newValue.getPositionDesc());
					DBUtil.set(pstmt, 3, newValue.hasState(), newValue.getState());
					DBUtil.set(pstmt, 4, newValue.hasUpdateAdminId(), newValue.getUpdateAdminId());
					DBUtil.set(pstmt, 5, newValue.hasUpdateTime(), newValue.getUpdateTime());
					
					DBUtil.set(pstmt, 6, true, companyId);
					DBUtil.set(pstmt, 7, true, newValue.getPositionId());
					
					pstmt.addBatch();
				}
			}
			pstmt.executeBatch();
		} finally {
			DBUtil.closeQuietly(pstmt);
		}
	}
	
	public static void deletePosition(Connection conn, long companyId, Collection<Integer> positionIds, @Nullable Long updateAdminId, @Nullable Integer updateTime) throws SQLException {
		if (positionIds.isEmpty()) {
			return;
		}
		
		PreparedStatement pstmt = null;
		try {
			pstmt = conn.prepareStatement("UPDATE weizhu_position SET state = 'DELETE', update_admin_id = ?, update_time = ? WHERE company_id = ? AND position_id = ?; ");
			
			for (Integer positionId : positionIds) {
				
				DBUtil.set(pstmt, 1, updateAdminId != null, updateAdminId == null ? 0 : updateAdminId);
				DBUtil.set(pstmt, 2, updateTime != null, updateTime == null ? 0 : updateTime);
				DBUtil.set(pstmt, 3, true, companyId);
				DBUtil.set(pstmt, 4, true, positionId);
				
				pstmt.addBatch();
			}
			pstmt.executeBatch();
		} finally {
			DBUtil.closeQuietly(pstmt);
		}
	}
}
