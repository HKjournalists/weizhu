package com.weizhu.webapp.boss.api.profile;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.common.util.concurrent.Futures;
import com.google.gson.JsonArray;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.weizhu.common.utils.JsonUtil;
import com.weizhu.proto.ProfileProtos;
import com.weizhu.proto.ProfileService;
import com.weizhu.proto.BossProtos.BossHead;
import com.weizhu.proto.ProfileProtos.GetProfileRequest;
import com.weizhu.proto.ProfileProtos.GetProfileResponse;
import com.weizhu.web.ParamUtil;

@Singleton
@SuppressWarnings("serial")
public class GetProfileServlet extends HttpServlet {

	private final Provider<BossHead> bossHeadProvider;
	private final ProfileService profileService;
	
	@Inject
	public GetProfileServlet(Provider<BossHead> bossHeadProvider, ProfileService profileService) {
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
		String namePrefix = ParamUtil.getString(httpRequest, "name_prefix", "");
		
		if (companyId == null) {
			JsonObject tmp = new JsonObject();
			tmp.addProperty("result", "SUCC");
			tmp.add("profile", new JsonArray());
			
			httpResponse.setContentType("application/json;charset=UTF-8");
			JsonUtil.GSON.toJson(tmp, httpResponse.getWriter());
			return;
		}
		
		
		// 2. 调用Service
		
		final BossHead head = this.bossHeadProvider.get().toBuilder().setCompanyId(companyId).build();
		
		GetProfileRequest request = GetProfileRequest.newBuilder()
				.addNamePrefix(namePrefix)
				.build();
		
		GetProfileResponse response = Futures.getUnchecked(this.profileService.getProfile(head, request));
		
		JsonObject resultObj = new JsonObject();
		resultObj.addProperty("result", "SUCC");
		
		JsonArray array = new JsonArray();
		for (ProfileProtos.Profile profile : response.getProfileList()) {
			JsonObject p = new JsonObject();
			p.addProperty("name", profile.getName());
			p.addProperty("value", profile.getValue());
			if (profile.hasComment()) {
				p.addProperty("comment", profile.getComment());
			} else {
				p.add("comment", JsonNull.INSTANCE);
			}
			
			array.add(p);
		}
		resultObj.add("profile", array);
		
		httpResponse.setContentType("application/json;charset=UTF-8");
		JsonUtil.GSON.toJson(resultObj, httpResponse.getWriter());
	}
}
