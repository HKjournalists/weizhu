package com.weizhu.service.push;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.Callable;
import java.util.concurrent.Executor;

import javax.annotation.Nullable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import com.google.common.primitives.Longs;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListenableFutureTask;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.weizhu.common.db.DBUtil;
import com.weizhu.common.service.AsyncImpl;
import com.weizhu.common.service.ServiceUtil;
import com.weizhu.proto.APNsProtos.DeleteDeviceTokenExpireRequest;
import com.weizhu.proto.APNsProtos.SendNotificationRequest;
import com.weizhu.proto.APNsService;
import com.weizhu.proto.APNsProtos.DeleteDeviceTokenRequest;
import com.weizhu.proto.AdminProtos.AdminHead;
import com.weizhu.proto.ConnProtos.CloseConnectionExpireRequest;
import com.weizhu.proto.ConnProtos.CloseConnectionRequest;
import com.weizhu.proto.ConnProtos.SendMessageRequest;
import com.weizhu.proto.ConnProtos.SendMessageResponse;
import com.weizhu.proto.ConnService;
import com.weizhu.proto.PushPollingProtos.GetPushMsgRequest;
import com.weizhu.proto.PushPollingProtos.GetPushMsgResponse;
import com.weizhu.proto.PushPollingService;
import com.weizhu.proto.PushProtos;
import com.weizhu.proto.PushProtos.GetOfflineMsgRequest;
import com.weizhu.proto.PushProtos.GetOfflineMsgResponse;
import com.weizhu.proto.PushProtos.PushMsgRequest;
import com.weizhu.proto.PushProtos.PushStateRequest;
import com.weizhu.proto.PushProtos.PushUserDeleteRequest;
import com.weizhu.proto.PushProtos.PushUserDisableRequest;
import com.weizhu.proto.PushProtos.PushUserExpireRequest;
import com.weizhu.proto.PushService;
import com.weizhu.proto.WeizhuProtos;
import com.weizhu.proto.WeizhuProtos.EmptyRequest;
import com.weizhu.proto.WeizhuProtos.EmptyResponse;
import com.weizhu.proto.WeizhuProtos.RequestHead;
import com.weizhu.proto.WeizhuProtos.SystemHead;
import com.zaxxer.hikari.HikariDataSource;

public class PushServiceImpl implements PushService, PushPollingService {

	private static final Logger logger = LoggerFactory.getLogger(PushServiceImpl.class);

	private static final int MAX_OFFLINE_MSG_NUM = 100;
	
	private final Executor serviceExecutor;
	private final HikariDataSource hikariDataSource;
	private final JedisPool jedisPool;
	private final Set<ConnService> connServiceSet;
	private final APNsService apnsService;
	
	@Inject
	public PushServiceImpl(
			@Named("service_executor") Executor serviceExecutor,
			HikariDataSource hikariDataSource, JedisPool jedisPool,
			Set<ConnService> connServiceSet,
			APNsService apnsService
			) {
		this.serviceExecutor = serviceExecutor;
		this.hikariDataSource = hikariDataSource;
		this.jedisPool = jedisPool;
		this.connServiceSet = connServiceSet;
		this.apnsService = apnsService;
		
		Jedis jedis = jedisPool.getResource();
		try {
			PushCache.loadScript(jedis);
		} finally {
			jedis.close();
		}
	}
	
	@Override
	@AsyncImpl
	public ListenableFuture<EmptyResponse> pushMsg(RequestHead head, PushMsgRequest request) {
		return this.pushMsg0(head, null, null, request);
	}
	
	@Override
	@AsyncImpl
	public ListenableFuture<EmptyResponse> pushMsg(AdminHead head, PushMsgRequest request) {
		return this.pushMsg0(null, head, null, request);
	}
	
	@Override
	@AsyncImpl
	public ListenableFuture<EmptyResponse> pushMsg(SystemHead head, PushMsgRequest request) {
		return this.pushMsg0(null, null, head, request);
	}
	
