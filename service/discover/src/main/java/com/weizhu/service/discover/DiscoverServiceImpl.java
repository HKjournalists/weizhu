package com.weizhu.service.discover;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.Executor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import com.google.common.primitives.Ints;
import com.google.common.primitives.Longs;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.google.protobuf.InvalidProtocolBufferException;
import com.weizhu.common.db.DBUtil;
import com.weizhu.common.service.ServiceUtil;
import com.weizhu.proto.DiscoverProtos;
import com.weizhu.proto.DiscoverProtos.CommentItemRequest;
import com.weizhu.proto.DiscoverProtos.CommentItemResponse;
import com.weizhu.proto.DiscoverProtos.DeleteCommentRequest;
import com.weizhu.proto.DiscoverProtos.DeleteCommentResponse;
import com.weizhu.proto.DiscoverProtos.GetDiscoverHomeResponse;
import com.weizhu.proto.DiscoverProtos.GetItemByIdRequest;
import com.weizhu.proto.DiscoverProtos.GetItemByIdResponse;
import com.weizhu.proto.DiscoverProtos.GetItemCommentListRequest;
import com.weizhu.proto.DiscoverProtos.GetItemCommentListResponse;
import com.weizhu.proto.DiscoverProtos.GetItemContentRequest;
import com.weizhu.proto.DiscoverProtos.GetItemContentResponse;
import com.weizhu.proto.DiscoverProtos.GetItemListRequest;
import com.weizhu.proto.DiscoverProtos.GetItemListResponse;
import com.weizhu.proto.DiscoverProtos.GetItemPVRequest;
import com.weizhu.proto.DiscoverProtos.GetItemPVResponse;
import com.weizhu.proto.DiscoverProtos.GetItemScoreRequest;
import com.weizhu.proto.DiscoverProtos.GetItemScoreResponse;
import com.weizhu.proto.DiscoverProtos.GetModuleItemListRequest;
import com.weizhu.proto.DiscoverProtos.GetModuleItemListResponse;
import com.weizhu.proto.DiscoverProtos.ScoreItemRequest;
import com.weizhu.proto.DiscoverProtos.ScoreItemResponse;
import com.weizhu.proto.DiscoverProtos.SearchItemRequest;
import com.weizhu.proto.DiscoverProtos.SearchItemResponse;
import com.weizhu.proto.DiscoverService;
//import com.weizhu.proto.StatsProtos;
//import com.weizhu.proto.StatsService;
import com.weizhu.proto.WeizhuProtos.EmptyRequest;
import com.weizhu.proto.WeizhuProtos.EmptyResponse;
import com.weizhu.proto.WeizhuProtos.RequestHead;
import com.weizhu.proto.WeizhuProtos.SystemHead;
import com.zaxxer.hikari.HikariDataSource;

public class DiscoverServiceImpl implements DiscoverService {

	@SuppressWarnings("unused")
	private static final Logger logger = LoggerFactory.getLogger(DiscoverServiceImpl.class);
	
	private final HikariDataSource hikariDataSource;
	private final JedisPool jedisPool;
	private final Executor serviceExecutor;
	
	private final DiscoverExtends discoverExtends;
	
//	private final StatsService statsService;
	
	@Inject
	public DiscoverServiceImpl(HikariDataSource hikariDataSource, JedisPool jedisPool,
			@Named("service_executor") Executor serviceExecutor, 
			DiscoverExtends discoverExtends /*, StatsService statsService*/) {
		this.hikariDataSource = hikariDataSource;
		this.jedisPool = jedisPool;
		this.serviceExecutor = serviceExecutor;
		this.discoverExtends = discoverExtends;
//		this.statsService = statsService;
		
		Jedis jedis = jedisPool.getResource();
		try {
			DiscoverCache.loadScript(jedis);
		} finally {
			jedis.close();
		}
	}
	
