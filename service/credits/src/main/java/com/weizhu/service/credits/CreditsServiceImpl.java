package com.weizhu.service.credits;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import com.google.common.base.Splitter;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.inject.Inject;
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import com.weizhu.common.db.DBUtil;
import com.weizhu.common.utils.ProfileManager;
import com.weizhu.proto.CreditsProtos.GetCreditsRuleResponse;
import com.weizhu.proto.CreditsProtos;
import com.weizhu.proto.CreditsProtos.DuibaConsumeCreditsRequest;
import com.weizhu.proto.CreditsProtos.DuibaConsumeCreditsResponse;
import com.weizhu.proto.CreditsProtos.DuibaNotifyRequest;
import com.weizhu.proto.CreditsProtos.DuibaNotifyResponse;
import com.weizhu.proto.CreditsProtos.DuibaShopUrlRequest;
import com.weizhu.proto.CreditsProtos.DuibaShopUrlResponse;
import com.weizhu.proto.CreditsProtos.GetCreditsOrderRequest;
import com.weizhu.proto.CreditsProtos.GetCreditsOrderResponse;
import com.weizhu.proto.CreditsProtos.GetCreditsResponse;
import com.weizhu.proto.CreditsService;
import com.weizhu.proto.WeizhuProtos.AnonymousHead;
import com.weizhu.proto.WeizhuProtos.EmptyRequest;
import com.weizhu.proto.WeizhuProtos.RequestHead;
import com.zaxxer.hikari.HikariDataSource;

public class CreditsServiceImpl implements CreditsService {
	
	private final HikariDataSource hikariDataSource;
	private final JedisPool jedisPool;
	private final ProfileManager profileManager;
	
	private static final Splitter SPLITTER = Splitter.on(':').trimResults().omitEmptyStrings();
	
	private static final ProfileManager.ProfileKey<String> WEIZHU_APP_KEY = 
			ProfileManager.createKey("credits:app_key", (String) null);
	private static final ProfileManager.ProfileKey<String> WEIZHU_APP_SECRET = 
			ProfileManager.createKey("credits:app_secret", (String) null);
	
	@Inject
	public CreditsServiceImpl(HikariDataSource hikariDataSource, JedisPool jedisPool, ProfileManager profileManager) {
		this.hikariDataSource = hikariDataSource;
		this.jedisPool = jedisPool;
		this.profileManager = profileManager;
	}

	@Override
	public ListenableFuture<DuibaShopUrlResponse> duibaShopUrl(
			RequestHead head, DuibaShopUrlRequest request) {
		final long companyId = head.getSession().getCompanyId();
		final long userId = head.getSession().getUserId();
		
		CreditsProtos.Credits credits = CreditsUtil.getUserCredits(hikariDataSource, jedisPool, companyId, Collections.singleton(userId)).get(userId);
		
		long userCredits = 0;
		if (credits != null) {
			userCredits = credits.getCredits();
		}
		
		ProfileManager.Profile profile = this.profileManager.getProfile(head, "credits:");
		String appKey = profile.get(WEIZHU_APP_KEY);
		String appSecret = profile.get(WEIZHU_APP_SECRET);
		
		final String redirect = request.hasRedirect() ? request.getRedirect() : null;
		final long timestamp = System.currentTimeMillis();
		
		Map<String, String> paramMap = new TreeMap<String, String>();
		paramMap.put("appKey", appKey);
		paramMap.put("appSecret", appSecret);
		paramMap.put("credits", String.valueOf(userCredits));
		paramMap.put("uid", companyId + ":" + userId);
		paramMap.put("timestamp", String.valueOf(timestamp));
		if (redirect != null && !redirect.isEmpty()) {
			paramMap.put("redirect", redirect);
		}
		
		StringBuilder param = new StringBuilder();
		StringBuilder url = new StringBuilder("http://www.duiba.com.cn/autoLogin/autologin?");
		for (Entry<String, String> entry : paramMap.entrySet()) {
			param.append(entry.getValue());
			if (!entry.getKey().equals("appSecret")) {
				url.append(entry.getKey()).append("=").append(entry.getValue()).append("&");
			}
		}
		
		String sign = CreditsUtil.createMD5(param.toString());
		url.append("sign=").append(sign);
		
		return Futures.immediateFuture(DuibaShopUrlResponse.newBuilder()
				.setUrl(url.toString())
				.build());
	}
	
