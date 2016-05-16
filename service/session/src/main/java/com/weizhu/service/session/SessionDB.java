package com.weizhu.service.session;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.weizhu.common.db.DBUtil;
import com.weizhu.common.db.ProtobufMapper;
import com.weizhu.proto.SessionProtos;
import com.weizhu.proto.WeizhuProtos;

public class SessionDB {

	private static final ProtobufMapper<SessionProtos.SessionData> SESSION_DATA_MAPPER = 
			ProtobufMapper.createMapper(SessionProtos.SessionData.getDefaultInstance(), 
					"session.company_id",
					"session.user_id",
					"session.session_id",
					"login_time",
					"active_time",
					"weizhu.platform",
					"weizhu.version_name",
					"weizhu.version_code",
					"weizhu.stage",
					"weizhu.build_time",
					"weizhu.build_hash",
					"android.device",
					"android.manufacturer",
					"android.brand",
					"android.model",
					"android.serial",
					"android.release",
					"android.sdk_int",
					"android.codename",
					"iphone.name",
					"iphone.system_name",
					"iphone.system_version",
					"iphone.model",
					"iphone.localized_model",
					"iphone.device_token",
					"iphone.mac",
					"iphone.app_id",
					"web_mobile.user_agent",
					"web_login.weblogin_id",
					"web_login.login_time",
					"web_login.active_time",
					"web_login.user_agent"
					);
	
	private static final String GET_SESSION_LIST_SQL_SEGMENT = 
			"SELECT "
			+ "S.company_id as `session.company_id`, "
			+ "S.user_id as `session.user_id`, "
			+ "S.session_id as `session.session_id`, "
			+ "S.login_time as `login_time`, "
			+ "S.active_time as `active_time`, "
			+ "W.platform as `weizhu.platform`, "
			+ "W.version_name as `weizhu.version_name`, "
			+ "W.version_code as `weizhu.version_code`, "
			+ "W.stage as `weizhu.stage`, "
			+ "W.build_time as `weizhu.build_time`, "
			+ "W.build_hash as `weizhu.build_hash`, "
			+ "A.device as `android.device`, "
			+ "A.manufacturer as `android.manufacturer`, "
			+ "A.brand as `android.brand`, "
			+ "A.model as `android.model`, "
			+ "A.serial as `android.serial`, "
			+ "A.release as `android.release`, "
			+ "A.sdk_int as `android.sdk_int`, "
			+ "A.codename as `android.codename`, "
			+ "I.name as `iphone.name`, "
			+ "I.system_name as `iphone.system_name`, "
			+ "I.system_version as `iphone.system_version`, "
			+ "I.model as `iphone.model`, "
			+ "I.localized_model as `iphone.localized_model`, "
			+ "I.device_token as `iphone.device_token`, "
			+ "I.mac as `iphone.mac`, "
			+ "I.app_id as `iphone.app_id`, "
			+ "WM.user_agent as `web_mobile.user_agent`, "
			+ "WL.weblogin_id as `web_login.weblogin_id`, "
			+ "WL.login_time as `web_login.login_time`, "
			+ "WL.active_time as `web_login.active_time`, "
			+ "WL.user_agent as `web_login.user_agent` "
			+ "FROM weizhu_session S "
			+ "LEFT OUTER JOIN weizhu_session_weizhu W ON S.company_id = W.company_id AND S.user_id = W.user_id AND S.session_id = W.session_id "
			+ "LEFT OUTER JOIN weizhu_session_android A ON S.company_id = A.company_id AND S.user_id = A.user_id AND S.session_id = A.session_id "
			+ "LEFT OUTER JOIN weizhu_session_iphone I ON S.company_id = I.company_id AND S.user_id = I.user_id AND S.session_id = I.session_id "
			+ "LEFT OUTER JOIN weizhu_session_web_mobile WM ON S.company_id = WM.company_id AND S.user_id = WM.user_id AND S.session_id = WM.session_id "
			+ "LEFT OUTER JOIN weizhu_session_web_login WL ON S.company_id = WL.company_id AND S.user_id = WL.user_id AND S.session_id = WL.session_id ";
	
