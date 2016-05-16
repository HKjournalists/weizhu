package com.weizhu.service.stats.dim;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ExecutionException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.weizhu.common.db.DBUtil;
import com.weizhu.common.service.ServiceUtil;
import com.weizhu.proto.AdminDiscoverProtos.GetItemListRequest;
import com.weizhu.proto.AdminDiscoverProtos.GetItemListResponse;
import com.weizhu.proto.AdminDiscoverProtos;
import com.weizhu.proto.AdminDiscoverService;
import com.weizhu.proto.CompanyProtos;
import com.weizhu.proto.CompanyService;
import com.weizhu.proto.DiscoverV2Protos;
import com.weizhu.proto.WeizhuProtos.SystemHead;
import com.weizhu.service.stats.StatsUtil;
import com.zaxxer.hikari.HikariDataSource;

public class LoadDimDiscoverTask implements Runnable {

	private static final Logger logger = LoggerFactory.getLogger(LoadDimDiscoverTask.class);
	
	private final HikariDataSource hikariDataSource;
	private final CompanyService companyService;
	private final AdminDiscoverService adminDiscoverService;
	
	public LoadDimDiscoverTask(
			HikariDataSource hikariDataSource, 
			CompanyService companyService, 
			AdminDiscoverService adminDiscoverService
			) {
		this.hikariDataSource = hikariDataSource;
		this.companyService = companyService;
		this.adminDiscoverService = adminDiscoverService;
	}
	
	@Override
	public void run() {
		Connection dbConn = null;
		try {
			dbConn = this.hikariDataSource.getConnection();
			
			List<CompanyProtos.Company> companyList = this.companyService.getCompanyList(
					SystemHead.newBuilder().build(), ServiceUtil.EMPTY_REQUEST
					).get().getCompanyList();
			if (companyList.isEmpty()) {
				return;
			}
			
			dbConn = this.hikariDataSource.getConnection();
			
			StatsUtil.replaceCompany(dbConn, companyList);
			
			for (CompanyProtos.Company company : companyList) {
				replaceDiscoverItem(dbConn, company.getCompanyId());
			}
		} catch (Throwable th) {
			logger.error("LoadDimDateTask fail!", th);
		} finally {
			DBUtil.closeQuietly(dbConn);
		}
	}
	
	private void replaceDiscoverItem(Connection dbConn, long companyId) throws InterruptedException, ExecutionException, SQLException {
		
		final SimpleDateFormat timeFormat = new SimpleDateFormat("yyyyMMddHHmmss");
		
		final SystemHead head = SystemHead.newBuilder().setCompanyId(companyId).build();
		int start = 0;
		final int length = 500;
		while (true) {
			GetItemListResponse getDiscoverItemResponse = this.adminDiscoverService.getItemList(head, 
					GetItemListRequest.newBuilder()
						.setStart(start)
						.setLength(length)
						.build()).get();
			
			if (getDiscoverItemResponse.getItemCount() <= 0) {
				break;
			}
			
			Map<Integer, DiscoverV2Protos.Module> refModuleMap = new TreeMap<Integer, DiscoverV2Protos.Module>();
			for (DiscoverV2Protos.Module module : getDiscoverItemResponse.getRefModuleList()) {
				refModuleMap.put(module.getModuleId(), module);
			}
			
			Map<Integer, DiscoverV2Protos.Module.Category> refCategoryMap = new TreeMap<Integer, DiscoverV2Protos.Module.Category>();
			for (DiscoverV2Protos.Module.Category category : getDiscoverItemResponse.getRefCategoryList()) {
				if (refModuleMap.containsKey(category.getModuleId())) {
					refCategoryMap.put(category.getCategoryId(), category);
				}
			}
			
			Map<Long, Integer> itemCategoryIdMap = new TreeMap<Long, Integer>();
			for (AdminDiscoverProtos.ItemCategory itemCategory : getDiscoverItemResponse.getRefItemCategoryList()) {
				Integer categoryId = null;
				// find min categoryId
				for (Integer tmpCategoryId : itemCategory.getCategoryIdList()) {
					if ((categoryId == null || categoryId > tmpCategoryId) && refCategoryMap.containsKey(tmpCategoryId)) {
						categoryId = tmpCategoryId;
					}
				}
				if (categoryId != null) {
					itemCategoryIdMap.put(itemCategory.getItemId(), categoryId);
				}
			}
			
			StringBuilder sqlBuilder = new StringBuilder();
			sqlBuilder.append("REPLACE INTO weizhu_stats_dim_discover_item (item_id, item_name, state, create_time, module_id, module_name, category_id, category_name) VALUES ");
			
			boolean isFirst = true;
			for (DiscoverV2Protos.Item item : getDiscoverItemResponse.getItemList()) {
				if (isFirst) {
					isFirst = false;
				} else {
					sqlBuilder.append(", ");
				}
				
				sqlBuilder.append("(");
				sqlBuilder.append(StatsUtil.toStatsDiscoverItemId(companyId, item.getBase().getItemId())).append(", '");
				sqlBuilder.append(DBUtil.SQL_STRING_ESCAPER.escape(StatsUtil.trimToSize(item.getBase().getItemName(), 50))).append("', '");
				sqlBuilder.append(DBUtil.SQL_STRING_ESCAPER.escape(StatsUtil.trimToSize(item.getBase().getState().name(), 10))).append("', ");
				
				if (item.getBase().hasCreateTime()) {
					sqlBuilder.append(timeFormat.format(new Date(item.getBase().getCreateTime() * 1000L)));
				} else {
					sqlBuilder.append("20150101000000");
				}
				sqlBuilder.append(", ");
				
				Integer categoryId = itemCategoryIdMap.get(item.getBase().getItemId());
				DiscoverV2Protos.Module.Category category = categoryId == null ? null : refCategoryMap.get(categoryId);
				DiscoverV2Protos.Module module = category == null ? null : refModuleMap.get(category.getModuleId());
				if (category != null && module != null) {
					sqlBuilder.append(StatsUtil.toStatsDiscoverModuleId(companyId, module.getModuleId())).append(", '");
					sqlBuilder.append(DBUtil.SQL_STRING_ESCAPER.escape(StatsUtil.trimToSize(module.getModuleName(), 20))).append("', ");
					sqlBuilder.append(StatsUtil.toStatsDiscoverCategoryId(companyId, category.getCategoryId())).append(", '");
					sqlBuilder.append(DBUtil.SQL_STRING_ESCAPER.escape(StatsUtil.trimToSize(category.getCategoryName(), 20))).append("'");
				} else {
					sqlBuilder.append("NULL, NULL, NULL, NULL");
				}
				
				sqlBuilder.append(")");
			}
			
			final String sql = sqlBuilder.toString();
			Statement stmt = dbConn.createStatement();
			try {
				stmt.executeUpdate(sql);
			} finally {
				DBUtil.closeQuietly(stmt);
			}
			
			start += length;
			if (start >= getDiscoverItemResponse.getFilteredSize()) {
				break;
			}
		}
	}
	
}