	@Override
	public ListenableFuture<GetDiscoverHomeResponse> getDiscoverHome(RequestHead head, EmptyRequest request) {
		
		DiscoverDAOProtos.DiscoverHome discoverHome = null;
		
		Jedis jedis = jedisPool.getResource();
		try {
			discoverHome = DiscoverCache.getDiscoverHome(jedis);
		} finally {
			jedis.close();
		}
		
		if (discoverHome == null) {
			Connection dbConn = null;
			try {
				dbConn = hikariDataSource.getConnection();
				discoverHome = DiscoverDB.getDiscoverHome(dbConn);
			} catch (SQLException e) {
				throw new RuntimeException("getDiscoverHome db fail", e);
			} finally {
				DBUtil.closeQuietly(dbConn);
			}
			
			jedis = jedisPool.getResource();
			try {
				DiscoverCache.setDiscoverHome(jedis, discoverHome);
			} finally {
				jedis.close();
			}
		}
		
		return Futures.immediateFuture(GetDiscoverHomeResponse.newBuilder()
				.addAllBanner(discoverHome.getBannerList())
				.addAllRecommendModule(discoverHome.getRecommendModuleList())
				.addAllModule(discoverHome.getModuleList())
				.build());
	}
	
	private static final DiscoverDAOProtos.ModuleItemDefault MODULE_ITEM_DEFAULT_EMPTY = 
			DiscoverDAOProtos.ModuleItemDefault.newBuilder().setItemId(0).setCreateTime(0).build();

	@Override
	public ListenableFuture<GetModuleItemListResponse> getModuleItemList(RequestHead head, GetModuleItemListRequest request) {
		final int moduleId = request.getModuleId();
		final int categoryId = request.getCategoryId();
		
		if (discoverExtends.isExtendsModule(moduleId)) {
			return Futures.immediateFuture(discoverExtends.getExtendsModuleItemList(head, request));
		}
		
		DiscoverDAOProtos.ModuleItemDefault begin = null;
		if (request.hasListIndexBegin()) {
			try {
				begin = DiscoverDAOProtos.ModuleItemDefault.parseFrom(request.getListIndexBegin());
				if (MODULE_ITEM_DEFAULT_EMPTY.equals(begin)) {
					begin = null;
				}
			} catch (InvalidProtocolBufferException e) {
				// ignore
			}
		}
		
		DiscoverDAOProtos.ModuleItemDefault end = null;
		if (request.hasListIndexEnd()) {
			try {
				end = DiscoverDAOProtos.ModuleItemDefault.parseFrom(request.getListIndexEnd());
				if (MODULE_ITEM_DEFAULT_EMPTY.equals(end)) {
					end = null;
				}
			} catch (InvalidProtocolBufferException e) {
				// ignore
			}
		}
		
		final int size;
		if (request.getItemSize() <= 0) {
			size = 10;
		} else if (request.getItemSize() > 50) {
			size = 50;
		} else {
			size = request.getItemSize();
		}
		
		DiscoverDAOProtos.ModuleItemDefaultList moduleItemDefaultList = this.doGetModuleItemDefaultList(moduleId, categoryId, begin, end, size);
		
		if (moduleItemDefaultList.getModuleItemDefaultCount() <= 0) {
			return Futures.immediateFuture(GetModuleItemListResponse.newBuilder()
					.setClearOldList(false)
					.setHasMore(false)
					.setListIndexBegin(MODULE_ITEM_DEFAULT_EMPTY.toByteString())
					.setListIndexEnd(MODULE_ITEM_DEFAULT_EMPTY.toByteString())
					.build());
		}
		
		Set<Long> itemIdSet = new HashSet<Long>(moduleItemDefaultList.getModuleItemDefaultCount());
		for (int i=0; i<moduleItemDefaultList.getModuleItemDefaultCount(); ++i) {
			itemIdSet.add(moduleItemDefaultList.getModuleItemDefault(i).getItemId());
		}
		
		Map<Long, DiscoverProtos.ItemContent> itemContentMap = this.doGetItemContent(itemIdSet);
		
		GetModuleItemListResponse.Builder responseBuilder = GetModuleItemListResponse.newBuilder();
		responseBuilder.setClearOldList(false);
		responseBuilder.setHasMore(moduleItemDefaultList.getHasMore());
		responseBuilder.setListIndexBegin(moduleItemDefaultList.getModuleItemDefault(0).toByteString());
		int cnt = moduleItemDefaultList.getModuleItemDefaultCount();
		responseBuilder.setListIndexEnd(moduleItemDefaultList.getModuleItemDefault(cnt - 1).toByteString());

		for (int i=0; i<moduleItemDefaultList.getModuleItemDefaultCount(); ++i) {
			long itemId = moduleItemDefaultList.getModuleItemDefault(i).getItemId();
			DiscoverProtos.ItemContent itemContent = itemContentMap.get(itemId);
			if (itemContent != null) {
				responseBuilder.addItem(itemContent.getItem());
			}
		}
		
		return Futures.immediateFuture(responseBuilder.build());
	}
	
