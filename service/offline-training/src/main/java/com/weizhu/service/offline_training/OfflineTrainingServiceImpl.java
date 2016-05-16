package com.weizhu.service.offline_training;

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

import com.google.common.collect.ImmutableSet;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.inject.Inject;
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import com.weizhu.common.db.DBUtil;
import com.weizhu.proto.AllowService;
import com.weizhu.proto.OfflineTrainingProtos;
import com.weizhu.proto.OfflineTrainingService;
import com.weizhu.proto.AllowProtos.CheckAllowRequest;
import com.weizhu.proto.AllowProtos.CheckAllowResponse;
import com.weizhu.proto.OfflineTrainingProtos.ApplyTrainRequest;
import com.weizhu.proto.OfflineTrainingProtos.ApplyTrainResponse;
import com.weizhu.proto.OfflineTrainingProtos.CheckInTrainRequest;
import com.weizhu.proto.OfflineTrainingProtos.CheckInTrainResponse;
import com.weizhu.proto.OfflineTrainingProtos.GetClosedTrainListRequest;
import com.weizhu.proto.OfflineTrainingProtos.GetClosedTrainListResponse;
import com.weizhu.proto.OfflineTrainingProtos.GetOpenTrainCountResponse;
import com.weizhu.proto.OfflineTrainingProtos.GetOpenTrainListRequest;
import com.weizhu.proto.OfflineTrainingProtos.GetOpenTrainListResponse;
import com.weizhu.proto.OfflineTrainingProtos.GetTrainByIdRequest;
import com.weizhu.proto.OfflineTrainingProtos.GetTrainByIdResponse;
import com.weizhu.proto.OfflineTrainingProtos.LeaveTrainRequest;
import com.weizhu.proto.OfflineTrainingProtos.LeaveTrainResponse;
import com.weizhu.proto.WeizhuProtos.EmptyRequest;
import com.weizhu.proto.WeizhuProtos.RequestHead;
import com.zaxxer.hikari.HikariDataSource;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

public class OfflineTrainingServiceImpl implements OfflineTrainingService {

	@SuppressWarnings("unused")
	private static final Logger logger = LoggerFactory.getLogger(AdminOfflineTrainingServiceImpl.class);

	private static final ImmutableSet<OfflineTrainingProtos.State> USER_STATE_SET = ImmutableSet.of(OfflineTrainingProtos.State.NORMAL);
	
	private final HikariDataSource hikariDataSource;
	private final JedisPool jedisPool;
	
	private final AllowService allowService;
	
	@Inject
	public OfflineTrainingServiceImpl(HikariDataSource hikariDataSource, JedisPool jedisPool, AllowService allowService) {
		this.hikariDataSource = hikariDataSource;
		this.jedisPool = jedisPool;
		this.allowService = allowService;
	}
	
	private Set<Integer> doCheckAllow(RequestHead head, Set<Integer> allowModelIdSet) {
		if (allowModelIdSet.isEmpty()) {
			return allowModelIdSet;
		}
		
		CheckAllowRequest request = CheckAllowRequest.newBuilder()
				.addAllModelId(allowModelIdSet)
				.addUserId(head.getSession().getUserId())
				.build();
		
		CheckAllowResponse response = Futures.getUnchecked(this.allowService.checkAllow(head, request));
		
		Set<Integer> checkedModelIdSet = new TreeSet<Integer>();
		for (CheckAllowResponse.CheckResult res : response.getCheckResultList()) {
			if (allowModelIdSet.contains(res.getModelId()) && res.getAllowUserIdList().contains(head.getSession().getUserId())) {
				checkedModelIdSet.add(res.getModelId());
			}
		}
		return checkedModelIdSet;
	}
	
