package com.weizhu.service.scene.tools.recommender;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collections;
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

import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.weizhu.common.db.DBUtil;
import com.weizhu.common.utils.DataPage;
import com.weizhu.proto.AdminProtos.AdminHead;
import com.weizhu.proto.AdminSceneProtos.AddRecommendProdToCompetitorProdRequest;
import com.weizhu.proto.AdminSceneProtos.AddRecommendProdToCompetitorProdResponse;
import com.weizhu.proto.AdminSceneProtos.CreateRecommenderCategoryRequest;
import com.weizhu.proto.AdminSceneProtos.CreateRecommenderCategoryResponse;
import com.weizhu.proto.AdminSceneProtos.CreateRecommenderCompetitorProductRequest;
import com.weizhu.proto.AdminSceneProtos.CreateRecommenderCompetitorProductResponse;
import com.weizhu.proto.AdminSceneProtos.CreateRecommenderRecommendProductPriceWebUrlRequest;
import com.weizhu.proto.AdminSceneProtos.CreateRecommenderRecommendProductPriceWebUrlResponse;
import com.weizhu.proto.AdminSceneProtos.CreateRecommenderRecommendProductRequest;
import com.weizhu.proto.AdminSceneProtos.CreateRecommenderRecommendProductResponse;
import com.weizhu.proto.AdminSceneProtos.DeleteRecommendProdFromCompetitorProdRequest;
import com.weizhu.proto.AdminSceneProtos.DeleteRecommendProdFromCompetitorProdResponse;
import com.weizhu.proto.AdminSceneProtos.DeleteRecommenderRecommendProductPriceWebUrlRequest;
import com.weizhu.proto.AdminSceneProtos.DeleteRecommenderRecommendProductPriceWebUrlResponse;
import com.weizhu.proto.AdminSceneProtos.GetRecommenderCompetitorProductRequest;
import com.weizhu.proto.AdminSceneProtos.GetRecommenderCompetitorProductResponse;
import com.weizhu.proto.AdminSceneProtos.GetRecommenderHomeResponse;
import com.weizhu.proto.AdminSceneProtos.GetRecommenderRecommendProductPriceWebUrlRequest;
import com.weizhu.proto.AdminSceneProtos.GetRecommenderRecommendProductPriceWebUrlResponse;
import com.weizhu.proto.AdminSceneProtos.GetRecommenderRecommendProductRequest;
import com.weizhu.proto.AdminSceneProtos.GetRecommenderRecommendProductResponse;
import com.weizhu.proto.AdminSceneProtos.MigrateRecommenderCompetitorProductRequest;
import com.weizhu.proto.AdminSceneProtos.MigrateRecommenderCompetitorProductResponse;
import com.weizhu.proto.AdminSceneProtos.UpdateRecommenderCategoryRequest;
import com.weizhu.proto.AdminSceneProtos.UpdateRecommenderCategoryResponse;
import com.weizhu.proto.AdminSceneProtos.UpdateRecommenderCategoryStateRequest;
import com.weizhu.proto.AdminSceneProtos.UpdateRecommenderCategoryStateResponse;
import com.weizhu.proto.AdminSceneProtos.UpdateRecommenderCompetitorProductRequest;
import com.weizhu.proto.AdminSceneProtos.UpdateRecommenderCompetitorProductResponse;
import com.weizhu.proto.AdminSceneProtos.UpdateRecommenderCompetitorProductStateRequest;
import com.weizhu.proto.AdminSceneProtos.UpdateRecommenderCompetitorProductStateResponse;
import com.weizhu.proto.AdminSceneProtos.UpdateRecommenderRecommendProductPriceWebUrlRequest;
import com.weizhu.proto.AdminSceneProtos.UpdateRecommenderRecommendProductPriceWebUrlResponse;
import com.weizhu.proto.AdminSceneProtos.UpdateRecommenderRecommendProductRequest;
import com.weizhu.proto.AdminSceneProtos.UpdateRecommenderRecommendProductResponse;
import com.weizhu.proto.AdminSceneProtos.UpdateRecommenderRecommendProductStateRequest;
import com.weizhu.proto.AdminSceneProtos.UpdateRecommenderRecommendProductStateResponse;
import com.weizhu.proto.DiscoverV2Protos.AppUri;
import com.weizhu.proto.DiscoverV2Protos.Audio;
import com.weizhu.proto.DiscoverV2Protos.Document;
import com.weizhu.proto.DiscoverV2Protos.Video;
import com.weizhu.proto.DiscoverV2Protos.WebUrl;
import com.weizhu.proto.SceneProtos;
import com.weizhu.proto.SceneProtos.RecommenderCategory;
import com.weizhu.proto.SceneProtos.RecommenderCompetitorProduct;
import com.weizhu.proto.SceneProtos.RecommenderPriceWebUrl;
import com.weizhu.proto.SceneProtos.RecommenderRecommendProduct;
import com.weizhu.proto.WeizhuProtos.EmptyRequest;
import com.weizhu.service.scene.SceneDAOProtos.RecommenderHome;
import com.zaxxer.hikari.HikariDataSource;

/**
 * 盖帽神器（超值推荐）针对管理后台的业务类
 * 
 * @author zhangjun
 *
 */
public class AdminRecommenderManager {

	private static final Logger logger = LoggerFactory.getLogger(AdminRecommenderManager.class);
	private static final ImmutableList<SceneProtos.State> ADMIN_STATE_LIST = ImmutableList.of(SceneProtos.State.NORMAL, SceneProtos.State.DISABLE);
	private static final ImmutableList<SceneProtos.State> USER_STATE_LIST = ImmutableList.of(SceneProtos.State.NORMAL);

	private final HikariDataSource hikariDataSource;
	private final JedisPool jedisPool;
	@SuppressWarnings("unused")
	private final Executor serviceExecutor;

	@Inject
	public AdminRecommenderManager(HikariDataSource hikariDataSource, JedisPool jedisPool, @Named("service_executor") Executor serviceExecutor) {
		this.hikariDataSource = hikariDataSource;
		this.jedisPool = jedisPool;
		this.serviceExecutor = serviceExecutor;
	}

	public ListenableFuture<GetRecommenderHomeResponse> getRecommenderHome(AdminHead head, EmptyRequest request) {

		final long companyId = head.getCompanyId();
		RecommenderHome recommenderHome = null;
		Connection dbConn = null;
		try {
			dbConn = hikariDataSource.getConnection();
			recommenderHome = RecommenderDB.getRecommenderHome(dbConn, companyId, ADMIN_STATE_LIST);
		} catch (SQLException ex) {
			throw new RuntimeException("db failed");
		} finally {
			DBUtil.closeQuietly(dbConn);
		}

		GetRecommenderHomeResponse.Builder responseBuilder = GetRecommenderHomeResponse.newBuilder();
		if (recommenderHome != null) {
			responseBuilder.addAllCategory(recommenderHome.getRecommenderCategoryList());
		}
		return Futures.immediateFuture(responseBuilder.build());
	}

