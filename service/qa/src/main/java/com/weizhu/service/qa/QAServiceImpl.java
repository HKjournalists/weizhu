package com.weizhu.service.qa;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.Executor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import redis.clients.jedis.JedisPool;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.weizhu.common.db.DBUtil;
import com.weizhu.proto.QAProtos;
import com.weizhu.proto.QAProtos.DeleteQuestionRequest;
import com.weizhu.proto.QAProtos.DeleteQuestionResponse;
import com.weizhu.proto.QAProtos.GetCategoryResponse;
import com.weizhu.proto.QAProtos.Question;
import com.weizhu.proto.QAProtos.SearchMoreQuestionRequest;
import com.weizhu.proto.QAProtos.SearchMoreQuestionResponse;
import com.weizhu.proto.QAProtos.SearchQuestionRequest;
import com.weizhu.proto.QAProtos.SearchQuestionResponse;
import com.weizhu.proto.QAService;
import com.weizhu.proto.QAProtos.AddAnswerRequest;
import com.weizhu.proto.QAProtos.AddAnswerResponse;
import com.weizhu.proto.QAProtos.AddQuestionRequest;
import com.weizhu.proto.QAProtos.AddQuestionResponse;
import com.weizhu.proto.QAProtos.GetAnswerRequest;
import com.weizhu.proto.QAProtos.GetAnswerResponse;
import com.weizhu.proto.QAProtos.GetQuestionRequest;
import com.weizhu.proto.QAProtos.GetQuestionResponse;
import com.weizhu.proto.QAProtos.LikeAnswerRequest;
import com.weizhu.proto.QAProtos.LikeAnswerResponse;
import com.weizhu.proto.WeizhuProtos.EmptyRequest;
import com.weizhu.proto.WeizhuProtos.RequestHead;
import com.zaxxer.hikari.HikariDataSource;

public class QAServiceImpl implements QAService {
	
	private static final Logger logger = LoggerFactory.getLogger(AdminQAServiceImpl.class);
	@SuppressWarnings("unused")
	private final Executor serviceExecutor;
	private final HikariDataSource hikariDataSource;
	private final JedisPool jedisPool;

	@Inject
	public QAServiceImpl(@Named("service_executor") Executor serviceExecutor, HikariDataSource hikariDataSource, JedisPool jedisPool) {
		this.serviceExecutor = serviceExecutor;
		this.hikariDataSource = hikariDataSource;
		this.jedisPool = jedisPool;
	}

	@Override
	public ListenableFuture<GetQuestionResponse> getQuestion(RequestHead head, GetQuestionRequest request) {
		final long companyId = head.getSession().getCompanyId();

		final int size;
		boolean hasMore;
		Integer lastQuestionId = request.hasLastQuestionId() ? request.getLastQuestionId() : null;
		Integer categoryId = request.hasCategoryId() ? request.getCategoryId() : null;
		if (request.getSize() < 0) {
			return Futures.immediateFuture(GetQuestionResponse.newBuilder().setHasMore(false).build());
		} else if (request.getSize() <= 100) {
			size = request.getSize();
		} else {
			size = 100;
		}

		Connection dbConn = null;
		List<Integer> questionIdList = null;
		Map<Integer, Integer> bestAnswerIdMap = null;
		// get questionIdList from DB
		try {
			dbConn = hikariDataSource.getConnection();
			questionIdList = QADB.getQuestionIdListByLastId(dbConn, companyId, lastQuestionId, size + 1, categoryId, null);
			bestAnswerIdMap = QADB.getBestAnswerId(dbConn, companyId, questionIdList);
		} catch (SQLException e) {
			throw new RuntimeException("db fail", e);
		} finally {
			DBUtil.closeQuietly(dbConn);
		}
		if (questionIdList.size() > size) {
			hasMore = true;
			questionIdList = questionIdList.subList(0, size);
		} else {
			hasMore = false;
		}

		// get questionInfo from redis or DB
		if (questionIdList.isEmpty()) {
			return Futures.immediateFuture(GetQuestionResponse.newBuilder().setHasMore(false).build());
		}
		Map<Integer, QAProtos.Question> questionInfoMap = QAUtil.getQuestion(hikariDataSource, jedisPool, companyId, questionIdList);

		// set best_answer to question

		QAProtos.Question.Builder questionBuilder = QAProtos.Question.newBuilder();
		for (java.util.Map.Entry<Integer, Question> entry : questionInfoMap.entrySet()) {
			questionBuilder.clear();
			questionBuilder.mergeFrom(entry.getValue());
			if (bestAnswerIdMap.get(entry.getKey()) != null) {
				questionBuilder.setBestAnswerId(bestAnswerIdMap.get(entry.getKey()));
			}
			questionBuilder.setCanDelete(entry.getValue().getUserId() == head.getSession().getUserId());
			entry.setValue(questionBuilder.build());
		}
		// recover the sequence of questions
		GetQuestionResponse.Builder responseBuilder = GetQuestionResponse.newBuilder().setHasMore(hasMore);
		for (int questionId : questionIdList) {
			QAProtos.Question question = questionInfoMap.get(questionId);
			if (question != null) {
				responseBuilder.addQuestion(question);
			}
		}
		Map<Integer, QAProtos.Answer> answerInfoMap = QAUtil.getAnswer(hikariDataSource, jedisPool, companyId, bestAnswerIdMap.values(), head.getSession()
				.getUserId());
		responseBuilder.addAllRefAnswer(answerInfoMap.values());

		logger.info("get question end, userId:"+head.getSession().getUserId());
		
		return Futures.immediateFuture(responseBuilder.build());
	}

