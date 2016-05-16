package com.weizhu.service.allow;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.inject.Inject;
import com.weizhu.common.db.DBUtil;
import com.weizhu.common.utils.DataPage;
import com.weizhu.proto.AdminProtos.AdminHead;
import com.weizhu.proto.AdminUserProtos;
import com.weizhu.proto.AdminUserService;
import com.weizhu.proto.AllowProtos;
import com.weizhu.proto.AllowProtos.Action;
import com.weizhu.proto.AllowProtos.CheckAllowRequest;
import com.weizhu.proto.AllowProtos.CheckAllowResponse;
import com.weizhu.proto.AllowProtos.CopyModelRequest;
import com.weizhu.proto.AllowProtos.CopyModelResponse;
import com.weizhu.proto.AllowProtos.CreateLevelRuleRequest;
import com.weizhu.proto.AllowProtos.CreateLevelRuleResponse;
import com.weizhu.proto.AllowProtos.CreateModelRequest;
import com.weizhu.proto.AllowProtos.CreateModelResponse;
import com.weizhu.proto.AllowProtos.CreatePositionRuleRequest;
import com.weizhu.proto.AllowProtos.CreatePositionRuleResponse;
import com.weizhu.proto.AllowProtos.CreateTeamRuleRequest;
import com.weizhu.proto.AllowProtos.CreateTeamRuleResponse;
import com.weizhu.proto.AllowProtos.CreateUserRuleRequest;
import com.weizhu.proto.AllowProtos.CreateUserRuleResponse;
import com.weizhu.proto.AllowProtos.DeleteModelRequest;
import com.weizhu.proto.AllowProtos.DeleteModelResponse;
import com.weizhu.proto.AllowProtos.DeleteRuleRequest;
import com.weizhu.proto.AllowProtos.DeleteRuleResponse;
import com.weizhu.proto.AllowProtos.GetModelByIdRequest;
import com.weizhu.proto.AllowProtos.GetModelByIdResponse;
import com.weizhu.proto.AllowProtos.GetModelListRequest;
import com.weizhu.proto.AllowProtos.GetModelListResponse;
import com.weizhu.proto.AllowProtos.GetModelRuleListRequest;
import com.weizhu.proto.AllowProtos.GetModelRuleListResponse;
import com.weizhu.proto.AllowProtos.UpdateLevelRuleRequest;
import com.weizhu.proto.AllowProtos.UpdateLevelRuleResponse;
import com.weizhu.proto.AllowProtos.UpdateModelRequest;
import com.weizhu.proto.AllowProtos.UpdateModelResponse;
import com.weizhu.proto.AllowProtos.UpdateModelRuleOrderRequest;
import com.weizhu.proto.AllowProtos.UpdateModelRuleOrderResponse;
import com.weizhu.proto.AllowProtos.UpdatePositionRuleRequest;
import com.weizhu.proto.AllowProtos.UpdatePositionRuleResponse;
import com.weizhu.proto.AllowProtos.UpdateTeamRuleRequest;
import com.weizhu.proto.AllowProtos.UpdateTeamRuleResponse;
import com.weizhu.proto.AllowProtos.UpdateUserRuleRequest;
import com.weizhu.proto.AllowProtos.UpdateUserRuleResponse;
import com.weizhu.proto.AllowService;
import com.weizhu.proto.UserProtos;
import com.weizhu.proto.UserService;
import com.weizhu.proto.WeizhuProtos.RequestHead;
import com.weizhu.proto.WeizhuProtos.SystemHead;
import com.weizhu.service.allow.AllowDAOProtos.ModelRule;
import com.weizhu.service.allow.rule.AbstractRule;
import com.zaxxer.hikari.HikariDataSource;

public class AllowServiceImpl implements AllowService {
	
	private static final Logger logger = LoggerFactory.getLogger(AllowServiceImpl.class);
	
	private final HikariDataSource hikariDataSource;
	private final JedisPool jedisPool;
	
	private final UserService userService;
	private final AdminUserService adminUserService;
	
	@Inject
	public AllowServiceImpl(HikariDataSource hikariDataSource, JedisPool jedisPool, 
			UserService userService, AdminUserService adminUserService
			) {
		this.hikariDataSource = hikariDataSource;
		this.jedisPool = jedisPool;
		this.userService = userService;
		this.adminUserService = adminUserService;
	}
	
	private static final ListenableFuture<CheckAllowResponse> EMPTY_CHECK_ALLOW_RESPONSE_IMMEDIATE_FUTURE = 
			Futures.immediateFuture(CheckAllowResponse.newBuilder().build());

	@Override
	public ListenableFuture<CheckAllowResponse> checkAllow(AdminHead head, CheckAllowRequest request) {
		if (!head.hasCompanyId() || request.getModelIdCount() <= 0 || request.getUserIdCount() <= 0) {
			return EMPTY_CHECK_ALLOW_RESPONSE_IMMEDIATE_FUTURE;
		}
		
		AdminUserProtos.GetUserByIdResponse getUserResponse = Futures.getUnchecked(
				this.adminUserService.getUserById(head, AdminUserProtos.GetUserByIdRequest.newBuilder()
				.addAllUserId(request.getUserIdList())
				.build()));
		
		if (getUserResponse.getUserCount() <= 0) {
			return EMPTY_CHECK_ALLOW_RESPONSE_IMMEDIATE_FUTURE;
		}
		
		Map<Long, UserProtos.User> userMap = new TreeMap<Long, UserProtos.User>();
		for (UserProtos.User user : getUserResponse.getUserList()) {
			userMap.put(user.getBase().getUserId(), user);
		}
		
		Map<Integer, UserProtos.Team> refTeamMap = new TreeMap<Integer, UserProtos.Team>();
		for (UserProtos.Team team : getUserResponse.getRefTeamList()) {
			refTeamMap.put(team.getTeamId(), team);
		}
		
		Map<Integer, UserProtos.Position> refPositionMap = new TreeMap<Integer, UserProtos.Position>();
		for (UserProtos.Position position : getUserResponse.getRefPositionList()) {
			refPositionMap.put(position.getPositionId(), position);
		}
		
		return this.doCheckAllow(head.getCompanyId(), request.getModelIdList(), userMap, refTeamMap, refPositionMap);
	}
	

