package com.weizhu.service.external;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Joiner;
import com.google.common.io.CharStreams;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.inject.name.Named;
import com.weizhu.common.utils.EmailUtil;
import com.weizhu.common.utils.MobileNoUtil;
import com.weizhu.proto.AdminProtos.AdminAnonymousHead;
import com.weizhu.proto.AdminProtos.AdminHead;
import com.weizhu.proto.ExternalProtos.SendEmailRequest;
import com.weizhu.proto.ExternalProtos.SendEmailResponse;
import com.weizhu.proto.ExternalProtos.SendSmsRequest;
import com.weizhu.proto.ExternalProtos.SendSmsResponse;
import com.weizhu.proto.ExternalService;
import com.weizhu.proto.WeizhuProtos.AnonymousHead;

public class ExternalServiceImpl implements ExternalService {

	private static final Logger logger = LoggerFactory.getLogger(ExternalServiceImpl.class);
	
	private final EmailInfo emailInfo;
	private final String smsSendUrl;
	
	@Inject
	public ExternalServiceImpl(@Nullable EmailInfo emailInfo, 
			@Named("external_sms_send_url") @Nullable String smsSendUrl
			) {
		this.emailInfo = emailInfo;
		this.smsSendUrl = smsSendUrl;
	}
	
	private static final Joiner COMMA_JOINER = Joiner.on(',').skipNulls();
	
	private ListenableFuture<SendSmsResponse> doSendSms(SendSmsRequest request) {
		if (request.getMobileNoCount() <= 0) {
			return Futures.immediateFuture(SendSmsResponse.newBuilder()
					.setResult(SendSmsResponse.Result.FAIL_MOBILE_NO_INVALID)
					.setFailText("mobile no is empty")
					.build());
		}
		
		List<String> invalidMobileNoList = new ArrayList<String>();
		for (String mobileNo : request.getMobileNoList()) {
			if (!MobileNoUtil.isValid(mobileNo)) {
				invalidMobileNoList.add(mobileNo);
			}
		}
		if (!invalidMobileNoList.isEmpty()) {
			return Futures.immediateFuture(SendSmsResponse.newBuilder()
					.setResult(SendSmsResponse.Result.FAIL_MOBILE_NO_INVALID)
					.setFailText("mobile no is invalid : " + invalidMobileNoList)
					.build());
		}
		
		if (request.getSmsText().isEmpty()) {
			return Futures.immediateFuture(SendSmsResponse.newBuilder()
					.setResult(SendSmsResponse.Result.FAIL_SMS_TEXT_INVALID)
					.setFailText("sms text is empty")
					.build());
		}
		if (request.getSmsText().length() > 122) {
			return Futures.immediateFuture(SendSmsResponse.newBuilder()
					.setResult(SendSmsResponse.Result.FAIL_SMS_TEXT_INVALID)
					.setFailText("sms text max text length 122. " + request.getSmsText().length())
					.build());
		}
		
		if (this.smsSendUrl == null) {
			return Futures.immediateFuture(SendSmsResponse.newBuilder()
					.setResult(SendSmsResponse.Result.SUCC)
					.build());
		}
		
		String responseText = null;
		try {
			URL sendUrl = new URL(this.smsSendUrl.
					replace("${mobile_no}", COMMA_JOINER.join(request.getMobileNoList())).
					replace("${sms_text}", URLEncoder.encode(request.getSmsText(), "GBK")));
			
			InputStream in = null;
			try {
				URLConnection conn = sendUrl.openConnection();
				conn.setConnectTimeout(3000);
				conn.setReadTimeout(10000);
				conn.connect();
				in = conn.getInputStream();
				responseText = CharStreams.toString(new InputStreamReader(in, "GBK"));
			} finally {
				if (in != null) {
					try {
						in.close();
					} catch (Throwable th) {
						// ignore
					}
				}
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		
		logger.info("sendSms|" + request.getMobileNoList() + "|" + request.getSmsText() + "|" + responseText );
		
		return Futures.immediateFuture(SendSmsResponse.newBuilder()
				.setResult(SendSmsResponse.Result.SUCC)
				.build());
	}
	
	@Override
	public ListenableFuture<SendSmsResponse> sendSms(AnonymousHead head, SendSmsRequest request) {
		return this.doSendSms(request);
	}
	
	@Override
	public ListenableFuture<SendSmsResponse> sendSms(AdminHead head, SendSmsRequest request) {
		return this.doSendSms(request);
	}
	
	private ListenableFuture<SendEmailResponse> doSendEmail(SendEmailRequest request) {
		if (request.getToRecipientsCount() <= 0) {
			return Futures.immediateFuture(SendEmailResponse.newBuilder()
					.setResult(SendEmailResponse.Result.FAIL_RECIPIENTS_INVALID)
					.setFailText("to recipients is empty")
					.build());
		}
		for (String email : request.getToRecipientsList()) {
			if (!EmailUtil.isValid(email)) {
				return Futures.immediateFuture(SendEmailResponse.newBuilder()
						.setResult(SendEmailResponse.Result.FAIL_RECIPIENTS_INVALID)
						.setFailText("to recipients is invalid format : " + email)
						.build());
			}
		}
		for (String email : request.getCcRecipientsList()) {
			if (!EmailUtil.isValid(email)) {
				return Futures.immediateFuture(SendEmailResponse.newBuilder()
						.setResult(SendEmailResponse.Result.FAIL_RECIPIENTS_INVALID)
						.setFailText("cc recipients is invalid format : " + email)
						.build());
			}
		}
		
		final String subject = request.getSubject().trim();
		if (subject.isEmpty()) {
			return Futures.immediateFuture(SendEmailResponse.newBuilder()
					.setResult(SendEmailResponse.Result.FAIL_SUBJECT_INVALID)
					.setFailText("subject is empty")
					.build());
		}
		if (subject.length() > 1024) {
			return Futures.immediateFuture(SendEmailResponse.newBuilder()
					.setResult(SendEmailResponse.Result.FAIL_SUBJECT_INVALID)
					.setFailText("subject is too long. max length is 1024")
					.build());
		}
		
		if (this.emailInfo == null) {
			return Futures.immediateFuture(SendEmailResponse.newBuilder()
					.setResult(SendEmailResponse.Result.SUCC)
					.build());
		}
		
		try {
			Message message = new MimeMessage(this.emailInfo.session());
			message.setFrom(this.emailInfo.from());
			for (String addr : request.getToRecipientsList()) {
				message.addRecipients(Message.RecipientType.TO, InternetAddress.parse(addr));
			}
			for (String addr : request.getCcRecipientsList()) {
				message.addRecipients(Message.RecipientType.CC, InternetAddress.parse(addr));
			}
			message.setSubject(subject);
			message.setContent(request.getHtmlContent(), "text/html;charset=utf-8");
			
			Transport.send(message);
			
		} catch (MessagingException e) {
			throw new RuntimeException("send mail error", e);
		}
		
		return Futures.immediateFuture(SendEmailResponse.newBuilder()
				.setResult(SendEmailResponse.Result.SUCC)
				.build());
	}
	
	@Override
	public ListenableFuture<SendEmailResponse> sendEmail(AdminAnonymousHead head, SendEmailRequest request) {
		return this.doSendEmail(request);
	}

	@Override
	public ListenableFuture<SendEmailResponse> sendEmail(AdminHead head, SendEmailRequest request) {
		return this.doSendEmail(request);
	}
	
}
