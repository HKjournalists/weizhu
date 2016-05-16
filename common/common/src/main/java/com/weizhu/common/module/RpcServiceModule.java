package com.weizhu.common.module;

import java.util.concurrent.Executor;

import javax.annotation.Nullable;

import com.google.inject.Key;
import com.google.inject.PrivateModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.google.inject.name.Names;
import com.weizhu.common.influxdb.InfluxDBReporter;
import com.weizhu.common.rpc.RpcInvoker;
import com.weizhu.common.service.ServiceInvoker;
import com.weizhu.common.service.ServiceStub;

public class RpcServiceModule extends PrivateModule {

	private final Class<?> serviceApi;
	private final String rpcServerName;
	
	public RpcServiceModule(Class<?> serviceApi, String rpcServerName) {
		this.serviceApi = serviceApi;
		this.rpcServerName = rpcServerName;
	}
	
	@SuppressWarnings("unchecked")
	@Override
	protected void configure() {
		
		bind(RpcInvoker.class).annotatedWith(Names.named("_internal_server_client"))
			.to(Key.get(RpcInvoker.class, Names.named(rpcServerName)))
			.in(Singleton.class);
		
		bind(ServiceInvoker.class).annotatedWith(Names.named(serviceApi.getSimpleName()))
			.to(Key.get(ServiceInvoker.class, Names.named("_internal_service_invoker")))
			.in(Singleton.class);
		
		bind((Class<Object>) serviceApi).to(Key.get(Object.class, Names.named("_internal_service")))
			.in(Singleton.class);
		
		expose(ServiceInvoker.class).annotatedWith(Names.named(serviceApi.getSimpleName()));
		expose(serviceApi);
	}
	
	@Provides
	@Singleton
	@Named("_internal_service_invoker")
	public ServiceInvoker provideInternalServiceInvoker(
			@Named("_internal_server_client") RpcInvoker internalServerClient, 
			@Named("service_executor") Executor serviceExecutor, 
			@Nullable InfluxDBReporter influxDBReporter) {
		return ServiceStub.createServiceInvoker(serviceApi.getSimpleName(), internalServerClient, serviceExecutor, influxDBReporter);
	}
	
	@Provides
	@Singleton
	@Named("_internal_service")
	public Object provideCompanyService(
			@Named("_internal_service_invoker") ServiceInvoker internalServiceInvoker) {
		return ServiceStub.createServiceApi(serviceApi, internalServiceInvoker);
	}

}
