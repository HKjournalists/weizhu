package com.weizhu.service.discover;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.annotation.Nullable;

import com.weizhu.common.db.DBUtil;
import com.weizhu.common.db.ProtobufMapper;
import com.weizhu.proto.DiscoverProtos;

public class DiscoverDB {
	
	private static final ProtobufMapper<DiscoverProtos.Banner> BANNER_MAPPER = 
			ProtobufMapper.createMapper(DiscoverProtos.Banner.getDefaultInstance(), 
					"banner_id", 
					"banner_name",
					"image_name",
					"item_id",
					"create_time");
	
	private static final ProtobufMapper<DiscoverProtos.Module.Category> CATEGORY_MAPPER = 
			ProtobufMapper.createMapper(DiscoverProtos.Module.Category.getDefaultInstance(), 
					"category_id",
					"category_name");
	
	private static final ProtobufMapper<DiscoverProtos.Module> MODULE_MAPPER = 
			ProtobufMapper.createMapper(DiscoverProtos.Module.getDefaultInstance(), 
					"module_id", 
					"module_name",
					"icon_name");
	
	private static final ProtobufMapper<DiscoverProtos.Item> ITEM_MAPPER = 
			ProtobufMapper.createMapper(DiscoverProtos.Item.getDefaultInstance(), 
					"item_id",
					"item_name",
					"icon_name",
					"create_time",
					"item_desc",
					"enable_score",
					"enable_comment");
	
	private static final ProtobufMapper<DiscoverProtos.ItemContent> ITEM_CONTENT_MAPPER = 
			ProtobufMapper.createMapper(DiscoverProtos.ItemContent.getDefaultInstance(), 
					"item.item_id",
					"item.item_name",
					"item.icon_name",
					"item.create_time",
					"item.item_desc",
					"item.enable_score",
					"item.enable_comment",
					"redirect_url");
	
	private static final String GET_DISCOVER_HOME_SQL = 
			"SELECT * FROM weizhu_discover_banner ORDER BY create_time DESC LIMIT 5; "
			+ "SELECT * FROM weizhu_discover_module_category ORDER BY module_id ASC, category_id ASC; "
			+ "SELECT * FROM weizhu_discover_module ORDER BY module_id ASC; ";

