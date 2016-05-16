package com.weizhu.service.survey;

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
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.annotation.Nullable;

import com.weizhu.common.db.DBUtil;
import com.weizhu.common.db.ProtobufMapper;
import com.weizhu.common.utils.DataPage;
import com.weizhu.proto.SurveyProtos;

public class SurveyDB {
	
	private static final ProtobufMapper<SurveyProtos.Survey> SURVEY_MAPPER = 
			ProtobufMapper.createMapper(SurveyProtos.Survey.getDefaultInstance(), 
					"survey_id",
					"survey_name",
					"survey_desc",
					"image_name",
					"start_time",
					"end_time",
					"show_result_type",
					"allow_model_id",
					"state",
					"create_time",
					"create_admin_id",
					"update_time",
					"update_admin_id"
					);
	
	private static final ProtobufMapper<SurveyProtos.Question> SURVEY_QUESTION_MAPPER = 
			ProtobufMapper.createMapper(SurveyProtos.Question.getDefaultInstance(), 
					"question_id", 
				    "question_name", 
				    "image_name",
				    "is_optional",
				    "state",
				    "create_time",
				    "create_admin_id",
				    "update_time",
				    "update_admin_id"
				    );
	
	private static final ProtobufMapper<SurveyProtos.Vote.Option> VOTE_OPTION_MAPPER = 
			ProtobufMapper.createMapper(SurveyProtos.Vote.Option.getDefaultInstance(), 
					"option_id",
					"option_name",
					"image_name"
					);
	
	private static final ProtobufMapper<SurveyProtos.InputSelect.Option> INPUT_SELECT_OPTION_MAPPER =
			ProtobufMapper.createMapper(SurveyProtos.InputSelect.Option.getDefaultInstance(), 
					"option_id",
					"option_name"
					);
	
	/**
	 * @param conn
	 * @param surveyIds
	 * @return
	 * @throws SQLException
	 */
	public static Map<Integer, SurveyProtos.Survey> getSurveyById(Connection conn, long companyId, Collection<Integer> surveyIds) throws SQLException {
		Statement stmt = null;
		ResultSet rs = null;
		
		StringBuilder sql = new StringBuilder("SELECT * FROM weizhu_survey WHERE company_id = ").append(companyId).append(" AND survey_id IN (");
		sql.append(DBUtil.COMMA_JOINER.join(surveyIds)).append("); ");
		
		try {
			stmt = conn.createStatement();
			rs = stmt.executeQuery(sql.toString());
			
			Map<Integer, SurveyProtos.Survey> surveyMap = new HashMap<Integer, SurveyProtos.Survey>();
			SurveyProtos.Survey.Builder surveyBuilder = SurveyProtos.Survey.newBuilder();
			while (rs.next()) {
				surveyBuilder.clear();
				
				SURVEY_MAPPER.mapToItem(rs, surveyBuilder);
				
				int surveyId = rs.getInt("survey_id");
				
				surveyMap.put(surveyId, surveyBuilder.build());
			}
			
			return surveyMap; 
		} finally {
			DBUtil.closeQuietly(rs);
			DBUtil.closeQuietly(stmt);
		}
	}
	
	/**
	 * @param conn
	 * @param questionIds
	 * @return
	 * @throws SQLException
	 */
	public static Map<Integer, SurveyProtos.Question> getQuestionById(Connection conn, long companyId, Collection<Integer> questionIds) throws SQLException {
		if (questionIds.isEmpty()) {
			return Collections.emptyMap();
		}
		
		Statement stmt = null;
		ResultSet rs = null;
		
		try {
			String questionIdStr = DBUtil.COMMA_JOINER.join(questionIds);
			
			StringBuilder sql = new StringBuilder();
			sql.append("SELECT * FROM weizhu_survey_question WHERE company_id = ").append(companyId).append(" AND state = 'NORMAL' AND question_id IN (").append(questionIdStr).append("); ");

			stmt = conn.createStatement();
			rs = stmt.executeQuery(sql.toString());
			
			List<Integer> voteQuestionIdList = new ArrayList<Integer>();
			List<Integer> inputSelectQuestionIdList = new ArrayList<Integer>();
			List<Integer> inputTextQuestionIdList = new ArrayList<Integer>();
			
			Map<Integer, SurveyProtos.Question> questionMap = new HashMap<Integer, SurveyProtos.Question>();
			SurveyProtos.Question.Builder questionBuilder = SurveyProtos.Question.newBuilder();
			while (rs.next()) {
				int questionId = rs.getInt("question_id");

				if (rs.getString("type").equals(SurveyProtos.Question.TypeCase.VOTE.name())) {
					voteQuestionIdList.add(questionId);
				} else if (rs.getString("type").equals(SurveyProtos.Question.TypeCase.INPUT_SELECT.name())) {
					inputSelectQuestionIdList.add(questionId);
				} else if (rs.getString("type").equals(SurveyProtos.Question.TypeCase.INPUT_TEXT.name())) {
					inputTextQuestionIdList.add(questionId);
				}
				
				questionBuilder.clear();
				
				SURVEY_QUESTION_MAPPER.mapToItem(rs, questionBuilder);
				
				questionMap.put(questionId, questionBuilder.build());
			}
			DBUtil.closeQuietly(rs);
			rs = null;
			DBUtil.closeQuietly(stmt);
			stmt = null;
			
			if (!voteQuestionIdList.isEmpty()) {
				StringBuilder voteSQL = new StringBuilder();
				voteSQL.append("SELECT * FROM weizhu_survey_vote_question WHERE company_id = ").append(companyId).append(" AND question_id IN (").append(DBUtil.COMMA_JOINER.join(voteQuestionIdList)).append("); ");
				voteSQL.append("SELECT * FROM weizhu_survey_vote_option WHERE company_id = ").append(companyId).append(" AND question_id IN (").append(DBUtil.COMMA_JOINER.join(voteQuestionIdList)).append("); ");
				
				stmt = conn.createStatement();
				stmt.execute(voteSQL.toString());
				
				rs = stmt.getResultSet();
				
				Map<Integer, Integer> voteCheckNum = new HashMap<Integer, Integer>();
				while (rs.next()) {
					voteCheckNum.put(rs.getInt("question_id"), rs.getInt("check_number"));
				}
				
				rs = null;
				stmt.getMoreResults();
				rs = stmt.getResultSet();

				SurveyProtos.Vote.Option.Builder voteOptionBuilder = SurveyProtos.Vote.Option.newBuilder();
				while (rs.next()) {
					voteOptionBuilder.clear();
					
					VOTE_OPTION_MAPPER.mapToItem(rs, voteOptionBuilder);
					
					int questionId = rs.getInt("question_id");
					SurveyProtos.Question question = questionMap.get(questionId);
					Integer checkNum = voteCheckNum.get(questionId);
					if (question != null && checkNum != null) {
						
						SurveyProtos.Vote.Question voteQuestion = SurveyProtos.Vote.Question.newBuilder()
								.mergeFrom(question.getVote())
								.setCheckNum(checkNum)
								.addOption(voteOptionBuilder.build())
								.build();
						questionBuilder.mergeFrom(question).setVote(voteQuestion);
						questionMap.put(questionId, questionBuilder.build());
					}
				}
				
				DBUtil.closeQuietly(rs);
				rs = null;
				DBUtil.closeQuietly(stmt);
				stmt = null;
			}
			
			if (!inputSelectQuestionIdList.isEmpty()) {
				StringBuilder inputSelectSQL = new StringBuilder();
				inputSelectSQL.append("SELECT * FROM weizhu_survey_input_select_option WHERE company_id = ").append(companyId).append(" AND question_id IN (").append(DBUtil.COMMA_JOINER.join(inputSelectQuestionIdList)).append("); ");
				
				stmt = conn.createStatement();
				rs = stmt.executeQuery(inputSelectSQL.toString());
				
				SurveyProtos.InputSelect.Option.Builder inputOptionBuilder = SurveyProtos.InputSelect.Option.newBuilder();
				while (rs.next()) {
					inputOptionBuilder.clear();
					
					INPUT_SELECT_OPTION_MAPPER.mapToItem(rs, inputOptionBuilder);
					
					int questionId = rs.getInt("question_id");
					SurveyProtos.Question question = questionMap.get(questionId);
					if (question != null) {
						
						SurveyProtos.InputSelect.Question inputQuestion = SurveyProtos.InputSelect.Question.newBuilder()
								.mergeFrom(question.getInputSelect())
								.addOption(inputOptionBuilder.build())
								.build();
						questionBuilder.mergeFrom(question).setInputSelect(inputQuestion);
						questionMap.put(questionId, questionBuilder.build());
					}
				}
				
				DBUtil.closeQuietly(rs);
				rs = null;
				DBUtil.closeQuietly(stmt);
				stmt = null;
			}
			
			if (!inputTextQuestionIdList.isEmpty()) {
				StringBuilder inputTextSelectSQL = new StringBuilder();
				inputTextSelectSQL.append("SELECT * FROM weizhu_survey_input_text_question WHERE company_id = ").append(companyId).append(" AND question_id IN (").append(DBUtil.COMMA_JOINER.join(inputTextQuestionIdList)).append("); ");
				
				stmt = conn.createStatement();
				rs = stmt.executeQuery(inputTextSelectSQL.toString());
				
				while (rs.next()) {
					String inputPrompt = rs.getString("input_prompt");
					
					int questionId = rs.getInt("question_id");
					SurveyProtos.Question question = questionMap.get(questionId);
					if (question != null) {
						
						SurveyProtos.InputText.Question inputQuestion = SurveyProtos.InputText.Question.newBuilder()
								.mergeFrom(question.getInputText())
								.setInputPrompt(inputPrompt)
								.build();
						questionBuilder.mergeFrom(question).setInputText(inputQuestion);
						questionMap.put(questionId, questionBuilder.build());
					}
				}
			}
			
			return questionMap;
		} finally {
			DBUtil.closeQuietly(rs);
			DBUtil.closeQuietly(stmt);
		}
	}

	/**
	 * 获取没有结束的调研id列表
	 * @param conn
	 * @param now          当前时间
	 * @param lastSurveyId 上一页最后一个调研id（初始为null）
	 * @param lastEndTime  上一页最后一个调研结束时间（初始为null）
	 * @return 调研id list
	 * @throws SQLException 
	 */
	public static List<Integer> getOpenSurveyId(Connection conn, long companyId, int now, @Nullable Integer lastSurveyId, @Nullable Integer lastEndTime) throws SQLException {
		PreparedStatement pstmt = null;
		ResultSet rs = null;

		try {
			if (lastEndTime == null || lastSurveyId == null) {
				pstmt = conn.prepareStatement("SELECT survey_id FROM weizhu_survey WHERE company_id = ? AND state = 'NORMAL' AND end_time > ? ORDER BY end_time ASC, survey_id ASC; ");
				pstmt.setLong(1, companyId);
				pstmt.setInt(2, now);
			} else {
				pstmt = conn.prepareStatement("SELECT survey_id FROM weizhu_survey WHERE company_id = ? AND state = 'NORMAL' AND end_time > ? AND ((end_time = ? AND survey_id > ?) OR end_time > ?) ORDER BY end_time ASC, survey_id ASC; ");
				pstmt.setLong(1, companyId);
				pstmt.setInt(2, now);
				pstmt.setInt(3, lastEndTime);
				pstmt.setInt(4, lastSurveyId);
				pstmt.setInt(5, lastEndTime);
			}
			rs = pstmt.executeQuery();
			
			List<Integer> surveyIdList = new ArrayList<Integer>();
			while (rs.next()) {
				surveyIdList.add(rs.getInt("survey_id"));
			}
			
			return surveyIdList;
		} finally {
			DBUtil.closeQuietly(rs);
			DBUtil.closeQuietly(pstmt);
		}
	}
	
