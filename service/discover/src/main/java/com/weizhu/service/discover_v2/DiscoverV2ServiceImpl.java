package com.weizhu.service.discover_v2;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.Executor;

import javax.annotation.Nullable;

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
import com.weizhu.common.service.ServiceUtil;
import com.weizhu.proto.AllowProtos;
import com.weizhu.proto.AllowService;
import com.weizhu.proto.DiscoverV2Protos;
import com.weizhu.proto.DiscoverV2Protos.CommentItemRequest;
import com.weizhu.proto.DiscoverV2Protos.CommentItemResponse;
import com.weizhu.proto.DiscoverV2Protos.DeleteCommentRequest;
import com.weizhu.proto.DiscoverV2Protos.DeleteCommentResponse;
import com.weizhu.proto.DiscoverV2Protos.GetDiscoverHomeRequest;
import com.weizhu.proto.DiscoverV2Protos.GetDiscoverHomeResponse;
import com.weizhu.proto.DiscoverV2Protos.GetItemByIdRequest;
import com.weizhu.proto.DiscoverV2Protos.GetItemByIdResponse;
import com.weizhu.proto.DiscoverV2Protos.GetItemCommentListRequest;
import com.weizhu.proto.DiscoverV2Protos.GetItemCommentListResponse;
import com.weizhu.proto.DiscoverV2Protos.GetItemLearnListRequest;
import com.weizhu.proto.DiscoverV2Protos.GetItemLearnListResponse;
import com.weizhu.proto.DiscoverV2Protos.GetItemLikeListRequest;
import com.weizhu.proto.DiscoverV2Protos.GetItemLikeListResponse;
import com.weizhu.proto.DiscoverV2Protos.GetItemListRequest;
import com.weizhu.proto.DiscoverV2Protos.GetItemListResponse;
import com.weizhu.proto.DiscoverV2Protos.GetItemScoreListRequest;
import com.weizhu.proto.DiscoverV2Protos.GetItemScoreListResponse;
import com.weizhu.proto.DiscoverV2Protos.GetItemShareListRequest;
import com.weizhu.proto.DiscoverV2Protos.GetItemShareListResponse;
import com.weizhu.proto.DiscoverV2Protos.GetModuleCategoryItemListRequest;
import com.weizhu.proto.DiscoverV2Protos.GetModuleCategoryItemListResponse;
import com.weizhu.proto.DiscoverV2Protos.GetModulePromptIndexRequest;
import com.weizhu.proto.DiscoverV2Protos.GetModulePromptIndexResponse;
import com.weizhu.proto.DiscoverV2Protos.GetUserCommentListRequest;
import com.weizhu.proto.DiscoverV2Protos.GetUserCommentListResponse;
import com.weizhu.proto.DiscoverV2Protos.GetUserDiscoverRequest;
import com.weizhu.proto.DiscoverV2Protos.GetUserDiscoverResponse;
import com.weizhu.proto.DiscoverV2Protos.GetUserLearnListRequest;
import com.weizhu.proto.DiscoverV2Protos.GetUserLearnListResponse;
import com.weizhu.proto.DiscoverV2Protos.GetUserLikeListRequest;
import com.weizhu.proto.DiscoverV2Protos.GetUserLikeListResponse;
import com.weizhu.proto.DiscoverV2Protos.GetUserScoreListRequest;
import com.weizhu.proto.DiscoverV2Protos.GetUserScoreListResponse;
import com.weizhu.proto.DiscoverV2Protos.GetUserShareListRequest;
import com.weizhu.proto.DiscoverV2Protos.GetUserShareListResponse;
import com.weizhu.proto.DiscoverV2Protos.Item.Count;
import com.weizhu.proto.DiscoverV2Protos.LearnItemRequest;
import com.weizhu.proto.DiscoverV2Protos.LearnItemResponse;
import com.weizhu.proto.DiscoverV2Protos.LikeItemRequest;
import com.weizhu.proto.DiscoverV2Protos.LikeItemResponse;
import com.weizhu.proto.DiscoverV2Protos.ReportLearnItemRequest;
import com.weizhu.proto.DiscoverV2Protos.ScoreItemRequest;
import com.weizhu.proto.DiscoverV2Protos.ScoreItemResponse;
import com.weizhu.proto.DiscoverV2Protos.SearchItemRequest;
import com.weizhu.proto.DiscoverV2Protos.SearchItemResponse;
import com.weizhu.proto.DiscoverV2Protos.ShareItemRequest;
import com.weizhu.proto.DiscoverV2Protos.ShareItemResponse;
import com.weizhu.proto.DiscoverV2Service;
import com.weizhu.proto.ExamProtos.GetOpenExamCountResponse;
import com.weizhu.proto.ExamService;
import com.weizhu.proto.OfflineTrainingProtos.GetOpenTrainCountResponse;
import com.weizhu.proto.OfflineTrainingService;
import com.weizhu.proto.SurveyProtos.GetOpenSurveyCountResponse;
import com.weizhu.proto.SurveyService;
import com.weizhu.proto.WeizhuProtos.EmptyResponse;
import com.weizhu.proto.WeizhuProtos.RequestHead;
import com.weizhu.proto.WeizhuProtos.SystemHead;
import com.zaxxer.hikari.HikariDataSource;

public class DiscoverV2ServiceImpl implements DiscoverV2Service {

	private static final Logger logger = LoggerFactory.getLogger(DiscoverV2ServiceImpl.class);
	
	private static final ImmutableList<DiscoverV2Protos.State> USER_STATE_LIST = ImmutableList.of(DiscoverV2Protos.State.NORMAL);
	
	private final HikariDataSource hikariDataSource;
	private final JedisPool jedisPool;
	private final Executor serviceExecutor;
	
	private final DiscoverV2Config config;
	private final AllowService allowService;
	private final ExamService examService;
	private final SurveyService surveyService;
	private final OfflineTrainingService offlineTrainingService;
	
	@Inject
	public DiscoverV2ServiceImpl(
			HikariDataSource hikariDataSource, JedisPool jedisPool,
			@Named("service_executor") Executor serviceExecutor,
			DiscoverV2Config config, 
			AllowService allowService,
			ExamService examService,
			SurveyService surveyService,
			OfflineTrainingService offlineTrainingService
			) {
		this.hikariDataSource = hikariDataSource;
		this.jedisPool = jedisPool;
		this.serviceExecutor = serviceExecutor;
		
		this.config = config;
		this.allowService = allowService;
		this.examService = examService;
		this.surveyService = surveyService;
		this.offlineTrainingService = offlineTrainingService;
	}
	
	private DiscoverV2DAOProtos.DiscoverHome doGetDiscoverHome(long companyId) {
		DiscoverV2DAOProtos.DiscoverHome discoverHome = null;
		
		Jedis jedis = this.jedisPool.getResource();
		try {
			discoverHome = DiscoverV2Cache.getDiscoverHome(jedis, Collections.singleton(companyId)).get(companyId);
		} finally {
			jedis.close();
		}

		if (discoverHome == null) {
			Connection dbConn = null;
			try {
				dbConn = this.hikariDataSource.getConnection();
				discoverHome = DiscoverV2DB.getDiscoverHome(dbConn, companyId, USER_STATE_LIST);
			} catch (SQLException e) {
				throw new RuntimeException("db failed", e);
			} finally {
				DBUtil.closeQuietly(dbConn);
			}
			if (discoverHome != null) {
				jedis = this.jedisPool.getResource();
				try {
					DiscoverV2Cache.setDiscoverHome(jedis, Collections.singletonMap(companyId, discoverHome));
				} finally {
					jedis.close();
				}
			}
		}
		
		return discoverHome;
	}
	
	private static final int MODULE_CATEGORY_ITEM_LIST_MAX_CACHE_SIZE = 100;
	
	private Map<Integer, DiscoverV2DAOProtos.ModuleCategoryItemList> doGetModuleCategoryItemList(long companyId, Collection<Integer> categoryIds) {
		if (categoryIds.isEmpty()) {
			return Collections.emptyMap();
		}
		
		Map<Integer, DiscoverV2DAOProtos.ModuleCategoryItemList> resultMap = new TreeMap<Integer, DiscoverV2DAOProtos.ModuleCategoryItemList>();
		
		Set<Integer> noCacheCategoryIdSet = new TreeSet<Integer>();
		Jedis jedis = jedisPool.getResource();
		try {
			resultMap.putAll(DiscoverV2Cache.getModuleCategoryItemList(jedis, companyId, categoryIds, noCacheCategoryIdSet));
		} finally {
			jedis.close();
		}
		
		if (!noCacheCategoryIdSet.isEmpty()) {
			
			Map<Integer, DiscoverV2DAOProtos.ModuleCategoryItemList> noCacheMap;
			Connection dbConn = null;
			try {
				dbConn = hikariDataSource.getConnection();
				noCacheMap = DiscoverV2DB.getModuleCategoryItemListOrderByCreateTime(dbConn, companyId, noCacheCategoryIdSet, USER_STATE_LIST, MODULE_CATEGORY_ITEM_LIST_MAX_CACHE_SIZE);
			} catch (SQLException e) {
				throw new RuntimeException("db failed", e);
			} finally {
				DBUtil.closeQuietly(dbConn);
			}
			
			jedis = jedisPool.getResource();
			try {
				DiscoverV2Cache.setModuleCategoryItemList(jedis, companyId, noCacheCategoryIdSet, noCacheMap);
			} finally {
				jedis.close();
			}
			
			resultMap.putAll(noCacheMap);
		}
		
		return resultMap;
	}
	
	private static final Comparator<DiscoverV2DAOProtos.ModuleCategoryItem> MODULE_CATEGORY_ITEM_CMP = 
			new Comparator<DiscoverV2DAOProtos.ModuleCategoryItem>() {

				@Override
				public int compare(DiscoverV2DAOProtos.ModuleCategoryItem o1, DiscoverV2DAOProtos.ModuleCategoryItem o2) {
					int cmp = 0;
					
					cmp = Ints.compare(o1.getCreateTime(), o2.getCreateTime());
					if (cmp != 0) {
						return -cmp;
					}
					
					cmp = Longs.compare(o1.getItemId(), o2.getItemId());
					if (cmp != 0) {
						return -cmp;
					}
					
					return 0;
				}
		
	};
	
