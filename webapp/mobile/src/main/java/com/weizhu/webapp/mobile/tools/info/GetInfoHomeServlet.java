package com.weizhu.webapp.mobile.tools.info;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

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
import com.weizhu.common.utils.HexUtil;
import com.weizhu.common.utils.JsonUtil;
import com.weizhu.proto.ToolsService;
import com.weizhu.proto.ToolsProtos;
import com.weizhu.proto.ToolsProtos.GetInfoHomeRequest;
import com.weizhu.proto.ToolsProtos.GetInfoHomeResponse;
import com.weizhu.proto.WeizhuProtos.RequestHead;
import com.weizhu.web.ParamUtil;

@Singleton
@SuppressWarnings("serial")
public class GetInfoHomeServlet extends HttpServlet {

	private final Provider<RequestHead> requestHeadProvider;
	private final ToolsService toolsService;
	
	@Inject
	public GetInfoHomeServlet(Provider<RequestHead> requestHeadProvider, ToolsService toolsService) {
		this.requestHeadProvider = requestHeadProvider;
		this.toolsService = toolsService;
	}
	
	@Override
	protected void doGet(HttpServletRequest httpRequest, HttpServletResponse httpResponse) throws ServletException, IOException {
		this.doPost(httpRequest, httpResponse);
	}
	
	@Override
	protected void doPost(HttpServletRequest httpRequest, HttpServletResponse httpResponse) throws ServletException, IOException {
		int toolId = ParamUtil.getInt(httpRequest, "tool_id", 0);
		List<Integer> selectedTagIdList = ParamUtil.getIntList(httpRequest, "selected_tag_id", Collections.<Integer>emptyList());
		int itemSize = ParamUtil.getInt(httpRequest, "item_size", 10);
		String offsetIndexHexStr = ParamUtil.getString(httpRequest, "offset_index", null);
		
		ByteString offsetIndex = null;
		if (offsetIndexHexStr != null) {
			try {
				offsetIndex = ByteString.copyFrom(HexUtil.hex2bin(offsetIndexHexStr));
			} catch (Exception e) {
				// ignore
			}
		}
		
		final RequestHead head = this.requestHeadProvider.get();
		
		GetInfoHomeRequest.Builder requestBuilder = GetInfoHomeRequest.newBuilder();
		requestBuilder.setToolId(toolId);
		requestBuilder.addAllSelectedTagId(selectedTagIdList);
		requestBuilder.setItemSize(itemSize);
		if (offsetIndex != null) {
			requestBuilder.setOffsetIndex(offsetIndex);
		}
		
		GetInfoHomeResponse response = Futures.getUnchecked(this.toolsService.getInfoHome(head, requestBuilder.build()));
		
		Map<Integer, ToolsProtos.InfoTag> refTagMap = new TreeMap<Integer, ToolsProtos.InfoTag>();
		for (ToolsProtos.InfoTag tag : response.getRefTagList()) {
			refTagMap.put(tag.getTagId(), tag);
		}
		Map<Integer, ToolsProtos.InfoDimension> refDimensionMap = new TreeMap<Integer, ToolsProtos.InfoDimension>();
		for (ToolsProtos.InfoDimension dimension : response.getRefDimensionList()) {
			refDimensionMap.put(dimension.getDimensionId(), dimension);
		}

		JsonArray hotTagArray = new JsonArray();
		for (int hotTagId : response.getHotTagIdList()) {
			ToolsProtos.InfoTag tag = refTagMap.get(hotTagId);
			if (tag == null) {
				continue;
			}
			ToolsProtos.InfoDimension dimension = refDimensionMap.get(tag.getDimensionId());
			if (dimension == null) {
				continue;
			}
			
			JsonObject tagObj = new JsonObject();
			
			tagObj.addProperty("tag_id", tag.getTagId());
			tagObj.addProperty("tag_name", tag.getTagName());
			tagObj.addProperty("item_size", tag.getItemSize());
			tagObj.add("data", JsonUtil.JSON_PARSER.parse(tag.getDataJson()));
			
			JsonObject dimensionObj = new JsonObject();
			dimensionObj.addProperty("dimension_id", dimension.getDimensionId());
			dimensionObj.addProperty("dimension_name", dimension.getDimensionName());
			dimensionObj.add("data", JsonUtil.JSON_PARSER.parse(dimension.getDataJson()));
			
			tagObj.add("dimension", dimensionObj);
			
			hotTagArray.add(tagObj);
		}
		
		JsonArray dimensionArray = new JsonArray();
		for (ToolsProtos.InfoDimension dimension : refDimensionMap.values()) {
			JsonObject dimensionObj = new JsonObject();
			dimensionObj.addProperty("dimension_id", dimension.getDimensionId());
			dimensionObj.addProperty("dimension_name", dimension.getDimensionName());
			dimensionObj.add("data", JsonUtil.JSON_PARSER.parse(dimension.getDataJson()));
			
			JsonArray tagArray = new JsonArray();
			for (int tagId : response.getAllTagIdList()) {
				ToolsProtos.InfoTag tag = refTagMap.get(tagId);
				if (tag == null || tag.getDimensionId() != dimension.getDimensionId()) {
					continue;
				}
				
				JsonObject tagObj = new JsonObject();
				
				tagObj.addProperty("tag_id", tag.getTagId());
				tagObj.addProperty("tag_name", tag.getTagName());
				tagObj.addProperty("item_size", tag.getItemSize());
				tagObj.add("data", JsonUtil.JSON_PARSER.parse(tag.getDataJson()));
				
				tagArray.add(tagObj);
			}
			
			if (tagArray.size() > 0) {
				dimensionObj.add("tag", tagArray);
				dimensionArray.add(dimensionObj);
			}
		}
		
		JsonArray itemArray = new JsonArray();
		for (ToolsProtos.InfoItem item : response.getItemList()) {
			JsonObject itemObj = new JsonObject();
			itemObj.addProperty("item_id", item.getItemId());
			itemObj.addProperty("item_name", item.getItemName());
			itemObj.add("data", JsonUtil.JSON_PARSER.parse(item.getDataJson()));
			
			itemArray.add(itemObj);
		}
		
		JsonObject resultObj = new JsonObject();
		resultObj.add("hot_tag_list", hotTagArray);
		resultObj.add("dimension_list", dimensionArray);
		resultObj.add("item_list", itemArray);
		resultObj.addProperty("has_more", response.getHasMore());
		resultObj.addProperty("offset_index", HexUtil.bin2Hex(response.getOffsetIndex().toByteArray()));
		
		httpResponse.setContentType("application/json;charset=UTF-8");
		JsonUtil.GSON.toJson(resultObj, httpResponse.getWriter());
	}
	
}
