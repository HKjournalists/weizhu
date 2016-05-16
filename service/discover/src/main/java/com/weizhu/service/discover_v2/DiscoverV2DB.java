package com.weizhu.service.discover_v2;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import javax.annotation.Nullable;

import com.google.common.base.Functions;
import com.google.common.collect.Iterables;
import com.weizhu.common.db.DBUtil;
import com.weizhu.common.db.ProtobufMapper;
import com.weizhu.common.utils.DataPage;
import com.weizhu.proto.DiscoverV2Protos;

public class DiscoverV2DB {

	private static final ProtobufMapper<DiscoverV2Protos.Banner> BANNER_MAPPER = ProtobufMapper.createMapper(DiscoverV2Protos.Banner.getDefaultInstance(),
			"banner_id",
			"banner_name",
			"image_name",
			"allow_model_id",
			"item_id",
			"web_url.web_url",
			"web_url.is_weizhu",
			"app_uri.app_uri",
			"state",
			"create_admin_id",
			"create_time",
			"update_admin_id",
			"update_time");

	private static final ProtobufMapper<DiscoverV2Protos.Module> MODULE_MAPPER = ProtobufMapper.createMapper(DiscoverV2Protos.Module.getDefaultInstance(),
			"module_id",
			"module_name",
			"image_name",
			"allow_model_id",
			"web_url.web_url",
			"web_url.is_weizhu",
			"app_uri.app_uri",
			"state",
			"create_admin_id",
			"create_time",
			"update_admin_id",
			"update_time");

	private static final ProtobufMapper<DiscoverV2Protos.Module.Category> MODULE_CATEGORY_MAPPER = ProtobufMapper.createMapper(DiscoverV2Protos.Module.Category.getDefaultInstance(),
			"category_id",
			"module_id",
			"category_name",
			"allow_model_id",
			"state",
			"create_admin_id",
			"create_time",
			"update_admin_id",
			"update_time");

	public static DiscoverV2DAOProtos.DiscoverHome getDiscoverHome(Connection conn, long companyId, @Nullable Collection<DiscoverV2Protos.State> states)
			throws SQLException {
		if (states != null && states.isEmpty()) {
			return DiscoverV2DAOProtos.DiscoverHome.newBuilder().build();
		}

		String statsCondStr;
		if (states == null) {
			statsCondStr = "";
		} else {
			StringBuilder sb = new StringBuilder(" AND state IN ");
			appendStatesCondSql(sb, states);
			statsCondStr = sb.toString();
		}

		StringBuilder sql = new StringBuilder();
		sql.append("SELECT * FROM weizhu_discover_v2_module_category WHERE company_id = ").append(companyId).append(statsCondStr).append(" ORDER BY category_id ASC; ");
		sql.append("SELECT * FROM weizhu_discover_v2_module WHERE company_id = ").append(companyId).append(statsCondStr).append(" ORDER BY module_id ASC; ");
		sql.append("SELECT * FROM weizhu_discover_v2_banner WHERE company_id = ").append(companyId).append(statsCondStr).append( "ORDER BY banner_id DESC; ");
		sql.append("SELECT * FROM weizhu_discover_v2_home WHERE company_id = ").append(companyId).append(";");

		Statement stmt = null;
		ResultSet rs = null;
		try {
			stmt = conn.createStatement();
			stmt.execute(sql.toString());

			rs = stmt.getResultSet();

			// 获取所有模块分类
			Map<Integer, LinkedList<DiscoverV2Protos.Module.Category>> moduleCategoryMap = new TreeMap<Integer, LinkedList<DiscoverV2Protos.Module.Category>>();

			DiscoverV2Protos.Module.Category.Builder tmpCategoryBuilder = DiscoverV2Protos.Module.Category.newBuilder();
			while (rs.next()) {
				tmpCategoryBuilder.clear();

				int moduleId = rs.getInt("module_id");
				DiscoverV2Protos.Module.Category category = MODULE_CATEGORY_MAPPER.mapToItem(rs, tmpCategoryBuilder).build();

				LinkedList<DiscoverV2Protos.Module.Category> list = moduleCategoryMap.get(moduleId);
				if (list == null) {
					list = new LinkedList<DiscoverV2Protos.Module.Category>();
					moduleCategoryMap.put(moduleId, list);
				}
				list.add(category);
			}

			DBUtil.closeQuietly(rs);
			rs = null;

			stmt.getMoreResults();
			rs = stmt.getResultSet();

			// 获取所有模块
			LinkedList<DiscoverV2Protos.Module> moduleList = new LinkedList<DiscoverV2Protos.Module>();
			LinkedList<DiscoverV2DAOProtos.ModulePromptDot> modulePromptDotList = new LinkedList<DiscoverV2DAOProtos.ModulePromptDot>();

			DiscoverV2Protos.Module.Builder tmpModuleBuilder = DiscoverV2Protos.Module.newBuilder();
			DiscoverV2Protos.Module.CategoryList.Builder tmpCategoryListBuilder = DiscoverV2Protos.Module.CategoryList.newBuilder();
			DiscoverV2DAOProtos.ModulePromptDot.Builder tmpModulePromptDotBuilder = DiscoverV2DAOProtos.ModulePromptDot.newBuilder();
			while (rs.next()) {
				tmpModuleBuilder.clear();
				tmpCategoryListBuilder.clear();

				MODULE_MAPPER.mapToItem(rs, tmpModuleBuilder);

				LinkedList<DiscoverV2Protos.Module.Category> categoryList = moduleCategoryMap.remove(tmpModuleBuilder.getModuleId());

				if (categoryList != null && !categoryList.isEmpty()) {
					String categoryOrderStr = rs.getString("category_order_str");
					if (categoryOrderStr != null) {
						for (String categoryIdStr : DBUtil.COMMA_SPLITTER.split(categoryOrderStr)) {
							int categoryId;
							try {
								categoryId = Integer.parseInt(categoryIdStr);
							} catch (NumberFormatException e) {
								continue;
							}

							Iterator<DiscoverV2Protos.Module.Category> it = categoryList.iterator();
							while (it.hasNext()) {
								DiscoverV2Protos.Module.Category category = it.next();
								if (categoryId == category.getCategoryId()) {
									tmpCategoryListBuilder.addCategory(category);
									it.remove();
									break;
								}
							}
						}
					}

					tmpCategoryListBuilder.addAllCategory(categoryList);

					tmpModuleBuilder.setCategoryList(tmpCategoryListBuilder.build());
				}

				moduleList.add(tmpModuleBuilder.build());

				long promptDotTimestamp = rs.getLong("prompt_dot_timestamp");
				if (!rs.wasNull()) {
					tmpModulePromptDotBuilder.clear();

					tmpModulePromptDotBuilder.setModuleId(rs.getInt("module_id"));
					tmpModulePromptDotBuilder.setPromptDotTimestamp(promptDotTimestamp);

					modulePromptDotList.add(tmpModulePromptDotBuilder.build());
				}

			}

			DBUtil.closeQuietly(rs);
			rs = null;

			stmt.getMoreResults();
			rs = stmt.getResultSet();

			// 获取所有banner
			LinkedList<DiscoverV2Protos.Banner> bannerList = new LinkedList<DiscoverV2Protos.Banner>();

			DiscoverV2Protos.Banner.Builder tmpBannerBuilder = DiscoverV2Protos.Banner.newBuilder();
			while (rs.next()) {
				tmpBannerBuilder.clear();
				bannerList.add(BANNER_MAPPER.mapToItem(rs, tmpBannerBuilder).build());
			}

			DBUtil.closeQuietly(rs);
			rs = null;

			stmt.getMoreResults();
			rs = stmt.getResultSet();

			// 拼装最后的Home
			DiscoverV2DAOProtos.DiscoverHome.Builder homeBuilder = DiscoverV2DAOProtos.DiscoverHome.newBuilder();

			if (rs.next()) {
				String bannerOrderStr = rs.getString("banner_order_str");
				if (bannerOrderStr != null) {
					for (String bannerIdStr : DBUtil.COMMA_SPLITTER.split(bannerOrderStr)) {
						int bannerId;
						try {
							bannerId = Integer.parseInt(bannerIdStr);
						} catch (NumberFormatException e) {
							continue;
						}

						Iterator<DiscoverV2Protos.Banner> it = bannerList.iterator();
						while (it.hasNext()) {
							DiscoverV2Protos.Banner banner = it.next();
							if (bannerId == banner.getBannerId()) {
								homeBuilder.addBanner(banner);
								it.remove();
								break;
							}
						}
					}
				}

				String moduleOrderStr = rs.getString("module_order_str");
				if (moduleOrderStr != null) {
					for (String moduleIdStr : DBUtil.COMMA_SPLITTER.split(moduleOrderStr)) {
						int moduleId;
						try {
							moduleId = Integer.parseInt(moduleIdStr);
						} catch (NumberFormatException e) {
							continue;
						}

						Iterator<DiscoverV2Protos.Module> it = moduleList.iterator();
						while (it.hasNext()) {
							DiscoverV2Protos.Module module = it.next();
							if (moduleId == module.getModuleId()) {
								homeBuilder.addModule(module);
								it.remove();
								break;
							}
						}
					}
				}
			}

			homeBuilder.addAllBanner(bannerList);
			homeBuilder.addAllModule(moduleList);
			homeBuilder.addAllModulePromptDot(modulePromptDotList);

			return homeBuilder.build();
		} finally {
			DBUtil.closeQuietly(rs);
			DBUtil.closeQuietly(stmt);
		}
	}

	private static final ProtobufMapper<DiscoverV2DAOProtos.ModuleCategoryItem> MODULE_CATEGORY_ITEM_MAPPER = ProtobufMapper.createMapper(DiscoverV2DAOProtos.ModuleCategoryItem.getDefaultInstance(),
			"category_id",
			"item_id",
			"item_allow_model_id",
			"item_state",
			"item_create_time",
			"create_admin_id",
			"create_time");

	public static Map<Integer, DiscoverV2DAOProtos.ModuleCategoryItemList> getModuleCategoryItemListOrderByCreateTime(Connection conn, long companyId, 
			Collection<Integer> categoryIds, @Nullable Collection<DiscoverV2Protos.State> states, int size) throws SQLException {
		if (categoryIds.isEmpty() || (states != null && states.isEmpty()) || size <= 0) {
			return Collections.emptyMap();
		}

		final Set<Integer> categoryIdSet = new TreeSet<Integer>(categoryIds);

		StringBuilder sql = new StringBuilder();
		for (Integer categoryId : categoryIdSet) {
			sql.append("SELECT A.category_id AS category_id, A.item_id AS item_id, A.create_admin_id AS create_admin_id, A.create_time AS create_time, B.allow_model_id AS item_allow_model_id, B.state AS item_state, B.create_time AS item_create_time ");
			sql.append("FROM weizhu_discover_v2_module_category_item A ");
			sql.append("INNER JOIN weizhu_discover_v2_item_base B ");
			sql.append("ON A.item_id = B.item_id ");
			sql.append("WHERE A.category_id = ").append(categoryId).append(" ");
			sql.append(" AND A.company_id = B.company_id AND A.company_id = ").append(companyId);
			
			if (states != null) {
				sql.append(" AND B.state IN ");
				appendStatesCondSql(sql, states);
			}

			sql.append(" ORDER BY A.create_time DESC, A.item_id DESC LIMIT ").append(size).append("; ");
		}

		Statement stmt = null;
		ResultSet rs = null;
		try {
			stmt = conn.createStatement();
			stmt.execute(sql.toString());

			Map<Integer, DiscoverV2DAOProtos.ModuleCategoryItemList.Builder> resultBuilderMap = new TreeMap<Integer, DiscoverV2DAOProtos.ModuleCategoryItemList.Builder>();
			DiscoverV2DAOProtos.ModuleCategoryItem.Builder tmpBuilder = DiscoverV2DAOProtos.ModuleCategoryItem.newBuilder();
			for (int i = 0; i < categoryIdSet.size(); ++i) {
				if (i > 0) {
					stmt.getMoreResults();
				}
				rs = stmt.getResultSet();

				while (rs.next()) {
					tmpBuilder.clear();

					DiscoverV2DAOProtos.ModuleCategoryItem moduleCategoryItem = MODULE_CATEGORY_ITEM_MAPPER.mapToItem(rs, tmpBuilder).build();

					DiscoverV2DAOProtos.ModuleCategoryItemList.Builder resultBuilder = resultBuilderMap.get(moduleCategoryItem.getCategoryId());
					if (resultBuilder == null) {
						resultBuilder = DiscoverV2DAOProtos.ModuleCategoryItemList.newBuilder();
						resultBuilderMap.put(moduleCategoryItem.getCategoryId(), resultBuilder);
					}
					resultBuilder.addModuleCategoryItem(moduleCategoryItem);
				}

				DBUtil.closeQuietly(rs);
				rs = null;
			}

			Map<Integer, DiscoverV2DAOProtos.ModuleCategoryItemList> resultMap = new TreeMap<Integer, DiscoverV2DAOProtos.ModuleCategoryItemList>();
			for (Entry<Integer, DiscoverV2DAOProtos.ModuleCategoryItemList.Builder> entry : resultBuilderMap.entrySet()) {
				resultMap.put(entry.getKey(), entry.getValue().build());
			}

			return resultMap;
		} finally {
			DBUtil.closeQuietly(rs);
			DBUtil.closeQuietly(stmt);
		}
	}

	public static DiscoverV2DAOProtos.ModuleCategoryItemList getModuleCategoryItemListOrderByCreateTime(Connection conn, long companyId, int categoryId,
			@Nullable Collection<DiscoverV2Protos.State> states, @Nullable DiscoverV2DAOProtos.ModuleCategoryItem lastIndex, int size)
			throws SQLException {
		if ((states != null && states.isEmpty()) || size <= 0) {
			return DiscoverV2DAOProtos.ModuleCategoryItemList.getDefaultInstance();
		}

		StringBuilder sql = new StringBuilder();

		sql.append("SELECT A.category_id AS category_id, A.item_id AS item_id, A.create_admin_id AS create_admin_id, A.create_time AS create_time, B.allow_model_id AS item_allow_model_id, B.state AS item_state, B.create_time AS item_create_time ");
		sql.append("FROM weizhu_discover_v2_module_category_item A ");
		sql.append("INNER JOIN weizhu_discover_v2_item_base B ");
		sql.append("ON A.item_id = B.item_id ");
		sql.append("WHERE A.category_id = ").append(categoryId).append(" ");
		sql.append(" AND A.company_id = B.company_id AND A.company_id = ").append(companyId);

		if (states != null) {
			sql.append(" AND B.state IN ");
			appendStatesCondSql(sql, states);
		}

		if (lastIndex != null) {
			sql.append(" AND (A.create_time < ").append(lastIndex.getCreateTime());
			sql.append(" OR (A.create_time = ").append(lastIndex.getCreateTime());
			sql.append(" AND A.item_id < ").append(lastIndex.getItemId()).append(")) ");
		}

		sql.append(" ORDER BY A.create_time DESC, A.item_id DESC LIMIT ").append(size);

		Statement stmt = null;
		ResultSet rs = null;
		try {
			stmt = conn.createStatement();
			rs = stmt.executeQuery(sql.toString());

			DiscoverV2DAOProtos.ModuleCategoryItemList.Builder resultBuilder = DiscoverV2DAOProtos.ModuleCategoryItemList.newBuilder();
			DiscoverV2DAOProtos.ModuleCategoryItem.Builder tmpBuilder = DiscoverV2DAOProtos.ModuleCategoryItem.newBuilder();
			while (rs.next()) {
				tmpBuilder.clear();

				resultBuilder.addModuleCategoryItem(MODULE_CATEGORY_ITEM_MAPPER.mapToItem(rs, tmpBuilder).build());
			}

			return resultBuilder.build();
		} finally {
			DBUtil.closeQuietly(rs);
			DBUtil.closeQuietly(stmt);
		}
	}

	private static final ProtobufMapper<DiscoverV2Protos.Item.Base> ITEM_BASE_MAPPER = ProtobufMapper.createMapper(DiscoverV2Protos.Item.Base.getDefaultInstance(),
			"item_id",
			"item_name",
			"item_desc",
			"image_name",
			"allow_model_id",
			"enable_comment",
			"enable_score",
			"enable_remind",
			"enable_like",
			"enable_share",
			"enable_external_share",
			"web_url.web_url",
			"web_url.is_weizhu",
			"document.document_url",
			"document.document_type",
			"document.document_size",
			"document.check_md5",
			"document.is_download",
			"document.is_auth_url",
			"video.video_url",
			"video.video_type",
			"video.video_size",
			"video.video_time",
			"video.check_md5",
			"video.is_download",
			"video.is_auth_url",
			"audio.audio_url",
			"audio.audio_type",
			"audio.audio_size",
			"audio.audio_time",
			"audio.check_md5",
			"audio.is_download",
			"audio.is_auth_url",
			"app_uri.app_uri",
			"state",
			"create_admin_id",
			"create_time",
			"update_admin_id",
			"update_time");

	public static Map<Long, DiscoverV2Protos.Item.Base> getItemBase(Connection conn, long companyId, Collection<Long> itemIds) throws SQLException {
		if (itemIds.isEmpty()) {
			return Collections.emptyMap();
		}

		StringBuilder sql = new StringBuilder();

		sql.append("SELECT * FROM weizhu_discover_v2_item_base WHERE company_id = ").append(companyId).append(" AND item_id IN (");
		DBUtil.COMMA_JOINER.appendTo(sql, itemIds);
		sql.append("); ");

		Statement stmt = null;
		ResultSet rs = null;
		try {
			stmt = conn.createStatement();
			rs = stmt.executeQuery(sql.toString());

			Map<Long, DiscoverV2Protos.Item.Base> resultMap = new TreeMap<Long, DiscoverV2Protos.Item.Base>();
			DiscoverV2Protos.Item.Base.Builder tmpBuilder = DiscoverV2Protos.Item.Base.newBuilder();
			while (rs.next()) {
				tmpBuilder.clear();

				DiscoverV2Protos.Item.Base itemBase = ITEM_BASE_MAPPER.mapToItem(rs, tmpBuilder).build();
				resultMap.put(itemBase.getItemId(), itemBase);
			}

			return resultMap;
		} finally {
			DBUtil.closeQuietly(rs);
			DBUtil.closeQuietly(stmt);
		}
	}