	@Override
	public ListenableFuture<GetDiscoverHomeResponse> getDiscoverHome(RequestHead head, GetDiscoverHomeRequest request) {
		
		final long companyId = head.getSession().getCompanyId();
		// 1. 解析请求里的 提示索引
		final Map<Integer, Long> modulePromptDotMap = // 模块红点提示
				new TreeMap<Integer, Long>(); 
		final Map<Integer, DiscoverV2DAOProtos.ModuleCategoryItem> categoryPromptIdxMap = // 模块分类列表数字提示
				new TreeMap<Integer, DiscoverV2DAOProtos.ModuleCategoryItem>();
		
		for (ByteString data : request.getPromptIndexList()) {
			if (!data.isEmpty()) {
				try {
					DiscoverV2DAOProtos.ModulePromptIndex promptIdx = DiscoverV2DAOProtos.ModulePromptIndex.parseFrom(data);
					switch (promptIdx.getTypeCase()) {
						case PROMPT_DOT:
							modulePromptDotMap.put(promptIdx.getPromptDot().getModuleId(), promptIdx.getPromptDot().getPromptDotTimestamp());
							break;
						case CATEGORY_ITEM:
							categoryPromptIdxMap.put(promptIdx.getCategoryItem().getCategoryId(), promptIdx.getCategoryItem());
							break;
						case TYPE_NOT_SET:
							break;
						default:
							break;
					}
				} catch (InvalidProtocolBufferException e) {
					// ignore
				}
			}
		}
		
		// 2. 获取home基础信息
		final DiscoverV2DAOProtos.DiscoverHome discoverHome = this.doGetDiscoverHome(companyId);
		
		// 3. 获取banner引用的item
		Set<Long> refItemIdSet = new TreeSet<Long>();
		for (DiscoverV2Protos.Banner banner : discoverHome.getBannerList()) {
			if (isValid(banner, null) && banner.getContentCase() == DiscoverV2Protos.Banner.ContentCase.ITEM_ID) {
				refItemIdSet.add(banner.getItemId());
			}
		}
		final Map<Long, DiscoverV2Protos.Item> refItemMap = DiscoverUtil.getItem(hikariDataSource, jedisPool, companyId, refItemIdSet, USER_STATE_LIST, head.getSession().getUserId());
		
		// 4. 获取模块分类提示数字所需的条目索引列表
		Set<Integer> categoryIdSet = new TreeSet<Integer>();
		for (DiscoverV2Protos.Module module : discoverHome.getModuleList()) {
			if (isValid(module, null) && module.getContentCase() == DiscoverV2Protos.Module.ContentCase.CATEGORY_LIST) {
				for (DiscoverV2Protos.Module.Category category : module.getCategoryList().getCategoryList()) {
					if (isValid(category, null)) {
						categoryIdSet.add(category.getCategoryId());
					}
				}
			}
		}
		final Map<Integer, DiscoverV2DAOProtos.ModuleCategoryItemList> categoryItemListMap = this.doGetModuleCategoryItemList(companyId, categoryIdSet);
		
		boolean hasModuleExam = false;
		boolean hasModuleSurvey = false;
		boolean hasModuleOfflineTraining = false;
		for (DiscoverV2Protos.Module module : discoverHome.getModuleList()) {
			if (isValid(module, null) 
					&& module.getContentCase() == DiscoverV2Protos.Module.ContentCase.WEB_URL
					&& module.getWebUrl().getIsWeizhu()
					) {
				if (!config.webappMobileUrlPrefix().isEmpty()) {
					if (module.getWebUrl().getWebUrl().equals(config.webappMobileUrlPrefix() + "exam/exam_list.html")) {
						hasModuleExam = true;
					}
					if (module.getWebUrl().getWebUrl().equals(config.webappMobileUrlPrefix() + "survey/survey_list.html")) {
						hasModuleSurvey = true;
					}
					if (module.getWebUrl().getWebUrl().equals(config.webappMobileUrlPrefix() + "offline_training/training_list.html")) {
						hasModuleOfflineTraining = true;
					}
				}
			}
		}
		
		final ListenableFuture<GetOpenExamCountResponse> getOpenExamCountFuture = hasModuleExam ? this.examService.getOpenExamCount(head, ServiceUtil.EMPTY_REQUEST) : null;
		final ListenableFuture<GetOpenSurveyCountResponse> getOpenSurveyCountFuture = hasModuleSurvey ? this.surveyService.getOpenSurveyCount(head, ServiceUtil.EMPTY_REQUEST) : null;
		final ListenableFuture<GetOpenTrainCountResponse> getOpenTrainCountFuture = hasModuleOfflineTraining ? this.offlineTrainingService.getOpenTrainCount(head, ServiceUtil.EMPTY_REQUEST) : null;
		
		// 5. 收集所有访问模型id，并检查当前用户可以访问的模型Id
		Set<Integer> modelIdSet = new TreeSet<Integer>();
		for (DiscoverV2Protos.Banner banner : discoverHome.getBannerList()) {
			if (isValid(banner, null) && banner.hasAllowModelId()) {
				modelIdSet.add(banner.getAllowModelId());
			}
		}
		for (DiscoverV2Protos.Item item : refItemMap.values()) {
			if (isValid(item.getBase(), null) && item.getBase().hasAllowModelId()) {
				modelIdSet.add(item.getBase().getAllowModelId());
			}
		}
		for (DiscoverV2Protos.Module module : discoverHome.getModuleList()) {
			if (isValid(module, null)) {
				if (module.hasAllowModelId()) {
					modelIdSet.add(module.getAllowModelId());
				}
				if (module.getContentCase() == DiscoverV2Protos.Module.ContentCase.CATEGORY_LIST) {
					for (DiscoverV2Protos.Module.Category category : module.getCategoryList().getCategoryList()) {
						if (isValid(category, null) && category.hasAllowModelId()) {
							modelIdSet.add(category.getAllowModelId());
						}
					}
				}
			}
		}
		for (Entry<Integer, DiscoverV2DAOProtos.ModuleCategoryItemList> entry : categoryItemListMap.entrySet()) {
			DiscoverV2DAOProtos.ModuleCategoryItem promptIdx = categoryPromptIdxMap.get(entry.getKey());
			for (DiscoverV2DAOProtos.ModuleCategoryItem moduleCategoryItem : entry.getValue().getModuleCategoryItemList()) {
				if (promptIdx != null && MODULE_CATEGORY_ITEM_CMP.compare(moduleCategoryItem, promptIdx) >= 0) {
					break;
				}
				
				if (isValid(moduleCategoryItem, null) && moduleCategoryItem.hasItemAllowModelId()) {
					modelIdSet.add(moduleCategoryItem.getItemAllowModelId());
				}
			}
		}
		final Set<Integer> allowedModelIdSet = this.doCheckAllowModelId(head, modelIdSet);
		
		/* 开始拼装返回结果 */
		
		GetDiscoverHomeResponse.Builder responseBuilder = GetDiscoverHomeResponse.newBuilder();
		
		// 1. 拼装banner
		for (DiscoverV2Protos.Banner banner : discoverHome.getBannerList()) {
			if (isValid(banner, allowedModelIdSet)) {
				switch (banner.getContentCase()) {
					case ITEM_ID: {
						// 需要保证item存在且可访问
						DiscoverV2Protos.Item item = refItemMap.get(banner.getItemId());
						if (item != null && isValid(item.getBase(), allowedModelIdSet)) {
							responseBuilder.addBanner(banner);
						}
						break;
					}
					case WEB_URL:
					case APP_URI:
					case CONTENT_NOT_SET:
						responseBuilder.addBanner(banner);
						break;
					default:
						break;
				}
			}
		}
		
		// 2. 拼装module
		
		DiscoverV2Protos.Module.Builder tmpModuleBuilder = DiscoverV2Protos.Module.newBuilder();
		DiscoverV2Protos.Module.CategoryList.Builder tmpCategoryListBuilder = DiscoverV2Protos.Module.CategoryList.newBuilder();
		DiscoverV2Protos.Module.Category.Builder tmpCategoryBuilder = DiscoverV2Protos.Module.Category.newBuilder();
		
		for (DiscoverV2Protos.Module module : discoverHome.getModuleList()) {
			if (isValid(module, allowedModelIdSet)) {
				switch (module.getContentCase()) {
					case CATEGORY_LIST: {
						tmpModuleBuilder.clear();
						tmpModuleBuilder.mergeFrom(module).clearPromptCnt().clearPromptDot().clearCategoryList();
						
						tmpCategoryListBuilder.clear();
						int modulePromptCnt = 0;
						for (DiscoverV2Protos.Module.Category category : module.getCategoryList().getCategoryList()) {
							if (isValid(category, allowedModelIdSet)) {
								tmpCategoryBuilder.clear();
								tmpCategoryBuilder.mergeFrom(category).clearPromptCnt().clearPromptDot();
								
								// 计算提醒数字
								int promptCnt = 0;
								
								DiscoverV2DAOProtos.ModuleCategoryItem promptIdx = categoryPromptIdxMap.get(category.getCategoryId());
								DiscoverV2DAOProtos.ModuleCategoryItemList moduleCategoryItemList = categoryItemListMap.get(category.getCategoryId());
								if (moduleCategoryItemList != null) {
									for (DiscoverV2DAOProtos.ModuleCategoryItem moduleCategoryItem : moduleCategoryItemList.getModuleCategoryItemList()) {
										if (promptIdx != null && MODULE_CATEGORY_ITEM_CMP.compare(moduleCategoryItem, promptIdx) >= 0) {
											break;
										}
										
										if (isValid(moduleCategoryItem, allowedModelIdSet)) {
											promptCnt++;
										}
									}
								}
								
								tmpCategoryBuilder.setPromptCnt(promptCnt);
								modulePromptCnt += promptCnt;
								
								tmpCategoryListBuilder.addCategory(tmpCategoryBuilder.build());
							}
						}
						
						// 确保分类数目大于0
						if (tmpCategoryListBuilder.getCategoryCount() > 0) {
							tmpModuleBuilder.setCategoryList(tmpCategoryListBuilder.build());
							tmpModuleBuilder.setPromptCnt(modulePromptCnt);
							
							responseBuilder.addModule(tmpModuleBuilder.build());
						}
						break;	
					}
					case WEB_URL: {
						if (!config.webappMobileUrlPrefix().isEmpty()
								&& module.getWebUrl().getIsWeizhu() 
								&& module.getWebUrl().getWebUrl().equals(config.webappMobileUrlPrefix() + "exam/exam_list.html")
								&& getOpenExamCountFuture != null
								) {
							tmpModuleBuilder.clear();
							tmpModuleBuilder.mergeFrom(module).clearPromptCnt().clearPromptDot();
							try {
								tmpModuleBuilder.setPromptCnt(getOpenExamCountFuture.get().getOpenExamCount());
							} catch (Exception e) {
							}
							responseBuilder.addModule(tmpModuleBuilder.build());
							break;
						}
						
						if (!config.webappMobileUrlPrefix().isEmpty()
								&& module.getWebUrl().getIsWeizhu() 
								&& module.getWebUrl().getWebUrl().equals(config.webappMobileUrlPrefix() + "survey/survey_list.html")
								&& getOpenSurveyCountFuture != null
								) {
							tmpModuleBuilder.clear();
							tmpModuleBuilder.mergeFrom(module).clearPromptCnt().clearPromptDot();
							try {
								tmpModuleBuilder.setPromptCnt(getOpenSurveyCountFuture.get().getOpenSurveyCount());
							} catch (Exception e) {
							}
							responseBuilder.addModule(tmpModuleBuilder.build());
							break;
						}
						
						if (!config.webappMobileUrlPrefix().isEmpty()
								&& module.getWebUrl().getIsWeizhu() 
								&& module.getWebUrl().getWebUrl().equals(config.webappMobileUrlPrefix() + "offline_training/training_list.html")
								&& getOpenTrainCountFuture != null
								) {
							tmpModuleBuilder.clear();
							tmpModuleBuilder.mergeFrom(module).clearPromptCnt().clearPromptDot();
							try {
								tmpModuleBuilder.setPromptCnt(getOpenTrainCountFuture.get().getOpenTrainCount());
							} catch (Exception e) {
							}
							responseBuilder.addModule(tmpModuleBuilder.build());
							break;
						}
					}
					case APP_URI:
						tmpModuleBuilder.clear();
						tmpModuleBuilder.mergeFrom(module).clearPromptCnt().clearPromptDot();
						
						Long promptDotUpdateTimestamp = modulePromptDotMap.get(module.getModuleId());
						for (DiscoverV2DAOProtos.ModulePromptDot promptDot : discoverHome.getModulePromptDotList()) {
							if (promptDot.getModuleId() == module.getModuleId()) {
								tmpModuleBuilder.setPromptDot(
										promptDotUpdateTimestamp == null 
										|| promptDotUpdateTimestamp < promptDot.getPromptDotTimestamp()
										);
								break;
							}
						}
						
						responseBuilder.addModule(tmpModuleBuilder.build());
						break;
					case CONTENT_NOT_SET:
						break;
					default:
						break;
				}
			}
		}
		
		// 3. 拼装引用 item
		for (DiscoverV2Protos.Item item : refItemMap.values()) {
			if (isValid(item.getBase(), allowedModelIdSet)) {
				responseBuilder.addRefItem(item);
			}
		}
		
		return Futures.immediateFuture(responseBuilder.build());
	}
	
	private static final ListenableFuture<GetModuleCategoryItemListResponse> GET_MODULE_CATEGORY_ITEM_LIST_EMPTY_RESPONSE_FUTURE =
			Futures.immediateFuture(GetModuleCategoryItemListResponse.newBuilder()
					.setHasMore(false)
					.setOffsetIndex(ByteString.EMPTY)
					.build());

	@Override
	public ListenableFuture<GetModuleCategoryItemListResponse> getModuleCategoryItemList(RequestHead head, GetModuleCategoryItemListRequest request) {
		// 1. 解析请求参数
		final int moduleId = request.getModuleId();
		final int categoryId = request.getCategoryId();
		final int itemSize = request.getItemSize() < 0 ? 0 : request.getItemSize() > 50 ? 50 : request.getItemSize();
		final long companyId = head.getSession().getCompanyId();
	
		final DiscoverV2DAOProtos.ModuleCategoryItem offsetIdx;
		if (request.hasOffsetIndex() && !request.getOffsetIndex().isEmpty()) {
			DiscoverV2DAOProtos.ModuleCategoryItem tmp = null;
			try {
				tmp = DiscoverV2DAOProtos.ModuleCategoryItem.parseFrom(request.getOffsetIndex());
			} catch (InvalidProtocolBufferException e) {
				tmp = null;
			}
			if (tmp != null && tmp.getCategoryId() == request.getCategoryId()) {
				offsetIdx = tmp;
			} else {
				offsetIdx = null;
			}
		} else {
			offsetIdx = null;
		}
		
		// 2. 获取对应的模块和分类
		DiscoverV2Protos.Module module = null;
		DiscoverV2Protos.Module.Category category = null;
		
		DiscoverV2DAOProtos.DiscoverHome discoverHome = this.doGetDiscoverHome(companyId);
		for (DiscoverV2Protos.Module m : discoverHome.getModuleList()) {
			if (m.getModuleId() == moduleId) {
				module = m;
				break;
			}
		}
		if (module != null && module.getContentCase() == DiscoverV2Protos.Module.ContentCase.CATEGORY_LIST) {
			for (DiscoverV2Protos.Module.Category c : module.getCategoryList().getCategoryList()) {
				if (c.getCategoryId() == categoryId) {
					category = c;
					break;
				}
			}
		}
		
		if (module == null || !isValid(module, null) || category == null || !isValid(category, null)) {
			// fail fast: 模块和分类不正确，立即返回空数据
			return GET_MODULE_CATEGORY_ITEM_LIST_EMPTY_RESPONSE_FUTURE;
		}
		
		// 3. 获取列表索引缓存
		
		DiscoverV2DAOProtos.ModuleCategoryItemList moduleCategoryItemList = 
				this.doGetModuleCategoryItemList(companyId, Collections.singleton(categoryId)).get(categoryId);
		if (moduleCategoryItemList == null) {
			return GET_MODULE_CATEGORY_ITEM_LIST_EMPTY_RESPONSE_FUTURE;
		}
		
		// 多获取一条用来判断 hasMore
		List<DiscoverV2DAOProtos.ModuleCategoryItem> list = new ArrayList<DiscoverV2DAOProtos.ModuleCategoryItem>(itemSize + 1);
		
		for (DiscoverV2DAOProtos.ModuleCategoryItem moduleCategoryItem : moduleCategoryItemList.getModuleCategoryItemList()) {
			if (offsetIdx != null && MODULE_CATEGORY_ITEM_CMP.compare(moduleCategoryItem, offsetIdx) <= 0) {
				continue;
			}

			if (isValid(moduleCategoryItem, null)) {
				list.add(moduleCategoryItem);
				
				if (list.size() > itemSize) {
					break;
				}
			}
		}
		
		if (list.size() < itemSize + 1 && moduleCategoryItemList.getModuleCategoryItemCount() >= MODULE_CATEGORY_ITEM_LIST_MAX_CACHE_SIZE) {
			// 缓存中的数据不够，且缓存之外还有数据 则直接访问db
			Connection dbConn = null;
			try {
				dbConn = hikariDataSource.getConnection();
				list = DiscoverV2DB.getModuleCategoryItemListOrderByCreateTime(dbConn, companyId, categoryId, USER_STATE_LIST, offsetIdx, itemSize + 1).getModuleCategoryItemList(); 
			} catch (SQLException e) {
				throw new RuntimeException("db failed", e);
			} finally {
				DBUtil.closeQuietly(dbConn);
			}
		}
		
		final boolean hasMore;
		if (list.size() > itemSize) {
			hasMore = true;
			list = list.subList(0, itemSize);
		} else {
			hasMore = false;
		}
		
		// 4. 获取列表对应的条目详细信息
		
		Set<Long> itemIdSet = new TreeSet<Long>();
		for (DiscoverV2DAOProtos.ModuleCategoryItem moduleCategoryItem : list) {
			itemIdSet.add(moduleCategoryItem.getItemId());
		}
		
		final Map<Long, DiscoverV2Protos.Item> itemMap = DiscoverUtil.getItem(hikariDataSource, jedisPool, companyId, itemIdSet, USER_STATE_LIST, head.getSession().getUserId());
		
		// 5. 检查各资源是否可访问
		
		Set<Integer> modelIdSet = new TreeSet<Integer>();
		if (module.hasAllowModelId()) {
			modelIdSet.add(module.getAllowModelId());
		}
		if (category.hasAllowModelId()) {
			modelIdSet.add(category.getAllowModelId());
		}
		for (DiscoverV2Protos.Item item : itemMap.values()) {
			if (item.getBase().hasAllowModelId()) {
				modelIdSet.add(item.getBase().getAllowModelId());
			}
		}
		final Set<Integer> allowedModelIdSet = this.doCheckAllowModelId(head, modelIdSet);
		
		if (!isValid(module, allowedModelIdSet) || !isValid(category, allowedModelIdSet)) {
			// fail fast: 模块和分类不正确，立即返回空数据
			return GET_MODULE_CATEGORY_ITEM_LIST_EMPTY_RESPONSE_FUTURE;
		}
		
		/* 开始拼装返回数据 */
		
		GetModuleCategoryItemListResponse.Builder responseBuilder = GetModuleCategoryItemListResponse.newBuilder();
		
		for (DiscoverV2DAOProtos.ModuleCategoryItem moduleCategoryItem : list) {
			DiscoverV2Protos.Item item = itemMap.get(moduleCategoryItem.getItemId());
			if (item != null && isValid(item.getBase(), allowedModelIdSet)) {
				responseBuilder.addItem(item);
			}
		}
		responseBuilder.setHasMore(hasMore);
		
		// 设置翻页索引
		if (list.isEmpty()) {
			// 获取到的索引列表为空，设置为传入参数
			responseBuilder.setOffsetIndex(request.hasOffsetIndex() ? request.getOffsetIndex() : ByteString.EMPTY);
		} else {
			responseBuilder.setOffsetIndex(list.get(list.size() - 1).toByteString());
		}
		
		// 设置客户端数字提示索引
		if (offsetIdx == null && !list.isEmpty()) {
			responseBuilder.setPromptIndex(DiscoverV2DAOProtos.ModulePromptIndex.newBuilder()
					.setCategoryItem(list.get(0))
					.build().toByteString());
		}
		
		return Futures.immediateFuture(responseBuilder.build());
	}