	private Map<Integer, OfflineTrainingProtos.Train> doCheckAllowTrain(RequestHead head, Map<Integer, OfflineTrainingProtos.Train> trainMap) {
		if (trainMap.isEmpty()) {
			return trainMap;
		}
		
		Set<Integer> allowModelIdSet = new TreeSet<Integer>();
		for (OfflineTrainingProtos.Train train : trainMap.values()) {
			if (train.hasAllowModelId()) {
				allowModelIdSet.add(train.getAllowModelId());
			}
		}
		if (allowModelIdSet.isEmpty()) {
			return trainMap;
		}
		
		Set<Integer> checkedModelIdSet = this.doCheckAllow(head, allowModelIdSet);
		if (checkedModelIdSet.equals(allowModelIdSet)) {
			return trainMap;
		}
		
		Map<Integer, OfflineTrainingProtos.Train> checkedTrainMap = new TreeMap<Integer, OfflineTrainingProtos.Train>();
		for (OfflineTrainingProtos.Train train : trainMap.values()) {
			if (train.hasAllowModelId() && checkedModelIdSet.contains(train.getAllowModelId())) {
				checkedTrainMap.put(train.getTrainId(), train);
			}
		}
		return checkedTrainMap;
	}

	@Override
	public ListenableFuture<GetOpenTrainListResponse> getOpenTrainList(RequestHead head, GetOpenTrainListRequest request) {
		final long companyId = head.getSession().getCompanyId();
		final int size = request.getSize() < 0 ? 0 : request.getSize() > 100 ? 100 : request.getSize();
		final OfflineTrainingDAOProtos.TrainIndex offsetIndex;
		if (request.hasOffsetIndex() && !request.getOffsetIndex().isEmpty()) {
			OfflineTrainingDAOProtos.TrainIndex tmp = null;
			try {
				tmp = OfflineTrainingDAOProtos.TrainIndex.parseFrom(request.getOffsetIndex());
			} catch (InvalidProtocolBufferException e) {
				tmp = null;
			}
			offsetIndex = tmp;
		} else {
			offsetIndex = null;
		}
		
		final int now = (int) (System.currentTimeMillis() / 1000L);
		
		OfflineTrainingDAOProtos.TrainIndexList firstPageIndexList;
		Jedis jedis = this.jedisPool.getResource();
		try {
			firstPageIndexList = OfflineTrainingCache.getOpenTrainIndexList(jedis, Collections.singleton(companyId)).get(companyId);
			if (firstPageIndexList != null && firstPageIndexList.hasExpiredTime() && now >= firstPageIndexList.getExpiredTime()) {
				firstPageIndexList = null;
			}
		} finally {
			jedis.close();
		}
		
		if (firstPageIndexList == null) {
			Connection dbConn = null;
			try {
				dbConn = this.hikariDataSource.getConnection();
				firstPageIndexList = OfflineTrainingDB.getOpenTrainIndexList(dbConn, companyId, now, 100, null, USER_STATE_SET);
			} catch (SQLException e) {
				throw new RuntimeException("db fail", e);
			} finally {
				DBUtil.closeQuietly(dbConn);
			}
			
			jedis = this.jedisPool.getResource();
			try {
				OfflineTrainingCache.setOpenTrainIndexList(jedis, Collections.singletonMap(companyId, firstPageIndexList));
			} finally {
				jedis.close();
			}
		}
		
		List<OfflineTrainingDAOProtos.TrainIndex> idxList = new ArrayList<OfflineTrainingDAOProtos.TrainIndex>();
		for (OfflineTrainingDAOProtos.TrainIndex idx : firstPageIndexList.getTrainIndexList()) {
			if (offsetIndex != null && 
					(idx.getStartTime() < offsetIndex.getStartTime() || 
							(idx.getStartTime() == offsetIndex.getStartTime() && idx.getTrainId() <= offsetIndex.getTrainId()))) {
				continue;
			}
			
			idxList.add(idx);
			if (idxList.size() > size) {
				break;
			}
		}
		
		if (idxList.size() < size + 1 && firstPageIndexList.getTrainIndexCount() >= 100) {
			Connection dbConn = null;
			try {
				dbConn = this.hikariDataSource.getConnection();
				idxList = OfflineTrainingDB.getOpenTrainIndexList(dbConn, companyId, now, size + 1, offsetIndex, USER_STATE_SET).getTrainIndexList();
			} catch (SQLException e) {
				throw new RuntimeException("db fail", e);
			} finally {
				DBUtil.closeQuietly(dbConn);
			}
		}
		
		final boolean hasMore;
		if (idxList.size() > size) {
			hasMore = true;
			idxList = idxList.subList(0, size);
		} else {
			hasMore = false;
		}
		
		List<Integer> trainIdList = new ArrayList<Integer>(idxList.size());
		for (OfflineTrainingDAOProtos.TrainIndex idx : idxList) {
			trainIdList.add(idx.getTrainId());
		}
		
		Map<Integer, OfflineTrainingProtos.Train> refTrainMap = this.doCheckAllowTrain(head, 
				OfflineTrainingUtil.getTrain(this.hikariDataSource, this.jedisPool, companyId, trainIdList, USER_STATE_SET));
		Map<Integer, OfflineTrainingProtos.TrainCount> refTrainCountMap = OfflineTrainingUtil.getTrainCount(this.hikariDataSource, this.jedisPool, companyId, refTrainMap.keySet());
		
		Map<Integer, OfflineTrainingProtos.TrainUser> refTrainUserMap;
		Connection dbConn = null;
		try {
			dbConn = this.hikariDataSource.getConnection();
			refTrainUserMap = OfflineTrainingDB.getTrainUser(dbConn, companyId, head.getSession().getUserId(), refTrainMap.keySet());
		} catch (SQLException e) {
			throw new RuntimeException("db fail", e);
		} finally {
			DBUtil.closeQuietly(dbConn);
		}
		
		GetOpenTrainListResponse.Builder responseBuilder = GetOpenTrainListResponse.newBuilder();
		for (Integer trainId : trainIdList) {
			OfflineTrainingProtos.Train train = refTrainMap.get(trainId);
			if (train != null) {
				responseBuilder.addTrain(train);
			}
		}
		responseBuilder.setHasMore(hasMore);
		// 设置翻页索引
		if (idxList.isEmpty()) {
			// 获取到的索引列表为空，设置为传入参数
			responseBuilder.setOffsetIndex(request.hasOffsetIndex() ? request.getOffsetIndex() : ByteString.EMPTY);
		} else {
			responseBuilder.setOffsetIndex(idxList.get(idxList.size() - 1).toByteString());
		}
		
		responseBuilder.addAllRefTrainCount(refTrainCountMap.values());
		responseBuilder.addAllRefTrainUser(refTrainUserMap.values());
		return Futures.immediateFuture(responseBuilder.build());
	}

