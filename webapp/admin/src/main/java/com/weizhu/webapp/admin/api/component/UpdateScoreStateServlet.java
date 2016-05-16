package com.weizhu.webapp.admin.api.component;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.common.util.concurrent.Futures;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.weizhu.common.utils.JsonUtil;
import com.weizhu.proto.AdminComponentProtos;
import com.weizhu.proto.AdminComponentService;
import com.weizhu.proto.ComponentProtos;
import com.weizhu.proto.AdminProtos.AdminHead;
import com.weizhu.web.ParamUtil;

@Singleton
@SuppressWarnings("serial")
public class UpdateScoreStateServlet extends HttpServlet {

	private final Provider<AdminHead> adminHeadProvider;
	private final AdminComponentService adminComponentService;
	
	@Inject
	public UpdateScoreStateServlet(Provider<AdminHead> adminHeadProvider,
			AdminComponentService adminComponentService){
		this.adminHeadProvider = adminHeadProvider;
		this.adminComponentService = adminComponentService;
	}
	
	@Override
	public void doPost(HttpServletRequest httpRequest, HttpServletResponse httpResponse) throws ServletException, IOException {
		// 获取参数
		List<Integer> scoreIdList = ParamUtil.getIntList(httpRequest, "score_id", Collections.<Integer>emptyList());
		String stateStr = ParamUtil.getString(httpRequest, "state", null);
		ComponentProtos.State state = null; 
		if(stateStr != null){
			state = ComponentProtos.State.valueOf(stateStr);
		}

		final AdminHead head = this.adminHeadProvider.get();
		
		AdminComponentProtos.UpdateScoreStateRequest.Builder requestBuilder = AdminComponentProtos.UpdateScoreStateRequest.newBuilder();
		requestBuilder.addAllScoreId(scoreIdList);
		requestBuilder.setState(state);
		
		AdminComponentProtos.UpdateScoreStateResponse response = Futures.getUnchecked(adminComponentService.updateScoreState(head, requestBuilder.build()));
		
		httpResponse.setContentType("application/json;charset=UTF-8");
		JsonUtil.PROTOBUF_JSON_FORMAT.print(response, httpResponse.getWriter());
	}
}
