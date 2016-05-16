package com.weizhu.server.api;

import java.io.IOException;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;

import javax.servlet.AsyncContext;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Charsets;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.google.common.util.concurrent.AsyncFunction;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Message;
import com.weizhu.common.server.LogUtil;
import com.weizhu.common.service.ServiceInvoker;
import com.weizhu.common.service.exception.HeadUnknownException;
import com.weizhu.common.service.exception.InvokeUnknownException;
import com.weizhu.proto.LoginProtos.LoginAutoRequest;
import com.weizhu.proto.SessionProtos.VerifySessionKeyRequest;
import com.weizhu.proto.SessionProtos.VerifySessionKeyResponse;
import com.weizhu.proto.SessionService;
import com.weizhu.proto.WeizhuProtos.AnonymousHead;
import com.weizhu.proto.WeizhuProtos.HttpApiRequest;
import com.weizhu.proto.WeizhuProtos.HttpApiResponse;
import com.weizhu.proto.WeizhuProtos.Network;
import com.weizhu.proto.WeizhuProtos.RequestHead;
import com.weizhu.proto.WeizhuProtos.Session;

@Singleton
@SuppressWarnings("serial")
public class HttpApiServlet extends HttpServlet {

	private static final Logger logger = LoggerFactory.getLogger(HttpApiServlet.class);
	
	private final SessionService sessionService;
	private final ImmutableMap<String, ServiceInvoker> serviceInvokerMap;

	@Inject
	public HttpApiServlet(SessionService sessionService, 
			@Named("http_api_server_service_invoker") Set<ServiceInvoker> serviceInvokerSet) {
		this.sessionService = sessionService;
		
		Map<String, ServiceInvoker> serviceInvokerMap = Maps.newTreeMap();
		for (ServiceInvoker serviceInvoker : serviceInvokerSet) {
			serviceInvokerMap.put(serviceInvoker.serviceName(), serviceInvoker);
		}
		
		this.serviceInvokerMap = ImmutableMap.copyOf(serviceInvokerMap);
	}
	
	private static final String REQUEST_ATTR = "com.weizhu.server.api.HttpApiServlet.REQUEST_ATTR";
	private static final String BEGIN_ATTR = "com.weizhu.server.api.HttpApiServlet.BEGIN_ATTR";
	private static final String NETWORK_ATTR = "com.weizhu.server.api.HttpApiServlet.NETWORK_ATTR";
	private static final String VERIFY_FUTURE_ATTR = "com.weizhu.server.api.HttpApiServlet.VERIFY_FUTURE_ATTR";
	private static final String RESPONSE_FUTURE_ATTR = "com.weizhu.server.api.HttpApiServlet.RESPONSE_FUTURE_ATTR";
	