	/**
	 * 获取已经结束的调研id列表
	 * @param conn
	 * @param now          当前时间
	 * @param lastSurveyId 上一页最后一个调研id（初始为null）
	 * @param lastEndtime  上一页最后一个调研结束时间（初始为null）
	 * @return
	 * @throws SQLException 
	 */
	public static List<Integer> getClosedSurveyId(Connection conn, long companyId, int now, @Nullable Integer lastSurveyId, @Nullable Integer lastEndTime) throws SQLException {
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		
		try {
			if (lastSurveyId == null || lastEndTime == null) {
				pstmt = conn.prepareStatement("SELECT survey_id FROM weizhu_survey WHERE company_id = ? AND state = 'NORMAL' AND end_time < ? ORDER BY end_time DESC, survey_id DESC; ");
				pstmt.setLong(1, companyId);
				pstmt.setInt(2, now);
			} else {
				pstmt = conn.prepareStatement("SELECT survey_id FROM weizhu_survey WHERE company_id = ? AND state = 'NORMAL' AND end_time < ? AND ((end_time = ? AND survey_id > ?) OR end_time < ? ) ORDER BY end_time DESC, survey_id ASC; ");
				pstmt.setLong(1, companyId);
				pstmt.setInt(2, now);
				pstmt.setInt(3, lastEndTime);
				pstmt.setInt(4, lastSurveyId);
				pstmt.setInt(5, lastEndTime);
			}
			rs = pstmt.executeQuery();
			
			List<Integer> surveyIdList = new ArrayList<Integer>();
			while (rs.next()) {
				surveyIdList.add(rs.getInt("survey_id"));
			}
			
			return surveyIdList;
		} finally {
			DBUtil.closeQuietly(rs);
			DBUtil.closeQuietly(pstmt);
		}
	}
	
	/**
	 * 根据调研id获取对应的问题id
	 * @param conn     
	 * @param surveyId 调研id
	 * @return
	 * @throws SQLException 
	 */
	public static List<Integer> getQuestionIdBySurveyId(Connection conn, long companyId, int surveyId) throws SQLException {
		Statement stmt = null;
		ResultSet rs = null;
		
		try {
			StringBuilder sql = new StringBuilder();
			sql.append("SELECT question_id FROM weizhu_survey_join_question WHERE company_id = ").append(companyId);
			sql.append(" AND survey_id = ").append(surveyId).append("; ");
			sql.append("SELECT question_order_str FROM weizhu_survey WHERE company_id = ").append(companyId).append(" AND survey_id = ").append(surveyId).append("; ");
			
			stmt = conn.createStatement();
			stmt.execute(sql.toString());
			rs = stmt.getResultSet();
			
			List<Integer> questionIdList = new ArrayList<Integer>();
			while (rs.next()) {
				questionIdList.add(rs.getInt("question_id"));
			}
			DBUtil.closeQuietly(rs);
			rs = null;
			
			stmt.getMoreResults();
			rs = stmt.getResultSet();
			
			String questionOrderStr = null;
			while (rs.next()) {
				questionOrderStr = rs.getString("question_order_str");
			}
			
			if (questionOrderStr == null) {
				return questionIdList;
			}
			
			// 按照questionOrder顺序返回
			List<Integer> questionIdOrderList = new ArrayList<Integer>();
			
			// 按照questionOrder排列questionId
			List<String> questionIdStrList = DBUtil.COMMA_SPLITTER.splitToList(questionOrderStr);
			for (String questionIdStr : questionIdStrList) {
				for (int questionId : questionIdList) {
					if (questionId == Integer.parseInt(questionIdStr)) {
						questionIdOrderList.add(questionId);
					}
				}
			}
			// 找出不再序列中的questionId
			List<Integer> noOrderQuestionIdList = new ArrayList<Integer>();
			for (int questionId : questionIdList) {
				if (!questionIdOrderList.contains(questionId)) {
					noOrderQuestionIdList.add(questionId);
				}
			}
			// 把不再序列中的questionId放到最后面
			questionIdOrderList.addAll(noOrderQuestionIdList);
			
			return questionIdOrderList;
		} finally {
			DBUtil.closeQuietly(rs);
			DBUtil.closeQuietly(stmt);
		}
	}
	
	/**
	 * 获取当前用户已经提交过的调研id
	 * @param conn         
	 * @param lastSurveyId 上一页最后一个调研id（初始为null）
	 * @param lastEndTime  上一页最后一个调研结束时间（初始为null）
	 * @return
	 * @throws SQLException 
	 */
	public static List<Integer> getSubmitSurveyId(Connection conn, long companyId, long userId, @Nullable Integer lastSurveyId, @Nullable Integer lastEndTime) throws SQLException {
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		
		try {
			if (lastSurveyId == null || lastEndTime == null) {
				pstmt = conn.prepareStatement("SELECT survey_id FROM weizhu_survey_result WHERE company_id = ? AND user_id = ? AND submit_time IS NOT NULL AND survey_id in (SELECT survey_id FROM weizhu_survey WHERE state = 'NORMAL') ORDER BY submit_time DESC, survey_id ASC; ");
				pstmt.setLong(1, companyId);
				pstmt.setLong(2, userId);
			} else {
				pstmt = conn.prepareStatement("SELECT survey_id FROM weizhu_survey_result WHERE company_id = ? AND user_id = ? AND submit_time IS NOT NULL AND ((submit_time = ? AND survey_id > ?) OR submit_time < ? ) AND survey_id in (SELECT survey_id FROM weizhu_survey WHERE state = 'NORMAL') ORDER BY submit_time DESC, survey_id ASC; ");
				pstmt.setLong(1, companyId);
				pstmt.setLong(2, userId);
				pstmt.setInt(3, lastEndTime);
				pstmt.setInt(4, lastSurveyId);
				pstmt.setInt(5, lastEndTime);
			}
			rs = pstmt.executeQuery();
			
			List<Integer> surveyIdList = new ArrayList<Integer>();
			while (rs.next()) {
				surveyIdList.add(rs.getInt("survey_id"));
			}
			
			return surveyIdList;
		} finally {
			DBUtil.closeQuietly(rs);
			DBUtil.closeQuietly(pstmt);
		}
	}
	
	/**
	 * 查询一个用户的调研信息结果（此接口调用次数过多，考虑性能不返回每个调研的详细结果）
	 * @param conn      db connection
	 * @param userId    需要查询的用户id
	 * @param surveyIds 需要查询的所有调研id
	 * @return key -> SurveyId, value -> SurveyResult
	 * @throws SQLException 
	 */
	public static Map<Integer, SurveyProtos.SurveyResult> getUserSurveyResult(Connection conn, long companyId, long userId, Collection<Integer> surveyIds) throws SQLException {
		if (surveyIds.isEmpty()) {
			return Collections.emptyMap();
		}
		
		Statement stmt = null;
		ResultSet rs = null;
		
		StringBuilder sql = new StringBuilder("SELECT * FROM weizhu_survey_result WHERE company_id = ").append(companyId).append(" AND user_id = ");
		sql.append(userId).append(" AND survey_id IN (").append(DBUtil.COMMA_JOINER.join(surveyIds)).append("); ");
		
		try {
			stmt = conn.createStatement();
			rs = stmt.executeQuery(sql.toString());
			
			Map<Integer, SurveyProtos.SurveyResult> surveyResultMap = new HashMap<Integer, SurveyProtos.SurveyResult>();
			
			SurveyProtos.SurveyResult.Builder surveyResultBuilder = SurveyProtos.SurveyResult.newBuilder()
					.setUserId(userId);
			while (rs.next()) {
				surveyResultBuilder.clear();
				
				int surveyId = rs.getInt("survey_id");
				
				surveyResultBuilder.setSurveyId(surveyId);
				surveyResultBuilder.setSubmitTime(rs.getInt("submit_time"));
				surveyResultBuilder.setUserId(rs.getLong("user_id"));
				
				surveyResultMap.put(surveyId, surveyResultBuilder.build());
			}
			
			return surveyResultMap;
		} finally {
			DBUtil.closeQuietly(rs);
			DBUtil.closeQuietly(stmt);
		}
	}
	
	/**
	 * 获取某个用户某个调研的详情
	 * @param conn
	 * @param companyId
	 * @param userId
	 * @param surveyId
	 * @return
	 * @throws SQLException 
	 */
	public static SurveyProtos.SurveyResult getUserSurveyResult(Connection conn, long companyId, long userId, int surveyId) throws SQLException {
		PreparedStatement pstmt = null;
		Statement stmt = null;
		ResultSet rs = null;
		
		try {
			pstmt = conn.prepareStatement("SELECT * FROM weizhu_survey_result WHERE company_id = ? AND survey_id = ? AND user_id = ?; ");
			pstmt.setLong(1, companyId);
			pstmt.setInt(2, surveyId);
			pstmt.setLong(3, userId);
			
			rs = pstmt.executeQuery();
			
			SurveyProtos.SurveyResult.Builder surveyResultBuilder = SurveyProtos.SurveyResult.newBuilder()
					.setSurveyId(surveyId)
					.setUserId(userId)
					.setSubmitTime(0);
			
			if (!rs.next()) {
				return surveyResultBuilder.build();
			}
			
			SurveyProtos.SurveyResult surveyResult = surveyResultBuilder.setSubmitTime(rs.getInt("submit_time")).build();
			
			DBUtil.closeQuietly(rs);
			rs = null;
			DBUtil.closeQuietly(pstmt);
			pstmt = null;
			
			// 取调研对应的题目id
			pstmt = conn.prepareStatement("SELECT question_id FROM weizhu_survey_join_question WHERE company_id = ? AND survey_id =?; ");
			pstmt.setLong(1, companyId);
			pstmt.setInt(2, surveyId);
			rs = pstmt.executeQuery();
			List<Integer> questionIdList = new ArrayList<Integer>();
			while (rs.next()) {
				questionIdList.add(rs.getInt("question_id"));
			}
			DBUtil.closeQuietly(rs);
			rs = null;
			DBUtil.closeQuietly(pstmt);
			pstmt = null;
			
			if (questionIdList.isEmpty()) {
				return surveyResult;
			}
			
			// 取题目id,和类型
			StringBuilder sql = new StringBuilder();
			sql.append("SELECT question_id, type FROM weizhu_survey_question WHERE ");
			sql.append(" company_id = ").append(companyId);
			sql.append(" AND question_id IN (").append(DBUtil.COMMA_JOINER.join(questionIdList));
			sql.append("); ");
			stmt = conn.createStatement();
			rs = stmt.executeQuery(sql.toString());
			
			Map<Integer, String> questionTypeMap = new HashMap<Integer, String>();
			while (rs.next()) {
				questionTypeMap.put(rs.getInt("question_id"), rs.getString("type"));
			}
			
			if (questionTypeMap.isEmpty()) {
				return surveyResult;
			}
			
			List<Integer> voteIdList = new ArrayList<Integer>();
			List<Integer> inputSelectIdList = new ArrayList<Integer>();
			List<Integer> inputTextIdList = new ArrayList<Integer>();
			for (Entry<Integer, String> entry : questionTypeMap.entrySet()) {
				if (entry.getValue().equals("VOTE")) {
					voteIdList.add(entry.getKey());
				} else if (entry.getValue().equals("INPUT_SELECT")) {
					inputSelectIdList.add(entry.getKey());
				} else if (entry.getValue().equals("INPUT_TEXT")) {
					inputTextIdList.add(entry.getKey());
				}
			}
			
			Map<Long, Map<Integer, SurveyProtos.Vote.Answer>> voteAnswerUserMap = getVoteAnswerUserMap(conn, companyId, Collections.singleton(userId), voteIdList);
			Map<Long, Map<Integer, SurveyProtos.InputSelect.Answer>> inputSelectAnswerUserMap = getInputSelectAnswerUserMap(conn, companyId, Collections.singleton(userId), inputSelectIdList);
			Map<Long, Map<Integer, SurveyProtos.InputText.Answer>> inputTextAnswerUserMap = getInputTextAnswerUserMap(conn, companyId, Collections.singleton(userId), inputTextIdList);
			
			// 遍历SurveyResult，放入SurveyResultMap
			int submitTime = surveyResult.getSubmitTime();
			Map<Integer, SurveyProtos.Vote.Answer>  voteAnswerMap = voteAnswerUserMap.get(userId);
			if (voteAnswerMap != null) {
				SurveyProtos.Answer.Builder answerBuilder = SurveyProtos.Answer.newBuilder();
				for (Entry<Integer, SurveyProtos.Vote.Answer> voteAnswerEntry : voteAnswerMap.entrySet()) {
					answerBuilder.clear();
					
					answerBuilder.setVote(voteAnswerEntry.getValue())
							.setQuestionId(voteAnswerEntry.getKey())
							.setUserId(userId).setAnswerTime(submitTime);
					
					surveyResult = SurveyProtos.SurveyResult.newBuilder()
							.mergeFrom(surveyResult)
							.addAnswer(answerBuilder.build())
							.build();
				}
			}
			
			Map<Integer, SurveyProtos.InputText.Answer>  inputTextAnswerMap = inputTextAnswerUserMap.get(userId);
			if (inputTextAnswerMap != null) {
				SurveyProtos.Answer.Builder answerBuilder = SurveyProtos.Answer.newBuilder();
				for (Entry<Integer, SurveyProtos.InputText.Answer> inputTextAnswerEntry : inputTextAnswerMap.entrySet()) {
					answerBuilder.clear();
					
					answerBuilder.setInputText(inputTextAnswerEntry.getValue())
							.setQuestionId(inputTextAnswerEntry.getKey())
							.setUserId(userId).setAnswerTime(submitTime);
					
					surveyResult = SurveyProtos.SurveyResult.newBuilder()
							.mergeFrom(surveyResult)
							.addAnswer(answerBuilder.build())
							.build();
				}
			}
			
			Map<Integer, SurveyProtos.InputSelect.Answer>  inputSelectAnswerMap = inputSelectAnswerUserMap.get(userId);
			if (inputSelectAnswerMap != null) {
				SurveyProtos.Answer.Builder answerBuilder = SurveyProtos.Answer.newBuilder();
				for (Entry<Integer, SurveyProtos.InputSelect.Answer> inputSelectAnswerEntry : inputSelectAnswerMap.entrySet()) {
					answerBuilder.clear();
					
					answerBuilder.setInputSelect(inputSelectAnswerEntry.getValue())
							.setQuestionId(inputSelectAnswerEntry.getKey())
							.setUserId(userId).setAnswerTime(submitTime);
					
					surveyResult = SurveyProtos.SurveyResult.newBuilder()
							.mergeFrom(surveyResult)
							.addAnswer(answerBuilder.build())
							.build();
				}
			}
			
			return surveyResult;
		} finally {
			DBUtil.closeQuietly(rs);
			DBUtil.closeQuietly(pstmt);
			DBUtil.closeQuietly(stmt);
		}
	}
	
