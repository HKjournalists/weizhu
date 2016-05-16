package com.weizhu.server.company.logic;

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
import com.weizhu.proto.ExternalService;
import com.weizhu.proto.PushService;
import com.weizhu.service.absence.AbsenceServiceModule;
import com.weizhu.service.allow.AllowServiceModule;
import com.weizhu.service.community.CommunityServiceModule;
import com.weizhu.service.credits.CreditsServiceModule;
import com.weizhu.service.discover.DiscoverServiceModule;
import com.weizhu.service.exam.ExamServiceModule;
import com.weizhu.service.im.IMServiceModule;
import com.weizhu.service.official.OfficialServiceModule;
import com.weizhu.service.offline_training.OfflineTrainingServiceModule;
import com.weizhu.service.profile.ProfileServiceModule;
import com.weizhu.service.qa.QAServiceModule;
import com.weizhu.service.settings.SettingsServiceModule;
import com.weizhu.service.survey.SurveyServiceModule;
import com.weizhu.service.tools.productcolock.ToolsProductclockServiceModule;
import com.weizhu.service.user.UserServiceModule;
import com.weizhu.service.webrtc.WebRTCServiceModule;

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
			
			install(new UserServiceModule());
			install(new IMServiceModule());
			install(new DiscoverServiceModule());
			install(new OfficialServiceModule());
			install(new SettingsServiceModule());
			install(new ExamServiceModule());
			install(new CommunityServiceModule());
			install(new QAServiceModule());
			install(new AllowServiceModule());
			install(new WebRTCServiceModule());
			install(new SurveyServiceModule());
			install(new ProfileServiceModule());
			install(new CreditsServiceModule());
			install(new AbsenceServiceModule());
			install(new ToolsProductclockServiceModule());
			install(new OfflineTrainingServiceModule());
			
			install(new RpcServerModule("company_logic_server", 
					ImmutableSet.of(
							"UserService", 
							"AdminUserService", 
							"IMService",
							"DiscoverService",
							"DiscoverV2Service",
							"AdminDiscoverService",
							"OfficialService",
							"AdminOfficialService",
							"SettingsService",
							"ExamService",
							"AdminExamService",
							"CommunityService",
							"AdminCommunityService",
							"QAService",
							"AdminQAService",
							"AllowService",
							"WebRTCService",
							"SurveyService",
							"ProfileService",
							"CreditsService",
							"AdminCreditsService",
							"AbsenceService",
							"ToolsProductclockService",
							"OfflineTrainingService",
							"AdminOfflineTrainingService")
					));
			
			install(new RpcClientModule("external_server"));
			install(new RpcServiceModule(ExternalService.class, "external_server"));
			
			install(new RpcClientModule("push_server"));
			install(new RpcServiceModule(PushService.class, "push_server"));
		}
	}

	public static void main(String[] args) throws Throwable {
		if (System.getProperty("logback.configurationFile") == null) {
			System.setProperty("logback.configurationFile", "com/weizhu/server/company/logic/logback.xml");
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
