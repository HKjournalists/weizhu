package com.weizhu.service.tools.test;

import java.util.Arrays;

import org.junit.BeforeClass;
import org.junit.Test;

import com.google.common.util.concurrent.Futures;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.weizhu.common.module.FakeExternalServiceModule;
import com.weizhu.common.module.FakeProfileServiceModule;
import com.weizhu.common.module.FakePushServiceModule;
import com.weizhu.common.module.TestModule;
import com.weizhu.common.utils.TestUtil;
import com.weizhu.proto.AdminProtos.AdminHead;
import com.weizhu.proto.ProductclockProtos;
import com.weizhu.proto.ProductclockProtos.GetCustomerListRequest;
import com.weizhu.proto.ProductclockProtos.ImportCustomerRequest;
import com.weizhu.proto.ProductclockProtos.ImportProductRequest;
import com.weizhu.proto.ToolsProductclockService;
import com.weizhu.proto.WeizhuProtos.RequestHead;
import com.weizhu.service.allow.AllowServiceModule;
import com.weizhu.service.official.OfficialServiceModule;
import com.weizhu.service.tools.productcolock.ToolsProductclockServiceModule;
import com.weizhu.service.user.UserServiceModule;
import com.weizhu.service.user.test.UserServiceTestModule;

public class ToolsServiceTest {

	static {
		System.setProperty("logback.configurationFile", "com/weizhu/service/tools/test/logback.xml");
	}
	
	private static final Injector INJECTOR = Guice.createInjector(new TestModule(), 
			new ToolsServiceTestModule(), new ToolsProductclockServiceModule(),
			new UserServiceTestModule(), new UserServiceModule(),
			new FakePushServiceModule(), new FakeProfileServiceModule(), new FakeExternalServiceModule(),
			new AllowServiceModule(), new OfficialServiceModule());
	
	@BeforeClass
	public static void init() {
		TestUtil.clearCache(INJECTOR);
		TestUtil.loadTestDataDB(INJECTOR);
	}

	private final RequestHead head;
	private final AdminHead adminHead;
	private final ToolsProductclockService toolsProductclockService;
	
	public ToolsServiceTest() throws Exception {
		this.head = INJECTOR.getInstance(RequestHead.class);
		this.adminHead = INJECTOR.getInstance(AdminHead.class);
		this.toolsProductclockService = INJECTOR.getInstance(ToolsProductclockService.class);
	}
	
	@Test
	public void testEmpty() {
		Futures.getUnchecked(toolsProductclockService.getCustomerList(head, GetCustomerListRequest.newBuilder()
				.setSize(10)
				.build()));
	}
	
	@Test
	public void testImportProduct() throws Exception {
		ProductclockProtos.Product product = ProductclockProtos.Product.newBuilder()
				.setProductId(0)
				.setProductName("111222")
				.setProductDesc("111122222211111112222")
				.setBuyTime(1)
				.setDefaultRemindDay(1)
				.build();
		
		ProductclockProtos.Product product1 = ProductclockProtos.Product.newBuilder()
				.setProductId(0)
				.setProductName("333334444")
				.setProductDesc("333334444444")
				.setBuyTime(1)
				.setDefaultRemindDay(1)
				.build();
		
		
		ImportProductRequest request = ImportProductRequest.newBuilder()
				.addAllProduct(Arrays.asList(product, product1))
				.build();
		
		Futures.getUnchecked(toolsProductclockService.importProduct(adminHead, request));
	}
	
	@Test
	public void testImportCustomer() throws Exception {
		ProductclockProtos.Customer customer = ProductclockProtos.Customer.newBuilder()
				.setCustomerId(0)
				.setCustomerName("1112222")
				.setIsRemind(true)
				.build();
		
		ProductclockProtos.Customer customer1 = ProductclockProtos.Customer.newBuilder()
				.setCustomerId(0)
				.setCustomerName("33333")
				.setIsRemind(true)
				.build();
		
		
		ImportCustomerRequest request = ImportCustomerRequest.newBuilder()
				.addAllCustomer(Arrays.asList(customer, customer1))
				.build();
		
		Futures.getUnchecked(toolsProductclockService.importCustomer(adminHead, request));
	}
	
}
