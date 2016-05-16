package com.weizhu.service.scene;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.Executor;

import javax.annotation.Nullable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import com.weizhu.common.db.DBUtil;
import com.weizhu.proto.DiscoverV2Protos;
import com.weizhu.proto.DiscoverV2Service;
import com.weizhu.proto.SceneProtos;
import com.weizhu.proto.SceneProtos.GetRecommenderCompetitorProductRequest;
import com.weizhu.proto.SceneProtos.GetRecommenderCompetitorProductResponse;
import com.weizhu.proto.SceneProtos.GetRecommenderHomeResponse;
import com.weizhu.proto.SceneProtos.GetRecommenderRecommendProductRequest;
import com.weizhu.proto.SceneProtos.GetRecommenderRecommendProductResponse;
import com.weizhu.proto.SceneProtos.GetSceneItemResponse;
import com.weizhu.proto.SceneProtos.Item.ItemIndex;
import com.weizhu.proto.SceneService;
import com.weizhu.proto.WeizhuProtos.EmptyRequest;
import com.weizhu.service.scene.tools.recommender.RecommenderManager;
import com.weizhu.proto.WeizhuProtos.RequestHead;
import com.zaxxer.hikari.HikariDataSource;

/**
 * 场景的针mobile的service类
 * 
 * @author zhangjun
 *
 */
public class SceneServiceImpl implements SceneService {

	@SuppressWarnings("unused")
	private static final Logger logger = LoggerFactory.getLogger(SceneServiceImpl.class);
	protected static final ImmutableList<SceneProtos.State> USER_STATE_LIST = ImmutableList.of(SceneProtos.State.NORMAL);
	private static final int SCENE_ITEM_INDEX_MAX_CACHE_COUNT = 100;

	private final HikariDataSource hikariDataSource;
	private final JedisPool jedisPool;
	@SuppressWarnings("unused")
	private final Executor serviceExecutor;
	private final RecommenderManager recommenderManager;
	private final DiscoverV2Service discoverV2Service;

	@Inject
	public SceneServiceImpl(HikariDataSource hikariDataSource, JedisPool jedisPool, @Named("service_executor") Executor serviceExecutor,
			RecommenderManager recommenderManager, DiscoverV2Service discoverV2Service) {
		this.hikariDataSource = hikariDataSource;
		this.jedisPool = jedisPool;
		this.serviceExecutor = serviceExecutor;
		this.recommenderManager = recommenderManager;
		this.discoverV2Service = discoverV2Service;
	}

	@Override
	public ListenableFuture<SceneProtos.GetSceneHomeResponse> getSceneHome(RequestHead head, EmptyRequest request) {
		
		final long companyId = head.getSession().getCompanyId();

		return Futures.immediateFuture(SceneProtos.GetSceneHomeResponse.newBuilder()
				.addAllScene(this.getSceneHome(hikariDataSource, jedisPool, companyId).getSceneList())
				.build());
	}