	public static Map<Long, DiscoverV2Protos.Item.Count> getItemCount(Connection conn, long companyId, Collection<Long> itemIds) throws SQLException {
		if (itemIds.isEmpty()) {
			return Collections.emptyMap();
		}

		Map<Long, DiscoverV2Protos.Item.Count.Builder> resultBuilderMap = new TreeMap<Long, DiscoverV2Protos.Item.Count.Builder>();
		for (Long itemId : itemIds) {
			if (!resultBuilderMap.containsKey(itemId)) {
				resultBuilderMap.put(itemId, DiscoverV2Protos.Item.Count.newBuilder()
						.setLearnCnt(0)
						.setLearnUserCnt(0)
						.setCommentCnt(0)
						.setCommentUserCnt(0)
						.setScoreNumber(0)
						.setScoreUserCnt(0)
						.setLikeCnt(0)
						.setShareCnt(0));
			}
		}

		String itemIdStr = DBUtil.COMMA_JOINER.join(resultBuilderMap.keySet());

		StringBuilder sql = new StringBuilder();

		sql.append("SELECT item_id, SUM(learn_cnt) AS learn_cnt, COUNT(DISTINCT user_id) AS learn_user_cnt FROM weizhu_discover_v2_item_learn WHERE company_id = ").append(companyId).append(" AND item_id IN (")
				.append(itemIdStr)
				.append(") AND learn_cnt > 0 GROUP BY item_id; ");
		sql.append("SELECT item_id, COUNT(*) AS comment_cnt, COUNT(DISTINCT user_id) AS comment_user_cnt FROM weizhu_discover_v2_item_comment WHERE company_id = ").append(companyId).append(" AND item_id IN (")
				.append(itemIdStr)
				.append(") AND is_delete = 0 GROUP BY item_id; ");
		sql.append("SELECT item_id, SUM(score_number) AS score_total_number, COUNT(DISTINCT user_id) AS score_user_cnt FROM weizhu_discover_v2_item_score WHERE company_id = ").append(companyId).append(" AND item_id IN (")
				.append(itemIdStr)
				.append(") GROUP BY item_id; ");
		sql.append("SELECT item_id, COUNT(DISTINCT user_id) AS like_cnt FROM weizhu_discover_v2_item_like WHERE company_id = ").append(companyId).append(" AND item_id IN (")
				.append(itemIdStr)
				.append(") GROUP BY item_id; ");
		sql.append("SELECT item_id, COUNT(DISTINCT user_id) AS share_cnt FROM weizhu_discover_v2_item_share WHERE company_id = ").append(companyId).append(" AND item_id IN (")
				.append(itemIdStr)
				.append(") GROUP BY item_id; ");
		Statement stmt = null;
		ResultSet rs = null;
		try {
			stmt = conn.createStatement();

			stmt.execute(sql.toString());
			rs = stmt.getResultSet();
			while (rs.next()) {
				long itemId = rs.getLong("item_id");
				int learnCnt = rs.getInt("learn_cnt");
				int learnUserCnt = rs.getInt("learn_user_cnt");

				DiscoverV2Protos.Item.Count.Builder builder = resultBuilderMap.get(itemId);
				if (builder != null) {
					builder.setLearnCnt(learnCnt);
					builder.setLearnUserCnt(learnUserCnt);
				}
			}

			DBUtil.closeQuietly(rs);
			rs = null;

			stmt.getMoreResults();
			rs = stmt.getResultSet();

			while (rs.next()) {
				long itemId = rs.getLong("item_id");
				int commentCnt = rs.getInt("comment_cnt");
				int commentUserCnt = rs.getInt("comment_user_cnt");

				DiscoverV2Protos.Item.Count.Builder builder = resultBuilderMap.get(itemId);
				if (builder != null) {
					builder.setCommentCnt(commentCnt);
					builder.setCommentUserCnt(commentUserCnt);
				}
			}

			DBUtil.closeQuietly(rs);
			rs = null;

			stmt.getMoreResults();
			rs = stmt.getResultSet();

			while (rs.next()) {
				long itemId = rs.getLong("item_id");
				int scoreTotalNumber = rs.getInt("score_total_number");
				int scoreUserCnt = rs.getInt("score_user_cnt");

				DiscoverV2Protos.Item.Count.Builder builder = resultBuilderMap.get(itemId);
				if (builder != null && scoreUserCnt > 0) {
					builder.setScoreNumber(Math.round(((float) scoreTotalNumber) / ((float) scoreUserCnt)));
					builder.setScoreUserCnt(scoreUserCnt);
				}
			}

			DBUtil.closeQuietly(rs);
			rs = null;

			stmt.getMoreResults();
			rs = stmt.getResultSet();

			while (rs.next()) {
				long itemId = rs.getLong("item_id");
				int likeCnt = rs.getInt("like_cnt");

				DiscoverV2Protos.Item.Count.Builder builder = resultBuilderMap.get(itemId);
				if (builder != null) {
					builder.setLikeCnt(likeCnt);
				}
			}
			
			DBUtil.closeQuietly(rs);
			rs = null;

			stmt.getMoreResults();
			rs = stmt.getResultSet();

			while (rs.next()) {
				long itemId = rs.getLong("item_id");
				int shareCnt = rs.getInt("share_cnt");

				DiscoverV2Protos.Item.Count.Builder builder = resultBuilderMap.get(itemId);
				if (builder != null) {
					builder.setShareCnt(shareCnt);
				}
			}
		} finally {
			DBUtil.closeQuietly(rs);
			DBUtil.closeQuietly(stmt);
		}

		Map<Long, DiscoverV2Protos.Item.Count> resultMap = new TreeMap<Long, DiscoverV2Protos.Item.Count>();
		for (Entry<Long, DiscoverV2Protos.Item.Count.Builder> entry : resultBuilderMap.entrySet()) {
			resultMap.put(entry.getKey(), entry.getValue().build());
		}

		return resultMap;
	}

	public static Map<Long, DiscoverV2Protos.Item.User> getItemUser(Connection conn, long companyId, Collection<Long> itemIds, long userId) throws SQLException {
		if (itemIds.isEmpty()) {
			return Collections.emptyMap();
		}

		Map<Long, DiscoverV2Protos.Item.User.Builder> resultBuilderMap = new TreeMap<Long, DiscoverV2Protos.Item.User.Builder>();
		for (Long itemId : itemIds) {
			if (!resultBuilderMap.containsKey(itemId)) {
				resultBuilderMap.put(itemId, DiscoverV2Protos.Item.User.newBuilder()
						.setUserId(userId)
						.setIsComment(false)
						.setIsScore(false)
						.setIsLearn(false));
			}
		}

		String itemIdStr = DBUtil.COMMA_JOINER.join(resultBuilderMap.keySet());

		StringBuilder sql = new StringBuilder();

		sql.append("SELECT item_id FROM weizhu_discover_v2_item_learn WHERE company_id = ").append(companyId).append(" AND item_id IN (")
				.append(itemIdStr)
				.append(") AND user_id = ")
				.append(userId)
				.append(" AND learn_cnt > 0; ");
		sql.append("SELECT DISTINCT(item_id) AS item_id FROM weizhu_discover_v2_item_comment WHERE company_id = ").append(companyId).append(" AND item_id IN (")
				.append(itemIdStr)
				.append(") AND user_id = ")
				.append(userId)
				.append(" AND is_delete = 0; ");
		sql.append("SELECT item_id FROM weizhu_discover_v2_item_score WHERE company_id = ").append(companyId).append(" AND item_id IN (")
				.append(itemIdStr)
				.append(") AND user_id = ")
				.append(userId)
				.append("; ");
		sql.append("SELECT item_id FROM weizhu_discover_v2_item_like WHERE company_id = ").append(companyId).append(" AND item_id IN (")
				.append(itemIdStr)
				.append(") AND user_id = ")
				.append(userId)
				.append("; ");

		Statement stmt = null;
		ResultSet rs = null;
		try {
			stmt = conn.createStatement();

			stmt.execute(sql.toString());
			rs = stmt.getResultSet();
			while (rs.next()) {
				long itemId = rs.getLong("item_id");

				DiscoverV2Protos.Item.User.Builder builder = resultBuilderMap.get(itemId);
				if (builder != null) {
					builder.setIsLearn(true);
				}
			}

			DBUtil.closeQuietly(rs);
			rs = null;

			stmt.getMoreResults();
			rs = stmt.getResultSet();

			while (rs.next()) {
				long itemId = rs.getLong("item_id");

				DiscoverV2Protos.Item.User.Builder builder = resultBuilderMap.get(itemId);
				if (builder != null) {
					builder.setIsComment(true);
				}
			}

			DBUtil.closeQuietly(rs);
			rs = null;

			stmt.getMoreResults();
			rs = stmt.getResultSet();

			while (rs.next()) {
				long itemId = rs.getLong("item_id");

				DiscoverV2Protos.Item.User.Builder builder = resultBuilderMap.get(itemId);
				if (builder != null) {
					builder.setIsScore(true);
				}
			}

			DBUtil.closeQuietly(rs);
			rs = null;

			stmt.getMoreResults();
			rs = stmt.getResultSet();

			while (rs.next()) {
				long itemId = rs.getLong("item_id");

				DiscoverV2Protos.Item.User.Builder builder = resultBuilderMap.get(itemId);
				if (builder != null) {
					builder.setIsLike(true);
				}
			}
		} finally {
			DBUtil.closeQuietly(rs);
			DBUtil.closeQuietly(stmt);
		}

		Map<Long, DiscoverV2Protos.Item.User> resultMap = new TreeMap<Long, DiscoverV2Protos.Item.User>();
		for (Entry<Long, DiscoverV2Protos.Item.User.Builder> entry : resultBuilderMap.entrySet()) {
			resultMap.put(entry.getKey(), entry.getValue().build());
		}

		return resultMap;
	}

	private static final ProtobufMapper<DiscoverV2Protos.ItemLearn> ITEM_LEARN_MAPPER = ProtobufMapper.createMapper(DiscoverV2Protos.ItemLearn.getDefaultInstance(),
			"item_id",
			"user_id",
			"learn_time",
			"learn_duration",
			"learn_cnt");

	public static Map<Long, DiscoverV2DAOProtos.ItemLearnList> getItemLearnList(Connection conn, long companyId, Collection<Long> itemIds, int size)
			throws SQLException {
		if (itemIds.isEmpty() || size <= 0) {
			return Collections.emptyMap();
		}

		final Set<Long> itemIdSet = new TreeSet<Long>(itemIds);

		StringBuilder sql = new StringBuilder();
		for (Long itemId : itemIdSet) {
			sql.append("SELECT * FROM weizhu_discover_v2_item_learn WHERE company_id = ").append(companyId).append(" AND item_id = ").append(itemId);
			sql.append(" ORDER BY learn_time DESC, user_id DESC LIMIT ").append(size).append("; ");
		}

		Statement stmt = null;
		ResultSet rs = null;
		try {
			stmt = conn.createStatement();
			stmt.execute(sql.toString());

			Map<Long, DiscoverV2DAOProtos.ItemLearnList.Builder> resultBuilderMap = new TreeMap<Long, DiscoverV2DAOProtos.ItemLearnList.Builder>();
			DiscoverV2Protos.ItemLearn.Builder tmpBuilder = DiscoverV2Protos.ItemLearn.newBuilder();
			for (int i = 0; i < itemIdSet.size(); ++i) {
				if (i > 0) {
					stmt.getMoreResults();
				}
				rs = stmt.getResultSet();

				while (rs.next()) {
					tmpBuilder.clear();

					DiscoverV2Protos.ItemLearn itemLearn = ITEM_LEARN_MAPPER.mapToItem(rs, tmpBuilder).build();

					DiscoverV2DAOProtos.ItemLearnList.Builder resultBuilder = resultBuilderMap.get(itemLearn.getItemId());
					if (resultBuilder == null) {
						resultBuilder = DiscoverV2DAOProtos.ItemLearnList.newBuilder();
						resultBuilderMap.put(itemLearn.getItemId(), resultBuilder);
					}

					resultBuilder.addItemLearn(itemLearn);
				}

				DBUtil.closeQuietly(rs);
				rs = null;
			}

			Map<Long, DiscoverV2DAOProtos.ItemLearnList> resultMap = new TreeMap<Long, DiscoverV2DAOProtos.ItemLearnList>();
			for (Entry<Long, DiscoverV2DAOProtos.ItemLearnList.Builder> entry : resultBuilderMap.entrySet()) {
				resultMap.put(entry.getKey(), entry.getValue().build());
			}

			return resultMap;
		} finally {
			DBUtil.closeQuietly(rs);
			DBUtil.closeQuietly(stmt);
		}
	}

	public static DiscoverV2DAOProtos.ItemLearnList getItemLearnList(Connection conn, long companyId, long itemId, @Nullable DiscoverV2Protos.ItemLearn lastIndex,
			int size) throws SQLException {
		if (size <= 0) {
			return DiscoverV2DAOProtos.ItemLearnList.getDefaultInstance();
		}

		StringBuilder sql = new StringBuilder();
		sql.append("SELECT * FROM weizhu_discover_v2_item_learn WHERE company_id = ").append(companyId).append(" AND item_id = ").append(itemId);
		if (lastIndex != null) {
			sql.append(" AND (learn_time < ").append(lastIndex.getLearnTime());
			sql.append(" OR (learn_time = ").append(lastIndex.getLearnTime());
			sql.append(" AND user_id < ").append(lastIndex.getUserId());
			sql.append("))");
		}
		sql.append(" ORDER BY learn_time DESC, user_id DESC LIMIT ").append(size).append("; ");

		Statement stmt = null;
		ResultSet rs = null;
		try {
			stmt = conn.createStatement();
			rs = stmt.executeQuery(sql.toString());

			DiscoverV2DAOProtos.ItemLearnList.Builder resultBuilder = DiscoverV2DAOProtos.ItemLearnList.newBuilder();
			DiscoverV2Protos.ItemLearn.Builder tmpBuilder = DiscoverV2Protos.ItemLearn.newBuilder();

			while (rs.next()) {
				tmpBuilder.clear();

				resultBuilder.addItemLearn(ITEM_LEARN_MAPPER.mapToItem(rs, tmpBuilder).build());
			}

			return resultBuilder.build();
		} finally {
			DBUtil.closeQuietly(rs);
			DBUtil.closeQuietly(stmt);
		}
	}

	public static DiscoverV2DAOProtos.ItemLearnList getUserLearnList(Connection conn, long companyId, long userId, @Nullable DiscoverV2Protos.ItemLearn lastIndex,
			int size) throws SQLException {
		if (size <= 0) {
			return DiscoverV2DAOProtos.ItemLearnList.getDefaultInstance();
		}

		StringBuilder sql = new StringBuilder();
		sql.append("SELECT * FROM weizhu_discover_v2_item_learn WHERE company_id = ").append(companyId).append(" AND user_id = ").append(userId);
		if (lastIndex != null) {
			sql.append(" AND (learn_time < ").append(lastIndex.getLearnTime());
			sql.append(" OR (learn_time = ").append(lastIndex.getLearnTime());
			sql.append(" AND item_id < ").append(lastIndex.getItemId());
			sql.append("))");
		}
		sql.append(" ORDER BY learn_time DESC, item_id DESC LIMIT ").append(size).append("; ");

		Statement stmt = null;
		ResultSet rs = null;
		try {
			stmt = conn.createStatement();
			rs = stmt.executeQuery(sql.toString());

			DiscoverV2DAOProtos.ItemLearnList.Builder resultBuilder = DiscoverV2DAOProtos.ItemLearnList.newBuilder();
			DiscoverV2Protos.ItemLearn.Builder tmpBuilder = DiscoverV2Protos.ItemLearn.newBuilder();

			while (rs.next()) {
				tmpBuilder.clear();

				resultBuilder.addItemLearn(ITEM_LEARN_MAPPER.mapToItem(rs, tmpBuilder).build());
			}

			return resultBuilder.build();
		} finally {
			DBUtil.closeQuietly(rs);
			DBUtil.closeQuietly(stmt);
		}
	}

	public static DiscoverV2Protos.ItemLearn getUserItemLearn(Connection conn, long companyId, long userId, long itemId) throws SQLException {
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try {
			pstmt = conn.prepareStatement("SELECT * FROM weizhu_discover_v2_item_learn WHERE company_id = ? AND user_id = ? AND item_id = ?; ");
			pstmt.setLong(1, companyId);
			pstmt.setLong(2, userId);
			pstmt.setLong(3, itemId);

			rs = pstmt.executeQuery();

			if (rs.next()) {
				return ITEM_LEARN_MAPPER.mapToItem(rs, DiscoverV2Protos.ItemLearn.newBuilder()).build();
			} else {
				return null;
			}
		} finally {
			DBUtil.closeQuietly(rs);
			DBUtil.closeQuietly(pstmt);
		}
	}

	public static class ItemLearnUpdateInfo {
		int newLearnCnt;
		int newLearnUserCnt;
		DiscoverV2Protos.ItemLearn userItemLearn;

		ItemLearnUpdateInfo() {
			this.newLearnCnt = 0;
			this.newLearnUserCnt = 0;
			this.userItemLearn = null;
		}
	}

