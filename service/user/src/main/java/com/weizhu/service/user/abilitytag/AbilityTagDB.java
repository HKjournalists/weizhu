package com.weizhu.service.user.abilitytag;

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
import java.util.Random;
import java.util.Set;
import java.util.TreeSet;

import javax.annotation.Nullable;

import com.google.common.collect.Iterables;
import com.weizhu.common.db.DBUtil;
import com.weizhu.proto.UserProtos;

public class AbilityTagDB {
	
	public static Map<Long, List<UserProtos.UserAbilityTag>> getUserAbilityTagList(Connection conn, long companyId, Collection<Long> userIds) throws SQLException {
		if (userIds.isEmpty()) {
			return Collections.emptyMap();
		}
		
		String userIdStr = DBUtil.COMMA_JOINER.join(userIds);
		
		StringBuilder sqlBuilder = new StringBuilder();
		
		sqlBuilder.append("SELECT user_id, tag_name, max(tag_time) tag_time, count(tag_user_id) as tag_count FROM weizhu_user_ability_tag_user WHERE company_id = ");
		sqlBuilder.append(companyId).append(" AND user_id IN (");
		sqlBuilder.append(userIdStr).append(") GROUP BY user_id, tag_name ORDER BY user_id, tag_count DESC; ");
		
		sqlBuilder.append("SELECT user_id, tag_name, create_user_id, create_time FROM weizhu_user_ability_tag WHERE company_id = ");
		sqlBuilder.append(companyId).append(" AND user_id IN (");
		sqlBuilder.append(userIdStr).append(") ORDER BY user_id, create_time DESC; ");
		
		String sql = sqlBuilder.toString();
		
		Statement stmt = null;
		ResultSet rs = null;
		try {
			stmt = conn.createStatement();
			stmt.execute(sql);
			
			rs = stmt.getResultSet();
			
			Map<Long, Map<String, UserProtos.UserAbilityTag.Builder>> userTagBuilderMap = new HashMap<Long, Map<String, UserProtos.UserAbilityTag.Builder>>(userIds.size());
			
			while (rs.next()) {
				long userId = rs.getLong("user_id");
				String tagName = rs.getString("tag_name");
				int tagTime = rs.getInt("tag_time");
				int tagCount = rs.getInt("tag_count");
				
				Map<String, UserProtos.UserAbilityTag.Builder> tagBuilderMap = userTagBuilderMap.get(userId);
				if (tagBuilderMap == null) {
					tagBuilderMap = new LinkedHashMap<String, UserProtos.UserAbilityTag.Builder>();
					userTagBuilderMap.put(userId, tagBuilderMap);
				}
				tagBuilderMap.put(tagName, UserProtos.UserAbilityTag.newBuilder()
						.setUserId(userId)
						.setTagName(tagName)
						.setTagTime(tagTime)
						.setTagCount(tagCount));
			}
			
			DBUtil.closeQuietly(rs);
			rs = null;
			
			stmt.getMoreResults();
			rs = stmt.getResultSet();
			
			while (rs.next()) {
				long userId = rs.getLong("user_id");
				String tagName = rs.getString("tag_name");
				Long createUserId = rs.getLong("create_user_id");
				if (rs.wasNull()) {
					createUserId = null;
				}
				Integer createTime = rs.getInt("create_time");
				if (rs.wasNull()) {
					createTime = null;
				}
				
				Map<String, UserProtos.UserAbilityTag.Builder> tagBuilderMap = userTagBuilderMap.get(userId);
				if (tagBuilderMap == null) {
					tagBuilderMap = new LinkedHashMap<String, UserProtos.UserAbilityTag.Builder>();
					userTagBuilderMap.put(userId, tagBuilderMap);
				}
				
				UserProtos.UserAbilityTag.Builder builder = tagBuilderMap.get(tagName);
				if (builder == null) {
					builder = UserProtos.UserAbilityTag.newBuilder()
							.setUserId(userId)
							.setTagName(tagName)
							.setTagTime(createTime)
							.setTagCount(0);
					tagBuilderMap.put(tagName, builder);
				}
				
				if (createUserId != null) {
					builder.setCreateUserId(createUserId);
				}
				if (createTime != null) {
					builder.setCreateTime(createTime);
				}
			}
			
			Map<Long, List<UserProtos.UserAbilityTag>> resultMap = new HashMap<Long, List<UserProtos.UserAbilityTag>>(userTagBuilderMap.size());
			
			for (Entry<Long, Map<String, UserProtos.UserAbilityTag.Builder>> entry : userTagBuilderMap.entrySet()) {
				List<UserProtos.UserAbilityTag> list = new ArrayList<UserProtos.UserAbilityTag>(entry.getValue().size());
				for (UserProtos.UserAbilityTag.Builder builder : entry.getValue().values()) {
					list.add(builder.build());
				}
				resultMap.put(entry.getKey(), list);
			}
			return resultMap;
		} finally {
			DBUtil.closeQuietly(rs);
			DBUtil.closeQuietly(stmt);
		}
	}
	
