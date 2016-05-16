package com.weizhu.web.filter;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.net.HttpHeaders;
import com.google.common.util.concurrent.Futures;
import com.google.inject.Inject;
import com.google.inject.Key;
import com.google.inject.Singleton;
import com.weizhu.common.service.ServiceUtil;
import com.weizhu.proto.SessionService;
import com.weizhu.proto.WeizhuProtos;
import com.weizhu.proto.SessionProtos.VerifyWebLoginSessionKeyRequest;
import com.weizhu.proto.SessionProtos.VerifyWebLoginSessionKeyResponse;
import com.weizhu.proto.WeizhuProtos.AnonymousHead;
import com.weizhu.proto.WeizhuProtos.RequestHead;
import com.weizhu.web.LogUtil;
import com.weizhu.web.ParamUtil;

@Singleton
public class UserWebLoginSessionFilter implements Filter {

	private static final Logger logger = LoggerFactory.getLogger(UserWebLoginSessionFilter.class);
	
	private final SessionService sessionService;
	
	@Inject
	public UserWebLoginSessionFilter(SessionService sessionService) {
		this.sessionService = sessionService;
	}
	
	@Override
	public void init(FilterConfig filterConfig) throws ServletException {
	}
	
	private static final String COOKIE_USER_WEB_LOGIN_SESSION_KEY = "x-web-login-session-key";
	
	private static final String REQUEST_ATTR_VERIFY_FLAG = "com.weizhu.web.filter.UserWebLoginSessionFilter.REQUEST_ATTR_VERIFY_FLAG";
	public static final String REQUEST_ATTR_ANONYMOUS_HEAD = Key.get(AnonymousHead.class).toString();
	public static final String REQUEST_ATTR_REQUEST_HEAD = Key.get(RequestHead.class).toString();
	public static final String REQUEST_ATTR_USER_VERIFY_WEB_LOGIN_SESSION_RESPONSE = Key.get(VerifyWebLoginSessionKeyResponse.class).toString();
	
