package com.weizhu.service.official;

import java.sql.Connection;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.Executor;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.annotation.Nullable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import com.google.common.collect.ImmutableSet;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.weizhu.common.db.DBUtil;
import com.weizhu.common.utils.DataPage;
import com.weizhu.common.utils.ProfileManager;
import com.weizhu.proto.AdminOfficialProtos;
import com.weizhu.proto.AdminOfficialProtos.CancelOfficialSendPlanRequest;
import com.weizhu.proto.AdminOfficialProtos.CancelOfficialSendPlanResponse;
import com.weizhu.proto.AdminOfficialProtos.CreateOfficialRequest;
import com.weizhu.proto.AdminOfficialProtos.CreateOfficialResponse;
import com.weizhu.proto.AdminOfficialProtos.CreateOfficialSendPlanRequest;
import com.weizhu.proto.AdminOfficialProtos.CreateOfficialSendPlanResponse;
import com.weizhu.proto.AdminOfficialProtos.DeleteOfficialRequest;
import com.weizhu.proto.AdminOfficialProtos.DeleteOfficialResponse;
import com.weizhu.proto.AdminOfficialProtos.GetOfficialByIdRequest;
import com.weizhu.proto.AdminOfficialProtos.GetOfficialByIdResponse;
import com.weizhu.proto.AdminOfficialProtos.GetOfficialListRequest;
import com.weizhu.proto.AdminOfficialProtos.GetOfficialListResponse;
import com.weizhu.proto.AdminOfficialProtos.GetOfficialMessageRequest;
import com.weizhu.proto.AdminOfficialProtos.GetOfficialMessageResponse;
import com.weizhu.proto.AdminOfficialProtos.GetOfficialSendPlanByIdRequest;
import com.weizhu.proto.AdminOfficialProtos.GetOfficialSendPlanByIdResponse;
import com.weizhu.proto.AdminOfficialProtos.GetOfficialSendPlanListRequest;
import com.weizhu.proto.AdminOfficialProtos.GetOfficialSendPlanListResponse;
import com.weizhu.proto.AdminOfficialProtos.SendSecretaryMessageRequest;
import com.weizhu.proto.AdminOfficialProtos.SendSecretaryMessageResponse;
import com.weizhu.proto.AdminOfficialProtos.SetOfficialStateRequest;
import com.weizhu.proto.AdminOfficialProtos.SetOfficialStateResponse;
import com.weizhu.proto.AdminOfficialProtos.UpdateOfficialRequest;
import com.weizhu.proto.AdminOfficialProtos.UpdateOfficialResponse;
import com.weizhu.proto.AdminOfficialService;
import com.weizhu.proto.AdminProtos;
import com.weizhu.proto.AdminProtos.AdminHead;
import com.weizhu.proto.AdminUserProtos.GetUserListRequest;
import com.weizhu.proto.AdminUserProtos.GetUserListResponse;
import com.weizhu.proto.AdminUserService;
import com.weizhu.proto.AllowProtos;
import com.weizhu.proto.AllowProtos.CheckAllowRequest;
import com.weizhu.proto.AllowProtos.CheckAllowResponse;
import com.weizhu.proto.AllowProtos.GetModelByIdRequest;
import com.weizhu.proto.AllowProtos.GetModelByIdResponse;
import com.weizhu.proto.AllowService;
import com.weizhu.proto.OfficialProtos;
import com.weizhu.proto.PushProtos;
import com.weizhu.proto.PushProtos.PushMsgRequest;
import com.weizhu.proto.PushProtos.PushTarget;
import com.weizhu.proto.PushService;
import com.weizhu.proto.UserProtos;
import com.weizhu.proto.WeizhuProtos.RequestHead;
import com.weizhu.proto.WeizhuProtos.SystemHead;
import com.zaxxer.hikari.HikariDataSource;

public class AdminOfficialServiceImpl implements AdminOfficialService {

	private static final Logger logger = LoggerFactory.getLogger(AdminOfficialServiceImpl.class);
	
	private static final ImmutableSet<OfficialProtos.State> ADMIN_STATE_SET = ImmutableSet.of(OfficialProtos.State.NORMAL, OfficialProtos.State.DISABLE);
	
	private final HikariDataSource hikariDataSource;
	private final JedisPool jedisPool;
	private final Executor serviceExecutor;
	private final ScheduledExecutorService scheduledExecutorService;
	
	private final AdminUserService adminUserService;
	private final PushService pushService;
	private final AllowService allowService;
	private final ProfileManager profileManager;
	
	@Inject
	public AdminOfficialServiceImpl(HikariDataSource hikariDataSource, JedisPool jedisPool, 
			@Named("service_executor") Executor serviceExecutor, 
			@Named("service_scheduled_executor") ScheduledExecutorService scheduledExecutorService,
			AdminUserService adminUserService, PushService pushService, AllowService allowService,
			ProfileManager profileManager
			) {
		this.hikariDataSource = hikariDataSource;
		this.jedisPool = jedisPool;
		this.serviceExecutor = serviceExecutor;
		this.scheduledExecutorService = scheduledExecutorService;
		this.adminUserService = adminUserService;
		this.pushService = pushService;
		this.allowService = allowService;
		this.profileManager = profileManager;
		
		this.serviceExecutor.execute(new LoadOfficialMessageSendPlanTask());
		
		Jedis jedis = jedisPool.getResource();
		try {
			OfficialCache.loadScript(jedis);
		} finally {
			jedis.close();
		}
	}
	
	@Override
	public ListenableFuture<GetOfficialByIdResponse> getOfficialById(AdminHead head, GetOfficialByIdRequest request) {
		if (!head.hasCompanyId()) {
			return Futures.immediateFuture(GetOfficialByIdResponse.newBuilder().build());
		}
		
		return Futures.immediateFuture(GetOfficialByIdResponse.newBuilder()
				.addAllOfficial(OfficialUtil.getOfficial(
						hikariDataSource, jedisPool, this.profileManager.getProfile(head, "official:"),
						head.getCompanyId(), request.getOfficialIdList(), ADMIN_STATE_SET).values())
				.build());
	}

	@Override
	public ListenableFuture<GetOfficialByIdResponse> getOfficialById(SystemHead head, GetOfficialByIdRequest request) {
		if (!head.hasCompanyId()) {
			return Futures.immediateFuture(GetOfficialByIdResponse.newBuilder().build());
		}
		
		return Futures.immediateFuture(GetOfficialByIdResponse.newBuilder()
				.addAllOfficial(OfficialUtil.getOfficial(
						hikariDataSource, jedisPool, this.profileManager.getProfile(head, "official:"),
						head.getCompanyId(), request.getOfficialIdList(), ADMIN_STATE_SET).values())
				.build());
	}

