package com.weizhu.server.api;

import java.util.Arrays;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Stage;
import com.google.inject.multibindings.Multibinder;
import com.google.inject.multibindings.MultibindingsScanner;
import com.google.inject.name.Names;
import com.weizhu.common.module.ConfModule;
import com.weizhu.common.module.ExecutorModule;
import com.weizhu.common.module.InfluxDBReporterModule;
import com.weizhu.common.module.NettyModule;
import com.weizhu.common.module.RpcClientModule;
import com.weizhu.common.module.RpcServiceModule;
import com.weizhu.common.server.ServerEntry;
import com.weizhu.proto.CommunityService;
import com.weizhu.proto.CreditsService;
import com.weizhu.proto.DiscoverService;
import com.weizhu.proto.DiscoverV2Service;
import com.weizhu.proto.IMService;
import com.weizhu.proto.LoginService;
import com.weizhu.proto.OfficialService;
import com.weizhu.proto.PushPollingService;
import com.weizhu.proto.SessionService;
import com.weizhu.proto.SettingsService;
import com.weizhu.proto.SystemService;
import com.weizhu.proto.UserService;
import com.weizhu.proto.WebRTCService;

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
			install(new InfluxDBReporterModule());
			
			install(new HttpApiModule());
			
			install(new RpcClientModule("common_logic_server"));
			install(new RpcServiceModule(SessionService.class, "common_logic_server"));
			install(new RpcServiceModule(LoginService.class, "common_logic_server"));
			install(new RpcServiceModule(SystemService.class, "common_logic_server"));
			
			install(new RpcClientModule("company_proxy_server"));
			install(new RpcServiceModule(UserService.class, "company_proxy_server"));
			install(new RpcServiceModule(IMService.class, "company_proxy_server"));
			install(new RpcServiceModule(DiscoverService.class, "company_proxy_server"));
			install(new RpcServiceModule(DiscoverV2Service.class, "company_proxy_server"));
			install(new RpcServiceModule(OfficialService.class, "company_proxy_server"));
			install(new RpcServiceModule(SettingsService.class, "company_proxy_server"));
			install(new RpcServiceModule(CommunityService.class, "company_proxy_server"));
			install(new RpcServiceModule(WebRTCService.class, "company_proxy_server"));
			install(new RpcServiceModule(CreditsService.class, "company_proxy_server"));
			
			install(new RpcClientModule("push_server"));
			install(new RpcServiceModule(PushPollingService.class, "push_server"));
		}
		
	}
	
	public static void main(String[] args) {
		if (System.getProperty("logback.configurationFile") == null) {
			System.setProperty("logback.configurationFile", "com/weizhu/server/api/logback.xml");
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
