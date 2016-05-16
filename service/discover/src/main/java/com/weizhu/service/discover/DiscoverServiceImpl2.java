package com.weizhu.service.discover;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.inject.Inject;
import com.google.protobuf.ByteString;
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
import com.weizhu.proto.DiscoverV2Protos;
import com.weizhu.proto.DiscoverService;
import com.weizhu.proto.DiscoverV2Service;
//import com.weizhu.proto.StatsProtos;
//import com.weizhu.proto.StatsService;
import com.weizhu.proto.WeizhuProtos.EmptyRequest;
import com.weizhu.proto.WeizhuProtos.EmptyResponse;
import com.weizhu.proto.WeizhuProtos.RequestHead;
import com.weizhu.proto.WeizhuProtos.SystemHead;
import com.weizhu.service.discover_v2.DiscoverV2Cache;
import com.weizhu.service.discover_v2.DiscoverV2DB;
import com.zaxxer.hikari.HikariDataSource;

public class DiscoverServiceImpl2 implements DiscoverService {

	private final HikariDataSource hikariDataSource;
	private final JedisPool jedisPool;
	
	private final DiscoverExtends discoverExtends;
	private final DiscoverV2Service discoverV2Service;
//	private final StatsService statsService;
	
	@Inject
	public DiscoverServiceImpl2(HikariDataSource hikariDataSource, JedisPool jedisPool, 
			DiscoverExtends discoverExtends, DiscoverV2Service discoverV2Service /*, StatsService statsService*/) {
		this.hikariDataSource = hikariDataSource;
		this.jedisPool = jedisPool;
		this.discoverExtends = discoverExtends;
		this.discoverV2Service = discoverV2Service;
//		this.statsService = statsService;
	}
	
	@Override
	public ListenableFuture<GetDiscoverHomeResponse> getDiscoverHome(RequestHead head, EmptyRequest request) {
		
		DiscoverV2Protos.GetDiscoverHomeResponse responseV2 = Futures.getUnchecked(
				this.discoverV2Service.getDiscoverHome(head, DiscoverV2Protos.GetDiscoverHomeRequest.getDefaultInstance()));
		
		GetDiscoverHomeResponse.Builder responseBuilder = GetDiscoverHomeResponse.newBuilder();
		
		DiscoverProtos.Banner.Builder tmpBannerBuilder = DiscoverProtos.Banner.newBuilder();
		for (DiscoverV2Protos.Banner bannerV2 : responseV2.getBannerList()) {
			if (bannerV2.getContentCase() == DiscoverV2Protos.Banner.ContentCase.ITEM_ID) {
				tmpBannerBuilder.clear();
				
				responseBuilder.addBanner(tmpBannerBuilder.setBannerId(bannerV2.getBannerId())
						.setBannerName(bannerV2.getBannerName())
						.setImageName(bannerV2.getImageName())
						.setItemId(String.valueOf(bannerV2.getItemId()))
						.setCreateTime(bannerV2.getCreateTime())
						.build());
			} else if (bannerV2.getContentCase() == DiscoverV2Protos.Banner.ContentCase.CONTENT_NOT_SET) {
				tmpBannerBuilder.clear();
				
				responseBuilder.addBanner(tmpBannerBuilder.setBannerId(bannerV2.getBannerId())
						.setBannerName(bannerV2.getBannerName())
						.setImageName(bannerV2.getImageName())
						.setCreateTime(bannerV2.getCreateTime())
						.build());
			}
			
			if (responseBuilder.getBannerCount() >= 5) {
				break;
			}
		}
		
		DiscoverProtos.Module.Builder tmpModuleBuilder = DiscoverProtos.Module.newBuilder();
		DiscoverProtos.Module.Category.Builder tmpCategoryBuilder = DiscoverProtos.Module.Category.newBuilder();
		for (DiscoverV2Protos.Module moduleV2 : responseV2.getModuleList()) {
			if (discoverExtends.isExtendsModule(moduleV2.getModuleId())) {
				tmpModuleBuilder.clear();
				
				tmpModuleBuilder.setModuleId(moduleV2.getModuleId());
				tmpModuleBuilder.setModuleName(moduleV2.getModuleName());
				tmpModuleBuilder.setIconName(moduleV2.getImageName());
				
				tmpModuleBuilder.addCategory(tmpCategoryBuilder.clear()
						.setCategoryId(1)
						.setCategoryName("参加考试")
						.build());
				
				tmpModuleBuilder.addCategory(tmpCategoryBuilder.clear()
						.setCategoryId(2)
						.setCategoryName("考试结果")
						.build());
				
				responseBuilder.addModule(tmpModuleBuilder.build());
				continue;
			}
			
			if (moduleV2.getContentCase() == DiscoverV2Protos.Module.ContentCase.CATEGORY_LIST) {
				tmpModuleBuilder.clear();
				
				tmpModuleBuilder.setModuleId(moduleV2.getModuleId());
				tmpModuleBuilder.setModuleName(moduleV2.getModuleName());
				tmpModuleBuilder.setIconName(moduleV2.getImageName());
				
				for (DiscoverV2Protos.Module.Category categoryV2 : moduleV2.getCategoryList().getCategoryList()) {
					tmpCategoryBuilder.clear();
					
					tmpCategoryBuilder.setCategoryId(categoryV2.getCategoryId());
					tmpCategoryBuilder.setCategoryName(categoryV2.getCategoryName());
					tmpModuleBuilder.addCategory(tmpCategoryBuilder.build());
					
					if (tmpModuleBuilder.getCategoryCount() >= 5) {
						break;
					}
				}
			
				if (tmpModuleBuilder.getCategoryCount() > 0) {
					responseBuilder.addModule(tmpModuleBuilder.build());
				}
			} 
		}
		
		return Futures.immediateFuture(responseBuilder.build());
	}

