package com.weizhu.service.component.score;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import javax.annotation.Nullable;

import com.google.common.collect.Sets;
import com.google.inject.Inject;
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import com.weizhu.common.db.DBUtil;
import com.weizhu.common.utils.DataPage;
import com.weizhu.proto.ComponentProtos;
import com.weizhu.proto.AdminComponentProtos;
import com.weizhu.proto.AdminComponentProtos.CreateScoreRequest;
import com.weizhu.proto.AdminComponentProtos.CreateScoreResponse;
import com.weizhu.proto.AdminComponentProtos.GetScoreByIdRequest;
import com.weizhu.proto.AdminComponentProtos.GetScoreListRequest;
import com.weizhu.proto.AdminComponentProtos.GetScoreListResponse;
import com.weizhu.proto.AdminComponentProtos.UpdateScoreRequest;
import com.weizhu.proto.AdminComponentProtos.UpdateScoreResponse;
import com.weizhu.proto.AdminComponentProtos.UpdateScoreStateRequest;
import com.weizhu.proto.AdminComponentProtos.UpdateScoreStateResponse;
import com.weizhu.proto.AdminProtos.AdminHead;
import com.weizhu.proto.ComponentProtos.GetScoreByIdResponse;
import com.weizhu.proto.ComponentProtos.GetScoreUserListRequest;
import com.weizhu.proto.ComponentProtos.GetScoreUserListResponse;
import com.weizhu.proto.ComponentProtos.GetUserScoreListRequest;
import com.weizhu.proto.ComponentProtos.GetUserScoreListResponse;
import com.weizhu.proto.ComponentProtos.ScoreRequest;
import com.weizhu.proto.ComponentProtos.ScoreResponse;
import com.weizhu.proto.WeizhuProtos.RequestHead;
import com.zaxxer.hikari.HikariDataSource;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

public class ScoreManager {

	private final HikariDataSource hikariDataSource;
	private final JedisPool jedisPool;
	
	@Inject
	public ScoreManager(HikariDataSource hikariDataSource, JedisPool jedisPool){
		this.hikariDataSource = hikariDataSource;
		this.jedisPool = jedisPool;
	}
	
	/**
	 * 先从缓存中获取score并将没有获取到的score的id存放到一个collection中，然后根据这些scoreids去DB中
	 * 查询，将从DB中查询到的score记录放到缓存中，最后将从DB中查到的记录附加到第一次从缓存中查到的score记录后，然后根据states将
	 * 查询到的记录筛选一下返回符合条件的记录
	 * @param hikariDataSource
	 * @param jedisPool
	 * @param companyId
	 * @param scoreIds
	 * @param states
	 * @return
	 */
	public  Map<Integer,ComponentProtos.Score> getScore(long companyId,
			Collection<Integer> scoreIds,@Nullable Collection<ComponentProtos.State> states
			){
		if (scoreIds.isEmpty() || (states != null && states.isEmpty())) {
			return Collections.emptyMap();
		}
		
		Map<Integer,ComponentProtos.Score> scoreMap = new TreeMap<Integer,ComponentProtos.Score>();
		
		Set<Integer> noCacheScoreIds = new TreeSet<Integer>();//用来保存没有从缓存中查到的scoreId
		Jedis jedis = jedisPool.getResource();
		try {
			//从缓存中查询score，将没有在缓存中查到的score的scoreid保存到noCacheScoreIds中，将从缓存中查到的score放到scoreMap中
			scoreMap.putAll(ScoreCache.getScore(jedis, companyId, scoreIds, noCacheScoreIds));
		} finally{
			jedis.close();
		}
		//如果有没有从缓存中查到的score就从数据库中查询
		if(!noCacheScoreIds.isEmpty()){
			Map<Integer,ComponentProtos.Score> noCacheScoreMap;
			Connection dbConn = null;
			try {
				dbConn = hikariDataSource.getConnection();
				noCacheScoreMap = ScoreDB.getScoreById(dbConn, companyId, noCacheScoreIds);//调用DB从数据库中查询
			} catch (SQLException e) {
				throw new RuntimeException("db failed");
			}finally{
				DBUtil.closeQuietly(dbConn);
			}
			
			jedis = jedisPool.getResource();
			try {
				ScoreCache.setScore(jedis, companyId, noCacheScoreIds, noCacheScoreMap);//将从数据库中查询到的记录放入缓存中
			}finally{
				jedis.close();
			}
			scoreMap.putAll(noCacheScoreMap);//将从数据库中查询到的记录放到scoreMap中
		}
		
		if(states == null){
			return scoreMap;
		}
		
		Iterator<ComponentProtos.Score> it = scoreMap.values().iterator();
		while(it.hasNext()){
			ComponentProtos.Score score = it.next();
			if(!states.contains(score.getState())){
				it.remove();
			}
		}
		
		return scoreMap;
	}
	
