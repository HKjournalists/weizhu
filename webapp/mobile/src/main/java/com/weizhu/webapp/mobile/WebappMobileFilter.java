package com.weizhu.webapp.mobile;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.inject.Singleton;
import com.weizhu.proto.WeizhuProtos.RequestHead;
import com.weizhu.web.filter.UserSessionFilter;

@Singleton
public class WebappMobileFilter implements Filter {

	@Override
	public void init(FilterConfig filterConfig) throws ServletException {
	}

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
		
		final RequestHead requestHead = (RequestHead) httpRequest.getAttribute(UserSessionFilter.REQUEST_ATTR_REQUEST_HEAD);

		// 无需考虑登录状态的访问请求
		if (requestPath.equals("/verify_session_fail.jsp") 
				|| requestPath.startsWith("/register/")
				|| requestPath.equals("/api/register/send_register_sms_code.json")
				|| requestPath.equals("/api/register/register_by_sms_code.json")
				|| requestPath.equals("/api/test/test_login.json") 
				|| requestPath.equals("/test/test_login.html")
				|| requestPath.equals("/api/credits/duiba_consume_credits.json")
				|| requestPath.equals("/api/credits/duiba_notify_credits.json")
				) {
			chain.doFilter(httpRequest, httpResponse);
		} 
		// 没有登陆身份且不能匿名访问
		else if (requestHead == null) {
			if (requestPath.startsWith("/api/")) {
				// 如果是接口访问，直接返回空
				httpResponse.sendError(HttpServletResponse.SC_UNAUTHORIZED);
			} else {
				// 默认全部foward到提示页
				httpRequest.getRequestDispatcher("/verify_session_fail.jsp").forward(httpRequest, httpResponse);
			}
		} 
		// 有登陆状态访问
		else {
			chain.doFilter(httpRequest, httpResponse);
		}
	}

	@Override
	public void destroy() {
	}

}
