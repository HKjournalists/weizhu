package com.weizhu.service.offline_training;

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
import com.weizhu.proto.AdminOfflineTrainingService;
import com.weizhu.proto.OfflineTrainingService;

public class OfflineTrainingServiceModule extends AbstractModule {
	
	@Override
	protected void configure() {
		bind(OfflineTrainingServiceImpl.class).in(Singleton.class);
		bind(AdminOfflineTrainingServiceImpl.class).in(Singleton.class);
		
		Multibinder<String> createTableSQLBinder = Multibinder.newSetBinder(binder(), String.class, Names.named("db_create_table.sql"));
		createTableSQLBinder.addBinding().toInstance("com/weizhu/service/offline_training/db_create_table.sql");
		
		Multibinder<String> dropTableSQLBinder = Multibinder.newSetBinder(binder(), String.class, Names.named("db_drop_table.sql"));
		dropTableSQLBinder.addBinding().toInstance("com/weizhu/service/offline_training/db_drop_table.sql");
	}
	
	@Provides
	@Singleton
	public OfflineTrainingService provideOfflineTrainingService(OfflineTrainingServiceImpl serviceImpl, @Named("service_executor") Executor serviceExecutor) {
		return ServiceStub.createServiceApi(OfflineTrainingService.class, serviceImpl, serviceExecutor);
	}
	
	@Provides
	@Singleton
	@Named("OfflineTrainingService")
	public ServiceInvoker provideOfflineTrainingServiceInvoker(OfflineTrainingServiceImpl serviceImpl, @Named("service_executor") Executor serviceExecutor, @Nullable InfluxDBReporter influxDBReporter) {
		return ServiceStub.createServiceInvoker(OfflineTrainingService.class, serviceImpl, serviceExecutor, influxDBReporter);
	}
	
	@Provides
	@Singleton
	public AdminOfflineTrainingService provideAdminOfflineTrainingService(AdminOfflineTrainingServiceImpl serviceImpl, @Named("service_executor") Executor serviceExecutor) {
		return ServiceStub.createServiceApi(AdminOfflineTrainingService.class, serviceImpl, serviceExecutor);
	}
	
	@Provides
	@Singleton
	@Named("AdminOfflineTrainingService")
	public ServiceInvoker provideAdminOfflineTrainingServiceInvoker(AdminOfflineTrainingServiceImpl serviceImpl, @Named("service_executor") Executor serviceExecutor, @Nullable InfluxDBReporter influxDBReporter) {
		return ServiceStub.createServiceInvoker(AdminOfflineTrainingService.class, serviceImpl, serviceExecutor, influxDBReporter);
	}
	
}
