package com.weizhu.service.qa.test;

import static org.junit.Assert.*;

import java.util.concurrent.ExecutionException;

import org.junit.BeforeClass;
import org.junit.Test;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.weizhu.common.module.TestModule;
import com.weizhu.common.utils.TestUtil;
import com.weizhu.proto.QAProtos;
import com.weizhu.proto.QAService;
import com.weizhu.proto.WeizhuProtos.RequestHead;
import com.weizhu.service.qa.QAServiceModule;

public class QAServiceImplTest {
	static {
		System.setProperty("logback.configurationFile", "com/weizhu/service/qa/test/logback.xml");
	}

	private static final Injector INJECTOR = Guice.createInjector(new TestModule(), new QAServiceTestModule(), new QAServiceModule());

	@BeforeClass
	public static void init() {
		TestUtil.clearCache(INJECTOR);
		TestUtil.loadTestDataDB(INJECTOR);
	}

	private final RequestHead head;
	private final QAService qaService;

	public QAServiceImplTest() {
		this.head = INJECTOR.getInstance(RequestHead.class);
		this.qaService = INJECTOR.getInstance(QAService.class);
	}

	@Test
	public void testGetQuestion() throws InterruptedException, ExecutionException {
		QAProtos.GetQuestionRequest request = QAProtos.GetQuestionRequest.newBuilder().setCategoryId(1)
		//.setLastQuestionId(100)
				.setSize(4)
				.build();

		QAProtos.GetQuestionResponse response = qaService.getQuestion(head, request).get();
		assertTrue(response.getQuestionCount() > 0);
		assertTrue(response.getRefAnswerCount() > 0);
	}

	@Test
	public void testAddQuestion() throws InterruptedException, ExecutionException {
		QAProtos.AddQuestionRequest request = QAProtos.AddQuestionRequest.newBuilder().setCategoryId(2).setQuestionContent("测试4").build();

		QAProtos.AddQuestionResponse response = qaService.addQuestion(head, request).get();
		assertEquals(QAProtos.AddQuestionResponse.Result.SUCC, response.getResult());
	}

	@Test
	public void testGetAnswer() throws InterruptedException, ExecutionException {
		QAProtos.GetAnswerRequest request = QAProtos.GetAnswerRequest.newBuilder().setQuestionId(1).setLastAnswerId(100).setSize(3).build();

		QAProtos.GetAnswerResponse response = qaService.getAnswer(head, request).get();
		assertTrue(response.getAnswerCount() > 0);
	}

	@Test
	public void testAddAnswer() throws InterruptedException, ExecutionException {
		QAProtos.AddAnswerRequest request = QAProtos.AddAnswerRequest.newBuilder().setQuestionId(2).setAnswerContent("测试answer2").build();

		QAProtos.AddAnswerResponse response = qaService.addAnswer(head, request).get();
		assertEquals(QAProtos.AddAnswerResponse.Result.SUCC, response.getResult());
	}

	@Test
	public void testLikeAnswer() throws InterruptedException, ExecutionException {
		QAProtos.LikeAnswerRequest request = QAProtos.LikeAnswerRequest.newBuilder().setAnswerId(6).setIsLike(true).build();
		QAProtos.LikeAnswerResponse response = qaService.likeAnswer(head, request).get();
		assertEquals(QAProtos.LikeAnswerResponse.Result.SUCC, response.getResult());
	}

	@Test
	public void testSearchQuestion() throws InterruptedException, ExecutionException {
		QAProtos.SearchQuestionRequest request = QAProtos.SearchQuestionRequest.newBuilder().setKeyword("测试问题1").build();

		QAProtos.SearchQuestionResponse response = qaService.searchQuestion(head, request).get();
		assertTrue(response.getQuestionCount() > 0);
		assertTrue(response.getRefCategoryCount() > 0);
	}

	@Test
	public void testSearchMoreQuestion() throws InterruptedException, ExecutionException {
		QAProtos.SearchMoreQuestionRequest request = QAProtos.SearchMoreQuestionRequest.newBuilder().setKeyword("").setSize(100).setCategoryId(3).build();

		QAProtos.SearchMoreQuestionResponse response = qaService.searchMoreQuestion(head, request).get();
		assertTrue(response.getQuestionCount() > 0);
	}
}
