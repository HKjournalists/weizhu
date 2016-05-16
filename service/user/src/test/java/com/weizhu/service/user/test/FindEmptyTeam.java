package com.weizhu.service.user.test;

import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.LinkedHashMap;
import java.util.Map;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

public class FindEmptyTeam {

	public static void main(String[] args) throws Throwable {
		PrintWriter w = new PrintWriter("team_mem_cnt.txt");
		PrintWriter w2 = new PrintWriter("team_mem_empty.txt");
		
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
			rs = stmt.executeQuery("SELECT * FROM weizhu_team ORDER BY team_id ASC; ");
			
			Map<Integer, Team> teamMap = new LinkedHashMap<Integer, Team>();
			while (rs.next()) {
				Team team = new Team();
				
				team.teamId = rs.getInt("team_id");
				team.teamName = rs.getString("team_name");
				int parentTeamId = rs.getInt("parent_team_id");
				team.parentTeamId = rs.wasNull() ? null : parentTeamId;
				team.memCnt = 0;
				
				teamMap.put(team.teamId, team);
			}
			
			rs.close();
			stmt.close();
			
			stmt = dbConn.createStatement();
			rs = stmt.executeQuery("SELECT * FROM weizhu_user_team; ");
			
			while (rs.next()) {
				int teamId = rs.getInt("team_id");
				
				while (true) {
					Team team = teamMap.get(teamId);
					if (team == null) {
						System.out.println("cannot find team : " + teamId);
					}
					
					team.memCnt ++;
					
					if (team.parentTeamId == null) {
						break;
					} else {
						teamId = team.parentTeamId;
					}
				}
			}
			
			for (Team team : teamMap.values()) {
				if (team.memCnt <= 0) {
					w2.println(team.teamId);
				}
				
				w.println(team.teamId + "\t" + team.teamName + "\t" + team.memCnt);
			}
			
		} finally {
			rs.close();
			stmt.close();
			dbConn.close();
		}
		
		hikariDataSource.close();
		w.close();
		w2.close();
	}
	
	static class Team {
		int teamId;
		String teamName;
		Integer parentTeamId;
		int memCnt;
	}

}
