package com.weizhu.service.push;

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
import com.weizhu.proto.PushPollingService;
import com.weizhu.proto.PushService;

public class PushServiceModule extends AbstractModule {

	@Override
	protected void configure() {
		bind(PushServiceImpl.class).in(Singleton.class);
		
		Multibinder<String> createTableSQLBinder = Multibinder.newSetBinder(binder(), String.class, Names.named("db_create_table.sql"));
		createTableSQLBinder.addBinding().toInstance("com/weizhu/service/push/db_create_table.sql");
		
		Multibinder<String> dropTableSQLBinder = Multibinder.newSetBinder(binder(), String.class, Names.named("db_drop_table.sql"));
		dropTableSQLBinder.addBinding().toInstance("com/weizhu/service/push/db_drop_table.sql");
	}

	@Provides
	@Singleton
	public PushService providePushService(PushServiceImpl serviceImpl, @Named("service_executor") Executor serviceExecutor) {
		return ServiceStub.createServiceApi(PushService.class, serviceImpl, serviceExecutor);
	}

	@Provides
	@Singleton
	@Named("PushService")
	public ServiceInvoker providePushServiceInvoker(PushServiceImpl serviceImpl, @Named("service_executor") Executor serviceExecutor, @Nullable InfluxDBReporter influxDBReporter) {
		return ServiceStub.createServiceInvoker(PushService.class, serviceImpl, serviceExecutor, influxDBReporter);
	}
	
	@Provides
	@Singleton
	public PushPollingService providePushPollingService(PushServiceImpl serviceImpl, @Named("service_executor") Executor serviceExecutor) {
		return ServiceStub.createServiceApi(PushPollingService.class, serviceImpl, serviceExecutor);
	}

	@Provides
	@Singleton
	@Named("PushPollingService")
	public ServiceInvoker providePushPollingServiceInvoker(PushServiceImpl serviceImpl, @Named("service_executor") Executor serviceExecutor, @Nullable InfluxDBReporter influxDBReporter) {
		return ServiceStub.createServiceInvoker(PushPollingService.class, serviceImpl, serviceExecutor, influxDBReporter);
	}
}