package com.weizhu.service.survey.test;

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
import com.weizhu.proto.AdminExamProtos.CreateExamRequest;
import com.weizhu.proto.AdminProtos.AdminHead;
import com.weizhu.proto.ExamProtos.ShowResult;
import com.weizhu.proto.SurveyProtos;
import com.weizhu.proto.SurveyProtos.CopySurveyRequest;
import com.weizhu.proto.SurveyProtos.CopySurveyResponse;
import com.weizhu.proto.SurveyProtos.CreateSurveyRequest;
import com.weizhu.proto.SurveyProtos.GetClosedSurveyRequest;
import com.weizhu.proto.SurveyProtos.GetClosedSurveyResponse;
import com.weizhu.proto.SurveyProtos.GetOpenSurveyRequest;
import com.weizhu.proto.SurveyProtos.GetOpenSurveyResponse;
import com.weizhu.proto.SurveyProtos.GetSurveyByIdRequest;
import com.weizhu.proto.SurveyProtos.GetSurveyByIdResponse;
import com.weizhu.proto.SurveyProtos.GetSurveyResultRequest;
import com.weizhu.proto.SurveyProtos.GetSurveyResultResponse;
import com.weizhu.proto.SurveyProtos.ShowResultType;
import com.weizhu.proto.SurveyProtos.SubmitSurveyRequest;
import com.weizhu.proto.SurveyProtos.SubmitSurveyResponse;
import com.weizhu.proto.SurveyProtos.UpdateSurveyRequest;
import com.weizhu.proto.SurveyProtos.UpdateSurveyResponse;
import com.weizhu.proto.SurveyService;
import com.weizhu.proto.WeizhuProtos.RequestHead;
import com.weizhu.service.allow.AllowServiceModule;
import com.weizhu.service.official.OfficialServiceModule;
import com.weizhu.service.survey.SurveyDAOProtos;
import com.weizhu.service.survey.SurveyServiceModule;
import com.weizhu.service.user.UserServiceModule;
import com.weizhu.service.user.test.UserServiceTestModule;

@SuppressWarnings("unused")
public class SurveyServiceTest {
	static {
		System.setProperty("logback.configurationFile", "com/weizhu/service/survey/test/logback.xml");
	}
	
	private static final Injector INJECTOR = Guice.createInjector(
			new TestModule(), 
			new SurveyServiceTestModule(), new SurveyServiceModule(),
			new UserServiceTestModule(), new UserServiceModule(),
			new FakePushServiceModule(), new FakeProfileServiceModule(), new FakeExternalServiceModule(), new AllowServiceModule(),
			new OfficialServiceModule());
	
	@BeforeClass
	public static void init() throws Exception {
		TestUtil.clearCache(INJECTOR);
		TestUtil.loadTestDataDB(INJECTOR);
	}
	
	private final AdminHead adminHead;
	private final RequestHead requestHead;
	private final SurveyService surveyService;
	
	public SurveyServiceTest() {
		this.adminHead = INJECTOR.getInstance(AdminHead.class);
		this.requestHead = INJECTOR.getInstance(RequestHead.class);
		this.surveyService = INJECTOR.getInstance(SurveyService.class);
	}
	
	@Test
	public void testGetOpenSurvey() {
		GetOpenSurveyRequest getOpenSurveyRequest = GetOpenSurveyRequest.newBuilder()
				.setSize(10)
				.build();
		
		GetOpenSurveyResponse getOpenSurveyResponse = Futures.getUnchecked(surveyService.getOpenSurvey(requestHead, getOpenSurveyRequest));
		
		assertTrue(getOpenSurveyResponse.getSurveyCount() > 0);
		// System.out.println(com.google.protobuf.TextFormat.printToString(getOpenSurveyResponse));
	}
	
	@Test
	public void testGetOpenSurveyIndexList() {
		GetOpenSurveyRequest getOpenSurveyRequest = GetOpenSurveyRequest.newBuilder()
				.setOffsetIndex(SurveyDAOProtos.SurveyListIndex.newBuilder()
						.setTime(1)
						.setSurveyId(1)
						.build().toByteString())
				.setSize(10)
				.build();
		
		GetOpenSurveyResponse getOpenSurveyResponse = Futures.getUnchecked(surveyService.getOpenSurvey(requestHead, getOpenSurveyRequest));
		
		assertTrue(getOpenSurveyResponse.getSurveyCount() > 0);
		// System.out.println(com.google.protobuf.TextFormat.printToString(getOpenSurveyResponse));
				
	}
	
	@Test
	public void testGetClosedSurvey() {
		GetClosedSurveyRequest getClosedSurveyRequest = GetClosedSurveyRequest.newBuilder()
				.setSize(10)
				.build();
		
		GetClosedSurveyResponse getClosedSurveyResponse = Futures.getUnchecked(surveyService.getClosedSurvey(requestHead, getClosedSurveyRequest));
		
		assertTrue(getClosedSurveyResponse.getSurveyCount() > 0);
		// System.out.println("close --" + com.google.protobuf.TextFormat.printToString(getClosedSurveyResponse));
	}
	
