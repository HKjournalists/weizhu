package com.weizhu.service.exam;

import java.sql.Connection;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executor;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.annotation.Nullable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Splitter;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.weizhu.common.db.DBUtil;
import com.weizhu.common.service.ServiceUtil;
import com.weizhu.common.utils.DataPage;
import com.weizhu.common.utils.ProfileManager;
import com.weizhu.proto.AdminExamProtos.CreateExamQuestionRandomRequest;
import com.weizhu.proto.AdminExamProtos.CreateExamQuestionRandomResponse;
import com.weizhu.proto.AdminExamProtos.CreateExamRequest;
import com.weizhu.proto.AdminExamProtos.CreateExamResponse;
import com.weizhu.proto.AdminExamProtos.CreateQuestionCategoryRequest;
import com.weizhu.proto.AdminExamProtos.CreateQuestionCategoryResponse;
import com.weizhu.proto.AdminExamProtos.CreateQuestionRequest;
import com.weizhu.proto.AdminExamProtos.CreateQuestionResponse;
import com.weizhu.proto.AdminExamProtos.DeleteExamRequest;
import com.weizhu.proto.AdminExamProtos.DeleteExamResponse;
import com.weizhu.proto.AdminExamProtos.DeleteQuestionCategoryRequest;
import com.weizhu.proto.AdminExamProtos.DeleteQuestionCategoryResponse;
import com.weizhu.proto.AdminExamProtos.DeleteQuestionRequest;
import com.weizhu.proto.AdminExamProtos.DeleteQuestionResponse;
import com.weizhu.proto.AdminExamProtos.GetExamByIdRequest;
import com.weizhu.proto.AdminExamProtos.GetExamByIdResponse;
import com.weizhu.proto.AdminExamProtos.GetExamQuestionRandomRequest;
import com.weizhu.proto.AdminExamProtos.GetExamQuestionRandomResponse;
import com.weizhu.proto.AdminExamProtos.GetExamQuestionRequest;
import com.weizhu.proto.AdminExamProtos.GetExamQuestionResponse;
import com.weizhu.proto.AdminExamProtos.GetExamRequest;
import com.weizhu.proto.AdminExamProtos.GetExamResponse;
import com.weizhu.proto.AdminExamProtos.GetExamStatisticsRequest;
import com.weizhu.proto.AdminExamProtos.GetExamStatisticsResponse;
import com.weizhu.proto.AdminExamProtos.GetExamStatisticsResponse.ExamStatistics;
import com.weizhu.proto.AdminExamProtos.GetExamUserResultRequest;
import com.weizhu.proto.AdminExamProtos.GetExamUserResultResponse;
import com.weizhu.proto.AdminExamProtos.GetPositionStatisticsRequest;
import com.weizhu.proto.AdminExamProtos.GetPositionStatisticsResponse;
import com.weizhu.proto.AdminExamProtos.GetPositionStatisticsResponse.PositionStatistics;
import com.weizhu.proto.AdminExamProtos.GetQuestionByCategoryIdRequest;
import com.weizhu.proto.AdminExamProtos.GetQuestionByCategoryIdResponse;
import com.weizhu.proto.AdminExamProtos.GetQuestionCategoryResponse;
import com.weizhu.proto.AdminExamProtos.GetQuestionCorrectRateRequest;
import com.weizhu.proto.AdminExamProtos.GetQuestionCorrectRateResponse;
import com.weizhu.proto.AdminExamProtos.GetQuestionCorrectRateResponse.QuestionCorrect;
import com.weizhu.proto.AdminExamProtos.GetQuestionRequest;
import com.weizhu.proto.AdminExamProtos.GetQuestionResponse;
import com.weizhu.proto.AdminExamProtos.GetTeamStatisticsRequest;
import com.weizhu.proto.AdminExamProtos.GetTeamStatisticsResponse;
import com.weizhu.proto.AdminExamProtos.GetTeamStatisticsResponse.TeamStatistics;
import com.weizhu.proto.AdminExamProtos.GetUserAnswerRequest;
import com.weizhu.proto.AdminExamProtos.GetUserAnswerResponse;
import com.weizhu.proto.AdminExamProtos.ImportQuestionRequest;
import com.weizhu.proto.AdminExamProtos.ImportQuestionResponse;
import com.weizhu.proto.AdminExamProtos.MoveQuestionCategoryRequest;
import com.weizhu.proto.AdminExamProtos.MoveQuestionCategoryResponse;
import com.weizhu.proto.AdminExamProtos.QuestionCategory;
import com.weizhu.proto.AdminExamProtos.ReExamRequest;
import com.weizhu.proto.AdminExamProtos.ReExamResponse;
import com.weizhu.proto.AdminExamProtos.UpdateExamQuestionRandomRequest;
import com.weizhu.proto.AdminExamProtos.UpdateExamQuestionRandomResponse;
import com.weizhu.proto.AdminExamProtos.UpdateExamQuestionRequest;
import com.weizhu.proto.AdminExamProtos.UpdateExamQuestionRequest.ExamQuestion;
import com.weizhu.proto.AdminExamProtos.UpdateExamQuestionResponse;
import com.weizhu.proto.AdminExamProtos.UpdateExamRequest;
import com.weizhu.proto.AdminExamProtos.UpdateExamResponse;
import com.weizhu.proto.AdminExamProtos.UpdateQuestionCategoryRequest;
import com.weizhu.proto.AdminExamProtos.UpdateQuestionCategoryResponse;
import com.weizhu.proto.AdminExamProtos.UpdateQuestionInQuestionCategoryRequest;
import com.weizhu.proto.AdminExamProtos.UpdateQuestionInQuestionCategoryResponse;
import com.weizhu.proto.AdminExamProtos.UpdateQuestionRequest;
import com.weizhu.proto.AdminExamProtos.UpdateQuestionResponse;
import com.weizhu.proto.AdminExamService;
import com.weizhu.proto.AdminOfficialProtos;
import com.weizhu.proto.AdminOfficialService;
import com.weizhu.proto.AdminProtos.AdminHead;
import com.weizhu.proto.AdminUserProtos.GetUserListRequest;
import com.weizhu.proto.AdminUserProtos.GetUserListResponse;
import com.weizhu.proto.AdminUserService;
import com.weizhu.proto.AllowProtos;
import com.weizhu.proto.AllowProtos.CheckAllowRequest;
import com.weizhu.proto.AllowProtos.CheckAllowResponse;
import com.weizhu.proto.AllowProtos.CreateModelRequest;
import com.weizhu.proto.AllowProtos.CreateModelResponse;
import com.weizhu.proto.AllowProtos.GetModelByIdRequest;
import com.weizhu.proto.AllowProtos.GetModelByIdResponse;
import com.weizhu.proto.AllowProtos.GetModelRuleListRequest;
import com.weizhu.proto.AllowService;
import com.weizhu.proto.ExamProtos;
import com.weizhu.proto.ExamProtos.Question;
import com.weizhu.proto.ExamProtos.UserResult;
import com.weizhu.proto.OfficialProtos;
import com.weizhu.proto.UserProtos;
import com.weizhu.proto.WeizhuProtos.EmptyRequest;
import com.weizhu.proto.WeizhuProtos.EmptyResponse;
import com.weizhu.proto.WeizhuProtos.SystemHead;
import com.zaxxer.hikari.HikariDataSource;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

public class AdminExamServiceImpl implements AdminExamService{

	private static final Logger logger = LoggerFactory.getLogger(ExamServiceImpl.class);
	
	private final HikariDataSource hikariDataSource;
	private final JedisPool jedisPool;
	private final Executor serviceExecutor;
	private final ScheduledExecutorService scheduledExecutorService;
	private final AllowService allowService;
	private final AdminUserService adminUserService;
	private final AdminOfficialService adminOfficialService;
	private final ProfileManager profileManager;
	
	private static final ProfileManager.ProfileKey<String> EXAM_TEMPLATE = 
			ProfileManager.createKey("exam:template", "亲爱的伙伴儿，考试中心新上传考试《${name}》，开始时间是：${start_time},结束时间：${end_time},请安排时间尽快完成！");
	
	private static final Splitter SPLITTER = Splitter.on(',').trimResults().omitEmptyStrings();
	
	@Inject
	public AdminExamServiceImpl(HikariDataSource hikariDataSource, JedisPool jedisPool,
			@Named("service_executor") Executor serviceExecutor,
			@Named("service_scheduled_executor") ScheduledExecutorService scheduledExecutorService,
			AllowService allowService, 
			AdminUserService adminUserService, AdminOfficialService adminOfficialService, ProfileManager profileManager) {
		this.hikariDataSource = hikariDataSource;
		this.jedisPool = jedisPool;
		this.serviceExecutor = serviceExecutor;
		this.scheduledExecutorService = scheduledExecutorService;
		this.allowService = allowService;
		this.adminUserService = adminUserService;
		this.adminOfficialService = adminOfficialService;
		this.profileManager = profileManager;
		
		this.doLoadExamSubmitTask();
	}
	
	@Override
	public ListenableFuture<GetQuestionResponse> getQuestion(AdminHead head,
			GetQuestionRequest request) {
		if (!head.hasCompanyId()) {
			return Futures.immediateFuture(GetQuestionResponse.newBuilder()
					.setTotal(0)
					.setFilterdSize(0)
					.build());
		}
		
		final long companyId = head.getCompanyId();
		
		final int start = request.getStart() < 0 ? 0 : request.getStart();
	    final int length = request.getLength() < 0 ? 0 : request.getLength() > 100 ? 100 : request.getLength();
	    final String questionName = request.hasQuestionName() ? request.getQuestionName() : "";
		if (length == 0) {
			return Futures.immediateFuture(GetQuestionResponse.newBuilder()
					.setTotal(0)
					.setFilterdSize(0)
					.build());
		}
		
		DataPage<Integer> questionIdPage = null;
		Map<Integer, ExamProtos.Question> questionMap = null;
		
		Connection conn = null;
		try {
			conn = hikariDataSource.getConnection();
			questionIdPage = ExamDB.getQuestionIdPage(conn, companyId, start, length, questionName);
		} catch (SQLException ex) {
			throw new RuntimeException("db fail", ex);
		} finally {
			DBUtil.closeQuietly(conn);
		}
		
		questionMap = ExamUtil.getQuestion(hikariDataSource, jedisPool, companyId, questionIdPage.dataList());
		
		GetQuestionResponse.Builder questionBuilder = GetQuestionResponse.newBuilder()
				.setTotal(questionIdPage.totalSize())
				.setFilterdSize(questionIdPage.filteredSize());
		
		for (int questionId : questionIdPage.dataList()) {
			if (questionMap.get(questionId) != null) {
				questionBuilder.addQuestion(questionMap.get(questionId));
			}
		}
		
		return Futures.immediateFuture(questionBuilder.build());
	}

