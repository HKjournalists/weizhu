package com.weizhu.service.offline_training;

import java.sql.Connection;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.weizhu.proto.AdminOfflineTrainingService;
import com.weizhu.proto.AllowProtos;
import com.weizhu.common.db.DBUtil;
import com.weizhu.common.utils.DataPage;
import com.weizhu.common.utils.ProfileManager;
import com.weizhu.common.utils.TaskManager;
import com.weizhu.proto.AdminOfficialService;
import com.weizhu.proto.AdminOfflineTrainingProtos.CreateTrainRequest;
import com.weizhu.proto.AdminOfflineTrainingProtos.CreateTrainResponse;
import com.weizhu.proto.AdminOfflineTrainingProtos.GetTrainByIdRequest;
import com.weizhu.proto.AdminOfflineTrainingProtos.GetTrainByIdResponse;
import com.weizhu.proto.AdminOfflineTrainingProtos.GetTrainListRequest;
import com.weizhu.proto.AdminOfflineTrainingProtos.GetTrainListResponse;
import com.weizhu.proto.AdminOfflineTrainingProtos.GetTrainUserListRequest;
import com.weizhu.proto.AdminOfflineTrainingProtos.GetTrainUserListResponse;
import com.weizhu.proto.AdminOfflineTrainingProtos.UpdateTrainDiscoverItemRequest;
import com.weizhu.proto.AdminOfflineTrainingProtos.UpdateTrainDiscoverItemResponse;
import com.weizhu.proto.AdminOfflineTrainingProtos.UpdateTrainRequest;
import com.weizhu.proto.AdminOfflineTrainingProtos.UpdateTrainResponse;
import com.weizhu.proto.AdminOfflineTrainingProtos.UpdateTrainStateRequest;
import com.weizhu.proto.AdminOfflineTrainingProtos.UpdateTrainStateResponse;
import com.weizhu.proto.AdminProtos.AdminHead;
import com.weizhu.proto.AdminUserProtos.GetUserListRequest;
import com.weizhu.proto.AdminUserProtos.GetUserListResponse;
import com.weizhu.proto.AdminUserService;
import com.weizhu.proto.AllowProtos.CheckAllowRequest;
import com.weizhu.proto.AllowProtos.CheckAllowResponse;
import com.weizhu.proto.AllowService;
import com.weizhu.proto.OfficialProtos;
import com.weizhu.proto.OfflineTrainingProtos;
import com.weizhu.proto.UserProtos;
import com.weizhu.proto.WeizhuProtos.EmptyRequest;
import com.weizhu.proto.WeizhuProtos.SystemHead;
import com.weizhu.proto.AdminOfficialProtos.SendSecretaryMessageRequest;
import com.zaxxer.hikari.HikariDataSource;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

@Singleton
public class AdminOfflineTrainingServiceImpl implements AdminOfflineTrainingService {

	private static final Logger logger = LoggerFactory.getLogger(AdminOfflineTrainingServiceImpl.class);

	private static final ImmutableSet<OfflineTrainingProtos.State> ADMIN_STATE_SET = 
			ImmutableSet.of(OfflineTrainingProtos.State.NORMAL, OfflineTrainingProtos.State.DISABLE);
	
	private final HikariDataSource hikariDataSource;
	private final JedisPool jedisPool;
	private final Executor serviceExecutor;
	private final AdminUserService adminUserService;
	private final AllowService allowService;
	private final AdminOfficialService adminOfficialService;
	private final ProfileManager profileManager;
	
	private final TaskManager.TaskHandler<OfflineTrainingDAOProtos.TrainApplyNotifyData> trainApplyNotifyTaskHandler;
	
	@Inject
	public AdminOfflineTrainingServiceImpl(
			HikariDataSource hikariDataSource, 
			JedisPool jedisPool,
			@Named("service_executor") Executor serviceExecutor,
			AdminUserService adminUserService,
			AllowService allowService,
			AdminOfficialService adminOfficialService,
			ProfileManager profileManager,
			TaskManager taskManager
			) {
		this.hikariDataSource = hikariDataSource;
		this.jedisPool = jedisPool;
		this.serviceExecutor = serviceExecutor;
		this.adminUserService = adminUserService;
		this.allowService = allowService;
		this.adminOfficialService = adminOfficialService;
		this.profileManager = profileManager;
		
		TaskManager.TaskHandler<EmptyRequest> tomorrowTrainNotifyTaskHandler = 
				taskManager.register("offline_training:tomorrow_train_notify", EmptyRequest.PARSER, new TomorrowTrainNotifyTask());
		try {
			DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
			int startTime = (int) (df.parse("2016-01-01 18:00:00") .getTime() / 1000L);
			tomorrowTrainNotifyTaskHandler.schedulePeriod("offline_training:tomorrow_train_notify", null, startTime, 24 * 60 * 60);
		} catch (ParseException e) {
			throw new RuntimeException(e);
		}
		
		this.trainApplyNotifyTaskHandler = taskManager.register("offline_training:train_apply_notify", 
				OfflineTrainingDAOProtos.TrainApplyNotifyData.PARSER, new TrainApplyNotifyTask());
		
		Map<Long, List<Integer>> notApplyNotifyTrainIdMap;
		Connection dbConn = null;
		try {
			dbConn = this.hikariDataSource.getConnection();
			notApplyNotifyTrainIdMap = OfflineTrainingDB.getTrainIdListByNotApplyNotify(dbConn, Collections.singleton(OfflineTrainingProtos.State.NORMAL));
		} catch (SQLException e) {
			throw new RuntimeException("db fail", e);
		} finally {
			DBUtil.closeQuietly(dbConn);
		}
		
		for (Entry<Long, List<Integer>> entry : notApplyNotifyTrainIdMap.entrySet()) {
			final long companyId = entry.getKey();
			Map<Integer, OfflineTrainingProtos.Train> trainMap = OfflineTrainingUtil.getTrain(hikariDataSource, jedisPool, companyId, entry.getValue(), Collections.singleton(OfflineTrainingProtos.State.NORMAL));	
			int cnt = 0;
			for (int trainId : entry.getValue()) {
				OfflineTrainingProtos.Train train = trainMap.get(trainId);
				if (train != null && train.getApplyEnable() && !train.getApplyIsNotify()) {
					this.trainApplyNotifyTaskHandler.schedule(companyId + ":" + trainId, 
							OfflineTrainingDAOProtos.TrainApplyNotifyData.newBuilder()
							.setCompanyId(companyId)
							.setTrainId(trainId)
							.setApplyStartTime(train.getApplyStartTime())
							.build(), train.getApplyStartTime());
					cnt++;
				}
			}
			
			logger.info("load train apply notify task " + companyId + ", " + cnt);
		}
	}
	
