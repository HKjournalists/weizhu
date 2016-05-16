package com.weizhu.proto;

import com.google.common.util.concurrent.ListenableFuture;
import com.weizhu.proto.ExamProtos.GetClosedExamListRequest;
import com.weizhu.proto.ExamProtos.GetClosedExamListResponse;
import com.weizhu.proto.ExamProtos.GetExamByIdRequest;
import com.weizhu.proto.ExamProtos.GetExamByIdResponse;
import com.weizhu.proto.ExamProtos.GetExamInfoRequest;
import com.weizhu.proto.ExamProtos.GetExamInfoResponse;
import com.weizhu.proto.ExamProtos.GetOpenExamCountResponse;
import com.weizhu.proto.ExamProtos.GetOpenExamListRequest;
import com.weizhu.proto.ExamProtos.GetOpenExamListResponse;
import com.weizhu.proto.ExamProtos.SaveAnswerRequest;
import com.weizhu.proto.ExamProtos.SaveAnswerResponse;
import com.weizhu.proto.ExamProtos.SubmitExamRequest;
import com.weizhu.proto.ExamProtos.SubmitExamResponse;
import com.weizhu.proto.WeizhuProtos.EmptyRequest;
import com.weizhu.proto.WeizhuProtos.RequestHead;

public interface ExamService {

	@ResponseType(GetOpenExamListResponse.class)
	ListenableFuture<GetOpenExamListResponse> getOpenExamList(RequestHead head, GetOpenExamListRequest request);
	
	@ResponseType(GetOpenExamCountResponse.class)
	ListenableFuture<GetOpenExamCountResponse> getOpenExamCount(RequestHead head, EmptyRequest request);
	
	@ResponseType(GetClosedExamListResponse.class)
	ListenableFuture<GetClosedExamListResponse> getClosedExamList(RequestHead head, GetClosedExamListRequest request);
	
	@ResponseType(GetExamByIdResponse.class)
	ListenableFuture<GetExamByIdResponse> getExamById(RequestHead head, GetExamByIdRequest request);
	
	@ResponseType(GetExamInfoResponse.class)
	ListenableFuture<GetExamInfoResponse> getExamInfo(RequestHead head, GetExamInfoRequest request);
	
	@WriteMethod
	@ResponseType(SaveAnswerResponse.class)
	ListenableFuture<SaveAnswerResponse> saveAnswer(RequestHead head, SaveAnswerRequest request);
	
	@WriteMethod
	@ResponseType(SubmitExamResponse.class)
	ListenableFuture<SubmitExamResponse> submitExam(RequestHead head, SubmitExamRequest request);
}
