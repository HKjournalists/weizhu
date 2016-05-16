package com.weizhu.service.discover_v2;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;
import javax.annotation.Nullable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.inject.Inject;
import com.weizhu.common.db.DBUtil;
import com.weizhu.common.utils.DataPage;
import com.weizhu.proto.AdminDiscoverProtos;
import com.weizhu.proto.AdminDiscoverProtos.AddItemToCategoryRequest;
import com.weizhu.proto.AdminDiscoverProtos.AddItemToCategoryResponse;
import com.weizhu.proto.AdminDiscoverProtos.CreateBannerRequest;
import com.weizhu.proto.AdminDiscoverProtos.CreateBannerResponse;
import com.weizhu.proto.AdminDiscoverProtos.CreateItemRequest;
import com.weizhu.proto.AdminDiscoverProtos.CreateItemResponse;
import com.weizhu.proto.AdminDiscoverProtos.CreateModuleCategoryRequest;
import com.weizhu.proto.AdminDiscoverProtos.CreateModuleCategoryResponse;
import com.weizhu.proto.AdminDiscoverProtos.CreateModuleRequest;
import com.weizhu.proto.AdminDiscoverProtos.CreateModuleResponse;
import com.weizhu.proto.AdminDiscoverProtos.DeleteItemFromCategoryRequest;
import com.weizhu.proto.AdminDiscoverProtos.DeleteItemFromCategoryResponse;
import com.weizhu.proto.AdminDiscoverProtos.GetBannerResponse;
import com.weizhu.proto.AdminDiscoverProtos.GetItemByIdRequest;
import com.weizhu.proto.AdminDiscoverProtos.GetItemByIdResponse;
import com.weizhu.proto.AdminDiscoverProtos.GetItemCommentListRequest;
import com.weizhu.proto.AdminDiscoverProtos.GetItemCommentListResponse;
import com.weizhu.proto.AdminDiscoverProtos.GetItemLearnListRequest;
import com.weizhu.proto.AdminDiscoverProtos.GetItemLearnListResponse;
import com.weizhu.proto.AdminDiscoverProtos.GetItemLikeListRequest;
import com.weizhu.proto.AdminDiscoverProtos.GetItemLikeListResponse;
import com.weizhu.proto.AdminDiscoverProtos.GetItemListRequest;
import com.weizhu.proto.AdminDiscoverProtos.GetItemListResponse;
import com.weizhu.proto.AdminDiscoverProtos.GetItemScoreListRequest;
import com.weizhu.proto.AdminDiscoverProtos.GetItemScoreListResponse;
import com.weizhu.proto.AdminDiscoverProtos.GetItemShareListRequest;
import com.weizhu.proto.AdminDiscoverProtos.GetItemShareListResponse;
import com.weizhu.proto.AdminDiscoverProtos.GetModuleCategoryRequest;
import com.weizhu.proto.AdminDiscoverProtos.GetModuleCategoryResponse;
import com.weizhu.proto.AdminDiscoverProtos.GetModuleResponse;
import com.weizhu.proto.AdminDiscoverProtos.ImportItemRequest;
import com.weizhu.proto.AdminDiscoverProtos.ImportItemResponse;
import com.weizhu.proto.AdminDiscoverProtos.MigrateModuleCategoryRequest;
import com.weizhu.proto.AdminDiscoverProtos.MigrateModuleCategoryResponse;
import com.weizhu.proto.AdminDiscoverProtos.SetDiscoverHomeRequest;
import com.weizhu.proto.AdminDiscoverProtos.SetDiscoverHomeResponse;
import com.weizhu.proto.AdminDiscoverProtos.UpdateBannerRequest;
import com.weizhu.proto.AdminDiscoverProtos.UpdateBannerResponse;
import com.weizhu.proto.AdminDiscoverProtos.UpdateBannerStateRequest;
import com.weizhu.proto.AdminDiscoverProtos.UpdateBannerStateResponse;
import com.weizhu.proto.AdminDiscoverProtos.UpdateItemRequest;
import com.weizhu.proto.AdminDiscoverProtos.UpdateItemResponse;
import com.weizhu.proto.AdminDiscoverProtos.UpdateItemStateRequest;
import com.weizhu.proto.AdminDiscoverProtos.UpdateItemStateResponse;
import com.weizhu.proto.AdminDiscoverProtos.UpdateModuleCategoryOrderRequest;
import com.weizhu.proto.AdminDiscoverProtos.UpdateModuleCategoryOrderResponse;
import com.weizhu.proto.AdminDiscoverProtos.UpdateModuleCategoryRequest;
import com.weizhu.proto.AdminDiscoverProtos.UpdateModuleCategoryResponse;
import com.weizhu.proto.AdminDiscoverProtos.UpdateModuleCategoryStateRequest;
import com.weizhu.proto.AdminDiscoverProtos.UpdateModuleCategoryStateResponse;
import com.weizhu.proto.AdminDiscoverProtos.UpdateModuleRequest;
import com.weizhu.proto.AdminDiscoverProtos.UpdateModuleResponse;
import com.weizhu.proto.AdminDiscoverProtos.UpdateModuleStateRequest;
import com.weizhu.proto.AdminDiscoverProtos.UpdateModuleStateResponse;
import com.weizhu.proto.AdminDiscoverService;
import com.weizhu.proto.AdminProtos.AdminHead;
import com.weizhu.proto.BossProtos.BossHead;
import com.weizhu.proto.DiscoverV2Protos;
import com.weizhu.proto.WeizhuProtos.EmptyRequest;
import com.weizhu.proto.WeizhuProtos.SystemHead;
import com.zaxxer.hikari.HikariDataSource;

public class AdminDiscoverServiceImpl implements AdminDiscoverService {

	@SuppressWarnings("unused")
	private static final Logger logger = LoggerFactory.getLogger(AdminDiscoverServiceImpl.class);

	private static final ImmutableList<DiscoverV2Protos.State> ADMIN_STATE_LIST = 
			ImmutableList.of(DiscoverV2Protos.State.NORMAL, DiscoverV2Protos.State.DISABLE);
	
	private final HikariDataSource hikariDataSource;
	private final JedisPool jedisPool;

	@Inject
	public AdminDiscoverServiceImpl(HikariDataSource hikariDataSource, JedisPool jedisPool) {
		this.hikariDataSource = hikariDataSource;
		this.jedisPool = jedisPool;
	}
	
	@Override
	public ListenableFuture<SetDiscoverHomeResponse> setDiscoverHome(AdminHead head, SetDiscoverHomeRequest request) {
		if (!head.hasCompanyId()) {
			return Futures.immediateFuture(SetDiscoverHomeResponse.newBuilder()
					.setResult(SetDiscoverHomeResponse.Result.FAIL_UNKNOWN)
					.setFailText("company_id 参数未填")
					.build());
		}
		final long companyId = head.getCompanyId();
		final List<Integer> bannerOrderIdList = request.getBannerOrderIdList();
		final List<Integer> moduleOrderIdList = request.getModuleOrderIdList();

		if (bannerOrderIdList.isEmpty() && moduleOrderIdList.isEmpty()) {
			return Futures.immediateFuture(SetDiscoverHomeResponse.newBuilder()
					.setResult(SetDiscoverHomeResponse.Result.SUCC)
					.build());
		}
		if (bannerOrderIdList.size() > 100) {
			return Futures.immediateFuture(SetDiscoverHomeResponse.newBuilder()
					.setResult(SetDiscoverHomeResponse.Result.FAIL_BANNER_ORDER_ID_INVALID)
					.setFailText("banner_id数量超过最大值")
					.build());
		}
		if (moduleOrderIdList.size() > 100) {
			return Futures.immediateFuture(SetDiscoverHomeResponse.newBuilder()
					.setResult(SetDiscoverHomeResponse.Result.FAIL_MODULE_ORDER_ID_INVALID)
					.setFailText("模块id数量超过最大值")
					.build());
		}
		
		Connection dbConn = null;
		try {
			dbConn = hikariDataSource.getConnection();
			DiscoverV2DB.setDiscoverHome(dbConn, companyId, bannerOrderIdList, moduleOrderIdList);
		} catch (SQLException e) {
			throw new RuntimeException("db failed", e);
		} finally {
			DBUtil.closeQuietly(dbConn);
		}

		Jedis jedis = jedisPool.getResource();
		try {
			DiscoverV2Cache.delDiscoverHome(jedis, Collections.singleton(companyId));
		} finally {
			jedis.close();
		}

		return Futures.immediateFuture(SetDiscoverHomeResponse.newBuilder()
				.setResult(SetDiscoverHomeResponse.Result.SUCC)
				.build());
	}
	
	@Override
	public ListenableFuture<GetBannerResponse> getBanner(AdminHead head, EmptyRequest request) {
		if (!head.hasCompanyId()) {
			return Futures.immediateFuture(GetBannerResponse.newBuilder().build());
		}
		final long companyId = head.getCompanyId();
		List<DiscoverV2Protos.Banner> bannerList;
		Connection dbConn = null;
		try {
			dbConn = hikariDataSource.getConnection();
			bannerList = DiscoverV2DB.getDiscoverHome(dbConn, companyId, ADMIN_STATE_LIST).getBannerList();
		} catch (SQLException e) {
			throw new RuntimeException("db failed", e);
		} finally {
			DBUtil.closeQuietly(dbConn);
		}
		return Futures.immediateFuture(GetBannerResponse.newBuilder()
				.addAllBanner(bannerList)
				.build());
	}

