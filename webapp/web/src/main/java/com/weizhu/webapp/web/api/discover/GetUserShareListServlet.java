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
import com.weizhu.proto.DiscoverV2Protos.GetUserShareListRequest;
import com.weizhu.proto.DiscoverV2Protos.GetUserShareListResponse;
import com.weizhu.proto.WeizhuProtos.RequestHead;
import com.weizhu.web.ParamUtil;

@Singleton
public class GetUserShareListServlet extends HttpServlet {

	private static final long serialVersionUID = 1L;

	private final Provider<RequestHead> requestHeadProvider;
	private final DiscoverV2Service discoverV2Service;
	private final UserService userService;
	private final UploadService uploadService;
	
	@Inject
	public GetUserShareListServlet(Provider<RequestHead> requestHeadProvider, DiscoverV2Service discoverV2Service,
								   UserService userService, UploadService uploadService) {
		this.requestHeadProvider = requestHeadProvider;
		this.discoverV2Service = discoverV2Service;
		this.userService = userService;
		this.uploadService = uploadService;
	}
	
	@Override
	public void doPost(HttpServletRequest httpRequest, HttpServletResponse httpResponse) throws IOException {
		final RequestHead head = requestHeadProvider.get();

		final long userId = ParamUtil.getLong(httpRequest, "user_id", head.getSession().getUserId());
		final int size = ParamUtil.getInt(httpRequest, "size", 12);
		
		GetUserShareListRequest.Builder requestBuilder = GetUserShareListRequest.newBuilder()
				.setUserId(userId)
				.setSize(size);
		
		final String offsetIndex = ParamUtil.getString(httpRequest, "offset_index", null);
		if (offsetIndex != null && !offsetIndex.equals("0")) {
			requestBuilder.setOffsetIndex(ByteString.copyFrom(HexUtil.hex2bin(offsetIndex)));
		}
		
		GetUserShareListResponse response = Futures.getUnchecked(discoverV2Service.getUserShareList(head, requestBuilder.build()));

		Set<Long> userIdSet = Sets.newTreeSet();
		for (DiscoverV2Protos.ItemShare itemShare : response.getItemShareList()) {
			userIdSet.add(itemShare.getUserId());
		}
		for (DiscoverV2Protos.Item item : response.getRefItemList()) {
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
		
		JsonArray itemShareArray = new JsonArray();
		for (DiscoverV2Protos.ItemShare itemShare : response.getItemShareList()) {
			itemShareArray.add(DiscoverUtil.itemShareJson(itemShare, userMap, teamMap, positionMap, imageUrlPrefix));
		}
		JsonArray itemArray = new JsonArray();
		for (DiscoverV2Protos.Item item : response.getRefItemList()) {
			itemArray.add(DiscoverUtil.itemJson(item, userMap, teamMap, positionMap, imageUrlPrefix));
		}
		
		JsonObject result = new JsonObject();
		result.add("item_share", itemShareArray);
		result.addProperty("has_more", response.getHasMore());
		result.addProperty("offset_index", HexUtil.bin2Hex(response.getOffsetIndex().toByteArray()));
		result.add("ref_item", itemArray);
		
		httpResponse.setContentType("application/json;charset=UTF-8");
		JsonUtil.GSON.toJson(result, httpResponse.getWriter());
	}
	
	@Override
	public void doGet(HttpServletRequest httpRequest, HttpServletResponse httpResponse) throws IOException {
		this.doPost(httpRequest, httpResponse);
	}
}
