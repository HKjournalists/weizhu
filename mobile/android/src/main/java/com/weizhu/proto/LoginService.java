package com.weizhu.proto;

import com.weizhu.network.Future;
import com.weizhu.proto.LoginProtos.LoginAutoRequest;
import com.weizhu.proto.LoginProtos.LoginAutoResponse;
import com.weizhu.proto.LoginProtos.LoginBySmsCodeRequest;
import com.weizhu.proto.LoginProtos.LoginBySmsCodeResponse;
import com.weizhu.proto.LoginProtos.SendSmsCodeRequest;
import com.weizhu.proto.LoginProtos.SendSmsCodeResponse;
import com.weizhu.proto.WeizhuProtos.EmptyRequest;
import com.weizhu.proto.WeizhuProtos.EmptyResponse;


public interface LoginService {

	@ResponseType(SendSmsCodeResponse.class)
	Future<SendSmsCodeResponse> sendSmsCode(SendSmsCodeRequest request, int priorityNum);
	
	@ResponseType(LoginBySmsCodeResponse.class)
	Future<LoginBySmsCodeResponse> loginBySmsCode(LoginBySmsCodeRequest request, int priorityNum);
	
	@ResponseType(LoginAutoResponse.class)
	Future<LoginAutoResponse> loginAuto(LoginAutoRequest request, int priorityNum);
	
	@ResponseType(EmptyResponse.class)
	Future<EmptyResponse> logout(EmptyRequest request, int priorityNum);
	
}
