package com.weizhu.service.official.upgrade;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import com.google.protobuf.ByteString;
import com.weizhu.common.db.DBUtil;
import com.weizhu.common.utils.HexUtil;
import com.weizhu.common.utils.JsonUtil;
import com.weizhu.proto.AdminOfficialProtos;
import com.weizhu.proto.AdminProtos;
import com.weizhu.proto.OfficialProtos;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

public class Upgrade0006 {

	public static void main(String[] args) throws Exception {
		String dbHost = "127.0.0.1";
		int dbPort = 3306;
		String dbUser = "root";
		String dbPassword = "";
		String dbName = "weizhu_test";
		long companyId = 0;
		
		for (String arg : args) {
			if (arg.startsWith("-h")) {
				dbHost = arg.substring(2);
			} else if (arg.startsWith("-p")) {
				dbPort = Integer.parseInt(arg.substring(2));
			} else if (arg.startsWith("-u")) {
				dbUser = arg.substring(2);
			} else if (arg.startsWith("-P")) {
				dbPassword = arg.substring(2);
			} else if (arg.startsWith("-n")){
				dbName = arg.substring(2);
			} else if (arg.startsWith("-com")) {
				companyId = Long.parseLong(arg.substring("-com".length()));
			}
		}
		
		HikariConfig config = new HikariConfig();
		config.setJdbcUrl("jdbc:mysql://" + dbHost + ":" + dbPort + "/" + dbName + "?useUnicode=true&characterEncoding=utf8&allowMultiQueries=true&autoReconnect=true&failOverReadOnly=false");
		config.setUsername(dbUser);
		if (dbPassword != null && !dbPassword.isEmpty()) {
			config.setPassword(dbPassword);
		}
		config.setMaximumPoolSize(3);
		config.addDataSourceProperty("cachePrepStmts", "true");
		config.addDataSourceProperty("prepStmtCacheSize", "250");
		config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
		config.addDataSourceProperty("useServerPrepStmts", "true");
		HikariDataSource hikariDataSource = new HikariDataSource(config);
		Connection dbConn = hikariDataSource.getConnection();
		try {
			doUpgrade(dbConn, companyId);
		} finally {
			DBUtil.closeQuietly(dbConn);
			hikariDataSource.close();
		}
	}
	
	private static void doUpgrade(Connection dbConn, long companyId) throws Exception {
		doUpgradeSecretaryOfficial(dbConn, companyId);
		doUpgradeOfficial(dbConn, companyId);
		doUpgradeSendPlan(dbConn, companyId);
		doUpgradeMessage(dbConn, companyId);
	}
	
	private static void doUpgradeSecretaryOfficial(Connection dbConn, long companyId) throws Exception {
		Statement stmt = null;
		ResultSet rs = null;
		
		OfficialProtos.Official secretaryOfficial;
		
		stmt = dbConn.createStatement();
		try {
			rs = stmt.executeQuery("SELECT * FROM weizhu_official WHERE official_id = 1; ");
			
			OfficialProtos.Official.Builder builder = OfficialProtos.Official.newBuilder()
				.setOfficialId(AdminOfficialProtos.ReservedOfficialId.WEIZHU_SECRETARY_VALUE)
				.setOfficialName("小秘书")
				.setAvatar("")
				.setState(OfficialProtos.State.NORMAL);
			
			if (rs.next()) {
				builder.setOfficialName(rs.getString("official_name"));
				builder.setAvatar(rs.getString("avatar"));
				
				String officialDesc = rs.getString("official_desc");
				if (officialDesc == null) {
					builder.clearOfficialDesc();
				} else {
					builder.setOfficialDesc(officialDesc);
				}
				
				String functionDesc = rs.getString("function_desc");
				if (functionDesc == null) {
					builder.clearFunctionDesc();
				} else {
					builder.setFunctionDesc(functionDesc);
				}
			}
			
			secretaryOfficial = builder.build();
			
		} finally {
			DBUtil.closeQuietly(rs);
			DBUtil.closeQuietly(stmt);
		}
		
		stmt = dbConn.createStatement();
		try {
			stmt.executeUpdate(
					"REPLACE INTO weizhu_profile_value (company_id, `name`, `value`) VALUES (" + companyId + 
					", 'official:weizhu_secretary_official', '" + 
					DBUtil.SQL_STRING_ESCAPER.escape(JsonUtil.PROTOBUF_JSON_FORMAT.printToString(secretaryOfficial)) + 
					"')");
		} finally {
			DBUtil.closeQuietly(stmt);
		}
	}
	
