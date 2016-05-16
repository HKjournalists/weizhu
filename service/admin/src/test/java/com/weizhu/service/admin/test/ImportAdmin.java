package com.weizhu.service.admin.test;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.PushbackInputStream;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.TreeMap;

import org.apache.poi.POIXMLDocument;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.openxml4j.opc.OPCPackage;
import org.apache.poi.poifs.filesystem.POIFSFileSystem;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import com.google.common.base.Charsets;
import com.google.common.hash.Hashing;
import com.google.common.io.Resources;
import com.weizhu.common.db.DBUtil;
import com.weizhu.common.utils.EmailUtil;
import com.weizhu.common.utils.PasswordUtil;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

public class ImportAdmin {

	public static void main(String[] args) throws Exception {
		
		// 1. 获取所有历史管理员id, 公司id, 邮箱
		
		Map<String, Admin> oldAdminMap = loadOldAdmin();
		
		// 2. 获取导入的 公司名，邮箱名，管理员名称
		
		Map<Long, String> companyMap = new TreeMap<Long, String>();
		companyMap.put(2L, "海尔");
		companyMap.put(5L, "海信");
		companyMap.put(6L, "容声");
		companyMap.put(7L, "海信服务");
		companyMap.put(14L, "乐视-新");
		companyMap.put(3L, "圆通");
		companyMap.put(15L, "红领");
		
		Map<String, Admin> newAdminMap = loadNewAdmin(companyMap);
		
		System.out.println(newAdminMap);
		
		// 3. 生成插入sql和 公司名, 邮箱名, 管理员名称, 随机密码 excel
		
		List<Admin> adminList = new ArrayList<Admin>();
		
		final Random rand = new Random();
		long adminIdGen = 100500;
		for (Admin admin : newAdminMap.values()) {
			admin.password = generatePassword(rand);
			
			Admin oldAdmin = oldAdminMap.get(admin.adminEmail);
			if (oldAdmin == null) {
				admin.adminId = adminIdGen ++;
				adminList.add(admin);
			} else {
				if (admin.companyId == oldAdmin.companyId) {
					admin.adminId = oldAdmin.adminId;
					adminList.add(admin);
				} else {
					System.err.println("invalid admin company id : " + admin.companyId + ", " + oldAdmin.companyId + ", " + admin.adminEmail);
				}
			}			
		}
		
		Collections.sort(adminList, new Comparator<Admin>() {

			@Override
			public int compare(Admin o1, Admin o2) {
				int cmp = Long.compare(o1.companyId, o2.companyId);
				if (cmp != 0) {
					return cmp;
				}
				cmp = Long.compare(o1.adminId, o2.adminId);
				if (cmp != 0) {
					return cmp;
				}
				
				return o1.adminEmail.compareTo(o2.adminEmail);
			}
		
		});
		
//		System.out.println(adminList);
		
		PrintWriter pw = new PrintWriter(new File("admin_import.sql"));
		
		for (Admin admin : adminList) {
			
			pw.append("INSERT INTO weizhu_admin (admin_id, admin_name, admin_email, admin_email_unique, admin_password, force_reset_password, state, create_time) VALUES (");
			pw.append(Long.toString(admin.adminId)).append(", '");
			pw.append(DBUtil.SQL_STRING_ESCAPER.escape(admin.adminName)).append("', '");
			pw.append(DBUtil.SQL_STRING_ESCAPER.escape(admin.adminEmail)).append("', '");
			pw.append(DBUtil.SQL_STRING_ESCAPER.escape(admin.adminEmail)).append("', '");
			pw.append(DBUtil.SQL_STRING_ESCAPER.escape(Hashing.sha1().hashString(admin.password + "#@D5$k3(z7!~admin@2016", Charsets.UTF_8).toString())).append("', 0, 'NORMAL', ");
			pw.append(Integer.toString((int) (System.currentTimeMillis() / 1000L))).append("); ");
			pw.println();
			
			pw.append("INSERT INTO weizhu_admin_company (admin_id, company_id, enable_team_permit) VALUES (");
			pw.append(Long.toString(admin.adminId)).append(", ");
			pw.append(Long.toString(admin.companyId)).append(", 0); ");
			pw.println();
		}
		
		pw.close();
		
		XSSFWorkbook wb = new XSSFWorkbook();
		
		Sheet sheet = wb.createSheet();
		
		int idx = 0;
		for (Admin admin : adminList) {
			Row row = sheet.createRow(idx ++);
			
			row.createCell(0).setCellValue(companyMap.get(admin.companyId));
			row.createCell(1).setCellValue(admin.adminName);
			row.createCell(2).setCellValue(admin.adminEmail);
			row.createCell(3).setCellValue(admin.password);
		}
		
		wb.write(new FileOutputStream("admin.xlsx"));
		wb.close();
	}
	
	private static String generatePassword(Random rand) {
		List<Character> charList = new ArrayList<Character>();
		for (char c = '0'; c <= '9'; ++c) {
			charList.add(c);
		}
		for (char c = 'a'; c <= 'z'; ++c) {
			charList.add(c);
		}
		for (char c = 'A'; c <= 'Z'; ++c) {
			charList.add(c);
		}
		while (true) {
			Collections.shuffle(charList, rand);
			char[] cs = new char[8];
			for (int i=0; i<8; ++i) {
				cs[i] = charList.get(i);
			}
			String str = new String(cs);
			
			if (PasswordUtil.isValid(str)) {
				return str;
			}
		}
	}
	
