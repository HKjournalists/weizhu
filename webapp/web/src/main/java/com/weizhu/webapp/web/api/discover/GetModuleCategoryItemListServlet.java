package com.weizhu.webapp.web.api.discover;

import java.io.IOException;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.common.util.concurrent.Futures;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.google.protobuf.ByteString;
import com.weizhu.common.utils.HexUtil;
import com.weizhu.common.utils.JsonUtil;
import com.weizhu.proto.*;
import com.weizhu.proto.DiscoverV2Protos.GetModuleCategoryItemListRequest;
import com.weizhu.proto.DiscoverV2Protos.GetModuleCategoryItemListResponse;
import com.weizhu.proto.WeizhuProtos.RequestHead;
import com.weizhu.web.ParamUtil;

@Singleton
public class GetModuleCategoryItemListServlet extends HttpServlet {

	private static final long serialVersionUID = 1L;

	private final Provider<RequestHead> requestHeadProvider;
	private final DiscoverV2Service discoverV2Service;
	private final UserService userService;
	private final UploadService uploadService;

	@Inject
	public GetModuleCategoryItemListServlet(Provider<RequestHead> requestHeadProvider, DiscoverV2Service discoverV2Service,
											UserService userService, UploadService uploadService) {
		this.requestHeadProvider = requestHeadProvider;
		this.discoverV2Service = discoverV2Service;
		this.userService = userService;
		this.uploadService = uploadService;
	}
	
	@Override
	public void doPost(HttpServletRequest httpRequest, HttpServletResponse httpResponse) throws IOException {
		final int moduleId = ParamUtil.getInt(httpRequest, "module_id", 0);
		final int categoryId = ParamUtil.getInt(httpRequest, "category_id", 0);
		final int itemSize = ParamUtil.getInt(httpRequest, "item_size", 12);
		
		GetModuleCategoryItemListRequest.Builder requestBuilder = GetModuleCategoryItemListRequest.newBuilder()
				.setModuleId(moduleId)
				.setCategoryId(categoryId)
				.setItemSize(itemSize);
		
		final String offsetIndex = ParamUtil.getString(httpRequest, "offset_index", null);
		if (offsetIndex != null && !offsetIndex.equals("0")) {
			requestBuilder.setOffsetIndex(ByteString.copyFrom(HexUtil.hex2bin(offsetIndex)));
		}
		
		final RequestHead head = requestHeadProvider.get();
		
		GetModuleCategoryItemListResponse response = Futures.getUnchecked(discoverV2Service.getModuleCategoryItemList(head, requestBuilder.build()));

		Set<Long> userIdSet = Sets.newTreeSet();
		for (DiscoverV2Protos.Item item : response.getItemList()) {
			if (item.hasUser()) {
				userIdSet.add(item.getUser().getUserId());
			}
		}

		UserProtos.GetUserResponse getUserResponse = Futures.getUnchecked(userService.getUserById(head, UserProtos.GetUserByIdRequest.newBuilder()
				.addAllUserId(userIdSet)
				.build()));
		Map<Long, UserProtos.User> userMap = Maps.newTreeMap();
		for (UserProtos.User user : getUserResponse.getUserList()) {
			userMap.put(user.getBase().getUserId(), user);
		}

		Map<Integer, UserProtos.Team> teamMap = Maps.newTreeMap();
		for (int i = 0; i < getUserResponse.getRefTeamCount(); ++i) {
			UserProtos.Team team = getUserResponse.getRefTeam(i);
			teamMap.put(team.getTeamId(), team);
		}

		Map<Integer, UserProtos.Position> positionMap = Maps.newTreeMap();
		for (int i = 0; i < getUserResponse.getRefPositionCount(); ++i) {
			UserProtos.Position position = getUserResponse
					.getRefPosition(i);
			positionMap.put(position.getPositionId(), position);
		}

		UploadProtos.GetUploadUrlPrefixResponse getUploadPrefixResponse = Futures.getUnchecked(uploadService.getUploadUrlPrefix(head, WeizhuProtos.EmptyRequest.getDefaultInstance()));
		String imageUrlPrefix = getUploadPrefixResponse.getImageUrlPrefix();

		JsonArray itemArray = new JsonArray();
		for (DiscoverV2Protos.Item item : response.getItemList()) {
			itemArray.add(DiscoverUtil.itemJson(item, userMap, teamMap, positionMap, imageUrlPrefix));
		}
		
		JsonObject result = new JsonObject();
		result.add("item", itemArray);
		result.addProperty("has_more", response.getHasMore());
		result.addProperty("offset_index", HexUtil.bin2Hex(response.getOffsetIndex().toByteArray()));
		// 模块下分类提示(红点或者数字)索引,当该字段有数据时客户端需保存好。建议保存为 module_id,category_id -> prompt_index 映射形式
		if (response.hasPromptIndex()) {
			result.addProperty("prompt_index", HexUtil.bin2Hex(response.getPromptIndex().toByteArray()));
		}
		
		httpResponse.setContentType("application/json;charset=UTF-8");
		JsonUtil.GSON.toJson(result, httpResponse.getWriter());
	}
	
	@Override
	public void doGet(HttpServletRequest httpRequest, HttpServletResponse httpResponse) throws IOException {
		this.doPost(httpRequest, httpResponse);
	}
}
