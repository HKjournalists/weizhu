package com.weizhu.service.user.test;

import static org.junit.Assert.*;

import org.junit.BeforeClass;
import org.junit.Test;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.weizhu.common.module.FakeExternalServiceModule;
import com.weizhu.common.module.FakeProfileServiceModule;
import com.weizhu.common.module.FakePushServiceModule;
import com.weizhu.common.module.TestModule;
import com.weizhu.common.utils.TestUtil;
import com.weizhu.proto.UserProtos.CreateUserExperienceRequest;
import com.weizhu.proto.UserProtos.CreateUserExperienceResponse;
import com.weizhu.proto.UserProtos.DeleteAbilityTagRequest;
import com.weizhu.proto.UserProtos.DeleteUserExperienceRequest;
import com.weizhu.proto.UserProtos.DeleteUserExperienceResponse;
import com.weizhu.proto.UserProtos.GetRandomAbilityTagUserRequest;
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
import com.weizhu.proto.UserProtos.UserExperience;
import com.weizhu.proto.UserService;
import com.weizhu.proto.WeizhuProtos.RequestHead;
import com.weizhu.service.user.UserServiceModule;

public class UserServiceTest {

	static {
		System.setProperty("logback.configurationFile", "com/weizhu/service/user/test/logback.xml");
	}
	
	private static final Injector INJECTOR = Guice.createInjector(new TestModule(), 
			new UserServiceTestModule(), new UserServiceModule(),
			new FakeProfileServiceModule(), new FakePushServiceModule(), new FakeExternalServiceModule());
	
	@BeforeClass
	public static void init() throws Exception {
		TestUtil.clearCache(INJECTOR);
		TestUtil.loadTestDataDB(INJECTOR);
	}
	
	private final RequestHead requestHead;
	private final UserService userService;
	
	public UserServiceTest() {
		this.requestHead = INJECTOR.getInstance(RequestHead.class);
		this.userService = INJECTOR.getInstance(UserService.class);
	}
	
	@Test
	public void testGetById() throws Exception {
		
		GetUserByIdRequest request = GetUserByIdRequest.newBuilder().addUserId(10000124207L).build();

		GetUserResponse response = userService.getUserById(requestHead, request).get();
		
		assertTrue(response.getUserCount() > 0);
		assertTrue(response.getUser(0).getMark().getIsStar());
	}
	
	@Test
	public void testGetByMobileNo() throws Exception {
		GetUserByMobileNoRequest request = GetUserByMobileNoRequest.newBuilder().setMobileNo("18601191171").build();

		GetUserResponse response = userService.getUserByMobileNo(requestHead, request).get();
		
		assertTrue(response.getUserCount() > 0);
		assertEquals("lindong@21tb.com", response.getUser(0).getBase().getEmail());
	}
	
	@Test
	public void testGetTeam() throws Exception {
		GetTeamRequest request = GetTeamRequest.newBuilder().setTeamId(1).build();
		GetTeamResponse response = userService.getTeam(requestHead, request).get();
		
		assertEquals("时代光华", response.getTeam().getTeamName());
		assertEquals(1, response.getSubTeamIdCount());
		assertEquals(0, response.getSubUserTeamCount());
		
		// System.out.println(JsonFormat.printToString(response));
	}
	
	@Test
	public void testGetTeam2() throws Exception {
		GetTeamRequest request = GetTeamRequest.newBuilder().setTeamId(4).build();
		GetTeamResponse response = userService.getTeam(requestHead, request).get();
		
		assertEquals("微助产品开发组", response.getTeam().getTeamName());
		assertEquals(0, response.getSubTeamIdCount());
		assertEquals(6, response.getSubUserTeamCount());
	}
	
	@Test
	public void testUpdateUserAvatar() throws Exception {
		UpdateUserAvatarRequest request = UpdateUserAvatarRequest.newBuilder()
				.setAvatar("123.jpg")
				.build();
		UpdateUserAvatarResponse response = userService.updateUserAvatar(requestHead, request).get();
		
		assertEquals(UpdateUserAvatarResponse.Result.SUCC, response.getResult());
	}
	
	@Test
	public void testUpdateUserSignature() throws Exception {
		UpdateUserSignatureRequest request = UpdateUserSignatureRequest.newBuilder()
				.setSignature("大猪头")
				.build();
		UpdateUserSignatureResponse response = userService.updateUserSignature(requestHead, request).get();
		
		assertEquals(UpdateUserSignatureResponse.Result.SUCC, response.getResult());
	}
	
	@Test
	public void testUpdateUserInterest() throws Exception {
		UpdateUserInterestRequest request = UpdateUserInterestRequest.newBuilder()
				.setInterest("吃喝玩乐")
				.build();
		UpdateUserInterestResponse response = userService.updateUserInterest(requestHead, request).get();
		
		assertEquals(UpdateUserInterestResponse.Result.SUCC, response.getResult());
	}
	
