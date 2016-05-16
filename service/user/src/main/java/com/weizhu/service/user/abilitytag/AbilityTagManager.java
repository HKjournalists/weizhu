package com.weizhu.service.user.abilitytag;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;

import javax.annotation.Nullable;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.weizhu.common.db.DBUtil;
import com.weizhu.proto.UserProtos;
import com.weizhu.proto.AdminProtos.AdminHead;
import com.weizhu.proto.AdminUserProtos.SetUserAbilityTagRequest;
import com.weizhu.proto.AdminUserProtos.SetUserAbilityTagResponse;
import com.weizhu.proto.UserProtos.CreateAbilityTagRequest;
import com.weizhu.proto.UserProtos.CreateAbilityTagResponse;
import com.weizhu.proto.UserProtos.DeleteAbilityTagRequest;
import com.weizhu.proto.UserProtos.TagUserAbilityRequest;
import com.weizhu.proto.UserProtos.TagUserAbilityResponse;
import com.weizhu.proto.WeizhuProtos.RequestHead;
import com.zaxxer.hikari.HikariDataSource;

@Singleton
public class AbilityTagManager {

	private final HikariDataSource hikariDataSource;
	private final JedisPool jedisPool;
	
	@Inject
	public AbilityTagManager(HikariDataSource hikariDataSource, JedisPool jedisPool) {
		this.hikariDataSource = hikariDataSource;
		this.jedisPool = jedisPool;
	}
	
	public Map<Long, List<UserProtos.UserAbilityTag>> getUserAbilityTag(long companyId, Collection<Long> userIds, @Nullable Long tagUserId) {
		
		Map<Long, List<UserProtos.UserAbilityTag>> resultMap = new HashMap<Long, List<UserProtos.UserAbilityTag>>(userIds.size());
		
		Set<Long> noCacheUserIdSet = new TreeSet<Long>();
		Jedis jedis = jedisPool.getResource();
		try {
			resultMap.putAll(AbilityTagCache.getAbilityTag(jedis, companyId, userIds, noCacheUserIdSet));
		} finally {
			jedis.close();
		}
		
		if (!noCacheUserIdSet.isEmpty()) {
			Map<Long, List<UserProtos.UserAbilityTag>> noCacheAbilityTagMap;
			
			Connection dbConn = null;
			try {
				dbConn = hikariDataSource.getConnection();
				noCacheAbilityTagMap = AbilityTagDB.getUserAbilityTagList(dbConn, companyId, noCacheUserIdSet);
			} catch (SQLException e) {
				throw new RuntimeException("db fail", e);
			} finally {
				DBUtil.closeQuietly(dbConn);
			}
			
			jedis = jedisPool.getResource();
			try {
				AbilityTagCache.setAbilityTag(jedis, companyId, noCacheUserIdSet, noCacheAbilityTagMap);
			} finally {
				jedis.close();
			}
			
			resultMap.putAll(noCacheAbilityTagMap);
		}
		
		if (tagUserId == null || resultMap.isEmpty()) {
			return resultMap;
		}
			
		Map<Long, Set<String>> tagNameSetMap;
		Connection dbConn = null;
		try {
			dbConn = hikariDataSource.getConnection();
			tagNameSetMap = AbilityTagDB.getUserAbilityTagNameSet(dbConn, companyId, resultMap.keySet(), tagUserId);
		} catch (SQLException e) {
			throw new RuntimeException("db fail", e);
		} finally {
			DBUtil.closeQuietly(dbConn);
		}
		
		Map<Long, List<UserProtos.UserAbilityTag>> newResultMap = new HashMap<Long, List<UserProtos.UserAbilityTag>>(resultMap.size());
		
		UserProtos.UserAbilityTag.Builder tmpTagBuilder = UserProtos.UserAbilityTag.newBuilder();
		for (Entry<Long, List<UserProtos.UserAbilityTag>> entry : resultMap.entrySet()) {
			
			Set<String> tagNameSet = tagNameSetMap.get(entry.getKey());
			if (tagNameSet == null || tagNameSet.isEmpty()) {
				newResultMap.put(entry.getKey(), entry.getValue());
			} else {
				List<UserProtos.UserAbilityTag> newList = new ArrayList<UserProtos.UserAbilityTag>(entry.getValue().size());
				for (UserProtos.UserAbilityTag tag : entry.getValue()) {
					if (tagNameSet.contains(tag.getTagName())) {
						tmpTagBuilder.clear();
						newList.add(tmpTagBuilder.mergeFrom(tag)
								.setIsTag(true)
								.build());
					} else {
						newList.add(tag);
					}
				}
				newResultMap.put(entry.getKey(), newList);
			}
		}
		
		return newResultMap;
	}
	
