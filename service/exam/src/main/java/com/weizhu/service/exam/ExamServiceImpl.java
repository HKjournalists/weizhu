package com.weizhu.service.exam;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import com.google.common.primitives.Ints;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.inject.Inject;
import com.weizhu.common.db.DBUtil;
import com.weizhu.proto.AllowProtos.CheckAllowRequest;
import com.weizhu.proto.AllowProtos.CheckAllowResponse;
import com.weizhu.proto.AllowService;
import com.weizhu.proto.ExamProtos;
import com.weizhu.proto.ExamProtos.Exam;
import com.weizhu.proto.ExamProtos.GetClosedExamListRequest;
import com.weizhu.proto.ExamProtos.GetClosedExamListResponse;
import com.weizhu.proto.ExamProtos.GetExamByIdRequest;
import com.weizhu.proto.ExamProtos.GetExamByIdResponse;
import com.weizhu.proto.ExamProtos.GetExamInfoRequest;
import com.weizhu.proto.ExamProtos.GetExamInfoResponse;
import com.weizhu.proto.ExamProtos.GetOpenExamCountResponse;
import com.weizhu.proto.ExamProtos.GetOpenExamListRequest;
import com.weizhu.proto.ExamProtos.GetOpenExamListResponse;
import com.weizhu.proto.ExamProtos.Option;
import com.weizhu.proto.ExamProtos.SaveAnswerRequest;
import com.weizhu.proto.ExamProtos.SaveAnswerResponse;
import com.weizhu.proto.ExamProtos.SubmitExamRequest;
import com.weizhu.proto.ExamProtos.SubmitExamResponse;
import com.weizhu.proto.ExamProtos.UserResult;
import com.weizhu.proto.ExamService;
import com.weizhu.proto.WeizhuProtos.EmptyRequest;
import com.weizhu.proto.WeizhuProtos.RequestHead;
import com.zaxxer.hikari.HikariDataSource;

import redis.clients.jedis.JedisPool;

public class ExamServiceImpl implements ExamService {
	
	private final HikariDataSource hikariDataSource;
	private final JedisPool jedisPool;
	private final AllowService allowService;
	
	@Inject
	public ExamServiceImpl(HikariDataSource hikariDataSource, JedisPool jedisPool,
			AllowService allowService){
		this.hikariDataSource = hikariDataSource;
		this.jedisPool = jedisPool;
		this.allowService = allowService;
	}

	@Override
	public ListenableFuture<GetOpenExamListResponse> getOpenExamList(RequestHead head, GetOpenExamListRequest request) {
		final long companyId = head.getSession().getCompanyId();
		final long userId = head.getSession().getUserId();
		
		final int size = request.getSize() > 0 && request.getSize() <= 50 ? request.getSize() : 10;
		final Integer lastExamId = request.hasLastExamId() ? request.getLastExamId() : null;
		final Integer lastExamEndTime = request.hasLastExamEndTime() ? request.getLastExamEndTime() : null;
		
		final int now = (int) (System.currentTimeMillis() / 1000L);
		
		List<Integer> examIdList;
		Map<Integer, ExamProtos.UserResult> userResultMap;
		
		Connection dbConn = null;
		try{
			dbConn = this.hikariDataSource.getConnection();
			examIdList = ExamDB.getOpenExamIdList(dbConn, companyId, now, lastExamId, lastExamEndTime);
			userResultMap = ExamDB.getUserResult(dbConn, companyId, userId, examIdList);
		} catch (SQLException ex) {
			throw new RuntimeException(ex);
		} finally {
			DBUtil.closeQuietly(dbConn);
		}
		
		Map<Integer, ExamDAOProtos.ExamInfo> examInfoMap = ExamUtil.getExamInfo(hikariDataSource, jedisPool, companyId, examIdList);
		
		examInfoMap = this.checkExamAllow(head, examInfoMap, userId);
		
		// 挑选 能参加且未交卷的考试 ,注意保证db返回的顺序
		List<ExamProtos.Exam> examList = new ArrayList<ExamProtos.Exam>(examInfoMap.size());
		for (Integer examId : examIdList) {
			ExamDAOProtos.ExamInfo examInfo = examInfoMap.get(examId);
			ExamProtos.UserResult userResult = userResultMap.get(examId);
			
			if (examInfo != null && (userResult == null || !userResult.hasSubmitTime())) {
				examList.add(examInfo.getExam());
			}
		}
		return Futures.immediateCheckedFuture(GetOpenExamListResponse.newBuilder()
				.setHasMore(examList.size() > size)
				.addAllExam(examList.size() > size ? examList.subList(0, size) : examList)
				.build());
	}
	
