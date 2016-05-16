package com.weizhu.service.discover;

import java.sql.Date;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;

import com.google.common.util.concurrent.Futures;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import com.weizhu.proto.DiscoverProtos;
import com.weizhu.proto.ExamProtos;
import com.weizhu.proto.DiscoverProtos.GetModuleItemListRequest;
import com.weizhu.proto.DiscoverProtos.GetModuleItemListResponse;
import com.weizhu.proto.ExamProtos.GetClosedExamListRequest;
import com.weizhu.proto.ExamProtos.GetClosedExamListResponse;
import com.weizhu.proto.ExamProtos.GetExamByIdRequest;
import com.weizhu.proto.ExamProtos.GetExamByIdResponse;
import com.weizhu.proto.ExamProtos.GetOpenExamListRequest;
import com.weizhu.proto.ExamProtos.GetOpenExamListResponse;
import com.weizhu.proto.ExamService;
import com.weizhu.proto.WeizhuProtos.RequestHead;

public class DiscoverExtends {

	private static final int EXAM_ITEM_PREFIX = 1;
	
	private final int examModuleId;
	private final String examItemIconName;
	private final ExamService examService;
	
	@Inject
	public DiscoverExtends(
			@Named("discover_extends_exam_module_id") int examModuleId, 
			@Named("discover_extends_exam_item_icon_name") String examItemIconName, 
			ExamService examService
			) {
		this.examModuleId = examModuleId;
		this.examItemIconName = examItemIconName;
		this.examService = examService;
	}
	
	public boolean isExtendsItem(long itemId) {
		final int prefix = getPrefix(itemId);
		if (prefix == EXAM_ITEM_PREFIX) {
			return true;
		}
		return false;
	}
	
	public boolean isExtendsModule(int moduleId) {
		if (moduleId == examModuleId) {
			return true;
		}
		return false;
	}
	
	public Map<Long, DiscoverProtos.ItemContent> getExtendsItemContent(RequestHead head, Collection<Long> itemIds) {
		Set<Integer> examIdSet = new TreeSet<Integer>();
		for (Long itemId : itemIds) {
			final int prefix = getPrefix(itemId);
			final int extendsId = getExtendsId(itemId);
			
			if (prefix == EXAM_ITEM_PREFIX) {
				examIdSet.add(extendsId);
			}
		}
		
		Map<Long, DiscoverProtos.ItemContent> resultMap = new HashMap<Long, DiscoverProtos.ItemContent>();
		
		if (!examIdSet.isEmpty()) {
			
			GetExamByIdResponse response = Futures.getUnchecked(examService.getExamById(head, GetExamByIdRequest.newBuilder()
					.addAllExamId(examIdSet)
					.build()));
			
			Map<Integer, DiscoverProtos.Item> examToitemMap = convertExamToItemList(response.getExamList(), Collections.<Integer, ExamProtos.UserResult>emptyMap());
			
			DiscoverProtos.ItemContent.Builder tmpItemContentBuilder = DiscoverProtos.ItemContent.newBuilder();
			for (Entry<Integer, DiscoverProtos.Item> entry : examToitemMap.entrySet()) {
				tmpItemContentBuilder.clear();
				
				resultMap.put(entry.getValue().getItemId(), tmpItemContentBuilder
						.setItem(entry.getValue())
						.setExamId(entry.getKey())
						.build());
			}
		}
		
		return resultMap;
	}
	
