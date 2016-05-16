package com.weizhu.service.tools.productcolock;

import java.sql.Connection;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import com.weizhu.common.db.DBUtil;
import com.weizhu.common.utils.DataPage;
import com.weizhu.common.utils.MobileNoUtil;
import com.weizhu.proto.AdminOfficialProtos;
import com.weizhu.proto.AdminOfficialService;
import com.weizhu.proto.AdminProtos.AdminHead;
import com.weizhu.proto.AdminUserProtos.GetUserByIdRequest;
import com.weizhu.proto.AdminUserProtos.GetUserByIdResponse;
import com.weizhu.proto.AdminUserService;
import com.weizhu.proto.OfficialProtos;
import com.weizhu.proto.ProductclockProtos;
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
import com.weizhu.proto.ProductclockProtos.Gender;
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
import com.weizhu.proto.ToolsProductclockService;
import com.weizhu.proto.UserProtos;
import com.weizhu.proto.WeizhuProtos.RequestHead;
import com.weizhu.proto.WeizhuProtos.SystemHead;
import com.weizhu.service.tools.ToolsDAOProtos;
import com.weizhu.service.tools.ToolsDAOProtos.ProductclockCustomerProduct;
import com.zaxxer.hikari.HikariDataSource;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

@Singleton
public class ToolsProductclockServiceImpl implements ToolsProductclockService {

	private static final Logger logger = LoggerFactory.getLogger(ToolsProductclockServiceImpl.class);
	
	private final HikariDataSource hikariDataSource;
	private final JedisPool jedisPool;
	private final AdminUserService adminUserService;
	private final AdminOfficialService adminOfficialService;
	private final Executor executorService;
	private final ScheduledExecutorService scheduledExecutorService;
	
	@Inject
	public ToolsProductclockServiceImpl(HikariDataSource hikariDataSource, JedisPool jedisPool,
			AdminUserService adminUserService, AdminOfficialService adminOfficialService,
			@Named("service_executor") Executor executorService, @Named("service_scheduled_executor") ScheduledExecutorService scheduledExecutorService) {
		this.hikariDataSource = hikariDataSource;
		this.jedisPool = jedisPool;
		this.adminUserService = adminUserService;
		this.adminOfficialService = adminOfficialService;
		this.executorService = executorService;
		this.scheduledExecutorService = scheduledExecutorService;
		
		this.doLoadRemindCustomer();
	}

	@Override
	public ListenableFuture<GetCustomerByIdResponse> getCustomerById(RequestHead head, GetCustomerByIdRequest request) {
		final List<Integer> customerIdList = request.getCustomerIdList();
		if (customerIdList.isEmpty()) {
			return Futures.immediateFuture(GetCustomerByIdResponse.getDefaultInstance());
		}
		
		final long companyId = head.getSession().getCompanyId();
		
		Map<Integer, ProductclockProtos.Customer> customerMap = ToolsProductclockUtil.getCustomer(hikariDataSource, jedisPool, companyId, customerIdList);
		
		return Futures.immediateFuture(GetCustomerByIdResponse.newBuilder()
				.addAllCustomer(customerMap.values())
				.build());
	}
	
	@Override
	public ListenableFuture<GetCustomerListResponse> getCustomerList(RequestHead head, GetCustomerListRequest request) {
		final ByteString data = request.hasOffsetIndex() ? request.getOffsetIndex() : null;
		Integer customerId = null;
		Integer createTime = null;
		ToolsDAOProtos.ProductclockCustomerListIndex offsetIndex = null;
		if (data != null) {
			try {
				offsetIndex = ToolsDAOProtos.ProductclockCustomerListIndex.parseFrom(data);
				customerId = offsetIndex.getCustomerId();
				createTime = offsetIndex.getCreateTime();
			} catch (InvalidProtocolBufferException e) {

			}
		}
		
		final long companyId = head.getSession().getCompanyId();
		final long userId = head.getSession().getUserId();
		final int size = request.getSize() < 0 ? 0 : request.getSize() > 50 ? 50 : request.getSize();
		if (size == 0) {
			return Futures.immediateFuture(GetCustomerListResponse.newBuilder()
					.setOffsetIndex(ByteString.EMPTY)
					.setHasMore(false)
					.build());
		}
		final Boolean hasProduct = request.hasHasProduct() ? request.getHasProduct() : null;
		
		List<Integer> userAllCustomerId = null;
		Connection conn = null;
		try {
			conn = this.hikariDataSource.getConnection();
			
			userAllCustomerId = ToolsProductclockDB.getCustomerIdByUserId(conn, companyId, userId, hasProduct);
		} catch (SQLException ex) {
			throw new RuntimeException("db fail", ex);
		} finally {
			DBUtil.closeQuietly(conn);
		}
		
		Map<Integer, ProductclockProtos.Customer> allCustomerMap = ToolsProductclockUtil.getCustomer(hikariDataSource, jedisPool, companyId, userAllCustomerId);
		
		Map<Integer, ProductclockProtos.Customer> todayRemindCustomerMap = Maps.newTreeMap();
		
		if (customerId == null || createTime == null) {
			Map<Integer, List<ProductclockCustomerProduct>> customerProductMap = null;
			try {
				conn = this.hikariDataSource.getConnection();
				
				customerProductMap = ToolsProductclockDB.getCustomerProduct(conn, companyId, allCustomerMap.keySet());
			} catch (SQLException ex) {
				throw new RuntimeException("db fail", ex);
			} finally {
				DBUtil.closeQuietly(conn);
			}
			
			long now = System.currentTimeMillis();
			
			Map<Integer, List<Integer>> notifyCustomerProductMap = Maps.newHashMap();
			for (Entry<Integer, List<ToolsDAOProtos.ProductclockCustomerProduct>> entry : customerProductMap.entrySet()) {
				List<ToolsDAOProtos.ProductclockCustomerProduct> customerProductList = entry.getValue();
				for (ToolsDAOProtos.ProductclockCustomerProduct customerProduct : customerProductList) {
					int cid = customerProduct.getCustomerId();
					
					ProductclockProtos.Customer customer = allCustomerMap.get(cid);
					if (customer == null) {
						break;
					}
					int daysAgoRemind = customer.hasDaysAgoRemind() ? (customer.getDaysAgoRemind() < 0 ? 0 : customer.getDaysAgoRemind()) : 0;
					
					int interval = ((int)(now/1000L) - customerProduct.getBuyTime())/(24*3600);
					int remindPeriodDay = customerProduct.getRemindPeriodDay() < 1 ? 1 : customerProduct.getRemindPeriodDay();
					if ((interval%remindPeriodDay == daysAgoRemind) && (interval != 0)) {
						
						List<Integer> productIdList = notifyCustomerProductMap.get(cid);
						if (productIdList == null) {
							productIdList = Lists.newArrayList();
						}
						productIdList.add(customerProduct.getProductId());
						
						notifyCustomerProductMap.put(cid, productIdList);
					}
				}
			}
			
			SimpleDateFormat df = new SimpleDateFormat("MM-dd");
			String date = df.format(new Date(now));
			String lunarDate = LunarUtil.solarTolunarTimeStamp((int)(now/1000L)).substring(5, 10);
			
			for (Entry<Integer, ProductclockProtos.Customer> entry : allCustomerMap.entrySet()) {
				ProductclockProtos.Customer customer = entry.getValue();
				if (!customer.getIsRemind()) {
					continue;
				}
				
				int daysAgoRemind = (customer.hasDaysAgoRemind() ? (customer.getDaysAgoRemind() < 0 ? 0 : customer.getDaysAgoRemind()) : 0) * 24 * 3600;
				
				if (customer.hasBirthdaySolar() && date.equals(df.format(new Date((customer.getBirthdaySolar() - daysAgoRemind) * 1000L)))) {
					todayRemindCustomerMap.put(entry.getKey(), ProductclockProtos.Customer.newBuilder()
							.mergeFrom(customer)
							.setIsRemindToday(true)
							.build());
				} else if (customer.hasBirthdayLunar() && lunarDate.equals(LunarUtil.solarTolunarTimeStamp(customer.getBirthdayLunar() - daysAgoRemind).substring(5, 10))) {
					todayRemindCustomerMap.put(entry.getKey(), ProductclockProtos.Customer.newBuilder()
							.mergeFrom(customer)
							.setIsRemindToday(true)
							.build());
				}
				
				if (customer.hasWeddingSolar() && date.equals(df.format(new Date((customer.getWeddingSolar() - daysAgoRemind) * 1000L)))) {
					todayRemindCustomerMap.put(entry.getKey(), ProductclockProtos.Customer.newBuilder()
							.mergeFrom(customer)
							.setIsRemindToday(true)
							.build());
				} else if (customer.hasWeddingLunar() && lunarDate.equals(LunarUtil.solarTolunarTimeStamp(customer.getWeddingLunar() - daysAgoRemind).substring(5, 10))) {
					todayRemindCustomerMap.put(entry.getKey(), ProductclockProtos.Customer.newBuilder()
							.mergeFrom(customer)
							.setIsRemindToday(true)
							.build());
				}
				
				List<Integer> productIdList = notifyCustomerProductMap.get(customer.getCustomerId());
				if (productIdList != null && !productIdList.isEmpty()) {
					todayRemindCustomerMap.put(entry.getKey(), ProductclockProtos.Customer.newBuilder()
							.mergeFrom(customer)
							.setIsRemindToday(true)
							.build());
				}
			}
		}
		
		List<Integer> customerIdList = null;
		
		try {
			conn = this.hikariDataSource.getConnection();
			
			customerIdList = ToolsProductclockDB.getCustomerIdList(conn, companyId, userId, customerId, createTime, size+1, hasProduct);
		} catch (SQLException ex) {
			throw new RuntimeException("db fail", ex);
		} finally {
			DBUtil.closeQuietly(conn);
		}
		
		List<ProductclockProtos.Customer> customerList = Lists.newArrayList();
		for (int ctmId : customerIdList) {
			ProductclockProtos.Customer customer = allCustomerMap.get(ctmId);
			if (customer != null && todayRemindCustomerMap.get(ctmId) == null) {
				customerList.add(customer);
			}
		}
		
		boolean hasMore = customerList.size() > size;
		
		ProductclockProtos.Customer lastCustomer;
		if (hasMore) {
			lastCustomer = customerList.get(size-1);
		} else {
			if (customerList.isEmpty()) {
				lastCustomer = null;
			} else {
				lastCustomer = customerList.get(customerList.size()-1);
			}
		}
		
		ToolsDAOProtos.ProductclockCustomerListIndex index = null;
		if (lastCustomer != null) {
			index = ToolsDAOProtos.ProductclockCustomerListIndex.newBuilder()
					.setCustomerId(lastCustomer.getCustomerId())
					.setCreateTime(lastCustomer.getCreateTime())
					.build();
		} else {
			index = ToolsDAOProtos.ProductclockCustomerListIndex.getDefaultInstance();
		}
		
		List<ProductclockProtos.Customer> resultList = Lists.newArrayList(todayRemindCustomerMap.values());
		resultList.addAll(hasMore ? customerList.subList(0, size) : customerList);
		
		return Futures.immediateFuture(GetCustomerListResponse.newBuilder()
				.addAllCustomer(resultList)
				.setOffsetIndex(index.toByteString())
				.setHasMore(hasMore)
				.build());
	}

