package com.weizhu.webapp.admin.api.tools.productclock;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

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
import com.weizhu.proto.ProductclockProtos;
import com.weizhu.proto.ProductclockProtos.GetProductAdminRequest;
import com.weizhu.proto.ProductclockProtos.GetProductAdminResponse;
import com.weizhu.proto.ToolsProductclockService;
import com.weizhu.proto.UploadProtos.GetUploadUrlPrefixResponse;
import com.weizhu.proto.UploadService;
import com.weizhu.web.ParamUtil;

@Singleton
public class GetProductAdminServlet extends HttpServlet {

	private static final long serialVersionUID = 1L;

	private final Provider<AdminHead> adminHeadProvider;
	private final ToolsProductclockService toolsProductclockService;
	private final UploadService uploadService;
	private final AdminService adminService;
	
	@Inject
	public GetProductAdminServlet(Provider<AdminHead> adminHeadProvider, ToolsProductclockService toolsProductclockService,
			UploadService uploadService, AdminService adminService) {
		this.adminHeadProvider = adminHeadProvider;
		this.toolsProductclockService = toolsProductclockService;
		this.uploadService = uploadService;
		this.adminService = adminService;
	}
	
	@Override
	public void doPost(HttpServletRequest httpRequest, HttpServletResponse httpResponse) throws IOException {
		final int start = ParamUtil.getInt(httpRequest, "start", 0);
		final int length = ParamUtil.getInt(httpRequest, "length", 50);
		
		final String productName = ParamUtil.getString(httpRequest, "product_name", null);
		
		GetProductAdminRequest.Builder requestBuilder = GetProductAdminRequest.newBuilder()
				.setStart(start)
				.setLength(length);
		if (productName != null) {
			requestBuilder.setProductName(productName);
		}
		
		final AdminHead adminHead = adminHeadProvider.get();
		
		GetProductAdminResponse response = Futures.getUnchecked(toolsProductclockService.getProductAdmin(adminHead, requestBuilder.build()));
		Set<Long> adminIdSet = Sets.newTreeSet();
		for (ProductclockProtos.Product product : response.getProductList()) {
			if (product.hasCreateAdmin()) {
				adminIdSet.add(product.getCreateAdmin());
			}
			if (product.hasUpdateAdmin()) {
				adminIdSet.add(product.getUpdateAdmin());
			}
			
		}
		
		GetAdminByIdResponse adminResponse = Futures.getUnchecked(adminService.getAdminById(adminHead, GetAdminByIdRequest.newBuilder()
				.addAllAdminId(adminIdSet)
				.build()));
		Map<Long, AdminProtos.Admin> adminMap = new HashMap<Long, AdminProtos.Admin>();
		for (AdminProtos.Admin admin : adminResponse.getAdminList()) {
			adminMap.put(admin.getAdminId(), admin);
		}
		
		GetUploadUrlPrefixResponse getUploadUrlPrefixResponse = Futures.getUnchecked(this.uploadService.getUploadUrlPrefix(adminHead, ServiceUtil.EMPTY_REQUEST));
		
		JsonArray array = new JsonArray();
		for (ProductclockProtos.Product product : response.getProductList()) {
			JsonObject obj = new JsonObject();
			
			obj.addProperty("product_id", product.getProductId());
			obj.addProperty("product_name", product.getProductName());
			obj.addProperty("product_desc", product.hasProductDesc() ? product.getProductDesc() : "");
			String imageName = product.hasImageName() ? product.getImageName() : "";
			obj.addProperty("image_name", imageName);
			obj.addProperty("image_url", getUploadUrlPrefixResponse.getImageUrlPrefix() + imageName);
			obj.addProperty("remind_period_day", product.getDefaultRemindDay());
			
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
		
		JsonObject result = new JsonObject();
		result.add("product_list", array);
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
