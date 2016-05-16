package com.weizhu.service.qa;

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
import com.weizhu.proto.QAProtos;

/**
 * 问答模块DB访问类，对应于手机端
 * 
 * @author zhangjun
 *
 */
public class QADB {
	private static final ProtobufMapper<QAProtos.Question> QUESTION_MAPPER = ProtobufMapper.createMapper(QAProtos.Question.getDefaultInstance(),
			"question_id",
			"question_content",
			"user_id",
			"admin_id",
			"category_id",
			"create_time");
	private static final ProtobufMapper<QAProtos.Answer> ANSWER_MAPPER = ProtobufMapper.createMapper(QAProtos.Answer.getDefaultInstance(),
			"answer_id",
			"question_id",
			"user_id",
			"admin_id",
			"answer_content",
			"create_time");
	private static final ProtobufMapper<QAProtos.Category> CATEGORY_MAPPER = ProtobufMapper.createMapper(QAProtos.Category.getDefaultInstance(),
			"category_id",
			"category_name",
			"user_id",
			"admin_id",
			"create_time");

	/**
	 * 根据id获取question的详细信息
	 * 
	 * @param conn
	 * @param questionIds
	 * @return
	 * @throws SQLException
	 */
	public static Map<Integer, QAProtos.Question> getQuestion(Connection conn, long companyId, Collection<Integer> questionIds) throws SQLException {
		if (questionIds.isEmpty()) {
			return Collections.emptyMap();
		}

		String questionIdStr = DBUtil.COMMA_JOINER.join(questionIds);
		Statement st = null;
		ResultSet rs = null;
		try {
			StringBuilder sql = new StringBuilder();
			sql.append("SELECT question_id,question_content,user_id,admin_id,category_id,create_time FROM weizhu_qa_question WHERE company_id = ")
					.append(companyId)
					.append(" AND question_id IN(")
					.append(questionIdStr)
					.append(");");
			st = conn.createStatement();
			rs = st.executeQuery(sql.toString());
			Map<Integer, QAProtos.Question> questionInfoMap = new HashMap<Integer, QAProtos.Question>();
			QAProtos.Question.Builder questionBuilder = QAProtos.Question.newBuilder();
			while (rs.next()) {
				questionBuilder.clear();

				QUESTION_MAPPER.mapToItem(rs, questionBuilder);
				questionInfoMap.put(rs.getInt("question_id"), questionBuilder.build());
			}
			return questionInfoMap;
		} finally {
			DBUtil.closeQuietly(rs);
			DBUtil.closeQuietly(st);
		}
	}

	/**
	 * 获取answerId列表
	 * 
	 * @param conn
	 * @param lastAnswerId 上次获取的最后一个元素的ID
	 * @param size 需要获取的元素个数
	 * @param questionId 回答所隶属的问题id
	 * @return
	 * @throws SQLException
	 */
	public static List<Integer> getAnswerIdListByLastId(Connection conn, long companyId, @Nullable Integer lastAnswerId, int size, int questionId)
			throws SQLException {
		if (size <= 0) {
			return Collections.emptyList();
		}

		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try {
			if (lastAnswerId == null) {
				pstmt = conn.prepareStatement("SELECT a.answer_id FROM weizhu_qa_answer a LEFT JOIN  (SELECT answer_id,COUNT(user_id) AS like_num FROM weizhu_qa_answer_like WHERE company_id = ? GROUP BY answer_id ) l ON  a.answer_id=l.answer_id WHERE company_id = ? AND a.question_id=? ORDER BY l.like_num DESC,a.answer_id DESC LIMIT ?; ");
				pstmt.setLong(1, companyId);
				pstmt.setLong(2, companyId);
				pstmt.setInt(3, questionId);
				pstmt.setInt(4, size);
			} else {
				pstmt = conn.prepareStatement("SELECT a.answer_id FROM weizhu_qa_answer a LEFT JOIN  (SELECT answer_id,COUNT(user_id) AS like_num FROM weizhu_qa_answer_like WHERE company_id = ? GROUP BY answer_id ) l ON  a.answer_id=l.answer_id WHERE company_id = ? AND a.question_id=? AND a.answer_id<? ORDER BY l.like_num DESC,a.answer_id DESC LIMIT ?;  ");
				pstmt.setLong(1, companyId);
				pstmt.setLong(2, companyId);
				pstmt.setInt(3, questionId);
				pstmt.setInt(4, lastAnswerId);
				pstmt.setInt(5, size);
			}
			rs = pstmt.executeQuery();
			List<Integer> answerIdList = new ArrayList<Integer>();
			while (rs.next()) {
				answerIdList.add(rs.getInt("answer_id"));
			}
			return answerIdList;
		} finally {
			DBUtil.closeQuietly(rs);
			DBUtil.closeQuietly(pstmt);
		}
	}

