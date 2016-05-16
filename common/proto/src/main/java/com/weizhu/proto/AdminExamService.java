package com.weizhu.proto;

import com.google.common.util.concurrent.ListenableFuture;
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
import com.weizhu.proto.AdminExamProtos.GetExamUserResultRequest;
import com.weizhu.proto.AdminExamProtos.GetExamUserResultResponse;
import com.weizhu.proto.AdminExamProtos.GetPositionStatisticsRequest;
import com.weizhu.proto.AdminExamProtos.GetPositionStatisticsResponse;
import com.weizhu.proto.AdminExamProtos.GetQuestionByCategoryIdRequest;
import com.weizhu.proto.AdminExamProtos.GetQuestionByCategoryIdResponse;
import com.weizhu.proto.AdminExamProtos.GetQuestionCategoryResponse;
import com.weizhu.proto.AdminExamProtos.GetQuestionCorrectRateRequest;
import com.weizhu.proto.AdminExamProtos.GetQuestionCorrectRateResponse;
import com.weizhu.proto.AdminExamProtos.GetQuestionRequest;
import com.weizhu.proto.AdminExamProtos.GetQuestionResponse;
import com.weizhu.proto.AdminExamProtos.GetTeamStatisticsRequest;
import com.weizhu.proto.AdminExamProtos.GetTeamStatisticsResponse;
import com.weizhu.proto.AdminExamProtos.GetUserAnswerRequest;
import com.weizhu.proto.AdminExamProtos.GetUserAnswerResponse;
import com.weizhu.proto.AdminExamProtos.ImportQuestionRequest;
import com.weizhu.proto.AdminExamProtos.ImportQuestionResponse;
import com.weizhu.proto.AdminExamProtos.MoveQuestionCategoryRequest;
import com.weizhu.proto.AdminExamProtos.MoveQuestionCategoryResponse;
import com.weizhu.proto.AdminExamProtos.ReExamRequest;
import com.weizhu.proto.AdminExamProtos.ReExamResponse;
import com.weizhu.proto.AdminExamProtos.UpdateExamQuestionRandomRequest;
import com.weizhu.proto.AdminExamProtos.UpdateExamQuestionRandomResponse;
import com.weizhu.proto.AdminExamProtos.UpdateExamQuestionRequest;
import com.weizhu.proto.AdminExamProtos.UpdateExamQuestionResponse;
import com.weizhu.proto.AdminExamProtos.UpdateExamRequest;
import com.weizhu.proto.AdminExamProtos.UpdateExamResponse;
import com.weizhu.proto.AdminExamProtos.UpdateQuestionCategoryRequest;
import com.weizhu.proto.AdminExamProtos.UpdateQuestionCategoryResponse;
import com.weizhu.proto.AdminExamProtos.UpdateQuestionInQuestionCategoryRequest;
import com.weizhu.proto.AdminExamProtos.UpdateQuestionInQuestionCategoryResponse;
import com.weizhu.proto.AdminExamProtos.UpdateQuestionRequest;
import com.weizhu.proto.AdminExamProtos.UpdateQuestionResponse;
import com.weizhu.proto.AdminProtos.AdminHead;
import com.weizhu.proto.WeizhuProtos.EmptyRequest;
import com.weizhu.proto.WeizhuProtos.EmptyResponse;

public interface AdminExamService {

	@ResponseType(GetQuestionResponse.class)
	ListenableFuture<GetQuestionResponse> getQuestion(AdminHead head, GetQuestionRequest request);
	
	@WriteMethod
	@ResponseType(CreateQuestionResponse.class)
	ListenableFuture<CreateQuestionResponse> createQuestion(AdminHead head, CreateQuestionRequest request);
	
	@WriteMethod
	@ResponseType(UpdateQuestionResponse.class)
	ListenableFuture<UpdateQuestionResponse> updateQuestion(AdminHead head, UpdateQuestionRequest request);
	
	@WriteMethod
	@ResponseType(DeleteQuestionResponse.class)
	ListenableFuture<DeleteQuestionResponse> deleteQuestion(AdminHead head, DeleteQuestionRequest request);
	
	@ResponseType(GetExamResponse.class)
	ListenableFuture<GetExamResponse> getExam(AdminHead head, GetExamRequest request);
	
	@WriteMethod
	@ResponseType(CreateExamResponse.class)
	ListenableFuture<CreateExamResponse> createExam(AdminHead head, CreateExamRequest request);
	
	@WriteMethod
	@ResponseType(UpdateExamResponse.class)
	ListenableFuture<UpdateExamResponse> updateExam(AdminHead head, UpdateExamRequest request);
	
	@WriteMethod
	@ResponseType(DeleteExamResponse.class)
	ListenableFuture<DeleteExamResponse> deleteExam(AdminHead head, DeleteExamRequest request);
	
