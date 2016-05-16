package com.weizhu.webapp.mobile.component;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.common.util.concurrent.Futures;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.weizhu.proto.ComponentProtos.ScoreRequest;
import com.weizhu.proto.ComponentProtos.ScoreResponse;
import com.weizhu.common.utils.JsonUtil;
import com.weizhu.proto.ComponentService;
import com.weizhu.proto.WeizhuProtos.RequestHead;
import com.weizhu.web.ParamUtil;

@Singleton
@SuppressWarnings("serial")
public class ScoreServlet extends HttpServlet {

	private final Provider<RequestHead> requestHeadProvider;
	private final ComponentService componentService;
	
	@Inject
	public ScoreServlet(Provider<RequestHead> requestHeadProvider,ComponentService componentService){
		this.requestHeadProvider = requestHeadProvider;
		this.componentService = componentService;
	}
	
	@Override
	public void doPost(HttpServletRequest httpRequest, HttpServletResponse httpResponse) throws ServletException, IOException {
		final int scoreId = ParamUtil.getInt(httpRequest, "score_id", 0);
		final int scoreValue = ParamUtil.getInt(httpRequest, "score_value", 0);
		
		// 获取requestHead
		final RequestHead requestHead = requestHeadProvider.get();
		
		ScoreResponse response = Futures.getUnchecked(componentService.score(requestHead, ScoreRequest.newBuilder()
				.setScoreId(scoreId)
				.setScoreValue(scoreValue)
				.build()));
		
		httpResponse.setContentType("application/json;charset=UTF-8");
		JsonUtil.PROTOBUF_JSON_FORMAT.print(response, httpResponse.getWriter());
	}
	
}
