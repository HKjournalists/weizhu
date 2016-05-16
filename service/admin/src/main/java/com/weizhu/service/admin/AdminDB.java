package com.weizhu.service.admin;

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
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;

import javax.annotation.Nullable;

import com.google.common.base.Functions;
import com.google.common.collect.Iterables;
import com.weizhu.common.db.DBUtil;
import com.weizhu.common.db.ProtobufMapper;
import com.weizhu.common.utils.DataPage;
import com.weizhu.proto.AdminProtos;

public class AdminDB {
	
	private static final ProtobufMapper<AdminProtos.AdminSessionData> ADMIN_SESSION_DATA_MAPPER = 
			ProtobufMapper.createMapper(AdminProtos.AdminSessionData.getDefaultInstance(), 
					"session.admin_id", 
					"session.session_id",
					"login_time",
					"login_host",
					"user_agent",
					"active_time",
					"logout_time");
	
	public static Map<Long, AdminProtos.AdminSessionData> getLatestAdminSession(Connection conn, 
			Set<Long> adminIdSet
			) throws SQLException {
		if (adminIdSet.isEmpty()) {
			return Collections.emptyMap();
		}
		
		StringBuilder sqlBuilder = new StringBuilder();
		for (Long adminId : adminIdSet) {
			sqlBuilder.append("SELECT admin_id as `session.admin_id`, session_id as `session.session_id`, login_time, login_host, user_agent, active_time, logout_time FROM weizhu_admin_session WHERE admin_id = ");
			sqlBuilder.append(adminId).append(" ORDER BY login_time DESC, session_id DESC LIMIT 1; ");
		}
		
		final String sql = sqlBuilder.toString();
		Statement stmt = null;
		ResultSet rs = null;
		try {
			stmt = conn.createStatement();
			
			Map<Long, AdminProtos.AdminSessionData> resultMap = new TreeMap<Long, AdminProtos.AdminSessionData>();
			AdminProtos.AdminSessionData.Builder tmpBuilder = AdminProtos.AdminSessionData.newBuilder();
			for (int i=0; i<adminIdSet.size(); ++i) {
				if (i == 0) {
					stmt.execute(sql);
					rs = stmt.getResultSet();
				} else {
					DBUtil.closeQuietly(rs);
					rs = null;
					stmt.getMoreResults();
					rs = stmt.getResultSet();
				}
				
				while (rs.next()) {
					tmpBuilder.clear();
					
					AdminProtos.AdminSessionData sessionData = ADMIN_SESSION_DATA_MAPPER.mapToItem(rs, tmpBuilder).build();
					resultMap.put(sessionData.getSession().getAdminId(), sessionData);
				}
			}
			return resultMap;
		} finally {
			DBUtil.closeQuietly(rs);
			DBUtil.closeQuietly(stmt);
		}
	}
	
	public static void insertAdminSession(Connection conn, 
			AdminProtos.AdminSessionData sessionData
			) throws SQLException {
		PreparedStatement pstmt = null;
		try {
			pstmt = conn.prepareStatement("INSERT INTO weizhu_admin_session (admin_id, session_id, login_time, login_host, user_agent, active_time, logout_time) VALUES (?, ?, ?, ?, ?, ?, ?); ");

			DBUtil.set(pstmt, 1, sessionData.getSession().getAdminId());
			DBUtil.set(pstmt, 2, sessionData.getSession().getSessionId());
			DBUtil.set(pstmt, 3, sessionData.getLoginTime());
			DBUtil.set(pstmt, 4, sessionData.getLoginHost());
			DBUtil.set(pstmt, 5, sessionData.getUserAgent());
			DBUtil.set(pstmt, 6, sessionData.getActiveTime());
			DBUtil.set(pstmt, 7, sessionData.hasLogoutTime(), sessionData.getLogoutTime());
			
			if (pstmt.executeUpdate() <= 0) {
				throw new RuntimeException("insert admin session fail");
			}
		} finally {
			DBUtil.closeQuietly(pstmt);
		}
	}
	
	public static boolean updateAdminSessionUserAgentAndActiveTime(Connection conn, 
			AdminProtos.AdminSession session, 
			String userAgent, 
			int activeTime
			) throws SQLException {
		PreparedStatement pstmt = null;
		try {
			pstmt = conn.prepareStatement("UPDATE weizhu_admin_session SET user_agent = ?, active_time = ? WHERE admin_id = ? AND session_id = ? AND logout_time IS NOT NULL; ");
			DBUtil.set(pstmt, 1, userAgent);
			DBUtil.set(pstmt, 2, activeTime);
			DBUtil.set(pstmt, 3, session.getAdminId());
			DBUtil.set(pstmt, 4, session.getSessionId());
			
			return pstmt.executeUpdate() > 0;
		} finally {
			DBUtil.closeQuietly(pstmt);
		}
	}
	
	public static boolean updateAdminSessionLogoutTime(Connection conn, 
			AdminProtos.AdminSession session, 
			int logoutTime
			) throws SQLException {
		PreparedStatement pstmt = null;
		try {
			pstmt = conn.prepareStatement("UPDATE weizhu_admin_session SET logout_time = ? WHERE admin_id = ? AND session_id = ? AND logout_time IS NULL; ");
			DBUtil.set(pstmt, 1, logoutTime);
			DBUtil.set(pstmt, 2, session.getAdminId());
			DBUtil.set(pstmt, 3, session.getSessionId());
			
			return pstmt.executeUpdate() > 0;
		} finally {
			DBUtil.closeQuietly(pstmt);
		}
	}
	
	public static DataPage<Long> getAdminIdPage(Connection conn, long companyId, 
			int start, 
			int length,
			@Nullable Collection<AdminProtos.State> totalStates
			) throws SQLException {
		if (start < 0) {
			start = 0;
		}
		if (length < 0) {
			length = 0;
		}
		if (totalStates != null && totalStates.isEmpty()) {
			return new DataPage<Long>(Collections.<Long> emptyList(), 0, 0);
		}
		
		StringBuilder whereBuilder = new StringBuilder();
		whereBuilder.append("WHERE admin_id IN (SELECT DISTINCT admin_id FROM weizhu_admin_company WHERE company_id = ");
		whereBuilder.append(companyId).append(")");
		if (totalStates != null) {
			whereBuilder.append(" AND state IN ('");
			DBUtil.QUOTE_COMMA_JOINER.appendTo(whereBuilder, Iterables.transform(Iterables.transform(totalStates, Functions.toStringFunction()), DBUtil.SQL_STRING_ESCAPER.asFunction()));
			whereBuilder.append("')");
		}
		final String where = whereBuilder.toString();
		
		StringBuilder sqlBuilder = new StringBuilder();
		sqlBuilder.append("SELECT admin_id FROM weizhu_admin ");
		sqlBuilder.append(where).append(" ORDER BY create_time DESC, admin_id DESC LIMIT ");
		sqlBuilder.append(start).append(", ");
		sqlBuilder.append(length).append("; ");
		sqlBuilder.append("SELECT count(*) AS total_size FROM weizhu_admin ");
		sqlBuilder.append(where).append("; ");

		final String sql = sqlBuilder.toString();
		Statement stmt = null;
		ResultSet rs = null;
		try {
			stmt = conn.createStatement();
			stmt.execute(sql);
			rs = stmt.getResultSet();

			List<Long> adminIdList = new ArrayList<Long>();
			while (rs.next()) {
				adminIdList.add(rs.getLong("admin_id"));
			}

			DBUtil.closeQuietly(rs);
			rs = null;
			stmt.getMoreResults();
			rs = stmt.getResultSet();

			final int totalSize;
			if (rs.next()) {
				totalSize = rs.getInt("total_size");
			} else {
				throw new RuntimeException("cannot get total size");
			}

			return new DataPage<Long>(adminIdList, totalSize, totalSize);
		} finally {
			DBUtil.closeQuietly(rs);
			DBUtil.closeQuietly(stmt);
		}
	}

