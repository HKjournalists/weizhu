package com.weizhu.webapp.mobile.tools.productclock;

import java.io.IOException;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.common.util.concurrent.Futures;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.google.protobuf.ByteString;
import com.weizhu.common.utils.HexUtil;
import com.weizhu.common.utils.JsonUtil;
import com.weizhu.proto.ProductclockProtos;
import com.weizhu.proto.ProductclockProtos.GetCustomerListRequest;
import com.weizhu.proto.ProductclockProtos.GetCustomerListResponse;
import com.weizhu.proto.ToolsProductclockService;
import com.weizhu.proto.UserProtos;
import com.weizhu.proto.UserProtos.GetUserByIdRequest;
import com.weizhu.proto.UserProtos.GetUserResponse;
import com.weizhu.proto.UserService;
import com.weizhu.proto.WeizhuProtos.RequestHead;
import com.weizhu.web.ParamUtil;

@Singleton
public class GetCustomerListServlet extends HttpServlet {

	private static final long serialVersionUID = 1L;

	private final Provider<RequestHead> requestHeadProvider;
	private final ToolsProductclockService toolsProductclockService;
	private final UserService userService;
	
	@Inject
	public GetCustomerListServlet(Provider<RequestHead> requestHeadProvider, ToolsProductclockService toolsProductclockService,
			UserService userService) {
		this.requestHeadProvider = requestHeadProvider;
		this.toolsProductclockService = toolsProductclockService;
		this.userService = userService;
	}
	
	@Override
	public void doPost(HttpServletRequest httpRequest, HttpServletResponse httpResponse) throws IOException {
		final String offSetIndexStr = ParamUtil.getString(httpRequest, "offset_index", null);
		final int size = ParamUtil.getInt(httpRequest, "size", 0);
		
		final Boolean hasProduct = ParamUtil.getBoolean(httpRequest, "has_product", null);
		
		final RequestHead requestHead = requestHeadProvider.get();
		
		GetCustomerListRequest.Builder requestBuilder = GetCustomerListRequest.newBuilder()
				.setSize(size);
		if (offSetIndexStr != null && !offSetIndexStr.equals("0")) {
			requestBuilder.setOffsetIndex(ByteString.copyFrom(HexUtil.hex2bin(offSetIndexStr)));
		}
		if (hasProduct != null) {
			requestBuilder.setHasProduct(hasProduct);
		}
		
		GetCustomerListResponse response = Futures.getUnchecked(toolsProductclockService.getCustomerList(requestHead, requestBuilder.build()));
		Set<Long> userIdSet = Sets.newTreeSet();
		for (ProductclockProtos.Customer customer : response.getCustomerList()) {
			if (customer.hasBelongUser()) {
				userIdSet.add(customer.getBelongUser());
			}
		}
		
		GetUserResponse getUserResponse = Futures.getUnchecked(userService.getUserById(requestHead, GetUserByIdRequest.newBuilder()
				.addAllUserId(userIdSet)
				.build()));
		Map<Long, UserProtos.User> userMap = Maps.newHashMap();
		for (UserProtos.User user : getUserResponse.getUserList()) {
			userMap.put(user.getBase().getUserId(), user);
		}
		
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
			if (customer.hasIsRemindToday()) {
				obj.addProperty("is_remind_today", customer.getIsRemindToday());
			}

			array.add(obj);
		}
		
		JsonObject result = new JsonObject();
		result.add("customer_list", array);
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
