package com.weizhu.proto;

import com.google.common.util.concurrent.ListenableFuture;
import com.weizhu.proto.AdminProtos.AdminHead;
import com.weizhu.proto.ProductclockProtos.AssignedSalerRequest;
import com.weizhu.proto.ProductclockProtos.AssignedSalerResponse;
import com.weizhu.proto.ProductclockProtos.CreateCommunicateRecordRequest;
import com.weizhu.proto.ProductclockProtos.CreateCommunicateRecordResponse;
import com.weizhu.proto.ProductclockProtos.CreateCustomerProductRequest;
import com.weizhu.proto.ProductclockProtos.CreateCustomerProductResponse;
import com.weizhu.proto.ProductclockProtos.CreateCustomerRequest;
import com.weizhu.proto.ProductclockProtos.CreateCustomerResponse;
import com.weizhu.proto.ProductclockProtos.CreateProductRequest;
import com.weizhu.proto.ProductclockProtos.CreateProductResponse;
import com.weizhu.proto.ProductclockProtos.DeleteCommunicateRecordRequest;
import com.weizhu.proto.ProductclockProtos.DeleteCommunicateRecordResponse;
import com.weizhu.proto.ProductclockProtos.DeleteCustomerProductRequest;
import com.weizhu.proto.ProductclockProtos.DeleteCustomerProductResponse;
import com.weizhu.proto.ProductclockProtos.DeleteCustomerRequest;
import com.weizhu.proto.ProductclockProtos.DeleteCustomerResponse;
import com.weizhu.proto.ProductclockProtos.DeleteProductRequest;
import com.weizhu.proto.ProductclockProtos.DeleteProductResponse;
import com.weizhu.proto.ProductclockProtos.GetCommunicateRecordAdminRequest;
import com.weizhu.proto.ProductclockProtos.GetCommunicateRecordAdminResponse;
import com.weizhu.proto.ProductclockProtos.GetCommunicateRecordRequest;
import com.weizhu.proto.ProductclockProtos.GetCommunicateRecordResponse;
import com.weizhu.proto.ProductclockProtos.GetCustomerAdminRequest;
import com.weizhu.proto.ProductclockProtos.GetCustomerAdminResponse;
import com.weizhu.proto.ProductclockProtos.GetCustomerByIdRequest;
import com.weizhu.proto.ProductclockProtos.GetCustomerByIdResponse;
import com.weizhu.proto.ProductclockProtos.GetCustomerListRequest;
import com.weizhu.proto.ProductclockProtos.GetCustomerListResponse;
import com.weizhu.proto.ProductclockProtos.GetCustomerProductRequest;
import com.weizhu.proto.ProductclockProtos.GetCustomerProductResponse;
import com.weizhu.proto.ProductclockProtos.GetProductAdminRequest;
import com.weizhu.proto.ProductclockProtos.GetProductAdminResponse;
import com.weizhu.proto.ProductclockProtos.GetProductListRequest;
import com.weizhu.proto.ProductclockProtos.GetProductListResponse;
import com.weizhu.proto.ProductclockProtos.ImportCustomerRequest;
import com.weizhu.proto.ProductclockProtos.ImportCustomerResponse;
import com.weizhu.proto.ProductclockProtos.ImportProductRequest;
import com.weizhu.proto.ProductclockProtos.ImportProductResponse;
import com.weizhu.proto.ProductclockProtos.UpdateCommunicateRecordRequest;
import com.weizhu.proto.ProductclockProtos.UpdateCommunicateRecordResponse;
import com.weizhu.proto.ProductclockProtos.UpdateCustomerProductRequest;
import com.weizhu.proto.ProductclockProtos.UpdateCustomerProductResponse;
import com.weizhu.proto.ProductclockProtos.UpdateCustomerRequest;
import com.weizhu.proto.ProductclockProtos.UpdateCustomerResponse;
import com.weizhu.proto.ProductclockProtos.UpdateProductRequest;
import com.weizhu.proto.ProductclockProtos.UpdateProductResponse;
import com.weizhu.proto.WeizhuProtos.RequestHead;

public interface ToolsProductclockService {

	/**client**/
	@ResponseType(GetCustomerByIdResponse.class)
	ListenableFuture<GetCustomerByIdResponse> getCustomerById(RequestHead head, GetCustomerByIdRequest request);
	
	@ResponseType(GetCustomerListResponse.class)
	ListenableFuture<GetCustomerListResponse> getCustomerList(RequestHead head, GetCustomerListRequest request);
	
	@ResponseType(GetProductListResponse.class)
	ListenableFuture<GetProductListResponse> getProductList(RequestHead head, GetProductListRequest request);
	
	@ResponseType(GetCustomerProductResponse.class)
	ListenableFuture<GetCustomerProductResponse> getCustomerProduct(RequestHead head, GetCustomerProductRequest request);
	
	@ResponseType(GetCommunicateRecordResponse.class)
	ListenableFuture<GetCommunicateRecordResponse> getCommunicateRecord(RequestHead head, GetCommunicateRecordRequest request);
	
	@WriteMethod
	@ResponseType(CreateCommunicateRecordResponse.class)
	ListenableFuture<CreateCommunicateRecordResponse> createCommunicateRecord(RequestHead head, CreateCommunicateRecordRequest request);
	
