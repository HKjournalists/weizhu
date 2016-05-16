package com.weizhu.webapp.admin.api.component;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import com.google.common.util.concurrent.Futures;
import com.weizhu.proto.AdminProtos;
import com.weizhu.proto.AdminService;
import com.weizhu.proto.AdminProtos.AdminHead;

public class ComponentServletUtil {

	public static Map<Long, AdminProtos.Admin> getAdminMap(AdminService adminService, AdminHead head, Set<Long> adminIdSet) {
		if (adminIdSet.isEmpty()) {
			return Collections.emptyMap();
		}
		
		AdminProtos.GetAdminByIdResponse response = Futures.getUnchecked(
				adminService.getAdminById(
						head, AdminProtos.GetAdminByIdRequest.newBuilder()
						.addAllAdminId(adminIdSet)
						.build()));
		
		if (response.getAdminCount() <= 0) {
			return Collections.emptyMap();
		}
		
		Map<Long, AdminProtos.Admin> adminMap = new TreeMap<Long, AdminProtos.Admin>();
		for (AdminProtos.Admin admin : response.getAdminList()) {
			adminMap.put(admin.getAdminId(), admin);
		}
		return adminMap;
	}
	
	public static String getAdminName(Map<Long, AdminProtos.Admin> adminMap, boolean hasAdminId, long adminId) {
		if (hasAdminId) {
			AdminProtos.Admin admin = adminMap.get(adminId);
			return admin == null ? "已删除管理员:" + adminId : admin.getAdminName();
		} else {
			return "";
		}
	}
}