	private static void doUpgradeOfficial(Connection dbConn, long companyId) throws Exception {
		
		List<OfficialProtos.Official> officialList = new ArrayList<OfficialProtos.Official>();
		
		Statement stmt = null;
		ResultSet rs = null;
		try {
			stmt = dbConn.createStatement();
			rs = stmt.executeQuery("SELECT * FROM weizhu_official WHERE official_id != 1; ");
			
			OfficialProtos.Official.Builder tmpBuilder = OfficialProtos.Official.newBuilder();
			while (rs.next()) {
				tmpBuilder.clear();
				
				tmpBuilder.setOfficialId(rs.getLong("official_id"));
				tmpBuilder.setOfficialName(rs.getString("official_name"));
				tmpBuilder.setAvatar(rs.getString("avatar"));
				
				String officialDesc = rs.getString("official_desc");
				if (officialDesc != null) {
					tmpBuilder.setOfficialDesc(officialDesc);
				}
				
				String functionDesc = rs.getString("function_desc");
				if (functionDesc != null) {
					tmpBuilder.setFunctionDesc(functionDesc);
				}
				
				boolean isEnable = rs.getBoolean("is_enable");
				if (rs.wasNull()) {
					isEnable = true;
				}
				
				tmpBuilder.setState(isEnable ? OfficialProtos.State.NORMAL : OfficialProtos.State.DISABLE);
				
				long createAdminId = rs.getLong("create_admin_id");
				if (!rs.wasNull()) {
					tmpBuilder.setCreateAdminId(createAdminId);
				}
				
				int createTime = rs.getInt("create_time");
				if (!rs.wasNull()) {
					tmpBuilder.setCreateTime(createTime);
				}
				
				int allowModelId = rs.getInt("allow_model_id");
				if (!rs.wasNull()) {
					tmpBuilder.setAllowModelId(allowModelId);
				}
				
				officialList.add(tmpBuilder.build());
			}
		} finally {
			DBUtil.closeQuietly(rs);
			DBUtil.closeQuietly(stmt);
		}
		
		PreparedStatement pstmt = null;
		try {
			pstmt = dbConn.prepareStatement("INSERT INTO tmp_weizhu_official (company_id, official_id, official_name, avatar, official_desc, function_desc, allow_model_id, state, create_admin_id, create_time) "
					+ "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?); ");
			
			for (OfficialProtos.Official official : officialList) {
				DBUtil.set(pstmt, 1, companyId);
				DBUtil.set(pstmt, 2, official.getOfficialId());
				DBUtil.set(pstmt, 3, official.getOfficialName());
				DBUtil.set(pstmt, 4, official.getAvatar());
				DBUtil.set(pstmt, 5, official.hasOfficialDesc(), official.getOfficialDesc());
				DBUtil.set(pstmt, 6, official.hasFunctionDesc(), official.getFunctionDesc());
				DBUtil.set(pstmt, 7, official.hasAllowModelId(), official.getAllowModelId());
				DBUtil.set(pstmt, 8, official.getState());
				DBUtil.set(pstmt, 9, official.hasCreateAdminId(), official.getCreateAdminId());
				DBUtil.set(pstmt, 10, official.hasCreateTime(), official.getCreateTime());
				
				pstmt.addBatch();
			}
			pstmt.executeBatch();
		} finally {
			DBUtil.closeQuietly(pstmt);
		}
	}
	
