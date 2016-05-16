package com.weizhu.service.stats.test.log;

import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LogCreator {
	
	static {
		System.setProperty("logback.configurationFile", "com/weizhu/service/stats/test/log/creator_logback.xml");
	}

	private static final Logger logger = LoggerFactory.getLogger(LogCreator.class);
	
	public static void main(String[] args) throws InterruptedException {
		
		int i=0;
		while (true) {
			logger.info("info: " + (i++));
			
			TimeUnit.SECONDS.sleep(3);
		}
		
	}

}
