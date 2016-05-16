package com.weizhu.webapp.admin.api.survey;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.weizhu.common.db.DBUtil;
import com.weizhu.proto.AdminProtos;
import com.weizhu.proto.AllowProtos;
import com.weizhu.proto.SurveyProtos;
import com.weizhu.proto.UserProtos;

public class SurveyUtil {

	public static JsonObject buildSurveyJsonObject(SurveyProtos.Survey survey, 
			Map<Integer, AllowProtos.Model> modelMap,
			Map<Long, AdminProtos.Admin> adminMap,
			String imageUrlPrefix) {
		JsonObject obj = new JsonObject();
		
		obj.addProperty("survey_id", survey.getSurveyId());
		obj.addProperty("survey_name", survey.getSurveyName());
		obj.addProperty("survey_desc", survey.getSurveyDesc());
		obj.addProperty("image_name", survey.getImageName());
		obj.addProperty("image_url",  imageUrlPrefix + survey.getImageName());
		obj.addProperty("start_time", survey.getStartTime());
		obj.addProperty("end_time", survey.getEndTime());
		obj.addProperty("show_result_type", survey.getShowResultType().name());
		if (survey.hasAllowModelId()) {
			AllowProtos.Model model = modelMap.get(survey.getAllowModelId());
			if (model != null) {
				obj.addProperty("allow_model_id", model.getModelId());
				obj.addProperty("allow_model_name", model.getModelName());
			}
		}
		obj.addProperty("survey_user_cnt", survey.getSurveyUserCnt());
		obj.addProperty("state", survey.getState().name());
		
		if (survey.hasCreateTime()){
			obj.addProperty("create_time", survey.getCreateTime());
		}
		
		AdminProtos.Admin admin = null;
		if (survey.hasCreateAdminId()) {
			admin = adminMap.get(survey.getCreateAdminId());
			obj.addProperty("create_admin_name", admin == null ? "未知[AdminId:" + survey.getCreateAdminId() + "]" : admin.getAdminName());
		}
		
		if (survey.hasUpdateTime()) {
			obj.addProperty("update_time", survey.getUpdateTime());
		}
		
		if (survey.hasUpdateAdminId()) {
			admin = adminMap.get(survey.getUpdateAdminId());
			obj.addProperty("update_admin_id", admin == null ? "未知[AdminId:" + survey.getUpdateAdminId() + "]": admin.getAdminName());
		}
		
		return obj;
	}
	
	public static JsonObject buildQuestionJsonObject(List<SurveyProtos.Question> questionList, 
			Map<Long, AdminProtos.Admin> adminMap,
			String imageUrlPrefix) {
		if (questionList.isEmpty()) {
			return new JsonObject();
		}
		
		JsonArray questions = new JsonArray();
		for (SurveyProtos.Question question : questionList) {
			JsonObject obj = new JsonObject();
			
			obj.addProperty("question_id", question.getQuestionId());
			obj.addProperty("question_name", question.getQuestionName());
			obj.addProperty("image_name", question.getImageName());
			obj.addProperty("image_url",  imageUrlPrefix + question.getImageName());
			obj.addProperty("is_optional", question.getIsOptional());
			obj.addProperty("state", question.getState().name());
			obj.addProperty("type", question.getTypeCase().name());
			
			if (question.hasCreateTime()){
				obj.addProperty("create_time", question.getCreateTime());
			}
			
			AdminProtos.Admin admin = null;
			if (question.hasCreateAdminId()) {
				admin = adminMap.get(question.getCreateAdminId());
				obj.addProperty("create_admin_name", admin == null ? "未知[AdminId:" + question.getCreateAdminId() + "]" : admin.getAdminName());
			}
			
			if (question.hasUpdateTime()) {
				obj.addProperty("update_time", question.getUpdateTime());
			}
			
			if (question.hasUpdateAdminId()) {
				admin = adminMap.get(question.getUpdateAdminId());
				obj.addProperty("update_admin_id", admin == null ? "未知[AdminId:" + question.getUpdateAdminId() + "]": admin.getAdminName());
			}
			
			if (question.getTypeCase().equals(SurveyProtos.Question.TypeCase.VOTE)) {
				obj.addProperty("check_num", question.getVote().getCheckNum());
				
				if (question.getVote().hasTotalCnt()) {
					obj.addProperty("total_cnt", question.getVote().getTotalCnt());
				}
				
				JsonArray optionArray = new JsonArray();
				for (SurveyProtos.Vote.Option option : question.getVote().getOptionList()) {
					JsonObject optionJson = new JsonObject();
					
					optionJson.addProperty("option_id", option.getOptionId());
					optionJson.addProperty("option_name", option.getOptionName());
					
					optionJson.addProperty("image_name", option.getImageName());
					optionJson.addProperty("image_url", imageUrlPrefix + option.getImageName());
					
					if (option.hasOptionCnt()) {
						optionJson.addProperty("option_cnt", option.getOptionCnt());
					}
					
					optionArray.add(optionJson);
				}
				
				obj.add("options", optionArray);
			} else if (question.getTypeCase().equals(SurveyProtos.Question.TypeCase.INPUT_SELECT)) {
				JsonArray optionArray = new JsonArray();
				for (SurveyProtos.InputSelect.Option option : question.getInputSelect().getOptionList()) {
					JsonObject optionJson = new JsonObject();
					
					optionJson.addProperty("option_id", option.getOptionId());
					optionJson.addProperty("option_name", option.getOptionName());
					
					optionArray.add(optionJson);
				}
				
				obj.add("options", optionArray);
			} else if (question.getTypeCase().equals(SurveyProtos.Question.TypeCase.INPUT_TEXT)) {
				obj.addProperty("input_prompt", question.getInputText().getInputPrompt());
			}
			
			questions.add(obj);
		}
		
		JsonObject result = new JsonObject();
		result.add("questions", questions);
		return result;
	}
	
	public static void getUserTeamPosition(JsonObject obj, UserProtos.User user, Map<Integer, UserProtos.Team> teamMap, Map<Integer, UserProtos.Position> positionMap) {
		obj.addProperty("user_name", user.getBase().getUserName());
		obj.addProperty("mobile_no", DBUtil.COMMA_JOINER.join(user.getBase().getMobileNoList()));
		if (user.getTeamCount() > 0) {
			UserProtos.UserTeam userTeam = user.getTeam(0);

			JsonArray teamArray = new JsonArray();

			LinkedList<UserProtos.Team> teamList = new LinkedList<UserProtos.Team>();
			int tmpTeamId = userTeam.getTeamId();
			while (true) {
				UserProtos.Team team = teamMap.get(tmpTeamId);
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
			obj.add("user_team", teamArray);
			if (userTeam.hasPositionId()) {
				UserProtos.Position position = positionMap.get(userTeam.getPositionId());
				obj.addProperty("user_position", position == null ? "" : position.getPositionName());
			}
		} else {
			obj.addProperty("user_team", "");
			obj.addProperty("user_position", "");
		}
		
	}
}
