package com.weizhu.webapp.web.api.discover;

import java.io.IOException;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.common.util.concurrent.Futures;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.google.protobuf.ExtensionRegistry;
import com.googlecode.protobuf.format.JsonFormat.ParseException;
import com.weizhu.common.utils.JsonUtil;
import com.weizhu.proto.DiscoverV2Protos.ReportLearnItemRequest;
import com.weizhu.proto.DiscoverV2Service;
import com.weizhu.proto.WeizhuProtos.EmptyResponse;
import com.weizhu.proto.WeizhuProtos.RequestHead;
import com.weizhu.web.ParamUtil;

@Singleton
public class ReportLearnItemServlet extends HttpServlet {

	private static final long serialVersionUID = 1L;

	private final Provider<RequestHead> requestHeadProvider;
	private final DiscoverV2Service discoverV2Service;
	
	@Inject
	public ReportLearnItemServlet(Provider<RequestHead> requestHeadProvider, DiscoverV2Service discoverV2Service) {
		this.requestHeadProvider = requestHeadProvider;
		this.discoverV2Service = discoverV2Service;
	}
	
	@Override
	public void doPost(HttpServletRequest httpRequest, HttpServletResponse httpResponse) throws IOException {
		// 格式 ： 
		// {\"item_learn\":[{\"item_id\": 1,\"user_id\": 1,\"learn_time\": 1,\"learn_duration\": 1,\"learn_cnt\": 1},
		//                  {\"item_id\": 2,\"user_id\": 2,\"learn_time\": 2,\"learn_duration\": 2,\"learn_cnt\": 2}]}
		final String itemLearn = ParamUtil.getString(httpRequest, "params", null);
		
		ReportLearnItemRequest.Builder requestBuilder = ReportLearnItemRequest.newBuilder();
		if (itemLearn != null) {
			try {
				JsonUtil.PROTOBUF_JSON_FORMAT.merge(itemLearn, ExtensionRegistry.getEmptyRegistry(), requestBuilder);
			} catch (ParseException e) {

			}
		}
		
		final RequestHead head = requestHeadProvider.get();
		
		EmptyResponse response = Futures.getUnchecked(discoverV2Service.reportLearnItem(head, requestBuilder.build()));
		
		httpResponse.setContentType("application/json;charset=UTF-8");
		JsonUtil.PROTOBUF_JSON_FORMAT.print(response, httpResponse.getWriter());
	}
	
	@Override
	public void doGet(HttpServletRequest httpRequest, HttpServletResponse httpResponse) throws IOException {
		this.doPost(httpRequest, httpResponse);
	}
}
