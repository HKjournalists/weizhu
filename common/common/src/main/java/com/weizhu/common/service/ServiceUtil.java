package com.weizhu.common.service;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.weizhu.proto.WeizhuProtos;
import com.weizhu.proto.AdminProtos.AdminAnonymousHead;
import com.weizhu.proto.AdminProtos.AdminHead;
import com.weizhu.proto.WeizhuProtos.AnonymousHead;
import com.weizhu.proto.WeizhuProtos.EmptyRequest;
import com.weizhu.proto.WeizhuProtos.EmptyResponse;
import com.weizhu.proto.WeizhuProtos.RequestHead;

public class ServiceUtil {

	public static final EmptyResponse EMPTY_RESPONSE = EmptyResponse.newBuilder().build();
	
	public static final EmptyRequest EMPTY_REQUEST = EmptyRequest.newBuilder().build();
	
	public static final ListenableFuture<EmptyResponse> EMPTY_RESPONSE_IMMEDIATE_FUTURE = Futures.immediateFuture(EMPTY_RESPONSE);
	
	public static AnonymousHead toAnonymousHead(RequestHead requestHead) {
		AnonymousHead.Builder builder = AnonymousHead.newBuilder()
				.setCompanyId(requestHead.getSession().getCompanyId())
				.setInvoke(requestHead.getInvoke())
				.setNetwork(requestHead.getNetwork());
		if (requestHead.hasWeizhu()) {
			builder.setWeizhu(requestHead.getWeizhu());
		}
		if (requestHead.hasAndroid()) {
			builder.setAndroid(requestHead.getAndroid());
		}
		if (requestHead.hasIphone()) {
			builder.setIphone(requestHead.getIphone());
		}
		if (requestHead.hasWebMobile()) {
			builder.setWebMobile(requestHead.getWebMobile());
		}
		if (requestHead.hasWebLogin()) {
			builder.setWebLogin(requestHead.getWebLogin());
		}
		return builder.build();
	}
	
	public static RequestHead toRequestHead(AnonymousHead anonymousHead, WeizhuProtos.Session session) {
		RequestHead.Builder builder = RequestHead.newBuilder()
				.setSession(session)
				.setInvoke(anonymousHead.getInvoke())
				.setNetwork(anonymousHead.getNetwork());
		if (anonymousHead.hasWeizhu()) {
			builder.setWeizhu(anonymousHead.getWeizhu());
		}
		if (anonymousHead.hasAndroid()) {
			builder.setAndroid(anonymousHead.getAndroid());
		}
		if (anonymousHead.hasIphone()) {
			builder.setIphone(anonymousHead.getIphone());
		}
		if (anonymousHead.hasWebMobile()) {
			builder.setWebMobile(anonymousHead.getWebMobile());
		}
		if (anonymousHead.hasWebLogin()) {
			builder.setWebLogin(anonymousHead.getWebLogin());
		}
		return builder.build();
	}
	
	public static AdminAnonymousHead toAdminAnonymousHead(AdminHead head) {
		AdminAnonymousHead.Builder builder = AdminAnonymousHead.newBuilder()
				.setRequestUri(head.getRequestUri())
				.setRemoteHost(head.getRemoteHost())
				.setUserAgent(head.getUserAgent());
		
		if (head.hasCompanyId()) {
			builder.setCompanyId(head.getCompanyId());
		}
		return builder.build();
	}
	
}
