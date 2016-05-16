package com.weizhu.webapp.mobile.component;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.common.util.concurrent.Futures;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.weizhu.proto.ComponentProtos.GetScoreByIdRequest;
import com.weizhu.proto.ComponentProtos.GetScoreByIdResponse;
import com.weizhu.common.utils.JsonUtil;
import com.weizhu.proto.ComponentProtos;
import com.weizhu.proto.ComponentService;
import com.weizhu.proto.WeizhuProtos.RequestHead;
import com.weizhu.web.ParamUtil;

@SuppressWarnings("serial")
@Singleton
public class GetScoreByIdsServlet extends HttpServlet{

	private final Provider<RequestHead> requestHeadProvider;
	private final ComponentService componentService;
	
	@Inject
	public GetScoreByIdsServlet(Provider<RequestHead> requestHeadProvider,ComponentService componentService){
		this.requestHeadProvider = requestHeadProvider;
		this.componentService = componentService;
	}
	
	@Override
	public void doPost(HttpServletRequest httpRequest, HttpServletResponse httpResponse) throws ServletException, IOException {
		// 获取参数
		List<Integer> scoreIdList = ParamUtil.getIntList(httpRequest, "score_id", Collections.<Integer>emptyList());
		
		// 获取requestHead
		final RequestHead requestHead = requestHeadProvider.get();
		
		GetScoreByIdResponse response = Futures.getUnchecked(componentService.getScoreById(requestHead, GetScoreByIdRequest.newBuilder()
				.addAllScoreId(scoreIdList)
				.build()));
		
		// 将scoreCountList 组装成Map<Integer,ComponentProtos.ScoreCount> 
		Map<Integer,ComponentProtos.ScoreCount> scoreCountMap = new HashMap<Integer, ComponentProtos.ScoreCount>();
		for(ComponentProtos.ScoreCount scoreCount : response.getRefScoreCountList()){
			scoreCountMap.put(scoreCount.getScoreId(), scoreCount);
		}
		
		// 将scoreUserList 组装成Map<Integer,ComponentProtos.ScoreUser> 
		Map<Integer,ComponentProtos.ScoreUser> scoreUserMap = new HashMap<Integer, ComponentProtos.ScoreUser>();
		for(ComponentProtos.ScoreUser scoreUser : response.getRefScoreUserList() ){
			scoreUserMap.put(scoreUser.getScoreId(), scoreUser);
		}
		
		JsonArray scoreArray = new JsonArray();
		// 循环scoreList
		for(ComponentProtos.Score score : response.getScoreList()){
			JsonObject scoreObj = new JsonObject();
			scoreObj.addProperty("score_id", score.getScoreId());
			scoreObj.addProperty("score_name", score.getScoreName());
			if(score.hasImageName()){
				scoreObj.addProperty("image_name", score.getImageName());
			}
			scoreObj.addProperty("type", score.getType().name());
			scoreObj.addProperty("result_view", score.getResultView().name());
			if(score.hasStartTime()){					
				scoreObj.addProperty("start_time", score.getStartTime());
			}
			if(score.hasEndTime()){
				scoreObj.addProperty("end_time", score.getEndTime());					
			}
			if(score.hasAllowModelId()){					
				scoreObj.addProperty("allow_model_id", score.getAllowModelId());
			}
			scoreObj.addProperty("state", score.getState().toString());
			if(score.hasCreateTime()){
				scoreObj.addProperty("create_time", score.getCreateTime());					
			}
			// 获取scoreCountList
			ComponentProtos.ScoreCount scoreCount = scoreCountMap.get(score.getScoreId());
			if(scoreCount != null){
				scoreObj.addProperty("user_count", scoreCount.getUserCount());
				scoreObj.addProperty("total_score", scoreCount.getTotalScore());
			}
			// 获取当前用户的对当前批次的打分的打分情况
			ComponentProtos.ScoreUser scoreUser = scoreUserMap.get(score.getScoreId());
			if(scoreUser != null){				
				scoreObj.addProperty("score_value", scoreUser.getScoreValue());
				scoreObj.addProperty("score_time", scoreUser.getScoreTime());
			}
			scoreArray.add(scoreObj);
		}
		
		JsonObject result = new JsonObject();
		result.add("score", scoreArray);
		httpResponse.setContentType("application/json;charset=UTF-8");
		JsonUtil.GSON.toJson(result, httpResponse.getWriter());
	}
	
	@Override
	public void doGet(HttpServletRequest httpRequest, HttpServletResponse httpResponse) throws ServletException, IOException {
		this.doPost(httpRequest, httpResponse);
	}
}