	@Override
	public ListenableFuture<GetModulePromptIndexResponse> getModulePromptIndex(RequestHead head, GetModulePromptIndexRequest request) {
		final int moduleId = request.getModuleId();
		final long companyId = head.getSession().getCompanyId();
		
		DiscoverV2Protos.Module module = null;
		DiscoverV2DAOProtos.ModulePromptDot modulePromptDot = null;
		
		DiscoverV2DAOProtos.DiscoverHome discoverHome = this.doGetDiscoverHome(companyId);
		for (DiscoverV2Protos.Module m : discoverHome.getModuleList()) {
			if (m.getModuleId() == moduleId) {
				module = m;
				break;
			}
		}
		for (DiscoverV2DAOProtos.ModulePromptDot p : discoverHome.getModulePromptDotList()) {
			if (p.getModuleId() == moduleId) {
				modulePromptDot = p;
				break;
			}
		}
		
		if (module != null && isValid(module, null) && modulePromptDot != null) {
			return Futures.immediateFuture(GetModulePromptIndexResponse.newBuilder()
					.setPromptIndex(DiscoverV2DAOProtos.ModulePromptIndex.newBuilder()
							.setPromptDot(modulePromptDot)
							.build().toByteString())
					.build());
		} else {
			return Futures.immediateFuture(GetModulePromptIndexResponse.newBuilder().build());
		}
	}

	@Override
	public ListenableFuture<GetItemByIdResponse> getItemById(RequestHead head, GetItemByIdRequest request) {
		final long companyId = head.getSession().getCompanyId();
		if (request.getItemIdCount() <= 0) {
			return Futures.immediateFuture(GetItemByIdResponse.newBuilder().build());
		}
		
		Map<Long, DiscoverV2Protos.Item> itemMap = 
				DiscoverUtil.getItem(hikariDataSource, jedisPool, companyId,
						new TreeSet<Long>(request.getItemIdList()), 
						USER_STATE_LIST, 
						head.getSession().getUserId()
						);
		
		Set<Integer> modelIdSet = new TreeSet<Integer>();
		for (DiscoverV2Protos.Item item : itemMap.values()) {
			if (item.getBase().hasAllowModelId()) {
				modelIdSet.add(item.getBase().getAllowModelId());
			}
		}
		final Set<Integer> allowedModelIdSet = this.doCheckAllowModelId(head, modelIdSet);
		
		GetItemByIdResponse.Builder responseBuilder = GetItemByIdResponse.newBuilder();
		
		for (DiscoverV2Protos.Item item : itemMap.values()) {
			if (isValid(item.getBase(), allowedModelIdSet)) {
				responseBuilder.addItem(item);
			}
		}
		
		return Futures.immediateFuture(responseBuilder.build());
	}
	
	@Override
	public ListenableFuture<GetItemByIdResponse> getItemById(SystemHead head, GetItemByIdRequest request) {
		final long companyId = head.getCompanyId();

		if (request.getItemIdCount() <= 0) {
			return Futures.immediateFuture(GetItemByIdResponse.newBuilder().build());
		}
		
		Map<Long, DiscoverV2Protos.Item> itemMap = 
				DiscoverUtil.getItem(hikariDataSource, jedisPool, companyId,
						new TreeSet<Long>(request.getItemIdList()), 
						USER_STATE_LIST, 
						null
						);
		
		GetItemByIdResponse.Builder responseBuilder = GetItemByIdResponse.newBuilder();
		
		for (DiscoverV2Protos.Item item : itemMap.values()) {
			if (isValid(item.getBase(), null)) {
				responseBuilder.addItem(item);
			}
		}
		
		return Futures.immediateFuture(responseBuilder.build());
	}
	
	private Map<Long, DiscoverV2Protos.Item.Base> doGetValidItemBase(RequestHead head, Collection<Long> itemIds) {
		final long companyId = head.getSession().getCompanyId();

		if (itemIds.isEmpty()) {
			return Collections.emptyMap();
		}
		
		Map<Long, DiscoverV2Protos.Item.Base> itemBaseMap = DiscoverUtil.getItemBase(
				hikariDataSource, jedisPool, companyId, 
				itemIds, 
				USER_STATE_LIST
				);
		
		if (itemBaseMap.isEmpty()) {
			return Collections.emptyMap();
		}
		
		Set<Integer> modelIdSet = new TreeSet<Integer>();
		for (DiscoverV2Protos.Item.Base itemBase : itemBaseMap.values()) {
			if (itemBase.hasAllowModelId()) {
				modelIdSet.add(itemBase.getAllowModelId());
			}
		}
		
		if (modelIdSet.isEmpty()) {
			return itemBaseMap;
		}
		
		final Set<Integer> allowedModelIdSet = this.doCheckAllowModelId(head, modelIdSet);
		if (allowedModelIdSet.containsAll(modelIdSet)) {
			return itemBaseMap;
		}
		
		Map<Long, DiscoverV2Protos.Item.Base> resultMap = new TreeMap<Long, DiscoverV2Protos.Item.Base>();
		for (DiscoverV2Protos.Item.Base itemBase : itemBaseMap.values()) {
			if (isValid(itemBase, allowedModelIdSet)) {
				resultMap.put(itemBase.getItemId(), itemBase);
			}
		}
		return resultMap;
	}
	
	private Map<Long, DiscoverV2Protos.Item> doGetValidItem(RequestHead head, Set<Long> itemIdSet, boolean hasUser) {
		final long companyId = head.getSession().getCompanyId();

		if (itemIdSet.isEmpty()) {
			return Collections.emptyMap();
		}
		
		Map<Long, DiscoverV2Protos.Item> itemMap = 
				DiscoverUtil.getItem(hikariDataSource, jedisPool, companyId,
						itemIdSet, 
						USER_STATE_LIST, 
						hasUser ? head.getSession().getUserId() : null
						);
		
		if (itemMap.isEmpty()) {
			return Collections.emptyMap();
		}
		
		Set<Integer> modelIdSet = new TreeSet<Integer>();
		for (DiscoverV2Protos.Item item : itemMap.values()) {
			if (item.getBase().hasAllowModelId()) {
				modelIdSet.add(item.getBase().getAllowModelId());
			}
		}
		
		if (modelIdSet.isEmpty()) {
			return itemMap;
		}
		
		final Set<Integer> allowedModelIdSet = this.doCheckAllowModelId(head, modelIdSet);
		if (allowedModelIdSet.containsAll(modelIdSet)) {
			return itemMap;
		}
		
		Map<Long, DiscoverV2Protos.Item> resultMap = new TreeMap<Long, DiscoverV2Protos.Item>();
		for (DiscoverV2Protos.Item item : itemMap.values()) {
			if (isValid(item.getBase(), allowedModelIdSet)) {
				resultMap.put(item.getBase().getItemId(), item);
			}
		}
		return resultMap;
	}
	
	private static final int ITEM_LEARN_LIST_MAX_CACHE_SIZE = 100;
	
	private Map<Long, DiscoverV2DAOProtos.ItemLearnList> doGetItemLearnList(long companyId, Collection<Long> itemIds) {
		if (itemIds.isEmpty()) {
			return Collections.emptyMap();
		}
		
		Map<Long, DiscoverV2DAOProtos.ItemLearnList> resultMap = new TreeMap<Long, DiscoverV2DAOProtos.ItemLearnList>();
		
		Set<Long> noCacheItemIdSet = new TreeSet<Long>();
		Jedis jedis = jedisPool.getResource();
		try {
			resultMap.putAll(DiscoverV2Cache.getItemLearnList(jedis, companyId, itemIds, noCacheItemIdSet));
		} finally {
			jedis.close();
		}
		
		if (!noCacheItemIdSet.isEmpty()) {
			
			Map<Long, DiscoverV2DAOProtos.ItemLearnList> noCacheMap;
			Connection dbConn = null;
			try {
				dbConn = hikariDataSource.getConnection();
				noCacheMap = DiscoverV2DB.getItemLearnList(dbConn, companyId, noCacheItemIdSet, ITEM_LEARN_LIST_MAX_CACHE_SIZE);
			} catch (SQLException e) {
				throw new RuntimeException("db failed", e);
			} finally {
				DBUtil.closeQuietly(dbConn);
			}
			
			jedis = jedisPool.getResource();
			try {
				DiscoverV2Cache.setItemLearnList(jedis, companyId, noCacheItemIdSet, noCacheMap);
			} finally {
				jedis.close();
			}
			
			resultMap.putAll(noCacheMap);
		}
		
		return resultMap;
	}
	
	private static final Comparator<DiscoverV2Protos.ItemLearn> ITEM_LEARN_CMP = 
			new Comparator<DiscoverV2Protos.ItemLearn>() {

				@Override
				public int compare(DiscoverV2Protos.ItemLearn o1, DiscoverV2Protos.ItemLearn o2) {
					int cmp = 0;
					
					cmp = Ints.compare(o1.getLearnTime(), o2.getLearnTime());
					if (cmp != 0) {
						return -cmp;
					}
					
					cmp = Longs.compare(o1.getItemId(), o2.getItemId());
					if (cmp != 0) {
						return -cmp;
					}
					
					cmp = Longs.compare(o1.getUserId(), o2.getUserId());
					if (cmp != 0) {
						return -cmp;
					}
					
					return 0;
				}
		
	};

	@Override
	public ListenableFuture<GetItemLearnListResponse> getItemLearnList(RequestHead head, GetItemLearnListRequest request) {
		
		final long companyId = head.getSession().getCompanyId();
		final long itemId = request.getItemId();
		final int size = request.getSize() < 0 ? 0 : request.getSize() > 50 ? 50 : request.getSize();
		final DiscoverV2Protos.ItemLearn offsetIdx;
		
		if (request.hasOffsetIndex() && !request.getOffsetIndex().isEmpty()) {
			DiscoverV2Protos.ItemLearn tmp = null;
			try {
				tmp = DiscoverV2Protos.ItemLearn.parseFrom(request.getOffsetIndex());
			} catch (InvalidProtocolBufferException e) {
				tmp = null;
			}
			
			if (tmp != null && tmp.getItemId() == itemId) {
				offsetIdx = tmp;
			} else {
				offsetIdx = null;
			}
		} else {
			offsetIdx = null;
		}
		
		DiscoverV2Protos.Item item = this.doGetValidItem(head, Collections.singleton(itemId), false).get(itemId);
		if (item == null) {
			return Futures.immediateFuture(GetItemLearnListResponse.newBuilder()
					.setHasMore(false)
					.setOffsetIndex(ByteString.EMPTY)
					.build());
		}
		
		DiscoverV2DAOProtos.ItemLearnList itemLearnList = this.doGetItemLearnList(companyId, Collections.singleton(itemId)).get(itemId);
		if (itemLearnList == null) {
			return Futures.immediateFuture(GetItemLearnListResponse.newBuilder()
					.setHasMore(false)
					.setOffsetIndex(ByteString.EMPTY)
					.build());
		}
		
		List<DiscoverV2Protos.ItemLearn> list = new ArrayList<DiscoverV2Protos.ItemLearn>(size + 1);
		
		for (DiscoverV2Protos.ItemLearn itemLearn : itemLearnList.getItemLearnList()) {
			if (offsetIdx != null && ITEM_LEARN_CMP.compare(itemLearn, offsetIdx) <= 0) {
				continue;
			}
			
			list.add(itemLearn);
			if (list.size() > size) {
				break;
			}
		}
		
		if (list.size() < size + 1 && itemLearnList.getItemLearnCount() >= ITEM_LEARN_LIST_MAX_CACHE_SIZE) {
			Connection dbConn = null;
			try {
				dbConn = hikariDataSource.getConnection();
				list = DiscoverV2DB.getItemLearnList(dbConn, companyId, itemId, offsetIdx, size + 1).getItemLearnList();
			} catch (SQLException e) {
				throw new RuntimeException("db failed", e);
			} finally {
				DBUtil.closeQuietly(dbConn);
			}
		}
		
		final boolean hasMore;
		if (list.size() > size) {
			hasMore = true;
			list = list.subList(0, size);
		} else {
			hasMore = false;
		}
		
		DiscoverV2Protos.ItemLearn userItemLearn;
		Connection dbConn = null;
		try {
			dbConn = hikariDataSource.getConnection();
			userItemLearn = DiscoverV2DB.getUserItemLearn(dbConn, companyId, head.getSession().getUserId(), itemId);
		} catch (SQLException e) {
			throw new RuntimeException("db failed", e);
		} finally {
			DBUtil.closeQuietly(dbConn);
		}
		
		GetItemLearnListResponse.Builder responseBuilder = GetItemLearnListResponse.newBuilder();
		responseBuilder.addAllItemLearn(list);
		responseBuilder.setHasMore(hasMore);
		if (list.isEmpty()) {
			responseBuilder.setOffsetIndex(request.hasOffsetIndex() ? request.getOffsetIndex() : ByteString.EMPTY);
		} else {
			responseBuilder.setOffsetIndex(list.get(list.size() - 1).toByteString());
		}
		
		responseBuilder.setItemLearnCnt(item.getCount().getLearnCnt());
		responseBuilder.setItemLearnUserCnt(item.getCount().getLearnUserCnt());
		if (userItemLearn != null) {
			responseBuilder.setUserItemLearn(userItemLearn);
		}
		
		return Futures.immediateFuture(responseBuilder.build());
	}