	@ResponseType(GetExamQuestionResponse.class)
	ListenableFuture<GetExamQuestionResponse> getExamQuestion(AdminHead head, GetExamQuestionRequest request);
	
	@ResponseType(GetExamQuestionRandomResponse.class)
	ListenableFuture<GetExamQuestionRandomResponse> getExamQuestionRandom(AdminHead head, GetExamQuestionRandomRequest request);
	
	@WriteMethod
	@ResponseType(UpdateExamQuestionResponse.class)
	ListenableFuture<UpdateExamQuestionResponse> updateExamQuestion(AdminHead head, UpdateExamQuestionRequest request);
	
	@WriteMethod
	@ResponseType(UpdateExamQuestionRandomResponse.class)
	ListenableFuture<UpdateExamQuestionRandomResponse> updateExamQuestionRandom(AdminHead head, UpdateExamQuestionRandomRequest request);
	
	@ResponseType(GetExamUserResultResponse.class)
	ListenableFuture<GetExamUserResultResponse> getExamUserResult(AdminHead head, GetExamUserResultRequest request);
	
	@ResponseType(GetExamByIdResponse.class)
	ListenableFuture<GetExamByIdResponse> getExamById(AdminHead head, GetExamByIdRequest request);
	
	@WriteMethod
	@ResponseType(CreateQuestionCategoryResponse.class)
	ListenableFuture<CreateQuestionCategoryResponse> createQuestionCategory(AdminHead head, CreateQuestionCategoryRequest request);
	
	@ResponseType(GetQuestionCategoryResponse.class)
	ListenableFuture<GetQuestionCategoryResponse> getQuestionCategory(AdminHead head, EmptyRequest request);
	
	@ResponseType(GetQuestionByCategoryIdResponse.class)
	ListenableFuture<GetQuestionByCategoryIdResponse> getQuestionByCategoryId(AdminHead head, GetQuestionByCategoryIdRequest request);
	
	@WriteMethod
	@ResponseType(UpdateQuestionCategoryResponse.class)
	ListenableFuture<UpdateQuestionCategoryResponse> updateQuestionCategory(AdminHead head, UpdateQuestionCategoryRequest request);
	
	@WriteMethod
	@ResponseType(DeleteQuestionCategoryResponse.class)
	ListenableFuture<DeleteQuestionCategoryResponse> deleteQuestionCategory(AdminHead head, DeleteQuestionCategoryRequest request);
	
	
	@WriteMethod
	@ResponseType(MoveQuestionCategoryResponse.class)
	ListenableFuture<MoveQuestionCategoryResponse> moveQuestionCategoryResponse(AdminHead head, MoveQuestionCategoryRequest request);
	
	
	@WriteMethod
	@ResponseType(UpdateQuestionInQuestionCategoryResponse.class)
	ListenableFuture<UpdateQuestionInQuestionCategoryResponse> updateQuestionInQuestionCategory(AdminHead head, UpdateQuestionInQuestionCategoryRequest request);
	
	@WriteMethod
	@ResponseType(ReExamResponse.class)
	ListenableFuture<ReExamResponse> reExam(AdminHead head, ReExamRequest request);
	
	@WriteMethod
	@ResponseType(CreateExamQuestionRandomResponse.class)
	ListenableFuture<CreateExamQuestionRandomResponse> createExamQuestionRandom(AdminHead head, CreateExamQuestionRandomRequest request);
	
	@WriteMethod
	@ResponseType(ImportQuestionResponse.class)
	ListenableFuture<ImportQuestionResponse> importQuestion(AdminHead head, ImportQuestionRequest request);

	@ResponseType(EmptyResponse.class)
	ListenableFuture<EmptyResponse> loadExamSubmitTask(AdminHead head, EmptyRequest request);
	
	@ResponseType(GetExamStatisticsResponse.class)
	ListenableFuture<GetExamStatisticsResponse> getExamStatistics(AdminHead head, GetExamStatisticsRequest request);
	
	@ResponseType(GetTeamStatisticsResponse.class)
	ListenableFuture<GetTeamStatisticsResponse> getTeamStatistics(AdminHead head, GetTeamStatisticsRequest request);
	
	@ResponseType(GetPositionStatisticsResponse.class)
	ListenableFuture<GetPositionStatisticsResponse> getPositionStatistics(AdminHead head, GetPositionStatisticsRequest request);
	
	@ResponseType(GetQuestionCorrectRateResponse.class)
	ListenableFuture<GetQuestionCorrectRateResponse> getQuestionCorrectRate(AdminHead head, GetQuestionCorrectRateRequest request);

	@ResponseType(GetUserAnswerResponse.class)
	ListenableFuture<GetUserAnswerResponse> getUserAnswer(AdminHead head, GetUserAnswerRequest request);
}