	/**
	 * 根据id获取回答的详细信息
	 * 
	 * @param conn
	 * @param answerIds
	 * @return
	 * @throws SQLException
	 */
	public static Map<Integer, QAProtos.Answer> getAnswer(Connection conn, long companyId, Collection<Integer> answerIds) throws SQLException {
		if (answerIds.isEmpty()) {
			return Collections.emptyMap();
		}

		String answerIdStr = DBUtil.COMMA_JOINER.join(answerIds);
		Statement st = null;
		ResultSet rs = null;
		try {
			StringBuilder sql = new StringBuilder();
			sql.append("SELECT answer_id,question_id,answer_content,user_id,admin_id,create_time FROM weizhu_qa_answer WHERE company_id = ")
					.append(companyId)
					.append(" AND answer_id IN(")
					.append(answerIdStr)
					.append(");");
			st = conn.createStatement();

			rs = st.executeQuery(sql.toString());
			Map<Integer, QAProtos.Answer> answerInfoMap = new HashMap<Integer, QAProtos.Answer>();
			QAProtos.Answer.Builder answerBuilder = QAProtos.Answer.newBuilder();
			while (rs.next()) {
				answerBuilder.clear();

				ANSWER_MAPPER.mapToItem(rs, answerBuilder);
				answerInfoMap.put(rs.getInt("answer_id"), answerBuilder.build());
			}
			return answerInfoMap;
		} finally {
			DBUtil.closeQuietly(rs);
			DBUtil.closeQuietly(st);
		}
	}

	/**
	 * 新增回答
	 * 
	 * @param conn
	 * @param questionId
	 * @param userId
	 * @param answerContent
	 * @param initLikeNum
	 * @param createTime
	 * @return
	 * @throws SQLException
	 */
	public static int insertAnswer(Connection conn, long companyId, int questionId, @Nullable Long userId, @Nullable Long adminId, String answerContent,
			int createTime) throws SQLException {
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try {
			pstmt = conn.prepareStatement("INSERT INTO weizhu_qa_answer(company_id,question_id,user_id,admin_id,answer_content,create_time) VALUES (?,?,?,?,?,?);",
					Statement.RETURN_GENERATED_KEYS);
			pstmt.setLong(1, companyId);
			pstmt.setInt(2, questionId);
			if (userId == null) {
				pstmt.setNull(3, java.sql.Types.BIGINT);
			} else {
				pstmt.setLong(3, userId);
			}
			if (adminId == null) {
				pstmt.setNull(4, java.sql.Types.BIGINT);
			} else {
				pstmt.setLong(4, adminId);
			}
			pstmt.setString(5, answerContent);
			pstmt.setInt(6, createTime);

			pstmt.executeUpdate();
			rs = pstmt.getGeneratedKeys();
			if (rs.next()) {
				return rs.getInt(1);
			} else {
				throw new RuntimeException("未发现新生成的答案id！");
			}
		} finally {
			DBUtil.closeQuietly(rs);
			DBUtil.closeQuietly(pstmt);
		}
	}

	/**
	 * 为某个回答点赞
	 * 
	 * @param conn
	 * @param userId
	 * @param answerId
	 * @throws SQLException
	 */
	public static void likeAnswer(Connection conn, long companyId, @Nullable Long userId, int answerId) throws SQLException {
		if (userId == null) {
			return;
		}
		PreparedStatement pstmt = null;
		try {
			pstmt = conn.prepareStatement("INSERT IGNORE INTO weizhu_qa_answer_like(company_id,user_id,answer_id) VALUES (?,?,?);");
			pstmt.setLong(1, companyId);
			pstmt.setLong(2, userId);
			pstmt.setInt(3, answerId);

			pstmt.executeUpdate();

		} finally {
			DBUtil.closeQuietly(pstmt);
		}
	}

	/**
	 * 取消点赞
	 * 
	 * @param conn
	 * @param answerId
	 * @param userId
	 * @throws SQLException
	 */
	public static void deletelikeAnswer(Connection conn, long companyId, @Nullable Long userId, int answerId) throws SQLException {
		if (userId == null) {
			return;
		}
		PreparedStatement pstmt = null;
		try {
			pstmt = conn.prepareStatement("DELETE FROM weizhu_qa_answer_like WHERE company_id = ? AND user_id=? AND answer_id=? ;");
			pstmt.setLong(1, companyId);
			pstmt.setLong(2, userId);
			pstmt.setInt(3, answerId);
			pstmt.executeUpdate();

		} finally {
			DBUtil.closeQuietly(pstmt);
		}
	}

	/**
	 * 获取answerIds，被userId用户点过赞的回答的ID
	 * 
	 * @param conn
	 * @param companyId
	 * @param answerIds
	 * @param userId
	 * @return
	 * @throws SQLException
	 * Set<Integer>
	 * @throws
	 */
	public static Set<Integer> getIsLike(Connection conn, long companyId, Set<Integer> answerIds, @Nullable Long userId) throws SQLException {
		if (answerIds.isEmpty() || userId == null) {
			return Collections.emptySet();
		}
		String answerIdStr = DBUtil.COMMA_JOINER.join(answerIds);
		Statement st = null;
		ResultSet rs = null;
		try {
			StringBuilder sql = new StringBuilder();
			sql.append("SELECT answer_id FROM weizhu_qa_answer_like WHERE company_id = ")
					.append(companyId)
					.append(" AND user_id=")
					.append(userId)
					.append(" AND answer_id IN (")
					.append(answerIdStr)
					.append("); ");
			st = conn.createStatement();
			rs = st.executeQuery(sql.toString());
			Set<Integer> answerIdSet = new TreeSet<Integer>();
			while (rs.next()) {
				answerIdSet.add(rs.getInt(1));
			}
			return answerIdSet;
		} finally {
			DBUtil.closeQuietly(rs);
			DBUtil.closeQuietly(st);
		}
	}

