package com.weizhu.webapp.admin;

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
import com.weizhu.proto.AbsenceService;
import com.weizhu.proto.AdminCommunityService;
import com.weizhu.proto.AdminComponentService;
import com.weizhu.proto.AdminCreditsService;
import com.weizhu.proto.AdminDiscoverService;
import com.weizhu.proto.AdminExamService;
import com.weizhu.proto.AdminOfficialService;
import com.weizhu.proto.AdminOfflineTrainingService;
import com.weizhu.proto.AdminQAService;
import com.weizhu.proto.AdminSceneService;
import com.weizhu.proto.AdminService;
import com.weizhu.proto.AdminUserService;
import com.weizhu.proto.AllowService;
import com.weizhu.proto.CreditsService;
import com.weizhu.proto.ExamService;
import com.weizhu.proto.LoginService;
import com.weizhu.proto.SessionService;
import com.weizhu.proto.StatsService;
import com.weizhu.proto.SurveyService;
import com.weizhu.proto.SystemService;
import com.weizhu.proto.ToolsProductclockService;
import com.weizhu.proto.UploadService;

public class GuiceConfigListener extends GuiceServletContextListener {
	
	static {
		if (System.getProperty("logback.configurationFile") == null) {
			System.setProperty("logback.configurationFile", "com/weizhu/webapp/admin/logback.xml");
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
			
			install(new AdminServletModule());
			
			install(new RpcClientModule("common_logic_server"));
			install(new RpcServiceModule(AdminService.class, "common_logic_server"));
			install(new RpcServiceModule(SessionService.class, "common_logic_server"));
			install(new RpcServiceModule(LoginService.class, "common_logic_server"));
			install(new RpcServiceModule(SystemService.class, "common_logic_server"));
			
			install(new RpcClientModule("company_proxy_server"));
			install(new RpcServiceModule(AdminUserService.class, "company_proxy_server"));
			install(new RpcServiceModule(AdminOfficialService.class, "company_proxy_server"));
			install(new RpcServiceModule(ExamService.class, "company_proxy_server"));
			install(new RpcServiceModule(AdminExamService.class, "company_proxy_server"));
			install(new RpcServiceModule(AdminQAService.class, "company_proxy_server"));
			install(new RpcServiceModule(AdminCommunityService.class, "company_proxy_server"));
			install(new RpcServiceModule(AllowService.class, "company_proxy_server"));
			install(new RpcServiceModule(AdminDiscoverService.class, "company_proxy_server"));
			install(new RpcServiceModule(AdminSceneService.class, "company_proxy_server"));
			install(new RpcServiceModule(SurveyService.class, "company_proxy_server"));
			install(new RpcServiceModule(StatsService.class, "company_proxy_server"));
			install(new RpcServiceModule(CreditsService.class, "company_proxy_server"));
			install(new RpcServiceModule(AdminCreditsService.class, "company_proxy_server"));
			install(new RpcServiceModule(AbsenceService.class, "company_proxy_server"));
			install(new RpcServiceModule(ToolsProductclockService.class, "company_proxy_server"));
			install(new RpcServiceModule(AdminOfflineTrainingService.class, "company_proxy_server"));
			install(new RpcServiceModule(AdminComponentService.class, "company_proxy_server"));
			
			install(new RpcClientModule("upload_server"));
			install(new RpcServiceModule(UploadService.class, "upload_server"));
		}
		
	}
	
	private final Injector injector = Guice.createInjector(
			Stage.PRODUCTION,
			System.getProperty("server.conf") != null ? 
					new ConfModule() : 
					new ConfModule(Resources.getResource("com/weizhu/webapp/admin/test.conf"))
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
