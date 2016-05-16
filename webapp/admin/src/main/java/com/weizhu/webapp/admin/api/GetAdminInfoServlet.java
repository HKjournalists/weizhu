package com.weizhu.webapp.admin.api;

import java.io.IOException;
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
import com.weizhu.proto.AdminProtos;
import com.weizhu.proto.CompanyProtos;
import com.weizhu.proto.SystemProtos;
import com.weizhu.proto.SystemService;
import com.weizhu.proto.UserProtos;
import com.weizhu.web.filter.AdminInfo;
import com.weizhu.webapp.admin.PermissionConst;
import com.weizhu.common.service.ServiceUtil;
import com.weizhu.common.utils.JsonUtil;
import com.weizhu.proto.AdminProtos.AdminHead;
import com.weizhu.proto.AdminUserProtos.GetTeamByIdRequest;
import com.weizhu.proto.AdminUserProtos.GetTeamByIdResponse;
import com.weizhu.proto.AdminUserService;
import com.weizhu.proto.SystemProtos.GetAdminConfigResponse;

@Singleton
@SuppressWarnings("serial")
public class GetAdminInfoServlet extends HttpServlet {

	private final Provider<AdminHead> adminHeadProvider;
	private final Provider<AdminInfo> adminInfoProvider;
	private final AdminUserService adminUserService;
	private final SystemService systemService;
	
	@Inject
	public GetAdminInfoServlet(
			Provider<AdminHead> adminHeadProvider, 
			Provider<AdminInfo> adminInfoProvider, 
			AdminUserService adminUserService,
			SystemService systemService
			) {
		this.adminHeadProvider = adminHeadProvider;
		this.adminInfoProvider = adminInfoProvider;
		this.adminUserService = adminUserService;
		this.systemService = systemService;
	}
	
	@Override
	protected void doGet(HttpServletRequest httpRequest, HttpServletResponse httpResponse) throws ServletException, IOException {
		this.doPost(httpRequest, httpResponse);
	}
	
	@Override
	protected void doPost(HttpServletRequest httpRequest, HttpServletResponse httpResponse) throws ServletException, IOException {		
		final AdminHead head = this.adminHeadProvider.get();
		final AdminInfo adminInfo = this.adminInfoProvider.get();
		
		if (!head.hasCompanyId()) {
			httpResponse.sendError(HttpServletResponse.SC_FORBIDDEN);
			return;
		}
		
		JsonObject resultObj = new JsonObject();
		
		final AdminProtos.Admin admin = adminInfo.admin();

		resultObj.add("admin", AdminUtil.toJsonAdmin(admin, head.getCompanyId(), adminInfo.refRoleMap()));
		
		final CompanyProtos.Company company = adminInfo.refCompanyMap().get(head.getCompanyId());

		resultObj.add("company", AdminUtil.toJsonCompany(company));
		
		AdminProtos.Admin.Company adminCompany = null;
		for (AdminProtos.Admin.Company c : admin.getCompanyList()) {
			if (c.getCompanyId() == head.getCompanyId()) {
				adminCompany = c;
				break;
			}
		}
		if (adminCompany == null) {
			throw new RuntimeException("cannot find admin company : " + head.getSession().getAdminId() + ", " + head.getCompanyId());
		}
		
		Set<String> permissionIdSet = new TreeSet<String>();
		for (Integer roleId : adminCompany.getRoleIdList()) {
			AdminProtos.Role role = adminInfo.refRoleMap().get(roleId);
			if (role != null) {
				permissionIdSet.addAll(role.getPermissionIdList());
			}
		}
		
		JsonArray permissionArray = new JsonArray();
		for (String permissionId : permissionIdSet) {
			PermissionConst.Permission permission = PermissionConst.permissionMap().get(permissionId);
			if (permission != null) {
				JsonObject permissionObj = new JsonObject();
				permissionObj.addProperty("permission_id", permission.permissionId());
				permissionObj.addProperty("permission_name", permission.permissionName());
				permissionArray.add(permissionObj);
			}
		}
		
		resultObj.add("permission", permissionArray);
		
		JsonObject teamPermitObj = new JsonObject();
		teamPermitObj.addProperty("enable_team_permit", adminCompany.getEnableTeamPermit());
		if (adminCompany.getEnableTeamPermit()) {
			
			GetTeamByIdResponse getTeamResponse = Futures.getUnchecked(
					this.adminUserService.getTeamById(head, GetTeamByIdRequest.newBuilder()
					.addAllTeamId(adminCompany.getPermitTeamIdList())
					.build()));
			
			JsonArray permitTeamArray = new JsonArray();
			for (UserProtos.Team team : getTeamResponse.getTeamList()) {
				JsonObject teamObj = new JsonObject();
				teamObj.addProperty("team_id", team.getTeamId());
				teamObj.addProperty("team_name", team.getTeamName());
				permitTeamArray.add(teamObj);
			}
			teamPermitObj.add("permit_team", permitTeamArray);
		}
		
		resultObj.add("team_permit", teamPermitObj);
		
		JsonObject configObj = new JsonObject();
		
		final GetAdminConfigResponse getConfigResponse = Futures.getUnchecked(this.systemService.getAdminConfig(head, ServiceUtil.EMPTY_REQUEST));
		configObj.addProperty("webapp_mobile_url_prefix", getConfigResponse.getAdmin().getWebappMobileUrlPrefix());
		configObj.addProperty("webapp_web_url_prefix", getConfigResponse.getAdmin().getWebappWebUrlPrefix());
		configObj.addProperty("webapp_upload_url_prefix", getConfigResponse.getAdmin().getWebappUploadUrlPrefix());
		configObj.addProperty("image_url_prefix", getConfigResponse.getImage().getImageUrlPrefix());
		configObj.addProperty("image_60_url_prefix", getConfigResponse.getImage().getImage60UrlPrefix());
		configObj.addProperty("image_120_url_prefix", getConfigResponse.getImage().getImage120UrlPrefix());
		configObj.addProperty("image_240_url_prefix", getConfigResponse.getImage().getImage240UrlPrefix());
		configObj.addProperty("image_480_url_prefix", getConfigResponse.getImage().getImage480UrlPrefix());
		
		JsonArray dynamicConfigArray = new JsonArray();
		for (SystemProtos.DynamicConfig dynamicConfig : getConfigResponse.getDynamicList()) {
			JsonObject dynamicConfigObj = new JsonObject();
			dynamicConfigObj.addProperty("name", dynamicConfig.getName());
			dynamicConfigObj.addProperty("value", dynamicConfig.getValue());
		}
		configObj.add("dynamic_config", dynamicConfigArray);
		
		resultObj.add("config", configObj);
		
		httpResponse.setContentType("application/json;charset=UTF-8");
		JsonUtil.GSON.toJson(resultObj, httpResponse.getWriter());
	}

}
