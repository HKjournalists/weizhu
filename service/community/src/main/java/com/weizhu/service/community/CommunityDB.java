package com.weizhu.service.community;

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
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Queue;
import java.util.Set;
import java.util.TreeSet;

import javax.annotation.Nullable;

import com.weizhu.common.db.DBUtil;
import com.weizhu.common.db.ProtobufMapper;
import com.weizhu.common.utils.DataPage;
import com.weizhu.proto.CommunityProtos;
import com.weizhu.proto.CommunityProtos.Comment;
import com.weizhu.proto.CommunityProtos.Comment.State;
import com.weizhu.proto.CommunityProtos.PostLike;

public class CommunityDB {

	private static final ProtobufMapper<CommunityProtos.Board> BOARD_MAPPER = ProtobufMapper.createMapper(CommunityProtos.Board.getDefaultInstance(),
			"board_id",
			"board_name",
			"board_icon",
			"board_desc",
			"parent_board_id",
			"is_leaf_board",
			"is_hot",
			"allow_model_id");
	private static final ProtobufMapper<CommunityProtos.Post> POST_MAPPER = ProtobufMapper.createMapper(CommunityProtos.Post.getDefaultInstance(),
			"post_id",
			"post_title",
			"board_id",
			"create_user_id",
			"create_time",
			"is_hot",
			"state",
			"is_sticky",
			"sticky_time",
			"is_recommend",
			"recommend_time");

	private static final ProtobufMapper<CommunityDAOProtos.RecommendPostListIndex> RECOMMEND_POST_INDEX_MAPPER = ProtobufMapper.createMapper(CommunityDAOProtos.RecommendPostListIndex.getDefaultInstance(),
			"post_id",
			"recommend_time",
			"state");

	private static final ProtobufMapper<PostLike> POST_LIKE_MAPPER = ProtobufMapper.createMapper(PostLike.getDefaultInstance(),
			"post_id",
			"user_id",
			"create_time");
	
	//	private static final ProtobufMapper<CommunityDAOProtos.StickyPostListIndex> STICKY_POST_INDEX_MAPPER = ProtobufMapper.createMapper(CommunityDAOProtos.StickyPostListIndex.getDefaultInstance(),
	//			"post_id",
	//			"create_time",
	//			"state",
	//			"is_sticky",
	//			"sticky_time");

	/**
	 * 获取社区信息，包括社区名称，板块列表和推荐贴的索引列表
	 * 
	 * @param conn
	 * @param companyId
	 * @return
	 * @throws SQLException
	 * CommunityDAOProtos.CommunityInfo
	 * @throws
	 */
	public static CommunityDAOProtos.CommunityInfo getCommunityInfo(Connection conn, long companyId) throws SQLException {
		
		StringBuilder sql = new StringBuilder();
		sql.append("SELECT * FROM weizhu_community_board WHERE company_id = ")
				.append(companyId)
				.append(" ORDER BY board_id;")
				.append("SELECT * FROM weizhu_community WHERE company_id = ")
				.append(companyId)
				.append("; SELECT post_id,recommend_time,state FROM weizhu_community_post WHERE company_id = ")
				.append(companyId)
				.append(" AND is_recommend = 1 ORDER BY recommend_time DESC,post_id DESC  LIMIT 100;");
		Statement stmt = null;
		ResultSet rs = null;
		try {
			stmt = conn.createStatement();
			stmt.execute(sql.toString());

			rs = stmt.getResultSet();

			Map<Integer, CommunityProtos.Board> boardMap = new LinkedHashMap<Integer, CommunityProtos.Board>(); // 注意保持db里的顺序

			CommunityProtos.Board.Builder tmpBoardBuilder = CommunityProtos.Board.newBuilder();
			while (rs.next()) {
				tmpBoardBuilder.clear();

				BOARD_MAPPER.mapToItem(rs, tmpBoardBuilder);
				tmpBoardBuilder.setPostTotalCount(0);
				tmpBoardBuilder.setPostNewCount(0);

				boardMap.put(tmpBoardBuilder.getBoardId(), tmpBoardBuilder.build());
			}

			DBUtil.closeQuietly(rs);
			rs = null;

			stmt.getMoreResults();
			rs = stmt.getResultSet();

			String communityName;
			String boardIdOrderStr;
			if (rs.next()) {
				communityName = rs.getString("community_name");
				boardIdOrderStr = rs.getString("board_id_order_str");
			} else {
				communityName = "社区";
				boardIdOrderStr = "";
			}

			CommunityDAOProtos.CommunityInfo.Builder communityInfoBuilder = CommunityDAOProtos.CommunityInfo.newBuilder();
			communityInfoBuilder.setCommunityName(communityName);

			List<Integer> boardIdOrderList;
			if (boardIdOrderStr == null || boardIdOrderStr.isEmpty()) {
				boardIdOrderList = Collections.emptyList();
			} else {
				List<String> boardIdStrList = DBUtil.COMMA_SPLITTER.splitToList(boardIdOrderStr);
				boardIdOrderList = new ArrayList<Integer>(boardIdStrList.size());
				for (String boardIdStr : boardIdStrList) {
					try {
						boardIdOrderList.add(Integer.parseInt(boardIdStr));
					} catch (NumberFormatException e) {
						// ignore
					}
				}
			}

			// 根据db里的顺序构建树结构

			// 根板块id
			List<Integer> rootBoardIdList = new ArrayList<Integer>();
			// 父板块id -> 子板块id列表
			Map<Integer, List<Integer>> parentBoardIdToSubIdListMap = new HashMap<Integer, List<Integer>>();
			// 用来标记板块id是否处理过，避免同一板块被处理多次
			Set<Integer> usedBoardIdSet = new TreeSet<Integer>();

			for (Integer id : boardIdOrderList) {
				if (usedBoardIdSet.contains(id)) {
					continue;
				}

				CommunityProtos.Board board = boardMap.get(id);
				if (board == null) {
					continue;
				}

				if (board.hasParentBoardId()) {
					List<Integer> subList = parentBoardIdToSubIdListMap.get(board.getParentBoardId());
					if (subList == null) {
						subList = new ArrayList<Integer>();
						parentBoardIdToSubIdListMap.put(board.getParentBoardId(), subList);
					}
					subList.add(id);
				} else {
					rootBoardIdList.add(id);
				}

				usedBoardIdSet.add(id);
			}

			// 处理order str没有的板块id
			for (CommunityProtos.Board board : boardMap.values()) {
				if (usedBoardIdSet.contains(board.getBoardId())) {
					continue;
				}

				if (board.hasParentBoardId()) {
					List<Integer> subList = parentBoardIdToSubIdListMap.get(board.getParentBoardId());
					if (subList == null) {
						subList = new ArrayList<Integer>();
						parentBoardIdToSubIdListMap.put(board.getParentBoardId(), subList);
					}
					subList.add(board.getBoardId());
				} else {
					rootBoardIdList.add(board.getBoardId());
				}

				usedBoardIdSet.add(board.getBoardId());
			}

			// 广度优先遍历树，组织校验后的顺序

			Queue<Integer> queue = new LinkedList<Integer>(rootBoardIdList);
			while (!queue.isEmpty()) {
				int boardId = queue.poll();

				CommunityProtos.Board board = boardMap.get(boardId);
				if (board == null) {
					continue;
				}

				communityInfoBuilder.addBoard(board);

				List<Integer> subList = parentBoardIdToSubIdListMap.get(boardId);
				if (subList != null) {
					queue.addAll(subList);
				}
			}

			// 获取推荐帖子id列表，
			DBUtil.closeQuietly(rs);
			rs = null;

			stmt.getMoreResults();
			rs = stmt.getResultSet();
			CommunityDAOProtos.RecommendPostListIndex.Builder builder = CommunityDAOProtos.RecommendPostListIndex.newBuilder();
			while (rs.next()) {
				builder.clear();
				RECOMMEND_POST_INDEX_MAPPER.mapToItem(rs, builder);
				communityInfoBuilder.addRecommendPostIndex(builder.build());
			}

			return communityInfoBuilder.build();
		} finally {
			DBUtil.closeQuietly(rs);
			DBUtil.closeQuietly(stmt);
		}
	}

	private static final ProtobufMapper<CommunityDAOProtos.PostCount> POST_COUNT_MAPPER = ProtobufMapper.createMapper(CommunityDAOProtos.PostCount.getDefaultInstance(),
			"state",
			"count");

	private static final ProtobufMapper<CommunityDAOProtos.PostListIndex> POST_LIST_INDEX_MAPPER = ProtobufMapper.createMapper(CommunityDAOProtos.PostListIndex.getDefaultInstance(),
			"post_id",
			"create_time",
			"state",
			"is_sticky",
			"sticky_time");

	/**
	 * 获取板块的拓展信息，包括帖子计数信息、普通贴索引列表、置顶贴索引列表、标签列表
	 * 
	 * @param conn
	 * @param companyId
	 * @param boardIds
	 * @param postIdxSize
	 * @return
	 * @throws SQLException
	 * Map<Integer,CommunityDAOProtos.BoardExt>
	 * @throws
	 */
	public static Map<Integer, CommunityDAOProtos.BoardExt> getBoardExt(Connection conn, long companyId, Collection<Integer> boardIds, int postIdxSize)
			throws SQLException {
		if (boardIds.isEmpty()) {
			return Collections.emptyMap();
		}

		String boardIdStr = DBUtil.COMMA_JOINER.join(boardIds);

		StringBuilder sql = new StringBuilder();
		sql.append("SELECT board_id, state, count(*) as count FROM weizhu_community_post WHERE company_id = ")
				.append(companyId)
				.append(" AND board_id IN (")
				.append(boardIdStr)
				.append(") GROUP BY board_id, state; ");

		for (Integer boardId : boardIds) {
			sql.append("SELECT board_id, post_id, create_time, state, is_sticky, sticky_time FROM weizhu_community_post WHERE company_id = ")
					.append(companyId)
					.append(" AND board_id = ");
			sql.append(boardId).append(" ORDER BY create_time DESC, post_id DESC LIMIT ").append(postIdxSize).append("; ");
		}

		for (Integer boardId : boardIds) {
			sql.append("SELECT board_id, post_id, create_time, state, is_sticky, sticky_time FROM weizhu_community_post WHERE company_id = ")
					.append(companyId)
					.append(" AND board_id = ")
					.append(boardId)
					.append(" AND is_sticky = 1 ORDER BY sticky_time DESC, post_id DESC;");
		}
		
		// 获取版块下的标签
		sql.append("SELECT board_id, tag FROM weizhu_community_board_tag WHERE company_id = ")
				.append(companyId)
				.append(" AND board_id IN(")
				.append(DBUtil.COMMA_JOINER.join(boardIds))
				.append(");");
		
		Statement stmt = null;
		ResultSet rs = null;
		try {
			stmt = conn.createStatement();
			stmt.execute(sql.toString());

			rs = stmt.getResultSet();

			Map<Integer, CommunityDAOProtos.BoardExt.Builder> resultBuilderMap = new HashMap<Integer, CommunityDAOProtos.BoardExt.Builder>();

			CommunityDAOProtos.PostCount.Builder tmpPostCountBuilder = CommunityDAOProtos.PostCount.newBuilder();
			while (rs.next()) {
				tmpPostCountBuilder.clear();

				int boardId = rs.getInt("board_id");
				CommunityDAOProtos.PostCount postCount = POST_COUNT_MAPPER.mapToItem(rs, tmpPostCountBuilder).build();

				CommunityDAOProtos.BoardExt.Builder builder = resultBuilderMap.get(boardId);
				if (builder == null) {
					builder = CommunityDAOProtos.BoardExt.newBuilder();
					resultBuilderMap.put(boardId, builder);
				}

				builder.addCount(postCount);
			}

			CommunityDAOProtos.PostListIndex.Builder tmpPostListIndexBuilder = CommunityDAOProtos.PostListIndex.newBuilder();
			for (int i = 0; i < boardIds.size(); ++i) {

				DBUtil.closeQuietly(rs);
				rs = null;

				stmt.getMoreResults();
				rs = stmt.getResultSet();

				while (rs.next()) {
					tmpPostListIndexBuilder.clear();

					int boardId = rs.getInt("board_id");
					CommunityDAOProtos.PostListIndex postListIndex = POST_LIST_INDEX_MAPPER.mapToItem(rs, tmpPostListIndexBuilder).build();

					CommunityDAOProtos.BoardExt.Builder builder = resultBuilderMap.get(boardId);
					if (builder == null) {
						builder = CommunityDAOProtos.BoardExt.newBuilder();
						resultBuilderMap.put(boardId, builder);
					}

					builder.addIndex(postListIndex);
				}
			}

			// 获取置顶贴id
			CommunityDAOProtos.PostListIndex.Builder tmpStickyPostListIndex = CommunityDAOProtos.PostListIndex.newBuilder();
			for (int i = 0; i < boardIds.size(); ++i) {

				DBUtil.closeQuietly(rs);
				rs = null;

				stmt.getMoreResults();
				rs = stmt.getResultSet();

				while (rs.next()) {
					tmpPostListIndexBuilder.clear();

					int boardId = rs.getInt("board_id");

					CommunityDAOProtos.PostListIndex stickyPostListIndex = POST_LIST_INDEX_MAPPER.mapToItem(rs, tmpStickyPostListIndex).build();

					CommunityDAOProtos.BoardExt.Builder builder = resultBuilderMap.get(boardId);
					if (builder == null) {
						builder = CommunityDAOProtos.BoardExt.newBuilder();
						resultBuilderMap.put(boardId, builder);
					}

					builder.addStickyPostIndex(stickyPostListIndex);
				}
			}

			// 获取每个版块下的标签
			DBUtil.closeQuietly(rs);
			rs = null;

			stmt.getMoreResults();
			rs = stmt.getResultSet();

			while (rs.next()) {
				int boardId = rs.getInt("board_id");
				CommunityDAOProtos.BoardExt.Builder builder = resultBuilderMap.get(boardId);
				if (builder == null) {
					builder = CommunityDAOProtos.BoardExt.newBuilder();
					resultBuilderMap.put(boardId, builder);
				}

				builder.addTag(rs.getString("tag"));
			}
			
			Map<Integer, CommunityDAOProtos.BoardExt> resultMap = new HashMap<Integer, CommunityDAOProtos.BoardExt>(resultBuilderMap.size());
			for (Entry<Integer, CommunityDAOProtos.BoardExt.Builder> entry : resultBuilderMap.entrySet()) {
				resultMap.put(entry.getKey(), entry.getValue().build());
			}

			return resultMap;
		} finally {
			DBUtil.closeQuietly(rs);
			DBUtil.closeQuietly(stmt);
		}
	}