	public static DataPage<Long> getAdminIdPage(Connection conn, long companyId, 
			int start, 
			int length,
			@Nullable AdminProtos.State state, 
			@Nullable String nameKeyword,
			@Nullable Collection<AdminProtos.State> totalStates
			) throws SQLException {
		if (state == null && nameKeyword == null) {
			return getAdminIdPage(conn, companyId, start, length, totalStates);
		}
		
		if (start < 0) {
			start = 0;
		}
		if (length < 0) {
			length = 0;
		}
		if (totalStates != null && totalStates.isEmpty()) {
			return new DataPage<Long>(Collections.<Long>emptyList(), 0, 0);
		}
		
		StringBuilder totalWhereBuilder = new StringBuilder();
		totalWhereBuilder.append("WHERE admin_id IN (SELECT DISTINCT admin_id FROM weizhu_admin_company WHERE company_id = ");
		totalWhereBuilder.append(companyId).append(")");
		if (totalStates != null) {
			totalWhereBuilder.append(" AND state IN ('");
			DBUtil.QUOTE_COMMA_JOINER.appendTo(totalWhereBuilder, Iterables.transform(Iterables.transform(totalStates, Functions.toStringFunction()), DBUtil.SQL_STRING_ESCAPER.asFunction()));
			totalWhereBuilder.append("')");
		}
		final String totalWhere = totalWhereBuilder.toString();
		
		StringBuilder filterWhereBuilder = new StringBuilder();
		filterWhereBuilder.append("WHERE admin_id IN (SELECT DISTINCT admin_id FROM weizhu_admin_company WHERE company_id = ");
		filterWhereBuilder.append(companyId).append(")");
		if (state != null) {
			filterWhereBuilder.append(" AND state = '");
			filterWhereBuilder.append(DBUtil.SQL_STRING_ESCAPER.escape(state.name())).append("'");
		} else if (totalStates != null){
			filterWhereBuilder.append(" AND state IN ('");
			DBUtil.QUOTE_COMMA_JOINER.appendTo(filterWhereBuilder, Iterables.transform(Iterables.transform(totalStates, Functions.toStringFunction()), DBUtil.SQL_STRING_ESCAPER.asFunction()));
			filterWhereBuilder.append("')");
		}
		if (nameKeyword != null) {
			filterWhereBuilder.append(" AND admin_name LIKE '%");
			filterWhereBuilder.append(DBUtil.SQL_LIKE_STRING_ESCAPER.escape(nameKeyword)).append("%'");
		}
		final String filterWhere = filterWhereBuilder.toString();
		
		StringBuilder sqlBuilder = new StringBuilder();
		sqlBuilder.append("SELECT admin_id FROM weizhu_admin ");
		sqlBuilder.append(filterWhere).append(" ORDER BY create_time DESC, admin_id DESC LIMIT ");
		sqlBuilder.append(start).append(", ");
		sqlBuilder.append(length).append("; ");
		sqlBuilder.append("SELECT count(*) AS filtered_size FROM weizhu_admin ");
		sqlBuilder.append(filterWhere).append("; ");
		sqlBuilder.append("SELECT count(*) AS total_size FROM weizhu_admin ");
		sqlBuilder.append(totalWhere).append("; ");

		final String sql = sqlBuilder.toString();
		Statement stmt = null;
		ResultSet rs = null;
		try {
			stmt = conn.createStatement();
			stmt.execute(sql);
			rs = stmt.getResultSet();

			List<Long> adminIdList = new ArrayList<Long>();
			while (rs.next()) {
				adminIdList.add(rs.getLong("admin_id"));
			}

			DBUtil.closeQuietly(rs);
			rs = null;
			stmt.getMoreResults();
			rs = stmt.getResultSet();

			final int filteredSize;
			if (rs.next()) {
				filteredSize = rs.getInt("filtered_size");
			} else {
				throw new RuntimeException("cannot get filtered size");
			}

			DBUtil.closeQuietly(rs);
			rs = null;
			stmt.getMoreResults();
			rs = stmt.getResultSet();

			final int totalSize;
			if (rs.next()) {
				totalSize = rs.getInt("total_size");
			} else {
				throw new RuntimeException("cannot get total size");
			}

			return new DataPage<Long>(adminIdList, totalSize, filteredSize);
		} finally {
			DBUtil.closeQuietly(rs);
			DBUtil.closeQuietly(stmt);
		}
	}
	
	public static Long getAdminIdByEmailUnique(Connection conn, 
			String adminEmail
			) throws SQLException {
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try {
			pstmt = conn.prepareStatement("SELECT admin_id FROM weizhu_admin WHERE admin_email_unique = ?; ");
			DBUtil.set(pstmt, 1, adminEmail);
			
			rs = pstmt.executeQuery();
			
			if (rs.next()) {
				long adminId = rs.getLong("admin_id");
				return rs.wasNull() ? null : adminId;
			} else {
				return null;
			}
		} finally {
			DBUtil.closeQuietly(rs);
			DBUtil.closeQuietly(pstmt);
		}
	}
	
	public static Long getAdminIdByEmailUniqueAndPassword(Connection conn, 
			String adminEmail, 
			String adminPassword
			) throws SQLException {
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try {
			pstmt = conn.prepareStatement("SELECT admin_id FROM weizhu_admin WHERE admin_email_unique = ? AND admin_password = ?; ");
			pstmt.setString(1, adminEmail);
			pstmt.setString(2, adminPassword);
			
			rs = pstmt.executeQuery();
			
			if (rs.next()) {
				long adminId = rs.getLong("admin_id");
				return rs.wasNull() ? null : adminId;
			} else {
				return null;
			}
		} finally {
			DBUtil.closeQuietly(rs);
			DBUtil.closeQuietly(pstmt);
		}
	}

	private static final ProtobufMapper<AdminProtos.Admin> ADMIN_MAPPER = 
			ProtobufMapper.createMapper(AdminProtos.Admin.getDefaultInstance(), 
					"admin_id",
					"admin_name",
					"admin_email",
					"force_reset_password",
					"state",
					"create_time",
					"create_admin_id",
					"update_time",
					"update_admin_id"
					);
	
