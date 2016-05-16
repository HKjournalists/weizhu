package com.weizhu.service.survey;

import java.sql.Connection;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.Executor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.common.primitives.Ints;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import com.weizhu.common.db.DBUtil;
import com.weizhu.common.utils.DataPage;
import com.weizhu.common.utils.ProfileManager;
import com.weizhu.proto.AdminOfficialProtos;
import com.weizhu.proto.AdminOfficialService;
import com.weizhu.proto.AdminProtos.AdminHead;
import com.weizhu.proto.AdminUserProtos.GetUserListRequest;
import com.weizhu.proto.AdminUserProtos.GetUserListResponse;
import com.weizhu.proto.AdminUserService;
import com.weizhu.proto.AllowProtos;
import com.weizhu.proto.AllowProtos.CheckAllowRequest;
import com.weizhu.proto.AllowProtos.CheckAllowResponse;
import com.weizhu.proto.AllowService;
import com.weizhu.proto.OfficialProtos;
import com.weizhu.proto.SurveyProtos;
import com.weizhu.proto.SurveyProtos.CopySurveyRequest;
import com.weizhu.proto.SurveyProtos.CopySurveyResponse;
import com.weizhu.proto.SurveyProtos.CreateQuestionRequest;
import com.weizhu.proto.SurveyProtos.CreateQuestionResponse;
import com.weizhu.proto.SurveyProtos.CreateSurveyRequest;
import com.weizhu.proto.SurveyProtos.CreateSurveyResponse;
import com.weizhu.proto.SurveyProtos.DeleteQuestionRequest;
import com.weizhu.proto.SurveyProtos.DeleteQuestionResponse;
import com.weizhu.proto.SurveyProtos.DeleteSurveyRequest;
import com.weizhu.proto.SurveyProtos.DeleteSurveyResponse;
import com.weizhu.proto.SurveyProtos.DisableSurveyRequest;
import com.weizhu.proto.SurveyProtos.DisableSurveyResponse;
import com.weizhu.proto.SurveyProtos.EnableSurveyRequest;
import com.weizhu.proto.SurveyProtos.EnableSurveyResponse;
import com.weizhu.proto.SurveyProtos.GetClosedSurveyRequest;
import com.weizhu.proto.SurveyProtos.GetClosedSurveyResponse;
import com.weizhu.proto.SurveyProtos.GetOpenSurveyCountResponse;
import com.weizhu.proto.SurveyProtos.GetOpenSurveyRequest;
import com.weizhu.proto.SurveyProtos.GetOpenSurveyResponse;
import com.weizhu.proto.SurveyProtos.GetQuestionAnswerRequest;
import com.weizhu.proto.SurveyProtos.GetQuestionAnswerResponse;
import com.weizhu.proto.SurveyProtos.GetSurveyByIdRequest;
import com.weizhu.proto.SurveyProtos.GetSurveyByIdResponse;
import com.weizhu.proto.SurveyProtos.GetSurveyListRequest;
import com.weizhu.proto.SurveyProtos.GetSurveyListResponse;
import com.weizhu.proto.SurveyProtos.GetSurveyResultListRequest;
import com.weizhu.proto.SurveyProtos.GetSurveyResultListResponse;
import com.weizhu.proto.SurveyProtos.GetSurveyResultRequest;
import com.weizhu.proto.SurveyProtos.GetSurveyResultResponse;
import com.weizhu.proto.SurveyProtos.ImportQuestionRequest;
import com.weizhu.proto.SurveyProtos.ImportQuestionResponse;
import com.weizhu.proto.SurveyProtos.QuestionSortRequest;
import com.weizhu.proto.SurveyProtos.QuestionSortResponse;
import com.weizhu.proto.SurveyProtos.SubmitSurveyRequest;
import com.weizhu.proto.SurveyProtos.SubmitSurveyResponse;
import com.weizhu.proto.SurveyProtos.UpdateQuestionRequest;
import com.weizhu.proto.SurveyProtos.UpdateQuestionResponse;
import com.weizhu.proto.SurveyProtos.UpdateSurveyRequest;
import com.weizhu.proto.SurveyProtos.UpdateSurveyResponse;
import com.weizhu.proto.SurveyService;
import com.weizhu.proto.UserProtos;
import com.weizhu.proto.WeizhuProtos.EmptyRequest;
import com.weizhu.proto.WeizhuProtos.RequestHead;
import com.zaxxer.hikari.HikariDataSource;

public class SurveyServiceImpl implements SurveyService {
	
	private static final Logger logger = LoggerFactory.getLogger(SurveyServiceImpl.class);
	
	private final HikariDataSource hikariDataSource;
	private final JedisPool jedisPool;
	private final Executor serviceExecutor;
	private final AllowService allowService;
	private final AdminUserService adminUserService;
	private final AdminOfficialService adminOfficialService;
	private final ProfileManager profileManager;
	
	private static final ProfileManager.ProfileKey<String> SURVEY_TEMPLATE = 
			ProfileManager.createKey("survey:template", "亲爱的伙伴儿，调研中心新上传调研《${name}》，开始时间是：${start_time},结束时间：${end_time},请安排时间尽快完成！");

	@Inject
	public SurveyServiceImpl(HikariDataSource hikariDataSource, JedisPool jedisPool, 
			@Named("service_executor") Executor serviceExecutor, AllowService allowService,
			AdminUserService adminUserService, AdminOfficialService adminOfficialService, ProfileManager profileManager) {
		this.hikariDataSource = hikariDataSource;
		this.jedisPool = jedisPool;
		this.serviceExecutor = serviceExecutor;
		this.allowService = allowService;
		this.adminUserService = adminUserService;
		this.adminOfficialService = adminOfficialService;
		this.profileManager = profileManager;
	}
	
	@Override
	public ListenableFuture<GetOpenSurveyResponse> getOpenSurvey(
			RequestHead head, GetOpenSurveyRequest request) {
		final ByteString data = request.hasOffsetIndex() && !request.getOffsetIndex().isEmpty() ? request.getOffsetIndex() : null;
		Integer lastSurveyId = null;
		Integer lastEndTime = null;
		SurveyDAOProtos.SurveyListIndex offsetIndex = null;
		if (data != null) {
			try {
				offsetIndex = SurveyDAOProtos.SurveyListIndex.parseFrom(data);
				lastSurveyId = offsetIndex.getSurveyId();
				lastEndTime = offsetIndex.getTime();
			} catch (InvalidProtocolBufferException e) {
				
			}
		}

		final long companyId = head.getSession().getCompanyId();
		final int now = (int) (System.currentTimeMillis() / 1000L);
		final long userId = head.getSession().getUserId();
		
		Connection conn = null;
		List<Integer> surveyIdTmpList = null;
		Map<Integer, SurveyProtos.SurveyResult> surveyResultMap = null;
		try {
			conn = this.hikariDataSource.getConnection();
			
			surveyIdTmpList = SurveyDB.getOpenSurveyId(conn, companyId, now, lastSurveyId, lastEndTime);
			surveyResultMap = SurveyDB.getUserSurveyResult(conn, companyId, userId, surveyIdTmpList);
		} catch (SQLException ex) {
			throw new RuntimeException("db fail", ex);
		} finally {
			DBUtil.closeQuietly(conn);
		}
		
		// 去掉已经有结果的调研
		Set<Integer> surveyIdSet = surveyResultMap.keySet();
		List<Integer> surveyIdList = new ArrayList<Integer>();
		for (int surveyId : surveyIdTmpList) {
			if (!surveyIdSet.contains(surveyId)) {
				surveyIdList.add(surveyId);
			}
		}
		
		// 取出所有的allowId
		Map<Integer, SurveyProtos.Survey> surveyMap = SurveyUtil.getSurveyById(hikariDataSource, jedisPool, companyId, surveyIdList);
		Set<Integer> allowModelIdSet = new HashSet<Integer>();
		for (SurveyProtos.Survey survey : surveyMap.values()) {
			if (survey.hasAllowModelId()) {
				allowModelIdSet.add(survey.getAllowModelId());
			}
		}
		
		// 筛选出能访问到的调研
		Set<Integer> allowedModelIdSet = doCheckAllowModelId(head, allowModelIdSet);
		Map<Integer, SurveyProtos.Survey> openSurveyMap = new HashMap<Integer, SurveyProtos.Survey>();
		for (Entry<Integer, SurveyProtos.Survey> entry : surveyMap.entrySet()) {
			if (!entry.getValue().hasAllowModelId() || allowedModelIdSet.contains(entry.getValue().getAllowModelId())) {
				openSurveyMap.put(entry.getKey(), entry.getValue());
			}
		}
		
		final int size = request.getSize() < 0 ? 0 : request.getSize() > 50 ? 50 : request.getSize();
		
		// 添加此调研参统计数量
		Map<Integer, SurveyDAOProtos.SurveyCount> surveyCountMap = SurveyUtil.getSurveyCount(hikariDataSource, jedisPool, companyId, surveyIdList);
		
		List<SurveyProtos.Survey> surveyList = new ArrayList<SurveyProtos.Survey>();
		for (int surveyId : surveyIdList) {
			if (surveyList.size() >= size) {
				break;
			}
			
			SurveyProtos.Survey survey = openSurveyMap.get(surveyId);
			if (survey != null) {
				SurveyDAOProtos.SurveyCount surveyCount = surveyCountMap.get(surveyId);
				if (surveyCount != null) {
					SurveyProtos.Survey tmpSurvey = SurveyProtos.Survey.newBuilder()
							.mergeFrom(survey)
							.setSurveyUserCnt(surveyCount.getSurveyCount())
							.build();
					surveyList.add(tmpSurvey);
				} else {
					surveyList.add(survey);
				}
				
			}
		}
		
		SurveyDAOProtos.SurveyListIndex offsetIndexResult = null;
		
		if (surveyList.isEmpty()) {
			offsetIndexResult = SurveyDAOProtos.SurveyListIndex.getDefaultInstance();
		} else {
			SurveyProtos.Survey survey = surveyList.get(surveyList.size() - 1);
			offsetIndexResult = SurveyDAOProtos.SurveyListIndex.newBuilder()
					.setSurveyId(survey.getSurveyId())
					.setTime(survey.getEndTime())
					.build();
		}
		
		return Futures.immediateFuture(GetOpenSurveyResponse.newBuilder()
				.setOffsetIndex(offsetIndexResult.toByteString())
				.setHasMore(openSurveyMap.size() > size)
				.addAllSurvey(surveyList)
				.build());
	}
	