	@Override
	public ListenableFuture<GetTrainByIdResponse> getTrainById(AdminHead head, GetTrainByIdRequest request) {
		final long companyId = head.getCompanyId();
		
		if (request.getTrainIdCount() <= 0) {
			return Futures.immediateFuture(GetTrainByIdResponse.getDefaultInstance());
		}
		final Set<Integer> trainIdSet = new TreeSet<Integer>(request.getTrainIdList());
		
		Map<Integer, OfflineTrainingProtos.Train> trainMap = OfflineTrainingUtil.getTrain(this.hikariDataSource, this.jedisPool, companyId, trainIdSet, ADMIN_STATE_SET);
		Map<Integer, OfflineTrainingProtos.TrainCount> trainCountMap = OfflineTrainingUtil.getTrainCount(this.hikariDataSource, this.jedisPool, companyId, trainMap.keySet());
		
		return Futures.immediateFuture(GetTrainByIdResponse.newBuilder()
				.addAllTrain(trainMap.values())
				.addAllRefTrainCount(trainCountMap.values())
				.build());
	}

	@Override
	public ListenableFuture<GetTrainListResponse> getTrainList(AdminHead head, GetTrainListRequest request) {
		final long companyId = head.getCompanyId();
		final int start = request.getStart() < 0 ? 0 : request.getStart();
		final int length = request.getLength() < 0 ? 0 : request.getLength() > 100 ? 100 : request.getLength();
		
		final Integer startTime = request.hasStartTime() ? request.getStartTime() : null;
		final Integer endTime = request.hasEndTime() ? request.getEndTime() : null;
		final Long createAdminId = request.hasCreateAdminId() ? request.getCreateAdminId() : null;
		final OfflineTrainingProtos.State state = request.hasState() ? request.getState() : null;
		final String trainName = request.hasTrainName() && !request.getTrainName().trim().isEmpty() ? request.getTrainName().trim() : null;
		
		DataPage<Integer> trainIdPage;
		Connection dbConn = null;
		try {
			dbConn = this.hikariDataSource.getConnection();
			trainIdPage = OfflineTrainingDB.getTrainIdPage(dbConn, companyId, start, length, startTime, endTime, createAdminId, state, trainName, ADMIN_STATE_SET);
		} catch (SQLException e) {
			throw new RuntimeException("db fail", e);
		} finally {
			DBUtil.closeQuietly(dbConn);
		}
		
		Map<Integer, OfflineTrainingProtos.Train> trainMap = OfflineTrainingUtil.getTrain(this.hikariDataSource, this.jedisPool, companyId, trainIdPage.dataList(), ADMIN_STATE_SET);
		Map<Integer, OfflineTrainingProtos.TrainCount> trainCountMap = OfflineTrainingUtil.getTrainCount(this.hikariDataSource, this.jedisPool, companyId, trainMap.keySet());
		
		GetTrainListResponse.Builder responseBuilder = GetTrainListResponse.newBuilder();
		for (Integer trainId : trainIdPage.dataList()) {
			OfflineTrainingProtos.Train train = trainMap.get(trainId);
			if (train != null) {
				responseBuilder.addTrain(train);
			}
		}
		responseBuilder.setTotalSize(trainIdPage.totalSize());
		responseBuilder.setFilteredSize(trainIdPage.filteredSize());
		responseBuilder.addAllRefTrainCount(trainCountMap.values());
		
		return Futures.immediateFuture(responseBuilder.build());
	}

