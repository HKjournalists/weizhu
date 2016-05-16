package com.weizhu.webapp.admin.api;

import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.weizhu.proto.AdminProtos;
import com.weizhu.proto.AllowProtos;
import com.weizhu.proto.AllowProtos.Model;
import com.weizhu.proto.UserProtos;

public class Util {

	public static final String ANONYMOUS_USER = "匿名用户";
	public static final String SYSTEM_USER = "官方用户";

	public static String getDate(int date) {
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		String dateFormate = sdf.format(new Date(date * 1000L));
		return dateFormate;
	}

	public static Map<Long, UserProtos.User> getUserMap(List<UserProtos.User> users) {
		Map<Long, UserProtos.User> userMap = new HashMap<Long, UserProtos.User>();
		for (UserProtos.User user : users) {
			userMap.put(user.getBase().getUserId(), user);
		}
		return userMap;
	}

	public static String getUserName(Map<Long, UserProtos.User> userMap, long userId) {
		String userName = "";
		UserProtos.User user = userMap.get(userId);

		if (user == null) {
			userName = ANONYMOUS_USER + ":" + userId;
		} else {
			userName = user.getBase().getUserName();
		}

		return userName;
	}

	public static String getAdminName(Map<Long, AdminProtos.Admin> adminMap, long adminId) {
		String userName = "";
		AdminProtos.Admin admin = adminMap.get(adminId);

		if (admin == null) {
			userName = SYSTEM_USER + ":" + adminId;
		} else {
			userName = admin.getAdminName();
		}

		return userName;
	}

	public static Map<Long, AdminProtos.Admin> getAdminMap(Collection<AdminProtos.Admin> admins) {
		Map<Long, AdminProtos.Admin> adminMap = new HashMap<Long, AdminProtos.Admin>();
		for (AdminProtos.Admin admin : admins) {
			adminMap.put(admin.getAdminId(), admin);
		}
		return adminMap;
	}

	public static Map<Integer, Model> getAllowModelMap(Collection<Model> models) {
		Map<Integer, AllowProtos.Model> allowModelMap = new HashMap<Integer, AllowProtos.Model>();
		for (AllowProtos.Model model : models) {
			allowModelMap.put(model.getModelId(), model);
		}
		return allowModelMap;
	}

	public static String getAllowModelName(Map<Integer, Model> allowModelMap, int modleId) {
		String modelName = "";
		Model model = allowModelMap.get(modleId);

		if (model == null) {
			modelName = "不存在的allow_model_id:" + modleId;
		} else {
			modelName = model.getModelName();
		}

		return modelName;
	}
}
