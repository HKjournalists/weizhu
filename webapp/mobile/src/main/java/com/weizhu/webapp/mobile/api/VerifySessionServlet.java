package com.weizhu.webapp.mobile.api;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.common.util.concurrent.Futures;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.weizhu.common.service.ServiceUtil;
import com.weizhu.proto.UploadService;
import com.weizhu.proto.UserProtos;
import com.weizhu.proto.UserService;
import com.weizhu.proto.UserProtos.GetUserByIdRequest;
import com.weizhu.proto.UserProtos.GetUserResponse;
import com.weizhu.proto.WeizhuProtos.RequestHead;

@Singleton
@SuppressWarnings("serial")
public class VerifySessionServlet extends HttpServlet {

	private final Provider<RequestHead> requestHeadProvider;
	private final UserService userService;
	private final UploadService uploadService;
	
	@Inject
	public VerifySessionServlet(Provider<RequestHead> requestHeadProvider, UserService userService, UploadService uploadService) {
		this.requestHeadProvider = requestHeadProvider;
		this.userService = userService;
		this.uploadService = uploadService;
	}
	
	@Override
	protected void doGet(HttpServletRequest httpRequest, HttpServletResponse httpResponse) throws ServletException, IOException {
		this.doPost(httpRequest, httpResponse);
	}
	
	private static final Gson GSON = new Gson();
	
	@Override
	protected void doPost(HttpServletRequest httpRequest, HttpServletResponse httpResponse) throws ServletException, IOException {
		
		final RequestHead head = this.requestHeadProvider.get();
		
		GetUserByIdRequest request = GetUserByIdRequest.newBuilder()
				.addUserId(head.getSession().getUserId())
				.build();
		
		GetUserResponse response = Futures.getUnchecked(this.userService.getUserById(head, request));
		
		UserProtos.User user = null;
		for (int i=0; i<response.getUserCount(); ++i) {
			if (head.getSession().getUserId() == response.getUser(i).getBase().getUserId()) {
				user = response.getUser(i);
			}
		}
		
		if (user == null) {
			httpResponse.sendError(HttpServletResponse.SC_NOT_FOUND, "user not found");
			return;
		}
		
		String imageUrlPrefix = Futures.getUnchecked(this.uploadService.getUploadUrlPrefix(head, ServiceUtil.EMPTY_REQUEST)).getImageUrlPrefix();
		
		JsonObject obj = new JsonObject();
		obj.addProperty("user_id", head.getSession().getUserId());
		obj.addProperty("user_name", user.getBase().getUserName());
		if (user.getBase().hasGender()) {
			obj.addProperty("gender", user.getBase().getGender().name());
		}
		if (user.getBase().hasAvatar()) {
			obj.addProperty("avatar", imageUrlPrefix + user.getBase().getAvatar());
		}
		
		httpResponse.setContentType("application/json;charset=UTF-8");
		GSON.toJson(obj, httpResponse.getWriter());
	}

}
