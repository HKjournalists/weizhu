package com.weizhu.webapp.mobile.exam;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.common.util.concurrent.Futures;
import com.google.gson.JsonObject;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.weizhu.common.service.ServiceUtil;
import com.weizhu.common.utils.JsonUtil;
import com.weizhu.proto.ExamProtos.GetExamInfoRequest;
import com.weizhu.proto.ExamProtos.GetExamInfoResponse;
import com.weizhu.proto.ExamService;
import com.weizhu.proto.UploadService;
import com.weizhu.proto.UserProtos;
import com.weizhu.proto.UploadProtos.GetUploadUrlPrefixResponse;
import com.weizhu.proto.UserProtos.GetUserByIdRequest;
import com.weizhu.proto.UserProtos.GetUserResponse;
import com.weizhu.proto.UserService;
import com.weizhu.proto.WeizhuProtos.RequestHead;
import com.weizhu.web.ParamUtil;

@Singleton
@SuppressWarnings("serial")
public class ExamInfoServlet extends HttpServlet {

	private final Provider<RequestHead> requestHeadProvider;
	private final ExamService examService;
	private final UserService userService;
	private final UploadService uploadService;
	
	@Inject
	public ExamInfoServlet(Provider<RequestHead> requestHeadProvider, ExamService examService, UserService userService, UploadService uploadService) {
		this.requestHeadProvider = requestHeadProvider;
		this.examService = examService;
		this.userService = userService;
		this.uploadService = uploadService;
	}
	
	@Override
	protected void doGet(HttpServletRequest httpRequest, HttpServletResponse httpResponse) throws ServletException, IOException {
		this.doPost(httpRequest, httpResponse);
	}
	
	public static final String HTTP_REQUEST_ATTR_GET_EXAM_INFO_RESPONSE = 
			"com.weizhu.webapp.mobile.exam.ExamInfoServlet.HTTP_REQUEST_ATTR_GET_EXAM_INFO_RESPONSE";
	public static final String HTTP_REQUEST_ATTR_USER_INFO_RESPONSE = 
			"com.weizhu.webapp.mobile.exam.ExamInfoServlet.HTTP_REQUEST_ATTR_USER_INFO_RESPONSE";
	
	@Override
	protected void doPost(HttpServletRequest httpRequest, HttpServletResponse httpResponse) throws ServletException, IOException {
		final int examId;
		if (httpRequest.getAttribute("exam_id") != null) {
			// forward from /discover/item_content
			examId = (Integer) httpRequest.getAttribute("exam_id");
		} else {
			examId = ParamUtil.getInt(httpRequest, "exam_id", -1);
		}
		
		final RequestHead head = requestHeadProvider.get();
		
		GetExamInfoRequest request = GetExamInfoRequest.newBuilder()
				.setExamId(examId)
				.build();
		
		GetExamInfoResponse response = Futures.getUnchecked(this.examService.getExamInfo(head, request));
		
		if (response.hasExam()) {
			GetExamInfoResponse.Builder tmpBuilder = response.toBuilder();
			tmpBuilder.setExam(response.getExam().toBuilder().build());
			response = tmpBuilder.build();
		}
		
		GetUserResponse getUserResponse = Futures.getUnchecked(this.userService.getUserById(head, 
				GetUserByIdRequest.newBuilder()
				.addUserId(head.getSession().getUserId())
				.build()));
		
		GetUploadUrlPrefixResponse getUploadUrlPrefixResponse = Futures.getUnchecked(this.uploadService.getUploadUrlPrefix(head, ServiceUtil.EMPTY_REQUEST));
		
		JsonObject userObj = new JsonObject();
		for (UserProtos.User user : getUserResponse.getUserList()) {
			userObj.addProperty("user_id", user.getBase().getUserId());
			userObj.addProperty("user_name", user.getBase().getUserName());
			if (user.getBase().hasAvatar()) {
				userObj.addProperty("avatar", user.getBase().getAvatar());
				userObj.addProperty("avatar_url", getUploadUrlPrefixResponse.getImageUrlPrefix() + user.getBase().getAvatar());
			} else {
				userObj.addProperty("avatar", "");
				userObj.addProperty("avatar_url", "");
			}
		}
		
		httpRequest.setAttribute(HTTP_REQUEST_ATTR_GET_EXAM_INFO_RESPONSE, JsonUtil.PROTOBUF_JSON_FORMAT.printToString(response));
		httpRequest.setAttribute(HTTP_REQUEST_ATTR_USER_INFO_RESPONSE, JsonUtil.GSON.toJson(userObj));
		httpRequest.getRequestDispatcher("/exam/exam_info.jsp").forward(httpRequest, httpResponse);
	}

}