	@Override
	public ListenableFuture<GetModuleItemListResponse> getModuleItemList(RequestHead head, GetModuleItemListRequest request) {
		final int moduleId = request.getModuleId();
		final int categoryId = request.getCategoryId();
		
		if (discoverExtends.isExtendsModule(moduleId)) {
			return Futures.immediateFuture(discoverExtends.getExtendsModuleItemList(head, request));
		}
		
		DiscoverV2Protos.GetModuleCategoryItemListResponse responseV2 = Futures.getUnchecked(
				this.discoverV2Service.getModuleCategoryItemList(head, 
						DiscoverV2Protos.GetModuleCategoryItemListRequest.newBuilder()
						.setModuleId(moduleId)
						.setCategoryId(categoryId)
						.setItemSize(request.getItemSize())
						.setOffsetIndex(request.getListIndexBegin())
						.build()));
		
		GetModuleItemListResponse.Builder responseBuilder = GetModuleItemListResponse.newBuilder();
		responseBuilder.setClearOldList(false);
		
		DiscoverProtos.Item.Builder tmpItemBuilder = DiscoverProtos.Item.newBuilder();
		for (DiscoverV2Protos.Item itemV2 : responseV2.getItemList()) {
			if (itemV2.getBase().getContentCase() != DiscoverV2Protos.Item.Base.ContentCase.WEB_URL) {
				continue;
			}
			
			tmpItemBuilder.clear();
			
			tmpItemBuilder.setItemId(itemV2.getBase().getItemId());
			tmpItemBuilder.setItemName(itemV2.getBase().getItemName());
			tmpItemBuilder.setIconName(itemV2.getBase().getImageName());
			tmpItemBuilder.setCreateTime(itemV2.getBase().getCreateTime());
			tmpItemBuilder.setItemDesc(itemV2.getBase().getItemDesc());
			tmpItemBuilder.setEnableScore(itemV2.getBase().getEnableScore());
			tmpItemBuilder.setEnableComment(itemV2.getBase().getEnableComment());
			
			responseBuilder.addItem(tmpItemBuilder.build());
		}
		
		responseBuilder.setHasMore(responseV2.getHasMore());
		responseBuilder.setListIndexBegin(ByteString.EMPTY);
		responseBuilder.setListIndexEnd(responseV2.getOffsetIndex());
		
		return Futures.immediateFuture(responseBuilder.build());
	}

