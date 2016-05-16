package com.weizhu.service.user.experience;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.weizhu.common.db.DBUtil;
import com.weizhu.proto.UserProtos;
import com.weizhu.proto.UserProtos.CreateUserExperienceRequest;
import com.weizhu.proto.UserProtos.CreateUserExperienceResponse;
import com.weizhu.proto.UserProtos.DeleteUserExperienceRequest;
import com.weizhu.proto.UserProtos.DeleteUserExperienceResponse;
import com.weizhu.proto.UserProtos.GetUserExperienceRequest;
import com.weizhu.proto.UserProtos.GetUserExperienceResponse;
import com.weizhu.proto.UserProtos.UpdateUserExperienceRequest;
import com.weizhu.proto.UserProtos.UpdateUserExperienceResponse;
import com.weizhu.proto.WeizhuProtos.RequestHead;
import com.zaxxer.hikari.HikariDataSource;

@Singleton
public class ExperienceManager {

	private final HikariDataSource hikariDataSource;
	private final JedisPool jedisPool;
	
	@Inject
	public ExperienceManager(HikariDataSource hikariDataSource, JedisPool jedisPool) {
		this.hikariDataSource = hikariDataSource;
		this.jedisPool = jedisPool;
	}
	
	public Map<Long, List<UserProtos.UserExperience>> getUserExperience(long companyId, Collection<Long> userIds) {
		if (userIds.isEmpty()) {
			return Collections.emptyMap();
		}
		
		Map<Long, List<UserProtos.UserExperience>> experienceMap = new HashMap<Long, List<UserProtos.UserExperience>>();
		
		Set<Long> noCacheUserIdSet = new TreeSet<Long>();
		Jedis jedis = this.jedisPool.getResource();
		try {
			experienceMap.putAll(ExperienceCache.getUserExperience(jedis, companyId, userIds, noCacheUserIdSet));
		} finally {
			jedis.close();
		}
		
		if (noCacheUserIdSet.isEmpty()) {
			return experienceMap;
		}
		
		Map<Long, List<UserProtos.UserExperience>> noCacheExperienceMap;
		Connection dbConn = null;
		try {
			dbConn = hikariDataSource.getConnection();
			noCacheExperienceMap = ExperienceDB.getUserExperience(dbConn, companyId, noCacheUserIdSet);
		} catch (SQLException e) {
			throw new RuntimeException("db fail", e);
		} finally {
			DBUtil.closeQuietly(dbConn);
		}
		
		jedis = jedisPool.getResource();
		try {
			ExperienceCache.setUserExperience(jedis, companyId, noCacheUserIdSet, noCacheExperienceMap);
		} finally {
			jedis.close();
		}
		
		experienceMap.putAll(noCacheExperienceMap);
		
		return experienceMap;
	}
	
	public GetUserExperienceResponse getUserExperience(RequestHead head, GetUserExperienceRequest request) {
		final long companyId = head.getSession().getCompanyId();
		List<UserProtos.UserExperience> list = this.getUserExperience(companyId, Collections.singleton(request.getUserId())).get(request.getUserId());
		if (list == null) {
			return GetUserExperienceResponse.newBuilder().build();
		} else {
			return GetUserExperienceResponse.newBuilder().addAllExperience(list).build();
		}
	}

	public CreateUserExperienceResponse createUserExperience(RequestHead head, CreateUserExperienceRequest request) {
		final long companyId = head.getSession().getCompanyId();
		final String experienceContent = request.getExperience().getExperienceContent().trim();
		if (experienceContent.isEmpty()) {
			return CreateUserExperienceResponse.newBuilder()
					.setResult(CreateUserExperienceResponse.Result.FAIL_EXPERIENCE_CONTENT_INVALID)
					.setFailText("项目经验内容为空")
					.build();
		}
		if (experienceContent.length() > 20) {
			return CreateUserExperienceResponse.newBuilder()
					.setResult(CreateUserExperienceResponse.Result.FAIL_EXPERIENCE_CONTENT_INVALID)
					.setFailText("项目经验单条最多20字")
					.build();
		}
		
		final long userId = head.getSession().getUserId();
		List<UserProtos.UserExperience> list = this.getUserExperience(companyId, Collections.singleton(userId)).get(userId);
		if (list != null && list.size() >= 20) {
			return CreateUserExperienceResponse.newBuilder()
					.setResult(CreateUserExperienceResponse.Result.FAIL_EXPERIENCE_NUM_LIMIT)
					.setFailText("项目经验最多20条")
					.build();
		}
		
		List<UserProtos.UserExperience> experienceList;
		int experienceId = -1;
		Connection dbConn = null;
		try {
			dbConn = hikariDataSource.getConnection();
			experienceId = ExperienceDB.insertUserExperience(dbConn, companyId, userId, experienceContent);
			experienceList = ExperienceDB.getUserExperience(dbConn, companyId, Collections.singleton(userId)).get(userId);
		} catch (SQLException e) {
			throw new RuntimeException("db fail", e);
		} finally {
			DBUtil.closeQuietly(dbConn);
		}
		
		Jedis jedis = jedisPool.getResource();
		try {
			ExperienceCache.setUserExperience(jedis, companyId, Collections.<Long, List<UserProtos.UserExperience>>singletonMap(userId, experienceList));
		} finally {
			jedis.close();
		}
		
		return CreateUserExperienceResponse.newBuilder()
				.setResult(CreateUserExperienceResponse.Result.SUCC)
				.setExperience(UserProtos.UserExperience.newBuilder()
						.setExperienceId(experienceId)
						.setExperienceContent(request.getExperience().getExperienceContent()))
				.build();
	}

