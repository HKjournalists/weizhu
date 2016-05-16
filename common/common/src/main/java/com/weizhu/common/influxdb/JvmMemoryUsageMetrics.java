package com.weizhu.common.influxdb;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryPoolMXBean;
import java.lang.management.MemoryUsage;
import java.util.Map;
import java.util.TreeMap;

import com.google.common.collect.ImmutableMap;

public class JvmMemoryUsageMetrics {

	public static void registerAll(InfluxDBReporter influxDBReporter) {
		MemoryMXBean mxBean = ManagementFactory.getMemoryMXBean();
		influxDBReporter.register("jvm_memory", ImmutableMap.of("name", "Heap"), new Metric(mxBean.getHeapMemoryUsage()));
		influxDBReporter.register("jvm_memory", ImmutableMap.of("name", "NonHeap"), new Metric(mxBean.getNonHeapMemoryUsage()));
		for (MemoryPoolMXBean mxBean2 : ManagementFactory.getMemoryPoolMXBeans()) {
			influxDBReporter.register("jvm_memory", ImmutableMap.of("name", mxBean2.getName().replaceAll("[\\s]+", "")), new Metric(mxBean2.getUsage()));
		}
	}

	private static class Metric implements InfluxDBMetric {

		final MemoryUsage memoryUsage;

		Metric(MemoryUsage memoryUsage) {
			this.memoryUsage = memoryUsage;
		}

		@Override
		public Map<String, Object> getField() {
			Map<String, Object> fieldMap = new TreeMap<String, Object>();
			fieldMap.put("init", this.memoryUsage.getInit());
			fieldMap.put("used", this.memoryUsage.getUsed());
			fieldMap.put("committed", this.memoryUsage.getCommitted());
			fieldMap.put("max", this.memoryUsage.getMax());
			return fieldMap;
		}

		@Override
		public Map<String, Object> getAndResetField() {
			return this.getField();
		}
	}

}