	@Override
	public ListenableFuture<GetUserLearnListResponse> getUserLearnList(RequestHead head, GetUserLearnListRequest request) {
		final long companyId = head.getSession().getCompanyId();
		final long userId = request.getUserId();
		final int size = request.getSize() < 0 ? 0 : request.getSize() > 50 ? 50 : request.getSize();
		final DiscoverV2Protos.ItemLearn offsetIdx;
		
		if (request.hasOffsetIndex() && !request.getOffsetIndex().isEmpty()) {
			DiscoverV2Protos.ItemLearn tmp = null;
			try {
				tmp = DiscoverV2Protos.ItemLearn.parseFrom(request.getOffsetIndex());
			} catch (InvalidProtocolBufferException e) {
				tmp = null;
			}
			if (tmp != null && tmp.getUserId() == userId) {
				offsetIdx = tmp;
			} else {
				offsetIdx = null;
			}
		} else {
			offsetIdx = null;
		}
		
		List<DiscoverV2Protos.ItemLearn> list;
		Connection dbConn = null;
		try {
			dbConn = hikariDataSource.getConnection();
			list = DiscoverV2DB.getUserLearnList(dbConn, companyId, userId, offsetIdx, size + 1).getItemLearnList();
		} catch (SQLException e) {
			throw new RuntimeException("db failed", e);
		} finally {
			DBUtil.closeQuietly(dbConn);
		}
		
		final boolean hasMore;
		if (list.size() > size) {
			hasMore = true;
			list = list.subList(0, size);
		} else {
			hasMore = false;
		}
		
		Set<Long> refItemIdSet = new TreeSet<Long>();
		for (DiscoverV2Protos.ItemLearn itemLearn : list) {
			refItemIdSet.add(itemLearn.getItemId());
		}
		
		Map<Long, DiscoverV2Protos.Item> refValidItemMap = this.doGetValidItem(head, refItemIdSet, true);
		
		GetUserLearnListResponse.Builder responseBuilder = GetUserLearnListResponse.newBuilder();
		
		for (DiscoverV2Protos.ItemLearn itemLearn : list) {
			DiscoverV2Protos.Item validItem = refValidItemMap.get(itemLearn.getItemId());
			if (validItem != null) {
				responseBuilder.addItemLearn(itemLearn);
			}
		}
		responseBuilder.setHasMore(hasMore);
		if (list.isEmpty()) {
			responseBuilder.setOffsetIndex(request.hasOffsetIndex() ? request.getOffsetIndex() : ByteString.EMPTY);
		} else {
			responseBuilder.setOffsetIndex(list.get(list.size() - 1).toByteString());
		}
		responseBuilder.addAllRefItem(refValidItemMap.values());
		
		return Futures.immediateFuture(responseBuilder.build());
	}
	
	private static final int ITEM_COMMENT_LIST_MAX_CACHE_SIZE = 100;
	
	private Map<Long, DiscoverV2DAOProtos.ItemCommentList> doGetItemCommentList(long companyId, Collection<Long> itemIds) {
		if (itemIds.isEmpty()) {
			return Collections.emptyMap();
		}
		
		Map<Long, DiscoverV2DAOProtos.ItemCommentList> resultMap = new TreeMap<Long, DiscoverV2DAOProtos.ItemCommentList>();
		
		Set<Long> noCacheItemIdSet = new TreeSet<Long>();
		Jedis jedis = jedisPool.getResource();
		try {
			resultMap.putAll(DiscoverV2Cache.getItemCommentList(jedis, companyId, itemIds, noCacheItemIdSet));
		} finally {
			jedis.close();
		}
		
		if (!noCacheItemIdSet.isEmpty()) {
			
			Map<Long, DiscoverV2DAOProtos.ItemCommentList> noCacheMap;
			Connection dbConn = null;
			try {
				dbConn = hikariDataSource.getConnection();
				noCacheMap = DiscoverV2DB.getItemCommentList(dbConn, companyId, noCacheItemIdSet, false, ITEM_COMMENT_LIST_MAX_CACHE_SIZE);
			} catch (SQLException e) {
				throw new RuntimeException("db failed", e);
			} finally {
				DBUtil.closeQuietly(dbConn);
			}
			
			jedis = jedisPool.getResource();
			try {
				DiscoverV2Cache.setItemCommentList(jedis, companyId, noCacheItemIdSet, noCacheMap);
			} finally {
				jedis.close();
			}
			
			resultMap.putAll(noCacheMap);
		}
		
		return resultMap;
	}
	
	private static final Comparator<DiscoverV2Protos.ItemComment> ITEM_COMMENT_CMP = 
			new Comparator<DiscoverV2Protos.ItemComment>() {

				@Override
				public int compare(DiscoverV2Protos.ItemComment o1, DiscoverV2Protos.ItemComment o2) {
					int cmp = 0;
					
					cmp = Ints.compare(o1.getCommentTime(), o2.getCommentTime());
					if (cmp != 0) {
						return -cmp;
					}
					
					cmp = Longs.compare(o1.getCommentId(), o2.getCommentId());
					if (cmp != 0) {
						return -cmp;
					}
					
					return 0;
				}
		
	};
	
	@Override
	public ListenableFuture<GetItemCommentListResponse> getItemCommentList(RequestHead head, GetItemCommentListRequest request) {
		final long companyId = head.getSession().getCompanyId();
		final long itemId = request.getItemId();
		final int size = request.getSize() < 0 ? 0 : request.getSize() > 50 ? 50 : request.getSize();
		final DiscoverV2Protos.ItemComment offsetIdx;
		
		if (request.hasOffsetIndex() && !request.getOffsetIndex().isEmpty()) {
			DiscoverV2Protos.ItemComment tmp = null;
			try {
				tmp = DiscoverV2Protos.ItemComment.parseFrom(request.getOffsetIndex());
			} catch (InvalidProtocolBufferException e) {
				tmp = null;
			}
			
			if (tmp != null && tmp.getItemId() == itemId) {
				offsetIdx = tmp;
			} else {
				offsetIdx = null;
			}
		} else {
			offsetIdx = null;
		}
		
		DiscoverV2Protos.Item item = this.doGetValidItem(head, Collections.singleton(itemId), false).get(itemId);
		if (item == null || !item.getBase().getEnableComment()) {
			return Futures.immediateFuture(GetItemCommentListResponse.newBuilder()
					.setHasMore(false)
					.setOffsetIndex(ByteString.EMPTY)
					.build());
		}
		
		DiscoverV2DAOProtos.ItemCommentList itemCommentList = this.doGetItemCommentList(companyId, Collections.singleton(itemId)).get(itemId);
		if (itemCommentList == null) {
			return Futures.immediateFuture(GetItemCommentListResponse.newBuilder()
					.setHasMore(false)
					.setOffsetIndex(ByteString.EMPTY)
					.build());
		}
		
		List<DiscoverV2Protos.ItemComment> list = new ArrayList<DiscoverV2Protos.ItemComment>(size + 1);
		
		for (DiscoverV2Protos.ItemComment itemComment : itemCommentList.getItemCommentList()) {
			if (offsetIdx != null && ITEM_COMMENT_CMP.compare(itemComment, offsetIdx) <= 0) {
				continue;
			}
			
			if (!itemComment.getIsDelete()) {
				list.add(itemComment);
				if (list.size() > size) {
					break;
				}
			}
		}
		
		if (list.size() < size + 1 && itemCommentList.getItemCommentCount() >= ITEM_COMMENT_LIST_MAX_CACHE_SIZE) {
			Connection dbConn = null;
			try {
				dbConn = hikariDataSource.getConnection();
				list = DiscoverV2DB.getItemCommentList(dbConn, companyId, itemId, false, offsetIdx, size + 1).getItemCommentList();
			} catch (SQLException e) {
				throw new RuntimeException("db failed", e);
			} finally {
				DBUtil.closeQuietly(dbConn);
			}
		}
		
		final boolean hasMore;
		if (list.size() > size) {
			hasMore = true;
			list = list.subList(0, size);
		} else {
			hasMore = false;
		}
		
		GetItemCommentListResponse.Builder responseBuilder = GetItemCommentListResponse.newBuilder();
		responseBuilder.addAllItemComment(list);
		responseBuilder.setHasMore(hasMore);
		if (list.isEmpty()) {
			responseBuilder.setOffsetIndex(request.hasOffsetIndex() ? request.getOffsetIndex() : ByteString.EMPTY);
		} else {
			responseBuilder.setOffsetIndex(list.get(list.size() - 1).toBuilder().setCommentText("").build().toByteString());
		}
		
		responseBuilder.setItemCommentCnt(item.getCount().getCommentCnt());
		responseBuilder.setItemCommentUserCnt(item.getCount().getCommentUserCnt());
		
		return Futures.immediateFuture(responseBuilder.build());
	}

	@Override
	public ListenableFuture<GetUserCommentListResponse> getUserCommentList(RequestHead head, GetUserCommentListRequest request) {
		final long companyId = head.getSession().getCompanyId();
		final long userId = request.getUserId();
		final int size = request.getSize() < 0 ? 0 : request.getSize() > 50 ? 50 : request.getSize();
		final DiscoverV2Protos.ItemComment offsetIdx;
		
		if (request.hasOffsetIndex() && !request.getOffsetIndex().isEmpty()) {
			DiscoverV2Protos.ItemComment tmp = null;
			try {
				tmp = DiscoverV2Protos.ItemComment.parseFrom(request.getOffsetIndex());
			} catch (InvalidProtocolBufferException e) {
				tmp = null;
			}
			if (tmp != null && tmp.getUserId() == userId) {
				offsetIdx = tmp;
			} else {
				offsetIdx = null;
			}
		} else {
			offsetIdx = null;
		}
		
		List<DiscoverV2Protos.ItemComment> list;
		Connection dbConn = null;
		try {
			dbConn = hikariDataSource.getConnection();
			list = DiscoverV2DB.getUserCommentList(dbConn, companyId, userId, false, offsetIdx, size + 1).getItemCommentList();
		} catch (SQLException e) {
			throw new RuntimeException("db failed", e);
		} finally {
			DBUtil.closeQuietly(dbConn);
		}
		
		final boolean hasMore;
		if (list.size() > size) {
			hasMore = true;
			list = list.subList(0, size);
		} else {
			hasMore = false;
		}
		
		Set<Long> refItemIdSet = new TreeSet<Long>();
		for (DiscoverV2Protos.ItemComment itemComment : list) {
			refItemIdSet.add(itemComment.getItemId());
		}
		
		Map<Long, DiscoverV2Protos.Item> refValidItemMap = this.doGetValidItem(head, refItemIdSet, true);
		
		GetUserCommentListResponse.Builder responseBuilder = GetUserCommentListResponse.newBuilder();
		
		for (DiscoverV2Protos.ItemComment itemComment : list) {
			DiscoverV2Protos.Item validItem = refValidItemMap.get(itemComment.getItemId());
			if (validItem != null && validItem.getBase().getEnableComment()) {
				responseBuilder.addItemComment(itemComment);
			}
		}
		responseBuilder.setHasMore(hasMore);
		if (list.isEmpty()) {
			responseBuilder.setOffsetIndex(request.hasOffsetIndex() ? request.getOffsetIndex() : ByteString.EMPTY);
		} else {
			responseBuilder.setOffsetIndex(list.get(list.size() - 1).toBuilder().setCommentText("").build().toByteString());
		}
		responseBuilder.addAllRefItem(refValidItemMap.values());
		
		return Futures.immediateFuture(responseBuilder.build());
	}
	
	private static final int ITEM_SCORE_LIST_MAX_CACHE_SIZE = 100;
	
	private Map<Long, DiscoverV2DAOProtos.ItemScoreList> doGetItemScoreList(long companyId, Collection<Long> itemIds) {
		if (itemIds.isEmpty()) {
			return Collections.emptyMap();
		}
		
		Map<Long, DiscoverV2DAOProtos.ItemScoreList> resultMap = new TreeMap<Long, DiscoverV2DAOProtos.ItemScoreList>();
		
		Set<Long> noCacheItemIdSet = new TreeSet<Long>();
		Jedis jedis = jedisPool.getResource();
		try {
			resultMap.putAll(DiscoverV2Cache.getItemScoreList(jedis, companyId, itemIds, noCacheItemIdSet));
		} finally {
			jedis.close();
		}
		
		if (!noCacheItemIdSet.isEmpty()) {
			
			Map<Long, DiscoverV2DAOProtos.ItemScoreList> noCacheMap;
			Connection dbConn = null;
			try {
				dbConn = hikariDataSource.getConnection();
				noCacheMap = DiscoverV2DB.getItemScoreList(dbConn, companyId, noCacheItemIdSet, ITEM_SCORE_LIST_MAX_CACHE_SIZE);
			} catch (SQLException e) {
				throw new RuntimeException("db failed", e);
			} finally {
				DBUtil.closeQuietly(dbConn);
			}
			
			jedis = jedisPool.getResource();
			try {
				DiscoverV2Cache.setItemScoreList(jedis, companyId, noCacheItemIdSet, noCacheMap);
			} finally {
				jedis.close();
			}
			
			resultMap.putAll(noCacheMap);
		}
		
		return resultMap;
	}
	
	private static final Comparator<DiscoverV2Protos.ItemScore> ITEM_SCORE_CMP = 
			new Comparator<DiscoverV2Protos.ItemScore>() {

				@Override
				public int compare(DiscoverV2Protos.ItemScore o1, DiscoverV2Protos.ItemScore o2) {
					int cmp = 0;
					
					cmp = Ints.compare(o1.getScoreTime(), o2.getScoreTime());
					if (cmp != 0) {
						return -cmp;
					}
					
					cmp = Longs.compare(o1.getItemId(), o2.getItemId());
					if (cmp != 0) {
						return -cmp;
					}
					
					cmp = Longs.compare(o1.getUserId(), o2.getUserId());
					if (cmp != 0) {
						return -cmp;
					}
					
					return 0;
				}
		
	};

