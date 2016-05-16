package com.weizhu.service.login;

import java.util.Properties;
import java.util.concurrent.Executor;

import javax.annotation.Nullable;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.weizhu.common.influxdb.InfluxDBReporter;
import com.weizhu.common.service.ServiceInvoker;
import com.weizhu.common.service.ServiceStub;
import com.weizhu.proto.LoginService;

public class LoginServiceModule extends AbstractModule {

	@Override
	protected void configure() {
		bind(LoginServiceImpl.class).in(Singleton.class);
	}

	@Provides
	@Named("login_fake_sms_code")
	public boolean provideLoginFakeSmsCode(@Named("server_conf") Properties confProperties) {
		return Boolean.valueOf(confProperties.getProperty("login_fake_sms_code"));
	}
	
	@Provides
	@Named("login_auto_login_enable") 
	public boolean provideLoginAutoLoginEnable(@Named("server_conf") Properties confProperties) {
		return Boolean.parseBoolean(confProperties.getProperty("login_auto_login_enable"));
	}

	@Provides
	@Singleton
	public LoginService provideLoginService(LoginServiceImpl serviceImpl, @Named("service_executor") Executor serviceExecutor) {
		return ServiceStub.createServiceApi(LoginService.class, serviceImpl, serviceExecutor);
	}

	@Provides
	@Singleton
	@Named("LoginService")
	public ServiceInvoker provideLoginServiceInvoker(LoginServiceImpl serviceImpl, @Named("service_executor") Executor serviceExecutor, @Nullable InfluxDBReporter influxDBReporter) {
		return ServiceStub.createServiceInvoker(LoginService.class, serviceImpl, serviceExecutor, influxDBReporter);
	}
}