	/**
	 * 获取符合参数要求的问题的总数
	 * 
	 * @param conn
	 * @param companyId
	 * @param categoryId
	 * @param keyword
	 * @return
	 * @throws SQLException
	 * int
	 * @throws
	 */
	public static int getTotalQuestionNum(Connection conn, long companyId, @Nullable Integer categoryId, String keyword) throws SQLException {
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		keyword = "%" + keyword.replace("%", "\\%") + "%";
		try {
			if (categoryId == null) {
				pstmt = conn.prepareStatement("SELECT count(question_id) FROM weizhu_qa_question WHERE company_id = ? AND question_content LIKE ?; ");
				pstmt.setLong(1, companyId);
				pstmt.setString(2, keyword);
			} else {
				pstmt = conn.prepareStatement("SELECT count(question_id) FROM weizhu_qa_question WHERE company_id = ? AND question_content LIKE ? AND category_id = ? ;");
				pstmt.setLong(1, companyId);
				pstmt.setString(2, keyword);
				pstmt.setInt(3, categoryId);
			}
			rs = pstmt.executeQuery();
			if (rs.next()) {
				return rs.getInt(1);
			}
			return 0;
		} finally {
			DBUtil.closeQuietly(rs);
			DBUtil.closeQuietly(pstmt);
		}
	}

	/**
	 * 批量新增问题
	 * 
	 * @param conn
	 * @param questionContents
	 * @param userId
	 * @param initNum
	 * @param initCateoryId
	 * @param createTime
	 * @return
	 * @return
	 * @throws SQLException
	 */
	public static List<Integer> insertQuestion(Connection conn, long companyId, List<String> questionContents, @Nullable Long userId, @Nullable Long adminId,
			int initCateoryId, int createTime) throws SQLException {
		if (questionContents.isEmpty()) {
			return Collections.emptyList();
		}
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try {
			pstmt = conn.prepareStatement("INSERT INTO weizhu_qa_question(company_id,question_content,user_id,admin_id,category_id,create_time) VALUES (?,?,?,?,?,?);",
					Statement.RETURN_GENERATED_KEYS);

			int qConListSize = questionContents.size();
			List<Integer> questionIds = new ArrayList<Integer>();
			for (int i = 0; i < qConListSize; i++) {
				pstmt.setLong(1, companyId);
				pstmt.setString(2, questionContents.get(i));
				if (null == userId) {
					pstmt.setNull(3, java.sql.Types.BIGINT);
				} else {
					pstmt.setLong(3, userId);
				}
				if (null == adminId) {
					pstmt.setNull(4, java.sql.Types.BIGINT);
				} else {
					pstmt.setLong(4, adminId);
				}

				pstmt.setInt(5, initCateoryId);
				pstmt.setInt(6, createTime);
				pstmt.addBatch();
			}
			pstmt.executeBatch();
			rs = pstmt.getGeneratedKeys();
			while (rs.next()) {
				questionIds.add(rs.getInt(1));
			}

			return questionIds;

		} finally {
			DBUtil.closeQuietly(rs);
			DBUtil.closeQuietly(pstmt);
		}
	}

	/**
	 * 根据问题列表获取回答列表
	 * 
	 * @param conn
	 * @param qIdList
	 * @return
	 * @throws SQLException
	 */
	public static List<Integer> getAnswerIdListByQusIds(Connection conn, long companyId, List<Integer> qIdList) throws SQLException {
		if (qIdList.isEmpty()) {
			return Collections.emptyList();
		}

		String questionIdStr = DBUtil.COMMA_JOINER.join(qIdList);
		Statement st = null;
		ResultSet rs = null;
		try {
			StringBuilder sql = new StringBuilder();
			sql.append("SELECT answer_id FROM weizhu_qa_answer WHERE company_id = ")
					.append(companyId)
					.append(" AND question_id IN(")
					.append(questionIdStr)
					.append(");");
			st = conn.createStatement();

			rs = st.executeQuery(sql.toString());
			List<Integer> answers = new ArrayList<Integer>();
			while (rs.next()) {
				answers.add(rs.getInt(1));
			}
			return answers;
		} finally {
			DBUtil.closeQuietly(rs);
			DBUtil.closeQuietly(st);
		}
	}

	/**
	 * 根据问题id列表获得分类id列表
	 * 
	 * @param conn
	 * @param qIdList
	 * @return
	 * @throws SQLException
	 */
	public static Set<Integer> getCategoryIdListByQueId(Connection conn, long companyId, List<Integer> qIdList) throws SQLException {
		if (qIdList.isEmpty()) {
			return Collections.emptySet();
		}

		String questionIdStr = DBUtil.COMMA_JOINER.join(qIdList);
		Statement st = null;
		ResultSet rs = null;
		try {
			StringBuilder sql = new StringBuilder();
			sql.append("SELECT DISTINCT category_id FROM weizhu_qa_question WHERE company_id = ")
					.append(companyId)
					.append(" AND question_id IN(").append(questionIdStr).append(");");
			st = conn.createStatement();

			rs = st.executeQuery(sql.toString());
			Set<Integer> categoryIds = new TreeSet<Integer>();
			while (rs.next()) {
				categoryIds.add(rs.getInt(1));
			}
			return categoryIds;
		} finally {
			DBUtil.closeQuietly(rs);
			DBUtil.closeQuietly(st);
		}
	}

