package com.weizhu.service.user.test;

import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.TreeSet;

import com.weizhu.common.db.DBUtil;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

public class FindUserNotLogin {

	public static void main(String[] args) throws Exception {
		PrintWriter w = new PrintWriter("user_id.txt");
		
		HikariConfig config = new HikariConfig();
		config.setJdbcUrl("jdbc:mysql://127.0.0.1:3306/haier_fridge?allowMultiQueries=true");
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
			
			HashMap<Integer, List<Integer>> teamIdMap = new HashMap<Integer, List<Integer>>();
			HashMap<Integer, String> teamNameMap = new HashMap<Integer, String>();
			HashMap<Integer, Integer> parentTeamIdMap = new HashMap<Integer, Integer>();
			while (rs.next()) {
				Integer teamId = rs.getInt("team_id");
				String teamName = rs.getString("team_name");
				Integer parentTeamId = rs.getInt("parent_team_id");
				if (rs.wasNull()) {
					parentTeamId = null;
				}
				
				List<Integer> list = teamIdMap.get(parentTeamId);
				if (list == null) {
					list = new ArrayList<Integer>();
					teamIdMap.put(parentTeamId, list);
				}
				list.add(teamId);
				
				teamNameMap.put(teamId, teamName);
				
				parentTeamIdMap.put(teamId, parentTeamId);
			}
			
			rs.close();
			stmt.close();
			
			stmt = dbConn.createStatement();
			rs = stmt.executeQuery("SELECT * FROM weizhu_user_team; ");
			
			HashMap<Integer, List<Long>> teamUserIdMap = new HashMap<Integer, List<Long>>();
			HashMap<Long, Integer> userTeamIdMap = new HashMap<Long, Integer>();
			while (rs.next()) {
				int teamId = rs.getInt("team_id");
				long userId = rs.getLong("user_id");
				
				List<Long> list = teamUserIdMap.get(teamId);
				if (list == null) {
					list = new ArrayList<Long>();
					teamUserIdMap.put(teamId, list);
				}
				list.add(userId);
				
				userTeamIdMap.put(userId, teamId);
			}
			
			
			Set<Long> userIdSet = new TreeSet<Long>();
			
			Queue<Integer> queue = new LinkedList<Integer>();
			queue.add(4306);
			
			while (!queue.isEmpty()) {
				Integer teamId = queue.poll();
				
				List<Long> userIdList = teamUserIdMap.get(teamId);
				
				if (userIdList != null) {
					userIdSet.addAll(userIdList);
				}
				
				List<Integer> teamIdList = teamIdMap.get(teamId);
				if (teamIdList != null) {
					queue.addAll(teamIdList);
				}
			}
			
			rs.close();
			stmt.close();
			
			stmt = dbConn.createStatement();
			rs = stmt.executeQuery("SELECT * FROM weizhu_session WHERE company_id=2; ");
			
			while (rs.next()) {
				long userId = rs.getLong("user_id");
				userIdSet.remove(userId);
			}
			
			String userIdStr = DBUtil.COMMA_JOINER.join(userIdSet);
			
			rs.close();
			stmt.close();
			
			stmt = dbConn.createStatement();
			rs = stmt.executeQuery("SELECT * FROM weizhu_user_base WHERE user_id IN (" + userIdStr + "); ");
			
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
				
				StringBuilder sb = new StringBuilder();
				sb.append(userId).append("\t");
				sb.append(rawId).append("\t");
				sb.append(userName).append("\t");
				
				for(String teamName : teamList) {
					sb.append(teamName).append("\t");
				}
				
				w.println(sb.toString());
			}
			
		} finally {
			rs.close();
			stmt.close();
			dbConn.close();
		}
		
		w.close();
		hikariDataSource.close();
	}
	
	public static class Team {
		int teamId;
		String teamName;
		List<Integer> subTeamIdList;
	}

}