	@Override
	public ListenableFuture<GetOfficialListResponse> getOfficialList(AdminHead head, GetOfficialListRequest request) {
		if (!head.hasCompanyId()) {
			return Futures.immediateFuture(GetOfficialListResponse.newBuilder()
					.setTotalSize(0)
					.build());
		}
		
		final long companyId = head.getCompanyId();
		final int start = request.getStart() < 0 ? 0 : request.getStart();
		final int length = request.getLength() < 0 ? 0 : request.getLength() > 100 ? 100 : request.getLength();
		
		DataPage<Long> officialIdPage;
		Connection dbConn = null;
		try {
			dbConn = this.hikariDataSource.getConnection();
			if (start < 1 && length > 0) {
				DataPage<Long> tmpPage = OfficialDB.getOfficialIdPage(dbConn, companyId, 0, length - 1, ADMIN_STATE_SET);
				List<Long> idList = new ArrayList<Long>();
				idList.add(Long.valueOf(AdminOfficialProtos.ReservedOfficialId.WEIZHU_SECRETARY_VALUE));
				idList.addAll(tmpPage.dataList());
				officialIdPage = new DataPage<Long>(idList, tmpPage.totalSize() + 1, tmpPage.filteredSize() + 1);
			} else {
				DataPage<Long> tmpPage = OfficialDB.getOfficialIdPage(dbConn, companyId, start - 1, length, ADMIN_STATE_SET);
				officialIdPage = new DataPage<Long>(tmpPage.dataList(), tmpPage.totalSize() + 1, tmpPage.filteredSize() + 1);
			}
		} catch (SQLException e) {
			throw new RuntimeException("db fail", e);
		} finally {
			DBUtil.closeQuietly(dbConn);
		}
		
		Map<Long, OfficialProtos.Official> officialMap = OfficialUtil.getOfficial(
				hikariDataSource, jedisPool, this.profileManager.getProfile(head, "official:"),
				companyId, officialIdPage.dataList(), ADMIN_STATE_SET);
		
		GetOfficialListResponse.Builder responseBuilder = GetOfficialListResponse.newBuilder();
		for (Long officialId : officialIdPage.dataList()) {
			OfficialProtos.Official official = officialMap.get(officialId);
			if (official != null) {
				responseBuilder.addOfficial(official);
			}
		}
		responseBuilder.setTotalSize(officialIdPage.totalSize());
		return Futures.immediateFuture(responseBuilder.build());
	}

	@Override
	public ListenableFuture<CreateOfficialResponse> createOfficial(AdminHead head, CreateOfficialRequest request) {
		if (!head.hasCompanyId()) {
			return Futures.immediateFuture(CreateOfficialResponse.newBuilder()
					.setResult(CreateOfficialResponse.Result.FAIL_UNKNOWN)
					.setFailText("company_id 参数未填")
					.build());
		}
		if (request.getOfficialName().isEmpty()) {
			return Futures.immediateFuture(CreateOfficialResponse.newBuilder()
					.setResult(CreateOfficialResponse.Result.FAIL_NAME_INVALID)
					.setFailText("服务号名称不能为空")
					.build());
		}
		if (request.getOfficialName().length() > 100) {
			return Futures.immediateFuture(CreateOfficialResponse.newBuilder()
					.setResult(CreateOfficialResponse.Result.FAIL_NAME_INVALID)
					.setFailText("服务号名称长度最多100字")
					.build());
		}
		if (request.getAvatar().isEmpty()) {
			return Futures.immediateFuture(CreateOfficialResponse.newBuilder()
					.setResult(CreateOfficialResponse.Result.FAIL_AVATAR_INVALID)
					.setFailText("服务号头像为空")
					.build());
		}
		if (request.getAvatar().length() > 191) {
			return Futures.immediateFuture(CreateOfficialResponse.newBuilder()
					.setResult(CreateOfficialResponse.Result.FAIL_AVATAR_INVALID)
					.setFailText("服务号头像错误")
					.build());
		}
		if (request.hasOfficialDesc() && request.getOfficialDesc().length() > 191) {
			return Futures.immediateFuture(CreateOfficialResponse.newBuilder()
					.setResult(CreateOfficialResponse.Result.FAIL_OFFICIAL_DESC_INVALID)
					.setFailText("服务号账号描述不能超过191个字")
					.build());
		}
		if (request.hasFunctionDesc() && request.getFunctionDesc().length() > 191) {
			return Futures.immediateFuture(CreateOfficialResponse.newBuilder()
					.setResult(CreateOfficialResponse.Result.FAIL_FUNCTION_DESC_INVALID)
					.setFailText("服务号功能描述不能超过191个字")
					.build());
		}
		
		final Integer allowModelId = request.hasAllowModelId() ? request.getAllowModelId() : null;
		
		if (allowModelId != null) {
			GetModelByIdResponse getModelByIdResponse = Futures.getUnchecked(allowService.getModelById(head, GetModelByIdRequest.newBuilder()
					.addModelId(allowModelId)
					.build()));
			if (getModelByIdResponse.getModelCount() <= 0) {
				return Futures.immediateFuture(CreateOfficialResponse.newBuilder()
						.setResult(CreateOfficialResponse.Result.FAIL_ALLOW_MODEL_NOT_EXIST)
						.setFailText("此访问模型不存在")
						.build());
			}
		}
		
		OfficialProtos.Official.Builder officialBuilder = OfficialProtos.Official.newBuilder()
				.setOfficialId(-1L)
				.setOfficialName(request.getOfficialName())
				.setAvatar(request.getAvatar());
		
		if (request.hasOfficialDesc()) {
			officialBuilder.setOfficialDesc(request.getOfficialDesc());
		}
		if (request.hasFunctionDesc()) {
			officialBuilder.setFunctionDesc(request.getFunctionDesc());
		}
		if (allowModelId != null) {
			officialBuilder.setAllowModelId(allowModelId);
		}
		
		officialBuilder.setState(OfficialProtos.State.NORMAL);
		officialBuilder.setCreateAdminId(head.getSession().getAdminId());
		officialBuilder.setCreateTime((int) (System.currentTimeMillis() / 1000L));

		final OfficialProtos.Official official = officialBuilder.build();
		
		final long companyId = head.getCompanyId();
		final long officialId;
		Connection dbConn = null;
		try {
			dbConn = this.hikariDataSource.getConnection();
			officialId = OfficialDB.insertOfficial(dbConn, companyId, Collections.singletonList(official)).get(0);
		} catch (SQLException e) {
			throw new RuntimeException("db fail", e);
		} finally {
			DBUtil.closeQuietly(dbConn);
		}
		
		Jedis jedis = this.jedisPool.getResource();
		try {
			OfficialCache.delOfficial(jedis, companyId, Collections.singleton(officialId));
		} finally {
			jedis.close();
		}
		
		return Futures.immediateFuture(CreateOfficialResponse.newBuilder()
				.setResult(CreateOfficialResponse.Result.SUCC)
				.setOfficialId(officialId)
				.build());
	}