	@Override
	public ListenableFuture<GetOpenExamCountResponse> getOpenExamCount(RequestHead head, EmptyRequest request) {
		final long companyId = head.getSession().getCompanyId();
		final long userId = head.getSession().getUserId();
		
		final int now = (int) (System.currentTimeMillis() / 1000L);
		
		List<Integer> examIdList;
		Map<Integer, ExamProtos.UserResult> userResultMap;
		
		Connection dbConn = null;
		try{
			dbConn = this.hikariDataSource.getConnection();
			examIdList = ExamDB.getOpenExamIdList(dbConn, companyId, now, null, null);
			userResultMap = ExamDB.getUserResult(dbConn, companyId, userId, examIdList);
		} catch (SQLException ex) {
			throw new RuntimeException(ex);
		} finally {
			DBUtil.closeQuietly(dbConn);
		}
		
		Map<Integer, ExamDAOProtos.ExamInfo> examInfoMap = ExamUtil.getExamInfo(hikariDataSource, jedisPool, companyId, examIdList);
		
		examInfoMap = this.checkExamAllow(head, examInfoMap, userId);
		
		// 挑选 能参加且未交卷的考试 ,注意保证db返回的顺序
		int openExamCount = 0;
		for (Integer examId : examIdList) {
			ExamDAOProtos.ExamInfo examInfo = examInfoMap.get(examId);
			ExamProtos.UserResult userResult = userResultMap.get(examId);
			
			if (examInfo != null && (userResult == null || !userResult.hasSubmitTime())) {
				openExamCount ++;
			}
		}
		
		return Futures.immediateFuture(GetOpenExamCountResponse.newBuilder()
				.setOpenExamCount(openExamCount)
				.build());
	}
	
	private static class ExamInfo implements Comparable<ExamInfo> {
		final ExamProtos.Exam exam;
		final ExamProtos.UserResult userResult;
		final int finishTime;
		ExamInfo(Exam exam, UserResult userResult) {
			this.exam = exam;
			this.userResult = userResult;
			this.finishTime = (userResult != null && userResult.hasSubmitTime()) ? userResult.getSubmitTime() : exam.getEndTime();
		}
		
		@Override
		public int compareTo(ExamInfo o) {
			return -Ints.compare(finishTime, o.finishTime);
		}
	}

	@Override
	public ListenableFuture<GetClosedExamListResponse> getClosedExamList(RequestHead head, GetClosedExamListRequest request) {
		final long companyId = head.getSession().getCompanyId();
		final long userId = head.getSession().getUserId();
		
		final int size = request.getSize() > 0 && request.getSize() <= 50 ? request.getSize() : 10;
		final Integer lastExamId = request.hasLastExamId() ? request.getLastExamId() : null;
		final Integer lastExamSubmitTime = request.hasLastExamSubmitTime() ? request.getLastExamSubmitTime() : null;
		
		final int now = (int) (System.currentTimeMillis() / 1000L);
		
		Set<Integer> examIdSet = new TreeSet<Integer>();
		Map<Integer, ExamProtos.UserResult> userResultMap;
		
		Connection dbConn = null;
		try {
			dbConn = this.hikariDataSource.getConnection();
			examIdSet.addAll(ExamDB.getClosedExamIdList(dbConn, companyId, now, lastExamId, lastExamSubmitTime));
			examIdSet.addAll(ExamDB.getSubmitExamIdList(dbConn, companyId, lastExamId, lastExamSubmitTime));
			userResultMap = ExamDB.getUserResult(dbConn, companyId, userId, examIdSet);
		} catch (SQLException ex) {
			throw new RuntimeException(ex);
		} finally {
			DBUtil.closeQuietly(dbConn);
		}
		
		Map<Integer, ExamDAOProtos.ExamInfo> examInfoMap = ExamUtil.getExamInfo(hikariDataSource, jedisPool, companyId, examIdSet);
		
		examInfoMap = this.checkExamAllow(head, examInfoMap, userId);
		
		List<ExamInfo> examInfoList = new ArrayList<ExamInfo>();
		for (ExamDAOProtos.ExamInfo examInfo : examInfoMap.values()) {
			ExamProtos.UserResult userResult = userResultMap.get(examInfo.getExam().getExamId());
			if ((userResult != null && userResult.hasSubmitTime()) 
					|| examInfo.getExam().getEndTime() <= now) {
				examInfoList.add(new ExamInfo(examInfo.getExam(), userResult));
			}
		}
		
		Collections.sort(examInfoList);
		
		GetClosedExamListResponse.Builder responseBuilder = GetClosedExamListResponse.newBuilder();
		responseBuilder.setHasMore(examInfoList.size() > size);
		for (int i=0; i<examInfoList.size() && i<size; ++i) {
			ExamInfo examInfo = examInfoList.get(i);
			responseBuilder.addExam(examInfo.exam);
			if (examInfo.userResult != null) {
				responseBuilder.addUserResult(examInfo.userResult);
			}
		}
		
		return Futures.immediateCheckedFuture(responseBuilder.build());
	}

