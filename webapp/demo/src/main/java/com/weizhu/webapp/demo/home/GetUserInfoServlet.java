package com.weizhu.webapp.demo.home;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.weizhu.common.db.DBUtil;
import com.weizhu.common.utils.JsonUtil;
import com.weizhu.web.ParamUtil;
import com.zaxxer.hikari.HikariDataSource;

@Singleton
public class GetUserInfoServlet extends HttpServlet {

	private static final long serialVersionUID = 1L;

	private final HikariDataSource hikariDataSource;
	
	@Inject
	public GetUserInfoServlet(HikariDataSource hikariDataSource) {
		this.hikariDataSource = hikariDataSource;
	}
	
	@Override
	public void doPost(HttpServletRequest httpRequest, HttpServletResponse httpResponse) throws IOException {
		final int page = ParamUtil.getInt(httpRequest, "page", 0);
		final int rows = ParamUtil.getInt(httpRequest, "rows", 0);
		
		Connection conn = null;
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try {
			conn = hikariDataSource.getConnection();
			
			pstmt = conn.prepareStatement("SELECT COUNT(*) as total FROM weizhu_home_user_info; SELECT * FROM weizhu_home_user_info ORDER BY user_info_id ASC LIMIT ?, ?; ");
			pstmt.setInt(1, (page - 1) * rows);
			pstmt.setInt(2, rows);
			pstmt.execute();
			rs = pstmt.getResultSet();
			
			if (!rs.next()) {
				throw new RuntimeException("cannot get total");
			}
			
			int total = rs.getInt("total");
			
			DBUtil.closeQuietly(rs);
			rs = null;
			
			pstmt.getMoreResults();
			rs = pstmt.getResultSet();
			
			JsonArray array = new JsonArray();
			while (rs.next()) {
				JsonObject object = new JsonObject();
				
				object.addProperty("user_info_id", rs.getInt("user_info_id"));
				object.addProperty("user_name", rs.getString("user_name"));
				object.addProperty("position", rs.getString("position"));
				object.addProperty("email", rs.getString("email"));
				object.addProperty("phone", rs.getString("phone"));
				object.addProperty("company", rs.getString("company"));
				object.addProperty("province", rs.getString("province"));
				object.addProperty("city", rs.getString("city"));
				object.addProperty("remark", rs.getString("remark"));
				
				array.add(object);
			}
			JsonObject result = new JsonObject();
			result.add("rows", array);
			result.addProperty("total", total);
			
			httpResponse.setContentType("application/json;charset=UTF-8");
			JsonUtil.GSON.toJson(result, httpResponse.getWriter());
			return ;
		} catch (SQLException ex) {
			JsonObject resultObj = new JsonObject();
			resultObj.addProperty("result", "SYSTEM_ERROR");
			resultObj.addProperty("fail_text", "系统异常，请联系管理员");
			
			httpResponse.setContentType("application/json;charset=UTF-8");
			JsonUtil.GSON.toJson(resultObj, httpResponse.getWriter());
			return ;
		} finally {
			DBUtil.closeQuietly(rs);
			DBUtil.closeQuietly(pstmt);
			DBUtil.closeQuietly(conn);
		}
	}
	
	@Override
	public void doGet(HttpServletRequest httpRequest, HttpServletResponse httpResponse) throws IOException {
		this.doPost(httpRequest, httpResponse);
	}

}