	@Override
	public ListenableFuture<GetOpenSurveyCountResponse> getOpenSurveyCount(RequestHead head, EmptyRequest request) {
		final long companyId = head.getSession().getCompanyId();
		final int now = (int) (System.currentTimeMillis() / 1000L);
		final long userId = head.getSession().getUserId();
		
		List<Integer> surveyIdList;
		Map<Integer, SurveyProtos.SurveyResult> surveyResultMap = null;
		Connection conn = null;
		try {
			conn = this.hikariDataSource.getConnection();
			
			surveyIdList = SurveyDB.getOpenSurveyId(conn, companyId, now, null, null);
			surveyResultMap = SurveyDB.getUserSurveyResult(conn, companyId, userId, surveyIdList);
		} catch (SQLException ex) {
			throw new RuntimeException("db fail", ex);
		} finally {
			DBUtil.closeQuietly(conn);
		}
		
		// 去掉已经有结果的调研
		Set<Integer> openSurveyIdSet = new TreeSet<Integer>();
		for (Integer surveyId : surveyIdList) {
			if (!surveyResultMap.containsKey(surveyId)) {
				openSurveyIdSet.add(surveyId);
			}
		}
		
		// 取出所有的allowId
		Map<Integer, SurveyProtos.Survey> openSurveyMap = SurveyUtil.getSurveyById(hikariDataSource, jedisPool, companyId, openSurveyIdSet);
		Set<Integer> allowModelIdSet = new TreeSet<Integer>();
		for (SurveyProtos.Survey survey : openSurveyMap.values()) {
			if (survey.hasAllowModelId()) {
				allowModelIdSet.add(survey.getAllowModelId());
			}
		}
		
		// 筛选出能访问到的调研
		Set<Integer> allowedModelIdSet = this.doCheckAllowModelId(head, allowModelIdSet);
		
		int openSurveyCount = 0;
		for (SurveyProtos.Survey survey : openSurveyMap.values()) {
			if (!survey.hasAllowModelId() || allowedModelIdSet.contains(survey.getAllowModelId())) {
				openSurveyCount ++;
			}
		}
		
		return Futures.immediateFuture(GetOpenSurveyCountResponse.newBuilder()
				.setOpenSurveyCount(openSurveyCount)
				.build());
	}
	
	@Override
	public ListenableFuture<GetClosedSurveyResponse> getClosedSurvey(
			RequestHead head, GetClosedSurveyRequest request) {
		final ByteString data = request.hasOffsetIndex() && !request.getOffsetIndex().isEmpty() ? request.getOffsetIndex() : null;
		Integer lastSurveyId = null;
		Integer lastEndTime = null;
		SurveyDAOProtos.SurveyListIndex offsetIndex = null;
		if (data != null) {
			try {
				offsetIndex = SurveyDAOProtos.SurveyListIndex.parseFrom(data);
				lastSurveyId = offsetIndex.getSurveyId();
				lastEndTime = offsetIndex.getTime();
			} catch (InvalidProtocolBufferException e) {
				
			}
		}
		
		final long companyId = head.getSession().getCompanyId();
		final int now = (int) (System.currentTimeMillis() / 1000L);
		final long userId = head.getSession().getUserId();
		
		Connection conn = null;
		Set<Integer> tmpSurveyIdSet = new HashSet<Integer>();
		Map<Integer, SurveyProtos.SurveyResult> surveyResultMap = new HashMap<Integer, SurveyProtos.SurveyResult>();
		try {
			conn = this.hikariDataSource.getConnection();
			
			tmpSurveyIdSet.addAll(SurveyDB.getClosedSurveyId(conn, companyId, now, lastSurveyId, lastEndTime));
			tmpSurveyIdSet.addAll(SurveyDB.getSubmitSurveyId(conn, companyId, userId,lastSurveyId, lastEndTime));
			surveyResultMap = SurveyDB.getUserSurveyResult(conn, companyId, userId, tmpSurveyIdSet);
		} catch (SQLException ex) {
			throw new RuntimeException("db fail", ex);
		} finally {
			DBUtil.closeQuietly(conn);
		}
		
		Map<Integer, SurveyProtos.Survey> surveyMap = SurveyUtil.getSurveyById(hikariDataSource, jedisPool, companyId, tmpSurveyIdSet);
		
		// 去除所有的allowModelId
		Set<Integer> allowModelIdSet = new HashSet<Integer>();
		for (SurveyProtos.Survey survey : surveyMap.values()) {
			if (survey.hasAllowModelId()) {
				allowModelIdSet.add(survey.getAllowModelId());
			}
		}
		
		// 筛选出能访问的调研信息
		Set<Integer> allowedModelIdSet = doCheckAllowModelId(head, allowModelIdSet);
		Map<Integer, SurveyProtos.Survey> tmpClosedSurveyMap = new HashMap<Integer, SurveyProtos.Survey>();
		for (Entry<Integer, SurveyProtos.Survey> entry : surveyMap.entrySet()) {
			if (!entry.getValue().hasAllowModelId() || allowedModelIdSet.contains(entry.getValue().getAllowModelId())) {
				tmpClosedSurveyMap.put(entry.getKey(), entry.getValue());
			}
		}
		
		// 添加此调研统计数量
		Map<Integer, SurveyDAOProtos.SurveyCount> surveyCountMap = SurveyUtil.getSurveyCount(hikariDataSource, jedisPool, companyId, tmpClosedSurveyMap.keySet());
		
		List<SurveyProtos.Survey> surveyList = new ArrayList<SurveyProtos.Survey>();
		for (Entry<Integer, SurveyProtos.Survey> entry : tmpClosedSurveyMap.entrySet()) {
			SurveyProtos.Survey.Builder surveyBuilder = SurveyProtos.Survey.newBuilder()
					.mergeFrom(entry.getValue());
			SurveyProtos.SurveyResult surveyResult = surveyResultMap.get(entry.getKey());
			if (surveyResult != null) {
				surveyBuilder.setSubmitTime(surveyResult.getSubmitTime());
			}
			
			SurveyDAOProtos.SurveyCount surveyCount = surveyCountMap.get(entry.getKey());
			if (surveyCount != null) {
				surveyBuilder.setSurveyUserCnt(surveyCount.getSurveyCount());
			}
			
			surveyList.add(surveyBuilder.build());
		}
		
		Collections.sort(surveyList, new Comparator<SurveyProtos.Survey>() {
			@Override
			public int compare(SurveyProtos.Survey o1, SurveyProtos.Survey o2) {
				int submitTime1 = o1.hasSubmitTime() ? o1.getSubmitTime() : o1.getEndTime();
				int submitTime2 = o2.hasSubmitTime() ? o2.getSubmitTime() : o2.getEndTime();
				
				return -Ints.compare(submitTime1, submitTime2);
			}
		});
		
		SurveyDAOProtos.SurveyListIndex offsetIndexResult = null;
		
		if (surveyList.isEmpty()) {
			offsetIndexResult = SurveyDAOProtos.SurveyListIndex.getDefaultInstance();
		} else {
			SurveyProtos.Survey survey = surveyList.get(surveyList.size() - 1);
			offsetIndexResult = SurveyDAOProtos.SurveyListIndex.newBuilder()
					.setSurveyId(survey.getSurveyId())
					.setTime(survey.getEndTime())
					.build();
		}
		
		final int size = request.getSize() < 0 ? 0 : request.getSize() > 50 ? 50 : request.getSize();
		
		return Futures.immediateFuture(GetClosedSurveyResponse.newBuilder()
				.setOffsetIndex(offsetIndexResult.toByteString())
				.setHasMore(surveyList.size() > size)
				.addAllSurvey(surveyList.size() > size ? surveyList.subList(0, size) : surveyList)
				.build());
	}
	