	@Override
	public ListenableFuture<GetExamByIdResponse> getExamById(RequestHead head, GetExamByIdRequest request) {
		final long companyId = head.getSession().getCompanyId();
		
		if (request.getExamIdCount() <= 0) {
			return Futures.immediateFuture(GetExamByIdResponse.newBuilder().build());
		}
		
		Map<Integer, ExamDAOProtos.ExamInfo> examInfoMap = ExamUtil.getExamInfo(hikariDataSource, jedisPool, companyId, request.getExamIdList());
		
		examInfoMap = this.checkExamAllow(head, examInfoMap, head.getSession().getUserId());
		
		GetExamByIdResponse.Builder responseBuilder = GetExamByIdResponse.newBuilder();
		for (ExamDAOProtos.ExamInfo examInfo : examInfoMap.values()) {
			responseBuilder.addExam(examInfo.getExam());
		}
		return Futures.immediateFuture(responseBuilder.build());
	}

	@Override
	public ListenableFuture<GetExamInfoResponse> getExamInfo(RequestHead head, GetExamInfoRequest request) {
		final long companyId = head.getSession().getCompanyId();
		final long userId = head.getSession().getUserId();
		final int examId = request.getExamId();
		
		ExamDAOProtos.ExamInfo examInfo = ExamUtil.getExamInfo(hikariDataSource, jedisPool, companyId, Collections.singleton(examId)).get(examId);
	
		if (examInfo == null) {
			return Futures.immediateFuture(GetExamInfoResponse.newBuilder().build());
		}
		
		final int now = (int) (System.currentTimeMillis() / 1000L);
		
		final GetExamInfoResponse.ExamState examState;
		if (now < examInfo.getExam().getStartTime()) {
			examState = GetExamInfoResponse.ExamState.EXAM_NOT_START;
		} else if (now < examInfo.getExam().getEndTime()) {
			examState = GetExamInfoResponse.ExamState.EXAM_RUNNING;
		} else {
			examState = GetExamInfoResponse.ExamState.EXAM_FINISH;
		}
		
		final boolean isJoin = !examInfo.getExam().hasAllowModelId() || checkAllow(head, examInfo.getExam().getAllowModelId(), userId);
		
		final Map<Integer, Set<Integer>> userAnswerOptionMap;
		final ExamProtos.UserResult userResult;
		final Map<Integer, Integer> questionScoreMap;
		
		Connection dbConn = null;
		try {
			dbConn = this.hikariDataSource.getConnection();
				
			if ((examState == GetExamInfoResponse.ExamState.EXAM_NOT_START) || !isJoin) {
				userAnswerOptionMap = Collections.emptyMap();
			} else  {
				userAnswerOptionMap = ExamDB.getExamUserAnswer(dbConn, companyId, userId, examId);
			}
			
			if ((examState == GetExamInfoResponse.ExamState.EXAM_NOT_START) || !isJoin) {
				userResult = null;
			} else {
				userResult = ExamDB.getUserResult(dbConn, companyId, userId, Collections.singleton(examId)).get(examId);
				
				if ((userResult == null) && (examState == GetExamInfoResponse.ExamState.EXAM_RUNNING)) {
					ExamDB.saveUserStartTime(dbConn, companyId, examId, userId, now);
				}
			}
			
			// 如果考试未开始 或者 正在考试且用户不可以参加本次考试 则不显示考题
			if ((examState == GetExamInfoResponse.ExamState.EXAM_NOT_START) 
					|| (examState == GetExamInfoResponse.ExamState.EXAM_RUNNING && !isJoin)
					) {
				questionScoreMap = Collections.emptyMap();
			} else {
				LinkedHashMap<Integer, Integer> tmp = null;
				if (examInfo.getExam().getType().equals(ExamProtos.Exam.Type.MANUAL)) {
					tmp = new LinkedHashMap<Integer, Integer>();
					for (ExamDAOProtos.ExamQuestion examQuestion : examInfo.getExamQuestionList()) {
						tmp.put(examQuestion.getQuestionId(), examQuestion.getScore());
					}
				} else if (examInfo.getExam().getType().equals(ExamProtos.Exam.Type.AUTO)) {
					tmp = ExamDB.getExamQuestionScoreRandom(dbConn, companyId, examId, userId, examInfo.getRandomQuestionCategoryIdList(), examInfo.getRandomQuestionNum());
				}
				questionScoreMap = tmp != null ? tmp : Collections.<Integer, Integer>emptyMap();
			}
		} catch (SQLException e) {
			throw new RuntimeException("db fail", e);
		} finally {
			DBUtil.closeQuietly(dbConn);
		}
		
		// 原始的考题信息map
		Map<Integer, ExamProtos.Question> questionMap = ExamUtil.getQuestion(hikariDataSource, jedisPool, companyId, questionScoreMap.keySet());
		// 处理后的考题信息map : 填入分值，去掉 Option is right 字段, 重新排序
		Map<Integer, ExamProtos.Question> questionResolvedMap = new LinkedHashMap<Integer, ExamProtos.Question>(questionMap.size());
		
		ExamProtos.Question.Builder tmpQuestionBuilder = ExamProtos.Question.newBuilder();
		ExamProtos.Option.Builder tmpOptionBuilder = ExamProtos.Option.newBuilder();
		
		for (Entry<Integer, Integer> entry : questionScoreMap.entrySet()) {
			int questionId = entry.getKey();
			int score = entry.getValue();
			ExamProtos.Question question = questionMap.get(questionId);
			
			if (question != null) {
				tmpQuestionBuilder.clear();
				
				// 填入分值
				tmpQuestionBuilder.mergeFrom(question).setScore(score);
				
				// 考试未开始或者 正在考试中且未交卷
				// 考试答案永久不显示，或者考试答案在考试结束后显示
				// 把Option is_right字段干掉
				if ((examState == GetExamInfoResponse.ExamState.EXAM_NOT_START)
						|| ((examState == GetExamInfoResponse.ExamState.EXAM_RUNNING) && (userResult == null || !userResult.hasSubmitTime()))
						|| examInfo.getExam().getShowResult() == ExamProtos.ShowResult.NONE
						|| (examInfo.getExam().getShowResult() == ExamProtos.ShowResult.AFTER_EXAM_END && examState == GetExamInfoResponse.ExamState.EXAM_RUNNING)) {
					
					tmpQuestionBuilder.clearOption();
					for (int i=0; i<question.getOptionCount(); ++i) {
						tmpOptionBuilder.clear();
						
						tmpOptionBuilder.mergeFrom(question.getOption(i));
						tmpOptionBuilder.clearIsRight();
						
						tmpQuestionBuilder.addOption(tmpOptionBuilder.build());
					}
				}
				
				questionResolvedMap.put(questionId, tmpQuestionBuilder.build());
			}
		}
		
		GetExamInfoResponse.Builder responseBuilder = GetExamInfoResponse.newBuilder()
				.setExam(examInfo.getExam())
				.setState(examState)
				.setIsJoin(isJoin)
				.addAllQuestion(questionResolvedMap.values());
		
		ExamProtos.UserAnswer.Builder tmpUserAnswerBuilder = ExamProtos.UserAnswer.newBuilder();
		for (Entry<Integer, Set<Integer>> entry : userAnswerOptionMap.entrySet()) {
			tmpUserAnswerBuilder.clear();
			
			Integer questionId = entry.getKey();
			Set<Integer> optionIdSet = entry.getValue();
			
			tmpUserAnswerBuilder.setQuestionId(questionId).addAllAnswerOptionId(optionIdSet);
			
			// 如果已交卷或者考试已结束，需要标明用户答题是否正确
			if ((userResult != null && userResult.hasSubmitTime()) || (examState == GetExamInfoResponse.ExamState.EXAM_FINISH)) {
				ExamProtos.Question question = questionMap.get(questionId);
				if (question != null) {
					
					boolean isRight = true;
					for (ExamProtos.Option option : question.getOptionList()) {
						if ((option.getIsRight() && !optionIdSet.contains(option.getOptionId()))
								|| (!option.getIsRight() && optionIdSet.contains(option.getOptionId()))) {
							isRight = false;
							break;
						}
					}
					
					tmpUserAnswerBuilder.setIsRight(isRight);
				}
			}
			
			responseBuilder.addUserAnswer(tmpUserAnswerBuilder.build());
		}
		
		if (userResult != null) {
			responseBuilder.setUserResult(userResult);
		}

		return Futures.immediateFuture(responseBuilder.build());
	}

