package com.weizhu.server.api;

import java.net.InetSocketAddress;
import java.util.Properties;

import com.google.common.net.HostAndPort;
import com.google.inject.AbstractModule;
import com.google.inject.Key;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.multibindings.Multibinder;
import com.google.inject.multibindings.ProvidesIntoSet;
import com.google.inject.name.Named;
import com.google.inject.name.Names;
import com.weizhu.common.server.ServerEntry;
import com.weizhu.common.service.ServiceInvoker;

public class HttpApiModule extends AbstractModule {
	
	@Override
	protected void configure() {
		
		Multibinder<ServiceInvoker> serviceInvokerBinder = Multibinder.newSetBinder(binder(), ServiceInvoker.class, Names.named("http_api_server_service_invoker"));
		// common logic server
		serviceInvokerBinder.addBinding().to(Key.get(ServiceInvoker.class, Names.named("LoginService")));
		serviceInvokerBinder.addBinding().to(Key.get(ServiceInvoker.class, Names.named("SystemService")));
		// company proxy server
		serviceInvokerBinder.addBinding().to(Key.get(ServiceInvoker.class, Names.named("UserService")));
		serviceInvokerBinder.addBinding().to(Key.get(ServiceInvoker.class, Names.named("IMService")));
		serviceInvokerBinder.addBinding().to(Key.get(ServiceInvoker.class, Names.named("DiscoverService")));
		serviceInvokerBinder.addBinding().to(Key.get(ServiceInvoker.class, Names.named("DiscoverV2Service")));
		serviceInvokerBinder.addBinding().to(Key.get(ServiceInvoker.class, Names.named("OfficialService")));
		serviceInvokerBinder.addBinding().to(Key.get(ServiceInvoker.class, Names.named("SettingsService")));
		serviceInvokerBinder.addBinding().to(Key.get(ServiceInvoker.class, Names.named("CommunityService")));
		serviceInvokerBinder.addBinding().to(Key.get(ServiceInvoker.class, Names.named("WebRTCService")));
		serviceInvokerBinder.addBinding().to(Key.get(ServiceInvoker.class, Names.named("CreditsService")));
		// push server
		serviceInvokerBinder.addBinding().to(Key.get(ServiceInvoker.class, Names.named("PushPollingService")));
		
		this.bind(HttpApiServer.class).in(Singleton.class);
	}
	
	@Provides
	@Named("http_api_server_bind_addr")
	public InetSocketAddress provideHttpApiServerBindAddr(@Named("server_conf") Properties confProperties) {
		HostAndPort httpApiBindAddr = HostAndPort.fromString(confProperties.getProperty("http_api_server_bind_addr"));
		return new InetSocketAddress(httpApiBindAddr.getHostText(), httpApiBindAddr.getPort());
	}
	
	@ProvidesIntoSet
	public ServerEntry.StartHook provideStartHook(final HttpApiServer httpApiServer) {
		return new ServerEntry.StartHook() {

			@Override
			public void execute() {
				httpApiServer.start();
			}
			
		};
	}
	
	@ProvidesIntoSet
	public ServerEntry.ShutdownHook provideShutdownHook(final HttpApiServer httpApiServer) {
		return new ServerEntry.ShutdownHook() {

			@Override
			public ServerEntry.ShutdownHook.Order order() {
				return ServerEntry.ShutdownHook.Order.SERVER;
			}
			
			@Override
			public void execute() {
				httpApiServer.stop();
			}
			
		};
	}
	
}
