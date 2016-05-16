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
import com.weizhu.common.utils.HexUtil;
import com.weizhu.common.utils.JsonUtil;
import com.weizhu.proto.ProductclockProtos;
import com.weizhu.proto.ProductclockProtos.GetCommunicateRecordRequest;
import com.weizhu.proto.ProductclockProtos.GetCommunicateRecordResponse;
import com.weizhu.proto.ToolsProductclockService;
import com.weizhu.proto.WeizhuProtos.RequestHead;
import com.weizhu.web.ParamUtil;

@Singleton
public class GetCommunicateRecordServlet extends HttpServlet {

	private static final long serialVersionUID = 1L;

	private final Provider<RequestHead> requestHeadProvider;
	private final ToolsProductclockService toolsProductclockService;
	
	@Inject
	public GetCommunicateRecordServlet(Provider<RequestHead> requestHeadProvider, ToolsProductclockService toolsProductclockService) {
		this.requestHeadProvider = requestHeadProvider;
		this.toolsProductclockService = toolsProductclockService;
	}
	
	@Override
	public void doPost(HttpServletRequest httpRequest, HttpServletResponse httpResponse) throws IOException {
		final int customerId = ParamUtil.getInt(httpRequest, "customer_id", 0);
		final String offSetIndexStr = ParamUtil.getString(httpRequest, "offset_index", null);
		final int size = ParamUtil.getInt(httpRequest, "size", 0);
		
		final RequestHead requestHead = requestHeadProvider.get();
		
		GetCommunicateRecordRequest.Builder requestBuilder = GetCommunicateRecordRequest.newBuilder()
				.setCustomerId(customerId)
				.setSize(size);
		if (offSetIndexStr != null && !offSetIndexStr.equals("0")) {
			requestBuilder.setOffsetIndex(ByteString.copyFrom(HexUtil.hex2bin(offSetIndexStr)));
		}
		
		GetCommunicateRecordResponse response = Futures.getUnchecked(toolsProductclockService.getCommunicateRecord(requestHead, requestBuilder.build()));

		JsonArray array = new JsonArray();
		for (ProductclockProtos.CommunicateRecord record :  response.getCommunicateRecordList()) {
			JsonObject obj = new JsonObject();
			
			obj.addProperty("record_id", record.getRecordId());
			obj.addProperty("content_text", record.getContentText());
			obj.addProperty("create_time", record.getCreateTime());
			array.add(obj);
		}
		
		JsonObject result = new JsonObject();
		result.add("communicate_record_list", array);
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