	@Override
	public ListenableFuture<GetProductListResponse> getProductList(RequestHead head, GetProductListRequest request) {
		final ByteString data = request.hasOffsetIndex() ? request.getOffsetIndex() : null;
		Integer productId = null;
		Integer createTime = null;
		ToolsDAOProtos.ProductclockProductListIndex offsetIndex = null;
		if (data != null) {
			try {
				offsetIndex = ToolsDAOProtos.ProductclockProductListIndex.parseFrom(data);
				productId = offsetIndex.getProductId();
				createTime = offsetIndex.getCreateTime();
			} catch (InvalidProtocolBufferException e) {

			}
		}
		
		final long companyId = head.getSession().getCompanyId();
		final int size = request.getSize() < 0 ? 0 : request.getSize() > 50 ? 50 : request.getSize();
		if (size == 0) {
			return Futures.immediateFuture(GetProductListResponse.newBuilder()
					.setOffsetIndex(ByteString.EMPTY)
					.setHasMore(false)
					.build());
		}
		final String productName = request.hasProductName() ? request.getProductName() : null;
		
		List<Integer> productIdList = null;
		Connection conn = null;
		try {
			conn = this.hikariDataSource.getConnection();
			
			productIdList = ToolsProductclockDB.getProductIdList(conn, companyId, productId, createTime, size+1, productName);
		} catch (SQLException ex) {
			throw new RuntimeException("db fail", ex);
		} finally {
			DBUtil.closeQuietly(conn);
		}
		
		Map<Integer, ProductclockProtos.Product> productMap = ToolsProductclockUtil.getProduct(hikariDataSource, jedisPool, companyId, productIdList);
		
		List<ProductclockProtos.Product> productList = Lists.newArrayList();
		for (int pdtId : productIdList) {
			ProductclockProtos.Product product = productMap.get(pdtId);
			if (product != null) {
				productList.add(product);
			}
		}
		
		boolean hasMore = productList.size() > size;
		ProductclockProtos.Product lastProduct;
		if (hasMore) {
			lastProduct = productList.get(size-1);
		} else {
			if (productList.isEmpty()) {
				lastProduct = null;
			} else {
				lastProduct = productList.get(productList.size()-1);
			}
		}
		
		ToolsDAOProtos.ProductclockProductListIndex index = null;
		if (lastProduct != null) {
			index = ToolsDAOProtos.ProductclockProductListIndex.newBuilder()
					.setProductId(lastProduct.getProductId())
					.setCreateTime(lastProduct.getCreateTime())
					.build();
		} else {
			index = ToolsDAOProtos.ProductclockProductListIndex.getDefaultInstance();
		}
		
		return Futures.immediateFuture(GetProductListResponse.newBuilder()
				.addAllProduct(hasMore ? productList.subList(0, size) : productList)
				.setOffsetIndex(index.toByteString())
				.setHasMore(hasMore)
				.build());
	}

	@Override
	public ListenableFuture<GetCustomerProductResponse> getCustomerProduct(RequestHead head,
			GetCustomerProductRequest request) {
		final long companyId = head.getSession().getCompanyId();
		final List<Integer> customerIdList = request.getCustomerIdList();
		
		Map<Integer, List<ToolsDAOProtos.ProductclockCustomerProduct>> customerProductMap = null;
		Connection conn = null;
		try {
			conn = this.hikariDataSource.getConnection();
			
			customerProductMap = ToolsProductclockDB.getCustomerProduct(conn, companyId, customerIdList);
		} catch (SQLException ex) {
			throw new RuntimeException("db fail", ex);
		} finally {
			DBUtil.closeQuietly(conn);
		}
		
		Set<Integer> productIdSet = Sets.newTreeSet();
		for (Entry<Integer, List<ToolsDAOProtos.ProductclockCustomerProduct>> entry : customerProductMap.entrySet()) {
			for (ToolsDAOProtos.ProductclockCustomerProduct customerProduct : entry.getValue()) {
				productIdSet.add(customerProduct.getProductId());
			}
		}
		
		Map<Integer, ProductclockProtos.Product> productMap = ToolsProductclockUtil.getProduct(hikariDataSource, jedisPool, companyId, productIdSet);

		List<GetCustomerProductResponse.CustomerProduct> customerProductList = Lists.newArrayList();
		GetCustomerProductResponse.CustomerProduct.Builder customerProductBuilder = GetCustomerProductResponse.CustomerProduct.newBuilder();
		List<ProductclockProtos.Product> productList = Lists.newArrayList();
		for (Entry<Integer, List<ToolsDAOProtos.ProductclockCustomerProduct>> entry : customerProductMap.entrySet()) {
			int customerId = entry.getKey();
			customerProductBuilder.clear();
			productList.clear();
			
			for (ToolsDAOProtos.ProductclockCustomerProduct customerProduct : entry.getValue()) {
				int productId = customerProduct.getProductId();
				ProductclockProtos.Product product = productMap.get(productId);
				if (product == null) {
					continue;
				}
				
				ProductclockProtos.Product.Builder productBuilder = ProductclockProtos.Product.newBuilder()
						.mergeFrom(product)
						.setBuyTime(customerProduct.getBuyTime())
						.setDefaultRemindDay(customerProduct.getRemindPeriodDay());
				productList.add(productBuilder.build());
			}
			
			customerProductList.add(
					customerProductBuilder
						.setCustomerId(customerId)
						.addAllProduct(productList)
						.build()
					);
		}
		
		return Futures.immediateFuture(GetCustomerProductResponse.newBuilder()
				.addAllCustomerProduct(customerProductList)
				.build());
	}

	@Override
	public ListenableFuture<CreateCustomerProductResponse> createCustomerProduct(RequestHead head, CreateCustomerProductRequest request) {
		final long companyId = head.getSession().getCompanyId();
		
		final int customerId = request.getCustomerId();
		final int productId = request.getProductId();
		ProductclockProtos.Customer customer = ToolsProductclockUtil.getCustomer(hikariDataSource, jedisPool, companyId, Collections.singleton(customerId)).get(customerId);
		if (customer == null) {
			return Futures.immediateFuture(CreateCustomerProductResponse.newBuilder()
					.setResult(CreateCustomerProductResponse.Result.FAIL_CUSTOMER_ID_INVALID)
					.setFailText("客户不合法！")
					.build());
		}
		
		ProductclockProtos.Product product = ToolsProductclockUtil.getProduct(hikariDataSource, jedisPool, companyId, Collections.singleton(productId)).get(productId);
		if (product == null) {
			return Futures.immediateFuture(CreateCustomerProductResponse.newBuilder()
					.setResult(CreateCustomerProductResponse.Result.FAIL_CUSTOMER_ID_INVALID)
					.setFailText("不存在的产品！")
					.build());
		}
		
		final Integer buyTime = request.hasBuyTime() ? request.getBuyTime() : (int) (System.currentTimeMillis()/ 1000L);
		final int remindPeriodDay = request.hasRemindPeriodDay() ? request.getRemindPeriodDay() : product.getDefaultRemindDay();
		
		Connection conn = null;
		try {
			conn = this.hikariDataSource.getConnection();
			
			List<ToolsDAOProtos.ProductclockCustomerProduct> productList = ToolsProductclockDB.getCustomerProduct(conn, companyId, Collections.singleton(customerId)).get(customerId);
			if (productList != null) {
				for (ToolsDAOProtos.ProductclockCustomerProduct customerProduct : productList) {
					if (customerProduct.getProductId() == productId) {
						return Futures.immediateFuture(CreateCustomerProductResponse.newBuilder()
								.setResult(CreateCustomerProductResponse.Result.FAIL_PRODUCT_ID_INVALID)
								.setFailText("选择的产品已经在客户产品列表中！")
								.build());
					}
				}
				
			}
			
			ToolsProductclockDB.insertCustomerProduct(conn, companyId, customerId, productId, buyTime, remindPeriodDay);
		} catch (SQLException ex) {
			throw new RuntimeException("db fail", ex);
		} finally {
			DBUtil.closeQuietly(conn);
		}
		
		return Futures.immediateFuture(CreateCustomerProductResponse.newBuilder()
				.setResult(CreateCustomerProductResponse.Result.SUCC)
				.build());
	}
	

	@Override
	public ListenableFuture<UpdateCustomerProductResponse> updateCustomerProduct(RequestHead head,
			UpdateCustomerProductRequest request) {
		final long companyId = head.getSession().getCompanyId();
		
		final int customerId = request.getCustomerId();
		ProductclockProtos.Customer customer = ToolsProductclockUtil.getCustomer(hikariDataSource, jedisPool, companyId, Collections.singleton(customerId)).get(customerId);
		if (customer == null) {
			return Futures.immediateFuture(UpdateCustomerProductResponse.newBuilder()
					.setResult(UpdateCustomerProductResponse.Result.FAIL_CUSTOMER_ID_INVALID)
					.setFailText("客户不合法！")
					.build());
		}
		
		final int oldProductId = request.getOldProductId();
		final int newProductId = request.getNewProductId();
		ProductclockProtos.Product product = ToolsProductclockUtil.getProduct(hikariDataSource, jedisPool, companyId, Collections.singleton(newProductId)).get(newProductId);
		if (product == null) {
			return Futures.immediateFuture(UpdateCustomerProductResponse.newBuilder()
					.setResult(UpdateCustomerProductResponse.Result.FAIL_NEW_PRODUCT_ID_INVALID)
					.setFailText("选择的产品不存在！")
					.build());
		}
		
		final int buyTime = request.hasBuyTime() ? request.getBuyTime() : (int)(System.currentTimeMillis()/1000L);
		final int remindPeriodDay = request.hasRemindPeriodDay() ? request.getRemindPeriodDay() : product.getDefaultRemindDay();
		
		Connection conn = null;
		try {
			conn = this.hikariDataSource.getConnection();
			
			List<ToolsDAOProtos.ProductclockCustomerProduct> productList = ToolsProductclockDB.getCustomerProduct(conn, companyId, Collections.singleton(customerId)).get(customerId);
			if (productList != null) {
				for (ToolsDAOProtos.ProductclockCustomerProduct customerProduct : productList) {
					if (customerProduct.getProductId() == newProductId && newProductId != oldProductId) {
						return Futures.immediateFuture(UpdateCustomerProductResponse.newBuilder()
								.setResult(UpdateCustomerProductResponse.Result.FAIL_NEW_PRODUCT_ID_INVALID)
								.setFailText("选择的产品已经在客户产品列表中！")
								.build());
					}
				}
				
			}
			
			ToolsProductclockDB.updateCustomerProduct(conn, companyId, customerId, oldProductId, newProductId, buyTime, remindPeriodDay);
		} catch (SQLException ex) {
			throw new RuntimeException("db fail", ex);
		} finally {
			DBUtil.closeQuietly(conn);
		}
		
		return Futures.immediateFuture(UpdateCustomerProductResponse.newBuilder()
				.setResult(UpdateCustomerProductResponse.Result.SUCC)
				.build());
	}
	
	@Override
	public ListenableFuture<DeleteCustomerProductResponse> deleteCustomerProduct(RequestHead head, DeleteCustomerProductRequest request) {
		final long companyId = head.getSession().getCompanyId();
		
		final int customerId = request.getCustomerId();
		ProductclockProtos.Customer customer = ToolsProductclockUtil.getCustomer(hikariDataSource, jedisPool, companyId, Collections.singleton(customerId)).get(customerId);
		if (customer == null) {
			return Futures.immediateFuture(DeleteCustomerProductResponse.newBuilder()
					.setResult(DeleteCustomerProductResponse.Result.FAIL_CUSTOMER_ID_INVALID)
					.setFailText("客户不合法！")
					.build());
		}
		
		final List<Integer> productIdList = request.getProductIdList();
		Map<Integer, ProductclockProtos.Product> productMap = ToolsProductclockUtil.getProduct(hikariDataSource, jedisPool, companyId, productIdList);
		for (int productId : productIdList) {
			ProductclockProtos.Product product = productMap.get(productId);
			if (product == null) {
				return Futures.immediateFuture(DeleteCustomerProductResponse.newBuilder()
					.setResult(DeleteCustomerProductResponse.Result.FAIL_PRODUCT_ID_INVALID)
					.setFailText("不存在的产品！")
					.build());
			}
		}
		
		Connection conn = null;
		try {
			conn = this.hikariDataSource.getConnection();
			
			ToolsProductclockDB.deleteCustomerProduct(conn, companyId, customerId, productIdList);
		} catch (SQLException ex) {
			throw new RuntimeException("db fail", ex);
		} finally {
			DBUtil.closeQuietly(conn);
		}
		
		return Futures.immediateFuture(DeleteCustomerProductResponse.newBuilder()
				.setResult(DeleteCustomerProductResponse.Result.SUCC)
				.build());
	}
	