	public static void insertAbilityTag(Connection conn, long companyId, long userId, String tagName, @Nullable Long createUserId, @Nullable Integer createTime) throws SQLException {
		PreparedStatement pstmt = null;
		try {
			pstmt = conn.prepareStatement("INSERT INTO weizhu_user_ability_tag (company_id, user_id, tag_name, create_user_id, create_time) VALUES (?, ?, ?, ?, ?); ");
			
			DBUtil.set(pstmt, 1, companyId);
			DBUtil.set(pstmt, 2, userId);
			DBUtil.set(pstmt, 3, tagName);
			DBUtil.set(pstmt, 4, createUserId);
			DBUtil.set(pstmt, 5, createTime);
			
			pstmt.executeUpdate();
		} finally {
			DBUtil.closeQuietly(pstmt);
		}
	}
	
	public static void deleteAbilityTag(Connection conn, long companyId, long userId, Collection<String> tagNames) throws SQLException {
		if (tagNames.isEmpty()) {
			return;
		}
		
		PreparedStatement pstmt = null;
		try {
			pstmt = conn.prepareStatement("DELETE FROM weizhu_user_ability_tag WHERE company_id = ? AND user_id = ? AND tag_name = ?; DELETE FROM weizhu_user_ability_tag_user WHERE company_id = ? AND user_id = ? AND tag_name = ?;");
			
			for (String tagName : tagNames) {
				pstmt.setLong(1, companyId);
				pstmt.setLong(2, userId);
				pstmt.setString(3, tagName);
				pstmt.setLong(4, companyId);
				pstmt.setLong(5, userId);
				pstmt.setString(6, tagName);
				
				pstmt.addBatch();
			}
			
			pstmt.executeBatch();
		} finally {
			DBUtil.closeQuietly(pstmt);
		}
	}
	
