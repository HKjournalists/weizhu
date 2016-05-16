package com.weizhu.service.credits;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.inject.Inject;
import com.weizhu.common.db.DBUtil;
import com.weizhu.common.utils.DataPage;
import com.weizhu.proto.AdminCreditsProtos.AddCreditsRequest;
import com.weizhu.proto.AdminCreditsProtos.AddCreditsResponse;
import com.weizhu.proto.AdminCreditsProtos.ClearUserCreditsRequest;
import com.weizhu.proto.AdminCreditsProtos.ClearUserCreditsResponse;
import com.weizhu.proto.AdminCreditsProtos.CreateCreditsOrderRequest;
import com.weizhu.proto.AdminCreditsProtos.CreateCreditsOrderResponse;
import com.weizhu.proto.AdminCreditsProtos.CreditsOperation;
import com.weizhu.proto.AdminCreditsProtos.GetCreditsLogRequest;
import com.weizhu.proto.AdminCreditsProtos.GetCreditsLogResponse;
import com.weizhu.proto.AdminCreditsProtos.GetCreditsOperationRequest;
import com.weizhu.proto.AdminCreditsProtos.GetCreditsOperationResponse;
import com.weizhu.proto.AdminCreditsProtos.GetCreditsOrderRequest;
import com.weizhu.proto.AdminCreditsProtos.GetCreditsOrderResponse;
import com.weizhu.proto.AdminCreditsProtos.GetCreditsResponse;
import com.weizhu.proto.AdminCreditsProtos.GetCreditsRuleResponse;
import com.weizhu.proto.AdminCreditsProtos.GetExpenseCreditsResponse;
import com.weizhu.proto.AdminCreditsProtos.GetUserCreditsRequest;
import com.weizhu.proto.AdminCreditsProtos.GetUserCreditsResponse;
import com.weizhu.proto.AdminCreditsProtos.UpdateCreditsRuleRequest;
import com.weizhu.proto.AdminCreditsProtos.UpdateCreditsRuleResponse;
import com.weizhu.proto.AdminCreditsProtos.UserCreditsDelta;
import com.weizhu.proto.AdminCreditsService;
import com.weizhu.proto.AdminProtos.AdminHead;
import com.weizhu.proto.AdminUserProtos.GetUserByIdRequest;
import com.weizhu.proto.AdminUserProtos.GetUserByIdResponse;
import com.weizhu.proto.AdminUserService;
import com.weizhu.proto.CreditsProtos;
import com.weizhu.proto.UserProtos;
import com.weizhu.proto.WeizhuProtos.EmptyRequest;
import com.zaxxer.hikari.HikariDataSource;

public class AdminCreditsServiceImpl implements AdminCreditsService {

	private final HikariDataSource hikariDataSource;
	private final JedisPool jedisPool;
	private final AdminUserService adminUserService;
	
	@Inject
	public AdminCreditsServiceImpl(HikariDataSource hikariDataSource, JedisPool jedisPool, AdminUserService adminUserService) {
		this.hikariDataSource = hikariDataSource;
		this.jedisPool = jedisPool;
		this.adminUserService = adminUserService;
	}
	
	@Override
	public ListenableFuture<GetCreditsResponse> getCredits(AdminHead head, 
			EmptyRequest request) {
		// TODO: bug, 对应的companyId可能没有积分数据
		final long companyId = head.getCompanyId();
		
		Connection conn = null;
		try {
			conn = this.hikariDataSource.getConnection();
			
			return Futures.immediateFuture(GetCreditsResponse.newBuilder()
					.setCredits(CreditsDB.getCredits(conn, companyId))
					.build());
		} catch (SQLException ex) {
			throw new RuntimeException("db fail", ex);
		} finally {
			DBUtil.closeQuietly(conn);
		}
	}

