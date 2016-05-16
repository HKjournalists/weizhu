package com.weizhu.service.user.base;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.annotation.Nullable;

import com.google.common.collect.Iterables;
import com.weizhu.common.db.DBUtil;
import com.weizhu.common.db.ProtobufMapper;
import com.weizhu.common.utils.DataPage;
import com.weizhu.proto.UserProtos;

public class UserBaseDB {
	
	public static Map<String, Long> getUserIdByMobileNoUnique(Connection conn, long companyId, Collection<String> mobileNos) throws SQLException {
		if (mobileNos.isEmpty()) {
			return Collections.emptyMap();
		}
		
		// 小心SQL注入
		StringBuilder sqlBuilder = new StringBuilder();
		sqlBuilder.append("SELECT user_id, mobile_no_unique FROM weizhu_user_base_mobile_no WHERE company_id = ");
		sqlBuilder.append(companyId).append(" AND mobile_no_unique IN ('");
		DBUtil.QUOTE_COMMA_JOINER.appendTo(sqlBuilder, Iterables.transform(mobileNos, DBUtil.SQL_STRING_ESCAPER.asFunction()));
		sqlBuilder.append("'); ");
		
		String sql = sqlBuilder.toString();
		
		Statement stmt = null;
		ResultSet rs = null;
		try {
			stmt = conn.createStatement();
			rs = stmt.executeQuery(sql);
			
			Map<String, Long> mobileNoToUserIdMap = new HashMap<String, Long>(mobileNos.size());
			while (rs.next()) {
				long userId = rs.getLong("user_id");
				String mobileNo = rs.getString("mobile_no_unique");
				mobileNoToUserIdMap.put(mobileNo, userId);
			}
			return mobileNoToUserIdMap;
		} finally {
			DBUtil.closeQuietly(rs);
			DBUtil.closeQuietly(stmt);
		}
	}
	
	private static final ProtobufMapper<UserProtos.UserBase> USER_BASE_MAPPER = 
			ProtobufMapper.createMapper(UserProtos.UserBase.getDefaultInstance(), 
					"user_id", 
					"raw_id",
					"user_name",
					"gender",
					"avatar",
					"email",
					"signature",
					"interest",
					"is_expert",
					"level_id",
					"state",
					"create_admin_id",
					"create_time",
					"update_admin_id",
					"update_time"
					);

	public static Map<Long, UserProtos.UserBase> getUserBase(Connection conn, long companyId, Collection<Long> userIds) throws SQLException {
		if (userIds.isEmpty()) {
			return Collections.emptyMap();
		}
		
		String userIdStr = DBUtil.COMMA_JOINER.join(userIds);
		
		StringBuilder sqlBuilder = new StringBuilder();
		sqlBuilder.append("SELECT user_id, mobile_no FROM weizhu_user_base_mobile_no WHERE company_id = ");
		sqlBuilder.append(companyId).append(" AND user_id IN (").append(userIdStr).append(") ORDER BY user_id ASC, mobile_no ASC; ");
		sqlBuilder.append("SELECT user_id, phone_no FROM weizhu_user_base_phone_no WHERE company_id = ");
		sqlBuilder.append(companyId).append(" AND user_id IN (").append(userIdStr).append(") ORDER BY user_id ASC, phone_no ASC; ");
		sqlBuilder.append("SELECT * FROM weizhu_user_base WHERE company_id = ");
		sqlBuilder.append(companyId).append(" AND user_id IN (").append(userIdStr).append("); ");
		
		String sql = sqlBuilder.toString();
		
		Statement stmt = null;
		ResultSet rs = null;
		try {
			stmt = conn.createStatement();
			stmt.execute(sql);
			rs = stmt.getResultSet();
			
			Map<Long, List<String>> mobileNoMap = new HashMap<Long, List<String>>(userIds.size());
			while (rs.next()) {
				Long userId = rs.getLong("user_id");
				String mobileNo = rs.getString("mobile_no");
				
				DBUtil.addMapLinkedList(mobileNoMap, userId, mobileNo);
			}
			
			DBUtil.closeQuietly(rs);
			rs = null;
			
			stmt.getMoreResults();
			rs = stmt.getResultSet();
			
			Map<Long, List<String>> phoneNoMap = new HashMap<Long, List<String>>(userIds.size());
			while (rs.next()) {
				Long userId = rs.getLong("user_id");
				String phoneNo = rs.getString("phone_no");
				
				DBUtil.addMapLinkedList(phoneNoMap, userId, phoneNo);
			}
			
			DBUtil.closeQuietly(rs);
			rs = null;
			
			stmt.getMoreResults();
			rs = stmt.getResultSet();
			
			Map<Long, UserProtos.UserBase> userBaseMap = new HashMap<Long, UserProtos.UserBase>(userIds.size());
			UserProtos.UserBase.Builder tmpUserBaseBuilder = UserProtos.UserBase.newBuilder();
			while (rs.next()) {
				tmpUserBaseBuilder.clear();
				
				Long userId = rs.getLong("user_id");
				USER_BASE_MAPPER.mapToItem(rs, tmpUserBaseBuilder);
				
				List<String> mobileNoList = mobileNoMap.get(userId);
				List<String> phoneNoList = phoneNoMap.get(userId);
				
				if (mobileNoList != null) {
					tmpUserBaseBuilder.addAllMobileNo(mobileNoList);
				}
				if (phoneNoList != null) {
					tmpUserBaseBuilder.addAllPhoneNo(phoneNoList);
				}
				
				userBaseMap.put(userId, tmpUserBaseBuilder.build());
			}
			
			return userBaseMap;
			
		} finally {
			DBUtil.closeQuietly(rs);
			DBUtil.closeQuietly(stmt);
		}
	}
	