	private ListenableFuture<EmptyResponse> pushMsg0(
			@Nullable final RequestHead requestHead, @Nullable final AdminHead adminHead, @Nullable final SystemHead systemHead, 
			final PushMsgRequest request
			) {
		if (requestHead == null && adminHead == null && systemHead == null) {
			return Futures.immediateFailedFuture(new RuntimeException("no head"));
		}	
		if (request.getPushPacketCount() <= 0) {
			return ServiceUtil.EMPTY_RESPONSE_IMMEDIATE_FUTURE;
		}
		
		final ListenableFutureTask<EmptyResponse> task = ListenableFutureTask.create(new Callable<EmptyResponse>(){

			@Override
			public EmptyResponse call() throws Exception {
				return pushMsgImpl(requestHead, adminHead, systemHead, request);
			}
			
		});
		this.serviceExecutor.execute(task);
		return task;
	}
	
	private EmptyResponse pushMsgImpl(@Nullable RequestHead requestHead, @Nullable AdminHead adminHead, @Nullable SystemHead systemHead, PushMsgRequest request) {
		if (requestHead == null && adminHead == null && systemHead == null) {
			throw new RuntimeException("no head");
		}
		
		final Long companyId;
		if (requestHead != null) {
			companyId = requestHead.getSession().getCompanyId();
		} else if (adminHead != null) {
			companyId = adminHead.hasCompanyId() ? adminHead.getCompanyId() : null;
		} else if (systemHead != null) {
			companyId = systemHead.hasCompanyId() ? systemHead.getCompanyId() : null;
		} else {
			companyId = null;
		}
		
		if (companyId == null) {
			throw new RuntimeException("cannot find company id");
		}
		
		List<PushProtos.PushPacket> pushPacketList = this.doGeneratePushSeq(companyId, request.getPushPacketList());
		
		// 将PushPacket 保存到离线消息队列中
		Map<Long, List<PushDAOProtos.OfflineMsg>> offlineMsgListMap = new HashMap<Long, List<PushDAOProtos.OfflineMsg>>();
		
		PushDAOProtos.OfflineMsg.Builder tmpOfflineMsgBuilder = PushDAOProtos.OfflineMsg.newBuilder();
		for (PushProtos.PushPacket packet : pushPacketList) {
			for (PushProtos.PushTarget target : packet.getPushTargetList()) {
				List<PushDAOProtos.OfflineMsg> list = offlineMsgListMap.get(target.getUserId());
				if (list == null) {
					list = new ArrayList<PushDAOProtos.OfflineMsg>();
					offlineMsgListMap.put(target.getUserId(), list);
				}
				
				list.add(tmpOfflineMsgBuilder.clear()
						.addAllIncludeSessionId(target.getIncludeSessionIdList())
						.addAllExcludeSessionId(target.getExcludeSessionIdList())
						.setPushSeq(target.getPushSeq())
						.setPushName(packet.getPushName())
						.setPushBody(packet.getPushBody())
						.build());
			}
		}
		
		Jedis jedis = jedisPool.getResource();
		try {
			Map<Long, Long> offlineMsgListSizeMap = PushCache.addOfflineMsg(jedis, companyId, offlineMsgListMap);
			
			// trim offline msg list size
			List<Long> userIdList = new ArrayList<Long>();
			for (Entry<Long, Long> entry : offlineMsgListSizeMap.entrySet()) {
				if (entry.getValue() > MAX_OFFLINE_MSG_NUM) {
					userIdList.add(entry.getKey());
				}
			}
			PushCache.trimOfflineMsg(jedis, companyId, userIdList, MAX_OFFLINE_MSG_NUM);
		} finally {
			jedis.close();
		}
		
		// 发送push msg
		doSendPushMsg(requestHead, adminHead, systemHead, pushPacketList);
		
		return ServiceUtil.EMPTY_RESPONSE;
	}
	
	@Override
	@AsyncImpl
	public ListenableFuture<EmptyResponse> pushState(final RequestHead head, final PushStateRequest request) {
		return this.pushState0(head, null, null, request);
	}
	
	@Override
	@AsyncImpl
	public ListenableFuture<EmptyResponse> pushState(AdminHead head, PushStateRequest request) {
		return this.pushState0(null, head, null, request);
	}
	
