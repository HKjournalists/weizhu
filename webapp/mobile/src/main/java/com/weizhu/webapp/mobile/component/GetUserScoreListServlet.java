package com.weizhu.webapp.mobile.component;

import java.io.IOException;
import java.util.Base64;
import java.util.HashMap;
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
import com.google.protobuf.ByteString;
import com.weizhu.proto.ComponentProtos.GetUserScoreListRequest;
import com.weizhu.proto.ComponentProtos.GetUserScoreListResponse;
import com.weizhu.common.utils.JsonUtil;
import com.weizhu.proto.ComponentProtos;
import com.weizhu.proto.ComponentService;
import com.weizhu.proto.WeizhuProtos.RequestHead;
import com.weizhu.web.ParamUtil;

@Singleton
@SuppressWarnings("serial")
public class GetUserScoreListServlet extends HttpServlet {

	private final Provider<RequestHead> requestHeadProvider;
	private final ComponentService componentService;
	
	@Inject
	public GetUserScoreListServlet(Provider<RequestHead> requestHeadProvider,ComponentService componentService){
		this.requestHeadProvider = requestHeadProvider;
		this.componentService = componentService;
	}
	
	@Override
	public void doPost(HttpServletRequest httpRequest, HttpServletResponse httpResponse) throws ServletException, IOException {
		// 获取userId
		final int userId = ParamUtil.getInt(httpRequest, "user_id", 0);
		// 获取size
		final int size = ParamUtil.getInt(httpRequest, "size", -1);
		//获取上一页最后一条OffsetIndex
		byte[] bs = null;
		try {
			bs = Base64.getUrlDecoder().decode(ParamUtil.getString(httpRequest, "offset_index", ""));
		} catch (IllegalArgumentException e) {
			bs = null;
		}
		final ByteString offsetIndex = bs == null ? null :ByteString.copyFrom(bs);
		
		// 获取requestHead
		final RequestHead requestHead = requestHeadProvider.get();
		
		GetUserScoreListRequest.Builder requestBuilder = GetUserScoreListRequest.newBuilder();
		requestBuilder.setUserId(userId);
		requestBuilder.setSize(size);
		// 判断上一页的最后一条是否存在，若存在则赋值
		if(offsetIndex != null){
			requestBuilder.setOffsetIndex(offsetIndex);
		}
		
		GetUserScoreListResponse response = Futures.getUnchecked(componentService.getUserScoreList(requestHead, requestBuilder.build()));
		
		// 将scoreUserList 组装成Map<Integer,ComponentProtos.ScoreUser> 
		Map<Integer,ComponentProtos.ScoreUser> scoreUserMap = new HashMap<Integer, ComponentProtos.ScoreUser>();
		for(ComponentProtos.ScoreUser scoreUser : response.getScoreUserList() ){
			scoreUserMap.put(scoreUser.getScoreId(), scoreUser);
		}
		
		JsonArray scoreArray = new JsonArray();
		// 获取scoreList
		for(ComponentProtos.Score score : response.getRefScoreList() ){
			JsonObject scoreObj = new JsonObject();
			scoreObj.addProperty("score_id", score.getScoreId());
			scoreObj.addProperty("score_name", score.getScoreName());
			if(score.hasImageName()){
				scoreObj.addProperty("image_name", score.getImageName());
			}
			scoreObj.addProperty("type", score.getType().toString());
			scoreObj.addProperty("result_view", score.getResultView().toString());
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
			// 获取当前用户的对当前批次的打分的打分情况
			ComponentProtos.ScoreUser scoreUser = scoreUserMap.get(score.getScoreId());
			scoreObj.addProperty("score_value", scoreUser.getScoreValue());
			scoreObj.addProperty("score_time", scoreUser.getScoreTime());
			scoreArray.add(scoreObj);
		}
		
		JsonObject result = new JsonObject();
		result.add("score_list", scoreArray);
		result.addProperty("has_more", response.getHasMore());
		if(!response.getOffsetIndex().isEmpty()){			
			result.addProperty("offset_index", Base64.getUrlEncoder().encodeToString(response.getOffsetIndex().toByteArray()));
		}
		
		httpResponse.setContentType("application/json;charset=UTF-8");
		JsonUtil.GSON.toJson(result, httpResponse.getWriter());
	}
	
	@Override
	public void doGet(HttpServletRequest httpRequest, HttpServletResponse httpResponse) throws ServletException, IOException {
		this.doPost(httpRequest, httpResponse);
	}
}