	@Override
	public ListenableFuture<GetOpenTrainCountResponse> getOpenTrainCount(RequestHead head, EmptyRequest request) {
		final long companyId = head.getSession().getCompanyId();
		final int now = (int) (System.currentTimeMillis() / 1000L);
		
		OfflineTrainingDAOProtos.TrainIndexList firstPageIndexList;
		Jedis jedis = this.jedisPool.getResource();
		try {
			firstPageIndexList = OfflineTrainingCache.getOpenTrainIndexList(jedis, Collections.singleton(companyId)).get(companyId);
			if (firstPageIndexList.hasExpiredTime() && now >= firstPageIndexList.getExpiredTime()) {
				firstPageIndexList = null;
			}
		} finally {
			jedis.close();
		}
		
		if (firstPageIndexList == null) {
			Connection dbConn = null;
			try {
				dbConn = this.hikariDataSource.getConnection();
				firstPageIndexList = OfflineTrainingDB.getOpenTrainIndexList(dbConn, companyId, now, 100, null, USER_STATE_SET);
			} catch (SQLException e) {
				throw new RuntimeException("db fail", e);
			} finally {
				DBUtil.closeQuietly(dbConn);
			}
			
			jedis = this.jedisPool.getResource();
			try {
				OfflineTrainingCache.setOpenTrainIndexList(jedis, Collections.singletonMap(companyId, firstPageIndexList));
			} finally {
				jedis.close();
			}
		}
		
		List<Integer> trainIdList = new ArrayList<Integer>(firstPageIndexList.getTrainIndexCount());
		for (OfflineTrainingDAOProtos.TrainIndex idx : firstPageIndexList.getTrainIndexList()) {
			trainIdList.add(idx.getTrainId());
		}
		Map<Integer, OfflineTrainingProtos.Train> refTrainMap = this.doCheckAllowTrain(head, 
				OfflineTrainingUtil.getTrain(this.hikariDataSource, this.jedisPool, companyId, trainIdList, USER_STATE_SET));
		
		return Futures.immediateFuture(GetOpenTrainCountResponse.newBuilder()
				.setOpenTrainCount(refTrainMap.size())
				.build());
	}