	@Override
	public ListenableFuture<CreateTrainResponse> createTrain(AdminHead head, CreateTrainRequest request) {
		final long companyId = head.getCompanyId();
		final String trainName = request.getTrainName().trim();
		if (trainName.isEmpty()) {
			return Futures.immediateFuture(CreateTrainResponse.newBuilder()
					.setResult(CreateTrainResponse.Result.FAIL_NAME_INVALID)
					.setFailText("培训名称不能为空")
					.build());
		} else if (trainName.length() > 50) {
			return Futures.immediateFuture(CreateTrainResponse.newBuilder()
					.setResult(CreateTrainResponse.Result.FAIL_NAME_INVALID)
					.setFailText("培训名称最多50个字")
					.build());
		}
		
		final String imageName = request.hasImageName() ? request.getImageName() : null;
		if (imageName != null && imageName.length() > 191) {
			return Futures.immediateFuture(CreateTrainResponse.newBuilder()
					.setResult(CreateTrainResponse.Result.FAIL_IMAGE_INVALID)
					.setFailText("图片名最多191个字")
					.build());
		}
		
		final int startTime = request.getStartTime();
		final int endTime = request.getEndTime();
		if (startTime >= endTime) {
			return Futures.immediateFuture(CreateTrainResponse.newBuilder()
					.setResult(CreateTrainResponse.Result.FAIL_TIME_INVLID)
					.setFailText("开始时间不能晚于结束时间")
					.build());
		}
		
		final boolean applyEnable = request.getApplyEnable();
		
		final Integer applyStartTime;
		final Integer applyEndTime;
		final Integer applyUserCount;
		if (applyEnable) {
			if (!request.hasApplyStartTime()) {
				return Futures.immediateFuture(CreateTrainResponse.newBuilder()
						.setResult(CreateTrainResponse.Result.FAIL_APPLY_INVALD)
						.setFailText("报名开始时间为空")
						.build());
			}
			applyStartTime = request.getApplyStartTime();
			if (!request.hasApplyEndTime()) {
				return Futures.immediateFuture(CreateTrainResponse.newBuilder()
						.setResult(CreateTrainResponse.Result.FAIL_APPLY_INVALD)
						.setFailText("报名结束时间为空")
						.build());
			}
			applyEndTime = request.getApplyEndTime();
			if (applyStartTime >= applyEndTime) {
				return Futures.immediateFuture(CreateTrainResponse.newBuilder()
						.setResult(CreateTrainResponse.Result.FAIL_TIME_INVLID)
						.setFailText("报名开始时间不能晚于报名结束时间")
						.build());
			}
			if (applyEndTime > endTime) {
				return Futures.immediateFuture(CreateTrainResponse.newBuilder()
						.setResult(CreateTrainResponse.Result.FAIL_TIME_INVLID)
						.setFailText("报名结束时间不能晚于培训结束时间")
						.build());
			}
			
			if (!request.hasApplyUserCount()) {
				return Futures.immediateFuture(CreateTrainResponse.newBuilder()
						.setResult(CreateTrainResponse.Result.FAIL_APPLY_INVALD)
						.setFailText("报名人数限制为空")
						.build());
			}
			if (request.getApplyUserCount() <= 0) {
				return Futures.immediateFuture(CreateTrainResponse.newBuilder()
						.setResult(CreateTrainResponse.Result.FAIL_APPLY_INVALD)
						.setFailText("报名人数限制必须大约0")
						.build());
			}
			applyUserCount = request.getApplyUserCount();
		} else {
			applyStartTime = null;
			applyEndTime = null;
			applyUserCount = null;
		}
		
		final String trainAddress = request.hasTrainAddress() ? request.getTrainAddress().trim() : null;
		if (trainAddress != null && trainAddress.length() > 191) {
			return Futures.immediateFuture(CreateTrainResponse.newBuilder()
					.setResult(CreateTrainResponse.Result.FAIL_ADDRESS_INVALID)
					.setFailText("培训地点最多为191个字")
					.build());
		}
		
		final String lecturerName = request.hasLecturerName() ? request.getLecturerName().trim() : null;
		if (lecturerName != null && lecturerName.length() > 191) {
			return Futures.immediateFuture(CreateTrainResponse.newBuilder()
					.setResult(CreateTrainResponse.Result.FAIL_LECTURER_INVALID)
					.setFailText("讲师名称最多为191个字")
					.build());
		}
		
		final Set<Long> lectureUserIdSet = new TreeSet<Long>(request.getLecturerUserIdList());
		if (lectureUserIdSet.size() > 100) {
			return Futures.immediateFuture(CreateTrainResponse.newBuilder()
					.setResult(CreateTrainResponse.Result.FAIL_LECTURER_INVALID)
					.setFailText("讲师最多为100个")
					.build());
		}
		
		final int checkInStartTime = request.getCheckInStartTime();
		final int checkInEndTime = request.getCheckInEndTime();
		if (checkInStartTime >= checkInEndTime) {
			return Futures.immediateFuture(CreateTrainResponse.newBuilder()
					.setResult(CreateTrainResponse.Result.FAIL_CHECK_IN_INVALID)
					.setFailText("签到开始时间不能晚于签到结束时间")
					.build());
		}
		if (checkInEndTime > endTime) {
			return Futures.immediateFuture(CreateTrainResponse.newBuilder()
					.setResult(CreateTrainResponse.Result.FAIL_CHECK_IN_INVALID)
					.setFailText("签到结束时间不能晚于培训结束时间")
					.build());
		}
		
		final String arrangementText = request.getArrangementText();
		if (arrangementText.length() > 65535) {
			return Futures.immediateFuture(CreateTrainResponse.newBuilder()
					.setResult(CreateTrainResponse.Result.FAIL_UNKNOWN)
					.setFailText("培训安排最多65535个字")
					.build());
		}
		
		final String describeText = request.hasDescribeText() ? request.getDescribeText() : null;
		if (describeText != null && describeText.length() > 65535) {
			return Futures.immediateFuture(CreateTrainResponse.newBuilder()
					.setResult(CreateTrainResponse.Result.FAIL_UNKNOWN)
					.setFailText("培训简介最多65535个字")
					.build());
		}
		
		final OfflineTrainingProtos.State state = request.hasState() ? request.getState() : OfflineTrainingProtos.State.NORMAL;
		if (state != OfflineTrainingProtos.State.NORMAL && state != OfflineTrainingProtos.State.DISABLE) {
			return Futures.immediateFuture(CreateTrainResponse.newBuilder()
					.setResult(CreateTrainResponse.Result.FAIL_UNKNOWN)
					.setFailText("培训状态错误，必须为启用或者禁用")
					.build());
		}
		
		final int now = (int) (System.currentTimeMillis() / 1000L);
		
		OfflineTrainingProtos.Train.Builder builder = OfflineTrainingProtos.Train.newBuilder();
		builder.setTrainId(-1);
		builder.setTrainName(trainName);
		if (imageName != null) {
			builder.setImageName(imageName);
		}
		builder.setStartTime(startTime);
		builder.setEndTime(endTime);
		builder.setApplyEnable(applyEnable);
		if (applyEnable) {
			builder.setApplyStartTime(applyStartTime);
			builder.setApplyEndTime(applyEndTime);
			builder.setApplyUserCount(applyUserCount);
			builder.setApplyIsNotify(false);
		}
		
		builder.setTrainAddress(trainAddress);
		if (lecturerName != null) {
			builder.setLecturerName(lecturerName);
		}
		builder.addAllLecturerUserId(lectureUserIdSet);
		builder.setCheckInStartTime(checkInStartTime);
		builder.setCheckInEndTime(checkInEndTime);
		builder.setArrangementText(arrangementText);
		if (describeText != null) {
			builder.setDescribeText(describeText);
		}
		if (request.hasAllowModelId()) {
			builder.setAllowModelId(request.getAllowModelId());
		}
		
		builder.setState(state);
		builder.setCreateAdminId(head.getSession().getAdminId());
		builder.setCreateTime(now);
		
		final OfflineTrainingProtos.Train newTrain = builder.build();
		final int newTrainId;
		Connection dbConn = null;
		try {
			dbConn = this.hikariDataSource.getConnection();
			newTrainId = OfflineTrainingDB.insertTrain(dbConn, companyId, newTrain);
		} catch (SQLException e) {
			throw new RuntimeException("db fail", e);
		} finally {
			DBUtil.closeQuietly(dbConn);
		}
		
		Jedis jedis = this.jedisPool.getResource();
		try {
			OfflineTrainingCache.delTrain(jedis, companyId, Collections.singleton(newTrainId));
			OfflineTrainingCache.delOpenTrainIndexList(jedis, Collections.singleton(companyId));
			OfflineTrainingCache.delClosedTrainIndexList(jedis, Collections.singleton(companyId));
		} finally {
			jedis.close();
		}
		
		if (applyEnable) {
			this.trainApplyNotifyTaskHandler.schedule(
					companyId + ":" + newTrainId, 
					OfflineTrainingDAOProtos.TrainApplyNotifyData.newBuilder()
					.setCompanyId(companyId)
					.setTrainId(newTrainId)
					.setApplyStartTime(applyStartTime)
					.build(), applyStartTime);
		}
		
		if (state == OfflineTrainingProtos.State.NORMAL) {
			// notify user
			this.serviceExecutor.execute(new TrainNotifyTask(head, newTrain, CREATE_TRAIN_NOTIFY_WORDING));
		}
		
		return Futures.immediateFuture(CreateTrainResponse.newBuilder()
				.setResult(CreateTrainResponse.Result.SUCC)
				.setTrainId(newTrainId)
				.build());
	}