	@Override
	public ListenableFuture<GetItemScoreListResponse> getItemScoreList(RequestHead head, GetItemScoreListRequest request) {
		final long companyId = head.getSession().getCompanyId();
		final long itemId = request.getItemId();
		final int size = request.getSize() < 0 ? 0 : request.getSize() > 50 ? 50 : request.getSize();
		final DiscoverV2Protos.ItemScore offsetIdx;
		
		if (request.hasOffsetIndex() && !request.getOffsetIndex().isEmpty()) {
			DiscoverV2Protos.ItemScore tmp = null;
			try {
				tmp = DiscoverV2Protos.ItemScore.parseFrom(request.getOffsetIndex());
			} catch (InvalidProtocolBufferException e) {
				tmp = null;
			}
			
			if (tmp != null && tmp.getItemId() == itemId) {
				offsetIdx = tmp;
			} else {
				offsetIdx = null;
			}
		} else {
			offsetIdx = null;
		}
		
		DiscoverV2Protos.Item item = this.doGetValidItem(head, Collections.singleton(itemId), false).get(itemId);
		if (item == null || !item.getBase().getEnableScore()) {
			return Futures.immediateFuture(GetItemScoreListResponse.newBuilder()
					.setHasMore(false)
					.setOffsetIndex(ByteString.EMPTY)
					.build());
		}
		
		DiscoverV2DAOProtos.ItemScoreList itemScoreList = this.doGetItemScoreList(companyId, Collections.singleton(itemId)).get(itemId);
		if (itemScoreList == null) {
			return Futures.immediateFuture(GetItemScoreListResponse.newBuilder()
					.setHasMore(false)
					.setOffsetIndex(ByteString.EMPTY)
					.build());
		}
		
		List<DiscoverV2Protos.ItemScore> list = new ArrayList<DiscoverV2Protos.ItemScore>(size + 1);
		
		for (DiscoverV2Protos.ItemScore itemScore : itemScoreList.getItemScoreList()) {
			if (offsetIdx != null && ITEM_SCORE_CMP.compare(itemScore, offsetIdx) <= 0) {
				continue;
			}
			
			list.add(itemScore);
			if (list.size() > size) {
				break;
			}
		}
		
		if (list.size() < size + 1 && itemScoreList.getItemScoreCount() >= ITEM_SCORE_LIST_MAX_CACHE_SIZE) {
			Connection dbConn = null;
			try {
				dbConn = hikariDataSource.getConnection();
				list = DiscoverV2DB.getItemScoreList(dbConn, companyId, itemId, offsetIdx, size + 1).getItemScoreList();
			} catch (SQLException e) {
				throw new RuntimeException("db failed", e);
			} finally {
				DBUtil.closeQuietly(dbConn);
			}
		}
		
		final boolean hasMore;
		if (list.size() > size) {
			hasMore = true;
			list = list.subList(0, size);
		} else {
			hasMore = false;
		}
		
		DiscoverV2Protos.ItemScore userItemScore;
		Connection dbConn = null;
		try {
			dbConn = hikariDataSource.getConnection();
			userItemScore = DiscoverV2DB.getUserItemScore(dbConn, companyId, head.getSession().getUserId(), itemId);
		} catch (SQLException e) {
			throw new RuntimeException("db failed", e);
		} finally {
			DBUtil.closeQuietly(dbConn);
		}
		
		GetItemScoreListResponse.Builder responseBuilder = GetItemScoreListResponse.newBuilder();
		responseBuilder.addAllItemScore(list);
		responseBuilder.setHasMore(hasMore);
		if (list.isEmpty()) {
			responseBuilder.setOffsetIndex(request.hasOffsetIndex() ? request.getOffsetIndex() : ByteString.EMPTY);
		} else {
			responseBuilder.setOffsetIndex(list.get(list.size() - 1).toByteString());
		}
		
		responseBuilder.setItemScoreNumber(item.getCount().getScoreNumber());
		responseBuilder.setItemScoreUserCnt(item.getCount().getScoreUserCnt());
		if (userItemScore != null) {
			responseBuilder.setUserItemScore(userItemScore);
		}
		
		return Futures.immediateFuture(responseBuilder.build());
	}

	@Override
	public ListenableFuture<GetUserScoreListResponse> getUserScoreList(RequestHead head, GetUserScoreListRequest request) {
		final long companyId = head.getSession().getCompanyId();
		final long userId = request.getUserId();
		final int size = request.getSize() < 0 ? 0 : request.getSize() > 50 ? 50 : request.getSize();
		final DiscoverV2Protos.ItemScore offsetIdx;
		
		if (request.hasOffsetIndex() && !request.getOffsetIndex().isEmpty()) {
			DiscoverV2Protos.ItemScore tmp = null;
			try {
				tmp = DiscoverV2Protos.ItemScore.parseFrom(request.getOffsetIndex());
			} catch (InvalidProtocolBufferException e) {
				tmp = null;
			}
			if (tmp != null && tmp.getUserId() == userId) {
				offsetIdx = tmp;
			} else {
				offsetIdx = null;
			}
		} else {
			offsetIdx = null;
		}
		
		List<DiscoverV2Protos.ItemScore> list;
		Connection dbConn = null;
		try {
			dbConn = hikariDataSource.getConnection();
			list = DiscoverV2DB.getUserScoreList(dbConn, companyId, userId, offsetIdx, size + 1).getItemScoreList();
		} catch (SQLException e) {
			throw new RuntimeException("db failed", e);
		} finally {
			DBUtil.closeQuietly(dbConn);
		}
		
		final boolean hasMore;
		if (list.size() > size) {
			hasMore = true;
			list = list.subList(0, size);
		} else {
			hasMore = false;
		}
		
		Set<Long> refItemIdSet = new TreeSet<Long>();
		for (DiscoverV2Protos.ItemScore itemScore : list) {
			refItemIdSet.add(itemScore.getItemId());
		}
		
		Map<Long, DiscoverV2Protos.Item> refValidItemMap = this.doGetValidItem(head, refItemIdSet, true);
		
		GetUserScoreListResponse.Builder responseBuilder = GetUserScoreListResponse.newBuilder();
		
		for (DiscoverV2Protos.ItemScore itemScore : list) {
			DiscoverV2Protos.Item validItem = refValidItemMap.get(itemScore.getItemId());
			if (validItem != null && validItem.getBase().getEnableScore()) {
				responseBuilder.addItemScore(itemScore);
			}
		}
		responseBuilder.setHasMore(hasMore);
		if (list.isEmpty()) {
			responseBuilder.setOffsetIndex(request.hasOffsetIndex() ? request.getOffsetIndex() : ByteString.EMPTY);
		} else {
			responseBuilder.setOffsetIndex(list.get(list.size() - 1).toByteString());
		}
		responseBuilder.addAllRefItem(refValidItemMap.values());
		
		return Futures.immediateFuture(responseBuilder.build());
	}
	
	private static final int ITEM_LIKE_LIST_MAX_CACHE_SIZE = 100;
	
	private Map<Long, DiscoverV2DAOProtos.ItemLikeList> doGetItemLikeList(long companyId, Collection<Long> itemIds) {
		if (itemIds.isEmpty()) {
			return Collections.emptyMap();
		}
		
		Map<Long, DiscoverV2DAOProtos.ItemLikeList> resultMap = new TreeMap<Long, DiscoverV2DAOProtos.ItemLikeList>();
		
		Set<Long> noCacheItemIdSet = new TreeSet<Long>();
		Jedis jedis = jedisPool.getResource();
		try {
			resultMap.putAll(DiscoverV2Cache.getItemLikeList(jedis, companyId, itemIds, noCacheItemIdSet));
		} finally {
			jedis.close();
		}
		
		if (!noCacheItemIdSet.isEmpty()) {
			
			Map<Long, DiscoverV2DAOProtos.ItemLikeList> noCacheMap;
			Connection dbConn = null;
			try {
				dbConn = hikariDataSource.getConnection();
				noCacheMap = DiscoverV2DB.getItemLikeList(dbConn, companyId, noCacheItemIdSet, ITEM_LIKE_LIST_MAX_CACHE_SIZE);
			} catch (SQLException e) {
				throw new RuntimeException("db failed", e);
			} finally {
				DBUtil.closeQuietly(dbConn);
			}
			
			jedis = jedisPool.getResource();
			try {
				DiscoverV2Cache.setItemLikeList(jedis, companyId, noCacheItemIdSet, noCacheMap);
			} finally {
				jedis.close();
			}
			
			resultMap.putAll(noCacheMap);
		}
		
		return resultMap;
	}
	
	private static final Comparator<DiscoverV2Protos.ItemLike> ITEM_LIKE_CMP = 
			new Comparator<DiscoverV2Protos.ItemLike>() {

				@Override
				public int compare(DiscoverV2Protos.ItemLike o1, DiscoverV2Protos.ItemLike o2) {
					int cmp = 0;
					
					cmp = Ints.compare(o1.getLikeTime(), o2.getLikeTime());
					if (cmp != 0) {
						return -cmp;
					}
					
					cmp = Longs.compare(o1.getItemId(), o2.getItemId());
					if (cmp != 0) {
						return -cmp;
					}
					
					cmp = Longs.compare(o1.getUserId(), o2.getUserId());
					if (cmp != 0) {
						return -cmp;
					}
					
					return 0;
				}
		
	};

	@Override
	public ListenableFuture<GetItemLikeListResponse> getItemLikeList(RequestHead head, GetItemLikeListRequest request) {
		final long companyId = head.getSession().getCompanyId();
		final long itemId = request.getItemId();
		final int size = request.getSize() < 0 ? 0 : request.getSize() > 50 ? 50 : request.getSize();
		final DiscoverV2Protos.ItemLike offsetIdx;
		
		if (request.hasOffsetIndex() && !request.getOffsetIndex().isEmpty()) {
			DiscoverV2Protos.ItemLike tmp = null;
			try {
				tmp = DiscoverV2Protos.ItemLike.parseFrom(request.getOffsetIndex());
			} catch (InvalidProtocolBufferException e) {
				tmp = null;
			}
			
			if (tmp != null && tmp.getItemId() == itemId) {
				offsetIdx = tmp;
			} else {
				offsetIdx = null;
			}
		} else {
			offsetIdx = null;
		}
		
		DiscoverV2Protos.Item item = this.doGetValidItem(head, Collections.singleton(itemId), false).get(itemId);
		if (item == null || !item.getBase().getEnableLike()) {
			return Futures.immediateFuture(GetItemLikeListResponse.newBuilder()
					.setHasMore(false)
					.setOffsetIndex(ByteString.EMPTY)
					.build());
		}
		
		DiscoverV2DAOProtos.ItemLikeList itemLikeList = this.doGetItemLikeList(companyId, Collections.singleton(itemId)).get(itemId);
		if (itemLikeList == null) {
			return Futures.immediateFuture(GetItemLikeListResponse.newBuilder()
					.setHasMore(false)
					.setOffsetIndex(ByteString.EMPTY)
					.build());
		}
		
		List<DiscoverV2Protos.ItemLike> list = new ArrayList<DiscoverV2Protos.ItemLike>(size + 1);
		
		for (DiscoverV2Protos.ItemLike itemLike : itemLikeList.getItemLikeList()) {
			if (offsetIdx != null && ITEM_LIKE_CMP.compare(itemLike, offsetIdx) <= 0) {
				continue;
			}
			
			list.add(itemLike);
			if (list.size() > size) {
				break;
			}
		}
		
		if (list.size() < size + 1 && itemLikeList.getItemLikeCount() >= ITEM_LIKE_LIST_MAX_CACHE_SIZE) {
			Connection dbConn = null;
			try {
				dbConn = hikariDataSource.getConnection();
				list = DiscoverV2DB.getItemLikeList(dbConn, companyId, itemId, offsetIdx, size + 1).getItemLikeList();
			} catch (SQLException e) {
				throw new RuntimeException("db failed", e);
			} finally {
				DBUtil.closeQuietly(dbConn);
			}
		}
		
		final boolean hasMore;
		if (list.size() > size) {
			hasMore = true;
			list = list.subList(0, size);
		} else {
			hasMore = false;
		}
		
		DiscoverV2Protos.ItemLike userItemLike;
		Connection dbConn = null;
		try {
			dbConn = hikariDataSource.getConnection();
			userItemLike = DiscoverV2DB.getUserItemLike(dbConn, companyId, head.getSession().getUserId(), itemId);
		} catch (SQLException e) {
			throw new RuntimeException("db failed", e);
		} finally {
			DBUtil.closeQuietly(dbConn);
		}
		
		GetItemLikeListResponse.Builder responseBuilder = GetItemLikeListResponse.newBuilder();
		responseBuilder.addAllItemLike(list);
		responseBuilder.setHasMore(hasMore);
		if (list.isEmpty()) {
			responseBuilder.setOffsetIndex(request.hasOffsetIndex() ? request.getOffsetIndex() : ByteString.EMPTY);
		} else {
			responseBuilder.setOffsetIndex(list.get(list.size() - 1).toByteString());
		}
		
		responseBuilder.setItemLikeCnt(item.getCount().getLikeCnt());
		if (userItemLike != null) {
			responseBuilder.setUserItemLike(userItemLike);
		}
		
		return Futures.immediateFuture(responseBuilder.build());
	}

	@Override
	public ListenableFuture<GetUserLikeListResponse> getUserLikeList(RequestHead head, GetUserLikeListRequest request) {
		final long companyId = head.getSession().getCompanyId();
		final long userId = request.getUserId();
		final int size = request.getSize() < 0 ? 0 : request.getSize() > 50 ? 50 : request.getSize();
		final DiscoverV2Protos.ItemLike offsetIdx;
		
		if (request.hasOffsetIndex() && !request.getOffsetIndex().isEmpty()) {
			DiscoverV2Protos.ItemLike tmp = null;
			try {
				tmp = DiscoverV2Protos.ItemLike.parseFrom(request.getOffsetIndex());
			} catch (InvalidProtocolBufferException e) {
				tmp = null;
			}
			if (tmp != null && tmp.getUserId() == userId) {
				offsetIdx = tmp;
			} else {
				offsetIdx = null;
			}
		} else {
			offsetIdx = null;
		}
		
		List<DiscoverV2Protos.ItemLike> list;
		Connection dbConn = null;
		try {
			dbConn = hikariDataSource.getConnection();
			list = DiscoverV2DB.getUserLikeList(dbConn, companyId, userId, offsetIdx, size + 1).getItemLikeList();
		} catch (SQLException e) {
			throw new RuntimeException("db failed", e);
		} finally {
			DBUtil.closeQuietly(dbConn);
		}
		
		final boolean hasMore;
		if (list.size() > size) {
			hasMore = true;
			list = list.subList(0, size);
		} else {
			hasMore = false;
		}
		
		Set<Long> refItemIdSet = new TreeSet<Long>();
		for (DiscoverV2Protos.ItemLike itemLike : list) {
			refItemIdSet.add(itemLike.getItemId());
		}
		
		Map<Long, DiscoverV2Protos.Item> refValidItemMap = this.doGetValidItem(head, refItemIdSet, true);
		
		GetUserLikeListResponse.Builder responseBuilder = GetUserLikeListResponse.newBuilder();
		
		for (DiscoverV2Protos.ItemLike itemLike : list) {
			DiscoverV2Protos.Item validItem = refValidItemMap.get(itemLike.getItemId());
			if (validItem != null && validItem.getBase().getEnableLike()) {
				responseBuilder.addItemLike(itemLike);
			}
		}
		responseBuilder.setHasMore(hasMore);
		if (list.isEmpty()) {
			responseBuilder.setOffsetIndex(request.hasOffsetIndex() ? request.getOffsetIndex() : ByteString.EMPTY);
		} else {
			responseBuilder.setOffsetIndex(list.get(list.size() - 1).toByteString());
		}
		responseBuilder.addAllRefItem(refValidItemMap.values());
		
		return Futures.immediateFuture(responseBuilder.build());
	}