	@Override
	public ListenableFuture<CreateBannerResponse> createBanner(AdminHead head, CreateBannerRequest request) {
		if (!head.hasCompanyId()) {
			return Futures.immediateFuture(CreateBannerResponse.newBuilder()
					.setResult(CreateBannerResponse.Result.FAIL_UNKNOWN)
					.setFailText("company_id 参数未填")
					.build());
		}
		final long companyId = head.getCompanyId();
		final String bannerName = request.getBannerName();
		final String imageName = request.getImageName();
		final Long itemId = request.hasItemId() ? request.getItemId() : null;
		final Integer allowModelId = request.hasAllowModelId() ? request.getAllowModelId() : null;
		final DiscoverV2Protos.WebUrl webUrl = request.hasWebUrl() ? request.getWebUrl() : null;
		final DiscoverV2Protos.AppUri appUri = request.hasAppUri() ? request.getAppUri() : null;

		if (bannerName.length() > 191) {
			return Futures.immediateFuture(CreateBannerResponse.newBuilder()
					.setResult(CreateBannerResponse.Result.FAIL_BANNER_NAME_INVALID)
					.setFailText("banner名称长度超出范围！")
					.build());
		}
		if (imageName.length() > 191) {
			return Futures.immediateFuture(CreateBannerResponse.newBuilder()
					.setResult(CreateBannerResponse.Result.FAIL_IMAGE_NAME_INVALID)
					.setFailText("图片名称长度超出范围！")
					.build());
		}
		if (itemId != null
				&& DiscoverUtil.getItemBase(hikariDataSource, jedisPool, companyId, Collections.singleton(itemId), ADMIN_STATE_LIST).get(itemId) == null) {
			return Futures.immediateFuture(CreateBannerResponse.newBuilder()
					.setResult(CreateBannerResponse.Result.FAIL_ITEM_ID_NOT_EXIST)
					.setFailText("条目id不存在！")
					.build());
		}
		if (webUrl != null && webUrl.getWebUrl().length() > 191) {
			return Futures.immediateFuture(CreateBannerResponse.newBuilder()
					.setResult(CreateBannerResponse.Result.FAIL_WEB_URL_INVALID)
					.setFailText("web url长度超出范围！！")
					.build());
		}
		if (appUri != null && appUri.getAppUri().length() > 191) {
			return Futures.immediateFuture(CreateBannerResponse.newBuilder()
					.setResult(CreateBannerResponse.Result.FAIL_APP_URI_INVALID)
					.setFailText("app uri长度超出范围！！")
					.build());
		}

		long adminId = head.getSession().getAdminId();
		
		DiscoverV2Protos.Banner.Builder bannerBuilder = DiscoverV2Protos.Banner.newBuilder()
				.setBannerId(0)
				.setBannerName(bannerName)
				.setImageName(imageName)
				.setState(DiscoverV2Protos.State.NORMAL)
				.setCreateAdminId(adminId)
				.setCreateTime((int) (System.currentTimeMillis() / 1000L));
		if (allowModelId != null) {
			bannerBuilder.setAllowModelId(allowModelId);
		}
		if (itemId != null) {
			bannerBuilder.setItemId(itemId);
		}
		if (webUrl != null) {
			bannerBuilder.setWebUrl(webUrl);
		}
		if (appUri != null) {
			bannerBuilder.setAppUri(appUri);
		}

		final int bannerId;
		Connection dbConn = null;
		try {
			dbConn = hikariDataSource.getConnection();
			bannerId = DiscoverV2DB.insertBanner(dbConn, companyId, bannerBuilder.build());
		} catch (SQLException e) {
			throw new RuntimeException("db failed", e);
		} finally {
			DBUtil.closeQuietly(dbConn);
		}

		Jedis jedis = jedisPool.getResource();
		try {
			DiscoverV2Cache.delDiscoverHome(jedis, Collections.singleton(companyId));
		} finally {
			jedis.close();
		}

		return Futures.immediateFuture(CreateBannerResponse.newBuilder()
				.setResult(CreateBannerResponse.Result.SUCC)
				.setBannerId(bannerId)
				.build());
	}

	@Override
	public ListenableFuture<UpdateBannerResponse> updateBanner(AdminHead head, UpdateBannerRequest request) {
		if (!head.hasCompanyId()) {
			return Futures.immediateFuture(UpdateBannerResponse.newBuilder()
					.setResult(UpdateBannerResponse.Result.FAIL_UNKNOWN)
					.setFailText("company_id 参数未填")
					.build());
		}
		final long companyId = head.getCompanyId();
		final int bannerId = request.getBannerId();
		final String bannerName = request.getBannerName();
		final String imageName = request.getImageName();
		final Long itemId = request.hasItemId() ? request.getItemId() : null;
		final DiscoverV2Protos.WebUrl webUrl = request.hasWebUrl() ? request.getWebUrl() : null;
		final DiscoverV2Protos.AppUri appUri = request.hasAppUri() ? request.getAppUri() : null;
		final Integer allowModelId = request.hasAllowModelId() ? request.getAllowModelId() : null;

		if (bannerName.length() > 191) {
			return Futures.immediateFuture(UpdateBannerResponse.newBuilder()
					.setResult(UpdateBannerResponse.Result.FAIL_BANNER_NAME_INVALID)
					.setFailText("banner名称长度超出范围！")
					.build());
		}
		if (request.getImageName().length() > 191) {
			return Futures.immediateFuture(UpdateBannerResponse.newBuilder()
					.setResult(UpdateBannerResponse.Result.FAIL_IMAGE_NAME_INVALID)
					.setFailText("图片名称长度超出范围！")
					.build());
		}
		if (itemId != null
				&& DiscoverUtil.getItemBase(hikariDataSource, jedisPool, companyId, Collections.singleton(itemId), ADMIN_STATE_LIST).get(itemId) == null) {
			return Futures.immediateFuture(UpdateBannerResponse.newBuilder()
					.setResult(UpdateBannerResponse.Result.FAIL_ITEM_ID_NOT_EXIST)
					.setFailText("条目id不存在！")
					.build());
		}
		if (webUrl != null && webUrl.getWebUrl().length() > 191) {
			return Futures.immediateFuture(UpdateBannerResponse.newBuilder()
					.setResult(UpdateBannerResponse.Result.FAIL_WEB_URL_INVALID)
					.setFailText("web url长度超出范围！！")
					.build());
		}
		if (appUri != null && appUri.getAppUri().length() > 191) {
			return Futures.immediateFuture(UpdateBannerResponse.newBuilder()
					.setResult(UpdateBannerResponse.Result.FAIL_APP_URI_INVALID)
					.setFailText("app uri长度超出范围！！")
					.build());
		}

		Connection dbConn = null;
		try {
			dbConn = hikariDataSource.getConnection();

			DiscoverV2Protos.Banner banner = DiscoverV2DB.getBannerById(dbConn, companyId, Collections.singleton(bannerId), ADMIN_STATE_LIST).get(bannerId);
			if (banner == null) {
				return Futures.immediateFuture(UpdateBannerResponse.newBuilder()
						.setResult(UpdateBannerResponse.Result.FAIL_BANNER_NOT_EXIST)
						.setFailText("banner不存在！")
						.build());
			}
			
			DiscoverV2Protos.Banner.Builder bannerBuilder = banner.toBuilder()
					.setBannerName(bannerName)
					.setImageName(imageName);
			if (allowModelId != null) {
				bannerBuilder.setAllowModelId(allowModelId);
			}
			if (itemId != null) {
				bannerBuilder.setItemId(itemId);
			}
			if (webUrl != null) {
				bannerBuilder.setWebUrl(webUrl);
			}
			if (appUri != null) {
				bannerBuilder.setAppUri(appUri);
			}
			bannerBuilder.setUpdateAdminId(head.getSession().getAdminId()).setUpdateTime((int) (System.currentTimeMillis() / 1000L));
			
			DiscoverV2DB.updateBanner(dbConn, companyId, bannerBuilder.build());
		} catch (SQLException e) {
			throw new RuntimeException("db failed", e);
		} finally {
			DBUtil.closeQuietly(dbConn);
		}

		Jedis jedis = jedisPool.getResource();
		try {
			DiscoverV2Cache.delDiscoverHome(jedis, Collections.singleton(companyId));
		} finally {
			jedis.close();
		}
		return Futures.immediateFuture(UpdateBannerResponse.newBuilder()
				.setResult(UpdateBannerResponse.Result.SUCC)
				.build());
	}

	@Override
	public ListenableFuture<UpdateBannerStateResponse> updateBannerState(AdminHead head, UpdateBannerStateRequest request) {
		if (!head.hasCompanyId()) {
			return Futures.immediateFuture(UpdateBannerStateResponse.newBuilder()
					.setResult(UpdateBannerStateResponse.Result.FAIL_UNKNOWN)
					.setFailText("company_id 参数未填")
					.build());
		}
		final long companyId = head.getCompanyId();
		final List<Integer> bannerIdList = request.getBannerIdList();
		
		if (bannerIdList.isEmpty()) {
			return Futures.immediateFuture(UpdateBannerStateResponse.newBuilder()
					.setResult(UpdateBannerStateResponse.Result.SUCC)
					.build());
		}
		if (bannerIdList.size() > 100) {
			return Futures.immediateFuture(UpdateBannerStateResponse.newBuilder()
					.setResult(UpdateBannerStateResponse.Result.FAIL_UNKNOWN)
					.setFailText("banner 数量太多")
					.build());
		}
		
		Connection dbConn = null;
		try {
			dbConn = hikariDataSource.getConnection();

			DiscoverV2DB.updateBannerState(dbConn, companyId, bannerIdList, ADMIN_STATE_LIST, request.getState());
		} catch (SQLException e) {
			throw new RuntimeException("db failed", e);
		} finally {
			DBUtil.closeQuietly(dbConn);
		}

		Jedis jedis = jedisPool.getResource();
		try {
			DiscoverV2Cache.delDiscoverHome(jedis, Collections.singleton(companyId));
		} finally {
			jedis.close();
		}

		return Futures.immediateFuture(UpdateBannerStateResponse.newBuilder()
				.setResult(UpdateBannerStateResponse.Result.SUCC)
				.build());
	}
	
	@Override
	public ListenableFuture<GetModuleResponse> getModule(AdminHead head, EmptyRequest request) {
		if (!head.hasCompanyId()) {
			return Futures.immediateFuture(GetModuleResponse.newBuilder().build());
		}
		return Futures.immediateFuture(this.doGetModule(head.getCompanyId(), ADMIN_STATE_LIST));
	}
	
	@Override
	public ListenableFuture<GetModuleResponse> getModule(BossHead head, EmptyRequest request) {
		if (!head.hasCompanyId()) {
			return Futures.immediateFuture(GetModuleResponse.getDefaultInstance());
		}
		return Futures.immediateFuture(this.doGetModule(head.getCompanyId(), null));
	}
	
	public GetModuleResponse doGetModule(final long companyId, @Nullable Collection<DiscoverV2Protos.State> states) {
		List<DiscoverV2Protos.Module> moduleList;
		Connection dbConn = null;
		try {
			dbConn = hikariDataSource.getConnection();
			moduleList = DiscoverV2DB.getDiscoverHome(dbConn, companyId, states).getModuleList();
		} catch (SQLException e) {
			throw new RuntimeException("db failed", e);
		} finally {
			DBUtil.closeQuietly(dbConn);
		}
		return GetModuleResponse.newBuilder()
				.addAllModule(moduleList)
				.build();
	}
	
