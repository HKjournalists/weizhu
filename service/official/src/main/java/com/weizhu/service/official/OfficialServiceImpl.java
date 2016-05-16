package com.weizhu.service.official;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import com.google.common.collect.ImmutableSet;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.inject.Inject;
import com.weizhu.common.db.DBUtil;
import com.weizhu.common.utils.ProfileManager;
import com.weizhu.proto.AdminOfficialProtos;
import com.weizhu.proto.AllowProtos.CheckAllowRequest;
import com.weizhu.proto.AllowProtos.CheckAllowResponse;
import com.weizhu.proto.AllowService;
import com.weizhu.proto.OfficialProtos;
import com.weizhu.proto.OfficialProtos.GetOfficialByIdRequest;
import com.weizhu.proto.OfficialProtos.GetOfficialByIdResponse;
import com.weizhu.proto.OfficialProtos.GetOfficialListRequest;
import com.weizhu.proto.OfficialProtos.GetOfficialListResponse;
import com.weizhu.proto.OfficialProtos.GetOfficialMessageRequest;
import com.weizhu.proto.OfficialProtos.GetOfficialMessageResponse;
import com.weizhu.proto.OfficialProtos.SendOfficialMessageRequest;
import com.weizhu.proto.OfficialProtos.SendOfficialMessageResponse;
import com.weizhu.proto.OfficialService;
import com.weizhu.proto.PushProtos;
import com.weizhu.proto.PushService;
import com.weizhu.proto.WeizhuProtos.RequestHead;
import com.zaxxer.hikari.HikariDataSource;

public class OfficialServiceImpl implements OfficialService {
	
	private static final Logger logger = LoggerFactory.getLogger(OfficialServiceImpl.class);
	
	private static final ImmutableSet<OfficialProtos.State> USER_STATE_SET = ImmutableSet.of(OfficialProtos.State.NORMAL);
	
	private final HikariDataSource hikariDataSource;
	private final JedisPool jedisPool;
	private final PushService pushService;
	private final AllowService allowService;
	private final ProfileManager profileManager;
	
	private final OfficialRobot officialRobot;
	
	@Inject
	public OfficialServiceImpl(HikariDataSource hikariDataSource, JedisPool jedisPool, 
			PushService pushService, AllowService allowService, 
			ProfileManager profileManager, OfficialRobot officialRobot
			) {
		this.hikariDataSource = hikariDataSource;
		this.jedisPool = jedisPool;
		this.pushService = pushService;
		this.allowService = allowService;
		this.profileManager = profileManager;
		this.officialRobot = officialRobot;
		
		Jedis jedis = jedisPool.getResource();
		try {
			OfficialCache.loadScript(jedis);
		} finally {
			jedis.close();
		}
	}
	
	private Map<Long, OfficialProtos.Official> filterAllowOfficial(RequestHead head, Map<Long, OfficialProtos.Official> officialMap) {
		if (officialMap.isEmpty()) {
			return Collections.emptyMap();
		}
		
		Set<Integer> allowModelIdSet = new TreeSet<Integer>();
		for (OfficialProtos.Official official : officialMap.values()) {
			if (official.hasAllowModelId()) {
				allowModelIdSet.add(official.getAllowModelId());
			}
		}
		
		if (allowModelIdSet.isEmpty()) {
			return officialMap;
		}

		CheckAllowResponse checkAllowResponse = Futures.getUnchecked(
				this.allowService.checkAllow(head, CheckAllowRequest.newBuilder()
					.addUserId(head.getSession().getUserId())
					.addAllModelId(allowModelIdSet)
					.build()));
		
		Set<Integer> allowedModelIdSet = new TreeSet<Integer>();
		for (CheckAllowResponse.CheckResult checkResult : checkAllowResponse.getCheckResultList()) {
			if (checkResult.getAllowUserIdList().contains(head.getSession().getUserId())) {
				allowedModelIdSet.add(checkResult.getModelId());
			}
		}
		
		Map<Long, OfficialProtos.Official> newOfficialMap = new TreeMap<Long, OfficialProtos.Official>();
		for (OfficialProtos.Official official : officialMap.values()) {
			if (!official.hasAllowModelId() || allowedModelIdSet.contains(official.getAllowModelId())) {
				newOfficialMap.put(official.getOfficialId(), official);
			}
		}
		return newOfficialMap;
	}
	
