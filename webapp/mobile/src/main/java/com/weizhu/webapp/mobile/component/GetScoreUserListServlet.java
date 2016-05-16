package com.weizhu.webapp.mobile.component;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Base64;
import java.util.Date;

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
import com.weizhu.proto.ComponentProtos.GetScoreUserListRequest;
import com.weizhu.proto.ComponentProtos.GetScoreUserListResponse;
import com.weizhu.common.utils.JsonUtil;
import com.weizhu.proto.ComponentProtos;
import com.weizhu.proto.ComponentService;
import com.weizhu.proto.WeizhuProtos.RequestHead;
import com.weizhu.web.ParamUtil;

@SuppressWarnings("serial")
@Singleton
public class GetScoreUserListServlet  extends HttpServlet{

	private final Provider<RequestHead> requestHeadProvider;
	private final ComponentService componentService;
	
	@Inject
	public GetScoreUserListServlet(Provider<RequestHead> requestHeadProvider,ComponentService componentService){
		this.requestHeadProvider = requestHeadProvider;
		this.componentService = componentService;
	}
	
	@Override
	public void doPost(HttpServletRequest httpRequest, HttpServletResponse httpResponse) throws ServletException, IOException {
		final int scoreId = ParamUtil.getInt(httpRequest, "score_id", 0);
		final int size = ParamUtil.getInt(httpRequest, "size", -1);
		// 获取上一页最后一条 OffsetIndex 
		byte[] bs = null;
		try {
			bs = Base64.getUrlDecoder().decode(ParamUtil.getString(httpRequest, "offset_index", ""));
		} catch (IllegalArgumentException e) {
			bs = null;
		}
		final ByteString offsetIndex = bs == null ? null : ByteString.copyFrom(bs);
		
		// 获取requestHead
		final RequestHead requestHead = requestHeadProvider.get();
		
		GetScoreUserListRequest.Builder requestBuilder = GetScoreUserListRequest.newBuilder();
		requestBuilder.setScoreId(scoreId);
		requestBuilder.setSize(size);
		// 判断上一页的最后一条是否存在，若存在则赋值
		if(offsetIndex != null){
			requestBuilder.setOffsetIndex(offsetIndex);
		}
		GetScoreUserListResponse response = Futures.getUnchecked(componentService.getScoreUserList(requestHead, requestBuilder.build()));
		
		SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		
		JsonArray scoreArray = new JsonArray();
		// 获取scoreUserList
		for(ComponentProtos.ScoreUser scoreUser : response.getScoreUserList()){
			JsonObject obj = new JsonObject();
			obj.addProperty("user_id", scoreUser.getUserId());
			obj.addProperty("score_value", scoreUser.getScoreValue());
			obj.addProperty("score_time", df.format(new Date(scoreUser.getScoreTime() * 1000L)));
			scoreArray.add(obj);
		}
		
		JsonObject result = new JsonObject();
		result.add("scoreUser_list", scoreArray);
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
