package com.weizhu.webapp.admin.api;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.gson.JsonObject;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.weizhu.common.utils.JsonUtil;

@Singleton
@SuppressWarnings("serial")
public class GetPermissionTreeServlet extends HttpServlet {
	
	@Inject
	public GetPermissionTreeServlet() {
	}
	
	@Override
	protected void doGet(HttpServletRequest httpRequest, HttpServletResponse httpResponse) throws ServletException, IOException {
		this.doPost(httpRequest, httpResponse);
	}

	@Override
	protected void doPost(HttpServletRequest httpRequest, HttpServletResponse httpResponse) throws ServletException, IOException {
		JsonObject result = new JsonObject();		
		result.add("permission_tree", AdminUtil.toJsonPermissionGroupList(null));
		
		httpResponse.setContentType("application/json;charset=UTF-8");
		JsonUtil.GSON.toJson(result, httpResponse.getWriter());
	}
	
}
