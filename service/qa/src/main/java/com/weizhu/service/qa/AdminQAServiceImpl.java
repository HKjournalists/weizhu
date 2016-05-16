package com.weizhu.service.qa;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.weizhu.common.db.DBUtil;
import com.weizhu.proto.AdminProtos.AdminHead;
import com.weizhu.proto.AdminQAProtos;
import com.weizhu.proto.AdminQAProtos.AddAnswerResponse;
import com.weizhu.proto.AdminQAProtos.AddCategoryRequest;
import com.weizhu.proto.AdminQAProtos.AddCategoryResponse;
import com.weizhu.proto.AdminQAProtos.AddQuestionRequest;
import com.weizhu.proto.AdminQAProtos.AddQuestionResponse;
import com.weizhu.proto.AdminQAProtos.ChangeQuestionCategoryRequest;
import com.weizhu.proto.AdminQAProtos.ChangeQuestionCategoryResponse;
import com.weizhu.proto.AdminQAProtos.DeleteAnswerResponse;
import com.weizhu.proto.AdminQAProtos.DeleteCategoryRequest;
import com.weizhu.proto.AdminQAProtos.DeleteCategoryResponse;
import com.weizhu.proto.AdminQAProtos.DeleteQuestionResponse;
import com.weizhu.proto.AdminQAProtos.ExportQuestionRequest;
import com.weizhu.proto.AdminQAProtos.ExportQuestionResponse;
import com.weizhu.proto.AdminQAProtos.GetAnswerRequest;
import com.weizhu.proto.AdminQAProtos.GetAnswerResponse;
import com.weizhu.proto.AdminQAProtos.GetCategoryResponse;
import com.weizhu.proto.AdminQAProtos.GetQuestionRequest;
import com.weizhu.proto.AdminQAProtos.GetQuestionResponse;
import com.weizhu.proto.AdminQAProtos.ImportQuestionRequest;
import com.weizhu.proto.AdminQAProtos.ImportQuestionResponse;
import com.weizhu.proto.AdminQAProtos.UpdateCategoryRequest;
import com.weizhu.proto.AdminQAProtos.UpdateCategoryResponse;
import com.weizhu.proto.AdminQAService;
import com.weizhu.proto.QAProtos;
import com.weizhu.proto.QAProtos.Question;
import com.weizhu.proto.WeizhuProtos.EmptyRequest;
import com.zaxxer.hikari.HikariDataSource;

public class AdminQAServiceImpl implements AdminQAService {
	
	private static final Logger logger = LoggerFactory.getLogger(AdminQAServiceImpl.class);
	@SuppressWarnings("unused")
	private final Executor serviceExecutor;
	private final HikariDataSource hikariDataSource;
	private final JedisPool jedisPool;

	@Inject
	public AdminQAServiceImpl(@Named("service_executor") Executor serviceExecutor, HikariDataSource hikariDataSource, JedisPool jedisPool) {
		this.serviceExecutor = serviceExecutor;
		this.hikariDataSource = hikariDataSource;
		this.jedisPool = jedisPool;
	}