	/**
	 * 获取问题下的回答总数
	 * 
	 * @param conn
	 * @param companyId
	 * @param questionId
	 * @return
	 * @throws SQLException
	 * int
	 * @throws
	 */
	public static int getTotalAnswerNum(Connection conn, long companyId, int questionId) throws SQLException {
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try {
			pstmt = conn.prepareStatement("SELECT count(answer_id) FROM weizhu_qa_answer where company_id = ? AND question_id=?; ");
			pstmt.setLong(1, companyId);
			pstmt.setInt(2, questionId);
			rs = pstmt.executeQuery();
			if (rs.next()) {
				return rs.getInt(1);
			}
			return 0;
		} finally {
			DBUtil.closeQuietly(rs);
			DBUtil.closeQuietly(pstmt);
		}
	}

	/**
	 * 批量删除回答
	 * 
	 * @param conn
	 * @param answerIds
	 * @throws SQLException
	 */
	public static void deleteAnswer(Connection conn, long companyId, List<Integer> answerIds) throws SQLException {
		if (answerIds.isEmpty()) {
			return;
		}
		Statement st = null;
		String answerIdStr = DBUtil.COMMA_JOINER.join(answerIds);
		try {
			StringBuilder sql = new StringBuilder();
			sql.append("DELETE FROM weizhu_qa_answer WHERE company_id = ")
					.append(companyId)
					.append(" AND answer_id IN(").append(answerIdStr).append(");");
			st = conn.createStatement();
			st.executeUpdate(sql.toString());
		} finally {
			DBUtil.closeQuietly(st);
		}
	}

	/**
	 * 批量删除问题
	 * 
	 * @param conn
	 * @param questionIds
	 * @throws SQLException
	 */
	public static void deleteQuestion(Connection conn, long companyId, List<Integer> questionIds, List<Integer> answerIds) throws SQLException {
		if (questionIds.isEmpty()) {
			return;
		}
		Statement st = null;
		String questionIdStr = DBUtil.COMMA_JOINER.join(questionIds);
		String answerIdStr = DBUtil.COMMA_JOINER.join(answerIds);
		try {
			StringBuilder sql = new StringBuilder();
			if (!answerIds.isEmpty()) {
				sql.append("DELETE FROM weizhu_qa_answer_like WHERE company_id = ")
					.append(companyId)
					.append(" AND answer_id IN(").append(answerIdStr).append(");");
			}
			sql.append("DELETE FROM weizhu_qa_answer WHERE  company_id = ")
					.append(companyId)
					.append(" AND question_id IN(")
					.append(questionIdStr)
					.append(");")
					.append("DELETE FROM weizhu_qa_question WHERE company_id = ")
					.append(companyId)
					.append(" AND question_id IN(")
					.append(questionIdStr)
					.append(");");
			st = conn.createStatement();
			st.executeUpdate(sql.toString());
		} finally {
			DBUtil.closeQuietly(st);
		}
	}

	/**
	 * 通过起始下标和长度的方式获取回答列表（管理后台使用）
	 * 
	 * @param conn
	 * @param start
	 * @param length
	 * @param questionId
	 * @return
	 * @throws SQLException
	 */
	public static List<Integer> getAnswerIdListByStart(Connection conn, long companyId, @Nullable Integer start, int length, int questionId) throws SQLException {
		if (start == null || start < 0) {
			start = 0;
		}
		if (length <= 0) {
			return Collections.emptyList();
		}

		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try {
			pstmt = conn.prepareStatement("SELECT a.answer_id FROM weizhu_qa_answer a LEFT JOIN  (SELECT answer_id,COUNT(user_id) AS like_num FROM weizhu_qa_answer_like WHERE company_id = ? GROUP BY answer_id ) l ON  a.answer_id=l.answer_id  WHERE company_id = ? AND a.question_id=? ORDER BY l.like_num DESC,a.answer_id DESC LIMIT ?,?; ");
			pstmt.setLong(1, companyId);
			pstmt.setLong(2, companyId);
			pstmt.setInt(3, questionId);
			pstmt.setInt(4, start);
			pstmt.setInt(5, length);

			rs = pstmt.executeQuery();
			List<Integer> answerIdList = new ArrayList<Integer>();
			while (rs.next()) {
				answerIdList.add(rs.getInt("answer_id"));
			}
			return answerIdList;
		} finally {
			DBUtil.closeQuietly(rs);
			DBUtil.closeQuietly(pstmt);
		}
	}

	/**
	 * 获取分类id列表
	 * 
	 * @param conn
	 * @return
	 * @throws SQLException
	 */
	public static List<Integer> getCategoryIdList(Connection conn, long companyId) throws SQLException {
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try {
			pstmt = conn.prepareStatement("SELECT category_id FROM weizhu_qa_question_category WHERE company_id = ?;");
			pstmt.setLong(1, companyId);
			rs = pstmt.executeQuery();
			List<Integer> categoryList = new ArrayList<Integer>();
			while (rs.next()) {
				categoryList.add(rs.getInt(1));
			}
			return categoryList;
		} finally {
			DBUtil.closeQuietly(rs);
			DBUtil.closeQuietly(pstmt);
		}
	}