	@Override
	public ListenableFuture<UpdateTrainResponse> updateTrain(AdminHead head, UpdateTrainRequest request) {
		final long companyId = head.getCompanyId();
		final int trainId = request.getTrainId();
		final String trainName = request.getTrainName().trim();
		if (trainName.isEmpty()) {
			return Futures.immediateFuture(UpdateTrainResponse.newBuilder()
					.setResult(UpdateTrainResponse.Result.FAIL_NAME_INVALID)
					.setFailText("培训名称不能为空")
					.build());
		} else if (trainName.length() > 50) {
			return Futures.immediateFuture(UpdateTrainResponse.newBuilder()
					.setResult(UpdateTrainResponse.Result.FAIL_NAME_INVALID)
					.setFailText("培训名称最多50个字")
					.build());
		}
		
		final String imageName = request.hasImageName() ? request.getImageName() : null;
		if (imageName != null && imageName.length() > 191) {
			return Futures.immediateFuture(UpdateTrainResponse.newBuilder()
					.setResult(UpdateTrainResponse.Result.FAIL_IMAGE_INVALID)
					.setFailText("图片名最多191个字")
					.build());
		}
		
		final int startTime = request.getStartTime();
		final int endTime = request.getEndTime();
		if (startTime >= endTime) {
			return Futures.immediateFuture(UpdateTrainResponse.newBuilder()
					.setResult(UpdateTrainResponse.Result.FAIL_TIME_INVLID)
					.setFailText("开始时间不能晚于结束时间")
					.build());
		}
		
		final boolean applyEnable = request.getApplyEnable();
		
		final Integer applyStartTime;
		final Integer applyEndTime;
		final Integer applyUserCount;
		if (applyEnable) {
			if (!request.hasApplyStartTime()) {
				return Futures.immediateFuture(UpdateTrainResponse.newBuilder()
						.setResult(UpdateTrainResponse.Result.FAIL_APPLY_INVALD)
						.setFailText("报名开始时间为空")
						.build());
			}
			applyStartTime = request.getApplyStartTime();
			if (!request.hasApplyEndTime()) {
				return Futures.immediateFuture(UpdateTrainResponse.newBuilder()
						.setResult(UpdateTrainResponse.Result.FAIL_APPLY_INVALD)
						.setFailText("报名结束时间为空")
						.build());
			}
			applyEndTime = request.getApplyEndTime();
			if (applyStartTime >= applyEndTime) {
				return Futures.immediateFuture(UpdateTrainResponse.newBuilder()
						.setResult(UpdateTrainResponse.Result.FAIL_TIME_INVLID)
						.setFailText("报名开始时间不能晚于报名结束时间")
						.build());
			}
			if (applyEndTime > endTime) {
				return Futures.immediateFuture(UpdateTrainResponse.newBuilder()
						.setResult(UpdateTrainResponse.Result.FAIL_TIME_INVLID)
						.setFailText("报名结束时间不能晚于培训结束时间")
						.build());
			}
			
			if (!request.hasApplyUserCount()) {
				return Futures.immediateFuture(UpdateTrainResponse.newBuilder()
						.setResult(UpdateTrainResponse.Result.FAIL_APPLY_INVALD)
						.setFailText("报名人数限制为空")
						.build());
			}
			if (request.getApplyUserCount() <= 0) {
				return Futures.immediateFuture(UpdateTrainResponse.newBuilder()
						.setResult(UpdateTrainResponse.Result.FAIL_APPLY_INVALD)
						.setFailText("报名人数限制必须大约0")
						.build());
			}
			applyUserCount = request.getApplyUserCount();
		} else {
			applyStartTime = null;
			applyEndTime = null;
			applyUserCount = null;
		}
		
		final String trainAddress = request.hasTrainAddress() ? request.getTrainAddress().trim() : null;
		if (trainAddress != null && trainAddress.length() > 191) {
			return Futures.immediateFuture(UpdateTrainResponse.newBuilder()
					.setResult(UpdateTrainResponse.Result.FAIL_ADDRESS_INVALID)
					.setFailText("培训地点最多为191个字")
					.build());
		}
		
		final String lecturerName = request.hasLecturerName() ? request.getLecturerName().trim() : null;
		if (lecturerName != null && lecturerName.length() > 191) {
			return Futures.immediateFuture(UpdateTrainResponse.newBuilder()
					.setResult(UpdateTrainResponse.Result.FAIL_LECTURER_INVALID)
					.setFailText("讲师名称最多为191个字")
					.build());
		}
		
		final Set<Long> lectureUserIdSet = new TreeSet<Long>(request.getLecturerUserIdList());
		if (lectureUserIdSet.size() > 100) {
			return Futures.immediateFuture(UpdateTrainResponse.newBuilder()
					.setResult(UpdateTrainResponse.Result.FAIL_LECTURER_INVALID)
					.setFailText("讲师最多为100个")
					.build());
		}
		
		final int checkInStartTime = request.getCheckInStartTime();
		final int checkInEndTime = request.getCheckInEndTime();
		if (checkInStartTime >= checkInEndTime) {
			return Futures.immediateFuture(UpdateTrainResponse.newBuilder()
					.setResult(UpdateTrainResponse.Result.FAIL_CHECK_IN_INVALID)
					.setFailText("签到开始时间不能晚于签到结束时间")
					.build());
		}
		if (checkInEndTime > endTime) {
			return Futures.immediateFuture(UpdateTrainResponse.newBuilder()
					.setResult(UpdateTrainResponse.Result.FAIL_CHECK_IN_INVALID)
					.setFailText("签到结束时间不能晚于培训结束时间")
					.build());
		}
		
		final String arrangementText = request.getArrangementText();
		if (arrangementText.length() > 65535) {
			return Futures.immediateFuture(UpdateTrainResponse.newBuilder()
					.setResult(UpdateTrainResponse.Result.FAIL_UNKNOWN)
					.setFailText("培训安排最多65535个字")
					.build());
		}
		
		final String describeText = request.hasDescribeText() ? request.getDescribeText() : null;
		if (describeText != null && describeText.length() > 65535) {
			return Futures.immediateFuture(UpdateTrainResponse.newBuilder()
					.setResult(UpdateTrainResponse.Result.FAIL_UNKNOWN)
					.setFailText("培训简介最多65535个字")
					.build());
		}
		
		final OfflineTrainingProtos.State state = request.hasState() ? request.getState() : OfflineTrainingProtos.State.NORMAL;
		if (state != OfflineTrainingProtos.State.NORMAL && state != OfflineTrainingProtos.State.DISABLE) {
			return Futures.immediateFuture(UpdateTrainResponse.newBuilder()
					.setResult(UpdateTrainResponse.Result.FAIL_UNKNOWN)
					.setFailText("培训状态错误，必须为启用或者禁用")
					.build());
		}
		
		final OfflineTrainingProtos.Train oldTrain = OfflineTrainingUtil.getTrain(hikariDataSource, jedisPool, companyId, Collections.singleton(trainId), ADMIN_STATE_SET).get(trainId);
		if (oldTrain == null) {
			return Futures.immediateFuture(UpdateTrainResponse.newBuilder()
					.setResult(UpdateTrainResponse.Result.FAIL_UNKNOWN)
					.setFailText("该培训不存在，或者已经被删除")
					.build());
		}
		
		final int now = (int) (System.currentTimeMillis() / 1000L);
		
		OfflineTrainingProtos.Train.Builder builder = oldTrain.toBuilder();
		builder.setTrainName(trainName);
		if (imageName != null) {
			builder.setImageName(imageName);
		}
		builder.setStartTime(startTime);
		builder.setEndTime(endTime);
		builder.setApplyEnable(applyEnable);
		if (applyEnable) {
			builder.setApplyStartTime(applyStartTime);
			builder.setApplyEndTime(applyEndTime);
			builder.setApplyUserCount(applyUserCount);
			
			if (oldTrain.getApplyEnable()) {
				if (oldTrain.getApplyStartTime() != applyStartTime) {
					builder.setApplyIsNotify(false);
				} else {
					builder.setApplyIsNotify(oldTrain.getApplyIsNotify());
				}
			} else {
				builder.setApplyIsNotify(false);
			}
		}
		
		builder.setTrainAddress(trainAddress);
		if (lecturerName != null) {
			builder.setLecturerName(lecturerName);
		}
		builder.clearLecturerUserId().addAllLecturerUserId(lectureUserIdSet);
		builder.setCheckInStartTime(checkInStartTime);
		builder.setCheckInEndTime(checkInEndTime);
		builder.setArrangementText(arrangementText);
		if (describeText != null) {
			builder.setDescribeText(describeText);
		}
		if (request.hasAllowModelId()) {
			builder.setAllowModelId(request.getAllowModelId());
		}
		
		builder.setState(state);
		builder.setUpdateAdminId(head.getSession().getAdminId());
		builder.setUpdateTime(now);
		
		final OfflineTrainingProtos.Train newTrain = builder.build();
		
		Connection dbConn = null;
		try {
			dbConn = this.hikariDataSource.getConnection();
			OfflineTrainingDB.updateTrain(dbConn, companyId, oldTrain, newTrain);
		} catch (SQLException e) {
			throw new RuntimeException("db fail", e);
		} finally {
			DBUtil.closeQuietly(dbConn);
		}
		
		Jedis jedis = this.jedisPool.getResource();
		try {
			OfflineTrainingCache.delTrain(jedis, companyId, Collections.singleton(trainId));
			OfflineTrainingCache.delOpenTrainIndexList(jedis, Collections.singleton(companyId));
			OfflineTrainingCache.delClosedTrainIndexList(jedis, Collections.singleton(companyId));
		} finally {
			jedis.close();
		}
		
		if (newTrain.getApplyEnable() && !newTrain.getApplyIsNotify()) {
			this.trainApplyNotifyTaskHandler.schedule(
					companyId + ":" + trainId, 
					OfflineTrainingDAOProtos.TrainApplyNotifyData.newBuilder()
					.setCompanyId(companyId)
					.setTrainId(trainId)
					.setApplyStartTime(applyStartTime)
					.build(), applyStartTime);
		}
		
		if (request.getEnableNotifyUser()) {
			// notify user
			if (state == OfflineTrainingProtos.State.NORMAL) {
				if (oldTrain.getState() == OfflineTrainingProtos.State.NORMAL) {
					this.serviceExecutor.execute(new TrainNotifyTask(head, newTrain, UPDATE_TRAIN_NOTIFY_WORDING));
				} else if (oldTrain.getState() == OfflineTrainingProtos.State.DISABLE) {
					this.serviceExecutor.execute(new TrainNotifyTask(head, newTrain, CREATE_TRAIN_NOTIFY_WORDING));
				}
			} else if (state == OfflineTrainingProtos.State.DISABLE) {
				if (oldTrain.getState() == OfflineTrainingProtos.State.NORMAL) {
					this.serviceExecutor.execute(new TrainNotifyTask(head, newTrain, DELETE_TRAIN_NOTIFY_WORDING));
				}
			}
		}
		
		return Futures.immediateFuture(UpdateTrainResponse.newBuilder()
				.setResult(UpdateTrainResponse.Result.SUCC)
				.build());
	}