	public static boolean updateUserAvatar(Connection conn, long companyId, long userId, @Nullable String avatar) throws SQLException {
		PreparedStatement pstmt = null;
		try {
			pstmt = conn.prepareStatement("UPDATE weizhu_user_base SET avatar = ? WHERE company_id = ? AND user_id = ?; ");
			DBUtil.set(pstmt, 1, avatar != null, avatar);
			DBUtil.set(pstmt, 2, true, companyId);
			DBUtil.set(pstmt, 3, true, userId);
			
			return pstmt.executeUpdate() > 0;
		} finally {
			DBUtil.closeQuietly(pstmt);
		}
	}
	
	public static boolean updateUserSignature(Connection conn, long companyId, long userId, @Nullable String signature) throws SQLException {
		PreparedStatement pstmt = null;
		try {
			pstmt = conn.prepareStatement("UPDATE weizhu_user_base SET signature = ? WHERE company_id = ? AND user_id = ?; ");
			DBUtil.set(pstmt, 1, signature != null, signature);
			DBUtil.set(pstmt, 2, true, companyId);
			DBUtil.set(pstmt, 3, true, userId);
			
			return pstmt.executeUpdate() > 0;
		} finally {
			DBUtil.closeQuietly(pstmt);
		}
	}
	
	public static boolean updateUserInterest(Connection conn, long companyId, long userId, @Nullable String interest) throws SQLException {
		PreparedStatement pstmt = null;
		try {
			pstmt = conn.prepareStatement("UPDATE weizhu_user_base SET interest = ? WHERE company_id = ? AND user_id = ?; ");
			DBUtil.set(pstmt, 1, interest != null, interest);
			DBUtil.set(pstmt, 2, true, companyId);
			DBUtil.set(pstmt, 3, true, userId);
			
			return pstmt.executeUpdate() > 0;
		} finally {
			DBUtil.closeQuietly(pstmt);
		}
	}
	
	public static List<Long> searchUserIdByName(Connection conn, long companyId, String keyword, int size, @Nullable Set<UserProtos.UserBase.State> userStateSet) throws SQLException {
		if (size <= 0 || (userStateSet != null && userStateSet.isEmpty())) {
			return Collections.emptyList();
		}
		
		StringBuilder sqlBuilder = new StringBuilder();
		sqlBuilder.append("SELECT user_id FROM weizhu_user_base WHERE company_id = ");
		sqlBuilder.append(companyId);
		
		if (userStateSet != null) {
			sqlBuilder.append(" AND state IN ('");
			DBUtil.QUOTE_COMMA_JOINER.appendTo(sqlBuilder, userStateSet);
			sqlBuilder.append("') ");
		}
		
		sqlBuilder.append(" AND user_name LIKE '%");
		sqlBuilder.append(DBUtil.SQL_LIKE_STRING_ESCAPER.escape(keyword)).append("%' ORDER BY user_id ASC LIMIT ");
		sqlBuilder.append(size).append("; ");
		
		String sql = sqlBuilder.toString();
		
		Statement stmt = null;
		ResultSet rs = null;
		try {
			stmt = conn.createStatement();
			rs = stmt.executeQuery(sql);
			
			List<Long> userIdList = new ArrayList<Long>();
			while (rs.next()) {
				userIdList.add(rs.getLong("user_id"));
			}
			return userIdList;
		} finally {
			DBUtil.closeQuietly(rs);
			DBUtil.closeQuietly(stmt);
		}
	}
	