	@Override
	public ListenableFuture<GetQuestionResponse> getQuestion(AdminHead head, GetQuestionRequest request) {
		if (!head.hasCompanyId()) {
			return Futures.immediateFuture(GetQuestionResponse.newBuilder().build());
		}
		final long companyId = head.getCompanyId();

		Integer start = request.hasStart() ? request.getStart() : null;
		Integer categoryId = request.hasCategoryId() ? request.getCategoryId() : null;
		int length = request.getLength();
		String keyword = request.hasKeyword() ? request.getKeyword() : "";
		int totalSize;
		if (request.getLength() < 0) {
			return Futures.immediateFuture(GetQuestionResponse.newBuilder().setTotalSize(0).setFilteredSize(0).build());
		} else if (length > 1000) {
			length = 1000;
		}
		// get questionIdList from DB
		Connection dbConn = null;
		Set<Integer> categoryIdList = null;
		List<Integer> questionIdList = null;
		try {
			dbConn = hikariDataSource.getConnection();
			questionIdList = QADB.getQuestionIdListByStart(dbConn, companyId, start, length, categoryId, keyword);
			categoryIdList = QADB.getCategoryIdListByQueId(dbConn, companyId, questionIdList);
			totalSize = QADB.getTotalQuestionNum(dbConn, companyId, categoryId, keyword);
		} catch (SQLException e) {
			throw new RuntimeException("db fail", e);
		} finally {
			DBUtil.closeQuietly(dbConn);
		}

		// get questionInfo from redis or DB
		if (questionIdList.isEmpty()) {
			return Futures.immediateFuture(GetQuestionResponse.newBuilder().setTotalSize(totalSize).setFilteredSize(totalSize).build());
		}
		Map<Integer, QAProtos.Question> questionInfoMap = QAUtil.getQuestion(hikariDataSource, jedisPool, companyId, questionIdList);
		// recover the sequence of questions
		GetQuestionResponse.Builder responseBuilder = GetQuestionResponse.newBuilder().setTotalSize(totalSize).setFilteredSize(totalSize);
		for (int questionId : questionIdList) {
			QAProtos.Question question = questionInfoMap.get(questionId);
			if (question != null) {
				responseBuilder.addQuestion(question);
			}
		}
		responseBuilder.addAllRefCategory(QAUtil.getCategory(hikariDataSource, jedisPool, companyId, categoryIdList).values());
		
		logger.info("get question end, adminId:"+head.getSession().getAdminId());
		
		return Futures.immediateFuture(responseBuilder.build());
	}

	@Override
	public ListenableFuture<AddQuestionResponse> addQuestion(AdminHead head, AddQuestionRequest request) {
		if (!head.hasCompanyId()) {
			throw new RuntimeException("company_id 参数未填");
		}
		final long companyId = head.getCompanyId();

		String questionContent = request.getQuestionContent();
		String answerContent = request.hasAnswerContent() ? request.getAnswerContent() : null;
		if (questionContent.isEmpty()) {
			return Futures.immediateFuture(AddQuestionResponse.newBuilder()
					.setResult(AddQuestionResponse.Result.FAIL_CONTENT_INVALID)
					.setFailText("问题内容不能为空！")
					.build());
		} else if (questionContent.length() > 100) {
			return Futures.immediateFuture(AddQuestionResponse.newBuilder()
					.setResult(AddQuestionResponse.Result.FAIL_CONTENT_INVALID)
					.setFailText("问题内容太长，请不要超过100个字符！")
					.build());
		}
		int categoryId = request.getCategoryId();
		Map<Integer, QAProtos.Category> categoryMap = QAUtil.getCategory(hikariDataSource, jedisPool, companyId, Collections.singleton(categoryId));

		if (categoryMap.isEmpty()) {
			return Futures.immediateFuture(AddQuestionResponse.newBuilder()
					.setResult(AddQuestionResponse.Result.FAIL_CATEGORY_NOT_EXIST)
					.setFailText("该分类不存在！")
					.build());
		}
		List<Integer> questionIds = QAUtil.addQuestion(hikariDataSource,
				jedisPool,
				companyId,
				Collections.singletonList(questionContent),
				null,
				head.getSession().getAdminId(),
				categoryId);
		int questionId = questionIds.iterator().next();
		Integer answerId = null;
		if (answerContent != null && !answerContent.isEmpty()) {
			answerId = QAUtil.addAnswer(hikariDataSource, jedisPool, companyId, answerContent, null, head.getSession().getAdminId(), questionId);
		}
		AddQuestionResponse.Builder responseBuildder = AddQuestionResponse.newBuilder();
		responseBuildder.setQuestionId(questionId);
		responseBuildder.setResult(AddQuestionResponse.Result.SUCC);
		if (answerId != null) {
			responseBuildder.setAnswerId(answerId);
		}
		logger.info("add question end , adminId:"+head.getSession().getAdminId());
		return Futures.immediateFuture(responseBuildder.build());
	}