	@Override
	public ListenableFuture<GetSurveyByIdResponse> getSurveyById(
			RequestHead head, GetSurveyByIdRequest request) {
		final long companyId = head.getSession().getCompanyId();
		final int surveyId = request.getSurveyId();
		
		SurveyProtos.Survey survey = SurveyUtil.getSurveyById(hikariDataSource, jedisPool, companyId, Collections.singleton(surveyId)).get(surveyId);
		if (survey == null) {
			return Futures.immediateFuture(GetSurveyByIdResponse.newBuilder()
					.setResult(GetSurveyByIdResponse.Result.FAIL_SURVEY_NOT_EXSIT)
					.setFailText("此调研不存在")
					.build());
		}
		
		// 判断用户是否能访问此调研
		if (survey.hasAllowModelId()) {
			int allowModelId = survey.getAllowModelId();
			
			Set<Integer> allowedModelIdSet = doCheckAllowModelId(head, Collections.singleton(allowModelId));
			
			if (!allowedModelIdSet.contains(allowModelId)) {
				return Futures.immediateFuture(GetSurveyByIdResponse.newBuilder()
						.setResult(GetSurveyByIdResponse.Result.FAIL_SURVEY_NOT_JOIN)
						.setFailText("您不需要参加这次调研！")
						.build());
			}
		}
		
		final long userId = head.getSession().getUserId();
		
		Connection conn = null;
		List<Integer> questionIdList = null;
		SurveyProtos.SurveyResult surveyResult = null;
		try {
			conn = this.hikariDataSource.getConnection();
			
			questionIdList = SurveyDB.getQuestionIdBySurveyId(conn, companyId, surveyId);
			surveyResult = SurveyDB.getUserSurveyResult(conn, companyId, userId, surveyId);
		} catch (SQLException ex) {
			throw new RuntimeException("db fail", ex);
		} finally {
			DBUtil.closeQuietly(conn);
		}
		
		SurveyProtos.Survey.Builder surveyBuilder = SurveyProtos.Survey.newBuilder()
				.mergeFrom(survey);
		if (surveyResult != null) {
			surveyBuilder.setSubmitTime(surveyResult.getSubmitTime());
		}
		
		SurveyProtos.ShowResultType resultType = survey.getShowResultType();
		
		// 添加调研参与人数
		SurveyDAOProtos.SurveyCount surveyCount = SurveyUtil.getSurveyCount(hikariDataSource, jedisPool, companyId, Collections.singleton(surveyId)).get(surveyId);
		if (surveyCount != null) {
			surveyBuilder.setSurveyUserCnt(surveyCount.getSurveyCount());
		}
		
		Map<Integer, SurveyProtos.Question> questionMap = SurveyUtil.getSurveyQuestionById(hikariDataSource, jedisPool, companyId, questionIdList);
		
		// 添加投票题的参与人数(目前只有投票有统计)
		List<Integer> voteIdList = new ArrayList<Integer>();
		for (SurveyProtos.Question question : questionMap.values()) {
			if (question.getTypeCase().name().equals("VOTE")) {
				voteIdList.add(question.getQuestionId());
			}
		}
		Map<Integer, SurveyDAOProtos.QuestionCount> questionCountMap = SurveyUtil.getQuestionCount(hikariDataSource, jedisPool, companyId, voteIdList);
		
		List<SurveyProtos.Question> questionList = new ArrayList<SurveyProtos.Question>();
		for (Entry<Integer, SurveyProtos.Question> entry : questionMap.entrySet()) {
			SurveyDAOProtos.QuestionCount questionCount = questionCountMap.get(entry.getKey());
			if (questionCount != null) {
				// 添加option count
				List<SurveyDAOProtos.QuestionCount.OptionCount> optionCountList = questionCount.getOptionCountList();
				Map<Integer, Integer> optionCntMap = new HashMap<Integer, Integer>();
				if (!(surveyResult == null && resultType.equals(SurveyProtos.ShowResultType.AFTER_SUBMIT_COUNT)) || !resultType.equals(SurveyProtos.ShowResultType.NONE)) {
					for (SurveyDAOProtos.QuestionCount.OptionCount optionCount : optionCountList) {
						optionCntMap.put(optionCount.getOptionId(), optionCount.getOptionCount());
					}
				}
				
				List<SurveyProtos.Vote.Option> optionList = entry.getValue().getVote().getOptionList();
				
				List<SurveyProtos.Vote.Option> tmpOptionList = new ArrayList<SurveyProtos.Vote.Option>();
				for (SurveyProtos.Vote.Option voteOption : optionList) {
					Integer optionCount = optionCntMap.get(voteOption.getOptionId()); 
					if (optionCount != null) {
						tmpOptionList.add(SurveyProtos.Vote.Option.newBuilder()
								.mergeFrom(voteOption)
								.setOptionCnt(optionCount)
								.build());
					} else {
						tmpOptionList.add(voteOption);
					}
					
				}
				
				// 添加question count
				SurveyProtos.Vote.Question voteQuestion = SurveyProtos.Vote.Question.newBuilder()
						.setCheckNum(entry.getValue().getVote().getCheckNum())
						.setTotalCnt(questionCount.getQuestionCount())
						.addAllOption(tmpOptionList)
						.build();
				
				questionList.add(SurveyProtos.Question.newBuilder()
						.mergeFrom(entry.getValue())
						.setVote(voteQuestion)
						.build());
			} else {
				questionList.add(entry.getValue());
			}
			
		}
		
		// 返回有序的题目
		List<SurveyProtos.Question> questionOrderList = new ArrayList<SurveyProtos.Question>();
		for (int questionId : questionIdList) {
			for (SurveyProtos.Question question : questionList) {
				if (question.getQuestionId() == questionId) {
					questionOrderList.add(question);
				}
			}
		}
		
		GetSurveyByIdResponse.Builder responseBuilder = GetSurveyByIdResponse.newBuilder()
				.setResult(GetSurveyByIdResponse.Result.SUCC)
				.setSurvey(surveyBuilder.build())
				.addAllQuestion(questionOrderList);
		
		if (surveyResult != null) {
			responseBuilder.setSurveyResult(surveyResult);
		}
		
		return Futures.immediateFuture(responseBuilder.build());
	}

	@Override
	public ListenableFuture<SubmitSurveyResponse> submitSurvey(
			RequestHead head, SubmitSurveyRequest request) {
		final long companyId = head.getSession().getCompanyId();
		final int surveyId = request.getSurveyId();
		
		SurveyProtos.Survey survey = SurveyUtil.getSurveyById(hikariDataSource, jedisPool, companyId, Collections.singleton(surveyId)).get(surveyId);
		if (survey == null) {
			return Futures.immediateFuture(SubmitSurveyResponse.newBuilder()
					.setResult(SubmitSurveyResponse.Result.FAIL_SURVEY_NOT_EXSIT)
					.setFailText("调研不存在！")
					.build());
		}
		
		final int now = (int) (System.currentTimeMillis() / 1000L);
		if (survey.getEndTime() < now) {
			return Futures.immediateFuture(SubmitSurveyResponse.newBuilder()
					.setResult(SubmitSurveyResponse.Result.FAIL_SURVEY_CLOSE)
					.setFailText("调研已经结束！")
					.build());
		}
		if (survey.getStartTime() > now) {
			return Futures.immediateFuture(SubmitSurveyResponse.newBuilder()
					.setResult(SubmitSurveyResponse.Result.FAIL_SURVEY_CLOSE)
					.setFailText("调研还没开始！")
					.build());
		}
		
		
		if (survey.hasAllowModelId()) {
			int allowModelId = survey.getAllowModelId();
			Set<Integer> allowedModelIdSet = doCheckAllowModelId(head, Collections.singleton(allowModelId));
			if (!allowedModelIdSet.contains(allowModelId)) {
				return Futures.immediateFuture(SubmitSurveyResponse.newBuilder()
						.setResult(SubmitSurveyResponse.Result.FAIL_SURVEY_NOT_EXSIT)
						.setFailText("您不用参加此调研！")
						.build());
			}
		}
		
		Connection conn = null;
		List<Integer> questionIdList = new ArrayList<Integer>();
		try {
			conn = this.hikariDataSource.getConnection();
			
			questionIdList = SurveyDB.getQuestionIdBySurveyId(conn, companyId, surveyId);
		} catch (SQLException ex) {
			throw new RuntimeException("db fail", ex);
		} finally {
			DBUtil.closeQuietly(conn);
		}
		
		// 根据surveyId获取对应的所有question
		Map<Integer, SurveyProtos.Question> questionMap = SurveyUtil.getSurveyQuestionById(hikariDataSource, jedisPool, companyId, questionIdList);
		
		final List<SurveyProtos.Answer> answerList = request.getAnswerList();
		
		Integer submitTime = null;
		// 查看必做题是否答完
		List<Integer> answerQuestionIdList = new ArrayList<Integer>();
		for (SurveyProtos.Answer answer : answerList) {
			answerQuestionIdList.add(answer.getQuestionId());
			if (submitTime == null) {
				submitTime = answer.getAnswerTime();
			}
		}
		for (SurveyProtos.Question question : questionMap.values()) {
			// 如果是必做并且用户回答的题中不存在
			if (question.getIsOptional() && !answerQuestionIdList.contains(question.getQuestionId())) {
				return Futures.immediateFuture(SubmitSurveyResponse.newBuilder()
						.setResult(SubmitSurveyResponse.Result.FAIL_ANSWER_INVALID)
						.setFailText("必答题目没有回答。题目名称：" + question.getQuestionName())
						.build());
			}
		}
		
		List<SurveyProtos.Answer> voteAnswerList = new ArrayList<SurveyProtos.Answer>();
		List<SurveyProtos.Answer> inputTextAnswerList = new ArrayList<SurveyProtos.Answer>();
		List<SurveyProtos.Answer> inputSelectAnswerList = new ArrayList<SurveyProtos.Answer>();
		for (SurveyProtos.Answer answer : answerList) {
			SurveyProtos.Question question = questionMap.get(answer.getQuestionId());
			if (question == null) {
				return Futures.immediateFuture(SubmitSurveyResponse.newBuilder()
						.setResult(SubmitSurveyResponse.Result.FAIL_ANSWER_INVALID)
						.setFailText("题目不存在。题目编号：" + answer.getQuestionId())
						.build());
			}
			
			switch (answer.getTypeCase()) {
				case VOTE :
					if (answer.getVote().getOptionIdCount() > question.getVote().getCheckNum()) {
						return Futures.immediateFuture(SubmitSurveyResponse.newBuilder()
								.setResult(SubmitSurveyResponse.Result.FAIL_ANSWER_INVALID)
								.setFailText("选择个数超出可选个数。题目名称：" + question.getQuestionName())
								.build());
					}
					for (int optionId : answer.getVote().getOptionIdList()) {
						List<Integer> optionIdList = new ArrayList<Integer>();
						for (SurveyProtos.Vote.Option option : question.getVote().getOptionList()) {
							optionIdList.add(option.getOptionId());
						}
						
						if (!optionIdList.contains(optionId)) {
							return Futures.immediateFuture(SubmitSurveyResponse.newBuilder()
									.setResult(SubmitSurveyResponse.Result.FAIL_ANSWER_INVALID)
									.setFailText("选项不在所在题目中。题目名称：" + question.getQuestionName() + "，选项编号：" + optionId)
									.build());
						}
					}
					
					voteAnswerList.add(answer);
					break;
				case INPUT_TEXT :
					if (answer.getInputText().getResultText().length() > 191) {
						return Futures.immediateFuture(SubmitSurveyResponse.newBuilder()
								.setResult(SubmitSurveyResponse.Result.FAIL_ANSWER_INVALID)
								.setFailText("回答内容过长，请控制在190个字符内。题目名称：" + question.getQuestionName())
								.build());
					}
					
					inputTextAnswerList.add(answer);
					break;
				case INPUT_SELECT : 
					List<Integer> optionIdList = new ArrayList<Integer>();
					for (SurveyProtos.InputSelect.Option option : question.getInputSelect().getOptionList()) {
						optionIdList.add(option.getOptionId());
					}
					
					if (!optionIdList.contains(answer.getInputSelect().getOptionId())) {
						return Futures.immediateFuture(SubmitSurveyResponse.newBuilder()
								.setResult(SubmitSurveyResponse.Result.FAIL_ANSWER_INVALID)
								.setFailText("选项不在所在题目中。选项编号：" + answer.getInputSelect().getOptionId() + "题目名称：" + question.getQuestionName())
								.build());
					}
					
					inputSelectAnswerList.add(answer);
					break;
				case TYPE_NOT_SET : 
					break;
				default :
					break;
			}
		}
		
		final long userId = head.getSession().getUserId();
		
		try {
			conn = this.hikariDataSource.getConnection();
			
			// 判断该用户是否重复提交
			if (SurveyDB.getUserSurveyResult(conn, companyId, userId, Collections.singleton(surveyId)).get(surveyId) != null) {
				return Futures.immediateFuture(SubmitSurveyResponse.newBuilder()
						.setResult(SubmitSurveyResponse.Result.FAIL_ANSWER_INVALID)
						.setFailText("已经回答过此调研，请不要重复回答！")
						.build());
			}
			SurveyDB.insertAnswer(conn, companyId, surveyId, userId, submitTime == null ? (int) (System.currentTimeMillis() / 1000L) : submitTime, voteAnswerList, inputTextAnswerList, inputSelectAnswerList);
		} catch (SQLException ex) {
			throw new RuntimeException("db fail", ex);
		} finally {
			DBUtil.closeQuietly(conn);
		}
		
		Jedis jedis = jedisPool.getResource();
		try {
			SurveyCache.delSurveyCount(jedis, companyId, Collections.singleton(surveyId));
			SurveyCache.delQuestionCount(jedis, companyId, questionIdList);
		} finally {
			jedis.close();
		}
		
		return Futures.immediateFuture(SubmitSurveyResponse.newBuilder()
				.setResult(SubmitSurveyResponse.Result.SUCC)
				.build());
	}