	@Override
	public ListenableFuture<SceneProtos.GetSceneItemResponse> getSceneItem(RequestHead head, SceneProtos.GetSceneItemRequest request) {

		final long companyId = head.getSession().getCompanyId();
		final Integer sceneId = request.hasSceneId() ? request.getSceneId() : null;
		final Integer size = request.hasSize() ? request.getSize() : null;

		final String itemTitle = request.hasItemTitle() ? request.getItemTitle() : null;

		final SceneProtos.Item.ItemIndex currentOffsetIndex;
		if (request.hasOffsetIndex() && !request.getOffsetIndex().isEmpty()) {
			SceneProtos.Item.ItemIndex tmp = null;
			try {
				tmp = SceneProtos.Item.ItemIndex.parseFrom(request.getOffsetIndex());
			} catch (InvalidProtocolBufferException e) {
				tmp = null;
			}
			currentOffsetIndex = tmp;
		} else {
			currentOffsetIndex = null;
		}

		// 当全量获取或搜索时，条目按创建时间倒序排列
		if (sceneId == null || itemTitle != null) {
			return getSceneItem(head, sceneId, size, itemTitle, currentOffsetIndex);
		}

		SceneDAOProtos.SceneExt sceneExt = this.getSceneExt(hikariDataSource, jedisPool, companyId, Collections.singleton(sceneId)).get(sceneId);
		if (sceneExt == null) {
			return Futures.immediateFuture(SceneProtos.GetSceneItemResponse.newBuilder().setHasMore(false).setOffsetIndex(ByteString.EMPTY).build());
		}

		Iterator<SceneProtos.Item.ItemIndex> itemIndexIterator = sceneExt.getItemIndexList().iterator();
		List<SceneProtos.Item.ItemIndex> itemIndexList = new ArrayList<SceneProtos.Item.ItemIndex>();
		while (itemIndexList.size() < size + 1 && itemIndexIterator.hasNext()) {
			SceneProtos.Item.ItemIndex itemIndex = itemIndexIterator.next();
			if ((currentOffsetIndex == null || currentOffsetIndex.getItemOrder() < itemIndex.getItemOrder())) {
				itemIndexList.add(itemIndex);
			}
		}

		// 当获取的列表超出缓存的范围时，则直接读取db
		if (itemIndexList.size() < size + 1 && sceneExt.getItemIndexCount() >= SCENE_ITEM_INDEX_MAX_CACHE_COUNT) {
			Connection dbConn = null;
			try {
				dbConn = hikariDataSource.getConnection();
				itemIndexList = SceneDB.getItemIndexByOrderStr(dbConn, companyId, sceneId, size + 1, currentOffsetIndex, USER_STATE_LIST);
			} catch (SQLException e) {
				throw new RuntimeException("DB FAILED");
			} finally {
				DBUtil.closeQuietly(dbConn);
			}
		}

		Boolean hasMore;
		if (itemIndexList.size() > size) {
			itemIndexList = itemIndexList.subList(0, size);
			hasMore = true;
		} else {
			hasMore = false;
		}

		ByteString offsetIndex = itemIndexList.isEmpty() ? ByteString.EMPTY : itemIndexList.get(itemIndexList.size() - 1).toByteString();

		// 获取item内容
		Set<Long> discoverItemIds = new TreeSet<Long>();
		for (SceneProtos.Item.ItemIndex itemIndex : itemIndexList) {
			if (itemIndex.hasDiscoverItemId()) {
				discoverItemIds.add(itemIndex.getDiscoverItemId());
			}
		}
		Map<Long, DiscoverV2Protos.Item> discoverItemMap = this.getDiscoverItemMap(head, discoverItemIds);
		List<SceneProtos.Item> itemList = new ArrayList<SceneProtos.Item>();
		SceneProtos.Item.Builder itemBuilder = SceneProtos.Item.newBuilder();
		for (SceneProtos.Item.ItemIndex itemIndex : itemIndexList) {
			itemBuilder.clear();
			itemBuilder.setItemIndex(itemIndex);
			if (itemIndex.hasDiscoverItemId()) {
				DiscoverV2Protos.Item.Base itemBase = discoverItemMap.get(itemIndex.getDiscoverItemId()).getBase();
				if (itemBase != null) {
					itemBuilder.setDiscoverItem(itemBase);
				}
			} else {
				// 目前只有发现，后期有可能添加其他内容
				continue;
			}
			itemList.add(itemBuilder.build());
		}

		return Futures.immediateFuture(SceneProtos.GetSceneItemResponse.newBuilder()
				.setHasMore(hasMore)
				.setOffsetIndex(offsetIndex)
				.addAllItem(itemList)
				.build());
	}

