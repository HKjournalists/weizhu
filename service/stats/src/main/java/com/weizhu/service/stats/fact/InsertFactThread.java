package com.weizhu.service.stats.fact;

import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Charsets;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.weizhu.common.db.DBUtil;
import com.weizhu.common.utils.JsonUtil;
import com.zaxxer.hikari.HikariDataSource;

public class InsertFactThread extends Thread {

	private static final Logger logger = LoggerFactory.getLogger(InsertFactThread.class);
	
	private final HikariDataSource hikariDataSource;
	private final String kafkaServer;
	private final InsertFactHandler handler;
	
	public InsertFactThread(
			HikariDataSource hikariDataSource,
			String kafkaServer,
			final InsertFactHandler handler
			) {
		this.hikariDataSource = hikariDataSource;
		this.kafkaServer = kafkaServer;
		this.handler = handler;
		
		this.setDaemon(false);
		this.setName("InsertFactThread-" + handler.kafkaGroupId());
		this.setUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
			
			@Override
			public void uncaughtException(Thread t, Throwable e) {
				logger.error("InsertFactThread " + handler.kafkaGroupId() + " uncaught exception", e);
			}
		});
	}
	
	private final AtomicBoolean isShutdown = new AtomicBoolean(false);
	public void shutdown() {
		this.isShutdown.set(true);
	}
	
	@Override
	public final void run() {
		if (Thread.currentThread() != this) {
			logger.info("InsertFactThread " + this.handler.kafkaGroupId() + " invalid run thread");
			return;
		}
		
		logger.info("InsertFactThread " + this.handler.kafkaGroupId() + " start");
		
		while (!this.isShutdown.get()) {
			try {
				Properties props = new Properties();
				props.put("bootstrap.servers", this.kafkaServer);
				props.put("group.id", this.handler.kafkaGroupId());
				props.put("auto.offset.reset", "earliest");
				// props.put("client.id", "demo-francislin");
				props.put("enable.auto.commit", "false");
				props.put("key.deserializer", "org.apache.kafka.common.serialization.ByteArrayDeserializer");
				props.put("value.deserializer", "org.apache.kafka.common.serialization.ByteArrayDeserializer");
				
				final KafkaConsumer<byte[], byte[]> kafkaConsumer = new KafkaConsumer<byte[], byte[]>(props);
				try {
					kafkaConsumer.subscribe(this.handler.kafkaTopicList());
					
					while(!this.isShutdown.get()) {
						ConsumerRecords<byte[], byte[]> records = kafkaConsumer.poll(1000L);
						int recordCount = records.count();
						if (recordCount > 0) {
							List<JsonObject> jsonList = new ArrayList<JsonObject>(recordCount);
							
							for (ConsumerRecord<byte[], byte[]> record : records) {
								try {
									JsonElement jsonElement = JsonUtil.JSON_PARSER.parse(new InputStreamReader(new ByteArrayInputStream(record.value()), Charsets.UTF_8));
									if (jsonElement != null) {
										if (jsonElement.isJsonObject()) {
											jsonList.add(jsonElement.getAsJsonObject());
										} else {
											logger.warn("not json object : " + record);
										}
									} else {
										logger.warn("parse null result : " + record);
									}
								} catch (JsonParseException e) {
									logger.warn("invalid json format : " + record, e);
								}
							}
							
							if (!jsonList.isEmpty()) {
								final String sql = this.handler.handleInsertSQL(jsonList);
								if (sql != null && !sql.isEmpty()) {
									Connection dbConn = null;
									Statement stmt = null;
									try {
										dbConn = this.hikariDataSource.getConnection();
										stmt = dbConn.createStatement();
										stmt.executeUpdate(sql);
									} finally {
										DBUtil.closeQuietly(stmt);
										DBUtil.closeQuietly(dbConn);
									}
								}
							}
						}
						
						kafkaConsumer.commitAsync();
					}
				} finally {
					kafkaConsumer.close();
				}
			} catch (Throwable th) {
				logger.error("InsertFactThread " + this.handler.kafkaGroupId() + " exception", th);
				if (!this.isShutdown.get()) {
					try {
						TimeUnit.SECONDS.sleep(10);
					} catch (InterruptedException e) {
						// ignore
					}
				}
			}
		}
		
		logger.info("InsertFactThread " + this.handler.kafkaGroupId() + " shutdown");
	}
}
