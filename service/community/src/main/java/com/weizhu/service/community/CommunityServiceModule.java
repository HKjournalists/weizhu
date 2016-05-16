package com.weizhu.service.community;

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
import com.weizhu.proto.AdminCommunityService;
import com.weizhu.proto.CommunityService;

public class CommunityServiceModule extends AbstractModule {

	@Override
	protected void configure() {

		bind(CommunityServiceImpl.class).in(Singleton.class);
		bind(AdminCommunityServiceImpl.class).in(Singleton.class);

		Multibinder<String> createTableSQLBinder = Multibinder.newSetBinder(binder(), String.class, Names.named("db_create_table.sql"));
		createTableSQLBinder.addBinding().toInstance("com/weizhu/service/community/db_create_table.sql");

		Multibinder<String> dropTableSQLBinder = Multibinder.newSetBinder(binder(), String.class, Names.named("db_drop_table.sql"));
		dropTableSQLBinder.addBinding().toInstance("com/weizhu/service/community/db_drop_table.sql");
	}

	@Provides
	@Singleton
	public CommunityService provideCommunityService(CommunityServiceImpl serviceImpl, @Named("service_executor") Executor serviceExecutor) {
		return ServiceStub.createServiceApi(CommunityService.class, serviceImpl, serviceExecutor);
	}

	@Provides
	@Singleton
	@Named("CommunityService")
	public ServiceInvoker provideCommunityServiceInvoker(CommunityServiceImpl serviceImpl, @Named("service_executor") Executor serviceExecutor, @Nullable InfluxDBReporter influxDBReporter) {
		return ServiceStub.createServiceInvoker(CommunityService.class, serviceImpl, serviceExecutor, influxDBReporter);
	}

	@Provides
	@Singleton
	public AdminCommunityService provideAdminCommunityService(AdminCommunityServiceImpl serviceImpl, @Named("service_executor") Executor serviceExecutor) {
		return ServiceStub.createServiceApi(AdminCommunityService.class, serviceImpl, serviceExecutor);
	}

	@Provides
	@Singleton
	@Named("AdminCommunityService")
	public ServiceInvoker provideAdminCommunityServiceInvoker(AdminCommunityServiceImpl serviceImpl, @Named("service_executor") Executor serviceExecutor, @Nullable InfluxDBReporter influxDBReporter) {
		return ServiceStub.createServiceInvoker(AdminCommunityService.class, serviceImpl, serviceExecutor, influxDBReporter);
	}
}
