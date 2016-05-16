package com.weizhu.service.boss;

import java.util.Properties;
import java.util.concurrent.Executor;

import javax.annotation.Nullable;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.multibindings.Multibinder;
import com.google.inject.name.Named;
import com.google.inject.name.Names;
import com.weizhu.common.influxdb.InfluxDBReporter;
import com.weizhu.common.service.ServiceInvoker;
import com.weizhu.common.service.ServiceStub;
import com.weizhu.proto.BossService;

public class BossServiceModule extends AbstractModule {

	@Override
	protected void configure() {
		bind(BossServiceImpl.class).in(Singleton.class);

		Multibinder<String> createTableSQLBinder = Multibinder.newSetBinder(binder(), String.class, Names.named("db_create_table.sql"));
		createTableSQLBinder.addBinding().toInstance("com/weizhu/service/boss/db_create_table.sql");

		Multibinder<String> dropTableSQLBinder = Multibinder.newSetBinder(binder(), String.class, Names.named("db_drop_table.sql"));
		dropTableSQLBinder.addBinding().toInstance("com/weizhu/service/boss/db_drop_table.sql");
	}
	
	@Provides
	@Named("boss_session_secret_key")
	public String provideBossSessionSecretKey(@Named("server_conf") Properties confProperties) {
		return confProperties.getProperty("boss_session_secret_key");
	}
	
	@Provides
	@Named("boss_password_salt")
	public String provideBossPasswordSalt(@Named("server_conf") Properties confProperties) {
		return confProperties.getProperty("boss_password_salt");
	}

	@Provides
	@Singleton
	public BossService provideBossService(BossServiceImpl serviceImpl, @Named("service_executor") Executor serviceExecutor) {
		return ServiceStub.createServiceApi(BossService.class, serviceImpl, serviceExecutor);
	}

	@Provides
	@Singleton
	@Named("BossService")
	public ServiceInvoker provideBossServiceInvoker(BossServiceImpl serviceImpl, @Named("service_executor") Executor serviceExecutor, @Nullable InfluxDBReporter influxDBReporter) {
		return ServiceStub.createServiceInvoker(BossService.class, serviceImpl, serviceExecutor, influxDBReporter);
	}

}
