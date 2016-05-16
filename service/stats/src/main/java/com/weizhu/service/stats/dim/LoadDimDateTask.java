package com.weizhu.service.stats.dim;

import java.sql.Connection;
import java.sql.Statement;
import java.util.Calendar;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.weizhu.common.db.DBUtil;
import com.zaxxer.hikari.HikariDataSource;

public class LoadDimDateTask implements Runnable {

	private static final Logger logger = LoggerFactory.getLogger(LoadDimDateTask.class);
	
	private final HikariDataSource hikariDataSource;
	
	public LoadDimDateTask(HikariDataSource hikariDataSource) {
		this.hikariDataSource = hikariDataSource;
	}
	
	@Override
	public void run() {
		Connection dbConn = null;
		try {
			dbConn = this.hikariDataSource.getConnection();
			
			final Calendar cal = Calendar.getInstance();
			cal.setFirstDayOfWeek(Calendar.MONDAY);
			cal.setMinimalDaysInFirstWeek(7);
			
			cal.set(Calendar.YEAR, 1950);
			cal.set(Calendar.MONTH, 0);
			cal.set(Calendar.DAY_OF_MONTH, 1);
			cal.set(Calendar.HOUR_OF_DAY, 0);
			cal.set(Calendar.MINUTE, 0);
			cal.set(Calendar.SECOND, 0);
			cal.set(Calendar.MILLISECOND, 0);
			
			StringBuilder sqlBuilder = null;
			int preWeekOfYear = 0;
			int week = 0;
			while (cal.get(Calendar.YEAR) <= 2050) {
				if (sqlBuilder == null) {
					sqlBuilder = new StringBuilder("REPLACE INTO weizhu_stats_dim_date (`date`, `year`, `quarter`, `month`, `week`) VALUES ");
				} else {
					sqlBuilder.append(", ");
				}
				
				int year = cal.get(Calendar.YEAR);
				int month = cal.get(Calendar.MONTH) + 1;
				int quarter = (month - 1) / 3 + 1;
				int day = cal.get(Calendar.DAY_OF_MONTH);
				int weekOfYear = cal.get(Calendar.WEEK_OF_YEAR);
				if (weekOfYear != preWeekOfYear) {
					week = year * 100 + weekOfYear;
					preWeekOfYear = weekOfYear;
				}
				
				sqlBuilder.append("(");
				sqlBuilder.append(year * 10000 + month * 100 + day).append(", ");
				sqlBuilder.append(year).append(", ");
				sqlBuilder.append(quarter).append(", ");
				sqlBuilder.append(month).append(", ");
				sqlBuilder.append(week).append(")");
				
				if (sqlBuilder.length() > 4096) {
					sqlBuilder.append("; ");
					
					Statement stmt = dbConn.createStatement();
					try {
						stmt.executeUpdate(sqlBuilder.toString());
					} finally {
						DBUtil.closeQuietly(stmt);
					}
					sqlBuilder = null;
				}
				
				cal.add(Calendar.DAY_OF_MONTH, 1);
			}
			
			if (sqlBuilder != null) {
				sqlBuilder.append("; ");
				
				Statement stmt = dbConn.createStatement();
				try {
					stmt.executeUpdate(sqlBuilder.toString());
				} finally {
					DBUtil.closeQuietly(stmt);
				}
				sqlBuilder = null;
			}
		} catch (Throwable th) {
			logger.error("LoadDimDateTask fail!", th);
		} finally {
			DBUtil.closeQuietly(dbConn);
		}
	}

}