	@Override
	public ListenableFuture<AddCreditsResponse> addCredits(AdminHead head,
			AddCreditsRequest request) {
		if (!head.hasCompanyId()) {
			return Futures.immediateFuture(AddCreditsResponse.newBuilder()
					.setResult(AddCreditsResponse.Result.FAIL_UNKNOWN)
					.setFailText("company_id 参数未填")
					.build());
		}
		final long companyId = head.getCompanyId();
		final long creditsDelta = request.getCreditsDelta();
		
		final String desc = request.hasDesc() ? request.getDesc() : null;
		final long adminId = head.getSession().getAdminId();
		final int now = (int) (System.currentTimeMillis() / 1000L);
		
		Connection conn = null;
		try {
			conn = this.hikariDataSource.getConnection();
			
			long credits = creditsDelta + CreditsDB.getCredits(conn, companyId); // 取出库中存的分值
			CreditsDB.updateCredits(conn, companyId, credits); // 更新分值
			CreditsDB.insertCreditsLog(conn, companyId, creditsDelta, desc, adminId, now); // 保存操作日志
			
			return Futures.immediateFuture(AddCreditsResponse.newBuilder()
					.setResult(AddCreditsResponse.Result.SUCC)
					.build());
		} catch (SQLException ex) {
			throw new RuntimeException("db fail", ex);
		} finally {
			DBUtil.closeQuietly(conn);
		}
		
	}

	@Override
	public ListenableFuture<GetCreditsLogResponse> getCreditsLog(
			AdminHead head, GetCreditsLogRequest request) {
		if (!head.hasCompanyId()) {
			return Futures.immediateFuture(GetCreditsLogResponse.newBuilder()
					.setTotal(0)
					.setFilteredSize(0)
					.build());
		}
		final long companyId = head.getCompanyId();
		
		final int start = request.getStart() < 0 ? 0 : request.getStart();
		final int length = request.getLength() < 0 ? 0 : request.getLength() > 50 ? 50 : request.getLength();
		
		Connection conn = null;
		try {
			conn = this.hikariDataSource.getConnection();
			
			DataPage<GetCreditsLogResponse.CreditsLog> creditsLogPage = CreditsDB.getCreditsLog(conn, companyId, start, length); // 获取积分日志信息
		
			return Futures.immediateFuture(GetCreditsLogResponse.newBuilder()
					.addAllCreditsLog(creditsLogPage.dataList())
					.setTotal(creditsLogPage.totalSize())
					.setFilteredSize(creditsLogPage.filteredSize())
					.build());
		} catch (SQLException ex) {
			throw new RuntimeException("db fail", ex);
		} finally {
			DBUtil.closeQuietly(conn);
		}
	}
	
	@Override
	public ListenableFuture<GetUserCreditsResponse> getUserCredits(AdminHead head,
			GetUserCreditsRequest request) {
		if (!head.hasCompanyId()) {
			return Futures.immediateFuture(GetUserCreditsResponse.newBuilder()
					.setTotal(0)
					.setFilteredSize(0)
					.build());
		}
		
		final long companyId = head.getCompanyId();
		
		final List<Long> userIdList = request.getUserIdList();
		final int start = request.getStart() < 0 ? 0 : request.getStart();
		final int length = request.getLength() < 0 ? 0 : request.getLength() > 50 ? 50 : request.getLength();
		
		Connection conn = null;
		try {
			conn = this.hikariDataSource.getConnection();
			
			DataPage<CreditsProtos.Credits> creditsPage = CreditsDB.getCreditsPage(conn, companyId, userIdList, start, length);
			
			return Futures.immediateFuture(GetUserCreditsResponse.newBuilder()
					.addAllCredits(creditsPage.dataList())
					.setTotal(creditsPage.totalSize())
					.setFilteredSize(creditsPage.filteredSize())
					.build());
		} catch (SQLException ex) {
			throw new RuntimeException("db fail", ex);
		} finally {
			DBUtil.closeQuietly(conn);
		}
		
	}