	private static void doUpgradeSendPlan(Connection dbConn, long companyId) throws Exception {
		
		List<AdminOfficialProtos.OfficialSendPlan> sendPlanList = new ArrayList<AdminOfficialProtos.OfficialSendPlan>();
		Map<Integer, AdminProtos.AdminHead> adminHeadMap = new TreeMap<Integer, AdminProtos.AdminHead>();
		
		Statement stmt = null;
		ResultSet rs = null;
		try {
			stmt = dbConn.createStatement();
			rs = stmt.executeQuery("SELECT * FROM weizhu_official_send_plan_msg; ");

			Map<Integer, OfficialProtos.OfficialMessage> msgMap = new TreeMap<Integer, OfficialProtos.OfficialMessage>();
			OfficialProtos.OfficialMessage.Builder tmpMsgBuilder = OfficialProtos.OfficialMessage.newBuilder();
			while (rs.next()) {
				tmpMsgBuilder.clear();
				
				int planId = rs.getInt("plan_id");
				
				tmpMsgBuilder.setMsgSeq(0);
				tmpMsgBuilder.setMsgTime(0);
				tmpMsgBuilder.setIsFromUser(false);
				
				String textContent = rs.getString("text_content");
				if (textContent != null) {
					tmpMsgBuilder.setText(OfficialProtos.OfficialMessage.Text.newBuilder().setContent(textContent).build());
				}
				
				byte[] voiceData = rs.getBytes("voice_data");
				int voiceDuration = rs.getInt("voice_duration");
				if (voiceData != null && !rs.wasNull()) {
					tmpMsgBuilder.setVoice(OfficialProtos.OfficialMessage.Voice.newBuilder().setData(ByteString.copyFrom(voiceData)).setDuration(voiceDuration).build());
				}
				
				String imageName = rs.getString("image_name");
				if (imageName != null) {
					tmpMsgBuilder.setImage(OfficialProtos.OfficialMessage.Image.newBuilder().setName(imageName).build());
				}
				
				long userId = rs.getLong("user_user_id");
				if (!rs.wasNull()) {
					tmpMsgBuilder.setUser(OfficialProtos.OfficialMessage.User.newBuilder().setUserId(userId).build());
				}
				
				long discoverItemId = rs.getLong("discover_item_item_id");
				if (!rs.wasNull()) {
					tmpMsgBuilder.setDiscoverItem(OfficialProtos.OfficialMessage.DiscoverItem.newBuilder().setItemId(discoverItemId).build());
				}
				
				msgMap.put(planId, tmpMsgBuilder.build());
			}
			
			DBUtil.closeQuietly(rs);
			DBUtil.closeQuietly(stmt);
			rs = null;
			stmt = null;
			
			stmt = dbConn.createStatement();
			rs = stmt.executeQuery("SELECT * FROM weizhu_official_send_plan_admin_head; ");
			
			while (rs.next()) {
				int planId = rs.getInt("plan_id");
				AdminProtos.AdminHead adminHead = AdminProtos.AdminHead.parseFrom(rs.getBytes("admin_head_data"));
				adminHeadMap.put(planId, adminHead);
			}
			
			DBUtil.closeQuietly(rs);
			DBUtil.closeQuietly(stmt);
			rs = null;
			stmt = null;
			
			stmt = dbConn.createStatement();
			rs = stmt.executeQuery("SELECT * FROM weizhu_official_send_plan; ");
			
			AdminOfficialProtos.OfficialSendPlan.Builder tmpPlanBuilder = AdminOfficialProtos.OfficialSendPlan.newBuilder();
			while (rs.next()) {
				tmpPlanBuilder.clear();
				
				int planId = rs.getInt("plan_id");
				OfficialProtos.OfficialMessage sendMsg = msgMap.get(planId);
				AdminProtos.AdminHead adminHead = adminHeadMap.get(planId);
				if (sendMsg != null && adminHead != null) {
					tmpPlanBuilder.setPlanId(planId);
					tmpPlanBuilder.setOfficialId(rs.getLong("official_id"));
					tmpPlanBuilder.setSendTime(rs.getInt("send_time"));
					tmpPlanBuilder.setSendState(AdminOfficialProtos.OfficialSendPlan.SendState.valueOf(rs.getString("send_state")));
					tmpPlanBuilder.setSendMsgRefId(0L);
					tmpPlanBuilder.setSendMsg(sendMsg);
					
					int allowlModelId = rs.getInt("allow_model_id");
					if (!rs.wasNull()) {
						tmpPlanBuilder.setAllowModelId(allowlModelId);
					}
					
					long createAdminId = rs.getLong("create_admin_id");
					if (!rs.wasNull()) {
						tmpPlanBuilder.setCreateAdminId(createAdminId);
					}
					
					int createTime = rs.getInt("create_time");
					if (!rs.wasNull()) {
						tmpPlanBuilder.setCreateTime(createTime);
					}
					
					sendPlanList.add(tmpPlanBuilder.build());
				}
			}
		} finally {
			DBUtil.closeQuietly(rs);
			DBUtil.closeQuietly(stmt);
		}
		
		// insert admin head
		PreparedStatement pstmt = null;
		try {
			pstmt = dbConn.prepareStatement("INSERT INTO tmp_weizhu_official_send_plan_head (company_id, plan_id, head_type, head_data) VALUES (?, ?, ?, ?); ");
			
			for (Entry<Integer, AdminProtos.AdminHead> entry : adminHeadMap.entrySet()) {
				
				DBUtil.set(pstmt, 1, companyId);
				DBUtil.set(pstmt, 2, entry.getKey());
				DBUtil.set(pstmt, 3, "AdminHead");
				DBUtil.set(pstmt, 4, entry.getValue().toByteString());
				
				pstmt.addBatch();
			}
			pstmt.executeBatch();
		} finally {
			DBUtil.closeQuietly(pstmt);
		}
		
		// insert msg ref
		Map<Integer, Long> planToMsgRefIdMap = new TreeMap<Integer, Long>();
		pstmt = null;
		rs = null;
		try {
			pstmt = dbConn.prepareStatement("INSERT INTO tmp_weizhu_official_msg_ref (msg_ref_id, msg_type, msg_data) VALUES (NULL, ?, ?); ", Statement.RETURN_GENERATED_KEYS);
			
			for (AdminOfficialProtos.OfficialSendPlan sendPlan : sendPlanList) {
				
				DBUtil.set(pstmt, 1, sendPlan.getSendMsg().getMsgTypeCase());
				DBUtil.set(pstmt, 2, sendPlan.getSendMsg().toByteString());
				
				pstmt.addBatch();
			}
			pstmt.executeBatch();
			
			rs = pstmt.getGeneratedKeys();
			Iterator<AdminOfficialProtos.OfficialSendPlan> it = sendPlanList.iterator();
			while (rs.next() && it.hasNext()) {
				
				long msgRefId = rs.getLong(1);
				AdminOfficialProtos.OfficialSendPlan sendPlan = it.next();
				
				planToMsgRefIdMap.put(sendPlan.getPlanId(), msgRefId);
			}
		} finally {
			DBUtil.closeQuietly(pstmt);
		}
		
		// insert send plan
		pstmt = null;
		try {
			pstmt = dbConn.prepareStatement("INSERT INTO tmp_weizhu_official_send_plan (company_id, plan_id, official_id, send_time, send_state, send_msg_ref_id, allow_model_id, create_admin_id, create_time) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?); ");
			
			for (AdminOfficialProtos.OfficialSendPlan sendPlan : sendPlanList) {
				
				long msgRefId = planToMsgRefIdMap.get(sendPlan.getPlanId());
				
				DBUtil.set(pstmt, 1, companyId);
				DBUtil.set(pstmt, 2, sendPlan.getPlanId());
				DBUtil.set(pstmt, 3, sendPlan.getOfficialId());
				DBUtil.set(pstmt, 4, sendPlan.getSendTime());
				DBUtil.set(pstmt, 5, sendPlan.getSendState());
				DBUtil.set(pstmt, 6, msgRefId);
				DBUtil.set(pstmt, 7, sendPlan.hasAllowModelId(), sendPlan.getAllowModelId());
				DBUtil.set(pstmt, 8, sendPlan.hasCreateAdminId(), sendPlan.getCreateAdminId());
				DBUtil.set(pstmt, 9, sendPlan.hasCreateTime(), sendPlan.getCreateTime());
				
				pstmt.addBatch();
			}
			pstmt.executeBatch();
		} finally {
			DBUtil.closeQuietly(pstmt);
		}
	}
	
