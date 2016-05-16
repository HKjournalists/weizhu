package com.weizhu.service.discover;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Map;
import java.util.TreeMap;

import com.weizhu.common.db.DBUtil;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

public class DataImport {

	public static void main(String[] args) throws Exception {
		String dbHost = "127.0.0.1";
		int dbPort = 3306;
		String dbUser = "root";
		String dbPassword = "";
		String dbName = "weizhu_test";
		
		for (String arg : args) {
			if (arg.startsWith("-h")) {
				dbHost = arg.substring(2);
			} else if (arg.startsWith("-p")) {
				dbPort = Integer.parseInt(arg.substring(2));
			} else if (arg.startsWith("-u")) {
				dbUser = arg.substring(2);
			} else if (arg.startsWith("-P")) {
				dbPassword = arg.substring(2);
			} else if (arg.startsWith("-n")){
				dbName = arg.substring(2);
			}
		}
		
		HikariConfig config = new HikariConfig();
		config.setJdbcUrl("jdbc:mysql://" + dbHost + ":" + dbPort + "/" + dbName + "?useUnicode=true&characterEncoding=utf8&allowMultiQueries=true&autoReconnect=true&failOverReadOnly=false");
		config.setUsername(dbUser);
		if (dbPassword != null && !dbPassword.isEmpty()) {
			config.setPassword(dbPassword);
		}
		config.setMaximumPoolSize(3);
		config.addDataSourceProperty("cachePrepStmts", "true");
		config.addDataSourceProperty("prepStmtCacheSize", "250");
		config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
		config.addDataSourceProperty("useServerPrepStmts", "true");
		HikariDataSource hikariDataSource = new HikariDataSource(config);
		Connection dbConn = hikariDataSource.getConnection();
		try {
			doImport(dbConn);
		} finally {
			DBUtil.closeQuietly(dbConn);
			hikariDataSource.close();
		}
	}
	
	public static void doImport(Connection dbConn) throws Exception {
		
		final Map<Integer, Banner> bannerMap = new TreeMap<Integer, Banner>();
		
		Statement stmt = null;
		ResultSet rs = null;
		try {
			stmt = dbConn.createStatement();
			rs = stmt.executeQuery("SELECT * FROM weizhu_discover_banner; ");
			while (rs.next()) {
				Banner b = new Banner();
				b.bannerId = rs.getInt("banner_id");
				b.bannerName = rs.getString("banner_name");
				b.imageName = rs.getString("image_name");
				b.itemId = rs.getLong("item_id");
				if (rs.wasNull()) {
					b.itemId = null;
				}
				b.createTime = rs.getInt("create_time");
				
				if (b.itemId == null) {
					bannerMap.put(b.bannerId, b);
				}
			}
			
		} finally {
			DBUtil.closeQuietly(rs);
			DBUtil.closeQuietly(stmt);
		}
		
		// insert banner;
		PreparedStatement pstmt = null;
		try {
			pstmt = dbConn.prepareStatement("INSERT INTO weizhu_discover_v2_banner (banner_id, banner_name, image_name, state, create_time) VALUES (NULL, ?, ?, 'NORMAL', ?); "); 
			for (Banner b : bannerMap.values()) {
				DBUtil.set(pstmt, 1, true, b.bannerName);
				DBUtil.set(pstmt, 2, true, b.imageName);
				DBUtil.set(pstmt, 3, true, b.createTime);
				
				pstmt.addBatch();
			}
			pstmt.executeBatch();
		} finally {
			DBUtil.closeQuietly(pstmt);
		}
	}
	
	private static class Banner {
		int bannerId;
		String bannerName;
		String imageName;
		Long itemId;
		Integer createTime;
	}
	
}