	public GetModuleItemListResponse getExtendsModuleItemList(RequestHead head, GetModuleItemListRequest request) {
		final int moduleId = request.getModuleId();
		final int categoryId = request.getCategoryId();
		
		if (moduleId == examModuleId) {
			if (categoryId == 1) {
				DiscoverDAOProtos.ModuleExamListIndex begin = null;
				if (request.hasListIndexBegin() && !request.getListIndexBegin().isEmpty()) {
					try {
						begin = DiscoverDAOProtos.ModuleExamListIndex.parseFrom(request.getListIndexBegin());
					} catch (InvalidProtocolBufferException e) {
						// ignore
					}
				}
				
				GetOpenExamListRequest.Builder getOpenExamListRequestBuilder = GetOpenExamListRequest.newBuilder();
				if (begin != null) {
					getOpenExamListRequestBuilder.setLastExamId(begin.getExamId());
					getOpenExamListRequestBuilder.setLastExamEndTime(begin.getTime());
				}
				getOpenExamListRequestBuilder.setSize(request.getItemSize());
				
				GetOpenExamListResponse getOpenExamListResponse = Futures.getUnchecked(this.examService.getOpenExamList(head, getOpenExamListRequestBuilder.build()));
				
				if (getOpenExamListResponse.getExamCount() <= 0) {
					return GetModuleItemListResponse.newBuilder()
							.setClearOldList(false)
							.setHasMore(false)
							.setListIndexBegin(ByteString.EMPTY)
							.setListIndexEnd(ByteString.EMPTY)
							.build();
				} else {
					ExamProtos.Exam beginExam = getOpenExamListResponse.getExam(0);
					ExamProtos.Exam endExam = getOpenExamListResponse.getExam(getOpenExamListResponse.getExamCount() - 1);
					
					return GetModuleItemListResponse.newBuilder()
							.setClearOldList(false)
							.addAllItem(convertExamToItemList(getOpenExamListResponse.getExamList(), Collections.<Integer, ExamProtos.UserResult>emptyMap()).values())
							.setHasMore(getOpenExamListResponse.getHasMore())
							.setListIndexBegin(DiscoverDAOProtos.ModuleExamListIndex.newBuilder()
									.setExamId(beginExam.getExamId())
									.setTime(beginExam.getEndTime())
									.build().toByteString())
							.setListIndexEnd(DiscoverDAOProtos.ModuleExamListIndex.newBuilder()
									.setExamId(endExam.getExamId())
									.setTime(endExam.getEndTime())
									.build().toByteString())
							.build();
				}
			} else if (categoryId == 2) {
				DiscoverDAOProtos.ModuleExamListIndex begin = null;
				if (request.hasListIndexBegin() && !request.getListIndexBegin().isEmpty()) {
					try {
						begin = DiscoverDAOProtos.ModuleExamListIndex.parseFrom(request.getListIndexBegin());
					} catch (InvalidProtocolBufferException e) {
						// ignore
					}
				}
				
				GetClosedExamListRequest.Builder getClosedExamListRequestBuilder = GetClosedExamListRequest.newBuilder();
				if (begin != null) {
					getClosedExamListRequestBuilder.setLastExamId(begin.getExamId());
					getClosedExamListRequestBuilder.setLastExamSubmitTime(begin.getTime());
				}
				getClosedExamListRequestBuilder.setSize(request.getItemSize());
				
				GetClosedExamListResponse getClosedExamListResponse = Futures.getUnchecked(this.examService.getClosedExamList(head, getClosedExamListRequestBuilder.build()));
				
				if (getClosedExamListResponse.getExamCount() <= 0) {
					return GetModuleItemListResponse.newBuilder()
							.setClearOldList(false)
							.setHasMore(false)
							.setListIndexBegin(ByteString.EMPTY)
							.setListIndexEnd(ByteString.EMPTY)
							.build();
				} else {
					Map<Integer, ExamProtos.UserResult> userResultMap = new HashMap<Integer, ExamProtos.UserResult>(getClosedExamListResponse.getUserResultCount());
					for (ExamProtos.UserResult userResult : getClosedExamListResponse.getUserResultList()) {
						userResultMap.put(userResult.getExamId(), userResult);
					}
					
					ExamProtos.Exam beginExam = getClosedExamListResponse.getExam(0);
					ExamProtos.UserResult beginExamUserResult = userResultMap.get(beginExam.getExamId());
					
					int beginSubmitTime = beginExamUserResult != null && beginExamUserResult.hasSubmitTime() ? beginExamUserResult.getSubmitTime() : beginExam.getEndTime();
					
					ExamProtos.Exam endExam = getClosedExamListResponse.getExam(getClosedExamListResponse.getExamCount() - 1);
					ExamProtos.UserResult endExamUserResult = userResultMap.get(endExam.getExamId());
					
					int endSubmitTime = endExamUserResult != null && endExamUserResult.hasSubmitTime() ? endExamUserResult.getSubmitTime() : endExam.getEndTime();
					
					return GetModuleItemListResponse.newBuilder()
							.setClearOldList(false)
							.addAllItem(convertExamToItemList(getClosedExamListResponse.getExamList(), userResultMap).values())
							.setHasMore(getClosedExamListResponse.getHasMore())
							.setListIndexBegin(DiscoverDAOProtos.ModuleExamListIndex.newBuilder()
									.setExamId(beginExam.getExamId())
									.setTime(beginSubmitTime)
									.build().toByteString())
							.setListIndexEnd(DiscoverDAOProtos.ModuleExamListIndex.newBuilder()
									.setExamId(endExam.getExamId())
									.setTime(endSubmitTime)
									.build().toByteString())
							.build();
				}
			}
		}
		return GetModuleItemListResponse.newBuilder()
				.setClearOldList(false)
				.setHasMore(false)
				.setListIndexBegin(ByteString.EMPTY)
				.setListIndexEnd(ByteString.EMPTY)
				.build();
	}
	