	@Override
	public ListenableFuture<UpdateTrainStateResponse> updateTrainState(AdminHead head, UpdateTrainStateRequest request) {
		final long companyId = head.getCompanyId();
		final Set<Integer> trainIdSet = new TreeSet<Integer>(request.getTrainIdList());
		if (trainIdSet.isEmpty()) {
			return Futures.immediateFuture(UpdateTrainStateResponse.newBuilder()
					.setResult(UpdateTrainStateResponse.Result.SUCC)
					.build());
		}
		
		final OfflineTrainingProtos.State state = request.getState();
		
		Map<Integer, OfflineTrainingProtos.Train> trainMap = OfflineTrainingUtil.getTrain(hikariDataSource, jedisPool, companyId, trainIdSet, ADMIN_STATE_SET);
		if (!trainIdSet.equals(trainMap.keySet())) {
			return Futures.immediateFuture(UpdateTrainStateResponse.newBuilder()
					.setResult(UpdateTrainStateResponse.Result.FAIL_UNKNOWN)
					.setFailText("以下培训id未找到对应的培训内容：" + Sets.difference(trainIdSet, trainMap.keySet()))
					.build());
		}
		
		Iterator<Integer> it = trainIdSet.iterator();
		while (it.hasNext()) {
			Integer trainId = it.next();
			if (trainMap.get(trainId).getState() == state) {
				it.remove();
			}
		}
		
		Connection dbConn = null;
		try {
			dbConn = this.hikariDataSource.getConnection();
			OfflineTrainingDB.updateTrainState(dbConn, companyId, trainIdSet, state);
		} catch (SQLException e) {
			throw new RuntimeException("db fail", e);
		} finally {
			DBUtil.closeQuietly(dbConn);
		}
		
		Jedis jedis = this.jedisPool.getResource();
		try {
			OfflineTrainingCache.delTrain(jedis, companyId, trainIdSet);
			OfflineTrainingCache.delOpenTrainIndexList(jedis, Collections.singleton(companyId));
			OfflineTrainingCache.delClosedTrainIndexList(jedis, Collections.singleton(companyId));
		} finally {
			jedis.close();
		}
		
		if (state == OfflineTrainingProtos.State.DELETE || state == OfflineTrainingProtos.State.DISABLE) {
			for (Integer trainId : trainIdSet) {
				OfflineTrainingProtos.Train train = trainMap.get(trainId);
				if (train.getState() == OfflineTrainingProtos.State.NORMAL) {
					// notify user
					this.serviceExecutor.execute(new TrainNotifyTask(head, train, DELETE_TRAIN_NOTIFY_WORDING));
				}
			}
		} else if (state == OfflineTrainingProtos.State.NORMAL) {
			for (Integer trainId : trainIdSet) {
				OfflineTrainingProtos.Train train = trainMap.get(trainId);
				if (train.getState() == OfflineTrainingProtos.State.DISABLE) {
					// notify user
					this.serviceExecutor.execute(new TrainNotifyTask(head, train, CREATE_TRAIN_NOTIFY_WORDING));
				}
			}
		}
		
		return Futures.immediateFuture(UpdateTrainStateResponse.newBuilder()
				.setResult(UpdateTrainStateResponse.Result.SUCC)
				.build());
	}
	