	/**
	 * 默认所有更新的ItemLearn 的userId 都相同 return itemId -> 更新信息
	 */
	public static Map<Long, ItemLearnUpdateInfo> updateItemLearn(Connection conn, long companyId, long userId, List<DiscoverV2Protos.ItemLearn> itemLearnList,
			boolean isReport) throws SQLException {
		if (itemLearnList.isEmpty()) {
			return Collections.emptyMap();
		}

		Map<Long, ItemLearnUpdateInfo> resultMap = new TreeMap<Long, ItemLearnUpdateInfo>();
		for (DiscoverV2Protos.ItemLearn itemLearn : itemLearnList) {
			if (itemLearn.getUserId() == userId && itemLearn.getLearnDuration() > 0) {
				ItemLearnUpdateInfo itemLearnUpdateInfo = resultMap.get(itemLearn.getItemId());
				if (itemLearnUpdateInfo == null) {
					itemLearnUpdateInfo = new ItemLearnUpdateInfo();
					resultMap.put(itemLearn.getItemId(), itemLearnUpdateInfo);
				}
				itemLearnUpdateInfo.newLearnCnt++;
			}
		}

		if (resultMap.isEmpty()) {
			return Collections.emptyMap();
		}

		PreparedStatement pstmt = null;
		try {
			pstmt = conn.prepareStatement("INSERT IGNORE INTO weizhu_discover_v2_item_learn (company_id, item_id, user_id, learn_time, learn_duration) VALUES (?, ?, ?, 0, 0); ");

			for (Long itemId : resultMap.keySet()) {
				pstmt.setLong(1, companyId);
				pstmt.setLong(2, itemId);
				pstmt.setLong(3, userId);

				pstmt.addBatch();
			}

			int[] rets = pstmt.executeBatch();

			int i = 0;
			for (ItemLearnUpdateInfo itemLearnUpdateInfo : resultMap.values()) {
				int ret = rets[i];

				if (ret > 0) {
					itemLearnUpdateInfo.newLearnUserCnt = 1;
				}

				i++;
			}
		} finally {
			DBUtil.closeQuietly(pstmt);
		}

		pstmt = null;
		try {
			pstmt = conn.prepareStatement("UPDATE weizhu_discover_v2_item_learn SET learn_time = IF(learn_time < ?, ?, learn_time), learn_duration = learn_duration + ?, learn_cnt = learn_cnt + 1 WHERE company_id = ? AND item_id = ? AND user_id = ?; ");

			for (DiscoverV2Protos.ItemLearn itemLearn : itemLearnList) {
				if (itemLearn.getUserId() == userId && itemLearn.getLearnDuration() > 0) {
					pstmt.setInt(1, itemLearn.getLearnTime());
					pstmt.setInt(2, itemLearn.getLearnTime());
					pstmt.setInt(3, itemLearn.getLearnDuration());
					pstmt.setLong(4, companyId);
					pstmt.setLong(5, itemLearn.getItemId());
					pstmt.setLong(6, itemLearn.getUserId());

					pstmt.addBatch();
				}
			}

			pstmt.executeBatch();
		} finally {
			DBUtil.closeQuietly(pstmt);
		}

		pstmt = null;
		try {
			pstmt = conn.prepareStatement("INSERT INTO weizhu_discover_v2_item_learn_log (company_id, log_id, item_id, user_id, learn_time, learn_duration, is_report) VALUES (?, NULL, ?, ?, ?, ?, ?); ");

			for (DiscoverV2Protos.ItemLearn itemLearn : itemLearnList) {
				if (itemLearn.getUserId() == userId && itemLearn.getLearnDuration() > 0) {
					pstmt.setLong(1, companyId);
					pstmt.setLong(2, itemLearn.getItemId());
					pstmt.setLong(3, itemLearn.getUserId());
					pstmt.setInt(4, itemLearn.getLearnTime());
					pstmt.setInt(5, itemLearn.getLearnDuration());
					pstmt.setBoolean(6, isReport);

					pstmt.addBatch();
				}
			}

			pstmt.executeBatch();
		} finally {
			DBUtil.closeQuietly(pstmt);
		}

		StringBuilder sql = new StringBuilder();
		sql.append("SELECT * FROM weizhu_discover_v2_item_learn WHERE company_id = ").append(companyId).append(" AND user_id = ").append(userId);
		sql.append(" AND item_id IN (");
		DBUtil.COMMA_JOINER.appendTo(sql, resultMap.keySet());
		sql.append(") AND learn_duration > 0 AND learn_cnt > 0; ");

		Statement stmt = null;
		ResultSet rs = null;
		try {
			stmt = conn.createStatement();
			rs = stmt.executeQuery(sql.toString());

			DiscoverV2Protos.ItemLearn.Builder tmpBuilder = DiscoverV2Protos.ItemLearn.newBuilder();
			while (rs.next()) {
				tmpBuilder.clear();

				DiscoverV2Protos.ItemLearn userItemLearn = ITEM_LEARN_MAPPER.mapToItem(rs, tmpBuilder).build();

				ItemLearnUpdateInfo itemLearnUpdateInfo = resultMap.get(userItemLearn.getItemId());
				if (itemLearnUpdateInfo != null) {
					itemLearnUpdateInfo.userItemLearn = userItemLearn;
				}
			}
		} finally {
			DBUtil.closeQuietly(rs);
			DBUtil.closeQuietly(stmt);
		}

		return resultMap;
	}

	private static final ProtobufMapper<DiscoverV2Protos.ItemComment> ITEM_COMMENT_MAPPER = ProtobufMapper.createMapper(DiscoverV2Protos.ItemComment.getDefaultInstance(),
			"comment_id",
			"item_id",
			"user_id",
			"comment_time",
			"comment_text",
			"is_delete");

	public static Map<Long, DiscoverV2DAOProtos.ItemCommentList> getItemCommentList(Connection conn, long companyId, Collection<Long> itemIds,
			@Nullable Boolean isDelete, int size) throws SQLException {
		if (itemIds.isEmpty() || size <= 0) {
			return Collections.emptyMap();
		}

		final Set<Long> itemIdSet = new TreeSet<Long>(itemIds);

		StringBuilder sql = new StringBuilder();
		for (Long itemId : itemIdSet) {
			sql.append("SELECT * FROM weizhu_discover_v2_item_comment WHERE company_id = ").append(companyId).append(" AND item_id = ").append(itemId);
			if (isDelete != null) {
				sql.append(" AND is_delete = ").append(isDelete ? 1 : 0);
			}
			sql.append(" ORDER BY comment_time DESC, comment_id DESC LIMIT ").append(size).append("; ");
		}

		Statement stmt = null;
		ResultSet rs = null;
		try {
			stmt = conn.createStatement();
			stmt.execute(sql.toString());

			Map<Long, DiscoverV2DAOProtos.ItemCommentList.Builder> resultBuilderMap = new TreeMap<Long, DiscoverV2DAOProtos.ItemCommentList.Builder>();
			DiscoverV2Protos.ItemComment.Builder tmpBuilder = DiscoverV2Protos.ItemComment.newBuilder();
			for (int i = 0; i < itemIdSet.size(); ++i) {
				if (i > 0) {
					stmt.getMoreResults();
				}
				rs = stmt.getResultSet();

				while (rs.next()) {
					tmpBuilder.clear();

					DiscoverV2Protos.ItemComment itemComment = ITEM_COMMENT_MAPPER.mapToItem(rs, tmpBuilder).build();

					DiscoverV2DAOProtos.ItemCommentList.Builder resultBuilder = resultBuilderMap.get(itemComment.getItemId());
					if (resultBuilder == null) {
						resultBuilder = DiscoverV2DAOProtos.ItemCommentList.newBuilder();
						resultBuilderMap.put(itemComment.getItemId(), resultBuilder);
					}

					resultBuilder.addItemComment(itemComment);
				}

				DBUtil.closeQuietly(rs);
				rs = null;
			}

			Map<Long, DiscoverV2DAOProtos.ItemCommentList> resultMap = new TreeMap<Long, DiscoverV2DAOProtos.ItemCommentList>();
			for (Entry<Long, DiscoverV2DAOProtos.ItemCommentList.Builder> entry : resultBuilderMap.entrySet()) {
				resultMap.put(entry.getKey(), entry.getValue().build());
			}

			return resultMap;
		} finally {
			DBUtil.closeQuietly(rs);
			DBUtil.closeQuietly(stmt);
		}
	}

	public static DiscoverV2DAOProtos.ItemCommentList getItemCommentList(Connection conn, long companyId, long itemId, @Nullable Boolean isDelete,
			@Nullable DiscoverV2Protos.ItemComment lastIndex, int size) throws SQLException {
		if (size <= 0) {
			return DiscoverV2DAOProtos.ItemCommentList.getDefaultInstance();
		}

		StringBuilder sql = new StringBuilder();
		sql.append("SELECT * FROM weizhu_discover_v2_item_comment WHERE company_id = ").append(companyId).append(" AND item_id = ").append(itemId);
		if (isDelete != null) {
			sql.append(" AND is_delete = ").append(isDelete ? 1 : 0);
		}
		if (lastIndex != null) {
			sql.append(" AND (comment_time < ").append(lastIndex.getCommentTime());
			sql.append(" OR (comment_time = ").append(lastIndex.getCommentTime());
			sql.append(" AND comment_id < ").append(lastIndex.getCommentId());
			sql.append("))");
		}
		sql.append(" ORDER BY comment_time DESC, comment_id DESC LIMIT ").append(size).append("; ");

		Statement stmt = null;
		ResultSet rs = null;
		try {
			stmt = conn.createStatement();
			rs = stmt.executeQuery(sql.toString());

			DiscoverV2DAOProtos.ItemCommentList.Builder resultBuilder = DiscoverV2DAOProtos.ItemCommentList.newBuilder();
			DiscoverV2Protos.ItemComment.Builder tmpBuilder = DiscoverV2Protos.ItemComment.newBuilder();

			while (rs.next()) {
				tmpBuilder.clear();

				resultBuilder.addItemComment(ITEM_COMMENT_MAPPER.mapToItem(rs, tmpBuilder).build());
			}

			return resultBuilder.build();
		} finally {
			DBUtil.closeQuietly(rs);
			DBUtil.closeQuietly(stmt);
		}
	}

	public static DiscoverV2DAOProtos.ItemCommentList getUserCommentList(Connection conn, long companyId, long userId, @Nullable Boolean isDelete,
			@Nullable DiscoverV2Protos.ItemComment lastIndex, int size) throws SQLException {
		if (size <= 0) {
			return DiscoverV2DAOProtos.ItemCommentList.getDefaultInstance();
		}

		StringBuilder sql = new StringBuilder();
		sql.append("SELECT * FROM weizhu_discover_v2_item_comment WHERE company_id = ").append(companyId).append(" AND user_id = ").append(userId);
		if (isDelete != null) {
			sql.append(" AND is_delete = ").append(isDelete ? 1 : 0);
		}
		if (lastIndex != null) {
			sql.append(" AND (comment_time < ").append(lastIndex.getCommentTime());
			sql.append(" OR (comment_time = ").append(lastIndex.getCommentTime());
			sql.append(" AND comment_id < ").append(lastIndex.getCommentId());
			sql.append("))");
		}
		sql.append(" ORDER BY comment_time DESC, comment_id DESC LIMIT ").append(size).append("; ");

		Statement stmt = null;
		ResultSet rs = null;
		try {
			stmt = conn.createStatement();
			rs = stmt.executeQuery(sql.toString());

			DiscoverV2DAOProtos.ItemCommentList.Builder resultBuilder = DiscoverV2DAOProtos.ItemCommentList.newBuilder();
			DiscoverV2Protos.ItemComment.Builder tmpBuilder = DiscoverV2Protos.ItemComment.newBuilder();

			while (rs.next()) {
				tmpBuilder.clear();

				resultBuilder.addItemComment(ITEM_COMMENT_MAPPER.mapToItem(rs, tmpBuilder).build());
			}

			return resultBuilder.build();
		} finally {
			DBUtil.closeQuietly(rs);
			DBUtil.closeQuietly(stmt);
		}
	}

	public static DiscoverV2Protos.ItemComment getItemComment(Connection conn, long companyId, long itemId, long commentId) throws SQLException {
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try {
			pstmt = conn.prepareStatement("SELECT * FROM weizhu_discover_v2_item_comment WHERE company_id = ? AND item_id = ? AND comment_id = ?; ");
			pstmt.setLong(1, companyId);
			pstmt.setLong(2, itemId);
			pstmt.setLong(3, commentId);

			rs = pstmt.executeQuery();

			if (rs.next()) {
				return ITEM_COMMENT_MAPPER.mapToItem(rs, DiscoverV2Protos.ItemComment.newBuilder()).build();
			} else {
				return null;
			}
		} finally {
			DBUtil.closeQuietly(rs);
			DBUtil.closeQuietly(pstmt);
		}
	}

