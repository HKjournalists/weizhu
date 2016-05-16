package com.weizhu.service.official;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import javax.annotation.Nullable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.protobuf.InvalidProtocolBufferException;
import com.weizhu.common.db.DBUtil;
import com.weizhu.common.db.ProtobufMapper;
import com.weizhu.common.utils.DataPage;
import com.weizhu.proto.AdminOfficialProtos;
import com.weizhu.proto.AdminProtos;
import com.weizhu.proto.OfficialProtos;
import com.weizhu.proto.OfficialProtos.OfficialMessage;

public class OfficialDB {
	
	private static final Logger logger = LoggerFactory.getLogger(OfficialDB.class);
	
	public static List<Long> getOfficialIdList(Connection conn, long companyId, @Nullable Long lastOfficialId, int size, @Nullable Set<OfficialProtos.State> stateSet) throws SQLException {
		if (size <= 0 || (stateSet != null && stateSet.isEmpty())) {
			return Collections.emptyList();
		}
		
		StringBuilder sqlBuilder = new StringBuilder();
		sqlBuilder.append("SELECT official_id FROM weizhu_official WHERE company_id = ").append(companyId);
		if (lastOfficialId != null) {
			sqlBuilder.append(" AND official_id > ").append(lastOfficialId);
		}
		if (stateSet != null) {
			sqlBuilder.append(" AND state IN ('");
			boolean isFirst = true;
			for (OfficialProtos.State state : stateSet) {
				if (isFirst) {
					isFirst = false;
				} else {
					sqlBuilder.append("', '");
				}
				
				sqlBuilder.append(DBUtil.SQL_STRING_ESCAPER.escape(state.name()));
			}
			sqlBuilder.append("')");
		}
		
		sqlBuilder.append(" ORDER BY official_id ASC LIMIT ").append(size).append("; ");
		
		final String sql = sqlBuilder.toString();
		
		Statement stmt = null;
		ResultSet rs = null;
		try {
			stmt = conn.createStatement();
			rs = stmt.executeQuery(sql);
			
			List<Long> list = new ArrayList<Long>();
			while (rs.next()) {
				list.add(rs.getLong("official_id"));
			}
			
			return list;
		} finally {
			DBUtil.closeQuietly(rs);
			DBUtil.closeQuietly(stmt);
		}
	}
	
	public static DataPage<Long> getOfficialIdPage(Connection conn, long companyId, int start, int length, @Nullable Set<OfficialProtos.State> stateSet) throws SQLException {
		if (stateSet != null && stateSet.isEmpty()) {
			return new DataPage<Long>(Collections.<Long>emptyList(), 0, 0);
		}
		if (start < 0) {
			start = 0;
		}
		if (length < 0) {
			length = 0;
		}
		
		StringBuilder sqlBuilder = new StringBuilder();
		sqlBuilder.append("SELECT count(*) AS total_size FROM weizhu_official WHERE company_id = ").append(companyId);
		if (stateSet != null) {
			sqlBuilder.append(" AND state IN ('");
			boolean isFirst = true;
			for (OfficialProtos.State state : stateSet) {
				if (isFirst) {
					isFirst = false;
				} else {
					sqlBuilder.append("', '");
				}
				
				sqlBuilder.append(DBUtil.SQL_STRING_ESCAPER.escape(state.name()));
			}
			sqlBuilder.append("')");
		}
		sqlBuilder.append("; ");
		
		sqlBuilder.append("SELECT official_id FROM weizhu_official WHERE company_id = ").append(companyId);
		if (stateSet != null) {
			sqlBuilder.append(" AND state IN ('");
			boolean isFirst = true;
			for (OfficialProtos.State state : stateSet) {
				if (isFirst) {
					isFirst = false;
				} else {
					sqlBuilder.append("', '");
				}
				
				sqlBuilder.append(DBUtil.SQL_STRING_ESCAPER.escape(state.name()));
			}
			sqlBuilder.append("')");
		}
		sqlBuilder.append(" ORDER BY official_id DESC LIMIT ").append(start).append(", ").append(length).append("; ");
		
		final String sql = sqlBuilder.toString();
		
		Statement stmt = null;
		ResultSet rs = null;
		try {
			stmt = conn.createStatement();
			stmt.execute(sql);
			rs = stmt.getResultSet();
			
			if (!rs.next()) {
				throw new RuntimeException("cannot get total_size");
			}
			
			int totalSize = rs.getInt("total_size");
			
			DBUtil.closeQuietly(rs);
			rs = null;
			
			stmt.getMoreResults();
			rs = stmt.getResultSet();
			
			List<Long> list = new ArrayList<Long>();
			while (rs.next()) {
				list.add(rs.getLong("official_id"));
			}
			
			return new DataPage<Long>(list, totalSize, totalSize);
		} finally {
			DBUtil.closeQuietly(rs);
			DBUtil.closeQuietly(stmt);
		}
	}
	
	private static final ProtobufMapper<OfficialProtos.Official> OFFICIAL_MAPPER = 
			ProtobufMapper.createMapper(OfficialProtos.Official.getDefaultInstance(), 
					"official_id", 
					"official_name", 
					"avatar", 
					"official_desc", 
					"function_desc",
					"allow_model_id",
					"state",
					"create_admin_id",
					"create_time",
					"update_admin_id",
					"update_time"
					);
	
	public static Map<Long, OfficialProtos.Official> getOfficial(Connection conn, long companyId, Collection<Long> officialIds) throws SQLException {
		if (officialIds.isEmpty()) {
			return Collections.emptyMap();
		}
		
		StringBuilder sqlBuilder = new StringBuilder();
		sqlBuilder.append("SELECT * FROM weizhu_official WHERE company_id = ").append(companyId);
		sqlBuilder.append(" AND official_id IN (");
		DBUtil.COMMA_JOINER.appendTo(sqlBuilder, officialIds);
		sqlBuilder.append("); ");
		
		final String sql = sqlBuilder.toString();
		
		Statement stmt = null;
		ResultSet rs = null;
		try {
			stmt = conn.createStatement();
			rs = stmt.executeQuery(sql);
			
			Map<Long, OfficialProtos.Official> resultMap = new TreeMap<Long, OfficialProtos.Official>();
			
			OfficialProtos.Official.Builder tmpBuilder = OfficialProtos.Official.newBuilder();
			while (rs.next()) {
				tmpBuilder.clear();
				
				OfficialProtos.Official official = OFFICIAL_MAPPER.mapToItem(rs, tmpBuilder).build();

				resultMap.put(official.getOfficialId(), official);
			}
			
			return resultMap;
		} finally {
			DBUtil.closeQuietly(rs);
			DBUtil.closeQuietly(stmt);
		}
	}
	