	@Override
	public ListenableFuture<CheckAllowResponse> checkAllow(SystemHead head, CheckAllowRequest request) {
		if (!head.hasCompanyId() || request.getModelIdCount() <= 0 || request.getUserIdCount() <= 0) {
			return EMPTY_CHECK_ALLOW_RESPONSE_IMMEDIATE_FUTURE;
		}
		
		AdminUserProtos.GetUserByIdResponse getUserResponse = Futures.getUnchecked(
				this.adminUserService.getUserById(head, AdminUserProtos.GetUserByIdRequest.newBuilder()
				.addAllUserId(request.getUserIdList())
				.build()));
		
		if (getUserResponse.getUserCount() <= 0) {
			return EMPTY_CHECK_ALLOW_RESPONSE_IMMEDIATE_FUTURE;
		}
		
		Map<Long, UserProtos.User> userMap = new TreeMap<Long, UserProtos.User>();
		for (UserProtos.User user : getUserResponse.getUserList()) {
			userMap.put(user.getBase().getUserId(), user);
		}
		
		Map<Integer, UserProtos.Team> refTeamMap = new TreeMap<Integer, UserProtos.Team>();
		for (UserProtos.Team team : getUserResponse.getRefTeamList()) {
			refTeamMap.put(team.getTeamId(), team);
		}
		
		Map<Integer, UserProtos.Position> refPositionMap = new TreeMap<Integer, UserProtos.Position>();
		for (UserProtos.Position position : getUserResponse.getRefPositionList()) {
			refPositionMap.put(position.getPositionId(), position);
		}
		
		return this.doCheckAllow(head.getCompanyId(), request.getModelIdList(), userMap, refTeamMap, refPositionMap);
	}

	@Override
	public ListenableFuture<CheckAllowResponse> checkAllow(RequestHead head, CheckAllowRequest request) {
		if (request.getModelIdCount() <= 0 || request.getUserIdCount() <= 0) {
			return EMPTY_CHECK_ALLOW_RESPONSE_IMMEDIATE_FUTURE;
		}
		
		UserProtos.GetUserResponse getUserResponse = Futures.getUnchecked(
				this.userService.getUserById(head, UserProtos.GetUserByIdRequest.newBuilder()
				.addAllUserId(request.getUserIdList())
				.build()));
		
		if (getUserResponse.getUserCount() <= 0) {
			return EMPTY_CHECK_ALLOW_RESPONSE_IMMEDIATE_FUTURE;
		}
		
		Map<Long, UserProtos.User> userMap = new TreeMap<Long, UserProtos.User>();
		for (UserProtos.User user : getUserResponse.getUserList()) {
			userMap.put(user.getBase().getUserId(), user);
		}
		
		Map<Integer, UserProtos.Team> refTeamMap = new TreeMap<Integer, UserProtos.Team>();
		for (UserProtos.Team team : getUserResponse.getRefTeamList()) {
			refTeamMap.put(team.getTeamId(), team);
		}
		
		Map<Integer, UserProtos.Position> refPositionMap = new TreeMap<Integer, UserProtos.Position>();
		for (UserProtos.Position position : getUserResponse.getRefPositionList()) {
			refPositionMap.put(position.getPositionId(), position);
		}
		
		final long companyId = head.getSession().getCompanyId();
		
		return this.doCheckAllow(companyId, request.getModelIdList(), userMap, refTeamMap, refPositionMap);
	}
	
	private ListenableFuture<CheckAllowResponse> doCheckAllow(
			long companyId,
			List<Integer> modelIdList,
			Map<Long, UserProtos.User> userMap, 
			Map<Integer, UserProtos.Team> refTeamMap,
			Map<Integer, UserProtos.Position> refPositionMap
			) {
		
		// fail fast
		if (modelIdList.isEmpty() || userMap.isEmpty()) {
			return EMPTY_CHECK_ALLOW_RESPONSE_IMMEDIATE_FUTURE;
		}
		
		Map<Integer, AllowDAOProtos.ModelRule> modelRuleMap = this.doGetModelRule(companyId, modelIdList);
		if (modelRuleMap.isEmpty()) {
			return EMPTY_CHECK_ALLOW_RESPONSE_IMMEDIATE_FUTURE;
		}
		
		CheckAllowResponse.Builder responseBuilder = CheckAllowResponse.newBuilder();
		CheckAllowResponse.CheckResult.Builder tmpCheckResultBuilder = CheckAllowResponse.CheckResult.newBuilder();
		
		for (Integer modelId : modelIdList) {
			AllowDAOProtos.ModelRule modelRule = modelRuleMap.get(modelId);
			if (modelRule == null) {
				continue;
			}

			if (modelRule.getRuleCount() <= 0) {
				if (modelRule.getModel().getDefaultAction() == AllowProtos.Action.ALLOW) {
					responseBuilder.addCheckResult(tmpCheckResultBuilder.clear()
							.setModelId(modelRule.getModel().getModelId())
							.addAllAllowUserId(userMap.keySet())
							.build());
				}
			} else {
				tmpCheckResultBuilder.clear();
				tmpCheckResultBuilder.setModelId(modelRule.getModel().getModelId());
				
				final List<RuleHolder> holderList = new ArrayList<RuleHolder>(modelRule.getRuleCount());
				for (AllowProtos.Rule rule : modelRule.getRuleList()) {
					AbstractRule r = AbstractRule.create(rule);
					if (r != null) {
						holderList.add(new RuleHolder(r, rule.getAction()));
					}
				}
				
				for (UserProtos.User user : userMap.values()) {
					AllowProtos.Action action = null;
					for (RuleHolder holder : holderList) {
						if (holder.rule.match(user, refTeamMap, refPositionMap)) {
							action = holder.action;
							break;
						}
					}
					if (action == null) {
						action = modelRule.getModel().getDefaultAction();
					}
					
					if (action == AllowProtos.Action.ALLOW) {
						tmpCheckResultBuilder.addAllowUserId(user.getBase().getUserId());
					}
				}
				
				if (tmpCheckResultBuilder.getAllowUserIdCount() > 0) {
					responseBuilder.addCheckResult(tmpCheckResultBuilder.build());
				}
			}
		}

		return Futures.immediateFuture(responseBuilder.build());
	}
	
	private static final class RuleHolder {
		final AbstractRule rule;
		final AllowProtos.Action action;
		RuleHolder(AbstractRule rule, Action action) {
			this.rule = rule;
			this.action = action;
		}
	}
	