	public static DiscoverDAOProtos.DiscoverHome getDiscoverHome(Connection conn) throws SQLException {
		DiscoverDAOProtos.DiscoverHome.Builder homeBuilder = DiscoverDAOProtos.DiscoverHome.newBuilder();
		
		PreparedStatement pstmt = null;
		Statement stmt = null;
		ResultSet rs = null;
		try {
			pstmt = conn.prepareStatement(GET_DISCOVER_HOME_SQL);
			
			pstmt.execute();
			rs = pstmt.getResultSet();
			
			DiscoverProtos.Banner.Builder tmpBannerBuilder = DiscoverProtos.Banner.newBuilder();
			while (rs.next()) {
				tmpBannerBuilder.clear();
				homeBuilder.addBanner(BANNER_MAPPER.mapToItem(rs, tmpBannerBuilder).build());
			}
			
			DBUtil.closeQuietly(rs);
			rs = null;
			
			pstmt.getMoreResults();
			rs = pstmt.getResultSet();
			
			Map<Integer, List<DiscoverProtos.Module.Category>> categoryMap = new HashMap<Integer, List<DiscoverProtos.Module.Category>>();
			
			DiscoverProtos.Module.Category.Builder tmpCategoryBuilder = DiscoverProtos.Module.Category.newBuilder();
			while (rs.next()) {
				tmpCategoryBuilder.clear();
				CATEGORY_MAPPER.mapToItem(rs, tmpCategoryBuilder);
				DBUtil.addMapLinkedList(categoryMap, rs.getInt("module_id"), tmpCategoryBuilder.build());
			}
			
			DBUtil.closeQuietly(rs);
			rs = null;
			
			pstmt.getMoreResults();
			rs = pstmt.getResultSet();
			
			Map<Integer, DiscoverProtos.RecommendModule.Builder> recommendModuleBuilderMap = new LinkedHashMap<Integer, DiscoverProtos.RecommendModule.Builder>();
			DiscoverProtos.Module.Builder tmpModuleBuilder = DiscoverProtos.Module.newBuilder();
			while (rs.next()) {
				tmpModuleBuilder.clear();
				MODULE_MAPPER.mapToItem(rs, tmpModuleBuilder);
				List<DiscoverProtos.Module.Category> categoryList = categoryMap.get(tmpModuleBuilder.getModuleId());
				if (categoryList != null) {
					tmpModuleBuilder.addAllCategory(categoryList);
				}
				
				DiscoverProtos.Module module = tmpModuleBuilder.build();
				
				boolean isRecommend = rs.getBoolean("is_recommend");
				if (isRecommend) {
					recommendModuleBuilderMap.put(module.getModuleId(), DiscoverProtos.RecommendModule.newBuilder()
							.setModule(module));
				} else {
					homeBuilder.addModule(module);
				}
			}
			
			if (recommendModuleBuilderMap.isEmpty()) {
				return homeBuilder.build();
			}
			
			DBUtil.closeQuietly(rs);
			rs = null;
			
			StringBuilder sql = new StringBuilder();
			
			boolean isFirst = true;
			for (Integer moduleId : recommendModuleBuilderMap.keySet()) {
				if (isFirst) {
					isFirst = false;
				} else {
					sql.append(" UNION ALL ");
				}
				sql.append("(SELECT module_id, category_id, item_id FROM weizhu_discover_module_item_default WHERE module_id = ");
				sql.append(moduleId);
				sql.append(" ORDER BY create_time DESC, item_id DESC LIMIT 1)");
			}
			
			stmt = conn.createStatement();
			rs = stmt.executeQuery(sql.toString());
			
			Map<Long, List<Integer>> itemIdToModuleIdListMap = new HashMap<Long, List<Integer>>();
			while (rs.next()) {
				int moduleId = rs.getInt("module_id");
				int categoryId = rs.getInt("category_id");
				long itemId = rs.getLong("item_id");
				
				DiscoverProtos.RecommendModule.Builder recommendModuleBuilder = recommendModuleBuilderMap.get(moduleId);
				if (recommendModuleBuilder != null) {
					recommendModuleBuilder.setCategoryId(categoryId);
					DBUtil.addMapArrayList(itemIdToModuleIdListMap, itemId, moduleId);
				}
			}
			
			if (itemIdToModuleIdListMap.isEmpty()) {
				for (DiscoverProtos.RecommendModule.Builder recommendModuleBuilder : recommendModuleBuilderMap.values()) {
					homeBuilder.addModule(recommendModuleBuilder.getModule());
				}
				return homeBuilder.build();
			}
			
			DBUtil.closeQuietly(rs);
			rs = null;
			DBUtil.closeQuietly(stmt);
			stmt = null;
			
			sql = new StringBuilder();
			sql.append("SELECT item_id, item_name, icon_name, create_time, item_desc, enable_score, enable_comment FROM weizhu_discover_item WHERE item_id IN (");
			DBUtil.COMMA_JOINER.appendTo(sql, itemIdToModuleIdListMap.keySet());
			sql.append("); ");
			
			stmt = conn.createStatement();
			rs = stmt.executeQuery(sql.toString());
			
			DiscoverProtos.Item.Builder tmpItemBuilder = DiscoverProtos.Item.newBuilder();
			while (rs.next()) {
				tmpItemBuilder.clear();
				DiscoverProtos.Item item = ITEM_MAPPER.mapToItem(rs, tmpItemBuilder).build();
				
				List<Integer> moduleIdList = itemIdToModuleIdListMap.get(item.getItemId());
				if (moduleIdList != null) {
					for (Integer moduleId : moduleIdList) {
						DiscoverProtos.RecommendModule.Builder recommendModuleBuilder = recommendModuleBuilderMap.get(moduleId);
						if (recommendModuleBuilder != null) {
							recommendModuleBuilder.setItem(item);
						}
					}
				}
			}
			
			for (DiscoverProtos.RecommendModule.Builder recommendModuleBuilder : recommendModuleBuilderMap.values()) {
				if (recommendModuleBuilder.hasItem()) {
					homeBuilder.addRecommendModule(recommendModuleBuilder.build());
				} else {
					homeBuilder.addModule(recommendModuleBuilder.getModule());
				}
			}
			return homeBuilder.build();
		} finally {
			DBUtil.closeQuietly(rs);
			DBUtil.closeQuietly(stmt);
			DBUtil.closeQuietly(pstmt);
		}
	}
	