	public static List<Long> insertOfficial(Connection conn, long companyId, List<OfficialProtos.Official> officialList) throws SQLException {
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try {
			pstmt = conn.prepareStatement("INSERT INTO weizhu_official (company_id, official_id, official_name, avatar, official_desc, function_desc, allow_model_id, state, create_admin_id, create_time, update_admin_id, update_time) VALUES (?, NULL, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?); ", Statement.RETURN_GENERATED_KEYS);
			
			for (OfficialProtos.Official official : officialList) {
				DBUtil.set(pstmt, 1, companyId);
				DBUtil.set(pstmt, 2, official.getOfficialName());
				DBUtil.set(pstmt, 3, official.getAvatar());
				DBUtil.set(pstmt, 4, official.hasOfficialDesc(), official.getOfficialDesc());
				DBUtil.set(pstmt, 5, official.hasFunctionDesc(), official.getFunctionDesc());
				DBUtil.set(pstmt, 6, official.hasAllowModelId(), official.getAllowModelId());
				DBUtil.set(pstmt, 7, official.hasState(), official.getState());
				DBUtil.set(pstmt, 8, official.hasCreateAdminId(), official.getCreateAdminId());
				DBUtil.set(pstmt, 9, official.hasCreateTime(), official.getCreateTime());
				DBUtil.set(pstmt, 10, official.hasUpdateAdminId(), official.getUpdateAdminId());
				DBUtil.set(pstmt, 11, official.hasUpdateTime(), official.getUpdateTime());
				
				pstmt.addBatch();
			}
			pstmt.executeBatch();
			
			rs = pstmt.getGeneratedKeys();
			
			List<Long> officialIdList = new ArrayList<Long>(officialList.size());
			while (rs.next()) {
				officialIdList.add(rs.getLong(1));
			}
			
			if (officialIdList.size() != officialList.size()) {
				throw new RuntimeException("insert fail");
			}
			
			return officialIdList;
		} finally {
			DBUtil.closeQuietly(rs);
			DBUtil.closeQuietly(pstmt);
		}
	}
	
	public static void updateOfficial(Connection conn, long companyId, OfficialProtos.Official official) throws SQLException {
		PreparedStatement pstmt = null;
		try {
			pstmt = conn.prepareStatement("UPDATE weizhu_official SET official_name = ?, avatar = ?, official_desc = ?, function_desc = ?, allow_model_id = ?, state = ?, update_admin_id = ?, update_time = ? WHERE company_id = ? AND official_id = ?; ");
			
			DBUtil.set(pstmt, 1, official.getOfficialName());
			DBUtil.set(pstmt, 2, official.getAvatar());
			DBUtil.set(pstmt, 3, official.hasOfficialDesc(), official.getOfficialDesc());
			DBUtil.set(pstmt, 4, official.hasOfficialName(), official.getOfficialName());
			DBUtil.set(pstmt, 5, official.hasAllowModelId(), official.getAllowModelId());
			DBUtil.set(pstmt, 6, official.getState());
			DBUtil.set(pstmt, 7, official.hasUpdateAdminId(), official.getUpdateAdminId());
			DBUtil.set(pstmt, 8, official.hasUpdateTime(), official.getUpdateTime());
			
			DBUtil.set(pstmt, 9, companyId);
			DBUtil.set(pstmt, 10, official.getOfficialId());
			
			pstmt.executeUpdate();
		} finally {
			DBUtil.closeQuietly(pstmt);
		}
	}
	
