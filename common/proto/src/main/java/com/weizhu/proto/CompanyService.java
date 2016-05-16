package com.weizhu.proto;

import com.google.common.util.concurrent.ListenableFuture;
import com.weizhu.proto.AdminProtos.AdminAnonymousHead;
import com.weizhu.proto.AdminProtos.AdminHead;
import com.weizhu.proto.BossProtos.BossHead;
import com.weizhu.proto.CompanyProtos.GetCompanyListResponse;
import com.weizhu.proto.CompanyProtos.GetCompanyResponse;
import com.weizhu.proto.CompanyProtos.VerifyCompanyKeyRequest;
import com.weizhu.proto.CompanyProtos.VerifyCompanyKeyResponse;
import com.weizhu.proto.WeizhuProtos.AnonymousHead;
import com.weizhu.proto.WeizhuProtos.EmptyRequest;
import com.weizhu.proto.WeizhuProtos.RequestHead;
import com.weizhu.proto.WeizhuProtos.SystemHead;

public interface CompanyService {

	/* 用户访问接口 */
	
	@ResponseType(VerifyCompanyKeyResponse.class)
	ListenableFuture<VerifyCompanyKeyResponse> verifyCompanyKey(AnonymousHead head, VerifyCompanyKeyRequest request);
	
	@ResponseType(GetCompanyResponse.class)
	ListenableFuture<GetCompanyResponse> getCompany(AnonymousHead head, EmptyRequest request);
	
	@ResponseType(GetCompanyResponse.class)
	ListenableFuture<GetCompanyResponse> getCompany(RequestHead head, EmptyRequest request);
	
	/* 管理员访问接口 */
	
	@ResponseType(GetCompanyListResponse.class)
	ListenableFuture<GetCompanyListResponse> getCompanyList(AdminAnonymousHead head, EmptyRequest request);
	
	@ResponseType(GetCompanyListResponse.class)
	ListenableFuture<GetCompanyListResponse> getCompanyList(AdminHead head, EmptyRequest request);
	
	/* Boss访问接口 */
	
	@ResponseType(GetCompanyListResponse.class)
	ListenableFuture<GetCompanyListResponse> getCompanyList(BossHead head, EmptyRequest request);
	
	/* 系统访问接口 */
	
	@ResponseType(GetCompanyListResponse.class)
	ListenableFuture<GetCompanyListResponse> getCompanyList(SystemHead head, EmptyRequest request);
}
