package com.weizhu.webapp.boss;

import java.util.List;

import javax.servlet.ServletContextEvent;

import com.google.common.io.Resources;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Stage;
import com.google.inject.multibindings.Multibinder;
import com.google.inject.multibindings.MultibindingsScanner;
import com.google.inject.servlet.GuiceServletContextListener;
import com.weizhu.common.module.ConfModule;
import com.weizhu.common.module.ExecutorModule;
import com.weizhu.common.module.InfluxDBReporterModule;
import com.weizhu.common.module.NettyModule;
import com.weizhu.common.module.RpcClientModule;
import com.weizhu.common.module.RpcServiceModule;
import com.weizhu.common.server.ServerEntry;
import com.weizhu.proto.AdminDiscoverService;
import com.weizhu.proto.BossService;
import com.weizhu.proto.CompanyService;
import com.weizhu.proto.ProfileService;
import com.weizhu.proto.StatsService;
import com.weizhu.proto.SystemService;

public class GuiceConfigListener extends GuiceServletContextListener {
	
	static {
		if (System.getProperty("logback.configurationFile") == null) {
			System.setProperty("logback.configurationFile", "com/weizhu/webapp/boss/logback.xml");
		}
	}
	
	public static class WebappModule extends AbstractModule {

		@Override
		protected void configure() {
			install(MultibindingsScanner.asModule());
			Multibinder.newSetBinder(binder(), ServerEntry.StartHook.class); 
			Multibinder.newSetBinder(binder(), ServerEntry.ShutdownHook.class);
			
			install(new NettyModule());
			install(new ExecutorModule());
			install(new InfluxDBReporterModule());
			
			install(new BossServletModule());
			
			install(new RpcClientModule("boss_server"));
			install(new RpcServiceModule(BossService.class, "boss_server"));
			
			install(new RpcClientModule("common_logic_server"));
			install(new RpcServiceModule(SystemService.class, "common_logic_server"));
			
			install(new RpcClientModule("company_proxy_server"));
			install(new RpcServiceModule(CompanyService.class, "company_proxy_server"));
			install(new RpcServiceModule(AdminDiscoverService.class, "company_proxy_server"));
			install(new RpcServiceModule(ProfileService.class, "company_proxy_server"));
			
			install(new RpcClientModule("data_server"));
			install(new RpcServiceModule(StatsService.class, "data_server"));
		}
		
	}
	
	private final Injector injector = Guice.createInjector(
			Stage.PRODUCTION,
			System.getProperty("server.conf") != null ? 
					new ConfModule() : 
					new ConfModule(Resources.getResource("com/weizhu/webapp/boss/test.conf"))
			, 
			new WebappModule()
			);
	
	@Override
	protected Injector getInjector() {
		return this.injector;
	}

	private List<ServerEntry.ShutdownHook> shutdownHookList;

	@Override
	public void contextInitialized(ServletContextEvent servletContextEvent) {
		this.shutdownHookList = ServerEntry.start(this.injector);
		super.contextInitialized(servletContextEvent);
	}

	@Override
	public void contextDestroyed(ServletContextEvent servletContextEvent) {
		super.contextDestroyed(servletContextEvent);
		if (this.shutdownHookList != null) {
			ServerEntry.shutdown(this.shutdownHookList);
			this.shutdownHookList = null;
		}
	}
	
}
