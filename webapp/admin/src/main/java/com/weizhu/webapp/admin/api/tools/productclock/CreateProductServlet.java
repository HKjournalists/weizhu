package com.weizhu.webapp.admin.api.tools.productclock;

import java.io.IOException;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.common.util.concurrent.Futures;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.weizhu.proto.AdminProtos.AdminHead;
import com.weizhu.proto.ProductclockProtos.CreateProductRequest;
import com.weizhu.proto.ProductclockProtos.CreateProductResponse;
import com.weizhu.common.utils.JsonUtil;
import com.weizhu.proto.ToolsProductclockService;
import com.weizhu.web.ParamUtil;

@Singleton
public class CreateProductServlet extends HttpServlet {

	private static final long serialVersionUID = 1L;

	private final Provider<AdminHead> adminHeadProvider;
	private final ToolsProductclockService toolsProductclockService;
	
	@Inject
	public CreateProductServlet(Provider<AdminHead> adminHeadProvider, ToolsProductclockService toolsProductclockService) {
		this.adminHeadProvider = adminHeadProvider;
		this.toolsProductclockService = toolsProductclockService;
	}
	
	@Override
	public void doPost(HttpServletRequest httpRequest, HttpServletResponse httpResponse) throws IOException {
		final String productName = ParamUtil.getString(httpRequest, "product_name", "");
		final int remindPeriodDay = ParamUtil.getInt(httpRequest, "remind_period_day", 1);
		final String imageName = ParamUtil.getString(httpRequest, "image_name", null);
		final String productDesc = ParamUtil.getString(httpRequest, "product_desc", null);
		
		final AdminHead adminHead = adminHeadProvider.get();
		
		CreateProductRequest.Builder requestBuilder = CreateProductRequest.newBuilder()
				.setProductName(productName)
				.setRemindPeriodDay(remindPeriodDay);
		if (imageName != null) {
			requestBuilder.setImageName(imageName);
		}
		if (productDesc != null) {
			requestBuilder.setProductDesc(productDesc);
		}
		
		CreateProductResponse response = Futures.getUnchecked(toolsProductclockService.createProduct(adminHead, requestBuilder.build()));
		
		httpResponse.setContentType("application/json;charset=UTF-8");
		JsonUtil.PROTOBUF_JSON_FORMAT.print(response, httpResponse.getWriter());
	}
	
	@Override
	public void doGet(HttpServletRequest httpRequest, HttpServletResponse httpResponse) throws IOException {
		this.doPost(httpRequest, httpResponse);
	}
}