	@Override
	public ListenableFuture<SaveAnswerResponse> saveAnswer(RequestHead head, SaveAnswerRequest request) {
		final long companyId = head.getSession().getCompanyId();
		if (request.getUserAnswerCount() <= 0) {
			return Futures.immediateCheckedFuture(SaveAnswerResponse.newBuilder()
					.setResult(SaveAnswerResponse.Result.SUCC)
					.build());
		}
		
		final int examId = request.getExamId() ;
		final long userId = head.getSession().getUserId();
		
		Map<Integer, Set<Integer>> newExamUserAnswerMap = new TreeMap<Integer, Set<Integer>>();
		for (ExamProtos.UserAnswer userAnswer : request.getUserAnswerList()) {
			Set<Integer> optionIdSet = newExamUserAnswerMap.get(userAnswer.getQuestionId());
			if (optionIdSet == null) {
				optionIdSet = new TreeSet<Integer>();
				newExamUserAnswerMap.put(userAnswer.getQuestionId(), optionIdSet);
			}
			optionIdSet.addAll(userAnswer.getAnswerOptionIdList());
		}
		
		ExamDAOProtos.ExamInfo examInfo = ExamUtil.getExamInfo(hikariDataSource, jedisPool, companyId, Collections.singleton(examId)).get(examId);
		if (examInfo == null) {
			return Futures.immediateCheckedFuture(SaveAnswerResponse.newBuilder()
					.setResult(SaveAnswerResponse.Result.FAIL_EXAM_NOT_EXSIT)
					.setFailText("考试不存在")
					.build());
		}
		
		final int now = (int) (System.currentTimeMillis() / 1000L);
		if (now < examInfo.getExam().getStartTime()) {
			return Futures.immediateCheckedFuture(SaveAnswerResponse.newBuilder()
					.setResult(SaveAnswerResponse.Result.FAIL_EXAM_CLOSED)
					.setFailText("考试未开始")
					.build());
		} else if (now >= examInfo.getExam().getEndTime()) {
			return Futures.immediateCheckedFuture(SaveAnswerResponse.newBuilder()
					.setResult(SaveAnswerResponse.Result.FAIL_EXAM_CLOSED)
					.setFailText("考试已结束")
					.build());
		}
		if (examInfo.getExam().hasAllowModelId() && !checkAllow(head, examInfo.getExam().getAllowModelId(), userId)) {
			return Futures.immediateCheckedFuture(SaveAnswerResponse.newBuilder()
					.setResult(SaveAnswerResponse.Result.FAIL_EXAM_NOT_JOIN)
					.setFailText("您不能参加本考试")
					.build());
		}
		
		Map<Integer, Integer> questionScoreMap = new LinkedHashMap<Integer, Integer>();
		for (ExamDAOProtos.ExamQuestion examQuestion : examInfo.getExamQuestionList()) {
			questionScoreMap.put(examQuestion.getQuestionId(), examQuestion.getScore());
		}
		
		Connection dbConn = null;
		try {
			dbConn = this.hikariDataSource.getConnection();
			
			ExamProtos.UserResult userResult = ExamDB.getUserResult(dbConn, companyId, userId, Collections.singleton(examId)).get(examId);
			if (userResult == null) {
				ExamDB.saveUserStartTime(dbConn, companyId, examId, userId, now);
			} else if (userResult.hasSubmitTime()) {
				return Futures.immediateCheckedFuture(SaveAnswerResponse.newBuilder()
						.setResult(SaveAnswerResponse.Result.FAIL_EXAM_CLOSED)
						.setFailText("考试已交卷不能再修改答案")
						.build());
			}
			
			if (examInfo.getExam().getType().equals(ExamProtos.Exam.Type.AUTO)) {
				questionScoreMap = ExamDB.getExamQuestionScoreRandom(dbConn, companyId, examId, userId, examInfo.getRandomQuestionCategoryIdList(), examInfo.getRandomQuestionNum());
			}
		} catch (SQLException ex){
			throw new RuntimeException(ex);
		} finally {
			DBUtil.closeQuietly(dbConn);
		}
		
		Map<Integer, ExamProtos.Question> questionMap = ExamUtil.getQuestion(hikariDataSource, jedisPool, companyId, questionScoreMap.keySet());
		for (Entry<Integer, Set<Integer>> entry : newExamUserAnswerMap.entrySet()) {
			final Integer questionId = entry.getKey();
			final Set<Integer> optionIdSet = entry.getValue();
			
			ExamProtos.Question question = questionMap.get(questionId);
			if (question == null) {
				return Futures.immediateCheckedFuture(SaveAnswerResponse.newBuilder()
						.setResult(SaveAnswerResponse.Result.FAIL_ANSWER_INVALID)
						.setFailText("您提交了一个本次考试不存在的考题")
						.build());
			}
			
			List<Integer> questionOptionId = new ArrayList<Integer>();
			for (Option option : question.getOptionList()) {
				questionOptionId.add(option.getOptionId());
			}
			if (!questionOptionId.containsAll(optionIdSet)) {
				return Futures.immediateCheckedFuture(SaveAnswerResponse.newBuilder()
						.setResult(SaveAnswerResponse.Result.FAIL_ANSWER_INVALID)
						.setFailText("您提交了一个本次考试不存在的考题选项")
						.build());
			}
			
			switch (question.getType()) {
				case OPTION_SINGLE:
					if (optionIdSet.size() > 1) {
						return Futures.immediateCheckedFuture(SaveAnswerResponse.newBuilder()
								.setResult(SaveAnswerResponse.Result.FAIL_ANSWER_INVALID)
								.setFailText("单选题不能勾选多项")
								.build());
					}
					break;
				case OPTION_MULTI:
					break;
				case OPTION_TF:
					if (optionIdSet.size() > 1) {
						return Futures.immediateCheckedFuture(SaveAnswerResponse.newBuilder()
								.setResult(SaveAnswerResponse.Result.FAIL_ANSWER_INVALID)
								.setFailText("判断题不能勾选多项")
								.build());
					}
				default:
					break;
			}
		}
		
		dbConn = null;
		try {
			dbConn = this.hikariDataSource.getConnection();
			
			Map<Integer, Set<Integer>> oldExamUserAnswerMap = ExamDB.getExamUserAnswer(dbConn, companyId, userId, examId);
			// 新增的答案只是按照题增量更新，所以要把老的题加上
			for (Entry<Integer, Set<Integer>> entry : oldExamUserAnswerMap.entrySet()) {
				if (!newExamUserAnswerMap.containsKey(entry.getKey())) {
					newExamUserAnswerMap.put(entry.getKey(), entry.getValue());
				}
			}
			
			ExamDB.updateExamUserAnswer(dbConn, companyId, userId, examId, oldExamUserAnswerMap, newExamUserAnswerMap, now);
		} catch (SQLException ex){
			throw new RuntimeException(ex);
		} finally {
			DBUtil.closeQuietly(dbConn);
		}
		
		return Futures.immediateFuture(SaveAnswerResponse.newBuilder()
				.setResult(SaveAnswerResponse.Result.SUCC)
				.build());
	}