	@Override
	public ListenableFuture<GetClosedTrainListResponse> getClosedTrainList(RequestHead head, GetClosedTrainListRequest request) {
		final long companyId = head.getSession().getCompanyId();
		final int size = request.getSize() < 0 ? 0 : request.getSize() > 100 ? 100 : request.getSize();
		final OfflineTrainingDAOProtos.TrainIndex offsetIndex;
		if (request.hasOffsetIndex() && !request.getOffsetIndex().isEmpty()) {
			OfflineTrainingDAOProtos.TrainIndex tmp = null;
			try {
				tmp = OfflineTrainingDAOProtos.TrainIndex.parseFrom(request.getOffsetIndex());
			} catch (InvalidProtocolBufferException e) {
				tmp = null;
			}
			offsetIndex = tmp;
		} else {
			offsetIndex = null;
		}
		
		final int now = (int) (System.currentTimeMillis() / 1000L);
		
		OfflineTrainingDAOProtos.TrainIndexList firstPageIndexList;
		Jedis jedis = this.jedisPool.getResource();
		try {
			firstPageIndexList = OfflineTrainingCache.getClosedTrainIndexList(jedis, Collections.singleton(companyId)).get(companyId);
			if (firstPageIndexList != null && firstPageIndexList.hasExpiredTime() && now >= firstPageIndexList.getExpiredTime()) {
				firstPageIndexList = null;
			}
		} finally {
			jedis.close();
		}
		
		if (firstPageIndexList == null) {
			Connection dbConn = null;
			try {
				dbConn = this.hikariDataSource.getConnection();
				firstPageIndexList = OfflineTrainingDB.getClosedTrainIndexList(dbConn, companyId, now, 100, null, USER_STATE_SET);
			} catch (SQLException e) {
				throw new RuntimeException("db fail", e);
			} finally {
				DBUtil.closeQuietly(dbConn);
			}
			
			jedis = this.jedisPool.getResource();
			try {
				OfflineTrainingCache.setClosedTrainIndexList(jedis, Collections.singletonMap(companyId, firstPageIndexList));
			} finally {
				jedis.close();
			}
		}
		
		List<OfflineTrainingDAOProtos.TrainIndex> idxList = new ArrayList<OfflineTrainingDAOProtos.TrainIndex>(size + 1);
		
		for (OfflineTrainingDAOProtos.TrainIndex idx : firstPageIndexList.getTrainIndexList()) {
			if (offsetIndex != null && 
					(idx.getEndTime() > offsetIndex.getEndTime() || 
							(idx.getEndTime() == offsetIndex.getEndTime() && idx.getTrainId() >= offsetIndex.getTrainId()))) {
				continue;
			}
			
			idxList.add(idx);
			if (idxList.size() > size) {
				break;
			}
		}
		
		if (idxList.size() < size + 1 && firstPageIndexList.getTrainIndexCount() >= 100) {
			Connection dbConn = null;
			try {
				dbConn = this.hikariDataSource.getConnection();
				idxList = OfflineTrainingDB.getClosedTrainIndexList(dbConn, companyId, now, size + 1, offsetIndex, USER_STATE_SET).getTrainIndexList();
			} catch (SQLException e) {
				throw new RuntimeException("db fail", e);
			} finally {
				DBUtil.closeQuietly(dbConn);
			}
		}
		
		final boolean hasMore;
		if (idxList.size() > size) {
			hasMore = true;
			idxList = idxList.subList(0, size);
		} else {
			hasMore = false;
		}
		
		List<Integer> trainIdList = new ArrayList<Integer>(idxList.size());
		for (OfflineTrainingDAOProtos.TrainIndex idx : idxList) {
			trainIdList.add(idx.getTrainId());
		}
		
		Map<Integer, OfflineTrainingProtos.Train> refTrainMap = this.doCheckAllowTrain(head, 
				OfflineTrainingUtil.getTrain(this.hikariDataSource, this.jedisPool, companyId, trainIdList, USER_STATE_SET));
		Map<Integer, OfflineTrainingProtos.TrainCount> refTrainCountMap = OfflineTrainingUtil.getTrainCount(this.hikariDataSource, this.jedisPool, companyId, refTrainMap.keySet());
		
		Map<Integer, OfflineTrainingProtos.TrainUser> refTrainUserMap;
		Connection dbConn = null;
		try {
			dbConn = this.hikariDataSource.getConnection();
			refTrainUserMap = OfflineTrainingDB.getTrainUser(dbConn, companyId, head.getSession().getUserId(), refTrainMap.keySet());
		} catch (SQLException e) {
			throw new RuntimeException("db fail", e);
		} finally {
			DBUtil.closeQuietly(dbConn);
		}
		
		GetClosedTrainListResponse.Builder responseBuilder = GetClosedTrainListResponse.newBuilder();
		for (Integer trainId : trainIdList) {
			OfflineTrainingProtos.Train train = refTrainMap.get(trainId);
			if (train != null) {
				responseBuilder.addTrain(train);
			}
		}
		responseBuilder.setHasMore(hasMore);
		// 设置翻页索引
		if (idxList.isEmpty()) {
			// 获取到的索引列表为空，设置为传入参数
			responseBuilder.setOffsetIndex(request.hasOffsetIndex() ? request.getOffsetIndex() : ByteString.EMPTY);
		} else {
			responseBuilder.setOffsetIndex(idxList.get(idxList.size() - 1).toByteString());
		}
		
		responseBuilder.addAllRefTrainCount(refTrainCountMap.values());
		responseBuilder.addAllRefTrainUser(refTrainUserMap.values());
		return Futures.immediateFuture(responseBuilder.build());
	}