	@Override
	public ListenableFuture<UpdateOfficialResponse> updateOfficial(AdminHead head, UpdateOfficialRequest request) {
		if (!head.hasCompanyId()) {
			return Futures.immediateFuture(UpdateOfficialResponse.newBuilder()
					.setResult(UpdateOfficialResponse.Result.FAIL_UNKNOWN)
					.setFailText("company_id 参数未填")
					.build());
		}
		if (request.getOfficialName().isEmpty()) {
			return Futures.immediateFuture(UpdateOfficialResponse.newBuilder()
					.setResult(UpdateOfficialResponse.Result.FAIL_NAME_INVALID)
					.setFailText("服务号名称不能为空")
					.build());
		}
		if (request.getOfficialName().length() > 100) {
			return Futures.immediateFuture(UpdateOfficialResponse.newBuilder()
					.setResult(UpdateOfficialResponse.Result.FAIL_NAME_INVALID)
					.setFailText("服务号名称长度最多100字")
					.build());
		}
		if (request.getAvatar().isEmpty()) {
			return Futures.immediateFuture(UpdateOfficialResponse.newBuilder()
					.setResult(UpdateOfficialResponse.Result.FAIL_AVATAR_INVALID)
					.setFailText("服务号头像为空")
					.build());
		}
		if (request.getAvatar().length() > 191) {
			return Futures.immediateFuture(UpdateOfficialResponse.newBuilder()
					.setResult(UpdateOfficialResponse.Result.FAIL_AVATAR_INVALID)
					.setFailText("服务号头像错误")
					.build());
		}
		if (request.hasOfficialDesc() && request.getOfficialDesc().length() > 191) {
			return Futures.immediateFuture(UpdateOfficialResponse.newBuilder()
					.setResult(UpdateOfficialResponse.Result.FAIL_OFFICIAL_DESC_INVALID)
					.setFailText("服务号账号描述不能超过191个字")
					.build());
		}
		if (request.hasFunctionDesc() && request.getFunctionDesc().length() > 191) {
			return Futures.immediateFuture(UpdateOfficialResponse.newBuilder()
					.setResult(UpdateOfficialResponse.Result.FAIL_FUNCTION_DESC_INVALID)
					.setFailText("服务号功能描述不能超过191个字")
					.build());
		}
		
		final long companyId = head.getCompanyId();
		final long officialId = request.getOfficialId();
		
		OfficialProtos.Official oldOfficial = OfficialUtil.getOfficial(
				hikariDataSource, jedisPool, this.profileManager.getProfile(head, "official:"), 
				companyId, Collections.singleton(officialId), ADMIN_STATE_SET).get(officialId);
		if (oldOfficial == null) {
			return Futures.immediateFuture(UpdateOfficialResponse.newBuilder()
					.setResult(UpdateOfficialResponse.Result.FAIL_OFFICIAL_NOT_EXIST)
					.setFailText("服务号不存在")
					.build());
		}
		
		final Integer allowModelId = request.hasAllowModelId() ? request.getAllowModelId() : null;
		
		if (allowModelId != null) {
			GetModelByIdResponse getModelByIdResponse = Futures.getUnchecked(allowService.getModelById(head, GetModelByIdRequest.newBuilder()
					.addModelId(allowModelId)
					.build()));
			if (getModelByIdResponse.getModelCount() <= 0) {
				return Futures.immediateFuture(UpdateOfficialResponse.newBuilder()
						.setResult(UpdateOfficialResponse.Result.FAIL_ALLOW_MODEL_NOT_EXIST)
						.setFailText("此访问模型不存在")
						.build());
			}
		}
		
		OfficialProtos.Official.Builder officialBuilder = OfficialProtos.Official.newBuilder()
				.setOfficialId(officialId)
				.setOfficialName(request.getOfficialName())
				.setAvatar(request.getAvatar());
		
		if (request.hasOfficialDesc()) {
			officialBuilder.setOfficialDesc(request.getOfficialDesc());
		}
		if (request.hasFunctionDesc()) {
			officialBuilder.setFunctionDesc(request.getFunctionDesc());
		}
		
		officialBuilder.setState(oldOfficial.getState());
		
		if (officialId != (long) (AdminOfficialProtos.ReservedOfficialId.WEIZHU_SECRETARY_VALUE)) {
			if (allowModelId != null) {
				officialBuilder.setAllowModelId(allowModelId);
			}
			
			if (request.hasState()) {
				officialBuilder.setState(request.getState());
			}
		}
		
		if (oldOfficial.hasCreateAdminId()) {
			officialBuilder.setCreateAdminId(oldOfficial.getCreateAdminId());
		}
		if (oldOfficial.hasCreateTime()) {
			officialBuilder.setCreateTime(oldOfficial.getCreateTime());
		}
		officialBuilder.setUpdateAdminId(head.getSession().getAdminId());
		officialBuilder.setUpdateTime((int) (System.currentTimeMillis() / 1000L));
		
		final OfficialProtos.Official newOfficial = officialBuilder.build();
		
		if (officialId == (long) (AdminOfficialProtos.ReservedOfficialId.WEIZHU_SECRETARY_VALUE)) {
			this.profileManager.setProfile(head, 
					new ProfileManager.ProfileBuilder()
						.set(OfficialUtil.WEIZHU_SECRETARY_OFFICIAL, newOfficial)
				);
		} else {
			Connection dbConn = null;
			try {
				dbConn = this.hikariDataSource.getConnection();
				OfficialDB.updateOfficial(dbConn, companyId, newOfficial);
			} catch (SQLException e) {
				throw new RuntimeException("db fail", e);
			} finally {
				DBUtil.closeQuietly(dbConn);
			}
		
			Jedis jedis = this.jedisPool.getResource();
			try {
				OfficialCache.delOfficial(jedis, companyId, Collections.singleton(officialId));
			} finally {
				jedis.close();
			}
		}
		
		return Futures.immediateFuture(UpdateOfficialResponse.newBuilder()
				.setResult(UpdateOfficialResponse.Result.SUCC)
				.build());
	}
	
