package com.weizhu.service.qa.test;

import com.google.inject.AbstractModule;
import com.google.inject.multibindings.Multibinder;
import com.google.inject.name.Names;

public class QAServiceTestModule extends AbstractModule {

	@Override
	protected void configure() {
		//		bind(UserServiceImpl.class).in(Singleton.class);

		Multibinder<String> testDataSQLBinder = Multibinder.newSetBinder(binder(), String.class, Names.named("db_test_data.sql"));
		testDataSQLBinder.addBinding().toInstance("com/weizhu/service/qa/test/db_test_data.sql");
		//		testDataSQLBinder.addBinding().toInstance("com/weizhu/service/exam/test/db_test_data_2.sql");
	}

}
