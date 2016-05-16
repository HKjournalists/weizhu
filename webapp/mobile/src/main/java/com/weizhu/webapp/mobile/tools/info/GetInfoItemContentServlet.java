package com.weizhu.webapp.mobile.tools.info;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.common.util.concurrent.Futures;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.weizhu.common.utils.JsonUtil;
import com.weizhu.proto.ToolsService;
import com.weizhu.proto.ToolsProtos.GetInfoItemContentRequest;
import com.weizhu.proto.ToolsProtos.GetInfoItemContentResponse;
import com.weizhu.proto.WeizhuProtos.RequestHead;
import com.weizhu.web.ParamUtil;

@Singleton
@SuppressWarnings("serial")
public class GetInfoItemContentServlet extends HttpServlet {

	private final Provider<RequestHead> requestHeadProvider;
	private final ToolsService toolsService;
	
	@Inject
	public GetInfoItemContentServlet(Provider<RequestHead> requestHeadProvider, ToolsService toolsService) {
		this.requestHeadProvider = requestHeadProvider;
		this.toolsService = toolsService;
	}
	
	@Override
	protected void doGet(HttpServletRequest httpRequest, HttpServletResponse httpResponse) throws ServletException, IOException {
		this.doPost(httpRequest, httpResponse);
	}
	
	@Override
	protected void doPost(HttpServletRequest httpRequest, HttpServletResponse httpResponse) throws ServletException, IOException {
		int toolId = ParamUtil.getInt(httpRequest, "tool_id", 0);
		int itemId = ParamUtil.getInt(httpRequest, "item_id", 0);
		
		final RequestHead head = this.requestHeadProvider.get();
		
		GetInfoItemContentRequest request = GetInfoItemContentRequest.newBuilder()
				.setToolId(toolId)
				.setItemId(itemId)
				.build();
		
		GetInfoItemContentResponse response = Futures.getUnchecked(this.toolsService.getInfoItemContent(head, request));
		
		JsonObject resultObj = new JsonObject();
		resultObj.add("item_content", response.hasItemContent() ? JsonUtil.JSON_PARSER.parse(response.getItemContent().getContentJson()) : JsonNull.INSTANCE);
		
		httpResponse.setContentType("application/json;charset=UTF-8");
		JsonUtil.GSON.toJson(resultObj, httpResponse.getWriter());
	}

}
