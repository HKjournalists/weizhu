package com.weizhu.service.apns;

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

import com.weizhu.common.db.DBUtil;
import com.weizhu.common.db.ProtobufMapper;
import com.weizhu.proto.APNsProtos;
import com.weizhu.proto.WeizhuProtos;

public class APNsDB {
	
	private static final ProtobufMapper<APNsProtos.APNsCert> APNS_CERT_MAPPER = 
			ProtobufMapper.createMapper(APNsProtos.APNsCert.getDefaultInstance(), 
					"app_id",
					"is_production",
					"cert_p12",
					"cert_pass",
					"expired_time"
					);
	
	public static List<APNsProtos.APNsCert> getAllAPNsCertList(Connection conn) throws SQLException {
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try {
			pstmt = conn.prepareStatement("SELECT * FROM weizhu_apns_cert;");
			rs = pstmt.executeQuery();
			return APNS_CERT_MAPPER.mapToList(rs);
		} finally {
			DBUtil.closeQuietly(rs);
			DBUtil.closeQuietly(pstmt);
		}
	}
	
	public static void replaceDeviceToken(Connection conn, APNsDAOProtos.APNsDeviceToken deviceToken) throws SQLException {
		PreparedStatement pstmt = null;
		try {
			pstmt = conn.prepareStatement("REPLACE INTO weizhu_apns_device_token (company_id, user_id, session_id, app_id, is_production, device_token, badge_number) VALUES (?, ?, ?, ?, ?, ?, ?); ");
			
			pstmt.setLong(1, deviceToken.getSession().getCompanyId());
			pstmt.setLong(2, deviceToken.getSession().getUserId());
			pstmt.setLong(3, deviceToken.getSession().getSessionId());
			pstmt.setString(4, deviceToken.getAppId());
			pstmt.setBoolean(5, deviceToken.getIsProduction());
			pstmt.setString(6, deviceToken.getDeviceToken());
			pstmt.setInt(7, deviceToken.getBadgeNumber());
			
			pstmt.executeUpdate();
		} finally {
			DBUtil.closeQuietly(pstmt);
		}
	}
	
	public static void updateDeviceToken(Connection conn, WeizhuProtos.Session session, String appId, boolean isProduction, String deviceToken) throws SQLException {
		PreparedStatement pstmt = null;
		try {
			pstmt = conn.prepareStatement("UPDATE weizhu_apns_device_token SET app_id = ?, is_production = ?, device_token = ? WHERE company_id = ? AND user_id = ? AND session_id = ?; ");
			
			pstmt.setString(1, appId);
			pstmt.setBoolean(2, isProduction);
			pstmt.setString(3, deviceToken);
			pstmt.setLong(4, session.getCompanyId());
			pstmt.setLong(5, session.getUserId());
			pstmt.setLong(6, session.getSessionId());
			
			pstmt.executeUpdate();
		} finally {
			DBUtil.closeQuietly(pstmt);
		}
	}
	
	public static void updateBadgeNumber(Connection conn, Map<WeizhuProtos.Session, Integer> badgeNumberMap) throws SQLException {
		if (badgeNumberMap.isEmpty()) {
			return;
		}
		PreparedStatement pstmt = null;
		try {
			pstmt = conn.prepareStatement("UPDATE weizhu_apns_device_token SET badge_number = ? WHERE company_id = ? AND user_id = ? AND session_id = ?; ");
			
			for (Entry<WeizhuProtos.Session, Integer> entry : badgeNumberMap.entrySet()) {
				pstmt.setInt(1, entry.getValue());
				pstmt.setLong(2, entry.getKey().getCompanyId());
				pstmt.setLong(3, entry.getKey().getUserId());
				pstmt.setLong(4, entry.getKey().getSessionId());
				
				pstmt.addBatch();
			}
			
			pstmt.executeBatch();
		} finally {
			DBUtil.closeQuietly(pstmt);
		}
	}
	
	public static void increBadgeNumber(Connection conn, Collection<WeizhuProtos.Session> sessions) throws SQLException {
		if (sessions.isEmpty()) {
			return;
		}
		
		PreparedStatement pstmt = null;
		try {
			pstmt = conn.prepareStatement("UPDATE weizhu_apns_device_token SET badge_number = badge_number + 1 WHERE company_id = ? AND user_id = ? AND session_id = ?; ");
			
			for (WeizhuProtos.Session session : sessions) {
				pstmt.setLong(1, session.getCompanyId());
				pstmt.setLong(2, session.getUserId());
				pstmt.setLong(3, session.getSessionId());
				
				pstmt.addBatch();
			}
				
			pstmt.executeBatch();
		} finally {
			DBUtil.closeQuietly(pstmt);
		}
	}
	