	public static Map<Long, AdminProtos.Admin> getAdmin(Connection conn, 
			Collection<Long> adminIds, 
			@Nullable Collection<AdminProtos.State> states
			) throws SQLException {
		if (adminIds.isEmpty() || (states != null && states.isEmpty())) {
			return Collections.emptyMap();
		}
		
		String adminIdStr = DBUtil.COMMA_JOINER.join(adminIds);
		
		StringBuilder sqlBuilder = new StringBuilder();
		sqlBuilder.append("SELECT * FROM weizhu_admin_company_role WHERE admin_id IN (").append(adminIdStr).append(") ORDER BY admin_id, company_id, role_id; "); 
		sqlBuilder.append("SELECT * FROM weizhu_admin_company_team_permit WHERE admin_id IN (").append(adminIdStr).append(") ORDER BY admin_id, company_id, permit_team_id; ");
		sqlBuilder.append("SELECT * FROM weizhu_admin_company WHERE admin_id IN (").append(adminIdStr).append(") ORDER BY admin_id, company_id; ");
		sqlBuilder.append("SELECT * FROM weizhu_admin WHERE admin_id IN (").append(adminIdStr).append(")");
		if (states != null) {
			sqlBuilder.append(" AND state IN ('");
			DBUtil.QUOTE_COMMA_JOINER.appendTo(sqlBuilder, Iterables.transform(Iterables.transform(states, Functions.toStringFunction()), DBUtil.SQL_STRING_ESCAPER.asFunction()));
			sqlBuilder.append("')");
		}
		sqlBuilder.append("; ");
		
		final String sql = sqlBuilder.toString();
		Statement stmt = null;
		ResultSet rs = null;
		try {
			stmt = conn.createStatement();
			stmt.execute(sql);
			rs = stmt.getResultSet();
			
			Map<Long, Map<Long, List<Integer>>> adminCompanyRoleIdMap = new TreeMap<Long, Map<Long, List<Integer>>>();
			while (rs.next()) {
				long adminId = rs.getLong("admin_id");
				long companyId = rs.getLong("company_id");
				int roleId = rs.getInt("role_id");
				
				Map<Long, List<Integer>> map = adminCompanyRoleIdMap.get(adminId);
				if (map == null) {
					map = new TreeMap<Long, List<Integer>>();
					adminCompanyRoleIdMap.put(adminId, map);
				}
				List<Integer> list = map.get(companyId);
				if (list == null) {
					list = new ArrayList<Integer>();
					map.put(companyId, list);
				}
				list.add(roleId);
			}
			
			DBUtil.closeQuietly(rs);
			rs = null;
			stmt.getMoreResults();
			rs = stmt.getResultSet();
			
			Map<Long, Map<Long, List<Integer>>> adminCompanyPermitTeamIdMap = new TreeMap<Long, Map<Long, List<Integer>>>();
			while (rs.next()) {
				long adminId = rs.getLong("admin_id");
				long companyId = rs.getLong("company_id");
				int permitTeamId = rs.getInt("permit_team_id");
				
				Map<Long, List<Integer>> map = adminCompanyPermitTeamIdMap.get(adminId);
				if (map == null) {
					map = new TreeMap<Long, List<Integer>>();
					adminCompanyPermitTeamIdMap.put(adminId, map);
				}
				List<Integer> list = map.get(companyId);
				if (list == null) {
					list = new ArrayList<Integer>();
					map.put(companyId, list);
				}
				list.add(permitTeamId);
			}
			
			DBUtil.closeQuietly(rs);
			rs = null;
			stmt.getMoreResults();
			rs = stmt.getResultSet();
			
			Map<Long, List<AdminProtos.Admin.Company>> adminCompanyMap = new TreeMap<Long, List<AdminProtos.Admin.Company>>();
			AdminProtos.Admin.Company.Builder tmpAdminCompanyBuilder = AdminProtos.Admin.Company.newBuilder();
			while (rs.next()) {
				tmpAdminCompanyBuilder.clear();
				
				long adminId = rs.getLong("admin_id");
				long companyId = rs.getLong("company_id");
				boolean enableTeamPermit = rs.getBoolean("enable_team_permit");
				
				Map<Long, List<Integer>> companyRoleIdMap = adminCompanyRoleIdMap.get(adminId);
				List<Integer> roleIdList = companyRoleIdMap == null ? null : companyRoleIdMap.get(companyId);
				
				Map<Long, List<Integer>> companyPermitTeamIdMap = adminCompanyPermitTeamIdMap.get(adminId);
				List<Integer> permitTeamIdList = companyPermitTeamIdMap == null ? null : companyPermitTeamIdMap.get(companyId);
				
				tmpAdminCompanyBuilder.setCompanyId(companyId);
				if (roleIdList != null) {
					tmpAdminCompanyBuilder.addAllRoleId(roleIdList);
				}
				tmpAdminCompanyBuilder.setEnableTeamPermit(enableTeamPermit);
				if (enableTeamPermit && permitTeamIdList != null) {
					tmpAdminCompanyBuilder.addAllPermitTeamId(permitTeamIdList);
				}
				AdminProtos.Admin.Company adminCompany = tmpAdminCompanyBuilder.build();
				
				List<AdminProtos.Admin.Company> list = adminCompanyMap.get(adminId);
				if (list == null) {
					list = new ArrayList<AdminProtos.Admin.Company>();
					adminCompanyMap.put(adminId, list);
				}
				list.add(adminCompany);
			}
			
			DBUtil.closeQuietly(rs);
			rs = null;
			stmt.getMoreResults();
			rs = stmt.getResultSet();
			
			Map<Long, AdminProtos.Admin> adminMap = new TreeMap<Long, AdminProtos.Admin>();
			AdminProtos.Admin.Builder tmpBuilder = AdminProtos.Admin.newBuilder();
			while (rs.next()) {
				tmpBuilder.clear();
				
				ADMIN_MAPPER.mapToItem(rs, tmpBuilder);
				
				List<AdminProtos.Admin.Company> companyList = adminCompanyMap.get(tmpBuilder.getAdminId());
				if (companyList != null) {
					tmpBuilder.addAllCompany(companyList);
				}
				
				adminMap.put(tmpBuilder.getAdminId(), tmpBuilder.build());
			}
			
			return adminMap;
		} finally {
			DBUtil.closeQuietly(rs);
			DBUtil.closeQuietly(stmt);
		}
	}
	