	private static final ProtobufMapper<CommunityProtos.Post.Part> POST_PART_MAPPER = ProtobufMapper.createMapper(CommunityProtos.Post.Part.getDefaultInstance(),
			"part_id",
			"text",
			"image_name");

	/**
	 * 根据id获取帖子信息
	 * 
	 * @param conn
	 * @param companyId
	 * @param postIds
	 * @return
	 * @throws SQLException
	 * Map<Integer,CommunityProtos.Post>
	 * @throws
	 */
	public static Map<Integer, CommunityProtos.Post> getPost(Connection conn, long companyId, Collection<Integer> postIds) throws SQLException {
		if (postIds.isEmpty()) {
			return Collections.emptyMap();
		}

		String postIdStr = DBUtil.COMMA_JOINER.join(postIds);

		StringBuilder sql = new StringBuilder();
		sql.append("SELECT * FROM weizhu_community_post_part WHERE company_id = ")
				.append(companyId)
				.append(" AND post_id IN (")
				.append(postIdStr)
				.append(") ORDER BY post_id, part_id; ");
		sql.append("SELECT * FROM weizhu_community_post WHERE company_id = ")
				.append(companyId)
				.append(" AND post_id IN (")
				.append(postIdStr)
				.append("); ");

		Statement stmt = null;
		ResultSet rs = null;
		try {
			stmt = conn.createStatement();
			stmt.execute(sql.toString());

			rs = stmt.getResultSet();

			Map<Integer, List<CommunityProtos.Post.Part>> postPartMap = new HashMap<Integer, List<CommunityProtos.Post.Part>>();

			CommunityProtos.Post.Part.Builder tmpPartBuilder = CommunityProtos.Post.Part.newBuilder();
			while (rs.next()) {
				tmpPartBuilder.clear();

				int postId = rs.getInt("post_id");
				CommunityProtos.Post.Part part = POST_PART_MAPPER.mapToItem(rs, tmpPartBuilder).build();

				if (part.hasText() || part.hasImageName()) {
					List<CommunityProtos.Post.Part> list = postPartMap.get(postId);
					if (list == null) {
						list = new ArrayList<CommunityProtos.Post.Part>();
						postPartMap.put(postId, list);
					}
					list.add(part);
				}
			}

			DBUtil.closeQuietly(rs);
			rs = null;

			stmt.getMoreResults();
			rs = stmt.getResultSet();

			Map<Integer, CommunityProtos.Post> postMap = new HashMap<Integer, CommunityProtos.Post>();

			CommunityProtos.Post.Builder tmpPostBuilder = CommunityProtos.Post.newBuilder();
			while (rs.next()) {
				tmpPostBuilder.clear();

				POST_MAPPER.mapToItem(rs, tmpPostBuilder);
				tmpPostBuilder.setCommentCount(0);
				tmpPostBuilder.setLikeCount(0);
				tmpPostBuilder.setIsLike(false);

				List<CommunityProtos.Post.Part> partList = postPartMap.get(tmpPostBuilder.getPostId());
				if (partList != null) {
					tmpPostBuilder.addAllPostPart(partList);
				}

				postMap.put(tmpPostBuilder.getPostId(), tmpPostBuilder.build());
			}

			return postMap;
		} finally {
			DBUtil.closeQuietly(rs);
			DBUtil.closeQuietly(stmt);
		}
	}

	/**
	 * 创建帖子
	 * 
	 * @param conn
	 * @param companyId
	 * @param postList
	 * @return
	 * @throws SQLException
	 * List<Integer>
	 * @throws
	 */
	public static List<Integer> insertPost(Connection conn, long companyId, List<CommunityProtos.Post> postList) throws SQLException {
		if (postList.isEmpty()) {
			return Collections.emptyList();
		}

		List<Integer> postIdList;

		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try {
			pstmt = conn.prepareStatement("INSERT INTO weizhu_community_post (company_id, post_id, post_title, board_id, create_user_id, create_time, is_hot, state, comment_id_max) VALUES (?, NULL, ?, ?, ?, ?, ?, ?, 0); ",
					Statement.RETURN_GENERATED_KEYS);

			for (CommunityProtos.Post post : postList) {
				pstmt.setLong(1, companyId);
				DBUtil.set(pstmt, 2, post.hasPostTitle(), post.getPostTitle());
				DBUtil.set(pstmt, 3, post.hasBoardId(), post.getBoardId());
				DBUtil.set(pstmt, 4, post.hasCreateUserId(), post.getCreateUserId());
				DBUtil.set(pstmt, 5, post.hasCreateTime(), post.getCreateTime());
				DBUtil.set(pstmt, 6, post.hasIsHot(), post.getIsHot());
				DBUtil.set(pstmt, 7, post.hasState(), post.getState());

				pstmt.addBatch();
			}

			pstmt.executeBatch();
			rs = pstmt.getGeneratedKeys();

			postIdList = new ArrayList<Integer>(postList.size());
			while (rs.next()) {
				postIdList.add(rs.getInt(1));
			}

			if (postIdList.size() != postList.size()) {
				throw new RuntimeException("insert fail!");
			}
		} finally {
			DBUtil.closeQuietly(rs);
			DBUtil.closeQuietly(pstmt);
		}

		pstmt = null;
		rs = null;
		try {
			pstmt = conn.prepareStatement("INSERT INTO weizhu_community_post_part (company_id, part_id, post_id, `text`, image_name) VALUES (?, NULL, ?, ?, ?); ",
					Statement.RETURN_GENERATED_KEYS);

			for (int i = 0; i < postList.size() && i < postIdList.size(); ++i) {
				int postId = postIdList.get(i);
				CommunityProtos.Post post = postList.get(i);

				for (CommunityProtos.Post.Part part : post.getPostPartList()) {
					pstmt.setLong(1, companyId);
					DBUtil.set(pstmt, 2, true, postId);
					DBUtil.set(pstmt, 3, part.hasText(), part.getText());
					DBUtil.set(pstmt, 4, part.hasImageName(), part.getImageName());

					pstmt.addBatch();
				}
			}

			pstmt.executeBatch();
		} finally {
			DBUtil.closeQuietly(rs);
			DBUtil.closeQuietly(pstmt);
		}

		return postIdList;
	}

	/**
	 * 状态删除帖子
	 * 
	 * @param conn
	 * @param companyId
	 * @param postIds
	 * @throws SQLException
	 * void
	 * @throws
	 */
	public static void updatePostStateDelete(Connection conn, long companyId, Collection<Integer> postIds) throws SQLException {
		if (postIds.isEmpty()) {
			return;
		}

		PreparedStatement pstmt = null;
		try {
			pstmt = conn.prepareStatement("UPDATE weizhu_community_post SET state = ? WHERE company_id = ? AND post_id = ?; UPDATE weizhu_community_post_comment SET state = ? WHERE company_id = ? AND post_id = ?;");

			for (Integer postId : postIds) {
				DBUtil.set(pstmt, 1, true, CommunityProtos.Post.State.DELETE.name());
				pstmt.setLong(2, companyId);
				DBUtil.set(pstmt, 3, true, postId);
				DBUtil.set(pstmt, 4, true, CommunityProtos.Comment.State.DELETE.name());
				pstmt.setLong(5, companyId);
				DBUtil.set(pstmt, 6, true, postId);

				pstmt.addBatch();
			}

			pstmt.executeBatch();
		} finally {
			DBUtil.closeQuietly(pstmt);
		}
	}

	private static final ProtobufMapper<CommunityDAOProtos.CommentCount> COMMENT_COUNT_MAPPER = ProtobufMapper.createMapper(CommunityDAOProtos.CommentCount.getDefaultInstance(),
			"state",
			"count");

	/**
	 * 获取帖子拓展信息，包括评论数、点赞数和标签列表
	 * 
	 * @param conn
	 * @param companyId
	 * @param postIds
	 * @return
	 * @throws SQLException
	 * Map<Integer,CommunityDAOProtos.PostExt>
	 * @throws
	 */
	public static Map<Integer, CommunityDAOProtos.PostExt> getPostExt(Connection conn, long companyId, Collection<Integer> postIds) throws SQLException {
		if (postIds.isEmpty()) {
			return Collections.emptyMap();
		}

		String postIdStr = DBUtil.COMMA_JOINER.join(postIds);

		StringBuilder sql = new StringBuilder();
		sql.append("SELECT post_id, state, count(*) as count FROM weizhu_community_post_comment WHERE company_id =")
				.append(companyId)
				.append(" AND post_id IN (")
				.append(postIdStr)
				.append(") GROUP BY post_id, state; ");
		sql.append("SELECT post_id, count(*) as like_count FROM weizhu_community_post_like WHERE company_id =")
				.append(companyId)
				.append(" AND post_id IN (")
				.append(postIdStr)
				.append(") GROUP BY post_id; ");
		// 获取帖子的标签
		sql.append("SELECT post_id, tag FROM weizhu_community_post_tag WHERE company_id = ")
				.append(companyId)
				.append(" AND post_id IN(")
				.append(DBUtil.COMMA_JOINER.join(postIds))
				.append(");");
		Statement stmt = null;
		ResultSet rs = null;
		try {
			stmt = conn.createStatement();
			stmt.execute(sql.toString());

			rs = stmt.getResultSet();

			Map<Integer, List<CommunityDAOProtos.CommentCount>> commentCountMap = new HashMap<Integer, List<CommunityDAOProtos.CommentCount>>();

			CommunityDAOProtos.CommentCount.Builder tmpCommentCountBuilder = CommunityDAOProtos.CommentCount.newBuilder();
			while (rs.next()) {
				tmpCommentCountBuilder.clear();

				int postId = rs.getInt("post_id");
				CommunityDAOProtos.CommentCount commentCount = COMMENT_COUNT_MAPPER.mapToItem(rs, tmpCommentCountBuilder).build();

				List<CommunityDAOProtos.CommentCount> list = commentCountMap.get(postId);
				if (list == null) {
					list = new ArrayList<CommunityDAOProtos.CommentCount>();
					commentCountMap.put(postId, list);
				}

				list.add(commentCount);
			}

			DBUtil.closeQuietly(rs);
			rs = null;

			stmt.getMoreResults();
			rs = stmt.getResultSet();

			Map<Integer, Integer> likeCountMap = new HashMap<Integer, Integer>();
			while (rs.next()) {
				likeCountMap.put(rs.getInt("post_id"), rs.getInt("like_count"));
			}

			
			DBUtil.closeQuietly(rs);
			rs = null;

			stmt.getMoreResults();
			rs = stmt.getResultSet();

			Map<Integer, List<String>> postIdTagListMap = new HashMap<Integer, List<String>>();
			while (rs.next()) {
				int postId = rs.getInt("post_id");
				List<String> tagList = postIdTagListMap.get(postId);
				if (tagList == null) {
					tagList = new ArrayList<String>();
					postIdTagListMap.put(postId, tagList);
				}
				tagList.add(rs.getString("tag"));
			}
			
			
			Map<Integer, CommunityDAOProtos.PostExt> resultMap = new HashMap<Integer, CommunityDAOProtos.PostExt>();

			CommunityDAOProtos.PostExt.Builder tmpBuilder = CommunityDAOProtos.PostExt.newBuilder();
			for (Integer postId : postIds) {
				List<CommunityDAOProtos.CommentCount> commentCountList = commentCountMap.get(postId);
				Integer likeCount = likeCountMap.get(postId);
				List<String> tagList = postIdTagListMap.get(postId);

				if (commentCountList != null || likeCount != null || tagList != null) {
					tmpBuilder.clear();
					if (commentCountList != null) {
						tmpBuilder.addAllCommentCount(commentCountList);
					}
					tmpBuilder.setLikeCount(likeCount == null ? 0 : likeCount);
					if (tagList != null) {
						tmpBuilder.addAllTag(tagList);
					}
					resultMap.put(postId, tmpBuilder.build());
				}
			}
			return resultMap;
		} finally {
			DBUtil.closeQuietly(rs);
			DBUtil.closeQuietly(stmt);
		}
	}