	@Override
	public ListenableFuture<CreateCreditsOrderResponse> createCreditsOrder(
			AdminHead head, CreateCreditsOrderRequest request) {
		if (!head.hasCompanyId()) {
			return Futures.immediateFuture(CreateCreditsOrderResponse.newBuilder()
					.setResult(CreateCreditsOrderResponse.Result.FAIL_UNKNOWN)
					.setFailText("company_id 参数未填")
					.build());
		}
		final long companyId = head.getCompanyId();
		
		final List<UserCreditsDelta> userCreditsDeltaList = request.getUserCreditsDeltaList();
		final String desc = request.getDesc();
		if (desc.length() > 191) {
			return Futures.immediateFuture(CreateCreditsOrderResponse.newBuilder()
					.setResult(CreateCreditsOrderResponse.Result.FAIL_DESC_INVALID)
					.setFailText("事由过长")
					.build());
		}
		
		final long adminId = head.getSession().getAdminId();
	
		final int now = (int) (System.currentTimeMillis() / 1000L);
		
		// 过滤掉不存在的userId
		List<Long> userIdList = new ArrayList<Long>();
		for (UserCreditsDelta userCreditsDelta : userCreditsDeltaList) {
			userIdList.add(userCreditsDelta.getUserId());
		}

		// 过滤不存在的userId
		GetUserByIdRequest getUserByIdRequest = GetUserByIdRequest.newBuilder()
				.addAllUserId(userIdList)
				.build();
		GetUserByIdResponse getUserByIdResponse = Futures.getUnchecked(adminUserService.getUserById(head, getUserByIdRequest));
		List<Long> validUserList = new ArrayList<Long>();
		for (UserProtos.User user : getUserByIdResponse.getUserList()) {
			validUserList.add(user.getBase().getUserId());
		}
		
		// 获取合法用户对应的所有分值
		Map<Long, CreditsProtos.Credits> userCreditsMap = CreditsUtil.getUserCredits(hikariDataSource, jedisPool, companyId, validUserList);
		
		long creditsTotal = 0;
		
		CreditsProtos.CreditsOrder.Builder creditsOrderBuilder = CreditsProtos.CreditsOrder.newBuilder();
		List<CreditsProtos.CreditsOrder> creditsOrderList = new ArrayList<CreditsProtos.CreditsOrder>();
		
		List<CreditsProtos.Credits> creditsList = new ArrayList<CreditsProtos.Credits>();
		
		for (UserCreditsDelta userCreditsDelta : userCreditsDeltaList) {
			long userId = userCreditsDelta.getUserId();
			long creditsDelta = userCreditsDelta.getCreditsDelta();
			
			if (!validUserList.contains(userId)) {
				continue;
			}
			creditsTotal += creditsDelta;
			creditsOrderBuilder.clear();
			
			creditsOrderBuilder
			.setOrderId(0)
			.setUserId(userCreditsDelta.getUserId())
			.setType(CreditsProtos.CreditsOrder.Type.ADMIN_INCOME)
			.setCreditsDelta(userCreditsDelta.getCreditsDelta())
			.setDesc(desc)
			.setState(CreditsProtos.CreditsOrder.State.SUCCESS)
			.setCreateAdmin(adminId)
			.setCreateTime(now);
			
			creditsOrderList.add(creditsOrderBuilder.build());
			
			// 加分之后的用户积分
			CreditsProtos.Credits tmpUserCredits = userCreditsMap.get(userId);
			CreditsProtos.Credits userCredits = null;
			if (tmpUserCredits == null) {
				userCredits = CreditsProtos.Credits.newBuilder()
						.setUserId(userId)
						.setCredits(0 + creditsDelta)
						.build();
			} else {
				userCredits = CreditsProtos.Credits.newBuilder()
						.setUserId(userId)
						.setCredits(tmpUserCredits.getCredits() + creditsDelta)
						.build();
			}
			creditsList.add(userCredits);
		}
		
		int operationId = 0;
		
		Connection conn = null;
		try {
			conn = this.hikariDataSource.getConnection();
			
			// 取出可用积分
			long credits = CreditsDB.getCredits(conn, companyId);
			if (credits < creditsTotal) {
				return Futures.immediateFuture(CreateCreditsOrderResponse.newBuilder()
						.setResult(CreateCreditsOrderResponse.Result.FAIL_CREDITS_DELTA_INVALID)
						.setFailText("可用积分不足")
						.build());
			}
			operationId = CreditsDB.insertCreditsOperation(conn, companyId, desc, creditsTotal, now, adminId);
			CreditsDB.insertCreditsOrder(conn, companyId, creditsOrderList, now, adminId, null, operationId, null);
			CreditsDB.updateUserCredits(conn, companyId, creditsList);
			CreditsDB.updateCredits(conn, companyId, credits - creditsTotal);
		} catch (SQLException ex) {
			throw new RuntimeException("db fail", ex);
		} finally {
			DBUtil.closeQuietly(conn);
		}
		
		Jedis jedis = jedisPool.getResource();
		try {
			CreditsCache.delCredits(jedis, companyId, validUserList);
		} finally {
			jedis.close();
		}
		
		return Futures.immediateFuture(CreateCreditsOrderResponse.newBuilder()
				.setResult(CreateCreditsOrderResponse.Result.SUCC)
				.setOpterationId(operationId)
				.build());
	}