	@Override
	public ListenableFuture<GetItemByIdResponse> getItemById(RequestHead head, GetItemByIdRequest request) {
		if (request.getItemIdCount() <= 0) {
			return Futures.immediateFuture(GetItemByIdResponse.getDefaultInstance());
		}
		
		Set<Long> itemIdSet = new TreeSet<Long>();
		Set<Long> extendsItemIdSet = new TreeSet<Long>();
		
		for (Long itemId : request.getItemIdList()) {
			if (this.discoverExtends.isExtendsItem(itemId)) {
				extendsItemIdSet.add(itemId);
			} else {
				itemIdSet.add(itemId);
			}
		}
		
		GetItemByIdResponse.Builder responseBuilder = GetItemByIdResponse.newBuilder();
		
		if (!extendsItemIdSet.isEmpty()) {
			Map<Long, DiscoverProtos.ItemContent> extendsItemMap = this.discoverExtends.getExtendsItemContent(head, extendsItemIdSet);
			for (DiscoverProtos.ItemContent itemContent : extendsItemMap.values()) {
				responseBuilder.addItem(itemContent.getItem());
			}
		}
		
		if (!itemIdSet.isEmpty()) {
			DiscoverV2Protos.GetItemByIdResponse responseV2 = Futures.getUnchecked(
					this.discoverV2Service.getItemById(head, 
							DiscoverV2Protos.GetItemByIdRequest.newBuilder()
							.addAllItemId(itemIdSet)
							.build()));
			
			DiscoverProtos.Item.Builder tmpItemBuilder = DiscoverProtos.Item.newBuilder();
			for (DiscoverV2Protos.Item itemV2 : responseV2.getItemList()) {
				if (itemV2.getBase().getContentCase() != DiscoverV2Protos.Item.Base.ContentCase.WEB_URL) {
					continue;
				}
				
				tmpItemBuilder.clear();
				
				tmpItemBuilder.setItemId(itemV2.getBase().getItemId());
				tmpItemBuilder.setItemName(itemV2.getBase().getItemName());
				tmpItemBuilder.setIconName(itemV2.getBase().getImageName());
				tmpItemBuilder.setCreateTime(itemV2.getBase().getCreateTime());
				tmpItemBuilder.setItemDesc(itemV2.getBase().getItemDesc());
				tmpItemBuilder.setEnableScore(itemV2.getBase().getEnableScore());
				tmpItemBuilder.setEnableComment(itemV2.getBase().getEnableComment());
				
				responseBuilder.addItem(tmpItemBuilder.build());
			}
		}
		
		return Futures.immediateFuture(responseBuilder.build());
	}

	@Override
	public ListenableFuture<GetItemContentResponse> getItemContent(RequestHead head, GetItemContentRequest request) {
		if (this.discoverExtends.isExtendsItem(request.getItemId())) {
			DiscoverProtos.ItemContent itemContent = this.discoverExtends.getExtendsItemContent(head, 
					Collections.singleton(request.getItemId())).get(request.getItemId());
			
			if (itemContent != null) {
				return Futures.immediateFuture(GetItemContentResponse.newBuilder()
						.setItemContent(itemContent)
						.build());
			} else {
				return Futures.immediateFuture(GetItemContentResponse.getDefaultInstance());
			}
		}
		
		DiscoverV2Protos.GetItemByIdResponse responseV2 = Futures.getUnchecked(
				this.discoverV2Service.getItemById(head, 
						DiscoverV2Protos.GetItemByIdRequest.newBuilder()
						.addItemId(request.getItemId())
						.build()));
		
		DiscoverV2Protos.Item itemV2 = null;
		for (DiscoverV2Protos.Item i : responseV2.getItemList()) {
			if (i.getBase().getItemId() == request.getItemId()) {
				itemV2 = i;
				break;
			}
		}
		
		if (itemV2 != null && itemV2.getBase().getContentCase() == DiscoverV2Protos.Item.Base.ContentCase.WEB_URL) {
			return Futures.immediateFuture(GetItemContentResponse.newBuilder()
					.setItemContent(DiscoverProtos.ItemContent.newBuilder()
							.setItem(DiscoverProtos.Item.newBuilder()
									.setItemId(itemV2.getBase().getItemId())
									.setItemName(itemV2.getBase().getItemName())
									.setIconName(itemV2.getBase().getImageName())
									.setCreateTime(itemV2.getBase().getCreateTime())
									.setItemDesc(itemV2.getBase().getItemDesc())
									.setEnableScore(itemV2.getBase().getEnableScore())
									.setEnableComment(itemV2.getBase().getEnableComment())
									.build())
							.setRedirectUrl(itemV2.getBase().getWebUrl().getWebUrl())
							.build())
					.build());
		} else {
			return Futures.immediateFuture(GetItemContentResponse.getDefaultInstance());
		}
	}