	private static final int MAX_MODULE_ITEM_LIST_CACHE_SIZE = 200;
	
	private static final Comparator<DiscoverDAOProtos.ModuleItemDefault> MODULE_ITEM_DEFAULT_CMP = 
			new Comparator<DiscoverDAOProtos.ModuleItemDefault>() {

		@Override
		public int compare(DiscoverDAOProtos.ModuleItemDefault o1, DiscoverDAOProtos.ModuleItemDefault o2) {
			int cmp = Ints.compare(o1.getCreateTime(), o2.getCreateTime());
			if (cmp != 0) {
				return -cmp;
			}
			return -Longs.compare(o1.getItemId(), o2.getItemId());
		}
		
	};
	
	/**
	 * 禁止返回 list 为空, has more 为true的情况, 否则后续处理会出问题
	 */
	private DiscoverDAOProtos.ModuleItemDefaultList doGetModuleItemDefaultList(int moduleId, int categoryId, 
			DiscoverDAOProtos.ModuleItemDefault begin, DiscoverDAOProtos.ModuleItemDefault end, int size) {
		
		DiscoverDAOProtos.ModuleItemDefaultList cacheList;
		Jedis jedis = jedisPool.getResource();
		try {
			cacheList = DiscoverCache.getModuleItemDefaultList(jedis, moduleId, categoryId);
		} finally {
			jedis.close();
		}
		
		if (cacheList != null) {
			int idx = 0;
			
			// 跳过 <= begin 的数据
			if (begin != null) {
				while (idx < cacheList.getModuleItemDefaultCount()) {
					DiscoverDAOProtos.ModuleItemDefault moduleItemDefault = cacheList.getModuleItemDefault(idx);
					if (MODULE_ITEM_DEFAULT_CMP.compare(moduleItemDefault, begin) <= 0) {
						idx ++;
					} else {
						break;
					}
				}
			}
			
			DiscoverDAOProtos.ModuleItemDefaultList.Builder resultBuilder = DiscoverDAOProtos.ModuleItemDefaultList.newBuilder();
			
			while (idx < cacheList.getModuleItemDefaultCount()) {
				DiscoverDAOProtos.ModuleItemDefault moduleItemDefault = cacheList.getModuleItemDefault(idx);
				if (resultBuilder.getModuleItemDefaultCount() >= size) { 
					// 已放满，检查下一条数据是否是结尾，返回
					return resultBuilder
							.setHasMore(end == null || MODULE_ITEM_DEFAULT_CMP.compare(moduleItemDefault, end) < 0)
							.build();
				} else if (end == null || MODULE_ITEM_DEFAULT_CMP.compare(moduleItemDefault, end) < 0) {
					// 未放满, 且未到结尾，继续放
					resultBuilder.addModuleItemDefault(moduleItemDefault);
					idx ++;
				} else {
					// 未放满, 但是已放到结尾, 返回
					return resultBuilder.setHasMore(false).build();
				}
			}
			
			// 遍历所有数据后，未放满，如果db中也没有数据 直接返回
			if (!cacheList.getHasMore()) {
				return resultBuilder.setHasMore(false).build();
			}
			
			// 遍历所有数据后，未放满，且db中还有数据，继续从db中获取
		}
		
		DiscoverDAOProtos.ModuleItemDefaultList setToCacheList = null;
		DiscoverDAOProtos.ModuleItemDefaultList resultList = null;
		Connection dbConn = null;
		try {
			dbConn = hikariDataSource.getConnection();
			
			if (cacheList == null) {
				// 多获取一条用来判断hasMore
				List<DiscoverDAOProtos.ModuleItemDefault> list = 
						DiscoverDB.getModuleItemDefaultList(dbConn, moduleId, categoryId, null, null, MAX_MODULE_ITEM_LIST_CACHE_SIZE + 1);
				
				if (list.size() > MAX_MODULE_ITEM_LIST_CACHE_SIZE) {
					setToCacheList = DiscoverDAOProtos.ModuleItemDefaultList.newBuilder()
							.addAllModuleItemDefault(list.subList(0, MAX_MODULE_ITEM_LIST_CACHE_SIZE))
							.setHasMore(true)
							.build();
				} else {
					setToCacheList = DiscoverDAOProtos.ModuleItemDefaultList.newBuilder()
							.addAllModuleItemDefault(list)
							.setHasMore(false)
							.build();
				}
			}
			
			List<DiscoverDAOProtos.ModuleItemDefault> list = DiscoverDB.getModuleItemDefaultList(dbConn, moduleId, categoryId, begin, end, size + 1);
			if (list.size() > size) {
				resultList = DiscoverDAOProtos.ModuleItemDefaultList.newBuilder()
						.addAllModuleItemDefault(list.subList(0, size))
						.setHasMore(true)
						.build();
			} else {
				resultList = DiscoverDAOProtos.ModuleItemDefaultList.newBuilder()
						.addAllModuleItemDefault(list)
						.setHasMore(false)
						.build();
			}
		} catch (SQLException e) {
			throw new RuntimeException("doGetModuleItemDefaultList db fail", e);
		} finally {
			DBUtil.closeQuietly(dbConn);
		}
		
		if (setToCacheList != null) {
			jedis = jedisPool.getResource();
			try {
				DiscoverCache.setModuleItemDefaultList(jedis, moduleId, categoryId, setToCacheList);
			} finally {
				jedis.close();
			}
		}
		
		return resultList;
	}
	
