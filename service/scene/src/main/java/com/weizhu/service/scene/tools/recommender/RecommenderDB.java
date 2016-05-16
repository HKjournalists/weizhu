package com.weizhu.service.scene.tools.recommender;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;

import javax.annotation.Nullable;

import com.weizhu.common.db.DBUtil;
import com.weizhu.common.db.ProtobufMapper;
import com.weizhu.common.utils.DataPage;
import com.weizhu.proto.AdminSceneProtos;
import com.weizhu.proto.AdminSceneProtos.PriceWebUrlCreateCondition;
import com.weizhu.proto.SceneProtos;
import com.weizhu.proto.SceneProtos.RecommenderCategory;
import com.weizhu.proto.SceneProtos.RecommenderCompetitorProduct;
import com.weizhu.proto.SceneProtos.RecommenderPriceWebUrl;
import com.weizhu.proto.SceneProtos.RecommenderRecommendProduct;
import com.weizhu.proto.SceneProtos.State;
import com.weizhu.service.scene.SceneDAOProtos;
import com.weizhu.service.scene.SceneDAOProtos.RecommenderHome;

public class RecommenderDB {
	private static final ProtobufMapper<SceneProtos.RecommenderCompetitorProduct> COMPETITOR_PRODUCT_MAPPER = ProtobufMapper.createMapper(SceneProtos.RecommenderCompetitorProduct.getDefaultInstance(),
			"competitor_product_id",
			"competitor_product_name",
			"image_name",
			"category_id",
			"allow_model_id",
			"state",
			"create_admin_id",
			"create_time",
			"update_admin_id",
			"update_time");
	private static final ProtobufMapper<SceneProtos.RecommenderRecommendProduct> RECOMMEND_PRODUCT_MAPPER = ProtobufMapper.createMapper(SceneProtos.RecommenderRecommendProduct.getDefaultInstance(),
			"recommend_product_id",
			"recommend_product_name",
			"recommend_product_desc",
			"image_name",
			"allow_model_id",
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
	private static final ProtobufMapper<SceneProtos.RecommenderCategory> CATEGORY_MAPPER = ProtobufMapper.createMapper(SceneProtos.RecommenderCategory.getDefaultInstance(),
			"category_id",
			"category_name",
			"category_desc",
			"image_name",
			"is_leaf_category",
			"parent_category_id",
			"state",
			"create_admin_id",
			"create_time",
			"update_admin_id",
			"update_time");

	private static final ProtobufMapper<SceneProtos.RecommenderPriceWebUrl> PRICE_URL_MAPPER = ProtobufMapper.createMapper(SceneProtos.RecommenderPriceWebUrl.getDefaultInstance(),
			"url_id",
			"recommend_product_id",
			"url_name",
			"url_content",
			"image_name",
			"is_weizhu",
			"create_admin_id",
			"create_time");

	public static List<RecommenderCompetitorProduct> getCompetitorProduct(Connection conn, long companyId, @Nullable Integer categoryId,
			@Nullable String competitorProductName, @Nullable RecommenderCompetitorProduct currentOffsetIndex, int size, Collection<State> states)
			throws SQLException {

		String statsCondStr;
		if (states == null) {
			statsCondStr = "";
		} else {
			StringBuilder sb = new StringBuilder(" AND state IN ");
			appendStatesCondSql(sb, states);
			statsCondStr = sb.toString();
		}

		StringBuilder sql = new StringBuilder();
		sql.append("SELECT * FROM weizhu_tool_recommender_competitor_product WHERE company_id = ").append(companyId).append(statsCondStr);
		if (categoryId != null) {
			sql.append(" AND category_id = ").append(categoryId);
		}
		if (competitorProductName != null) {
			sql.append(" AND competitor_product_name LIKE '%").append(DBUtil.SQL_LIKE_STRING_ESCAPER.escape(competitorProductName)).append("%'");
		}
		if (currentOffsetIndex != null) {
			sql.append(" AND (create_time <")
					.append(currentOffsetIndex.getCreateTime())
					.append(" OR (create_time=")
					.append(currentOffsetIndex.getCreateTime())
					.append(" AND competitor_product_id<")
					.append(currentOffsetIndex.getCompetitorProductId())
					.append(")) ");
		}
		sql.append(" ORDER BY create_time DESC,competitor_product_id DESC LIMIT ").append(size).append(";");

		Statement st = null;
		ResultSet rs = null;

		try {
			st = conn.createStatement();
			rs = st.executeQuery(sql.toString());

			List<RecommenderCompetitorProduct> competitorProductList = new ArrayList<RecommenderCompetitorProduct>();
			RecommenderCompetitorProduct.Builder competitorProductBuilder = RecommenderCompetitorProduct.newBuilder();
			while (rs.next()) {
				competitorProductBuilder.clear();
				competitorProductList.add(COMPETITOR_PRODUCT_MAPPER.mapToItem(rs, competitorProductBuilder).build());
			}
			return competitorProductList;
		} finally {
			DBUtil.closeQuietly(rs);
			DBUtil.closeQuietly(st);
		}

	}

	public static RecommenderHome getRecommenderHome(Connection conn, long companyId, Collection<State> states) throws SQLException {
		String statsCondStr;
		if (states == null) {
			statsCondStr = "";
		} else {
			StringBuilder sb = new StringBuilder(" AND state IN ");
			appendStatesCondSql(sb, states);
			statsCondStr = sb.toString();
		}

		StringBuilder sql = new StringBuilder();
		sql.append("SELECT * FROM weizhu_tool_recommender_category WHERE company_id = ").append(companyId)
				.append(statsCondStr)
				.append(" ORDER BY create_time DESC, category_id DESC;");

		Statement st = null;
		ResultSet rs = null;

		try {
			st = conn.createStatement();
			rs = st.executeQuery(sql.toString());

			RecommenderHome.Builder recommenderHomeBuilder = RecommenderHome.newBuilder();
			RecommenderCategory.Builder categoryBuilder = RecommenderCategory.newBuilder();
			while (rs.next()) {
				categoryBuilder.clear();
				recommenderHomeBuilder.addRecommenderCategory(CATEGORY_MAPPER.mapToItem(rs, categoryBuilder).build());
			}
			return recommenderHomeBuilder.build();
		} finally {
			DBUtil.closeQuietly(rs);
			DBUtil.closeQuietly(st);
		}
	}

	public static Map<Integer, SceneDAOProtos.RecommenderCategoryExt> getCategoryExt(Connection conn, long companyId, Collection<Integer> categoryIds, int size,
			Collection<State> states) throws SQLException {

		if (categoryIds.isEmpty()) {
			return Collections.emptyMap();
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

		for (int categoryId : categoryIds) {
			sql.append("SELECT * FROM weizhu_tool_recommender_competitor_product WHERE company_id = ")
					.append(companyId)
					.append(" AND category_id = ")
					.append(categoryId)
					.append(statsCondStr)
					.append(" ORDER BY create_time DESC,competitor_product_id DESC LIMIT ")
					.append(size)
					.append(";");
		}

		Statement st = null;
		ResultSet rs = null;

		try {
			st = conn.createStatement();
			rs = st.executeQuery(sql.toString());

			Map<Integer, SceneDAOProtos.RecommenderCategoryExt.Builder> categoryExtBuilderMap = new HashMap<Integer, SceneDAOProtos.RecommenderCategoryExt.Builder>();
			RecommenderCompetitorProduct.Builder competitorProductBuilder = RecommenderCompetitorProduct.newBuilder();

			boolean isFirst = true;
			for (int i = 0; i < categoryIds.size(); i++) {
				if (isFirst) {
					isFirst = false;
				} else {
					DBUtil.closeQuietly(rs);
					rs = null;

					st.getMoreResults();
					rs = st.getResultSet();
				}

				while (rs.next()) {

					competitorProductBuilder.clear();

					int categoryId = rs.getInt("category_id");
					SceneDAOProtos.RecommenderCategoryExt.Builder categoryExtBuilder = categoryExtBuilderMap.get(categoryId);
					if (categoryExtBuilder == null) {
						categoryExtBuilder = SceneDAOProtos.RecommenderCategoryExt.newBuilder();
						categoryExtBuilderMap.put(categoryId, categoryExtBuilder);
					}
					COMPETITOR_PRODUCT_MAPPER.mapToItem(rs, competitorProductBuilder);
					categoryExtBuilder.addCompetitorProduct(competitorProductBuilder.build());
				}
			}

			Map<Integer, SceneDAOProtos.RecommenderCategoryExt> categoryExtMap = new HashMap<Integer, SceneDAOProtos.RecommenderCategoryExt>();

			for (Entry<Integer, SceneDAOProtos.RecommenderCategoryExt.Builder> entry : categoryExtBuilderMap.entrySet()) {
				categoryExtMap.put(entry.getKey(), entry.getValue().build());
			}
			return categoryExtMap;
		} finally {
			DBUtil.closeQuietly(rs);
			DBUtil.closeQuietly(st);
		}

	}

	public static Map<Integer, List<Integer>> getCoptorRecommendProductIdListMap(Connection conn, long companyId, Collection<Integer> competitorProductIds)
			throws SQLException {

		StringBuilder sql = new StringBuilder();
		sql.append("SELECT competitor_product_id, recommend_product_id FROM weizhu_tool_recommender_competitor_recommend_product WHERE company_id = ")
				.append(companyId)
				.append(" AND competitor_product_id IN( ")
				.append(DBUtil.COMMA_JOINER.join(competitorProductIds))
				.append(") ORDER BY create_time DESC,recommend_product_id DESC;");

		Statement st = null;
		ResultSet rs = null;

		try {
			st = conn.createStatement();
			rs = st.executeQuery(sql.toString());

			Map<Integer, List<Integer>> compProdIdRecoProdIdListMap = new HashMap<Integer, List<Integer>>();
			while (rs.next()) {
				int competitorProductId = rs.getInt("competitor_product_id");
				List<Integer> recoProdIdList = compProdIdRecoProdIdListMap.get(competitorProductId);
				if (recoProdIdList == null) {
					recoProdIdList = new ArrayList<Integer>();
					compProdIdRecoProdIdListMap.put(competitorProductId, recoProdIdList);
				}
				recoProdIdList.add(rs.getInt("recommend_product_id"));
			}
			return compProdIdRecoProdIdListMap;
		} finally {
			DBUtil.closeQuietly(rs);
			DBUtil.closeQuietly(st);
		}
	}

	private static void appendStatesCondSql(StringBuilder sql, Collection<State> states) {
		if (states.isEmpty()) {
			throw new RuntimeException("states is empty");
		}
		sql.append("('");
		boolean isFirst = true;
		for (State state : states) {
			if (isFirst) {
				isFirst = false;
			} else {
				sql.append("', '");
			}
			sql.append(DBUtil.SQL_STRING_ESCAPER.escape(state.name()));
		}

		sql.append("') ");
	}

	public static Map<Integer, RecommenderRecommendProduct> getRecommendProductMap(Connection conn, long companyId, Collection<Integer> recommendProductIds,
			Collection<State> states) throws SQLException {

		String statsCondStr;
		if (states == null) {
			statsCondStr = "";
		} else {
			StringBuilder sb = new StringBuilder(" AND state IN ");
			appendStatesCondSql(sb, states);
			statsCondStr = sb.toString();
		}

		StringBuilder sql = new StringBuilder();
		sql.append("SELECT * FROM weizhu_tool_recommender_recommend_product WHERE company_id = ")
				.append(companyId)
				.append(" AND recommend_product_id IN( ")
				.append(DBUtil.COMMA_JOINER.join(recommendProductIds))
				.append(" ) ")
				.append(statsCondStr)
				.append(";");

		Statement st = null;
		ResultSet rs = null;

		try {
			st = conn.createStatement();
			rs = st.executeQuery(sql.toString());

			Map<Integer, RecommenderRecommendProduct> recommendProductMap = new HashMap<Integer, RecommenderRecommendProduct>();
			RecommenderRecommendProduct.Builder recommendProductBuilder = RecommenderRecommendProduct.newBuilder();
			while (rs.next()) {
				recommendProductBuilder.clear();
				int recommendProductId = rs.getInt("recommend_product_id");
				recommendProductMap.put(recommendProductId, RECOMMEND_PRODUCT_MAPPER.mapToItem(rs, recommendProductBuilder).build());
			}
			return recommendProductMap;
		} finally {
			DBUtil.closeQuietly(rs);
			DBUtil.closeQuietly(st);
		}
	}

	public static Map<Integer, RecommenderCompetitorProduct> getCompetitorProductMap(Connection conn, long companyId, Collection<Integer> competitorProductIds,
			Collection<State> states) throws SQLException {

		if (competitorProductIds.isEmpty()) {
			return Collections.emptyMap();
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
		sql.append("SELECT * FROM weizhu_tool_recommender_competitor_product WHERE company_id = ")
				.append(companyId)
				.append(" AND competitor_product_id IN( ")
				.append(DBUtil.COMMA_JOINER.join(competitorProductIds))
				.append(" ) ")
				.append(statsCondStr)
				.append(";");

		Statement st = null;
		ResultSet rs = null;

		try {
			st = conn.createStatement();
			rs = st.executeQuery(sql.toString());

			Map<Integer, RecommenderCompetitorProduct> competitorProductMap = new HashMap<Integer, RecommenderCompetitorProduct>();
			RecommenderCompetitorProduct.Builder competitorProductBuilder = RecommenderCompetitorProduct.newBuilder();
			while (rs.next()) {
				competitorProductBuilder.clear();
				int competitorProductId = rs.getInt("competitor_product_id");
				competitorProductMap.put(competitorProductId, COMPETITOR_PRODUCT_MAPPER.mapToItem(rs, competitorProductBuilder).build());
			}
			return competitorProductMap;
		} finally {
			DBUtil.closeQuietly(rs);
			DBUtil.closeQuietly(st);
		}
	}

	public static Integer insertCategory(Connection conn, long companyId, RecommenderCategory category) throws SQLException {

		PreparedStatement pstmt = null;
		ResultSet rs = null;

		try {
			pstmt = conn.prepareStatement("INSERT INTO weizhu_tool_recommender_category(company_id,category_name,image_name,category_desc,is_leaf_category,parent_category_id,state,create_admin_id,create_time) VALUES(?,?,?,?,?,?,?,?,?);",
					Statement.RETURN_GENERATED_KEYS);
			pstmt.setLong(1, companyId);
			DBUtil.set(pstmt, 2, category.hasCategoryName(), category.getCategoryName());
			DBUtil.set(pstmt, 3, category.hasImageName(), category.getImageName());
			DBUtil.set(pstmt, 4, category.hasCategoryDesc(), category.getCategoryDesc());
			DBUtil.set(pstmt, 5, category.hasIsLeafCategory(), category.getIsLeafCategory());
			DBUtil.set(pstmt, 6, category.hasParentCategoryId(), category.getParentCategoryId());
			DBUtil.set(pstmt, 7, category.hasState(), category.getState().name());
			DBUtil.set(pstmt, 8, category.hasCreateAdminId(), category.getCreateAdminId());
			DBUtil.set(pstmt, 9, category.hasCreateTime(), category.getCreateTime());

			pstmt.executeUpdate();
			rs = pstmt.getGeneratedKeys();
			if (rs.next()) {
				return rs.getInt(1);
			} else {
				throw new RuntimeException("db failed!");
			}
		} finally {
			DBUtil.closeQuietly(rs);
			DBUtil.closeQuietly(pstmt);
		}
	}

	public static void updateCategory(Connection conn, long companyId, int categoryId, String categoryName, @Nullable String imageName,
			@Nullable String categoryDesc, long updateAdminId, int updateTime) throws SQLException {

		PreparedStatement pstmt = null;

		try {
			pstmt = conn.prepareStatement("UPDATE  weizhu_tool_recommender_category SET category_name = ?,image_name = ?,category_desc = ?, update_admin_id = ?,update_time = ? WHERE company_id = ? AND category_id = ?;");
			
			pstmt.setString(1, categoryName);
			DBUtil.set(pstmt, 2, imageName != null, imageName);
			DBUtil.set(pstmt, 3, categoryDesc != null, categoryDesc);
			pstmt.setLong(4, updateAdminId);
			pstmt.setInt(5, updateTime);
			pstmt.setLong(6, companyId);
			pstmt.setInt(7, categoryId);

			pstmt.executeUpdate();
		} finally {
			DBUtil.closeQuietly(pstmt);
		}
	}

	public static void updateCategoryState(Connection conn, long companyId, Collection<Integer> categoryIds, State state, long updateAdminId, int updateTime)
			throws SQLException {

		if (categoryIds.isEmpty()) {
			return;
		}

		StringBuilder sql = new StringBuilder();
		sql.append("UPDATE weizhu_tool_recommender_category SET state = '")
				.append(DBUtil.SQL_STRING_ESCAPER.escape(state.name()))
				.append("', update_admin_id = ")
				.append(updateAdminId)
				.append(", update_time = ")
				.append(updateTime)
				.append(" WHERE company_id = ")
				.append(companyId)
				.append(" AND category_id IN(")
				.append(DBUtil.COMMA_JOINER.join(categoryIds))
				.append(");");

		Statement st = null;
		try {
			st = conn.createStatement();
			st.executeUpdate(sql.toString());
		} finally {
			DBUtil.closeQuietly(st);
		}
	}

	public static Integer insertCompetitorProduct(Connection conn, long companyId, RecommenderCompetitorProduct competitorProduct) throws SQLException {
		PreparedStatement pstmt = null;
		ResultSet rs = null;

		try {
			pstmt = conn.prepareStatement("INSERT INTO weizhu_tool_recommender_competitor_product(company_id,competitor_product_name,image_name,category_id,allow_model_id,state,create_admin_id,create_time) VALUES(?,?,?,?,?,?,?,?);",
					Statement.RETURN_GENERATED_KEYS);
			pstmt.setLong(1, companyId);
			DBUtil.set(pstmt, 2, competitorProduct.hasCompetitorProductName(), competitorProduct.getCompetitorProductName());
			DBUtil.set(pstmt, 3, competitorProduct.hasImageName(), competitorProduct.getImageName());
			DBUtil.set(pstmt, 4, competitorProduct.hasCategoryId(), competitorProduct.getCategoryId());
			DBUtil.set(pstmt, 5, competitorProduct.hasAllowModelId(), competitorProduct.getAllowModelId());
			DBUtil.set(pstmt, 6, competitorProduct.hasState(), competitorProduct.getState().name());
			DBUtil.set(pstmt, 7, competitorProduct.hasCreateAdminId(), competitorProduct.getCreateAdminId());
			DBUtil.set(pstmt, 8, competitorProduct.hasCreateTime(), competitorProduct.getCreateTime());

			pstmt.executeUpdate();
			rs = pstmt.getGeneratedKeys();
			if (rs.next()) {
				return rs.getInt(1);
			} else {
				throw new RuntimeException("db failed!");
			}
		} finally {
			DBUtil.closeQuietly(rs);
			DBUtil.closeQuietly(pstmt);
		}

	}

	public static void updateCompetitorProduct(Connection conn, long companyId, int competitorProductId, String competitorProductName, String imageName,
			int categoryId, @Nullable Integer allowModelId, long updateAdminId, int updateTime) throws SQLException {
		PreparedStatement pstmt = null;

		try {
			pstmt = conn.prepareStatement("UPDATE  weizhu_tool_recommender_competitor_product SET competitor_product_name = ?,image_name = ?,category_id = ?, allow_model_id = ?, update_admin_id = ?,update_time = ? WHERE company_id = ? AND competitor_product_id = ?;");
			pstmt.setString(1, competitorProductName);
			pstmt.setString(2, imageName);
			pstmt.setInt(3, categoryId);
			DBUtil.set(pstmt, 4, allowModelId != null, allowModelId == null ? 0 : allowModelId);
			pstmt.setLong(5, updateAdminId);
			pstmt.setInt(6, updateTime);
			pstmt.setLong(7, companyId);
			pstmt.setInt(8, competitorProductId);

			pstmt.executeUpdate();
		} finally {
			DBUtil.closeQuietly(pstmt);
		}
	}

	public static void updateCompetitorProductState(Connection conn, long companyId, Collection<Integer> competitorProductIds, State state, long updateAdminId,
			int updateTime) throws SQLException {

		if (competitorProductIds.isEmpty()) {
			return;
		}

		StringBuilder sql = new StringBuilder();
		sql.append("UPDATE weizhu_tool_recommender_competitor_product SET state = '")
				.append(DBUtil.SQL_STRING_ESCAPER.escape(state.name()))
				.append("', update_admin_id = ")
				.append(updateAdminId)
				.append(", update_time=")
				.append(updateTime)
				.append(" WHERE company_id = ")
				.append(companyId)
				.append(" AND competitor_product_id IN(")
				.append(DBUtil.COMMA_JOINER.join(competitorProductIds))
				.append(");");

		Statement st = null;
		try {
			st = conn.createStatement();
			st.executeUpdate(sql.toString());
		} finally {
			DBUtil.closeQuietly(st);
		}
	}

	public static Integer insertRecommendProduct(Connection conn, long companyId, RecommenderRecommendProduct recommendProduct) throws SQLException {

		StringBuilder sql = new StringBuilder("INSERT INTO weizhu_tool_recommender_recommend_product(company_id, recommend_product_name, recommend_product_desc, image_name, allow_model_id,").append(" `web_url.web_url`, `web_url.is_weizhu`,")
				.append(" `document.document_url`, `document.document_type`, `document.document_size`, `document.check_md5`, `document.is_download`, `document.is_auth_url`, ")
				.append(" `video.video_url`,  `video.video_type`, `video.video_size`, `video.video_time`, `video.check_md5`, `video.is_download`, `video.is_auth_url`, ")
				.append(" `audio.audio_url`, `audio.audio_type`, `audio.audio_size`, `audio.audio_time`, `audio.check_md5`, `audio.is_download`, `audio.is_auth_url`, ")
				.append(" `app_uri.app_uri`, state, create_admin_id, create_time) VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?) ;");

		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try {
			pstmt = conn.prepareStatement(sql.toString(), Statement.RETURN_GENERATED_KEYS);
			pstmt.setLong(1, companyId);
			pstmt.setString(2, recommendProduct.getRecommendProductName());
			pstmt.setString(3, recommendProduct.getRecommendProductDesc());
			pstmt.setString(4, recommendProduct.getImageName());

			DBUtil.set(pstmt, 5, recommendProduct.hasAllowModelId(), recommendProduct.getAllowModelId());

			DBUtil.set(pstmt, 6, recommendProduct.hasWebUrl(), recommendProduct.getWebUrl().getWebUrl());
			DBUtil.set(pstmt, 7, recommendProduct.hasWebUrl(), recommendProduct.getWebUrl().getIsWeizhu());

			DBUtil.set(pstmt, 8, recommendProduct.hasDocument(), recommendProduct.getDocument().getDocumentUrl());
			DBUtil.set(pstmt, 9, recommendProduct.hasDocument(), recommendProduct.getDocument().getDocumentType());
			DBUtil.set(pstmt, 10, recommendProduct.hasDocument(), recommendProduct.getDocument().getDocumentSize());
			DBUtil.set(pstmt, 11, recommendProduct.hasDocument() && recommendProduct.getDocument().hasCheckMd5(), recommendProduct.getDocument()
					.getCheckMd5());
			DBUtil.set(pstmt, 12, recommendProduct.hasDocument(), recommendProduct.getDocument().getIsDownload());
			DBUtil.set(pstmt, 13, recommendProduct.hasDocument(), recommendProduct.getDocument().getIsAuthUrl());

			DBUtil.set(pstmt, 14, recommendProduct.hasVideo(), recommendProduct.getVideo().getVideoUrl());
			DBUtil.set(pstmt, 15, recommendProduct.hasVideo(), recommendProduct.getVideo().getVideoType());
			DBUtil.set(pstmt, 16, recommendProduct.hasVideo(), recommendProduct.getVideo().getVideoSize());
			DBUtil.set(pstmt, 17, recommendProduct.hasVideo(), recommendProduct.getVideo().getVideoTime());
			DBUtil.set(pstmt, 18, recommendProduct.hasVideo() && recommendProduct.getVideo().hasCheckMd5(), recommendProduct.getVideo().getCheckMd5());
			DBUtil.set(pstmt, 19, recommendProduct.hasVideo(), recommendProduct.getVideo().getIsDownload());
			DBUtil.set(pstmt, 20, recommendProduct.hasVideo(), recommendProduct.getVideo().getIsAuthUrl());

			DBUtil.set(pstmt, 21, recommendProduct.hasAudio(), recommendProduct.getAudio().getAudioUrl());
			DBUtil.set(pstmt, 22, recommendProduct.hasAudio(), recommendProduct.getAudio().getAudioType());
			DBUtil.set(pstmt, 23, recommendProduct.hasAudio(), recommendProduct.getAudio().getAudioSize());
			DBUtil.set(pstmt, 24, recommendProduct.hasAudio(), recommendProduct.getAudio().getAudioTime());
			DBUtil.set(pstmt, 25, recommendProduct.hasAudio() && recommendProduct.getAudio().hasCheckMd5(), recommendProduct.getAudio().getCheckMd5());
			DBUtil.set(pstmt, 26, recommendProduct.hasAudio(), recommendProduct.getAudio().getIsDownload());
			DBUtil.set(pstmt, 27, recommendProduct.hasAudio(), recommendProduct.getAudio().getIsAuthUrl());

			DBUtil.set(pstmt, 28, recommendProduct.hasAppUri(), recommendProduct.getAppUri().getAppUri());

			pstmt.setString(29, recommendProduct.getState().name());
			pstmt.setLong(30, recommendProduct.getCreateAdminId());
			pstmt.setInt(31, recommendProduct.getCreateTime());

			pstmt.executeUpdate();
			rs = pstmt.getGeneratedKeys();
			if (rs.next()) {
				return rs.getInt(1);
			} else {
				throw new RuntimeException("DB FAILED");
			}
		} finally {
			DBUtil.closeQuietly(rs);
			DBUtil.closeQuietly(pstmt);

		}
	}

	public static void updateRecommendProduct(Connection conn, long companyId, RecommenderRecommendProduct recommendProduct) throws SQLException {
		StringBuilder sql = new StringBuilder("UPDATE weizhu_tool_recommender_recommend_product SET recommend_product_name = ?, recommend_product_desc = ?, image_name = ?, allow_model_id = ?,").append(" `web_url.web_url` = ?, `web_url.is_weizhu` = ?,")
				.append(" `document.document_url` = ?, `document.document_type` = ?, `document.document_size` = ?, `document.check_md5` = ?, `document.is_download` = ?, `document.is_auth_url` = ?, ")
				.append(" `video.video_url` = ?,  `video.video_type` = ?, `video.video_size` = ?, `video.video_time` = ?, `video.check_md5` = ?, `video.is_download` = ?, `video.is_auth_url` = ?, ")
				.append(" `audio.audio_url` = ?, `audio.audio_type` = ?, `audio.audio_size` = ?, `audio.audio_time` = ?, `audio.check_md5` = ?, `audio.is_download` = ?, `audio.is_auth_url` = ?, ")
				.append(" `app_uri.app_uri` = ?, update_admin_id = ?, update_time = ? WHERE company_id = ? AND recommend_product_id = ?;");

		PreparedStatement pstmt = null;
		try {
			pstmt = conn.prepareStatement(sql.toString());
			pstmt.setString(1, recommendProduct.getRecommendProductName());
			pstmt.setString(2, recommendProduct.getRecommendProductDesc());
			pstmt.setString(3, recommendProduct.getImageName());
			DBUtil.set(pstmt, 4, recommendProduct.hasAllowModelId(), recommendProduct.getAllowModelId());
			DBUtil.set(pstmt, 5, recommendProduct.hasWebUrl(), recommendProduct.getWebUrl().getWebUrl());
			DBUtil.set(pstmt, 6, recommendProduct.hasWebUrl(), recommendProduct.getWebUrl().getIsWeizhu());

			DBUtil.set(pstmt, 7, recommendProduct.hasDocument(), recommendProduct.getDocument().getDocumentUrl());
			DBUtil.set(pstmt, 8, recommendProduct.hasDocument(), recommendProduct.getDocument().getDocumentType());
			DBUtil.set(pstmt, 9, recommendProduct.hasDocument(), recommendProduct.getDocument().getDocumentSize());
			DBUtil.set(pstmt, 10, recommendProduct.hasDocument() && recommendProduct.getDocument().hasCheckMd5(), recommendProduct.getDocument()
					.getCheckMd5());
			DBUtil.set(pstmt, 11, recommendProduct.hasDocument(), recommendProduct.getDocument().getIsDownload());
			DBUtil.set(pstmt, 12, recommendProduct.hasDocument(), recommendProduct.getDocument().getIsAuthUrl());

			DBUtil.set(pstmt, 13, recommendProduct.hasVideo(), recommendProduct.getVideo().getVideoUrl());
			DBUtil.set(pstmt, 14, recommendProduct.hasVideo(), recommendProduct.getVideo().getVideoType());
			DBUtil.set(pstmt, 15, recommendProduct.hasVideo(), recommendProduct.getVideo().getVideoSize());
			DBUtil.set(pstmt, 16, recommendProduct.hasVideo(), recommendProduct.getVideo().getVideoTime());
			DBUtil.set(pstmt, 17, recommendProduct.hasVideo() && recommendProduct.getVideo().hasCheckMd5(), recommendProduct.getVideo().getCheckMd5());
			DBUtil.set(pstmt, 18, recommendProduct.hasVideo(), recommendProduct.getVideo().getIsDownload());
			DBUtil.set(pstmt, 19, recommendProduct.hasVideo(), recommendProduct.getVideo().getIsAuthUrl());

			DBUtil.set(pstmt, 20, recommendProduct.hasAudio(), recommendProduct.getAudio().getAudioUrl());
			DBUtil.set(pstmt, 21, recommendProduct.hasAudio(), recommendProduct.getAudio().getAudioType());
			DBUtil.set(pstmt, 22, recommendProduct.hasAudio(), recommendProduct.getAudio().getAudioSize());
			DBUtil.set(pstmt, 23, recommendProduct.hasAudio(), recommendProduct.getAudio().getAudioTime());
			DBUtil.set(pstmt, 24, recommendProduct.hasAudio() && recommendProduct.getAudio().hasCheckMd5(), recommendProduct.getAudio().getCheckMd5());
			DBUtil.set(pstmt, 25, recommendProduct.hasAudio(), recommendProduct.getAudio().getIsDownload());
			DBUtil.set(pstmt, 26, recommendProduct.hasAudio(), recommendProduct.getAudio().getIsAuthUrl());

			DBUtil.set(pstmt, 27, recommendProduct.hasAppUri(), recommendProduct.getAppUri().getAppUri());

			pstmt.setLong(28, recommendProduct.getUpdateAdminId());
			pstmt.setInt(29, recommendProduct.getUpdateTime());
			pstmt.setLong(30, companyId);
			pstmt.setLong(31, recommendProduct.getRecommendProductId());

			pstmt.executeUpdate();

		} finally {
			DBUtil.closeQuietly(pstmt);
		}

	}

	public static void updateRecommendProductState(Connection conn, long companyId, Collection<Integer> recommendProductIds, State state, long updateAdminId,
			int updateTime) throws SQLException {

		if (recommendProductIds.isEmpty()) {
			return;
		}

		StringBuilder sql = new StringBuilder();
		sql.append("UPDATE weizhu_tool_recommender_recommend_product SET state = '")
				.append(DBUtil.SQL_STRING_ESCAPER.escape(state.name()))
				.append("', update_admin_id = ")
				.append(updateAdminId)
				.append(", update_time=")
				.append(updateTime)
				.append(" WHERE company_id = ")
				.append(companyId)
				.append(" AND recommend_product_id IN(")
				.append(DBUtil.COMMA_JOINER.join(recommendProductIds))
				.append(");");

		Statement st = null;
		try {
			st = conn.createStatement();
			st.executeUpdate(sql.toString());
		} finally {
			DBUtil.closeQuietly(st);
		}

	}

	public static void insertRecommendProdToCompetitorProd(Connection conn, long companyId, int competitorProductId, Collection<Integer> recommendProductIds,
			long createAdminId, int createTime) throws SQLException {
		if (recommendProductIds.isEmpty()) {
			return;
		}
		PreparedStatement pstmt = null;
		try {
			pstmt = conn.prepareStatement("INSERT IGNORE weizhu_tool_recommender_competitor_recommend_product (company_id, competitor_product_id, recommend_product_id, create_admin_id, create_time) VALUES (?,?,?,?,?) ;");
			for (int recommendProductId : recommendProductIds) {
				pstmt.setLong(1, companyId);
				pstmt.setInt(2, competitorProductId);
				pstmt.setLong(3, recommendProductId);
				pstmt.setLong(4, createAdminId);
				pstmt.setInt(5, createTime);
				pstmt.addBatch();
			}
			pstmt.executeBatch();
		} finally {
			DBUtil.closeQuietly(pstmt);
		}
	}

	public static void deleteRecommendProdFromCompetitorProd(Connection conn, long companyId, int competitorProductId, Collection<Integer> recommendProductIds)
			throws SQLException {

		if (recommendProductIds.isEmpty()) {
			return;
		}
		StringBuilder sql = new StringBuilder();
		sql.append("DELETE FROM weizhu_tool_recommender_competitor_recommend_product WHERE company_id = ")
				.append(companyId)
				.append(" AND competitor_product_id = ")
				.append(competitorProductId)
				.append(" AND recommend_product_id IN(")
				.append(DBUtil.COMMA_JOINER.join(recommendProductIds))
				.append(");");

		Statement st = null;
		try {

			st = conn.createStatement();

			st.executeUpdate(sql.toString());
		} finally {
			DBUtil.closeQuietly(st);
		}
	}

	/**
	 * 根据competitorProductIds，删除竞品和推荐品直接的关联关系
	 * 
	 * @param conn
	 * @param competitorProductIds
	 * @throws SQLException
	 */
	public static void deleteRecommendProdFromCompetitorProd(Connection conn, long companyId, Collection<Integer> competitorProductIds) throws SQLException {

		if (competitorProductIds.isEmpty()) {
			return;
		}

		StringBuilder sql = new StringBuilder();
		sql.append("DELETE FROM weizhu_tool_recommender_competitor_recommend_product WHERE company_id = ")
				.append(companyId)
				.append(" AND competitor_product_id IN(")
				.append(DBUtil.COMMA_JOINER.join(competitorProductIds))
				.append(");");
		PreparedStatement pstmt = null;
		try {

			pstmt = conn.prepareStatement(sql.toString());

			pstmt.executeUpdate();
		} finally {
			DBUtil.closeQuietly(pstmt);
		}
	}

	public static void insertRecommendProductPriceUrl(Connection conn, long companyId, int recommendProductId, Collection<AdminSceneProtos.PriceWebUrlCreateCondition> priceWebUrlCreateConditions,
			long createAdminId, int createTime) throws SQLException {
		if (priceWebUrlCreateConditions.isEmpty()) {
			return;
		}
		PreparedStatement pstmt = null;
		try {

			pstmt = conn.prepareStatement("INSERT IGNORE weizhu_tool_recommender_price_url (company_id, recommend_product_id, url_name, url_content, image_name,is_weizhu, create_admin_id, create_time) VALUES (?,?,?,?,?,?,?,?) ;");
			for (PriceWebUrlCreateCondition priceWebUrlCreateCondition : priceWebUrlCreateConditions) {
				pstmt.setLong(1, companyId);
				pstmt.setInt(2, recommendProductId);
				pstmt.setString(3, priceWebUrlCreateCondition.getUrlName());
				pstmt.setString(4, priceWebUrlCreateCondition.getUrlContent());
				pstmt.setString(5, priceWebUrlCreateCondition.getImageName());
				pstmt.setBoolean(6, priceWebUrlCreateCondition.getIsWeizhu());
				pstmt.setLong(7, createAdminId);
				pstmt.setInt(8, createTime);
				pstmt.addBatch();
			}
			pstmt.executeBatch();
		} finally {
			DBUtil.closeQuietly(pstmt);
		}
	}

	public static void updateRecommendProductPriceUrl(Connection conn, long companyId, Collection<RecommenderPriceWebUrl> priceWebUrls) throws SQLException {
		if (priceWebUrls.isEmpty()) {
			return;
		}
		PreparedStatement pstmt = null;
		try {

			pstmt = conn.prepareStatement("UPDATE weizhu_tool_recommender_price_url SET recommend_product_id=?, url_name=?, url_content=?, image_name=?,is_weizhu=? WHERE company_id = ? AND url_id=? ;");
			for (RecommenderPriceWebUrl priceWebUrl : priceWebUrls) {
				pstmt.setInt(1, priceWebUrl.getRecommendProductId());
				pstmt.setString(2, priceWebUrl.getUrlName());
				pstmt.setString(3, priceWebUrl.getUrlContent());
				pstmt.setString(4, priceWebUrl.getImageName());
				DBUtil.set(pstmt, 5, priceWebUrl.hasIsWeizhu(), priceWebUrl.getIsWeizhu());
				pstmt.setLong(6, companyId);
				pstmt.setInt(7, priceWebUrl.getUrlId());
				pstmt.addBatch();
			}
			pstmt.executeBatch();
		} finally {
			DBUtil.closeQuietly(pstmt);
		}
	}

	public static void deletePriceUrlByUrlIds(Connection conn, long companyId, Collection<Integer> urlIds) throws SQLException {
		if (urlIds.isEmpty()) {
			return;
		}

		StringBuilder sql = new StringBuilder();
		sql.append("DELETE FROM weizhu_tool_recommender_price_url WHERE company_id = ")
				.append(companyId)
				.append(" AND url_id IN(").append(DBUtil.COMMA_JOINER.join(urlIds)).append(")");
		Statement st = null;
		try {

			st = conn.createStatement();

			st.executeUpdate(sql.toString());
		} finally {
			DBUtil.closeQuietly(st);
		}
	}

	public static void deletePriceUrlByRecommendProdIds(Connection conn, long companyId, Collection<Integer> recommendProdIds) throws SQLException {
		if (recommendProdIds.isEmpty()) {
			return;
		}

		StringBuilder sql = new StringBuilder();
		sql.append("DELETE FROM weizhu_tool_recommender_price_url WHERE company_id = ")
				.append(companyId)
				.append(" AND recommend_product_id IN(")
				.append(DBUtil.COMMA_JOINER.join(recommendProdIds))
				.append(")");
		Statement st = null;
		try {

			st = conn.createStatement();

			st.executeUpdate(sql.toString());
		} finally {
			DBUtil.closeQuietly(st);
		}
	}

	public static DataPage<RecommenderCompetitorProduct> getCompetitorProduct(Connection conn, long companyId, @Nullable Integer categoryId,
			@Nullable String competitorProductName, @Nullable Integer start, int length, Collection<State> states) throws SQLException {

		if (length <= 0 || (states != null && states.isEmpty())) {
			return new DataPage<RecommenderCompetitorProduct>(Collections.<RecommenderCompetitorProduct> emptyList(), 0, 0);
		}
		start = start == null ? 0 : start < 0 ? 0 : start;

		String statsCondStr;
		if (states == null) {
			statsCondStr = "";
		} else {
			StringBuilder sb = new StringBuilder(" AND state IN ");
			appendStatesCondSql(sb, states);
			statsCondStr = sb.toString();
		}

		StringBuilder sql = new StringBuilder();
		sql.append("SELECT * FROM weizhu_tool_recommender_competitor_product WHERE company_id = ")
				.append(companyId)
				.append(statsCondStr);
		if (categoryId != null) {
			sql.append(" AND category_id = ").append(categoryId);
		}
		if (competitorProductName != null) {
			sql.append(" AND competitor_product_name LIKE  '%").append(DBUtil.SQL_LIKE_STRING_ESCAPER.escape(competitorProductName)).append("%'");
		}
		sql.append(" ORDER BY create_time DESC, competitor_product_id DESC LIMIT ").append(start).append(",").append(length).append(";");

		// 获取filtered_size
		sql.append("SELECT count(*) FROM weizhu_tool_recommender_competitor_product WHERE company_id = ")
				.append(companyId)
				.append(statsCondStr);
		if (categoryId != null) {
			sql.append(" AND category_id = ").append(categoryId);
		}
		if (competitorProductName != null) {
			sql.append(" AND competitor_product_name LIKE  '%").append(DBUtil.SQL_LIKE_STRING_ESCAPER.escape(competitorProductName)).append("%'");
		}
		sql.append(";");

		// 获取total_size
		sql.append("SELECT count(*) FROM weizhu_tool_recommender_competitor_product WHERE company_id = ")
				.append(companyId)
				.append(statsCondStr).append(";");

		Statement st = null;
		ResultSet rs = null;
		try {
			st = conn.createStatement();
			rs = st.executeQuery(sql.toString());
			List<RecommenderCompetitorProduct> competitorProducts = new ArrayList<RecommenderCompetitorProduct>();
			RecommenderCompetitorProduct.Builder competitorProductBuilder = RecommenderCompetitorProduct.newBuilder();
			while (rs.next()) {
				competitorProductBuilder.clear();
				competitorProducts.add(COMPETITOR_PRODUCT_MAPPER.mapToItem(rs, competitorProductBuilder).build());
			}

			DBUtil.closeQuietly(rs);
			rs = null;
			st.getMoreResults();
			rs = st.getResultSet();
			int filteredSize = 0;
			if (rs.next()) {
				filteredSize = rs.getInt(1);
			}

			DBUtil.closeQuietly(rs);
			rs = null;
			st.getMoreResults();
			rs = st.getResultSet();
			int totalSize = 0;
			if (rs.next()) {
				totalSize = rs.getInt(1);
			}
			return new DataPage<RecommenderCompetitorProduct>(competitorProducts, totalSize, filteredSize);
		} finally {
			DBUtil.closeQuietly(rs);
			DBUtil.closeQuietly(st);
		}
	}

	public static Map<Integer, List<RecommenderPriceWebUrl>> getRecommendProductIdPriceUrlsMap(Connection conn, long companyId, 
			Collection<Integer> recommendProductIds) throws SQLException {

		StringBuilder sql = new StringBuilder();
		sql.append("SELECT * FROM weizhu_tool_recommender_price_url WHERE company_id = ")
				.append(companyId).append(" AND recommend_product_id IN(")
				.append(DBUtil.COMMA_JOINER.join(recommendProductIds))
				.append(");");
		Statement st = null;
		ResultSet rs = null;
		try {
			st = conn.createStatement();
			rs = st.executeQuery(sql.toString());
			Map<Integer, List<RecommenderPriceWebUrl>> productIdwebUrlsMap = new HashMap<Integer, List<RecommenderPriceWebUrl>>();
			RecommenderPriceWebUrl.Builder webUrlBuilder = RecommenderPriceWebUrl.newBuilder();
			while (rs.next()) {
				int productId = rs.getInt("recommend_product_id");
				List<RecommenderPriceWebUrl> WebUrls = productIdwebUrlsMap.get(productId);
				if (WebUrls == null) {
					WebUrls = new ArrayList<RecommenderPriceWebUrl>();
					productIdwebUrlsMap.put(productId, WebUrls);
				}
				webUrlBuilder.clear();
				WebUrls.add(PRICE_URL_MAPPER.mapToItem(rs, webUrlBuilder).build());
			}
			return productIdwebUrlsMap;
		} finally {
			DBUtil.closeQuietly(rs);
			DBUtil.closeQuietly(st);
		}
	}

	public static DataPage<Integer> getRecommendProduct(Connection conn, long companyId, @Nullable Integer competitorProductId,
			@Nullable String recommendProductName, @Nullable Integer start, int length, Collection<State> states) throws SQLException {

		String statsCondStr;
		if (states == null) {
			statsCondStr = "";
		} else {
			StringBuilder sb = new StringBuilder(" AND state IN ");
			appendStatesCondSql(sb, states);
			statsCondStr = sb.toString();
		}

		start = (start == null || start < 0) ? 0 : start;
		length = length < 0 ? 0 : length;
		StringBuilder sql = new StringBuilder();

		if (recommendProductName != null) {
			sql.append("SELECT recommend_product_id FROM weizhu_tool_recommender_recommend_product WHERE company_id = ")
					.append(companyId)
					.append(" AND recommend_product_name LIKE '%")
					.append(DBUtil.SQL_LIKE_STRING_ESCAPER.escape(recommendProductName))
					.append("%' ")
					.append(statsCondStr)
					.append(" ORDER BY create_time DESC,recommend_product_id DESC LIMIT ")
					.append(start)
					.append(",")
					.append(length)
					.append(";");
			// fitered_size
			sql.append("SELECT recommend_product_id FROM weizhu_tool_recommender_recommend_product WHERE company_id = ")
					.append(companyId)
					.append(" AND recommend_product_name LIKE '%")
					.append(DBUtil.SQL_LIKE_STRING_ESCAPER.escape(recommendProductName))
					.append("%' ")
					.append(statsCondStr)
					.append(" ORDER BY create_time DESC,recommend_product_id DESC ;");
		} else if (competitorProductId != null) {
			// weizhu_tool_recommender_competitor_recommend_product表中没有state字段，所以无需过滤
			sql.append("SELECT recommend_product_id FROM weizhu_tool_recommender_competitor_recommend_product WHERE company_id = ")
					.append(companyId)
					.append(" AND competitor_product_id = ")
					.append(competitorProductId)
					.append(" ORDER BY create_time DESC,recommend_product_id DESC LIMIT ")
					.append(start)
					.append(",")
					.append(length)
					.append(";");
			// fitered_size
			sql.append("SELECT recommend_product_id FROM weizhu_tool_recommender_competitor_recommend_product WHERE company_id = ")
					.append(companyId)
					.append(" AND competitor_product_id = ")
					.append(competitorProductId)
					.append(" ORDER BY create_time DESC,recommend_product_id DESC ;");
		} else {
			sql.append("SELECT recommend_product_id FROM weizhu_tool_recommender_recommend_product WHERE company_id = ")
					.append(companyId)
					.append(statsCondStr)
					.append(" ORDER BY create_time DESC,recommend_product_id DESC LIMIT ")
					.append(start)
					.append(",")
					.append(length)
					.append(";");
			// fitered_size
			sql.append("SELECT count(recommend_product_id) FROM weizhu_tool_recommender_recommend_product WHERE company_id = ")
					.append(companyId)
					.append(statsCondStr)
					.append(" ORDER BY create_time DESC,recommend_product_id DESC ;");
		}

		// total_size
		sql.append("SELECT count(recommend_product_id) FROM weizhu_tool_recommender_recommend_product WHERE company_id = ")
				.append(companyId)
				.append(statsCondStr)
				.append(" ORDER BY create_time DESC,recommend_product_id DESC ;");

		Statement st = null;
		ResultSet rs = null;
		try {
			st = conn.createStatement();
			rs = st.executeQuery(sql.toString());
			List<Integer> recommendProductIds = new ArrayList<Integer>();
			while (rs.next()) {
				recommendProductIds.add(rs.getInt("recommend_product_id"));
			}

			DBUtil.closeQuietly(rs);
			rs = null;
			st.getMoreResults();
			rs = st.getResultSet();

			int filteredSize = 0;
			if (rs.next()) {
				filteredSize = rs.getInt(1);
			}

			DBUtil.closeQuietly(rs);
			rs = null;
			st.getMoreResults();
			rs = st.getResultSet();

			int totalSize = 0;
			if (rs.next()) {
				totalSize = rs.getInt(1);
			}
			return new DataPage<Integer>(recommendProductIds, totalSize, filteredSize);
		} finally {
			DBUtil.closeQuietly(rs);
			DBUtil.closeQuietly(st);
		}
	}

	public static Map<Integer, RecommenderCategory> getCategoryMapByCategoryIds(Connection conn, long companyId, Collection<Integer> categoryIds,
			Collection<State> states) throws SQLException {

		if (categoryIds.isEmpty()) {
			return Collections.emptyMap();
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
		sql.append("SELECT * FROM weizhu_tool_recommender_category WHERE company_id = ")
				.append(companyId)
				.append(" AND category_id IN(")
				.append(DBUtil.COMMA_JOINER.join(categoryIds))
				.append(")")
				.append(statsCondStr)
				.append(";");

		Statement st = null;
		ResultSet rs = null;
		try {
			st = conn.createStatement();
			rs = st.executeQuery(sql.toString());
			Map<Integer, RecommenderCategory> categoryMap = new HashMap<Integer, RecommenderCategory>();
			RecommenderCategory.Builder categoryBuilder = RecommenderCategory.newBuilder();
			while (rs.next()) {
				categoryBuilder.clear();
				categoryMap.put(rs.getInt("category_id"), CATEGORY_MAPPER.mapToItem(rs, categoryBuilder).build());
			}
			return categoryMap;
		} finally {
			DBUtil.closeQuietly(rs);
			DBUtil.closeQuietly(st);
		}
	}

	public static void updateCompetitorProductCategory(Connection conn, long companyId, int categoryId, Collection<Integer> competitorProductIds, long updateAdminId,
			int updateTime) throws SQLException {
		if (competitorProductIds.isEmpty()) {
			return;
		}

		StringBuilder sql = new StringBuilder();
		sql.append("UPDATE weizhu_tool_recommender_competitor_product SET category_id = ")
				.append(categoryId)
				.append(", update_admin_id = ")
				.append(updateAdminId)
				.append(", update_time=")
				.append(updateTime)
				.append(" WHERE company_id = ")
				.append(companyId)
				.append(" AND competitor_product_id IN(")
				.append(DBUtil.COMMA_JOINER.join(competitorProductIds))
				.append(");");

		Statement st = null;
		try {
			st = conn.createStatement();
			st.executeUpdate(sql.toString());
		} finally {
			DBUtil.closeQuietly(st);
		}
	}

	public static Map<Integer, Set<Integer>> getChildrenCategoryIds(Connection conn, long companyId, Collection<Integer> parentCategoryIds, Collection<State> states)
			throws SQLException {
		if (parentCategoryIds.isEmpty()) {
			return Collections.emptyMap();
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
		sql.append("SELECT category_id,parent_category_id FROM weizhu_tool_recommender_category WHERE company_id = ")
				.append(companyId)
				.append(" AND parent_category_id IN(")
				.append(DBUtil.COMMA_JOINER.join(parentCategoryIds))
				.append(")")
				.append(statsCondStr)
				.append(";");

		Statement st = null;
		ResultSet rs = null;
		try {
			st = conn.createStatement();
			rs = st.executeQuery(sql.toString());
			Map<Integer, Set<Integer>> parentCateIdChildrenCateIdsMap = new HashMap<Integer, Set<Integer>>();
			while (rs.next()) {
				int parentCategoryId = rs.getInt("parent_category_id");
				Set<Integer> childrenCateIds = parentCateIdChildrenCateIdsMap.get(parentCategoryId);
				if (childrenCateIds == null) {
					childrenCateIds = new TreeSet<Integer>();
					parentCateIdChildrenCateIdsMap.put(parentCategoryId, childrenCateIds);
				}
				childrenCateIds.add(rs.getInt("category_id"));
			}
			return parentCateIdChildrenCateIdsMap;
		} finally {
			DBUtil.closeQuietly(rs);
			DBUtil.closeQuietly(st);
		}
	}

	public static void updateCategoryIsLeafCategory(Connection conn, long companyId, int parentCategoryId, boolean isLeafCategory, long updateAdminId, int updateTime)
			throws SQLException {

		PreparedStatement pstmt = null;

		try {
			pstmt = conn.prepareStatement("UPDATE weizhu_tool_recommender_category SET is_leaf_category = ?, update_admin_id = ?, update_time = ? WHERE company_id = ? AND category_id = ?; ");

			pstmt.setBoolean(1, isLeafCategory);
			pstmt.setLong(2, updateAdminId);
			pstmt.setInt(3, updateTime);
			pstmt.setLong(4, companyId);
			pstmt.setInt(5, parentCategoryId);

			pstmt.executeUpdate();
		} finally {
			DBUtil.closeQuietly(pstmt);
		}
	}

	public static Set<Integer> getCompProdIdsByCategoryIds(Connection conn, long companyId, Set<Integer> categoryIds, Collection<State> states) throws SQLException {
		if (categoryIds.isEmpty()) {
			return Collections.emptySet();
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
		sql.append("SELECT competitor_product_id FROM weizhu_tool_recommender_competitor_product WHERE company_id = ")
				.append(companyId)
				.append(" AND category_id IN(")
				.append(DBUtil.COMMA_JOINER.join(categoryIds))
				.append(")")
				.append(statsCondStr)
				.append(";");

		Statement st = null;
		ResultSet rs = null;
		try {
			st = conn.createStatement();
			rs = st.executeQuery(sql.toString());
			Set<Integer> productIds = new TreeSet<Integer>();
			while (rs.next()) {
				productIds.add(rs.getInt("competitor_product_id"));
			}
			return productIds;
		} finally {
			DBUtil.closeQuietly(rs);
			DBUtil.closeQuietly(st);
		}
	}

	public static Set<Integer> getCategoryIdsByCompetitorProductIds(Connection conn, long companyId, Collection<Integer> competitorProductIds) throws SQLException {
		if (competitorProductIds.isEmpty()) {
			return Collections.emptySet();
		}

		StringBuilder sql = new StringBuilder();
		sql.append("SELECT category_id FROM weizhu_tool_recommender_competitor_product WHERE company_id = ")
				.append(companyId)
				.append(" AND competitor_product_id IN(")
				.append(DBUtil.COMMA_JOINER.join(competitorProductIds))
				.append(");");

		Statement st = null;
		ResultSet rs = null;
		try {
			st = conn.createStatement();
			rs = st.executeQuery(sql.toString());
			Set<Integer> categoryIds = new TreeSet<Integer>();
			while (rs.next()) {
				categoryIds.add(rs.getInt("category_id"));
			}
			return categoryIds;
		} finally {
			DBUtil.closeQuietly(rs);
			DBUtil.closeQuietly(st);
		}
	}

	public static Set<Integer> getParentCategoryIds(Connection conn, long companyId, Collection<Integer> categoryIds) throws SQLException {
		
		if (categoryIds.isEmpty()) {
			return Collections.emptySet();
		}

		StringBuilder sql = new StringBuilder();
		sql.append("SELECT parent_category_id FROM weizhu_tool_recommender_category WHERE company_id = ")
				.append(companyId)
				.append(" AND category_id IN(")
				.append(DBUtil.COMMA_JOINER.join(categoryIds))
				.append(") AND parent_category_id IS NOT null;");

		Statement st = null;
		ResultSet rs = null;
		try {
			st = conn.createStatement();
			rs = st.executeQuery(sql.toString());
			Set<Integer> parentCategoryIds = new TreeSet<Integer>();
			while (rs.next()) {
				parentCategoryIds.add(rs.getInt("parent_category_id"));
			}
			return parentCategoryIds;
		} finally {
			DBUtil.closeQuietly(rs);
			DBUtil.closeQuietly(st);
		}
	}

}