	public static void deleteOfficial(Connection conn, long companyId, Collection<Long> officialIds) throws SQLException {
		if (officialIds.isEmpty()) {
			return ;
		}
		
		StringBuilder sqlBuilder = new StringBuilder();
		sqlBuilder.append("UPDATE weizhu_official SET state = '").append(DBUtil.SQL_STRING_ESCAPER.escape(OfficialProtos.State.DELETE.name()));
		sqlBuilder.append("' WHERE company_id = ").append(companyId);
		sqlBuilder.append(" AND official_id IN (");
		DBUtil.COMMA_JOINER.appendTo(sqlBuilder, officialIds);
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
	
	public static void setOfficialState(Connection conn, long companyId, Collection<Long> officialIds, OfficialProtos.State state) throws SQLException {
		if (officialIds.isEmpty()) {
			return ;
		}
		
		StringBuilder sqlBuilder = new StringBuilder();
		sqlBuilder.append("UPDATE weizhu_official SET state = '").append(DBUtil.SQL_STRING_ESCAPER.escape(state.name()));
		sqlBuilder.append("' WHERE company_id = ").append(companyId);
		sqlBuilder.append(" AND official_id IN (");
		DBUtil.COMMA_JOINER.appendTo(sqlBuilder, officialIds);
		sqlBuilder.append(") AND state != '").append(DBUtil.SQL_STRING_ESCAPER.escape(OfficialProtos.State.DELETE.name()));
		sqlBuilder.append("'; ");
		
		final String sql = sqlBuilder.toString();
		
		Statement stmt = null;
		try {
			stmt = conn.createStatement();
			stmt.executeUpdate(sql);
		} finally {
			DBUtil.closeQuietly(stmt);
		}
	}
	
	public static DataPage<Integer> getOfficialSendPlanIdPage(Connection conn, long companyId, int start, int length) throws SQLException {
		if (start < 0) {
			start = 0;
		}
		if (length < 0) {
			length = 0;
		}
		
		StringBuilder sqlBuilder = new StringBuilder();
		sqlBuilder.append("SELECT count(*) as total_size FROM weizhu_official_send_plan WHERE company_id = ").append(companyId).append("; ");
		sqlBuilder.append("SELECT plan_id FROM weizhu_official_send_plan WHERE company_id = ").append(companyId);
		sqlBuilder.append(" ORDER BY plan_id DESC LIMIT ").append(start).append(", ").append(length).append("; ");
		
		final String sql = sqlBuilder.toString();
		
		Statement stmt = null;
		ResultSet rs = null;
		try {
			stmt = conn.createStatement();
			stmt.execute(sql);
			rs = stmt.getResultSet();
			
			if (!rs.next()) {
				throw new RuntimeException("cannot get total_size");
			}
			
			int totalSize = rs.getInt("total_size");
			
			DBUtil.closeQuietly(rs);
			rs = null;
			
			stmt.getMoreResults();
			rs = stmt.getResultSet();
			
			List<Integer> list = new ArrayList<Integer>();
			while (rs.next()) {
				list.add(rs.getInt("plan_id"));
			}
			
			return new DataPage<Integer>(list, totalSize, totalSize);
		} finally {
			DBUtil.closeQuietly(rs);
			DBUtil.closeQuietly(stmt);
		}
	}
	
	public static DataPage<Integer> getOfficialSendPlanIdPage(Connection conn, long companyId, int start, int length, long officialId) throws SQLException {
		if (start < 0) {
			start = 0;
		}
		if (length < 0) {
			length = 0;
		}
		
		StringBuilder sqlBuilder = new StringBuilder();
		sqlBuilder.append("SELECT count(*) as total_size FROM weizhu_official_send_plan WHERE company_id = ").append(companyId).append("; ");
		sqlBuilder.append("SELECT count(*) as filtered_size FROM weizhu_official_send_plan WHERE company_id = ").append(companyId).append(" AND official_id = ").append(officialId).append("; ");
		sqlBuilder.append("SELECT plan_id FROM weizhu_official_send_plan WHERE company_id = ").append(companyId);
		sqlBuilder.append(" AND official_id = ").append(officialId);
		sqlBuilder.append(" ORDER BY plan_id DESC LIMIT ").append(start).append(", ").append(length).append("; ");
		
		final String sql = sqlBuilder.toString();
		
		Statement stmt = null;
		ResultSet rs = null;
		try {
			stmt = conn.createStatement();
			stmt.execute(sql);
			rs = stmt.getResultSet();
			
			if (!rs.next()) {
				throw new RuntimeException("cannot get total_size");
			}
			
			int totalSize = rs.getInt("total_size");
			
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
			
			List<Integer> list = new ArrayList<Integer>();
			while (rs.next()) {
				list.add(rs.getInt("plan_id"));
			}
			
			return new DataPage<Integer>(list, totalSize, filteredSize);
		} finally {
			DBUtil.closeQuietly(rs);
			DBUtil.closeQuietly(stmt);
		}
	}
	
	public static Map<Long, List<Integer>> getOfficialSendPlanIdListBySendState(Connection conn, AdminOfficialProtos.OfficialSendPlan.SendState sendState) throws SQLException {
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try {
			pstmt = conn.prepareStatement("SELECT company_id, plan_id FROM weizhu_official_send_plan WHERE send_state = ? ORDER BY plan_id; ");
			pstmt.setString(1, sendState.name());
			
			rs = pstmt.executeQuery();
			
			Map<Long, List<Integer>> resultMap = new TreeMap<Long, List<Integer>>();
			while (rs.next()) {
				long companyId = rs.getLong("company_id");
				int planId = rs.getInt("plan_id");
				
				List<Integer> list = resultMap.get(companyId);
				if (list == null) {
					list = new ArrayList<Integer>();
					resultMap.put(companyId, list);
				}
				list.add(planId);
			}
			
			return resultMap;
		} finally {
			DBUtil.closeQuietly(rs);
			DBUtil.closeQuietly(pstmt);
		}
	}
	
	private static final ProtobufMapper<AdminOfficialProtos.OfficialSendPlan> OFFICIAL_SEND_PLAN_MAPPER = 
			ProtobufMapper.createMapper(AdminOfficialProtos.OfficialSendPlan.getDefaultInstance(), 
					"plan_id",
					"official_id",
					"send_time",
					"send_state",
					"send_msg_ref_id",
					"allow_model_id",
					"create_admin_id",
					"create_time",
					"update_admin_id",
					"update_time"
					);
	
	public static Map<Integer, AdminOfficialProtos.OfficialSendPlan> getOfficialSendPlan(Connection conn, long companyId, Collection<Integer> planIds) throws SQLException {
		if (planIds.isEmpty()) {
			return Collections.emptyMap();
		}
		
		StringBuilder sqlBuilder = new StringBuilder();
		sqlBuilder.append("SELECT * FROM weizhu_official_send_plan WHERE company_id = ").append(companyId);
		sqlBuilder.append(" AND plan_id IN (");
		DBUtil.COMMA_JOINER.appendTo(sqlBuilder, planIds);
		sqlBuilder.append("); ");
		
		final String sql = sqlBuilder.toString();
		
		Map<Integer, AdminOfficialProtos.OfficialSendPlan.Builder> sendPlanBuilderMap = new TreeMap<Integer, AdminOfficialProtos.OfficialSendPlan.Builder>();
		List<Long> msgRefIdList = new ArrayList<Long>();
		
		Statement stmt = null;
		ResultSet rs = null;
		try {
			stmt = conn.createStatement();
			rs = stmt.executeQuery(sql);
			
			while (rs.next()) {
				AdminOfficialProtos.OfficialSendPlan.Builder builder = AdminOfficialProtos.OfficialSendPlan.newBuilder();
				OFFICIAL_SEND_PLAN_MAPPER.mapToItem(rs, builder);
				sendPlanBuilderMap.put(builder.getPlanId(), builder);
				msgRefIdList.add(builder.getSendMsgRefId());
			}
		} finally {
			DBUtil.closeQuietly(rs);
			DBUtil.closeQuietly(stmt);
		}
		
		if (msgRefIdList.isEmpty()) {
			return Collections.emptyMap();
		}
		
		Map<Long, OfficialProtos.OfficialMessage> msgRefMap = getOfficialMsgRef(conn, msgRefIdList);
		
		Map<Integer, AdminOfficialProtos.OfficialSendPlan> resultMap = new TreeMap<Integer, AdminOfficialProtos.OfficialSendPlan>();
		
		for (Entry<Integer, AdminOfficialProtos.OfficialSendPlan.Builder> entry : sendPlanBuilderMap.entrySet()) {
			OfficialProtos.OfficialMessage msg = msgRefMap.get(entry.getValue().getSendMsgRefId());
			if (msg != null) {
				resultMap.put(entry.getKey(), entry.getValue().setSendMsg(msg).build());
			}
		}
		
		return resultMap;
	}
	
	public static List<Integer> insertOfficialSendPlan(Connection conn, long companyId, List<AdminOfficialProtos.OfficialSendPlan> officialSendPlanList) throws SQLException {
		if (officialSendPlanList.isEmpty()) {
			return Collections.emptyList();
		}
		
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try {
			pstmt = conn.prepareStatement("INSERT INTO weizhu_official_send_plan (company_id, plan_id, official_id, send_time, send_state, send_msg_ref_id, allow_model_id, create_admin_id, create_time, update_admin_id, update_time) VALUES (?, NULL, ?, ?, ?, ?, ?, ?, ?, ?, ?); ", Statement.RETURN_GENERATED_KEYS);
			
			for (AdminOfficialProtos.OfficialSendPlan sendPlan : officialSendPlanList) {
				DBUtil.set(pstmt, 1, companyId);
				DBUtil.set(pstmt, 2, sendPlan.getOfficialId());
				DBUtil.set(pstmt, 3, sendPlan.getSendTime());
				DBUtil.set(pstmt, 4, sendPlan.getSendState());
				DBUtil.set(pstmt, 5, sendPlan.getSendMsgRefId());
				DBUtil.set(pstmt, 6, sendPlan.hasAllowModelId(), sendPlan.getAllowModelId());
				DBUtil.set(pstmt, 7, sendPlan.hasCreateAdminId(), sendPlan.getCreateAdminId());
				DBUtil.set(pstmt, 8, sendPlan.hasCreateTime(), sendPlan.getCreateTime());
				DBUtil.set(pstmt, 9, sendPlan.hasUpdateAdminId(), sendPlan.getUpdateAdminId());
				DBUtil.set(pstmt, 10, sendPlan.hasUpdateTime(), sendPlan.getUpdateTime());
				
				pstmt.addBatch();
			}
			pstmt.executeBatch();
			
			rs = pstmt.getGeneratedKeys();
			
			List<Integer> planIdList = new ArrayList<Integer>(officialSendPlanList.size());
			while (rs.next()) {
				planIdList.add(rs.getInt(1));
			}
			
			if (planIdList.size() != officialSendPlanList.size()) {
				throw new RuntimeException("insert fail");
			}
			
			return planIdList;
		} finally {
			DBUtil.closeQuietly(rs);
			DBUtil.closeQuietly(pstmt);
		}
	}
	
	public static void setOfficialSendPlanStateCancel(Connection conn, long companyId, 
			Collection<Integer> planIds, @Nullable Long updateAdminId, @Nullable Integer updateTime
			) throws SQLException {
		if (planIds.isEmpty()) {
			return;
		}
		
		StringBuilder sqlBuilder = new StringBuilder();
		sqlBuilder.append("UPDATE weizhu_official_send_plan SET send_state = '");
		sqlBuilder.append(DBUtil.SQL_STRING_ESCAPER.escape(AdminOfficialProtos.OfficialSendPlan.SendState.CANCEL_SEND.name()));
		sqlBuilder.append("', update_admin_id = ").append(updateAdminId == null ? "NULL" : updateAdminId);
		sqlBuilder.append(", update_time = ").append(updateTime == null ? "NULL" : updateTime);
		sqlBuilder.append(" WHERE company_id = ").append(companyId);
		sqlBuilder.append(" AND plan_id IN (");
		DBUtil.COMMA_JOINER.appendTo(sqlBuilder, planIds);
		sqlBuilder.append(") AND send_state = '");
		sqlBuilder.append(DBUtil.SQL_STRING_ESCAPER.escape(AdminOfficialProtos.OfficialSendPlan.SendState.WAIT_SEND.name()));
		sqlBuilder.append("'; ");
		
		final String sql = sqlBuilder.toString();
		
		Statement stmt = null;
		try {
			stmt = conn.createStatement();
			stmt.executeUpdate(sql);
		} finally {
			DBUtil.closeQuietly(stmt);
		}
	}
	
	public static boolean setOfficialSendPlanStateFinish(Connection conn, long companyId, int planId) throws SQLException {
		PreparedStatement pstmt = null;
		try {
			pstmt = conn.prepareStatement("UPDATE weizhu_official_send_plan SET send_state = ? WHERE company_id = ? AND plan_id = ? AND send_state = ?; ");
			DBUtil.set(pstmt, 1, AdminOfficialProtos.OfficialSendPlan.SendState.ALREADY_SEND);
			DBUtil.set(pstmt, 2, companyId);
			DBUtil.set(pstmt, 3, planId);
			DBUtil.set(pstmt, 4, AdminOfficialProtos.OfficialSendPlan.SendState.WAIT_SEND);
			
			return pstmt.executeUpdate() > 0;
		} finally {
			DBUtil.closeQuietly(pstmt);
		}
	}
	
	public static void insertOfficialSendPlanAdminHead(Connection conn, long companyId, int planId, AdminProtos.AdminHead adminHead) throws SQLException {
		PreparedStatement pstmt = null;
		try {
			pstmt = conn.prepareStatement("INSERT INTO weizhu_official_send_plan_head (company_id, plan_id, head_type, head_data) VALUES (?, ?, ?, ?); ");
			DBUtil.set(pstmt, 1, companyId);
			DBUtil.set(pstmt, 2, planId);
			DBUtil.set(pstmt, 3, "AdminHead");
			DBUtil.set(pstmt, 4, adminHead.toByteString());
			
			pstmt.executeUpdate();
		} finally {
			DBUtil.closeQuietly(pstmt);
		}
	}
	
	public static AdminProtos.AdminHead getOfficialSendPlanAdminHead(Connection conn, long companyId, int planId) throws SQLException, InvalidProtocolBufferException {
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try {
			pstmt = conn.prepareStatement("SELECT head_data FROM weizhu_official_send_plan_head WHERE company_id = ? AND plan_id = ? AND head_type = ?; ");
			DBUtil.set(pstmt, 1, companyId);
			DBUtil.set(pstmt, 2, planId);
			DBUtil.set(pstmt, 3, "AdminHead");
			
			rs = pstmt.executeQuery();
			
			if (!rs.next()) {
				return null;
			}
			
			return AdminProtos.AdminHead.parseFrom(rs.getBytes("head_data"));
		} finally {
			DBUtil.closeQuietly(rs);
			DBUtil.closeQuietly(pstmt);
		}
	}
	
	public static DataPage<AdminOfficialProtos.OfficialMessageInfo> getOfficialMessagePage(Connection conn, long companyId, long officialId, int start, int length) throws SQLException {
		if (start < 0) {
			start = 0;
		}
		if (length < 0) {
			length = 0;
		}
		
		final int totalSize;
		final ArrayList<AdminOfficialProtos.OfficialMessageInfo> list = new ArrayList<AdminOfficialProtos.OfficialMessageInfo>();
		final Map<String, Long> msgToRefIdMap = new TreeMap<String, Long>();
		
		StringBuilder sqlBuilder = new StringBuilder();
		sqlBuilder.append("SELECT count(*) AS total_size FROM weizhu_official_msg WHERE company_id = ").append(companyId);
		sqlBuilder.append(" AND official_id = ").append(officialId).append("; ");
		sqlBuilder.append("SELECT * FROM weizhu_official_msg WHERE company_id = ").append(companyId);
		sqlBuilder.append(" AND official_id = ").append(officialId);
		sqlBuilder.append(" ORDER BY msg_time DESC, user_id DESC, msg_seq DESC LIMIT ").append(start).append(", ").append(length).append("; ");
		
		final String sql = sqlBuilder.toString();
		
		Statement stmt = null;
		ResultSet rs = null;
		try {
			stmt = conn.createStatement();
			stmt.execute(sql);
			rs = stmt.getResultSet();
			
			if (!rs.next()) {
				throw new RuntimeException("cannot get total_size");
			}
			totalSize = rs.getInt("total_size");
			
			DBUtil.closeQuietly(rs);
			rs = null;
			
			stmt.getMoreResults();
			rs = stmt.getResultSet();
			
			AdminOfficialProtos.OfficialMessageInfo.Builder tmpInfoBuilder = AdminOfficialProtos.OfficialMessageInfo.newBuilder();
			OfficialProtos.OfficialMessage.Builder tmpMsgBuilder = OfficialProtos.OfficialMessage.newBuilder();
			while (rs.next()) {
				tmpInfoBuilder.clear();
				tmpMsgBuilder.clear();
				
				tmpInfoBuilder.setUserId(rs.getLong("user_id"));
				tmpInfoBuilder.setOfficialId(rs.getLong("official_id"));
				
				byte[] msgData = rs.getBytes("msg_data");
				if (msgData != null) {
					try {
						tmpMsgBuilder.mergeFrom(msgData);
					} catch (InvalidProtocolBufferException e) {
						logger.warn("official msg parse fail : " + companyId + "," + rs.getLong("user_id") + "," + rs.getLong("official_id") + "," + rs.getLong("msg_seq"), e);
						continue;
					}
				} else {
					long msgRefId = rs.getLong("msg_ref_id");
					if (rs.wasNull()) {
						continue;
					}
					long tmpUserId = rs.getLong("user_id");
					long tmpOfficialId = rs.getLong("official_id");
					long tmpMsgSeq = rs.getLong("msg_seq");
					msgToRefIdMap.put(tmpUserId + ":" + tmpOfficialId + ":" + tmpMsgSeq, msgRefId);
				}
				
				tmpMsgBuilder.setMsgSeq(rs.getLong("msg_seq"));
				tmpMsgBuilder.setMsgTime(rs.getInt("msg_time"));
				tmpMsgBuilder.setIsFromUser(rs.getBoolean("is_from_user"));
				
				tmpInfoBuilder.setMsg(tmpMsgBuilder.build());
				list.add(tmpInfoBuilder.build());
			}
		} finally {
			DBUtil.closeQuietly(rs);
			DBUtil.closeQuietly(stmt);
		}
		
		if (!msgToRefIdMap.isEmpty()) {
			Map<Long, OfficialProtos.OfficialMessage> refMsgMap = getOfficialMsgRef(conn, new TreeSet<Long>(msgToRefIdMap.values()));
			
			AdminOfficialProtos.OfficialMessageInfo.Builder tmpInfoBuilder = AdminOfficialProtos.OfficialMessageInfo.newBuilder();
			OfficialProtos.OfficialMessage.Builder tmpMsgBuilder = OfficialProtos.OfficialMessage.newBuilder();
			
			ListIterator<AdminOfficialProtos.OfficialMessageInfo> it = list.listIterator();
			while (it.hasNext()) {
				AdminOfficialProtos.OfficialMessageInfo msgInfo = it.next();
				if (msgInfo.getMsg().getMsgTypeCase() == OfficialMessage.MsgTypeCase.MSGTYPE_NOT_SET) {
					Long msgRefId = msgToRefIdMap.get(msgInfo.getUserId() + ":" + msgInfo.getOfficialId() + ":" + msgInfo.getMsg().getMsgSeq());
					OfficialProtos.OfficialMessage msg = msgRefId == null ? null : refMsgMap.get(msgRefId);
					if (msg == null) {
						it.remove();
					} else {
						tmpInfoBuilder.clear();
						tmpMsgBuilder.clear();
						
						tmpMsgBuilder.mergeFrom(msg);
						tmpMsgBuilder.setMsgSeq(msgInfo.getMsg().getMsgSeq());
						tmpMsgBuilder.setMsgTime(msgInfo.getMsg().getMsgTime());
						tmpMsgBuilder.setIsFromUser(msgInfo.getMsg().getIsFromUser());
						
						tmpInfoBuilder.setUserId(msgInfo.getUserId());
						tmpInfoBuilder.setOfficialId(msgInfo.getOfficialId());
						tmpInfoBuilder.setMsg(tmpMsgBuilder.build());
						
						it.set(tmpInfoBuilder.build());
					}
				}
			}
		}
		
		return new DataPage<AdminOfficialProtos.OfficialMessageInfo>(list, totalSize, totalSize);
	}
	
	public static DataPage<AdminOfficialProtos.OfficialMessageInfo> getOfficialMessagePage(Connection conn, 
			long companyId, long officialId, int start, int length,
			@Nullable Long userId, @Nullable Boolean isFromUser
			) throws SQLException {
		if (userId == null && isFromUser == null) {
			return getOfficialMessagePage(conn, companyId, officialId, start, length);
		}
		
		if (start < 0) {
			start = 0;
		}
		if (length < 0) {
			length = 0;
		}
		
		final int totalSize;
		final int filteredSize;
		final ArrayList<AdminOfficialProtos.OfficialMessageInfo> list = new ArrayList<AdminOfficialProtos.OfficialMessageInfo>();
		final Map<String, Long> msgToRefIdMap = new TreeMap<String, Long>();
		
		StringBuilder sqlBuilder = new StringBuilder();
		sqlBuilder.append("SELECT count(*) AS total_size FROM weizhu_official_msg WHERE company_id = ").append(companyId);
		sqlBuilder.append(" AND official_id = ").append(officialId).append("; ");
		sqlBuilder.append("SELECT count(*) AS filtered_size FROM weizhu_official_msg WHERE company_id = ").append(companyId);
		sqlBuilder.append(" AND official_id = ").append(officialId);
		if (userId != null) {
			sqlBuilder.append(" AND user_id = ").append(userId);
		}
		if (isFromUser != null) {
			sqlBuilder.append(" AND is_from_user = ").append(isFromUser ? 1 : 0);
		}
		sqlBuilder.append("; ");
		sqlBuilder.append("SELECT * FROM weizhu_official_msg WHERE company_id = ").append(companyId);
		sqlBuilder.append(" AND official_id = ").append(officialId);
		if (userId != null) {
			sqlBuilder.append(" AND user_id = ").append(userId);
		}
		if (isFromUser != null) {
			sqlBuilder.append(" AND is_from_user = ").append(isFromUser ? 1 : 0);
		}
		sqlBuilder.append(" ORDER BY msg_time DESC, user_id DESC, msg_seq DESC LIMIT ").append(start).append(", ").append(length).append("; ");
		
		final String sql = sqlBuilder.toString();
		
		Statement stmt = null;
		ResultSet rs = null;
		try {
			stmt = conn.createStatement();
			stmt.execute(sql);
			rs = stmt.getResultSet();
			
			if (!rs.next()) {
				throw new RuntimeException("cannot get total_size");
			}
			totalSize = rs.getInt("total_size");
			
			DBUtil.closeQuietly(rs);
			rs = null;
			
			stmt.getMoreResults();
			rs = stmt.getResultSet();
			
			if (!rs.next()) {
				throw new RuntimeException("cannot get total_size");
			}
			filteredSize = rs.getInt("filtered_size");
			
			DBUtil.closeQuietly(rs);
			rs = null;
			
			stmt.getMoreResults();
			rs = stmt.getResultSet();
			
			AdminOfficialProtos.OfficialMessageInfo.Builder tmpInfoBuilder = AdminOfficialProtos.OfficialMessageInfo.newBuilder();
			OfficialProtos.OfficialMessage.Builder tmpMsgBuilder = OfficialProtos.OfficialMessage.newBuilder();
			while (rs.next()) {
				tmpInfoBuilder.clear();
				tmpMsgBuilder.clear();
				
				tmpInfoBuilder.setUserId(rs.getLong("user_id"));
				tmpInfoBuilder.setOfficialId(rs.getLong("official_id"));
				
				byte[] msgData = rs.getBytes("msg_data");
				if (msgData != null) {
					try {
						tmpMsgBuilder.mergeFrom(msgData);
					} catch (InvalidProtocolBufferException e) {
						logger.warn("official msg parse fail : " + companyId + "," + rs.getLong("user_id") + "," + rs.getLong("official_id") + "," + rs.getLong("msg_seq"), e);
						continue;
					}
				} else {
					long msgRefId = rs.getLong("msg_ref_id");
					if (rs.wasNull()) {
						continue;
					}
					long tmpUserId = rs.getLong("user_id");
					long tmpOfficialId = rs.getLong("official_id");
					long tmpMsgSeq = rs.getLong("msg_seq");
					msgToRefIdMap.put(tmpUserId + ":" + tmpOfficialId + ":" + tmpMsgSeq, msgRefId);
				}
				
				tmpMsgBuilder.setMsgSeq(rs.getLong("msg_seq"));
				tmpMsgBuilder.setMsgTime(rs.getInt("msg_time"));
				tmpMsgBuilder.setIsFromUser(rs.getBoolean("is_from_user"));
				
				tmpInfoBuilder.setMsg(tmpMsgBuilder.build());
				list.add(tmpInfoBuilder.build());
			}
		} finally {
			DBUtil.closeQuietly(rs);
			DBUtil.closeQuietly(stmt);
		}
		
		if (!msgToRefIdMap.isEmpty()) {
			Map<Long, OfficialProtos.OfficialMessage> refMsgMap = getOfficialMsgRef(conn, new TreeSet<Long>(msgToRefIdMap.values()));
			
			AdminOfficialProtos.OfficialMessageInfo.Builder tmpInfoBuilder = AdminOfficialProtos.OfficialMessageInfo.newBuilder();
			OfficialProtos.OfficialMessage.Builder tmpMsgBuilder = OfficialProtos.OfficialMessage.newBuilder();
			
			ListIterator<AdminOfficialProtos.OfficialMessageInfo> it = list.listIterator();
			while (it.hasNext()) {
				AdminOfficialProtos.OfficialMessageInfo msgInfo = it.next();
				if (msgInfo.getMsg().getMsgTypeCase() == OfficialMessage.MsgTypeCase.MSGTYPE_NOT_SET) {
					Long msgRefId = msgToRefIdMap.get(msgInfo.getUserId() + ":" + msgInfo.getOfficialId() + ":" + msgInfo.getMsg().getMsgSeq());
					OfficialProtos.OfficialMessage msg = msgRefId == null ? null : refMsgMap.get(msgRefId);
					if (msg == null) {
						it.remove();
					} else {
						tmpInfoBuilder.clear();
						tmpMsgBuilder.clear();
						
						tmpMsgBuilder.mergeFrom(msg);
						tmpMsgBuilder.setMsgSeq(msgInfo.getMsg().getMsgSeq());
						tmpMsgBuilder.setMsgTime(msgInfo.getMsg().getMsgTime());
						tmpMsgBuilder.setIsFromUser(msgInfo.getMsg().getIsFromUser());
						
						tmpInfoBuilder.setUserId(msgInfo.getUserId());
						tmpInfoBuilder.setOfficialId(msgInfo.getOfficialId());
						tmpInfoBuilder.setMsg(tmpMsgBuilder.build());
						
						it.set(tmpInfoBuilder.build());
					}
				}
			}
		}
		
		return new DataPage<AdminOfficialProtos.OfficialMessageInfo>(list, totalSize, filteredSize);
	}
	
	public static List<OfficialProtos.OfficialMessage> getOfficialMessage(Connection conn, long companyId, 
			long userId, long officialId, @Nullable Long msgSeqBegin, @Nullable Long msgSeqEnd, int size
			) throws SQLException {
		if (size <= 0 || (msgSeqBegin != null && msgSeqEnd != null && msgSeqBegin - msgSeqEnd <= 1)) {
			return Collections.emptyList();
		}
		
		final ArrayList<OfficialProtos.OfficialMessage> list = new ArrayList<OfficialProtos.OfficialMessage>();
		final Map<Long, Long> msgToRefIdMap = new TreeMap<Long, Long>();
		
		StringBuilder sqlBuilder = new StringBuilder();
		sqlBuilder.append("SELECT * FROM weizhu_official_msg WHERE company_id = ").append(companyId);
		sqlBuilder.append(" AND user_id = ").append(userId).append(" AND official_id = ").append(officialId);
		if (msgSeqBegin != null) {
			sqlBuilder.append(" AND msg_seq < ").append(msgSeqBegin);
		}
		if (msgSeqEnd != null) {
			sqlBuilder.append(" AND msg_seq > ").append(msgSeqEnd);
		}
		sqlBuilder.append(" ORDER BY msg_seq DESC LIMIT ").append(size);
		sqlBuilder.append("; ");
		
		final String sql = sqlBuilder.toString();
		
		Statement stmt = null;
		ResultSet rs = null;
		try {
			stmt = conn.createStatement();
			rs = stmt.executeQuery(sql);
			
			OfficialProtos.OfficialMessage.Builder tmpMsgBuilder = OfficialProtos.OfficialMessage.newBuilder();
			while (rs.next()) {
				tmpMsgBuilder.clear();
				
				byte[] msgData = rs.getBytes("msg_data");
				if (msgData != null) {
					try {
						tmpMsgBuilder.mergeFrom(msgData);
					} catch (InvalidProtocolBufferException e) {
						logger.warn("official msg parse fail : " + companyId + "," + rs.getLong("user_id") + "," + rs.getLong("official_id") + "," + rs.getLong("msg_seq"), e);
						continue;
					}
				} else {
					long msgRefId = rs.getLong("msg_ref_id");
					if (rs.wasNull()) {
						continue;
					}
					msgToRefIdMap.put(rs.getLong("msg_seq"), msgRefId);
				}
				
				tmpMsgBuilder.setMsgSeq(rs.getLong("msg_seq"));
				tmpMsgBuilder.setMsgTime(rs.getInt("msg_time"));
				tmpMsgBuilder.setIsFromUser(rs.getBoolean("is_from_user"));
				
				list.add(tmpMsgBuilder.build());
			}
		} finally {
			DBUtil.closeQuietly(rs);
			DBUtil.closeQuietly(stmt);
		}
		
		if (!msgToRefIdMap.isEmpty()) {
			Map<Long, OfficialProtos.OfficialMessage> refMsgMap = getOfficialMsgRef(conn, new TreeSet<Long>(msgToRefIdMap.values()));
			
			OfficialProtos.OfficialMessage.Builder tmpMsgBuilder = OfficialProtos.OfficialMessage.newBuilder();
			
			ListIterator<OfficialProtos.OfficialMessage> it = list.listIterator();
			while (it.hasNext()) {
				OfficialProtos.OfficialMessage msg = it.next();
				if (msg.getMsgTypeCase() == OfficialMessage.MsgTypeCase.MSGTYPE_NOT_SET) {
					Long msgRefId = msgToRefIdMap.get(msg.getMsgSeq());
					OfficialProtos.OfficialMessage tmpMsg = msgRefId == null ? null : refMsgMap.get(msgRefId);
					if (tmpMsg == null) {
						it.remove();
					} else {
						tmpMsgBuilder.clear();
						
						tmpMsgBuilder.mergeFrom(tmpMsg);
						tmpMsgBuilder.setMsgSeq(msg.getMsgSeq());
						tmpMsgBuilder.setMsgTime(msg.getMsgTime());
						tmpMsgBuilder.setIsFromUser(msg.getIsFromUser());
						
						it.set(tmpMsgBuilder.build());
					}
				}
			}
		}
		
		return list;
	}
	 
	public static Map<Long, Long> getOfficialLatestMsgSeq(Connection conn, long companyId, long officialId, Collection<Long> userIds) throws SQLException {
		if (userIds.isEmpty()) {
			return Collections.emptyMap();
		}
		
		StringBuilder sqlBuilder = new StringBuilder();
		sqlBuilder.append("INSERT IGNORE INTO weizhu_official_chat (company_id, user_id, official_id, latest_msg_seq, latest_msg_time) VALUES ");
		
		boolean isFirst = true;
		for (Long userId : userIds) {
			if (isFirst) {
				isFirst = false;
			} else {
				sqlBuilder.append(", ");
			}
			
			sqlBuilder.append("(").append(companyId).append(", ").append(userId).append(", ").append(officialId).append(", 0, 0)");
		}
		sqlBuilder.append("; ");
		
		sqlBuilder.append("SELECT user_id, latest_msg_seq FROM weizhu_official_chat WHERE company_id = ").append(companyId);
		sqlBuilder.append(" AND official_id = ").append(officialId);
		sqlBuilder.append(" AND user_id IN (");
		DBUtil.COMMA_JOINER.appendTo(sqlBuilder, userIds);
		sqlBuilder.append("); ");
		
		final String sql = sqlBuilder.toString();
		
		Statement stmt = null;
		ResultSet rs = null;
		try {
			stmt = conn.createStatement();
			stmt.execute(sql);
			stmt.getMoreResults();
			rs = stmt.getResultSet();
			
			Map<Long, Long> resultMap = new TreeMap<Long, Long>();
			while (rs.next()) {
				resultMap.put(rs.getLong("user_id"), rs.getLong("latest_msg_seq"));
			}
			return resultMap;
		} finally {
			DBUtil.closeQuietly(rs);
			DBUtil.closeQuietly(stmt);
		}
	}
	
	public static void insertOfficialSingleMessage(Connection conn, long companyId, long officialId, long userId, OfficialProtos.OfficialMessage msg) throws SQLException {
		PreparedStatement pstmt = null;
		try {
			pstmt = conn.prepareStatement("INSERT INTO weizhu_official_msg (company_id, user_id, official_id, msg_seq, msg_time, is_from_user, msg_type, msg_data) VALUES (?, ?, ?, ?, ?, ?, ?, ?); UPDATE weizhu_official_chat SET latest_msg_seq = ?, latest_msg_time = ? WHERE company_id = ? AND user_id = ? AND official_id = ? AND latest_msg_seq < ?; ");
			
			DBUtil.set(pstmt, 1, companyId);
			DBUtil.set(pstmt, 2, userId);
			DBUtil.set(pstmt, 3, officialId);
			DBUtil.set(pstmt, 4, msg.getMsgSeq());
			DBUtil.set(pstmt, 5, msg.getMsgTime());
			DBUtil.set(pstmt, 6, msg.getIsFromUser());
			DBUtil.set(pstmt, 7, msg.getMsgTypeCase());
			DBUtil.set(pstmt, 8, msg.toByteString());
			
			DBUtil.set(pstmt, 9, msg.getMsgSeq());
			DBUtil.set(pstmt, 10, msg.getMsgTime());
			DBUtil.set(pstmt, 11, companyId);
			DBUtil.set(pstmt, 12, userId);
			DBUtil.set(pstmt, 13, officialId);
			DBUtil.set(pstmt, 14, msg.getMsgSeq());
				
			pstmt.executeUpdate();
		} finally {
			DBUtil.closeQuietly(pstmt);
		}
	}
	
	public static void insertOfficialMultiMessage(Connection conn, long companyId, long officialId, Map<Long, OfficialProtos.OfficialMessage> msgMap, long msgRefId) throws SQLException {
		if (msgMap.isEmpty()) {
			return;
		}
		
		StringBuilder sqlBuilder = new StringBuilder();
		sqlBuilder.append("INSERT INTO weizhu_official_msg (company_id, user_id, official_id, msg_seq, msg_time, is_from_user, msg_type, msg_ref_id) VALUES ");
		boolean isFirst = true;
		for (Entry<Long, OfficialProtos.OfficialMessage> entry : msgMap.entrySet()) {
			Long userId = entry.getKey();
			OfficialProtos.OfficialMessage msg = entry.getValue();
			
			if (isFirst) {
				isFirst = false;
			} else {
				sqlBuilder.append(", ");
			}
			
			sqlBuilder.append("(");
			sqlBuilder.append(companyId).append(", ");
			sqlBuilder.append(userId).append(", ");
			sqlBuilder.append(officialId).append(", ");
			sqlBuilder.append(msg.getMsgSeq()).append(", ");
			sqlBuilder.append(msg.getMsgTime()).append(", ");
			sqlBuilder.append(msg.getIsFromUser() ? 1 : 0).append(", '");
			sqlBuilder.append(DBUtil.SQL_STRING_ESCAPER.escape(msg.getMsgTypeCase().name())).append("', ");
			sqlBuilder.append(msgRefId).append(")");
		}
		
		final String sql = sqlBuilder.toString();
		Statement stmt = null;
		try {
			stmt = conn.createStatement();
			stmt.executeUpdate(sql);
		} finally {
			DBUtil.closeQuietly(stmt);
		}
		
		PreparedStatement pstmt = null;
		try {
			pstmt = conn.prepareStatement("UPDATE weizhu_official_chat SET latest_msg_seq = ?, latest_msg_time = ? WHERE company_id = ? AND user_id = ? AND official_id = ? AND latest_msg_seq < ?; ");
			
			for (Entry<Long, OfficialProtos.OfficialMessage> entry : msgMap.entrySet()) {
				Long userId = entry.getKey();
				OfficialProtos.OfficialMessage msg = entry.getValue();
				
				DBUtil.set(pstmt, 1, msg.getMsgSeq());
				DBUtil.set(pstmt, 2, msg.getMsgTime());
				DBUtil.set(pstmt, 3, companyId);
				DBUtil.set(pstmt, 4, userId);
				DBUtil.set(pstmt, 5, officialId);
				DBUtil.set(pstmt, 6, msg.getMsgSeq());
				
				pstmt.addBatch();
			}
			
			pstmt.executeBatch();
		} finally {
			DBUtil.closeQuietly(pstmt);
		}
	}
	
	public static Map<Long, OfficialProtos.OfficialMessage> getOfficialMsgRef(Connection conn, Collection<Long> msgRefIds) throws SQLException {
		if (msgRefIds.isEmpty()) {
			return Collections.emptyMap();
		}
		
		StringBuilder sqlBuilder = new StringBuilder();
		sqlBuilder.append("SELECT * FROM weizhu_official_msg_ref WHERE msg_ref_id IN (");
		DBUtil.COMMA_JOINER.appendTo(sqlBuilder, msgRefIds);
		sqlBuilder.append("); ");
		
		final String sql = sqlBuilder.toString();
		
		Statement stmt = null;
		ResultSet rs = null;
		try {
			stmt = conn.createStatement();
			rs = stmt.executeQuery(sql);
			
			Map<Long, OfficialProtos.OfficialMessage> resultMap = new TreeMap<Long, OfficialProtos.OfficialMessage>();
			while (rs.next()) {
				try {
					resultMap.put(rs.getLong("msg_ref_id"), OfficialProtos.OfficialMessage.parseFrom(rs.getBytes("msg_data")));
				} catch (InvalidProtocolBufferException e) {
					logger.warn("official msg ref parse fail : " + rs.getLong("msg_ref_id"), e);
				}
			}
			return resultMap;
		} finally {
			DBUtil.closeQuietly(rs);
			DBUtil.closeQuietly(stmt);
		}
	}
	
	public static List<Long> insertOfficialMsgRef(Connection conn, List<OfficialProtos.OfficialMessage> msgRefList) throws SQLException {
		if (msgRefList.isEmpty()) {
			return Collections.emptyList();
		}
		
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try {
			pstmt = conn.prepareStatement("INSERT INTO weizhu_official_msg_ref (msg_ref_id, msg_type, msg_data) VALUES (NULL, ?, ?); ", Statement.RETURN_GENERATED_KEYS);
			
			for (OfficialProtos.OfficialMessage msgRef : msgRefList) {
				DBUtil.set(pstmt, 1, msgRef.getMsgTypeCase());
				DBUtil.set(pstmt, 2, msgRef.toByteString());
				pstmt.addBatch();
			}
			pstmt.executeBatch();
			
			rs = pstmt.getGeneratedKeys();
			
			List<Long> msgRefIdList = new ArrayList<Long>(msgRefList.size());
			while (rs.next()) {
				msgRefIdList.add(rs.getLong(1));
			}
			
			if (msgRefIdList.size() != msgRefList.size()) {
				throw new RuntimeException("insert fail");
			}
			
			return msgRefIdList;
		} finally {
			DBUtil.closeQuietly(rs);
			DBUtil.closeQuietly(pstmt);
		}
	}
}