	private static Map<String, Admin> loadOldAdmin() throws Exception {
		
		HikariConfig config = new HikariConfig();
		config.setJdbcUrl("jdbc:mysql://127.0.0.1:3306/weizhu_common?allowMultiQueries=true");
		config.setUsername("root");
		//config.setPassword("51mp50n");
		config.addDataSourceProperty("cachePrepStmts", "true");
		config.addDataSourceProperty("prepStmtCacheSize", "250");
		config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
		config.addDataSourceProperty("useServerPrepStmts", "true");
		
		HikariDataSource hikariDataSource = new HikariDataSource(config);
		
		Connection dbConn = null;
		Statement stmt = null;
		ResultSet rs = null;
		try {
			dbConn = hikariDataSource.getConnection();
			
			stmt = dbConn.createStatement();
			
			rs = stmt.executeQuery("SELECT * FROM weizhu_admin; ");
			
			Map<String, Admin> adminMap = new TreeMap<String, Admin>();
			while (rs.next()) {
				Admin admin = new Admin();
				admin.adminId = rs.getLong("admin_id");
				admin.companyId = rs.getLong("company_id");
				admin.adminEmail = rs.getString("admin_email");
				admin.adminName = rs.getString("admin_name");
				
				adminMap.put(admin.adminEmail, admin);
			}
			return adminMap;
		} finally {
			DBUtil.closeQuietly(rs);
			DBUtil.closeQuietly(stmt);
			DBUtil.closeQuietly(dbConn);
			hikariDataSource.close();
		}
	}
	
	private static Map<String, Admin> loadNewAdmin(Map<Long, String> companyMap) throws Exception {
		Workbook wb = null; 
		try {
			InputStream input = Resources.getResource("com/weizhu/service/admin/test/import_admin.xls").openStream();
			if (!input.markSupported()) {
				input = new PushbackInputStream(input, 8);
			}
			if (POIFSFileSystem.hasPOIFSHeader(input)) {
				wb = new HSSFWorkbook(input);
			} else if (POIXMLDocument.hasOOXMLHeader(input)) {
				wb = new XSSFWorkbook(OPCPackage.open(input));
			} else {
				throw new RuntimeException("invalid import file");
			}
			
			Map<String, Admin> adminMap = new TreeMap<String, Admin>();
			for (Sheet sheet : wb) {
				final long companyId;
				
				Long tmp = null;
				for (Entry<Long, String> entry : companyMap.entrySet()) {
					if (entry.getValue().equals(sheet.getSheetName())) {
						tmp = entry.getKey();
						break;
					}
				}
				if (tmp != null) {
					companyId = tmp;
				} else {
					System.err.println("unknown company : " + sheet.getSheetName());
					continue;
				}
				
				int nameIdx = 0;
				int emailIdx = 1;
				if ("乐视-新".equals(sheet.getSheetName())) {
					nameIdx = 1;
					emailIdx = 3;
				}
				
				for (Row row : sheet) {
					String name = getValue(row.getCell(nameIdx));
					String email = getValue(row.getCell(emailIdx));
					
					if (EmailUtil.isValid(email) && !email.endsWith("wehelpu.cn") && !email.equals("root@weizhu.com")) {
						Admin admin = new Admin();
						
						admin.adminId = 0;
						admin.companyId = companyId;
						admin.adminEmail = email;
						admin.adminName = name;
						
						adminMap.put(admin.adminEmail, admin);
					} else {
						System.err.println("invalid email : " + companyId + ", " + name + ", " + email);
					}
				}
			}
			return adminMap;
		} finally {
			if (wb != null) {
				wb.close();
			}
		}
	}
	
	private static String getValue(Cell cell) {
		if (cell == null) {
			return "";
		}
		switch (cell.getCellType()) {
			case Cell.CELL_TYPE_BLANK:
				return "";
			case Cell.CELL_TYPE_BOOLEAN:
				return Boolean.toString(cell.getBooleanCellValue());
			case Cell.CELL_TYPE_ERROR:
				return "";
			case Cell.CELL_TYPE_FORMULA:
				return "";
			case Cell.CELL_TYPE_NUMERIC:
				DecimalFormat df = new DecimalFormat("0.#");
				df.setDecimalSeparatorAlwaysShown(false);
				return df.format(cell.getNumericCellValue());
			case Cell.CELL_TYPE_STRING:
				String str = cell.getStringCellValue();
				return str == null ? "" : str.trim();
			default:
				return "";
		}
	}
	
	private static class Admin {
		long adminId;
		long companyId;
		String adminEmail;
		String adminName;
		String password;
		
		@Override
		public String toString() {
			return "Admin [adminId=" + adminId + ", companyId=" + companyId + ", adminEmail=" + adminEmail
					+ ", adminName=" + adminName + ", password=" + password + "]";
		}
	}
}