	@Override
	public ListenableFuture<GetItemByIdResponse> getItemById(RequestHead head, GetItemByIdRequest request) {
		if (request.getItemIdCount() <= 0 || request.getItemIdCount() > 50) {
			return Futures.immediateFuture(GetItemByIdResponse.newBuilder().build());
		}
		
		Map<Long, DiscoverProtos.ItemContent> itemContentMap = this.doGetItemContent(head, request.getItemIdList());
		
		GetItemByIdResponse.Builder responseBuilder = GetItemByIdResponse.newBuilder();
		for (DiscoverProtos.ItemContent itemContent : itemContentMap.values()) {
			responseBuilder.addItem(itemContent.getItem());
		}
		
		return Futures.immediateFuture(responseBuilder.build());
	}
	
	private Map<Long, DiscoverProtos.ItemContent> doGetItemContent(RequestHead head, Collection<Long> itemIds) {
		Set<Long> itemIdSet = new TreeSet<Long>();
		Set<Long> extendsItemIdSet = new TreeSet<Long>();
		
		for (Long itemId : itemIds) {
			if (this.discoverExtends.isExtendsItem(itemId)) {
				extendsItemIdSet.add(itemId);
			} else {
				itemIdSet.add(itemId);
			}
		}
		
		Map<Long, DiscoverProtos.ItemContent> resultMap = new HashMap<Long, DiscoverProtos.ItemContent>();
		resultMap.putAll(this.discoverExtends.getExtendsItemContent(head, extendsItemIdSet));
		resultMap.putAll(this.doGetItemContent(itemIdSet));
		
		return resultMap;
	}
	
	private Map<Long, DiscoverProtos.ItemContent> doGetItemContent(Collection<Long> itemIds) {
		Map<Long, DiscoverProtos.ItemContent> resultMap = new HashMap<Long, DiscoverProtos.ItemContent>();
		
		Set<Long> noCacheItemIdSet = new TreeSet<Long>();
		Jedis jedis = jedisPool.getResource();
		try {
			resultMap.putAll(DiscoverCache.getItemContent(jedis, itemIds, noCacheItemIdSet));
		} finally {
			jedis.close();
		}
		
		if (noCacheItemIdSet.isEmpty()) {
			return resultMap;
		}
		
		Map<Long, DiscoverProtos.ItemContent> noCacheItemContentMap;
		Connection dbConn = null;
		try {
			dbConn = hikariDataSource.getConnection();
			noCacheItemContentMap = DiscoverDB.getItemById(dbConn, noCacheItemIdSet);
		} catch (SQLException e) {
			throw new RuntimeException("doGetItemById db fail", e);
		} finally {
			DBUtil.closeQuietly(dbConn);
		}
		
		jedis = jedisPool.getResource();
		try {
			DiscoverCache.setItemContent(jedis, noCacheItemIdSet, noCacheItemContentMap);
		} finally {
			jedis.close();
		}
		
		resultMap.putAll(noCacheItemContentMap);
		
		return resultMap;
	}
	
