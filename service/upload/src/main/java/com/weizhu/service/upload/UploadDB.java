package com.weizhu.service.upload;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;

import javax.annotation.Nullable;

import com.google.common.collect.Iterables;
import com.weizhu.common.db.DBUtil;
import com.weizhu.common.db.ProtobufMapper;
import com.weizhu.common.utils.DataPage;
import com.weizhu.proto.UploadProtos;

public class UploadDB {

	private static final ProtobufMapper<UploadProtos.Image> IMAGE_MAPPER = 
			ProtobufMapper.createMapper(UploadProtos.Image.getDefaultInstance(), 
					"name", 
					"type",
					"size",
					"md5",
					"width",
					"hight"
					);

	public static Map<String, UploadProtos.Image> getImage(Connection conn, Collection<String> imageNames) throws SQLException {
		if (imageNames.isEmpty()) {
			return Collections.emptyMap();
		}
		
		StringBuilder sqlBuilder = new StringBuilder();
		sqlBuilder.append("SELECT * FROM weizhu_upload_image WHERE name IN ('");
		DBUtil.QUOTE_COMMA_JOINER.appendTo(sqlBuilder, Iterables.transform(imageNames, DBUtil.SQL_STRING_ESCAPER.asFunction()));
		sqlBuilder.append("'); ");
		
		final String sql = sqlBuilder.toString();
		
		Statement stmt = null;
		ResultSet rs = null;
		try {
			stmt = conn.createStatement();
			rs = stmt.executeQuery(sql);
			
			Map<String, UploadProtos.Image> resultMap = new TreeMap<String, UploadProtos.Image>();
			UploadProtos.Image.Builder tmpBuilder = UploadProtos.Image.newBuilder();
			while (rs.next()) {
				tmpBuilder.clear();
				
				UploadProtos.Image image = IMAGE_MAPPER.mapToItem(rs, tmpBuilder).build();
				resultMap.put(image.getName(), image);
			}
			
			return resultMap;
		} finally {
			DBUtil.closeQuietly(rs);
			DBUtil.closeQuietly(stmt);
		}
	}
	