	@Override
	public ListenableFuture<ImportQuestionResponse> importQuestion(AdminHead head, ImportQuestionRequest request) {
		if (!head.hasCompanyId()) {
			throw new RuntimeException("company_id 参数未填");
		}
		final long companyId = head.getCompanyId();

		List<ImportQuestionRequest.QuestionAnswer> quesAnswerList = request.getQuestionAnswerList();
		List<String> qConList = new ArrayList<String>();
		if (quesAnswerList.isEmpty()) {
			return Futures.immediateFuture(ImportQuestionResponse.newBuilder()
					.setResult(ImportQuestionResponse.Result.FAIL_CONTENT_INVALID)
					.setFailText("问题内容不能为空！")
					.build());
		} else {
			for (ImportQuestionRequest.QuestionAnswer quesAnswer : quesAnswerList) {
				String question_content = quesAnswer.getQuestionContent();
				if (null == question_content || "".equals(question_content)) {
					return Futures.immediateFuture(ImportQuestionResponse.newBuilder()
							.setResult(ImportQuestionResponse.Result.FAIL_CONTENT_INVALID)
							.setFailText("问题内容不能为空！")
							.build());
				}
				qConList.add(question_content);
			}
		}
		int categoryId = request.getCategoryId();
		Map<Integer, QAProtos.Category> categoryMap = QAUtil.getCategory(hikariDataSource, jedisPool, companyId, Collections.singleton(categoryId));

		if (categoryMap.isEmpty()) {
			return Futures.immediateFuture(ImportQuestionResponse.newBuilder()
					.setResult(ImportQuestionResponse.Result.FAIL_CATEGORY_NOT_EXIST)
					.setFailText("该分类不存在！")
					.build());
		}

		List<Integer> questonIds = QAUtil.addQuestion(hikariDataSource,
				jedisPool,
				companyId,
				qConList,
				null,
				head.getSession().getAdminId(),
				request.getCategoryId());
		Map<Integer, String> queIdAnsContMap = new HashMap<Integer, String>();
		for (int i = 0; i < quesAnswerList.size(); i++) {
			ImportQuestionRequest.QuestionAnswer quesAnswer = quesAnswerList.get(i);
			if (quesAnswer.hasAnswerContent()) {
				queIdAnsContMap.put(questonIds.get(i), quesAnswer.getAnswerContent());
			}
		}
		QAUtil.addAnswer(hikariDataSource, jedisPool, companyId, queIdAnsContMap, null, head.getSession().getAdminId());
		
		logger.info("import question end , adminId:"+head.getSession().getAdminId());
		
		return Futures.immediateFuture(ImportQuestionResponse.newBuilder().setResult(ImportQuestionResponse.Result.SUCC).build());
	}

	@Override
	public ListenableFuture<AdminQAProtos.DeleteQuestionResponse> deleteQuestion(AdminHead head, AdminQAProtos.DeleteQuestionRequest request) {
		if (!head.hasCompanyId()) {
			throw new RuntimeException("company_id 参数未填");
		}
		final long companyId = head.getCompanyId();
		List<Integer> qIdList = request.getQuestionIdList();
		if (qIdList.isEmpty()) {
			return Futures.immediateFuture(DeleteQuestionResponse.newBuilder()
					.setResult(DeleteQuestionResponse.Result.FAIL_QUESTION_NOT_EXIST)
					.setFailText("需要删除的问题ID不能为空！")
					.build());
		}
		QAUtil.deleteQuestion(hikariDataSource, jedisPool, companyId, qIdList);
		
		logger.info("delete question end , adminId:"+head.getSession().getAdminId());
		
		return Futures.immediateFuture(DeleteQuestionResponse.newBuilder().setResult(DeleteQuestionResponse.Result.SUCC).build());
	}