	@Override
	public ListenableFuture<SearchItemResponse> searchItem(RequestHead head, SearchItemRequest request) {
		
		DiscoverV2Protos.SearchItemResponse responseV2 = Futures.getUnchecked(
				this.discoverV2Service.searchItem(head, DiscoverV2Protos.SearchItemRequest.newBuilder()
						.setKeyword(request.getKeyword())
						.build()));
		
		SearchItemResponse.Builder responseBuilder = SearchItemResponse.newBuilder();
		
		DiscoverProtos.Item.Builder tmpItemBuilder = DiscoverProtos.Item.newBuilder();
		for (DiscoverV2Protos.Item itemV2 : responseV2.getItemList()) {
			if (itemV2.getBase().getContentCase() != DiscoverV2Protos.Item.Base.ContentCase.WEB_URL) {
				continue;
			}
			
			tmpItemBuilder.clear();
			
			tmpItemBuilder.setItemId(itemV2.getBase().getItemId());
			tmpItemBuilder.setItemName(itemV2.getBase().getItemName());
			tmpItemBuilder.setIconName(itemV2.getBase().getImageName());
			tmpItemBuilder.setCreateTime(itemV2.getBase().getCreateTime());
			tmpItemBuilder.setItemDesc(itemV2.getBase().getItemDesc());
			tmpItemBuilder.setEnableScore(itemV2.getBase().getEnableScore());
			tmpItemBuilder.setEnableComment(itemV2.getBase().getEnableComment());
			
			responseBuilder.addItem(tmpItemBuilder.build());
		}
		
		return Futures.immediateFuture(responseBuilder.build());
	}

	@Override
	public ListenableFuture<EmptyResponse> clearCache(SystemHead head, EmptyRequest request) {
		Jedis jedis = jedisPool.getResource();
		try {
			DiscoverCache.clearCache(jedis);
			DiscoverV2Cache.clearCache(jedis);
		} finally {
			jedis.close();
		}
		return ServiceUtil.EMPTY_RESPONSE_IMMEDIATE_FUTURE;
	}