	@Override
	public ListenableFuture<DeleteOfficialResponse> deleteOfficial(AdminHead head, DeleteOfficialRequest request) {
		if (!head.hasCompanyId()) {
			return Futures.immediateFuture(DeleteOfficialResponse.newBuilder()
					.setResult(DeleteOfficialResponse.Result.FAIL_UNKNOWN)
					.setFailText("company_id 参数未填")
					.build());
		}
		final long companyId = head.getCompanyId();
		final Set<Long> officialIdSet = new TreeSet<Long>(request.getOfficialIdList());
		officialIdSet.contains(Long.valueOf(AdminOfficialProtos.ReservedOfficialId.WEIZHU_SECRETARY_VALUE));
		
		if (officialIdSet.isEmpty()) {
			return Futures.immediateFuture(DeleteOfficialResponse.newBuilder()
					.setResult(DeleteOfficialResponse.Result.SUCC)
					.build());
		}
		
		Connection dbConn = null;
		try {
			dbConn = this.hikariDataSource.getConnection();
			
			OfficialDB.deleteOfficial(dbConn, companyId, officialIdSet);
			
		} catch (SQLException ex) {
			throw new RuntimeException("db fail", ex);
		} finally {
			DBUtil.closeQuietly(dbConn);
		}
		
		Jedis jedis = this.jedisPool.getResource();
		try {
			OfficialCache.delOfficial(jedis, companyId, officialIdSet);
		} finally {
			jedis.close();
		}
		
		return Futures.immediateFuture(DeleteOfficialResponse.newBuilder()
				.setResult(DeleteOfficialResponse.Result.SUCC)
				.build());
	}

	@Override
	public ListenableFuture<SetOfficialStateResponse> setOfficialState(AdminHead head, SetOfficialStateRequest request) {
		if (!head.hasCompanyId()) {
			return Futures.immediateFuture(SetOfficialStateResponse.newBuilder()
					.setResult(SetOfficialStateResponse.Result.FAIL_UNKNOWN)
					.setFailText("company_id 参数未填")
					.build());
		}
		final long companyId = head.getCompanyId();
		final Set<Long> officialIdSet = new TreeSet<Long>(request.getOfficialIdList());
		officialIdSet.contains(Long.valueOf(AdminOfficialProtos.ReservedOfficialId.WEIZHU_SECRETARY_VALUE));
		if (officialIdSet.isEmpty()) {
			return Futures.immediateFuture(SetOfficialStateResponse.newBuilder()
					.setResult(SetOfficialStateResponse.Result.SUCC)
					.build());
		}
		
		Map<Long, OfficialProtos.Official> officialMap = OfficialUtil.getOfficial(
				hikariDataSource, jedisPool, this.profileManager.getProfile(head, "official:"), 
				companyId, officialIdSet, ADMIN_STATE_SET);
		
		boolean isUpdate = false;
		for (OfficialProtos.Official official : officialMap.values()) {
			if (official.getState() != request.getState()) {
				isUpdate = true;
				break;
			}
		}
		
		if (!isUpdate) {
			return Futures.immediateFuture(SetOfficialStateResponse.newBuilder()
					.setResult(SetOfficialStateResponse.Result.SUCC)
					.build());
		}
		
		Connection dbConn = null;
		try {
			dbConn = this.hikariDataSource.getConnection();
			
			OfficialDB.setOfficialState(dbConn, companyId, officialMap.keySet(), request.getState());
			
		} catch (SQLException e) {
			throw new RuntimeException("db fail", e);
		} finally {
			DBUtil.closeQuietly(dbConn);
		}

		Jedis jedis = this.jedisPool.getResource();
		try {
			OfficialCache.delOfficial(jedis, companyId, officialMap.keySet());
		} finally {
			jedis.close();
		}
		
		return Futures.immediateFuture(SetOfficialStateResponse.newBuilder()
				.setResult(SetOfficialStateResponse.Result.SUCC)
				.build());
	}

	@Override
	public ListenableFuture<GetOfficialSendPlanByIdResponse> getOfficialSendPlanById(AdminHead head, GetOfficialSendPlanByIdRequest request) {
		if (!head.hasCompanyId() || request.getPlanIdCount() <= 0) {
			return Futures.immediateFuture(GetOfficialSendPlanByIdResponse.newBuilder().build());
		}
		
		final long companyId = head.getCompanyId();
		
		Map<Integer, AdminOfficialProtos.OfficialSendPlan> sendPlanMap;
		Connection dbConn = null;
		try {
			dbConn = this.hikariDataSource.getConnection();
			sendPlanMap = OfficialDB.getOfficialSendPlan(dbConn, companyId, request.getPlanIdList());
		} catch (SQLException e) {
			throw new RuntimeException("db fail", e);
		} finally {
			DBUtil.closeQuietly(dbConn);
		}
		
		Set<Long> refOfficialIdSet = new TreeSet<Long>();
		for (AdminOfficialProtos.OfficialSendPlan sendPlan : sendPlanMap.values()) {
			refOfficialIdSet.add(sendPlan.getOfficialId());
		}
		
		Map<Long, OfficialProtos.Official> refOfficialMap = OfficialUtil.getOfficial(
				hikariDataSource, jedisPool, this.profileManager.getProfile(head, "official:"), 
				companyId, refOfficialIdSet, ADMIN_STATE_SET);
		
		return Futures.immediateFuture(GetOfficialSendPlanByIdResponse.newBuilder()
				.addAllOfficialSendPlan(sendPlanMap.values())
				.addAllRefOfficial(refOfficialMap.values())
				.build());
	}

