package com.weizhu.webapp.mobile.qa;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.weizhu.common.db.DBUtil;
import com.weizhu.proto.QAProtos;
import com.weizhu.proto.QAProtos.Answer;
import com.weizhu.proto.QAProtos.Category;
import com.weizhu.proto.QAProtos.Question;
import com.weizhu.proto.UserProtos;
import com.weizhu.proto.UserProtos.GetUserResponse;
import com.weizhu.proto.UserProtos.UserBase;
import com.weizhu.proto.UserProtos.UserTeam;

public class QAServletUtil {
	public static final String ANONYMOUS_USER = "匿名用户";
	public static final String SYSTEM_USER = "官方用户";

	public static Map<Long, JsonObject> getUserJson(GetUserResponse userResponse, String imageUrlPrefix) {
		Map<Long, JsonObject> userJsonMap = new HashMap<Long, JsonObject>();
		for (UserProtos.User user : userResponse.getUserList()) {
			JsonObject u = new JsonObject();
			u.addProperty("user_id", user.getBase().getUserId());
			u.addProperty("user_name", user.getBase().getUserName());
			if(user.getBase().getGender().equals(UserBase.Gender.MALE)){
				u.addProperty("gender","男");
			}else{
				u.addProperty("gender", "女");
			}		
			u.addProperty("avatar", imageUrlPrefix + user.getBase().getAvatar());
			u.addProperty("mobile_no", DBUtil.COMMA_JOINER.join(user.getBase().getMobileNoList()));
			u.addProperty("phone_no", DBUtil.COMMA_JOINER.join(user.getBase().getPhoneNoList()));
			u.addProperty("email", user.getBase().getEmail());
			u.add("team_position", getTeamJson(userResponse.getRefTeamList(), userResponse.getRefPositionList(), user.getTeamList()));
			userJsonMap.put(user.getBase().getUserId(), u);
		}
		return userJsonMap;
	}

	public static JsonObject getUserJson(boolean isAdmin, long userId, Map<Long, JsonObject> userJsonMap) {
		JsonObject u = userJsonMap.get(userId);
		if (isAdmin || u == null) {
			u = new JsonObject();
			u.addProperty("user_id", "");
			u.addProperty("avatar", "");
			u.addProperty("mobile_no", "");
			u.addProperty("phone_no", "");
			u.addProperty("email", "");
			u.addProperty("team_position", "");
			if (isAdmin) {
				u.addProperty("user_name", QAServletUtil.SYSTEM_USER);
			} else {
				u.addProperty("user_name", QAServletUtil.ANONYMOUS_USER);
			}
		}
		return u;
	}

	private static JsonArray getTeamJson(List<UserProtos.Team> refTeams, List<UserProtos.Position> refRositions, List<UserTeam> userTeamList) {
		Map<Integer,UserProtos.Team> teamMap=new HashMap<Integer,UserProtos.Team>();
		for(UserProtos.Team team:refTeams){
			teamMap.put(team.getTeamId(), team);
		}
		JsonArray userTeamArray = new JsonArray();
		for (UserProtos.UserTeam userTeamTemp : userTeamList) {
			JsonObject userTeam = new JsonObject();
			JsonArray teamArray = new JsonArray();
			JsonObject position = getPositionJson(refRositions, userTeamTemp.getPositionId());
			setTeamJson(teamArray,teamMap, userTeamTemp.getTeamId());
			if (teamArray != null) {
				userTeam.add("team", teamArray);
			}
			if (position != null) {
				userTeam.add("position", position);
			}
			if (userTeamArray != null) {
				userTeamArray.add(userTeam);
			}
		}
		return userTeamArray;
	}

	private static JsonObject getPositionJson(List<UserProtos.Position> refPositions, int positionId) {
		UserProtos.Position position = null;
		for (UserProtos.Position positionTemp : refPositions) {
			if (positionId == positionTemp.getPositionId()) {
				position = positionTemp;
			}
		}
		if (position == null) {
			return null;
		}
		JsonObject u = new JsonObject();
		u.addProperty("position_id", position.getPositionId());
		u.addProperty("position_name", position.getPositionName());
		u.addProperty("position_desc", position.getPositionDesc());
		return u;
	}

