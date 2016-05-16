package com.weizhu.server.common.logic;

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
import com.weizhu.common.module.RedisModule;
import com.weizhu.common.module.RpcClientModule;
import com.weizhu.common.module.RpcServerModule;
import com.weizhu.common.module.RpcServiceModule;
import com.weizhu.common.server.ServerEntry;
import com.weizhu.proto.APNsService;
import com.weizhu.proto.AdminOfficialService;
import com.weizhu.proto.AdminUserService;
import com.weizhu.proto.CompanyService;
import com.weizhu.proto.ExternalService;
import com.weizhu.proto.OfficialService;
import com.weizhu.proto.ProfileService;
import com.weizhu.proto.PushService;
import com.weizhu.proto.UploadService;
import com.weizhu.proto.UserService;
import com.weizhu.service.admin.AdminServiceModule;
import com.weizhu.service.login.LoginServiceModule;
import com.weizhu.service.session.SessionServiceModule;
import com.weizhu.service.system.SystemServiceModule;

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
			
			install(new LoginServiceModule());
			install(new SessionServiceModule());
			install(new SystemServiceModule());
			install(new AdminServiceModule());
			install(new RpcServerModule("common_logic_server", 
					ImmutableSet.of(
							"LoginService", 
							"SessionService", 
							"SystemService",
							"AdminService")
					));
			
			install(new RpcClientModule("external_server"));
			install(new RpcServiceModule(ExternalService.class, "external_server"));
			install(new RpcServiceModule(APNsService.class, "external_server"));
			
			install(new RpcClientModule("upload_server"));
			install(new RpcServiceModule(UploadService.class, "upload_server"));
			
			install(new RpcClientModule("company_proxy_server"));
			install(new RpcServiceModule(CompanyService.class, "company_proxy_server"));
			install(new RpcServiceModule(UserService.class, "company_proxy_server"));
			install(new RpcServiceModule(AdminUserService.class, "company_proxy_server"));
			install(new RpcServiceModule(OfficialService.class, "company_proxy_server"));
			install(new RpcServiceModule(AdminOfficialService.class, "company_proxy_server"));
			install(new RpcServiceModule(ProfileService.class, "company_proxy_server"));
			
			install(new RpcClientModule("push_server"));
			install(new RpcServiceModule(PushService.class, "push_server"));
		}
	}

	public static void main(String[] args) throws Throwable {
		if (System.getProperty("logback.configurationFile") == null) {
			System.setProperty("logback.configurationFile", "com/weizhu/server/common/logic/logback.xml");
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