	public static long insertAdmin(Connection conn, 
			AdminProtos.Admin admin, 
			String adminPassword
			) throws SQLException {
		final long adminId; 
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try {
			pstmt = conn.prepareStatement("INSERT INTO weizhu_admin (admin_id, admin_name, admin_email, admin_email_unique, admin_password, force_reset_password, state, create_time, create_admin_id, update_time, update_admin_id) VALUES (NULL, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?); ", Statement.RETURN_GENERATED_KEYS);
			DBUtil.set(pstmt, 1, admin.getAdminName());
			DBUtil.set(pstmt, 2, admin.getAdminEmail());
			DBUtil.set(pstmt, 3, admin.getState() != AdminProtos.State.DELETE, admin.getAdminEmail());
			DBUtil.set(pstmt, 4, adminPassword);
			DBUtil.set(pstmt, 5, admin.getForceResetPassword());
			DBUtil.set(pstmt, 6, admin.getState());
			DBUtil.set(pstmt, 7, admin.hasCreateTime(), admin.getCreateTime());
			DBUtil.set(pstmt, 8, admin.hasCreateAdminId(), admin.getCreateAdminId());
			DBUtil.set(pstmt, 9, admin.hasUpdateTime(), admin.getUpdateTime());
			DBUtil.set(pstmt, 10, admin.hasUpdateAdminId(), admin.getUpdateAdminId());
			
			pstmt.executeUpdate();
			rs = pstmt.getGeneratedKeys();
			if (!rs.next()) {
				throw new RuntimeException("Cannot get next_admin_id");
			}
			
			adminId = rs.getLong(1);
		} finally {
			DBUtil.closeQuietly(rs);
			DBUtil.closeQuietly(pstmt);
		}
		
		if (admin.getCompanyCount() <= 0) {
			return adminId;
		}
		
		StringBuilder sqlBuilder = new StringBuilder();
		
		boolean isCompanyFirst = true;
		for (AdminProtos.Admin.Company adminCompany : admin.getCompanyList()) {
			if (isCompanyFirst) {
				isCompanyFirst = false;
				sqlBuilder.append("INSERT INTO weizhu_admin_company (admin_id, company_id, enable_team_permit) VALUES ");
			} else {
				sqlBuilder.append(", ");
			}
			
			sqlBuilder.append("(");
			sqlBuilder.append(adminId).append(", ");
			sqlBuilder.append(adminCompany.getCompanyId()).append(", ");
			sqlBuilder.append(adminCompany.getEnableTeamPermit() ? 1 : 0).append(")");
		}
		if (!isCompanyFirst) {
			sqlBuilder.append("; ");
		}
		
		boolean isCompanyRoleFirst = true;
		for (AdminProtos.Admin.Company adminCompany : admin.getCompanyList()) {
			for (Integer roleId : adminCompany.getRoleIdList()) {
				if (isCompanyRoleFirst) {
					isCompanyRoleFirst = false;
					sqlBuilder.append("INSERT INTO weizhu_admin_company_role (admin_id, company_id, role_id) VALUES ");
				} else {
					sqlBuilder.append(", ");
				}
				
				sqlBuilder.append("(");
				sqlBuilder.append(adminId).append(", ");
				sqlBuilder.append(adminCompany.getCompanyId()).append(", ");
				sqlBuilder.append(roleId).append(")");
			}
		}
		if (!isCompanyRoleFirst) {
			sqlBuilder.append("; ");
		}
		
		boolean isCompanyPermitTeamFirst = true;
		for (AdminProtos.Admin.Company adminCompany : admin.getCompanyList()) {
			if (adminCompany.getEnableTeamPermit()) {
				for (Integer permitTeamId : adminCompany.getPermitTeamIdList()) {
					if (isCompanyPermitTeamFirst) {
						isCompanyPermitTeamFirst = false;
						sqlBuilder.append("INSERT INTO weizhu_admin_company_team_permit (admin_id, company_id, permit_team_id) VALUES ");
					} else {
						sqlBuilder.append(", ");
					}
					
					sqlBuilder.append("(");
					sqlBuilder.append(adminId).append(", ");
					sqlBuilder.append(adminCompany.getCompanyId()).append(", ");
					sqlBuilder.append(permitTeamId).append(")");
				}
			}
		}
		if (!isCompanyPermitTeamFirst) {
			sqlBuilder.append("; ");
		}
		
		if (!isCompanyFirst || !isCompanyRoleFirst || !isCompanyPermitTeamFirst ) {
			final String sql = sqlBuilder.toString();
			Statement stmt = null;
			try {
				stmt = conn.createStatement();
				stmt.executeUpdate(sql);
			} finally {
				DBUtil.closeQuietly(pstmt);
			}
		}
		return adminId;
	}
	
