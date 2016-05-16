package com.weizhu.service.scene.tools.recommender;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.Executor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import com.google.common.collect.ImmutableList;
import com.google.common.primitives.Ints;
import com.google.common.primitives.Longs;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import com.weizhu.common.db.DBUtil;
import com.weizhu.proto.SceneProtos;
import com.weizhu.proto.SceneProtos.GetRecommenderHomeResponse;
import com.weizhu.proto.SceneProtos.GetRecommenderCompetitorProductRequest;
import com.weizhu.proto.SceneProtos.GetRecommenderCompetitorProductResponse;
import com.weizhu.proto.SceneProtos.GetRecommenderRecommendProductRequest;
import com.weizhu.proto.SceneProtos.GetRecommenderRecommendProductResponse;
import com.weizhu.proto.SceneProtos.RecommenderCompetitorProduct;
import com.weizhu.proto.SceneProtos.RecommenderPriceWebUrl;
import com.weizhu.proto.SceneProtos.RecommenderRecommendProduct;
import com.weizhu.proto.WeizhuProtos.EmptyRequest;
import com.weizhu.proto.WeizhuProtos.RequestHead;
import com.weizhu.service.scene.SceneDAOProtos;
import com.weizhu.service.scene.SceneDAOProtos.RecommenderHome;
import com.zaxxer.hikari.HikariDataSource;

/**
 * 盖帽神器（超值推荐）针mobile的业务逻辑类
 * 
 * @author zhangjun
 *
 */
public class RecommenderManager {

	@SuppressWarnings("unused")
	private static final Logger logger = LoggerFactory.getLogger(RecommenderManager.class);
	protected static final ImmutableList<SceneProtos.State> USER_STATE_LIST = ImmutableList.of(SceneProtos.State.NORMAL);
	private static final int CATEGORY_COMPETITOR_PRODUCT_MAX_CACHE_COUNT = 100;

	private final HikariDataSource hikariDataSource;
	private final JedisPool jedisPool;
	@SuppressWarnings("unused")
	private final Executor serviceExecutor;

	@Inject
	public RecommenderManager(HikariDataSource hikariDataSource, JedisPool jedisPool, @Named("service_executor") Executor serviceExecutor) {
		this.hikariDataSource = hikariDataSource;
		this.jedisPool = jedisPool;
		this.serviceExecutor = serviceExecutor;
	}

	public ListenableFuture<GetRecommenderHomeResponse> getRecommenderHome(RequestHead head, EmptyRequest request) {

		final long companyId = head.getSession().getCompanyId();
		RecommenderHome recommenderHome = null;
		Jedis jedis = jedisPool.getResource();
		try {
			recommenderHome = RecommenderCache.getRecommenderHome(jedis, Collections.singleton(companyId)).get(companyId);
		} finally {
			jedis.close();
		}

		if (recommenderHome == null) {
			Connection dbConn = null;
			try {
				dbConn = hikariDataSource.getConnection();
				recommenderHome = RecommenderDB.getRecommenderHome(dbConn, companyId, USER_STATE_LIST);
			} catch (SQLException ex) {
				throw new RuntimeException("db failed");
			} finally {
				DBUtil.closeQuietly(dbConn);
			}
			if (recommenderHome != null) {
				jedis = jedisPool.getResource();
				try {
					RecommenderCache.setRecommenderHome(jedis, Collections.singletonMap(companyId, recommenderHome));
				} finally {
					jedis.close();
				}
			}
		}

		if (recommenderHome != null) {
			return Futures.immediateFuture(GetRecommenderHomeResponse.newBuilder()
					.addAllCategory(recommenderHome.getRecommenderCategoryList())
					.build());
		} else {
			return Futures.immediateFuture(GetRecommenderHomeResponse.newBuilder().build());
		}
	}

	private static final Comparator<SceneProtos.RecommenderCompetitorProduct> COMPETITOR_PRODUCT_CMP = new Comparator<SceneProtos.RecommenderCompetitorProduct>() {

		@Override
		public int compare(SceneProtos.RecommenderCompetitorProduct o1, SceneProtos.RecommenderCompetitorProduct o2) {
			int cmp = 0;

			cmp = Ints.compare(o1.getCreateTime(), o2.getCreateTime());
			if (cmp != 0) {
				return -cmp;
			}

			cmp = Longs.compare(o1.getCategoryId(), o2.getCategoryId());
			if (cmp != 0) {
				return -cmp;
			}

			return 0;
		}

	};