	@Override
	public ListenableFuture<CreateModuleResponse> createModule(AdminHead head, CreateModuleRequest request) {
		if (!head.hasCompanyId()) {
			return Futures.immediateFuture(CreateModuleResponse.newBuilder()
					.setResult(CreateModuleResponse.Result.FAIL_UNKNOWN)
					.setFailText("company_id 参数未填")
					.build());
		}
		final long companyId = head.getCompanyId();
		final String moduleName = request.getModuleName();
		final String imageName = request.getImageName();
		final Integer allowModelId = request.hasAllowModelId() ? request.getAllowModelId() : null;
		final DiscoverV2Protos.WebUrl webUrl = request.hasWebUrl() ? request.getWebUrl() : null;
		final DiscoverV2Protos.AppUri appUri = request.hasAppUri() ? request.getAppUri() : null;
		final Boolean isPromptDot = request.hasIsPromptDot() ? request.getIsPromptDot() : null;

		if (request.getModuleName().length() > 191) {
			return Futures.immediateFuture(CreateModuleResponse.newBuilder()
					.setResult(CreateModuleResponse.Result.FAIL_MODULE_NAME_INVALID)
					.setFailText("模块名称长度超出范围！")
					.build());
		}
		if (request.getImageName().length() > 191) {
			return Futures.immediateFuture(CreateModuleResponse.newBuilder()
					.setResult(CreateModuleResponse.Result.FAIL_IMAGE_NAME_INVALID)
					.setFailText("图片名称长度超出范围！")
					.build());
		}
		if (webUrl != null && webUrl.getWebUrl().length() > 191) {
			return Futures.immediateFuture(CreateModuleResponse.newBuilder()
					.setResult(CreateModuleResponse.Result.FAIL_WEB_URL_INVALID)
					.setFailText("web url长度超出范围！！")
					.build());
		}
		if (appUri != null && appUri.getAppUri().length() > 191) {
			return Futures.immediateFuture(CreateModuleResponse.newBuilder()
					.setResult(CreateModuleResponse.Result.FAIL_APP_URI_INVALID)
					.setFailText("app uri长度超出范围！！")
					.build());
		}
		
		long currentTimeMillis = System.currentTimeMillis();

		int moduleId;
		Connection dbConn = null;
		try {
			dbConn = hikariDataSource.getConnection();
			
			moduleId = DiscoverV2DB.insertModule(dbConn,
					companyId,
					moduleName,
					imageName,
					allowModelId,
					webUrl,
					appUri,
					isPromptDot != null && isPromptDot ? currentTimeMillis : null,
					DiscoverV2Protos.State.DISABLE,
					head.getSession().getAdminId(),
					(int) (currentTimeMillis / 1000L));
		} catch (SQLException e) {
			throw new RuntimeException("db failed", e);
		} finally {
			DBUtil.closeQuietly(dbConn);
		}

		Jedis jedis = jedisPool.getResource();
		try {
			DiscoverV2Cache.delDiscoverHome(jedis, Collections.singleton(companyId));
		} finally {
			jedis.close();
		}
		return Futures.immediateFuture(CreateModuleResponse.newBuilder()
				.setResult(CreateModuleResponse.Result.SUCC)
				.setModuleId(moduleId)
				.build());
	}

	@Override
	public ListenableFuture<UpdateModuleResponse> updateModule(AdminHead head, UpdateModuleRequest request) {
		if (!head.hasCompanyId()) {
			return Futures.immediateFuture(UpdateModuleResponse.newBuilder()
					.setResult(UpdateModuleResponse.Result.FAIL_UNKNOWN)
					.setFailText("company_id 参数未填")
					.build());
		}
		final long companyId = head.getCompanyId();
		final int moduleId = request.getModuleId();
		final String moduleName = request.hasModuleName() ? request.getModuleName() : null;
		final String imageName = request.hasImageName() ? request.getImageName() : null;
		final Integer allowModelId = request.hasAllowModelId() ? request.getAllowModelId() : null;
		final DiscoverV2Protos.WebUrl webUrl = request.hasWebUrl() ? request.getWebUrl() : null;
		final DiscoverV2Protos.AppUri appUri = request.hasAppUri() ? request.getAppUri() : null;
		final Boolean isPromptDot = request.hasIsPromptDot() ? request.getIsPromptDot() : null;
		final List<Integer> categoryOrderIdList = request.getCategoryOrderIdList();

		if (moduleName != null && request.getModuleName().length() > 191) {
			return Futures.immediateFuture(UpdateModuleResponse.newBuilder()
					.setResult(UpdateModuleResponse.Result.FAIL_MODULE_NAME_INVALID)
					.setFailText("模块名称长度超出范围！")
					.build());
		}

		if (imageName != null && request.getImageName().length() > 191) {
			return Futures.immediateFuture(UpdateModuleResponse.newBuilder()
					.setResult(UpdateModuleResponse.Result.FAIL_IMAGE_NAME_INVALID)
					.setFailText("图片名称长度超出范围！")
					.build());
		}

		if (webUrl != null && webUrl.getWebUrl().length() > 191) {
			return Futures.immediateFuture(UpdateModuleResponse.newBuilder()
					.setResult(UpdateModuleResponse.Result.FAIL_WEB_URL_INVALID)
					.setFailText("web url长度超出范围！！")
					.build());
		}
		if (appUri != null && appUri.getAppUri().length() > 191) {
			return Futures.immediateFuture(UpdateModuleResponse.newBuilder()
					.setResult(UpdateModuleResponse.Result.FAIL_APP_URI_INVALID)
					.setFailText("app uri长度超出范围！！")
					.build());
		}
		if (categoryOrderIdList.size() > 100) { // 2的16次方-1
			return Futures.immediateFuture(UpdateModuleResponse.newBuilder()
					.setResult(UpdateModuleResponse.Result.FAIL_UNKNOWN)
					.setFailText("分类ID序列长度超出范围！！")
					.build());
		}
		
		long currentTimeMillis = System.currentTimeMillis();

		Connection dbConn = null;
		try {
			dbConn = hikariDataSource.getConnection();

			DiscoverV2Protos.Module module = DiscoverV2DB.getModuleById(dbConn, companyId, Collections.singleton(moduleId), ADMIN_STATE_LIST).get(moduleId);
			if (module == null) {
				return Futures.immediateFuture(UpdateModuleResponse.newBuilder()
						.setResult(UpdateModuleResponse.Result.FAIL_MODULE_NOT_EXIST)
						.setFailText("模块id不存在！")
						.build());
			}

			DiscoverV2DB.updateModule(dbConn,
					companyId,
					moduleId,
					moduleName,
					imageName,
					allowModelId,
					webUrl,
					appUri,
					isPromptDot != null && isPromptDot ? currentTimeMillis : null,
					categoryOrderIdList,
					head.getSession().getAdminId(),
					(int) (currentTimeMillis / 1000L));
		} catch (SQLException e) {
			throw new RuntimeException("db failed", e);
		} finally {
			DBUtil.closeQuietly(dbConn);
		}

		Jedis jedis = jedisPool.getResource();
		try {
			DiscoverV2Cache.delDiscoverHome(jedis, Collections.singleton(companyId));
		} finally {
			jedis.close();
		}
		return Futures.immediateFuture(UpdateModuleResponse.newBuilder()
				.setResult(UpdateModuleResponse.Result.SUCC)
				.build());
	}

	@Override
	public ListenableFuture<UpdateModuleStateResponse> updateModuleState(AdminHead head, UpdateModuleStateRequest request) {
		if (!head.hasCompanyId()) {
			return Futures.immediateFuture(UpdateModuleStateResponse.newBuilder()
					.setResult(UpdateModuleStateResponse.Result.FAIL_UNKNOWN)
					.setFailText("company_id 参数未填")
					.build());
		}
		final long companyId = head.getCompanyId();
		final List<Integer> moduleIdList = request.getModuleIdList();
		
		if (moduleIdList.isEmpty()) {
			return Futures.immediateFuture(UpdateModuleStateResponse.newBuilder()
					.setResult(UpdateModuleStateResponse.Result.SUCC)
					.build());
		}
		
		Connection dbConn = null;
		try {
			dbConn = hikariDataSource.getConnection();
			DiscoverV2DB.updateModuleState(dbConn, companyId, moduleIdList, ADMIN_STATE_LIST, request.getState());
		} catch (SQLException e) {
			throw new RuntimeException("db failed", e);
		} finally {
			DBUtil.closeQuietly(dbConn);
		}

		Jedis jedis = jedisPool.getResource();
		try {
			DiscoverV2Cache.delDiscoverHome(jedis, Collections.singleton(companyId));
		} finally {
			jedis.close();
		}

		return Futures.immediateFuture(UpdateModuleStateResponse.newBuilder()
				.setResult(UpdateModuleStateResponse.Result.SUCC)
				.build());
	}
	
	@Override
	public ListenableFuture<GetModuleCategoryResponse> getModuleCategory(AdminHead head, GetModuleCategoryRequest request) {
		if (!head.hasCompanyId()) {
			return Futures.immediateFuture(GetModuleCategoryResponse.newBuilder().build());
		}
		final long companyId = head.getCompanyId();
		final int moduleId = request.getModuleId();

		DiscoverV2Protos.Module module;
		Connection dbConn = null;
		try {
			dbConn = hikariDataSource.getConnection();
			module = DiscoverV2DB.getModuleById(dbConn, companyId, Collections.singleton(moduleId), ADMIN_STATE_LIST).get(moduleId);
		} catch (SQLException e) {
			throw new RuntimeException("db failed", e);
		} finally {
			DBUtil.closeQuietly(dbConn);
		}

		if (module == null) {
			return Futures.immediateFuture(GetModuleCategoryResponse.getDefaultInstance());
		} else {
			return Futures.immediateFuture(GetModuleCategoryResponse.newBuilder()
					.addAllCategory(module.getCategoryList().getCategoryList())
					.build());
		}
	}

	@Override
	public ListenableFuture<CreateModuleCategoryResponse> createModuleCategory(AdminHead head, CreateModuleCategoryRequest request) {
		if (!head.hasCompanyId()) {
			return Futures.immediateFuture(CreateModuleCategoryResponse.newBuilder()
					.setResult(CreateModuleCategoryResponse.Result.FAIL_UNKNOWN)
					.setFailText("company_id 参数未填")
					.build());
		}
		final long companyId = head.getCompanyId();
		final int moduleId = request.getModuleId();
		final String categoryName = request.getCategoryName();
		final Integer allowModelId = request.hasAllowModelId() ? request.getAllowModelId() : null;

		if (categoryName.length() > 191) {
			return Futures.immediateFuture(CreateModuleCategoryResponse.newBuilder()
					.setResult(CreateModuleCategoryResponse.Result.FAIL_CATEGORY_NAME_INVALID)
					.setFailText("分类名长度超出范围！")
					.build());
		}
		
		int categoryId;
		Connection dbConn = null;
		try {
			dbConn = hikariDataSource.getConnection();
			
			DiscoverV2Protos.Module module = DiscoverV2DB.getModuleById(dbConn, companyId, Collections.singleton(moduleId), ADMIN_STATE_LIST).get(moduleId);
			if (module == null) {
				return Futures.immediateFuture(CreateModuleCategoryResponse.newBuilder()
						.setResult(CreateModuleCategoryResponse.Result.FAIL_MODULE_NOT_EXIST)
						.setFailText("模块id不存在！")
						.build());
			}
			
			categoryId = DiscoverV2DB.insertModuleCategory(dbConn,
					companyId,
					moduleId,
					categoryName,
					allowModelId,
					DiscoverV2Protos.State.DISABLE,
					head.getSession().getAdminId(),
					(int) (System.currentTimeMillis() / 1000L));
		} catch (SQLException e) {
			throw new RuntimeException("db failed", e);
		} finally {
			DBUtil.closeQuietly(dbConn);
		}

		Jedis jedis = jedisPool.getResource();
		try {
			DiscoverV2Cache.delDiscoverHome(jedis, Collections.singleton(companyId));
		} finally {
			jedis.close();
		}
		return Futures.immediateFuture(CreateModuleCategoryResponse.newBuilder()
				.setResult(CreateModuleCategoryResponse.Result.SUCC)
				.setCategoryId(categoryId)
				.build());
	}