	@Override
	public ListenableFuture<UpdateTrainDiscoverItemResponse> updateTrainDiscoverItem(AdminHead head, UpdateTrainDiscoverItemRequest request) {
		final long companyId = head.getCompanyId();
		final int trainId = request.getTrainId();
		final Set<Long> newItemIdSet = new TreeSet<Long>(request.getDiscoverItemIdList());
		
		final OfflineTrainingProtos.Train oldTrain = OfflineTrainingUtil.getTrain(hikariDataSource, jedisPool, companyId, 
				Collections.singleton(trainId), ADMIN_STATE_SET).get(trainId);
		
		if (oldTrain == null) {
			return Futures.immediateFuture(UpdateTrainDiscoverItemResponse.newBuilder()
					.setResult(UpdateTrainDiscoverItemResponse.Result.FAIL_UNKNOWN)
					.setFailText("该培训不存在或者已经被删除")
					.build());
		}
		
		if (newItemIdSet.equals(new TreeSet<Long>(oldTrain.getDiscoverItemIdList()))) {
			return Futures.immediateFuture(UpdateTrainDiscoverItemResponse.newBuilder()
					.setResult(UpdateTrainDiscoverItemResponse.Result.SUCC)
					.build());
		}
		
		final OfflineTrainingProtos.Train newTrain = oldTrain.toBuilder()
				.clearDiscoverItemId().addAllDiscoverItemId(newItemIdSet).build();
		
		Connection dbConn = null;
		try {
			dbConn = this.hikariDataSource.getConnection();
			OfflineTrainingDB.updateTrain(dbConn, companyId, oldTrain, newTrain);
		} catch (SQLException e) {
			throw new RuntimeException("db fail", e);
		} finally {
			DBUtil.closeQuietly(dbConn);
		}
		
		Jedis jedis = this.jedisPool.getResource();
		try {
			OfflineTrainingCache.delTrain(jedis, companyId, Collections.singleton(trainId));
		} finally {
			jedis.close();
		}
		
		return Futures.immediateFuture(UpdateTrainDiscoverItemResponse.newBuilder()
				.setResult(UpdateTrainDiscoverItemResponse.Result.SUCC)
				.build());
	}

	@Override
	public ListenableFuture<GetTrainUserListResponse> getTrainUserList(AdminHead head, GetTrainUserListRequest request) {
		final long companyId = head.getCompanyId();
		final int trainId = request.getTrainId();
		final int start = request.getStart() < 0 ? 0 : request.getStart();
		final int length = request.getLength() < 0 ? 0 : request.getLength() > 100 ? 100 : request.getLength();
		
		final Boolean isCheckIn = request.hasIsCheckIn() ? request.getIsCheckIn() : null;
		final Boolean isLeave = request.hasIsLeave() ? request.getIsLeave() : null;
		
		final OfflineTrainingProtos.Train train = OfflineTrainingUtil.getTrain(hikariDataSource, jedisPool, companyId, 
				Collections.singleton(trainId), ADMIN_STATE_SET).get(trainId);
		
		if (train == null) {
			return Futures.immediateFuture(GetTrainUserListResponse.newBuilder()
					.setTotalSize(0)
					.setFilteredSize(0)
					.build());
		}
		
		DataPage<OfflineTrainingProtos.TrainUser> trainUserPage;
		Connection dbConn = null;
		try {
			dbConn = this.hikariDataSource.getConnection();
			trainUserPage = OfflineTrainingDB.getTrainUserPage(dbConn, companyId, trainId, start, length, isCheckIn, isLeave);
		} catch (SQLException e) {
			throw new RuntimeException("db fail", e);
		} finally {
			DBUtil.closeQuietly(dbConn);
		}
		
		return Futures.immediateFuture(GetTrainUserListResponse.newBuilder()
				.addAllTrainUser(trainUserPage.dataList())
				.setTotalSize(trainUserPage.totalSize())
				.setFilteredSize(trainUserPage.filteredSize())
				.build());
	}
	
	private static final ProfileManager.ProfileKey<String> CREATE_TRAIN_NOTIFY_WORDING = 
			ProfileManager.createKey(
					"offline_training:create_train_notify_wording", 
					"培训部定于${start_time}在${train_address}进行《${train_name}》培训，请及时参加特此通知。"
					);
	
	private static final ProfileManager.ProfileKey<String> UPDATE_TRAIN_NOTIFY_WORDING = 
			ProfileManager.createKey(
					"offline_training:update_train_notify_wording", 
					"原培训部定于${start_time}在${train_address}进行的《${train_name}》培训进行了信息修改。"
					);
	
	private static final ProfileManager.ProfileKey<String> DELETE_TRAIN_NOTIFY_WORDING = 
			ProfileManager.createKey(
					"offline_training:delete_train_notify_wording", 
					"原培训部定于${start_time}在${train_address}进行的《${train_name}》培训因故取消，请知晓。"
					);

	private final class TrainNotifyTask implements Runnable {