	@Override
	public ListenableFuture<CreateCustomerProductResponse> createCustomerProduct(AdminHead head, CreateCustomerProductRequest request) {
		final long companyId = head.getCompanyId();
		
		final int customerId = request.getCustomerId();
		final int productId = request.getProductId();
		ProductclockProtos.Customer customer = ToolsProductclockUtil.getCustomer(hikariDataSource, jedisPool, companyId, Collections.singleton(customerId)).get(customerId);
		if (customer == null) {
			return Futures.immediateFuture(CreateCustomerProductResponse.newBuilder()
					.setResult(CreateCustomerProductResponse.Result.FAIL_CUSTOMER_ID_INVALID)
					.setFailText("客户不合法！")
					.build());
		}
		
		ProductclockProtos.Product product = ToolsProductclockUtil.getProduct(hikariDataSource, jedisPool, companyId, Collections.singleton(productId)).get(productId);
		if (product == null) {
			return Futures.immediateFuture(CreateCustomerProductResponse.newBuilder()
					.setResult(CreateCustomerProductResponse.Result.FAIL_CUSTOMER_ID_INVALID)
					.setFailText("不存在的产品！")
					.build());
		}
		
		final Integer buyTime = request.hasBuyTime() ? request.getBuyTime() < 0 ? 0 : request.getBuyTime() : null;
		final int remindPeriodDay = request.hasRemindPeriodDay() ? request.getRemindPeriodDay() : product.getDefaultRemindDay();
		
		Connection conn = null;
		try {
			conn = this.hikariDataSource.getConnection();
			
			List<ToolsDAOProtos.ProductclockCustomerProduct> productList = ToolsProductclockDB.getCustomerProduct(conn, companyId, Collections.singleton(customerId)).get(customerId);
			if (productList != null) {
				for (ToolsDAOProtos.ProductclockCustomerProduct customerProduct : productList) {
					if (customerProduct.getProductId() == productId) {
						return Futures.immediateFuture(CreateCustomerProductResponse.newBuilder()
								.setResult(CreateCustomerProductResponse.Result.FAIL_PRODUCT_ID_INVALID)
								.setFailText("选择的产品已经在客户产品列表中！")
								.build());
					}
				}
			}
			
			ToolsProductclockDB.insertCustomerProduct(conn, companyId, customerId, productId, buyTime, remindPeriodDay);
		} catch (SQLException ex) {
			throw new RuntimeException("db fail", ex);
		} finally {
			DBUtil.closeQuietly(conn);
		}
		
		return Futures.immediateFuture(CreateCustomerProductResponse.newBuilder()
				.setResult(CreateCustomerProductResponse.Result.SUCC)
				.build());
	}
	
	@Override
	public ListenableFuture<UpdateCustomerProductResponse> updateCustomerProduct(AdminHead head,
			UpdateCustomerProductRequest request) {
		final long companyId = head.getCompanyId();
		
		final int customerId = request.getCustomerId();
		ProductclockProtos.Customer customer = ToolsProductclockUtil.getCustomer(hikariDataSource, jedisPool, companyId, Collections.singleton(customerId)).get(customerId);
		if (customer == null) {
			return Futures.immediateFuture(UpdateCustomerProductResponse.newBuilder()
					.setResult(UpdateCustomerProductResponse.Result.FAIL_CUSTOMER_ID_INVALID)
					.setFailText("客户不合法！")
					.build());
		}
		
		final int oldProductId = request.getOldProductId();
		final int newProductId = request.getNewProductId();
		ProductclockProtos.Product product = ToolsProductclockUtil.getProduct(hikariDataSource, jedisPool, companyId, Collections.singleton(newProductId)).get(newProductId);
		if (product == null) {
			return Futures.immediateFuture(UpdateCustomerProductResponse.newBuilder()
					.setResult(UpdateCustomerProductResponse.Result.FAIL_NEW_PRODUCT_ID_INVALID)
					.setFailText("选择的产品不存在！")
					.build());
		}
		
		final int buyTime = request.hasBuyTime() ? request.getBuyTime() : (int)(System.currentTimeMillis()/1000L);
		final int remindPeriodDay = request.hasRemindPeriodDay() ? request.getRemindPeriodDay() : product.getDefaultRemindDay();
		
		Connection conn = null;
		try {
			conn = this.hikariDataSource.getConnection();
			
			List<ToolsDAOProtos.ProductclockCustomerProduct> productList = ToolsProductclockDB.getCustomerProduct(conn, companyId, Collections.singleton(customerId)).get(customerId);
			if (productList != null) {
				for (ToolsDAOProtos.ProductclockCustomerProduct customerProduct : productList) {
					if (customerProduct.getProductId() == newProductId && newProductId != oldProductId) {
						return Futures.immediateFuture(UpdateCustomerProductResponse.newBuilder()
								.setResult(UpdateCustomerProductResponse.Result.FAIL_NEW_PRODUCT_ID_INVALID)
								.setFailText("选择的产品已经在客户产品列表中！")
								.build());
					}
				}
				
			}
			
			ToolsProductclockDB.updateCustomerProduct(conn, companyId, customerId, oldProductId, newProductId, buyTime, remindPeriodDay);
		} catch (SQLException ex) {
			throw new RuntimeException("db fail", ex);
		} finally {
			DBUtil.closeQuietly(conn);
		}
		
		return Futures.immediateFuture(UpdateCustomerProductResponse.newBuilder()
				.setResult(UpdateCustomerProductResponse.Result.SUCC)
				.build());
	}
	
	@Override
	public ListenableFuture<DeleteCustomerProductResponse> deleteCustomerProduct(AdminHead head, DeleteCustomerProductRequest request) {
		final long companyId = head.getCompanyId();
		
		final int customerId = request.getCustomerId();
		ProductclockProtos.Customer customer = ToolsProductclockUtil.getCustomer(hikariDataSource, jedisPool, companyId, Collections.singleton(customerId)).get(customerId);
		if (customer == null) {
			return Futures.immediateFuture(DeleteCustomerProductResponse.newBuilder()
					.setResult(DeleteCustomerProductResponse.Result.FAIL_CUSTOMER_ID_INVALID)
					.setFailText("客户不合法！")
					.build());
		}
		
		final List<Integer> productIdList = request.getProductIdList();
		Map<Integer, ProductclockProtos.Product> productMap = ToolsProductclockUtil.getProduct(hikariDataSource, jedisPool, companyId, productIdList);
		for (int productId : productIdList) {
			ProductclockProtos.Product product = productMap.get(productId);
			if (product == null) {
				return Futures.immediateFuture(DeleteCustomerProductResponse.newBuilder()
					.setResult(DeleteCustomerProductResponse.Result.FAIL_PRODUCT_ID_INVALID)
					.setFailText("不存在的产品！")
					.build());
			}
		}
		
		Connection conn = null;
		try {
			conn = this.hikariDataSource.getConnection();
			
			ToolsProductclockDB.deleteCustomerProduct(conn, companyId, customerId, productIdList);
		} catch (SQLException ex) {
			throw new RuntimeException("db fail", ex);
		} finally {
			DBUtil.closeQuietly(conn);
		}
		
		return Futures.immediateFuture(DeleteCustomerProductResponse.newBuilder()
				.setResult(DeleteCustomerProductResponse.Result.SUCC)
				.build());
	}

	@Override
	public ListenableFuture<GetCommunicateRecordResponse> getCommunicateRecord(RequestHead head,
			GetCommunicateRecordRequest request) {
		final ByteString data = request.hasOffsetIndex() ? request.getOffsetIndex() : null;
		Integer recordId = null;
		Integer createTime = null;
		ToolsDAOProtos.ProductclockCommunicateRecordIndex offsetIndex = null;
		if (data != null) {
			try {
				offsetIndex = ToolsDAOProtos.ProductclockCommunicateRecordIndex.parseFrom(data);
				recordId = offsetIndex.getRecordId();
				createTime = offsetIndex.getCreateTime();
			} catch (InvalidProtocolBufferException e) {

			}
		}
		
		final long companyId = head.getSession().getCompanyId();
		final long userId = head.getSession().getUserId();
		final int customerId = request.getCustomerId();
		final int size = request.getSize();
		if (size == 0) {
			return Futures.immediateFuture(GetCommunicateRecordResponse.newBuilder()
					.setOffsetIndex(ByteString.EMPTY)
					.setHasMore(false)
					.build());
		}
		
		List<ProductclockProtos.CommunicateRecord> communicateRecordList = Lists.newArrayList();
		Connection conn = null;
		try {
			conn = this.hikariDataSource.getConnection();
			
			communicateRecordList = ToolsProductclockDB.getCommunicateRecord(conn, companyId, userId, customerId, recordId, createTime, size+1);
		} catch (SQLException ex) {
			throw new RuntimeException("db fail", ex);
		} finally {
			DBUtil.closeQuietly(conn);
		}
		
		boolean hasMore = communicateRecordList.size() > size;
		
		ProductclockProtos.CommunicateRecord lastCommunicateRecord;
		if (hasMore) {
			lastCommunicateRecord = communicateRecordList.get(size-1);
		} else {
			if (communicateRecordList.isEmpty()) {
				lastCommunicateRecord = null;
			} else {
				lastCommunicateRecord = communicateRecordList.get(communicateRecordList.size()-1);
			}
		}
		
		ToolsDAOProtos.ProductclockCommunicateRecordIndex index;
		if (lastCommunicateRecord != null) {
			index = ToolsDAOProtos.ProductclockCommunicateRecordIndex.newBuilder()
					.setRecordId(lastCommunicateRecord.getRecordId())
					.setCreateTime(lastCommunicateRecord.getCreateTime())
					.build();
		} else {
			index = ToolsDAOProtos.ProductclockCommunicateRecordIndex.getDefaultInstance();
		}
		
		return Futures.immediateFuture(GetCommunicateRecordResponse.newBuilder()
				.addAllCommunicateRecord(hasMore ? communicateRecordList.subList(0, size) : communicateRecordList)
				.setOffsetIndex(index.toByteString())
				.setHasMore(hasMore)
				.build());
	}

	@Override
	public ListenableFuture<CreateCommunicateRecordResponse> createCommunicateRecord(RequestHead head,
			CreateCommunicateRecordRequest request) {
		final long companyId = head.getSession().getCompanyId();
		final int customerId = request.getCustomerId();
		
		ProductclockProtos.Customer customer = ToolsProductclockUtil.getCustomer(hikariDataSource, jedisPool, companyId, Collections.singleton(customerId)).get(customerId);
		if (customer == null) {
			return Futures.immediateFuture(CreateCommunicateRecordResponse.newBuilder()
					.setResult(CreateCommunicateRecordResponse.Result.FAIL_CUSTOMER_ID_INVALID)
					.setFailText("不存在的用户，CUSTOMER_ID:" + customerId)
					.build());
		}
		
		final String contentText = request.getContentText();
		if (contentText.length() > 191) {
			return Futures.immediateFuture(CreateCommunicateRecordResponse.newBuilder()
					.setResult(CreateCommunicateRecordResponse.Result.FAIL_CONTENT_TEXT_INVALID)
					.setFailText("输入的内容过长")
					.build());
		}
		
		final int now = (int) (System.currentTimeMillis() / 1000L);
		final long createUserId = head.getSession().getUserId(); 
		
		int recordId = 0;
		Connection conn = null;
		try {
			conn = this.hikariDataSource.getConnection();
			
			recordId = ToolsProductclockDB.insertCommunicateRecord(conn, companyId, customerId, contentText, now, createUserId);
		} catch (SQLException ex) {
			throw new RuntimeException("db fail", ex);
		} finally {
			DBUtil.closeQuietly(conn);
		}
		
		return Futures.immediateFuture(CreateCommunicateRecordResponse.newBuilder()
				.setResult(CreateCommunicateRecordResponse.Result.SUCC)
				.setRecordId(recordId)
				.build());
	}

