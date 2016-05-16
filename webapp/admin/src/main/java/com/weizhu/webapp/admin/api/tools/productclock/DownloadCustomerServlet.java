package com.weizhu.webapp.admin.api.tools.productclock;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nullable;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.streaming.SXSSFCell;
import org.apache.poi.xssf.streaming.SXSSFRow;
import org.apache.poi.xssf.streaming.SXSSFSheet;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.common.util.concurrent.Futures;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.weizhu.proto.AdminProtos.AdminHead;
import com.weizhu.proto.AdminUserProtos.GetUserByIdRequest;
import com.weizhu.proto.AdminUserProtos.GetUserByIdResponse;
import com.weizhu.proto.AdminUserService;
import com.weizhu.proto.ProductclockProtos;
import com.weizhu.proto.ProductclockProtos.GetCustomerAdminRequest;
import com.weizhu.proto.ProductclockProtos.GetCustomerAdminResponse;
import com.weizhu.proto.ToolsProductclockService;
import com.weizhu.proto.UserProtos;
import com.weizhu.web.ParamUtil;
import com.weizhu.webapp.admin.api.tools.ToolsUtil;

@Singleton
public class DownloadCustomerServlet extends HttpServlet {

	private static final long serialVersionUID = 1L;

	private final Provider<AdminHead> adminHeadProvider;
	private final ToolsProductclockService toolsProductclockService;
	private final AdminUserService adminUserService;
	
	@Inject
	public DownloadCustomerServlet(Provider<AdminHead> adminHeadProvider, ToolsProductclockService toolsProductclockService,
			AdminUserService adminUserService) {
		this.adminHeadProvider = adminHeadProvider;
		this.toolsProductclockService = toolsProductclockService;
		this.adminUserService = adminUserService;
	}
	
	@Override
	public void doPost(HttpServletRequest httpRequest, HttpServletResponse httpResponse) throws IOException {
		final List<Long> salerIdList = ParamUtil.getLongList(httpRequest, "saler_id", Collections.emptyList());
		final Boolean hasProduct = ParamUtil.getBoolean(httpRequest, "has_product", null);
		
		SXSSFWorkbook wb = new SXSSFWorkbook();
		try {
			writeExecl(wb, salerIdList, hasProduct);
			
			httpResponse.setContentType("text/plain");
			httpResponse.setHeader("Content-Disposition", "attachment;filename=" + new String("tool_productclock_customer.xlsx".getBytes("utf-8"),"iso8859-1"));
			
			wb.write(httpResponse.getOutputStream());
		} catch (Exception ex) {
			
		} finally {
			if (wb != null) {
				wb.close();
			}
		}
	}
	
	@Override
	public void doGet(HttpServletRequest httpRequest, HttpServletResponse httpResponse) throws IOException {
		this.doPost(httpRequest, httpResponse);
	}
	
