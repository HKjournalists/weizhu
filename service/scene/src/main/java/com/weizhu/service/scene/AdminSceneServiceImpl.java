package com.weizhu.service.scene;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
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
import com.weizhu.common.db.DBUtil;
import com.weizhu.common.utils.DataPage;
import com.weizhu.proto.AdminDiscoverProtos;
import com.weizhu.proto.AdminDiscoverService;
import com.weizhu.proto.AdminProtos.AdminHead;
import com.weizhu.proto.AdminSceneProtos;
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
import com.weizhu.proto.AdminSceneProtos.CreateSceneItemRequest.CreateItemParameter;
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
import com.weizhu.proto.AdminSceneProtos.GetSceneItemResponse;
import com.weizhu.proto.AdminSceneProtos.MigrateRecommenderCompetitorProductRequest;
import com.weizhu.proto.AdminSceneProtos.MigrateRecommenderCompetitorProductResponse;
import com.weizhu.proto.AdminSceneProtos.SetSceneHomeResponse;
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
import com.weizhu.proto.AdminSceneService;
import com.weizhu.proto.DiscoverV2Protos;
import com.weizhu.proto.SceneProtos;
import com.weizhu.proto.SceneProtos.Item.ItemIndex;
import com.weizhu.proto.WeizhuProtos.EmptyRequest;
import com.weizhu.service.scene.tools.recommender.AdminRecommenderManager;
import com.zaxxer.hikari.HikariDataSource;

/**
 * 场景的针对管理后台的service类
 * 
 * @author zhangjun
 *
 */
public class AdminSceneServiceImpl implements AdminSceneService {

	private static final Logger logger = LoggerFactory.getLogger(AdminSceneServiceImpl.class);
	public static final ImmutableList<SceneProtos.State> ADMIN_STATE_LIST = ImmutableList.of(SceneProtos.State.NORMAL, SceneProtos.State.DISABLE);

	private final HikariDataSource hikariDataSource;
	private final JedisPool jedisPool;
	@SuppressWarnings("unused")
	private final Executor serviceExecutor;
	private final AdminRecommenderManager adminRecommenderManager;
	private final AdminDiscoverService adminDiscoverService;

	@Inject
	public AdminSceneServiceImpl(HikariDataSource hikariDataSource, JedisPool jedisPool, @Named("service_executor") Executor serviceExecutor,
			AdminRecommenderManager adminRecommenderManager, AdminDiscoverService adminDiscoverService) {
		this.hikariDataSource = hikariDataSource;
		this.jedisPool = jedisPool;
		this.serviceExecutor = serviceExecutor;
		this.adminRecommenderManager = adminRecommenderManager;
		this.adminDiscoverService = adminDiscoverService;
	}

	@Override
	public ListenableFuture<AdminSceneProtos.SetSceneHomeResponse> setSceneHome(AdminHead head, AdminSceneProtos.SetSceneHomeRequest request) {
		if (!head.hasCompanyId()) {
			return Futures.immediateFuture(SetSceneHomeResponse.newBuilder()
					.setResult(SetSceneHomeResponse.Result.FAIL_UNKNOWN)
					.setFailText("company_id 参数未填")
					.build());
		}
		final long companyId = head.getCompanyId();

		final String sceneIdOrderStr = request.getSceneIdOrderStr();

		if (sceneIdOrderStr.length() > 65535) { // 2的16次方-1
			return Futures.immediateFuture(AdminSceneProtos.SetSceneHomeResponse.newBuilder()
					.setResult(AdminSceneProtos.SetSceneHomeResponse.Result.FAIL_SCENE_ORDER_STR_INVALID)
					.setFailText("场景ID序列不能超过65535个字符！")
					.build());
		}
		Connection dbConn = null;
		try {
			dbConn = hikariDataSource.getConnection();
			SceneDB.setSceneHome(dbConn, companyId, sceneIdOrderStr);
		} catch (SQLException e) {
			throw new RuntimeException("保存场景信息失败！", e);
		} finally {
			DBUtil.closeQuietly(dbConn);
		}

		Jedis jedis = jedisPool.getResource();
		try {
			SceneCache.delSceneHome(jedis, Collections.singleton(companyId));
		} finally {
			jedis.close();
		}

		logger.info("set Scene Home end, adminId:" + head.getSession().getAdminId());

		return Futures.immediateFuture(AdminSceneProtos.SetSceneHomeResponse.newBuilder()
				.setResult(AdminSceneProtos.SetSceneHomeResponse.Result.SUCC)
				.build());

	}