	private Map<Integer, AllowDAOProtos.ModelRule> doGetModelRule(long companyId, Collection<Integer> modelIds) {
		if (modelIds.isEmpty()) {
			return Collections.emptyMap();
		}
		
		Map<Integer, AllowDAOProtos.ModelRule> resultMap = new HashMap<Integer, AllowDAOProtos.ModelRule>();
		
		Set<Integer> noCacheModelIdSet = new TreeSet<Integer>();
		Jedis jedis = jedisPool.getResource();
		
		try {
			resultMap.putAll(AllowCache.getModelRule(jedis, companyId, modelIds, noCacheModelIdSet));
		} finally {
			jedis.close();
		}
		
		if (noCacheModelIdSet.isEmpty()) {
			return resultMap;
		}

		Map<Integer, AllowDAOProtos.ModelRule> noCacheResultMap;
		Connection conn = null;
		try {
			conn = hikariDataSource.getConnection();
			
			noCacheResultMap = AllowDB.getModelRule(conn, companyId, noCacheModelIdSet);
		} catch (SQLException ex) {
			throw new RuntimeException("db fail！", ex);
		} finally {
			DBUtil.closeQuietly(conn);
		}
		
		jedis = jedisPool.getResource();
		try {
			AllowCache.setModelRule(jedis, companyId, noCacheModelIdSet, noCacheResultMap);
		} finally {
			jedis.close();
		}
		
		resultMap.putAll(noCacheResultMap);
		
		return resultMap;
	}
	
	private Map<Integer, AllowProtos.Model> doGetModel(long companyId, Collection<Integer> modelIds) {
		if (modelIds.isEmpty()) {
			return Collections.emptyMap();
		}
		
		Map<Integer, AllowProtos.Model> resultMap = new HashMap<Integer, AllowProtos.Model>();
		
		Connection conn = null;
		try {
			conn = hikariDataSource.getConnection();
			
			resultMap = AllowDB.getModel(conn, companyId, modelIds);
		} catch (SQLException ex) {
			throw new RuntimeException("db fail！", ex);
		} finally {
			DBUtil.closeQuietly(conn);
		}
		
		return resultMap;
	}

	@Override
	public ListenableFuture<GetModelListResponse> getModelList(AdminHead head, GetModelListRequest request) {
		if (!head.hasCompanyId()) {
			return Futures.immediateFuture(GetModelListResponse.newBuilder()
					.setTotalSize(0)
					.setFilteredSize(0)
					.build());
		}
		
		final long companyId = head.getCompanyId();
		final int start = request.getStart() < 0 ? 0 : request.getStart();
		final int length = request.getLength() < 0 ? 0 : request.getLength() > 100 ? 100 : request.getLength();
		
		final String keyword = request.hasKeyword() ? request.getKeyword() : "";
		
		Connection conn = null;
		try {
			conn = this.hikariDataSource.getConnection();
			
			DataPage<Integer> modelIdPage = AllowDB.getModelIdPage(conn, companyId, start, length, keyword);
			
			List<Integer> modelIdList = modelIdPage.dataList();
			Map<Integer, AllowProtos.Model> modelMap = this.doGetModel(companyId, modelIdList);
			
			List<AllowProtos.Model> modelList = new ArrayList<AllowProtos.Model>();
			for (int modelId : modelIdList) {
				AllowProtos.Model model = modelMap.get(modelId);
				if (model != null) {
					modelList.add(model);
				}
			}
			
			int total = modelIdPage.totalSize();
			
			return Futures.immediateFuture(GetModelListResponse.newBuilder()
					.addAllModel(modelList)
					.setTotalSize(total)
					.setFilteredSize(total)
					.build());
		} catch (SQLException ex) {
			throw new RuntimeException("db fail", ex);
		} finally {
			DBUtil.closeQuietly(conn);
		}
	}

	@Override
	public ListenableFuture<GetModelByIdResponse> getModelById(AdminHead head, GetModelByIdRequest request) {
		if (!head.hasCompanyId()) {
			return Futures.immediateFuture(GetModelByIdResponse.newBuilder().build());
		}
		final long companyId = head.getCompanyId();
		final List<Integer> modelIdList = request.getModelIdList();
		if (modelIdList.isEmpty()) {
			return Futures.immediateFuture(GetModelByIdResponse.newBuilder().build());
		}
		
		Map<Integer, AllowProtos.Model> modelMap = this.doGetModel(companyId, modelIdList);
		
		List<AllowProtos.Model> modelList = new ArrayList<AllowProtos.Model>();
		for (AllowProtos.Model model : modelMap.values()) {
			modelList.add(model);
		}
		
		return Futures.immediateFuture(GetModelByIdResponse.newBuilder()
				.addAllModel(modelList)
				.build());
	}
	
	@Override
	public ListenableFuture<GetModelRuleListResponse> getModelRuleList(AdminHead head, GetModelRuleListRequest request) {
		if (!head.hasCompanyId()) {
			return Futures.immediateFuture(GetModelRuleListResponse.newBuilder().build());
		}
		final long companyId = head.getCompanyId();
		final int modelId = request.getModelId();
		
		AllowDAOProtos.ModelRule modelRule = this.doGetModelRule(companyId, Collections.singleton(modelId)).get(modelId);
		if (modelRule == null) {
			return Futures.immediateFuture(GetModelRuleListResponse.newBuilder().build());
		}
		
		return Futures.immediateFuture(GetModelRuleListResponse.newBuilder()
				.addAllRule(modelRule.getRuleList())
				.build());
	}

	@Override
	public ListenableFuture<CreateModelResponse> createModel(AdminHead head, CreateModelRequest request) {
		if (!head.hasCompanyId()) {
			return Futures.immediateFuture(CreateModelResponse.newBuilder()
					.setResult(CreateModelResponse.Result.FAIL_UNKNOWN)
					.setFailText("company_id 参数未填")
					.build());
		}
		final long companyId = head.getCompanyId();
		final String modelName = request.getModelName();
		if (modelName.length() > 191) {
			return Futures.immediateFuture(CreateModelResponse.newBuilder()
					.setResult(CreateModelResponse.Result.FAIL_MODEL_NAME_INVALID)
					.setFailText("您输入模型的名称太长")
					.build());
		}
		
		final List<AllowProtos.Rule> ruleList = request.getRuleList();
		for (AllowProtos.Rule rule : ruleList) {
			if (rule.getRuleName().length() > 191) {
				return Futures.immediateFuture(CreateModelResponse.newBuilder()
						.setResult(CreateModelResponse.Result.FAIL_RULE_INVALID)
						.setFailText("您输入规则的名称太长")
						.build());
			}
			String failReason = AbstractRule.check(rule);
			if (failReason != null) {
				return Futures.immediateFuture(CreateModelResponse.newBuilder()
						.setResult(CreateModelResponse.Result.FAIL_RULE_INVALID)
						.setFailText(failReason)
						.build());
			}
		}
		
		final AllowProtos.Action action = request.getDefaultAction();
		final int now = (int) (System.currentTimeMillis() / 1000L);
		
		AllowProtos.Model model = AllowProtos.Model.newBuilder()
				.setModelId(0) // 插入的modelId默认是0
				.setModelName(modelName)
				.setDefaultAction(action)
				.setCreateAdminId(head.getSession().getAdminId())
				.setCreateTime(now)
				.build();
		
		Connection conn = null;
		int modelId = 0;
		try {
			conn = this.hikariDataSource.getConnection();
			
			modelId = AllowDB.insertModel(conn, companyId, model);
			AllowDB.insertRule(conn, companyId, modelId, ruleList);
		} catch (SQLException ex) {
			throw new RuntimeException("db fail", ex);
		} finally {
			DBUtil.closeQuietly(conn);
		}
		
		Jedis jedis = jedisPool.getResource();
		try {
			AllowCache.delModelRule(jedis, companyId, Collections.singleton(modelId));
		} finally {
			jedis.close();
		}
		
		return Futures.immediateFuture(CreateModelResponse.newBuilder()
				.setResult(CreateModelResponse.Result.SUCC)
				.setModelId(modelId)
				.build());
	}

