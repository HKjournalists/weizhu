package com.weizhu.service.exam;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import javax.annotation.Nullable;

import com.google.common.base.Splitter;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.weizhu.common.db.DBUtil;
import com.weizhu.common.db.ProtobufMapper;
import com.weizhu.common.utils.DataPage;
import com.weizhu.proto.AdminExamProtos.GetExamStatisticsResponse.ExamStatistics;
import com.weizhu.proto.AdminExamProtos.GetPositionStatisticsResponse.PositionStatistics;
import com.weizhu.proto.AdminExamProtos.GetTeamStatisticsResponse.TeamStatistics;
import com.weizhu.proto.AdminExamProtos.QuestionCategory;
import com.weizhu.proto.AdminExamProtos.StatisticalParams;
import com.weizhu.proto.ExamProtos;
import com.weizhu.proto.ExamProtos.Option;
import com.weizhu.proto.ExamProtos.UserResult;
import com.weizhu.proto.UserProtos;
import com.weizhu.proto.UserProtos.User;

public class ExamDB {
	
	private static final Splitter SPLITTER = Splitter.on(',').trimResults().omitEmptyStrings();

	private static final ProtobufMapper<ExamProtos.Exam> EXAM_MAPPER = 
			ProtobufMapper.createMapper(ExamProtos.Exam.getDefaultInstance(),
					"exam_id",
					"exam_name",
					"image_name",
					"start_time",
					"end_time",
					"is_submit_execute",
					"create_exam_admin_id",
					"create_exam_time",
					"pass_mark",
					"type",
					"allow_model_id",
					"show_result",
					"is_load_all_user");
	
	public static Map<Integer, ExamDAOProtos.ExamInfo> getExamInfo(Connection conn, long companyId, Collection<Integer> examIds) throws SQLException {
		if (examIds.isEmpty()) {
			return Collections.emptyMap();
		}
		
		String examIdStr = DBUtil.COMMA_JOINER.join(examIds);
		
		StringBuilder sqlBuilder = new StringBuilder();
		sqlBuilder.append("SELECT * FROM weizhu_exam_exam_question WHERE company_id = ").append(companyId);
		sqlBuilder.append(" AND exam_id IN (").append(examIdStr);
		sqlBuilder.append(") ORDER BY exam_id ASC, question_id ASC; ");
		sqlBuilder.append("SELECT * FROM weizhu_exam_exam_join_category WHERE company_id = ").append(companyId);
		sqlBuilder.append(" AND exam_id IN (").append(examIdStr);
		sqlBuilder.append(") ORDER BY exam_id ASC; ");
		sqlBuilder.append("SELECT * FROM weizhu_exam_exam WHERE company_id = ").append(companyId);
		sqlBuilder.append(" AND exam_id IN (").append(examIdStr);
		sqlBuilder.append(") ORDER BY exam_id ASC; ");
		
		final String sql = sqlBuilder.toString();
		Statement stmt = null;
		ResultSet rs = null;
		try {
			stmt = conn.createStatement();
			stmt.execute(sql);
			rs = stmt.getResultSet();
			
			Map<Integer, LinkedList<ExamDAOProtos.ExamQuestion>> examQuestionMap = new TreeMap<Integer, LinkedList<ExamDAOProtos.ExamQuestion>>();
			
			ExamDAOProtos.ExamQuestion.Builder tmpBuilder = ExamDAOProtos.ExamQuestion.newBuilder();
			while (rs.next()) {
				tmpBuilder.clear();
				
				tmpBuilder.setExamId(rs.getInt("exam_id"));
				tmpBuilder.setQuestionId(rs.getInt("question_id"));
				tmpBuilder.setScore(rs.getInt("score"));
				ExamDAOProtos.ExamQuestion examQuestion = tmpBuilder.build();
				
				LinkedList<ExamDAOProtos.ExamQuestion> list = examQuestionMap.get(examQuestion.getExamId());
				if (list == null) {
					list = new LinkedList<ExamDAOProtos.ExamQuestion>();
					examQuestionMap.put(examQuestion.getExamId(), list);
				}
				list.add(examQuestion);
			}
			
			DBUtil.closeQuietly(rs);
			rs = null;
			stmt.getMoreResults();
			rs = stmt.getResultSet();
			
			Map<Integer, List<Integer>> examQuestionCategoryIdMap = new TreeMap<Integer, List<Integer>>();
			Map<Integer, Integer> examQuestionNumMap = new TreeMap<Integer, Integer>();
			while (rs.next()) {
				int examId = rs.getInt("exam_id");
				String questionCategoryStr = rs.getString("question_category_str");
				int questionNum = rs.getInt("question_num");
				
				List<Integer> categoryIdList;
				if (questionCategoryStr == null || questionCategoryStr.isEmpty()) {
					categoryIdList = Collections.emptyList();
				} else {
					categoryIdList = new ArrayList<Integer>();
					for (String str : SPLITTER.split(questionCategoryStr)) {
						try {
							categoryIdList.add(Integer.parseInt(str));
						} catch (NumberFormatException e) {
						}
					}
				}
				
				examQuestionCategoryIdMap.put(examId, categoryIdList);
				examQuestionNumMap.put(examId, questionNum);
			}
			
			DBUtil.closeQuietly(rs);
			rs = null;
			stmt.getMoreResults();
			rs = stmt.getResultSet();
			
			Map<Integer, ExamDAOProtos.ExamInfo> examInfoMap = new TreeMap<Integer, ExamDAOProtos.ExamInfo>();
			ExamDAOProtos.ExamInfo.Builder tmpExamInfoBuilder = ExamDAOProtos.ExamInfo.newBuilder();
			ExamProtos.Exam.Builder tmpExamBuilder = ExamProtos.Exam.newBuilder();
			while (rs.next()) {
				tmpExamInfoBuilder.clear();
				tmpExamBuilder.clear();
				
				ExamProtos.Exam exam = EXAM_MAPPER.mapToItem(rs, tmpExamBuilder).build();
				tmpExamInfoBuilder.setExam(exam);
				
				LinkedList<ExamDAOProtos.ExamQuestion> list = examQuestionMap.get(exam.getExamId());
				if (list != null && !list.isEmpty()) {
					String questionOrderStr = rs.getString("question_order_str");
					if (questionOrderStr == null || questionOrderStr.isEmpty()) {
						tmpExamInfoBuilder.addAllExamQuestion(list);
					} else {
						List<String> questionIdStrList = SPLITTER.splitToList(questionOrderStr);
						for (String questionIdStr : questionIdStrList) {
							try {
								int questionId = Integer.parseInt(questionIdStr);
								
								Iterator<ExamDAOProtos.ExamQuestion> it = list.iterator();
								while (it.hasNext()) {
									ExamDAOProtos.ExamQuestion examQuestion = it.next();
									if (examQuestion.getQuestionId() == questionId) {
										tmpExamInfoBuilder.addExamQuestion(examQuestion);
										it.remove();
										break;
									}
								}
							} catch (NumberFormatException e) {
								// ignore
							}
						}
						tmpExamInfoBuilder.addAllExamQuestion(list);
					}
				}
				
				List<Integer> questionCategoryIdList = examQuestionCategoryIdMap.get(exam.getExamId());
				if (questionCategoryIdList != null) {
					tmpExamInfoBuilder.addAllRandomQuestionCategoryId(questionCategoryIdList);
				}
				Integer questionNum = examQuestionNumMap.get(exam.getExamId());
				if (questionNum != null) {
					tmpExamInfoBuilder.setRandomQuestionNum(questionNum);
				}
				
				examInfoMap.put(exam.getExamId(), tmpExamInfoBuilder.build());
			}
			return examInfoMap;
		} finally {
			DBUtil.closeQuietly(rs);
			DBUtil.closeQuietly(stmt);
		}
	}
	
	private static final ProtobufMapper<ExamProtos.Question> QUESTION_MAPPER = 
			ProtobufMapper.createMapper(ExamProtos.Question.getDefaultInstance(),
					"question_id",
					"question_name",
					"type",
					"create_question_time",
					"create_question_admin_id");
	
	private static final ProtobufMapper<ExamProtos.Option> OPTION_MAPPER = 
			ProtobufMapper.createMapper(Option.getDefaultInstance(),
					"option_id",
					"option_name",
					"is_right");
	
	public static Map<Integer, ExamProtos.Question> getQuestion(Connection conn, long companyId, Collection<Integer> questionIds) throws SQLException {
    	if (questionIds.isEmpty()){
    		return Collections.emptyMap();
    	}
    	
    	String questionIdStr = DBUtil.COMMA_JOINER.join(questionIds);
    	
    	StringBuilder sqlBuilder = new StringBuilder();
    	sqlBuilder.append("SELECT * FROM weizhu_exam_option WHERE company_id = ").append(companyId);
		sqlBuilder.append(" AND question_id IN (").append(questionIdStr).append(") ORDER BY question_id ASC, option_id ASC; ");
		sqlBuilder.append("SELECT * FROM weizhu_exam_question WHERE company_id = ").append(companyId);
		sqlBuilder.append(" AND question_id IN (").append(questionIdStr).append(") ORDER BY question_id ASC; ");
		
		final String sql = sqlBuilder.toString();
    	Statement stmt = null;
    	ResultSet rs = null;
		try {
			stmt = conn.createStatement();
			stmt.execute(sql);
			rs = stmt.getResultSet();
			
			Map<Integer, List<ExamProtos.Option>> optionMap = new TreeMap<Integer, List<ExamProtos.Option>>();
			
			ExamProtos.Option.Builder tmpOptionBuilder = ExamProtos.Option.newBuilder();
			while(rs.next()){
				tmpOptionBuilder.clear();
				
				ExamProtos.Option option = OPTION_MAPPER.mapToItem(rs, tmpOptionBuilder).build();
				DBUtil.addMapArrayList(optionMap, rs.getInt("question_id"), option);
			}
			
			DBUtil.closeQuietly(rs);
			rs = null;
			stmt.getMoreResults();
			rs = stmt.getResultSet();
			
			Map<Integer, ExamProtos.Question> questionMap = new LinkedHashMap<Integer, ExamProtos.Question>();
			
			ExamProtos.Question.Builder tmpQuestionBuilder = ExamProtos.Question.newBuilder();
			while (rs.next()) {
				tmpQuestionBuilder.clear();
				
				QUESTION_MAPPER.mapToItem(rs, tmpQuestionBuilder);
				
				List<ExamProtos.Option> optionList = optionMap.get(tmpQuestionBuilder.getQuestionId());
				if (optionList != null) {
					tmpQuestionBuilder.addAllOption(optionList);
				}
				questionMap.put(tmpQuestionBuilder.getQuestionId(), tmpQuestionBuilder.build());
			}
			
			return questionMap;
    	} finally {
    		DBUtil.closeQuietly(rs);
    		DBUtil.closeQuietly(stmt);
    	}
    }
	
	/**
	 * 随机出题，获取指定考试，指定用户的题目
	 * @param conn
	 * @param companyId
	 * @param examId
	 * @param userId
	 * @return
	 * @throws SQLException
	 */
	public static LinkedHashMap<Integer, Integer> getExamQuestionScoreRandom(Connection conn, long companyId, 
			int examId, long userId, 
			List<Integer> randomQuestionCategoryIdList, int randomQuestionNum
			) throws SQLException {
		
		final LinkedHashMap<Integer, Integer> questionScoreMap = new LinkedHashMap<Integer, Integer>();
		
		StringBuilder sqlBuilder = new StringBuilder();
		sqlBuilder.append("SELECT question_id, score FROM weizhu_exam_user_question WHERE company_id = ").append(companyId).append(" AND ");
		sqlBuilder.append("user_id = ").append(userId).append(" AND ");
		sqlBuilder.append("exam_id = ").append(examId).append("; ");
		
		final String sql = sqlBuilder.toString();
		Statement stmt = null;
		ResultSet rs = null;
		try {
			stmt = conn.createStatement();
			rs = stmt.executeQuery(sql.toString());
			
			while (rs.next()) {
				int questionId = rs.getInt("question_id");
				int score = rs.getInt("score");
				questionScoreMap.put(questionId, score);
			}
		} finally {
			DBUtil.closeQuietly(rs);
			DBUtil.closeQuietly(stmt);
		}
			
		if (!questionScoreMap.isEmpty()) {
			return questionScoreMap;
		}
		
		if (randomQuestionCategoryIdList.isEmpty() || randomQuestionNum <= 0) {
			return Maps.newLinkedHashMap();
		}
		randomQuestionNum = randomQuestionNum > 100 ? 100 : randomQuestionNum;
		
		StringBuilder sqlBuilder2 = new StringBuilder();
		sqlBuilder2.append("SELECT a.question_id FROM (SELECT DISTINCT question_id FROM weizhu_exam_question_category_join_question WHERE company_id = ").append(companyId);
		sqlBuilder2.append(" AND category_id IN (");
		DBUtil.COMMA_JOINER.appendTo(sqlBuilder2, randomQuestionCategoryIdList);
		sqlBuilder2.append(") ORDER BY RAND() LIMIT ").append(randomQuestionNum);
		sqlBuilder2.append(") a ORDER BY question_id ASC; ");
		
		final String sql2 = sqlBuilder2.toString();
		stmt = null;
		rs = null;
		try {
			stmt = conn.createStatement();
			rs = stmt.executeQuery(sql2);
			
			Integer questionId = null;
			while (rs.next()) {
				questionId = rs.getInt("question_id");
				questionScoreMap.put(questionId, 100/randomQuestionNum);
			}
			if (questionId != null) {
				questionScoreMap.put(questionId, questionScoreMap.get(questionId) + 100%randomQuestionNum);
			}
		} finally {
			DBUtil.closeQuietly(rs);
			DBUtil.closeQuietly(stmt);
		}
		
		StringBuilder sqlBuilder3 = null;
		for (Entry<Integer, Integer> entry : questionScoreMap.entrySet()) {
			if (sqlBuilder3 == null) {
				sqlBuilder3 = new StringBuilder("INSERT INTO weizhu_exam_user_question (company_id, exam_id, user_id, question_id, score) VALUES ");
			} else {
				sqlBuilder3.append(", ");
			}
			sqlBuilder3.append("(");
			sqlBuilder3.append(companyId).append(", ");
			sqlBuilder3.append(examId).append(", ");
			sqlBuilder3.append(userId).append(", ");
			sqlBuilder3.append(entry.getKey()).append(", ");
			sqlBuilder3.append(entry.getValue()).append(") ");
		}
		if (sqlBuilder3 != null) {
			sqlBuilder3.append("; ");
			final String sql3 = sqlBuilder3.toString();
			stmt = null;
			try {
				stmt = conn.createStatement();
				stmt.executeUpdate(sql3);
			} finally {
				DBUtil.closeQuietly(stmt);
			}	
		}
		return questionScoreMap;
	}
	
