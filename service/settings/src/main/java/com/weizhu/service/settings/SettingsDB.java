package com.weizhu.service.settings;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import com.weizhu.common.db.DBUtil;
import com.weizhu.common.db.ProtobufMapper;
import com.weizhu.proto.SettingsProtos;

public class SettingsDB {

	private static final ProtobufMapper<SettingsProtos.Settings> SETTINGS_MAPPER = 
			ProtobufMapper.createMapper(SettingsProtos.Settings.getDefaultInstance(), 
					"user_id",
					"do_not_disturb.enable",
					"do_not_disturb.begin_time",
					"do_not_disturb.end_time");
	
	public static Map<Long, SettingsProtos.Settings> getSettings(Connection conn, long companyId, Collection<Long> userIds) throws SQLException {
		if (userIds.isEmpty()) {
			return Collections.emptyMap();
		}
		StringBuilder sql = new StringBuilder();
		sql.append("SELECT user_id, do_not_disturb_enable as `do_not_disturb.enable`, do_not_disturb_begin_time as `do_not_disturb.begin_time`, do_not_disturb_end_time as `do_not_disturb.end_time` FROM weizhu_settings WHERE  company_id = ")
					.append(companyId)
					.append(" AND user_id IN (");
		DBUtil.COMMA_JOINER.appendTo(sql, userIds);
		sql.append("); ");
		
		Statement stmt = null;
		ResultSet rs = null;
		try {
			stmt = conn.createStatement();
			rs = stmt.executeQuery(sql.toString());
			
			SettingsProtos.Settings.Builder tmpBuilder = SettingsProtos.Settings.newBuilder();
			
			Map<Long, SettingsProtos.Settings> resultMap = new HashMap<Long, SettingsProtos.Settings>(userIds.size());
			while (rs.next()) {
				tmpBuilder.clear();
				SettingsProtos.Settings settings = SETTINGS_MAPPER.mapToItem(rs, tmpBuilder).build();
				resultMap.put(settings.getUserId(), settings);
			}
			
			return resultMap;
		} finally {
			DBUtil.closeQuietly(rs);
			DBUtil.closeQuietly(stmt);
		}
	}
	
	public static boolean updateDoNotDisturb(Connection conn, long companyId, long userId, SettingsProtos.Settings.DoNotDisturb doNotDisturb) throws SQLException {
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try {
			pstmt = conn.prepareStatement("INSERT IGNORE INTO weizhu_settings (company_id, user_id) VALUES (?, ?); UPDATE weizhu_settings SET do_not_disturb_enable = ?, do_not_disturb_begin_time = ?, do_not_disturb_end_time = ? WHERE company_id = ? AND user_id = ?; ");
			
			pstmt.setLong(1, companyId);
			pstmt.setLong(2, userId);
			pstmt.setBoolean(3, doNotDisturb.getEnable());
			DBUtil.set(pstmt, 4, doNotDisturb.hasBeginTime(), doNotDisturb.getBeginTime());
			DBUtil.set(pstmt, 5, doNotDisturb.hasEndTime(), doNotDisturb.getEndTime());
			pstmt.setLong(6, companyId);
			pstmt.setLong(7, userId);
			
			return pstmt.executeUpdate() > 0;
		} finally {
			DBUtil.closeQuietly(rs);
			DBUtil.closeQuietly(pstmt);
		}
	}
	
}
