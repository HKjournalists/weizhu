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
import com.weizhu.common.utils.JsonUtil;
import com.weizhu.proto.*;
import com.weizhu.proto.DiscoverV2Protos.GetDiscoverHomeRequest;
import com.weizhu.proto.DiscoverV2Protos.GetDiscoverHomeResponse;
import com.weizhu.proto.DiscoverV2Protos.GetItemListRequest;
import com.weizhu.proto.DiscoverV2Protos.GetItemListResponse;
import com.weizhu.proto.UploadProtos.GetUploadUrlPrefixResponse;
import com.weizhu.proto.WeizhuProtos.EmptyRequest;
import com.weizhu.proto.WeizhuProtos.RequestHead;
import com.weizhu.proto.WeizhuProtos.SystemHead;
import com.weizhu.web.ParamUtil;

@Singleton
public class GetDiscoverHomeServlet extends HttpServlet {

	private static final long serialVersionUID = 1L;

	private final Provider<RequestHead> requestHeadProvider;
	private final DiscoverV2Service discoverV2Service;
	private final UploadService uploadService;
	private final UserService userService;
	
	@Inject
	public GetDiscoverHomeServlet(Provider<RequestHead> requestHeadProvider, DiscoverV2Service discoverV2Service,
								  UploadService uploadService,
								  UserService userService) {
		this.requestHeadProvider = requestHeadProvider;
		this.discoverV2Service = discoverV2Service;
		this.uploadService = uploadService;
		this.userService = userService;
	}
	
	@Override
	public void doPost(HttpServletRequest httpRequest, HttpServletResponse httpResponse) throws IOException {
		final int itemSize = ParamUtil.getInt(httpRequest, "item_size", 12);
		
		final RequestHead head = requestHeadProvider.get();
		
		GetDiscoverHomeResponse response = Futures.getUnchecked(discoverV2Service.getDiscoverHome(head, GetDiscoverHomeRequest.getDefaultInstance()));
		
		GetUploadUrlPrefixResponse getUploadPrefixResponse = Futures.getUnchecked(uploadService.getUploadUrlPrefix(head, EmptyRequest.getDefaultInstance()));
		String imageUrlPrefix = getUploadPrefixResponse.getImageUrlPrefix();
		
		GetItemListResponse getItemListResponse = Futures.getUnchecked(discoverV2Service.getItemList(
				SystemHead.newBuilder()
					.setCompanyId(head.getSession().getCompanyId())
					.build(),
				GetItemListRequest.newBuilder()
				.setItemSize(itemSize)
				.build()));
		
		JsonArray bannerArray = new JsonArray();
		for (DiscoverV2Protos.Banner banner : response.getBannerList()) {
			JsonObject obj = new JsonObject();
			obj.addProperty("banner_id", banner.getBannerId());
			obj.addProperty("banner_name", banner.getBannerName());
			obj.addProperty("image_name", banner.getImageName());
			obj.addProperty("image_url", imageUrlPrefix + banner.getImageName());
			// 轮播图的内容
			switch (banner.getContentCase()) {
				case ITEM_ID :
					obj.addProperty("item_id", banner.getItemId());
					break;
				case WEB_URL :
					DiscoverV2Protos.WebUrl webUrl = banner.getWebUrl();
					obj.addProperty("web_url", webUrl.getWebUrl());
					obj.addProperty("is_weizhu", webUrl.getIsWeizhu());
					break;
				case APP_URI :
					DiscoverV2Protos.AppUri appUri = banner.getAppUri();
					obj.addProperty("app_uri", appUri.getAppUri());
					break;
				default:
					break;
			}
			bannerArray.add(obj);
		}
		JsonArray moduleArray = new JsonArray();
		for (DiscoverV2Protos.Module module : response.getModuleList()) {
			JsonObject moduleJson = new JsonObject();
			moduleJson.addProperty("module_id", module.getModuleId());
			moduleJson.addProperty("module_name", module.getModuleName());
			moduleJson.addProperty("image_name", module.getImageName());
			moduleJson.addProperty("image_url", imageUrlPrefix + module.getImageName());
			// 提示红点。true: 显示红点。 false或者没有设置：不显示
			if (module.hasPromptDot()) {
				moduleJson.addProperty("prompt_dot", module.getPromptDot());
			}
			// 提示计数。prompt_cnt>0: 提示具体数字。prompt_cnt<=0或者没有设置: 不显示
			if (module.hasPromptCnt()) {
				moduleJson.addProperty("prompt_cnt", module.getPromptCnt());
			}
			switch (module.getContentCase()) {
				case CATEGORY_LIST :
					JsonArray categoryArray = new JsonArray();
					for (DiscoverV2Protos.Module.Category category : module.getCategoryList().getCategoryList()) {
						JsonObject categoryJson = new JsonObject();
						categoryJson.addProperty("category_id", category.getCategoryId());
						categoryJson.addProperty("category_name", category.getCategoryName());
						categoryJson.addProperty("module_id", category.getModuleId());
						// 提示红点。true: 显示红点。 false或者没有设置：不显示
						if (category.hasPromptDot()) {
							categoryJson.addProperty("prompt_dot", category.getPromptDot());
						}
						// 提示计数。prompt_cnt>0: 提示具体数字。prompt_cnt<=0或者没有设置: 不显示
						if (category.hasPromptCnt()) {
							categoryJson.addProperty("prompt_cnt", category.getPromptCnt());
						}
						
						categoryArray.add(categoryJson);
					}
					
					moduleJson.add("category_list", categoryArray);
					break;
				case WEB_URL :
					JsonObject webJson = new JsonObject();
					DiscoverV2Protos.WebUrl webUrl = module.getWebUrl();
					webJson.addProperty("web_url", webUrl.getWebUrl());
					webJson.addProperty("is_weizhu", webUrl.getIsWeizhu());
					
					moduleJson.add("web_url", webJson);
					break;
				case APP_URI :
					JsonObject appJson = new JsonObject();
					DiscoverV2Protos.AppUri appUri = module.getAppUri();
					appJson.addProperty("app_uri", appUri.getAppUri());
					
					moduleJson.add("app_uri", appJson);
					break;
				default:
					break;
			}
			
			moduleArray.add(moduleJson);
		}

		Set<Long> userIdSet = Sets.newTreeSet();
		for (DiscoverV2Protos.Item item : getItemListResponse.getItemList()) {
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

		JsonArray itemArray = new JsonArray();
		for (DiscoverV2Protos.Item item : getItemListResponse.getItemList()) {
			if (item.getBase().hasState() && item.getBase().getState().equals(DiscoverV2Protos.State.NORMAL)) {
				itemArray.add(DiscoverUtil.itemJson(item, userMap, teamMap, positionMap, imageUrlPrefix));
			}
		}
		
		JsonObject result = new JsonObject();
		result.add("banner", bannerArray);
		result.add("module", moduleArray);
		result.add("item", itemArray);
		
		httpResponse.setContentType("application/json;charset=UTF-8");
		JsonUtil.GSON.toJson(result, httpResponse.getWriter());
	}
	
	@Override
	public void doGet(HttpServletRequest httpRequest, HttpServletResponse httpResponse) throws IOException {
		this.doPost(httpRequest, httpResponse);
	}
}