	@Override
	public ListenableFuture<GetTrainByIdResponse> getTrainById(RequestHead head, GetTrainByIdRequest request) {
		final long companyId = head.getSession().getCompanyId();
		
		if (request.getTrainIdCount() <= 0) {
			return Futures.immediateFuture(GetTrainByIdResponse.getDefaultInstance());
		}
		final Set<Integer> trainIdSet = new TreeSet<Integer>(request.getTrainIdList());
		
		Map<Integer, OfflineTrainingProtos.Train> trainMap = this.doCheckAllowTrain(head, 
				OfflineTrainingUtil.getTrain(this.hikariDataSource, this.jedisPool, companyId, trainIdSet, USER_STATE_SET));
		Map<Integer, OfflineTrainingProtos.TrainCount> trainCountMap = OfflineTrainingUtil.getTrainCount(this.hikariDataSource, this.jedisPool, companyId, trainMap.keySet());
		
		Map<Integer, OfflineTrainingProtos.TrainUser> refTrainUserMap;
		Connection dbConn = null;
		try {
			dbConn = this.hikariDataSource.getConnection();
			refTrainUserMap = OfflineTrainingDB.getTrainUser(dbConn, companyId, head.getSession().getUserId(), trainMap.keySet());
		} catch (SQLException e) {
			throw new RuntimeException("db fail", e);
		} finally {
			DBUtil.closeQuietly(dbConn);
		}
		
		return Futures.immediateFuture(GetTrainByIdResponse.newBuilder()
				.addAllTrain(trainMap.values())
				.addAllRefTrainCount(trainCountMap.values())
				.addAllRefTrainUser(refTrainUserMap.values())
				.build());
	}

