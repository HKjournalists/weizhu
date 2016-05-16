package com.weizhu.webapp.web.api.discover;

import com.beust.jcommander.internal.Lists;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.weizhu.common.db.DBUtil;
import com.weizhu.proto.DiscoverV2Protos;
import com.weizhu.proto.UserProtos;

import java.util.LinkedList;
import java.util.Map;

public class DiscoverUtil {

	public static JsonObject itemJson(DiscoverV2Protos.Item item,
									  Map<Long, UserProtos.User> userMap,
									  Map<Integer, UserProtos.Team> userTeamMap,
									  Map<Integer, UserProtos.Position> userPositionMap,
									  String imagePrefix) {
		JsonObject obj = new JsonObject();
		obj.addProperty("item_id", item.getBase().getItemId());
		obj.addProperty("item_name", item.getBase().getItemName());

		String imageName = item.getBase().getImageName();
		obj.addProperty("item_image", imageName);
		obj.addProperty("item_url", imagePrefix + imageName);

		obj.addProperty("item_desc", item.getBase().getItemDesc());
		obj.addProperty("enable_comment", item.getBase().getEnableComment());
		obj.addProperty("enable_score", item.getBase().getEnableScore());
		obj.addProperty("enable_remind", item.getBase().getEnableRemind());
		obj.addProperty("enable_like", item.getBase().getEnableLike());
		obj.addProperty("enable_share", item.getBase().getEnableShare());
		obj.addProperty("enable_external_share", item.getBase().getEnableExternalShare());
		switch (item.getBase().getContentCase()) {
			case WEB_URL : 
				DiscoverV2Protos.WebUrl webUrl = item.getBase().getWebUrl();
				obj.addProperty("web_url", webUrl.getWebUrl());
				obj.addProperty("is_weizhu", webUrl.getIsWeizhu());
				break;
			case DOCUMENT : 
				DiscoverV2Protos.Document document = item.getBase().getDocument();
				obj.addProperty("document_url", document.getDocumentUrl());
				obj.addProperty("document_type", document.getDocumentType());
				obj.addProperty("document_size", document.getDocumentSize()); // 单位是Byte
				if (document.hasCheckMd5()) {
					obj.addProperty("check_md5", document.getCheckMd5());
				}
				obj.addProperty("is_download", document.getIsDownload()); // 是否可以离线下载
				obj.addProperty("is_auth_url", document.getIsAuthUrl()); // 是否需要授权访问
				break;
			case VIDEO :
				DiscoverV2Protos.Video video = item.getBase().getVideo();
				obj.addProperty("video_url", video.getVideoUrl());
				obj.addProperty("video_type", video.getVideoType());
				obj.addProperty("video_size", video.getVideoSize());
				obj.addProperty("video_time", video.getVideoTime());
				if (video.hasCheckMd5()) {
					obj.addProperty("check_md5", video.getCheckMd5());
				}
				obj.addProperty("is_download", video.getIsDownload()); // 是否可以离线下载
				obj.addProperty("is_auth_url", video.getIsAuthUrl()); // 是否需要授权访问
				break;
			case AUDIO :
				DiscoverV2Protos.Audio audio = item.getBase().getAudio();
				obj.addProperty("audio_url", audio.getAudioUrl());
				obj.addProperty("audio_type", audio.getAudioType());
				obj.addProperty("audio_size", audio.getAudioSize());
				obj.addProperty("audio_time", audio.getAudioTime());
				if (audio.hasCheckMd5()) {
					obj.addProperty("check_md5", audio.getCheckMd5());
				}
				obj.addProperty("is_download", audio.getIsDownload()); // 是否可以离线下载
				obj.addProperty("is_auth_url", audio.getIsAuthUrl()); // 是否需要授权访问
				break;
			case APP_URI :
				DiscoverV2Protos.AppUri appUri = item.getBase().getAppUri();
				obj.addProperty("app_uri", appUri.getAppUri());
				break;
			default:
				break;
		}
		obj.addProperty("learn_cnt", item.getCount().getLearnCnt());
		obj.addProperty("learn_user_cnt", item.getCount().getLearnUserCnt());
		obj.addProperty("comment_cnt", item.getCount().getCommentCnt());
		obj.addProperty("comment_user_cnt", item.getCount().getCommentUserCnt());
		obj.addProperty("score_number", item.getCount().getScoreNumber());
		obj.addProperty("score_user_cnt", item.getCount().getScoreUserCnt());
		obj.addProperty("like_cnt", item.getCount().getLikeCnt());
		obj.addProperty("share_cnt", item.getCount().getShareCnt());
		
		if (item.hasUser()) {
			DiscoverV2Protos.Item.User user = item.getUser();

			obj.add("user", userJson(user.getUserId(), userMap, userTeamMap, userPositionMap, imagePrefix));
			if (user.hasIsLearn()) {
				obj.addProperty("is_learn", user.getIsLearn());
			}
			if (user.hasIsComment()) {
				obj.addProperty("is_comment", user.getIsComment());
			}
			if (user.hasIsScore()) {
				obj.addProperty("is_score", user.getIsScore());
			}
			if (user.hasIsLike()) {
				obj.addProperty("is_like", user.getIsLike());
			}
			if (user.hasIsShare()) {
				obj.addProperty("is_share", user.getIsShare());
			}
		}
		
		return obj;
	}
	
