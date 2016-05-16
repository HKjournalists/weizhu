package com.weizhu.webapp.admin;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.common.collect.ImmutableSet;
import com.google.inject.Singleton;
import com.weizhu.proto.AdminProtos;
import com.weizhu.proto.AdminProtos.AdminHead;
import com.weizhu.web.filter.AdminInfo;
import com.weizhu.web.filter.AdminSessionFilter;

@Singleton
public class WebappAdminFilter implements Filter {

	@Override
	public void init(FilterConfig filterConfig) throws ServletException {
	}

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
		
		final AdminHead adminHead = (AdminHead) httpRequest.getAttribute(AdminSessionFilter.REQUEST_ATTR_ADMIN_HEAD);
		final AdminInfo adminInfo = (AdminInfo) httpRequest.getAttribute(AdminSessionFilter.REQUEST_ATTR_ADMIN_INFO);
		
		// 处理顺序: 登陆状态 -> 权限
		
		// 不需要校验登陆状态即可访问
		if (requestPath.equals("/login.html")
				|| requestPath.equals("/getpassindex.html")
				|| requestPath.equals("/version_update.html")
				|| requestPath.equals("/403.html")
				|| requestPath.equals("/api/get_permission_tree.json")
				|| requestPath.equals("/api/admin_login.json")
				|| requestPath.equals("/api/admin_reset_password.json")
				|| requestPath.equals("/api/admin_forgot_password.json")
				|| requestPath.equals("/api/admin_forgot_password_reset.json")
				) {
			chain.doFilter(httpRequest, httpResponse);
		} 
		// 无登陆状态访问
		else if (adminHead == null) {
			if (requestPath.startsWith("/api/")) {
				// 如果是接口访问，设置状态码为401并返回空
				httpResponse.sendError(HttpServletResponse.SC_UNAUTHORIZED);
			} else {
				// 默认全部redirect到登录页
				httpResponse.sendRedirect(httpRequest.getContextPath() + "/login.html?redirect_url=" + httpRequest.getRequestURI());
			}
		} 
		// 有登陆状态访问
		else {
			// 不需要考虑权限的请求
			if (requestPath.equals("/index.html")
					|| requestPath.equals("/main.html")
					|| requestPath.equals("/api/admin_logout.json")
					|| requestPath.equals("/api/get_company_list.json")
					|| requestPath.equals("/api/get_admin_info.json")
					|| requestPath.equals("/api/qr_code.jpg")
					) {
				chain.doFilter(httpRequest, httpResponse);
			}
			// 有权限的请求
			else if (this.checkPermission(requestPath, adminHead, adminInfo)) {
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
	}
	
	private boolean checkPermission(String requestPath, AdminHead head, AdminInfo adminInfo) {
		if (!head.hasCompanyId()) {
			return false;
		}
		
		if (!PermissionConst.pathSet().contains(requestPath)) {
			return true;
		}
		
		AdminProtos.Admin.Company company = null;
		for (AdminProtos.Admin.Company tmp : adminInfo.admin().getCompanyList()) {
			if (tmp.getCompanyId() == head.getCompanyId()) {
				company = tmp;
				break;
			}
		}
		
		if (company == null) {
			// never
			return false;
		}
		
		for (Integer roleId : company.getRoleIdList()) {
			AdminProtos.Role role = adminInfo.refRoleMap().get(roleId);
			if (role != null && (!role.hasCompanyId() || role.getCompanyId() == head.getCompanyId())) {
				for (String permissionId : role.getPermissionIdList()) {
					ImmutableSet<String> pathSet = PermissionConst.permissionIdToPathMap().get(permissionId);
					if (pathSet != null && pathSet.contains(requestPath)) {
						return true;
					}
				}
			}
		}
		return false;
	}

	@Override
	public void destroy() {
	}

}