	public static Map<Long, List<SessionProtos.SessionData>> getSessionData(Connection conn, long companyId, Collection<Long> userIds) throws SQLException {
		if (userIds.isEmpty()) {
			return Collections.emptyMap();
		}
		
		StringBuilder sql = new StringBuilder();
		sql.append(GET_SESSION_LIST_SQL_SEGMENT);
		sql.append("WHERE S.company_id = ").append(companyId).append(" AND S.user_id IN (");
		DBUtil.COMMA_JOINER.appendTo(sql, userIds);
		sql.append(") ORDER BY S.user_id ASC, S.active_time DESC; ");
		
		Statement stmt = null;
		ResultSet rs = null;
		try {
			stmt = conn.createStatement();
			rs = stmt.executeQuery(sql.toString());
			
			Map<Long, List<SessionProtos.SessionData>> resultMap = new HashMap<Long, List<SessionProtos.SessionData>>();
			
			SessionProtos.SessionData.Builder tmpBuilder = SessionProtos.SessionData.newBuilder();
			while (rs.next()) {
				tmpBuilder.clear();
				
				SessionProtos.SessionData sessionData = SESSION_DATA_MAPPER.mapToItem(rs, tmpBuilder).build();
				
				List<SessionProtos.SessionData> list = resultMap.get(sessionData.getSession().getUserId());
				if (list == null) {
					list = new ArrayList<SessionProtos.SessionData>();
					resultMap.put(sessionData.getSession().getUserId(), list);
				}
				list.add(sessionData);
			}
			
			return resultMap;
		} finally {
			DBUtil.closeQuietly(rs);
			DBUtil.closeQuietly(stmt);
		}
	}
	
	public static boolean insertSession(Connection conn, SessionProtos.SessionData sessionData) throws SQLException {
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try {
			pstmt = conn.prepareStatement("INSERT IGNORE INTO weizhu_session (company_id, user_id, session_id, login_time, active_time) VALUES (?, ?, ?, ?, ?); ");
			
			pstmt.setLong(1, sessionData.getSession().getCompanyId());
			pstmt.setLong(2, sessionData.getSession().getUserId());
			pstmt.setLong(3, sessionData.getSession().getSessionId());
			pstmt.setInt(4, sessionData.getLoginTime());
			pstmt.setInt(5, sessionData.getActiveTime());
			
			if (pstmt.executeUpdate() <= 0) {
				return false;
			}
		} finally {
			DBUtil.closeQuietly(rs);
			DBUtil.closeQuietly(pstmt);
		}
		
		if (sessionData.hasWeizhu()) {
			replaceSessionWeizhu(conn, sessionData.getSession(), sessionData.getWeizhu());
		}
		
		if (sessionData.hasAndroid()) {
			replaceSessionAndroid(conn, sessionData.getSession(), sessionData.getAndroid());
		}
		if (sessionData.hasIphone()) {
			replaceSessionIphone(conn, sessionData.getSession(), sessionData.getIphone());
		}
		if (sessionData.hasWebMobile()) {
			replaceSessionWebMobile(conn, sessionData.getSession(), sessionData.getWebMobile());
		}
		if (sessionData.hasWebLogin()) {
			replaceSessionWebLogin(conn, sessionData.getSession(), sessionData.getWebLogin());
		}
		
		return true;
	}
	
	public static boolean updateSession(Connection conn, SessionProtos.SessionData sessionData) throws SQLException {
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try {
			pstmt = conn.prepareStatement("UPDATE weizhu_session SET active_time = ? WHERE company_id = ? AND user_id = ? AND session_id = ?; ");
			
			pstmt.setInt(1, sessionData.getActiveTime());
			pstmt.setLong(2, sessionData.getSession().getCompanyId());
			pstmt.setLong(3, sessionData.getSession().getUserId());
			pstmt.setLong(4, sessionData.getSession().getSessionId());
			
			if (pstmt.executeUpdate() <= 0) {
				return false;
			}
		} finally {
			DBUtil.closeQuietly(rs);
			DBUtil.closeQuietly(pstmt);
		}
		
		if (sessionData.hasWeizhu()) {
			replaceSessionWeizhu(conn, sessionData.getSession(), sessionData.getWeizhu());
		}
		
		if (sessionData.hasAndroid()) {
			replaceSessionAndroid(conn, sessionData.getSession(), sessionData.getAndroid());
		}
		if (sessionData.hasIphone()) {
			replaceSessionIphone(conn, sessionData.getSession(), sessionData.getIphone());
		}
		if (sessionData.hasWebMobile()) {
			replaceSessionWebMobile(conn, sessionData.getSession(), sessionData.getWebMobile());
		}
		if (sessionData.hasWebLogin()) {
			replaceSessionWebLogin(conn, sessionData.getSession(), sessionData.getWebLogin());
		}
		
		return true;
	}
	
