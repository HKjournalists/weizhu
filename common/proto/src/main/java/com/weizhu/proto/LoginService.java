package com.weizhu.proto;

import com.google.common.util.concurrent.ListenableFuture;
import com.weizhu.proto.AdminProtos.AdminHead;
import com.weizhu.proto.LoginProtos.GetLoginSmsCodeRequest;
import com.weizhu.proto.LoginProtos.GetLoginSmsCodeResponse;
import com.weizhu.proto.LoginProtos.GetWebLoginByTokenRequest;
import com.weizhu.proto.LoginProtos.GetWebLoginByTokenResponse;
import com.weizhu.proto.LoginProtos.LoginAutoRequest;
import com.weizhu.proto.LoginProtos.LoginAutoResponse;
import com.weizhu.proto.LoginProtos.LoginBySmsCodeRequest;
import com.weizhu.proto.LoginProtos.LoginBySmsCodeResponse;
import com.weizhu.proto.LoginProtos.NotifyWebLoginByTokenRequest;
import com.weizhu.proto.LoginProtos.NotifyWebLoginByTokenResponse;
import com.weizhu.proto.LoginProtos.RegisterBySmsCodeRequest;
import com.weizhu.proto.LoginProtos.RegisterBySmsCodeResponse;
import com.weizhu.proto.LoginProtos.SendRegisterSmsCodeRequest;
import com.weizhu.proto.LoginProtos.SendRegisterSmsCodeResponse;
import com.weizhu.proto.LoginProtos.SendSmsCodeRequest;
import com.weizhu.proto.LoginProtos.SendSmsCodeResponse;
import com.weizhu.proto.LoginProtos.WebLoginByTokenRequest;
import com.weizhu.proto.LoginProtos.WebLoginByTokenResponse;
import com.weizhu.proto.WeizhuProtos.AnonymousHead;
import com.weizhu.proto.WeizhuProtos.EmptyRequest;
import com.weizhu.proto.WeizhuProtos.EmptyResponse;
import com.weizhu.proto.WeizhuProtos.RequestHead;

public interface LoginService {

	@WriteMethod
	@ResponseType(SendSmsCodeResponse.class)
	ListenableFuture<SendSmsCodeResponse> sendSmsCode(AnonymousHead head, SendSmsCodeRequest request);
	
	@WriteMethod
	@ResponseType(LoginBySmsCodeResponse.class)
	ListenableFuture<LoginBySmsCodeResponse> loginBySmsCode(AnonymousHead head, LoginBySmsCodeRequest request);
	
	@WriteMethod
	@ResponseType(LoginAutoResponse.class)
	ListenableFuture<LoginAutoResponse> loginAuto(AnonymousHead head, LoginAutoRequest request);
	
	@WriteMethod
	@ResponseType(EmptyResponse.class)
	ListenableFuture<EmptyResponse> logout(AnonymousHead head, EmptyRequest request);
	
	@WriteMethod
	@ResponseType(SendSmsCodeResponse.class)
	ListenableFuture<SendSmsCodeResponse> sendSmsCode(RequestHead head, SendSmsCodeRequest request);
	
	@WriteMethod
	@ResponseType(LoginBySmsCodeResponse.class)
	ListenableFuture<LoginBySmsCodeResponse> loginBySmsCode(RequestHead head, LoginBySmsCodeRequest request);
	
	@WriteMethod
	@ResponseType(EmptyResponse.class)
	ListenableFuture<EmptyResponse> logout(RequestHead head, EmptyRequest request);
	
	@ResponseType(GetLoginSmsCodeResponse.class)
	ListenableFuture<GetLoginSmsCodeResponse> getLoginSmsCode(AdminHead head, GetLoginSmsCodeRequest request);
	
	@WriteMethod
	@ResponseType(SendRegisterSmsCodeResponse.class)
	ListenableFuture<SendRegisterSmsCodeResponse> sendRegisterSmsCode(AnonymousHead head, SendRegisterSmsCodeRequest request);
	
	@WriteMethod
	@ResponseType(RegisterBySmsCodeResponse.class)
	ListenableFuture<RegisterBySmsCodeResponse> registerBySmsCode(AnonymousHead head, RegisterBySmsCodeRequest request);
	
	@WriteMethod
	@ResponseType(WebLoginByTokenResponse.class)
	ListenableFuture<WebLoginByTokenResponse> webLoginByToken(AnonymousHead head, WebLoginByTokenRequest request);
	
	@WriteMethod
	@ResponseType(EmptyResponse.class)
	ListenableFuture<EmptyResponse> webLogout(RequestHead head, EmptyRequest request);
	
	@ResponseType(GetWebLoginByTokenResponse.class)
	ListenableFuture<GetWebLoginByTokenResponse> getWebLoginByToken(RequestHead head, GetWebLoginByTokenRequest request);
	
	@WriteMethod
	@ResponseType(NotifyWebLoginByTokenResponse.class)
	ListenableFuture<NotifyWebLoginByTokenResponse> notifyWebLoginByToken(RequestHead head, NotifyWebLoginByTokenRequest request);
}
