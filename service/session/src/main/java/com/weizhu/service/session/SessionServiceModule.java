package com.weizhu.service.session;

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
import com.weizhu.proto.SessionService;

public class SessionServiceModule extends AbstractModule {

	@Override
	protected void configure() {
		bind(SessionServiceImpl.class).in(Singleton.class);
		
		Multibinder<String> createTableSQLBinder = Multibinder.newSetBinder(binder(), String.class, Names.named("db_create_table.sql"));
		createTableSQLBinder.addBinding().toInstance("com/weizhu/service/session/db_create_table.sql");
		
		Multibinder<String> dropTableSQLBinder = Multibinder.newSetBinder(binder(), String.class, Names.named("db_drop_table.sql"));
		dropTableSQLBinder.addBinding().toInstance("com/weizhu/service/session/db_drop_table.sql");
	}
	
	@Provides
	@Named("session_secret_key")
	public byte[] provideSessionSecretKey(@Named("server_conf") Properties confProperties) {
		return confProperties.getProperty("session_secret_key").getBytes();
	}
	
	@Provides
	@Singleton
	public SessionService provideSessionService(SessionServiceImpl serviceImpl, @Named("service_executor") Executor serviceExecutor) {
		return ServiceStub.createServiceApi(SessionService.class, serviceImpl, serviceExecutor);
	}

	@Provides
	@Singleton
	@Named("SessionService")
	public ServiceInvoker provideSessionServiceInvoker(SessionServiceImpl serviceImpl, @Named("service_executor") Executor serviceExecutor, @Nullable InfluxDBReporter influxDBReporter) {
		return ServiceStub.createServiceInvoker(SessionService.class, serviceImpl, serviceExecutor, influxDBReporter);
	}

}