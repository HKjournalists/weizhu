package com.weizhu.webapp.web.api.discover;

import java.io.IOException;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.common.util.concurrent.Futures;
import com.google.gson.JsonObject;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.weizhu.proto.DiscoverV2Service;
import com.weizhu.common.utils.HexUtil;
import com.weizhu.common.utils.JsonUtil;
import com.weizhu.proto.DiscoverV2Protos.GetModulePromptIndexRequest;
import com.weizhu.proto.DiscoverV2Protos.GetModulePromptIndexResponse;
import com.weizhu.proto.WeizhuProtos.RequestHead;
import com.weizhu.web.ParamUtil;

@Singleton
public class GetModulePromptIndexServlet extends HttpServlet {

	private static final long serialVersionUID = 1L;

	private final Provider<RequestHead> requestHeadProvider;
	private final DiscoverV2Service discoverV2Service;
	
	@Inject
	public GetModulePromptIndexServlet(Provider<RequestHead> requestHeadProvider, DiscoverV2Service discoverV2Service) {
		this.requestHeadProvider = requestHeadProvider;
		this.discoverV2Service = discoverV2Service;
	}
	
	@Override
	public void doPost(HttpServletRequest httpRequest, HttpServletResponse httpResponse) throws IOException {
		final int moduleId = ParamUtil.getInt(httpRequest, "module_id", 0);
		
		final RequestHead head = requestHeadProvider.get();
		
		GetModulePromptIndexResponse response = Futures.getUnchecked(discoverV2Service.getModulePromptIndex(head, GetModulePromptIndexRequest.newBuilder()
				.setModuleId(moduleId)
				.build()));
		
		JsonObject result = new JsonObject();
		if (response.hasPromptIndex()) {
			result.addProperty("prompt_index", HexUtil.bin2Hex(response.getPromptIndex().toByteArray()));
		}
		
		httpResponse.setContentType("application/json;charset=UTF-8");
		JsonUtil.GSON.toJson(result, httpResponse.getWriter());
	}
	
	@Override
	public void doGet(HttpServletRequest httpRequest, HttpServletResponse httpResponse) throws IOException {
		this.doPost(httpRequest, httpResponse);
	}
}