		private final AdminHead head;
		private final OfflineTrainingProtos.Train train;
		private final ProfileManager.ProfileKey<String> notifyWordingProfileKey; 
		
		TrainNotifyTask(AdminHead head, OfflineTrainingProtos.Train train, ProfileManager.ProfileKey<String> notifyWordingProfileKey) {
			this.head = head;
			this.train = train;
			this.notifyWordingProfileKey = notifyWordingProfileKey;
		}
		
		@Override
		public void run() {
			
			ProfileManager.Profile profile = AdminOfflineTrainingServiceImpl.this.profileManager.getProfile(head, "offline_training:");
			String notifyWording = profile.get(notifyWordingProfileKey);
			
			DateFormat df = new SimpleDateFormat("MM月dd日HH:mm");
			notifyWording = notifyWording.replace("${train_name}", this.train.getTrainName());
			notifyWording = notifyWording.replace("${start_time}", df.format(new Date(this.train.getStartTime() * 1000L)));
			notifyWording = notifyWording.replace("${train_address}", this.train.getTrainAddress());
			
			int start = 0;
			final int length = 500;
			while (true) {
				GetUserListResponse getUserListResponse = Futures.getUnchecked(
						AdminOfflineTrainingServiceImpl.this.adminUserService.getUserList(
						this.head, 
						GetUserListRequest.newBuilder()
							.setStart(start)
							.setLength(length)
							.build())
						);
				
				Set<Long> userIdSet = Sets.newTreeSet();
				for (UserProtos.User user : getUserListResponse.getUserList()) {
					userIdSet.add(user.getBase().getUserId());
				}
				
				if (this.train.hasAllowModelId()) {
					CheckAllowResponse checkAllowResponse = Futures.getUnchecked(
							AdminOfflineTrainingServiceImpl.this.allowService.checkAllow(
							this.head, 
							CheckAllowRequest.newBuilder()
								.addAllUserId(userIdSet)
								.addModelId(this.train.getAllowModelId())
								.build())
							);
					for (AllowProtos.CheckAllowResponse.CheckResult checkResult : checkAllowResponse.getCheckResultList()) {
						if (checkResult.getModelId() == this.train.getAllowModelId()) {
							userIdSet.retainAll(checkResult.getAllowUserIdList());
						}
					}
				}
				
				if (!userIdSet.isEmpty()) {
					AdminOfflineTrainingServiceImpl.this.adminOfficialService.sendSecretaryMessage(
							this.head, 
							SendSecretaryMessageRequest.newBuilder()
							.addAllUserId(userIdSet)
							.setSendMsg(OfficialProtos.OfficialMessage.newBuilder()
									.setMsgSeq(0)
									.setMsgTime(0)
									.setIsFromUser(false)
									.setText(OfficialProtos.OfficialMessage.Text.newBuilder()
											.setContent(notifyWording)
											.build())
									.build())
							.build());
				}
				
				start += length;
				if (start >= getUserListResponse.getFilteredSize()) {
					break;
				}
			}
		}
	}
	
	private static final ProfileManager.ProfileKey<String> TOMORROW_TRAIN_NOTIFY_WORDING = 
			ProfileManager.createKey(
					"offline_training:tomorrow_train_notify_wording", 
					"《${train_name}》培训明天${start_time}在${train_address}进行，别忘了参加哦。"
					);
	
	private final class TomorrowTrainNotifyTask implements TaskManager.TaskPrototype<EmptyRequest> {

		@Override
		public void execute(EmptyRequest data) {
			Calendar cal = Calendar.getInstance();
			cal.set(Calendar.HOUR_OF_DAY, 0);
			cal.set(Calendar.MINUTE, 0);
			cal.set(Calendar.SECOND, 0);
			cal.set(Calendar.MILLISECOND, 0);
			cal.add(Calendar.DAY_OF_YEAR, 1);
			
			final int tomorrow = (int)(cal.getTime().getTime() / 1000L);
			final Set<OfflineTrainingProtos.State> stateSet = Collections.singleton(OfflineTrainingProtos.State.NORMAL);
			
			Map<Long, List<Integer>> trainIdListMap;
			Connection dbConn = null;
			try {
				dbConn = AdminOfflineTrainingServiceImpl.this.hikariDataSource.getConnection();
				trainIdListMap = OfflineTrainingDB.getTrainIdListByStartTime(dbConn, 
						tomorrow, tomorrow + (24 * 60 * 60), stateSet);
			} catch (SQLException e) {
				throw new RuntimeException("db fail", e);
			} finally {
				DBUtil.closeQuietly(dbConn);
			}
		
			final DateFormat df = new SimpleDateFormat("HH:mm");
			for (Entry<Long, List<Integer>> entry : trainIdListMap.entrySet()) {
				final long companyId = entry.getKey();
				final SystemHead head = SystemHead.newBuilder().setCompanyId(companyId).build();
				
				ProfileManager.Profile profile = AdminOfflineTrainingServiceImpl.this.profileManager.getProfile(head, "offline_training:");
				final String notifyWording = profile.get(TOMORROW_TRAIN_NOTIFY_WORDING);
				if (notifyWording.isEmpty()) {
					continue;
				}
				
				Map<Integer, OfflineTrainingProtos.Train> trainMap = OfflineTrainingUtil.getTrain(hikariDataSource, jedisPool, companyId, entry.getValue(), stateSet);
				if (trainMap.isEmpty()) {
					continue;
				}
				
				Set<Integer> allowModelIdSet = new TreeSet<Integer>();
				for (OfflineTrainingProtos.Train train : trainMap.values()) {
					if (train.hasAllowModelId()) {
						allowModelIdSet.add(train.getAllowModelId());
					}
				}
				
				int start = 0;
				final int length = 500;
				while (true) {
					GetUserListResponse getUserListResponse = Futures.getUnchecked(
							AdminOfflineTrainingServiceImpl.this.adminUserService.getUserList(
							head, 
							GetUserListRequest.newBuilder()
								.setStart(start)
								.setLength(length)
								.build())
							);
					
					List<Long> allUserIdList = new ArrayList<Long>();
					for (UserProtos.User user : getUserListResponse.getUserList()) {
						if (user.getBase().getState() == UserProtos.UserBase.State.NORMAL) {
							allUserIdList.add(user.getBase().getUserId());
						}
					}
					
					Map<Integer, List<Long>> allowModelUserIdMap;
					if (allowModelIdSet.isEmpty()) {
						allowModelUserIdMap = Collections.emptyMap();
					} else {
						CheckAllowResponse checkAllowResponse = Futures.getUnchecked(
								AdminOfflineTrainingServiceImpl.this.allowService.checkAllow(
								head, 
								CheckAllowRequest.newBuilder()
									.addAllUserId(allUserIdList)
									.addAllModelId(allowModelIdSet)
									.build())
								);
						allowModelUserIdMap = new TreeMap<Integer, List<Long>>();
						for (AllowProtos.CheckAllowResponse.CheckResult checkResult : checkAllowResponse.getCheckResultList()) {
							allowModelUserIdMap.put(checkResult.getModelId(), checkResult.getAllowUserIdList());
						}
					}
					
					for (final OfflineTrainingProtos.Train train : trainMap.values()) {
						String trainNotifyWording = notifyWording;
						trainNotifyWording = trainNotifyWording.replace("${train_name}", train.getTrainName());
						trainNotifyWording = trainNotifyWording.replace("${start_time}", df.format(new Date(train.getStartTime() * 1000L)));
						trainNotifyWording = trainNotifyWording.replace("${train_address}", train.getTrainAddress());
						
						final List<Long> userIdList;
						if (train.hasAllowModelId()) {
							List<Long> tmp = allowModelUserIdMap.get(train.getAllowModelId());
							userIdList = tmp == null ? Collections.<Long>emptyList() : tmp;
						} else {
							userIdList = allUserIdList;
						}
						
						if (!userIdList.isEmpty()) {
							AdminOfflineTrainingServiceImpl.this.adminOfficialService.sendSecretaryMessage(
									head, 
									SendSecretaryMessageRequest.newBuilder()
									.addAllUserId(userIdList)
									.setSendMsg(OfficialProtos.OfficialMessage.newBuilder()
											.setMsgSeq(0)
											.setMsgTime(0)
											.setIsFromUser(false)
											.setText(OfficialProtos.OfficialMessage.Text.newBuilder()
													.setContent(trainNotifyWording)
													.build())
											.build())
									.build());
						}
					}
					
					start += length;
					if (start >= getUserListResponse.getFilteredSize()) {
						break;
					}
				}
			}
		}
		
	}
	