	/**
	 * 获取分类详细信息
	 * 
	 * @param conn
	 * @param categoryIds
	 * @return
	 * @throws SQLException
	 */
	public static Map<Integer, QAProtos.Category> getCategory(Connection conn, long companyId, Collection<Integer> categoryIds) throws SQLException {
		if (categoryIds.isEmpty()) {
			return Collections.emptyMap();
		}

		String categoryIdStr = DBUtil.COMMA_JOINER.join(categoryIds);
		Statement st = null;
		ResultSet rs = null;
		try {
			StringBuilder sql = new StringBuilder();
			sql.append("SELECT category_id,category_name,user_id,admin_id,create_time FROM weizhu_qa_question_category WHERE company_id = ")
					.append(companyId)
					.append(" AND category_id IN(")
					.append(categoryIdStr)
					.append(");");
			st = conn.createStatement();
			rs = st.executeQuery(sql.toString());
			Map<Integer, QAProtos.Category> categoryInfoMap = new HashMap<Integer, QAProtos.Category>();
			QAProtos.Category.Builder categoryBuilder = QAProtos.Category.newBuilder();
			while (rs.next()) {
				categoryBuilder.clear();

				CATEGORY_MAPPER.mapToItem(rs, categoryBuilder);
				categoryInfoMap.put(rs.getInt("category_id"), categoryBuilder.build());
			}
			return categoryInfoMap;
		} finally {
			DBUtil.closeQuietly(rs);
			DBUtil.closeQuietly(st);
		}
	}

	/**
	 * 创建分类
	 * 
	 * @param conn
	 * @param companyId
	 * @param categoryName
	 * @param userId
	 * @param adminId
	 * @param createTime
	 * @return
	 * @throws SQLException
	 * int
	 * @throws
	 */
	public static int insertCategory(Connection conn, long companyId, String categoryName, @Nullable Long userId, @Nullable Long adminId, int createTime)
			throws SQLException {
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try {
			pstmt = conn.prepareStatement("INSERT INTO weizhu_qa_question_category(company_id,category_name,user_id,admin_id,create_time) VALUES (?,?,?,?,?);",
					Statement.RETURN_GENERATED_KEYS);
			pstmt.setLong(1, companyId);
			pstmt.setString(2, categoryName);
			if (userId == null) {
				pstmt.setNull(3, java.sql.Types.BIGINT);
			} else {
				pstmt.setLong(3, userId);
			}
			if (adminId == null) {
				pstmt.setNull(4, java.sql.Types.BIGINT);
			} else {
				pstmt.setLong(4, adminId);
			}
			pstmt.setInt(5, createTime);
			pstmt.executeUpdate();
			rs = pstmt.getGeneratedKeys();
			if (rs.next()) {
				return rs.getInt(1);
			} else {
				throw new RuntimeException("新增分类失败！");
			}
		} finally {
			DBUtil.closeQuietly(rs);
			DBUtil.closeQuietly(pstmt);
		}
	}

	/**
	 * 删除分类
	 * 
	 * @param conn
	 * @param companyId
	 * @param categoryId
	 * @throws SQLException
	 * void
	 * @throws
	 */
	public static void deleteCategory(Connection conn, long companyId, int categoryId) throws SQLException {
		PreparedStatement pstmt = null;
		try {
			pstmt = conn.prepareStatement("DELETE FROM  weizhu_qa_question_category WHERE company_id=? AND category_id=? LIMIT 1;");
			pstmt.setLong(1, companyId);
			pstmt.setInt(2, categoryId);
			pstmt.executeUpdate();
		} finally {
			DBUtil.closeQuietly(pstmt);
		}
	}

	/**
	 * 更改分类
	 * 
	 * @param conn
	 * @param companyId
	 * @param categoryName
	 * @param categoryId
	 * @throws SQLException
	 * void
	 * @throws
	 */
	public static void updateCategory(Connection conn, long companyId, String categoryName, int categoryId) throws SQLException {
		PreparedStatement pstmt = null;
		try {
			pstmt = conn.prepareStatement("UPDATE  weizhu_qa_question_category SET company_id=? AND category_name=? WHERE category_id=? LIMIT 1;");
			pstmt.setLong(1, companyId);
			pstmt.setString(2, categoryName);
			pstmt.setInt(3, categoryId);
			pstmt.executeUpdate();
		} finally {
			DBUtil.closeQuietly(pstmt);
		}

	}

	/**
	 * 判断分类下是否有问题
	 * 
	 * @param conn
	 * @param categoryId
	 * @return
	 * @throws SQLException
	 */
	
	public static boolean hasQueInCategory(Connection conn, long companyId, int categoryId) throws SQLException {
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try {
			pstmt = conn.prepareStatement("SELECT question_id FROM  weizhu_qa_question  WHERE company_id=? AND category_id=? LIMIT 1;");
			pstmt.setLong(1, companyId);
			pstmt.setInt(2, categoryId);
			rs = pstmt.executeQuery();
			if (rs.next()) {
				return true;
			}
		} finally {
			DBUtil.closeQuietly(rs);
			DBUtil.closeQuietly(pstmt);
		}
		return false;
	}

	/**
	 * 获取问题的最佳回答，最佳回答为点赞数最多的回答
	 * 
	 * @param conn
	 * @param questionIds
	 * @return
	 * @throws SQLException
	 */