	@Test
	public void testGetUserExperience() throws Exception {
		GetUserExperienceRequest request = GetUserExperienceRequest.newBuilder()
				.setUserId(10000124212L)
				.build();
		GetUserExperienceResponse response = userService.getUserExperience(requestHead, request).get();
		
		assertTrue(response.getExperienceCount() > 0);
	}
	
	@Test
	public void testCreateUserExperience() throws Exception {
		CreateUserExperienceRequest request = CreateUserExperienceRequest.newBuilder()
				.setExperience(UserExperience.newBuilder()
						.setExperienceId(123)
						.setExperienceContent("哈哈哈")
						.build())
				.build();
		CreateUserExperienceResponse response = userService.createUserExperience(requestHead, request).get();
		
		assertEquals(CreateUserExperienceResponse.Result.SUCC, response.getResult());
		assertTrue(response.getExperience().getExperienceId() > 0 && response.getExperience().getExperienceId() != 123);
	}
	
	@Test
	public void testUpdateUserExperience() throws Exception {
		UpdateUserExperienceRequest request = UpdateUserExperienceRequest.newBuilder()
				.setExperience(UserExperience.newBuilder()
						.setExperienceId(3)
						.setExperienceContent("aaaaaaaaaa")
						.build())
				.build();
		
		UpdateUserExperienceResponse response = userService.updateUserExperience(requestHead, request).get();
		
		assertEquals(UpdateUserExperienceResponse.Result.SUCC, response.getResult());
	}
	
	@Test
	public void testDeleteUserExperience() throws Exception {
		DeleteUserExperienceRequest request = DeleteUserExperienceRequest.newBuilder()
				.setExperienceId(2)
				.build();
		
		DeleteUserExperienceResponse response = userService.deleteUserExperience(requestHead, request).get();
		
		assertEquals(DeleteUserExperienceResponse.Result.SUCC, response.getResult());
	}
	
	@Test
	public void testMarkUserName() throws Exception {
		MarkUserNameRequest request = MarkUserNameRequest.newBuilder()
				.setUserId(10000124209L)
				.setMarkName("gongning")
				.build();
		MarkUserNameResponse response = userService.markUserName(requestHead, request).get();
		
		assertEquals(MarkUserNameResponse.Result.SUCC, response.getResult());
	}
	
	@Test
	public void testMarkUserStar() throws Exception {
		MarkUserStarRequest request = MarkUserStarRequest.newBuilder()
				.setUserId(10000124211L)
				.setIsStar(true)
				.build();
		MarkUserStarResponse response = userService.markUserStar(requestHead, request).get();
		
		assertEquals(MarkUserStarResponse.Result.SUCC, response.getResult());
	}
	
	@Test
	public void testSearchUser() throws Exception {
		SearchUserRequest request = SearchUserRequest.newBuilder()
				.setKeyword("林")
				.build();
		
		SearchUserResponse response = userService.searchUser(requestHead, request).get();
		
		assertTrue(response.getUserCount() > 0);
	}
	
	@Test
	public void testGetUserAbilityTag() throws Exception {
		GetUserAbilityTagRequest request = GetUserAbilityTagRequest.newBuilder()
				.setUserId(10000124212L)
				.build();
		GetUserAbilityTagResponse response = userService.getUserAbilityTag(requestHead, request).get();
		
		// System.out.println(TextFormat.printToUnicodeString(response));
		
		assertTrue(response.getAbilityTagCount() > 0);
	}
	
	@Test
	public void testTagUserAbility() throws Exception {
		
		TagUserAbilityRequest request = TagUserAbilityRequest.newBuilder()
				.setUserId(10000124211L)
				.setTagName("削他!!!")
				.setIsTag(true)
				.build();
		
		TagUserAbilityResponse response = userService.tagUserAbility(requestHead, request).get();
		
		// System.out.println(TextFormat.printToUnicodeString(response));
		
		assertEquals(TagUserAbilityResponse.Result.SUCC, response.getResult());
	}
	
	@Test
	public void testDeleteAbilityTag() throws Exception {
		
		DeleteAbilityTagRequest request = DeleteAbilityTagRequest.newBuilder()
				.addTagName("哈哈")
				.build();
		
		userService.deleteAbilityTag(requestHead, request).get();
	}
	
	@Test
	public void testGetRandomAbilityTagUser() throws Exception {
		
		GetRandomAbilityTagUserRequest request = GetRandomAbilityTagUserRequest.newBuilder()
				.addTagName("小伙儿")
				.setSize(10)
				.build();
		
		userService.getRandomAbilityTagUser(requestHead, request).get();
	}
}
