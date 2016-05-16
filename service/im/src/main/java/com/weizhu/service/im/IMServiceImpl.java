package com.weizhu.service.im;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
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

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.inject.Inject;
import com.weizhu.common.db.DBUtil;
import com.weizhu.proto.IMProtos.CreateGroupChatRequest;
import com.weizhu.proto.IMProtos.CreateGroupChatResponse;
import com.weizhu.proto.IMProtos.GetGroupChatByIdRequest;
import com.weizhu.proto.IMProtos.GetGroupChatByIdResponse;
import com.weizhu.proto.IMProtos.GetGroupChatListRequest;
import com.weizhu.proto.IMProtos.GetGroupChatListResponse;
import com.weizhu.proto.IMProtos.GetGroupMessageRequest;
import com.weizhu.proto.IMProtos.GetMessageResponse;
import com.weizhu.proto.IMProtos.GetP2PChatListRequest;
import com.weizhu.proto.IMProtos.GetP2PChatListResponse;
import com.weizhu.proto.IMProtos.GetP2PMessageRequest;
import com.weizhu.proto.IMProtos.IMP2PMessagePush;
import com.weizhu.proto.IMProtos.JoinGroupChatRequest;
import com.weizhu.proto.IMProtos.JoinGroupChatResponse;
import com.weizhu.proto.IMProtos.LeaveGroupChatRequest;
import com.weizhu.proto.IMProtos.LeaveGroupChatResponse;
import com.weizhu.proto.IMProtos.SendGroupMessageRequest;
import com.weizhu.proto.IMProtos.SendGroupMessageResponse;
import com.weizhu.proto.IMProtos.SendP2PMessageRequest;
import com.weizhu.proto.IMProtos.SendP2PMessageResponse;
import com.weizhu.proto.IMProtos.SetGroupNameRequest;
import com.weizhu.proto.IMProtos.SetGroupNameResponse;
import com.weizhu.proto.PushProtos.PushMsgRequest;
import com.weizhu.proto.PushProtos.PushStateRequest;
import com.weizhu.proto.PushProtos.PushTarget;
import com.weizhu.proto.IMProtos;
import com.weizhu.proto.IMService;
import com.weizhu.proto.PushProtos;
import com.weizhu.proto.PushService;
import com.weizhu.proto.UserProtos;
import com.weizhu.proto.UserProtos.GetUserByIdRequest;
import com.weizhu.proto.UserProtos.GetUserResponse;
import com.weizhu.proto.UserService;
import com.weizhu.proto.WeizhuProtos.RequestHead;
import com.zaxxer.hikari.HikariDataSource;

public class IMServiceImpl implements IMService {

	@SuppressWarnings("unused")
	private static final Logger logger = LoggerFactory.getLogger(IMServiceImpl.class);
	
	private final HikariDataSource hikariDataSource;
	private final JedisPool jedisPool;
	
	private final PushService pushService;
	private final UserService userService;
	
	@Inject
	public IMServiceImpl(HikariDataSource hikariDataSource, JedisPool jedisPool, 
			PushService pushService, UserService userService
			) {
		this.hikariDataSource = hikariDataSource;
		this.jedisPool = jedisPool;
		this.pushService = pushService;
		this.userService = userService;
		
		Jedis jedis = jedisPool.getResource();
		try {
			IMCache.loadScript(jedis);
		} finally {
			jedis.close();
		}
	}
	
	@Override
	public ListenableFuture<GetMessageResponse> getP2PMessage(RequestHead head, GetP2PMessageRequest request) {
		final long companyId = head.getSession().getCompanyId();
		
		final long userIdMost;
		final long userIdLeast;
		if (head.getSession().getUserId() > request.getUserId()) {
			userIdMost = head.getSession().getUserId();
			userIdLeast = request.getUserId();
		} else {
			userIdMost = request.getUserId();
			userIdLeast = head.getSession().getUserId();
		}
		
		final Long msgSeqBegin = request.hasMsgSeqBegin() ? request.getMsgSeqBegin() : null;
		final Long msgSeqEnd = request.hasMsgSeqEnd() ? request.getMsgSeqEnd() : null;
		
		// 多获取一条数据用于判断 hasMore
		List<IMProtos.InstantMessage> msgList = doGetP2PMessage(companyId, userIdMost, userIdLeast, msgSeqBegin, msgSeqEnd, request.getMsgSize() + 1);
		
		boolean hasMore;
		if (msgList.size() > request.getMsgSize()) {
			hasMore = true;
			msgList = msgList.subList(0, request.getMsgSize());
		} else {
			hasMore = false;
		}
		
		return Futures.immediateFuture(GetMessageResponse.newBuilder()
				.addAllMsg(msgList)
				.setHasMore(hasMore)
				.build());
	}
	
	private List<IMProtos.InstantMessage> doGetP2PMessage(long companyId, long userIdMost, long userIdLeast, Long msgSeqBegin, Long msgSeqEnd, int size) {
		Long latestMsgSeq = null;
		Jedis jedis = jedisPool.getResource();
		try {
			latestMsgSeq = IMCache.getLatestP2PMsgSeq(jedis, companyId, userIdMost, userIdLeast);
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
			
			return IMDB.getP2PMessage(dbConn, companyId, userIdMost, userIdLeast, msgSeqBegin, msgSeqEnd, size);
			
		} catch (SQLException e) {
			throw new RuntimeException("get p2p message db fail!", e);
		} finally {
			DBUtil.closeQuietly(dbConn);
		}
	}

