package com.weizhu.service.absence;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.protobuf.InvalidProtocolBufferException;
import com.weizhu.common.db.DBUtil;
import com.weizhu.common.db.ProtobufMapper;
import com.weizhu.common.utils.DataPage;
import com.weizhu.proto.AbsenceProtos;
import com.weizhu.proto.AbsenceProtos.GetAbsenceSerRequest;
import com.weizhu.proto.WeizhuProtos.RequestHead;

public class AbsenceDB {

	private static final ProtobufMapper<AbsenceProtos.Absence> ABSENCE_MAPPER = ProtobufMapper
			.createMapper(AbsenceProtos.Absence.getDefaultInstance(), 
					"absence_id",
					"type",
					"start_time",
					"pre_end_time",
					"fac_end_time",
					"desc",
					"days",
					"state",
					"create_user",
					"create_time");
			
	public static Map<Integer, AbsenceProtos.Absence> getAbsence(Connection conn, long companyId, Collection<Integer> absenceIds) throws SQLException {
		if (absenceIds.isEmpty()) {
			return Maps.newHashMap();
		}
		
		String absenceIdStr = DBUtil.COMMA_JOINER.join(absenceIds);
		
		StringBuilder sql = new StringBuilder();
		sql.append("SELECT * FROM weizhu_absence_notify_user WHERE company_id = ").append(companyId);
		sql.append(" AND absence_id IN (");
		sql.append(absenceIdStr).append("); ");
		sql.append("SELECT * FROM weizhu_absence WHERE company_id = ").append(companyId);
		sql.append(" AND absence_id IN (");
		sql.append(absenceIdStr).append("); ");
		
		Statement stmt = null;
		ResultSet rs = null;
		
		try {
			stmt = conn.createStatement();
			stmt.execute(sql.toString());
			rs = stmt.getResultSet();
			
			Map<Integer, List<Long>> notifyUserMap = Maps.newHashMap();
			while (rs.next()) {
				int absenceId = rs.getInt("absence_id");
				
				List<Long> userIdList = notifyUserMap.get(absenceId);
				if (userIdList == null) {
					userIdList = Lists.newArrayList();
				}
				userIdList.add(rs.getLong("user_id"));
				
				notifyUserMap.put(absenceId, userIdList);
			}
			DBUtil.closeQuietly(rs);
			rs = null;

			stmt.getMoreResults();
			rs = stmt.getResultSet();

			AbsenceProtos.Absence.Builder absenceBuilder = AbsenceProtos.Absence.newBuilder();
			Map<Integer, AbsenceProtos.Absence> absenceMap = Maps.newHashMap();
			while (rs.next()) {
				absenceBuilder.clear();
				
				ABSENCE_MAPPER.mapToItem(rs, absenceBuilder);
				int absenceId = rs.getInt("absence_id");
				List<Long> userIdList = notifyUserMap.get(absenceId) == null ? Collections.emptyList() : notifyUserMap.get(absenceId);
				
				absenceMap.put(absenceId, absenceBuilder.addAllUserId(userIdList).build());
			}
			
			return absenceMap;
		} finally {
			DBUtil.closeQuietly(rs);
			DBUtil.closeQuietly(stmt);
		}
	}
	