	@WriteMethod
	@ResponseType(UpdateCommunicateRecordResponse.class)
	ListenableFuture<UpdateCommunicateRecordResponse> updateCommunicateRecord(RequestHead head, UpdateCommunicateRecordRequest request);
	
	@WriteMethod
	@ResponseType(DeleteCommunicateRecordResponse.class)
	ListenableFuture<DeleteCommunicateRecordResponse> deleteCommunicateRecord(RequestHead head, DeleteCommunicateRecordRequest request);
	
	
	/**common**/
	@WriteMethod
	@ResponseType(CreateCustomerResponse.class)
	ListenableFuture<CreateCustomerResponse> createCustomer(RequestHead head, CreateCustomerRequest request);
	
	@WriteMethod
	@ResponseType(UpdateCustomerResponse.class)
	ListenableFuture<UpdateCustomerResponse> updateCustomer(RequestHead head, UpdateCustomerRequest request);
	
	@WriteMethod
	@ResponseType(DeleteCustomerResponse.class)
	ListenableFuture<DeleteCustomerResponse> deleteCustomer(RequestHead head, DeleteCustomerRequest request);
	
	@WriteMethod
	@ResponseType(CreateCustomerProductResponse.class)
	ListenableFuture<CreateCustomerProductResponse> createCustomerProduct(RequestHead head, CreateCustomerProductRequest request);
	
	@WriteMethod
	@ResponseType(UpdateCustomerProductResponse.class)
	ListenableFuture<UpdateCustomerProductResponse> updateCustomerProduct(RequestHead head, UpdateCustomerProductRequest request);
	
	@WriteMethod
	@ResponseType(DeleteCustomerProductResponse.class)
	ListenableFuture<DeleteCustomerProductResponse> deleteCustomerProduct(RequestHead head, DeleteCustomerProductRequest request);
	@WriteMethod
	@ResponseType(CreateCustomerResponse.class)
	ListenableFuture<CreateCustomerResponse> createCustomer(AdminHead head, CreateCustomerRequest request);
	
	@WriteMethod
	@ResponseType(UpdateCustomerResponse.class)
	ListenableFuture<UpdateCustomerResponse> updateCustomer(AdminHead head, UpdateCustomerRequest request);
	
	@WriteMethod
	@ResponseType(DeleteCustomerResponse.class)
	ListenableFuture<DeleteCustomerResponse> deleteCustomer(AdminHead head, DeleteCustomerRequest request);
	
	@ResponseType(GetCustomerProductResponse.class)
	ListenableFuture<GetCustomerProductResponse> getCustomerProduct(AdminHead head, GetCustomerProductRequest request);
	
	@WriteMethod
	@ResponseType(CreateCustomerProductResponse.class)
	ListenableFuture<CreateCustomerProductResponse> createCustomerProduct(AdminHead head, CreateCustomerProductRequest request);
	
	@WriteMethod
	@ResponseType(UpdateCustomerProductResponse.class)
	ListenableFuture<UpdateCustomerProductResponse> updateCustomerProduct(AdminHead head, UpdateCustomerProductRequest request);
	
	@WriteMethod
	@ResponseType(DeleteCustomerProductResponse.class)
	ListenableFuture<DeleteCustomerProductResponse> deleteCustomerProduct(AdminHead head, DeleteCustomerProductRequest request);
	
	
	/**admin**/
	@ResponseType(GetCustomerAdminResponse.class)
	ListenableFuture<GetCustomerAdminResponse> getCustomerAdmin(AdminHead head, GetCustomerAdminRequest request);
	
	@WriteMethod
	@ResponseType(ImportCustomerResponse.class)
	ListenableFuture<ImportCustomerResponse> importCustomer(AdminHead head, ImportCustomerRequest request);

	@WriteMethod
	@ResponseType(AssignedSalerResponse.class)
	ListenableFuture<AssignedSalerResponse> assignedSaler(AdminHead head, AssignedSalerRequest request);

	@ResponseType(GetProductAdminResponse.class)
	ListenableFuture<GetProductAdminResponse> getProductAdmin(AdminHead head, GetProductAdminRequest request);

	@WriteMethod
	@ResponseType(CreateProductResponse.class)
	ListenableFuture<CreateProductResponse> createProduct(AdminHead head, CreateProductRequest request);

	@WriteMethod
	@ResponseType(UpdateProductResponse.class)
	ListenableFuture<UpdateProductResponse> updateProduct(AdminHead head, UpdateProductRequest request);

	@WriteMethod
	@ResponseType(DeleteProductResponse.class)
	ListenableFuture<DeleteProductResponse> deleteProduct(AdminHead head, DeleteProductRequest request);

	@WriteMethod
	@ResponseType(ImportProductResponse.class)
	ListenableFuture<ImportProductResponse> importProduct(AdminHead head, ImportProductRequest request);
	
	@ResponseType(GetCommunicateRecordAdminResponse.class)
	ListenableFuture<GetCommunicateRecordAdminResponse> getCommunicateRecordAdmin(AdminHead head, GetCommunicateRecordAdminRequest request);

}