	@Override
	public ListenableFuture<CreateOfficialSendPlanResponse> createOfficialSendPlan(AdminHead head, CreateOfficialSendPlanRequest request) {
		if (!head.hasCompanyId()) {
			return Futures.immediateFuture(CreateOfficialSendPlanResponse.newBuilder()
					.setResult(CreateOfficialSendPlanResponse.Result.FAIL_UNKNOWN)
					.setFailText("company_id 参数未填")
					.build());
		}
		String failText = OfficialUtil.checkSendMessage(request.getSendMsg());
		if (failText != null) {
			return Futures.immediateFuture(CreateOfficialSendPlanResponse.newBuilder()
					.setResult(CreateOfficialSendPlanResponse.Result.FAIL_SEND_MSG_INVALID)
					.setFailText(failText)
					.build());
		}
		
		int now = (int) (System.currentTimeMillis() / 1000L);
		if (!request.getIsSendImmediately()) {
			if (!request.hasSendTime()) {
				return Futures.immediateFuture(CreateOfficialSendPlanResponse.newBuilder()
						.setResult(CreateOfficialSendPlanResponse.Result.FAIL_SEND_TIME_INVALID)
						.setFailText("发送消息时间未设置")
						.build());
			}
			if (request.getSendTime() <= now) {
				return Futures.immediateFuture(CreateOfficialSendPlanResponse.newBuilder()
						.setResult(CreateOfficialSendPlanResponse.Result.FAIL_SEND_TIME_INVALID)
						.setFailText("发送消息时间不能选择已经过去的时间点")
						.build());
			}
		}
		
		final long companyId = head.getCompanyId();
		final long officialId = request.getOfficialId();
		final OfficialProtos.Official official = OfficialUtil.getOfficial(
				hikariDataSource, jedisPool, this.profileManager.getProfile(head, "official:"), 
				companyId, Collections.singleton(officialId), ADMIN_STATE_SET).get(officialId);
		
		if (official == null) {
			return Futures.immediateFuture(CreateOfficialSendPlanResponse.newBuilder()
					.setResult(CreateOfficialSendPlanResponse.Result.FAIL_OFFICIAL_NOT_EXIST)
					.setFailText("该服务号不存在")
					.build());
		}
		
		final Integer allowModelId = request.hasAllowModelId() ? request.getAllowModelId() : null;
		
		if (allowModelId != null) {
			GetModelByIdResponse getModelByIdResponse = Futures.getUnchecked(allowService.getModelById(head, GetModelByIdRequest.newBuilder()
					.addModelId(allowModelId)
					.build()));
			if (getModelByIdResponse.getModelCount() <= 0) {
				return Futures.immediateFuture(CreateOfficialSendPlanResponse.newBuilder()
						.setResult(CreateOfficialSendPlanResponse.Result.FAIL_ALLOW_MODEL_NOT_EXIST)
						.setFailText("此访问模型不存在")
						.build());
			}
		}
		
		final OfficialProtos.OfficialMessage sendMsg = request.getSendMsg().toBuilder()
				.setMsgSeq(0)
				.setMsgTime(0)
				.setIsFromUser(false)
				.build();
		
		AdminOfficialProtos.OfficialSendPlan.Builder sendPlanBuilder = AdminOfficialProtos.OfficialSendPlan.newBuilder()
				.setPlanId(-1)
				.setOfficialId(officialId)
				.setSendTime(request.getIsSendImmediately() ? now : request.getSendTime())
				.setSendState(AdminOfficialProtos.OfficialSendPlan.SendState.WAIT_SEND)
				.setSendMsg(sendMsg)
				.setCreateAdminId(head.getSession().getAdminId())
				.setCreateTime(now);
		
		if (allowModelId != null) {
			sendPlanBuilder.setAllowModelId(allowModelId);
		}
		
		final int planId;
		Connection dbConn = null;
		try {
			dbConn = this.hikariDataSource.getConnection();
			sendPlanBuilder.setSendMsgRefId(OfficialDB.insertOfficialMsgRef(dbConn, Collections.singletonList(sendMsg)).get(0));		
			planId = OfficialDB.insertOfficialSendPlan(dbConn, companyId, Collections.singletonList(sendPlanBuilder.build())).get(0);
			OfficialDB.insertOfficialSendPlanAdminHead(dbConn, companyId, planId, head);
		} catch (SQLException e) {
			throw new RuntimeException("db fail", e);
		} finally {
			DBUtil.closeQuietly(dbConn);
		}
		
		if (request.getIsSendImmediately()) {
			this.serviceExecutor.execute(new OfficialMessageSendTask(companyId, planId));
		} else {
			this.scheduledExecutorService.schedule(new Runnable() {

				@Override
				public void run() {
					serviceExecutor.execute(new OfficialMessageSendTask(companyId, planId));
				}
				
			}, sendPlanBuilder.build().getSendTime() - now, TimeUnit.SECONDS);
			
			logger.info("schedule OfficialMessageSendTask : " + planId + ", " + new SimpleDateFormat("yyyyMMddHHmmss").format(new Date(sendPlanBuilder.build().getSendTime() * 1000L)));
		}
		
		return Futures.immediateFuture(CreateOfficialSendPlanResponse.newBuilder()
				.setResult(CreateOfficialSendPlanResponse.Result.SUCC)
				.setPlanId(planId)
				.build());
	}

	@Override
	public ListenableFuture<CancelOfficialSendPlanResponse> cancelOfficialSendPlan(AdminHead head, CancelOfficialSendPlanRequest request) {
		if (!head.hasCompanyId() || request.getPlanIdCount() <= 0) {
			return Futures.immediateFuture(CancelOfficialSendPlanResponse.newBuilder()
					.setResult(CancelOfficialSendPlanResponse.Result.SUCC)
					.build());
		}
		
		Connection dbConn = null;
		try {
			dbConn = this.hikariDataSource.getConnection();
			
			OfficialDB.setOfficialSendPlanStateCancel(dbConn, head.getCompanyId(), request.getPlanIdList(),
					head.getSession().getAdminId(), (int) (System.currentTimeMillis() / 1000L));

		} catch (SQLException e) {
			throw new RuntimeException("db fail", e);
		} finally {
			DBUtil.closeQuietly(dbConn);
		}
		
		return Futures.immediateFuture(CancelOfficialSendPlanResponse.newBuilder()
				.setResult(CancelOfficialSendPlanResponse.Result.SUCC)
				.build());
	}