	public static Map<Long, AbsenceProtos.Absence> getAbsenceNow(Connection conn, long companyId, int now, Collection<Long> userIds) throws SQLException {
		if (userIds.isEmpty()) {
			return Maps.newHashMap();
		}
		
		StringBuilder sql = new StringBuilder();
		sql.append("SELECT * FROM weizhu_absence WHERE company_id = ").append(companyId).append(" AND ");
		sql.append("fac_end_time IS NULL ").append(" AND ");
		sql.append("create_user IN (").append(DBUtil.COMMA_JOINER.join(userIds)).append("); ");
		
		Statement stmt = null;
		ResultSet rs = null;
		try {
			stmt = conn.createStatement();
			rs = stmt.executeQuery(sql.toString());
			
			Map<Long, AbsenceProtos.Absence> absenceMap = Maps.newHashMap();
			AbsenceProtos.Absence.Builder absenceBuilder = AbsenceProtos.Absence.newBuilder();
			
			List<Integer> absenceIdList = Lists.newArrayList();
			while (rs.next()) {
				absenceBuilder.clear();
				
				ABSENCE_MAPPER.mapToItem(rs, absenceBuilder);
				
				long userId = rs.getLong("create_user");
				
				absenceMap.put(userId, absenceBuilder.build());
				absenceIdList.add(absenceBuilder.getAbsenceId());
			}
			DBUtil.closeQuietly(rs);
			rs = null;
			DBUtil.closeQuietly(stmt);
			stmt = null;
			
			if (absenceMap.isEmpty()) {
				return absenceMap;
			}
			
			StringBuilder sql1 = new StringBuilder();
			sql1.append("SELECT * FROM weizhu_absence_notify_user WHERE company_id = ").append(companyId).append(" AND ");
			sql1.append("absence_id IN (").append(DBUtil.COMMA_JOINER.join(absenceIdList)).append("); ");
			
			stmt = conn.createStatement();
			rs = stmt.executeQuery(sql1.toString());
			Map<Integer, List<Long>> absenceUserMap = Maps.newHashMap();
			while (rs.next()) {
				long userId = rs.getLong("user_id");
				int absenceId = rs.getInt("absence_id");
				
				List<Long> userIdList = absenceUserMap.get(absenceId);
				if (userIdList == null) {
					userIdList = Lists.newArrayList();
				}
				userIdList.add(userId);
				
				absenceUserMap.put(absenceId, userIdList);
			}
			
			Map<Long, AbsenceProtos.Absence> newAbsenceMap = Maps.newHashMap();
			for (AbsenceProtos.Absence value : absenceMap.values()) {
				List<Long> userIdList = absenceUserMap.get(value.getAbsenceId());
				if (userIdList != null && value.hasCreateUser()) {
					newAbsenceMap.put(value.getCreateUser(), AbsenceProtos.Absence.newBuilder()
							.mergeFrom(value)
							.addAllUserId(userIdList)
							.build());
				}
			}
			
			return newAbsenceMap;
		} finally {
			DBUtil.closeQuietly(rs);
			DBUtil.closeQuietly(stmt);
		}
	}
	
	public static int insertAbsence(Connection conn, RequestHead head, long companyId, String type, int startTime, int endTime, String desc, String days, long createUser, int createTime, Collection<Long> userIdList) throws SQLException {
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try {
			pstmt = conn.prepareStatement("INSERT INTO weizhu_absence (company_id, `type`, start_time, pre_end_time, `desc`, days, state, create_user, create_time) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?); ", Statement.RETURN_GENERATED_KEYS);
			DBUtil.set(pstmt, 1, companyId);
			DBUtil.set(pstmt, 2, type);
			DBUtil.set(pstmt, 3, startTime);
			DBUtil.set(pstmt, 4, endTime);
			DBUtil.set(pstmt, 5, desc);
			DBUtil.set(pstmt, 6, days);
			DBUtil.set(pstmt, 7, AbsenceProtos.State.NORMAL.name());
			DBUtil.set(pstmt, 8, createUser);
			DBUtil.set(pstmt, 9, createTime);
			
			pstmt.execute();
			rs = pstmt.getGeneratedKeys();
			if (!rs.next()) {
				throw new RuntimeException("cannot generate key!");
			}
			
			int absenceId = rs.getInt(1);
			
			DBUtil.closeQuietly(rs);
			rs = null;
			DBUtil.closeQuietly(pstmt);
			pstmt = null;
			
			pstmt = conn.prepareStatement("INSERT INTO weizhu_absence_head (company_id, absence_id, head_data) VALUES (?, ?, ?); ");
			DBUtil.set(pstmt, 1, companyId);
			DBUtil.set(pstmt, 2, absenceId);
			DBUtil.set(pstmt, 3, head.toByteString());
			pstmt.executeUpdate();
			
			DBUtil.closeQuietly(pstmt);
			pstmt = null;
			
			if (!userIdList.isEmpty()) {
				pstmt = conn.prepareStatement("INSERT INTO weizhu_absence_notify_user (company_id, absence_id, user_id) VALUES (?, ?, ?); ");
				for (long userId : userIdList) {
					DBUtil.set(pstmt, 1, companyId);
					DBUtil.set(pstmt, 2, absenceId);
					DBUtil.set(pstmt, 3, userId);
					
					pstmt.addBatch();
				}
				pstmt.executeBatch();
			}
			
			return absenceId;
		} finally {
			DBUtil.closeQuietly(rs);
			DBUtil.closeQuietly(pstmt);
		}
	}
	