	@Override
	public ListenableFuture<SubmitExamResponse> submitExam(RequestHead head, SubmitExamRequest request) {
		final long companyId = head.getSession().getCompanyId();
		final int examId = request.getExamId() ;
		final long userId = head.getSession().getUserId();
		
		Map<Integer, Set<Integer>> newExamUserAnswerMap = new HashMap<Integer, Set<Integer>>();
		for (ExamProtos.UserAnswer userAnswer : request.getUserAnswerList()) {
			Set<Integer> optionIdSet = newExamUserAnswerMap.get(userAnswer.getQuestionId());
			if (optionIdSet == null) {
				optionIdSet = new TreeSet<Integer>();
				newExamUserAnswerMap.put(userAnswer.getQuestionId(), optionIdSet);
			}
			optionIdSet.addAll(userAnswer.getAnswerOptionIdList());
		}
		
		ExamDAOProtos.ExamInfo examInfo = ExamUtil.getExamInfo(hikariDataSource, jedisPool, companyId, Collections.singleton(examId)).get(examId);
		if (examInfo == null) {
			return Futures.immediateCheckedFuture(SubmitExamResponse.newBuilder()
					.setResult(SubmitExamResponse.Result.FAIL_EXAM_NOT_EXSIT)
					.setFailText("考试不存在")
					.build());
		}
		
		final int now = (int) (System.currentTimeMillis() / 1000L);
		if (now < examInfo.getExam().getStartTime()) {
			return Futures.immediateCheckedFuture(SubmitExamResponse.newBuilder()
					.setResult(SubmitExamResponse.Result.FAIL_EXAM_CLOSED)
					.setFailText("考试未开始")
					.build());
		} else if (now >= examInfo.getExam().getEndTime()) {
			return Futures.immediateCheckedFuture(SubmitExamResponse.newBuilder()
					.setResult(SubmitExamResponse.Result.FAIL_EXAM_CLOSED)
					.setFailText("考试已结束")
					.build());
		}

		if (examInfo.getExam().hasAllowModelId() && !checkAllow(head, examInfo.getExam().getAllowModelId(), userId)) {
			return Futures.immediateCheckedFuture(SubmitExamResponse.newBuilder()
					.setResult(SubmitExamResponse.Result.FAIL_EXAM_NOT_JOIN)
					.setFailText("您不能参加本考试")
					.build());
		}
		
		Map<Integer, Integer> questionScoreMap = new LinkedHashMap<Integer, Integer>();
		for (ExamDAOProtos.ExamQuestion examQuestion : examInfo.getExamQuestionList()) {
			questionScoreMap.put(examQuestion.getQuestionId(), examQuestion.getScore());
		}
		
		Connection dbConn = null;
		try {
			dbConn = this.hikariDataSource.getConnection();
			
			ExamProtos.UserResult userResult = ExamDB.getUserResult(dbConn, companyId, userId, Collections.singleton(examId)).get(examId);
			if (userResult == null) {
				ExamDB.saveUserStartTime(dbConn, companyId, examId, userId, now);
			} else if (userResult.hasSubmitTime()) {
				return Futures.immediateCheckedFuture(SubmitExamResponse.newBuilder()
						.setResult(SubmitExamResponse.Result.FAIL_EXAM_CLOSED)
						.setFailText("考试已交卷不能再修改答案")
						.build());
			}
			
			if (examInfo.getExam().getType().equals(ExamProtos.Exam.Type.AUTO)) {
				questionScoreMap = ExamDB.getExamQuestionScoreRandom(dbConn, companyId, examId, userId, examInfo.getRandomQuestionCategoryIdList(), examInfo.getRandomQuestionNum());
			}
		} catch (SQLException ex){
			throw new RuntimeException(ex);
		} finally {
			DBUtil.closeQuietly(dbConn);
		}

		Map<Integer, ExamProtos.Question> questionMap = ExamUtil.getQuestion(hikariDataSource, jedisPool, companyId, questionScoreMap.keySet());
			
		int totalScore = 0;
		for (Entry<Integer, Set<Integer>> entry : newExamUserAnswerMap.entrySet()) {
			final Integer questionId = entry.getKey();
			final Set<Integer> optionIdSet = entry.getValue();
			
			ExamProtos.Question question = questionMap.get(questionId);
			if (question == null) {
				return Futures.immediateCheckedFuture(SubmitExamResponse.newBuilder()
						.setResult(SubmitExamResponse.Result.FAIL_ANSWER_INVALID)
						.setFailText("您提交了一个本次考试不存在的考题:" + questionId)
						.build());
			}
			
			List<Integer> questionOptionId = new ArrayList<Integer>();
			for (Option option : question.getOptionList()) {
				questionOptionId.add(option.getOptionId());
			}
			if (!questionOptionId.containsAll(optionIdSet)) {
				return Futures.immediateCheckedFuture(SubmitExamResponse.newBuilder()
						.setResult(SubmitExamResponse.Result.FAIL_ANSWER_INVALID)
						.setFailText("您提交了一个本次考试不存在的考题选项:" + questionId)
						.build());
			}
			
			switch (question.getType()) {
				case OPTION_SINGLE:
					if (optionIdSet.size() > 1) {
						return Futures.immediateCheckedFuture(SubmitExamResponse.newBuilder()
								.setResult(SubmitExamResponse.Result.FAIL_ANSWER_INVALID)
								.setFailText("单选题不能勾选多项:" + questionId)
								.build());
					}
					break;
				case OPTION_MULTI:
					break;
				case OPTION_TF:
					if (optionIdSet.size() > 1) {
						return Futures.immediateCheckedFuture(SubmitExamResponse.newBuilder()
								.setResult(SubmitExamResponse.Result.FAIL_ANSWER_INVALID)
								.setFailText("判断题不能勾选多项:" + questionId)
								.build());
					}
					break;
				default:
					break;
			}
			
			boolean isRight = true;
			for (ExamProtos.Option option : question.getOptionList()) {
				if ((option.getIsRight() && !optionIdSet.contains(option.getOptionId()))
						|| (!option.getIsRight() && optionIdSet.contains(option.getOptionId()))) {
					isRight = false;
					break;
				}
			}
			
			if (isRight) {
				Integer score = questionScoreMap.get(questionId);
				if (score != null) {
					totalScore += score;
				}
			}
		}
		
		dbConn = null;
		try {
			dbConn = this.hikariDataSource.getConnection();
			Map<Integer, Set<Integer>> oldExamUserAnswerMap = ExamDB.getExamUserAnswer(dbConn, companyId, userId, examId);
			ExamDB.updateExamUserAnswer(dbConn, companyId, userId, examId, oldExamUserAnswerMap, newExamUserAnswerMap, now);
			ExamDB.saveUserScore(dbConn, companyId, userId, examId, totalScore, now, now);
		} catch (SQLException ex){
			throw new RuntimeException(ex);
		} finally {
			DBUtil.closeQuietly(dbConn);
		}
		
		return Futures.immediateFuture(SubmitExamResponse.newBuilder()
				.setResult(SubmitExamResponse.Result.SUCC)
				.setUserScore(totalScore)
				.build());
	}
	
