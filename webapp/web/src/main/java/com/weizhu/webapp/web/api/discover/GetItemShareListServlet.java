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
import com.weizhu.proto.DiscoverV2Protos.GetItemShareListRequest;
import com.weizhu.proto.DiscoverV2Protos.GetItemShareListResponse;
import com.weizhu.proto.WeizhuProtos.RequestHead;
import com.weizhu.web.ParamUtil;

@Singleton
public class GetItemShareListServlet extends HttpServlet {

	private static final long serialVersionUID = 1L;

	private final Provider<RequestHead> requestHeadProvider;
	private final DiscoverV2Service discoverV2Service;
	private final UserService userService;
	private final UploadService uploadService;
	
	@Inject
	public GetItemShareListServlet(Provider<RequestHead> requestHeadProvider, DiscoverV2Service discoverV2Service,
								   UserService userService, UploadService uploadService) {
		this.requestHeadProvider = requestHeadProvider;
		this.discoverV2Service = discoverV2Service;
		this.userService = userService;
		this.uploadService = uploadService;
	}
	
	@Override
	public void doPost(HttpServletRequest httpRequest, HttpServletResponse httpResponse) throws IOException {
		final long itemId = ParamUtil.getLong(httpRequest, "item_id", 0L);
		final int size = ParamUtil.getInt(httpRequest, "size", 12);
		
		GetItemShareListRequest.Builder requestBuilder = GetItemShareListRequest.newBuilder()
				.setItemId(itemId)
				.setSize(size);
		
		final String offsetIndex = ParamUtil.getString(httpRequest, "offset_index", null);
		if (offsetIndex != null && !offsetIndex.equals("0")) {
			requestBuilder.setOffsetIndex(ByteString.copyFrom(HexUtil.hex2bin(offsetIndex)));
		}
		
		final RequestHead head = requestHeadProvider.get();
		
		GetItemShareListResponse response = Futures.getUnchecked(discoverV2Service.getItemShareList(head, requestBuilder.build()));

		Set<Long> userIdSet = Sets.newTreeSet();
		for (DiscoverV2Protos.ItemShare itemShare : response.getItemShareList()) {
			userIdSet.add(itemShare.getUserId());
		}
		if (response.hasUserItemShare()) {
			userIdSet.add(response.getUserItemShare().getUserId());
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

		JsonArray itemShareArray = new JsonArray();
		for (DiscoverV2Protos.ItemShare itemShare : response.getItemShareList()) {
			itemShareArray.add(DiscoverUtil.itemShareJson(itemShare, userMap, teamMap, positionMap, imageUrlPrefix));
		}
		
		JsonObject result = new JsonObject();
		result.add("item_score", itemShareArray);
		result.addProperty("has_more", response.getHasMore());
		result.addProperty("offset_index", HexUtil.bin2Hex(response.getOffsetIndex().toByteArray()));
		if (response.hasItemShareCnt()) {
			result.addProperty("item_share_cnt", response.getItemShareCnt());
		}
		// 访问用户的分享情况
		if (response.hasUserItemShare()) {
			result.add("user_item_share", DiscoverUtil.itemShareJson(response.getUserItemShare(), userMap, teamMap, positionMap, imageUrlPrefix));
		}
		
		httpResponse.setContentType("application/json;charset=UTF-8");
		JsonUtil.GSON.toJson(result, httpResponse.getWriter());
	}
	
	@Override
	public void doGet(HttpServletRequest httpRequest, HttpServletResponse httpResponse) throws IOException {
		this.doPost(httpRequest, httpResponse);
	}
}