	@Override
	public ListenableFuture<AdminSceneProtos.GetSceneResponse> getScene(AdminHead head, EmptyRequest request) {
		if (!head.hasCompanyId()) {
			return Futures.immediateFuture(AdminSceneProtos.GetSceneResponse.newBuilder().build());
		}
		final long companyId = head.getCompanyId();

		SceneDAOProtos.SceneHome sceneHome = null;
		Connection dbConn = null;
		try {
			dbConn = hikariDataSource.getConnection();
			sceneHome = SceneDB.getSceneHome(dbConn, companyId, ADMIN_STATE_LIST);
		} catch (SQLException e) {
			throw new RuntimeException("获取场景信息失败！", e);
		} finally {
			DBUtil.closeQuietly(dbConn);
		}
		if (sceneHome == null) {
			return Futures.immediateFuture(AdminSceneProtos.GetSceneResponse.newBuilder().build());
		}
		return Futures.immediateFuture(AdminSceneProtos.GetSceneResponse.newBuilder().addAllScene(sceneHome.getSceneList()).build());
	}

	@Override
	public ListenableFuture<AdminSceneProtos.CreateSceneResponse> createScene(AdminHead head, AdminSceneProtos.CreateSceneRequest request) {
		if (!head.hasCompanyId()) {
			return Futures.immediateFuture(AdminSceneProtos.CreateSceneResponse.newBuilder()
					.setResult(AdminSceneProtos.CreateSceneResponse.Result.FAIL_UNKNOWN)
					.setFailText("company_id 参数未填")
					.build());
		}
		final long companyId = head.getCompanyId();

		String sceneName = request.getSceneName();
		String imageName = request.getImageName();
		String sceneDesc = request.getSceneDesc();
		Integer parentSceneId = request.hasParentSceneId() ? request.getParentSceneId() : null;

		if (sceneName.length() > 191) {
			return Futures.immediateFuture(AdminSceneProtos.CreateSceneResponse.newBuilder()
					.setResult(AdminSceneProtos.CreateSceneResponse.Result.FAIL_SCENE_NAME_INVALID)
					.setFailText("场景名称不能超过191个字符！")
					.build());
		}

		if (imageName.length() > 191) {
			return Futures.immediateFuture(AdminSceneProtos.CreateSceneResponse.newBuilder()
					.setResult(AdminSceneProtos.CreateSceneResponse.Result.FAIL_IMAGE_NAME_INVALID)
					.setFailText("图片名称不能超过191个字符！")
					.build());
		}

		if (sceneDesc.length() > 191) {
			return Futures.immediateFuture(AdminSceneProtos.CreateSceneResponse.newBuilder()
					.setResult(AdminSceneProtos.CreateSceneResponse.Result.FAIL_SCENE_DESC_INVALID)
					.setFailText("场景描述不能超过191个字符！")
					.build());
		}

		Connection dbConn = null;
		Integer sceneIdNew = null;
		try {
			dbConn = hikariDataSource.getConnection();

			Set<Integer> childrenSceneIds = null;
			List<Integer> itemIds = new ArrayList<Integer>();
			if (parentSceneId != null) {
				childrenSceneIds = SceneDB.getChildrenSceneId(dbConn, companyId, Collections.singleton(parentSceneId), SceneServiceImpl.USER_STATE_LIST)
						.get(parentSceneId);
				List<ItemIndex> itemIndexList = new ArrayList<ItemIndex>();
				ItemIndex offItemIndex = null;
				while (true) {

					offItemIndex = itemIndexList.size() == 0 ? null : itemIndexList.get(itemIndexList.size() - 1);
					List<ItemIndex> tmpItemIndexList = SceneDB.getItemIndexByOrderStr(dbConn, companyId, parentSceneId, 1000, offItemIndex, null);
					if (tmpItemIndexList.isEmpty()) {
						break;
					}
					itemIndexList.addAll(tmpItemIndexList);
				}

				for (ItemIndex itemIndex : itemIndexList) {
					itemIds.add(itemIndex.getItemId());
				}
			}
			sceneIdNew = SceneDB.insertSceneScene(dbConn,
					companyId,
					sceneName,
					imageName,
					sceneDesc,
					parentSceneId,
					true,
					head.getSession().getAdminId(),
					(int) (System.currentTimeMillis() / 1000L),
					SceneProtos.State.NORMAL);
			// 更新其父节点状态
			if (parentSceneId != null && (childrenSceneIds == null || childrenSceneIds.isEmpty())) {
				SceneDB.updateSceneIsLeafScene(dbConn,
						companyId,
						parentSceneId,
						false,
						head.getSession().getAdminId(),
						(int) (System.currentTimeMillis() / 1000L));
				if (!itemIds.isEmpty()) {
					SceneDB.updateItemScene(dbConn, companyId, itemIds, sceneIdNew);
				}
			}

		} catch (SQLException e) {
			throw new RuntimeException("插入新版块信息失败！", e);
		} finally {
			DBUtil.closeQuietly(dbConn);
		}

		// 子节点和父节点缓存都需要更新
		List<Integer> sceneIds = new ArrayList<Integer>();
		sceneIds.add(sceneIdNew);
		if (parentSceneId != null) {
			sceneIds.add(parentSceneId);
		}

		Jedis jedis = jedisPool.getResource();
		try {
			SceneCache.delSceneHome(jedis, Collections.singleton(companyId));
			SceneCache.delSceneExt(jedis, companyId, sceneIds);
		} finally {
			jedis.close();
		}

		logger.info("create scene end, adminId:" + head.getSession().getAdminId());

		return Futures.immediateFuture(AdminSceneProtos.CreateSceneResponse.newBuilder()
				.setResult(AdminSceneProtos.CreateSceneResponse.Result.SUCC)
				.setSceneId(sceneIdNew)
				.build());

	}

