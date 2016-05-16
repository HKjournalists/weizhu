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
import com.weizhu.proto.AdminProtos.AdminHead;
import com.weizhu.proto.BossProtos.BossHead;
import com.weizhu.proto.ProfileProtos.GetProfileRequest;
import com.weizhu.proto.ProfileProtos.GetProfileResponse;
import com.weizhu.proto.ProfileProtos.SetProfileRequest;
import com.weizhu.proto.ProfileProtos.SetProfileResponse;
import com.weizhu.proto.ProfileService;
import com.weizhu.proto.WeizhuProtos.AnonymousHead;
import com.weizhu.proto.WeizhuProtos.RequestHead;
import com.weizhu.proto.WeizhuProtos.SystemHead;

public class FakeProfileServiceModule extends AbstractModule {

	@Override
	protected void configure() {
		bind(FakeProfileServiceImpl.class).in(Singleton.class);
	}
	
	@Provides
	@Singleton
	public ProfileService provideSurveyService(FakeProfileServiceImpl serviceImpl, @Named("service_executor") Executor serviceExecutor) {
		return ServiceStub.createServiceApi(ProfileService.class, serviceImpl, serviceExecutor);
	}
	
	@Provides
	@Singleton
	@Named("ProfileService")
	public ServiceInvoker provideSurveyServiceInvoker(FakeProfileServiceImpl serviceImpl, @Named("service_executor") Executor serviceExecutor, @Nullable InfluxDBReporter influxDBReporter) {
		return ServiceStub.createServiceInvoker(ProfileService.class, serviceImpl, serviceExecutor, influxDBReporter);
	}
	
	public static class FakeProfileServiceImpl implements ProfileService {

		private static final ListenableFuture<GetProfileResponse> EMPTY_RESPONSE = Futures.immediateFuture(GetProfileResponse.newBuilder().build());
		
		@Override
		public ListenableFuture<GetProfileResponse> getProfile(AnonymousHead head, GetProfileRequest request) {
			return EMPTY_RESPONSE;
		}

		@Override
		public ListenableFuture<GetProfileResponse> getProfile(RequestHead head, GetProfileRequest request) {
			return EMPTY_RESPONSE;
		}

		@Override
		public ListenableFuture<GetProfileResponse> getProfile(AdminHead head, GetProfileRequest request) {
			return EMPTY_RESPONSE;
		}

		@Override
		public ListenableFuture<GetProfileResponse> getProfile(BossHead head, GetProfileRequest request) {
			return EMPTY_RESPONSE;
		}
		
		@Override
		public ListenableFuture<GetProfileResponse> getProfile(SystemHead head, GetProfileRequest request) {
			return EMPTY_RESPONSE;
		}

		@Override
		public ListenableFuture<SetProfileResponse> setProfile(AdminHead head, SetProfileRequest request) {
			return Futures.immediateFuture(SetProfileResponse.newBuilder()
					.setResult(SetProfileResponse.Result.SUCC)
					.build());
		}

		@Override
		public ListenableFuture<SetProfileResponse> setProfile(BossHead head, SetProfileRequest request) {
			return Futures.immediateFuture(SetProfileResponse.newBuilder()
					.setResult(SetProfileResponse.Result.SUCC)
					.build());
		}
		
	}

}