	public static List<Long> searchUserIdByMobileNoUnique(Connection conn, long companyId, String keyword, int size) throws SQLException {
		if (size <= 0) {
			return Collections.emptyList();
		}
		
		StringBuilder sqlBuilder = new StringBuilder();
		sqlBuilder.append("SELECT distinct(user_id) as id FROM weizhu_user_base_mobile_no WHERE company_id = ");
		sqlBuilder.append(companyId).append(" AND mobile_no_unique LIKE '%");
		sqlBuilder.append(DBUtil.SQL_LIKE_STRING_ESCAPER.escape(keyword)).append("%' ORDER BY user_id ASC LIMIT ");
		sqlBuilder.append(size).append("; ");
		
		String sql = sqlBuilder.toString();
		
		Statement stmt = null;
		ResultSet rs = null;
		try {
			stmt = conn.createStatement();
			rs = stmt.executeQuery(sql);
			
			List<Long> userIdList = new ArrayList<Long>();
			while (rs.next()) {
				userIdList.add(rs.getLong("id"));
			}
			return userIdList;
		} finally {
			DBUtil.closeQuietly(rs);
			DBUtil.closeQuietly(stmt);
		}
	}
	
	/* admin */
	
	public static List<Long> getUserIdList(Connection conn, long companyId, @Nullable Long lastUserId, int size, @Nullable Set<UserProtos.UserBase.State> userStateSet) throws SQLException {
		if (size <= 0 || (userStateSet != null && userStateSet.isEmpty())) {
			return Collections.emptyList();
		}
		
		StringBuilder sqlBuilder = new StringBuilder();
		sqlBuilder.append("SELECT user_id FROM weizhu_user_base WHERE company_id = ");
		sqlBuilder.append(companyId);
		
		if (userStateSet != null) {
			sqlBuilder.append(" AND state IN ('");
			DBUtil.QUOTE_COMMA_JOINER.appendTo(sqlBuilder, userStateSet);
			sqlBuilder.append("')");
		}
		
		if (lastUserId != null) {
			sqlBuilder.append(" AND user_id > ").append(lastUserId);
		}
		sqlBuilder.append(" ORDER BY user_id ASC LIMIT ").append(size).append("; ");
		
		String sql = sqlBuilder.toString();
		
		Statement stmt = null;
		ResultSet rs = null;
		try {
			stmt = conn.createStatement();
			rs = stmt.executeQuery(sql);
			
			List<Long> userIdList = new ArrayList<Long>(size);
			while (rs.next()) {
				userIdList.add(rs.getLong("user_id"));
			}
			
			return userIdList;
		} finally {
			DBUtil.closeQuietly(rs);
			DBUtil.closeQuietly(stmt);
		}
	}

