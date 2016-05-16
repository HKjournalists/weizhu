package com.weizhu.webapp.demo.home;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

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
public class UpdateUserInfoServlet extends HttpServlet {

	private static final long serialVersionUID = 1L;

	private final HikariDataSource hikariDataSource;
	
	@Inject
	public UpdateUserInfoServlet(HikariDataSource hikariDataSource) {
		this.hikariDataSource = hikariDataSource;
	}
	
	@Override
	public void doPost(HttpServletRequest httpRequest, HttpServletResponse httpResponse) throws IOException {
		final int userInfoId = ParamUtil.getInt(httpRequest, "user_info_id", 0);
		
		final String userName = ParamUtil.getString(httpRequest, "user_name", null);
		if (userName == null || userName.isEmpty()) {
			JsonObject resultObj = new JsonObject();
			resultObj.addProperty("result", "FAIL_USER_NAME_INVALID");
			resultObj.addProperty("fail_text", "用户名称为空");
			
			httpResponse.setContentType("application/json;charset=UTF-8");
			JsonUtil.GSON.toJson(resultObj, httpResponse.getWriter());
			return ;
		}
		if (userName.length() > 10) {
			JsonObject resultObj = new JsonObject();
			resultObj.addProperty("result", "FAIL_USER_NAME_INVALID");
			resultObj.addProperty("fail_text", "用户名称过长");
			
			httpResponse.setContentType("application/json;charset=UTF-8");
			JsonUtil.GSON.toJson(resultObj, httpResponse.getWriter());
			return ;
		}
		
		final String position = ParamUtil.getString(httpRequest, "position", null);
		if (position.length() > 20) {
			JsonObject resultObj = new JsonObject();
			resultObj.addProperty("result", "FAIL_POSITION_INVALID");
			resultObj.addProperty("fail_text", "职位名称过长");
			
			httpResponse.setContentType("application/json;charset=UTF-8");
			JsonUtil.GSON.toJson(resultObj, httpResponse.getWriter());
			return ;
		}
		
		final String email = ParamUtil.getString(httpRequest, "email", null);
		if (!email.matches("^([a-z0-9A-Z]+[-|\\.]?)+[a-z0-9A-Z]@([a-z0-9A-Z]+(-[a-z0-9A-Z]+)?\\.)+[a-zA-Z]{2,}$")) {
			JsonObject resultObj = new JsonObject();
			resultObj.addProperty("result", "FAIL_EMAIL_INVALID");
			resultObj.addProperty("fail_text", "请输入正确的邮箱");
			
			httpResponse.setContentType("application/json;charset=UTF-8");
			JsonUtil.GSON.toJson(resultObj, httpResponse.getWriter());
			return ;
		}
		
		final String phone = ParamUtil.getString(httpRequest, "phone", null);
		if (!phone.matches("[0-9]{11}")) {
			JsonObject resultObj = new JsonObject();
			resultObj.addProperty("result", "FAIL_PHONE_INVALID");
			resultObj.addProperty("fail_text", "请输入正确的手机号");
			
			httpResponse.setContentType("application/json;charset=UTF-8");
			JsonUtil.GSON.toJson(resultObj, httpResponse.getWriter());
			return ;
		}
	
		final String company = ParamUtil.getString(httpRequest, "company", null);
		if (company == null || company.isEmpty()) {
			JsonObject resultObj = new JsonObject();
			resultObj.addProperty("result", "FAIL_COMPANY_INVALID");
			resultObj.addProperty("fail_text", "请输入正确的公司名称");
			
			httpResponse.setContentType("application/json;charset=UTF-8");
			JsonUtil.GSON.toJson(resultObj, httpResponse.getWriter());
			return ;
		}
		
		final String province = ParamUtil.getString(httpRequest, "province", null);
		if (province.isEmpty() || province.length() > 20) {
			JsonObject resultObj = new JsonObject();
			resultObj.addProperty("result", "FAIL_PROVINCE_INVALID");
			resultObj.addProperty("fail_text", "请输入正确的省份");
			
			httpResponse.setContentType("application/json;charset=UTF-8");
			JsonUtil.GSON.toJson(resultObj, httpResponse.getWriter());
			return ;
		}
		
		final String city = ParamUtil.getString(httpRequest, "city", null);
		if (city.isEmpty() || city.length() > 20) {
			JsonObject resultObj = new JsonObject();
			resultObj.addProperty("result", "FAIL_CITY_INVALID");
			resultObj.addProperty("fail_text", "请输入正确的城市");
			
			httpResponse.setContentType("application/json;charset=UTF-8");
			JsonUtil.GSON.toJson(resultObj, httpResponse.getWriter());
			return ;
		}
		
		final String remark = ParamUtil.getString(httpRequest, "remark", null);
		if (remark.length() > 191) {
			JsonObject resultObj = new JsonObject();
			resultObj.addProperty("result", "FAIL_REMARK_INVALID");
			resultObj.addProperty("fail_text", "备注过长");
			
			httpResponse.setContentType("application/json;charset=UTF-8");
			JsonUtil.GSON.toJson(resultObj, httpResponse.getWriter());
			return ;
		}
		
		Connection conn = null;
		PreparedStatement pstmt = null;
		try {
			conn = hikariDataSource.getConnection();
			
			pstmt = conn.prepareStatement("UPDATE weizhu_home_user_info SET user_name=?, position=?, email=?, phone=?, company=?, province=?, city=?, remark=? WHERE user_info_id=?; ");
			pstmt.setInt(1, userInfoId);
			pstmt.setString(2, userName);
			pstmt.setString(3, position);
			pstmt.setString(4, email);
			pstmt.setString(5, phone);
			pstmt.setString(6, company);
			pstmt.setString(7, province);
			pstmt.setString(8, city);
			pstmt.setString(9, remark);
			
			pstmt.executeUpdate();
		} catch (SQLException ex) {
			JsonObject resultObj = new JsonObject();
			resultObj.addProperty("result", "SYSTEM_ERROR");
			resultObj.addProperty("fail_text", "系统异常，请联系管理员");
			
			httpResponse.setContentType("application/json;charset=UTF-8");
			JsonUtil.GSON.toJson(resultObj, httpResponse.getWriter());
			return ;
		} finally {
			DBUtil.closeQuietly(pstmt);
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
