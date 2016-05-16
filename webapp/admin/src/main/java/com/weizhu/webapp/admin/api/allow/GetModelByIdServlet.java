package com.weizhu.webapp.admin.api.allow;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.common.util.concurrent.Futures;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.weizhu.common.db.DBUtil;
import com.weizhu.common.utils.JsonUtil;
import com.weizhu.proto.AdminProtos.AdminHead;
import com.weizhu.proto.AdminUserProtos.GetPositionResponse;
import com.weizhu.proto.AdminUserProtos.GetTeamByIdRequest;
import com.weizhu.proto.AdminUserProtos.GetTeamByIdResponse;
import com.weizhu.proto.AdminUserProtos.GetUserByIdRequest;
import com.weizhu.proto.AdminUserProtos.GetUserByIdResponse;
import com.weizhu.proto.AdminUserService;
import com.weizhu.proto.AllowProtos;
import com.weizhu.proto.AllowProtos.GetModelByIdRequest;
import com.weizhu.proto.AllowProtos.GetModelByIdResponse;
import com.weizhu.proto.AllowProtos.GetModelRuleListRequest;
import com.weizhu.proto.AllowService;
import com.weizhu.proto.UserProtos;
import com.weizhu.proto.UserProtos.Position;
import com.weizhu.proto.UserProtos.Team;
import com.weizhu.proto.UserProtos.User;
import com.weizhu.proto.WeizhuProtos.EmptyRequest;
import com.weizhu.web.ParamUtil;

@Singleton
public class GetModelByIdServlet extends HttpServlet {

	private static final long serialVersionUID = 1L;

	private final Provider<AdminHead> adminHeadProvider;
	private final AllowService allowService;
	private final AdminUserService adminUserService;
	
	@Inject
	public GetModelByIdServlet(Provider<AdminHead> adminHeadProvider, AllowService allowService, AdminUserService adminUserService) {
		this.adminHeadProvider = adminHeadProvider;
		this.allowService = allowService;
		this.adminUserService = adminUserService;
	}
	
