package com.weizhu.service.absence;

import java.sql.Connection;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executor;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.weizhu.common.db.DBUtil;
import com.weizhu.common.utils.DataPage;
import com.weizhu.common.utils.ProfileManager;
import com.weizhu.proto.AbsenceProtos;
import com.weizhu.proto.AbsenceProtos.CancelAbsenceRequest;
import com.weizhu.proto.AbsenceProtos.CancelAbsenceResponse;
import com.weizhu.proto.AbsenceProtos.CreateAbsenceRequest;
import com.weizhu.proto.AbsenceProtos.CreateAbsenceResponse;
import com.weizhu.proto.AbsenceProtos.GetAbsenceByIdRequest;
import com.weizhu.proto.AbsenceProtos.GetAbsenceByIdResponse;
import com.weizhu.proto.AbsenceProtos.GetAbsenceCliRequest;
import com.weizhu.proto.AbsenceProtos.GetAbsenceCliResponse;
import com.weizhu.proto.AbsenceProtos.GetAbsenceNowResponse;
import com.weizhu.proto.AbsenceProtos.GetAbsenceSerRequest;
import com.weizhu.proto.AbsenceProtos.GetAbsenceSerResponse;
import com.weizhu.proto.AbsenceProtos.UpdateAbsenceRequest;
import com.weizhu.proto.AbsenceProtos.UpdateAbsenceResponse;
import com.weizhu.proto.AbsenceService;
import com.weizhu.proto.AdminOfficialProtos;
import com.weizhu.proto.AdminOfficialService;
import com.weizhu.proto.AdminProtos.AdminHead;
import com.weizhu.proto.OfficialProtos;
import com.weizhu.proto.UserProtos;
import com.weizhu.proto.UserProtos.GetUserByIdRequest;
import com.weizhu.proto.UserProtos.GetUserResponse;
import com.weizhu.proto.UserService;
import com.weizhu.proto.WeizhuProtos.EmptyRequest;
import com.weizhu.proto.WeizhuProtos.RequestHead;
import com.zaxxer.hikari.HikariDataSource;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

public class AbsenceServiceImpl implements AbsenceService {
	
	private static final Logger logger = LoggerFactory.getLogger(AbsenceServiceImpl.class);
	
	private final HikariDataSource hikariDataSource;
	private final JedisPool jedisPool;
	private final UserService userService;
	private final AdminOfficialService adminOfficialService;
	private final ProfileManager profileManager;
	private final Executor serviceExecutor;
	private final ScheduledExecutorService scheduledExecutorService;
	
	private static final ProfileManager.ProfileKey<String> CREATE_ABSENCE_TEMPLATE = 
			ProfileManager.createKey("absence:template", "您好，${name}于${start_time}至${end_time}，因${absence_desc}的原因，请${absence_type},${days}天。请提前安排好相关工作。");
	private static final ProfileManager.ProfileKey<String> CANCEL_ABSENCE_TEMPLATE = 
			ProfileManager.createKey("absence:template", "您好，${name}于${start_time}，因${absence_desc}，请${absence_type}的假期已取消，请假${days}天。");
	private static final ProfileManager.ProfileKey<String> ABSENCE_NOTIFY = 
			ProfileManager.createKey("absence:notify", "您好，您有请假尚未取消，请及时处理。");
	
	@Inject
	public AbsenceServiceImpl(HikariDataSource hikariDataSource, JedisPool jedisPool, 
			UserService userService, AdminOfficialService adminOfficialService, ProfileManager profileManager,
			@Named("service_executor") Executor serviceExecutor,
			@Named("service_scheduled_executor") ScheduledExecutorService scheduledExecutorService) {
		this.hikariDataSource = hikariDataSource;
		this.jedisPool = jedisPool;
		this.userService = userService;
		this.adminOfficialService = adminOfficialService;
		this.profileManager = profileManager;
		this.serviceExecutor = serviceExecutor;
		this.scheduledExecutorService = scheduledExecutorService;
		
		loadNotifyAbsence();
	}

	@Override
	public ListenableFuture<GetAbsenceByIdResponse> getAbsenceById(
			RequestHead head, GetAbsenceByIdRequest request) {
		final long companyId = head.getSession().getCompanyId();
		final List<Integer> absenceIdList = request.getAbsenceIdList();
	
		Map<Integer, AbsenceProtos.Absence> absenceMap = AbsenceUtil.getAbsence(hikariDataSource, jedisPool, companyId, absenceIdList);
		
		return Futures.immediateFuture(GetAbsenceByIdResponse.newBuilder()
				.addAllAbsence(absenceMap.values())
				.build());
	}