	@Override
	public ListenableFuture<UpdateModuleCategoryResponse> updateModuleCategory(AdminHead head, UpdateModuleCategoryRequest request) {
		if (!head.hasCompanyId()) {
			return Futures.immediateFuture(UpdateModuleCategoryResponse.newBuilder()
					.setResult(UpdateModuleCategoryResponse.Result.FAIL_UNKNOWN)
					.setFailText("company_id 参数未填")
					.build());
		}
		final long companyId = head.getCompanyId();
		final int categoryId = request.getCategoryId();
		final int moduleId = request.getModuleId();
		final String categoryName = request.getCategoryName();
		final Integer allowModelId = request.hasAllowModelId() ? request.getAllowModelId() : null;

		if (categoryName.length() > 191) {
			return Futures.immediateFuture(UpdateModuleCategoryResponse.newBuilder()
					.setResult(UpdateModuleCategoryResponse.Result.FAIL_CATEGORY_NAME_INVALID)
					.setFailText("分类名长度超出范围！")
					.build());
		}
		
		Connection dbConn = null;
		try {
			dbConn = hikariDataSource.getConnection();

			DiscoverV2Protos.Module module = DiscoverV2DB.getModuleById(dbConn, companyId, Collections.singleton(moduleId), ADMIN_STATE_LIST).get(moduleId);
			if (module == null) {
				return Futures.immediateFuture(UpdateModuleCategoryResponse.newBuilder()
						.setResult(UpdateModuleCategoryResponse.Result.FAIL_MODULE_NOT_EXIST)
						.setFailText("模块id不存在！")
						.build());
			}

			boolean hasCategory = false;
			for (DiscoverV2Protos.Module.Category category : module.getCategoryList().getCategoryList()) {
				if (category.getCategoryId() == categoryId) {
					hasCategory = true;
					break;
				}
			}

			if (!hasCategory) {
				return Futures.immediateFuture(UpdateModuleCategoryResponse.newBuilder()
						.setResult(UpdateModuleCategoryResponse.Result.FAIL_CATEGORY_NOT_EXIST)
						.setFailText("分类id不存在！")
						.build());
			}

			DiscoverV2DB.updateModuleCategory(dbConn,
					companyId,
					categoryId,
					moduleId,
					categoryName,
					allowModelId,
					head.getSession().getAdminId(),
					(int) (System.currentTimeMillis() / 1000L));
		} catch (SQLException e) {
			throw new RuntimeException("db failed", e);
		} finally {
			DBUtil.closeQuietly(dbConn);
		}

		Jedis jedis = jedisPool.getResource();
		try {
			DiscoverV2Cache.delDiscoverHome(jedis, Collections.singleton(companyId));
		} finally {
			jedis.close();
		}
		return Futures.immediateFuture(UpdateModuleCategoryResponse.newBuilder()
				.setResult(UpdateModuleCategoryResponse.Result.SUCC)
				.build());
	}

	@Override
	public ListenableFuture<UpdateModuleCategoryStateResponse> updateModuleCategoryState(AdminHead head, UpdateModuleCategoryStateRequest request) {
		if (!head.hasCompanyId()) {
			return Futures.immediateFuture(UpdateModuleCategoryStateResponse.newBuilder()
					.setResult(UpdateModuleCategoryStateResponse.Result.FAIL_UNKNOWN)
					.setFailText("company_id 参数未填")
					.build());
		}
		final long companyId = head.getCompanyId();
		final List<Integer> categoryIdList = request.getCategoryIdList();
		
		if (categoryIdList.isEmpty()) {
			return Futures.immediateFuture(UpdateModuleCategoryStateResponse.newBuilder()
					.setResult(UpdateModuleCategoryStateResponse.Result.SUCC)
					.build());
		}
		
		Connection dbConn = null;
		try {
			dbConn = hikariDataSource.getConnection();

			DiscoverV2DB.updateModuleCategoryState(dbConn, companyId, categoryIdList, ADMIN_STATE_LIST, request.getState());
		} catch (SQLException e) {
			throw new RuntimeException("db failed", e);
		} finally {
			DBUtil.closeQuietly(dbConn);
		}

		Jedis jedis = jedisPool.getResource();
		try {
			DiscoverV2Cache.delDiscoverHome(jedis, Collections.singleton(companyId));
		} finally {
			jedis.close();
		}
		
		return Futures.immediateFuture(UpdateModuleCategoryStateResponse.newBuilder()
				.setResult(UpdateModuleCategoryStateResponse.Result.SUCC)
				.build());
	}

	@Override
	public ListenableFuture<UpdateModuleCategoryOrderResponse> updateModuleCategoryOrder(AdminHead head, UpdateModuleCategoryOrderRequest request) {
		if (!head.hasCompanyId()) {
			return Futures.immediateFuture(UpdateModuleCategoryOrderResponse.newBuilder()
					.setResult(UpdateModuleCategoryOrderResponse.Result.FAIL_UNKNOWN)
					.setFailText("company_id 参数未填")
					.build());
		}
		final long companyId = head.getCompanyId();
		final int moduleId = request.getModuleId();
		final List<Integer> categoryOrderIdList = request.getCategoryOrderIdList();

		if (categoryOrderIdList.size() > 100) {
			return Futures.immediateFuture(UpdateModuleCategoryOrderResponse.newBuilder()
					.setResult(UpdateModuleCategoryOrderResponse.Result.FAIL_CATEGORY_ORDER_ID_INVALID)
					.setFailText("分类序列字符串长度超出范围！")
					.build());
		}

		Connection dbConn = null;
		try {
			dbConn = hikariDataSource.getConnection();
			
			DiscoverV2Protos.Module module = DiscoverV2DB.getModuleById(dbConn, companyId, Collections.singleton(moduleId), ADMIN_STATE_LIST).get(moduleId);
			if (module == null) {
				return Futures.immediateFuture(UpdateModuleCategoryOrderResponse.newBuilder()
						.setResult(UpdateModuleCategoryOrderResponse.Result.FAIL_MODULE_NOT_EXIST)
						.setFailText("模块id不存在！")
						.build());
			}
			
			DiscoverV2DB.updateModuleCategoryOrder(dbConn,
					companyId,
					moduleId,
					categoryOrderIdList,
					head.getSession().getAdminId(),
					(int) (System.currentTimeMillis() / 1000L));
		} catch (SQLException e) {
			throw new RuntimeException("db failed", e);
		} finally {
			DBUtil.closeQuietly(dbConn);
		}

		Jedis jedis = jedisPool.getResource();
		try {
			DiscoverV2Cache.delDiscoverHome(jedis, Collections.singleton(companyId));
		} finally {
			jedis.close();
		}
		
		return Futures.immediateFuture(UpdateModuleCategoryOrderResponse.newBuilder()
				.setResult(UpdateModuleCategoryOrderResponse.Result.SUCC)
				.build());
	}

	@Override
	public ListenableFuture<MigrateModuleCategoryResponse> migrateModuleCategory(AdminHead head, MigrateModuleCategoryRequest request) {
		if (!head.hasCompanyId()) {
			return Futures.immediateFuture(MigrateModuleCategoryResponse.newBuilder()
					.setResult(MigrateModuleCategoryResponse.Result.FAIL_UNKNOWN)
					.setFailText("company_id 参数未填")
					.build());
		}
		final long companyId = head.getCompanyId();
		final List<Integer> categoryIdList = request.getCategoryIdList();
		final int moduleId = request.getModuleId();

		if (categoryIdList.isEmpty()) {
			return Futures.immediateFuture(MigrateModuleCategoryResponse.newBuilder()
					.setResult(MigrateModuleCategoryResponse.Result.SUCC)
					.build());
		}
		
		Connection dbConn = null;
		try {
			dbConn = hikariDataSource.getConnection();

			DiscoverV2Protos.Module module = DiscoverV2DB.getModuleById(dbConn, companyId, Collections.singleton(moduleId), ADMIN_STATE_LIST).get(moduleId);
			if (module == null) {
				return Futures.immediateFuture(MigrateModuleCategoryResponse.newBuilder()
						.setResult(MigrateModuleCategoryResponse.Result.FAIL_MODULE_NOT_EXIST)
						.setFailText("模块id不存在！")
						.build());
			}

			DiscoverV2DB.updateModuleCategoryModuleId(dbConn,
					companyId,
					request.getCategoryIdList(),
					ADMIN_STATE_LIST, 
					moduleId,
					head.getSession().getAdminId(),
					(int) (System.currentTimeMillis() / 1000L));
		} catch (SQLException e) {
			throw new RuntimeException("db failed", e);
		} finally {
			DBUtil.closeQuietly(dbConn);
		}

		Jedis jedis = jedisPool.getResource();
		try {
			DiscoverV2Cache.delDiscoverHome(jedis, Collections.singleton(companyId));
		} finally {
			jedis.close();
		}
		return Futures.immediateFuture(MigrateModuleCategoryResponse.newBuilder()
				.setResult(MigrateModuleCategoryResponse.Result.SUCC)
				.build());
	}
	
	@Override
	public ListenableFuture<AddItemToCategoryResponse> addItemToCategory(AdminHead head, AddItemToCategoryRequest request) {
		if (!head.hasCompanyId()) {
			return Futures.immediateFuture(AddItemToCategoryResponse.newBuilder()
					.setResult(AddItemToCategoryResponse.Result.FAIL_UNKNOWN)
					.setFailText("company_id 参数未填")
					.build());
		}
		final long companyId = head.getCompanyId();
		final List<Long> itemIdList = request.getItemIdList();
		final int categoryId = request.getCategoryId();

		if (itemIdList.isEmpty()) {
			return Futures.immediateFuture(AddItemToCategoryResponse.newBuilder()
					.setResult(AddItemToCategoryResponse.Result.SUCC)
					.build());
		}
		
		Connection dbConn = null;
		try {
			dbConn = hikariDataSource.getConnection();

			DiscoverV2Protos.Module.Category category = DiscoverV2DB.getModuleCategoryById(dbConn, companyId, Collections.singleton(categoryId), ADMIN_STATE_LIST).get(categoryId);
			if (category == null) {
				return Futures.immediateFuture(AddItemToCategoryResponse.newBuilder()
						.setResult(AddItemToCategoryResponse.Result.FAIL_CATEGORY_NOT_EXIST)
						.setFailText("该分类不存在！")
						.build());
			}

			DiscoverV2DB.addItemToCategory(dbConn,
					companyId,
					Collections.singletonMap(categoryId, itemIdList), 
					head.getSession().getAdminId(),
					(int) (System.currentTimeMillis() / 1000L));
		} catch (SQLException e) {
			throw new RuntimeException("db failed", e);
		} finally {
			DBUtil.closeQuietly(dbConn);
		}
		
		Jedis jedis = jedisPool.getResource();
		try {
			DiscoverV2Cache.delModuleCategoryItemList(jedis, companyId, Collections.singleton(categoryId));
		} finally {
			jedis.close();
		}
		return Futures.immediateFuture(AddItemToCategoryResponse.newBuilder()
				.setResult(AddItemToCategoryResponse.Result.SUCC)
				.build());
	}
	