	@Test
	public void testGetClosedSurveyIndexList() {
		GetClosedSurveyRequest getOpenSurveyRequest = GetClosedSurveyRequest.newBuilder()
				.setOffsetIndex(SurveyDAOProtos.SurveyListIndex.newBuilder()
						.setTime(1453516518)
						.setSurveyId(0)
						.build().toByteString())
				.setSize(10)
				.build();
		
		GetClosedSurveyResponse getClosedSurveyResponse = Futures.getUnchecked(surveyService.getClosedSurvey(requestHead, getOpenSurveyRequest));
		
		assertTrue(getClosedSurveyResponse.getSurveyCount() > 0);
		// System.out.println("closed survey" + com.google.protobuf.TextFormat.printToString(getClosedSurveyResponse));
				
	}
	
	@Test
	public void testGetSurveyById() {
		GetSurveyByIdRequest getSurveyByIdRequest = GetSurveyByIdRequest.newBuilder()
				.setSurveyId(1)
				.build();
		
		GetSurveyByIdResponse getSurveyByIdResponse = Futures.getUnchecked(surveyService.getSurveyById(requestHead, getSurveyByIdRequest));
		
		assertTrue(getSurveyByIdResponse.getSurvey() != null);
		// System.out.println(com.google.protobuf.TextFormat.printToString(getSurveyByIdResponse));
	}
	
	@Test
	public void testSubmitSurvey() {
		SurveyProtos.Vote.Answer voteAnswer = SurveyProtos.Vote.Answer.newBuilder()
				.addOptionId(1)
				.build();
		SurveyProtos.InputSelect.Answer inputSelectAnswer = SurveyProtos.InputSelect.Answer.newBuilder()
				.setOptionId(1)
				.build();
		SurveyProtos.InputText.Answer inputTextAnswer = SurveyProtos.InputText.Answer.newBuilder()
				.setResultText("这就是我的答案")
				.build();
		
		int now = (int) (System.currentTimeMillis() / 1000L);
		
		SurveyProtos.Answer answer1 = SurveyProtos.Answer.newBuilder()
				.setUserId(10000124196L)
				.setQuestionId(1)
				.setAnswerTime(now)
				.setVote(voteAnswer)
				.build();
		SurveyProtos.Answer answer2 = SurveyProtos.Answer.newBuilder()
				.setUserId(10000124196L)
				.setQuestionId(2)
				.setAnswerTime(now)
				.setInputSelect(inputSelectAnswer)
				.build();
		SurveyProtos.Answer answer3 = SurveyProtos.Answer.newBuilder()
				.setUserId(10000124196L)
				.setQuestionId(3)
				.setAnswerTime(now)
				.setInputText(inputTextAnswer)
				.build();
		
		SubmitSurveyRequest submitSurveyRequest = SubmitSurveyRequest.newBuilder()
				.addAnswer(answer1)
				.addAnswer(answer2)
				.addAnswer(answer3)
				.setSurveyId(1)
				.build();
		
		SubmitSurveyResponse submitSurveyResponse = Futures.getUnchecked(surveyService.submitSurvey(requestHead, submitSurveyRequest));
		
		// System.out.println("submit survey info ... " + com.google.protobuf.TextFormat.printToUnicodeString(submitSurveyResponse));
	}
	
	@Test
	public void testGetSurveyResult() {
		GetSurveyResultRequest request = GetSurveyResultRequest.newBuilder()
				.setSize(10)
				.setSurveyId(1) 
				.build();
		GetSurveyResultResponse response = Futures.getUnchecked(surveyService.getSurveyResult(requestHead, request));
		// System.out.println("survey result info ... " + com.google.protobuf.TextFormat.printToUnicodeString(response));
	}
	
	@Test
	public void testCopySurvey() {
		CopySurveyRequest request = CopySurveyRequest.newBuilder()
				.setSurveyName("11111'111")
				.setStartTime(1111)
				.setEndTime(1111111)
				.setSurveyId(1)
				.build();
		CopySurveyResponse response = Futures.getUnchecked(surveyService.copySurvey(adminHead, request));
		// System.out.println("copy survey ..." + com.google.protobuf.TextFormat.printToUnicodeString(response));
	}
	
	@Test
	public void testUpdateSurvey() {
		UpdateSurveyRequest request = UpdateSurveyRequest.newBuilder()
				.setSurveyId(1)
				.setSurveyName("222333")
				.setImageName("123123123.png")
				.setSurveyDesc("sdfadfadfadf")
				.setStartTime(1111)
				.setEndTime(22222)
				.setShowResultType(SurveyProtos.ShowResultType.AFTER_SUBMIT_COUNT)
				.build();
		UpdateSurveyResponse response = Futures.getUnchecked(surveyService.updateSurvey(adminHead, request));
		// System.out.println("update ... " + com.google.protobuf.TextFormat.printToUnicodeString(response));
	}
	
	@Test
	public void testCreateSurvey() {
		Futures.getUnchecked(surveyService.createSurvey(adminHead, CreateSurveyRequest.newBuilder()
				.setAllowModelId(17)
				.setSurveyName("yyyyyyyyyyyyyyyy")
				.setSurveyDesc("mmmmmmmmmm")
				.setImageName("")
				.setShowResultType(ShowResultType.AFTER_SUBMIT_COUNT)
				.setStartTime(1454405694)
				.setEndTime(1454664894)
				.build()));
	}
}