	private static final int ITEM_SHARE_LIST_MAX_CACHE_SIZE = 100;
	
	private Map<Long, DiscoverV2DAOProtos.ItemShareList> doGetItemShareList(long companyId, Collection<Long> itemIds) {
		if (itemIds.isEmpty()) {
			return Collections.emptyMap();
		}
		
		Map<Long, DiscoverV2DAOProtos.ItemShareList> resultMap = new TreeMap<Long, DiscoverV2DAOProtos.ItemShareList>();
		
		Set<Long> noCacheItemIdSet = new TreeSet<Long>();
		Jedis jedis = jedisPool.getResource();
		try {
			resultMap.putAll(DiscoverV2Cache.getItemShareList(jedis, companyId, itemIds, noCacheItemIdSet));
		} finally {
			jedis.close();
		}
		
		if (!noCacheItemIdSet.isEmpty()) {
			
			Map<Long, DiscoverV2DAOProtos.ItemShareList> noCacheMap;
			Connection dbConn = null;
			try {
				dbConn = hikariDataSource.getConnection();
				noCacheMap = DiscoverV2DB.getItemShareList(dbConn, companyId, noCacheItemIdSet, ITEM_SHARE_LIST_MAX_CACHE_SIZE);
			} catch (SQLException e) {
				throw new RuntimeException("db failed", e);
			} finally {
				DBUtil.closeQuietly(dbConn);
			}
			
			jedis = jedisPool.getResource();
			try {
				DiscoverV2Cache.setItemShareList(jedis, companyId, noCacheItemIdSet, noCacheMap);
			} finally {
				jedis.close();
			}
			
			resultMap.putAll(noCacheMap);
		}
		
		return resultMap;
	}
	
	private static final Comparator<DiscoverV2Protos.ItemShare> ITEM_SHARE_CMP = 
			new Comparator<DiscoverV2Protos.ItemShare>() {

				@Override
				public int compare(DiscoverV2Protos.ItemShare o1, DiscoverV2Protos.ItemShare o2) {
					int cmp = 0;
					
					cmp = Ints.compare(o1.getShareTime(), o2.getShareTime());
					if (cmp != 0) {
						return -cmp;
					}
					
					cmp = Longs.compare(o1.getItemId(), o2.getItemId());
					if (cmp != 0) {
						return -cmp;
					}
					
					cmp = Longs.compare(o1.getUserId(), o2.getUserId());
					if (cmp != 0) {
						return -cmp;
					}
					
					return 0;
				}
		
	};
	
	@Override
	public ListenableFuture<GetItemShareListResponse> getItemShareList(RequestHead head, GetItemShareListRequest request) {
		final long companyId = head.getSession().getCompanyId();
		final long itemId = request.getItemId();
		final int size = request.getSize() < 0 ? 0 : request.getSize() > 50 ? 50 : request.getSize();
		final DiscoverV2Protos.ItemShare offsetIdx;
		
		if (request.hasOffsetIndex() && !request.getOffsetIndex().isEmpty()) {
			DiscoverV2Protos.ItemShare tmp = null;
			try {
				tmp = DiscoverV2Protos.ItemShare.parseFrom(request.getOffsetIndex());
			} catch (InvalidProtocolBufferException e) {
				tmp = null;
			}
			
			if (tmp != null && tmp.getItemId() == itemId) {
				offsetIdx = tmp;
			} else {
				offsetIdx = null;
			}
		} else {
			offsetIdx = null;
		}
		
		DiscoverV2Protos.Item item = this.doGetValidItem(head, Collections.singleton(itemId), false).get(itemId);
		if (item == null || !item.getBase().getEnableLike()) {
			return Futures.immediateFuture(GetItemShareListResponse.newBuilder()
					.setHasMore(false)
					.setOffsetIndex(ByteString.EMPTY)
					.build());
		}
		
		DiscoverV2DAOProtos.ItemShareList itemShareList = this.doGetItemShareList(companyId, Collections.singleton(itemId)).get(itemId);
		if (itemShareList == null) {
			return Futures.immediateFuture(GetItemShareListResponse.newBuilder()
					.setHasMore(false)
					.setOffsetIndex(ByteString.EMPTY)
					.build());
		}
		
		List<DiscoverV2Protos.ItemShare> list = new ArrayList<DiscoverV2Protos.ItemShare>(size + 1);
		
		for (DiscoverV2Protos.ItemShare itemShare : itemShareList.getItemShareList()) {
			if (offsetIdx != null && ITEM_SHARE_CMP.compare(itemShare, offsetIdx) <= 0) {
				continue;
			}
			
			list.add(itemShare);
			if (list.size() > size) {
				break;
			}
		}
		
		if (list.size() < size + 1 && itemShareList.getItemShareCount() >= ITEM_SHARE_LIST_MAX_CACHE_SIZE) {
			Connection dbConn = null;
			try {
				dbConn = hikariDataSource.getConnection();
				list = DiscoverV2DB.getItemShareList(dbConn, companyId, itemId, offsetIdx, size + 1).getItemShareList();
			} catch (SQLException e) {
				throw new RuntimeException("db failed", e);
			} finally {
				DBUtil.closeQuietly(dbConn);
			}
		}
		
		final boolean hasMore;
		if (list.size() > size) {
			hasMore = true;
			list = list.subList(0, size);
		} else {
			hasMore = false;
		}
		
		DiscoverV2Protos.ItemShare userItemShare;
		Connection dbConn = null;
		try {
			dbConn = hikariDataSource.getConnection();
			userItemShare = DiscoverV2DB.getUserItemShare(dbConn, companyId, head.getSession().getUserId(), itemId);
		} catch (SQLException e) {
			throw new RuntimeException("db failed", e);
		} finally {
			DBUtil.closeQuietly(dbConn);
		}
		
		GetItemShareListResponse.Builder responseBuilder = GetItemShareListResponse.newBuilder();
		responseBuilder.addAllItemShare(list);
		responseBuilder.setHasMore(hasMore);
		if (list.isEmpty()) {
			responseBuilder.setOffsetIndex(request.hasOffsetIndex() ? request.getOffsetIndex() : ByteString.EMPTY);
		} else {
			responseBuilder.setOffsetIndex(list.get(list.size() - 1).toByteString());
		}
		
		responseBuilder.setItemShareCnt(item.getCount().getShareCnt());
		if (userItemShare != null) {
			responseBuilder.setUserItemShare(userItemShare);
		}
		
		return Futures.immediateFuture(responseBuilder.build());
	}

	@Override
	public ListenableFuture<GetUserShareListResponse> getUserShareList(RequestHead head, GetUserShareListRequest request) {
		final long companyId = head.getSession().getCompanyId();
		final long userId = request.getUserId();
		final int size = request.getSize() < 0 ? 0 : request.getSize() > 50 ? 50 : request.getSize();
		final DiscoverV2Protos.ItemShare offsetIdx;
		
		if (request.hasOffsetIndex() && !request.getOffsetIndex().isEmpty()) {
			DiscoverV2Protos.ItemShare tmp = null;
			try {
				tmp = DiscoverV2Protos.ItemShare.parseFrom(request.getOffsetIndex());
			} catch (InvalidProtocolBufferException e) {
				tmp = null;
			}
			if (tmp != null && tmp.getUserId() == userId) {
				offsetIdx = tmp;
			} else {
				offsetIdx = null;
			}
		} else {
			offsetIdx = null;
		}
		
		List<DiscoverV2Protos.ItemShare> list;
		Connection dbConn = null;
		try {
			dbConn = hikariDataSource.getConnection();
			list = DiscoverV2DB.getUserShareList(dbConn, companyId, userId, offsetIdx, size + 1).getItemShareList();
		} catch (SQLException e) {
			throw new RuntimeException("db failed", e);
		} finally {
			DBUtil.closeQuietly(dbConn);
		}
		
		final boolean hasMore;
		if (list.size() > size) {
			hasMore = true;
			list = list.subList(0, size);
		} else {
			hasMore = false;
		}
		
		Set<Long> refItemIdSet = new TreeSet<Long>();
		for (DiscoverV2Protos.ItemShare itemShare : list) {
			refItemIdSet.add(itemShare.getItemId());
		}
		
		Map<Long, DiscoverV2Protos.Item> refValidItemMap = this.doGetValidItem(head, refItemIdSet, true);
		
		GetUserShareListResponse.Builder responseBuilder = GetUserShareListResponse.newBuilder();
		
		for (DiscoverV2Protos.ItemShare itemShare : list) {
			DiscoverV2Protos.Item validItem = refValidItemMap.get(itemShare.getItemId());
			if (validItem != null && validItem.getBase().getEnableLike()) {
				responseBuilder.addItemShare(itemShare);
			}
		}
		responseBuilder.setHasMore(hasMore);
		if (list.isEmpty()) {
			responseBuilder.setOffsetIndex(request.hasOffsetIndex() ? request.getOffsetIndex() : ByteString.EMPTY);
		} else {
			responseBuilder.setOffsetIndex(list.get(list.size() - 1).toByteString());
		}
		responseBuilder.addAllRefItem(refValidItemMap.values());
		
		return Futures.immediateFuture(responseBuilder.build());
	}

	@Override
	public ListenableFuture<GetUserDiscoverResponse> getUserDiscover(RequestHead head, GetUserDiscoverRequest request) {
		
		final long companyId = head.getSession().getCompanyId();
		Connection dbConn = null;
		try {
			dbConn = hikariDataSource.getConnection();
			DiscoverV2DB.UserDiscover userDiscover = DiscoverV2DB.getUserDiscover(dbConn, companyId, request.getUserId());
			if (userDiscover == null) {
				return Futures.immediateFuture(GetUserDiscoverResponse.newBuilder()
						.setWeekLearnCnt(0)
						.setWeekLearnDuration(0)
						.setWeekLearnItemCnt(0)
						.setWeekCommentCnt(0)
						.setWeekCommentItemCnt(0)
						.setWeekScoreItemCnt(0)
						.setWeekLikeItemCnt(0)
						.setWeekShareItemCnt(0)
						.build());
			} else {
				return Futures.immediateFuture(GetUserDiscoverResponse.newBuilder()
						.setWeekLearnCnt(userDiscover.weekLearnCnt)
						.setWeekLearnDuration(userDiscover.weekLearnDuration)
						.setWeekLearnItemCnt(userDiscover.weekLearnItemCnt)
						.setWeekCommentCnt(userDiscover.weekCommentCnt)
						.setWeekCommentItemCnt(userDiscover.weekCommentItemCnt)
						.setWeekScoreItemCnt(userDiscover.weekScoreItemCnt)
						.setWeekLikeItemCnt(userDiscover.weekLikeItemCnt)
						.setWeekShareItemCnt(userDiscover.weekShareItemCnt)
						.build());
			}
		} catch (SQLException e) {
			throw new RuntimeException("db failed", e);
		} finally {
			DBUtil.closeQuietly(dbConn);
		}
	}
	
	@Override
	public ListenableFuture<SearchItemResponse> searchItem(RequestHead head, SearchItemRequest request) {
		final long companyId = head.getSession().getCompanyId();

		final String keyword = request.getKeyword().trim();
		if (keyword.isEmpty()) {
			return Futures.immediateFuture(SearchItemResponse.newBuilder().build());
		}
		
		List<Long> itemIdList;
		Connection dbConn = null;
		try {
			dbConn = hikariDataSource.getConnection();
			itemIdList = DiscoverV2DB.searchItemId(dbConn, companyId, keyword, USER_STATE_LIST, 20);
		} catch (SQLException e) {
			throw new RuntimeException("db failed", e);
		} finally {
			DBUtil.closeQuietly(dbConn);
		}
		
		Map<Long, DiscoverV2Protos.Item> itemMap = this.doGetValidItem(head, new TreeSet<Long>(itemIdList), true);
		
		SearchItemResponse.Builder repsonseBuilder = SearchItemResponse.newBuilder();
		for (Long itemId : itemIdList) {
			DiscoverV2Protos.Item item = itemMap.get(itemId);
			if (item != null) {
				repsonseBuilder.addItem(item);
			}
		}
		
		return Futures.immediateFuture(repsonseBuilder.build());
	}
	
	@Override
	public ListenableFuture<GetItemListResponse> getItemList(SystemHead head, GetItemListRequest request) {
		final long companyId;
		if (head.hasCompanyId()) {
			companyId = head.getCompanyId();
		} else {
			throw new RuntimeException("company_id不存在！");
		}

		final int itemSize = request.getItemSize() < 0 ? 0 : request.getItemSize() > 500 ? 500 : request.getItemSize();
		final Long lastItemId;
		if (request.hasOffsetIndex() && !request.getOffsetIndex().isEmpty()) {
			Long tmp = null;
			try {
				tmp = Longs.fromByteArray(request.getOffsetIndex().toByteArray());
			} catch (IllegalArgumentException e) {
				tmp = null;
			}
			lastItemId = tmp;
		} else {
			lastItemId = null;
		}
		
		List<Long> itemIdList;
		Connection dbConn = null;
		try {
			dbConn = this.hikariDataSource.getConnection();
			itemIdList = DiscoverV2DB.getItemIdList(dbConn, companyId, lastItemId, itemSize + 1);
		} catch (SQLException e) {
			throw new RuntimeException("db failed", e);
		} finally {
			DBUtil.closeQuietly(dbConn);
		}
		
		final boolean hasMore;
		if (itemIdList.size() > itemSize) {
			hasMore = true;
			itemIdList = itemIdList.subList(0, itemSize);
		} else {
			hasMore = false;
		}
		
		Map<Long, DiscoverV2Protos.Item> itemMap = DiscoverUtil.getItem(hikariDataSource, jedisPool, companyId,
				itemIdList, Arrays.<DiscoverV2Protos.State>asList(DiscoverV2Protos.State.values()), null);
		
		GetItemListResponse.Builder responseBuilder = GetItemListResponse.newBuilder();
		for (Long itemId : itemIdList) {
			DiscoverV2Protos.Item item = itemMap.get(itemId);
			if (item != null) {
				responseBuilder.addItem(item);
			}
		}
		responseBuilder.setHasMore(hasMore);
		
		if (itemIdList.isEmpty()) {
			responseBuilder.setOffsetIndex(ByteString.EMPTY);
		} else {
			responseBuilder.setOffsetIndex(ByteString.copyFrom(Longs.toByteArray(itemIdList.get(itemIdList.size() - 1))));
		}
		
		return Futures.immediateFuture(responseBuilder.build());
	}
	
	private final class RecordUserLearnTask implements Runnable {
		
		private final long companyId;
		private final long userId;
		private final List<DiscoverV2Protos.ItemLearn> itemLearnList;
		private final boolean isReport;