	public static List<Long> insertUserBase(Connection conn, long companyId, List<UserProtos.UserBase> userBaseList) throws SQLException {
		if (userBaseList.isEmpty()) {
			return Collections.emptyList();
		}
		
		Boolean autoCommit = null;
		try {
			autoCommit = conn.getAutoCommit();
			conn.setAutoCommit(false);
			
			final List<Long> userIdList;
			PreparedStatement pstmt = null;
			ResultSet rs = null;
			try {
				pstmt = conn.prepareStatement("INSERT INTO weizhu_user_base (company_id, user_id, raw_id, raw_id_unique, user_name, gender, avatar, email, signature, interest, is_expert, level_id, state, create_admin_id, create_time, update_admin_id, update_time) VALUES (?, NULL, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?); ", Statement.RETURN_GENERATED_KEYS);
				
				for (UserProtos.UserBaseOrBuilder userBase : userBaseList) {
					DBUtil.set(pstmt, 1, true, companyId);
					
					DBUtil.set(pstmt, 2,  userBase.hasRawId(), userBase.getRawId());
					DBUtil.set(pstmt, 3,  userBase.getState() != UserProtos.UserBase.State.DELETE && userBase.hasRawId(), userBase.getRawId());
					
					DBUtil.set(pstmt, 4,  userBase.hasUserName(), userBase.getUserName());
					DBUtil.set(pstmt, 5,  userBase.hasGender(), userBase.getGender());
					DBUtil.set(pstmt, 6,  userBase.hasAvatar(), userBase.getAvatar());
					DBUtil.set(pstmt, 7,  userBase.hasEmail(), userBase.getEmail());
					DBUtil.set(pstmt, 8,  userBase.hasSignature(), userBase.getSignature());
					DBUtil.set(pstmt, 9,  userBase.hasInterest(), userBase.getInterest());
					DBUtil.set(pstmt, 10, userBase.hasIsExpert(), userBase.getIsExpert());
					DBUtil.set(pstmt, 11, userBase.hasLevelId(), userBase.getLevelId());
					DBUtil.set(pstmt, 12, userBase.hasState(), userBase.getState());
					DBUtil.set(pstmt, 13, userBase.hasCreateAdminId(), userBase.getCreateAdminId());
					DBUtil.set(pstmt, 14, userBase.hasCreateTime(), userBase.getCreateTime());
					DBUtil.set(pstmt, 15, userBase.hasUpdateAdminId(), userBase.getUpdateAdminId());
					DBUtil.set(pstmt, 16, userBase.hasUpdateTime(), userBase.getUpdateTime());
					
					pstmt.addBatch();
				}
				
				pstmt.executeBatch();
				
				rs = pstmt.getGeneratedKeys();
				
				userIdList = new ArrayList<Long>(userBaseList.size());
				while (rs.next()) {
					userIdList.add(rs.getLong(1));
				}
				
				if (userIdList.size() != userBaseList.size()) {
					throw new RuntimeException("insert fail");
				}
				
			} finally {
				DBUtil.closeQuietly(rs);
				DBUtil.closeQuietly(pstmt);
			}
			
			pstmt = null;
			try {
				// insert mobile no
				pstmt = conn.prepareStatement("INSERT INTO weizhu_user_base_mobile_no (company_id, user_id, mobile_no, mobile_no_unique) VALUES (?, ?, ?, ?); ");
				
				Iterator<Long> userIdIt = userIdList.iterator();
				Iterator<UserProtos.UserBase> userBaseIt = userBaseList.iterator();
				while (userIdIt.hasNext() && userBaseIt.hasNext()) {
					Long userId = userIdIt.next();
					UserProtos.UserBase userBase = userBaseIt.next();
					
					for (String mobileNo : userBase.getMobileNoList()) {
						DBUtil.set(pstmt, 1, true, companyId);
						DBUtil.set(pstmt, 2, true, userId);
						DBUtil.set(pstmt, 3, true, mobileNo);
						DBUtil.set(pstmt, 4, userBase.getState() != UserProtos.UserBase.State.DELETE, mobileNo);
						
						pstmt.addBatch();
					}
				}
				
				pstmt.executeBatch();
			} finally {
				DBUtil.closeQuietly(rs);
				DBUtil.closeQuietly(pstmt);
			}

			pstmt = null;
			try {
				// insert phone no
				pstmt = conn.prepareStatement("INSERT INTO weizhu_user_base_phone_no (company_id, user_id, phone_no) VALUES (?, ?, ?); ");
				
				Iterator<Long> userIdIt = userIdList.iterator();
				Iterator<UserProtos.UserBase> userBaseIt = userBaseList.iterator();
				while (userIdIt.hasNext() && userBaseIt.hasNext()) {
					Long userId = userIdIt.next();
					UserProtos.UserBase userBase = userBaseIt.next();
					
					for (String phoneNo : userBase.getPhoneNoList()) {
						DBUtil.set(pstmt, 1, true, companyId);
						DBUtil.set(pstmt, 2, true, userId);
						DBUtil.set(pstmt, 3, true, phoneNo);
						
						pstmt.addBatch();
					}
				}
				
				pstmt.executeBatch();
			} finally {
				DBUtil.closeQuietly(rs);
				DBUtil.closeQuietly(pstmt);
			}
			
			conn.commit();
			
			return userIdList;
		} catch (SQLException e) {
			conn.rollback();
			throw e;
		} finally {
			if  (autoCommit != null) {
				conn.setAutoCommit(autoCommit);
			}
		}
	}
	