	private static final ProtobufMapper<DiscoverDAOProtos.ModuleItemDefault> MODULE_ITEM_DEFAULT_MAPPER = 
			ProtobufMapper.createMapper(DiscoverDAOProtos.ModuleItemDefault.getDefaultInstance(), 
					"item_id",
					"create_time");
	
	public static List<DiscoverDAOProtos.ModuleItemDefault> getModuleItemDefaultList(Connection conn, int moduleId, int categoryId,
			DiscoverDAOProtos.ModuleItemDefault begin, DiscoverDAOProtos.ModuleItemDefault end, int size) throws SQLException {
		if (size <= 0) {
			return Collections.emptyList();
		}
		
		StringBuilder sql = new StringBuilder();
		sql.append("SELECT item_id, create_time FROM weizhu_discover_module_item_default WHERE module_id = ")
			.append(moduleId).append(" AND category_id = ").append(categoryId);
		
		if (begin != null) {
			sql.append(" AND (create_time < ").append(begin.getCreateTime());
			sql.append(" OR (create_time = ").append(begin.getCreateTime()).append(" AND item_id < ").append(begin.getItemId()).append("))");
		}
		if (end != null) {
			sql.append(" AND (create_time > ").append(end.getCreateTime());
			sql.append(" OR (create_time = ").append(end.getCreateTime()).append(" AND item_id > ").append(end.getItemId()).append("))");
		}
		
		sql.append(" ORDER BY create_time DESC, item_id DESC LIMIT ").append(size).append("; ");
		
		Statement stmt = null;
		ResultSet rs = null;
		try {
			stmt = conn.createStatement();
			rs = stmt.executeQuery(sql.toString());
			return MODULE_ITEM_DEFAULT_MAPPER.mapToList(rs);
		} finally {
			DBUtil.closeQuietly(rs);
			DBUtil.closeQuietly(stmt);
		}
	}
	
	public static Map<Long, DiscoverProtos.ItemContent> getItemById(Connection conn, Collection<Long> itemIds) throws SQLException {
		if (itemIds.isEmpty()) {
			return Collections.emptyMap();
		}
		
		StringBuilder sql = new StringBuilder();
		sql.append("SELECT item_id as `item.item_id`, item_name as `item.item_name`, icon_name as `item.icon_name`, create_time as `item.create_time`, item_desc as `item.item_desc`, enable_score as `item.enable_score`, enable_comment as `item.enable_comment`, redirect_url FROM weizhu_discover_item WHERE item_id IN (");
		DBUtil.COMMA_JOINER.appendTo(sql, itemIds);
		sql.append("); ");
		
		Statement stmt = null;
		ResultSet rs = null;
		try {
			stmt = conn.createStatement();
			rs = stmt.executeQuery(sql.toString());
			
			Map<Long, DiscoverProtos.ItemContent> resultMap = new HashMap<Long, DiscoverProtos.ItemContent>(itemIds.size());
			DiscoverProtos.ItemContent.Builder tmpItemBuilder = DiscoverProtos.ItemContent.newBuilder();
			while (rs.next()) {
				tmpItemBuilder.clear();
				DiscoverProtos.ItemContent itemContent = ITEM_CONTENT_MAPPER.mapToItem(rs, tmpItemBuilder).build();
				resultMap.put(itemContent.getItem().getItemId(), itemContent);
			}
			return resultMap;
		} finally {
			DBUtil.closeQuietly(rs);
			DBUtil.closeQuietly(stmt);
		}
	}
	
