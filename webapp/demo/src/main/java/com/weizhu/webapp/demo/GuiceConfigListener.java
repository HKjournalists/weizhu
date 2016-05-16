package com.weizhu.webapp.demo;

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
import com.weizhu.common.module.DBModule;
import com.weizhu.common.module.ExecutorModule;
import com.weizhu.common.module.NettyModule;
import com.weizhu.common.module.RpcClientModule;
import com.weizhu.common.module.RpcServiceModule;
import com.weizhu.common.server.ServerEntry;
import com.weizhu.proto.AdminUserService;
import com.weizhu.proto.DiscoverService;
import com.weizhu.proto.DiscoverV2Service;
import com.weizhu.proto.ExamService;
import com.weizhu.proto.StatsService;

public class GuiceConfigListener extends GuiceServletContextListener {

	static {
		if (System.getProperty("logback.configurationFile") == null) {
			System.setProperty("logback.configurationFile", "com/weizhu/webapp/demo/logback.xml");
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
			install(new DBModule());
			
			install(new DemoServletModule());
			
			install(new RpcClientModule("company_proxy_server"));
			install(new RpcServiceModule(DiscoverService.class, "company_proxy_server"));
			install(new RpcServiceModule(DiscoverV2Service.class, "company_proxy_server"));
			install(new RpcServiceModule(ExamService.class, "company_proxy_server"));
			install(new RpcServiceModule(AdminUserService.class, "company_proxy_server"));
			
			install(new RpcClientModule("boss_server"));
			install(new RpcServiceModule(StatsService.class, "boss_server"));
		}
		
	}
	
	private final Injector injector = Guice.createInjector(
			Stage.PRODUCTION,
			System.getProperty("server.conf") != null ? 
					new ConfModule() : 
					new ConfModule(Resources.getResource("com/weizhu/webapp/demo/test.conf"))
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