	@Override
	@AsyncImpl
	public ListenableFuture<EmptyResponse> pushState(SystemHead head, PushStateRequest request) {
		return this.pushState0(null, null, head, request);
	}
	
	private ListenableFuture<EmptyResponse> pushState0(
			@Nullable final RequestHead requestHead, @Nullable final AdminHead adminHead, @Nullable final SystemHead systemHead, 
			final PushStateRequest request) {
		if (requestHead == null && adminHead == null && systemHead == null) {
			return Futures.immediateFailedFuture(new RuntimeException("no head"));
		}
		if (request.getPushPacketCount() <= 0) {
			return ServiceUtil.EMPTY_RESPONSE_IMMEDIATE_FUTURE;
		}
		final ListenableFutureTask<EmptyResponse> task = ListenableFutureTask.create(new Callable<EmptyResponse>(){

			@Override
			public EmptyResponse call() throws Exception {
				return pushStateImpl(requestHead, adminHead, systemHead, request);
			}
			
		});
		this.serviceExecutor.execute(task);
		return task;
	}
	
	private EmptyResponse pushStateImpl(
			@Nullable RequestHead requestHead, @Nullable AdminHead adminHead, @Nullable SystemHead systemHead, 
			PushStateRequest request
			) {
		if (requestHead == null && adminHead == null && systemHead == null) {
			throw new RuntimeException("no head");
		}
		
		final Long companyId;
		if (requestHead != null) {
			companyId = requestHead.getSession().getCompanyId();
		} else if (adminHead != null) {
			companyId = adminHead.hasCompanyId() ? adminHead.getCompanyId() : null;
		} else if (systemHead != null) {
			companyId = systemHead.hasCompanyId() ? systemHead.getCompanyId() : null;
		} else {
			companyId = null;
		}
		
		if (companyId == null) {
			throw new RuntimeException("cannot find company id");
		}
		
		List<PushProtos.PushPacket> pushPacketList = this.doGeneratePushSeq(companyId, request.getPushPacketList());
		
		// 将PushPacket 保存到离线状态中
		Map<Long, List<PushDAOProtos.OfflineState>> offlineStateListMap = new HashMap<Long, List<PushDAOProtos.OfflineState>>();
		
		PushDAOProtos.OfflineState.Builder tmpOfflineStateBuilder = PushDAOProtos.OfflineState.newBuilder();
		for (PushProtos.PushPacket packet : pushPacketList) {
			for (PushProtos.PushTarget target : packet.getPushTargetList()) {
				List<PushDAOProtos.OfflineState> list = offlineStateListMap.get(target.getUserId());
				if (list == null) {
					list = new ArrayList<PushDAOProtos.OfflineState>();
					offlineStateListMap.put(target.getUserId(), list);
				}
				
				list.add(tmpOfflineStateBuilder.clear()
						.setPushSeq(target.getPushSeq())
						.setPushName(packet.getPushName())
						.setPushKey(packet.getPushBody())
						.build());
			}
		}
		
		Jedis jedis = jedisPool.getResource();
		try {
			PushCache.setOfflineState(jedis, companyId, offlineStateListMap);
			
			// TODO : trim offline state size
			
		} finally {
			jedis.close();
		}
		
		// 发送push msg
		doSendPushMsg(requestHead, adminHead, systemHead, pushPacketList);
		
		return ServiceUtil.EMPTY_RESPONSE;
	}
	