	private static void doUpgradeMessage(Connection dbConn, long companyId) throws Exception {
		
		long lastUserId = 0;
		long lastOfficialId = 0;
		long lastMsgSeq = 0;
		
		final int size = 500;
		
		while (true) {
			
			List<AdminOfficialProtos.OfficialMessageInfo> msgInfoList = new ArrayList<AdminOfficialProtos.OfficialMessageInfo>();
			
			StringBuilder sqlBuilder = new StringBuilder();
			sqlBuilder.append("SELECT * FROM weizhu_official_msg WHERE user_id > ").append(lastUserId);
			sqlBuilder.append(" OR ( user_id = ").append(lastUserId);
			sqlBuilder.append(" AND ( official_id > ").append(lastOfficialId);
			sqlBuilder.append(" OR ( official_id = ").append(lastOfficialId);
			sqlBuilder.append(" AND  msg_seq > ").append(lastMsgSeq);
			sqlBuilder.append(" ))) ORDER BY user_id ASC, official_id ASC, msg_seq ASC LIMIT ").append(size).append("; ");
			
			final String sql = sqlBuilder.toString();
			
			Statement stmt = null;
			ResultSet rs = null;
			try {
				stmt = dbConn.createStatement();
				rs = stmt.executeQuery(sql);
				
				AdminOfficialProtos.OfficialMessageInfo.Builder msgInfoBuilder = AdminOfficialProtos.OfficialMessageInfo.newBuilder();
				OfficialProtos.OfficialMessage.Builder msgBuilder = OfficialProtos.OfficialMessage.newBuilder();
				while (rs.next()) {
					msgInfoBuilder.clear();
					msgBuilder.clear();
					
					msgInfoBuilder.setUserId(rs.getLong("user_id"));
					msgInfoBuilder.setOfficialId(rs.getLong("official_id"));
					
					msgBuilder.setMsgSeq(rs.getLong("msg_seq"));
					msgBuilder.setMsgTime(rs.getInt("msg_time"));
					msgBuilder.setIsFromUser(rs.getBoolean("is_from_user"));
					
					String textContent = rs.getString("text_content");
					if (textContent != null) {
						msgBuilder.setText(OfficialProtos.OfficialMessage.Text.newBuilder().setContent(textContent).build());
					}
					
					byte[] voiceData = rs.getBytes("voice_data");
					int voiceDuration = rs.getInt("voice_duration");
					if (voiceData != null && !rs.wasNull()) {
						msgBuilder.setVoice(OfficialProtos.OfficialMessage.Voice.newBuilder().setData(ByteString.copyFrom(voiceData)).setDuration(voiceDuration).build());
					}
					
					String imageName = rs.getString("image_name");
					if (imageName != null) {
						msgBuilder.setImage(OfficialProtos.OfficialMessage.Image.newBuilder().setName(imageName).build());
					}
					
					long userId = rs.getLong("user_user_id");
					if (!rs.wasNull()) {
						msgBuilder.setUser(OfficialProtos.OfficialMessage.User.newBuilder().setUserId(userId).build());
					}
					
					long discoverItemId = rs.getLong("discover_item_item_id");
					if (!rs.wasNull()) {
						msgBuilder.setDiscoverItem(OfficialProtos.OfficialMessage.DiscoverItem.newBuilder().setItemId(discoverItemId).build());
					}
					
					msgInfoList.add(msgInfoBuilder.setMsg(msgBuilder.build()).build());
				}
			} finally {
				DBUtil.closeQuietly(rs);
				DBUtil.closeQuietly(stmt);
			}
			
			if (msgInfoList.isEmpty()) {
				break;
			}
			
			lastUserId = msgInfoList.get(msgInfoList.size() - 1).getUserId();
			lastOfficialId = msgInfoList.get(msgInfoList.size() - 1).getOfficialId();
			lastMsgSeq = msgInfoList.get(msgInfoList.size() - 1).getMsg().getMsgSeq();
			
			Map<Long, Map<Long, OfficialProtos.OfficialMessage>> userOfficialMsgMap = new TreeMap<Long, Map<Long, OfficialProtos.OfficialMessage>>();
			
			sqlBuilder = new StringBuilder();
			sqlBuilder.append("INSERT INTO tmp_weizhu_official_msg (company_id, user_id, official_id, msg_seq, msg_time, is_from_user, msg_type, msg_data) VALUES ");
			boolean isFirst = true;
			for (AdminOfficialProtos.OfficialMessageInfo msgInfo : msgInfoList) {
				if (isFirst) {
					isFirst = false;
				} else {
					sqlBuilder.append(", ");
				}
				
				sqlBuilder.append("(");
				sqlBuilder.append(companyId).append(", ");
				sqlBuilder.append(msgInfo.getUserId()).append(", ");
				sqlBuilder.append(msgInfo.getOfficialId()).append(", ");
				sqlBuilder.append(msgInfo.getMsg().getMsgSeq()).append(", ");
				sqlBuilder.append(msgInfo.getMsg().getMsgTime()).append(", ");
				sqlBuilder.append(msgInfo.getMsg().getIsFromUser() ? 1 : 0).append(", '");
				sqlBuilder.append(DBUtil.SQL_STRING_ESCAPER.escape(msgInfo.getMsg().getMsgTypeCase().name())).append("', UNHEX('");
				sqlBuilder.append(HexUtil.bin2Hex(msgInfo.getMsg().toByteArray())).append("'))");
				
				Map<Long, OfficialProtos.OfficialMessage> officialMsgMap = userOfficialMsgMap.get(msgInfo.getUserId());
				if (officialMsgMap == null) {
					officialMsgMap = new TreeMap<Long, OfficialProtos.OfficialMessage>();
					userOfficialMsgMap.put(msgInfo.getUserId(), officialMsgMap);
				}
				
				OfficialProtos.OfficialMessage msg = officialMsgMap.get(msgInfo.getOfficialId());
				if (msg == null || msg.getMsgSeq() < msgInfo.getMsg().getMsgSeq()) {
					officialMsgMap.put(msgInfo.getOfficialId(), msgInfo.getMsg());
				}
			}
			sqlBuilder.append("; ");
			
			sqlBuilder.append("INSERT IGNORE INTO tmp_weizhu_official_chat (company_id, user_id, official_id, latest_msg_seq, latest_msg_time) VALUES ");
			isFirst = true;
			for (Entry<Long, Map<Long, OfficialProtos.OfficialMessage>> entry : userOfficialMsgMap.entrySet()) {
				final long userId = entry.getKey();
				for (long officialId : entry.getValue().keySet()) {
					if (isFirst) {
						isFirst = false;
					} else {
						sqlBuilder.append(", ");
					}
					
					sqlBuilder.append("(").append(companyId);
					sqlBuilder.append(", ").append(userId);
					sqlBuilder.append(", ").append(officialId);
					sqlBuilder.append(", 0, 0)");
				}
			}
			sqlBuilder.append("; ");
			
			stmt = null;
			try {
				stmt = dbConn.createStatement();
				stmt.execute(sqlBuilder.toString());
			} finally {
				DBUtil.closeQuietly(stmt);
			}
			
			PreparedStatement pstmt = null;
			try {
				pstmt = dbConn.prepareStatement("UPDATE tmp_weizhu_official_chat SET latest_msg_seq = ?, latest_msg_time = ? WHERE company_id = ? AND user_id = ? AND official_id = ? AND latest_msg_seq < ?; ");
				
				for (Entry<Long, Map<Long, OfficialProtos.OfficialMessage>> entry : userOfficialMsgMap.entrySet()) {
					final long userId = entry.getKey();
					for (Entry<Long, OfficialProtos.OfficialMessage> entry2 : entry.getValue().entrySet()) {
						final long officialId = entry2.getKey();
						final long msgSeq = entry2.getValue().getMsgSeq();
						final int msgTime = entry2.getValue().getMsgTime();
						
						DBUtil.set(pstmt, 1, msgSeq);
						DBUtil.set(pstmt, 2, msgTime);
						DBUtil.set(pstmt, 3, companyId);
						DBUtil.set(pstmt, 4, userId);
						DBUtil.set(pstmt, 5, officialId);
						DBUtil.set(pstmt, 6, msgSeq);
						
						pstmt.addBatch();
					}
				}
				pstmt.executeBatch();
			} finally {
				DBUtil.closeQuietly(pstmt);
			}
		}
	}
	
}
