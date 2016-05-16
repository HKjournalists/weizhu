package com.weizhu.service.stats.test.log;

import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LogCollector {
	
	static {
		System.setProperty("logback.configurationFile", "com/weizhu/service/stats/test/log/collector_logback.xml");
	}
	
	private static Logger logger = LoggerFactory.getLogger(LogCollector.class);

	public static void main(String[] args) throws InterruptedException  {
		logger.info("start!!!");
		TimeUnit.SECONDS.sleep(100);
	}
	
	
}
