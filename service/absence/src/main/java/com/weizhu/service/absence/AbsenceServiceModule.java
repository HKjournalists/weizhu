package com.weizhu.service.absence;

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
import com.weizhu.proto.AbsenceService;

public class AbsenceServiceModule extends AbstractModule {

	@Override
	protected void configure() {
		bind(AbsenceServiceImpl.class).in(Singleton.class);
		
		Multibinder<String> createTableSQLBinder = Multibinder.newSetBinder(binder(), String.class, Names.named("db_create_table.sql"));
		createTableSQLBinder.addBinding().toInstance("com/weizhu/service/absence/db_create_table.sql");
		
		Multibinder<String> dropTableSQLBinder = Multibinder.newSetBinder(binder(), String.class, Names.named("db_drop_table.sql"));
		dropTableSQLBinder.addBinding().toInstance("com/weizhu/service/absence/db_drop_table.sql");
	}
	
	@Provides
	@Singleton
	public AbsenceService provideAbsenceService(AbsenceServiceImpl serviceImpl, @Named("service_executor") Executor serviceExecutor) {
		return ServiceStub.createServiceApi(AbsenceService.class, serviceImpl, serviceExecutor);
	}
	
	@Provides
	@Singleton
	@Named("AbsenceService")
	public ServiceInvoker provideAbsenceServiceInvoker(AbsenceServiceImpl serviceImpl, @Named("service_executor") Executor serviceExecutor, @Nullable InfluxDBReporter influxDBReporter) {
		return ServiceStub.createServiceInvoker(AbsenceService.class, serviceImpl, serviceExecutor, influxDBReporter);
	}
	
}
