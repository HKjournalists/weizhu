package com.weizhu.service.exam.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.BeforeClass;
import org.junit.Test;

import com.google.common.util.concurrent.Futures;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.weizhu.common.module.FakeExternalServiceModule;
import com.weizhu.common.module.FakeProfileServiceModule;
import com.weizhu.common.module.FakePushServiceModule;
import com.weizhu.common.module.TestModule;
import com.weizhu.common.utils.TestUtil;
import com.weizhu.proto.ExamProtos.GetExamInfoRequest;
import com.weizhu.proto.ExamProtos.GetExamInfoResponse;
import com.weizhu.proto.ExamService;
import com.weizhu.proto.WeizhuProtos.RequestHead;
import com.weizhu.proto.WeizhuProtos.Session;
import com.weizhu.service.allow.AllowServiceModule;
import com.weizhu.service.exam.ExamServiceModule;
import com.weizhu.service.official.OfficialServiceModule;
import com.weizhu.service.user.UserServiceModule;
import com.weizhu.service.user.test.UserServiceTestModule;

public class ExamServiceTest2 {

	static {
		System.setProperty("logback.configurationFile", "com/weizhu/service/exam/test/logback.xml");
	}
	
	private static final Injector INJECTOR = Guice.createInjector(
			new TestModule(), 
			new ExamServiceTestModule(), new ExamServiceModule(),
			new UserServiceTestModule(), new UserServiceModule(),
			new FakePushServiceModule(), new FakeProfileServiceModule(), new FakeExternalServiceModule(), 
			new AllowServiceModule(), new OfficialServiceModule());
	
	@BeforeClass
	public static void init() throws Exception {
		TestUtil.clearCache(INJECTOR);
		TestUtil.loadTestDataDB(INJECTOR);
	}
	
	private final RequestHead head;
	private final ExamService examService;
	
	public ExamServiceTest2() {
		this.head = INJECTOR.getInstance(RequestHead.class).toBuilder()
				.setSession(Session.newBuilder().setCompanyId(0).setUserId(10000124207L).setSessionId(0))
				.build();
		this.examService = INJECTOR.getInstance(ExamService.class);
	}
	
	@Test
	public void testGetExamInfoFinish() throws Exception {
		GetExamInfoRequest request = GetExamInfoRequest.newBuilder()
				.setExamId(10001)
				.build();
		
		GetExamInfoResponse response = this.examService.getExamInfo(head, request).get();
		
		// System.out.println(com.google.protobuf.TextFormat.printToUnicodeString(response));
		
		assertTrue(response.hasExam());
		assertEquals(GetExamInfoResponse.ExamState.EXAM_FINISH, response.getState());
		assertEquals(10, response.getQuestionCount());
		assertTrue(response.getQuestion(0).getOption(0).hasIsRight());
		assertEquals(9, response.getUserAnswerCount());
		assertTrue(response.getUserAnswer(0).hasIsRight());
	}
	
//	@Test
	public void testGetExamInfoNotStart() throws Exception {
		GetExamInfoRequest request = GetExamInfoRequest.newBuilder()
				.setExamId(10003)
				.build();
		
		GetExamInfoResponse response = this.examService.getExamInfo(head, request).get();
		
		assertTrue(response.hasExam());
		assertEquals(GetExamInfoResponse.ExamState.EXAM_NOT_START, response.getState());
		assertTrue(response.getQuestionCount() <= 0);
		assertTrue(response.getUserAnswerCount() <= 0);
		assertFalse(response.hasUserResult());
	}
	
//	@Test
	public void testGetExamInfoRunning()throws Exception {
		GetExamInfoRequest request = GetExamInfoRequest.newBuilder()
				.setExamId(10002)
				.build();
		
		GetExamInfoResponse response = this.examService.getExamInfo(head, request).get();
		
		// System.out.println(com.google.protobuf.TextFormat.printToUnicodeString(response));
		
		assertTrue(response.hasExam());
		assertEquals(GetExamInfoResponse.ExamState.EXAM_RUNNING, response.getState());
		assertEquals(10, response.getQuestionCount());
		assertFalse(response.getQuestion(0).getOption(0).hasIsRight());
		
		assertEquals(0, response.getUserAnswerCount());
		// assertFalse(response.hasUserResult());
	}
	
	@Test
	public void testExamShowResult() {
		Futures.getUnchecked(examService.getExamInfo(head, GetExamInfoRequest.newBuilder()
				.setExamId(10001)
				.build()));
	}
	
}
