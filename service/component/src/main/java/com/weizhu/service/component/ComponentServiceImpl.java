package com.weizhu.service.component;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.inject.Inject;
import com.weizhu.proto.ComponentProtos.GetScoreByIdRequest;
import com.weizhu.proto.ComponentProtos.GetScoreByIdResponse;
import com.weizhu.proto.ComponentProtos.GetScoreUserListRequest;
import com.weizhu.proto.ComponentProtos.GetScoreUserListResponse;
import com.weizhu.proto.ComponentProtos.GetUserScoreListRequest;
import com.weizhu.proto.ComponentProtos.GetUserScoreListResponse;
import com.weizhu.proto.ComponentProtos.ScoreRequest;
import com.weizhu.proto.ComponentProtos.ScoreResponse;
import com.weizhu.proto.ComponentProtos;
import com.weizhu.proto.ComponentService;
import com.weizhu.proto.WeizhuProtos.RequestHead;
import com.weizhu.service.component.score.ScoreManager;

public class ComponentServiceImpl implements ComponentService {
	
	@SuppressWarnings("unused")
	private static final Logger logger = LoggerFactory.getLogger(ComponentServiceImpl.class);
	
	private static final ImmutableList<ComponentProtos.State> USER_LIST_STATE = ImmutableList.of(ComponentProtos.State.NORMAL);
	
	private final ScoreManager scoreManager;
	
	@Inject
	public ComponentServiceImpl( ScoreManager scoreManager){
		this.scoreManager = scoreManager;
	}
	
	@Override
	public ListenableFuture<GetScoreByIdResponse> getScoreById(RequestHead head, GetScoreByIdRequest request) {
		if(request.getScoreIdCount() <= 0){
			return Futures.immediateFuture(GetScoreByIdResponse.newBuilder().build());
		}
		
		return Futures.immediateFuture(scoreManager.getScoreById(head, request.getScoreIdList(), USER_LIST_STATE));
	}

	@Override
	public ListenableFuture<GetScoreUserListResponse> getScoreUserList(RequestHead head,
			GetScoreUserListRequest request) {
		final long companyId = head.getSession().getCompanyId();
		
		return Futures.immediateFuture(scoreManager.getScoreUserList(companyId, request, USER_LIST_STATE));
	}

	@Override
	public ListenableFuture<GetUserScoreListResponse> getUserScoreList(RequestHead head,
			GetUserScoreListRequest request) {
		//获取companyId
		final long companyId = head.getSession().getCompanyId();
		
		return Futures.immediateFuture(scoreManager.getUserScoreList(companyId, request, USER_LIST_STATE));
	}

	@Override
	public ListenableFuture<ScoreResponse> score(RequestHead head, ScoreRequest request) {
		return Futures.immediateFuture(scoreManager.score(head, request, USER_LIST_STATE));
	}

}
