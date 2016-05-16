package com.weizhu.proto;

import com.google.common.util.concurrent.ListenableFuture;
import com.weizhu.proto.UserProtos.CreateAbilityTagRequest;
import com.weizhu.proto.UserProtos.CreateAbilityTagResponse;
import com.weizhu.proto.UserProtos.CreateUserExperienceRequest;
import com.weizhu.proto.UserProtos.CreateUserExperienceResponse;
import com.weizhu.proto.UserProtos.DeleteAbilityTagRequest;
import com.weizhu.proto.UserProtos.DeleteUserExperienceRequest;
import com.weizhu.proto.UserProtos.DeleteUserExperienceResponse;
import com.weizhu.proto.UserProtos.GetAbilityTagUserIdRequest;
import com.weizhu.proto.UserProtos.GetAbilityTagUserIdResponse;
import com.weizhu.proto.UserProtos.GetAllLevelRequest;
import com.weizhu.proto.UserProtos.GetAllLevelResponse;
import com.weizhu.proto.UserProtos.GetAllPositionRequest;
import com.weizhu.proto.UserProtos.GetAllPositionResponse;
import com.weizhu.proto.UserProtos.GetMarkStarUserRequest;
import com.weizhu.proto.UserProtos.GetMarkStarUserResponse;
import com.weizhu.proto.UserProtos.GetRandomAbilityTagUserRequest;
import com.weizhu.proto.UserProtos.GetRandomAbilityTagUserResponse;
import com.weizhu.proto.UserProtos.GetTeamRequest;
import com.weizhu.proto.UserProtos.GetTeamResponse;
import com.weizhu.proto.UserProtos.GetUserAbilityTagRequest;
import com.weizhu.proto.UserProtos.GetUserAbilityTagResponse;
import com.weizhu.proto.UserProtos.GetUserBaseByIdRequest;
import com.weizhu.proto.UserProtos.GetUserBaseByIdResponse;
import com.weizhu.proto.UserProtos.GetUserByIdRequest;
import com.weizhu.proto.UserProtos.GetUserByMobileNoRequest;
import com.weizhu.proto.UserProtos.GetUserExperienceRequest;
import com.weizhu.proto.UserProtos.GetUserExperienceResponse;
import com.weizhu.proto.UserProtos.GetUserResponse;
import com.weizhu.proto.UserProtos.MarkUserNameRequest;
import com.weizhu.proto.UserProtos.MarkUserNameResponse;
import com.weizhu.proto.UserProtos.MarkUserStarRequest;
import com.weizhu.proto.UserProtos.MarkUserStarResponse;
import com.weizhu.proto.UserProtos.SearchUserRequest;
import com.weizhu.proto.UserProtos.SearchUserResponse;
import com.weizhu.proto.UserProtos.TagUserAbilityRequest;
import com.weizhu.proto.UserProtos.TagUserAbilityResponse;
import com.weizhu.proto.UserProtos.UpdateUserAvatarRequest;
import com.weizhu.proto.UserProtos.UpdateUserAvatarResponse;
import com.weizhu.proto.UserProtos.UpdateUserExperienceRequest;
import com.weizhu.proto.UserProtos.UpdateUserExperienceResponse;
import com.weizhu.proto.UserProtos.UpdateUserInterestRequest;
import com.weizhu.proto.UserProtos.UpdateUserInterestResponse;
import com.weizhu.proto.UserProtos.UpdateUserSignatureRequest;
import com.weizhu.proto.UserProtos.UpdateUserSignatureResponse;
import com.weizhu.proto.WeizhuProtos.AnonymousHead;
import com.weizhu.proto.WeizhuProtos.EmptyResponse;
import com.weizhu.proto.WeizhuProtos.RequestHead;

public interface UserService {
	
	/* 匿名访问接口 */
	
	@ResponseType(GetUserResponse.class)
	ListenableFuture<GetUserResponse> getUserByMobileNo(AnonymousHead head, GetUserByMobileNoRequest request);
	
	/* 带登陆身份接口 */

	@ResponseType(GetUserBaseByIdResponse.class)
	ListenableFuture<GetUserBaseByIdResponse> getUserBaseById(RequestHead head, GetUserBaseByIdRequest request);
	
	@ResponseType(GetUserResponse.class)
	ListenableFuture<GetUserResponse> getUserById(RequestHead head, GetUserByIdRequest request);
	