	private static boolean replaceSessionWeizhu(Connection conn, WeizhuProtos.Session session, WeizhuProtos.Weizhu weizhu) throws SQLException {
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try {
			pstmt = conn.prepareStatement("REPLACE INTO weizhu_session_weizhu (company_id, user_id, session_id, platform, version_name, version_code, stage, build_time, build_hash) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?); ");
			
			pstmt.setLong(1, session.getCompanyId());
			pstmt.setLong(2, session.getUserId());
			pstmt.setLong(3, session.getSessionId());
			
			pstmt.setString(4, weizhu.getPlatform().name());
			pstmt.setString(5, weizhu.getVersionName());
			pstmt.setInt(6, weizhu.getVersionCode());
			pstmt.setString(7, weizhu.getStage().name());
			pstmt.setInt(8, weizhu.getBuildTime());
			DBUtil.set(pstmt, 9, weizhu.hasBuildHash(), weizhu.getBuildHash());
			
			return pstmt.executeUpdate() > 0;
		} finally {
			DBUtil.closeQuietly(rs);
			DBUtil.closeQuietly(pstmt);
		}
	}
	
	private static boolean replaceSessionAndroid(Connection conn, WeizhuProtos.Session session, WeizhuProtos.Android android) throws SQLException {
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try {
			pstmt = conn.prepareStatement("REPLACE INTO weizhu_session_android (company_id, user_id, session_id, device, manufacturer, brand, model, serial, `release`, sdk_int, codename) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?); ");
			
			pstmt.setLong(1, session.getCompanyId());
			pstmt.setLong(2, session.getUserId());
			pstmt.setLong(3, session.getSessionId());
			
			pstmt.setString(4, android.getDevice());
			pstmt.setString(5, android.getManufacturer());
			pstmt.setString(6, android.getBrand());
			pstmt.setString(7, android.getModel());
			pstmt.setString(8, android.getSerial());
			pstmt.setString(9, android.getRelease());
			pstmt.setInt(10, android.getSdkInt());
			pstmt.setString(11, android.getCodename());
			
			return pstmt.executeUpdate() > 0;
		} finally {
			DBUtil.closeQuietly(rs);
			DBUtil.closeQuietly(pstmt);
		}
	}
	
	private static boolean replaceSessionIphone(Connection conn, WeizhuProtos.Session session, WeizhuProtos.Iphone iphone) throws SQLException {
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try {
			pstmt = conn.prepareStatement("REPLACE INTO weizhu_session_iphone (company_id, user_id, session_id, name, system_name, system_version, model, localized_model, device_token, mac, app_id) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?); ");
			
			pstmt.setLong(1, session.getCompanyId());
			pstmt.setLong(2, session.getUserId());
			pstmt.setLong(3, session.getSessionId());
			
			pstmt.setString(4, iphone.getName());
			pstmt.setString(5, iphone.getSystemName());
			pstmt.setString(6, iphone.getSystemVersion());
			pstmt.setString(7, iphone.getModel());
			pstmt.setString(8, iphone.getLocalizedModel());
			pstmt.setString(9, iphone.getDeviceToken());
			pstmt.setString(10, iphone.getMac());
			DBUtil.set(pstmt, 11, iphone.hasAppId(), iphone.getAppId());
			
			return pstmt.executeUpdate() > 0;
		} finally {
			DBUtil.closeQuietly(rs);
			DBUtil.closeQuietly(pstmt);
		}
	}
	