	/**
	 * 先从缓存中查询并将没有查询到的scoreCount的scoreids保存到一个collection，然后根据这个collection去DB中查询，
	 * 将查询到的结果先放到缓存中然后将查询到的结果附加到第一次从缓存中查询到的结果之后，最后返回完整的查询结果。
	 * @param hikariDataSource
	 * @param jedisPool
	 * @param companyId
	 * @param scoreIds
	 * @return
	 */
	public  Map<Integer,ComponentProtos.ScoreCount> getScoreCount( long companyId,
			Collection<Integer> scoreIds){
		if (scoreIds.isEmpty()) {
			return Collections.emptyMap();
		}
		
		Map<Integer,ComponentProtos.ScoreCount> scoreCountMap = new TreeMap<Integer,ComponentProtos.ScoreCount>();
		
		Set<Integer> noCacheSoreIdSet = new TreeSet<Integer>();
		Jedis jedis = jedisPool.getResource();
		try {
			scoreCountMap.putAll(ScoreCache.getScoreCount(jedis, companyId, scoreIds, noCacheSoreIdSet));
		}finally{
			jedis.close();
		}
		if(!noCacheSoreIdSet.isEmpty()){
			Map<Integer,ComponentProtos.ScoreCount> noCacheScoreCountMap;
			Connection dbConn = null;
			try {
				dbConn = hikariDataSource.getConnection();
				noCacheScoreCountMap = ScoreDB.getScoreCount(dbConn, companyId, scoreIds);
			} catch (SQLException e) {
				throw new RuntimeException("db failed");
			} finally {
				DBUtil.closeQuietly(dbConn);
			}
			
			jedis = jedisPool.getResource();
			try {
				ScoreCache.setScoreCount(jedis, companyId, scoreIds, noCacheScoreCountMap);
			}finally{
				jedis.close();
			}
			scoreCountMap.putAll(noCacheScoreCountMap);
		}
		
		return scoreCountMap;
	}
	
	public GetScoreByIdResponse getScoreById (RequestHead head, Collection<Integer> scoreIds, @Nullable Collection<ComponentProtos.State> states){
		final long companyId = head.getSession().getCompanyId();
		final long userId = head.getSession().getUserId();
		
		//获取score
		Map<Integer,ComponentProtos.Score> scoreMap = this.getScore(companyId, scoreIds, states);
		GetScoreByIdResponse.Builder responseBuilder = GetScoreByIdResponse.newBuilder();
		//将score赋值给response
		responseBuilder.addAllScore(scoreMap.values());
		
		//获取scoreCount
		Map<Integer,ComponentProtos.ScoreCount> scoreCountMap = this.getScoreCount(companyId, scoreMap.keySet()); // 这里就不用request.getScoreIdList()了，而是用上面查询到的在缓存和DB中存在的scoreIds
		//将scoreCount赋值给response
		for(ComponentProtos.ScoreCount scoreCount : scoreCountMap.values()){
			responseBuilder.addRefScoreCount(scoreCount);
		}
		responseBuilder.addAllRefScoreCount(scoreCountMap.values());
		
		//获取scoreUser(直接从DB中获取)
		Connection conn = null;
		try {
			conn = hikariDataSource.getConnection();
			Map<Integer, ComponentProtos.ScoreUser> scoreUserMap = ScoreDB.getScoreUser(conn, companyId, 
					userId,scoreMap.keySet()); // 这里就不用request.getScoreIdList()了，而是用上面查询到的在缓存和DB中存在的scoreIds
			//将scoreUser放入response
			responseBuilder.addAllRefScoreUser(scoreUserMap.values());
		} catch (SQLException e) {
			throw new RuntimeException("db failed");
		}finally{
			DBUtil.closeQuietly(conn);
		}
		return responseBuilder.build();
	}
	
