package com.weizhu.service.exam.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import com.google.common.util.concurrent.Futures;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.weizhu.common.module.FakeExternalServiceModule;
import com.weizhu.common.module.FakeProfileServiceModule;
import com.weizhu.common.module.FakePushServiceModule;
import com.weizhu.common.module.TestModule;
import com.weizhu.common.utils.TestUtil;
import com.weizhu.proto.ExamProtos;
import com.weizhu.proto.ExamProtos.GetClosedExamListRequest;
import com.weizhu.proto.ExamProtos.GetClosedExamListResponse;
import com.weizhu.proto.ExamProtos.GetExamInfoRequest;
import com.weizhu.proto.ExamProtos.GetExamInfoResponse;
import com.weizhu.proto.ExamProtos.GetOpenExamListRequest;
import com.weizhu.proto.ExamProtos.GetOpenExamListResponse;
import com.weizhu.proto.ExamProtos.Question;
import com.weizhu.proto.ExamProtos.SaveAnswerRequest;
import com.weizhu.proto.ExamProtos.SaveAnswerResponse;
import com.weizhu.proto.ExamProtos.SubmitExamRequest;
import com.weizhu.proto.ExamProtos.SubmitExamResponse;
import com.weizhu.proto.ExamProtos.UserAnswer;
import com.weizhu.proto.ExamService;
import com.weizhu.proto.WeizhuProtos.RequestHead;
import com.weizhu.proto.WeizhuProtos.Session;
import com.weizhu.service.allow.AllowServiceModule;
import com.weizhu.service.exam.ExamServiceModule;
import com.weizhu.service.official.OfficialServiceModule;
import com.weizhu.service.user.UserServiceModule;
import com.weizhu.service.user.test.UserServiceTestModule;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class ExamServiceTest {
	
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
	
	private final RequestHead requestHead;
	private final ExamService examService;
	
	public ExamServiceTest() {
		this.requestHead = INJECTOR.getInstance(RequestHead.class);
		this.examService = INJECTOR.getInstance(ExamService.class);
	}

	//@Test
	@SuppressWarnings("unused")
	public void getExamInfo() throws Exception {
		// 已经结束的考试信息
		GetExamInfoRequest  request = GetExamInfoRequest.newBuilder()
				.setExamId(123)
				.build();
		
		GetExamInfoResponse response = examService.getExamInfo(requestHead, request).get();
		
		List<Question> questionList = response.getQuestionList();
		assertEquals(GetExamInfoResponse.ExamState.EXAM_FINISH, response.getState());

		// 没有开始的考试信息
		request = GetExamInfoRequest.newBuilder()
				.setExamId(1234568)
				.build();
		
		response = examService.getExamInfo(requestHead, request).get();
		assertEquals(GetExamInfoResponse.ExamState.EXAM_NOT_START, response.getState());
		
		// 正常考试的考试
		request = GetExamInfoRequest.newBuilder()
				.setExamId(1234)
				.build();

		response = examService.getExamInfo(requestHead, request).get();
	}
	
	//@Test
	// @org.junit.Ignore
	@SuppressWarnings("unused")
	public void atestFullExam() throws Exception {
		// 在openExam里面应该可以看到这条记录
		GetOpenExamListRequest openExamRequest = GetOpenExamListRequest.newBuilder()
				.setLastExamEndTime(0)
				.setLastExamId(0)
				.setSize(10)
				.build();
		GetOpenExamListResponse openExamResponse = examService.getOpenExamList(requestHead, openExamRequest).get();
		List<Integer> examIDList = new ArrayList<Integer>(10);
		for (ExamProtos.Exam exam : openExamResponse.getExamList()) {
			examIDList.add(exam.getExamId());
		}
		assertTrue(examIDList.contains(123456));
		// 测试全部考试获取这个考试的信息(考试还没有答题的时候)
		GetExamInfoRequest requestFullTest = GetExamInfoRequest.newBuilder()
				.setExamId(123456)
				.build();
		GetExamInfoResponse responseFullTest = examService.getExamInfo(requestHead, requestFullTest).get();
		// 保存部分考试 还没交卷 
		List<ExamProtos.UserAnswer> answerList = new ArrayList<ExamProtos.UserAnswer>();
		int n = 1;
		for (int j = 1; j < 4; j++) {
			answerList.add(ExamProtos.UserAnswer.newBuilder()
					.setQuestionId(j)
					.addAnswerOptionId(n)
					.build()
			);
			n = n + 4;
		}
		SaveAnswerResponse answeResponse = examService.saveAnswer(requestHead, 
				SaveAnswerRequest.newBuilder()
				.addAllUserAnswer(answerList)
				.setExamId(123456)
				.build())
			.get();
		assertEquals(SaveAnswerResponse.Result.SUCC, answeResponse.getResult());
		// 测试全部考试获取这个考试的信息(考试已经答题之后的)
		GetExamInfoResponse reponseExamInfoContainsAnswer = examService.getExamInfo(requestHead, GetExamInfoRequest.newBuilder()
				.setExamId(123456)
				.build()).get();
		
		// 交卷
		List<ExamProtos.UserAnswer> answerListJJ = new ArrayList<ExamProtos.UserAnswer>();
		int a = 1;
		for (int j = 1; j < 4; j++) {
			answerListJJ.add(ExamProtos.UserAnswer.newBuilder()
					.setQuestionId(j)
					.addAnswerOptionId(a)
					.build()
			);
			a = a + 4;
		}
		SubmitExamResponse submitResponse = examService.submitExam(requestHead, 
				SubmitExamRequest.newBuilder()
				.addAllUserAnswer(answerListJJ)
				.setExamId(123456)
				.build())
			.get();
	}
	
	//@Test
	// @org.junit.Ignore
	public void saveAnswer() throws Exception {
		// 回答没在进行中的考试
		SaveAnswerRequest.Builder request = SaveAnswerRequest.newBuilder();
		UserAnswer.Builder answer = UserAnswer.newBuilder();
		for(int i=1; i<3; i++){
			answer.addAnswerOptionId(i);
		}
		answer.setQuestionId(123);
		request.setExamId(123);
		SaveAnswerResponse response = examService.saveAnswer(requestHead, request.addUserAnswer(answer.build()).build()).get();
		
		assertEquals(SaveAnswerResponse.Result.FAIL_EXAM_CLOSED, response.getResult());
		
		// 回答进行中,但是考题不对的考试
		request = SaveAnswerRequest.newBuilder();
		answer = UserAnswer.newBuilder();
		for (int i = 25; i < 3; i++) {
			answer.addAnswerOptionId(i);
		}
		answer.setQuestionId(123);
		request.setExamId(12345);
		response = examService.saveAnswer(requestHead, request.addUserAnswer(answer.build()).build()).get();

		assertEquals(SaveAnswerResponse.Result.SUCC, response.getResult());
	}
	
	@Test
	public void submitExam() throws Exception {
		//回答100分交卷
		SubmitExamRequest.Builder request = SubmitExamRequest.newBuilder();
		UserAnswer.Builder answer = UserAnswer.newBuilder();
		answer.addAnswerOptionId(25);
		answer.setQuestionId(123);
		
		SubmitExamResponse response = examService.submitExam(requestHead, request.setExamId(1234567).addUserAnswer(answer.build()).build()).get();
		assertTrue(response.getResult().toString().equals("SUCC"));
		
		// 回答0分交卷
		request = SubmitExamRequest.newBuilder();
		answer = UserAnswer.newBuilder();
		answer.addAnswerOptionId(27);
		answer.setQuestionId(123);

		response = examService.submitExam(requestHead,
				request.setExamId(1224).addUserAnswer(answer.build()).build()).get();

		assertTrue(response.getResult().toString().equals("SUCC"));
	}
	
	//@Test
	// @org.junit.Ignore
	public void getOpenExamList() throws Exception {
		// 翻一页
		GetOpenExamListRequest request = GetOpenExamListRequest.newBuilder()
											.setLastExamId(0)
											.setLastExamEndTime(0)
											.setSize(1)
											.build();

		GetOpenExamListResponse response = examService.getOpenExamList(requestHead, request).get();
		
		ExamProtos.Exam exam = response.getExamList().get(response.getExamCount() - 1);
		GetOpenExamListRequest openExamRequest = GetOpenExamListRequest.newBuilder()
				.setLastExamId(exam.getExamId())
				.setLastExamEndTime(exam.getEndTime())
				.setSize(3)
				.build();
		GetOpenExamListResponse openExamResponse = examService.getOpenExamList(requestHead, openExamRequest).get();
		
		assertTrue(!response.getExamList().get(response.getExamCount() - 1).equals(openExamResponse.getExamList().get(openExamResponse.getExamCount() - 1)));
	}
	
	//@Test
	@org.junit.Ignore
	public void getClosedExamList() throws Exception {
		
		GetClosedExamListRequest request = GetClosedExamListRequest.newBuilder()
											.setLastExamId(0)
											.setLastExamSubmitTime(0)
											.setSize(3)
											.build();

		GetClosedExamListResponse response = examService.getClosedExamList(requestHead, request).get();

		assertTrue(response.getExamCount() > 0);	
	}
	
	//@Test
	public void testFinish() throws Exception {
		
		GetExamInfoRequest request = GetExamInfoRequest.newBuilder()
				.setExamId(111111).build();

		RequestHead webHead = requestHead.toBuilder()
				.setSession(Session.newBuilder()
						.setCompanyId(0)
						.setUserId(10000124207L).setSessionId(0))
				.build();
		GetExamInfoResponse examInfoResponse = examService.getExamInfo(webHead, request).get();
		assertEquals(GetExamInfoResponse.ExamState.EXAM_FINISH, examInfoResponse.getState());
	}
	
	//@Test
	public void testNotStart() throws Exception {
		GetExamInfoRequest request = GetExamInfoRequest.newBuilder()
				.setExamId(111112).build();

		RequestHead webHead = requestHead.toBuilder()
				.setSession(Session.newBuilder()
						.setCompanyId(0)
						.setUserId(10000124207L).setSessionId(0))
				.build();
		GetExamInfoResponse examInfoResponse = examService.getExamInfo(webHead, request).get();
		assertEquals(GetExamInfoResponse.ExamState.EXAM_NOT_START, examInfoResponse.getState());
	}
	
	//@Test
	public void testExamRunning() throws Exception {
		GetExamInfoRequest request = GetExamInfoRequest.newBuilder()
				.setExamId(111113).build();

		RequestHead webHead = requestHead.toBuilder()
				.setSession(Session.newBuilder()
						.setCompanyId(0)
						.setUserId(10000124207L).setSessionId(0))
				.build();
		GetExamInfoResponse examInfoResponse = examService.getExamInfo(webHead, request).get();
		assertEquals(GetExamInfoResponse.ExamState.EXAM_RUNNING, examInfoResponse.getState());
	}
	
	//@Test
	public void testFinishUserNotInExam() throws Exception {
		
		GetExamInfoRequest request = GetExamInfoRequest.newBuilder()
				.setExamId(111111).build();

		RequestHead webHead = requestHead.toBuilder()
				.setSession(Session.newBuilder()
						.setCompanyId(0)
						.setUserId(10000124196L).setSessionId(0))
				.build();
		GetExamInfoResponse examInfoResponse = examService.getExamInfo(webHead, request).get();
		assertEquals(GetExamInfoResponse.ExamState.EXAM_FINISH, examInfoResponse.getState());
	}
	
	//@Test
	public void testNotStartUserNotInExam() throws Exception {
		GetExamInfoRequest request = GetExamInfoRequest.newBuilder()
				.setExamId(111112).build();

		RequestHead webHead = requestHead.toBuilder()
				.setSession(Session.newBuilder()
						.setCompanyId(0)
						.setUserId(10000124196L).setSessionId(0))
				.build();
		GetExamInfoResponse examInfoResponse = examService.getExamInfo(webHead, request).get();
		assertEquals(GetExamInfoResponse.ExamState.EXAM_NOT_START, examInfoResponse.getState());
	}
	
	//@Test
	public void testExamRunningUserNotInExam() throws Exception {
		GetExamInfoRequest request = GetExamInfoRequest.newBuilder()
				.setExamId(111113).build();

		RequestHead webHead = requestHead.toBuilder()
				.setSession(Session.newBuilder()
						.setCompanyId(0)
						.setUserId(10000124196L).setSessionId(0))
				.build();
		GetExamInfoResponse examInfoResponse = examService.getExamInfo(webHead, request).get();
		assertEquals(GetExamInfoResponse.ExamState.EXAM_RUNNING, examInfoResponse.getState());
	}
	
	//@Test
	public void testSaveExamAnswerSucc() throws Exception {
		ExamProtos.UserAnswer userAnswer = ExamProtos.UserAnswer.newBuilder()
				.setQuestionId(1)
				.addAnswerOptionId(1)
				.build();
		
		SaveAnswerRequest request = SaveAnswerRequest.newBuilder()
				.setExamId(111113)
				.addUserAnswer(userAnswer)
				.build();

		RequestHead webHead = requestHead.toBuilder()
				.setSession(Session.newBuilder()
						.setCompanyId(0)
						.setUserId(10000124207L).setSessionId(0))
				.build();
		SaveAnswerResponse saveAnswerResponse = examService.saveAnswer(webHead, request).get();
		assertEquals(SaveAnswerResponse.Result.SUCC, saveAnswerResponse.getResult());
	}
	
	//@Test
	public void testSaveExamAnswerErrNoQu() throws Exception {
		ExamProtos.UserAnswer userAnswer = ExamProtos.UserAnswer.newBuilder()
				.setQuestionId(100)
				.addAnswerOptionId(1)
				.build();
		
		SaveAnswerRequest request = SaveAnswerRequest.newBuilder()
				.setExamId(111113)
				.addUserAnswer(userAnswer)
				.build();

		RequestHead webHead = requestHead.toBuilder()
				.setSession(Session.newBuilder()
						.setCompanyId(0)
						.setUserId(10000124207L).setSessionId(0))
				.build();
		SaveAnswerResponse saveAnswerResponse = examService.saveAnswer(webHead, request).get();
		assertEquals(SaveAnswerResponse.Result.FAIL_ANSWER_INVALID, saveAnswerResponse.getResult());
	}
	
	//@Test
	public void testSaveExamAnswerErrNoOp() throws Exception {
		
		ExamProtos.UserAnswer userAnswer = ExamProtos.UserAnswer.newBuilder()
				.setQuestionId(1)
				.addAnswerOptionId(5)
				.build();
		
		SaveAnswerRequest request = SaveAnswerRequest.newBuilder()
				.setExamId(111113)
				.addUserAnswer(userAnswer)
				.build();

		RequestHead webHead = requestHead.toBuilder()
				.setSession(Session.newBuilder()
						.setCompanyId(0)
						.setUserId(10000124207L).setSessionId(0))
				.build();
		SaveAnswerResponse saveAnswerResponse = examService.saveAnswer(webHead, request).get();
		assertEquals(SaveAnswerResponse.Result.FAIL_ANSWER_INVALID, saveAnswerResponse.getResult());
	}
	
	//@Test
	@org.junit.Ignore
	public void testSaveExamAnswerErrNoJoin() throws Exception {
		ExamProtos.UserAnswer userAnswer = ExamProtos.UserAnswer.newBuilder()
				.setQuestionId(1)
				.addAnswerOptionId(5)
				.build();
		
		SaveAnswerRequest request = SaveAnswerRequest.newBuilder()
				.setExamId(111113)
				.addUserAnswer(userAnswer)
				.build();

		RequestHead webHead = requestHead.toBuilder()
				.setSession(Session.newBuilder()
						.setCompanyId(0)
						.setUserId(10000124196L).setSessionId(0))
				.build();
		SaveAnswerResponse saveAnswerResponse = examService.saveAnswer(webHead, request).get();
		assertEquals(SaveAnswerResponse.Result.FAIL_EXAM_NOT_JOIN, saveAnswerResponse.getResult());
	}
	
	//@Test
	public void testSaveExamAnswerErrExClose() throws Exception {
		ExamProtos.UserAnswer userAnswer = ExamProtos.UserAnswer.newBuilder()
				.setQuestionId(1)
				.addAnswerOptionId(5)
				.build();
		
		SaveAnswerRequest request = SaveAnswerRequest.newBuilder()
				.setExamId(111112)
				.addUserAnswer(userAnswer)
				.build();

		RequestHead webHead = requestHead.toBuilder()
				.setSession(Session.newBuilder()
						.setCompanyId(0)
						.setUserId(10000124207L).setSessionId(0))
				.build();
		SaveAnswerResponse saveAnswerResponse = examService.saveAnswer(webHead, request).get();
		assertEquals(SaveAnswerResponse.Result.FAIL_EXAM_CLOSED, saveAnswerResponse.getResult());
	}
	
	//@Test
	public void testSubmitExamMulti() throws Exception {
		ExamProtos.UserAnswer userAnswer = ExamProtos.UserAnswer.newBuilder()
				.setQuestionId(1)
				.addAnswerOptionId(1)
				.addAnswerOptionId(1)
				.build();
		
		ExamProtos.UserAnswer userAnswer1 = ExamProtos.UserAnswer.newBuilder()
				.setQuestionId(1)
				.addAnswerOptionId(1)
				.addAnswerOptionId(1)
				.build();
		
		SubmitExamRequest request = SubmitExamRequest.newBuilder()
				.setExamId(111113)
				.addUserAnswer(userAnswer)
				.addUserAnswer(userAnswer1)
				.build();
		
		RequestHead webHead = requestHead.toBuilder()
				.setSession(Session.newBuilder()
						.setCompanyId(0)
						.setUserId(10000124207L)
						.setSessionId(0))
				.build();
		
		SubmitExamResponse submitExamResponse = examService.submitExam(webHead, request).get();
		assertEquals(10, submitExamResponse.getUserScore());
	}
	
	//@Test
	public void testGetExamInfo() {
		GetExamInfoRequest getExamInfo = GetExamInfoRequest.newBuilder()
				.setExamId(666)
				.build();
		
		RequestHead webHead = requestHead.toBuilder()
				.setSession(Session.newBuilder()
						.setCompanyId(0)
						.setUserId(10000124207L)
						.setSessionId(0))
				.build();
		GetExamInfoResponse response = Futures.getUnchecked(examService.getExamInfo(webHead, getExamInfo));
		assertEquals(666, response.getExam().getExamId());
	}
	//@Test
	public void testGetExamInfo1() {
		GetExamInfoRequest getExamInfo = GetExamInfoRequest.newBuilder()
				.setExamId(667)
				.build();
		
		RequestHead webHead = requestHead.toBuilder()
				.setSession(Session.newBuilder()
						.setCompanyId(0)
						.setUserId(10000124207L)
						.setSessionId(0))
				.build();
		GetExamInfoResponse response = Futures.getUnchecked(examService.getExamInfo(webHead, getExamInfo));
		assertEquals(667, response.getExam().getExamId());
	}
	//@Test
	public void testGetExamInfo2() {
		GetExamInfoRequest getExamInfo = GetExamInfoRequest.newBuilder()
				.setExamId(668)
				.build();
		
		RequestHead webHead = requestHead.toBuilder()
				.setSession(Session.newBuilder()
						.setCompanyId(0)
						.setUserId(10000124207L)
						.setSessionId(0))
				.build();
		GetExamInfoResponse response = Futures.getUnchecked(examService.getExamInfo(webHead, getExamInfo));
		assertEquals(668, response.getExam().getExamId());
	}
	//@Test
	public void testGetExamInfo3() {
		GetExamInfoRequest getExamInfo = GetExamInfoRequest.newBuilder()
				.setExamId(1234569)
				.build();
		
		RequestHead webHead = requestHead.toBuilder()
				.setSession(Session.newBuilder()
						.setCompanyId(0)
						.setUserId(10000124207L)
						.setSessionId(0))
				.build();
		GetExamInfoResponse response = Futures.getUnchecked(examService.getExamInfo(webHead, getExamInfo));
		assertEquals(1234569, response.getExam().getExamId());
	}
}