	@Override
	public ListenableFuture<GetQuestionAnswerResponse> getQuestionAnswer(
			RequestHead head, GetQuestionAnswerRequest request) {
		final ByteString data = request.hasOffsetIndex() && !request.getOffsetIndex().isEmpty() ? request.getOffsetIndex() : null;
		Long lastUserId = null;
		Integer lastSubmitTime = null;
		SurveyDAOProtos.SurveyResultListIndex offsetIndex = null;
		if (data != null) {
			try {
				offsetIndex = SurveyDAOProtos.SurveyResultListIndex.parseFrom(data);
				lastUserId = offsetIndex.getUserId();
				lastSubmitTime = offsetIndex.getSubmitTime();
			} catch (InvalidProtocolBufferException e) {
				
			}
		}
		
		final long companyId = head.getSession().getCompanyId();
		final int questionId = request.getQuestionId();
		
		SurveyProtos.Question question = SurveyUtil.getSurveyQuestionById(hikariDataSource, jedisPool, companyId, Collections.singleton(questionId)).get(questionId);
		if (question == null) {
			return Futures.immediateFuture(GetQuestionAnswerResponse.newBuilder().setHasMore(false).build());
		}
		
		final int size = request.getSize();
		
		Connection conn = null;
		List<SurveyProtos.Answer> answerList = null;
		try {
			conn = this.hikariDataSource.getConnection();
			
			answerList = SurveyDB.getQuestionAnswer(conn, companyId, questionId, lastUserId, lastSubmitTime, size + 1);
		} catch (SQLException ex) {
			throw new RuntimeException("db fail", ex);
		} finally {
			DBUtil.closeQuietly(conn);
		}
		
		if (answerList.isEmpty()) {
			return Futures.immediateFuture(GetQuestionAnswerResponse.newBuilder().setHasMore(false).build());
		}
		
		List<SurveyProtos.Answer> resultList = answerList.size() > size ? answerList.subList(0, size) : answerList;
		
		SurveyProtos.Answer lastAnswer = resultList.get(resultList.size() - 1);
		
		return Futures.immediateFuture(GetQuestionAnswerResponse.newBuilder()
				.addAllAnswer(resultList)
				.setOffsetIndex(SurveyDAOProtos.SurveyResultListIndex.newBuilder()
						.setSubmitTime(lastAnswer.getAnswerTime())
						.setUserId(lastAnswer.getUserId())
						.build()
						.toByteString())
				.setHasMore(answerList.size() > size)
				.build());
	}

	@Override
	public ListenableFuture<CreateSurveyResponse> createSurvey(
			AdminHead head, CreateSurveyRequest request) {
		if (!head.hasCompanyId()) {
			return Futures.immediateFuture(CreateSurveyResponse.newBuilder()
					.setResult(CreateSurveyResponse.Result.FAIL_UNKNOWN)
					.setFailText("company_id 参数未填")
					.build());
		}
		final long companyId = head.getCompanyId();
		final String surveyName = request.getSurveyName();
		if (surveyName.length() > 191) {
			return Futures.immediateFuture(CreateSurveyResponse.newBuilder()
					.setResult(CreateSurveyResponse.Result.FAIL_SURVEY_NAME_INVALID)
					.setFailText("调研名称过长")
					.build());
		}
		
		final String surveyDesc = request.getSurveyDesc();
		if (surveyDesc.length() > 65535 || surveyDesc.isEmpty()) {
			return Futures.immediateFuture(CreateSurveyResponse.newBuilder()
					.setResult(CreateSurveyResponse.Result.FAIL_SURVEY_DESC_INVALID)
					.setFailText("调研描述不合法")
					.build());
		}
		
		final String imageName = request.hasImageName() ? request.getImageName() : null;
		if (imageName != null && imageName.length() > 191) {
			return Futures.immediateFuture(CreateSurveyResponse.newBuilder()
					.setResult(CreateSurveyResponse.Result.FAIL_SURVEY_IMAGE_INVALID)
					.setFailText("调研图标名称过长")
					.build());
		}
		
		final int startTime = request.getStartTime();
		final int endTime = request.getEndTime();
		if (startTime >= endTime) {
			return Futures.immediateFuture(CreateSurveyResponse.newBuilder()
					.setResult(CreateSurveyResponse.Result.FAIL_TIME_INVALID)
					.setFailText("请确保结束时间大于开始时间")
					.build());
		}
		
		final SurveyProtos.ShowResultType type = request.getShowResultType();
		final Integer allowModelId = request.hasAllowModelId() ? request.getAllowModelId() : null;
		
		final List<SurveyProtos.Question> questionList = request.getQuestionList();
		// 检验每一个题目是否合法
		for (SurveyProtos.Question question : questionList) {
			if (question.getQuestionName().length() > 191) {
				return Futures.immediateFuture(CreateSurveyResponse.newBuilder()
						.setResult(CreateSurveyResponse.Result.FAIL_QUESTION_INVALID)
						.setFailText("题目名称过长，选项：" + question.getQuestionName())
						.build());
			}
			if (question.getTypeCase().equals(SurveyProtos.Question.TypeCase.VOTE)) {
				for (SurveyProtos.Vote.Option option : question.getVote().getOptionList()) {
					if (option.getOptionName().length() > 191) {
						return Futures.immediateFuture(CreateSurveyResponse.newBuilder()
								.setResult(CreateSurveyResponse.Result.FAIL_QUESTION_INVALID)
								.setFailText("选项名称过长，选项：" + option.getOptionName())
								.build());
					}
				}
			} else if (question.getTypeCase().equals(SurveyProtos.Question.TypeCase.INPUT_SELECT)) {
				for (SurveyProtos.InputSelect.Option option : question.getInputSelect().getOptionList()) {
					if (option.getOptionName().length() > 191) {
						return Futures.immediateFuture(CreateSurveyResponse.newBuilder()
								.setResult(CreateSurveyResponse.Result.FAIL_QUESTION_INVALID)
								.setFailText("选项名称过长，选项：" + option.getOptionName())
								.build());
					}
				}
			}
		}
		
		final int now = (int) (System.currentTimeMillis() / 1000L);
		final long createAdminId = head.getSession().getAdminId();
		
		Connection conn = null;
		int surveyId = 0;
		try {
			conn = this.hikariDataSource.getConnection();
			
			surveyId = SurveyDB.insertSurvey(conn, companyId, surveyName, surveyDesc, imageName, startTime, endTime, type, allowModelId, now, createAdminId, questionList);
		} catch (SQLException ex) {
			throw new RuntimeException("db fail", ex);
		} finally {
			DBUtil.closeQuietly(conn);
		}
		
		Jedis jedis = jedisPool.getResource();
		try {
			SurveyCache.delSurvey(jedis, companyId, Collections.singleton(surveyId));
		} finally {
			jedis.close();
		}

		this.serviceExecutor.execute(new SendSecretaryMessageTask(head, surveyName, startTime, endTime, allowModelId));

		return Futures.immediateFuture(CreateSurveyResponse.newBuilder()
				.setResult(CreateSurveyResponse.Result.SUCC)
				.setSurveyId(surveyId)
				.build());
	}

	@Override
	public ListenableFuture<UpdateSurveyResponse> updateSurvey(
			AdminHead head, UpdateSurveyRequest request) {
		if (!head.hasCompanyId()) {
			return Futures.immediateFuture(UpdateSurveyResponse.newBuilder()
					.setResult(UpdateSurveyResponse.Result.FAIL_UNKNOWN)
					.setFailText("company_id 参数未填")
					.build());
		}
		final long companyId = head.getCompanyId();
		final int surveyId = request.getSurveyId();
		
		SurveyProtos.Survey survey = SurveyUtil.getSurveyById(hikariDataSource, jedisPool, companyId, Collections.singleton(surveyId)).get(surveyId);
		if (survey == null) {
			return Futures.immediateFuture(UpdateSurveyResponse.newBuilder()
					.setResult(UpdateSurveyResponse.Result.FAIL_SURVEY_INVALID)
					.setFailText("不存在的调研")
					.build());
		}
		
		final String surveyName = request.getSurveyName();
		if (surveyName.length() > 191 || surveyName.isEmpty()) {
			return Futures.immediateFuture(UpdateSurveyResponse.newBuilder()
					.setResult(UpdateSurveyResponse.Result.FAIL_SURVEY_NAME_INVALID)
					.setFailText("调研名称不合法")
					.build());
		}
		
		final String surveyDesc = request.getSurveyDesc();
		if (surveyDesc.length() > 65535 || surveyDesc.isEmpty()) {
			return Futures.immediateFuture(UpdateSurveyResponse.newBuilder()
					.setResult(UpdateSurveyResponse.Result.FAIL_SURVEY_DESC_INVALID)
					.setFailText("调研描述不合法")
					.build());
		}
		
		final String imageName = request.hasImageName() ? request.getImageName() : null;
		if (imageName != null && imageName.length() > 191) {
			return Futures.immediateFuture(UpdateSurveyResponse.newBuilder()
					.setResult(UpdateSurveyResponse.Result.FAIL_SUVERY_IMAGE_INVALID)
					.setFailText("调研图标名称过长")
					.build());
					
		}
		
		final int startTime = request.getStartTime();
		final int endTime = request.getEndTime();
		if (startTime >= endTime) {
			return Futures.immediateFuture(UpdateSurveyResponse.newBuilder()
					.setResult(UpdateSurveyResponse.Result.FAIL_TIME_INVALID)
					.setFailText("请确保结束时间大于开始时间")
					.build());
		}
		
		final SurveyProtos.ShowResultType type = request.getShowResultType();
		final Integer allowModelId = request.hasAllowModelId() ? request.getAllowModelId() : null;
		
		final int now = (int) (System.currentTimeMillis() / 1000L);
		final long updateAdminId = head.getSession().getAdminId();
		
		Connection conn = null;
		try {
			conn = this.hikariDataSource.getConnection();
			
			SurveyDB.updateSurvey(conn, companyId, surveyId, surveyName, surveyDesc, imageName, startTime, endTime, type, allowModelId, now, updateAdminId);
		} catch (SQLException ex) {
			throw new RuntimeException("db fail", ex);
		} finally {
			DBUtil.closeQuietly(conn);
		}
		
		Jedis jedis = jedisPool.getResource();
		try {
			SurveyCache.delSurvey(jedis, companyId, Collections.singleton(surveyId));
		} finally {
			jedis.close();
		}
		
		return Futures.immediateFuture(UpdateSurveyResponse.newBuilder()
				.setResult(UpdateSurveyResponse.Result.SUCC)
				.build());
	}

