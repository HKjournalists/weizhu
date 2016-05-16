package com.weizhu.common.module;

import java.util.concurrent.Executor;

import javax.annotation.Nullable;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.weizhu.common.influxdb.InfluxDBReporter;
import com.weizhu.common.service.ServiceInvoker;
import com.weizhu.common.service.ServiceStub;
import com.weizhu.common.service.ServiceUtil;
import com.weizhu.proto.APNsProtos.DeleteDeviceTokenExpireRequest;
import com.weizhu.proto.APNsProtos.DeleteDeviceTokenRequest;
import com.weizhu.proto.APNsProtos.SendNotificationRequest;
import com.weizhu.proto.APNsProtos.UpdateDeviceTokenRequest;
import com.weizhu.proto.APNsService;
import com.weizhu.proto.AdminProtos.AdminHead;
import com.weizhu.proto.WeizhuProtos.EmptyRequest;
import com.weizhu.proto.WeizhuProtos.EmptyResponse;
import com.weizhu.proto.WeizhuProtos.RequestHead;
import com.weizhu.proto.WeizhuProtos.SystemHead;

public class FakeAPNsServiceModule extends AbstractModule {

	@Override
	protected void configure() {
		bind(FakeAPNsServiceImpl.class).in(Singleton.class);
	}
	
	@Provides
	@Singleton
	public APNsService provideAPNsService(FakeAPNsServiceImpl serviceImpl, @Named("service_executor") Executor serviceExecutor) {
		return ServiceStub.createServiceApi(APNsService.class, serviceImpl, serviceExecutor);
	}

	@Provides
	@Singleton
	@Named("APNsService")
	public ServiceInvoker provideAPNsServiceInvoker(FakeAPNsServiceImpl serviceImpl, @Named("service_executor") Executor serviceExecutor, @Nullable InfluxDBReporter influxDBReporter) {
		return ServiceStub.createServiceInvoker(APNsService.class, serviceImpl, serviceExecutor, influxDBReporter);
	}
	
	public static class FakeAPNsServiceImpl implements APNsService {

		@Override
		public ListenableFuture<EmptyResponse> updateDeviceToken(RequestHead head, UpdateDeviceTokenRequest request) {
			return ServiceUtil.EMPTY_RESPONSE_IMMEDIATE_FUTURE;
		}

		@Override
		public ListenableFuture<EmptyResponse> deleteDeviceToken(AdminHead head, DeleteDeviceTokenRequest request) {
			return ServiceUtil.EMPTY_RESPONSE_IMMEDIATE_FUTURE;
		}

		@Override
		public ListenableFuture<EmptyResponse> deleteDeviceTokenExpire(RequestHead head, DeleteDeviceTokenExpireRequest request) {
			return ServiceUtil.EMPTY_RESPONSE_IMMEDIATE_FUTURE;
		}

		@Override
		public ListenableFuture<EmptyResponse> deleteDeviceTokenLogout(RequestHead head, EmptyRequest request) {
			return ServiceUtil.EMPTY_RESPONSE_IMMEDIATE_FUTURE;
		}

		@Override
		public ListenableFuture<EmptyResponse> sendNotification(RequestHead head, SendNotificationRequest request) {
			return ServiceUtil.EMPTY_RESPONSE_IMMEDIATE_FUTURE;
		}
		
		@Override
		public ListenableFuture<EmptyResponse> sendNotification(AdminHead head, SendNotificationRequest request) {
			return ServiceUtil.EMPTY_RESPONSE_IMMEDIATE_FUTURE;
		}

		@Override
		public ListenableFuture<EmptyResponse> sendNotification(SystemHead head, SendNotificationRequest request) {
			return ServiceUtil.EMPTY_RESPONSE_IMMEDIATE_FUTURE;
		}
		
	}

}
