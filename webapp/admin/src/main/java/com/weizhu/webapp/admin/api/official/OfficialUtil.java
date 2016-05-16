package com.weizhu.webapp.admin.api.official;

import java.util.Map;

import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.weizhu.proto.AdminOfficialProtos;
import com.weizhu.proto.AdminProtos;
import com.weizhu.proto.AllowProtos;
import com.weizhu.proto.OfficialProtos;
import com.weizhu.proto.UserProtos;

public class OfficialUtil {
	
	public static JsonObject buildJsonObject(
			OfficialProtos.Official official, 
			Map<Integer, AllowProtos.Model> allowModelMap, 
			Map<Long, AdminProtos.Admin> refAdminMap, 
			String imageUrlPrefix
			) {
		
		JsonObject obj = new JsonObject();
		
		obj.addProperty("official_id", official.getOfficialId());
		obj.addProperty("official_name", official.getOfficialName());
		obj.addProperty("avatar", official.getAvatar());
		obj.addProperty("avatar_url", imageUrlPrefix + official.getAvatar());
		
		if (official.hasAllowModelId()) {
			int modelId = official.getAllowModelId();
			obj.addProperty("allow_model_id", modelId);
			AllowProtos.Model model = allowModelMap.get(modelId);
			if (model != null) {
				obj.addProperty("allow_model_name", model.getModelName());
			} else {
				obj.addProperty("allow_model_name", "");
			}
		} else {
			obj.add("allow_model_id", JsonNull.INSTANCE);
			obj.add("allow_model_name", JsonNull.INSTANCE);
		}
		
		if (official.hasOfficialDesc()) {
			obj.addProperty("official_desc", official.getOfficialDesc());
		}
		if (official.hasFunctionDesc()) {
			obj.addProperty("function_desc", official.getFunctionDesc());
		}
		
		obj.addProperty("is_enable", official.getState() == OfficialProtos.State.NORMAL);
		obj.addProperty("state", official.getState().name());
		if (official.hasCreateAdminId()) {
			obj.addProperty("create_admin_id", official.getCreateAdminId());
			AdminProtos.Admin admin = refAdminMap.get(official.getCreateAdminId());
			obj.addProperty("create_admin_name", admin != null ? admin.getAdminName() : "[AdminId:" + official.getCreateAdminId() + "]");
		}
		if (official.hasCreateTime()) {
			obj.addProperty("create_time", official.getCreateTime());
		}
		
		if (official.hasUpdateAdminId()) {
			obj.addProperty("update_admin_id", official.getUpdateAdminId());
			AdminProtos.Admin admin = refAdminMap.get(official.getUpdateAdminId());
			obj.addProperty("update_admin_name", admin != null ? admin.getAdminName() : "[AdminId:" + official.getUpdateAdminId() + "]");
		}
		if (official.hasUpdateTime()) {
			obj.addProperty("update_time", official.getUpdateTime());
		}
		return obj;
	}
	