	public static void deleteDeviceToken(Connection conn, long companyId, Collection<Long> userIds) throws SQLException {
		if (userIds.isEmpty()) {
			return;
		}
		
		PreparedStatement pstmt = null;
		try {
			pstmt = conn.prepareStatement("DELETE FROM weizhu_apns_device_token WHERE company_id = ? AND user_id = ?; ");
			
			for (Long userId : userIds) {
				pstmt.setLong(1, companyId);
				pstmt.setLong(2, userId);
				pstmt.addBatch();
			}
			
			pstmt.executeBatch();
		} finally {
			DBUtil.closeQuietly(pstmt);
		}
	}
	
	public static void deleteDeviceToken(Connection conn, Collection<WeizhuProtos.Session> sessions) throws SQLException {
		if (sessions.isEmpty()) {
			return;
		}
		
		PreparedStatement pstmt = null;
		try {
			pstmt = conn.prepareStatement("DELETE FROM weizhu_apns_device_token WHERE company_id = ? AND user_id = ? AND session_id = ?; ");
			
			for (WeizhuProtos.Session session : sessions) {
				pstmt.setLong(1, session.getCompanyId());
				pstmt.setLong(2, session.getUserId());
				pstmt.setLong(3, session.getSessionId());
				pstmt.addBatch();
			}
			
			pstmt.executeBatch();
		} finally {
			DBUtil.closeQuietly(pstmt);
		}
	}
	
	private static final ProtobufMapper<APNsDAOProtos.APNsDeviceToken> APNS_DEVICE_TOKEN_MAPPER = 
			ProtobufMapper.createMapper(APNsDAOProtos.APNsDeviceToken.getDefaultInstance(), 
					"session.company_id", 
					"session.user_id", 
					"session.session_id", 
					"app_id", 
					"is_production",
					"device_token",
					"badge_number"
					);
	
	public static Map<Long, List<APNsDAOProtos.APNsDeviceToken>> getDeviceToken(Connection conn, long companyId, Collection<Long> userIds) throws SQLException {
		if (userIds.isEmpty()) {
			return Collections.emptyMap();
		}
		
		StringBuilder sql = new StringBuilder();
		sql.append("SELECT company_id AS `session.company_id`, user_id AS `session.user_id`, session_id AS `session.session_id`, app_id, is_production, device_token, badge_number FROM weizhu_apns_device_token WHERE company_id = ").append(companyId).append(" AND user_id IN (");
		DBUtil.COMMA_JOINER.appendTo(sql, userIds);
		sql.append("); ");
		
		Statement stmt = null;
		ResultSet rs = null;
		try {
			stmt = conn.createStatement();
			rs = stmt.executeQuery(sql.toString());
			
			Map<Long, List<APNsDAOProtos.APNsDeviceToken>> resultMap = new HashMap<Long, List<APNsDAOProtos.APNsDeviceToken>>(userIds.size());
			
			APNsDAOProtos.APNsDeviceToken.Builder tmpBuilder = APNsDAOProtos.APNsDeviceToken.newBuilder();
			while (rs.next()) {
				tmpBuilder.clear();
				
				APNsDAOProtos.APNsDeviceToken deviceToken = APNS_DEVICE_TOKEN_MAPPER.mapToItem(rs, tmpBuilder).build();
				
				DBUtil.addMapArrayList(resultMap, deviceToken.getSession().getUserId(), deviceToken);
			}
			return resultMap;
		} finally {
			DBUtil.closeQuietly(rs);
			DBUtil.closeQuietly(stmt);
		}
	}
	
	public static List<APNsDAOProtos.APNsDeviceToken> getDeviceToken(Connection conn, String appId, boolean isProduction, String deviceToken) throws SQLException {
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try {
			pstmt = conn.prepareStatement("SELECT company_id AS `session.company_id`, user_id AS `session.user_id`, session_id AS `session.session_id`, app_id, is_production, device_token, badge_number FROM weizhu_apns_device_token WHERE app_id = ? AND is_production = ? AND device_token = ?; ");
			pstmt.setString(1, appId);
			pstmt.setBoolean(2, isProduction);
			pstmt.setString(3, deviceToken);
			
			rs = pstmt.executeQuery();
			return APNS_DEVICE_TOKEN_MAPPER.mapToList(rs);
		} finally {
			DBUtil.closeQuietly(rs);
			DBUtil.closeQuietly(pstmt);
		}
	}
	
}
