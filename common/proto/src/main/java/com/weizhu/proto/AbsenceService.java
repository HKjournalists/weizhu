package com.weizhu.proto;

import com.google.common.util.concurrent.ListenableFuture;
import com.weizhu.proto.AbsenceProtos.CancelAbsenceRequest;
import com.weizhu.proto.AbsenceProtos.CancelAbsenceResponse;
import com.weizhu.proto.AbsenceProtos.CreateAbsenceRequest;
import com.weizhu.proto.AbsenceProtos.CreateAbsenceResponse;
import com.weizhu.proto.AbsenceProtos.GetAbsenceByIdRequest;
import com.weizhu.proto.AbsenceProtos.GetAbsenceByIdResponse;
import com.weizhu.proto.AbsenceProtos.GetAbsenceCliRequest;
import com.weizhu.proto.AbsenceProtos.GetAbsenceCliResponse;
import com.weizhu.proto.AbsenceProtos.GetAbsenceNowResponse;
import com.weizhu.proto.AbsenceProtos.GetAbsenceSerRequest;
import com.weizhu.proto.AbsenceProtos.GetAbsenceSerResponse;
import com.weizhu.proto.AbsenceProtos.UpdateAbsenceRequest;
import com.weizhu.proto.AbsenceProtos.UpdateAbsenceResponse;
import com.weizhu.proto.AdminProtos.AdminHead;
import com.weizhu.proto.WeizhuProtos.EmptyRequest;
import com.weizhu.proto.WeizhuProtos.RequestHead;

public interface AbsenceService {

	@ResponseType(GetAbsenceByIdResponse.class)
	ListenableFuture<GetAbsenceByIdResponse> getAbsenceById(RequestHead head, GetAbsenceByIdRequest request);
	
	@ResponseType(GetAbsenceNowResponse.class)
	ListenableFuture<GetAbsenceNowResponse> getAbsenceNow(RequestHead head, EmptyRequest request);
	
	@WriteMethod
	@ResponseType(CreateAbsenceResponse.class)
	ListenableFuture<CreateAbsenceResponse> createAbsence(RequestHead head, CreateAbsenceRequest request);
	
	@WriteMethod
	@ResponseType(CancelAbsenceResponse.class)
	ListenableFuture<CancelAbsenceResponse> cancelAbsence(RequestHead head, CancelAbsenceRequest request);
	
	@ResponseType(GetAbsenceCliResponse.class)
	ListenableFuture<GetAbsenceCliResponse> getAbsenceCli(RequestHead head, GetAbsenceCliRequest request);
	
	
	@ResponseType(GetAbsenceByIdResponse.class)
	ListenableFuture<GetAbsenceByIdResponse> getAbsenceById(AdminHead head, GetAbsenceByIdRequest request);
	
	@ResponseType(GetAbsenceSerResponse.class)
	ListenableFuture<GetAbsenceSerResponse> getAbsenceSer(AdminHead head, GetAbsenceSerRequest request);
	
	@WriteMethod
	@ResponseType(UpdateAbsenceResponse.class)
	ListenableFuture<UpdateAbsenceResponse> updateAbsence(AdminHead head, UpdateAbsenceRequest request);
	
}