	public GetScoreUserListResponse getScoreUserList(long companyId, GetScoreUserListRequest request, @Nullable Collection<ComponentProtos.State> states){
		final int scoreId = request.getScoreId();
		final int size = request.getSize() < 0 ? 0 : request.getSize() > 100 ? 100 : request.getSize(); //每页显示的条数最多100条
		final ComponentProtos.ScoreUser lastScoreUser;//上一页最后一个scoreUser
		
		//为上一页最后一个scoreUser赋值
		if (request.hasOffsetIndex() && !request.getOffsetIndex().isEmpty()) {
			ComponentProtos.ScoreUser tmp = null;
			try {
				tmp = ComponentProtos.ScoreUser.parseFrom(request.getOffsetIndex());
			} catch (InvalidProtocolBufferException e) {
				tmp = null;
			}
			
			if(tmp != null && tmp.getScoreId() == scoreId){
				lastScoreUser = tmp;
			}else{
				lastScoreUser = null;
			}
		}else{
			lastScoreUser = null;
		}
		
		//判断score 基础信息是否存在
		Map<Integer,ComponentProtos.Score> scoreMap = this.getScore(companyId, Collections.singleton(scoreId), states);
		if(scoreMap.isEmpty()){
			return GetScoreUserListResponse.newBuilder().build();
		}
		
		//定义保存scoreUser的list
		List<ComponentProtos.ScoreUser> list;
		Connection conn = null;
		try {
			conn = hikariDataSource.getConnection();
			//从DB中查询scoreUserList，这里的size+1 是为了多获取一个从而用来判断是否还有更多和下一页
			list = ScoreDB.getScoreUserList(conn, companyId, scoreId, size+1, lastScoreUser);
		} catch (SQLException e) {
			throw new RuntimeException("db failed", e);
		}finally{
			DBUtil.closeQuietly(conn);
		}
		
		//判断是否有更多
		final boolean hasMore;
		if (list.size() > size) {
			hasMore = true;
			list = list.subList(0, size);//获取本页的记录
		} else {
			hasMore = false;
		}
		//定义response
		GetScoreUserListResponse.Builder responseBuilder = GetScoreUserListResponse.newBuilder();
		responseBuilder.addAllScoreUser(list);//将list放入response
		responseBuilder.setHasMore(hasMore);//将hasMore放入response
		//为offsetindex赋值
		if (list.isEmpty()) {
			responseBuilder.setOffsetIndex(request.hasOffsetIndex() ? request.getOffsetIndex() : ByteString.EMPTY);
		} else {
			responseBuilder.setOffsetIndex(list.get(list.size() - 1).toByteString());
		}
		return responseBuilder.build();
	}
	