	@Override
	public ListenableFuture<GetItemContentResponse> getItemContent(RequestHead head, GetItemContentRequest request) {
		
		DiscoverProtos.ItemContent itemContent = this.doGetItemContent(head, Collections.singleton(request.getItemId())).get(request.getItemId());
		
		if (itemContent != null) {
			return Futures.immediateFuture(GetItemContentResponse.newBuilder()
					.setItemContent(itemContent)
					.build());
		} else {
			return Futures.immediateFuture(GetItemContentResponse.newBuilder()
					.build());
		}
	}
	
	@Override
	public ListenableFuture<SearchItemResponse> searchItem(RequestHead head, SearchItemRequest request) {
		Connection dbConn = null;
		try {
			dbConn = this.hikariDataSource.getConnection();
			
			return Futures.immediateFuture(SearchItemResponse.newBuilder()
					.addAllItem(DiscoverDB.searchItemId(dbConn, request.getKeyword()))
					.build());
			
		} catch (SQLException e) {
			throw new RuntimeException("db fail", e);
		} finally {
			DBUtil.closeQuietly(dbConn);
		}
	}

	@Override
	public ListenableFuture<EmptyResponse> clearCache(SystemHead head, EmptyRequest request) {
		Jedis jedis = jedisPool.getResource();
		try {
			DiscoverCache.clearCache(jedis);
		} finally {
			jedis.close();
		}
		return ServiceUtil.EMPTY_RESPONSE_IMMEDIATE_FUTURE;
	}
	
	@Override
	public ListenableFuture<GetItemListResponse> getItemList(SystemHead head, GetItemListRequest request) {
		final int size = request.getSize() < 0 ? 0 : request.getSize() > 500 ? 500 : request.getSize();
		final Long lastItemId = request.hasLastItemId() ? request.getLastItemId() : null;
		
		List<Long> itemIdList;
		Connection dbConn = null;
		try {
			dbConn = this.hikariDataSource.getConnection();	
			itemIdList = DiscoverDB.getItemIdList(dbConn, lastItemId, size + 1);
		} catch (SQLException e) {
			throw new RuntimeException("db fail", e);
		} finally {
			DBUtil.closeQuietly(dbConn);
		}
		
		boolean hasMore;
		if (itemIdList.size() > size) {
			hasMore = true;
			itemIdList = itemIdList.subList(0, size);
		} else {
			hasMore = false;
		}
		
		Map<Long, DiscoverProtos.ItemContent> itemContentMap = this.doGetItemContent(itemIdList);
		
		GetItemListResponse.Builder responseBuilder = GetItemListResponse.newBuilder();
		
		for (Long itemId : itemIdList) {
			DiscoverProtos.ItemContent itemContent = itemContentMap.get(itemId);
			if (itemContent != null) {
				responseBuilder.addItem(itemContent.getItem());
			}
		}
		responseBuilder.setHasMore(hasMore);
		return Futures.immediateFuture(responseBuilder.build());
	}
	
	@Override
	public ListenableFuture<GetItemPVResponse> getItemPV(RequestHead head, GetItemPVRequest request) {
//		if (request.getIsIncrePv()) {
//			this.statsService.reportDiscoverItemAccess(head, StatsProtos.DiscoverItemAccess.newBuilder()
//					.setTime(System.currentTimeMillis())
//					.setItemId(request.getItemId())
//					.setActionName("VIEW")
//					.build());
//		}
		
		final long itemId = request.getItemId();
		
		// 肯定可以获取到 pv值，不会出现 null的情况
		int pv = this.doGetItemPV(Collections.singleton(itemId)).get(itemId);
		
		if (request.getIsIncrePv()) {
			final Long incredPV;
			Jedis jedis = jedisPool.getResource();
			try {
				incredPV = DiscoverCache.increItemPV(jedis, itemId);
			} finally {
				jedis.close();
			}
			
			// ignore increPV null
			if (incredPV != null) {
				pv = incredPV.intValue();
				this.serviceExecutor.execute(new Runnable() {

					@Override
					public void run() {
						Connection dbConn = null;
						try {
							dbConn = DiscoverServiceImpl.this.hikariDataSource.getConnection();
							DiscoverDB.increItemPV(dbConn, Collections.<Long, Integer>singletonMap(itemId, incredPV.intValue()));
						} catch (SQLException e) {
							throw new RuntimeException("db fail", e);
						} finally {
							DBUtil.closeQuietly(dbConn);
						}
					}
				
				});
			}
		}
		
		return Futures.immediateFuture(GetItemPVResponse.newBuilder()
				.setPv(pv)
				.build());
	}
	