	@Override
	public ListenableFuture<DuibaConsumeCreditsResponse> duibaConsumeCredits(AnonymousHead head, DuibaConsumeCreditsRequest request) {
		final String uid = request.getUid();
		final long   credits = request.getCredits();
		final String appKey = request.getAppKey();
		final String timestamp = request.getTimeStamp();
		final String description = request.hasDescription() ? request.getDescription() : null;
		final String orderNum = request.getOrderNum();
		final String type = request.getType();
		final Integer facePrice = request.hasFacePrice() ? request.getFacePrice() : null;
		final Integer actualPrice = request.getActualPrice();
		final String ip = request.hasIp() ? request.getIp() : null;
		final Boolean waitAudit = request.hasWaitAudit() ? request.getWaitAudit() : null;
		final String params = request.hasParams() ? request.getParams() : null;
		final String sign = request.getSign();
		
		Long userId;
		try {
			List<String> list = SPLITTER.splitToList(uid);
			userId = list.size() > 1 ? Long.parseLong(list.get(1)) : null;
		} catch (NumberFormatException e) {
			userId = null;
		}
		
		ProfileManager.Profile profile = this.profileManager.getProfile(head, "credits:");
		String appSecret = profile.get(WEIZHU_APP_SECRET);
		
		Map<String, String> paramMap = new TreeMap<String, String>();
		paramMap.put("uid", uid);
		paramMap.put("credits", String.valueOf(credits));
		paramMap.put("appKey", appKey);
		paramMap.put("timestamp", timestamp);
		paramMap.put("description", description);
		paramMap.put("orderNum", orderNum);
		paramMap.put("type", type);
		paramMap.put("facePrice", String.valueOf(facePrice));
		paramMap.put("actualPrice", String.valueOf(actualPrice));
		paramMap.put("ip", ip);
		paramMap.put("waitAudit", String.valueOf(waitAudit));
		paramMap.put("params", params);
		paramMap.put("appSecret", appSecret);
		
		StringBuilder param = new StringBuilder();
		for (String values : paramMap.values()) {
			param.append(values);
		}
		
		final long companyId = head.getCompanyId();
		
		if (userId == null) {
			return Futures.immediateFuture(DuibaConsumeCreditsResponse.newBuilder()
					.setStatus("fail")
					.setErrorMessage("兑换商品失败，请联系管理员！")
					.setBizId(companyId + ":" + 0)
					.setCredits(0)
					.build());
		}
		
		CreditsProtos.Credits tmpCredits = CreditsUtil.getUserCredits(hikariDataSource, jedisPool, companyId, Collections.singleton(userId)).get(userId);
		long creditsLeft = 0;
		if (tmpCredits != null) {
			creditsLeft = tmpCredits.getCredits();
		}
		
		if (!CreditsUtil.createMD5(param.toString()).equals(sign)) {
			return Futures.immediateFuture(DuibaConsumeCreditsResponse.newBuilder()
					.setStatus("fail")
					.setErrorMessage("兑换商品失败，请联系管理员！")
					.setBizId(companyId + ":" + 0)
					.setCredits(creditsLeft)
					.build());
		}
		
		// 扣除用户积分
		CreditsProtos.CreditsOrder.Builder creditsOrderBuilder = CreditsProtos.CreditsOrder.newBuilder()
				.setOrderId(0)
				.setUserId(userId)
				.setType(CreditsProtos.CreditsOrder.Type.EXPENSE)
				.setCreditsDelta(- credits)
				.setDesc(description)
				.setState(CreditsProtos.CreditsOrder.State.CONFIRM);
		boolean isEnough = (credits <= creditsLeft);
		if (!isEnough) {
			creditsOrderBuilder.setState(CreditsProtos.CreditsOrder.State.FAIL);
		}
		
		final int now = (int) (System.currentTimeMillis() / 1000L);
		
		int orderId = 0;
		Connection conn = null;
		try {
			conn = this.hikariDataSource.getConnection();
			
			orderId = CreditsDB.insertCreditsOrder(conn, companyId, Collections.singleton(creditsOrderBuilder.build()), now, 0L, orderNum, null, params).get(0); // 微助操作的 admin_id = 0
			if (orderId != 0 && !creditsOrderBuilder.getState().equals(CreditsProtos.CreditsOrder.State.FAIL)) {
				CreditsProtos.Credits cds = CreditsProtos.Credits.newBuilder()
						.setUserId(userId)
						.setCredits(creditsLeft - credits)
						.build();
				CreditsDB.updateUserCredits(conn, companyId, Collections.singleton(cds));
			}
		} catch (SQLException ex) {
			throw new RuntimeException("db fail", ex);
		} finally {
			DBUtil.closeQuietly(conn);
		}
		
		Jedis jedis = jedisPool.getResource();
		try {
			CreditsCache.delCredits(jedis, companyId, Collections.singleton(userId));
		} finally {
			jedis.close();
		}
		
		return Futures.immediateFuture(DuibaConsumeCreditsResponse.newBuilder()
				.setBizId(companyId + ":" + orderId)
				.setStatus("ok")
				.setCredits(creditsLeft - credits)
				.build());
	}