	/**
	 * 获取postIds中userId用户点过赞的帖子id
	 * 
	 * @param conn
	 * @param companyId
	 * @param userId
	 * @param postIds
	 * @return
	 * @throws SQLException
	 * Set<Integer>
	 * @throws
	 */
	public static Set<Integer> getUserPostLikeIdSet(Connection conn, long companyId, long userId, Collection<Integer> postIds) throws SQLException {
		if (postIds.isEmpty()) {
			return Collections.emptySet();
		}

		StringBuilder sql = new StringBuilder();
		sql.append("SELECT post_id FROM weizhu_community_post_like WHERE company_id =").append(companyId).append(" AND post_id IN (");
		DBUtil.COMMA_JOINER.appendTo(sql, postIds);
		sql.append(") AND user_id = ").append(userId).append("; ");

		Statement stmt = null;
		ResultSet rs = null;
		try {
			stmt = conn.createStatement();
			rs = stmt.executeQuery(sql.toString());

			Set<Integer> postIdSet = new TreeSet<Integer>();
			while (rs.next()) {
				postIdSet.add(rs.getInt("post_id"));
			}
			return postIdSet;
		} finally {
			DBUtil.closeQuietly(rs);
			DBUtil.closeQuietly(stmt);
		}
	}

	/**
	 * 点赞
	 * 
	 * @param conn
	 * @param companyId
	 * @param userId
	 * @param postId
	 * @param now
	 * @throws SQLException
	 * void
	 * @throws
	 */
	public static void insertUserPostLike(Connection conn, long companyId, long userId, int postId, int now) throws SQLException {
		PreparedStatement pstmt = null;
		try {
			pstmt = conn.prepareStatement("INSERT IGNORE weizhu_community_post_like (company_id, post_id, user_id, create_time) VALUES (?, ?, ?, ?);");
			pstmt.setLong(1, companyId);
			pstmt.setInt(2, postId);
			pstmt.setLong(3, userId);
			pstmt.setInt(4, now);

			pstmt.executeUpdate();
		} finally {
			DBUtil.closeQuietly(pstmt);
		}
	}

	/**
	 * 取消点赞
	 * 
	 * @param conn
	 * @param companyId
	 * @param userId
	 * @param postId
	 * @throws SQLException
	 * void
	 * @throws
	 */
	public static void deleteUserPostLike(Connection conn, long companyId, long userId, int postId) throws SQLException {
		PreparedStatement pstmt = null;
		try {
			pstmt = conn.prepareStatement("DELETE FROM weizhu_community_post_like WHERE company_id = ? AND post_id = ? AND user_id = ?;");
			pstmt.setLong(1, companyId);
			pstmt.setInt(2, postId);
			pstmt.setLong(3, userId);

			pstmt.executeUpdate();
		} finally {
			DBUtil.closeQuietly(pstmt);
		}
	}

	/**
	 * 根据时间倒序获取板块下帖子的索引列表
	 * 
	 * @param conn
	 * @param boardId
	 * @param states
	 * @param size
	 * @param isSticky 是否获取置顶贴
	 * @return
	 * @throws SQLException
	 */
	public static List<CommunityDAOProtos.PostListIndex> getBoardPostListIndexList(Connection conn, long companyId, int boardId,
			Collection<CommunityProtos.Post.State> states, int size, boolean isSticky) throws SQLException {
		if (size <= 0 || states.isEmpty()) {
			return Collections.emptyList();
		}

		StringBuilder sql = new StringBuilder();
		sql.append("SELECT post_id, create_time, state, is_sticky, sticky_time FROM weizhu_community_post WHERE company_id = ")
				.append(companyId)
				.append(" AND board_id = ");
		sql.append(boardId).append(" AND state IN ('");

		boolean isFirst = true;
		for (CommunityProtos.Post.State state : states) {
			if (isFirst) {
				isFirst = false;
			} else {
				sql.append("', '");
			}
			sql.append(DBUtil.SQL_STRING_ESCAPER.escape(state.name()));
		}

		sql.append("') ");

		if (isSticky) {
			sql.append(" AND is_sticky IS NOT true");
		}

		sql.append(" ORDER BY create_time DESC, post_id DESC LIMIT ").append(size).append("; ");

		Statement stmt = null;
		ResultSet rs = null;
		try {
			stmt = conn.createStatement();

			rs = stmt.executeQuery(sql.toString());

			List<CommunityDAOProtos.PostListIndex> resultList = new ArrayList<CommunityDAOProtos.PostListIndex>();

			CommunityDAOProtos.PostListIndex.Builder tmpBuilder = CommunityDAOProtos.PostListIndex.newBuilder();
			while (rs.next()) {
				tmpBuilder.clear();
				resultList.add(POST_LIST_INDEX_MAPPER.mapToItem(rs, tmpBuilder).build());
			}
			return resultList;
		} finally {
			DBUtil.closeQuietly(rs);
			DBUtil.closeQuietly(stmt);
		}
	}

	/**
	 * 根据时间倒序获取板块下帖子的索引列表
	 * 
	 * @param conn
	 * @param boardId
	 * @param states
	 * @param lastIndex
	 * @param size
	 * @param isSticky 是否获取置顶贴
	 * @return
	 * @throws SQLException
	 */
	public static List<CommunityDAOProtos.PostListIndex> getBoardPostListIndexList(Connection conn, long companyId, int boardId,
			Collection<CommunityProtos.Post.State> states, CommunityDAOProtos.PostListIndex lastIndex, int size, boolean isSticky)
			throws SQLException {
		if (size <= 0 || states.isEmpty()) {
			return Collections.emptyList();
		}

		StringBuilder sql = new StringBuilder();
		sql.append("SELECT post_id, create_time, state, is_sticky, sticky_time FROM weizhu_community_post WHERE company_id = ")
				.append(companyId)
				.append(" AND board_id = ");
		sql.append(boardId).append(" AND state IN ('");

		boolean isFirst = true;
		for (CommunityProtos.Post.State state : states) {
			if (isFirst) {
				isFirst = false;
			} else {
				sql.append("', '");
			}
			sql.append(DBUtil.SQL_STRING_ESCAPER.escape(state.name()));
		}

		sql.append("') AND (create_time < ");
		sql.append(lastIndex.getCreateTime()).append(" OR (create_time = ");
		sql.append(lastIndex.getCreateTime()).append(" AND post_id < ");
		sql.append(lastIndex.getPostId()).append(")) ");

		if (isSticky) {
			sql.append(" AND is_sticky IS NOT true ");
		}

		sql.append(" ORDER BY create_time DESC, post_id DESC LIMIT ").append(size).append("; ");

		Statement stmt = null;
		ResultSet rs = null;
		try {
			stmt = conn.createStatement();
			rs = stmt.executeQuery(sql.toString());

			List<CommunityDAOProtos.PostListIndex> resultList = new ArrayList<CommunityDAOProtos.PostListIndex>();

			CommunityDAOProtos.PostListIndex.Builder tmpBuilder = CommunityDAOProtos.PostListIndex.newBuilder();
			while (rs.next()) {
				tmpBuilder.clear();
				resultList.add(POST_LIST_INDEX_MAPPER.mapToItem(rs, tmpBuilder).build());
			}
			return resultList;
		} finally {
			DBUtil.closeQuietly(rs);
			DBUtil.closeQuietly(stmt);
		}
	}

	/**
	 * 获取用户创建的帖子索引列表，创建时间和post_id倒序排列
	 * 
	 * @param conn
	 * @param companyId
	 * @param userId
	 * @param states
	 * @param size
	 * @return
	 * @throws SQLException
	 * List<CommunityDAOProtos.PostListIndex>
	 * @throws
	 */
	public static List<CommunityDAOProtos.PostListIndex> getUserPostListIndexList(Connection conn, long companyId, long userId,
			Collection<CommunityProtos.Post.State> states, int size) throws SQLException {
		if (size <= 0 || states.isEmpty()) {
			return Collections.emptyList();
		}

		StringBuilder sql = new StringBuilder();
		sql.append("SELECT post_id, create_time, state, is_sticky, sticky_time  FROM weizhu_community_post WHERE company_id = ")
				.append(companyId)
				.append(" AND create_user_id = ");
		sql.append(userId).append(" AND state IN ('");

		boolean isFirst = true;
		for (CommunityProtos.Post.State state : states) {
			if (isFirst) {
				isFirst = false;
			} else {
				sql.append("', '");
			}
			sql.append(DBUtil.SQL_STRING_ESCAPER.escape(state.name()));
		}

		sql.append("') ORDER BY create_time DESC, post_id DESC LIMIT ").append(size).append("; ");

		Statement stmt = null;
		ResultSet rs = null;
		try {
			stmt = conn.createStatement();
			rs = stmt.executeQuery(sql.toString());

			List<CommunityDAOProtos.PostListIndex> resultList = new ArrayList<CommunityDAOProtos.PostListIndex>();

			CommunityDAOProtos.PostListIndex.Builder tmpBuilder = CommunityDAOProtos.PostListIndex.newBuilder();
			while (rs.next()) {
				tmpBuilder.clear();
				resultList.add(POST_LIST_INDEX_MAPPER.mapToItem(rs, tmpBuilder).build());
			}
			return resultList;
		} finally {
			DBUtil.closeQuietly(rs);
			DBUtil.closeQuietly(stmt);
		}
	}

	/**
	 * 获取用户所创建的帖子的索引列表
	 * 
	 * @param conn
	 * @param companyId
	 * @param userId
	 * @param states
	 * @param lastIndex
	 * @param size
	 * @return
	 * @throws SQLException
	 * List<CommunityDAOProtos.PostListIndex>
	 * @throws
	 */
	public static List<CommunityDAOProtos.PostListIndex> getUserPostListIndexList(Connection conn, long companyId, long userId,
			Collection<CommunityProtos.Post.State> states, CommunityDAOProtos.PostListIndex lastIndex, int size) throws SQLException {
		if (size <= 0 || states.isEmpty()) {
			return Collections.emptyList();
		}

		StringBuilder sql = new StringBuilder();
		sql.append("SELECT post_id, create_time, state, is_sticky, sticky_time  FROM weizhu_community_post WHERE company_id = ")
				.append(companyId)
				.append(" AND create_user_id = ");
		sql.append(userId).append(" AND state IN ('");

		boolean isFirst = true;
		for (CommunityProtos.Post.State state : states) {
			if (isFirst) {
				isFirst = false;
			} else {
				sql.append("', '");
			}
			sql.append(DBUtil.SQL_STRING_ESCAPER.escape(state.name()));
		}

		sql.append("') AND (create_time < ");
		sql.append(lastIndex.getCreateTime()).append(" OR (create_time = ");
		sql.append(lastIndex.getCreateTime()).append(" AND post_id < ");
		sql.append(lastIndex.getPostId()).append(")) ORDER BY create_time DESC, post_id DESC LIMIT ").append(size).append("; ");

		Statement stmt = null;
		ResultSet rs = null;
		try {

			stmt = conn.createStatement();
			rs = stmt.executeQuery(sql.toString());

			List<CommunityDAOProtos.PostListIndex> resultList = new ArrayList<CommunityDAOProtos.PostListIndex>();

			CommunityDAOProtos.PostListIndex.Builder tmpBuilder = CommunityDAOProtos.PostListIndex.newBuilder();
			while (rs.next()) {
				tmpBuilder.clear();
				resultList.add(POST_LIST_INDEX_MAPPER.mapToItem(rs, tmpBuilder).build());
			}
			return resultList;
		} finally {
			DBUtil.closeQuietly(rs);
			DBUtil.closeQuietly(stmt);
		}
	}