	// return examId -> item
	private Map<Integer, DiscoverProtos.Item> convertExamToItemList(List<ExamProtos.Exam> examList, Map<Integer, ExamProtos.UserResult> userResultMap) {
		if (examList.isEmpty()) {
			return Collections.emptyMap();
		}
		
		DiscoverProtos.Item.Builder tmpItemBuilder = DiscoverProtos.Item.newBuilder();
		DateFormat df = new SimpleDateFormat("M月d日HH:mm");
		
		Map<Integer, DiscoverProtos.Item> itemMap = new LinkedHashMap<Integer, DiscoverProtos.Item>(examList.size());
		
		int now = (int) (System.currentTimeMillis() / 1000L);
		for (ExamProtos.Exam exam : examList) {
			ExamProtos.UserResult userResult = userResultMap.get(exam.getExamId());
			final long itemId = getItemId(EXAM_ITEM_PREFIX, exam.getExamId());
			
			tmpItemBuilder.setItemId(itemId);
			tmpItemBuilder.setItemName(exam.getExamName());
			if (examItemIconName != null && !examItemIconName.isEmpty()) {
				tmpItemBuilder.setIconName(examItemIconName);
			}
			tmpItemBuilder.setCreateTime(userResult != null && userResult.hasSubmitTime() ? userResult.getSubmitTime() : exam.getEndTime());
			
			if (userResult != null && userResult.hasScore()) {
				if (userResult.hasSubmitTime()) {
					tmpItemBuilder.setItemDesc(userResult.getScore() +"分, 已考完");
				} else {
					tmpItemBuilder.setItemDesc(userResult.getScore() +"分, 未交卷");
				}
			} else {
				if (now < exam.getEndTime()) {
					tmpItemBuilder.setItemDesc("开始时间:" + df.format(new Date(exam.getStartTime() * 1000L)));
				} else {
					tmpItemBuilder.setItemDesc("缺考");
				}
			}
			tmpItemBuilder.setEnableComment(false);
			tmpItemBuilder.setEnableScore(false);
			itemMap.put(exam.getExamId(), tmpItemBuilder.build());
		}
		
		return itemMap;
	}
	
	private static int getPrefix(long itemId) {
		return Math.abs((int) (itemId / 1000000000L));
	}
	
	private static int getExtendsId(long itemId) {
		return (int) (itemId % 1000000000L);
	}
	
	private static long getItemId(int prefix, int extendsId) {
		if (extendsId < 0) {
			return - ((prefix * 1000000000L) - extendsId);
		} else {
			return prefix * 1000000000L + extendsId;
		}
	}
	
	public static void main(String args[]) {
		long itemId = -1234567890L;
		
		int prefix = getPrefix(itemId);
		int extendsId = getExtendsId(itemId);
		
		System.out.println(prefix + "\t" + extendsId);
	}
	
}
