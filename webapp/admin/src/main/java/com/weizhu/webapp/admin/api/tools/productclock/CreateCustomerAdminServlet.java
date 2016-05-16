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
import com.weizhu.proto.ProductclockProtos.CreateCustomerRequest;
import com.weizhu.proto.ProductclockProtos.CreateCustomerResponse;
import com.weizhu.proto.ProductclockProtos.Gender;
import com.weizhu.proto.ToolsProductclockService;
import com.weizhu.web.ParamUtil;

@Singleton
public class CreateCustomerAdminServlet extends HttpServlet {

	private static final long serialVersionUID = 1L;

	private final Provider<AdminHead> adminHeadProvider;
	private final ToolsProductclockService toolsProductclockService;
	
	@Inject
	public CreateCustomerAdminServlet(Provider<AdminHead> adminHeadProvider, ToolsProductclockService toolsProductclockService) {
		this.adminHeadProvider = adminHeadProvider;
		this.toolsProductclockService = toolsProductclockService;
	}
	
	@Override
	public void doPost(HttpServletRequest httpRequest, HttpServletResponse httpResponse) throws IOException {
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
		
		final boolean isRemind = ParamUtil.getBoolean(httpRequest, "is_remind", false);
		final int daysAgoRemind = ParamUtil.getInt(httpRequest, "days_ago_remind", 0);
		
		CreateCustomerRequest.Builder builder = CreateCustomerRequest.newBuilder()
				.setCustomerName(customerName)
				.setIsRemind(isRemind)
				.setDaysAgoRemind(daysAgoRemind);
		if (mobileNo != null) {
			builder.setMobileNo(mobileNo);
		}
		if (gender != null) {
			builder.setGender(gender);
		}
		if (birthdaySolar != null) {
			builder.setBirthdaySolar(birthdaySolar);
		}
		if (birthdayLunar != null) {
			builder.setBirthdayLunar(birthdayLunar);
		}
		if (weddingSolar != null) {
			builder.setWeddingSolar(weddingSolar);
		}
		if (weddingLunar != null) {
			builder.setWeddingLunar(weddingLunar);
		}
		if (address != null) {
			builder.setAddress(address);
		}
		if (remark != null) {
			builder.setRemark(remark);
		}
		
		final AdminHead adminHead = adminHeadProvider.get();
		
		CreateCustomerResponse response = Futures.getUnchecked(toolsProductclockService.createCustomer(adminHead, builder.build()));
		
		httpResponse.setContentType("application/json;charset=UTF-8");
		JsonUtil.PROTOBUF_JSON_FORMAT.print(response, httpResponse.getWriter());
	}
	
	@Override
	public void doGet(HttpServletRequest httpRequest, HttpServletResponse httpResponse) throws IOException {
		this.doPost(httpRequest, httpResponse);
	}
}