	@Override
	public ListenableFuture<ApplyTrainResponse> applyTrain(RequestHead head, ApplyTrainRequest request) {
		final long companyId = head.getSession().getCompanyId();
		final int trainId = request.getTrainId();
		final boolean isCancel = request.getIsCancel();
		
		final OfflineTrainingProtos.Train train = this.doCheckAllowTrain(head, 
				OfflineTrainingUtil.getTrain(this.hikariDataSource, this.jedisPool, companyId, 
				Collections.singleton(trainId), USER_STATE_SET)).get(trainId);
		if (train == null) {
			return Futures.immediateFuture(ApplyTrainResponse.newBuilder()
					.setResult(ApplyTrainResponse.Result.FAIL_UNKNOWN)
					.setFailText("该培训不存在或者已经被删除")
					.build());
		}
		
		if (!train.getApplyEnable()) {
			return Futures.immediateFuture(ApplyTrainResponse.newBuilder()
					.setResult(ApplyTrainResponse.Result.FAIL_UNKNOWN)
					.setFailText("该培训不需要报名")
					.build());
		}
		
		final int now = (int) (System.currentTimeMillis() / 1000L);
		if (now < train.getApplyStartTime()) {
			return Futures.immediateFuture(ApplyTrainResponse.newBuilder()
					.setResult(ApplyTrainResponse.Result.FAIL_UNKNOWN)
					.setFailText("报名还未开始")
					.build());
		}
		if (now >= train.getApplyEndTime()) {
			return Futures.immediateFuture(ApplyTrainResponse.newBuilder()
					.setResult(ApplyTrainResponse.Result.FAIL_UNKNOWN)
					.setFailText("报名已经结束")
					.build());
		}
		
		final OfflineTrainingProtos.TrainUser trainUser;
		Connection dbConn = null;
		try {
			dbConn = this.hikariDataSource.getConnection();
			trainUser = OfflineTrainingDB.getTrainUser(dbConn, companyId, head.getSession().getUserId(), Collections.singleton(trainId)).get(trainId);
		} catch (SQLException e) {
			throw new RuntimeException("db fail", e);
		} finally {
			DBUtil.closeQuietly(dbConn);
		}
		
		if (!isCancel) {
			if (trainUser != null && trainUser.getIsApply()) {
				// 已报名成功
				return Futures.immediateFuture(ApplyTrainResponse.newBuilder()
						.setResult(ApplyTrainResponse.Result.FAIL_UNKNOWN)
						.setFailText("您已报名成功,不需要再次报名")
						.build());
			}
			
			final OfflineTrainingProtos.TrainCount trainCount = OfflineTrainingUtil.getTrainCount(this.hikariDataSource, this.jedisPool, companyId, 
					Collections.singleton(trainId)).get(trainId);
			if (trainCount != null && trainCount.getUserApplyCount() >= train.getApplyUserCount()) {
				return Futures.immediateFuture(ApplyTrainResponse.newBuilder()
						.setResult(ApplyTrainResponse.Result.FAIL_UNKNOWN)
						.setFailText("报名人数已满")
						.build());
			}
		} else {
			if (trainUser == null || !trainUser.getIsApply()) {
				// 未报名状态, 不需要再取消了
				return Futures.immediateFuture(ApplyTrainResponse.newBuilder()
						.setResult(ApplyTrainResponse.Result.SUCC)
						.build());
			}
			
			if (trainUser.getIsCheckIn()) {
				// 已签到用户，不能再取消报名
				return Futures.immediateFuture(ApplyTrainResponse.newBuilder()
						.setResult(ApplyTrainResponse.Result.FAIL_UNKNOWN)
						.setFailText("您已签到成功，无法再取消报名")
						.build());
			}
		}
		
		Map<Integer, OfflineTrainingProtos.TrainCount> trainCountMap;
		dbConn = null;
		try {
			dbConn = this.hikariDataSource.getConnection();
			OfflineTrainingDB.updateTrainUserApply(dbConn, companyId, trainId, head.getSession().getUserId(), !isCancel, now, now);
			trainCountMap = OfflineTrainingDB.getTrainCount(dbConn, companyId, Collections.singleton(trainId));
		} catch (SQLException e) {
			throw new RuntimeException("db fail", e);
		} finally {
			DBUtil.closeQuietly(dbConn);
		}
		
		Jedis jedis = this.jedisPool.getResource();
		try {
			OfflineTrainingCache.setTrainCount(jedis, companyId, trainCountMap);
		} finally {
			jedis.close();
		}
		
		return Futures.immediateFuture(ApplyTrainResponse.newBuilder()
				.setResult(ApplyTrainResponse.Result.SUCC)
				.build());
	}

