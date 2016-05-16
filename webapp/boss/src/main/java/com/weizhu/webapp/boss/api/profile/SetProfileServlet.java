package com.weizhu.webapp.boss.api.profile;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.common.base.Strings;
import com.google.common.util.concurrent.Futures;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.weizhu.common.utils.JsonUtil;
import com.weizhu.proto.ProfileProtos;
import com.weizhu.proto.ProfileService;
import com.weizhu.proto.BossProtos.BossHead;
import com.weizhu.proto.ProfileProtos.SetProfileRequest;
import com.weizhu.proto.ProfileProtos.SetProfileResponse;
import com.weizhu.web.ParamUtil;

@Singleton
@SuppressWarnings("serial")
public class SetProfileServlet extends HttpServlet {

	private final Provider<BossHead> bossHeadProvider;
	private final ProfileService profileService;
	
	@Inject
	public SetProfileServlet(Provider<BossHead> bossHeadProvider, ProfileService profileService) {
		this.bossHeadProvider = bossHeadProvider;
		this.profileService = profileService;
	}
	
	@Override
	protected void doGet(HttpServletRequest httpRequest, HttpServletResponse httpResponse) throws ServletException, IOException {
		this.doPost(httpRequest, httpResponse);
	}
	
	@Override
	protected void doPost(HttpServletRequest httpRequest, HttpServletResponse httpResponse) throws ServletException, IOException {
		// 1. 取出参数
		Long companyId = ParamUtil.getLong(httpRequest, "company_id", null);
		String profileJsonStr = ParamUtil.getString(httpRequest, "profile_json", null);
		if (Strings.isNullOrEmpty(profileJsonStr)) {
			JsonObject resultObj = new JsonObject();
			resultObj.addProperty("result", "FAIL_PROFILE_INVALID");
			resultObj.addProperty("fail_text", "profile参数为空");
			
			httpResponse.setContentType("application/json;charset=UTF-8");
			JsonUtil.GSON.toJson(resultObj, httpResponse.getWriter());
			return;
		}
		
		// 2. 调用Service
		BossHead.Builder headBuilder = this.bossHeadProvider.get().toBuilder();
		if (companyId == null) {
			headBuilder.clearCompanyId();
		} else {
			headBuilder.setCompanyId(companyId);
		}
		final BossHead head = headBuilder.build();
		
		SetProfileRequest.Builder requestBuilder = SetProfileRequest.newBuilder();
		
		JsonArray profileJson = JsonUtil.JSON_PARSER.parse(profileJsonStr).getAsJsonArray();
		ProfileProtos.Profile.Builder profileBuilder = ProfileProtos.Profile.newBuilder();
		for (int i=0; i<profileJson.size(); ++i) {
			profileBuilder.clear();
			
			JsonObject profileObj = profileJson.get(i).getAsJsonObject();
			profileBuilder.setName(profileObj.get("name").getAsString());
			profileBuilder.setValue(profileObj.get("value").getAsString());
			if (profileObj.has("comment")) {
				profileBuilder.setComment(profileObj.get("comment").getAsString());
			}
			requestBuilder.addProfile(profileBuilder.build());
		}
		
		SetProfileResponse response = Futures.getUnchecked(this.profileService.setProfile(head, requestBuilder.build()));
		
		httpResponse.setContentType("application/json;charset=UTF-8");
		JsonUtil.PROTOBUF_JSON_FORMAT.print(response, httpResponse.getWriter());
	}
}