	private Map<Long, Integer> doGetItemPV(Collection<Long> itemIds) {
		Map<Long, Integer> resultMap = new HashMap<Long, Integer>();
		
		Set<Long> noCacheItemIdSet = new TreeSet<Long>();
		Jedis jedis = jedisPool.getResource();
		try {
			resultMap.putAll(DiscoverCache.getItemPV(jedis, itemIds, noCacheItemIdSet));
		} finally {
			jedis.close();
		}
		
		if (noCacheItemIdSet.isEmpty()) {
			return resultMap;
		}
		
		Map<Long, Integer> noCacheResultMap;
		Connection dbConn = null;
		try {
			dbConn = this.hikariDataSource.getConnection();
			noCacheResultMap = DiscoverDB.getItemPV(dbConn, noCacheItemIdSet);
		} catch (SQLException e) {
			throw new RuntimeException("db fail", e);
		} finally {
			DBUtil.closeQuietly(dbConn);
		}
		
		jedis = jedisPool.getResource();
		try {
			DiscoverCache.setItemPV(jedis, noCacheResultMap);
		} finally {
			jedis.close();
		}
		
		resultMap.putAll(noCacheResultMap);
		
		return resultMap;
	}

	@Override
	public ListenableFuture<GetItemScoreResponse> getItemScore(RequestHead head, GetItemScoreRequest request) {
		final long userId = head.getSession().getUserId();
		final long itemId = request.getItemId();
		
		Connection dbConn = null;
		try {
			dbConn = this.hikariDataSource.getConnection();
			
			DiscoverDAOProtos.ItemScore itemScore = DiscoverDB.getItemScore(dbConn, Collections.singleton(itemId)).get(itemId);
			Integer userScore = DiscoverDB.getUserItemScore(dbConn, userId, itemId);
			
			GetItemScoreResponse.Builder responseBuilder = GetItemScoreResponse.newBuilder();
			if (userScore != null) {
				responseBuilder.setScore(userScore);
			}
			responseBuilder.setTotalScore(itemScore.getTotalScore());
			responseBuilder.setTotalUser(itemScore.getTotalUser());
			
			return Futures.immediateFuture(responseBuilder.build());
			
		} catch (SQLException e) {
			throw new RuntimeException("db fail", e);
		} finally {
			DBUtil.closeQuietly(dbConn);
		}
	}
	
	@Override
	public ListenableFuture<ScoreItemResponse> scoreItem(RequestHead head, ScoreItemRequest request) {
		ScoreItemResponse response = this.doScoreItem(head, request);
		
//		StatsProtos.DiscoverItemAccess.Builder builder = StatsProtos.DiscoverItemAccess.newBuilder()
//				.setTime(System.currentTimeMillis())
//				.setItemId(request.getItemId())
//				.setActionName("SCORE_ITEM")
//				.setScore(request.getScore())
//				.setScoreResult(response.getResult().name());
//		if (response.hasFailText()) {
//			builder.setScoreFailText(response.getFailText());
//		}
//		this.statsService.reportDiscoverItemAccess(head, builder.build());
		
		return Futures.immediateFuture(response);
	}

	private ScoreItemResponse doScoreItem(RequestHead head, ScoreItemRequest request) {
		final long userId = head.getSession().getUserId();
		final long itemId = request.getItemId();
		final int score = request.getScore();
		if (score < 0 || score > 100) {
			return ScoreItemResponse.newBuilder()
					.setResult(ScoreItemResponse.Result.FAIL_SCORE_INVALID)
					.setFailText("请在0～100中选择分数")
					.build();
		}
		
		Connection dbConn = null;
		try {
			dbConn = this.hikariDataSource.getConnection();
			
			DiscoverProtos.ItemContent itemContent = DiscoverDB.getItemById(dbConn, Collections.singleton(itemId)).get(itemId);
			
			if (itemContent == null) {
				return ScoreItemResponse.newBuilder()
						.setResult(ScoreItemResponse.Result.FAIL_ITEM_NOT_EXSIT)
						.setFailText("您打分的条目不存在")
						.build();
			}
			if (!itemContent.getItem().hasEnableScore() || !itemContent.getItem().getEnableScore()) {
				return ScoreItemResponse.newBuilder()
						.setResult(ScoreItemResponse.Result.FAIL_ITEM_DISABLE)
						.setFailText("该条目不允许打分")
						.build();
			}
			
			if (DiscoverDB.getUserItemScore(dbConn, userId, itemId) != null) {
				return ScoreItemResponse.newBuilder()
						.setResult(ScoreItemResponse.Result.FAIL_ITEM_IS_SCORED)
						.setFailText("您对该条目已经打过分了")
						.build();
			}
			
			DiscoverDB.insertUserItemScore(dbConn, userId, itemId, score);
			
			return ScoreItemResponse.newBuilder()
					.setResult(ScoreItemResponse.Result.SUCC)
					.build();
			
		} catch (SQLException e) {
			throw new RuntimeException("db fail", e);
		} finally {
			DBUtil.closeQuietly(dbConn);
		}
	}

