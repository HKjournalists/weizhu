package com.weizhu.webapp.admin.api.offline_training;

import java.util.Map;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.weizhu.proto.AdminProtos;
import com.weizhu.proto.AllowProtos;
import com.weizhu.proto.OfflineTrainingProtos;
import com.weizhu.proto.UserProtos;

public class OfflineTrainingUtil {

	public static JsonObject buildTrain(
			int now, 
			OfflineTrainingProtos.Train train, 
			Map<Integer, OfflineTrainingProtos.TrainCount> refTrainCountMap,
			Map<Long, UserProtos.User> refUserMap,
			Map<Long, AdminProtos.Admin> refAdminMap,
			Map<Integer, AllowProtos.Model> refAllowModelMap
			) {
		JsonObject obj = new JsonObject();
		
		obj.addProperty("train_id", train.getTrainId());
		obj.addProperty("train_name", train.getTrainName());
		if (train.hasImageName()) {
			obj.addProperty("image_name", train.getImageName());
		}
		obj.addProperty("start_time", train.getStartTime());
		obj.addProperty("end_time", train.getEndTime());
		obj.addProperty("apply_enable", train.getApplyEnable());
		if (train.hasApplyStartTime()) {
			obj.addProperty("apply_start_time", train.getApplyStartTime());
		}
		if (train.hasApplyEndTime()) {
			obj.addProperty("apply_end_time", train.getApplyEndTime());
		}
		if (train.hasApplyUserCount()) {
			obj.addProperty("apply_user_count", train.getApplyUserCount());
		}
		if (train.hasApplyIsNotify()) {
			obj.addProperty("apply_is_notify", train.getApplyIsNotify());
		}
		
		obj.addProperty("train_address", train.getTrainAddress());
		if (train.hasLecturerName()) {
			obj.addProperty("lecturer_name", train.getLecturerName());
		}
		if (train.getLecturerUserIdCount() > 0) {
			JsonArray lecturerUserArray = new JsonArray();
			for (Long userId : train.getLecturerUserIdList()) {
				UserProtos.User user = refUserMap.get(userId);
				
				JsonObject userObj = new JsonObject();
				userObj.addProperty("user_id", userId);
				userObj.addProperty("user_name", user != null ? user.getBase().getUserName() : "该用户已被删除");
			
				lecturerUserArray.add(userObj);
			}
			obj.add("lecturer_user", lecturerUserArray);
		}
		
		obj.addProperty("check_in_start_time", train.getCheckInStartTime());
		obj.addProperty("check_in_end_time", train.getCheckInEndTime());
		obj.addProperty("arrangement_text", train.getArrangementText());
		if (train.hasDescribeText()) {
			obj.addProperty("describe_text", train.getDescribeText());
		}
		
		if (train.hasAllowModelId()) {
			obj.addProperty("allow_model_id", train.getAllowModelId());
			
			AllowProtos.Model allowModel = refAllowModelMap.get(train.getAllowModelId());
			obj.addProperty("allow_model_name", allowModel == null ? "已删除" : allowModel.getModelName());
		}
		
		if (train.getDiscoverItemIdCount() > 0) {
			JsonArray discoverItemIdArray = new JsonArray();
			for (Long itemId : train.getDiscoverItemIdList()) {
				discoverItemIdArray.add(itemId);
			}
			obj.add("discover_item_id", discoverItemIdArray);
		}
		
		OfflineTrainingProtos.TrainCount trainCount = refTrainCountMap.get(train.getTrainId());
		obj.addProperty("user_allow_count", trainCount == null ? 0 : trainCount.getUserAllowCount());
		obj.addProperty("user_apply_count", trainCount == null ? 0 : trainCount.getUserApplyCount());
		obj.addProperty("user_check_in_count", trainCount == null ? 0 : trainCount.getUserCheckInCount());
		obj.addProperty("user_leave_count", trainCount == null ? 0 : trainCount.getUserLeaveCount());
		
		if (train.getApplyEnable()) {
			if (now < train.getApplyStartTime()) {
				obj.addProperty("apply_state", "报名未开始");
			} else if (now < train.getApplyEndTime()) {
				if (trainCount != null && trainCount.getUserApplyCount() >= train.getApplyUserCount()) {
					obj.addProperty("apply_state", "报名已满员");
				} else {
					obj.addProperty("apply_state", "报名正在进行");
				}
			} else {
				obj.addProperty("apply_state", "报名已结束");
			}
		}
		if (now < train.getStartTime()) {
			obj.addProperty("train_state", "培训未开始");
		} else if (now < train.getEndTime()) {
			obj.addProperty("train_state", "培训正在进行");
		} else {
			obj.addProperty("train_state", "培训已结束");
		}
		if (now < train.getCheckInStartTime()) {
			obj.addProperty("check_in_state", "签到未开始");
		} else if (now < train.getCheckInEndTime()) {
			obj.addProperty("check_in_state", "签到正在进行");
		} else {
			obj.addProperty("check_in_state", "签到已结束");
		}
		
		obj.addProperty("state", train.getState().name());
		
		if (train.hasCreateAdminId()) {
			obj.addProperty("create_admin_id", train.getCreateAdminId());
			AdminProtos.Admin admin = refAdminMap.get(train.getCreateAdminId());
			obj.addProperty("create_admin_name", admin != null ? admin.getAdminName() : "[已删除管理员:" + train.getCreateAdminId() + "]");
		}
		if (train.hasCreateTime()) {
			obj.addProperty("create_time", train.getCreateTime());
		}
		
		if (train.hasUpdateAdminId()) {
			obj.addProperty("update_admin_id", train.getUpdateAdminId());
			AdminProtos.Admin admin = refAdminMap.get(train.getUpdateAdminId());
			obj.addProperty("update_admin_name", admin != null ? admin.getAdminName() : "[已删除管理员:" + train.getUpdateAdminId() + "]");
		}
		if (train.hasUpdateTime()) {
			obj.addProperty("update_time", train.getUpdateTime());
		}
		return obj;
	}
}