	@Override
	public ListenableFuture<CreateUserRuleResponse> createUserRule(AdminHead head, CreateUserRuleRequest request) {
		if (!head.hasCompanyId()) {
			return Futures.immediateFuture(CreateUserRuleResponse.newBuilder()
					.setResult(CreateUserRuleResponse.Result.FAIL_UNKNOWN)
					.setFailText("company_id 参数未填")
					.build());
		}
		final long companyId = head.getCompanyId();
		final String ruleName = request.getRuleName();
		if (ruleName.length() > 191) {
			return Futures.immediateFuture(CreateUserRuleResponse.newBuilder()
					.setResult(CreateUserRuleResponse.Result.FAIL_RULE_NAME_INVALID)
					.setFailText("输入的规则名称太长")
					.build());
		}
		
		final List<Long> userIdList = request.getUserIdList();
		if (userIdList.size() > 100) {
			return Futures.immediateFuture(CreateUserRuleResponse.newBuilder()
					.setResult(CreateUserRuleResponse.Result.FAIL_USER_INVALID)
					.setFailText("输入的规则内容太长")
					.build());
		}

		final int modelId = request.getModelId();
		AllowProtos.Model model = this.doGetModel(companyId, Collections.singleton(modelId)).get(modelId);
		if (model == null) {
			return Futures.immediateFuture(CreateUserRuleResponse.newBuilder()
					.setResult(CreateUserRuleResponse.Result.FAIL_MODEL_INVALID)
					.setFailText("不存在的模型id")
					.build());
		}
		
		final AllowProtos.Action ruleAction = request.getRuleAction();
		
		AllowProtos.UserRule userRule = AllowProtos.UserRule.newBuilder()
				.addAllUserId(userIdList)
				.build();
		AllowProtos.Rule rule = AllowProtos.Rule.newBuilder()
				.setRuleId(0)
				.setAction(ruleAction)
				.setRuleName(ruleName)
				.setUserRule(userRule)
				.build();
				
		Connection conn = null;
		try {
			conn = this.hikariDataSource.getConnection();
			
			int ruleId = AllowDB.insertRule(conn, companyId, modelId, Arrays.asList(rule)).get(0);
			
			return Futures.immediateFuture(CreateUserRuleResponse.newBuilder()
					.setResult(CreateUserRuleResponse.Result.SUCC)
					.setRuleId(ruleId)
					.build());
		} catch (SQLException ex) {
			throw new RuntimeException("db fail", ex);
		} finally {
			DBUtil.closeQuietly(conn);
		}
	}

	@Override
	public ListenableFuture<CreateTeamRuleResponse> createTeamRule(AdminHead head, CreateTeamRuleRequest request) {
		if (!head.hasCompanyId()) {
			return Futures.immediateFuture(CreateTeamRuleResponse.newBuilder()
					.setResult(CreateTeamRuleResponse.Result.FAIL_UNKNOWN)
					.setFailText("company_id 参数未填")
					.build());
		}
		final long companyId = head.getCompanyId();
		final String ruleName = request.getRuleName();
		if (ruleName.length() > 191) {
			return Futures.immediateFuture(CreateTeamRuleResponse.newBuilder()
					.setResult(CreateTeamRuleResponse.Result.FAIL_RULE_NAME_INVALID)
					.setFailText("输入的规则名称太长")
					.build());
		}
		
		final List<Integer> teamIdList = request.getTeamIdList();
		if (teamIdList.size() > 100) {
			return Futures.immediateFuture(CreateTeamRuleResponse.newBuilder()
					.setResult(CreateTeamRuleResponse.Result.FAIL_TEAM_INVALID)
					.setFailText("输入的规则内容太长")
					.build());
		}
		
		final int modelId = request.getModelId();
		AllowProtos.Model model = this.doGetModel(companyId, Collections.singleton(modelId)).get(modelId);
		if (model == null) {
			return Futures.immediateFuture(CreateTeamRuleResponse.newBuilder()
					.setResult(CreateTeamRuleResponse.Result.FAIL_MODEL_INVALID)
					.setFailText("不存在的模型id")
					.build());
		}
		
		final AllowProtos.Action ruleAction = request.getRuleAction();
		
		AllowProtos.TeamRule teamRule = AllowProtos.TeamRule.newBuilder()
				.addAllTeamId(teamIdList)
				.build();
		AllowProtos.Rule rule = AllowProtos.Rule.newBuilder()
				.setRuleId(0)
				.setAction(ruleAction)
				.setRuleName(ruleName)
				.setTeamRule(teamRule)
				.build();
		
		Connection conn = null;
		try {
			conn = this.hikariDataSource.getConnection();
			
			int ruleId = AllowDB.insertRule(conn, companyId, modelId, Arrays.asList(rule)).get(0);
			
			return Futures.immediateFuture(CreateTeamRuleResponse.newBuilder()
					.setResult(CreateTeamRuleResponse.Result.SUCC)
					.setRuleId(ruleId)
					.build());
		} catch (SQLException ex) {
			throw new RuntimeException("db fail", ex);
		} finally {
			DBUtil.closeQuietly(conn);
		}
	}

