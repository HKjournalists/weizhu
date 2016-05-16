package com.weizhu.webapp.mobile.tools.productclock;

import java.io.IOException;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.common.util.concurrent.Futures;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.weizhu.proto.ProductclockProtos.UpdateCommunicateRecordRequest;
import com.weizhu.proto.ProductclockProtos.UpdateCommunicateRecordResponse;
import com.weizhu.common.utils.JsonUtil;
import com.weizhu.proto.ToolsProductclockService;
import com.weizhu.proto.WeizhuProtos.RequestHead;
import com.weizhu.web.ParamUtil;

@Singleton
public class UpdateCommunicateRecordServlet extends HttpServlet {

	private static final long serialVersionUID = 1L;

	private final Provider<RequestHead> requestHeadProvider;
	private final ToolsProductclockService toolsProductclockService;
	
	@Inject
	public UpdateCommunicateRecordServlet(Provider<RequestHead> requestHeadProvider, ToolsProductclockService toolsProductclockService) {
		this.requestHeadProvider = requestHeadProvider;
		this.toolsProductclockService = toolsProductclockService;
	}
	
	@Override
	public void doPost(HttpServletRequest httpRequest, HttpServletResponse httpResponse) throws IOException {
		final int recordId = ParamUtil.getInt(httpRequest, "record_id", 0);
		final String contentText = ParamUtil.getString(httpRequest, "content_text", null);
		
		final RequestHead requestHead = requestHeadProvider.get();
		
		UpdateCommunicateRecordRequest.Builder requestBuilder = UpdateCommunicateRecordRequest.newBuilder()
				.setRecordId(recordId);
		if (contentText != null) {
			requestBuilder.setContentText(contentText);
		}
		
		UpdateCommunicateRecordResponse response = Futures.getUnchecked(toolsProductclockService.updateCommunicateRecord(requestHead, requestBuilder.build()));
	
		httpResponse.setContentType("application/json;charset=UTF-8");
		JsonUtil.PROTOBUF_JSON_FORMAT.print(response, httpResponse.getWriter());
	}
	
	@Override
	public void doGet(HttpServletRequest httpRequest, HttpServletResponse httpResponse) throws IOException {
		this.doPost(httpRequest, httpResponse);
	}
	
}