	@Override
	public ListenableFuture<UpdateCommunicateRecordResponse> updateCommunicateRecord(RequestHead head,
			UpdateCommunicateRecordRequest request) {
		final long companyId = head.getSession().getCompanyId();
		
		final String contextText = request.getContentText();
		if (contextText.length() > 191) {
			return Futures.immediateFuture(UpdateCommunicateRecordResponse.newBuilder()
					.setResult(UpdateCommunicateRecordResponse.Result.FAIL_CONTENT_TEXT_INVALID)
					.setFailText("输入的内容过长")
					.build());
		}
		
		final int recordId = request.getRecordId();
		
		Connection conn = null;
		try {
			conn = this.hikariDataSource.getConnection();
			
			ToolsProductclockDB.updateCommunicateRecord(conn, companyId, contextText, recordId);
			
			return Futures.immediateFuture(UpdateCommunicateRecordResponse.newBuilder()
					.setResult(UpdateCommunicateRecordResponse.Result.SUCC)
					.build());
		} catch (SQLException ex) {
			throw new RuntimeException("db fail", ex);
		} finally {
			DBUtil.closeQuietly(conn);
		}
	}

	@Override
	public ListenableFuture<DeleteCommunicateRecordResponse> deleteCommunicateRecord(RequestHead head,
			DeleteCommunicateRecordRequest request) {
		final long companyId = head.getSession().getCompanyId();
		final List<Integer> recordIdList = request.getRecordIdList();
		
		Connection conn = null;
		try {
			conn = this.hikariDataSource.getConnection();
			
			ToolsProductclockDB.deleteCommunicateRecord(conn, companyId, recordIdList);
		} catch (SQLException ex) {
			throw new RuntimeException("db fail", ex);
		} finally {
			DBUtil.closeQuietly(conn);
		}
		
		return Futures.immediateFuture(DeleteCommunicateRecordResponse.newBuilder()
				.setResult(DeleteCommunicateRecordResponse.Result.SUCC)
				.build());
	}

	@Override
	public ListenableFuture<CreateCustomerResponse> createCustomer(RequestHead head, CreateCustomerRequest request) {
		final long companyId = head.getSession().getCompanyId();
		
		final String customerName = request.getCustomerName();
		if (customerName.isEmpty() || customerName.length() > 10) {
			return Futures.immediateFuture(CreateCustomerResponse.newBuilder()
					.setResult(CreateCustomerResponse.Result.FAIL_CUSTOMER_NAME_INVALID)
					.setFailText("请输入合法的客户名称！")
					.build());
		}
		
		final String mobileNo = request.hasMobileNo() ? request.getMobileNo() : null;
		if (mobileNo != null && !MobileNoUtil.isValid(mobileNo)) {
			return Futures.immediateFuture(CreateCustomerResponse.newBuilder()
					.setResult(CreateCustomerResponse.Result.FAIL_MOBILE_NO_INVALID)
					.setFailText("请输入合法的手机号码！")
					.build());
		}
		
		final Gender gender = request.getGender();
		final Integer birthdaySolar = request.hasBirthdaySolar() ? request.getBirthdaySolar() : null;
		if (birthdaySolar != null && checkTime(birthdaySolar) == 0L) {
			return Futures.immediateFuture(CreateCustomerResponse.newBuilder()
					.setResult(CreateCustomerResponse.Result.FAIL_BIRTHDAY_SOLAR_INVALID)
					.setFailText("请输入合法的生日(1936-2025)！")
					.build());
		}
		
		final Integer birthdayLunar = request.hasBirthdayLunar() ? request.getBirthdayLunar() : null;
		if (birthdayLunar != null && checkTime(birthdayLunar) == 0L) {
			return Futures.immediateFuture(CreateCustomerResponse.newBuilder()
					.setResult(CreateCustomerResponse.Result.FAIL_BIRTHDAY_LUNAR_INVALID)
					.setFailText("请输入合法的生日(1936-2025)！")
					.build());
		}
		
		final Integer weddingSolar = request.hasWeddingSolar() ? request.getWeddingSolar() : null;
		if (weddingSolar != null && checkTime(weddingSolar) == 0L) {
			return Futures.immediateFuture(CreateCustomerResponse.newBuilder()
					.setResult(CreateCustomerResponse.Result.FAIL_WEDDING_SOLAR_INVALID)
					.setFailText("请输入合法的结婚纪念日(1936-2025)！")
					.build());
		}
		
		final Integer weddingLunar = request.hasWeddingLunar() ? request.getWeddingLunar() : null;
		if (weddingLunar != null && checkTime(weddingLunar) == 0L) {
			return Futures.immediateFuture(CreateCustomerResponse.newBuilder()
					.setResult(CreateCustomerResponse.Result.FAIL_WEDDING_LUNAR_INVALID)
					.setFailText("请输入合法的结婚纪念日(1936-2025)！")
					.build());
		}
		
		final String address = request.hasAddress() ? request.getAddress() : null;
		if (address != null && address.length() > 191) {
			return Futures.immediateFuture(CreateCustomerResponse.newBuilder()
					.setResult(CreateCustomerResponse.Result.FAIL_ADDRESS_INVALID)
					.setFailText("请输入合法的地址！")
					.build());
		}
		
		final String remark = request.hasRemark() ? request.getRemark() : null;
		if (remark != null && remark.length() > 191) {
			return Futures.immediateFuture(CreateCustomerResponse.newBuilder()
					.setResult(CreateCustomerResponse.Result.FAIL_ADDRESS_INVALID)
					.setFailText("请输入合法的备注信息！")
					.build());
		}
		
		final boolean isRemind = request.getIsRemind();
		final int daysAgoRemind = request.hasDaysAgoRemind() ? (request.getDaysAgoRemind() < 0 ? 0 : request.getDaysAgoRemind()) : 0;
		
		final long createUserId = head.getSession().getUserId();
		final int now = (int) (System.currentTimeMillis() / 1000L);
		
		ProductclockProtos.Customer.Builder customerBuilder = ProductclockProtos.Customer.newBuilder()
				.setCustomerId(0)
				.setCustomerName(customerName)
				.setGender(gender)
				.setIsRemind(isRemind)
				.setDaysAgoRemind(daysAgoRemind)
				.setCreateUser(createUserId)
				.setCreateTime(now);
		if (mobileNo != null) {
			customerBuilder.setMobileNo(mobileNo);
		}
		if (birthdaySolar != null) {
			customerBuilder.setBirthdaySolar(birthdaySolar);
		}
		if (birthdayLunar != null) {
			customerBuilder.setBirthdayLunar(birthdayLunar);
		}
		if (weddingSolar != null) {
			customerBuilder.setWeddingSolar(weddingSolar);
		}
		if (weddingLunar != null) {
			customerBuilder.setWeddingLunar(weddingLunar);
		}
		if (address != null) {
			customerBuilder.setAddress(address);
		}
		if (remark != null) {
			customerBuilder.setRemark(remark);
		}
		
		List<Integer> customerIdList = null;
		Connection conn = null;
		try {
			conn = this.hikariDataSource.getConnection();
			
			customerIdList = ToolsProductclockDB.insertCustomer(conn, companyId, 
					Collections.singleton(customerBuilder.build()), createUserId, now);
		} catch (SQLException ex) {
			throw new RuntimeException("db fail", ex);
		} finally {
			DBUtil.closeQuietly(conn);
		}
		
		if (customerIdList == null) {
			customerIdList = Collections.emptyList();
		}
		
		Jedis jedis = this.jedisPool.getResource();
		try {
			ToolsProductclockCache.delCustomer(jedis, companyId, customerIdList);
		} finally {
			jedis.close();
		}
		
		return Futures.immediateFuture(CreateCustomerResponse.newBuilder()
				.setResult(CreateCustomerResponse.Result.SUCC)
				.setCustomerId(customerIdList.isEmpty() ? 0 : customerIdList.get(0))
				.build());
	}

	@Override
	public ListenableFuture<UpdateCustomerResponse> updateCustomer(RequestHead head, UpdateCustomerRequest request) {
		final long companyId = head.getSession().getCompanyId();
		
		final int customerId = request.getCustomerId();
		
		final String customerName = request.getCustomerName();
		if (customerName.isEmpty() || customerName.length() > 10) {
			return Futures.immediateFuture(UpdateCustomerResponse.newBuilder()
					.setResult(UpdateCustomerResponse.Result.FAIL_CUSTOMER_NAME_INVALID)
					.setFailText("请输入合法的客户名称！")
					.build());
		}
		
		final String mobileNo = request.hasMobileNo() ? request.getMobileNo() : null;
		if (mobileNo != null && !MobileNoUtil.isValid(mobileNo)) {
			return Futures.immediateFuture(UpdateCustomerResponse.newBuilder()
					.setResult(UpdateCustomerResponse.Result.FAIL_MOBILE_NO_INVALID)
					.setFailText("请输入合法的手机号码！")
					.build());
		}
		
		final Gender gender = request.getGender();
		final Integer birthdaySolar = request.hasBirthdaySolar() ? request.getBirthdaySolar() : null;
		if (birthdaySolar != null && checkTime(birthdaySolar) == 0L) {
			return Futures.immediateFuture(UpdateCustomerResponse.newBuilder()
					.setResult(UpdateCustomerResponse.Result.FAIL_BIRTHDAY_SOLAR_INVALID)
					.setFailText("请输入合法的生日(1936-2025)！")
					.build());
		}
		
		final Integer birthdayLunar = request.hasBirthdayLunar() ? request.getBirthdayLunar() : null;
		if (birthdayLunar != null && checkTime(birthdayLunar) == 0L) {
			return Futures.immediateFuture(UpdateCustomerResponse.newBuilder()
					.setResult(UpdateCustomerResponse.Result.FAIL_BIRTHDAY_LUNAR_INVALID)
					.setFailText("请输入合法的生日(1936-2025)！")
					.build());
		}
		
		final Integer weddingSolar = request.hasWeddingSolar() ? request.getWeddingSolar() : null;
		if (weddingSolar != null && checkTime(weddingSolar) == 0L) {
			return Futures.immediateFuture(UpdateCustomerResponse.newBuilder()
					.setResult(UpdateCustomerResponse.Result.FAIL_WEDDING_SOLAR_INVALID)
					.setFailText("请输入合法的结婚纪念日(1936-2025)！")
					.build());
		}
		
		final Integer weddingLunar = request.hasWeddingLunar() ? request.getWeddingLunar() : null;
		if (weddingLunar != null && checkTime(weddingLunar) == 0L) {
			return Futures.immediateFuture(UpdateCustomerResponse.newBuilder()
					.setResult(UpdateCustomerResponse.Result.FAIL_WEDDING_LUNAR_INVALID)
					.setFailText("请输入合法的结婚纪念日(1936-2025)！")
					.build());
		}
		final String address = request.hasAddress() ? request.getAddress() : null;
		if (address != null && address.length() > 191) {
			return Futures.immediateFuture(UpdateCustomerResponse.newBuilder()
					.setResult(UpdateCustomerResponse.Result.FAIL_ADDRESS_INVALID)
					.setFailText("请输入合法的地址！")
					.build());
		}
		
		final String remark = request.hasRemark() ? request.getRemark() : null;
		if (remark != null && remark.length() > 191) {
			return Futures.immediateFuture(UpdateCustomerResponse.newBuilder()
					.setResult(UpdateCustomerResponse.Result.FAIL_ADDRESS_INVALID)
					.setFailText("请输入合法的备注信息！")
					.build());
		}
		
		final boolean isRemind = request.getIsRemind();
		final int daysAgoRemind = request.hasDaysAgoRemind() ? (request.getDaysAgoRemind() < 0 ? 0 : request.getDaysAgoRemind()) : 0;
		
		final long updateUserId = head.getSession().getUserId();
		final int now = (int) (System.currentTimeMillis() / 1000L);
		
		Connection conn = null;
		try {
			conn = this.hikariDataSource.getConnection();
			
			ToolsProductclockDB.updateCustomer(conn, companyId, customerId,
					customerName, mobileNo, gender,
					birthdaySolar, birthdayLunar, weddingSolar, weddingLunar,
					address, remark, isRemind, daysAgoRemind, updateUserId, now);
		} catch (SQLException ex) {
			throw new RuntimeException("db fail", ex);
		} finally {
			DBUtil.closeQuietly(conn);
		}
		
		Jedis jedis = this.jedisPool.getResource();
		try {
			ToolsProductclockCache.delCustomer(jedis, companyId, Collections.singleton(customerId));
		} finally {
			jedis.close();
		}
		
		return Futures.immediateFuture(UpdateCustomerResponse.newBuilder()
				.setResult(UpdateCustomerResponse.Result.SUCC)
				.build());
	}

