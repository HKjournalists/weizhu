package com.weizhu.service.admin;

import java.util.Properties;
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
import com.weizhu.proto.AdminService;

public class AdminServiceModule extends AbstractModule {
	
	@Override
	protected void configure() {
		
		bind(AdminServiceImpl.class).in(Singleton.class);
		
		Multibinder<String> createTableSQLBinder = Multibinder.newSetBinder(binder(), String.class, Names.named("db_create_table.sql"));
		createTableSQLBinder.addBinding().toInstance("com/weizhu/service/admin/db_create_table.sql");
		
		Multibinder<String> dropTableSQLBinder = Multibinder.newSetBinder(binder(), String.class, Names.named("db_drop_table.sql"));
		dropTableSQLBinder.addBinding().toInstance("com/weizhu/service/admin/db_drop_table.sql");
	}
	
	@Provides
	@Named("admin_session_secret_key")
	public String provideAdminSessionSecretKey(@Named("server_conf") Properties confProperties) {
		return confProperties.getProperty("admin_session_secret_key");
	}
	
	@Provides
	@Named("admin_password_salt")
	public String provideAdminPasswordSaltKey(@Named("server_conf") Properties confProperties) {
		return confProperties.getProperty("admin_password_salt");
	}
	
	@Provides
	@Singleton
	public AdminService provideAdminService(AdminServiceImpl serviceImpl, @Named("service_executor") Executor serviceExecutor) {
		return ServiceStub.createServiceApi(AdminService.class, serviceImpl, serviceExecutor);
	}

	@Provides
	@Singleton
	@Named("AdminService")
	public ServiceInvoker provideAdminServiceInvoker(AdminServiceImpl serviceImpl, @Named("service_executor") Executor serviceExecutor, @Nullable InfluxDBReporter influxDBReporter) {
		return ServiceStub.createServiceInvoker(AdminService.class, serviceImpl, serviceExecutor, influxDBReporter);
	}

}
