package com.weizhu.service.boss;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import com.weizhu.common.db.DBUtil;
import com.weizhu.common.db.ProtobufMapper;
import com.weizhu.proto.BossProtos;

public class BossDB {
	
	public static String getBossPassword(Connection conn, String bossId) throws SQLException {
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try {
			pstmt = conn.prepareStatement("SELECT boss_password FROM weizhu_boss WHERE boss_id = ?; ");
			pstmt.setString(1, bossId);
			
			rs = pstmt.executeQuery();
			
			if (rs.next()) {
				return rs.getString("boss_password");
			} else {
				return null;
			}
		} finally {
			DBUtil.closeQuietly(rs);
			DBUtil.closeQuietly(pstmt);
		}
	}
	
	private static final ProtobufMapper<BossProtos.BossSessionData> BOSS_SESSION_DATA_MAPPER = 
			ProtobufMapper.createMapper(BossProtos.BossSessionData.getDefaultInstance(), 
					"session.boss_id", 
					"session.session_id",
					"login_time",
					"login_host",
					"user_agent",
					"active_time",
					"logout_time");
	
	private static final String GET_BOSS_LATEST_SESSION_SQL = 
			"SELECT "
			+ "boss_id AS `session.boss_id`, "
			+ "session_id AS `session.session_id`, "
			+ "login_time, "
			+ "login_host, "
			+ "user_agent, "
			+ "active_time, "
			+ "logout_time "
			+ "FROM weizhu_boss_session "
			+ "WHERE boss_id = ? "
			+ "ORDER BY login_time DESC, session_id DESC LIMIT 1; ";
	
	public static BossProtos.BossSessionData getBossLatestSession(Connection conn, String bossId) throws SQLException {
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try {
			pstmt = conn.prepareStatement(GET_BOSS_LATEST_SESSION_SQL);
			pstmt.setString(1, bossId);
			
			rs = pstmt.executeQuery();
			
			if (rs.next()) {
				return BOSS_SESSION_DATA_MAPPER.mapToItem(rs, BossProtos.BossSessionData.newBuilder()).build();
			} else {
				return null;
			}
		} finally {
			DBUtil.closeQuietly(rs);
			DBUtil.closeQuietly(pstmt);
		}
	}
	
	public static void insertBossSession(Connection conn, BossProtos.BossSessionData sessionData) throws SQLException {
		PreparedStatement pstmt = null;
		try {
			pstmt = conn.prepareStatement("INSERT INTO weizhu_boss_session (boss_id, session_id, login_time, login_host, user_agent, active_time, logout_time) VALUES (?, ?, ?, ?, ?, ?, ?); ");
			
			boolean hasSession = sessionData.hasSession();
			DBUtil.set(pstmt, 1, hasSession && sessionData.getSession().hasBossId(), sessionData.getSession().getBossId());
			DBUtil.set(pstmt, 2, hasSession && sessionData.getSession().hasSessionId(), sessionData.getSession().getSessionId());
			
			DBUtil.set(pstmt, 3, sessionData.hasLoginTime(), sessionData.getLoginTime());
			DBUtil.set(pstmt, 4, sessionData.hasLoginHost(), sessionData.getLoginHost());
			DBUtil.set(pstmt, 5, sessionData.hasUserAgent(), sessionData.getUserAgent());
			DBUtil.set(pstmt, 6, sessionData.hasActiveTime(), sessionData.getActiveTime());
			DBUtil.set(pstmt, 7, sessionData.hasLogoutTime(), sessionData.getLogoutTime());
			
			if (pstmt.executeUpdate() <= 0) {
				throw new RuntimeException("insert admin session fail");
			}
		} finally {
			DBUtil.closeQuietly(pstmt);
		}
	}
	
	public static boolean setBossSessionUserAgentAndActiveTime(Connection conn, BossProtos.BossSession bossSession, String userAgent, int activeTime) throws SQLException {
		PreparedStatement pstmt = null;
		try {
			pstmt = conn.prepareStatement("UPDATE weizhu_boss_session SET user_agent = ?, active_time = ? WHERE boss_id = ? AND session_id = ?; ");
			pstmt.setString(1, userAgent);
			pstmt.setInt(2, activeTime);
			pstmt.setString(3, bossSession.getBossId());
			pstmt.setLong(4, bossSession.getSessionId());
			
			return pstmt.executeUpdate() > 0;
		} finally {
			DBUtil.closeQuietly(pstmt);
		}
	}
	
	public static boolean setBossSessionLogoutTime(Connection conn, BossProtos.BossSession bossSession, int logoutTime) throws SQLException {
		PreparedStatement pstmt = null;
		try {
			pstmt = conn.prepareStatement("UPDATE weizhu_boss_session SET logout_time = ? WHERE boss_id = ? AND session_id = ?; ");
			pstmt.setInt(1, logoutTime);
			pstmt.setString(2, bossSession.getBossId());
			pstmt.setLong(3, bossSession.getSessionId());
			
			return pstmt.executeUpdate() > 0;
		} finally {
			DBUtil.closeQuietly(pstmt);
		}
	}
}
