package com.weizhu.webapp.admin.api.community;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.common.util.concurrent.Futures;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.weizhu.common.utils.JsonUtil;
import com.weizhu.proto.AdminCommunityProtos;
import com.weizhu.proto.AdminCommunityService;
import com.weizhu.proto.AdminProtos.AdminHead;
import com.weizhu.web.ParamUtil;

@Singleton
@SuppressWarnings("serial")
public class UpdateBoardServlet extends HttpServlet {
	private final Provider<AdminHead> adminHeadProvider;
	private final AdminCommunityService adminCommunityService;

	@Inject
	public UpdateBoardServlet(Provider<AdminHead> adminHeadProvider, AdminCommunityService adminCommunityService) {
		this.adminHeadProvider = adminHeadProvider;
		this.adminCommunityService = adminCommunityService;
	}

	@Override
	protected void doGet(HttpServletRequest httpRequest, HttpServletResponse httpResponse) throws ServletException, IOException {
		this.doPost(httpRequest, httpResponse);
	}

	@Override
	protected void doPost(HttpServletRequest httpRequest, HttpServletResponse httpResponse) throws ServletException, IOException {

		// 1. 取出参数
		Integer boardId = ParamUtil.getInt(httpRequest, "board_id", -1);
		String boardName = ParamUtil.getString(httpRequest, "board_name", "");
		String board_icon = ParamUtil.getString(httpRequest, "board_icon", "");
		String board_desc = ParamUtil.getString(httpRequest, "board_desc", "");
		Integer parent_board_id = ParamUtil.getInt(httpRequest, "parent_board_id", null);
		Integer allowModelId = ParamUtil.getInt(httpRequest, "allow_model_id", null);
		
		
		AdminCommunityProtos.UpdateBoardRequest.Builder requestBuilder= AdminCommunityProtos.UpdateBoardRequest.newBuilder();
		requestBuilder.setBoardId(boardId);
		requestBuilder.setBoardName(boardName);
		requestBuilder.setBoardIcon(board_icon);
		requestBuilder.setBoardDesc(board_desc);
		
		if(null!=parent_board_id){
			requestBuilder.setParentBoardId(parent_board_id);
		}
		if (null != allowModelId) {
			requestBuilder.setAllowModelId(allowModelId);
		}
		AdminCommunityProtos.UpdateBoardResponse response = Futures.getUnchecked(this.adminCommunityService.updateBoard(this.adminHeadProvider.get(),
				requestBuilder.build()));

		httpResponse.setContentType("application/json;charset=UTF-8");
		JsonUtil.PROTOBUF_JSON_FORMAT.print(response, httpResponse.getWriter());
	}

}
