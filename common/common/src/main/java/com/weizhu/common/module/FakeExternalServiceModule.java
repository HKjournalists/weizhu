package com.weizhu.common.module;

import java.util.concurrent.Executor;

import javax.annotation.Nullable;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.weizhu.common.influxdb.InfluxDBReporter;
import com.weizhu.common.module.FakeAPNsServiceModule.FakeAPNsServiceImpl;
import com.weizhu.common.service.ServiceInvoker;
import com.weizhu.common.service.ServiceStub;
import com.weizhu.proto.AdminProtos.AdminAnonymousHead;
import com.weizhu.proto.AdminProtos.AdminHead;
import com.weizhu.proto.ExternalProtos.SendEmailRequest;
import com.weizhu.proto.ExternalProtos.SendEmailResponse;
import com.weizhu.proto.ExternalProtos.SendSmsRequest;
import com.weizhu.proto.ExternalProtos.SendSmsResponse;
import com.weizhu.proto.ExternalService;
import com.weizhu.proto.WeizhuProtos.AnonymousHead;

public class FakeExternalServiceModule extends AbstractModule {

	@Override
	protected void configure() {
		bind(FakeAPNsServiceImpl.class).in(Singleton.class);
	}
	
	@Provides
	@Singleton
	public ExternalService provideExternalService(FakeExternalServiceImpl serviceImpl, @Named("service_executor") Executor serviceExecutor) {
		return ServiceStub.createServiceApi(ExternalService.class, serviceImpl, serviceExecutor);
	}

	@Provides
	@Singleton
	@Named("ExternalService")
	public ServiceInvoker provideExternalServiceInvoker(FakeExternalServiceImpl serviceImpl, @Named("service_executor") Executor serviceExecutor, @Nullable InfluxDBReporter influxDBReporter) {
		return ServiceStub.createServiceInvoker(ExternalService.class, serviceImpl, serviceExecutor, influxDBReporter);
	}
	
	public static class FakeExternalServiceImpl implements ExternalService {

		@Override
		public ListenableFuture<SendSmsResponse> sendSms(AnonymousHead head, SendSmsRequest request) {
			return Futures.immediateFuture(SendSmsResponse.newBuilder().setResult(SendSmsResponse.Result.SUCC).build());
		}

		@Override
		public ListenableFuture<SendSmsResponse> sendSms(AdminHead head,
				SendSmsRequest request) {
			return Futures.immediateFuture(SendSmsResponse.newBuilder().setResult(SendSmsResponse.Result.SUCC).build());
		}

		@Override
		public ListenableFuture<SendEmailResponse> sendEmail( AdminAnonymousHead head, SendEmailRequest request) {
			return Futures.immediateFuture(SendEmailResponse.newBuilder().setResult(SendEmailResponse.Result.SUCC).build());
		}

		@Override
		public ListenableFuture<SendEmailResponse> sendEmail(AdminHead head, SendEmailRequest request) {
			return Futures.immediateFuture(SendEmailResponse.newBuilder().setResult(SendEmailResponse.Result.SUCC).build());
		}
	}

}
