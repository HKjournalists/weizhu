package com.weizhu.webapp.admin.api.component;

import java.io.IOException;

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
public class CreateScoreServlet  extends HttpServlet {

	private final Provider<AdminHead> adminHeadProvider;
	private final AdminComponentService adminComponentService;
	
	@Inject
	public CreateScoreServlet(Provider<AdminHead> adminHeadProvider,
			AdminComponentService adminComponentService){
		this.adminHeadProvider = adminHeadProvider;
		this.adminComponentService = adminComponentService;
	}
	
	@Override
	public void doPost(HttpServletRequest httpRequest, HttpServletResponse httpResponse) throws ServletException, IOException {
		final String scoreName = ParamUtil.getString(httpRequest, "score_name", "");
		final String imageName = ParamUtil.getString(httpRequest, "image_name", "");
		String typeStr = ParamUtil.getString(httpRequest, "type", null);
		ComponentProtos.Score.Type type = null;
		if(typeStr != null){
			type = ComponentProtos.Score.Type.valueOf(typeStr);
		}
		String resultViewStr = ParamUtil.getString(httpRequest, "result_view", null);
		ComponentProtos.Score.ResultView resultView = null;
		if(resultViewStr != null){
			resultView = ComponentProtos.Score.ResultView.valueOf(resultViewStr);
		}
		final Integer startTime = ParamUtil.getInt(httpRequest, "start_time", null);
		final Integer endTime = ParamUtil.getInt(httpRequest, "end_time", null);
		Integer allowModelId = ParamUtil.getInt(httpRequest, "allow_model_id", null);
		
		final AdminHead head = this.adminHeadProvider.get();
		
		AdminComponentProtos.CreateScoreRequest.Builder requestBuilder = AdminComponentProtos.CreateScoreRequest.newBuilder();
		requestBuilder.setScoreName(scoreName);
		if(imageName != null){
			requestBuilder.setImageName(imageName);
		}
		requestBuilder.setType(type);
		requestBuilder.setResultView(resultView);
		if(startTime != null){
			requestBuilder.setStartTime(startTime);
		}
		if(endTime != null){			
			requestBuilder.setEndTime(endTime);
		}
		if(allowModelId != null){
			requestBuilder.setAllowModelId(allowModelId);
		}
		
		AdminComponentProtos.CreateScoreResponse response = Futures.getUnchecked(adminComponentService.createScore(head, requestBuilder.build()));
		
		httpResponse.setContentType("application/json;charset=UTF-8");
		JsonUtil.PROTOBUF_JSON_FORMAT.print(response, httpResponse.getWriter());
	}
}