	public static long insertItemComment(Connection conn, long companyId, DiscoverV2Protos.ItemComment itemComment) throws SQLException {
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try {
			pstmt = conn.prepareStatement("INSERT INTO weizhu_discover_v2_item_comment (company_id, comment_id, item_id, user_id, comment_time, comment_text, is_delete) VALUES (?, NULL, ?, ?, ?, ?, ?); ",
					Statement.RETURN_GENERATED_KEYS);
			pstmt.setLong(1, companyId);
			DBUtil.set(pstmt, 2, itemComment.hasItemId(), itemComment.getItemId());
			DBUtil.set(pstmt, 3, itemComment.hasUserId(), itemComment.getUserId());
			DBUtil.set(pstmt, 4, itemComment.hasCommentTime(), itemComment.getCommentTime());
			DBUtil.set(pstmt, 5, itemComment.hasCommentText(), itemComment.getCommentText());
			DBUtil.set(pstmt, 6, itemComment.hasIsDelete(), itemComment.getIsDelete());

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

	public static boolean updateItemCommentIsDelete(Connection conn, long companyId, long itemId, long commentId, boolean isDelete) throws SQLException {
		PreparedStatement pstmt = null;
		try {
			pstmt = conn.prepareStatement("UPDATE weizhu_discover_v2_item_comment SET is_delete = ? WHERE company_id = ? AND item_id = ? AND comment_id = ?; ");
			pstmt.setBoolean(1, isDelete);
			pstmt.setLong(2, companyId);
			pstmt.setLong(3, itemId);
			pstmt.setLong(4, commentId);

			return pstmt.executeUpdate() > 0;
		} finally {
			DBUtil.closeQuietly(pstmt);
		}
	}

	private static final ProtobufMapper<DiscoverV2Protos.ItemScore> ITEM_SCORE_MAPPER = ProtobufMapper.createMapper(DiscoverV2Protos.ItemScore.getDefaultInstance(),
			"item_id",
			"user_id",
			"score_time",
			"score_number");

	public static Map<Long, DiscoverV2DAOProtos.ItemScoreList> getItemScoreList(Connection conn, long companyId, Collection<Long> itemIds, int size)
			throws SQLException {
		if (itemIds.isEmpty() || size <= 0) {
			return Collections.emptyMap();
		}

		final Set<Long> itemIdSet = new TreeSet<Long>(itemIds);

		StringBuilder sql = new StringBuilder();
		for (Long itemId : itemIdSet) {
			sql.append("SELECT * FROM weizhu_discover_v2_item_score WHERE company_id = ").append(companyId).append(" AND item_id = ").append(itemId);
			sql.append(" ORDER BY score_time DESC, user_id DESC LIMIT ").append(size).append("; ");
		}

		Statement stmt = null;
		ResultSet rs = null;
		try {
			stmt = conn.createStatement();
			stmt.execute(sql.toString());

			Map<Long, DiscoverV2DAOProtos.ItemScoreList.Builder> resultBuilderMap = new TreeMap<Long, DiscoverV2DAOProtos.ItemScoreList.Builder>();
			DiscoverV2Protos.ItemScore.Builder tmpBuilder = DiscoverV2Protos.ItemScore.newBuilder();
			for (int i = 0; i < itemIdSet.size(); ++i) {
				if (i > 0) {
					stmt.getMoreResults();
				}
				rs = stmt.getResultSet();

				while (rs.next()) {
					tmpBuilder.clear();

					DiscoverV2Protos.ItemScore itemScore = ITEM_SCORE_MAPPER.mapToItem(rs, tmpBuilder).build();

					DiscoverV2DAOProtos.ItemScoreList.Builder resultBuilder = resultBuilderMap.get(itemScore.getItemId());
					if (resultBuilder == null) {
						resultBuilder = DiscoverV2DAOProtos.ItemScoreList.newBuilder();
						resultBuilderMap.put(itemScore.getItemId(), resultBuilder);
					}

					resultBuilder.addItemScore(itemScore);
				}

				DBUtil.closeQuietly(rs);
				rs = null;
			}

			Map<Long, DiscoverV2DAOProtos.ItemScoreList> resultMap = new TreeMap<Long, DiscoverV2DAOProtos.ItemScoreList>();
			for (Entry<Long, DiscoverV2DAOProtos.ItemScoreList.Builder> entry : resultBuilderMap.entrySet()) {
				resultMap.put(entry.getKey(), entry.getValue().build());
			}

			return resultMap;
		} finally {
			DBUtil.closeQuietly(rs);
			DBUtil.closeQuietly(stmt);
		}
	}

	public static DiscoverV2DAOProtos.ItemScoreList getItemScoreList(Connection conn, long companyId, long itemId, @Nullable DiscoverV2Protos.ItemScore lastIndex,
			int size) throws SQLException {
		if (size <= 0) {
			return DiscoverV2DAOProtos.ItemScoreList.getDefaultInstance();
		}

		StringBuilder sql = new StringBuilder();
		sql.append("SELECT * FROM weizhu_discover_v2_item_score WHERE company_id = ").append(companyId).append(" AND item_id = ").append(itemId);
		if (lastIndex != null) {
			sql.append(" AND (score_time < ").append(lastIndex.getScoreTime());
			sql.append(" OR (score_time = ").append(lastIndex.getScoreTime());
			sql.append(" AND user_id < ").append(lastIndex.getUserId());
			sql.append("))");
		}
		sql.append(" ORDER BY score_time DESC, user_id DESC LIMIT ").append(size).append("; ");

		Statement stmt = null;
		ResultSet rs = null;
		try {
			stmt = conn.createStatement();
			rs = stmt.executeQuery(sql.toString());

			DiscoverV2DAOProtos.ItemScoreList.Builder resultBuilder = DiscoverV2DAOProtos.ItemScoreList.newBuilder();
			DiscoverV2Protos.ItemScore.Builder tmpBuilder = DiscoverV2Protos.ItemScore.newBuilder();

			while (rs.next()) {
				tmpBuilder.clear();

				resultBuilder.addItemScore(ITEM_SCORE_MAPPER.mapToItem(rs, tmpBuilder).build());
			}

			return resultBuilder.build();
		} finally {
			DBUtil.closeQuietly(rs);
			DBUtil.closeQuietly(stmt);
		}
	}

	public static DiscoverV2DAOProtos.ItemScoreList getUserScoreList(Connection conn, long companyId, long userId, @Nullable DiscoverV2Protos.ItemScore lastIndex,
			int size) throws SQLException {
		if (size <= 0) {
			return DiscoverV2DAOProtos.ItemScoreList.getDefaultInstance();
		}

		StringBuilder sql = new StringBuilder();
		sql.append("SELECT * FROM weizhu_discover_v2_item_score WHERE company_id = ").append(companyId).append(" AND user_id = ").append(userId);
		if (lastIndex != null) {
			sql.append(" AND (score_time < ").append(lastIndex.getScoreTime());
			sql.append(" OR (score_time = ").append(lastIndex.getScoreTime());
			sql.append(" AND item_id < ").append(lastIndex.getItemId());
			sql.append("))");
		}
		sql.append(" ORDER BY score_time DESC, item_id DESC LIMIT ").append(size).append("; ");

		Statement stmt = null;
		ResultSet rs = null;
		try {
			stmt = conn.createStatement();
			rs = stmt.executeQuery(sql.toString());

			DiscoverV2DAOProtos.ItemScoreList.Builder resultBuilder = DiscoverV2DAOProtos.ItemScoreList.newBuilder();
			DiscoverV2Protos.ItemScore.Builder tmpBuilder = DiscoverV2Protos.ItemScore.newBuilder();

			while (rs.next()) {
				tmpBuilder.clear();

				resultBuilder.addItemScore(ITEM_SCORE_MAPPER.mapToItem(rs, tmpBuilder).build());
			}

			return resultBuilder.build();
		} finally {
			DBUtil.closeQuietly(rs);
			DBUtil.closeQuietly(stmt);
		}
	}

	public static DiscoverV2Protos.ItemScore getUserItemScore(Connection conn, long companyId, long userId, long itemId) throws SQLException {
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try {
			pstmt = conn.prepareStatement("SELECT * FROM weizhu_discover_v2_item_score WHERE company_id = ? AND user_id = ? AND item_id = ?; ");
			pstmt.setLong(1, companyId);
			pstmt.setLong(2, userId);
			pstmt.setLong(3, itemId);

			rs = pstmt.executeQuery();

			if (rs.next()) {
				return ITEM_SCORE_MAPPER.mapToItem(rs, DiscoverV2Protos.ItemScore.newBuilder()).build();
			} else {
				return null;
			}
		} finally {
			DBUtil.closeQuietly(rs);
			DBUtil.closeQuietly(pstmt);
		}
	}

	public static DiscoverV2Protos.ItemScore getItemScore(Connection conn, long companyId, long itemId, long userId) throws SQLException {
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try {
			pstmt = conn.prepareStatement("SELECT * FROM weizhu_discover_v2_item_score WHERE company_id = ? AND item_id = ? AND user_id = ?; ");
			pstmt.setLong(1, companyId);
			pstmt.setLong(2, itemId);
			pstmt.setLong(3, userId);

			rs = pstmt.executeQuery();

			if (rs.next()) {
				return ITEM_SCORE_MAPPER.mapToItem(rs, DiscoverV2Protos.ItemScore.newBuilder()).build();
			} else {
				return null;
			}
		} finally {
			DBUtil.closeQuietly(rs);
			DBUtil.closeQuietly(pstmt);
		}
	}

	public static boolean insertItemScore(Connection conn, long companyId, DiscoverV2Protos.ItemScore itemScore) throws SQLException {
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try {
			pstmt = conn.prepareStatement("INSERT INTO weizhu_discover_v2_item_score (company_id, item_id, user_id, score_time, score_number) VALUES (?, ?, ?, ?, ?); ");

			pstmt.setLong(1, companyId);
			DBUtil.set(pstmt, 2, itemScore.hasItemId(), itemScore.getItemId());
			DBUtil.set(pstmt, 3, itemScore.hasUserId(), itemScore.getUserId());
			DBUtil.set(pstmt, 4, itemScore.hasScoreTime(), itemScore.getScoreTime());
			DBUtil.set(pstmt, 5, itemScore.hasScoreNumber(), itemScore.getScoreNumber());

			return pstmt.executeUpdate() > 0;
		} finally {
			DBUtil.closeQuietly(rs);
			DBUtil.closeQuietly(pstmt);
		}
	}

	private static final ProtobufMapper<DiscoverV2Protos.ItemLike> ITEM_LIKE_MAPPER = ProtobufMapper.createMapper(DiscoverV2Protos.ItemLike.getDefaultInstance(),
			"item_id",
			"user_id",
			"like_time");

	public static Map<Long, DiscoverV2DAOProtos.ItemLikeList> getItemLikeList(Connection conn, long companyId, Collection<Long> itemIds, int size)
			throws SQLException {
		if (itemIds.isEmpty() || size <= 0) {
			return Collections.emptyMap();
		}

		final Set<Long> itemIdSet = new TreeSet<Long>(itemIds);

		StringBuilder sql = new StringBuilder();
		for (Long itemId : itemIdSet) {
			sql.append("SELECT * FROM weizhu_discover_v2_item_like WHERE company_id = ").append(companyId).append(" AND item_id = ").append(itemId);
			sql.append(" ORDER BY like_time DESC, user_id DESC LIMIT ").append(size).append("; ");
		}

		Statement stmt = null;
		ResultSet rs = null;
		try {
			stmt = conn.createStatement();
			stmt.execute(sql.toString());

			Map<Long, DiscoverV2DAOProtos.ItemLikeList.Builder> resultBuilderMap = new TreeMap<Long, DiscoverV2DAOProtos.ItemLikeList.Builder>();
			DiscoverV2Protos.ItemLike.Builder tmpBuilder = DiscoverV2Protos.ItemLike.newBuilder();
			for (int i = 0; i < itemIdSet.size(); ++i) {
				if (i > 0) {
					stmt.getMoreResults();
				}
				rs = stmt.getResultSet();

				while (rs.next()) {
					tmpBuilder.clear();

					DiscoverV2Protos.ItemLike itemLike = ITEM_LIKE_MAPPER.mapToItem(rs, tmpBuilder).build();

					DiscoverV2DAOProtos.ItemLikeList.Builder resultBuilder = resultBuilderMap.get(itemLike.getItemId());
					if (resultBuilder == null) {
						resultBuilder = DiscoverV2DAOProtos.ItemLikeList.newBuilder();
						resultBuilderMap.put(itemLike.getItemId(), resultBuilder);
					}

					resultBuilder.addItemLike(itemLike);
				}

				DBUtil.closeQuietly(rs);
				rs = null;
			}

			Map<Long, DiscoverV2DAOProtos.ItemLikeList> resultMap = new TreeMap<Long, DiscoverV2DAOProtos.ItemLikeList>();
			for (Entry<Long, DiscoverV2DAOProtos.ItemLikeList.Builder> entry : resultBuilderMap.entrySet()) {
				resultMap.put(entry.getKey(), entry.getValue().build());
			}

			return resultMap;
		} finally {
			DBUtil.closeQuietly(rs);
			DBUtil.closeQuietly(stmt);
		}
	}

	public static DiscoverV2DAOProtos.ItemLikeList getItemLikeList(Connection conn, long companyId, long itemId, @Nullable DiscoverV2Protos.ItemLike lastIndex,
			int size) throws SQLException {
		if (size <= 0) {
			return DiscoverV2DAOProtos.ItemLikeList.getDefaultInstance();
		}

		StringBuilder sql = new StringBuilder();
		sql.append("SELECT * FROM weizhu_discover_v2_item_like WHERE company_id = ").append(companyId).append(" AND item_id = ").append(itemId);
		if (lastIndex != null) {
			sql.append(" AND (like_time < ").append(lastIndex.getLikeTime());
			sql.append(" OR (like_time = ").append(lastIndex.getLikeTime());
			sql.append(" AND user_id < ").append(lastIndex.getUserId());
			sql.append("))");
		}
		sql.append(" ORDER BY like_time DESC, user_id DESC LIMIT ").append(size).append("; ");

		Statement stmt = null;
		ResultSet rs = null;
		try {
			stmt = conn.createStatement();
			rs = stmt.executeQuery(sql.toString());

			DiscoverV2DAOProtos.ItemLikeList.Builder resultBuilder = DiscoverV2DAOProtos.ItemLikeList.newBuilder();
			DiscoverV2Protos.ItemLike.Builder tmpBuilder = DiscoverV2Protos.ItemLike.newBuilder();

			while (rs.next()) {
				tmpBuilder.clear();

				resultBuilder.addItemLike(ITEM_LIKE_MAPPER.mapToItem(rs, tmpBuilder).build());
			}

			return resultBuilder.build();
		} finally {
			DBUtil.closeQuietly(rs);
			DBUtil.closeQuietly(stmt);
		}
	}

	public static DiscoverV2DAOProtos.ItemLikeList getUserLikeList(Connection conn, long companyId, long userId, @Nullable DiscoverV2Protos.ItemLike lastIndex,
			int size) throws SQLException {
		if (size <= 0) {
			return DiscoverV2DAOProtos.ItemLikeList.getDefaultInstance();
		}

		StringBuilder sql = new StringBuilder();
		sql.append("SELECT * FROM weizhu_discover_v2_item_like WHERE company_id = ").append(companyId).append(" AND user_id = ").append(userId);
		if (lastIndex != null) {
			sql.append(" AND (like_time < ").append(lastIndex.getLikeTime());
			sql.append(" OR (like_time = ").append(lastIndex.getLikeTime());
			sql.append(" AND item_id < ").append(lastIndex.getItemId());
			sql.append("))");
		}
		sql.append(" ORDER BY like_time DESC, item_id DESC LIMIT ").append(size).append("; ");

		Statement stmt = null;
		ResultSet rs = null;
		try {
			stmt = conn.createStatement();
			rs = stmt.executeQuery(sql.toString());

			DiscoverV2DAOProtos.ItemLikeList.Builder resultBuilder = DiscoverV2DAOProtos.ItemLikeList.newBuilder();
			DiscoverV2Protos.ItemLike.Builder tmpBuilder = DiscoverV2Protos.ItemLike.newBuilder();

			while (rs.next()) {
				tmpBuilder.clear();

				resultBuilder.addItemLike(ITEM_LIKE_MAPPER.mapToItem(rs, tmpBuilder).build());
			}

			return resultBuilder.build();
		} finally {
			DBUtil.closeQuietly(rs);
			DBUtil.closeQuietly(stmt);
		}
	}

	public static DiscoverV2Protos.ItemLike getUserItemLike(Connection conn, long companyId, long userId, long itemId) throws SQLException {
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try {
			pstmt = conn.prepareStatement("SELECT * FROM weizhu_discover_v2_item_like WHERE company_id = ? AND user_id = ? AND item_id = ?; ");
			pstmt.setLong(1, companyId);
			pstmt.setLong(2, userId);
			pstmt.setLong(3, itemId);

			rs = pstmt.executeQuery();

			if (rs.next()) {
				return ITEM_LIKE_MAPPER.mapToItem(rs, DiscoverV2Protos.ItemLike.newBuilder()).build();
			} else {
				return null;
			}
		} finally {
			DBUtil.closeQuietly(rs);
			DBUtil.closeQuietly(pstmt);
		}
	}

	public static boolean insertItemLike(Connection conn, long companyId, DiscoverV2Protos.ItemLike itemLike) throws SQLException {
		PreparedStatement pstmt = null;
		try {
			pstmt = conn.prepareStatement("INSERT INTO weizhu_discover_v2_item_like (company_id, item_id, user_id, like_time) VALUES (?, ?, ?, ?); ");
			pstmt.setLong(1, companyId);
			DBUtil.set(pstmt, 2, itemLike.hasItemId(), itemLike.getItemId());
			DBUtil.set(pstmt, 3, itemLike.hasUserId(), itemLike.getUserId());
			DBUtil.set(pstmt, 4, itemLike.hasLikeTime(), itemLike.getLikeTime());

			return pstmt.executeUpdate() > 0;
		} finally {
			DBUtil.closeQuietly(pstmt);
		}
	}

	public static boolean deleteItemLike(Connection conn, long companyId, long itemId, long userId) throws SQLException {
		PreparedStatement pstmt = null;
		try {
			pstmt = conn.prepareStatement("DELETE FROM weizhu_discover_v2_item_like WHERE company_id = ? AND item_id = ? AND user_id = ?; ");
			pstmt.setLong(1, companyId);
			pstmt.setLong(2, itemId);
			pstmt.setLong(3, userId);

			return pstmt.executeUpdate() > 0;
		} finally {
			DBUtil.closeQuietly(pstmt);
		}
	}

	private static final ProtobufMapper<DiscoverV2Protos.ItemShare> ITEM_SHARE_MAPPER = ProtobufMapper.createMapper(DiscoverV2Protos.ItemShare.getDefaultInstance(),
			"item_id",
			"user_id",
			"share_time");

	public static Map<Long, DiscoverV2DAOProtos.ItemShareList> getItemShareList(Connection conn, long companyId, Collection<Long> itemIds, int size)
			throws SQLException {
		if (itemIds.isEmpty() || size <= 0) {
			return Collections.emptyMap();
		}

		final Set<Long> itemIdSet = new TreeSet<Long>(itemIds);

		StringBuilder sql = new StringBuilder();
		for (Long itemId : itemIdSet) {
			sql.append("SELECT * FROM weizhu_discover_v2_item_share WHERE company_id = ").append(companyId).append(" AND item_id = ").append(itemId);
			sql.append(" ORDER BY share_time DESC, user_id DESC LIMIT ").append(size).append("; ");
		}

		Statement stmt = null;
		ResultSet rs = null;
		try {
			stmt = conn.createStatement();
			stmt.execute(sql.toString());

			Map<Long, DiscoverV2DAOProtos.ItemShareList.Builder> resultBuilderMap = new TreeMap<Long, DiscoverV2DAOProtos.ItemShareList.Builder>();
			DiscoverV2Protos.ItemShare.Builder tmpBuilder = DiscoverV2Protos.ItemShare.newBuilder();
			for (int i = 0; i < itemIdSet.size(); ++i) {
				if (i > 0) {
					stmt.getMoreResults();
				}
				rs = stmt.getResultSet();

				while (rs.next()) {
					tmpBuilder.clear();

					DiscoverV2Protos.ItemShare itemShare = ITEM_SHARE_MAPPER.mapToItem(rs, tmpBuilder).build();

					DiscoverV2DAOProtos.ItemShareList.Builder resultBuilder = resultBuilderMap.get(itemShare.getItemId());
					if (resultBuilder == null) {
						resultBuilder = DiscoverV2DAOProtos.ItemShareList.newBuilder();
						resultBuilderMap.put(itemShare.getItemId(), resultBuilder);
					}

					resultBuilder.addItemShare(itemShare);
				}

				DBUtil.closeQuietly(rs);
				rs = null;
			}

			Map<Long, DiscoverV2DAOProtos.ItemShareList> resultMap = new TreeMap<Long, DiscoverV2DAOProtos.ItemShareList>();
			for (Entry<Long, DiscoverV2DAOProtos.ItemShareList.Builder> entry : resultBuilderMap.entrySet()) {
				resultMap.put(entry.getKey(), entry.getValue().build());
			}

			return resultMap;
		} finally {
			DBUtil.closeQuietly(rs);
			DBUtil.closeQuietly(stmt);
		}
	}
	
	public static DiscoverV2DAOProtos.ItemShareList getItemShareList(Connection conn, long companyId, long itemId, @Nullable DiscoverV2Protos.ItemShare lastIndex,
			int size) throws SQLException {
		if (size <= 0) {
			return DiscoverV2DAOProtos.ItemShareList.getDefaultInstance();
		}

		StringBuilder sql = new StringBuilder();
		sql.append("SELECT * FROM weizhu_discover_v2_item_share WHERE company_id = ").append(companyId).append(" AND item_id = ").append(itemId);
		if (lastIndex != null) {
			sql.append(" AND (share_time < ").append(lastIndex.getShareTime());
			sql.append(" OR (share_time = ").append(lastIndex.getShareTime());
			sql.append(" AND user_id < ").append(lastIndex.getUserId());
			sql.append("))");
		}
		sql.append(" ORDER BY share_time DESC, user_id DESC LIMIT ").append(size).append("; ");

		Statement stmt = null;
		ResultSet rs = null;
		try {
			stmt = conn.createStatement();
			rs = stmt.executeQuery(sql.toString());

			DiscoverV2DAOProtos.ItemShareList.Builder resultBuilder = DiscoverV2DAOProtos.ItemShareList.newBuilder();
			DiscoverV2Protos.ItemShare.Builder tmpBuilder = DiscoverV2Protos.ItemShare.newBuilder();

			while (rs.next()) {
				tmpBuilder.clear();

				resultBuilder.addItemShare(ITEM_SHARE_MAPPER.mapToItem(rs, tmpBuilder).build());
			}

			return resultBuilder.build();
		} finally {
			DBUtil.closeQuietly(rs);
			DBUtil.closeQuietly(stmt);
		}
	}
	

	public static DiscoverV2DAOProtos.ItemShareList getUserShareList(Connection conn, long companyId, long userId, @Nullable DiscoverV2Protos.ItemShare lastIndex,
			int size) throws SQLException {
		if (size <= 0) {
			return DiscoverV2DAOProtos.ItemShareList.getDefaultInstance();
		}

		StringBuilder sql = new StringBuilder();
		sql.append("SELECT * FROM weizhu_discover_v2_item_share WHERE company_id = ").append(companyId).append(" AND user_id = ").append(userId);
		if (lastIndex != null) {
			sql.append(" AND (share_time < ").append(lastIndex.getShareTime());
			sql.append(" OR (share_time = ").append(lastIndex.getShareTime());
			sql.append(" AND item_id < ").append(lastIndex.getItemId());
			sql.append("))");
		}
		sql.append(" ORDER BY share_time DESC, item_id DESC LIMIT ").append(size).append("; ");

		Statement stmt = null;
		ResultSet rs = null;
		try {
			stmt = conn.createStatement();
			rs = stmt.executeQuery(sql.toString());

			DiscoverV2DAOProtos.ItemShareList.Builder resultBuilder = DiscoverV2DAOProtos.ItemShareList.newBuilder();
			DiscoverV2Protos.ItemShare.Builder tmpBuilder = DiscoverV2Protos.ItemShare.newBuilder();

			while (rs.next()) {
				tmpBuilder.clear();

				resultBuilder.addItemShare(ITEM_SHARE_MAPPER.mapToItem(rs, tmpBuilder).build());
			}

			return resultBuilder.build();
		} finally {
			DBUtil.closeQuietly(rs);
			DBUtil.closeQuietly(stmt);
		}
	}

	public static DiscoverV2Protos.ItemShare getUserItemShare(Connection conn, long companyId, long userId, long itemId) throws SQLException {
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try {
			pstmt = conn.prepareStatement("SELECT * FROM weizhu_discover_v2_item_share WHERE company_id = ? AND user_id = ? AND item_id = ?; ");
			pstmt.setLong(1, companyId);
			pstmt.setLong(2, userId);
			pstmt.setLong(3, itemId);

			rs = pstmt.executeQuery();

			if (rs.next()) {
				return ITEM_SHARE_MAPPER.mapToItem(rs, DiscoverV2Protos.ItemShare.newBuilder()).build();
			} else {
				return null;
			}
		} finally {
			DBUtil.closeQuietly(rs);
			DBUtil.closeQuietly(pstmt);
		}
	}

	public static boolean insertItemShare(Connection conn, long companyId, DiscoverV2Protos.ItemShare itemShare) throws SQLException {
		PreparedStatement pstmt = null;
		try {
			pstmt = conn.prepareStatement("REPLACE INTO weizhu_discover_v2_item_share (company_id, item_id, user_id, share_time) VALUES (?, ?, ?, ?); ");
			pstmt.setLong(1, companyId);
			DBUtil.set(pstmt, 2, itemShare.hasItemId(), itemShare.getItemId());
			DBUtil.set(pstmt, 3, itemShare.hasUserId(), itemShare.getUserId());
			DBUtil.set(pstmt, 4, itemShare.hasShareTime(), itemShare.getShareTime());

			return pstmt.executeUpdate() > 0;
		} finally {
			DBUtil.closeQuietly(pstmt);
		}
	}
	
	static class UserDiscover {
		final int weekLearnCnt;
		final int weekLearnDuration;
		final int weekLearnItemCnt;
		final int weekCommentCnt;
		final int weekCommentItemCnt;
		final int weekScoreItemCnt;
		final int weekLikeItemCnt;
		final int weekShareItemCnt;

		UserDiscover(int weekLearnCnt, int weekLearnDuration, int weekLearnItemCnt, int weekCommentCnt, int weekCommentItemCnt, int weekScoreItemCnt,
				int weekLikeItemCnt, int weekShareItemCnt) {
			this.weekLearnCnt = weekLearnCnt;
			this.weekLearnDuration = weekLearnDuration;
			this.weekLearnItemCnt = weekLearnItemCnt;
			this.weekCommentCnt = weekCommentCnt;
			this.weekCommentItemCnt = weekCommentItemCnt;
			this.weekScoreItemCnt = weekScoreItemCnt;
			this.weekLikeItemCnt = weekLikeItemCnt;
			this.weekShareItemCnt = weekShareItemCnt;

		}
	}

	private static final String GET_USER_DISCOVER_SQL = "SELECT COUNT(log_id) AS learn_cnt, SUM(learn_duration) AS learn_duration, COUNT(DISTINCT item_id) AS learn_item_cnt FROM weizhu_discover_v2_item_learn_log WHERE company_id = ? AND user_id = ? AND learn_time > ?; "
			+ "SELECT COUNT(comment_id) AS comment_cnt, COUNT(DISTINCT item_id) AS comment_item_cnt FROM weizhu_discover_v2_item_comment WHERE company_id = ? AND user_id = ? AND comment_time > ? AND is_delete = 0; "
			+ "SELECT COUNT(item_id) AS score_item_cnt FROM weizhu_discover_v2_item_score WHERE company_id = ? AND user_id = ? AND score_time > ?; "
			+ "SELECT COUNT(item_id) AS like_item_cnt FROM weizhu_discover_v2_item_like WHERE company_id = ? AND user_id = ? AND like_time > ?; "
			+ "SELECT COUNT(item_id) AS share_item_cnt FROM weizhu_discover_v2_item_share WHERE company_id = ? AND user_id = ? AND share_time > ?; ";

	public static UserDiscover getUserDiscover(Connection conn, long companyId, long userId) throws SQLException {
		int timeBegin = (int) (System.currentTimeMillis() / 1000L) - 7 * 24 * 60 * 60;

		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try {
			pstmt = conn.prepareStatement(GET_USER_DISCOVER_SQL);
			pstmt.setLong(1, companyId);
			pstmt.setLong(2, userId);
			pstmt.setInt(3, timeBegin);
			pstmt.setLong(4, companyId);
			pstmt.setLong(5, userId);
			pstmt.setInt(6, timeBegin);
			pstmt.setLong(7, companyId);
			pstmt.setLong(8, userId);
			pstmt.setInt(9, timeBegin);
			pstmt.setLong(10, companyId);
			pstmt.setLong(11, userId);
			pstmt.setInt(12, timeBegin);
			pstmt.setLong(13, companyId);
			pstmt.setLong(14, userId);
			pstmt.setInt(15, timeBegin);
			
			pstmt.execute();
			rs = pstmt.getResultSet();

			final int weekLearnCnt;
			final int weekLearnDuration;
			final int weekLearnItemCnt;
			if (rs.next()) {
				weekLearnCnt = rs.getInt("learn_cnt");
				weekLearnDuration = rs.getInt("learn_duration");
				weekLearnItemCnt = rs.getInt("learn_item_cnt");
			} else {
				weekLearnCnt = 0;
				weekLearnDuration = 0;
				weekLearnItemCnt = 0;
			}

			DBUtil.closeQuietly(rs);
			rs = null;

			pstmt.getMoreResults();
			rs = pstmt.getResultSet();

			final int weekCommentCnt;
			final int weekCommentItemCnt;
			if (rs.next()) {
				weekCommentCnt = rs.getInt("comment_cnt");
				weekCommentItemCnt = rs.getInt("comment_item_cnt");
			} else {
				weekCommentCnt = 0;
				weekCommentItemCnt = 0;
			}

			DBUtil.closeQuietly(rs);
			rs = null;

			pstmt.getMoreResults();
			rs = pstmt.getResultSet();

			final int weekScoreItemCnt;
			if (rs.next()) {
				weekScoreItemCnt = rs.getInt("score_item_cnt");
			} else {
				weekScoreItemCnt = 0;
			}

			DBUtil.closeQuietly(rs);
			rs = null;

			pstmt.getMoreResults();
			rs = pstmt.getResultSet();

			final int weekLikeItemCnt;
			if (rs.next()) {
				weekLikeItemCnt = rs.getInt("like_item_cnt");
			} else {
				weekLikeItemCnt = 0;
			}

			DBUtil.closeQuietly(rs);
			rs = null;

			pstmt.getMoreResults();
			rs = pstmt.getResultSet();

			final int weekShareItemCnt;
			if (rs.next()) {
				weekShareItemCnt = rs.getInt("share_item_cnt");
			} else {
				weekShareItemCnt = 0;
			}
			
			return new UserDiscover(weekLearnCnt,
					weekLearnDuration,
					weekLearnItemCnt,
					weekCommentCnt,
					weekCommentItemCnt,
					weekScoreItemCnt,
					weekLikeItemCnt,
					weekShareItemCnt);
		} finally {
			DBUtil.closeQuietly(rs);
			DBUtil.closeQuietly(pstmt);
		}
	}

	public static List<Long> searchItemId(Connection conn, long companyId, String keyword, @Nullable Collection<DiscoverV2Protos.State> states, int size)
			throws SQLException {
		if (keyword.isEmpty() || (states != null && states.isEmpty()) || size <= 0) {
			return Collections.emptyList();
		}

		StringBuilder sql = new StringBuilder();
		sql.append("SELECT item_id FROM weizhu_discover_v2_item_base WHERE company_id = ").append(companyId).append(" AND item_name LIKE '%");
		sql.append(DBUtil.SQL_LIKE_STRING_ESCAPER.escape(keyword)).append("%'");
		if (states != null) {
			sql.append(" AND state IN ");
			appendStatesCondSql(sql, states);
		}
		sql.append(" ORDER BY create_time DESC LIMIT ").append(size).append("; ");

		Statement stmt = null;
		ResultSet rs = null;
		try {
			stmt = conn.createStatement();
			rs = stmt.executeQuery(sql.toString());

			List<Long> itemIdList = new ArrayList<Long>();
			while (rs.next()) {
				itemIdList.add(rs.getLong("item_id"));
			}
			return itemIdList;
		} finally {
			DBUtil.closeQuietly(rs);
			DBUtil.closeQuietly(stmt);
		}
	}

	public static List<Long> getItemIdList(Connection conn, long companyId, @Nullable Long lastItemId, int size) throws SQLException {
		if (size <= 0) {
			return Collections.emptyList();
		}

		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try {
			if (lastItemId == null) {
				pstmt = conn.prepareStatement("SELECT item_id FROM weizhu_discover_v2_item_base WHERE company_id = ? ORDER BY item_id DESC LIMIT ?; ");
				pstmt.setLong(1, companyId);
				pstmt.setInt(2, size);
			} else {
				pstmt = conn.prepareStatement("SELECT item_id FROM weizhu_discover_v2_item_base WHERE company_id = ? AND item_id < ? ORDER BY item_id DESC LIMIT ?; ");
				pstmt.setLong(1, companyId);
				pstmt.setLong(2, lastItemId);
				pstmt.setInt(3, size);
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

	private static void appendStatesCondSql(StringBuilder sql, Collection<DiscoverV2Protos.State> states) {
		if (states.isEmpty()) {
			throw new RuntimeException("states is empty");
		}
		sql.append("('");
		boolean isFirst = true;
		for (DiscoverV2Protos.State state : states) {
			if (isFirst) {
				isFirst = false;
			} else {
				sql.append("', '");
			}
			sql.append(DBUtil.SQL_STRING_ESCAPER.escape(state.name()));
		}

		sql.append("') ");
	}

	public static void setDiscoverHome(Connection conn, long companyId, 
			List<Integer> bannerOrderIdList, 
			List<Integer> moduleOrderIdList
			) throws SQLException {
		if (bannerOrderIdList.isEmpty() && moduleOrderIdList.isEmpty()) {
			return;
		}
		
		final String bannerOrderStr = DBUtil.COMMA_JOINER.join(bannerOrderIdList);
		final String moduleOrderStr = DBUtil.COMMA_JOINER.join(moduleOrderIdList);
		
		StringBuilder sqlBuilder = new StringBuilder();
		sqlBuilder.append("INSERT INTO weizhu_discover_v2_home (company_id, banner_order_str, module_order_str) VALUES (");
		sqlBuilder.append(companyId).append(", '");
		sqlBuilder.append(bannerOrderStr).append("', '");
		sqlBuilder.append(moduleOrderStr).append("') ON DUPLICATE KEY UPDATE ");
		
		if (!bannerOrderIdList.isEmpty()) {
			sqlBuilder.append(" banner_order_str = '");
			sqlBuilder.append(bannerOrderStr).append("'");
		}
		if (!bannerOrderIdList.isEmpty() && !moduleOrderIdList.isEmpty()) {
			sqlBuilder.append(", ");
		}
		if (!moduleOrderIdList.isEmpty()) {
			sqlBuilder.append(" module_order_str = '");
			sqlBuilder.append(moduleOrderStr).append("'");
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
	
	public static Map<Integer, DiscoverV2Protos.Banner> getBannerById(Connection conn, long companyId, 
			Collection<Integer> bannerIds,
			@Nullable Collection<DiscoverV2Protos.State> states
			) throws SQLException {
		if (bannerIds.isEmpty() || (states != null && states.isEmpty())) {
			return Collections.emptyMap();
		}
		
		StringBuilder sqlBuilder = new StringBuilder();
		sqlBuilder.append("SELECT * FROM weizhu_discover_v2_banner WHERE company_id = ");
		sqlBuilder.append(companyId).append(" AND banner_id IN (");
		DBUtil.COMMA_JOINER.appendTo(sqlBuilder, bannerIds);
		sqlBuilder.append(")");
		
		if (states != null) {
			sqlBuilder.append(" AND state IN ('");
			DBUtil.QUOTE_COMMA_JOINER.appendTo(sqlBuilder, Iterables.transform(Iterables.transform(states, Functions.toStringFunction()), DBUtil.SQL_STRING_ESCAPER.asFunction()));
			sqlBuilder.append("')");
		}

		sqlBuilder.append("; ");
		
		final String sql = sqlBuilder.toString();
		Statement stmt = null;
		ResultSet rs = null;
		try {
			stmt = conn.createStatement();
			rs = stmt.executeQuery(sql);

			Map<Integer, DiscoverV2Protos.Banner> bannerMap = new TreeMap<Integer, DiscoverV2Protos.Banner>();
			DiscoverV2Protos.Banner.Builder tmpBuilder = DiscoverV2Protos.Banner.newBuilder();
			while (rs.next()) {
				tmpBuilder.clear();
				
				DiscoverV2Protos.Banner banner = BANNER_MAPPER.mapToItem(rs, tmpBuilder).build();
				bannerMap.put(banner.getBannerId(), banner);
			}
			return bannerMap;
		} finally {
			DBUtil.closeQuietly(rs);
			DBUtil.closeQuietly(stmt);
		}
	}

	public static int insertBanner(Connection conn, long companyId, 
			DiscoverV2Protos.Banner banner
			) throws SQLException {
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try {
			pstmt = conn.prepareStatement("INSERT INTO weizhu_discover_v2_banner(company_id, banner_name, image_name, allow_model_id, item_id, `web_url.web_url`, `web_url.is_weizhu`, `app_uri.app_uri`, state, create_admin_id, create_time) VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) ;",
					Statement.RETURN_GENERATED_KEYS);
			DBUtil.set(pstmt, 1, companyId);
			DBUtil.set(pstmt, 2, banner.getBannerName());
			DBUtil.set(pstmt, 3, banner.getImageName());
			DBUtil.set(pstmt, 4, banner.hasAllowModelId(), banner.getAllowModelId());
			DBUtil.set(pstmt, 5, banner.hasItemId(), banner.getItemId());
			DBUtil.set(pstmt, 6, banner.hasWebUrl(), banner.getWebUrl().getWebUrl());
			DBUtil.set(pstmt, 7, banner.hasWebUrl(), banner.getWebUrl().getIsWeizhu());
			DBUtil.set(pstmt, 8, banner.hasAppUri(), banner.getAppUri().getAppUri());
			DBUtil.set(pstmt, 9, banner.getState());
			DBUtil.set(pstmt, 10, banner.hasCreateAdminId(), banner.getCreateAdminId());
			DBUtil.set(pstmt, 11, banner.hasCreateTime(), banner.getCreateTime());

			pstmt.executeUpdate();
			rs = pstmt.getGeneratedKeys();
			if (rs.next()) {
				return rs.getInt(1);
			} else {
				throw new RuntimeException("插入banner数据出错！");
			}
		} finally {
			DBUtil.closeQuietly(rs);
			DBUtil.closeQuietly(pstmt);
		}
	}

	public static void updateBanner(Connection conn, long companyId, 
			DiscoverV2Protos.Banner banner
			) throws SQLException {
		PreparedStatement pstmt = null;
		try {
			pstmt = conn.prepareStatement("UPDATE weizhu_discover_v2_banner SET banner_name = ?, image_name = ?, allow_model_id = ?, item_id = ?, `web_url.web_url` = ?, `web_url.is_weizhu` = ?, `app_uri.app_uri` = ?, update_admin_id = ?, update_time = ? WHERE company_id = ? AND banner_id = ?;");
			DBUtil.set(pstmt, 1, banner.getBannerName());
			DBUtil.set(pstmt, 2, banner.getImageName());
			DBUtil.set(pstmt, 3, banner.hasAllowModelId(),  banner.getAllowModelId());
			DBUtil.set(pstmt, 4, banner.hasItemId(),        banner.getItemId());
			DBUtil.set(pstmt, 5, banner.hasWebUrl(),        banner.getWebUrl().getWebUrl());
			DBUtil.set(pstmt, 6, banner.hasWebUrl(),        banner.getWebUrl().getIsWeizhu());
			DBUtil.set(pstmt, 7, banner.hasAppUri(),        banner.getAppUri().getAppUri());
			DBUtil.set(pstmt, 8, banner.hasUpdateAdminId(), banner.getUpdateAdminId());
			DBUtil.set(pstmt, 9, banner.hasUpdateTime(),    banner.getUpdateTime());
			DBUtil.set(pstmt, 10, companyId);
			DBUtil.set(pstmt, 11, banner.getBannerId());

			pstmt.executeUpdate();
		} finally {
			DBUtil.closeQuietly(pstmt);
		}
	}

	public static void updateBannerState(Connection conn, long companyId, 
			Collection<Integer> bannerIds, 
			@Nullable Collection<DiscoverV2Protos.State> states, 
			DiscoverV2Protos.State newState
			) throws SQLException {
		if (bannerIds.isEmpty() || (states != null && states.isEmpty())) {
			return;
		}
		
		StringBuilder sqlBuilder = new StringBuilder();
		sqlBuilder.append("UPDATE weizhu_discover_v2_banner SET state = '");
		sqlBuilder.append(DBUtil.SQL_STRING_ESCAPER.escape(newState.name()));
		sqlBuilder.append("' WHERE company_id = ");
		sqlBuilder.append(companyId);
		sqlBuilder.append(" AND banner_id IN (");
		DBUtil.COMMA_JOINER.appendTo(sqlBuilder, bannerIds);
		sqlBuilder.append(")");
		
		if (states != null) {
			sqlBuilder.append(" AND state IN ('");
			DBUtil.QUOTE_COMMA_JOINER.appendTo(sqlBuilder, Iterables.transform(Iterables.transform(states, Functions.toStringFunction()), DBUtil.SQL_STRING_ESCAPER.asFunction()));
			sqlBuilder.append("')");
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
	
	public static Map<Integer, DiscoverV2Protos.Module> getModuleById(Connection conn, long companyId, 
			Collection<Integer> moduleIds,
			@Nullable Collection<DiscoverV2Protos.State> states
			) throws SQLException {
		if (moduleIds.isEmpty() || (states != null && states.isEmpty())) {
			return Collections.emptyMap();
		}
		
		final String moduleIdStr = DBUtil.COMMA_JOINER.join(moduleIds);
		final String stateStr;
		if (states == null) {
			stateStr = null;
		} else {
			stateStr = DBUtil.QUOTE_COMMA_JOINER.join(Iterables.transform(Iterables.transform(states, Functions.toStringFunction()), DBUtil.SQL_STRING_ESCAPER.asFunction()));
		}
		
		StringBuilder sqlBuilder = new StringBuilder();
		sqlBuilder.append("SELECT * FROM weizhu_discover_v2_module_category WHERE company_id = ");
		sqlBuilder.append(companyId).append(" AND module_id IN (");
		sqlBuilder.append(moduleIdStr).append(")");
		if (stateStr != null) {
			sqlBuilder.append(" AND state IN ('");
			sqlBuilder.append(stateStr).append("')");
		}
		sqlBuilder.append(" ORDER BY category_id ASC; ");
		
		sqlBuilder.append("SELECT * FROM weizhu_discover_v2_module WHERE company_id = ");
		sqlBuilder.append(companyId).append(" AND module_id IN (");
		sqlBuilder.append(moduleIdStr).append(")");
		if (stateStr != null) {
			sqlBuilder.append(" AND state IN ('");
			sqlBuilder.append(stateStr).append("')");
		}
		sqlBuilder.append(" ORDER BY module_id ASC; ");

		final String sql = sqlBuilder.toString();
		Statement stmt = null;
		ResultSet rs = null;
		try {
			stmt = conn.createStatement();
			stmt.execute(sql);
			rs = stmt.getResultSet();

			// 获取模块分类
			Map<Integer, LinkedList<DiscoverV2Protos.Module.Category>> moduleCategoryMap = new TreeMap<Integer, LinkedList<DiscoverV2Protos.Module.Category>>();

			DiscoverV2Protos.Module.Category.Builder tmpCategoryBuilder = DiscoverV2Protos.Module.Category.newBuilder();
			while (rs.next()) {
				tmpCategoryBuilder.clear();

				DiscoverV2Protos.Module.Category category = MODULE_CATEGORY_MAPPER.mapToItem(rs, tmpCategoryBuilder).build();

				LinkedList<DiscoverV2Protos.Module.Category> list = moduleCategoryMap.get(category.getModuleId());
				if (list == null) {
					list = new LinkedList<DiscoverV2Protos.Module.Category>();
					moduleCategoryMap.put(category.getModuleId(), list);
				}
				list.add(category);
			}

			DBUtil.closeQuietly(rs);
			rs = null;
			stmt.getMoreResults();
			rs = stmt.getResultSet();

			Map<Integer, DiscoverV2Protos.Module> moduleMap = new TreeMap<Integer, DiscoverV2Protos.Module>();
			DiscoverV2Protos.Module.Builder tmpModuleBuilder = DiscoverV2Protos.Module.newBuilder();
			DiscoverV2Protos.Module.CategoryList.Builder tmpCategoryListBuilder = DiscoverV2Protos.Module.CategoryList.newBuilder();
			while (rs.next()) {
				tmpModuleBuilder.clear();
				tmpCategoryListBuilder.clear();

				MODULE_MAPPER.mapToItem(rs, tmpModuleBuilder);

				LinkedList<DiscoverV2Protos.Module.Category> categoryList = moduleCategoryMap.remove(tmpModuleBuilder.getModuleId());

				if (categoryList != null && !categoryList.isEmpty()) {
					String categoryOrderStr = rs.getString("category_order_str");
					if (categoryOrderStr != null) {
						for (String categoryIdStr : DBUtil.COMMA_SPLITTER.split(categoryOrderStr)) {
							int categoryId;
							try {
								categoryId = Integer.parseInt(categoryIdStr);
							} catch (NumberFormatException e) {
								continue;
							}

							Iterator<DiscoverV2Protos.Module.Category> it = categoryList.iterator();
							while (it.hasNext()) {
								DiscoverV2Protos.Module.Category category = it.next();
								if (categoryId == category.getCategoryId()) {
									tmpCategoryListBuilder.addCategory(category);
									it.remove();
									break;
								}
							}
						}
					}

					tmpCategoryListBuilder.addAllCategory(categoryList);

					tmpModuleBuilder.setCategoryList(tmpCategoryListBuilder.build());
				}
				moduleMap.put(tmpModuleBuilder.getModuleId(), tmpModuleBuilder.build());
			}
			return moduleMap;
		} finally {
			DBUtil.closeQuietly(rs);
			DBUtil.closeQuietly(stmt);
		}
	}

	public static int insertModule(Connection conn, long companyId, 
			String moduleName, 
			String imageName, 
			@Nullable Integer allowModelId,
			@Nullable DiscoverV2Protos.WebUrl webUrl, 
			@Nullable DiscoverV2Protos.AppUri appUri, 
			@Nullable Long promptDotTimestamp, 
			DiscoverV2Protos.State state,
			@Nullable Long createAdminId, 
			@Nullable Integer createTime
			) throws SQLException {
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try {
			pstmt = conn.prepareStatement("INSERT INTO weizhu_discover_v2_module (company_id, module_name, image_name, allow_model_id, `web_url.web_url`, `web_url.is_weizhu`, `app_uri.app_uri`, prompt_dot_timestamp, state, create_admin_id, create_time) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?); ",
					Statement.RETURN_GENERATED_KEYS);

			DBUtil.set(pstmt, 1, companyId);
			DBUtil.set(pstmt, 2, moduleName);
			DBUtil.set(pstmt, 3, imageName);
			DBUtil.set(pstmt, 4, allowModelId);
			DBUtil.set(pstmt, 5, webUrl != null ? webUrl.getWebUrl() : null);
			DBUtil.set(pstmt, 6, webUrl != null ? webUrl.getIsWeizhu() : null);
			DBUtil.set(pstmt, 7, appUri != null ? appUri.getAppUri() : null);
			DBUtil.set(pstmt, 8, promptDotTimestamp);
			DBUtil.set(pstmt, 9, state);
			DBUtil.set(pstmt, 10, createAdminId);
			DBUtil.set(pstmt, 11, createTime);

			pstmt.executeUpdate();
			rs = pstmt.getGeneratedKeys();
			if (rs.next()) {
				return rs.getInt(1);
			} else {
				throw new RuntimeException("插入模块数据出错！");
			}
		} finally {
			DBUtil.closeQuietly(rs);
			DBUtil.closeQuietly(pstmt);
		}
	}

	public static void updateModule(Connection conn, long companyId, 
			int moduleId, 
			String moduleName, 
			String imageName,
			@Nullable Integer allowModelId, 
			@Nullable DiscoverV2Protos.WebUrl webUrl, 
			@Nullable DiscoverV2Protos.AppUri appUri, 
			@Nullable Long promptDotTimestamp,
			@Nullable List<Integer> categoryOrderIdList, 
			@Nullable Long updateAdminId, 
			@Nullable Integer updateTime
			) throws SQLException {
		PreparedStatement pstmt = null;
		try {
			pstmt = conn.prepareStatement("UPDATE weizhu_discover_v2_module SET module_name = ?, image_name = ?, allow_model_id = ?, `web_url.web_url` = ?, `web_url.is_weizhu` = ?, `app_uri.app_uri` = ?, prompt_dot_timestamp = ?, category_order_str = ?, update_admin_id = ?, update_time = ? WHERE company_id = ? AND module_id = ?;");
			
			DBUtil.set(pstmt, 1, moduleName);
			DBUtil.set(pstmt, 2, imageName);
			DBUtil.set(pstmt, 3, allowModelId);
			DBUtil.set(pstmt, 4, webUrl != null ? webUrl.getWebUrl() : null);
			DBUtil.set(pstmt, 5, webUrl != null ? webUrl.getIsWeizhu() : null);
			DBUtil.set(pstmt, 6, appUri != null ? appUri.getAppUri() : null);
			DBUtil.set(pstmt, 7, promptDotTimestamp);
			DBUtil.set(pstmt, 8, categoryOrderIdList != null ? DBUtil.COMMA_JOINER.join(categoryOrderIdList) : null);
			DBUtil.set(pstmt, 9, updateAdminId);
			DBUtil.set(pstmt, 10, updateTime);
			DBUtil.set(pstmt, 11, companyId);
			DBUtil.set(pstmt, 12, moduleId);

			pstmt.executeUpdate();
		} finally {
			DBUtil.closeQuietly(pstmt);
		}
	}

	public static void updateModuleState(Connection conn, long companyId, 
			Collection<Integer> moduleIds, 
			@Nullable Collection<DiscoverV2Protos.State> states, 
			DiscoverV2Protos.State newState
			) throws SQLException {
		if (moduleIds.isEmpty() || (states != null && states.isEmpty())) {
			return;
		}
		
		StringBuilder sqlBuilder = new StringBuilder();
		sqlBuilder.append("UPDATE weizhu_discover_v2_module SET state = '");
		sqlBuilder.append(DBUtil.SQL_STRING_ESCAPER.escape(newState.name())).append("' WHERE company_id = ");
		sqlBuilder.append(companyId).append(" AND module_id IN (");
		DBUtil.COMMA_JOINER.appendTo(sqlBuilder, moduleIds);
		sqlBuilder.append(")");
		
		if (states != null) {
			sqlBuilder.append(" AND state IN ('");
			DBUtil.QUOTE_COMMA_JOINER.appendTo(sqlBuilder, Iterables.transform(Iterables.transform(states, Functions.toStringFunction()), DBUtil.SQL_STRING_ESCAPER.asFunction()));
			sqlBuilder.append("')");
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
	
	public static Map<Integer, DiscoverV2Protos.Module.Category> getModuleCategoryById(Connection conn, long companyId, 
			Collection<Integer> categoryIds,
			@Nullable Collection<DiscoverV2Protos.State> states
			) throws SQLException {
		if (categoryIds.isEmpty() || (states != null && states.isEmpty())) {
			return Collections.emptyMap();
		}
		
		StringBuilder sqlBuilder = new StringBuilder();
		sqlBuilder.append("SELECT * FROM weizhu_discover_v2_module_category WHERE company_id = ");
		sqlBuilder.append(companyId).append(" AND category_id IN (");
		DBUtil.COMMA_JOINER.appendTo(sqlBuilder, categoryIds);
		sqlBuilder.append(")");
		
		if (states != null) {
			sqlBuilder.append(" AND state IN ('");
			DBUtil.QUOTE_COMMA_JOINER.appendTo(sqlBuilder, Iterables.transform(Iterables.transform(states, Functions.toStringFunction()), DBUtil.SQL_STRING_ESCAPER.asFunction()));
			sqlBuilder.append("')");
		}
		
		sqlBuilder.append("; ");
		
		final String sql = sqlBuilder.toString();
		
		Statement stmt = null;
		ResultSet rs = null;
		try {
			stmt = conn.createStatement();
			rs = stmt.executeQuery(sql);

			// 获取模块分类
			Map<Integer, DiscoverV2Protos.Module.Category> categoryMap = new TreeMap<Integer, DiscoverV2Protos.Module.Category>();

			DiscoverV2Protos.Module.Category.Builder tmpCategoryBuilder = DiscoverV2Protos.Module.Category.newBuilder();
			while (rs.next()) {
				tmpCategoryBuilder.clear();

				DiscoverV2Protos.Module.Category category = MODULE_CATEGORY_MAPPER.mapToItem(rs, tmpCategoryBuilder).build();
				categoryMap.put(category.getCategoryId(), category);
			}
			return categoryMap;
		} finally {
			DBUtil.closeQuietly(rs);
			DBUtil.closeQuietly(stmt);
		}
	}

	public static int insertModuleCategory(Connection conn, long companyId, 
			int moduleId, 
			String categoryName, 
			@Nullable Integer allowModelId, 
			DiscoverV2Protos.State state,
			@Nullable Long createAdminId, 
			@Nullable Integer createTime
			) throws SQLException {
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try {
			pstmt = conn.prepareStatement("INSERT INTO weizhu_discover_v2_module_category (company_id, module_id, category_name, allow_model_id, state, create_admin_id, create_time) VALUES (?, ?, ?, ?, ?, ?, ?);",
					Statement.RETURN_GENERATED_KEYS);
			DBUtil.set(pstmt, 1, companyId);
			DBUtil.set(pstmt, 2, moduleId);
			DBUtil.set(pstmt, 3, categoryName);
			DBUtil.set(pstmt, 4, allowModelId);
			DBUtil.set(pstmt, 5, state);
			DBUtil.set(pstmt, 6, createAdminId);
			DBUtil.set(pstmt, 7, createTime);
			
			pstmt.executeUpdate();
			rs = pstmt.getGeneratedKeys();
			if (rs.next()) {
				return rs.getInt(1);
			} else {
				throw new RuntimeException("插入模块下分类出错！");
			}
		} finally {
			DBUtil.closeQuietly(rs);
			DBUtil.closeQuietly(pstmt);
		}
	}

	public static void updateModuleCategory(Connection conn, long companyId, 
			int categoryId, 
			int moduleId, 
			String categoryName,
			@Nullable Integer allowModelId, 
			@Nullable Long updateAdminId, 
			@Nullable Integer updateTime
			) throws SQLException {
		PreparedStatement pstmt = null;
		try {
			pstmt = conn.prepareStatement("UPDATE weizhu_discover_v2_module_category SET category_name = ?, allow_model_id = ?, update_admin_id = ?, update_time = ? WHERE company_id = ? AND category_id = ? AND module_id = ?;");

			DBUtil.set(pstmt, 1, categoryName);
			DBUtil.set(pstmt, 2, allowModelId);
			DBUtil.set(pstmt, 3, updateAdminId);
			DBUtil.set(pstmt, 4, updateTime);
			DBUtil.set(pstmt, 5, companyId);
			DBUtil.set(pstmt, 6, categoryId);
			DBUtil.set(pstmt, 7, moduleId);
			
			pstmt.executeUpdate();
		} finally {
			DBUtil.closeQuietly(pstmt);
		}
	}

	public static void updateModuleCategoryState(Connection conn, long companyId, 
			Collection<Integer> categoryIds, 
			@Nullable Collection<DiscoverV2Protos.State> states, 
			DiscoverV2Protos.State newState
			) throws SQLException {
		if (categoryIds.isEmpty()) {
			return;
		}
		
		StringBuilder sqlBuilder = new StringBuilder();
		sqlBuilder.append("UPDATE weizhu_discover_v2_module_category SET state = '");
		sqlBuilder.append(DBUtil.SQL_STRING_ESCAPER.escape(newState.name())).append("' WHERE company_id = ");
		sqlBuilder.append(companyId).append(" AND category_id IN (");
		DBUtil.COMMA_JOINER.appendTo(sqlBuilder, categoryIds);
		sqlBuilder.append(")");
		
		if (states != null) {
			sqlBuilder.append(" AND state IN ('");
			DBUtil.QUOTE_COMMA_JOINER.appendTo(sqlBuilder, Iterables.transform(Iterables.transform(states, Functions.toStringFunction()), DBUtil.SQL_STRING_ESCAPER.asFunction()));
			sqlBuilder.append("')");
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
	
	public static void updateModuleCategoryOrder(Connection conn, long companyId, 
			int moduleId, 
			List<Integer> categoryOrderIdList, 
			@Nullable Long updateAdminId, 
			@Nullable Integer updateTime
			) throws SQLException {
		PreparedStatement pstmt = null;
		try {
			pstmt = conn.prepareStatement("UPDATE weizhu_discover_v2_module SET category_order_str = ?, update_admin_id = ?, update_time = ? WHERE company_id = ? AND module_id = ?;");
			
			DBUtil.set(pstmt, 1, DBUtil.COMMA_JOINER.join(categoryOrderIdList));
			DBUtil.set(pstmt, 2, updateAdminId);
			DBUtil.set(pstmt, 3, updateTime);
			DBUtil.set(pstmt, 4, companyId);
			DBUtil.set(pstmt, 5, moduleId);
			
			pstmt.executeUpdate();
		} finally {
			DBUtil.closeQuietly(pstmt);
		}
	}

	public static void updateModuleCategoryModuleId(Connection conn, long companyId, 
			Collection<Integer> categoryIds, 
			@Nullable Collection<DiscoverV2Protos.State> states, 
			int newModuleId, 
			@Nullable Long updateAdminId, 
			@Nullable Integer updateTime
			) throws SQLException {
		if (categoryIds.isEmpty()) {
			return;
		}
		
		StringBuilder sqlBuilder = new StringBuilder();
		sqlBuilder.append("UPDATE weizhu_discover_v2_module_category SET module_id = ");
		sqlBuilder.append(newModuleId).append(", update_admin_id = ");
		sqlBuilder.append(updateAdminId == null ? "NULL" : updateAdminId).append(", update_time = ");
		sqlBuilder.append(updateTime == null ? "NULL" : updateTime).append(" WHERE company_id = ");
		sqlBuilder.append(companyId).append(" AND category_id IN (");
		DBUtil.COMMA_JOINER.appendTo(sqlBuilder, categoryIds);
		sqlBuilder.append(")");
		
		if (states != null) {
			sqlBuilder.append(" AND state IN ('");
			DBUtil.QUOTE_COMMA_JOINER.appendTo(sqlBuilder, Iterables.transform(Iterables.transform(states, Functions.toStringFunction()), DBUtil.SQL_STRING_ESCAPER.asFunction()));
			sqlBuilder.append("')");
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
	
	public static void addItemToCategory(Connection conn, long companyId, 
			Map<Integer, List<Long>> categoryItemIdMap,
			@Nullable Long createAdminId, 
			@Nullable Integer createTime
			) throws SQLException {
		if (categoryItemIdMap.isEmpty()) {
			return;
		}
		boolean isEmpty = true;
		for (Entry<Integer, List<Long>> entry : categoryItemIdMap.entrySet()) {
			if (!entry.getValue().isEmpty()) {
				isEmpty = false;
				break;
			}
		}
		if (isEmpty) {
			return;
		}
		
		StringBuilder sqlBuilder = new StringBuilder();
		sqlBuilder.append("INSERT IGNORE INTO weizhu_discover_v2_module_category_item (company_id, category_id, item_id, create_admin_id, create_time) VALUES ");
		
		boolean isFirst = true;
		for (Entry<Integer, List<Long>> entry : categoryItemIdMap.entrySet()) {
			final int categoryId = entry.getKey();
			for (long itemId : entry.getValue()) {
				if (isFirst) {
					isFirst = false;
				} else {
					sqlBuilder.append(", ");
				}
				
				sqlBuilder.append("(");
				sqlBuilder.append(companyId).append(", ");
				sqlBuilder.append(categoryId).append(", ");
				sqlBuilder.append(itemId).append(", ");
				sqlBuilder.append(createAdminId == null ? "NULL" : createAdminId).append(", ");
				sqlBuilder.append(createTime == null ? "NULL" : createTime).append(")");
			}
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
	
	public static void deleteItemFromCategory(Connection conn, long companyId, 
			Map<Integer, List<Long>> categoryItemIdMap
			) throws SQLException {
		if (categoryItemIdMap.isEmpty()) {
			return;
		}
		
		boolean isEmpty = true;
		for (Entry<Integer, List<Long>> entry : categoryItemIdMap.entrySet()) {
			if (!entry.getValue().isEmpty()) {
				isEmpty = false;
				break;
			}
		}
		if (isEmpty) {
			return;
		}

		StringBuilder sqlBuilder = new StringBuilder();
		sqlBuilder.append("DELETE FROM weizhu_discover_v2_module_category_item WHERE company_id = ");
		sqlBuilder.append(companyId).append(" AND (category_id, item_id) IN (");
		
		boolean isFirst = true;
		for (Entry<Integer, List<Long>> entry : categoryItemIdMap.entrySet()) {
			final int categoryId = entry.getKey();
			for (long itemId : entry.getValue()) {
				if (isFirst) {
					isFirst = false;
				} else {
					sqlBuilder.append(", ");
				}
				
				sqlBuilder.append("(");
				sqlBuilder.append(categoryId).append(", ");
				sqlBuilder.append(itemId).append(")");
			}
		}
		
		sqlBuilder.append("); ");
		
		final String sql = sqlBuilder.toString();
		Statement stmt = null;
		try {
			stmt = conn.createStatement();
			stmt.executeUpdate(sql);
		} finally {
			DBUtil.closeQuietly(stmt);
		}
	}
	
	public static Map<Long, Set<Integer>> getItemCategoryId(Connection conn, long companyId, 
			Collection<Long> itemIds
			) throws SQLException {
		if (itemIds.isEmpty()) {
			return Collections.emptyMap();
		}
		
		StringBuilder sqlBuilder = new StringBuilder();
		sqlBuilder.append("SELECT item_id, category_id FROM weizhu_discover_v2_module_category_item WHERE item_id IN (");
		DBUtil.COMMA_JOINER.appendTo(sqlBuilder, itemIds);
		sqlBuilder.append(") ORDER BY item_id ASC, category_id ASC; ");
		
		final String sql = sqlBuilder.toString();
		
		Statement stmt = null;
		ResultSet rs = null;
		try {
			stmt = conn.createStatement();
			rs = stmt.executeQuery(sql);
			
			Map<Long, Set<Integer>> itemCategoryIdMap = new TreeMap<Long, Set<Integer>>();
			while (rs.next()) {
				long itemId = rs.getLong("item_id");
				int categoryId = rs.getInt("category_id");
				
				Set<Integer> set = itemCategoryIdMap.get(itemId);
				if (set == null) {
					set = new TreeSet<Integer>();
					itemCategoryIdMap.put(itemId, set);
				}
				set.add(categoryId);
			}
			
			for (Long itemId : itemIds) {
				if (!itemCategoryIdMap.containsKey(itemId)) {
					itemCategoryIdMap.put(itemId, Collections.<Integer>emptySet());
				}
			}
			return itemCategoryIdMap;
		} finally {
			DBUtil.closeQuietly(rs);
			DBUtil.closeQuietly(stmt);
		}
	}
	
	public static void updateItemCategoryId(Connection conn, long companyId, 
			Map<Long, Set<Integer>> oldItemCategoryIdMap,
			Map<Long, Set<Integer>> newItemCategoryIdMap,
			@Nullable Long createAdminId, 
			@Nullable Integer createTime
			) throws SQLException {

		// 必须保证key完全一样
		if (!oldItemCategoryIdMap.keySet().equals(newItemCategoryIdMap.keySet())) {
			throw new IllegalArgumentException("update key not equals");
		}
		
		StringBuilder sqlBuilder = new StringBuilder();
		
		boolean isFirstInsert = true;
		for (Entry<Long, Set<Integer>> newEntry : newItemCategoryIdMap.entrySet()) {
			Long itemId = newEntry.getKey();
			Set<Integer> oldCategoryIdSet = oldItemCategoryIdMap.get(itemId);
			for (Integer categoryId : newEntry.getValue()) {
				if (!oldCategoryIdSet.contains(categoryId)) {
					if (isFirstInsert) {
						isFirstInsert = false;
						sqlBuilder.append("INSERT IGNORE INTO weizhu_discover_v2_module_category_item (company_id, category_id, item_id, create_admin_id, create_time) VALUES ");
					} else {
						sqlBuilder.append(", ");
					}
					
					sqlBuilder.append("(");
					sqlBuilder.append(companyId).append(", ");
					sqlBuilder.append(categoryId).append(", ");
					sqlBuilder.append(itemId).append(", ");
					sqlBuilder.append(createAdminId == null ? "NULL" : createAdminId).append(", ");
					sqlBuilder.append(createTime == null ? "NULL" : createTime).append(")");
				}
			}
		}
		if (!isFirstInsert) {
			sqlBuilder.append("; ");
		}
		
		boolean isFirstDelete = true;
		for (Entry<Long, Set<Integer>> oldEntry : oldItemCategoryIdMap.entrySet()) {
			Long itemId = oldEntry.getKey();
			Set<Integer> newCategoryIdSet = newItemCategoryIdMap.get(itemId);
			for (Integer categoryId : oldEntry.getValue()) {
				if (!newCategoryIdSet.contains(categoryId)) {
					if (isFirstDelete) {
						isFirstDelete = false;
						sqlBuilder.append("DELETE FROM weizhu_discover_v2_module_category_item WHERE company_id = ");
						sqlBuilder.append(companyId).append(" AND (category_id, item_id) IN (");
					} else {
						sqlBuilder.append(", ");
					}
					
					sqlBuilder.append("(");
					sqlBuilder.append(categoryId).append(", ");
					sqlBuilder.append(itemId).append(")");
				}
			}
		}
		if (!isFirstDelete) {
			sqlBuilder.append("); ");
		}
		
		if (!isFirstInsert || !isFirstDelete) {
			final String sql = sqlBuilder.toString();
			Statement stmt = null;
			try {
				stmt = conn.createStatement();
				stmt.execute(sql);
			} finally {
				DBUtil.closeQuietly(stmt);
			}
		}
	}
	
	public static DataPage<Long> getItemIdPage(Connection conn, long companyId, 
			int start, 
			int length, 
			@Nullable Boolean orderCreateTimeAsc,
			@Nullable Collection<DiscoverV2Protos.State> states
			) throws SQLException {
		if (start < 0) {
			start = 0;
		}
		if (length < 0) {
			length = 0;
		}
		
		StringBuilder whereBuilder = new StringBuilder();
		whereBuilder.append("WHERE company_id = ");
		whereBuilder.append(companyId);
		if (states != null) {
			whereBuilder.append(" AND state IN ('");
			DBUtil.QUOTE_COMMA_JOINER.appendTo(whereBuilder, Iterables.transform(Iterables.transform(states, Functions.toStringFunction()), DBUtil.SQL_STRING_ESCAPER.asFunction()));
			whereBuilder.append("')");
		}
		final String where = whereBuilder.toString();
		
		StringBuilder sqlBuilder = new StringBuilder();
		sqlBuilder.append("SELECT item_id FROM weizhu_discover_v2_item_base ");
		sqlBuilder.append(where).append(" ORDER BY");
		if (orderCreateTimeAsc != null && orderCreateTimeAsc) {
			sqlBuilder.append(" create_time ASC, item_id ASC LIMIT ");
		} else {
			sqlBuilder.append(" create_time DESC, item_id DESC LIMIT ");
		}
		sqlBuilder.append(start).append(", ");
		sqlBuilder.append(length).append("; ");
		sqlBuilder.append("SELECT count(*) AS total_size FROM weizhu_discover_v2_item_base ");
		sqlBuilder.append(where).append("; ");
		
		final String sql = sqlBuilder.toString();
		Statement stmt = null;
		ResultSet rs = null;
		try {
			stmt = conn.createStatement();
			stmt.execute(sql);
			rs = stmt.getResultSet();
			
			List<Long> itemIdList = new ArrayList<Long>();
			while(rs.next()) {
				itemIdList.add(rs.getLong("item_id"));
			}
			
			DBUtil.closeQuietly(rs);
			rs = null;
			
			stmt.getMoreResults();
			rs = stmt.getResultSet();
			
			if (!rs.next()) {
				throw new RuntimeException("get item id page total size fail");
			}
			int totalSize = rs.getInt("total_size");
			
			return new DataPage<Long>(itemIdList, totalSize, totalSize);
		} finally {
			DBUtil.closeQuietly(rs);
			DBUtil.closeQuietly(stmt);
		}
	}

	public static DataPage<Long> getItemIdPage(Connection conn, long companyId, 
			int start, 
			int length, 
			@Nullable Integer categoryId, 
			@Nullable String itemNameKeyword, 
			@Nullable Boolean orderCreateTimeAsc,
			@Nullable Collection<DiscoverV2Protos.State> states
			) throws SQLException {
		if (categoryId == null && itemNameKeyword == null) {
			return getItemIdPage(conn, companyId, start, length, orderCreateTimeAsc, states);
		}
		if (start < 0) {
			start = 0;
		}
		if (length < 0) {
			length = 0;
		}
		
		StringBuilder totalWhereBuilder = new StringBuilder();
		totalWhereBuilder.append("WHERE company_id = ");
		totalWhereBuilder.append(companyId);
		if (states != null) {
			totalWhereBuilder.append(" AND state IN ('");
			DBUtil.QUOTE_COMMA_JOINER.appendTo(totalWhereBuilder, Iterables.transform(Iterables.transform(states, Functions.toStringFunction()), DBUtil.SQL_STRING_ESCAPER.asFunction()));
			totalWhereBuilder.append("')");
		}
		final String totalWhere = totalWhereBuilder.toString();
		
		StringBuilder filterWhereBuilder = new StringBuilder();
		filterWhereBuilder.append(totalWhere);
		if (categoryId != null) {
			filterWhereBuilder.append(" AND item_id IN ( SELECT DISTINCT item_id FROM weizhu_discover_v2_module_category_item WHERE company_id = ");
			filterWhereBuilder.append(companyId).append(" AND category_id = ");
			filterWhereBuilder.append(categoryId).append(")");
		}
		if (itemNameKeyword != null) {
			filterWhereBuilder.append(" AND item_name LIKE '%");
			filterWhereBuilder.append(DBUtil.SQL_LIKE_STRING_ESCAPER.escape(itemNameKeyword)).append("%'");
		}
		final String filterWhere = filterWhereBuilder.toString();
		
		StringBuilder sqlBuilder = new StringBuilder();
		sqlBuilder.append("SELECT item_id FROM weizhu_discover_v2_item_base ");
		sqlBuilder.append(filterWhere).append(" ORDER BY");
		if (orderCreateTimeAsc != null && orderCreateTimeAsc) {
			sqlBuilder.append(" create_time ASC, item_id ASC LIMIT ");
		} else {
			sqlBuilder.append(" create_time DESC, item_id DESC LIMIT ");
		}
		sqlBuilder.append(start).append(", ");
		sqlBuilder.append(length).append("; ");
		sqlBuilder.append("SELECT count(*) AS filtered_size FROM weizhu_discover_v2_item_base ");
		sqlBuilder.append(filterWhere).append("; ");
		sqlBuilder.append("SELECT count(*) AS total_size FROM weizhu_discover_v2_item_base ");
		sqlBuilder.append(totalWhere).append("; ");
		
		final String sql = sqlBuilder.toString();
		Statement stmt = null;
		ResultSet rs = null;
		try {
			stmt = conn.createStatement();
			stmt.execute(sql);
			rs = stmt.getResultSet();
			
			List<Long> itemIdList = new ArrayList<Long>();
			while (rs.next()) {
				itemIdList.add(rs.getLong("item_id"));
			}

			DBUtil.closeQuietly(rs);
			rs = null;
			stmt.getMoreResults();
			rs = stmt.getResultSet();
			
			int filteredSize;
			if (!rs.next()) {
				throw new RuntimeException("cannot get item id page filter size");
			} else {
				filteredSize = rs.getInt("filtered_size");
			}

			DBUtil.closeQuietly(rs);
			rs = null;
			stmt.getMoreResults();
			rs = stmt.getResultSet();
			
			int totalSize;
			if (!rs.next()) {
				throw new RuntimeException("cannot get item id page filter size");
			} else {
				totalSize = rs.getInt("total_size");
			}

			return new DataPage<Long>(itemIdList, totalSize, filteredSize);
		} finally {
			DBUtil.closeQuietly(rs);
			DBUtil.closeQuietly(stmt);
		}
	}
	
	private static final String INSERT_ITEM_BASE_SQL = 
			"INSERT INTO weizhu_discover_v2_item_base ("
			+ "company_id, item_name, item_desc, image_name, allow_model_id, enable_comment, enable_score, enable_remind, enable_like, enable_share, enable_external_share, "
			+ "`web_url.web_url`, `web_url.is_weizhu`, "
			+ "`document.document_url`, `document.document_type`, `document.document_size`, `document.check_md5`, `document.is_download`, `document.is_auth_url`, "
			+ "`video.video_url`,  `video.video_type`, `video.video_size`, `video.video_time`, `video.check_md5`, `video.is_download`, `video.is_auth_url`, "
			+ "`audio.audio_url`, `audio.audio_type`, `audio.audio_size`, `audio.audio_time`, `audio.check_md5`, `audio.is_download`, `audio.is_auth_url`, "
			+ "`app_uri.app_uri`, "
			+ "state, create_admin_id, create_time"
			+ ") VALUES ("
			+ "?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, "
			+ "?, ?, "
			+ "?, ?, ?, ?, ?, ?, "
			+ "?, ?, ?, ?, ?, ?, ?, "
			+ "?, ?, ?, ?, ?, ?, ?, "
			+ "?, "
			+ "?, ?, ?);";

	public static List<Long> insertItem(Connection conn, long companyId, 
			List<DiscoverV2Protos.Item.Base> itemBaseList
			) throws SQLException {
		if (itemBaseList.isEmpty()) {
			return Collections.emptyList();
		}
		
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try {
			pstmt = conn.prepareStatement(INSERT_ITEM_BASE_SQL, Statement.RETURN_GENERATED_KEYS);
			for (DiscoverV2Protos.Item.Base itemBase : itemBaseList) {
				DBUtil.set(pstmt, 1,  companyId);
				DBUtil.set(pstmt, 2,  itemBase.getItemName());
				DBUtil.set(pstmt, 3,  itemBase.getItemDesc());
				DBUtil.set(pstmt, 4,  itemBase.getImageName());
				DBUtil.set(pstmt, 5,  itemBase.hasAllowModelId(), itemBase.getAllowModelId());
				DBUtil.set(pstmt, 6,  itemBase.getEnableComment());
				DBUtil.set(pstmt, 7,  itemBase.getEnableScore());
				DBUtil.set(pstmt, 8,  itemBase.getEnableRemind());
				DBUtil.set(pstmt, 9,  itemBase.getEnableLike());
				DBUtil.set(pstmt, 10, itemBase.getEnableShare());
				DBUtil.set(pstmt, 11, itemBase.hasEnableExternalShare(), itemBase.getEnableExternalShare());
				
				DBUtil.set(pstmt, 12, itemBase.hasWebUrl(), itemBase.getWebUrl().getWebUrl());
				DBUtil.set(pstmt, 13, itemBase.hasWebUrl(), itemBase.getWebUrl().getIsWeizhu());
	
				DBUtil.set(pstmt, 14, itemBase.hasDocument(), itemBase.getDocument().getDocumentUrl());
				DBUtil.set(pstmt, 15, itemBase.hasDocument(), itemBase.getDocument().getDocumentType());
				DBUtil.set(pstmt, 16, itemBase.hasDocument(), itemBase.getDocument().getDocumentSize());
				DBUtil.set(pstmt, 17, itemBase.hasDocument() && itemBase.getDocument().hasCheckMd5(), itemBase.getDocument().getCheckMd5());
				DBUtil.set(pstmt, 18, itemBase.hasDocument(), itemBase.getDocument().getIsDownload());
				DBUtil.set(pstmt, 19, itemBase.hasDocument(), itemBase.getDocument().getIsAuthUrl());
	
				DBUtil.set(pstmt, 20, itemBase.hasVideo(), itemBase.getVideo().getVideoUrl());
				DBUtil.set(pstmt, 21, itemBase.hasVideo(), itemBase.getVideo().getVideoType());
				DBUtil.set(pstmt, 22, itemBase.hasVideo(), itemBase.getVideo().getVideoSize());
				DBUtil.set(pstmt, 23, itemBase.hasVideo(), itemBase.getVideo().getVideoTime());
				DBUtil.set(pstmt, 24, itemBase.hasVideo() && itemBase.getVideo().hasCheckMd5(), itemBase.getVideo().getCheckMd5());
				DBUtil.set(pstmt, 25, itemBase.hasVideo(), itemBase.getVideo().getIsDownload());
				DBUtil.set(pstmt, 26, itemBase.hasVideo(), itemBase.getVideo().getIsAuthUrl());
	
				DBUtil.set(pstmt, 27, itemBase.hasAudio(), itemBase.getAudio().getAudioUrl());
				DBUtil.set(pstmt, 28, itemBase.hasAudio(), itemBase.getAudio().getAudioType());
				DBUtil.set(pstmt, 29, itemBase.hasAudio(), itemBase.getAudio().getAudioSize());
				DBUtil.set(pstmt, 30, itemBase.hasAudio(), itemBase.getAudio().getAudioTime());
				DBUtil.set(pstmt, 31, itemBase.hasAudio() && itemBase.getAudio().hasCheckMd5(), itemBase.getAudio().getCheckMd5());
				DBUtil.set(pstmt, 32, itemBase.hasAudio(), itemBase.getAudio().getIsDownload());
				DBUtil.set(pstmt, 33, itemBase.hasAudio(), itemBase.getAudio().getIsAuthUrl());
	
				DBUtil.set(pstmt, 34, itemBase.hasAppUri(), itemBase.getAppUri().getAppUri());
	
				DBUtil.set(pstmt, 35, itemBase.hasState(), itemBase.getState());
				DBUtil.set(pstmt, 36, itemBase.hasCreateAdminId(), itemBase.getCreateAdminId());
				DBUtil.set(pstmt, 37, itemBase.hasCreateTime(), itemBase.getCreateTime());
				
				pstmt.addBatch();
			}
			pstmt.executeBatch();
			rs = pstmt.getGeneratedKeys();
			
			List<Long> itemIdList = new ArrayList<Long>();
			while (rs.next()) {
				itemIdList.add(rs.getLong(1));
			}
			
			if (itemBaseList.size() != itemIdList.size()) {
				throw new RuntimeException("insert item base fail");
			}
			
			return itemIdList;
		} finally {
			DBUtil.closeQuietly(rs);
			DBUtil.closeQuietly(pstmt);
		}
	}
	
	private static final String UPDATE_ITEM_SQL =
			"UPDATE weizhu_discover_v2_item_base SET "
			+ "item_name = ?, item_desc = ?, image_name = ?, allow_model_id = ?, enable_comment = ?, enable_score = ?, enable_remind = ?, enable_like = ?, enable_share = ?, enable_external_share = ?, "
			+ "`web_url.web_url` = ?, `web_url.is_weizhu` = ?, "
			+ "`document.document_url` = ?, `document.document_type` = ?, `document.document_size` = ?, `document.check_md5` = ?, `document.is_download` = ?, `document.is_auth_url` = ?, "
			+ "`video.video_url` = ?,  `video.video_type` = ?, `video.video_size` = ?, `video.video_time` = ?, `video.check_md5` = ?, `video.is_download` = ?, `video.is_auth_url` = ?, "
			+ "`audio.audio_url` = ?, `audio.audio_type` = ?, `audio.audio_size` = ?, `audio.audio_time` = ?, `audio.check_md5` = ?, `audio.is_download` = ?, `audio.is_auth_url` = ?, "
			+ "`app_uri.app_uri` = ?, "
			+ "update_admin_id = ?, update_time = ? "
			+ "WHERE company_Id = ? AND item_id = ?; ";
	
	public static void updateItem(Connection conn, long companyId, 
			DiscoverV2Protos.Item.Base itemBase
			) throws SQLException {
		PreparedStatement pstmt = null;
		try {
			pstmt = conn.prepareStatement(UPDATE_ITEM_SQL);
			DBUtil.set(pstmt, 1, itemBase.getItemName());
			DBUtil.set(pstmt, 2, itemBase.getItemDesc());
			DBUtil.set(pstmt, 3, itemBase.getImageName());
			DBUtil.set(pstmt, 4, itemBase.hasAllowModelId(), itemBase.getAllowModelId());
			DBUtil.set(pstmt, 5, itemBase.getEnableComment());
			DBUtil.set(pstmt, 6, itemBase.getEnableScore());
			DBUtil.set(pstmt, 7, itemBase.getEnableRemind());
			DBUtil.set(pstmt, 8, itemBase.getEnableLike());
			DBUtil.set(pstmt, 9, itemBase.getEnableShare());
			DBUtil.set(pstmt, 10, itemBase.hasEnableExternalShare(), itemBase.getEnableExternalShare());

			DBUtil.set(pstmt, 11, itemBase.hasWebUrl(), itemBase.getWebUrl().getWebUrl());
			DBUtil.set(pstmt, 12, itemBase.hasWebUrl(), itemBase.getWebUrl().getIsWeizhu());

			DBUtil.set(pstmt, 13, itemBase.hasDocument(), itemBase.getDocument().getDocumentUrl());
			DBUtil.set(pstmt, 14, itemBase.hasDocument(), itemBase.getDocument().getDocumentType());
			DBUtil.set(pstmt, 15, itemBase.hasDocument(), itemBase.getDocument().getDocumentSize());
			DBUtil.set(pstmt, 16, itemBase.hasDocument() && itemBase.getDocument().hasCheckMd5(), itemBase.getDocument().getCheckMd5());
			DBUtil.set(pstmt, 17, itemBase.hasDocument(), itemBase.getDocument().getIsDownload());
			DBUtil.set(pstmt, 18, itemBase.hasDocument(), itemBase.getDocument().getIsAuthUrl());

			DBUtil.set(pstmt, 19, itemBase.hasVideo(), itemBase.getVideo().getVideoUrl());
			DBUtil.set(pstmt, 20, itemBase.hasVideo(), itemBase.getVideo().getVideoType());
			DBUtil.set(pstmt, 21, itemBase.hasVideo(), itemBase.getVideo().getVideoSize());
			DBUtil.set(pstmt, 22, itemBase.hasVideo(), itemBase.getVideo().getVideoTime());
			DBUtil.set(pstmt, 23, itemBase.hasVideo() && itemBase.getVideo().hasCheckMd5(), itemBase.getVideo().getCheckMd5());
			DBUtil.set(pstmt, 24, itemBase.hasVideo(), itemBase.getVideo().getIsDownload());
			DBUtil.set(pstmt, 25, itemBase.hasVideo(), itemBase.getVideo().getIsAuthUrl());

			DBUtil.set(pstmt, 26, itemBase.hasAudio(), itemBase.getAudio().getAudioUrl());
			DBUtil.set(pstmt, 27, itemBase.hasAudio(), itemBase.getAudio().getAudioType());
			DBUtil.set(pstmt, 28, itemBase.hasAudio(), itemBase.getAudio().getAudioSize());
			DBUtil.set(pstmt, 29, itemBase.hasAudio(), itemBase.getAudio().getAudioTime());
			DBUtil.set(pstmt, 30, itemBase.hasAudio() && itemBase.getAudio().hasCheckMd5(), itemBase.getAudio().getCheckMd5());
			DBUtil.set(pstmt, 31, itemBase.hasAudio(), itemBase.getAudio().getIsDownload());
			DBUtil.set(pstmt, 32, itemBase.hasAudio(), itemBase.getAudio().getIsAuthUrl());

			DBUtil.set(pstmt, 33, itemBase.hasAppUri(), itemBase.getAppUri().getAppUri());

			DBUtil.set(pstmt, 34, itemBase.hasUpdateAdminId(), itemBase.getUpdateAdminId());
			DBUtil.set(pstmt, 35, itemBase.hasUpdateTime(), itemBase.getUpdateTime());
			DBUtil.set(pstmt, 36, companyId);
			DBUtil.set(pstmt, 37, itemBase.getItemId());

			pstmt.executeUpdate();
		} finally {
			DBUtil.closeQuietly(pstmt);
		}
	}

	public static void updateItemState(Connection conn, long companyId, 
			Collection<Long> itemIds, 
			@Nullable Collection<DiscoverV2Protos.State> states,
			DiscoverV2Protos.State newState
			) throws SQLException {
		if (itemIds.isEmpty() || (states != null && states.isEmpty())) {
			return;
		}
		
		StringBuilder sqlBuilder = new StringBuilder();
		sqlBuilder.append("UPDATE weizhu_discover_v2_item_base SET state = '");
		sqlBuilder.append(DBUtil.SQL_STRING_ESCAPER.escape(newState.name())).append("' WHERE company_id = ");
		sqlBuilder.append(companyId).append(" AND item_id IN (");
		DBUtil.COMMA_JOINER.appendTo(sqlBuilder, itemIds);
		sqlBuilder.append(")");

		if (states != null) {
			sqlBuilder.append(" AND state IN ('");
			DBUtil.QUOTE_COMMA_JOINER.appendTo(sqlBuilder, Iterables.transform(Iterables.transform(states, Functions.toStringFunction()), DBUtil.SQL_STRING_ESCAPER.asFunction()));
			sqlBuilder.append("')");
		}
		
		sqlBuilder.append("; ");
		
		final String sql = sqlBuilder.toString();
		Statement st = null;
		try {
			st = conn.createStatement();
			st.executeUpdate(sql);
		} finally {
			DBUtil.closeQuietly(st);
		}
	}
	
	public static DataPage<DiscoverV2Protos.ItemLearn> getItemLearnPage(Connection conn, long companyId, 
			long itemId, 
			int start, 
			int length
			) throws SQLException {
		if (start < 0) {
			start = 0;
		}
		if (length < 0) {
			length = 0;
		}
		
		StringBuilder whereBuilder = new StringBuilder();
		whereBuilder.append("WHERE company_id = ");
		whereBuilder.append(companyId).append(" AND item_id = ");
		whereBuilder.append(itemId);
		final String where = whereBuilder.toString();

		StringBuilder sqlBuilder = new StringBuilder();
		sqlBuilder.append("SELECT * FROM weizhu_discover_v2_item_learn ");
		sqlBuilder.append(where).append(" ORDER BY learn_time DESC, user_id DESC LIMIT ");
		sqlBuilder.append(start).append(", ").append(length).append("; ");
		sqlBuilder.append("SELECT count(*) AS total_size FROM weizhu_discover_v2_item_learn ");
		sqlBuilder.append(where).append("; ");
		
		final String sql = sqlBuilder.toString();
		Statement stmt = null;
		ResultSet rs = null;
		try {
			stmt = conn.createStatement();
			stmt.execute(sql);
			rs = stmt.getResultSet();

			List<DiscoverV2Protos.ItemLearn> itemLearnList = new ArrayList<DiscoverV2Protos.ItemLearn>();
			DiscoverV2Protos.ItemLearn.Builder tmpBuilder = DiscoverV2Protos.ItemLearn.newBuilder();
			while (rs.next()) {
				tmpBuilder.clear();
				itemLearnList.add(ITEM_LEARN_MAPPER.mapToItem(rs, tmpBuilder).build());
			}

			DBUtil.closeQuietly(rs);
			rs = null;
			stmt.getMoreResults();
			rs = stmt.getResultSet();

			if (!rs.next()) {
				throw new RuntimeException("cannot get item learn page total size");
			}
			int totalSize = rs.getInt("total_size");
			
			return new DataPage<DiscoverV2Protos.ItemLearn>(itemLearnList, totalSize, totalSize);
		} finally {
			DBUtil.closeQuietly(rs);
			DBUtil.closeQuietly(stmt);
		}
	}
	
	public static DataPage<DiscoverV2Protos.ItemComment> getItemCommentPage(Connection conn, long companyId, 
			long itemId, 
			int start,
			int length,
			@Nullable Boolean isDelete
			) throws SQLException {
		if (start < 0) {
			start = 0;
		}
		if (length < 0) {
			length = 0;
		}
		
		StringBuilder whereBuilder = new StringBuilder();
		whereBuilder.append("WHERE company_id = ");
		whereBuilder.append(companyId).append(" AND item_id = ");
		whereBuilder.append(itemId);
		if (isDelete != null) {
			whereBuilder.append(" AND is_delete = ");
			whereBuilder.append(isDelete ? 1 : 0);
		}
		final String where = whereBuilder.toString();

		StringBuilder sqlBuilder = new StringBuilder();
		sqlBuilder.append("SELECT * FROM weizhu_discover_v2_item_comment ");
		sqlBuilder.append(where).append(" ORDER BY comment_time DESC, comment_id DESC LIMIT ");
		sqlBuilder.append(start).append(", ").append(length).append("; ");
		sqlBuilder.append("SELECT count(*) AS total_size FROM weizhu_discover_v2_item_comment ");
		sqlBuilder.append(where).append("; ");
		
		final String sql = sqlBuilder.toString();
		Statement stmt = null;
		ResultSet rs = null;
		try {
			stmt = conn.createStatement();
			stmt.execute(sql);
			rs = stmt.getResultSet();

			List<DiscoverV2Protos.ItemComment> itemCommentList = new ArrayList<DiscoverV2Protos.ItemComment>();
			DiscoverV2Protos.ItemComment.Builder tmpBuilder = DiscoverV2Protos.ItemComment.newBuilder();
			while (rs.next()) {
				tmpBuilder.clear();
				itemCommentList.add(ITEM_COMMENT_MAPPER.mapToItem(rs, tmpBuilder).build());
			}

			DBUtil.closeQuietly(rs);
			rs = null;
			stmt.getMoreResults();
			rs = stmt.getResultSet();

			if (!rs.next()) {
				throw new RuntimeException("cannot get item comment page total size");
			}
			int totalSize = rs.getInt("total_size");
			
			return new DataPage<DiscoverV2Protos.ItemComment>(itemCommentList, totalSize, totalSize);
		} finally {
			DBUtil.closeQuietly(rs);
			DBUtil.closeQuietly(stmt);
		}
	}

	public static DataPage<DiscoverV2Protos.ItemScore> getItemScorePage(Connection conn, long companyId, 
			long itemId, 
			int start, 
			int length
			) throws SQLException {
		if (start < 0) {
			start = 0;
		}
		if (length < 0) {
			length = 0;
		}
		
		StringBuilder whereBuilder = new StringBuilder();
		whereBuilder.append("WHERE company_id = ");
		whereBuilder.append(companyId).append(" AND item_id = ");
		whereBuilder.append(itemId);
		final String where = whereBuilder.toString();

		StringBuilder sqlBuilder = new StringBuilder();
		sqlBuilder.append("SELECT * FROM weizhu_discover_v2_item_score ");
		sqlBuilder.append(where).append(" ORDER BY score_time DESC, user_id DESC LIMIT ");
		sqlBuilder.append(start).append(", ").append(length).append("; ");
		sqlBuilder.append("SELECT count(*) AS total_size FROM weizhu_discover_v2_item_score ");
		sqlBuilder.append(where).append("; ");
		
		final String sql = sqlBuilder.toString();
		Statement stmt = null;
		ResultSet rs = null;
		try {
			stmt = conn.createStatement();
			stmt.execute(sql);
			rs = stmt.getResultSet();

			List<DiscoverV2Protos.ItemScore> itemScoreList = new ArrayList<DiscoverV2Protos.ItemScore>();
			DiscoverV2Protos.ItemScore.Builder tmpBuilder = DiscoverV2Protos.ItemScore.newBuilder();
			while (rs.next()) {
				tmpBuilder.clear();
				itemScoreList.add(ITEM_SCORE_MAPPER.mapToItem(rs, tmpBuilder).build());
			}

			DBUtil.closeQuietly(rs);
			rs = null;
			stmt.getMoreResults();
			rs = stmt.getResultSet();

			if (!rs.next()) {
				throw new RuntimeException("cannot get item score page total size");
			}
			int totalSize = rs.getInt("total_size");
			
			return new DataPage<DiscoverV2Protos.ItemScore>(itemScoreList, totalSize, totalSize);
		} finally {
			DBUtil.closeQuietly(rs);
			DBUtil.closeQuietly(stmt);
		}
	}

	public static DataPage<DiscoverV2Protos.ItemLike> getItemLikePage(Connection conn, long companyId, 
			long itemId, 
			int start, 
			int length
			) throws SQLException {
		if (start < 0) {
			start = 0;
		}
		if (length < 0) {
			length = 0;
		}
		
		StringBuilder whereBuilder = new StringBuilder();
		whereBuilder.append("WHERE company_id = ");
		whereBuilder.append(companyId).append(" AND item_id = ");
		whereBuilder.append(itemId);
		final String where = whereBuilder.toString();

		StringBuilder sqlBuilder = new StringBuilder();
		sqlBuilder.append("SELECT * FROM weizhu_discover_v2_item_like ");
		sqlBuilder.append(where).append(" ORDER BY like_time DESC, user_id DESC LIMIT ");
		sqlBuilder.append(start).append(", ").append(length).append("; ");
		sqlBuilder.append("SELECT count(*) AS total_size FROM weizhu_discover_v2_item_like ");
		sqlBuilder.append(where).append("; ");
		
		final String sql = sqlBuilder.toString();
		Statement stmt = null;
		ResultSet rs = null;
		try {
			stmt = conn.createStatement();
			stmt.execute(sql);
			rs = stmt.getResultSet();

			List<DiscoverV2Protos.ItemLike> itemLikeList = new ArrayList<DiscoverV2Protos.ItemLike>();
			DiscoverV2Protos.ItemLike.Builder tmpBuilder = DiscoverV2Protos.ItemLike.newBuilder();
			while (rs.next()) {
				tmpBuilder.clear();
				itemLikeList.add(ITEM_LIKE_MAPPER.mapToItem(rs, tmpBuilder).build());
			}

			DBUtil.closeQuietly(rs);
			rs = null;
			stmt.getMoreResults();
			rs = stmt.getResultSet();

			if (!rs.next()) {
				throw new RuntimeException("cannot get item like page total size");
			}
			int totalSize = rs.getInt("total_size");
			
			return new DataPage<DiscoverV2Protos.ItemLike>(itemLikeList, totalSize, totalSize);
		} finally {
			DBUtil.closeQuietly(rs);
			DBUtil.closeQuietly(stmt);
		}
	}

	public static DataPage<DiscoverV2Protos.ItemShare> getItemSharePage(Connection conn, long companyId, 
			long itemId, 
			int start, 
			int length
			) throws SQLException {
		if (start < 0) {
			start = 0;
		}
		if (length < 0) {
			length = 0;
		}
		
		StringBuilder whereBuilder = new StringBuilder();
		whereBuilder.append("WHERE company_id = ");
		whereBuilder.append(companyId).append(" AND item_id = ");
		whereBuilder.append(itemId);
		final String where = whereBuilder.toString();

		StringBuilder sqlBuilder = new StringBuilder();
		sqlBuilder.append("SELECT * FROM weizhu_discover_v2_item_share ");
		sqlBuilder.append(where).append(" ORDER BY share_time DESC, user_id DESC LIMIT ");
		sqlBuilder.append(start).append(", ").append(length).append("; ");
		sqlBuilder.append("SELECT count(*) AS total_size FROM weizhu_discover_v2_item_share ");
		sqlBuilder.append(where).append("; ");
		
		final String sql = sqlBuilder.toString();
		Statement stmt = null;
		ResultSet rs = null;
		try {
			stmt = conn.createStatement();
			stmt.execute(sql);
			rs = stmt.getResultSet();

			List<DiscoverV2Protos.ItemShare> itemShareList = new ArrayList<DiscoverV2Protos.ItemShare>();
			DiscoverV2Protos.ItemShare.Builder tmpBuilder = DiscoverV2Protos.ItemShare.newBuilder();
			while (rs.next()) {
				tmpBuilder.clear();
				itemShareList.add(ITEM_SHARE_MAPPER.mapToItem(rs, tmpBuilder).build());
			}

			DBUtil.closeQuietly(rs);
			rs = null;
			stmt.getMoreResults();
			rs = stmt.getResultSet();

			if (!rs.next()) {
				throw new RuntimeException("cannot get item share page total size");
			}
			int totalSize = rs.getInt("total_size");
			
			return new DataPage<DiscoverV2Protos.ItemShare>(itemShareList, totalSize, totalSize);
		} finally {
			DBUtil.closeQuietly(rs);
			DBUtil.closeQuietly(stmt);
		}
	}

}