	private static final ProtobufMapper<CommunityProtos.Comment> COMMENT_MAPPER = ProtobufMapper.createMapper(CommunityProtos.Comment.getDefaultInstance(),
			"post_id",
			"comment_id",
			"reply_comment_id",
			"content",
			"create_user_id",
			"create_time",
			"state");

	/**
	 * 获取评论列表
	 * 
	 * @param conn
	 * @param companyId
	 * @param postId
	 * @param states
	 * @param size
	 * @return
	 * @throws SQLException
	 * List<CommunityProtos.Comment>
	 * @throws
	 */
	public static List<CommunityProtos.Comment> getPostCommentList(Connection conn, long companyId, int postId, Collection<CommunityProtos.Comment.State> states,
			int size) throws SQLException {
		if (size <= 0 || states.isEmpty()) {
			return Collections.emptyList();
		}

		StringBuilder sql = new StringBuilder();
		sql.append("SELECT * FROM weizhu_community_post_comment WHERE company_id = ").append(companyId).append(" AND post_id = ");
		sql.append(postId).append(" AND state IN ('");

		boolean isFirst = true;
		for (CommunityProtos.Comment.State state : states) {
			if (isFirst) {
				isFirst = false;
			} else {
				sql.append("', '");
			}
			sql.append(DBUtil.SQL_STRING_ESCAPER.escape(state.name()));
		}

		sql.append("') ORDER BY comment_id DESC LIMIT ").append(size).append("; ");

		Statement stmt = null;
		ResultSet rs = null;
		try {
			stmt = conn.createStatement();
			rs = stmt.executeQuery(sql.toString());

			List<CommunityProtos.Comment> resultList = new ArrayList<CommunityProtos.Comment>();

			CommunityProtos.Comment.Builder tmpBuilder = CommunityProtos.Comment.newBuilder();
			while (rs.next()) {
				tmpBuilder.clear();

				resultList.add(COMMENT_MAPPER.mapToItem(rs, tmpBuilder).setLikeCount(0).setIsLike(false).build());
			}

			return resultList;
		} finally {
			DBUtil.closeQuietly(rs);
			DBUtil.closeQuietly(stmt);
		}
	}

	/**
	 * 根据索引获取评论列表
	 * 
	 * @param conn
	 * @param companyId
	 * @param postId
	 * @param states
	 * @param lastIndex
	 * @param size
	 * @return
	 * @throws SQLException
	 * List<CommunityProtos.Comment>
	 * @throws
	 */
	public static List<CommunityProtos.Comment> getPostCommentList(Connection conn, long companyId, int postId, Collection<CommunityProtos.Comment.State> states,
			CommunityDAOProtos.PostCommentListIndex lastIndex, int size) throws SQLException {
		if (size <= 0 || states.isEmpty()) {
			return Collections.emptyList();
		}

		StringBuilder sql = new StringBuilder();
		sql.append("SELECT * FROM weizhu_community_post_comment WHERE company_id = ").append(companyId).append(" AND post_id = ");
		sql.append(postId).append(" AND state IN ('");

		boolean isFirst = true;
		for (CommunityProtos.Comment.State state : states) {
			if (isFirst) {
				isFirst = false;
			} else {
				sql.append("', '");
			}
			sql.append(DBUtil.SQL_STRING_ESCAPER.escape(state.name()));
		}

		sql.append("') AND comment_id < ").append(lastIndex.getCommentId()).append(" ORDER BY comment_id DESC LIMIT ").append(size).append("; ");

		Statement stmt = null;
		ResultSet rs = null;
		try {
			stmt = conn.createStatement();
			rs = stmt.executeQuery(sql.toString());

			List<CommunityProtos.Comment> resultList = new ArrayList<CommunityProtos.Comment>();

			CommunityProtos.Comment.Builder tmpBuilder = CommunityProtos.Comment.newBuilder();
			while (rs.next()) {
				tmpBuilder.clear();

				resultList.add(COMMENT_MAPPER.mapToItem(rs, tmpBuilder).setLikeCount(0).setIsLike(false).build());
			}

			return resultList;
		} finally {
			DBUtil.closeQuietly(rs);
			DBUtil.closeQuietly(stmt);
		}
	}

	/**
	 * 获取用户的评论列表
	 * 
	 * @param conn
	 * @param companyId
	 * @param userId
	 * @param states
	 * @param size
	 * @return
	 * @throws SQLException
	 * List<CommunityProtos.Comment>
	 * @throws
	 */
	public static List<CommunityProtos.Comment> getUserPostCommentList(Connection conn, long companyId, long userId,
			Collection<CommunityProtos.Comment.State> states, int size) throws SQLException {
		if (size <= 0 || states.isEmpty()) {
			return Collections.emptyList();
		}

		StringBuilder sql = new StringBuilder();
		sql.append("SELECT * FROM weizhu_community_post_comment WHERE company_id = ").append(companyId).append(" AND create_user_id = ");
		sql.append(userId).append(" AND state IN ('");

		boolean isFirst = true;
		for (CommunityProtos.Comment.State state : states) {
			if (isFirst) {
				isFirst = false;
			} else {
				sql.append("', '");
			}
			sql.append(DBUtil.SQL_STRING_ESCAPER.escape(state.name()));
		}

		sql.append("') ORDER BY create_time DESC, post_id DESC, comment_id DESC LIMIT ").append(size).append("; ");

		Statement stmt = null;
		ResultSet rs = null;
		try {
			stmt = conn.createStatement();
			rs = stmt.executeQuery(sql.toString());

			List<CommunityProtos.Comment> resultList = new ArrayList<CommunityProtos.Comment>();

			CommunityProtos.Comment.Builder tmpBuilder = CommunityProtos.Comment.newBuilder();
			while (rs.next()) {
				tmpBuilder.clear();

				resultList.add(COMMENT_MAPPER.mapToItem(rs, tmpBuilder).setLikeCount(0).setIsLike(false).build());
			}

			return resultList;
		} finally {
			DBUtil.closeQuietly(rs);
			DBUtil.closeQuietly(stmt);
		}
	}

	/**
	 * 根据索引获取用户的评论列表
	 * 
	 * @param conn
	 * @param companyId
	 * @param userId
	 * @param states
	 * @param lastIndex
	 * @param size
	 * @return
	 * @throws SQLException
	 * List<CommunityProtos.Comment>
	 * @throws
	 */
	public static List<CommunityProtos.Comment> getUserPostCommentList(Connection conn, long companyId, long userId,
			Collection<CommunityProtos.Comment.State> states, CommunityDAOProtos.UserCommentListIndex lastIndex, int size) throws SQLException {
		if (size <= 0 || states.isEmpty()) {
			return Collections.emptyList();
		}

		StringBuilder sql = new StringBuilder();
		sql.append("SELECT * FROM weizhu_community_post_comment WHERE company_id = ").append(companyId).append(" AND create_user_id = ");
		sql.append(userId).append(" AND state IN ('");

		boolean isFirst = true;
		for (CommunityProtos.Comment.State state : states) {
			if (isFirst) {
				isFirst = false;
			} else {
				sql.append("', '");
			}
			sql.append(DBUtil.SQL_STRING_ESCAPER.escape(state.name()));
		}

		sql.append("') AND (create_time < ").append(lastIndex.getCreateTime());
		sql.append(" OR (create_time = ").append(lastIndex.getCreateTime());
		sql.append(" AND (post_id < ").append(lastIndex.getPostId());
		sql.append(" OR (post_id = ").append(lastIndex.getPostId());
		sql.append(" AND comment_id < ").append(lastIndex.getCommentId());
		sql.append(")))) ORDER BY create_time DESC, post_id DESC, comment_id DESC LIMIT ").append(size).append("; ");

		Statement stmt = null;
		ResultSet rs = null;
		try {
			stmt = conn.createStatement();
			rs = stmt.executeQuery(sql.toString());

			List<CommunityProtos.Comment> resultList = new ArrayList<CommunityProtos.Comment>();

			CommunityProtos.Comment.Builder tmpBuilder = CommunityProtos.Comment.newBuilder();
			while (rs.next()) {
				tmpBuilder.clear();

				resultList.add(COMMENT_MAPPER.mapToItem(rs, tmpBuilder).setLikeCount(0).setIsLike(false).build());
			}

			return resultList;
		} finally {
			DBUtil.closeQuietly(rs);
			DBUtil.closeQuietly(stmt);
		}
	}

	/**
	 * 获取评论详情
	 * 
	 * @param conn
	 * @param companyId
	 * @param postId
	 * @param commentId
	 * @return
	 * @throws SQLException
	 * CommunityProtos.Comment
	 * @throws
	 */
	public static CommunityProtos.Comment getComment(Connection conn, long companyId, int postId, int commentId) throws SQLException {
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try {
			pstmt = conn.prepareStatement("SELECT * FROM weizhu_community_post_comment WHERE company_id = ? AND post_id = ? AND comment_id = ?; ");
			pstmt.setLong(1, companyId);
			pstmt.setInt(2, postId);
			pstmt.setInt(3, commentId);

			rs = pstmt.executeQuery();

			if (!rs.next()) {
				return null;
			}

			CommunityProtos.Comment.Builder builder = CommunityProtos.Comment.newBuilder();

			COMMENT_MAPPER.mapToItem(rs, builder);

			return builder.setLikeCount(0).setIsLike(false).build();
		} finally {
			DBUtil.closeQuietly(rs);
			DBUtil.closeQuietly(pstmt);
		}
	}

	/**
	 * 创建评论
	 * 
	 * @param conn
	 * @param companyId
	 * @param comment
	 * @return
	 * @throws SQLException
	 * int
	 * @throws
	 */
	public static int insertComment(Connection conn, long companyId, CommunityProtos.Comment comment) throws SQLException {
		int commentId;

		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try {
			pstmt = conn.prepareStatement("UPDATE weizhu_community_post SET comment_id_max = comment_id_max + 1 WHERE company_id = ? AND post_id = ?; SELECT comment_id_max FROM weizhu_community_post WHERE company_id = ? AND post_id = ?; ");
			pstmt.setLong(1, companyId);
			pstmt.setInt(2, comment.getPostId());
			pstmt.setLong(3, companyId);
			pstmt.setInt(4, comment.getPostId());

			pstmt.execute();
			pstmt.getMoreResults();

			rs = pstmt.getResultSet();

			if (!rs.next()) {
				throw new RuntimeException("comment cannot generate id");
			}

			commentId = rs.getInt("comment_id_max");
		} finally {
			DBUtil.closeQuietly(rs);
			DBUtil.closeQuietly(pstmt);
		}

		pstmt = null;
		try {
			pstmt = conn.prepareStatement("INSERT INTO weizhu_community_post_comment (company_id, post_id, comment_id, reply_comment_id, content, create_user_id, create_time, state) VALUES (?, ?, ?, ?, ?, ?, ?, ?); ");

			pstmt.setLong(1, companyId);
			DBUtil.set(pstmt, 2, comment.hasPostId(), comment.getPostId());
			DBUtil.set(pstmt, 3, true, commentId);
			DBUtil.set(pstmt, 4, comment.hasReplyCommentId(), comment.getReplyCommentId());
			DBUtil.set(pstmt, 5, comment.hasContent(), comment.getContent());
			DBUtil.set(pstmt, 6, comment.hasCreateUserId(), comment.getCreateUserId());
			DBUtil.set(pstmt, 7, comment.hasCreateTime(), comment.getCreateTime());
			DBUtil.set(pstmt, 8, comment.hasState(), comment.getState());

			pstmt.executeUpdate();
		} finally {
			DBUtil.closeQuietly(pstmt);
		}

		return commentId;
	}

	/**
	 * 状态删除评论
	 * 
	 * @param conn
	 * @param companyId
	 * @param postId
	 * @param commentId
	 * @throws SQLException
	 * void
	 * @throws
	 */
	public static void updateCommentStateDelete(Connection conn, long companyId, int postId, int commentId) throws SQLException {
		PreparedStatement pstmt = null;
		try {
			pstmt = conn.prepareStatement("UPDATE weizhu_community_post_comment SET state = ? WHERE company_id = ? AND post_id = ? AND comment_id = ?; ");
			DBUtil.set(pstmt, 1, true, CommunityProtos.Comment.State.DELETE.name());
			pstmt.setLong(2, companyId);
			DBUtil.set(pstmt, 3, true, postId);
			DBUtil.set(pstmt, 4, true, commentId);

			pstmt.executeUpdate();
		} finally {
			DBUtil.closeQuietly(pstmt);
		}
	}

