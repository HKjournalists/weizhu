package com.weizhu.service.discover;

import java.util.Properties;
import java.util.concurrent.Executor;

import javax.annotation.Nullable;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.multibindings.Multibinder;
import com.google.inject.name.Named;
import com.google.inject.name.Names;
import com.weizhu.common.config.ConfigUtil;
import com.weizhu.common.influxdb.InfluxDBReporter;
import com.weizhu.common.service.ServiceInvoker;
import com.weizhu.common.service.ServiceStub;
import com.weizhu.proto.AdminDiscoverService;
import com.weizhu.proto.DiscoverService;
import com.weizhu.proto.DiscoverV2Service;
import com.weizhu.service.discover_v2.AdminDiscoverServiceImpl;
import com.weizhu.service.discover_v2.DiscoverV2ServiceImpl;

public class DiscoverServiceModule extends AbstractModule {

	@Override
	protected void configure() {
		bind(DiscoverV2ServiceImpl.class).in(Singleton.class);
		bind(DiscoverServiceImpl2.class).in(Singleton.class);
		bind(DiscoverExtends.class).in(Singleton.class);
		
		Multibinder<String> createTableSQLBinder = Multibinder.newSetBinder(binder(), String.class, Names.named("db_create_table.sql"));
		createTableSQLBinder.addBinding().toInstance("com/weizhu/service/discover_v2/db_create_table.sql");
		
		Multibinder<String> dropTableSQLBinder = Multibinder.newSetBinder(binder(), String.class, Names.named("db_drop_table.sql"));
		dropTableSQLBinder.addBinding().toInstance("com/weizhu/service/discover_v2/db_drop_table.sql");
	}
	
	@Provides
	@Singleton
	public DiscoverV2Service provideDiscoverV2Service(DiscoverV2ServiceImpl serviceImpl, @Named("service_executor") Executor serviceExecutor) {
		return ServiceStub.createServiceApi(DiscoverV2Service.class, serviceImpl, serviceExecutor);
	}

	@Provides
	@Singleton
	@Named("DiscoverV2Service")
	public ServiceInvoker provideDiscoverV2ServiceInvoker(DiscoverV2ServiceImpl serviceImpl, @Named("service_executor") Executor serviceExecutor, @Nullable InfluxDBReporter influxDBReporter) {
		return ServiceStub.createServiceInvoker(DiscoverV2Service.class, serviceImpl, serviceExecutor, influxDBReporter);
	}
	
	@Provides
	@Singleton
	public AdminDiscoverService provideAdminDiscoverService(AdminDiscoverServiceImpl serviceImpl, @Named("service_executor") Executor serviceExecutor) {
		return ServiceStub.createServiceApi(AdminDiscoverService.class, serviceImpl, serviceExecutor);
	}

	@Provides
	@Singleton
	@Named("AdminDiscoverService")
	public ServiceInvoker provideAdminDiscoverServiceInvoker(AdminDiscoverServiceImpl serviceImpl, @Named("service_executor") Executor serviceExecutor, @Nullable InfluxDBReporter influxDBReporter) {
		return ServiceStub.createServiceInvoker(AdminDiscoverService.class, serviceImpl, serviceExecutor, influxDBReporter);
	}
	
	// 以下代码废弃

	@Provides
	@Singleton
	public DiscoverService provideDiscoverService(DiscoverServiceImpl2 serviceImpl, @Named("service_executor") Executor serviceExecutor) {
		return ServiceStub.createServiceApi(DiscoverService.class, serviceImpl, serviceExecutor);
	}

	@Provides
	@Singleton
	@Named("DiscoverService")
	public ServiceInvoker provideDiscoverServiceInvoker(DiscoverServiceImpl2 serviceImpl, @Named("service_executor") Executor serviceExecutor, @Nullable InfluxDBReporter influxDBReporter) {
		return ServiceStub.createServiceInvoker(DiscoverService.class, serviceImpl, serviceExecutor, influxDBReporter);
	}
	
	@Provides
	@Named("discover_extends_exam_module_id")
	public int provideDiscoverExtendsExamModuleId(@Named("server_conf") Properties confProperties) {
		Integer value = ConfigUtil.getNullOrInt(confProperties, "discover_extends_exam_module_id");
		return value == null ? -1 : value;
	}
	
	@Provides
	@Named("discover_extends_exam_item_icon_name")
	public String provideDiscoverExtendsExamItemIconName(@Named("server_conf") Properties confProperties) {
		return ConfigUtil.getNullToEmpty(confProperties, "discover_extends_exam_item_icon_name");
	}
}