	public ListenableFuture<GetRecommenderCompetitorProductResponse> getRecommenderCompetitorProduct(RequestHead head,
			GetRecommenderCompetitorProductRequest request) {
		final long companyId = head.getSession().getCompanyId();
		final SceneProtos.RecommenderCompetitorProduct currentOffsetIndex;
		if (request.hasOffsetIndex() && !request.getOffsetIndex().isEmpty()) {
			SceneProtos.RecommenderCompetitorProduct tmp = null;
			try {
				tmp = SceneProtos.RecommenderCompetitorProduct.parseFrom(request.getOffsetIndex());
			} catch (InvalidProtocolBufferException e) {
				tmp = null;
			}
			currentOffsetIndex = tmp;
		} else {
			currentOffsetIndex = null;
		}
		final Integer categoryId = request.hasCategoryId() ? request.getCategoryId() : null;
		final String competitorProductName = request.hasCompetitorProductName() ? request.getCompetitorProductName() : null;
		final int size = request.getSize();

		List<RecommenderCompetitorProduct> competitorProductList = new ArrayList<RecommenderCompetitorProduct>();

		if (categoryId != null) {
			SceneDAOProtos.RecommenderCategoryExt categoryExt = this.getCategoryExt(hikariDataSource,
					jedisPool,
					companyId,
					categoryId,
					competitorProductName,
					size);
			if (categoryExt == null) {
				return Futures.immediateFuture(GetRecommenderCompetitorProductResponse.newBuilder()
						.setOffsetIndex(ByteString.EMPTY)
						.setHasMore(false)
						.build());
			}

			List<RecommenderCompetitorProduct> tmpCompetitorProductList = new ArrayList<RecommenderCompetitorProduct>();

			tmpCompetitorProductList.addAll(categoryExt.getCompetitorProductList());

			Iterator<RecommenderCompetitorProduct> competitorProductIterator = tmpCompetitorProductList.iterator();
			while (competitorProductList.size() < size + 1 && competitorProductIterator.hasNext()) {
				RecommenderCompetitorProduct competitorProduct = competitorProductIterator.next();
				if (currentOffsetIndex != null && COMPETITOR_PRODUCT_CMP.compare(competitorProduct, currentOffsetIndex) <= 0) {
					continue;
				}
				competitorProductList.add(competitorProduct);
			}

			if (competitorProductList.size() < size + 1 && tmpCompetitorProductList.size() >= CATEGORY_COMPETITOR_PRODUCT_MAX_CACHE_COUNT) {
				Connection dbConn = null;
				try {
					dbConn = hikariDataSource.getConnection();
					competitorProductList = RecommenderDB.getCompetitorProduct(dbConn,
							companyId,
							categoryId,
							competitorProductName,
							currentOffsetIndex,
							size + 1,
							USER_STATE_LIST);
				} catch (SQLException e) {
					throw new RuntimeException("获取竞争对手产品出错！");
				} finally {
					DBUtil.closeQuietly(dbConn);
				}
			}
		} else {
			Connection dbConn = null;
			try {
				dbConn = hikariDataSource.getConnection();
				competitorProductList = RecommenderDB.getCompetitorProduct(dbConn,
						companyId,
						categoryId,
						competitorProductName,
						currentOffsetIndex,
						size + 1,
						USER_STATE_LIST);
			} catch (SQLException e) {
				throw new RuntimeException("获取竞争对手产品出错！");
			} finally {
				DBUtil.closeQuietly(dbConn);
			}
		}

		Boolean hasMore;
		if (competitorProductList.size() > size) {
			hasMore = true;
			competitorProductList = competitorProductList.subList(0, size);
		} else {
			hasMore = false;
		}

		ByteString offsetIndex = competitorProductList.isEmpty() ? ByteString.EMPTY : competitorProductList.get(competitorProductList.size() - 1)
				.toByteString();

		return Futures.immediateFuture(GetRecommenderCompetitorProductResponse.newBuilder()
				.addAllCompetitorProduct(competitorProductList)
				.setOffsetIndex(offsetIndex)
				.setHasMore(hasMore)
				.build());
	}