	/**
	 * 查询一个调研的所有用户回答信息(提供给客户端)
	 * @param conn           db connection
	 * @param surveyId       需要查询的调研id
	 * @param lastUserId     上一页的最后一个用户id
	 * @param lastSubmitTime 上一页的最后一个提交时间
	 * @param size           获取请求数量
	 * @return key -> UserId, value -> SurveyResult
	 * @throws SQLException 
	 */
	public static Map<Long, SurveyProtos.SurveyResult> getSurveyUserResult(Connection conn, long companyId, int surveyId, @Nullable Long lastUserId, @Nullable Integer lastSubmitTime, int size) throws SQLException {
		PreparedStatement pstmt = null;
		Statement stmt = null;
		ResultSet rs = null;
		
		Map<Long, SurveyProtos.SurveyResult> surveyResultMap = new LinkedHashMap<Long, SurveyProtos.SurveyResult>();
		try {
			if (lastUserId == null || lastSubmitTime == null) {
				pstmt = conn.prepareStatement("SELECT * FROM weizhu_survey_result WHERE company_id = ? AND survey_id = ? ORDER BY submit_time DESC LIMIT ?; ");
				pstmt.setLong(1, companyId);
				pstmt.setInt(2, surveyId);
				pstmt.setInt(3, size);
			} else {
				pstmt = conn.prepareStatement("SELECT * FROM weizhu_survey_result WHERE company_id = ? AND survey_id = ? AND (submit_time < ? OR (submit_time = ? AND user_id > ?)) ORDER BY submit_time DESC, user_id ASC LIMIT ?;");
				pstmt.setLong(1, companyId);
				pstmt.setInt(2, surveyId);
				pstmt.setInt(3, lastSubmitTime);
				pstmt.setInt(4, lastSubmitTime);
				pstmt.setLong(5, lastUserId);
				pstmt.setInt(6, size);
			}
			rs = pstmt.executeQuery();
			
			Map<Long, SurveyProtos.SurveyResult> surveyResultTmpMap = new LinkedHashMap<Long, SurveyProtos.SurveyResult>();
			SurveyProtos.SurveyResult.Builder surveyResultBuilder = SurveyProtos.SurveyResult.newBuilder()
					.setSurveyId(surveyId);
			while (rs.next()) {
				surveyResultBuilder.clear();
				
				long userId = rs.getLong("user_id");
				surveyResultBuilder.setSurveyId(rs.getInt("survey_id"));
				surveyResultBuilder.setSubmitTime(rs.getInt("submit_time"));
				surveyResultBuilder.setUserId(userId);
				surveyResultTmpMap.put(userId, surveyResultBuilder.build());
			}
			DBUtil.closeQuietly(rs);
			rs = null;
			DBUtil.closeQuietly(pstmt);
			pstmt = null;
			
			if (surveyResultTmpMap.isEmpty()) {
				return Collections.emptyMap();
			}
			
			// 取调研对应的题目id
			pstmt = conn.prepareStatement("SELECT question_id FROM weizhu_survey_join_question WHERE company_id = ? AND survey_id =?; ");
			pstmt.setLong(1, companyId);
			pstmt.setInt(2, surveyId);
			rs = pstmt.executeQuery();
			List<Integer> questionIdList = new ArrayList<Integer>();
			while (rs.next()) {
				questionIdList.add(rs.getInt("question_id"));
			}
			DBUtil.closeQuietly(rs);
			rs = null;
			DBUtil.closeQuietly(pstmt);
			pstmt = null;
			
			if (questionIdList.isEmpty()) {
				return Collections.emptyMap();
			}
			
			// 取题目id,和类型
			StringBuilder sql = new StringBuilder();
			sql.append("SELECT question_id, type FROM weizhu_survey_question WHERE ");
			sql.append(" company_id = ").append(companyId);
			sql.append(" AND question_id IN (").append(DBUtil.COMMA_JOINER.join(questionIdList));
			sql.append("); ");
			stmt = conn.createStatement();
			rs = stmt.executeQuery(sql.toString());
			
			Map<Integer, String> questionTypeMap = new HashMap<Integer, String>();
			while (rs.next()) {
				questionTypeMap.put(rs.getInt("question_id"), rs.getString("type"));
			}
			
			if (questionTypeMap.isEmpty()) {
				return Collections.emptyMap();
			}
			
			List<Integer> voteIdList = new ArrayList<Integer>();
			List<Integer> inputSelectIdList = new ArrayList<Integer>();
			List<Integer> inputTextIdList = new ArrayList<Integer>();
			for (Entry<Integer, String> entry : questionTypeMap.entrySet()) {
				if (entry.getValue().equals("VOTE")) {
					voteIdList.add(entry.getKey());
				} else if (entry.getValue().equals("INPUT_SELECT")) {
					inputSelectIdList.add(entry.getKey());
				} else if (entry.getValue().equals("INPUT_TEXT")) {
					inputTextIdList.add(entry.getKey());
				}
			}
			
			Map<Long, Map<Integer, SurveyProtos.Vote.Answer>> voteAnswerUserMap = getVoteAnswerUserMap(conn, companyId, surveyResultTmpMap.keySet(), voteIdList);
			Map<Long, Map<Integer, SurveyProtos.InputSelect.Answer>> inputSelectAnswerUserMap = getInputSelectAnswerUserMap(conn, companyId, surveyResultTmpMap.keySet(), inputSelectIdList);
			Map<Long, Map<Integer, SurveyProtos.InputText.Answer>> inputTextAnswerUserMap = getInputTextAnswerUserMap(conn, companyId, surveyResultTmpMap.keySet(), inputTextIdList);
			
			// 遍历SurveyResult，放入SurveyResultMap
			for (Entry<Long, SurveyProtos.SurveyResult> entry : surveyResultTmpMap.entrySet()) {
				long userId = entry.getKey();
				SurveyProtos.SurveyResult surveyResult = entry.getValue();
				int submitTime = surveyResult.getSubmitTime();
				Map<Integer, SurveyProtos.Vote.Answer>  voteAnswerMap = voteAnswerUserMap.get(userId);
				if (voteAnswerMap != null) {
					SurveyProtos.Answer.Builder answerBuilder = SurveyProtos.Answer.newBuilder();
					for (Entry<Integer, SurveyProtos.Vote.Answer> voteAnswerEntry : voteAnswerMap.entrySet()) {
						answerBuilder.clear();
						
						answerBuilder.setVote(voteAnswerEntry.getValue())
								.setQuestionId(voteAnswerEntry.getKey())
								.setUserId(userId).setAnswerTime(submitTime);
						
						SurveyProtos.SurveyResult result = surveyResultMap.get(userId);
						if (result != null) {
							surveyResultMap.put(userId, SurveyProtos.SurveyResult
									.newBuilder().mergeFrom(result)
									.addAnswer(answerBuilder.build()).build());
						} else {
							surveyResultMap.put(userId, SurveyProtos.SurveyResult
									.newBuilder().mergeFrom(surveyResult)
									.addAnswer(answerBuilder.build()).build());
						}
						
					}
				}
				
				Map<Integer, SurveyProtos.InputText.Answer>  inputTextAnswerMap = inputTextAnswerUserMap.get(userId);
				if (inputTextAnswerMap != null) {
					SurveyProtos.Answer.Builder answerBuilder = SurveyProtos.Answer.newBuilder();
					for (Entry<Integer, SurveyProtos.InputText.Answer> inputTextAnswerEntry : inputTextAnswerMap.entrySet()) {
						answerBuilder.clear();
						
						answerBuilder.setInputText(inputTextAnswerEntry.getValue())
								.setQuestionId(inputTextAnswerEntry.getKey())
								.setUserId(userId).setAnswerTime(submitTime);
						
						SurveyProtos.SurveyResult result = surveyResultMap.get(userId);
						if (result != null) {
							surveyResultMap.put(userId, SurveyProtos.SurveyResult
									.newBuilder().mergeFrom(result)
									.addAnswer(answerBuilder.build()).build());
						} else {
							surveyResultMap.put(userId, SurveyProtos.SurveyResult
									.newBuilder().mergeFrom(surveyResult)
									.addAnswer(answerBuilder.build()).build());
						}
					}
				}
				
				Map<Integer, SurveyProtos.InputSelect.Answer>  inputSelectAnswerMap = inputSelectAnswerUserMap.get(userId);
				if (inputSelectAnswerMap != null) {
					SurveyProtos.Answer.Builder answerBuilder = SurveyProtos.Answer.newBuilder();
					for (Entry<Integer, SurveyProtos.InputSelect.Answer> inputSelectAnswerEntry : inputSelectAnswerMap.entrySet()) {
						answerBuilder.clear();
						
						answerBuilder.setInputSelect(inputSelectAnswerEntry.getValue())
								.setQuestionId(inputSelectAnswerEntry.getKey())
								.setUserId(userId).setAnswerTime(submitTime);
						
						SurveyProtos.SurveyResult result = surveyResultMap.get(userId);
						if (result != null) {
							surveyResultMap.put(userId, SurveyProtos.SurveyResult
									.newBuilder().mergeFrom(result)
									.addAnswer(answerBuilder.build()).build());
						} else {
							surveyResultMap.put(userId, SurveyProtos.SurveyResult
									.newBuilder().mergeFrom(surveyResult)
									.addAnswer(answerBuilder.build()).build());
						}
					}
				}
			}
		} finally {
			DBUtil.closeQuietly(rs);
			DBUtil.closeQuietly(pstmt);
			DBUtil.closeQuietly(stmt);
		}
		
		return surveyResultMap;
	}
	