	@Override
	public ListenableFuture<CreatePositionRuleResponse> createPositionRule(AdminHead head, CreatePositionRuleRequest request) {
		if (!head.hasCompanyId()) {
			return Futures.immediateFuture(CreatePositionRuleResponse.newBuilder()
					.setResult(CreatePositionRuleResponse.Result.FAIL_UNKNOWN)
					.setFailText("company_id 参数未填")
					.build());
		}
		final long companyId = head.getCompanyId();
		final String ruleName = request.getRuleName();
		if (ruleName.length() > 191) {
			return Futures.immediateFuture(CreatePositionRuleResponse.newBuilder()
					.setResult(CreatePositionRuleResponse.Result.FAIL_RULE_NAME_INVALID)
					.setFailText("输入的规则名称太长")
					.build());
		}
		
		final List<Integer> psotionIdList = request.getPositionIdList();
		if (psotionIdList.size() > 100) {
			return Futures.immediateFuture(CreatePositionRuleResponse.newBuilder()
					.setResult(CreatePositionRuleResponse.Result.FAIL_POSITION_INVALID)
					.setFailText("输入的规则内容太长")
					.build());
		}
		
		final int modelId = request.getModelId();
		AllowProtos.Model model = this.doGetModel(companyId, Collections.singleton(modelId)).get(modelId);
		if (model == null) {
			return Futures.immediateFuture(CreatePositionRuleResponse.newBuilder()
					.setResult(CreatePositionRuleResponse.Result.FAIL_MODEL_INVALID)
					.setFailText("不存在的模型id")
					.build());
		}
		
		final AllowProtos.Action ruleAction = request.getRuleAction();
		
		AllowProtos.PositionRule positionRule = AllowProtos.PositionRule.newBuilder()
				.addAllPositionId(psotionIdList)
				.build();
		AllowProtos.Rule rule = AllowProtos.Rule.newBuilder()
				.setRuleId(0)
				.setAction(ruleAction)
				.setRuleName(ruleName)
				.setPositionRule(positionRule)
				.build();
				
		Connection conn = null;
		try {
			conn = this.hikariDataSource.getConnection();
			
			int ruleId = AllowDB.insertRule(conn, companyId, modelId, Arrays.asList(rule)).get(0);
			
			return Futures.immediateFuture(CreatePositionRuleResponse.newBuilder()
					.setResult(CreatePositionRuleResponse.Result.SUCC)
					.setRuleId(ruleId)
					.build());
		} catch (SQLException ex) {
			throw new RuntimeException("db fail", ex);
		} finally {
			DBUtil.closeQuietly(conn);
		}
	}
	
	@Override
	public ListenableFuture<CreateLevelRuleResponse> createLevelRule(AdminHead head, CreateLevelRuleRequest request) {
		if (!head.hasCompanyId()) {
			return Futures.immediateFuture(CreateLevelRuleResponse.newBuilder()
					.setResult(CreateLevelRuleResponse.Result.FAIL_UNKNOWN)
					.setFailText("company_id 参数未填")
					.build());
		}
		final long companyId = head.getCompanyId();
		final String ruleName = request.getRuleName();
		if (ruleName.length() > 191) {
			return Futures.immediateFuture(CreateLevelRuleResponse.newBuilder()
					.setResult(CreateLevelRuleResponse.Result.FAIL_RULE_NAME_INVALID)
					.setFailText("输入的规则名称太长")
					.build());
		}
		
		final List<Integer> levelIdList = request.getLevelIdList();
		if (levelIdList.size() > 100) {
			return Futures.immediateFuture(CreateLevelRuleResponse.newBuilder()
					.setResult(CreateLevelRuleResponse.Result.FAIL_LEVEL_INVALID)
					.setFailText("输入的规则内容太长")
					.build());
		}
		
		final int modelId = request.getModelId();
		AllowProtos.Model model = this.doGetModel(companyId, Collections.singleton(modelId)).get(modelId);
		if (model == null) {
			return Futures.immediateFuture(CreateLevelRuleResponse.newBuilder()
					.setResult(CreateLevelRuleResponse.Result.FAIL_LEVEL_INVALID)
					.setFailText("不存在的模型id")
					.build());
		}
		
		final AllowProtos.Action ruleAction = request.getRuleAction();
		
		AllowProtos.LevelRule levelRule = AllowProtos.LevelRule.newBuilder()
				.addAllLevelId(levelIdList)
				.build();
		AllowProtos.Rule rule = AllowProtos.Rule.newBuilder()
				.setRuleId(0)
				.setAction(ruleAction)
				.setRuleName(ruleName)
				.setLevelRule(levelRule)
				.build();
				
		Connection conn = null;
		try {
			conn = this.hikariDataSource.getConnection();
			
			int ruleId = AllowDB.insertRule(conn, companyId, modelId, Arrays.asList(rule)).get(0);
			
			return Futures.immediateFuture(CreateLevelRuleResponse.newBuilder()
					.setResult(CreateLevelRuleResponse.Result.SUCC)
					.setRuleId(ruleId)
					.build());
		} catch (SQLException ex) {
			throw new RuntimeException("db fail", ex);
		} finally {
			DBUtil.closeQuietly(conn);
		}
	}

	@Override
	public ListenableFuture<DeleteModelResponse> deleteModel(AdminHead head, DeleteModelRequest request) {
		if (!head.hasCompanyId()) {
			return Futures.immediateFuture(DeleteModelResponse.newBuilder()
					.setResult(DeleteModelResponse.Result.FAIL_UNKNOWN)
					.setFailText("company_id 参数未填")
					.build());
		}
		final long companyId = head.getCompanyId();
		final List<Integer> modelIdList = request.getModelIdList();
		if (modelIdList.isEmpty()) {
			return Futures.immediateFuture(DeleteModelResponse.newBuilder()
					.setResult(DeleteModelResponse.Result.SUCC)
					.build());
		}
		
		Map<Integer, AllowProtos.Model> modelMap = this.doGetModel(companyId, modelIdList);
		Set<Integer> modelIdSet = modelMap.keySet();
		for (int modelId : modelIdList) {
			if (!modelIdSet.contains(modelId)) {
				return Futures.immediateFuture(DeleteModelResponse.newBuilder()
						.setResult(DeleteModelResponse.Result.FAIL_MODEL_INVALID)
						.setFailText("不存在的模型，模型id：" + modelId)
						.build());
			}
		}
		
		Connection conn = null;
		try {
			conn = this.hikariDataSource.getConnection();
			
			AllowDB.deleteModel(conn, companyId, modelIdList);
		} catch (SQLException ex) {
			throw new RuntimeException("db fail", ex);
		} finally {
			DBUtil.closeQuietly(conn);
		}
		
		Jedis jedis = jedisPool.getResource();
		try {
			AllowCache.delModelRule(jedis, companyId, modelIdList);
		} finally {
			jedis.close();
		}
		
		return Futures.immediateFuture(DeleteModelResponse.newBuilder()
				.setResult(DeleteModelResponse.Result.SUCC)
				.build());
	}

