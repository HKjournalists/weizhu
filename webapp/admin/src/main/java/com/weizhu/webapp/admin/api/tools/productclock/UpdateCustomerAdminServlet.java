package com.weizhu.webapp.admin.api.tools.productclock;

import java.io.IOException;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.common.util.concurrent.Futures;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.weizhu.common.utils.JsonUtil;
import com.weizhu.proto.AdminProtos.AdminHead;
import com.weizhu.proto.ProductclockProtos.Gender;
import com.weizhu.proto.ProductclockProtos.UpdateCustomerRequest;
import com.weizhu.proto.ProductclockProtos.UpdateCustomerResponse;
import com.weizhu.proto.ToolsProductclockService;
import com.weizhu.web.ParamUtil;

@Singleton
public class UpdateCustomerAdminServlet extends HttpServlet {

	private static final long serialVersionUID = 1L;

	private final Provider<AdminHead> adminHeadProvider;
	private final ToolsProductclockService toolsProductclockService;
	
	@Inject
	public UpdateCustomerAdminServlet(Provider<AdminHead> adminHeadProvider, ToolsProductclockService toolsProductclockService) {
		this.adminHeadProvider = adminHeadProvider;
		this.toolsProductclockService = toolsProductclockService;
	}
	
	@Override
	public void doPost(HttpServletRequest httpRequest, HttpServletResponse httpResponse) throws IOException {
		final int customerId = ParamUtil.getInt(httpRequest, "customer_id", 0);
		final String customerName = ParamUtil.getString(httpRequest, "customer_name", "");
		final String mobileNo = ParamUtil.getString(httpRequest, "mobile_no", null);
		final String genderStr = ParamUtil.getString(httpRequest, "gender", "MALE");
		Gender gender = null;
		for (Gender g : Gender.values()) {
			if (g.name().equals(genderStr)) {
				gender = g;
			}
		}
		final Integer birthdaySolar = ParamUtil.getInt(httpRequest, "birthday_solar", null);
		final Integer birthdayLunar = ParamUtil.getInt(httpRequest, "birthday_lunar", null);
		final Integer weddingSolar = ParamUtil.getInt(httpRequest, "wedding_solar", null);
		final Integer weddingLunar = ParamUtil.getInt(httpRequest, "wedding_lunar", null);
		
		final String address = ParamUtil.getString(httpRequest, "address", null);
		final String remark = ParamUtil.getString(httpRequest, "remark", null);
		final int daysAgoRemind = ParamUtil.getInt(httpRequest, "days_ago_remind", 0);
		
		final boolean isRemind = ParamUtil.getBoolean(httpRequest, "is_remind", false);
		
		UpdateCustomerRequest.Builder requestBuilder = UpdateCustomerRequest.newBuilder()
				.setCustomerId(customerId)
				.setCustomerName(customerName)
				.setIsRemind(isRemind)
				.setDaysAgoRemind(daysAgoRemind);
		if (mobileNo != null) {
			requestBuilder.setMobileNo(mobileNo);
		}
		if (gender != null) {
			requestBuilder.setGender(gender);
		}
		if (birthdaySolar != null) {
			requestBuilder.setBirthdaySolar(birthdaySolar);
		}
		if (birthdayLunar != null) {
			requestBuilder.setBirthdayLunar(birthdayLunar);
		}
		if (weddingSolar != null) {
			requestBuilder.setWeddingSolar(weddingSolar);
		}
		if (weddingLunar != null) {
			requestBuilder.setWeddingLunar(weddingLunar);
		}
		if (address != null) {
			requestBuilder.setAddress(address);
		}
		if (remark != null) {
			requestBuilder.setRemark(remark);
		}
		
		final AdminHead adminHead = adminHeadProvider.get();
		
		UpdateCustomerResponse response = Futures.getUnchecked(toolsProductclockService.updateCustomer(adminHead, requestBuilder.build()));
		
		httpResponse.setContentType("application/json;charset=UTF-8");
		JsonUtil.PROTOBUF_JSON_FORMAT.print(response, httpResponse.getWriter());
	}
	
	@Override
	public void doGet(HttpServletRequest httpRequest, HttpServletResponse httpResponse) throws IOException {
		this.doPost(httpRequest, httpResponse);
	}
}
