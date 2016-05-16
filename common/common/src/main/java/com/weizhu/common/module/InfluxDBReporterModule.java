package com.weizhu.common.module;

import java.util.Properties;
import java.util.concurrent.TimeUnit;

import javax.annotation.Nullable;

import org.influxdb.InfluxDB.ConsistencyLevel;

import com.google.common.collect.ImmutableMap;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.multibindings.ProvidesIntoSet;
import com.google.inject.name.Named;
import com.weizhu.common.config.ConfigUtil;
import com.weizhu.common.influxdb.InfluxDBReporter;
import com.weizhu.common.influxdb.JvmGarbageCollectorMetrics;
import com.weizhu.common.influxdb.JvmMemoryUsageMetrics;
import com.weizhu.common.server.ServerConst;
import com.weizhu.common.server.ServerEntry;

public class InfluxDBReporterModule extends AbstractModule {

	@Override
	protected void configure() {
		
	}
	
	@Provides
	@Singleton
	public InfluxDBReporter provideInfluxDBReporter(@Named("server_conf") Properties confProperties) {
		String url = ConfigUtil.getNullToEmpty(confProperties, "influxdb_reporter_url");
		if (url.isEmpty()) {
			return null;
		}
		
		String username = ConfigUtil.getNullToEmpty(confProperties, "influxdb_reporter_username");
		String password = ConfigUtil.getNullToEmpty(confProperties, "influxdb_reporter_password");
		String database = ConfigUtil.getNullToEmpty(confProperties, "influxdb_reporter_database");
		
		InfluxDBReporter reporter = new InfluxDBReporter(url, username, password, database, "default", ConsistencyLevel.ANY, ImmutableMap.of("server", ServerConst.SERVER_NAME));
		// reporter.register("gc", ImmutableMap.of(), new JvmGarbageCollectorMetric());
		// reporter.register("memory", ImmutableMap.of(), new JvmMemoryUsageMetric());
		
		JvmGarbageCollectorMetrics.registerAll(reporter);
		JvmMemoryUsageMetrics.registerAll(reporter);
		
		reporter.start(1, TimeUnit.MINUTES);
		return reporter;
	}

	@ProvidesIntoSet
	public ServerEntry.ShutdownHook provideExecutorShutdownHook(@Nullable final InfluxDBReporter influxDBReporter) {
		return new ServerEntry.ShutdownHook() {

			@Override
			public Order order() {
				return ServerEntry.ShutdownHook.Order.RESOURCE;
			}
			
			@Override
			public void execute() {
				if (influxDBReporter != null) {
					influxDBReporter.stop();
				}
			}
			
		};
	}
}
