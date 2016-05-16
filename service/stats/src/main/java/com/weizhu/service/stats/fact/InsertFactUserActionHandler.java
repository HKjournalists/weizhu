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
public class InsertFactUserActionHandler implements InsertFactHandler {

	@Inject
	public InsertFactUserActionHandler() {
	}
	
	@Override
	public String kafkaGroupId() {
		return "stats_fact_user_action_0";
	}
	
	private static final ImmutableList<String> KAFAK_TOPIC_LIST = ImmutableList.of("log_weizhu_service_invoke_write");

	@Override
	public List<String> kafkaTopicList() {
		return KAFAK_TOPIC_LIST;
	}

	@Override
	public String handleInsertSQL(List<JsonObject> jsonList) {
		if (jsonList.isEmpty()) {
			return null;
		}
		
		final SimpleDateFormat timeFormat = new SimpleDateFormat("yyyyMMddHHmmss");
		final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd");
		
		StringBuilder sqlBuilder = null;
		for (JsonObject jsonObj : jsonList) {
			if (!"weizhu_service_invoke_write".equals(StatsUtil.tryGetJsonString(jsonObj, "logger_name"))) {
				continue;
			}
			
			String headType = StatsUtil.tryGetJsonString(jsonObj, "message.head.type");
			if (!"RequestHead".equals(headType) && !"AnonymousHead".equals(headType)) {
				continue;
			}
			
			Long timestamp = StatsUtil.tryGetJsonLong(jsonObj, "timestamp");
			Integer duration = StatsUtil.tryGetJsonInt(jsonObj, "message.duration");
			String server = StatsUtil.tryGetJsonString(jsonObj, "message.server");
			String service = StatsUtil.tryGetJsonString(jsonObj, "message.service");
			String function = StatsUtil.tryGetJsonString(jsonObj, "message.function");
			
			Long companyId;
			Long userId;
			Long sessionId;
			if ("RequestHead".equals(headType)) {
				companyId = StatsUtil.tryGetJsonLong(jsonObj, "message.head.data.session.company_id");
				userId = StatsUtil.tryGetJsonLong(jsonObj, "message.head.data.session.user_id");
				sessionId = StatsUtil.tryGetJsonLong(jsonObj, "message.head.data.session.session_id");
			} else if ("AnonymousHead".equals(headType)) {
				if ("SessionService".equals(service) && "createSessionKey".equals(function)) {
					if (StatsUtil.tryGetJsonObject(jsonObj, "message.response.data.session") != null) {
						companyId = StatsUtil.tryGetJsonLong(jsonObj, "message.response.data.session.company_id");
						userId = StatsUtil.tryGetJsonLong(jsonObj, "message.response.data.session.user_id");
						sessionId = StatsUtil.tryGetJsonLong(jsonObj, "message.response.data.session.session_id");
					} else {
						companyId = StatsUtil.tryGetJsonLong(jsonObj, "message.request.data.company_id");
						userId = StatsUtil.tryGetJsonLong(jsonObj, "message.request.data.user_id");
						sessionId = null;
					}
				} else {
					companyId = StatsUtil.tryGetJsonLong(jsonObj, "message.head.data.company_id");
					userId = null;
					sessionId = null;
				}
			} else {
				continue;
			}
			
			String result;
			JsonObject responseObj = StatsUtil.tryGetJsonObject(jsonObj, "message.response");
			if (responseObj == null) {
				result = "INTERNAL_EXCEPTION";
			} else {
				result = StatsUtil.tryGetJsonString(responseObj, "data.result");
			}
			
			if (timestamp != null && duration != null && server != null && service != null && function != null) {
				if (sqlBuilder == null) {
					sqlBuilder = new StringBuilder("INSERT INTO weizhu_stats_fact_user_action (log_time, log_date, duration, server, service, function, company_id, user_id, session_id, result) VALUES ");
				} else {
					sqlBuilder.append(", ");
				}
				
				sqlBuilder.append("(");
				sqlBuilder.append(timeFormat.format(new Date(timestamp))).append(", ");
				sqlBuilder.append(dateFormat.format(new Date(timestamp))).append(", ");
				sqlBuilder.append(duration).append(", '");
				sqlBuilder.append(DBUtil.SQL_STRING_ESCAPER.escape(StatsUtil.trimToSize(server, 50))).append("', '");
				sqlBuilder.append(DBUtil.SQL_STRING_ESCAPER.escape(StatsUtil.trimToSize(service, 50))).append("', '");
				sqlBuilder.append(DBUtil.SQL_STRING_ESCAPER.escape(StatsUtil.trimToSize(function, 50))).append("', ");
				sqlBuilder.append(companyId == null ? "NULL" : companyId).append(", ");
				sqlBuilder.append(userId == null ? "NULL" : StatsUtil.toStatsUserId(companyId, userId)).append(", ");
				sqlBuilder.append(sessionId == null ? "NULL" : sessionId).append(", ");
				sqlBuilder.append(result == null ? "NULL" : "'" + DBUtil.SQL_STRING_ESCAPER.escape(StatsUtil.trimToSize(result, 50)) + "'").append(")");
			}
		}

		return sqlBuilder == null ? null : sqlBuilder.append("; ").toString();
	}

}