	/**
	 * 查询一个调研的所有用户回答信息(提供给服务端)
	 * @param conn
	 * @param companyId
	 * @param surveyId
	 * @param start
	 * @param length
	 * @return
	 * @throws SQLException
	 */
	public static Map<Long, SurveyProtos.SurveyResult> getSurveyUserResult(Connection conn, long companyId, int surveyId, int start, int length) throws SQLException {
		PreparedStatement pstmt = null;
		Statement stmt = null;
		ResultSet rs = null;
		
		Map<Long, SurveyProtos.SurveyResult> surveyResultMap = new LinkedHashMap<Long, SurveyProtos.SurveyResult>();
		try {
			pstmt = conn.prepareStatement("SELECT * FROM weizhu_survey_result WHERE company_id = ? AND survey_id = ? LIMIT ?, ?;");
			pstmt.setLong(1, companyId);
			pstmt.setInt(2, surveyId);
			pstmt.setInt(3, start);
			pstmt.setInt(4, length);
			
			rs = pstmt.executeQuery();
			
			Map<Long, SurveyProtos.SurveyResult> surveyResultTmpMap = new LinkedHashMap<Long, SurveyProtos.SurveyResult>();
			SurveyProtos.SurveyResult.Builder surveyResultBuilder = SurveyProtos.SurveyResult.newBuilder()
					.setSurveyId(surveyId);
			while (rs.next()) {
				surveyResultBuilder.clear();
				
				long userId = rs.getLong("user_id");
				surveyResultBuilder.setSurveyId(rs.getInt("survey_id"));
				surveyResultBuilder.setSubmitTime(rs.getInt("submit_time"));
				surveyResultBuilder.setUserId(userId);
				surveyResultTmpMap.put(userId, surveyResultBuilder.build());
			}
			DBUtil.closeQuietly(rs);
			rs = null;
			DBUtil.closeQuietly(pstmt);
			pstmt = null;
			
			if (surveyResultTmpMap.isEmpty()) {
				return Collections.emptyMap();
			}
			
			// 取调研对应的题目id
			pstmt = conn.prepareStatement("SELECT question_id FROM weizhu_survey_join_question WHERE company_id = ? AND survey_id =?; ");
			pstmt.setLong(1, companyId);
			pstmt.setInt(2, surveyId);
			rs = pstmt.executeQuery();
			List<Integer> questionIdList = new ArrayList<Integer>();
			while (rs.next()) {
				questionIdList.add(rs.getInt("question_id"));
			}
			DBUtil.closeQuietly(rs);
			rs = null;
			DBUtil.closeQuietly(pstmt);
			pstmt = null;
			
			if (questionIdList.isEmpty()) {
				return Collections.emptyMap();
			}
			
			// 取题目id,和类型
			StringBuilder sql = new StringBuilder();
			sql.append("SELECT question_id, type FROM weizhu_survey_question WHERE ");
			sql.append(" company_id = ").append(companyId);
			sql.append(" AND question_id IN (").append(DBUtil.COMMA_JOINER.join(questionIdList));
			sql.append("); ");
			stmt = conn.createStatement();
			rs = stmt.executeQuery(sql.toString());
			
			Map<Integer, String> questionTypeMap = new HashMap<Integer, String>();
			while (rs.next()) {
				questionTypeMap.put(rs.getInt("question_id"), rs.getString("type"));
			}
			
			if (questionTypeMap.isEmpty()) {
				return Collections.emptyMap();
			}
			
			List<Integer> voteIdList = new ArrayList<Integer>();
			List<Integer> inputSelectIdList = new ArrayList<Integer>();
			List<Integer> inputTextIdList = new ArrayList<Integer>();
			for (Entry<Integer, String> entry : questionTypeMap.entrySet()) {
				if (entry.getValue().equals("VOTE")) {
					voteIdList.add(entry.getKey());
				} else if (entry.getValue().equals("INPUT_SELECT")) {
					inputSelectIdList.add(entry.getKey());
				} else if (entry.getValue().equals("INPUT_TEXT")) {
					inputTextIdList.add(entry.getKey());
				}
			}
			
			Map<Long, Map<Integer, SurveyProtos.Vote.Answer>> voteAnswerUserMap = getVoteAnswerUserMap(conn, companyId, surveyResultTmpMap.keySet(), voteIdList);
			Map<Long, Map<Integer, SurveyProtos.InputSelect.Answer>> inputSelectAnswerUserMap = getInputSelectAnswerUserMap(conn, companyId, surveyResultTmpMap.keySet(), inputSelectIdList);
			Map<Long, Map<Integer, SurveyProtos.InputText.Answer>> inputTextAnswerUserMap = getInputTextAnswerUserMap(conn, companyId, surveyResultTmpMap.keySet(), inputTextIdList);
			// 遍历SurveyResult，放入SurveyResultMap
			for (Entry<Long, SurveyProtos.SurveyResult> entry : surveyResultTmpMap.entrySet()) {
				long userId = entry.getKey();
				SurveyProtos.SurveyResult surveyResult = entry.getValue();
				int submitTime = surveyResult.getSubmitTime();
				Map<Integer, SurveyProtos.Vote.Answer>  voteAnswerMap = voteAnswerUserMap.get(userId);
				
				surveyResultMap.put(userId, SurveyProtos.SurveyResult
						.newBuilder().mergeFrom(surveyResult).build());
						
				if (voteAnswerMap != null) {
					SurveyProtos.Answer.Builder answerBuilder = SurveyProtos.Answer.newBuilder();
					for (Entry<Integer, SurveyProtos.Vote.Answer> voteAnswerEntry : voteAnswerMap.entrySet()) {
						answerBuilder.clear();
						
						answerBuilder.setVote(voteAnswerEntry.getValue())
								.setQuestionId(voteAnswerEntry.getKey())
								.setUserId(userId).setAnswerTime(submitTime);
						
						SurveyProtos.SurveyResult result = surveyResultMap.get(userId);
						if (result != null) {
							surveyResultMap.put(userId, SurveyProtos.SurveyResult
									.newBuilder().mergeFrom(result)
									.addAnswer(answerBuilder.build()).build());
						} else {
							surveyResultMap.put(userId, SurveyProtos.SurveyResult
									.newBuilder().mergeFrom(surveyResult)
									.addAnswer(answerBuilder.build()).build());
						}
						
					}
				}
				
				Map<Integer, SurveyProtos.InputText.Answer>  inputTextAnswerMap = inputTextAnswerUserMap.get(userId);
				if (inputTextAnswerMap != null) {
					SurveyProtos.Answer.Builder answerBuilder = SurveyProtos.Answer.newBuilder();
					for (Entry<Integer, SurveyProtos.InputText.Answer> inputTextAnswerEntry : inputTextAnswerMap.entrySet()) {
						answerBuilder.clear();
						
						answerBuilder.setInputText(inputTextAnswerEntry.getValue())
								.setQuestionId(inputTextAnswerEntry.getKey())
								.setUserId(userId).setAnswerTime(submitTime);
						
						SurveyProtos.SurveyResult result = surveyResultMap.get(userId);
						if (result != null) {
							surveyResultMap.put(userId, SurveyProtos.SurveyResult
									.newBuilder().mergeFrom(result)
									.addAnswer(answerBuilder.build()).build());
						} else {
							surveyResultMap.put(userId, SurveyProtos.SurveyResult
									.newBuilder().mergeFrom(surveyResult)
									.addAnswer(answerBuilder.build()).build());
						}
					}
				}
				
				Map<Integer, SurveyProtos.InputSelect.Answer>  inputSelectAnswerMap = inputSelectAnswerUserMap.get(userId);
				if (inputSelectAnswerMap != null) {
					SurveyProtos.Answer.Builder answerBuilder = SurveyProtos.Answer.newBuilder();
					for (Entry<Integer, SurveyProtos.InputSelect.Answer> inputSelectAnswerEntry : inputSelectAnswerMap.entrySet()) {
						answerBuilder.clear();
						
						answerBuilder.setInputSelect(inputSelectAnswerEntry.getValue())
								.setQuestionId(inputSelectAnswerEntry.getKey())
								.setUserId(userId).setAnswerTime(submitTime);
						
						SurveyProtos.SurveyResult result = surveyResultMap.get(userId);
						if (result != null) {
							surveyResultMap.put(userId, SurveyProtos.SurveyResult
									.newBuilder().mergeFrom(result)
									.addAnswer(answerBuilder.build()).build());
						} else {
							surveyResultMap.put(userId, SurveyProtos.SurveyResult
									.newBuilder().mergeFrom(surveyResult)
									.addAnswer(answerBuilder.build()).build());
						}
					}
				}
			}
		} finally {
			DBUtil.closeQuietly(rs);
			DBUtil.closeQuietly(pstmt);
			DBUtil.closeQuietly(stmt);
		}
		
		return surveyResultMap;
	}
	
	private static Map<Long, Map<Integer, SurveyProtos.Vote.Answer>> getVoteAnswerUserMap(Connection conn, long companyId, Collection<Long> userIdList, 
			List<Integer> voteIdList) throws SQLException {
		if (voteIdList.isEmpty() || userIdList.isEmpty()) {
			return Collections.emptyMap();
		}
		
		StringBuilder sql = new StringBuilder();
		sql.append("SELECT * FROM weizhu_survey_vote_answer WHERE company_id = ")
				.append(companyId)
				.append(" AND question_id IN (")
				.append(DBUtil.COMMA_JOINER.join(voteIdList)).append(") AND user_id IN (")
				.append(DBUtil.COMMA_JOINER.join(userIdList)).append(")").append("; ");
	
		Statement stmt = null;
		ResultSet rs = null;
		try {
			stmt = conn.createStatement();
			rs = stmt.executeQuery(sql.toString());
			
			// Map<UserId, Map<QuestionId, Vote.Answer.Builder>>
			Map<Long, Map<Integer, SurveyProtos.Vote.Answer>> voteAnswerUserMap = new HashMap<Long, Map<Integer, SurveyProtos.Vote.Answer>>();
			while (rs.next()) {
				long userId = rs.getLong("user_id");
				int questionId = rs.getInt("question_id");
				int optionId = rs.getInt("option_id");
				
				Map<Integer, SurveyProtos.Vote.Answer> voteAnswerMap = voteAnswerUserMap.get(userId);
				if (voteAnswerMap != null) {
					SurveyProtos.Vote.Answer voteAnswer = voteAnswerMap.get(questionId);
					if (voteAnswer != null) {
						voteAnswer = SurveyProtos.Vote.Answer.newBuilder()
								.mergeFrom(voteAnswer)
								.addOptionId(optionId)
								.build();
					} else {
						voteAnswer = SurveyProtos.Vote.Answer.newBuilder()
								.addOptionId(optionId)
								.build();
					}
					voteAnswerMap.put(questionId, voteAnswer);
				} else {
					SurveyProtos.Vote.Answer voteAnswer = SurveyProtos.Vote.Answer.newBuilder()
							.addOptionId(optionId)
							.build();
					voteAnswerMap = new HashMap<Integer, SurveyProtos.Vote.Answer>();
					voteAnswerMap.put(questionId, voteAnswer);
					voteAnswerUserMap.put(userId, voteAnswerMap);
				}
			}
			
			return voteAnswerUserMap;
		} finally {
			DBUtil.closeQuietly(rs);
			DBUtil.closeQuietly(stmt);
		}
	}
	