	@Override
	public ListenableFuture<CreateQuestionResponse> createQuestion(
			AdminHead head, CreateQuestionRequest request) {
		if (!head.hasCompanyId()) {
			return Futures.immediateFuture(CreateQuestionResponse.newBuilder()
					.setResult(CreateQuestionResponse.Result.FAIL_UNKNOWN)
					.setFailText("company_id 参数未填")
					.build());
		}
		final long companyId = head.getCompanyId();
		final int surveyId = request.getSurveyId();
		
		SurveyProtos.Survey survey = SurveyUtil.getSurveyById(hikariDataSource, jedisPool, companyId, Collections.singleton(surveyId)).get(surveyId);
		if (survey == null) {
			return Futures.immediateFuture(CreateQuestionResponse.newBuilder()
					.setResult(CreateQuestionResponse.Result.FAIL_SURVEY_INVALID)
					.setFailText("调研不存在")
					.build());
		}

		final String questionName = request.getQuestionName();
		if (questionName.length() > 191) {
			return Futures.immediateFuture(CreateQuestionResponse.newBuilder()
					.setResult(CreateQuestionResponse.Result.FAIL_QUESTION_NAME_INVALID)
					.setFailText("题目名称过长")
					.build());
		}
		
		final String imageName = request.hasImageName() ? request.getImageName() : "";
		if (imageName.length() > 191) {
			return Futures.immediateFuture(CreateQuestionResponse.newBuilder()
					.setResult(CreateQuestionResponse.Result.FAIL_IMAGE_NAME_INVALID)
					.setFailText("题目图片名称过长")
					.build());
		}
		
		final int now = (int) (System.currentTimeMillis() / 1000L);
		final long createAdminId = head.getSession().getAdminId();
		
		SurveyProtos.Question.Builder questionBuilder = SurveyProtos.Question.newBuilder()
				.setQuestionId(0)
				.setQuestionName(questionName)
				.setImageName(imageName)
				.setState(SurveyProtos.State.NORMAL)
				.setCreateTime(now)
				.setCreateAdminId(createAdminId);
		
		if (request.hasIsOptional()) {
			questionBuilder.setIsOptional(request.getIsOptional());
		}
		
		SurveyProtos.Vote.Question voteQuestion = null;
		SurveyProtos.InputSelect.Question inputSelectQuestion = null;
		SurveyProtos.InputText.Question inputTextQuestion = null;
		
		if (request.getTypeCase().equals(CreateQuestionRequest.TypeCase.VOTE)) {
			voteQuestion = request.getVote();
			questionBuilder.setVote(voteQuestion);
			
		} else if (request.getTypeCase().equals(CreateQuestionRequest.TypeCase.INPUT_SELECT)) {
			inputSelectQuestion = request.getInputSelect();
			questionBuilder.setInputSelect(inputSelectQuestion);
			
		} else if (request.getTypeCase().equals(CreateQuestionRequest.TypeCase.INPUT_TEXT)) {
			inputTextQuestion = request.getInputText();
			questionBuilder.setInputText(inputTextQuestion);
			
		} else {
			return Futures.immediateFuture(CreateQuestionResponse.newBuilder()
					.setResult(CreateQuestionResponse.Result.FAIL_OPTION_INVALID)
					.setFailText("无效的问题类型")
					.build());
					
		}
		
		Connection conn = null;
		List<Integer> questionIdList = null;
		try {
			conn = this.hikariDataSource.getConnection();
			
			questionIdList = SurveyDB.insertQuestion(conn, companyId, surveyId, now, createAdminId, Arrays.asList(questionBuilder.build()));
		} catch (SQLException ex) {
			throw new RuntimeException("db fail", ex);
		} finally {
			DBUtil.closeQuietly(conn);
		}
		
		if (questionIdList.isEmpty()) {
			return Futures.immediateFuture(CreateQuestionResponse.newBuilder()
					.setResult(CreateQuestionResponse.Result.FAIL_UNKNOWN)
					.setFailText("创建题目失败")
					.build());
		}
		
		Jedis jedis = jedisPool.getResource();
		try {
			SurveyCache.delQuestion(jedis, companyId, questionIdList);
		} finally {
			jedis.close();
		}
		
		return Futures.immediateFuture(CreateQuestionResponse.newBuilder()
				.setResult(CreateQuestionResponse.Result.SUCC)
				.setQuestionId(questionIdList.get(0))
				.build());
	}

	@Override
	public ListenableFuture<UpdateQuestionResponse> updateQuestion(
			AdminHead head, UpdateQuestionRequest request) {
		if (!head.hasCompanyId()) {
			return Futures.immediateFuture(UpdateQuestionResponse.newBuilder()
					.setResult(UpdateQuestionResponse.Result.FAIL_UNKNOWN)
					.setFailText("company_id 参数未填")
					.build());
		}
		final long companyId = head.getCompanyId();
		final int questionId = request.getQuestionId();
		SurveyProtos.UpdateQuestionRequest.TypeCase questionType = request.getTypeCase();
		
		SurveyProtos.Question question = SurveyUtil.getSurveyQuestionById(hikariDataSource, jedisPool, companyId, Collections.singleton(questionId)).get(questionId);
		
		if (question == null) {
			return Futures.immediateFuture(UpdateQuestionResponse.newBuilder()
					.setResult(UpdateQuestionResponse.Result.FAIL_QUESTION_NOT_EXSIT)
					.setFailText("不存在的题目")
					.build());
		}
		if (!question.getTypeCase().name().equals(questionType.name())) {
			return Futures.immediateFuture(UpdateQuestionResponse.newBuilder()
					.setResult(UpdateQuestionResponse.Result.FAIL_QUESTION_TYPE_INVALID)
					.setFailText("新题目类型和旧题目类型不一致，新类型：" + questionType + "，旧类型：" + question.getTypeCase().name())
					.build());
		}
		
		final String questionName = request.getQuestionName();
		if (questionName.length() > 191) {
			return Futures.immediateFuture(UpdateQuestionResponse.newBuilder()
					.setResult(UpdateQuestionResponse.Result.FAIL_QUESTION_NAME_INVALID)
					.setFailText("题目名称过长")
					.build());
		}
		
		final String imageName = request.hasImageName() ? request.getImageName() : null;
		if (imageName != null && imageName.length() > 191) {
			return Futures.immediateFuture(UpdateQuestionResponse.newBuilder()
					.setResult(UpdateQuestionResponse.Result.FAIL_IMAGE_NAME_INVALID)
					.setFailText("图片名称过长")
					.build());
		}
		
		final int now = (int) (System.currentTimeMillis() / 1000L);
		final long updateAdminId = head.getSession().getAdminId();
		
		SurveyProtos.Question.Builder questionBuilder = SurveyProtos.Question.newBuilder()
				.setQuestionId(questionId)
				.setQuestionName(questionName)
				.setImageName(imageName)
				.setState(SurveyProtos.State.NORMAL)
				.setCreateTime(now)
				.setCreateAdminId(updateAdminId);
		
		if (request.hasIsOptional()) {
			questionBuilder.setIsOptional(request.getIsOptional());
		}
		
		SurveyProtos.Vote.Question voteQuestion = null;
		SurveyProtos.InputSelect.Question inputSelectQuestion = null;
		SurveyProtos.InputText.Question inputTextQuestion = null;
		
		if (request.getTypeCase().equals(UpdateQuestionRequest.TypeCase.VOTE)) {
			voteQuestion = request.getVote();
			questionBuilder.setVote(voteQuestion);
			
		} else if (request.getTypeCase().equals(UpdateQuestionRequest.TypeCase.INPUT_SELECT)) {
			inputSelectQuestion = request.getInputSelect();
			questionBuilder.setInputSelect(inputSelectQuestion);
			
		} else if (request.getTypeCase().equals(UpdateQuestionRequest.TypeCase.INPUT_TEXT)) {
			inputTextQuestion = request.getInputText();
			questionBuilder.setInputText(inputTextQuestion);
			
		}
		
		Connection conn = null;
		try {
			conn = this.hikariDataSource.getConnection();
			
			SurveyDB.updateQuestion(conn, companyId, now, updateAdminId, Collections.singleton(questionBuilder.build()));
		} catch (SQLException ex) {
			throw new RuntimeException("db fail", ex);
		} finally {
			DBUtil.closeQuietly(conn);
		}
		
		Jedis jedis = jedisPool.getResource();
		try {
			SurveyCache.delQuestion(jedis, companyId, Collections.singleton(questionId));
		} finally {
			jedis.close();
		}
		
		return Futures.immediateFuture(UpdateQuestionResponse.newBuilder()
				.setResult(UpdateQuestionResponse.Result.SUCC)
				.build());
	}

	@Override
	public ListenableFuture<DeleteQuestionResponse> deleteQuestion(
			AdminHead head, DeleteQuestionRequest request) {
		if (!head.hasCompanyId()) {
			return Futures.immediateFuture(DeleteQuestionResponse.newBuilder()
					.setResult(DeleteQuestionResponse.Result.FAIL_UNKNOWN)
					.setFailText("company_id 参数未填")
					.build());
		}
		final long companyId = head.getCompanyId();
		final int surveyId = request.getSurveyId();
		final List<Integer> questionIdList = request.getQuestionIdList();
		
		Connection conn = null;
		try {
			conn = this.hikariDataSource.getConnection();
			
			SurveyDB.deleteQuestion(conn, companyId, surveyId, questionIdList);
		} catch (SQLException ex) {
			throw new RuntimeException("db fail", ex);
		} finally {
			DBUtil.closeQuietly(conn);
		}
		
		Jedis jedis = jedisPool.getResource();
		try {
			SurveyCache.delQuestion(jedis, companyId, questionIdList);
		} finally {
			jedis.close();
		}
		
		return Futures.immediateFuture(DeleteQuestionResponse.newBuilder()
				.setResult(DeleteQuestionResponse.Result.SUCC)
				.build());
	}

