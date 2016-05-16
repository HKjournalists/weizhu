package com.weizhu.webapp.admin.api.tools.productclock;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.beust.jcommander.internal.Lists;
import com.beust.jcommander.internal.Maps;
import com.google.common.collect.Sets;
import com.google.common.util.concurrent.Futures;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.weizhu.common.service.ServiceUtil;
import com.weizhu.common.utils.JsonUtil;
import com.weizhu.proto.AdminProtos;
import com.weizhu.proto.AdminProtos.AdminHead;
import com.weizhu.proto.AdminProtos.GetAdminByIdRequest;
import com.weizhu.proto.AdminProtos.GetAdminByIdResponse;
import com.weizhu.proto.AdminService;
import com.weizhu.proto.AdminUserProtos.GetUserByIdRequest;
import com.weizhu.proto.AdminUserProtos.GetUserByIdResponse;
import com.weizhu.proto.AdminUserService;
import com.weizhu.proto.ProductclockProtos;
import com.weizhu.proto.ProductclockProtos.GetCustomerAdminRequest;
import com.weizhu.proto.ProductclockProtos.GetCustomerAdminResponse;
import com.weizhu.proto.ProductclockProtos.GetCustomerProductRequest;
import com.weizhu.proto.ProductclockProtos.GetCustomerProductResponse;
import com.weizhu.proto.ProductclockProtos.GetCustomerProductResponse.CustomerProduct;
import com.weizhu.proto.UploadProtos.GetUploadUrlPrefixResponse;
import com.weizhu.proto.ToolsProductclockService;
import com.weizhu.proto.UploadService;
import com.weizhu.proto.UserProtos;
import com.weizhu.web.ParamUtil;

@Singleton
public class GetCustomerAdminServlet extends HttpServlet {

	private static final long serialVersionUID = 1L;

	private final Provider<AdminHead> adminHeadProvider;
	private final ToolsProductclockService toolsProductclockService;
	private final AdminUserService adminUserService;
	private final AdminService adminService;
	private final UploadService uploadService;
	
	@Inject
	public GetCustomerAdminServlet(Provider<AdminHead> adminHeadProvider, ToolsProductclockService toolsProductclockService, 
			AdminUserService adminUserService, AdminService adminService,
			UploadService uploadService) {
		this.adminHeadProvider = adminHeadProvider;
		this.toolsProductclockService = toolsProductclockService;
		this.adminUserService = adminUserService;
		this.adminService = adminService;
		this.uploadService = uploadService;
	}
	