	@Override
	public ListenableFuture<AddQuestionResponse> addQuestion(RequestHead head, AddQuestionRequest request) {
		final long companyId = head.getSession().getCompanyId();

		String questionContent = request.getQuestionContent();
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

		List<Integer> questionIds = QAUtil.addQuestion(hikariDataSource, jedisPool, companyId, Collections.singletonList(questionContent), head.getSession()
				.getUserId(), null, categoryId);

		logger.info("add question end, userId:"+head.getSession().getUserId());
		
		return Futures.immediateFuture(AddQuestionResponse.newBuilder()
				.setQuestionId(questionIds.iterator().next())
				.setResult(AddQuestionResponse.Result.SUCC)
				.build());
	}

	@Override
	public ListenableFuture<GetAnswerResponse> getAnswer(RequestHead head, GetAnswerRequest request) {
		final long companyId = head.getSession().getCompanyId();

		final int size;
		boolean hasMore;
		Integer lastAnswerId = request.hasLastAnswerId() ? request.getLastAnswerId() : null;
		if (request.getSize() < 0) {
			return Futures.immediateFuture(GetAnswerResponse.newBuilder().setHasMore(false).build());
		} else if (request.getSize() <= 100) {
			size = request.getSize();
		} else {
			size = 100;
		}
		// get answerIdList from DB
		Connection dbConn = null;
		List<Integer> answerIdList = null;
		try {
			dbConn = hikariDataSource.getConnection();
			answerIdList = QADB.getAnswerIdListByLastId(dbConn, companyId, lastAnswerId, size + 1, request.getQuestionId());
		} catch (SQLException e) {
			throw new RuntimeException("db fail", e);
		} finally {
			DBUtil.closeQuietly(dbConn);
		}

		if (answerIdList.size() > size) {
			hasMore = true;
			answerIdList = answerIdList.subList(0, size);
		} else {
			hasMore = false;
		}

		if (answerIdList.isEmpty()) {
			return Futures.immediateFuture(GetAnswerResponse.newBuilder().setHasMore(false).build());
		}
		Map<Integer, QAProtos.Answer> answerInfoMap = QAUtil.getAnswer(hikariDataSource, jedisPool, companyId, answerIdList, head.getSession().getUserId());
		// recover the sequence of answers
		GetAnswerResponse.Builder responseBuilder = GetAnswerResponse.newBuilder().setHasMore(hasMore);
		for (int answerId : answerIdList) {
			QAProtos.Answer answer = answerInfoMap.get(answerId);
			if (answer != null) {
				responseBuilder.addAnswer(answer);
			}
		}
		
		logger.info("get answer end, userId:"+head.getSession().getUserId());
		
		return Futures.immediateFuture(responseBuilder.build());
	}