	@Override
	public ListenableFuture<CheckInTrainResponse> checkInTrain(RequestHead head, CheckInTrainRequest request) {
		final long companyId = head.getSession().getCompanyId();
		final int trainId = request.getTrainId();
		
		final OfflineTrainingProtos.Train train = this.doCheckAllowTrain(head, 
				OfflineTrainingUtil.getTrain(this.hikariDataSource, this.jedisPool, companyId, 
				Collections.singleton(trainId), USER_STATE_SET)).get(trainId);
		if (train == null) {
			return Futures.immediateFuture(CheckInTrainResponse.newBuilder()
					.setResult(CheckInTrainResponse.Result.FAIL_UNKNOWN)
					.setFailText("该培训不存在或者已经被删除")
					.build());
		}
		
		final int now = (int) (System.currentTimeMillis() / 1000L);
		if (now < train.getCheckInStartTime()) {
			return Futures.immediateFuture(CheckInTrainResponse.newBuilder()
					.setResult(CheckInTrainResponse.Result.FAIL_UNKNOWN)
					.setFailText("签到还未开始")
					.build());
		}
		if (now >= train.getCheckInEndTime()) {
			return Futures.immediateFuture(CheckInTrainResponse.newBuilder()
					.setResult(CheckInTrainResponse.Result.FAIL_UNKNOWN)
					.setFailText("签到已经结束")
					.build());
		}
		
		final OfflineTrainingProtos.TrainUser trainUser;
		Connection dbConn = null;
		try {
			dbConn = this.hikariDataSource.getConnection();
			trainUser = OfflineTrainingDB.getTrainUser(dbConn, companyId, head.getSession().getUserId(), Collections.singleton(trainId)).get(trainId);
		} catch (SQLException e) {
			throw new RuntimeException("db fail", e);
		} finally {
			DBUtil.closeQuietly(dbConn);
		}
		
		if (trainUser != null && trainUser.getIsCheckIn()) {
			return Futures.immediateFuture(CheckInTrainResponse.newBuilder()
					.setResult(CheckInTrainResponse.Result.SUCC)
					.build());
		}
		
		if (train.getApplyEnable() && (trainUser == null || !trainUser.getIsApply())) {
			return Futures.immediateFuture(CheckInTrainResponse.newBuilder()
					.setResult(CheckInTrainResponse.Result.FAIL_UNKNOWN)
					.setFailText("您未报名本次培训，无法签到")
					.build());
		}
		
		Map<Integer, OfflineTrainingProtos.TrainCount> trainCountMap;
		dbConn = null;
		try {
			dbConn = this.hikariDataSource.getConnection();
			OfflineTrainingDB.updateTrainUserCheckIn(dbConn, companyId, trainId, head.getSession().getUserId(), true, now, now);
			trainCountMap = OfflineTrainingDB.getTrainCount(dbConn, companyId, Collections.singleton(trainId));
		} catch (SQLException e) {
			throw new RuntimeException("db fail", e);
		} finally {
			DBUtil.closeQuietly(dbConn);
		}
		
		Jedis jedis = this.jedisPool.getResource();
		try {
			OfflineTrainingCache.setTrainCount(jedis, companyId, trainCountMap);
		} finally {
			jedis.close();
		}
		
		return Futures.immediateFuture(CheckInTrainResponse.newBuilder()
				.setResult(CheckInTrainResponse.Result.SUCC)
				.build());
	}

