package com.weizhu.service.stats.test;

import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Ignore;
import org.junit.runners.MethodSorters;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Singleton;
import com.google.inject.name.Names;
import com.weizhu.common.module.FakeExternalServiceModule;
import com.weizhu.common.module.FakeProfileServiceModule;
import com.weizhu.common.module.FakePushServiceModule;
import com.weizhu.common.module.TestModule;
import com.weizhu.common.utils.TestUtil;
import com.weizhu.proto.StatsService;
import com.weizhu.service.allow.AllowServiceModule;
import com.weizhu.service.company.CompanyServiceLocalModule;
import com.weizhu.service.company.test.CompanyServiceTestModule;
import com.weizhu.service.discover.DiscoverServiceModule;
import com.weizhu.service.discover.test.DiscoverServiceTestModule;
import com.weizhu.service.discover_v2.test.DiscoverV2ServiceTestModule;
import com.weizhu.service.exam.ExamServiceModule;
import com.weizhu.service.exam.test.ExamServiceTestModule;
import com.weizhu.service.stats.StatsServiceModule;
import com.weizhu.service.user.UserServiceModule;
import com.weizhu.service.user.test.UserServiceTestModule;
import com.zaxxer.hikari.HikariDataSource;

@Ignore
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class StatsServiceTest {
	
	static {
		System.setProperty("logback.configurationFile", "com/weizhu/service/stats/test/logback.xml");
	}

	private static final Injector INJECTOR = Guice.createInjector(new TestModule(), 
			new StatsServiceModule(),
			new CompanyServiceLocalModule(), new CompanyServiceTestModule(), 
			new UserServiceModule(), new UserServiceTestModule(), new FakePushServiceModule(), new FakeProfileServiceModule(), new FakeExternalServiceModule(),
			new DiscoverServiceModule(), new DiscoverServiceTestModule(), new DiscoverV2ServiceTestModule(),
			new ExamServiceTestModule(), new ExamServiceModule(), new AllowServiceModule(),
			new AbstractModule() {

				@Override
				protected void configure() {
					bind(HikariDataSource.class).annotatedWith(Names.named("stats_db"))
						.to(HikariDataSource.class).in(Singleton.class);
				}
		
	});
	
	@BeforeClass
	public static void init() throws Exception {
		TestUtil.clearCache(INJECTOR);
		TestUtil.loadTestDataDB(INJECTOR);
	}
	
	@SuppressWarnings("unused")
	private final StatsService statsService;
	
	public StatsServiceTest() {
		this.statsService = INJECTOR.getInstance(StatsService.class);
	}
}
