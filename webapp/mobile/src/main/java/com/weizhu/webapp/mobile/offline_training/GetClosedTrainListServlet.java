package com.weizhu.webapp.mobile.offline_training;

import java.io.IOException;
import java.util.Base64;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.common.util.concurrent.Futures;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.google.protobuf.ByteString;
import com.weizhu.common.service.ServiceUtil;
import com.weizhu.common.utils.JsonUtil;
import com.weizhu.proto.DiscoverV2Protos;
import com.weizhu.proto.DiscoverV2Service;
import com.weizhu.proto.OfflineTrainingProtos;
import com.weizhu.proto.OfflineTrainingService;
import com.weizhu.proto.UploadService;
import com.weizhu.proto.UserProtos;
import com.weizhu.proto.UserService;
import com.weizhu.proto.DiscoverV2Protos.GetItemByIdRequest;
import com.weizhu.proto.DiscoverV2Protos.GetItemByIdResponse;
import com.weizhu.proto.OfflineTrainingProtos.GetClosedTrainListRequest;
import com.weizhu.proto.OfflineTrainingProtos.GetClosedTrainListResponse;
import com.weizhu.proto.UploadProtos.GetUploadUrlPrefixResponse;
import com.weizhu.proto.UserProtos.GetUserByIdRequest;
import com.weizhu.proto.UserProtos.GetUserResponse;
import com.weizhu.proto.WeizhuProtos.RequestHead;
import com.weizhu.web.ParamUtil;

@Singleton
@SuppressWarnings("serial")
public class GetClosedTrainListServlet extends HttpServlet {

	private final Provider<RequestHead> requestHeadProvider;
	private final OfflineTrainingService offlineTrainingService;
	private final UserService userService;
	private final DiscoverV2Service discoverV2Service;
	private final UploadService uploadService;
	
	@Inject
	public GetClosedTrainListServlet(Provider<RequestHead> requestHeadProvider, 
			OfflineTrainingService offlineTrainingService, 
			UserService userService, 
			DiscoverV2Service discoverV2Service,
			UploadService uploadService
			) {
		this.requestHeadProvider = requestHeadProvider;
		this.offlineTrainingService = offlineTrainingService;
		this.userService = userService;
		this.discoverV2Service = discoverV2Service;
		this.uploadService = uploadService;
	}
	
	@Override
	protected void doGet(HttpServletRequest httpRequest, HttpServletResponse httpResponse) throws ServletException, IOException {
		this.doPost(httpRequest, httpResponse);
	}
	
	@Override
	protected void doPost(HttpServletRequest httpRequest, HttpServletResponse httpResponse) throws ServletException, IOException {
		final int size = ParamUtil.getInt(httpRequest, "size", 10);
		final String offsetIndexStr = ParamUtil.getString(httpRequest, "offset_index", null);
		
		final ByteString offsetIndex;
		if (offsetIndexStr != null && !offsetIndexStr.isEmpty()) {
			ByteString tmp = null;
			try {
				tmp = ByteString.copyFrom(Base64.getUrlDecoder().decode(offsetIndexStr));
			} catch (IllegalArgumentException e) {
				tmp = null;
			}
			offsetIndex = tmp;
		} else {
			offsetIndex = null;
		}
		
		final RequestHead head = this.requestHeadProvider.get();
		
		GetClosedTrainListRequest.Builder requestBuilder = GetClosedTrainListRequest.newBuilder();
		requestBuilder.setSize(size);
		if (offsetIndex != null) {
			requestBuilder.setOffsetIndex(offsetIndex);
		}
		
		GetClosedTrainListResponse response = Futures.getUnchecked(this.offlineTrainingService.getClosedTrainList(head, requestBuilder.build()));

		final Map<Integer, OfflineTrainingProtos.TrainCount> refTrainCountMap = new TreeMap<Integer, OfflineTrainingProtos.TrainCount>();
		for (OfflineTrainingProtos.TrainCount trainCount : response.getRefTrainCountList()) {
			refTrainCountMap.put(trainCount.getTrainId(), trainCount);
		}
		
		final Map<Integer, OfflineTrainingProtos.TrainUser> refTrainUserMap = new TreeMap<Integer, OfflineTrainingProtos.TrainUser>();
		for (OfflineTrainingProtos.TrainUser trainUser : response.getRefTrainUserList()) {
			refTrainUserMap.put(trainUser.getTrainId(), trainUser);
		}
		
		Set<Long> refUserIdSet = new TreeSet<Long>();
		Set<Long> refDiscoverItemIdSet = new TreeSet<Long>();
		for (OfflineTrainingProtos.Train train : response.getTrainList()) {
			refUserIdSet.addAll(train.getLecturerUserIdList());
			refDiscoverItemIdSet.addAll(train.getDiscoverItemIdList());
		}
		
		final Map<Long, UserProtos.User> refUserMap;
		if (refUserIdSet.isEmpty()) {
			refUserMap = Collections.emptyMap();
		} else {
			GetUserResponse getUserResponse = Futures.getUnchecked(
					this.userService.getUserById(head, 
							GetUserByIdRequest.newBuilder()
							.addAllUserId(refUserIdSet)
							.build()));
			refUserMap = new TreeMap<Long, UserProtos.User>();
			for (UserProtos.User user : getUserResponse.getUserList()) {
				refUserMap.put(user.getBase().getUserId(), user);
			}
		}
		
		final Map<Long, DiscoverV2Protos.Item> refDiscoverItemMap;
		if (refDiscoverItemIdSet.isEmpty()) {
			refDiscoverItemMap = Collections.emptyMap();
		} else {
			GetItemByIdResponse getItemResponse = Futures.getUnchecked(
					this.discoverV2Service.getItemById(head, 
							GetItemByIdRequest.newBuilder()
							.addAllItemId(refDiscoverItemIdSet)
							.build()));
			refDiscoverItemMap = new TreeMap<Long, DiscoverV2Protos.Item>();
			for (DiscoverV2Protos.Item item : getItemResponse.getItemList()) {
				refDiscoverItemMap.put(item.getBase().getItemId(), item);
			}
		}
		
		GetUploadUrlPrefixResponse getUploadUrlPrefixResponse = Futures.getUnchecked(this.uploadService.getUploadUrlPrefix(head, ServiceUtil.EMPTY_REQUEST));
		
		final int now = (int) (System.currentTimeMillis() / 1000L);
		
		JsonArray trainArray = new JsonArray();
		for (OfflineTrainingProtos.Train train : response.getTrainList()) {
			trainArray.add(OfflineTrainingUtil.buildTrain(now, train, refTrainCountMap, refTrainUserMap, refUserMap, refDiscoverItemMap, getUploadUrlPrefixResponse));
		}
		
		JsonObject resultObj = new JsonObject();
		
		resultObj.add("train", trainArray);
		resultObj.addProperty("has_more", response.getHasMore());
		resultObj.addProperty("offset_index", Base64.getUrlEncoder().encodeToString(response.getOffsetIndex().toByteArray()));
		
		httpResponse.setContentType("application/json;charset=UTF-8");
		JsonUtil.GSON.toJson(resultObj, httpResponse.getWriter());
	}

}
