package com.weizhu.webapp.admin.api.qa;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.weizhu.proto.AdminProtos;
import com.weizhu.proto.QAProtos;
import com.weizhu.proto.UserProtos;

public class QAServletUtil {
	public static final String ANONYMOUS_USER = "匿名用户";
	public static final String SYSTEM_USER = "官方用户";

	public static String getUserName(Map<Long, UserProtos.User> userMap, Map<Long, AdminProtos.Admin> adminMap, long userId, boolean isAdmin) {
		String userName = "";
		if (isAdmin) {
			AdminProtos.Admin admin = adminMap.get(userId);

			if (admin == null) {
				userName = SYSTEM_USER + ":" + userId;
			} else {
				userName = admin.getAdminName();
			}
		} else {
			UserProtos.User user = userMap.get(userId);

			if (user == null) {
				userName = ANONYMOUS_USER + ":" + userId;
			} else {
				userName = user.getBase().getUserName();
			}
		}

		return userName;
	}

	public static Map<Long, AdminProtos.Admin> getAdminMap(List<AdminProtos.Admin> admins) {
		Map<Long, AdminProtos.Admin> adminMap = new HashMap<Long, AdminProtos.Admin>();
		for (AdminProtos.Admin admin : admins) {
			adminMap.put(admin.getAdminId(), admin);
		}
		return adminMap;
	}

	public static Map<Long, UserProtos.User> getUserMap(List<UserProtos.User> users) {
		Map<Long, UserProtos.User> userMap = new HashMap<Long, UserProtos.User>();
		for (UserProtos.User user : users) {
			userMap.put(user.getBase().getUserId(), user);
		}
		return userMap;
	}

	public static Map<Integer, QAProtos.Category> getCategoryMap(List<QAProtos.Category> categorys) {
		//获取问题列表对应的分类信息
		Map<Integer, QAProtos.Category> categoryMap = new HashMap<Integer, QAProtos.Category>();
		for (QAProtos.Category category : categorys) {
			categoryMap.put(category.getCategoryId(), category);
		}
		return categoryMap;
	}

	public static String getCategoryName(Map<Integer, QAProtos.Category> categoryMap, int categoryId) {
		QAProtos.Category category = categoryMap.get(categoryId);
		String categoryName = "";
		if (category == null) {
			categoryName = "未找到该分类：" + categoryId;
		} else {
			categoryName = category.getCategoryName();
		}
		return categoryName;
	}

	public static String getDate(int date) {
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		String dateFormate = sdf.format(new Date(date * 1000L));
		return dateFormate;
	}
}
