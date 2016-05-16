package com.weizhu.service.stats;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.weizhu.common.db.DBUtil;
import com.weizhu.proto.CompanyProtos;

public class StatsUtil {

	private static final long COMPANY_ID_OFFSET = 1000000000000L;
	
	public static long toStatsUserId(long companyId, long userId) {
		if (userId < 0) {
			return companyId * COMPANY_ID_OFFSET + userId + COMPANY_ID_OFFSET;
		} else {
			return companyId * COMPANY_ID_OFFSET + userId;
		}
	}
	
	public static long toStatsLevelId(long companyId, int levelId) {
		if (levelId < 0) {
			return companyId * COMPANY_ID_OFFSET + levelId + COMPANY_ID_OFFSET;
		} else {
			return companyId * COMPANY_ID_OFFSET + levelId;
		}
	}
	
	public static long toStatsTeamId(long companyId, int teamId) {
		if (teamId < 0) {
			return companyId * COMPANY_ID_OFFSET + teamId + COMPANY_ID_OFFSET;
		} else {
			return companyId * COMPANY_ID_OFFSET + teamId;
		}
	}
	
	public static long toStatsPositionId(long companyId, int positionId) {
		if (positionId < 0) {
			return companyId * COMPANY_ID_OFFSET + positionId + COMPANY_ID_OFFSET;
		} else {
			return companyId * COMPANY_ID_OFFSET + positionId;
		}
	}
	
	public static long toStatsAdminId(long companyId, long adminId) {
		if (adminId < 0) {
			return companyId * COMPANY_ID_OFFSET + adminId + COMPANY_ID_OFFSET;
		} else {
			return companyId * COMPANY_ID_OFFSET + adminId;
		}
	}
	
	public static long toStatsDiscoverItemId(long companyId, long itemId) {
		if (itemId < 0) {
			return companyId * COMPANY_ID_OFFSET + itemId + COMPANY_ID_OFFSET;
		} else {
			return companyId * COMPANY_ID_OFFSET + itemId;
		}
	}
	
	public static long toStatsDiscoverModuleId(long companyId, int moduleId) {
		if (moduleId < 0) {
			return companyId * COMPANY_ID_OFFSET + moduleId + COMPANY_ID_OFFSET;
		} else {
			return companyId * COMPANY_ID_OFFSET + moduleId;
		}
	}
	
	public static long toStatsDiscoverCategoryId(long companyId, int categoryId) {
		if (categoryId < 0) {
			return companyId * COMPANY_ID_OFFSET + categoryId + COMPANY_ID_OFFSET;
		} else {
			return companyId * COMPANY_ID_OFFSET + categoryId;
		}
	}
	
	public static void replaceCompany(Connection dbConn, List<CompanyProtos.Company> companyList) throws SQLException {
		StringBuilder sqlBuilder = new StringBuilder();
		sqlBuilder.append("REPLACE INTO weizhu_stats_dim_company (company_id, company_name) VALUES ");
		
		boolean isFirst = true;
		for (CompanyProtos.Company company : companyList) {
			if (isFirst) {
				isFirst = false;
			} else {
				sqlBuilder.append(", ");
			}
			sqlBuilder.append("(");
			sqlBuilder.append(company.getCompanyId()).append(", '");
			sqlBuilder.append(DBUtil.SQL_STRING_ESCAPER.escape(StatsUtil.trimToSize(company.getCompanyName(), 50))).append("')");
		}
		sqlBuilder.append("; ");
		
		final String sql = sqlBuilder.toString();
		Statement stmt = dbConn.createStatement();
		try {
			stmt.executeUpdate(sql);
		} finally {
			DBUtil.closeQuietly(stmt);
		}
	}
	
	public static JsonObject tryGetJsonObject(JsonObject obj, String field) {
		int idx = field.indexOf('.');
		if (idx < 0) {
			JsonElement e = obj.get(field);
			if (e != null && e.isJsonObject()) {
				return e.getAsJsonObject();
			} else {
				return null;
			}
		} else {
			JsonElement e = obj.get(field.substring(0, idx));
			if (e != null && e.isJsonObject()) {
				return tryGetJsonObject(e.getAsJsonObject(), field.substring(idx + 1));
			} else {
				return null;
			}
		}
	}
	
	public static String tryGetJsonString(JsonObject obj, String field) {
		int idx = field.indexOf('.');
		if (idx < 0) {
			JsonElement e = obj.get(field);
			if (e != null && e.isJsonPrimitive()) {
				return e.getAsString();
			} else {
				return null;
			}
		} else {
			JsonElement e = obj.get(field.substring(0, idx));
			if (e != null && e.isJsonObject()) {
				return tryGetJsonString(e.getAsJsonObject(), field.substring(idx + 1));
			} else {
				return null;
			}
		}
	}
	
	public static Long tryGetJsonLong(JsonObject obj, String field) {
		int idx = field.indexOf('.');
		if (idx < 0) {
			JsonElement e = obj.get(field);
			if (e != null && e.isJsonPrimitive()) {
				try {
					return e.getAsLong();
				} catch (NumberFormatException nfe) {
					return null;
				}
			} else {
				return null;
			}
		} else {
			JsonElement e = obj.get(field.substring(0, idx));
			if (e != null && e.isJsonObject()) {
				return tryGetJsonLong(e.getAsJsonObject(), field.substring(idx + 1));
			} else {
				return null;
			}
		}
	}
	
	public static Integer tryGetJsonInt(JsonObject obj, String field) {
		int idx = field.indexOf('.');
		if (idx < 0) {
			JsonElement e = obj.get(field);
			if (e != null && e.isJsonPrimitive()) {
				try {
					return e.getAsInt();
				} catch (NumberFormatException nfe) {
					return null;
				}
			} else {
				return null;
			}
		} else {
			JsonElement e = obj.get(field.substring(0, idx));
			if (e != null && e.isJsonObject()) {
				return tryGetJsonInt(e.getAsJsonObject(), field.substring(idx + 1));
			} else {
				return null;
			}
		}
	}
	
	public static String trimToSize(String str, int size) {
		if (str == null) {
			return null;
		}
		return str.length() > size ? str.substring(0, size) : str;
	}
}