	@Override
	public ListenableFuture<DeleteRuleResponse> deleteRule(AdminHead head, DeleteRuleRequest request) {
		if (!head.hasCompanyId()) {
			return Futures.immediateFuture(DeleteRuleResponse.newBuilder()
					.setResult(DeleteRuleResponse.Result.FAIL_UNKNOWN)
					.setFailText("company_id 参数未填")
					.build());
		}
		final long companyId = head.getCompanyId();
		final List<Integer> ruleIdList = request.getRuleIdList();
		if (ruleIdList.isEmpty()) {
			return Futures.immediateFuture(DeleteRuleResponse.newBuilder()
					.setResult(DeleteRuleResponse.Result.SUCC)
					.build());
		}
		
		final int modelId = request.getModelId();
		
		AllowDAOProtos.ModelRule modelRule = this.doGetModelRule(companyId, Collections.singleton(modelId)).get(modelId);
		if (modelRule == null) {
			return Futures.immediateFuture(DeleteRuleResponse.newBuilder()
					.setResult(DeleteRuleResponse.Result.FAIL_MODEL_INVALID)
					.setFailText("不存在的模型，模型id：" + modelId)
					.build());
		}
		
		List<AllowProtos.Rule> ruleList = modelRule.getRuleList();
		List<Integer> tmpRuleIdList = new ArrayList<Integer>();
		for (AllowProtos.Rule rule : ruleList) {
			tmpRuleIdList.add(rule.getRuleId());
		}
		
		for (int ruleId : ruleIdList) {
			if (!tmpRuleIdList.contains(ruleId)) {
				return Futures.immediateFuture(DeleteRuleResponse.newBuilder()
						.setResult(DeleteRuleResponse.Result.FAIL_RULE_INVALID)
						.setFailText("不存在的规则，规则id" + ruleId)
						.build());
			}
		}
		
		Connection conn = null;
		try {
			conn = this.hikariDataSource.getConnection();
			
			AllowDB.deleteRule(conn, companyId, modelId, ruleIdList);
		} catch (SQLException ex) {
			throw new RuntimeException("db fail", ex);
		} finally {
			DBUtil.closeQuietly(conn);
		}
		
		Jedis jedis = jedisPool.getResource();
		try {
			AllowCache.delModelRule(jedis, companyId, Collections.singleton(modelId));
		} finally {
			jedis.close();
		}
		
		return Futures.immediateFuture(DeleteRuleResponse.newBuilder()
				.setResult(DeleteRuleResponse.Result.SUCC)
				.build());
	}

	@Override
	public ListenableFuture<UpdateModelResponse> updateModel(AdminHead head, UpdateModelRequest request) {
		if (!head.hasCompanyId()) {
			return Futures.immediateFuture(UpdateModelResponse.newBuilder()
					.setResult(UpdateModelResponse.Result.FAIL_UNKNOWN)
					.setFailText("company_id 参数未填")
					.build());
		}
		final long companyId = head.getCompanyId();
		final String modelName = request.getModelName();
		if (modelName.length() > 191) {
			return Futures.immediateFuture(UpdateModelResponse.newBuilder()
					.setResult(UpdateModelResponse.Result.FAIL_MODEL_NAME_INVALID)
					.setFailText("您输入的模型名称过长")
					.build());
		}
		
		final int modelId = request.getModelId();
		
		AllowProtos.Model model = this.doGetModel(companyId, Collections.singleton(modelId)).get(modelId);
		if (model == null) {
			return Futures.immediateFuture(UpdateModelResponse.newBuilder()
					.setResult(UpdateModelResponse.Result.FAIL_MODEL_NOT_EXIST)
					.setFailText("不存在的模型，模型id：" + modelId)
					.build());
		}
		
		final AllowProtos.Action action = request.getDefaultAction();
		
		Connection conn = null;
		try {
			conn = this.hikariDataSource.getConnection();
			
			AllowDB.updateModelName(conn, companyId, modelId, modelName, action);
		} catch (SQLException ex) {
			throw new RuntimeException("db fail", ex);
		} finally {
			DBUtil.closeQuietly(conn);
		}
		
		Jedis jedis = jedisPool.getResource();
		try {
			AllowCache.delModelRule(jedis, companyId, Collections.singleton(modelId));
		} finally {
			jedis.close();
		}
		
		return Futures.immediateFuture(UpdateModelResponse.newBuilder()
				.setResult(UpdateModelResponse.Result.SUCC)
				.build());
	}

	@Override
	public ListenableFuture<UpdateModelRuleOrderResponse> updateModelRuleOrder(AdminHead head, UpdateModelRuleOrderRequest request) {
		if (!head.hasCompanyId()) {
			return Futures.immediateFuture(UpdateModelRuleOrderResponse.newBuilder()
					.setResult(UpdateModelRuleOrderResponse.Result.FAIL_UNKNOWN)
					.setFailText("company_id 参数未填")
					.build());
		}
		final long companyId = head.getCompanyId();
		final int modelId = request.getModelId();
		final List<Integer> ruleIdList = request.getRuleIdList();
		
		AllowDAOProtos.ModelRule modelRule = this.doGetModelRule(companyId, Collections.singleton(modelId)).get(modelId);
		if (modelRule == null) {
			return Futures.immediateFuture(UpdateModelRuleOrderResponse.newBuilder()
					.setResult(UpdateModelRuleOrderResponse.Result.FAIL_MODEL_NOT_EXIST)
					.setFailText("不存在的模型，模型id：" + modelId)
					.build());
		}
		
		List<Integer> tmpRuleIdList = new ArrayList<Integer>();
		for (AllowProtos.Rule rule : modelRule.getRuleList()) {
			tmpRuleIdList.add(rule.getRuleId());
		}
		for (int ruleId : ruleIdList) {
			if (!tmpRuleIdList.contains(ruleId)) {
				return Futures.immediateFuture(UpdateModelRuleOrderResponse.newBuilder()
						.setResult(UpdateModelRuleOrderResponse.Result.FAIL_RULE_INVALID)
						.setFailText("不存在的规则，规则id：" + ruleId)
						.build());
			}
		}
		
		Connection conn = null;
		try {
			conn = this.hikariDataSource.getConnection();
			
			AllowDB.updateModelRuleOrder(conn, companyId, modelId, ruleIdList);
		} catch (SQLException ex) {
			throw new RuntimeException("db fail", ex);
		} finally {
			DBUtil.closeQuietly(conn);
		}
		
		Jedis jedis = jedisPool.getResource();
		try {
			AllowCache.delModelRule(jedis, companyId, Collections.singleton(modelId));
		} finally {
			jedis.close();
		}
		
		return Futures.immediateFuture(UpdateModelRuleOrderResponse.newBuilder()
				.setResult(UpdateModelRuleOrderResponse.Result.SUCC)
				.build());
	}

