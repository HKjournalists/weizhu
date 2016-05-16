package com.weizhu.webapp.web.api.discover;

import com.google.common.util.concurrent.Futures;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.weizhu.common.utils.JsonUtil;
import com.weizhu.proto.SystemProtos.GetAuthUrlRequest;
import com.weizhu.proto.SystemProtos.GetAuthUrlResponse;
import com.weizhu.proto.SystemService;
import com.weizhu.proto.WeizhuProtos.RequestHead;
import com.weizhu.web.ParamUtil;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@Singleton
@SuppressWarnings("serial")
public class GetAuthUrlServlet extends HttpServlet {

	private final Provider<RequestHead> requestHeadProvider;
	private final SystemService systemService;
	
	@Inject
	public GetAuthUrlServlet(Provider<RequestHead> requestHeadProvider, SystemService systemService) {
		this.requestHeadProvider = requestHeadProvider;
		this.systemService = systemService;
	}
	
	@Override
	protected void doGet(HttpServletRequest httpRequest, HttpServletResponse httpResponse) throws ServletException, IOException {
		this.doPost(httpRequest, httpResponse);
	}

	@Override
	protected void doPost(HttpServletRequest httpRequest, HttpServletResponse httpResponse) throws ServletException, IOException {
		final String url = ParamUtil.getString(httpRequest, "url", "");
		
		final RequestHead head = this.requestHeadProvider.get();
		
		GetAuthUrlResponse response = Futures.getUnchecked(
				this.systemService.getAuthUrl(head, 
						GetAuthUrlRequest.newBuilder()
						.setUrl(url)
						.build()));
		
		httpResponse.setContentType("application/json;charset=UTF-8");
		JsonUtil.PROTOBUF_JSON_FORMAT.print(response, httpResponse.getWriter());
	}
}
