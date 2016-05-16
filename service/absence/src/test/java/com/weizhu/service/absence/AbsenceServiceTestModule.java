package com.weizhu.service.absence;

import com.google.inject.AbstractModule;
import com.google.inject.multibindings.Multibinder;
import com.google.inject.name.Names;

public class AbsenceServiceTestModule extends AbstractModule {

	@Override
	protected void configure() {
		
		Multibinder<String> testDataSQLBinder = Multibinder.newSetBinder(binder(), String.class, Names.named("db_test_data.sql"));
		testDataSQLBinder.addBinding().toInstance("com/weizhu/service/absence/test/db_test_data.sql");
		
	}

}