	@Override
	public ListenableFuture<DuibaNotifyResponse> duibaNotify(AnonymousHead head, DuibaNotifyRequest request) {
		final String appKey = request.getAppKey();
		final long timestamp = request.getTimeStamp();
		final boolean success = request.getSuccess();
		final String errorMessage = request.hasErrorMessage() ? request.getErrorMessage() : null;
		final String orderNum = request.getOrderNum();
		final String bizId = request.hasBizId() ? request.getBizId() : null;
		final String sign = request.getSign();
		final String uid = request.getUid();
		
		ProfileManager.Profile profile = this.profileManager.getProfile(head, "credits:");
		String appSecret = profile.get(WEIZHU_APP_SECRET);
		
		Map<String, String> paramMap = new TreeMap<String, String>();
		paramMap.put("appKey", appKey);
		paramMap.put("timestamp", String.valueOf(timestamp));
		paramMap.put("success", String.valueOf(success));
		paramMap.put("uid", uid);
		if (errorMessage != null && !errorMessage.isEmpty()) {
			paramMap.put("errorMessage", errorMessage);
		}
		paramMap.put("orderNum", orderNum);
		if (bizId != null && !bizId.isEmpty()) {
			paramMap.put("bizId", bizId);
		}
		paramMap.put("appSercet", appSecret);
		
		StringBuilder param = new StringBuilder();
		for (String value : paramMap.values()) {
			param.append(value);
		}
		
		if (!CreditsUtil.createMD5(param.toString()).equals(sign)) {
			return Futures.immediateFuture(DuibaNotifyResponse.newBuilder()
					.setResult(DuibaNotifyResponse.Result.FAIL)
					.setFailText("验签失败")
					.build());
		}
		
		CreditsProtos.CreditsOrder creditsOrder = null;
		final long companyId = head.getCompanyId();
		
		Connection conn = null;
		try {
			conn = this.hikariDataSource.getConnection();
			
			creditsOrder = CreditsDB.getCreditsOrderByOrderNum(conn, Collections.singleton(orderNum)).get(orderNum); // orderNum是第三方唯一的值，不需要companyId
			if (creditsOrder == null) {
				return Futures.immediateFuture(DuibaNotifyResponse.newBuilder()
						.setResult(DuibaNotifyResponse.Result.FAIL)
						.setFailText("不存在的order num")
						.build());
			}
			
			// 如果有状态，直接返回
			if (creditsOrder.getState().equals(CreditsProtos.CreditsOrder.State.FAIL) || creditsOrder.getState().equals(CreditsProtos.CreditsOrder.State.SUCCESS)) {
				return Futures.immediateFuture(DuibaNotifyResponse.newBuilder()
						.setResult(DuibaNotifyResponse.Result.SUCC)
						.build());
			}
			if (creditsOrder.getState().equals(CreditsProtos.CreditsOrder.State.CONFIRM) && success) {
				CreditsDB.updateCreditsOrder(conn, companyId, orderNum, CreditsProtos.CreditsOrder.State.SUCCESS);
				
				return Futures.immediateFuture(DuibaNotifyResponse.newBuilder()
						.setResult(DuibaNotifyResponse.Result.SUCC)
						.build());
			}
		} catch (SQLException ex) {
			throw new RuntimeException("db fail", ex);
		} finally {
			DBUtil.closeQuietly(conn);
		}
		
		// 把积已经扣除的积分退给用户
		long userId = creditsOrder.getUserId();
		CreditsProtos.Credits credits = CreditsUtil.getUserCredits(hikariDataSource, jedisPool, companyId, Collections.singleton(userId)).get(userId);
		
		long creditsDelta = creditsOrder.getCreditsDelta();
		
		CreditsProtos.CreditsOrder.Builder creditsOrderBuilder = CreditsProtos.CreditsOrder.newBuilder()
				.setOrderId(0)
				.setUserId(userId)
				.setType(CreditsProtos.CreditsOrder.Type.WEIZHU_INCOME)
				.setCreditsDelta(- creditsDelta)
				.setDesc("兑换失败,积分退回")
				.setState(CreditsProtos.CreditsOrder.State.SUCCESS);
		
		final int now = (int) (System.currentTimeMillis() / 1000L);
		
		List<Integer> orderIdList = new ArrayList<Integer>();
		try {
			conn = this.hikariDataSource.getConnection();
			
			CreditsDB.updateCreditsOrder(conn, companyId, orderNum, CreditsProtos.CreditsOrder.State.FAIL);
			orderIdList = CreditsDB.insertCreditsOrder(conn, companyId, Collections.singleton(creditsOrderBuilder.build()), now, 0L, null, null, null); // 微助操作的 admin_id = 0
			if (orderIdList.size() != 0) {
				CreditsProtos.Credits cds = CreditsProtos.Credits.newBuilder()
						.setUserId(userId)
						.setCredits(credits.getCredits() - creditsDelta)
						.build();
				CreditsDB.updateUserCredits(conn, companyId, Collections.singleton(cds));
			}
		} catch (SQLException ex) {
			throw new RuntimeException("db fail", ex);
		} finally {
			DBUtil.closeQuietly(conn);
		}
		
		Jedis jedis = this.jedisPool.getResource();
		try {
			CreditsCache.delCredits(jedis, companyId, Collections.singleton(userId));
		} finally {
			jedis.close();
		}
		
		return Futures.immediateFuture(DuibaNotifyResponse.newBuilder()
				.setResult(DuibaNotifyResponse.Result.SUCC)
				.build());
	}