	@Override
	public ListenableFuture<DisableSurveyResponse> disableSurvey(
			AdminHead head, DisableSurveyRequest request) {
		if (!head.hasCompanyId()) {
			return Futures.immediateFuture(DisableSurveyResponse.newBuilder()
					.setResult(DisableSurveyResponse.Result.FAIL_UNKNOWN)
					.setFailText("company_id 参数未填")
					.build());
		}
		final long companyId = head.getCompanyId();
		final List<Integer> surveyIdList = request.getSurveyIdList();
		
		Connection conn = null;
		try {
			conn = this.hikariDataSource.getConnection();
			
			SurveyDB.disableSurvey(conn, companyId, surveyIdList);
		} catch (SQLException ex) {
			throw new RuntimeException("db fail", ex);
		} finally {
			DBUtil.closeQuietly(conn);
		}
		
		Jedis jedis = jedisPool.getResource();
		try {
			SurveyCache.delSurvey(jedis, companyId, surveyIdList);
		} finally {
			jedis.close();
		}
		
		return Futures.immediateFuture(DisableSurveyResponse.newBuilder()
				.setResult(DisableSurveyResponse.Result.SUCC)
				.build());
	}
	
	@Override
	public ListenableFuture<EnableSurveyResponse> enableSurvey(
			AdminHead head, EnableSurveyRequest request) {
		if (!head.hasCompanyId()) {
			return Futures.immediateFuture(EnableSurveyResponse.newBuilder()
					.setResult(EnableSurveyResponse.Result.FAIL_UNKNOWN)
					.setFailText("company_id 参数未填")
					.build());
		}
		final long companyId = head.getCompanyId();
		final List<Integer> surveyIdList = request.getSurveyIdList();
		
		Connection conn = null;
		try {
			conn = this.hikariDataSource.getConnection();
			
			SurveyDB.enableSurvey(conn, companyId, surveyIdList);
		} catch (SQLException ex) {
			throw new RuntimeException("db fail", ex);
		} finally {
			DBUtil.closeQuietly(conn);
		}
		
		Jedis jedis = jedisPool.getResource();
		try {
			SurveyCache.delSurvey(jedis, companyId, surveyIdList);
		} finally {
			jedis.close();
		}
		
		return Futures.immediateFuture(EnableSurveyResponse.newBuilder()
				.setResult(EnableSurveyResponse.Result.SUCC)
				.build());
	}

	@Override
	public ListenableFuture<DeleteSurveyResponse> deleteSurvey(
			AdminHead head, DeleteSurveyRequest request) {
		if (!head.hasCompanyId()) {
			return Futures.immediateFuture(DeleteSurveyResponse.newBuilder()
					.setResult(DeleteSurveyResponse.Result.FAIL_UNKNOWN)
					.setFailText("company_id 参数未填")
					.build());
		}
		final long companyId = head.getCompanyId();
		final List<Integer> surveyIdList = request.getSurveyIdList();
		
		Connection conn = null;
		try {
			conn = this.hikariDataSource.getConnection();
			
			SurveyDB.deleteSurvey(conn, companyId, surveyIdList);
		} catch (SQLException ex) {
			throw new RuntimeException("db fail", ex);
		} finally {
			DBUtil.closeQuietly(conn);
		}
		
		Jedis jedis = jedisPool.getResource();
		try {
			SurveyCache.delSurvey(jedis, companyId, surveyIdList);
		} finally {
			jedis.close();
		}
		
		return Futures.immediateFuture(DeleteSurveyResponse.newBuilder()
				.setResult(DeleteSurveyResponse.Result.SUCC)
				.build());
	}
	
	@Override
	public ListenableFuture<GetSurveyByIdResponse> getSurveyById(
			AdminHead head, GetSurveyByIdRequest request) {
		if (!head.hasCompanyId()) {
			return Futures.immediateFuture(GetSurveyByIdResponse.newBuilder()
					.setResult(GetSurveyByIdResponse.Result.FAIL_UNKNOWN)
					.setFailText("company_id 参数未填")
					.build());
		}
		final long companyId = head.getCompanyId();
		final int surveyId = request.getSurveyId();
		
		SurveyProtos.Survey survey = SurveyUtil.getSurveyById(hikariDataSource, jedisPool, companyId, Collections.singleton(surveyId)).get(surveyId);
		if (survey == null) {
			return Futures.immediateFuture(GetSurveyByIdResponse.newBuilder()
					.setResult(GetSurveyByIdResponse.Result.FAIL_SURVEY_NOT_EXSIT)
					.setFailText("此调研不存在")
					.build());
		}
		
		Connection conn = null;
		List<Integer> questionIdList = null;
		try {
			conn = this.hikariDataSource.getConnection();
			
			questionIdList = SurveyDB.getQuestionIdBySurveyId(conn, companyId, surveyId);
		} catch (SQLException ex) {
			throw new RuntimeException("db fail", ex);
		} finally {
			DBUtil.closeQuietly(conn);
		}
		
		SurveyProtos.Survey.Builder surveyBuilder = SurveyProtos.Survey.newBuilder()
				.mergeFrom(survey);
		
		// 添加调研参与人数
		SurveyDAOProtos.SurveyCount surveyCount = SurveyUtil.getSurveyCount(hikariDataSource, jedisPool, companyId, Collections.singleton(surveyId)).get(surveyId);
		if (surveyCount != null) {
			surveyBuilder.setSurveyUserCnt(surveyCount.getSurveyCount());
		}
		
		Map<Integer, SurveyProtos.Question> questionMap = SurveyUtil.getSurveyQuestionById(hikariDataSource, jedisPool, companyId, questionIdList);
		
		// 添加考题的参与人数
		Map<Integer, SurveyDAOProtos.QuestionCount> questionCountMap = SurveyUtil.getQuestionCount(hikariDataSource, jedisPool, companyId, questionMap.keySet());
		
		List<SurveyProtos.Question> questionList = new ArrayList<SurveyProtos.Question>();
		for (Entry<Integer, SurveyProtos.Question> entry : questionMap.entrySet()) {
			SurveyDAOProtos.QuestionCount questionCount = questionCountMap.get(entry.getKey());
			if (questionCount != null) {
				// 添加option count
				List<SurveyDAOProtos.QuestionCount.OptionCount> optionCountList = questionCount.getOptionCountList();
				Map<Integer, Integer> optionCntMap = new HashMap<Integer, Integer>();
				for (SurveyDAOProtos.QuestionCount.OptionCount optionCount : optionCountList) {
					optionCntMap.put(optionCount.getOptionId(), optionCount.getOptionCount());
				}
				
				List<SurveyProtos.Vote.Option> optionList = entry.getValue().getVote().getOptionList();
				
				List<SurveyProtos.Vote.Option> tmpOptionList = new ArrayList<SurveyProtos.Vote.Option>();
				for (SurveyProtos.Vote.Option voteOption : optionList) {
					Integer optionCount = optionCntMap.get(voteOption.getOptionId()); 
					if (optionCount != null) {
						tmpOptionList.add(SurveyProtos.Vote.Option.newBuilder()
								.mergeFrom(voteOption)
								.setOptionCnt(optionCount)
								.build());
					} else {
						tmpOptionList.add(voteOption);
					}
					
				}
				
				// 添加question count
				SurveyProtos.Vote.Question voteQuestion = SurveyProtos.Vote.Question.newBuilder()
						.setCheckNum(entry.getValue().getVote().getCheckNum())
						.setTotalCnt(questionCount.getQuestionCount())
						.addAllOption(tmpOptionList)
						.build();
				
				questionList.add(SurveyProtos.Question.newBuilder()
						.mergeFrom(entry.getValue())
						.setVote(voteQuestion)
						.build());
			} else {
				questionList.add(entry.getValue());
			}
			
		}
		
		// 返回有序的题目,把余下的题目拼装到最后
		List<SurveyProtos.Question> questionOrderList = new ArrayList<SurveyProtos.Question>();
		for (int questionId : questionIdList) {
			for (SurveyProtos.Question question : questionList) {
				if (question.getQuestionId() == questionId) {
					questionOrderList.add(question);
				}
			}
		}
		
		GetSurveyByIdResponse.Builder getSurveyByIdResponseBuilder = GetSurveyByIdResponse.newBuilder()
				.setResult(GetSurveyByIdResponse.Result.SUCC)
				.setSurvey(surveyBuilder.build())
				.addAllQuestion(questionOrderList);
		
		return Futures.immediateFuture(getSurveyByIdResponseBuilder.build());
	}

	@Override
	public ListenableFuture<GetSurveyResultResponse> getSurveyResult(
			RequestHead head, GetSurveyResultRequest request) {
		final ByteString data = request.hasOffsetIndex() && !request.getOffsetIndex().isEmpty() ? request.getOffsetIndex() : null;
		Long lastUserId = null;
		Integer lastSubmitTime = null;
		SurveyDAOProtos.SurveyResultListIndex offsetIndex = null;
		if (data != null) {
			try {
				offsetIndex = SurveyDAOProtos.SurveyResultListIndex.parseFrom(data);
				lastUserId = offsetIndex.getUserId();
				lastSubmitTime = offsetIndex.getSubmitTime();
			} catch (InvalidProtocolBufferException e) {
				
			}
		}
		
		final long companyId = head.getSession().getCompanyId();
		final int surveyId = request.getSurveyId();
		
		SurveyProtos.Survey survey = SurveyUtil.getSurveyById(hikariDataSource, jedisPool, companyId, Collections.singleton(surveyId)).get(surveyId);
		if (survey == null) {
			return Futures.immediateFuture(GetSurveyResultResponse.newBuilder()
					.setHasMore(false)
					.build());
		}
		
		final int size = request.getSize();
		
		Connection conn = null;
		Map<Long, SurveyProtos.SurveyResult> userSurveyResult = null;
		try {
			conn = this.hikariDataSource.getConnection();
			
			userSurveyResult = SurveyDB.getSurveyUserResult(conn, companyId, surveyId, lastUserId, lastSubmitTime, size + 1);
		} catch (SQLException ex) {
			throw new RuntimeException("db fail", ex);
		} finally {
			DBUtil.closeQuietly(conn);
		}
		
		if (userSurveyResult.isEmpty()) {
			return Futures.immediateFuture(GetSurveyResultResponse.newBuilder()
					.setHasMore(false)
					.build());
		}
		
		List<SurveyProtos.SurveyResult> surveyResultList = new ArrayList<SurveyProtos.SurveyResult>();
		for (Entry<Long, SurveyProtos.SurveyResult> entry : userSurveyResult.entrySet()) {
			surveyResultList.add(entry.getValue());
		}
		
		int resultSize = surveyResultList.size();
		SurveyProtos.SurveyResult surveyResult = resultSize > size ? surveyResultList.get(size - 1) : surveyResultList.get(resultSize - 1);
		
		long userId = surveyResult.getUserId();
		int submitTime = surveyResult.getSubmitTime();
		
		return Futures.immediateFuture(GetSurveyResultResponse.newBuilder()
				.setHasMore(resultSize > size)
				.setOffsetIndex(SurveyDAOProtos.SurveyResultListIndex.newBuilder()
						.setUserId(userId)
						.setSubmitTime(submitTime)
						.build().toByteString())
				.addAllSurveyResult(resultSize > size ? surveyResultList.subList(0, size) : surveyResultList)
				.build());
	}

