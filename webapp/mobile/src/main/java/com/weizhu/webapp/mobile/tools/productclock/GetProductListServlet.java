package com.weizhu.webapp.mobile.tools.productclock;

import java.io.IOException;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.common.util.concurrent.Futures;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.google.protobuf.ByteString;
import com.weizhu.common.service.ServiceUtil;
import com.weizhu.common.utils.HexUtil;
import com.weizhu.common.utils.JsonUtil;
import com.weizhu.proto.ProductclockProtos;
import com.weizhu.proto.ProductclockProtos.GetProductListRequest;
import com.weizhu.proto.ProductclockProtos.GetProductListResponse;
import com.weizhu.proto.ToolsProductclockService;
import com.weizhu.proto.UploadProtos.GetUploadUrlPrefixResponse;
import com.weizhu.proto.UploadService;
import com.weizhu.proto.WeizhuProtos.RequestHead;
import com.weizhu.web.ParamUtil;

@Singleton
public class GetProductListServlet extends HttpServlet {

	private static final long serialVersionUID = 1L;
	
	private final Provider<RequestHead> requestHeadProvider;
	private final ToolsProductclockService toolsProductclockService;
	private final UploadService uploadService;
	
	@Inject
	public GetProductListServlet(Provider<RequestHead> requestHeadProvider, ToolsProductclockService toolsProductclockService,
			UploadService uploadService) {
		this.requestHeadProvider = requestHeadProvider;
		this.toolsProductclockService = toolsProductclockService;
		this.uploadService = uploadService;
	}
	
	@Override
	public void doPost(HttpServletRequest httpRequest, HttpServletResponse httpResponse) throws IOException {
		final String offSetIndexStr = ParamUtil.getString(httpRequest, "offset_index", null);
		final int size = ParamUtil.getInt(httpRequest, "size", 0);
		
		final String productName = ParamUtil.getString(httpRequest, "product_name", null);
		
		final RequestHead requestHead = requestHeadProvider.get();
		
		GetProductListRequest.Builder requestBuilder = GetProductListRequest.newBuilder()
				.setSize(size);
		if (offSetIndexStr != null && !offSetIndexStr.equals("0")) {
			requestBuilder.setOffsetIndex(ByteString.copyFrom(HexUtil.hex2bin(offSetIndexStr)));
		}
		if (productName != null) {
			requestBuilder.setProductName(productName);
		}
		
		GetProductListResponse response = Futures.getUnchecked(toolsProductclockService.getProductList(requestHead, requestBuilder.build()));

		GetUploadUrlPrefixResponse getUploadUrlPrefixResponse = Futures.getUnchecked(this.uploadService.getUploadUrlPrefix(requestHead, ServiceUtil.EMPTY_REQUEST));
		
		JsonArray array = new JsonArray();
		for (ProductclockProtos.Product product : response.getProductList()) {
			JsonObject obj = new JsonObject();
			
			obj.addProperty("product_id", product.getProductId());
			obj.addProperty("product_name", product.getProductName());
			obj.addProperty("product_desc", product.hasProductDesc() ? product.getProductDesc() : "");
			String imageName = product.hasImageName() ? product.getImageName() : "";
			obj.addProperty("image_name", imageName);
			obj.addProperty("image_url", getUploadUrlPrefixResponse.getImageUrlPrefix() + imageName);
			obj.addProperty("default_remind_day", product.getDefaultRemindDay());
			
			array.add(obj);
		}
		
		JsonObject result = new JsonObject();
		result.add("product_list", array);
		result.addProperty("offset_index", HexUtil.bin2Hex(response.getOffsetIndex().toByteArray()));
		result.addProperty("has_more", response.getHasMore());
		
		httpResponse.setContentType("application/json;charset=UTF-8");
		JsonUtil.GSON.toJson(result, httpResponse.getWriter());
	}
	
	@Override
	public void doGet(HttpServletRequest httpRequest, HttpServletResponse httpResponse) throws IOException {
		this.doPost(httpRequest, httpResponse);
	}
}