	@Override
	public ListenableFuture<GetItemListResponse> getItemList(SystemHead head, GetItemListRequest request) {
		final long companyId = head.getCompanyId();
		final int size = request.getSize() < 0 ? 0 : request.getSize() > 500 ? 500 : request.getSize();
		final Long lastItemId = request.hasLastItemId() ? request.getLastItemId() : null;
		
		List<Long> itemIdList;
		Connection dbConn = null;
		try {
			dbConn = this.hikariDataSource.getConnection();	
			itemIdList = DiscoverV2DB.getItemIdList(dbConn, companyId, lastItemId, size + 1);
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
		
		DiscoverV2Protos.GetItemByIdResponse responseV2 = Futures.getUnchecked(
				this.discoverV2Service.getItemById(head, 
						DiscoverV2Protos.GetItemByIdRequest.newBuilder()
						.addAllItemId(itemIdList)
						.build()));
		
		Map<Long, DiscoverProtos.Item> itemMap = new TreeMap<Long, DiscoverProtos.Item>();
		
		DiscoverProtos.Item.Builder tmpItemBuilder = DiscoverProtos.Item.newBuilder();
		for (DiscoverV2Protos.Item itemV2 : responseV2.getItemList()) {
			
			tmpItemBuilder.clear();
			
			tmpItemBuilder.setItemId(itemV2.getBase().getItemId());
			tmpItemBuilder.setItemName(itemV2.getBase().getItemName());
			tmpItemBuilder.setIconName(itemV2.getBase().getImageName());
			tmpItemBuilder.setCreateTime(itemV2.getBase().getCreateTime());
			tmpItemBuilder.setItemDesc(itemV2.getBase().getItemDesc());
			tmpItemBuilder.setEnableScore(itemV2.getBase().getEnableScore());
			tmpItemBuilder.setEnableComment(itemV2.getBase().getEnableComment());
			
			itemMap.put(itemV2.getBase().getItemId(), tmpItemBuilder.build());
		}
		
		GetItemListResponse.Builder responseBuilder = GetItemListResponse.newBuilder();
		
		for (Long itemId : itemIdList) {
			DiscoverProtos.Item item = itemMap.get(itemId);
			if (item != null) {
				responseBuilder.addItem(item);
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
		
		// TODO Auto-generated method stub
		return Futures.immediateFuture(GetItemPVResponse.getDefaultInstance());
	}

	@Override
	public ListenableFuture<GetItemScoreResponse> getItemScore(RequestHead head, GetItemScoreRequest request) {
		
		DiscoverV2Protos.GetItemScoreListResponse responseV2 = 
				Futures.getUnchecked(this.discoverV2Service.getItemScoreList(head, 
						DiscoverV2Protos.GetItemScoreListRequest.newBuilder()
						.setItemId(request.getItemId())
						.setSize(0)
						.build()));
		GetItemScoreResponse.Builder responseBuilder = GetItemScoreResponse.newBuilder();
		
		if (responseV2.hasUserItemScore()) {
			responseBuilder.setScore(responseV2.getUserItemScore().getScoreNumber());
		}
		responseBuilder.setTotalScore(responseV2.getItemScoreNumber() * responseV2.getItemScoreUserCnt());
		responseBuilder.setTotalUser(responseV2.getItemScoreUserCnt());
		
		return Futures.immediateFuture(responseBuilder.build());
	}

	@Override
	public ListenableFuture<ScoreItemResponse> scoreItem(RequestHead head, ScoreItemRequest request) {
		DiscoverV2Protos.ScoreItemResponse responseV2 = 
				Futures.getUnchecked(this.discoverV2Service.scoreItem(head, 
						DiscoverV2Protos.ScoreItemRequest.newBuilder()
						.setItemId(request.getItemId())
						.setScore(request.getScore())
						.build()));
		
		ScoreItemResponse.Result result;
		switch (responseV2.getResult()) {
			case SUCC:
				result = ScoreItemResponse.Result.SUCC;
				break;
			case FAIL_SCORE_INVALID:
				result = ScoreItemResponse.Result.FAIL_SCORE_INVALID;
				break;
			case FAIL_ITEM_NOT_EXSIT:
				result = ScoreItemResponse.Result.FAIL_ITEM_NOT_EXSIT;
				break;
			case FAIL_ITEM_DISABLE:
				result = ScoreItemResponse.Result.FAIL_ITEM_DISABLE;
				break;
			case FAIL_ITEM_IS_SCORED:
				result = ScoreItemResponse.Result.FAIL_ITEM_IS_SCORED;
				break;
			case FAIL_UNKNOWN:
				result = ScoreItemResponse.Result.FAIL_UNKNOWN;
				break;
			default:
				result = ScoreItemResponse.Result.FAIL_UNKNOWN;
				break;
		}
		
		ScoreItemResponse.Builder responseBuilder = ScoreItemResponse.newBuilder();
		responseBuilder.setResult(result);
		if (responseV2.hasFailText()) {
			responseBuilder.setFailText(responseV2.getFailText());
		}
		
		return Futures.immediateFuture(responseBuilder.build());
	}

	@Override
	public ListenableFuture<GetItemCommentListResponse> getItemAllCommentList(RequestHead head, GetItemCommentListRequest request) {
		DiscoverV2Protos.GetItemCommentListResponse responseV2 = 
				Futures.getUnchecked(this.discoverV2Service.getItemCommentList(head, 
						DiscoverV2Protos.GetItemCommentListRequest.newBuilder()
						.setItemId(request.getItemId())
						.setSize(request.getSize())
						.setOffsetIndex(
								request.hasLastCommentId() && request.hasLastCommentTime() ? 
										DiscoverV2Protos.ItemComment.newBuilder()
										.setCommentId(request.getLastCommentId())
										.setItemId(request.getItemId())
										.setUserId(0)
										.setCommentTime(request.getLastCommentTime())
										.setCommentText("")
										.setIsDelete(false)
										.build().toByteString() : ByteString.EMPTY)
						.build()));
		
		GetItemCommentListResponse.Builder responseBuilder = GetItemCommentListResponse.newBuilder();
		
		DiscoverProtos.Comment.Builder tmpCommentBuilder = DiscoverProtos.Comment.newBuilder();
		for (DiscoverV2Protos.ItemComment itemCommentV2 : responseV2.getItemCommentList()) {
			if (itemCommentV2.getIsDelete()) {
				continue;
			}
			
			tmpCommentBuilder.clear();
			tmpCommentBuilder.setCommentId(itemCommentV2.getCommentId());
			tmpCommentBuilder.setCommentTime(itemCommentV2.getCommentTime());
			tmpCommentBuilder.setUserId(itemCommentV2.getUserId());
			tmpCommentBuilder.setContent(itemCommentV2.getCommentText());
			
			responseBuilder.addComment(tmpCommentBuilder.build());
		}
		
		responseBuilder.setHasMore(responseV2.getHasMore());
		responseBuilder.setTotal(responseV2.getItemCommentCnt());
		
		return Futures.immediateFuture(responseBuilder.build());
	}

	@Override
	public ListenableFuture<GetItemCommentListResponse> getItemMyCommentList(RequestHead head, GetItemCommentListRequest request) {
		return Futures.immediateFuture(GetItemCommentListResponse.getDefaultInstance());
	}

	@Override
	public ListenableFuture<CommentItemResponse> commentItem(RequestHead head, CommentItemRequest request) {
		
		DiscoverV2Protos.CommentItemResponse responseV2 = 
				Futures.getUnchecked(this.discoverV2Service.commentItem(head, 
						DiscoverV2Protos.CommentItemRequest.newBuilder()
						.setItemId(request.getItemId())
						.setCommentText(request.getCommentContent())
						.build()));
		
		CommentItemResponse.Result result;
		switch (responseV2.getResult()) {
			case FAIL_CONTENT_INVALID:
				result = CommentItemResponse.Result.FAIL_CONTENT_INVALID;
				break;
			case FAIL_ITEM_DISABLE:
				result = CommentItemResponse.Result.FAIL_ITEM_DISABLE;
				break;
			case FAIL_ITEM_NOT_EXSIT:
				result = CommentItemResponse.Result.FAIL_ITEM_NOT_EXSIT;
				break;
			case FAIL_UNKNOWN:
				result = CommentItemResponse.Result.FAIL_UNKNOWN;
				break;
			case SUCC:
				result = CommentItemResponse.Result.SUCC;
				break;
			default:
				result = CommentItemResponse.Result.FAIL_UNKNOWN;
				break;
		}
		
		CommentItemResponse.Builder responseBuilder = CommentItemResponse.newBuilder();
		responseBuilder.setResult(result);
		if (responseV2.hasFailText()) {
			responseBuilder.setFailText(responseV2.getFailText());
		}
		if (responseV2.hasCommentId()) {
			responseBuilder.setCommentId(responseV2.getCommentId());
		}
		
		return Futures.immediateFuture(responseBuilder.build());
	}

	@Override
	public ListenableFuture<DeleteCommentResponse> deleteComment(RequestHead head, DeleteCommentRequest request) {
		return Futures.immediateFuture(DeleteCommentResponse.newBuilder()
				.setResult(DeleteCommentResponse.Result.SUCC)
				.build());
	}

}
