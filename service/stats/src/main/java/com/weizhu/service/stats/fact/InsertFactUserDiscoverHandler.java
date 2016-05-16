package com.weizhu.service.stats.fact;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.inject.Singleton;

import com.google.common.collect.ImmutableList;
import com.google.gson.JsonObject;
import com.google.inject.Inject;
import com.weizhu.common.db.DBUtil;
import com.weizhu.service.stats.StatsUtil;

@Singleton
public class InsertFactUserDiscoverHandler implements InsertFactHandler {

	@Inject
	public InsertFactUserDiscoverHandler() {
	}
	
	@Override
	public String kafkaGroupId() {
		return "stats_fact_user_discover_0";
	}
	
	private static final ImmutableList<String> KAFAK_TOPIC_LIST = ImmutableList.of("log_weizhu_service_invoke_write", "log_weizhu_webapp_access");

	@Override
	public List<String> kafkaTopicList() {
		return KAFAK_TOPIC_LIST;
	}
	
	private static final Pattern ITEM_ID_PATTERN = Pattern.compile("item_id=(\\d+)");

	@Override
	public String handleInsertSQL(List<JsonObject> jsonList) {
		if (jsonList.isEmpty()) {
			return null;
		}
		
		final SimpleDateFormat timeFormat = new SimpleDateFormat("yyyyMMddHHmmss");
		final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd");
		
		StringBuilder sqlBuilder = null;
		for (JsonObject jsonObj : jsonList) {
			
			Long timestamp = StatsUtil.tryGetJsonLong(jsonObj, "timestamp");
			
			Long companyId;
			Long userId;
			Long sessionId;
			String function;
			Long itemId;
			String result;
			
			String loggerName = StatsUtil.tryGetJsonString(jsonObj, "logger_name");
			if ("weizhu_service_invoke_write".equals(loggerName)) {
				
				if (!"RequestHead".equals(StatsUtil.tryGetJsonString(jsonObj, "message.head.type"))) {
					continue;
				}
				if (!"DiscoverV2Service".equals(StatsUtil.tryGetJsonString(jsonObj, "message.service"))) {
					continue;
				}
				
				companyId = StatsUtil.tryGetJsonLong(jsonObj, "message.head.data.session.company_id");
				userId = StatsUtil.tryGetJsonLong(jsonObj, "message.head.data.session.user_id");
				sessionId = StatsUtil.tryGetJsonLong(jsonObj, "message.head.data.session.session_id");
				function = StatsUtil.tryGetJsonString(jsonObj, "message.function");
				itemId = StatsUtil.tryGetJsonLong(jsonObj, "message.request.data.item_id");
				
				JsonObject responseObj = StatsUtil.tryGetJsonObject(jsonObj, "message.response");
				if (responseObj == null) {
					result = "INTERNAL_EXCEPTION";
				} else {
					result = StatsUtil.tryGetJsonString(responseObj, "data.result");
				}
			} else if ("weizhu_webapp_access".equals(loggerName)) {
				
				if (!"RequestHead".equals(StatsUtil.tryGetJsonString(jsonObj, "message.head.type"))) {
					continue;
				}
				if (!"/mobile/discover/item_content".equals(StatsUtil.tryGetJsonString(jsonObj, "message.request.request_uri"))) {
					continue;
				}
				
				companyId = StatsUtil.tryGetJsonLong(jsonObj, "message.head.data.session.company_id");
				userId = StatsUtil.tryGetJsonLong(jsonObj, "message.head.data.session.user_id");
				sessionId = StatsUtil.tryGetJsonLong(jsonObj, "message.head.data.session.session_id");
				function = "learnItem";
				
				itemId = null;
				String queryStr = StatsUtil.tryGetJsonString(jsonObj, "message.request.query_str");
				if (queryStr != null) {
					Matcher m = ITEM_ID_PATTERN.matcher(queryStr);
					if (m.find()) {
						try {
							itemId = Long.parseLong(m.group(1));
						} catch (NumberFormatException e) {
						}
					}
				}
				
				result = "SUCC";
			} else {
				continue;
			}
			
			if (timestamp != null && companyId != null && userId != null && sessionId != null && function != null && itemId != null) {
				if (sqlBuilder == null) {
					sqlBuilder = new StringBuilder("INSERT INTO weizhu_stats_fact_user_discover (log_time, log_date, company_id, user_id, session_id, function, item_id, result) VALUES ");
				} else {
					sqlBuilder.append(", ");
				}
				
				sqlBuilder.append("(");
				sqlBuilder.append(timeFormat.format(new Date(timestamp))).append(", ");
				sqlBuilder.append(dateFormat.format(new Date(timestamp))).append(", ");
				sqlBuilder.append(companyId).append(", ");
				sqlBuilder.append(StatsUtil.toStatsUserId(companyId, userId)).append(", ");
				sqlBuilder.append(sessionId).append(", '");
				sqlBuilder.append(DBUtil.SQL_STRING_ESCAPER.escape(StatsUtil.trimToSize(function, 50))).append("', ");
				sqlBuilder.append(StatsUtil.toStatsDiscoverItemId(companyId, itemId)).append(", ");
				sqlBuilder.append(result == null ? "NULL" : "'" + DBUtil.SQL_STRING_ESCAPER.escape(StatsUtil.trimToSize(result, 50)) + "'").append(")");
			}
		}

		return sqlBuilder == null ? null : sqlBuilder.append("; ").toString();
	}
}