	private List<PushProtos.PushPacket> doGeneratePushSeq(long companyId, List<PushProtos.PushPacket> pushPacketList0) {
		PushProtos.PushPacket.Builder tmpPacketBuilder = PushProtos.PushPacket.newBuilder();
		PushProtos.PushTarget.Builder tmpTargetBuilder = PushProtos.PushTarget.newBuilder();
		
		// 构建packetList, 确保pushTarget数目大于0且pushTarget中pushSeq字段没有设置
		LinkedList<PushProtos.PushPacket> pushPacketList = new LinkedList<PushProtos.PushPacket>();
		for (PushProtos.PushPacket packet : pushPacketList0) {
			if (packet.getPushTargetCount() <= 0) {
				continue;
			}
			
			tmpPacketBuilder.clear().setPushName(packet.getPushName()).setPushBody(packet.getPushBody());
			
			boolean hasPushSeq = false;
			for (PushProtos.PushTarget target : packet.getPushTargetList()) {
				if (target.hasPushSeq()) {
					tmpPacketBuilder.addPushTarget(tmpTargetBuilder.clear()
							.mergeFrom(target)
							.clearPushSeq()
							.build());
					hasPushSeq = true;
				} else {
					tmpPacketBuilder.addPushTarget(target);
				}
			}
			pushPacketList.add(hasPushSeq ? tmpPacketBuilder.build() : packet);
		}
		
		if (pushPacketList.isEmpty()) {
			return Collections.emptyList();
		}
		
		// 构建带PushSeq的完整PushPacket
		// 注意：生成seq后，需要立即更新db，确保db中的数据尽量是最新的。所以千万不要等所有seq都生成后再统一更新db，很容易造成并发问题
		for (int retry = 0; retry < 3; ++retry) {
			List<Long> generatePushSeqUserIdList = new ArrayList<Long>();
			for (PushProtos.PushPacket packet : pushPacketList) {
				for (PushProtos.PushTarget target : packet.getPushTargetList()) {
					if (!target.hasPushSeq()) {
						generatePushSeqUserIdList.add(target.getUserId());
					}
				}
			}
			
			if (generatePushSeqUserIdList.isEmpty()) {
				break;
			}
			
			// 1. 生成pushSeq
			List<Long> pushSeqList;
			Jedis jedis = jedisPool.getResource();
			try {
				pushSeqList = PushCache.generatePushSeq(jedis, companyId, generatePushSeqUserIdList);
			} finally {
				jedis.close();
			}
			
			// 2. 构建完整的带pushSeq的pushPacketList
			
			// 更新db的pushSeq
			Map<Long, Long> updatePushSeqMap = new HashMap<Long, Long>();
			// 没有生成pushSeq的用户Id Set
			Set<Long> noPushSeqUserIdSet = new TreeSet<Long>();
			
			ListIterator<PushProtos.PushPacket> pushPacketIt = pushPacketList.listIterator();
			Iterator<Long> pushSeqIt = pushSeqList.iterator();
			
			while (pushPacketIt.hasNext()) {
				final PushProtos.PushPacket packet = pushPacketIt.next();
				
				tmpPacketBuilder.clear().setPushName(packet.getPushName()).setPushBody(packet.getPushBody());
				
				boolean isGeneratePushSeq = false;
				for(PushProtos.PushTarget target : packet.getPushTargetList()) {
					if (target.hasPushSeq()) {
						tmpPacketBuilder.addPushTarget(target);
					} else {
						final Long pushSeq = pushSeqIt.next();
						
						if (pushSeq == null) {
							tmpPacketBuilder.addPushTarget(target);
							noPushSeqUserIdSet.add(target.getUserId());
						} else {
							
							Long oldPushSeq = updatePushSeqMap.get(target.getUserId());
							if (oldPushSeq == null || oldPushSeq < pushSeq) {
								updatePushSeqMap.put(target.getUserId(), pushSeq);
							}
							
							tmpPacketBuilder.addPushTarget(tmpTargetBuilder.clear()
									.mergeFrom(target)
									.setPushSeq(pushSeq)
									.build());
							isGeneratePushSeq = true;
						}
					}
				}
				
				if (isGeneratePushSeq) {
					pushPacketIt.set(tmpPacketBuilder.build());
				}
			}
			
			Map<Long, Long> setToCachePushSeqMap;
			Connection dbConn = null;
			try {
				dbConn = hikariDataSource.getConnection();
				
				// 4: 更新seq db
				PushDB.updatePushSeq(dbConn, companyId, updatePushSeqMap);
				
				// 5: 获取不在cache中用户的 push seq
				setToCachePushSeqMap = PushDB.getPushSeq(dbConn, companyId, noPushSeqUserIdSet);
			} catch (SQLException e) {
				throw new RuntimeException("update db fail!", e);
			} finally {
				DBUtil.closeQuietly(dbConn);
			}
			
			if (!setToCachePushSeqMap.isEmpty()) {
				jedis = jedisPool.getResource();
				try {
					PushCache.setnxPushSeq(jedis, companyId, setToCachePushSeqMap);
				} finally {
					jedis.close();
				}
			}
		}
		
		// 将没有PushSeq的Target打日志后丢弃
		ListIterator<PushProtos.PushPacket> pushPacketIt = pushPacketList.listIterator();
		while (pushPacketIt.hasNext()) {
			final PushProtos.PushPacket packet = pushPacketIt.next();
			
			tmpPacketBuilder.clear().setPushName(packet.getPushName()).setPushBody(packet.getPushBody());
			
			boolean hasNoPushSeq = false;
			for(PushProtos.PushTarget target : packet.getPushTargetList()) {
				if (target.hasPushSeq()) {
					tmpPacketBuilder.addPushTarget(target);
				} else {
					hasNoPushSeq = true;
					logger.warn("has no push seq : " + target + ", " + packet.getPushName());
				}
			}
			
			if (tmpPacketBuilder.getPushTargetCount() <= 0) {
				pushPacketIt.remove();
			} else if (hasNoPushSeq) {
				pushPacketIt.set(tmpPacketBuilder.build());
			}
		}
		
		return pushPacketList;
	}
	
