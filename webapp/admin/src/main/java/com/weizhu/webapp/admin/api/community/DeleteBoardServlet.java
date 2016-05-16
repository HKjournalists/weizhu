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
public class DeleteBoardServlet extends HttpServlet {
	private final Provider<AdminHead> adminHeadProvider;
	private final AdminCommunityService adminCommunityService;

	@Inject
	public DeleteBoardServlet(Provider<AdminHead> adminHeadProvider, AdminCommunityService adminCommunityService) {
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
		Boolean isForceDelete = ParamUtil.getBoolean(httpRequest, "is_force_delete", null);
		
		AdminCommunityProtos.DeleteBoardRequest.Builder requestBuilder =AdminCommunityProtos.DeleteBoardRequest.newBuilder();
		requestBuilder.setBoardId(boardId);
		if(isForceDelete!=null){
			requestBuilder.setIsForceDelete(isForceDelete);
		}
		AdminCommunityProtos.DeleteBoardResponse response = Futures.getUnchecked(this.adminCommunityService.deleteBoard(this.adminHeadProvider.get(),
				requestBuilder.build()));

		httpResponse.setContentType("application/json;charset=UTF-8");
		JsonUtil.PROTOBUF_JSON_FORMAT.print(response, httpResponse.getWriter());
	}

}