	@Override
	public ListenableFuture<AdminSceneProtos.UpdateSceneResponse> updateScene(AdminHead head, AdminSceneProtos.UpdateSceneRequest request) {
		if (!head.hasCompanyId()) {
			return Futures.immediateFuture(AdminSceneProtos.UpdateSceneResponse.newBuilder()
					.setResult(AdminSceneProtos.UpdateSceneResponse.Result.FAIL_UNKNOWN)
					.setFailText("company_id 参数未填")
					.build());
		}
		final long companyId = head.getCompanyId();

		int sceneId = request.getSceneId();
		String sceneName = request.getSceneName();
		String imageName = request.getImageName();
		String sceneDesc = request.getSceneDesc();

		if (sceneName.length() > 191) {
			return Futures.immediateFuture(AdminSceneProtos.UpdateSceneResponse.newBuilder()
					.setResult(AdminSceneProtos.UpdateSceneResponse.Result.FAIL_SCENE_NAME_INVALID)
					.setFailText("场景名称不能超过191个字符！")
					.build());
		}

		if (imageName.length() > 191) {
			return Futures.immediateFuture(AdminSceneProtos.UpdateSceneResponse.newBuilder()
					.setResult(AdminSceneProtos.UpdateSceneResponse.Result.FAIL_IMAGE_NAME_INVALID)
					.setFailText("图片名称不能超过191个字符！")
					.build());
		}

		if (sceneDesc.length() > 191) {
			return Futures.immediateFuture(AdminSceneProtos.UpdateSceneResponse.newBuilder()
					.setResult(AdminSceneProtos.UpdateSceneResponse.Result.FAIL_SCENE_DESC_INVALID)
					.setFailText("场景描述不能超过191个字符！")
					.build());
		}

		Connection dbConn = null;
		try {
			dbConn = hikariDataSource.getConnection();
			SceneDB.updateScene(dbConn,
					companyId,
					sceneId,
					sceneName,
					imageName,
					sceneDesc,
					head.getSession().getAdminId(),
					(int) (System.currentTimeMillis() / 1000L));

		} catch (SQLException e) {
			throw new RuntimeException("更新模块信息失败！", e);
		} finally {
			DBUtil.closeQuietly(dbConn);
		}

		Jedis jedis = jedisPool.getResource();
		try {
			SceneCache.delSceneHome(jedis, Collections.singleton(companyId));
			SceneCache.delSceneExt(jedis, companyId, Collections.singleton(sceneId));
		} finally {
			jedis.close();
		}

		logger.info("update scene end, adminId:" + head.getSession().getAdminId());

		return Futures.immediateFuture(AdminSceneProtos.UpdateSceneResponse.newBuilder()
				.setResult(AdminSceneProtos.UpdateSceneResponse.Result.SUCC)
				.build());

	}

