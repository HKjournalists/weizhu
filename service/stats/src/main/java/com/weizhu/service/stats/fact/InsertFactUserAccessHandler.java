package com.weizhu.service.stats.fact;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import com.google.common.collect.ImmutableList;
import com.google.gson.JsonObject;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.weizhu.service.stats.StatsUtil;

@Singleton
public class InsertFactUserAccessHandler implements InsertFactHandler {

	@Inject
	public InsertFactUserAccessHandler() {
	}
	
	@Override
	public String kafkaGroupId() {
		return "stats_fact_user_access_0";
	}
	
	private static final ImmutableList<String> KAFAK_TOPIC_LIST = ImmutableList.of("log_weizhu_api_access", "log_weizhu_webapp_access");

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
			String loggerName = StatsUtil.tryGetJsonString(jsonObj, "logger_name");
			if (!"weizhu_api_access".equals(loggerName) && !"weizhu_webapp_access".equals(loggerName)) {
				continue;
			}
			
			Long timestamp = StatsUtil.tryGetJsonLong(jsonObj, "timestamp");
			
			Long companyId;
			Long userId;
			if ("RequestHead".equals(StatsUtil.tryGetJsonString(jsonObj, "message.head.type"))) {
				companyId = StatsUtil.tryGetJsonLong(jsonObj, "message.head.data.session.company_id");
				userId = StatsUtil.tryGetJsonLong(jsonObj, "message.head.data.session.user_id");
			} else {
				continue;
			}
			
			if (timestamp != null && companyId != null && userId != null) {
				if (sqlBuilder == null) {
					sqlBuilder = new StringBuilder("INSERT INTO weizhu_stats_fact_user_access (log_date, company_id, user_id, log_cnt) VALUES ");
				} else {
					sqlBuilder.append(", ");
				}
				
				sqlBuilder.append("(");
				sqlBuilder.append(dateFormat.format(new Date(timestamp))).append(", ");
				sqlBuilder.append(companyId).append(", ");
				sqlBuilder.append(StatsUtil.toStatsUserId(companyId, userId)).append(", 1)");
			}
		}

		return sqlBuilder == null ? null : sqlBuilder.append(" ON DUPLICATE KEY UPDATE log_cnt = log_cnt + 1; ").toString();
	}

}