	public ListenableFuture<CreateRecommenderCategoryResponse> createRecommenderCategory(AdminHead head, CreateRecommenderCategoryRequest request) {

		final long companyId = head.getCompanyId();

		final String categoryName = request.getCategoryName();
		final String imageName = request.hasImageName() ? request.getImageName() : null;
		final String categoryDesc = request.hasCategoryDesc() ? request.getCategoryDesc() : null;
		final Integer parentCategoryId = request.hasParentCategoryId() ? request.getParentCategoryId() : null;
		if (categoryName.length() > 191) {
			return Futures.immediateFuture(CreateRecommenderCategoryResponse.newBuilder()
					.setResult(CreateRecommenderCategoryResponse.Result.FAIL_CATEGORY_NAME_INVALID)
					.setFailText("分类名称长度超出限制！")
					.build());
		}
		if (imageName != null && imageName.length() > 191) {
			return Futures.immediateFuture(CreateRecommenderCategoryResponse.newBuilder()
					.setResult(CreateRecommenderCategoryResponse.Result.FAIL_CATEGORY_IMAGE_INVALID)
					.setFailText("图片名称长度超出限制！")
					.build());
		}
		if (categoryDesc != null && categoryDesc.length() > 191) {
			return Futures.immediateFuture(CreateRecommenderCategoryResponse.newBuilder()
					.setResult(CreateRecommenderCategoryResponse.Result.FAIL_CATEGORY_DESC_INVALID)
					.setFailText("分类描述长度超出限制！")
					.build());
		}

		long adminId = head.getSession().getAdminId();
		int currentTime = (int) (System.currentTimeMillis() / 1000L);

		RecommenderCategory.Builder categoryBuilder = RecommenderCategory.newBuilder()
				.setCategoryId(0)
				.setCategoryName(categoryName)
				.setIsLeafCategory(true)
				.setState(SceneProtos.State.NORMAL)
				.setCreateAdminId(adminId)
				.setCreateTime(currentTime);

		if (imageName != null) {
			categoryBuilder.setImageName(imageName);
		}
		if (categoryDesc != null) {
			categoryBuilder.setCategoryDesc(categoryDesc);
		}

		Integer categoryId = null;
		Connection dbConn = null;
		try {
			dbConn = hikariDataSource.getConnection();

			// 分类不存在，删除的和作废的分类下不允许创建子分类
			if (RecommenderDB.getCategoryMapByCategoryIds(dbConn, companyId, Collections.singleton(parentCategoryId), USER_STATE_LIST) == null) {
				return Futures.immediateFuture(CreateRecommenderCategoryResponse.newBuilder()
						.setResult(CreateRecommenderCategoryResponse.Result.FAIL_CATEGORY_ID_NOT_EXIST)
						.setFailText("父分类不可用！")
						.build());
			}

			Set<Integer> childrenCategoryIds = RecommenderDB.getChildrenCategoryIds(dbConn, companyId, Collections.singleton(parentCategoryId), ADMIN_STATE_LIST)
					.get(parentCategoryId);

			Set<Integer> competitorProductIds = RecommenderDB.getCompProdIdsByCategoryIds(dbConn,
					companyId,
					Collections.singleton(parentCategoryId),
					ADMIN_STATE_LIST);
			categoryId = RecommenderDB.insertCategory(dbConn, companyId, categoryBuilder.build());

			RecommenderDB.updateCompetitorProductCategory(dbConn, companyId, categoryId, competitorProductIds, adminId, currentTime);

			if (childrenCategoryIds == null || childrenCategoryIds.isEmpty()) {
				RecommenderDB.updateCategoryIsLeafCategory(dbConn, companyId, parentCategoryId, false, adminId, currentTime);
			}
		} catch (SQLException ex) {
			throw new RuntimeException("db failed");
		} finally {
			DBUtil.closeQuietly(dbConn);
		}

		if (categoryId == null) {
			return Futures.immediateFuture(CreateRecommenderCategoryResponse.newBuilder()
					.setResult(CreateRecommenderCategoryResponse.Result.FAIL_UNKNOWN)
					.setFailText("创建分类失败！")
					.build());
		}

		// 清理缓存
		Jedis jedis = jedisPool.getResource();
		try {
			RecommenderCache.delRecommenderHome(jedis, Collections.singleton(companyId));
			RecommenderCache.delCategoryExt(jedis, companyId, Arrays.<Integer> asList(categoryId, parentCategoryId));
		} finally {
			jedis.close();
		}

		logger.info("Create Recommender Category end, adminId:" + head.getSession().getAdminId());

		return Futures.immediateFuture(CreateRecommenderCategoryResponse.newBuilder()
				.setResult(CreateRecommenderCategoryResponse.Result.SUCC)
				.setCategoryId(categoryId)
				.build());
	}

	public ListenableFuture<UpdateRecommenderCategoryResponse> updateRecommenderCategory(AdminHead head, UpdateRecommenderCategoryRequest request) {
		
		final long companyId = head.getCompanyId();

		final int categoryId = request.getCategoryId();
		final String categoryName = request.getCategoryName();
		final String imageName = request.hasImageName() ? request.getImageName() : null;
		final String categoryDesc = request.hasCategoryDesc() ? request.getCategoryDesc() : null;
		if (categoryName.length() > 191) {
			return Futures.immediateFuture(UpdateRecommenderCategoryResponse.newBuilder()
					.setResult(UpdateRecommenderCategoryResponse.Result.FAIL_CATEGORY_NAME_INVALID)
					.setFailText("分类名称长度超出限制！")
					.build());
		}
		if (imageName != null && imageName.length() > 191) {
			return Futures.immediateFuture(UpdateRecommenderCategoryResponse.newBuilder()
					.setResult(UpdateRecommenderCategoryResponse.Result.FAIL_CATEGORY_IMAGE_INVALID)
					.setFailText("图片名称长度超出限制！")
					.build());
		}
		if (categoryDesc != null && categoryDesc.length() > 191) {
			return Futures.immediateFuture(UpdateRecommenderCategoryResponse.newBuilder()
					.setResult(UpdateRecommenderCategoryResponse.Result.FAIL_CATEGORY_DESC_INVALID)
					.setFailText("分类描述长度超出限制！")
					.build());
		}

		// pan duan categoryId
		Connection dbConn = null;
		try {
			dbConn = hikariDataSource.getConnection();
			RecommenderDB.updateCategory(dbConn,
					companyId,
					categoryId,
					categoryName,
					imageName,
					categoryDesc,
					head.getSession().getAdminId(),
					(int) (System.currentTimeMillis() / 1000L));
		} catch (SQLException ex) {
			throw new RuntimeException("db failed");
		} finally {
			DBUtil.closeQuietly(dbConn);
		}

		// 清理缓存
		Jedis jedis = jedisPool.getResource();
		try {
			RecommenderCache.delRecommenderHome(jedis, Collections.singleton(companyId));
		} finally {
			jedis.close();
		}

		logger.info("Update Recommender Category end, adminId:" + head.getSession().getAdminId());

		return Futures.immediateFuture(UpdateRecommenderCategoryResponse.newBuilder()
				.setResult(UpdateRecommenderCategoryResponse.Result.SUCC)
				.build());

	}