	@Override
	public ListenableFuture<AdminSceneProtos.UpdateSceneStateResponse> updateSceneState(AdminHead head,
			AdminSceneProtos.UpdateSceneStateRequest request) {
		if (!head.hasCompanyId()) {
			return Futures.immediateFuture(AdminSceneProtos.UpdateSceneStateResponse.newBuilder()
					.setResult(AdminSceneProtos.UpdateSceneStateResponse.Result.FAIL_UNKNOWN)
					.setFailText("company_id 参数未填")
					.build());
		}
		final long companyId = head.getCompanyId();

		final int sceneId = request.getSceneId();

		Connection dbConn = null;
		try {
			dbConn = hikariDataSource.getConnection();
			Map<Integer, SceneProtos.Scene> sceneMap = SceneDB.getSceneBySceneIds(dbConn, companyId, Collections.singleton(sceneId), ADMIN_STATE_LIST);
			SceneProtos.Scene scene = sceneMap.get(sceneId);
			if (null == scene) {
				return Futures.immediateFuture(AdminSceneProtos.UpdateSceneStateResponse.newBuilder()
						.setResult(AdminSceneProtos.UpdateSceneStateResponse.Result.FAIL_SCENE_ID_NOT_EXIST)
						.setFailText("该场景不存在")
						.build());
			}

			Set<Integer> sceneIds = new TreeSet<Integer>();
			sceneIds.add(sceneId);
			if (scene.hasParentSceneId()) {
				sceneIds.add(scene.getParentSceneId());
			}
			Map<Integer, Set<Integer>> parentSceneIdChildrenSceneIdsMap = SceneDB.getChildrenSceneId(dbConn, companyId, sceneIds, ADMIN_STATE_LIST);
			Set<Integer> childrenSceneIds = parentSceneIdChildrenSceneIdsMap.get(sceneId);
			if (childrenSceneIds != null && !childrenSceneIds.isEmpty() && SceneProtos.State.DELETE.equals(request.getState())) {
				return Futures.immediateFuture(AdminSceneProtos.UpdateSceneStateResponse.newBuilder()
						.setResult(AdminSceneProtos.UpdateSceneStateResponse.Result.FAIL_SCENE_EXIST_CHILDREN_SCENE)
						.setFailText("该场景下存在子场景不能删除！")
						.build());
			}

			// 当状态为DISABLE和NORMAL时更新其子节点的状态一起更新
			Set<Integer> updateSceneIds = new TreeSet<Integer>();
			if (childrenSceneIds != null) {
				updateSceneIds.addAll(childrenSceneIds);
			}
			updateSceneIds.add(sceneId);

			SceneDB.updateSceneState(dbConn,
					companyId,
					updateSceneIds,
					head.getSession().getAdminId(),
					(int) (System.currentTimeMillis() / 1000L),
					request.getState());

			// 更新父节点的is_leaf_scene字段
			if (scene.hasParentSceneId()) {
				Set<Integer> pareChildrenSceneIds = parentSceneIdChildrenSceneIdsMap.get(scene.getParentSceneId());
				if (pareChildrenSceneIds == null) {
					pareChildrenSceneIds = Collections.emptySet();
				}

				Boolean isLeafScene;
				if ((SceneProtos.State.DELETE.equals(request.getState()) || SceneProtos.State.DISABLE.equals(request.getState()))
						&& (pareChildrenSceneIds.size() == 1 && pareChildrenSceneIds.contains(sceneId))) {
					isLeafScene = true;
				} else {
					isLeafScene = false;
				}
				SceneDB.updateSceneIsLeafScene(dbConn,
						companyId,
						scene.getParentSceneId(),
						isLeafScene,
						head.getSession().getAdminId(),
						(int) (System.currentTimeMillis() / 1000L));
			}

		} catch (SQLException e) {
			throw new RuntimeException("db failed");
		} finally {
			DBUtil.closeQuietly(dbConn);
		}

		Jedis jedis = jedisPool.getResource();
		try {
			SceneCache.delSceneHome(jedis, Collections.singleton(companyId));
		} finally {
			jedis.close();
		}
		logger.info("update Scene State, adminId:" + head.getSession().getAdminId());

		return Futures.immediateFuture(AdminSceneProtos.UpdateSceneStateResponse.newBuilder()
				.setResult(AdminSceneProtos.UpdateSceneStateResponse.Result.SUCC)
				.build());
	}

	@Override
	public ListenableFuture<AdminSceneProtos.CreateSceneItemResponse> createSceneItem(AdminHead head, AdminSceneProtos.CreateSceneItemRequest request) {
		if (!head.hasCompanyId()) {
			return Futures.immediateFuture(AdminSceneProtos.CreateSceneItemResponse.newBuilder()
					.setResult(AdminSceneProtos.CreateSceneItemResponse.Result.FAIL_UNKNOWN)
					.setFailText("company_id 参数未填")
					.build());
		}
		final long companyId = head.getCompanyId();

		final long adminId = head.getSession().getAdminId();
		final List<CreateItemParameter> createItemParameterList = request.getCreateItemParameterList();
		if (createItemParameterList.isEmpty()) {
			return Futures.immediateFuture(AdminSceneProtos.CreateSceneItemResponse.newBuilder()
					.setResult(AdminSceneProtos.CreateSceneItemResponse.Result.FAIL_SCENE_ID_NOT_EXIST)
					.setFailText("场景id不能为空")
					.build());
		}

		List<SceneProtos.Item.ItemIndex> itemIndexList = new ArrayList<SceneProtos.Item.ItemIndex>();
		SceneProtos.Item.ItemIndex.Builder itemIndexBuilder = SceneProtos.Item.ItemIndex.newBuilder();
		Set<Long> discoverItemIds = new TreeSet<Long>();
		Set<Integer> sceneIds = new TreeSet<Integer>();
		for (CreateItemParameter createItemParameter : createItemParameterList) {
			if (createItemParameter.hasDiscoverItemId()) {
				discoverItemIds.add(createItemParameter.getDiscoverItemId());
			}
			sceneIds.add(createItemParameter.getSceneId());

			int currenTime = (int) (System.currentTimeMillis() / 1000L);
			itemIndexBuilder.clear();
			if (createItemParameter.hasDiscoverItemId()) {
				itemIndexBuilder.setDiscoverItemId(createItemParameter.getDiscoverItemId());
			}
			if (createItemParameter.hasCommunityPostId()) {
				itemIndexBuilder.setCommunityItemId(createItemParameter.getCommunityPostId());
			}
			itemIndexBuilder.setItemId(0)
					.setSceneId(createItemParameter.getSceneId())
					.setCreateAdminId(adminId)
					.setCreateTime(currenTime)
					.setState(SceneProtos.State.NORMAL);

			itemIndexList.add(itemIndexBuilder.build());
		}

		Map<Long, DiscoverV2Protos.Item> discoverItemMap = this.getDiscoverItemMap(head, discoverItemIds);
		for (long discoverItemId : discoverItemIds) {
			if (discoverItemMap.get(discoverItemId) == null) {
				return Futures.immediateFuture(AdminSceneProtos.CreateSceneItemResponse.newBuilder()
						.setResult(AdminSceneProtos.CreateSceneItemResponse.Result.FAIL_DISCOVER_ITEM_ID_NOT_EXIST)
						.setFailText("该发现条目的id不存在: " + discoverItemId)
						.build());
			}
		}

		Connection dbConn = null;
		try {

			dbConn = hikariDataSource.getConnection();
			Map<Integer, SceneProtos.Scene> sceneMap = SceneDB.getSceneBySceneIds(dbConn, companyId, sceneIds, ADMIN_STATE_LIST);
			for (int sceneId : sceneIds) {
				if (null == sceneMap.get(sceneId)) {
					return Futures.immediateFuture(AdminSceneProtos.CreateSceneItemResponse.newBuilder()
							.setResult(AdminSceneProtos.CreateSceneItemResponse.Result.FAIL_SCENE_ID_NOT_EXIST)
							.setFailText("该场景id不存在: " + sceneId)
							.build());
				}
			}

			SceneDB.insertSceneItem(dbConn, companyId, itemIndexList);

		} catch (SQLException e) {
			throw new RuntimeException("db failed");
		} finally {
			DBUtil.closeQuietly(dbConn);
		}

		Jedis jedis = jedisPool.getResource();
		try {
			SceneCache.delSceneExt(jedis, companyId, sceneIds);
		} finally {
			jedis.close();
		}
		logger.info("create Scene Item end, adminId:" + head.getSession().getAdminId());

		return Futures.immediateFuture(AdminSceneProtos.CreateSceneItemResponse.newBuilder()
				.setResult(AdminSceneProtos.CreateSceneItemResponse.Result.SUCC)
				.build());
	}