	private static final ProtobufMapper<CommunityDAOProtos.HotPostListIndex> HOT_POST_INDEX_MAPPER = ProtobufMapper.createMapper(CommunityDAOProtos.HotPostListIndex.getDefaultInstance(),
			"post_id",
			"state");

	/**
	 * 获取板块下热帖的id列表
	 * 
	 * @param conn
	 * @param companyId
	 * @param boardIds
	 * @param viewFactor
	 * @param commentFactor
	 * @param likeFactor
	 * @param size
	 * @return
	 * @throws SQLException
	 * Map<Integer,List<CommunityDAOProtos.HotPostListIndex>>
	 * @throws
	 */
	public static Map<Integer, List<CommunityDAOProtos.HotPostListIndex>> getBoardHotPostIdList(Connection conn, long companyId, Collection<Integer> boardIds,
			int viewFactor, int commentFactor, int likeFactor, int size) throws SQLException {
		if (boardIds.isEmpty()) {
			return Collections.emptyMap();
		}

		StringBuilder sql = new StringBuilder();

		for (Integer boardId : boardIds) {
			sql.append("SELECT a.board_id, a.post_id, b.state, ");
			sql.append("a.pre_view_cnt * ").append(viewFactor);
			sql.append(" + a.pre_comment_cnt * ").append(commentFactor);
			sql.append(" + a.pre_like_cnt * ").append(likeFactor).append(" as hot_degree ");
			sql.append("FROM weizhu_community_post_hot a, (SELECT post_id, state FROM weizhu_community_post WHERE company_id = ")
					.append(companyId)
					.append(" AND board_id = ")
					.append(boardId)
					.append(") b WHERE company_id = ")
					.append(companyId)
					.append(" AND a.post_id = b.post_id AND a.board_id = ")
					.append(boardId)
					.append(" ORDER BY hot_degree DESC LIMIT ")
					.append(size)
					.append("; ");
		}

		Statement stmt = null;
		ResultSet rs = null;
		try {
			Map<Integer, List<CommunityDAOProtos.HotPostListIndex>> resultMap = new HashMap<Integer, List<CommunityDAOProtos.HotPostListIndex>>();

			for (int i = 0; i < boardIds.size(); ++i) {
				if (stmt == null) {
					stmt = conn.createStatement();
					stmt.execute(sql.toString());
					rs = stmt.getResultSet();
				} else {
					stmt.getMoreResults();
					rs = stmt.getResultSet();
				}

				CommunityDAOProtos.HotPostListIndex.Builder builder = CommunityDAOProtos.HotPostListIndex.newBuilder();
				while (rs.next()) {

					int boardId = rs.getInt("board_id");

					List<CommunityDAOProtos.HotPostListIndex> list = resultMap.get(boardId);
					if (list == null) {
						list = new ArrayList<CommunityDAOProtos.HotPostListIndex>();
						resultMap.put(boardId, list);
					}
					builder.clear();
					HOT_POST_INDEX_MAPPER.mapToItem(rs, builder);

					list.add(builder.build());
				}

				DBUtil.closeQuietly(rs);
				rs = null;
			}

			return resultMap;
		} finally {
			DBUtil.closeQuietly(rs);
			DBUtil.closeQuietly(stmt);
		}
	}

	/**
	 * 创建热帖的查看数、评论数、点赞数的当前变量
	 * 
	 * @param conn
	 * @param companyId
	 * @param postId
	 * @param boardId
	 * @throws SQLException
	 * void
	 * @throws
	 */
	public static void insertBoardHotPostCount(Connection conn, long companyId, int postId, int boardId) throws SQLException {
		PreparedStatement pstmt = null;
		try {
			pstmt = conn.prepareStatement("INSERT INTO weizhu_community_post_hot (company_id, post_id, board_id, update_time, cur_view_cnt, cur_comment_cnt, cur_like_cnt, pre_view_cnt, pre_comment_cnt, pre_like_cnt) VALUES (?, ?, ?, 0, 0, 0, 0, 0, 0, 0); ");
			pstmt.setLong(1, companyId);
			pstmt.setInt(2, postId);
			pstmt.setInt(3, boardId);
			pstmt.executeUpdate();
		} finally {
			DBUtil.closeQuietly(pstmt);
		}
	}

	/**
	 * 更新热帖的查看数、评论数、点赞数的当前变量
	 * 
	 * @param conn
	 * @param companyId
	 * @param postId
	 * @param isView
	 * @param isComment
	 * @param isLike
	 * @throws SQLException
	 * void
	 * @throws
	 */
	public static void updateBoardHotPostCount(Connection conn, long companyId, int postId, boolean isView, boolean isComment, boolean isLike) throws SQLException {
		if (!isView && !isComment && !isLike) {
			return;
		}

		StringBuilder sql = new StringBuilder();
		sql.append("UPDATE weizhu_community_post_hot SET ");

		if (isView) {
			sql.append("cur_view_cnt = cur_view_cnt + 1");
		}

		if (isComment) {
			if (isView) {
				sql.append(", ");
			}
			sql.append("cur_comment_cnt = cur_comment_cnt + 1");
		}

		if (isLike) {
			if (isView || isComment) {
				sql.append(", ");
			}
			sql.append("cur_like_cnt = cur_like_cnt + 1");
		}

		sql.append(" WHERE company_id = ").append(companyId).append(" AND post_id = ").append(postId).append("; ");

		Statement stmt = null;
		try {
			stmt = conn.createStatement();
			stmt.executeUpdate(sql.toString());
		} finally {
			DBUtil.closeQuietly(stmt);
		}
	}

	/**
	 * 更新热帖的查看数、评论数、点赞数的之前变量，用于排序
	 * 
	 * @param conn
	 * @param companyId
	 * @param now
	 * @param factor
	 * @throws SQLException
	 * void
	 * @throws
	 */
	public static void refreshBoardHotPostCount(Connection conn, long companyId, int now, double factor) throws SQLException {
		StringBuilder sql = new StringBuilder();
		sql.append("UPDATE weizhu_community_post_hot SET update_time = ").append(now).append(", ");
		sql.append("pre_view_cnt = pre_view_cnt * ").append(factor).append(" + cur_view_cnt, cur_view_cnt = 0, ");
		sql.append("pre_comment_cnt = pre_comment_cnt * ").append(factor).append(" + cur_comment_cnt, cur_comment_cnt = 0, ");
		sql.append("pre_like_cnt = pre_like_cnt * ").append(factor).append(" + cur_like_cnt, cur_like_cnt = 0 ");
		sql.append("WHERE company_id = ").append(companyId).append(" AND update_time < ").append(now).append("; ");

		Statement stmt = null;
		try {
			stmt = conn.createStatement();
			stmt.executeUpdate(sql.toString());
		} finally {
			DBUtil.closeQuietly(stmt);
		}
	}

	/**
	 * 创建板块
	 * 
	 * @param conn
	 * @param companyId
	 * @param boardName
	 * @param boardIcon
	 * @param boardDesc
	 * @param parentBoardId
	 * @param isLeafBoard
	 * @param isHot
	 * @param allowModelId
	 * @return
	 * int
	 * @throws
	 */
	public static int insertBoard(Connection conn, long companyId, String boardName, String boardIcon, String boardDesc, @Nullable Integer parentBoardId,
			boolean isLeafBoard, boolean isHot, @Nullable Integer allowModelId) {

		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try {
			pstmt = conn.prepareStatement(
					"INSERT INTO weizhu_community_board(company_id,board_name,board_icon,board_desc,parent_board_id,is_leaf_board,is_hot,allow_model_id) VALUES(?,?,?,?,?,?,?,?);",
					Statement.RETURN_GENERATED_KEYS);
			pstmt.setLong(1, companyId);
			pstmt.setString(2, boardName);
			pstmt.setString(3, boardIcon);
			pstmt.setString(4, boardDesc);
			if (null == parentBoardId) {
				pstmt.setNull(5, java.sql.Types.INTEGER);
			} else {
				pstmt.setInt(5, parentBoardId);
			}
			pstmt.setBoolean(6, isLeafBoard);
			pstmt.setBoolean(7, isHot);
			if (null == allowModelId) {
				pstmt.setNull(8, java.sql.Types.INTEGER);
			} else {
				pstmt.setInt(8, allowModelId);
			}
			pstmt.executeUpdate();
			rs = pstmt.getGeneratedKeys();
			if (rs.next()) {
				return rs.getInt(1);
			} else {
				throw new RuntimeException("插入版块失败");
			}

		} catch (SQLException e) {
			throw new RuntimeException("插入新版块出错！", e);
		} finally {
			DBUtil.closeQuietly(rs);
			DBUtil.closeQuietly(pstmt);
		}

	}

	/**
	 * 更新板块
	 * 
	 * @param conn
	 * @param companyId
	 * @param boardId
	 * @param boardName
	 * @param boardIcon
	 * @param boardDesc
	 * @param isLeafBoard
	 * @param allowModelId
	 * void
	 * @throws
	 */
	public static void updateBoard(Connection conn, long companyId, int boardId, @Nullable String boardName, @Nullable String boardIcon, @Nullable String boardDesc,
			@Nullable Boolean isLeafBoard, @Nullable Integer allowModelId) {
		if (null == boardName && null == boardIcon && null == boardDesc && null == isLeafBoard) {
			throw new RuntimeException("更新数据数据不能为空！");
		}
		StringBuilder sql = new StringBuilder("UPDATE weizhu_community_board SET board_id=").append(boardId);
		if (null != boardName) {
			sql.append(",  board_name='").append(DBUtil.SQL_STRING_ESCAPER.escape(boardName)).append("' ");
		}
		if (null != boardIcon) {
			sql.append(", board_icon='").append(DBUtil.SQL_STRING_ESCAPER.escape(boardIcon)).append("' ");
		}
		if (null != boardDesc) {
			sql.append(", board_desc='").append(DBUtil.SQL_STRING_ESCAPER.escape(boardDesc)).append("' ");
		}
		if (null != isLeafBoard) {
			sql.append(", is_leaf_board=").append(isLeafBoard ? 1 : 0);
		}
		if (null != allowModelId) {
			sql.append(", allow_model_id=").append(allowModelId);

		}
		sql.append("  WHERE company_id = ").append(companyId).append(" AND board_id=").append(boardId).append(";");
		Statement st = null;
		try {
			st = conn.createStatement();
			st.executeUpdate(sql.toString());
		} catch (SQLException e) {
			throw new RuntimeException("更新版块信息出错！", e);
		} finally {
			DBUtil.closeQuietly(st);
		}

	}

   /**
    * 删除版块，并将其下所有帖子和评论的状态变为删除状态和版块下标签
    * @param conn
    * @param companyId
    * @param boardId
    * void
    * @throws
    */
	public static void deleteBoard(Connection conn, long companyId, int boardId) {
		PreparedStatement pstmt = null;
		StringBuilder sql = new StringBuilder();
		sql.append("DELETE FROM  weizhu_community_board WHERE company_id = ? AND board_id=? ; ")// 删除版块
				.append("UPDATE weizhu_community_post SET state = ? WHERE company_id = ? AND board_id = ?; ")
				// 更新帖子状态
				.append("UPDATE weizhu_community_post_comment SET state = ? WHERE company_id = ? AND post_id IN ( SELECT post_id FROM weizhu_community_post WHERE company_id = ? AND board_id=?); ") // 更新评论状态
				.append("DELETE FROM weizhu_community_board_tag WHERE company_id = ? AND board_id = ?;"); // 删除版块下的标签
		
		try {
			pstmt = conn.prepareStatement(sql.toString());
			pstmt.setLong(1, companyId);
			DBUtil.set(pstmt, 2, true, boardId);
			DBUtil.set(pstmt, 3, true, CommunityProtos.Comment.State.DELETE.name());
			pstmt.setLong(4, companyId);
			DBUtil.set(pstmt, 5, true, boardId);
			DBUtil.set(pstmt, 6, true, CommunityProtos.Comment.State.DELETE.name());
			pstmt.setLong(7, companyId);
			pstmt.setLong(8, companyId);
			DBUtil.set(pstmt, 9, true, boardId);
			pstmt.setLong(10, companyId);
			DBUtil.set(pstmt, 11, true, boardId);
			pstmt.executeUpdate();

		} catch (SQLException e) {
			e.printStackTrace();
			throw new RuntimeException("删除版块信息出错！", e);
		} finally {
			DBUtil.closeQuietly(pstmt);
		}

	}