	@Override
	public ListenableFuture<GetOfficialSendPlanListResponse> getOfficialSendPlanList(AdminHead head, GetOfficialSendPlanListRequest request) {
		if (!head.hasCompanyId()) {
			return Futures.immediateFuture(GetOfficialSendPlanListResponse.newBuilder()
					.setTotalSize(0)
					.setFilteredSize(0)
					.build());
		}
		final long companyId = head.getCompanyId();
		final int start = request.getStart() < 0 ? 0 : request.getStart();
		final int length = request.getLength() < 0 ? 0 : request.getLength() > 100 ? 100 : request.getLength();
		
		final DataPage<Integer> sendPlanIdPage;
		final Map<Integer, AdminOfficialProtos.OfficialSendPlan> sendPlanMap;
		Connection dbConn = null;
		try {
			dbConn = this.hikariDataSource.getConnection();
		
			if (request.hasOfficialId()) {
				sendPlanIdPage = OfficialDB.getOfficialSendPlanIdPage(dbConn, companyId, start, length, request.getOfficialId());
			} else {
				sendPlanIdPage = OfficialDB.getOfficialSendPlanIdPage(dbConn, companyId, start, length);
			}
			
			sendPlanMap = OfficialDB.getOfficialSendPlan(dbConn, companyId, sendPlanIdPage.dataList());
		} catch (SQLException e) {
			throw new RuntimeException("db fail", e);
		} finally {
			DBUtil.closeQuietly(dbConn);
		}
		
		Set<Long> refOfficialIdSet = new TreeSet<Long>();
		for (AdminOfficialProtos.OfficialSendPlan sendPlan : sendPlanMap.values()) {
			refOfficialIdSet.add(sendPlan.getOfficialId());
		}
		
		Map<Long, OfficialProtos.Official> refOfficialMap = OfficialUtil.getOfficial(
				hikariDataSource, jedisPool, this.profileManager.getProfile(head, "official:"), 
				companyId, refOfficialIdSet, ADMIN_STATE_SET);
		
		GetOfficialSendPlanListResponse.Builder responseBuilder = GetOfficialSendPlanListResponse.newBuilder();
		for (Integer sendPlanId : sendPlanIdPage.dataList()) {
			AdminOfficialProtos.OfficialSendPlan sendPlan = sendPlanMap.get(sendPlanId);
			if (sendPlan != null) {
				responseBuilder.addOfficialSendPlan(sendPlan);
			}
		}
		
		responseBuilder.setTotalSize(sendPlanIdPage.totalSize());
		responseBuilder.setFilteredSize(sendPlanIdPage.filteredSize());
		responseBuilder.addAllRefOfficial(refOfficialMap.values());
		
		return Futures.immediateFuture(responseBuilder.build());
	}

	@Override
	public ListenableFuture<GetOfficialMessageResponse> getOfficialMessage(AdminHead head, GetOfficialMessageRequest request) {
		if (!head.hasCompanyId()) {
			return Futures.immediateFuture(GetOfficialMessageResponse.newBuilder()
					.setTotalSize(0)
					.setFilteredSize(0)
					.build());
		}
		final long companyId = head.getCompanyId();
		final long officialId = request.getOfficialId();
		final int start = request.getStart() < 0 ? 0 : request.getStart();
		final int length = request.getLength() < 0 ? 0 : request.getLength() > 100 ? 100 : request.getLength();
		
		OfficialProtos.Official official = OfficialUtil.getOfficial(
				hikariDataSource, jedisPool, this.profileManager.getProfile(head, "official:"), 
				companyId, Collections.singleton(officialId), ADMIN_STATE_SET).get(officialId);
		
		if (official == null) {
			return Futures.immediateFuture(GetOfficialMessageResponse.newBuilder()
					.setTotalSize(0)
					.setFilteredSize(0)
					.build());
		}
		
		final DataPage<AdminOfficialProtos.OfficialMessageInfo> msgInfoPage;
		Connection dbConn = null;
		try {
			dbConn = this.hikariDataSource.getConnection();
			msgInfoPage = OfficialDB.getOfficialMessagePage(dbConn, companyId, 
					officialId, start, length, 
					request.hasUserId() ? request.getUserId() : null,
					request.hasIsFromUser() ? request.getIsFromUser() : null);
		} catch (SQLException e) {
			throw new RuntimeException("db fail", e);
		} finally {
			DBUtil.closeQuietly(dbConn);
		}
		
		return Futures.immediateFuture(GetOfficialMessageResponse.newBuilder()
				.addAllMsgInfo(msgInfoPage.dataList())
				.setTotalSize(msgInfoPage.totalSize())
				.setFilteredSize(msgInfoPage.filteredSize())
				.build());
	}
	
	private final class LoadOfficialMessageSendPlanTask implements Runnable {

		@Override
		public void run() {
			final Map<Long, Map<Integer, AdminOfficialProtos.OfficialSendPlan>> companyPlanMap = new TreeMap<Long, Map<Integer, AdminOfficialProtos.OfficialSendPlan>>();
			Connection dbConn = null;
			try {
				dbConn = hikariDataSource.getConnection();
				Map<Long, List<Integer>> companyToPlanIdMap = OfficialDB.getOfficialSendPlanIdListBySendState(dbConn, AdminOfficialProtos.OfficialSendPlan.SendState.WAIT_SEND);
				for (Entry<Long, List<Integer>> entry : companyToPlanIdMap.entrySet()) {
					companyPlanMap.put(entry.getKey(), OfficialDB.getOfficialSendPlan(dbConn, entry.getKey(), entry.getValue()));
				}
			} catch (SQLException e) {
				throw new RuntimeException("db fail", e);
			} finally {
				DBUtil.closeQuietly(dbConn);
			}
			
			int now = (int) (System.currentTimeMillis() / 1000L);
			
			for (Entry<Long, Map<Integer, AdminOfficialProtos.OfficialSendPlan>> entry : companyPlanMap.entrySet()) {
				final long companyId = entry.getKey();
				for (AdminOfficialProtos.OfficialSendPlan plan : entry.getValue().values()) {
					if (plan.getSendState() != AdminOfficialProtos.OfficialSendPlan.SendState.WAIT_SEND) {
						continue;
					}
					
					if (plan.getSendTime() <= now) {
						serviceExecutor.execute(new OfficialMessageSendTask(companyId, plan.getPlanId()));
					} else {
						final int planId = plan.getPlanId();
						scheduledExecutorService.schedule(new Runnable() {
	
							@Override
							public void run() {
								serviceExecutor.execute(new OfficialMessageSendTask(companyId, planId));
							}
							
						}, plan.getSendTime() - now, TimeUnit.SECONDS);
						
						logger.info("schedule OfficialMessageSendTask : " + planId + ", " + new SimpleDateFormat("yyyyMMddHHmmss").format(new Date(plan.getSendTime() * 1000L)));
					}
				}
			}
		}
		
	}
	
	private final class OfficialMessageSendTask implements Runnable {

		private final long companyId;
		private final int planId;
		
		OfficialMessageSendTask(long companyId, int planId) {
			this.companyId = companyId;
			this.planId = planId;
		}
		