	@Override
	public ListenableFuture<SendP2PMessageResponse> sendP2PMessage(RequestHead head, SendP2PMessageRequest request) {
		final long companyId = head.getSession().getCompanyId();
		
		String failText = checkSendMessage(request.getMsg());
		if (failText != null) {
			return Futures.immediateFuture(SendP2PMessageResponse.newBuilder()
					.setResult(SendP2PMessageResponse.Result.FAIL_MSG_INVALID)
					.setFailText(failText)
					.build());
		}
		
		// 检查用户id是否有效
		Set<Long> invalidUserIdSet = checkUserById(head, Collections.singleton(request.getToUserId()));
		if (!invalidUserIdSet.isEmpty()) {
			return Futures.immediateFuture(SendP2PMessageResponse.newBuilder()
					.setResult(SendP2PMessageResponse.Result.FAIL_USER_NOT_EXIST)
					.setFailText("用户不存在")
					.build());
		}
		
		final long userIdMost;
		final long userIdLeast;
		if (head.getSession().getUserId() > request.getToUserId()) {
			userIdMost = head.getSession().getUserId();
			userIdLeast = request.getToUserId();
		} else {
			userIdMost = request.getToUserId();
			userIdLeast = head.getSession().getUserId();
		}
		
		// 1. 生成 seq
		Long msgSeq = null;
		Jedis jedis = jedisPool.getResource();
		try {
			msgSeq = IMCache.generateP2PMsgSeq(jedis, companyId, userIdMost, userIdLeast);
		} finally {
			jedis.close();
		}
		
		if (msgSeq == null) {
			long dbMsgSeq;
			Connection dbConn = null;
			try {
				dbConn = hikariDataSource.getConnection();
				dbMsgSeq = IMDB.getP2PLatestMsgSeq(dbConn, companyId, userIdMost, userIdLeast);
			} catch (SQLException e) {
				throw new RuntimeException("get p2p message db fail!", e);
			} finally {
				DBUtil.closeQuietly(dbConn);
			}
			
			jedis = jedisPool.getResource();
			try {
				IMCache.setnxP2PMsgSeq(jedis, companyId, userIdMost, userIdLeast, dbMsgSeq);
				msgSeq = IMCache.generateP2PMsgSeq(jedis, companyId, userIdMost, userIdLeast);
			} finally {
				jedis.close();
			}
			
			if (msgSeq == null) {
				throw new RuntimeException("cannot get p2p message seq");
			}
		}
		
		IMProtos.InstantMessage msg = request.getMsg().toBuilder()
				.setMsgSeq(msgSeq)
				.setMsgTime((int) (System.currentTimeMillis() / 1000L))
				.setFromUserId(head.getSession().getUserId())
				.build();
		
		// 2. 存入db
		Connection dbConn = null;
		try {
			dbConn = hikariDataSource.getConnection();
			IMDB.insertP2PMessage(dbConn, companyId, userIdMost, userIdLeast, msg);
		} catch (SQLException e) {
			throw new RuntimeException("get p2p message db fail!", e);
		} finally {
			DBUtil.closeQuietly(dbConn);
		}
		
		// 3. push
		
		PushMsgRequest.Builder pushMsgRequestBuilder = PushMsgRequest.newBuilder();
		
		if (head.getSession().getUserId() != request.getToUserId()) {
			// 发送到对方
			pushMsgRequestBuilder.addPushPacket(
					PushProtos.PushPacket.newBuilder()
						.addPushTarget(PushTarget.newBuilder()
								.setUserId(request.getToUserId())
								.setEnableOffline(true)
								.build())
						.setPushName("IMP2PMessagePush")
						.setPushBody(IMP2PMessagePush.newBuilder()
								.setUserId(head.getSession().getUserId())
								.setMsg(msg)
								.build().toByteString())
						.build());
		}
		
		// 发送到自己其他终端上
		pushMsgRequestBuilder.addPushPacket(
				PushProtos.PushPacket.newBuilder()
					.addPushTarget(PushTarget.newBuilder()
							.setUserId(head.getSession().getUserId())
							.addExcludeSessionId(head.getSession().getSessionId())
							.setEnableOffline(true)
							.build())
					.setPushName("IMP2PMessagePush")
					.setPushBody(IMP2PMessagePush.newBuilder()
							.setUserId(request.getToUserId())
							.setMsg(msg)
							.build().toByteString())
					.build());
		
		this.pushService.pushMsg(head, pushMsgRequestBuilder.build());
		
		return Futures.immediateFuture(SendP2PMessageResponse.newBuilder()
				.setResult(SendP2PMessageResponse.Result.SUCC)
				.setMsgSeq(msg.getMsgSeq())
				.setMsgTime(msg.getMsgTime())
				.build());
	}