	@Override
	public ListenableFuture<GetItemCommentListResponse> getItemAllCommentList(RequestHead head, GetItemCommentListRequest request) {
		if (request.getSize() < 0 || request.getSize() > 100) {
			return Futures.immediateFuture(GetItemCommentListResponse.newBuilder()
					.setHasMore(false)
					.build());
		}
		
		Connection dbConn = null;
		try {
			dbConn = this.hikariDataSource.getConnection();
			
			int total = DiscoverDB.getItemAllCommentCount(dbConn, Collections.singleton(request.getItemId())).get(request.getItemId());
			
			List<DiscoverProtos.Comment> list;
			if (total <= 0) {
				list = Collections.emptyList();
			} else if (request.hasLastCommentId() || request.hasLastCommentTime()) {
				list = DiscoverDB.getItemAllCommentList(dbConn, request.getItemId(), request.getLastCommentId(), request.getLastCommentTime(), request.getSize() + 1);
			} else {
				list = DiscoverDB.getItemAllCommentList(dbConn, request.getItemId(), request.getSize() + 1);
			}
			
			GetItemCommentListResponse.Builder responseBuilder = GetItemCommentListResponse.newBuilder();
			
			for (DiscoverProtos.Comment comment : list) {
				if (responseBuilder.getCommentCount() >= request.getSize()) {
					break;
				}
				responseBuilder.addComment(comment);
			}
			responseBuilder.setHasMore(list.size() > request.getSize());
			responseBuilder.setTotal(total);
			
			return Futures.immediateFuture(responseBuilder.build());
		} catch (SQLException e) {
			throw new RuntimeException("db fail", e);
		} finally {
			DBUtil.closeQuietly(dbConn);
		}
	}
	
	@Override
	public ListenableFuture<GetItemCommentListResponse> getItemMyCommentList(RequestHead head, GetItemCommentListRequest request) {
		if (request.getSize() < 0 || request.getSize() > 100) {
			return Futures.immediateFuture(GetItemCommentListResponse.newBuilder()
					.setHasMore(false)
					.build());
		}
		
		final long userId = head.getSession().getUserId();
		
		Connection dbConn = null;
		try {
			dbConn = this.hikariDataSource.getConnection();
			
			int total = DiscoverDB.getItemMyCommentCount(dbConn, userId, Collections.singleton(request.getItemId())).get(request.getItemId());
			
			List<DiscoverProtos.Comment> list;
			if (total <= 0) {
				list = Collections.emptyList();
			} else if (request.hasLastCommentId() || request.hasLastCommentTime()) {
				list = DiscoverDB.getItemMyCommentList(dbConn, userId, request.getItemId(), request.getLastCommentId(), request.getLastCommentTime(), request.getSize() + 1);
			} else {
				list = DiscoverDB.getItemMyCommentList(dbConn, userId, request.getItemId(), request.getSize() + 1);
			}
			
			GetItemCommentListResponse.Builder responseBuilder = GetItemCommentListResponse.newBuilder();
			
			for (DiscoverProtos.Comment comment : list) {
				if (responseBuilder.getCommentCount() >= request.getSize()) {
					break;
				}
				responseBuilder.addComment(comment);
			}
			responseBuilder.setHasMore(list.size() > request.getSize());
			responseBuilder.setTotal(total);
			
			return Futures.immediateFuture(responseBuilder.build());
		} catch (SQLException e) {
			throw new RuntimeException("db fail", e);
		} finally {
			DBUtil.closeQuietly(dbConn);
		}
	}

