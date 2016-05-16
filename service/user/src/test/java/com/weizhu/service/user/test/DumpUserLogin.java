package com.weizhu.service.user.test;

import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

public class DumpUserLogin {

	public static void main(String[] args) throws Exception {
		
		PrintWriter w = new PrintWriter("user_id.txt");
		
		HikariConfig config = new HikariConfig();
		config.setJdbcUrl("jdbc:mysql://127.0.0.1:3306/weizhu_company_haier_fridge?allowMultiQueries=true");
		config.setUsername("root");
		//config.setPassword("51mp50n");
		config.addDataSourceProperty("cachePrepStmts", "true");
		config.addDataSourceProperty("prepStmtCacheSize", "250");
		config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
		config.addDataSourceProperty("useServerPrepStmts", "true");
		
		HikariDataSource hikariDataSource = new HikariDataSource(config);
		
		Connection dbConn = hikariDataSource.getConnection();
		Statement stmt = null;
		ResultSet rs = null;
		try {
			stmt = dbConn.createStatement();
			rs = stmt.executeQuery("SELECT * FROM weizhu_team; ");
			
			HashMap<Integer, String> teamNameMap = new HashMap<Integer, String>();
			HashMap<Integer, Integer> parentTeamIdMap = new HashMap<Integer, Integer>();
			while (rs.next()) {
				Integer teamId = rs.getInt("team_id");
				String teamName = rs.getString("team_name");
				Integer parentTeamId = rs.getInt("parent_team_id");
				if (rs.wasNull()) {
					parentTeamId = null;
				}
				
				teamNameMap.put(teamId, teamName);
				parentTeamIdMap.put(teamId, parentTeamId);
			}
			
			rs.close();
			stmt.close();
			
			stmt = dbConn.createStatement();
			rs = stmt.executeQuery("SELECT * FROM weizhu_user_team; ");
			
			HashMap<Long, Integer> userTeamIdMap = new HashMap<Long, Integer>();
			while (rs.next()) {
				int teamId = rs.getInt("team_id");
				long userId = rs.getLong("user_id");
				
				userTeamIdMap.put(userId, teamId);
			}
			
			rs.close();
			stmt.close();
			
			stmt = dbConn.createStatement();
			rs = stmt.executeQuery("SELECT * FROM weizhu_common.weizhu_session WHERE company_id=2; ");
			
			HashMap<Long, UserLoginInfo> userLoginInfoMap = new HashMap<Long, UserLoginInfo>(); 
			while (rs.next()) {
				long userId = rs.getLong("user_id");
				long sessionId = rs.getLong("session_id");
				int loginTime = rs.getInt("login_time");
				int activeTime = rs.getInt("active_time");
				
				UserLoginInfo userLoginInfo = userLoginInfoMap.get(userId);
				if (userLoginInfo == null || userLoginInfo.loginTime < loginTime) {
					userLoginInfo = new UserLoginInfo();
					userLoginInfo.userId = userId;
					userLoginInfo.sessionId = sessionId;
					userLoginInfo.loginTime = loginTime;
					userLoginInfo.activeTime = activeTime;
					userLoginInfoMap.put(userId, userLoginInfo);
				}
			}

			rs.close();
			stmt.close();
			
			stmt = dbConn.createStatement();
			rs = stmt.executeQuery("SELECT * FROM weizhu_common.weizhu_session_weizhu WHERE company_id=2; ");
			
			while (rs.next()) {
				long userId = rs.getLong("user_id");
				long sessionId = rs.getLong("session_id");
				String platform = rs.getString("platform");
				String versionName = rs.getString("version_name");
				String versionCode = rs.getString("version_code");
				
				UserLoginInfo userLoginInfo = userLoginInfoMap.get(userId);
				if (userLoginInfo != null && userLoginInfo.sessionId == sessionId) {
					userLoginInfo.platform = platform;
					userLoginInfo.versionName = versionName;
					userLoginInfo.versionCode = versionCode;
				}
			}

			rs.close();
			stmt.close();
			
			stmt = dbConn.createStatement();
			rs = stmt.executeQuery("SELECT * FROM weizhu_user_base; ");
			
			DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
			while (rs.next()) {
				long userId = rs.getLong("user_id");
				String rawId = rs.getString("raw_id");
				String userName = rs.getString("user_name");
				
				LinkedList<String> teamList = new LinkedList<String>();
				
				Integer teamId = userTeamIdMap.get(userId);
				while (teamId != null) {
					String teamName = teamNameMap.get(teamId);
					teamList.addFirst(teamName);
					teamId = parentTeamIdMap.get(teamId);
				}
				while (teamList.size() < 5) {
					teamList.add("");
				}
				
				
				UserLoginInfo userLoginInfo = userLoginInfoMap.get(userId);
				
				if (userLoginInfo != null) {
					
					StringBuilder sb = new StringBuilder();
					sb.append(userId).append("\t");
					sb.append(rawId).append("\t");
					sb.append(userName).append("\t");
					
					for(String teamName : teamList) {
						sb.append(teamName).append("\t");
					}
					
					sb.append(df.format(new Date(userLoginInfo.loginTime * 1000L))).append("\t");
					sb.append(df.format(new Date(userLoginInfo.activeTime * 1000L))).append("\t");
					sb.append(userLoginInfo.platform).append("\t");
					sb.append(userLoginInfo.versionName).append("\t");
					sb.append(userLoginInfo.versionCode).append("\t");
					
					w.println(sb.toString());
				}
			}
			
		} finally {
			rs.close();
			stmt.close();
			dbConn.close();
		}
		
		w.close();
		hikariDataSource.close();
	}
	
	static class UserLoginInfo {
		long userId;
		long sessionId;
		int loginTime;
		int activeTime;
		String platform;
		String versionName;
		String versionCode;
	}
	
}