	private void doSendPushMsg(
			final @Nullable RequestHead requestHead, final @Nullable AdminHead adminHead, final @Nullable SystemHead systemHead, 
			final List<PushProtos.PushPacket> pushPacketList
			) {
		if (requestHead == null && adminHead == null && systemHead == null) {
			return;
		}
		if (pushPacketList.isEmpty()) {
			return;
		}
		
		SendMessageRequest sendPushMsgRequest = SendMessageRequest.newBuilder()
				.addAllPushPacket(pushPacketList)
				.build();
		
		List<ListenableFuture<SendMessageResponse>> futureList = new ArrayList<ListenableFuture<SendMessageResponse>>(connServiceSet.size());
		for (ConnService connService : connServiceSet) {
			if (requestHead != null) {
				futureList.add(connService.sendMessage(requestHead, sendPushMsgRequest));
			} else if (adminHead != null) {
				futureList.add(connService.sendMessage(adminHead, sendPushMsgRequest));
			} else if (systemHead != null) {
				futureList.add(connService.sendMessage(systemHead, sendPushMsgRequest));
			} else {
				// ignore
			}
		}
		
		Futures.addCallback(Futures.successfulAsList(futureList), new FutureCallback<List<SendMessageResponse>>() {

			@Override
			public void onSuccess(List<SendMessageResponse> responseList) {
				
				Map<Integer, Map<Integer, List<WeizhuProtos.Session>>> pushSessionMap = 
						new TreeMap<Integer, Map<Integer, List<WeizhuProtos.Session>>>();
				
				for (SendMessageResponse response : responseList) {
					if (response == null) {
						// !!! response may be null !!! 详情见Futures.successfulAsList 注释
						continue;
					}
					
					for (PushProtos.PushSession pushSession : response.getPushSessionList()) {
						Map<Integer, List<WeizhuProtos.Session>> map = pushSessionMap.get(pushSession.getPacketIdx());
						if (map == null) {
							map = new TreeMap<Integer, List<WeizhuProtos.Session>>();
							pushSessionMap.put(pushSession.getPacketIdx(), map);
						}
						
						List<WeizhuProtos.Session> list = map.get(pushSession.getTargetIdx());
						if (list == null) {
							list = new ArrayList<WeizhuProtos.Session>();
							map.put(pushSession.getTargetIdx(), list);
						}
						
						list.add(pushSession.getSession());
					}
				}
				
				SendNotificationRequest.Builder requestBuilder = SendNotificationRequest.newBuilder();
				
				PushProtos.PushPacket.Builder tmpPacketBuilder = PushProtos.PushPacket.newBuilder();
				PushProtos.PushTarget.Builder tmpTargetBuilder = PushProtos.PushTarget.newBuilder();
				
				for (int i=0; i<pushPacketList.size(); ++i) {
					final PushProtos.PushPacket packet = pushPacketList.get(i);
					
					final Map<Integer, List<WeizhuProtos.Session>> map = pushSessionMap.get(i);
					if (map == null) {
						requestBuilder.addPushPacket(packet);
					} else {
						tmpPacketBuilder.clear();
						tmpPacketBuilder.setPushName(packet.getPushName()).setPushBody(packet.getPushBody());
						
						for (int j=0; j<packet.getPushTargetCount(); ++j) {
							final PushProtos.PushTarget target = packet.getPushTarget(j);
							
							final List<WeizhuProtos.Session> list = map.get(j);
							if (list == null) {
								tmpPacketBuilder.addPushTarget(target);
							} else {
								Set<Long> excludeSessionIdSet = new TreeSet<Long>(target.getExcludeSessionIdList());
								
								for (WeizhuProtos.Session session : list) {
									if (session.getUserId() == target.getUserId()) {
										excludeSessionIdSet.add(session.getSessionId());
									}
								}
								
								tmpPacketBuilder.addPushTarget(tmpTargetBuilder.clear()
										.mergeFrom(target)
										.clearExcludeSessionId()
										.addAllExcludeSessionId(excludeSessionIdSet)
										.build());
								
							}
						}
						
						requestBuilder.addPushPacket(tmpPacketBuilder.build());
					}
				}
				
				if (requestHead != null) {
					apnsService.sendNotification(requestHead, requestBuilder.build());
				} else if (adminHead != null) {
					apnsService.sendNotification(adminHead, requestBuilder.build());
				} else if (systemHead != null) {
					apnsService.sendNotification(systemHead, requestBuilder.build());
				} else {
					// ignore
				}
			}

			@Override
			public void onFailure(Throwable t) {
				logger.warn("doSendPushMsg connService.sendMessage() fail", t);
			}
			
		});
	}

