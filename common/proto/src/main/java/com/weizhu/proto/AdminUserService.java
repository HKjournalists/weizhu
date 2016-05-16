package com.weizhu.proto;

import com.google.common.util.concurrent.ListenableFuture;
import com.weizhu.proto.AdminProtos.AdminHead;
import com.weizhu.proto.AdminUserProtos.CreateLevelRequest;
import com.weizhu.proto.AdminUserProtos.CreateLevelResponse;
import com.weizhu.proto.AdminUserProtos.CreatePositionRequest;
import com.weizhu.proto.AdminUserProtos.CreatePositionResponse;
import com.weizhu.proto.AdminUserProtos.CreateTeamRequest;
import com.weizhu.proto.AdminUserProtos.CreateTeamResponse;
import com.weizhu.proto.AdminUserProtos.CreateUserRequest;
import com.weizhu.proto.AdminUserProtos.CreateUserResponse;
import com.weizhu.proto.AdminUserProtos.DeleteLevelRequest;
import com.weizhu.proto.AdminUserProtos.DeleteLevelResponse;
import com.weizhu.proto.AdminUserProtos.DeletePositionRequest;
import com.weizhu.proto.AdminUserProtos.DeletePositionResponse;
import com.weizhu.proto.AdminUserProtos.DeleteTeamRequest;
import com.weizhu.proto.AdminUserProtos.DeleteTeamResponse;
import com.weizhu.proto.AdminUserProtos.DeleteUserRequest;
import com.weizhu.proto.AdminUserProtos.DeleteUserResponse;
import com.weizhu.proto.AdminUserProtos.GetAbilityTagUserIdRequest;
import com.weizhu.proto.AdminUserProtos.GetAbilityTagUserIdResponse;
import com.weizhu.proto.AdminUserProtos.GetAllTeamResponse;
import com.weizhu.proto.AdminUserProtos.GetLevelResponse;
import com.weizhu.proto.AdminUserProtos.GetPositionResponse;
import com.weizhu.proto.AdminUserProtos.GetTeamAllUserIdRequest;
import com.weizhu.proto.AdminUserProtos.GetTeamAllUserIdResponse;
import com.weizhu.proto.AdminUserProtos.GetTeamByIdRequest;
import com.weizhu.proto.AdminUserProtos.GetTeamByIdResponse;
import com.weizhu.proto.AdminUserProtos.GetTeamRequest;
import com.weizhu.proto.AdminUserProtos.GetTeamResponse;
import com.weizhu.proto.AdminUserProtos.GetUserAbilityTagRequest;
import com.weizhu.proto.AdminUserProtos.GetUserAbilityTagResponse;
import com.weizhu.proto.AdminUserProtos.GetUserByIdRequest;
import com.weizhu.proto.AdminUserProtos.GetUserByIdResponse;
import com.weizhu.proto.AdminUserProtos.GetUserByMobileNoUniqueRequest;
import com.weizhu.proto.AdminUserProtos.GetUserByMobileNoUniqueResponse;
import com.weizhu.proto.AdminUserProtos.GetUserExtendsNameResponse;
import com.weizhu.proto.AdminUserProtos.GetUserListRequest;
import com.weizhu.proto.AdminUserProtos.GetUserListResponse;
import com.weizhu.proto.AdminUserProtos.ImportUserRequest;
import com.weizhu.proto.AdminUserProtos.ImportUserResponse;
import com.weizhu.proto.AdminUserProtos.RegisterUserRequest;
import com.weizhu.proto.AdminUserProtos.RegisterUserResponse;
import com.weizhu.proto.AdminUserProtos.SetExpertRequest;
import com.weizhu.proto.AdminUserProtos.SetExpertResponse;
import com.weizhu.proto.AdminUserProtos.SetStateRequest;
import com.weizhu.proto.AdminUserProtos.SetStateResponse;
import com.weizhu.proto.AdminUserProtos.SetUserAbilityTagRequest;
import com.weizhu.proto.AdminUserProtos.SetUserAbilityTagResponse;
import com.weizhu.proto.AdminUserProtos.UpdateLevelRequest;
import com.weizhu.proto.AdminUserProtos.UpdateLevelResponse;
import com.weizhu.proto.AdminUserProtos.UpdatePositionRequest;
import com.weizhu.proto.AdminUserProtos.UpdatePositionResponse;
import com.weizhu.proto.AdminUserProtos.UpdateTeamRequest;
import com.weizhu.proto.AdminUserProtos.UpdateTeamResponse;
import com.weizhu.proto.AdminUserProtos.UpdateUserRequest;
import com.weizhu.proto.AdminUserProtos.UpdateUserResponse;
import com.weizhu.proto.WeizhuProtos.AnonymousHead;
import com.weizhu.proto.WeizhuProtos.EmptyRequest;
import com.weizhu.proto.WeizhuProtos.EmptyResponse;
import com.weizhu.proto.WeizhuProtos.RequestHead;
import com.weizhu.proto.WeizhuProtos.SystemHead;

public interface AdminUserService {

	@WriteMethod
	@ResponseType(ImportUserResponse.class)
	ListenableFuture<ImportUserResponse> importUser(AdminHead head, ImportUserRequest request);

	@WriteMethod
	@ResponseType(CreateUserResponse.class)
	ListenableFuture<CreateUserResponse> createUser(AdminHead head, CreateUserRequest request);
	
	@WriteMethod
	@ResponseType(UpdateUserResponse.class)
	ListenableFuture<UpdateUserResponse> updateUser(AdminHead head, UpdateUserRequest request);
	
	@WriteMethod
	@ResponseType(DeleteUserResponse.class)
	ListenableFuture<DeleteUserResponse> deleteUser(AdminHead head, DeleteUserRequest request);
	
