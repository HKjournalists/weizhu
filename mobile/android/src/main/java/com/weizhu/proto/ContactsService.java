package com.weizhu.proto;

import com.weizhu.network.Future;
import com.weizhu.proto.ContactsProtos.CreateCustomerRequest;
import com.weizhu.proto.ContactsProtos.CreateCustomerResponse;
import com.weizhu.proto.ContactsProtos.DeleteCustomerRequest;
import com.weizhu.proto.ContactsProtos.DeleteCustomerResponse;
import com.weizhu.proto.ContactsProtos.GetCustomerListRequest;
import com.weizhu.proto.ContactsProtos.GetCustomerListResponse;
import com.weizhu.proto.ContactsProtos.UpdateCustomerRequest;
import com.weizhu.proto.ContactsProtos.UpdateCustomerResponse;

public interface ContactsService {

	@ResponseType(CreateCustomerResponse.class)
	Future<CreateCustomerResponse> createCustomer(CreateCustomerRequest request, int priorityNum);

	@ResponseType(UpdateCustomerResponse.class)
	Future<UpdateCustomerResponse> updateCustomer(UpdateCustomerRequest request, int priorityNum);

	@ResponseType(DeleteCustomerResponse.class)
	Future<DeleteCustomerResponse> deleteCustomer(DeleteCustomerRequest request, int priorityNum);

	@ResponseType(GetCustomerListResponse.class)
	Future<GetCustomerListResponse> getCustomerList(GetCustomerListRequest request, int priorityNum);
}
