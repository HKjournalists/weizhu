package com.weizhu.proto;

import com.google.common.util.concurrent.ListenableFuture;
import com.weizhu.proto.ContactsProtos.CreateCustomerRequest;
import com.weizhu.proto.ContactsProtos.CreateCustomerResponse;
import com.weizhu.proto.ContactsProtos.DeleteCustomerRequest;
import com.weizhu.proto.ContactsProtos.DeleteCustomerResponse;
import com.weizhu.proto.ContactsProtos.GetCustomerListRequest;
import com.weizhu.proto.ContactsProtos.GetCustomerListResponse;
import com.weizhu.proto.ContactsProtos.UpdateCustomerRequest;
import com.weizhu.proto.ContactsProtos.UpdateCustomerResponse;
import com.weizhu.proto.WeizhuProtos.RequestHead;

public interface ContactsService {

	@WriteMethod
	@ResponseType(CreateCustomerResponse.class)
	ListenableFuture<CreateCustomerResponse> createCustomer(RequestHead head, CreateCustomerRequest request);

	@WriteMethod
	@ResponseType(UpdateCustomerResponse.class)
	ListenableFuture<UpdateCustomerResponse> updateCustomer(RequestHead head, UpdateCustomerRequest request);

	@WriteMethod
	@ResponseType(DeleteCustomerResponse.class)
	ListenableFuture<DeleteCustomerResponse> deleteCustomer(RequestHead head, DeleteCustomerRequest request);

	@ResponseType(GetCustomerListResponse.class)
	ListenableFuture<GetCustomerListResponse> getCustomerList(RequestHead head, GetCustomerListRequest request);
}