	@Override
	public ListenableFuture<CreateQuestionResponse> createQuestion(AdminHead head,
			CreateQuestionRequest request) {
		final long companyId = head.getCompanyId();
		
		final String questionName = request.getQuestionName();
		if (questionName.length() > 191) {
			return Futures.immediateFuture(CreateQuestionResponse.newBuilder()
					.setResult(CreateQuestionResponse.Result.FAIL_QUESTION_NAME_INVALID)
					.setFailText("考题内容过长！")
					.build());
		}
		
		int rightAnswerCount = 0;
		
		List<ExamProtos.Option> optionList = request.getOptionList();
		for (ExamProtos.Option option : optionList) {
			if (option.getOptionName().length() > 191) {
				return Futures.immediateFuture(CreateQuestionResponse.newBuilder()
						.setResult(CreateQuestionResponse.Result.FAIL_QUESTION_NAME_INVALID)
						.setFailText("选项内容太长！选项：" + option.getOptionName())
						.build());
			}
			if (option.getIsRight()) {
				rightAnswerCount++;
			}
		}
		
		final ExamProtos.Question.Type questionType = request.getType();
		if (questionType.equals(ExamProtos.Question.Type.OPTION_SINGLE) && rightAnswerCount != 1) {
			return Futures.immediateFuture(CreateQuestionResponse.newBuilder()
					.setResult(CreateQuestionResponse.Result.FAIL_OPTION_INVALID)
					.setFailText("请保证单选题有一个正确选项！")
					.build());
		}
		if (questionType.equals(ExamProtos.Question.Type.OPTION_MULTI) && rightAnswerCount < 1) {
			return Futures.immediateFuture(CreateQuestionResponse.newBuilder()
					.setResult(CreateQuestionResponse.Result.FAIL_OPTION_INVALID)
					.setFailText("请保证多选题有两个或多个正确选项！")
					.build());
		}
		
		final int now = (int) (System.currentTimeMillis() / 1000L);
		final long adminId = head.getSession().getAdminId();
		int categoryId = request.getCategoryId();
		int questionId = 0;

		Connection conn = null;
		try {
			conn = this.hikariDataSource.getConnection();
			questionId = ExamDB.insertQuestion(conn, companyId, questionName, questionType, now, adminId, optionList, categoryId);
		} catch (SQLException ex) {
			throw new RuntimeException("db fail", ex);
		} finally {
			DBUtil.closeQuietly(conn);
		}
		
		Jedis jedis = jedisPool.getResource();
		try {
			ExamCache.delQuestion(jedis, companyId, Collections.singleton(questionId));
		} finally {
			jedis.close();
		}
		
		return Futures.immediateFuture(CreateQuestionResponse.newBuilder()
				.setResult(CreateQuestionResponse.Result.SUCC)
				.setQuestionId(questionId)
				.build());
	}