	@Override
	public ListenableFuture<DeleteCustomerResponse> deleteCustomer(RequestHead head, DeleteCustomerRequest request) {
		final long companyId = head.getSession().getCompanyId();
		final List<Integer> customerIdList = request.getCustomerIdList();
		
		Map<Integer, ProductclockProtos.Customer> customerMap = ToolsProductclockUtil.getCustomer(hikariDataSource, jedisPool, companyId, customerIdList);
		List<Integer> validCustomerIdList = Lists.newArrayList();
		for (int customerId : customerIdList) {
			ProductclockProtos.Customer customer = customerMap.get(customerId);
			if (customer != null) {
				validCustomerIdList.add(customerId);
			}
		}
		
		Connection conn = null;
		try {
			conn = this.hikariDataSource.getConnection();
			
			ToolsProductclockDB.deleteCustomer(conn, companyId, validCustomerIdList);
		} catch (SQLException ex) {
			throw new RuntimeException("db fail", ex);
		} finally {
			DBUtil.closeQuietly(conn);
		}

		Jedis jedis = this.jedisPool.getResource();
		try {
			ToolsProductclockCache.delCustomer(jedis, companyId, validCustomerIdList);
		} finally {
			jedis.close();
		}
		
		return Futures.immediateFuture(DeleteCustomerResponse.newBuilder()
				.setResult(DeleteCustomerResponse.Result.SUCC)
				.build());
	}

	@Override
	public ListenableFuture<CreateCustomerResponse> createCustomer(AdminHead head, CreateCustomerRequest request) {
		final long companyId = head.getCompanyId();
		final String customerName = request.getCustomerName();
		if (customerName.isEmpty() || customerName.length() > 10) {
			return Futures.immediateFuture(CreateCustomerResponse.newBuilder()
					.setResult(CreateCustomerResponse.Result.FAIL_CUSTOMER_NAME_INVALID)
					.setFailText("请输入合法的客户名称！")
					.build());
		}
		
		final String mobileNo = request.hasMobileNo() ? request.getMobileNo() : null;
		if (mobileNo != null && !MobileNoUtil.isValid(mobileNo)) {
			return Futures.immediateFuture(CreateCustomerResponse.newBuilder()
					.setResult(CreateCustomerResponse.Result.FAIL_MOBILE_NO_INVALID)
					.setFailText("请输入合法的手机号码！")
					.build());
		}
		
		final Gender gender = request.getGender();
		final Integer birthdaySolar = request.hasBirthdaySolar() ? request.getBirthdaySolar() : null;
		if (birthdaySolar != null && checkTime(birthdaySolar) == 0L) {
			return Futures.immediateFuture(CreateCustomerResponse.newBuilder()
					.setResult(CreateCustomerResponse.Result.FAIL_BIRTHDAY_SOLAR_INVALID)
					.setFailText("请输入合法的生日(1936-2025)！")
					.build());
		}
		
		final Integer birthdayLunar = request.hasBirthdayLunar() ? request.getBirthdayLunar() : null;
		if (birthdayLunar != null && checkTime(birthdayLunar) == 0L) {
			return Futures.immediateFuture(CreateCustomerResponse.newBuilder()
					.setResult(CreateCustomerResponse.Result.FAIL_BIRTHDAY_LUNAR_INVALID)
					.setFailText("请输入合法的生日(1936-2025)！")
					.build());
		}
		
		final Integer weddingSolar = request.hasWeddingSolar() ? request.getWeddingSolar() : null;
		if (weddingSolar != null && checkTime(weddingSolar) == 0L) {
			return Futures.immediateFuture(CreateCustomerResponse.newBuilder()
					.setResult(CreateCustomerResponse.Result.FAIL_WEDDING_SOLAR_INVALID)
					.setFailText("请输入合法的结婚纪念日(1936-2025)！")
					.build());
		}
		
		final Integer weddingLunar = request.hasWeddingLunar() ? request.getWeddingLunar() : null;
		if (weddingLunar != null && checkTime(weddingLunar) == 0L) {
			return Futures.immediateFuture(CreateCustomerResponse.newBuilder()
					.setResult(CreateCustomerResponse.Result.FAIL_WEDDING_LUNAR_INVALID)
					.setFailText("请输入合法的结婚纪念日(1936-2025)！")
					.build());
		}
		
		final String address = request.hasAddress() ? request.getAddress() : null;
		if (address != null && address.length() > 191) {
			return Futures.immediateFuture(CreateCustomerResponse.newBuilder()
					.setResult(CreateCustomerResponse.Result.FAIL_ADDRESS_INVALID)
					.setFailText("请输入合法的地址！")
					.build());
		}
		
		final String remark = request.hasRemark() ? request.getRemark() : null;
		if (remark != null && remark.length() > 191) {
			return Futures.immediateFuture(CreateCustomerResponse.newBuilder()
					.setResult(CreateCustomerResponse.Result.FAIL_ADDRESS_INVALID)
					.setFailText("请输入合法的备注信息！")
					.build());
		}
		
		final boolean isRemind = request.getIsRemind();
		final int daysAgoRemind = request.hasDaysAgoRemind() ? (request.getDaysAgoRemind() < 0 ? 0 : request.getDaysAgoRemind()) : 0;
		
		final long createAdminId = head.getSession().getAdminId();
		final int now = (int) (System.currentTimeMillis() / 1000L);
		
		ProductclockProtos.Customer.Builder customerBuilder = ProductclockProtos.Customer.newBuilder()
				.setCustomerId(0)
				.setCustomerName(customerName)
				.setGender(gender)
				.setIsRemind(isRemind)
				.setDaysAgoRemind(daysAgoRemind)
				.setCreateTime(now);
		if (mobileNo != null) {
			customerBuilder.setMobileNo(mobileNo);
		}
		if (birthdaySolar != null) {
			customerBuilder.setBirthdaySolar(birthdaySolar);
		}
		if (birthdayLunar != null) {
			customerBuilder.setBirthdayLunar(birthdayLunar);
		}
		if (weddingSolar != null) {
			customerBuilder.setWeddingSolar(weddingSolar);
		}
		if (weddingLunar != null) {
			customerBuilder.setWeddingLunar(weddingLunar);
		}
		if (address != null) {
			customerBuilder.setAddress(address);
		}
		if (remark != null) {
			customerBuilder.setRemark(remark);
		}
		
		List<Integer> customerIdList = null;
		Connection conn = null;
		try {
			conn = this.hikariDataSource.getConnection();
			
			customerIdList = ToolsProductclockDB.insertCustomer(conn, companyId, 
					Collections.singleton(customerBuilder.build()),
					createAdminId, now);
		} catch (SQLException ex) {
			throw new RuntimeException("db fail", ex);
		} finally {
			DBUtil.closeQuietly(conn);
		}
		
		if (customerIdList == null) {
			customerIdList = Collections.emptyList();
		}
		
		Jedis jedis = this.jedisPool.getResource();
		try {
			ToolsProductclockCache.delCustomer(jedis, companyId, customerIdList);
		} finally {
			jedis.close();
		}
		
		return Futures.immediateFuture(CreateCustomerResponse.newBuilder()
					.setResult(CreateCustomerResponse.Result.SUCC)
					.build());
	}

	@Override
	public ListenableFuture<UpdateCustomerResponse> updateCustomer(AdminHead head, UpdateCustomerRequest request) {
		final long companyId = head.getCompanyId();
		
		final int customerId = request.getCustomerId();
		
		final String customerName = request.getCustomerName();
		if (customerName.isEmpty() || customerName.length() > 10) {
			return Futures.immediateFuture(UpdateCustomerResponse.newBuilder()
					.setResult(UpdateCustomerResponse.Result.FAIL_CUSTOMER_NAME_INVALID)
					.setFailText("请输入合法的客户名称！")
					.build());
		}
		
		final String mobileNo = request.hasMobileNo() ? request.getMobileNo() : null;
		if (mobileNo != null && !MobileNoUtil.isValid(mobileNo)) {
			return Futures.immediateFuture(UpdateCustomerResponse.newBuilder()
					.setResult(UpdateCustomerResponse.Result.FAIL_MOBILE_NO_INVALID)
					.setFailText("请输入合法的手机号码！")
					.build());
		}
		
		final Gender gender = request.getGender();
		final Integer birthdaySolar = request.hasBirthdaySolar() ? request.getBirthdaySolar() : null;
		if (birthdaySolar != null && checkTime(birthdaySolar) == 0L) {
			return Futures.immediateFuture(UpdateCustomerResponse.newBuilder()
					.setResult(UpdateCustomerResponse.Result.FAIL_BIRTHDAY_SOLAR_INVALID)
					.setFailText("请输入合法的生日(1936-2025)！")
					.build());
		}
		
		final Integer birthdayLunar = request.hasBirthdayLunar() ? request.getBirthdayLunar() : null;
		if (birthdayLunar != null && checkTime(birthdayLunar) == 0L) {
			return Futures.immediateFuture(UpdateCustomerResponse.newBuilder()
					.setResult(UpdateCustomerResponse.Result.FAIL_BIRTHDAY_LUNAR_INVALID)
					.setFailText("请输入合法的生日(1936-2025)！")
					.build());
		}
		
		final Integer weddingSolar = request.hasWeddingSolar() ? request.getWeddingSolar() : null;
		if (weddingSolar != null && checkTime(weddingSolar) == 0L) {
			return Futures.immediateFuture(UpdateCustomerResponse.newBuilder()
					.setResult(UpdateCustomerResponse.Result.FAIL_WEDDING_SOLAR_INVALID)
					.setFailText("请输入合法的结婚纪念日(1936-2025)！")
					.build());
		}
		
		final Integer weddingLunar = request.hasWeddingLunar() ? request.getWeddingLunar() : null;
		if (weddingLunar != null && checkTime(weddingLunar) == 0L) {
			return Futures.immediateFuture(UpdateCustomerResponse.newBuilder()
					.setResult(UpdateCustomerResponse.Result.FAIL_WEDDING_LUNAR_INVALID)
					.setFailText("请输入合法的结婚纪念日(1936-2025)！")
					.build());
		}
		
		final String address = request.hasAddress() ? request.getAddress() : null;
		if (address != null && address.length() > 191) {
			return Futures.immediateFuture(UpdateCustomerResponse.newBuilder()
					.setResult(UpdateCustomerResponse.Result.FAIL_ADDRESS_INVALID)
					.setFailText("请输入合法的地址！")
					.build());
		}
		
		final String remark = request.hasRemark() ? request.getRemark() : null;
		if (remark != null && remark.length() > 191) {
			return Futures.immediateFuture(UpdateCustomerResponse.newBuilder()
					.setResult(UpdateCustomerResponse.Result.FAIL_ADDRESS_INVALID)
					.setFailText("请输入合法的备注信息！")
					.build());
		}
		
		final boolean isRemind = request.getIsRemind();
		final int daysAgoRemind = request.hasDaysAgoRemind() ? (request.getDaysAgoRemind() < 0 ? 0 : request.getDaysAgoRemind()) : 0;
		
		final long updateUserId = head.getSession().getAdminId();
		final int now = (int) (System.currentTimeMillis() / 1000L);
		
		Connection conn = null;
		try {
			conn = this.hikariDataSource.getConnection();
			
			ToolsProductclockDB.updateCustomer(conn, companyId, customerId,
					customerName, mobileNo, gender,
					birthdaySolar, birthdayLunar, weddingSolar, weddingLunar,
					address, remark, isRemind, daysAgoRemind, updateUserId, now);
		} catch (SQLException ex) {
			throw new RuntimeException("db fail", ex);
		} finally {
			DBUtil.closeQuietly(conn);
		}
		
		Jedis jedis = this.jedisPool.getResource();
		try {
			ToolsProductclockCache.delCustomer(jedis, companyId, Collections.singleton(customerId));
		} finally {
			jedis.close();
		}
		
		return Futures.immediateFuture(UpdateCustomerResponse.newBuilder()
				.setResult(UpdateCustomerResponse.Result.SUCC)
				.build());
	}

