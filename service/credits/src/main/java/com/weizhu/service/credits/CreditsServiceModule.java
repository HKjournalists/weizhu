package com.weizhu.service.credits;

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
import com.weizhu.proto.AdminCreditsService;
import com.weizhu.proto.CreditsService;

public class CreditsServiceModule extends AbstractModule {
	@Override
	protected void configure() {
		bind(CreditsServiceImpl.class).in(Singleton.class);
		bind(AdminCreditsServiceImpl.class).in(Singleton.class);
		
		Multibinder<String> createTableSQLBinder = Multibinder.newSetBinder(binder(), String.class, Names.named("db_create_table.sql"));
		createTableSQLBinder.addBinding().toInstance("com/weizhu/service/credits/db_create_table.sql");
		
		Multibinder<String> dropTableSQLBinder = Multibinder.newSetBinder(binder(), String.class, Names.named("db_drop_table.sql"));
		dropTableSQLBinder.addBinding().toInstance("com/weizhu/service/credits/db_drop_table.sql");
	}
	
	@Provides
	@Singleton
	public CreditsService provideCreditsService(CreditsServiceImpl serviceImpl, @Named("service_executor") Executor serviceExecutor) {
		return ServiceStub.createServiceApi(CreditsService.class, serviceImpl, serviceExecutor);
	}
	
	@Provides
	@Singleton
	@Named("CreditsService")
	public ServiceInvoker provideCreditsServiceInvoker(CreditsServiceImpl serviceImpl, @Named("service_executor") Executor serviceExecutor, @Nullable InfluxDBReporter influxDBReporter) {
		return ServiceStub.createServiceInvoker(CreditsService.class, serviceImpl, serviceExecutor, influxDBReporter);
	}
	
	@Provides
	@Singleton
	public AdminCreditsService provideAdminCreditsService(AdminCreditsServiceImpl serviceImpl, @Named("service_executor") Executor serviceExecutor) {
		return ServiceStub.createServiceApi(AdminCreditsService.class, serviceImpl, serviceExecutor);
	}
	
	@Provides
	@Singleton
	@Named("AdminCreditsService")
	public ServiceInvoker provideAdminCreditsServiceInvoker(AdminCreditsServiceImpl serviceImpl, @Named("service_executor") Executor serviceExecutor, @Nullable InfluxDBReporter influxDBReporter) {
		return ServiceStub.createServiceInvoker(AdminCreditsService.class, serviceImpl, serviceExecutor, influxDBReporter);
	}
}