	public static Map<Integer, Integer> getBestAnswerId(Connection conn, long companyId, Collection<Integer> questionIds) throws SQLException {
		if (questionIds.isEmpty()) {
			return Collections.emptyMap();
		}
		Statement st = null;
		ResultSet rs = null;
		try {

			StringBuilder sql = new StringBuilder();
			for (int questionId : questionIds) {
				sql.append("SELECT a1.question_id,a1.answer_id FROM weizhu_qa_answer a1 LEFT JOIN ")
						.append("(SELECT answer_id, COUNT(user_id) AS like_num FROM weizhu_qa_answer_like WHERE company_id = ")
						.append(companyId)
						.append(" AND answer_id IN (SELECT answer_id FROM weizhu_qa_answer WHERE company_id = ")
						.append(companyId)
						.append(" AND question_id = ")
						.append(questionId)
						.append(") GROUP BY answer_id) l ")
						.append("ON a1.answer_id = l.answer_id WHERE company_id = ")
						.append(companyId)
						.append(" AND a1.question_id = ")
						.append(questionId)
						.append(" ORDER BY l.like_num DESC,a1.answer_id DESC LIMIT 1 ;");
			}

			st = conn.createStatement();
			rs = st.executeQuery(sql.toString());
			Map<Integer, Integer> bestAnswerIdMap = new HashMap<Integer, Integer>();

			for (int i = 0; i < questionIds.size(); i++) {
				if (i == 0) {
					if (rs.next()) {

						bestAnswerIdMap.put(rs.getInt("question_id"), rs.getInt("answer_id"));
					}
				} else {
					DBUtil.closeQuietly(rs);
					rs = null;
					st.getMoreResults();
					rs = st.getResultSet();
					if (rs.next()) {
						bestAnswerIdMap.put(rs.getInt("question_id"), rs.getInt("answer_id"));
					}
				}
			}

			return bestAnswerIdMap;
		} finally {
			DBUtil.closeQuietly(rs);
			DBUtil.closeQuietly(st);
		}
	}

	/**
	 * 通过起始下标和长度的分页方式获取问题列表，（管理后台使用）
	 * 
	 * @param conn
	 * @param start
	 * @param length
	 * @param categoryId
	 * @param keyword
	 * @return
	 * @throws SQLException
	 */
	public static List<Integer> getQuestionIdListByStart(Connection conn, long companyId, @Nullable Integer start, int length, @Nullable Integer categoryId,
			String keyword) throws SQLException {
		if (start == null || start < 0) {
			start = 0;
		}
		if (length <= 0) {
			return Collections.emptyList();
		}
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		keyword = "%" + keyword.replace("%", "\\%") + "%";
		try {
			if (categoryId == null) {
				pstmt = conn.prepareStatement("SELECT question_id FROM weizhu_qa_question WHERE company_id=? AND question_content LIKE ? ORDER BY question_id DESC LIMIT ?,?; ");
				pstmt.setLong(1, companyId);
				pstmt.setString(2, keyword);
				pstmt.setInt(3, start);
				pstmt.setInt(4, length);
			} else {
				pstmt = conn.prepareStatement("SELECT question_id FROM weizhu_qa_question WHERE company_id=? AND category_id=? AND question_content LIKE ? ORDER BY question_id DESC LIMIT ?,?; ");
				pstmt.setLong(1, companyId);
				pstmt.setInt(2, categoryId);
				pstmt.setString(3, keyword);
				pstmt.setInt(4, start);
				pstmt.setInt(5, length);
			}

			rs = pstmt.executeQuery();
			List<Integer> questionIdList = new ArrayList<Integer>();
			while (rs.next()) {
				questionIdList.add(rs.getInt("question_id"));
			}
			return questionIdList;
		} finally {
			DBUtil.closeQuietly(rs);
			DBUtil.closeQuietly(pstmt);
		}
	}

	/**
	 * 通过上一次访问的问题id和获取数量来分页获取问题列表，（手机端使用）
	 * 
	 * @param conn
	 * @param lastQuestionId
	 * @param size
	 * @param categoryId
	 * @param keyword
	 * @return
	 * @throws SQLException
	 */
	public static List<Integer> getQuestionIdListByLastId(Connection conn, long companyId, @Nullable Integer lastQuestionId, int size, @Nullable Integer categoryId,
			@Nullable String keyword) throws SQLException {
		if (size <= 0) {
			return Collections.emptyList();
		}

		PreparedStatement pstmt = null;
		ResultSet rs = null;
		keyword = keyword == null ? "%%" : ("%" + DBUtil.SQL_LIKE_STRING_ESCAPER.escape(keyword) + "%");
		try {
			if (lastQuestionId == null) {
				if (categoryId == null) {
					pstmt = conn.prepareStatement("SELECT question_id FROM weizhu_qa_question WHERE company_id=? AND question_content LIKE ? ORDER BY question_id DESC LIMIT ?; ");
					pstmt.setLong(1, companyId);
					pstmt.setString(2, keyword);
					pstmt.setInt(3, size);
				} else {
					pstmt = conn.prepareStatement("SELECT question_id FROM weizhu_qa_question WHERE company_id=? AND question_content LIKE ? AND category_id=? ORDER BY question_id DESC LIMIT ?; ");
					pstmt.setLong(1, companyId);
					pstmt.setString(2, keyword);
					pstmt.setInt(3, categoryId);
					pstmt.setInt(4, size);
				}

			} else {
				if (categoryId == null) {
					pstmt = conn.prepareStatement("SELECT question_id FROM weizhu_qa_question WHERE company_id=? AND question_content LIKE ? AND question_id<? ORDER BY question_id DESC LIMIT ?; ");
					pstmt.setLong(1, companyId);
					pstmt.setString(2, keyword);
					pstmt.setInt(3, lastQuestionId);
					pstmt.setInt(4, size);
				} else {
					pstmt = conn.prepareStatement("SELECT question_id FROM weizhu_qa_question WHERE company_id=? AND question_content LIKE ? AND category_id=? and question_id<? ORDER BY question_id DESC LIMIT ?; ");
					pstmt.setLong(1, companyId);
					pstmt.setString(2, keyword);
					pstmt.setInt(3, categoryId);
					pstmt.setInt(4, lastQuestionId);
					pstmt.setInt(5, size);
				}
			}

			rs = pstmt.executeQuery();
			List<Integer> questionIdList = new ArrayList<Integer>();
			while (rs.next()) {
				questionIdList.add(rs.getInt("question_id"));
			}
			return questionIdList;
		} finally {
			DBUtil.closeQuietly(rs);
			DBUtil.closeQuietly(pstmt);
		}
	}

