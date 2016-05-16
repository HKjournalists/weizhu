package com.weizhu.webapp.boss;

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
import com.weizhu.proto.BossProtos.BossHead;
import com.weizhu.web.filter.BossSessionFilter;

@Singleton
public class WebappBossFilter implements Filter {

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
		
		BossHead bossHead = (BossHead) httpRequest.getAttribute(BossSessionFilter.REQUEST_ATTR_BOSS_HEAD);
		
		String sslCommonName = getSslClientCommonName(httpRequest);
		if (bossHead != null && sslCommonName != null && !sslCommonName.equals(bossHead.getSession().getBossId())) {
			bossHead = null;
		}
		
		// 无需考虑登录状态的访问请求
		if (requestPath.equals("/login.jsp") 
				|| requestPath.equals("/api/login.json")
				) {
			chain.doFilter(httpRequest, httpResponse);
		}
		// 没有登录身份
		else if (bossHead == null) {
			if (requestPath.startsWith("/api/")) {
				// 如果是接口访问，直接返回空
				httpResponse.sendError(HttpServletResponse.SC_UNAUTHORIZED);
			} else {
				// 默认全部redirect到登录页
				httpResponse.sendRedirect(httpRequest.getContextPath() + "/login.jsp?redirect_url=" + httpRequest.getRequestURI());
			}
		} else {
			chain.doFilter(httpRequest, httpResponse);
		}
	}
	
	private static String getSslClientCommonName(HttpServletRequest httpRequest) {
		// /C=CN/ST=Beijing/L=Beijing/O=weizhu/OU=weizhu/CN=francislin/emailAddress=francislin@wehelpu.cn
		String sslClientSubjectDN = httpRequest.getHeader("X-SSL-Client-Subject-DN");
		if (sslClientSubjectDN == null) {
			return null;
		}
		
		String[] fields = sslClientSubjectDN.split("/");
		for (String field : fields) {
			if (field.startsWith("CN=")) {
				return field.substring("CN=".length());
			}
		}
		return null;
	}

	@Override
	public void destroy() {
	}

}