	@Override
	public ListenableFuture<GetCreditsResponse> getCredits(RequestHead head,
			EmptyRequest request) {
		final long companyId = head.getSession().getCompanyId();
		final long userId = head.getSession().getUserId();
		
		CreditsProtos.Credits credits = CreditsUtil.getUserCredits(hikariDataSource, jedisPool, companyId, Collections.singleton(userId)).get(userId);
		if (credits == null) {
			credits = CreditsProtos.Credits.newBuilder()
					.setCredits(0L)
					.setUserId(userId)
					.build();
		}
		
		return Futures.immediateFuture(GetCreditsResponse.newBuilder()
				.setCredits(credits)
				.build());
	}

	@Override
	public ListenableFuture<GetCreditsOrderResponse> getCreditsOrder(
			RequestHead head, GetCreditsOrderRequest request) {
		final long companyId = head.getSession().getCompanyId();
		final long userId = head.getSession().getUserId();
		
		final boolean isExpense = request.getIsExpense();
		final ByteString data = request.hasOffsetIndex() && !request.getOffsetIndex().isEmpty() ? request.getOffsetIndex() : null;
		
		Integer orderId = null;
		Integer time = null;
		CreditsDAOProtos.CreditsOrderListIndex offsetIndex = null;
		if (data != null) {
			try {
				offsetIndex = CreditsDAOProtos.CreditsOrderListIndex.parseFrom(data);
				orderId = offsetIndex.getOrderId();
				time = offsetIndex.getTime();
			} catch (InvalidProtocolBufferException e) {
				
			}
		}
		
		final int size = request.getSize() < 0 ? 0 : request.getSize() > 50 ? 50 : request.getSize();
		
		List<CreditsProtos.CreditsOrder> tmpCreditsOrderList = null;
		Connection conn = null;
		try {
			conn = this.hikariDataSource.getConnection();
			
			tmpCreditsOrderList = CreditsDB.getCreditsOrder(conn, companyId, userId, isExpense, orderId, time, size + 1);
		} catch (SQLException ex) {
			throw new RuntimeException("db fail", ex);
		} finally {
			DBUtil.closeQuietly(conn);
		}
		
		boolean hasMore = tmpCreditsOrderList.size() > size;
		
		List<CreditsProtos.CreditsOrder> creditsOrderList = null;
		if (hasMore) {
			creditsOrderList = tmpCreditsOrderList.subList(0, size);
		} else {
			creditsOrderList = tmpCreditsOrderList;
		}
		
		GetCreditsOrderResponse.Builder responseBuilder = GetCreditsOrderResponse.newBuilder()
				.addAllCreditsOrder(creditsOrderList)
				.setHasMore(hasMore); 
		
		if (!creditsOrderList.isEmpty()) {
			CreditsDAOProtos.CreditsOrderListIndex.Builder offsetIndexBuilder = CreditsDAOProtos.CreditsOrderListIndex.newBuilder();
			CreditsProtos.CreditsOrder creditsOrder = creditsOrderList.get(creditsOrderList.size() - 1);
			offsetIndexBuilder.setOrderId(creditsOrder.getOrderId()).setTime(creditsOrder.getCreateTime());
			
			responseBuilder.setOffsetIndex(offsetIndexBuilder.build().toByteString());
		}
		
		return Futures.immediateFuture(responseBuilder.build());
		
	}

	@Override
	public ListenableFuture<GetCreditsRuleResponse> getCreditsRule(
			RequestHead head, EmptyRequest request) {
		final long companyId = head.getSession().getCompanyId();
		
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

}
