package com.weizhu.service.user.exts;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.Map.Entry;

import com.weizhu.common.db.DBUtil;
import com.weizhu.common.db.ProtobufMapper;
import com.weizhu.proto.UserProtos;

public class UserExtendsDB {

	private static final ProtobufMapper<UserProtos.UserExtends> USER_EXTENDS_MAPPER = 
			ProtobufMapper.createMapper(UserProtos.UserExtends.getDefaultInstance(), 
					"user_id", 
					"name",
					"value"
					);
	
	public static Map<Long, List<UserProtos.UserExtends>> getUserExtends(Connection conn, long companyId, Collection<Long> userIds) throws SQLException {
		if (userIds.isEmpty()) {
			return Collections.emptyMap();
		}
		
		StringBuilder sqlBuilder = new StringBuilder();
		sqlBuilder.append("SELECT * FROM weizhu_user_extends WHERE company_id = ");
		sqlBuilder.append(companyId).append(" AND user_id IN (");
		DBUtil.COMMA_JOINER.appendTo(sqlBuilder, userIds);
		sqlBuilder.append(") ORDER BY user_id ASC, `name` ASC; ");
		
		String sql = sqlBuilder.toString();
		
		Statement stmt = null;
		ResultSet rs = null;
		try {
			stmt = conn.createStatement();
			rs = stmt.executeQuery(sql);
			
			Map<Long, List<UserProtos.UserExtends>> resultMap = new TreeMap<Long, List<UserProtos.UserExtends>>();
			
			UserProtos.UserExtends.Builder tmpBuilder = UserProtos.UserExtends.newBuilder();
			while (rs.next()) {
				tmpBuilder.clear();
				UserProtos.UserExtends exts = USER_EXTENDS_MAPPER.mapToItem(rs, tmpBuilder).build();
				
				DBUtil.addMapArrayList(resultMap, exts.getUserId(), exts);
			}
			
			for (Long userId : userIds) {
				if (!resultMap.containsKey(userId)) {
					resultMap.put(userId, Collections.<UserProtos.UserExtends>emptyList());
				}
			}
			
			return resultMap;
		} finally {
			DBUtil.closeQuietly(rs);
			DBUtil.closeQuietly(stmt);
		}
	}
	
	public static void updateUserExtends(Connection conn, long companyId, 
			Map<Long, List<UserProtos.UserExtends>> oldUserExtendsMap,
			Map<Long, List<UserProtos.UserExtends>> newUserExtendsMap
			) throws SQLException {
		// 必须保证key完全一样
		if (!oldUserExtendsMap.keySet().equals(newUserExtendsMap.keySet())) {
			throw new IllegalArgumentException("update key not equals");
		}
		
		Boolean autoCommit = null;
		try {
			autoCommit = conn.getAutoCommit();
			conn.setAutoCommit(false);
			
			PreparedStatement pstmt = null;
			try {
				pstmt = conn.prepareStatement("DELETE FROM weizhu_user_extends WHERE company_id = ? AND user_id = ? AND `name` = ?; ");
				
				for (Entry<Long, List<UserProtos.UserExtends>> entry : oldUserExtendsMap.entrySet()) {
					List<UserProtos.UserExtends> oldValue = entry.getValue();
					List<UserProtos.UserExtends> newValue = newUserExtendsMap.get(entry.getKey());
					
					for (UserProtos.UserExtends oldExts : oldValue) {
						
						boolean find = false;
						for (UserProtos.UserExtends newExts : newValue) {
							if (oldExts.getUserId() == newExts.getUserId() && oldExts.getName().equals(newExts.getName())) {
								find = true;
								break;
							}
						}
						
						
						if (!find) {
							
							DBUtil.set(pstmt, 1, true, companyId);
							DBUtil.set(pstmt, 2, true, oldExts.getUserId());
							DBUtil.set(pstmt, 3, true, oldExts.getName());
							
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
				pstmt = conn.prepareStatement("REPLACE INTO weizhu_user_extends (company_id, user_id, `name`, `value`) VALUES (?, ?, ?, ?); ");
				
				for (Entry<Long, List<UserProtos.UserExtends>> entry : newUserExtendsMap.entrySet()) {
					List<UserProtos.UserExtends> newValue = entry.getValue();
					List<UserProtos.UserExtends> oldValue = oldUserExtendsMap.get(entry.getKey());
					
					for (UserProtos.UserExtends newExts : newValue) {
						
						boolean isReplace = true;
						for (UserProtos.UserExtends oldExts : oldValue) {
							if (newExts.getUserId() == oldExts.getUserId() 
									&& newExts.getName().equals(oldExts.getName())
									&& newExts.getValue().equals(oldExts.getValue())
									) {
								isReplace = false;
								break;
							}
						}
						
						
						if (isReplace) {
							
							DBUtil.set(pstmt, 1, true, companyId);
							DBUtil.set(pstmt, 2, true, newExts.getUserId());
							DBUtil.set(pstmt, 3, true, newExts.getName());
							DBUtil.set(pstmt, 4, true, newExts.getValue());
							
							pstmt.addBatch();
						}
					}
				}
				pstmt.executeBatch();
			} finally {
				DBUtil.closeQuietly(pstmt);
			}
			
			conn.commit();
			
		} catch (SQLException e) {
			conn.rollback();
			throw e;
		} finally {
			if (autoCommit != null) {
				conn.setAutoCommit(autoCommit);
			}
		}
	}
	
	public static List<String> getUserExtendsName(Connection conn, long companyId) throws SQLException {
		StringBuilder sqlBuilder = new StringBuilder();
		sqlBuilder.append("SELECT DISTINCT `name` FROM weizhu_user_extends WHERE company_id = ");
		sqlBuilder.append(companyId).append(" AND user_id IN (SELECT DISTINCT user_id FROM weizhu_user_base WHERE company_id = ");
		sqlBuilder.append(companyId).append(" AND state != 'DELETE') ORDER BY `name` ASC; ");
		
		String sql = sqlBuilder.toString();
		
		Statement stmt = null;
		ResultSet rs = null;
		try {
			stmt = conn.createStatement();
			rs = stmt.executeQuery(sql);
			
			List<String> list = new ArrayList<String>();
			while (rs.next()) {
				list.add(rs.getString("name"));
			}
			return list;
		} finally {
			DBUtil.closeQuietly(rs);
			DBUtil.closeQuietly(stmt);
		}
	}
}
