package com.weizhu.service.user.experience;

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

import com.weizhu.common.db.DBUtil;
import com.weizhu.common.db.ProtobufMapper;
import com.weizhu.proto.UserProtos;

public class ExperienceDB {

	private static final ProtobufMapper<UserProtos.UserExperience> USER_EXPERIENCE_MAPPER = 
			ProtobufMapper.createMapper(UserProtos.UserExperience.getDefaultInstance(), 
					"experience_id", 
					"experience_content"
					);
	
	public static Map<Long, List<UserProtos.UserExperience>> getUserExperience(Connection conn, long companyId, Collection<Long> userIds) throws SQLException {
		if (userIds.isEmpty()) {
			return Collections.emptyMap();
		}
		
		StringBuilder sqlBuilder = new StringBuilder();
		sqlBuilder.append("SELECT * FROM weizhu_user_experience WHERE company_id = ");
		sqlBuilder.append(companyId).append(" AND user_id IN (");
		DBUtil.COMMA_JOINER.appendTo(sqlBuilder, userIds);
		sqlBuilder.append(") ORDER BY user_id ASC, experience_id ASC");
		
		String sql = sqlBuilder.toString();
		
		Statement stmt = null;
		ResultSet rs = null;
		try {
			stmt = conn.createStatement();
			rs = stmt.executeQuery(sql);
			
			Map<Long, List<UserProtos.UserExperience>> resultMap = new HashMap<Long, List<UserProtos.UserExperience>>(userIds.size());
			
			UserProtos.UserExperience.Builder tmpUserExperienceBuilder = UserProtos.UserExperience.newBuilder();
			while (rs.next()) {
				tmpUserExperienceBuilder.clear();
				
				long userId = rs.getLong("user_id");
				UserProtos.UserExperience userExperience = USER_EXPERIENCE_MAPPER.mapToItem(rs, tmpUserExperienceBuilder).build();
				DBUtil.addMapArrayList(resultMap, userId, userExperience);
			}
			
			for (Long userId : userIds) {
				if (!resultMap.containsKey(userId)) {
					resultMap.put(userId, Collections.<UserProtos.UserExperience>emptyList());
				}
			}
			return resultMap;
		} finally {
			DBUtil.closeQuietly(rs);
			DBUtil.closeQuietly(stmt);
		}
	}
	
	public static int insertUserExperience(Connection conn, long companyId, long userId, String experienceContent) throws SQLException {
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try {
			pstmt = conn.prepareStatement("SELECT IFNULL(MAX(experience_id), 0) + 1 as experience_id FROM weizhu_user_experience WHERE company_id = ? AND user_id = ?; ");
			pstmt.setLong(1, companyId);
			pstmt.setLong(2, userId);
			
			rs = pstmt.executeQuery();
			
			int experienceId;
			if (rs.next()) {
				experienceId = rs.getInt("experience_id");
			} else {
				experienceId = 1;
			}
			
			DBUtil.closeQuietly(rs);
			rs = null;
			DBUtil.closeQuietly(pstmt);
			pstmt = null;
			
			pstmt = conn.prepareStatement("INSERT INTO weizhu_user_experience (company_id, user_id, experience_id, experience_content) VALUES (?, ?, ?, ?); ");
			pstmt.setLong(1, companyId);
			pstmt.setLong(2, userId);
			pstmt.setInt(3, experienceId);
			pstmt.setString(4, experienceContent);
			
			int cnt = pstmt.executeUpdate();
			
			if (cnt <= 0) {
				throw new RuntimeException("insert experience fail");
			}
				
			return experienceId;
		} finally {
			DBUtil.closeQuietly(rs);
			DBUtil.closeQuietly(pstmt);
		}
	}
	
	public static boolean updateUserExperience(Connection conn, long companyId, long userId, int experienceId, String experienceContent) throws SQLException {
		PreparedStatement pstmt = null;
		try {
			pstmt = conn.prepareStatement("UPDATE weizhu_user_experience SET experience_content = ? WHERE company_id = ? AND user_id = ? AND experience_id = ?; ");
			pstmt.setString(1, experienceContent);
			pstmt.setLong(2, companyId);
			pstmt.setLong(3, userId);
			pstmt.setInt(4, experienceId);
			
			return pstmt.executeUpdate() > 0;
		} finally {
			DBUtil.closeQuietly(pstmt);
		}
	}
	
	public static boolean deleteUserExperience(Connection conn, long companyId, long userId, int experienceId) throws SQLException {
		PreparedStatement pstmt = null;
		try {
			pstmt = conn.prepareStatement("DELETE FROM weizhu_user_experience WHERE company_id = ? AND user_id = ? AND experience_id = ?; ");
			pstmt.setLong(1, companyId);
			pstmt.setLong(2, userId);
			pstmt.setInt(3, experienceId);
			
			return pstmt.executeUpdate() > 0;
		} finally {
			DBUtil.closeQuietly(pstmt);
		}
	}
	
}