	@SuppressWarnings("unchecked")
	@Override
	protected void doPost(HttpServletRequest httpRequest, HttpServletResponse httpResponse) throws IOException {
		
		HttpApiRequest httpApiRequest = (HttpApiRequest) httpRequest.getAttribute(REQUEST_ATTR);
		Long begin = (Long) httpRequest.getAttribute(BEGIN_ATTR);
		Network network = (Network) httpRequest.getAttribute(NETWORK_ATTR);
		ListenableFuture<VerifySessionKeyResponse> verifyFuture = (ListenableFuture<VerifySessionKeyResponse>) httpRequest.getAttribute(VERIFY_FUTURE_ATTR);
		ListenableFuture<ByteString> responseFuture = (ListenableFuture<ByteString>) httpRequest.getAttribute(RESPONSE_FUTURE_ATTR);
		
		if (responseFuture == null) {
			httpApiRequest = HttpApiRequest.parseFrom(httpRequest.getInputStream());
			
			// 军用微信自动登录
			httpApiRequest = autoLoginModifyRequest(httpRequest, httpApiRequest);
			
			begin = System.currentTimeMillis();
			network = Network.newBuilder()
					.setType(httpApiRequest.getNetworkType())
					.setProtocol(Network.Protocol.HTTP_PB)
					.setRemoteHost(httpRequest.getRemoteAddr())
					.setRemotePort(httpRequest.getRemotePort())
					.build();
			
			// 1, 验证session
			verifyFuture = verifySessionKey(httpApiRequest, network);
			
			// 2. 调用服务
			responseFuture = invokeService(httpApiRequest, network, verifyFuture);
			
			if (!responseFuture.isDone()) {
				httpRequest.setAttribute(REQUEST_ATTR, httpApiRequest);
				httpRequest.setAttribute(BEGIN_ATTR, begin);
				httpRequest.setAttribute(NETWORK_ATTR, network);
				httpRequest.setAttribute(VERIFY_FUTURE_ATTR, verifyFuture);
				httpRequest.setAttribute(RESPONSE_FUTURE_ATTR, responseFuture);
				
				final AsyncContext asyncContext = httpRequest.startAsync();
				responseFuture.addListener(new Runnable() {

					@Override
					public void run() {
						asyncContext.dispatch();
					}
				
				}, MoreExecutors.directExecutor());
				return;
			}
		}
		
		Session session = null;
		HttpApiResponse httpApiResponse = null;
		Throwable throwable = null;
		try {
			VerifySessionKeyResponse verifyResponse = verifyFuture.get();
			session = verifyResponse.hasSession() ? verifyResponse.getSession() : null;
			
			try {
				ByteString responseBody = responseFuture.get();
				httpApiResponse = HttpApiResponse.newBuilder()
						.setResult(HttpApiResponse.Result.SUCC)
						.setInvoke(httpApiRequest.getInvoke())
						.setResponseBody(responseBody)
						.build();
			} catch (ExecutionException e) {
				if (verifyResponse.getResult() != VerifySessionKeyResponse.Result.SUCC && e.getCause() instanceof HeadUnknownException) {
					switch (verifyResponse.getResult()) {
						case FAIL_SESSION_DECRYPTION:
							httpApiResponse = HttpApiResponse.newBuilder()
								.setInvoke(httpApiRequest.getInvoke())
								.setResult(HttpApiResponse.Result.FAIL_SESSION_DECRYPTION)
								.setFailText(verifyResponse.getFailText())
								.build();
							break;
						case FAIL_SESSION_EXPIRED:
							httpApiResponse = HttpApiResponse.newBuilder()
								.setInvoke(httpApiRequest.getInvoke())
								.setResult(HttpApiResponse.Result.FAIL_SESSION_EXPIRED)
								.setFailText(verifyResponse.getFailText())
								.build();
							break;
						case FAIL_USER_NOT_EXSIT:
							httpApiResponse = HttpApiResponse.newBuilder()
								.setInvoke(httpApiRequest.getInvoke())
								.setResult(HttpApiResponse.Result.FAIL_USER_DISABLE)
								.setFailText(verifyResponse.getFailText())
								.build();
							break;
						case FAIL_USER_DISABLE:
							httpApiResponse = HttpApiResponse.newBuilder()
								.setInvoke(httpApiRequest.getInvoke())
								.setResult(HttpApiResponse.Result.FAIL_USER_DISABLE)
								.setFailText(verifyResponse.getFailText())
								.build();
							break;
						default:
							throw e;
					}
				} else {
					throw e;
				}
			}
		} catch (Throwable th) {
			throwable = th;
			httpApiResponse = HttpApiResponse.newBuilder()
					.setInvoke(httpApiRequest.getInvoke())
					.setResult(HttpApiResponse.Result.FAIL_SERVER_EXCEPTION)
					.setFailText("服务器内部错误")
					.build();
		} finally {
			if (throwable != null) {
				logger.error("unkonw error", throwable);
			}
			
			httpApiResponse.writeTo(httpResponse.getOutputStream());
			
			if (!"PushPollingService".equals(httpApiRequest.getInvoke().getServiceName())) {
				long time = System.currentTimeMillis() - begin;
				try {
					LogUtil.logApiAccess(
							session == null ? buildAnonymousHead(httpApiRequest, network) : buildRequestHead(httpApiRequest, network, session), 
							httpApiRequest.getRequestBody().size(), httpApiResponse.getResult().name(), 
							httpApiResponse.hasFailText() ? httpApiResponse.getFailText() : null, 
							httpApiResponse.getResponseBody().size(), 
							time, throwable);
				} catch (Throwable th) {
					logger.warn("log access print fail", th);
				}
			}
		}
	}
	
	private static final ListenableFuture<VerifySessionKeyResponse> EMPTY_SESSION_KEY_RESPONSE = 
			Futures.immediateFuture(VerifySessionKeyResponse.newBuilder()
					.setResult(VerifySessionKeyResponse.Result.FAIL_SESSION_DECRYPTION)
					.setFailText("身份key为空")
					.build());
	
	private ListenableFuture<VerifySessionKeyResponse> verifySessionKey(HttpApiRequest httpApiRequest, Network network) {
		if (httpApiRequest.getSessionKey().isEmpty()) {
			return EMPTY_SESSION_KEY_RESPONSE;
		}
		AnonymousHead head = buildAnonymousHead(httpApiRequest, network);
		VerifySessionKeyRequest request = VerifySessionKeyRequest.newBuilder()
				.setSessionKey(httpApiRequest.getSessionKey())
				.build();
		
		ListenableFuture<VerifySessionKeyResponse> future;
		try {
			future = sessionService.verifySessionKey(head, request);
		} catch (Throwable th) {
			future = Futures.immediateFailedFuture(th);
		}
		return future;
	}
	
