package com.weizhu.webapp.admin.api.tools.productclock;

import java.io.IOException;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.beust.jcommander.internal.Maps;
import com.google.common.collect.Sets;
import com.google.common.util.concurrent.Futures;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.weizhu.common.utils.JsonUtil;
import com.weizhu.proto.AdminProtos;
import com.weizhu.proto.AdminProtos.AdminHead;
import com.weizhu.proto.AdminProtos.GetAdminByIdRequest;
import com.weizhu.proto.AdminProtos.GetAdminByIdResponse;
import com.weizhu.proto.AdminService;
import com.weizhu.proto.ProductclockProtos;
import com.weizhu.proto.ProductclockProtos.GetCustomerProductRequest;
import com.weizhu.proto.ProductclockProtos.GetCustomerProductResponse;
import com.weizhu.proto.ProductclockProtos.GetCustomerProductResponse.CustomerProduct;
import com.weizhu.proto.ToolsProductclockService;
import com.weizhu.proto.UploadProtos.GetUploadUrlPrefixResponse;
import com.weizhu.proto.UploadService;
import com.weizhu.proto.WeizhuProtos.EmptyRequest;
import com.weizhu.web.ParamUtil;

@Singleton
public class GetCustomerProductServlet extends HttpServlet {

	private static final long serialVersionUID = 1L;

	private final Provider<AdminHead> adminHeadProvider;
	private final ToolsProductclockService toolsProductclockService;
	private final AdminService adminService;
	private final UploadService uploadService;
	
	@Inject
	public GetCustomerProductServlet(Provider<AdminHead> adminHeadProvider, ToolsProductclockService toolsProductclockService,
			AdminService adminService, UploadService uploadService) {
		this.adminHeadProvider = adminHeadProvider;
		this.toolsProductclockService = toolsProductclockService;
		this.adminService = adminService;
		this.uploadService = uploadService;
	}
	
	@Override
	public void doPost(HttpServletRequest httpRequest, HttpServletResponse httpResponse) throws IOException {
		final int customerId = ParamUtil.getInt(httpRequest, "customer_id", 0);
		
		final AdminHead adminHead = adminHeadProvider.get();
		
		GetCustomerProductResponse response = Futures.getUnchecked(toolsProductclockService.getCustomerProduct(adminHead, GetCustomerProductRequest.newBuilder()
				.addCustomerId(customerId)
				.build()));
		Set<Long> adminIdSet = Sets.newHashSet();
		for (CustomerProduct customerProduct : response.getCustomerProductList()) {
			for (ProductclockProtos.Product product : customerProduct.getProductList()) {
				if (product.hasCreateAdmin()) {
					adminIdSet.add(product.getCreateAdmin());
				}
				if (product.hasUpdateAdmin()) {
					adminIdSet.add(product.getUpdateAdmin());
				}
			}
		}
		
		GetAdminByIdResponse getAdminByIdResponse = Futures.getUnchecked(adminService.getAdminById(adminHead, GetAdminByIdRequest.newBuilder()
				.addAllAdminId(adminIdSet)
				.build()));
		Map<Long, AdminProtos.Admin> adminMap = Maps.newHashMap();
		for (AdminProtos.Admin admin : getAdminByIdResponse.getAdminList()) {
			adminMap.put(admin.getAdminId(), admin);
		}
		
		GetUploadUrlPrefixResponse getUploadUrlPrefixResponse = Futures.getUnchecked(uploadService.getUploadUrlPrefix(adminHead, EmptyRequest.getDefaultInstance()));
		
		
		JsonArray array = new JsonArray();
		for (GetCustomerProductResponse.CustomerProduct customerProduct : response.getCustomerProductList()) {
			for (ProductclockProtos.Product product : customerProduct.getProductList()) {
				JsonObject obj = new JsonObject();
				
				obj.addProperty("product_id", product.getProductId());
				obj.addProperty("product_name", product.getProductName());
				obj.addProperty("product_desc", product.hasProductDesc() ? product.getProductDesc() : "");
				String imageName = product.hasImageName() ? product.getImageName() : "";
				obj.addProperty("image_name", imageName);
				obj.addProperty("image_url", getUploadUrlPrefixResponse.getImageUrlPrefix() + imageName);
				obj.addProperty("default_remind_day", product.getDefaultRemindDay());
				obj.addProperty("buy_time", product.getBuyTime());
				
				AdminProtos.Admin admin = null;
				if (product.hasCreateAdmin()) {
					admin = adminMap.get(product.getCreateAdmin());
					obj.addProperty("create_admin_name", admin == null ? "未知[AdminId:" + product.getCreateAdmin() + "]" : admin.getAdminName());
				}
				if (product.hasUpdateTime()) {
					obj.addProperty("create_time", product.getCreateTime());
				}
				if (product.hasUpdateAdmin()) {
					admin = adminMap.get(product.getUpdateAdmin());
					obj.addProperty("update_admin_id", admin == null ? "未知[AdminId:" + product.getUpdateAdmin() + "]": admin.getAdminName());
				}
				if (product.hasUpdateTime()) {
					obj.addProperty("update_time", product.getUpdateTime());
				}
				array.add(obj);
			}
		}
		
		JsonObject result = new JsonObject();
		result.add("product_list", array);
		
		httpResponse.setContentType("application/json;charset=UTF-8");
		JsonUtil.GSON.toJson(result, httpResponse.getWriter());
	}
	
	@Override
	public void doGet(HttpServletRequest httpRequest, HttpServletResponse httpResponse) throws IOException {
		this.doPost(httpRequest, httpResponse);
	}
}
