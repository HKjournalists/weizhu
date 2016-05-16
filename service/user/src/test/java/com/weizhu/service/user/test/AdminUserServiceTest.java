package com.weizhu.service.user.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.BeforeClass;
import org.junit.Test;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.protobuf.TextFormat;
import com.weizhu.common.module.FakeExternalServiceModule;
import com.weizhu.common.module.FakeProfileServiceModule;
import com.weizhu.common.module.FakePushServiceModule;
import com.weizhu.common.module.TestModule;
import com.weizhu.common.utils.TestUtil;
import com.weizhu.proto.AdminUserService;
import com.weizhu.proto.AdminProtos.AdminHead;
import com.weizhu.proto.AdminUserProtos.CreateUserRequest;
import com.weizhu.proto.AdminUserProtos.CreateUserResponse;
import com.weizhu.proto.AdminUserProtos.ImportUserRequest;
import com.weizhu.proto.AdminUserProtos.ImportUserResponse;
import com.weizhu.proto.AdminUserProtos.RawUser;
import com.weizhu.proto.AdminUserProtos.RawUserExtends;
import com.weizhu.proto.AdminUserProtos.RawUserTeam;
import com.weizhu.proto.AdminUserProtos.UpdateUserRequest;
import com.weizhu.proto.AdminUserProtos.UpdateUserResponse;
import com.weizhu.proto.UserProtos.UserTeam;
import com.weizhu.service.user.UserServiceModule;

public class AdminUserServiceTest {
	
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
	
	private final AdminHead adminHead;
	private final AdminUserService adminUserService;
	
	public AdminUserServiceTest() {
		this.adminHead = INJECTOR.getInstance(AdminHead.class);
		this.adminUserService = INJECTOR.getInstance(AdminUserService.class);
	}
	
	@Test
	public void testImportUser() throws Exception {
		ImportUserRequest request = ImportUserRequest.newBuilder()
				.addRawUser(RawUser.newBuilder()
						.setRawId("A")
						.setUserName("我是A")
						.addMobileNo("12300000000")
						.addPhoneNo("010-1234567")
						.setLevel("职级1")
						.addUserTeam(RawUserTeam.newBuilder()
								.setPosition("职位1")
								.addTeam("公司")
								.addTeam("部门")
								.addTeam("小组")
								.build())
						.addAbilityTag("哈哈")
						.addAbilityTag("大猪头")
						.addUserExts(RawUserExtends.newBuilder()
								.setName("测试").setValue("猪头")
								.build())
						.build())
				.build();
		
		ImportUserResponse response = adminUserService.importUser(adminHead, request).get();
		
		assertEquals(ImportUserResponse.Result.SUCC, response.getResult());
		assertTrue(response.getInvalidUserCount() <= 0);
	}
	
	@Test
	public void testCreateUser() throws Exception {
		long begin = System.currentTimeMillis();
		
		CreateUserRequest request = CreateUserRequest.newBuilder()
				.setRawId("AAAAAA")
				.setUserName("测试用户")
				.addMobileNo("13412341234")
				.addMobileNo("15643214321")
				.setLevelId(1)
				.addUserTeam(UserTeam.newBuilder()
						.setUserId(-1)
						.setTeamId(4)
						.setPositionId(8)
						.build())
				.build();
		
		CreateUserResponse response = adminUserService.createUser(adminHead, request).get();
		
		System.out.println("time : " + (System.currentTimeMillis() - begin) + "(ms)\n" + TextFormat.printToString(response));
		
		assertEquals(CreateUserResponse.Result.SUCC, response.getResult());
	}
	
	@Test
	public void testUpdateUser() throws Exception {
		UpdateUserRequest request = UpdateUserRequest.newBuilder()
				.setUserId(10000124196L)
				.setUserName("林栋aaa")
				.addMobileNo("18601191171")
				.addMobileNo("15700000000")
				.setEmail("lindongjlu@qq.com")
				.build();
		
		UpdateUserResponse response = adminUserService.updateUser(adminHead, request).get();
		
		assertEquals(UpdateUserResponse.Result.SUCC, response.getResult());
	}
	
}
