package com.weizhu.service.contacts.test;

import static org.junit.Assert.assertEquals;

import org.junit.BeforeClass;
import org.junit.Test;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.weizhu.common.module.TestModule;
import com.weizhu.common.utils.TestUtil;
import com.weizhu.proto.ContactsProtos.CreateCustomerRequest;
import com.weizhu.proto.ContactsProtos.CreateCustomerResponse;
import com.weizhu.proto.ContactsProtos.Customer;
import com.weizhu.proto.ContactsProtos.GetCustomerListResponse;
import com.weizhu.proto.ContactsProtos.UpdateCustomerRequest;
import com.weizhu.proto.ContactsProtos.UpdateCustomerResponse;
import com.weizhu.proto.ContactsService;
import com.weizhu.proto.ContactsProtos.GetCustomerListRequest;
import com.weizhu.proto.WeizhuProtos.RequestHead;
import com.weizhu.service.contacts.ContactsServiceModule;

public class ContactsServiceTest {

	static {
		System.setProperty("logback.configurationFile", "com/weizhu/service/contacts/test/logback.xml");
	}
	
	private static final Injector INJECTOR = Guice.createInjector(new TestModule(), 
			new ContactsServiceTestModule(), new ContactsServiceModule());
	
	@BeforeClass
	public static void init() throws Exception {
		TestUtil.loadTestDataDB(INJECTOR);
	}
	
	private final RequestHead requestHead;
	private final ContactsService contactsService;
	
	public ContactsServiceTest() {
		this.requestHead = INJECTOR.getInstance(RequestHead.class);
		this.contactsService = INJECTOR.getInstance(ContactsService.class);
	}
	
	@Test
	public void testCreateCustomer() throws Exception {
		CreateCustomerRequest request  = CreateCustomerRequest.newBuilder()
				.setCustomer(Customer.newBuilder()
						.setUserId(222)
						.setCustomerId(333)
						.setCustomerName("二货")
						.setMobileNo("15022223333")
						.setIsStar(true)
						.setPosition("大王")
						.build())
				.build();
		
		CreateCustomerResponse response = contactsService.createCustomer(requestHead, request).get();
		
		assertEquals(2, response.getCustomerId());
	}
	
	@Test
	public void testGetAndModifyCustomerList() throws Exception {
		GetCustomerListRequest request = GetCustomerListRequest.newBuilder().build();
		
		GetCustomerListResponse response = contactsService.getCustomerList(requestHead, request).get();
		
		assertEquals(1, response.getCustomerListCount());
		
		Customer customer = response.getCustomerList(0);
		customer = customer.toBuilder().setQq(1234L).build();
		
		UpdateCustomerRequest modifyRequest = UpdateCustomerRequest.newBuilder().setCustomer(customer).build();
		
		UpdateCustomerResponse modifyResponse = contactsService.updateCustomer(requestHead, modifyRequest).get();
		
		assertEquals(UpdateCustomerResponse.Result.SUCC, modifyResponse.getResult());
	}
	
}
