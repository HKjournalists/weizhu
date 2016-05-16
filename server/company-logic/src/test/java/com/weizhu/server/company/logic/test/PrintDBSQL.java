package com.weizhu.server.company.logic.test;

import java.util.Set;
import com.google.common.base.Charsets;
import com.google.common.io.Resources;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.Stage;
import com.google.inject.name.Names;
import com.google.inject.util.Types;
import com.weizhu.common.module.ConfModule;
import com.weizhu.server.company.logic.Main.ServerModule;

public class PrintDBSQL {

	public static void main(String[] args) throws Exception {
		if (System.getProperty("logback.configurationFile") == null) {
			System.setProperty("logback.configurationFile", "com/weizhu/server/company/logic/test/logback.xml");
		}
		
		final Injector injector = Guice.createInjector(
				Stage.DEVELOPMENT, 
				new ConfModule(Resources.getResource("com/weizhu/server/company/logic/test/server.conf")), 
				new ServerModule()
				);
		
		@SuppressWarnings("unchecked")
		final Set<String> createTableSQLSet = (Set<String>) injector.getInstance(Key.get(Types.setOf(String.class), Names.named("db_create_table.sql")));
		
		for (String sqlFile : createTableSQLSet) {
			System.out.println(Resources.toString(Resources.getResource(sqlFile), Charsets.UTF_8));
		}
		
	}

}
