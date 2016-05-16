package com.weizhu.common.influxdb;

import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicLong;

import com.google.common.collect.ImmutableMap;


public class JvmGarbageCollectorMetrics {

	public static void registerAll(InfluxDBReporter influxDBReporter) {
		for (GarbageCollectorMXBean mxBean : ManagementFactory.getGarbageCollectorMXBeans()) {
			influxDBReporter.register("jvm_gc", ImmutableMap.of("name", mxBean.getName().replaceAll("[\\s]+", "")), new Metric(mxBean));
		}
	}
	
	private static class Metric implements InfluxDBMetric {

		final GarbageCollectorMXBean mxBean;
		final AtomicLong lastGcCount = new AtomicLong(0L);
		final AtomicLong lastGcTime = new AtomicLong(0L);
		
		Metric(GarbageCollectorMXBean mxBean) {
			this.mxBean = mxBean;
		}
		
		@Override
		public Map<String, Object> getField() {
			Map<String, Object> fieldMap = new TreeMap<String, Object>();
			long countValue = this.mxBean.getCollectionCount() - this.lastGcCount.get();
			if (countValue > 0) {
				fieldMap.put("gc_count", countValue);
			}
			long timeValue = this.mxBean.getCollectionTime() - this.lastGcTime.get();
			if (timeValue > 0) {
				fieldMap.put("gc_time", timeValue);
			}
			return fieldMap;
		}

		@Override
		public Map<String, Object> getAndResetField() {
			Map<String, Object> fieldMap = new TreeMap<String, Object>();
			long countValue = this.mxBean.getCollectionCount();
			countValue = countValue - this.lastGcCount.getAndSet(countValue);
			if (countValue > 0) {
				fieldMap.put("gc_count", countValue);
			}
			long timeValue = this.mxBean.getCollectionTime();
			timeValue = timeValue - this.lastGcTime.getAndSet(timeValue);
			if (timeValue > 0) {
				fieldMap.put("gc_time", timeValue);
			}
			return fieldMap;
		}
		
	}
	
}