	public static List<DiscoverProtos.Item> searchItemId(Connection conn, String keyword) throws SQLException {
		keyword = "%" + DBUtil.SQL_STRING_ESCAPER.escape(keyword) + "%";
		
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try {
			pstmt = conn.prepareStatement("SELECT * FROM weizhu_discover_item WHERE item_name LIKE ? ORDER BY create_time DESC LIMIT 20; ");
			
			pstmt.setString(1, keyword);
			
			rs = pstmt.executeQuery();
			
			return ITEM_MAPPER.mapToList(rs);
			
		} finally {
			DBUtil.closeQuietly(rs);
			DBUtil.closeQuietly(pstmt);
		}
	}
	
	public static Map<Long, Integer> getItemPV(Connection conn, Collection<Long> itemIds) throws SQLException {
		if (itemIds.isEmpty()) {
			return Collections.emptyMap();
		}
		
		StringBuilder sql = new StringBuilder();
		sql.append("SELECT * FROM weizhu_discover_item_pv WHERE item_id IN (");
		DBUtil.COMMA_JOINER.appendTo(sql, itemIds);
		sql.append("); ");
		
		Statement stmt = null;
		ResultSet rs = null;
		try {
			stmt = conn.createStatement();
			rs = stmt.executeQuery(sql.toString());
			
			Map<Long, Integer> itemPVMap = new HashMap<Long, Integer>(itemIds.size());
			while (rs.next()) {
				long itemId = rs.getLong("item_id");
				int pv = rs.getInt("pv");
				
				itemPVMap.put(itemId, pv);
			}
			
			for (Long itemId : itemIds) {
				if (!itemPVMap.containsKey(itemId)) {
					itemPVMap.put(itemId, 0);
				}
			}
			
			return itemPVMap;
		} finally {
			DBUtil.closeQuietly(rs);
			DBUtil.closeQuietly(stmt);
		}
	}
	
	public static void increItemPV(Connection conn, Map<Long, Integer> itemPVMap) throws SQLException {
		if (itemPVMap.isEmpty()) {
			return;
		}
		
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try {
			pstmt = conn.prepareStatement("UPDATE weizhu_discover_item_pv SET pv = ? WHERE item_id = ? AND pv < ?; INSERT IGNORE INTO weizhu_discover_item_pv (item_id, pv) VALUES (?, ?); ");
			
			for (Entry<Long, Integer> entry : itemPVMap.entrySet()) {
				pstmt.setInt(1, entry.getValue());
				pstmt.setLong(2, entry.getKey());
				pstmt.setInt(3, entry.getValue());
				pstmt.setLong(4, entry.getKey());
				pstmt.setInt(5, entry.getValue());
				
				pstmt.addBatch();
			}
			
			pstmt.executeBatch();
			
		} finally {
			DBUtil.closeQuietly(rs);
			DBUtil.closeQuietly(pstmt);
		}
	}
	
	public static Map<Long, DiscoverDAOProtos.ItemScore> getItemScore(Connection conn, Collection<Long> itemIds) throws SQLException {
		if (itemIds.isEmpty()) {
			return Collections.emptyMap();
		}
		
		StringBuilder sql = new StringBuilder();
		sql.append("SELECT item_id, sum(score) as total_score, count(user_id) as total_user FROM weizhu_discover_item_score WHERE item_id IN (");
		DBUtil.COMMA_JOINER.appendTo(sql, itemIds);
		sql.append(") GROUP BY item_id; ");
		
		Statement stmt = null;
		ResultSet rs = null;
		try {
			stmt = conn.createStatement();
			rs = stmt.executeQuery(sql.toString());
			
			Map<Long, DiscoverDAOProtos.ItemScore> resultMap = new HashMap<Long, DiscoverDAOProtos.ItemScore>(itemIds.size());
			
			DiscoverDAOProtos.ItemScore.Builder tmpBuilder = DiscoverDAOProtos.ItemScore.newBuilder();
			while (rs.next()) {
				tmpBuilder.clear();
				tmpBuilder.setTotalScore(rs.getInt("total_score"));
				tmpBuilder.setTotalUser(rs.getInt("total_user"));
				resultMap.put(rs.getLong("item_id"), tmpBuilder.build());
			}
			
			DiscoverDAOProtos.ItemScore emptyItemScore = tmpBuilder.clear().setTotalScore(0).setTotalUser(0).build();
			for (Long itemId : itemIds) {
				if (!resultMap.containsKey(itemId)) {
					resultMap.put(itemId, emptyItemScore);
				}
			}
			return resultMap;
		} finally {
			DBUtil.closeQuietly(rs);
			DBUtil.closeQuietly(stmt);
		}
	}
	
