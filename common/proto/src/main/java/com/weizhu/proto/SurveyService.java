package com.weizhu.proto;

import com.google.common.util.concurrent.ListenableFuture;
import com.weizhu.proto.AdminProtos.AdminHead;
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
import com.weizhu.proto.WeizhuProtos.EmptyRequest;
import com.weizhu.proto.WeizhuProtos.RequestHead;

public interface SurveyService {

	@ResponseType(GetOpenSurveyResponse.class)
	ListenableFuture<GetOpenSurveyResponse> getOpenSurvey(RequestHead head, GetOpenSurveyRequest request);
	
	@ResponseType(GetOpenSurveyCountResponse.class)
	ListenableFuture<GetOpenSurveyCountResponse> getOpenSurveyCount(RequestHead head, EmptyRequest request);
	
	@ResponseType(GetClosedSurveyResponse.class)
	ListenableFuture<GetClosedSurveyResponse> getClosedSurvey(RequestHead head, GetClosedSurveyRequest request);
	
	@ResponseType(GetSurveyByIdResponse.class)
	ListenableFuture<GetSurveyByIdResponse> getSurveyById(RequestHead head, GetSurveyByIdRequest request);
	
	@WriteMethod
	@ResponseType(SubmitSurveyResponse.class)
	ListenableFuture<SubmitSurveyResponse> submitSurvey(RequestHead head, SubmitSurveyRequest request);
	
	@ResponseType(GetSurveyResultResponse.class)
	ListenableFuture<GetSurveyResultResponse> getSurveyResult(RequestHead head, GetSurveyResultRequest request);
	
	@ResponseType(GetQuestionAnswerResponse.class)
	ListenableFuture<GetQuestionAnswerResponse> getQuestionAnswer(RequestHead head, GetQuestionAnswerRequest request);
	
	
	@WriteMethod
	@ResponseType(CreateSurveyResponse.class)
	ListenableFuture<CreateSurveyResponse> createSurvey(AdminHead head, CreateSurveyRequest request);
	
	@WriteMethod
	@ResponseType(UpdateSurveyResponse.class)
	ListenableFuture<UpdateSurveyResponse> updateSurvey(AdminHead head, UpdateSurveyRequest request);
	
	@WriteMethod
	@ResponseType(CreateQuestionResponse.class)
	ListenableFuture<CreateQuestionResponse> createQuestion(AdminHead head, CreateQuestionRequest request);
	
	@WriteMethod
	@ResponseType(UpdateQuestionResponse.class)
	ListenableFuture<UpdateQuestionResponse> updateQuestion(AdminHead head, UpdateQuestionRequest request);
	
	@WriteMethod
	@ResponseType(DeleteQuestionResponse.class)
	ListenableFuture<DeleteQuestionResponse> deleteQuestion(AdminHead head, DeleteQuestionRequest request);
	
	@WriteMethod
	@ResponseType(DisableSurveyResponse.class)
	ListenableFuture<DisableSurveyResponse> disableSurvey(AdminHead head, DisableSurveyRequest request);
	
	@WriteMethod
	@ResponseType(EnableSurveyResponse.class)
	ListenableFuture<EnableSurveyResponse> enableSurvey(AdminHead head, EnableSurveyRequest request);
	
	@WriteMethod
	@ResponseType(DeleteSurveyResponse.class)
	ListenableFuture<DeleteSurveyResponse> deleteSurvey(AdminHead head, DeleteSurveyRequest request);
	
	@ResponseType(GetSurveyByIdResponse.class)
	ListenableFuture<GetSurveyByIdResponse> getSurveyById(AdminHead head, GetSurveyByIdRequest request);
	
	@ResponseType(GetQuestionAnswerResponse.class)
	ListenableFuture<GetQuestionAnswerResponse> getQuestionAnswer(AdminHead head, GetQuestionAnswerRequest request);
	
	@ResponseType(GetSurveyListResponse.class)
	ListenableFuture<GetSurveyListResponse> getSurveyList(AdminHead head, GetSurveyListRequest request);
	
	@ResponseType(GetSurveyResultListResponse.class)
	ListenableFuture<GetSurveyResultListResponse> getSurveyResultList(AdminHead head, GetSurveyResultListRequest request);
	
	@WriteMethod
	@ResponseType(QuestionSortResponse.class)
	ListenableFuture<QuestionSortResponse> questionSort(AdminHead head, QuestionSortRequest request);
	
	@WriteMethod
	@ResponseType(ImportQuestionResponse.class)
	ListenableFuture<ImportQuestionResponse> importQuestion(AdminHead head, ImportQuestionRequest request);
	
	@WriteMethod
	@ResponseType(CopySurveyResponse.class)
	ListenableFuture<CopySurveyResponse> copySurvey(AdminHead head, CopySurveyRequest request);
}