	@Override
	public ListenableFuture<AddAnswerResponse> addAnswer(RequestHead head, AddAnswerRequest request) {
		final long companyId = head.getSession().getCompanyId();

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
		long userId = head.getSession().getUserId();

		Map<Integer, Question> questions = QAUtil.getQuestion(hikariDataSource, jedisPool, companyId, Collections.singleton(questionId));

		if (questions.isEmpty()) {
			return Futures.immediateFuture(AddAnswerResponse.newBuilder()
					.setResult(AddAnswerResponse.Result.FAIL_QUESTION_NOT_EXIST)
					.setFailText("该问题不存在！")
					.build());
		}

		int answerId = QAUtil.addAnswer(hikariDataSource, jedisPool, companyId, answerContent, userId, null, questionId);

		logger.info("add answer end, userId:"+head.getSession().getUserId());
		
		return Futures.immediateFuture(AddAnswerResponse.newBuilder().setAnswerId(answerId).setResult(AddAnswerResponse.Result.SUCC).build());
	}

	@Override
	public ListenableFuture<LikeAnswerResponse> likeAnswer(RequestHead head, LikeAnswerRequest request) {
		final long companyId = head.getSession().getCompanyId();

		int answerId = request.getAnswerId();
		Map<Integer, QAProtos.Answer> answers = QAUtil.getAnswer(hikariDataSource, jedisPool, companyId, Collections.singleton(answerId), head.getSession()
				.getUserId());

		if (answers.isEmpty()) {
			return Futures.immediateFuture(LikeAnswerResponse.newBuilder()
					.setResult(LikeAnswerResponse.Result.FAIL_ANSWER_NOT_EXIST)
					.setFailText("点赞的回答不存在！")
					.build());
		}

		Connection dbConn = null;

		try {
			dbConn = hikariDataSource.getConnection();
			if (request.getIsLike()) {
				QADB.likeAnswer(dbConn, companyId, head.getSession().getUserId(), answerId);
			} else {
				QADB.deletelikeAnswer(dbConn, companyId, head.getSession().getUserId(), answerId);
			}
		} catch (SQLException e) {

			throw new RuntimeException("db fail", e);
		} finally {
			DBUtil.closeQuietly(dbConn);
		}

		logger.info("like answer end, userId:"+head.getSession().getUserId());
		
		return Futures.immediateFuture(LikeAnswerResponse.newBuilder().setResult(LikeAnswerResponse.Result.SUCC).build());
	}

	@Override
	public ListenableFuture<GetCategoryResponse> getCategory(RequestHead head, EmptyRequest request) {
		final long companyId = head.getSession().getCompanyId();

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
		
		logger.info("get category end, userId:"+head.getSession().getUserId());
		
		return Futures.immediateFuture(GetCategoryResponse.newBuilder()
				.addAllCategory(QAUtil.getCategory(hikariDataSource, jedisPool, companyId, categoryIds).values())
				.build());
	}

	@Override
	public ListenableFuture<QAProtos.DeleteAnswerResponse> deleteAnswer(RequestHead head, QAProtos.DeleteAnswerRequest request) {
		final long companyId = head.getSession().getCompanyId();

		int answerId = request.getAnswerId();
		Map<Integer, QAProtos.Answer> answerMap = QAUtil.getAnswer(hikariDataSource, jedisPool, companyId, Collections.singleton(answerId), head.getSession()
				.getUserId());
		if (answerMap.isEmpty()) {
			return Futures.immediateFuture(QAProtos.DeleteAnswerResponse.newBuilder().setResult(QAProtos.DeleteAnswerResponse.Result.SUCC).build());
		} else {
			QAProtos.Answer answer = answerMap.get(answerId);
			if (answer.hasUserId() && answer.getUserId() != head.getSession().getUserId()) {
				return Futures.immediateFuture(QAProtos.DeleteAnswerResponse.newBuilder()
						.setResult(QAProtos.DeleteAnswerResponse.Result.FAIL_ANSWER_OTHER)
						.setFailText("其他人给出的回答，不能删除")
						.build());
			}
		}

		QAUtil.deleteAnswer(hikariDataSource, jedisPool, companyId, Collections.singletonList(request.getAnswerId()));

		logger.info("delete answer end, userId:"+head.getSession().getUserId());
		
		return Futures.immediateFuture(QAProtos.DeleteAnswerResponse.newBuilder().setResult(QAProtos.DeleteAnswerResponse.Result.SUCC).build());
	}

