package com.weizhu.service.stats.fact;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import javax.inject.Singleton;

import com.google.common.collect.ImmutableList;
import com.google.gson.JsonObject;
import com.google.inject.Inject;
import com.weizhu.common.db.DBUtil;
import com.weizhu.service.stats.StatsUtil;

@Singleton
public class InsertFactUserLoginHandler implements InsertFactHandler {

	@Inject
	public InsertFactUserLoginHandler() {
	}
	
	@Override
	public String kafkaGroupId() {
		return "stats_fact_user_login_0";
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
			if (!"LoginService".equals(StatsUtil.tryGetJsonString(jsonObj, "message.service"))) {
				continue;
			}
			
			Long timestamp = StatsUtil.tryGetJsonLong(jsonObj, "timestamp");
			
			Long companyId;
			Long userId;
			Long sessionId;
			String companyKey;
			String mobileNo;

			String function = StatsUtil.tryGetJsonString(jsonObj, "message.function");
			if ("sendSmsCode".equals(function)) {
				companyId = StatsUtil.tryGetJsonLong(jsonObj, "message.response.data.company_id");
				userId = null;
				sessionId = null;
				companyKey = StatsUtil.tryGetJsonString(jsonObj, "message.request.data.company_key");
				mobileNo = StatsUtil.tryGetJsonString(jsonObj, "message.request.data.mobile_no");
			} else if ("loginBySmsCode".equals(function)) {
				companyId = StatsUtil.tryGetJsonLong(jsonObj, "message.response.data.company_id");
				userId = StatsUtil.tryGetJsonLong(jsonObj, "message.response.data.user.base.user_id");
				sessionId = null;
				companyKey = StatsUtil.tryGetJsonString(jsonObj, "message.request.data.company_key");
				mobileNo = StatsUtil.tryGetJsonString(jsonObj, "message.request.data.mobile_no");
			} else if ("loginAuto".equals(function)) {
				companyId = StatsUtil.tryGetJsonLong(jsonObj, "message.response.data.company_id");
				userId = StatsUtil.tryGetJsonLong(jsonObj, "message.response.data.user.base.user_id");
				sessionId = null;
				companyKey = StatsUtil.tryGetJsonString(jsonObj, "message.request.data.company_key");
				mobileNo = StatsUtil.tryGetJsonString(jsonObj, "message.request.data.mobile_no");
			} else if ("logout".equals(function)) {
				companyId = StatsUtil.tryGetJsonLong(jsonObj, "message.head.data.session.company_id");
				userId = StatsUtil.tryGetJsonLong(jsonObj, "message.head.data.session.user_id");
				sessionId = StatsUtil.tryGetJsonLong(jsonObj, "message.head.data.session.session_id");
				companyKey = null;
				mobileNo = null;
			} else if ("sendRegisterSmsCode".equals(function)) {
				companyId = StatsUtil.tryGetJsonLong(jsonObj, "message.response.data.company_id");
				userId = null;
				sessionId = null;
				companyKey = StatsUtil.tryGetJsonString(jsonObj, "message.request.data.company_key");
				mobileNo = StatsUtil.tryGetJsonString(jsonObj, "message.request.data.mobile_no");
			} else if ("registerBySmsCode".equals(function)) {
				companyId = StatsUtil.tryGetJsonLong(jsonObj, "message.response.data.company_id");
				userId = null;
				sessionId = null;
				companyKey = StatsUtil.tryGetJsonString(jsonObj, "message.request.data.company_key");
				mobileNo = StatsUtil.tryGetJsonString(jsonObj, "message.request.data.mobile_no");
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
			
			if (timestamp != null && function != null) {
				if (sqlBuilder == null) {
					sqlBuilder = new StringBuilder("INSERT INTO weizhu_stats_fact_user_login (log_time, log_date, company_id, user_id, session_id, function, company_key, mobile_no, result) VALUES ");
				} else {
					sqlBuilder.append(", ");
				}
				
				sqlBuilder.append("(");
				sqlBuilder.append(timeFormat.format(new Date(timestamp))).append(", ");
				sqlBuilder.append(dateFormat.format(new Date(timestamp))).append(", ");
				sqlBuilder.append(companyId == null ? "NULL" : companyId).append(", ");
				sqlBuilder.append(userId == null ? "NULL" : StatsUtil.toStatsUserId(companyId, userId)).append(", ");
				sqlBuilder.append(sessionId == null ? "NULL" : sessionId).append(", '");
				sqlBuilder.append(DBUtil.SQL_STRING_ESCAPER.escape(StatsUtil.trimToSize(function, 50))).append("', ");
				sqlBuilder.append(companyKey == null ? "NULL" : "'" + DBUtil.SQL_STRING_ESCAPER.escape(StatsUtil.trimToSize(companyKey, 30)) + "'").append(", ");
				sqlBuilder.append(mobileNo == null ? "NULL" : "'" + DBUtil.SQL_STRING_ESCAPER.escape(StatsUtil.trimToSize(mobileNo, 30)) + "'").append(", ");
				sqlBuilder.append(result == null ? "NULL" : "'" + DBUtil.SQL_STRING_ESCAPER.escape(StatsUtil.trimToSize(result, 50)) + "'").append(")");
			}
		}

		return sqlBuilder == null ? null : sqlBuilder.append("; ").toString();
	}

}