	@Override
	public ListenableFuture<DeleteCustomerResponse> deleteCustomer(AdminHead head, DeleteCustomerRequest request) {
		final long companyId = head.getCompanyId();
		final List<Integer> customerIdList = request.getCustomerIdList();
		
		Connection conn = null;
		try {
			conn = this.hikariDataSource.getConnection();
			
			ToolsProductclockDB.deleteCustomer(conn, companyId, customerIdList);
		} catch (SQLException ex) {
			throw new RuntimeException("db fail", ex);
		} finally {
			DBUtil.closeQuietly(conn);
		}

		Jedis jedis = this.jedisPool.getResource();
		try {
			ToolsProductclockCache.delCustomer(jedis, companyId, customerIdList);
		} finally {
			jedis.close();
		}
		
		return Futures.immediateFuture(DeleteCustomerResponse.newBuilder()
				.setResult(DeleteCustomerResponse.Result.SUCC)
				.build());
	}

	@Override
	public ListenableFuture<GetCustomerProductResponse> getCustomerProduct(AdminHead head,
			GetCustomerProductRequest request) {
		final long companyId = head.getCompanyId();
		final List<Integer> customerIdList = request.getCustomerIdList();
		
		Map<Integer, List<ToolsDAOProtos.ProductclockCustomerProduct>> customerProductMap = null;
		Connection conn = null;
		try {
			conn = this.hikariDataSource.getConnection();
			
			customerProductMap = ToolsProductclockDB.getCustomerProduct(conn, companyId, customerIdList);
		} catch (SQLException ex) {
			throw new RuntimeException("db fail", ex);
		} finally {
			DBUtil.closeQuietly(conn);
		}
		
		Set<Integer> productIdSet = Sets.newTreeSet();
		for (Entry<Integer, List<ToolsDAOProtos.ProductclockCustomerProduct>> entry : customerProductMap.entrySet()) {
			for (ToolsDAOProtos.ProductclockCustomerProduct customerProduct : entry.getValue()) {
				productIdSet.add(customerProduct.getProductId());
			}
		}
		
		Map<Integer, ProductclockProtos.Product> productMap = ToolsProductclockUtil.getProduct(hikariDataSource, jedisPool, companyId, productIdSet);

		List<GetCustomerProductResponse.CustomerProduct> customerProductList = Lists.newArrayList();
		GetCustomerProductResponse.CustomerProduct.Builder customerProductBuilder = GetCustomerProductResponse.CustomerProduct.newBuilder();
		List<ProductclockProtos.Product> productList = Lists.newArrayList();
		for (Entry<Integer, List<ToolsDAOProtos.ProductclockCustomerProduct>> entry : customerProductMap.entrySet()) {
			int customerId = entry.getKey();
			customerProductBuilder.clear();
			productList.clear();
			
			for (ToolsDAOProtos.ProductclockCustomerProduct customerProduct : entry.getValue()) {
				int productId = customerProduct.getProductId();
				ProductclockProtos.Product product = productMap.get(productId);
				if (product == null) {
					continue;
				}
				
				ProductclockProtos.Product.Builder productBuilder = ProductclockProtos.Product.newBuilder()
						.mergeFrom(product)
						.setBuyTime(customerProduct.getBuyTime())
						.setDefaultRemindDay(customerProduct.getRemindPeriodDay());
				productList.add(productBuilder.build());
			}
			
			customerProductList.add(
					customerProductBuilder
						.setCustomerId(customerId)
						.addAllProduct(productList)
						.build()
					);
		}
		
		return Futures.immediateFuture(GetCustomerProductResponse.newBuilder()
				.addAllCustomerProduct(customerProductList)
				.build());
	}

	@Override
	public ListenableFuture<GetCustomerAdminResponse> getCustomerAdmin(AdminHead head,
			GetCustomerAdminRequest request) {
		final long companyId = head.getCompanyId();
		
		final int start = request.getStart();
		final int length = request.getLength();
		
		final List<Long> salorIdList = request.getSalerIdList();
		final Boolean hasProduct = request.hasHasProduct() ? request.getHasProduct() : null;
		final String customerName = request.hasCustomerName() ? request.getCustomerName() : null;
		
		DataPage<Integer> customerIdPage = null;
		Connection conn = null;
		try {
			conn = this.hikariDataSource.getConnection();
			
			customerIdPage = ToolsProductclockDB.getCustomerIdPage(conn, companyId, start, length, salorIdList, hasProduct, customerName);
		} catch (SQLException ex) {
			throw new RuntimeException("db fail",ex);
		} finally {
			DBUtil.closeQuietly(conn);
		}
		
		Map<Integer, ProductclockProtos.Customer> customerMap = ToolsProductclockUtil.getCustomer(hikariDataSource, jedisPool, companyId, customerIdPage.dataList());
		
		List<ProductclockProtos.Customer> customerList = Lists.newArrayList();
		for (int customerId : customerIdPage.dataList()) {
			ProductclockProtos.Customer customer = customerMap.get(customerId);
			if (customer != null) {
				customerList.add(customer);
			}
		}
		
		return Futures.immediateFuture(GetCustomerAdminResponse.newBuilder()
				.addAllCustomer(customerList)
				.setTotalSize(customerIdPage.totalSize())
				.setFilteredSize(customerIdPage.filteredSize())
				.build());
	}

	@Override
	public ListenableFuture<ImportCustomerResponse> importCustomer(AdminHead head, ImportCustomerRequest request) {
		final long companyId = head.getCompanyId();
		final List<ProductclockProtos.Customer> customerList = request.getCustomerList();
		
		// 检查customerList里面的参数是否合理
		List<ImportCustomerResponse.InvalidCustomer> invalidCustomerList = Lists.newArrayList();
		for (ProductclockProtos.Customer customer : customerList) {
			checkCustomerParams(customer, invalidCustomerList);
		}
		if (!invalidCustomerList.isEmpty()) {
			return Futures.immediateFuture(ImportCustomerResponse.newBuilder()
					.setResult(ImportCustomerResponse.Result.FAIL_CUSTOMER_INVALID)
					.setFailText("存在不合法的客户！")
					.addAllInvalidCustomer(invalidCustomerList)
					.build());
		}
		
		final long createUserId = head.getSession().getAdminId();
		final int createTime = (int) (System.currentTimeMillis() / 1000L);
		
		List<Integer> customerIdList = Lists.newArrayList();
		Connection conn = null;
		try {
			conn = this.hikariDataSource.getConnection();
			
			customerIdList = ToolsProductclockDB.insertCustomer(conn, companyId, customerList, createUserId, createTime);
		} catch (SQLException ex) {
			throw new RuntimeException("db fail", ex);
		} finally {
			DBUtil.closeQuietly(conn);
		}
		
		Jedis jedis = this.jedisPool.getResource();
		try {
			ToolsProductclockCache.delCustomer(jedis, companyId, customerIdList);
		} finally {
			jedis.close();
		}
		
		return Futures.immediateFuture(ImportCustomerResponse.newBuilder()
				.setResult(ImportCustomerResponse.Result.SUCC)
				.build());
	}
	
	private void checkCustomerParams(ProductclockProtos.Customer customer, List<ImportCustomerResponse.InvalidCustomer> invalidCustomerList) {
		final String customerName = customer.getCustomerName();
		if (customerName.isEmpty() || customerName.length() > 10) {
			invalidCustomerList.add(ImportCustomerResponse.InvalidCustomer.newBuilder()
					.setCustomerName(customerName)
					.setFailText("客户名称不合法！")
					.build());
			return;
		}
		
		final String mobileNo = customer.hasMobileNo() ? customer.getMobileNo() : null;
		if (mobileNo != null && !MobileNoUtil.isValid(mobileNo)) {
			invalidCustomerList.add(ImportCustomerResponse.InvalidCustomer.newBuilder()
					.setCustomerName(customerName)
					.setFailText("手机号不合法！")
					.build());
			return;
		}
		
		final Integer birthdaySolar = customer.hasBirthdaySolar() ? customer.getBirthdaySolar() : null;
		if (birthdaySolar != null && (birthdaySolar > 1798732800 || birthdaySolar < -1073030400)) {
			invalidCustomerList.add(ImportCustomerResponse.InvalidCustomer.newBuilder()
					.setCustomerName(customerName)
					.setFailText("生日(公历)不合法！")
					.build());
		}
		
		final Integer birthdayLunar = customer.hasBirthdayLunar() ? customer.getBirthdayLunar() : null;
		if (birthdayLunar != null && (birthdayLunar > 1798732800 || birthdayLunar < -1073030400)) {
			invalidCustomerList.add(ImportCustomerResponse.InvalidCustomer.newBuilder()
					.setCustomerName(customerName)
					.setFailText("生日(农历)不合法！")
					.build());
		}
		
		final Integer weddingSolar = customer.hasWeddingSolar() ? customer.getWeddingSolar() : null;
		if (weddingSolar != null && (weddingSolar > 1798732800 || weddingSolar < -1073030400)) {
			invalidCustomerList.add(ImportCustomerResponse.InvalidCustomer.newBuilder()
					.setCustomerName(customerName)
					.setFailText("结婚纪念日(公历)不合法！")
					.build());
		}
		
		final Integer weddingLunar = customer.hasWeddingLunar() ? customer.getWeddingLunar() : null;
		if (weddingLunar != null && (weddingLunar > 1798732800 || weddingLunar < -1073030400)) {
			invalidCustomerList.add(ImportCustomerResponse.InvalidCustomer.newBuilder()
					.setCustomerName(customerName)
					.setFailText("结婚纪念日(农历)不合法！")
					.build());
		}
		
		final String address = customer.hasAddress() ? customer.getAddress() : null;
		if (address != null && address.length() > 191) {
			invalidCustomerList.add(ImportCustomerResponse.InvalidCustomer.newBuilder()
					.setCustomerName(customerName)
					.setFailText("地址不合法！")
					.build());
			return;
		}
		
		final String remark = customer.hasRemark() ? customer.getRemark() : null;
		if (remark != null && remark.length() > 191) {
			invalidCustomerList.add(ImportCustomerResponse.InvalidCustomer.newBuilder()
					.setCustomerName(customerName)
					.setFailText("备注信息不合法！")
					.build());
			return;
		}
	}

