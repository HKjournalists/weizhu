package com.weizhu.server.conn;

import java.net.InetSocketAddress;
import java.util.Properties;
import java.util.concurrent.Executor;

import javax.annotation.Nullable;

import com.google.common.net.HostAndPort;
import com.google.inject.AbstractModule;
import com.google.inject.Key;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.multibindings.Multibinder;
import com.google.inject.multibindings.ProvidesIntoSet;
import com.google.inject.name.Named;
import com.google.inject.name.Names;
import com.weizhu.common.influxdb.InfluxDBReporter;
import com.weizhu.common.server.ServerEntry;
import com.weizhu.common.service.ServiceInvoker;
import com.weizhu.common.service.ServiceStub;
import com.weizhu.proto.ConnService;

public class SocketConnectionModule extends AbstractModule {
	
	@Override
	protected void configure() {
		
		Multibinder<ServiceInvoker> connServiceInvokerBinder = Multibinder.newSetBinder(binder(), ServiceInvoker.class, Names.named("socket_connection_server_service_invoker"));
		connServiceInvokerBinder.addBinding().to(Key.get(ServiceInvoker.class, Names.named("UserService")));
		connServiceInvokerBinder.addBinding().to(Key.get(ServiceInvoker.class, Names.named("IMService")));
		connServiceInvokerBinder.addBinding().to(Key.get(ServiceInvoker.class, Names.named("DiscoverService")));
		connServiceInvokerBinder.addBinding().to(Key.get(ServiceInvoker.class, Names.named("DiscoverV2Service")));
		connServiceInvokerBinder.addBinding().to(Key.get(ServiceInvoker.class, Names.named("OfficialService")));
		connServiceInvokerBinder.addBinding().to(Key.get(ServiceInvoker.class, Names.named("SettingsService")));
		connServiceInvokerBinder.addBinding().to(Key.get(ServiceInvoker.class, Names.named("CommunityService")));
		connServiceInvokerBinder.addBinding().to(Key.get(ServiceInvoker.class, Names.named("WebRTCService")));
		connServiceInvokerBinder.addBinding().to(Key.get(ServiceInvoker.class, Names.named("CreditsService")));
		
		bind(SocketConnectionServer.class).in(Singleton.class);
		bind(ConnServiceImpl.class).in(Singleton.class);
		bind(SocketRegistry.class).in(Singleton.class);
	}
	
	@Provides
	@Named("socket_connection_server_bind_addr")
	public InetSocketAddress provideSocketConnectionServerBindAddr(@Named("server_conf") Properties confProperties) {
		HostAndPort socketConnectionBindAddr = HostAndPort.fromString(confProperties.getProperty("socket_connection_server_bind_addr"));
		return new InetSocketAddress(socketConnectionBindAddr.getHostText(), socketConnectionBindAddr.getPort());
	}
	
	@ProvidesIntoSet
	public ServerEntry.StartHook provideStartHook(final SocketConnectionServer socketConnServer) {
		return new ServerEntry.StartHook() {

			@Override
			public void execute() {
				socketConnServer.start();
			}
			
		};
	}
	
	@ProvidesIntoSet
	public ServerEntry.ShutdownHook provideShutdownHook(final SocketConnectionServer socketConnServer) {
		return new ServerEntry.ShutdownHook() {

			@Override
			public ServerEntry.ShutdownHook.Order order() {
				return ServerEntry.ShutdownHook.Order.SERVER;
			}
			
			@Override
			public void execute() {
				socketConnServer.stop();
			}
			
		};
	}
	
	@Provides
	@Singleton
	public ConnService provideConnService(ConnServiceImpl serviceImpl, @Named("service_executor") Executor serviceExecutor) {
		return ServiceStub.createServiceApi(ConnService.class, serviceImpl, serviceExecutor);
	}

	@Provides
	@Singleton
	@Named("ConnService")
	public ServiceInvoker provideConnServiceInvoker(ConnServiceImpl serviceImpl, @Named("service_executor") Executor serviceExecutor, @Nullable InfluxDBReporter influxDBReporter) {
		return ServiceStub.createServiceInvoker(ConnService.class, serviceImpl, serviceExecutor, influxDBReporter);
	}
	
}
