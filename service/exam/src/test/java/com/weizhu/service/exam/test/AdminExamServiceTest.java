package com.weizhu.service.exam.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutionException;

import org.junit.BeforeClass;
import org.junit.Test;

import com.google.common.util.concurrent.Futures;
import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.weizhu.common.module.FakeExternalServiceModule;
import com.weizhu.common.module.FakeProfileServiceModule;
import com.weizhu.common.module.FakePushServiceModule;
import com.weizhu.common.module.TestModule;
import com.weizhu.common.utils.TestUtil;
import com.weizhu.proto.AdminExamProtos.CreateExamRequest;
import com.weizhu.proto.AdminExamProtos.CreateExamResponse;
import com.weizhu.proto.AdminExamProtos.CreateQuestionRequest;
import com.weizhu.proto.AdminExamProtos.CreateQuestionResponse;
import com.weizhu.proto.AdminExamProtos.GetExamStatisticsRequest;
import com.weizhu.proto.AdminExamProtos.GetExamStatisticsResponse;
import com.weizhu.proto.AdminExamProtos.GetPositionStatisticsRequest;
import com.weizhu.proto.AdminExamProtos.GetPositionStatisticsResponse;
import com.weizhu.proto.AdminExamProtos.GetQuestionCorrectRateRequest;
import com.weizhu.proto.AdminExamProtos.GetQuestionCorrectRateResponse;
import com.weizhu.proto.AdminExamProtos.GetTeamStatisticsRequest;
import com.weizhu.proto.AdminExamProtos.GetTeamStatisticsResponse;
import com.weizhu.proto.AdminExamProtos.GetUserAnswerRequest;
import com.weizhu.proto.AdminExamProtos.GetUserAnswerResponse;
import com.weizhu.proto.AdminExamProtos.UpdateExamQuestionRandomRequest;
import com.weizhu.proto.AdminExamService;
import com.weizhu.proto.AdminProtos.AdminHead;
import com.weizhu.proto.ExamProtos;
import com.weizhu.proto.ExamProtos.Option;
import com.weizhu.proto.ExamProtos.ShowResult;
import com.weizhu.proto.WeizhuProtos.EmptyRequest;
import com.weizhu.service.allow.AllowServiceModule;
import com.weizhu.service.exam.ExamServiceModule;
import com.weizhu.service.official.OfficialServiceModule;
import com.weizhu.service.user.UserServiceModule;
import com.weizhu.service.user.test.UserServiceTestModule;

public class AdminExamServiceTest {
	
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
	
	private final AdminHead adminHead;
	private final AdminExamService adminExamService;
	
	@Inject
	public AdminExamServiceTest() {
		this.adminHead = INJECTOR.getInstance(AdminHead.class);
		this.adminExamService = INJECTOR.getInstance(AdminExamService.class);
	}
	
	@Test
	public void createQuestion() throws InterruptedException, ExecutionException {
		ExamProtos.Option option1 = ExamProtos.Option.newBuilder()
				.setOptionId(0)
				.setOptionName("语文")
				.setIsRight(true)
				.build();
		ExamProtos.Option option2 = ExamProtos.Option.newBuilder()
				.setOptionId(0)
				.setOptionName("数学")
				.setIsRight(true)
				.build();
		ExamProtos.Option option3 = ExamProtos.Option.newBuilder()
				.setOptionId(0)
				.setOptionName("英语")
				.setIsRight(true)
				.build();
		List<ExamProtos.Option> optionList = new ArrayList<ExamProtos.Option>();
		optionList.add(option1);
		optionList.add(option2);
		optionList.add(option3);
		
		CreateQuestionRequest createQuestion = CreateQuestionRequest.newBuilder()
				.setQuestionName("考试？")
				.setType(ExamProtos.Question.Type.OPTION_MULTI)
				.addAllOption(optionList)
				.build();
		
		CreateQuestionResponse response = adminExamService.createQuestion(adminHead, createQuestion).get();
		assertEquals(CreateQuestionResponse.Result.SUCC, response.getResult());
	}
	
	@Test
	public void testGetQuestionCategory() {
		Futures.getUnchecked(adminExamService.getQuestionCategory(adminHead, EmptyRequest.getDefaultInstance()));
	}
	
