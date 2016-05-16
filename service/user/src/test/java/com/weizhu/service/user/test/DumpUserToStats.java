package com.weizhu.service.user.test;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

public class DumpUserToStats {

	public static void main(String[] args) throws Exception {

		HikariConfig config = new HikariConfig();
		config.setJdbcUrl("jdbc:mysql://127.0.0.1:3306/weizhu_stats?allowMultiQueries=true");
		config.setUsername("root");
		//config.setPassword("51mp50n");
		config.addDataSourceProperty("cachePrepStmts", "true");
		config.addDataSourceProperty("prepStmtCacheSize", "250");
		config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
		config.addDataSourceProperty("useServerPrepStmts", "true");
		
		HikariDataSource statsDB = new HikariDataSource(config);
		
		HikariDataSource haierFridgeDB = providesHaierFridgeDB();
		dumpUserInfo(2, haierFridgeDB, statsDB);
		haierFridgeDB.close();
		
		HikariDataSource yuantongDB = providesYuantongDB();
		dumpUserInfo(3, yuantongDB, statsDB);
		yuantongDB.close();
		
		statsDB.close();
	}
	
	private static HikariDataSource providesHaierFridgeDB() {
		HikariConfig config = new HikariConfig();
		config.setJdbcUrl("jdbc:mysql://127.0.0.1:3306/weizhu_company_haier_fridge?allowMultiQueries=true");
		config.setUsername("root");
		//config.setPassword("51mp50n");
		config.addDataSourceProperty("cachePrepStmts", "true");
		config.addDataSourceProperty("prepStmtCacheSize", "250");
		config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
		config.addDataSourceProperty("useServerPrepStmts", "true");
		
		return new HikariDataSource(config);
	}
	
	private static HikariDataSource providesYuantongDB() {
		HikariConfig config = new HikariConfig();
		config.setJdbcUrl("jdbc:mysql://127.0.0.1:3306/weizhu_company_yuantong?allowMultiQueries=true");
		config.setUsername("root");
		//config.setPassword("51mp50n");
		config.addDataSourceProperty("cachePrepStmts", "true");
		config.addDataSourceProperty("prepStmtCacheSize", "250");
		config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
		config.addDataSourceProperty("useServerPrepStmts", "true");
		
		return new HikariDataSource(config);
	}
	
	private static void dumpUserInfo(final long companyId, HikariDataSource userDB, HikariDataSource statsDB) throws SQLException {
		
		List<UserInfo> userInfoList;
		
		Connection dbConn = userDB.getConnection();
		Statement stmt = null;
		ResultSet rs = null;
		try {
			stmt = dbConn.createStatement();
			rs = stmt.executeQuery("SELECT * FROM weizhu_team; ");
			
			HashMap<Integer, TeamInfo> teamInfoMap = new HashMap<Integer, TeamInfo>();
			while (rs.next()) {
				TeamInfo teamInfo = new TeamInfo();
				
				teamInfo.teamId = rs.getInt("team_id");
				teamInfo.teamName = rs.getString("team_name");
				teamInfo.parentTeamId = rs.getInt("parent_team_id");
				if (rs.wasNull()) {
					teamInfo.parentTeamId = null;
				}
				
				teamInfoMap.put(teamInfo.teamId, teamInfo);
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
			rs = stmt.executeQuery("SELECT * FROM weizhu_user_base; ");
			
			userInfoList = new ArrayList<UserInfo>();
			
			while (rs.next()) {
				UserInfo userInfo = new UserInfo();
				userInfo.userId = rs.getLong("user_id");
				userInfo.userName = rs.getString("user_name");
				userInfo.rawId = rs.getString("raw_id");
				
				userInfo.teamInfoList = new LinkedList<TeamInfo>();
				
				Integer teamId = userTeamIdMap.get(userInfo.userId);
				while (teamId != null) {
					TeamInfo teamInfo = teamInfoMap.get(teamId);
					
					if (teamInfo == null) {
						// error
						System.err.println("cannot find team : " + companyId + ", " + teamId);
					}
					userInfo.teamInfoList.addFirst(teamInfo);
					teamId = teamInfo.parentTeamId;
				}
				
				userInfoList.add(userInfo);
			}
		} finally {
			rs.close();
			stmt.close();
			dbConn.close();
		}
		
		dbConn = statsDB.getConnection();
		stmt = null;
		try {
			stmt = dbConn.createStatement();
			stmt.executeUpdate("DELETE FROM weizhu_stats_user WHERE company_id = " + companyId + "; ");
			
			int idx = 0;
			
			while (idx < userInfoList.size()) {
				
				StringBuilder sql = new StringBuilder();
				sql.append("INSERT INTO weizhu_stats_user (company_id, user_id, user_name, raw_id, team_1_id, team_1_name, team_2_id, team_2_name, team_3_id, team_3_name, team_4_id, team_4_name, team_5_id, team_5_name) VALUES ");
				
				boolean isFirst = true;
				for (int i=0; i<100 && idx < userInfoList.size(); ++i, ++idx) {
					
					if (isFirst) {
						isFirst = false;
					} else {
						sql.append(", ");
					}
					
					UserInfo userInfo = userInfoList.get(idx);
					
					sql.append("(").append(companyId).append(", ");
					sql.append(userInfo.userId).append(", ");
					sql.append("'").append(userInfo.userName).append("', ");
					sql.append("'").append(userInfo.rawId).append("'");
					
					for (int j=0; j<5; ++j) {
						sql.append(", ");
						if (j < userInfo.teamInfoList.size()) {
							TeamInfo teamInfo = userInfo.teamInfoList.get(j);
							sql.append(teamInfo.teamId).append(", ");
							sql.append("'").append(teamInfo.teamName).append("' ");
						} else {
							sql.append("NULL, NULL");
						}
					}
					sql.append(")");
				}
				
				stmt.executeUpdate(sql.toString());
			}
		} finally {
			if (stmt != null) {
				stmt.close();
			}
			dbConn.close();
		}
	}
	
	static class UserInfo {
		long userId;
		String userName;
		String rawId;
		LinkedList<TeamInfo> teamInfoList;
	}
	
	static class TeamInfo {
		private int teamId;
		private String teamName;
		private Integer parentTeamId;
	}

}