	@Override
	public ListenableFuture<GetAbsenceNowResponse> getAbsenceNow(
			RequestHead head, EmptyRequest request) {
		final long companyId = head.getSession().getCompanyId();
		
		final int now = (int) (System.currentTimeMillis() / 1000L);
		final long userId = head.getSession().getUserId();
		
		AbsenceProtos.Absence absence = null;
		Connection conn = null;
		try {
			conn = this.hikariDataSource.getConnection();
			
			absence = AbsenceDB.getAbsenceNow(conn, companyId, now, Collections.singleton(userId)).get(userId);
		} catch (SQLException ex) {
			throw new RuntimeException("db fail", ex);
		} finally {
			DBUtil.closeQuietly(conn);
		}
		
		if (absence == null) {
			return Futures.immediateFuture(GetAbsenceNowResponse.getDefaultInstance());
		}
		
		return Futures.immediateFuture(GetAbsenceNowResponse.newBuilder()
				.setAbsence(absence)
				.build());
	}

	@Override
	public ListenableFuture<CreateAbsenceResponse> createAbsence(
			RequestHead head, CreateAbsenceRequest request) {
		final long companyId = head.getSession().getCompanyId();
		
		final String type = request.getType();
		if (type.length() > 191) {
			return Futures.immediateFuture(CreateAbsenceResponse.newBuilder()
					.setResult(CreateAbsenceResponse.Result.FAIL_TYPE_INVALID)
					.setFailText("请假类型过长")
					.build());
		}
		
		final int startTime = request.getStartTime();
		final int endTime = request.getEndTime();
		if (startTime > endTime) {
			return Futures.immediateFuture(CreateAbsenceResponse.newBuilder()
					.setResult(CreateAbsenceResponse.Result.FAIL_TIME_INVALID)
					.setFailText("开始时间大于结束时间")
					.build());
		}
		
		final String desc = request.getDesc();
		if (desc.length() > 191) {
			return Futures.immediateFuture(CreateAbsenceResponse.newBuilder()
					.setResult(CreateAbsenceResponse.Result.FAIL_DESC_INVALID)
					.setFailText("请假描述过长")
					.build());
		}
		
		final String days = request.getDays();
		if (days.length() > 191) {
			return Futures.immediateFuture(CreateAbsenceResponse.newBuilder()
					.setResult(CreateAbsenceResponse.Result.FAIL_DAYS_INVALID)
					.setFailText("请输入合理的请假天数")
					.build());
		}
		
		final long createUser = head.getSession().getUserId();
		final int createTime = (int) (System.currentTimeMillis() / 1000L);
		
		final List<Long> userIdList = request.getUserIdList();
		Set<Long> userIdSet = Sets.newTreeSet(userIdList);
		GetUserResponse getUserResponse = Futures.getUnchecked(userService.getUserById(head, 
				GetUserByIdRequest.newBuilder()
				.addAllUserId(userIdSet)
				.build()));
		List<Long> validUserIdList = Lists.newArrayList();
		for (UserProtos.User user : getUserResponse.getUserList()) {
			validUserIdList.add(user.getBase().getUserId());
		}
		
		int absenceId = 0;
		Connection conn = null;
		try {
			conn = this.hikariDataSource.getConnection();
			
			absenceId = AbsenceDB.insertAbsence(conn, head, companyId, type, startTime, endTime, desc, days, createUser, createTime, validUserIdList);
		} catch (SQLException ex) {
			throw new RuntimeException("db fail", ex);
		} finally {
			DBUtil.closeQuietly(conn);
		}
		
		Jedis jedis = jedisPool.getResource();
		try {
			AbsenceCache.delAbsence(jedis, companyId, Collections.singleton(absenceId));
		} finally {
			jedis.close();
		}
		
		String userName = null;
		GetUserResponse getUserResponse1 = Futures.getUnchecked(userService.getUserById(head, 
				GetUserByIdRequest.newBuilder()
				.addAllUserId(Collections.singleton(createUser))
				.build()));
		for (UserProtos.User user : getUserResponse1.getUserList()) {
			if (user.getBase().getUserId() == createUser) {
				userName = user.getBase().getUserName();
			}
		}
		
		// 通知
		SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd");
		ProfileManager.Profile profile = profileManager.getProfile(head, "absence:");
		String template = profile.get(CREATE_ABSENCE_TEMPLATE).replace("${name}", userName == null ? String.valueOf(createUser) : userName)
				.replace("${start_time}", df.format(new Date(startTime * 1000L)))
				.replace("${end_time}", df.format(new Date(endTime * 1000L)))
				.replace("${absence_desc}", desc)
				.replace("${absence_type}", type)
				.replace("${days}", days);
		
		Iterator<Long> it = userIdSet.iterator();
		while (it.hasNext()) {
			List<Long> list = Lists.newArrayList();
			while (list.size() < 1000 && it.hasNext()) {
				list.add(it.next());
			}
			
			adminOfficialService.sendSecretaryMessage(head,
					AdminOfficialProtos.SendSecretaryMessageRequest.newBuilder()
							.addAllUserId(list)
							.setSendMsg(OfficialProtos.OfficialMessage.newBuilder()
									.setMsgSeq(0)
									.setMsgTime(0)
									.setIsFromUser(false)
									.setText(OfficialProtos.OfficialMessage.Text.newBuilder()
											.setContent(template))
									.build())
							.build());
			logger.info("小秘书请假提醒：" + template + ", 提醒人数：" + list.size());
		}
		
		// 提示，半小时提示一次
		loadNotifyAbsence();

		return Futures.immediateFuture(CreateAbsenceResponse.newBuilder()
				.setAbsenceId(absenceId)
				.setResult(CreateAbsenceResponse.Result.SUCC)
				.build());
	}

