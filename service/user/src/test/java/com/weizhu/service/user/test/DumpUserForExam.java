package com.weizhu.service.user.test;

import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

public class DumpUserForExam {

	public static void main(String[] args) throws Throwable {
		
		PrintWriter w = new PrintWriter("all_user.txt");
		
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
			rs = stmt.executeQuery("SELECT * FROM weizhu_user_base ORDER BY user_id ASC; ");
			
			w.println("姓名\t性别\t唯一标识\t电话\temail\t所属地区");
			while (rs.next()) {
				String userName = rs.getString("user_name");
				String genderStr = rs.getString("gender");
				String gender;
				if ("MALE".equals(genderStr)) {
					gender = "男";
				} else if ("FEMALE".equals(genderStr)) {
					gender = "女";
				} else {
					gender = "";
				}
				
				long userId = rs.getLong("user_id");
				String email = rs.getString("email");
				
				email = email == null ? "" : email;
				
				w.println(userName + "\t" + gender + "\t" + userId + "\t\t" + email + "\t\t");
			}
		} finally {
			rs.close();
			stmt.close();
			dbConn.close();
		}
		
		w.close();
		hikariDataSource.close();
	}

}
