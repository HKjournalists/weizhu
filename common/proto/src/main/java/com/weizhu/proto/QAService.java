package com.weizhu.proto;

import com.google.common.util.concurrent.ListenableFuture;
import com.weizhu.proto.QAProtos.SearchMoreQuestionRequest;
import com.weizhu.proto.QAProtos.SearchMoreQuestionResponse;
import com.weizhu.proto.QAProtos.SearchQuestionRequest;
import com.weizhu.proto.QAProtos.SearchQuestionResponse;
import com.weizhu.proto.QAProtos.DeleteAnswerRequest;
import com.weizhu.proto.QAProtos.DeleteAnswerResponse;
import com.weizhu.proto.QAProtos.AddAnswerRequest;
import com.weizhu.proto.QAProtos.AddAnswerResponse;
import com.weizhu.proto.QAProtos.AddQuestionRequest;
import com.weizhu.proto.QAProtos.AddQuestionResponse;
import com.weizhu.proto.QAProtos.DeleteQuestionRequest;
import com.weizhu.proto.QAProtos.DeleteQuestionResponse;
import com.weizhu.proto.QAProtos.GetAnswerRequest;
import com.weizhu.proto.QAProtos.GetAnswerResponse;
import com.weizhu.proto.QAProtos.GetCategoryResponse;
import com.weizhu.proto.QAProtos.GetQuestionRequest;
import com.weizhu.proto.QAProtos.GetQuestionResponse;
import com.weizhu.proto.QAProtos.LikeAnswerRequest;
import com.weizhu.proto.QAProtos.LikeAnswerResponse;
import com.weizhu.proto.WeizhuProtos.EmptyRequest;
import com.weizhu.proto.WeizhuProtos.RequestHead;

public interface QAService {
	
	@ResponseType(GetCategoryResponse.class)
	ListenableFuture<GetCategoryResponse> getCategory(RequestHead head, EmptyRequest request);
		
	@ResponseType(GetQuestionResponse.class)
	ListenableFuture<GetQuestionResponse> getQuestion(RequestHead head, GetQuestionRequest request);

	@ResponseType( SearchQuestionResponse.class)
	ListenableFuture<SearchQuestionResponse> searchQuestion(RequestHead head, SearchQuestionRequest request);

	@ResponseType( SearchMoreQuestionResponse.class)
	ListenableFuture<SearchMoreQuestionResponse> searchMoreQuestion(RequestHead head, SearchMoreQuestionRequest request);

	@WriteMethod
	@ResponseType(AddQuestionResponse.class)
	ListenableFuture<AddQuestionResponse> addQuestion(RequestHead head, AddQuestionRequest request);

	@WriteMethod
	@ResponseType(DeleteQuestionResponse.class)
	ListenableFuture<DeleteQuestionResponse> deleteQuestion(RequestHead head, DeleteQuestionRequest request);

	@ResponseType(GetAnswerResponse.class)
	ListenableFuture<GetAnswerResponse> getAnswer(RequestHead head, GetAnswerRequest request);
	
	@WriteMethod
	@ResponseType(AddAnswerResponse.class)
	ListenableFuture<AddAnswerResponse> addAnswer(RequestHead head, AddAnswerRequest request);

	@WriteMethod
	@ResponseType(DeleteAnswerResponse.class)
	ListenableFuture<DeleteAnswerResponse> deleteAnswer(RequestHead head, DeleteAnswerRequest request);
	
	@WriteMethod
	@ResponseType(LikeAnswerResponse.class)
	ListenableFuture<LikeAnswerResponse> likeAnswer(RequestHead head, LikeAnswerRequest request);
}