	private ListenableFuture<GetSceneItemResponse> getSceneItem(RequestHead head, @Nullable Integer sceneId, @Nullable Integer size,
			@Nullable String itemTitle, @Nullable ItemIndex currentOffsetIndex) {
		
		final long companyId = head.getSession().getCompanyId();

		List<ItemIndex> itemIndexList = new ArrayList<ItemIndex>();
		Connection dbConn = null;
		try {
			dbConn = hikariDataSource.getConnection();
			ItemIndex lastIndex = null;
			int maxSize = 1000;
			while (true) {
				List<ItemIndex> tmpItemIndexList = SceneDB.getItemIndexByCreateTime(dbConn, companyId, sceneId, maxSize + 1, lastIndex, USER_STATE_LIST);
				if (tmpItemIndexList.size() > maxSize) {
					tmpItemIndexList = tmpItemIndexList.subList(0, tmpItemIndexList.size());
					itemIndexList.addAll(tmpItemIndexList);
					lastIndex = itemIndexList.get(itemIndexList.size() - 1);
				} else {
					itemIndexList.addAll(tmpItemIndexList);
					break;
				}
			}
		} catch (SQLException e) {
			throw new RuntimeException("DB failed");
		} finally {
			DBUtil.closeQuietly(dbConn);
		}

		// 获取item内容
		Set<Long> discoverItemIds = new TreeSet<Long>();
		for (SceneProtos.Item.ItemIndex itemIndex : itemIndexList) {
			if (itemIndex.hasDiscoverItemId()) {
				discoverItemIds.add(itemIndex.getDiscoverItemId());
			}
		}

		Map<Long, DiscoverV2Protos.Item> discoverItemMap = this.getDiscoverItemMap(head, discoverItemIds);
		List<SceneProtos.Item> itemList = new ArrayList<SceneProtos.Item>();
		SceneProtos.Item.Builder itemBuilder = SceneProtos.Item.newBuilder();
		for (SceneProtos.Item.ItemIndex itemIndex : itemIndexList) {
			itemBuilder.clear();
			itemBuilder.setItemIndex(itemIndex);
			if (itemIndex.hasDiscoverItemId()) {
				DiscoverV2Protos.Item.Base itemBase = discoverItemMap.get(itemIndex.getDiscoverItemId()).getBase();
				if (itemBase != null) {
					// 根据关键字过滤条目
					if (itemTitle != null && !itemBase.getItemName().contains(itemTitle)) {
						continue;
					}
					itemBuilder.setDiscoverItem(itemBase);
				}
			} else {
				// 目前只有发现，后期有可能添加其他内容
				continue;
			}
			itemList.add(itemBuilder.build());
		}

		List<SceneProtos.Item> resultItemList = new ArrayList<SceneProtos.Item>();
		Iterator<SceneProtos.Item> itemIterator = itemList.iterator();
		while (itemIterator.hasNext() && resultItemList.size() < size + 1) {
			SceneProtos.Item item = itemIterator.next();
			if (currentOffsetIndex == null
					|| (currentOffsetIndex.getCreateTime() > item.getItemIndex().getCreateTime() || (currentOffsetIndex.getCreateTime() == item.getItemIndex()
							.getCreateTime() && currentOffsetIndex.getItemId() > item.getItemIndex().getItemId()))) {
				resultItemList.add(item);
			}
		}

		boolean hasMore;
		if (resultItemList.size() > size) {
			hasMore = true;
			resultItemList = resultItemList.subList(0, size);
		} else {
			hasMore = false;
		}

		ByteString offsetIndex;
		if (resultItemList.size() > 0) {
			offsetIndex = resultItemList.get(resultItemList.size() - 1).toByteString();
		} else {
			offsetIndex = ByteString.EMPTY;
		}

		return Futures.immediateFuture(SceneProtos.GetSceneItemResponse.newBuilder()
				.setHasMore(hasMore)
				.setOffsetIndex(offsetIndex)
				.addAllItem(resultItemList)
				.build());
	}