	public static void cancelAbsence(Connection conn, long companyId, int now, int absenceId, String days) throws SQLException {
		PreparedStatement pstmt = null;
		try {
			pstmt = conn.prepareStatement("UPDATE weizhu_absence SET fac_end_time = ?, days = ? WHERE company_id = ? AND absence_id = ?");
			DBUtil.set(pstmt, 1, now);
			DBUtil.set(pstmt, 2, days);
			DBUtil.set(pstmt, 3, companyId);
			DBUtil.set(pstmt, 4, absenceId);
			
			pstmt.executeUpdate();
		} finally {
			DBUtil.closeQuietly(pstmt);
		}
	}
	
	public static List<Integer> getAbsenceId(Connection conn, long companyId, long userId, @Nullable Integer lastAbsenceId, int size) throws SQLException {
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try {
			if (lastAbsenceId == null) {
				pstmt = conn.prepareStatement("SELECT absence_id FROM weizhu_absence WHERE company_id = ? AND create_user = ? ORDER BY absence_id DESC LIMIT ?; ");
				DBUtil.set(pstmt, 1, companyId);
				DBUtil.set(pstmt, 2, userId);
				DBUtil.set(pstmt, 3, size);
			} else {
				pstmt = conn.prepareStatement("SELECT absence_id FROM weizhu_absence WHERE company_id = ? AND create_user = ? AND absence_id < ? ORDER BY absence_id DESC LIMIT ?; ");
				DBUtil.set(pstmt, 1, companyId);
				DBUtil.set(pstmt, 2, userId);
				DBUtil.set(pstmt, 3, lastAbsenceId);
				DBUtil.set(pstmt, 4, size);
			}
			rs = pstmt.executeQuery();
			
			List<Integer> list = Lists.newArrayList();
			while (rs.next()) {
				list.add(rs.getInt("absence_id"));
			}
			
			return list;
		} finally {
			DBUtil.closeQuietly(pstmt);
		}
	}
	
	/**
	 * 服务端查询请假记录
	 * @param conn
	 * @param companyId
	 * @param start
	 * @param length
	 * @param userIdList
	 * @param startTime
	 * @param endTime
	 * @param action
	 * @return
	 * @throws SQLException
	 */
	public static DataPage<Integer> getAbsenceId(Connection conn, long companyId, int start, int length,
			List<Long> userIdList, @Nullable Integer startTime, @Nullable Integer endTime, @Nullable GetAbsenceSerRequest.Action action) throws SQLException {
		if (length == 0) {
			return new DataPage<Integer>(Lists.newArrayList(), 0, 0);
		}
		
		StringBuilder condition = new StringBuilder();
		if (!userIdList.isEmpty()) {
			condition.append("create_user IN (").append(DBUtil.COMMA_JOINER.join(userIdList)).append(") AND ");
		}
		if (startTime != null) {
			condition.append("(fac_end_time > ").append(startTime).append(" OR fac_end_time IS NULL) AND ");
		}
		if (endTime != null) {
			condition.append("start_time < ").append(endTime).append(" AND ");
		}
		if (action != null) {
			int now = (int) (System.currentTimeMillis() / 1000L);
			if (action.equals(GetAbsenceSerRequest.Action.LEAVE)) {
				condition.append("start_time < ").append(now).append(" AND ").append("fac_end_time IS NULL AND ");
			} else {
				condition.append("fac_end_time IS NOT NULL AND ");
			}
			
		}
		condition.append(" ((fac_end_time IS NOT NULL AND fac_end_time > (start_time + 12*3600)) OR fac_end_time IS NULL) ");
		
		StringBuilder sql = new StringBuilder();
		sql.append("SELECT COUNT(1) FROM weizhu_absence WHERE company_id = ").append(companyId).append(" AND ");
		sql.append(condition).append("; ");
		sql.append("SELECT absence_id FROM weizhu_absence WHERE company_id = ").append(companyId).append(" AND ");
		sql.append(condition);
		sql.append("ORDER BY absence_id DESC ");
		sql.append("LIMIT ").append(start).append(", ").append(length).append("; ");
		
		Statement stmt = null;
		ResultSet rs = null;
		try {
			stmt = conn.createStatement();
			stmt.execute(sql.toString());
			
			rs = stmt.getResultSet();
			if (!rs.next()) {
				throw new RuntimeException("cannot get total!");
			}
			int total = rs.getInt(1);
			
			DBUtil.closeQuietly(rs);
			rs = null;
			
			stmt.getMoreResults();
			rs = stmt.getResultSet();
			
			List<Integer> list = Lists.newArrayList();
			while (rs.next()) {
				list.add(rs.getInt("absence_id"));
			}
			
			return new DataPage<Integer>(list, total, total);
		} finally {
			DBUtil.closeQuietly(rs);
			DBUtil.closeQuietly(stmt);
		}
	}
	
