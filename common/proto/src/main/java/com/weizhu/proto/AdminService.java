package com.weizhu.proto;

import com.google.common.util.concurrent.ListenableFuture;
import com.weizhu.proto.AdminProtos.AdminAnonymousHead;
import com.weizhu.proto.AdminProtos.AdminForgotPasswordRequest;
import com.weizhu.proto.AdminProtos.AdminForgotPasswordResetRequest;
import com.weizhu.proto.AdminProtos.AdminForgotPasswordResetResponse;
import com.weizhu.proto.AdminProtos.AdminForgotPasswordResponse;
import com.weizhu.proto.AdminProtos.AdminHead;
import com.weizhu.proto.AdminProtos.AdminLoginRequest;
import com.weizhu.proto.AdminProtos.AdminLoginResponse;
import com.weizhu.proto.AdminProtos.AdminResetPasswordRequest;
import com.weizhu.proto.AdminProtos.AdminResetPasswordResponse;
import com.weizhu.proto.AdminProtos.AdminVerifySessionRequest;
import com.weizhu.proto.AdminProtos.AdminVerifySessionResponse;
import com.weizhu.proto.AdminProtos.CreateAdminRequest;
import com.weizhu.proto.AdminProtos.CreateAdminResponse;
import com.weizhu.proto.AdminProtos.CreateRoleRequest;
import com.weizhu.proto.AdminProtos.CreateRoleResponse;
import com.weizhu.proto.AdminProtos.GetAdminByIdRequest;
import com.weizhu.proto.AdminProtos.GetAdminByIdResponse;
import com.weizhu.proto.AdminProtos.GetAdminListRequest;
import com.weizhu.proto.AdminProtos.GetAdminListResponse;
import com.weizhu.proto.AdminProtos.GetRoleByIdRequest;
import com.weizhu.proto.AdminProtos.GetRoleByIdResponse;
import com.weizhu.proto.AdminProtos.GetRoleListRequest;
import com.weizhu.proto.AdminProtos.GetRoleListResponse;
import com.weizhu.proto.AdminProtos.UpdateAdminRequest;
import com.weizhu.proto.AdminProtos.UpdateAdminResponse;
import com.weizhu.proto.AdminProtos.UpdateAdminStateRequest;
import com.weizhu.proto.AdminProtos.UpdateAdminStateResponse;
import com.weizhu.proto.AdminProtos.UpdateRoleRequest;
import com.weizhu.proto.AdminProtos.UpdateRoleResponse;
import com.weizhu.proto.AdminProtos.UpdateRoleStateRequest;
import com.weizhu.proto.AdminProtos.UpdateRoleStateResponse;
import com.weizhu.proto.WeizhuProtos.EmptyRequest;
import com.weizhu.proto.WeizhuProtos.EmptyResponse;

public interface AdminService {
	
	@ResponseType(AdminVerifySessionResponse.class)
	ListenableFuture<AdminVerifySessionResponse> adminVerifySession(AdminAnonymousHead head, AdminVerifySessionRequest request);

	/* 登陆相关 */
	
	@WriteMethod
	@ResponseType(AdminLoginResponse.class)
	ListenableFuture<AdminLoginResponse> adminLogin(AdminAnonymousHead head, AdminLoginRequest request);
	
	@WriteMethod
	@ResponseType(EmptyResponse.class)
	ListenableFuture<EmptyResponse> adminLogout(AdminHead head, EmptyRequest request);
	
	@WriteMethod
	@ResponseType(AdminResetPasswordResponse.class)
	ListenableFuture<AdminResetPasswordResponse> adminResetPassword(AdminAnonymousHead head, AdminResetPasswordRequest request);
	
	@WriteMethod
	@ResponseType(AdminForgotPasswordResponse.class)
	ListenableFuture<AdminForgotPasswordResponse> adminForgotPassword(AdminAnonymousHead head, AdminForgotPasswordRequest request);
	
	@WriteMethod
	@ResponseType(AdminForgotPasswordResetResponse.class)
	ListenableFuture<AdminForgotPasswordResetResponse> adminForgotPasswordReset(AdminAnonymousHead head, AdminForgotPasswordResetRequest request);
	
	/* 管理员账号管理 */
	
	@ResponseType(GetAdminByIdResponse.class)
	ListenableFuture<GetAdminByIdResponse> getAdminById(AdminHead head, GetAdminByIdRequest request);
	
	@ResponseType(GetAdminListResponse.class)
	ListenableFuture<GetAdminListResponse> getAdminList(AdminHead head, GetAdminListRequest request);
	
	@WriteMethod
	@ResponseType(CreateAdminResponse.class)
	ListenableFuture<CreateAdminResponse> createAdmin(AdminHead head, CreateAdminRequest request);
	
	@WriteMethod
	@ResponseType(UpdateAdminResponse.class)
	ListenableFuture<UpdateAdminResponse> updateAdmin(AdminHead head, UpdateAdminRequest request);
	
	@WriteMethod
	@ResponseType(UpdateAdminStateResponse.class)
	ListenableFuture<UpdateAdminStateResponse> updateAdminState(AdminHead head, UpdateAdminStateRequest request);
	
	@ResponseType(GetRoleByIdResponse.class)
	ListenableFuture<GetRoleByIdResponse> getRoleById(AdminHead head, GetRoleByIdRequest request);
	
	@ResponseType(GetRoleListResponse.class)
	ListenableFuture<GetRoleListResponse> getRoleList(AdminHead head, GetRoleListRequest request);
	
	@WriteMethod
	@ResponseType(CreateRoleResponse.class)
	ListenableFuture<CreateRoleResponse> createRole(AdminHead head, CreateRoleRequest request);
	
	@WriteMethod
	@ResponseType(UpdateRoleResponse.class)
	ListenableFuture<UpdateRoleResponse> updateRole(AdminHead head, UpdateRoleRequest request);
	
	@WriteMethod
	@ResponseType(UpdateRoleStateResponse.class)
	ListenableFuture<UpdateRoleStateResponse> updateRoleState(AdminHead head, UpdateRoleStateRequest request);
	
}
