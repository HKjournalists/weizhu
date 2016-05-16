package com.weizhu.service.system;

import java.util.concurrent.Executor;

import javax.annotation.Nullable;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.weizhu.common.influxdb.InfluxDBReporter;
import com.weizhu.common.service.ServiceInvoker;
import com.weizhu.common.service.ServiceStub;
import com.weizhu.proto.SystemService;

public class SystemServiceModule extends AbstractModule {

	@Override
	protected void configure() {
		bind(SystemServiceImpl.class).in(Singleton.class);
	}
	
	@Provides
	@Singleton
	public SystemService provideSystemService(SystemServiceImpl serviceImpl, @Named("service_executor") Executor serviceExecutor) {
		return ServiceStub.createServiceApi(SystemService.class, serviceImpl, serviceExecutor);
	}

	@Provides
	@Singleton
	@Named("SystemService")
	public ServiceInvoker provideSystemServiceInvoker(SystemServiceImpl serviceImpl, @Named("service_executor") Executor serviceExecutor, @Nullable InfluxDBReporter influxDBReporter) {
		return ServiceStub.createServiceInvoker(SystemService.class, serviceImpl, serviceExecutor, influxDBReporter);
	}
}
