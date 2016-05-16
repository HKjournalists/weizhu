package com.weizhu.service.qa.test;

import static org.junit.Assert.*;

import java.util.concurrent.ExecutionException;

import org.junit.BeforeClass;
import org.junit.Test;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.weizhu.common.module.TestModule;
import com.weizhu.common.utils.TestUtil;
import com.weizhu.proto.AdminQAProtos;
import com.weizhu.proto.AdminQAProtos.ImportQuestionRequest.QuestionAnswer;
import com.weizhu.proto.AdminQAService;
import com.weizhu.proto.AdminProtos.AdminHead;
import com.weizhu.proto.WeizhuProtos.EmptyRequest;
import com.weizhu.service.qa.QAServiceModule;

public class AdminQAServiceImplTest {

	static {
		System.setProperty("logback.configurationFile", "com/weizhu/service/qa/test/logback.xml");
	}

	private static final Injector INJECTOR = Guice.createInjector(new TestModule(), new QAServiceModule(), new QAServiceTestModule());

	@BeforeClass
	public static void init() {
		TestUtil.clearCache(INJECTOR);
		TestUtil.loadTestDataDB(INJECTOR);
	}

	private final AdminHead adminHead;
	private final AdminQAService adminQAService;

	public AdminQAServiceImplTest() {
		this.adminHead = INJECTOR.getInstance(AdminHead.class);
		this.adminQAService = INJECTOR.getInstance(AdminQAService.class);
	}

	@Test
	public void testGetQuestion() throws InterruptedException, ExecutionException {
		AdminQAProtos.GetQuestionRequest request = AdminQAProtos.GetQuestionRequest.newBuilder().setStart(0).setLength(100).setKeyword("测试问题").build();

		AdminQAProtos.GetQuestionResponse response = adminQAService.getQuestion(adminHead, request).get();
		assertTrue(response.getQuestionCount() > 0);
	}

	@Test
	public void testAddQuestion() throws InterruptedException, ExecutionException {
		AdminQAProtos.AddQuestionRequest request = AdminQAProtos.AddQuestionRequest.newBuilder().setQuestionContent("测试admin").setCategoryId(2).build();

		AdminQAProtos.AddQuestionResponse response = adminQAService.addQuestion(adminHead, request).get();
		assertEquals(AdminQAProtos.AddQuestionResponse.Result.SUCC, response.getResult());
	}

	@Test
	public void testImportQuestion() throws InterruptedException, ExecutionException {
		QuestionAnswer.Builder questionAnswerBuilder = QuestionAnswer.newBuilder();
		questionAnswerBuilder.setAnswerContent("admin测试回答二");
		questionAnswerBuilder.setQuestionContent("admin测试问题二");
		AdminQAProtos.ImportQuestionRequest request = AdminQAProtos.ImportQuestionRequest.newBuilder()
				.addQuestionAnswer(questionAnswerBuilder.build())
				.addQuestionAnswer(questionAnswerBuilder.build())
				.setCategoryId(2)
				.build();

		AdminQAProtos.ImportQuestionResponse response = adminQAService.importQuestion(adminHead, request).get();
		assertEquals(AdminQAProtos.ImportQuestionResponse.Result.SUCC, response.getResult());
	}

	@Test
	public void testDeleteQuestion() throws InterruptedException, ExecutionException {
		AdminQAProtos.DeleteQuestionRequest request = AdminQAProtos.DeleteQuestionRequest.newBuilder().addQuestionId(3).addQuestionId(4).addQuestionId(5).build();

		AdminQAProtos.DeleteQuestionResponse response = adminQAService.deleteQuestion(adminHead, request).get();
		assertEquals(AdminQAProtos.DeleteQuestionResponse.Result.SUCC, response.getResult());
	}

	@Test
	public void testGetAnswer() throws InterruptedException, ExecutionException {
		AdminQAProtos.GetAnswerRequest request = AdminQAProtos.GetAnswerRequest.newBuilder().setQuestionId(1).setStart(0).setLength(100).build();

		AdminQAProtos.GetAnswerResponse response = adminQAService.getAnswer(adminHead, request).get();
		assertTrue(response.getAnswerCount() > 0);
	}

	@Test
	public void testAddAnswer() throws InterruptedException, ExecutionException {
		AdminQAProtos.AddAnswerRequest request = AdminQAProtos.AddAnswerRequest.newBuilder().setQuestionId(1).setAnswerContent("测试admin answer").build();

		AdminQAProtos.AddAnswerResponse response = adminQAService.addAnswer(adminHead, request).get();
		assertEquals(AdminQAProtos.AddAnswerResponse.Result.SUCC, response.getResult());
	}

	@Test
	public void testDeleteAnswer() throws InterruptedException, ExecutionException {
		AdminQAProtos.DeleteAnswerRequest request = AdminQAProtos.DeleteAnswerRequest.newBuilder().addAnswerId(20).addAnswerId(21).addAnswerId(22).build();

		AdminQAProtos.DeleteAnswerResponse response = adminQAService.deleteAnswer(adminHead, request).get();
		assertEquals(AdminQAProtos.DeleteAnswerResponse.Result.SUCC, response.getResult());
	}

	@Test
	public void testAddCategory() throws InterruptedException, ExecutionException {
		AdminQAProtos.AddCategoryRequest request = AdminQAProtos.AddCategoryRequest.newBuilder().setCategoryName("科技").build();

		AdminQAProtos.AddCategoryResponse response = adminQAService.addCategory(adminHead, request).get();
		assertEquals(AdminQAProtos.AddCategoryResponse.Result.SUCC, response.getResult());
	}

	@Test
	public void testGetCategory() throws InterruptedException, ExecutionException {
		EmptyRequest request = EmptyRequest.newBuilder().build();

		AdminQAProtos.GetCategoryResponse response = adminQAService.getCategory(adminHead, request).get();
		assertTrue(response.getCategoryCount() > 0);
	}

	@Test
	public void testUpdateCategory() throws InterruptedException, ExecutionException {
		AdminQAProtos.UpdateCategoryRequest request = AdminQAProtos.UpdateCategoryRequest.newBuilder().setCategoryName("科技").setCategoryId(1).build();

		AdminQAProtos.UpdateCategoryResponse response = adminQAService.updateCategory(adminHead, request).get();
		assertEquals(AdminQAProtos.UpdateCategoryResponse.Result.SUCC, response.getResult());
	}

	@Test
	public void testDeleteCategory() throws InterruptedException, ExecutionException {
		AdminQAProtos.DeleteCategoryRequest request = AdminQAProtos.DeleteCategoryRequest.newBuilder().setCategoryId(5).build();

		AdminQAProtos.DeleteCategoryResponse response = adminQAService.deleteCategory(adminHead, request).get();
		assertEquals(AdminQAProtos.DeleteCategoryResponse.Result.SUCC, response.getResult());
	}
	
	@Test
	public void testExportQuestion() throws InterruptedException, ExecutionException {
		AdminQAProtos.ExportQuestionRequest request = AdminQAProtos.ExportQuestionRequest.newBuilder().setSize(100).setCategoryId(1).setKeyword("测试问题1－1").build();

		AdminQAProtos.ExportQuestionResponse response = adminQAService.exportQuestion(adminHead, request).get();
		assertTrue(response.getQuestionCount() > 0);
	}
}