	public static void updateAbilityTag(Connection conn, long companyId, 
			Map<Long, Set<String>> oldTagMap, 
			Map<Long, Set<String>> newTagMap,
			@Nullable Long createUserId, @Nullable Integer createTime
			) throws SQLException {

		// 必须保证key完全一样
		if (!oldTagMap.keySet().equals(newTagMap.keySet())) {
			throw new IllegalArgumentException("update key not equals");
		}
		
		if (oldTagMap.isEmpty() || newTagMap.isEmpty()) {
			return;
		}
		
		Boolean autoCommit = null;
		try {
			autoCommit = conn.getAutoCommit();
			conn.setAutoCommit(false);
			
			PreparedStatement pstmt = null;
			try {
				pstmt = conn.prepareStatement("INSERT INTO weizhu_user_ability_tag (company_id, user_id, tag_name, create_user_id, create_time) VALUES (?, ?, ?, ?, ?); ");
				
				for (Entry<Long, Set<String>> entry : oldTagMap.entrySet()) {
					final long userId = entry.getKey();
					Set<String> oldValue = entry.getValue();
					Set<String> newValue = newTagMap.get(userId);
					
					for (String newTag : newValue) {
						if (!oldValue.contains(newTag)) {
							
							DBUtil.set(pstmt, 1, companyId);
							DBUtil.set(pstmt, 2, userId);
							DBUtil.set(pstmt, 3, newTag);
							DBUtil.set(pstmt, 4, createUserId);
							DBUtil.set(pstmt, 5, createTime);
							
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
				pstmt = conn.prepareStatement("DELETE FROM weizhu_user_ability_tag WHERE company_id = ? AND user_id = ? AND tag_name = ?; DELETE FROM weizhu_user_ability_tag_user WHERE company_id = ? AND user_id = ? AND tag_name = ?;");
				
				for (Entry<Long, Set<String>> entry : oldTagMap.entrySet()) {
					final long userId = entry.getKey();
					Set<String> oldValue = entry.getValue();
					Set<String> newValue = newTagMap.get(userId);
					
					for (String oldTag : oldValue) {
						if (!newValue.contains(oldTag)) {
							
							pstmt.setLong(1, companyId);
							pstmt.setLong(2, userId);
							pstmt.setString(3, oldTag);
							pstmt.setLong(4, companyId);
							pstmt.setLong(5, userId);
							pstmt.setString(6, oldTag);
							
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
	
	public static void incrementalUpdateAbilityTag(Connection conn, long companyId, 
			Map<Long, Set<String>> oldTagMap, 
			Map<Long, Set<String>> newTagMap,
			@Nullable Long createUserId, @Nullable Integer createTime
			) throws SQLException {

		// 必须保证key完全一样
		if (!oldTagMap.keySet().equals(newTagMap.keySet())) {
			throw new IllegalArgumentException("update key not equals");
		}
		
		if (oldTagMap.isEmpty() || newTagMap.isEmpty()) {
			return;
		}
		
		Boolean autoCommit = null;
		PreparedStatement pstmt = null;
		try {
			autoCommit = conn.getAutoCommit();
			conn.setAutoCommit(false);
			
			pstmt = conn.prepareStatement("INSERT INTO weizhu_user_ability_tag (company_id, user_id, tag_name, create_user_id, create_time) VALUES (?, ?, ?, ?, ?); ");
			
			for (Entry<Long, Set<String>> entry : oldTagMap.entrySet()) {
				final long userId = entry.getKey();
				Set<String> oldValue = entry.getValue();
				Set<String> newValue = newTagMap.get(userId);
				
				for (String newTag : newValue) {
					if (!oldValue.contains(newTag)) {
						
						DBUtil.set(pstmt, 1, companyId);
						DBUtil.set(pstmt, 2, userId);
						DBUtil.set(pstmt, 3, newTag);
						DBUtil.set(pstmt, 4, createUserId);
						DBUtil.set(pstmt, 5, createTime);
						
						pstmt.addBatch();
					}
				}
			}
			pstmt.executeBatch();
			
			conn.commit();
			
		} catch (SQLException e) {
			conn.rollback();
			throw e;
		} finally {
			DBUtil.closeQuietly(pstmt);
			if (autoCommit != null) {
				conn.setAutoCommit(autoCommit);
			}
		}
	}
	
	public static Map<Long, Set<String>> getUserAbilityTagNameSet(Connection conn, long companyId, Collection<Long> userIds, long tagUserId) throws SQLException {
		if (userIds.isEmpty()) {
			return Collections.emptyMap();
		}
		
		StringBuilder sqlBuilder = new StringBuilder();
		sqlBuilder.append("SELECT user_id, tag_name FROM weizhu_user_ability_tag_user WHERE company_id = ");
		sqlBuilder.append(companyId).append(" AND user_id IN (");
		DBUtil.COMMA_JOINER.appendTo(sqlBuilder, userIds);
		sqlBuilder.append(") AND tag_user_id = ").append(tagUserId).append("; ");
		
		String sql = sqlBuilder.toString();
		
		Statement stmt = null;
		ResultSet rs = null;
		try {
			stmt = conn.createStatement();
			
			rs = stmt.executeQuery(sql);
			
			Map<Long, Set<String>> resultMap = new HashMap<Long, Set<String>>();
			while (rs.next()) {
				long userId = rs.getLong("user_id");
				String tagName = rs.getString("tag_name");
				
				Set<String> tagNameSet = resultMap.get(userId);
				if (tagNameSet == null) {
					tagNameSet = new TreeSet<String>();
					resultMap.put(userId, tagNameSet);
				}
				tagNameSet.add(tagName);
			}
			
			return resultMap;
		} finally {
			DBUtil.closeQuietly(rs);
			DBUtil.closeQuietly(stmt);
		}
	}
	
	public static void insertUserAbilityTag(Connection conn, long companyId, long userId, String tagName, long tagUserId, int tagTime) throws SQLException {
		PreparedStatement pstmt = null;
		try {
			pstmt = conn.prepareStatement("INSERT INTO weizhu_user_ability_tag_user (company_id, user_id, tag_name, tag_user_id, tag_time) VALUES (?, ?, ?, ?, ?); ");
			
			pstmt.setLong(1, companyId);
			pstmt.setLong(2, userId);
			pstmt.setString(3, tagName);
			pstmt.setLong(4, tagUserId);
			pstmt.setInt(5, tagTime);
			
			pstmt.executeUpdate();
		} finally {
			DBUtil.closeQuietly(pstmt);
		}
	}
	
	public static void deleteUserAbilityTag(Connection conn, long companyId, long userId, String tagName, long tagUserId) throws SQLException {
		PreparedStatement pstmt = null;
		try {
			pstmt = conn.prepareStatement("DELETE FROM weizhu_user_ability_tag_user WHERE company_id = ? AND user_id = ? AND tag_name = ? AND tag_user_id = ?; ");
			
			pstmt.setLong(1, companyId);
			pstmt.setLong(2, userId);
			pstmt.setString(3, tagName);
			pstmt.setLong(4, tagUserId);
			
			pstmt.executeUpdate();
		} finally {
			DBUtil.closeQuietly(pstmt);
		}
	}
	
	private static final Random rand = new Random();
	
	public static List<Long> getRandomAbilityTagUserIdList(Connection conn, long companyId, Set<String> tagNameSet, @Nullable Boolean isExpert, int size) throws SQLException {
		if (tagNameSet.isEmpty() || size <= 0) {
			return Collections.emptyList();
		}
		
		StringBuilder sqlBuilder = new StringBuilder();
		sqlBuilder.append("SELECT DISTINCT A.user_id FROM weizhu_user_ability_tag A INNER JOIN weizhu_user_base B ON A.company_id = B.company_id AND A.user_id = B.user_id WHERE A.company_id = ");
		sqlBuilder.append(companyId).append(" AND A.tag_name IN ('");
		DBUtil.QUOTE_COMMA_JOINER.appendTo(sqlBuilder, Iterables.transform(tagNameSet, DBUtil.SQL_STRING_ESCAPER.asFunction()));
		sqlBuilder.append("') AND B.state = 'NORMAL'");
		if (isExpert != null) {
			sqlBuilder.append(" AND B.is_expert = ").append(isExpert ? 1 : 0);
		}
		sqlBuilder.append(" ;");
		
		String sql = sqlBuilder.toString();
		
		Statement stmt = null;
		ResultSet rs = null;
		try {
			stmt = conn.createStatement();
			rs = stmt.executeQuery(sql);
			
			List<Long> resultList = new ArrayList<Long>();
			while (rs.next()) {
				resultList.add(rs.getLong("A.user_id"));
			}
			
			Collections.shuffle(resultList, rand);
			
			if (resultList.size() <= size) {
				return resultList;
			} else {
				return resultList.subList(0, size);
			}
		} finally {
			DBUtil.closeQuietly(rs);
			DBUtil.closeQuietly(stmt);
		}
	}
	
	public static List<Long> getAbilityTagUserIdList(Connection conn, long companyId, Set<String> tagNameSet, @Nullable Boolean isExpert) throws SQLException {
		if (tagNameSet.isEmpty()) {
			return Collections.emptyList();
		}
		
		StringBuilder sqlBuilder = new StringBuilder();
		sqlBuilder.append("SELECT DISTINCT A.user_id FROM weizhu_user_ability_tag A INNER JOIN weizhu_user_base B ON A.company_id = B.company_id AND A.user_id = B.user_id WHERE A.company_id = ");
		sqlBuilder.append(companyId).append(" AND A.tag_name IN ('");
		DBUtil.QUOTE_COMMA_JOINER.appendTo(sqlBuilder, Iterables.transform(tagNameSet, DBUtil.SQL_STRING_ESCAPER.asFunction()));
		sqlBuilder.append("') AND B.state = 'NORMAL'");
		if (isExpert != null) {
			if (isExpert) {
				sqlBuilder.append(" AND B.is_expert = 1");
			} else {
				sqlBuilder.append(" AND (B.is_expert = 0 OR B.is_expert IS NULL)");
			}
		}
		sqlBuilder.append(" ;");
		
		String sql = sqlBuilder.toString();
		
		Statement stmt = null;
		ResultSet rs = null;
		try {
			stmt = conn.createStatement();
			rs = stmt.executeQuery(sql);
			
			List<Long> resultList = new ArrayList<Long>();
			while (rs.next()) {
				resultList.add(rs.getLong("A.user_id"));
			}
			return resultList;
		} finally {
			DBUtil.closeQuietly(rs);
			DBUtil.closeQuietly(stmt);
		}
	}
}