	@Override
	public ListenableFuture<LeaveTrainResponse> leaveTrain(RequestHead head, LeaveTrainRequest request) {
		final long companyId = head.getSession().getCompanyId();
		final int trainId = request.getTrainId();
		final boolean isCancel = request.getIsCancel();
		final String leaveReason = request.hasLeaveReason() ? request.getLeaveReason().trim() : null;
		
		final OfflineTrainingProtos.Train train = this.doCheckAllowTrain(head, 
				OfflineTrainingUtil.getTrain(this.hikariDataSource, this.jedisPool, companyId, 
				Collections.singleton(trainId), USER_STATE_SET)).get(trainId);
		if (train == null) {
			return Futures.immediateFuture(LeaveTrainResponse.newBuilder()
					.setResult(LeaveTrainResponse.Result.FAIL_UNKNOWN)
					.setFailText("该培训不存在或者已经被删除")
					.build());
		}
		
		final int now = (int) (System.currentTimeMillis() / 1000L);
		if (now >= train.getEndTime()) {
			return Futures.immediateFuture(LeaveTrainResponse.newBuilder()
					.setResult(LeaveTrainResponse.Result.FAIL_UNKNOWN)
					.setFailText("培训已经结束，" + (isCancel ? "无法取消请假" : "无法请假"))
					.build());
		}
		
		final OfflineTrainingProtos.TrainUser trainUser;
		Connection dbConn = null;
		try {
			dbConn = this.hikariDataSource.getConnection();
			trainUser = OfflineTrainingDB.getTrainUser(dbConn, companyId, head.getSession().getUserId(), Collections.singleton(trainId)).get(trainId);
		} catch (SQLException e) {
			throw new RuntimeException("db fail", e);
		} finally {
			DBUtil.closeQuietly(dbConn);
		}
		
		if (!isCancel) {
			if (trainUser == null || (!(train.getApplyEnable() && trainUser.getIsApply()) && !trainUser.getIsCheckIn())) {
				// 没有参与本次培训，且未报名&签到，不需要请假
				return Futures.immediateFuture(LeaveTrainResponse.newBuilder()
						.setResult(LeaveTrainResponse.Result.FAIL_UNKNOWN)
						.setFailText("您没有参与本次培训, 无需请假")
						.build());
			}
		} else {
			if (trainUser == null || !trainUser.getIsLeave()) {
				// 没有参与本次培训, 未请过假，无需取消请假
				return Futures.immediateFuture(LeaveTrainResponse.newBuilder()
						.setResult(LeaveTrainResponse.Result.FAIL_UNKNOWN)
						.setFailText("本次培训您没有请假纪录, 无需取消请假")
						.build());
			}
		}
		
		Map<Integer, OfflineTrainingProtos.TrainCount> trainCountMap;
		dbConn = null;
		try {
			dbConn = this.hikariDataSource.getConnection();
			OfflineTrainingDB.updateTrainUserLeave(dbConn, companyId, trainId, head.getSession().getUserId(), !isCancel, now, leaveReason, now);
			trainCountMap = OfflineTrainingDB.getTrainCount(dbConn, companyId, Collections.singleton(trainId));
		} catch (SQLException e) {
			throw new RuntimeException("db fail", e);
		} finally {
			DBUtil.closeQuietly(dbConn);
		}
		
		Jedis jedis = this.jedisPool.getResource();
		try {
			OfflineTrainingCache.setTrainCount(jedis, companyId, trainCountMap);
		} finally {
			jedis.close();
		}
		
		return Futures.immediateFuture(LeaveTrainResponse.newBuilder()
				.setResult(LeaveTrainResponse.Result.SUCC)
				.build());
	}

}