	private Set<Integer> doCheckAllowModelId(RequestHead head, Set<Integer> modelIdSet) {
		if (modelIdSet.isEmpty()) {
			return Collections.emptySet();
		} 
		
		AllowProtos.CheckAllowResponse checkAllowResponse = Futures.getUnchecked(
				this.allowService.checkAllow(head, AllowProtos.CheckAllowRequest.newBuilder()
						.addAllModelId(modelIdSet)
						.addUserId(head.getSession().getUserId())
						.build()));
		
		Set<Integer> allowedModelIdSet = new TreeSet<Integer>();
		for (AllowProtos.CheckAllowResponse.CheckResult checkResult : checkAllowResponse.getCheckResultList()) {
			if (checkResult.getAllowUserIdList().contains(head.getSession().getUserId())) {
				allowedModelIdSet.add(checkResult.getModelId());
			}
		}
		
		return allowedModelIdSet;
	}

	@Override
	public ListenableFuture<GetQuestionAnswerResponse> getQuestionAnswer(
			AdminHead head, GetQuestionAnswerRequest request) {
		if (!head.hasCompanyId()) {
			return Futures.immediateFuture(GetQuestionAnswerResponse.newBuilder().build());
		}
		final long companyId = head.getCompanyId();
		
		final ByteString data = request.hasOffsetIndex() && !request.getOffsetIndex().isEmpty() ? request.getOffsetIndex() : null;
		Long lastUserId = null;
		Integer lastSubmitTime = null;
		SurveyDAOProtos.SurveyResultListIndex offsetIndex = null;
		if (data != null) {
			try {
				offsetIndex = SurveyDAOProtos.SurveyResultListIndex.parseFrom(data);
				lastUserId = offsetIndex.getUserId();
				lastSubmitTime = offsetIndex.getSubmitTime();
			} catch (InvalidProtocolBufferException e) {
				
			}
		}
		
		final int questionId = request.getQuestionId();
		
		SurveyProtos.Question question = SurveyUtil.getSurveyQuestionById(hikariDataSource, jedisPool, companyId, Collections.singleton(questionId)).get(questionId);
		if (question == null) {
			return Futures.immediateFuture(GetQuestionAnswerResponse.newBuilder().setHasMore(false).build());
		}
		
		final int size = request.getSize();
		
		Connection conn = null;
		List<SurveyProtos.Answer> answerList = null;
		try {
			conn = this.hikariDataSource.getConnection();
			
			answerList = SurveyDB.getQuestionAnswer(conn, companyId, questionId, lastUserId, lastSubmitTime, size + 1);
		} catch (SQLException ex) {
			throw new RuntimeException("db fail", ex);
		} finally {
			DBUtil.closeQuietly(conn);
		}
		
		if (answerList.isEmpty()) {
			return Futures.immediateFuture(GetQuestionAnswerResponse.newBuilder().setHasMore(false).build());
		}
		
		List<SurveyProtos.Answer> resultList = answerList.size() > size ? answerList.subList(0, size) : answerList;
		
		SurveyProtos.Answer lastAnswer = resultList.get(resultList.size() - 1);
		
		return Futures.immediateFuture(GetQuestionAnswerResponse.newBuilder()
				.addAllAnswer(resultList)
				.setOffsetIndex(SurveyDAOProtos.SurveyResultListIndex.newBuilder()
						.setSubmitTime(lastAnswer.getAnswerTime())
						.setUserId(lastAnswer.getUserId())
						.build()
						.toByteString())
				.setHasMore(answerList.size() > size)
				.build());
	}

	@Override
	public ListenableFuture<GetSurveyListResponse> getSurveyList(
			AdminHead head, GetSurveyListRequest request) {
		if (!head.hasCompanyId()) {
			return Futures.immediateFuture(GetSurveyListResponse.newBuilder()
					.setTotal(0)
					.setFilteredSize(0)
					.build());
		}
		final long companyId = head.getCompanyId();
		
		final int start = request.getStart() < 0 ? 0 : request.getStart();
		final int length = request.getLength() < 0 ? 0 : request.getLength() > 50 ? 50 : request.getLength();
		
		final String surveyName = request.hasSurveyName() ? request.getSurveyName() : null;
		
		Connection conn = null;
		DataPage<Integer> surveyIdPage = null;
		try {
			conn = this.hikariDataSource.getConnection();
			
			surveyIdPage = SurveyDB.getSurveyIdPage(conn, companyId, start, length, surveyName);
		} catch (SQLException ex) {
			throw new RuntimeException("db fail", ex);
		} finally {
			DBUtil.closeQuietly(conn);
		}
		
		List<Integer> surveyIdList = surveyIdPage.dataList();
		Map<Integer, SurveyProtos.Survey> surveyMap = SurveyUtil.getSurveyById(hikariDataSource, jedisPool, companyId, surveyIdList);
		// 按照dataList顺序排序
		List<SurveyProtos.Survey> orderSurveyList = new ArrayList<SurveyProtos.Survey>();
		for (int surveyId : surveyIdList) {
			SurveyProtos.Survey survey = surveyMap.get(surveyId);
			if (survey != null) {
				orderSurveyList.add(survey);
			}
		}
		
		return Futures.immediateFuture(GetSurveyListResponse.newBuilder()
				.addAllSurvey(orderSurveyList)
				.setTotal(surveyIdPage.totalSize())
				.setFilteredSize(surveyIdPage.filteredSize())
				.build());
	}

	@Override
	public ListenableFuture<GetSurveyResultListResponse> getSurveyResultList(
			AdminHead head, GetSurveyResultListRequest request) {
		if (!head.hasCompanyId()) {
			return Futures.immediateFuture(GetSurveyResultListResponse.newBuilder()
					.setTotal(0)
					.setFilteredSize(0)
					.build());
		}
		final long companyId = head.getCompanyId();
		
		final int start = request.getStart() < 0 ? 0 : request.getStart();
		final int length = request.getLength() < 0 ? 0 : request.getLength() > 100 ? 100 : request.getLength();
		
		final int surveyId = request.getSurveyId();
		
		Connection conn = null;
		try {
			conn = this.hikariDataSource.getConnection();
			
			Map<Long, SurveyProtos.SurveyResult> surveyResultMap = SurveyDB.getSurveyUserResult(conn, companyId, surveyId, start, length);
			int total = SurveyDB.getSurveyUserResultCount(conn, companyId, surveyId);
			return Futures.immediateFuture(GetSurveyResultListResponse.newBuilder()
					.addAllSurveyResult(surveyResultMap.values())
					.setTotal(total)
					.setFilteredSize(total)
					.build());
		} catch (SQLException ex) {
			throw new RuntimeException("db fail", ex);
		} finally {
			DBUtil.closeQuietly(conn);
		}
	}

	@Override
	public ListenableFuture<QuestionSortResponse> questionSort(AdminHead head,
			QuestionSortRequest request) {
		if (!head.hasCompanyId()) {
			return Futures.immediateFuture(QuestionSortResponse.newBuilder()
					.setResult(QuestionSortResponse.Result.FAIL_UNKNOWN)
					.setFailText("company_id 参数未填")
					.build());
		}
		final long companyId = head.getCompanyId();
		final int surveyId = request.getSurveyId();
		SurveyProtos.Survey survey = SurveyUtil.getSurveyById(hikariDataSource, jedisPool, companyId, Collections.singleton(surveyId)).get(surveyId);
		if (survey == null) {
			return Futures.immediateFuture(QuestionSortResponse.newBuilder()
					.setResult(QuestionSortResponse.Result.FAIL_SURVEY_INVALID)
					.setFailText("调研不存在！")
					.build());
		}
		
		final List<Integer> questionIdList = request.getQuestionIdList();
		List<Integer> filteredQuestionIdList = new ArrayList<Integer>();
		Map<Integer, SurveyProtos.Question> questionMap = SurveyUtil.getSurveyQuestionById(hikariDataSource, jedisPool, companyId, questionIdList);
		for (int questionId : questionIdList) {
			if (questionMap.get(questionId) != null) {
				filteredQuestionIdList.add(questionId);
			}
		}
		
		Connection conn = null;
		try {
			conn = this.hikariDataSource.getConnection();
			
			SurveyDB.updateQuestionOrder(conn, companyId, surveyId, filteredQuestionIdList);
			
			return Futures.immediateFuture(QuestionSortResponse.newBuilder()
					.setResult(QuestionSortResponse.Result.SUCC)
					.build());
		} catch (SQLException ex) {
			throw new RuntimeException("db fail", ex);
		} finally {
			DBUtil.closeQuietly(conn);
		}
	}

