package com.weizhu.webapp.mobile.offline_training;

import java.util.Map;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.weizhu.proto.DiscoverV2Protos;
import com.weizhu.proto.OfflineTrainingProtos;
import com.weizhu.proto.UserProtos;
import com.weizhu.proto.UploadProtos.GetUploadUrlPrefixResponse;

public class OfflineTrainingUtil {

	public static JsonObject buildTrain(
			int now, 
			OfflineTrainingProtos.Train train, 
			Map<Integer, OfflineTrainingProtos.TrainCount> refTrainCountMap,
			Map<Integer, OfflineTrainingProtos.TrainUser> refTrainUserMap,
			Map<Long, UserProtos.User> refUserMap,
			Map<Long, DiscoverV2Protos.Item> refDiscoverItemMap,
			GetUploadUrlPrefixResponse getUploadUrlPrefixResponse
			) {
		JsonObject obj = new JsonObject();
		
		obj.addProperty("train_id", train.getTrainId());
		obj.addProperty("train_name", train.getTrainName());
		if (train.hasImageName()) {
			obj.addProperty("image_name", train.getImageName());
			obj.addProperty("image_url", getUploadUrlPrefixResponse.getImageUrlPrefix() + train.getImageName());
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
		
		obj.addProperty("train_address", train.getTrainAddress());
		if (train.hasLecturerName()) {
			obj.addProperty("lecturer_name", train.getLecturerName());
		}
		if (train.getLecturerUserIdCount() > 0) {
			JsonArray lecturerUserArray = new JsonArray();
			for (Long userId : train.getLecturerUserIdList()) {
				UserProtos.User user = refUserMap.get(userId);
				
				if (user != null) {
					JsonObject userObj = new JsonObject();
					userObj.addProperty("user_id", userId);
					userObj.addProperty("user_name", user.getBase().getUserName());
					if (user.getBase().hasAvatar()) {
						userObj.addProperty("avatar_60_url", getUploadUrlPrefixResponse.getImage60UrlPrefix() + user.getBase().getAvatar());
					}
					lecturerUserArray.add(userObj);
				}
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
		
		OfflineTrainingProtos.TrainUser trainUser = refTrainUserMap.get(train.getTrainId());
		if (trainUser != null && train.getApplyEnable()) {
			obj.addProperty("is_apply", trainUser.getIsApply());
			if (trainUser.hasApplyTime()) {
				obj.addProperty("apply_time", trainUser.getApplyTime());
			}
		}
		
		obj.addProperty("is_check_in", trainUser == null ? false : trainUser.getIsCheckIn());
		if (trainUser != null && trainUser.hasCheckInTime()) {
			obj.addProperty("check_in_time", trainUser.getCheckInTime());
		}
		obj.addProperty("is_leave", trainUser == null ? false : trainUser.getIsLeave());
		if (trainUser != null && trainUser.hasLeaveTime()) {
			obj.addProperty("leave_time", trainUser.getLeaveTime());
		}
		if (trainUser != null && trainUser.hasLeaveReason()) {
			obj.addProperty("leave_reason", trainUser.getLeaveReason());
		}
		
		if (train.getDiscoverItemIdCount() > 0) {
			JsonArray discoverItemArray = new JsonArray();
			for (Long itemId : train.getDiscoverItemIdList()) {
				DiscoverV2Protos.Item item = refDiscoverItemMap.get(itemId);
				if (item != null) {
					JsonObject itemObj = new JsonObject();
					itemObj.addProperty("item_id", item.getBase().getItemId());
					itemObj.addProperty("item_name", item.getBase().getItemName());
					discoverItemArray.add(itemObj);
				}
			}
			obj.add("discover_item", discoverItemArray);
		}
		return obj;
	}
}