	public void writeExecl(SXSSFWorkbook wb, List<Long> salerIdList, @Nullable Boolean hasProduct) {
		SXSSFSheet sheet = wb.createSheet("产品详细");
		CellRangeAddress cra = new CellRangeAddress(0, 0, 0, 10);
		sheet.addMergedRegion(cra);

		SXSSFRow headRow = sheet.createRow(0);
		CellStyle headStyle = wb.createCellStyle();
		headStyle.setAlignment(CellStyle.ALIGN_CENTER);
		SXSSFCell headCell = headRow.createCell(0);
		headCell.setCellValue("产品详细表");
		headCell.setCellStyle(headStyle);
		
		// 表的列名称
		SXSSFRow row = sheet.createRow(1);
		CellStyle style = wb.createCellStyle();
		style.setAlignment(CellStyle.ALIGN_CENTER);
		
		Map<Integer, Integer> cellWidth = Maps.newHashMap();
		
		ToolsUtil.createCell(row, 0, "客户名称", style, cellWidth);
		ToolsUtil.createCell(row, 1, "手机号", style, cellWidth);
		ToolsUtil.createCell(row, 2, "性别", style, cellWidth);
		ToolsUtil.createCell(row, 3, "生日（阳历）", style, cellWidth);
		ToolsUtil.createCell(row, 4, "生日（阴历）", style, cellWidth);
		ToolsUtil.createCell(row, 5, "结婚纪念日（阳历）", style, cellWidth);
		ToolsUtil.createCell(row, 6, "结婚纪念日（阴历）", style, cellWidth);
		ToolsUtil.createCell(row, 7, "地址", style, cellWidth);
		ToolsUtil.createCell(row, 8, "备注", style, cellWidth);
		ToolsUtil.createCell(row, 9, "所属销售", style, cellWidth);
		ToolsUtil.createCell(row, 10, "是否提醒", style, cellWidth);
		
		final AdminHead adminHead = adminHeadProvider.get();
		
		GetCustomerAdminRequest.Builder requestBuilder = GetCustomerAdminRequest.newBuilder();
		if (!salerIdList.isEmpty()) {
			requestBuilder.addAllSalerId(salerIdList);
		}
		if (hasProduct != null) {
			requestBuilder.setHasProduct(hasProduct);
		}
		
		int rowStart = 2;
		int start = 0;
		boolean hasMore = true;
		while (hasMore) {
			GetCustomerAdminResponse response = Futures.getUnchecked(toolsProductclockService.getCustomerAdmin(adminHead, requestBuilder
					.setStart(start)
					.setLength(50)
					.build()));
			
			Set<Long> salerIdSet = Sets.newTreeSet();
			for (ProductclockProtos.Customer customer : response.getCustomerList()) {
				if (customer.hasBelongUser()) {
					salerIdSet.add(customer.getBelongUser());
				}
			}
			GetUserByIdResponse getUserByIdResponse = Futures.getUnchecked(adminUserService.getUserById(adminHead, GetUserByIdRequest.newBuilder()
					.addAllUserId(salerIdSet)
					.build()));
			Map<Long, UserProtos.User> userMap = Maps.newHashMap();
			for (UserProtos.User user : getUserByIdResponse.getUserList()) {
				userMap.put(user.getBase().getUserId(), user);
			}
			
			SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd");
			
			for (ProductclockProtos.Customer customer : response.getCustomerList()) {
				SXSSFRow row1 = sheet.createRow(rowStart++);
				ToolsUtil.createCell(row1, 0, customer.getCustomerName(), style, cellWidth);
				ToolsUtil.createCell(row1, 1, customer.hasMobileNo() ? customer.getMobileNo() : "", style, cellWidth);
				ToolsUtil.createCell(row1, 2, customer.hasGender() ? (customer.getGender().equals(ProductclockProtos.Gender.MALE) ? "男" : "女") : "", style, cellWidth);
				ToolsUtil.createCell(row1, 3, customer.hasBirthdaySolar() && customer.getBirthdaySolar() != 0 ? 
						df.format(new Date(customer.getBirthdaySolar() * 1000L)) : 
							"", style, cellWidth);
				ToolsUtil.createCell(row1, 4, customer.hasBirthdayLunar() && checkTime(customer.getBirthdayLunar()) != 0L ? 
						ToolsUtil.solarTolunarTimeStamp(customer.getBirthdayLunar()) : 
							"", style, cellWidth);
				ToolsUtil.createCell(row1, 5, customer.hasWeddingSolar() && customer.getWeddingSolar() != 0 ?
						df.format(new Date(customer.getWeddingSolar() * 1000L)) : 
							"", style, cellWidth);
				ToolsUtil.createCell(row1, 6, customer.hasWeddingLunar() && checkTime(customer.getWeddingLunar()) != 0L ? 
						ToolsUtil.solarTolunarTimeStamp(customer.getWeddingLunar()) : 
							"", style, cellWidth);
				ToolsUtil.createCell(row1, 7, customer.hasAddress() ? customer.getAddress() : "", style, cellWidth);
				ToolsUtil.createCell(row1, 8, customer.hasRemark() ? customer.getRemark() : "", style, cellWidth);
				if (customer.hasBelongUser()) {
					UserProtos.User user = userMap.get(customer.getBelongUser());
					if (user != null) {
						ToolsUtil.createCell(row1, 9, user.getBase().getUserName(), style, cellWidth);
					}
				}
				ToolsUtil.createCell(row1, 10, customer.getIsRemind() ? "是" : "否", style, cellWidth);
			}
			
			if (response.getCustomerCount() == 0) {
				hasMore = false;
			}
			start += 50;
		}
		
		ToolsUtil.adjustWidth(sheet, cellWidth);
	}
	
	private long checkTime(int timeStamp) {
		long time = timeStamp * 1000L;
		if (time > 1798732800000L || time < -1073030400000L) {
			return 0L;
		}
		return time;
	}
	
}