	public static void updateAdmin(Connection conn, 
			Map<Long, AdminProtos.Admin> oldAdminMap,
			Map<Long, AdminProtos.Admin> newAdminMap
			) throws SQLException {

		// 必须保证key完全一样
		if (!oldAdminMap.keySet().equals(newAdminMap.keySet())) {
			throw new IllegalArgumentException("update key not equals");
		}
		if (oldAdminMap.isEmpty()) {
			return;
		}
		
		PreparedStatement pstmt = null;
		try {
			pstmt = conn.prepareStatement("UPDATE weizhu_admin SET admin_name = ?, admin_email = ?, admin_email_unique = ?, force_reset_password = ?, state = ?, update_time = ?, update_admin_id = ? WHERE admin_id = ?; ");
			
			for (Entry<Long, AdminProtos.Admin> oldEntry : oldAdminMap.entrySet()) {
				final Long adminId = oldEntry.getKey();
				final AdminProtos.Admin oldAdmin = oldEntry.getValue();
				final AdminProtos.Admin newAdmin = newAdminMap.get(adminId);
				
				if (!oldAdmin.getAdminName().equals(newAdmin.getAdminName())
						|| !oldAdmin.getAdminEmail().equals(newAdmin.getAdminEmail())
						|| oldAdmin.getForceResetPassword() != newAdmin.getForceResetPassword()
						|| oldAdmin.getState() != newAdmin.getState() 
						|| oldAdmin.hasUpdateTime() != newAdmin.hasUpdateTime()
						|| oldAdmin.getUpdateTime() != newAdmin.getUpdateTime()
						|| oldAdmin.hasUpdateAdminId() != newAdmin.hasUpdateAdminId()
						|| oldAdmin.getUpdateAdminId() != newAdmin.getUpdateAdminId()
						) {
					DBUtil.set(pstmt, 1, newAdmin.getAdminName());
					DBUtil.set(pstmt, 2, newAdmin.getAdminEmail());
					DBUtil.set(pstmt, 3, newAdmin.getState() != AdminProtos.State.DELETE, newAdmin.getAdminEmail());
					DBUtil.set(pstmt, 4, newAdmin.getForceResetPassword());
					DBUtil.set(pstmt, 5, newAdmin.getState());
					DBUtil.set(pstmt, 6, newAdmin.hasUpdateTime(), newAdmin.getUpdateTime());
					DBUtil.set(pstmt, 7, newAdmin.hasUpdateAdminId(), newAdmin.getUpdateAdminId());
					
					DBUtil.set(pstmt, 8, newAdmin.getAdminId());					
					pstmt.addBatch();
				}
			}
			pstmt.executeBatch();
		} finally {
			DBUtil.closeQuietly(pstmt);
		}
		
		StringBuilder sqlBuilder = new StringBuilder();
		
		boolean isInsertCompanyFirst = true;
		for (Entry<Long, AdminProtos.Admin> oldEntry : oldAdminMap.entrySet()) {
			final Long adminId = oldEntry.getKey();
			final AdminProtos.Admin oldAdmin = oldEntry.getValue();
			final AdminProtos.Admin newAdmin = newAdminMap.get(adminId);
			
			for (AdminProtos.Admin.Company newAdminCompany : newAdmin.getCompanyList()) {
				boolean isFind = false;
				for (AdminProtos.Admin.Company oldAdminCompany : oldAdmin.getCompanyList()) {
					if (newAdminCompany.getCompanyId() == oldAdminCompany.getCompanyId() 
							&& newAdminCompany.getEnableTeamPermit() == oldAdminCompany.getEnableTeamPermit()
							) {
						isFind = true;
						break;
					}
				}
				
				if (!isFind) {
					if (isInsertCompanyFirst) {
						isInsertCompanyFirst = false;
						sqlBuilder.append("REPLACE INTO weizhu_admin_company (admin_id, company_id, enable_team_permit) VALUES ");
					} else {
						sqlBuilder.append(", ");
					}
					sqlBuilder.append("(");
					sqlBuilder.append(adminId).append(", ");
					sqlBuilder.append(newAdminCompany.getCompanyId()).append(", ");
					sqlBuilder.append(newAdminCompany.getEnableTeamPermit() ? 1 : 0).append(")");
				}
			}
		}
		if (!isInsertCompanyFirst) {
			sqlBuilder.append("; ");
		}
		
		boolean isInsertCompanyRoleFirst = true;
		for (Entry<Long, AdminProtos.Admin> oldEntry : oldAdminMap.entrySet()) {
			final Long adminId = oldEntry.getKey();
			final AdminProtos.Admin oldAdmin = oldEntry.getValue();
			final AdminProtos.Admin newAdmin = newAdminMap.get(adminId);
			
			for (AdminProtos.Admin.Company newAdminCompany : newAdmin.getCompanyList()) {
				AdminProtos.Admin.Company oldAdminCompany = null;
				for (AdminProtos.Admin.Company tmpAdminCompany : oldAdmin.getCompanyList()) {
					if (newAdminCompany.getCompanyId() == tmpAdminCompany.getCompanyId()) {
						oldAdminCompany = tmpAdminCompany;
						break;
					}
				}
				
				for (Integer newAdminRoldId : newAdminCompany.getRoleIdList()) {
					if (oldAdminCompany == null || !oldAdminCompany.getRoleIdList().contains(newAdminRoldId)) {
						if (isInsertCompanyRoleFirst) {
							isInsertCompanyRoleFirst = false;
							sqlBuilder.append("INSERT IGNORE INTO weizhu_admin_company_role (admin_id, company_id, role_id) VALUES ");
						} else {
							sqlBuilder.append(", ");
						}
						sqlBuilder.append("(");
						sqlBuilder.append(adminId).append(", ");
						sqlBuilder.append(newAdminCompany.getCompanyId()).append(", ");
						sqlBuilder.append(newAdminRoldId).append(")");
					}
				}
			}
		}
		if (!isInsertCompanyRoleFirst) {
			sqlBuilder.append("; ");
		}
		
		boolean isInsertCompanyTeamPermitFirst = true;
		for (Entry<Long, AdminProtos.Admin> oldEntry : oldAdminMap.entrySet()) {
			final Long adminId = oldEntry.getKey();
			final AdminProtos.Admin oldAdmin = oldEntry.getValue();
			final AdminProtos.Admin newAdmin = newAdminMap.get(adminId);
			
			for (AdminProtos.Admin.Company newAdminCompany : newAdmin.getCompanyList()) {
				AdminProtos.Admin.Company oldAdminCompany = null;
				for (AdminProtos.Admin.Company tmpAdminCompany : oldAdmin.getCompanyList()) {
					if (newAdminCompany.getCompanyId() == tmpAdminCompany.getCompanyId()) {
						oldAdminCompany = tmpAdminCompany;
						break;
					}
				}
				
				for (Integer newPermitTeamId : newAdminCompany.getPermitTeamIdList()) {
					if (oldAdminCompany == null || !oldAdminCompany.getPermitTeamIdList().contains(newPermitTeamId)) {
						if (isInsertCompanyTeamPermitFirst) {
							isInsertCompanyTeamPermitFirst = false;
							sqlBuilder.append("INSERT IGNORE INTO weizhu_admin_company_team_permit (admin_id, company_id, permit_team_id) VALUES ");
						} else {
							sqlBuilder.append(", ");
						}
						sqlBuilder.append("(");
						sqlBuilder.append(adminId).append(", ");
						sqlBuilder.append(newAdminCompany.getCompanyId()).append(", ");
						sqlBuilder.append(newPermitTeamId).append(")");
					}
				}
			}
		}
		if (!isInsertCompanyTeamPermitFirst) {
			sqlBuilder.append("; ");
		}
		
		boolean isDeleteCompanyFirst = true;
		for (Entry<Long, AdminProtos.Admin> oldEntry : oldAdminMap.entrySet()) {
			final Long adminId = oldEntry.getKey();
			final AdminProtos.Admin oldAdmin = oldEntry.getValue();
			final AdminProtos.Admin newAdmin = newAdminMap.get(adminId);
			
			for (AdminProtos.Admin.Company oldAdminCompany : oldAdmin.getCompanyList()) {
				boolean isFind = false;
				for (AdminProtos.Admin.Company newAdminCompany : newAdmin.getCompanyList()) {
					if (oldAdminCompany.getCompanyId() == newAdminCompany.getCompanyId()) {
						isFind = true;
						break;
					}
				}
				
				if (!isFind) {
					if (isDeleteCompanyFirst) {
						isDeleteCompanyFirst = false;
						sqlBuilder.append("DELETE FROM weizhu_admin_company WHERE (admin_id, company_id) IN (");
					} else {
						sqlBuilder.append(", ");
					}
					sqlBuilder.append("(");
					sqlBuilder.append(adminId).append(", ");
					sqlBuilder.append(oldAdminCompany.getCompanyId()).append(")");
				}
			}
		}
		if (!isDeleteCompanyFirst) {
			sqlBuilder.append("); ");
		}
		
		boolean isDeleteCompanyRoleFirst = true;
		for (Entry<Long, AdminProtos.Admin> oldEntry : oldAdminMap.entrySet()) {
			final Long adminId = oldEntry.getKey();
			final AdminProtos.Admin oldAdmin = oldEntry.getValue();
			final AdminProtos.Admin newAdmin = newAdminMap.get(adminId);
			
			for (AdminProtos.Admin.Company oldAdminCompany : oldAdmin.getCompanyList()) {
				AdminProtos.Admin.Company newAdminCompany = null;
				for (AdminProtos.Admin.Company tmpAdminCompany : newAdmin.getCompanyList()) {
					if (oldAdminCompany.getCompanyId() == tmpAdminCompany.getCompanyId()) {
						newAdminCompany = tmpAdminCompany;
						break;
					}
				}
				
				for (Integer oldAdminRoldId : oldAdminCompany.getRoleIdList()) {
					if (newAdminCompany == null || !newAdminCompany.getRoleIdList().contains(oldAdminRoldId)) {
						if (isDeleteCompanyRoleFirst) {
							isDeleteCompanyRoleFirst = false;
							sqlBuilder.append("DELETE FROM weizhu_admin_company_role WHERE (admin_id, company_id, role_id) IN (");
						} else {
							sqlBuilder.append(", ");
						}
						sqlBuilder.append("(");
						sqlBuilder.append(adminId).append(", ");
						sqlBuilder.append(oldAdminCompany.getCompanyId()).append(", ");
						sqlBuilder.append(oldAdminRoldId).append(")");
					}
				}
			}
		}
		if (!isDeleteCompanyRoleFirst) {
			sqlBuilder.append("); ");
		}
		
		boolean isDeleteCompanyTeamPermitFirst = true;
		for (Entry<Long, AdminProtos.Admin> oldEntry : oldAdminMap.entrySet()) {
			final Long adminId = oldEntry.getKey();
			final AdminProtos.Admin oldAdmin = oldEntry.getValue();
			final AdminProtos.Admin newAdmin = newAdminMap.get(adminId);
			
			for (AdminProtos.Admin.Company oldAdminCompany : oldAdmin.getCompanyList()) {
				AdminProtos.Admin.Company newAdminCompany = null;
				for (AdminProtos.Admin.Company tmpAdminCompany : newAdmin.getCompanyList()) {
					if (oldAdminCompany.getCompanyId() == tmpAdminCompany.getCompanyId()) {
						newAdminCompany = tmpAdminCompany;
						break;
					}
				}
				
				for (Integer oldPermitTeamId : oldAdminCompany.getPermitTeamIdList()) {
					if (newAdminCompany == null || !newAdminCompany.getPermitTeamIdList().contains(oldPermitTeamId)) {
						if (isDeleteCompanyTeamPermitFirst) {
							isDeleteCompanyTeamPermitFirst = false;
							sqlBuilder.append("DELETE FROM weizhu_admin_company_team_permit WHERE (admin_id, company_id, permit_team_id) IN (");
						} else {
							sqlBuilder.append(", ");
						}
						sqlBuilder.append("(");
						sqlBuilder.append(adminId).append(", ");
						sqlBuilder.append(oldAdminCompany.getCompanyId()).append(", ");
						sqlBuilder.append(oldPermitTeamId).append(")");
					}
				}
			}
		}
		if (!isDeleteCompanyTeamPermitFirst) {
			sqlBuilder.append("); ");
		}
		
		if (!isInsertCompanyFirst
				|| !isInsertCompanyRoleFirst
				|| !isInsertCompanyTeamPermitFirst
				|| !isDeleteCompanyFirst
				|| !isDeleteCompanyRoleFirst
				|| !isDeleteCompanyTeamPermitFirst
				) {
			final String sql = sqlBuilder.toString();
			Statement stmt = null;
			try {
				stmt = conn.createStatement();
				stmt.executeUpdate(sql);
			} finally {
				DBUtil.closeQuietly(stmt);
			}
		}
	}
	