	@Override
	public ListenableFuture<DeleteItemFromCategoryResponse> deleteItemFromCategory(AdminHead head, DeleteItemFromCategoryRequest request) {
		if (!head.hasCompanyId()) {
			return Futures.immediateFuture(DeleteItemFromCategoryResponse.newBuilder()
					.setResult(DeleteItemFromCategoryResponse.Result.FAIL_UNKNOWN)
					.setFailText("company_id 参数未填")
					.build());
		}
		final long companyId = head.getCompanyId();
		final List<Long> itemIdList = request.getItemIdList();
		final int categoryId = request.getCategoryId();

		Connection dbConn = null;
		try {
			dbConn = hikariDataSource.getConnection();
			
			DiscoverV2Protos.Module.Category category = DiscoverV2DB.getModuleCategoryById(dbConn, companyId, Collections.singleton(categoryId), ADMIN_STATE_LIST).get(categoryId);
			if (category == null) {
				return Futures.immediateFuture(DeleteItemFromCategoryResponse.newBuilder()
						.setResult(DeleteItemFromCategoryResponse.Result.FAIL_CATEGORY_NOT_EXIST)
						.setFailText("该分类不存在！")
						.build());
			}

			DiscoverV2DB.deleteItemFromCategory(dbConn, companyId, Collections.singletonMap(categoryId, itemIdList));
		} catch (SQLException e) {
			throw new RuntimeException("db failed", e);
		} finally {
			DBUtil.closeQuietly(dbConn);
		}

		Jedis jedis = jedisPool.getResource();
		try {
			DiscoverV2Cache.delModuleCategoryItemList(jedis, companyId, Collections.singleton(categoryId));
		} finally {
			jedis.close();
		}
		return Futures.immediateFuture(DeleteItemFromCategoryResponse.newBuilder()
				.setResult(DeleteItemFromCategoryResponse.Result.SUCC)
				.build());
	}
	
	
	@Override
	public ListenableFuture<GetItemListResponse> getItemList(AdminHead head, GetItemListRequest request) {
		if (!head.hasCompanyId()) {
			return Futures.immediateFuture(GetItemListResponse.newBuilder()
					.setTotalSize(0)
					.setFilteredSize(0)
					.build());
		}
		return Futures.immediateFuture(this.doGetItemList(head.getCompanyId(), request, ADMIN_STATE_LIST));
	}
	
	@Override
	public ListenableFuture<GetItemListResponse> getItemList(SystemHead head, GetItemListRequest request) {
		if (!head.hasCompanyId()) {
			return Futures.immediateFuture(GetItemListResponse.newBuilder()
					.setTotalSize(0)
					.setFilteredSize(0)
					.build());
		}
		return Futures.immediateFuture(this.doGetItemList(head.getCompanyId(), request, null));
	}
	
	@Override
	public ListenableFuture<GetItemListResponse> getItemList(BossHead head, GetItemListRequest request) {
		if (!head.hasCompanyId()) {
			return Futures.immediateFuture(GetItemListResponse.newBuilder()
					.setTotalSize(0)
					.setFilteredSize(0)
					.build());
		}
		return Futures.immediateFuture(this.doGetItemList(head.getCompanyId(), request, null));
	}
	
	private GetItemListResponse doGetItemList(final long companyId, GetItemListRequest request, @Nullable Collection<DiscoverV2Protos.State> states) {
		final int start = request.getStart() < 0 ? 0 : request.getStart();
		final int length = request.getLength() < 0 ? 0 : request.getLength();
		final Integer categoryId = request.hasCategoryId() ? request.getCategoryId() : null;
		final String itemNameKeyword = request.hasItemName() && !request.getItemName().trim().isEmpty() ? request.getItemName().trim() : null;
		final Boolean orderCreateTimeAsc = request.hasOrderCreateTimeAsc() ? request.getOrderCreateTimeAsc() : null;
		
		DataPage<Long> itemIdPage;
		Map<Long, Set<Integer>> itemCategoryIdMap;
		Map<Integer, DiscoverV2Protos.Module.Category> categoryMap;
		Map<Integer, DiscoverV2Protos.Module> moduleMap;

		Connection dbConn = null;
		try {
			dbConn = hikariDataSource.getConnection();

			itemIdPage = DiscoverV2DB.getItemIdPage(dbConn, companyId, start, length, categoryId, itemNameKeyword, orderCreateTimeAsc, states);
			itemCategoryIdMap = DiscoverV2DB.getItemCategoryId(dbConn, companyId, itemIdPage.dataList());
			
			Set<Integer> categoryIdSet = new TreeSet<Integer>();
			for (Set<Integer> set : itemCategoryIdMap.values()) {
				categoryIdSet.addAll(set);
			}
			categoryMap = DiscoverV2DB.getModuleCategoryById(dbConn, companyId, categoryIdSet, states);
			
			Set<Integer> moduleIdSet = new TreeSet<Integer>();
			for (DiscoverV2Protos.Module.Category category : categoryMap.values()) {
				moduleIdSet.add(category.getModuleId());
			}
			moduleMap = DiscoverV2DB.getModuleById(dbConn, companyId, moduleIdSet, states);
		} catch (SQLException e) {
			throw new RuntimeException("db failed", e);
		} finally {
			DBUtil.closeQuietly(dbConn);
		}

		Map<Long, DiscoverV2Protos.Item> itemMap = DiscoverUtil.getItem(hikariDataSource, jedisPool, companyId, itemIdPage.dataList(), states, null);
		
		GetItemListResponse.Builder responseBuilder = GetItemListResponse.newBuilder();
		for (Long itemId : itemIdPage.dataList()) {
			DiscoverV2Protos.Item item = itemMap.get(itemId);
			if (item != null) {
				responseBuilder.addItem(item);
			}
		}
		responseBuilder.setFilteredSize(itemIdPage.filteredSize());
		responseBuilder.setTotalSize(itemIdPage.totalSize());
		
		AdminDiscoverProtos.ItemCategory.Builder tmpBuilder = AdminDiscoverProtos.ItemCategory.newBuilder();
		for (Entry<Long, Set<Integer>> entry : itemCategoryIdMap.entrySet()) {
			tmpBuilder.clear();
			tmpBuilder.setItemId(entry.getKey());
			for (Integer catId : entry.getValue()) {
				if (categoryMap.containsKey(catId)) {
					tmpBuilder.addCategoryId(catId);
				}
			}
			if (tmpBuilder.getCategoryIdCount() > 0) {
				responseBuilder.addRefItemCategory(tmpBuilder.build());
			}
		}
		
		responseBuilder.addAllRefCategory(categoryMap.values());
		responseBuilder.addAllRefModule(moduleMap.values());
		return responseBuilder.build();
	}
	
	@Override
	public ListenableFuture<GetItemByIdResponse> getItemById(AdminHead head, GetItemByIdRequest request) {
		if (!head.hasCompanyId()) {
			return Futures.immediateFuture(GetItemByIdResponse.newBuilder().build());
		}
		return Futures.immediateFuture(this.doGetItemById(head.getCompanyId(), ADMIN_STATE_LIST, request));
	}

	@Override
	public ListenableFuture<GetItemByIdResponse> getItemById(BossHead head, GetItemByIdRequest request) {
		if (!head.hasCompanyId()) {
			return Futures.immediateFuture(GetItemByIdResponse.getDefaultInstance());
		}
		return Futures.immediateFuture(this.doGetItemById(head.getCompanyId(), ADMIN_STATE_LIST, request));
	}
	
	private GetItemByIdResponse doGetItemById(final long companyId, @Nullable Collection<DiscoverV2Protos.State> states, GetItemByIdRequest request) {
		final Set<Long> itemIdSet = new TreeSet<Long>(request.getItemIdList());
		if (itemIdSet.isEmpty()) {
			return GetItemByIdResponse.getDefaultInstance();
		}

		Map<Long, Set<Integer>> itemCategoryIdMap;
		Map<Integer, DiscoverV2Protos.Module.Category> categoryMap;
		Map<Integer, DiscoverV2Protos.Module> moduleMap;

		Connection dbConn = null;
		try {
			dbConn = hikariDataSource.getConnection();

			itemCategoryIdMap = DiscoverV2DB.getItemCategoryId(dbConn, companyId, itemIdSet);
			
			Set<Integer> categoryIdSet = new TreeSet<Integer>();
			for (Set<Integer> set : itemCategoryIdMap.values()) {
				categoryIdSet.addAll(set);
			}
			categoryMap = DiscoverV2DB.getModuleCategoryById(dbConn, companyId, categoryIdSet, states);
			
			Set<Integer> moduleIdSet = new TreeSet<Integer>();
			for (DiscoverV2Protos.Module.Category category : categoryMap.values()) {
				moduleIdSet.add(category.getModuleId());
			}
			moduleMap = DiscoverV2DB.getModuleById(dbConn, companyId, moduleIdSet, states);
		} catch (SQLException e) {
			throw new RuntimeException("db failed", e);
		} finally {
			DBUtil.closeQuietly(dbConn);
		}

		Map<Long, DiscoverV2Protos.Item> itemMap = DiscoverUtil.getItem(hikariDataSource, jedisPool, companyId, itemIdSet, states, null);
		
		GetItemByIdResponse.Builder responseBuilder = GetItemByIdResponse.newBuilder();
		responseBuilder.addAllItem(itemMap.values());
		
		AdminDiscoverProtos.ItemCategory.Builder tmpBuilder = AdminDiscoverProtos.ItemCategory.newBuilder();
		for (Entry<Long, Set<Integer>> entry : itemCategoryIdMap.entrySet()) {
			tmpBuilder.clear();
			tmpBuilder.setItemId(entry.getKey());
			for (Integer catId : entry.getValue()) {
				if (categoryMap.containsKey(catId)) {
					tmpBuilder.addCategoryId(catId);
				}
			}
			if (tmpBuilder.getCategoryIdCount() > 0) {
				responseBuilder.addRefItemCategory(tmpBuilder.build());
			}
		}
		
		responseBuilder.addAllRefCategory(categoryMap.values());
		responseBuilder.addAllRefModule(moduleMap.values());
		return responseBuilder.build();
	}
	