	@Override
	public ListenableFuture<GetP2PChatListResponse> getP2PChatList(RequestHead head, GetP2PChatListRequest request) {
		final long companyId = head.getSession().getCompanyId();
		final long userId = head.getSession().getUserId();
		
		if (request.getChatSize() <= 0) {
			return Futures.immediateFuture(GetP2PChatListResponse.newBuilder().setHasMore(false).build());
		}
		
		int chatSize = request.getChatSize();
		if (chatSize > 50) {
			chatSize = 50;
		}
		
		Connection dbConn = null;
		try {
			dbConn = hikariDataSource.getConnection();
			
			List<IMProtos.P2PChat> list;
			if (request.hasLastUserId() && request.hasLastMsgTime()) {
				list = IMDB.getP2PChatList(dbConn, companyId, userId, request.getLastUserId(), request.getLastMsgTime(), chatSize + 1);
			} else {
				list = IMDB.getP2PChatList(dbConn, companyId, userId, chatSize + 1);
			}
			
			GetP2PChatListResponse.Builder responseBuilder = GetP2PChatListResponse.newBuilder();
			responseBuilder.setHasMore(list.size() > chatSize);
			for (IMProtos.P2PChat p2pChat : list) {
				responseBuilder.addChat(p2pChat);
				if (responseBuilder.getChatCount() >= chatSize) {
					break;
				}
			}
			return Futures.immediateFuture(responseBuilder.build());
			
		} catch (SQLException e) {
			throw new RuntimeException("getP2PChatList db fail!", e);
		} finally {
			DBUtil.closeQuietly(dbConn);
		}
	}
	
	@Override
	public ListenableFuture<GetGroupChatByIdResponse> getGroupChatById(RequestHead head, GetGroupChatByIdRequest request) {
		final long companyId = head.getSession().getCompanyId();
		
		if (request.getGroupIdCount() <= 0 || request.getGroupIdCount() > 50) {
			return Futures.immediateFuture(GetGroupChatByIdResponse.newBuilder().build());
		}
		
		Map<Long, IMProtos.GroupChat> groupChatMap = this.doGetGroupChat(companyId, request.getGroupIdList());
		
		GetGroupChatByIdResponse.Builder responseBuilder = GetGroupChatByIdResponse.newBuilder();
		for (IMProtos.GroupChat groupChat : groupChatMap.values()) {
			if (checkIsMember(head.getSession().getUserId(), groupChat)) {
				responseBuilder.addGroupChat(groupChat);
			}
		}
		
		return Futures.immediateFuture(responseBuilder.build());
	}
	
	@Override
	public ListenableFuture<GetMessageResponse> getGroupMessage(RequestHead head, GetGroupMessageRequest request) {
		final long companyId = head.getSession().getCompanyId();
		
		IMProtos.GroupChat groupChat = this.doGetGroupChat(companyId, Collections.singleton(request.getGroupId())).get(request.getGroupId());
		if (groupChat == null || !checkIsMember(head.getSession().getUserId(), groupChat)) {
			return Futures.immediateFuture(GetMessageResponse.newBuilder()
					.setHasMore(false)
					.build());
		}
		
		Long msgSeqBegin = request.hasMsgSeqBegin() ? request.getMsgSeqBegin() : null;
		Long msgSeqEnd = request.hasMsgSeqEnd() ? request.getMsgSeqEnd() : null;
		
		for (int i=0; i<groupChat.getMemberCount(); ++i) {
			IMProtos.GroupChat.Member member = groupChat.getMember(i);
			if (member.getUserId() == head.getSession().getUserId()) {
				if (msgSeqEnd == null || msgSeqEnd < member.getJoinMsgSeq() - 1) {
					msgSeqEnd = member.getJoinMsgSeq() - 1;
				}
			}
		}
		
		// 多获取一条数据用于判断 hasMore
		List<IMProtos.InstantMessage> msgList = doGetGroupMessage(companyId, request.getGroupId(), msgSeqBegin, msgSeqEnd, request.getMsgSize() + 1);
		
		boolean hasMore;
		if (msgList.size() > request.getMsgSize()) {
			hasMore = true;
			msgList = msgList.subList(0, request.getMsgSize());
		} else {
			hasMore = false;
		}
		
		return Futures.immediateFuture(GetMessageResponse.newBuilder()
				.addAllMsg(msgList)
				.setHasMore(hasMore)
				.build());
	}
	
