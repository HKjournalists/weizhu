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
import com.weizhu.proto.UploadService;
import com.weizhu.proto.AdminProtos.AdminHead;
import com.weizhu.proto.DiscoverV2Protos;

@Singleton
@SuppressWarnings("serial")
public class GetDiscoverModuleServlet extends HttpServlet {
	
	private final Provider<AdminHead> adminHeadProvider;
	private final AdminDiscoverService adminDiscoverService;
	private final AdminService adminService;
	private final UploadService uploadService;
	private final AllowService allowService;

	@Inject
	public GetDiscoverModuleServlet(Provider<AdminHead> adminHeadProvider, 
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
		
		AdminDiscoverProtos.GetModuleResponse response = Futures.getUnchecked(this.adminDiscoverService.getModule(head, ServiceUtil.EMPTY_REQUEST));

		// 获取admin信息,和allowModel信息
		Set<Long> adminIdSet = new TreeSet<Long>();
		Set<Integer> allowModelIdSet = new TreeSet<Integer>();
		for (DiscoverV2Protos.Module module : response.getModuleList()) {
			if (module.hasCreateAdminId()) {
				adminIdSet.add(module.getCreateAdminId());
			}
			if (module.hasUpdateAdminId()) {
				adminIdSet.add(module.getUpdateAdminId());
			}
			if (module.hasAllowModelId()) {
				allowModelIdSet.add(module.getAllowModelId());
			}
			for (DiscoverV2Protos.Module.Category category : module.getCategoryList().getCategoryList()) {
				if (category.hasCreateAdminId()) {
					adminIdSet.add(category.getCreateAdminId());
				}
				if (category.hasUpdateAdminId()) {
					adminIdSet.add(category.getUpdateAdminId());
				}
				if (category.hasAllowModelId()) {
					allowModelIdSet.add(category.getAllowModelId());
				}
			}
		}
		
		final Map<Long, AdminProtos.Admin> refAdminMap = DiscoverServletUtil.getAdminMap(adminService, head, adminIdSet);
		final Map<Integer, AllowProtos.Model> refAllowModelMap = DiscoverServletUtil.getAllowModelMap(allowService, head, allowModelIdSet);
		final String imageUrlPrefix = Futures.getUnchecked(this.uploadService.getUploadUrlPrefix(head, ServiceUtil.EMPTY_REQUEST)).getImageUrlPrefix();
		
		JsonArray moduleArray = new JsonArray();
		for (DiscoverV2Protos.Module module : response.getModuleList()) {
			JsonObject moduleObj = new JsonObject();
			moduleObj.addProperty("module_id", module.getModuleId());
			moduleObj.addProperty("module_name", module.getModuleId());
			moduleObj.addProperty("module_name", module.getModuleName());
			moduleObj.addProperty("image_name", module.getImageName());
			moduleObj.addProperty("image_url", imageUrlPrefix + module.getImageName());
			if (module.hasAllowModelId()) {
				moduleObj.addProperty("allow_model_id", module.getAllowModelId());
				moduleObj.addProperty("allow_model_name", DiscoverServletUtil.getAllowModelName(refAllowModelMap, module.getAllowModelId()));
			} else {
				moduleObj.addProperty("allow_model_id", "");
				moduleObj.addProperty("allow_model_name", "");
			}
			moduleObj.addProperty("web_url", JsonUtil.PROTOBUF_JSON_FORMAT.printToString(module.getWebUrl()));
			moduleObj.addProperty("app_uri", JsonUtil.PROTOBUF_JSON_FORMAT.printToString(module.getAppUri()));
			
			JsonArray categoryArray = new JsonArray();
			for (DiscoverV2Protos.Module.Category category : module.getCategoryList().getCategoryList()) {
				JsonObject categoryObj = new JsonObject();
				categoryObj.addProperty("category_id", category.getCategoryId());
				categoryObj.addProperty("category_name", category.getCategoryName());
				categoryObj.addProperty("module_id", category.getModuleId());
				if (category.hasAllowModelId()) {
					categoryObj.addProperty("allow_model_id", category.getAllowModelId());
					categoryObj.addProperty("allow_model_name", DiscoverServletUtil.getAllowModelName(refAllowModelMap, category.getAllowModelId()));
				} else {
					categoryObj.addProperty("allow_model_id", "");
					categoryObj.addProperty("allow_model_name", "");
				}
				categoryObj.addProperty("state", category.getState().name());
				categoryObj.addProperty("create_admin_id", category.getCreateAdminId());
				categoryObj.addProperty("create_admin_name", DiscoverServletUtil.getAdminName(refAdminMap, category.hasCreateAdminId(), category.getCreateAdminId()));
				categoryObj.addProperty("create_time", DiscoverServletUtil.getDateStr(category.hasCreateTime(), category.getCreateTime()));
				categoryObj.addProperty("update_admin_id", category.getUpdateAdminId());
				categoryObj.addProperty("update_admin_name", DiscoverServletUtil.getAdminName(refAdminMap, category.hasUpdateAdminId(), category.getUpdateAdminId()));
				categoryObj.addProperty("update_time", DiscoverServletUtil.getDateStr(category.hasUpdateTime(), category.getUpdateTime()));
				categoryArray.add(categoryObj);
			}
			
			moduleObj.add("category", categoryArray);
			moduleObj.addProperty("state", module.getState().name());
			moduleObj.addProperty("create_admin_id", module.getCreateAdminId());
			moduleObj.addProperty("create_admin_name", DiscoverServletUtil.getAdminName(refAdminMap, module.hasCreateAdminId(), module.getCreateAdminId()));
			moduleObj.addProperty("create_time", DiscoverServletUtil.getDateStr(module.hasCreateTime(), module.getCreateTime()));
			moduleObj.addProperty("update_admin_id", module.getUpdateAdminId());
			moduleObj.addProperty("update_admin_name", DiscoverServletUtil.getAdminName(refAdminMap, module.hasUpdateAdminId(), module.getUpdateAdminId()));
			moduleObj.addProperty("update_time", DiscoverServletUtil.getDateStr(module.hasUpdateTime(), module.getUpdateTime()));

			moduleArray.add(moduleObj);
		}
		
		JsonObject resultObj = new JsonObject();
		resultObj.add("banner", moduleArray);
		httpResponse.setContentType("application/json;charset=UTF-8");
		JsonUtil.GSON.toJson(resultObj, httpResponse.getWriter());
	}
}