	public ListenableFuture<UpdateRecommenderCategoryStateResponse> updateRecommenderCategoryState(AdminHead head,
			UpdateRecommenderCategoryStateRequest request) {

		final long companyId = head.getCompanyId();

		final int categoryId = request.getCategoryId();

		Set<Integer> categoryIds = new TreeSet<Integer>();
		categoryIds.add(categoryId);

		Connection dbConn = null;
		try {
			dbConn = hikariDataSource.getConnection();

			RecommenderCategory category = RecommenderDB.getCategoryMapByCategoryIds(dbConn, companyId, Collections.singleton(categoryId), ADMIN_STATE_LIST)
					.get(categoryId);
			if (category == null) {
				return Futures.immediateFuture(UpdateRecommenderCategoryStateResponse.newBuilder()
						.setResult(UpdateRecommenderCategoryStateResponse.Result.FAIL_CATEGORY_ID_NOT_EXIST)
						.setFailText("该分类不存在！")
						.build());
			}

			long adminId = head.getSession().getAdminId();
			int currentTime = (int) (System.currentTimeMillis() / 1000L);

			Set<Integer> parentCategoryIds = new HashSet<Integer>();
			parentCategoryIds.add(categoryId);
			while (true) {
				Map<Integer, Set<Integer>> parentCategoryIdChildrenCategoryIdsMap = RecommenderDB.getChildrenCategoryIds(dbConn,
						companyId,
						parentCategoryIds,
						ADMIN_STATE_LIST);
				if (parentCategoryIdChildrenCategoryIdsMap.isEmpty()) {
					break;
				}

				parentCategoryIds.clear();
				for (Set<Integer> childrenSceneIds : parentCategoryIdChildrenCategoryIdsMap.values()) {
					parentCategoryIds.addAll(childrenSceneIds);
				}
				categoryIds.addAll(parentCategoryIds);
			}

			Set<Integer> competitorProductIds = RecommenderDB.getCompProdIdsByCategoryIds(dbConn, companyId, categoryIds, ADMIN_STATE_LIST);
			// 当状态为DELETE时,若分类下有子分类不能删除,若分类下有条目也不能删除
			if (SceneProtos.State.DELETE.equals(request.getState())) {

				if (categoryIds.size() > 1) {
					return Futures.immediateFuture(UpdateRecommenderCategoryStateResponse.newBuilder()
							.setResult(UpdateRecommenderCategoryStateResponse.Result.FAIL_CATEGORY_IS_NOT_LEAF_CATEGORY)
							.setFailText("该分类不是叶子分类，不能删除！")
							.build());
				}
				if (competitorProductIds.size() > 1) {
					return Futures.immediateFuture(UpdateRecommenderCategoryStateResponse.newBuilder()
							.setResult(UpdateRecommenderCategoryStateResponse.Result.FAIL_CATEGORY_EXISTED_PRODUCT)
							.setFailText("分类下存在产品，不能删除！")
							.build());
				}
			} else {
				// 当状态为DISABLE和NORMAL时更新其子节点的状态一起更新
				RecommenderDB.updateCategoryState(dbConn, companyId, categoryIds, request.getState(), adminId, currentTime);
				// 分类下所有帖子状态都需要更新
				RecommenderDB.updateCompetitorProductState(dbConn, companyId, competitorProductIds, request.getState(), adminId, currentTime);
			}
			// 更新父节点的is_leaf_category
			if (category.hasParentCategoryId()) {
				Set<Integer> childrenCategoryIds = RecommenderDB.getChildrenCategoryIds(dbConn,
						companyId,
						Collections.singleton(category.getParentCategoryId()),
						ADMIN_STATE_LIST).get(category.getParentCategoryId());

				if (childrenCategoryIds == null) {
					childrenCategoryIds = Collections.emptySet();
				}

				Boolean isLeafCategory = null;
				if (SceneProtos.State.DELETE.equals(request.getState()) && childrenCategoryIds.isEmpty()) {
					isLeafCategory = true;
				} else {
					isLeafCategory = false;
				}
				RecommenderDB.updateCategoryIsLeafCategory(dbConn, companyId, category.getParentCategoryId(), isLeafCategory, adminId, currentTime);
			}

		} catch (SQLException ex) {
			throw new RuntimeException("db failed");
		} finally {
			DBUtil.closeQuietly(dbConn);
		}

		// 清理缓存
		Jedis jedis = jedisPool.getResource();
		try {
			RecommenderCache.delRecommenderHome(jedis, Collections.singleton(companyId));
			RecommenderCache.delCategoryExt(jedis, companyId, categoryIds);
		} finally {
			jedis.close();
		}

		logger.info("Update Recommender Category State end, adminId:" + head.getSession().getAdminId());

		return Futures.immediateFuture(UpdateRecommenderCategoryStateResponse.newBuilder()
				.setResult(UpdateRecommenderCategoryStateResponse.Result.SUCC)
				.build());
	}

	public ListenableFuture<MigrateRecommenderCompetitorProductResponse> migrateRecommenderCompetitorProduct(AdminHead head,
			MigrateRecommenderCompetitorProductRequest request) {
		
		final long companyId = head.getCompanyId();

		final int categoryId = request.getCategoryId();

		Set<Integer> categoryIds = new TreeSet<Integer>();
		categoryIds.add(categoryId);

		Connection dbConn = null;
		try {
			dbConn = hikariDataSource.getConnection();

			if (RecommenderDB.getCategoryMapByCategoryIds(dbConn, companyId, Collections.singleton(categoryId), ADMIN_STATE_LIST).get(categoryId) == null) {
				return Futures.immediateFuture(MigrateRecommenderCompetitorProductResponse.newBuilder()
						.setResult(MigrateRecommenderCompetitorProductResponse.Result.FAIL_CATEGORY_ID_NOT_EXIST)
						.setFailText("该分类不存在！")
						.build());
			}

			categoryIds.addAll(RecommenderDB.getCategoryIdsByCompetitorProductIds(dbConn, companyId, request.getCompetitorProductIdList()));

			RecommenderDB.updateCompetitorProductCategory(dbConn,
					companyId,
					categoryId,
					request.getCompetitorProductIdList(),
					head.getSession().getAdminId(),
					(int) (System.currentTimeMillis() / 1000L));

		} catch (SQLException ex) {
			throw new RuntimeException("db failed");
		} finally {
			DBUtil.closeQuietly(dbConn);
		}

		// 清理缓存
		Jedis jedis = jedisPool.getResource();
		try {
			RecommenderCache.delCategoryExt(jedis, companyId, categoryIds);
		} finally {
			jedis.close();
		}

		logger.info("Migrate Recommender Competitor Product end, adminId:" + head.getSession().getAdminId());

		return Futures.immediateFuture(MigrateRecommenderCompetitorProductResponse.newBuilder()
				.setResult(MigrateRecommenderCompetitorProductResponse.Result.SUCC)
				.build());
	}