	private static Map<Long, Map<Integer, SurveyProtos.InputSelect.Answer>> getInputSelectAnswerUserMap(Connection conn, long companyId, Collection<Long> userIdList, 
			List<Integer> inputSelectIdList) throws SQLException {
		if (inputSelectIdList.isEmpty() || userIdList.isEmpty()) {
			return Collections.emptyMap();
		}
		
		StringBuilder sql = new StringBuilder();
		sql.append("SELECT * FROM weizhu_survey_input_select_answer WHERE company_id = ")
				.append(companyId)
				.append(" AND question_id IN (")
				.append(DBUtil.COMMA_JOINER.join(inputSelectIdList)).append(") AND user_id IN (")
				.append(DBUtil.COMMA_JOINER.join(userIdList)).append(")").append("; ");
	
		Statement stmt = null;
		ResultSet rs = null;
		try {
			stmt = conn.createStatement();
			rs = stmt.executeQuery(sql.toString());
			
			// Map<UserId, Map<QuestionId, InputSelect.Answer.Builder>>
			Map<Long, Map<Integer, SurveyProtos.InputSelect.Answer>> inputSelectAnswerUserMap = new HashMap<Long, Map<Integer, SurveyProtos.InputSelect.Answer>>();
			Map<Integer, SurveyProtos.InputSelect.Answer> inputSelectAnswerMap = null;
			while (rs.next()) {
				long userId = rs.getLong("user_id");
				int questionId = rs.getInt("question_id");
				int optionId = rs.getInt("option_id");
				
				inputSelectAnswerMap = inputSelectAnswerUserMap.get(userId);
				if (inputSelectAnswerMap == null) {
					inputSelectAnswerMap = new HashMap<Integer, SurveyProtos.InputSelect.Answer>();
				}
				inputSelectAnswerMap.put(questionId, SurveyProtos.InputSelect.Answer.newBuilder()
						.setOptionId(optionId)
						.build());
				inputSelectAnswerUserMap.put(userId, inputSelectAnswerMap);
			}
			
			return inputSelectAnswerUserMap;
		} finally {
			DBUtil.closeQuietly(rs);
			DBUtil.closeQuietly(stmt);
		}
	}
	
	private static Map<Long, Map<Integer, SurveyProtos.InputText.Answer>> getInputTextAnswerUserMap(Connection conn, long companyId, Collection<Long> userIdList, 
			List<Integer> inputTextIdList) throws SQLException {
		if (inputTextIdList.isEmpty() || userIdList.isEmpty()) {
			return Collections.emptyMap();
		}
		
		StringBuilder sql = new StringBuilder();
		sql.append("SELECT * FROM weizhu_survey_input_text_answer WHERE company_id = ")
				.append(companyId)
				.append(" AND question_id IN (")
				.append(DBUtil.COMMA_JOINER.join(inputTextIdList)).append(") AND user_id IN (")
				.append(DBUtil.COMMA_JOINER.join(userIdList)).append(")").append("; ");

		Statement stmt = null;
		ResultSet rs = null;
		try {
			stmt = conn.createStatement();
			rs = stmt.executeQuery(sql.toString());
			
			// Map<UserId, Map<QuestionId, InputText.Answer.Builder>>
			Map<Long, Map<Integer, SurveyProtos.InputText.Answer>> inputTextAnswerUserMap = new HashMap<Long, Map<Integer, SurveyProtos.InputText.Answer>>();
			Map<Integer, SurveyProtos.InputText.Answer> inputTextAnswerMap = null;
			while (rs.next()) {
				long userId = rs.getLong("user_id");
				int questionId = rs.getInt("question_id");
				String resultText = rs.getString("result_text");
				
				inputTextAnswerMap = inputTextAnswerUserMap.get(userId);
				if (inputTextAnswerMap == null) {
					inputTextAnswerMap = new HashMap<Integer, SurveyProtos.InputText.Answer>();
				}
				inputTextAnswerMap.put(questionId, SurveyProtos.InputText.Answer.newBuilder()
						.setResultText(resultText)
						.build());
				inputTextAnswerUserMap.put(userId, inputTextAnswerMap);
			}
			
			return inputTextAnswerUserMap;
		} finally {
			DBUtil.closeQuietly(rs);
			DBUtil.closeQuietly(stmt);
		}
	}
	
	public static int getSurveyUserResultCount(Connection conn, long companyId, int surveyId) throws SQLException {
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		
		try {
			pstmt = conn.prepareStatement("SELECT COUNT(*) AS total FROM weizhu_survey_result WHERE company_id = ? AND survey_id = ?; ");
			pstmt.setLong(1, companyId);
			pstmt.setInt(2, surveyId);
			
			rs = pstmt.executeQuery();
			if (!rs.next()) {
				throw new RuntimeException("cannt get the total! ");
			}
			
			return rs.getInt("total");
		} finally {
			DBUtil.closeQuietly(rs);
			DBUtil.closeQuietly(pstmt);
		}
	}
	
	/**
	 * 获取一个题目对应的所有用户的回答信息
	 * @param conn
	 * @param questionId
	 * @param lastUserId
	 * @param lastSubmitTime
	 * @param size
	 * @return
	 * @throws SQLException 
	 */
	public static List<SurveyProtos.Answer> getQuestionAnswer(Connection conn, long companyId, int questionId, @Nullable Long lastUserId, @Nullable Integer lastSubmitTime, int size) throws SQLException {
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		
		try {
			pstmt = conn.prepareStatement("SELECT type FROM weizhu_survey_question WHERE company_id = ? AND question_id = ?; ");
			pstmt.setLong(1, companyId);
			pstmt.setInt(2, questionId);
			rs = pstmt.executeQuery();
			
			String type = null;
			while (rs.next()) {
				type = rs.getString("type");
			}
			DBUtil.closeQuietly(rs);
			rs = null;
			DBUtil.closeQuietly(pstmt);
			pstmt = null;
			
			List<SurveyProtos.Answer> answerList = new ArrayList<SurveyProtos.Answer>();
			if (type.equals(SurveyProtos.Question.TypeCase.VOTE.name())) {
				if (lastUserId == null || lastSubmitTime == null) {
					pstmt = conn.prepareStatement("SELECT * FROM weizhu_survey_vote_answer WHERE company_id = ? AND question_id = ? ORDER BY answer_time DESC, user_id ASC LIMIT ?; ");
					pstmt.setLong(1, companyId);
					pstmt.setInt(2, questionId);
					pstmt.setInt(3, size);
				} else {
					pstmt = conn.prepareStatement("SELECT * FROM weizhu_survey_vote_answer WHERE company_id = ? AND question_id = ? AND (answer_time > ? OR (answer_time = ? AND user_id > ?)) ORDER BY answer_time DESC, user_id ASC LIMIT ?; ");
					pstmt.setLong(1, companyId);
					pstmt.setInt(2, questionId);
					pstmt.setInt(3, lastSubmitTime);
					pstmt.setInt(4, lastSubmitTime);
					pstmt.setLong(5, lastUserId);
					pstmt.setInt(6, size);
				}
				rs = pstmt.executeQuery();

				Map<Long, SurveyProtos.Vote.Answer> voteAnswerMap = new HashMap<Long, SurveyProtos.Vote.Answer>();
				Map<Long, Integer> userAnswerTimeMap = new LinkedHashMap<Long, Integer>();
				SurveyProtos.Vote.Answer.Builder answerBuilder = null;
				while (rs.next()) {
					answerBuilder.clear();
					
					long userId = rs.getLong("user_id");
					int submitTime = rs.getInt("submit_time");
					SurveyProtos.Vote.Answer voteAnswer = voteAnswerMap.get(userId);
					if (voteAnswer != null) {
						answerBuilder = SurveyProtos.Vote.Answer.newBuilder().mergeFrom(voteAnswer).addOptionId(rs.getInt("option_id"));
						voteAnswerMap.put(userId, answerBuilder.build());
					} else {
						answerBuilder = SurveyProtos.Vote.Answer.newBuilder().addOptionId(rs.getInt("option_id"));
						voteAnswerMap.put(userId, answerBuilder.build());
						userAnswerTimeMap.put(userId, submitTime);
					}
				}
				
				for (Entry<Long, Integer> entry : userAnswerTimeMap.entrySet()) {
					long userId = entry.getKey();
					int time = entry.getValue();
					
					SurveyProtos.Vote.Answer voteAnswer = voteAnswerMap.get(userId);
					if (voteAnswer != null) {
						answerList.add(SurveyProtos.Answer.newBuilder()
								.setVote(voteAnswer).setAnswerTime(time)
								.setQuestionId(questionId).setUserId(userId)
								.build());
					}
				}
				
			} else if (type.equals(SurveyProtos.Question.TypeCase.INPUT_TEXT.name())) {
				if (lastUserId == null || lastSubmitTime == null) {
					pstmt = conn.prepareStatement("SELECT * FROM weizhu_survey_input_text_answer WHERE company_id = ? AND question_id = ? ORDER BY answer_time DESC, user_id ASC LIMIT ?; ");
					pstmt.setLong(1, companyId);
					pstmt.setInt(2, questionId);
					pstmt.setInt(3, size);
				} else {
					pstmt = conn.prepareStatement("SELECT * FROM weizhu_survey_input_text_answer WHERE company_id = ? AND question_id = ? AND (answer_time > ? OR (answer_time = ? AND user_id > ?)) ORDER BY answer_time DESC, user_id ASC LIMIT ?; ");
					pstmt.setLong(1, companyId);
					pstmt.setInt(2, questionId);
					pstmt.setInt(3, lastSubmitTime);
					pstmt.setInt(4, lastSubmitTime);
					pstmt.setLong(5, lastUserId);
					pstmt.setInt(6, size);
				}
				rs = pstmt.executeQuery();

				Map<Long, SurveyProtos.InputText.Answer> inputTextAnswerMap = new HashMap<Long, SurveyProtos.InputText.Answer>();
				Map<Long, Integer> userAnswerTimeMap = new LinkedHashMap<Long, Integer>();
				SurveyProtos.InputText.Answer.Builder answerBuilder = null;
				while (rs.next()) {
					answerBuilder.clear();
					
					long userId = rs.getLong("user_id");
					int submitTime = rs.getInt("submit_time");
					SurveyProtos.InputText.Answer voteAnswer = inputTextAnswerMap.get(userId);
					if (voteAnswer != null) {
						answerBuilder = SurveyProtos.InputText.Answer.newBuilder().mergeFrom(voteAnswer).setResultText(rs.getString("result_text"));
						inputTextAnswerMap.put(userId, answerBuilder.build());
					} else {
						answerBuilder = SurveyProtos.InputText.Answer.newBuilder().setResultText(rs.getString("result_text"));
						inputTextAnswerMap.put(userId, answerBuilder.build());
						userAnswerTimeMap.put(userId, submitTime);
					}
				}
				
				for (Entry<Long, Integer> entry : userAnswerTimeMap.entrySet()) {
					long userId = entry.getKey();
					int time = entry.getValue();
					
					SurveyProtos.InputText.Answer inputTextAnswer = inputTextAnswerMap.get(userId);
					if (inputTextAnswer != null) {
						answerList.add(SurveyProtos.Answer.newBuilder()
								.setInputText(inputTextAnswer).setAnswerTime(time)
								.setQuestionId(questionId).setUserId(userId)
								.build());
					}
				}
			} else if (type.equals(SurveyProtos.Question.TypeCase.INPUT_SELECT.name())) {
				if (lastUserId == null || lastSubmitTime == null) {
					pstmt = conn.prepareStatement("SELECT * FROM weizhu_survey_input_select_answer WHERE company_id = ? AND question_id = ? ORDER BY answer_time DESC, user_id ASC LIMIT ?; ");
					pstmt.setLong(1, companyId);
					pstmt.setInt(2, questionId);
					pstmt.setInt(3, size);
				} else {
					pstmt = conn.prepareStatement("SELECT * FROM weizhu_survey_input_select_answer WHERE company_id = ? AND question_id = ? AND (answer_time > ? OR (answer_time = ? AND user_id > ?)) ORDER BY answer_time DESC, user_id ASC LIMIT ?; ");
					pstmt.setLong(1, companyId);
					pstmt.setInt(2, questionId);
					pstmt.setInt(3, lastSubmitTime);
					pstmt.setInt(4, lastSubmitTime);
					pstmt.setLong(5, lastUserId);
					pstmt.setInt(6, size);
				}
				rs = pstmt.executeQuery();

				Map<Long, SurveyProtos.InputSelect.Answer> inputSelectAnswerMap = new HashMap<Long, SurveyProtos.InputSelect.Answer>();
				Map<Long, Integer> userAnswerTimeMap = new LinkedHashMap<Long, Integer>();
				SurveyProtos.InputSelect.Answer.Builder answerBuilder = null;
				while (rs.next()) {
					answerBuilder.clear();
					
					long userId = rs.getLong("user_id");
					int submitTime = rs.getInt("submit_time");
					SurveyProtos.InputSelect.Answer voteAnswer = inputSelectAnswerMap.get(userId);
					if (voteAnswer != null) {
						answerBuilder = SurveyProtos.InputSelect.Answer.newBuilder().mergeFrom(voteAnswer).setOptionId(rs.getInt("option_id"));
						inputSelectAnswerMap.put(userId, answerBuilder.build());
					} else {
						answerBuilder = SurveyProtos.InputSelect.Answer.newBuilder().setOptionId(rs.getInt("option_id"));
						inputSelectAnswerMap.put(userId, answerBuilder.build());
						userAnswerTimeMap.put(userId, submitTime);
					}
				}
				
				for (Entry<Long, Integer> entry : userAnswerTimeMap.entrySet()) {
					long userId = entry.getKey();
					int time = entry.getValue();
					
					SurveyProtos.InputSelect.Answer inputSelectAnswer = inputSelectAnswerMap.get(userId);
					if (inputSelectAnswer != null) {
						answerList.add(SurveyProtos.Answer.newBuilder()
								.setInputSelect(inputSelectAnswer).setAnswerTime(time)
								.setQuestionId(questionId).setUserId(userId)
								.build());
					}
				}
			}
			return answerList;
			
		} finally {
			DBUtil.closeQuietly(rs);
			DBUtil.closeQuietly(pstmt);
		}
	}
	
