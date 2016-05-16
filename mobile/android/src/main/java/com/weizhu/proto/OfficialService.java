package com.weizhu.proto;

import com.weizhu.network.Future;
import com.weizhu.proto.OfficialProtos.GetOfficialByIdRequest;
import com.weizhu.proto.OfficialProtos.GetOfficialByIdResponse;
import com.weizhu.proto.OfficialProtos.GetOfficialListRequest;
import com.weizhu.proto.OfficialProtos.GetOfficialListResponse;
import com.weizhu.proto.OfficialProtos.GetOfficialMessageRequest;
import com.weizhu.proto.OfficialProtos.GetOfficialMessageResponse;
import com.weizhu.proto.OfficialProtos.SendOfficialMessageRequest;
import com.weizhu.proto.OfficialProtos.SendOfficialMessageResponse;

public interface OfficialService {

	@ResponseType(GetOfficialByIdResponse.class)
	Future<GetOfficialByIdResponse> getOfficialById(GetOfficialByIdRequest request, int priorityNum);
	
	@ResponseType(GetOfficialListResponse.class)
	Future<GetOfficialListResponse> getOfficialList(GetOfficialListRequest request, int priorityNum);
	
	@ResponseType(GetOfficialMessageResponse.class)
	Future<GetOfficialMessageResponse> getOfficialMessage(GetOfficialMessageRequest request, int priorityNum);
	
	@ResponseType(SendOfficialMessageResponse.class)
	Future<SendOfficialMessageResponse> sendOfficialMessage(SendOfficialMessageRequest request, int priorityNum);
}