	@Override
	public void doPost(HttpServletRequest httpRequest, HttpServletResponse httpResponse) throws IOException {
		final String modelIdStr = ParamUtil.getString(httpRequest, "model_id", "");
		List<Integer> modelIdList = new ArrayList<Integer>();
		try {
			List<String> modelIdStrList = DBUtil.COMMA_SPLITTER.splitToList(modelIdStr);
			for (String modelId : modelIdStrList) {
				modelIdList.add(Integer.parseInt(modelId));
			}
			
		} catch (Exception ex) {
			JsonObject result = new JsonObject();
			result.addProperty("result", "FAIL_MODEL_INVALID");
			result.addProperty("fail_text", "传入的模型不正确！");

			httpResponse.setContentType("application/json;charset=UTF-8");
			JsonUtil.GSON.toJson(result, httpResponse.getWriter());
			return ;
		}
		
		AdminHead head = this.adminHeadProvider.get();
		
		GetModelByIdRequest getModelByIdRequest = GetModelByIdRequest.newBuilder()
				.addAllModelId(modelIdList)
				.build();
		GetModelByIdResponse getModelByIdResponse = Futures.getUnchecked(allowService.getModelById(head, getModelByIdRequest));
		
		GetModelRuleListRequest.Builder getModelRuleListRequestBuilder = GetModelRuleListRequest.newBuilder();
		
		List<AllowProtos.Model> modelList = getModelByIdResponse.getModelList();
		List<AllowProtos.Rule> ruleList = null;
		Map<Integer, List<AllowProtos.Rule>> ruleMap = new HashMap<Integer, List<AllowProtos.Rule>>();
		
		List<Long> adminUserIdList = new ArrayList<Long>();
		for (AllowProtos.Model model : modelList) {
			getModelRuleListRequestBuilder.clear();
			getModelRuleListRequestBuilder.setModelId(model.getModelId());
			
			adminUserIdList.add(model.getCreateAdminId());
			ruleList = Futures.getUnchecked(allowService.getModelRuleList(head, getModelRuleListRequestBuilder.build())).getRuleList();
			ruleMap.put(model.getModelId(), ruleList);
		}
		
		List<Long> ruleUserIdList = new ArrayList<Long>(adminUserIdList);
		List<Integer> ruleTeamIdList = new ArrayList<Integer>();
		List<Integer> rulePositionIdList = new ArrayList<Integer>();
		for (List<AllowProtos.Rule> tmpRuleList : ruleMap.values()) {
			for (AllowProtos.Rule rule : tmpRuleList) {
				switch(rule.getRuleTypeCase()) {
					case USER_RULE:
						ruleUserIdList.addAll(rule.getUserRule().getUserIdList());
						break;
					case TEAM_RULE:
						ruleTeamIdList.addAll(rule.getTeamRule().getTeamIdList());
						break;
					case POSITION_RULE:
						rulePositionIdList.addAll(rule.getPositionRule().getPositionIdList());
						break;
					case RULETYPE_NOT_SET:
						break;
					default:
						break;
				}
			}
		}

		GetUserByIdRequest getUserByIdRequest = GetUserByIdRequest.newBuilder()
				.addAllUserId(ruleUserIdList)
				.build();
		GetUserByIdResponse getUserByIdResponse = Futures.getUnchecked(adminUserService.getUserById(head, getUserByIdRequest));
		Map<Long, User> userMap = new HashMap<Long, User>();
		for (User user : getUserByIdResponse.getUserList()) {
			userMap.put(user.getBase().getUserId(), user);
		}
		
		GetPositionResponse getPositionResponse = Futures.getUnchecked(adminUserService.getPosition(head, EmptyRequest.newBuilder().build()));
		Map<Integer, Position> positionMap = new HashMap<Integer, Position>();
		for (Position position : getPositionResponse.getPositionList()) {
			positionMap.put(position.getPositionId(), position);
		}
		
		GetTeamByIdResponse getTeamByIdResponse = Futures.getUnchecked(adminUserService.getTeamById(head, GetTeamByIdRequest.newBuilder()
				.addAllTeamId(ruleTeamIdList)
				.build()));
		Map<Integer, Team> teamMap = new HashMap<Integer, Team>();
		for (Team team : getTeamByIdResponse.getTeamList()) {
			teamMap.put(team.getTeamId(), team);
		}
		// 再加入规则里面的用户的部门信息
		for (int i = 0; i < getUserByIdResponse.getRefTeamCount(); ++i) {
			UserProtos.Team team = getUserByIdResponse.getRefTeam(i);
			teamMap.put(team.getTeamId(), team);
		}
		
		JsonArray modelArray = new JsonArray();
		for (AllowProtos.Model model : modelList) {
			JsonObject modelJson = new JsonObject();
			modelJson.addProperty("model_id", model.getModelId());
			modelJson.addProperty("model_name", model.getModelName());
			modelJson.addProperty("default_action", model.getDefaultAction().name());
			JsonArray ruleArray = new JsonArray();
			for (AllowProtos.Rule rule : ruleList) {
				JsonObject ruleJson = new JsonObject();
				
				ruleJson.addProperty("rule_id", rule.getRuleId());
				ruleJson.addProperty("rule_name", rule.getRuleName());
				ruleJson.addProperty("rule_type", rule.getRuleTypeCase().name());
				ruleJson.addProperty("rule_action", rule.getAction().name());
				
				JsonArray ruleData = new JsonArray();
				switch(rule.getRuleTypeCase()) {
					case USER_RULE:
						for (long userId : rule.getUserRule().getUserIdList()) {
							JsonObject tmpRule = new JsonObject();
							tmpRule.addProperty("user_id", userId);
							User user = userMap.get(userId);
							if (user == null) {
								tmpRule.addProperty("user_name", "");
								tmpRule.addProperty("mobile_no", "");
								tmpRule.addProperty("team", "");
								tmpRule.addProperty("position", "");
							} else {
								tmpRule.addProperty("user_name", user == null ? "" : user.getBase().getUserName());
								tmpRule.addProperty("mobile_no", DBUtil.COMMA_JOINER.join(user.getBase().getMobileNoList()));
								AllowUtil.getUserTeamPosition(tmpRule, user, teamMap, positionMap);
							}
							
							ruleData.add(tmpRule);
						}
						break;
					case TEAM_RULE:
						for (int teamId : rule.getTeamRule().getTeamIdList()) {
							JsonObject tmpRule = new JsonObject();
							tmpRule.addProperty("team_id", teamId);
							Team team = teamMap.get(teamId);
							tmpRule.addProperty("team_name", team == null ? "" : team.getTeamName());
							
							ruleData.add(tmpRule);
						}
						break;
					case POSITION_RULE:
						for (int positionId : rule.getPositionRule().getPositionIdList()) {
							JsonObject tmpRule = new JsonObject();
							tmpRule.addProperty("position_id", positionId);
							Position position = positionMap.get(positionId);
							tmpRule.addProperty("position_name", position == null ? "" : position.getPositionName());
							
							ruleData.add(tmpRule);
						}
						break;
					case RULETYPE_NOT_SET:
						break;
					default:
						break;
				}
				ruleJson.add("rule_data", ruleData);
				
				ruleArray.add(ruleJson);
			}
			modelJson.add("rule_list", ruleArray);
			modelArray.add(modelJson);
		}
		
		JsonObject result = new JsonObject();
		result.addProperty("result", "SUCC");
		result.add("model", modelArray);

		httpResponse.setContentType("application/json;charset=UTF-8");
		JsonUtil.GSON.toJson(result, httpResponse.getWriter());
	}
	
	@Override
	public void doGet(HttpServletRequest httpRequest, HttpServletResponse httpResponse) throws IOException {
		this.doPost(httpRequest, httpResponse);
	}
}