	public static void updateAdminState(Connection conn,
			Set<Long> adminIdSet, 
			AdminProtos.State newState
			) throws SQLException {
		if (adminIdSet.isEmpty()) {
			return;
		}
		
		StringBuilder sqlBuilder = new StringBuilder();
		sqlBuilder.append("UPDATE weizhu_admin SET state = '");
		sqlBuilder.append(DBUtil.SQL_STRING_ESCAPER.escape(newState.name())).append("'");
		if (newState == AdminProtos.State.DELETE) {
			sqlBuilder.append(", admin_email_unique = NULL");
		}
		sqlBuilder.append(" WHERE admin_id IN (");
		DBUtil.COMMA_JOINER.appendTo(sqlBuilder, adminIdSet);
		sqlBuilder.append("); ");
		
		final String sql = sqlBuilder.toString();
		Statement stmt = null;
		try {
			stmt = conn.createStatement();
			stmt.executeUpdate(sql);
		} finally {
			DBUtil.closeQuietly(stmt);
		}
	}
	
	public static boolean updateAdminPassword(Connection conn, 
			long adminId, 
			String oldPassword, 
			String newPassword, 
			boolean forceResetPassword, 
			@Nullable Collection<AdminProtos.State> states
			) throws SQLException {
		if (states != null && states.isEmpty()) {
			return false;
		}
		
		StringBuilder sqlBuilder = new StringBuilder();
		sqlBuilder.append("UPDATE weizhu_admin SET admin_password = '");
		sqlBuilder.append(DBUtil.SQL_STRING_ESCAPER.escape(newPassword)).append("', force_reset_password = ");
		sqlBuilder.append(forceResetPassword ? 1 : 0).append(" WHERE admin_id = ");
		sqlBuilder.append(adminId).append(" AND admin_password = '");
		sqlBuilder.append(DBUtil.SQL_STRING_ESCAPER.escape(oldPassword)).append("'");
		if (states != null) {
			sqlBuilder.append(" AND state IN ('");
			DBUtil.QUOTE_COMMA_JOINER.appendTo(sqlBuilder, Iterables.transform(Iterables.transform(states, Functions.toStringFunction()), DBUtil.SQL_STRING_ESCAPER.asFunction()));
			sqlBuilder.append("')");
		}
		sqlBuilder.append("; ");
		
		final String sql = sqlBuilder.toString();
		Statement stmt = null;
		try {
			stmt = conn.createStatement();
			return stmt.executeUpdate(sql) > 0;
		} finally {
			DBUtil.closeQuietly(stmt);
		}
	}
	
	public static boolean updateAdminPassword(Connection conn, 
			long adminId, 
			String newPassword,
			@Nullable Collection<AdminProtos.State> states
			) throws SQLException {
		if (states != null && states.isEmpty()) {
			return false;
		}
		
		StringBuilder sqlBuilder = new StringBuilder();
		sqlBuilder.append("UPDATE weizhu_admin SET admin_password = '");
		sqlBuilder.append(DBUtil.SQL_STRING_ESCAPER.escape(newPassword)).append("' WHERE admin_id = ");
		sqlBuilder.append(adminId);
		if (states != null) {
			sqlBuilder.append(" AND state IN ('");
			DBUtil.QUOTE_COMMA_JOINER.appendTo(sqlBuilder, Iterables.transform(Iterables.transform(states, Functions.toStringFunction()), DBUtil.SQL_STRING_ESCAPER.asFunction()));
			sqlBuilder.append("')");
		}
		sqlBuilder.append("; ");
		
		final String sql = sqlBuilder.toString();
		Statement stmt = null;
		try {
			stmt = conn.createStatement();
			return stmt.executeUpdate(sql) > 0;
		} finally {
			DBUtil.closeQuietly(stmt);
		}
	}
	
