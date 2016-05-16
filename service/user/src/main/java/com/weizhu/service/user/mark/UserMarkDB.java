package com.weizhu.service.user.mark;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import com.weizhu.common.db.DBUtil;
import com.weizhu.common.db.ProtobufMapper;
import com.weizhu.proto.UserProtos;

public class UserMarkDB {

	private static final ProtobufMapper<UserProtos.UserMark> USER_MARK_MAPPER = 
			ProtobufMapper.createMapper(UserProtos.UserMark.getDefaultInstance(), 
					"user_id", 
					"is_star",
					"star_time",
					"mark_name"
					);
	
	public static Map<Long, Map<Long, UserProtos.UserMark>> getUserMark(Connection conn, long companyId, Collection<Long> markerIds) throws SQLException {
		if (markerIds.isEmpty()) {
			return Collections.emptyMap();
		}
		
		StringBuilder sql = new StringBuilder();
		sql.append("SELECT * FROM weizhu_user_mark WHERE company_id = ");
		sql.append(companyId).append(" AND marker_id IN (");
		DBUtil.COMMA_JOINER.appendTo(sql, markerIds);
		sql.append(") ORDER BY marker_id ASC, user_id ASC");
		
		Statement stmt = null;
		ResultSet rs = null;
		try {
			stmt = conn.createStatement();
			rs = stmt.executeQuery(sql.toString());
			
			Map<Long, Map<Long, UserProtos.UserMark>> resultMap = new HashMap<Long, Map<Long, UserProtos.UserMark>>(markerIds.size());
			
			UserProtos.UserMark.Builder tmpUserMarkBuilder = UserProtos.UserMark.newBuilder();
			while (rs.next()) {
				tmpUserMarkBuilder.clear();
				
				Long markerId = rs.getLong("marker_id");
				UserProtos.UserMark userMark = USER_MARK_MAPPER.mapToItem(rs, tmpUserMarkBuilder).build();
				
				Map<Long, UserProtos.UserMark> map = resultMap.get(markerId);
				if (map == null) {
					map = new TreeMap<Long, UserProtos.UserMark>();
					resultMap.put(markerId, map);
				}
				map.put(userMark.getUserId(), userMark);
			}
			return resultMap;
		} finally {
			DBUtil.closeQuietly(rs);
			DBUtil.closeQuietly(stmt);
		}
	}
	
	public static boolean replaceUserMark(Connection conn, long companyId, long markerId, UserProtos.UserMark userMark) throws SQLException {
		PreparedStatement pstmt = null;
		try {
			pstmt = conn.prepareStatement("REPLACE INTO weizhu_user_mark (company_id, marker_id, user_id, is_star, star_time, mark_name) VALUES (?, ?, ?, ?, ?, ?); ");
			pstmt.setLong(1, companyId);
			pstmt.setLong(2, markerId);
			DBUtil.set(pstmt, 3, userMark.hasUserId(), userMark.getUserId());
			DBUtil.set(pstmt, 4, userMark.hasIsStar(), userMark.getIsStar());
			DBUtil.set(pstmt, 5, userMark.hasStarTime(), userMark.getStarTime());
			DBUtil.set(pstmt, 6, userMark.hasMarkName(), userMark.getMarkName());
			
			return pstmt.executeUpdate() > 0;
		} finally {
			DBUtil.closeQuietly(pstmt);
		}
	}
	
	public static boolean deleteUserMark(Connection conn, long companyId, long markerId, long userId) throws SQLException {
		PreparedStatement pstmt = null;
		try {
			pstmt = conn.prepareStatement("DELETE FROM weizhu_user_mark WHERE company_id = ? AND marker_id = ? AND user_id = ?; ");
			pstmt.setLong(1, companyId);
			pstmt.setLong(2, markerId);
			pstmt.setLong(3, userId);
			
			return pstmt.executeUpdate() > 0;
		} finally {
			DBUtil.closeQuietly(pstmt);
		}
	}
	
	public static void updateUserMark(Connection conn, 
			long companyId, 
			Map<Long, List<UserProtos.UserMark>> oldUserMarkMap, 
			Map<Long, List<UserProtos.UserMark>> newUserMarkMap
			) throws SQLException {
		
		// 必须保证key完全一样
		if (!oldUserMarkMap.keySet().equals(newUserMarkMap.keySet())) {
			throw new IllegalArgumentException("update key not equals");
		}
		
		PreparedStatement pstmt = null;
		try {
			pstmt = conn.prepareStatement("DELETE FROM weizhu_user_mark WHERE company_id = ? AND marker_id = ? AND user_id = ?; ");
			
			for (Entry<Long, List<UserProtos.UserMark>> entry : oldUserMarkMap.entrySet()) {
				final Long markerId = entry.getKey();
				List<UserProtos.UserMark> newUserMarkList = newUserMarkMap.get(markerId);
				for (UserProtos.UserMark oldUserMark : entry.getValue()) {
					boolean find = false;
					for (UserProtos.UserMark newUserMark : newUserMarkList) {
						if (oldUserMark.getUserId() == newUserMark.getUserId()) {
							find = true;
							break;
						}
					}
					
					if (!find) {
						DBUtil.set(pstmt, 1, true, companyId);
						DBUtil.set(pstmt, 2, true, markerId);
						DBUtil.set(pstmt, 3, oldUserMark.hasUserId(), oldUserMark.getUserId());
						
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
			pstmt = conn.prepareStatement("REPLACE INTO weizhu_user_mark (company_id, marker_id, user_id, is_star, star_time, mark_name) VALUES (?, ?, ?, ?, ?, ?); ");
			
			for (Entry<Long, List<UserProtos.UserMark>> entry : newUserMarkMap.entrySet()) {
				final Long markerId = entry.getKey();
				
				List<UserProtos.UserMark> oldUserMarkList = oldUserMarkMap.get(markerId);
				for (UserProtos.UserMark newUserMark : entry.getValue()) {
					boolean find = false;
					for (UserProtos.UserMark oldUserMark : oldUserMarkList) {
						if (newUserMark.equals(oldUserMark)) {
							find = true;
							break;
						}
					}
					
					if (!find) {
						DBUtil.set(pstmt, 1, true, companyId);
						DBUtil.set(pstmt, 2, true, markerId);
						DBUtil.set(pstmt, 3, newUserMark.hasUserId(),   newUserMark.getUserId());
						DBUtil.set(pstmt, 4, newUserMark.hasIsStar(),   newUserMark.getIsStar());
						DBUtil.set(pstmt, 5, newUserMark.hasStarTime(), newUserMark.getStarTime());
						DBUtil.set(pstmt, 6, newUserMark.hasMarkName(), newUserMark.getMarkName());
						
						pstmt.addBatch();
					}
				}
			}
			
			pstmt.executeBatch();
		} finally {
			DBUtil.closeQuietly(pstmt);
		}
	}
	
}
