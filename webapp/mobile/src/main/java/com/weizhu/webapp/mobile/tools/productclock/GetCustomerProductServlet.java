package com.weizhu.webapp.mobile.tools.productclock;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.common.util.concurrent.Futures;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.weizhu.common.service.ServiceUtil;
import com.weizhu.common.utils.JsonUtil;
import com.weizhu.proto.ProductclockProtos;
import com.weizhu.proto.ProductclockProtos.GetCustomerProductRequest;
import com.weizhu.proto.ProductclockProtos.GetCustomerProductResponse;
import com.weizhu.proto.ToolsProductclockService;
import com.weizhu.proto.UploadProtos.GetUploadUrlPrefixResponse;
import com.weizhu.proto.UploadService;
import com.weizhu.proto.WeizhuProtos.RequestHead;
import com.weizhu.web.ParamUtil;

@Singleton
public class GetCustomerProductServlet extends HttpServlet {

	private static final long serialVersionUID = 1L;

	private final Provider<RequestHead> requestHeadProvider;
	private final ToolsProductclockService toolsProductclockService;
	private final UploadService uploadService;
	
	@Inject
	public GetCustomerProductServlet(Provider<RequestHead> requestHeadProvider, ToolsProductclockService toolsProductclockService, 
			UploadService uploadService) {
		this.requestHeadProvider = requestHeadProvider;
		this.toolsProductclockService = toolsProductclockService;
		this.uploadService = uploadService;
	}
	
	@Override
	public void doPost(HttpServletRequest httpRequest, HttpServletResponse httpResponse) throws IOException {
		final List<Integer> customerIdList = ParamUtil.getIntList(httpRequest, "customer_id_list", Collections.emptyList());
		
		final RequestHead requestHead = requestHeadProvider.get();
		
		GetCustomerProductResponse response = Futures.getUnchecked(toolsProductclockService.getCustomerProduct(requestHead, GetCustomerProductRequest.newBuilder()
				.addAllCustomerId(customerIdList)
				.build()));
		
		GetUploadUrlPrefixResponse getUploadUrlPrefixResponse = Futures.getUnchecked(this.uploadService.getUploadUrlPrefix(requestHead, ServiceUtil.EMPTY_REQUEST));
		
		JsonArray array = new JsonArray();
		for (ProductclockProtos.GetCustomerProductResponse.CustomerProduct customerProduct : response.getCustomerProductList()) {
			JsonArray productArray = new JsonArray();
			
			List<ProductclockProtos.Product> productList = customerProduct.getProductList();
			for (ProductclockProtos.Product product : productList) {
				JsonObject obj = new JsonObject();
				
				obj.addProperty("product_id", product.getProductId());
				obj.addProperty("product_name", product.getProductName());
				obj.addProperty("product_desc", product.hasProductDesc() ? product.getProductDesc() : "");
				String imageName = product.hasImageName() ? product.getImageName() : "";
				obj.addProperty("image_name", imageName);
				obj.addProperty("image_url", getUploadUrlPrefixResponse.getImageUrlPrefix() + imageName);
				obj.addProperty("default_remind_day", product.getDefaultRemindDay());
				obj.addProperty("buy_time", product.getBuyTime());
				
				productArray.add(obj);
			}
			JsonObject obj = new JsonObject();
			obj.addProperty("customer_id", customerProduct.getCustomerId());
			obj.add("customer_product", productArray);
			
			array.add(obj);
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