	@Override
	public ListenableFuture<UpdateUserRuleResponse> updateUserRule(AdminHead head, UpdateUserRuleRequest request) {
		if (!head.hasCompanyId()) {
			return Futures.immediateFuture(UpdateUserRuleResponse.newBuilder()
					.setResult(UpdateUserRuleResponse.Result.FAIL_UNKNOWN)
					.setFailText("company_id 参数未填")
					.build());
		}
		final long companyId = head.getCompanyId();
		final List<Long> userIdList = request.getUserIdList();
		if (userIdList.size() > 100) {
			return Futures.immediateFuture(UpdateUserRuleResponse.newBuilder()
					.setResult(UpdateUserRuleResponse.Result.FAIL_RULE_INVALID)
					.setFailText("规则条数太多，最大100条")
					.build());
		}
		
		final String ruleName = request.getRuleName();
		if (ruleName.length() > 191) {
			return Futures.immediateFuture(UpdateUserRuleResponse.newBuilder()
					.setResult(UpdateUserRuleResponse.Result.FAIL_RULE_INVALID)
					.setFailText("规则名称太长")
					.build());
		}
		
		final int modelId = request.getModelId();
		AllowDAOProtos.ModelRule modelRule = this.doGetModelRule(companyId, Collections.singleton(modelId)).get(modelId);
		if (modelRule == null) {
			logger.info("this rule has no Model now！");
		}
		
		final int ruleId = request.getRuleId();
		List<Integer> ruleIdList = new ArrayList<Integer>();
		for (AllowProtos.Rule rule : modelRule.getRuleList()) {
			ruleIdList.add(rule.getRuleId());
		}
		if (!ruleIdList.contains(ruleId)) {
			return Futures.immediateFuture(UpdateUserRuleResponse.newBuilder()
					.setResult(UpdateUserRuleResponse.Result.FAIL_RULE_NOT_EXIST)
					.setFailText("不存在的规则，规则id：" + ruleId)
					.build());
		}
		
		final AllowProtos.Action ruleAction = request.getRuleAction();
		
		Connection conn = null;
		try {
			conn = this.hikariDataSource.getConnection();
			
			AllowDB.updateUserRule(conn, companyId, ruleId, ruleName, ruleAction, userIdList);
		} catch (SQLException ex) {
			throw new RuntimeException("db fail", ex);
		} finally {
			DBUtil.closeQuietly(conn);
		}
		
		Jedis jedis = jedisPool.getResource();
		try {
			AllowCache.delModelRule(jedis, companyId, Collections.singleton(modelId));
		} finally {
			jedis.close();
		}
		
		return Futures.immediateFuture(UpdateUserRuleResponse.newBuilder()
				.setResult(UpdateUserRuleResponse.Result.SUCC)
				.build());
	}

	@Override
	public ListenableFuture<UpdateTeamRuleResponse> updateTeamRule(AdminHead head, UpdateTeamRuleRequest request) {
		if (!head.hasCompanyId()) {
			return Futures.immediateFuture(UpdateTeamRuleResponse.newBuilder()
					.setResult(UpdateTeamRuleResponse.Result.FAIL_UNKNOWN)
					.setFailText("company_id 参数未填")
					.build());
		}
		final long companyId = head.getCompanyId();
		final List<Integer> teamIdList = request.getTeamIdList();
		if (teamIdList.size() > 100) {
			return Futures.immediateFuture(UpdateTeamRuleResponse.newBuilder()
					.setResult(UpdateTeamRuleResponse.Result.FAIL_RULE_INVALID)
					.setFailText("规则条数太多，最大100条")
					.build());
		}
		
		final String ruleName = request.getRuleName();
		if (ruleName.length() > 191) {
			return Futures.immediateFuture(UpdateTeamRuleResponse.newBuilder()
					.setResult(UpdateTeamRuleResponse.Result.FAIL_RULE_INVALID)
					.setFailText("规则名称太长")
					.build());
		}
		
		final int modelId = request.getModelId();
		AllowDAOProtos.ModelRule modelRule = this.doGetModelRule(companyId, Collections.singleton(modelId)).get(modelId);
		if (modelRule == null) {
			return Futures.immediateFuture(UpdateTeamRuleResponse.newBuilder()
					.setResult(UpdateTeamRuleResponse.Result.FAIL_MODEL_NOT_EXIST)
					.setFailText("不存在的模型，模型id：" + modelId)
					.build());
		}
		
		final int ruleId = request.getRuleId();
		List<Integer> ruleIdList = new ArrayList<Integer>();
		for (AllowProtos.Rule rule : modelRule.getRuleList()) {
			ruleIdList.add(rule.getRuleId());
		}
		if (!ruleIdList.contains(ruleId)) {
			return Futures.immediateFuture(UpdateTeamRuleResponse.newBuilder()
					.setResult(UpdateTeamRuleResponse.Result.FAIL_RULE_NOT_EXIST)
					.setFailText("不存在的规则，规则id：" + ruleId)
					.build());
		}
		
		final AllowProtos.Action ruleAction = request.getRuleAction();
		
		Connection conn = null;
		try {
			conn = this.hikariDataSource.getConnection();
			
			AllowDB.updateTeamRule(conn, companyId, ruleId, ruleName, ruleAction, teamIdList);
		} catch (SQLException ex) {
			throw new RuntimeException("db fail", ex);
		} finally {
			DBUtil.closeQuietly(conn);
		}
		
		Jedis jedis = jedisPool.getResource();
		try {
			AllowCache.delModelRule(jedis, companyId, Collections.singleton(modelId));
		} finally {
			jedis.close();
		}
		
		return Futures.immediateFuture(UpdateTeamRuleResponse.newBuilder()
				.setResult(UpdateTeamRuleResponse.Result.SUCC)
				.build());
	}