	private static final ProtobufMapper<ExamProtos.UserResult> USER_RESULT_MAPPER = 
			ProtobufMapper.createMapper(ExamProtos.UserResult.getDefaultInstance(),
					"user_id",
					"exam_id",
					"start_time",
					"submit_time",
					"score");
	
	public static Map<Integer, ExamProtos.UserResult> getUserResult(Connection conn, long companyId, long userId, Collection<Integer> examIds) throws SQLException {
		if (examIds.isEmpty()) {
			return Collections.emptyMap();
		}
		
		StringBuilder sql = new StringBuilder();
		sql.append("SELECT user_id, exam_id, start_time, submit_time, score FROM weizhu_exam_user_result WHERE company_id = ").append(companyId).append(" AND user_id = ").append(userId).append(" AND exam_id IN (");
		DBUtil.COMMA_JOINER.appendTo(sql, examIds);
		sql.append(") ORDER BY user_id, exam_id; ");
		
		Statement stmt = null;
		PreparedStatement pstmt = null;
    	ResultSet rs = null;
		try {
			stmt = conn.createStatement();
			rs = stmt.executeQuery(sql.toString());
			
			Map<Integer, ExamProtos.UserResult> examUserResultMap = new LinkedHashMap<Integer, ExamProtos.UserResult>(examIds.size());
			
			ExamProtos.UserResult.Builder tmpUserResultBuilder = ExamProtos.UserResult.newBuilder();
			while (rs.next()) {
				tmpUserResultBuilder.clear();
				
				USER_RESULT_MAPPER.mapToItem(rs, tmpUserResultBuilder);

				examUserResultMap.put(rs.getInt("exam_id"), tmpUserResultBuilder.build());
			}
			return examUserResultMap;
			/*
			DBUtil.closeQuietly(rs);
			rs = null;
			
			Map<Integer, Integer> rankMap = new HashMap<Integer, Integer>();
			if (!examUserResultMap.isEmpty()) {
				pstmt = conn.prepareStatement("SELECT exam_id, count(*) AS ranking FROM weizhu_exam_user_result WHERE company_id = ? AND exam_id = ? AND score > ?; ");
				for (Entry<Integer, ExamProtos.UserResult> entry : examUserResultMap.entrySet()) {
					ExamProtos.UserResult userResult = entry.getValue();
					
					pstmt.setLong(1, companyId);
					pstmt.setInt(2, userResult.getExamId());
					pstmt.setInt(3, userResult.getScore());
					
					pstmt.addBatch();
				}
				pstmt.executeQuery();
				
				rs = pstmt.getResultSet();
				while (rs.next()) {
					rankMap.put(rs.getInt("exam_id"), rs.getInt("ranking"));
				}
			}
			
			Map<Integer, UserResult> resultMap = new HashMap<Integer, UserResult>();
			UserResult.Builder userResultBuilder = UserResult.newBuilder();
			for (Entry<Integer, UserResult> entry : examUserResultMap.entrySet()) {
				userResultBuilder.clear();
				
				int eId = entry.getKey();
				int ranking = rankMap.get(entry.getKey()) == null ? 0 : rankMap.get(entry.getKey());
				
				resultMap.put(eId, userResultBuilder.mergeFrom(entry.getValue()).setRanking(ranking + 1).build());
			}
			
			return resultMap;
			*/
		} finally {
    		DBUtil.closeQuietly(rs);
    		DBUtil.closeQuietly(stmt);
    		DBUtil.closeQuietly(pstmt);
    	}
	}
	
	/**
	 * 获取没有结束的考试信息
	 * @param conn
	 * @param lastExamId
	 * @param lastExamEndTime
	 * @return
	 * @throws SQLException
	 */
	public static List<Integer> getOpenExamIdList(Connection conn, long companyId, int now, @Nullable Integer lastExamId, @Nullable Integer lastExamEndTime) throws SQLException {
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try {
			if (lastExamId == null || lastExamEndTime == null) {
				pstmt = conn.prepareStatement("SELECT exam_id FROM weizhu_exam_exam WHERE company_id = ? AND end_time > ? ORDER BY end_time ASC, exam_id ASC; ");
				pstmt.setLong(1, companyId);
				pstmt.setInt(2, now);
			} else {
				pstmt = conn.prepareStatement("SELECT exam_id FROM weizhu_exam_exam WHERE company_id = ? AND end_time > ? AND ((end_time = ? AND exam_id > ?) OR end_time > ?) ORDER BY end_time ASC, exam_id ASC; ");
				pstmt.setLong(1, companyId);
				pstmt.setInt(2, now);
				pstmt.setInt(3, lastExamEndTime);
				pstmt.setInt(4, lastExamId);
				pstmt.setInt(5, lastExamEndTime);
			}
			
			rs = pstmt.executeQuery();
			
			List<Integer> examIdList = new ArrayList<Integer>();
			while (rs.next()) {
				examIdList.add(rs.getInt("exam_id"));
			}
			return examIdList;
		} finally {
			DBUtil.closeQuietly(rs);
			DBUtil.closeQuietly(pstmt);
		}
	}
	
	/**
	 * 获取已经结束的考试信息
	 * @param conn
	 * @param lastExamId
	 * @param lastExamEndTime
	 * @return
	 * @throws SQLException
	 */
	public static List<Integer> getClosedExamIdList(Connection conn, long companyId, int now, @Nullable Integer lastExamId, @Nullable Integer lastExamEndTime) throws SQLException {
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try {
			if (lastExamId == null || lastExamEndTime == null) {
				pstmt = conn.prepareStatement("SELECT exam_id FROM weizhu_exam_exam WHERE company_id = ? AND end_time <= ? ORDER BY end_time DESC, exam_id ASC; ");
				pstmt.setLong(1, companyId);
				pstmt.setInt(2, now);
			} else {
				pstmt = conn.prepareStatement("SELECT exam_id FROM weizhu_exam_exam WHERE company_id = ? AND end_time <= ? AND ((end_time = ? AND exam_id > ?) OR end_time < ? ) ORDER BY end_time DESC, exam_id ASC; ");
				pstmt.setLong(1, companyId);
				pstmt.setInt(2, now);
				pstmt.setInt(3, lastExamEndTime);
				pstmt.setInt(4, lastExamId);
				pstmt.setInt(5, lastExamEndTime);
			}
			
			rs = pstmt.executeQuery();
			
			List<Integer> closeExamIdList = new ArrayList<Integer>();
			while (rs.next()) {
				closeExamIdList.add(rs.getInt("exam_id"));
			}
			return closeExamIdList;
		} finally {
			DBUtil.closeQuietly(rs);
			DBUtil.closeQuietly(pstmt);
		}
	}
	
	public static List<Integer> getSubmitExamIdList(Connection conn, long companyId, @Nullable Integer lastExamId, @Nullable Integer lastExamSubmitTime) throws SQLException {
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try {
			if (lastExamId == null || lastExamSubmitTime == null) {
				pstmt = conn.prepareStatement("SELECT exam_id FROM weizhu_exam_user_result WHERE company_id = ? AND submit_time IS NOT NULL ORDER BY submit_time DESC, exam_id ASC; ");
				pstmt.setLong(1, companyId);
			} else {
				pstmt = conn.prepareStatement("SELECT exam_id FROM weizhu_exam_user_result WHERE company_id = ? AND submit_time IS NOT NULL AND ((submit_time = ? AND exam_id > ?) OR submit_time < ? ) ORDER BY submit_time DESC, exam_id ASC; ");
				pstmt.setLong(1, companyId);
				pstmt.setInt(2, lastExamSubmitTime);
				pstmt.setInt(3, lastExamId);
				pstmt.setInt(4, lastExamSubmitTime);
			}
			
			rs = pstmt.executeQuery();
			
			List<Integer> closeExamIdList = new ArrayList<Integer>();
			while (rs.next()) {
				closeExamIdList.add(rs.getInt("exam_id"));
			}
			return closeExamIdList;
		} finally {
			DBUtil.closeQuietly(rs);
			DBUtil.closeQuietly(pstmt);
		}
	}
	
	public static Map<Integer, Set<Integer>> getExamUserAnswer(Connection conn, long companyId, long userId, int examId) throws SQLException {
		StringBuilder sqlBuilder = new StringBuilder();
		sqlBuilder.append("SELECT * FROM weizhu_exam_user_answer WHERE company_id = ").append(companyId);
		sqlBuilder.append(" AND user_id = ").append(userId);
		sqlBuilder.append(" AND exam_id = ").append(examId);
		sqlBuilder.append("; ");
		
		final String sql = sqlBuilder.toString();
		Statement stmt = null;
		ResultSet rs = null;
		try {
			stmt = conn.createStatement();
			rs = stmt.executeQuery(sql);
			
			Map<Integer, Set<Integer>> examUserAnswerMap = new TreeMap<Integer, Set<Integer>>();
			while (rs.next()) {
				int questionId = rs.getInt("question_id");
				int answerOptionId = rs.getInt("answer_option_id");
				
				Set<Integer> answerOptionIdSet = examUserAnswerMap.get(questionId);
				if (answerOptionIdSet == null) {
					answerOptionIdSet = new TreeSet<Integer>();
					examUserAnswerMap.put(questionId, answerOptionIdSet);
				}
				answerOptionIdSet.add(answerOptionId);
			}
			return examUserAnswerMap;
		} finally {
			DBUtil.closeQuietly(rs);
			DBUtil.closeQuietly(stmt);
		}
	}
	