		RecordUserLearnTask(long companyId, long userId, List<DiscoverV2Protos.ItemLearn> itemLearnList, boolean isReport) {
			this.companyId = companyId;
			this.userId = userId;
			this.itemLearnList = itemLearnList;
			this.isReport = isReport;
		}
		
		@Override
		public void run() {
			if (itemLearnList.isEmpty()) {
				return ;
			}
			
			try {
				Map<Long, DiscoverV2DB.ItemLearnUpdateInfo> itemLearnUpdateInfoMap;
				Connection dbConn = null;
				try {
					dbConn = DiscoverV2ServiceImpl.this.hikariDataSource.getConnection();
					itemLearnUpdateInfoMap = DiscoverV2DB.updateItemLearn(dbConn, companyId, userId, itemLearnList, isReport);
				} catch (SQLException e) {
					throw new RuntimeException("db fail", e);
				} finally {
					DBUtil.closeQuietly(dbConn);
				}
				
				if (itemLearnUpdateInfoMap.isEmpty()) {
					return;
				}
				
				Jedis jedis = DiscoverV2ServiceImpl.this.jedisPool.getResource();
				try {
					Set<Long> noCacheItemIdSet = new TreeSet<Long>();
					Map<Long, DiscoverV2Protos.Item.Count> itemCountMap = DiscoverV2Cache.getItemCount(jedis, companyId, itemLearnUpdateInfoMap.keySet(), noCacheItemIdSet);
					
					Map<Long, DiscoverV2Protos.Item.Count> updateItemCountMap = new TreeMap<Long, DiscoverV2Protos.Item.Count>();
					Set<Long> deleteItemCountSet = new TreeSet<Long>();
					
					DiscoverV2Protos.Item.Count.Builder tmpCountBuilder = DiscoverV2Protos.Item.Count.newBuilder();
					for (Entry<Long, DiscoverV2DB.ItemLearnUpdateInfo> entry : itemLearnUpdateInfoMap.entrySet()) {
						final Long itemId = entry.getKey();
						final DiscoverV2DB.ItemLearnUpdateInfo itemLearnUpdateInfo = entry.getValue();
						
						DiscoverV2Protos.Item.Count itemCount = itemCountMap.get(itemId);
						
						if (itemCount != null) {
							tmpCountBuilder.clear();
							tmpCountBuilder.mergeFrom(itemCount);
							tmpCountBuilder.setLearnCnt(tmpCountBuilder.getLearnCnt() + itemLearnUpdateInfo.newLearnCnt);
							tmpCountBuilder.setLearnUserCnt(tmpCountBuilder.getLearnUserCnt() + itemLearnUpdateInfo.newLearnUserCnt);
							
							updateItemCountMap.put(entry.getKey(), tmpCountBuilder.build());
						} else if (!noCacheItemIdSet.contains(itemId)) {
							deleteItemCountSet.add(itemId);
						}
						
					}
					
					DiscoverV2Cache.setItemCount(jedis, companyId, updateItemCountMap);
					DiscoverV2Cache.delItemCount(jedis, companyId, deleteItemCountSet);
					
					noCacheItemIdSet.clear();
					Map<Long, DiscoverV2DAOProtos.ItemLearnList> itemLearnListMap = DiscoverV2Cache.getItemLearnList(jedis, companyId, itemLearnUpdateInfoMap.keySet(), noCacheItemIdSet);
					
					Map<Long, DiscoverV2DAOProtos.ItemLearnList> updateItemLearnListMap = new TreeMap<Long, DiscoverV2DAOProtos.ItemLearnList>();
					Set<Long> deleteItemLearnListSet = new TreeSet<Long>();
					
					DiscoverV2DAOProtos.ItemLearnList.Builder tmpLearnListBuilder = DiscoverV2DAOProtos.ItemLearnList.newBuilder();
					for (Entry<Long, DiscoverV2DB.ItemLearnUpdateInfo> entry : itemLearnUpdateInfoMap.entrySet()) {
						final Long itemId = entry.getKey();
						final DiscoverV2DB.ItemLearnUpdateInfo itemLearnUpdateInfo = entry.getValue();
						
						DiscoverV2DAOProtos.ItemLearnList itemLearnList = itemLearnListMap.get(itemId);
						
						if (itemLearnList != null) {
							tmpLearnListBuilder.clear();
							
							for (DiscoverV2Protos.ItemLearn itemLearn : itemLearnList.getItemLearnList()) {
								if (itemLearn.getUserId() != userId && ITEM_LEARN_CMP.compare(itemLearn, itemLearnUpdateInfo.userItemLearn) < 0) {
									tmpLearnListBuilder.addItemLearn(itemLearn);
								}
							}
							tmpLearnListBuilder.addItemLearn(itemLearnUpdateInfo.userItemLearn);
							for (DiscoverV2Protos.ItemLearn itemLearn : itemLearnList.getItemLearnList()) {
								if (itemLearn.getUserId() != userId && ITEM_LEARN_CMP.compare(itemLearn, itemLearnUpdateInfo.userItemLearn) > 0) {
									tmpLearnListBuilder.addItemLearn(itemLearn);
								}
							}
							
							while (tmpLearnListBuilder.getItemLearnCount() > ITEM_LEARN_LIST_MAX_CACHE_SIZE) {
								tmpLearnListBuilder.removeItemLearn(tmpLearnListBuilder.getItemLearnCount() - 1);
							}
							
							updateItemLearnListMap.put(itemId, tmpLearnListBuilder.build());
							
						} else if (!noCacheItemIdSet.contains(itemId)) {
							deleteItemLearnListSet.add(itemId);
						}
					}
					
					DiscoverV2Cache.setItemLearnList(jedis, companyId, updateItemLearnListMap);
					DiscoverV2Cache.delItemLearnList(jedis, companyId, deleteItemLearnListSet);
					
				} finally {
					jedis.close();
				}
			} catch (Throwable th) {
				logger.error("RecordUserLearnTask fail " + userId + ", " + itemLearnList, th);
			}
		}
		
	}
	
	@Override
	public ListenableFuture<LearnItemResponse> learnItem(RequestHead head, LearnItemRequest request) {
		final long companyId = head.getSession().getCompanyId();
		final long itemId = request.getItemId();
		final int learnDuration = request.getLearnDuration();
		
		if (learnDuration <= 0) {
			return Futures.immediateFuture(LearnItemResponse.newBuilder()
					.setResult(LearnItemResponse.Result.FAIL_DURATION_INVALID)
					.setFailText("学习时长必须大于0秒")
					.build());
		}
		if (learnDuration >= 24 * 60 * 60) {
			return Futures.immediateFuture(LearnItemResponse.newBuilder()
					.setResult(LearnItemResponse.Result.FAIL_DURATION_INVALID)
					.setFailText("学习时长必须小于24小时")
					.build());
		}
		
		DiscoverV2Protos.Item.Base itemBase = this.doGetValidItemBase(head, Collections.singleton(itemId)).get(itemId);
		if (itemBase == null) {
			return Futures.immediateFuture(LearnItemResponse.newBuilder()
					.setResult(LearnItemResponse.Result.FAIL_ITEM_NOT_EXSIT)
					.setFailText("您学习的课件条目不存在")
					.build());
		}
		
		DiscoverV2Protos.ItemLearn itemLearn = DiscoverV2Protos.ItemLearn.newBuilder()
				.setItemId(itemId)
				.setUserId(head.getSession().getUserId())
				.setLearnTime((int) (System.currentTimeMillis() / 1000L))
				.setLearnDuration(learnDuration)
				.setLearnCnt(1)
				.build();
		
		this.serviceExecutor.execute(new RecordUserLearnTask(companyId, itemLearn.getUserId(), Collections.singletonList(itemLearn), false));
		
		return Futures.immediateFuture(LearnItemResponse.newBuilder()
				.setResult(LearnItemResponse.Result.SUCC)
				.build());
	}

	@Override
	public ListenableFuture<EmptyResponse> reportLearnItem(RequestHead head, ReportLearnItemRequest request) {
		final long companyId = head.getSession().getCompanyId();
		if (request.getItemLearnCount() <= 0) {
			return ServiceUtil.EMPTY_RESPONSE_IMMEDIATE_FUTURE;
		}
		
		final int now = (int) (System.currentTimeMillis()/1000L);
		
		Set<Long> itemIdSet = new TreeSet<Long>();
		for (DiscoverV2Protos.ItemLearn itemLearn : request.getItemLearnList()) {
			if (itemLearn.getLearnDuration() <= 0 || itemLearn.getLearnDuration() >= 24 * 60 * 60) {
				continue;
			}
			
			if (itemLearn.getLearnTime() < now - 7 * 24 * 60 * 60 || itemLearn.getLearnTime() > now + 60 * 60 ) {
				// 保证一周之内，且比当前时间1小时小的时间
				continue;
			}
			
			itemIdSet.add(itemLearn.getItemId());
		}
		
		Map<Long, DiscoverV2Protos.Item.Base> itemBaseMap = this.doGetValidItemBase(head, itemIdSet);
		if (itemBaseMap.isEmpty()) {
			return ServiceUtil.EMPTY_RESPONSE_IMMEDIATE_FUTURE;
		}
		
		List<DiscoverV2Protos.ItemLearn> list = new ArrayList<DiscoverV2Protos.ItemLearn>(request.getItemLearnCount());
		
		DiscoverV2Protos.ItemLearn.Builder tmpBuilder = DiscoverV2Protos.ItemLearn.newBuilder();
		for (DiscoverV2Protos.ItemLearn itemLearn : request.getItemLearnList()) {
			if (itemLearn.getLearnDuration() <= 0 || itemLearn.getLearnDuration() >= 24 * 60 * 60) {
				continue;
			}
			
			if (itemLearn.getLearnTime() < now - 7 * 24 * 60 * 60 || itemLearn.getLearnTime() > now + 60 * 60 ) {
				// 保证一周之内，且比当前时间1小时小的时间
				continue;
			}
			
			if (!itemBaseMap.containsKey(itemLearn.getItemId())) {
				continue;
			}
			
			if (itemLearn.getUserId() != head.getSession().getUserId()) {
				list.add(tmpBuilder.clear().mergeFrom(itemLearn).setUserId(head.getSession().getUserId()).build());
			} else {
				list.add(itemLearn);
			}
		}
		
		if (!list.isEmpty()) {
			this.serviceExecutor.execute(new RecordUserLearnTask(companyId, head.getSession().getUserId(), list, true));
		}
		
		return ServiceUtil.EMPTY_RESPONSE_IMMEDIATE_FUTURE;
	}

	@Override
	public ListenableFuture<CommentItemResponse> commentItem(RequestHead head, CommentItemRequest request) {
		CommentItemResponse response = this.doCommentItem(head, request);
		return Futures.immediateFuture(response);
	}
	
	private CommentItemResponse doCommentItem(RequestHead head, CommentItemRequest request) {
		final long companyId = head.getSession().getCompanyId();
		final long itemId = request.getItemId();
		final String commentText = request.getCommentText().trim();
		
		if (commentText.isEmpty()) {
			return CommentItemResponse.newBuilder()
					.setResult(CommentItemResponse.Result.FAIL_CONTENT_INVALID)
					.setFailText("评论内容不能为空")
					.build();
		}
		if (commentText.length() > 191) {
			return CommentItemResponse.newBuilder()
					.setResult(CommentItemResponse.Result.FAIL_CONTENT_INVALID)
					.setFailText("评论内容不能多于191个字符")
					.build();
		}
		
		DiscoverV2Protos.Item.Base itemBase = this.doGetValidItemBase(head, Collections.singleton(itemId)).get(itemId);
		if (itemBase == null) {
			return CommentItemResponse.newBuilder()
					.setResult(CommentItemResponse.Result.FAIL_ITEM_NOT_EXSIT)
					.setFailText("您评论的课件条目不存在")
					.build();
		}
		if (!itemBase.getEnableComment()) {
			return CommentItemResponse.newBuilder()
					.setResult(CommentItemResponse.Result.FAIL_ITEM_DISABLE)
					.setFailText("您评论的课件条目不支持评论功能")
					.build();
		}
		
		DiscoverV2Protos.ItemComment itemComment = DiscoverV2Protos.ItemComment.newBuilder()
				.setCommentId(-1)
				.setItemId(itemId)
				.setUserId(head.getSession().getUserId())
				.setCommentTime((int) (System.currentTimeMillis() / 1000L))
				.setCommentText(commentText)
				.setIsDelete(false)
				.build();
		
		long commentId;
		Map<Long, DiscoverV2Protos.Item.Count> itemCountMap;
		Map<Long, DiscoverV2DAOProtos.ItemCommentList> itemCommentListMap;
		Connection dbConn = null;
		try {
			dbConn = hikariDataSource.getConnection();
			commentId = DiscoverV2DB.insertItemComment(dbConn, companyId, itemComment);
			itemCountMap = DiscoverV2DB.getItemCount(dbConn, companyId, Collections.singleton(itemId));
			itemCommentListMap = DiscoverV2DB.getItemCommentList(dbConn, companyId, Collections.singleton(itemId), false, ITEM_COMMENT_LIST_MAX_CACHE_SIZE);
		} catch (SQLException e) {
			throw new RuntimeException("db failed", e);
		} finally {
			DBUtil.closeQuietly(dbConn);
		}
		
		Jedis jedis = this.jedisPool.getResource();
		try {
			DiscoverV2Cache.setItemCount(jedis, companyId, Collections.singleton(itemId), itemCountMap);
			DiscoverV2Cache.setItemCommentList(jedis, companyId, Collections.singleton(itemId), itemCommentListMap);
		} finally {
			jedis.close();
		}
		
		return CommentItemResponse.newBuilder()
				.setResult(CommentItemResponse.Result.SUCC)
				.setCommentId(commentId)
				.build();
	}

