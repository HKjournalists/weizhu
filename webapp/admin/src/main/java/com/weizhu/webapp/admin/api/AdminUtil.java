package com.weizhu.webapp.admin.api;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import javax.annotation.Nullable;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.weizhu.proto.AdminProtos;
import com.weizhu.proto.CompanyProtos;
import com.weizhu.webapp.admin.PermissionConst;

public class AdminUtil {
	
	public static Map<Integer, AdminProtos.Role> toRefRoleMap(List<AdminProtos.Role> refRoleList) {
		if (refRoleList.isEmpty()) {
			return Collections.emptyMap();
		}
		Map<Integer, AdminProtos.Role> refRoleMap = new TreeMap<Integer, AdminProtos.Role>();
		for (AdminProtos.Role role : refRoleList) {
			refRoleMap.put(role.getRoleId(), role);
		}
		return refRoleMap;
	}
	
	public static JsonObject toJsonAdmin(AdminProtos.Admin admin, @Nullable Long companyId, @Nullable Map<Integer, AdminProtos.Role> refRoleMap) {
		JsonObject adminObj = new JsonObject();
		adminObj.addProperty("admin_id", admin.getAdminId());
		adminObj.addProperty("admin_name", admin.getAdminName());
		adminObj.addProperty("admin_email", admin.getAdminEmail());
		adminObj.addProperty("force_reset_password", admin.getForceResetPassword());
		
		if (companyId != null && refRoleMap != null) {
			AdminProtos.Admin.Company adminCompany = null;
			for (AdminProtos.Admin.Company c : admin.getCompanyList()) {
				if (c.getCompanyId() == companyId) {
					adminCompany = c;
					break;
				}
			}
			if (adminCompany != null) {
				JsonArray roleArray = new JsonArray();
				for (Integer roleId : adminCompany.getRoleIdList()) {
					AdminProtos.Role role = refRoleMap.get(roleId);
					if (role != null && (!role.hasCompanyId() || role.getCompanyId() == companyId)) {
						roleArray.add(toJsonRole(role));
					}
				}
				adminObj.add("role", roleArray);
				adminObj.addProperty("enable_team_permit", adminCompany.getEnableTeamPermit());
				
				JsonArray permitTeamIdArray = new JsonArray();
				for (Integer permitTeamId : adminCompany.getPermitTeamIdList()) {
					permitTeamIdArray.add(permitTeamId);
				}
				adminObj.add("permit_team_id", permitTeamIdArray);
			}
		}
		
		adminObj.addProperty("state", admin.getState().name());
		if (admin.hasCreateTime()) {
			adminObj.addProperty("create_time", admin.getCreateTime());
		}
		if (admin.hasCreateAdminId()) {
			adminObj.addProperty("create_admin_id", admin.getCreateAdminId());
		}
		if (admin.hasUpdateTime()) {
			adminObj.addProperty("update_time", admin.getUpdateTime());
		}
		if (admin.hasUpdateAdminId()) {
			adminObj.addProperty("update_admin_id", admin.getUpdateAdminId());
		}
		return adminObj;
	}
	
	public static JsonObject toJsonCompany(CompanyProtos.Company company) {
		JsonObject companyObj = new JsonObject();
		companyObj.addProperty("company_id", company.getCompanyId());
		companyObj.addProperty("company_name", company.getCompanyName());
		return companyObj;
	}
	
	public static JsonObject toJsonRole(AdminProtos.Role role) {
		JsonObject roleObj = new JsonObject();
		roleObj.addProperty("role_id", role.getRoleId());
		roleObj.addProperty("role_name", role.getRoleName());
		
		JsonArray permissionIdArray = new JsonArray();
		for (String permissionId : role.getPermissionIdList()) {
			permissionIdArray.add(permissionId);
		}
		roleObj.add("permission_id", permissionIdArray);
		
		roleObj.addProperty("state", role.getState().name());
		if (role.hasCreateTime()) {
			roleObj.addProperty("create_time", role.getCreateTime());
		}
		if (role.hasCreateAdminId()) {
			roleObj.addProperty("create_admin_id", role.getCreateAdminId());
		}
		if (role.hasUpdateTime()) {
			roleObj.addProperty("update_time", role.getUpdateTime());
		}
		if (role.hasUpdateAdminId()) {
			roleObj.addProperty("update_admin_id", role.getUpdateAdminId());
		}
		return roleObj;
	}
	
	public static JsonArray toJsonPermissionGroupList(@Nullable Set<String> permissionIdSet) {
		JsonArray groupArray = new JsonArray();
		for (PermissionConst.Group g : PermissionConst.permissionGroupList()) {
			groupArray.add(toJsonGroup(g, permissionIdSet));
		}
		return groupArray;
	}
	
	private static JsonObject toJsonGroup(PermissionConst.Group group, @Nullable Set<String> permissionIdSet) {
		JsonObject obj = new JsonObject();
		obj.addProperty("group_id", group.groupId());
		obj.addProperty("group_name", group.groupName());
		
		JsonArray groupArray = new JsonArray();
		for (PermissionConst.Group g : group.groupList()) {
			groupArray.add(toJsonGroup(g, permissionIdSet));
		}
		obj.add("group", groupArray);
		
		JsonArray permissionArray = new JsonArray();
		for (PermissionConst.Permission p : group.permissionList()) {
			groupArray.add(toJsonPermission(p, permissionIdSet));
		}
		obj.add("permission", permissionArray);
		
		return obj;
	}
	
	private static JsonObject toJsonPermission(PermissionConst.Permission permission, @Nullable Set<String> permissionIdSet) {
		JsonObject obj = new JsonObject();
		obj.addProperty("permission_id", permission.permissionId());
		obj.addProperty("permission_name", permission.permissionName());
		if (permissionIdSet != null) {
			obj.addProperty("has_permission", permissionIdSet.contains(permission.permissionId()));
		}
		return obj;
	}
	
}