	@Override
	public ListenableFuture<GetOfficialByIdResponse> getOfficialById(RequestHead head, GetOfficialByIdRequest request) {
		Map<Long, OfficialProtos.Official> officialMap = OfficialUtil.getOfficial(
				hikariDataSource, jedisPool, this.profileManager.getProfile(head, "official:"), 
				head.getSession().getCompanyId(), request.getOfficialIdList(), USER_STATE_SET
				);
		return Futures.immediateFuture(GetOfficialByIdResponse.newBuilder()
				.addAllOfficial(this.filterAllowOfficial(head, officialMap).values())
				.build());
	}

	@Override
	public ListenableFuture<GetOfficialListResponse> getOfficialList(RequestHead head, GetOfficialListRequest request) {
		final long companyId = head.getSession().getCompanyId();
		final Long lastOfficialId = request.hasLastOfficialId() ? request.getLastOfficialId() : null;
		final int officialSize = request.getOfficialSize() < 10 ? 10 : request.getOfficialSize() > 100 ? 100 : request.getOfficialSize();
		
		List<Long> officialIdList;
		Connection dbConn = null;
		try {
			dbConn = this.hikariDataSource.getConnection();
			
			if ((lastOfficialId == null || lastOfficialId <= 0) && officialSize > 0) {
				officialIdList = new ArrayList<Long>();
				officialIdList.add(Long.valueOf(AdminOfficialProtos.ReservedOfficialId.WEIZHU_SECRETARY_VALUE));
				officialIdList.addAll(OfficialDB.getOfficialIdList(dbConn, companyId, lastOfficialId, officialSize, USER_STATE_SET));
			} else {
				officialIdList = OfficialDB.getOfficialIdList(dbConn, companyId, lastOfficialId, officialSize + 1, USER_STATE_SET);
			}
		} catch (SQLException e) {
			throw new RuntimeException("db fail", e);
		} finally {
			DBUtil.closeQuietly(dbConn);
		}
		
		boolean hasMore;
		if (officialIdList.size() > officialSize) {
			hasMore = true;
			officialIdList = officialIdList.subList(0, officialSize);
		} else {
			hasMore = false;
		}
		
		Map<Long, OfficialProtos.Official> officialMap = this.filterAllowOfficial(head, 
				OfficialUtil.getOfficial(
						hikariDataSource, jedisPool, this.profileManager.getProfile(head, "official:"), 
						companyId, officialIdList, USER_STATE_SET));
		
		GetOfficialListResponse.Builder responseBuilder = GetOfficialListResponse.newBuilder();
		responseBuilder.setHasMore(hasMore);
		for (Long officialId : officialIdList) {
			OfficialProtos.Official official = officialMap.get(officialId);
			if (official != null) {
				responseBuilder.addOfficial(official);
			}
		}
		
		return Futures.immediateFuture(responseBuilder.build());
	}
	
	@Override
	public ListenableFuture<GetOfficialMessageResponse> getOfficialMessage(RequestHead head, GetOfficialMessageRequest request) {
		
		Long msgSeqBegin = request.hasMsgSeqBegin() ? request.getMsgSeqBegin() : null;
		Long msgSeqEnd = request.hasMsgSeqEnd() ? request.getMsgSeqEnd() : null;
		
		// 多获取一条数据用于判断 hasMore
		List<OfficialProtos.OfficialMessage> msgList = doGetOfficialMessage(
				head.getSession().getCompanyId(), head.getSession().getUserId(), request.getOfficialId(), 
				msgSeqBegin, msgSeqEnd, request.getMsgSize() + 1);
		
		boolean hasMore;
		if (msgList.size() > request.getMsgSize()) {
			hasMore = true;
			msgList = msgList.subList(0, request.getMsgSize());
		} else {
			hasMore = false;
		}
		
		return Futures.immediateFuture(GetOfficialMessageResponse.newBuilder()
				.addAllMsg(msgList)
				.setHasMore(hasMore)
				.build());
	}
	