	/**
	 * 获取当前没有销假的
	 * @param conn
	 * @return
	 * @throws SQLException
	 */
	public static Map<Long, List<Integer>> getAbsenceId(Connection conn) throws SQLException {
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try {
			pstmt = conn.prepareStatement("SELECT company_id, absence_id FROM weizhu_absence WHERE fac_end_time IS NULL; ");
			
			rs = pstmt.executeQuery();
			
			Map<Long, List<Integer>> map = Maps.newHashMap();
			List<Integer> list = null;
			while (rs.next()) {
				long companyId = rs.getLong("company_id");
				list = map.get(companyId);
				if (list == null) {
					list = Lists.newArrayList();
				}
				list.add(rs.getInt("absence_id"));
				
				map.put(companyId, list);
			}
			
			return map;
		} finally {
			DBUtil.closeQuietly(rs);
			DBUtil.closeQuietly(pstmt);
		}
	}
	
	public static void updateAbsence(Connection conn, long companyId, String type, int startTime, int preEndTime, @Nullable Integer facEndTime, String days, String desc, int absenceId) throws SQLException {
		PreparedStatement pstmt = null;
		try {
			pstmt = conn.prepareStatement("UPDATE weizhu_absence SET `type` = ?, start_time = ?, pre_end_time = ?, fac_end_time = ?, days = ?, `desc` = ? WHERE company_id = ? AND absence_id = ?; ");
			DBUtil.set(pstmt, 1, type);
			DBUtil.set(pstmt, 2, startTime);
			DBUtil.set(pstmt, 3, preEndTime);
			DBUtil.set(pstmt, 4, facEndTime);
			DBUtil.set(pstmt, 5, days);
			DBUtil.set(pstmt, 6, desc);
			DBUtil.set(pstmt, 7, companyId);
			DBUtil.set(pstmt, 8, absenceId);
			
			pstmt.executeUpdate();
		} finally {
			DBUtil.closeQuietly(pstmt);
		}
	}
	
	public static RequestHead getAbsenceRequestHead(Connection conn, long companyId, int absenceId) throws SQLException, InvalidProtocolBufferException {
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try {
			pstmt = conn.prepareStatement("SELECT head_data FROM weizhu_absence_head WHERE company_id = ? AND absence_id = ?; ");
			DBUtil.set(pstmt, 1, companyId);
			DBUtil.set(pstmt, 2, absenceId);
			
			rs = pstmt.executeQuery();
			
			if (!rs.next()) {
				return null;
			}
			
			return RequestHead.parseFrom(rs.getBytes("head_data"));
		} finally {
			DBUtil.closeQuietly(rs);
			DBUtil.closeQuietly(pstmt);
		}
	}
	
}