		@Override
		public void run() {
			logger.info("send official message start : " + planId);
			try {
				final int now = (int) (System.currentTimeMillis() / 1000L);
				final AdminOfficialProtos.OfficialSendPlan sendPlan;
				final AdminProtos.AdminHead adminHead;
				Connection dbConn = null;
				try {
					dbConn = hikariDataSource.getConnection();
					sendPlan = OfficialDB.getOfficialSendPlan(dbConn, companyId, Collections.singleton(planId)).get(planId);
					
					if (sendPlan == null) {
						logger.error("cannot find send plan : " + planId);
						return;
					} 
					
					if (sendPlan.getSendState() != AdminOfficialProtos.OfficialSendPlan.SendState.WAIT_SEND) {
						logger.error("send plan invalid state : " + planId + ", " + sendPlan.getSendState());
						return;
					}
					
					if (sendPlan.getSendTime() > now) {
						logger.warn("send plan send time was modified! : " + planId + ", " + sendPlan.getSendTime() + ", " + now);
						// return;
					}
					
					adminHead = OfficialDB.getOfficialSendPlanAdminHead(dbConn, companyId, planId);
					if (adminHead == null) {
						logger.error("send plan admin head not found : " + planId);
						return;
					}
					
					boolean succ = OfficialDB.setOfficialSendPlanStateFinish(dbConn, companyId, planId);
					if (!succ) {
						logger.warn("send plan set finish fail : " + planId);
						return;
					}
				} finally {
					DBUtil.closeQuietly(dbConn);
				}
				
				final OfficialProtos.Official official = OfficialUtil.getOfficial(
						hikariDataSource, jedisPool, profileManager.getProfile(adminHead, "official:"), 
						companyId, Collections.singleton(sendPlan.getOfficialId()), ADMIN_STATE_SET).get(sendPlan.getOfficialId());
				
				if (official == null) {
					logger.error("send plan official not found : " + planId + ", " + sendPlan.getOfficialId());
					return;
				}
				
				if (official.getState() != OfficialProtos.State.NORMAL) {
					logger.error("send plan official state invalid!");
					return;
				}
				
				final Integer sendPlanModelId = sendPlan.hasAllowModelId() ? sendPlan.getAllowModelId() : null;
				final Integer officialModelId = official.hasAllowModelId() ? official.getAllowModelId() : null;
				
				Set<Long> sendUserIdSet = new TreeSet<Long>();
				
				int start = 0;
				final int length = 500;
				while (true) {
					GetUserListResponse getUserListResponse = Futures.getUnchecked(
							adminUserService.getUserList(adminHead, 
									GetUserListRequest.newBuilder()
										.setStart(start)
										.setLength(length)
										.build()));
					
					Set<Long> userIdSet = new TreeSet<Long>();
					for (UserProtos.User user : getUserListResponse.getUserList()) {
						userIdSet.add(user.getBase().getUserId());
					}
					
					if (sendPlanModelId == null && officialModelId == null) {
						sendUserIdSet.addAll(userIdSet);
					} else {
						CheckAllowRequest.Builder checkAllowRequestBuilder = CheckAllowRequest.newBuilder()
								.addAllUserId(userIdSet);
						if (sendPlanModelId != null) {
							checkAllowRequestBuilder.addModelId(sendPlanModelId);
						}
						if (officialModelId != null) {
							checkAllowRequestBuilder.addModelId(officialModelId);
						}
						
						CheckAllowRequest checkAllowRequest = checkAllowRequestBuilder.build();
						CheckAllowResponse checkAllowResponse = Futures.getUnchecked(allowService.checkAllow(adminHead, checkAllowRequest));
						
						for (int modelId : checkAllowRequest.getModelIdList()) {
							
							List<Long> allowUserIdList = Collections.emptyList();
							for (AllowProtos.CheckAllowResponse.CheckResult checkResult : checkAllowResponse.getCheckResultList()) {
								if (modelId == checkResult.getModelId()) {
									allowUserIdList = checkResult.getAllowUserIdList();
									break;
								}
							}
							
							userIdSet.retainAll(allowUserIdList);
						}
						
						sendUserIdSet.addAll(userIdSet);
					}
					
					start += length;
					if (start >= getUserListResponse.getFilteredSize()) {
						break;
					}
				}
				
				// push , each 1000 user push
				PushMsgRequest.Builder tmpRequestBuilder = PushMsgRequest.newBuilder();
				PushProtos.PushPacket.Builder tmpPacketBuilder = PushProtos.PushPacket.newBuilder();
				PushTarget.Builder tmpTargetBuilder = PushTarget.newBuilder();
				OfficialProtos.OfficialMessagePush.Builder tmpOfficialMessagePushBuilder = OfficialProtos.OfficialMessagePush.newBuilder();
				
				final int totalSize = sendUserIdSet.size();
				
				Iterator<Long> userIdIt = sendUserIdSet.iterator();
				int cnt = 0;
				while (userIdIt.hasNext()) {
					
					Set<Long> userIdSet = new TreeSet<Long>();
 					while (userIdIt.hasNext() && userIdSet.size() < 1000) {
 						userIdSet.add(userIdIt.next());
					}
					
					Map<Long, OfficialProtos.OfficialMessage> savedMsgMap = 
							OfficialUtil.saveOfficialMultiMessage(hikariDataSource, jedisPool, companyId, 
									official.getOfficialId(), userIdSet, sendPlan.getSendMsg(), sendPlan.getSendMsgRefId(), 
									now, false);
					
					tmpRequestBuilder.clear();
					
					for (Entry<Long, OfficialProtos.OfficialMessage> entry : savedMsgMap.entrySet()) {
						tmpRequestBuilder.addPushPacket(tmpPacketBuilder.clear()
								.addPushTarget(tmpTargetBuilder.clear()
									.setUserId(entry.getKey())
									.setEnableOffline(true)
									.build())
								.setPushName("OfficialMessagePush")
								.setPushBody(tmpOfficialMessagePushBuilder.clear()
										.setOfficialId(official.getOfficialId())
										.setMsg(entry.getValue())
										.build().toByteString())
								.build());
					}
					
					pushService.pushMsg(adminHead, tmpRequestBuilder.build());
					
					cnt += userIdSet.size();
					logger.info("send official message progress : " + planId + ", " + cnt + "/" + totalSize);
				}
				
				logger.info("send official message finish : " + planId);
			} catch (Throwable th) {
				logger.error("send official message error : " + planId, th);
			}
		}
		
	}
	
	@Override
	public ListenableFuture<SendSecretaryMessageResponse> sendSecretaryMessage(AdminHead head, SendSecretaryMessageRequest request) {
		return Futures.immediateFuture(this.doSendSecretaryMessage(head, null, null, request));
	}

	@Override
	public ListenableFuture<SendSecretaryMessageResponse> sendSecretaryMessage(RequestHead head, SendSecretaryMessageRequest request) {
		return Futures.immediateFuture(this.doSendSecretaryMessage(null, head, null, request));
	}
	
