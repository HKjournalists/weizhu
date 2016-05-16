package com.weizhu.service.company.test;

import static org.junit.Assert.assertTrue;

import org.junit.BeforeClass;
import org.junit.Test;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.weizhu.common.module.TestModule;
import com.weizhu.common.utils.TestUtil;
import com.weizhu.proto.CompanyService;
import com.weizhu.proto.CompanyProtos.VerifyCompanyKeyRequest;
import com.weizhu.proto.CompanyProtos.VerifyCompanyKeyResponse;
import com.weizhu.proto.WeizhuProtos.AnonymousHead;
import com.weizhu.service.company.CompanyServiceLocalModule;

public class CompanyServiceTest {

	static {
		System.setProperty("logback.configurationFile", "com/weizhu/service/company/test/logback.xml");
	}
	
	private static final Injector INJECTOR = Guice.createInjector(new TestModule(), 
			new CompanyServiceLocalModule(), new CompanyServiceTestModule());
	
	@BeforeClass
	public static void init() {
		TestUtil.clearCache(INJECTOR);
		TestUtil.loadTestDataDB(INJECTOR);
	}

	private final AnonymousHead anonymousHead;
	private final CompanyService companyService;
	
	public CompanyServiceTest() {
		this.anonymousHead = INJECTOR.getInstance(AnonymousHead.class);
		this.companyService = INJECTOR.getInstance(CompanyService.class);
	}
	
	@Test
	public void testVerifyCompanyKey() throws Exception {
		
		VerifyCompanyKeyRequest request = VerifyCompanyKeyRequest.newBuilder()
				.setCompanyKey("weizhu")
				.build();
		
		VerifyCompanyKeyResponse response = this.companyService.verifyCompanyKey(anonymousHead, request).get();
		
		assertTrue(response.hasCompany());
	}
}
