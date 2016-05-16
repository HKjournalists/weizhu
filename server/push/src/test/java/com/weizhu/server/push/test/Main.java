package com.weizhu.server.push.test;

import java.util.Arrays;
import com.google.common.io.Resources;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Stage;
import com.weizhu.common.module.ConfModule;
import com.weizhu.common.server.ServerEntry;
import com.weizhu.server.push.Main.ServerModule;

public class Main {

	public static void main(String[] args) throws Throwable {
		if (System.getProperty("logback.configurationFile") == null) {
			System.setProperty("logback.configurationFile", "com/weizhu/server/push/test/logback.xml");
		}
		if (args.length <= 0) {
			final Injector injector = Guice.createInjector(
					Stage.DEVELOPMENT, 
					new ConfModule(Resources.getResource("com/weizhu/server/push/test/server.conf")), 
					new ServerModule()
					);
			
			ServerEntry.main(injector);
		} else if ("initdb".equals(args[0])) {
			Injector injector = Guice.createInjector(
					Stage.DEVELOPMENT, 
					new ConfModule(Resources.getResource("com/weizhu/server/all/test/server.conf")), 
					new ServerModule()
					);
			
			ServerEntry.initDB(injector);
		} else {
			System.err.println("invalid server arg : " + Arrays.asList(args));
			System.exit(1);
		}
	}

}