	public ListenableFuture<GetRecommenderCompetitorProductResponse> getRecommenderCompetitorProduct(AdminHead head,
			GetRecommenderCompetitorProductRequest request) {

		final long companyId = head.getCompanyId();

		final Integer categoryId = request.hasCategoryId() ? request.getCategoryId() : null;
		final String competitorProductName = request.hasCompetitorProductName() ? request.getCompetitorProductName() : null;
		final Integer start = request.hasStart() ? request.getStart() : null;
		final int length = request.getLength();

		Set<Integer> categoryIds = new TreeSet<Integer>();
		Map<Integer, RecommenderCategory> categoryMap = new HashMap<Integer, RecommenderCategory>();
		DataPage<RecommenderCompetitorProduct> dataPage = null;
		Connection dbConn = null;
		try {
			dbConn = hikariDataSource.getConnection();
			dataPage = RecommenderDB.getCompetitorProduct(dbConn, companyId, categoryId, competitorProductName, start, length, ADMIN_STATE_LIST);
			
			if (dataPage == null) {
				return Futures.immediateFuture(GetRecommenderCompetitorProductResponse.newBuilder().setFilteredSize(0).setTotalSize(0).build());
			}
			
			for(RecommenderCompetitorProduct competitorProduct : dataPage.dataList()){
				categoryIds.add(competitorProduct.getCategoryId());
			}
			
			Set<Integer> tmpCategoryIds = new TreeSet<Integer>();
			tmpCategoryIds.addAll(categoryIds);
			while(!tmpCategoryIds.isEmpty()){
				tmpCategoryIds = RecommenderDB.getParentCategoryIds(dbConn, companyId, tmpCategoryIds);
				categoryIds.addAll(tmpCategoryIds);
			}
			
			categoryMap = RecommenderDB.getCategoryMapByCategoryIds(dbConn, companyId, categoryIds, ADMIN_STATE_LIST);
		} catch (SQLException ex) {
			throw new RuntimeException("db failed");
		} finally {
			DBUtil.closeQuietly(dbConn);
		}

		return Futures.immediateFuture(GetRecommenderCompetitorProductResponse.newBuilder()
				.addAllCompetitorProduct(dataPage.dataList())
				.setFilteredSize(dataPage.filteredSize())
				.setTotalSize(dataPage.totalSize())
				.addAllRefCategory(categoryMap.values())
				.build());
	}

	public ListenableFuture<CreateRecommenderCompetitorProductResponse> createRecommenderCompetitorProduct(AdminHead head,
			CreateRecommenderCompetitorProductRequest request) {

		final long companyId = head.getCompanyId();

		final String competitorProductName = request.getCompetitorProductName();
		final String imageName = request.getImageName();
		final int categoryId = request.getCategoryId();
		final Integer allowModelId = request.hasAllowModelId() ? request.getAllowModelId() : null;
		if (competitorProductName.length() > 191) {
			return Futures.immediateFuture(CreateRecommenderCompetitorProductResponse.newBuilder()
					.setResult(CreateRecommenderCompetitorProductResponse.Result.FAIL_COMPETITOR_PRODUCT_NAME_INVALID)
					.setFailText("竞品名称长度超出限制！")
					.build());
		}
		if (imageName != null && imageName.length() > 191) {
			return Futures.immediateFuture(CreateRecommenderCompetitorProductResponse.newBuilder()
					.setResult(CreateRecommenderCompetitorProductResponse.Result.FAIL_PRODUCT_IMAGE_INVALID)
					.setFailText("图片长度超出限制！")
					.build());
		}
		long adminId = head.getSession().getAdminId();
		int currentTime = (int) (System.currentTimeMillis() / 1000L);

		RecommenderCompetitorProduct.Builder competitorProduct = RecommenderCompetitorProduct.newBuilder()
				.setCompetitorProductId(0)
				.setCompetitorProductName(competitorProductName)
				.setImageName(imageName)
				.setCategoryId(categoryId)
				.setState(SceneProtos.State.NORMAL)
				.setCreateAdminId(adminId)
				.setCreateTime(currentTime);

		if (allowModelId != null) {
			competitorProduct.setAllowModelId(allowModelId);
		}

		Integer competitorProductId = null;
		Connection dbConn = null;
		try {
			dbConn = hikariDataSource.getConnection();

			if (RecommenderDB.getCategoryMapByCategoryIds(dbConn, companyId, Collections.singleton(categoryId), ADMIN_STATE_LIST).get(categoryId) == null) {
				return Futures.immediateFuture(CreateRecommenderCompetitorProductResponse.newBuilder()
						.setResult(CreateRecommenderCompetitorProductResponse.Result.FAIL_CATEGORY_ID_NOT_EXIST)
						.setFailText("该分类不存在！")
						.build());
			}

			competitorProductId = RecommenderDB.insertCompetitorProduct(dbConn, companyId, competitorProduct.build());

			if (competitorProductId == null) {
				return Futures.immediateFuture(CreateRecommenderCompetitorProductResponse.newBuilder()
						.setResult(CreateRecommenderCompetitorProductResponse.Result.FAIL_UNKNOWN)
						.setFailText("创建竞品出错！")
						.build());
			}

			RecommenderDB.insertRecommendProdToCompetitorProd(dbConn, companyId, competitorProductId, request.getRecommendProductIdList(), adminId, currentTime);
		} catch (SQLException ex) {
			throw new RuntimeException("db failed");
		} finally {
			DBUtil.closeQuietly(dbConn);
		}
		// 清理缓存
		Jedis jedis = jedisPool.getResource();
		try {
			RecommenderCache.delCategoryExt(jedis, companyId, Collections.singleton(categoryId));
		} finally {
			jedis.close();
		}

		logger.info("Create Recommender Competitor Product end, adminId:" + head.getSession().getAdminId());

		return Futures.immediateFuture(CreateRecommenderCompetitorProductResponse.newBuilder()
				.setResult(CreateRecommenderCompetitorProductResponse.Result.SUCC)
				.build());
	}