	/**
	 * 保存调研回答问题
	 * @param conn
	 * @param surveyId              调研id
	 * @param voteAnswerList        投票和多选、单选题的用户答案
	 * @param inputTextAnswerList   主观题的用户答案
	 * @param inputSelectAnswerList 下拉菜单的用户答案
	 * @throws SQLException 
	 */
	public static void insertAnswer(Connection conn, long companyId, int surveyId, long userId, int submitTime,
			List<SurveyProtos.Answer> voteAnswerList,
			List<SurveyProtos.Answer> inputTextAnswerList,
			List<SurveyProtos.Answer> inputSelectAnswerList) throws SQLException {
		PreparedStatement pstmt = null;
		
		try {
			// 保存提交结果
			pstmt = conn.prepareStatement("INSERT INTO weizhu_survey_result (company_id, survey_id, user_id, submit_time) VALUES (?, ?, ?, ?); ");
			pstmt.setLong(1, companyId);
			pstmt.setInt(2, surveyId);
			pstmt.setLong(3, userId);
			pstmt.setInt(4, submitTime);
			pstmt.executeUpdate();
			DBUtil.closeQuietly(pstmt);
			pstmt = null;
			
			if (!voteAnswerList.isEmpty()) {
				pstmt = conn.prepareStatement("INSERT INTO weizhu_survey_vote_answer (company_id, question_id, user_id, option_id) VALUES (?, ?, ?, ?); ");
				for (SurveyProtos.Answer answer : voteAnswerList) {
					List<Integer> optionIdList = answer.getVote().getOptionIdList();
					for (int optionId : optionIdList) {
						pstmt.setLong(1, companyId);
						pstmt.setInt(2, answer.getQuestionId());
						pstmt.setLong(3, userId);
						pstmt.setInt(4, optionId);
						
						pstmt.addBatch();
					}
				}
				pstmt.executeBatch();
			}
			DBUtil.closeQuietly(pstmt);
			pstmt = null;
			
			if (!inputTextAnswerList.isEmpty()) {
				pstmt = conn.prepareStatement("INSERT INTO weizhu_survey_input_text_answer (company_id, question_id, user_id, result_text) VALUES (?, ?, ?, ?); ");
				for (SurveyProtos.Answer answer : inputTextAnswerList) {
					pstmt.setLong(1, companyId);
					pstmt.setInt(2, answer.getQuestionId());
					pstmt.setLong(3, userId);
					pstmt.setString(4, answer.getInputText().getResultText());
					
					pstmt.addBatch();
				}
				pstmt.executeBatch();
			}
			DBUtil.closeQuietly(pstmt);
			pstmt = null;
			
			if (!inputSelectAnswerList.isEmpty()) {
				pstmt = conn.prepareStatement("INSERT INTO weizhu_survey_input_select_answer (company_id, question_id, user_id, option_id) VALUES (?, ?, ?, ?); ");
				for (SurveyProtos.Answer answer : inputSelectAnswerList) {
					pstmt.setLong(1, companyId);
					pstmt.setInt(2, answer.getQuestionId());
					pstmt.setLong(3, userId);
					pstmt.setInt(4, answer.getInputSelect().getOptionId());
					
					pstmt.addBatch();
				}
				pstmt.executeBatch();
			}
			DBUtil.closeQuietly(pstmt);
			pstmt = null;
			
		} finally {
			DBUtil.closeQuietly(pstmt);
		}
	}
	
	/**
	 * 创建调研
	 * @param conn
	 * @param surveyName     调研名称
	 * @param surveyDesc     调研描述
	 * @param imageName      调研图标名称
	 * @param startTime      开始时间
	 * @param endTime        结束时间
	 * @param showResultType 调研结果展示类型
	 * @param allowModelId   访问模型id （可以为null）
	 * @param questions      问题列表
	 * @return
	 * @throws SQLException 
	 */
	public static int insertSurvey(Connection conn, long companyId, String surveyName,
			String surveyDesc, @Nullable String imageName, int startTime,
			int endTime, SurveyProtos.ShowResultType showResultType,
			@Nullable Integer allowModelId, int createTime, long createAdminId,
			List<SurveyProtos.Question> questionList) throws SQLException {
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		
		int surveyId = 0;
		try {
			pstmt = conn.prepareStatement("INSERT INTO weizhu_survey (company_id, survey_name, survey_desc, image_name, start_time, end_time, show_result_type, allow_model_id, state, create_time, create_admin_id) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?); ", Statement.RETURN_GENERATED_KEYS);
			DBUtil.set(pstmt, 1, companyId);
			DBUtil.set(pstmt, 2, surveyName);
			DBUtil.set(pstmt, 3, surveyDesc);
			DBUtil.set(pstmt, 4, imageName);
			DBUtil.set(pstmt, 5, startTime);
			DBUtil.set(pstmt, 6, endTime);
			DBUtil.set(pstmt, 7, showResultType.name());
			DBUtil.set(pstmt, 8, allowModelId);
			DBUtil.set(pstmt, 9, "NORMAL");
			DBUtil.set(pstmt, 10, createTime);
			DBUtil.set(pstmt, 11, createAdminId);
			
			pstmt.execute();
			rs = pstmt.getGeneratedKeys();
			if (!rs.next()) {
				throw new RuntimeException("cann't generate survey key");
			}
			surveyId = rs.getInt(1);
		} finally {
			DBUtil.closeQuietly(rs);
			DBUtil.closeQuietly(pstmt);
		}
		
		insertQuestion(conn, companyId, surveyId, createTime, createAdminId, questionList);
		
		return surveyId;
	}
	
	/**
	 * 更新调研信息
	 * @param conn
	 * @param surveyId       调研编号
	 * @param surveyName     调研名称
	 * @param surveyDesc     调研描述
	 * @param imageName      调研图标名称
	 * @param startTime      开始时间
	 * @param endTime        结束时间
	 * @param type           结果查看类型
	 * @param allowModelId   访问模型
	 * @param now            更新时间
	 * @param updateAdminId  更新管理员id
	 * @param questionIdList 问题id有序列表
	 * @throws SQLException
	 */
	public static void updateSurvey(Connection conn, long companyId, int surveyId,
			String surveyName, String surveyDesc, @Nullable String imageName,
			int startTime, int endTime, SurveyProtos.ShowResultType type,
			@Nullable Integer allowModelId, int now, long updateAdminId) throws SQLException {
		StringBuilder sql = new StringBuilder();
		sql.append("UPDATE weizhu_survey SET survey_name = '").append(DBUtil.SQL_STRING_ESCAPER.escape(surveyName)).append("'");
		sql.append(", survey_desc = '").append(DBUtil.SQL_STRING_ESCAPER.escape(surveyDesc)).append("'");
		
		if (imageName != null) {
			sql.append(", image_name = '").append(DBUtil.SQL_STRING_ESCAPER.escape(imageName)).append("'");
		}
		
		sql.append(", start_time = ").append(startTime);
		sql.append(", end_time = ").append(endTime);
		sql.append(", show_result_type = '").append(type.name()).append("'");
		
		if (allowModelId != null) {
			sql.append(", allow_model_id = ").append(allowModelId);
		}
		
		sql.append(", update_time = ").append(now);
		sql.append(", update_admin_id = ").append(updateAdminId);
		sql.append(" WHERE survey_id = ").append(surveyId);
		sql.append(" AND company_id = ").append(companyId);
		sql.append("; ");
		
		Statement stmt = null;
		try {
			stmt = conn.createStatement();
			stmt.executeUpdate(sql.toString());
		} finally {
			DBUtil.closeQuietly(stmt);
		}
	}
	