	private List<IMProtos.InstantMessage> doGetGroupMessage(long companyId, long groupId, Long msgSeqBegin, Long msgSeqEnd, int size) {
		
		List<IMProtos.InstantMessage> cacheMsgList;
		Jedis jedis = jedisPool.getResource();
		try {
			Long latestMsgSeq = IMCache.getLatestGroupMsgSeq(jedis, companyId, groupId);
			if (latestMsgSeq != null) {
				if ((msgSeqEnd == null && latestMsgSeq <= 0) 
						|| (msgSeqEnd != null && latestMsgSeq <= msgSeqEnd)) {
					return Collections.emptyList();
				}
			}
			cacheMsgList = IMCache.getGroupMsg(jedis, companyId, groupId);
		} finally {
			jedis.close();
		}
		
		if (!cacheMsgList.isEmpty()) {
			List<IMProtos.InstantMessage> resultList = new ArrayList<IMProtos.InstantMessage>(size);
			long lastMsgSeq = 0;
			for (IMProtos.InstantMessage msg : cacheMsgList) {
				lastMsgSeq = msg.getMsgSeq();
				if (msgSeqBegin != null && msg.getMsgSeq() >= msgSeqBegin) {
					continue;
				}
				if (msgSeqEnd != null && msg.getMsgSeq() <= msgSeqEnd) {
					continue;
				}
				if (resultList.size() < size) {
					resultList.add(msg);
				}
			}
			
			if (lastMsgSeq <= 1 // no more data
					|| resultList.size() >= size // size enough
					|| (msgSeqEnd != null && lastMsgSeq <= msgSeqEnd) // reach the end
					) {
				return resultList;
			}
		}
		
		Connection dbConn = null;
		try {
			dbConn = hikariDataSource.getConnection();
			
			return IMDB.getGroupMessage(dbConn, companyId, groupId, msgSeqBegin, msgSeqEnd, size);
			
		} catch (SQLException e) {
			throw new RuntimeException("get group message db fail!", e);
		} finally {
			DBUtil.closeQuietly(dbConn);
		}
	}

	@Override
	public ListenableFuture<CreateGroupChatResponse> createGroupChat(RequestHead head, CreateGroupChatRequest request) {
		final long companyId = head.getSession().getCompanyId();
		// check arg
		if (request.getGroupName().isEmpty() || request.getGroupName().length() > 50) {
			return Futures.immediateFuture(CreateGroupChatResponse.newBuilder()
					.setResult(CreateGroupChatResponse.Result.FAIL_NAME_INVALID)
					.setFailText("群组名称必须非空且长度小于等于50")
					.build());
		}
		if (request.getMemberUserIdCount() <= 0) {
			return Futures.immediateFuture(CreateGroupChatResponse.newBuilder()
					.setResult(CreateGroupChatResponse.Result.FAIL_MEMBER_EMPTY)
					.setFailText("群组成员不能为空")
					.build());
		}
		
		Set<Long> memberUserIdSet = new TreeSet<Long>(request.getMemberUserIdList());
		memberUserIdSet.add(head.getSession().getUserId());
		if (memberUserIdSet.size() < 3) {
			return Futures.immediateFuture(CreateGroupChatResponse.newBuilder()
					.setResult(CreateGroupChatResponse.Result.FAIL_MEMBER_INVALID)
					.setFailText("群组成员必须大于等于3人")
					.build());
		}
		if (memberUserIdSet.size() > 100) {
			return Futures.immediateFuture(CreateGroupChatResponse.newBuilder()
					.setResult(CreateGroupChatResponse.Result.FAIL_MEMBER_INVALID)
					.setFailText("群组成员必须小于等于100人")
					.build());
		}
		
		Set<Long> invalidUserIdSet = checkUserById(head, memberUserIdSet);
		if (!invalidUserIdSet.isEmpty()) {
			return Futures.immediateFuture(CreateGroupChatResponse.newBuilder()
					.setResult(CreateGroupChatResponse.Result.FAIL_MEMBER_INVALID)
					.setFailText("群组用户不正确: " + invalidUserIdSet)
					.build());
		}
		
		IMProtos.GroupChat groupChat;
		
		// insert db
		Connection dbConn = null;
		try {
			dbConn = hikariDataSource.getConnection();
			
			long groupId = IMDB.insertGroupChat(dbConn, companyId, request.getGroupName());
			IMDB.updateGroupMember(dbConn, companyId, groupId, memberUserIdSet, Collections.<Long>emptyList(), 0);
			
			IMProtos.GroupChat.Builder groupChatBuilder = IMProtos.GroupChat.newBuilder()
					.setGroupId(groupId)
					.setGroupName(request.getGroupName());
			
			IMProtos.GroupChat.Member.Builder tmpMemberBuilder = IMProtos.GroupChat.Member.newBuilder();
			for (Long userId : memberUserIdSet) {
				groupChatBuilder.addMember(tmpMemberBuilder.clear()
						.setUserId(userId)
						.setJoinMsgSeq(0)
						.build());
			}
			
			groupChat = groupChatBuilder.build();
			
		} catch (SQLException e) {
			throw new RuntimeException("createGroupChat db fail!", e);
		} finally {
			DBUtil.closeQuietly(dbConn);
		}
		
		// update cache
		Jedis jedis = jedisPool.getResource();
		try {
			IMCache.setGroupChat(jedis, companyId, Collections.singletonMap(groupChat.getGroupId(), groupChat));
			IMCache.setnxGroupMsgSeq(jedis, companyId, groupChat.getGroupId(), 0);
		} finally {
			jedis.close();
		}
		
		// push state
		pushGroupState(head, groupChat);

		return Futures.immediateFuture(CreateGroupChatResponse.newBuilder()
				.setResult(CreateGroupChatResponse.Result.SUCC)
				.setGroupChat(groupChat)
				.build());
	}
	