	private Map<Long, DiscoverV2Protos.Item> getDiscoverItemMap(RequestHead head, Collection<Long> discoverItemIds) {

		DiscoverV2Protos.GetItemByIdResponse response = Futures.getUnchecked(discoverV2Service.getItemById(head,
				DiscoverV2Protos.GetItemByIdRequest.newBuilder().addAllItemId(discoverItemIds).build()));

		Map<Long, DiscoverV2Protos.Item> itemMap = new HashMap<Long, DiscoverV2Protos.Item>();

		for (DiscoverV2Protos.Item discoverItem : response.getItemList()) {
			itemMap.put(discoverItem.getBase().getItemId(), discoverItem);
		}
		return itemMap;
	}

	private SceneDAOProtos.SceneHome getSceneHome(HikariDataSource hikariDataSource, JedisPool jedisPool, long companyId) {

		SceneDAOProtos.SceneHome sceneHome = null;
		Jedis jedis = jedisPool.getResource();
		try {
			sceneHome = SceneCache.getSceneHome(jedis, Collections.singleton(companyId)).get(companyId);
		} finally {
			jedis.close();
		}

		if (sceneHome == null) {
			Connection dbConn = null;
			try {
				dbConn = hikariDataSource.getConnection();
				sceneHome = SceneDB.getSceneHome(dbConn, companyId, USER_STATE_LIST);
			} catch (SQLException e) {
				throw new RuntimeException("获取场景主页信息出错！");
			} finally {
				DBUtil.closeQuietly(dbConn);
			}

			if (sceneHome != null) {
				jedis = jedisPool.getResource();
				try {
					SceneCache.setSceneHome(jedis, Collections.singletonMap(companyId, sceneHome));
				} finally {
					jedis.close();
				}
			}
		}
		return sceneHome;
	}

	private Map<Integer, SceneDAOProtos.SceneExt> getSceneExt(HikariDataSource hikariDataSource, JedisPool jedisPool, long companyId, Collection<Integer> sceneIds) {

		if (sceneIds.isEmpty()) {
			return Collections.emptyMap();
		}

		Set<Integer> noCacheSceneIds = new TreeSet<Integer>();
		Map<Integer, SceneDAOProtos.SceneExt> sceneExtMap = new HashMap<Integer, SceneDAOProtos.SceneExt>();
		Jedis jedis = jedisPool.getResource();
		try {
			sceneExtMap.putAll(SceneCache.getSceneExt(jedis, companyId, sceneIds, noCacheSceneIds));
		} finally {
			jedis.close();
		}

		if (!noCacheSceneIds.isEmpty()) {

			Map<Integer, SceneDAOProtos.SceneExt> noCacheSceneExtMap;
			Connection dbConn = null;
			try {
				dbConn = hikariDataSource.getConnection();

				noCacheSceneExtMap = SceneDB.getSceneExt(dbConn, companyId, noCacheSceneIds, SCENE_ITEM_INDEX_MAX_CACHE_COUNT, SceneServiceImpl.USER_STATE_LIST);

				if (!noCacheSceneExtMap.isEmpty()) {

					sceneExtMap.putAll(noCacheSceneExtMap);

					jedis = jedisPool.getResource();
					try {
						SceneCache.setSceneExt(jedis, companyId, noCacheSceneExtMap);
					} finally {
						jedis.close();
					}
				}

			} catch (SQLException e) {
				throw new RuntimeException("获取场景主页信息出错！");
			}
		}
		return sceneExtMap;
	}

	
	/**
	 * 以下为工具——盖帽神器（超值推荐）的内容
	 */
	
	@Override
	public ListenableFuture<GetRecommenderHomeResponse> getRecommenderHome(RequestHead head, EmptyRequest request) {
		return recommenderManager.getRecommenderHome(head, request);
	}

	@Override
	public ListenableFuture<GetRecommenderCompetitorProductResponse> getRecommenderCompetitorProduct(RequestHead head,
			GetRecommenderCompetitorProductRequest request) {
		return recommenderManager.getRecommenderCompetitorProduct(head, request);
	}

	@Override
	public ListenableFuture<GetRecommenderRecommendProductResponse> getRecommenderRecommendProduct(RequestHead head,
			GetRecommenderRecommendProductRequest request) {
		return recommenderManager.getRecommenderRecommendProduct(head, request);
	}

}
