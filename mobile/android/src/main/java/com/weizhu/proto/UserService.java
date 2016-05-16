package com.weizhu.proto;

import com.weizhu.network.Future;
import com.weizhu.proto.UserProtos.CreateAbilityTagRequest;
import com.weizhu.proto.UserProtos.CreateAbilityTagResponse;
import com.weizhu.proto.UserProtos.CreateUserExperienceRequest;
import com.weizhu.proto.UserProtos.CreateUserExperienceResponse;
import com.weizhu.proto.UserProtos.DeleteAbilityTagRequest;
import com.weizhu.proto.UserProtos.DeleteUserExperienceRequest;
import com.weizhu.proto.UserProtos.DeleteUserExperienceResponse;
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
import com.weizhu.proto.WeizhuProtos.EmptyResponse;


public interface UserService {

	@ResponseType(GetUserResponse.class)
	Future<GetUserResponse> getUserById(GetUserByIdRequest request, int priorityNum);
	
	@ResponseType(GetUserResponse.class)
	Future<GetUserResponse> getUserByMobileNo(GetUserByMobileNoRequest request, int priorityNum);
	
	@ResponseType(GetTeamResponse.class)
	Future<GetTeamResponse> getTeam(GetTeamRequest request, int priorityNum);
	
	@ResponseType(UpdateUserAvatarResponse.class)
	Future<UpdateUserAvatarResponse> updateUserAvatar(UpdateUserAvatarRequest request, int priorityNum);
	
	@ResponseType(UpdateUserSignatureResponse.class)
	Future<UpdateUserSignatureResponse> updateUserSignature(UpdateUserSignatureRequest request, int priorityNum);
	
	@ResponseType(UpdateUserInterestResponse.class)
	Future<UpdateUserInterestResponse> updateUserInterest(UpdateUserInterestRequest request, int priorityNum);
	
	@ResponseType(GetUserExperienceResponse.class)
	Future<GetUserExperienceResponse> getUserExperience(GetUserExperienceRequest request, int priorityNum);
	
	@ResponseType(CreateUserExperienceResponse.class)
	Future<CreateUserExperienceResponse> createUserExperience(CreateUserExperienceRequest request, int priorityNum);
	
	@ResponseType(UpdateUserExperienceResponse.class)
	Future<UpdateUserExperienceResponse> updateUserExperience(UpdateUserExperienceRequest request, int priorityNum);
	
	@ResponseType(DeleteUserExperienceResponse.class)
	Future<DeleteUserExperienceResponse> deleteUserExperience(DeleteUserExperienceRequest request, int priorityNum);
	
	@ResponseType(MarkUserNameResponse.class)
	Future<MarkUserNameResponse> markUserName(MarkUserNameRequest request, int priorityNum);
	
	@ResponseType(MarkUserStarResponse.class)
	Future<MarkUserStarResponse> markUserStar(MarkUserStarRequest request, int priorityNum);
	
	@ResponseType(GetMarkStarUserResponse.class)
	Future<GetMarkStarUserResponse> getMarkStarUser(GetMarkStarUserRequest request, int priorityNum);
	
	@ResponseType(GetUserAbilityTagResponse.class)
	Future<GetUserAbilityTagResponse> getUserAbilityTag(GetUserAbilityTagRequest request, int priorityNum);
	
	@ResponseType(TagUserAbilityResponse.class)
	Future<TagUserAbilityResponse> tagUserAbility(TagUserAbilityRequest request, int priorityNum);
	
	@ResponseType(CreateAbilityTagResponse.class)
	Future<CreateAbilityTagResponse> createAbilityTag(CreateAbilityTagRequest request, int priorityNum);
	
	@ResponseType(EmptyResponse.class)
	Future<EmptyResponse> deleteAbilityTag(DeleteAbilityTagRequest request, int priorityNum);
	
	@ResponseType(GetRandomAbilityTagUserResponse.class)
	Future<GetRandomAbilityTagUserResponse> getRandomAbilityTagUser(GetRandomAbilityTagUserRequest request, int priorityNum);
	
	@ResponseType(GetAllPositionResponse.class)
	Future<GetAllPositionResponse> getAllPosition(GetAllPositionRequest request, int priorityNum);
	
	@ResponseType(GetAllLevelResponse.class)
	Future<GetAllLevelResponse> getAllLevel(GetAllLevelRequest request, int priorityNum);
	
	@ResponseType(SearchUserResponse.class)
	Future<SearchUserResponse> searchUser(SearchUserRequest request, int priorityNum);
}
