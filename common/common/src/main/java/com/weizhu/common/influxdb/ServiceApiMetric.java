package com.weizhu.common.influxdb;

import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;

public class ServiceApiMetric implements InfluxDBMetric {

	private long count = 0L;
	private long duration = 0L;
	private long requestPacketSize = 0L;
	private long responsePacketSize = 0L;
	
	private long maxDuration = -1L;
	private long maxRequestPacketSize = -1L;
	private long maxResponsePacketSize = -1L;
	
	public synchronized void record(long durationMillis, int requestPacketSize, int responsePacketSize) { 
		if (durationMillis < 0 || requestPacketSize < 0 || responsePacketSize < 0) {
			return;
		}
		
		this.count++;
		this.duration += durationMillis;
		this.requestPacketSize += requestPacketSize;
		this.responsePacketSize += responsePacketSize;
		
		if (this.maxDuration == -1L || durationMillis > this.maxDuration) {
			this.maxDuration = durationMillis;
		}
		
		if (this.maxRequestPacketSize == -1L || requestPacketSize > this.maxRequestPacketSize) {
			this.maxRequestPacketSize = requestPacketSize;
		}
		if (this.maxResponsePacketSize == -1L || responsePacketSize > this.maxResponsePacketSize) {
			this.maxResponsePacketSize = responsePacketSize;
		}
	}
	
	@Override
	public synchronized Map<String, Object> getField() {
		if (this.count <= 0) {
			return Collections.emptyMap();
		}
		
		Map<String, Object> fieldMap = new TreeMap<String, Object>();
		fieldMap.put("count", this.count);
		fieldMap.put("duration", this.duration);
		fieldMap.put("requestPacketSize", this.requestPacketSize);
		fieldMap.put("responsePacketSize", this.responsePacketSize);
		fieldMap.put("maxDuration", this.maxDuration);
		fieldMap.put("maxRequestPacketSize", this.maxRequestPacketSize);
		fieldMap.put("maxResponsePacketSize", this.maxResponsePacketSize);
		return fieldMap;
	}

	@Override
	public synchronized Map<String, Object> getAndResetField() {
		if (this.count <= 0) {
			return Collections.emptyMap();
		}
		
		Map<String, Object> fieldMap = new TreeMap<String, Object>();
		fieldMap.put("count", this.count);
		fieldMap.put("duration", this.duration);
		fieldMap.put("requestPacketSize", this.requestPacketSize);
		fieldMap.put("responsePacketSize", this.responsePacketSize);
		fieldMap.put("maxDuration", this.maxDuration);
		fieldMap.put("maxRequestPacketSize", this.maxRequestPacketSize);
		fieldMap.put("maxResponsePacketSize", this.maxResponsePacketSize);
		
		this.count = 0L;
		this.duration = 0L;
		this.requestPacketSize = 0L;
		this.responsePacketSize = 0L;
		this.maxDuration = -1L;
		this.maxRequestPacketSize = -1L;
		this.maxResponsePacketSize = -1L;
		
		return fieldMap;
	}

}
