package com.weizhu.service.tools.productcolock;

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
import com.weizhu.proto.ToolsProductclockService;

public class ToolsProductclockServiceModule extends AbstractModule {
	
	@Override
	protected void configure() {
		bind(ToolsProductclockServiceImpl.class).in(Singleton.class);
		
		Multibinder<String> createTableSQLBinder = Multibinder.newSetBinder(binder(), String.class, Names.named("db_create_table.sql"));
		createTableSQLBinder.addBinding().toInstance("com/weizhu/service/tools/productclock/db_create_table.sql");
		
		Multibinder<String> dropTableSQLBinder = Multibinder.newSetBinder(binder(), String.class, Names.named("db_drop_table.sql"));
		dropTableSQLBinder.addBinding().toInstance("com/weizhu/service/tools/productclock/db_drop_table.sql");
	}

	@Provides
	@Singleton
	public ToolsProductclockService provideToolsProductclockService(ToolsProductclockServiceImpl serviceImpl, @Named("service_executor") Executor serviceExecutor) {
		return ServiceStub.createServiceApi(ToolsProductclockService.class, serviceImpl, serviceExecutor);
	}
	
	@Provides
	@Singleton
	@Named("ToolsProductclockService")
	public ServiceInvoker provideToolsProductclockServiceInvoker(ToolsProductclockServiceImpl serviceImpl, @Named("service_executor") Executor serviceExecutor, @Nullable InfluxDBReporter influxDBReporter) {
		return ServiceStub.createServiceInvoker(ToolsProductclockService.class, serviceImpl, serviceExecutor, influxDBReporter);
	}
}