	@Override
	public ListenableFuture<CancelAbsenceResponse> cancelAbsence(
			RequestHead head, CancelAbsenceRequest request) {
		final long companyId = head.getSession().getCompanyId();
		
		final int now = (int) (System.currentTimeMillis() / 1000L);
		final int absenceId = request.getAbsenceId();
		final String days = request.getDays();
		
		AbsenceProtos.Absence absence = AbsenceUtil.getAbsence(hikariDataSource, jedisPool, companyId, Collections.singleton(absenceId)).get(absenceId);
		if (absence == null) {
			return Futures.immediateFuture(CancelAbsenceResponse.newBuilder()
					.setResult(CancelAbsenceResponse.Result.SUCC)
					.build());
		}
		
		Connection conn = null;
		try {
			conn = this.hikariDataSource.getConnection();
			
			AbsenceDB.cancelAbsence(conn, companyId, now, absenceId, days);
		} catch (SQLException ex) {
			throw new RuntimeException("db fail", ex);
		} finally {
			DBUtil.closeQuietly(conn);
		}
		
		Jedis jedis = jedisPool.getResource();
		try {
			AbsenceCache.delAbsence(jedis, companyId, Collections.singleton(absenceId));
		} finally {
			jedis.close();
		}
		
		long createUserId = absence.getCreateUser();
		int startTime = absence.getStartTime();
		String desc = absence.getDesc();
		String type = absence.getType();
		
		String userName = null;
		GetUserResponse getUserResponse1 = Futures.getUnchecked(userService.getUserById(head, 
				GetUserByIdRequest.newBuilder()
				.addAllUserId(Collections.singleton(createUserId))
				.build()));
		for (UserProtos.User user : getUserResponse1.getUserList()) {
			if (user.getBase().getUserId() == createUserId) {
				userName = user.getBase().getUserName();
			}
		}
		
		// 通知
		SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd");
		ProfileManager.Profile profile = profileManager.getProfile(head, "absence:");
		String template = profile.get(CANCEL_ABSENCE_TEMPLATE).replace("${name}", userName == null ? String.valueOf(createUserId) : userName)
				.replace("${start_time}", df.format(new Date(startTime * 1000L)))
				.replace("${absence_desc}", desc)
				.replace("${absence_type}", type)
				.replace("${days}", days);
		
		Set<Long> notifyUserIdList = Sets.newTreeSet(absence.getUserIdList());
		Iterator<Long> it = notifyUserIdList.iterator();
		while (it.hasNext()) {
			List<Long> list = Lists.newArrayList();
			while (list.size() < 1000 && it.hasNext()) {
				list.add(it.next());
			}
			
			adminOfficialService.sendSecretaryMessage(head,
					AdminOfficialProtos.SendSecretaryMessageRequest.newBuilder()
							.addAllUserId(list)
							.setSendMsg(OfficialProtos.OfficialMessage.newBuilder()
									.setMsgSeq(0)
									.setMsgTime(0)
									.setIsFromUser(false)
									.setText(OfficialProtos.OfficialMessage.Text.newBuilder()
											.setContent(template))
									.build())
							.build());
			logger.info("小秘书销假提醒：" + template + ", 提醒人数：" + list.size());
		}
		
		return Futures.immediateFuture(CancelAbsenceResponse.newBuilder()
				.setResult(CancelAbsenceResponse.Result.SUCC)
				.build());
	}