	@Override
	public ListenableFuture<SetGroupNameResponse> setGroupName(RequestHead head, SetGroupNameRequest request) {
		final long companyId = head.getSession().getCompanyId();
		// check arg
		if (request.getGroupName().isEmpty() || request.getGroupName().length() > 50) {
			return Futures.immediateFuture(SetGroupNameResponse.newBuilder()
					.setResult(SetGroupNameResponse.Result.FAIL_NAME_INVALID)
					.setFailText("群组名称必须非空且长度小于等于50")
					.build());
		}
		
		IMProtos.GroupChat groupChat = this.doGetGroupChat(companyId, Collections.singleton(request.getGroupId())).get(request.getGroupId());
		if (groupChat == null) {
			return Futures.immediateFuture(SetGroupNameResponse.newBuilder()
					.setResult(SetGroupNameResponse.Result.FAIL_GROUP_NOT_EXIST)
					.setFailText("群组不存在")
					.build());
		}
		if (!checkIsMember(head.getSession().getUserId(), groupChat)) {
			return Futures.immediateFuture(SetGroupNameResponse.newBuilder()
					.setResult(SetGroupNameResponse.Result.FAIL_GROUP_NOT_JOIN)
					.setFailText("您没有加入该群组")
					.build());
		}
		
		long msgSeq = this.generateGroupMsgSeq(companyId, request.getGroupId());
		IMProtos.InstantMessage groupMsg = IMProtos.InstantMessage.newBuilder()
				.setMsgSeq(msgSeq)
				.setMsgTime((int) (System.currentTimeMillis() / 1000L))
				.setFromUserId(head.getSession().getUserId())
				.setGroup(IMProtos.InstantMessage.Group.newBuilder()
						.setGroupName(request.getGroupName())
						.build())
				.build();
		
		Connection dbConn = null;
		try {
			dbConn = hikariDataSource.getConnection();
			IMDB.updateGroupName(dbConn, companyId, request.getGroupId(), request.getGroupName());
			IMDB.insertGroupMessage(dbConn, companyId, request.getGroupId(), groupMsg);
			groupChat = IMDB.getGroupChatById(dbConn, companyId, Collections.singleton(request.getGroupId())).get(request.getGroupId());
		} catch (SQLException e) {
			throw new RuntimeException("createGroupChat db fail!", e);
		} finally {
			DBUtil.closeQuietly(dbConn);
		}

		Jedis jedis = jedisPool.getResource();
		try {
			IMCache.setGroupChat(jedis, companyId, Collections.singletonMap(groupChat.getGroupId(), groupChat));
			IMCache.addGroupMsg(jedis, companyId, groupChat.getGroupId(), Collections.singletonList(groupMsg));
		} finally {
			jedis.close();
		}
		
		pushGroupState(head, groupChat);
		
		return Futures.immediateFuture(SetGroupNameResponse.newBuilder()
				.setResult(SetGroupNameResponse.Result.SUCC)
				.setGroupMsg(groupMsg)
				.setGroupChat(groupChat)
				.build());
	}

	@Override
	public ListenableFuture<JoinGroupChatResponse> joinGroupChat(RequestHead head, JoinGroupChatRequest request) {
		final long companyId = head.getSession().getCompanyId();
		// check arg
		if (request.getJoinUserIdCount() <= 0) {
			return Futures.immediateFuture(JoinGroupChatResponse.newBuilder()
					.setResult(JoinGroupChatResponse.Result.FAIL_USER_EMPTY)
					.setFailText("被邀请加入群组的用户为空")
					.build());
		}
		
		IMProtos.GroupChat groupChat = this.doGetGroupChat(companyId, Collections.singleton(request.getGroupId())).get(request.getGroupId());
		if (groupChat == null) {
			return Futures.immediateFuture(JoinGroupChatResponse.newBuilder()
					.setResult(JoinGroupChatResponse.Result.FAIL_GROUP_NOT_EXIST)
					.setFailText("群组不存在")
					.build());
		}
		if (!checkIsMember(head.getSession().getUserId(), groupChat)) {
			return Futures.immediateFuture(JoinGroupChatResponse.newBuilder()
					.setResult(JoinGroupChatResponse.Result.FAIL_GROUP_NOT_JOIN)
					.setFailText("您没有加入该群组")
					.build());
		}
		Set<Long> joinUserIdSet = new TreeSet<Long>(request.getJoinUserIdList());
		for (int i=0; i<groupChat.getMemberCount(); ++i) {
			long memberUserId = groupChat.getMember(i).getUserId();
			if (joinUserIdSet.contains(memberUserId)) {
				return Futures.immediateFuture(JoinGroupChatResponse.newBuilder()
						.setResult(JoinGroupChatResponse.Result.FAIL_USER_INVALID)
						.setFailText("用户已加入群组: " + memberUserId)
						.build());
			}
		}
		if (joinUserIdSet.size() + groupChat.getMemberCount() > 100) {
			return Futures.immediateFuture(JoinGroupChatResponse.newBuilder()
					.setResult(JoinGroupChatResponse.Result.FAIL_MEMBER_NUM_LIMITED)
					.setFailText("超过群组最大人数限制：群组成员必须小于等于100人")
					.build());
		}
		
		Set<Long> invalidUserIdSet = this.checkUserById(head, joinUserIdSet);
		if (!invalidUserIdSet.isEmpty()) {
			return Futures.immediateFuture(JoinGroupChatResponse.newBuilder()
					.setResult(JoinGroupChatResponse.Result.FAIL_USER_INVALID)
					.setFailText("群组加入用户不正确: " + invalidUserIdSet)
					.build());
		}
		
		long msgSeq = this.generateGroupMsgSeq(companyId, request.getGroupId());
		IMProtos.InstantMessage groupMsg = IMProtos.InstantMessage.newBuilder()
				.setMsgSeq(msgSeq)
				.setMsgTime((int) (System.currentTimeMillis() / 1000L))
				.setFromUserId(head.getSession().getUserId())
				.setGroup(IMProtos.InstantMessage.Group.newBuilder()
						.addAllJoinUserId(joinUserIdSet)
						.build())
				.build();
		
		Connection dbConn = null;
		try {
			dbConn = hikariDataSource.getConnection();
			IMDB.updateGroupMember(dbConn, companyId, request.getGroupId(), joinUserIdSet, Collections.<Long>emptyList(), msgSeq);
			IMDB.insertGroupMessage(dbConn, companyId, request.getGroupId(), groupMsg);
			groupChat = IMDB.getGroupChatById(dbConn, companyId, Collections.singleton(request.getGroupId())).get(request.getGroupId());
		} catch (SQLException e) {
			throw new RuntimeException("createGroupChat db fail!", e);
		} finally {
			DBUtil.closeQuietly(dbConn);
		}

		Jedis jedis = jedisPool.getResource();
		try {
			IMCache.setGroupChat(jedis, companyId, Collections.singletonMap(groupChat.getGroupId(), groupChat));
			IMCache.addGroupMsg(jedis, companyId, groupChat.getGroupId(), Collections.singletonList(groupMsg));
		} finally {
			jedis.close();
		}
		
		pushGroupState(head, groupChat);
		
		return Futures.immediateFuture(JoinGroupChatResponse.newBuilder()
				.setResult(JoinGroupChatResponse.Result.SUCC)
				.setGroupMsg(groupMsg)
				.setGroupChat(groupChat)
				.build());
	}