	@Override
	public ListenableFuture<GetAnswerResponse> getAnswer(AdminHead head, GetAnswerRequest request) {
		if (!head.hasCompanyId()) {
			return Futures.immediateFuture(GetAnswerResponse.newBuilder()
					.setTotalSize(0)
					.setFilteredSize(0)
					.build());
		}
		final long companyId = head.getCompanyId();

		int length = request.getLength();
		int totalSize;
		Integer start = request.hasStart() ? request.getStart() : null;
		if (request.getLength() < 0) {
			return Futures.immediateFuture(GetAnswerResponse.newBuilder().setTotalSize(0).setFilteredSize(0).build());
		} else if (length > 1000) {
			length = 1000;
		}
		// get answerIdList from DB
		Connection dbConn = null;
		List<Integer> answerIdList = null;
		try {
			dbConn = hikariDataSource.getConnection();
			answerIdList = QADB.getAnswerIdListByStart(dbConn, companyId, start, length, request.getQuestionId());
			totalSize = QADB.getTotalAnswerNum(dbConn, companyId, request.getQuestionId());
		} catch (SQLException e) {
			throw new RuntimeException("db fail", e);
		} finally {
			DBUtil.closeQuietly(dbConn);
		}

		// get answerInfo from redis or DB
		if (answerIdList.isEmpty()) {
			return Futures.immediateFuture(GetAnswerResponse.newBuilder().setTotalSize(totalSize).setFilteredSize(totalSize).build());
		}
		Map<Integer, QAProtos.Answer> answerInfoMap = QAUtil.getAnswer(hikariDataSource, jedisPool, companyId, answerIdList, null);

		// recover the sequence of answers
		GetAnswerResponse.Builder responseBuilder = GetAnswerResponse.newBuilder().setTotalSize(totalSize).setFilteredSize(totalSize);
		for (int answerId : answerIdList) {
			QAProtos.Answer answer = answerInfoMap.get(answerId);
			if (answer != null) {
				responseBuilder.addAnswer(answer);
			}
		}
		
		logger.info("get answer end , adminId:"+head.getSession().getAdminId());
		
		return Futures.immediateFuture(responseBuilder.build());
	}

	@Override
	public ListenableFuture<AdminQAProtos.AddAnswerResponse> addAnswer(AdminHead head, AdminQAProtos.AddAnswerRequest request) {
		if (!head.hasCompanyId()) {
			throw new RuntimeException("company_id 参数未填");
		}
		final long companyId = head.getCompanyId();

		String answerContent = request.getAnswerContent();
		if (answerContent.isEmpty()) {
			return Futures.immediateFuture(AddAnswerResponse.newBuilder()
					.setResult(AddAnswerResponse.Result.FAIL_CONTENT_INVALID)
					.setFailText("回答内容不能为空")
					.build());
		} else if (answerContent.length() > 1000) {
			return Futures.immediateFuture(AddAnswerResponse.newBuilder()
					.setResult(AddAnswerResponse.Result.FAIL_CONTENT_INVALID)
					.setFailText("回答内容不能大于1000个字符！")
					.build());
		}
		int questionId = request.getQuestionId();
		Map<Integer, Question> questions = QAUtil.getQuestion(hikariDataSource, jedisPool, companyId, Collections.singleton(questionId));

		if (questions.isEmpty()) {
			return Futures.immediateFuture(AddAnswerResponse.newBuilder()
					.setResult(AddAnswerResponse.Result.FAIL_QUESTION_NOT_EXIST)
					.setFailText("该问题不存在！")
					.build());
		}
		int answerId = QAUtil.addAnswer(hikariDataSource, jedisPool, companyId, answerContent, null, head.getSession().getAdminId(), questionId);
		
		logger.info("add answer end , adminId:"+head.getSession().getAdminId());
		
		return Futures.immediateFuture(AddAnswerResponse.newBuilder().setAnswerId(answerId).setResult(AddAnswerResponse.Result.SUCC).build());
	}