	@Override
	public ListenableFuture<UpdateQuestionResponse> updateQuestion(
			AdminHead head, UpdateQuestionRequest request) {
		final String questionName = request.getQuestionName();
		
		if (questionName.length() > 191) {
			return Futures.immediateFuture(UpdateQuestionResponse.newBuilder()
					.setResult(UpdateQuestionResponse.Result.FAIL_QUESTION_NAME_INVALID)
					.setFailText("您输入的考题太长！")
					.build());
		}
		
		int rightAnswerCount = 0;
		List<Integer> optionIdList = new ArrayList<Integer>();
		
		List<ExamProtos.Option> optionList = request.getOptionList();
		for (ExamProtos.Option option : optionList) {
			if (optionIdList.contains(option.getOptionId()) && option.getOptionId() != 0) {
				return Futures.immediateFuture(UpdateQuestionResponse.newBuilder()
						.setResult(UpdateQuestionResponse.Result.FAIL_OPTION_INVALID)
						.setFailText("存在相同的选项编号")
						.build());
			}
			
			optionIdList.add(option.getOptionId());
			
			if (option.getOptionName().length() > 191) {
				return Futures.immediateFuture(UpdateQuestionResponse.newBuilder()
						.setResult(UpdateQuestionResponse.Result.FAIL_QUESTION_NAME_INVALID)
						.setFailText("您输入的选项内容太长！选项：" + option.getOptionName())
						.build());
			}
			if (option.getIsRight()) {
				rightAnswerCount++;
			}
		}
		
		final ExamProtos.Question.Type questionType = request.getType();
		if (questionType.equals(ExamProtos.Question.Type.OPTION_SINGLE) && rightAnswerCount != 1) {
			return Futures.immediateFuture(UpdateQuestionResponse.newBuilder()
					.setResult(UpdateQuestionResponse.Result.FAIL_OPTION_INVALID)
					.setFailText("请保证单选题有一个正确选项！")
					.build());
		}
		if (questionType.equals(ExamProtos.Question.Type.OPTION_MULTI) && rightAnswerCount < 1) {
			return Futures.immediateFuture(UpdateQuestionResponse.newBuilder()
					.setResult(UpdateQuestionResponse.Result.FAIL_OPTION_INVALID)
					.setFailText("请保证多选题有两个或多个正确选项！")
					.build());
		}
		if (questionType.equals(ExamProtos.Question.Type.OPTION_TF) && rightAnswerCount != 1) {
			return Futures.immediateFuture(UpdateQuestionResponse.newBuilder()
					.setResult(UpdateQuestionResponse.Result.FAIL_OPTION_INVALID)
					.setFailText("请保证判断题有一个是对的！")
					.build());
		}
		
		final long companyId = head.getCompanyId();
		final int questionId = request.getQuestionId();
		
		Map<Integer, Question> oldQuestionMap = ExamUtil.getQuestion(hikariDataSource, jedisPool, companyId, Collections.singleton(questionId));
		if (oldQuestionMap.isEmpty()) {
			return Futures.immediateFuture(UpdateQuestionResponse.newBuilder()
					.setResult(UpdateQuestionResponse.Result.FAIL_QUESTION_NOT_EXIST)
					.setFailText("此项考题不存在！")
					.build());
		}
		
//	        比较选项：以前存在的就直接更新，optionId为0的添加，多余的删掉
		List<ExamProtos.Option> oldOptionList = oldQuestionMap.get(questionId).getOptionList();
		
		Set<ExamProtos.Option> addOptionSet = new HashSet<ExamProtos.Option>();
		Set<ExamProtos.Option> updateOptionSet = new HashSet<ExamProtos.Option>();
		Set<ExamProtos.Option> delOptionSet = new HashSet<ExamProtos.Option>(oldOptionList);
		
		for (ExamProtos.Option newOption : request.getOptionList()) {
			for (ExamProtos.Option oldOption : oldOptionList) {
				if (newOption.getOptionId() == oldOption.getOptionId()) {
					if ((newOption.getIsRight() == oldOption.getIsRight()) 
							&& (newOption.getOptionName().equals(oldOption.getOptionName()))) {
						delOptionSet.remove(oldOption);
						
						break ;
					} else {
						updateOptionSet.add(newOption);
						delOptionSet.remove(oldOption);
						
						break ;
					}
				} else if (newOption.getOptionId() == 0) {
					// 当optionId是0的时候是新增
					addOptionSet.add(newOption);
				}
			}
		}
		
		List<Integer> examIdList = null;
		Connection conn = null;
		if (delOptionSet.size() > 0) {
			try {
				conn = this.hikariDataSource.getConnection();
				examIdList = ExamDB.getExamIdByQuestionId(conn, companyId, Collections.singleton(questionId));
			} catch (SQLException ex) {
				throw new RuntimeException("db fail", ex);
			} finally {
				DBUtil.closeQuietly(conn);
			}
		}
		
		if (examIdList == null) {
			examIdList = Collections.emptyList();
		}
		
		Map<Integer, ExamDAOProtos.ExamInfo> examInfoMap = ExamUtil.getExamInfo(hikariDataSource, jedisPool, companyId, examIdList);
		
		int now = (int) (System.currentTimeMillis() / 1000L);
		
		for (ExamDAOProtos.ExamInfo examInfo : examInfoMap.values()) {
			if (examInfo.getExam().getStartTime() <= now && examInfo.getExam().getEndTime() > now) {
				return Futures.immediateFuture(UpdateQuestionResponse.newBuilder()
						.setResult(UpdateQuestionResponse.Result.FAIL_OPTION_INVALID)
						.setFailText("正在考试的考题不能删除选项！")
						.build());
			}
		}
		
		conn = null;
		try {
			conn = this.hikariDataSource.getConnection();
			
			ExamDB.addOption(conn, companyId, questionId, addOptionSet);
			ExamDB.updateOption(conn, companyId, updateOptionSet);
			ExamDB.deleteOption(conn, companyId, delOptionSet);
			
			ExamDB.updateQuestion(conn, companyId, questionId, questionName, questionType);
		} catch (SQLException ex) {
			throw new RuntimeException("db fail", ex);
		} finally {
			DBUtil.closeQuietly(conn);
		}
		
		Jedis jedis = jedisPool.getResource();
		try {
			ExamCache.delQuestion(jedis, companyId, Collections.singleton(questionId));
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
		final long companyId = head.getCompanyId();
		
		final List<Integer> questionIdList = request.getQuestionIdList();
		if (questionIdList.isEmpty()) {
			return Futures.immediateFuture(DeleteQuestionResponse.newBuilder()
					.setResult(DeleteQuestionResponse.Result.SUCC)
					.build());
		}
		
		List<Integer> examIdList;
		
		Connection conn = null;
		try {
			conn = this.hikariDataSource.getConnection();
			examIdList = ExamDB.getExamIdByQuestionId(conn, companyId, new HashSet<Integer>(questionIdList));
			if (examIdList.isEmpty()) {
				ExamDB.deleteQuestion(conn, companyId, questionIdList);
			}
		} catch (SQLException ex) {
			throw new RuntimeException("db fail", ex);
		} finally {
			DBUtil.closeQuietly(conn);
		}
		
		Map<Integer, ExamDAOProtos.ExamInfo> examInfoMap = ExamUtil.getExamInfo(hikariDataSource, jedisPool, companyId, examIdList);

		if (!examInfoMap.isEmpty()) {
			List<String> examNameList = new ArrayList<String>();
			for (int examId : examIdList) {
				if (examInfoMap.get(examId) != null) {
					examNameList.add(examInfoMap.get(examId).getExam().getExamName());
				}
			}
			
			return Futures.immediateFuture(DeleteQuestionResponse.newBuilder()
					.setResult(DeleteQuestionResponse.Result.FAIL_QUESTION_IN_USE)
					.setFailText("要删除的考题在" + DBUtil.COMMA_JOINER.join(examNameList) + "中，考题不能删除！")
					.build());
		}
		
		Jedis jedis = jedisPool.getResource();
		try {
			ExamCache.delQuestion(jedis, companyId, questionIdList);
		} finally {
			jedis.close();
		}
		
		return Futures.immediateFuture(DeleteQuestionResponse.newBuilder()
				.setResult(DeleteQuestionResponse.Result.SUCC)
				.build());
	}

	@Override
	public ListenableFuture<GetExamResponse> getExam(AdminHead head,
			GetExamRequest request) {
		final long companyId = head.getCompanyId();
		
		final int start = request.getStart() < 0 ? 0 : request.getStart();
		final int length = request.getLength() < 0 ? 0 : request.getLength() > 100 ? 100 : request.getLength();
		final String examName = request.hasExamName() ? request.getExamName() : "";
		final Integer state = request.hasState() ? request.getState() : null;
		
		if (length == 0) {
			return Futures.immediateFuture(GetExamResponse.newBuilder()
					.setTotal(0)
					.setFilteredSize(0)
					.build());
		}
		
		DataPage<Integer> examIdPage = null;
		final int now = (int) (System.currentTimeMillis() / 1000L);
		
		Connection conn = null;
		try {
			conn = this.hikariDataSource.getConnection();
			examIdPage = ExamDB.getExamIdByPage(conn, companyId, start, length, now, examName, state);
		} catch (SQLException ex) {
			throw new RuntimeException("db fail", ex);
		} finally {
			DBUtil.closeQuietly(conn);
		}
		
		Map<Integer, ExamDAOProtos.ExamInfo> examInfoMap = ExamUtil.getExamInfo(hikariDataSource, jedisPool, companyId, examIdPage.dataList());
		
		GetExamResponse.Builder examBuilder = GetExamResponse.newBuilder()
				.setTotal(examIdPage.totalSize())
				.setFilteredSize(examIdPage.filteredSize());
		
		for (Integer examId : examIdPage.dataList()) {
			if (examInfoMap.get(examId) != null) {
				examBuilder.addExam(examInfoMap.get(examId).getExam());
			}
		}

		return Futures.immediateFuture(examBuilder.build());
	}

	@Override
	public ListenableFuture<CreateExamResponse> createExam(
			AdminHead head, CreateExamRequest request) {
		final long companyId = head.getCompanyId();
		
		final String examName = request.getExamName();
		if (examName.length() > 191) {
			return Futures.immediateFuture(CreateExamResponse.newBuilder()
					.setResult(CreateExamResponse.Result.FAIL_EXAM_NAME_INVALID)
					.setFailText("考试名称过长！")
					.build());
		}
		
		final String imageName = request.hasImageName() ? request.getImageName() : null;
		if (imageName != null && imageName.length() > 191) {
			return Futures.immediateFuture(CreateExamResponse.newBuilder()
					.setResult(CreateExamResponse.Result.FAIL_EXAM_IMAGE_INVALID)
					.setFailText("图片名称过长！")
					.build());
		}
		
		final int startTime = request.getStartTime();
		final int endTime = request.getEndTime();
		
		if (startTime >= endTime) {
			return Futures.immediateFuture(CreateExamResponse.newBuilder()
					.setResult(CreateExamResponse.Result.FAIL_EXAM_TIME_INVALID)
					.setFailText("开始时间大于结束时间！")
					.build());
		}
		final int now = (int) (System.currentTimeMillis() / 1000L);
		// 时间范围在2000-01-01 00:00:00到2035-01-01 00:00:00
		if (startTime < 946656000 || endTime > 2051193600 || endTime <= now) {
			return Futures.immediateFuture(CreateExamResponse.newBuilder()
					.setResult(CreateExamResponse.Result.FAIL_EXAM_TIME_INVALID)
					.setFailText("请输入有效的时间！")
					.build());
		}
		
		final int passMark = request.getPassMark();
		if (passMark >100 || passMark < 0) {
			return Futures.immediateFuture(CreateExamResponse.newBuilder()
					.setResult(CreateExamResponse.Result.FAIL_PASS_MARK_INVALID)
					.setFailText("及格分数必须在0~100分之间！")
					.build());
		}
		
		final ExamProtos.Exam.Type type = request.hasType() ? request.getType() : ExamProtos.Exam.Type.MANUAL;
		
		final ExamProtos.ShowResult showResult = request.hasShowResult() ? request.getShowResult() : ExamProtos.ShowResult.AFTER_EXAM_END;
		
		final long adminId = head.getSession().getAdminId();
		final Integer allowModelId = request.hasAllowModelId() ? request.getAllowModelId() : null;
		int examId = 0;
		
		Connection conn = null;
		try {
			conn = this.hikariDataSource.getConnection();
			examId = ExamDB.insertExam(conn, companyId, examName, imageName, startTime, endTime, adminId, now, passMark, type, allowModelId, showResult);
		} catch (SQLException ex) {
			throw new RuntimeException("db fail", ex);
		} finally {
			DBUtil.closeQuietly(conn);
		}
		
		Jedis jedis = jedisPool.getResource();
		try {
			ExamCache.delExamInfo(jedis, companyId, Collections.singleton(examId));
		} finally {
			jedis.close();
		}
		
		this.serviceExecutor.execute(new SendSecretaryMessageTask(head, examName, startTime, endTime, allowModelId));

		return Futures.immediateFuture(CreateExamResponse.newBuilder()
				.setResult(CreateExamResponse.Result.SUCC)
				.setExamId(examId)
				.build());
	}

	@Override
	public ListenableFuture<UpdateExamResponse> updateExam(
			AdminHead head, UpdateExamRequest request) {
		final long companyId = head.getCompanyId();
		
		final String examName = request.getExamName();
		if (examName.length() > 191) {
			return Futures.immediateFuture(UpdateExamResponse.newBuilder()
					.setResult(UpdateExamResponse.Result.FAIL_EXAM_NAME_INVALID)
					.setFailText("考试名称过长！")
					.build());
		}
		
		final String imageName = request.hasImageName() ? request.getImageName() : null;
		if (imageName != null && imageName.length() > 191) {
			return Futures.immediateFuture(UpdateExamResponse.newBuilder()
					.setResult(UpdateExamResponse.Result.FAIL_EXAM_IMAGE_INVALID)
					.setFailText("图标名称过长！")
					.build());
		}
		
		final int startTime = request.getStartTime();
		final int endTime = request.getEndTime();
		
		if (startTime > endTime) {
			return Futures.immediateFuture(UpdateExamResponse.newBuilder()
					.setResult(UpdateExamResponse.Result.FAIL_EXAM_TIME_INVALID)
					.setFailText("开始时间大于结束时间！")
					.build());
		}
		// 时间范围在2000-01-01 00:00:00到2035-01-01 00:00:00
		if (startTime < 946656000 || endTime > 2051193600) {
			return Futures.immediateFuture(UpdateExamResponse.newBuilder()
					.setResult(UpdateExamResponse.Result.FAIL_EXAM_TIME_INVALID)
					.setFailText("请输入有效的时间！")
					.build());
		}
		
		final int examId = request.getExamId();
		ExamDAOProtos.ExamInfo examInfo = ExamUtil.getExamInfo(hikariDataSource, jedisPool, companyId, Collections.singletonList(examId)).get(examId);
		if (examInfo == null) {
			return Futures.immediateFuture(UpdateExamResponse.newBuilder()
					.setResult(UpdateExamResponse.Result.FAIL_EXAM_NOT_EXIST)
					.setFailText("此次考试不存在！")
					.build());
		}
		
		final int passMark = request.hasPassMark() ? request.getPassMark() : 0;
		
		final Integer allowModelId = request.hasAllowModelId() ? request.getAllowModelId() : null;
		
		final ExamProtos.ShowResult showResult = request.hasShowResult() ? request.getShowResult() : ExamProtos.ShowResult.AFTER_EXAM_END;
		
		Connection conn = null;
		try {
			conn = this.hikariDataSource.getConnection();
			ExamDB.updateExam(conn, companyId, examId, examName, imageName, startTime, endTime, passMark, allowModelId, showResult);
		} catch (SQLException ex) {
			throw new RuntimeException("db", ex);
		} finally {
			DBUtil.closeQuietly(conn);
		}
		
		Jedis jedis = jedisPool.getResource();
		try {
			ExamCache.delExamInfo(jedis, companyId, Collections.singleton(examId));
		} finally {
			jedis.close();
		}
		
		return Futures.immediateFuture(UpdateExamResponse.newBuilder()
				.setResult(UpdateExamResponse.Result.SUCC)
				.build());
	}

	@Override
	public ListenableFuture<DeleteExamResponse> deleteExam(
			AdminHead head, DeleteExamRequest request) {
		final long companyId = head.getCompanyId();
		
		final List<Integer> examIdList = request.getExamIdList();
		if (examIdList.isEmpty() ) {
			return Futures.immediateFuture(DeleteExamResponse.newBuilder()
					.setResult(DeleteExamResponse.Result.SUCC)
					.build());
		}
		
		Connection conn = null;
		try {
			conn = this.hikariDataSource.getConnection();
			ExamDB.deleteExam(conn, companyId, examIdList);
		} catch (SQLException ex) {
			throw new RuntimeException("db fail", ex);
		} finally {
			DBUtil.closeQuietly(conn);
		}
		
		Jedis jedis = jedisPool.getResource();
		try {
			ExamCache.delExamInfo(jedis, companyId, examIdList);
		} finally {
			jedis.close();
		}
		
		return Futures.immediateFuture(DeleteExamResponse.newBuilder()
				.setResult(DeleteExamResponse.Result.SUCC)
				.build());
	}

	@Override
	public ListenableFuture<GetExamQuestionResponse> getExamQuestion(
			AdminHead head, GetExamQuestionRequest request) {
		final long companyId = head.getCompanyId();
		final int examId = request.getExamId();
		
		ExamDAOProtos.ExamInfo examInfo = ExamUtil.getExamInfo(hikariDataSource, jedisPool, companyId, Collections.singleton(examId)).get(examId);
		if (examInfo == null) {
			return Futures.immediateFuture(GetExamQuestionResponse.newBuilder().build());
		}
		
		LinkedHashMap<Integer, Integer> questionScoreMap = new LinkedHashMap<Integer, Integer>();
		for (ExamDAOProtos.ExamQuestion examQuestion : examInfo.getExamQuestionList()) {
			questionScoreMap.put(examQuestion.getQuestionId(), examQuestion.getScore());
		}

		Map<Integer, ExamProtos.Question> questionMap = ExamUtil.getQuestion(hikariDataSource, jedisPool, companyId, questionScoreMap.keySet());
		
		ExamProtos.Question.Builder questionBuilder = ExamProtos.Question.newBuilder();
		
		GetExamQuestionResponse.Builder getExamQuestionBuilder = GetExamQuestionResponse.newBuilder();
		for (Entry<Integer, Integer> entry : questionScoreMap.entrySet()) {
			questionBuilder.clear();
			ExamProtos.Question question = questionMap.get(entry.getKey());
			if (question == null) {
				continue;
			}
			questionBuilder.mergeFrom(question)
				.setScore(entry.getValue());
			
			getExamQuestionBuilder.addQuestion(questionBuilder.build());
		}
		
		return Futures.immediateFuture(getExamQuestionBuilder.build());
	}

	@Override
	public ListenableFuture<GetExamQuestionRandomResponse> getExamQuestionRandom(AdminHead head,
			GetExamQuestionRandomRequest request) {
		final long companyId = head.getCompanyId();
		final int examId = request.getExamId();
		
		ExamDAOProtos.ExamInfo examInfo = ExamUtil.getExamInfo(hikariDataSource, jedisPool, companyId, Collections.singleton(examId)).get(examId);
		if (examInfo == null) {
			return Futures.immediateFuture(GetExamQuestionRandomResponse.newBuilder().build());
		}
		
		Map<Integer, QuestionCategory> categoryMap;
		Connection conn = null;
		try {
			conn = this.hikariDataSource.getConnection();
			categoryMap = ExamDB.getQuestionCategoryById(conn, companyId, examInfo.getRandomQuestionCategoryIdList());
		} catch (SQLException ex) {
 			throw new RuntimeException("db fail", ex);
		} finally {
			DBUtil.closeQuietly(conn);
		}
		
		return Futures.immediateFuture(GetExamQuestionRandomResponse.newBuilder()
				.setQuestionNum(examInfo.getRandomQuestionNum())
				.addAllQuestionCategory(categoryMap.values())
				.build());
	}

	@Override
	public ListenableFuture<UpdateExamQuestionResponse> updateExamQuestion(
			AdminHead head, UpdateExamQuestionRequest request) {
		final long companyId = head.getCompanyId();
		final int examId = request.getExamId();
		final List<ExamQuestion> examQuestionList = request.getExamQuestionList();
		
		int total = 0;
		List<Integer> questionIdList = Lists.newArrayList();
		LinkedHashMap<Integer, Integer> examQuestionMap = Maps.newLinkedHashMap();
		for (ExamQuestion examQuestion : examQuestionList) {
			int score = examQuestion.getScore();
			int questionId = examQuestion.getQuestionId();
			if (score > 100 || score <= 0) {
				return Futures.immediateFuture(UpdateExamQuestionResponse.newBuilder()
						.setResult(UpdateExamQuestionResponse.Result.FAIL_SCORE_INVALID)
						.setFailText("输入的分值不合法！")
						.build());
			}
			total += score;
			questionIdList.add(questionId);
			examQuestionMap.put(questionId, score);
		}
		if (total != 100) {
			return Futures.immediateFuture(UpdateExamQuestionResponse.newBuilder()
					.setResult(UpdateExamQuestionResponse.Result.FAIL_SCORE_INVALID)
					.setFailText("保证分值总和是100分！")
					.build());
		}
		
		Map<Integer, ExamProtos.Question> questionMap = ExamUtil.getQuestion(hikariDataSource, jedisPool, companyId, questionIdList);
		for (int questionId : questionIdList) {
			if (!questionMap.containsKey(questionId)) {
				return Futures.immediateFuture(UpdateExamQuestionResponse.newBuilder()
						.setResult(UpdateExamQuestionResponse.Result.FAIL_QUESTION_INVALID)
						.setFailText("考题不存在，编号：" + questionId)
						.build());
			}
		}
		
		ExamDAOProtos.ExamInfo examInfo = ExamUtil.getExamInfo(hikariDataSource, jedisPool, companyId, Collections.singleton(examId)).get(examId);
		if (examInfo == null) {
			return Futures.immediateFuture(UpdateExamQuestionResponse.newBuilder()
					.setResult(UpdateExamQuestionResponse.Result.FAIL_EXAM_INVALID)
					.setFailText("此次考试不存在！")
					.build());
		}
		
		Connection conn = null;
		try {
			conn = this.hikariDataSource.getConnection();
			ExamDB.updateExamQuestion(conn, companyId, examId, examQuestionMap);
		} catch (SQLException ex) {
			throw new RuntimeException("db fail", ex);
		} finally {
			DBUtil.closeQuietly(conn);
		}
		
		Jedis jedis = jedisPool.getResource();
		try {
			ExamCache.delExamInfo(jedis, companyId, Collections.singleton(examId));
		} finally {
			jedis.close();
		}

		return Futures.immediateFuture(UpdateExamQuestionResponse.newBuilder()
				.setResult(UpdateExamQuestionResponse.Result.SUCC)
				.build());
	}
	
	@Override
	public ListenableFuture<UpdateExamQuestionRandomResponse> updateExamQuestionRandom(AdminHead head,
			UpdateExamQuestionRandomRequest request) {
		final long companyId = head.getCompanyId();
		final int examId = request.getExamId();
		
		ExamDAOProtos.ExamInfo examInfo = ExamUtil.getExamInfo(hikariDataSource, jedisPool, companyId, Collections.singleton(examId)).get(examId);
		if (examInfo == null) {
			return Futures.immediateFuture(UpdateExamQuestionRandomResponse.newBuilder()
					.setResult(UpdateExamQuestionRandomResponse.Result.FAIL_EXAM_INVALID)
					.setFailText("此次考试不存在！")
					.build());
		}
		
		final int questionNum = request.getQuestionNum();
		if (questionNum < 0 || questionNum > 100) {
			return Futures.immediateFuture(UpdateExamQuestionRandomResponse.newBuilder()
					.setResult(UpdateExamQuestionRandomResponse.Result.FAIL_QUESTION_NUM_INVALID)
					.setFailText("考题数量不正确！")
					.build());
		}
		
		final List<Integer> questionCategoryIdList = request.getCategoryIdList();
		
		Connection conn = null;
		try {
			conn = this.hikariDataSource.getConnection();
			int count = ExamDB.getQuestionCount(conn, companyId, questionCategoryIdList);
			if (count < questionNum) {
				return Futures.immediateFuture(UpdateExamQuestionRandomResponse.newBuilder()
						.setResult(UpdateExamQuestionRandomResponse.Result.FAIL_QUESTION_NUM_INVALID)
						.setFailText("考题数量不正确！")
						.build());
			}
			
			Map<Integer, QuestionCategory> questionCategoryMap = ExamDB.getQuestionCategoryById(conn, companyId, questionCategoryIdList);
			ExamDB.UpdateExamCategory(conn, companyId, examId, questionCategoryMap.keySet(), questionNum);
		} catch (SQLException ex) {
			throw new RuntimeException("db fail", ex);
		} finally {
			DBUtil.closeQuietly(conn);
		}
		
		Jedis jedis = jedisPool.getResource();
		try {
			ExamCache.delExamInfo(jedis, companyId, Collections.singleton(examId));
		} finally {
			jedis.close();
		}
		
		return Futures.immediateFuture(UpdateExamQuestionRandomResponse.newBuilder()
				.setResult(UpdateExamQuestionRandomResponse.Result.SUCC)
				.build());
	}

	@Override
	public ListenableFuture<GetExamUserResultResponse> getExamUserResult(
			AdminHead head, GetExamUserResultRequest request) {
		final long companyId = head.getCompanyId();
		
		final int examId = request.getExamId();
		final int start = request.getStart() < 0 ? 0 : request.getStart();
		final int length = request.getLength() < 0 ? 0 : request.getLength();
		
		Connection conn = null;
		try {
			conn = this.hikariDataSource.getConnection();
			DataPage<UserResult> userResultPage = ExamDB.getUserResultPage(conn, companyId, examId, start, length);
			
			return Futures.immediateFuture(GetExamUserResultResponse.newBuilder()
					.setTotal(userResultPage.totalSize())
					.setFilteredSize(userResultPage.filteredSize())
					.addAllUserReuslt(userResultPage.dataList())
					.build());
		} catch (SQLException ex) {
			throw new RuntimeException("db fail", ex);
		} finally {
			DBUtil.closeQuietly(conn);
		}
	}

	@Override
	public ListenableFuture<GetExamByIdResponse> getExamById(AdminHead head,
			GetExamByIdRequest request) {
		final long companyId = head.getCompanyId();
		final List<Integer> examIdList = request.getExamIdList();
		
		Map<Integer, ExamDAOProtos.ExamInfo> examInfoMap = ExamUtil.getExamInfo(hikariDataSource, jedisPool, companyId, examIdList);
		
		GetExamByIdResponse.Builder responseBuilder = GetExamByIdResponse.newBuilder();
		for (ExamDAOProtos.ExamInfo examInfo : examInfoMap.values()) {
			responseBuilder.addExam(examInfo.getExam());
		}
		return Futures.immediateFuture(responseBuilder.build());
	}

	@Override
	public ListenableFuture<CreateQuestionCategoryResponse> createQuestionCategory(
			AdminHead head, CreateQuestionCategoryRequest request) {
		final long companyId = head.getCompanyId();
		
		final String categoryName = request.getCategoryName();
		if (categoryName.length() > 191) {
			return Futures.immediateFuture(CreateQuestionCategoryResponse.newBuilder()
					.setResult(CreateQuestionCategoryResponse.Result.FAIL_NAME_INVALID)
					.setFailText("您输入的题库名称太长！")
					.build());
		}
		
		final Integer parentCategoryId = request.hasParentCategoryId() ? request.getParentCategoryId() : null;
		
		final int now = (int) (System.currentTimeMillis() / 1000L);
		final long adminId = head.getSession().getAdminId();
		
		Connection conn = null;
		try {
			conn = hikariDataSource.getConnection();
			
			if (ExamDB.getQuestionCategoryByName(conn, companyId, categoryName)) {
				return Futures.immediateFuture(CreateQuestionCategoryResponse.newBuilder()
						.setResult(CreateQuestionCategoryResponse.Result.FAIL_NAME_INVALID)
						.setFailText("题库中已经存在此名称！")
						.build());
			}
			
			int categoryId = ExamDB.insertQuestionCategory(conn, companyId, categoryName, parentCategoryId, now, adminId);
			
			return Futures.immediateFuture(CreateQuestionCategoryResponse.newBuilder()
					.setCategoryId(categoryId)
					.setResult(CreateQuestionCategoryResponse.Result.SUCC)
					.build());
		} catch (SQLException ex) {
			throw new RuntimeException("db fail", ex);
		} finally {
			DBUtil.closeQuietly(conn);
		}
	}

	@Override
	public ListenableFuture<GetQuestionByCategoryIdResponse> getQuestionByCategoryId(
			AdminHead head, GetQuestionByCategoryIdRequest request) {
		final long companyId = head.getCompanyId();

		final int start = request.getStart() < 0 ? 0 : request.getStart();
		final int length = request.getLength() < 0 ? 0 :request.getLength() > 100 ? 100 : request.getLength();
		final int categoryId = request.getCategoryId();
		
		Connection conn = null;
		List<Integer> questionIdList = null;
		List<Integer> totalQuestionIdList = null;
		try {
			conn = hikariDataSource.getConnection();
			questionIdList = ExamDB.getQuestionIdByCategory(conn, companyId, start, length, Collections.singleton(categoryId)).get(categoryId);
			totalQuestionIdList = ExamDB.getQuestionIdByCategory(conn, companyId, start, -1, Collections.singleton(categoryId)).get(categoryId);
		} catch (SQLException ex) {
			throw new RuntimeException("db fail", ex);
		} finally {
			DBUtil.closeQuietly(conn);
		}
		
		if (questionIdList == null) {
			questionIdList = Collections.emptyList();
		}
		
		Map<Integer, ExamProtos.Question> questionMap = ExamUtil.getQuestion(hikariDataSource, jedisPool, companyId, questionIdList);
		List<ExamProtos.Question> questionList = new ArrayList<ExamProtos.Question>();
		for (int questionId : questionIdList) {
			if (questionMap.get(questionId) != null) {
				questionList.add(questionMap.get(questionId));
			}
		}
		
		if (totalQuestionIdList == null) {
			totalQuestionIdList = Collections.emptyList();
		}
		
		int total = totalQuestionIdList.size();
		return Futures.immediateFuture(GetQuestionByCategoryIdResponse.newBuilder()
				.addAllQuestion(questionList)
				.setTotal(total)
				.setFilteredSize(total)
				.build());
	}

	@Override
	public ListenableFuture<GetQuestionCategoryResponse> getQuestionCategory(
			AdminHead head, EmptyRequest request) {
		final long companyId = head.getCompanyId();
		
		Connection conn = null;
		try {
			conn = hikariDataSource.getConnection();
			
			Map<Integer, QuestionCategory> questionCategoryRoot = ExamDB.getQuestionCategoryRoot(conn, companyId);
			Map<Integer, List<QuestionCategory>> questionCategoryMap = ExamDB.getQuestionCategory(conn, companyId);
			
			// 根据根节点拼装其它叶子节点，避免环形
			Map<Integer, QuestionCategory> questionCategory = new LinkedHashMap<Integer, QuestionCategory>();
			for (Entry<Integer, QuestionCategory> entry : questionCategoryRoot.entrySet()) {
				int questionCategoryId = entry.getKey();
				List<QuestionCategory> questionCategoryList = createCategoryList(questionCategoryId, questionCategoryMap);
				
				QuestionCategory.Builder questionCategoryBuilder = QuestionCategory.newBuilder()
						.mergeFrom(entry.getValue());
				if (questionCategoryList != null) {
					questionCategoryBuilder.addAllQuestionCategory(questionCategoryList);
				}
				
				questionCategory.put(questionCategoryId, questionCategoryBuilder.build());
			}
			
			return Futures.immediateFuture(GetQuestionCategoryResponse.newBuilder()
					.addAllQuestionCategory(questionCategory.values())
					.build());
			
		} catch (SQLException ex) {
			throw new RuntimeException("db fail", ex);
		} finally {
			DBUtil.closeQuietly(conn);
		}
	}
	
	// 获取此节点和此节点下面所有节点的题库列表
	// 题库指定已经指定父节点，都是单向的，所以不可能出现回路，不用排除重复一个key的递归
	private List<QuestionCategory> createCategoryList(int key, Map<Integer, List<QuestionCategory>> questionCategoryMap) {
		List<QuestionCategory> categoryList = new ArrayList<QuestionCategory>();
		List<QuestionCategory> tmpCategoryList = questionCategoryMap.get(key); // key节点下的题库列表
		if (tmpCategoryList != null) {
			categoryList.addAll(tmpCategoryList);
		} else {
			tmpCategoryList = Collections.emptyList();
		}
		
		if (categoryList != null) {
			for (QuestionCategory questionCategory : tmpCategoryList) {
				List<QuestionCategory> questionCategoryList = createCategoryList(questionCategory.getCategoryId(), questionCategoryMap); // 递归此节点
				QuestionCategory.Builder categoryBuilder = QuestionCategory.newBuilder()
						.mergeFrom(questionCategory);
				if (!questionCategoryList.isEmpty()) {
					categoryBuilder.addAllQuestionCategory(questionCategoryList)
							.build();
				}
				categoryList.remove(questionCategory);
				categoryList.add(categoryBuilder.build()); 
			}
		}
		return categoryList == null ? Collections.<QuestionCategory>emptyList() : categoryList;
	}

	@Override
	public ListenableFuture<UpdateQuestionCategoryResponse> updateQuestionCategory(
			AdminHead head, UpdateQuestionCategoryRequest request) {
		final long companyId = head.getCompanyId();
		
		final String categoryName = request.getCategoryName();
		final int categoryId = request.getCategoryId();
		
		if (categoryName.length() > 191) {
			return Futures.immediateFuture(UpdateQuestionCategoryResponse.newBuilder()
					.setResult(UpdateQuestionCategoryResponse.Result.FAIL_QUESTION_CATEGORY_NAME_INVALID)
					.setFailText("您输入的分类名称过长")
					.build());
		}
		
		Connection conn = null;
		try {
			conn = hikariDataSource.getConnection();
			
			Map<Integer, QuestionCategory> categoryMap = ExamDB.getQuestionCategoryById(conn, companyId, Collections.singleton(categoryId));
			if (categoryMap.get(categoryId) == null) {
				return Futures.immediateFuture(UpdateQuestionCategoryResponse.newBuilder()
						.setResult(UpdateQuestionCategoryResponse.Result.FAIL_QUESTION_CATEGORY_NOT_EXIST)
						.setFailText("原始考题分类不存在！")
						.build());
			}
			
			if (ExamDB.getQuestionCategoryByName(conn, companyId, categoryName)) {
				return Futures.immediateFuture(UpdateQuestionCategoryResponse.newBuilder()
						.setResult(UpdateQuestionCategoryResponse.Result.FAIL_QUESTION_CATEGORY_NAME_INVALID)
						.setFailText("题库中已经存在此名称！")
						.build());
			}
			
			ExamDB.updateQuestionCategory(conn, companyId, categoryName, categoryId);
			
			return Futures.immediateFuture(UpdateQuestionCategoryResponse.newBuilder()
					.setResult(UpdateQuestionCategoryResponse.Result.SUCC)
					.build());
		} catch (SQLException ex) {
			throw new RuntimeException("db fail", ex);
		} finally {
			DBUtil.closeQuietly(conn);
		}
	}

	@Override
	public ListenableFuture<MoveQuestionCategoryResponse> moveQuestionCategoryResponse(
			AdminHead head, MoveQuestionCategoryRequest request) {
		final long companyId = head.getCompanyId();
		
		final Integer categoryId = request.getCategoryId();
		final Integer parentCategoryId = request.hasParentCategoryId() ? request.getParentCategoryId() : null;
		List<Integer> categoryIdList = new ArrayList<Integer>();
		if (parentCategoryId != null) {
			categoryIdList.add(parentCategoryId);
		}
		categoryIdList.add(categoryId);
		
		Connection conn = null;
		try {
			conn = this.hikariDataSource.getConnection();
			
			Map<Integer, QuestionCategory> categoryMap = ExamDB.getQuestionCategoryById(conn, companyId, categoryIdList);
			if (!categoryMap.keySet().contains(categoryId)) {
				return Futures.immediateFuture(MoveQuestionCategoryResponse.newBuilder()
					.setResult(MoveQuestionCategoryResponse.Result.FAIL_QUESTION_CATEGORY_NOT_EXIST)
					.setFailText("要移动的节点不存在")
					.build());
			} 
			if (parentCategoryId != null && !categoryMap.keySet().contains(parentCategoryId)) {
				return Futures.immediateFuture(MoveQuestionCategoryResponse.newBuilder()
						.setResult(MoveQuestionCategoryResponse.Result.FAIL_QUESTION_CATEGORY_NOT_EXIST)
						.setFailText("目的节点不存在")
						.build());
			}
			
			ExamDB.updateCategoryBelongs(conn, companyId, categoryId, parentCategoryId);
		} catch (SQLException ex) {
			throw new RuntimeException("db fail", ex);
		} finally {
			DBUtil.closeQuietly(conn);
		}
		
		return Futures.immediateFuture(MoveQuestionCategoryResponse.newBuilder()
						.setResult(MoveQuestionCategoryResponse.Result.SUCC)
						.build());
	}

	@Override
	public ListenableFuture<UpdateQuestionInQuestionCategoryResponse> updateQuestionInQuestionCategory(
			AdminHead head, UpdateQuestionInQuestionCategoryRequest request) {
		final List<Integer> questionIdList = request.getQuestionIdList();
		if (questionIdList.isEmpty()) {
			return Futures.immediateFuture(UpdateQuestionInQuestionCategoryResponse.newBuilder()
					.setResult(UpdateQuestionInQuestionCategoryResponse.Result.FAIL_QUESTION_NOT_EXIST)
					.setFailText("您输入的问题不存在！")
					.build());
		}
		
		final long companyId = head.getCompanyId();
		
		final int oldCategoryId = request.getOldCategoryId();
		final int newCategoryId = request.getNewCategoryId();
		List<Integer> categoryIdList = Arrays.asList(oldCategoryId, newCategoryId);
		
		Connection conn = null;
		try {
			conn = hikariDataSource.getConnection();
			
			Map<Integer, QuestionCategory> categoryMap = ExamDB.getQuestionCategoryById(conn, companyId, categoryIdList);
			if (categoryMap.get(oldCategoryId) == null) {
				return Futures.immediateFuture(UpdateQuestionInQuestionCategoryResponse.newBuilder()
						.setResult(UpdateQuestionInQuestionCategoryResponse.Result.FAIL_QUESTION_CATEGORY_NOT_EXIST)
						.setFailText("原始考题分类不存在！")
						.build());
			}
			
			if (categoryMap.get(newCategoryId) == null) {
				return Futures.immediateFuture(UpdateQuestionInQuestionCategoryResponse.newBuilder()
						.setResult(UpdateQuestionInQuestionCategoryResponse.Result.FAIL_QUESTION_CATEGORY_NOT_EXIST)
						.setFailText("被更新的考题分类类不存在！")
						.build());
			}
			
			List<Integer> allQuestionId = ExamDB.getQuestionIdByCategory(conn, companyId, 0, -1, Collections.singleton(oldCategoryId)).get(oldCategoryId);// 当length是-1的时候表示查询全部
			for (Integer questionId : questionIdList) {
				if (allQuestionId == null || !allQuestionId.contains(questionId)) {
					return Futures.immediateFuture(UpdateQuestionInQuestionCategoryResponse.newBuilder()
							.setResult(UpdateQuestionInQuestionCategoryResponse.Result.FAIL_QUESTION_NOT_EXIST)
							.setFailText("存在考题不在原始分类中的考题")
							.build());
				}
			}
			
			ExamDB.updateQuestionInQuestionCategory(conn, companyId, oldCategoryId, newCategoryId, questionIdList);
			
			return Futures.immediateFuture(UpdateQuestionInQuestionCategoryResponse.newBuilder()
							.setResult(UpdateQuestionInQuestionCategoryResponse.Result.SUCC)
							.build());
		} catch (SQLException ex) {
			throw new RuntimeException("db fail", ex);
		} finally {
			DBUtil.closeQuietly(conn);
		}
	}

	@Override
	public ListenableFuture<ReExamResponse> reExam(AdminHead head,
			ReExamRequest request) {
		final long companyId = head.getCompanyId();
		
		final String examName = request.getExamName();
		if (examName.length() > 191) {
			return Futures.immediateFuture(ReExamResponse.newBuilder()
					.setResult(ReExamResponse.Result.FAIL_EXAM_NAME_INVAILD)
					.setFailText("您输入的名称过长！请重新输入。")
					.build());
		}
		
		final String imageName = request.hasImageName() ? request.getImageName() : null;
		if (imageName != null && imageName.length() > 191) {
			return Futures.immediateFuture(ReExamResponse.newBuilder()
					.setResult(ReExamResponse.Result.FAIL_EXAM_IMAGE_INVALID)
					.setFailText("图标名称过长！")
					.build());
		}
		
		final int startTime = request.getStartTime();
		final int endTime = request.getEndTime();
		if (startTime >= endTime) {
			return Futures.immediateFuture(ReExamResponse.newBuilder()
					.setResult(ReExamResponse.Result.FAIL_EXAM_TIME_INVAILD)
					.setFailText("开始时间大于结束时间！")
					.build());
		}
		
		final int examId = request.getExamId();
		ExamDAOProtos.ExamInfo examInfo = ExamUtil.getExamInfo(hikariDataSource, jedisPool, companyId, Collections.singleton(examId)).get(examId);
		if (examInfo == null) {
			return Futures.immediateFuture(ReExamResponse.newBuilder()
					.setResult(ReExamResponse.Result.FAIL_EXAM_NOT_EXIST)
					.setFailText("此次考试不存在！")
					.build());
		}
		
		List<UserResult> userResultList = new ArrayList<UserResult>();
		List<Long> passExamUserIdList = new ArrayList<Long>(); // 获取所有通过考试的userId
		Connection conn = null;
		try {
			conn = hikariDataSource.getConnection();
			userResultList = ExamDB.getAllUserResult(conn, companyId, examId); // 获取所有考试结果的用户
			
			for (UserResult userResult : userResultList) {
				if (userResult.getScore() >= examInfo.getExam().getPassMark()) {
					passExamUserIdList.add(userResult.getUserId());
				}
			}
		} catch (SQLException ex) {
			throw new RuntimeException("db fail", ex);
		} finally {
			DBUtil.closeQuietly(conn);
		}
		
		final List<AllowProtos.Rule> oldRuleList;
		final AllowProtos.Action oldDefaultAction;

		if (examInfo.getExam().hasAllowModelId()) {
			int allowModelId = examInfo.getExam().getAllowModelId();
			GetModelByIdRequest getModelByIdRequest = GetModelByIdRequest.newBuilder()
					.addAllModelId(Collections.singleton(allowModelId))
					.build();
			GetModelByIdResponse getModelByIdResponse = Futures.getUnchecked(allowService.getModelById(head, getModelByIdRequest));
			Map<Integer, AllowProtos.Model> modelMap = new HashMap<Integer, AllowProtos.Model>();
			for (AllowProtos.Model model : getModelByIdResponse.getModelList()) {
				modelMap.put(model.getModelId(), model); 
			}
			
			AllowProtos.Model allowModel = modelMap.get(allowModelId);
			if (allowModel == null) {
				return Futures.immediateFuture(ReExamResponse.newBuilder()
						.setResult(ReExamResponse.Result.FAIL_EXAM_NOT_EXIST)
						.setFailText("此次考试没有人员参加过考试")
						.build());
			}
			
			oldRuleList = Futures.getUnchecked(
					allowService.getModelRuleList(head, 
							GetModelRuleListRequest.newBuilder()
							.setModelId(allowModelId)
							.build())).getRuleList();
			
			oldDefaultAction = allowModel.getDefaultAction();
		} else {
			oldRuleList = Collections.emptyList();
			oldDefaultAction = AllowProtos.Action.ALLOW;
		}
		
		List<AllowProtos.Rule> ruleList = new ArrayList<AllowProtos.Rule>();
		
		int total = passExamUserIdList.size();
		for (int i=0; i <= (total-1)/100 && total != 0; i++) {
			int length = total - (i+1)*100 > 0 ? 100 : total - i*100;
			AllowProtos.UserRule userRule = AllowProtos.UserRule.newBuilder()
					.addAllUserId(passExamUserIdList.subList(i*100, i*100 + length))
					.build();

			ruleList.add(AllowProtos.Rule.newBuilder()
									.setRuleId(0)
									.setRuleName("考试已通过人员")
									.setAction(AllowProtos.Action.DENY)
									.setUserRule(userRule)
								.build());
		}
		
		CreateModelRequest createModelRequest = CreateModelRequest.newBuilder()
				.setModelName(examInfo.getExam().getExamName() + "（补考）访问模型")
				.setDefaultAction(oldDefaultAction)
				.addAllRule(ruleList)
				.addAllRule(oldRuleList)
				.build();
		CreateModelResponse createModelResponse = Futures.getUnchecked(allowService.createModel(head, createModelRequest));
		if (!createModelResponse.getResult().equals(CreateModelResponse.Result.SUCC)) {
			return Futures.immediateFuture(ReExamResponse.newBuilder()
					.setResult(ReExamResponse.Result.FAIL_EXAM_NOT_EXIST)
					.setFailText(createModelResponse.getFailText())
					.build());
		}
		
		final ExamProtos.Exam.Type type = examInfo.getExam().hasType() ? examInfo.getExam().getType() : ExamProtos.Exam.Type.MANUAL;
		
		Integer newAllowModelId = createModelResponse.hasModelId() ? createModelResponse.getModelId() : null;
		// 保存补考信息
		long adminId = head.getSession().getAdminId();
		int now = (int) (System.currentTimeMillis() / 1000L);
		int reExamId = 0;
		
		conn = null;
		try {
			conn = hikariDataSource.getConnection();
			reExamId = ExamDB.insertExam(conn, companyId, examName, imageName, startTime, endTime, adminId, now, examInfo.getExam().getPassMark(), type, newAllowModelId, examInfo.getExam().getShowResult());
			ExamDB.updateReExamQuestion(conn, companyId, examId, reExamId, type);
		} catch (SQLException ex) {
			throw new RuntimeException("db fail", ex);
		} finally {
			DBUtil.closeQuietly(conn);
		}

		this.serviceExecutor.execute(new SendSecretaryMessageTask(head, examName, startTime, endTime, newAllowModelId));
		
		return Futures.immediateFuture(ReExamResponse.newBuilder()
				.setResult(ReExamResponse.Result.SUCC)
				.setExamId(reExamId)
				.build());
	}

	@Override
	public ListenableFuture<CreateExamQuestionRandomResponse> createExamQuestionRandom(
			AdminHead head, CreateExamQuestionRandomRequest request) {
		final long companyId = head.getCompanyId();
		
		final List<Integer> questionCategoryIdList = request.getQuestionCategoryIdList();
		if (questionCategoryIdList.isEmpty()) {
			return Futures.immediateFuture(CreateExamQuestionRandomResponse.newBuilder()
					.setResult(CreateExamQuestionRandomResponse.Result.FAIL_QUESTION_CATEGORY_INVALID)
					.setFailText("请选一个题库名称！")
					.build());
		}
		
		final Integer score = request.getScore();
		List<Integer> questionIdList;
		
		Connection conn = null;
		try {
			conn = hikariDataSource.getConnection();
			
			Map<Integer, QuestionCategory> questionCategoryMap = ExamDB.getQuestionCategoryById(conn, companyId, questionCategoryIdList);
			for (int questionCategoryId : questionCategoryIdList) {
				if (!questionCategoryMap.keySet().contains(questionCategoryId)) {
					return Futures.immediateFuture(CreateExamQuestionRandomResponse.newBuilder()
							.setResult(CreateExamQuestionRandomResponse.Result.FAIL_QUESTION_CATEGORY_INVALID)
							.setFailText("存在不存在的题库！")
							.build());
				}
			}
			
			int questionCount = 100 / score;
			questionIdList = ExamDB.createExamQuestionRandom(conn, companyId, questionCount, questionCategoryIdList);
		} catch (SQLException ex) {
			throw new RuntimeException("db fail ", ex);
		} finally {
			DBUtil.closeQuietly(conn);
		}
		
		Map<Integer, Question> questionMap = ExamUtil.getQuestion(hikariDataSource, jedisPool, companyId, questionIdList);
		
		return Futures.immediateFuture(CreateExamQuestionRandomResponse.newBuilder()
				.setResult(CreateExamQuestionRandomResponse.Result.SUCC)
				.addAllQuestion(questionMap.values())
				.build());
	}

	@Override
	public ListenableFuture<ImportQuestionResponse> importQuestion(AdminHead head, ImportQuestionRequest request) {
		final long companyId = head.getCompanyId();
		
		final int questionCategoryId = request.getQuestionCategoryId();
		
		Set<ExamProtos.Question> questionSet = new HashSet<ExamProtos.Question>(request.getQuestionList());
		
		List<ImportQuestionResponse.InvalidQuestion> invalidQuestionList = new ArrayList<ImportQuestionResponse.InvalidQuestion>();
		ImportQuestionResponse.InvalidQuestion.Builder invalidQuestionBuilder = ImportQuestionResponse.InvalidQuestion.newBuilder();
		List<ExamProtos.Question> tmpQuestionList = new ArrayList<ExamProtos.Question>(request.getQuestionList());
		// 校验传参合法性
		for (Question question : questionSet) {
			invalidQuestionBuilder.clear();
			String questionName = question.getQuestionName();
			if (questionName.length() > 191) {
				invalidQuestionBuilder.setQuestionName(question.getQuestionName()).setFailText("题目内容太长！");
				tmpQuestionList.removeAll(Collections.singleton(question));
				invalidQuestionList.add(invalidQuestionBuilder.build());
				continue ;
			}
			
			int rightAnswerCount = 0;
			
			List<ExamProtos.Option> optionList = question.getOptionList();
			for (ExamProtos.Option option : optionList) {
				if (option.getOptionName().length() > 191) {
					invalidQuestionBuilder.setQuestionName(question.getQuestionName()).setFailText("选项内容太长！");
					tmpQuestionList.removeAll(Collections.singleton(question));
					invalidQuestionList.add(invalidQuestionBuilder.build());
					break ;
				}
				if (option.getIsRight()) {
					rightAnswerCount++;
				}
			}
			
			if (tmpQuestionList.contains(question)) {
				ExamProtos.Question.Type questionType = question.getType();
				if (questionType.equals(ExamProtos.Question.Type.OPTION_SINGLE) && rightAnswerCount != 1) {
					invalidQuestionBuilder.setQuestionName(question.getQuestionName()).setFailText("请保证单选题有一个正确选项，确保考题类型和正确答案个数一致");
					tmpQuestionList.removeAll(Collections.singleton(question));
					invalidQuestionList.add(invalidQuestionBuilder.build());
				} else if (questionType.equals(ExamProtos.Question.Type.OPTION_MULTI) && rightAnswerCount < 2) {
					invalidQuestionBuilder.setQuestionName(question.getQuestionName()).setFailText("请保证多选题有两个或多个正确选项，确保考题类型和正确答案个数一致");
					tmpQuestionList.removeAll(Collections.singleton(question));
					invalidQuestionList.add(invalidQuestionBuilder.build());
				} else if (questionType.equals(ExamProtos.Question.Type.OPTION_TF) && rightAnswerCount != 1) {
					invalidQuestionBuilder.setQuestionName(question.getQuestionName()).setFailText("请保证判断题有一个正确选项，确保考题类型和正确答案个数一致");
					tmpQuestionList.removeAll(Collections.singleton(question));
					invalidQuestionList.add(invalidQuestionBuilder.build());
				}
			}
		}
		
		if (!invalidQuestionList.isEmpty()) {
			return Futures.immediateFuture(ImportQuestionResponse.newBuilder()
					.setResult(ImportQuestionResponse.Result.FAIL_QUESTION_INVALID)
					.addAllInvalidQuestion(invalidQuestionList)
					.setFailText("导入考题失败！")
					.build());
		}
		
		final long adminId = head.getSession().getAdminId();
		final int now = (int) (System.currentTimeMillis() / 1000L);
		
		Connection conn = null;
		try {
			conn = hikariDataSource.getConnection();
			
			QuestionCategory questionCategory = ExamDB.getQuestionCategoryById(conn, companyId, Collections.singleton(questionCategoryId)).get(questionCategoryId);
			if (questionCategory == null) {
				return Futures.immediateFuture(ImportQuestionResponse.newBuilder()
						.setResult(ImportQuestionResponse.Result.FAIL_QUESTION_CATEGORY_INVALID)
						.setFailText("不存在的考题分类！")
						.build());
			}
			
			ExamDB.batchInsertQuestion(conn, companyId, tmpQuestionList, adminId, now, questionCategoryId);
			
			return Futures.immediateFuture(ImportQuestionResponse.newBuilder()
					.setResult(ImportQuestionResponse.Result.SUCC)
					.build());
		} catch (SQLException ex) {
			throw new RuntimeException("db fail ", ex);
		} finally {
			DBUtil.closeQuietly(conn);
		}
	}

	@Override
	public ListenableFuture<DeleteQuestionCategoryResponse> deleteQuestionCategory(
			AdminHead head, DeleteQuestionCategoryRequest request) {
		final long companyId = head.getCompanyId();
		
		final List<Integer> questionCategoryIdList = request.getCategoryIdList();
		
		Connection conn = null;
		try {
			conn = hikariDataSource.getConnection();
			Map<Integer, List<Integer>> questionCategoryMap = ExamDB.getQuestionIdByCategory(conn, companyId, 0, -1, questionCategoryIdList);
			for (Entry<Integer, List<Integer>> entry : questionCategoryMap.entrySet()) {
				if (!entry.getValue().isEmpty()) {
					return Futures.immediateFuture(DeleteQuestionCategoryResponse.newBuilder()
							.setResult(DeleteQuestionCategoryResponse.Result.FAIL_CATEGORY_INVALID)
							.setFailText("题库中存在考题，请先迁移！")
							.build());
				}
			}
			ExamDB.deleteQuestionCategory(conn, companyId, questionCategoryIdList);
			return Futures.immediateFuture(DeleteQuestionCategoryResponse.newBuilder()
					.setResult(DeleteQuestionCategoryResponse.Result.SUCC)
					.build());
		} catch (SQLException ex) {
			throw new RuntimeException("db fail", ex);
		} finally {
			DBUtil.closeQuietly(conn);
		}
	}

	@Override
	public ListenableFuture<EmptyResponse> loadExamSubmitTask(AdminHead head, EmptyRequest request) {
		this.doLoadExamSubmitTask();
		return ServiceUtil.EMPTY_RESPONSE_IMMEDIATE_FUTURE;
	}
	
	@Override
	public ListenableFuture<GetExamStatisticsResponse> getExamStatistics(AdminHead head,
			GetExamStatisticsRequest request) {
		final long companyId = head.getCompanyId();
		
		final List<Integer> examIdList = request.getExamIdList();
		// 除去尚未结束的考试
		final int now = (int) (System.currentTimeMillis() / 1000L); 
		Map<Integer, ExamDAOProtos.ExamInfo> examInfoMap = ExamUtil.getExamInfo(hikariDataSource, jedisPool, companyId, examIdList);
		
		Map<Integer, Integer> passMarkMap = Maps.newHashMap();
		for (ExamDAOProtos.ExamInfo examInfo : examInfoMap.values()) {
			if (examInfo.getExam().getEndTime() <= now) {
				passMarkMap.put(examInfo.getExam().getExamId(), examInfo.getExam().hasPassMark() ? examInfo.getExam().getPassMark() : 0);
			}
		}
		
		Map<Integer, ExamStatistics> examStatisticsMap;
		Connection conn = null;
		try {
			conn = this.hikariDataSource.getConnection();
			
			examStatisticsMap = ExamDB.getExamStatistics(conn, companyId, passMarkMap);
		} catch (SQLException ex) {
			throw new RuntimeException("db fail", ex);
		} finally {
			DBUtil.closeQuietly(conn);
		}
		
		return Futures.immediateFuture(GetExamStatisticsResponse.newBuilder()
				.addAllExamStatistics(examStatisticsMap.values())
				.build());
	}

	@Override
	public ListenableFuture<GetTeamStatisticsResponse> getTeamStatistics(AdminHead head,
			GetTeamStatisticsRequest request) {
		final long companyId = head.getCompanyId();
		
		final int start = request.getStart() < 0 ? 0 : request.getStart();
		final int length  = request.getLength() < 0 ? 0 : request.getLength() > 50 ? 50 : request.getLength();
		
		final int examId = request.getExamId();
		final String teamIdStr = request.hasTeamId() ? request.getTeamId() : null;
		List<Integer> teamIdList = Lists.newArrayList();
		Integer teamId = null;
		Integer teamLevel = null;
		try {
			if (teamIdStr != null) {
				List<String> teamIdStrList = SPLITTER.splitToList(teamIdStr);
				for (String idStr : teamIdStrList) {
					teamIdList.add(Integer.parseInt(idStr));
				}
			}
			teamId = teamIdList.isEmpty() ? null : teamIdList.get(teamIdList.size()-1);
			teamLevel = teamIdList.isEmpty() ? null : teamIdList.size() + 1;
		} catch (Exception ex) {
			
		}
		
		ExamDAOProtos.ExamInfo examInfo = ExamUtil.getExamInfo(hikariDataSource, jedisPool, companyId, Collections.singleton(examId)).get(examId);
		if (examInfo == null) {
			return Futures.immediateFuture(GetTeamStatisticsResponse.newBuilder()
					.setTotal(0)
					.setFilteredSize(0)
					.build());
		}
		
		Connection conn = null;
		try {
			conn = this.hikariDataSource.getConnection();
			
			DataPage<TeamStatistics> teamStatisticsPage = ExamDB.getTeamStatistics(conn, companyId, examId, examInfo.getExam().getPassMark(), teamId, teamLevel, start, length);
		
			return Futures.immediateFuture(GetTeamStatisticsResponse.newBuilder()
					.addAllTeamStatistics(teamStatisticsPage.dataList())
					.setTotal(teamStatisticsPage.totalSize())
					.setFilteredSize(teamStatisticsPage.totalSize())
					.build());
		} catch (SQLException ex) {
			throw new RuntimeException("db fail", ex);
		} finally {
			DBUtil.closeQuietly(conn);
		}
		
	}

	@Override
	public ListenableFuture<GetPositionStatisticsResponse> getPositionStatistics(AdminHead head,
			GetPositionStatisticsRequest request) {
		final long companyId = head.getCompanyId();
		
		final int examId = request.getExamId();
		final int start = request.getStart() < 0 ? 0 : request.getStart();
		final int length = request.getLength() < 0 ? 0 : request.getLength() > 50 ? 50 : request.getLength();
		
		ExamDAOProtos.ExamInfo examInfo = ExamUtil.getExamInfo(hikariDataSource, jedisPool, companyId, Collections.singleton(examId)).get(examId);
		if (examInfo == null) {
			return Futures.immediateFuture(GetPositionStatisticsResponse.newBuilder()
					.setTotal(0)
					.setFilteredSize(0)
					.build());
		}
		
		Connection conn = null;
		try {
			conn = this.hikariDataSource.getConnection();
			
			DataPage<PositionStatistics> positionStatisticsPage = ExamDB.getPositionStatistics(conn, companyId, examId, examInfo.getExam().getPassMark(), start, length);
			
			return Futures.immediateFuture(GetPositionStatisticsResponse.newBuilder()
					.addAllPostionStatistics(positionStatisticsPage.dataList())
					.setTotal(positionStatisticsPage.totalSize())
					.setFilteredSize(positionStatisticsPage.filteredSize())
					.build());
		} catch (SQLException ex) {
			throw new RuntimeException("db fail", ex);
		} finally {
			DBUtil.closeQuietly(conn);
		}
	}

	@Override
	public ListenableFuture<GetQuestionCorrectRateResponse> getQuestionCorrectRate(AdminHead head,
			GetQuestionCorrectRateRequest request) {
		final long companyId = head.getCompanyId();
		
		final int examId = request.getExamId();
		
		final int start = request.getStart() < 0 ? 0 : request.getStart();
		final int length = request.getLength() < 0 ? 0 : request.getLength() > 50 ? 50 : request.getLength();
		
		ExamDAOProtos.ExamInfo examInfo = ExamUtil.getExamInfo(hikariDataSource, jedisPool, companyId, Collections.singleton(examId)).get(examId);
		if (examInfo == null || !examInfo.getExam().getType().equals(ExamProtos.Exam.Type.MANUAL)) {
			return Futures.immediateFuture(GetQuestionCorrectRateResponse.newBuilder()
					.setTotal(0)
					.setFilteredSize(0)
					.build());
		}
		
		DataPage<Integer> questionIdPage = null;
		Map<Integer, ExamProtos.Question> questionMap = null;
		Map<Integer, Set<Integer>> rightQuestionMap = Maps.newHashMap();
		Map<Long, Map<Integer, Set<Integer>>> userQuestionAnswerMap = null;
		
		Connection conn = null;
		try {
			conn = this.hikariDataSource.getConnection();
			
			questionIdPage = ExamDB.getQuestionIdPageByExam(conn, companyId, start, length, examId);
			questionMap = ExamDB.getQuestion(conn, companyId, questionIdPage.dataList());
			if (questionMap.isEmpty()) {
				return Futures.immediateFuture(GetQuestionCorrectRateResponse.newBuilder()
						.setTotal(0)
						.setFilteredSize(0)
						.build());
			}
			
			for (Entry<Integer, ExamProtos.Question> entry : questionMap.entrySet()) {
				ExamProtos.Question question = entry.getValue();
				int questionId = entry.getKey();
				
				Set<Integer> rightOptionIdSet = Sets.newTreeSet();
				for (ExamProtos.Option option : question.getOptionList()) {
					if (option.getIsRight()) {
						rightOptionIdSet.add(option.getOptionId());
					}
				}
				rightQuestionMap.put(questionId, rightOptionIdSet);
			}

			userQuestionAnswerMap = ExamDB.getUserAnswerMap(conn, companyId, examId);
		} catch (SQLException ex) {
			throw new RuntimeException("db fail", ex);
		} finally {
			DBUtil.closeQuietly(conn);
		}
		
		Map<Integer, Integer> rightAnswerCountMap = Maps.newHashMap();
		Map<Integer, Integer> answerCountMap = Maps.newHashMap();
		for (Entry<Integer, Set<Integer>> entry : rightQuestionMap.entrySet()) {
			int questionId = entry.getKey();
			Set<Integer> rightOptionSet = entry.getValue();
			for (Map<Integer, Set<Integer>> answerMap : userQuestionAnswerMap.values()) {
				Set<Integer> userAnswer = answerMap.get(questionId);
				if (userAnswer != null && setEquals(userAnswer, rightOptionSet)) {
					int rightCount = rightAnswerCountMap.get(questionId) == null ? 0 : rightAnswerCountMap.get(questionId);
					rightCount++;
					rightAnswerCountMap.put(questionId, rightCount);
				}
				int answerCount = answerCountMap.get(questionId) == null ? 0 : answerCountMap.get(questionId);
				answerCount++;
				answerCountMap.put(questionId, answerCount);
			}
		}
		
		List<QuestionCorrect> questionCorrectList = Lists.newArrayList();
		for (int questionId : questionIdPage.dataList()) {
			ExamProtos.Question question = questionMap.get(questionId);
			if (question == null) {
				continue;
			}
			Integer answerNum = answerCountMap.get(questionId);
			Integer correctNum = rightAnswerCountMap.get(questionId);
			questionCorrectList.add(QuestionCorrect.newBuilder()
					.setAnswerNum(answerNum == null ? 0 : answerNum)
					.setCorrectNum(correctNum == null ? 0 : correctNum)
					.setQuestion(question)
					.build());
		}
		
		return Futures.immediateFuture(GetQuestionCorrectRateResponse.newBuilder()
				.addAllQuestionCorrect(questionCorrectList)
				.setFilteredSize(questionIdPage.filteredSize())
				.setTotal(questionIdPage.totalSize())
				.build());
	}
	
	@Override
	public ListenableFuture<GetUserAnswerResponse> getUserAnswer(AdminHead head, GetUserAnswerRequest request) {
		final long companyId = head.getCompanyId();
		final int examId = request.getExamId();
		final long userId = request.getUserId();
		
		ExamDAOProtos.ExamInfo examInfo = ExamUtil.getExamInfo(hikariDataSource, jedisPool, companyId, Collections.singleton(examId)).get(examId);
		if (examInfo == null) {
			return Futures.immediateFuture(GetUserAnswerResponse.getDefaultInstance());
		}
		
		Map<Integer, Integer> questionScoreMap = new LinkedHashMap<Integer, Integer>();
		for (ExamDAOProtos.ExamQuestion examQuestion : examInfo.getExamQuestionList()) {
			questionScoreMap.put(examQuestion.getQuestionId(), examQuestion.getScore());
		}
		
		final ExamProtos.UserResult userResult;
		final Map<Integer, Set<Integer>> userAnswerOptionMap;
		
		Connection conn = null;
		try {
			conn = this.hikariDataSource.getConnection();

			if (examInfo.getExam().getType().equals(ExamProtos.Exam.Type.AUTO)) {
				questionScoreMap = ExamDB.getExamQuestionScoreRandom(conn, companyId, examId, userId, examInfo.getRandomQuestionCategoryIdList(), examInfo.getRandomQuestionNum());
			}
			
			userResult = ExamDB.getUserResult(conn, companyId, userId, Collections.singleton(examId)).get(examId);
			userAnswerOptionMap = ExamDB.getExamUserAnswer(conn, companyId, userId, examId);
		} catch(SQLException ex) {
			throw new RuntimeException("db fail", ex);
		} finally {
			DBUtil.closeQuietly(conn);
		}
		
		Map<Integer, ExamProtos.Question> questionMap = ExamUtil.getQuestion(hikariDataSource, jedisPool, companyId, questionScoreMap.keySet());
		// 给考题加上分值
		LinkedHashMap<Integer, ExamProtos.Question> scoreMap = Maps.newLinkedHashMap();
		for (Entry<Integer, Integer> entry : questionScoreMap.entrySet()) {
			int questionId = entry.getKey();
			int score = entry.getValue();
			ExamProtos.Question question = questionMap.get(questionId);
			if (question != null) {
				ExamProtos.Question.Builder questionBuilder = ExamProtos.Question.newBuilder();
				scoreMap.put(questionId, questionBuilder.mergeFrom(question)
						.setScore(score)
						.build());
			}
		}

		GetUserAnswerResponse.Builder responseBuilder = GetUserAnswerResponse.newBuilder()
				.setExam(examInfo.getExam())
				.addAllQuestion(scoreMap.values());
		
		ExamProtos.UserAnswer.Builder tmpUserAnswerBuilder = ExamProtos.UserAnswer.newBuilder();
		for (Entry<Integer, Set<Integer>> entry : userAnswerOptionMap.entrySet()) {
			tmpUserAnswerBuilder.clear();
			
			Integer questionId = entry.getKey();
			Set<Integer> optionIdSet = entry.getValue();
			
			tmpUserAnswerBuilder.setQuestionId(questionId).addAllAnswerOptionId(optionIdSet);
			
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
			
			responseBuilder.addUserAnswer(tmpUserAnswerBuilder.build());
		}
		
		if (userResult != null) {
			responseBuilder.setUserResult(userResult);
		}

		return Futures.immediateFuture(responseBuilder.build());
	}
	
	private boolean setEquals(Set<Integer> set1, Set<Integer> set2) {
		if (set1.size() != set2.size()) {
			return false;
		}
		for (int s : set1) {
			if (!set2.contains(s)) {
				return false;
			}
		}
		return true;
	}
	
	public Set<Long> getExamUser(long companyId, @Nullable Integer allowModelId) {
		Set<Long> examUserIdSet = Sets.newTreeSet();
		int start = 0;
		final int length = 500;
		while (true) {
			GetUserListResponse getUserListResponse = Futures.getUnchecked(
					AdminExamServiceImpl.this.adminUserService.getUserList(
					SystemHead.newBuilder()
						.setCompanyId(companyId)
						.build(), 
					GetUserListRequest.newBuilder()
						.setStart(start)
						.setLength(length)
						.build())
					);
			
			Set<Long> userIdSet = Sets.newTreeSet();
			for (UserProtos.User user : getUserListResponse.getUserList()) {
				if (user.getBase().getState().equals(UserProtos.UserBase.State.NORMAL)) {
					userIdSet.add(user.getBase().getUserId());
				}
			}

			if (allowModelId == null) {
				examUserIdSet.addAll(userIdSet);
			} else {
				CheckAllowResponse checkAllowResponse = Futures.getUnchecked(allowService.checkAllow(
						SystemHead.newBuilder()
							.setCompanyId(companyId)
							.build(), 
						CheckAllowRequest.newBuilder()
							.addAllUserId(userIdSet)
							.addModelId(allowModelId)
							.build())
						);
				for (AllowProtos.CheckAllowResponse.CheckResult checkResult : checkAllowResponse.getCheckResultList()) {
					if (checkResult.getModelId() == allowModelId) {
						examUserIdSet.addAll(checkResult.getAllowUserIdList());
					}
				}
			}
			
			start += length;
			if (start >= getUserListResponse.getFilteredSize()) {
				break;
			}
		}
		return examUserIdSet;
	}
	
	private final ConcurrentMap<Integer, Integer> examIdToSubmitTimeMap = new ConcurrentHashMap<Integer, Integer>();
	
	private void doLoadExamSubmitTask() {
		Map<Long, List<Integer>> companyExamMap;
		
		Connection dbConn = null;
		try {
			dbConn = AdminExamServiceImpl.this.hikariDataSource.getConnection();
			companyExamMap = ExamDB.getUnsubmitExamId(dbConn);
		} catch (SQLException e) {
			throw new RuntimeException("load exam submit task db fail", e);
		} finally {
			DBUtil.closeQuietly(dbConn);
		}
		
		for (Entry<Long, List<Integer>> entry : companyExamMap.entrySet()) {
			final long companyId = entry.getKey();
			List<Integer> examIdList = entry.getValue();

			Map<Integer, ExamDAOProtos.ExamInfo> examInfoMap = ExamUtil.getExamInfo(hikariDataSource, jedisPool, companyId, examIdList);

			int now = (int) (System.currentTimeMillis() / 1000L);
			for (ExamDAOProtos.ExamInfo examInfo : examInfoMap.values()) {
				if (examInfo.getExam().hasIsSubmitExecute() && examInfo.getExam().getIsSubmitExecute()) {
					continue;
				}

				final Integer examId = examInfo.getExam().getExamId();
				final Integer endTime = new Integer(examInfo.getExam().getEndTime()); // 保证必须是new出来的对象，执行任务时会对比是否是同一个对象

				boolean isTaskAdd = false;

				while (true) {
					Integer oldSubmitTime = this.examIdToSubmitTimeMap
							.get(examId);
					if (oldSubmitTime == null) {
						if (this.examIdToSubmitTimeMap.putIfAbsent(examId,
								endTime) == null) {
							// 没有此任务，且添加成功
							isTaskAdd = true;
							break;
						}
					} else {
						if (oldSubmitTime.equals(endTime)) {
							// 和原有任务重复
							isTaskAdd = false;
							break;
						} else {
							if (this.examIdToSubmitTimeMap.replace(examId,
									oldSubmitTime, endTime)) {
								// 更新了提交时间且放置成功
								isTaskAdd = true;
								break;
							}
						}
					}
				}

				if (isTaskAdd) {

					DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
					logger.info("load exam submit task : " + examId + ","
							+ endTime + ","
							+ df.format(new Date(endTime * 1000L)));

					if (endTime.intValue() <= now) {
						this.serviceExecutor.execute(new ExamSubmitTask(companyId, examId, endTime));
					} else {
						this.scheduledExecutorService.schedule(new Runnable() {

							@Override
							public void run() {
								AdminExamServiceImpl.this.serviceExecutor.execute(new ExamSubmitTask(companyId, examId, endTime));
							}

						}, endTime.intValue() - now, TimeUnit.SECONDS);
					}

				}
			}
		}
	}
	
	private class ExamSubmitTask implements Runnable {

		private final long companyId;
		private final int examId;
		private final Integer submitTime;
		
		ExamSubmitTask(long companyId, int examId, Integer submitTime) {
			this.companyId = companyId;
			this.examId = examId;
			this.submitTime = submitTime;
		}
		
		@Override
		public void run() {
			// 这里时对比是否是同一个对象，而不是值是否相等
			if (submitTime != AdminExamServiceImpl.this.examIdToSubmitTimeMap.get(examId)) {
				return;
			}
			
			ExamDAOProtos.ExamInfo examInfo = ExamUtil.getExamInfo(hikariDataSource, jedisPool, companyId, Collections.singleton(examId)).get(examId);
			if (examInfo == null 
					|| (examInfo.getExam().hasIsSubmitExecute() && examInfo.getExam().getIsSubmitExecute())
					|| (examInfo.getExam().getEndTime() != submitTime.intValue())
					) {
				return;
			}

			// 所有参与考试的人
			final Set<Long> takeExamUserIdSet;
			
			Connection dbConn = null;
			try {
				dbConn = AdminExamServiceImpl.this.hikariDataSource.getConnection();

				if (!ExamDB.setExamSubmitExecuted(dbConn, companyId, examId)) {
					return;
				}
				
				List<Long> unsubmitUserIdList = ExamDB.getUnsubmitUserId(dbConn, companyId, examId);
				if (!unsubmitUserIdList.isEmpty()) {
					DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
					logger.info("exam submit task execute : [" + examId + "," + submitTime + "," + df.format(new Date(submitTime * 1000L)) + "] " + unsubmitUserIdList);
					
					Map<Integer, Integer> questionScoreMap = new LinkedHashMap<Integer, Integer>();
					for (ExamDAOProtos.ExamQuestion examQuestion : examInfo.getExamQuestionList()) {
						questionScoreMap.put(examQuestion.getQuestionId(), examQuestion.getScore());
					}
					Map<Integer, ExamProtos.Question> questionMap = ExamDB.getQuestion(dbConn, companyId, questionScoreMap.keySet());
					
					for (final long userId : unsubmitUserIdList) {
						if (examInfo.getExam().getType().equals(ExamProtos.Exam.Type.AUTO)) {
							questionScoreMap = ExamDB.getExamQuestionScoreRandom(dbConn, companyId, examId, userId, examInfo.getRandomQuestionCategoryIdList(), examInfo.getRandomQuestionNum());
							questionMap = ExamDB.getQuestion(dbConn, companyId, questionScoreMap.keySet());
						}
						
						Map<Integer, Set<Integer>> userAnswerMap = ExamDB.getExamUserAnswer(dbConn, companyId, userId, examId);
						
						int totalScore = 0;
						for (Entry<Integer, Set<Integer>> entry : userAnswerMap.entrySet()) {
							final Integer questionId = entry.getKey();
							final Set<Integer> optionIdSet = entry.getValue();
							
							ExamProtos.Question question = questionMap.get(questionId);
							if (question == null) {
								continue;
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
						ExamDB.saveUserScore(dbConn, companyId, userId, examId, totalScore, null, null);
					}
				}
				takeExamUserIdSet = ExamDB.getTakeExamUserId(dbConn, companyId, examId);
			} catch (SQLException e) {
				AdminExamServiceImpl.logger.error("exam submit task db fail: " + examId + ", " + submitTime, e);
				return;
			} finally {
				DBUtil.closeQuietly(dbConn);
				AdminExamServiceImpl.this.examIdToSubmitTimeMap.remove(examId, submitTime);
			}
			
			int start = 0;
			final int length = 500;
			while (true) {
				GetUserListResponse getUserListResponse = Futures.getUnchecked(
						AdminExamServiceImpl.this.adminUserService.getUserList(
						SystemHead.newBuilder()
							.setCompanyId(companyId)
							.build(), 
						GetUserListRequest.newBuilder()
							.setStart(start)
							.setLength(length)
							.build())
						);
				
				Set<Long> userIdSet = Sets.newTreeSet();
				for (UserProtos.User user : getUserListResponse.getUserList()) {
					if (user.getBase().getState().equals(UserProtos.UserBase.State.NORMAL)) {
						userIdSet.add(user.getBase().getUserId());
					}
				}
				
				Set<Long> allowUserIdSet = Sets.newTreeSet();
				if (examInfo.getExam().hasAllowModelId()) {
					CheckAllowResponse checkAllowResponse = Futures.getUnchecked(allowService.checkAllow(
							SystemHead.newBuilder()
								.setCompanyId(companyId)
								.build(), 
							CheckAllowRequest.newBuilder()
								.addAllUserId(userIdSet)
								.addModelId(examInfo.getExam().getAllowModelId())
								.build())
							);
					for (AllowProtos.CheckAllowResponse.CheckResult checkResult : checkAllowResponse.getCheckResultList()) {
						if (checkResult.getModelId() == examInfo.getExam().getAllowModelId()) {
							allowUserIdSet.addAll(checkResult.getAllowUserIdList());
						}
					}
				}
				
				Map<Integer, UserProtos.Team> teamMap = Maps.newTreeMap();
				for (UserProtos.Team team : getUserListResponse.getRefTeamList()) {
					teamMap.put(team.getTeamId(), team);
				}
				Map<Integer, UserProtos.Position> positionMap = Maps.newTreeMap();
				for (UserProtos.Position position : getUserListResponse.getRefPositionList()) {
					positionMap.put(position.getPositionId(), position);
				}
				Map<Integer, UserProtos.Level> levelMap = Maps.newTreeMap();
				for (UserProtos.Level level : getUserListResponse.getRefLevelList()) {
					levelMap.put(level.getLevelId(), level);
				}
				
				Map<Long, List<Integer>> userTeamIdListMap = Maps.newTreeMap();
				Map<Long, Integer> userPositionIdMap = Maps.newTreeMap();
				Map<Long, Integer> userLevelIdMap = Maps.newTreeMap();
				
				for (UserProtos.User user : getUserListResponse.getUserList()) {
					final long userId = user.getBase().getUserId();
					
					if (user.getTeamCount() > 0) {
						LinkedList<Integer> teamIdList = Lists.newLinkedList();
						
						int tmpTeamId = user.getTeam(0).getTeamId();
						while (true) {
							UserProtos.Team team = teamMap.get(tmpTeamId);
							if (team == null) {
								// warn : cannot find team
								teamIdList.clear();
								break;
							}
			
							teamIdList.addFirst(team.getTeamId());
			
							if (team.hasParentTeamId()) {
								tmpTeamId = team.getParentTeamId();
							} else {
								break;
							}
						}
						
						userTeamIdListMap.put(user.getBase().getUserId(), teamIdList);
						
						if (user.getTeam(0).hasPositionId() && positionMap.containsKey(user.getTeam(0).getPositionId())) {
							userPositionIdMap.put(userId, user.getTeam(0).getPositionId());
						}
					}
					
					if (user.getBase().hasLevelId() && levelMap.containsKey(user.getBase().getLevelId())) {
						userLevelIdMap.put(userId, user.getBase().getLevelId());
					}
				}
				
				Connection conn = null;
				try {
					conn = AdminExamServiceImpl.this.hikariDataSource.getConnection();
					
					ExamDB.saveUserTeam(conn, companyId, examId, allowUserIdSet, takeExamUserIdSet, userTeamIdListMap, userPositionIdMap, userLevelIdMap);
				} catch (SQLException e) {
					AdminExamServiceImpl.logger.error("save exam user fail", e);
				} finally {
					DBUtil.closeQuietly(conn);
				}
				
				start += length;
				if (start >= getUserListResponse.getFilteredSize()) {
					break;
				}
			}
			
			Connection conn = null;
			try {
				conn = AdminExamServiceImpl.this.hikariDataSource.getConnection();
				// 运行结束之后更新加载所有用户字段
				ExamDB.setIsLoadAllUser(conn, companyId, examId);
			} catch (SQLException e) {
				AdminExamServiceImpl.logger.error("save exam user fail", e);
			} finally {
				DBUtil.closeQuietly(conn);
			}
			
			Jedis jedis = jedisPool.getResource();
			try {
				ExamCache.delExamInfo(jedis, companyId, Collections.singleton(examId));
			} finally {
				jedis.close();
			}
		}
	}
	
	private final class SendSecretaryMessageTask implements Runnable {

		private final AdminHead adminHead;
		
		private final String examName;
		private final int startTime;
		private final int endTime;
		private final Integer allowModelId;
		
		public SendSecretaryMessageTask(AdminHead adminHead, String examName, int startTime, int endTime, Integer allowModelId) {
			this.adminHead = adminHead;
			this.examName = examName;
			this.startTime = startTime;
			this.endTime = endTime;
			this.allowModelId = allowModelId;
		}
		
		@Override
		public void run() {
			final long companyId = adminHead.getCompanyId();
			Set<Long> sendUserIdSet = AdminExamServiceImpl.this.getExamUser(companyId, allowModelId);
			
			SimpleDateFormat df = new SimpleDateFormat("MM-dd HH:mm");
			
			ProfileManager.Profile profile = profileManager.getProfile(adminHead, "exam:");
			String template = profile.get(EXAM_TEMPLATE).replace("${name}", examName)
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
				logger.info("小秘书考试提醒：" + template + ", 提醒人数：" + list.size());
			}
		}
		
	}

}