	@Override
	public ListenableFuture<GetOfflineMsgResponse> getOfflineMsg(RequestHead head, GetOfflineMsgRequest request) {
		final long companyId = head.getSession().getCompanyId();
		final long userId = head.getSession().getUserId();
		
		List<PushDAOProtos.OfflineMsg> offlineMsgList;
		List<PushDAOProtos.OfflineState> offlineStateList;
		Long latestPushSeq;
		Jedis jedis = jedisPool.getResource();
		try {
			offlineMsgList = PushCache.getOfflineMsg(jedis, companyId, userId);
			offlineStateList = PushCache.getOfflineState(jedis, companyId, userId);
			latestPushSeq = PushCache.getPushSeq(jedis, companyId, userId);
		} finally {
			jedis.close();
		}
		
		if (latestPushSeq == null) {
			Connection dbConn = null;
			try {
				dbConn = hikariDataSource.getConnection();
				latestPushSeq = PushDB.getPushSeq(dbConn, companyId, Collections.singleton(userId)).get(userId);
			} catch (SQLException e) {
				throw new RuntimeException("update db fail!", e);
			} finally {
				DBUtil.closeQuietly(dbConn);
			}
			
			if (latestPushSeq == null) {
				// log error!
				throw new RuntimeException("cannot get push seq from db");
			}
			
			jedis = jedisPool.getResource();
			try {
				PushCache.setnxPushSeq(jedis, companyId, Collections.singletonMap(userId, latestPushSeq));
			} finally {
				jedis.close();
			}
		}
		
		GetOfflineMsgResponse.Builder responseBuilder = GetOfflineMsgResponse.newBuilder();
		
		WeizhuProtos.PushMessage.Builder tmpPushMessageBuilder = WeizhuProtos.PushMessage.newBuilder();
		for (PushDAOProtos.OfflineMsg offlineMsg : offlineMsgList) {
			if (offlineMsg.getPushSeq() > request.getPushSeq()
					&& (offlineMsg.getIncludeSessionIdCount() <= 0 || offlineMsg.getIncludeSessionIdList().contains(head.getSession().getSessionId()))
					&& !offlineMsg.getExcludeSessionIdList().contains(head.getSession().getSessionId())
					) {
				responseBuilder.addOfflineMsg(tmpPushMessageBuilder.clear()
						.setPushSeq(offlineMsg.getPushSeq())
						.setPushName(offlineMsg.getPushName())
						.setPushBody(offlineMsg.getPushBody())
						.build());
			}
		}
		for (PushDAOProtos.OfflineState offlineState : offlineStateList) {
			if (offlineState.getPushSeq() > request.getPushSeq()) {
				responseBuilder.addOfflineMsg(tmpPushMessageBuilder.clear()
						.setPushSeq(offlineState.getPushSeq())
						.setPushName(offlineState.getPushName())
						.setPushBody(offlineState.getPushKey())
						.build());
			}
		}
		
		responseBuilder.setPushSeq(latestPushSeq);
		
		return Futures.immediateFuture(responseBuilder.build());
	}
	
