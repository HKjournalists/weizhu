package com.weizhu.proto;

import com.google.common.util.concurrent.ListenableFuture;
import com.weizhu.proto.AdminProtos.AdminHead;
import com.weizhu.proto.AdminQAProtos.AddAnswerRequest;
import com.weizhu.proto.AdminQAProtos.AddAnswerResponse;
import com.weizhu.proto.AdminQAProtos.AddCategoryRequest;
import com.weizhu.proto.AdminQAProtos.AddCategoryResponse;
import com.weizhu.proto.AdminQAProtos.AddQuestionRequest;
import com.weizhu.proto.AdminQAProtos.AddQuestionResponse;
import com.weizhu.proto.AdminQAProtos.DeleteAnswerRequest;
import com.weizhu.proto.AdminQAProtos.DeleteAnswerResponse;
import com.weizhu.proto.AdminQAProtos.DeleteCategoryRequest;
import com.weizhu.proto.AdminQAProtos.DeleteCategoryResponse;
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
import com.weizhu.proto.AdminQAProtos.DeleteQuestionRequest;
import com.weizhu.proto.AdminQAProtos.DeleteQuestionResponse;
import com.weizhu.proto.AdminQAProtos.ChangeQuestionCategoryRequest;
import com.weizhu.proto.AdminQAProtos.ChangeQuestionCategoryResponse;
import com.weizhu.proto.WeizhuProtos.EmptyRequest;

public interface AdminQAService {
	
	@ResponseType(GetCategoryResponse.class)
	ListenableFuture<GetCategoryResponse> getCategory(AdminHead head, EmptyRequest request);

	@WriteMethod
	@ResponseType(AddCategoryResponse.class)
	ListenableFuture<AddCategoryResponse> addCategory(AdminHead head, AddCategoryRequest request);
	
	@WriteMethod
	@ResponseType(UpdateCategoryResponse.class)
	ListenableFuture<UpdateCategoryResponse> updateCategory(AdminHead head, UpdateCategoryRequest request);
	
	@WriteMethod
	@ResponseType(DeleteCategoryResponse.class)
	ListenableFuture<DeleteCategoryResponse> deleteCategory(AdminHead head, DeleteCategoryRequest request);
		
	@ResponseType(GetQuestionResponse.class)
	ListenableFuture<GetQuestionResponse> getQuestion(AdminHead head, GetQuestionRequest request);
	
	@WriteMethod
	@ResponseType(AddQuestionResponse.class)
	ListenableFuture<AddQuestionResponse> addQuestion(AdminHead head, AddQuestionRequest request);

	@WriteMethod
	@ResponseType(ChangeQuestionCategoryResponse.class)
	ListenableFuture<ChangeQuestionCategoryResponse> changeQuestionCategory(AdminHead head, ChangeQuestionCategoryRequest request);	
	
	@WriteMethod
	@ResponseType(ImportQuestionResponse.class)
	ListenableFuture<ImportQuestionResponse> importQuestion(AdminHead head, ImportQuestionRequest request);

	@ResponseType(ExportQuestionResponse.class)
	ListenableFuture<ExportQuestionResponse> exportQuestion(AdminHead head, ExportQuestionRequest request);
	
	@WriteMethod
	@ResponseType(DeleteQuestionResponse.class)
	ListenableFuture<DeleteQuestionResponse> deleteQuestion(AdminHead head, DeleteQuestionRequest request);	
	
	@ResponseType(GetAnswerResponse.class)
	ListenableFuture<GetAnswerResponse> getAnswer(AdminHead head, GetAnswerRequest request);
	
	@WriteMethod
	@ResponseType(AddAnswerResponse.class)
	ListenableFuture<AddAnswerResponse> addAnswer(AdminHead head, AddAnswerRequest request);
	
	@WriteMethod
	@ResponseType(DeleteAnswerResponse.class)
	ListenableFuture<DeleteAnswerResponse> deleteAnswer(AdminHead head, DeleteAnswerRequest request);	



}
