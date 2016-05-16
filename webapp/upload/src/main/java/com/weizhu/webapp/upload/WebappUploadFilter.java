package com.weizhu.webapp.upload;

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
import com.weizhu.proto.AdminProtos.AdminHead;
import com.weizhu.proto.BossProtos.BossHead;
import com.weizhu.proto.WeizhuProtos.RequestHead;
import com.weizhu.web.filter.AdminSessionFilter;
import com.weizhu.web.filter.BossSessionFilter;
import com.weizhu.web.filter.UserSessionFilter;

@Singleton
public class WebappUploadFilter implements Filter {

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
		
		if (requestPath.equals("/avatar")
				|| requestPath.equals("/im/image") 
				|| requestPath.equals("/community/image")
				) {
			chain.doFilter(httpRequest, httpResponse);
			return;
		}
		
		if (requestPath.startsWith("/user/") || requestPath.startsWith("/api/user/")) {
			final RequestHead requestHead = (RequestHead) httpRequest.getAttribute(UserSessionFilter.REQUEST_ATTR_REQUEST_HEAD);
			
			if (requestHead == null) {
				if (requestPath.startsWith("/api/")) {
					// 如果是接口访问，直接返回空
					httpResponse.sendError(HttpServletResponse.SC_UNAUTHORIZED);
				} else {
					// 默认全部foward到提示页
					httpRequest.getRequestDispatcher("/verify_user_session_fail.jsp").forward(httpRequest, httpResponse);
				}
			} 
			// 有登陆状态访问
			else {
				chain.doFilter(httpRequest, httpResponse);
			}
		} else if (requestPath.startsWith("/admin/") || requestPath.startsWith("/api/admin/")) {
			final AdminHead adminHead = (AdminHead) httpRequest.getAttribute(AdminSessionFilter.REQUEST_ATTR_ADMIN_HEAD);
			
			if (adminHead == null) {
				if (requestPath.startsWith("/api/")) {
					// 如果是接口访问，设置状态码为401并返回空
					httpResponse.sendError(HttpServletResponse.SC_UNAUTHORIZED);
				} else {
					// 默认全部foward到提示页
					httpRequest.getRequestDispatcher("/verify_admin_session_fail.jsp").forward(httpRequest, httpResponse);
				}
			} else {
				// 不需要考虑权限的请求
				if (requestPath.equals("/admin/test_upload.html")) {
					chain.doFilter(httpRequest, httpResponse);
				}
				// 有权限的请求
				else if (adminHead.hasCompanyId()) {
					chain.doFilter(httpRequest, httpResponse);
				}
				// 无权限的请求
				else {
					if (requestPath.startsWith("/api/")) {
						// 如果是接口访问，设置状态码为403并返回空
						httpResponse.sendError(HttpServletResponse.SC_FORBIDDEN);
					} else {
						// 默认全部redirect到登录页
						httpResponse.sendRedirect(httpRequest.getContextPath() + "/403.html");
					}
				}
			}
		} else if (requestPath.startsWith("/boss/") || requestPath.startsWith("/api/boss/")) {
			final BossHead bossHead = (BossHead) httpRequest.getAttribute(BossSessionFilter.REQUEST_ATTR_BOSS_HEAD);
			
			if (bossHead == null) {
				if (requestPath.startsWith("/api/")) {
					// 如果是接口访问，直接返回空
					httpResponse.sendError(HttpServletResponse.SC_UNAUTHORIZED);
				} else {
					// 默认全部foward到提示页
					httpRequest.getRequestDispatcher("/verify_boss_session_fail.jsp").forward(httpRequest, httpResponse);
				}
			} 
			// 有登陆状态访问
			else {
				chain.doFilter(httpRequest, httpResponse);
			}
		} else {
			chain.doFilter(httpRequest, httpResponse);
		}
	}

	@Override
	public void destroy() {
	}

}
