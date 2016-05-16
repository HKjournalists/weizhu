package com.weizhu.service.component;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.inject.Inject;
import com.weizhu.proto.AdminComponentProtos.CreateScoreRequest;
import com.weizhu.proto.AdminComponentProtos.CreateScoreResponse;
import com.weizhu.proto.AdminComponentProtos.GetScoreByIdRequest;
import com.weizhu.proto.AdminComponentProtos.GetScoreByIdResponse;
import com.weizhu.proto.AdminComponentProtos.GetScoreListRequest;
import com.weizhu.proto.AdminComponentProtos.GetScoreListResponse;
import com.weizhu.proto.AdminComponentProtos.UpdateScoreRequest;
import com.weizhu.proto.AdminComponentProtos.UpdateScoreResponse;
import com.weizhu.proto.AdminComponentProtos.UpdateScoreStateRequest;
import com.weizhu.proto.AdminComponentProtos.UpdateScoreStateResponse;
import com.weizhu.proto.AdminComponentService;
import com.weizhu.proto.ComponentProtos;
import com.weizhu.proto.AdminProtos.AdminHead;
import com.weizhu.service.component.score.ScoreManager;

public class AdminComponentServiceImpl implements AdminComponentService {

	@SuppressWarnings("unused")
	private static final Logger logger = LoggerFactory.getLogger(AdminComponentServiceImpl.class);
	
	private static final ImmutableList<ComponentProtos.State> SCORE_LIST_STATE = 
			ImmutableList.of(ComponentProtos.State.NORMAL,ComponentProtos.State.DISABLE);
	
	private final ScoreManager scoreManager;
	
	@Inject
	public AdminComponentServiceImpl( ScoreManager scoreManager){
		this.scoreManager = scoreManager;
	}
	
	@Override
	public ListenableFuture<GetScoreByIdResponse> getScoreById(AdminHead head, GetScoreByIdRequest request) {
		return Futures.immediateFuture(scoreManager.getScoreById(head, request, SCORE_LIST_STATE));
	}

	@Override
	public ListenableFuture<GetScoreListResponse> getScoreList(AdminHead head, GetScoreListRequest request) {
		final long companyId = head.getCompanyId();
		
		return Futures.immediateFuture(scoreManager.getScoreList(companyId, request, SCORE_LIST_STATE));
	}

	@Override
	public ListenableFuture<CreateScoreResponse> createScore(AdminHead head, CreateScoreRequest request) {
		return Futures.immediateFuture(scoreManager.createScore(head, request));
	}

	@Override
	public ListenableFuture<UpdateScoreResponse> updateScore(AdminHead head, UpdateScoreRequest request) {
		return Futures.immediateFuture(scoreManager.updateScore(head, request, SCORE_LIST_STATE));
	}

	@Override
	public ListenableFuture<UpdateScoreStateResponse> updateScoreState(AdminHead head,
			UpdateScoreStateRequest request) {
		return Futures.immediateFuture(scoreManager.updateScoreState(head, request, SCORE_LIST_STATE));
	}

}
