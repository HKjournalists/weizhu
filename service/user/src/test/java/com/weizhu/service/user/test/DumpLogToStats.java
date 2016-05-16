package com.weizhu.service.user.test;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.common.base.Charsets;
import com.google.common.base.Splitter;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

/*
在线用户数统计
SELECT U.company_id, U.team_1_id, U.team_2_id, U.team_1_name, U.team_2_name, count(distinct(U.user_id)) FROM 
  weizhu_stats_api A LEFT JOIN weizhu_stats_user U 
  ON A.company_id = U.company_id AND A.user_id = U.user_id 
WHERE
  A.time_millis >= 1432224000000 AND A.time_millis < 1432828800000
GROUP BY U.company_id, U.team_1_id, U.team_2_id
LIMIT 100;

在线用户列表
SELECT U.company_id, U.team_1_id, U.team_2_id, U.team_1_name, U.team_2_name, U.user_id, U.user_name, U.raw_id, count(*) FROM 
  weizhu_stats_api A LEFT JOIN weizhu_stats_user U 
  ON A.company_id = U.company_id AND A.user_id = U.user_id 
WHERE
  A.time_millis >= 1432224000000 AND A.time_millis < 1432828800000 AND
  U.company_id = 2 AND
  U.team_1_id = 2 AND 
  U.team_2_id = 4
GROUP BY U.company_id, U.user_id
ORDER BY A.time_millis ASC LIMIT 120;

在线用户详细操作
SELECT U.company_id, U.team_1_id, U.team_2_id, U.team_1_name, U.team_2_name, U.user_id, U.user_name, U.raw_id, FROM_UNIXTIME(A.time_millis/1000), A.invoke_name FROM 
  weizhu_stats_api A LEFT JOIN weizhu_stats_user U 
  ON A.company_id = U.company_id AND A.user_id = U.user_id 
WHERE
  A.time_millis >= 1432224000000 AND A.time_millis < 1432828800000 AND
  U.company_id = 2 AND
  U.team_1_id = 2 AND 
  U.team_2_id = 4
ORDER BY A.time_millis ASC LIMIT 100;

 */
public class DumpLogToStats {
	
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
		
		File apiLogDir = new File("/Users/lindongjlu/Downloads/weizhu_log/conn/");
		
		for (File logFile : apiLogDir.listFiles()) {
			if (logFile.getName().matches("^weizhu_server\\.\\d{4}-\\d{2}-\\d{2}\\.log$")) {
				System.out.println(logFile.getAbsolutePath());
				
				dumpLogToStats(logFile, statsDB);
			}
		}

		statsDB.close();
	}
	
	private static final Pattern TIME_PATTERN = Pattern.compile("^\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}\\.\\d{3}");
	private static final Splitter FIELD_SPLITTER = Splitter.on('|').trimResults().omitEmptyStrings();
	private static final Splitter FIELD_SPLITTER_2 = Splitter.on('/').trimResults().omitEmptyStrings();
	
	private static void dumpLogToStats(File logFile, HikariDataSource statsDB) throws SQLException, IOException, ParseException {
		BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(logFile), Charsets.UTF_8));
		Connection dbConn = statsDB.getConnection();
		try {
			
			final DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
			
			List<ApiItem> itemList = new ArrayList<ApiItem>(100);
			
			String line = null;
			while ((line = reader.readLine()) != null) {
				if (line.contains("HttpApiServlet")) {
					
					Matcher m = TIME_PATTERN.matcher(line);
					if (!m.find()) {
						continue;
					}
					
					ApiItem item = new ApiItem();
					
					item.time = df.parse(line.substring(0, 23)).getTime();
					
					for (String field : FIELD_SPLITTER.split(line)) {
						if (field.startsWith("Session:")) {
							List<String> l = FIELD_SPLITTER_2.splitToList(field.substring("Session:".length()));
							if (l.size() == 3) {
								item.companyId = Long.parseLong(l.get(0));
								item.userId = Long.parseLong(l.get(1));
								item.sessionId = Long.parseLong(l.get(2));
							}
						} else if (field.startsWith("Request:")) {
							List<String> l = FIELD_SPLITTER_2.splitToList(field.substring("Request:".length()));
							if (l.size() == 2) {
								item.invokeName = l.get(1);
							}
						}
					}
					
					if (item.time > 0 && item.companyId > 0 && item.userId > 0) {
						itemList.add(item);
						
						if (itemList.size() >= 100) {
							// do insert
							doInsertApiItemToDB(dbConn, itemList);
							itemList.clear();
						}
					}
					
				} else if (line.contains("SocketConnectionHandler") && line.contains("processApiRequest")) {
					Matcher m = TIME_PATTERN.matcher(line);
					if (!m.find()) {
						continue;
					}
					
					ApiItem item = new ApiItem();
					
					item.time = df.parse(line.substring(0, 23)).getTime();
					
					List<String> fieldList = FIELD_SPLITTER.splitToList(line);
					
					if (fieldList.size() >= 5) {
						List<String> l = FIELD_SPLITTER_2.splitToList(fieldList.get(2));
						if (l.size() == 3) {
							item.companyId = Long.parseLong(l.get(0));
							item.userId = Long.parseLong(l.get(1));
							item.sessionId = Long.parseLong(l.get(2));
						}
						
						item.invokeName = fieldList.get(3) + "." + fieldList.get(4);
					}
					
					if (item.time > 0 && item.companyId > 0 && item.userId > 0) {
						itemList.add(item);
						
						if (itemList.size() >= 100) {
							// do insert
							doInsertApiItemToDB(dbConn, itemList);
							itemList.clear();
						}
					}
				}
			}
			
			if (!itemList.isEmpty()) {
				// do insert
				doInsertApiItemToDB(dbConn, itemList);
				itemList.clear();
			}
			
		} finally {
			dbConn.close();
			reader.close();
		}
	}
	
	private static void doInsertApiItemToDB(Connection dbConn, List<ApiItem> itemList) throws SQLException {
		if (itemList.isEmpty()) {
			return;
		}
		
		StringBuilder sql = new StringBuilder();
		sql.append("INSERT INTO weizhu_stats_api (time_millis, company_id, user_id, session_id, invoke_name) VALUES ");
		boolean isFirst = true;
		
		for (ApiItem item : itemList) {
			if (isFirst) {
				isFirst = false;
			} else {
				sql.append(", ");
			}
			
			sql.append("(").append(item.time).append(", ");
			sql.append(item.companyId).append(", ").append(item.userId).append(", ").append(item.sessionId);
			sql.append(", '").append(item.invokeName).append("') ");
		}
		
		sql.append("; ");
		
		Statement stmt = dbConn.createStatement();
		try {
			stmt.executeUpdate(sql.toString());
		} finally {
			stmt.close();
		}
	}
	
	private static class ApiItem {
		long time;
		long companyId;
		long userId;
		long sessionId;
		String invokeName;
	}
}