	@Override
	public ListenableFuture<SendSecretaryMessageResponse> sendSecretaryMessage(SystemHead head, SendSecretaryMessageRequest request) {
		return Futures.immediateFuture(this.doSendSecretaryMessage(null, null, head, request));
	}
	
	private SendSecretaryMessageResponse doSendSecretaryMessage(
			@Nullable AdminHead adminHead, @Nullable RequestHead requestHead, @Nullable SystemHead systemHead, 
			SendSecretaryMessageRequest request) {
		// check head
		if (adminHead == null && requestHead == null && systemHead == null) {
			throw new RuntimeException("no head");
		}
		
		final Long companyId;
		if (adminHead != null) {
			companyId = adminHead.hasCompanyId() ? adminHead.getCompanyId() : null;
		} else if (requestHead != null) {
			companyId = requestHead.getSession().getCompanyId();
		} else if (systemHead != null) {
			companyId = systemHead.hasCompanyId() ? systemHead.getCompanyId() : null;
		} else {
			companyId = null;
		}
		
		if (companyId == null) {
			return SendSecretaryMessageResponse.newBuilder()
					.setResult(SendSecretaryMessageResponse.Result.FAIL_UNKNOWN)
					.setFailText("company id not found")
					.build();
		}
		
		final Set<Long> userIdSet = new TreeSet<Long>(request.getUserIdList());
		final long officialId = AdminOfficialProtos.ReservedOfficialId.WEIZHU_SECRETARY_VALUE;
		
		if (userIdSet.isEmpty()) {
			return SendSecretaryMessageResponse.newBuilder()
					.setResult(SendSecretaryMessageResponse.Result.SUCC)
					.build();
		}
		
		String failText = OfficialUtil.checkSendMessage(request.getSendMsg());
		if (failText != null) {
			return SendSecretaryMessageResponse.newBuilder()
					.setResult(SendSecretaryMessageResponse.Result.FAIL_MSG_INVALID)
					.setFailText(failText)
					.build();
		}
		
		final ProfileManager.Profile profile;
		if (adminHead != null) {
			profile = this.profileManager.getProfile(adminHead, "official:");
		} else if (requestHead != null) {
			profile = this.profileManager.getProfile(requestHead, "official:");
		} else if (systemHead != null) {
			profile = this.profileManager.getProfile(systemHead, "official:");
		} else {
			throw new RuntimeException("no head");
		}
	
		final OfficialProtos.Official official = 
				OfficialUtil.getOfficial(hikariDataSource, jedisPool, profile, 
						companyId, Collections.singleton(officialId), ImmutableSet.of(OfficialProtos.State.NORMAL)).get(officialId);
		if (official == null) {
			return SendSecretaryMessageResponse.newBuilder()
					.setResult(SendSecretaryMessageResponse.Result.FAIL_UNKNOWN)
					.setFailText("服务号错误")
					.build();
		}
		
		if (userIdSet.size() == 1) {
			final long userId = userIdSet.iterator().next();
			OfficialProtos.OfficialMessage msg = OfficialUtil.saveOfficialSingleMessage(
					hikariDataSource, jedisPool, 
					companyId, officialId, userId,
					request.getSendMsg(),
					(int) (System.currentTimeMillis() / 1000L), false);
			
			if (msg == null) {
				throw new RuntimeException("cannot save official msg");
			}
			
			PushProtos.PushMsgRequest pushMsgRequest = PushProtos.PushMsgRequest.newBuilder()
					.addPushPacket(PushProtos.PushPacket.newBuilder()
							.addPushTarget(PushProtos.PushTarget.newBuilder()
									.setUserId(userId)
									.setEnableOffline(true)
									.build())
							.setPushName("OfficialMessagePush")
							.setPushBody(OfficialProtos.OfficialMessagePush.newBuilder()
									.setOfficialId(officialId)
									.setMsg(msg)
									.build().toByteString())
							.build())
					.build();
			
			if (adminHead != null) {
				this.pushService.pushMsg(adminHead, pushMsgRequest);
			} else if (requestHead != null) {
				this.pushService.pushMsg(requestHead, pushMsgRequest);
			} else if (systemHead != null) {
				this.pushService.pushMsg(systemHead, pushMsgRequest);
			} else {
				// ignore
			}
 		} else {
			
			long msgRefId;
			Connection dbConn = null;
			try {
				dbConn = this.hikariDataSource.getConnection();
				msgRefId = OfficialDB.insertOfficialMsgRef(dbConn, Collections.singletonList(request.getSendMsg())).get(0);
			} catch (SQLException e) {
				throw new RuntimeException("db fail", e);
			} finally {
				DBUtil.closeQuietly(dbConn);
			}
			
			Map<Long, OfficialProtos.OfficialMessage> msgMap = 
					OfficialUtil.saveOfficialMultiMessage(
							hikariDataSource, jedisPool, 
							companyId, officialId, userIdSet, 
							request.getSendMsg(), msgRefId, 
							(int) (System.currentTimeMillis() / 1000L), false);
			
			PushMsgRequest.Builder pushRequestBuilder = PushMsgRequest.newBuilder();
			PushProtos.PushPacket.Builder tmpPacketBuilder = PushProtos.PushPacket.newBuilder();
			PushTarget.Builder tmpTargetBuilder = PushTarget.newBuilder();
			OfficialProtos.OfficialMessagePush.Builder tmpOfficialMessagePushBuilder = OfficialProtos.OfficialMessagePush.newBuilder();
			
			for (Entry<Long, OfficialProtos.OfficialMessage> entry : msgMap.entrySet()) {
				pushRequestBuilder.addPushPacket(tmpPacketBuilder.clear()
						.addPushTarget(tmpTargetBuilder.clear()
							.setUserId(entry.getKey())
							.setEnableOffline(true)
							.build())
						.setPushName("OfficialMessagePush")
						.setPushBody(tmpOfficialMessagePushBuilder.clear()
								.setOfficialId(official.getOfficialId())
								.setMsg(entry.getValue())
								.build().toByteString())
						.build());
			}
			
			PushProtos.PushMsgRequest pushMsgRequest = pushRequestBuilder.build();
			
			if (adminHead != null) {
				this.pushService.pushMsg(adminHead, pushMsgRequest);
			} else if (requestHead != null) {
				this.pushService.pushMsg(requestHead, pushMsgRequest);
			} else if (systemHead != null) {
				this.pushService.pushMsg(systemHead, pushMsgRequest);
			} else {
				// ignore
			}
		}
		
		return SendSecretaryMessageResponse.newBuilder()
				.setResult(SendSecretaryMessageResponse.Result.SUCC)
				.build();
	}
	
}