	public GetUserScoreListResponse getUserScoreList(long companyId, GetUserScoreListRequest request, @Nullable Collection<ComponentProtos.State> states){
		//获取userId
		final long userId = request.getUserId(); // 这里的userId要从request中获取
		//获取size
		final int size = request.getSize() < 0 ? 0 : request.getSize() > 100 ? 100 : request.getSize(); //每页显示的条数
		final ComponentProtos.ScoreUser lastScoreUser;//用来保存上一页最后一个scoreUser
		
		//获取上一页最后一个记录
		if (request.hasOffsetIndex() && !request.getOffsetIndex().isEmpty()) {
			ComponentProtos.ScoreUser tmp = null;
			try {
				tmp = ComponentProtos.ScoreUser.parseFrom(request.getOffsetIndex());
			} catch (InvalidProtocolBufferException e) {
				tmp = null;
			}
			
			if(tmp != null && tmp.getUserId() == userId){
				lastScoreUser = tmp;
			}else{
				lastScoreUser = null;
			}
		}else{
			lastScoreUser = null;
		}
		
		//定义保存scoreUser的list
		List<ComponentProtos.ScoreUser> list;
		Connection conn = null;
		try {
			conn = hikariDataSource.getConnection();
			//从DB中查询scoreUserList，这里的size+1 是为了多获取一个从而用来判断是否还有更多和下一页
			list = ScoreDB.getUserScoreList(conn, companyId, userId, size+1, lastScoreUser);
		} catch (SQLException e) {
			throw new RuntimeException("db failed", e);
		}finally{
			DBUtil.closeQuietly(conn);
		}
		
		//判断是否有更多
		final boolean hasMore;
		if (list.size() > size) {
			hasMore = true;
			list = list.subList(0, size);//获取本页的记录
		} else {
			hasMore = false;
		}
		
		//定义response
		GetUserScoreListResponse.Builder responseBuilder = GetUserScoreListResponse.newBuilder();
		responseBuilder.addAllScoreUser(list);//将list放入response
		responseBuilder.setHasMore(hasMore);//将hasMore放入response
		//为offsetindex赋值
		if (list.isEmpty()) {
			responseBuilder.setOffsetIndex(request.hasOffsetIndex() ? request.getOffsetIndex() : ByteString.EMPTY);
		} else {
			responseBuilder.setOffsetIndex(list.get(list.size() - 1).toByteString());
		}
		
		//获取对应的score信息
		Set<Integer> scoreIdSet = new TreeSet<Integer>();
		for(ComponentProtos.ScoreUser scoreUser : list){
			scoreIdSet.add(scoreUser.getScoreId());
		}
		//获取score
		Map<Integer,ComponentProtos.Score> scoreMap = this.getScore(companyId, scoreIdSet, states); 
		//将scoreList放入response
		responseBuilder.addAllRefScore(scoreMap.values());
		
		return responseBuilder.build();
	}
	
