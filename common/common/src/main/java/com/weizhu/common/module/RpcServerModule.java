package com.weizhu.common.module;

import io.netty.channel.nio.NioEventLoopGroup;

import java.net.InetSocketAddress;
import java.util.Properties;
import java.util.Set;

import com.google.common.collect.ImmutableSet;
import com.google.common.net.HostAndPort;
import com.google.inject.AbstractModule;
import com.google.inject.Key;
import com.google.inject.PrivateModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.multibindings.Multibinder;
import com.google.inject.name.Named;
import com.google.inject.name.Names;
import com.weizhu.common.rpc.RpcServer;
import com.weizhu.common.server.ServerEntry;
import com.weizhu.common.service.ServiceInvoker;
import com.weizhu.common.service.ServiceStub;

public class RpcServerModule extends AbstractModule {

	private final String rpcServerName;
	private final ImmutableSet<String> serviceNameSet;
	
	public RpcServerModule(String rpcServerName, ImmutableSet<String> serviceNameSet) {
		this.rpcServerName = rpcServerName;
		this.serviceNameSet = serviceNameSet;
	}

	@Override
	protected void configure() {
		install(new InternalRpcServerModule(this.rpcServerName, this.serviceNameSet));
		
		Multibinder.newSetBinder(binder(), ServerEntry.StartHook.class).addBinding().to(Key.get(ServerEntry.StartHook.class, Names.named(rpcServerName)));
	}
	
	public static class InternalRpcServerModule extends PrivateModule {
		
		private final String rpcServerName;
		private final ImmutableSet<String> serviceNameSet;
		
		public InternalRpcServerModule(String rpcServerName, ImmutableSet<String> serviceNameSet) {
			this.rpcServerName = rpcServerName;
			this.serviceNameSet = serviceNameSet;
		}
		
		@Override
		protected void configure() {
			
			Multibinder<ServiceInvoker> serviceInvokerBinder = Multibinder.newSetBinder(binder(), ServiceInvoker.class, Names.named("_internal_service_invoker"));
			for (String serviceName : serviceNameSet) {
				serviceInvokerBinder.addBinding().to(Key.get(ServiceInvoker.class, Names.named(serviceName)));
			}
			
			bind(RpcServer.class).annotatedWith(Names.named(rpcServerName))
				.to(Key.get(RpcServer.class, Names.named("_internal_rpc_server")));
			
			bind(ServerEntry.StartHook.class).annotatedWith(Names.named(rpcServerName))
				.to(Key.get(ServerEntry.StartHook.class, Names.named("_internal_rpc_server")));
			
			expose(RpcServer.class).annotatedWith(Names.named(rpcServerName));
			expose(ServerEntry.StartHook.class).annotatedWith(Names.named(rpcServerName));
		}
		
		@Provides
		@Singleton
		@Named("_internal_rpc_server")
		public RpcServer provideInternalRpcServer(
				@Named("server_conf") Properties confProperties,
				@Named("boss_group") NioEventLoopGroup bossGroup, 
				@Named("worker_group") NioEventLoopGroup workerGroup, 
				@Named("_internal_service_invoker") Set<ServiceInvoker> serviceInvokerSet) {
			String str = confProperties.getProperty(rpcServerName + "_bind_addr");
			if (str == null) {
				throw new RuntimeException("cannot find rpc server bind addr conf : " + rpcServerName + "_bind_addr");
			}
			
			HostAndPort bindAddr = HostAndPort.fromString(str);
			
			return new RpcServer(bossGroup, workerGroup, 
					new InetSocketAddress(bindAddr.getHostText(), bindAddr.getPort()), 
					ServiceStub.createRpcInvoker(serviceInvokerSet));
		}
		
		@Provides
		@Named("_internal_rpc_server")
		public ServerEntry.StartHook provideStartHook(@Named("_internal_rpc_server") final RpcServer rpcServer) {
			return new ServerEntry.StartHook() {

				@Override
				public void execute() {
					rpcServer.start();
				}
				
			};
		}
	}

}
