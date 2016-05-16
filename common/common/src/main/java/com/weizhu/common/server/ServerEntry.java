package com.weizhu.common.server;

import java.sql.Connection;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Set;

import com.google.common.base.Charsets;
import com.google.common.io.Resources;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.name.Names;
import com.google.inject.util.Types;
import com.weizhu.common.db.DBUtil;
import com.zaxxer.hikari.HikariDataSource;

public class ServerEntry {

	public static interface StartHook {
		void execute();
	}
	
	public static interface ShutdownHook {
		public static enum Order {
			SERVER,
			NETTY_EVENTLOOP,
			EXECUTOR,
			RESOURCE
		}
		
		Order order();
		void execute();
	}
	
	@SuppressWarnings("unchecked")
	public static void main(Injector injector) {
		try {
			final Set<StartHook> startHookSet = (Set<StartHook>) injector.getInstance(Key.get(Types.setOf(StartHook.class)));
			final List<ShutdownHook> shutdownHookList = new ArrayList<ShutdownHook>((Set<ShutdownHook>) injector.getInstance(Key.get(Types.setOf(ShutdownHook.class))));
			Collections.sort(shutdownHookList, new Comparator<ShutdownHook>() {

				@Override
				public int compare(ShutdownHook o1, ShutdownHook o2) {
					return o1.order().compareTo(o2.order());
				}
				
			});
			
			for (StartHook startHook : startHookSet) {
				startHook.execute();
			}
			Runtime.getRuntime().addShutdownHook(new Thread() {

				@Override
				public void run() {
					for (ShutdownHook shutdownHook : shutdownHookList) {
						long begin = System.currentTimeMillis();
						try {
							shutdownHook.execute();
						} catch (Throwable th) {
							th.printStackTrace();
						}
						System.out.println("shutdown " + shutdownHook.getClass().getName() + " " + (System.currentTimeMillis() - begin) + "(ms)");
					}
					System.out.println(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS").format(new Date()) + " " + ServerConst.SERVER_NAME + " shutdown");
				}
				
			});
			
			System.out.println(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS").format(new Date()) + " " + ServerConst.SERVER_NAME + " started ");
		} catch (Throwable th) {
			th.printStackTrace();
			System.exit(1);
		}
	}
	
	@SuppressWarnings("unchecked")
	public static List<ShutdownHook> start(Injector injector) {
		try {
			Set<StartHook> startHookSet = (Set<StartHook>) injector.getInstance(Key.get(Types.setOf(StartHook.class)));
			List<ShutdownHook> shutdownHookList = new ArrayList<ShutdownHook>((Set<ShutdownHook>) injector.getInstance(Key.get(Types.setOf(ShutdownHook.class))));
			Collections.sort(shutdownHookList, new Comparator<ShutdownHook>() {

				@Override
				public int compare(ShutdownHook o1, ShutdownHook o2) {
					return o1.order().compareTo(o2.order());
				}
				
			});
			
			for (StartHook startHook : startHookSet) {
				startHook.execute();
			}
			return shutdownHookList;
		} catch (Throwable th) {
			th.printStackTrace();
			System.exit(1);
			return null;
		}
	}
	
	public static void shutdown(List<ShutdownHook> shutdownHookList) {
		for (ShutdownHook shutdownHook : shutdownHookList) {
			try {
				shutdownHook.execute();
			} catch (Throwable th) {
				th.printStackTrace();
			}
		}
	}
	
	@SuppressWarnings("unchecked")
	public static void initDB(Injector injector) {
		try {
			final Set<String> createTableSQLSet = (Set<String>) injector.getInstance(Key.get(Types.setOf(String.class), Names.named("db_create_table.sql")));
			final Set<String> initDataSQLSet = (Set<String>) injector.getInstance(Key.get(Types.setOf(String.class), Names.named("db_init_data.sql")));
			if (createTableSQLSet.isEmpty() && initDataSQLSet.isEmpty()) {
				return;
			}
			final HikariDataSource hikariDataSource = injector.getInstance(HikariDataSource.class);
			Connection dbConn = null;
			Statement stmt = null;;
			try {
				dbConn = hikariDataSource.getConnection();
				stmt = dbConn.createStatement();
				for (String sqlFile : createTableSQLSet) {
					System.out.print("execute sql : " + sqlFile);
					stmt.execute(Resources.toString(Resources.getResource(sqlFile), Charsets.UTF_8));
				}
				
				for (String sqlFile : initDataSQLSet) {
					System.out.print("execute sql : " + sqlFile);
					stmt.execute(Resources.toString(Resources.getResource(sqlFile), Charsets.UTF_8));
				}
			} finally {
				DBUtil.closeQuietly(stmt);
				DBUtil.closeQuietly(dbConn);
				hikariDataSource.close();
			}
		} catch (Throwable th) {
			th.printStackTrace();
			System.exit(1);
		}
	}
}