	@Override
	public ListenableFuture<AssignedSalerResponse> assignedSaler(AdminHead head, AssignedSalerRequest request) {
		final long companyId = head.getCompanyId();
		
		final List<Integer> customerIdList = request.getCustomerIdList();
		Map<Integer, ProductclockProtos.Customer> customerMap = ToolsProductclockUtil.getCustomer(hikariDataSource, jedisPool, companyId, customerIdList);
		
		List<Integer> validCustomerIdList = Lists.newArrayList();
		for (int customerId :  customerIdList) {
			ProductclockProtos.Customer customer = customerMap.get(customerId);
			if (customer != null) {
				validCustomerIdList.add(customer.getCustomerId());
			}
		}
		
		final long salerId = request.getSalerId();
		UserProtos.User validUser = null;
		GetUserByIdResponse getUserByIdResponse = Futures.getUnchecked(adminUserService.getUserById(head, GetUserByIdRequest.newBuilder()
				.addUserId(salerId)
				.build()));
		for (UserProtos.User user : getUserByIdResponse.getUserList()) {
			if (user.getBase().getUserId() == salerId) {
				validUser = user;
			}
		}
		
		if (validUser == null) {
			return Futures.immediateFuture(AssignedSalerResponse.newBuilder()
					.setResult(AssignedSalerResponse.Result.FAIL_SALER_ID_INVALID)
					.setFailText("不存在的客户")
					.build());
		}
		
		Connection conn = null;
		try {
			conn = this.hikariDataSource.getConnection();
			
			ToolsProductclockDB.updateCustomerBelongSaler(conn, companyId, validCustomerIdList, salerId);
		} catch (SQLException ex) {
			throw new RuntimeException("db fail", ex);
		} finally {
			DBUtil.closeQuietly(conn);
		}
		
		Jedis jedis = this.jedisPool.getResource();
		try {
			ToolsProductclockCache.delCustomer(jedis, companyId, customerIdList);
		} finally {
			jedis.close();
		}
		
		return Futures.immediateFuture(AssignedSalerResponse.newBuilder()
				.setResult(AssignedSalerResponse.Result.SUCC)
				.build());
	}

	@Override
	public ListenableFuture<GetProductAdminResponse> getProductAdmin(AdminHead head, GetProductAdminRequest request) {
		final long companyId = head.getCompanyId();
		
		final int start = request.getStart() < 0 ? 0 : request.getStart();
		final int length = request.getLength() < 0 ? 0 : request.getLength() > 50 ? 50 : request.getLength();
		
		final String productName = request.hasProductName() ? request.getProductName() : null;
		
		DataPage<Integer> productIdPage = null;
		Connection conn = null;
		try {
			conn = this.hikariDataSource.getConnection();
			
			productIdPage = ToolsProductclockDB.getProductIdPage(conn, companyId, start, length, productName);
		} catch (SQLException ex) {
			throw new RuntimeException("db fail", ex);
		} finally {
			DBUtil.closeQuietly(conn);
		}
		
		Map<Integer, ProductclockProtos.Product> productMap = ToolsProductclockUtil.getProduct(hikariDataSource, jedisPool, companyId, productIdPage.dataList());
		
		List<ProductclockProtos.Product> productList = Lists.newArrayList();
		for (int productId : productIdPage.dataList()) {
			ProductclockProtos.Product product = productMap.get(productId);
			if (product != null) {
				productList.add(product);
			}
		}
		
		Jedis jedis = this.jedisPool.getResource();
		try {
			ToolsProductclockCache.delProduct(jedis, companyId, productIdPage.dataList());
		} finally {
			jedis.close();
		}
		
		return Futures.immediateFuture(GetProductAdminResponse.newBuilder()
				.addAllProduct(productList)
				.setTotalSize(productIdPage.totalSize())
				.setFilteredSize(productIdPage.filteredSize())
				.build());
	}

	@Override
	public ListenableFuture<CreateProductResponse> createProduct(AdminHead head, CreateProductRequest request) {
		final long companyId = head.getCompanyId();
		
		final String productName = request.getProductName();
		if (productName.length() > 191) {
			return Futures.immediateFuture(CreateProductResponse.newBuilder()
					.setResult(CreateProductResponse.Result.FAIL_PRODUCT_NAME_INVALID)
					.setFailText("产品名称不合法！")
					.build());
		}
		
		final int remindDay = request.getRemindPeriodDay();
		if (remindDay < 1) {
			return Futures.immediateFuture(CreateProductResponse.newBuilder()
					.setResult(CreateProductResponse.Result.FAIL_REMIND_DAY_INVALID)
					.setFailText("提醒时间不合法！")
					.build());
		}
		
		final String imageName = request.hasImageName() ? request.getImageName() : null;
		if (imageName != null && imageName.length() > 191) {
			return Futures.immediateFuture(CreateProductResponse.newBuilder()
					.setResult(CreateProductResponse.Result.FAIL_IMAGE_NAME_INVALID)
					.setFailText("图片不合法！")
					.build());
		}
		
		final String productDesc = request.hasProductDesc() ? request.getProductDesc() : null;
		if (productDesc != null && productDesc.length() > 191) {
			return Futures.immediateFuture(CreateProductResponse.newBuilder()
					.setResult(CreateProductResponse.Result.FAIL_PRODUCT_DESC_INVALID)
					.setFailText("产品描述过长！")
					.build());
		}
		
		ProductclockProtos.Product.Builder productBuilder = ProductclockProtos.Product.newBuilder()
				.setProductId(0)
				.setProductName(productName)
				.setDefaultRemindDay(remindDay);
		if (imageName != null) {
			productBuilder.setImageName(imageName);
		}
		if (productDesc != null) {
			productBuilder.setProductDesc(productDesc);
		}
		
		final long createAdminId = head.getSession().getAdminId();
		final int now = (int) (System.currentTimeMillis() / 1000L);
		
		List<Integer> productIdList;
		Connection conn = null;
		try {
			conn = this.hikariDataSource.getConnection();
			
			productIdList = ToolsProductclockDB.insertProduct(conn, companyId, 
					Collections.singleton(productBuilder.build()), 
					createAdminId, now);
		} catch (SQLException ex) {
			throw new RuntimeException("db fail", ex);
		} finally {
			DBUtil.closeQuietly(conn);
		}
		
		if (productIdList == null) {
			productIdList = Collections.emptyList();
		}
		
		Jedis jedis = this.jedisPool.getResource();
		try {
			ToolsProductclockCache.delProduct(jedis, companyId, productIdList);
		} finally {
			jedis.close();
		}
		
		return Futures.immediateFuture(CreateProductResponse.newBuilder()
				.setResult(CreateProductResponse.Result.SUCC)
				.setProductId(productIdList.isEmpty() ? 0 : productIdList.get(0))
				.build());
	}

	@Override
	public ListenableFuture<UpdateProductResponse> updateProduct(AdminHead head, UpdateProductRequest request) {
		final long companyId = head.getCompanyId();
		
		final int productId = request.getProductId();
		ProductclockProtos.Product product = ToolsProductclockUtil.getProduct(hikariDataSource, jedisPool, companyId, Collections.singleton(productId)).get(productId);
		if (product == null) {
			return Futures.immediateFuture(UpdateProductResponse.newBuilder()
					.setResult(UpdateProductResponse.Result.FAIL_PRODUCT_ID_INVALID)
					.setFailText("不存在的产品！")
					.build());
		}
		
		final String productName = request.getProductName();
		if (productName.length() > 191) {
			return Futures.immediateFuture(UpdateProductResponse.newBuilder()
					.setResult(UpdateProductResponse.Result.FAIL_PRODUCT_NAME_INVALID)
					.setFailText("产品名称过长！")
					.build());
		}
		
		final int remindDay = request.getRemindPeriodDay();
		if (remindDay < 1) {
			return Futures.immediateFuture(UpdateProductResponse.newBuilder()
					.setResult(UpdateProductResponse.Result.FAIL_REMIND_TIME_INVALID)
					.setFailText("提醒时间不合法！")
					.build());
		}
		
		final String imageName = request.hasImageName() ? request.getImageName() : null;
		if (imageName != null && imageName.length() > 191) {
			return Futures.immediateFuture(UpdateProductResponse.newBuilder()
					.setResult(UpdateProductResponse.Result.FAIL_IMAGE_NAME_INVALID)
					.setFailText("产品图表不合法！")
					.build());
		}
		
		final String productDesc = request.hasProductDesc() ? request.getProductDesc() : null;
		if (productDesc != null && productDesc.length() > 191) {
			return Futures.immediateFuture(UpdateProductResponse.newBuilder()
					.setResult(UpdateProductResponse.Result.FAIL_PRODUCT_DESC_INVALID)
					.setFailText("产品描述过长！")
					.build());
		}
		
		final long updateAdmin = head.getSession().getAdminId();
		final int updateTime = (int) (System.currentTimeMillis() / 1000L);

		Connection conn = null;
		try {
			conn = this.hikariDataSource.getConnection();
			
			ToolsProductclockDB.updateProduct(conn, companyId, productId,
					productName, remindDay, imageName, productDesc,
					updateAdmin, updateTime);
		} catch (SQLException ex) {
			throw new RuntimeException("db fail", ex);
		} finally {
			DBUtil.closeQuietly(conn);
		}
		
		Jedis jedis = this.jedisPool.getResource();
		try {
			ToolsProductclockCache.delProduct(jedis, companyId, Collections.singleton(productId));
		} finally {
			jedis.close();
		}
		
		return Futures.immediateFuture(UpdateProductResponse.newBuilder()
				.setResult(UpdateProductResponse.Result.SUCC)
				.build());
	}

	@Override
	public ListenableFuture<DeleteProductResponse> deleteProduct(AdminHead head, DeleteProductRequest request) {
		final long companyId = head.getCompanyId();
		
		final List<Integer> productIdList = request.getProductIdList();
		Map<Integer, ProductclockProtos.Product> productMap = ToolsProductclockUtil.getProduct(hikariDataSource, jedisPool, companyId, productIdList);
		List<Integer> validProductIdList = Lists.newArrayList();
		for (int productId : productIdList) {
			ProductclockProtos.Product product = productMap.get(productId);
			if (product != null) {
				validProductIdList.add(productId);
			}
		}
		
		Connection conn = null;
		try {
			conn = this.hikariDataSource.getConnection();
			
			ToolsProductclockDB.deleteProduct(conn, companyId, validProductIdList);
		} catch (SQLException ex) {
			throw new RuntimeException("db fail", ex);
		} finally {
			DBUtil.closeQuietly(conn);
		}
		
		Jedis jedis = this.jedisPool.getResource();
		try {
			ToolsProductclockCache.delProduct(jedis, companyId, validProductIdList);
		} finally {
			jedis.close();
		}
		
		return Futures.immediateFuture(DeleteProductResponse.newBuilder()
				.setResult(DeleteProductResponse.Result.SUCC)
				.build());
	}