	@Override
	public ListenableFuture<DeleteQuestionResponse> deleteQuestion(RequestHead head, DeleteQuestionRequest request) {
		final long companyId = head.getSession().getCompanyId();

		int questionId = request.getQuestionId();
		Map<Integer, Question> questionMap = QAUtil.getQuestion(hikariDataSource, jedisPool, companyId, Collections.singleton(questionId));
		if (questionMap.isEmpty()) {
			return Futures.immediateFuture(QAProtos.DeleteQuestionResponse.newBuilder()
					.setResult(QAProtos.DeleteQuestionResponse.Result.SUCC)
					.build());
		} else {
			QAProtos.Question question = questionMap.get(questionId);
			if (question.hasUserId() && question.getUserId() != head.getSession().getUserId()) {
				return Futures.immediateFuture(QAProtos.DeleteQuestionResponse.newBuilder()
						.setResult(QAProtos.DeleteQuestionResponse.Result.FAIL_QUESTION_OTHER)
						.setFailText("其他人提出的问题，不能删除")
						.build());
			}
		}
		Connection dbConn = null;
		try {
			dbConn = hikariDataSource.getConnection();
			if (!(QADB.getAnswerIdListByQusIds(dbConn, companyId, Collections.singletonList(questionId)).isEmpty())) {
				return Futures.immediateFuture(QAProtos.DeleteQuestionResponse.newBuilder()
						.setResult(QAProtos.DeleteQuestionResponse.Result.FAIL_QUESTION_HAS_ANSWER)
						.setFailText("问题下有回答，不能删除")
						.build());
			}
		} catch (SQLException e) {
			throw new RuntimeException("DB FAILED");
		} finally {
			DBUtil.closeQuietly(dbConn);
		}

		QAUtil.deleteQuestion(hikariDataSource, jedisPool, companyId, Collections.singletonList(questionId));

		logger.info("delete question end, userId:"+head.getSession().getUserId());
		
		return Futures.immediateFuture(QAProtos.DeleteQuestionResponse.newBuilder().setResult(QAProtos.DeleteQuestionResponse.Result.SUCC).build());
	}

	@Override
	public ListenableFuture<SearchQuestionResponse> searchQuestion(RequestHead head, SearchQuestionRequest request) {
		final long companyId = head.getSession().getCompanyId();

		Connection dbConn = null;
		Map<Integer, List<Integer>> categoryQueMap = null;
		List<Integer> questionIds = null;
		Map<Integer, Integer> bestAnswerIdMap = null;
		try {
			dbConn = hikariDataSource.getConnection();
			categoryQueMap = QADB.getQuestionIdList(dbConn, companyId, request.getKeyword());
			questionIds = new ArrayList<Integer>();
			for (Entry<Integer, List<Integer>> entry : categoryQueMap.entrySet()) {
				questionIds.addAll(entry.getValue());
			}
			bestAnswerIdMap = QADB.getBestAnswerId(dbConn, companyId, questionIds);
		} catch (SQLException e) {
			throw new RuntimeException("db failed");
		} finally {
			DBUtil.closeQuietly(dbConn);
		}
		if (categoryQueMap.isEmpty()) {
			return Futures.immediateFuture(QAProtos.SearchQuestionResponse.newBuilder().build());
		}

		Map<Integer, QAProtos.Question> questionInfoMap = QAUtil.getQuestion(hikariDataSource, jedisPool, companyId, questionIds);

		QAProtos.Question.Builder questionBuilder = QAProtos.Question.newBuilder();
		for (Entry<Integer, Question> entry : questionInfoMap.entrySet()) {
			questionBuilder.clear();
			questionBuilder.mergeFrom(entry.getValue());
			if (bestAnswerIdMap.get(entry.getKey()) != null) {
				questionBuilder.setBestAnswerId(bestAnswerIdMap.get(entry.getKey()));
			}
			questionBuilder.setCanDelete(entry.getValue().getUserId() == head.getSession().getUserId());
			entry.setValue(questionBuilder.build());
		}
		Map<Integer, QAProtos.Answer> answerInfoMap = QAUtil.getAnswer(hikariDataSource, jedisPool, companyId, bestAnswerIdMap.values(), head.getSession()
				.getUserId());

		logger.info("search question end, userId:"+head.getSession().getUserId());
		
		return Futures.immediateFuture(QAProtos.SearchQuestionResponse.newBuilder()
				.addAllRefCategory(QAUtil.getCategory(hikariDataSource, jedisPool, companyId, categoryQueMap.keySet()).values())
				.addAllQuestion(questionInfoMap.values())
				.addAllRefAnswer(answerInfoMap.values())
				.build());

	}