	public static DataPage<Integer> getRoleIdPage(Connection conn, long companyId, 
			int start, 
			int length,
			@Nullable Collection<AdminProtos.State> totalStates
			) throws SQLException {
		if (start < 0) {
			start = 0;
		}
		if (length < 0) {
			length = 0;
		}
		if (totalStates != null && totalStates.isEmpty()) {
			return new DataPage<Integer>(Collections.<Integer> emptyList(), 0, 0);
		}
		
		StringBuilder whereBuilder = new StringBuilder();
		whereBuilder.append("WHERE ((company_id IS NULL) OR (company_id = ");
		whereBuilder.append(companyId).append("))");
		if (totalStates != null) {
			whereBuilder.append(" AND state IN ('");
			DBUtil.QUOTE_COMMA_JOINER.appendTo(whereBuilder, Iterables.transform(Iterables.transform(totalStates, Functions.toStringFunction()), DBUtil.SQL_STRING_ESCAPER.asFunction()));
			whereBuilder.append("')");
		}
		final String where = whereBuilder.toString();
		
		StringBuilder sqlBuilder = new StringBuilder();
		sqlBuilder.append("SELECT role_id FROM weizhu_admin_role ");
		sqlBuilder.append(where).append(" ORDER BY create_time DESC, role_id DESC LIMIT ");
		sqlBuilder.append(start).append(", ");
		sqlBuilder.append(length).append("; ");
		sqlBuilder.append("SELECT count(*) AS total_size FROM weizhu_admin_role ");
		sqlBuilder.append(where).append("; ");

		final String sql = sqlBuilder.toString();
		Statement stmt = null;
		ResultSet rs = null;
		try {
			stmt = conn.createStatement();
			stmt.execute(sql);
			rs = stmt.getResultSet();

			List<Integer> roleIdList = new ArrayList<Integer>();
			while (rs.next()) {
				roleIdList.add(rs.getInt("role_id"));
			}

			DBUtil.closeQuietly(rs);
			rs = null;
			stmt.getMoreResults();
			rs = stmt.getResultSet();

			final int totalSize;
			if (rs.next()) {
				totalSize = rs.getInt("total_size");
			} else {
				throw new RuntimeException("cannot get total size");
			}

			return new DataPage<Integer>(roleIdList, totalSize, totalSize);
		} finally {
			DBUtil.closeQuietly(rs);
			DBUtil.closeQuietly(stmt);
		}
	}
	
	private static final ProtobufMapper<AdminProtos.Role> ROLE_MAPPER = 
			ProtobufMapper.createMapper(AdminProtos.Role.getDefaultInstance(), 
					"company_id",
					"role_id",
					"role_name",
					"state",
					"create_time",
					"create_admin_id",
					"update_time",
					"update_admin_id"
					);
	
	public static Map<Integer, AdminProtos.Role> getRole(Connection conn, 
			Collection<Integer> roleIds,
			@Nullable Collection<AdminProtos.State> states
			) throws SQLException {
		if (roleIds.isEmpty() || (states != null && states.isEmpty())) {
			return Collections.emptyMap();
		}
		
		final String roleIdStr = DBUtil.COMMA_JOINER.join(roleIds);
		
		StringBuilder sqlBuilder = new StringBuilder();
		sqlBuilder.append("SELECT * FROM weizhu_admin_role_permission WHERE role_id IN (");
		sqlBuilder.append(roleIdStr).append("); ");
		sqlBuilder.append("SELECT * FROM weizhu_admin_role WHERE role_id IN (");
		sqlBuilder.append(roleIdStr).append(")");
		if (states != null) {
			sqlBuilder.append(" AND state IN ('");
			DBUtil.QUOTE_COMMA_JOINER.appendTo(sqlBuilder, Iterables.transform(Iterables.transform(states, Functions.toStringFunction()), DBUtil.SQL_STRING_ESCAPER.asFunction()));
			sqlBuilder.append("')");
		}
		sqlBuilder.append("; ");
		
		final String sql = sqlBuilder.toString();
		Statement stmt = null;
		ResultSet rs = null;
		try {
			stmt = conn.createStatement();
			stmt.execute(sql);
			rs = stmt.getResultSet();
			
			Map<Integer, List<String>> rolePermissionIdMap = new TreeMap<Integer, List<String>>();
			while (rs.next()) {
				Integer roleId = rs.getInt("role_id");
				String permissionId = rs.getString("permission_id");
				
				List<String> list = rolePermissionIdMap.get(roleId);
				if (list == null) {
					list = new ArrayList<String>();
					rolePermissionIdMap.put(roleId, list);
				}
				list.add(permissionId);
			}
			
			DBUtil.closeQuietly(rs);
			rs = null;
			stmt.getMoreResults();
			rs = stmt.getResultSet();
			
			Map<Integer, AdminProtos.Role> roleMap = new TreeMap<Integer, AdminProtos.Role>();
			AdminProtos.Role.Builder tmpBuilder = AdminProtos.Role.newBuilder();
			while (rs.next()) {
				tmpBuilder.clear();
				
				ROLE_MAPPER.mapToItem(rs, tmpBuilder);
				
				List<String> permissionIdList = rolePermissionIdMap.get(tmpBuilder.getRoleId());
				if (permissionIdList != null) {
					tmpBuilder.addAllPermissionId(permissionIdList);
				}
				roleMap.put(tmpBuilder.getRoleId(), tmpBuilder.build());
			}
			
			return roleMap;
		} finally {
			DBUtil.closeQuietly(rs);
			DBUtil.closeQuietly(stmt);
		}
	}
	
	public static int insertRole(Connection conn, 
			AdminProtos.Role role
			) throws SQLException {
		final int roleId;
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try {
			pstmt = conn.prepareStatement("INSERT INTO weizhu_admin_role (company_id, role_id, role_name, state, create_time, create_admin_id, update_time, update_admin_id) VALUES (?, NULL, ?, ?, ?, ?, ?, ?); ", Statement.RETURN_GENERATED_KEYS);
			DBUtil.set(pstmt, 1, role.hasCompanyId(), role.getCompanyId());
			DBUtil.set(pstmt, 2, role.getRoleName());
			DBUtil.set(pstmt, 3, role.getState());
			DBUtil.set(pstmt, 4, role.hasCreateTime(), role.getCreateTime());
			DBUtil.set(pstmt, 5, role.hasCreateAdminId(), role.getCreateAdminId());
			DBUtil.set(pstmt, 6, role.hasUpdateTime(), role.getUpdateTime());
			DBUtil.set(pstmt, 7, role.hasUpdateAdminId(), role.getUpdateAdminId());
			
			pstmt.executeUpdate();
			rs = pstmt.getGeneratedKeys();
			if (!rs.next()) {
				throw new RuntimeException("Cannot get role id");
			}
			
			roleId = rs.getInt(1);			
		} finally {
			DBUtil.closeQuietly(rs);
			DBUtil.closeQuietly(pstmt);
		}
		
		if (role.getPermissionIdCount() <= 0) {
			return roleId;
		}
		
		StringBuilder sqlBuilder = new StringBuilder();
		sqlBuilder.append("INSERT INTO weizhu_admin_role_permission (role_id, permission_id) VALUES ");
		boolean isFirst = true;
		for (String permissionId : role.getPermissionIdList()) {
			if (isFirst) {
				isFirst = false;
			} else {
				sqlBuilder.append(", ");
			}
			sqlBuilder.append("(");
			sqlBuilder.append(roleId).append(", '");
			sqlBuilder.append(DBUtil.SQL_STRING_ESCAPER.escape(permissionId)).append("')");
		}
		sqlBuilder.append("; ");
		
		final String sql = sqlBuilder.toString();
		Statement stmt = null;
		try {
			stmt = conn.createStatement();
			stmt.executeUpdate(sql);
		} finally {
			DBUtil.closeQuietly(stmt);
		}
		return roleId;
	}
	