	private static final Comparator<WeizhuProtos.PushMessage> PUSH_MESSAGE_CMP = new Comparator<WeizhuProtos.PushMessage>() {

		@Override
		public int compare(WeizhuProtos.PushMessage o1, WeizhuProtos.PushMessage o2) {
			return Longs.compare(o1.getPushSeq(), o2.getPushSeq());
		}
		
	};

	@Override
	public ListenableFuture<GetPushMsgResponse> getPushMsg(RequestHead head, GetPushMsgRequest request) {
		int msgSize = request.getMsgSize() > 50 ? 50 : request.getMsgSize();
		
		final long companyId = head.getSession().getCompanyId();
		final long userId = head.getSession().getUserId();
		
		List<PushDAOProtos.OfflineMsg> offlineMsgList;
		List<PushDAOProtos.OfflineState> offlineStateList;
		Long latestPushSeq;
		Jedis jedis = jedisPool.getResource();
		try {
			offlineMsgList = PushCache.getOfflineMsg(jedis, companyId, userId);
			offlineStateList = PushCache.getOfflineState(jedis, companyId, userId);
			latestPushSeq = PushCache.getPushSeq(jedis, companyId, userId);
		} finally {
			jedis.close();
		}
		
		if (latestPushSeq == null) {
			Connection dbConn = null;
			try {
				dbConn = hikariDataSource.getConnection();
				latestPushSeq = PushDB.getPushSeq(dbConn, companyId, Collections.singleton(userId)).get(userId);
			} catch (SQLException e) {
				throw new RuntimeException("update db fail!", e);
			} finally {
				DBUtil.closeQuietly(dbConn);
			}
			
			if (latestPushSeq == null) {
				// log error!
				throw new RuntimeException("cannot get push seq from db");
			}
			
			jedis = jedisPool.getResource();
			try {
				PushCache.setnxPushSeq(jedis, companyId, Collections.singletonMap(userId, latestPushSeq));
			} finally {
				jedis.close();
			}
		}
		
		List<WeizhuProtos.PushMessage> pushMsgList = new ArrayList<WeizhuProtos.PushMessage>(offlineMsgList.size());
		
		WeizhuProtos.PushMessage.Builder tmpPushMessageBuilder = WeizhuProtos.PushMessage.newBuilder();
		for (PushDAOProtos.OfflineMsg offlineMsg : offlineMsgList) {
			if (offlineMsg.getPushSeq() > request.getPushSeq()
					&& (offlineMsg.getIncludeSessionIdCount() <= 0 || offlineMsg.getIncludeSessionIdList().contains(head.getSession().getSessionId()))
					&& !offlineMsg.getExcludeSessionIdList().contains(head.getSession().getSessionId())) {
				pushMsgList.add(tmpPushMessageBuilder.clear()
						.setPushSeq(offlineMsg.getPushSeq())
						.setPushName(offlineMsg.getPushName())
						.setPushBody(offlineMsg.getPushBody())
						.build());
			}
		}
		for (PushDAOProtos.OfflineState offlineState : offlineStateList) {
			if (offlineState.getPushSeq() > request.getPushSeq()) {
				pushMsgList.add(tmpPushMessageBuilder.clear()
						.setPushSeq(offlineState.getPushSeq())
						.setPushName(offlineState.getPushName())
						.setPushBody(offlineState.getPushKey())
						.build());
			}
		}
		
		Collections.sort(pushMsgList, PUSH_MESSAGE_CMP);
		
		GetPushMsgResponse.Builder responseBuilder = GetPushMsgResponse.newBuilder();
		for (int i=0; i<pushMsgList.size() && i<msgSize; ++i) {
			responseBuilder.addPushMsg(pushMsgList.get(i));
		}
		responseBuilder.setHasMore(pushMsgList.size() > msgSize);
		
		return Futures.immediateFuture(responseBuilder.build());
	}

