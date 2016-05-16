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
import com.weizhu.common.service.ServiceInvoker;
import com.weizhu.common.service.ServiceStub;
import com.weizhu.common.service.ServiceUtil;
import com.weizhu.proto.AdminProtos.AdminHead;
import com.weizhu.proto.PushProtos.GetOfflineMsgRequest;
import com.weizhu.proto.PushProtos.GetOfflineMsgResponse;
import com.weizhu.proto.PushProtos.PushMsgRequest;
import com.weizhu.proto.PushProtos.PushStateRequest;
import com.weizhu.proto.PushProtos.PushUserDeleteRequest;
import com.weizhu.proto.PushProtos.PushUserDisableRequest;
import com.weizhu.proto.PushProtos.PushUserExpireRequest;
import com.weizhu.proto.PushService;
import com.weizhu.proto.WeizhuProtos.EmptyRequest;
import com.weizhu.proto.WeizhuProtos.EmptyResponse;
import com.weizhu.proto.WeizhuProtos.RequestHead;
import com.weizhu.proto.WeizhuProtos.SystemHead;

public class FakePushServiceModule extends AbstractModule {

	@Override
	protected void configure() {
		bind(FakePushServiceImpl.class).in(Singleton.class);
	}
	
	@Provides
	@Singleton
	public PushService providePushService(FakePushServiceImpl serviceImpl, @Named("service_executor") Executor serviceExecutor) {
		return ServiceStub.createServiceApi(PushService.class, serviceImpl, serviceExecutor);
	}

	@Provides
	@Singleton
	@Named("PushService")
	public ServiceInvoker providePushServiceInvoker(FakePushServiceImpl serviceImpl, @Named("service_executor") Executor serviceExecutor, @Nullable InfluxDBReporter influxDBReporter) {
		return ServiceStub.createServiceInvoker(PushService.class, serviceImpl, serviceExecutor, influxDBReporter);
	}
	
	public static class FakePushServiceImpl implements PushService {

		@Override
		public ListenableFuture<EmptyResponse> pushMsg(RequestHead head, PushMsgRequest request) {
			return ServiceUtil.EMPTY_RESPONSE_IMMEDIATE_FUTURE;
		}
		
		@Override
		public ListenableFuture<EmptyResponse> pushMsg(AdminHead head, PushMsgRequest request) {
			return ServiceUtil.EMPTY_RESPONSE_IMMEDIATE_FUTURE;
		}
		
		@Override
		public ListenableFuture<EmptyResponse> pushMsg(SystemHead head, PushMsgRequest request) {
			return ServiceUtil.EMPTY_RESPONSE_IMMEDIATE_FUTURE;
		}

		@Override
		public ListenableFuture<EmptyResponse> pushState(RequestHead head, PushStateRequest request) {
			return ServiceUtil.EMPTY_RESPONSE_IMMEDIATE_FUTURE;
		}
		
		@Override
		public ListenableFuture<EmptyResponse> pushState(AdminHead head, PushStateRequest request) {
			return ServiceUtil.EMPTY_RESPONSE_IMMEDIATE_FUTURE;
		}
		
		@Override
		public ListenableFuture<EmptyResponse> pushState(SystemHead head, PushStateRequest request) {
			return ServiceUtil.EMPTY_RESPONSE_IMMEDIATE_FUTURE;
		}

		@Override
		public ListenableFuture<GetOfflineMsgResponse> getOfflineMsg(RequestHead head, GetOfflineMsgRequest request) {
			return Futures.immediateFuture(GetOfflineMsgResponse.newBuilder()
					.setPushSeq(0)
					.build());
		}

		@Override
		public ListenableFuture<EmptyResponse> pushUserDelete(AdminHead head, PushUserDeleteRequest request) {
			return ServiceUtil.EMPTY_RESPONSE_IMMEDIATE_FUTURE;
		}
		
		@Override
		public ListenableFuture<EmptyResponse> pushUserDisable(AdminHead head, PushUserDisableRequest request) {
			return ServiceUtil.EMPTY_RESPONSE_IMMEDIATE_FUTURE;
		}
		
		@Override
		public ListenableFuture<EmptyResponse> pushUserExpire(RequestHead head, PushUserExpireRequest request) {
			return ServiceUtil.EMPTY_RESPONSE_IMMEDIATE_FUTURE;
		}

		@Override
		public ListenableFuture<EmptyResponse> pushUserLogout(RequestHead head, EmptyRequest request) {
			return ServiceUtil.EMPTY_RESPONSE_IMMEDIATE_FUTURE;
		}
		
	}

}
