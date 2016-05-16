package com.weizhu.service.community.test;

import com.google.inject.AbstractModule;
import com.google.inject.multibindings.Multibinder;
import com.google.inject.name.Names;

public class CommunityServiceTestModule extends AbstractModule {

	@Override
	protected void configure() {
		Multibinder<String> testDataSQLBinder = Multibinder.newSetBinder(binder(), String.class, Names.named("db_test_data.sql"));
		testDataSQLBinder.addBinding().toInstance("com/weizhu/service/community/test/db_test_data.sql");
	}

}