	@Override
	public ListenableFuture<LeaveGroupChatResponse> leaveGroupChat(RequestHead head, LeaveGroupChatRequest request) {
		final long companyId = head.getSession().getCompanyId();
		// check arg
		IMProtos.GroupChat groupChat = this.doGetGroupChat(companyId, Collections.singleton(request.getGroupId())).get(request.getGroupId());
		if (groupChat == null) {
			return Futures.immediateFuture(LeaveGroupChatResponse.newBuilder()
					.setResult(LeaveGroupChatResponse.Result.FAIL_GROUP_NOT_EXIST)
					.setFailText("群组不存在")
					.build());
		}
		if (!checkIsMember(head.getSession().getUserId(), groupChat)) {
			return Futures.immediateFuture(LeaveGroupChatResponse.newBuilder()
					.setResult(LeaveGroupChatResponse.Result.FAIL_GROUP_NOT_JOIN)
					.setFailText("您没有加入该群组")
					.build());
		}
		
		long msgSeq = this.generateGroupMsgSeq(companyId, request.getGroupId());
		IMProtos.InstantMessage groupMsg = IMProtos.InstantMessage.newBuilder()
				.setMsgSeq(msgSeq)
				.setMsgTime((int) (System.currentTimeMillis() / 1000L))
				.setFromUserId(head.getSession().getUserId())
				.setGroup(IMProtos.InstantMessage.Group.newBuilder()
						.addLeaveUserId(head.getSession().getUserId())
						.build())
				.build();
		
		Connection dbConn = null;
		try {
			dbConn = hikariDataSource.getConnection();
			IMDB.updateGroupMember(dbConn, companyId, request.getGroupId(), 
					Collections.<Long>emptyList(), Collections.singleton(head.getSession().getUserId()), msgSeq);
			IMDB.insertGroupMessage(dbConn, companyId, request.getGroupId(), groupMsg);
			groupChat = IMDB.getGroupChatById(dbConn, companyId, Collections.singleton(request.getGroupId())).get(request.getGroupId());
		} catch (SQLException e) {
			throw new RuntimeException("leaveGroupChat db fail!", e);
		} finally {
			DBUtil.closeQuietly(dbConn);
		}

		Jedis jedis = jedisPool.getResource();
		try {
			IMCache.setGroupChat(jedis, companyId, Collections.singletonMap(groupChat.getGroupId(), groupChat));
			IMCache.addGroupMsg(jedis, companyId, groupChat.getGroupId(), Collections.singletonList(groupMsg));
		} finally {
			jedis.close();
		}
		
		pushGroupState(head, groupChat);
		
		return Futures.immediateFuture(LeaveGroupChatResponse.newBuilder()
				.setResult(LeaveGroupChatResponse.Result.SUCC)
				.setGroupMsg(groupMsg)
				.setGroupChat(groupChat)
				.build());
	}
	