	@Override
	public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain chain) throws IOException, ServletException {
		final HttpServletRequest httpRequest = (HttpServletRequest) servletRequest;
		final HttpServletResponse httpResponse = (HttpServletResponse) servletResponse;
		
		final String requestPath = httpRequest.getRequestURI().substring(httpRequest.getContextPath().length());
		
		// 无需校验身份的请求
		if (requestPath.startsWith("/static/")) {
			chain.doFilter(httpRequest, httpResponse);
			return;
		}
		
		if (httpRequest.getAttribute(REQUEST_ATTR_VERIFY_FLAG) != null) {
			chain.doFilter(httpRequest, httpResponse);
			return;
		}
		
		httpRequest.setAttribute(REQUEST_ATTR_VERIFY_FLAG, Boolean.TRUE);
		
		AnonymousHead anonymousHead = null;
		RequestHead requestHead = null;
		
		final long begin = System.currentTimeMillis();
		Throwable throwable = null;
		try {
			anonymousHead = buildAnonymousHead(httpRequest);
			httpRequest.setAttribute(REQUEST_ATTR_ANONYMOUS_HEAD, anonymousHead);

			String webLoginSessionKey = getUserWebLoginSessionKey(httpRequest);
			
			if (webLoginSessionKey != null && !webLoginSessionKey.isEmpty()) {
				VerifyWebLoginSessionKeyResponse response = Futures.getUnchecked(
						this.sessionService.verifyWebLoginSessionKey(anonymousHead, 
								VerifyWebLoginSessionKeyRequest.newBuilder()
								.setWebLoginSessionKey(webLoginSessionKey)
								.build()));
				
				httpRequest.setAttribute(REQUEST_ATTR_USER_VERIFY_WEB_LOGIN_SESSION_RESPONSE, response);
				
				if (response.getResult() == VerifyWebLoginSessionKeyResponse.Result.SUCC) {
					anonymousHead = anonymousHead.toBuilder().setWebLogin(response.getWebLogin()).build();
					requestHead = ServiceUtil.toRequestHead(anonymousHead, response.getSession());
					
					httpRequest.setAttribute(REQUEST_ATTR_ANONYMOUS_HEAD, anonymousHead);
					httpRequest.setAttribute(REQUEST_ATTR_REQUEST_HEAD, requestHead);
					
					Cookie cookie = new Cookie(COOKIE_USER_WEB_LOGIN_SESSION_KEY, webLoginSessionKey);
					cookie.setMaxAge(30 * 24 * 60 * 60);
					cookie.setPath("/");
					httpResponse.addCookie(cookie);
				} else {
					// 如果有cookie且身份校验失败，要清除cookie
					Cookie cookie = new Cookie(COOKIE_USER_WEB_LOGIN_SESSION_KEY, "");
					cookie.setPath("/");
					cookie.setMaxAge(0);
					httpResponse.addCookie(cookie);
				}
			}
			
			chain.doFilter(httpRequest, httpResponse);
		}
		// 对异常尽量不做过多包装
		catch (IOException e) {
			throwable = e;
			throw e;
		} catch (ServletException e) {
			throwable = e;
			throw e;
		} catch (RuntimeException e) {
			throwable = e;
			throw e;
		} catch (Error e) {
			throwable = e;
			throw e;
		} catch (Throwable th) {
			throwable = th;
			throw new ServletException(th);
		} finally {
			if (throwable != null) {
				logger.error("unkonw error", throwable);
			}
			long time = System.currentTimeMillis() - begin;
			try {
				LogUtil.logWebappAccess(httpRequest, httpResponse, requestHead != null ? requestHead : anonymousHead, time, throwable);
			} catch (Throwable th) {
				logger.warn("log access print fail", th);
			}
		}
	}
	
	private static AnonymousHead buildAnonymousHead(HttpServletRequest httpRequest) {
		String userAgent = httpRequest.getHeader(HttpHeaders.USER_AGENT);
		if (userAgent == null || userAgent.trim().isEmpty()) {
			userAgent = "_unknown_";
		}
		
		return AnonymousHead.newBuilder()
				.setInvoke(WeizhuProtos.Invoke.newBuilder()
						.setInvokeId(0)
						.setServiceName(httpRequest.getServerName())
						.setFunctionName(httpRequest.getRequestURI())
						.build())
				.setNetwork(buildNetwork(httpRequest))
				.setWebLogin(WeizhuProtos.WebLogin.newBuilder()
						.setWebloginId(0)
						.setLoginTime(0)
						.setActiveTime((int) (System.currentTimeMillis() / 1000L))
						.setUserAgent(userAgent.trim())
						.build())
				.build();
	}
	
	private static WeizhuProtos.Network buildNetwork(HttpServletRequest httpRequest) {
		String remoteHost = httpRequest.getHeader("X-Real-IP");
		if (remoteHost == null || remoteHost.trim().isEmpty()) {
			remoteHost = httpRequest.getRemoteAddr();
		}
		
		Integer remotePort = null;
		String remotePortStr = httpRequest.getHeader("X-Real-Port");
		if (remotePortStr != null && !remotePortStr.trim().isEmpty()) {
			try {
				remotePort = Integer.parseInt(remotePortStr.trim());
			} catch (NumberFormatException e) {
				// ignore
			}
		}
		if (remotePort == null) {
			remotePort = httpRequest.getRemotePort();
		}
		
		return WeizhuProtos.Network.newBuilder()
				.setType(WeizhuProtos.Network.Type.UNKNOWN)
				.setProtocol(WeizhuProtos.Network.Protocol.WEB_MOBILE)
				.setRemoteHost(remoteHost)
				.setRemotePort(remotePort)
				.build();
	}

	private static String getUserWebLoginSessionKey(HttpServletRequest httpRequest) {
		String sessionKey = ParamUtil.getString(httpRequest, "session_key", null);
		if (sessionKey != null && !sessionKey.trim().isEmpty()) {
			return sessionKey.trim();
		}
		
		Cookie[] cookies = httpRequest.getCookies();
		if (cookies == null) {
			return null;
		}
		
		for (Cookie c : cookies) {
			if (COOKIE_USER_WEB_LOGIN_SESSION_KEY.equals(c.getName())) {
				if (c.getValue() != null && !c.getValue().trim().isEmpty()) {
					return c.getValue().trim();
				}
			}
		}
		return null;
	}

	@Override
	public void destroy() {
	}


}