	@Override
	public ListenableFuture<ClearUserCreditsResponse> clearUserCredits(
			AdminHead head, ClearUserCreditsRequest request) {
		if (!head.hasCompanyId()) {
			return Futures.immediateFuture(ClearUserCreditsResponse.newBuilder()
					.setResult(ClearUserCreditsResponse.Result.FAIL_UNKNOWN)
					.setFailText("company_id 参数未填")
					.build());
		}
		final long companyId = head.getCompanyId();
		final List<Long> userIdList = request.getUserIdList();
		
		Map<Long, CreditsProtos.Credits> creditsMap = CreditsUtil.getUserCredits(hikariDataSource, jedisPool, companyId, userIdList);
		long creditsTotal = 0;
		for (CreditsProtos.Credits credits : creditsMap.values()) {
			creditsTotal += credits.getCredits();
		}
		
		Connection conn = null;
		try {
			conn = this.hikariDataSource.getConnection();
			
			// 取出积分
			long credits = CreditsDB.getCredits(conn, companyId);
			// 清空积分
			CreditsDB.clearUserCredits(conn, companyId, userIdList);
			// 更新总积分
			CreditsDB.updateCredits(conn, companyId, credits + creditsTotal);

		} catch (SQLException ex) {
			throw new RuntimeException("db fail", ex);
		} finally {
			DBUtil.closeQuietly(conn);
		}
		
		Jedis jedis = jedisPool.getResource();
		try {
			CreditsCache.delCredits(jedis, companyId, userIdList);
		} finally {
			jedis.close();
		}
		
		return Futures.immediateFuture(ClearUserCreditsResponse.newBuilder()
				.setResult(ClearUserCreditsResponse.Result.SUCC)
				.build());
	}

	@Override
	public ListenableFuture<GetCreditsRuleResponse> getCreditsRule(
			AdminHead head, EmptyRequest request) {
		// TODO: bug, companyId可能对应无数据
		final long companyId = head.getCompanyId();
		
		Connection conn = null;
		try {
			conn = this.hikariDataSource.getConnection();
			
			return Futures.immediateFuture(GetCreditsRuleResponse.newBuilder()
					.setCreditsRule(CreditsDB.getCreditsRule(conn, companyId))
					.build());
		} catch (SQLException ex) {
			throw new RuntimeException("db fail", ex);
		} finally {
			DBUtil.closeQuietly(conn);
		}
	}

	@Override
	public ListenableFuture<UpdateCreditsRuleResponse> updateCreditsRule(
			AdminHead head, UpdateCreditsRuleRequest request) {
		if (!head.hasCompanyId()) {
			return Futures.immediateFuture(UpdateCreditsRuleResponse.newBuilder()
					.setResult(UpdateCreditsRuleResponse.Result.FAIL_UNKNOWN)
					.setFailText("company_id 参数未填")
					.build());
		}
		final long companyId = head.getCompanyId();
		final String creditsRule = request.getCreditsRule();
		
		Connection conn = null;
		try {
			conn = this.hikariDataSource.getConnection();
			
			CreditsDB.updateCreditsRule(conn, companyId, creditsRule);

			return Futures.immediateFuture(UpdateCreditsRuleResponse.newBuilder()
					.setResult(UpdateCreditsRuleResponse.Result.SUCC)
					.build());
		} catch (SQLException ex) {
			throw new RuntimeException("db fail", ex);
		} finally {
			DBUtil.closeQuietly(conn);
		}
	}

