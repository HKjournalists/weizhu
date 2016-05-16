package com.weizhu.server.company.proxy;

import io.netty.channel.nio.NioEventLoopGroup;

import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.Properties;
import com.google.common.net.HostAndPort;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.Stage;
import com.google.inject.multibindings.Multibinder;
import com.google.inject.multibindings.MultibindingsScanner;
import com.google.inject.multibindings.ProvidesIntoSet;
import com.google.inject.name.Named;
import com.google.inject.name.Names;
import com.weizhu.common.module.ConfModule;
import com.weizhu.common.module.DBModule;
import com.weizhu.common.module.ExecutorModule;
import com.weizhu.common.module.InfluxDBReporterModule;
import com.weizhu.common.module.NettyModule;
import com.weizhu.common.rpc.RpcServer;
import com.weizhu.common.server.ServerEntry;
import com.weizhu.service.company.CompanyServiceProxyImpl;
import com.weizhu.service.company.CompanyServiceProxyModule;

public class Main {

	public static class ServerModule extends AbstractModule {

		@Override
		protected void configure() {
			install(MultibindingsScanner.asModule());
			Multibinder.newSetBinder(binder(), ServerEntry.StartHook.class); 
			Multibinder.newSetBinder(binder(), ServerEntry.ShutdownHook.class);
			Multibinder.newSetBinder(binder(), String.class, Names.named("db_create_table.sql"));
			Multibinder.newSetBinder(binder(), String.class, Names.named("db_init_data.sql"));
			
			install(new NettyModule());
			install(new ExecutorModule());
			install(new DBModule());
			install(new InfluxDBReporterModule());
			
			install(new CompanyServiceProxyModule());
		}
		
		@Provides
		@Singleton
		@Named("company_proxy_server")
		public RpcServer provideCompanyProxyServer(
				@Named("server_conf") Properties confProperties,
				@Named("boss_group") NioEventLoopGroup bossGroup, 
				@Named("worker_group") NioEventLoopGroup workerGroup, 
				CompanyServiceProxyImpl proxyImpl) {
			HostAndPort companyProxyBindAddr = HostAndPort.fromString(confProperties.getProperty("company_proxy_server_bind_addr"));

			return new RpcServer(bossGroup, workerGroup, 
					new InetSocketAddress(companyProxyBindAddr.getHostText(), companyProxyBindAddr.getPort()), 
					proxyImpl);
		}
		
		@ProvidesIntoSet
		public ServerEntry.StartHook provideStartHook(@Named("company_proxy_server") final RpcServer rpcServer) {
			return new ServerEntry.StartHook() {

				@Override
				public void execute() {
					rpcServer.start();
				}
				
			};
		}
	}
	
	public static void main(String[] args) throws Throwable {
		if (System.getProperty("logback.configurationFile") == null) {
			System.setProperty("logback.configurationFile", "com/weizhu/server/company/proxy/logback.xml");
		}
		if (args.length <= 0) {
			final Injector injector = Guice.createInjector(
					Stage.PRODUCTION, 
					new ConfModule(), 
					new ServerModule()
					);
			
			ServerEntry.main(injector);
		} else if ("initdb".equals(args[0])) {
			Injector injector = Guice.createInjector(
					Stage.DEVELOPMENT, 
					new ConfModule(), 
					new ServerModule()
					);
			
			ServerEntry.initDB(injector);
		} else {
			System.err.println("invalid server arg : " + Arrays.asList(args));
			System.exit(1);
		}
	}

}