	/**
	 * 设置社区信息，包括社区名称和板块顺序
	 * 
	 * @param conn
	 * @param companyId
	 * @param communityName
	 * @param boardIdOrderStr
	 * void
	 * @throws
	 */
	public static void setCommunity(Connection conn, long companyId, @Nullable String communityName, @Nullable String boardIdOrderStr) {

		if (null == communityName && null == boardIdOrderStr) {
			throw new RuntimeException("社区名称和版块id序列不能同时为空！");
		}

		// boardIdOrderStr校验

		// 将板块id序列拆分放到list中
		List<Integer> boardIdOrderList;
		if (boardIdOrderStr == null || boardIdOrderStr.isEmpty()) {
			boardIdOrderList = Collections.emptyList();
		} else {
			List<String> boardIdStrList = DBUtil.COMMA_SPLITTER.splitToList(boardIdOrderStr);
			boardIdOrderList = new ArrayList<Integer>(boardIdStrList.size());
			for (String boardIdStr : boardIdStrList) {
				try {
					boardIdOrderList.add(Integer.parseInt(boardIdStr));
				} catch (NumberFormatException e) {
					// ignore
				}
			}
		}
		// 将板块放到map中
		Map<Integer, CommunityProtos.Board> boardMap = new HashMap<Integer, CommunityProtos.Board>();
		try {
			CommunityDAOProtos.CommunityInfo communityInfo = getCommunityInfo(conn, companyId);
			for (CommunityProtos.Board board : communityInfo.getBoardList()) {

				boardMap.put(board.getBoardId(), board);
			}
		} catch (SQLException e2) {
			throw new RuntimeException("db failed");
		}

		// 根据board_id_order_str里的顺序构建树结构

		// 根板块id
		List<Integer> rootBoardIdList = new ArrayList<Integer>();
		// 父板块id -> 子板块id列表
		Map<Integer, List<Integer>> parentBoardIdToSubIdListMap = new HashMap<Integer, List<Integer>>();
		// 用来标记板块id是否处理过，避免同一板块被处理多次
		Set<Integer> usedBoardIdSet = new TreeSet<Integer>();

		for (Integer id : boardIdOrderList) {
			if (usedBoardIdSet.contains(id)) {
				continue;
			}

			CommunityProtos.Board board = boardMap.get(id);
			if (board == null) {
				continue;
			}

			if (board.hasParentBoardId()) {
				List<Integer> subList = parentBoardIdToSubIdListMap.get(board.getParentBoardId());
				if (subList == null) {
					subList = new ArrayList<Integer>();
					parentBoardIdToSubIdListMap.put(board.getParentBoardId(), subList);
				}
				subList.add(id);
			} else {
				rootBoardIdList.add(id);
			}

			usedBoardIdSet.add(id);
		}

		// 广度优先遍历树，组织校验后的顺序
		List<Integer> boardIdOrderListValidated = new ArrayList<Integer>();
		Queue<Integer> queue = new LinkedList<Integer>(rootBoardIdList);
		while (!queue.isEmpty()) {
			int boardId = queue.poll();

			CommunityProtos.Board board = boardMap.get(boardId);
			if (board == null) {
				continue;
			}

			boardIdOrderListValidated.add(boardId);

			List<Integer> subList = parentBoardIdToSubIdListMap.get(boardId);
			if (subList != null) {
				queue.addAll(subList);
			}
		}

		// 将新调整好的board_id的顺序赋值给boardIdOrderStr
		boardIdOrderStr = DBUtil.COMMA_JOINER.join(boardIdOrderListValidated);

		// 判断数据库中是否已有社区数据
		Boolean isInsert = null;
		PreparedStatement pstmt = null;
		ResultSet rs = null;

		try {
			pstmt = conn.prepareStatement("SELECT company_id FROM weizhu_community WHERE company_id = ?");
			pstmt.setLong(1, companyId);
			rs = pstmt.executeQuery();
			if (rs.next()) {
				isInsert = false;
			} else {
				isInsert = true;
			}
		} catch (SQLException e1) {
			throw new RuntimeException("获取社区信息出错！", e1);

		} finally {
			DBUtil.closeQuietly(rs);
			DBUtil.closeQuietly(pstmt);
		}

		// 有则更新，无则插入
		if (isInsert) {

			pstmt = null;
			try {
				pstmt = conn.prepareStatement("INSERT INTO weizhu_community(company_id,community_name,board_id_order_str) VALUES(?,?,?)");
				pstmt.setLong(1, companyId);
				pstmt.setString(2, communityName == null ? "社区" : communityName);
				if (null == boardIdOrderStr) {
					pstmt.setNull(3, java.sql.Types.VARCHAR);
				} else {
					pstmt.setString(3, boardIdOrderStr);
				}
				pstmt.executeUpdate();

			} catch (SQLException e) {
				throw new RuntimeException("插入社区信息出错！", e);
			} finally {
				DBUtil.closeQuietly(pstmt);
			}
		} else {
			StringBuilder sql = new StringBuilder();
			sql.append("UPDATE weizhu_community SET ");
			if (null != communityName) {
				sql.append("community_name='").append(DBUtil.SQL_STRING_ESCAPER.escape(communityName)).append("' ");
			}
			if (null != boardIdOrderStr) {
				if (null != communityName) {
					sql.append(", ");
				}
				sql.append("board_id_order_str='").append(DBUtil.SQL_STRING_ESCAPER.escape(boardIdOrderStr)).append("' ");
			}
			sql.append(" WHERE company_id= ").append(companyId).append(";");
			
			Statement st = null;
			try {
				st = conn.createStatement();
				st.executeUpdate(sql.toString());

			} catch (SQLException e) {
				throw new RuntimeException("更新社区信息出错！", e);
			} finally {
				DBUtil.closeQuietly(st);
			}
		}

	}

	/**
	 * 迁移帖子，更改帖子所属板块
	 * 
	 * @param conn
	 * @param companyId
	 * @param postIds
	 * @param boardId
	 * void
	 * @throws
	 */
	public static void updatePostBoard(Connection conn, long companyId, Collection<Integer> postIds, int boardId) {
		if (postIds.isEmpty()) {
			throw new RuntimeException("帖子ID列表不能为空！");
		}

		String postIdStr = DBUtil.COMMA_JOINER.join(postIds);
		Statement st = null;
		StringBuilder sql = new StringBuilder("UPDATE weizhu_community_post SET board_id =").append(boardId)
				.append(" WHERE company_id= ")
				.append(companyId)
				.append(" AND post_id IN (")
				.append(postIdStr)
				.append(");")
				.append("UPDATE weizhu_community_post_hot SET board_id =")
				.append(boardId)
				.append(" WHERE company_id= ")
				.append(companyId)
				.append(" AND post_id IN (")
				.append(postIdStr)
				.append(");");

		try {
			st = conn.createStatement();
			st.executeUpdate(sql.toString());
		} catch (SQLException e) {
			throw new RuntimeException("更新帖子的所属版块出错！");
		} finally {
			DBUtil.closeQuietly(st);
		}

	}

	/**
	 * 根据开始位置和获取个数来获取帖子id列表（用于查询和搜索）
	 * 
	 * @param conn
	 * @param boardId
	 * @param postTitle
	 * @param start
	 * @param length
	 * @param states
	 * @param createUserId 
	 * @return
	 */
	public static DataPage<Integer> getBoardPostIdListByStart(Connection conn, long companyId, @Nullable Integer boardId, @Nullable String postTitle,
			@Nullable Integer start, int length, Collection<CommunityProtos.Post.State> states, Collection<Long> createUserIds, boolean isSticky) {

		// SQL的where 条件
		StringBuilder whereSql = new StringBuilder(" WHERE company_id = ").append(companyId);
		if (null != boardId) {
			whereSql.append(" AND board_id=").append(boardId);
		}
		if (null != postTitle) {
			whereSql.append(" AND post_title LIKE '%").append(DBUtil.SQL_STRING_ESCAPER.escape(postTitle.replace("%", "\\%"))).append("%' ");
		}

		if (!createUserIds.isEmpty()) {
			whereSql.append(" AND create_user_id IN(").append(DBUtil.COMMA_JOINER.join(createUserIds)).append(") ");
		}
		
		StringBuilder stateSql = new StringBuilder();
		if (!states.isEmpty()) {
			stateSql.append(" AND state IN ('");
			boolean isFirst = true;
			for (CommunityProtos.Post.State state : states) {
				if (isFirst) {
					isFirst = false;
				} else {
					stateSql.append("', '");
				}
				stateSql.append(DBUtil.SQL_STRING_ESCAPER.escape(state.name()));
			}
			stateSql.append("') ");

			whereSql.append(stateSql);
		}

		// 获取post_id的SQL
		StringBuilder sql = new StringBuilder("SELECT post_id FROM weizhu_community_post ").append(whereSql);

		// 过滤掉置顶贴
		if (isSticky) {
			sql.append(" AND is_sticky IS NOT true ");
		}

		if (null == start || start < 0) {
			start = 0;
		}
		sql.append(" ORDER BY create_time DESC, post_id DESC LIMIT ").append(start).append(",").append(length).append(";");

		// 获取filtered_size的SQL，包含置顶贴
		sql.append("SELECT COUNT(post_id) FROM weizhu_community_post ").append(whereSql).append("; ");

		// 获取total_size的SQL，包含置顶贴
		sql.append("SELECT COUNT(post_id) FROM weizhu_community_post WHERE company_id = ").append(companyId).append(stateSql).append(";");
		Statement st = null;
		ResultSet rs = null;

		try {
			st = conn.createStatement();
			rs = st.executeQuery(sql.toString());
			List<Integer> postIds = new ArrayList<Integer>();
			int filteredSize = 0;
			int totalSize = 0;
			while (rs.next()) {
				postIds.add(rs.getInt(1));
			}

			DBUtil.closeQuietly(rs);
			rs = null;

			st.getMoreResults();
			rs = st.getResultSet();
			if (rs.next()) {
				filteredSize = rs.getInt(1);
			}

			DBUtil.closeQuietly(rs);
			rs = null;

			st.getMoreResults();
			rs = st.getResultSet();
			if (rs.next()) {
				totalSize = rs.getInt(1);
			}

			return new DataPage<Integer>(postIds, totalSize, filteredSize);
		} catch (SQLException e) {
			throw new RuntimeException("获取帖子id列表出错！");
		} finally {
			DBUtil.closeQuietly(rs);
			DBUtil.closeQuietly(st);
		}

	}

	/**
	 * 根据上次获取的帖子id和获取的数量来获取id列表（用于导出）
	 * 
	 * @param conn
	 * @param boardId
	 * @param postTitle
	 * @param start
	 * @param length
	 * @param states
	 * @return
	 */
	public static List<Integer> getBoardPostIdListByLastId(Connection conn, long companyId, @Nullable Integer boardId, @Nullable String postTitle,
			@Nullable Integer lastPostId, @Nullable Integer size, Collection<CommunityProtos.Post.State> states) {

		StringBuilder sql = new StringBuilder("SELECT post_id FROM weizhu_community_post WHERE company_id = ").append(companyId);
		if (null != boardId) {
			sql.append(" AND board_id=").append(boardId);
		}
		if (null != postTitle) {
			sql.append(" AND post_title LIKE '%").append(DBUtil.SQL_STRING_ESCAPER.escape(postTitle.replace("%", "\\%"))).append("%' ");
		}

		if (null != lastPostId) {
			sql.append(" AND post_id<").append(lastPostId);
		}
		if (!states.isEmpty()) {
			sql.append(" AND state IN ('");
			boolean isFirst = true;
			for (CommunityProtos.Post.State state : states) {
				if (isFirst) {
					isFirst = false;
				} else {
					sql.append("', '");
				}
				sql.append(DBUtil.SQL_STRING_ESCAPER.escape(state.name()));
			}
			sql.append("') ");

		}

		if (null != size) {
			if (size < 0) {
				size = 0;
			}

			sql.append(" ORDER BY create_time DESC, post_id DESC LIMIT ").append(size).append(";");
		} else {
			sql.append(" ORDER BY create_time DESC, post_id DESC;");
		}

		Statement st = null;
		ResultSet rs = null;

		try {
			st = conn.createStatement();
			rs = st.executeQuery(sql.toString());
			List<Integer> postIds = new ArrayList<Integer>();
			while (rs.next()) {
				postIds.add(rs.getInt(1));
			}
			return postIds;
		} catch (SQLException e) {
			throw new RuntimeException("获取帖子id列表出错！");
		} finally {
			DBUtil.closeQuietly(rs);
			DBUtil.closeQuietly(st);
		}

	}

