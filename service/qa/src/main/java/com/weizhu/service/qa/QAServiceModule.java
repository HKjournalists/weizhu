package com.weizhu.service.qa;

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
import com.weizhu.proto.AdminQAService;
import com.weizhu.proto.QAService;

public class QAServiceModule extends AbstractModule {

	@Override
	protected void configure() {
		bind(QAServiceImpl.class).in(Singleton.class);
		bind(AdminQAServiceImpl.class).in(Singleton.class);

		Multibinder<String> createTableSQLBinder = Multibinder.newSetBinder(binder(), String.class, Names.named("db_create_table.sql"));
		createTableSQLBinder.addBinding().toInstance("com/weizhu/service/qa/db_create_table.sql");

		Multibinder<String> dropTableSQLBinder = Multibinder.newSetBinder(binder(), String.class, Names.named("db_drop_table.sql"));
		dropTableSQLBinder.addBinding().toInstance("com/weizhu/service/qa/db_drop_table.sql");
	}

	@Provides
	@Singleton
	public QAService provideQAService(QAServiceImpl serviceImpl, @Named("service_executor") Executor serviceExecutor) {
		return ServiceStub.createServiceApi(QAService.class, serviceImpl, serviceExecutor);
	}

	@Provides
	@Singleton
	@Named("QAService")
	public ServiceInvoker provideQAServiceInvoker(QAServiceImpl serviceImpl, @Named("service_executor") Executor serviceExecutor, @Nullable InfluxDBReporter influxDBReporter) {
		return ServiceStub.createServiceInvoker(QAService.class, serviceImpl, serviceExecutor, influxDBReporter);
	}

	@Provides
	@Singleton
	public AdminQAService provideAdminQAService(AdminQAServiceImpl serviceImpl, @Named("service_executor") Executor serviceExecutor) {
		return ServiceStub.createServiceApi(AdminQAService.class, serviceImpl, serviceExecutor);
	}

	@Provides
	@Singleton
	@Named("AdminQAService")
	public ServiceInvoker provideAdminQAServiceInvoker(AdminQAServiceImpl serviceImpl, @Named("service_executor") Executor serviceExecutor, @Nullable InfluxDBReporter influxDBReporter) {
		return ServiceStub.createServiceInvoker(AdminQAService.class, serviceImpl, serviceExecutor, influxDBReporter);
	}
}
