package com.weizhu.common.module;

import java.io.IOException;
import java.util.Properties;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

import com.google.common.io.Resources;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.weizhu.common.service.ServiceThread;
import com.weizhu.common.service.ServiceUtil;
import com.weizhu.proto.AdminProtos.AdminAnonymousHead;
import com.weizhu.proto.AdminProtos.AdminHead;
import com.weizhu.proto.AdminProtos.AdminSession;
import com.weizhu.proto.BossProtos.BossAnonymousHead;
import com.weizhu.proto.BossProtos.BossHead;
import com.weizhu.proto.BossProtos.BossSession;
import com.weizhu.proto.WeizhuProtos.Android;
import com.weizhu.proto.WeizhuProtos.AnonymousHead;
import com.weizhu.proto.WeizhuProtos.Invoke;
import com.weizhu.proto.WeizhuProtos.Network;
import com.weizhu.proto.WeizhuProtos.RequestHead;
import com.weizhu.proto.WeizhuProtos.Session;
import com.weizhu.proto.WeizhuProtos.Weizhu;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

/**
 * test 对应的资源
 * @author lindongjlu
 *
 */
public class TestModule extends AbstractModule {

	@Override
	protected void configure() {
		install(new InfluxDBReporterModule());
	}
	
	@Provides
	@Singleton
	public AdminHead provideAdminHead() {
		return AdminHead.newBuilder()
				.setCompanyId(0)
				.setSession(AdminSession.newBuilder()
						.setAdminId(1)
						.setSessionId(1)
						.build())
				.setRequestUri("/admin/json/")
				.setUserAgent("Mozilla/5.0 (Macintosh; Intel Mac OS X 10_10_3) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/41.0.2272.118 Safari/537.36")
				.setRemoteHost("127.0.0.1")
				.build();
	}
	
	@Provides
	@Singleton
	public AdminAnonymousHead provideAdminAnonymousHead(AdminHead adminHead) {
		return ServiceUtil.toAdminAnonymousHead(adminHead);
	}
	
	@Provides
	@Singleton
	public RequestHead provideRequestHead() {
		return RequestHead.newBuilder()
				.setSession(Session.newBuilder().setCompanyId(0).setUserId(10000124196L).setSessionId(0))
				.setInvoke(Invoke.newBuilder().setInvokeId(0).setServiceName("TestService").setFunctionName("testFunction"))
				.setNetwork(Network.newBuilder()
						.setType(Network.Type.WIFI)
						.setProtocol(Network.Protocol.HTTP_PB)
						.setRemoteHost("127.0.0.1")
						.setRemotePort(8080))
				.setWeizhu(Weizhu.newBuilder()
						.setPlatform(Weizhu.Platform.ANDROID)
						.setVersionName("1.0.0")
						.setVersionCode(0)
						.setStage(Weizhu.Stage.ALPHA)
						.setBuildTime((int)(System.currentTimeMillis()/1000L)))
				.setAndroid(Android.newBuilder()
						.setDevice("device")
						.setManufacturer("LGE")
						.setBrand("google")
						.setModel("Nexus 5")
						.setSerial("02c288c1f0a697d9")
						.setRelease("4.4.4")
						.setSdkInt(19)
						.setCodename("REL"))
				.build();
	}
	
	@Provides
	@Singleton
	public AnonymousHead provideAnonymousHead(RequestHead requestHead) {
		return ServiceUtil.toAnonymousHead(requestHead);
	}
	
	@Provides
	@Singleton
	public BossHead provideBossHead() {
		return BossHead.newBuilder()
				.setSession(BossSession.newBuilder()
						.setBossId("francislin")
						.setSessionId(123456)
						.build())
				.setRequestUri("/boss/json/")
				.setUserAgent("Mozilla/5.0 (Macintosh; Intel Mac OS X 10_10_3) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/41.0.2272.118 Safari/537.36")
				.setRemoteHost("127.0.0.1")
				.build();
	}
	
	@Provides
	@Singleton
	public BossAnonymousHead provideBossAnonymousHead() {
		return BossAnonymousHead.newBuilder()
				.setRequestUri("/boss/json/")
				.setUserAgent("Mozilla/5.0 (Macintosh; Intel Mac OS X 10_10_3) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/41.0.2272.118 Safari/537.36")
				.setRemoteHost("127.0.0.1")
				.build();
	}
	
	@Provides
	@Singleton
	@Named("service_executor")
	public Executor provideServiceExecutor() {
		return Executors.newCachedThreadPool(new ServiceThread.Factory());
	}
	
	@Provides
	@Singleton
	@Named("service_scheduled_executor")
	public ScheduledExecutorService provideScheduledExecutorService() {
		return Executors.newScheduledThreadPool(1);
	}

	@Provides
	@Singleton
	public JedisPool provideJedisPool() {
		return new JedisPool(new JedisPoolConfig(), "127.0.0.1", 6379, 2000, null, 0);
	}
	
	@Provides
	@Singleton
	public HikariDataSource provideHikariDataSource() {
		HikariConfig config = new HikariConfig();
		config.setJdbcUrl("jdbc:mysql://127.0.0.1:3306/weizhu_test?useUnicode=true&characterEncoding=utf8&allowMultiQueries=true&autoReconnect=true&failOverReadOnly=false&useSSL=false");
		config.setUsername("root");
		// config.setPassword("");
		config.setMaximumPoolSize(3);
		config.addDataSourceProperty("cachePrepStmts", "true");
		config.addDataSourceProperty("prepStmtCacheSize", "250");
		config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
		config.addDataSourceProperty("useServerPrepStmts", "true");
		return new HikariDataSource(config);
	}
	
	@Provides
	@Singleton
	@Named("server_conf")
	public Properties provideConfProperties() {
		Properties confProperties = new Properties();
		try {
			confProperties.load(Resources.getResource("com/weizhu/common/module/test.conf").openStream());
		} catch (IOException e) {
			throw new Error(e);
		}
		return confProperties;
	}
	
}
