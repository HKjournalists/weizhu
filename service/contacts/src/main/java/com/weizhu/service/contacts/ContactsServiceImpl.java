package com.weizhu.service.contacts;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.inject.Inject;
import com.weizhu.common.db.DBUtil;
import com.weizhu.common.utils.MobileNoUtil;
import com.weizhu.proto.ContactsProtos.CreateCustomerRequest;
import com.weizhu.proto.ContactsProtos.CreateCustomerResponse;
import com.weizhu.proto.ContactsProtos.DeleteCustomerRequest;
import com.weizhu.proto.ContactsProtos.DeleteCustomerResponse;
import com.weizhu.proto.ContactsProtos.GetCustomerListRequest;
import com.weizhu.proto.ContactsProtos.GetCustomerListResponse;
import com.weizhu.proto.ContactsProtos;
import com.weizhu.proto.ContactsProtos.UpdateCustomerRequest;
import com.weizhu.proto.ContactsProtos.UpdateCustomerResponse;
import com.weizhu.proto.ContactsService;
import com.weizhu.proto.WeizhuProtos.RequestHead;
import com.zaxxer.hikari.HikariDataSource;

public class ContactsServiceImpl implements ContactsService {

	// private static final Logger logger = LoggerFactory.getLogger(ContactsServiceImpl.class);
	
	private final HikariDataSource hikariDataSource;
	
	@Inject
	public ContactsServiceImpl(HikariDataSource hikariDataSource) {
		this.hikariDataSource = hikariDataSource;
	}

	@Override
	public ListenableFuture<CreateCustomerResponse> createCustomer(RequestHead head, CreateCustomerRequest request) {
		if (!MobileNoUtil.isValid(request.getCustomer().getMobileNo())) {
			return Futures.immediateFuture(CreateCustomerResponse.newBuilder()
					.setResult(CreateCustomerResponse.Result.FAIL_MOBILE_NO_INVALID)
					.setFailText("手机号格式不正确")
					.build());
		}
		
		ContactsProtos.Customer customer = request.getCustomer().toBuilder()
				.setUserId(head.getSession().getUserId())
				.setMobileNo(MobileNoUtil.adjustMobileNo(request.getCustomer().getMobileNo()))
				.build();
		
		Connection dbConn = null;
		try {
			dbConn = hikariDataSource.getConnection();
			
			int customerId = ContactsDB.insertCustomer(dbConn, customer);
			
			return Futures.immediateFuture(CreateCustomerResponse.newBuilder()
					.setResult(CreateCustomerResponse.Result.SUCC)
					.setCustomerId(customerId)
					.build());
			
		} catch (SQLException e) {
			throw new RuntimeException("createCustomer db fail!", e);
		}  finally {
			DBUtil.closeQuietly(dbConn);
		}
	}

	@Override
	public ListenableFuture<UpdateCustomerResponse> updateCustomer(RequestHead head, UpdateCustomerRequest request) {
		if (!MobileNoUtil.isValid(request.getCustomer().getMobileNo())) {
			return Futures.immediateFuture(UpdateCustomerResponse.newBuilder()
					.setResult(UpdateCustomerResponse.Result.FAIL_MOBILE_NO_INVALID)
					.setFailText("手机号格式不正确")
					.build());
		}
		
		ContactsProtos.Customer customer = request.getCustomer().toBuilder()
				.setUserId(head.getSession().getUserId())
				.setMobileNo(MobileNoUtil.adjustMobileNo(request.getCustomer().getMobileNo()))
				.build();
		
		Connection dbConn = null;
		try {
			dbConn = hikariDataSource.getConnection();
			
			boolean succ = ContactsDB.updateCustomer(dbConn, customer);
			
			if (succ) {
				return Futures.immediateFuture(UpdateCustomerResponse.newBuilder()
						.setResult(UpdateCustomerResponse.Result.SUCC)
						.build());
			} else {
				return Futures.immediateFuture(UpdateCustomerResponse.newBuilder()
						.setResult(UpdateCustomerResponse.Result.FAIL_CUSTOMER_NOT_EXIST)
						.build());
			}
			
		} catch (SQLException e) {
			throw new RuntimeException("createCustomer db fail!", e);
		}  finally {
			DBUtil.closeQuietly(dbConn);
		}
	}

	@Override
	public ListenableFuture<DeleteCustomerResponse> deleteCustomer(RequestHead head, DeleteCustomerRequest request) {

		Connection dbConn = null;
		try {
			dbConn = hikariDataSource.getConnection();
			
			ContactsDB.deleteCustomer(dbConn, head.getSession().getUserId(), request.getCustomerIdList());
			
		} catch (SQLException e) {
			throw new RuntimeException("deleteCustomer db fail!", e);
		}  finally {
			DBUtil.closeQuietly(dbConn);
		}
		
		return Futures.immediateFuture(DeleteCustomerResponse.newBuilder().build());
	}

	@Override
	public ListenableFuture<GetCustomerListResponse> getCustomerList(RequestHead head, GetCustomerListRequest request) {
		List<ContactsProtos.Customer> list;
		Connection dbConn = null;
		try {
			dbConn = hikariDataSource.getConnection();
			
			list = ContactsDB.getCustomerList(dbConn, head.getSession().getUserId());
			
		} catch (SQLException e) {
			throw new RuntimeException("getCustomerList db fail!", e);
		}  finally {
			DBUtil.closeQuietly(dbConn);
		}
		
		return Futures.immediateFuture(GetCustomerListResponse.newBuilder()
				.addAllCustomerList(list)
				.build());
	}

}
