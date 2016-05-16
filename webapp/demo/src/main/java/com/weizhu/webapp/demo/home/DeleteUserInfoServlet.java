package com.weizhu.webapp.demo.home;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.gson.JsonObject;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.weizhu.common.db.DBUtil;
import com.weizhu.common.utils.JsonUtil;
import com.weizhu.web.ParamUtil;
import com.zaxxer.hikari.HikariDataSource;

@Singleton
public class DeleteUserInfoServlet extends HttpServlet {

	private static final long serialVersionUID = 1L;

	private final HikariDataSource hikariDataSource;
	
	@Inject
	public DeleteUserInfoServlet(HikariDataSource hikariDataSource) {
		this.hikariDataSource = hikariDataSource;
	}
	
	@Override
	public void doPost(HttpServletRequest httpRequest, HttpServletResponse httpResponse) throws IOException {
		final String userInfoIdStr = ParamUtil.getString(httpRequest, "user_info_id_str", "");
		List<String> userInfoIdDStrList = DBUtil.COMMA_SPLITTER.splitToList(userInfoIdStr);
		List<Integer> userInfoIdList = new ArrayList<Integer>();
		try {
			for (String userInfoId : userInfoIdDStrList) {
				userInfoIdList.add(Integer.parseInt(userInfoId));
			}
		} catch (Exception ex) {
			JsonObject resultObj = new JsonObject();
			resultObj.addProperty("result", "FAIL_USER_ID_INVALID");
			resultObj.addProperty("fail_text", "用户编号不合法");
			
			httpResponse.setContentType("application/json;charset=UTF-8");
			JsonUtil.GSON.toJson(resultObj, httpResponse.getWriter());
			return ;
		}
		
		if (userInfoIdList.isEmpty()) {
			JsonObject resultObj = new JsonObject();
			resultObj.addProperty("result", "SUCC");
			
			httpResponse.setContentType("application/json;charset=UTF-8");
			JsonUtil.GSON.toJson(resultObj, httpResponse.getWriter());
			return ;
		}
		
		StringBuilder sql = new StringBuilder("DELETE FROM weizhu_home_user_info WHERE user_info_id IN (");
		sql.append(DBUtil.COMMA_JOINER.join(userInfoIdList));
		sql.append("); ");
		
		Connection conn = null;
		Statement stmt = null;
		try {
			conn = hikariDataSource.getConnection();
			
			stmt = conn.createStatement();
			stmt.execute(sql.toString());
		} catch (SQLException ex) {
			JsonObject resultObj = new JsonObject();
			resultObj.addProperty("result", "SYSTEM_ERROR");
			resultObj.addProperty("fail_text", "系统异常，请联系管理员");
			
			httpResponse.setContentType("application/json;charset=UTF-8");
			JsonUtil.GSON.toJson(resultObj, httpResponse.getWriter());
			return ;
		} finally {
			DBUtil.closeQuietly(stmt);
			DBUtil.closeQuietly(conn);
		}
		
		JsonObject resultObj = new JsonObject();
		resultObj.addProperty("result", "SUCC");
		
		httpResponse.setContentType("application/json;charset=UTF-8");
		JsonUtil.GSON.toJson(resultObj, httpResponse.getWriter());
		return ;
	}
	
	@Override
	public void doGet(HttpServletRequest httpRequest, HttpServletResponse httpResponse) throws IOException {
		this.doPost(httpRequest, httpResponse);
	}

}