	private ListenableFuture<ByteString> invokeService(final HttpApiRequest httpApiRequest, final Network network, ListenableFuture<VerifySessionKeyResponse> verifyFuture) {
		return Futures.transformAsync(verifyFuture, new AsyncFunction<VerifySessionKeyResponse, ByteString>() {

			@Override
			public ListenableFuture<ByteString> apply(VerifySessionKeyResponse verifyResponse) throws Exception {
				Message head;
				if (verifyResponse.getResult() == VerifySessionKeyResponse.Result.SUCC) {
					head = buildRequestHead(httpApiRequest, network, verifyResponse.getSession());
				} else {
					head = buildAnonymousHead(httpApiRequest, network);
				}
				
				ServiceInvoker serviceInvoker = serviceInvokerMap.get(httpApiRequest.getInvoke().getServiceName());
				if (serviceInvoker == null) {
					return Futures.immediateFailedFuture(new InvokeUnknownException("service: " + httpApiRequest.getInvoke().getServiceName()));
				} else {
					return serviceInvoker.invoke(httpApiRequest.getInvoke().getFunctionName(), head, httpApiRequest.getRequestBody());
				}
			}
			
		});
	}
	
	private static RequestHead buildRequestHead(HttpApiRequest httpApiRequest, Network network, Session session) {
		RequestHead.Builder headBuilder = RequestHead.newBuilder()
				.setSession(session)
				.setInvoke(httpApiRequest.getInvoke())
				.setNetwork(network)
				.setWeizhu(httpApiRequest.getWeizhu());
		if (httpApiRequest.hasAndroid()) {
			headBuilder.setAndroid(httpApiRequest.getAndroid());
		}
		if (httpApiRequest.hasIphone()) {
			headBuilder.setIphone(httpApiRequest.getIphone());
		}
		return headBuilder.build();
	}
	
	private static AnonymousHead buildAnonymousHead(HttpApiRequest httpApiRequest, Network network) {
		AnonymousHead.Builder headBuilder = AnonymousHead.newBuilder()
				.setInvoke(httpApiRequest.getInvoke())
				.setNetwork(network)
				.setWeizhu(httpApiRequest.getWeizhu());
		if (httpApiRequest.hasAndroid()) {
			headBuilder.setAndroid(httpApiRequest.getAndroid());
		}
		if (httpApiRequest.hasIphone()) {
			headBuilder.setIphone(httpApiRequest.getIphone());
		}
		return headBuilder.build();
	}
	
	private static final Splitter AUTO_LOGIN_FIELD_SPLITER = Splitter.on(',').omitEmptyStrings().trimResults();
	private static final Splitter AUTO_LOGIN_KV_SPLITER = Splitter.on('=').limit(2).trimResults();

	private static HttpApiRequest autoLoginModifyRequest(HttpServletRequest httpRequest, HttpApiRequest apiRequest) {
		if (!"LoginService".equals(apiRequest.getInvoke().getServiceName())
				|| !"loginAuto".equals(apiRequest.getInvoke().getServiceName())
				) {
			return apiRequest;
		}
		
		String userProperty = null;
		for (Cookie cookie : httpRequest.getCookies()) {
			if ("UserProperty".equals(cookie.getName())) {
				try {
					userProperty = new String(Base64.getDecoder().decode(cookie.getValue()), Charsets.UTF_8);
					break;
				} catch (Throwable th) {
					// ignore
				}
			}
		}
		
		if (userProperty == null) {
			for (Cookie cookie : httpRequest.getCookies()) {
				logger.debug("auto login debug cookie : " + cookie.getName() + ", " + cookie.getValue());
			}
			return apiRequest;
		}
		
		logger.info("auto login UserProperty : " + userProperty);
		
		String mobileNo = null;
		Long companyId = null;
		Long userId = null;
		
		for (String field : AUTO_LOGIN_FIELD_SPLITER.splitToList(userProperty)) {
			List<String> strList = AUTO_LOGIN_KV_SPLITER.splitToList(field);
			if (strList.size() > 1) {
				String key = strList.get(0);
				String value = strList.get(1);
				
				if ("mobile_no".equals(key)) {
					mobileNo = value;
				} else if ("company_id".equals(key)) {
					try {
						companyId = Long.parseLong(value);
					} catch (NumberFormatException e) {
					}
				} else if ("user_id".equals(key)) {
					try {
						userId = Long.parseLong(value);
					} catch (NumberFormatException e) {
					}
				}
			}
		}
		
		LoginAutoRequest.Builder requestBuilder;
		try {
			requestBuilder = LoginAutoRequest.parseFrom(apiRequest.getRequestBody()).toBuilder();
		} catch (InvalidProtocolBufferException e) {
			logger.error("invalid auto login request", e);
			return apiRequest;
		}
		
		if (mobileNo != null) {
			requestBuilder.setMobileNo(mobileNo);
		} 
		if (companyId != null) {
			requestBuilder.setCompanyId(companyId);
		}
		if (userId != null) {
			requestBuilder.setUserId(userId);
		}
		
		return apiRequest.toBuilder().setRequestBody(requestBuilder.build().toByteString()).build();
	}
	
}
