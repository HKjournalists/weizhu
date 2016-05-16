package com.weizhu.webapp.admin.api.community;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
import com.weizhu.common.service.ServiceUtil;
import com.weizhu.common.utils.JsonUtil;
import com.weizhu.proto.AdminCommunityProtos;
import com.weizhu.proto.AdminCommunityService;
import com.weizhu.proto.AllowProtos;
import com.weizhu.proto.AllowService;
import com.weizhu.proto.UploadService;
import com.weizhu.proto.AdminProtos.AdminHead;
import com.weizhu.proto.AllowProtos.GetModelByIdRequest;
import com.weizhu.proto.AllowProtos.GetModelByIdResponse;
import com.weizhu.proto.CommunityProtos;
import com.weizhu.web.ParamUtil;
import com.weizhu.webapp.admin.api.Util;

@Singleton
@SuppressWarnings("serial")
public class GetBoardServlet extends HttpServlet {
	private final Provider<AdminHead> adminHeadProvider;
	private final AdminCommunityService adminCommunityService;
	private final UploadService uploadService;
	private final AllowService allowService;

	@Inject
	public GetBoardServlet(Provider<AdminHead> adminHeadProvider, AdminCommunityService adminCommunityService, UploadService uploadService, AllowService allowService) {
		this.adminHeadProvider = adminHeadProvider;
		this.adminCommunityService = adminCommunityService;
		this.uploadService = uploadService;
		this.allowService = allowService;

	}

	@Override
	protected void doGet(HttpServletRequest httpRequest, HttpServletResponse httpResponse) throws ServletException, IOException {
		this.doPost(httpRequest, httpResponse);
	}

	@Override
	protected void doPost(HttpServletRequest httpRequest, HttpServletResponse httpResponse) throws ServletException, IOException {

		// 1. 取出参数
		String boardName = ParamUtil.getString(httpRequest, "board_name", null);
		
		final AdminHead head = this.adminHeadProvider.get();

		AdminCommunityProtos.GetBoardListRequest.Builder requestBuilder = AdminCommunityProtos.GetBoardListRequest.newBuilder();
		if (null != boardName) {
			requestBuilder.setBoardName(boardName);
		}

		AdminCommunityProtos.GetBoardListResponse response = Futures.getUnchecked(this.adminCommunityService.getBoardList(head,
				requestBuilder.build()));

		String imageUrlPrefix = Futures.getUnchecked(this.uploadService.getUploadUrlPrefix(head, ServiceUtil.EMPTY_REQUEST)).getImageUrlPrefix();

		
		Set<Integer> allowModelIds = new TreeSet<Integer>();
		for (CommunityProtos.Board board : response.getBoardList()) {
			if(board.hasAllowModelId()){
				allowModelIds.add(board.getAllowModelId());
			}
		}
		GetModelByIdResponse allowModelResponse = Futures.getUnchecked(allowService.getModelById(head, GetModelByIdRequest.newBuilder()
				.addAllModelId(allowModelIds)
				.build()));
		Map<Integer, AllowProtos.Model> allowModelMap = Util.getAllowModelMap(allowModelResponse.getModelList());
		
		//拼接json
		JsonObject result = new JsonObject();
		JsonArray boardJsonArray = new JsonArray();

		Map<Integer, CommunityProtos.Board> boardMap = new HashMap<Integer, CommunityProtos.Board>();
		List<Integer> rootBoardIdList = new ArrayList<Integer>();
		Map<Integer, List<Integer>> parentBoardIdToSubIdListMap = new HashMap<Integer, List<Integer>>();
		for (CommunityProtos.Board board : response.getBoardList()) {
			int boardId = board.getBoardId();
			Integer parentId = board.hasParentBoardId() ? board.getParentBoardId() : null;
			boardMap.put(boardId, board);
			if (parentId == null) {
				rootBoardIdList.add(board.getBoardId());
			} else {
				List<Integer> subIdList = parentBoardIdToSubIdListMap.get(parentId);
				if (subIdList == null) {
					subIdList = new ArrayList<Integer>();
				}
				subIdList.add(board.getBoardId());
				parentBoardIdToSubIdListMap.put(parentId, subIdList);
			}
		}

		for (int rootBoardId : rootBoardIdList) {
			boardJsonArray.add(this.buildBoardJsonTree(rootBoardId, boardMap, parentBoardIdToSubIdListMap, imageUrlPrefix, allowModelMap));
		}
		result.add("board", boardJsonArray);

		httpResponse.setContentType("application/json;charset=UTF-8");
		JsonUtil.GSON.toJson(result, httpResponse.getWriter());
	}

	private JsonObject buildBoardJsonTree(int boardId, Map<Integer, CommunityProtos.Board> boardMap,
			Map<Integer, List<Integer>> parentBoardIdToSubIdListMap, String imageUrlPrefix, Map<Integer, AllowProtos.Model> allowModelMap) {
		CommunityProtos.Board board = boardMap.get(boardId);
		if (board == null) {
			return new JsonObject();
		}

		JsonObject u = new JsonObject();
		u.addProperty("board_id", board.getBoardId());
		u.addProperty("board_name", board.getBoardName());
		u.addProperty("board_icon", board.getBoardIcon());
		u.addProperty("board_icon_url", board.getBoardIcon().isEmpty() ? "" : imageUrlPrefix + board.getBoardIcon());
		u.addProperty("board_desc", board.getBoardDesc());
		u.addProperty("parent_board_id", board.getParentBoardId());
		u.addProperty("is_leaf_board", board.getIsLeafBoard());
		u.addProperty("is_hot", board.getIsHot());
		u.addProperty("post_total_count", board.getPostTotalCount());
		if (board.hasAllowModelId()) {
			u.addProperty("allow_model_id", board.getAllowModelId());
			u.addProperty("allow_model_name", Util.getAllowModelName(allowModelMap, board.getAllowModelId()));
		} else {
			u.addProperty("allow_model_id", "");
			u.addProperty("allow_model_name", "");
		}
		JsonArray children = new JsonArray();

		List<Integer> subList = parentBoardIdToSubIdListMap.get(boardId);
		if (subList != null) {
			for (Integer subBoardId : subList) {
				JsonObject child = buildBoardJsonTree(subBoardId, boardMap, parentBoardIdToSubIdListMap, imageUrlPrefix, allowModelMap);
				if (child != null) {
					children.add(child);
				}
			}
		}

		u.add("children", children);
		return u;
	}

}