	@Override
	public ListenableFuture<UpdatePositionRuleResponse> updatePositionRule(AdminHead head, UpdatePositionRuleRequest request) {
		if (!head.hasCompanyId()) {
			return Futures.immediateFuture(UpdatePositionRuleResponse.newBuilder()
					.setResult(UpdatePositionRuleResponse.Result.FAIL_UNKNOWN)
					.setFailText("company_id 参数未填")
					.build());
		}
		final long companyId = head.getCompanyId();
		final List<Integer> positionIdList = request.getPositionIdList();
		if (positionIdList.size() > 100) {
			return Futures.immediateFuture(UpdatePositionRuleResponse.newBuilder()
					.setResult(UpdatePositionRuleResponse.Result.FAIL_RULE_INVALID)
					.setFailText("规则条数太多，最大100条")
					.build());
		}
		
		final String ruleName = request.getRuleName();
		if (ruleName.length() > 191) {
			return Futures.immediateFuture(UpdatePositionRuleResponse.newBuilder()
					.setResult(UpdatePositionRuleResponse.Result.FAIL_RULE_INVALID)
					.setFailText("规则名称太长")
					.build());
		}
		
		final int modelId = request.getModelId();
		AllowDAOProtos.ModelRule modelRule = this.doGetModelRule(companyId, Collections.singleton(modelId)).get(modelId);
		if (modelRule == null) {
			return Futures.immediateFuture(UpdatePositionRuleResponse.newBuilder()
					.setResult(UpdatePositionRuleResponse.Result.FAIL_MODEL_NOT_EXIST)
					.setFailText("不存在的模型，模型id：" + modelId)
					.build());
		}
		
		final int ruleId = request.getRuleId();
		List<Integer> ruleIdList = new ArrayList<Integer>();
		for (AllowProtos.Rule rule : modelRule.getRuleList()) {
			ruleIdList.add(rule.getRuleId());
		}
		if (!ruleIdList.contains(ruleId)) {
			return Futures.immediateFuture(UpdatePositionRuleResponse.newBuilder()
					.setResult(UpdatePositionRuleResponse.Result.FAIL_RULE_NOT_EXIST)
					.setFailText("不存在的规则，规则id：" + ruleId)
					.build());
		}
		
		final AllowProtos.Action ruleAction = request.getRuleAction();
		
		Connection conn = null;
		try {
			conn = this.hikariDataSource.getConnection();
			
			AllowDB.updatePositionRule(conn, companyId, ruleId, ruleName, ruleAction, positionIdList);
		} catch (SQLException ex) {
			throw new RuntimeException("db fail", ex);
		} finally {
			DBUtil.closeQuietly(conn);
		}
		
		Jedis jedis = jedisPool.getResource();
		try {
			AllowCache.delModelRule(jedis, companyId, Collections.singleton(modelId));
		} finally {
			jedis.close();
		}
		
		return Futures.immediateFuture(UpdatePositionRuleResponse.newBuilder()
				.setResult(UpdatePositionRuleResponse.Result.SUCC)
				.build());
	}
	
	@Override
	public ListenableFuture<UpdateLevelRuleResponse> updateLevelRule(AdminHead head, UpdateLevelRuleRequest request) {
		if (!head.hasCompanyId()) {
			return Futures.immediateFuture(UpdateLevelRuleResponse.newBuilder()
					.setResult(UpdateLevelRuleResponse.Result.FAIL_UNKNOWN)
					.setFailText("company_id 参数未填")
					.build());
		}
		final long companyId = head.getCompanyId();
		final List<Integer> levelIdList = request.getLevelIdList();
		if (levelIdList.size() > 100) {
			return Futures.immediateFuture(UpdateLevelRuleResponse.newBuilder()
					.setResult(UpdateLevelRuleResponse.Result.FAIL_RULE_INVALID)
					.setFailText("规则条数太多，最大100条")
					.build());
		}
		
		final String ruleName = request.getRuleName();
		if (ruleName.length() > 191) {
			return Futures.immediateFuture(UpdateLevelRuleResponse.newBuilder()
					.setResult(UpdateLevelRuleResponse.Result.FAIL_RULE_INVALID)
					.setFailText("规则名称太长")
					.build());
		}
		
		final int modelId = request.getModelId();
		AllowDAOProtos.ModelRule modelRule = this.doGetModelRule(companyId, Collections.singleton(modelId)).get(modelId);
		if (modelRule == null) {
			return Futures.immediateFuture(UpdateLevelRuleResponse.newBuilder()
					.setResult(UpdateLevelRuleResponse.Result.FAIL_MODEL_NOT_EXIST)
					.setFailText("不存在的模型，模型id：" + modelId)
					.build());
		}
		
		final int ruleId = request.getRuleId();
		List<Integer> ruleIdList = new ArrayList<Integer>();
		for (AllowProtos.Rule rule : modelRule.getRuleList()) {
			ruleIdList.add(rule.getRuleId());
		}
		if (!ruleIdList.contains(ruleId)) {
			return Futures.immediateFuture(UpdateLevelRuleResponse.newBuilder()
					.setResult(UpdateLevelRuleResponse.Result.FAIL_RULE_NOT_EXIST)
					.setFailText("不存在的规则，规则id：" + ruleId)
					.build());
		}
		
		final AllowProtos.Action ruleAction = request.getRuleAction();
		
		Connection conn = null;
		try {
			conn = this.hikariDataSource.getConnection();
			
			AllowDB.updateLevelRule(conn, companyId, ruleId, ruleName, ruleAction, levelIdList);
		} catch (SQLException ex) {
			throw new RuntimeException("db fail", ex);
		} finally {
			DBUtil.closeQuietly(conn);
		}
		
		Jedis jedis = jedisPool.getResource();
		try {
			AllowCache.delModelRule(jedis, companyId, Collections.singleton(modelId));
		} finally {
			jedis.close();
		}
		
		return Futures.immediateFuture(UpdateLevelRuleResponse.newBuilder()
				.setResult(UpdateLevelRuleResponse.Result.SUCC)
				.build());
	}

	@Override
	public ListenableFuture<CopyModelResponse> copyModel(AdminHead head, CopyModelRequest request) {
		if (!head.hasCompanyId()) {
			return Futures.immediateFuture(CopyModelResponse.newBuilder()
					.setResult(CopyModelResponse.Result.FAIL_UNKNOWN)
					.setFailText("company_id 参数未填")
					.build());
		}
		final long companyId = head.getCompanyId();
		final int modelId = request.getModelId();
		ModelRule modelRule = this.doGetModelRule(companyId, Collections.singleton(modelId)).get(modelId);
		if (modelRule == null) {
			return Futures.immediateFuture(CopyModelResponse.newBuilder()
					.setResult(CopyModelResponse.Result.FAIL_MODEL_NOT_EXIST)
					.setFailText("访问模型不存在")
					.build());
		}
		
		int newModelId = 0;
		
		Connection conn = null;
		try {
			conn = this.hikariDataSource.getConnection();
			
			newModelId = AllowDB.insertModel(conn, companyId, modelRule.getModel());
			AllowDB.insertRule(conn, companyId, newModelId, modelRule.getRuleList());
			
			return Futures.immediateFuture(CopyModelResponse.newBuilder()
					.setResult(CopyModelResponse.Result.SUCC)
					.setNewModelId(newModelId)
					.build());
		} catch (SQLException ex) {
			throw new RuntimeException("db fail", ex);
		} finally {
			DBUtil.closeQuietly(conn);
		}
	}

}
