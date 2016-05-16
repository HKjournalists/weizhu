package com.weizhu.service.upload;

import java.util.concurrent.Executor;

import javax.annotation.Nullable;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.multibindings.Multibinder;
import com.google.inject.name.Named;
import com.google.inject.name.Names;
import com.qiniu.storage.UploadManager;
import com.weizhu.common.influxdb.InfluxDBReporter;
import com.weizhu.common.service.ServiceInvoker;
import com.weizhu.common.service.ServiceStub;
import com.weizhu.proto.UploadService;

public class UploadServiceModule  extends AbstractModule{

	@Override
	protected void configure() {
		bind(UploadServiceImpl.class).in(Singleton.class);
		
		Multibinder<String> createTableSQLBinder = Multibinder.newSetBinder(binder(), String.class, Names.named("db_create_table.sql"));
		createTableSQLBinder.addBinding().toInstance("com/weizhu/service/upload/db_create_table.sql");
		
		Multibinder<String> dropTableSQLBinder = Multibinder.newSetBinder(binder(), String.class, Names.named("db_drop_table.sql"));
		dropTableSQLBinder.addBinding().toInstance("com/weizhu/service/upload/db_drop_table.sql");
		
		bind(UploadManager.class).toInstance(new UploadManager());
	}
	
	@Provides
	@Singleton
	public UploadService provideUploadService(UploadServiceImpl serviceImpl, @Named("service_executor") Executor serviceExecutor) {
		return ServiceStub.createServiceApi(UploadService.class, serviceImpl, serviceExecutor);
	}
	
	@Provides
	@Singleton
	@Named("UploadService")
	public ServiceInvoker provideUploadServiceInvoker(UploadServiceImpl serviceImpl, @Named("service_executor") Executor serviceExecutor, @Nullable InfluxDBReporter influxDBReporter) {
		return ServiceStub.createServiceInvoker(UploadService.class, serviceImpl, serviceExecutor, influxDBReporter);
	}
	
}
