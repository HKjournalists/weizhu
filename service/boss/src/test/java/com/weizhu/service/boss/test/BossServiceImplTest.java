package com.weizhu.service.boss.test;

import org.junit.BeforeClass;
import org.junit.Test;

import com.google.common.base.Charsets;
import com.google.common.hash.Hashing;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.weizhu.common.module.TestModule;
import com.weizhu.common.utils.TestUtil;
import com.weizhu.proto.BossProtos.BossHead;
import com.weizhu.proto.BossService;
import com.weizhu.service.boss.BossServiceModule;

@SuppressWarnings("unused")
public class BossServiceImplTest {
	
	static {
		System.setProperty("logback.configurationFile", "com/weizhu/service/boss/test/logback.xml");
	}

	private static final Injector INJECTOR = Guice.createInjector(new TestModule(), new BossServiceModule(), new BossServiceTestModule());

	@BeforeClass
	public static void init() {
		TestUtil.clearCache(INJECTOR);
		TestUtil.loadTestDataDB(INJECTOR);
	}

	private final BossHead bossHead;
	private final BossService bossService;

	public BossServiceImplTest() {
		this.bossHead = INJECTOR.getInstance(BossHead.class);
		this.bossService = INJECTOR.getInstance(BossService.class);
	}

	@Test
	public void test() {
		System.out.println("ok");
	}
	
	public static void main(String... args) {
		System.out.println(Hashing.sha1().hashString("1234abcd" + "weizhu@2015", Charsets.UTF_8).toString());
	}
	
}