	public ScoreResponse score(RequestHead head, ScoreRequest request, @Nullable Collection<ComponentProtos.State> states){
		//获取companyId
		final long companyId = head.getSession().getCompanyId();
		//获取userId
		final long userId = head.getSession().getUserId();
		//获取scoreId
		final int scoreId = request.getScoreId();
		//获取scoreValue
		final int scoreValue = request.getScoreValue();
		
		//判断当前打分活动是否存在或是否还有效
		ComponentProtos.Score score = this.getScore(companyId, Collections.singleton(scoreId), states).get(scoreId);
		if(score == null){
			return ScoreResponse.newBuilder()
					.setResult(ScoreResponse.Result.FAIL_SCORE_NOT_EXSIT)
					.setFailText("您打分的项目不存在")
					.build();
		}
		
		//根据打分类型判断 scoreValue 是否正确 FIVE_STAR : 1-10
		if(ComponentProtos.Score.Type.FIVE_STAR.equals(score.getType())){
			if(scoreValue < 1 || scoreValue >10){
				return ScoreResponse.newBuilder()
						.setResult(ScoreResponse.Result.FAIL_VALUE_INVALID)
						.setFailText("您打分的分数不正确")
						.build();
			}
		}
		
		//判断该用户是否已经参加过该打分活动
		Connection conn = null;
		try {
			conn = hikariDataSource.getConnection();
			Map<Integer, ComponentProtos.ScoreUser> scoreUserMap = ScoreDB.getScoreUser(conn, companyId, userId, Collections.singleton(scoreId));
			if(!scoreUserMap.isEmpty()){
				return ScoreResponse.newBuilder()
						.setResult(ScoreResponse.Result.FAIL_IS_SCORED)
						.setFailText("您已经打过分了")
						.build();
			}
			
		} catch (SQLException e) {
			throw new RuntimeException("db failed", e);
		}finally{
			DBUtil.closeQuietly(conn);
		}
		
		//为scoreUser的各项属性赋值
		ComponentProtos.ScoreUser scoreUser = ComponentProtos.ScoreUser.newBuilder()
				.setScoreId(scoreId)
				.setScoreValue(scoreValue)
				.setUserId(userId)
				.setScoreTime((int) (System.currentTimeMillis() / 1000L))
				.build();
		
		//将打分结果存入数据库
		Connection dbConn = null;
		try {
			dbConn = hikariDataSource.getConnection();
			ScoreDB.insertScoreUser(dbConn, companyId, scoreUser);
		} catch (SQLException e) {
			throw new RuntimeException("db failed", e);
		}finally{
			DBUtil.closeQuietly(dbConn);
		}
		
		Jedis jedis = this.jedisPool.getResource();
		try {
			//清理scoreCount缓存
			ScoreCache.delScoreCount(jedis, companyId,  Collections.singleton(scoreId));
		}finally{
			jedis.close();
		}
		
		//为response赋值
		return ScoreResponse.newBuilder()
				.setResult(ScoreResponse.Result.SUCC).build();
	}
	
	public AdminComponentProtos.GetScoreByIdResponse getScoreById(AdminHead head, GetScoreByIdRequest request, @Nullable Collection<ComponentProtos.State> states){
		final long companyId = head.getCompanyId();
		
		if(request.getScoreIdCount() <= 0){
			return AdminComponentProtos.GetScoreByIdResponse.newBuilder().build();
		}
		
		//获取score
		Map<Integer,ComponentProtos.Score> scoreMap = this.getScore(companyId, new TreeSet<Integer>(request.getScoreIdList()), states);
		
		AdminComponentProtos.GetScoreByIdResponse.Builder responseBuilder = AdminComponentProtos.GetScoreByIdResponse.newBuilder();
		//将score赋值给response
		responseBuilder.addAllScore(scoreMap.values());
		
		//获取scoreCount
		Map<Integer,ComponentProtos.ScoreCount> scoreCountMap = this.getScoreCount(companyId, new TreeSet<Integer>(request.getScoreIdList()));
		//将scoreCount赋值给response
		responseBuilder.addAllRefScoreCount(scoreCountMap.values());
		
		return responseBuilder.build();
	}
	
	public GetScoreListResponse getScoreList(long companyId, GetScoreListRequest request, @Nullable Collection<ComponentProtos.State> states){
		final int start = request.getStart() < 0 ? 0 : request.getStart();
		final int length = request.getLength() < 0 ? 0 : request.getLength() > 100 ? 100 : request.getLength(); //每页显示的条数
		final ComponentProtos.State state = request.hasState() ? request.getState() : null;
		
		DataPage<Integer> scorePage ;
		Connection dbConn = null;
		try {
			dbConn = hikariDataSource.getConnection();
			scorePage = ScoreDB.getScoreIdList(dbConn, companyId, start, length, state, null, states);
		} catch (SQLException e) {
			throw new RuntimeException("db fail!", e);
		} finally {
			DBUtil.closeQuietly(dbConn);
		}
		
		Map<Integer, ComponentProtos.Score> scoreMap = this.getScore(companyId, scorePage.dataList(), states);
		Map<Integer, ComponentProtos.ScoreCount> scoreCountMap = this.getScoreCount(companyId, scorePage.dataList());
		
		GetScoreListResponse.Builder responsBuilder = GetScoreListResponse.newBuilder();
		for(ComponentProtos.Score score : scoreMap.values()){
			responsBuilder.addScore(score);
		}
		responsBuilder.addAllRefScoreCount(scoreCountMap.values());
		responsBuilder.setTotalSize(scorePage.totalSize());
		responsBuilder.setFilteredSize(scorePage.filteredSize());
		return responsBuilder.build();
	}
	
