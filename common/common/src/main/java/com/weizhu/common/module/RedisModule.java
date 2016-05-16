package com.weizhu.common.module;

import java.util.Properties;

import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.multibindings.ProvidesIntoSet;
import com.google.inject.name.Named;
import com.weizhu.common.server.ServerEntry;

public final class RedisModule extends AbstractModule {

	@Override
	protected void configure() {
	}
	
	@Provides
	@Singleton
	public JedisPool provideJedisPool(@Named("server_conf") Properties confProperties) {
		String redisHost = confProperties.getProperty("redis_host");
		Integer redisPort = Integer.parseInt(confProperties.getProperty("redis_port"));
		Integer redisTimeout = Integer.parseInt(confProperties.getProperty("redis_timeout"));
		String redisPassword = confProperties.getProperty("redis_password");
		Integer redisDatabase = Integer.parseInt(confProperties.getProperty("redis_database"));
		return new JedisPool(new JedisPoolConfig(), redisHost, redisPort, redisTimeout, redisPassword, redisDatabase);
	}
	
	@ProvidesIntoSet
	public ServerEntry.ShutdownHook provideShutdownHook(final JedisPool jedisPool) {
		return new ServerEntry.ShutdownHook() {

			@Override
			public Order order() {
				return ServerEntry.ShutdownHook.Order.RESOURCE;
			}

			@Override
			public void execute() {
				jedisPool.close();
			}
			
		};
	}

}
