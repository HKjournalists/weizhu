package com.weizhu.webapp.admin.api.discover;

import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import com.google.common.util.concurrent.Futures;
import com.weizhu.proto.AdminProtos;
import com.weizhu.proto.AdminService;
import com.weizhu.proto.AdminUserProtos;
import com.weizhu.proto.AdminUserService;
import com.weizhu.proto.AllowProtos;
import com.weizhu.proto.AllowService;
import com.weizhu.proto.UserProtos;
import com.weizhu.proto.AdminProtos.AdminHead;
import com.weizhu.proto.AllowProtos.GetModelByIdRequest;
import com.weizhu.proto.AllowProtos.GetModelByIdResponse;

public class DiscoverServletUtil {
	
	public static String getDateStr(boolean hasTimestampSecond, int timestampSecond) {
		if (hasTimestampSecond) {
			SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
			return df.format(new Date(timestampSecond * 1000L));
		} else {
			return "";
		}
	}
	
	public static Map<Long, UserProtos.User> getUserMap(AdminUserService adminUserService, AdminHead head, Set<Long> userIdSet) {
		if (userIdSet.isEmpty()) {
			return Collections.emptyMap();
		}
		
		AdminUserProtos.GetUserByIdResponse response = Futures.getUnchecked(
				adminUserService.getUserById(head, AdminUserProtos.GetUserByIdRequest.newBuilder()
						.addAllUserId(userIdSet)
						.build()));
		if (response.getUserCount() <= 0) {
			return Collections.emptyMap();
		}
	
		Map<Long, UserProtos.User> userMap = new TreeMap<Long, UserProtos.User>();
		for (UserProtos.User user : response.getUserList()) {
			userMap.put(user.getBase().getUserId(), user);
		}
		return userMap;
	}
	
	public static String getUserName(Map<Long, UserProtos.User> userMap, boolean hasUserId, long userId) {
		if (hasUserId) {
			UserProtos.User user = userMap.get(userId);
			return user == null ? "已删除用户:" + userId : user.getBase().getUserName();
		} else {
			return "";
		}
	}
	
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
	
	public static Map<Integer, AllowProtos.Model> getAllowModelMap(AllowService allowService, AdminHead head, Set<Integer> allowModelIdSet) {
		if (allowModelIdSet.isEmpty()) {
			return Collections.emptyMap();
		}
		
		GetModelByIdResponse response = Futures.getUnchecked(
				allowService.getModelById(
						head, GetModelByIdRequest.newBuilder()
						.addAllModelId(allowModelIdSet)
						.build()));
		
		if (response.getModelCount() <= 0) {
			return Collections.emptyMap();
		}
		
		Map<Integer, AllowProtos.Model> allowModelMap = new TreeMap<Integer, AllowProtos.Model>();
		for (AllowProtos.Model model : response.getModelList()) {
			allowModelMap.put(model.getModelId(), model);
		}
		return allowModelMap;
	}
	
	public static String getAllowModelName(Map<Integer, AllowProtos.Model> allowModelMap, int allowModelId) {
		AllowProtos.Model model = allowModelMap.get(allowModelId);
		return model == null ? "已删除访问模型:" + allowModelId : model.getModelName();
	}
}