	public CreateScoreResponse createScore (AdminHead head, CreateScoreRequest request){
		if (!head.hasCompanyId()) {
			return CreateScoreResponse.newBuilder()
					.setResult(CreateScoreResponse.Result.FAIL_UNKNOWN)
					.setFailText("company_id 参数为空")
					.build();
		}
		final long companyId = head.getCompanyId();
		final String scoreName = request.getScoreName();
		
		if(scoreName.isEmpty()){
			return CreateScoreResponse.newBuilder()
					.setResult(CreateScoreResponse.Result.FAIL_UNKNOWN)
					.setFailText("score_name不能为空！")
					.build();
		}
		if(scoreName.length() > 191){
			return CreateScoreResponse.newBuilder()
					.setResult(CreateScoreResponse.Result.FAIL_UNKNOWN)
					.setFailText("score_name长度超出范围！")
					.build();
		}
		final String imageName = request.hasImageName() ? request.getImageName() : null;
		if(imageName != null && imageName.length() > 191){
			return CreateScoreResponse.newBuilder()
					.setResult(CreateScoreResponse.Result.FAIL_UNKNOWN)
					.setFailText("image_name长度超出范围！")
					.build();
		}
		if(!request.hasType()){
			return CreateScoreResponse.newBuilder()
					.setResult(CreateScoreResponse.Result.FAIL_UNKNOWN)
					.setFailText("type 参数为空")
					.build();
		}
		final ComponentProtos.Score.Type type = request.getType();
		if(!request.hasResultView()){
			return CreateScoreResponse.newBuilder()
					.setResult(CreateScoreResponse.Result.FAIL_UNKNOWN)
					.setFailText("result_view 参数为空")
					.build();
		}
		final ComponentProtos.Score.ResultView resultView = request.getResultView();
		final Integer startTime = request.hasStartTime() ? request.getStartTime() : null; 
		final Integer endTime = request.hasEndTime() ? request.getEndTime() : null;
		
		if(startTime != null && endTime != null && startTime >= endTime){
			return CreateScoreResponse.newBuilder()
					.setResult(CreateScoreResponse.Result.FAIL_UNKNOWN)
					.setFailText("结束时间不能小于开始时间！")
					.build();
		}
		final Integer allowModelId = request.hasAllowModelId() ? request.getAllowModelId() : null;
		
		ComponentProtos.Score.Builder scoreBulider = ComponentProtos.Score.newBuilder();
		scoreBulider.setScoreId(0);
		scoreBulider.setScoreName(scoreName);
		if(imageName != null){
			scoreBulider.setImageName(imageName);
		}
		scoreBulider.setType(type);
		scoreBulider.setResultView(resultView);
		if(startTime != null){
			scoreBulider.setStartTime(startTime);
		}
		if(endTime != null){			
			scoreBulider.setEndTime(endTime);
		}
		if(allowModelId != null){
			scoreBulider.setAllowModelId(allowModelId);
		}
		scoreBulider.setState(ComponentProtos.State.NORMAL);
		scoreBulider.setCreateAdminId(head.getSession().getAdminId());
		scoreBulider.setCreateTime((int)(System.currentTimeMillis() / 1000L));
		
		final ComponentProtos.Score score = scoreBulider.build();
		
		Connection conn = null;
		Integer scoreId;
		try {
			conn = hikariDataSource.getConnection();
			scoreId = ScoreDB.insertScore(conn, companyId, Collections.singletonList(score)).get(0);
		} catch (SQLException e) {
			throw new RuntimeException("db fail!", e);
		} finally {
			DBUtil.closeQuietly(conn);
		}
		
		Jedis jedis = null;
		try {
			jedis = jedisPool.getResource();
			ScoreCache.delScore(jedis, companyId, Collections.singleton(scoreId));
			ScoreCache.delScoreCount(jedis, companyId, Collections.singleton(scoreId));
		} finally{
			jedis.close();
		}
		CreateScoreResponse.Builder responseBuilder = CreateScoreResponse.newBuilder();
		responseBuilder.setResult(CreateScoreResponse.Result.SUCC)
		.setScoreId(scoreId);
		return responseBuilder.build();
	}
	
