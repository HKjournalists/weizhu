package com.weizhu.service.stats.test.kafka;

import java.util.Arrays;
import java.util.Properties;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;

public class ConsumerTest {

	public static void main(String[] args) {
		Properties props = new Properties();
		props.put("bootstrap.servers", "192.168.56.11:9092");
		props.put("group.id", "test2");
		props.put("auto.offset.reset", "earliest");
		// props.put("client.id", "demo-francislin");
		props.put("enable.auto.commit", "true");
		props.put("auto.commit.interval.ms", "1000");
		props.put("session.timeout.ms", "30000");
		props.put("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
		props.put("value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
		KafkaConsumer<String, String> consumer = new KafkaConsumer<String, String>(props);
		try {
			consumer.subscribe(Arrays.asList("test3"));
			while (true) {
				ConsumerRecords<String, String> records = consumer.poll(100);
				for (ConsumerRecord<String, String> record : records) {
					System.out.println(">>>>> " + record);
				}
			}
		} finally {
			consumer.close();
		}
	}

}
