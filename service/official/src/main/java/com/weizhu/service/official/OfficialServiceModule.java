package com.weizhu.service.official;

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
import com.weizhu.proto.AdminOfficialService;
import com.weizhu.proto.OfficialService;

public class OfficialServiceModule extends AbstractModule {

	@Override
	protected void configure() {
		bind(OfficialServiceImpl.class).in(Singleton.class);
		
		Multibinder<String> createTableSQLBinder = Multibinder.newSetBinder(binder(), String.class, Names.named("db_create_table.sql"));
		createTableSQLBinder.addBinding().toInstance("com/weizhu/service/official/db_create_table.sql");
		
		Multibinder<String> dropTableSQLBinder = Multibinder.newSetBinder(binder(), String.class, Names.named("db_drop_table.sql"));
		dropTableSQLBinder.addBinding().toInstance("com/weizhu/service/official/db_drop_table.sql");
	}
	
	@Provides
	@Singleton
	public OfficialService provideOfficialService(OfficialServiceImpl serviceImpl, @Named("service_executor") Executor serviceExecutor) {
		return ServiceStub.createServiceApi(OfficialService.class, serviceImpl, serviceExecutor);
	}

	@Provides
	@Singleton
	@Named("OfficialService")
	public ServiceInvoker provideOfficialServiceInvoker(OfficialServiceImpl serviceImpl, @Named("service_executor") Executor serviceExecutor, @Nullable InfluxDBReporter influxDBReporter) {
		return ServiceStub.createServiceInvoker(OfficialService.class, serviceImpl, serviceExecutor, influxDBReporter);
	}
	
	@Provides
	@Singleton
	public AdminOfficialService provideAdminOfficialService(AdminOfficialServiceImpl serviceImpl, @Named("service_executor") Executor serviceExecutor) {
		return ServiceStub.createServiceApi(AdminOfficialService.class, serviceImpl, serviceExecutor);
	}

	@Provides
	@Singleton
	@Named("AdminOfficialService")
	public ServiceInvoker provideAdminOfficialServiceInvoker(AdminOfficialServiceImpl serviceImpl, @Named("service_executor") Executor serviceExecutor, @Nullable InfluxDBReporter influxDBReporter) {
		return ServiceStub.createServiceInvoker(AdminOfficialService.class, serviceImpl, serviceExecutor, influxDBReporter);
	}

}
