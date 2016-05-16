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
import com.weizhu.proto.ProductclockProtos.CreateCustomerProductRequest;
import com.weizhu.proto.ProductclockProtos.CreateCustomerProductResponse;
import com.weizhu.common.utils.JsonUtil;
import com.weizhu.proto.ToolsProductclockService;
import com.weizhu.web.ParamUtil;

@Singleton
public class CreateCustomerProductServlet extends HttpServlet {

	private static final long serialVersionUID = 1L;

	private final Provider<AdminHead> adminHeadProvider;
	private final ToolsProductclockService toolsProductclockService;
	
	@Inject
	public CreateCustomerProductServlet(Provider<AdminHead> adminHeadProvider, ToolsProductclockService toolsProductclockService) {
		this.adminHeadProvider = adminHeadProvider;
		this.toolsProductclockService = toolsProductclockService;
	}
	
	@Override
	public void doPost(HttpServletRequest httpRequest, HttpServletResponse httpResponse) throws IOException {
		final int customerId = ParamUtil.getInt(httpRequest, "customer_id", 0);
		final int productId = ParamUtil.getInt(httpRequest, "product_id", 0);
		final int buyTime = ParamUtil.getInt(httpRequest, "buy_time", (int) (System.currentTimeMillis() / 1000L));
		final Integer remindPeriodDay = ParamUtil.getInt(httpRequest, "remind_period_day", null);
		
		CreateCustomerProductRequest.Builder requestBuilder = CreateCustomerProductRequest.newBuilder()
				.setCustomerId(customerId)
				.setProductId(productId)
				.setBuyTime(buyTime);
		if (remindPeriodDay != null) {
			requestBuilder.setRemindPeriodDay(remindPeriodDay);
		}
		
		final AdminHead adminHead = adminHeadProvider.get();
		
		CreateCustomerProductResponse response = Futures.getUnchecked(toolsProductclockService.createCustomerProduct(adminHead, requestBuilder.build()));
		
		httpResponse.setContentType("application/json;charset=UTF-8");
		JsonUtil.PROTOBUF_JSON_FORMAT.print(response, httpResponse.getWriter());
	}
	
	@Override
	public void doGet(HttpServletRequest httpRequest, HttpServletResponse httpResponse) throws IOException {
		this.doPost(httpRequest, httpResponse);
	}
	
}