	public UpdateScoreResponse updateScore(AdminHead head, UpdateScoreRequest request, @Nullable Collection<ComponentProtos.State> states){
		if (!head.hasCompanyId()) {
			return UpdateScoreResponse.newBuilder()
					.setResult(UpdateScoreResponse.Result.FAIL_UNKNOWN)
					.setFailText("company_id 参数为空")
					.build();
		}
		final long companyId = head.getCompanyId();
		if(!request.hasScoreId()){
			return UpdateScoreResponse.newBuilder()
					.setResult(UpdateScoreResponse.Result.FAIL_UNKNOWN)
					.setFailText("score_id 参数为空")
					.build();
		}
		final int scoreId = request.getScoreId();
		final String scoreName = request.getScoreName();
		if(scoreName.isEmpty()){
			return UpdateScoreResponse.newBuilder()
					.setResult(UpdateScoreResponse.Result.FAIL_UNKNOWN)
					.setFailText("score_name不能为空！")
					.build();
		}
		if(scoreName.length() > 191){
			return UpdateScoreResponse.newBuilder()
					.setResult(UpdateScoreResponse.Result.FAIL_UNKNOWN)
					.setFailText("score_name长度超出范围！")
					.build();
		}
		final String imageName = request.hasImageName() ? request.getImageName() : null;
		if(imageName != null && imageName.length() > 191){
			return UpdateScoreResponse.newBuilder()
					.setResult(UpdateScoreResponse.Result.FAIL_UNKNOWN)
					.setFailText("image_name长度超出范围！")
					.build();
		}
		if(!request.hasResultView()){
			return UpdateScoreResponse.newBuilder()
					.setResult(UpdateScoreResponse.Result.FAIL_UNKNOWN)
					.setFailText("result_view 参数为空")
					.build();
		}
		final ComponentProtos.Score.ResultView resultView = request.getResultView();
		final Integer startTime = request.hasStartTime() ? request.getStartTime() : null;
		final Integer endTime = request.hasEndTime() ? request.getEndTime() : null;
		
		if(startTime != null && endTime != null && startTime >= endTime){
			return UpdateScoreResponse.newBuilder()
					.setResult(UpdateScoreResponse.Result.FAIL_UNKNOWN)
					.setFailText("结束时间不能小于开始时间！")
					.build();
		}
		final Integer allowModelId = request.hasAllowModelId() ? request.getAllowModelId() : null;
		ComponentProtos.Score.Builder scoreBulider = ComponentProtos.Score.newBuilder();
		scoreBulider.setScoreId(scoreId);
		scoreBulider.setScoreName(scoreName);
		if(imageName != null){
			scoreBulider.setImageName(imageName);
		}
		scoreBulider.setResultView(resultView);
		if(startTime != null){
			scoreBulider.setStartTime(startTime);
		}
		if(endTime != null){			
			scoreBulider.setEndTime(endTime);
		}
		if(allowModelId != null){
			scoreBulider.setAllowModelId(allowModelId);
		}
		scoreBulider.setUpdateAdminId(head.getSession().getAdminId());
		scoreBulider.setUpdateTime((int)(System.currentTimeMillis() / 1000L));
		final ComponentProtos.Score score = scoreBulider.build();
		
		//先查询该打分是不是还是正常状态
		Map<Integer, ComponentProtos.Score> scoreMap = this.getScore(companyId, Collections.singleton(scoreId), states);
		if(scoreMap.isEmpty()){
			return UpdateScoreResponse.newBuilder()
					.setResult(UpdateScoreResponse.Result.FAIL_UNKNOWN)
					.setFailText("该打分已经被删除！")
					.build();
		}
		
		Connection conn = null;
		try {
			conn = hikariDataSource.getConnection();
			ScoreDB.updateScore(conn, companyId, score);
		} catch (SQLException e) {
			throw new RuntimeException("db fail!", e);
		} finally {
			DBUtil.closeQuietly(conn);
		}
		
		Jedis jedis = jedisPool.getResource();
		try {
			ScoreCache.delScore(jedis, companyId, Collections.singleton(scoreId));
		} finally{
			jedis.close();
		}
		UpdateScoreResponse.Builder responseBuilder = UpdateScoreResponse.newBuilder();
		responseBuilder.setResult(UpdateScoreResponse.Result.SUCC);
		return responseBuilder.build();
	}
	
