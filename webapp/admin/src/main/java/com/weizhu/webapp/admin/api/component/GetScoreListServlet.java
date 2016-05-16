package com.weizhu.webapp.admin.api.component;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

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
import com.weizhu.proto.AdminComponentProtos.GetScoreListRequest;
import com.weizhu.proto.AdminComponentProtos.GetScoreListResponse;
import com.weizhu.common.utils.JsonUtil;
import com.weizhu.proto.AdminComponentService;
import com.weizhu.proto.AdminProtos;
import com.weizhu.proto.AdminService;
import com.weizhu.proto.ComponentProtos;
import com.weizhu.proto.AdminProtos.AdminHead;
import com.weizhu.web.ParamUtil;

@Singleton
@SuppressWarnings("serial")
public class GetScoreListServlet extends HttpServlet {

	private final Provider<AdminHead> adminHeadProvider;
	private final AdminService adminService;
	private final AdminComponentService adminComponentService;
	
	@Inject
	public GetScoreListServlet(Provider<AdminHead> adminHeadProvider,
			AdminService adminService,
			AdminComponentService adminComponentService){
		this.adminHeadProvider = adminHeadProvider;
		this.adminService = adminService;
		this.adminComponentService = adminComponentService;
	}
	
	@Override
	public void doGet(HttpServletRequest httpRequest, HttpServletResponse httpResponse) throws ServletException, IOException {
		this.doPost(httpRequest, httpResponse);
	}
	
	@Override
	public void doPost(HttpServletRequest httpRequest, HttpServletResponse httpResponse) throws ServletException, IOException {
		final int start = ParamUtil.getInt(httpRequest, "start", 0);
		final int length = ParamUtil.getInt(httpRequest, "length", 0);
		String stateStr = ParamUtil.getString(httpRequest, "state", null);
		ComponentProtos.State state = null; 
		if(stateStr != null){
			state = ComponentProtos.State.valueOf(stateStr);
		}
		
		// 获取adminHead
		final AdminHead head = this.adminHeadProvider.get();
		
		GetScoreListResponse response = Futures.getUnchecked(adminComponentService.getScoreList(head, GetScoreListRequest.newBuilder()
				.setStart(start)
				.setLength(length)
				.setState(state)
				.build()));
		
		// 将scoreCountList 组装成Map<Integer,ComponentProtos.ScoreCount> 
		Map<Integer,ComponentProtos.ScoreCount> scoreCountMap = new HashMap<Integer, ComponentProtos.ScoreCount>();
		for(ComponentProtos.ScoreCount scoreCount : response.getRefScoreCountList()){
			scoreCountMap.put(scoreCount.getScoreId(), scoreCount);
		}
		
		// 获取admin信息,和allowModel信息
		Set<Long> adminIdSet = new TreeSet<Long>();
		for(ComponentProtos.Score score : response.getScoreList()){
			if(score.hasCreateAdminId()){
				adminIdSet.add(score.getCreateAdminId());
			}
			if(score.hasUpdateAdminId()){
				adminIdSet.add(score.getUpdateAdminId());
			}
		}
		
		final Map<Long, AdminProtos.Admin> refAdminMap = ComponentServletUtil.getAdminMap(adminService, head, adminIdSet);
		
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
			if(score.hasCreateAdminId()){
				scoreObj.addProperty("create_admin_id", score.getCreateAdminId());	
				scoreObj.addProperty("create_admin_name", ComponentServletUtil.getAdminName(refAdminMap, score.hasCreateAdminId(), score.getCreateAdminId()));
			}
			if(score.hasCreateTime()){
				scoreObj.addProperty("create_time", score.getCreateTime());					
			}
			if(score.hasUpdateAdminId()){
				scoreObj.addProperty("update_admin_id", score.getUpdateAdminId());	
				scoreObj.addProperty("update_admin_name", ComponentServletUtil.getAdminName(refAdminMap, score.hasUpdateAdminId(), score.getUpdateAdminId()));
			}
			if(score.hasUpdateTime()){					
				scoreObj.addProperty("update_time", score.getUpdateTime());
			}
			// 获取scoreCountList
			ComponentProtos.ScoreCount scoreCount = scoreCountMap.get(score.getScoreId());
			if(scoreCount != null){
				scoreObj.addProperty("user_count", scoreCount.getUserCount());
				scoreObj.addProperty("total_score", scoreCount.getTotalScore());
			}
			scoreArray.add(scoreObj);
		}
		
		JsonObject result = new JsonObject();
		result.add("score", scoreArray);
		result.addProperty("total_size", response.getTotalSize());
		result.addProperty("filter_size", response.getFilteredSize());
		httpResponse.setContentType("application/json;charset=UTF-8");
		JsonUtil.GSON.toJson(result, httpResponse.getWriter());
	}
}