	@ResponseType(GetUserListResponse.class)
	ListenableFuture<GetUserListResponse> getUserList(AdminHead head, GetUserListRequest request);
	
	@ResponseType(GetUserByIdResponse.class)
	ListenableFuture<GetUserByIdResponse> getUserById(AdminHead head, GetUserByIdRequest request);
	
	@ResponseType(GetUserExtendsNameResponse.class)
	ListenableFuture<GetUserExtendsNameResponse> getUserExtendsName(AdminHead head, EmptyRequest request);
	
	@ResponseType(GetUserByMobileNoUniqueResponse.class)
	ListenableFuture<GetUserByMobileNoUniqueResponse> getUserByMobileNoUnique(AdminHead head, GetUserByMobileNoUniqueRequest request);
	
	@WriteMethod
	@ResponseType(SetExpertResponse.class)
	ListenableFuture<SetExpertResponse> setExpert(AdminHead head, SetExpertRequest request);

	@WriteMethod
	@ResponseType(SetStateResponse.class)
	ListenableFuture<SetStateResponse> setState(AdminHead head, SetStateRequest request);
	
	
	@WriteMethod
	@ResponseType(CreatePositionResponse.class)
	ListenableFuture<CreatePositionResponse> createPosition(AdminHead head, CreatePositionRequest request);
	
	@WriteMethod
	@ResponseType(UpdatePositionResponse.class)
	ListenableFuture<UpdatePositionResponse> updatePosition(AdminHead head, UpdatePositionRequest request);
	
	@WriteMethod
	@ResponseType(DeletePositionResponse.class)
	ListenableFuture<DeletePositionResponse> deletePosition(AdminHead head, DeletePositionRequest request);
	
	@ResponseType(GetPositionResponse.class)
	ListenableFuture<GetPositionResponse> getPosition(AdminHead head, EmptyRequest request);
	
	
	@WriteMethod
	@ResponseType(CreateLevelResponse.class)
	ListenableFuture<CreateLevelResponse> createLevel(AdminHead head, CreateLevelRequest request);
	
	@WriteMethod
	@ResponseType(UpdateLevelResponse.class)
	ListenableFuture<UpdateLevelResponse> updateLevel(AdminHead head, UpdateLevelRequest request);
	
	@WriteMethod
	@ResponseType(DeleteLevelResponse.class)
	ListenableFuture<DeleteLevelResponse> deleteLevel(AdminHead head, DeleteLevelRequest request);
	
	@ResponseType(GetLevelResponse.class)
	ListenableFuture<GetLevelResponse> getLevel(AdminHead head, EmptyRequest request);
	
	
	@WriteMethod
	@ResponseType(CreateTeamResponse.class)
	ListenableFuture<CreateTeamResponse> createTeam(AdminHead head, CreateTeamRequest request);
	
	@WriteMethod
	@ResponseType(UpdateTeamResponse.class)
	ListenableFuture<UpdateTeamResponse> updateTeam(AdminHead head, UpdateTeamRequest request);
	
	@WriteMethod
	@ResponseType(DeleteTeamResponse.class)
	ListenableFuture<DeleteTeamResponse> deleteTeam(AdminHead head, DeleteTeamRequest request);
	
	@ResponseType(GetTeamResponse.class)
	ListenableFuture<GetTeamResponse> getTeam(AdminHead head, GetTeamRequest request);
	
	@ResponseType(GetTeamByIdResponse.class)
	ListenableFuture<GetTeamByIdResponse> getTeamById(AdminHead head, GetTeamByIdRequest request);
	
	@ResponseType(GetTeamAllUserIdResponse.class)
	ListenableFuture<GetTeamAllUserIdResponse> getTeamAllUserId(AdminHead head, GetTeamAllUserIdRequest request);
	
	@ResponseType(GetAllTeamResponse.class)
	ListenableFuture<GetAllTeamResponse> getAllTeam(AdminHead head, EmptyRequest request);
	
	@ResponseType(EmptyResponse.class)
	ListenableFuture<EmptyResponse> reloadTeam(AdminHead head, EmptyRequest request);
	
	
	@ResponseType(GetUserAbilityTagResponse.class)
	ListenableFuture<GetUserAbilityTagResponse> getUserAbilityTag(AdminHead head, GetUserAbilityTagRequest request);
	
	@WriteMethod
	@ResponseType(SetUserAbilityTagResponse.class)
	ListenableFuture<SetUserAbilityTagResponse> setUserAbilityTag(AdminHead head, SetUserAbilityTagRequest request);
	
	@ResponseType(GetAbilityTagUserIdResponse.class)
	ListenableFuture<GetAbilityTagUserIdResponse> getAbilityTagUserId(AdminHead head, GetAbilityTagUserIdRequest request);
	
	
	/* for user login / register */
	
	@ResponseType(GetUserByMobileNoUniqueResponse.class)
	ListenableFuture<GetUserByMobileNoUniqueResponse> getUserByMobileNoUnique(AnonymousHead head, GetUserByMobileNoUniqueRequest request);
	
	@ResponseType(GetUserByIdResponse.class)
	ListenableFuture<GetUserByIdResponse> getUserById(RequestHead head, GetUserByIdRequest request);
	
	@WriteMethod
	@ResponseType(RegisterUserResponse.class)
	ListenableFuture<RegisterUserResponse> registerUser(AnonymousHead head, RegisterUserRequest request);
	
	
	/* for system */
	
	@ResponseType(GetUserListResponse.class)
	ListenableFuture<GetUserListResponse> getUserList(SystemHead head, GetUserListRequest request);
	
	@ResponseType(GetUserByIdResponse.class)
	ListenableFuture<GetUserByIdResponse> getUserById(SystemHead head, GetUserByIdRequest request);
}