	private SceneDAOProtos.RecommenderCategoryExt getCategoryExt(HikariDataSource hikariDataSource2, JedisPool jedisPool2, long companyId, Integer categoryId,
			String competitorProductName, int size) {

		SceneDAOProtos.RecommenderCategoryExt categoryExt = null;
		Jedis jedis = jedisPool.getResource();
		try {
			categoryExt = RecommenderCache.getCategoryExt(jedis, companyId, Collections.singleton(categoryId)).get(categoryId);
		} finally {
			jedis.close();
		}

		if (categoryExt == null) {
			Connection dbConn = null;
			try {
				dbConn = hikariDataSource.getConnection();
				categoryExt = RecommenderDB.getCategoryExt(dbConn,
						companyId,
						Collections.singleton(categoryId),
						CATEGORY_COMPETITOR_PRODUCT_MAX_CACHE_COUNT,
						USER_STATE_LIST).get(categoryId);
			} catch (SQLException e) {
				throw new RuntimeException("获取竞争对手产品出错！");
			} finally {
				DBUtil.closeQuietly(dbConn);
			}

			if (categoryExt != null) {
				jedis = jedisPool.getResource();
				try {
					RecommenderCache.setCategoryExt(jedis, companyId, Collections.singletonMap(categoryId, categoryExt));
				} finally {
					jedis.close();
				}
			}
		}
		return categoryExt;
	}

	public ListenableFuture<GetRecommenderRecommendProductResponse> getRecommenderRecommendProduct(RequestHead head,
			GetRecommenderRecommendProductRequest request) {
		
		final long companyId = head.getSession().getCompanyId();

		final int competitorProductId = request.getCompetitorProductId();
		Map<Integer, List<RecommenderPriceWebUrl>> recommendProductIdPriceUrlsMap = null;
		List<Integer> recommendProductIdList = null;
		Connection dbConn = null;
		try {
			dbConn = hikariDataSource.getConnection();
			recommendProductIdList = RecommenderDB.getCoptorRecommendProductIdListMap(dbConn, companyId, Collections.singleton(competitorProductId))
					.get(competitorProductId);

			if (recommendProductIdList == null || recommendProductIdList.isEmpty()) {
				return Futures.immediateFuture(GetRecommenderRecommendProductResponse.newBuilder().build());
			}
			
			recommendProductIdPriceUrlsMap = RecommenderDB.getRecommendProductIdPriceUrlsMap(dbConn, companyId, recommendProductIdList);

		} catch (SQLException e) {
			throw new RuntimeException("获取推荐产品出错！");
		} finally {
			DBUtil.closeQuietly(dbConn);
		}

		Map<Integer, RecommenderRecommendProduct> recommendProductMap = new HashMap<Integer, RecommenderRecommendProduct>();
		Set<Integer> noCacheRecommendProductIds = new TreeSet<Integer>();
		Jedis jedis = jedisPool.getResource();
		try {
			recommendProductMap.putAll(RecommenderCache.getRecommendProduct(jedis, companyId, recommendProductIdList, noCacheRecommendProductIds));
		} finally {
			jedis.close();
		}

		if (!noCacheRecommendProductIds.isEmpty()) {
			Map<Integer, RecommenderRecommendProduct> temRecommendProductMap = new HashMap<Integer, RecommenderRecommendProduct>();
			try {
				dbConn = hikariDataSource.getConnection();
				temRecommendProductMap.putAll(RecommenderDB.getRecommendProductMap(dbConn, companyId, noCacheRecommendProductIds, USER_STATE_LIST));
			} catch (SQLException e) {
				throw new RuntimeException("获取推荐产品出错！");
			} finally {
				DBUtil.closeQuietly(dbConn);
			}

			if (!temRecommendProductMap.isEmpty()) {
				jedis = jedisPool.getResource();
				try {
					RecommenderCache.setRecommendProduct(jedis, companyId, temRecommendProductMap);
				} finally {
					jedis.close();
				}

				recommendProductMap.putAll(temRecommendProductMap);
			}
		}

		GetRecommenderRecommendProductResponse.Builder responseBuilder = GetRecommenderRecommendProductResponse.newBuilder();
		RecommenderRecommendProduct.Builder recommendProductBuilder = RecommenderRecommendProduct.newBuilder();

		for (int recommendProductId : recommendProductIdList) {
			RecommenderRecommendProduct recommenderRecommendProduct = recommendProductMap.get(recommendProductId);
			if (recommenderRecommendProduct == null) {
				continue;
			}

			if (recommendProductIdPriceUrlsMap != null && recommendProductIdPriceUrlsMap.get(recommendProductId) != null) {
				recommendProductBuilder.clear();
				recommendProductBuilder.mergeFrom(recommenderRecommendProduct);
				recommendProductBuilder.addAllPriceWebUrl(recommendProductIdPriceUrlsMap.get(recommendProductId));
				responseBuilder.addRecommendProduct(recommendProductBuilder.build());
			} else {
				responseBuilder.addRecommendProduct(recommenderRecommendProduct);
			}
		}
		return Futures.immediateFuture(responseBuilder.build());
	}

}
