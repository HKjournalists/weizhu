package com.weizhu.service.user;

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
import com.weizhu.proto.AdminUserService;
import com.weizhu.proto.UserService;
import com.weizhu.service.user.abilitytag.AbilityTagManager;
import com.weizhu.service.user.base.UserBaseManager;
import com.weizhu.service.user.experience.ExperienceManager;
import com.weizhu.service.user.level.LevelManager;
import com.weizhu.service.user.mark.UserMarkManager;
import com.weizhu.service.user.position.PositionManager;
import com.weizhu.service.user.team.TeamManager;

public class UserServiceModule extends AbstractModule {

	@Override
	protected void configure() {
		bind(ImportUserManager.class);
		bind(TeamManager.class);
		bind(PositionManager.class);
		bind(UserBaseManager.class);
		bind(UserMarkManager.class);
		bind(LevelManager.class);
		bind(ExperienceManager.class);
		bind(AbilityTagManager.class);
		
		bind(UserServiceImpl.class).in(Singleton.class);
		bind(AdminUserServiceImpl.class).in(Singleton.class);
		
		Multibinder<String> createTableSQLBinder = Multibinder.newSetBinder(binder(), String.class, Names.named("db_create_table.sql"));
		createTableSQLBinder.addBinding().toInstance("com/weizhu/service/user/db_create_table.sql");
		
		Multibinder<String> dropTableSQLBinder = Multibinder.newSetBinder(binder(), String.class, Names.named("db_drop_table.sql"));
		dropTableSQLBinder.addBinding().toInstance("com/weizhu/service/user/db_drop_table.sql");
	}
	
	@Provides
	@Singleton
	public UserService provideUserService(UserServiceImpl serviceImpl, @Named("service_executor") Executor serviceExecutor) {
		return ServiceStub.createServiceApi(UserService.class, serviceImpl, serviceExecutor);
	}

	@Provides
	@Singleton
	@Named("UserService")
	public ServiceInvoker provideUserServiceInvoker(UserServiceImpl serviceImpl, @Named("service_executor") Executor serviceExecutor, @Nullable InfluxDBReporter influxDBReporter) {
		return ServiceStub.createServiceInvoker(UserService.class, serviceImpl, serviceExecutor, influxDBReporter);
	}
	
	@Provides
	@Singleton
	public AdminUserService provideAdminUserService(AdminUserServiceImpl serviceImpl, @Named("service_executor") Executor serviceExecutor) {
		return ServiceStub.createServiceApi(AdminUserService.class, serviceImpl, serviceExecutor);
	}

	@Provides
	@Singleton
	@Named("AdminUserService")
	public ServiceInvoker provideAdminUserServiceInvoker(AdminUserServiceImpl serviceImpl, @Named("service_executor") Executor serviceExecutor, @Nullable InfluxDBReporter influxDBReporter) {
		return ServiceStub.createServiceInvoker(AdminUserService.class, serviceImpl, serviceExecutor, influxDBReporter);
	}
}
