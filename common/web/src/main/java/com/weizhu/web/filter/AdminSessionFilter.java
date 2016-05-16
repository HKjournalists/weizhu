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
import com.weizhu.proto.AdminProtos;
import com.weizhu.proto.AdminService;
import com.weizhu.proto.AdminProtos.AdminAnonymousHead;
import com.weizhu.proto.AdminProtos.AdminHead;
import com.weizhu.proto.AdminProtos.AdminVerifySessionRequest;
import com.weizhu.proto.AdminProtos.AdminVerifySessionResponse;
import com.weizhu.web.LogUtil;
import com.weizhu.web.ParamUtil;

@Singleton
public class AdminSessionFilter implements Filter {

	private static final Logger logger = LoggerFactory.getLogger(AdminSessionFilter.class);
	
	private final AdminService adminService;
	
	@Inject
	public AdminSessionFilter(AdminService adminService) {
		this.adminService = adminService;
	}
	
	@Override
	public void init(FilterConfig filterConfig) throws ServletException {
	}
	
	private static final String COOKIE_ADMIN_SESSION_KEY = "x-admin-session-key";
	private static final String REQUEST_ATTR_VERIFY_FLAG = "com.weizhu.web.filter.AdminSessionFilter.REQUEST_ATTR_VERIFY_FLAG";
	public static final String REQUEST_ATTR_ADMIN_ANONYMOUS_HEAD = Key.get(AdminAnonymousHead.class).toString();
	public static final String REQUEST_ATTR_ADMIN_HEAD = Key.get(AdminHead.class).toString();
	public static final String REQUEST_ATTR_ADMIN_INFO = Key.get(AdminInfo.class).toString();
	public static final String REQUEST_ATTR_ADMIN_VERIFY_SESSION_RESPONSE = Key.get(AdminVerifySessionResponse.class).toString();

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
		
		AdminAnonymousHead adminAnonymousHead = null;
		AdminHead adminHead = null;
		AdminInfo adminInfo = null;
		
		final long begin = System.currentTimeMillis();
		Throwable throwable = null;
		try {
			adminAnonymousHead = buildAnonymousHead(httpRequest);
			httpRequest.setAttribute(REQUEST_ATTR_ADMIN_ANONYMOUS_HEAD, adminAnonymousHead);
			
			final String sessionKey = getAdminSessionKey(httpRequest);
			if (sessionKey != null && !sessionKey.isEmpty()) {
				AdminVerifySessionResponse response = Futures.getUnchecked(
						this.adminService.adminVerifySession(adminAnonymousHead, 
								AdminVerifySessionRequest.newBuilder()
								.setSessionKey(sessionKey)
								.build()));
				
				httpRequest.setAttribute(REQUEST_ATTR_ADMIN_VERIFY_SESSION_RESPONSE, response);
				
				if (response.getResult() == AdminVerifySessionResponse.Result.SUCC) {
					adminInfo = new AdminInfo(response.getAdmin(), response.getRefRoleList(), response.getRefCompanyList());
					adminHead = buildHead(httpRequest, adminAnonymousHead, response.getSession(), adminInfo);
					
					httpRequest.setAttribute(REQUEST_ATTR_ADMIN_HEAD, adminHead);
					httpRequest.setAttribute(REQUEST_ATTR_ADMIN_INFO, adminInfo);
				}
			}
			
			// 如果有cookie且身份校验失败，要清除cookie
			if (sessionKey != null && adminHead == null) {
				Cookie cookie = new Cookie(COOKIE_ADMIN_SESSION_KEY, "");
				cookie.setPath("/");
				cookie.setMaxAge(0);
				httpResponse.addCookie(cookie);
			}
			
			chain.doFilter(httpRequest, httpResponse);
			
			if (requestPath.endsWith(".json")) {
				// IE 浏览器要对json数据返回的content-type做特殊处理
				String ua = adminAnonymousHead.getUserAgent();
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
				LogUtil.logWebappAccess(httpRequest, httpResponse, adminHead != null ? adminHead : adminAnonymousHead, time, throwable);
			} catch (Throwable th) {
				logger.warn("log access print fail", th);
			}
		}
	}
	
	private static AdminAnonymousHead buildAnonymousHead(HttpServletRequest httpRequest) {
		return AdminAnonymousHead.newBuilder()
				.setRequestUri(httpRequest.getRequestURI())
				.setUserAgent(getUserAgent(httpRequest))
				.setRemoteHost(getRemoteHost(httpRequest))
				.build();
	}
	
	private static AdminHead buildHead(HttpServletRequest httpRequest, AdminAnonymousHead anonymousHead, AdminProtos.AdminSession session, AdminInfo adminInfo) {
		AdminHead.Builder builder = AdminHead.newBuilder()
				.setSession(session)
				.setRequestUri(anonymousHead.getRequestUri())
				.setUserAgent(anonymousHead.getUserAgent())
				.setRemoteHost(anonymousHead.getRemoteHost());
		
		Long companyId = ParamUtil.getLong(httpRequest, "company_id", null);
		if (companyId != null && adminInfo.refCompanyMap().containsKey(companyId)) {
			builder.setCompanyId(companyId);
		}
		return builder.build();
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
	
	private static String getAdminSessionKey(HttpServletRequest httpRequest) {
		String sessionKey = ParamUtil.getString(httpRequest, "admin_session_key", null);
		if (sessionKey != null && !sessionKey.trim().isEmpty()) {
			return sessionKey.trim();
		}
		
		Cookie[] cookies = httpRequest.getCookies();
		if (cookies == null) {
			return null;
		}
		
		for (Cookie c : cookies) {
			if (COOKIE_ADMIN_SESSION_KEY.equals(c.getName())) {
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