	@Override
	public ListenableFuture<SendGroupMessageResponse> sendGroupMessage(RequestHead head, SendGroupMessageRequest request) {
		final long companyId = head.getSession().getCompanyId();
		String failText = checkSendMessage(request.getMsg());
		if (failText != null) {
			return Futures.immediateFuture(SendGroupMessageResponse.newBuilder()
					.setResult(SendGroupMessageResponse.Result.FAIL_MSG_INVALID)
					.setFailText(failText)
					.build());
		}
		
		IMProtos.GroupChat groupChat = this.doGetGroupChat(companyId, Collections.singleton(request.getGroupId())).get(request.getGroupId());
		if (groupChat == null) {
			return Futures.immediateFuture(SendGroupMessageResponse.newBuilder()
					.setResult(SendGroupMessageResponse.Result.FAIL_GROUP_NOT_EXIST)
					.setFailText("群组不存在")
					.build());
		}
		if (!checkIsMember(head.getSession().getUserId(), groupChat)) {
			return Futures.immediateFuture(SendGroupMessageResponse.newBuilder()
					.setResult(SendGroupMessageResponse.Result.FAIL_GROUP_NOT_JOIN)
					.setFailText("您没有加入该群组")
					.build());
		}
		
		long msgSeq = this.generateGroupMsgSeq(companyId, request.getGroupId());
		IMProtos.InstantMessage sendMsg = request.getMsg().toBuilder()
				.setMsgSeq(msgSeq)
				.setMsgTime((int) (System.currentTimeMillis() / 1000L))
				.setFromUserId(head.getSession().getUserId())
				.build();
		
		Connection dbConn = null;
		try {
			dbConn = hikariDataSource.getConnection();
			IMDB.insertGroupMessage(dbConn, companyId, request.getGroupId(), sendMsg);
		} catch (SQLException e) {
			throw new RuntimeException("createGroupChat db fail!", e);
		} finally {
			DBUtil.closeQuietly(dbConn);
		}

		Jedis jedis = jedisPool.getResource();
		try {
			IMCache.addGroupMsg(jedis, companyId, groupChat.getGroupId(), Collections.singletonList(sendMsg));
		} finally {
			jedis.close();
		}
		
		pushGroupState(head, groupChat);
		
		return Futures.immediateFuture(SendGroupMessageResponse.newBuilder()
				.setResult(SendGroupMessageResponse.Result.SUCC)
				.setMsgSeq(sendMsg.getMsgSeq())
				.setMsgTime(sendMsg.getMsgTime())
				.build());
	}
	
	private Map<Long, IMProtos.GroupChat> doGetGroupChat(long companyId, Collection<Long> groupIds) {
		if (groupIds.isEmpty()) {
			return Collections.emptyMap();
		}
		
		Map<Long, IMProtos.GroupChat> resultMap = new TreeMap<Long, IMProtos.GroupChat>();
		
		Set<Long> noCacheGroupIdSet = new TreeSet<Long>();
		Jedis jedis = jedisPool.getResource();
		try {
			resultMap.putAll(IMCache.getGroupChat(jedis, companyId, groupIds, noCacheGroupIdSet));
		} finally {
			jedis.close();
		}
		
		if (noCacheGroupIdSet.isEmpty()) {
			return resultMap;
		}
		
		Map<Long, IMProtos.GroupChat> noCacheMap;
		Connection dbConn = null;
		try {
			dbConn = hikariDataSource.getConnection();
			noCacheMap = IMDB.getGroupChatById(dbConn, companyId, noCacheGroupIdSet);
		} catch (SQLException e) {
			throw new RuntimeException("getGroupChat db fail!", e);
		} finally {
			DBUtil.closeQuietly(dbConn);
		}
		
		jedis = jedisPool.getResource();
		try {
			IMCache.setGroupChat(jedis, companyId, noCacheGroupIdSet, noCacheMap);
		} finally {
			jedis.close();
		}
		
		resultMap.putAll(noCacheMap);
		
		return resultMap;
	}
	
	private boolean checkIsMember(long userId, IMProtos.GroupChat groupChat) {
		for (int i=0; i<groupChat.getMemberCount(); ++i) {
			if (groupChat.getMember(i).getUserId() == userId) {
				return true;
			}
		}
		return false;
	}
	
	private long generateGroupMsgSeq(long companyId, long groupId) {
		Long msgSeq = null;
		Jedis jedis = jedisPool.getResource();
		try {
			msgSeq = IMCache.generateGroupMsgSeq(jedis, companyId, groupId);
		} finally {
			jedis.close();
		}
		
		if (msgSeq != null) {
			return msgSeq;
		}
		
		long dbMsgSeq;
		Connection dbConn = null;
		try {
			dbConn = hikariDataSource.getConnection();
			dbMsgSeq = IMDB.getGroupLatestMsgSeq(dbConn, companyId, groupId);
		} catch (SQLException e) {
			throw new RuntimeException("get p2p message db fail!", e);
		} finally {
			DBUtil.closeQuietly(dbConn);
		}
		
		jedis = jedisPool.getResource();
		try {
			IMCache.setnxGroupMsgSeq(jedis, companyId, groupId, dbMsgSeq);
			msgSeq = IMCache.generateGroupMsgSeq(jedis, companyId, groupId);
		} finally {
			jedis.close();
		}
		
		if (msgSeq == null) {
			throw new RuntimeException("cannot get p2p message seq");
		}
		
		return msgSeq;
	}
	
