package com.weizhu.server.data;

import java.util.Arrays;

import com.google.common.collect.ImmutableSet;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Stage;
import com.google.inject.multibindings.Multibinder;
import com.google.inject.multibindings.MultibindingsScanner;
import com.google.inject.name.Names;
import com.weizhu.common.module.ConfModule;
import com.weizhu.common.module.DBModule;
import com.weizhu.common.module.ExecutorModule;
import com.weizhu.common.module.InfluxDBReporterModule;
import com.weizhu.common.module.NettyModule;
import com.weizhu.common.module.RpcClientModule;
import com.weizhu.common.module.RpcServerModule;
import com.weizhu.common.module.RpcServiceModule;
import com.weizhu.common.server.ServerEntry;
import com.weizhu.proto.AdminDiscoverService;
import com.weizhu.proto.AdminUserService;
import com.weizhu.proto.CompanyService;
import com.weizhu.service.stats.StatsServiceModule;

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
			
			install(new StatsServiceModule());
			install(new RpcServerModule("data_server", 
					ImmutableSet.of(
							"StatsService")
					));
			
			install(new RpcClientModule("company_proxy_server"));
			install(new RpcServiceModule(CompanyService.class, "company_proxy_server"));
			install(new RpcServiceModule(AdminUserService.class, "company_proxy_server"));
			install(new RpcServiceModule(AdminDiscoverService.class, "company_proxy_server"));
		}
	}
	
	public static void main(String[] args) {
		if (System.getProperty("logback.configurationFile") == null) {
			System.setProperty("logback.configurationFile", "com/weizhu/server/data/logback.xml");
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