	@Override
	public ListenableFuture<SearchMoreQuestionResponse> searchMoreQuestion(RequestHead head, SearchMoreQuestionRequest request) {
		final long companyId = head.getSession().getCompanyId();

		final int size;
		boolean hasMore;
		Integer lastQuestionId = request.hasLastQuestionId() ? request.getLastQuestionId() : null;
		Integer categoryId = request.hasCategoryId() ? request.getCategoryId() : null;
		if (request.getSize() < 0) {
			return Futures.immediateFuture(SearchMoreQuestionResponse.newBuilder().setHasMore(false).build());
		} else if (request.getSize() <= 100) {
			size = request.getSize();
		} else {
			size = 100;
		}

		Connection dbConn = null;
		List<Integer> questionIdList = null;
		Map<Integer, Integer> bestAnswerIdMap = null;
		// get questionIdList from DB
		try {
			dbConn = hikariDataSource.getConnection();

			questionIdList = QADB.getQuestionIdListByLastId(dbConn, companyId, lastQuestionId, size + 1, categoryId, request.getKeyword());
			bestAnswerIdMap = QADB.getBestAnswerId(dbConn, companyId, questionIdList);
		} catch (SQLException e) {
			throw new RuntimeException("db fail", e);
		} finally {
			DBUtil.closeQuietly(dbConn);
		}
		if (questionIdList.size() > size) {
			hasMore = true;
			questionIdList = questionIdList.subList(0, size);
		} else {
			hasMore = false;
		}

		// get questionInfo from redis or DB
		if (questionIdList.isEmpty()) {
			return Futures.immediateFuture(SearchMoreQuestionResponse.newBuilder().setHasMore(false).build());
		}
		Map<Integer, QAProtos.Question> questionInfoMap = QAUtil.getQuestion(hikariDataSource, jedisPool, companyId, questionIdList);

		// set best_answer to question

		QAProtos.Question.Builder questionBuilder = QAProtos.Question.newBuilder();
		for (java.util.Map.Entry<Integer, Question> entry : questionInfoMap.entrySet()) {
			questionBuilder.clear();
			questionBuilder.mergeFrom(entry.getValue());
			if (bestAnswerIdMap.get(entry.getKey()) != null) {
				questionBuilder.setBestAnswerId(bestAnswerIdMap.get(entry.getKey()));
			}
			questionBuilder.setCanDelete(entry.getValue().getUserId() == head.getSession().getUserId());
			entry.setValue(questionBuilder.build());
		}
		// recover the sequence of questions
		SearchMoreQuestionResponse.Builder responseBuilder = SearchMoreQuestionResponse.newBuilder().setHasMore(hasMore);
		for (int questionId : questionIdList) {
			QAProtos.Question question = questionInfoMap.get(questionId);
			if (question != null) {
				responseBuilder.addQuestion(question);
			}
		}

		Map<Integer, QAProtos.Answer> answerInfoMap = QAUtil.getAnswer(hikariDataSource, jedisPool, companyId, bestAnswerIdMap.values(), head.getSession()
				.getUserId());

		responseBuilder.addAllRefAnswer(answerInfoMap.values());

		logger.info("search more question end, userId:"+head.getSession().getUserId());
		
		return Futures.immediateFuture(responseBuilder.build());
	}

}