	static JsonObject itemCommentJson(DiscoverV2Protos.ItemComment itemComment,
									  Map<Long, UserProtos.User> userMap,
									  Map<Integer, UserProtos.Team> userTeamMap,
									  Map<Integer, UserProtos.Position> userPositionMap,
									  String imagePrefix) {
		JsonObject obj = new JsonObject();
		obj.addProperty("comment_id", itemComment.getCommentId());
		obj.addProperty("item_id", itemComment.getItemId());
		obj.add("user", userJson(itemComment.getUserId(), userMap, userTeamMap, userPositionMap, imagePrefix));
		obj.addProperty("comment_time", itemComment.getCommentTime());
		obj.addProperty("comment_text", itemComment.getCommentText());
		obj.addProperty("is_delete", itemComment.getIsDelete());
		
		return obj;
	}
	
	static JsonObject itemLearnJson(DiscoverV2Protos.ItemLearn itemLearn,
									Map<Long, UserProtos.User> userMap,
									Map<Integer, UserProtos.Team> userTeamMap,
									Map<Integer, UserProtos.Position> userPositionMap,
									String imagePrefix) {
		JsonObject obj = new JsonObject();
		obj.addProperty("item_id", itemLearn.getItemId());
		obj.add("user", userJson(itemLearn.getUserId(), userMap, userTeamMap, userPositionMap, imagePrefix));
		obj.addProperty("learn_time", itemLearn.getLearnTime());
		obj.addProperty("learn_duration", itemLearn.getLearnDuration());
		obj.addProperty("learn_cnt", itemLearn.getLearnCnt());
		
		return obj;
	}
	
	static JsonObject itemLikeJson(DiscoverV2Protos.ItemLike itemLike,
								   Map<Long, UserProtos.User> userMap,
								   Map<Integer, UserProtos.Team> userTeamMap,
								   Map<Integer, UserProtos.Position> userPositionMap,
								   String imagePrefix) {
		JsonObject obj = new JsonObject();
		obj.addProperty("item_id", itemLike.getItemId());
		obj.add("user", userJson(itemLike.getUserId(), userMap, userTeamMap, userPositionMap, imagePrefix));
		obj.addProperty("like_time", itemLike.getLikeTime());
		
		return obj;
	}
	
	static JsonObject itemScoreJson(DiscoverV2Protos.ItemScore itemScore,
									Map<Long, UserProtos.User> userMap,
									Map<Integer, UserProtos.Team> userTeamMap,
									Map<Integer, UserProtos.Position> userPositionMap,
									String imagePrefix) {
		JsonObject obj = new JsonObject();
		obj.addProperty("item_id", itemScore.getItemId());
		obj.add("user", userJson(itemScore.getUserId(), userMap, userTeamMap, userPositionMap, imagePrefix));
		obj.addProperty("score_time", itemScore.getScoreTime());
		obj.addProperty("score_number", itemScore.getScoreNumber());
		
		return obj;
	}
	
	static JsonObject itemShareJson(DiscoverV2Protos.ItemShare itemShare,
									Map<Long, UserProtos.User> userMap,
									Map<Integer, UserProtos.Team> userTeamMap,
									Map<Integer, UserProtos.Position> userPositionMap,
									String imagePrefix) {
		JsonObject obj = new JsonObject();
		obj.addProperty("item_id", itemShare.getItemId());
		obj.add("user", userJson(itemShare.getUserId(), userMap, userTeamMap, userPositionMap, imagePrefix));
		obj.addProperty("score_time", itemShare.getShareTime());
		
		return obj;
	}

	private static JsonObject userJson(long userId,
							   Map<Long, UserProtos.User> userMap,
							   Map<Integer, UserProtos.Team> userTeamMap,
							   Map<Integer, UserProtos.Position> userPositionMap,
							   String imagePrefix) {
		JsonObject userObj = new JsonObject();

		UserProtos.User user = userMap.get(userId);
		if (user == null) {
			return userObj;
		}

		userObj.addProperty("user_name", user.getBase().getUserName());
		if (user.getBase().hasAvatar()) {
			String imageAvater = user.getBase().getAvatar();
			userObj.addProperty("user_image", imageAvater);
			userObj.addProperty("image_url", imagePrefix + imageAvater);
		}

		userObj.addProperty("mobile_no", DBUtil.COMMA_JOINER.join(user.getBase().getMobileNoList()));
		if (user.getTeamCount() > 0) {
			UserProtos.UserTeam userTeam = user.getTeam(0);

			JsonArray teamArray = new JsonArray();

			LinkedList<UserProtos.Team> teamList = Lists.newLinkedList();
			int tmpTeamId = userTeam.getTeamId();
			while (true) {
				UserProtos.Team team = userTeamMap.get(tmpTeamId);
				if (team == null) {
					// warn : cannot find team
					teamList.clear();
					break;
				}

				teamList.addFirst(team);

				if (team.hasParentTeamId()) {
					tmpTeamId = team.getParentTeamId();
				} else {
					break;
				}
			}

			for (UserProtos.Team team : teamList) {
				JsonObject teamObj = new JsonObject();
				teamObj.addProperty("team_name", team.getTeamName());
				teamArray.add(teamObj);
			}
			userObj.add("user_team", teamArray);

			if (userTeam.hasPositionId()) {
				UserProtos.Position position = userPositionMap.get(userTeam.getPositionId());
				userObj.addProperty("user_position", position == null ? "" : position.getPositionName());
			}
		}

		return userObj;
	}
}