	@Override
	public ListenableFuture<AdminQAProtos.DeleteAnswerResponse> deleteAnswer(AdminHead head, AdminQAProtos.DeleteAnswerRequest request) {
		if (!head.hasCompanyId()) {
			throw new RuntimeException("company_id 参数未填");
		}
		final long companyId = head.getCompanyId();
		List<Integer> aIdList = request.getAnswerIdList();
		if (aIdList.isEmpty()) {
			return Futures.immediateFuture(DeleteAnswerResponse.newBuilder().setResult(DeleteAnswerResponse.Result.FAIL_ANSWER_NOT_EXIST).build());
		}

		QAUtil.deleteAnswer(hikariDataSource, jedisPool, companyId, aIdList);
		
		logger.info("delete answer end, adminId:"+head.getSession().getAdminId());
		
		return Futures.immediateFuture(DeleteAnswerResponse.newBuilder().setResult(DeleteAnswerResponse.Result.SUCC).build());
	}

	@Override
	public ListenableFuture<GetCategoryResponse> getCategory(AdminHead head, EmptyRequest request) {
		if (!head.hasCompanyId()) {
			return Futures.immediateFuture(GetCategoryResponse.newBuilder().build());
		}
		final long companyId = head.getCompanyId();
		Connection conn = null;
		List<Integer> categoryIds = null;
		try {
			conn = hikariDataSource.getConnection();
			categoryIds = QADB.getCategoryIdList(conn, companyId);
		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			DBUtil.closeQuietly(conn);
		}
		
		logger.info("get category end, adminId:"+head.getSession().getAdminId());
		
		return Futures.immediateFuture(GetCategoryResponse.newBuilder()
				.addAllCategory(QAUtil.getCategory(hikariDataSource, jedisPool, companyId, categoryIds).values())
				.build());
	}

	@Override
	public ListenableFuture<AddCategoryResponse> addCategory(AdminHead head, AddCategoryRequest request) {
		if (!head.hasCompanyId()) {
			throw new RuntimeException("company_id 参数未填");
		}
		final long companyId = head.getCompanyId();

		String categoryName = request.getCategoryName();
		if (categoryName.isEmpty()) {
			return Futures.immediateFuture(AddCategoryResponse.newBuilder()
					.setResult(AddCategoryResponse.Result.FAIL_CONTENT_INVALID)
					.setFailText("分类名不能为空！")
					.build());
		} else if (categoryName.length() > 50) {
			return Futures.immediateFuture(AddCategoryResponse.newBuilder()
					.setResult(AddCategoryResponse.Result.FAIL_CONTENT_INVALID)
					.setFailText("分类名太长，请不要超过50个字符！")
					.build());
		}
		Connection dbConn = null;
		try {
			dbConn = hikariDataSource.getConnection();
			if (QADB.getCategoryNum(dbConn, companyId) >= QAUtil.CATEGORY_MAX_NUMBER) {
				return Futures.immediateFuture(AddCategoryResponse.newBuilder()
						.setResult(AddCategoryResponse.Result.FAIL_CATEGORY_MAX_NUMBER_OUT)
						.setFailText("分类个数超过最大限制！")
						.build());
			}
		} catch (SQLException e) {
			throw new RuntimeException("db fail", e);
		} finally {
			DBUtil.closeQuietly(dbConn);
		}
		int categoryId = QAUtil.addCategory(hikariDataSource, jedisPool, companyId, categoryName, null, head.getSession().getAdminId());
		
		logger.info("add category end, adminId:"+head.getSession().getAdminId());
		
		return Futures.immediateFuture(AddCategoryResponse.newBuilder().setCategoryId(categoryId).setResult(AddCategoryResponse.Result.SUCC).build());
	}