	public static void updateRole(Connection conn, 
			Map<Integer, AdminProtos.Role> oldRoleMap,
			Map<Integer, AdminProtos.Role> newRoleMap
			) throws SQLException {
		// 必须保证key完全一样
		if (!oldRoleMap.keySet().equals(newRoleMap.keySet())) {
			throw new IllegalArgumentException("update key not equals");
		}
		if (oldRoleMap.isEmpty()) {
			return;
		}
		
		PreparedStatement pstmt = null;
		try {
			pstmt = conn.prepareStatement("UPDATE weizhu_admin_role SET role_name = ?, state = ?, update_time = ?, update_admin_id = ? WHERE role_id = ?; ");
			
			for (Entry<Integer, AdminProtos.Role> oldEntry : oldRoleMap.entrySet()) {
				final Integer roleId = oldEntry.getKey();
				final AdminProtos.Role oldRole = oldEntry.getValue();
				final AdminProtos.Role newRole = newRoleMap.get(roleId);
				
				if (!oldRole.getRoleName().equals(newRole.getRoleName())
						|| oldRole.getState() != newRole.getState()
						|| oldRole.hasUpdateTime() != newRole.hasUpdateTime()
						|| oldRole.getUpdateTime() != newRole.getUpdateTime()
						|| oldRole.hasUpdateAdminId() != newRole.hasUpdateAdminId()
						|| oldRole.getUpdateAdminId() != newRole.getUpdateAdminId()
						) {
					
					DBUtil.set(pstmt, 1, newRole.getRoleName());
					DBUtil.set(pstmt, 2, newRole.getState());
					DBUtil.set(pstmt, 3, newRole.hasUpdateTime(), newRole.getUpdateTime());
					DBUtil.set(pstmt, 4, newRole.hasUpdateAdminId(), newRole.getUpdateAdminId());
					
					DBUtil.set(pstmt, 5, roleId);
					pstmt.addBatch();
				}
			}
			
			pstmt.executeBatch();
		} finally {
			DBUtil.closeQuietly(pstmt);
		}
		
		StringBuilder sqlBuilder = new StringBuilder();
		
		boolean isInsertFirst = true;
		for (Entry<Integer, AdminProtos.Role> oldEntry : oldRoleMap.entrySet()) {
			final Integer roleId = oldEntry.getKey();
			final AdminProtos.Role oldRole = oldEntry.getValue();
			final AdminProtos.Role newRole = newRoleMap.get(roleId);
		
			for (String newPermissionId : newRole.getPermissionIdList()) {
				if (!oldRole.getPermissionIdList().contains(newPermissionId)) {
					
					if (isInsertFirst) {
						isInsertFirst = false;
						sqlBuilder.append("INSERT INTO weizhu_admin_role_permission (role_id, permission_id) VALUES ");
					} else {
						sqlBuilder.append(", ");
					}
					sqlBuilder.append("(");
					sqlBuilder.append(roleId).append(", '");
					sqlBuilder.append(DBUtil.SQL_STRING_ESCAPER.escape(newPermissionId)).append("')");
				}
			}
		}
		if (!isInsertFirst) {
			sqlBuilder.append("; ");
		}
		
		boolean isDeleteFirst = true;
		for (Entry<Integer, AdminProtos.Role> oldEntry : oldRoleMap.entrySet()) {
			final Integer roleId = oldEntry.getKey();
			final AdminProtos.Role oldRole = oldEntry.getValue();
			final AdminProtos.Role newRole = newRoleMap.get(roleId);
		
			for (String oldPermissionId : oldRole.getPermissionIdList()) {
				if (!newRole.getPermissionIdList().contains(oldPermissionId)) {
					
					if (isDeleteFirst) {
						isDeleteFirst = false;
						sqlBuilder.append("DELETE FROM weizhu_admin_role_permission WHERE (role_id, permission_id) IN (");
					} else {
						sqlBuilder.append(", ");
					}
					sqlBuilder.append("(");
					sqlBuilder.append(roleId).append(", '");
					sqlBuilder.append(DBUtil.SQL_STRING_ESCAPER.escape(oldPermissionId)).append("')");
				}
			}
		}
		if (!isDeleteFirst) {
			sqlBuilder.append("); ");
		}
		
		if (!isInsertFirst || !isDeleteFirst) {
			final String sql = sqlBuilder.toString();
			Statement stmt = null;
			try {
				stmt = conn.createStatement();
				stmt.executeUpdate(sql);
			} finally {
				DBUtil.closeQuietly(stmt);
			}
		}
	}
	
	public static void updateRoleState(Connection conn,
			Collection<Integer> roleIds, 
			AdminProtos.State newState,
			@Nullable Collection<AdminProtos.State> states
			) throws SQLException {
		if (roleIds.isEmpty() || (states != null && states.isEmpty())) {
			return;
		}
		
		StringBuilder sqlBuilder = new StringBuilder();
		sqlBuilder.append("UPDATE weizhu_admin_role SET state = '");
		sqlBuilder.append(DBUtil.SQL_STRING_ESCAPER.escape(newState.name())).append("' WHERE role_id IN (");
		DBUtil.COMMA_JOINER.appendTo(sqlBuilder, roleIds);
		sqlBuilder.append(")");
		if (states != null) {
			sqlBuilder.append(" AND state IN ('");
			DBUtil.QUOTE_COMMA_JOINER.appendTo(sqlBuilder, Iterables.transform(Iterables.transform(states, Functions.toStringFunction()), DBUtil.SQL_STRING_ESCAPER.asFunction()));
			sqlBuilder.append("')");
		}
		sqlBuilder.append("; ");
		
		final String sql = sqlBuilder.toString();
		Statement stmt = null;
		try {
			stmt = conn.createStatement();
			stmt.executeUpdate(sql);
		} finally {
			DBUtil.closeQuietly(stmt);
		}
	}
	
}