	@Override
	public ListenableFuture<ImportProductResponse> importProduct(AdminHead head, ImportProductRequest request) {
		final long companyId = head.getCompanyId();
		
		final List<ProductclockProtos.Product> productList = request.getProductList();
		
		List<ImportProductResponse.InvalidProduct> invalidProductList = Lists.newArrayList();
		for (ProductclockProtos.Product product : productList) {
			checkProductParams(product, invalidProductList);
		}
		if (!invalidProductList.isEmpty()) {
			return Futures.immediateFuture(ImportProductResponse.newBuilder()
					.setResult(ImportProductResponse.Result.FAIL_PRODUCT_INVALID)
					.setFailText("存在不合法的产品")
					.addAllInvalidProduct(invalidProductList)
					.build());
		}
		
		final long createAdminId = head.getSession().getAdminId();
		final int createTime = (int) (System.currentTimeMillis() / 1000L);
		
		List<Integer> productIdList;
		Connection conn = null;
		try {
			conn = this.hikariDataSource.getConnection();
			
			productIdList = ToolsProductclockDB.insertProduct(conn, companyId, productList, createAdminId, createTime);
		} catch (SQLException ex) {
			throw new RuntimeException("db fail", ex);
		} finally {
			DBUtil.closeQuietly(conn);
		}
		
		Jedis jedis = this.jedisPool.getResource();
		try {
			ToolsProductclockCache.delProduct(jedis, companyId, productIdList);
		} finally {
			jedis.close();
		}
		
		return Futures.immediateFuture(ImportProductResponse.newBuilder()
				.setResult(ImportProductResponse.Result.SUCC)
				.build());
	}
	
	@Override
	public ListenableFuture<GetCommunicateRecordAdminResponse> getCommunicateRecordAdmin(AdminHead head,
			GetCommunicateRecordAdminRequest request) {
		final long companyId = head.getCompanyId();
		final int customerId = request.getCustomerId();
		
		final int start = request.getStart() < 0 ? 0 : request.getStart();
		final int length = request.getLength() < 0 ? 0 : request.getLength() > 50 ? 50 : request.getLength();

		DataPage<ProductclockProtos.CommunicateRecord> recordPage;
		Connection conn = null;
		try {
			conn = this.hikariDataSource.getConnection();
			
			recordPage = ToolsProductclockDB.getCommunicateRecordPage(conn, companyId, customerId, start, length);
		} catch (SQLException ex) {
			throw new RuntimeException("db fail", ex);
		} finally {
			DBUtil.closeQuietly(conn);
		}
		
		return Futures.immediateFuture(GetCommunicateRecordAdminResponse.newBuilder()
				.addAllCommunicateRecord(recordPage.dataList())
				.setTotalSize(recordPage.totalSize())
				.setFilteredSize(recordPage.filteredSize())
				.build());
	}

	private void checkProductParams(ProductclockProtos.Product product, List<ImportProductResponse.InvalidProduct> invalidProductList) {
		final String productName = product.getProductName();
		if (productName.length() > 191) {
			invalidProductList.add(ImportProductResponse.InvalidProduct.newBuilder()
					.setProductName(productName)
					.setFailText("产品名称不合法！")
					.build());
			return;
		}
		
		final int remindDay = product.getDefaultRemindDay();
		if (remindDay < 0) {
			invalidProductList.add(ImportProductResponse.InvalidProduct.newBuilder()
					.setProductName(productName)
					.setFailText("提醒时间不合法！")
					.build());
			return;
		}
		
		final String imageName = product.hasImageName() ? product.getImageName() : null;
		if (imageName != null && imageName.length() > 191) {
			invalidProductList.add(ImportProductResponse.InvalidProduct.newBuilder()
					.setProductName(productName)
					.setFailText("产品图片不合法！")
					.build());
			return;
		}
		
		final String productDesc = product.hasProductDesc() ? product.getProductDesc() : null;
		if (productDesc != null && productDesc.length() > 191) {
			invalidProductList.add(ImportProductResponse.InvalidProduct.newBuilder()
					.setProductName(productName)
					.setFailText("产品描述过长！")
					.build());
			return;
		}
	}
	
	private void doLoadRemindCustomer() {
		Map<Long, List<Integer>> companyCustomerMap;
		Connection conn = null;
		try {
			conn = this.hikariDataSource.getConnection();
			
			companyCustomerMap = ToolsProductclockDB.getRemindCustomerId(conn); // 获取要提醒的
		} catch (SQLException ex) {
			throw new RuntimeException("load remind customer fail", ex);
		} finally {
			DBUtil.closeQuietly(conn);
		}
		
		Calendar cal = Calendar.getInstance();
		cal.set(Calendar.HOUR_OF_DAY, 12);
		cal.set(Calendar.MINUTE, 0);
		cal.set(Calendar.SECOND, 0);
		if (System.currentTimeMillis() > cal.getTimeInMillis()) {
			cal.add(Calendar.DAY_OF_YEAR, 1);
		}
		
		for (Entry<Long, List<Integer>> entry : companyCustomerMap.entrySet()) {
			long companyId = entry.getKey();
			List<Integer> customerIdList = entry.getValue();
			
			Map<Integer, ProductclockProtos.Customer> customerMap = ToolsProductclockUtil.getCustomer(hikariDataSource, jedisPool, companyId, customerIdList);
			
			this.scheduledExecutorService.scheduleAtFixedRate(new Runnable() {
				
				@Override
				public void run() {
					ToolsProductclockServiceImpl.this.executorService.execute(new SendSecretaryMessageTask(companyId, customerMap.values()));
				}
				
			}, (cal.getTimeInMillis()-System.currentTimeMillis())/1000, 24 * 3600, TimeUnit.SECONDS);
		}
	}
	

	private final class SendSecretaryMessageTask implements Runnable {
		private final long companyId;
		private final Collection<ProductclockProtos.Customer> customers;
		
		public SendSecretaryMessageTask(long companyId, Collection<ProductclockProtos.Customer> customers) {
			this.companyId = companyId;
			this.customers = customers;
		}
		
		@Override
		public void run() {
			Map<Integer, ProductclockProtos.Customer> customerMap = Maps.newHashMap();
			for (ProductclockProtos.Customer customer : customers) {
				// 只提醒正常用户
				if (customer.hasState() && customer.getState().equals(ProductclockProtos.State.NORMAL)) {
					customerMap.put(customer.getCustomerId(), customer);
				}
			}
			
			Map<Integer, List<ToolsDAOProtos.ProductclockCustomerProduct>> customerProductMap;
			Connection conn = null;
			try {
				conn = ToolsProductclockServiceImpl.this.hikariDataSource.getConnection();
				
				customerProductMap = ToolsProductclockDB.getCustomerProduct(conn, companyId, customerMap.keySet());
			} catch (SQLException ex) {
				throw new RuntimeException("db fail", ex);
			} finally {
				DBUtil.closeQuietly(conn);
			}
			
			long now = System.currentTimeMillis();
			Map<Integer, List<Integer>> notifyCustomerProductMap = Maps.newHashMap();
			for (Entry<Integer, List<ToolsDAOProtos.ProductclockCustomerProduct>> entry : customerProductMap.entrySet()) {
				List<ToolsDAOProtos.ProductclockCustomerProduct> customerProductList = entry.getValue();
				for (ToolsDAOProtos.ProductclockCustomerProduct customerProduct : customerProductList) {
					int customerId = customerProduct.getCustomerId();
					
					ProductclockProtos.Customer customer = customerMap.get(customerId);
					if (customer == null) {
						break;
					}
					int daysAgoRemind = customer.hasDaysAgoRemind() ? (customer.getDaysAgoRemind() < 0 ? 0 : customer.getDaysAgoRemind()) : 0;
					
					int interval = ((int)(now/1000L) - customerProduct.getBuyTime())/(24*3600);
					int remindPeriodDay = customerProduct.getRemindPeriodDay() < 1 ? 1 : customerProduct.getRemindPeriodDay();
					if ((interval%remindPeriodDay == daysAgoRemind) && (interval != 0)) {
						
						List<Integer> productIdList = notifyCustomerProductMap.get(customerId);
						if (productIdList == null) {
							productIdList = Lists.newArrayList();
						}
						productIdList.add(customerProduct.getProductId());
						
						notifyCustomerProductMap.put(customerId, productIdList);
					}
				}
			}
			
			SimpleDateFormat df = new SimpleDateFormat("MM-dd");
			String date = df.format(new Date(now));
			String lunarDate = LunarUtil.solarTolunarTimeStamp((int)(now/1000L)).substring(5, 10);
			
			int notifyCount = 0;
			for (Entry<Integer, ProductclockProtos.Customer> entry : customerMap.entrySet()) {
				ProductclockProtos.Customer customer = entry.getValue();
				if (!customer.getIsRemind()) {
					continue;
				}
				
				int daysAgoRemind = customer.hasDaysAgoRemind() ? (customer.getDaysAgoRemind() < 0 ? 0 : customer.getDaysAgoRemind()) : 0;
				
				StringBuilder textBuilder = new StringBuilder();
				if (customer.hasBirthdaySolar() && date.equals(df.format(new Date((customer.getBirthdaySolar() - daysAgoRemind*24*3600) * 1000L)))) {
					textBuilder.append("生日还有").append(daysAgoRemind).append("天就要到了，快去准备准备吧~");
				} else if (customer.hasBirthdayLunar() && lunarDate.equals(LunarUtil.solarTolunarTimeStamp(customer.getBirthdayLunar() - daysAgoRemind).substring(5, 10))) {
					textBuilder.append("生日还有").append(daysAgoRemind).append("天就要到了，快去准备准备吧~");
				}
				if (customer.hasWeddingSolar() && date.equals(df.format(new Date((customer.getWeddingSolar() - daysAgoRemind*24*3600) * 1000L)))) {
					textBuilder.append("结婚纪念日还有").append(daysAgoRemind).append("天就要到了，快去准备准备吧~");
				} else if (customer.hasWeddingLunar() && lunarDate.equals(LunarUtil.solarTolunarTimeStamp(customer.getWeddingLunar() - daysAgoRemind).substring(5, 10))) {
					textBuilder.append("结婚纪念日还有").append(daysAgoRemind).append("天就要到了，快去准备准备吧~");
				}
				
				List<Integer> productIdList = notifyCustomerProductMap.get(customer.getCustomerId());
				if (productIdList != null && !productIdList.isEmpty()) {
					Map<Integer, ProductclockProtos.Product> productMap = ToolsProductclockUtil.getProduct(hikariDataSource, jedisPool, companyId, productIdList);
					for (Entry<Integer, ProductclockProtos.Product> productEntry : productMap.entrySet()) {
						if (textBuilder.length() != 0 ) {
							textBuilder.append("，并且他");
						}
						textBuilder.append("上次购买的产品“").append(productEntry.getValue().getProductName()).append("”，还有").append(daysAgoRemind).append("天用完了，赶紧去跟踪一下吧~");
					}
				}
				
				if (textBuilder.length() != 0 && customer.hasBelongUser()) {
					sendSecretaryMessage(companyId, customer.getBelongUser(), "您的客户：" + customer.getCustomerName() + textBuilder.toString());
					notifyCount ++;
					logger.info("小秘书闹钟提醒： 提醒内容：" + "您的客户" + customer.getCustomerName() + textBuilder.toString());
				}
			}
			
			logger.info("小秘书闹钟提醒： 本此提醒人数：" + notifyCount);
		}
		
	}
	
	private void sendSecretaryMessage(long companyId, long userId, String template) {
		adminOfficialService.sendSecretaryMessage(
				SystemHead.newBuilder()
						.setCompanyId(companyId)
						.build(),
				AdminOfficialProtos.SendSecretaryMessageRequest.newBuilder()
						.addUserId(userId)
						.setSendMsg(OfficialProtos.OfficialMessage.newBuilder()
								.setMsgSeq(0)
								.setMsgTime(0)
								.setIsFromUser(false)
								.setText(OfficialProtos.OfficialMessage.Text.newBuilder()
										.setContent(template))
								.build())
						.build());
	}
	
	private long checkTime(int timeStamp) {
		long time = timeStamp * 1000L;
		if (time > 1798732800000L || time < -1073030400000L) {
			return 0L;
		}
		return time;
	}

}
