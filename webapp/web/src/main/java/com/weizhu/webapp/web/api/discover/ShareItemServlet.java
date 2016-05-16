package com.weizhu.webapp.web.api.discover;

import java.io.IOException;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.common.util.concurrent.Futures;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.weizhu.common.utils.JsonUtil;
import com.weizhu.proto.DiscoverV2Protos.ShareItemRequest;
import com.weizhu.proto.DiscoverV2Protos.ShareItemResponse;
import com.weizhu.proto.DiscoverV2Service;
import com.weizhu.proto.WeizhuProtos.RequestHead;
import com.weizhu.web.ParamUtil;

@Singleton
public class ShareItemServlet extends HttpServlet {

	private static final long serialVersionUID = 1L;

	private final Provider<RequestHead> requestHeadProvider;
	private final DiscoverV2Service discoverV2Service;
	
	@Inject
	public ShareItemServlet(Provider<RequestHead> requestHeadProvider, DiscoverV2Service discoverV2Service) {
		this.requestHeadProvider = requestHeadProvider;
		this.discoverV2Service = discoverV2Service;
	}
	
	@Override
	public void doPost(HttpServletRequest httpRequest, HttpServletResponse httpResponse) throws IOException {
		final long itemId = ParamUtil.getLong(httpRequest, "item_id", 0L);
		
		final RequestHead head = requestHeadProvider.get();
		
		ShareItemResponse response = Futures.getUnchecked(discoverV2Service.shareItem(head, ShareItemRequest.newBuilder()
				.setItemId(itemId)
				.build()));
		
		httpResponse.setContentType("application/json;charset=UTF-8");
		JsonUtil.PROTOBUF_JSON_FORMAT.print(response, httpResponse.getWriter());
	}
	
	@Override
	public void doGet(HttpServletRequest httpRequest, HttpServletResponse httpResponse) throws IOException {
		this.doPost(httpRequest, httpResponse);
	}
}
