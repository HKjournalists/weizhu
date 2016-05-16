package com.weizhu.webapp.admin.api.discover;

import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

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
import com.weizhu.common.service.ServiceUtil;
import com.weizhu.common.utils.JsonUtil;
import com.weizhu.proto.AdminDiscoverProtos;
import com.weizhu.proto.AdminDiscoverService;
import com.weizhu.proto.AdminProtos;
import com.weizhu.proto.AdminService;
import com.weizhu.proto.AllowProtos;
import com.weizhu.proto.AllowService;
import com.weizhu.proto.DiscoverV2Protos;
import com.weizhu.proto.UploadService;
import com.weizhu.proto.AdminProtos.AdminHead;

@Singleton
@SuppressWarnings("serial")
public class GetDiscoverBannerServlet extends HttpServlet {
	
	private final Provider<AdminHead> adminHeadProvider;
	private final AdminDiscoverService adminDiscoverService;
	private final AdminService adminService;
	private final UploadService uploadService;
	private final AllowService allowService;

	@Inject
	public GetDiscoverBannerServlet(Provider<AdminHead> adminHeadProvider, 
			AdminDiscoverService adminDiscoverService, 
			AdminService adminService,
			UploadService uploadService, 
			AllowService allowService
			) {
		this.adminHeadProvider = adminHeadProvider;
		this.adminDiscoverService = adminDiscoverService;
		this.adminService = adminService;
		this.uploadService = uploadService;
		this.allowService = allowService;
	}

	@Override
	protected void doGet(HttpServletRequest httpRequest, HttpServletResponse httpResponse) throws ServletException, IOException {
		this.doPost(httpRequest, httpResponse);
	}

	@Override
	protected void doPost(HttpServletRequest httpRequest, HttpServletResponse httpResponse) throws ServletException, IOException {
		final AdminHead head = this.adminHeadProvider.get();

		AdminDiscoverProtos.GetBannerResponse response = Futures.getUnchecked(this.adminDiscoverService.getBanner(head, ServiceUtil.EMPTY_REQUEST));

		// 获取user信息,和allowModel信息
		Set<Long> adminIdSet = new TreeSet<Long>();
		Set<Integer> allowModelIdSet = new TreeSet<Integer>();
		for (DiscoverV2Protos.Banner banner : response.getBannerList()) {
			if (banner.hasCreateAdminId()) {
				adminIdSet.add(banner.getCreateAdminId());
			}
			if (banner.hasUpdateAdminId()) {
				adminIdSet.add(banner.getUpdateAdminId());
			}
			if (banner.hasAllowModelId()) {
				allowModelIdSet.add(banner.getAllowModelId());
			}
		}
		
		final Map<Long, AdminProtos.Admin> refAdminMap = DiscoverServletUtil.getAdminMap(adminService, head, adminIdSet);
		final Map<Integer, AllowProtos.Model> refAllowModelMap = DiscoverServletUtil.getAllowModelMap(allowService, head, allowModelIdSet);
		final String imageUrlPrefix = Futures.getUnchecked(this.uploadService.getUploadUrlPrefix(head, ServiceUtil.EMPTY_REQUEST)).getImageUrlPrefix();

		JsonObject resultObj = new JsonObject();
		JsonArray bannerArray = new JsonArray();
		for (DiscoverV2Protos.Banner banner : response.getBannerList()) {
			JsonObject bannerObj = new JsonObject();
			bannerObj.addProperty("banner_id", banner.getBannerId());
			bannerObj.addProperty("banner_name", banner.getBannerName());
			bannerObj.addProperty("image_name", banner.getImageName());
			bannerObj.addProperty("image_url", imageUrlPrefix + banner.getImageName());
			if (banner.hasAllowModelId()) {
				bannerObj.addProperty("allow_model_id", banner.getAllowModelId());
				bannerObj.addProperty("allow_model_name", DiscoverServletUtil.getAllowModelName(refAllowModelMap, banner.getAllowModelId()));
			} else {
				bannerObj.addProperty("allow_model_id", "");
				bannerObj.addProperty("allow_model_name", "");
			}
			bannerObj.addProperty("item_id", banner.getItemId());
			bannerObj.addProperty("item_url", imageUrlPrefix + banner.getItemId());
			bannerObj.addProperty("web_url", JsonUtil.PROTOBUF_JSON_FORMAT.printToString(banner.getWebUrl()));
			bannerObj.addProperty("app_uri", JsonUtil.PROTOBUF_JSON_FORMAT.printToString(banner.getAppUri()));
			bannerObj.addProperty("state", banner.getState().name());
			
			bannerObj.addProperty("create_admin_id", banner.getCreateAdminId());
			bannerObj.addProperty("create_admin_name", DiscoverServletUtil.getAdminName(refAdminMap, banner.hasCreateAdminId(), banner.getCreateAdminId()));
			bannerObj.addProperty("create_time", DiscoverServletUtil.getDateStr(banner.hasCreateTime(), banner.getCreateTime()));
			bannerObj.addProperty("update_admin_id", banner.getUpdateAdminId());
			bannerObj.addProperty("update_admin_name", DiscoverServletUtil.getAdminName(refAdminMap, banner.hasUpdateAdminId(), banner.getUpdateAdminId()));
			bannerObj.addProperty("update_time", DiscoverServletUtil.getDateStr(banner.hasUpdateTime(), banner.getUpdateTime()));

			bannerArray.add(bannerObj);
		}
		resultObj.add("banner", bannerArray);
		httpResponse.setContentType("application/json;charset=UTF-8");
		JsonUtil.GSON.toJson(resultObj, httpResponse.getWriter());
	}

}