	@Override
	public void doPost(HttpServletRequest httpRequest, HttpServletResponse httpResponse) throws IOException {
		final int start = ParamUtil.getInt(httpRequest, "start", 0);
		final int length = ParamUtil.getInt(httpRequest, "length", 10);
		
		final List<Long> salerId = ParamUtil.getLongList(httpRequest, "saler_id", Collections.emptyList());
		final Boolean hasProduct = ParamUtil.getBoolean(httpRequest, "has_product", null);
		final String customerName = ParamUtil.getString(httpRequest, "customer_name", null);
		
		GetCustomerAdminRequest.Builder requestBuilder = GetCustomerAdminRequest.newBuilder()
				.setStart(start)
				.setLength(length);
		if (salerId != null) {
			requestBuilder.addAllSalerId(salerId);
		}
		if (hasProduct != null) {
			requestBuilder.setHasProduct(hasProduct);
		}
		if (customerName != null) {
			requestBuilder.setCustomerName(customerName);
		}
		
		final AdminHead adminHead = adminHeadProvider.get();
		
		GetCustomerAdminResponse response = Futures.getUnchecked(toolsProductclockService.getCustomerAdmin(adminHead, requestBuilder.build()));
		
		List<Integer> customerIdList = Lists.newArrayList();
		Set<Long> salerIdSet = Sets.newTreeSet();
		Set<Long> adminIdSet = Sets.newTreeSet();
		for (ProductclockProtos.Customer customer : response.getCustomerList()) {
			customerIdList.add(customer.getCustomerId());
			if (customer.hasBelongUser()) {
				salerIdSet.add(customer.getBelongUser());
			}
			if (customer.hasCreateAdmin()) {
				adminIdSet.add(customer.getCreateAdmin());
			}
			if (customer.hasUpdateAdmin()) {
				adminIdSet.add(customer.getUpdateAdmin());
			}
		}
		
		GetCustomerProductResponse getCustomerProductResponse = Futures.getUnchecked(toolsProductclockService.getCustomerProduct(adminHead, GetCustomerProductRequest.newBuilder()
				.addAllCustomerId(customerIdList)
				.build()));
		Map<Integer, CustomerProduct> customerProductMap = Maps.newHashMap();
		for (CustomerProduct customerProduct : getCustomerProductResponse.getCustomerProductList()) {
			
			customerProductMap.put(customerProduct.getCustomerId(), customerProduct);
			
			for (ProductclockProtos.Product product : customerProduct.getProductList()) {
				if (product.hasCreateAdmin()) {
					adminIdSet.add(product.getCreateAdmin());
				}
				if (product.hasUpdateAdmin()) {
					adminIdSet.add(product.getUpdateAdmin());
				}
			}
		}
		
		GetUserByIdResponse getUserByIdResponse = Futures.getUnchecked(adminUserService.getUserById(adminHead, GetUserByIdRequest.newBuilder()
				.addAllUserId(salerIdSet)
				.build()));
		Map<Long, UserProtos.User> userMap = Maps.newHashMap();
		for (UserProtos.User user : getUserByIdResponse.getUserList()) {
			userMap.put(user.getBase().getUserId(), user);
		}
		
		GetAdminByIdResponse getAdminByIdResponse = Futures.getUnchecked(adminService.getAdminById(adminHead, GetAdminByIdRequest.newBuilder()
				.addAllAdminId(adminIdSet)
				.build()));
		Map<Long, AdminProtos.Admin> adminMap = Maps.newHashMap();
		for (AdminProtos.Admin admin : getAdminByIdResponse.getAdminList()) {
			adminMap.put(admin.getAdminId(), admin);
		}
		
		GetUploadUrlPrefixResponse getUploadUrlPrefixResponse = Futures.getUnchecked(this.uploadService.getUploadUrlPrefix(adminHead, ServiceUtil.EMPTY_REQUEST));
		
		JsonArray array = new JsonArray();
		for (ProductclockProtos.Customer customer : response.getCustomerList()) {
			JsonObject obj = new JsonObject();
			
			int customerId = customer.getCustomerId();
			obj.addProperty("customer_id", customerId);
			obj.addProperty("customer_name", customer.getCustomerName());
			obj.addProperty("mobile_no", customer.hasMobileNo() ? customer.getMobileNo() : "");
			obj.addProperty("gender", customer.hasGender() ? customer.getGender().name() : "");
			if (customer.hasBirthdaySolar()) {
				obj.addProperty("birthday_solar", customer.getBirthdaySolar());
			}
			if (customer.hasBirthdayLunar()) {
				obj.addProperty("birthday_lunar", customer.getBirthdayLunar());
			}
			if (customer.hasWeddingSolar()) {
				obj.addProperty("wedding_solar", customer.getWeddingSolar());
			}
			if (customer.hasWeddingLunar()) {
				obj.addProperty("wedding_lunar", customer.getWeddingLunar());
			}
			obj.addProperty("address", customer.hasAddress() ? customer.getAddress() : "");
			if (customer.hasBelongUser()) {
				UserProtos.User user = userMap.get(customer.getBelongUser());
				if (user != null) {
					obj.addProperty("belong_saler", user.getBase().getUserName());
				}
			}
			obj.addProperty("is_remind", customer.getIsRemind());
			
			if (customer.hasDaysAgoRemind()) {
				obj.addProperty("days_ago_remind", customer.getDaysAgoRemind());
			}
			if (customer.hasRemark()) {
				obj.addProperty("remark", customer.getRemark());
			}
			
			if (customer.hasCreateAdmin()) {
				AdminProtos.Admin admin = adminMap.get(customer.getCreateAdmin());
				if (admin != null) {
					obj.addProperty("create_admin", adminMap.get(customer.getCreateAdmin()).getAdminName());
				}
			}
			if (customer.hasCreateTime()) {
				obj.addProperty("create_time", customer.getCreateTime());
			}
			if (customer.hasUpdateAdmin()) {
				AdminProtos.Admin admin = adminMap.get(customer.getCreateAdmin());
				if (admin != null) {
					obj.addProperty("update_admin", admin.getAdminName());
				}
			}
			if (customer.hasUpdateTime()) {
				obj.addProperty("update_time", customer.getUpdateTime());
			}
			
			JsonArray productArray = new JsonArray();
			CustomerProduct customerProduct = customerProductMap.get(customerId);
			if (customerProduct != null) {
				for (ProductclockProtos.Product product : customerProduct.getProductList()) {
					JsonObject productObj = new JsonObject();
					
					productObj.addProperty("product_id", product.getProductId());
					productObj.addProperty("product_name", product.getProductName());
					productObj.addProperty("product_desc", product.hasProductDesc() ? product.getProductDesc() : "");
					String imageName = product.hasImageName() ? product.getImageName() : "";
					productObj.addProperty("image_name", imageName);
					productObj.addProperty("image_url", getUploadUrlPrefixResponse.getImageUrlPrefix() + imageName);
					productObj.addProperty("default_remind_day", product.getDefaultRemindDay());
					productObj.addProperty("buy_time", product.getBuyTime());
					
					AdminProtos.Admin admin = null;
					if (product.hasCreateAdmin()) {
						admin = adminMap.get(product.getCreateAdmin());
						productObj.addProperty("create_admin_name", admin == null ? "未知[AdminId:" + product.getCreateAdmin() + "]" : admin.getAdminName());
					}
					if (product.hasUpdateTime()) {
						productObj.addProperty("create_time", product.getCreateTime());
					}
					if (product.hasUpdateAdmin()) {
						admin = adminMap.get(product.getUpdateAdmin());
						productObj.addProperty("update_admin_id", admin == null ? "未知[AdminId:" + product.getUpdateAdmin() + "]": admin.getAdminName());
					}
					if (product.hasUpdateTime()) {
						productObj.addProperty("update_time", product.getUpdateTime());
					}
					productArray.add(productObj);
				}
			}
			obj.add("product_list", productArray);
			
			array.add(obj);
		}
		
		JsonObject result = new JsonObject();
		result.add("customer_list", array);
		result.addProperty("total_size", response.getTotalSize());
		result.addProperty("filtered_size", response.getFilteredSize());
		
		httpResponse.setContentType("application/json;charset=UTF-8");
		JsonUtil.GSON.toJson(result, httpResponse.getWriter());
	}
	
	@Override
	public void doGet(HttpServletRequest httpRequest, HttpServletResponse httpResponse) throws IOException {
		this.doPost(httpRequest, httpResponse);
	}
	
}
