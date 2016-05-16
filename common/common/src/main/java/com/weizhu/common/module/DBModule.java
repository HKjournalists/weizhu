package com.weizhu.common.module;

import java.util.Properties;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.multibindings.ProvidesIntoSet;
import com.google.inject.name.Named;
import com.weizhu.common.server.ServerEntry;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

public final class DBModule extends AbstractModule {
	
	@Override
	protected void configure() {
	}
	
	@Provides
	@Singleton
	public HikariDataSource provideHikariDataSource(@Named("server_conf") Properties confProperties) {
		HikariConfig config = new HikariConfig();
		config.setJdbcUrl(confProperties.getProperty("db_url"));
		config.setUsername(confProperties.getProperty("db_username"));
		config.setPassword(confProperties.getProperty("db_password"));
		config.addDataSourceProperty("cachePrepStmts", "true");
		config.addDataSourceProperty("prepStmtCacheSize", "250");
		config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
		config.addDataSourceProperty("useServerPrepStmts", "true");
		config.setConnectionTimeout(3000);
		return new HikariDataSource(config);
	}
	
	@ProvidesIntoSet
	public ServerEntry.ShutdownHook provideShutdownHook(final HikariDataSource hikariDataSource) {
		return new ServerEntry.ShutdownHook() {
			
			@Override
			public Order order() {
				return ServerEntry.ShutdownHook.Order.RESOURCE;
			}

			@Override
			public void execute() {
				hikariDataSource.close();
			}
			
		};
	}
	
}
