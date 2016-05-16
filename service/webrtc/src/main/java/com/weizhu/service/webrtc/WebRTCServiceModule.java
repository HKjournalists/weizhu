package com.weizhu.service.webrtc;

import java.util.concurrent.Executor;

import javax.annotation.Nullable;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.weizhu.common.influxdb.InfluxDBReporter;
import com.weizhu.common.service.ServiceInvoker;
import com.weizhu.common.service.ServiceStub;
import com.weizhu.proto.WebRTCService;

public class WebRTCServiceModule extends AbstractModule {

	@Override
	protected void configure() {
		bind(WebRTCServiceImpl.class).in(Singleton.class);
	}

	@Provides
	@Singleton
	public WebRTCService provideWebRTCService(WebRTCServiceImpl serviceImpl, @Named("service_executor") Executor serviceExecutor) {
		return ServiceStub.createServiceApi(WebRTCService.class, serviceImpl, serviceExecutor);
	}

	@Provides
	@Singleton
	@Named("WebRTCService")
	public ServiceInvoker provideWebRTCServiceInvoker(WebRTCServiceImpl serviceImpl, @Named("service_executor") Executor serviceExecutor, @Nullable InfluxDBReporter influxDBReporter) {
		return ServiceStub.createServiceInvoker(WebRTCService.class, serviceImpl, serviceExecutor, influxDBReporter);
	}

}