	private static boolean replaceSessionWebMobile(Connection conn, WeizhuProtos.Session session, WeizhuProtos.WebMobile webMobile) throws SQLException {
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try {
			pstmt = conn.prepareStatement("REPLACE INTO weizhu_session_web_mobile (company_id, user_id, session_id, user_agent) VALUES (?, ?, ?, ?); ");
			
			pstmt.setLong(1, session.getCompanyId());
			pstmt.setLong(2, session.getUserId());
			pstmt.setLong(3, session.getSessionId());
			pstmt.setString(4, webMobile.getUserAgent());
			
			return pstmt.executeUpdate() > 0;
		} finally {
			DBUtil.closeQuietly(rs);
			DBUtil.closeQuietly(pstmt);
		}
	}
	
	public static boolean replaceSessionWebLogin(Connection conn, WeizhuProtos.Session session, WeizhuProtos.WebLogin webLogin) throws SQLException {
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try {
			pstmt = conn.prepareStatement("REPLACE INTO weizhu_session_web_login (company_id, user_id, session_id, weblogin_id, login_time, active_time, user_agent) VALUES (?, ?, ?, ?, ?, ?, ?); ");
			
			pstmt.setLong(1, session.getCompanyId());
			pstmt.setLong(2, session.getUserId());
			pstmt.setLong(3, session.getSessionId());
			pstmt.setLong(4, webLogin.getWebloginId());
			pstmt.setInt(5, webLogin.getLoginTime());
			pstmt.setInt(6, webLogin.getActiveTime());
			pstmt.setString(7, webLogin.getUserAgent());
			
			return pstmt.executeUpdate() > 0;
		} finally {
			DBUtil.closeQuietly(rs);
			DBUtil.closeQuietly(pstmt);
		}
	}
	
	public static void deleteSessionWebLogin(Connection conn, WeizhuProtos.Session session, long webLoginId) throws SQLException {
		StringBuilder sqlBuilder = new StringBuilder();
		sqlBuilder.append("DELETE FROM weizhu_session_web_login WHERE company_id = ").append(session.getCompanyId());
		sqlBuilder.append(" AND user_id = ").append(session.getUserId());
		sqlBuilder.append(" AND session_id = ").append(session.getSessionId());
		sqlBuilder.append(" AND weblogin_id = ").append(webLoginId).append("; ");
		
		final String sql = sqlBuilder.toString();
		Statement stmt = null;
		try {
			stmt = conn.createStatement();
			stmt.execute(sql);
		} finally {
			DBUtil.closeQuietly(stmt);
		}
	}

	public static void deleteSession(Connection conn, long companyId, long userId, Collection<Long> sessionIds) throws SQLException {
		if (sessionIds.isEmpty()) {
			return;
		}
		
		String sessionIdStr = DBUtil.COMMA_JOINER.join(sessionIds);
		
		StringBuilder sql = new StringBuilder();
		sql.append("DELETE FROM weizhu_session WHERE company_id = ").append(companyId).append(" AND user_id = ").append(userId).append(" AND session_id IN (").append(sessionIdStr).append("); ");
		sql.append("DELETE FROM weizhu_session_weizhu WHERE company_id = ").append(companyId).append(" AND user_id = ").append(userId).append(" AND session_id IN (").append(sessionIdStr).append("); ");
		sql.append("DELETE FROM weizhu_session_android WHERE company_id = ").append(companyId).append(" AND user_id = ").append(userId).append(" AND session_id IN (").append(sessionIdStr).append("); ");
		sql.append("DELETE FROM weizhu_session_iphone WHERE company_id = ").append(companyId).append(" AND user_id = ").append(userId).append(" AND session_id IN (").append(sessionIdStr).append("); ");
		sql.append("DELETE FROM weizhu_session_web_mobile WHERE company_id = ").append(companyId).append(" AND user_id = ").append(userId).append(" AND session_id IN (").append(sessionIdStr).append("); ");
		sql.append("DELETE FROM weizhu_session_web_login WHERE company_id = ").append(companyId).append(" AND user_id = ").append(userId).append(" AND session_id IN (").append(sessionIdStr).append("); ");
		
		Statement stmt = null;
		try {
			stmt = conn.createStatement();
			stmt.execute(sql.toString());
		} finally {
			DBUtil.closeQuietly(stmt);
		}
	}
}