	public static void updateExamUserAnswer(Connection conn, long companyId, long userId, int examId, 
			Map<Integer, Set<Integer>> oldUserAnswerMap, 
			Map<Integer, Set<Integer>> newUserAnswerMap, 
			int now
			) throws SQLException {
		StringBuilder sqlBuilder = new StringBuilder();
		
		boolean isFirstDelete = true;
		for (Entry<Integer, Set<Integer>> oldEntry : oldUserAnswerMap.entrySet()) {
			Integer questionId = oldEntry.getKey();
			Set<Integer> newAnswerOptionIdSet = newUserAnswerMap.get(questionId);
			
			for (Integer oldAnswerOptionId : oldEntry.getValue()) {
				if (newAnswerOptionIdSet == null || !newAnswerOptionIdSet.contains(oldAnswerOptionId)) {
					if (isFirstDelete) {
						isFirstDelete = false;
						sqlBuilder.append("DELETE FROM weizhu_exam_user_answer WHERE company_id = ").append(companyId);
						sqlBuilder.append(" AND user_id = ").append(userId);
						sqlBuilder.append(" AND exam_id = ").append(examId);
						sqlBuilder.append(" AND (question_id, answer_option_id) IN (");
					} else {
						sqlBuilder.append(", ");
					}
					
					sqlBuilder.append("(").append(questionId);
					sqlBuilder.append(", ").append(oldAnswerOptionId);
					sqlBuilder.append(")");	
				}
			}
		}
		if (!isFirstDelete) {
			sqlBuilder.append(");");
		}
		
		boolean isFirstReplace = true;
		for (Entry<Integer, Set<Integer>> newEntry : newUserAnswerMap.entrySet()) {
			Integer questionId = newEntry.getKey();
			Set<Integer> oldAnswerOptionIdSet = oldUserAnswerMap.get(questionId);
			
			for (Integer newAnswerOptionId : newEntry.getValue()) {
				if (oldAnswerOptionIdSet == null || !oldAnswerOptionIdSet.contains(newAnswerOptionId)) {
					if (isFirstReplace) {
						isFirstReplace = false;
						sqlBuilder.append("REPLACE INTO weizhu_exam_user_answer (company_id, user_id, exam_id, question_id, answer_option_id, answer_time) VALUES ");
					} else {
						sqlBuilder.append(", ");
					}
					
					sqlBuilder.append("(").append(companyId);
					sqlBuilder.append(", ").append(userId);
					sqlBuilder.append(", ").append(examId);
					sqlBuilder.append(", ").append(questionId);
					sqlBuilder.append(", ").append(newAnswerOptionId);
					sqlBuilder.append(", ").append(now);
					sqlBuilder.append(")");	
				}
			}
		}
		if (!isFirstReplace) {
			sqlBuilder.append(";");
		}
		
		if (!isFirstDelete || !isFirstReplace) {
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
	
	public static void saveUserScore(Connection conn, long companyId, 
			long userId, int examId, int score, @Nullable Integer startTime, @Nullable Integer submitTime 
			) throws SQLException {
		PreparedStatement pstmt = null;
		try {
			pstmt = conn.prepareStatement("INSERT IGNORE INTO weizhu_exam_user_result (company_id, user_id, exam_id, start_time, submit_time, score) VALUES (?, ?, ?, ?, ?, ?); "
					+ "UPDATE weizhu_exam_user_result SET submit_time = ?, score = ? WHERE company_id = ? AND user_id = ? AND exam_id = ?; ");

			DBUtil.set(pstmt, 1, companyId);
			DBUtil.set(pstmt, 2, userId);
			DBUtil.set(pstmt, 3, examId);
			DBUtil.set(pstmt, 4, startTime);
			DBUtil.set(pstmt, 5, submitTime);
			DBUtil.set(pstmt, 6, score);
			
			DBUtil.set(pstmt, 7, submitTime);
			DBUtil.set(pstmt, 8, score);
			DBUtil.set(pstmt, 9, companyId);
			DBUtil.set(pstmt, 10, userId);
			DBUtil.set(pstmt, 11, examId);

			pstmt.executeUpdate();
		} finally {
			DBUtil.closeQuietly(pstmt);
		}
    }
	
	public static void saveUserStartTime(Connection conn, long companyId, int examId, long userId, int now) throws SQLException {
		PreparedStatement pstmt = null;
		try {
			pstmt = conn.prepareStatement("INSERT IGNORE INTO weizhu_exam_user_result (company_id, user_id, exam_id, start_time, submit_time, score) VALUES (?, ?, ?, ?, ?, ?);");
			pstmt.setLong(1, companyId);
			pstmt.setLong(2, userId);
			pstmt.setInt(3, examId);
			pstmt.setInt(4, now);
			pstmt.setNull(5, java.sql.Types.INTEGER);
			pstmt.setInt(6, 0);

			pstmt.executeUpdate();
		} finally {
    		DBUtil.closeQuietly(pstmt);
		}
	}
	
	public static Map<Long, List<Integer>> getUnsubmitExamId(Connection conn) throws SQLException {
		Statement stmt = null;
    	ResultSet rs = null;
		try {
			stmt = conn.createStatement();
			rs = stmt.executeQuery("SELECT company_id, exam_id FROM weizhu_exam_exam WHERE is_submit_execute IS NULL OR is_submit_execute = 0; ");
			
			Map<Long, List<Integer>> companyExamMap = new HashMap<Long, List<Integer>>();
			List<Integer> examIdList = null;
			while (rs.next()) {
				long companyId = rs.getLong("company_id");
				
				examIdList = companyExamMap.get(companyId);
				
				if (examIdList == null) {
					examIdList = new ArrayList<Integer>();
				}
				examIdList.add(rs.getInt("exam_id"));
				
				companyExamMap.put(companyId, examIdList);
			}
			
			return companyExamMap;
		} finally {
    		DBUtil.closeQuietly(rs);
    		DBUtil.closeQuietly(stmt);
    	}
	}
	
	public static boolean setExamSubmitExecuted(Connection conn, long companyId, int examId) throws SQLException {
		PreparedStatement pstmt = null;
		try {
			pstmt = conn.prepareStatement("UPDATE weizhu_exam_exam SET is_submit_execute = 1 WHERE company_id = ? AND exam_id = ? AND (is_submit_execute IS NULL OR is_submit_execute = 0); ");
			pstmt.setLong(1, companyId);
			pstmt.setInt(2, examId);
			
			return pstmt.executeUpdate() > 0;
		} finally {
    		DBUtil.closeQuietly(pstmt);
    	}
	}
	
	public static List<Long> getUnsubmitUserId(Connection conn, long companyId, int examId) throws SQLException {
		PreparedStatement pstmt = null;
    	ResultSet rs = null;
		try {
			pstmt = conn.prepareStatement("SELECT user_id FROM weizhu_exam_user_result WHERE company_id = ? AND exam_id = ? AND submit_time IS NULL; ");
			pstmt.setLong(1, companyId);
			pstmt.setInt(2, examId);
			
			rs = pstmt.executeQuery();
			
			List<Long> userIdList = new ArrayList<Long>();
			while (rs.next()) {
				userIdList.add(rs.getLong("user_id"));
			}
			return userIdList;
		} finally {
    		DBUtil.closeQuietly(rs);
    		DBUtil.closeQuietly(pstmt);
    	}
	}
	
	public static DataPage<Integer> getQuestionIdPage(Connection conn, long companyId, int start, int length, String condition) throws SQLException {
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		
		List<Integer> questionIdList = new LinkedList<Integer>();
		
		try {
			if (condition.isEmpty()) {
				pstmt = conn.prepareStatement("SELECT question_id FROM weizhu_exam_question WHERE company_id = ? ORDER BY question_id DESC LIMIT ?,?; ");
				pstmt.setLong(1, companyId);
				pstmt.setInt(2, start);
				pstmt.setInt(3, length);
			} else {
				pstmt = conn.prepareStatement("SELECT question_id FROM weizhu_exam_question WHERE company_id = ? AND question_name LIKE ? ORDER BY question_id DESC LIMIT ?,?; ");
				pstmt.setLong(1, companyId);
				pstmt.setString(2, '%' + DBUtil.SQL_LIKE_STRING_ESCAPER.escape(condition) + '%');
				pstmt.setInt(3, start);
				pstmt.setInt(4, length);
			}
			
			rs = pstmt.executeQuery();
			while (rs.next()) {
				questionIdList.add(rs.getInt("question_id"));
			}
		} finally {
			DBUtil.closeQuietly(rs);
			DBUtil.closeQuietly(pstmt);
		}

		int total = questionIdList.size();
		
		return new DataPage<Integer>(questionIdList, total, total);
	}
	
	public static DataPage<Integer> getQuestionIdPageByExam(Connection conn, long companyId, int start, int length, int examId) throws SQLException {
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		
		try {
			pstmt = conn.prepareStatement("SELECT COUNT(1) AS total FROM weizhu_exam_exam_question WHERE company_id = ? AND exam_id = ?; "
					+ "SELECT question_id FROM weizhu_exam_exam_question WHERE company_id = ? AND exam_id = ? LIMIT ?, ?; ");
			pstmt.setLong(1, companyId);
			pstmt.setInt(2, examId);
			pstmt.setLong(3, companyId);
			pstmt.setInt(4, examId);
			pstmt.setInt(5, start);
			pstmt.setInt(6, length);
			pstmt.execute();
			rs = pstmt.getResultSet();
			if (!rs.next()) {
				throw new RuntimeException("cannot get total!");
			}
			int total = rs.getInt("total");
			DBUtil.closeQuietly(rs);
			rs = null;
			
			pstmt.getMoreResults();
			rs = pstmt.getResultSet();
			List<Integer> list = Lists.newArrayList();
			while (rs.next()) {
				list.add(rs.getInt("question_id"));
			}
			
			return new DataPage<Integer>(list, total, total);
		} finally {
			DBUtil.closeQuietly(rs);
			DBUtil.closeQuietly(pstmt);
		}
	}
	
	public static int insertQuestion(Connection conn, long companyId, String questionName, ExamProtos.Question.Type type, int now, long adminId, List<ExamProtos.Option> optionList, int categoryId) throws SQLException {
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		int questionId = 0;
		try {
			pstmt = conn.prepareStatement("INSERT INTO weizhu_exam_question (company_id, question_name, type, create_question_time, create_question_admin_id) VALUES (?, ?, ?, ?, ?); ", Statement.RETURN_GENERATED_KEYS);
			pstmt.setLong(1, companyId);
			pstmt.setString(2, questionName);
			pstmt.setString(3, type.toString());
			pstmt.setInt(4, now);
			pstmt.setLong(5, adminId);
			
			pstmt.executeUpdate();
			rs = pstmt.getGeneratedKeys();
			if (!rs.next()) {
				throw new RuntimeException("key not generate");
			}
			questionId = rs.getInt(1);
			
			DBUtil.closeQuietly(pstmt);
			pstmt = null;
			if (!optionList.isEmpty()) {
				pstmt = conn.prepareStatement("INSERT INTO weizhu_exam_option (company_id, option_name, question_id, is_right) VALUES (?, ?, ?, ?); ");
				for (ExamProtos.Option option : optionList) {
					pstmt.setLong(1, companyId);
					pstmt.setString(2, option.getOptionName());
					pstmt.setInt(3, questionId);
					pstmt.setBoolean(4, option.getIsRight());
					
					pstmt.addBatch();
				}
				pstmt.executeBatch();
			}
			
			DBUtil.closeQuietly(pstmt);
			pstmt = null;
			pstmt = conn.prepareStatement("INSERT INTO weizhu_exam_question_category_join_question (company_id, category_id, question_id) VALUES (?, ?, ?); ");
			pstmt.setLong(1, companyId);
			pstmt.setInt(2, categoryId);
			pstmt.setInt(3, questionId);
			pstmt.executeUpdate();
			
		} finally {
			DBUtil.closeQuietly(rs);
			DBUtil.closeQuietly(pstmt);
		}
		
		return questionId;
	}
	
	public static void deleteOption(Connection conn, long companyId, Collection<ExamProtos.Option> options) throws SQLException {
		if (options.isEmpty()) {
			return ;
		}
		
		Statement stmt = null;
		
		int size = 0;
		StringBuilder sql = new StringBuilder("DELETE FROM weizhu_exam_option WHERE company_id = ").append(companyId).append(" AND option_id IN (");
		for (ExamProtos.Option option : options) {
			if (size == options.size() - 1) {
				sql.append(option.getOptionId());
			} else {
				sql.append(option.getOptionId()).append(", ");
			}
			size ++ ;
		}
		sql.append("); ");
		
		try {
			stmt = conn.createStatement();
			stmt.executeUpdate(sql.toString());
		} finally {
			DBUtil.closeQuietly(stmt);
		}
	}
	
	public static void addOption(Connection conn, long companyId, int questionId, Collection<ExamProtos.Option> options) throws SQLException {
		if (options.isEmpty()) {
			return ;
		}
		
		PreparedStatement pstmt = null;
		try {
			pstmt = conn.prepareStatement("INSERT INTO weizhu_exam_option (company_id, option_name, is_right, question_id) VALUES (?, ?, ?, ?); ");
			for (ExamProtos.Option option : options) {
				pstmt.setLong(1, companyId);
				pstmt.setString(2, option.getOptionName());
				pstmt.setBoolean(3, option.getIsRight());
				pstmt.setInt(4, questionId);
				
				pstmt.addBatch();
			}
			pstmt.executeBatch();
		} finally {
			DBUtil.closeQuietly(pstmt);
		}
	}
	
	public static void updateOption(Connection conn, long companyId, Collection<ExamProtos.Option> options) throws SQLException {
		if (options.isEmpty()) {
			return ;
		}
		
		PreparedStatement pstmt = null;
		try {
			pstmt = conn.prepareStatement("UPDATE weizhu_exam_option SET option_name = ?, is_right = ? WHERE company_id = ? AND option_id = ?; ");
			for (ExamProtos.Option option : options) {
				pstmt.setString(1, option.getOptionName());
				pstmt.setBoolean(2, option.getIsRight());
				pstmt.setLong(3, companyId);
				pstmt.setInt(4, option.getOptionId());
				
				pstmt.addBatch();
			}
			pstmt.executeBatch();
		} finally {
			DBUtil.closeQuietly(pstmt);
		}
	}
	
	public static List<Integer> getExamIdByQuestionId(Connection conn, long companyId, Collection<Integer> questionIds) throws SQLException {
		if (questionIds.isEmpty()) {
			return Collections.emptyList();
		}
		
		StringBuilder sql = new StringBuilder("SELECT distinct(exam_id) FROM weizhu_exam_exam_question WHERE company_id = ").append(companyId).append(" AND question_id IN (");
		sql.append(DBUtil.COMMA_JOINER.join(questionIds));
		sql.append("); ");
		
		List<Integer> examIdList = new ArrayList<Integer>();
		
		Statement stmt = null;
		ResultSet rs = null;
		
		try {
			stmt = conn.createStatement();
			rs = stmt.executeQuery(sql.toString());
			
			while (rs.next()) {
				examIdList.add(rs.getInt("exam_id"));
			}
			
			return examIdList;
		} finally {
			DBUtil.closeQuietly(rs);
			DBUtil.closeQuietly(stmt);
		}
	}
	
	public static void updateQuestion(Connection conn, long companyId, int questionId, String questionName, ExamProtos.Question.Type type) throws SQLException {
		PreparedStatement pstmt = null;
		try {
			pstmt = conn.prepareStatement("UPDATE weizhu_exam_question SET question_name = ?, type = ? WHERE company_id = ? AND question_id = ?; ");
			pstmt.setString(1, questionName);
			pstmt.setString(2, type.toString());
			pstmt.setLong(3, companyId);
			pstmt.setInt(4, questionId);
			
			pstmt.executeUpdate();
		} finally {
			DBUtil.closeQuietly(pstmt);
		}
	}
	
	public static void deleteQuestion(Connection conn, long companyId, Collection<Integer> questionIds) throws SQLException {
		StringBuilder sql = new StringBuilder("DELETE FROM weizhu_exam_question WHERE company_id = ").append(companyId).append(" AND question_id IN (");
		sql.append(DBUtil.COMMA_JOINER.join(questionIds));
		sql.append("); ");
		sql.append("DELETE FROM weizhu_exam_question_category_join_question WHERE company_id = ").append(companyId).append(" AND question_id IN (");
		sql.append(DBUtil.COMMA_JOINER.join(questionIds));
		sql.append("); ");
		
		Statement stmt = null;
		try {
			stmt = conn.createStatement();
			stmt.executeUpdate(sql.toString());
		} finally {
			DBUtil.closeQuietly(stmt);
		}
	}
	
	public static DataPage<Integer> getExamIdByPage(Connection conn, long companyId, int start, int length, int now, @Nullable String examName, @Nullable Integer state) throws SQLException {
		StringBuilder conditionBuilder = new StringBuilder();
		if (examName != null && !examName.isEmpty()) {
			conditionBuilder.append(" AND exam_name like '%").append(DBUtil.SQL_LIKE_STRING_ESCAPER.escape(examName)).append("%' ");
		}
		if (state != null && state == 0) {
			conditionBuilder.append(" AND start_time > ").append(now);
		} else if (state != null && state == 1) {
			conditionBuilder.append(" AND start_time <= ").append(now).append(" AND end_time > ").append(now);
		} else if (state != null && state == 2) {
			conditionBuilder.append(" AND end_time <= ").append(now);
		}
		
		String condition = conditionBuilder.toString();
		
		StringBuilder sql = new StringBuilder("SELECT COUNT(1) AS total FROM weizhu_exam_exam WHERE company_id = ").append(companyId);
		if (!condition.isEmpty()) {
			sql.append(condition);
		}
		sql.append("; ");
		sql.append("SELECT exam_id FROM weizhu_exam_exam WHERE company_id = ").append(companyId);
		if (!condition.isEmpty()) {
			sql.append(condition);
		}
		sql.append(" ORDER BY exam_id DESC LIMIT ").append(start).append(",").append(length).append("; ");
		
		Statement stmt = null;
		ResultSet rs = null;

		try {
			stmt = conn.createStatement();
			stmt.execute(sql.toString());
			
			rs = stmt.getResultSet();
			if (!rs.next()) {
				throw new RuntimeException("cannot get total!");
			}
			int total = rs.getInt("total");
			
			DBUtil.closeQuietly(rs);
			rs = null;
			stmt.getMoreResults();
			rs = stmt.getResultSet();
			
			List<Integer> examIdList = new LinkedList<Integer>();
			while (rs.next()) {
				examIdList.add(rs.getInt("exam_id"));
			}
			
			return new DataPage<Integer>(examIdList, total, total);
		} finally {
			DBUtil.closeQuietly(rs);
			DBUtil.closeQuietly(stmt);
		}
	}
	
	public static int insertExam(Connection conn, long companyId, String examName, @Nullable String imageName, int startTime, int endTime, long adminId, int now, int passMark, ExamProtos.Exam.Type type, @Nullable Integer allowModelId, ExamProtos.ShowResult showResult) throws SQLException {
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		
		int examId = 0;
		
		try {
			pstmt = conn.prepareStatement("INSERT INTO weizhu_exam_exam (company_id, exam_name, image_name, start_time, end_time, create_exam_admin_id, create_exam_time, pass_mark, type, allow_model_id, show_result) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?); ", Statement.RETURN_GENERATED_KEYS);
			pstmt.setLong(1, companyId);
			pstmt.setString(2, examName);
			if (imageName == null) {
				pstmt.setNull(3, java.sql.Types.VARCHAR);
			} else {
				pstmt.setString(3, imageName);
			}
			pstmt.setInt(4, startTime);
			pstmt.setInt(5, endTime);
			pstmt.setLong(6, adminId);
			pstmt.setInt(7, now);
			pstmt.setInt(8, passMark);
			pstmt.setString(9, type.name());
			if (allowModelId == null) {
				pstmt.setNull(10, java.sql.Types.INTEGER);
			} else {
				pstmt.setInt(10, allowModelId);
			}
			pstmt.setString(11, showResult.name());
			
			pstmt.executeUpdate();
			rs = pstmt.getGeneratedKeys();
			if (rs.next()) {
				examId = rs.getInt(1);
			}
			
			return examId;
		} finally {
			DBUtil.closeQuietly(rs);
			DBUtil.closeQuietly(pstmt);
		}
	}
	
	public static void updateExam(Connection conn, long companyId, int examId, String examName, @Nullable String imageName, int startTime, int endTime, int passMark, @Nullable Integer allowModelId, ExamProtos.ShowResult showResult) throws SQLException {
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		Statement stmt = null;
		
		try {
			pstmt = conn.prepareStatement("UPDATE weizhu_exam_exam SET exam_name = ?, image_name = ?, start_time = ?, end_time = ?, pass_mark = ?, allow_model_id = ?, show_result = ? WHERE company_id = ? AND exam_id = ?; ");
			pstmt.setString(1, examName);
			if (imageName == null) {
				pstmt.setNull(2, java.sql.Types.VARCHAR);
			} else {
				pstmt.setString(2, imageName);
			}
			pstmt.setString(2, imageName);
			pstmt.setInt(3, startTime);
			pstmt.setInt(4, endTime);
			pstmt.setInt(5, passMark);
			if (allowModelId == null) {
				pstmt.setNull(6, java.sql.Types.INTEGER);
			} else {
				pstmt.setInt(6, allowModelId);
			}
			pstmt.setString(7, showResult.name());
			pstmt.setLong(8, companyId);
			pstmt.setInt(9, examId);
			
			pstmt.executeUpdate();
		} finally {
			DBUtil.closeQuietly(stmt);
			DBUtil.closeQuietly(pstmt);
			DBUtil.closeQuietly(rs);
		}
	}
	
	public static void deleteExam(Connection conn, long companyId, Collection<Integer> examIds) throws SQLException {
		if (examIds.isEmpty()) {
			return ;
		}
		
		String examIdStr = DBUtil.COMMA_JOINER.join(examIds);
		
		StringBuilder sql = new StringBuilder("DELETE FROM weizhu_exam_exam WHERE company_id = ").append(companyId).append(" AND exam_id IN (");
		sql.append(examIdStr);
		sql.append("); ");
		sql.append("DELETE FROM weizhu_exam_exam_question WHERE company_id = ").append(companyId).append(" AND exam_id IN (");
		sql.append(examIdStr);
		sql.append("); ");
		
		Statement stmt = null;
		try {
			stmt = conn.createStatement();
			stmt.executeUpdate(sql.toString());
		} finally {
			DBUtil.closeQuietly(stmt);
		}
	}
	
	/**
	 * 更新手动选题，考试对题目
	 * @param conn
	 * @param companyId
	 * @param examId           指定的考试
	 * @param questionScoreMap Map<questionId, score>
	 * @throws SQLException
	 */
	public static void updateExamQuestion(Connection conn, long companyId, int examId, LinkedHashMap<Integer, Integer> questionScoreMap) throws SQLException {
		PreparedStatement pstmt = null;
		Statement stmt = null;
		
		try {
			pstmt = conn.prepareStatement("DELETE FROM weizhu_exam_exam_question WHERE company_id = ? AND exam_id = ?; ");
			pstmt.setLong(1, companyId);
			pstmt.setInt(2, examId);
			pstmt.executeUpdate();
		} finally {
			DBUtil.closeQuietly(pstmt);
		}
	
		if (questionScoreMap.isEmpty()) {
			return;
		}
		
		StringBuilder sql = null;
		for (Entry<Integer, Integer> entry : questionScoreMap.entrySet()) {
			if (sql == null) {
				sql = new StringBuilder("INSERT INTO weizhu_exam_exam_question (company_id, exam_id, question_id, score) VALUES ");
			} else {
				sql.append(", ");
			}
			sql.append("(");
			sql.append(companyId).append(", ");
			sql.append(examId).append(", ");
			sql.append(entry.getKey()).append(", ");
			sql.append(entry.getValue()).append(")");
		}
		sql = sql == null ? null : sql.append("; ");
		if (sql == null) {
			return ;
		}
		sql.append("UPDATE weizhu_exam_exam SET question_order_str = '").append(DBUtil.COMMA_JOINER.join(questionScoreMap.keySet())).append("' ");
		sql.append("WHERE company_id = ").append(companyId).append(" AND ");
		sql.append("exam_id = ").append(examId).append("; ");
		
		try {
			stmt = conn.createStatement();
			stmt.execute(sql.toString());
		} finally {
			DBUtil.closeQuietly(stmt);
		}
	}
	
	/**
	 * 更新随机出题考试对题库
	 * @param conn
	 * @param companyId
	 * @param examId
	 * @param questionCategoryStr 考试题库id，中间用
	 * @param questionNum
	 * @throws SQLException
	 */
	public static void UpdateExamCategory(Connection conn, long companyId, int examId, Collection<Integer> questionCategoryStr, int questionNum) throws SQLException {
		PreparedStatement pstmt = null;
		
		try {
			pstmt = conn.prepareStatement("DELETE FROM weizhu_exam_exam_join_category WHERE company_id = ? AND exam_id = ?; ");
			pstmt.setLong(1, companyId);
			pstmt.setInt(2, examId);
			pstmt.executeUpdate();
		} finally {
			DBUtil.closeQuietly(pstmt);
		}
		
		if (questionCategoryStr.isEmpty()) {
			return;
		}
		
		try {
			pstmt = conn.prepareStatement("INSERT INTO weizhu_exam_exam_join_category (company_id, exam_id, question_category_str, question_num) VALUES (?, ?, ?, ?); ");
			pstmt.setLong(1, companyId);
			pstmt.setInt(2, examId);
			pstmt.setString(3, DBUtil.COMMA_JOINER.join(questionCategoryStr));
			pstmt.setInt(4, questionNum);
			pstmt.executeUpdate();
		} finally {
			DBUtil.closeQuietly(pstmt);
		}
	}
	
	public static DataPage<UserResult> getUserResultPage(Connection conn, long companyId, int examId, int start, int length) throws SQLException {
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		
		try {
			pstmt = conn.prepareStatement("SELECT COUNT(1) AS total FROM weizhu_exam_user_result WHERE company_id = ? AND exam_id = ?; SELECT user_id, start_time, submit_time, score FROM weizhu_exam_user_result WHERE company_id = ? AND exam_id = ? ORDER BY score DESC LIMIT ?, ?; ");
			pstmt.setLong(1, companyId);
			pstmt.setInt(2, examId);
			pstmt.setLong(3, companyId);
			pstmt.setInt(4, examId);
			pstmt.setInt(5, start);
			pstmt.setInt(6, length);
			
			pstmt.execute();
			
			rs = pstmt.getResultSet();
			if (!rs.next()) {
				throw new RuntimeException("can't get total!");
			}
			int total = rs.getInt("total");
			DBUtil.closeQuietly(rs);
			rs = null;
			
			pstmt.getMoreResults();
			rs = pstmt.getResultSet();
			
			List<UserResult> userResultList = new ArrayList<UserResult>();
			UserResult.Builder userResultBuilder = UserResult.newBuilder();
			while (rs.next()) {
				userResultBuilder.clear();
				userResultBuilder.setExamId(examId);
				userResultBuilder.setUserId(rs.getLong("user_id"));
				userResultBuilder.setStartTime(rs.getInt("start_time"));
				userResultBuilder.setSubmitTime(rs.getInt("submit_time"));
				userResultBuilder.setScore(rs.getInt("score"));
				
				userResultList.add(userResultBuilder.build());
			}
			
			return new DataPage<UserResult>(userResultList, total, total);
		} finally {
			DBUtil.closeQuietly(pstmt);
			DBUtil.closeQuietly(rs);
		}
	}
	
	public static boolean getQuestionCategoryByName(Connection conn, long companyId, String categoryName) throws SQLException {
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		
		try {
			pstmt = conn.prepareStatement("SELECT COUNT(1) AS record FROM weizhu_exam_question_category WHERE company_id = ? AND category_name = ?; ");
			pstmt.setLong(1, companyId);
			pstmt.setString(2, categoryName);
			
			rs = pstmt.executeQuery();
			if (!rs.next()) {
				throw new RuntimeException("cannot get record!");
			}
			return rs.getInt("record") > 0;
		} finally {
			DBUtil.closeQuietly(rs);
			DBUtil.closeQuietly(pstmt);
		}
	}
	
	public static int insertQuestionCategory(Connection conn, long companyId, String categoryName, @Nullable Integer parentCategoryId, int now, long adminId) throws SQLException {
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		
		try {
			if (parentCategoryId == null) {
				pstmt = conn.prepareStatement("INSERT INTO weizhu_exam_question_category (company_id, category_name, create_admin_id, create_time) VALUES (?, ?, ?, ?); ", Statement.RETURN_GENERATED_KEYS);
				DBUtil.set(pstmt, 1, true, companyId);
				DBUtil.set(pstmt, 2, true, categoryName);
				DBUtil.set(pstmt, 3, true, adminId);
				DBUtil.set(pstmt, 4, true, now);
			} else {
				pstmt = conn.prepareStatement("INSERT INTO weizhu_exam_question_category (company_id, category_name, parent_category_id, create_admin_id, create_time) VALUES (?, ?, ?, ?, ?); ", Statement.RETURN_GENERATED_KEYS);
				DBUtil.set(pstmt, 1, true, companyId);
				DBUtil.set(pstmt, 2, true, categoryName);
				DBUtil.set(pstmt, 3, true, parentCategoryId);
				DBUtil.set(pstmt, 4, true, adminId);
				DBUtil.set(pstmt, 5, true, now);
			}
			
			pstmt.executeUpdate();
			rs = pstmt.getGeneratedKeys();
			if (!rs.next()) {
				throw new RuntimeException("key not generate");
			}
			return rs.getInt(1);
		} finally {
			DBUtil.closeQuietly(rs);
			DBUtil.closeQuietly(pstmt);
		}
	}
	
	public static int getQuestionCount(Connection conn, long companyId, Collection<Integer> categoryIds) throws SQLException {
		if (categoryIds.isEmpty()) {
			return 0;
		}

		StringBuilder sql = new StringBuilder();
		sql.append("SELECT COUNT(1) FROM weizhu_exam_question_category_join_question WHERE company_id = ").append(companyId).append(" AND ");
		sql.append("category_id IN (");
		sql.append(DBUtil.COMMA_JOINER.join(categoryIds)).append("); ");
		
		Statement stmt = null;
		ResultSet rs = null;
		try {
			stmt = conn.createStatement();
			rs = stmt.executeQuery(sql.toString());
			
			if (!rs.next()) {
				throw new RuntimeException("cannot get total!");
			}
			
			return rs.getInt(1);
		} finally {
			DBUtil.closeQuietly(rs);
			DBUtil.closeQuietly(stmt);
		}
	}
	
	public static Map<Integer, QuestionCategory> getQuestionCategoryById(Connection conn, long companyId, Collection<Integer> categoryIds) throws SQLException {
		if (categoryIds.isEmpty()) {
			return Collections.emptyMap();
		}
		
		StringBuilder sql = new StringBuilder();
		sql.append("SELECT category_id, category_name, create_admin_id, create_time FROM weizhu_exam_question_category WHERE company_id = ").append(companyId).append(" AND category_id IN (");
		sql.append(DBUtil.COMMA_JOINER.join(categoryIds));
		sql.append("); ");
		
		Statement stmt = null;
		ResultSet rs = null;
		
		try {
			stmt = conn.createStatement();
			rs = stmt.executeQuery(sql.toString());
			Map<Integer, QuestionCategory> questionCategoryMap = new HashMap<Integer, QuestionCategory>();
			QuestionCategory.Builder questionCategoryBuilder = QuestionCategory.newBuilder();
			while (rs.next()) {
				questionCategoryBuilder.clear();
				
				int categoryId = rs.getInt("category_id");
				questionCategoryBuilder.setCategoryId(categoryId);
				questionCategoryBuilder.setCategoryName(rs.getString("category_name"));
				questionCategoryBuilder.setCreateAdminId(rs.getLong("create_admin_id"));
				questionCategoryBuilder.setCreateTime(rs.getInt("create_time"));
				
				questionCategoryMap.put(categoryId, questionCategoryBuilder.build());
			}
			return questionCategoryMap;
		} finally {
			DBUtil.closeQuietly(rs);
			DBUtil.closeQuietly(stmt);
		}
	}
	
	public static DataPage<Integer> getQuestionCategoryId(Connection conn, long companyId, int start, int length, String condition) throws SQLException {
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		
		try {
			if (condition.isEmpty()) {
			    pstmt = conn.prepareStatement("SELECT COUNT(1) AS total FROM weizhu_exam_question_category WHERE company_id = ?; SELECT category_id FROM weizhu_exam_question_category WHERE company_id = ? ORDER BY category_id LIMIT ?, ?; ");
				pstmt.setLong(1, companyId);
				pstmt.setLong(2, companyId);
			    pstmt.setInt(3, start);
				pstmt.setInt(4, length);
			} else {
				pstmt = conn.prepareStatement("SELECT COUNT(1) AS total FROM weizhu_exam_question_category WHERE company_id = ? AND category_name LIKE ?; SELECT category_id FROM weizhu_exam_question_category WHERE company_id = ? AND category_name LIKE ? ORDER BY category_id LIMIT ?, ?; ");
				pstmt.setLong(1, companyId);
				pstmt.setString(2, '%' + DBUtil.SQL_LIKE_STRING_ESCAPER.escape(condition) + '%');
				pstmt.setLong(3, companyId);
				pstmt.setString(4, '%' + DBUtil.SQL_LIKE_STRING_ESCAPER.escape(condition) + '%');
				pstmt.setInt(5, start);
				pstmt.setInt(6, length);
			}
			
			pstmt.execute();
			rs = pstmt.getResultSet();
			if (!rs.next()) {
				throw new RuntimeException("cannot get total");
			}
			int total = rs.getInt("total");
			
			DBUtil.closeQuietly(rs);
			rs = null;
			
			pstmt.getMoreResults();
			rs = pstmt.getResultSet();
			
			List<Integer> questionCategoryIdList = new ArrayList<Integer>();
			while (rs.next()) {
				questionCategoryIdList.add(rs.getInt("category_id"));
			}
			
			return new DataPage<Integer>(questionCategoryIdList, total, total);
		} finally {
			DBUtil.closeQuietly(rs);
			DBUtil.closeQuietly(pstmt);
		}
	}
	
	public static Map<Integer, List<QuestionCategory>> getQuestionCategory(Connection conn, long companyId) throws SQLException {
		Statement stmt = null;
		ResultSet rs = null;
		
		StringBuilder sql = new StringBuilder();
		sql.append("SELECT * FROM weizhu_exam_question_category_join_question; SELECT * FROM weizhu_exam_question_category WHERE company_id = ").append(companyId).append(" ORDER BY category_name ASC; ");
		try {
			stmt = conn.createStatement();
			
			stmt.execute(sql.toString());
			rs = stmt.getResultSet();
			
			Map<Integer, List<Integer>> categoryJoinQuestionMap = new HashMap<Integer, List<Integer>>();
			while (rs.next()) {
				int categoryId = rs.getInt("category_id");
				List<Integer> questionIdList = categoryJoinQuestionMap.get(categoryId);
				if (questionIdList == null) {
					questionIdList = new ArrayList<Integer>();
				}
				questionIdList.add(rs.getInt("question_id"));
				
				categoryJoinQuestionMap.put(categoryId, questionIdList);
			}
			
			DBUtil.closeQuietly(rs);
			rs = null;
			
			stmt.getMoreResults();
			rs = stmt.getResultSet();
			
			Map<Integer, List<QuestionCategory>> questionCategoryMap = new LinkedHashMap<Integer, List<QuestionCategory>>();
			QuestionCategory.Builder questionCategoryBuilder = QuestionCategory.newBuilder();
			while (rs.next()) {
				int parentCategoryId = rs.getInt("parent_category_id");
				
				if (!rs.wasNull()) {
					questionCategoryBuilder.clear();
					
					int categoryId = rs.getInt("category_id");
					questionCategoryBuilder.setCategoryId(categoryId);
					questionCategoryBuilder.setCategoryName(rs.getString("category_name"));
					questionCategoryBuilder.setCreateAdminId(rs.getLong("create_admin_id"));
					questionCategoryBuilder.setCreateTime(rs.getInt("create_time"));
					questionCategoryBuilder.addAllQuestionId(categoryJoinQuestionMap.get(categoryId) == null ? Collections.emptyList() : categoryJoinQuestionMap.get(categoryId));
					
					List<QuestionCategory> questionCategoryList = questionCategoryMap.get(parentCategoryId);
					if (questionCategoryList == null) {
						questionCategoryList = new ArrayList<QuestionCategory>();
						
					}
					questionCategoryList.add(questionCategoryBuilder.build());
					questionCategoryMap.put(parentCategoryId, questionCategoryList);
				}
				
			}
			
			return questionCategoryMap;
		} finally {
			DBUtil.closeQuietly(rs);
			DBUtil.closeQuietly(stmt);
		}
		
	}
	
	public static void updateCategoryBelongs(Connection conn, long companyId, int categoryId, @Nullable Integer parentCategoryId) throws SQLException {
		PreparedStatement pstmt = null;
		
		try {
			pstmt = conn.prepareStatement("UPDATE weizhu_exam_category SET parent_category_id = ? WHERE company_id = ? AND category_id = ?; ");
			DBUtil.set(pstmt, 1, parentCategoryId != null, parentCategoryId);
			DBUtil.set(pstmt, 2, true, companyId);
			DBUtil.set(pstmt, 3, true, categoryId);
			
			pstmt.executeUpdate();
		} finally {
			DBUtil.closeQuietly(pstmt);
		}
	}
	
	public static Map<Integer, QuestionCategory> getQuestionCategoryRoot(Connection conn, long companyId) throws SQLException {
		Statement stmt = null;
		ResultSet rs = null;
		
		StringBuilder sql = new StringBuilder();
		sql.append("SELECT * FROM weizhu_exam_question_category WHERE company_id = ").append(companyId).append(" AND parent_category_id IS NULL ORDER BY category_name ASC; ");
		
		try {
			stmt = conn.createStatement();
			rs = stmt.executeQuery(sql.toString());
			
			Map<Integer, QuestionCategory> questionCategoryRoot = new LinkedHashMap<Integer, QuestionCategory>();
			QuestionCategory.Builder questionCategoryRootBuilder = QuestionCategory.newBuilder();
			while (rs.next()) {
				questionCategoryRootBuilder.clear();
				
				int categoryId = rs.getInt("category_id");
				questionCategoryRootBuilder.setCategoryId(categoryId);
				questionCategoryRootBuilder.setCategoryName(rs.getString("category_name"));
				questionCategoryRootBuilder.setCreateAdminId(rs.getLong("create_admin_id"));
				questionCategoryRootBuilder.setCreateTime(rs.getInt("create_time"));
				
				questionCategoryRoot.put(categoryId, questionCategoryRootBuilder.build());
			}
			
			DBUtil.closeQuietly(rs);
			rs = null;
			DBUtil.closeQuietly(stmt);
			stmt = null;
			
			Map<Integer, List<Integer>> categoryQuestionMap = Maps.newHashMap();
			if (!questionCategoryRoot.isEmpty()) {
				StringBuilder sql1 = new StringBuilder();
				sql1.append("SELECT category_id, question_id FROM weizhu_exam_question_category_join_question WHERE company_id = ").append(companyId);
				sql1.append(" AND category_id IN (").append(DBUtil.COMMA_JOINER.join(questionCategoryRoot.keySet())).append("); ");
				
				stmt = conn.createStatement();
				rs = stmt.executeQuery(sql1.toString());
				
				List<Integer> list;
				while (rs.next()) {
					int categoryId = rs.getInt("category_id");
					list = categoryQuestionMap.get(categoryId);
					if (list == null) {
						list = Lists.newArrayList();
					}
					
					int questionId = rs.getInt("question_id");
					list.add(questionId);
					categoryQuestionMap.put(categoryId, list);
				}
			}
			
			Map<Integer, QuestionCategory> categoryMap = Maps.newHashMap();
			for (Entry<Integer, QuestionCategory> entry : questionCategoryRoot.entrySet()) {
				int categoryId = entry.getKey();
				QuestionCategory questionCategory = entry.getValue();
				
				List<Integer> list = categoryQuestionMap.get(categoryId);
				if (list != null) {
					categoryMap.put(categoryId, QuestionCategory.newBuilder()
							.mergeFrom(questionCategory)
							.addAllQuestionId(list)
							.build());
				} else {
					categoryMap.put(categoryId, questionCategory);
				}
			}
			
			return categoryMap;
		} finally {
			DBUtil.closeQuietly(rs);
			DBUtil.closeQuietly(stmt);
		}
		
	}
	
	public static Map<Integer, List<Integer>> getQuestionIdByCategory(Connection conn, long companyId, int start, int length, Collection<Integer> categoryIds) throws SQLException {
		if (categoryIds.isEmpty()) {
			return Collections.emptyMap();
		}
		
		Statement stmt = null;
		ResultSet rs = null;
		
		StringBuilder sql = new StringBuilder("SELECT question_id, category_id FROM weizhu_exam_question_category_join_question WHERE company_id = ").append(companyId).append(" AND category_id IN (");
		sql.append(DBUtil.COMMA_JOINER.join(categoryIds));
		if (length == -1) {
			sql.append(") ORDER BY question_id DESC; ");
		} else {
			sql.append(") ORDER BY question_id DESC LIMIT "+ start + "," + length + "; ");
		}
		
		try {
			stmt = conn.createStatement();
			rs = stmt.executeQuery(sql.toString());
			
			List<Integer> questionIdList = null;
			Map<Integer, List<Integer>> questionCategoryMap = new HashMap<Integer, List<Integer>>();
			while (rs.next()) {
				int categoryId = rs.getInt("category_id");
				
				if (questionCategoryMap.get(categoryId) == null) {
					questionIdList = new ArrayList<Integer>();
					questionCategoryMap.put(categoryId, questionIdList);
				} else {
					questionIdList = questionCategoryMap.get(categoryId);
				}
				questionIdList.add(rs.getInt("question_id"));
			}

			return questionCategoryMap;
		} finally {
			DBUtil.closeQuietly(rs);
			DBUtil.closeQuietly(stmt);
		}
	}
	
	public static void updateQuestionInQuestionCategory(Connection conn, long companyId, int oldCategory, int newCategory, Collection<Integer> questionIds) throws SQLException {
		if (questionIds.isEmpty()) {
			return ;
		}
		
		StringBuilder sql = new StringBuilder("DELETE FROM weizhu_exam_question_category_join_question WHERE company_id = ").append(companyId).append(" AND category_id = ");
		sql.append(oldCategory);
		sql.append(" AND question_id IN (");
		sql.append(DBUtil.COMMA_JOINER.join(questionIds));
		sql.append("); ");
		
		Statement stmt = null;
	
		try {
			stmt = conn.createStatement();
			
			stmt.executeUpdate(sql.toString());
		} finally {
			DBUtil.closeQuietly(stmt);
		}
		
		PreparedStatement pstmt = null;
		
		try {
			pstmt = conn.prepareStatement("INSERT INTO weizhu_exam_question_category_join_question (company_id, category_id, question_id) VALUES (?, ?, ?); ");
			for (int questionId : questionIds) {
				pstmt.setLong(1, companyId);
				pstmt.setInt(2, newCategory);
				pstmt.setInt(3, questionId);
				
				pstmt.addBatch();
			}
			pstmt.executeBatch();
		} finally {
			DBUtil.closeQuietly(pstmt);
		}
	}
	
	public static void updateQuestionCategory(Connection conn, long companyId, String categoryName, int categoryId) throws SQLException {
		PreparedStatement pstmt = null;
		
		try {
			pstmt = conn.prepareStatement("UPDATE weizhu_exam_question_category SET category_name = ? WHERE company_id = ? AND category_id = ?; ");
			pstmt.setString(1, categoryName);
			pstmt.setLong(2, companyId);
			pstmt.setInt(3, categoryId);
			
			pstmt.executeUpdate();
		} finally {
			DBUtil.closeQuietly(pstmt);
		}
	}

	public static List<UserResult> getAllUserResult(Connection conn, long companyId, int examId) throws SQLException {
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		
		try {
			pstmt = conn.prepareStatement("SELECT user_id, exam_id, start_time, submit_time, score FROM weizhu_exam_user_result WHERE company_id = ? AND exam_id = ?; ");
			pstmt.setLong(1, companyId);
			pstmt.setInt(2, examId);
			
			rs = pstmt.executeQuery();
			List<UserResult> userResultList = new ArrayList<UserResult>();
			UserResult.Builder userResultBuilder = UserResult.newBuilder();
			while (rs.next()) {
				userResultBuilder.clear();
				
				userResultBuilder.setUserId(rs.getLong("user_id"));
				userResultBuilder.setExamId(rs.getInt("exam_id"));
				userResultBuilder.setStartTime(rs.getInt("start_time"));
				userResultBuilder.setSubmitTime(rs.getInt("submit_time"));
				userResultBuilder.setScore(rs.getInt("score"));
				userResultList.add(userResultBuilder.build());
			}
			return userResultList;
		} finally {
			DBUtil.closeQuietly(rs);
			DBUtil.closeQuietly(pstmt);
		}
	}
	
	public static void updateReExamQuestion(Connection conn, long companyId, int examId, int reExamId, ExamProtos.Exam.Type type) throws SQLException {
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		
		try {
			pstmt = conn.prepareStatement("SELECT pass_mark, question_order_str FROM weizhu_exam_exam WHERE company_id = ? AND exam_id = ?; ");
			pstmt.setLong(1, companyId);
			pstmt.setInt(2, examId);
			rs = pstmt.executeQuery();
			
			int passMark = 0;
			String questionOrderStr = "";
			while (rs.next()) {
				passMark = rs.getInt("pass_mark");
				questionOrderStr = rs.getString("question_order_str");
			}
			DBUtil.closeQuietly(rs);
			rs = null;
			DBUtil.closeQuietly(pstmt);
			pstmt = null;
			
			Map<Integer, Integer> questionScoreMap;
			String questionCategoryStr = "";
			int questionNum = 0;
			switch (type) {
				case MANUAL :
					pstmt = conn.prepareStatement("SELECT question_id, score FROM weizhu_exam_exam_question WHERE company_id = ? AND exam_id = ?; ");
					pstmt.setLong(1, companyId);
					pstmt.setInt(2, examId);
					
					rs = pstmt.executeQuery();
					
					questionScoreMap = Maps.newHashMap();
					while (rs.next()) {
						int score = rs.getInt("score");
						if (!rs.wasNull()) {
							questionScoreMap.put(rs.getInt("question_id"), score);
						}
					}
					
					if (!questionScoreMap.isEmpty()) {
						pstmt = conn.prepareStatement("INSERT INTO weizhu_exam_exam_question (company_id, exam_id, question_id, score) VALUES (?, ?, ?, ?); ");
						for (Entry<Integer, Integer> entry : questionScoreMap.entrySet()) {
							pstmt.setLong(1, companyId);
							pstmt.setInt(2, reExamId);
							pstmt.setInt(3, entry.getKey());
							pstmt.setInt(4, entry.getValue());
							
							pstmt.addBatch();
						}
						pstmt.executeBatch();
						
						DBUtil.closeQuietly(pstmt);
						pstmt = null;
					}
					
					break;
				case AUTO :
					pstmt = conn.prepareStatement("SELECT question_category_str, question_num FROM weizhu_exam_exam_join_category WHERE company_id = ? AND exam_id = ?; ");
					pstmt.setLong(1, companyId);
					pstmt.setInt(2, examId);
					
					rs = pstmt.executeQuery();
					
					while (rs.next()) {
						questionCategoryStr = rs.getString("question_category_str");
						questionNum = rs.getInt("question_num");
					}
					
					if (questionNum != 0 && !questionCategoryStr.isEmpty()) {
						pstmt = conn.prepareStatement("INSERT INTO weizhu_exam_exam_join_category (company_id, exam_id, question_category_str, question_num) VALUES (?, ?, ?, ?); ");
						pstmt.setLong(1, companyId);
						pstmt.setInt(2, reExamId);
						pstmt.setString(3, questionCategoryStr);
						pstmt.setInt(4, questionNum);
						pstmt.executeUpdate();
						
						DBUtil.closeQuietly(pstmt);
						pstmt = null;
					}
					
					break;
				case AUTO_MANUAL :
					pstmt = conn.prepareStatement("SELECT question_id, score FROM weizhu_exam_exam_question WHERE company_id = ? AND exam_id = ?; ");
					pstmt.setLong(1, companyId);
					pstmt.setInt(2, examId);
					
					rs = pstmt.executeQuery();
					
					questionScoreMap = Maps.newHashMap();
					while (rs.next()) {
						int score = rs.getInt("score");
						if (!rs.wasNull()) {
							questionScoreMap.put(rs.getInt("question_id"), score);
						}
					}
					DBUtil.closeQuietly(rs);
					rs = null;
					DBUtil.closeQuietly(pstmt);
					pstmt = null;

					pstmt = conn.prepareStatement("SELECT question_category_str, question_num FROM weizhu_exam_exam_join_category WHERE company_id = ? AND exam_id = ?; ");
					pstmt.setLong(1, companyId);
					pstmt.setInt(2, examId);
					
					rs = pstmt.executeQuery();
					
					while (rs.next()) {
						questionCategoryStr = rs.getString("question_category_str");
						questionNum = rs.getInt("question_num");
					}
					
					if (questionNum != 0 && !questionCategoryStr.isEmpty()) {
						pstmt = conn.prepareStatement("INSERT INTO weizhu_exam_exam_join_category (company_id, exam_id, question_category_str, question_num) VALUES (?, ?, ?, ?); ");
						pstmt.setLong(1, companyId);
						pstmt.setInt(2, reExamId);
						pstmt.setString(3, questionCategoryStr);
						pstmt.setInt(4, questionNum);
						pstmt.executeUpdate();
						
						DBUtil.closeQuietly(pstmt);
						pstmt = null;
					}
					
					if (!questionScoreMap.isEmpty()) {
						pstmt = conn.prepareStatement("INSERT INTO weizhu_exam_exam_question (company_id, exam_id, question_id, score) VALUES (?, ?, ?, ?); ");
						for (Entry<Integer, Integer> entry : questionScoreMap.entrySet()) {
							pstmt.setLong(1, companyId);
							pstmt.setInt(2, reExamId);
							pstmt.setInt(3, entry.getKey());
							pstmt.setInt(4, entry.getValue());
							
							pstmt.addBatch();
						}
						pstmt.executeBatch();
						
						DBUtil.closeQuietly(pstmt);
						pstmt = null;
					}
					
					break;
				default :
					return;
			}
			
			DBUtil.closeQuietly(pstmt);
			pstmt = null;
			pstmt = conn.prepareStatement("UPDATE weizhu_exam_exam SET pass_mark = ?, question_order_str = ? WHERE company_id = ? AND exam_id = ?; ");
			pstmt.setInt(1, passMark);
			pstmt.setString(2, questionOrderStr);
			pstmt.setLong(3, companyId);
			pstmt.setInt(4, reExamId);
			
			pstmt.executeUpdate();

			DBUtil.closeQuietly(pstmt);
			pstmt = null;

		} finally {
			DBUtil.closeQuietly(rs);
			DBUtil.closeQuietly(pstmt);
		}
	}
	
	public static List<Integer> createExamQuestionRandom(Connection conn, long companyId, int count, Collection<Integer> questionCategoryIds) throws SQLException {
		if (questionCategoryIds.isEmpty()) {
			return Collections.emptyList();
		}
		
		Statement stmt = null;
		ResultSet rs = null;
		
		StringBuilder sql = new StringBuilder("SELECT question_id FROM weizhu_exam_question_category_join_question WHERE company_id = ").append(companyId).append(" AND category_id IN (");
		sql.append(DBUtil.COMMA_JOINER.join(questionCategoryIds));
		sql.append(") ORDER BY RAND() LIMIT ");
		sql.append(count);
		sql.append("; ");
		
		try {
			stmt = conn.createStatement();
			rs = stmt.executeQuery(sql.toString());
			
			List<Integer> questionIdList = new ArrayList<Integer>();
			
			while (rs.next()) {
				questionIdList.add(rs.getInt("question_id"));
			}
			return questionIdList;
		} finally {
			DBUtil.closeQuietly(rs);
			DBUtil.closeQuietly(stmt);
		}
	}
	
	public static List<ExamProtos.Question> batchInsertQuestion(Connection conn, long companyId, List<ExamProtos.Question> questions, long adminId, int now, int categoryId) throws SQLException {
		if (questions.isEmpty()) {
			return Collections.emptyList();
		}
		
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		conn.setAutoCommit(false);
		List<Integer> questionIdList = new ArrayList<Integer>();
		List<ExamProtos.Question> questionList = new ArrayList<ExamProtos.Question>();
		try {
			pstmt = conn.prepareStatement("INSERT INTO weizhu_exam_question (company_id, question_name, type, create_question_admin_id, create_question_time) VALUES (?, ?, ?, ?, ?); ",Statement.RETURN_GENERATED_KEYS);
			for (ExamProtos.Question question : questions) {
				pstmt.setLong(1, companyId);
				pstmt.setString(2, question.getQuestionName());
				pstmt.setString(3, question.getType().toString());
				pstmt.setLong(4, adminId);
				pstmt.setInt(5, now);
				
				pstmt.addBatch();
			}
			pstmt.executeBatch();
			rs = pstmt.getGeneratedKeys();
			
			ExamProtos.Question.Builder questionBuilder = ExamProtos.Question.newBuilder();
			int i = 0;
			while (rs.next()) {
				int questionId = rs.getInt(1);
				questionBuilder.clear();
				questionBuilder.mergeFrom(questions.get(i)).setQuestionId(rs.getInt(1));
				questionList.add(questionBuilder.build());
				
				questionIdList.add(questionId);
				i ++ ;
			}
			
			if (questionIdList.size() != questions.size()) {
				conn.rollback();
				throw new RuntimeException("has invalid question ");
			}
			
			DBUtil.closeQuietly(pstmt);
			pstmt = null;
			pstmt = conn.prepareStatement("INSERT INTO weizhu_exam_option (company_id, question_id, option_name, is_right) VALUES (?, ?, ?, ?); ");
			for (ExamProtos.Question question : questionList) {
				for (Option option : question.getOptionList()) {
					pstmt.setLong(1, companyId);
					pstmt.setInt(2, question.getQuestionId());
					pstmt.setString(3, option.getOptionName());
					pstmt.setBoolean(4, option.getIsRight());
					
					pstmt.addBatch();
				}
			}
			pstmt.executeBatch();
			
			DBUtil.closeQuietly(pstmt);
			pstmt = null;
			pstmt = conn.prepareStatement("INSERT INTO weizhu_exam_question_category_join_question (company_id, question_id, category_id) VALUES (?, ?, ?); ");
			for (int questionId : questionIdList) {
				pstmt.setLong(1, companyId);
				pstmt.setInt(2, questionId);
				pstmt.setInt(3, categoryId);
				
				pstmt.addBatch();
			}
			pstmt.executeBatch();
		} finally {
			DBUtil.closeQuietly(rs);
			DBUtil.closeQuietly(pstmt);
			conn.commit();
		}
		return questionList;
	}
	
	public static void deleteQuestionCategory(Connection conn, long companyId, Collection<Integer> questionCategoryIds) throws SQLException {
		if (questionCategoryIds.isEmpty()) {
			return ;
		}
		
		Statement stmt = null;
		
		StringBuilder sql = new StringBuilder();
		sql.append("DELETE FROM weizhu_exam_question_category WHERE company_id = ").append(companyId).append(" AND category_id IN (");
		sql.append(DBUtil.COMMA_JOINER.join(questionCategoryIds));
		sql.append("); ");
		
		try {
			stmt = conn.createStatement();
			stmt.executeUpdate(sql.toString());
		} finally {
			DBUtil.closeQuietly(stmt);
		}
	}
	
	public static void insertUserAndTeamInExam(Connection conn, long companyId, int examId, Collection<Integer> teamIds, Collection<Long> userIds) throws SQLException {
		PreparedStatement pstmt = null;
		
		try {
			if (!teamIds.isEmpty()) {
				pstmt = conn.prepareStatement("INSERT INTO weizhu_exam_exam_join_team (company_id, exam_id, team_id) VALUES (?, ?, ?); ");
				for (int teamId : teamIds) {
					pstmt.setLong(1, companyId);
					pstmt.setInt(2, examId);
					pstmt.setInt(3, teamId);
					pstmt.addBatch();
				}
				pstmt.executeBatch();
			}

			DBUtil.closeQuietly(pstmt);
			pstmt = null;

			if (!userIds.isEmpty()) {
				pstmt = conn.prepareStatement("INSERT INTO weizhu_exam_exam_join_user (company_id, exam_id, user_id) VALUES (?, ?, ?); ");
				for (long userId : userIds) {
					pstmt.setLong(1, companyId);
					pstmt.setInt(1, examId);
					pstmt.setLong(2, userId);
					pstmt.addBatch();
				}
				pstmt.executeBatch();
			}
		} finally {
			DBUtil.closeQuietly(pstmt);
		}
	}
	
	public static Map<Integer, ExamStatistics> getExamStatistics(Connection conn, long companyId, Map<Integer, Integer> passMarkMap) throws SQLException {
		if (passMarkMap.isEmpty()) {
			return Maps.newHashMap();
		}
		
		String examIdStr = DBUtil.COMMA_JOINER.join(passMarkMap.keySet());
		
		StringBuilder sql = new StringBuilder();
		sql.append("SELECT exam_id, COUNT(1) AS total_num FROM weizhu_exam_user_result WHERE company_id = ").append(companyId).append(" AND ");
		sql.append("exam_id IN (").append(examIdStr).append(") ");
		sql.append("GROUP BY exam_id; ");
		sql.append("SELECT exam_id, COUNT(1) AS take_num FROM weizhu_exam_user_result WHERE company_id = ").append(companyId).append(" AND ");
		sql.append("exam_id IN (").append(examIdStr).append(") AND ");
		sql.append("start_time IS NOT NULL ");
		sql.append("GROUP BY exam_id; ");
		sql.append("SELECT exam_id, AVG(score) AS average FROM weizhu_exam_user_result WHERE company_id = ").append(companyId).append(" AND ");
		sql.append("exam_id IN (").append(examIdStr).append(") ");
		sql.append("GROUP BY exam_id; ");
		Map<Integer, Integer> specifyExamNumMap = Maps.newHashMap();
		Map<Integer, Integer> takeExamNumMap = Maps.newHashMap();
		Map<Integer, Integer> averageMap = Maps.newHashMap();
		Statement stmt = null;
		ResultSet rs = null;
		try {
			stmt = conn.createStatement();
			stmt.execute(sql.toString());
			
			rs = stmt.getResultSet();
			while (rs.next()) {
				specifyExamNumMap.put(rs.getInt("exam_id"), rs.getInt("total_num"));
			}
			DBUtil.closeQuietly(rs);
			rs = null;
			
			stmt.getMoreResults();
			rs = stmt.getResultSet();
			while (rs.next()) {
				takeExamNumMap.put(rs.getInt("exam_id"), rs.getInt("take_num"));
			}
			
			stmt.getMoreResults();
			rs = stmt.getResultSet();
			while (rs.next()) {
				averageMap.put(rs.getInt("exam_id"), rs.getInt("average"));
			}
		} finally {
			DBUtil.closeQuietly(rs);
			DBUtil.closeQuietly(stmt);
		}
		
		stmt = null;
		rs = null;
		try {
			
		} finally {
			
		}
		
		Map<Integer, Integer> passExamNumMap = Maps.newHashMap();
		PreparedStatement pstmt = null;
		rs = null;
		try {
			for (Entry<Integer, Integer> entry : passMarkMap.entrySet()) {
				pstmt = conn.prepareStatement("SELECT exam_id, COUNT(1) AS pass_num FROM weizhu_exam_user_result WHERE company_id = ? AND exam_id = ? AND score >= ? GROUP BY exam_id; ");
				pstmt.setLong(1, companyId);
				pstmt.setInt(2, entry.getKey());
				pstmt.setInt(3, entry.getValue());
				
				pstmt.addBatch();
			}
			rs = pstmt.executeQuery();
			
			while (rs.next()) {
				passExamNumMap.put(rs.getInt("exam_id"), rs.getInt("pass_num"));
			}
		} finally {
			DBUtil.closeQuietly(rs);
			DBUtil.closeQuietly(pstmt);
		}
		
		Map<Integer, ExamStatistics> map = Maps.newHashMap();
		ExamStatistics.Builder builder = ExamStatistics.newBuilder();
		for (Entry<Integer, Integer> entry : specifyExamNumMap.entrySet()) {
			builder.clear();
			
			int examId = entry.getKey();
			StatisticalParams statisticalParam = StatisticalParams.newBuilder()
					.setTotalExamNum(specifyExamNumMap.get(examId) == null ? 0 : specifyExamNumMap.get(examId))
					.setTakeExamNum(takeExamNumMap.get(examId) == null ? 0 : takeExamNumMap.get(examId))
					.setPassExamNum(passExamNumMap.get(examId) == null ? 0 : passExamNumMap.get(examId))
					.setAverageScore(averageMap.get(examId) == null ? 0 : averageMap.get(examId))
					.build();
			
			map.put(examId, builder
					.setExamId(examId)
					.setStatisticalParams(statisticalParam)
					.build());
		}
		
		return  map;
	}

	public static DataPage<TeamStatistics> getTeamStatistics(Connection conn, long companyId, int examId, int passMark, @Nullable Integer teamId, @Nullable Integer teamLevel, int start, int length) throws SQLException {
		Statement stmt = null;
		ResultSet rs = null;
		
		// 当传入的teamId或者teamLevel为null的时候，默认按第一个不同的部门分类
		int level = 1;
		List<Integer> teamIdList = Lists.newArrayList();
		List<Integer> lastList = Lists.newArrayList();
		int cnt = 0;
		// 计算部门级次
		if (teamId == null || teamLevel == null) {
			for (int i=1; i < 8; i++) {
				StringBuilder sql = new StringBuilder();
				sql.append("SELECT DISTINCT(team_id_").append(level).append(") AS id FROM weizhu_exam_user_result WHERE company_id = ").append(companyId).append(" AND exam_id = ").append(examId).append("; ");
				
				List<Integer> list = Lists.newArrayList();
				stmt = null;
				rs = null;
				try {
					stmt = conn.createStatement();
					rs = stmt.executeQuery(sql.toString());
					
					while (rs.next()) {
						int id = rs.getInt("id");
						if (rs.wasNull()) {
							id = -1;
						}
						list.add(id);
					}
				} finally {
					DBUtil.closeQuietly(rs);
					DBUtil.closeQuietly(stmt);
				}
				
				Integer nullTeam = new Integer(-1);
				// 如果查到多个并且含有空部门 ，级次按上一个
				if (list.size() >= 1 && list.contains(nullTeam)) {
					list = lastList;
					level = (i-1) == 0 ? 1 : (i-1);
					break;
				}
				
				// 如果查到多个，并且没有空部门，按照当前级次
				if (list.size() > 1) {
					level = i;
					break;
				}
				
				lastList.clear();
				lastList.addAll(list);
				level++;
			}
			
			stmt = null;
			rs = null;
			try {
				StringBuilder sql = new StringBuilder();
				sql.append("SELECT COUNT(DISTINCT(team_id_").append(level).append(")) AS cnt FROM weizhu_exam_user_result WHERE company_id = ").append(companyId).append(" AND ");
				sql.append("exam_id = ").append(examId).append("; ");
				sql.append("SELECT ").append("team_id_").append(level).append(" AS team_id FROM weizhu_exam_user_result WHERE company_id = ").append(companyId).append(" AND ");
				sql.append("exam_id = ").append(examId).append(" GROUP BY team_id_").append(level);
				sql.append(" ORDER BY team_id_").append(level).append(" ASC LIMIT ").append(start).append(", ").append(length).append("; ");

				stmt = conn.createStatement();
				stmt.execute(sql.toString());
				rs = stmt.getResultSet();
				if (!rs.next()) {
					throw new RuntimeException("cannot get total!");
				}
				cnt = rs.getInt("cnt");
				
				rs = null;
				stmt.getMoreResults();
				rs = stmt.getResultSet();
				
				while (rs.next()) {
					int id = rs.getInt("team_id");
					if (rs.wasNull()) {
						id = -1;
						cnt++; // distinct不记录为null的行
					}
					teamIdList.add(id);
				}
			} finally {
				DBUtil.closeQuietly(rs);
				DBUtil.closeQuietly(stmt);
			}
		 } else {
			if (teamId < 0 || teamLevel < 0 || teamLevel > 8) {
				return new DataPage<TeamStatistics>(Collections.emptyList(), 0, 0);
			}

			level = teamLevel;
			StringBuilder sql = new StringBuilder();
			sql.append("SELECT COUNT(DISTINCT(team_id_").append(level).append(")) AS cnt FROM weizhu_exam_user_result WHERE company_id = ").append(companyId).append(" AND ");
			sql.append("exam_id = ").append(examId).append(" AND ");
			sql.append("team_id_").append(level - 1).append(" = ").append(teamId).append("; ");
			sql.append("SELECT ").append("team_id_").append(level).append(" AS team_id FROM weizhu_exam_user_result WHERE company_id = ").append(companyId).append(" AND ");
			sql.append("team_id_").append(level - 1).append(" = ").append(teamId).append(" AND ");
			sql.append("exam_id = ").append(examId).append(" GROUP BY team_id_").append(level);
			sql.append(" ORDER BY team_id_").append(level).append(" ASC LIMIT ").append(start).append(", ").append(length).append("; ");

			stmt = conn.createStatement();
			stmt.execute(sql.toString());
			rs = stmt.getResultSet();
			if (!rs.next()) {
				throw new RuntimeException("cannot get total!");
			}
			cnt = rs.getInt("cnt");
			
			rs = null;
			stmt.getMoreResults();
			rs = stmt.getResultSet();
			
			while (rs.next()) {
				int id = rs.getInt("team_id");
				if (rs.wasNull()) {
					id = -1;
				}
				teamIdList.add(id);
			}
		}
		
		if (teamIdList.isEmpty()) {
			return new DataPage<TeamStatistics>(Collections.emptyList(), 0, 0);
		}
		
		StringBuilder teamIdStr = new StringBuilder();
		Integer nullPosition = new Integer(-1); // -1用来表示null的positionId
		if (teamIdList.contains(nullPosition)) {
			teamIdList.remove(nullPosition);
			teamIdStr.append("team_id_").append(level).append(" IS NULL ");
			if (!teamIdList.isEmpty()) {
				teamIdStr.append("OR team_id_").append(level).append(" IN (").append(DBUtil.COMMA_JOINER.join(teamIdList)).append(")) ");
				teamIdStr.insert(0, "("); // 在开始的未知添加括号
			}
		} else {
			teamIdStr.append("team_id_").append(level).append(" IN (").append(DBUtil.COMMA_JOINER.join(teamIdList)).append(") ");
		}

		stmt = null;
		rs = null;
		Map<Integer, Integer> averageMap = Maps.newHashMap();
		Map<Integer, Integer> totalNumMap = Maps.newHashMap();
		Map<Integer, Integer> takeNumMap = Maps.newHashMap();
		Map<Integer, Integer> passNumMap = Maps.newHashMap();
		try {
			StringBuilder sqlBuilder = new StringBuilder();
			sqlBuilder.append("SELECT team_id_").append(level).append(" AS team_id, AVG(score) AS score FROM weizhu_exam_user_result WHERE company_id = ").append(companyId).append(" AND ");
			sqlBuilder.append("exam_id = ").append(examId).append(" AND ");
			sqlBuilder.append(teamIdStr).append(" GROUP BY team_id_").append(level).append("; ");
			sqlBuilder.append("SELECT team_id_").append(level).append(" AS team_id, COUNT(1) AS total_num FROM weizhu_exam_user_result WHERE company_id = ").append(companyId).append(" AND ");
			sqlBuilder.append("exam_id = ").append(examId).append(" AND ");
			sqlBuilder.append(teamIdStr).append(" GROUP BY team_id_").append(level).append("; ");
			sqlBuilder.append("SELECT team_id_").append(level).append(" AS team_id, COUNT(1) AS take_num FROM weizhu_exam_user_result WHERE company_id = ").append(companyId).append(" AND ");
			sqlBuilder.append("exam_id = ").append(examId).append(" AND ");
			sqlBuilder.append("start_time IS NOT NULL").append(" AND ");
			sqlBuilder.append(teamIdStr).append(" GROUP BY team_id_").append(level).append("; ");
			sqlBuilder.append("SELECT team_id_").append(level).append(" AS team_id, COUNT(1) AS pass_num FROM weizhu_exam_user_result WHERE company_id = ").append(companyId).append(" AND ");
			sqlBuilder.append("exam_id = ").append(examId).append(" AND ");
			sqlBuilder.append("score >= ").append(passMark).append(" AND ");
			sqlBuilder.append(teamIdStr).append(" GROUP BY team_id_").append(level).append("; ");
			stmt = conn.createStatement();
			
			String sql = sqlBuilder.toString();

			stmt.executeQuery(sql);
			rs = stmt.getResultSet();
			while (rs.next()) {
				int id = rs.getInt("team_id");
				if (rs.wasNull()) {
					id = -1;
				}
				averageMap.put(id, rs.getInt("score"));
			}
			
			rs = null;
			stmt.getMoreResults();
			rs = stmt.getResultSet();
			while (rs.next()) {
				int id = rs.getInt("team_id");
				if (rs.wasNull()) {
					id = -1;
				}
				totalNumMap.put(id, rs.getInt("total_num"));
			}
			
			rs = null;
			stmt.getMoreResults();
			rs = stmt.getResultSet();
			while (rs.next()) {
				int id = rs.getInt("team_id");
				if (rs.wasNull()) {
					id = -1;
				}
				takeNumMap.put(id, rs.getInt("take_num"));
			}
			
			rs = null;
			stmt.getMoreResults();
			rs = stmt.getResultSet();
			while (rs.next()) {
				int id = rs.getInt("team_id");
				if (rs.wasNull()) {
					id = -1;
				}
				passNumMap.put(id, rs.getInt("pass_num"));
			}
		} finally {
			DBUtil.closeQuietly(rs);
			DBUtil.closeQuietly(stmt);
		}
		
		List<String> teamNameList = Lists.newArrayList();
		int j = 1;
		while (teamNameList.size() < level) {
			teamNameList.add("team_id_" + j);
			j++;
		}
		
		String teamNameStr = DBUtil.SQL_STRING_ESCAPER.escape(DBUtil.COMMA_JOINER.join(teamNameList));
		
		Map<Integer, List<Integer>> teamIdMap = Maps.newHashMap();
		stmt = null;
		rs = null;
		try {
			StringBuilder sql = new StringBuilder();
			sql.append("SELECT ").append(teamNameStr).append(" FROM weizhu_exam_user_result WHERE company_id = ").append(companyId).append(" AND ");
			sql.append(teamIdStr).append(" AND ");
			sql.append("exam_id = ").append(examId).append("; ");
			stmt = conn.createStatement();
			rs = stmt.executeQuery(sql.toString());
			
			while (rs.next()) {
				List<Integer> idList = Lists.newArrayList();
				int lvl = 1;
				while (idList.size() < level) {
					idList.add(rs.getInt("team_id_" + lvl));
					lvl++;
				}
				
				Integer id = rs.getInt("team_id_" + level);
				if (rs.wasNull()) {
					id = -1;
				}
				teamIdMap.put(id, idList);
			}
		} finally {
			DBUtil.closeQuietly(rs);
			DBUtil.closeQuietly(stmt);
		}
		
		List<TeamStatistics> teamStatisticsList = Lists.newArrayList();
		TeamStatistics.Builder builder = TeamStatistics.newBuilder();
		for (Entry<Integer, Integer> entry : totalNumMap.entrySet()) {
			builder.clear();
			
			int id = entry.getKey();
			int totalNum = entry.getValue();
			
			List<Integer> list = teamIdMap.get(id);
			if (list == null) {
				list = Collections.emptyList();
			}
			Integer takeNum = takeNumMap.get(id);
			if (takeNum == null) {
				takeNum = 0;
			}
			Integer passNum = passNumMap.get(id);
			if (passNum == null) {
				passNum = 0;
			}
			Integer average = averageMap.get(id);
			if (average == null) {
				average = 0;
			}
			
			teamStatisticsList.add(
				builder
					.addAllTeamId(list)
					.setStatisticalParams(
						StatisticalParams.newBuilder()
							.setTotalExamNum(totalNum)
							.setTakeExamNum(takeNum)
							.setPassExamNum(passNum)
							.setAverageScore(average)
							.build()
						).build()
					);
		}
		
		return new DataPage<TeamStatistics>(teamStatisticsList, cnt, cnt);
	}
	
	public static DataPage<PositionStatistics> getPositionStatistics(Connection conn, long companyId, int examId, int passMark, int start, int length) throws SQLException {
		if (length == 0) {
			return new DataPage<PositionStatistics>(Collections.emptyList(), 0, 0);
		}
		
		List<Integer> positionIdList = Lists.newArrayList();
		int cnt = 0;
		
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try {
			pstmt = conn.prepareStatement("SELECT COUNT(DISTINCT(position_id)) AS cnt FROM weizhu_exam_user_result WHERE company_id = ? AND exam_id = ?; "
					+ "SELECT DISTINCT(position_id) FROM weizhu_exam_user_result WHERE company_id = ? AND exam_id = ? ORDER BY position_id LIMIT ?, ?; ");
			pstmt.setLong(1, companyId);
			pstmt.setInt(2, examId);
			pstmt.setLong(3, companyId);
			pstmt.setInt(4, examId);
			pstmt.setInt(5, start);
			pstmt.setInt(6, length);
			
			pstmt.execute();
			rs = pstmt.getResultSet();
			if (!rs.next()) {
				throw new RuntimeException("cannot get total");
			}
			cnt = rs.getInt("cnt");
			DBUtil.closeQuietly(rs);
			rs = null;

			pstmt.getMoreResults();
			rs = pstmt.getResultSet();
			while (rs.next()) {
				int positionId = rs.getInt("position_id");
				if (rs.wasNull()) {
					positionId = -1;
					cnt++; // distinct 不记录为null的行
				}
				positionIdList.add(positionId);
			}
		} finally {
			DBUtil.closeQuietly(rs);
			DBUtil.closeQuietly(pstmt);
		}
		
		if (positionIdList.isEmpty()) {
			return new DataPage<PositionStatistics>(Collections.emptyList(), 0, 0);
		}
		
		StringBuilder positionIdStr = new StringBuilder();
		Integer nullPosition = new Integer(-1); // -1用来表示null的positionId
		if (positionIdList.contains(nullPosition)) {
			positionIdList.remove(nullPosition);
			positionIdStr.append("position_id IS NULL ");
			if (!positionIdList.isEmpty()) {
				positionIdStr.append("OR position_id IN (").append(DBUtil.COMMA_JOINER.join(positionIdList)).append(")) ");
				positionIdStr.insert(0, "("); // 在开始的未知添加括号
			}
		} else {
			positionIdStr.append("position_id IN (").append(DBUtil.COMMA_JOINER.join(positionIdList)).append(") ");
		}

		Map<Integer, Integer> totalNumMap = Maps.newHashMap();
		Map<Integer, Integer> takeNumMap = Maps.newHashMap();
		Map<Integer, Integer> passNumMap = Maps.newHashMap();
		Map<Integer, Integer> averageMap = Maps.newHashMap();
		
		Statement stmt = null;
		rs = null;
		try {
			StringBuilder sqlBuilder = new StringBuilder();
			sqlBuilder.append("SELECT position_id, COUNT(1) AS total_num FROM weizhu_exam_user_result WHERE company_id = ").append(companyId).append(" AND ");
			sqlBuilder.append("exam_id = ").append(examId).append(" AND ");
			sqlBuilder.append(positionIdStr).append("GROUP BY position_id; ");
			sqlBuilder.append("SELECT position_id, COUNT(1) AS take_num FROM weizhu_exam_user_result WHERE company_id = ").append(companyId).append(" AND ");
			sqlBuilder.append("exam_id = ").append(examId).append(" AND ");
			sqlBuilder.append("start_time IS NOT NULL AND ");
			sqlBuilder.append(positionIdStr).append("GROUP BY position_id; ");
			sqlBuilder.append("SELECT position_id, COUNT(1) AS pass_num FROM weizhu_exam_user_result WHERE company_id = ").append(companyId).append(" AND ");
			sqlBuilder.append("exam_id = ").append(examId).append(" AND ");
			sqlBuilder.append("score >= ").append(passMark).append(" AND ");
			sqlBuilder.append(positionIdStr).append("GROUP BY position_id; ");
			sqlBuilder.append("SELECT position_id, AVG(score) AS average FROM weizhu_exam_user_result WHERE company_id = ").append(companyId).append(" AND ");
			sqlBuilder.append("exam_id = ").append(examId).append(" AND ");
			sqlBuilder.append(positionIdStr).append("GROUP BY position_id; ");

			String sql = sqlBuilder.toString();

			stmt = conn.createStatement();
			stmt.execute(sql);
			rs = stmt.getResultSet();
			while (rs.next()) {
				int positionId = rs.getInt("position_id");
				if (rs.wasNull()) {
					positionId = -1;
				}
				totalNumMap.put(positionId, rs.getInt("total_num"));
			}
			
			DBUtil.closeQuietly(rs);
			rs = null;
			stmt.getMoreResults();
			rs = stmt.getResultSet();
			while (rs.next()) {
				int positionId = rs.getInt("position_id");
				if (rs.wasNull()) {
					positionId = -1;
				}
				takeNumMap.put(positionId, rs.getInt("take_num"));
			}
			
			DBUtil.closeQuietly(rs);
			rs = null;
			stmt.getMoreResults();
			rs = stmt.getResultSet();
			while (rs.next()) {
				int positionId = rs.getInt("position_id");
				if (rs.wasNull()) {
					positionId = -1;
				}
				passNumMap.put(positionId, rs.getInt("pass_num"));
			}
			
			DBUtil.closeQuietly(rs);
			rs = null;
			stmt.getMoreResults();
			rs = stmt.getResultSet();
			while (rs.next()) {
				int positionId = rs.getInt("position_id");
				if (rs.wasNull()) {
					positionId = -1;
				}
				averageMap.put(positionId, rs.getInt("average"));
			}
		} finally {
			DBUtil.closeQuietly(rs);
			DBUtil.closeQuietly(stmt);
		}
		
		List<PositionStatistics> list = Lists.newArrayList();
		PositionStatistics.Builder builder = PositionStatistics.newBuilder();
		for (Entry<Integer, Integer> entry : totalNumMap.entrySet()) {
			builder.clear();
			
			int positionId = entry.getKey();
			int totalNum = entry.getValue();
			
			Integer takeNum = takeNumMap.get(positionId);
			if (takeNum == null) {
				takeNum = 0;
			}
			Integer passNum = passNumMap.get(positionId);
			if (passNum == null) {
				passNum = 0;
			}
			Integer average = averageMap.get(positionId);
			if (average == null) {
				average = 0;
			}
			
			StatisticalParams statisticsParam = StatisticalParams.newBuilder()
					.setTotalExamNum(totalNum)
					.setTakeExamNum(takeNum)
					.setPassExamNum(passNum)
					.setAverageScore(average)
					.build();
			
			list.add(builder
						.setStatisticalParams(statisticsParam)
						.setPositionId(positionId)
					.build()
					);
		}
		
		return new DataPage<PositionStatistics>(list, cnt, cnt);
	}
	
	public static Map<Long, Map<Integer, Set<Integer>>> getUserAnswerMap(Connection conn, long companyId, int examId) throws SQLException {
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		
		try {
			pstmt = conn.prepareStatement("SELECT * FROM weizhu_exam_user_answer WHERE company_id = ? AND exam_id = ?; ");
			pstmt.setLong(1, companyId);
			pstmt.setInt(2, examId);
			rs = pstmt.executeQuery();
			Map<Long, Map<Integer, Set<Integer>>> userAnswerMap = Maps.newHashMap();
			while (rs.next()) {
				long userId = rs.getLong("user_id");
				Map<Integer, Set<Integer>> answerMap = userAnswerMap.get(userId);
				if (answerMap == null) {
					answerMap = Maps.newHashMap();
				}
				int questionId = rs.getInt("question_id");
				Set<Integer> optionIdSet = answerMap.get(questionId);
				if (optionIdSet == null) {
					optionIdSet = Sets.newTreeSet();
				}
				int optionId = rs.getInt("answer_option_id");
				optionIdSet.add(optionId);
				answerMap.put(questionId, optionIdSet);
				userAnswerMap.put(userId, answerMap);
			}
			return userAnswerMap;
		} finally {
			DBUtil.closeQuietly(rs);
			DBUtil.closeQuietly(pstmt);
		}
	}
	
	public static Set<Long> getTakeExamUserId(Connection conn, long companyId, int examId) throws SQLException {
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try {
			pstmt = conn.prepareStatement("SELECT user_id FROM weizhu_exam_user_result WHERE company_id = ? AND exam_id = ?; ");
			pstmt.setLong(1, companyId);
			pstmt.setInt(2, examId);
			
			rs = pstmt.executeQuery();
			Set<Long> userIdSet = Sets.newTreeSet();
			while (rs.next()) {
				userIdSet.add(rs.getLong("user_id"));
			}
			
			return userIdSet;
		} finally {
			DBUtil.closeQuietly(rs);
			DBUtil.closeQuietly(pstmt);
		}
	}
	
	public static void batchInsertExamUser(Connection conn, long companyId, int examId, Map<UserProtos.User, List<Integer>> userTeamMap) throws SQLException {
		if (userTeamMap.isEmpty()) {
			return;
		}
		
		// 100个一条sql
		String sqlHead = "INSERT INTO weizhu_exam_user_result (company_id, user_id, exam_id, score, team_id_1, team_id_2, team_id_3, team_id_4, team_id_5, team_id_6, team_id_7, team_id_8, position_id, level_id) VALUES ";
		
		StringBuilder tmpSQL = null;
		StringBuilder sql = new StringBuilder();
		int i = 1;
		for (Entry<UserProtos.User, List<Integer>> entry : userTeamMap.entrySet()) {
			User user = entry.getKey();
			List<Integer> list = entry.getValue();
			if (tmpSQL == null) {
				tmpSQL = new StringBuilder(sqlHead);
			} else {
				tmpSQL.append(", ");
			}
			
			tmpSQL.append("(");
			tmpSQL.append(companyId).append(", ");
			tmpSQL.append(user.getBase().getUserId()).append(", ");
			tmpSQL.append(examId).append(", ");
			tmpSQL.append(0).append(", ");
			tmpSQL.append(list.get(0)).append(", ");
			tmpSQL.append(list.get(1)).append(", ");
			tmpSQL.append(list.get(2)).append(", ");
			tmpSQL.append(list.get(3)).append(", ");
			tmpSQL.append(list.get(4)).append(", ");
			tmpSQL.append(list.get(5)).append(", ");
			tmpSQL.append(list.get(6)).append(", ");
			tmpSQL.append(list.get(7)).append(", ");
			tmpSQL.append(user.getTeam(0).getPositionId()).append(", ");
			tmpSQL.append(user.getBase().getLevelId());
			tmpSQL.append(")");
			if (i%100 == 0 && userTeamMap.size()%100 != 0) {
				tmpSQL.append("; ");
				sql.append(tmpSQL.toString());
				tmpSQL = null;
			}
			
			i++;
		}
		if (tmpSQL != null) {
			tmpSQL.append("; ");
			sql.append(tmpSQL.toString());
		}
		
		Statement stmt = null;
		try {
			stmt = conn.createStatement();
			stmt.executeUpdate(tmpSQL.toString());
		} finally {
			DBUtil.closeQuietly(stmt);
		}
	}
	
	public static void setIsLoadAllUser(Connection conn, long companyId, int examId) throws SQLException {
		PreparedStatement pstmt = null;
		try {
			pstmt = conn.prepareStatement("UPDATE weizhu_exam_exam SET is_load_all_user = 1 WHERE company_id = ? AND exam_id = ?; ");
			pstmt.setLong(1, companyId);
			pstmt.setInt(2, examId);
			pstmt.executeUpdate();
		} finally {
			DBUtil.closeQuietly(pstmt);
		}
	}
	
	public static void saveUserTeam(Connection conn, long companyId, 
			int examId, Set<Long> userIdSet, Collection<Long> takeExamUserIdSet,
			Map<Long, List<Integer>> userTeamMap, Map<Long, Integer> userPositionMap, Map<Long, Integer> userLevelMap
			) throws SQLException {
		if (userIdSet.isEmpty()) {
			return;
		}
		
		PreparedStatement pstmt = null;
		try {
			// 参加考试的用户
			pstmt = conn.prepareStatement("UPDATE weizhu_exam_user_result SET team_id_1 = ?, team_id_2 = ?, team_id_3 = ?, team_id_4 = ?, team_id_5 = ?, team_id_6 = ?, team_id_7 = ?, team_id_8 = ?, "
					+ "position_id = ?, level_id = ? "
					+ "WHERE company_id = ? AND exam_id = ? AND user_id = ?; ");
			for (Long userId : userIdSet) {
				if (takeExamUserIdSet.contains(userId)) {
					List<Integer> teamIdList = userTeamMap.get(userId);
					if (teamIdList == null) {
						teamIdList = Collections.emptyList();
					}
					
					for (int i=0; i<8; i++) {
						DBUtil.set(pstmt, i + 1, i<teamIdList.size() ? teamIdList.get(i) : null);
					}
					DBUtil.set(pstmt, 9, userPositionMap.get(userId));
					DBUtil.set(pstmt, 10, userLevelMap.get(userId));
					DBUtil.set(pstmt, 11, companyId);
					DBUtil.set(pstmt, 12, examId);
					DBUtil.set(pstmt, 13, userId);
					pstmt.addBatch();
				}
			}
			pstmt.executeBatch();
		} finally {
			DBUtil.closeQuietly(pstmt);
		}
		
		StringBuilder insertSqlBuilder = null; // 没参加考试用户
		for (Long userId : userIdSet) {
			if (!takeExamUserIdSet.contains(userId)) {
				if (insertSqlBuilder == null) {
					insertSqlBuilder = new StringBuilder();
					insertSqlBuilder.append("INSERT INTO weizhu_exam_user_result (company_id, user_id, exam_id, start_time, submit_time, score,"
							+ " team_id_1, team_id_2, team_id_3, team_id_4, team_id_5, team_id_6, team_id_7, team_id_8, position_id, level_id) VALUES ");
				} else {
					insertSqlBuilder.append(", ");
				}
				
				insertSqlBuilder.append("(");
				insertSqlBuilder.append(companyId).append(", ");
				insertSqlBuilder.append(userId).append(", ");
				insertSqlBuilder.append(examId).append(", ");
				insertSqlBuilder.append("NULL, ");
				insertSqlBuilder.append("NULL, ");
				insertSqlBuilder.append("0, ");
				
				List<Integer> teamIdList = userTeamMap.get(userId);
				if (teamIdList == null) {
					teamIdList = Collections.emptyList();
				}
				for (int i=0; i<8; i++) {
					insertSqlBuilder.append(i<teamIdList.size() ? teamIdList.get(i) : "NULL").append(", ");
				}
				insertSqlBuilder.append(userPositionMap.get(userId) == null ? "NULL" : userPositionMap.get(userId)).append(", ");
				insertSqlBuilder.append(userLevelMap.get(userId) == null ? "NULL" : userLevelMap.get(userId)).append(") ");
			}
		}
		
		if (insertSqlBuilder != null) {
			insertSqlBuilder.append("; ");
			final String insertSql = insertSqlBuilder.toString();
			
			Statement stmt = null;
			try {
				stmt = conn.createStatement();
				stmt.executeUpdate(insertSql);
			} finally {
				DBUtil.closeQuietly(stmt);
			}
		}
	}
	
}