	/**
	 * 根据关键字获取问题列表，每个分类最多三个问题（手机端使用）
	 * 
	 * @param conn
	 * @param keyword
	 * @return
	 * @throws SQLException
	 */
	public static Map<Integer, List<Integer>> getQuestionIdList(Connection conn, long companyId, String keyword) throws SQLException {
		List<Integer> categoryIds=getCategoryIdList(conn, companyId);
		if(categoryIds.isEmpty()){
			return Collections.emptyMap();
		}
		keyword = "%" + keyword.replace("%", "\\%") + "%";
		StringBuffer sql=new StringBuffer();
		//搜索时分类个数的上线为100
		for(int i=0;i<categoryIds.size() && i<100;i++){
			sql.append("SELECT question_id,category_id FROM weizhu_qa_question WHERE company_id = ").append(companyId).append(" AND category_id=").append(categoryIds.get(i)).append(" AND question_content LIKE '").append(keyword).append("' ORDER BY question_id DESC LIMIT 3;");
		}
		Statement st = null;
		ResultSet rs = null;

		try {
			st = conn.createStatement();

			rs = st.executeQuery(sql.toString());
			Map<Integer, List<Integer>> categoryQueMap = new HashMap<Integer, List<Integer>>();
			int categoryId;
			int questionId;
			for (int i = 0; i < categoryIds.size() && i < 100; i++) {
				if (i == 0) {
					while (rs.next()) {
						questionId = rs.getInt("question_id");
						categoryId = rs.getInt("category_id");
						List<Integer> questions = null;
						if (!categoryQueMap.isEmpty() && categoryQueMap.containsKey(categoryId)) {
							questions = categoryQueMap.get(categoryId);
						} else {
							questions = new ArrayList<Integer>();
						}
						questions.add(questionId);
						categoryQueMap.put(categoryId, questions);
					}
				} else {
					DBUtil.closeQuietly(rs);
					rs = null;
					st.getMoreResults();
					rs = st.getResultSet();
					while (rs.next()) {
						questionId = rs.getInt("question_id");
						categoryId = rs.getInt("category_id");
						List<Integer> questions = null;
						if (!categoryQueMap.isEmpty() && categoryQueMap.containsKey(categoryId)) {
							questions = categoryQueMap.get(categoryId);
						} else {
							questions = new ArrayList<Integer>();
						}
						questions.add(questionId);
						categoryQueMap.put(categoryId, questions);
					}
				}
			}

			return categoryQueMap;
		} finally {
			DBUtil.closeQuietly(rs);
			DBUtil.closeQuietly(st);
		}
	}
	/**
	 * 改变问题的分类
	 * 
	 * @param conn
	 * @param questionIds
	 * @param categoryId
	 * @throws SQLException
	 */
	public static void updateQuestionCategory(Connection conn, long companyId, Collection<Integer> questionIds, int categoryId) throws SQLException {
		if (questionIds.isEmpty()) {
			return;
		}
		String questionStr = DBUtil.COMMA_JOINER.join(questionIds);
		StringBuilder sql = new StringBuilder();
		sql.append("UPDATE weizhu_qa_question SET category_id=").append(categoryId).append(" WHERE company_id = ").append(companyId).append(" AND question_id IN(").append(questionStr).append(");");
		Statement st = null;
		try {
			st = conn.createStatement();
			st.executeUpdate(sql.toString());
		} finally {
			DBUtil.closeQuietly(st);
		}
	}