	public static Integer getUserItemScore(Connection conn, long userId, long itemId) throws SQLException {
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try {
			pstmt = conn.prepareStatement("SELECT score FROM weizhu_discover_item_score WHERE item_id = ? AND user_id = ?; ");
			
			pstmt.setLong(1, itemId);
			pstmt.setLong(2, userId);

			rs = pstmt.executeQuery();
			
			if (rs.next()) {
				return rs.getInt("score");
			} else {
				return null;
			}
			
		} finally {
			DBUtil.closeQuietly(rs);
			DBUtil.closeQuietly(pstmt);
		}
	}
	
	public static void insertUserItemScore(Connection conn, long userId, long itemId, int score) throws SQLException {
		PreparedStatement pstmt = null;
		try {
			pstmt = conn.prepareStatement("INSERT INTO weizhu_discover_item_score (item_id, user_id, score) VALUES (?, ?, ?); ");
			
			pstmt.setLong(1, itemId);
			pstmt.setLong(2, userId);
			pstmt.setInt(3, score);

			pstmt.executeUpdate();
			
		} finally {
			DBUtil.closeQuietly(pstmt);
		}
	}
	
	public static Map<Long, Integer> getItemAllCommentCount(Connection conn, Collection<Long> itemIds) throws SQLException {
		if (itemIds.isEmpty()) {
			return Collections.emptyMap();
		}
		
		StringBuilder sql = new StringBuilder();
		sql.append("SELECT item_id, count(comment_id) as count FROM weizhu_discover_comment WHERE item_id IN (");
		DBUtil.COMMA_JOINER.appendTo(sql, itemIds);
		sql.append(") GROUP BY item_id; ");
		
		Statement stmt = null;
		ResultSet rs = null;
		try {
			stmt = conn.createStatement();
			rs = stmt.executeQuery(sql.toString());
			
			Map<Long, Integer> resultMap = new HashMap<Long, Integer>(itemIds.size());
			
			while (rs.next()) {
				long itemId = rs.getLong("item_id");
				int count = rs.getInt("count");
				resultMap.put(itemId, count);
			}
			
			for (Long itemId : itemIds) {
				if (!resultMap.containsKey(itemId)) {
					resultMap.put(itemId, 0);
				}
			}
			return resultMap;
		} finally {
			DBUtil.closeQuietly(rs);
			DBUtil.closeQuietly(stmt);
		}
	}
	
	public static Map<Long, Integer> getItemMyCommentCount(Connection conn, long userId, Collection<Long> itemIds) throws SQLException {
		if (itemIds.isEmpty()) {
			return Collections.emptyMap();
		}
		
		StringBuilder sql = new StringBuilder();
		sql.append("SELECT item_id, count(comment_id) as count FROM weizhu_discover_comment WHERE item_id IN (");
		DBUtil.COMMA_JOINER.appendTo(sql, itemIds);
		sql.append(") AND user_id = ").append(userId).append(" GROUP BY item_id; ");
		
		Statement stmt = null;
		ResultSet rs = null;
		try {
			stmt = conn.createStatement();
			rs = stmt.executeQuery(sql.toString());
			
			Map<Long, Integer> resultMap = new HashMap<Long, Integer>(itemIds.size());
			
			while (rs.next()) {
				long itemId = rs.getLong("item_id");
				int count = rs.getInt("count");
				resultMap.put(itemId, count);
			}
			
			for (Long itemId : itemIds) {
				if (!resultMap.containsKey(itemId)) {
					resultMap.put(itemId, 0);
				}
			}
			return resultMap;
		} finally {
			DBUtil.closeQuietly(rs);
			DBUtil.closeQuietly(stmt);
		}
	}
	