	@Override
	public ListenableFuture<DeleteCategoryResponse> deleteCategory(AdminHead head, DeleteCategoryRequest request) {
		if (!head.hasCompanyId()) {
			throw new RuntimeException("company_id 参数未填");
		}
		final long companyId = head.getCompanyId();

		int categoryId = request.getCategoryId();
		Connection dbConn = null;
		try {
			dbConn = hikariDataSource.getConnection();
			if (QADB.hasQueInCategory(dbConn, companyId, categoryId)) {
				return Futures.immediateFuture(DeleteCategoryResponse.newBuilder()
						.setResult(DeleteCategoryResponse.Result.FAIL_CATEGORY_HAS_QUESTION)
						.setFailText("分类下存在问题不能删除")
						.build());
			} else {
				QADB.deleteCategory(dbConn, companyId, categoryId);
			}
		} catch (SQLException e) {
			throw new RuntimeException("db fail", e);
		} finally {
			DBUtil.closeQuietly(dbConn);
		}
		Jedis jedis = jedisPool.getResource();
		try {
			QACache.delCategory(jedis, companyId, Collections.singleton(categoryId));
		} finally {
			jedis.close();
		}
		
		logger.info("delete category end , adminId:"+head.getSession().getAdminId());
		
		return Futures.immediateFuture(DeleteCategoryResponse.newBuilder().setResult(DeleteCategoryResponse.Result.SUCC).build());

	}

	@Override
	public ListenableFuture<UpdateCategoryResponse> updateCategory(AdminHead head, UpdateCategoryRequest request) {
		if (!head.hasCompanyId()) {
			throw new RuntimeException("company_id 参数未填");
		}
		final long companyId = head.getCompanyId();

		String categoryName = request.getCategoryName();
		int categoryId = request.getCategoryId();
		if (categoryName.isEmpty()) {
			return Futures.immediateFuture(UpdateCategoryResponse.newBuilder()
					.setResult(UpdateCategoryResponse.Result.FAIL_CONTENT_INVALID)
					.setFailText("分类名不能为空！")
					.build());
		} else if (categoryName.length() > 50) {
			return Futures.immediateFuture(UpdateCategoryResponse.newBuilder()
					.setResult(UpdateCategoryResponse.Result.FAIL_CONTENT_INVALID)
					.setFailText("分类名太长，请不要超过50个字符！")
					.build());
		}

		Map<Integer, QAProtos.Category> categoryMap = QAUtil.getCategory(hikariDataSource, jedisPool, companyId, Collections.singleton(categoryId));

		if (categoryMap.isEmpty()) {
			return Futures.immediateFuture(UpdateCategoryResponse.newBuilder()
					.setResult(UpdateCategoryResponse.Result.FAIL_CATEGORY_NOT_EXIST)
					.setFailText("该分类不存在！")
					.build());
		}

		Connection dbConn = null;
		try {
			dbConn = hikariDataSource.getConnection();
			QADB.updateCategory(dbConn, companyId, categoryName, categoryId);
		} catch (SQLException e) {
			throw new RuntimeException("db fail", e);
		} finally {
			DBUtil.closeQuietly(dbConn);
		}

		Jedis jedis = jedisPool.getResource();
		try {
			QACache.delCategory(jedis, companyId, Collections.singleton(categoryId));
		} finally {
			jedis.close();
		}
		
		logger.info("update category end , adminId:"+head.getSession().getAdminId());
		
		return Futures.immediateFuture(UpdateCategoryResponse.newBuilder().setResult(UpdateCategoryResponse.Result.SUCC).build());
	}