	private boolean checkAllow(RequestHead webHead, int modelId, long userId) {
		CheckAllowRequest checkAllowRequest = CheckAllowRequest.newBuilder()
				.addModelId(modelId)
				.addUserId(userId)
				.build();
				
		CheckAllowResponse checkAllowResponse = Futures.getUnchecked(allowService.checkAllow(webHead, checkAllowRequest));
		List<CheckAllowResponse.CheckResult> checkAllowList = checkAllowResponse.getCheckResultList();
		for (CheckAllowResponse.CheckResult checkResult : checkAllowList) {
			if (checkResult.getModelId() == modelId && checkResult.getAllowUserIdList().contains(userId)) {
				return true;
			}
		}
		return false;
	}
	
	/**
	 * @param head
	 * @param modelId
	 * @param userId
	 * @return modelId -> allow userId set
	 */
	private Map<Integer, ExamDAOProtos.ExamInfo> checkExamAllow(RequestHead head, Map<Integer, ExamDAOProtos.ExamInfo> examInfoMap, long userId) {
		if (examInfoMap.isEmpty()) {
			return Collections.emptyMap();
		}
		
		Set<Integer> modelIdSet = new TreeSet<Integer>();
		for (ExamDAOProtos.ExamInfo examInfo : examInfoMap.values()) {
			if (examInfo.getExam().hasAllowModelId()) {
				modelIdSet.add(examInfo.getExam().getAllowModelId());
			}
		}
		
		// 没有人员访问规则校验model，直接全量返回
		if (modelIdSet.isEmpty()) {
			return examInfoMap;
		}
		
		CheckAllowRequest checkAllowRequest = CheckAllowRequest.newBuilder()
				.addAllModelId(modelIdSet)
				.addUserId(userId)
				.build();
		
		CheckAllowResponse checkAllowResponse = Futures.getUnchecked(allowService.checkAllow(head, checkAllowRequest));
		
		Map<Integer, ExamDAOProtos.ExamInfo> resultMap = new TreeMap<Integer, ExamDAOProtos.ExamInfo>();
		for (Entry<Integer, ExamDAOProtos.ExamInfo> entry : examInfoMap.entrySet()) {
			if (!entry.getValue().getExam().hasAllowModelId()) {
				resultMap.put(entry.getKey(), entry.getValue());
			} else {
				boolean isAllow = false;
				for (CheckAllowResponse.CheckResult checkResult : checkAllowResponse.getCheckResultList()) {
					if (checkResult.getModelId() == entry.getValue().getExam().getAllowModelId()
							&& checkResult.getAllowUserIdList().contains(userId)
							) {
						isAllow = true;
						break;
					}
				}
				if (isAllow) {
					resultMap.put(entry.getKey(), entry.getValue());
				}
			}
		}
		return resultMap;
	}

}