	@Override
	public ListenableFuture<ImportQuestionResponse> importQuestion(
			AdminHead head, ImportQuestionRequest request) {
		if (!head.hasCompanyId()) {
			return Futures.immediateFuture(ImportQuestionResponse.newBuilder()
					.setResult(ImportQuestionResponse.Result.FAIL_UNKNOWN)
					.setFailText("company_id 参数未填")
					.build());
		}
		final long companyId = head.getCompanyId();
		final int surveyId = request.getSurveyId();
		
		SurveyProtos.Survey survey = SurveyUtil.getSurveyById(hikariDataSource, jedisPool, companyId, Collections.singleton(surveyId)).get(surveyId);
		if (survey == null) {
			return Futures.immediateFuture(ImportQuestionResponse.newBuilder()
					.setResult(ImportQuestionResponse.Result.FAIL_SURVEY_INVALID)
					.setFailText("调研不存在")
					.build());
		}
		
		int now = (int) (System.currentTimeMillis() / 1000L);
		long createAdminId = head.getSession().getAdminId();

		List<SurveyProtos.Question> questionList = request.getQuestionList();
		List<ImportQuestionResponse.InvalidQuestion> invalidQuestionList = new ArrayList<ImportQuestionResponse.InvalidQuestion>();
		ImportQuestionResponse.InvalidQuestion.Builder invalidQuestionBuilder = null;
		for (SurveyProtos.Question question : questionList) {
			String questionName = question.getQuestionName();
			if (questionName.length() > 191) {
				invalidQuestionBuilder = ImportQuestionResponse.InvalidQuestion.newBuilder()
						.setQuestionName(questionName)
						.setFailText("题目名称过长");
				continue;
			}
			
			String imageName = question.hasImageName() ? question.getImageName() : "";
			if (imageName.length() > 191) {
				invalidQuestionBuilder = ImportQuestionResponse.InvalidQuestion.newBuilder()
						.setQuestionName(questionName)
						.setFailText("题目图片名称过长");
				continue;
			}
			
			SurveyProtos.Vote.Question voteQuestion = null;
			SurveyProtos.InputSelect.Question inputSelectQuestion = null;
			SurveyProtos.InputText.Question inputTextQuestion = null;
			
			if (question.getTypeCase().equals(SurveyProtos.Question.TypeCase.VOTE)) {
				voteQuestion = question.getVote();
				for (SurveyProtos.Vote.Option option : voteQuestion.getOptionList()) {
					String optionName = option.getOptionName();
					if (optionName.length() > 191) {
						invalidQuestionBuilder = ImportQuestionResponse.InvalidQuestion.newBuilder()
								.setQuestionName(questionName)
								.setFailText("选项名称过长");
						break;
					}
					
					String optionImageName = option.hasImageName() ? option.getImageName() : "";
					if (optionImageName.length() > 191) {
						invalidQuestionBuilder = ImportQuestionResponse.InvalidQuestion.newBuilder()
								.setQuestionName(questionName)
								.setFailText("选项图片名称过长");
						break;
					}
				}
				
			} else if (question.getTypeCase().equals(SurveyProtos.Question.TypeCase.INPUT_SELECT)) {
				inputSelectQuestion = question.getInputSelect();
				for (SurveyProtos.InputSelect.Option option : inputSelectQuestion.getOptionList()) {
					String optionName = option.getOptionName();
					if (optionName.length() > 191) {
						invalidQuestionBuilder = ImportQuestionResponse.InvalidQuestion.newBuilder()
								.setQuestionName(questionName)
								.setFailText("选项名称过长");
						break;
					}
				}
				
			} else if (question.getTypeCase().equals(SurveyProtos.Question.TypeCase.INPUT_TEXT)) {
				inputTextQuestion = question.getInputText();
				if (inputTextQuestion.getInputPrompt().length() > 191) {
					invalidQuestionBuilder = ImportQuestionResponse.InvalidQuestion.newBuilder()
							.setQuestionName(questionName)
							.setFailText("题目备注过长");
				}
				
			} else {
				invalidQuestionBuilder = ImportQuestionResponse.InvalidQuestion.newBuilder()
						.setQuestionName(questionName)
						.setFailText("错误的题目类型");
			}
			
			if (invalidQuestionBuilder != null) {
				invalidQuestionList.add(invalidQuestionBuilder.build());
			}
			
		}
		
		if (!invalidQuestionList.isEmpty()) {
			return Futures.immediateFuture(ImportQuestionResponse.newBuilder()
					.setResult(ImportQuestionResponse.Result.FAIL_QUESTION_INVALID)
					.setFailText("存在不合法的题目")
					.addAllInvalidQuestion(invalidQuestionList)
					.build());
		}
		
		Connection conn = null;
		List<Integer> questionIdList = null;
		try {
			conn = this.hikariDataSource.getConnection();
			
			questionIdList = SurveyDB.insertQuestion(conn, companyId, surveyId, now, createAdminId, questionList);
		} catch (SQLException ex) {
			throw new RuntimeException("db fail", ex);
		} finally {
			DBUtil.closeQuietly(conn);
		}
		
		Jedis jedis = jedisPool.getResource();
		try {
			SurveyCache.delQuestion(jedis, companyId, questionIdList);
		} finally {
			jedis.close();
		}
		
		return Futures.immediateFuture(ImportQuestionResponse.newBuilder()
				.setResult(ImportQuestionResponse.Result.SUCC)
				.build());
	}

	@Override
	public ListenableFuture<CopySurveyResponse> copySurvey(AdminHead head,
			CopySurveyRequest request) {
		if (!head.hasCompanyId()) {
			return Futures.immediateFuture(CopySurveyResponse.newBuilder()
					.setResult(CopySurveyResponse.Result.FAIL_UNKNOWN)
					.setFailText("company_id 参数未填")
					.build());
		}
		final long companyId = head.getCompanyId();
		final int surveyId = request.getSurveyId();
		
		SurveyProtos.Survey survey = SurveyUtil.getSurveyById(hikariDataSource, jedisPool, companyId, Collections.singleton(surveyId)).get(surveyId);
		if (survey == null) {
			return Futures.immediateFuture(CopySurveyResponse.newBuilder()
					.setResult(CopySurveyResponse.Result.FAIL_SURVEY_INVALID)
					.setFailText("要复制的调研不存在")
					.build());
		}
		
		String surveyName = request.getSurveyName();
		if (surveyName.length() > 191) {
			return Futures.immediateFuture(CopySurveyResponse.newBuilder()
					.setResult(CopySurveyResponse.Result.FAIL_SURVEY_NAME_INVALID)
					.setFailText("名称太长")
					.build());
		}
		
		final int startTime = request.getStartTime();
		final int endTime = request.getEndTime();
		if (endTime <= startTime) {
			return Futures.immediateFuture(CopySurveyResponse.newBuilder()
					.setResult(CopySurveyResponse.Result.FAIL_SURVEY_TIME_INVALID)
					.setFailText("时间不合法")
					.build());
		}

		final Integer allowModelId = request.hasAllowModelId() ? request.getAllowModelId() : null;
		
		final int now = (int) (System.currentTimeMillis() / 1000L);
		final long createAdminId = head.getSession().getAdminId();
		
		
		Connection conn = null;
		int newSurveyId = 0;
		try {
			conn = this.hikariDataSource.getConnection();
			
			List<Integer> questionIdList = SurveyDB.getQuestionIdBySurveyId(conn, companyId, surveyId);
			Map<Integer, SurveyProtos.Question> questionMap = SurveyUtil.getSurveyQuestionById(hikariDataSource, jedisPool, companyId, questionIdList);
			List<SurveyProtos.Question> questionList = new ArrayList<SurveyProtos.Question>();
			for (int questionId : questionIdList) {
				SurveyProtos.Question question = questionMap.get(questionId);
				if (question != null) {
					questionList.add(question);
				}
			}
			
			newSurveyId = SurveyDB.copySurvey(conn, companyId, surveyId, surveyName, startTime, endTime, allowModelId, now, createAdminId, questionList);
		} catch (SQLException ex) {
			throw new RuntimeException("db fail", ex);
		} finally {
			DBUtil.closeQuietly(conn);
		}
		
		Jedis jedis = jedisPool.getResource();
		try {
			SurveyCache.delSurvey(jedis, companyId, Collections.singleton(newSurveyId));
		} finally {
			jedis.close();
		}
		
		this.serviceExecutor.execute(new SendSecretaryMessageTask(head, surveyName, startTime, endTime, allowModelId));
		
		return Futures.immediateFuture(CopySurveyResponse.newBuilder()
				.setResult(CopySurveyResponse.Result.SUCC)
				.setSurveyId(newSurveyId)
				.build());
	}

	private final class SendSecretaryMessageTask implements Runnable {

		private final AdminHead adminHead;
		
		private final String surveyName;
		private final int startTime;
		private final int endTime;
		private final Integer allowModelId;
		
		public SendSecretaryMessageTask (AdminHead adminHead, String surveyName, int startTime, int endTime, Integer allowModelId) {
			this.adminHead = adminHead;
			this.surveyName = surveyName;
			this.startTime = startTime;
			this.endTime = endTime;
			this.allowModelId = allowModelId;
		}
		
		@Override
		public void run() {
			Set<Long> sendUserIdSet = Sets.newTreeSet();
			
			int start = 0;
			final int length = 500;
			while (true) {
				GetUserListResponse getUserListResponse = Futures.getUnchecked(adminUserService.getUserList(adminHead, GetUserListRequest.newBuilder()
						.setStart(start)
						.setLength(length)
						.build()));
				
				Set<Long> userIdSet = Sets.newTreeSet();
				for (UserProtos.User user : getUserListResponse.getUserList()) {
					userIdSet.add(user.getBase().getUserId());
				}
				
				if (allowModelId == null) {
					sendUserIdSet.addAll(userIdSet);
				} else {
					CheckAllowResponse checkAllowResponse = Futures.getUnchecked(allowService.checkAllow(adminHead, CheckAllowRequest.newBuilder()
							.addAllUserId(userIdSet)
							.addModelId(allowModelId)
							.build()));
					for (AllowProtos.CheckAllowResponse.CheckResult checkResult : checkAllowResponse.getCheckResultList()) {
						if (checkResult.getModelId() == allowModelId) {
							sendUserIdSet.addAll(checkResult.getAllowUserIdList());
						}
					}
				}
				
				start += length;
				if (start >= getUserListResponse.getFilteredSize()) {
					break;
				}
			}
			
			SimpleDateFormat df = new SimpleDateFormat("MM-dd HH:mm");
			
			ProfileManager.Profile profile = profileManager.getProfile(adminHead, "survey:");
			String template = profile.get(SURVEY_TEMPLATE).replace("${name}", surveyName)
					.replace("${start_time}", df.format(new Date(startTime * 1000L)))
					.replace("${end_time}", df.format(new Date(endTime * 1000L)));
			
			Iterator<Long> it = sendUserIdSet.iterator();
			while (it.hasNext()) {
				List<Long> list = Lists.newArrayList();
				while (list.size() < 1000 && it.hasNext()) {
					list.add(it.next());
				}
				
				adminOfficialService.sendSecretaryMessage(adminHead,
						AdminOfficialProtos.SendSecretaryMessageRequest.newBuilder()
								.addAllUserId(list)
								.setSendMsg(OfficialProtos.OfficialMessage.newBuilder()
										.setMsgSeq(0)
										.setMsgTime(0)
										.setIsFromUser(false)
										.setText(OfficialProtos.OfficialMessage.Text.newBuilder()
												.setContent(template))
										.build())
								.build());
				logger.info("小秘书调研提醒：" + template + ", 提醒人数：" + list.size());
			}
			
		}
		
	}
	
}