	private static final ProfileManager.ProfileKey<String> TRAIN_APPLY_NOTIFY_WORDING = 
			ProfileManager.createKey(
					"offline_training:train_apply_notify_wording", 
					"《${train_name}》培训马上就要在${train_address}开始了如果您已经进入会场，别忘了扫二维码签到。"
					);
	
	private final class TrainApplyNotifyTask implements TaskManager.TaskPrototype<OfflineTrainingDAOProtos.TrainApplyNotifyData> {

		@Override
		public void execute(OfflineTrainingDAOProtos.TrainApplyNotifyData data) {
			final long companyId = data.getCompanyId();
			final int trainId = data.getTrainId();
			final int applyStartTime = data.getApplyStartTime();
			
			OfflineTrainingProtos.Train train = OfflineTrainingUtil.getTrain(hikariDataSource, jedisPool, companyId, 
					Collections.singleton(trainId), Collections.singleton(OfflineTrainingProtos.State.NORMAL)).get(trainId);
			if ((train == null) || !train.getApplyEnable() || (train.getApplyStartTime() != applyStartTime)) {
				return;
			}
			
			boolean isUpdateSucc;
			Connection dbConn = null;
			try {
				dbConn = AdminOfflineTrainingServiceImpl.this.hikariDataSource.getConnection();
				isUpdateSucc = OfflineTrainingDB.updateTrainApplyIsNotify(dbConn, companyId, trainId, true, applyStartTime, OfflineTrainingProtos.State.NORMAL);
			} catch (SQLException e) {
				throw new RuntimeException("db fail", e);
			} finally {
				DBUtil.closeQuietly(dbConn);
			}
			
			if (!isUpdateSucc) {
				return;
			}
			
			Jedis jedis = AdminOfflineTrainingServiceImpl.this.jedisPool.getResource();
			try {
				OfflineTrainingCache.delTrain(jedis, companyId, Collections.singleton(trainId));
			} finally {
				jedis.close();
			}
			
			final SystemHead head = SystemHead.newBuilder().setCompanyId(companyId).build();
			ProfileManager.Profile profile = AdminOfflineTrainingServiceImpl.this.profileManager.getProfile(head, "offline_training:");
			String notifyWording = profile.get(TRAIN_APPLY_NOTIFY_WORDING);
			
			DateFormat df = new SimpleDateFormat("MM月dd日HH:mm");
			notifyWording = notifyWording.replace("${train_name}", train.getTrainName());
			notifyWording = notifyWording.replace("${start_time}", df.format(new Date(train.getStartTime() * 1000L)));
			notifyWording = notifyWording.replace("${train_address}", train.getTrainAddress());
			
			int start = 0;
			final int length = 500;
			while (true) {
				GetUserListResponse getUserListResponse = Futures.getUnchecked(
						AdminOfflineTrainingServiceImpl.this.adminUserService.getUserList(
						head, 
						GetUserListRequest.newBuilder()
							.setStart(start)
							.setLength(length)
							.build())
						);
				
				Set<Long> userIdSet = Sets.newTreeSet();
				for (UserProtos.User user : getUserListResponse.getUserList()) {
					userIdSet.add(user.getBase().getUserId());
				}
				
				if (!userIdSet.isEmpty() && train.hasAllowModelId()) {
					CheckAllowResponse checkAllowResponse = Futures.getUnchecked(
							AdminOfflineTrainingServiceImpl.this.allowService.checkAllow(
							head, 
							CheckAllowRequest.newBuilder()
								.addAllUserId(userIdSet)
								.addModelId(train.getAllowModelId())
								.build())
							);
					for (AllowProtos.CheckAllowResponse.CheckResult checkResult : checkAllowResponse.getCheckResultList()) {
						if (checkResult.getModelId() == train.getAllowModelId()) {
							userIdSet.retainAll(checkResult.getAllowUserIdList());
						}
					}
				}
				
				if (!userIdSet.isEmpty()) {
					AdminOfflineTrainingServiceImpl.this.adminOfficialService.sendSecretaryMessage(
							head, 
							SendSecretaryMessageRequest.newBuilder()
							.addAllUserId(userIdSet)
							.setSendMsg(OfficialProtos.OfficialMessage.newBuilder()
									.setMsgSeq(0)
									.setMsgTime(0)
									.setIsFromUser(false)
									.setText(OfficialProtos.OfficialMessage.Text.newBuilder()
											.setContent(notifyWording)
											.build())
									.build())
							.build());
				}
				
				start += length;
				if (start >= getUserListResponse.getFilteredSize()) {
					break;
				}
			}
		}
		
	}
}