	@Override
	public ListenableFuture<CreateItemResponse> createItem(AdminHead head, CreateItemRequest request) {
		if (!head.hasCompanyId()) {
			return Futures.immediateFuture(CreateItemResponse.newBuilder()
					.setResult(CreateItemResponse.Result.FAIL_UNKNOWN)
					.setFailText("company_id 参数未填")
					.build());
		}
		return Futures.immediateFuture(this.doCreateItem(head.getCompanyId(), head.getSession().getAdminId(), request));
	}

	@Override
	public ListenableFuture<CreateItemResponse> createItem(BossHead head, CreateItemRequest request) {
		if (!head.hasCompanyId()) {
			return Futures.immediateFuture(CreateItemResponse.newBuilder()
					.setResult(CreateItemResponse.Result.FAIL_UNKNOWN)
					.setFailText("boss has no company_id")
					.build());
		}
		return Futures.immediateFuture(this.doCreateItem(head.getCompanyId(), null, request));
	}

	private CreateItemResponse doCreateItem(final long companyId, @Nullable Long adminId, CreateItemRequest request) {
		final Set<Integer> categoryIdSet = new TreeSet<Integer>(request.getCategoryIdList());
		final String itemName = request.getItemName().trim();
		final String itemDesc = request.getItemDesc();
		final String imageName = request.getImageName();
		final Integer allowModelId = request.hasAllowModelId() ? request.getAllowModelId() : null;
		final boolean enableComment = request.getEnableComment();
		final boolean enableScore = request.getEnableScore();
		final boolean enableRemind = request.getEnableRemind();
		final boolean enableLike = request.getEnableLike();
	    final boolean enableShare = request.getEnableShare();
		final Boolean enableExternalShare = request.hasEnableExternalShare() ? request.getEnableExternalShare() : null;
		final DiscoverV2Protos.WebUrl webUrl = request.hasWebUrl() ? request.getWebUrl() : null;
		final DiscoverV2Protos.AppUri appUri = request.hasAppUri() ? request.getAppUri() : null;
		final DiscoverV2Protos.Document document = request.hasDocument() ? request.getDocument() : null;
		final DiscoverV2Protos.Video video = request.hasVideo() ? request.getVideo() : null;
		final DiscoverV2Protos.Audio audio = request.hasAudio() ? request.getAudio() : null;

		if (itemName.isEmpty()) {
			return CreateItemResponse.newBuilder()
					.setResult(CreateItemResponse.Result.FAIL_ITEM_NAME_INVALID)
					.setFailText("条目名称为空！")
					.build();
		}
		if (itemName.length() > 191) {
			return CreateItemResponse.newBuilder()
					.setResult(CreateItemResponse.Result.FAIL_ITEM_NAME_INVALID)
					.setFailText("条目名称长度超出范围！")
					.build();
		}

		if (itemDesc.length() > 191) {
			return CreateItemResponse.newBuilder()
					.setResult(CreateItemResponse.Result.FAIL_ITEM_DESC_INVALID)
					.setFailText("条目描述长度超出范围！")
					.build();
		}

		if (imageName.length() > 191) {
			return CreateItemResponse.newBuilder()
					.setResult(CreateItemResponse.Result.FAIL_IMAGE_NAME_INVALID)
					.setFailText("图片名称长度超出范围！")
					.build();
		}
		
		if (webUrl != null) {
			String failText = DiscoverUtil.checkWebUrl(webUrl);
			if (failText != null) {
				return CreateItemResponse.newBuilder()
						.setResult(CreateItemResponse.Result.FAIL_WEB_URL_INVALID)
						.setFailText(failText)
						.build();
			}
		}
		if (appUri != null) {
			String failText = DiscoverUtil.checkAppUri(appUri);
			if (failText != null) {
				return CreateItemResponse.newBuilder()
						.setResult(CreateItemResponse.Result.FAIL_APP_URI_INVALID)
						.setFailText(failText)
						.build();
			}
		}
		if (document != null) {
			String failText = DiscoverUtil.checkDocument(document);
			if (failText != null) {
				return CreateItemResponse.newBuilder()
						.setResult(CreateItemResponse.Result.FAIL_DOCUMENT_INVALID)
						.setFailText(failText)
						.build();
			}
		}
		if (video != null) {
			String failText = DiscoverUtil.checkVideo(video);
			if (failText != null) {
				return CreateItemResponse.newBuilder()
						.setResult(CreateItemResponse.Result.FAIL_VIDEO_INVALID)
						.setFailText(failText)
						.build();
			}
		}
		if (audio != null) {
			String failText = DiscoverUtil.checkAudio(audio);
			if (failText != null) {
				return CreateItemResponse.newBuilder()
						.setResult(CreateItemResponse.Result.FAIL_AUDIO_INVALID)
						.setFailText(failText)
						.build();
			}
		}

		final int now = (int) (System.currentTimeMillis() / 1000L);
		DiscoverV2Protos.Item.Base.Builder itemBaseBuilder = DiscoverV2Protos.Item.Base.newBuilder()
				.setItemId(0)
				.setItemName(itemName)
				.setItemDesc(itemDesc)
				.setImageName(imageName)
				.setEnableComment(enableComment)
				.setEnableScore(enableScore)
				.setEnableRemind(enableRemind)
				.setEnableLike(enableLike)
				.setEnableShare(enableShare)
				.setState(DiscoverV2Protos.State.NORMAL)
				.setCreateTime(now);
		if (enableExternalShare != null) {
			itemBaseBuilder.setEnableExternalShare(enableExternalShare);
		}
		if (allowModelId != null) {
			itemBaseBuilder.setAllowModelId(allowModelId);
		}
		if (webUrl != null) {
			itemBaseBuilder.setWebUrl(webUrl);
		}
		if (appUri != null) {
			itemBaseBuilder.setAppUri(appUri);
		}
		if (document != null) {
			itemBaseBuilder.setDocument(document);
		}
		if (video != null) {
			itemBaseBuilder.setVideo(video);
		}
		if (audio != null) {
			itemBaseBuilder.setAudio(audio);
		}
		if (adminId != null) {
			itemBaseBuilder.setCreateAdminId(adminId);
		}
		
		final DiscoverV2Protos.Item.Base itemBase = itemBaseBuilder.build();
		final long itemId;
		Connection dbConn = null;
		try {
			dbConn = hikariDataSource.getConnection();

			if (!categoryIdSet.isEmpty()) {
				Map<Integer, DiscoverV2Protos.Module.Category> categoryMap = DiscoverV2DB.getModuleCategoryById(dbConn, companyId, categoryIdSet, ADMIN_STATE_LIST);
				List<Integer> notContainsCategoryIdList = new ArrayList<Integer>();
				for (Integer categoryId : categoryIdSet) {
					if (!categoryMap.containsKey(categoryId)) {
						notContainsCategoryIdList.add(categoryId);
					}
				}
				if (!notContainsCategoryIdList.isEmpty()) {
					return CreateItemResponse.newBuilder()
							.setResult(CreateItemResponse.Result.FAIL_CATEGORY_NOT_EXIST)
							.setFailText("分类id" + notContainsCategoryIdList + "不存在！")
							.build();
				}
			}

			itemId = DiscoverV2DB.insertItem(dbConn, companyId, Collections.singletonList(itemBase)).get(0);
			
			if (!categoryIdSet.isEmpty()) {
				DiscoverV2DB.updateItemCategoryId(dbConn, companyId, 
						Collections.<Long, Set<Integer>>singletonMap(itemId, Collections.<Integer>emptySet()), 
						Collections.<Long, Set<Integer>>singletonMap(itemId, categoryIdSet), 
						adminId, now);
			}
		} catch (SQLException e) {
			throw new RuntimeException("db failed", e);
		} finally {
			DBUtil.closeQuietly(dbConn);
		}

		Jedis jedis = jedisPool.getResource();
		try {
			DiscoverV2Cache.delItemBase(jedis, companyId, Collections.singleton(itemId));
			DiscoverV2Cache.delModuleCategoryItemList(jedis, companyId, categoryIdSet);
		} finally {
			jedis.close();
		}
		return CreateItemResponse.newBuilder()
				.setResult(CreateItemResponse.Result.SUCC)
				.setItemId(itemId)
				.build();
	}