	@Override
	public ListenableFuture<GetAbsenceCliResponse> getAbsenceCli(
			RequestHead head, GetAbsenceCliRequest request) {
		final long companyId = head.getSession().getCompanyId();
		final long userId = head.getSession().getUserId();
		
		final int size = request.getSize();
		final Integer lastAbsenceId = request.hasLastAbsenceId() ? request.getLastAbsenceId() : null;
		
		List<Integer> absenceIdList = null;
		Connection conn = null;
		try {
			conn = this.hikariDataSource.getConnection();
			
			absenceIdList = AbsenceDB.getAbsenceId(conn, companyId, userId, lastAbsenceId, size + 1);
		} catch (SQLException ex) {
			throw new RuntimeException("db fail", ex);
		} finally {
			DBUtil.closeQuietly(conn);
		}
		
		Map<Integer, AbsenceProtos.Absence> absenceMap = AbsenceUtil.getAbsence(hikariDataSource, jedisPool, companyId, absenceIdList);
		if (absenceMap.isEmpty()) {
			return Futures.immediateFuture(GetAbsenceCliResponse.newBuilder()
					.setHasMore(false)
					.build());
		}
		
		// 排序
		List<AbsenceProtos.Absence> list = Lists.newArrayList();
		for (int absenceId : absenceIdList) {
			list.add(absenceMap.get(absenceId));
		}
		
		boolean hasMore = list.size() > size;
		if (hasMore) {
			list = list.subList(0, size);
		}
		
		return Futures.immediateFuture(GetAbsenceCliResponse.newBuilder()
				.addAllAbsence(list)
				.setHasMore(hasMore)
				.build());
	}

	@Override
	public ListenableFuture<GetAbsenceByIdResponse> getAbsenceById(
			AdminHead head, GetAbsenceByIdRequest request) {
		final long companyId = head.getCompanyId();
		final List<Integer> absenceIdList = request.getAbsenceIdList();
		
		Map<Integer, AbsenceProtos.Absence> absenceMap = AbsenceUtil.getAbsence(hikariDataSource, jedisPool, companyId, absenceIdList);
		if (absenceMap.isEmpty()) {
			return Futures.immediateFuture(GetAbsenceByIdResponse.getDefaultInstance());
		}
		
		return Futures.immediateFuture(GetAbsenceByIdResponse.newBuilder()
				.addAllAbsence(absenceMap.values())
				.build());
	}

	@Override
	public ListenableFuture<GetAbsenceSerResponse> getAbsenceSer(
			AdminHead head, GetAbsenceSerRequest request) {
		final long companyId = head.getCompanyId();
		
		final int start = request.getStart() < 0 ? 0 : request.getStart();
		final int length = request.getLength() < 0 ? 0 : request.getLength() > 50 ? 50 : request.getLength();
		
		final List<Long> userIdList = request.getUserIdList();
		final Integer startTime = request.hasStartTime() ? request.getStartTime() : null;
		final Integer endTime = request.hasEndTime() ? request.getEndTime() : null;
		final GetAbsenceSerRequest.Action action = request.hasAction() ? request.getAction() : null;
		
		DataPage<Integer> absenceIdPage = null;
		Connection conn = null;
		try {
			conn = this.hikariDataSource.getConnection();
			
			absenceIdPage = AbsenceDB.getAbsenceId(conn, companyId, start, length, userIdList, startTime, endTime, action);
		} catch (SQLException ex) {
			throw new RuntimeException("db fail", ex);
		} finally {
			DBUtil.closeQuietly(conn);
		}
		
		Map<Integer, AbsenceProtos.Absence> absenceMap = AbsenceUtil.getAbsence(hikariDataSource, jedisPool, companyId, absenceIdPage.dataList());
		List<AbsenceProtos.Absence> absenceList = Lists.newArrayList();
		for (int absenceId : absenceIdPage.dataList()) {
			AbsenceProtos.Absence absence = absenceMap.get(absenceId);
			if (absence != null) {
				absenceList.add(absence);
			}
		}
		
		return Futures.immediateFuture(GetAbsenceSerResponse.newBuilder()
				.addAllAbsence(absenceList)
				.setTotal(absenceIdPage.totalSize())
				.setFilteredSize(absenceIdPage.filteredSize())
				.build());
	}

