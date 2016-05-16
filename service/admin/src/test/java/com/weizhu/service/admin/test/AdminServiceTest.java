package com.weizhu.service.admin.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.BeforeClass;
import org.junit.Test;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.weizhu.common.module.FakeExternalServiceModule;
import com.weizhu.common.module.TestModule;
import com.weizhu.common.utils.TestUtil;
import com.weizhu.proto.AdminProtos.AdminAnonymousHead;
import com.weizhu.proto.AdminProtos.AdminHead;
import com.weizhu.proto.AdminProtos.AdminLoginRequest;
import com.weizhu.proto.AdminProtos.AdminLoginResponse;
import com.weizhu.proto.AdminProtos.AdminVerifySessionRequest;
import com.weizhu.proto.AdminProtos.AdminVerifySessionResponse;
import com.weizhu.proto.AdminProtos.CreateAdminRequest;
import com.weizhu.proto.AdminProtos.CreateAdminResponse;
import com.weizhu.proto.AdminProtos.GetAdminListRequest;
import com.weizhu.proto.AdminProtos.GetAdminListResponse;
import com.weizhu.proto.AdminProtos.UpdateAdminRequest;
import com.weizhu.proto.AdminProtos.UpdateAdminResponse;
import com.weizhu.proto.AdminService;
import com.weizhu.service.admin.AdminServiceModule;
import com.weizhu.service.company.CompanyServiceLocalModule;
import com.weizhu.service.company.test.CompanyServiceTestModule;

public class AdminServiceTest {
	
	static {
		System.setProperty("logback.configurationFile", "com/weizhu/service/admin/test/logback.xml");
	}
	
	private static final Injector INJECTOR = Guice.createInjector(new TestModule(), 
			new AdminServiceTestModule(), new AdminServiceModule(),
			new CompanyServiceTestModule(), new CompanyServiceLocalModule(), 
			new FakeExternalServiceModule());
	
	@BeforeClass
	public static void init() {
		TestUtil.clearCache(INJECTOR);
		TestUtil.loadTestDataDB(INJECTOR);
	}

	private final AdminAnonymousHead adminAnonymousHead;
	private final AdminHead adminHead;
	private final AdminService adminService;
	
	public AdminServiceTest() {
		this.adminAnonymousHead = INJECTOR.getInstance(AdminAnonymousHead.class);
		this.adminHead = INJECTOR.getInstance(AdminHead.class);
		this.adminService = INJECTOR.getInstance(AdminService.class);
	}
	
	@Test
	public void testAdminLoginFail() throws Exception {
		AdminLoginRequest request = AdminLoginRequest.newBuilder()
				.setAdminEmail("root@weizhu.com")
				.setAdminPassword("")
				.build();
		AdminLoginResponse response = adminService.adminLogin(adminAnonymousHead, request).get();
		
		assertEquals(AdminLoginResponse.Result.FAIL_PASSWORD_INVALID, response.getResult());
	}
	
	@Test
	public void testAdminLoginSucc() throws Exception {
		AdminLoginRequest request = AdminLoginRequest.newBuilder()
				.setAdminEmail("root@weizhu.com")
				.setAdminPassword("123abcABC")
				.build();
		AdminLoginResponse response = adminService.adminLogin(adminAnonymousHead, request).get();
		
		assertEquals(AdminLoginResponse.Result.SUCC, response.getResult());
		
		// System.out.println("admin : " + response.getAdmin());
		// System.out.println("session key : " + response.getSessionKey());
		// System.out.println("first login : " + response.getFirstLogin());
	}
	
	@Test
	public void testVerifySessionKey() throws Exception {
		
		AdminLoginRequest loginRequest = AdminLoginRequest.newBuilder()
				.setAdminEmail("root@weizhu.com")
				.setAdminPassword("123abcABC")
				.build();
		AdminLoginResponse loginResponse = adminService.adminLogin(adminAnonymousHead, loginRequest).get();
		
		assertEquals(AdminLoginResponse.Result.SUCC, loginResponse.getResult());
		
		// System.out.println("session key : " + loginResponse.getSessionKey());
		
		AdminVerifySessionRequest request = AdminVerifySessionRequest.newBuilder()
				.setSessionKey(loginResponse.getSessionKey())
				.build();
		
		AdminVerifySessionResponse response = adminService.adminVerifySession(adminAnonymousHead, request).get();
		
		assertEquals(AdminVerifySessionResponse.Result.SUCC, response.getResult());
		assertEquals(1, response.getSession().getAdminId());
	}
	
	@Test
	public void testCreateAdmin() throws Exception {
		
		CreateAdminRequest request = CreateAdminRequest.newBuilder()
				.setAdminName("hahahhhh")
				.setAdminEmail("xxxx@yyy.com")
				.setAdminPassword("567bcd!@#")
				.addRoleId(1)
				.setEnableTeamPermit(false)
				.build();
		
		CreateAdminResponse response = adminService.createAdmin(adminHead, request).get();
		
		assertEquals(CreateAdminResponse.Result.SUCC, response.getResult());
		
		// System.out.println("create admin id : " + response.getAdminId());
	}
	
	@Test
	public void testUpdateAdmin() throws Exception {
		
		// System.out.println(adminService.getAdminById(adminHead, com.weizhu.proto.AdminProtos.GetAdminByIdRequest.newBuilder().addAdminId(3).build()).get());
		
		UpdateAdminRequest request = UpdateAdminRequest.newBuilder()
				.setAdminId(3)
				.setAdminName("测试管理员aaa")
				.setAdminEmail("aaa@weizhu.com")
				.setForceResetPassword(true)
				.setEnableTeamPermit(true)
				.addPermitTeamId(100)
				.addPermitTeamId(101)
				.addPermitTeamId(102)
				.build();
		
		UpdateAdminResponse response = adminService.updateAdmin(adminHead, request).get();
		
		assertEquals(UpdateAdminResponse.Result.SUCC, response.getResult());
		
		// System.out.println(adminService.getAdminById(adminHead, com.weizhu.proto.AdminProtos.GetAdminByIdRequest.newBuilder().addAdminId(3).build()).get());
	}
	
	@Test
	public void testGetAdminList() throws Exception {
		
		GetAdminListRequest request = GetAdminListRequest.newBuilder()
				.setStart(0)
				.setLength(10)
				.build();
		
		GetAdminListResponse response = adminService.getAdminList(adminHead, request).get();
		
		assertTrue(response.getAdminCount() > 0);
		assertTrue(response.getTotalSize() > 0);
		assertTrue(response.getFilteredSize() > 0);
		
//		System.out.println("list: " + response.toString());
	}
	
}