	private static final ProtobufMapper<DiscoverProtos.Comment> COMMENT_MAPPER = 
			ProtobufMapper.createMapper(DiscoverProtos.Comment.getDefaultInstance(), 
					"comment_id",
					"comment_time",
					"user_id",
					"content");
	
	public static List<DiscoverProtos.Comment> getItemAllCommentList(Connection conn, long itemId, int size) throws SQLException {
		if (size <= 0) {
			return Collections.emptyList();
		}
		
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try {
			pstmt = conn.prepareStatement("SELECT * FROM weizhu_discover_comment WHERE item_id = ? ORDER BY comment_time DESC, comment_id DESC LIMIT ?; ");
			
			pstmt.setLong(1, itemId);
			pstmt.setInt(2, size);

			rs = pstmt.executeQuery();
			
			return COMMENT_MAPPER.mapToList(rs);
			
		} finally {
			DBUtil.closeQuietly(rs);
			DBUtil.closeQuietly(pstmt);
		}
	}
	
	public static List<DiscoverProtos.Comment> getItemAllCommentList(Connection conn, long itemId, long lastCommentId, int lastCommentTime, int size) throws SQLException {
		if (size <= 0) {
			return Collections.emptyList();
		}
		
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try {
			pstmt = conn.prepareStatement("SELECT * FROM weizhu_discover_comment WHERE item_id = ? AND (comment_time < ? OR (comment_time = ? AND comment_id < ?)) ORDER BY comment_time DESC, comment_id DESC LIMIT ?; ");
			
			pstmt.setLong(1, itemId);
			pstmt.setInt(2, lastCommentTime);
			pstmt.setInt(3, lastCommentTime);
			pstmt.setLong(4, lastCommentId);
			pstmt.setInt(5, size);

			rs = pstmt.executeQuery();
			
			return COMMENT_MAPPER.mapToList(rs);
			
		} finally {
			DBUtil.closeQuietly(rs);
			DBUtil.closeQuietly(pstmt);
		}
	}
	
	public static List<DiscoverProtos.Comment> getItemMyCommentList(Connection conn, long userId, long itemId, int size) throws SQLException {
		if (size <= 0) {
			return Collections.emptyList();
		}
		
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try {
			pstmt = conn.prepareStatement("SELECT * FROM weizhu_discover_comment WHERE item_id = ? AND user_id = ? ORDER BY comment_time DESC, comment_id DESC LIMIT ?; ");
			
			pstmt.setLong(1, itemId);
			pstmt.setLong(2, userId);
			pstmt.setInt(3, size);

			rs = pstmt.executeQuery();
			
			return COMMENT_MAPPER.mapToList(rs);
			
		} finally {
			DBUtil.closeQuietly(rs);
			DBUtil.closeQuietly(pstmt);
		}
	}
	
	public static List<DiscoverProtos.Comment> getItemMyCommentList(Connection conn, long userId, long itemId, long lastCommentId, int lastCommentTime, int size) throws SQLException {
		if (size <= 0) {
			return Collections.emptyList();
		}
		
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try {
			pstmt = conn.prepareStatement("SELECT * FROM weizhu_discover_comment WHERE item_id = ? AND user_id = ? AND (comment_time < ? OR (comment_time = ? AND comment_id < ?)) ORDER BY comment_time DESC, comment_id DESC LIMIT ?; ");
			
			pstmt.setLong(1, itemId);
			pstmt.setLong(2, userId);
			pstmt.setInt(3, lastCommentTime);
			pstmt.setInt(4, lastCommentTime);
			pstmt.setLong(5, lastCommentId);
			pstmt.setInt(6, size);

			rs = pstmt.executeQuery();
			
			return COMMENT_MAPPER.mapToList(rs);
			
		} finally {
			DBUtil.closeQuietly(rs);
			DBUtil.closeQuietly(pstmt);
		}
	}