	@Override
	public ListenableFuture<UpdateAbsenceResponse> updateAbsence(
			AdminHead head, UpdateAbsenceRequest request) {
		final long companyId = head.getCompanyId();
		
		final String type = request.getType();
		if (type.length() > 191) {
			return Futures.immediateFuture(UpdateAbsenceResponse.newBuilder()
					.setResult(UpdateAbsenceResponse.Result.FAIL_TYPE_INVALID)
					.setFailText("请假类型过长")
					.build());
		}
		
		final int startTime = request.getStartTime();
		final int preEndTime = request.getPreEndTime();
		final Integer facEndTime = request.hasFacEndTime() ? request.getFacEndTime() : null;
		if (preEndTime < startTime) {
			return Futures.immediateFuture(UpdateAbsenceResponse.newBuilder()
					.setResult(UpdateAbsenceResponse.Result.FAIL_TIME_INVALID)
					.setFailText("开始时间大于结束时间")
					.build());
		}
		if (facEndTime != null && preEndTime < facEndTime) {
			return Futures.immediateFuture(UpdateAbsenceResponse.newBuilder()
					.setResult(UpdateAbsenceResponse.Result.FAIL_TIME_INVALID)
					.setFailText("开始时间大于结束时间")
					.build());
		}
		
		final String days = request.getDays();
		if (days.isEmpty()) {
			return Futures.immediateFuture(UpdateAbsenceResponse.newBuilder()
					.setResult(UpdateAbsenceResponse.Result.FAIL_DAYS_INVALID)
					.setFailText("传入的时间天数不能是空")
					.build());
		}
		
		final String desc = request.getDesc();
		if (desc.length() > 191) {
			return Futures.immediateFuture(UpdateAbsenceResponse.newBuilder()
					.setResult(UpdateAbsenceResponse.Result.FAIL_DESC_INVALID)
					.setFailText("请假描述过长")
					.build());
		}
		
		final int absenceId = request.getAbsenceId();
		AbsenceProtos.Absence absence = AbsenceUtil.getAbsence(hikariDataSource, jedisPool, companyId, Collections.singleton(absenceId)).get(absenceId);
		if (absence == null) {
			return Futures.immediateFuture(UpdateAbsenceResponse.newBuilder()
					.setResult(UpdateAbsenceResponse.Result.FAIL_ABSENCE_INVALID)
					.setFailText("不存在的请假" + absenceId)
					.build());
		}
		
		Connection conn = null;
		try {
			conn = this.hikariDataSource.getConnection();
			
			AbsenceDB.updateAbsence(conn, companyId, type, startTime, preEndTime, facEndTime, days, desc, absenceId);
		} catch (SQLException ex) {
			throw new RuntimeException("db fail", ex);
		} finally {
			DBUtil.closeQuietly(conn);
		}
		
		Jedis jedis = jedisPool.getResource();
		try {
			AbsenceCache.delAbsence(jedis, companyId, Collections.singleton(absenceId));
		} finally {
			jedis.close();
		}
		
		return Futures.immediateFuture(UpdateAbsenceResponse.newBuilder()
				.setResult(UpdateAbsenceResponse.Result.SUCC)
				.build());
	}
	
	private final ConcurrentMap<Integer, Integer> absenceNotifyMap = new ConcurrentHashMap<Integer, Integer>();
	