	@Test
	public void testCreateExam() {
		Futures.getUnchecked(adminExamService.createExam(adminHead, CreateExamRequest.newBuilder()
				.setAllowModelId(17)
				.setExamName("yyyyyyyyyyyyyyyy")
				.setStartTime(1454405694)
				.setEndTime(1454664894)
				.setShowResult(ShowResult.AFTER_EXAM_END)
				.setPassMark(60)
				.setType(ExamProtos.Exam.Type.AUTO)
				.build()));
	}
	
	@Test
	public void testCreateTFQuestion() {
		adminExamService.createQuestion(adminHead, CreateQuestionRequest.newBuilder()
				.setCategoryId(1)
				.setQuestionName("判断题")
				.addAllOption(Arrays.asList(Option.newBuilder()
						.setIsRight(true)
						.setOptionName("对")
						.setOptionId(0)
						.build(), Option.newBuilder()
						.setIsRight(false)
						.setOptionName("错")
						.setOptionId(0)
						.build()))
				.setType(ExamProtos.Question.Type.OPTION_TF)
				.build());
	}
	
	@Test
	public void testCreateExam1() {
		CreateExamResponse response = Futures.getUnchecked(adminExamService.createExam(adminHead, CreateExamRequest.newBuilder()
				.setExamName("混合类型考题")
				.setStartTime(1454405694)
				.setEndTime(1654664894)
				.setShowResult(ShowResult.AFTER_EXAM_END)
				.setPassMark(60)
				.setType(ExamProtos.Exam.Type.AUTO)
				.build()));
		int examId = response.getExamId();
		
		Futures.getUnchecked(adminExamService.updateExamQuestionRandom(adminHead, UpdateExamQuestionRandomRequest.newBuilder()
				.addAllCategoryId(Arrays.asList(1,2))
				.setQuestionNum(4)
				.setExamId(examId)
				.build()));
	}
	
	@Test
	public void testGetExamStatistics() {
		GetExamStatisticsResponse response = Futures.getUnchecked(adminExamService.getExamStatistics(adminHead, GetExamStatisticsRequest.newBuilder()
				.addExamId(10001)
				.build()));
		assertTrue(response.getExamStatisticsCount() > 0);
		//System.out.println(TextFormat.printToUnicodeString(response));
	}
	
	@Test
	public void testGetStatisticsTeam() {
		GetTeamStatisticsResponse response = Futures.getUnchecked(adminExamService.getTeamStatistics(adminHead, GetTeamStatisticsRequest.newBuilder()
				.setExamId(10001)
				.setStart(0)
				.setLength(10)
				.build()));
		assertTrue(response.getTeamStatisticsCount() > 0);
		//System.out.println(TextFormat.printToUnicodeString(response));
	}
	
	@Test
	public void testGetStatisticsTeam1() {
		GetTeamStatisticsResponse response = Futures.getUnchecked(adminExamService.getTeamStatistics(adminHead, GetTeamStatisticsRequest.newBuilder()
				.setExamId(10001)
				.setStart(0)
				.setLength(10)
				.setTeamId("1,2")
				.build()));
		assertTrue(response.getTeamStatisticsCount() > 0);
		//System.out.println(TextFormat.printToUnicodeString(response));
	}
	
	@Test
	public void testGetStatisticsPosition() {
		GetPositionStatisticsResponse response = Futures.getUnchecked(adminExamService.getPositionStatistics(adminHead, GetPositionStatisticsRequest.newBuilder()
				.setExamId(10001)
				.setStart(0)
				.setLength(10)
				.build()));
//		System.out.println(TextFormat.printToUnicodeString(response));
		assertTrue(response.getPostionStatisticsCount() > 0);
	}
	
	@Test
	public void testGetQuestionCorrectRate() {
		GetQuestionCorrectRateResponse response = Futures.getUnchecked(adminExamService.getQuestionCorrectRate(adminHead, GetQuestionCorrectRateRequest.newBuilder()
				.setExamId(10001)
				.setStart(0)
				.setLength(10)
				.build()));
		assertTrue(response.getQuestionCorrectCount() > 0);
		//System.out.println(TextFormat.printToUnicodeString(response));
	}
	
	@Test
	public void testGetUserAnswer() {
		GetUserAnswerResponse response = Futures.getUnchecked(adminExamService.getUserAnswer(adminHead, GetUserAnswerRequest.newBuilder()
				.setExamId(10001)
				.setUserId(10000124207L)
				.build()));
		assertTrue(response.getUserAnswerCount() > 0);
		//System.out.println(TextFormat.printToUnicodeString(response));
	}
}