	/**
	 * 创建调研的问题
	 * @param conn
	 * @param surveyId
	 * @param questions
	 * @return
	 * @throws SQLException 
	 */
	public static List<Integer> insertQuestion(Connection conn, long companyId,
			@Nullable Integer surveyId, int createTime, long createAdminId,
			List<SurveyProtos.Question> questionList) throws SQLException {
		if (questionList.isEmpty()) {
			return Collections.emptyList();
		}
		
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		Statement stmt = null;
		
		try {
			pstmt = conn.prepareStatement("INSERT INTO weizhu_survey_question (company_id, question_name, image_name, is_optional, type, state, create_time, create_admin_id) VALUES (?, ?, ?, ?, ?, ?, ?, ?); ", Statement.RETURN_GENERATED_KEYS);
			for (SurveyProtos.Question question : questionList) {
				DBUtil.set(pstmt, 1, companyId);
				DBUtil.set(pstmt, 2, question.getQuestionName());
				DBUtil.set(pstmt, 3, question.getImageName());
				DBUtil.set(pstmt, 4, question.getIsOptional());
				DBUtil.set(pstmt, 5, question.getTypeCase().name());
				DBUtil.set(pstmt, 6, question.getState().name());
				DBUtil.set(pstmt, 7, createTime);
				DBUtil.set(pstmt, 8, createAdminId);
				
				pstmt.addBatch();
			}
			pstmt.executeBatch();
			rs = pstmt.getGeneratedKeys();
			
			List<Integer> questionIdList = new ArrayList<Integer>();
			while (rs.next()) {
				questionIdList.add(rs.getInt(1));
			}
			DBUtil.closeQuietly(rs);
			rs = null;
			DBUtil.closeQuietly(pstmt);
			pstmt = null;
			
			if (!questionIdList.isEmpty()) {
				pstmt = conn.prepareStatement("INSERT INTO weizhu_survey_join_question (company_id, survey_id, question_id) VALUES (?, ?, ?); ");
				for (int questionId : questionIdList) {
					pstmt.setLong(1, companyId);
					pstmt.setInt(2, surveyId);
					pstmt.setInt(3, questionId);
					
					pstmt.addBatch();
				}
				pstmt.executeBatch();
				DBUtil.closeQuietly(pstmt);
				pstmt = null;
				
				Iterator<Integer> it = questionIdList.iterator();
				StringBuilder sql = new StringBuilder();
				for (SurveyProtos.Question question : questionList) {

					int questionId = it.next();
					if (question.getTypeCase().equals(SurveyProtos.Question.TypeCase.VOTE)) {
						
						sql.append("INSERT INTO weizhu_survey_vote_question (company_id, question_id, check_number) VALUES (").append(companyId).append(", ");
						sql.append(questionId).append(", ").append(question.getVote().getCheckNum()).append("); ");
						
						for (SurveyProtos.Vote.Option option : question.getVote().getOptionList()) {
							if (option.hasImageName()) {
								
								sql.append("INSERT INTO weizhu_survey_vote_option (company_id, option_name, question_id, image_name) VALUES (").append(companyId).append(", '");
								sql.append(DBUtil.SQL_STRING_ESCAPER.escape(option.getOptionName())).append("', ").append(questionId).append(", '").append(DBUtil.SQL_STRING_ESCAPER.escape(option.getImageName())).append("'); ");
							} else {
								
								sql.append("INSERT INTO weizhu_survey_vote_option (company_id, option_name, question_id) VALUES (").append(companyId).append(", '");
								sql.append(DBUtil.SQL_STRING_ESCAPER.escape(option.getOptionName())).append("', ").append(questionId).append("); ");
							}
						}
						
					} else if (question.getTypeCase().equals(SurveyProtos.Question.TypeCase.INPUT_SELECT)) {
						
						for (SurveyProtos.InputSelect.Option option : question.getInputSelect().getOptionList()) {
							
							sql.append("INSERT INTO weizhu_survey_input_select_option (company_id, option_name, question_id) VALUES (").append(companyId).append(", '");
							sql.append(option.getOptionName()).append("', ").append(questionId).append("); ");
						}
						
					} else if (question.getTypeCase().equals(SurveyProtos.Question.TypeCase.INPUT_TEXT)) {
						
						sql.append("INSERT INTO weizhu_survey_input_text_question (company_id, question_id, input_prompt) VALUES (").append(companyId).append(", ");
						sql.append(questionId).append(", '").append(DBUtil.SQL_STRING_ESCAPER.escape(question.getInputText().getInputPrompt())).append("'); ");
					}
				}
				
				if (sql.length() != 0) {
					stmt = conn.createStatement();
					stmt.executeUpdate(sql.toString());
				}
			}
			
			return questionIdList;
		} finally {
			DBUtil.closeQuietly(rs);
			DBUtil.closeQuietly(pstmt);
			DBUtil.closeQuietly(stmt);
		}
	}
	
	/**
	 * 更新题目信息
	 * @param conn
	 * @param surveyId
	 * @param updateTime
	 * @param updateAdminId
	 * @param questionList
	 * @throws SQLException
	 */
	public static void updateQuestion(Connection conn, long companyId,
			int updateTime, long updateAdminId,
			Collection<SurveyProtos.Question> questions) throws SQLException {
		if (questions.isEmpty()) {
			return ;
		}
		
		StringBuilder updateSQL = new StringBuilder();

		Statement stmt = null;
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try {
			stmt = conn.createStatement();
			
			for (SurveyProtos.Question question : questions) {
				updateSQL.append("UPDATE weizhu_survey_question SET question_name = '").append(DBUtil.SQL_STRING_ESCAPER.escape(question.getQuestionName())).append("'");
				if (question.hasImageName()) {
					updateSQL.append(", image_name = '").append(DBUtil.SQL_STRING_ESCAPER.escape(question.getImageName())).append("'");
				}
				updateSQL.append(", is_optional = ").append(question.getIsOptional());
				updateSQL.append(", type = '").append(question.getTypeCase().name()).append("'");
				updateSQL.append(", state = '").append(question.getState().name()).append("'");
				updateSQL.append(", update_time = ").append(updateTime);
				updateSQL.append(", update_admin_id = ").append(updateAdminId);
				updateSQL.append(" WHERE question_id = ").append(question.getQuestionId());
				updateSQL.append(" AND company_id = ").append(companyId);
				updateSQL.append("; ");
			
				if (question.getTypeCase().equals(SurveyProtos.Question.TypeCase.VOTE)) {
					updateSQL.append("UPDATE weizhu_survey_vote_question SET check_number = ").append(question.getVote().getCheckNum())
					.append(" WHERE company_id = ").append(companyId)
					.append(" AND question_id = ").append(question.getQuestionId()).append("; ");
					
					pstmt = conn.prepareStatement("SELECT option_id FROM weizhu_survey_vote_option WHERE question_id = ?; ");
					pstmt.setInt(1, question.getQuestionId());
					
					rs = pstmt.executeQuery();
					List<Integer> optionIdList = new ArrayList<Integer>();
					while (rs.next()) {
						optionIdList.add(rs.getInt("option_id"));
					}
					List<Integer> delOptionIdList = new ArrayList<Integer>(optionIdList);
					for (SurveyProtos.Vote.Option option : question.getVote().getOptionList()) {
						if (optionIdList.contains(option.getOptionId())) {
							delOptionIdList.remove(new Integer(option.getOptionId()));
						}
					}
					
					DBUtil.closeQuietly(rs);
					rs = null;
					DBUtil.closeQuietly(pstmt);
					pstmt = null;
					
					if (!delOptionIdList.isEmpty()) {
						updateSQL.append("DELETE FROM weizhu_survey_vote_option WHERE option_id IN (").append(DBUtil.COMMA_JOINER.join(delOptionIdList)).append("); ");
					}
					
					for (SurveyProtos.Vote.Option option : question.getVote().getOptionList()) {
						if (option.hasImageName()) {
							// 判断是否新增选项
							if (option.getOptionId() == 0) {
								updateSQL.append("REPLACE INTO weizhu_survey_vote_option (company_id, option_name, question_id, image_name) VALUES ")
								.append(companyId).append(", '")
								.append(DBUtil.SQL_STRING_ESCAPER.escape(option.getOptionName())).append("', ")
								.append(question.getQuestionId()).append(", '")
								.append(DBUtil.SQL_STRING_ESCAPER.escape(option.getImageName())).append("'); ");
							} else {
								updateSQL.append("REPLACE INTO weizhu_survey_vote_option (company_id, option_id, option_name, question_id, image_name) VALUES ")
								.append(companyId).append(", ")
								.append(option.getOptionId()).append(", '")
								.append(DBUtil.SQL_STRING_ESCAPER.escape(option.getOptionName())).append("', ")
								.append(question.getQuestionId()).append(", '")
								.append(DBUtil.SQL_STRING_ESCAPER.escape(option.getImageName())).append("'); ");
							}
						} else {
							// 判断是否是新增选项
							if (option.getOptionId() == 0) {
								updateSQL.append("REPLACE INTO weizhu_survey_vote_option (company_id, option_name, question_id) VALUES (")
								.append(companyId).append(", '")
								.append(DBUtil.SQL_STRING_ESCAPER.escape(option.getOptionName())).append("', ")
								.append(question.getQuestionId()).append("); ");
							} else {
								updateSQL.append("REPLACE INTO weizhu_survey_vote_option (company_id, option_id, option_name, question_id) VALUES (")
								.append(companyId).append(", ")
								.append(option.getOptionId()).append(", '")
								.append(DBUtil.SQL_STRING_ESCAPER.escape(option.getOptionName())).append("', ")
								.append(question.getQuestionId()).append("); ");
							}
							
						}
					}
					
				} else if (question.getTypeCase().equals(SurveyProtos.Question.TypeCase.INPUT_SELECT)) {
					
					pstmt = conn.prepareStatement("SELECT option_id FROM weizhu_survey_input_select_option WHERE question_id = ?; ");
					pstmt.setInt(1, question.getQuestionId());
					
					rs = pstmt.executeQuery();
					List<Integer> optionIdList = new ArrayList<Integer>();
					while (rs.next()) {
						optionIdList.add(rs.getInt("option_id"));
					}
					List<Integer> delOptionIdList = new ArrayList<Integer>(optionIdList);
					for (SurveyProtos.Vote.Option option : question.getVote().getOptionList()) {
						if (optionIdList.contains(option.getOptionId())) {
							delOptionIdList.remove(new Integer(option.getOptionId()));
						}
					}
					
					DBUtil.closeQuietly(rs);
					rs = null;
					DBUtil.closeQuietly(pstmt);
					pstmt = null;
					
					if (!delOptionIdList.isEmpty()) {
						updateSQL.append("DELETE FROM weizhu_survey_input_select_option WHERE option_id IN (").append(DBUtil.COMMA_JOINER.join(delOptionIdList)).append("); ");
					}
					
					for (SurveyProtos.InputSelect.Option option : question.getInputSelect().getOptionList()) {
						if (option.getOptionId() == 0) {
							updateSQL.append("REPLACE INTO weizhu_survey_input_select_option (company_id, option_name, question_id) VALUES (")
							.append(companyId).append(", '")
							.append(DBUtil.SQL_STRING_ESCAPER.escape(option.getOptionName())).append("', ")
							.append(question.getQuestionId()).append("); ");
						} else {
							updateSQL.append("REPLACE INTO weizhu_survey_input_select_option (company_id, option_id, option_name, question_id) VALUES (")
							.append(companyId).append(", ")
							.append(option.getOptionId()).append(", '")
							.append(DBUtil.SQL_STRING_ESCAPER.escape(option.getOptionName())).append("', ")
							.append(question.getQuestionId()).append("); ");
						}
					}
					
				} else if (question.getTypeCase().equals(SurveyProtos.Question.TypeCase.INPUT_TEXT)) {
					
					updateSQL.append("UPDATE weizhu_survey_input_text_question SET question_id = ").append(question.getQuestionId())
					.append(", input_prompt = '").append(DBUtil.SQL_STRING_ESCAPER.escape(question.getInputText().getInputPrompt())).append("'")
					.append(" WHERE question_id = ").append(question.getQuestionId())
					.append(" AND company_id = ").append(companyId)
					.append("; ");
					
				}
			}
			
			stmt = conn.createStatement();
			stmt.execute(updateSQL.toString());
		} finally {
			DBUtil.closeQuietly(rs);
			DBUtil.closeQuietly(pstmt);
			DBUtil.closeQuietly(stmt);
		}
	}
	