	@Override
	public ListenableFuture<CommentItemResponse> commentItem(RequestHead head, CommentItemRequest request) {
		CommentItemResponse response = this.doCommentItem(head, request);
		
//		StatsProtos.DiscoverItemAccess.Builder builder = StatsProtos.DiscoverItemAccess.newBuilder()
//				.setTime(System.currentTimeMillis())
//				.setItemId(request.getItemId())
//				.setActionName("COMMENT_ITEM")
//				.setComment(request.getCommentContent())
//				.setCommentResult(response.getResult().name());
//		if (response.hasFailText()) {
//			builder.setCommentFailText(response.getFailText());
//		}
//		this.statsService.reportDiscoverItemAccess(head, builder.build());
		
		return Futures.immediateFuture(response);
	}
	
	private CommentItemResponse doCommentItem(RequestHead head, CommentItemRequest request) {
		
		final long itemId = request.getItemId();
		
		if (request.getCommentContent().isEmpty()) {
			return CommentItemResponse.newBuilder()
					.setResult(CommentItemResponse.Result.FAIL_CONTENT_INVALID)
					.setFailText("评论内容不能为空")
					.build();
		}
		
		if (request.getCommentContent().length() > 190) {
			return CommentItemResponse.newBuilder()
					.setResult(CommentItemResponse.Result.FAIL_CONTENT_INVALID)
					.setFailText("评论内容最多190个字")
					.build();
		}
		
		DiscoverProtos.Comment comment = DiscoverProtos.Comment.newBuilder()
				.setCommentId(-1L)
				.setCommentTime((int) (System.currentTimeMillis() / 1000L))
				.setUserId(head.getSession().getUserId())
				.setContent(request.getCommentContent())
				.build();
		
		Connection dbConn = null;
		try {
			dbConn = this.hikariDataSource.getConnection();
			
			DiscoverProtos.ItemContent itemContent = DiscoverDB.getItemById(dbConn, Collections.singleton(itemId)).get(itemId);
			
			if (itemContent == null) {
				return CommentItemResponse.newBuilder()
						.setResult(CommentItemResponse.Result.FAIL_ITEM_NOT_EXSIT)
						.setFailText("您评论的条目不存在")
						.build();
			}
			
			if (!itemContent.getItem().hasEnableComment() || !itemContent.getItem().getEnableComment()) {
				return CommentItemResponse.newBuilder()
						.setResult(CommentItemResponse.Result.FAIL_ITEM_DISABLE)
						.setFailText("该条目不允许评论")
						.build();
			}
			
			long commentId = DiscoverDB.insertItemComment(dbConn, itemId, comment);
			
			return CommentItemResponse.newBuilder()
					.setResult(CommentItemResponse.Result.SUCC)
					.setCommentId(commentId)
					.build();
			
		} catch (SQLException e) {
			throw new RuntimeException("db fail", e);
		} finally {
			DBUtil.closeQuietly(dbConn);
		}
	}

	@Override
	public ListenableFuture<DeleteCommentResponse> deleteComment(RequestHead head, DeleteCommentRequest request) {
		
		final long commentId = request.getCommentId();
		
		Connection dbConn = null;
		try {
			dbConn = this.hikariDataSource.getConnection();
		
			DiscoverProtos.Comment comment = DiscoverDB.getComment(dbConn, Collections.singleton(commentId)).get(commentId);
			
			if (comment == null) {
				return Futures.immediateFuture(DeleteCommentResponse.newBuilder()
						.setResult(DeleteCommentResponse.Result.FAIL_COMMENT_NOT_EXSIT)
						.setFailText("不存在该评论")
						.build());
			}
			if (comment.getUserId() != head.getSession().getUserId()) {
				return Futures.immediateFuture(DeleteCommentResponse.newBuilder()
						.setResult(DeleteCommentResponse.Result.FAIL_COMMENT_OTHER)
						.setFailText("您不是该评论的作者，无法删除该评论")
						.build());
			}
			
			DiscoverDB.deleteComment(dbConn, Collections.singleton(commentId));
			
			return Futures.immediateFuture(DeleteCommentResponse.newBuilder()
					.setResult(DeleteCommentResponse.Result.SUCC)
					.build());
		} catch (SQLException e) {
			throw new RuntimeException("db fail", e);
		} finally {
			DBUtil.closeQuietly(dbConn);
		}
	}

}