	/**
	 * 根据start和length翻页获取评论列表
	 * 
	 * @param conn
	 * @param companyId
	 * @param postId
	 * @param start
	 * @param length
	 * @param states
	 * @return
	 * DataPage<CommunityProtos.Comment>
	 * @throws
	 */
	public static DataPage<CommunityProtos.Comment> getPostCommentListByStart(Connection conn, long companyId, int postId, @Nullable Integer start, int length,
			Collection<CommunityProtos.Comment.State> states) {

		// SQL的where条件
		StringBuilder whereSql = new StringBuilder(" WHERE company_id = ").append(companyId).append(" AND post_id=").append(postId);
		if (!states.isEmpty()) {
			whereSql.append(" AND state IN ('");
			boolean isFirst = true;
			for (CommunityProtos.Comment.State state : states) {
				if (isFirst) {
					isFirst = false;
				} else {
					whereSql.append("', '");
				}
				whereSql.append(DBUtil.SQL_STRING_ESCAPER.escape(state.name()));
			}
			whereSql.append("') ");
		}

		// 获取comment列表的sql
		StringBuilder sql = new StringBuilder("SELECT post_id,comment_id,reply_comment_id,content,create_user_id,create_time,state FROM weizhu_community_post_comment ").append(whereSql);

		if (null == start || start < 0) {
			start = 0;
		}

		sql.append(" ORDER BY create_time DESC, comment_id DESC LIMIT ").append(start).append(",").append(length).append(";");

		// 获取total_size的SQL
		sql.append("SELECT COUNT(*) FROM weizhu_community_post_comment ").append(whereSql).append(";");

		Statement st = null;
		ResultSet rs = null;

		try {
			st = conn.createStatement();
			rs = st.executeQuery(sql.toString());

			List<CommunityProtos.Comment> comments = new ArrayList<CommunityProtos.Comment>();
			int filterSize = 0;
			int totalSize = 0;
			CommunityProtos.Comment.Builder commentBuilder = CommunityProtos.Comment.newBuilder();
			while (rs.next()) {
				commentBuilder.clear();
				// 将查询出的数据赋值给comment，并给like_count,is_like赋初值
				comments.add(COMMENT_MAPPER.mapToItem(rs, commentBuilder).setLikeCount(0).setIsLike(false).build());
			}

			DBUtil.closeQuietly(rs);
			rs = null;

			st.getMoreResults();
			rs = st.getResultSet();
			if (rs.next()) {
				totalSize = rs.getInt(1);
				filterSize = totalSize;
			}

			return new DataPage<CommunityProtos.Comment>(comments, totalSize, filterSize);
		} catch (SQLException e) {
			throw new RuntimeException("获取帖子评论列表出错！");
		} finally {
			DBUtil.closeQuietly(rs);
			DBUtil.closeQuietly(st);
		}
	}

	/**
	 * 获取板块的子板块ID
	 * 
	 * @param conn
	 * @param companyId
	 * @param parentBoardIds
	 * @return
	 * @throws SQLException
	 * Map<Integer,Set<Integer>>
	 * @throws
	 */
	public static Map<Integer, Set<Integer>> getChildrenBoardId(Connection conn, long companyId, Collection<Integer> parentBoardIds) throws SQLException {

		if (parentBoardIds.isEmpty()) {
			return Collections.emptyMap();
		}

		StringBuilder sql = new StringBuilder("SELECT board_id,parent_board_id FROM weizhu_community_board WHERE company_id = ").append(companyId).append(" AND parent_board_id IN(").append(DBUtil.COMMA_JOINER.join(parentBoardIds))
				.append(");");

		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try {
			pstmt = conn.prepareStatement(sql.toString());
			rs = pstmt.executeQuery();
			Map<Integer, Set<Integer>> parentBoardIdChildrenBoardIdsMap = new HashMap<Integer, Set<Integer>>();
			while (rs.next()) {
				int parentBoardId = rs.getInt("parent_board_id");
				Set<Integer> childrenBoardIds = parentBoardIdChildrenBoardIdsMap.get(parentBoardId);
				if (childrenBoardIds == null) {
					childrenBoardIds = new TreeSet<Integer>();
					parentBoardIdChildrenBoardIdsMap.put(parentBoardId, childrenBoardIds);
				}
				childrenBoardIds.add(rs.getInt("board_id"));
			}
			return parentBoardIdChildrenBoardIdsMap;
		} finally {
			DBUtil.closeQuietly(rs);
			DBUtil.closeQuietly(pstmt);
		}

	}

	/**
	 * 帖子置顶和取消置顶
	 * 
	 * @param conn
	 * @param companyId
	 * @param postIds
	 * @param isSticky
	 * @param stickyTime
	 * void
	 * @throws
	 */
	public static void updatePostSticky(Connection conn, long companyId, Collection<Integer> postIds, boolean isSticky, int stickyTime) {

		if (postIds.isEmpty()) {
			throw new RuntimeException("帖子id列表不能为空！");
		}

		String postIdStr = DBUtil.COMMA_JOINER.join(postIds);
		Statement st = null;
		StringBuilder sql = new StringBuilder("UPDATE weizhu_community_post SET is_sticky =").append(isSticky ? 1 : 0)
				.append(", sticky_time=")
				.append(stickyTime)
				.append(" WHERE company_id = ")
				.append(companyId)
				.append(" AND post_id IN (")
				.append(postIdStr)
				.append(");");

		try {
			st = conn.createStatement();
			st.executeUpdate(sql.toString());
		} catch (SQLException e) {
			throw new RuntimeException("DB FAILED");
		} finally {
			DBUtil.closeQuietly(st);
		}
	}

	/**
	 * 帖子推荐和取消推荐
	 * 
	 * @param conn
	 * @param companyId
	 * @param postIds
	 * @param isRecommend
	 * @param recommendTime
	 * void
	 * @throws
	 */
	public static void updatePostRecommended(Connection conn, long companyId, Collection<Integer> postIds, boolean isRecommend, int recommendTime) {

		if (postIds.isEmpty()) {
			throw new RuntimeException("帖子id列表不能为空！");
		}

		String postIdStr = DBUtil.COMMA_JOINER.join(postIds);
		Statement st = null;
		StringBuilder sql = new StringBuilder("UPDATE weizhu_community_post SET is_recommend =").append(isRecommend ? 1 : 0)
				.append(", recommend_time=")
				.append(recommendTime)
				.append(" WHERE company_id = ")
				.append(companyId)
				.append(" AND post_id IN (")
				.append(postIdStr)
				.append(");");

		try {
			st = conn.createStatement();
			st.executeUpdate(sql.toString());
		} catch (SQLException e) {
			throw new RuntimeException("DB FAILED");
		} finally {
			DBUtil.closeQuietly(st);
		}

	}

	/**
	 * 获取用户点过赞的评论ID列表的相关map，key是帖子ID，value是用户点过赞的评论的ID列表
	 * 
	 * @param conn
	 * @param companyId
	 * @param userId
	 * @param postCommentIdMap
	 * @return
	 * @throws SQLException
	 * Map<Integer,Set<Integer>>
	 * @throws
	 */
	public static Map<Integer, Set<Integer>> getUserPostCommentLikeIdSet(Connection conn, long companyId, long userId, Map<Integer, List<Integer>> postCommentIdMap)
			throws SQLException {
		if (postCommentIdMap.isEmpty()) {
			return Collections.emptyMap();
		}

		StringBuilder sql = new StringBuilder();
		sql.append("SELECT post_id,comment_id FROM weizhu_community_comment_like WHERE company_id = ")
				.append(companyId)
				.append(" AND (post_id,comment_id) IN (");

		boolean isFirst = true;
		for (Entry<Integer, List<Integer>> entry : postCommentIdMap.entrySet()) {
			Integer postId = entry.getKey();
			for (Integer commentId : entry.getValue()) {
				if (isFirst) {
					isFirst = false;
				} else {
					sql.append(", ");
				}
				sql.append("(").append(postId).append(", ").append(commentId).append(")");
			}
		}
		sql.append(") AND user_id = ").append(userId).append(";");

		if (isFirst) {
			// sql里的数据为空
			return Collections.emptyMap();
		}

		Statement stmt = null;
		ResultSet rs = null;

		try {
			stmt = conn.createStatement();
			rs = stmt.executeQuery(sql.toString());

			Map<Integer, Set<Integer>> postCommentLikeIdSetMap = new HashMap<Integer, Set<Integer>>();

			while (rs.next()) {
				int postId = rs.getInt("post_id");
				int commentId = rs.getInt("comment_id");
				Set<Integer> commentIds = postCommentLikeIdSetMap.get(postId);
				if (commentIds == null) {
					commentIds = new TreeSet<Integer>();
					postCommentLikeIdSetMap.put(postId, commentIds);
				}
				commentIds.add(commentId);
			}
			return postCommentLikeIdSetMap;
		} finally {
			DBUtil.closeQuietly(rs);
			DBUtil.closeQuietly(stmt);
		}

	}

	/**
	 * 评论点赞
	 * 
	 * @param conn
	 * @param companyId
	 * @param userId
	 * @param postId
	 * @param commentId
	 * @param now
	 * @throws SQLException
	 * void
	 * @throws
	 */
	public static void insertUserCommentLike(Connection conn, long companyId, long userId, int postId, int commentId, int now) throws SQLException {
		PreparedStatement pstmt = null;
		try {
			pstmt = conn.prepareStatement("INSERT IGNORE weizhu_community_comment_like (company_id, post_id, comment_id, user_id, create_time) VALUES (?, ?, ?, ?, ?);");
			pstmt.setLong(1, companyId);
			pstmt.setInt(2, postId);
			pstmt.setInt(3, commentId);
			pstmt.setLong(4, userId);
			pstmt.setInt(5, now);

			pstmt.executeUpdate();
		} finally {
			DBUtil.closeQuietly(pstmt);
		}
	}

	/**
	 * 评论取消点赞
	 * 
	 * @param conn
	 * @param companyId
	 * @param userId
	 * @param postId
	 * @param commentId
	 * @throws SQLException
	 * void
	 * @throws
	 */
	public static void deleteUserCommentLike(Connection conn, long companyId, long userId, int postId, int commentId) throws SQLException {
		PreparedStatement pstmt = null;
		try {
			pstmt = conn.prepareStatement("DELETE FROM weizhu_community_comment_like WHERE company_id = ? AND post_id = ? AND comment_id = ? AND user_id = ?;");
			pstmt.setLong(1, companyId);
			pstmt.setInt(2, postId);
			pstmt.setInt(3, commentId);
			pstmt.setLong(4, userId);

			pstmt.executeUpdate();
		} finally {
			DBUtil.closeQuietly(pstmt);
		}
	}

	/**
	 * 获取评论的点赞数
	 * 
	 * @param conn
	 * @param companyId
	 * @param postCommentIdMap
	 * @return
	 * @throws SQLException
	 * Map<Integer,Map<Integer,CommunityDAOProtos.PostCommentExt>>
	 * @throws
	 */
	public static Map<Integer, Map<Integer, CommunityDAOProtos.PostCommentExt>> getPostCommentExt(Connection conn, long companyId,
			Map<Integer, List<Integer>> postCommentIdMap) throws SQLException {
		if (postCommentIdMap.isEmpty()) {
			return Collections.emptyMap();
		}

		StringBuilder sql = new StringBuilder();
		sql.append("SELECT post_id, comment_id, COUNT(*) AS like_count FROM weizhu_community_comment_like WHERE company_id = ")
				.append(companyId)
				.append(" AND (post_id, comment_id) IN (");

		boolean isFirst = true;
		for (Entry<Integer, List<Integer>> entry : postCommentIdMap.entrySet()) {
			Integer postId = entry.getKey();
			for (Integer commentId : entry.getValue()) {
				if (isFirst) {
					isFirst = false;
				} else {
					sql.append(", ");
				}
				sql.append("(").append(postId).append(", ").append(commentId).append(")");
			}
		}
		sql.append(") GROUP BY post_id, comment_id; ");

		if (isFirst) {
			// sql里的数据为空
			return Collections.emptyMap();
		}

		Statement stmt = null;
		ResultSet rs = null;
		try {
			stmt = conn.createStatement();
			rs = stmt.executeQuery(sql.toString());

			Map<Integer, Map<Integer, CommunityDAOProtos.PostCommentExt>> postCommentExtMap = new HashMap<Integer, Map<Integer, CommunityDAOProtos.PostCommentExt>>();
			CommunityDAOProtos.PostCommentExt.Builder tmpBuilder = CommunityDAOProtos.PostCommentExt.newBuilder();

			while (rs.next()) {
				int postId = rs.getInt("post_id");
				int commentId = rs.getInt("comment_id");
				int likeCount = rs.getInt("like_count");

				Map<Integer, CommunityDAOProtos.PostCommentExt> map = postCommentExtMap.get(postId);
				if (map == null) {
					map = new HashMap<Integer, CommunityDAOProtos.PostCommentExt>();
					postCommentExtMap.put(postId, map);
				}

				map.put(commentId, tmpBuilder.clear().setLikeCount(likeCount).build());
			}

			return postCommentExtMap;

		} finally {
			DBUtil.closeQuietly(rs);
			DBUtil.closeQuietly(stmt);
		}

	}