	private List<OfficialProtos.OfficialMessage> doGetOfficialMessage(long companyId, long userId, long officialId, Long msgSeqBegin, Long msgSeqEnd, int size) {
		Long latestMsgSeq = null;
		Jedis jedis = jedisPool.getResource();
		try {
			latestMsgSeq = OfficialCache.getLatestOfficialMsgSeq(jedis, companyId, officialId, Collections.singleton(userId)).get(userId);
		} finally {
			jedis.close();
		}
		
		if (latestMsgSeq != null) {
			if ((msgSeqEnd == null && latestMsgSeq <= 0) 
					|| (msgSeqEnd != null && latestMsgSeq <= msgSeqEnd)) {
				return Collections.emptyList();
			}
		}
		
		Connection dbConn = null;
		try {
			dbConn = hikariDataSource.getConnection();
			
			return OfficialDB.getOfficialMessage(dbConn, companyId, userId, officialId, msgSeqBegin, msgSeqEnd, size);
			
		} catch (SQLException e) {
			throw new RuntimeException("get official message db fail!", e);
		} finally {
			DBUtil.closeQuietly(dbConn);
		}
	}
	
	@Override
	public ListenableFuture<SendOfficialMessageResponse> sendOfficialMessage(final RequestHead head, SendOfficialMessageRequest request) {
		String failText = OfficialUtil.checkSendMessage(request.getMsg());
		if (failText != null) {
			return Futures.immediateFuture(SendOfficialMessageResponse.newBuilder()
					.setResult(SendOfficialMessageResponse.Result.FAIL_MSG_INVALID)
					.setFailText(failText)
					.build());
		}
		
		final long companyId = head.getSession().getCompanyId();
		final long officialId = request.getOfficialId();
		final long userId = head.getSession().getUserId();
		
		final Map<Long, OfficialProtos.Official> officialMap = OfficialUtil.getOfficial(
				hikariDataSource, jedisPool, this.profileManager.getProfile(head, "official:"),
				companyId, Collections.singleton(officialId), USER_STATE_SET);
		if (!officialMap.containsKey(officialId)) {
			return Futures.immediateFuture(SendOfficialMessageResponse.newBuilder()
					.setResult(SendOfficialMessageResponse.Result.FAIL_OFFICIAL_NOT_EXIST)
					.setFailText("服务号不存在")
					.build());
		}
		
		final OfficialProtos.Official official = this.filterAllowOfficial(head, officialMap).get(officialId);
		if (official == null) {
			return Futures.immediateFuture(SendOfficialMessageResponse.newBuilder()
					.setResult(SendOfficialMessageResponse.Result.FAIL_OFFICIAL_NOT_EXIST)
					.setFailText("您不能向该服务号发消息")
					.build());
		}
		
		OfficialProtos.OfficialMessage userMsg = 
				OfficialUtil.saveOfficialSingleMessage(
						hikariDataSource, jedisPool, 
						companyId, officialId, userId, 
						request.getMsg(), (int) (System.currentTimeMillis() / 1000L), true
						);
		
		if (userMsg == null) {
			throw new RuntimeException("cannot save official msg");
		}
		
		this.pushService.pushMsg(head, PushProtos.PushMsgRequest.newBuilder()
				.addPushPacket(PushProtos.PushPacket.newBuilder()
						.addPushTarget(PushProtos.PushTarget.newBuilder()
								.setUserId(head.getSession().getUserId())
								.addExcludeSessionId(head.getSession().getSessionId())
								.setEnableOffline(true)
								.build())
						.setPushName("OfficialMessagePush")
						.setPushBody(OfficialProtos.OfficialMessagePush.newBuilder()
								.setOfficialId(officialId)
								.setMsg(userMsg)
								.build().toByteString())
						.build())
				.build());
		
		ListenableFuture<List<OfficialProtos.OfficialMessage>> robotResponseFuture = this.officialRobot.sendMessage(head, officialId, userMsg, this.profileManager.getProfile(head, "official:"));
		if (robotResponseFuture == null) {
			return Futures.immediateFuture(SendOfficialMessageResponse.newBuilder()
					.setResult(SendOfficialMessageResponse.Result.SUCC)
					.setMsgSeq(userMsg.getMsgSeq())
					.setMsgTime(userMsg.getMsgTime())
					.build());
		}
		
		if (robotResponseFuture.isDone()) {
			List<OfficialProtos.OfficialMessage> responseMsgUnsaveList = Futures.getUnchecked(robotResponseFuture);
			if (responseMsgUnsaveList.isEmpty()) {
				return Futures.immediateFuture(SendOfficialMessageResponse.newBuilder()
						.setResult(SendOfficialMessageResponse.Result.SUCC)
						.setMsgSeq(userMsg.getMsgSeq())
						.setMsgTime(userMsg.getMsgTime())
						.build());
			}
			
			List<OfficialProtos.OfficialMessage> responseMsgList = new ArrayList<OfficialProtos.OfficialMessage>(responseMsgUnsaveList.size());
			
			final int now = (int) (System.currentTimeMillis() / 1000L);
			for (OfficialProtos.OfficialMessage unsaveMsg : responseMsgUnsaveList) {
				OfficialProtos.OfficialMessage savedMsg = OfficialUtil.saveOfficialSingleMessage(
						hikariDataSource, jedisPool, 
						companyId, officialId, userId, unsaveMsg, 
						now, false);
				
				if (savedMsg != null) {
					responseMsgList.add(savedMsg);
				}
			}
			
			for (OfficialProtos.OfficialMessage officialMsg : responseMsgList) {
				this.pushService.pushMsg(head, PushProtos.PushMsgRequest.newBuilder()
						.addPushPacket(PushProtos.PushPacket.newBuilder()
								.addPushTarget(PushProtos.PushTarget.newBuilder()
										.setUserId(head.getSession().getUserId())
										.addExcludeSessionId(head.getSession().getSessionId())
										.setEnableOffline(true)
										.build())
								.setPushName("OfficialMessagePush")
								.setPushBody(OfficialProtos.OfficialMessagePush.newBuilder()
										.setOfficialId(officialId)
										.setMsg(officialMsg)
										.build().toByteString())
								.build())
						.build());
			}
			
			return Futures.immediateFuture(SendOfficialMessageResponse.newBuilder()
					.setResult(SendOfficialMessageResponse.Result.SUCC)
					.setMsgSeq(userMsg.getMsgSeq())
					.setMsgTime(userMsg.getMsgTime())
					.addAllResponseMsg(responseMsgList)
					.build());
		}
		
		Futures.addCallback(robotResponseFuture, new FutureCallback<List<OfficialProtos.OfficialMessage>>() {

			@Override
			public void onSuccess(List<OfficialProtos.OfficialMessage> responseMsgUnsaveList) {
				if (responseMsgUnsaveList.isEmpty()) {
					return;
				}
				
				List<OfficialProtos.OfficialMessage> responseMsgList = new ArrayList<OfficialProtos.OfficialMessage>(responseMsgUnsaveList.size());
				
				final long userId = head.getSession().getUserId();
				final int now = (int) (System.currentTimeMillis() / 1000L);
				for (OfficialProtos.OfficialMessage unsaveMsg : responseMsgUnsaveList) {
					OfficialProtos.OfficialMessage savedMsg = OfficialUtil.saveOfficialSingleMessage(
							hikariDataSource, jedisPool, 
							companyId, officialId, userId, unsaveMsg, 
							now, false);
					
					if (savedMsg != null) {
						responseMsgList.add(savedMsg);
					}
				}
				
				for (OfficialProtos.OfficialMessage officialMsg : responseMsgList) {
					OfficialServiceImpl.this.pushService.pushMsg(head, PushProtos.PushMsgRequest.newBuilder()
							.addPushPacket(PushProtos.PushPacket.newBuilder()
									.addPushTarget(PushProtos.PushTarget.newBuilder()
											.setUserId(userId)
											.setEnableOffline(true)
											.build())
									.setPushName("OfficialMessagePush")
									.setPushBody(OfficialProtos.OfficialMessagePush.newBuilder()
											.setOfficialId(officialId)
											.setMsg(officialMsg)
											.build().toByteString())
									.build())
							.build());
				}
			}

			@Override
			public void onFailure(Throwable t) {
				logger.error("robot response error", t);
			}
			
		});
		
		return Futures.immediateFuture(SendOfficialMessageResponse.newBuilder()
					.setResult(SendOfficialMessageResponse.Result.SUCC)
					.setMsgSeq(userMsg.getMsgSeq())
					.setMsgTime(userMsg.getMsgTime())
					.build());
	}
	