	/**
	 * 创建回答
	 * 
	 * @param conn
	 * @param companyId
	 * @param queIdAnsContMap
	 * @param userId
	 * @param adminId
	 * @param createTime
	 * @return
	 * @throws SQLException
	 * List<Integer>
	 * @throws
	 */
	public static List<Integer> insertAnswer(Connection conn, long companyId, Map<Integer, String> queIdAnsContMap, @Nullable Long userId, @Nullable Long adminId,
			int createTime) throws SQLException {
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try {
			pstmt = conn.prepareStatement("INSERT INTO weizhu_qa_answer(company_id,question_id,user_id,admin_id,answer_content,create_time) VALUES (?,?,?,?,?,?);",
					Statement.RETURN_GENERATED_KEYS);

			List<Integer> answerIds = new ArrayList<Integer>();
			for (Entry<Integer, String> entry : queIdAnsContMap.entrySet()) {
				pstmt.setLong(1, companyId);
				pstmt.setInt(2, entry.getKey());
				if (null == userId) {
					pstmt.setNull(3, java.sql.Types.BIGINT);
				} else {
					pstmt.setLong(3, userId);
				}
				if (null == adminId) {
					pstmt.setNull(4, java.sql.Types.BIGINT);
				} else {
					pstmt.setLong(4, adminId);
				}
				pstmt.setString(5, entry.getValue());
				pstmt.setInt(6, createTime);
				pstmt.addBatch();
			}
			pstmt.executeBatch();
			rs = pstmt.getGeneratedKeys();
			while (rs.next()) {
				answerIds.add(rs.getInt(1));
			}
			return answerIds;
		} finally {
			DBUtil.closeQuietly(rs);
			DBUtil.closeQuietly(pstmt);
		}
	}

	/**
	 * 获取分类的数量
	 * 
	 * @param conn
	 * @param companyId
	 * @return
	 * @throws SQLException
	 * int
	 * @throws
	 */
	public static int getCategoryNum(Connection conn, long companyId) throws SQLException {
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try {
			pstmt = conn.prepareStatement("SELECT COUNT(category_id) FROM weizhu_qa_question_category WHERE company_id = ?;");
			pstmt.setLong(1, companyId);
			rs = pstmt.executeQuery();
			if (rs.next()) {
				return rs.getInt(1);
			} else {
				return 0;
			}
		} finally {
			DBUtil.closeQuietly(rs);
			DBUtil.closeQuietly(pstmt);
		}
	}

	/**
	 * 获取分类下的问题数量
	 * 
	 * @param conn
	 * @param categoryIds
	 * @return
	 * @throws SQLException
	 */
	public static Map<Integer, Integer> getQuestionNum(Connection conn, long companyId, Collection<Integer> categoryIds) throws SQLException {
		if (categoryIds.isEmpty()) {
			return Collections.emptyMap();
		}
		String categoryIdStr = DBUtil.COMMA_JOINER.join(categoryIds);

		Statement st = null;
		ResultSet rs = null;

		StringBuilder sql = new StringBuilder();
		sql.append("SELECT category_id,COUNT(question_id) as question_num FROM weizhu_qa_question WHERE company_id =")
				.append(companyId)
				.append(" AND category_id IN(")
				.append(categoryIdStr)
				.append(") GROUP BY category_id;");
		try {
			st = conn.createStatement();
			rs = st.executeQuery(sql.toString());
			Map<Integer, Integer> map = new HashMap<Integer, Integer>();
			while (rs.next()) {
				map.put(rs.getInt(1), rs.getInt(2));
			}
			return map;
		} finally {
			DBUtil.closeQuietly(rs);
			DBUtil.closeQuietly(st);
		}
	}

	/**
	 * 获取问题下的回答数量
	 * 
	 * @param conn
	 * @param questionIds
	 * @return
	 * @throws SQLException
	 */
	public static Map<Integer, Integer> getAnswerNum(Connection conn, long companyId, Collection<Integer> questionIds) throws SQLException {
		if (questionIds.isEmpty()) {
			return Collections.emptyMap();
		}
		String questionIdStr = DBUtil.COMMA_JOINER.join(questionIds);

		Statement st = null;
		ResultSet rs = null;

		StringBuilder sql = new StringBuilder();
		sql.append("SELECT question_id,COUNT(answer_id) as answer_num FROM weizhu_qa_answer WHERE company_id =")
				.append(companyId)
				.append(" AND question_id IN(")
				.append(questionIdStr)
				.append(")  GROUP BY question_id;");
		try {
			st = conn.createStatement();
			rs = st.executeQuery(sql.toString());
			Map<Integer, Integer> map = new HashMap<Integer, Integer>();
			while (rs.next()) {
				map.put(rs.getInt(1), rs.getInt(2));
			}
			return map;
		} finally {
			DBUtil.closeQuietly(rs);
			DBUtil.closeQuietly(st);
		}
	}

	/**
	 * 获取回答的点赞数
	 * 
	 * @param conn
	 * @param answerIds
	 * @return
	 * @throws SQLException
	 */
	public static Map<Integer, Integer> getLikeNum(Connection conn, long companyId, Collection<Integer> answerIds) throws SQLException {
		if (answerIds.isEmpty()) {
			return Collections.emptyMap();
		}
		String answerIdStr = DBUtil.COMMA_JOINER.join(answerIds);

		Statement st = null;
		ResultSet rs = null;

		StringBuilder sql = new StringBuilder();

		sql.append("SELECT answer_id,COUNT(user_id) as like_num FROM weizhu_qa_answer_like WHERE company_id =")
				.append(companyId)
				.append(" AND answer_id IN(")
				.append(answerIdStr)
				.append(") GROUP BY answer_id;");
		try {
			st = conn.createStatement();
			rs = st.executeQuery(sql.toString());
			Map<Integer, Integer> map = new HashMap<Integer, Integer>();
			while (rs.next()) {
				map.put(rs.getInt(1), rs.getInt(2));
			}
			return map;
		} finally {
			DBUtil.closeQuietly(rs);
			DBUtil.closeQuietly(st);
		}
	}
}