	/**
	 * 根据帖子ID和评论id获取评论详情
	 * @param conn
	 * @param companyId
	 * @param postCommentIds
	 * @param states
	 * @return
	 * List<CommunityProtos.Comment>
	 * @throws
	 */
	public static List<CommunityProtos.Comment> getPostCommentListById(Connection conn, long companyId,
			List<CommunityProtos.GetPostCommentByIdRequest.PostCommentId> postCommentIds, Collection<CommunityProtos.Comment.State> states) {

		if (postCommentIds.isEmpty()) {
			return Collections.emptyList();
		}
		StringBuilder sql = new StringBuilder();
		sql.append("SELECT post_id,comment_id,reply_comment_id,content,create_user_id,create_time,state FROM weizhu_community_post_comment WHERE company_id = ")
				.append(companyId)
				.append(" AND (post_id, comment_id) IN (");

		boolean isFirst = true;
		for (CommunityProtos.GetPostCommentByIdRequest.PostCommentId postCommentId : postCommentIds) {
			if (isFirst) {
				isFirst = false;
			} else {
				sql.append(", ");
			}
			sql.append("(").append(postCommentId.getPostId()).append(", ").append(postCommentId.getCommentId()).append(")");
		}
		sql.append(") ");

		if (!states.isEmpty()) {
			sql.append(" AND state IN ('");
			boolean isFirstState = true;
			for (CommunityProtos.Comment.State state : states) {
				if (isFirstState) {
					isFirstState = false;
				} else {
					sql.append("', '");
				}
				sql.append(DBUtil.SQL_STRING_ESCAPER.escape(state.name()));
			}
			sql.append("') ");
		}

		sql.append("; ");

		Statement st = null;
		ResultSet rs = null;

		try {
			st = conn.createStatement();
			rs = st.executeQuery(sql.toString());
			List<CommunityProtos.Comment> commentList = new ArrayList<CommunityProtos.Comment>();
			CommunityProtos.Comment.Builder commentBuilder = CommunityProtos.Comment.newBuilder();
			while (rs.next()) {
				commentBuilder.clear();
				commentList.add(COMMENT_MAPPER.mapToItem(rs, commentBuilder).build());
			}

			return commentList;
		} catch (SQLException e) {
			throw new RuntimeException("获取帖子评论列表出错！");
		} finally {
			DBUtil.closeQuietly(rs);
			DBUtil.closeQuietly(st);
		}

	}

	/**
	 * 根据帖子ID获取其所属的板块ID
	 * 
	 * @param conn
	 * @param companyId
	 * @param postIds
	 * @return
	 * @throws SQLException
	 * Set<Integer>
	 * @throws
	 */
	public static Set<Integer> getBoardIdsByPostIds(Connection conn, long companyId, Collection<Integer> postIds) throws SQLException {

		StringBuilder sql = new StringBuilder();
		sql.append("SELECT board_id FROM weizhu_community_post WHERE company_id = ")
				.append(companyId)
				.append(" AND post_id IN(").append(DBUtil.COMMA_JOINER.join(postIds)).append(");");

		Statement st = null;
		ResultSet rs = null;
		try {
			st = conn.createStatement();
			rs = st.executeQuery(sql.toString());

			Set<Integer> boardIds = new TreeSet<Integer>();
			while (rs.next()) {
				boardIds.add(rs.getInt(1));
			}
			return boardIds;
		} finally {
			DBUtil.closeQuietly(rs);
			DBUtil.closeQuietly(st);
		}
	}

	/**
	 * 获取使用社区的所有公司ID
	 * 
	 * @param conn
	 * @return
	 * @throws SQLException
	 * Set<Long>
	 * @throws
	 */
	public static Set<Long> getCompanyId(Connection conn) throws SQLException {
		
		Statement st = null;
		ResultSet rs = null;
		
		try {
			st = conn.createStatement();

			rs = st.executeQuery("SELECT DISTINCT company_id FROM weizhu_community_board;");
			Set<Long> companyIds = new TreeSet<Long>();
			while(rs.next()){
				companyIds.add(rs.getLong("company_id"));
			}
			
			return companyIds;
		} finally{
			DBUtil.closeQuietly(rs);
			DBUtil.closeQuietly(st);
		}
	}

	/**
	 * 获取热门评论列表
	 * 
	 * @param conn
	 * @param companyId
	 * @param postIds
	 * @param maxNum
	 * @param states
	 * @return
	 * @throws SQLException
	 * Map<Integer,List<Comment>>
	 * @throws
	 */
	public static Map<Integer, List<Comment>> getHotCommentList(Connection conn, long companyId, Collection<Integer> postIds, int maxNum, Collection<State> states) throws SQLException {

		if(postIds.isEmpty()){
			return Collections.emptyMap();
		}
		StringBuilder stateCondition = new StringBuilder();
		if (!states.isEmpty()) {
			stateCondition.append(" AND state IN ('");
			boolean isFirstState = true;
			for (CommunityProtos.Comment.State state : states) {
				if (isFirstState) {
					isFirstState = false;
				} else {
					stateCondition.append("', '");
				}
				stateCondition.append(DBUtil.SQL_STRING_ESCAPER.escape(state.name()));
			}
			stateCondition.append("') ");
		}
		
		
		StringBuilder sql = new StringBuilder();
		for (int postId : postIds) {
			sql.append("select * from weizhu_community_post_comment A LEFT JOIN")
					.append("(select post_id,comment_id, count(user_id) like_count from weizhu_community_comment_like group by post_id, comment_id) AS B ")
					.append(" ON A.post_id=B.post_id AND A.comment_id=B.comment_id WHERE A.company_id = ")
					.append(companyId)
					.append(stateCondition)
					.append(" AND A.post_id = ")
					.append(postId)
					.append(" ORDER BY like_count DESC, create_time DESC LIMIT ")
					.append(maxNum)
					.append(";");
		}
		
		Statement st = null;
		ResultSet rs = null;
		
		try {
			st = conn.createStatement();

			rs = st.executeQuery(sql.toString());
			
			Map<Integer, List<Comment>> postIdHotCommentListMap = new HashMap<Integer, List<Comment>>();
			Comment.Builder commentBuilder = Comment.newBuilder();
			
			for (int i = 0; i < postIds.size(); i++) {

				if (i != 0) {
					DBUtil.closeQuietly(rs);
					rs = null;
					st.getMoreResults();
					rs = st.getResultSet();
				}
				
				while(rs.next()){
					
					int postId = rs.getInt("post_id");
					List<Comment> commentList = postIdHotCommentListMap.get(postId);
					if(commentList==null){
						commentList = new ArrayList<Comment>();
						postIdHotCommentListMap.put(postId, commentList);
					}
					
					commentBuilder.clear();
					commentList.add(COMMENT_MAPPER.mapToItem(rs, commentBuilder).build());
				}

			}
			
			return postIdHotCommentListMap;
		} finally{
			DBUtil.closeQuietly(rs);
			DBUtil.closeQuietly(st);
		}
	}

	/**
	 * 在板块下创建标签
	 * 
	 * @param conn
	 * @param companyId
	 * @param boardId
	 * @param tagList
	 * @throws SQLException
	 * void
	 * @throws
	 */
	public static void insertBoardTag(Connection conn, long companyId, int boardId, Collection<String> tagList) throws SQLException {
		
		if(tagList.isEmpty()){
			return;
		}
		
		PreparedStatement pstmt = null;
		try {
			pstmt = conn.prepareStatement("INSERT INTO weizhu_community_board_tag(company_id,board_id,tag) VALUES(?,?,?);");
			for(String tag:tagList){
				pstmt.setLong(1, companyId);
				pstmt.setInt(2, boardId);
				pstmt.setString(3, DBUtil.SQL_STRING_ESCAPER.escape(tag));
				pstmt.addBatch();
			}
			pstmt.executeBatch();
		} finally{
			DBUtil.closeQuietly(pstmt);
		}
	}

	/**
	 * 删除板块下的标签
	 * 
	 * @param conn
	 * @param companyId
	 * @param boardId
	 * @param tagList
	 * @throws SQLException
	 * void
	 * @throws
	 */
	public static void deleteBoardTag(Connection conn, long companyId, int boardId, Collection<String> tagList) throws SQLException {
		
		if(tagList.isEmpty()){
			return;
		}
		
		List<String> tagListTmp = new ArrayList<String>();
		for(String tag : tagList){
			tagListTmp.add(DBUtil.SQL_STRING_ESCAPER.escape(tag));
		}
		
		StringBuilder sql = new StringBuilder();
		sql.append("DELETE FROM weizhu_community_board_tag WHERE company_id = ")
				.append(companyId)
				.append(" AND board_id = ")
				.append(boardId)
				.append(" AND tag IN('")
				.append(DBUtil.QUOTE_COMMA_JOINER.join(tagList))
				.append("');");
		Statement st = null;
		try {
			st = conn.createStatement();
		    st.executeUpdate(sql.toString());
		} finally{
			DBUtil.closeQuietly(st);
		}
	}
	
	/**
	 * 给帖子打标签
	 * 
	 * @param conn
	 * @param companyId
	 * @param postId
	 * @param tagList
	 * @throws SQLException
	 * void
	 * @throws
	 */
	public static void insertPostTag(Connection conn, long companyId, int postId, Collection<String> tagList) throws SQLException {
		
		if(tagList.isEmpty()){
			return;
		}
		
		PreparedStatement pstmt = null;
		try {
			pstmt = conn.prepareStatement("INSERT INTO weizhu_community_post_tag(company_id,post_id,tag) VALUES(?,?,?);");
			for(String tag:tagList){
				pstmt.setLong(1, companyId);
				pstmt.setInt(2, postId);
				pstmt.setString(3, DBUtil.SQL_STRING_ESCAPER.escape(tag));
				pstmt.addBatch();
			}
			pstmt.executeBatch();
		} finally{
			DBUtil.closeQuietly(pstmt);
		}
	}

	/**
	 * 获取板块下的标签
	 * 
	 * @param conn
	 * @param companyId
	 * @param boardId
	 * @return
	 * @throws SQLException
	 * List<String>
	 * @throws
	 */
	public static List<String> getBoardTag(Connection conn, long companyId, int boardId) throws SQLException {
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try {
			pstmt = conn.prepareStatement("SELECT tag FROM  weizhu_community_board_tag WHERE company_id = ? AND board_id = ?;");
			pstmt.setLong(1, companyId);
			pstmt.setInt(2, boardId);
			rs = pstmt.executeQuery();
			List<String> tagList = new ArrayList<String>();
			while(rs.next()){
				tagList.add(rs.getString("tag"));
			}
			return tagList;
		} finally {
			DBUtil.closeQuietly(pstmt);
		}
	}
	
	/**
	 * 获取帖子的点赞列表
	 * 
	 * @param conn
	 * @param companyId
	 * @param postId
	 * @param size
	 * @param currentOffsetIndex
	 * @return
	 * @throws SQLException
	 * List<PostLike>
	 * @throws
	 */
	public static List<PostLike> getPostLikeList(Connection conn, long companyId, int postId, int size, @Nullable PostLike currentOffsetIndex) throws SQLException {
		StringBuilder sql = new StringBuilder();
		sql.append("SELECT * FROM weizhu_community_post_like WHERE company_id = ").append(companyId).append(" AND post_id = ").append(postId);
		if (currentOffsetIndex != null) {
			sql.append(" AND (create_time < ")
					.append(currentOffsetIndex.getCreateTime())
					.append(" OR (create_time = ")
					.append(currentOffsetIndex.getCreateTime())
					.append(" AND user_id < ")
					.append(currentOffsetIndex.getUserId())
					.append(")) ");
		}
		sql.append(" ORDER BY create_time DESC, user_id DESC LIMIT ").append(size).append("; ");

		Statement st = null;
		ResultSet rs = null;
		try {
			st = conn.createStatement();
			rs = st.executeQuery(sql.toString());

			List<PostLike> resultList = new ArrayList<PostLike>();

			PostLike.Builder tmpBuilder = PostLike.newBuilder();
			while (rs.next()) {
				tmpBuilder.clear();
				resultList.add(POST_LIKE_MAPPER.mapToItem(rs, tmpBuilder).build());
			}
			return resultList;
		} finally {
			DBUtil.closeQuietly(rs);
			DBUtil.closeQuietly(st);
		}
	}

}
