package com.weizhu.service.component;

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
import com.weizhu.proto.AdminComponentService;
import com.weizhu.proto.ComponentService;

public class ComponentServiceModule extends AbstractModule {

	@Override
	protected void configure() {
		
		bind(ComponentServiceImpl.class).in(Singleton.class);
		bind(AdminComponentServiceImpl.class).in(Singleton.class);
		
		Multibinder<String> createTavleSQLBinder = Multibinder.newSetBinder(binder(), String.class, Names.named("db_create_table.sql"));
		createTavleSQLBinder.addBinding().toInstance("com/weizhu/service/component/db_create_table.sql");
		
		Multibinder<String> dropTableSQLBinder = Multibinder.newSetBinder(binder(), String.class, Names.named("db_drop_table.sql"));
		dropTableSQLBinder.addBinding().toInstance("com/weizhu/service/component/db_drop_table.sql");
	}
	
	@Provides
	@Singleton
	public ComponentService provideComponentService(ComponentServiceImpl serviceImpl,@Named("service_executor") Executor serviceExecutor ){
		return ServiceStub.createServiceApi(ComponentService.class, serviceImpl, serviceExecutor);
	}

	@Provides
	@Singleton
	@Named("ComponentService")
	public ServiceInvoker provideComponentServiceInvoker(ComponentServiceImpl serviceImpl,@Named("service_executor") Executor serviceExecutor,@Nullable InfluxDBReporter influxDBReporter){
		return ServiceStub.createServiceInvoker(ComponentService.class, serviceImpl, serviceExecutor, influxDBReporter);
	}
	
	@Provides
	@Singleton
	public AdminComponentService proviceAdminComponentService(AdminComponentServiceImpl serviceImpl,@Named("service_executor") Executor serviceExecutor){
		return ServiceStub.createServiceApi(AdminComponentService.class, serviceImpl, serviceExecutor);
	}
	
	@Provides
	@Singleton
	@Named("AdminComponentService")
	public ServiceInvoker provideAdminComponentServiceInvoker(AdminComponentServiceImpl serviceImpl,@Named("service_executor") Executor serviceExecutor,@Nullable InfluxDBReporter influxDBReporter){
		return ServiceStub.createServiceInvoker(AdminComponentService.class, serviceImpl, serviceExecutor, influxDBReporter);
	}
	
}
