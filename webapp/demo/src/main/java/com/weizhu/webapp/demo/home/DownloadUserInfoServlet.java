package com.weizhu.webapp.demo.home;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.poi.hssf.usermodel.HSSFCell;
import org.apache.poi.hssf.usermodel.HSSFCellStyle;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.util.CellRangeAddress;

import com.google.gson.JsonObject;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.weizhu.common.db.DBUtil;
import com.weizhu.common.utils.JsonUtil;
import com.weizhu.web.ParamUtil;
import com.zaxxer.hikari.HikariDataSource;

@Singleton
public class DownloadUserInfoServlet extends HttpServlet {

	private static final long serialVersionUID = 1L;

	private final HikariDataSource hikariDataSource;
	
	@Inject
	public DownloadUserInfoServlet(HikariDataSource hikariDataSource) {
		this.hikariDataSource = hikariDataSource;
	}
	
	@Override
	public void doPost(HttpServletRequest httpRequest, HttpServletResponse httpResponse) throws IOException {
		final int start = ParamUtil.getInt(httpRequest, "start", 0);
		final int length = ParamUtil.getInt(httpRequest, "length", 0);
		
		Connection conn = null;
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		
		List<UserInfo> userInfoList = new ArrayList<UserInfo>();
		try {
			conn = hikariDataSource.getConnection();
			
			pstmt = conn.prepareStatement("SELECT * FROM weizhu_home_user_info ORDER BY user_info_id ASC LIMIT ?, ?; ");
			pstmt.setInt(1, start);
			pstmt.setInt(2, length);
			
			rs = pstmt.executeQuery();
			
			while (rs.next()) {
				userInfoList.add(new UserInfo(rs.getInt("user_info_id"), 
						rs.getString("user_name"),
						rs.getString("position"),
						rs.getString("email"),
						rs.getString("phone"),
						rs.getString("company"),
						rs.getString("province"),
						rs.getString("city"),
						rs.getString("remark")));
			}
			
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
		
		// 写表格
		HSSFWorkbook wb = new HSSFWorkbook();
		HSSFSheet sheet = wb.createSheet("注册人员表");
		CellRangeAddress cra = new CellRangeAddress(0, 0, 0, 8);
		sheet.addMergedRegion(cra);

		HSSFRow headRow = sheet.createRow(0);
		HSSFCellStyle headStyle = wb.createCellStyle();
		headStyle.setAlignment(HSSFCellStyle.ALIGN_CENTER);
		headStyle.setAlignment(HSSFCellStyle.BIG_SPOTS);
		HSSFCell headCell = headRow.createCell(0);
		headCell.setCellValue("注册人员表");
		headCell.setCellStyle(headStyle);

		sheet.setColumnWidth(0, 10 * 256);
		sheet.setColumnWidth(1, 10 * 256);
		sheet.setColumnWidth(2, 10 * 256);
		sheet.setColumnWidth(3, 10 * 256);
		sheet.setColumnWidth(4, 10 * 256);
		sheet.setColumnWidth(5, 10 * 256);
		sheet.setColumnWidth(6, 10 * 256);
		sheet.setColumnWidth(7, 10 * 256);
		sheet.setColumnWidth(8, 10 * 256);

		HSSFRow row = sheet.createRow(1);

		HSSFCellStyle style = wb.createCellStyle();
		style.setAlignment(HSSFCellStyle.ALIGN_CENTER);

		try {
			HSSFCell cell = row.createCell(0);
			cell.setCellValue("用户编号");
			cell.setCellStyle(style);
			
			cell = row.createCell(0);
			cell.setCellValue("用户名称");
			cell.setCellStyle(style);
			
			cell = row.createCell(0);
			cell.setCellValue("职位");
			cell.setCellStyle(style);
			
			cell = row.createCell(0);
			cell.setCellValue("邮箱");
			cell.setCellStyle(style);

			cell = row.createCell(0);
			cell.setCellValue("电话");
			cell.setCellStyle(style);

			cell = row.createCell(0);
			cell.setCellValue("所在公司");
			cell.setCellStyle(style);

			cell = row.createCell(0);
			cell.setCellValue("所属省份");
			cell.setCellStyle(style);

			cell = row.createCell(0);
			cell.setCellValue("城市");
			cell.setCellStyle(style);

			cell = row.createCell(0);
			cell.setCellValue("备注");
			cell.setCellStyle(style);

			int cellLine = 1;
			for (UserInfo userInfo : userInfoList) {
				row = sheet.createRow(cellLine);
				
				cell = row.createCell(0);
				cell.setCellValue(userInfo.userInfoId);
				cell.setCellStyle(style);
				
				cell = row.createCell(1);
				cell.setCellValue(userInfo.userName);
				cell.setCellStyle(style);
				
				cell = row.createCell(2);
				cell.setCellValue(userInfo.position);
				cell.setCellStyle(style);

				cell = row.createCell(3);
				cell.setCellValue(userInfo.email);
				cell.setCellStyle(style);
				
				cell = row.createCell(4);
				cell.setCellValue(userInfo.phone);
				cell.setCellStyle(style);
				
				cell = row.createCell(5);
				cell.setCellValue(userInfo.company);
				cell.setCellStyle(style);
				
				cell = row.createCell(6);
				cell.setCellValue(userInfo.province);
				cell.setCellStyle(style);
				
				cell = row.createCell(7);
				cell.setCellValue(userInfo.city);
				cell.setCellStyle(style);
				
				cell = row.createCell(8);
				cell.setCellValue(userInfo.remark);
				cell.setCellStyle(style);
				
				cellLine ++;
			}
		} catch (Exception ex) {
			throw new RuntimeException(ex);
		} finally {
			try {
				wb.close();
			} catch (IOException e) {
				throw new RuntimeException("close HSSFWorkbook error ", e);
			}
		}
		
		String path = "./home/userInfo";  
		File file = new File(path);
		if (!file.exists()) {
			file.mkdir();
		}
		
		String name = "注册人员表.xls";
		File fileName = new File(path + "/" + name);
		fileName.createNewFile();
		
		try {
			FileOutputStream fo = new FileOutputStream(path + "/" + name);
			wb.write(fo);
			fo.close();
		} catch (IOException ex) {
			throw new RuntimeException("io exception", ex);
		} finally {
		}
		
		File fileRead = new File(path + "/" + name);
		HSSFWorkbook wbRead = new HSSFWorkbook(new FileInputStream(fileRead));
		
		httpResponse.addHeader("Content-Disposition", "attachment;filename=" + new String(name.getBytes("utf-8"),"iso8859-1"));
		httpResponse.addHeader("Content-Length", "" + fileRead.length());
		httpResponse.setContentType("application/vnd.ms-excel; charset=utf-8");
		
		OutputStream os = null;
		try {
			os = httpResponse.getOutputStream();
			wbRead.write(os);
		} catch (Exception ex) {
			throw new RuntimeException("io exception", ex);
		} finally {
			wbRead.close();
			os.flush();
			os.close();
			wb.close();
		}
	}
	
	@Override
	public void doGet(HttpServletRequest httpRequest, HttpServletResponse httpResponse) throws IOException {
		this.doPost(httpRequest, httpResponse);
	}

	private class UserInfo {
		private int userInfoId;
		private String userName;
		private String position;
		private String email;
		private String phone;
		private String company;
		private String province;
		private String city;
		private String remark;
		
		public UserInfo(int userInfoId, String userName, String position, String email, String phone, String company, String province, String city, String remark) {
			this.userInfoId = userInfoId;
			this.userName = userName;
			this.position = position;
			this.email = email;
			this.phone = phone;
			this.company = company;
			this.province = province;
			this.city = city;
			this.remark = remark;
		}
	}
}