	public ListenableFuture<UpdateRecommenderCompetitorProductResponse> updateRecommenderCompetitorProduct(AdminHead head,
			UpdateRecommenderCompetitorProductRequest request) {
		
		final long companyId = head.getCompanyId();

		final int competitorProductId = request.getCompetitorProductId();
		final String competitorProductName = request.getCompetitorProductName();
		final String imageName = request.getImageName();
		final int categoryId = request.getCategoryId();
		final Integer allowModelId = request.hasAllowModelId() ? request.getAllowModelId() : null;
		if (competitorProductName.length() > 191) {
			return Futures.immediateFuture(UpdateRecommenderCompetitorProductResponse.newBuilder()
					.setResult(UpdateRecommenderCompetitorProductResponse.Result.FAIL_COMPETITOR_PRODUCT_NAME_INVALID)
					.setFailText("竞品名称长度超出限制！")
					.build());
		}
		if (imageName.length() > 191) {
			return Futures.immediateFuture(UpdateRecommenderCompetitorProductResponse.newBuilder()
					.setResult(UpdateRecommenderCompetitorProductResponse.Result.FAIL_PRODUCT_IMAGE_INVALID)
					.setFailText("图片长度超出限制！")
					.build());
		}

		Connection dbConn = null;
		try {
			dbConn = hikariDataSource.getConnection();

			if (RecommenderDB.getCompetitorProductMap(dbConn, companyId, Collections.singleton(competitorProductId), ADMIN_STATE_LIST).get(competitorProductId) == null) {
				return Futures.immediateFuture(UpdateRecommenderCompetitorProductResponse.newBuilder()
						.setResult(UpdateRecommenderCompetitorProductResponse.Result.FAIL_COMPETITOR_PRODUCT_ID_INVALID)
						.setFailText("该竞品不存在！")
						.build());
			}
			if (RecommenderDB.getCategoryMapByCategoryIds(dbConn, companyId, Collections.singleton(categoryId), ADMIN_STATE_LIST).get(categoryId) == null) {
				return Futures.immediateFuture(UpdateRecommenderCompetitorProductResponse.newBuilder()
						.setResult(UpdateRecommenderCompetitorProductResponse.Result.FAIL_CATEGORY_ID_NOT_EXIST)
						.setFailText("该分类不存在！")
						.build());
			}

			long adminId = head.getSession().getAdminId();
			int currentTime = (int) (System.currentTimeMillis() / 1000L);

			RecommenderDB.updateCompetitorProduct(dbConn,
					companyId,
					competitorProductId,
					competitorProductName,
					imageName,
					categoryId,
					allowModelId,
					adminId,
					currentTime);

			RecommenderDB.deleteRecommendProdFromCompetitorProd(dbConn, companyId, Collections.singleton(competitorProductId));
			RecommenderDB.insertRecommendProdToCompetitorProd(dbConn, companyId, competitorProductId, request.getRecommendProductIdList(), adminId, currentTime);

		} catch (SQLException ex) {
			throw new RuntimeException("db failed");
		} finally {
			DBUtil.closeQuietly(dbConn);
		}

		// 清理缓存
		Jedis jedis = jedisPool.getResource();
		try {
			RecommenderCache.delCategoryExt(jedis, companyId, Collections.singleton(categoryId));
		} finally {
			jedis.close();
		}

		logger.info("Update Recommender Competitor Product end, adminId:" + head.getSession().getAdminId());

		return Futures.immediateFuture(UpdateRecommenderCompetitorProductResponse.newBuilder()
				.setResult(UpdateRecommenderCompetitorProductResponse.Result.SUCC)
				.build());
	}

	public ListenableFuture<UpdateRecommenderCompetitorProductStateResponse> updateRecommenderCompetitorProductState(AdminHead head,
			UpdateRecommenderCompetitorProductStateRequest request) {
		
		final long companyId = head.getCompanyId();

		Set<Integer> categoryIds = new TreeSet<Integer>();

		Connection dbConn = null;
		try {
			dbConn = hikariDataSource.getConnection();

			categoryIds.addAll(RecommenderDB.getCategoryIdsByCompetitorProductIds(dbConn, companyId, request.getCompetitorProductIdList()));

			RecommenderDB.updateCompetitorProductState(dbConn, companyId, request.getCompetitorProductIdList(), request.getState(), head.getSession()
					.getAdminId(), (int) (System.currentTimeMillis() / 1000L));

			if (SceneProtos.State.DELETE.equals(request.getState())) {
				RecommenderDB.deleteRecommendProdFromCompetitorProd(dbConn, companyId, request.getCompetitorProductIdList());
			}
		} catch (SQLException ex) {
			throw new RuntimeException("db failed");
		} finally {
			DBUtil.closeQuietly(dbConn);
		}

		// 清理缓存
		Jedis jedis = jedisPool.getResource();
		try {
			RecommenderCache.delCategoryExt(jedis, companyId, categoryIds);
		} finally {
			jedis.close();
		}

		logger.info("Update Recommender Competitor Product State end, adminId:" + head.getSession().getAdminId());

		return Futures.immediateFuture(UpdateRecommenderCompetitorProductStateResponse.newBuilder()
				.setResult(UpdateRecommenderCompetitorProductStateResponse.Result.SUCC)
				.build());
	}