	@Override
	public ListenableFuture<AdminSceneProtos.UpdateSceneItemStateResponse> updateSceneItemState(AdminHead head,
			AdminSceneProtos.UpdateSceneItemStateRequest request) {
		if (!head.hasCompanyId()) {
			return Futures.immediateFuture(AdminSceneProtos.UpdateSceneItemStateResponse.newBuilder()
					.setResult(AdminSceneProtos.UpdateSceneItemStateResponse.Result.FAIL_UNKNOWN)
					.setFailText("company_id 参数未填")
					.build());
		}
		final long companyId = head.getCompanyId();

		final SceneProtos.State state = request.getState();
		Connection dbConn = null;
		Set<Integer> sceneIds = new TreeSet<Integer>();
		try {

			dbConn = hikariDataSource.getConnection();

			Map<Integer, List<Integer>> itemIdSceneIdListMap = SceneDB.getSceneIdByItemId(dbConn, companyId, request.getItemIdList());
			for (List<Integer> sceneIdList : itemIdSceneIdListMap.values()) {
				sceneIds.addAll(sceneIdList);
			}

			SceneDB.updateSceneItemState(dbConn,
					companyId,
					request.getItemIdList(),
					state,
					head.getSession().getAdminId(),
					(int) (System.currentTimeMillis() / 1000L));

		} catch (SQLException e) {
			throw new RuntimeException("db failed");
		} finally {
			DBUtil.closeQuietly(dbConn);
		}

		Jedis jedis = jedisPool.getResource();
		try {
			SceneCache.delSceneExt(jedis, companyId, sceneIds);
		} finally {
			jedis.close();
		}
		logger.info("create Scene Item end, adminId:" + head.getSession().getAdminId());

		return Futures.immediateFuture(AdminSceneProtos.UpdateSceneItemStateResponse.newBuilder()
				.setResult(AdminSceneProtos.UpdateSceneItemStateResponse.Result.SUCC)
				.build());

	}

	@Override
	public ListenableFuture<AdminSceneProtos.MigrateSceneItemResponse> migrateSceneItem(AdminHead head,
			AdminSceneProtos.MigrateSceneItemRequest request) {
		if (!head.hasCompanyId()) {
			return Futures.immediateFuture(AdminSceneProtos.MigrateSceneItemResponse.newBuilder()
					.setResult(AdminSceneProtos.MigrateSceneItemResponse.Result.FAIL_UNKNOWN)
					.setFailText("company_id 参数未填")
					.build());
		}
		final long companyId = head.getCompanyId();

		int sceneId = request.getSceneId();
		List<Integer> itemIds = request.getItemIdList();

		Connection dbConn = null;
		Set<Integer> sceneIds = new TreeSet<Integer>();
		sceneIds.add(sceneId);
		try {

			dbConn = hikariDataSource.getConnection();
			if (null == SceneDB.getSceneBySceneIds(dbConn, companyId, Collections.singleton(sceneId), ADMIN_STATE_LIST).get(sceneId)) {
				return Futures.immediateFuture(AdminSceneProtos.MigrateSceneItemResponse.newBuilder()
						.setResult(AdminSceneProtos.MigrateSceneItemResponse.Result.FAIL_SCENE_ID_NOT_EXIST)
						.setFailText("scene id不存在")
						.build());
			}

			Map<Integer, List<Integer>> itemIdSceneIdListMap = SceneDB.getSceneIdByItemId(dbConn, companyId, itemIds);
			for (List<Integer> sceneIdList : itemIdSceneIdListMap.values()) {
				sceneIds.addAll(sceneIdList);
			}
			SceneDB.updateItemScene(dbConn, companyId, itemIds, sceneId);

		} catch (SQLException e) {
			throw new RuntimeException("db failed");
		} finally {
			DBUtil.closeQuietly(dbConn);
		}

		Jedis jedis = jedisPool.getResource();
		try {
			SceneCache.delSceneExt(jedis, companyId, sceneIds);
		} finally {
			jedis.close();
		}
		logger.info("migrate Scene Item end, adminId:" + head.getSession().getAdminId());

		return Futures.immediateFuture(AdminSceneProtos.MigrateSceneItemResponse.newBuilder()
				.setResult(AdminSceneProtos.MigrateSceneItemResponse.Result.SUCC)
				.build());
	}