	public UpdateScoreStateResponse updateScoreState(AdminHead head,
			UpdateScoreStateRequest request, @Nullable Collection<ComponentProtos.State> states) {
		if (!head.hasCompanyId()) {
			return UpdateScoreStateResponse.newBuilder()
					.setResult(UpdateScoreStateResponse.Result.FAIL_UNKNOWN)
					.setFailText("company_id 参数为空")
					.build();
		}
		final long companyId = head.getCompanyId();
		Set<Integer> scoreIds = Sets.newTreeSet(request.getScoreIdList());
		if(scoreIds.isEmpty()){
			return UpdateScoreStateResponse.newBuilder()
					.setResult(UpdateScoreStateResponse.Result.FAIL_UNKNOWN)
					.setFailText("没有要更新的打分活动")
					.build();
		}
		if(!request.hasState()){
			return UpdateScoreStateResponse.newBuilder()
					.setResult(UpdateScoreStateResponse.Result.FAIL_UNKNOWN)
					.setFailText("state 参数为空")
					.build();
		}
		final ComponentProtos.State state = request.getState();
		
		Map<Integer, ComponentProtos.Score> scoreMap = this.getScore(companyId, scoreIds, states);
		if(scoreMap.isEmpty()){
			return UpdateScoreStateResponse.newBuilder()
					.setResult(UpdateScoreStateResponse.Result.FAIL_UNKNOWN)
					.setFailText("修改项已被删除！")
					.build();
		}else{
			if(scoreMap.size() != scoreIds.size()){
				return UpdateScoreStateResponse.newBuilder()
						.setResult(UpdateScoreStateResponse.Result.FAIL_UNKNOWN)
						.setFailText("打分项已被删除！被删除的score_id："+ Sets.difference(scoreIds, scoreMap.keySet()))
						.build();
			}
		}
		
		Connection conn = null;
		try {
			conn = hikariDataSource.getConnection();
			ScoreDB.updateScoreState(conn, companyId, scoreIds, state);
		} catch (SQLException e) {
			throw new RuntimeException("db fail!", e);
		} finally {
			DBUtil.closeQuietly(conn);
		}
		
		Jedis jedis = null;
		try {
			jedis = jedisPool.getResource();
			ScoreCache.delScore(jedis, companyId, scoreIds);
		} finally{
			jedis.close();
		}
		UpdateScoreStateResponse.Builder responseBuilder = UpdateScoreStateResponse.newBuilder();
		responseBuilder.setResult(UpdateScoreStateResponse.Result.SUCC);
		return responseBuilder.build();
	}
}
