package com.weizhu.webapp.admin.api.tools.productclock;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.common.util.concurrent.Futures;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.weizhu.proto.AdminProtos.AdminHead;
import com.weizhu.proto.ProductclockProtos.DeleteCustomerProductRequest;
import com.weizhu.proto.ProductclockProtos.DeleteCustomerProductResponse;
import com.weizhu.common.utils.JsonUtil;
import com.weizhu.proto.ToolsProductclockService;
import com.weizhu.web.ParamUtil;

@Singleton
public class DeleteCustomerProductServlet extends HttpServlet {

	private static final long serialVersionUID = 1L;
	
	private final Provider<AdminHead> adminHeadProvider;
	private final ToolsProductclockService toolsProductclockService;
	
	@Inject
	public DeleteCustomerProductServlet(Provider<AdminHead> adminHeadProvider, ToolsProductclockService toolsProductclockService) {
		this.adminHeadProvider = adminHeadProvider;
		this.toolsProductclockService = toolsProductclockService;
	}
	
	@Override
	public void doPost(HttpServletRequest httpRequest, HttpServletResponse httpResponse) throws IOException {
		final int customerId = ParamUtil.getInt(httpRequest, "customer_id", 0);
		final List<Integer> productIdList = ParamUtil.getIntList(httpRequest, "product_id_list", Collections.emptyList());
		
		final AdminHead adminHead = adminHeadProvider.get();
		
		DeleteCustomerProductResponse response = Futures.getUnchecked(toolsProductclockService.deleteCustomerProduct(adminHead, DeleteCustomerProductRequest.newBuilder()
				.addAllProductId(productIdList)
				.setCustomerId(customerId)
				.build()));
		
		httpResponse.setContentType("application/json;charset=UTF-8");
		JsonUtil.PROTOBUF_JSON_FORMAT.print(response, httpResponse.getWriter());
	}
	
	@Override
	public void doGet(HttpServletRequest httpRequest, HttpServletResponse httpResponse) throws IOException {
		this.doPost(httpRequest, httpResponse);
	}
}
