package com.weizhu.webapp.web.api.discover;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
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
import com.weizhu.proto.*;
import com.weizhu.proto.DiscoverV2Protos.GetItemByIdRequest;
import com.weizhu.proto.DiscoverV2Protos.GetItemByIdResponse;
import com.weizhu.common.utils.JsonUtil;
import com.weizhu.proto.WeizhuProtos.RequestHead;
import com.weizhu.web.ParamUtil;

@Singleton
public class GetItemByIdServlet extends HttpServlet {

	private static final long serialVersionUID = 1L;

	private final Provider<RequestHead> requestHeadProvider;
	private final DiscoverV2Service discoverV2Service;
	private final UploadService uploadService;
	private final UserService userService;
	
	@Inject
	public GetItemByIdServlet(Provider<RequestHead> requestHeadProvider, DiscoverV2Service discoverV2Service,
							  UploadService uploadService,
							  UserService userService) {
		this.requestHeadProvider = requestHeadProvider;
		this.discoverV2Service = discoverV2Service;
		this.uploadService = uploadService;
		this.userService = userService;
	}
	
	@Override
	public void doPost(HttpServletRequest httpRequest, HttpServletResponse httpResponse) throws IOException {
		final List<Long> itemIdList = ParamUtil.getLongList(httpRequest, "item_id", Collections.emptyList());
		
		final RequestHead head = requestHeadProvider.get();
		
		GetItemByIdResponse response = Futures.getUnchecked(discoverV2Service.getItemById(head, GetItemByIdRequest.newBuilder()
				.addAllItemId(itemIdList)
				.build()));

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
			if (item.getBase().hasState() && item.getBase().getState().equals(DiscoverV2Protos.State.NORMAL)) {
				itemArray.add(DiscoverUtil.itemJson(item, userMap, teamMap, positionMap, imageUrlPrefix));
			}
		}
		
		JsonObject result = new JsonObject();
		result.add("item", itemArray);
		
		httpResponse.setContentType("application/json;charset=UTF-8");
		JsonUtil.GSON.toJson(result, httpResponse.getWriter());
	}
	
	@Override
	public void doGet(HttpServletRequest httpRequest, HttpServletResponse httpResponse) throws IOException {
		this.doPost(httpRequest, httpResponse);
	}
}
