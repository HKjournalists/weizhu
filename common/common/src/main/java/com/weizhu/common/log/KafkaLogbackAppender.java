package com.weizhu.common.log;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.StringReader;
import java.util.Properties;

import org.apache.kafka.clients.producer.Callback;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Charsets;
import com.google.gson.stream.JsonWriter;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.UnsynchronizedAppenderBase;

/**
样例配置
<?xml version="1.0" encoding="UTF-8"?>
<configuration>
	<appender name="kafka_test" class="com.weizhu.common.log.KafkaLogbackAppender">
		<kafkaProperties>
			bootstrap.servers=192.168.56.11:9092
			acks=1
			retries=1
			compression.type=snappy
		</kafkaProperties>
		<kafkaTopic>test</kafkaTopic>
		<isJsonMessage>true</isJsonMessage>
	</appender>
</configuration>
 */
public class KafkaLogbackAppender extends UnsynchronizedAppenderBase<ILoggingEvent> {
	
	private String kafkaProperties;
	private String kafkaTopic;
	private boolean isJsonMessage;

	@Override
	public void stop() {
		this.closeKafkaProducer();
		super.stop();
	}
	
	private Producer<byte[], byte[]> kafkaProducer = null;
	private Callback kafkaSendCallback = null;
	
	private synchronized Producer<byte[], byte[]> getKafkaProducer() {
		if (this.kafkaProducer != null) {
			return this.kafkaProducer;
		}
		
		Properties props = new Properties();
		try {
			props.load(new StringReader(this.kafkaProperties));
		} catch (IOException e) {
			throw new Error(e);
		}
		props.put("key.serializer", "org.apache.kafka.common.serialization.ByteArraySerializer");
		props.put("value.serializer", "org.apache.kafka.common.serialization.ByteArraySerializer");
		
		this.kafkaProducer = new KafkaProducer<byte[], byte[]>(props);
		
		return this.kafkaProducer;
	}
	
	private synchronized void closeKafkaProducer() {
		if (this.kafkaProducer == null) {
			return;
		}
		
		this.kafkaProducer.close();
		this.kafkaProducer = null;
		this.kafkaSendCallback = null;
	}
	
	private Callback getKafkaSendCallback() {
		Callback callback = this.kafkaSendCallback;
		if (callback != null) {
			return callback;
		}
		
		callback = new Callback() {

			private final Logger logger = LoggerFactory.getLogger(KafkaLogbackAppender.class);
			
			@Override
			public void onCompletion(RecordMetadata metadata, Exception exception) {
				if (exception != null) {
					logger.error("kafka log fail", exception);
				}
			}
			
		};
		
		this.kafkaSendCallback = callback;
		return callback;
	}
	
	@Override
	protected void append(ILoggingEvent eventObject) {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		
		JsonWriter jsonWriter = new JsonWriter(new OutputStreamWriter(out, Charsets.UTF_8));
		try {
			jsonWriter.setLenient(true);
			
			jsonWriter.beginObject();
			jsonWriter.name("timestamp");
			jsonWriter.value(eventObject.getTimeStamp());
			jsonWriter.name("level");
			jsonWriter.value(eventObject.getLevel().toString());
			jsonWriter.name("logger_name");
			jsonWriter.value(eventObject.getLoggerName());
			jsonWriter.name("message");
			
			if (this.isJsonMessage) {
				jsonWriter.jsonValue(eventObject.getMessage());
			} else {
				jsonWriter.value(eventObject.getMessage());
			}
			jsonWriter.endObject();
			jsonWriter.flush();
		} catch (IOException e) {
			throw new RuntimeException(e);
		} finally {
			try {
				jsonWriter.close();
			} catch (IOException e) {
				// ignore
			}
		}
		
		this.getKafkaProducer().send(new ProducerRecord<byte[], byte[]>(this.kafkaTopic, null, out.toByteArray()), this.getKafkaSendCallback());
	}
	
	public String getKafkaProperties() {
		return kafkaProperties;
	}

	public void setKafkaProperties(String kafkaProperties) {
		this.kafkaProperties = kafkaProperties;
	}

	public String getKafkaTopic() {
		return kafkaTopic;
	}

	public void setKafkaTopic(String kafkaTopic) {
		this.kafkaTopic = kafkaTopic;
	}

	public boolean getIsJsonMessage() {
		return isJsonMessage;
	}

	public void setIsJsonMessage(boolean isJsonMessage) {
		this.isJsonMessage = isJsonMessage;
	}

}