	/**
	 * @param conn
	 * @param oldUserBaseMap  不要包含null元素
	 * @param newUserBaseMap  不要包含null元素
	 * @throws SQLException
	 */
	public static void updateUserBase(Connection conn, long companyId,  
			Map<Long, UserProtos.UserBase> oldUserBaseMap, 
			Map<Long, UserProtos.UserBase> newUserBaseMap
			) throws SQLException {
		
		// 必须保证key完全一样
		if (!oldUserBaseMap.keySet().equals(newUserBaseMap.keySet())) {
			throw new IllegalArgumentException("update key not equals");
		}
		
		Boolean autoCommit = null;
		try {
			autoCommit = conn.getAutoCommit();
			conn.setAutoCommit(false);
			
			PreparedStatement pstmt = null;
			try {
				pstmt = conn.prepareStatement("UPDATE weizhu_user_base SET raw_id_unique = ?, user_name = ?, gender = ?, avatar = ?, email = ?, signature = ?, interest = ?, is_expert = ?, level_id = ?, state = ?, update_admin_id = ?, update_time = ? WHERE company_id = ? AND user_id = ?; ");
				
				for (Entry<Long, UserProtos.UserBase> entry : oldUserBaseMap.entrySet()) {
					UserProtos.UserBase oldValue = entry.getValue();
					UserProtos.UserBase newValue = newUserBaseMap.get(entry.getKey());
					
					if (!oldValue.equals(newValue)) {
						
						DBUtil.set(pstmt, 1,  newValue.getState() != UserProtos.UserBase.State.DELETE, newValue.getRawId());
						
						DBUtil.set(pstmt, 2,  newValue.hasUserName(),      newValue.getUserName());
						DBUtil.set(pstmt, 3,  newValue.hasGender(),        newValue.getGender());
						DBUtil.set(pstmt, 4,  newValue.hasAvatar(),        newValue.getAvatar());
						DBUtil.set(pstmt, 5,  newValue.hasEmail(),         newValue.getEmail());
						DBUtil.set(pstmt, 6,  newValue.hasSignature(),     newValue.getSignature());
						DBUtil.set(pstmt, 7,  newValue.hasInterest(),      newValue.getInterest());
						DBUtil.set(pstmt, 8,  newValue.hasIsExpert(),      newValue.getIsExpert());
						DBUtil.set(pstmt, 9,  newValue.hasLevelId(),       newValue.getLevelId());
						DBUtil.set(pstmt, 10, newValue.hasState(),         newValue.getState());
						DBUtil.set(pstmt, 11, newValue.hasUpdateAdminId(), newValue.getUpdateAdminId());
						DBUtil.set(pstmt, 12, newValue.hasUpdateTime(),    newValue.getUpdateTime());
						
						DBUtil.set(pstmt, 13, true, companyId);
						DBUtil.set(pstmt, 14, true, newValue.getUserId());
						
						pstmt.addBatch();
					}
				}
				pstmt.executeBatch();
			} finally {
				DBUtil.closeQuietly(pstmt);
			}
			
			pstmt = null;
			try {
				pstmt = conn.prepareStatement("DELETE FROM weizhu_user_base_mobile_no WHERE company_id = ? AND user_id = ? AND mobile_no = ?; ");
				
				for (Entry<Long, UserProtos.UserBase> entry : oldUserBaseMap.entrySet()) {
					UserProtos.UserBase oldValue = entry.getValue();
					UserProtos.UserBase newValue = newUserBaseMap.get(entry.getKey());
					
					for (String oldMobileNo : oldValue.getMobileNoList()) {
						
						if (!newValue.getMobileNoList().contains(oldMobileNo)) {
							
							DBUtil.set(pstmt, 1, true, companyId);
							DBUtil.set(pstmt, 2, true, oldValue.getUserId());
							DBUtil.set(pstmt, 3, true, oldMobileNo);
							
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
				pstmt = conn.prepareStatement("UPDATE weizhu_user_base_mobile_no SET mobile_no_unique = ? WHERE company_id = ? AND user_id = ? AND mobile_no = ?; ");
				
				for (Entry<Long, UserProtos.UserBase> entry : oldUserBaseMap.entrySet()) {
					UserProtos.UserBase oldValue = entry.getValue();
					UserProtos.UserBase newValue = newUserBaseMap.get(entry.getKey());
					
					if ((oldValue.getState() != UserProtos.UserBase.State.DELETE) != (newValue.getState() != UserProtos.UserBase.State.DELETE)) {
						for (String newMobileNo : newValue.getMobileNoList()) {
							if (oldValue.getMobileNoList().contains(newMobileNo)) {
								
								DBUtil.set(pstmt, 1, newValue.getState() != UserProtos.UserBase.State.DELETE, newMobileNo);
								DBUtil.set(pstmt, 2, true, companyId);
								DBUtil.set(pstmt, 3, true, newValue.getUserId());
								DBUtil.set(pstmt, 4, true, newMobileNo);
								
								pstmt.addBatch();
							}
						}
					}
				}
				pstmt.executeBatch();
			} finally {
				DBUtil.closeQuietly(pstmt);
			}
			
			pstmt = null;
			try {
				pstmt = conn.prepareStatement("INSERT INTO weizhu_user_base_mobile_no (company_id, user_id, mobile_no, mobile_no_unique) VALUES (?, ?, ?, ?); ");
				
				for (Entry<Long, UserProtos.UserBase> entry : oldUserBaseMap.entrySet()) {
					UserProtos.UserBase oldValue = entry.getValue();
					UserProtos.UserBase newValue = newUserBaseMap.get(entry.getKey());
					
					for (String newMobileNo : newValue.getMobileNoList()) {
						
						if (!oldValue.getMobileNoList().contains(newMobileNo)) {
							
							DBUtil.set(pstmt, 1, true, companyId);
							DBUtil.set(pstmt, 2, true, newValue.getUserId());
							DBUtil.set(pstmt, 3, true, newMobileNo);
							DBUtil.set(pstmt, 4, newValue.getState() != UserProtos.UserBase.State.DELETE, newMobileNo);
							
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
				pstmt = conn.prepareStatement("DELETE FROM weizhu_user_base_phone_no WHERE company_id = ? AND user_id = ? AND phone_no = ?; ");
				
				for (Entry<Long, UserProtos.UserBase> entry : oldUserBaseMap.entrySet()) {
					UserProtos.UserBase oldValue = entry.getValue();
					UserProtos.UserBase newValue = newUserBaseMap.get(entry.getKey());
					
					for (String oldPhoneNo : oldValue.getPhoneNoList()) {
						if (!newValue.getPhoneNoList().contains(oldPhoneNo)) {
							
							DBUtil.set(pstmt, 1, true, companyId);
							DBUtil.set(pstmt, 2, true, oldValue.getUserId());
							DBUtil.set(pstmt, 3, true, oldPhoneNo);
							
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
				pstmt = conn.prepareStatement("INSERT INTO weizhu_user_base_phone_no (company_id, user_id, phone_no) VALUES (?, ?, ?); ");
				
				for (Entry<Long, UserProtos.UserBase> entry : oldUserBaseMap.entrySet()) {
					UserProtos.UserBase oldValue = entry.getValue();
					UserProtos.UserBase newValue = newUserBaseMap.get(entry.getKey());
					
					for (String newPhoneNo : newValue.getPhoneNoList()) {
						if (!oldValue.getPhoneNoList().contains(newPhoneNo)) {
							
							DBUtil.set(pstmt, 1, true, companyId);
							DBUtil.set(pstmt, 2, true, newValue.getUserId());
							DBUtil.set(pstmt, 3, true, newPhoneNo);
							
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
	
	public static void deleteUserBase(Connection conn, long companyId, Collection<Long> userIds, @Nullable Long updateAdminId, @Nullable Integer updateTime) throws SQLException {
		if (userIds.isEmpty()) {
			return;
		}
		
		PreparedStatement pstmt = null;
		try {
			pstmt = conn.prepareStatement("UPDATE weizhu_user_base SET state = 'DELETE', raw_id_unique = ?, update_admin_id = ?, update_time = ? WHERE company_id = ? AND user_id = ?; UPDATE weizhu_user_base_mobile_no SET mobile_no_unique = ? WHERE company_id = ? AND user_id = ?;");
			
			for (Long userId : userIds) {
				
				DBUtil.set(pstmt, 1, false, false);
				DBUtil.set(pstmt, 2, updateAdminId != null, updateAdminId == null ? 0 : updateAdminId);
				DBUtil.set(pstmt, 3, updateTime != null, updateTime == null ? 0 : updateTime);
				DBUtil.set(pstmt, 4, true, companyId);
				DBUtil.set(pstmt, 5, true, userId);
				
				DBUtil.set(pstmt, 6, false, false);
				DBUtil.set(pstmt, 7, true, companyId);
				DBUtil.set(pstmt, 8, true, userId);
				
				pstmt.addBatch();
			}
			pstmt.executeBatch();
		} finally {
			DBUtil.closeQuietly(pstmt);
		}
	}
	
	public static DataPage<Long> getUserIdPage(Connection conn, long companyId, int start, int length, @Nullable Set<UserProtos.UserBase.State> userStateSet) throws SQLException {
		if (userStateSet != null && userStateSet.isEmpty()) {
			return new DataPage<Long>(Collections.<Long>emptyList(), 0, 0);
		}
		
		if (start < 0) {
			start = 0;
		}
		if (length < 0) {
			length = 0;
		}
		
		StringBuilder sqlBuilder = new StringBuilder();
		sqlBuilder.append("SELECT user_id FROM weizhu_user_base WHERE company_id = ");
		sqlBuilder.append(companyId);
		
		if (userStateSet != null) {
			sqlBuilder.append(" AND state IN ('");
			DBUtil.QUOTE_COMMA_JOINER.appendTo(sqlBuilder, userStateSet);
			sqlBuilder.append("')");
		}
		
		sqlBuilder.append(" ORDER BY user_id DESC LIMIT ");
		sqlBuilder.append(start).append(", ");
		sqlBuilder.append(length).append("; ");
		
		sqlBuilder.append("SELECT count(user_id) as total_size FROM weizhu_user_base WHERE company_id = ");
		sqlBuilder.append(companyId);
		
		if (userStateSet != null) {
			sqlBuilder.append(" AND state IN ('");
			DBUtil.QUOTE_COMMA_JOINER.appendTo(sqlBuilder, userStateSet);
			sqlBuilder.append("')");
		}

		sqlBuilder.append("; ");
		
		String sql = sqlBuilder.toString();
		
		Statement stmt = null;
		ResultSet rs = null;
		try {
			stmt = conn.createStatement();
			stmt.execute(sql);
			
			rs = stmt.getResultSet();
			
			List<Long> list = new ArrayList<Long>();
			while(rs.next()) {
				list.add(rs.getLong("user_id"));
			}
			
			DBUtil.closeQuietly(rs);
			rs = null;
			
			stmt.getMoreResults();
			rs = stmt.getResultSet();
			
			if (!rs.next()) {
				throw new RuntimeException("cannot get total_size");
			}
			
			int totalSize = rs.getInt("total_size");
			
			return new DataPage<Long>(list, totalSize, totalSize);
			
		} finally {
			DBUtil.closeQuietly(rs);
			DBUtil.closeQuietly(stmt);
		}
	}
	
	public static DataPage<Long> getUserIdPage(Connection conn, long companyId, int start, int length, @Nullable Set<UserProtos.UserBase.State> userStateSet, 
			@Nullable Boolean isExpert, 
			@Nullable Collection<Integer> teamIds, 
			@Nullable Integer positionId,
			@Nullable String keyword,
			@Nullable String mobileNo
			) throws SQLException {
		
		if (isExpert == null && (teamIds == null || teamIds.isEmpty()) && positionId == null && keyword == null && mobileNo == null) {
			return getUserIdPage(conn, companyId, start, length, userStateSet);
		}
		
		if (userStateSet != null && userStateSet.isEmpty()) {
			return new DataPage<Long>(Collections.<Long>emptyList(), 0, 0);
		}
		
		if (teamIds != null && teamIds.isEmpty()) {
			return new DataPage<Long>(Collections.<Long>emptyList(), 0, 0);
		}
		
		if (start < 0) {
			start = 0;
		}
		if (length < 0) {
			length = 0;
		}
		
		StringBuilder condSb = new StringBuilder();
		if (userStateSet != null) {
			condSb.append(" AND state IN ('");
			DBUtil.QUOTE_COMMA_JOINER.appendTo(condSb, userStateSet);
			condSb.append("')");
		}
		if (isExpert != null) {
			condSb.append(" AND is_expert = ").append(isExpert ? "1" : "0");
		}
		if (teamIds != null) {
			condSb.append(" AND user_id IN ( SELECT user_id FROM weizhu_user_team WHERE team_id IN (");
			DBUtil.COMMA_JOINER.appendTo(condSb, teamIds);
			condSb.append(")");
			if (positionId != null) {
				condSb.append(" AND position_id = ").append(positionId).append(")");
			} else {
				condSb.append(")");
			}
		} else if (positionId != null) {
			condSb.append(" AND user_id IN ( SELECT user_id FROM weizhu_user_team WHERE position_id = ").append(positionId).append(")");
		}
		if (keyword != null && !keyword.isEmpty()) {
			condSb.append(" AND user_name LIKE '%").append(DBUtil.SQL_LIKE_STRING_ESCAPER.escape(keyword)).append("%'");
		}
		if (mobileNo != null && !mobileNo.isEmpty()) {
			condSb.append(" AND user_id IN ( SELECT user_id FROM weizhu_user_base_mobile_no WHERE mobile_no LIKE '%").append(DBUtil.SQL_LIKE_STRING_ESCAPER.escape(mobileNo)).append("%' )");
		}
		
		StringBuilder sqlBuilder = new StringBuilder();
		sqlBuilder.append("SELECT user_id FROM weizhu_user_base WHERE company_id = ").append(companyId).append(condSb);
		sqlBuilder.append(" ORDER BY user_id DESC LIMIT ").append(start).append(", ").append(length).append("; ");
		sqlBuilder.append("SELECT count(user_id) as filtered_size FROM weizhu_user_base WHERE company_id = ").append(companyId).append(" ").append(condSb).append("; ");
		sqlBuilder.append("SELECT count(user_id) as total_size FROM weizhu_user_base WHERE company_id = ").append(companyId);
		
		if (userStateSet != null) {
			sqlBuilder.append(" AND state IN ('");
			DBUtil.QUOTE_COMMA_JOINER.appendTo(sqlBuilder, userStateSet);
			sqlBuilder.append("')");
		}
		sqlBuilder.append("; ");
		
		String sql = sqlBuilder.toString();
		
		Statement stmt = null;
		ResultSet rs = null;
		try {
			stmt = conn.createStatement();
			stmt.execute(sql);
			
			rs = stmt.getResultSet();
			
			List<Long> list = new ArrayList<Long>();
			while(rs.next()) {
				list.add(rs.getLong("user_id"));
			}
			
			DBUtil.closeQuietly(rs);
			rs = null;
			
			stmt.getMoreResults();
			rs = stmt.getResultSet();
			
			if (!rs.next()) {
				throw new RuntimeException("cannot get filtered_size");
			}
			
			int filteredSize = rs.getInt("filtered_size");
			
			DBUtil.closeQuietly(rs);
			rs = null;
			
			stmt.getMoreResults();
			rs = stmt.getResultSet();
			
			if (!rs.next()) {
				throw new RuntimeException("cannot get total_size");
			}
			
			int totalSize = rs.getInt("total_size");
			
			return new DataPage<Long>(list, totalSize, filteredSize);
			
		} finally {
			DBUtil.closeQuietly(rs);
			DBUtil.closeQuietly(stmt);
		}
	}
	
	public static void setUserExpert(Connection conn, long companyId, long userId, boolean isExpert) throws SQLException {
		PreparedStatement pstmt = null;
		try {
			pstmt = conn.prepareStatement("UPDATE weizhu_user_base SET is_expert = ? WHERE company_id = ? AND user_id = ?; ");
			DBUtil.set(pstmt, 1, true, isExpert);
			DBUtil.set(pstmt, 2, true, companyId);
			DBUtil.set(pstmt, 3, true, userId);
			
			pstmt.executeUpdate();
			
		} finally {
			DBUtil.closeQuietly(pstmt);
		}
	}
	
	public static void setUserState(Connection conn, long companyId, Set<Long> userIdSet, UserProtos.UserBase.State state) throws SQLException {
		if (userIdSet.isEmpty()) {
			return;
		}
		
		if (state == UserProtos.UserBase.State.DELETE) {
			PreparedStatement pstmt = null;
			try {
				pstmt = conn.prepareStatement("UPDATE weizhu_user_base SET raw_id_unique = NULL, state = 'DELETE' WHERE company_id = ? AND user_id = ?; UPDATE weizhu_user_base_mobile_no SET mobile_no_unique = NULL WHERE company_id = ? AND user_id = ?; ");
				
				for (Long userId : userIdSet) {
					DBUtil.set(pstmt, 1, true, companyId);
					DBUtil.set(pstmt, 2, true, userId);
					DBUtil.set(pstmt, 3, true, companyId);
					DBUtil.set(pstmt, 4, true, userId);
					
					pstmt.addBatch();
				}
				pstmt.executeBatch();
			} finally {
				DBUtil.closeQuietly(pstmt);
			}
		} else {
			PreparedStatement pstmt = null;
			try {
				pstmt = conn.prepareStatement("UPDATE weizhu_user_base SET state = ? WHERE company_id = ? AND user_id = ?; ");
				for (Long userId : userIdSet) {
				
					DBUtil.set(pstmt, 1, true, state);
					DBUtil.set(pstmt, 2, true, companyId);
					DBUtil.set(pstmt, 3, true, userId);
					
					pstmt.addBatch();
				}
				pstmt.executeBatch();
			} finally {
				DBUtil.closeQuietly(pstmt);
			}
		}
	}
	
	/* for import user */
	
	public static Map<String, Long> getUserIdByRawIdUnique(Connection conn, long companyId, Collection<String> rawIds) throws SQLException {
		if (rawIds.isEmpty()) {
			return Collections.emptyMap();
		}
		
		// 小心SQL注入
		StringBuilder sqlBuilder = new StringBuilder();
		sqlBuilder.append("SELECT user_id, raw_id_unique FROM weizhu_user_base WHERE company_id = ").append(companyId).append(" AND raw_id_unique IN ('");
		DBUtil.QUOTE_COMMA_JOINER.appendTo(sqlBuilder, Iterables.transform(rawIds, DBUtil.SQL_STRING_ESCAPER.asFunction()));
		sqlBuilder.append("'); ");
		
		String sql = sqlBuilder.toString();
		
		Statement stmt = null;
		ResultSet rs = null;
		try {
			stmt = conn.createStatement();
			rs = stmt.executeQuery(sql);
			
			Map<String, Long> rawIdToUserIdMap = new HashMap<String, Long>(rawIds.size());
			while (rs.next()) {
				long userId = rs.getLong("user_id");
				String rawId = rs.getString("raw_id_unique");
				rawIdToUserIdMap.put(rawId, userId);
			}
			return rawIdToUserIdMap;
		} finally {
			DBUtil.closeQuietly(rs);
			DBUtil.closeQuietly(stmt);
		}
	}
	
	public static Map<String, Long> getMobileNoUniqueExcludeUserId(Connection conn, long companyId, Collection<Long> excludeUserIds) throws SQLException {
		StringBuilder sqlBuilder = new StringBuilder();
		sqlBuilder.append("SELECT mobile_no_unique, user_id FROM weizhu_user_base_mobile_no WHERE company_id = ").append(companyId);
		if (!excludeUserIds.isEmpty()) {
			sqlBuilder.append(" AND user_id NOT IN (");
			DBUtil.COMMA_JOINER.appendTo(sqlBuilder, excludeUserIds);
			sqlBuilder.append(")");
		}
		sqlBuilder.append("; ");
		
		String sql = sqlBuilder.toString();
		
		Statement stmt = null;
		ResultSet rs = null;
		try {
			stmt = conn.createStatement();
			rs = stmt.executeQuery(sql);
			
			Map<String, Long> mobileNoToUserIdMap = new HashMap<String, Long>();
			while (rs.next()) {
				mobileNoToUserIdMap.put(rs.getString("mobile_no_unique"), rs.getLong("user_id"));
			}
			return mobileNoToUserIdMap;
		} finally {
			DBUtil.closeQuietly(rs);
			DBUtil.closeQuietly(stmt);
		}
	}
	
}