	@Override
	public ListenableFuture<ImportItemResponse> importItem(AdminHead head, ImportItemRequest importRequest) {
		if (!head.hasCompanyId()) {
			return Futures.immediateFuture(ImportItemResponse.newBuilder()
					.setResult(ImportItemResponse.Result.FAIL_UNKNOWN)
					.setFailText("company_id 参数未填")
					.build());
		}
		final long companyId = head.getCompanyId();
		final List<CreateItemRequest> createItemRequestList = importRequest.getCreateItemRequestList();
		
		if (createItemRequestList.isEmpty()) {
			return Futures.immediateFuture(ImportItemResponse.newBuilder()
					.setResult(ImportItemResponse.Result.FAIL_UNKNOWN)
					.setFailText("导入数据不能为空！")
					.build());
		}
		
		List<DiscoverV2Protos.Item.Base> itemBaseList = new ArrayList<DiscoverV2Protos.Item.Base>(createItemRequestList.size());
		
		final long adminId = head.getSession().getAdminId();
		final int now = (int) (System.currentTimeMillis() / 1000L);
		for (CreateItemRequest request : createItemRequestList) {
			final String itemName = request.getItemName().trim();
			final String itemDesc = request.getItemDesc();
			final String imageName = request.getImageName();
			final Integer allowModelId = request.hasAllowModelId() ? request.getAllowModelId() : null;
			final boolean enableComment = request.getEnableComment();
			final boolean enableScore = request.getEnableScore();
			final boolean enableRemind = request.getEnableRemind();
			final boolean enableLike = request.getEnableLike();
		    final boolean enableShare = request.getEnableShare();
			final Boolean enableExternalShare = request.hasEnableExternalShare() ? request.getEnableExternalShare() : null;
			final DiscoverV2Protos.WebUrl webUrl = request.hasWebUrl() ? request.getWebUrl() : null;
			final DiscoverV2Protos.AppUri appUri = request.hasAppUri() ? request.getAppUri() : null;
			final DiscoverV2Protos.Document document = request.hasDocument() ? request.getDocument() : null;
			final DiscoverV2Protos.Video video = request.hasVideo() ? request.getVideo() : null;
			final DiscoverV2Protos.Audio audio = request.hasAudio() ? request.getAudio() : null;

			if (itemName.isEmpty()) {
				return Futures.immediateFuture(ImportItemResponse.newBuilder()
						.setResult(ImportItemResponse.Result.FAIL_ITEM_NAME_INVALID)
						.setFailText("条目名称为空！")
						.build());
			}
			if (itemName.length() > 191) {
				return Futures.immediateFuture(ImportItemResponse.newBuilder()
						.setResult(ImportItemResponse.Result.FAIL_ITEM_NAME_INVALID)
						.setFailText("条目名称长度超出范围！")
						.build());
			}

			if (itemDesc.length() > 191) {
				return Futures.immediateFuture(ImportItemResponse.newBuilder()
						.setResult(ImportItemResponse.Result.FAIL_ITEM_DESC_INVALID)
						.setFailText("条目描述长度超出范围！")
						.build());
			}

			if (imageName.length() > 191) {
				return Futures.immediateFuture(ImportItemResponse.newBuilder()
						.setResult(ImportItemResponse.Result.FAIL_IMAGE_NAME_INVALID)
						.setFailText("图片名称长度超出范围！")
						.build());
			}
			
			if (webUrl != null) {
				String failText = DiscoverUtil.checkWebUrl(webUrl);
				if (failText != null) {
					return Futures.immediateFuture(ImportItemResponse.newBuilder()
							.setResult(ImportItemResponse.Result.FAIL_WEB_URL_INVALID)
							.setFailText(failText)
							.build());
				}
			}
			if (appUri != null) {
				String failText = DiscoverUtil.checkAppUri(appUri);
				if (failText != null) {
					return Futures.immediateFuture(ImportItemResponse.newBuilder()
							.setResult(ImportItemResponse.Result.FAIL_APP_URI_INVALID)
							.setFailText(failText)
							.build());
				}
			}
			if (document != null) {
				String failText = DiscoverUtil.checkDocument(document);
				if (failText != null) {
					return Futures.immediateFuture(ImportItemResponse.newBuilder()
							.setResult(ImportItemResponse.Result.FAIL_DOCUMENT_INVALID)
							.setFailText(failText)
							.build());
				}
			}
			if (video != null) {
				String failText = DiscoverUtil.checkVideo(video);
				if (failText != null) {
					return Futures.immediateFuture(ImportItemResponse.newBuilder()
							.setResult(ImportItemResponse.Result.FAIL_VIDEO_INVALID)
							.setFailText(failText)
							.build());
				}
			}
			if (audio != null) {
				String failText = DiscoverUtil.checkAudio(audio);
				if (failText != null) {
					return Futures.immediateFuture(ImportItemResponse.newBuilder()
							.setResult(ImportItemResponse.Result.FAIL_AUDIO_INVALID)
							.setFailText(failText)
							.build());
				}
			}
			
			DiscoverV2Protos.Item.Base.Builder itemBaseBuilder = DiscoverV2Protos.Item.Base.newBuilder()
					.setItemId(0)
					.setItemName(itemName)
					.setItemDesc(itemDesc)
					.setImageName(imageName)
					.setEnableComment(enableComment)
					.setEnableScore(enableScore)
					.setEnableRemind(enableRemind)
					.setEnableLike(enableLike)
					.setEnableShare(enableShare)
					.setState(DiscoverV2Protos.State.NORMAL)
					.setCreateAdminId(adminId)
					.setCreateTime(now);
			if (enableExternalShare != null) {
				itemBaseBuilder.setEnableExternalShare(enableExternalShare);
			}
			if (allowModelId != null) {
				itemBaseBuilder.setAllowModelId(allowModelId);
			}
			if (webUrl != null) {
				itemBaseBuilder.setWebUrl(webUrl);
			}
			if (appUri != null) {
				itemBaseBuilder.setAppUri(appUri);
			}
			if (document != null) {
				itemBaseBuilder.setDocument(document);
			}
			if (video != null) {
				itemBaseBuilder.setVideo(video);
			}
			if (audio != null) {
				itemBaseBuilder.setAudio(audio);
			}
			
			itemBaseList.add(itemBaseBuilder.build());
		}
		
		final List<Long> itemIdList;
		Connection dbConn = null;
		try {
			dbConn = hikariDataSource.getConnection();
			itemIdList = DiscoverV2DB.insertItem(dbConn, companyId, itemBaseList);
		} catch (SQLException e) {
			throw new RuntimeException("db failed", e);
		} finally {
			DBUtil.closeQuietly(dbConn);
		}

		Jedis jedis = jedisPool.getResource();
		try {
			DiscoverV2Cache.delItemBase(jedis, companyId, itemIdList);
		} finally {
			jedis.close();
		}
		return Futures.immediateFuture(ImportItemResponse.newBuilder()
				.setResult(ImportItemResponse.Result.SUCC)
				.addAllItemId(itemIdList)
				.build());
	}
	
	@Override
	public ListenableFuture<UpdateItemResponse> updateItem(AdminHead head, UpdateItemRequest request) {
		if (!head.hasCompanyId()) {
			return Futures.immediateFuture(UpdateItemResponse.newBuilder()
					.setResult(UpdateItemResponse.Result.FAIL_UNKNOWN)
					.setFailText("company_id 参数未填")
					.build());
		}
		return Futures.immediateFuture(this.doUpdateItem(head.getCompanyId(), head.getSession().getAdminId(), ADMIN_STATE_LIST, request));
	}
	
	@Override
	public ListenableFuture<UpdateItemResponse> updateItem(BossHead head, UpdateItemRequest request) {
		if (!head.hasCompanyId()) {
			return Futures.immediateFuture(UpdateItemResponse.newBuilder()
					.setResult(UpdateItemResponse.Result.FAIL_UNKNOWN)
					.setFailText("boss head no company_id")
					.build());
		}
		return Futures.immediateFuture(this.doUpdateItem(head.getCompanyId(), null, null, request));
	}
	
	private UpdateItemResponse doUpdateItem(final long companyId, @Nullable Long adminId, @Nullable Collection<DiscoverV2Protos.State> states, UpdateItemRequest request) {
		final long itemId = request.getItemId();
		final Set<Integer> categoryIdSet = new TreeSet<Integer>(request.getCategoryIdList());
		final String itemName = request.getItemName();
		final String itemDesc = request.hasItemDesc() ? request.getItemDesc() : null;
		final String imageName = request.hasImageName() ? request.getImageName() : null;
		final Integer allowModelId = request.hasAllowModelId() ? request.getAllowModelId() : null;
		final boolean enableComment = request.getEnableComment();
		final boolean enableScore = request.getEnableScore();
		final boolean enableRemind = request.getEnableRemind();
		final boolean enableLike = request.getEnableLike();
		final boolean enableShare = request.getEnableShare();
		final Boolean enableExternalShare = request.hasEnableExternalShare() ? request.getEnableExternalShare() : null;
		final DiscoverV2Protos.WebUrl webUrl = request.hasWebUrl() ? request.getWebUrl() : null;
		final DiscoverV2Protos.AppUri appUri = request.hasAppUri() ? request.getAppUri() : null;
		final DiscoverV2Protos.Document document = request.hasDocument() ? request.getDocument() : null;
		final DiscoverV2Protos.Video video = request.hasVideo() ? request.getVideo() : null;
		final DiscoverV2Protos.Audio audio = request.hasAudio() ? request.getAudio() : null;

		if (itemName.isEmpty()) {
			return UpdateItemResponse.newBuilder()
					.setResult(UpdateItemResponse.Result.FAIL_ITEM_NAME_INVALID)
					.setFailText("条目名称为空！")
					.build();
		}
		if (itemName.length() > 191) {
			return UpdateItemResponse.newBuilder()
					.setResult(UpdateItemResponse.Result.FAIL_ITEM_NAME_INVALID)
					.setFailText("条目名称长度超出范围！")
					.build();
		}

		if (itemDesc.length() > 191) {
			return UpdateItemResponse.newBuilder()
					.setResult(UpdateItemResponse.Result.FAIL_ITEM_DESC_INVALID)
					.setFailText("条目描述长度超出范围！")
					.build();
		}

		if (imageName.length() > 191) {
			return UpdateItemResponse.newBuilder()
					.setResult(UpdateItemResponse.Result.FAIL_IMAGE_NAME_INVALID)
					.setFailText("图片名称长度超出范围！")
					.build();
		}
		
		if (webUrl != null) {
			String failText = DiscoverUtil.checkWebUrl(webUrl);
			if (failText != null) {
				return UpdateItemResponse.newBuilder()
						.setResult(UpdateItemResponse.Result.FAIL_WEB_URL_INVALID)
						.setFailText(failText)
						.build();
			}
		}
		if (appUri != null) {
			String failText = DiscoverUtil.checkAppUri(appUri);
			if (failText != null) {
				return UpdateItemResponse.newBuilder()
						.setResult(UpdateItemResponse.Result.FAIL_APP_URI_INVALID)
						.setFailText(failText)
						.build();
			}
		}
		if (document != null) {
			String failText = DiscoverUtil.checkDocument(document);
			if (failText != null) {
				return UpdateItemResponse.newBuilder()
						.setResult(UpdateItemResponse.Result.FAIL_DOCUMENT_INVALID)
						.setFailText(failText)
						.build();
			}
		}
		if (video != null) {
			String failText = DiscoverUtil.checkVideo(video);
			if (failText != null) {
				return UpdateItemResponse.newBuilder()
						.setResult(UpdateItemResponse.Result.FAIL_VIDEO_INVALID)
						.setFailText(failText)
						.build();
			}
		}
		if (audio != null) {
			String failText = DiscoverUtil.checkAudio(audio);
			if (failText != null) {
				return UpdateItemResponse.newBuilder()
						.setResult(UpdateItemResponse.Result.FAIL_AUDIO_INVALID)
						.setFailText(failText)
						.build();
			}
		}

		final int now = (int) (System.currentTimeMillis() / 1000L);
		DiscoverV2Protos.Item.Base.Builder itemBaseBuilder = DiscoverV2Protos.Item.Base.newBuilder()
				.setItemId(itemId)
				.setItemName(itemName)
				.setItemDesc(itemDesc)
				.setImageName(imageName)
				.setEnableComment(enableComment)
				.setEnableScore(enableScore)
				.setEnableRemind(enableRemind)
				.setEnableLike(enableLike)
				.setEnableShare(enableShare)
				.setState(DiscoverV2Protos.State.NORMAL)
				.setUpdateTime(now);
		if (enableExternalShare != null) {
			itemBaseBuilder.setEnableExternalShare(enableExternalShare);
		}
		if (allowModelId != null) {
			itemBaseBuilder.setAllowModelId(allowModelId);
		}
		if (webUrl != null) {
			itemBaseBuilder.setWebUrl(webUrl);
		}
		if (appUri != null) {
			itemBaseBuilder.setAppUri(appUri);
		}
		if (document != null) {
			itemBaseBuilder.setDocument(document);
		}
		if (video != null) {
			itemBaseBuilder.setVideo(video);
		}
		if (audio != null) {
			itemBaseBuilder.setAudio(audio);
		}
		if (adminId != null) {
			itemBaseBuilder.setUpdateAdminId(adminId);
		}
		
		final DiscoverV2Protos.Item.Base itemBase = itemBaseBuilder.build();

		Set<Integer> oldCategoryIdSet;
		Connection dbConn = null;
		try {
			dbConn = hikariDataSource.getConnection();

			if (!categoryIdSet.isEmpty()) {
				Map<Integer, DiscoverV2Protos.Module.Category> categoryMap = DiscoverV2DB.getModuleCategoryById(dbConn, companyId, categoryIdSet, states);
				List<Integer> notContainsCategoryIdList = new ArrayList<Integer>();
				for (Integer categoryId : categoryIdSet) {
					if (!categoryMap.containsKey(categoryId)) {
						notContainsCategoryIdList.add(categoryId);
					}
				}
				if (!notContainsCategoryIdList.isEmpty()) {
					return UpdateItemResponse.newBuilder()
							.setResult(UpdateItemResponse.Result.FAIL_CATEGORY_NOT_EXIST)
							.setFailText("分类id" + notContainsCategoryIdList + "不存在！")
							.build();
				}
			}

			DiscoverV2Protos.Item.Base oldItemBase = DiscoverUtil.getItemBase(hikariDataSource, jedisPool, companyId, Collections.singleton(itemId), states).get(itemId);
			if (oldItemBase == null) {
				return UpdateItemResponse.newBuilder()
						.setResult(UpdateItemResponse.Result.FAIL_ITEM_NOT_EXIST)
						.setFailText("该条目不存在！")
						.build();
			}
			
			DiscoverV2DB.updateItem(dbConn, companyId, itemBase);
			
			oldCategoryIdSet = DiscoverV2DB.getItemCategoryId(dbConn, companyId, Collections.singleton(itemId)).get(itemId);
			DiscoverV2DB.updateItemCategoryId(dbConn, companyId, 
					Collections.singletonMap(itemId, oldCategoryIdSet), 
					Collections.singletonMap(itemId, categoryIdSet), 
					adminId, now);
		} catch (SQLException e) {
			throw new RuntimeException("db failed", e);
		} finally {
			DBUtil.closeQuietly(dbConn);
		}

		// 原分类的条目列表缓存也要清楚
		Set<Integer> clearCacheCategoryIdSet = new TreeSet<Integer>();
		for (Integer newCategoryId : categoryIdSet) {
			if (!oldCategoryIdSet.contains(newCategoryId)) {
				clearCacheCategoryIdSet.add(newCategoryId);
			}
		}
		for (Integer oldCategoryId : oldCategoryIdSet) {
			if (!categoryIdSet.contains(oldCategoryId)) {
				clearCacheCategoryIdSet.add(oldCategoryId);
			}
		}
		
		Jedis jedis = jedisPool.getResource();
		try {
			DiscoverV2Cache.delItemBase(jedis, companyId, Collections.singleton(itemId));
			DiscoverV2Cache.delModuleCategoryItemList(jedis, companyId, clearCacheCategoryIdSet);
		} finally {
			jedis.close();
		}
		return UpdateItemResponse.newBuilder()
				.setResult(UpdateItemResponse.Result.SUCC)
				.build();
	}