	@Override
	public ListenableFuture<ExportQuestionResponse> exportQuestion(AdminHead head, ExportQuestionRequest request) {
		if (!head.hasCompanyId()) {
			throw new RuntimeException("company_id 参数未填");
		}
		final long companyId = head.getCompanyId();

		final int size;
		boolean hasMore;
		Integer lastQuestionId = request.hasLastQuestionId() ? request.getLastQuestionId() : null;
		Integer categoryId = request.hasCategoryId() ? request.getCategoryId() : null;
		String questionContent = request.hasKeyword() ? request.getKeyword() : null;
		if (request.getSize() < 0) {
			return Futures.immediateFuture(ExportQuestionResponse.newBuilder().setHasMore(false).build());
		} else if (request.getSize() <= 100) {
			size = request.getSize();
		} else {
			size = 100;
		}

		Connection dbConn = null;
		List<Integer> questionIdList = null;
		Set<Integer> categoryIdList = null;
		// get questionIdList from DB
		try {
			dbConn = hikariDataSource.getConnection();
			questionIdList = QADB.getQuestionIdListByLastId(dbConn, companyId, lastQuestionId, size + 1, categoryId, questionContent);
			categoryIdList = QADB.getCategoryIdListByQueId(dbConn, companyId, questionIdList);
		} catch (SQLException e) {
			throw new RuntimeException("db fail", e);
		} finally {
			DBUtil.closeQuietly(dbConn);
		}
		if (questionIdList.size() > size) {
			hasMore = true;
			questionIdList.subList(0, size);
		} else {
			hasMore = false;
		}

		// get questionInfo from redis or DB
		if (questionIdList.isEmpty()) {
			return Futures.immediateFuture(ExportQuestionResponse.newBuilder().setHasMore(false).build());
		}
		Map<Integer, QAProtos.Question> questionInfoMap = QAUtil.getQuestion(hikariDataSource, jedisPool, companyId, questionIdList);
		// recover the sequence of questions
		ExportQuestionResponse.Builder responseBuilder = ExportQuestionResponse.newBuilder().setHasMore(hasMore);
		for (int questionId : questionIdList) {
			QAProtos.Question question = questionInfoMap.get(questionId);
			if (question != null) {
				responseBuilder.addQuestion(question);
			}
		}
		responseBuilder.addAllRefCategory(QAUtil.getCategory(hikariDataSource, jedisPool, companyId, categoryIdList).values());
		
		logger.info("export question end , adminId:"+head.getSession().getAdminId());
		
		return Futures.immediateFuture(responseBuilder.build());
	}

	@Override
	public ListenableFuture<ChangeQuestionCategoryResponse> changeQuestionCategory(AdminHead head, ChangeQuestionCategoryRequest request) {
		if (!head.hasCompanyId()) {
			throw new RuntimeException("company_id 参数未填");
		}
		final long companyId = head.getCompanyId();

		List<Integer> questionIds = request.getQuestionIdList();
		if (questionIds.isEmpty()) {
			return Futures.immediateFuture(AdminQAProtos.ChangeQuestionCategoryResponse.newBuilder()
					.setResult(ChangeQuestionCategoryResponse.Result.FAIL_QUESTION_NOT_EXIST)
					.setFailText("问题id不能为空")
					.build());
		}
		int categoryId = request.getCategoryId();
		Map<Integer, QAProtos.Category> categoryMap = QAUtil.getCategory(hikariDataSource, jedisPool, companyId, Collections.singleton(categoryId));

		if (categoryMap.isEmpty()) {
			return Futures.immediateFuture(ChangeQuestionCategoryResponse.newBuilder()
					.setResult(ChangeQuestionCategoryResponse.Result.FAIL_CATEGORY_NOT_EXIST)
					.setFailText("该分类不存在！")
					.build());
		}

		Connection dbConn = null;
		try {
			dbConn = hikariDataSource.getConnection();
			QADB.updateQuestionCategory(dbConn, companyId, questionIds, categoryId);
		} catch (SQLException e) {
			throw new RuntimeException("db fail", e);
		} finally {
			DBUtil.closeQuietly(dbConn);
		}

		Jedis jedis = jedisPool.getResource();
		try {
			QACache.delQuestion(jedis, companyId, questionIds);
		} finally {
			jedis.close();
		}
		
		logger.info("migrate question end , adminId:"+head.getSession().getAdminId());
		
		return Futures.immediateFuture(AdminQAProtos.ChangeQuestionCategoryResponse.newBuilder()
				.setResult(ChangeQuestionCategoryResponse.Result.SUCC)
				.build());
	}

}