	@Override
	@AsyncImpl
	public ListenableFuture<EmptyResponse> pushUserDelete(AdminHead head, PushUserDeleteRequest request) {
		if (!head.hasCompanyId() || (request.getUserIdCount() <= 0 && request.getSessionCount() <= 0)) {
			return ServiceUtil.EMPTY_RESPONSE_IMMEDIATE_FUTURE;
		}
		final long companyId = head.getCompanyId();
		// check session company id
		for (WeizhuProtos.Session session : request.getSessionList()) {
			if (session.getCompanyId() != companyId) {
				return Futures.immediateFailedFuture(new RuntimeException("invalid companyId : " + session.getCompanyId() + ", expect : " + companyId));
			}
		}
		
		CloseConnectionRequest closeConnectionRequest = CloseConnectionRequest.newBuilder()
				.addAllUserId(request.getUserIdList())
				.addAllSession(request.getSessionList())
				.build();
		
		for (ConnService connService : connServiceSet) {
			connService.closeConnection(head, closeConnectionRequest);
		}
		
		apnsService.deleteDeviceToken(head, DeleteDeviceTokenRequest.newBuilder()
				.addAllUserId(request.getUserIdList())
				.addAllSession(request.getSessionList())
				.build());
		
		return ServiceUtil.EMPTY_RESPONSE_IMMEDIATE_FUTURE;
	}
	
	@Override
	@AsyncImpl
	public ListenableFuture<EmptyResponse> pushUserDisable(AdminHead head, PushUserDisableRequest request) {
		if (request.getUserIdCount() <= 0 ) {
			return ServiceUtil.EMPTY_RESPONSE_IMMEDIATE_FUTURE;
		}
		
		CloseConnectionRequest closeConnectionRequest = CloseConnectionRequest.newBuilder()
				.addAllUserId(request.getUserIdList())
				.build();
		
		for (ConnService connService : connServiceSet) {
			connService.closeConnection(head, closeConnectionRequest);
		}
		
		return ServiceUtil.EMPTY_RESPONSE_IMMEDIATE_FUTURE;
	}
	
	@Override
	@AsyncImpl
	public ListenableFuture<EmptyResponse> pushUserExpire(RequestHead head, PushUserExpireRequest request) {
		if (request.getExpireSessionIdCount() <= 0) {
			return ServiceUtil.EMPTY_RESPONSE_IMMEDIATE_FUTURE;
		}
		
		CloseConnectionExpireRequest closeConnectionExpireRequest = CloseConnectionExpireRequest.newBuilder()
				.addAllExpireSessionId(request.getExpireSessionIdList())
				.build();
		
		for (ConnService connService : connServiceSet) {
			connService.closeConnectionExpire(head, closeConnectionExpireRequest);
		}
		
		apnsService.deleteDeviceTokenExpire(head, DeleteDeviceTokenExpireRequest.newBuilder()
				.addAllExpireSessionId(request.getExpireSessionIdList())
				.build());
		
		return ServiceUtil.EMPTY_RESPONSE_IMMEDIATE_FUTURE;
	}

	@Override
	@AsyncImpl
	public ListenableFuture<EmptyResponse> pushUserLogout(RequestHead head, EmptyRequest request) {
		for (ConnService connService : connServiceSet) {
			connService.closeConnectionLogout(head, ServiceUtil.EMPTY_REQUEST);
		}
		
		apnsService.deleteDeviceTokenLogout(head, ServiceUtil.EMPTY_REQUEST);
		
		return ServiceUtil.EMPTY_RESPONSE_IMMEDIATE_FUTURE;
	}

}
