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
public class UpdateScoreServlet  extends HttpServlet {

	private final Provider<AdminHead> adminHeadProvider;
	private final AdminComponentService adminComponentService;
	
	@Inject
	public UpdateScoreServlet(Provider<AdminHead> adminHeadProvider,
			AdminComponentService adminComponentService){
		this.adminHeadProvider = adminHeadProvider;
		this.adminComponentService = adminComponentService;
	}
	
	@Override
	public void doPost(HttpServletRequest httpRequest, HttpServletResponse httpResponse) throws ServletException, IOException {
		final int scoreId = ParamUtil.getInt(httpRequest, "score_id", 0);
		final String scoreName = ParamUtil.getString(httpRequest, "score_name", "");
		final String imageName = ParamUtil.getString(httpRequest, "image_name", "");
		String resultViewStr = ParamUtil.getString(httpRequest, "result_view", null);
		ComponentProtos.Score.ResultView resultView = null;
		if(resultViewStr != null){
			resultView = ComponentProtos.Score.ResultView.valueOf(resultViewStr);
		}
		final Integer startTime = ParamUtil.getInt(httpRequest, "start_time", null);
		final Integer endTime = ParamUtil.getInt(httpRequest, "end_time", null);
		Integer allowModelId = ParamUtil.getInt(httpRequest, "allow_model_id", null);
		
		final AdminHead head = this.adminHeadProvider.get();
		
		AdminComponentProtos.UpdateScoreRequest.Builder requestBuilder = AdminComponentProtos.UpdateScoreRequest.newBuilder();
		requestBuilder.setScoreId(scoreId);
		requestBuilder.setScoreName(scoreName);
		if(imageName != null){
			requestBuilder.setImageName(imageName);
		}
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
		
		AdminComponentProtos.UpdateScoreResponse response = Futures.getUnchecked(adminComponentService.updateScore(head, requestBuilder.build()));
		
		httpResponse.setContentType("application/json;charset=UTF-8");
		JsonUtil.PROTOBUF_JSON_FORMAT.print(response, httpResponse.getWriter());
	}
}