	@Override
	public ListenableFuture<AdminSceneProtos.GetSceneItemResponse> getSceneItem(AdminHead head, AdminSceneProtos.GetSceneItemRequest request) {
		if (!head.hasCompanyId()) {
			return Futures.immediateFuture(GetSceneItemResponse.newBuilder()
					.setTotalSize(0)
					.setFilteredSize(0)
					.build());
		}
		final long companyId = head.getCompanyId();

		final Integer sceneId = request.hasSceneId() ? request.getSceneId() : null;
		final Integer start = request.hasStart() ? request.getStart() : null;
		final Integer length = request.hasLength() ? request.getLength() : null;
		final String itemTitle = request.hasItemTitle() ? request.getItemTitle() : null;

		// 当全量获取或搜索时，条目按创建时间倒序排列
		if (sceneId == null || itemTitle != null) {
			return getSceneItemByCreateTime(head, sceneId, start, length, itemTitle);
		}

		DataPage<SceneProtos.Item.ItemIndex> itemIndexDataPage = null;
		List<SceneProtos.Scene> refSceneList = new ArrayList<SceneProtos.Scene>();
		Connection dbConn = null;
		try {
			dbConn = hikariDataSource.getConnection();
			itemIndexDataPage = SceneDB.getItemIndexByOrderStr(dbConn, companyId, sceneId, start, length, ADMIN_STATE_LIST);

			if (itemIndexDataPage == null) {
				return Futures.immediateFuture(AdminSceneProtos.GetSceneItemResponse.newBuilder().setFilteredSize(0).setTotalSize(0).build());
			}
			SceneProtos.Scene scene = SceneDB.getSceneBySceneIds(dbConn, companyId, Collections.singleton(sceneId), ADMIN_STATE_LIST).get(sceneId);
			if (scene != null) {
				refSceneList.add(scene);
			}
		} catch (SQLException e) {
			throw new RuntimeException("DB failed");
		} finally {
			DBUtil.closeQuietly(dbConn);
		}

		// 获取item内容
		Set<Long> discoverItemIds = new TreeSet<Long>();
		for (SceneProtos.Item.ItemIndex itemIndex : itemIndexDataPage.dataList()) {
			if (itemIndex.hasDiscoverItemId()) {
				discoverItemIds.add(itemIndex.getDiscoverItemId());
			}
		}

		Map<Long, DiscoverV2Protos.Item> discoverItemMap = this.getDiscoverItemMap(head, discoverItemIds);
		List<SceneProtos.Item> itemList = new ArrayList<SceneProtos.Item>();
		SceneProtos.Item.Builder itemBuilder = SceneProtos.Item.newBuilder();
		for (SceneProtos.Item.ItemIndex itemIndex : itemIndexDataPage.dataList()) {
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
		return Futures.immediateFuture(AdminSceneProtos.GetSceneItemResponse.newBuilder()
				.setFilteredSize(itemIndexDataPage.filteredSize())
				.setTotalSize(itemIndexDataPage.totalSize())
				.addAllItem(itemList)
				.addAllRefScene(refSceneList)
				.build());
	}

	private ListenableFuture<GetSceneItemResponse> getSceneItemByCreateTime(AdminHead head, @Nullable Integer sceneId, @Nullable Integer start,
			@Nullable Integer length, @Nullable String itemTitle) {
		if (!head.hasCompanyId()) {
			return Futures.immediateFuture(GetSceneItemResponse.newBuilder()
					.setTotalSize(0)
					.setFilteredSize(0)
					.build());
		}
		final long companyId = head.getCompanyId();

		List<SceneProtos.Scene> refSceneList = new ArrayList<SceneProtos.Scene>();
		List<ItemIndex> itemIndexList = new ArrayList<ItemIndex>();
		Connection dbConn = null;
		try {
			dbConn = hikariDataSource.getConnection();
			ItemIndex lastIndex = null;
			int maxSize = 1000;
			while (true) {
				List<ItemIndex> tmpItemIndexList = SceneDB.getItemIndexByCreateTime(dbConn, companyId, sceneId, maxSize + 1, lastIndex, ADMIN_STATE_LIST);
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
		if (length == null) {
			resultItemList = itemList;
		} else {
			start = start == null ? 0 : start;
			if (start < itemList.size()) {
				resultItemList = itemList.subList(start, start + length > itemList.size() ? itemList.size() : start + length);
			}
		}

		try {
			dbConn = hikariDataSource.getConnection();
			Set<Integer> itemIds = new TreeSet<Integer>();
			for (SceneProtos.Item item : resultItemList) {
				itemIds.add(item.getItemIndex().getItemId());
			}
			refSceneList.addAll(SceneDB.getSceneByItemIds(dbConn, companyId, itemIds, ADMIN_STATE_LIST).values());
		} catch (SQLException e) {
			throw new RuntimeException("DB failed");
		} finally {
			DBUtil.closeQuietly(dbConn);
		}

		return Futures.immediateFuture(AdminSceneProtos.GetSceneItemResponse.newBuilder()
				.setFilteredSize(itemList.size())
				.setTotalSize(itemIndexList.size())
				.addAllItem(resultItemList)
				.addAllRefScene(refSceneList)
				.build());
	}

	@Override
	public ListenableFuture<AdminSceneProtos.UpdateSceneItemOrderResponse> updateSceneItemOrder(AdminHead head,
			AdminSceneProtos.UpdateSceneItemOrderRequest request) {
		if (!head.hasCompanyId()) {
			return Futures.immediateFuture(AdminSceneProtos.UpdateSceneItemOrderResponse.newBuilder()
					.setResult(AdminSceneProtos.UpdateSceneItemOrderResponse.Result.FAIL_UNKNOWN)
					.setFailText("company_id 参数未填")
					.build());
		}
		final long companyId = head.getCompanyId();

		final int sceneId = request.getSceneId();
		final String itemIdOrderStr = request.getItemIdOrderStr();

		if (itemIdOrderStr.length() > 65535) { // 2的16次方-1
			return Futures.immediateFuture(AdminSceneProtos.UpdateSceneItemOrderResponse.newBuilder()
					.setResult(AdminSceneProtos.UpdateSceneItemOrderResponse.Result.FAIL_ITEM_ID_ORDER_STR_INVALID)
					.setFailText("条目ID序列不能超过65535个字符！")
					.build());
		}

		Connection dbConn = null;
		try {
			dbConn = hikariDataSource.getConnection();
			if (null == SceneDB.getSceneBySceneIds(dbConn, companyId, Collections.singleton(sceneId), ADMIN_STATE_LIST).get(sceneId)) {
				return Futures.immediateFuture(AdminSceneProtos.UpdateSceneItemOrderResponse.newBuilder()
						.setResult(AdminSceneProtos.UpdateSceneItemOrderResponse.Result.FAIL_SCENE_ID_NOT_EXIST)
						.setFailText("scene id不存在")
						.build());
			}
			SceneDB.updateSceneItemOrder(dbConn, companyId, sceneId, itemIdOrderStr);
		} catch (SQLException e) {
			throw new RuntimeException("DB failed");
		} finally {
			DBUtil.closeQuietly(dbConn);
		}
		return Futures.immediateFuture(AdminSceneProtos.UpdateSceneItemOrderResponse.newBuilder()
				.setResult(AdminSceneProtos.UpdateSceneItemOrderResponse.Result.SUCC)
				.build());
	}

	private Map<Long, DiscoverV2Protos.Item> getDiscoverItemMap(AdminHead head, Collection<Long> discoverItemIds) {

		AdminDiscoverProtos.GetItemByIdResponse response = Futures.getUnchecked(adminDiscoverService.getItemById(head,
				AdminDiscoverProtos.GetItemByIdRequest.newBuilder().addAllItemId(discoverItemIds).build()));

		Map<Long, DiscoverV2Protos.Item> itemMap = new HashMap<Long, DiscoverV2Protos.Item>();

		for (DiscoverV2Protos.Item discoverItem : response.getItemList()) {
			itemMap.put(discoverItem.getBase().getItemId(), discoverItem);
		}
		return itemMap;
	}

	/**
	 * 以下为工具——盖帽神器（超值推荐）的内容
	 */

	@Override
	public ListenableFuture<GetRecommenderHomeResponse> getRecommenderHome(AdminHead head, EmptyRequest request) {
		return adminRecommenderManager.getRecommenderHome(head, request);
	}

	@Override
	public ListenableFuture<CreateRecommenderCategoryResponse> createRecommenderCategory(AdminHead head, CreateRecommenderCategoryRequest request) {
		return adminRecommenderManager.createRecommenderCategory(head, request);
	}

	@Override
	public ListenableFuture<UpdateRecommenderCategoryResponse> updateRecommenderCategory(AdminHead head, UpdateRecommenderCategoryRequest request) {
		return adminRecommenderManager.updateRecommenderCategory(head, request);
	}

	@Override
	public ListenableFuture<UpdateRecommenderCategoryStateResponse> updateRecommenderCategoryState(AdminHead head,
			UpdateRecommenderCategoryStateRequest request) {
		return adminRecommenderManager.updateRecommenderCategoryState(head, request);
	}

	@Override
	public ListenableFuture<MigrateRecommenderCompetitorProductResponse> migrateRecommenderCompetitorProduct(AdminHead head,
			MigrateRecommenderCompetitorProductRequest request) {
		return adminRecommenderManager.migrateRecommenderCompetitorProduct(head, request);
	}

	@Override
	public ListenableFuture<GetRecommenderCompetitorProductResponse> getRecommenderCompetitorProduct(AdminHead head,
			GetRecommenderCompetitorProductRequest request) {
		return adminRecommenderManager.getRecommenderCompetitorProduct(head, request);
	}

	@Override
	public ListenableFuture<CreateRecommenderCompetitorProductResponse> createRecommenderCompetitorProduct(AdminHead head,
			CreateRecommenderCompetitorProductRequest request) {
		return adminRecommenderManager.createRecommenderCompetitorProduct(head, request);
	}

	@Override
	public ListenableFuture<UpdateRecommenderCompetitorProductResponse> updateRecommenderCompetitorProduct(AdminHead head,
			UpdateRecommenderCompetitorProductRequest request) {
		return adminRecommenderManager.updateRecommenderCompetitorProduct(head, request);
	}

	@Override
	public ListenableFuture<UpdateRecommenderCompetitorProductStateResponse> updateRecommenderCompetitorProductState(AdminHead head,
			UpdateRecommenderCompetitorProductStateRequest request) {
		return adminRecommenderManager.updateRecommenderCompetitorProductState(head, request);
	}

	@Override
	public ListenableFuture<GetRecommenderRecommendProductResponse> getRecommenderRecommendProduct(AdminHead head,
			GetRecommenderRecommendProductRequest request) {
		return adminRecommenderManager.getRecommenderRecommendProduct(head, request);
	}

	@Override
	public ListenableFuture<CreateRecommenderRecommendProductResponse> createRecommenderRecommendProduct(AdminHead head,
			CreateRecommenderRecommendProductRequest request) {
		return adminRecommenderManager.createRecommenderRecommendProduct(head, request);
	}

	@Override
	public ListenableFuture<UpdateRecommenderRecommendProductResponse> updateRecommenderRecommendProduct(AdminHead head,
			UpdateRecommenderRecommendProductRequest request) {
		return adminRecommenderManager.updateRecommenderRecommendProduct(head, request);
	}

	@Override
	public ListenableFuture<UpdateRecommenderRecommendProductStateResponse> updateRecommenderRecommendProductState(AdminHead head,
			UpdateRecommenderRecommendProductStateRequest request) {
		return adminRecommenderManager.updateRecommenderRecommendProductState(head, request);
	}

	@Override
	public ListenableFuture<AddRecommendProdToCompetitorProdResponse> addRecommendProdToCompetitorProd(AdminHead head,
			AddRecommendProdToCompetitorProdRequest request) {
		return adminRecommenderManager.addRecommendProdToCompetitorProd(head, request);
	}

	@Override
	public ListenableFuture<DeleteRecommendProdFromCompetitorProdResponse> deleteRecommendProdFromCompetitorProd(AdminHead head,
			DeleteRecommendProdFromCompetitorProdRequest request) {
		return adminRecommenderManager.deleteRecommendProdFromCompetitorProd(head, request);
	}

	@Override
	public ListenableFuture<GetRecommenderRecommendProductPriceWebUrlResponse> getRecommenderRecommendProductPriceWebUrl(AdminHead head,
			GetRecommenderRecommendProductPriceWebUrlRequest request) {
		return adminRecommenderManager.getRecommenderRecommendProductPriceWebUrl(head, request);
	}

	@Override
	public ListenableFuture<CreateRecommenderRecommendProductPriceWebUrlResponse> createRecommenderRecommendProductPriceWebUrl(AdminHead head,
			CreateRecommenderRecommendProductPriceWebUrlRequest request) {
		return adminRecommenderManager.createRecommenderRecommendProductPriceWebUrl(head, request);
	}

	@Override
	public ListenableFuture<UpdateRecommenderRecommendProductPriceWebUrlResponse> updateRecommenderRecommendProductPriceWebUrl(AdminHead head,
			UpdateRecommenderRecommendProductPriceWebUrlRequest request) {
		return adminRecommenderManager.updateRecommenderRecommendProductPriceWebUrl(head, request);
	}

	@Override
	public ListenableFuture<DeleteRecommenderRecommendProductPriceWebUrlResponse> deleteRecommenderRecommendProductPriceWebUrl(AdminHead head,
			DeleteRecommenderRecommendProductPriceWebUrlRequest request) {
		return adminRecommenderManager.deleteRecommenderRecommendProductPriceWebUrl(head, request);
	}

}
