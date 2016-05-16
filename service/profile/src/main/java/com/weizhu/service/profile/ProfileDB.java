package com.weizhu.service.profile;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.TreeMap;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;
import com.weizhu.common.db.DBUtil;
import com.weizhu.common.db.ProtobufMapper;
import com.weizhu.proto.ProfileProtos;

public class ProfileDB {
	
	private static final ProtobufMapper<ProfileProtos.Profile> PROFILE_MAPPER = 
			ProtobufMapper.createMapper(ProfileProtos.Profile.getDefaultInstance(), "name", "value" );

	public static ImmutableMap<Long, ImmutableList<ProfileProtos.Profile>> getAllProfileValue(Connection conn) throws SQLException {
		Statement stmt = null;
		ResultSet rs = null;
		try {
			stmt = conn.createStatement();
			rs = stmt.executeQuery("SELECT * FROM weizhu_profile_value ORDER BY company_id, `name`; ");
			
			Map<Long, ImmutableList.Builder<ProfileProtos.Profile>> map = new TreeMap<Long, ImmutableList.Builder<ProfileProtos.Profile>>();
			
			ProfileProtos.Profile.Builder tmpBuilder = ProfileProtos.Profile.newBuilder();
			while (rs.next()) {
				final long companyId = rs.getLong("company_id");
				final ProfileProtos.Profile profile = PROFILE_MAPPER.mapToItem(rs, tmpBuilder).build();
				
				ImmutableList.Builder<ProfileProtos.Profile> listBuilder = map.get(companyId);
				if (listBuilder == null) {
					listBuilder = ImmutableList.builder();
					map.put(companyId, listBuilder);
				}
				listBuilder.add(profile);
			}
			
			Map<Long, ImmutableList<ProfileProtos.Profile>> resultMap = Maps.newTreeMap();
			for (Entry<Long, ImmutableList.Builder<ProfileProtos.Profile>> entry : map.entrySet()) {
				resultMap.put(entry.getKey(), entry.getValue().build());
			}
			return ImmutableMap.copyOf(resultMap);
		} finally {
			DBUtil.closeQuietly(rs);
			DBUtil.closeQuietly(stmt);
		}
	}
	
	public static void replaceProfileValue(Connection conn, long companyId, List<ProfileProtos.Profile> profileList) throws SQLException {
		if (profileList.isEmpty()) {
			return;
		}
		
		StringBuilder sqlBuilder = new StringBuilder();
		sqlBuilder.append("REPLACE INTO weizhu_profile_value (company_id, `name`, `value`) VALUES ");
		
		boolean isFirst = true;
		for (ProfileProtos.Profile profile : profileList) {
			if (isFirst) {
				isFirst = false;
			} else {
				sqlBuilder.append(", ");
			}
			sqlBuilder.append("(");
			sqlBuilder.append(companyId).append(", '");
			sqlBuilder.append(DBUtil.SQL_STRING_ESCAPER.escape(profile.getName())).append("', '");
			sqlBuilder.append(DBUtil.SQL_STRING_ESCAPER.escape(profile.getValue())).append("')");
		}
		
		sqlBuilder.append("; ");
		
		final String sql = sqlBuilder.toString();
		Statement stmt = null;
		try {
			stmt = conn.createStatement();
			stmt.executeUpdate(sql);
		} finally {
			DBUtil.closeQuietly(stmt);
		}
	}
	
	public static Map<String, String> getProfileComment(Connection conn, long companyId, Set<String> nameSet) throws SQLException {
		if (nameSet.isEmpty()) {
			return Collections.emptyMap();
		}
		
		StringBuilder sqlBuilder = new StringBuilder();
		sqlBuilder.append("SELECT * FROM weizhu_profile_comment WHERE company_id = ").append(companyId).append(" AND `name` IN ('");
		DBUtil.QUOTE_COMMA_JOINER.appendTo(sqlBuilder, Iterables.transform(nameSet, DBUtil.SQL_STRING_ESCAPER.asFunction()));
		sqlBuilder.append("'); ");
		
		final String sql = sqlBuilder.toString();
		Statement stmt = null;
		ResultSet rs = null;
		try {
			stmt = conn.createStatement();
			rs = stmt.executeQuery(sql);
			Map<String, String> commentMap = new TreeMap<String, String>();
			while (rs.next()) {
				commentMap.put(rs.getString("name"), rs.getString("comment"));
			}
			return commentMap;
		} finally {
			DBUtil.closeQuietly(rs);
			DBUtil.closeQuietly(stmt);
		}
	}
	
	public static void replaceProfileComment(Connection conn, long companyId, Map<String, String> commentMap) throws SQLException {
		if (commentMap.isEmpty()) {
			return;
		}
		
		StringBuilder sqlBuilder = new StringBuilder();
		sqlBuilder.append("REPLACE INTO weizhu_profile_comment (company_id, `name`, `comment`) VALUES ");
		
		boolean isFirst = true;
		for (Entry<String, String> entry : commentMap.entrySet()) {
			if (isFirst) {
				isFirst = false;
			} else {
				sqlBuilder.append(", ");
			}
			sqlBuilder.append("(");
			sqlBuilder.append(companyId).append(", '");
			sqlBuilder.append(DBUtil.SQL_STRING_ESCAPER.escape(entry.getKey())).append("', '");
			sqlBuilder.append(DBUtil.SQL_STRING_ESCAPER.escape(entry.getValue())).append("')");
		}
		
		sqlBuilder.append("; ");
		
		final String sql = sqlBuilder.toString();
		Statement stmt = null;
		try {
			stmt = conn.createStatement();
			stmt.executeUpdate(sql);
		} finally {
			DBUtil.closeQuietly(stmt);
		}
	}
	
}