	public SetUserAbilityTagResponse setUserAbilityTag(AdminHead head, SetUserAbilityTagRequest request) {
		if (!head.hasCompanyId()) {
			return SetUserAbilityTagResponse.newBuilder()
					.setResult(SetUserAbilityTagResponse.Result.FAIL_UNKNOWN)
					.setFailText("company_id 参数未填")
					.build();
		}
		final long companyId = head.getCompanyId();
		final long userId = request.getUserId();
		final Set<String> newTagNameSet = new TreeSet<String>();
		for (String tagName : request.getTagNameList()) {
			tagName = tagName.trim();
			if (tagName.isEmpty()) {
				continue;
			}
			if (tagName.length() > 10 ) {
				return SetUserAbilityTagResponse.newBuilder()
						.setResult(SetUserAbilityTagResponse.Result.FAIL_TAG_NAME_INVALID)
						.setFailText("标签最多10个字")
						.build();
			}
			newTagNameSet.add(tagName);
		}
		
		if (newTagNameSet.size() > 50) {
			return SetUserAbilityTagResponse.newBuilder()
					.setResult(SetUserAbilityTagResponse.Result.FAIL_TAG_NUM_LIMIT)
					.setFailText("标签最多50个")
					.build();
		}
		
		List<UserProtos.UserAbilityTag> list = this.getUserAbilityTag(companyId, Collections.singleton(userId), null).get(userId);
		
		final Set<String> oldTagNameSet;
		if (list == null) {
			oldTagNameSet = Collections.emptySet();
		} else {
			oldTagNameSet = new TreeSet<String>();
			for (UserProtos.UserAbilityTag tag : list) {
				oldTagNameSet.add(tag.getTagName());
			}
		}
		
		final int createTime = (int) (System.currentTimeMillis() / 1000L);
		
		Map<Long, List<UserProtos.UserAbilityTag>> abilityTagMap;
		Connection dbConn = null;
		try {
			dbConn = hikariDataSource.getConnection();
			AbilityTagDB.updateAbilityTag(dbConn, companyId, 
					Collections.<Long, Set<String>>singletonMap(userId, oldTagNameSet), 
					Collections.<Long, Set<String>>singletonMap(userId, newTagNameSet), 
					null, createTime);
			
			abilityTagMap = AbilityTagDB.getUserAbilityTagList(dbConn, companyId, Collections.singleton(userId));
		} catch (SQLException e) {
			throw new RuntimeException("db fail", e);
		} finally {
			DBUtil.closeQuietly(dbConn);
		}
		
		Jedis jedis = jedisPool.getResource();
		try {
			AbilityTagCache.setAbilityTag(jedis, companyId, Collections.singleton(userId), abilityTagMap);
		} finally {
			jedis.close();
		}
		
		return SetUserAbilityTagResponse.newBuilder()
				.setResult(SetUserAbilityTagResponse.Result.SUCC)
				.build();
	}
	
