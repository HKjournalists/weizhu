package com.weizhu.server.all;

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
import com.weizhu.common.module.RpcServerModule;
import com.weizhu.common.server.ServerEntry;
import com.weizhu.proto.ConnService;
import com.weizhu.server.api.HttpApiModule;
import com.weizhu.server.conn.SocketConnectionModule;
import com.weizhu.service.absence.AbsenceServiceModule;
import com.weizhu.service.admin.AdminServiceModule;
import com.weizhu.service.allow.AllowServiceModule;
import com.weizhu.service.apns.APNsServiceModule;
import com.weizhu.service.boss.BossServiceModule;
import com.weizhu.service.community.CommunityServiceModule;
import com.weizhu.service.company.CompanyServiceLocalModule;
import com.weizhu.service.component.ComponentServiceModule;
import com.weizhu.service.credits.CreditsServiceModule;
import com.weizhu.service.discover.DiscoverServiceModule;
import com.weizhu.service.exam.ExamServiceModule;
import com.weizhu.service.external.ExternalServiceModule;
import com.weizhu.service.im.IMServiceModule;
import com.weizhu.service.login.LoginServiceModule;
import com.weizhu.service.official.OfficialServiceModule;
import com.weizhu.service.offline_training.OfflineTrainingServiceModule;
import com.weizhu.service.profile.ProfileServiceModule;
import com.weizhu.service.push.PushServiceModule;
import com.weizhu.service.qa.QAServiceModule;
import com.weizhu.service.scene.SceneServiceModule;
import com.weizhu.service.session.SessionServiceModule;
import com.weizhu.service.settings.SettingsServiceModule;
import com.weizhu.service.stats.StatsServiceModule;
import com.weizhu.service.survey.SurveyServiceModule;
import com.weizhu.service.system.SystemServiceModule;
import com.weizhu.service.tools.productcolock.ToolsProductclockServiceModule;
import com.weizhu.service.upload.UploadServiceModule;
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
			Multibinder<String> initDataSQLBinder = Multibinder.newSetBinder(binder(), String.class, Names.named("db_init_data.sql"));
			initDataSQLBinder.addBinding().toInstance("com/weizhu/server/all/db_init_data.sql");
			
			install(new NettyModule());
			install(new ExecutorModule());
			install(new DBModule());
			install(new RedisModule());
			install(new InfluxDBReporterModule());
			
			install(new ExternalServiceModule());
			install(new APNsServiceModule());
			
			install(new AdminServiceModule());
			install(new SessionServiceModule());
			install(new LoginServiceModule());
			install(new SystemServiceModule());
			
			install(new CompanyServiceLocalModule());
			
			install(new PushServiceModule());
			
			install(new BossServiceModule());
			install(new StatsServiceModule());
			
			install(new UserServiceModule());
			install(new IMServiceModule());
			install(new DiscoverServiceModule());
			install(new ExamServiceModule());
			install(new OfficialServiceModule());
			install(new SettingsServiceModule());
			install(new CommunityServiceModule());
			install(new QAServiceModule());
			install(new SurveyServiceModule());
			install(new ProfileServiceModule());
			install(new CreditsServiceModule());
			install(new AbsenceServiceModule());
			install(new OfflineTrainingServiceModule());
			install(new ComponentServiceModule());
			
			install(new AllowServiceModule());
			install(new WebRTCServiceModule());
			install(new SceneServiceModule());
			install(new ToolsProductclockServiceModule());
			install(new UploadServiceModule());
			install(new RpcServerModule("weizhu_all_server", 
					ImmutableSet.of(
							"AdminService",
							"SessionService", 
							"LoginService", 
							"UserService",
							"AdminUserService",
							"PushService",
							"PushPollingService",
							"IMService",
							"DiscoverService",
							"DiscoverV2Service",
							"ExamService",
							"AdminExamService",
							"SystemService",
							"ConnService",
							"OfficialService",
							"AdminOfficialService",
							"SettingsService",
							"CompanyService",
							"CommunityService",
							"AdminCommunityService",
							"AdminQAService",
							"QAService",
							"BossService",
							"StatsService",
							"AllowService",
							"AdminDiscoverService",
							"WebRTCService",
							"SceneService",
							"AdminSceneService",
							"SurveyService",
							"ToolsProductclockService",
							"UploadService",
							"ProfileService",
							"CreditsService",
							"AdminCreditsService",
							"AbsenceService",
							"OfflineTrainingService",
							"AdminOfflineTrainingService",
							"ComponentService",
							"AdminComponentService")));
			
			install(new HttpApiModule());
			install(new SocketConnectionModule());
			Multibinder.newSetBinder(binder(), ConnService.class).addBinding().to(ConnService.class);
		}
	}

	public static void main(String[] args) throws Throwable {
		if (System.getProperty("logback.configurationFile") == null) {
			System.setProperty("logback.configurationFile", "com/weizhu/server/all/logback.xml");
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
