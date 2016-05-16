package com.weizhu.service.stats.test;

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
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.common.base.Charsets;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

public class DumpLogToStats {
	
	public static void main(String[] args) throws Exception {

		HikariConfig config = new HikariConfig();
		config.setJdbcUrl("jdbc:mysql://127.0.0.1:3306/weizhu_test?allowMultiQueries=true");
		config.setUsername("root");
		//config.setPassword("51mp50n");
		config.addDataSourceProperty("cachePrepStmts", "true");
		config.addDataSourceProperty("prepStmtCacheSize", "250");
		config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
		config.addDataSourceProperty("useServerPrepStmts", "true");
		
		HikariDataSource statsDB = new HikariDataSource(config);
		
		File apiLogDir = new File("/Users/lindongjlu/Downloads/weizhu_log/api/");
		
		for (File logFile : apiLogDir.listFiles()) {
			if (logFile.getName().matches("^weizhu_server\\.\\d{4}-\\d{2}-\\d{2}\\.log$")) {
				System.out.println(logFile.getAbsolutePath());
				
				dumpLogToStats(logFile, statsDB);
			}
		}

		statsDB.close();
	}
	
// 2015-06-04 00:02:05.727 INFO  c.w.s.api.HttpApiServlet 1(ms)|Session:2/20000008698/-8099085000243533796|Request:162(B)/SystemService.checkNewVersion|Weizhu:ANDROID/0.8.8/27/BETA/0|Android:HM2014501/Xiaomi/Xiaomi/2014501/LRSO9SVGS4MZAQQC/4.4.2/19/REL|Response:40(B)/SUCC/
	
	private static final Pattern LOG_PATTERN = Pattern.compile("^(?<time>\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}\\.\\d{3}) INFO  c.w.s.api.HttpApiServlet "
			+ "(?<duration>\\d+)\\(ms\\)\\|Session:(?<companyId>\\d+)/(?<userId>-?\\d+)/(?<sessionId>-?\\d+)\\|"
			+ "Request:(?<requestPacketLength>\\d+)\\(B\\)/(?<serviceName>\\w+)\\.(?<functionName>\\w+)\\|"
			+ "Weizhu:(?<weizhuPlatform>ANDROID|IPHONE)/(?<weizhuVersionName>[0-9\\.]+)/(?<weizhuVersionCode>\\d+)/.*\\|"
			+ "Response:(?<responsePacketLength>\\d+)\\(B\\)/");
	
	private static void dumpLogToStats(File logFile, HikariDataSource statsDB) throws SQLException, IOException, ParseException {
		BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(logFile), Charsets.UTF_8));
		Connection dbConn = statsDB.getConnection();
		try {
			
			final DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
			
			List<ApiItem> itemList = new ArrayList<ApiItem>(100);
			
			String line = null;
			while ((line = reader.readLine()) != null) {
				if (line.contains("HttpApiServlet")) {
					
					Matcher m = LOG_PATTERN.matcher(line);
					if (!m.find()) {
						continue;
					}
					
					ApiItem item = new ApiItem();
					
					item.time = df.parse(m.group("time")).getTime();
					item.duration = Integer.parseInt(m.group("duration"));
					item.companyId = Long.parseLong(m.group("companyId"));
					item.userId = Long.parseLong(m.group("userId"));
					item.sessionId = Long.parseLong(m.group("sessionId"));
					item.serviceName = m.group("serviceName");
					item.functionName = m.group("functionName");
					item.weizhuPlatform = m.group("weizhuPlatform");
					item.weizhuVersionName = m.group("weizhuVersionName");
					item.weizhuVersionCode = m.group("weizhuVersionCode");
					item.requestPacketLength = Integer.parseInt(m.group("requestPacketLength"));
					item.responsePacketLength = Integer.parseInt(m.group("responsePacketLength"));
					
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
		
		sql.append("INSERT INTO weizhu_stats_weizhu_api_invoke (stats_millis, stats_date, company_id, user_id, session_id, weizhu_platform, weizhu_version, server_name, service_name, function_name, request_packet_length, response_packet_length, invoke_duration, is_exception) VALUES ");
		boolean isFirst = true;
		
		final DateFormat df = new SimpleDateFormat("yyyy-MM-dd");
		for (ApiItem item : itemList) {
			if (isFirst) {
				isFirst = false;
			} else {
				sql.append(", ");
			}
			
			sql.append("(").append(item.time).append(", '").append(df.format(new Date(item.time))).append("', ");
			sql.append(item.companyId).append(", ").append(item.userId).append(", ").append(item.sessionId).append(", '");
			sql.append(item.weizhuPlatform).append("', '").append(item.weizhuVersionName + "_" + item.weizhuVersionCode).append("', '");
			sql.append("weizhu_api_server', '").append(item.serviceName).append("', '").append(item.functionName).append("', ");
			sql.append(item.requestPacketLength).append(", ").append(item.responsePacketLength).append(", ").append(item.duration).append(", 0)");
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
		int duration;
		long companyId;
		long userId;
		long sessionId;
		String serviceName;
		String functionName;
		String weizhuPlatform;
		String weizhuVersionName;
		String weizhuVersionCode;
		int requestPacketLength;
		int responsePacketLength;
	}
}
