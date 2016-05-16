package com.weizhu.service.company;

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
import com.weizhu.proto.CompanyService;

public class CompanyServiceProxyModule extends AbstractModule {

	@Override
	protected void configure() {
		bind(CompanyServiceProxyImpl.class).in(Singleton.class);
		
		Multibinder<String> createTableSQLBinder = Multibinder.newSetBinder(binder(), String.class, Names.named("db_create_table.sql"));
		createTableSQLBinder.addBinding().toInstance("com/weizhu/service/company/db_create_table.sql");
		
		Multibinder<String> dropTableSQLBinder = Multibinder.newSetBinder(binder(), String.class, Names.named("db_drop_table.sql"));
		dropTableSQLBinder.addBinding().toInstance("com/weizhu/service/company/db_drop_table.sql");
	}

	@Provides
	@Singleton
	public CompanyService provideCompanyService(CompanyServiceProxyImpl serviceImpl, @Named("service_executor") Executor serviceExecutor) {
		return ServiceStub.createServiceApi(CompanyService.class, serviceImpl, serviceExecutor);
	}

	@Provides
	@Singleton
	@Named("CompanyService")
	public ServiceInvoker provideCompanyServiceInvoker(CompanyServiceProxyImpl serviceImpl, @Named("service_executor") Executor serviceExecutor, @Nullable InfluxDBReporter influxDBReporter) {
		return ServiceStub.createServiceInvoker(CompanyService.class, serviceImpl, serviceExecutor, influxDBReporter);
	}
	
}