	public static long insertItemComment(Connection conn, long itemId, DiscoverProtos.Comment comment) throws SQLException {
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try {
			pstmt = conn.prepareStatement("INSERT INTO weizhu_discover_comment (comment_id, comment_time, user_id, content, item_id) VALUES (NULL, ?, ?, ?, ?); ", Statement.RETURN_GENERATED_KEYS);
			
			DBUtil.set(pstmt, 1, comment.hasCommentTime(), comment.getCommentTime());
			DBUtil.set(pstmt, 2, comment.hasUserId(), comment.getUserId());
			DBUtil.set(pstmt, 3, comment.hasContent(), comment.getContent());
			DBUtil.set(pstmt, 4, true, itemId);
			
			pstmt.executeUpdate();
			
			rs = pstmt.getGeneratedKeys();
			if (!rs.next()) {
				throw new RuntimeException("insert fail");
			}
			
			return rs.getLong(1);
		} finally {
			DBUtil.closeQuietly(rs);
			DBUtil.closeQuietly(pstmt);
		}
	}
	
	public static Map<Long, DiscoverProtos.Comment> getComment(Connection conn, Collection<Long> commentIds) throws SQLException {
		if (commentIds.isEmpty()) {
			return Collections.emptyMap();
		}
		
		StringBuilder sql = new StringBuilder();
		sql.append("SELECT * FROM weizhu_discover_comment WHERE comment_id IN (");
		DBUtil.COMMA_JOINER.appendTo(sql, commentIds);
		sql.append("); ");
		
		Statement stmt = null;
		ResultSet rs = null;
		try {
			stmt = conn.createStatement();
			
			rs = stmt.executeQuery(sql.toString());
			
			Map<Long, DiscoverProtos.Comment> resultMap = new HashMap<Long, DiscoverProtos.Comment>(commentIds.size());
			
			DiscoverProtos.Comment.Builder tmpBuilder = DiscoverProtos.Comment.newBuilder();
			while (rs.next()) {
				tmpBuilder.clear();
				
				DiscoverProtos.Comment comment = COMMENT_MAPPER.mapToItem(rs, tmpBuilder).build();
				resultMap.put(comment.getCommentId(), comment);
			}
			
			return resultMap;
		} finally {
			DBUtil.closeQuietly(rs);
			DBUtil.closeQuietly(stmt);
		}
	}
	
	public static void deleteComment(Connection conn, Collection<Long> commentIds) throws SQLException {
		if (commentIds.isEmpty()) {
			return;
		}
		
		StringBuilder sql = new StringBuilder();
		sql.append("DELETE FROM weizhu_discover_comment WHERE comment_id IN (");
		DBUtil.COMMA_JOINER.appendTo(sql, commentIds);
		sql.append("); ");
		
		Statement stmt = null;
		try {
			stmt = conn.createStatement();
			stmt.executeUpdate(sql.toString());
		} finally {
			DBUtil.closeQuietly(stmt);
		}
	}
	
	public static List<Long> getItemIdList(Connection conn, @Nullable Long lastItemId, int size) throws SQLException {
		if (size <= 0) {
			return Collections.emptyList();
		}
		
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try {
			if (lastItemId == null) {
				pstmt = conn.prepareStatement("SELECT item_id FROM weizhu_discover_item ORDER BY item_id ASC LIMIT ?; ");
				pstmt.setInt(1, size);
			} else {
				pstmt = conn.prepareStatement("SELECT item_id FROM weizhu_discover_item WHERE item_id > ? ORDER BY item_id ASC LIMIT ?; ");
				pstmt.setLong(1, lastItemId);
				pstmt.setInt(2, size);
			}
			
			rs = pstmt.executeQuery();
			
			List<Long> list = new ArrayList<Long>();
			while (rs.next()) {
				list.add(rs.getLong("item_id"));
			}
			return list;
			
		} finally {
			DBUtil.closeQuietly(rs);
			DBUtil.closeQuietly(pstmt);
		}
	}
	
}
