package com.weizhu.service.stats.fact;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import com.google.common.collect.ImmutableList;
import com.google.gson.JsonObject;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.weizhu.common.db.DBUtil;
import com.weizhu.service.stats.StatsUtil;

@Singleton
public class InsertFactWeizhuVersionHandler implements InsertFactHandler {

	@Inject
	public InsertFactWeizhuVersionHandler() {
	}
	
	@Override
	public String kafkaGroupId() {
		return "stats_fact_weizhu_version_0";
	}
	
	private static final ImmutableList<String> KAFAK_TOPIC_LIST = ImmutableList.of("log_weizhu_api_access");

	@Override
	public List<String> kafkaTopicList() {
		return KAFAK_TOPIC_LIST;
	}

	@Override
	public String handleInsertSQL(List<JsonObject> jsonList) {
		if (jsonList.isEmpty()) {
			return null;
		}
		
		final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd");
		
		StringBuilder sqlBuilder = null;
		for (JsonObject jsonObj : jsonList) {
			if (!"weizhu_api_access".equals(StatsUtil.tryGetJsonString(jsonObj, "logger_name"))) {
				continue;
			}
			
			Long timestamp = StatsUtil.tryGetJsonLong(jsonObj, "timestamp");
			String platform = StatsUtil.tryGetJsonString(jsonObj, "message.head.data.weizhu.platform");
			String versionName = StatsUtil.tryGetJsonString(jsonObj, "message.head.data.weizhu.version_name");
			Integer versionCode = StatsUtil.tryGetJsonInt(jsonObj, "message.head.data.weizhu.version_code");
			
			Long companyId;
			Long userId;
			String headType = StatsUtil.tryGetJsonString(jsonObj, "message.head.type");
			if ("RequestHead".equals(headType)) {
				companyId = StatsUtil.tryGetJsonLong(jsonObj, "message.head.data.session.company_id");
				userId = StatsUtil.tryGetJsonLong(jsonObj, "message.head.data.session.user_id");
			} else if ("AnonymousHead".equals(headType)) {
				companyId = StatsUtil.tryGetJsonLong(jsonObj, "message.head.data.company_id");
				userId = null;
			} else {
				continue;
			}
			
			if (timestamp != null && platform != null && versionName != null && versionCode != null) {
				if (sqlBuilder == null) {
					sqlBuilder = new StringBuilder("INSERT INTO weizhu_stats_fact_weizhu_version (log_date, platform, version_name, version_code, company_id, user_id, log_cnt) VALUES ");
				} else {
					sqlBuilder.append(", ");
				}
				
				sqlBuilder.append("(");
				sqlBuilder.append(dateFormat.format(new Date(timestamp))).append(", '");
				sqlBuilder.append(DBUtil.SQL_STRING_ESCAPER.escape(StatsUtil.trimToSize(platform, 10))).append("', '");
				sqlBuilder.append(DBUtil.SQL_STRING_ESCAPER.escape(StatsUtil.trimToSize(versionName, 50))).append("', ");
				sqlBuilder.append(versionCode).append(", ");
				sqlBuilder.append(companyId == null ? "0" : companyId).append(", ");
				sqlBuilder.append(userId == null ? "0" : StatsUtil.toStatsUserId(companyId, userId)).append(", 1)");
			}
		}
		
		return sqlBuilder == null ? null : sqlBuilder.append(" ON DUPLICATE KEY UPDATE log_cnt = log_cnt + 1; ").toString();
	}

}
