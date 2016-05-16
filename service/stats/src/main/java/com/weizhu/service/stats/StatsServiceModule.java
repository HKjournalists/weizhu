package com.weizhu.service.stats;

import java.util.Properties;
import java.util.Set;
import java.util.concurrent.Executor;

import javax.annotation.Nullable;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.multibindings.Multibinder;
import com.google.inject.multibindings.ProvidesIntoSet;
import com.google.inject.name.Named;
import com.google.inject.name.Names;
import com.weizhu.common.influxdb.InfluxDBReporter;
import com.weizhu.common.server.ServerEntry;
import com.weizhu.common.service.ServiceInvoker;
import com.weizhu.common.service.ServiceStub;
import com.weizhu.proto.StatsService;
import com.weizhu.service.stats.fact.InsertFactAdminActionHandler;
import com.weizhu.service.stats.fact.InsertFactHandler;
import com.weizhu.service.stats.fact.InsertFactThread;
import com.weizhu.service.stats.fact.InsertFactUserAccessHandler;
import com.weizhu.service.stats.fact.InsertFactUserActionHandler;
import com.weizhu.service.stats.fact.InsertFactUserDiscoverHandler;
import com.weizhu.service.stats.fact.InsertFactUserLoginHandler;
import com.weizhu.service.stats.fact.InsertFactWeizhuVersionHandler;
import com.zaxxer.hikari.HikariDataSource;

public class StatsServiceModule extends AbstractModule {

	@Override
	protected void configure() {
		bind(StatsServiceImpl.class).in(Singleton.class);
		
		Multibinder<String> createTableSQLBinder = Multibinder.newSetBinder(binder(), String.class, Names.named("db_create_table.sql"));
		createTableSQLBinder.addBinding().toInstance("com/weizhu/service/stats/db_create_table.sql");
		
		Multibinder<String> dropTableSQLBinder = Multibinder.newSetBinder(binder(), String.class, Names.named("db_drop_table.sql"));
		dropTableSQLBinder.addBinding().toInstance("com/weizhu/service/stats/db_drop_table.sql");
		
		Multibinder<InsertFactHandler> insertFactHandlerBinder = Multibinder.newSetBinder(binder(), InsertFactHandler.class);
		insertFactHandlerBinder.addBinding().to(InsertFactWeizhuVersionHandler.class);
		insertFactHandlerBinder.addBinding().to(InsertFactAdminActionHandler.class);
		insertFactHandlerBinder.addBinding().to(InsertFactUserActionHandler.class);
		insertFactHandlerBinder.addBinding().to(InsertFactUserAccessHandler.class);
		insertFactHandlerBinder.addBinding().to(InsertFactUserDiscoverHandler.class);
		insertFactHandlerBinder.addBinding().to(InsertFactUserLoginHandler.class);
	}
	
	@Provides
	@Singleton
	public StatsService provideStatsService(StatsServiceImpl serviceImpl, @Named("service_executor") Executor serviceExecutor) {
		return ServiceStub.createServiceApi(StatsService.class, serviceImpl, serviceExecutor);
	}

	@Provides
	@Singleton
	@Named("StatsService")
	public ServiceInvoker provideStatsServiceInvoker(StatsServiceImpl serviceImpl, @Named("service_executor") Executor serviceExecutor, @Nullable InfluxDBReporter influxDBReporter) {
		return ServiceStub.createServiceInvoker(StatsService.class, serviceImpl, serviceExecutor, influxDBReporter);
	}
	
	@Provides
	@Singleton
	@Named("stats_kafka_server")
	public String provideStatsKafkaServer(@Named("server_conf") Properties confProperties) {
		return confProperties.getProperty("stats_kafka_server");
	}

	@Provides
	@Singleton
	public ImmutableList<InsertFactThread> provideFactWeizhuVersion(
			HikariDataSource hikariDataSource, 
			@Named("stats_kafka_server") @Nullable String kafkaServer, 
			Set<InsertFactHandler> insertFactHandlerSet
			) {
		if (Strings.isNullOrEmpty(kafkaServer)) {
			return ImmutableList.of();
		}
		
		ImmutableList.Builder<InsertFactThread> listBuilder = ImmutableList.builder();
		for (InsertFactHandler handler : insertFactHandlerSet) {
			listBuilder.add(new InsertFactThread(hikariDataSource, kafkaServer, handler));
		}
		return listBuilder.build();
	}
	
	@ProvidesIntoSet
	public ServerEntry.StartHook provideInsertFactThreadStartHook(final ImmutableList<InsertFactThread> insertFactThreadList) {
		return new ServerEntry.StartHook() {

			@Override
			public void execute() {
				for (InsertFactThread thread : insertFactThreadList) {
					thread.start();
				}
			}
			
		};
	}
	
	@ProvidesIntoSet
	public ServerEntry.ShutdownHook provideInsertFactThreadShutdownHook(final ImmutableList<InsertFactThread> insertFactThreadList) {
		return new ServerEntry.ShutdownHook() {

			@Override
			public ServerEntry.ShutdownHook.Order order() {
				return ServerEntry.ShutdownHook.Order.SERVER;
			}
			
			@Override
			public void execute() {
				for (InsertFactThread thread : insertFactThreadList) {
					thread.shutdown();
				}
				
				for (InsertFactThread thread : insertFactThreadList) {
					try {
						thread.join();
					} catch (InterruptedException e) {
						// ignore
					}
				}
			}
			
		};
	}

}
