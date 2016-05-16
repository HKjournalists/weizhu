package com.weizhu.service.external;

import java.util.Properties;
import java.util.concurrent.Executor;

import javax.annotation.Nullable;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.weizhu.common.influxdb.InfluxDBReporter;
import com.weizhu.common.service.ServiceInvoker;
import com.weizhu.common.service.ServiceStub;
import com.weizhu.common.utils.EmailUtil;
import com.weizhu.proto.ExternalService;

public class ExternalServiceModule extends AbstractModule {

	@Override
	protected void configure() {
		bind(ExternalServiceImpl.class).in(Singleton.class);
	}
	
	@Provides
	@Singleton
	public ExternalService provideExamService(ExternalServiceImpl serviceImpl, @Named("service_executor") Executor serviceExecutor) {
		return ServiceStub.createServiceApi(ExternalService.class, serviceImpl, serviceExecutor);
	}
	
	@Provides
	@Singleton
	@Named("ExternalService")
	public ServiceInvoker provideExamServiceInvoker(ExternalServiceImpl serviceImpl, @Named("service_executor") Executor serviceExecutor, @Nullable InfluxDBReporter influxDBReporter) {
		return ServiceStub.createServiceInvoker(ExternalService.class, serviceImpl, serviceExecutor, influxDBReporter);
	}
	
	@Provides
	@Singleton
	@Nullable
	@Named("external_sms_send_url")
	public String provideSmsSendUrl(@Named("server_conf") Properties confProperties) {
		boolean smsEnable = Boolean.parseBoolean(confProperties.getProperty("external_sms_enable"));
		if (!smsEnable) {
			return null;
		}
		return confProperties.getProperty("external_sms_send_url");
	}
	
	@Provides
	@Singleton
	@Nullable
	public EmailInfo provideEmailInfo(@Named("server_conf") Properties confProperties) {
		boolean emailEnable = Boolean.parseBoolean(confProperties.getProperty("external_email_enable"));
		if (!emailEnable) {
			return null;
		}
		
		String smtpHost = confProperties.getProperty("external_email_smtp_host");
		int smtpPort = Integer.parseInt(confProperties.getProperty("external_email_smtp_port"));
		final String username = confProperties.getProperty("external_email_username");
		final String password = confProperties.getProperty("external_email_password");
		boolean enableSsl = Boolean.parseBoolean(confProperties.getProperty("external_email_ssl_enable"));
		
		Properties props = new Properties();
		props.put("mail.smtp.host", smtpHost);
		props.put("mail.smtp.port", String.valueOf(smtpPort));
		props.put("mail.smtp.auth", "true");
		props.put("mail.smtp.starttls.enable", String.valueOf(enableSsl));
		if (enableSsl) {
			props.put("mail.smtp.socketFactory.port", String.valueOf(smtpPort));
			props.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
		}
		
		if (!EmailUtil.isValid(username)) {
			throw new RuntimeException("invalid email username : " + username);
		}

		try {
			return new EmailInfo(new InternetAddress(username), Session.getInstance(props, new javax.mail.Authenticator() {
				protected PasswordAuthentication getPasswordAuthentication() {
					return new PasswordAuthentication(username, password);
				}
			}));
		} catch (AddressException e) {
			throw new Error(e);
		}
	}
	
}