	@ResponseType(GetUserResponse.class)
	ListenableFuture<GetUserResponse> getUserByMobileNo(RequestHead head, GetUserByMobileNoRequest request);
	
	@ResponseType(GetTeamResponse.class)
	ListenableFuture<GetTeamResponse> getTeam(RequestHead head, GetTeamRequest request);
	
	@WriteMethod
	@ResponseType(UpdateUserAvatarResponse.class)
	ListenableFuture<UpdateUserAvatarResponse> updateUserAvatar(RequestHead head, UpdateUserAvatarRequest request);
	
	@WriteMethod
	@ResponseType(UpdateUserSignatureResponse.class)
	ListenableFuture<UpdateUserSignatureResponse> updateUserSignature(RequestHead head, UpdateUserSignatureRequest request);
	
	@WriteMethod
	@ResponseType(UpdateUserInterestResponse.class)
	ListenableFuture<UpdateUserInterestResponse> updateUserInterest(RequestHead head, UpdateUserInterestRequest request);
	
	@ResponseType(GetUserExperienceResponse.class)
	ListenableFuture<GetUserExperienceResponse> getUserExperience(RequestHead head, GetUserExperienceRequest request);
	
	@WriteMethod
	@ResponseType(CreateUserExperienceResponse.class)
	ListenableFuture<CreateUserExperienceResponse> createUserExperience(RequestHead head, CreateUserExperienceRequest request);
	
	@WriteMethod
	@ResponseType(UpdateUserExperienceResponse.class)
	ListenableFuture<UpdateUserExperienceResponse> updateUserExperience(RequestHead head, UpdateUserExperienceRequest request);
	
	@WriteMethod
	@ResponseType(DeleteUserExperienceResponse.class)
	ListenableFuture<DeleteUserExperienceResponse> deleteUserExperience(RequestHead head, DeleteUserExperienceRequest request);
	
	@WriteMethod
	@ResponseType(MarkUserNameResponse.class)
	ListenableFuture<MarkUserNameResponse> markUserName(RequestHead head, MarkUserNameRequest request);
	
	@WriteMethod
	@ResponseType(MarkUserStarResponse.class)
	ListenableFuture<MarkUserStarResponse> markUserStar(RequestHead head, MarkUserStarRequest request);
	
	@ResponseType(GetMarkStarUserResponse.class)
	ListenableFuture<GetMarkStarUserResponse> getMarkStarUser(RequestHead head, GetMarkStarUserRequest request);
	
	@ResponseType(GetUserAbilityTagResponse.class)
	ListenableFuture<GetUserAbilityTagResponse> getUserAbilityTag(RequestHead head, GetUserAbilityTagRequest request);
	
	@WriteMethod
	@ResponseType(TagUserAbilityResponse.class)
	ListenableFuture<TagUserAbilityResponse> tagUserAbility(RequestHead head, TagUserAbilityRequest request);
	
	@WriteMethod
	@ResponseType(CreateAbilityTagResponse.class)
	ListenableFuture<CreateAbilityTagResponse> createAbilityTag(RequestHead head, CreateAbilityTagRequest request);
	
	@WriteMethod
	@ResponseType(EmptyResponse.class)
	ListenableFuture<EmptyResponse> deleteAbilityTag(RequestHead head, DeleteAbilityTagRequest request);
	
	@ResponseType(GetRandomAbilityTagUserResponse.class)
	ListenableFuture<GetRandomAbilityTagUserResponse> getRandomAbilityTagUser(RequestHead head, GetRandomAbilityTagUserRequest request);
	
	@ResponseType(GetAbilityTagUserIdResponse.class)
	ListenableFuture<GetAbilityTagUserIdResponse> getAbilityTagUserId(RequestHead head, GetAbilityTagUserIdRequest request);
	
	@ResponseType(GetAllPositionResponse.class)
	ListenableFuture<GetAllPositionResponse> getAllPosition(RequestHead head, GetAllPositionRequest request);
	
	@ResponseType(GetAllLevelResponse.class)
	ListenableFuture<GetAllLevelResponse> getAllLevel(RequestHead head, GetAllLevelRequest request);
	
	@ResponseType(SearchUserResponse.class)
	ListenableFuture<SearchUserResponse> searchUser(RequestHead head, SearchUserRequest request);
	
}
