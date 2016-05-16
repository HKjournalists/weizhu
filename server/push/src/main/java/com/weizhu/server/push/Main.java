package com.weizhu.server.push;

import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import javax.annotation.Nullable;

import com.google.common.base.CharMatcher;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.net.HostAndPort;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.Stage;
import com.google.inject.multibindings.Multibinder;
import com.google.inject.multibindings.MultibindingsScanner;
import com.google.inject.name.Named;
import com.google.inject.name.Names;
import com.weizhu.common.influxdb.InfluxDBReporter;
import com.weizhu.common.module.ConfModule;
import com.weizhu.common.module.DBModule;
import com.weizhu.common.module.ExecutorModule;
import com.weizhu.common.module.InfluxDBReporterModule;
import com.weizhu.common.module.NettyModule;
import com.weizhu.common.module.RedisModule;
import com.weizhu.common.module.RpcClientModule;
import com.weizhu.common.module.RpcServerModule;
import com.weizhu.common.module.RpcServiceModule;
import com.weizhu.common.rpc.AutoSwitchRpcClient;
import com.weizhu.common.rpc.RpcInvoker;
import com.weizhu.common.server.ServerEntry;
import com.weizhu.common.service.ServiceStub;
import com.weizhu.proto.APNsService;
import com.weizhu.proto.ConnService;
import com.weizhu.service.push.PushServiceModule;

import io.netty.channel.nio.NioEventLoopGroup;

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
			install(new RedisModule());
			install(new InfluxDBReporterModule());
			
			install(new PushServiceModule());
			install(new RpcServerModule("push_server", 
					ImmutableSet.of(
							"PushService",
							"PushPollingService"
							)));
			
			install(new RpcClientModule("external_server"));
			install(new RpcServiceModule(APNsService.class, "external_server"));
		}
		
		@Provides
		@Singleton
		public Set<ConnService> provideConnServiceSet(@Named("server_conf") Properties confProperties, NioEventLoopGroup eventLoop, @Nullable InfluxDBReporter influxDBReporter) {
			String str = confProperties.getProperty("conn_server_addr");
			List<String> addrStrList = Splitter.on(CharMatcher.anyOf(",;")).trimResults().omitEmptyStrings().splitToList(str);
			
			IdentityHashMap<ConnService, Object> connServiceSet = new IdentityHashMap<ConnService, Object>();
			for (String addrStr : addrStrList) {
				HostAndPort hp = HostAndPort.fromString(addrStr);
				RpcInvoker connClient = new AutoSwitchRpcClient(ImmutableList.of(new InetSocketAddress(hp.getHostText(), hp.getPort())), eventLoop);
				ConnService connService = ServiceStub.createServiceApi(ConnService.class, 
						ServiceStub.createServiceInvoker("ConnService", connClient, eventLoop, influxDBReporter));
				
				connServiceSet.put(connService, Boolean.FALSE);
			}
			
			return connServiceSet.keySet();
		}
	}

	public static void main(String[] args) throws Throwable {
		if (System.getProperty("logback.configurationFile") == null) {
			System.setProperty("logback.configurationFile", "com/weizhu/server/push/logback.xml");
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
