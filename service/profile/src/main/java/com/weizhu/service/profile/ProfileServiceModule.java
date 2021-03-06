package com.weizhu.service.profile;

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
import com.weizhu.proto.ProfileService;

public class ProfileServiceModule extends AbstractModule {

	@Override
	protected void configure() {
		bind(ProfileServiceImpl.class).in(Singleton.class);
		
		Multibinder<String> createTableSQLBinder = Multibinder.newSetBinder(binder(), String.class, Names.named("db_create_table.sql"));
		createTableSQLBinder.addBinding().toInstance("com/weizhu/service/profile/db_create_table.sql");
		
		Multibinder<String> dropTableSQLBinder = Multibinder.newSetBinder(binder(), String.class, Names.named("db_drop_table.sql"));
		dropTableSQLBinder.addBinding().toInstance("com/weizhu/service/profile/db_drop_table.sql");
	}

	@Provides
	@Singleton
	public ProfileService provideSurveyService(ProfileServiceImpl serviceImpl, @Named("service_executor") Executor serviceExecutor) {
		return ServiceStub.createServiceApi(ProfileService.class, serviceImpl, serviceExecutor);
	}
	
	@Provides
	@Singleton
	@Named("ProfileService")
	public ServiceInvoker provideSurveyServiceInvoker(ProfileServiceImpl serviceImpl, @Named("service_executor") Executor serviceExecutor, @Nullable InfluxDBReporter influxDBReporter) {
		return ServiceStub.createServiceInvoker(ProfileService.class, serviceImpl, serviceExecutor, influxDBReporter);
	}
	
}