	/**
	 * 删除考题
	 * @param conn
	 * @param questionIds
	 * @throws SQLException 
	 */
	public static void deleteQuestion(Connection conn, long companyId, int surveyId, Collection<Integer> questionIds) throws SQLException {
		if (questionIds.isEmpty()) {
			return ;
		}
		
		StringBuilder sql = new StringBuilder();
		sql.append("DELETE FROM weizhu_survey_join_question WHERE company_id = ").append(companyId);
		sql.append(" AND survey_id = ").append(surveyId);
		sql.append(" AND question_id IN (").append(DBUtil.COMMA_JOINER.join(questionIds));
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
	 * 禁用调研
	 * @param conn
	 * @param surveyIds
	 * @throws SQLException
	 */
	public static void disableSurvey(Connection conn, long companyId, Collection<Integer> surveyIds) throws SQLException {
		if (surveyIds.isEmpty()) {
			return ;
		}
		
		StringBuilder sql = new StringBuilder();
		sql.append("UPDATE weizhu_survey SET state = 'DISABLE' WHERE company_id = ").append(companyId).append(" AND survey_id IN (");
		sql.append(DBUtil.COMMA_JOINER.join(surveyIds));
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
	 * 启用调研
	 * @param conn
	 * @param companyId
	 * @param surveyIds
	 * @throws SQLException
	 */
	public static void enableSurvey(Connection conn, long companyId, Collection<Integer> surveyIds) throws SQLException {
		if (surveyIds.isEmpty()) {
			return ;
		}
		
		StringBuilder sql = new StringBuilder();
		sql.append("UPDATE weizhu_survey SET state = 'NORMAL' WHERE company_id = ").append(companyId).append(" AND survey_id IN (");
		sql.append(DBUtil.COMMA_JOINER.join(surveyIds));
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
	 * 删除调研
	 * @param conn
	 * @param surveyIds
	 * @throws SQLException
	 */
	public static void deleteSurvey(Connection conn, long companyId, Collection<Integer> surveyIds) throws SQLException {
		if (surveyIds.isEmpty()) {
			return ;
		}
		
		StringBuilder sql = new StringBuilder();
		sql.append("UPDATE weizhu_survey SET state = 'DELETE' WHERE company_id = ").append(companyId).append(" AND survey_id IN (");
		sql.append(DBUtil.COMMA_JOINER.join(surveyIds));
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
	 * 获取调研统计数据
	 * @param conn
	 * @param surveyIds
	 * @return
	 * @throws SQLException 
	 */
	public static Map<Integer, SurveyDAOProtos.SurveyCount> getSurveyCount(Connection conn, long companyId, Collection<Integer> surveyIds) throws SQLException {
		if (surveyIds.isEmpty()) {
			return Collections.emptyMap();
		}
		
		StringBuilder surveyCountSQL = new StringBuilder();
		surveyCountSQL.append("SELECT survey_id, COUNT(1) AS count FROM weizhu_survey_result WHERE company_id = ").append(companyId).append(" AND survey_id IN (");
		surveyCountSQL.append(DBUtil.COMMA_JOINER.join(surveyIds)).append(") GROUP BY survey_id; ");

		Statement stmt = null;
		ResultSet rs = null;
		try {
			stmt = conn.createStatement();
			rs = stmt.executeQuery(surveyCountSQL.toString());
			
			Map<Integer, SurveyDAOProtos.SurveyCount> surveyCountMap = new HashMap<Integer, SurveyDAOProtos.SurveyCount>();
			SurveyDAOProtos.SurveyCount.Builder surveyCountBuilder = SurveyDAOProtos.SurveyCount.newBuilder();
			while (rs.next()) {
				int surveyId = rs.getInt("survey_id");
				surveyCountBuilder.setSurveyId(surveyId);
				surveyCountBuilder.setSurveyCount(rs.getInt("count"));
				
				surveyCountMap.put(surveyId, surveyCountBuilder.build());
			}
			
			return surveyCountMap;
		} finally {
			DBUtil.closeQuietly(rs);
			DBUtil.closeQuietly(stmt);
		}
	}
	
	/**
	 * 
	 * @param conn
	 * @param questionIds
	 * @return
	 * @throws SQLException
	 */
	public static Map<Integer, SurveyDAOProtos.QuestionCount> getQuestionCount(Connection conn, long companyId, Collection<Integer> questionIds) throws SQLException {
		if (questionIds.isEmpty()) {
			return Collections.emptyMap();
		}
		
		Statement stmt = null;
		ResultSet rs = null;
		
		try {
			StringBuilder questionCountSQL = new StringBuilder();
			questionCountSQL.append("SELECT question_id, user_id AS count FROM weizhu_survey_vote_answer WHERE company_id = ").append(companyId).append(" AND question_id IN (");
			questionCountSQL.append(DBUtil.COMMA_JOINER.join(questionIds));
			questionCountSQL.append(") GROUP BY question_id, user_id; ");
			
			stmt = conn.createStatement();
			stmt.execute(questionCountSQL.toString());
			rs = stmt.getResultSet();
			
			Map<Integer, Integer> questionCountMap = new HashMap<Integer, Integer>();
			while (rs.next()) {
				int questionId = rs.getInt("question_id");
				Integer count = questionCountMap.get(questionId);
				if (questionCountMap.get(questionId) != null) {
					count++;
				} else {
					count = 1;
				}
				questionCountMap.put(questionId, count);
			}
			DBUtil.closeQuietly(rs);
			rs = null;
			DBUtil.closeQuietly(stmt);
			stmt = null;
			
			StringBuilder optionCountSQL = new StringBuilder();
			optionCountSQL.append("SELECT question_id, option_id, COUNT(user_id) AS count FROM weizhu_survey_vote_answer WHERE company_id = ").append(companyId).append(" AND question_id IN (");
			optionCountSQL.append(DBUtil.COMMA_JOINER.join(questionIds));
			optionCountSQL.append(") GROUP BY option_id; ");
			
			stmt = conn.createStatement();
			rs = stmt.executeQuery(optionCountSQL.toString());
			Map<Integer, SurveyDAOProtos.QuestionCount> result = new HashMap<Integer, SurveyDAOProtos.QuestionCount>();
			while (rs.next()) {
				int questionId = rs.getInt("question_id");
				SurveyDAOProtos.QuestionCount questionCount = result.get(questionId);
				SurveyDAOProtos.QuestionCount.OptionCount optionCount = SurveyDAOProtos.QuestionCount.OptionCount.newBuilder()
						.setOptionId(rs.getInt("option_id"))
						.setOptionCount(rs.getInt("count"))
						.build();
				if (questionCount != null) {
					result.put(questionId, SurveyDAOProtos.QuestionCount.newBuilder()
							.mergeFrom(questionCount).addOptionCount(optionCount).build());
				} else {
					result.put(questionId, SurveyDAOProtos.QuestionCount.newBuilder()
							.setQuestionId(questionId)
							.setQuestionCount(questionCountMap.get(questionId) == null ? 0 : questionCountMap.get(questionId))
							.addOptionCount(optionCount).build());
				}
			}
			return result;
		} finally {
			DBUtil.closeQuietly(rs);
			DBUtil.closeQuietly(stmt);
		}
	}
	
	public static DataPage<Integer> getSurveyIdPage(Connection conn, long companyId, int start, int length, @Nullable String surveyName) throws SQLException {
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		
		try {
			if (surveyName == null) {
				pstmt = conn.prepareStatement("SELECT COUNT(*) AS total FROM weizhu_survey WHERE company_id = ? AND state != 'DELETE'; SELECT survey_id FROM weizhu_survey WHERE company_id = ? AND state != 'DELETE' ORDER BY survey_id DESC LIMIT ?, ?; ");
				pstmt.setLong(1, companyId);
				pstmt.setLong(2, companyId);
				pstmt.setInt(3, start);
				pstmt.setLong(4, length);
			} else {
				pstmt = conn.prepareStatement("SELECT COUNT(*) AS total FROM weizhu_survey WHERE company_id = ? AND state != 'DELETE' AND survey_name LIKE ?; SELECT survey_id FROM weizhu_survey WHERE company_id = ? AND state != 'DELETE' AND survey_name LIKE ? ORDER BY survey_id DESC LIMIT ?, ?; ");
				pstmt.setLong(1, companyId);
				pstmt.setString(2, "%" + DBUtil.SQL_LIKE_STRING_ESCAPER.escape(surveyName) + "%");
				pstmt.setLong(3, companyId);
				pstmt.setString(4, "%" + DBUtil.SQL_LIKE_STRING_ESCAPER.escape(surveyName) + "%");
				pstmt.setInt(5, start);
				pstmt.setLong(6, length);
			}

			pstmt.execute();
			rs = pstmt.getResultSet();

			if (!rs.next()) {
				throw new RuntimeException("cannt get the total!");
			}
			int total = rs.getInt("total");
			DBUtil.closeQuietly(rs);
			rs = null;

			pstmt.getMoreResults();
			rs = pstmt.getResultSet();

			List<Integer> surveyIdList = new ArrayList<Integer>();
			while (rs.next()) {
				surveyIdList.add(rs.getInt("survey_id"));
			}
			
			return new DataPage<Integer>(surveyIdList, total, total);
		} finally {
			DBUtil.closeQuietly(rs);
			DBUtil.closeQuietly(pstmt);
		}
	}
	
	/**
	 * 更新考题顺序
	 * @param conn
	 * @param companyId
	 * @param surveyId
	 * @param questionIdList
	 * @throws SQLException
	 */
	public static void updateQuestionOrder(Connection conn, long companyId, int surveyId, List<Integer> questionIdList) throws SQLException {
		if (questionIdList.isEmpty()) {
			return ;
		}
		
		StringBuilder sql = new StringBuilder("UPDATE weizhu_survey SET question_order_str = '");
		sql.append(DBUtil.COMMA_JOINER.join(questionIdList));
		sql.append("' WHERE company_id = ").append(companyId);
		sql.append(" AND survey_id = ").append(surveyId);
		sql.append("; ");
		
		Statement stmt = null;
		try {
			stmt = conn.createStatement();
			stmt.executeUpdate(sql.toString());
		} finally {
			DBUtil.closeQuietly(stmt);
		}
	}
	
	/**
	 * 复制调研
	 * @param conn
	 * @param companyId
	 * @param surveyId
	 * @return
	 * @throws SQLException 
	 */
	public static int copySurvey(Connection conn, long companyId, int surveyId, String surveyName, 
			int startTime, int endTime, @Nullable Integer allowModelId, 
			int now, long createAdminId, List<SurveyProtos.Question> questionList) throws SQLException {
		
		
		// 复制一条新的调研
		StringBuilder sql = new StringBuilder();
		sql.append("INSERT INTO weizhu_survey (company_id, survey_id, survey_name, survey_desc, image_name, start_time, end_time,")
				.append(" show_result_type, allow_model_id, question_order_str, state, create_time, create_admin_id)")
				.append(" SELECT s.company_id, NULL, '")
				.append(DBUtil.SQL_STRING_ESCAPER.escape(surveyName))
				.append("', s.survey_desc, s.image_name, ")
				.append(startTime).append(", ")
				.append(endTime)
				.append(", s.show_result_type, ")
				.append(allowModelId == null ? "NULL" : allowModelId.intValue())
				.append(", '', s.state, ")
				.append(now).append(", ")
				.append(createAdminId)
				.append(" FROM weizhu_survey s WHERE company_id = ")
				.append(companyId).append(" AND survey_id = ").append(surveyId).append("; ");
		
		Statement stmt = null;
		ResultSet rs = null;
		
		int newSurveyId = 0;
		try {
			stmt = conn.createStatement();
			stmt.execute(sql.toString(), Statement.RETURN_GENERATED_KEYS);
			rs = stmt.getGeneratedKeys();
			if (!rs.next()) {
				throw new RuntimeException("cann't generate survey key");
			}
			newSurveyId = rs.getInt(1);
		} finally {
			DBUtil.closeQuietly(stmt);
		}
		
		insertQuestion(conn, companyId, newSurveyId, now, createAdminId, questionList);
		
		return newSurveyId;
	}
	
}