	@Override
	public ListenableFuture<DeleteCommentResponse> deleteComment(RequestHead head, DeleteCommentRequest request) {
		final long companyId = head.getSession().getCompanyId();
		final long itemId = request.getItemId();
		final long commentId = request.getCommentId();
		
		DiscoverV2Protos.Item.Base itemBase = this.doGetValidItemBase(head, Collections.singleton(itemId)).get(itemId);
		if (itemBase == null) {
			return Futures.immediateFuture(DeleteCommentResponse.newBuilder()
					.setResult(DeleteCommentResponse.Result.FAIL_COMMENT_NOT_EXSIT)
					.setFailText("您评论的课件条目不存在")
					.build());
		}
		if (!itemBase.getEnableComment()) {
			return Futures.immediateFuture(DeleteCommentResponse.newBuilder()
					.setResult(DeleteCommentResponse.Result.FAIL_COMMENT_NOT_EXSIT)
					.setFailText("您删除评论的课件条目不支持评论功能")
					.build());
		}
		
		boolean succ;
		Map<Long, DiscoverV2Protos.Item.Count> itemCountMap;
		Map<Long, DiscoverV2DAOProtos.ItemCommentList> itemCommentListMap;
		Connection dbConn = null;
		try {
			dbConn = hikariDataSource.getConnection();
			DiscoverV2Protos.ItemComment itemComment = DiscoverV2DB.getItemComment(dbConn, companyId, itemId, commentId);
			if (itemComment == null) {
				return Futures.immediateFuture(DeleteCommentResponse.newBuilder()
						.setResult(DeleteCommentResponse.Result.FAIL_COMMENT_NOT_EXSIT)
						.setFailText("该评论不存在")
						.build());
			}
			if (itemComment.getUserId() != head.getSession().getUserId()) {
				return Futures.immediateFuture(DeleteCommentResponse.newBuilder()
						.setResult(DeleteCommentResponse.Result.FAIL_COMMENT_OTHER)
						.setFailText("您只能删除自己发表的评论")
						.build());
			}
			if (itemComment.getIsDelete()) {
				return Futures.immediateFuture(DeleteCommentResponse.newBuilder()
						.setResult(DeleteCommentResponse.Result.FAIL_COMMENT_NOT_EXSIT)
						.setFailText("该评论已被删除")
						.build());
			}
			
			succ = DiscoverV2DB.updateItemCommentIsDelete(dbConn, companyId, itemId, commentId, true);
			if (succ) {
				itemCountMap = DiscoverV2DB.getItemCount(dbConn, companyId, Collections.singleton(itemId));
				itemCommentListMap = DiscoverV2DB.getItemCommentList(dbConn, companyId, Collections.singleton(itemId), false, ITEM_COMMENT_LIST_MAX_CACHE_SIZE);
			} else {
				itemCountMap = null;
				itemCommentListMap = null;
			}
		} catch (SQLException e) {
			throw new RuntimeException("db failed", e);
		} finally {
			DBUtil.closeQuietly(dbConn);
		}
		
		if (succ) {
			Jedis jedis = this.jedisPool.getResource();
			try {
				DiscoverV2Cache.setItemCount(jedis, companyId, Collections.singleton(itemId), itemCountMap);
				DiscoverV2Cache.setItemCommentList(jedis, companyId, Collections.singleton(itemId), itemCommentListMap);
			} finally {
				jedis.close();
			}
		}
		
		return Futures.immediateFuture(DeleteCommentResponse.newBuilder()
				.setResult(DeleteCommentResponse.Result.SUCC)
				.build());
	}

	@Override
	public ListenableFuture<ScoreItemResponse> scoreItem(RequestHead head, ScoreItemRequest request) {
		ScoreItemResponse response = this.doScoreItem(head, request);
		return Futures.immediateFuture(response);
	}
	
	private ScoreItemResponse doScoreItem(RequestHead head, ScoreItemRequest request) {
		final long companyId = head.getSession().getCompanyId();
		final long itemId = request.getItemId();
		final int score = request.getScore();
		
		if (score < 0 || score > 100) {
			return ScoreItemResponse.newBuilder()
					.setResult(ScoreItemResponse.Result.FAIL_SCORE_INVALID)
					.setFailText("请在0～100中选择分数")
					.build();
		}
		
		DiscoverV2Protos.Item.Base itemBase = this.doGetValidItemBase(head, Collections.singleton(itemId)).get(itemId);
		if (itemBase == null) {
			return ScoreItemResponse.newBuilder()
					.setResult(ScoreItemResponse.Result.FAIL_ITEM_NOT_EXSIT)
					.setFailText("您打分的课件条目不存在")
					.build();
		}
		if (!itemBase.getEnableScore()) {
			return ScoreItemResponse.newBuilder()
					.setResult(ScoreItemResponse.Result.FAIL_ITEM_DISABLE)
					.setFailText("您打分的课件条目不支持打分功能")
					.build();
		}
		
		DiscoverV2Protos.ItemScore itemScore = DiscoverV2Protos.ItemScore.newBuilder()
				.setItemId(itemId)
				.setUserId(head.getSession().getUserId())
				.setScoreTime((int) (System.currentTimeMillis() / 1000L))
				.setScoreNumber(score)
				.build();
		
		boolean succ;
		Map<Long, DiscoverV2Protos.Item.Count> itemCountMap;
		Map<Long, DiscoverV2DAOProtos.ItemScoreList> itemScoreListMap;
		Connection dbConn = null;
		try {
			dbConn = hikariDataSource.getConnection();
			DiscoverV2Protos.ItemScore oldItemScore = DiscoverV2DB.getItemScore(dbConn, companyId, itemId, head.getSession().getUserId());
			if (oldItemScore != null) {
				return ScoreItemResponse.newBuilder()
						.setResult(ScoreItemResponse.Result.FAIL_ITEM_IS_SCORED)
						.setFailText("该课件条目您已打过分")
						.build();
			}
			
			succ = DiscoverV2DB.insertItemScore(dbConn, companyId, itemScore);
			if (succ) {
				itemCountMap = DiscoverV2DB.getItemCount(dbConn, companyId, Collections.singleton(itemId));
				itemScoreListMap = DiscoverV2DB.getItemScoreList(dbConn, companyId, Collections.singleton(itemId), ITEM_SCORE_LIST_MAX_CACHE_SIZE);
			} else {
				itemCountMap = null;
				itemScoreListMap = null;
			}
		} catch (SQLException e) {
			throw new RuntimeException("db failed", e);
		} finally {
			DBUtil.closeQuietly(dbConn);
		}
		
		if (succ) {
			Jedis jedis = this.jedisPool.getResource();
			try {
				DiscoverV2Cache.setItemCount(jedis, companyId, Collections.singleton(itemId), itemCountMap);
				DiscoverV2Cache.setItemScoreList(jedis, companyId, Collections.singleton(itemId), itemScoreListMap);
			} finally {
				jedis.close();
			}
		}
		
		return ScoreItemResponse.newBuilder()
				.setResult(ScoreItemResponse.Result.SUCC)
				.build();
	}
	
	@Override
	public ListenableFuture<LikeItemResponse> likeItem(RequestHead head, LikeItemRequest request) {
		final long companyId = head.getSession().getCompanyId();
		final long itemId = request.getItemId();
		final boolean isLike = request.getIsLike();
		
		DiscoverV2Protos.Item.Base itemBase = this.doGetValidItemBase(head, Collections.singleton(itemId)).get(itemId);
		if (itemBase == null) {
			return Futures.immediateFuture(LikeItemResponse.newBuilder()
					.setResult(LikeItemResponse.Result.FAIL_ITEM_NOT_EXSIT)
					.setFailText("您点赞的课件条目不存在")
					.build());
		}
		if (!itemBase.getEnableLike()) {
			return Futures.immediateFuture(LikeItemResponse.newBuilder()
					.setResult(LikeItemResponse.Result.FAIL_ITEM_DISABLE)
					.setFailText("您点赞的课件条目不支持点赞功能")
					.build());
		}
		
		DiscoverV2Protos.ItemLike itemLike = DiscoverV2Protos.ItemLike.newBuilder()
				.setItemId(itemId)
				.setUserId(head.getSession().getUserId())
				.setLikeTime((int) (System.currentTimeMillis() / 1000L))
				.build();
		
		boolean succ;
		Map<Long, DiscoverV2Protos.Item.Count> itemCountMap;
		Map<Long, DiscoverV2DAOProtos.ItemLikeList> itemLikeListMap;
		Connection dbConn = null;
		try {
			dbConn = hikariDataSource.getConnection();
			DiscoverV2Protos.ItemLike oldItemLike = DiscoverV2DB.getUserItemLike(dbConn, companyId, head.getSession().getUserId(), itemId);
			if ((isLike && oldItemLike != null) || (!isLike && oldItemLike == null)) {
				return Futures.immediateFuture(LikeItemResponse.newBuilder()
						.setResult(LikeItemResponse.Result.SUCC)
						.build());
			}
			
			if (isLike) {
				succ = DiscoverV2DB.insertItemLike(dbConn, companyId, itemLike);
			} else {
				succ = DiscoverV2DB.deleteItemLike(dbConn, companyId, itemId, head.getSession().getUserId());
			}
			if (succ) {
				itemCountMap = DiscoverV2DB.getItemCount(dbConn, companyId, Collections.singleton(itemId));
				itemLikeListMap = DiscoverV2DB.getItemLikeList(dbConn, companyId, Collections.singleton(itemId), ITEM_LIKE_LIST_MAX_CACHE_SIZE);
			} else {
				itemCountMap = null;
				itemLikeListMap = null;
			}
		} catch (SQLException e) {
			throw new RuntimeException("db failed", e);
		} finally {
			DBUtil.closeQuietly(dbConn);
		}
		
		if (succ) {
			Jedis jedis = this.jedisPool.getResource();
			try {
				DiscoverV2Cache.setItemCount(jedis, companyId, Collections.singleton(itemId), itemCountMap);
				DiscoverV2Cache.setItemLikeList(jedis, companyId, Collections.singleton(itemId), itemLikeListMap);
			} finally {
				jedis.close();
			}
		}
		
		return Futures.immediateFuture(LikeItemResponse.newBuilder()
				.setResult(LikeItemResponse.Result.SUCC)
				.build());
	}

	private boolean isValid(DiscoverV2Protos.Banner banner, @Nullable Collection<Integer> allowModelIds) {
		if (!USER_STATE_LIST.contains(banner.getState())) {
			return false;
		}
		if (allowModelIds != null && banner.hasAllowModelId() && !allowModelIds.contains(banner.getAllowModelId())) {
			return false;
		}
		return true;
	}
	
	private boolean isValid(DiscoverV2Protos.Module module, @Nullable Collection<Integer> allowModelIds) {
		if (!USER_STATE_LIST.contains(module.getState())) {
			return false;
		}
		if (allowModelIds != null && module.hasAllowModelId() && !allowModelIds.contains(module.getAllowModelId())) {
			return false;
		}
		return true;
	}
	
	private boolean isValid(DiscoverV2Protos.Module.Category category, @Nullable Collection<Integer> allowModelIds) {
		if (!USER_STATE_LIST.contains(category.getState())) {
			return false;
		}
		if (allowModelIds != null && category.hasAllowModelId() && !allowModelIds.contains(category.getAllowModelId())) {
			return false;
		}
		return true;
	}
	
	private boolean isValid(DiscoverV2Protos.Item.Base itemBase, @Nullable Collection<Integer> allowModelIds) {
		if (!USER_STATE_LIST.contains(itemBase.getState())) {
			return false;
		}
		if (allowModelIds != null && itemBase.hasAllowModelId() && !allowModelIds.contains(itemBase.getAllowModelId())) {
			return false;
		}
		return true;
	}
	
	private boolean isValid(DiscoverV2DAOProtos.ModuleCategoryItem moduleCategoryItem, @Nullable Collection<Integer> allowModelIds) {
		if (!USER_STATE_LIST.contains(moduleCategoryItem.getItemState())) {
			return false;
		}
		if (allowModelIds != null && moduleCategoryItem.hasItemAllowModelId() && !allowModelIds.contains(moduleCategoryItem.getItemAllowModelId())) {
			return false;
		}
		return true;
	}
	
	private Set<Integer> doCheckAllowModelId(RequestHead head, Set<Integer> modelIdSet) {
		if (modelIdSet.isEmpty()) {
			return Collections.emptySet();
		} 
		
		AllowProtos.CheckAllowResponse checkAllowResponse = Futures.getUnchecked(
				this.allowService.checkAllow(head, AllowProtos.CheckAllowRequest.newBuilder()
						.addAllModelId(modelIdSet)
						.addUserId(head.getSession().getUserId())
						.build()));
		
		Set<Integer> allowedModelIdSet = new TreeSet<Integer>();
		for (AllowProtos.CheckAllowResponse.CheckResult checkResult : checkAllowResponse.getCheckResultList()) {
			if (checkResult.getAllowUserIdList().contains(head.getSession().getUserId())) {
				allowedModelIdSet.add(checkResult.getModelId());
			}
		}
		
		return allowedModelIdSet;
	}


	@Override
	public ListenableFuture<ShareItemResponse> shareItem(RequestHead head, ShareItemRequest request) {
		final long companyId = head.getSession().getCompanyId();
		final long itemId = request.getItemId();
		final long userId = head.getSession().getUserId();
		
		DiscoverV2Protos.Item.Base itemBase = this.doGetValidItemBase(head, Collections.singleton(itemId)).get(itemId);
		if (itemBase == null) {
			return Futures.immediateFuture(ShareItemResponse.newBuilder()
					.setResult(ShareItemResponse.Result.FAIL_ITEM_NOT_EXSIT)
					.setFailText("您分享的课件条目不存在")
					.build());
		}
		if (!itemBase.getEnableShare() || !itemBase.hasWebUrl() || itemBase.getWebUrl().getIsWeizhu()) {
			return Futures.immediateFuture(ShareItemResponse.newBuilder()
					.setResult(ShareItemResponse.Result.FAIL_PERMISSION_DENIED)
					.setFailText("您分享的课件条目不支持分享功能")
					.build());
		}
		
		DiscoverV2Protos.ItemShare itemShare = DiscoverV2Protos.ItemShare.newBuilder()
				.setItemId(itemId)
				.setUserId(userId)
				.setShareTime((int) (System.currentTimeMillis() / 1000L))
				.build();
		
		Connection dbConn = null;
		Map<Long, Count> itemCountMap;
		Map<Long, DiscoverV2DAOProtos.ItemShareList> itemShareListMap;
		try {
			dbConn = hikariDataSource.getConnection();
			DiscoverV2DB.insertItemShare(dbConn, companyId, itemShare);
			
			itemCountMap = DiscoverV2DB.getItemCount(dbConn, companyId, Collections.singleton(itemId));
			itemShareListMap = DiscoverV2DB.getItemShareList(dbConn, companyId, Collections.singleton(itemId), ITEM_SHARE_LIST_MAX_CACHE_SIZE);
		} catch (SQLException e) {
			throw new RuntimeException("db failed", e);
		} finally {
			DBUtil.closeQuietly(dbConn);
		}
		
		Jedis jedis = this.jedisPool.getResource();
		try {
			DiscoverV2Cache.setItemCount(jedis, companyId, Collections.singleton(itemId), itemCountMap);
			DiscoverV2Cache.setItemShareList(jedis, companyId, Collections.singleton(itemId), itemShareListMap);
		} finally {
			jedis.close();
		}
		
		return Futures.immediateFuture(ShareItemResponse.newBuilder()
				.setResult(ShareItemResponse.Result.SUCC)
				.setItemShareContent(ShareItemResponse.ItemShareContent.newBuilder()
						.setItemName(itemBase.getItemName())
						.setItemDesc(itemBase.getItemDesc())
						.setImageName(itemBase.getImageName())
						.setWebUrl(itemBase.getWebUrl().getWebUrl()))
				.build());
	}

}