	private static void setTeamJson(JsonArray teamArray,Map<Integer,UserProtos.Team> teamMap, int teamId) {
		UserProtos.Team team = teamMap.get(teamId);
		if (team == null) {
			return ;
		}
		JsonObject u = new JsonObject();
		u.addProperty("team_id", team.getTeamId());
		u.addProperty("team_name", team.getTeamName());
		u.addProperty("parent_team_id", team.getParentTeamId());
		//递归调用
		if(team.hasParentTeamId()){
			setTeamJson(teamArray,teamMap, team.getParentTeamId());
		}		
		teamArray.add(u);
	}

	public static String getCategoryName(List<QAProtos.Category> categorys, int categoryId) {
		//获取问题列表对应的分类信息
		Map<Integer, QAProtos.Category> categoryMap = new HashMap<Integer, QAProtos.Category>();
		for (QAProtos.Category category : categorys) {
			categoryMap.put(category.getCategoryId(), category);
		}
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

	public static Map<Integer, JsonObject> getAnswerJsonMap(List<Answer> refAnswerList, Map<Long, JsonObject> userJsonMap) {
		Map<Integer, JsonObject> answerJsonMap = new HashMap<Integer, JsonObject>();
		for (QAProtos.Answer answer : refAnswerList) {
			answerJsonMap.put(answer.getAnswerId(), getAnswerJson(answer, userJsonMap));
		}
		return answerJsonMap;
	}

	public static JsonObject getAnswerJson(Answer answer, Map<Long, JsonObject> userJsonMap) {
		long userId = answer.hasUserId() ? answer.getUserId() : answer.getAdminId();
		boolean isAdmin = !answer.hasUserId();
		JsonObject u = new JsonObject();
		u.addProperty("answer_id", answer.getAnswerId());
		u.addProperty("question_id", answer.getQuestionId());
		u.addProperty("user_id", userId);
		u.addProperty("answer_content", answer.getAnswerContent());
		u.addProperty("like_num", answer.getLikeNum());
		u.addProperty("create_time", answer.getCreateTime());
		u.addProperty("is_like", answer.getIsLike());
		u.addProperty("can_delete", answer.getCanDelete());
		u.add("user", QAServletUtil.getUserJson(isAdmin, userId, userJsonMap));
		return u;
	}

	public static JsonObject getQuestionJson(Question question, Map<Long, JsonObject> userJsonMap, Map<Integer, JsonObject> answerInfoMap) {
		long userId = question.hasUserId() ? question.getUserId() : question.getAdminId();
		boolean isAdmin = !question.hasUserId();
		JsonObject u = new JsonObject();
		u.addProperty("question_id", question.getQuestionId());
		u.addProperty("question_content", question.getQuestionContent());
		u.addProperty("user_id", userId);
		u.addProperty("answer_num", question.getAnswerNum());
		u.addProperty("category_id", question.getCategoryId());
		u.addProperty("create_time", question.getCreateTime());
		u.addProperty("best_answer_id", question.getBestAnswerId());
		u.addProperty("can_delete", question.getCanDelete());
		u.add("user", QAServletUtil.getUserJson(isAdmin, userId, userJsonMap));
		u.add("best_answer", answerInfoMap.get(question.getBestAnswerId()));
		return u;
	}

	public static JsonObject getCategoryJson(Category category, Map<Long, JsonObject> userJsonMap) {
		long userId = category.hasUserId() ? category.getUserId() : category.getAdminId();
		boolean isAdmin = !category.hasUserId();
		JsonObject u = new JsonObject();
		u.addProperty("category_id", category.getCategoryId());
		u.addProperty("category_name", category.getCategoryName());
		u.addProperty("user_id", userId);
		u.addProperty("question_num", category.getQuestionNum());
		u.addProperty("create_time", category.getCreateTime());
		u.add("user", QAServletUtil.getUserJson(isAdmin, userId, userJsonMap));
		return u;
	}

	public static Map<Integer, List<Question>> getCategoryQuestionMap(List<Category> refCategoryList, List<Question> questionList) {
		Map<Integer, List<Question>> cateQuesMap = new HashMap<Integer, List<Question>>();
		for (Question question : questionList) {
			int categoryId = question.getCategoryId();
			List<Question> questions = null;
			if (cateQuesMap.containsKey(categoryId)) {
				questions = cateQuesMap.get(categoryId);

			} else {
				questions = new ArrayList<Question>();
			}
			questions.add(question);
			cateQuesMap.put(categoryId, questions);
		}

		return cateQuesMap;
	}
}