	/*

	@Override
	public ListenableFuture<EmptyResponse> loginSayHello(RequestHead head, EmptyRequest request) {
		
		ProfileManager.Profile profile = this.profileManager.getProfile(head, "official:");
		String sayHelloContent = profile.get(WEIZHU_SECRETARY_LOGIN_SAY_HELLO_CONTENT);
		
		if (sayHelloContent == null) {
			return ServiceUtil.EMPTY_RESPONSE_IMMEDIATE_FUTURE;
		}
		
		final long companyId = head.getSession().getCompanyId();
		final long userId = head.getSession().getUserId();
		final long officialId = AdminOfficialProtos.ReservedOfficialId.WEIZHU_SECRETARY_VALUE;
		
		final OfficialProtos.Official official = this.filterAllowOfficial(head, 
				OfficialUtil.getOfficial(hikariDataSource, jedisPool, profile, 
						companyId, Collections.singleton(officialId), USER_STATE_SET)).get(officialId);
		if (official == null) {
			return ServiceUtil.EMPTY_RESPONSE_IMMEDIATE_FUTURE;
		}
		
		OfficialProtos.OfficialMessage sayHelloMsg = OfficialUtil.saveOfficialSingleMessage(
				hikariDataSource, jedisPool, 
				companyId, officialId, userId,
				OfficialProtos.OfficialMessage.newBuilder()
					.setMsgSeq(-1L)
					.setMsgTime(0)
					.setIsFromUser(false)
					.setText(OfficialProtos.OfficialMessage.Text.newBuilder()
							.setContent(sayHelloContent)
							.build())
					.build(),
				(int) (System.currentTimeMillis() / 1000L), false);
		
		if (sayHelloMsg == null) {
			throw new RuntimeException("cannot save official msg");
		}
		
		this.pushService.pushMsg(head, PushProtos.PushMsgRequest.newBuilder()
				.addPushPacket(PushProtos.PushPacket.newBuilder()
						.addPushTarget(PushProtos.PushTarget.newBuilder()
								.setUserId(userId)
								.setEnableOffline(true)
								.build())
						.setPushName("OfficialMessagePush")
						.setPushBody(OfficialProtos.OfficialMessagePush.newBuilder()
								.setOfficialId(officialId)
								.setMsg(sayHelloMsg)
								.build().toByteString())
						.build())
				.build());
		
		return ServiceUtil.EMPTY_RESPONSE_IMMEDIATE_FUTURE;
	}
	*/

}
