package com.weizhu.proto;

import com.google.common.util.concurrent.ListenableFuture;
import com.weizhu.proto.AdminProtos.AdminAnonymousHead;
import com.weizhu.proto.AdminProtos.AdminHead;
import com.weizhu.proto.ExternalProtos.SendEmailRequest;
import com.weizhu.proto.ExternalProtos.SendEmailResponse;
import com.weizhu.proto.ExternalProtos.SendSmsRequest;
import com.weizhu.proto.ExternalProtos.SendSmsResponse;
import com.weizhu.proto.WeizhuProtos.AnonymousHead;

public interface ExternalService {

	@WriteMethod
	@ResponseType(SendSmsResponse.class)
	ListenableFuture<SendSmsResponse> sendSms(AnonymousHead head, SendSmsRequest request);

	@WriteMethod
	@ResponseType(SendSmsResponse.class)
	ListenableFuture<SendSmsResponse> sendSms(AdminHead head, SendSmsRequest request);
	
	@WriteMethod
	@ResponseType(SendEmailResponse.class)
	ListenableFuture<SendEmailResponse> sendEmail(AdminAnonymousHead head, SendEmailRequest request);
	
	@WriteMethod
	@ResponseType(SendEmailResponse.class)
	ListenableFuture<SendEmailResponse> sendEmail(AdminHead head, SendEmailRequest request);
}
