package com.weizhu.common.influxdb;

import java.util.Map;

public interface InfluxDBMetric {
	
	Map<String, Object> getField();
	Map<String, Object> getAndResetField();
	
}