	public ListenableFuture<GetRecommenderRecommendProductResponse> getRecommenderRecommendProduct(AdminHead head,
			GetRecommenderRecommendProductRequest request) {
		
		final long companyId = head.getCompanyId();

		final Integer competitorProductId = request.hasCompetitorProductId() ? request.getCompetitorProductId() : null;
		final String recommendProductName = request.hasRecommendProductName() ? request.getRecommendProductName() : null;
		final Integer start = request.hasStart() ? request.getStart() : null;
		final int length = request.getLength();

		Map<Integer, List<RecommenderPriceWebUrl>> recommendProductIdPriceUrlsMap = null;
		DataPage<Integer> dataPage = null;

		Connection dbConn = null;
		try {
			dbConn = hikariDataSource.getConnection();
			dataPage = RecommenderDB.getRecommendProduct(dbConn, companyId, competitorProductId, recommendProductName, start, length, ADMIN_STATE_LIST);

			if (dataPage == null) {
				return Futures.immediateFuture(GetRecommenderRecommendProductResponse.newBuilder().setFilteredSize(0).setTotalSize(0).build());
			}
			recommendProductIdPriceUrlsMap = RecommenderDB.getRecommendProductIdPriceUrlsMap(dbConn, companyId, dataPage.dataList());
		} catch (SQLException ex) {
			throw new RuntimeException("db failed");
		} finally {
			DBUtil.closeQuietly(dbConn);
		}

		Map<Integer, RecommenderRecommendProduct> recommendProductMap = new HashMap<Integer, RecommenderRecommendProduct>();
		Set<Integer> noCacheRecommendProductIds = new TreeSet<Integer>();
		Jedis jedis = jedisPool.getResource();
		try {
			recommendProductMap.putAll(RecommenderCache.getRecommendProduct(jedis, companyId, dataPage.dataList(), noCacheRecommendProductIds));
		} finally {
			jedis.close();
		}

		if (!noCacheRecommendProductIds.isEmpty()) {
			Map<Integer, RecommenderRecommendProduct> temRecommendProductMap = new HashMap<Integer, RecommenderRecommendProduct>();
			try {
				dbConn = hikariDataSource.getConnection();
				temRecommendProductMap.putAll(RecommenderDB.getRecommendProductMap(dbConn, companyId, noCacheRecommendProductIds, ADMIN_STATE_LIST));
			} catch (SQLException e) {
				throw new RuntimeException("获取竞争对手产品出错！");
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

		for (int recommendProductId : dataPage.dataList()) {
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
		return Futures.immediateFuture(responseBuilder.setFilteredSize(dataPage.filteredSize()).setTotalSize(dataPage.totalSize()).build());
	}

	public ListenableFuture<CreateRecommenderRecommendProductResponse> createRecommenderRecommendProduct(AdminHead head,
			CreateRecommenderRecommendProductRequest request) {
		
		final long companyId = head.getCompanyId();

		final String recommendProductName = request.getRecommendProductName();
		final String imageName = request.getImageName();
		final String recommendProductDesc = request.getRecommendProductDesc();
		final WebUrl webUrl = request.hasWebUrl() ? request.getWebUrl() : null;
		final Document document = request.hasDocument() ? request.getDocument() : null;
		final Video video = request.hasVideo() ? request.getVideo() : null;
		final Audio audio = request.hasAudio() ? request.getAudio() : null;
		final AppUri appUri = request.hasAppUri() ? request.getAppUri() : null;
		final Integer allowModelId = request.hasAllowModelId() ? request.getAllowModelId() : null;

		if (recommendProductName.length() > 191) {
			return Futures.immediateFuture(CreateRecommenderRecommendProductResponse.newBuilder()
					.setResult(CreateRecommenderRecommendProductResponse.Result.FAIL_RECOMMEND_PRODUCT_NAME_INVALID)
					.setFailText("推荐产品的名称长度超出限制！")
					.build());
		}
		if (imageName != null && imageName.length() > 191) {
			return Futures.immediateFuture(CreateRecommenderRecommendProductResponse.newBuilder()
					.setResult(CreateRecommenderRecommendProductResponse.Result.FAIL_PRODUCT_IMAGE_INVALID)
					.setFailText("图片长度超出限制！")
					.build());
		}
		if (recommendProductDesc.length() > 191) {
			return Futures.immediateFuture(CreateRecommenderRecommendProductResponse.newBuilder()
					.setResult(CreateRecommenderRecommendProductResponse.Result.FAIL_RECOMMEND_PRODUCT_DESC_INVALID)
					.setFailText("推荐产品的描述长度超出限制！")
					.build());
		}

		if (webUrl != null && webUrl.getWebUrl().length() > 191) {
			return Futures.immediateFuture(CreateRecommenderRecommendProductResponse.newBuilder()
					.setResult(CreateRecommenderRecommendProductResponse.Result.FAIL_WEB_URL_INVALID)
					.setFailText("web url长度超出范围！")
					.build());
		}

		if (appUri != null && appUri.getAppUri().length() > 191) {
			return Futures.immediateFuture(CreateRecommenderRecommendProductResponse.newBuilder()
					.setResult(CreateRecommenderRecommendProductResponse.Result.FAIL_APP_URI_INVALID)
					.setFailText("app uri长度超出范围！")
					.build());
		}

		if (document != null && (document.getDocumentUrl().length() > 191 || document.getDocumentType().length() > 191)) {
			return Futures.immediateFuture(CreateRecommenderRecommendProductResponse.newBuilder()
					.setResult(CreateRecommenderRecommendProductResponse.Result.FAIL_DOCUMENT_INVALID)
					.setFailText("文件uri过长或类型名过长！")
					.build());
		}

		if (video != null && (video.getVideoUrl().length() > 191 || video.getVideoType().length() > 191)) {
			return Futures.immediateFuture(CreateRecommenderRecommendProductResponse.newBuilder()
					.setResult(CreateRecommenderRecommendProductResponse.Result.FAIL_VIDEO_INVALID)
					.setFailText("视频uri过长或类型名过长！")
					.build());
		}

		if (audio != null && (audio.getAudioUrl().length() > 191 || audio.getAudioType().length() > 191)) {
			return Futures.immediateFuture(CreateRecommenderRecommendProductResponse.newBuilder()
					.setResult(CreateRecommenderRecommendProductResponse.Result.FAIL_AUDIO_INVALID)
					.setFailText("音频url过长或类型名过长！")
					.build());
		}

		long adminId = head.getSession().getAdminId();
		int currentTime = (int) (System.currentTimeMillis() / 1000L);

		RecommenderRecommendProduct.Builder builder = RecommenderRecommendProduct.newBuilder()
				.setRecommendProductId(0)
				.setRecommendProductName(recommendProductName)
				.setRecommendProductDesc(recommendProductDesc)
				.setCreateAdminId(head.getSession().getAdminId())
				.setCreateAdminId(adminId)
				.setCreateTime(currentTime)
				.setState(SceneProtos.State.NORMAL);

		if (imageName != null) {
			builder.setImageName(imageName);
		}
		if (allowModelId != null) {
			builder.setAllowModelId(allowModelId);
		}
		if (webUrl != null) {
			builder.setWebUrl(webUrl);
		}
		if (appUri != null) {
			builder.setAppUri(appUri);
		}
		if (document != null) {
			builder.setDocument(document);
		}
		if (video != null) {
			builder.setVideo(video);
		}
		if (audio != null) {
			builder.setAudio(audio);
		}

		Integer recommendProductId = null;
		Connection dbConn = null;
		try {
			dbConn = hikariDataSource.getConnection();
			recommendProductId = RecommenderDB.insertRecommendProduct(dbConn, companyId, builder.build());

			RecommenderDB.insertRecommendProductPriceUrl(dbConn, companyId, recommendProductId, request.getPriceWebUrlCreateConditionList(), adminId, currentTime);
		} catch (SQLException ex) {
			throw new RuntimeException("db failed");
		} finally {
			DBUtil.closeQuietly(dbConn);
		}

		if (recommendProductId == null) {
			return Futures.immediateFuture(CreateRecommenderRecommendProductResponse.newBuilder()
					.setResult(CreateRecommenderRecommendProductResponse.Result.FAIL_UNKNOWN)
					.setFailText("创建推荐产品出错！！")
					.build());
		}

		// 清理缓存
		Jedis jedis = jedisPool.getResource();
		try {
			RecommenderCache.delRecommendProduct(jedis, companyId, Collections.singleton(recommendProductId));
		} finally {
			jedis.close();
		}

		logger.info("Create Recommender Recommend Product end, adminId:" + head.getSession().getAdminId());

		return Futures.immediateFuture(CreateRecommenderRecommendProductResponse.newBuilder()
				.setResult(CreateRecommenderRecommendProductResponse.Result.SUCC)
				.setRecommendProductId(recommendProductId)
				.build());
	}

	public ListenableFuture<UpdateRecommenderRecommendProductResponse> updateRecommenderRecommendProduct(AdminHead head,
			UpdateRecommenderRecommendProductRequest request) {
		
		final long companyId = head.getCompanyId();

		final int recommendProductId = request.getRecommendProductId();
		final String recommendProductName = request.getRecommendProductName();
		final String imageName = request.hasImageName() ? request.getImageName() : null;
		final String recommendProductDesc = request.getRecommendProductDesc();
		final WebUrl webUrl = request.hasWebUrl() ? request.getWebUrl() : null;
		final Document document = request.hasDocument() ? request.getDocument() : null;
		final Video video = request.hasVideo() ? request.getVideo() : null;
		final Audio audio = request.hasAudio() ? request.getAudio() : null;
		final AppUri appUri = request.hasAppUri() ? request.getAppUri() : null;
		final Integer allowModelId = request.hasAllowModelId() ? request.getAllowModelId() : null;

		if (recommendProductName.length() > 191) {
			return Futures.immediateFuture(UpdateRecommenderRecommendProductResponse.newBuilder()
					.setResult(UpdateRecommenderRecommendProductResponse.Result.FAIL_RECOMMEND_PRODUCT_NAME_INVALID)
					.setFailText("推荐产品的名称长度超出限制！")
					.build());
		}
		if (imageName != null && imageName.length() > 191) {
			return Futures.immediateFuture(UpdateRecommenderRecommendProductResponse.newBuilder()
					.setResult(UpdateRecommenderRecommendProductResponse.Result.FAIL_PRODUCT_IMAGE_INVALID)
					.setFailText("图片长度超出限制！")
					.build());
		}
		if (recommendProductDesc.length() > 191) {
			return Futures.immediateFuture(UpdateRecommenderRecommendProductResponse.newBuilder()
					.setResult(UpdateRecommenderRecommendProductResponse.Result.FAIL_RECOMMEND_PRODUCT_DESC_INVALID)
					.setFailText("推荐产品的描述长度超出限制！")
					.build());
		}

		if (webUrl != null && webUrl.getWebUrl().length() > 191) {
			return Futures.immediateFuture(UpdateRecommenderRecommendProductResponse.newBuilder()
					.setResult(UpdateRecommenderRecommendProductResponse.Result.FAIL_WEB_URL_INVALID)
					.setFailText("web url长度超出范围！")
					.build());
		}

		if (appUri != null && appUri.getAppUri().length() > 191) {
			return Futures.immediateFuture(UpdateRecommenderRecommendProductResponse.newBuilder()
					.setResult(UpdateRecommenderRecommendProductResponse.Result.FAIL_APP_URI_INVALID)
					.setFailText("app uri长度超出范围！")
					.build());
		}

		if (document != null && (document.getDocumentUrl().length() > 191 || document.getDocumentType().length() > 191)) {
			return Futures.immediateFuture(UpdateRecommenderRecommendProductResponse.newBuilder()
					.setResult(UpdateRecommenderRecommendProductResponse.Result.FAIL_DOCUMENT_INVALID)
					.setFailText("文件uri过长或类型名过长！")
					.build());
		}

		if (video != null && (video.getVideoUrl().length() > 191 || video.getVideoType().length() > 191)) {
			return Futures.immediateFuture(UpdateRecommenderRecommendProductResponse.newBuilder()
					.setResult(UpdateRecommenderRecommendProductResponse.Result.FAIL_VIDEO_INVALID)
					.setFailText("视频uri过长或类型名过长！")
					.build());
		}

		if (audio != null && (audio.getAudioUrl().length() > 191 || audio.getAudioType().length() > 191)) {
			return Futures.immediateFuture(UpdateRecommenderRecommendProductResponse.newBuilder()
					.setResult(UpdateRecommenderRecommendProductResponse.Result.FAIL_AUDIO_INVALID)
					.setFailText("音频url过长或类型名过长！")
					.build());
		}

		long adminId = head.getSession().getAdminId();
		int currentTime = (int) (System.currentTimeMillis() / 1000L);

		RecommenderRecommendProduct.Builder builder = RecommenderRecommendProduct.newBuilder()
				.setRecommendProductId(recommendProductId)
				.setRecommendProductName(recommendProductName)
				.setRecommendProductDesc(recommendProductDesc)
				.setCreateAdminId(head.getSession().getAdminId())
				.setCreateAdminId(adminId)
				.setCreateTime(currentTime)
				.setUpdateAdminId(adminId)
				.setUpdateTime(currentTime)
				.setState(SceneProtos.State.NORMAL);

		if (imageName != null) {
			builder.setImageName(imageName);
		}
		if (allowModelId != null) {
			builder.setAllowModelId(allowModelId);
		}
		if (webUrl != null) {
			builder.setWebUrl(webUrl);
		}
		if (appUri != null) {
			builder.setAppUri(appUri);
		}
		if (document != null) {
			builder.setDocument(document);
		}
		if (video != null) {
			builder.setVideo(video);
		}
		if (audio != null) {
			builder.setAudio(audio);
		}

		Connection dbConn = null;
		try {
			dbConn = hikariDataSource.getConnection();

			if (RecommenderDB.getRecommendProductMap(dbConn, companyId, Collections.singleton(recommendProductId), ADMIN_STATE_LIST).get(recommendProductId) == null) {
				return Futures.immediateFuture(UpdateRecommenderRecommendProductResponse.newBuilder()
						.setResult(UpdateRecommenderRecommendProductResponse.Result.FAIL_RECOMMEND_PRODUCT_ID_INVALID)
						.setFailText("该推荐产品不存在！")
						.build());
			}

			RecommenderDB.updateRecommendProduct(dbConn, companyId, builder.build());

			RecommenderDB.deletePriceUrlByRecommendProdIds(dbConn, companyId, Collections.singleton(recommendProductId));
			RecommenderDB.insertRecommendProductPriceUrl(dbConn, companyId, recommendProductId, request.getPriceWebUrlCreateConditionList(), adminId, currentTime);
		} catch (SQLException ex) {
			throw new RuntimeException("db failed");
		} finally {
			DBUtil.closeQuietly(dbConn);
		}

		// 清理缓存
		Jedis jedis = jedisPool.getResource();
		try {
			RecommenderCache.delRecommendProduct(jedis, companyId, Collections.singleton(recommendProductId));
		} finally {
			jedis.close();
		}

		logger.info("Update Recommender Recommend Product end, adminId:" + head.getSession().getAdminId());

		return Futures.immediateFuture(UpdateRecommenderRecommendProductResponse.newBuilder()
				.setResult(UpdateRecommenderRecommendProductResponse.Result.SUCC)
				.build());

	}

	public ListenableFuture<UpdateRecommenderRecommendProductStateResponse> updateRecommenderRecommendProductState(AdminHead head,
			UpdateRecommenderRecommendProductStateRequest request) {

		final long companyId = head.getCompanyId();

		Connection dbConn = null;
		try {
			dbConn = hikariDataSource.getConnection();

			RecommenderDB.updateRecommendProductState(dbConn,
					companyId,
					request.getRecommendProductIdList(),
					request.getState(),
					head.getSession().getAdminId(),
					(int) (System.currentTimeMillis() / 1000L));

			// 删除的时候，要把price_url一起删除
			if (SceneProtos.State.DELETE.equals(request.getState())) {
				RecommenderDB.deletePriceUrlByRecommendProdIds(dbConn, companyId, request.getRecommendProductIdList());
			}
		} catch (SQLException ex) {
			throw new RuntimeException("db failed");
		} finally {
			DBUtil.closeQuietly(dbConn);
		}

		// 清理缓存
		Jedis jedis = jedisPool.getResource();
		try {
			RecommenderCache.delRecommendProduct(jedis, companyId, request.getRecommendProductIdList());
		} finally {
			jedis.close();
		}

		logger.info("Update Recommender Recommend Product State end, adminId:" + head.getSession().getAdminId());

		return Futures.immediateFuture(UpdateRecommenderRecommendProductStateResponse.newBuilder()
				.setResult(UpdateRecommenderRecommendProductStateResponse.Result.SUCC)
				.build());
	}

	public ListenableFuture<AddRecommendProdToCompetitorProdResponse> addRecommendProdToCompetitorProd(AdminHead head,
			AddRecommendProdToCompetitorProdRequest request) {

		final long companyId = head.getCompanyId();

		Connection dbConn = null;
		try {
			dbConn = hikariDataSource.getConnection();

			RecommenderDB.insertRecommendProdToCompetitorProd(dbConn,
					companyId,
					request.getCompetitorProductId(),
					request.getRecommendProductIdList(),
					head.getSession().getAdminId(),
					(int) (System.currentTimeMillis() / 1000L));

		} catch (SQLException ex) {
			throw new RuntimeException("db failed");
		} finally {
			DBUtil.closeQuietly(dbConn);
		}

		logger.info("Add Recommend Product To Competitor Product end, adminId:" + head.getSession().getAdminId());

		return Futures.immediateFuture(AddRecommendProdToCompetitorProdResponse.newBuilder()
				.setResult(AddRecommendProdToCompetitorProdResponse.Result.SUCC)
				.build());
	}

	public ListenableFuture<DeleteRecommendProdFromCompetitorProdResponse> deleteRecommendProdFromCompetitorProd(AdminHead head,
			DeleteRecommendProdFromCompetitorProdRequest request) {

		Connection dbConn = null;
		try {
			dbConn = hikariDataSource.getConnection();

			RecommenderDB.deleteRecommendProdFromCompetitorProd(dbConn, request.getCompetitorProductId(), request.getRecommendProductIdList());

		} catch (SQLException ex) {
			throw new RuntimeException("db failed");
		} finally {
			DBUtil.closeQuietly(dbConn);
		}

		logger.info("Delete Recommend Product From Competitor Product end, adminId:" + head.getSession().getAdminId());

		return Futures.immediateFuture(DeleteRecommendProdFromCompetitorProdResponse.newBuilder()
				.setResult(DeleteRecommendProdFromCompetitorProdResponse.Result.SUCC)
				.build());
	}

	public ListenableFuture<GetRecommenderRecommendProductPriceWebUrlResponse> getRecommenderRecommendProductPriceWebUrl(AdminHead head,
			GetRecommenderRecommendProductPriceWebUrlRequest request) {

		final long companyId = head.getCompanyId();

		final int recommendProductId = request.getRecommendProductId();

		List<RecommenderPriceWebUrl> webUrlList = null;
		Connection dbConn = null;
		try {
			dbConn = hikariDataSource.getConnection();

			webUrlList = RecommenderDB.getRecommendProductIdPriceUrlsMap(dbConn, companyId, Collections.singleton(recommendProductId)).get(recommendProductId);
		} catch (SQLException ex) {
			throw new RuntimeException("db failed");
		} finally {
			DBUtil.closeQuietly(dbConn);
		}
		if (webUrlList == null) {
			return Futures.immediateFuture(GetRecommenderRecommendProductPriceWebUrlResponse.newBuilder().build());
		}
		return Futures.immediateFuture(GetRecommenderRecommendProductPriceWebUrlResponse.newBuilder().addAllPriceWebUrl(webUrlList).build());
	}

	public ListenableFuture<CreateRecommenderRecommendProductPriceWebUrlResponse> createRecommenderRecommendProductPriceWebUrl(AdminHead head,
			CreateRecommenderRecommendProductPriceWebUrlRequest request) {
		
		final long companyId = head.getCompanyId();

		final int recommendProductId = request.getRecommendProductId();

		Connection dbConn = null;
		try {
			dbConn = hikariDataSource.getConnection();

			if (RecommenderDB.getRecommendProductMap(dbConn, companyId, Collections.singleton(recommendProductId), ADMIN_STATE_LIST).get(recommendProductId) == null) {
				return Futures.immediateFuture(CreateRecommenderRecommendProductPriceWebUrlResponse.newBuilder()
						.setResult(CreateRecommenderRecommendProductPriceWebUrlResponse.Result.FAIL_RECOMMEND_PRODUCT_ID_NOT_EXIST)
						.setFailText("该推荐产品不存在！")
						.build());
			}

			RecommenderDB.insertRecommendProductPriceUrl(dbConn,
					companyId,
					recommendProductId,
					request.getPriceWebUrlCreateConditionList(),
					head.getSession().getAdminId(),
					(int) (System.currentTimeMillis() / 1000L));

		} catch (SQLException ex) {
			throw new RuntimeException("db failed");
		} finally {
			DBUtil.closeQuietly(dbConn);
		}

		logger.info("Create Recommender Recommend Product Price Url end, adminId:" + head.getSession().getAdminId());

		return Futures.immediateFuture(CreateRecommenderRecommendProductPriceWebUrlResponse.newBuilder()
				.setResult(CreateRecommenderRecommendProductPriceWebUrlResponse.Result.SUCC)
				.build());
	}

	public ListenableFuture<UpdateRecommenderRecommendProductPriceWebUrlResponse> updateRecommenderRecommendProductPriceWebUrl(AdminHead head,
			UpdateRecommenderRecommendProductPriceWebUrlRequest request) {
		
		final long companyId = head.getCompanyId();

		Connection dbConn = null;
		try {
			dbConn = hikariDataSource.getConnection();
			RecommenderDB.updateRecommendProductPriceUrl(dbConn, companyId, Collections.singleton(request.getPriceWebUrl()));
		} catch (SQLException ex) {
			throw new RuntimeException("db failed");
		} finally {
			DBUtil.closeQuietly(dbConn);
		}

		logger.info("Update Recommender Recommend Product Price Url end, adminId:" + head.getSession().getAdminId());

		return Futures.immediateFuture(UpdateRecommenderRecommendProductPriceWebUrlResponse.newBuilder()
				.setResult(UpdateRecommenderRecommendProductPriceWebUrlResponse.Result.SUCC)
				.build());
	}

	public ListenableFuture<DeleteRecommenderRecommendProductPriceWebUrlResponse> deleteRecommenderRecommendProductPriceWebUrl(AdminHead head,
			DeleteRecommenderRecommendProductPriceWebUrlRequest request) {
		
		final long companyId = head.getCompanyId();

		Connection dbConn = null;
		try {
			dbConn = hikariDataSource.getConnection();
			RecommenderDB.deletePriceUrlByUrlIds(dbConn, companyId, request.getUrlIdList());
		} catch (SQLException ex) {
			throw new RuntimeException("db failed");
		} finally {
			DBUtil.closeQuietly(dbConn);
		}

		logger.info("Delete Recommender Recommend Product Price Url end, adminId:" + head.getSession().getAdminId());

		return Futures.immediateFuture(DeleteRecommenderRecommendProductPriceWebUrlResponse.newBuilder()
				.setResult(DeleteRecommenderRecommendProductPriceWebUrlResponse.Result.SUCC)
				.build());
	}

}
