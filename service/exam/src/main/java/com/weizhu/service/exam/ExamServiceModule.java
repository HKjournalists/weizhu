package com.weizhu.service.exam;

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
import com.weizhu.proto.AdminExamService;
import com.weizhu.proto.ExamService;

public class ExamServiceModule extends AbstractModule{

	@Override
	protected void configure() {
		bind(ExamServiceImpl.class).in(Singleton.class);
		bind(AdminExamServiceImpl.class).in(Singleton.class);
		
		Multibinder<String> createTableSQLBinder = Multibinder.newSetBinder(binder(), String.class, Names.named("db_create_table.sql"));
		createTableSQLBinder.addBinding().toInstance("com/weizhu/service/exam/db_create_table.sql");
		
		Multibinder<String> dropTableSQLBinder = Multibinder.newSetBinder(binder(), String.class, Names.named("db_drop_table.sql"));
		dropTableSQLBinder.addBinding().toInstance("com/weizhu/service/exam/db_drop_table.sql");
	}
	
	@Provides
	@Singleton
	public ExamService provideExamService(ExamServiceImpl serviceImpl, @Named("service_executor") Executor serviceExecutor) {
		return ServiceStub.createServiceApi(ExamService.class, serviceImpl, serviceExecutor);
	}
	
	@Provides
	@Singleton
	@Named("ExamService")
	public ServiceInvoker provideExamServiceInvoker(ExamServiceImpl serviceImpl, @Named("service_executor") Executor serviceExecutor, @Nullable InfluxDBReporter influxDBReporter) {
		return ServiceStub.createServiceInvoker(ExamService.class, serviceImpl, serviceExecutor, influxDBReporter);
	}
	
	@Provides
	@Singleton
	public AdminExamService provideAdminExamService(AdminExamServiceImpl serviceImpl, @Named("service_executor") Executor serviceExecutor) {
		return ServiceStub.createServiceApi(AdminExamService.class, serviceImpl, serviceExecutor);
	}
	
	@Provides
	@Singleton
	@Named("AdminExamService")
	public ServiceInvoker provideAdminExamServiceInvoker(AdminExamServiceImpl serviceImpl, @Named("service_executor") Executor serviceExecutor, @Nullable InfluxDBReporter influxDBReporter) {
		return ServiceStub.createServiceInvoker(AdminExamService.class, serviceImpl, serviceExecutor, influxDBReporter);
	}
	
}
