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
import com.weizhu.proto.BossService;
import com.weizhu.proto.BossProtos;
import com.weizhu.proto.BossProtos.BossAnonymousHead;
import com.weizhu.proto.BossProtos.BossHead;
import com.weizhu.proto.BossProtos.VerifySessionRequest;
import com.weizhu.proto.BossProtos.VerifySessionResponse;
import com.weizhu.web.LogUtil;
import com.weizhu.web.ParamUtil;

@Singleton
public class BossSessionFilter implements Filter {

	private static final Logger logger = LoggerFactory.getLogger(BossSessionFilter.class);
	
	private final BossService bossService;
	
	@Inject
	public BossSessionFilter(BossService bossService) {
		this.bossService = bossService;
	}
	
	@Override
	public void init(FilterConfig filterConfig) throws ServletException {
	}
	
	private static final String COOKIE_BOSS_SESSION_KEY = "x-boss-session-key";
	private static final String REQUEST_ATTR_VERIFY_FLAG = "com.weizhu.web.filter.BossSessionFilter.REQUEST_ATTR_VERIFY_FLAG";
	public static final String REQUEST_ATTR_BOSS_ANONYMOUS_HEAD = Key.get(BossAnonymousHead.class).toString();
	public static final String REQUEST_ATTR_BOSS_HEAD = Key.get(BossHead.class).toString();
	public static final String REQUEST_ATTR_BOSS_VERIFY_SESSION_RESPONSE = Key.get(VerifySessionResponse.class).toString();;
	
	@Override
	public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain chain) throws IOException, ServletException {
		final HttpServletRequest httpRequest = (HttpServletRequest) servletRequest;
		final HttpServletResponse httpResponse = (HttpServletResponse) servletResponse;
		
		final String requestPath = httpRequest.getRequestURI().substring(httpRequest.getContextPath().length());
		
		// 无需任何处理的请求
		if (requestPath.startsWith("/static/")) {
			chain.doFilter(httpRequest, httpResponse);
			return;
		}
		
		if (httpRequest.getAttribute(REQUEST_ATTR_VERIFY_FLAG) != null) {
			chain.doFilter(httpRequest, httpResponse);
			return;
		}
		
		httpRequest.setAttribute(REQUEST_ATTR_VERIFY_FLAG, Boolean.TRUE);
		
		BossAnonymousHead bossAnonymousHead = null;
		BossHead bossHead = null;
		
		final long begin = System.currentTimeMillis();
		Throwable throwable = null;
		try {
			bossAnonymousHead = buildAnonymousHead(httpRequest);
			httpRequest.setAttribute(REQUEST_ATTR_BOSS_ANONYMOUS_HEAD, bossAnonymousHead);
			
			final String sessionKey = getBossSessionKey(httpRequest);
			if (sessionKey != null && !sessionKey.isEmpty()) {
				VerifySessionResponse response = Futures.getUnchecked(
						this.bossService.verifySession(bossAnonymousHead, 
								VerifySessionRequest.newBuilder()
								.setSessionKey(sessionKey)
								.build()));
				
				httpRequest.setAttribute(REQUEST_ATTR_BOSS_VERIFY_SESSION_RESPONSE, response);
				
				if (response.getResult() == VerifySessionResponse.Result.SUCC) {
					bossHead = buildHead(httpRequest, bossAnonymousHead, response.getSession());
					httpRequest.setAttribute(REQUEST_ATTR_BOSS_HEAD, bossHead);
				}
			}
			
			// 如果有cookie且身份校验失败，要清除cookie
			if (sessionKey != null && bossHead == null) {
				Cookie cookie = new Cookie(COOKIE_BOSS_SESSION_KEY, "");
				cookie.setPath("/");
				cookie.setMaxAge(0);
				httpResponse.addCookie(cookie);
			}
			
			chain.doFilter(httpRequest, httpResponse);
			
			if (requestPath.endsWith(".json")) {
				// IE 浏览器要对json数据返回的content-type做特殊处理
				String ua = bossAnonymousHead.getUserAgent();
				if (ua.contains("MSIE") || (ua.contains("Trident/") && ua.contains("rv:"))) {
					httpResponse.setContentType("text/html;charset=UTF-8");
				}
			}
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
				LogUtil.logWebappAccess(httpRequest, httpResponse, bossHead != null ? bossHead : bossAnonymousHead, time, throwable);
			} catch (Throwable th) {
				logger.warn("log access print fail", th);
			}
		}
	}
	
	private static BossAnonymousHead buildAnonymousHead(HttpServletRequest httpRequest) {
		return BossAnonymousHead.newBuilder()
				.setRequestUri(httpRequest.getRequestURI())
				.setUserAgent(getUserAgent(httpRequest))
				.setRemoteHost(getRemoteHost(httpRequest))
				.build();
	}
	
	private static BossHead buildHead(HttpServletRequest httpRequest, BossAnonymousHead anonymousHead, BossProtos.BossSession session) {
		return BossHead.newBuilder()
				.setSession(session)
				.setRequestUri(anonymousHead.getRequestUri())
				.setUserAgent(anonymousHead.getUserAgent())
				.setRemoteHost(anonymousHead.getRemoteHost())
				.build();
	}
	
	private static String getUserAgent(HttpServletRequest httpRequest) {
		String value = httpRequest.getHeader(HttpHeaders.USER_AGENT);
		return value == null ? "" : value.trim();
	}
	
	private static String getRemoteHost(HttpServletRequest httpRequest) {
		String realIp = httpRequest.getHeader("X-Real-IP");
		if (realIp == null || realIp.trim().isEmpty()) {
			return httpRequest.getRemoteAddr();
		} else {
			return realIp.trim();
		}
	}
	
	private String getBossSessionKey(HttpServletRequest httpRequest) {
		String sessionKey = ParamUtil.getString(httpRequest, "boss_session_key", "").trim();
		if (sessionKey != null && !sessionKey.trim().isEmpty()) {
			return sessionKey.trim();
		}
		
		Cookie[] cookies = httpRequest.getCookies();
		if (cookies == null) {
			return null;
		}
		
		for (Cookie c : cookies) {
			if (COOKIE_BOSS_SESSION_KEY.equals(c.getName())) {
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