	@Override
	public ListenableFuture<GetCreditsOrderResponse> getCreditsOrder(
			AdminHead head, GetCreditsOrderRequest request) {
		if (!head.hasCompanyId()) {
			return Futures.immediateFuture(GetCreditsOrderResponse.newBuilder()
					.setTotal(0)
					.setFilteredSize(0)
					.build());
		}
		final long companyId = head.getCompanyId();
		final List<Long> userIdList = request.getUserIdList();
		final boolean isExpense = request.hasIsExpense() ? request.getIsExpense() : false;
		final Integer startTime = request.hasStartTime() ? request.getStartTime() : null;
		final Integer endTime = request.hasEndTime() ? request.getEndTime() : null;
		
		final int start = request.getStart() < 0 ? 0 : request.getStart();
		final int length = request.getLength() < 0 ? 0 : request.getLength() > 50 ? 50 : request.getLength();
		
		Connection conn = null;
		try {
			conn = this.hikariDataSource.getConnection();
			
			DataPage<CreditsProtos.CreditsOrder> creditsOrderPage = CreditsDB.getCreditsOrderPage(conn, companyId, userIdList, isExpense, startTime, endTime, start, length);
		
			return Futures.immediateFuture(GetCreditsOrderResponse.newBuilder()
					.addAllCreditsOrder(creditsOrderPage.dataList())
					.setTotal(creditsOrderPage.totalSize())
					.setFilteredSize(creditsOrderPage.filteredSize())
					.build());
		} catch (SQLException ex) {
			throw new RuntimeException("db fail", ex);
		} finally {
			DBUtil.closeQuietly(conn);
		}
		
	}

	@Override
	public ListenableFuture<GetCreditsOperationResponse> getCreditsOperation(
			AdminHead head, GetCreditsOperationRequest request) {
		if (!head.hasCompanyId()) {
			return Futures.immediateFuture(GetCreditsOperationResponse.newBuilder()
					.setTotal(0)
					.setFilteredSize(0)
					.build());
		}
		final long companyId = head.getCompanyId();
		
		final int start = request.getStart() < 0 ? 0 : request.getStart();
		final int length = request.getLength() < 0 ? 0 : request.getLength() > 50 ? 50 : request.getLength();
		
		Connection conn = null;
		try {
			conn = this.hikariDataSource.getConnection();
			
			DataPage<CreditsOperation> operationPage = CreditsDB.getCreditsOperation(conn, companyId, start, length);
			
			List<Integer> operationIdList = new ArrayList<Integer>();
			for (CreditsOperation creditsOrderOperation : operationPage.dataList()) {
				operationIdList.add(creditsOrderOperation.getOperationId());
			}
			
			Map<Integer, List<UserCreditsDelta>> userCreditsDeltaMap = CreditsDB.getUserCreditsDelta(conn, companyId, operationIdList);
			
			List<CreditsOperation> CreditsOperationList = new ArrayList<CreditsOperation>();
			for (CreditsOperation creditsOrderOperation : operationPage.dataList()) {
				int operationId = creditsOrderOperation.getOperationId();
				List<UserCreditsDelta> creditsDeltaList = userCreditsDeltaMap.get(operationId);
				
				CreditsOperation.Builder operationBuilder = CreditsOperation.newBuilder();
				if (creditsDeltaList != null) {
					operationBuilder.addAllUserCreditsDelta(creditsDeltaList);
				}
				CreditsOperationList.add(operationBuilder.mergeFrom(creditsOrderOperation).build());
			}
			
			return Futures.immediateFuture(GetCreditsOperationResponse.newBuilder()
					.addAllCreditsOperation(CreditsOperationList)
					.setTotal(operationPage.totalSize())
					.setFilteredSize(operationPage.filteredSize())
					.build());
					
		} catch (SQLException ex) {
			throw new RuntimeException("db fail", ex);
		} finally {
			DBUtil.closeQuietly(conn);
		}
		
	}

	@Override
	public ListenableFuture<GetExpenseCreditsResponse> getExpenseCredits(
			AdminHead head, EmptyRequest request) {
		// TODO : bug, companyId可能对应的数据为空
		final long companyId = head.getCompanyId();
		
		Connection conn = null;
		try {
			conn = this.hikariDataSource.getConnection();
			
			int expenseCredits = CreditsDB.getExpenseCredits(conn, companyId);
			
			return Futures.immediateFuture(GetExpenseCreditsResponse.newBuilder()
					.setCredits(expenseCredits)
					.build());
		} catch (SQLException ex) {
			throw new RuntimeException("db fail", ex);
		} finally {
			DBUtil.closeQuietly(conn);
		}
	}

}
