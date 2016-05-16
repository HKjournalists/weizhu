package com.weizhu.common.influxdb;

import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import org.influxdb.InfluxDB;
import org.influxdb.InfluxDBFactory;
import org.influxdb.InfluxDB.ConsistencyLevel;
import org.influxdb.dto.BatchPoints;
import org.influxdb.dto.Point;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import com.google.common.util.concurrent.ThreadFactoryBuilder;


public class InfluxDBReporter {
	
	private static final Logger logger = LoggerFactory.getLogger(InfluxDBReporter.class);

	private final InfluxDB influxDB;
	private final String database;
	private final String retentionPolicy;
	private final ConsistencyLevel consistencyLevel;
	private final ImmutableMap<String, String> tagMap;
	private final ScheduledThreadPoolExecutor executor;
	
	public InfluxDBReporter(String url, String username, String password, 
			String database, String retentionPolicy, ConsistencyLevel consistencyLevel, 
			ImmutableMap<String, String> tagMap
			) {
		this.influxDB = InfluxDBFactory.connect(url, username, password);
		// this.influxDB.setLogLevel(org.influxdb.InfluxDB.LogLevel.FULL);
		this.database = database;
		this.retentionPolicy = retentionPolicy;
		this.consistencyLevel = consistencyLevel;
		this.tagMap = tagMap;
		
		ThreadFactory threadFactory = new ThreadFactoryBuilder().setNameFormat("influxDB-reporter-%d").build();
		this.executor = new ScheduledThreadPoolExecutor(1, threadFactory);
		this.executor.setContinueExistingPeriodicTasksAfterShutdownPolicy(false);
		this.executor.setExecuteExistingDelayedTasksAfterShutdownPolicy(false);
	}

	// measurement, tags => fields
	private final ConcurrentMap<String, ConcurrentMap<ImmutableMap<String, String>, InfluxDBMetric>> measurementMap = 
			new ConcurrentHashMap<String, ConcurrentMap<ImmutableMap<String, String>, InfluxDBMetric>>();
	
	public <T extends InfluxDBMetric> T register(String measurement, ImmutableMap<String, String> tagMap, T metric) {
		Preconditions.checkNotNull(measurement, "measurement is null");
		Preconditions.checkNotNull(tagMap, "tagMap is null");
		Preconditions.checkNotNull(metric, "metric is null");
		
		measurement = measurement.trim();
		Preconditions.checkArgument(!measurement.isEmpty(), "measurement is empty");
		
		ConcurrentMap<ImmutableMap<String, String>, InfluxDBMetric> tagsToMetricMap = this.measurementMap.get(measurement);
		if (tagsToMetricMap == null) {
			tagsToMetricMap = new ConcurrentHashMap<ImmutableMap<String, String>, InfluxDBMetric>();
			ConcurrentMap<ImmutableMap<String, String>, InfluxDBMetric> tmp = this.measurementMap.putIfAbsent(measurement, tagsToMetricMap);
			if (tmp != null) {
				tagsToMetricMap = tmp;
			}
		}
		
		InfluxDBMetric existing = tagsToMetricMap.putIfAbsent(tagMap, metric);
		if (existing != null) {
			throw new IllegalArgumentException(measurement + ", " + tagMap + " metric is already exist");
		}		
		return metric;
	}
	
	public ServiceApiMetric serviceApiMetric(String measurement, String serviceName, String functionName, boolean isException, String type) {
		ConcurrentMap<ImmutableMap<String, String>, InfluxDBMetric> tagsToMetricMap = this.measurementMap.get(measurement);
		if (tagsToMetricMap == null) {
			tagsToMetricMap = new ConcurrentHashMap<ImmutableMap<String, String>, InfluxDBMetric>();
			ConcurrentMap<ImmutableMap<String, String>, InfluxDBMetric> tmp = this.measurementMap.putIfAbsent(measurement, tagsToMetricMap);
			if (tmp != null) {
				tagsToMetricMap = tmp;
			}
		}
		
		ImmutableMap<String, String> tagMap = ImmutableMap.of("service", serviceName, "function", functionName, "exception", String.valueOf(isException), "type", type);
		InfluxDBMetric metric = tagsToMetricMap.get(tagMap);
		if (metric == null) {
			metric = new ServiceApiMetric();
			InfluxDBMetric existing = tagsToMetricMap.putIfAbsent(tagMap, metric);
			if (existing != null) {
				metric = existing;
			}
		}
		return (ServiceApiMetric) metric;
	}
	
	private int report() {
		long now = System.currentTimeMillis();
		int count = 0;
		BatchPoints batchPoints = null;
		for (Entry<String, ConcurrentMap<ImmutableMap<String, String>, InfluxDBMetric>> entry0 : measurementMap.entrySet()) {
			final String measurement = entry0.getKey();
			for (Entry<ImmutableMap<String, String>, InfluxDBMetric> entry1 : entry0.getValue().entrySet()) {
				final ImmutableMap<String, String> tagMap = entry1.getKey();
				final Map<String, Object> fieldMap = entry1.getValue().getAndResetField();
				
				if (fieldMap != null && !fieldMap.isEmpty()) {
					count++;
					Point point = Point.measurement(measurement)
		                    .time(now, TimeUnit.MILLISECONDS)
		                    .tag(this.tagMap)
		                    .tag(tagMap)
		                    .fields(fieldMap)
		                    .useInteger(true)
		                    .build();
					
					if (batchPoints == null) {
						batchPoints = BatchPoints
								.database(this.database)
								.retentionPolicy(this.retentionPolicy)
								.consistency(this.consistencyLevel)
								.build();
					}
					
					batchPoints.point(point);
					
					if (batchPoints.getPoints().size() > 200) {
						this.influxDB.write(batchPoints);
						batchPoints = null;
					}
				}
			}
		}
		
		if (batchPoints != null) {
			this.influxDB.write(batchPoints);
		}
		return count;
	}
	
	private ScheduledFuture<?> future = null;
	
	public synchronized void start(long period, TimeUnit unit) {
		Preconditions.checkArgument(period > 0, "period");
		Preconditions.checkNotNull(unit, unit);
		
		if (this.future != null) {
			return;
		}
		this.future = this.executor.scheduleAtFixedRate(new Runnable() {

			@Override
			public void run() {
				try {
					int cnt = InfluxDBReporter.this.report();
					logger.info("influxDB report " + cnt + " row");
				} catch (Throwable th) {
					logger.error("influxDB report fail", th);
				}
			}
			
		}, period, period, unit);
		
		logger.info("influxDB reporter start. period : " + unit.toMillis(period) + "(ms)");
	}
	
	public synchronized void stop() {
		if (this.future != null) {
			this.future.cancel(false);
			this.executor.shutdown();
			this.report();
			this.future = null;
			
			logger.info("influxDB reporter stop");
		}
	}
	
	
	public static void main(String args[]) throws InterruptedException {
		
		InfluxDBReporter reporter = new InfluxDBReporter("http://weizhu-monitor:8086", "root", "root", "test", "default", ConsistencyLevel.ANY, ImmutableMap.of("server", "local"));
		
		JvmGarbageCollectorMetrics.registerAll(reporter);
		JvmMemoryUsageMetrics.registerAll(reporter);
		
		reporter.start(10, TimeUnit.SECONDS);
		
		for (int i=0; i<100; ++i) {
			TimeUnit.SECONDS.sleep(3);
			System.gc();
		}
		
		reporter.stop();
	}
	
}
