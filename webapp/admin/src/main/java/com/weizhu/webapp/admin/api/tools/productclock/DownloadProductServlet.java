package com.weizhu.webapp.admin.api.tools.productclock;

import java.io.IOException;
import java.util.Map;

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
import com.google.common.util.concurrent.Futures;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.weizhu.proto.AdminProtos.AdminHead;
import com.weizhu.proto.ProductclockProtos;
import com.weizhu.proto.ProductclockProtos.GetProductAdminRequest;
import com.weizhu.proto.ProductclockProtos.GetProductAdminResponse;
import com.weizhu.proto.ToolsProductclockService;
import com.weizhu.web.ParamUtil;
import com.weizhu.webapp.admin.api.exam.UserInfoUtil;
import com.weizhu.webapp.admin.api.tools.ToolsUtil;

@Singleton
public class DownloadProductServlet extends HttpServlet {

	private static final long serialVersionUID = 1L;

	private Provider<AdminHead> adminHeadProvider;
	private ToolsProductclockService toolsProductclockService;
	
	@Inject
	public DownloadProductServlet(Provider<AdminHead> adminHeadProvider, ToolsProductclockService toolsProductclockService) {
		this.adminHeadProvider = adminHeadProvider;
		this.toolsProductclockService = toolsProductclockService;
	}
	
	@Override
	public void doPost(HttpServletRequest httpRequest, HttpServletResponse httpResponse) throws IOException {
		final String productName = ParamUtil.getString(httpRequest, "product_name", null);
		
		SXSSFWorkbook wb = new SXSSFWorkbook();
		try {
			writeExecl(wb, productName);
			
			httpResponse.setContentType("text/plain");
			httpResponse.setHeader("Content-Disposition", "attachment;filename=" + new String("tools_productclock_product.xlsx".getBytes("utf-8"),"iso8859-1"));
			
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
	
	public void writeExecl(SXSSFWorkbook wb, @Nullable String productName) {
		SXSSFSheet sheet = wb.createSheet("产品详细");
		CellRangeAddress cra = new CellRangeAddress(0, 0, 0, 2);
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
		
		UserInfoUtil.createCell(row, 0, "产品名称", style, cellWidth);
		UserInfoUtil.createCell(row, 1, "产品描述", style, cellWidth);
		UserInfoUtil.createCell(row, 2, "默认提醒天数", style, cellWidth);
		
		final AdminHead adminHead = adminHeadProvider.get();
		
		GetProductAdminRequest.Builder requestBuilder = GetProductAdminRequest.newBuilder();
		if (productName != null) {
			requestBuilder.setProductName(productName);
		}
		
		int rowStart = 2;
		int start = 0;
		boolean hasMore = true;
		while (hasMore) {
			GetProductAdminResponse response = Futures.getUnchecked(toolsProductclockService.getProductAdmin(adminHead, requestBuilder
					.setStart(start)
					.setLength(50)
					.build()));
			for (ProductclockProtos.Product product : response.getProductList()) {
				SXSSFRow row1 = sheet.createRow(rowStart++);
				ToolsUtil.createCell(row1, 0, product.getProductName(), style, cellWidth);
				ToolsUtil.createCell(row1, 1, product.getProductDesc(), style, cellWidth);
				ToolsUtil.createCell(row1, 2, String.valueOf(product.getDefaultRemindDay()), style, cellWidth);
			}
			
			if (response.getProductCount() == 0) {
				hasMore = false;
			}
			start += 50;
		}
		
		ToolsUtil.adjustWidth(sheet, cellWidth);
	}
}