	public UpdateUserExperienceResponse updateUserExperience(RequestHead head, UpdateUserExperienceRequest request) {
		final long companyId = head.getSession().getCompanyId();
		final int experienceId = request.getExperience().getExperienceId();
		final String experienceContent = request.getExperience().getExperienceContent().trim();
		if (experienceContent.isEmpty()) {
			return UpdateUserExperienceResponse.newBuilder()
					.setResult(UpdateUserExperienceResponse.Result.FAIL_EXPERIENCE_CONTENT_INVALID)
					.setFailText("项目经验内容为空")
					.build();
		}
		if (experienceContent.length() > 20) {
			return UpdateUserExperienceResponse.newBuilder()
					.setResult(UpdateUserExperienceResponse.Result.FAIL_EXPERIENCE_CONTENT_INVALID)
					.setFailText("项目经验单条最多20字")
					.build();
		}
		
		final long userId = head.getSession().getUserId();
		List<UserProtos.UserExperience> list = this.getUserExperience(companyId, Collections.singleton(userId)).get(userId);
		
		boolean find = false;
		if (list != null) {
			for (UserProtos.UserExperience exp : list) {
				if (exp.getExperienceId() == experienceId) {
					find = true;
					if (exp.getExperienceContent().equals(experienceContent)) {
						return UpdateUserExperienceResponse.newBuilder()
								.setResult(UpdateUserExperienceResponse.Result.SUCC)
								.setExperience(request.getExperience())
								.build();
					}
					break;
				}
	 		}
		}
		
		if (!find) {
			return UpdateUserExperienceResponse.newBuilder()
					.setResult(UpdateUserExperienceResponse.Result.FAIL_EXPERIENCE_ID_INVALID)
					.setFailText("项目经验id没找到")
					.build();
		}
		
		List<UserProtos.UserExperience> experienceList;
		Connection dbConn = null;
		try {
			dbConn = hikariDataSource.getConnection();
			ExperienceDB.updateUserExperience(dbConn, companyId, userId, experienceId, experienceContent);
			experienceList = ExperienceDB.getUserExperience(dbConn, companyId, Collections.singleton(userId)).get(userId);
		} catch (SQLException e) {
			throw new RuntimeException("db fail", e);
		} finally {
			DBUtil.closeQuietly(dbConn);
		}
		
		Jedis jedis = jedisPool.getResource();
		try {
			ExperienceCache.setUserExperience(jedis, companyId, Collections.<Long, List<UserProtos.UserExperience>>singletonMap(userId, experienceList));
		} finally {
			jedis.close();
		}
		
		return UpdateUserExperienceResponse.newBuilder()
				.setResult(UpdateUserExperienceResponse.Result.SUCC)
				.setExperience(request.getExperience())
				.build();
	}

	public DeleteUserExperienceResponse deleteUserExperience(RequestHead head, DeleteUserExperienceRequest request) {
		final long companyId = head.getSession().getCompanyId();
		final long userId = head.getSession().getUserId();
		List<UserProtos.UserExperience> list = this.getUserExperience(companyId, Collections.singleton(userId)).get(userId);
		
		boolean find = false;
		if (list != null) {
			for (UserProtos.UserExperience exp : list) {
				if (exp.getExperienceId() == request.getExperienceId()) {
					find = true;
					break;
				}
	 		}
		}
		
		if (!find) {
			return DeleteUserExperienceResponse.newBuilder()
					.setResult(DeleteUserExperienceResponse.Result.FAIL_EXPERIENCE_ID_INVALID)
					.setFailText("项目经验id没找到")
					.build();
		}
		
		List<UserProtos.UserExperience> experienceList;
		Connection dbConn = null;
		try {
			dbConn = hikariDataSource.getConnection();
			ExperienceDB.deleteUserExperience(dbConn, companyId, userId, request.getExperienceId());
			experienceList = ExperienceDB.getUserExperience(dbConn, companyId, Collections.singleton(userId)).get(userId);
		} catch (SQLException e) {
			throw new RuntimeException("db fail", e);
		} finally {
			DBUtil.closeQuietly(dbConn);
		}
		
		Jedis jedis = jedisPool.getResource();
		try {
			ExperienceCache.setUserExperience(jedis, companyId, Collections.<Long, List<UserProtos.UserExperience>>singletonMap(userId, experienceList));
		} finally {
			jedis.close();
		}
		
		return DeleteUserExperienceResponse.newBuilder()
				.setResult(DeleteUserExperienceResponse.Result.SUCC)
				.build();
	}
	
}