	private void pushGroupState(RequestHead head, IMProtos.GroupChat groupChat) {
		if (groupChat.getMemberCount() <= 0) {
			return;
		}
		
		PushProtos.PushPacket.Builder pushPacketBuilder = PushProtos.PushPacket.newBuilder()
				.setPushName("IMGroupStatePush")
				.setPushBody(IMProtos.IMGroupStatePush.newBuilder()
						.setGroupId(groupChat.getGroupId())
						.build().toByteString());
		PushProtos.PushTarget.Builder tmpPushTargetBuilder = PushProtos.PushTarget.newBuilder();
		for (int i=0; i<groupChat.getMemberCount(); ++i) {
			IMProtos.GroupChat.Member member = groupChat.getMember(i);
			if (member.getUserId() != head.getSession().getUserId()) {
				pushPacketBuilder.addPushTarget(tmpPushTargetBuilder.clear()
						.setUserId(member.getUserId())
						.setEnableOffline(true)
						.build());
			} else {
				pushPacketBuilder.addPushTarget(tmpPushTargetBuilder.clear()
						.setUserId(head.getSession().getUserId())
						.addExcludeSessionId(head.getSession().getSessionId())
						.setEnableOffline(true)
						.build());
			}
		}
		pushService.pushState(head, PushStateRequest.newBuilder().addPushPacket(pushPacketBuilder.build()).build());
	}

	@Override
	public ListenableFuture<GetGroupChatListResponse> getGroupChatList(RequestHead head, GetGroupChatListRequest request) {
		final long companyId = head.getSession().getCompanyId();
		// check arg
		if (request.hasLastGroupId() || request.hasLastMsgTime() || request.getChatSize() <= 0 || request.getChatSize() > 100) {
			return Futures.immediateFuture(GetGroupChatListResponse.newBuilder().setHasMore(false).build());
		}
		
		List<IMProtos.GroupChat> groupChatList;
		Connection dbConn = null;
		try {
			dbConn = hikariDataSource.getConnection();
			groupChatList = IMDB.getGroupChatList(dbConn, companyId, head.getSession().getUserId(), request.getChatSize());
		} catch (SQLException e) {
			throw new RuntimeException("getGroupChatList db fail!", e);
		} finally {
			DBUtil.closeQuietly(dbConn);
		}
		
		return Futures.immediateFuture(GetGroupChatListResponse.newBuilder()
				.addAllChat(groupChatList)
				.setHasMore(false)
				.build());
	}
	
	/**
	 * 检查发送的消息是否正确。 如果正确 return null, 如果不正确 return failText;
	 */
	private String checkSendMessage(IMProtos.InstantMessage msg) {
		switch (msg.getMsgTypeCase()) {
			case TEXT: {
				IMProtos.InstantMessage.Text text = msg.getText();
				if (text.getContent().length() > 65535) {
					return "发送文本内容超长";
				}
				return null;
			}
			case VOICE: {
				IMProtos.InstantMessage.Voice voice = msg.getVoice();
				if (voice.getData().size() > 65535) {
					return "发送语音内容超长";
				}
				return null;
			}
			case IMAGE: {
				IMProtos.InstantMessage.Image image = msg.getImage();
				if (image.getName().length() > 191) {
					return "图片名称超长";
				}
				return null;
			}
			case USER: {
				return null;
			}
			case VIDEO: {
				IMProtos.InstantMessage.Video video = msg.getVideo();
				if (video.getName().isEmpty()) {
					return "视频名称为空";
				} else if (video.getName().length() > 191) {
					return "视频名称超长";
				} else if (video.getType().isEmpty()) {
					return "视频类型为空";
				} else if (video.getType().length() > 191) {
					return "视频类型超长";
				} else if (video.getSize() <= 0 || video.getSize() > 40 * 1024 * 1024) {
					return "视频大小不正确";
				} else if (video.getTime() <= 0 || video.getTime() > 1000) {
					return "视频长度不正确";
				} else if (video.getImageName().isEmpty()) {
					return "视频截图名称为空";
				} else if (video.getImageName().length() > 191) {
					return "视频截图名称超长";
				}
				return null;
			}
			case FILE: {
				return "暂不支持发送此类型消息";
			}
			case GROUP: {
				return "不能发送此类型消息";
			}
			case DISCOVER_ITEM: {
				return null;
			}
			case MSGTYPE_NOT_SET:
				return "不能发送空类型消息";
			default:
				return "发送消息类型未知";
		}
	}
	
	/**
	 * 检查用户id是否合法，返回不合法的id集合
	 */
	private Set<Long> checkUserById(RequestHead head, Collection<Long> userIds) {
		if (userIds.isEmpty()) {
			return Collections.emptySet();
		}
		
		Set<Long> userIdSet = new TreeSet<Long>(userIds);
		
		GetUserResponse response = Futures.getUnchecked(userService.getUserById(head, GetUserByIdRequest.newBuilder()
				.addAllUserId(userIdSet)
				.build()));
		for (UserProtos.User user : response.getUserList()) {
			userIdSet.remove(user.getBase().getUserId());
		}
		
		return userIdSet;
	}

}