	public void loadNotifyAbsence() {
		final int now = (int) (System.currentTimeMillis() / 1000L);
		// 获取所有请假已经开始，但没到时间的
		Map<Long, List<Integer>> companyAbsenceIdMap = null;
		Connection conn = null;
		try {
			conn = AbsenceServiceImpl.this.hikariDataSource.getConnection();
			
			companyAbsenceIdMap = AbsenceDB.getAbsenceId(conn);
		} catch (SQLException ex) {
			throw new RuntimeException("db fail", ex);
		} finally {
			DBUtil.closeQuietly(conn);
		}
		
		for (Entry<Long, List<Integer>> entry : companyAbsenceIdMap.entrySet()) {
			long companyId = entry.getKey();
			List<Integer> absenceIdList = entry.getValue();
			
			Map<Integer, AbsenceProtos.Absence> absenceMap = AbsenceUtil.getAbsence(hikariDataSource, jedisPool, companyId, absenceIdList);
			for (AbsenceProtos.Absence absence : absenceMap.values()) {
				int absenceId = absence.getAbsenceId();
				int endTime = absence.getPreEndTime();
				Integer oldAbsenceEndTime = this.absenceNotifyMap.get(absenceId);
				
				boolean isTaskAdd = false;
				
				if (oldAbsenceEndTime == null) {
					if (absenceNotifyMap.putIfAbsent(absenceId, endTime) == null) {
						isTaskAdd = true;
					}
				} else {
					if (oldAbsenceEndTime.equals(endTime)) {
						isTaskAdd = false;
					} else {
						if (absenceNotifyMap.replace(absenceId, endTime) != null) {
							isTaskAdd = true;
						}
					}
				}

				if (isTaskAdd) {
					SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
					logger.info("load notify absence task : " + absenceId + ","
							+ endTime + ","
							+ df.format(new Date(endTime * 1000L)));
					
					RequestHead requestHead = null;
					try {
						conn = this.hikariDataSource.getConnection();
						
						requestHead = AbsenceDB.getAbsenceRequestHead(conn, companyId, absenceId);
					} catch (Exception ex) {
						throw new RuntimeException("get absence head fail", ex);
					} finally {
						DBUtil.closeQuietly(conn);
					}
					
					// 如果剩余时间不足30分钟直接提示
					int delay = endTime - now;
					if (delay < 30 * 60 && delay > 0) {
						this.serviceExecutor.execute(new NotifyCancelAbsence(requestHead, companyId, absenceId, absence.getCreateUser()));
					} else if (delay > 0) {
						this.scheduledExecutorService.scheduleAtFixedRate(new NotifyCancelAbsence(requestHead, companyId, absenceId, absence.getCreateUser()),
								delay > (60 * 60) ? delay - 60 * 60 : delay - 30 * 60, 30 * 60, TimeUnit.SECONDS);
					}
//					int delay = endTime - now;
//					if (delay < 3 && delay > 0) {
//						this.serviceExecutor.execute(new NotifyCancelAbsence(requestHead, companyId, absenceId, absence.getCreateUser()));
//					} else if (delay > 0) {
//						this.scheduledExecutorService.scheduleAtFixedRate(new NotifyCancelAbsence(requestHead, companyId, absenceId, absence.getCreateUser()),
//								delay > 6 ? 0 : delay - 3, 3, TimeUnit.SECONDS);
//					}
					
				}
			}
			
		
		}
		
	}
	
	private class NotifyCancelAbsence implements Runnable {
		private RequestHead requestHead;
		private long companyId;
		private int absenceId;
		private long createUserId;
		
		public NotifyCancelAbsence(RequestHead requestHead, long companyId, int absenceId, long createUserId) {
			this.requestHead = requestHead;
			this.companyId = companyId;
			this.absenceId = absenceId;
			this.createUserId = createUserId;
		}

		@Override
		public void run() {
			Integer endTime = absenceNotifyMap.get(absenceId);
			int now = (int) (System.currentTimeMillis() / 1000L);
			if (endTime == null || now > endTime.intValue()) {
				return;
			}
			
			AbsenceProtos.Absence absence = AbsenceUtil.getAbsence(hikariDataSource, jedisPool, companyId, Collections.singleton(absenceId)).get(absenceId);
			if (absence == null || (absence.getPreEndTime() < now) ||
					(absence.hasFacEndTime() && absence.getFacEndTime() < now)) {
				return;
			}
			
			ProfileManager.Profile profile = profileManager.getProfile(requestHead, "absence:");
			String template = profile.get(ABSENCE_NOTIFY);
			
			AbsenceServiceImpl.this.adminOfficialService.sendSecretaryMessage(requestHead,
					AdminOfficialProtos.SendSecretaryMessageRequest.newBuilder()
							.addUserId(createUserId)
							.setSendMsg(OfficialProtos.OfficialMessage.newBuilder()
									.setMsgSeq(0)
									.setMsgTime(0)
									.setIsFromUser(false)
									.setText(OfficialProtos.OfficialMessage.Text.newBuilder()
											.setContent(template))
									.build())
							.build());
			logger.info("小秘书请假提醒：absenceId: " + absenceId + ", userId：" + createUserId);
		}
		
	}

}