	public static void replaceImage(Connection conn, List<UploadProtos.Image> imageList) throws SQLException {
		if (imageList.isEmpty()) {
			return;
		}
		
		StringBuilder sqlBuilder = new StringBuilder();
		sqlBuilder.append("REPLACE INTO weizhu_upload_image (name, `type`, size, md5, width, hight) VALUES ");
		
		boolean isFirst = true;
		for (UploadProtos.Image image : imageList) {
			if (isFirst) {
				isFirst = false;
			} else {
				sqlBuilder.append(", ");
			}
			
			sqlBuilder.append("('");
			sqlBuilder.append(DBUtil.SQL_STRING_ESCAPER.escape(image.getName())).append("', '");
			sqlBuilder.append(DBUtil.SQL_STRING_ESCAPER.escape(image.getType())).append("', ");
			sqlBuilder.append(image.getSize()).append(", '");
			sqlBuilder.append(DBUtil.SQL_STRING_ESCAPER.escape(image.getMd5())).append("', ");
			sqlBuilder.append(image.getWidth()).append(", ");
			sqlBuilder.append(image.getHight()).append(")");
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
	
	public static Map<String, List<String>> getImageTag(Connection conn, long companyId, Collection<String> imageNames) throws SQLException {
		if (imageNames.isEmpty()) {
			return Collections.emptyMap();
		}
		
		StringBuilder sqlBuilder = new StringBuilder();
		sqlBuilder.append("SELECT image_name, tag FROM weizhu_upload_image_tag WHERE company_id = ").append(companyId);
		sqlBuilder.append(" AND image_name IN ('");
		DBUtil.QUOTE_COMMA_JOINER.appendTo(sqlBuilder, Iterables.transform(imageNames, DBUtil.SQL_STRING_ESCAPER.asFunction()));
		sqlBuilder.append("') ORDER BY image_name ASC, tag ASC; ");
		
		final String sql = sqlBuilder.toString();
		
		Statement stmt = null;
		ResultSet rs = null;
		try {
			stmt = conn.createStatement();
			rs = stmt.executeQuery(sql);
			
			Map<String, List<String>> resultMap = new TreeMap<String, List<String>>();
			while (rs.next()) {
				String imageName = rs.getString("image_name");
				String tag = rs.getString("tag");
				
				List<String> list = resultMap.get(imageName);
				if (list == null) {
					list = new ArrayList<String>();
					resultMap.put(imageName, list);
				}
				list.add(tag);
			}
			
			return resultMap;
		} finally {
			DBUtil.closeQuietly(rs);
			DBUtil.closeQuietly(stmt);
		}
	}
	
	public static void insertImageTag(Connection conn, long companyId, Map<String, List<String>> imageTagListMap) throws SQLException {
		if (imageTagListMap.isEmpty()) {
			return;
		}
		
		StringBuilder sqlBuilder = new StringBuilder();
		sqlBuilder.append("INSERT IGNORE INTO weizhu_upload_image_tag (company_id, image_name, tag) VALUES ");
		
		boolean isFirst = true;
		for (Entry<String, List<String>> entry : imageTagListMap.entrySet()) {
			final String imageName = entry.getKey();
			for (String tag : entry.getValue()) {
				if (isFirst) {
					isFirst = false;
				} else {
					sqlBuilder.append(", ");
				}
				
				sqlBuilder.append("(");
				sqlBuilder.append(companyId).append(", '");
				sqlBuilder.append(DBUtil.SQL_STRING_ESCAPER.escape(imageName)).append("', '");
				sqlBuilder.append(DBUtil.SQL_STRING_ESCAPER.escape(tag)).append("')");
			}
		}

		if (!isFirst) {
			final String sql = sqlBuilder.append("; ").toString();
			Statement stmt = null;
			try {
				stmt = conn.createStatement();
				stmt.executeUpdate(sql);
			} finally {
				DBUtil.closeQuietly(stmt);
			}
		}
	}
	
	public static void updateImageTag(Connection conn, long companyId, 
			Map<String, List<String>> oldImageTagListMap, 
			Map<String, List<String>> newImageTagListMap
			) throws SQLException {
		
		// 必须保证key完全一样
		if (!oldImageTagListMap.keySet().equals(newImageTagListMap.keySet())) {
			throw new IllegalArgumentException("update key not equals");
		}
		
		if (newImageTagListMap.isEmpty()) {
			return;
		}
		
		StringBuilder sqlBuilder = new StringBuilder();
		
		// insert
		boolean isInsertFirst = true;
		for (Entry<String, List<String>> entry : newImageTagListMap.entrySet()) {
			final String imageName = entry.getKey();
			final List<String> oldTagList = oldImageTagListMap.get(imageName);
			
			for (String newTag : entry.getValue()) {
				if (!oldTagList.contains(newTag)) {
					if (isInsertFirst) {
						sqlBuilder.append("REPLACE INTO weizhu_upload_image_tag (company_id, image_name, tag) VALUES ");
						isInsertFirst = false;
					} else {
						sqlBuilder.append(", ");
					}
					
					sqlBuilder.append("(");
					sqlBuilder.append(companyId).append(", '");
					sqlBuilder.append(DBUtil.SQL_STRING_ESCAPER.escape(imageName)).append("', '");
					sqlBuilder.append(DBUtil.SQL_STRING_ESCAPER.escape(newTag)).append("')");
				}
			}
		}
		if (!isInsertFirst) {
			sqlBuilder.append("; ");
		}
			
		// delete
		boolean isDeleteFirst = true;
		for (Entry<String, List<String>> entry : oldImageTagListMap.entrySet()) {
			final String imageName = entry.getKey();
			final List<String> newTagList = newImageTagListMap.get(imageName);
			
			for (String oldTag : entry.getValue()) {
				if (!newTagList.contains(oldTag)) {
					if (isDeleteFirst) {
						sqlBuilder.append("DELETE FROM weizhu_upload_image_tag WHERE company_id = ");
						sqlBuilder.append(companyId).append(" AND (image_name, tag) IN (");
						isDeleteFirst = false;
					} else {
						sqlBuilder.append(", ");
					}
					
					sqlBuilder.append("('");
					sqlBuilder.append(DBUtil.SQL_STRING_ESCAPER.escape(imageName)).append("', '");
					sqlBuilder.append(DBUtil.SQL_STRING_ESCAPER.escape(oldTag)).append("')");
				}
			}
		}
		if (!isDeleteFirst) {
			sqlBuilder.append("); ");
		}
		
		if (!isInsertFirst || !isDeleteFirst) {
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
	
	public static Map<String, Integer> getImageCountByTag(Connection conn, long companyId) throws SQLException {
		StringBuilder sqlBuilder = new StringBuilder();
		
		sqlBuilder.append("SELECT tag, COUNT(image_name) AS cnt FROM weizhu_upload_image_tag WHERE company_id = ");
		sqlBuilder.append(companyId).append(" AND image_name IN (SELECT DISTINCT image_name FROM weizhu_upload_image_tag WHERE company_id = ");
		sqlBuilder.append(companyId).append(" ) GROUP BY tag ORDER BY cnt DESC, tag ASC; ");
		
		final String sql = sqlBuilder.toString();
		
		Statement stmt = null;
		ResultSet rs = null;
		try {
			stmt = conn.createStatement();
			rs = stmt.executeQuery(sql);
			
			Map<String, Integer> resultMap = new LinkedHashMap<String, Integer>();
			while (rs.next()) {
				String tag = rs.getString("tag");
				int cnt = rs.getInt("cnt");
				resultMap.put(tag, cnt);
			}
			
			return resultMap;
		} finally {
			DBUtil.closeQuietly(rs);
			DBUtil.closeQuietly(stmt);
		}
	}
	
	public static Map<String, Integer> getImageCountByTag(Connection conn, long companyId, Set<String> selectedImageTagSet) throws SQLException {
		if (selectedImageTagSet.isEmpty()) {
			return getImageCountByTag(conn, companyId);
		}
		
		final String tagStr = DBUtil.QUOTE_COMMA_JOINER.join(Iterables.transform(selectedImageTagSet, DBUtil.SQL_STRING_ESCAPER.asFunction()));
		
		StringBuilder sqlBuilder = new StringBuilder();
		
		sqlBuilder.append("SELECT tag, COUNT(image_name) AS cnt FROM weizhu_upload_image_tag WHERE company_id = ");
		sqlBuilder.append(companyId).append(" AND tag NOT IN ('");
		sqlBuilder.append(tagStr).append("') AND image_name IN (SELECT DISTINCT image_name FROM weizhu_upload_image_tag WHERE company_id = ");
		sqlBuilder.append(companyId).append(" AND tag IN ('");
		sqlBuilder.append(tagStr).append("') ) GROUP BY tag ORDER BY cnt DESC, tag ASC; ");
		
		final String sql = sqlBuilder.toString();
		
		Statement stmt = null;
		ResultSet rs = null;
		try {
			stmt = conn.createStatement();
			rs = stmt.executeQuery(sql);
			
			Map<String, Integer> resultMap = new LinkedHashMap<String, Integer>();
			while (rs.next()) {
				String tag = rs.getString("tag");
				int cnt = rs.getInt("cnt");
				resultMap.put(tag, cnt);
			}
			
			return resultMap;
		} finally {
			DBUtil.closeQuietly(rs);
			DBUtil.closeQuietly(stmt);
		}
	}
	
	public static DataPage<String> getImageNamePageByTag(Connection conn, long companyId, int start, int length) throws SQLException {
		if (start < 0) {
			start = 0;
		}
		if (length < 0) {
			length = 0;
		}
		
		StringBuilder sqlBuilder = new StringBuilder();
		sqlBuilder.append("SELECT DISTINCT image_name FROM weizhu_upload_image_tag WHERE company_id = ");
		sqlBuilder.append(companyId).append(" ORDER BY image_name ASC ");
		sqlBuilder.append(start).append(", ").append(length).append("; ");
		sqlBuilder.append("SELECT COUNT(DISTINCT image_name) AS total_size FROM weizhu_upload_image_tag WHERE company_id = ");
		sqlBuilder.append(companyId).append("; ");
		
		final String sql = sqlBuilder.toString();
		
		Statement stmt = null;
		ResultSet rs = null;
		try {
			stmt = conn.createStatement();
			stmt.execute(sql);
			rs = stmt.getResultSet();
			
			List<String> list = new ArrayList<String>();
			while (rs.next()) {
				list.add(rs.getString("image_name"));
			}
			
			DBUtil.closeQuietly(rs);
			rs = null;
			
			stmt.getMoreResults();
			rs = stmt.getResultSet();
			
			if (!rs.next()) {
				throw new RuntimeException("cannot get total size");
			}
			
			int totalSize = rs.getInt("total_size");
			
			return new DataPage<String>(list, totalSize, totalSize);
		} finally {
			DBUtil.closeQuietly(rs);
			DBUtil.closeQuietly(stmt);
		}
	}
	
	public static DataPage<String> getImageNamePageByTag(Connection conn, long companyId, Set<String> selectedImageTagSet, int start, int length) throws SQLException {
		if (selectedImageTagSet.isEmpty()) {
			return getImageNamePageByTag(conn, companyId, start, length);
		}
		
		if (start < 0) {
			start = 0;
		}
		if (length < 0) {
			length = 0;
		}
		
		final String tagStr = DBUtil.QUOTE_COMMA_JOINER.join(Iterables.transform(selectedImageTagSet, DBUtil.SQL_STRING_ESCAPER.asFunction()));
		
		StringBuilder sqlBuilder = new StringBuilder();
		sqlBuilder.append("SELECT DISTINCT image_name FROM weizhu_upload_image_tag WHERE company_id = ");
		sqlBuilder.append(companyId).append(" AND tag IN ('");
		sqlBuilder.append(tagStr).append("') ORDER BY image_name ASC ");
		sqlBuilder.append(start).append(", ").append(length).append("; ");
		sqlBuilder.append("SELECT COUNT(DISTINCT image_name) AS filtered_size FROM weizhu_upload_image_tag WHERE company_id = ");
		sqlBuilder.append(companyId).append(" AND tag IN ('");
		sqlBuilder.append(tagStr).append("'); ");
		sqlBuilder.append("SELECT COUNT(DISTINCT image_name) AS total_size FROM weizhu_upload_image_tag WHERE company_id = ");
		sqlBuilder.append(companyId).append("; ");
		
		final String sql = sqlBuilder.toString();
		
		Statement stmt = null;
		ResultSet rs = null;
		try {
			stmt = conn.createStatement();
			stmt.execute(sql);
			rs = stmt.getResultSet();
			
			List<String> list = new ArrayList<String>();
			while (rs.next()) {
				list.add(rs.getString("image_name"));
			}
			
			DBUtil.closeQuietly(rs);
			rs = null;
			
			stmt.getMoreResults();
			rs = stmt.getResultSet();
			
			if (!rs.next()) {
				throw new RuntimeException("cannot get filtered size");
			}
			
			int filteredSize = rs.getInt("filtered_size");
			
			DBUtil.closeQuietly(rs);
			rs = null;
			
			stmt.getMoreResults();
			rs = stmt.getResultSet();
			
			if (!rs.next()) {
				throw new RuntimeException("cannot get total size");
			}
			
			int totalSize = rs.getInt("total_size");
			
			return new DataPage<String>(list, totalSize, filteredSize);
		} finally {
			DBUtil.closeQuietly(rs);
			DBUtil.closeQuietly(stmt);
		}
	}
	
	private static final ProtobufMapper<UploadProtos.UploadImageAction> UPLOAD_IMAGE_ACTION_MAPPER = 
			ProtobufMapper.createMapper(UploadProtos.UploadImageAction.getDefaultInstance(), 
					"action_id", 
					"image_name",
					"upload_time",
					"upload_admin_id",
					"upload_user_id"
					);
	
	public static DataPage<UploadProtos.UploadImageAction> getUploadImageActionPage(Connection conn, long companyId, int start, int length) throws SQLException {
		if (start < 0) {
			start = 0;
		}
		if (length < 0) {
			length = 0;
		}

		StringBuilder sqlBuilder = new StringBuilder();
		sqlBuilder.append("SELECT * FROM weizhu_upload_image_action WHERE company_id = ");
		sqlBuilder.append(companyId).append(" ORDER BY action_id DESC LIMIT ").append(start).append(", ").append(length).append("; ");
		
		sqlBuilder.append("SELECT COUNT(*) AS total_size FROM weizhu_upload_image_action WHERE company_id = ");
		sqlBuilder.append(companyId).append("; ");
		
		final String sql = sqlBuilder.toString();
		
		Statement stmt = null;
		ResultSet rs = null;
		try {
			stmt = conn.createStatement();
			stmt.execute(sql);
			rs = stmt.getResultSet();
			
			List<UploadProtos.UploadImageAction> list = new ArrayList<UploadProtos.UploadImageAction>();
			UploadProtos.UploadImageAction.Builder tmpBuilder = UploadProtos.UploadImageAction.newBuilder();
			while (rs.next()) {
				tmpBuilder.clear();
				
				list.add(UPLOAD_IMAGE_ACTION_MAPPER.mapToItem(rs, tmpBuilder).build());
			}
			
			DBUtil.closeQuietly(rs);
			rs = null;
			
			stmt.getMoreResults();
			rs = stmt.getResultSet();
			
			if (!rs.next()) {
				throw new RuntimeException("cannot get total size");
			}
			
			int totalSize = rs.getInt("total_size");
			
			return new DataPage<UploadProtos.UploadImageAction>(list, totalSize, totalSize);
		} finally {
			DBUtil.closeQuietly(rs);
			DBUtil.closeQuietly(stmt);
		}
	}
	
	public static DataPage<UploadProtos.UploadImageAction> getUploadImageActionPage(Connection conn, long companyId, 
			@Nullable Long uploadAdminId, @Nullable Long uploadUserId, int start, int length) throws SQLException {
		if (uploadAdminId == null && uploadUserId == null) {
			return getUploadImageActionPage(conn, companyId, start, length);
		}
		
		if (start < 0) {
			start = 0;
		}
		if (length < 0) {
			length = 0;
		}

		StringBuilder sqlBuilder = new StringBuilder();
		sqlBuilder.append("SELECT * FROM weizhu_upload_image_action WHERE company_id = ");
		sqlBuilder.append(companyId);
		if (uploadAdminId != null) {
			sqlBuilder.append(" AND upload_admin_id = ").append(uploadAdminId);
		}
		if (uploadUserId != null) {
			sqlBuilder.append(" AND upload_user_id = ").append(uploadUserId);
		}
		sqlBuilder.append(" ORDER BY action_id DESC LIMIT ").append(start).append(", ").append(length).append("; ");
		
		sqlBuilder.append("SELECT COUNT(*) AS filtered_size FROM weizhu_upload_image_action WHERE company_id = ");
		sqlBuilder.append(companyId);
		if (uploadAdminId != null) {
			sqlBuilder.append(" AND upload_admin_id = ").append(uploadAdminId);
		}
		if (uploadUserId != null) {
			sqlBuilder.append(" AND upload_user_id = ").append(uploadUserId);
		}
		sqlBuilder.append("; ");
		
		sqlBuilder.append("SELECT COUNT(*) AS total_size FROM weizhu_upload_image_action WHERE company_id = ");
		sqlBuilder.append(companyId).append("; ");
		
		final String sql = sqlBuilder.toString();
		
		Statement stmt = null;
		ResultSet rs = null;
		try {
			stmt = conn.createStatement();
			stmt.execute(sql);
			rs = stmt.getResultSet();
			
			List<UploadProtos.UploadImageAction> list = new ArrayList<UploadProtos.UploadImageAction>();
			UploadProtos.UploadImageAction.Builder tmpBuilder = UploadProtos.UploadImageAction.newBuilder();
			while (rs.next()) {
				tmpBuilder.clear();
				
				list.add(UPLOAD_IMAGE_ACTION_MAPPER.mapToItem(rs, tmpBuilder).build());
			}
			
			DBUtil.closeQuietly(rs);
			rs = null;
			
			stmt.getMoreResults();
			rs = stmt.getResultSet();
			
			if (!rs.next()) {
				throw new RuntimeException("cannot get filtered size");
			}
			
			int filteredSize = rs.getInt("filtered_size");
			
			DBUtil.closeQuietly(rs);
			rs = null;
			
			stmt.getMoreResults();
			rs = stmt.getResultSet();
			
			if (!rs.next()) {
				throw new RuntimeException("cannot get total size");
			}
			
			int totalSize = rs.getInt("total_size");
			
			return new DataPage<UploadProtos.UploadImageAction>(list, totalSize, filteredSize);
		} finally {
			DBUtil.closeQuietly(rs);
			DBUtil.closeQuietly(stmt);
		}
	}
	
	public static List<Long> insertUploadImageAction(Connection conn, long companyId, List<UploadProtos.UploadImageAction> actionList) throws SQLException {
		if (actionList.isEmpty()) {
			return Collections.emptyList();
		}
		
		StringBuilder sqlBuilder = new StringBuilder();
		sqlBuilder.append("INSERT INTO weizhu_upload_image_action (company_id, action_id, image_name, upload_time, upload_admin_id, upload_user_id) VALUES ");
		
		boolean isFirst = true;
		for (UploadProtos.UploadImageAction action : actionList) {
			if (isFirst) {
				isFirst = false;
			} else {
				sqlBuilder.append(", ");
			}
			
			sqlBuilder.append("(");
			sqlBuilder.append(companyId).append(", NULL, '");
			sqlBuilder.append(DBUtil.SQL_STRING_ESCAPER.escape(action.getImageName())).append("', ");
			sqlBuilder.append(action.getUploadTime()).append(", ");
			sqlBuilder.append(action.hasUploadAdminId() ? action.getUploadAdminId() : "NULL").append(", ");
			sqlBuilder.append(action.hasUploadUserId() ? action.getUploadUserId() : "NULL").append(")");
		}
		
		final String sql = sqlBuilder.toString();
		
		Statement stmt = null;
		ResultSet rs = null;
		try {
			stmt = conn.createStatement();
			stmt.executeUpdate(sql, Statement.RETURN_GENERATED_KEYS);
			
			rs = stmt.getGeneratedKeys();
			
			List<Long> actionIdList = new ArrayList<Long>(actionList.size());
			while (rs.next()) {
				actionIdList.add(rs.getLong(1));
			}
			return actionIdList;
		} finally {
			DBUtil.closeQuietly(rs);
			DBUtil.closeQuietly(stmt);
		}
	}
}