	public TagUserAbilityResponse tagUserAbility(RequestHead head, TagUserAbilityRequest request) {
		final long companyId = head.getSession().getCompanyId();
		final long userId = request.getUserId();
		if (userId == head.getSession().getUserId()) {
			return TagUserAbilityResponse.newBuilder()
					.setResult(TagUserAbilityResponse.Result.FAIL_TAG_SELF)
					.setFailText("不能给自己打标签")
					.build();
		}
		
		final String tagName = request.getTagName().trim();
		if (tagName.isEmpty()) {
			return TagUserAbilityResponse.newBuilder()
					.setResult(TagUserAbilityResponse.Result.FAIL_TAG_NOT_EXIST)
					.setFailText("标签名称为空")
					.build();
		}
		if (tagName.length() > 191 ) {
			return TagUserAbilityResponse.newBuilder()
					.setResult(TagUserAbilityResponse.Result.FAIL_TAG_NOT_EXIST)
					.setFailText("标签不存在")
					.build();
		}
		
		List<UserProtos.UserAbilityTag> list = this.getUserAbilityTag(companyId, Collections.singleton(userId), head.getSession().getUserId()).get(userId);
		
		boolean isExsitTag = false;
		
		if (list != null) {
			for (UserProtos.UserAbilityTag tag : list) {
				if (tag.getTagName().equals(tagName)) {
					isExsitTag = true;
					if (tag.getIsTag() == request.getIsTag()) {
						return TagUserAbilityResponse.newBuilder()
								.setResult(TagUserAbilityResponse.Result.SUCC)
								.build();
					} else {
						break;
					}
				}
			}
		}
		
		if (!isExsitTag) {
			return TagUserAbilityResponse.newBuilder()
					.setResult(TagUserAbilityResponse.Result.FAIL_TAG_NOT_EXIST)
					.setFailText("标签不存在")
					.build();
		}
		
		final int tagTime = (int) (System.currentTimeMillis() / 1000L);
		
		Map<Long, List<UserProtos.UserAbilityTag>> abilityTagMap;
		Connection dbConn = null;
		try {
			dbConn = hikariDataSource.getConnection();
			if (request.getIsTag()) {
				AbilityTagDB.insertUserAbilityTag(dbConn, companyId, userId, tagName, head.getSession().getUserId(), tagTime);
			} else {
				AbilityTagDB.deleteUserAbilityTag(dbConn, companyId, userId, tagName, head.getSession().getUserId());
			}
			abilityTagMap = AbilityTagDB.getUserAbilityTagList(dbConn, companyId, Collections.singleton(userId));
		} catch (SQLException e) {
			throw new RuntimeException("db fail", e);
		} finally {
			DBUtil.closeQuietly(dbConn);
		}
		
		Jedis jedis = jedisPool.getResource();
		try {
			AbilityTagCache.setAbilityTag(jedis, companyId, Collections.singleton(userId), abilityTagMap);
		} finally {
			jedis.close();
		}
		
		return TagUserAbilityResponse.newBuilder()
				.setResult(TagUserAbilityResponse.Result.SUCC)
				.build();
	}
	