	@Override
	public ListenableFuture<UpdateItemStateResponse> updateItemState(AdminHead head, UpdateItemStateRequest request) {
		if (!head.hasCompanyId()) {
			return Futures.immediateFuture(UpdateItemStateResponse.newBuilder()
					.setResult(UpdateItemStateResponse.Result.FAIL_UNKNOWN)
					.setFailText("company_id 参数未填")
					.build());
		}
		final long companyId = head.getCompanyId();
		final Set<Long> itemIdSet = new TreeSet<Long>(request.getItemIdList());

		if (itemIdSet.isEmpty()) {
			return Futures.immediateFuture(UpdateItemStateResponse.newBuilder()
					.setResult(UpdateItemStateResponse.Result.SUCC)
					.build());
		}
		
		Map<Long, Set<Integer>> itemCategoryIdMap;
		Connection dbConn = null;
		try {
			dbConn = hikariDataSource.getConnection();
			
			DiscoverV2DB.updateItemState(dbConn, companyId, itemIdSet, ADMIN_STATE_LIST, request.getState());
			if (request.getState() == DiscoverV2Protos.State.NORMAL) {
				itemCategoryIdMap = null;
			} else {
				itemCategoryIdMap = DiscoverV2DB.getItemCategoryId(dbConn, companyId, itemIdSet);
			}
		} catch (SQLException e) {
			throw new RuntimeException("db failed", e);
		} finally {
			DBUtil.closeQuietly(dbConn);
		}

		Jedis jedis = jedisPool.getResource();
		try {
			DiscoverV2Cache.delItemBase(jedis, companyId, itemIdSet);
			if (itemCategoryIdMap != null) {
				Set<Integer> categoryIdSet = new TreeSet<Integer>();
				for (Set<Integer> set : itemCategoryIdMap.values()) {
					categoryIdSet.addAll(set);
				}
				DiscoverV2Cache.delModuleCategoryItemList(jedis, companyId, categoryIdSet);
			}
		} finally {
			jedis.close();
		}
		return Futures.immediateFuture(UpdateItemStateResponse.newBuilder()
				.setResult(UpdateItemStateResponse.Result.SUCC)
				.build());
	}
	
	@Override
	public ListenableFuture<GetItemLearnListResponse> getItemLearnList(AdminHead head, GetItemLearnListRequest request) {
		if (!head.hasCompanyId()) {
			return Futures.immediateFuture(GetItemLearnListResponse.newBuilder()
					.setTotalSize(0)
					.setFilteredSize(0)
					.build());
		}
		final long companyId = head.getCompanyId();
		final long itemId = request.getItemId();
		final int start = request.getStart() < 0 ? 0 : request.getStart();
		final int length = request.getLength() < 0 ? 0 : request.getLength();

		DataPage<DiscoverV2Protos.ItemLearn> itemLearnPage;
		Connection dbConn = null;
		try {
			dbConn = hikariDataSource.getConnection();
			itemLearnPage = DiscoverV2DB.getItemLearnPage(dbConn, companyId, itemId, start, length);
		} catch (SQLException e) {
			throw new RuntimeException("db failed", e);
		} finally {
			DBUtil.closeQuietly(dbConn);
		}

		return Futures.immediateFuture(GetItemLearnListResponse.newBuilder()
				.addAllItemLearn(itemLearnPage.dataList())
				.setTotalSize(itemLearnPage.totalSize())
				.setFilteredSize(itemLearnPage.filteredSize())
				.build());
	}

	@Override
	public ListenableFuture<GetItemCommentListResponse> getItemCommentList(AdminHead head, GetItemCommentListRequest request) {
		if (!head.hasCompanyId()) {
			return Futures.immediateFuture(GetItemCommentListResponse.newBuilder()
					.setTotalSize(0)
					.setFilteredSize(0)
					.build());
		}
		final long companyId = head.getCompanyId();
		final long itemId = request.getItemId();
		final int start = request.getStart() < 0 ? 0 : request.getStart();
		final int length = request.getLength() < 0 ? 0 : request.getLength();

		DataPage<DiscoverV2Protos.ItemComment> itemCommentPage;
		Connection dbConn = null;
		try {
			dbConn = hikariDataSource.getConnection();
			itemCommentPage = DiscoverV2DB.getItemCommentPage(dbConn, companyId, itemId, start, length, false);
		} catch (SQLException e) {
			throw new RuntimeException("db failed", e);
		} finally {
			DBUtil.closeQuietly(dbConn);
		}

		return Futures.immediateFuture(GetItemCommentListResponse.newBuilder()
				.addAllItemComment(itemCommentPage.dataList())
				.setTotalSize(itemCommentPage.totalSize())
				.setFilteredSize(itemCommentPage.filteredSize())
				.build());
	}
	
	@Override
	public ListenableFuture<GetItemScoreListResponse> getItemScoreList(AdminHead head, GetItemScoreListRequest request) {
		if (!head.hasCompanyId()) {
			return Futures.immediateFuture(GetItemScoreListResponse.newBuilder()
					.setTotalSize(0)
					.setFilteredSize(0)
					.build());
		}
		final long companyId = head.getCompanyId();
		final long itemId = request.getItemId();
		final int start = request.getStart() < 0 ? 0 : request.getStart();
		final int length = request.getLength() < 0 ? 0 : request.getLength();

		DataPage<DiscoverV2Protos.ItemScore> itemScorePage;
		Connection dbConn = null;
		try {
			dbConn = hikariDataSource.getConnection();
			itemScorePage = DiscoverV2DB.getItemScorePage(dbConn, companyId, itemId, start, length);
		} catch (SQLException e) {
			throw new RuntimeException("db failed", e);
		} finally {
			DBUtil.closeQuietly(dbConn);
		}

		return Futures.immediateFuture(GetItemScoreListResponse.newBuilder()
				.addAllItemScore(itemScorePage.dataList())
				.setTotalSize(itemScorePage.totalSize())
				.setFilteredSize(itemScorePage.filteredSize())
				.build());
	}
	
	@Override
	public ListenableFuture<GetItemLikeListResponse> getItemLikeList(AdminHead head, GetItemLikeListRequest request) {
		if (!head.hasCompanyId()) {
			return Futures.immediateFuture(GetItemLikeListResponse.newBuilder()
					.setTotalSize(0)
					.setFilteredSize(0)
					.build());
		}
		final long companyId = head.getCompanyId();
		final long itemId = request.getItemId();
		final int start = request.getStart() < 0 ? 0 : request.getStart();
		final int length = request.getLength() < 0 ? 0 : request.getLength();

		DataPage<DiscoverV2Protos.ItemLike> itemLikePage;
		Connection dbConn = null;
		try {
			dbConn = hikariDataSource.getConnection();
			itemLikePage = DiscoverV2DB.getItemLikePage(dbConn, companyId, itemId, start, length);
		} catch (SQLException e) {
			throw new RuntimeException("db failed", e);
		} finally {
			DBUtil.closeQuietly(dbConn);
		}

		return Futures.immediateFuture(GetItemLikeListResponse.newBuilder()
				.addAllItemLike(itemLikePage.dataList())
				.setTotalSize(itemLikePage.totalSize())
				.setFilteredSize(itemLikePage.filteredSize())
				.build());
	}
	
	@Override
	public ListenableFuture<GetItemShareListResponse> getItemShareList(AdminHead head, GetItemShareListRequest request) {
		if (!head.hasCompanyId()) {
			return Futures.immediateFuture(GetItemShareListResponse.newBuilder()
					.setTotalSize(0)
					.setFilteredSize(0)
					.build());
		}
		final long companyId = head.getCompanyId();
		final long itemId = request.getItemId();
		final int start = request.getStart() < 0 ? 0 : request.getStart();
		final int length = request.getLength() < 0 ? 0 : request.getLength();

		DataPage<DiscoverV2Protos.ItemShare> itemSharePage;
		Connection dbConn = null;
		try {
			dbConn = hikariDataSource.getConnection();
			itemSharePage = DiscoverV2DB.getItemSharePage(dbConn, companyId, itemId, start, length);
		} catch (SQLException e) {
			throw new RuntimeException("db failed", e);
		} finally {
			DBUtil.closeQuietly(dbConn);
		}

		return Futures.immediateFuture(GetItemShareListResponse.newBuilder()
				.addAllItemShare(itemSharePage.dataList())
				.setTotalSize(itemSharePage.totalSize())
				.setFilteredSize(itemSharePage.filteredSize())
				.build());
	}
}
