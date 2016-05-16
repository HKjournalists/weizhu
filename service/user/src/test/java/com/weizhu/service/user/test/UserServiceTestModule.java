package com.weizhu.service.user.test;

import com.google.inject.AbstractModule;
import com.google.inject.multibindings.Multibinder;
import com.google.inject.name.Names;

public class UserServiceTestModule extends AbstractModule {

	@Override
	protected void configure() {
		Multibinder<String> testDataSQLBinder = Multibinder.newSetBinder(binder(), String.class, Names.named("db_test_data.sql"));
		testDataSQLBinder.addBinding().toInstance("com/weizhu/service/user/test/db_test_data.sql");
	}

}