	public CreateAbilityTagResponse createAbilityTag(RequestHead head, CreateAbilityTagRequest request) {
		final long companyId = head.getSession().getCompanyId();
		final long userId = request.getUserId();
		final String tagName = request.getTagName().trim();
		if (tagName.isEmpty()) {
			return CreateAbilityTagResponse.newBuilder()
					.setResult(CreateAbilityTagResponse.Result.FAIL_TAG_NAME_INVALID)
					.setFailText("标签名称为空")
					.build();
		}
		if (tagName.length() > 10 ) {
			return CreateAbilityTagResponse.newBuilder()
					.setResult(CreateAbilityTagResponse.Result.FAIL_TAG_NAME_INVALID)
					.setFailText("标签最多10个字")
					.build();
		}
		
		List<UserProtos.UserAbilityTag> list = this.getUserAbilityTag(companyId, Collections.singleton(userId), null).get(userId);
		
		if (list != null) {
			for (UserProtos.UserAbilityTag tag : list) {
				if (tag.getTagName().equals(tagName)) {
					return CreateAbilityTagResponse.newBuilder()
							.setResult(CreateAbilityTagResponse.Result.FAIL_TAG_EXIST)
							.setFailText("标签已存在")
							.build();
				}
			}
			
			if (list.size() >= 50) {
				return CreateAbilityTagResponse.newBuilder()
						.setResult(CreateAbilityTagResponse.Result.FAIL_TAG_NUM_LIMIT)
						.setFailText("标签最多50个")
						.build();
			}
		}
		
		final int createTime = (int) (System.currentTimeMillis() / 1000L);
		
		Map<Long, List<UserProtos.UserAbilityTag>> abilityTagMap;
		Connection dbConn = null;
		try {
			dbConn = hikariDataSource.getConnection();
			AbilityTagDB.insertAbilityTag(dbConn, companyId, userId, tagName, head.getSession().getUserId(), createTime);
			abilityTagMap = AbilityTagDB.getUserAbilityTagList(dbConn, companyId, Collections.singleton(userId));
		} catch (SQLException e) {
			throw new RuntimeException("db fail", e);
		} finally {
			DBUtil.closeQuietly(dbConn);
		}
		
		Jedis jedis = jedisPool.getResource();
		try {
			AbilityTagCache.setAbilityTag(jedis, companyId, Collections.singleton(userId), abilityTagMap);
		} finally {
			jedis.close();
		}
		
		return CreateAbilityTagResponse.newBuilder()
				.setResult(CreateAbilityTagResponse.Result.SUCC)
				.build();
	}

	public void deleteAbilityTag(RequestHead head, DeleteAbilityTagRequest request) {
		if (request.getTagNameCount() <= 0) {
			return ;
		}
		
		final long companyId = head.getSession().getCompanyId();
		final long userId = head.getSession().getUserId();
		
		List<UserProtos.UserAbilityTag> list = this.getUserAbilityTag(companyId, Collections.singleton(userId), null).get(userId);
		
		Set<String> deleteTagNameSet = new TreeSet<String>();
		if (list != null) {
			for (UserProtos.UserAbilityTag tag : list) {
				for (int i=0; i<request.getTagNameCount(); ++i) {
					if (tag.getTagName().equals(request.getTagName(i))) {
						deleteTagNameSet.add(tag.getTagName());
						break;
					}
				}
			}
		}
		
		if (deleteTagNameSet.isEmpty()) {
			return;
		}
		
		Map<Long, List<UserProtos.UserAbilityTag>> abilityTagMap;
		Connection dbConn = null;
		try {
			dbConn = hikariDataSource.getConnection();
			AbilityTagDB.deleteAbilityTag(dbConn, companyId, userId, deleteTagNameSet);
			abilityTagMap = AbilityTagDB.getUserAbilityTagList(dbConn, companyId, Collections.singleton(userId));
		} catch (SQLException e) {
			throw new RuntimeException("db fail", e);
		} finally {
			DBUtil.closeQuietly(dbConn);
		}
		
		Jedis jedis = jedisPool.getResource();
		try {
			AbilityTagCache.setAbilityTag(jedis, companyId, Collections.singleton(userId), abilityTagMap);
		} finally {
			jedis.close();
		}
	}
	
	public List<Long> getAbilityTagUserId(long companyId, Set<String> tagNameSet, @Nullable Boolean isExpert) {
		if (tagNameSet.isEmpty()) {
			return Collections.emptyList();
		}
		
		Connection dbConn = null;
		try {
			dbConn = hikariDataSource.getConnection();
			return AbilityTagDB.getAbilityTagUserIdList(dbConn, 
					companyId, tagNameSet, isExpert);
		} catch (SQLException e) {
			throw new RuntimeException("db fail", e);
		} finally {
			DBUtil.closeQuietly(dbConn);
		}
	}
	
}
