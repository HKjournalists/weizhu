package com.weizhu.webapp.admin.api.discover;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import com.google.common.io.Resources;
import com.google.common.util.concurrent.Futures;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.weizhu.proto.AdminDiscoverProtos;
import com.weizhu.proto.AdminDiscoverService;
import com.weizhu.proto.AdminProtos;
import com.weizhu.proto.AdminProtos.AdminHead;
import com.weizhu.proto.AdminService;
import com.weizhu.proto.DiscoverV2Protos;
import com.weizhu.web.ParamUtil;

@Singleton
@SuppressWarnings("serial")
public class ExportDiscoverItemServlet extends HttpServlet {
	
	private final Provider<AdminHead> adminHeadProvider;
	private final AdminDiscoverService adminDiscoverService;
	private final AdminService adminService;

	@Inject
	public ExportDiscoverItemServlet(Provider<AdminHead> adminHeadProvider, AdminDiscoverService adminDiscoverService, AdminService adminService) {
		this.adminHeadProvider = adminHeadProvider;
		this.adminDiscoverService = adminDiscoverService;
		this.adminService = adminService;
	}

	@Override
	protected void doGet(HttpServletRequest httpRequest, HttpServletResponse httpResponse) throws ServletException, IOException {
		this.doPost(httpRequest, httpResponse);
	}

	@Override
	protected void doPost(HttpServletRequest httpRequest, HttpServletResponse httpResponse) throws ServletException, IOException {
		Integer categoryId = ParamUtil.getInt(httpRequest, "category_id", null);
		String itemName = ParamUtil.getString(httpRequest, "item_name", null);
		
		final AdminHead head = this.adminHeadProvider.get();
		
		final AdminDiscoverProtos.GetItemListRequest.Builder requestBuilder = AdminDiscoverProtos.GetItemListRequest.newBuilder();
		if (categoryId != null) {
			requestBuilder.setCategoryId(categoryId);
		}
		if (itemName != null) {
			requestBuilder.setItemName(itemName);
		}
		
		final SXSSFWorkbook wb = new SXSSFWorkbook(new XSSFWorkbook(Resources.getResource("com/weizhu/webapp/admin/api/discover/export_discover_item_file.xlsx").openStream()));
		try {
			CellStyle cellStyle = wb.createCellStyle(); //在工作薄的基础上建立一个样式
			cellStyle.setBorderBottom((short) 1); //设置边框样式
			cellStyle.setBorderLeft((short) 1); //左边框
			cellStyle.setBorderRight((short) 1); //右边框
			cellStyle.setBorderTop((short) 1); //顶边框

			Font font = wb.createFont();
			font.setFontHeight((short) 200);
			font.setFontName("微软雅黑");
			cellStyle.setFont(font);
			// titleStyle.setFillForegroundColor(XSSFColor.LIGHT_ORANGE.index);    //填充的背景颜色
			// titleStyle.setFillPattern(HSSFCellStyle.SOLID_FOREGROUND);    //填充图案
			
			Sheet sheet = wb.getSheetAt(0);
			
			int rowIdx = 2;
			int start = 0;
			final int length = 500;
			while (true) {
				AdminDiscoverProtos.GetItemListResponse response = Futures.getUnchecked(
						this.adminDiscoverService.getItemList(head, requestBuilder
								.setStart(start)
								.setLength(length)
								.build()));

				Map<Long, List<Integer>> itemCategoryIdMap = new TreeMap<Long, List<Integer>>();
				for (AdminDiscoverProtos.ItemCategory itemCategory : response.getRefItemCategoryList()) {
					itemCategoryIdMap.put(itemCategory.getItemId(), itemCategory.getCategoryIdList());
				}
				Map<Integer, DiscoverV2Protos.Module.Category> refCategoryMap = new TreeMap<Integer, DiscoverV2Protos.Module.Category>();
				for (DiscoverV2Protos.Module.Category category : response.getRefCategoryList()) {
					refCategoryMap.put(category.getCategoryId(), category);
				}
				Map<Integer, DiscoverV2Protos.Module> refModuleMap = new TreeMap<Integer, DiscoverV2Protos.Module>();
				for (DiscoverV2Protos.Module module : response.getRefModuleList()) {
					refModuleMap.put(module.getModuleId(), module);
				}

				// 获取admin信息,和allowModel信息
				Set<Long> adminIdSet = new TreeSet<Long>();
				for (DiscoverV2Protos.Item item : response.getItemList()) {
					if (item.getBase().hasCreateAdminId()) {
						adminIdSet.add(item.getBase().getCreateAdminId());
					}
					if (item.getBase().hasUpdateAdminId()) {
						adminIdSet.add(item.getBase().getUpdateAdminId());
					}
				}
				final Map<Long, AdminProtos.Admin> refAdminMap = DiscoverServletUtil.getAdminMap(adminService, head, adminIdSet);
				
				//写出到excel文件
				for (DiscoverV2Protos.Item item : response.getItemList()) {
					Row row = sheet.createRow(rowIdx++);
					row.createCell(0).setCellValue(item.getBase().getItemId());
					row.createCell(1).setCellValue(item.getBase().getItemName());
					row.createCell(2).setCellValue(item.getBase().getItemDesc());

					StringBuilder sb = new StringBuilder();
					List<Integer> categoryIdList = itemCategoryIdMap.get(item.getBase().getItemId());
					if (categoryIdList != null) {
						boolean isFirst = true;
						
						for (Integer catId : categoryIdList) {
							DiscoverV2Protos.Module.Category category = refCategoryMap.get(catId);
							DiscoverV2Protos.Module module = category == null ? null : refModuleMap.get(category.getModuleId());
							
							if (category != null && module != null) {
								if (isFirst) {
									isFirst = false;
								} else {
									sb.append(", ");
								}
								sb.append(module.getModuleName()).append("-").append(category.getCategoryName());
							}
						}
					}
					row.createCell(3).setCellValue(sb.toString());
					
					row.createCell(4).setCellValue(DiscoverServletUtil.getAdminName(refAdminMap, item.getBase().hasCreateAdminId(), item.getBase().getCreateAdminId()));
					row.createCell(5).setCellValue(DiscoverServletUtil.getDateStr(item.getBase().hasCreateTime(), item.getBase().getCreateTime()));
					row.createCell(6).setCellValue(item.getCount().getCommentCnt());
					row.createCell(7).setCellValue(item.getCount().getCommentUserCnt());
					row.createCell(8).setCellValue(item.getCount().getLearnCnt());
					row.createCell(9).setCellValue(item.getCount().getLearnUserCnt());
					row.createCell(10).setCellValue(item.getCount().getLikeCnt());
					row.createCell(11).setCellValue(item.getCount().getScoreNumber());
					row.createCell(12).setCellValue(item.getCount().getScoreUserCnt());
					row.createCell(13).setCellValue(item.getCount().getShareCnt());
					
					for (int i = 0; i <= 13; i++) {
						Cell cell = row.getCell(i);
						if (cell == null) {
							cell = row.createCell(i);
							cell.setCellValue("");
						}
						cell.setCellStyle(cellStyle);
					}
				}
				
				start += length;
				if (start >= response.getFilteredSize()) {
					break;
				}
			}
			
			httpResponse.setContentType("text/plain");
			httpResponse.setHeader("Content-Disposition", "attachment;filename=export_discover_item.xlsx");
			wb.write(httpResponse.getOutputStream());
			wb.close();
		} finally {
			wb.close();
		}
	}
}