	public static JsonObject buildJsonObject(
			AdminOfficialProtos.OfficialSendPlan sendPlan,
			Map<Integer, AllowProtos.Model> allowModelMap,
			Map<Long, OfficialProtos.Official> refOfficialMap,
			Map<Long, AdminProtos.Admin> refAdminMap, 
			Map<Long, UserProtos.User> refUserMap, 
			String imageUrlPrefix
			) {
		JsonObject obj = new JsonObject();
		
		obj.addProperty("plan_id", sendPlan.getPlanId());
		obj.addProperty("official_id", sendPlan.getOfficialId());
		
		OfficialProtos.Official official = refOfficialMap.get(sendPlan.getOfficialId());
		obj.addProperty("official_name", official != null ? official.getOfficialName() : "[OfficialId:" + sendPlan.getOfficialId() + "]");
		
		if (official != null && official.hasAllowModelId()) {
			int modelId = official.getAllowModelId();
			obj.addProperty("allow_model_id", modelId);
			AllowProtos.Model model = allowModelMap.get(modelId);
			if (model != null) {
				obj.addProperty("allow_model_name", model.getModelName());
			} else {
				obj.add("allow_model_name", JsonNull.INSTANCE);
			}
		} else {
			obj.add("allow_model_id", JsonNull.INSTANCE);
			obj.add("allow_model_name", JsonNull.INSTANCE);
		}
		
		obj.add("send_msg", buildJsonObject(sendPlan.getSendMsg(), refUserMap, imageUrlPrefix));
		
		obj.addProperty("send_time", sendPlan.getSendTime());
		obj.addProperty("send_state", sendPlan.getSendState().name());
		if (sendPlan.hasCreateAdminId()) {
			obj.addProperty("create_admin_id", sendPlan.getCreateAdminId());
			AdminProtos.Admin admin = refAdminMap.get(sendPlan.getCreateAdminId());
			obj.addProperty("create_admin_name", admin != null ? admin.getAdminName() : "[AdminId:" + sendPlan.getCreateAdminId() + "]");
		}
		if (sendPlan.hasCreateTime()) {
			obj.addProperty("create_time", sendPlan.getCreateTime());
		}
		
		if (sendPlan.hasUpdateAdminId()) {
			obj.addProperty("update_admin_id", sendPlan.getUpdateAdminId());
			AdminProtos.Admin admin = refAdminMap.get(sendPlan.getUpdateAdminId());
			obj.addProperty("update_admin_name", admin != null ? admin.getAdminName() : "[AdminId:" + sendPlan.getUpdateAdminId() + "]");
		}
		if (sendPlan.hasUpdateTime()) {
			obj.addProperty("update_time", sendPlan.getUpdateTime());
		}
		
		return obj;
	}

	public static JsonObject buildJsonObject(
			OfficialProtos.OfficialMessage msg, 
			Map<Long, UserProtos.User> refUserMap, 
			String imageUrlPrefix
			) {
		JsonObject obj = new JsonObject();
		
		obj.addProperty("msg_seq", msg.getMsgSeq());
		obj.addProperty("msg_time", msg.getMsgTime());
		
		switch (msg.getMsgTypeCase()) {
			case TEXT: {
				JsonObject text = new JsonObject();
				text.addProperty("content", msg.getText().getContent());
				obj.add("text", text);
				break;
			}
			case VOICE: {
				JsonObject voice = new JsonObject();
				voice.addProperty("duration", msg.getVoice().getDuration());
				obj.add("voice", voice);
				break;
			}
			case IMAGE: {
				JsonObject image = new JsonObject();
				image.addProperty("name", msg.getImage().getName());
				image.addProperty("url", imageUrlPrefix + msg.getImage().getName());
				obj.add("image", image);
				break;
			}
			case USER: {
				UserProtos.User u = refUserMap.get(msg.getUser().getUserId());
				JsonObject userObj = new JsonObject();
				userObj.addProperty("user_id", msg.getUser().getUserId());
				userObj.addProperty("user_name", u != null ? u.getBase().getUserName() : "未知[UserId:" + msg.getUser().getUserId() + "]");
				obj.add("user", userObj);
				break;
			}
			case DISCOVER_ITEM: {
				JsonObject discoverItem = new JsonObject();
				discoverItem.addProperty("item_id", msg.getDiscoverItem().getItemId());
				obj.add("discover_item", discoverItem);
				break;
			}
			case COMMUNITY_POST: {
				JsonObject communityPost = new JsonObject();
				communityPost.addProperty("post_id", msg.getCommunityPost().getPostId());
				obj.add("community_post", communityPost);
				break;
			}
			default: {
				JsonObject text = new JsonObject();
				text.addProperty("content", "[未知内容]");
				obj.add("text", text);
				break;
			}
		}
		
		return obj;
	}
	
}
